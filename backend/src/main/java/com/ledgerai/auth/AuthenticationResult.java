package com.ledgerai.auth;

import com.ledgerai.auth.dto.AuthResponse;

/**
 * Internal result of register/login: the response body plus the <em>raw</em> refresh token, which the
 * controller places in an httpOnly cookie (ADR-018) and which is never serialized into a response body.
 */
record AuthenticationResult(AuthResponse body, String refreshToken) {
}
