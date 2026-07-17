package com.ledgerai.common.exception;

/**
 * No authenticated user could be established for an operation that requires one.
 *
 * <p>Maps to {@code 401} (BACKEND_CODING_STANDARDS §8, API_SPEC §2.4). The message is generic and
 * non-revealing (BR-020, SECURITY §4): it never distinguishes a missing token from an expired or
 * malformed one.
 */
public class UnauthenticatedException extends LedgerAiException {
    
    public UnauthenticatedException() {
        super("Authentication is required.");
    }
}
