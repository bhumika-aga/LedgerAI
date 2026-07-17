package com.ledgerai.auth;

import com.ledgerai.auth.config.AuthProperties;
import com.ledgerai.auth.config.SecurityConfig;
import com.ledgerai.auth.dto.AuthResponse;
import com.ledgerai.auth.dto.AuthTokensResponse;
import com.ledgerai.auth.dto.UserResponse;
import com.ledgerai.auth.exception.EmailAlreadyExistsException;
import com.ledgerai.auth.exception.InvalidCredentialsException;
import com.ledgerai.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for {@link AuthenticationController} (API_SPEC §5). Verifies
 * the HTTP contract —
 * status codes, the httpOnly refresh cookie (ADR-018), request-body validation
 * surfaced as RFC 7807
 * (API_SPEC §2.12), the public/authenticated split enforced by
 * {@link SecurityConfig}, and the
 * {@code /me} projection from the JWT principal — with the service mocked.
 */
@WebMvcTest(AuthenticationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AuthenticationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private AuthenticationService authenticationService;
    
    @Autowired
    private AuthProperties properties;
    
    private AuthResponse sampleAuthResponse(UUID id) {
        Instant now = Instant.now();
        UserResponse user = new UserResponse(id, "pro@example.com", "Ada Pro", null, null, now, now);
        return new AuthResponse(user, AuthTokensResponse.bearer("access-token", 900L));
    }
    
    @Test
    void registerReturns201WithRefreshCookie() throws Exception {
        UUID id = UUID.randomUUID();
        when(authenticationService.register(any()))
            .thenReturn(new AuthenticationResult(sampleAuthResponse(id), "raw-refresh"));
        
        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"pro@example.com\",\"password\":\"correct-horse\",\"fullName\":\"Ada Pro\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.email").value("pro@example.com"))
            .andExpect(jsonPath("$.tokens.accessToken").value("access-token"))
            .andExpect(jsonPath("$.tokens.tokenType").value("Bearer"))
            .andExpect(cookie().exists(properties.cookie().name()))
            .andExpect(cookie().httpOnly(properties.cookie().name(), true))
            .andExpect(cookie().value(properties.cookie().name(), "raw-refresh"));
    }
    
    @Test
    void registerRejectsInvalidBodyWith422ProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE))
            .andExpect(jsonPath("$.type").value("/problems/validation-error"))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.validationErrors").isArray())
            .andExpect(jsonPath("$.traceId").exists());
    }
    
    @Test
    void registerSurfacesDuplicateEmailAs409() throws Exception {
        when(authenticationService.register(any())).thenThrow(new EmailAlreadyExistsException());
        
        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"pro@example.com\",\"password\":\"correct-horse\",\"fullName\":\"Ada Pro\"}"))
            .andExpect(status().isConflict())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE))
            .andExpect(jsonPath("$.status").value(409));
    }
    
    @Test
    void loginReturns200WithRefreshCookie() throws Exception {
        UUID id = UUID.randomUUID();
        when(authenticationService.login(any()))
            .thenReturn(new AuthenticationResult(sampleAuthResponse(id), "raw-refresh"));
        
        mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"pro@example.com\",\"password\":\"correct-horse\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokens.accessToken").value("access-token"))
            .andExpect(cookie().exists(properties.cookie().name()));
    }
    
    @Test
    void loginSurfacesBadCredentialsAs401() throws Exception {
        when(authenticationService.login(any())).thenThrow(new InvalidCredentialsException());
        
        mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"pro@example.com\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }
    
    @Test
    void logoutReturns204AndClearsCookie() throws Exception {
        // Logout requires authentication (API_SPEC §5.4) and revokes the token carried
        // by the cookie.
        mockMvc.perform(post("/api/v1/auth/logout")
                            .with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString())))
                            .cookie(new jakarta.servlet.http.Cookie(properties.cookie().name(), "raw-refresh")))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge(properties.cookie().name(), 0));
    }
    
    @Test
    void logoutWithoutAuthenticationIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").cookie(
                new jakarta.servlet.http.Cookie(properties.cookie().name(), "raw-refresh")))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }
    
    @Test
    void meReturnsCurrentUserFromJwtSubject() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        when(authenticationService.getCurrentUser(id))
            .thenReturn(new UserResponse(id, "pro@example.com", "Ada Pro", null, null, now, now));
        
        mockMvc.perform(get("/api/v1/auth/me").with(jwt().jwt(builder -> builder.subject(id.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.email").value("pro@example.com"));
    }
}
