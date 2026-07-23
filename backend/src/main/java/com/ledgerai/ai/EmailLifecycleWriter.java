package com.ledgerai.ai;

import com.ledgerai.activity.ActivityService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The atomic completion boundary for an AI Email draft (DATABASE §11 — the AI-generation unit transitions
 * the request to {@code COMPLETED}, inserts the {@code AIOutput}, <em>and</em> inserts the Activity, all
 * together). It composes the existing, reused writers rather than duplicating their logic:
 * {@link AiRequestLifecycleWriter#completeWithOutput} (request → {@code COMPLETED} + output) and
 * {@link ActivityService#recordEmailGenerated} (the {@code EMAIL_GENERATED} timeline entry). Both are
 * {@code @Transactional} with default propagation, so invoked from this single transaction they join it and
 * commit as one unit — an email draft is never persisted without its activity, nor vice-versa. Mirrors
 * {@link ChatLifecycleWriter}.
 *
 * <p>The provider call itself stays <em>outside</em> this transaction (ADR-010); only persistence of its
 * result is transactional.
 */
@Component
public class EmailLifecycleWriter {
    
    private final AiRequestLifecycleWriter requestLifecycleWriter;
    private final ActivityService activityService;
    
    public EmailLifecycleWriter(AiRequestLifecycleWriter requestLifecycleWriter,
                                ActivityService activityService) {
        this.requestLifecycleWriter = requestLifecycleWriter;
        this.activityService = activityService;
    }
    
    /**
     * Atomically completes the email request with its draft and records the {@code EMAIL_GENERATED} activity
     * (DATABASE §11). {@code clientId}/{@code documentId} are whatever the request referenced (either may be
     * null — email context is optional).
     */
    @Transactional
    public void completeWithDraftAndActivity(UUID requestId, String content, UUID userId,
                                             UUID clientId, UUID documentId) {
        requestLifecycleWriter.completeWithOutput(requestId, content);
        activityService.recordEmailGenerated(userId, clientId, documentId);
    }
}
