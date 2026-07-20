package com.ledgerai.documents.adapter;

import com.ledgerai.documents.domain.ExtractionQuality;
import com.ledgerai.documents.port.OcrResult;
import com.ledgerai.documents.port.OcrUnavailableException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link GoogleVisionOcrAdapter} against a mocked HTTP endpoint. They pin the provider
 * contract the adapter speaks — the {@code images:annotate} call with the API key, mapping the full
 * text + page confidence into the domain {@link OcrResult}/{@link ExtractionQuality}, and turning any
 * provider failure into {@link OcrUnavailableException}. No live Vision is involved; provider details
 * never leave this class.
 */
class GoogleVisionOcrAdapterTest {
    
    private static final String BASE = "https://vision.googleapis.com";
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47};
    
    private MockRestServiceServer server;
    private GoogleVisionOcrAdapter adapter;
    
    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new GoogleVisionOcrAdapter(builder, new GoogleVisionProperties(BASE, "vision-key", 0.8));
    }
    
    @Test
    void extractsTextAndMapsHighConfidence() {
        server.expect(requestTo(Matchers.startsWith(BASE + "/v1/images:annotate")))
            .andExpect(method(HttpMethod.POST))
            .andExpect(queryParam("key", "vision-key"))
            .andRespond(withSuccess(
                "{\"responses\":[{\"fullTextAnnotation\":{\"text\":\"Invoice 42\",\"pages\":[{\"confidence\":0.95}]}}]}",
                MediaType.APPLICATION_JSON));
        
        OcrResult result = adapter.extract(PNG, "image/png");
        
        assertThat(result.text()).isEqualTo("Invoice 42");
        assertThat(result.quality()).isEqualTo(ExtractionQuality.HIGH);
        server.verify();
    }
    
    @Test
    void mapsLowConfidenceToLowQuality() {
        server.expect(requestTo(Matchers.startsWith(BASE + "/v1/images:annotate")))
            .andRespond(withSuccess(
                "{\"responses\":[{\"fullTextAnnotation\":{\"text\":\"blurry\",\"pages\":[{\"confidence\":0.4}]}}]}",
                MediaType.APPLICATION_JSON));
        
        assertThat(adapter.extract(PNG, "image/png").quality()).isEqualTo(ExtractionQuality.LOW);
    }
    
    @Test
    void returnsEmptyTextWhenNothingWasDetected() {
        server.expect(requestTo(Matchers.startsWith(BASE + "/v1/images:annotate")))
            .andRespond(withSuccess("{\"responses\":[{}]}", MediaType.APPLICATION_JSON));
        
        OcrResult result = adapter.extract(PNG, "image/png");
        
        assertThat(result.text()).isEmpty();
        assertThat(result.quality()).isEqualTo(ExtractionQuality.UNKNOWN);
    }
    
    @Test
    void mapsProviderFailureToOcrUnavailable() {
        server.expect(requestTo(Matchers.startsWith(BASE + "/v1/images:annotate")))
            .andRespond(withServerError());
        
        assertThatThrownBy(() -> adapter.extract(PNG, "image/png"))
            .isInstanceOf(OcrUnavailableException.class);
    }
}
