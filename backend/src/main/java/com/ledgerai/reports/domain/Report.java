package com.ledgerai.reports.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A saved, editable report generated from a single Document (DATABASE §5.7; SRS §4.10). The
 * {@code content} is AI-generated (via the AI port) but stored here, in its own table — reports are
 * first-class resources, not {@code AIOutput} rows (DATABASE §11 gives report generation its own
 * transaction boundary: insert Report + insert Activity). Single-document in V1 (BR-035); owner-scoped by
 * {@code userId} (owner scoping); editable and review-required (BR-031/032).
 *
 * <p>Persistence entity only; it never crosses the API boundary (a DTO does).
 */
@Entity
@Table(name = "report")
public class Report {
    
    @Id
    private UUID id;
    
    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;
    
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "content", nullable = false)
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected Report() {
        // for JPA
    }
    
    /**
     * A newly generated report enters as {@code DRAFT} (SRS §7.3) with the AI-produced {@code content}.
     * Only created for a {@code READY} document (BR-010/BR-035) — that precondition is enforced by the
     * service, not this factory.
     */
    public static Report createDraft(UUID userId, UUID documentId, String title, String content) {
        Report report = new Report();
        report.id = UUID.randomUUID();
        report.userId = userId;
        report.documentId = documentId;
        report.title = title;
        report.content = content;
        report.status = ReportStatus.DRAFT;
        Instant now = Instant.now();
        report.createdAt = now;
        report.updatedAt = now;
        return report;
    }
    
    /**
     * Applies a partial edit (API_SPEC §13.4, FR-RPT-003): each argument is optional — a {@code null} value
     * leaves that field unchanged. {@code status} may move {@code DRAFT → SAVED} (or back). Touches
     * {@code updatedAt} only when something actually changed.
     */
    public void applyUpdate(String newTitle, String newContent, ReportStatus newStatus) {
        boolean changed = false;
        if (newTitle != null && !newTitle.equals(this.title)) {
            this.title = newTitle;
            changed = true;
        }
        if (newContent != null && !newContent.equals(this.content)) {
            this.content = newContent;
            changed = true;
        }
        if (newStatus != null && newStatus != this.status) {
            this.status = newStatus;
            changed = true;
        }
        if (changed) {
            this.updatedAt = Instant.now();
        }
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getDocumentId() {
        return documentId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getContent() {
        return content;
    }
    
    public ReportStatus getStatus() {
        return status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
