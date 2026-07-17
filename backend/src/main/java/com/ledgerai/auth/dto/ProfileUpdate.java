package com.ledgerai.auth.dto;

import java.util.Map;

/**
 * A partial profile edit, expressed as a command across the module boundary (API_SPEC §6.2).
 *
 * <p>Part of {@link com.ledgerai.auth.UserAccountService}'s published contract: the User module builds
 * one of these after applying its own rules, and the Authentication module persists it. Entities never
 * cross the boundary (ARCHITECTURE §5.2).
 *
 * <p>A {@code null} field means "not supplied — leave unchanged"; a supplied value replaces the current
 * one. Callers clear a text field by supplying an empty string rather than {@code null}, because the
 * documentation does not define a distinct "explicitly null" semantic for PATCH.
 */
public record ProfileUpdate(
    String fullName,
    String professionalDetails,
    Map<String, Object> preferences) {
}
