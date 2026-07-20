package com.ledgerai.ai.port;

/**
 * The domain-owned AI port (ADR-003, AI_ARCHITECTURE §6, ARCHITECTURE §10). Business logic depends
 * only on this interface, expressed in domain terms — "generate text for this grounded prompt". The
 * concrete provider (Anthropic, ADR-003) is an adapter selected by configuration; <strong>no provider
 * SDK, request, or response type ever crosses this boundary</strong>.
 *
 * <p>The prompt is assembled centrally on the domain side <em>before</em> this port is consulted
 * (AI_ARCHITECTURE §8 — prompt composition is centralized, not scattered into adapters); the adapter
 * only maps the domain {@link AiPrompt} onto the provider request and maps the provider response back
 * to an {@link AiCompletion}. Provider failures are translated by the adapter into
 * {@link AiUnavailableException} so the pipeline can transition the request to {@code FAILED}
 * (AI_ARCHITECTURE §12).
 */
public interface AiPort {
    
    /**
     * Generates a completion for the given grounded prompt.
     *
     * @param prompt the centrally-assembled, channel-separated prompt (system instructions + grounded
     *               content)
     * @return the model's text output
     * @throws AiUnavailableException if the provider could not be reached or returned an error
     */
    AiCompletion generate(AiPrompt prompt);
}
