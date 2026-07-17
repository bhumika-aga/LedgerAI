package com.ledgerai.users.exception;

import com.ledgerai.common.exception.LedgerAiException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One or more profile fields failed VR-003. Maps to {@code 422} with {@code validationErrors}
 * (API_SPEC §6.2, §2.12; BACKEND_CODING_STANDARDS §8).
 *
 * <p>It carries field-level messages because VR-003 requires rejection "with field-level messages",
 * and because the limits are configured rather than annotated, so Bean Validation cannot express them.
 */
public class ProfileValidationException extends LedgerAiException {
    
    private final transient Map<String, String> fieldErrors;
    
    public ProfileValidationException(Map<String, String> fieldErrors) {
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
