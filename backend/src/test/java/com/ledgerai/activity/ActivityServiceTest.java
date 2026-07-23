package com.ledgerai.activity;

import com.ledgerai.activity.domain.Activity;
import com.ledgerai.activity.domain.ActivityType;
import com.ledgerai.activity.dto.ActivityResponse;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ActivityService} — the shared recording API and owner-scoped timeline read
 * (API_SPEC §15; DATABASE §5.8; BR-006). Collaborators are mocked; the focus is owner scoping, the
 * account-vs-per-client query selection, and that each documented event records the right type/fields.
 */
@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {
    
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    
    @InjectMocks
    private ActivityService service;
    
    private ArgumentCaptor<Activity> captureSaved() {
        return ArgumentCaptor.forClass(Activity.class);
    }
    
    @Test
    void accountTimelineIsScopedToTheCurrentUser() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserProvider.requireUserId()).thenReturn(USER_ID);
        when(activityRepository.findByUserId(USER_ID, pageable))
            .thenReturn(new PageImpl<>(List.of(
                Activity.record(ActivityType.CLIENT_CREATED, USER_ID, CLIENT_ID, null, "Created client \"Acme\"", null))));
        
        PageResponse<ActivityResponse> page = service.getTimeline(null, pageable);
        
        verify(activityRepository).findByUserId(USER_ID, pageable);
        assertThat(page.content()).singleElement()
            .satisfies(a -> assertThat(a.actionType()).isEqualTo(ActivityType.CLIENT_CREATED));
    }
    
    @Test
    void perClientTimelineFiltersByClientAndUser() {
        Pageable pageable = PageRequest.of(0, 20);
        when(currentUserProvider.requireUserId()).thenReturn(USER_ID);
        when(activityRepository.findByUserIdAndClientId(USER_ID, CLIENT_ID, pageable))
            .thenReturn(new PageImpl<>(List.of()));
        
        service.getTimeline(CLIENT_ID, pageable);
        
        verify(activityRepository).findByUserIdAndClientId(USER_ID, CLIENT_ID, pageable);
    }
    
    @Test
    void recordClientCreatedPersistsAClientCreatedRow() {
        service.recordClientCreated(USER_ID, CLIENT_ID, "Acme Corp");
        
        ArgumentCaptor<Activity> saved = captureSaved();
        verify(activityRepository).save(saved.capture());
        Activity activity = saved.getValue();
        assertThat(activity.getActionType()).isEqualTo(ActivityType.CLIENT_CREATED);
        assertThat(activity.getUserId()).isEqualTo(USER_ID);
        assertThat(activity.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(activity.getDocumentId()).isNull();
        assertThat(activity.getSummary()).contains("Acme Corp");
        // The recording API never touches the current-user context — the caller supplies the owner id.
        verifyNoInteractions(currentUserProvider);
    }
    
    @Test
    void recordDocumentUploadedPersistsADocumentUploadedRow() {
        service.recordDocumentUploaded(USER_ID, CLIENT_ID, DOCUMENT_ID, "statement.pdf");
        
        ArgumentCaptor<Activity> saved = captureSaved();
        verify(activityRepository).save(saved.capture());
        Activity activity = saved.getValue();
        assertThat(activity.getActionType()).isEqualTo(ActivityType.DOCUMENT_UPLOADED);
        assertThat(activity.getDocumentId()).isEqualTo(DOCUMENT_ID);
        assertThat(activity.getSummary()).contains("statement.pdf");
    }
    
    @Test
    void recordDocumentDeletedPersistsADocumentDeletedRow() {
        service.recordDocumentDeleted(USER_ID, CLIENT_ID, DOCUMENT_ID, "statement.pdf");
        
        ArgumentCaptor<Activity> saved = captureSaved();
        verify(activityRepository).save(saved.capture());
        assertThat(saved.getValue().getActionType()).isEqualTo(ActivityType.DOCUMENT_DELETED);
    }
    
    @Test
    void recordSummaryGeneratedPersistsASummaryGeneratedRow() {
        service.recordSummaryGenerated(USER_ID, null, DOCUMENT_ID);
        
        ArgumentCaptor<Activity> saved = captureSaved();
        verify(activityRepository).save(saved.capture());
        Activity activity = saved.getValue();
        assertThat(activity.getActionType()).isEqualTo(ActivityType.SUMMARY_GENERATED);
        assertThat(activity.getDocumentId()).isEqualTo(DOCUMENT_ID);
        assertThat(activity.getClientId()).isNull();
    }
    
    @Test
    void recordReportCreatedPersistsAReportCreatedRow() {
        service.recordReportCreated(USER_ID, null, DOCUMENT_ID);
        
        ArgumentCaptor<Activity> saved = captureSaved();
        verify(activityRepository).save(saved.capture());
        Activity activity = saved.getValue();
        assertThat(activity.getActionType()).isEqualTo(ActivityType.REPORT_CREATED);
        assertThat(activity.getDocumentId()).isEqualTo(DOCUMENT_ID);
        assertThat(activity.getClientId()).isNull();
    }
}
