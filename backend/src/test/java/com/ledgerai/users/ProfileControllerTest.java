package com.ledgerai.users;

import com.ledgerai.auth.config.SecurityConfig;
import com.ledgerai.auth.dto.UserResponse;
import com.ledgerai.common.exception.GlobalExceptionHandler;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.users.dto.UpdateProfileRequest;
import com.ledgerai.users.exception.ProfileValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link ProfileController} (API_SPEC §6). Verifies the HTTP contract — the
 * authenticated-only split, the documented statuses (200/401/422), the full {@code UserResponse} shape
 * from §17.1, and that the subject acted on is always the token's, never the client's.
 */
@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CurrentUserProvider.class})
@ActiveProfiles("test")
class ProfileControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ProfileService profileService;
    
    private UserResponse sampleResponse(UUID id) {
        Instant now = Instant.now();
        return new UserResponse(id, "pro@example.com", "Ada Pro", "Chartered Accountant",
            Map.of("theme", "dark"), now, now);
    }
    
    @Test
    void getReturnsTheDocumentedUserResponseShape() throws Exception {
        UUID id = UUID.randomUUID();
        when(profileService.getProfile(id)).thenReturn(sampleResponse(id));
        
        mockMvc.perform(get("/api/v1/users/me").with(jwt().jwt(builder -> builder.subject(id.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.email").value("pro@example.com"))
            .andExpect(jsonPath("$.fullName").value("Ada Pro"))
            .andExpect(jsonPath("$.professionalDetails").value("Chartered Accountant"))
            .andExpect(jsonPath("$.preferences.theme").value("dark"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
    
    @Test
    void getRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value("/problems/authentication-failed"));
    }
    
    @Test
    void patchRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Ada\"}"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void patchUpdatesAndReturnsTheProfile() throws Exception {
        UUID id = UUID.randomUUID();
        when(profileService.updateProfile(eq(id), any())).thenReturn(sampleResponse(id));
        
        mockMvc.perform(patch("/api/v1/users/me")
                            .with(jwt().jwt(builder -> builder.subject(id.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Ada Pro\",\"professionalDetails\":\"Chartered Accountant\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Ada Pro"));
        
        verify(profileService).updateProfile(eq(id),
            eq(new UpdateProfileRequest("Ada Pro", "Chartered Accountant", null)));
    }
    
    @Test
    void patchActsOnTheTokenSubjectNotOnAnyClientSuppliedId() throws Exception {
        UUID tokenSubject = UUID.randomUUID();
        UUID attemptedVictim = UUID.randomUUID();
        when(profileService.updateProfile(any(), any())).thenReturn(sampleResponse(tokenSubject));
        
        // An "id" in the body is not part of the contract and must not steer the update (FR-PROF-004).
        mockMvc.perform(patch("/api/v1/users/me")
                            .with(jwt().jwt(builder -> builder.subject(tokenSubject.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\":\"" + attemptedVictim + "\",\"fullName\":\"Mallory\"}"))
            .andExpect(status().isOk());
        
        verify(profileService).updateProfile(eq(tokenSubject), any());
    }
    
    @Test
    void patchSurfacesValidationFailuresAs422WithFieldErrors() throws Exception {
        UUID id = UUID.randomUUID();
        when(profileService.updateProfile(eq(id), any()))
            .thenThrow(new ProfileValidationException(Map.of("fullName", "Must be at most 255 characters.")));
        
        mockMvc.perform(patch("/api/v1/users/me")
                            .with(jwt().jwt(builder -> builder.subject(id.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"way-too-long\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("/problems/validation-error"))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.validationErrors[0].field").value("fullName"))
            .andExpect(jsonPath("$.validationErrors[0].message").value("Must be at most 255 characters."))
            .andExpect(jsonPath("$.traceId").exists());
    }
    
    @Test
    void patchAcceptsAPartialBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(profileService.updateProfile(eq(id), any())).thenReturn(sampleResponse(id));
        
        mockMvc.perform(patch("/api/v1/users/me")
                            .with(jwt().jwt(builder -> builder.subject(id.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"preferences\":{\"theme\":\"dark\"}}"))
            .andExpect(status().isOk());
        
        verify(profileService).updateProfile(eq(id),
            eq(new UpdateProfileRequest(null, null, Map.of("theme", "dark"))));
    }
}
