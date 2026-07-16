package com.ledgerai.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ledgerai.auth.domain.UserAccount;

/**
 * Persistence for {@link UserAccount}. Data access only — no business rules
 * (BACKEND_CODING_STANDARDS §4).
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    
    Optional<UserAccount> findByEmail(String email);
    
    boolean existsByEmail(String email);
}
