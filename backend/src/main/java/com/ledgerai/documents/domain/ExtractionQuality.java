package com.ledgerai.documents.domain;

/**
 * The confidence signal for a document's extracted text (DATABASE §5.4, FR-OCR-006).
 *
 * <p>{@code HIGH}/{@code LOW} express how much to trust the extraction so the user can judge
 * reliability; {@code UNKNOWN} is used when no confidence signal is available. Native (embedded-text)
 * extraction is treated as {@code HIGH} — it is the document's own digital text, not a guess; OCR
 * quality is derived by the adapter from the provider's confidence score (ADR-009).
 */
public enum ExtractionQuality {
    HIGH,
    LOW,
    UNKNOWN
}
