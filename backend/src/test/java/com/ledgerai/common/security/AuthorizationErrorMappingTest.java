package com.ledgerai.common.security;

import com.ledgerai.auth.config.SecurityConfig;
import com.ledgerai.common.exception.ForbiddenException;
import com.ledgerai.common.exception.GlobalExceptionHandler;
import com.ledgerai.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Proves the authorization foundation end to end over HTTP: the filter chain denies by default, the
 * principal reaches application code, ownership is enforced, and every outcome is rendered as an
 * RFC 7807 document (API_SPEC §2.12, §2.4; SECURITY §5).
 *
 * <p>The probe controller below exists <strong>only in test sources</strong>. It ships no product
 * behavior and introduces no documented endpoint; it is the minimum protected surface needed to
 * exercise infrastructure that has no protected feature to ride on yet.
 */
@WebMvcTest(controllers = AuthorizationErrorMappingTest.AuthorizationProbeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CurrentUserProvider.class, OwnershipGuard.class,
    AuthorizationErrorMappingTest.AuthorizationProbeController.class})
@ActiveProfiles("test")
class AuthorizationErrorMappingTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    private static String problemField(String body, String field) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).at("/" + field).asText();
    }
    
    @Test
    void deniesUnauthenticatedRequestsWithProblemDetails() throws Exception {
        mockMvc.perform(get("/test-only/authorization/whoami"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/authentication-failed"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Authentication failed"))
            .andExpect(jsonPath("$.instance").value("/test-only/authorization/whoami"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.traceId").exists());
    }
    
    @Test
    void resolvesThePrincipalForAuthenticatedRequests() throws Exception {
        UUID userId = UUID.randomUUID();
        
        mockMvc.perform(get("/test-only/authorization/whoami")
                            .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(content().string(userId.toString()));
    }
    
    @Test
    void rejectsATokenWhoseSubjectIsNotAUserId() throws Exception {
        // Fails closed: a well-signed token that cannot identify a user is not an authenticated user.
        mockMvc.perform(get("/test-only/authorization/whoami")
                            .with(jwt().jwt(builder -> builder.subject("not-a-uuid"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value("/problems/authentication-failed"));
    }
    
    @Test
    void allowsTheOwnerThroughTheOwnershipGuard() throws Exception {
        UUID userId = UUID.randomUUID();
        
        mockMvc.perform(get("/test-only/authorization/owned/{ownerId}", userId)
                            .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }
    
    @Test
    void reportsAnotherUsersResourceAsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID someoneElses = UUID.randomUUID();
        
        mockMvc.perform(get("/test-only/authorization/owned/{ownerId}", someoneElses)
                            .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/resource-not-found"))
            .andExpect(jsonPath("$.status").value(404));
    }
    
    @Test
    void makesANonOwnedResourceIndistinguishableFromAnAbsentOne() throws Exception {
        UUID userId = UUID.randomUUID();
        
        String nonOwned = mockMvc.perform(
                get("/test-only/authorization/owned/{ownerId}", UUID.randomUUID())
                    .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                              .andExpect(status().isNotFound())
                              .andReturn().getResponse().getContentAsString();
        
        String absent = mockMvc.perform(
                get("/test-only/authorization/absent")
                    .with(jwt().jwt(builder -> builder.subject(userId.toString()))))
                            .andExpect(status().isNotFound())
                            .andReturn().getResponse().getContentAsString();
        
        // Same status, type, title and detail: the response cannot betray whether the id was real
        // (SECURITY §5). Only `instance` differs, which merely echoes the path the caller already knows.
        assertThat(problemField(nonOwned, "type")).isEqualTo(problemField(absent, "type"));
        assertThat(problemField(nonOwned, "title")).isEqualTo(problemField(absent, "title"));
        assertThat(problemField(nonOwned, "detail")).isEqualTo(problemField(absent, "detail"));
        assertThat(problemField(nonOwned, "status")).isEqualTo(problemField(absent, "status"));
    }
    
    @Test
    void mapsForbiddenToProblemDetailsWhenExistenceIsAlreadyKnown() throws Exception {
        mockMvc.perform(get("/test-only/authorization/forbidden")
                            .with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/access-denied"))
            .andExpect(jsonPath("$.status").value(403));
    }
    
    @Test
    void neverLeaksInternalsInAuthorizationFailures() throws Exception {
        String body = mockMvc.perform(get("/test-only/authorization/whoami"))
                          .andReturn().getResponse().getContentAsString();
        
        assertThat(body)
            .doesNotContain("Exception")
            .doesNotContain("com.ledgerai")
            .doesNotContain("org.springframework");
    }
    
    /**
     * Test-only probe. Not a product endpoint; never compiled into the application.
     */
    @RestController
    @RequestMapping("/test-only/authorization")
    static class AuthorizationProbeController {
        
        private final CurrentUserProvider currentUserProvider;
        private final OwnershipGuard ownershipGuard;
        
        AuthorizationProbeController(CurrentUserProvider currentUserProvider, OwnershipGuard ownershipGuard) {
            this.currentUserProvider = currentUserProvider;
            this.ownershipGuard = ownershipGuard;
        }
        
        /**
         * Echoes the resolved principal, proving the security context reaches application code.
         */
        @GetMapping("/whoami")
        String whoami() {
            return currentUserProvider.requireUserId().toString();
        }
        
        /**
         * Guarded by ownership of the supplied owner id.
         */
        @GetMapping("/owned/{ownerId}")
        String owned(@PathVariable UUID ownerId) {
            ownershipGuard.requireOwner(ownerId);
            return "ok";
        }
        
        /**
         * Stands in for a resource that genuinely does not exist.
         */
        @GetMapping("/absent")
        String absent() {
            throw new ResourceNotFoundException();
        }
        
        /**
         * Stands in for the narrow case where existence is already known to the caller.
         */
        @GetMapping("/forbidden")
        String forbidden() {
            throw new ForbiddenException();
        }
    }
}
