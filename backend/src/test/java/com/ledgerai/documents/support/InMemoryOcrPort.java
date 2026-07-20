package com.ledgerai.documents.support;

import com.ledgerai.documents.domain.ExtractionQuality;
import com.ledgerai.documents.port.OcrPort;
import com.ledgerai.documents.port.OcrResult;
import com.ledgerai.documents.port.OcrUnavailableException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Test-profile {@link OcrPort} — the in-memory stand-in for the Google Vision adapter, which is absent
 * from the {@code test} profile (ARCHITECTURE §10 — the active adapter is chosen by configuration).
 *
 * <p>Deterministic and controllable: an end-to-end test autowires this bean and sets the next outcome
 * before uploading, so the OCR success/empty/unavailable paths can be exercised without a live
 * provider. It is test infrastructure, not a production adapter.
 */
@Component
@Profile("test")
public class InMemoryOcrPort implements OcrPort {
    
    private volatile Mode mode = Mode.SUCCESS;
    private volatile String text = "OCR extracted text";
    private volatile ExtractionQuality quality = ExtractionQuality.HIGH;
    
    @Override
    public OcrResult extract(byte[] content, String mimeType) {
        return switch (mode) {
            case UNAVAILABLE -> throw new OcrUnavailableException("OCR provider unavailable (test).", null);
            case EMPTY -> new OcrResult("", ExtractionQuality.UNKNOWN);
            case SUCCESS -> new OcrResult(text, quality);
        };
    }
    
    public void succeedWith(String text, ExtractionQuality quality) {
        this.mode = Mode.SUCCESS;
        this.text = text;
        this.quality = quality;
    }
    
    public void returnEmpty() {
        this.mode = Mode.EMPTY;
    }
    
    public void beUnavailable() {
        this.mode = Mode.UNAVAILABLE;
    }
    
    public void reset() {
        this.mode = Mode.SUCCESS;
        this.text = "OCR extracted text";
        this.quality = ExtractionQuality.HIGH;
    }
    
    public enum Mode {SUCCESS, EMPTY, UNAVAILABLE}
}
