package com.ledgerai.ai.domain;

/**
 * The AI Action a request represents (DATABASE §5.5). The documented type domain is
 * {@code SUMMARY | CHAT | EMAIL | REPORT}; the database {@code CHECK(type IN (...))} guards that full
 * set. This slice implements <strong>only</strong> {@code SUMMARY} (AI Summary Generation), so that is
 * the only value the application ever writes or reads — the other capabilities are documented but not
 * built here.
 */
public enum AiRequestType {
    SUMMARY
}
