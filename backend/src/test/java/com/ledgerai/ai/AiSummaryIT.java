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
 * End-to-end tests for the AI Summary slice (SRS §4.7; API_SPEC §10; ADR-003/010/013) over real HTTP
 * against a real PostgreSQL, with the in-memory AI port standing in for Anthropic (the adapter is absent
 * from the test profile — the real provider is never contacted) and the in-memory OCR/storage ports
 * standing in for their providers. Covers the grounded happy path, edit, the READY/text/ownership
 * preconditions (409/422/404), provider-unavailable (503 + recorded failure), and authentication.
 * Skipped where no Docker runtime is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AiSummaryIT {
    
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
        // Shared singletons across @SpringBootTest classes — restore defaults so other tests are not
        // affected by modes set here.
        aiPort.reset();
        ocrPort.reset();
    }
    
    @Test
    void generatesAGroundedSummaryForAReadyDocument() throws Exception {
        aiPort.succeedWith("This statement reports a balance sheet total of 987654.");
        String token = registerAndGetToken("summary@example.com");
        String documentId = uploadReadyPdf(token, "Balance sheet total 987654");
        
        JsonNode created = objectMapper.readTree(generate(token, documentId).getBody());
        assertThat(created.at("/type").asText()).isEqualTo("SUMMARY");
        assertThat(created.at("/status").asText()).isEqualTo("COMPLETED");
        assertThat(created.at("/content").asText()).contains("987654");
        assertThat(created.at("/edited").asBoolean()).isFalse();
        
        // The prompt that reached the port was grounded in the extracted document text.
        assertThat(aiPort.lastPrompt().groundedUserContent()).contains("Balance sheet total 987654");
        
        // GET returns the saved summary (also the async poll target).
        JsonNode fetched = objectMapper.readTree(getSummary(token, documentId).getBody());
        assertThat(fetched.at("/status").asText()).isEqualTo("COMPLETED");
        assertThat(fetched.at("/content").asText()).contains("987654");
    }
    
    @Test
    void persistsAUserEditToTheSummary() throws Exception {
        aiPort.succeedWith("Original AI summary.");
        String token = registerAndGetToken("edit@example.com");
        String documentId = uploadReadyPdf(token, "Invoice total 4200");
        generate(token, documentId);
        
        ResponseEntity<String> edited = restTemplate.exchange(
            "/api/v1/documents/" + documentId + "/summary", HttpMethod.PATCH,
            json("{\"content\":\"My edited summary.\"}", token), String.class);
        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(edited.getBody());
        assertThat(body.at("/content").asText()).isEqualTo("My edited summary.");
        assertThat(body.at("/edited").asBoolean()).isTrue();
    }
    
    @Test
    void summaryIsOwnerScoped() throws Exception {
        aiPort.succeedWith("Alice's summary.");
        String aliceToken = registerAndGetToken("alice-sum@example.com");
        String documentId = uploadReadyPdf(aliceToken, "Confidential figures");
        generate(aliceToken, documentId);
        
        String bobToken = registerAndGetToken("bob-sum@example.com");
        // Non-owned document → 404, never distinguishing "unknown" from "not owned" (BR-004, SECURITY §5).
        assertThat(getSummary(bobToken, documentId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(generate(bobToken, documentId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void generateForANonReadyDocumentIs409() throws Exception {
        // An image whose OCR provider is unavailable fails extraction → the document is FAILED, not READY.
        ocrPort.beUnavailable();
        String token = registerAndGetToken("notready@example.com");
        String documentId = upload(token, createClient(token), PNG, "scan.png", "image/png");
        
        assertThat(generate(token, documentId).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
    
    @Test
    void providerUnavailableIs503AndRecordsAFailedAttempt() throws Exception {
        aiPort.beUnavailable();
        String token = registerAndGetToken("aidown@example.com");
        String documentId = uploadReadyPdf(token, "Balance sheet total 5000");
        
        assertThat(generate(token, documentId).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        
        // The failed attempt is recorded and observable via the poll target (SRS §7.2).
        JsonNode fetched = objectMapper.readTree(getSummary(token, documentId).getBody());
        assertThat(fetched.at("/status").asText()).isEqualTo("FAILED");
        assertThat(fetched.at("/failureReason").asText()).isNotBlank();
    }
    
    @Test
    void getWithoutASummaryIs404() throws Exception {
        String token = registerAndGetToken("nosummary@example.com");
        String documentId = uploadReadyPdf(token, "Some ready document text");
        
        assertThat(getSummary(token, documentId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void everyEndpointRequiresAuthentication() {
        String path = "/api/v1/documents/" + java.util.UUID.randomUUID() + "/summary";
        assertThat(restTemplate.postForEntity(path, HttpEntity.EMPTY, String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.getForEntity(path, String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(restTemplate.exchange(path, HttpMethod.PATCH,
            json("{\"content\":\"x\"}", null), String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    // --- helpers -------------------------------------------------------------------------------
    
    private String uploadReadyPdf(String token, String text) throws Exception {
        // A PDF with embedded text reaches READY via native extraction (no OCR needed).
        String documentId = upload(token, createClient(token), pdfWithText(text), "statement.pdf",
            "application/pdf");
        assertThat(objectMapper.readTree(ocrStatus(token, documentId).getBody()).at("/status").asText())
            .isEqualTo("READY");
        return documentId;
    }
    
    private ResponseEntity<String> generate(String token, String documentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/v1/documents/" + documentId + "/summary",
            HttpMethod.POST, new HttpEntity<>(headers), String.class);
    }
    
    private ResponseEntity<String> getSummary(String token, String documentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/v1/documents/" + documentId + "/summary",
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
