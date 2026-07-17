package com.ledgerai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * System Health slice — backend integration test (ADR-015 health baseline,
 * ADR-017 test database).
 *
 * <p>
 * Proves that the health endpoint the frontend calls works end to end over real
 * HTTP against a
 * real PostgreSQL. The application boots (datasource and Flyway initialized),
 * and a GET to
 * {@code /actuator/health} returns 200 with an aggregate {@code UP} status.
 * That aggregate can only be
 * {@code UP} if the datasource health indicator reaches PostgreSQL, so this
 * exercises the
 * HTTP → backend → persistence → response chain with no business code, entity,
 * or endpoint of its own.
 *
 * <p>
 * Skipped where no Docker runtime is available; runs in full in CI
 * (TESTING_STRATEGY §13).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class HealthEndpointIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void healthEndpointReportsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
