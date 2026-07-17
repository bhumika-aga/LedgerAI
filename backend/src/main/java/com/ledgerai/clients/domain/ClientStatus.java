package com.ledgerai.clients.domain;

/**
 * The Client lifecycle states (DATABASE §5.2: {@code CHECK(status IN ('ACTIVE','ARCHIVED'))}).
 *
 * <p>A defined enum rather than free strings, so the constrained set lives in one place and the
 * database check constraint and the API filter can never drift apart (CLAUDE.md §10 — no magic
 * strings). {@code ARCHIVED} is LedgerAI's soft delete for a Client: archiving retains the client's
 * documents (BR-002, DATABASE §8) — it is archival, not deletion.
 */
public enum ClientStatus {
    ACTIVE,
    ARCHIVED
}
