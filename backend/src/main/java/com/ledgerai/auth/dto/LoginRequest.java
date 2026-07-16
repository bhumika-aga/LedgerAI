package com.ledgerai.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login payload (API_SPEC §5.2, VR-002). Only presence is validated at the boundary; credential
 * correctness is checked in the service with a generic, non-revealing failure (BR-020).
 */
public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password) {
}
