package com.ledgerai.common.security;

import com.ledgerai.common.exception.UnauthenticatedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the authenticated principal from the security context (ARCHITECTURE §9.1–9.2).
 *
 * <p>This is the single place that reads the security context and translates it into the application's
 * own {@link CurrentUser}. Keeping it here means business modules never import Spring Security types
 * and never parse token claims themselves — a shared control is a consistently-correct control
 * (SECURITY §Engineering Practices).
 *
 * <p>It <strong>fails closed</strong> (SECURITY §5): anything it cannot positively establish as an
 * authenticated user with a well-formed identity resolves to "no current user" rather than a guess.
 */
@Component
public class CurrentUserProvider {
    
    /**
     * The current user, or empty when the request is not authenticated as a real user. Never throws.
     */
    public Optional<CurrentUser> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        return toUserId(jwt.getSubject()).map(CurrentUser::new);
    }
    
    /**
     * The current user, or {@link UnauthenticatedException} (→ {@code 401}) when there is none.
     * Use this wherever a caller's identity is required to proceed.
     */
    public CurrentUser require() {
        return find().orElseThrow(UnauthenticatedException::new);
    }
    
    /**
     * Convenience for the common case: the authenticated user's id, or {@code 401}.
     */
    public UUID requireUserId() {
        return require().id();
    }
    
    private Optional<UUID> toUserId(String subject) {
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(subject));
        } catch (IllegalArgumentException e) {
            // A token whose subject is not a user id cannot identify a user; treat it as unauthenticated
            // rather than trusting it (SECURITY §Trust Boundaries — never trust client-provided state).
            return Optional.empty();
        }
    }
}
