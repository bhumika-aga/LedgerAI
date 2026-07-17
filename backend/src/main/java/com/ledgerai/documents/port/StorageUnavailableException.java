package com.ledgerai.documents.port;

import com.ledgerai.common.exception.LedgerAiException;

/**
 * The storage provider could not service the request (store/sign/delete). Maps to {@code 503}
 * (API_SPEC §8.1/§8.5; BACKEND_CODING_STANDARDS §8). The message is user-facing and non-technical; the
 * underlying provider error is logged, never leaked.
 */
public class StorageUnavailableException extends LedgerAiException {
    
    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
