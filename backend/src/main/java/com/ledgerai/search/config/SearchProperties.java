package com.ledgerai.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for search query validation (VR-006). API_SPEC §14.1 rejects an invalid/oversized query
 * with {@code 422} but the concrete maximum length is a VR-006 {@code [Assumption]} that the SRS/architecture
 * has not finalized — so, like the profile (VR-003), client (VR-004), and document (VR-005) limits, it is
 * externalized as tunable configuration rather than a product commitment baked into code.
 *
 * @param maxQueryLength the maximum accepted length of the {@code q} keyword string
 */
@ConfigurationProperties(prefix = "search")
public record SearchProperties(@DefaultValue("256") int maxQueryLength) {
}
