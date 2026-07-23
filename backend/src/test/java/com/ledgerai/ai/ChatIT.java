package com.ledgerai.ai;

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
 * End-to-end tests for the AI Chat slice (SRS §4.8; API_SPEC §11; ADR-003/010/013) over real HTTP against a
 * real PostgreSQL, with the in-memory AI port standing in for Anthropic (the adapter is absent from the
 * test profile — the real provider is never contacted) and the in-memory OCR/storage ports standing in for
 * their providers. Covers the grounded happy path, the chronological thread (history), the READY/text/
 * question/ownership preconditions (409/422/404), provider-unavailable (503 + recorded failure), and
 * authentication. Skipped where no Docker runtime is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ChatIT {
    
    // A minimal but valid PNG signature; enough to pass the upload validator (magic + extension).
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
    void answersAGroundedQuestionAndRetainsTheThread() throws Exception {
        aiPort.succeedWith("The balance sheet total is 987654.");
        String token = registerAndGetToken("chat@example.com");
        String documentId = uploadReadyPdf(token, "Balance sheet total 987654");
        
        JsonNode answer = objectMapper.readTree(
            ask(token, documentId, "What is the balance sheet total?").getBody());
        assertThat(answer.at("/type").asText()).isEqualTo("CHAT");
        assertThat(answer.at("/status").asText()).isEqualTo("COMPLETED");
        assertThat(answer.at("/prompt").asText()).isEqualTo("What is the balance sheet total?");
        assertThat(answer.at("/content").asText()).contains("987654");
        
        // The prompt that reached the port was grounded in the document and carried the question.
        assertThat(aiPort.lastPrompt().groundedUserContent()).contains("Balance sheet total 987654");
        assertThat(aiPort.lastPrompt().groundedUserContent()).contains("What is the balance sheet total?");
        
        // A second question appends to the thread; GET returns both, oldest first (FR-CHAT-004).
        ask(token, documentId, "And the filename?");
        JsonNode thread = objectMapper.readTree(history(token, documentId).getBody());
        assertThat(thread.at("/totalElements").asInt()).isEqualTo(2);
        assertThat(thread.at("/content/0/prompt").asText()).isEqualTo("What is the balance sheet total?");
        assertThat(thread.at("/content/1/prompt").asText()).isEqualTo("And the filename?");
    }
    
    @Test
    void emptyQuestionIs422() throws Exception {
        String token = registerAndGetToken("blankq@example.com");
        String documentId = uploadReadyPdf(token, "Some ready text");
        
        assertThat(ask(token, documentId, "   ").getStatusCode())
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    @Test
    void chatIsOwnerScoped() throws Exception {
        aiPort.succeedWith("Alice's answer.");
        String aliceToken = registerAndGetToken("alice-chat@example.com");
        String documentId = uploadReadyPdf(aliceToken, "Confidential figures");
        ask(aliceToken, documentId, "What figures?");
        
        String bobToken = registerAndGetToken("bob-chat@example.com");
        // Non-owned document → 404, never distinguishing "unknown" from "not owned" (BR-004, SECURITY §5).
        assertThat(ask(bobToken, documentId, "What figures?").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(history(bobToken, documentId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void questionAgainstANonReadyDocumentIs409() throws Exception {
        // An image whose OCR provider is unavailable fails extraction → the document is FAILED, not READY.
        ocrPort.beUnavailable();
        String token = registerAndGetToken("notready-chat@example.com");
        String documentId = upload(token, createClient(token), PNG, "scan.png", "image/png");
        
        assertThat(ask(token, documentId, "Anything?").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
    
    @Test
    void providerUnavailableIs503AndRecordsAFailedAttempt() throws Exception {
        aiPort.beUnavailable();
        String token = registerAndGetToken("aidown-chat@example.com");
        String documentId = uploadReadyPdf(token, "Balance sheet total 5000");
        
        assertThat(ask(token, documentId, "Total?").getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        
        // The failed attempt is recorded and observable in the thread (SRS §7.2).
        JsonNode thread = objectMapper.readTree(history(token, documentId).getBody());
        assertThat(thread.at("/totalElements").asInt()).isEqualTo(1);
        assertThat(thread.at("/content/0/status").asText()).isEqualTo("FAILED");
        assertThat(thread.at("/content/0/failureReason").asText()).isNotBlank();
    }
    
    @Test
    void emptyThreadForAReadyDocumentIs200AndEmpty() throws Exception {
        String token = registerAndGetToken("emptythread@example.com");
        String documentId = uploadReadyPdf(token, "Some ready document text");
        
        JsonNode thread = objectMapper.readTree(history(token, documentId).getBody());
        assertThat(thread.at("/totalElements").asInt()).isZero();
        assertThat(thread.at("/content").isEmpty()).isTrue();
    }
    
    @Test
    void everyEndpointRequiresAuthentication() {
        String path = "/api/v1/documents/" + java.util.UUID.randomUUID() + "/chat";
        assertThat(restTemplate.exchange(path, HttpMethod.POST,
            json("{\"question\":\"x\"}", null), String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity(path, String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    // --- helpers -------------------------------------------------------------------------------
    
    private String uploadReadyPdf(String token, String text) throws Exception {
        String documentId = upload(token, createClient(token), pdfWithText(text), "statement.pdf",
            "application/pdf");
        assertThat(objectMapper.readTree(ocrStatus(token, documentId).getBody()).at("/status").asText())
            .isEqualTo("READY");
        return documentId;
    }
    
    private ResponseEntity<String> ask(String token, String documentId, String question) {
        return restTemplate.exchange("/api/v1/documents/" + documentId + "/chat", HttpMethod.POST,
            json("{\"question\":\"" + question + "\"}", token), String.class);
    }
    
    private ResponseEntity<String> history(String token, String documentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/v1/documents/" + documentId + "/chat",
            HttpMethod.GET, new HttpEntity<>(headers), String.class);
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
