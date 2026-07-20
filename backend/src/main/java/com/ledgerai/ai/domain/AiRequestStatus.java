package com.ledgerai.ai.domain;

/**
 * The AI Request lifecycle states (SRS §7.2, ADR-010, DATABASE §5.5):
 * {@code REQUESTED → IN_PROGRESS → COMPLETED | FAILED}. Exactly the documented set; the database
 * {@code CHECK(status IN (...))} guards the same values. {@code COMPLETED} carries an editable
 * {@code AIOutput}; {@code FAILED} carries a {@code failure_reason} and no output.
 */
public enum AiRequestStatus {
    REQUESTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
