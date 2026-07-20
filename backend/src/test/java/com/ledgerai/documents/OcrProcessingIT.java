package com.ledgerai.documents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerai.documents.domain.ExtractionQuality;
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
 * End-to-end tests for the OCR Processing slice (SRS §4.6, §7.1; ADR-009) over real HTTP against a real
 * PostgreSQL, with the in-memory OCR port standing in for Google Vision (the adapter is absent from the
 * test profile). Covers the native-first path, the OCR fallback, the FAILED paths, and the OCR-status
 * poll endpoint including ownership. Real text PDFs are generated in-test so native extraction runs for
 * real. Skipped where no Docker runtime is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class OcrProcessingIT {
    
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
    void resetOcr() {
        // Shared singleton across @SpringBootTest classes — restore the default so other tests are not
        // affected by a mode set here.
        ocrPort.reset();
    }
    
    @Test
    void nativeFirstReachesReadyWithoutInvokingOcr() throws Exception {
        // A PDF that already contains selectable text → native path → READY(NATIVE, HIGH), OCR skipped.
        ocrPort.beUnavailable(); // prove OCR is NOT called: if it were, this would fail the document.
        String token = registerAndGetToken("native@example.com");
        String clientId = createClient(token);
        
        String documentId = upload(token, clientId, pdfWithText("Balance sheet total 987654"), "statement.pdf",
            "application/pdf");
        
        JsonNode status = objectMapper.readTree(ocrStatus(token, documentId).getBody());
        assertThat(status.at("/status").asText()).isEqualTo("READY");
        assertThat(status.at("/extractionMethod").asText()).isEqualTo("NATIVE");
        assertThat(status.at("/extractionQuality").asText()).isEqualTo("HIGH");
        assertThat(status.at("/failureReason").isNull()).isTrue();
    }
    
    @Test
    void imageFallsBackToOcrAndReachesReady() throws Exception {
        ocrPort.succeedWith("Scanned invoice text", ExtractionQuality.LOW);
        String token = registerAndGetToken("ocr-success@example.com");
        String clientId = createClient(token);
        
        String documentId = upload(token, clientId, PNG, "scan.png", "image/png");
        
        JsonNode status = objectMapper.readTree(ocrStatus(token, documentId).getBody());
        assertThat(status.at("/status").asText()).isEqualTo("READY");
        assertThat(status.at("/extractionMethod").asText()).isEqualTo("OCR");
        assertThat(status.at("/extractionQuality").asText()).isEqualTo("LOW");
    }
    
    @Test
    void ocrProviderUnavailableTransitionsToFailed() throws Exception {
        ocrPort.beUnavailable();
        String token = registerAndGetToken("ocr-down@example.com");
        String clientId = createClient(token);
        
        String documentId = upload(token, clientId, PNG, "scan.png", "image/png");
        
        JsonNode status = objectMapper.readTree(ocrStatus(token, documentId).getBody());
        assertThat(status.at("/status").asText()).isEqualTo("FAILED");
        assertThat(status.at("/failureReason").asText()).isNotBlank();
    }
    
    @Test
    void ocrProducingNoTextTransitionsToFailed() throws Exception {
        ocrPort.returnEmpty();
        String token = registerAndGetToken("ocr-blank@example.com");
        String clientId = createClient(token);
        
        String documentId = upload(token, clientId, PNG, "scan.png", "image/png");
        
        assertThat(objectMapper.readTree(ocrStatus(token, documentId).getBody()).at("/status").asText())
            .isEqualTo("FAILED");
    }
    
    @Test
    void ocrStatusIsOwnerScoped() throws Exception {
        String aliceToken = registerAndGetToken("alice-ocr@example.com");
        String aliceClient = createClient(aliceToken);
        String documentId = upload(aliceToken, aliceClient, PNG, "scan.png", "image/png");
        
        String bobToken = registerAndGetToken("bob-ocr@example.com");
        // Non-owned document → 404, never distinguishing "unknown" from "not owned" (BR-004, SECURITY §5).
        assertThat(ocrStatus(bobToken, documentId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    // --- helpers -------------------------------------------------------------------------------
    
    @Test
    void ocrStatusRequiresAuthentication() {
        assertThat(restTemplate.getForEntity(
            "/api/v1/documents/" + java.util.UUID.randomUUID() + "/ocr-status", String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
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
        // The upload response reports the initial status (API_SPEC §8.1).
        assertThat(objectMapper.readTree(response.getBody()).at("/status").asText()).isEqualTo("UPLOADED");
        return objectMapper.readTree(response.getBody()).at("/id").asText();
    }
    
    private ResponseEntity<String> ocrStatus(String token, String documentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/v1/documents/" + documentId + "/ocr-status",
            HttpMethod.GET, new HttpEntity<>(headers), String.class);
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
