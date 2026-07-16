package com.ledgerai.auth.dto;

/**
 * Access-token envelope (API_SPEC §17.2 AuthTokens). Per ADR-018 the refresh token is delivered as an
 * httpOnly cookie, so it is intentionally absent from the body.
 */
public record AuthTokensResponse(
    String accessToken,
    String tokenType,
    long expiresIn) {
    
    public static AuthTokensResponse bearer(String accessToken, long expiresInSeconds) {
        return new AuthTokensResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
