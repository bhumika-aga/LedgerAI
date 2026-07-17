package com.ledgerai.auth;

import com.ledgerai.auth.domain.UserAccount;
import com.ledgerai.auth.dto.ProfileUpdate;
import com.ledgerai.auth.dto.UserResponse;
import com.ledgerai.common.exception.UnauthenticatedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The Authentication module's <strong>published</strong> access to the user account record
 * (ARCHITECTURE §5.1, §5.4; BACKEND_CODING_STANDARDS §3).
 *
 * <p>The {@code user} row is owned by this module — it is created at registration (DATABASE §5.1) — but
 * it is also "updated via profile", which the User module owns. Rather than let another module reach
 * into this one's repository or entity (forbidden by ARCHITECTURE §5.1), this service is the seam:
 * it takes and returns DTOs only, so {@link UserAccount} never crosses the boundary.
 *
 * <p>It holds no profile business rules — those belong to the User module (SRS §4.2). This is data
 * access and transaction boundary only.
 *
 * <p>A well-formed token whose subject no longer identifies a user is treated as unauthenticated
 * ({@code 401}), which is the only failure API_SPEC §6.1/§6.2 admit for these reads and writes.
 */
@Service
public class UserAccountService {
    
    private final UserAccountRepository userRepository;
    
    public UserAccountService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(require(userId));
    }
    
    @Transactional
    public UserResponse updateProfile(UUID userId, ProfileUpdate update) {
        UserAccount user = require(userId);
        user.applyProfileUpdate(update.fullName(), update.professionalDetails(), update.preferences());
        return UserResponse.from(userRepository.save(user));
    }
    
    private UserAccount require(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UnauthenticatedException::new);
    }
}
