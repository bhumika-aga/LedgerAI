package com.ledgerai.auth.exception;

import com.ledgerai.common.exception.LedgerAiException;

/**
 * A password that does not meet the configured strength policy (VR-001;
 * threshold is a configured
 * value, not a product commitment — SECURITY §6). Maps to 422 with a
 * field-level error.
 */
public class WeakPasswordException extends LedgerAiException {
    
    public WeakPasswordException(String message) {
        super(message);
    }
}
