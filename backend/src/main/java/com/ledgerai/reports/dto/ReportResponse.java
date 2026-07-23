package com.ledgerai.reports.dto;

import com.ledgerai.reports.domain.Report;
import com.ledgerai.reports.domain.ReportStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbound report representation (API_SPEC §17.6):
 * {@code { id, documentId, title?, content, status, createdAt, updatedAt }}. It exposes the editable
 * content and never the owning {@code userId} (the report is already the caller's own).
 */
public record ReportResponse(
    UUID id,
    UUID documentId,
    String title,
    String content,
    ReportStatus status,
    Instant createdAt,
    Instant updatedAt) {
    
    public static ReportResponse from(Report report) {
        return new ReportResponse(
            report.getId(),
            report.getDocumentId(),
            report.getTitle(),
            report.getContent(),
            report.getStatus(),
            report.getCreatedAt(),
            report.getUpdatedAt());
    }
}
