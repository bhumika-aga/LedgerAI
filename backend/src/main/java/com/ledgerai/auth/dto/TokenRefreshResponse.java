package com.ledgerai.auth.dto;

/**
 * Refresh response (API_SPEC §5.3): the new access-token envelope only.
 */
public record TokenRefreshResponse(AuthTokensResponse tokens) {
}
