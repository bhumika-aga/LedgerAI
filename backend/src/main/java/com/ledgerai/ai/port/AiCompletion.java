package com.ledgerai.ai.port;

/**
 * The provider's generated output in domain terms — plain text (AI_ARCHITECTURE §6, response mapper).
 * The text is portable across providers ({@code AIOutput.content}, DATABASE §5.6); no provider metadata
 * crosses the port. May be blank, in which case the service treats it as an output-validation failure
 * (AI_ARCHITECTURE §11), never a success.
 */
public record AiCompletion(String text) {
}
