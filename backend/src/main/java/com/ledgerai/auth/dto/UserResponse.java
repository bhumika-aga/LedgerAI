package com.ledgerai.auth.dto;

import com.ledgerai.auth.domain.UserAccount;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Outbound user representation (API_SPEC §17.1):
 * {@code { id, email, fullName?, professionalDetails?, preferences?, createdAt, updatedAt }}.
 * Never includes the password hash.
 *
 * <p>This is a shared schema — API_SPEC defines it once and both the Authentication module
 * (§5.1, §5.2, §5.5) and the User module (§6.1, §6.2) return it, so it is published rather than
 * duplicated per module.
 */
public record UserResponse(
    UUID id,
    String email,
    String fullName,
    String professionalDetails,
    Map<String, Object> preferences,
    Instant createdAt,
    Instant updatedAt) {
    
    public static UserResponse from(UserAccount user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getProfessionalDetails(),
            user.getPreferences(),
            user.getCreatedAt(),
            user.getUpdatedAt());
    }
}
