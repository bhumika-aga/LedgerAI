package com.ledgerai.users.exception;

import com.ledgerai.common.exception.ValidationFailedException;

import java.util.Map;

/**
 * One or more profile fields failed VR-003 (API_SPEC §6.2).
 *
 * <p>A named specialization of the shared {@link ValidationFailedException}: the User module keeps its
 * own vocabulary (BACKEND_CODING_STANDARDS §3 — a module owns its exceptions) while the field-error
 * carrying and the {@code 422} mapping stay in the one shared type, so no module reimplements them.
 */
public class ProfileValidationException extends ValidationFailedException {
    
    public ProfileValidationException(Map<String, String> fieldErrors) {
        super(fieldErrors);
    }
}
