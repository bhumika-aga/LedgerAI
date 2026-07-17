package com.ledgerai.users.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the User module (BACKEND_CODING_STANDARDS §4 — Configuration). It only binds the module's
 * externalized settings; it holds no behavior.
 */
@Configuration
@EnableConfigurationProperties(ProfileProperties.class)
public class UsersConfig {
}
