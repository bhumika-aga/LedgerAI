package com.ledgerai.clients.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the Client module (BACKEND_CODING_STANDARDS §4 — Configuration). It only binds the
 * module's externalized settings; it holds no behavior.
 */
@Configuration
@EnableConfigurationProperties(ClientProperties.class)
public class ClientsConfig {
}
