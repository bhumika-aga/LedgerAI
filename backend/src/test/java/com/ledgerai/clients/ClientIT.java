package com.ledgerai.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the Client Management slice (API_SPEC §7; SRS §4.3) over real HTTP against a
 * real PostgreSQL: the create → list → read → edit → archive lifecycle, pagination and filtering, the
 * documented failure statuses, and — most importantly — cross-user isolation (BR-004, FR-CLNT-005,
 * AC-5). Skipped where no Docker runtime is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ClientIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void everyEndpointRequiresAuthentication() {
        UUID someId = UUID.randomUUID();
        
        assertThat(restTemplate.getForEntity("/api/v1/clients", String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/v1/clients/" + someId, String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.postForEntity("/api/v1/clients", body("{\"name\":\"X\"}", null), String.class)
                       .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.exchange("/api/v1/clients/" + someId, HttpMethod.DELETE,
            new HttpEntity<>(new HttpHeaders()), String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    void supportsTheFullClientLifecycle() throws Exception {
        String token = registerAndGetToken("lifecycle@example.com");
        
        // Create (API_SPEC §7.3).
        ResponseEntity<String> created = post(token,
            "{\"name\":\"Acme Corp\",\"contactDetails\":\"acme@example.com\",\"notes\":\"First notes\"}");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode client = objectMapper.readTree(created.getBody());
        String clientId = client.at("/id").asText();
        assertThat(client.at("/status").asText()).isEqualTo("ACTIVE");
        assertThat(client.at("/archivedAt").isNull()).isTrue();
        assertThat(created.getBody()).doesNotContain("userId");
        
        // Read (§7.2).
        JsonNode fetched = objectMapper.readTree(get(token, "/api/v1/clients/" + clientId).getBody());
        assertThat(fetched.at("/name").asText()).isEqualTo("Acme Corp");
        
        // List — the new client appears, ACTIVE by default (§7.1).
        JsonNode page = objectMapper.readTree(get(token, "/api/v1/clients").getBody());
        assertThat(page.at("/totalElements").asInt()).isEqualTo(1);
        assertThat(page.at("/content/0/name").asText()).isEqualTo("Acme Corp");
        
        // Edit — partial (§7.4).
        ResponseEntity<String> updated = patch(token, clientId, "{\"notes\":\"Updated notes\"}");
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode updatedBody = objectMapper.readTree(updated.getBody());
        assertThat(updatedBody.at("/notes").asText()).isEqualTo("Updated notes");
        assertThat(updatedBody.at("/name").asText()).isEqualTo("Acme Corp");
        
        // Archive — soft, 204 (§7.5).
        assertThat(archive(token, clientId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // Archiving is idempotent (§7.5).
        assertThat(archive(token, clientId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // The client is gone from the default (ACTIVE) list but retained and readable.
        assertThat(objectMapper.readTree(get(token, "/api/v1/clients").getBody())
                       .at("/totalElements").asInt()).isZero();
        JsonNode archivedList = objectMapper.readTree(
            get(token, "/api/v1/clients?status=ARCHIVED").getBody());
        assertThat(archivedList.at("/totalElements").asInt()).isEqualTo(1);
        assertThat(archivedList.at("/content/0/archivedAt").isNull()).isFalse();
        assertThat(get(token, "/api/v1/clients/" + clientId).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    
    @Test
    void keepsClientsIsolatedBetweenUsers() throws Exception {
        String aliceToken = registerAndGetToken("alice-clients@example.com");
        String bobToken = registerAndGetToken("bob-clients@example.com");
        String aliceClientId = objectMapper.readTree(
            post(aliceToken, "{\"name\":\"Alice Client\"}").getBody()).at("/id").asText();
        
        // Bob's list never contains Alice's client (BR-004, AC-5).
        assertThat(objectMapper.readTree(get(bobToken, "/api/v1/clients").getBody())
                       .at("/totalElements").asInt()).isZero();
        
        // Reading, editing and archiving it are all 404 — not 403, which would confirm it exists.
        assertThat(get(bobToken, "/api/v1/clients/" + aliceClientId).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(patch(bobToken, aliceClientId, "{\"name\":\"Stolen\"}").getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(archive(bobToken, aliceClientId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        // Alice's client is untouched by any of it.
        JsonNode alices = objectMapper.readTree(get(aliceToken, "/api/v1/clients/" + aliceClientId).getBody());
        assertThat(alices.at("/name").asText()).isEqualTo("Alice Client");
        assertThat(alices.at("/status").asText()).isEqualTo("ACTIVE");
    }
    
    @Test
    void reportsANonOwnedClientExactlyLikeAnUnknownOne() throws Exception {
        String aliceToken = registerAndGetToken("alice-probe@example.com");
        String bobToken = registerAndGetToken("bob-probe@example.com");
        String aliceClientId = objectMapper.readTree(
            post(aliceToken, "{\"name\":\"Alice Client\"}").getBody()).at("/id").asText();
        
        String nonOwned = get(bobToken, "/api/v1/clients/" + aliceClientId).getBody();
        String unknown = get(bobToken, "/api/v1/clients/" + UUID.randomUUID()).getBody();
        
        // Identical bar `instance`, so a probe cannot tell a real id from a fake one (SECURITY §5).
        JsonNode a = objectMapper.readTree(nonOwned);
        JsonNode b = objectMapper.readTree(unknown);
        assertThat(a.at("/type")).isEqualTo(b.at("/type"));
        assertThat(a.at("/title")).isEqualTo(b.at("/title"));
        assertThat(a.at("/detail")).isEqualTo(b.at("/detail"));
        assertThat(a.at("/status")).isEqualTo(b.at("/status"));
    }
    
    @Test
    void paginatesFiltersAndSorts() throws Exception {
        String token = registerAndGetToken("paging@example.com");
        post(token, "{\"name\":\"Charlie Ltd\"}");
        post(token, "{\"name\":\"alpha Industries\"}");
        post(token, "{\"name\":\"Bravo Acme\"}");
        
        // Default ordering is name,asc and is case-insensitive-free — a plain ORDER BY name.
        JsonNode firstPage = objectMapper.readTree(get(token, "/api/v1/clients?size=2").getBody());
        assertThat(firstPage.at("/totalElements").asInt()).isEqualTo(3);
        assertThat(firstPage.at("/totalPages").asInt()).isEqualTo(2);
        assertThat(firstPage.at("/hasNext").asBoolean()).isTrue();
        assertThat(firstPage.at("/content").size()).isEqualTo(2);
        assertThat(firstPage.at("/size").asInt()).isEqualTo(2);
        
        JsonNode secondPage = objectMapper.readTree(get(token, "/api/v1/clients?size=2&page=1").getBody());
        assertThat(secondPage.at("/page").asInt()).isEqualTo(1);
        assertThat(secondPage.at("/hasNext").asBoolean()).isFalse();
        
        // Name filter: case-insensitive, matches anywhere (§7.1 `q`).
        JsonNode filtered = objectMapper.readTree(get(token, "/api/v1/clients?q=acme").getBody());
        assertThat(filtered.at("/totalElements").asInt()).isEqualTo(1);
        assertThat(filtered.at("/content/0/name").asText()).isEqualTo("Bravo Acme");
        
        // Unknown filter params are ignored (§2.6).
        assertThat(objectMapper.readTree(get(token, "/api/v1/clients?bogus=1").getBody())
                       .at("/totalElements").asInt()).isEqualTo(3);
    }
    
    @Test
    void defaultsToTwentyPerPage() throws Exception {
        String token = registerAndGetToken("defaults@example.com");
        post(token, "{\"name\":\"Only One\"}");
        
        JsonNode page = objectMapper.readTree(get(token, "/api/v1/clients").getBody());
        
        assertThat(page.at("/page").asInt()).isZero();
        assertThat(page.at("/size").asInt()).isEqualTo(20);
    }
    
    @Test
    void rejectsInvalidInputWith422AndPersistsNothing() throws Exception {
        String token = registerAndGetToken("validation@example.com");
        
        ResponseEntity<String> response = post(token, "{\"name\":\"\"}");
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        JsonNode problem = objectMapper.readTree(response.getBody());
        assertThat(problem.at("/type").asText()).isEqualTo("/problems/validation-error");
        assertThat(problem.at("/validationErrors/0/field").asText()).isEqualTo("name");
        assertThat(objectMapper.readTree(get(token, "/api/v1/clients").getBody())
                       .at("/totalElements").asInt()).isZero();
    }
    
    @Test
    void rejectsAnOverLongNameWith422() throws Exception {
        String token = registerAndGetToken("too-long-client@example.com");
        
        ResponseEntity<String> response = post(token, "{\"name\":\"" + "x".repeat(201) + "\"}");
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(objectMapper.readTree(response.getBody()).at("/validationErrors/0/field").asText())
            .isEqualTo("name");
    }
    
    @Test
    void rejectsAMalformedClientIdWith400() throws Exception {
        String token = registerAndGetToken("malformed@example.com");
        
        // API_SPEC §2.9.
        assertThat(get(token, "/api/v1/clients/not-a-uuid").getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }
    
    @Test
    void allowsDuplicateNames() throws Exception {
        String token = registerAndGetToken("duplicates@example.com");
        
        assertThat(post(token, "{\"name\":\"Acme Corp\"}").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // BR-024: a duplicate is not an error.
        assertThat(post(token, "{\"name\":\"Acme Corp\"}").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(objectMapper.readTree(get(token, "/api/v1/clients").getBody())
                       .at("/totalElements").asInt()).isEqualTo(2);
    }
    
    private String registerAndGetToken(String email) throws Exception {
        String payload = "{\"email\":\"%s\",\"password\":\"correct-horse\",\"fullName\":\"Pro\"}".formatted(email);
        ResponseEntity<String> response =
            restTemplate.postForEntity("/api/v1/auth/register", body(payload, null), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).at("/tokens/accessToken").asText();
    }
    
    private ResponseEntity<String> get(String token, String url) {
        return restTemplate.exchange(url, HttpMethod.GET, authorized(token), String.class);
    }
    
    private ResponseEntity<String> post(String token, String payload) {
        return restTemplate.postForEntity("/api/v1/clients", body(payload, token), String.class);
    }
    
    private ResponseEntity<String> patch(String token, String clientId, String payload) {
        return restTemplate.exchange("/api/v1/clients/" + clientId, HttpMethod.PATCH,
            body(payload, token), String.class);
    }
    
    private ResponseEntity<String> archive(String token, String clientId) {
        return restTemplate.exchange("/api/v1/clients/" + clientId, HttpMethod.DELETE,
            authorized(token), String.class);
    }
    
    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
    
    private HttpEntity<String> body(String payload, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(payload, headers);
    }
}
