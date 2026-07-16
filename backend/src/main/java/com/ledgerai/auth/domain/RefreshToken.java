package com.ledgerai.auth.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persisted refresh-token record enabling renewal, rotation, and revocation
 * (DATABASE §5.9).
 *
 * <p>
 * Stores only the <em>hash</em> of the refresh token, never the raw value
 * (SECURITY §7). Records
 * are effectively immutable except for revocation, so there is no
 * {@code updated_at}.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    protected RefreshToken() {
        // for JPA
    }
    
    public static RefreshToken issue(UUID userId, String tokenHash, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.id = UUID.randomUUID();
        token.userId = userId;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        token.createdAt = Instant.now();
        return token;
    }
    
    public void revoke() {
        if (revokedAt == null) {
            revokedAt = Instant.now();
        }
    }
    
    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public String getTokenHash() {
        return tokenHash;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public Instant getRevokedAt() {
        return revokedAt;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
}
