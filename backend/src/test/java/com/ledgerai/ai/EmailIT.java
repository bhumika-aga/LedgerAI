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
 * End-to-end tests for the AI Email slice (SRS §4.9; API_SPEC §12; ADR-003/010/013) over real HTTP against
 * a real PostgreSQL, with the in-memory AI port standing in for Anthropic (the adapter is absent from the
 * test profile — the real provider is never contacted) and the in-memory OCR/storage ports standing in for
 * their providers. Covers the instruction-only happy path, the client+document-context path (grounded
 * prompt), the ownership/readiness/instruction preconditions (404/409/422), provider-unavailable (503), and
 * authentication. Skipped where no Docker runtime is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class EmailIT {
    
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
    void draftsAnEmailFromAnInstructionAloneWithNoContext() throws Exception {
        aiPort.succeedWith("Dear client, this is a follow-up regarding your account.");
        String token = registerAndGetToken("email@example.com");
        
        JsonNode draft = objectMapper.readTree(generate(token, "{\"instruction\":\"Write a follow-up.\"}").getBody());
        assertThat(draft.at("/type").asText()).isEqualTo("EMAIL");
        assertThat(draft.at("/status").asText()).isEqualTo("COMPLETED");
        assertThat(draft.at("/documentId").isNull()).isTrue();
        assertThat(draft.at("/content").asText()).contains("Dear client");
        assertThat(draft.at("/edited").asBoolean()).isFalse();
        
        // The instruction reached the port in its own channel; no client/document context was included.
        assertThat(aiPort.lastPrompt().groundedUserContent()).contains("Write a follow-up.");
    }
    
    @Test
    void groundsTheDraftInClientAndReadyDocumentContext() throws Exception {
        aiPort.succeedWith("Dear Acme Corp, regarding invoice total 4200 ...");
        String token = registerAndGetToken("emailctx@example.com");
        String clientId = createClient(token);
        String documentId = uploadReadyPdf(token, clientId, "Invoice total 4200");
        
        String body = "{\"instruction\":\"Chase the invoice.\",\"clientId\":\"" + clientId
                          + "\",\"documentId\":\"" + documentId + "\"}";
        JsonNode draft = objectMapper.readTree(generate(token, body).getBody());
        assertThat(draft.at("/status").asText()).isEqualTo("COMPLETED");
        assertThat(draft.at("/documentId").asText()).isEqualTo(documentId);
        
        // Both context channels reached the port: the client name and the grounded document text.
        assertThat(aiPort.lastPrompt().groundedUserContent()).contains("Acme Corp");
        assertThat(aiPort.lastPrompt().groundedUserContent()).contains("Invoice total 4200");
    }
    
    @Test
    void emptyInstructionIs422() throws Exception {
        String token = registerAndGetToken("blankinstr@example.com");
        
        assertThat(generate(token, "{\"instruction\":\"   \"}").getStatusCode())
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    @Test
    void nonOwnedClientContextIs404() throws Exception {
        String aliceToken = registerAndGetToken("alice-email@example.com");
        String aliceClient = createClient(aliceToken);
        
        String bobToken = registerAndGetToken("bob-email@example.com");
        String body = "{\"instruction\":\"Draft it.\",\"clientId\":\"" + aliceClient + "\"}";
        // A client the caller does not own → 404, never distinguishing "unknown" from "not owned".
        assertThat(generate(bobToken, body).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void nonOwnedDocumentContextIs404() throws Exception {
        String aliceToken = registerAndGetToken("alice-emaildoc@example.com");
        String aliceDoc = uploadReadyPdf(aliceToken, createClient(aliceToken), "Confidential figures");
        
        String bobToken = registerAndGetToken("bob-emaildoc@example.com");
        String body = "{\"instruction\":\"Draft it.\",\"documentId\":\"" + aliceDoc + "\"}";
        assertThat(generate(bobToken, body).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void nonReadyDocumentContextIs409() throws Exception {
        ocrPort.beUnavailable();
        String token = registerAndGetToken("notready-email@example.com");
        String documentId = upload(token, createClient(token), PNG, "scan.png", "image/png");
        
        String body = "{\"instruction\":\"Draft it.\",\"documentId\":\"" + documentId + "\"}";
        assertThat(generate(token, body).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
    
    @Test
    void providerUnavailableIs503() throws Exception {
        aiPort.beUnavailable();
        String token = registerAndGetToken("aidown-email@example.com");
        
        assertThat(generate(token, "{\"instruction\":\"Draft it.\"}").getStatusCode())
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    @Test
    void generateRequiresAuthentication() {
        assertThat(restTemplate.exchange("/api/v1/ai/emails", HttpMethod.POST,
            json("{\"instruction\":\"Draft it.\"}", null), String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    // --- helpers -------------------------------------------------------------------------------
    
    private ResponseEntity<String> generate(String token, String body) {
        return restTemplate.exchange("/api/v1/ai/emails", HttpMethod.POST, json(body, token), String.class);
    }
    
    private String uploadReadyPdf(String token, String clientId, String text) throws Exception {
        String documentId = upload(token, clientId, pdfWithText(text), "statement.pdf", "application/pdf");
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
