package com.ledgerai.search.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the Search module (BACKEND_CODING_STANDARDS §4 — Configuration). Binds the module's
 * externalized settings only; holds no behavior.
 */
@Configuration
@EnableConfigurationProperties(SearchProperties.class)
public class SearchConfig {
}
