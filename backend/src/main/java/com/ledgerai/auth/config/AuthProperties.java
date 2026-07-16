package com.ledgerai.auth.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalized authentication configuration (SECURITY §13). Token lifetimes,
 * cookie attributes,
 * allowed CORS origins, and the password-policy threshold are all configuration
 * — never code
 * constants or product commitments. The JWT signing secret is supplied only via
 * the environment.
 */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
    Jwt jwt,
    RefreshToken refreshToken,
    Cookie cookie,
    Cors cors,
    @DefaultValue("8") int passwordMinLength) {
    
    public record Jwt(
        String secret,
        @DefaultValue("15m") Duration accessTokenTtl) {
    }
    
    public record RefreshToken(
        @DefaultValue("30d") Duration ttl) {
    }
    
    public record Cookie(
        @DefaultValue("refresh_token") String name,
        @DefaultValue("true") boolean secure,
        @DefaultValue("Strict") String sameSite,
        @DefaultValue("/api/v1/auth") String path) {
    }
    
    public record Cors(
        @DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
    }
}
