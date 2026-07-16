package com.ledgerai.auth.exception;

import com.ledgerai.common.exception.LedgerAiException;

/**
 * Login failure. The message is deliberately generic and non-revealing (BR-020) — it never indicates
 * whether the account exists. Maps to 401 (API_SPEC §5.2).
 */
public class InvalidCredentialsException extends LedgerAiException {
    
    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
