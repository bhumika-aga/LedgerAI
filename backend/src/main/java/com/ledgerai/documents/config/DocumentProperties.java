package com.ledgerai.documents.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;

/**
 * Externalized upload validation limits (VR-005).
 *
 * <p>VR-005 marks the allowed types and maximum size as an {@code [Assumption, to be fixed in
 * SRS/architecture]} and they are not finalized, so — as with the password (VR-001), profile (VR-003)
 * and client (VR-004) thresholds — they live here as tunable configuration, not product commitments in
 * code. Defaults cover the financial-document formats the product targets (PDF and common scan images).
 */
@ConfigurationProperties(prefix = "documents")
public record DocumentProperties(
    @DefaultValue("25MB") DataSize maxFileSize,
    @DefaultValue({"application/pdf", "image/png", "image/jpeg"}) List<String> allowedMimeTypes,
    @DefaultValue("5m") Duration downloadUrlTtl,
    // Native-first threshold (ADR-009): the minimum embedded-text length that counts as "sufficient"
    // native text, below which the document is routed to OCR. No document fixes this value, so it is
    // tunable configuration, not a product commitment — like the other [Assumption] thresholds.
    @DefaultValue("16") int nativeMinChars) {
}
