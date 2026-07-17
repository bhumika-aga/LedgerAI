package com.ledgerai.users.dto;

import java.util.Map;

/**
 * Profile update payload (API_SPEC §6.2): {@code { fullName?, professionalDetails?, preferences? }}.
 *
 * <p>Every field is optional — this is a PATCH, a "partial update of a resource" (API_SPEC §2.3), so an
 * omitted field leaves the stored value untouched. Length limits are enforced in the service against
 * configured VR-003 values rather than as annotations, because the limits are configuration, not
 * constants (see {@link com.ledgerai.users.config.ProfileProperties}).
 */
public record UpdateProfileRequest(
    String fullName,
    String professionalDetails,
    Map<String, Object> preferences) {
}
