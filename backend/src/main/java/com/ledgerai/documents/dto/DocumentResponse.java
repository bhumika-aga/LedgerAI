package com.ledgerai.documents.dto;

import com.ledgerai.documents.domain.Document;
import com.ledgerai.documents.domain.DocumentStatus;
import com.ledgerai.documents.domain.ExtractionMethod;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound document representation (API_SPEC §17.4):
 * {@code { id, clientId, originalFilename, mimeType, sizeBytes, status, extractionMethod?, failureReason?, createdAt, updatedAt }}.
 *
 * <p>The internal {@code storageReference} is never exposed (API_SPEC §17.4, SECURITY §9). The static
 * factory is this repository's established entity→DTO mapping convention.
 */
public record DocumentResponse(
    UUID id,
    UUID clientId,
    String originalFilename,
    String mimeType,
    long sizeBytes,
    DocumentStatus status,
    ExtractionMethod extractionMethod,
    String failureReason,
    Instant createdAt,
    Instant updatedAt) {
    
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
            document.getId(),
            document.getClientId(),
            document.getOriginalFilename(),
            document.getMimeType(),
            document.getSizeBytes(),
            document.getStatus(),
            document.getExtractionMethod(),
            document.getFailureReason(),
            document.getCreatedAt(),
            document.getUpdatedAt());
    }
}
