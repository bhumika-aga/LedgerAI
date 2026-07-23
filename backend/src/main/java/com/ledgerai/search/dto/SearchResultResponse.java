package com.ledgerai.search.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound search hit (API_SPEC §17.7): {@code { documentId, clientId, title, snippet, matchContext, updatedAt }}.
 * Enough to render a result and navigate to the owning document ([FR-SRCH-002]); it never carries the
 * full extracted text or any internal reference.
 *
 * <ul>
 *   <li>{@code title} — the document's original filename (the document has no separate title).</li>
 *   <li>{@code snippet} — a short leading excerpt of the extracted text, for preview.</li>
 *   <li>{@code matchContext} — a plain-text window of the extracted text around the first matched keyword,
 *       so the professional can see why the document matched. No highlighting markup is added.</li>
 * </ul>
 */
public record SearchResultResponse(
    UUID documentId,
    UUID clientId,
    String title,
    String snippet,
    String matchContext,
    Instant updatedAt) {
}
