package com.ledgerai.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ledgerai.auth.config.AuthProperties;
import com.ledgerai.auth.domain.RefreshToken;
import com.ledgerai.auth.domain.UserAccount;
import com.ledgerai.auth.dto.LoginRequest;
import com.ledgerai.auth.dto.RegisterRequest;
import com.ledgerai.auth.dto.UserResponse;
import com.ledgerai.auth.exception.EmailAlreadyExistsException;
import com.ledgerai.auth.exception.InvalidCredentialsException;
import com.ledgerai.auth.exception.InvalidRefreshTokenException;
import com.ledgerai.auth.exception.WeakPasswordException;

/**
 * Unit tests for the authentication business rules (SRS §4.1, SECURITY §4). The collaborators are
 * mocked so every rule — password policy, duplicate email, non-revealing credential failure, refresh
 * rotation, idempotent logout — is asserted in isolation from HTTP and persistence.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    
    private static final String EMAIL = "professional@example.com";
    private static final String PASSWORD = "correct-horse";
    
    @Mock
    private UserAccountRepository userRepository;
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtService jwtService;
    
    private AuthenticationService service;
    
    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
            new AuthProperties.Jwt("test-secret-that-is-at-least-32-bytes-long!!", Duration.ofMinutes(15)),
            new AuthProperties.RefreshToken(Duration.ofDays(30)),
            new AuthProperties.Cookie("refresh_token", false, "Strict", "/api/v1/auth"),
            new AuthProperties.Cors(List.of("http://localhost:5173")),
            8);
        service = new AuthenticationService(
            userRepository, refreshTokenRepository, passwordEncoder, jwtService, properties);
    }
    
    private void stubSessionIssuance() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.issueAccessToken(any(UUID.class))).thenReturn("access-token");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);
    }
    
    @Test
    void registerRejectsPasswordBelowPolicy() {
        RegisterRequest request = new RegisterRequest(EMAIL, "short", "Ada Pro");
        
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(WeakPasswordException.class);
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, "Ada Pro");
        
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void registerPersistsUserAndIssuesSession() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        stubSessionIssuance();
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, "Ada Pro");
        
        AuthenticationResult result = service.register(request);
        
        assertThat(result.body().user().email()).isEqualTo(EMAIL);
        assertThat(result.body().tokens().accessToken()).isEqualTo("access-token");
        assertThat(result.body().tokens().tokenType()).isEqualTo("Bearer");
        assertThat(result.body().tokens().expiresIn()).isEqualTo(900L);
        assertThat(result.refreshToken()).isNotBlank();
        verify(passwordEncoder).encode(PASSWORD);
        verify(userRepository).save(any(UserAccount.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
    
    @Test
    void loginRejectsUnknownEmailWithoutRevealingIt() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
        
        assertThatThrownBy(() -> service.login(request)).isInstanceOf(InvalidCredentialsException.class);
    }
    
    @Test
    void loginRejectsWrongPassword() {
        UserAccount user = UserAccount.create(EMAIL, "hashed", "Ada Pro");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, "hashed")).thenReturn(false);
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
        
        assertThatThrownBy(() -> service.login(request)).isInstanceOf(InvalidCredentialsException.class);
    }
    
    @Test
    void loginIssuesSessionOnValidCredentials() {
        UserAccount user = UserAccount.create(EMAIL, "hashed", "Ada Pro");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, "hashed")).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.issueAccessToken(any(UUID.class))).thenReturn("access-token");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
        
        AuthenticationResult result = service.login(request);
        
        assertThat(result.body().user().email()).isEqualTo(EMAIL);
        assertThat(result.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
    
    @Test
    void refreshRejectsMissingToken() {
        assertThatThrownBy(() -> service.refresh(null)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.refresh("  ")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
    }
    
    @Test
    void refreshRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> service.refresh("raw-token")).isInstanceOf(InvalidRefreshTokenException.class);
    }
    
    @Test
    void refreshRejectsExpiredToken() {
        RefreshToken expired = RefreshToken.issue(UUID.randomUUID(), "hash", Instant.now().minusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));
        
        assertThatThrownBy(() -> service.refresh("raw-token")).isInstanceOf(InvalidRefreshTokenException.class);
    }
    
    @Test
    void refreshRotatesTokenAndIssuesAccessToken() {
        UUID userId = UUID.randomUUID();
        RefreshToken active = RefreshToken.issue(userId, "hash", Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(active));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.issueAccessToken(userId)).thenReturn("access-token");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);
        
        TokenRefreshResult result = service.refresh("raw-token");
        
        assertThat(active.getRevokedAt()).isNotNull();
        assertThat(result.tokens().accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
    
    @Test
    void logoutIsIdempotentWhenTokenMissing() {
        service.logout(null);
        service.logout("   ");
        
        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
    }
    
    @Test
    void logoutRevokesKnownToken() {
        RefreshToken active = RefreshToken.issue(UUID.randomUUID(), "hash", Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(active));
        
        service.logout("raw-token");
        
        assertThat(active.getRevokedAt()).isNotNull();
    }
    
    @Test
    void getCurrentUserReturnsProfile() {
        UserAccount user = UserAccount.create(EMAIL, "hashed", "Ada Pro");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        
        UserResponse response = service.getCurrentUser(user.getId());
        
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.fullName()).isEqualTo("Ada Pro");
    }
    
    @Test
    void getCurrentUserRejectsUnknownId() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> service.getCurrentUser(unknown)).isInstanceOf(InvalidCredentialsException.class);
    }
}
