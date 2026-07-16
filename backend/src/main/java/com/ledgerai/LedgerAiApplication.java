package com.ledgerai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point and composition root for the LedgerAI backend.
 *
 * <p>
 * Component scanning from this package covers every domain module beneath
 * {@code com.ledgerai}.
 * The application is a single deployable modular monolith; module boundaries
 * are enforced in code,
 * not by separate deployments.
 */
@SpringBootApplication
public class LedgerAiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LedgerAiApplication.class, args);
    }
}
