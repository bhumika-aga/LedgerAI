package com.ledgerai.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for the AI provider (ADR-003 → Anthropic) and the prompt/context budget. All values are
 * environment-driven and server-held; the API key is a secret with no default and MUST be supplied by
 * the environment, never committed (SECURITY §13). Provider-specific values are consumed only by the
 * Anthropic adapter, which is absent from the test profile (an in-memory AI port stands in).
 *
 * <p>The concrete model is chosen here at implementation time, behind the port (AI_ARCHITECTURE §7,
 * ADR-003); it is tunable configuration, not a hard-coded constant. {@code maxDocumentChars} caps how
 * much extracted text is sent, applying the data- and cost-minimization levers (AI_ARCHITECTURE §13,
 * NFR-018) and keeping the prompt within the model's context window (§5, §7).
 *
 * @param apiUrl           the Anthropic Messages API base, e.g. {@code https://api.anthropic.com}
 * @param apiVersion       the {@code anthropic-version} header value
 * @param apiKey           the server-side API key authorizing requests (secret; no default)
 * @param model            the model id used for summarization (default, tunable behind the port)
 * @param maxTokens        the maximum output tokens to request (summaries are short)
 * @param maxDocumentChars the maximum characters of extracted text sent to the provider per request
 */
@ConfigurationProperties(prefix = "ai.anthropic")
public record AiProperties(
    @DefaultValue("https://api.anthropic.com") String apiUrl,
    @DefaultValue("2023-06-01") String apiVersion,
    String apiKey,
    @DefaultValue("claude-opus-4-8") String model,
    @DefaultValue("1024") int maxTokens,
    @DefaultValue("24000") int maxDocumentChars) {
}
