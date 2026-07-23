package com.ledgerai.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Body for {@code POST /ai/emails} (API_SPEC §12.1): the user's email generation instruction plus optional
 * client and/or document context (AI Email, SRS §4.9). The instruction is required and non-blank — an empty
 * instruction is a {@code 422} field error, surfaced through the same validation model as every other
 * boundary check (API_SPEC §2.12, VR-007). The maximum length (a VR-007 {@code [Assumption]}) is enforced
 * in the service against configuration, since it is tunable rather than a compile-time constant.
 *
 * <p>{@code clientId}/{@code documentId} are optional context: each is validated for ownership when present
 * (a referenced document must additionally be {@code READY}). The client is context only — it is not
 * persisted on the AI request (no column, DATABASE §5.5) and is not returned in the {@code AIResponse}.
 */
public record GenerateEmailRequest(
    @NotBlank(message = "An instruction is required.") String instruction,
    UUID clientId,
    UUID documentId) {
}
