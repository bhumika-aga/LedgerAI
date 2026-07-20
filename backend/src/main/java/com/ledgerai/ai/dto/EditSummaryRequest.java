package com.ledgerai.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for {@code PATCH …/summary} (API_SPEC §10.3): the user's edited summary content
 * (human-in-the-loop, BR-031). Content is required and non-blank — an empty edit is a {@code 422}
 * field error, surfaced through the same validation model as every other boundary check
 * (API_SPEC §2.12).
 */
public record EditSummaryRequest(
    @NotBlank(message = "Summary content is required.") String content) {
}
