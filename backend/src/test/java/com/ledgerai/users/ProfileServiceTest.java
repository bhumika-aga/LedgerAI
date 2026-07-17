package com.ledgerai.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerai.auth.UserAccountService;
import com.ledgerai.auth.dto.ProfileUpdate;
import com.ledgerai.auth.dto.UserResponse;
import com.ledgerai.users.config.ProfileProperties;
import com.ledgerai.users.dto.UpdateProfileRequest;
import com.ledgerai.users.exception.ProfileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the profile business rules (SRS §4.2: FR-PROF-001…003; VR-003).
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    
    private static final int MAX_FULL_NAME = 10;
    private static final int MAX_DETAILS = 20;
    private static final int MAX_PREFERENCES_BYTES = 50;
    
    @Mock
    private UserAccountService userAccountService;
    
    private ProfileService service;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        service = new ProfileService(
            userAccountService,
            new ProfileProperties(MAX_FULL_NAME, MAX_DETAILS, MAX_PREFERENCES_BYTES),
            new ObjectMapper());
        userId = UUID.randomUUID();
    }
    
    private UserResponse sampleResponse() {
        Instant now = Instant.now();
        return new UserResponse(userId, "pro@example.com", "Ada", "CA", Map.of("theme", "dark"), now, now);
    }
    
    @Test
    void returnsTheCallersProfile() {
        when(userAccountService.getProfile(userId)).thenReturn(sampleResponse());
        
        assertThat(service.getProfile(userId).email()).isEqualTo("pro@example.com");
    }
    
    @Test
    void passesSuppliedFieldsThroughToPersistence() {
        when(userAccountService.updateProfile(eq(userId), any())).thenReturn(sampleResponse());
        
        service.updateProfile(userId, new UpdateProfileRequest("Ada", "CA", Map.of("theme", "dark")));
        
        ArgumentCaptor<ProfileUpdate> captor = ArgumentCaptor.forClass(ProfileUpdate.class);
        verify(userAccountService).updateProfile(eq(userId), captor.capture());
        assertThat(captor.getValue().fullName()).isEqualTo("Ada");
        assertThat(captor.getValue().professionalDetails()).isEqualTo("CA");
        assertThat(captor.getValue().preferences()).containsEntry("theme", "dark");
    }
    
    @Test
    void treatsOmittedFieldsAsUnchanged() {
        when(userAccountService.updateProfile(eq(userId), any())).thenReturn(sampleResponse());
        
        // A PATCH carrying only fullName must not disturb the other fields (API_SPEC §2.3).
        service.updateProfile(userId, new UpdateProfileRequest("Ada", null, null));
        
        ArgumentCaptor<ProfileUpdate> captor = ArgumentCaptor.forClass(ProfileUpdate.class);
        verify(userAccountService).updateProfile(eq(userId), captor.capture());
        assertThat(captor.getValue().fullName()).isEqualTo("Ada");
        assertThat(captor.getValue().professionalDetails()).isNull();
        assertThat(captor.getValue().preferences()).isNull();
    }
    
    @Test
    void acceptsAnEmptyPatch() {
        when(userAccountService.updateProfile(eq(userId), any())).thenReturn(sampleResponse());
        
        assertThatCode(() -> service.updateProfile(userId, new UpdateProfileRequest(null, null, null)))
            .doesNotThrowAnyException();
    }
    
    @Test
    void rejectsAnOverLongFullName() {
        UpdateProfileRequest request =
            new UpdateProfileRequest("x".repeat(MAX_FULL_NAME + 1), null, null);
        
        ProfileValidationException thrown = catchThrowableOfType(
            () -> service.updateProfile(userId, request), ProfileValidationException.class);
        
        assertThat(thrown.getFieldErrors()).containsOnlyKeys("fullName");
        verify(userAccountService, never()).updateProfile(any(), any());
    }
    
    @Test
    void rejectsOverLongProfessionalDetails() {
        UpdateProfileRequest request = new UpdateProfileRequest(null, "y".repeat(MAX_DETAILS + 1), null);
        
        ProfileValidationException thrown = catchThrowableOfType(
            () -> service.updateProfile(userId, request), ProfileValidationException.class);
        
        assertThat(thrown.getFieldErrors()).containsOnlyKeys("professionalDetails");
    }
    
    @Test
    void rejectsOversizedPreferences() {
        UpdateProfileRequest request =
            new UpdateProfileRequest(null, null, Map.of("key", "z".repeat(MAX_PREFERENCES_BYTES)));
        
        ProfileValidationException thrown = catchThrowableOfType(
            () -> service.updateProfile(userId, request), ProfileValidationException.class);
        
        assertThat(thrown.getFieldErrors()).containsOnlyKeys("preferences");
    }
    
    @Test
    void reportsEveryInvalidFieldAtOnce() {
        // VR-003 requires field-level messages; a user should not have to fix one field per round trip.
        UpdateProfileRequest request = new UpdateProfileRequest(
            "x".repeat(MAX_FULL_NAME + 1),
            "y".repeat(MAX_DETAILS + 1),
            null);
        
        ProfileValidationException thrown = catchThrowableOfType(
            () -> service.updateProfile(userId, request), ProfileValidationException.class);
        
        assertThat(thrown.getFieldErrors()).containsOnlyKeys("fullName", "professionalDetails");
    }
    
    @Test
    void acceptsValuesExactlyAtTheLimit() {
        when(userAccountService.updateProfile(eq(userId), any())).thenReturn(sampleResponse());
        
        assertThatCode(() -> service.updateProfile(userId,
            new UpdateProfileRequest("x".repeat(MAX_FULL_NAME), "y".repeat(MAX_DETAILS), null)))
            .doesNotThrowAnyException();
    }
    
    @Test
    void allowsClearingATextFieldWithAnEmptyString() {
        when(userAccountService.updateProfile(eq(userId), any())).thenReturn(sampleResponse());
        
        service.updateProfile(userId, new UpdateProfileRequest("", "", null));
        
        ArgumentCaptor<ProfileUpdate> captor = ArgumentCaptor.forClass(ProfileUpdate.class);
        verify(userAccountService).updateProfile(eq(userId), captor.capture());
        assertThat(captor.getValue().fullName()).isEmpty();
    }
}
