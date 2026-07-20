package com.ledgerai.documents.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ledgerai.documents.domain.ExtractionQuality;
import com.ledgerai.documents.port.OcrPort;
import com.ledgerai.documents.port.OcrResult;
import com.ledgerai.documents.port.OcrUnavailableException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Google Cloud Vision adapter (ADR-009) implementing the domain {@link OcrPort}.
 *
 * <p>This is the <strong>only</strong> place that knows the provider: the Vision
 * {@code images:annotate} endpoint, its request/response JSON, and the API key live here, and no
 * provider type crosses the port — callers see only {@link OcrResult} / {@link ExtractionQuality} and
 * {@link OcrUnavailableException}. The active adapter is chosen by configuration (ARCHITECTURE §10);
 * this one is absent from the {@code test} profile, where an in-memory port stands in.
 *
 * <p>Uses {@code DOCUMENT_TEXT_DETECTION} and maps the returned full text + page confidence into the
 * domain quality signal. Only the file bytes are sent — no account/client metadata (minimum-necessary,
 * SECURITY §10, NFR-018). Any provider/transport failure becomes {@link OcrUnavailableException}.
 */
@Component
@Profile("!test")
@EnableConfigurationProperties(GoogleVisionProperties.class)
public class GoogleVisionOcrAdapter implements OcrPort {
    
    private final RestClient restClient;
    private final GoogleVisionProperties properties;
    
    public GoogleVisionOcrAdapter(RestClient.Builder builder, GoogleVisionProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.apiUrl()).build();
    }
    
    @Override
    public OcrResult extract(byte[] content, String mimeType) {
        // Vision reads the raw bytes as base64; the RestClient encodes the body as JSON.
        String base64 = java.util.Base64.getEncoder().encodeToString(content);
        Map<String, Object> request = Map.of("requests", List.of(Map.of(
            "image", Map.of("content", base64),
            "features", List.of(Map.of("type", "DOCUMENT_TEXT_DETECTION")))));
        try {
            VisionResponse response = restClient.post()
                                          .uri(uriBuilder -> uriBuilder.path("/v1/images:annotate").queryParam("key", properties.apiKey()).build())
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .body(request)
                                          .retrieve()
                                          .body(VisionResponse.class);
            return toResult(response);
        } catch (RestClientException e) {
            throw new OcrUnavailableException("The document text could not be extracted. Please try again.", e);
        }
    }
    
    private OcrResult toResult(VisionResponse response) {
        if (response == null || response.responses() == null || response.responses().isEmpty()) {
            return new OcrResult("", ExtractionQuality.UNKNOWN);
        }
        AnnotateResponse first = response.responses().getFirst();
        if (first == null || first.fullTextAnnotation() == null) {
            return new OcrResult("", ExtractionQuality.UNKNOWN);
        }
        String text = first.fullTextAnnotation().text();
        return new OcrResult(text == null ? "" : text, quality(first.fullTextAnnotation()));
    }
    
    private ExtractionQuality quality(FullTextAnnotation annotation) {
        if (annotation.pages() == null || annotation.pages().isEmpty()) {
            return ExtractionQuality.UNKNOWN;
        }
        double confidence = annotation.pages().stream()
                                .filter(Objects::nonNull)
                                .mapToDouble(Page::confidence)
                                .average()
                                .orElse(0.0);
        if (confidence <= 0.0) {
            return ExtractionQuality.UNKNOWN;
        }
        return confidence >= properties.highConfidenceThreshold() ? ExtractionQuality.HIGH : ExtractionQuality.LOW;
    }
    
    // --- Minimal Vision response shapes (provider detail confined to this adapter) -----------------
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VisionResponse(List<AnnotateResponse> responses) {
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnnotateResponse(FullTextAnnotation fullTextAnnotation) {
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FullTextAnnotation(String text, List<Page> pages) {
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Page(double confidence) {
    }
}
