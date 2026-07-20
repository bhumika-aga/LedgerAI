package com.ledgerai.documents.port;

import com.ledgerai.documents.domain.ExtractionQuality;

/**
 * The outcome of an OCR extraction in domain terms (ADR-009): the extracted text and the derived
 * quality signal. Provider-specific confidence scores are mapped to {@link ExtractionQuality} inside
 * the adapter, so no provider detail crosses the port. A blank {@code text} means the provider found
 * nothing readable — the pipeline treats that as a failed extraction.
 */
public record OcrResult(String text, ExtractionQuality quality) {
}
