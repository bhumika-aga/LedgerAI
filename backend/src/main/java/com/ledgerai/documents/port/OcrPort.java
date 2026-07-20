package com.ledgerai.documents.port;

/**
 * The domain-owned OCR port (ADR-009, ARCHITECTURE §10). Business logic depends only on this
 * interface, expressed in domain terms — "extract text from these bytes, and tell me how confident you
 * are". The concrete provider (Google Cloud Vision, ADR-009) is an adapter selected by configuration;
 * no provider type ever crosses this boundary.
 *
 * <p>Native (embedded-text) extraction happens on the domain side <em>before</em> this port is
 * consulted (native-first, ADR-009); the port is invoked only for scans/images or PDFs without usable
 * embedded text. Provider failures are translated by the adapter into {@link OcrUnavailableException}
 * so the extraction pipeline can transition the document to {@code FAILED} (FR-OCR-005).
 */
public interface OcrPort {
    
    /**
     * Extracts text from the given content.
     *
     * @param content  the raw file bytes
     * @param mimeType the detected content type (e.g. {@code image/png})
     * @return the extracted text plus a quality signal; the text may be blank if nothing was readable
     * @throws OcrUnavailableException if the provider could not be reached or returned an error
     */
    OcrResult extract(byte[] content, String mimeType);
}
