package com.ledgerai.users;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerai.auth.UserAccountService;
import com.ledgerai.auth.dto.ProfileUpdate;
import com.ledgerai.auth.dto.UserResponse;
import com.ledgerai.users.config.ProfileProperties;
import com.ledgerai.users.dto.UpdateProfileRequest;
import com.ledgerai.users.exception.ProfileValidationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Profile business rules (SRS §4.2: FR-PROF-001…005; VR-003; BR-023).
 *
 * <p>Owns validation and the partial-update semantics; the account row itself is reached through the
 * Authentication module's published {@link UserAccountService}, never through its repository or entity
 * (ARCHITECTURE §5.1).
 *
 * <p><strong>Isolation (FR-PROF-004, BR-023) is structural.</strong> Every operation is scoped to the
 * user id the caller's access token resolved to; no id is ever accepted from the request path or body,
 * and API_SPEC §6.1 confirms there is no {@code /users/{id}} in the MVP. There is therefore no
 * client-supplied identifier to forge and nothing for an ownership check to compare — reading one's own
 * row by one's own id cannot be a cross-user access. Adding an {@code OwnershipGuard} call here would
 * compare the current user to itself and prove nothing.
 */
@Service
public class ProfileService {
    
    private final UserAccountService userAccountService;
    private final ProfileProperties properties;
    private final ObjectMapper objectMapper;
    
    public ProfileService(UserAccountService userAccountService, ProfileProperties properties,
                          ObjectMapper objectMapper) {
        this.userAccountService = userAccountService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }
    
    /**
     * FR-PROF-001: the caller's own profile.
     */
    public UserResponse getProfile(UUID userId) {
        return userAccountService.getProfile(userId);
    }
    
    /**
     * FR-PROF-002/003: validate against VR-003, then persist the supplied fields.
     */
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        validate(request);
        return userAccountService.updateProfile(userId, new ProfileUpdate(
            request.fullName(),
            request.professionalDetails(),
            request.preferences()));
    }
    
    /**
     * VR-003: editable fields must stay within the configured length limits, and every failure is
     * reported field-by-field in one response rather than one at a time.
     */
    private void validate(UpdateProfileRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkLength(errors, "fullName", request.fullName(), properties.maxFullNameLength());
        checkLength(errors, "professionalDetails", request.professionalDetails(),
            properties.maxProfessionalDetailsLength());
        checkPreferences(errors, request.preferences());
        if (!errors.isEmpty()) {
            throw new ProfileValidationException(errors);
        }
    }
    
    private void checkLength(Map<String, String> errors, String field, String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            errors.put(field, "Must be at most " + maxLength + " characters.");
        }
    }
    
    private void checkPreferences(Map<String, String> errors, Map<String, Object> preferences) {
        if (preferences == null) {
            return;
        }
        // The shape is intentionally opaque (DATABASE §5.1: free-form jsonb), so the only rule that can
        // be applied without inventing keys is a size bound on the serialized document.
        try {
            int bytes = objectMapper.writeValueAsString(preferences).getBytes(StandardCharsets.UTF_8).length;
            if (bytes > properties.maxPreferencesBytes()) {
                errors.put("preferences", "Must be at most " + properties.maxPreferencesBytes() + " bytes.");
            }
        } catch (JsonProcessingException e) {
            errors.put("preferences", "Must be a valid JSON object.");
        }
    }
}
