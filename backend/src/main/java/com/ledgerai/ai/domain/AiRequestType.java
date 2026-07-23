package com.ledgerai.ai.domain;

/**
 * The AI Action a request represents (DATABASE §5.5). The documented type domain is
 * {@code SUMMARY | CHAT | EMAIL | REPORT}; the database {@code CHECK(type IN (...))} guards that full
 * set. Implemented so far: {@code SUMMARY} (AI Summary Generation) and {@code CHAT} (AI Chat) — both
 * persisted as {@code ai_request}/{@code ai_output} pairs (DATABASE §3.1 chat note, §5.5–5.6). Reports
 * persist as their own {@code report} rows (DATABASE §11), not via this type. {@code EMAIL} is documented
 * but not built yet.
 */
public enum AiRequestType {
    SUMMARY,
    CHAT
}
