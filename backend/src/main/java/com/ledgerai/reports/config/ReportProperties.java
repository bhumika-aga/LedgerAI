package com.ledgerai.reports.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for report validation (VR-008). API_SPEC §13 rejects invalid user-supplied parameters with
 * {@code 422}, but the concrete length bounds are a VR-008 {@code [Assumption]} the SRS/architecture has not
 * finalized — so, like the profile (VR-003), client (VR-004), document (VR-005), and search (VR-006) limits,
 * they are externalized as tunable configuration rather than product commitments baked into code.
 *
 * @param maxTitleLength   the maximum accepted length of a report title
 * @param maxContentLength the maximum accepted length of user-edited report content
 */
@ConfigurationProperties(prefix = "reports")
public record ReportProperties(
    @DefaultValue("200") int maxTitleLength,
    @DefaultValue("100000") int maxContentLength) {
}
