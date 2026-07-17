package com.ledgerai.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One or more fields failed a business validation rule. Maps to {@code 422} with
 * {@code validationErrors} (API_SPEC §2.12; BACKEND_CODING_STANDARDS §8).
 *
 * <p>This is the shared "validation exception" of the error taxonomy — §8 calls for "a small hierarchy
 * so handling is consistent and centralized", so every module raises this one type rather than
 * declaring its own near-identical copy. It exists alongside Bean Validation: annotations cover request
 * *shape* at the boundary, while rules whose limits are configuration (the SRS `[Assumption]` values)
 * can only be checked in a service, and both must surface identically to the client.
 *
 * <p>Field errors are ordered as validated, so a form shows them top-to-bottom as the user reads them.
 */
public class ValidationFailedException extends LedgerAiException {
    
    private final transient Map<String, String> fieldErrors;
    
    public ValidationFailedException(Map<String, String> fieldErrors) {
        super("One or more fields are invalid.");
        this.fieldErrors = new LinkedHashMap<>(fieldErrors);
    }
    
    /**
     * Field name → message, in the order the fields were validated.
     */
    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }
}
