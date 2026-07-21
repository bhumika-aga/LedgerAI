package com.ledgerai.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerai.ai.support.InMemoryAiPort;
import com.ledgerai.documents.support.InMemoryOcrPort;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the Activity Timeline slice (SRS §4.12; API_SPEC §15; DATABASE §5.8) over real HTTP
 * against a real PostgreSQL. Verifies that the already-implemented modules emit their documented events
 * through the shared service, that the timeline is read-only, owner-scoped, newest-first, and supports the
 * per-client view. The in-memory AI/OCR/storage ports stand in for their providers. Skipped where no
 * Docker runtime is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ActivityIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private InMemoryAiPort aiPort;
    @Autowired
    private InMemoryOcrPort ocrPort;
    
    private static byte[] pdfWithText(String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(72, 700);
                stream.showText(text);
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
    
    @AfterEach
    void resetPorts() {
        aiPort.reset();
        ocrPort.reset();
    }
    
    @Test
    void eachImplementedModuleEmitsItsDocumentedEventNewestFirst() throws Exception {
        aiPort.succeedWith("A grounded summary.");
        String token = registerAndGetToken("timeline@example.com");
        
        // Empty to start.
        assertThat(actionTypes(activities(token, null))).isEmpty();
        
        String clientId = createClient(token);            // → CLIENT_CREATED
        String documentId = uploadReadyPdf(token, clientId, "Balance sheet total 987654"); // → DOCUMENT_UPLOADED
        generateSummary(token, documentId);               // → SUMMARY_GENERATED
        deleteDocument(token, documentId);                // → DOCUMENT_DELETED
        
        List<String> types = actionTypes(activities(token, null));
        assertThat(types).containsExactlyInAnyOrder(
            "CLIENT_CREATED", "DOCUMENT_UPLOADED", "SUMMARY_GENERATED", "DOCUMENT_DELETED");
        // Newest first (default sort createdAt,desc): the last action performed is the first entry.
        assertThat(types.getFirst()).isEqualTo("DOCUMENT_DELETED");
    }
    
    @Test
    void perClientViewShowsClientScopedEventsOnly() throws Exception {
        aiPort.succeedWith("A grounded summary.");
        String token = registerAndGetToken("perclient@example.com");
        String clientId = createClient(token);
        String documentId = uploadReadyPdf(token, clientId, "Invoice total 4200");
        generateSummary(token, documentId);
        
        List<String> types = actionTypes(activities(token, clientId));
        // Client/document events carry client_id; the summary is document-scoped (client_id null), so it
        // does not appear in the per-client view (DATABASE §5.8 allows null client_id).
        assertThat(types).containsExactlyInAnyOrder("CLIENT_CREATED", "DOCUMENT_UPLOADED");
        assertThat(types).doesNotContain("SUMMARY_GENERATED");
    }
    
    @Test
    void timelineIsOwnerScoped() throws Exception {
        String aliceToken = registerAndGetToken("alice-tl@example.com");
        createClient(aliceToken);
        
        String bobToken = registerAndGetToken("bob-tl@example.com");
        // Bob sees none of Alice's activities (BR-006, SECURITY §5).
        assertThat(actionTypes(activities(bobToken, null))).isEmpty();
    }
    
    @Test
    void timelineRequiresAuthentication() {
        assertThat(restTemplate.getForEntity("/api/v1/activities", String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    void timelineIsReadOnly() throws Exception {
        String token = registerAndGetToken("readonly-tl@example.com");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        // Append-only over the API (FR-TMLN-004, BR-016): a write is never accepted (no write endpoint).
        assertThat(restTemplate.exchange("/api/v1/activities", HttpMethod.POST,
            new HttpEntity<>(headers), String.class).getStatusCode().is2xxSuccessful())
            .isFalse();
    }
    
    // --- helpers -------------------------------------------------------------------------------
    
    private ResponseEntity<String> activities(String token, String clientId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        String url = clientId == null ? "/api/v1/activities" : "/api/v1/activities?clientId=" + clientId;
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
    
    private List<String> actionTypes(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode content = objectMapper.readTree(response.getBody()).at("/content");
        List<String> types = new ArrayList<>();
        content.forEach(node -> types.add(node.at("/actionType").asText()));
        return types;
    }
    
    private String uploadReadyPdf(String token, String clientId, String text) throws Exception {
        String documentId = upload(token, clientId, pdfWithText(text), "statement.pdf", "application/pdf");
        assertThat(objectMapper.readTree(ocrStatus(token, documentId).getBody()).at("/status").asText())
            .isEqualTo("READY");
        return documentId;
    }
    
    private void generateSummary(String token, String documentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/documents/" + documentId + "/summary",
            HttpMethod.POST, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
    
    private void deleteDocument(String token, String documentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Void> response = restTemplate.exchange("/api/v1/documents/" + documentId,
            HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
    
    private ResponseEntity<String> ocrStatus(String token, String documentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/v1/documents/" + documentId + "/ocr-status",
            HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
    
    private String registerAndGetToken(String email) throws Exception {
        String payload = "{\"email\":\"%s\",\"password\":\"correct-horse\",\"fullName\":\"Pro\"}".formatted(email);
        ResponseEntity<String> response =
            restTemplate.postForEntity("/api/v1/auth/register", json(payload, null), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).at("/tokens/accessToken").asText();
    }
    
    private String createClient(String token) throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/clients", json("{\"name\":\"Acme Corp\"}", token), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).at("/id").asText();
    }
    
    private String upload(String token, String clientId, byte[] content, String filename, String contentType)
        throws Exception {
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
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/clients/" + clientId + "/documents", new HttpEntity<>(body, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).at("/id").asText();
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
