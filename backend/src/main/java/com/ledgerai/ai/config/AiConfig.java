package com.ledgerai.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the AI module (BACKEND_CODING_STANDARDS §4 — Configuration). Binds the module's
 * externalized settings only; holds no behavior.
 */
@Configuration
@EnableConfigurationProperties({AiProperties.class, ChatProperties.class, EmailProperties.class})
public class AiConfig {
}
