package com.ledgerai.documents.dto;

import com.ledgerai.documents.domain.*;

import java.util.UUID;

/**
 * OCR status projection (API_SPEC §9.1):
 * {@code { documentId, status, extractionMethod?, extractionQuality?, failureReason? }}.
 *
 * <p>The poll endpoint for upload/processing (§2.11). {@code status} is the document lifecycle state
 * (SRS §7.1); {@code extractionMethod}/{@code failureReason} come from the document; the quality signal
 * comes from the extracted content (present only once extraction has succeeded). It deliberately does
 * <strong>not</strong> include the extracted text — no documented endpoint exposes that.
 */
public record OcrStatusResponse(
    UUID documentId,
    DocumentStatus status,
    ExtractionMethod extractionMethod,
    ExtractionQuality extractionQuality,
    String failureReason) {
    
    public static OcrStatusResponse of(Document document, DocumentContent content) {
        return new OcrStatusResponse(
            document.getId(),
            document.getStatus(),
            document.getExtractionMethod(),
            content == null ? null : content.getExtractionQuality(),
            document.getFailureReason());
    }
}
