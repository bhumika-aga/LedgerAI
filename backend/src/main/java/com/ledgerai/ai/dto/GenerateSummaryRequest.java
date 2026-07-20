package com.ledgerai.ai.dto;

/**
 * Optional body for {@code POST …/summary} (API_SPEC §10.1): {@code { regenerate?: true }} to force a
 * fresh attempt even when a completed summary already exists. Absent body / null {@code regenerate}
 * means "return the existing summary if there is one, otherwise generate".
 */
public record GenerateSummaryRequest(Boolean regenerate) {
    
    public boolean isRegenerate() {
        return Boolean.TRUE.equals(regenerate);
    }
}
