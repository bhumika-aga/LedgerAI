package com.ledgerai.activity.dto;

import com.ledgerai.activity.domain.Activity;
import com.ledgerai.activity.domain.ActivityType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Outbound timeline entry (API_SPEC §17.8):
 * {@code { id, actionType, summary?, clientId?, documentId?, metadata?, createdAt }} — read-only. Mirrors
 * the {@code Activity} record (DATABASE §5.8); it never exposes the owning {@code userId} (the timeline
 * is already the caller's own, so the id is redundant and withheld).
 */
public record ActivityResponse(
    UUID id,
    ActivityType actionType,
    String summary,
    UUID clientId,
    UUID documentId,
    Map<String, Object> metadata,
    Instant createdAt) {
    
    public static ActivityResponse from(Activity activity) {
        return new ActivityResponse(
            activity.getId(),
            activity.getActionType(),
            activity.getSummary(),
            activity.getClientId(),
            activity.getDocumentId(),
            activity.getMetadata(),
            activity.getCreatedAt());
    }
}
