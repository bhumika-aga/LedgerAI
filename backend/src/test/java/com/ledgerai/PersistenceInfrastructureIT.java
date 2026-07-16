package com.ledgerai;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

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
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        
        assertThat(entityManagerFactory).isNotNull();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            assertThat(entityManager.isOpen()).isTrue();
        } finally {
            entityManager.close();
        }
    }
}
