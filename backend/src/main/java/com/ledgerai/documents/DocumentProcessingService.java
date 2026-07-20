package com.ledgerai.documents;

import com.ledgerai.documents.config.DocumentProperties;
import com.ledgerai.documents.domain.ExtractionMethod;
import com.ledgerai.documents.domain.ExtractionQuality;
import com.ledgerai.documents.port.OcrPort;
import com.ledgerai.documents.port.OcrResult;
import com.ledgerai.documents.port.OcrUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orchestrates document text extraction — the OCR Processing pipeline (SRS §4.6, §7.1; ADR-009).
 *
 * <p>Runs the documented state machine and the native-first strategy exactly:
 * <pre>
 *   UPLOADED → PROCESSING
 *     PDF with sufficient embedded text → READY (extraction_method = NATIVE)   [OCR skipped]
 *     otherwise → OCR_PROCESSING → OCR provider
 *         text produced → READY (extraction_method = OCR)
 *         no text / provider unavailable → FAILED
 * </pre>
 *
 * <p>Processing is <strong>synchronous-with-status</strong> (ADR-013): it runs inline (no worker,
 * queue, or scheduler), and status is observable via the OCR-status endpoint. The OCR provider is
 * reached only through the {@link OcrPort}; native extraction is in-process (PDFBox). DB writes are
 * delegated to {@link DocumentLifecycleWriter} so the external OCR call is never inside a transaction
 * (DATABASE §11). It <strong>never throws</strong>: any extraction problem becomes a {@code FAILED}
 * document with a clear reason (FR-OCR-005), so the upload flow that triggers it always completes.
 */
@Service
public class DocumentProcessingService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);
    
    private final DocumentLifecycleWriter lifecycleWriter;
    private final NativeTextExtractor nativeTextExtractor;
    private final OcrPort ocrPort;
    private final DocumentProperties properties;
    
    public DocumentProcessingService(DocumentLifecycleWriter lifecycleWriter,
                                     NativeTextExtractor nativeTextExtractor, OcrPort ocrPort,
                                     DocumentProperties properties) {
        this.lifecycleWriter = lifecycleWriter;
        this.nativeTextExtractor = nativeTextExtractor;
        this.ocrPort = ocrPort;
        this.properties = properties;
    }
    
    /**
     * Extracts text for a freshly uploaded document, driving it to {@code READY} or {@code FAILED}.
     * Invoked synchronously by the upload flow with the in-memory bytes (the same bytes just stored).
     */
    public void process(UUID documentId, byte[] content, String mimeType) {
        lifecycleWriter.markProcessing(documentId);
        try {
            extract(documentId, content, mimeType);
        } catch (OcrUnavailableException e) {
            // Provider unreachable/error — retryable, surfaced, never hidden (FR-OCR-005).
            lifecycleWriter.markFailed(documentId, e.getMessage());
        } catch (ExtractionFailedException e) {
            lifecycleWriter.markFailed(documentId, e.getMessage());
        } catch (RuntimeException e) {
            // Graceful degradation: an unexpected fault fails the document safely rather than the request.
            log.error("Unexpected error while processing document {}", documentId, e);
            lifecycleWriter.markFailed(documentId, "The document could not be processed.");
        }
    }
    
    private void extract(UUID documentId, byte[] content, String mimeType) {
        if (isPdf(mimeType)) {
            String nativeText = nativeTextExtractor.extractText(content);
            if (nativeText != null && nativeText.strip().length() >= properties.nativeMinChars()) {
                // Native-first: the PDF already has usable text; OCR is not invoked (ADR-009, FR-OCR-002).
                lifecycleWriter.completeReady(documentId, ExtractionMethod.NATIVE, nativeText, ExtractionQuality.HIGH);
                return;
            }
        }
        
        // Scan/image (or a PDF without embedded text): route to the OCR provider.
        lifecycleWriter.markOcrProcessing(documentId);
        OcrResult result = ocrPort.extract(content, mimeType);
        if (result.text() == null || result.text().isBlank()) {
            throw new ExtractionFailedException("No readable text could be extracted from the document.");
        }
        lifecycleWriter.completeReady(documentId, ExtractionMethod.OCR, result.text(), result.quality());
    }
    
    private boolean isPdf(String mimeType) {
        return "application/pdf".equals(mimeType);
    }
    
    /**
     * Internal signal that extraction produced no usable text (not a provider outage).
     */
    private static final class ExtractionFailedException extends RuntimeException {
        ExtractionFailedException(String message) {
            super(message);
        }
    }
}
