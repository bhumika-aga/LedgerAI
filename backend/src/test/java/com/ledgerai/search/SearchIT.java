package com.ledgerai.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * End-to-end tests for the Search slice (SRS §4.11; API_SPEC §14) over real HTTP against a real PostgreSQL.
 * Uploads real text PDFs (extracted natively so {@code document_content} is populated), then searches over
 * that content. Verifies owner scoping, the empty-result case, the {@code 422} on an invalid query, and
 * authentication. The in-memory OCR/storage ports stand in for their providers. Skipped where no Docker
 * runtime is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SearchIT {
    
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
    void resetPorts() {
        ocrPort.reset();
    }
    
    @Test
    void findsAnUploadedDocumentByItsContent() throws Exception {
        String token = registerAndGetToken("search@example.com");
        String documentId = uploadReadyPdf(token, "Balance sheet total 987654");
        
        JsonNode page = objectMapper.readTree(search(token, "balance").getBody());
        assertThat(page.at("/content").size()).isEqualTo(1);
        assertThat(page.at("/content/0/documentId").asText()).isEqualTo(documentId);
        assertThat(page.at("/content/0/title").asText()).isEqualTo("statement.pdf");
        assertThat(page.at("/content/0/matchContext").asText()).isNotBlank();
    }
    
    @Test
    void returnsAnEmptyPageWhenNothingMatches() throws Exception {
        String token = registerAndGetToken("nomatch@example.com");
        uploadReadyPdf(token, "Balance sheet total 987654");
        
        JsonNode page = objectMapper.readTree(search(token, "helicopter").getBody());
        assertThat(page.at("/content").size()).isZero();
        assertThat(page.at("/totalElements").asInt()).isZero();
    }
    
    @Test
    void searchIsOwnerScoped() throws Exception {
        String aliceToken = registerAndGetToken("alice-search@example.com");
        uploadReadyPdf(aliceToken, "Confidential balance figures");
        
        String bobToken = registerAndGetToken("bob-search@example.com");
        // Bob searches the same keyword and sees none of Alice's documents (BR-004/006, SECURITY §5).
        assertThat(objectMapper.readTree(search(bobToken, "balance").getBody()).at("/content").size()).isZero();
    }
    
    @Test
    void aBlankQueryIs422() {
        String token = registerAndGetToken("blank-search@example.com");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        assertThat(restTemplate.exchange("/api/v1/search?q=%20", HttpMethod.GET,
            new HttpEntity<>(headers), String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    @Test
    void searchRequiresAuthentication() {
        assertThat(restTemplate.getForEntity("/api/v1/search?q=balance", String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    // --- helpers -------------------------------------------------------------------------------
    
    private ResponseEntity<String> search(String token, String q) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange("/api/v1/search?q=" + q, HttpMethod.GET,
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
    
    private String registerAndGetToken(String email) {
        String payload = "{\"email\":\"%s\",\"password\":\"correct-horse\",\"fullName\":\"Pro\"}".formatted(email);
        ResponseEntity<String> response =
            restTemplate.postForEntity("/api/v1/auth/register", json(payload, null), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        try {
            return objectMapper.readTree(response.getBody()).at("/tokens/accessToken").asText();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
