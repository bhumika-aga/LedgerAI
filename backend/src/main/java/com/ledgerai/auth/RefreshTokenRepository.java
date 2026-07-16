package com.ledgerai.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ledgerai.auth.domain.RefreshToken;

/**
 * Persistence for {@link RefreshToken}. Data access only — no business rules (BACKEND_CODING_STANDARDS §4).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
