package com.ledgerai;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence infrastructure smoke test (ADR-017).
 *
 * <p>
 * Proves the persistence stack wires together against a real, disposable
 * PostgreSQL container:
 * the Spring context starts, the datasource initializes and connects, Flyway
 * applies the migration
 * history, and the {@link EntityManagerFactory} initializes. It exercises
 * infrastructure only — no
 * entities, repositories, or business behavior.
 *
 * <p>
 * The class is skipped automatically where no Docker runtime is available; it
 * runs in full in CI
 * and on any Docker-enabled machine (TESTING_STRATEGY §13).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PersistenceInfrastructureIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    
    @Autowired
    private Flyway flyway;
    
    @Test
    void persistenceInfrastructureInitializes() throws Exception {
        assertThat(postgres.isRunning()).isTrue();
        
        assertThat(dataSource).isNotNull();
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(5)).isTrue();
        }
        
        assertThat(flyway).isNotNull();
        // The migration history is fully applied with nothing pending — asserted independently of the
        // current migration count so the smoke test does not go stale as feature slices add migrations.
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().current()).isNotNull();
        
        assertThat(entityManagerFactory).isNotNull();
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            assertThat(entityManager.isOpen()).isTrue();
        }
    }
}
