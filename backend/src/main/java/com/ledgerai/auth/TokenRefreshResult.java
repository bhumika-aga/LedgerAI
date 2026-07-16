package com.ledgerai.auth;

import com.ledgerai.auth.dto.AuthTokensResponse;

/**
 * Internal result of a refresh: the new access-token envelope plus the
 * <em>raw</em> rotated refresh
 * token for the httpOnly cookie (ADR-018). The raw token is never serialized
 * into a response body.
 */
record TokenRefreshResult(AuthTokensResponse tokens, String refreshToken) {
}
