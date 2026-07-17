package com.ledgerai.common.security;

import com.ledgerai.common.exception.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for principal resolution (ARCHITECTURE §9.1–9.2, SECURITY §5).
 *
 * <p>The emphasis is on <strong>failing closed</strong>: every context the provider cannot positively
 * establish as an authenticated user with a well-formed id must resolve to "no current user", never to
 * a guess.
 */
class CurrentUserProviderTest {
    
    private final CurrentUserProvider provider = new CurrentUserProvider();
    
    private static Jwt jwtWithSubject(String subject) {
        return Jwt.withTokenValue("token")
                   .header("alg", "HS256")
                   .subject(subject)
                   .claim("sub", subject)
                   .build();
    }
    
    private static void authenticateWith(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void resolvesCurrentUserFromJwtSubject() {
        UUID userId = UUID.randomUUID();
        authenticateWith(new JwtAuthenticationToken(jwtWithSubject(userId.toString()), List.of()));
        
        assertThat(provider.find()).contains(new CurrentUser(userId));
        assertThat(provider.requireUserId()).isEqualTo(userId);
    }
    
    @Test
    void findsNoUserWhenContextIsEmpty() {
        assertThat(provider.find()).isEmpty();
    }
    
    @Test
    void findsNoUserWhenAuthenticationIsAnonymous() {
        authenticateWith(new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        
        assertThat(provider.find()).isEmpty();
    }
    
    @Test
    void findsNoUserWhenAuthenticationIsNotAuthenticated() {
        // The single-argument constructor leaves the token unauthenticated.
        authenticateWith(new JwtAuthenticationToken(jwtWithSubject(UUID.randomUUID().toString())));
        
        assertThat(provider.find()).isEmpty();
    }
    
    @Test
    void findsNoUserWhenPrincipalIsNotAJwt() {
        authenticateWith(new UsernamePasswordAuthenticationToken("someone", "credentials", List.of()));
        
        assertThat(provider.find()).isEmpty();
    }
    
    @Test
    void failsClosedWhenSubjectIsNotAUserId() {
        authenticateWith(new JwtAuthenticationToken(jwtWithSubject("not-a-uuid"), List.of()));
        
        assertThat(provider.find()).isEmpty();
        assertThatThrownBy(provider::require).isInstanceOf(UnauthenticatedException.class);
    }
    
    @Test
    void requireRejectsAnUnauthenticatedContext() {
        assertThatThrownBy(provider::require).isInstanceOf(UnauthenticatedException.class);
        assertThatThrownBy(provider::requireUserId).isInstanceOf(UnauthenticatedException.class);
    }
    
    @Test
    void currentUserRejectsANullIdentity() {
        assertThatThrownBy(() -> new CurrentUser(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
