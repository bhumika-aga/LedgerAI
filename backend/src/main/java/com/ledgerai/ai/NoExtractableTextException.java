package com.ledgerai.ai;

import com.ledgerai.common.exception.LedgerAiException;

/**
 * A summary was requested for a document that is {@code READY} but whose extracted text is empty — there
 * is nothing to ground the summary in (AI_ARCHITECTURE §9). A degenerate case in practice (content is
 * only stored when extraction produced text), but the contract calls it out explicitly.
 *
 * <p>Maps to {@code 422} (API_SPEC §10.1 — "no extractable text"). Unlike a field-validation failure it
 * carries no {@code validationErrors}; it is a precondition on the resource, not on the request body.
 */
public class NoExtractableTextException extends LedgerAiException {
    
    public NoExtractableTextException(String message) {
        super(message);
    }
}
