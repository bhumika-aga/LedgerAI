package com.ledgerai.users;

import com.ledgerai.auth.UserAccountRepository;
import com.ledgerai.auth.domain.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence tests for the profile columns (DATABASE §5.1) against a real PostgreSQL with the Flyway
 * schema (ADR-016, ADR-017). The point of interest is {@code preferences}: it is a {@code jsonb} column
 * mapped to a free-form map, so this proves the JSON round-trip actually works against Postgres rather
 * than only in a mock. Skipped where no Docker runtime is available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ProfilePersistenceIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private UserAccountRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void persistsProfileFieldsIncludingJsonbPreferences() {
        UserAccount user = userRepository.saveAndFlush(
            UserAccount.create("profile@example.com", "hashed", "Ada Pro"));
        
        Map<String, Object> preferences = new LinkedHashMap<>();
        preferences.put("theme", "dark");
        preferences.put("density", "compact");
        user.applyProfileUpdate("Ada Professional", "Chartered Accountant, Mumbai", preferences);
        userRepository.saveAndFlush(user);
        
        // Clear the persistence context so the read below comes from PostgreSQL, not the first-level cache.
        entityManager.clear();
        
        UserAccount reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getFullName()).isEqualTo("Ada Professional");
        assertThat(reloaded.getProfessionalDetails()).isEqualTo("Chartered Accountant, Mumbai");
        assertThat(reloaded.getPreferences())
            .containsEntry("theme", "dark")
            .containsEntry("density", "compact");
    }
    
    @Test
    void roundTripsNestedAndNonStringPreferenceValues() {
        UserAccount user = userRepository.saveAndFlush(
            UserAccount.create("nested@example.com", "hashed", "Ada Pro"));
        
        // The shape is opaque by design, so arbitrary JSON must survive unchanged.
        Map<String, Object> preferences = Map.of(
            "notifications", Map.of("email", true),
            "pageSize", 25);
        user.applyProfileUpdate(null, null, preferences);
        userRepository.saveAndFlush(user);
        entityManager.clear();
        
        UserAccount reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getPreferences()).containsEntry("pageSize", 25);
        assertThat(reloaded.getPreferences().get("notifications"))
            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
            .containsEntry("email", true);
    }
    
    @Test
    void leavesProfileFieldsNullUntilSet() {
        UserAccount user = userRepository.saveAndFlush(
            UserAccount.create("fresh@example.com", "hashed", null));
        entityManager.clear();
        
        UserAccount reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getFullName()).isNull();
        assertThat(reloaded.getProfessionalDetails()).isNull();
        assertThat(reloaded.getPreferences()).isNull();
    }
    
    @Test
    void keepsUnsuppliedFieldsUnchanged() {
        UserAccount user = userRepository.saveAndFlush(
            UserAccount.create("partial@example.com", "hashed", "Original Name"));
        user.applyProfileUpdate(null, "Original Details", Map.of("theme", "light"));
        userRepository.saveAndFlush(user);
        entityManager.clear();
        
        UserAccount loaded = userRepository.findById(user.getId()).orElseThrow();
        loaded.applyProfileUpdate("Updated Name", null, null);
        userRepository.saveAndFlush(loaded);
        entityManager.clear();
        
        UserAccount reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getFullName()).isEqualTo("Updated Name");
        assertThat(reloaded.getProfessionalDetails()).isEqualTo("Original Details");
        assertThat(reloaded.getPreferences()).containsEntry("theme", "light");
    }
}
