package com.ledgerai.common.exception;

/**
 * Base type for the backend's typed exceptions.
 *
 * <p>
 * Domain, validation, security, and provider exceptions extend this shared root
 * so that error
 * handling can be centralized and mapped consistently to the API error model.
 * It carries no
 * transport or HTTP-status concerns; that mapping lives in the central
 * exception handler introduced
 * with the API layer.
 */
public abstract class LedgerAiException extends RuntimeException {
    
    protected LedgerAiException(String message) {
        super(message);
    }
    
    protected LedgerAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
