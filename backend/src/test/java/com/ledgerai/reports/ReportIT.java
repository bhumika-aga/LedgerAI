package com.ledgerai.reports;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the Reports slice (SRS §4.10; API_SPEC §13; ADR-010/013) over real HTTP against a
 * real PostgreSQL, with the in-memory AI port standing in for Anthropic (the adapter is absent from the test
 * profile — the real provider is never contacted) and the in-memory OCR/storage ports for their providers.
 * Covers generate → list → get → edit/save → delete, the READY/ownership preconditions, and authentication.
 * Skipped where no Docker runtime is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ReportIT {
    
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01, 0x02};
    
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
    void generatesListsGetsEditsAndDeletesAReport() throws Exception {
        aiPort.succeedWith("Structured report: balance sheet total 987654.");
        String token = registerAndGetToken("report@example.com");
        String documentId = uploadReadyPdf(token, "Balance sheet total 987654");
        
        // Generate → 201 DRAFT.
        JsonNode created = objectMapper.readTree(generate(token, documentId, "{\"title\":\"Q4\"}").getBody());
        String reportId = created.at("/id").asText();
        assertThat(created.at("/status").asText()).isEqualTo("DRAFT");
        assertThat(created.at("/documentId").asText()).isEqualTo(documentId);
        assertThat(created.at("/content").asText()).contains("987654");
        
        // List (account-level) shows it.
        JsonNode list = objectMapper.readTree(reports(token, null).getBody());
        assertThat(list.at("/content").size()).isEqualTo(1);
        assertThat(list.at("/content/0/id").asText()).isEqualTo(reportId);
        
        // List filtered by document shows it.
        assertThat(objectMapper.readTree(reports(token, "?documentId=" + documentId).getBody())
                       .at("/content").size()).isEqualTo(1);
        
        // Get one.
        assertThat(objectMapper.readTree(getReport(token, reportId).getBody()).at("/status").asText())
            .isEqualTo("DRAFT");
        
        // Edit + save (DRAFT → SAVED).
        ResponseEntity<String> edited = restTemplate.exchange("/api/v1/reports/" + reportId, HttpMethod.PATCH,
            json("{\"content\":\"My edited report.\",\"status\":\"SAVED\"}", token), String.class);
        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode editedBody = objectMapper.readTree(edited.getBody());
        assertThat(editedBody.at("/content").asText()).isEqualTo("My edited report.");
        assertThat(editedBody.at("/status").asText()).isEqualTo("SAVED");
        
        // Delete → 204, then gone (404).
        assertThat(deleteReport(token, reportId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(getReport(token, reportId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void reportsAreOwnerScoped() throws Exception {
        aiPort.succeedWith("Alice's report.");
        String aliceToken = registerAndGetToken("alice-rpt@example.com");
        String documentId = uploadReadyPdf(aliceToken, "Confidential figures");
        String reportId = objectMapper.readTree(generate(aliceToken, documentId, null).getBody()).at("/id").asText();
        
        String bobToken = registerAndGetToken("bob-rpt@example.com");
        // Non-owned report → 404, never distinguishing "unknown" from "not owned" (BR-004, SECURITY §5).
        assertThat(getReport(bobToken, reportId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(reports(bobToken, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(reports(bobToken, null).getBody()).at("/content").size()).isZero();
    }
    
    @Test
    void generateForANonReadyDocumentIs409() throws Exception {
        ocrPort.beUnavailable(); // image whose OCR fails → document FAILED, not READY.
        String token = registerAndGetToken("notready-rpt@example.com");
        String documentId = upload(token, createClient(token), PNG, "scan.png", "image/png");
        
        assertThat(generate(token, documentId, null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
    
    @Test
    void everyEndpointRequiresAuthentication() {
        String base = "/api/v1/reports/" + java.util.UUID.randomUUID();
        assertThat(restTemplate.postForEntity(
                "/api/v1/documents/" + java.util.UUID.randomUUID() + "/reports", HttpEntity.EMPTY, String.class)
                       .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity("/api/v1/reports", String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity(base, String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.exchange(base, HttpMethod.DELETE, HttpEntity.EMPTY, String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    // --- helpers -------------------------------------------------------------------------------
    
    private ResponseEntity<String> generate(String token, String documentId, String body) {
        return restTemplate.exchange("/api/v1/documents/" + documentId + "/reports", HttpMethod.POST,
            json(body, token), String.class);
    }
    
    private ResponseEntity<String> reports(String token, String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/v1/reports" + (query == null ? "" : query),
            HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
    
    private ResponseEntity<String> getReport(String token, String reportId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/v1/reports/" + reportId, HttpMethod.GET,
            new HttpEntity<>(headers), String.class);
    }
    
    private ResponseEntity<String> deleteReport(String token, String reportId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/v1/reports/" + reportId, HttpMethod.DELETE,
            new HttpEntity<>(headers), String.class);
    }
    
    private String uploadReadyPdf(String token, String text) throws Exception {
        String documentId = upload(token, createClient(token), pdfWithText(text), "statement.pdf",
            "application/pdf");
        assertThat(objectMapper.readTree(ocrStatus(token, documentId).getBody()).at("/status").asText())
            .isEqualTo("READY");
        return documentId;
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
