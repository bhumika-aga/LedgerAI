package com.ledgerai.ai.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A single AI Action invocation with its own lifecycle and attempt record (DATABASE §5.5, SRS §7.2,
 * ADR-010). Kept separate from its editable {@link AiOutput} so failed attempts are recorded without
 * bloating output rows (DATABASE §3.1). Ownership is the requesting {@code userId} (BR-004); the
 * document-bound types also reference the source {@code documentId}.
 *
 * <p>Persistence entity only; it never crosses the API boundary (a DTO does). For AI Summary the
 * {@code prompt} column is null — summary takes no user question (only chat/email carry a prompt,
 * VR-007), and the composed grounded prompt is not persisted as content (AI_ARCHITECTURE §14: prompt
 * content is not logged/stored as operational data).
 */
@Entity
@Table(name = "ai_request")
public class AiRequest {
    
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    
    @Column(name = "document_id", updatable = false)
    private UUID documentId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private AiRequestType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AiRequestStatus status;
    
    @Column(name = "prompt", updatable = false)
    private String prompt;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected AiRequest() {
        // for JPA
    }
    
    /**
     * A new AI Summary request for a document, entering the lifecycle at {@code REQUESTED} (SRS §7.2).
     * Only created when the source document is {@code READY} (BR-010) — that precondition is enforced by
     * the service, not this factory.
     */
    public static AiRequest createSummary(UUID userId, UUID documentId) {
        AiRequest request = new AiRequest();
        request.id = UUID.randomUUID();
        request.userId = userId;
        request.documentId = documentId;
        request.type = AiRequestType.SUMMARY;
        request.status = AiRequestStatus.REQUESTED;
        request.prompt = null;
        Instant now = Instant.now();
        request.createdAt = now;
        request.updatedAt = now;
        return request;
    }
    
    /**
     * A new AI Chat request for a document (AI Chat, SRS §4.8; DATABASE §3.1 chat note), entering the
     * lifecycle at {@code REQUESTED}. The user's {@code question} is retained as the request
     * {@code prompt} (the documented {@code prompt?} field — chat/email carry it, VR-007), so a chat
     * exchange records both the question and, on completion, its grounded answer ({@link AiOutput}).
     * Only created when the source document is {@code READY} (BR-010); that precondition is enforced by
     * the service, not this factory.
     */
    public static AiRequest createChat(UUID userId, UUID documentId, String question) {
        AiRequest request = new AiRequest();
        request.id = UUID.randomUUID();
        request.userId = userId;
        request.documentId = documentId;
        request.type = AiRequestType.CHAT;
        request.status = AiRequestStatus.REQUESTED;
        request.prompt = question;
        Instant now = Instant.now();
        request.createdAt = now;
        request.updatedAt = now;
        return request;
    }
    
    /**
     * SRS §7.2: Requested → InProgress (generation started). Only legal from {@code REQUESTED}.
     */
    public void markInProgress() {
        requireStatus(AiRequestStatus.REQUESTED);
        transitionTo(AiRequestStatus.IN_PROGRESS);
    }
    
    /**
     * SRS §7.2: InProgress → Completed (valid output produced). Clears any prior failure reason.
     */
    public void markCompleted() {
        requireStatus(AiRequestStatus.IN_PROGRESS);
        this.failureReason = null;
        transitionTo(AiRequestStatus.COMPLETED);
    }
    
    /**
     * SRS §7.2: InProgress → Failed (provider unavailable or invalid output, AI_ARCHITECTURE §11–12).
     * The reason is a clear, non-technical message; the request is never presented as completed.
     */
    public void markFailed(String reason) {
        requireStatus(AiRequestStatus.IN_PROGRESS);
        this.failureReason = reason;
        transitionTo(AiRequestStatus.FAILED);
    }
    
    private void transitionTo(AiRequestStatus target) {
        this.status = target;
        this.updatedAt = Instant.now();
    }
    
    private void requireStatus(AiRequestStatus... allowed) {
        for (AiRequestStatus s : allowed) {
            if (this.status == s) {
                return;
            }
        }
        // Guards the documented state machine (SRS §7.2); an out-of-order transition is a bug.
        throw new IllegalStateException("Illegal transition from " + this.status);
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public UUID getDocumentId() {
        return documentId;
    }
    
    public AiRequestType getType() {
        return type;
    }
    
    public AiRequestStatus getStatus() {
        return status;
    }
    
    public String getPrompt() {
        return prompt;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
