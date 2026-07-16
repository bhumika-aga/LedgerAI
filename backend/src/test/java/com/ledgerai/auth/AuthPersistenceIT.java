package com.ledgerai.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ledgerai.auth.domain.RefreshToken;
import com.ledgerai.auth.domain.UserAccount;

/**
 * Repository tests for the auth persistence (DATABASE §5.1, §5.9) against a real PostgreSQL with the
 * Flyway schema (ADR-016, ADR-017). Verifies the finder queries and the case-insensitive email
 * uniqueness the {@code citext} column provides. Skipped where no Docker runtime is available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthPersistenceIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private UserAccountRepository userRepository;
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Test
    void persistsAndFindsUserByEmail() {
        UserAccount saved = userRepository.saveAndFlush(
            UserAccount.create("pro@example.com", "hashed", "Ada Pro"));
        
        assertThat(userRepository.findByEmail("pro@example.com")).get()
            .extracting(UserAccount::getId).isEqualTo(saved.getId());
        assertThat(userRepository.existsByEmail("pro@example.com")).isTrue();
    }
    
    @Test
    void treatsEmailCaseInsensitively() {
        userRepository.saveAndFlush(UserAccount.create("Mixed.Case@Example.com", "hashed", "Ada Pro"));
        
        assertThat(userRepository.findByEmail("mixed.case@example.com")).isPresent();
        assertThat(userRepository.existsByEmail("MIXED.CASE@EXAMPLE.COM")).isTrue();
    }
    
    @Test
    void persistsAndFindsRefreshTokenByHash() {
        UserAccount user = userRepository.saveAndFlush(
            UserAccount.create("token-owner@example.com", "hashed", "Ada Pro"));
        refreshTokenRepository.saveAndFlush(
            RefreshToken.issue(user.getId(), "token-hash", Instant.now().plusSeconds(3600)));
        
        assertThat(refreshTokenRepository.findByTokenHash("token-hash")).get()
            .extracting(RefreshToken::getUserId).isEqualTo(user.getId());
        assertThat(refreshTokenRepository.findByTokenHash("missing")).isEmpty();
    }
    
    @Test
    void generatesDistinctIdentifiersPerToken() {
        UserAccount user = userRepository.saveAndFlush(
            UserAccount.create("multi@example.com", "hashed", "Ada Pro"));
        RefreshToken first = refreshTokenRepository.saveAndFlush(
            RefreshToken.issue(user.getId(), "hash-1", Instant.now().plusSeconds(3600)));
        RefreshToken second = refreshTokenRepository.saveAndFlush(
            RefreshToken.issue(user.getId(), "hash-2", Instant.now().plusSeconds(3600)));
        
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }
}
