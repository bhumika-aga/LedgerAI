package com.ledgerai.ai;

import com.ledgerai.activity.ActivityService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The atomic completion boundary for an AI Chat exchange (DATABASE §11 — the AI-generation unit
 * transitions the request to {@code COMPLETED}, inserts the {@code AIOutput}, <em>and</em> inserts the
 * Activity, all together). It composes the existing, reused writers rather than duplicating their logic:
 * {@link AiRequestLifecycleWriter#completeWithOutput} (request → {@code COMPLETED} + output) and
 * {@link ActivityService#recordChatMessageSent} (the {@code CHAT_MESSAGE_SENT} timeline entry). Both are
 * {@code @Transactional} with default propagation, so invoked from this single transaction they join it
 * and commit as one unit — a chat answer is never persisted without its activity, nor vice versa.
 *
 * <p>The provider call itself stays <em>outside</em> this transaction (ADR-010); only persistence of its
 * result is transactional. This is deliberately tighter than the AI-Summary flow, which emits its activity
 * immediately after (non-atomically) — here the AI-generation atomic unit of DATABASE §11 is honored
 * literally, since chat is a fresh implementation.
 */
@Component
public class ChatLifecycleWriter {
    
    private final AiRequestLifecycleWriter requestLifecycleWriter;
    private final ActivityService activityService;
    
    public ChatLifecycleWriter(AiRequestLifecycleWriter requestLifecycleWriter,
                               ActivityService activityService) {
        this.requestLifecycleWriter = requestLifecycleWriter;
        this.activityService = activityService;
    }
    
    /**
     * Atomically completes the chat request with its grounded answer and records the chat activity
     * (DATABASE §11). The activity is document-scoped ({@code clientId} null), matching the summary event.
     */
    @Transactional
    public void completeWithAnswerAndActivity(UUID requestId, String content, UUID userId, UUID documentId) {
        requestLifecycleWriter.completeWithOutput(requestId, content);
        activityService.recordChatMessageSent(userId, null, documentId);
    }
}
