package com.ledgerai.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for {@code POST …/chat} (API_SPEC §11.1): the user's question about a document (AI Chat, SRS §4.8;
 * FR-CHAT-006). The question is required and non-blank — an empty question is a {@code 422} field error,
 * surfaced through the same validation model as every other boundary check (API_SPEC §2.12, VR-007). The
 * maximum length (a VR-007 {@code [Assumption]}) is enforced in the service against configuration, since it
 * is tunable rather than a compile-time constant.
 */
public record AskQuestionRequest(
    @NotBlank(message = "A question is required.") String question) {
}
