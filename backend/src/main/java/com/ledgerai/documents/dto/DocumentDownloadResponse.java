package com.ledgerai.documents.dto;

import java.time.Instant;

/**
 * Download access reference (API_SPEC §8.5):
 * {@code { downloadUrl, expiresAt, mimeType, originalFilename, sizeBytes }}.
 *
 * <p>A short-lived, owner-scoped link to the stored file — not the bytes themselves (ADR-008). The URL
 * and expiry come from the Storage port; the file metadata comes from the Document row.
 */
public record DocumentDownloadResponse(
    String downloadUrl,
    Instant expiresAt,
    String mimeType,
    String originalFilename,
    long sizeBytes) {
}
