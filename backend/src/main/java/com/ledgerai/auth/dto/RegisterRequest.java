package com.ledgerai.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Registration payload (API_SPEC §5.1, VR-001). Password strength beyond "present" is enforced in the
 * service against a configured policy — the SRS VR-001 [Assumption] value, not fixed here.
 */
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    String fullName) {
}
