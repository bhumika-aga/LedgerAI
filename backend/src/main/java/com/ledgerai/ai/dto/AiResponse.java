package com.ledgerai.ai.dto;

import com.ledgerai.ai.domain.AiOutput;
import com.ledgerai.ai.domain.AiRequest;
import com.ledgerai.ai.domain.AiRequestStatus;
import com.ledgerai.ai.domain.AiRequestType;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound AI representation (API_SPEC §17.5):
 * {@code { id, type, status, documentId?, prompt?, content?, edited, failureReason?, createdAt, updatedAt }}.
 * Mirrors {@code AIRequest} + its optional {@code AIOutput} (DATABASE §5.5–5.6). {@code content} is
 * present only when {@code status = COMPLETED} (an output exists); {@code failureReason} only when
 * {@code FAILED}.
 *
 * <p>The composed grounded prompt is never exposed — {@code prompt} is the user's question/instruction
 * (null for summary), not the internal system/grounding text (AI_ARCHITECTURE §14).
 */
public record AiResponse(
    UUID id,
    AiRequestType type,
    AiRequestStatus status,
    UUID documentId,
    String prompt,
    String content,
    boolean edited,
    String failureReason,
    Instant createdAt,
    Instant updatedAt) {
    
    /**
     * Builds the response from the request and its output (which is {@code null} unless the request has
     * completed).
     */
    public static AiResponse from(AiRequest request, AiOutput output) {
        return new AiResponse(
            request.getId(),
            request.getType(),
            request.getStatus(),
            request.getDocumentId(),
            request.getPrompt(),
            output == null ? null : output.getContent(),
            output != null && output.isEdited(),
            request.getFailureReason(),
            request.getCreatedAt(),
            request.getUpdatedAt());
    }
}
