package com.ledgerai.common.exception;

/**
 * The caller is authenticated but not permitted to perform this operation, in a case where the
 * resource's existence is <em>already legitimately known</em> to them.
 *
 * <p>Maps to {@code 403} (BACKEND_CODING_STANDARDS §8, API_SPEC §2.4). This is the narrow exception to
 * the {@code 404} rule: {@code 403} is correct only when answering it reveals nothing the caller does
 * not already know. For anything reached by id that the caller may not own — the ordinary case — use
 * {@link ResourceNotFoundException} instead, per the non-disclosure trade-off in SECURITY §5.
 */
public class ForbiddenException extends LedgerAiException {
    
    public ForbiddenException() {
        super("You do not have access to perform this action.");
    }
    
    public ForbiddenException(String message) {
        super(message);
    }
}
