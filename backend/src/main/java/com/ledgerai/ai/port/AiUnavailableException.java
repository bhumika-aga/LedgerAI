package com.ledgerai.ai.port;

import com.ledgerai.common.exception.LedgerAiException;

/**
 * The AI provider could not service a generation request (unreachable, timeout, rate limit, or provider
 * error). The adapter translates any provider/transport failure into this domain exception so no
 * provider type escapes the port.
 *
 * <p>Maps to {@code 503} (API_SPEC §10.1 — "AI provider unavailable"); the underlying provider error is
 * logged inside the adapter, never leaked (AI_ARCHITECTURE §12, §14). The service also transitions the
 * in-flight {@code AIRequest} to {@code FAILED} before this propagates, so the attempt is recorded and
 * retryable (SRS §7.2).
 */
public class AiUnavailableException extends LedgerAiException {
    
    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
