package com.ledgerai.activity;

import com.ledgerai.activity.domain.ActivityType;
import com.ledgerai.activity.dto.ActivityResponse;
import com.ledgerai.auth.config.SecurityConfig;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.GlobalExceptionHandler;
import com.ledgerai.common.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link ActivityController} (API_SPEC §15): the read-only timeline, the page
 * envelope (§17.9), the default newest-first ordering, the per-client view, the authenticated-only split,
 * and that no write method exists. The service is mocked.
 */
@WebMvcTest(ActivityController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, CurrentUserProvider.class})
@ActiveProfiles("test")
class ActivityControllerTest {
    
    private static final UUID ACTIVITY_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ActivityService activityService;
    
    private static RequestPostProcessor signedIn() {
        return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()));
    }
    
    private PageResponse<ActivityResponse> onePage() {
        ActivityResponse entry = new ActivityResponse(ACTIVITY_ID, ActivityType.CLIENT_CREATED,
            "Created client \"Acme\"", CLIENT_ID, null, null, Instant.now());
        return new PageResponse<>(List.of(entry), 0, 20, 1L, 1, false);
    }
    
    @Test
    void timelineReturnsThePageEnvelope() throws Exception {
        when(activityService.getTimeline(isNull(), any())).thenReturn(onePage());
        
        mockMvc.perform(get("/api/v1/activities").with(signedIn()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(ACTIVITY_ID.toString()))
            .andExpect(jsonPath("$.content[0].actionType").value("CLIENT_CREATED"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    void timelineDefaultsToNewestFirst() throws Exception {
        when(activityService.getTimeline(isNull(), any())).thenReturn(onePage());
        
        mockMvc.perform(get("/api/v1/activities").with(signedIn())).andExpect(status().isOk());
        
        var pageable = forClass(Pageable.class);
        verify(activityService).getTimeline(isNull(), pageable.capture());
        Sort.Order order = pageable.getValue().getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }
    
    @Test
    void timelineSupportsThePerClientView() throws Exception {
        when(activityService.getTimeline(eq(CLIENT_ID), any())).thenReturn(onePage());
        
        mockMvc.perform(get("/api/v1/activities").param("clientId", CLIENT_ID.toString()).with(signedIn()))
            .andExpect(status().isOk());
        
        verify(activityService).getTimeline(eq(CLIENT_ID), any());
    }
    
    @Test
    void timelineRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/activities")).andExpect(status().isUnauthorized());
    }
    
    @Test
    void thereIsNoWriteEndpoint() throws Exception {
        // The log is append-only over the API (FR-TMLN-004, BR-016): a write method is never accepted.
        // The controller declares no write mapping, so a POST is rejected (not a 2xx). (The exact error
        // status is the shared handler's concern; what matters here is that no write endpoint exists.)
        mockMvc.perform(post("/api/v1/activities").with(signedIn()))
            .andExpect(result -> assertThat(
                HttpStatus.valueOf(result.getResponse().getStatus()).is2xxSuccessful()).isFalse());
    }
}
