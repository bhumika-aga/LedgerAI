package com.ledgerai.documents.port;

/**
 * The bytes to store plus the minimum the adapter needs to store them (ADR-008). Deliberately carries
 * no owner/client identifiers: storage keys are opaque and never derived from user input
 * (SECURITY §9), so the adapter generates the key itself.
 */
public record StorageUpload(byte[] content, String contentType) {
}
