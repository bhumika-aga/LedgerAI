package com.ledgerai.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ledgerai.auth.config.AuthProperties;
import com.ledgerai.auth.domain.RefreshToken;
import com.ledgerai.auth.domain.UserAccount;
import com.ledgerai.auth.dto.AuthResponse;
import com.ledgerai.auth.dto.AuthTokensResponse;
import com.ledgerai.auth.dto.LoginRequest;
import com.ledgerai.auth.dto.RegisterRequest;
import com.ledgerai.auth.dto.UserResponse;
import com.ledgerai.auth.exception.EmailAlreadyExistsException;
import com.ledgerai.auth.exception.InvalidCredentialsException;
import com.ledgerai.auth.exception.InvalidRefreshTokenException;
import com.ledgerai.auth.exception.WeakPasswordException;

/**
 * Owns the authentication business rules (SECURITY §4, SRS §4.1): registration, credential
 * verification, session issuance, refresh-token rotation, and revocation. All rules live here, not in
 * the controller (BACKEND_CODING_STANDARDS §4).
 */
@Service
public class AuthenticationService {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    
    private final UserAccountRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties properties;
    
    public AuthenticationService(
        UserAccountRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AuthProperties properties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
    }
    
    @Transactional
    public AuthenticationResult register(RegisterRequest request) {
        if (request.password().length() < properties.passwordMinLength()) {
            throw new WeakPasswordException(
                "Password must be at least " + properties.passwordMinLength() + " characters.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }
        UserAccount user = userRepository.save(UserAccount.create(
            request.email(),
            passwordEncoder.encode(request.password()),
            request.fullName()));
        return issueSession(user);
    }
    
    @Transactional
    public AuthenticationResult login(LoginRequest request) {
        UserAccount user = userRepository.findByEmail(request.email())
                               .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueSession(user);
    }
    
    @Transactional
    public TokenRefreshResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                                    .orElseThrow(InvalidRefreshTokenException::new);
        if (!existing.isActive(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }
        existing.revoke();
        String rawRefresh = issueRefreshToken(existing.getUserId());
        AuthTokensResponse tokens = AuthTokensResponse.bearer(
            jwtService.issueAccessToken(existing.getUserId()), jwtService.accessTokenTtlSeconds());
        return new TokenRefreshResult(tokens, rawRefresh);
    }
    
    @Transactional
    public void logout(String rawRefreshToken) {
        // Idempotent (API_SPEC §5.4): an unknown or already-revoked token still succeeds.
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(RefreshToken::revoke);
    }
    
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                   .map(UserResponse::from)
                   .orElseThrow(InvalidCredentialsException::new);
    }
    
    private AuthenticationResult issueSession(UserAccount user) {
        String rawRefresh = issueRefreshToken(user.getId());
        AuthTokensResponse tokens = AuthTokensResponse.bearer(
            jwtService.issueAccessToken(user.getId()), jwtService.accessTokenTtlSeconds());
        return new AuthenticationResult(new AuthResponse(UserResponse.from(user), tokens), rawRefresh);
    }
    
    private String issueRefreshToken(UUID userId) {
        byte[] raw = new byte[32];
        SECURE_RANDOM.nextBytes(raw);
        String rawToken = URL_ENCODER.encodeToString(raw);
        Instant expiresAt = Instant.now().plus(properties.refreshToken().ttl());
        refreshTokenRepository.save(RefreshToken.issue(userId, hash(rawToken), expiresAt));
        return rawToken;
    }
    
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }
}
