package com.ledgerai.auth.exception;

import com.ledgerai.common.exception.LedgerAiException;

/**
 * Registration with an email that already exists (BR-021). Maps to 409 (API_SPEC §5.1).
 */
public class EmailAlreadyExistsException extends LedgerAiException {
    
    public EmailAlreadyExistsException() {
        super("An account with this email already exists.");
    }
}
