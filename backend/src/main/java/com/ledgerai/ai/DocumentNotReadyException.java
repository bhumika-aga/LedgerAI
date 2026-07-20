package com.ledgerai.ai;

import com.ledgerai.common.exception.LedgerAiException;

/**
 * An AI action was requested for a document that is not {@code READY} (BR-010, AI_ARCHITECTURE §4). AI
 * capabilities operate only on a document whose text extraction has succeeded; a document still
 * processing, or one that failed extraction, cannot be summarized.
 *
 * <p>Maps to {@code 409 Conflict} (API_SPEC §10.1). The message is clear and non-technical.
 */
public class DocumentNotReadyException extends LedgerAiException {
    
    public DocumentNotReadyException(String message) {
        super(message);
    }
}
