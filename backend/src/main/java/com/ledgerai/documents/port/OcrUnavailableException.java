package com.ledgerai.documents.port;

import com.ledgerai.common.exception.LedgerAiException;

/**
 * The OCR provider could not service an extraction request (unreachable, timeout, or provider error).
 *
 * <p>Unlike storage failures, this never becomes an HTTP status: OCR runs during processing, so the
 * extraction pipeline catches it and transitions the document to {@code FAILED} with a retryable,
 * non-technical reason (FR-OCR-005, AI_ARCHITECTURE graceful-degradation). The underlying provider
 * error is logged inside the adapter, never leaked.
 */
public class OcrUnavailableException extends LedgerAiException {
    
    public OcrUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
