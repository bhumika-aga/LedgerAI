package com.ledgerai.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * End-to-end authentication test (API_SPEC §5, SECURITY §4, ADR-001, ADR-018) over real HTTP against a
 * real PostgreSQL. Exercises the whole slice through the security filter chain: the authenticated/public
 * split, the register → me → refresh → logout lifecycle with the httpOnly refresh cookie and rotation,
 * and the non-revealing credential failure (BR-020). Skipped where no Docker runtime is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthenticationIT {
    
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void meWithoutTokenIsUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/auth/me", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    void fullSessionLifecycle() throws Exception {
        String email = "lifecycle@example.com";
        
        ResponseEntity<String> registration = register(email, "correct-horse", "Ada Pro");
        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode registerBody = objectMapper.readTree(registration.getBody());
        String accessToken = registerBody.at("/tokens/accessToken").asText();
        assertThat(accessToken).isNotBlank();
        String refreshCookie = extractRefreshCookie(registration);
        assertThat(refreshCookie).isNotBlank();
        
        // The access token identifies the user at /me.
        ResponseEntity<String> me = restTemplate.exchange(
            "/api/v1/auth/me", HttpMethod.GET, bearer(accessToken), String.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(me.getBody()).at("/email").asText()).isEqualTo(email);
        
        // Refresh rotates the cookie and returns a fresh access token.
        ResponseEntity<String> refreshed = restTemplate.exchange(
            "/api/v1/auth/refresh", HttpMethod.POST, cookie(refreshCookie), String.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String rotatedAccessToken = objectMapper.readTree(refreshed.getBody()).at("/tokens/accessToken").asText();
        assertThat(rotatedAccessToken).isNotBlank();
        String rotatedCookie = extractRefreshCookie(refreshed);
        assertThat(rotatedCookie).isNotBlank().isNotEqualTo(refreshCookie);
        
        // The old (already-rotated) cookie is now revoked and cannot refresh again.
        ResponseEntity<String> reusedOld = restTemplate.exchange(
            "/api/v1/auth/refresh", HttpMethod.POST, cookie(refreshCookie), String.class);
        assertThat(reusedOld.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        
        // Logout requires authentication (API_SPEC §5.4) and revokes the token carried by the cookie.
        ResponseEntity<String> logout = restTemplate.exchange(
            "/api/v1/auth/logout", HttpMethod.POST, bearerWithCookie(rotatedAccessToken, rotatedCookie),
            String.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        ResponseEntity<String> refreshAfterLogout = restTemplate.exchange(
            "/api/v1/auth/refresh", HttpMethod.POST, cookie(rotatedCookie), String.class);
        assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    void credentialFailuresAreNonRevealing() throws Exception {
        String email = "victim@example.com";
        register(email, "correct-horse", "Ada Pro");
        
        ResponseEntity<String> wrongPassword = login(email, "wrong-password");
        ResponseEntity<String> unknownEmail = login("nobody@example.com", "correct-horse");
        
        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        
        // Identical response shape and message: a wrong password is indistinguishable from an unknown
        // account (BR-020).
        JsonNode wrongBody = objectMapper.readTree(wrongPassword.getBody());
        JsonNode unknownBody = objectMapper.readTree(unknownEmail.getBody());
        assertThat(wrongBody.at("/detail")).isEqualTo(unknownBody.at("/detail"));
        assertThat(wrongBody.at("/type")).isEqualTo(unknownBody.at("/type"));
    }
    
    private ResponseEntity<String> register(String email, String password, String fullName) {
        String body = "{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"%s\"}".formatted(email, password, fullName);
        return restTemplate.postForEntity("/api/v1/auth/register", json(body), String.class);
    }
    
    private ResponseEntity<String> login(String email, String password) {
        String body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        return restTemplate.postForEntity("/api/v1/auth/login", json(body), String.class);
    }
    
    private HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
    
    private HttpEntity<Void> bearer(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }
    
    private HttpEntity<Void> cookie(String cookieValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookieValue);
        return new HttpEntity<>(headers);
    }
    
    private HttpEntity<Void> bearerWithCookie(String accessToken, String cookieValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.add(HttpHeaders.COOKIE, cookieValue);
        return new HttpEntity<>(headers);
    }
    
    private String extractRefreshCookie(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        // Keep only the "name=value" pair, dropping the attributes (Path, HttpOnly, …).
        return setCookie.split(";", 2)[0];
    }
}
