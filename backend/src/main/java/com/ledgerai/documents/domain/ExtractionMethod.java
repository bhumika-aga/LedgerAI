package com.ledgerai.documents.domain;

/**
 * How a document's text was obtained (DATABASE §5.3, BR-014). Determined by the OCR/extraction
 * pipeline (out of scope for this slice), so it remains null on a freshly uploaded document.
 */
public enum ExtractionMethod {
    NATIVE,
    OCR
}
