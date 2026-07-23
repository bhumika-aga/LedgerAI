package com.ledgerai.activity;

import com.ledgerai.activity.domain.Activity;
import com.ledgerai.activity.domain.ActivityType;
import com.ledgerai.activity.dto.ActivityResponse;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The Activity Timeline: the single shared service that both <strong>records</strong> activities (called
 * by the mutating modules) and <strong>reads</strong> the timeline (API_SPEC §15; SRS §4.12).
 *
 * <p><strong>One place owns activity persistence.</strong> Client, Document, and AI Summary modules do
 * not write activity rows themselves — they call the {@code record…} methods here, so there is exactly
 * one persistence path and no duplication. Each recording method is transactional with default
 * propagation, so when it is invoked inside a mutation's transaction (e.g. client create, document
 * delete) the activity commits <em>together</em> with the mutation (DATABASE §11); when invoked outside a
 * transaction (the storage-first upload flow, the provider-outside-transaction summary flow) it commits
 * in its own transaction immediately after the mutation.
 *
 * <p><strong>Ownership.</strong> Reads are scoped to the authenticated user (BR-006), so a user only ever
 * sees their own activities — isolation is enforced at the query, not inferred, and no ownership logic is
 * duplicated from other modules. The optional {@code clientId} narrows to a per-client view; because the
 * query is already user-scoped, a {@code clientId} the caller does not own simply yields an empty page
 * (no cross-user leak, SECURITY §5). The log is <strong>read-only</strong> over the API — there are no
 * create/update/delete endpoints (FR-TMLN-004, BR-016).
 */
@Service
public class ActivityService {
    
    private final ActivityRepository activityRepository;
    private final CurrentUserProvider currentUserProvider;
    
    public ActivityService(ActivityRepository activityRepository, CurrentUserProvider currentUserProvider) {
        this.activityRepository = activityRepository;
        this.currentUserProvider = currentUserProvider;
    }
    
    /**
     * API_SPEC §15.1 (FR-TMLN-002): the caller's chronological timeline, newest first by default, paged.
     * Owner-scoped by the authenticated user; an optional {@code clientId} narrows to that client's view.
     */
    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> getTimeline(UUID clientId, Pageable pageable) {
        UUID userId = currentUserProvider.requireUserId();
        Page<Activity> page = clientId == null
                                  ? activityRepository.findByUserId(userId, pageable)
                                  : activityRepository.findByUserIdAndClientId(userId, clientId, pageable);
        return PageResponse.from(page, ActivityResponse::from);
    }
    
    /**
     * Records {@code CLIENT_CREATED} (API_SPEC §7.3). Called from the client-create transaction, so it
     * commits with the new client (DATABASE §11).
     */
    @Transactional
    public void recordClientCreated(UUID userId, UUID clientId, String clientName) {
        activityRepository.save(Activity.record(ActivityType.CLIENT_CREATED, userId, clientId, null,
            "Created client \"" + clientName + "\"", null));
    }
    
    /**
     * Records {@code DOCUMENT_UPLOADED} (API_SPEC §8.1).
     */
    @Transactional
    public void recordDocumentUploaded(UUID userId, UUID clientId, UUID documentId, String filename) {
        activityRepository.save(Activity.record(ActivityType.DOCUMENT_UPLOADED, userId, clientId, documentId,
            "Uploaded document \"" + filename + "\"", null));
    }
    
    /**
     * Records {@code DOCUMENT_DELETED} (API_SPEC §8.4). Called from the delete transaction.
     */
    @Transactional
    public void recordDocumentDeleted(UUID userId, UUID clientId, UUID documentId, String filename) {
        activityRepository.save(Activity.record(ActivityType.DOCUMENT_DELETED, userId, clientId, documentId,
            "Deleted document \"" + filename + "\"", null));
    }
    
    /**
     * Records {@code SUMMARY_GENERATED} (API_SPEC §10.1).
     */
    @Transactional
    public void recordSummaryGenerated(UUID userId, UUID clientId, UUID documentId) {
        activityRepository.save(Activity.record(ActivityType.SUMMARY_GENERATED, userId, clientId, documentId,
            "Generated an AI summary", null));
    }
    
    /**
     * Records {@code REPORT_CREATED} (API_SPEC §13.1). Called from the report-generation transaction, so it
     * commits together with the new report (DATABASE §11).
     */
    @Transactional
    public void recordReportCreated(UUID userId, UUID clientId, UUID documentId) {
        activityRepository.save(Activity.record(ActivityType.REPORT_CREATED, userId, clientId, documentId,
            "Generated a report", null));
    }
    
    /**
     * Records {@code CHAT_MESSAGE_SENT} (API_SPEC §11.1 — "emits chat activity"). Called from the chat
     * completion transaction so it commits together with the request/output (the AI-generation atomic
     * unit, DATABASE §11). The exchange is document-scoped, so {@code clientId} is left null (as with the
     * summary event).
     */
    @Transactional
    public void recordChatMessageSent(UUID userId, UUID clientId, UUID documentId) {
        activityRepository.save(Activity.record(ActivityType.CHAT_MESSAGE_SENT, userId, clientId, documentId,
            "Asked a question about a document", null));
    }
}
