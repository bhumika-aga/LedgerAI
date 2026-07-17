package com.ledgerai.users.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalized profile validation limits (VR-003).
 *
 * <p>VR-003 requires editable fields to stay "within defined length limits", but marks the concrete
 * values as an {@code [Assumption]}, and API_SPEC §18 defers them to the SRS/architecture, where they
 * are not yet finalized. They are therefore configuration — tunable per environment — and not product
 * commitments baked into code. This mirrors how the password threshold (VR-001) is handled per
 * SECURITY §6 ("finalized in configuration — not invented here as a product commitment").
 */
@ConfigurationProperties(prefix = "users.profile")
public record ProfileProperties(
    @DefaultValue("255") int maxFullNameLength,
    @DefaultValue("2000") int maxProfessionalDetailsLength,
    @DefaultValue("8192") int maxPreferencesBytes) {
}
