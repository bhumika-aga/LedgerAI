package com.ledgerai.auth.dto;

/**
 * Register/login response (API_SPEC §5.1–5.2): the user plus the access-token
 * envelope.
 */
public record AuthResponse(
    UserResponse user,
    AuthTokensResponse tokens) {
}
