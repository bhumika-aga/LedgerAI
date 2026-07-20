package com.ledgerai.documents.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * The Extracted Text for a Document, stored apart from hot metadata (DATABASE §5.4). 1:1 with its
 * Document. Created only when extraction succeeds (native or OCR) — never for a document that failed
 * extraction, so its presence means "text is available". Persistence entity only.
 */
@Entity
@Table(name = "document_content")
public class DocumentContent {
    
    @Id
    private UUID id;
    
    @Column(name = "document_id", nullable = false, unique = true, updatable = false)
    private UUID documentId;
    
    @Column(name = "extracted_text")
    private String extractedText;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_quality")
    private ExtractionQuality extractionQuality;
    
    @Column(name = "char_count")
    private Integer charCount;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    protected DocumentContent() {
        // for JPA
    }
    
    /**
     * Records successful extraction. {@code charCount} is the length of the extracted text (DATABASE §5.4).
     */
    public static DocumentContent create(UUID documentId, String extractedText, ExtractionQuality quality) {
        DocumentContent content = new DocumentContent();
        content.id = UUID.randomUUID();
        content.documentId = documentId;
        content.extractedText = extractedText;
        content.extractionQuality = quality;
        content.charCount = extractedText == null ? null : extractedText.length();
        Instant now = Instant.now();
        content.createdAt = now;
        content.updatedAt = now;
        return content;
    }
    
    public UUID getId() {
        return id;
    }
    
    public UUID getDocumentId() {
        return documentId;
    }
    
    public String getExtractedText() {
        return extractedText;
    }
    
    public ExtractionQuality getExtractionQuality() {
        return extractionQuality;
    }
    
    public Integer getCharCount() {
        return charCount;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
