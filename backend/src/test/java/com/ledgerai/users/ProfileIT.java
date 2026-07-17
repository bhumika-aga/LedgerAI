package com.ledgerai.users;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the User Profile slice (API_SPEC §6; SRS §4.2) over real HTTP against a real
 * PostgreSQL: read, partial update, persistence across sessions (FR-PROF-003), per-user isolation
 * (FR-PROF-004, BR-023), and the documented failure statuses. Skipped where no Docker runtime is
 * available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ProfileIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void getRequiresAuthentication() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/users/me", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(objectMapper.readTree(response.getBody()).at("/type").asText())
            .isEqualTo("/problems/authentication-failed");
    }
    
    @Test
    void patchRequiresAuthentication() {
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/users/me", HttpMethod.PATCH, json("{\"fullName\":\"Mallory\"}", null), String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    void readsAndPartiallyUpdatesOwnProfile() throws Exception {
        String token = registerAndGetToken("profile-owner@example.com", "Ada Pro");
        
        JsonNode initial = objectMapper.readTree(getProfile(token).getBody());
        assertThat(initial.at("/email").asText()).isEqualTo("profile-owner@example.com");
        assertThat(initial.at("/fullName").asText()).isEqualTo("Ada Pro");
        assertThat(initial.at("/professionalDetails").isNull()).isTrue();
        
        ResponseEntity<String> updated = patchProfile(token,
            "{\"professionalDetails\":\"Chartered Accountant\",\"preferences\":{\"theme\":\"dark\"}}");
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // The supplied fields changed; the omitted one did not (API_SPEC §2.3 partial update).
        JsonNode body = objectMapper.readTree(updated.getBody());
        assertThat(body.at("/professionalDetails").asText()).isEqualTo("Chartered Accountant");
        assertThat(body.at("/preferences/theme").asText()).isEqualTo("dark");
        assertThat(body.at("/fullName").asText()).isEqualTo("Ada Pro");
    }
    
    @Test
    void persistsProfileChangesAcrossSessions() throws Exception {
        String email = "persist@example.com";
        String token = registerAndGetToken(email, "Before");
        patchProfile(token, "{\"fullName\":\"After\",\"preferences\":{\"density\":\"compact\"}}");
        
        // A brand-new session (fresh login, fresh token) must still observe the change — FR-PROF-003.
        String freshToken = loginAndGetToken(email);
        JsonNode profile = objectMapper.readTree(getProfile(freshToken).getBody());
        
        assertThat(profile.at("/fullName").asText()).isEqualTo("After");
        assertThat(profile.at("/preferences/density").asText()).isEqualTo("compact");
    }
    
    @Test
    void keepsProfilesIsolatedBetweenUsers() throws Exception {
        String aliceToken = registerAndGetToken("alice@example.com", "Alice");
        String bobToken = registerAndGetToken("bob@example.com", "Bob");
        
        patchProfile(aliceToken, "{\"fullName\":\"Alice Updated\",\"professionalDetails\":\"Alice's firm\"}");
        
        // Bob's token must only ever resolve to Bob — FR-PROF-004 / BR-023.
        JsonNode bob = objectMapper.readTree(getProfile(bobToken).getBody());
        assertThat(bob.at("/email").asText()).isEqualTo("bob@example.com");
        assertThat(bob.at("/fullName").asText()).isEqualTo("Bob");
        assertThat(bob.at("/professionalDetails").isNull()).isTrue();
        
        JsonNode alice = objectMapper.readTree(getProfile(aliceToken).getBody());
        assertThat(alice.at("/fullName").asText()).isEqualTo("Alice Updated");
    }
    
    @Test
    void rejectsOverLongFieldsWith422() throws Exception {
        String token = registerAndGetToken("too-long@example.com", "Ada Pro");
        
        ResponseEntity<String> response = patchProfile(token,
            "{\"fullName\":\"" + "x".repeat(256) + "\"}");
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        JsonNode problem = objectMapper.readTree(response.getBody());
        assertThat(problem.at("/type").asText()).isEqualTo("/problems/validation-error");
        assertThat(problem.at("/validationErrors/0/field").asText()).isEqualTo("fullName");
        
        // The rejected value must not have been persisted — VR-003 "retain prior valid values".
        assertThat(objectMapper.readTree(getProfile(token).getBody()).at("/fullName").asText())
            .isEqualTo("Ada Pro");
    }
    
    @Test
    void neverReturnsThePasswordHash() throws Exception {
        String token = registerAndGetToken("no-secrets@example.com", "Ada Pro");
        
        assertThat(getProfile(token).getBody()).doesNotContain("passwordHash").doesNotContain("$2a$");
    }
    
    private String registerAndGetToken(String email, String fullName) throws Exception {
        String body = "{\"email\":\"%s\",\"password\":\"correct-horse\",\"fullName\":\"%s\"}"
                          .formatted(email, fullName);
        ResponseEntity<String> response =
            restTemplate.postForEntity("/api/v1/auth/register", json(body, null), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).at("/tokens/accessToken").asText();
    }
    
    private String loginAndGetToken(String email) throws Exception {
        String body = "{\"email\":\"%s\",\"password\":\"correct-horse\"}".formatted(email);
        ResponseEntity<String> response =
            restTemplate.postForEntity("/api/v1/auth/login", json(body, null), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody()).at("/tokens/accessToken").asText();
    }
    
    private ResponseEntity<String> getProfile(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(
            "/api/v1/users/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
    
    private ResponseEntity<String> patchProfile(String token, String body) {
        return restTemplate.exchange(
            "/api/v1/users/me", HttpMethod.PATCH, json(body, token), String.class);
    }
    
    private HttpEntity<String> json(String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }
}
