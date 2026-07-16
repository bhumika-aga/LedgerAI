package com.ledgerai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test for the scaffold: the Spring application context starts under the
 * isolated {@code test}
 * profile. It asserts that the foundation is wired and boots; it verifies no
 * product behavior.
 */
@SpringBootTest
@ActiveProfiles("test")
class LedgerAiApplicationTests {
    
    @Test
    void contextLoads() {
    }
}
