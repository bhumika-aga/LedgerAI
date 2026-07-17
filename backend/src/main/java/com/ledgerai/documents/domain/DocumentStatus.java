package com.ledgerai.documents.domain;

/**
 * The Document lifecycle states (DATABASE §5.3, SRS §7.1). Exactly the documented set; the database
 * {@code CHECK(status IN (...))} guards the same values. This slice only ever assigns {@code UPLOADED}
 * (on create) and {@code DELETED} (on soft-delete); the intermediate processing states are driven by
 * the OCR pipeline, which is out of scope here.
 */
public enum DocumentStatus {
    UPLOADED,
    PROCESSING,
    OCR_PROCESSING,
    READY,
    FAILED,
    DELETED
}
