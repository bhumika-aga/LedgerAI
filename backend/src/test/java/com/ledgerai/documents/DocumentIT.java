package com.ledgerai.documents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the Document Management slice (API_SPEC §8; SRS §4.4–4.5) over real HTTP against
 * a real PostgreSQL, with the in-memory Storage port standing in for Supabase (the adapter is absent
 * from the test profile). Covers the upload → list → get → download → delete lifecycle, VR-005
 * rejection, the never-return-deleted rule (FR-STOR-005), and cross-user isolation (BR-004). Skipped
 * where no Docker runtime is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class DocumentIT {
    
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n".getBytes(StandardCharsets.UTF_8);
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void supportsTheFullDocumentLifecycle() throws Exception {
        String token = registerAndGetToken("doc-lifecycle@example.com");
        String clientId = createClient(token, "Acme Corp");
        
        // Upload (API_SPEC §8.1).
        ResponseEntity<String> uploaded = upload(token, clientId, PDF, "statement.pdf", "application/pdf");
        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode doc = objectMapper.readTree(uploaded.getBody());
        String documentId = doc.at("/id").asText();
        assertThat(doc.at("/clientId").asText()).isEqualTo(clientId);
        assertThat(doc.at("/originalFilename").asText()).isEqualTo("statement.pdf");
        assertThat(doc.at("/mimeType").asText()).isEqualTo("application/pdf");
        assertThat(doc.at("/status").asText()).isEqualTo("UPLOADED");
        assertThat(uploaded.getBody()).doesNotContain("storageReference");
        
        // List (§8.2) — the new document appears.
        JsonNode list = objectMapper.readTree(get(token, "/api/v1/clients/" + clientId + "/documents").getBody());
        assertThat(list.at("/totalElements").asInt()).isEqualTo(1);
        assertThat(list.at("/content/0/originalFilename").asText()).isEqualTo("statement.pdf");
        
        // Get (§8.3).
        assertThat(objectMapper.readTree(get(token, "/api/v1/documents/" + documentId).getBody())
                       .at("/id").asText()).isEqualTo(documentId);
        
        // Download (§8.5) — a short-lived link plus file metadata; not the bytes.
        JsonNode download = objectMapper.readTree(get(token, "/api/v1/documents/" + documentId + "/download").getBody());
        assertThat(download.at("/downloadUrl").asText()).isNotBlank();
        assertThat(download.at("/expiresAt").asText()).isNotBlank();
        assertThat(download.at("/mimeType").asText()).isEqualTo("application/pdf");
        assertThat(download.at("/originalFilename").asText()).isEqualTo("statement.pdf");
        
        // Delete (§8.4) — soft, 204.
        assertThat(deleteDoc(token, documentId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // Idempotent.
        assertThat(deleteDoc(token, documentId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // Gone from every retrieval path (FR-STOR-005).
        assertThat(get(token, "/api/v1/documents/" + documentId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(objectMapper.readTree(get(token, "/api/v1/clients/" + clientId + "/documents").getBody())
                       .at("/totalElements").asInt()).isZero();
    }
    
    @Test
    void keepsDocumentsIsolatedBetweenUsers() throws Exception {
        String aliceToken = registerAndGetToken("alice-docs@example.com");
        String aliceClient = createClient(aliceToken, "Alice Client");
        String documentId = objectMapper.readTree(
            upload(aliceToken, aliceClient, PDF, "statement.pdf", "application/pdf").getBody()).at("/id").asText();
        
        String bobToken = registerAndGetToken("bob-docs@example.com");
        
        // Bob cannot read, download, delete, or list Alice's document/client — all 404 (BR-004).
        assertThat(get(bobToken, "/api/v1/documents/" + documentId).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(bobToken, "/api/v1/documents/" + documentId + "/download").getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(deleteDoc(bobToken, documentId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get(bobToken, "/api/v1/clients/" + aliceClient + "/documents").getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(upload(bobToken, aliceClient, PDF, "x.pdf", "application/pdf").getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
        
        // Alice's document is untouched.
        assertThat(get(aliceToken, "/api/v1/documents/" + documentId).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    
    @Test
    void rejectsAnUnsupportedFileTypeWith422() throws Exception {
        String token = registerAndGetToken("doc-validation@example.com");
        String clientId = createClient(token, "Acme Corp");
        
        ResponseEntity<String> response = upload(token, clientId,
            "just text".getBytes(StandardCharsets.UTF_8), "notes.txt", "text/plain");
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        JsonNode problem = objectMapper.readTree(response.getBody());
        assertThat(problem.at("/type").asText()).isEqualTo("/problems/validation-error");
        assertThat(problem.at("/validationErrors/0/field").asText()).isEqualTo("file");
        // Nothing persisted.
        assertThat(objectMapper.readTree(get(token, "/api/v1/clients/" + clientId + "/documents").getBody())
                       .at("/totalElements").asInt()).isZero();
    }
    
    @Test
    void uploadToAnUnknownClientIs404() throws Exception {
        String token = registerAndGetToken("doc-unknown-client@example.com");
        
        assertThat(upload(token, java.util.UUID.randomUUID().toString(), PDF, "statement.pdf", "application/pdf")
                       .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void endpointsRequireAuthentication() {
        String someId = java.util.UUID.randomUUID().toString();
        assertThat(restTemplate.getForEntity("/api/v1/documents/" + someId, String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/v1/clients/" + someId + "/documents", String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    // --- helpers -------------------------------------------------------------------------------
    
    private String registerAndGetToken(String email) throws Exception {
        String payload = "{\"email\":\"%s\",\"password\":\"correct-horse\",\"fullName\":\"Pro\"}".formatted(email);
        ResponseEntity<String> response =
            restTemplate.postForEntity("/api/v1/auth/register", json(payload, null), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).at("/tokens/accessToken").asText();
    }
    
    private String createClient(String token, String name) throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/clients", json("{\"name\":\"" + name + "\"}", token), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).at("/id").asText();
    }
    
    private ResponseEntity<String> upload(String token, String clientId, byte[] content, String filename,
                                          String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(contentType));
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(resource, partHeaders));
        
        return restTemplate.postForEntity(
            "/api/v1/clients/" + clientId + "/documents", new HttpEntity<>(body, headers), String.class);
    }
    
    private ResponseEntity<String> get(String token, String url) {
        return restTemplate.exchange(url, HttpMethod.GET, authorized(token), String.class);
    }
    
    private ResponseEntity<String> deleteDoc(String token, String documentId) {
        return restTemplate.exchange(
            "/api/v1/documents/" + documentId, HttpMethod.DELETE, authorized(token), String.class);
    }
    
    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
    
    private HttpEntity<String> json(String payload, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(payload, headers);
    }
}
