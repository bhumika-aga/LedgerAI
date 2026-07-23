package com.ledgerai.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for AI Email instruction validation (VR-007). API_SPEC §12.1 rejects an empty or invalid
 * instruction with {@code 422}; non-emptiness is checked at the boundary (Bean Validation), but the
 * concrete maximum length is a VR-007 {@code [Assumption]} the SRS/architecture has not finalized — so,
 * like the profile (VR-003), client (VR-004), document (VR-005), search (VR-006), chat (VR-007), and
 * report (VR-008) limits, it is externalized as tunable configuration rather than a product commitment
 * baked into code.
 *
 * @param maxInstructionLength the maximum accepted length of an email generation instruction
 */
@ConfigurationProperties(prefix = "ai.email")
public record EmailProperties(
    @DefaultValue("4000") int maxInstructionLength) {
}
