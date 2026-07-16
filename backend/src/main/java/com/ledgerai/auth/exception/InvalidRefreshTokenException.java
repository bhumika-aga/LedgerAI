package com.ledgerai.auth.exception;

import com.ledgerai.common.exception.LedgerAiException;

/**
 * A refresh token that is missing, unknown, expired, or already revoked. Maps
 * to 401 (API_SPEC §5.3).
 */
public class InvalidRefreshTokenException extends LedgerAiException {
    
    public InvalidRefreshTokenException() {
        super("The session could not be renewed. Please sign in again.");
    }
}
