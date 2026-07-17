package com.ledgerai.auth.dto;

import com.ledgerai.auth.domain.UserAccount;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound user representation (API_SPEC §17.1). Never includes the password
 * hash. Profile fields
 * (professionalDetails, preferences) are owned by the future profile slice and
 * omitted here.
 */
public record UserResponse(
    UUID id,
    String email,
    String fullName,
    Instant createdAt,
    Instant updatedAt) {
    
    public static UserResponse from(UserAccount user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getCreatedAt(),
            user.getUpdatedAt());
    }
}
