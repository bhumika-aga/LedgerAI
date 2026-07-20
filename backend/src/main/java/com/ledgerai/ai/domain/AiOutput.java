package com.ledgerai.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * The produced, <strong>editable</strong> result of a completed {@link AiRequest} (DATABASE §5.6,
 * BR-031). 1:1 with its request; created only when the request reaches {@code COMPLETED}. The
 * {@code edited} flag records whether the professional has modified the AI-produced content — AI is
 * assistive, never the system of record (BR-032). Persistence entity only.
 */
@Entity
@Table(name = "ai_output")
public class AiOutput {
    
    @Id
    private UUID id;
    
    @Column(name = "ai_request_id", nullable = false, unique = true, updatable = false)
    private UUID aiRequestId;
    
    @Column(name = "content", nullable = false)
    private String content;
    
    @Column(name = "edited", nullable = false)
    private boolean edited;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected AiOutput() {
        // for JPA
    }
    
    /**
     * Records the AI-produced content for a completed request; {@code edited} starts {@code false}.
     */
    public static AiOutput create(UUID aiRequestId, String content) {
        AiOutput output = new AiOutput();
        output.id = UUID.randomUUID();
        output.aiRequestId = aiRequestId;
        output.content = content;
        output.edited = false;
        Instant now = Instant.now();
        output.createdAt = now;
        output.updatedAt = now;
        return output;
    }
    
    /**
     * Persists a user edit (BR-031, human-in-the-loop). Sets {@code edited = true} so the API can report
     * that the content is no longer the raw AI output.
     */
    public void edit(String newContent) {
        this.content = newContent;
        this.edited = true;
        this.updatedAt = Instant.now();
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getAiRequestId() {
        return aiRequestId;
    }
    
    public String getContent() {
        return content;
    }
    
    public boolean isEdited() {
        return edited;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
