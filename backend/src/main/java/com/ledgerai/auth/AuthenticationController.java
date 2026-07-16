package com.ledgerai.auth;

import java.time.Duration;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ledgerai.auth.config.AuthProperties;
import com.ledgerai.auth.dto.AuthResponse;
import com.ledgerai.auth.dto.LoginRequest;
import com.ledgerai.auth.dto.RegisterRequest;
import com.ledgerai.auth.dto.TokenRefreshResponse;
import com.ledgerai.auth.dto.UserResponse;

import jakarta.validation.Valid;

/**
 * The authentication endpoints defined by API_SPEC §5 (`/auth`). This controller is thin: it
 * translates HTTP ↔ application calls, moves the refresh token between the service and an httpOnly
 * cookie (ADR-018), and delegates every rule to {@link AuthenticationService}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    private final AuthProperties properties;
    
    public AuthenticationController(AuthenticationService authenticationService, AuthProperties properties) {
        this.authenticationService = authenticationService;
        this.properties = properties;
    }
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticationResult result = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                   .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
                   .body(result.body());
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResult result = authenticationService.login(request);
        return ResponseEntity.ok()
                   .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
                   .body(result.body());
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
        @CookieValue(name = "${auth.cookie.name}", required = false) String refreshToken) {
        TokenRefreshResult result = authenticationService.refresh(refreshToken);
        return ResponseEntity.ok()
                   .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
                   .body(new TokenRefreshResponse(result.tokens()));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(name = "${auth.cookie.name}", required = false) String refreshToken) {
        authenticationService.logout(refreshToken);
        return ResponseEntity.noContent()
                   .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
                   .build();
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(authenticationService.getCurrentUser(userId));
    }
    
    private ResponseCookie refreshCookie(String value) {
        return baseCookie(value).maxAge(properties.refreshToken().ttl()).build();
    }
    
    private ResponseCookie clearedRefreshCookie() {
        return baseCookie("").maxAge(Duration.ZERO).build();
    }
    
    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        AuthProperties.Cookie cookie = properties.cookie();
        return ResponseCookie.from(cookie.name(), value)
                   .httpOnly(true)
                   .secure(cookie.secure())
                   .sameSite(cookie.sameSite())
                   .path(cookie.path());
    }
}
