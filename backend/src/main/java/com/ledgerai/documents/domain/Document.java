package com.ledgerai.documents.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata and lifecycle state for an uploaded file (DATABASE §5.3). Holds the external storage
 * reference, never the bytes (ADR-008). A Document belongs to one Client (BR-001); ownership is the
 * Client's owner, enforced in the service via the clients module — this entity carries no user id.
 *
 * <p>Persistence entity only; it never crosses the API boundary and never exposes
 * {@code storageReference} outward (API_SPEC §17.4).
 */
@Entity
@Table(name = "document")
public class Document {
    
    @Id
    private UUID id;
    
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;
    
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    
    @Column(name = "mime_type", nullable = false)
    private String mimeType;
    
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    
    @Column(name = "storage_reference")
    private String storageReference;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_method")
    private ExtractionMethod extractionMethod;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "deleted_at")
    private Instant deletedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected Document() {
        // for JPA
    }
    
    /**
     * FR-UPLD-001: a stored upload enters the lifecycle at {@code UPLOADED} (SRS §7.1). The storage
     * reference is already known because the bytes are persisted to storage before this row is written.
     */
    public static Document create(UUID clientId, String originalFilename, String mimeType, long sizeBytes,
                                  String storageReference) {
        Document document = new Document();
        document.id = UUID.randomUUID();
        document.clientId = clientId;
        document.originalFilename = originalFilename;
        document.mimeType = mimeType;
        document.sizeBytes = sizeBytes;
        document.storageReference = storageReference;
        document.status = DocumentStatus.UPLOADED;
        Instant now = Instant.now();
        document.createdAt = now;
        document.updatedAt = now;
        return document;
    }
    
    /**
     * SRS §7.1: Uploaded → Processing (processing begins). Only legal from {@code UPLOADED}.
     */
    public void markProcessing() {
        requireStatus(DocumentStatus.UPLOADED);
        transitionTo(DocumentStatus.PROCESSING);
    }
    
    /**
     * SRS §7.1: Processing → OCRProcessing (a scan/image is routed to OCR). Only legal from
     * {@code PROCESSING}.
     */
    public void markOcrProcessing() {
        requireStatus(DocumentStatus.PROCESSING);
        transitionTo(DocumentStatus.OCR_PROCESSING);
    }
    
    /**
     * SRS §7.1: Processing → Ready (native text extracted) or OCRProcessing → Ready (OCR succeeded).
     * Records how the text was obtained (BR-014) and clears any prior failure reason.
     */
    public void markReady(ExtractionMethod method) {
        requireStatus(DocumentStatus.PROCESSING, DocumentStatus.OCR_PROCESSING);
        this.extractionMethod = method;
        this.failureReason = null;
        transitionTo(DocumentStatus.READY);
    }
    
    /**
     * SRS §7.1: Processing → Failed or OCRProcessing → Failed (extraction/processing error, FR-OCR-005).
     * The reason is a clear, non-technical message; the document is never presented as Ready.
     */
    public void markFailed(String reason) {
        requireStatus(DocumentStatus.PROCESSING, DocumentStatus.OCR_PROCESSING);
        this.failureReason = reason;
        transitionTo(DocumentStatus.FAILED);
    }
    
    private void transitionTo(DocumentStatus target) {
        this.status = target;
        this.updatedAt = Instant.now();
    }
    
    private void requireStatus(DocumentStatus... allowed) {
        for (DocumentStatus s : allowed) {
            if (this.status == s) {
                return;
            }
        }
        // Guards the documented state machine (SRS §7.1); an out-of-order transition is a bug, not a
        // user error.
        throw new IllegalStateException("Illegal transition from " + this.status);
    }
    
    /**
     * FR-STOR-004: soft-delete (SRS §7.1 → Deleted, DATABASE §8). Idempotent (API_SPEC §8.4). The
     * storage reference is retained on the row; removing the external file is a separate best-effort
     * step in the service.
     */
    public void markDeleted() {
        if (status == DocumentStatus.DELETED) {
            return;
        }
        status = DocumentStatus.DELETED;
        Instant now = Instant.now();
        deletedAt = now;
        updatedAt = now;
    }
    
    public boolean isDeleted() {
        return status == DocumentStatus.DELETED;
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getClientId() {
        return clientId;
    }
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public long getSizeBytes() {
        return sizeBytes;
    }
    
    public String getStorageReference() {
        return storageReference;
    }
    
    public DocumentStatus getStatus() {
        return status;
    }
    
    public ExtractionMethod getExtractionMethod() {
        return extractionMethod;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public Instant getDeletedAt() {
        return deletedAt;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
