package com.ledgerai.users;

import com.ledgerai.auth.dto.UserResponse;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.users.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The User module's profile endpoints (API_SPEC §6): {@code GET} and {@code PATCH /api/v1/users/me}.
 *
 * <p>Thin by design (BACKEND_CODING_STANDARDS §4): it resolves the caller from the security context and
 * delegates every rule to {@link ProfileService}. The subject id comes only from the authenticated
 * principal — never from the path or body — which is what makes "a user can only ever read their own
 * profile" (FR-PROF-004, BR-023) true by construction rather than by a check.
 */
@RestController
@RequestMapping("/api/v1/users/me")
public class ProfileController {
    
    private final ProfileService profileService;
    private final CurrentUserProvider currentUserProvider;
    
    public ProfileController(ProfileService profileService, CurrentUserProvider currentUserProvider) {
        this.profileService = profileService;
        this.currentUserProvider = currentUserProvider;
    }
    
    @GetMapping
    public ResponseEntity<UserResponse> getProfile() {
        UUID userId = currentUserProvider.requireUserId();
        return ResponseEntity.ok(profileService.getProfile(userId));
    }
    
    @PatchMapping
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = currentUserProvider.requireUserId();
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }
}
