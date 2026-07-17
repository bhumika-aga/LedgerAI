package com.ledgerai.clients.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalized client validation limits (VR-004).
 *
 * <p>VR-004 requires the name to be "within length limit", and DATABASE §5.2 notes "≤ 200 chars" — but
 * marks it an {@code [Assumption]}, and API_SPEC §18 defers concrete limits to the SRS/architecture
 * where they are not finalized. They are therefore configuration, not product commitments in code,
 * exactly as the password (VR-001) and profile (VR-003) thresholds already are.
 *
 * <p>The name default mirrors the documented 200. VR-004 also says optional fields are "validated if
 * present" without stating any limit for them, so those defaults are engineering choices, tunable per
 * environment, and are called out as such.
 */
@ConfigurationProperties(prefix = "clients")
public record ClientProperties(
    @DefaultValue("200") int maxNameLength,
    @DefaultValue("2000") int maxContactDetailsLength,
    @DefaultValue("5000") int maxNotesLength) {
}
