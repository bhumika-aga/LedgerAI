package com.ledgerai.documents.port;

import java.time.Instant;

/**
 * A short-lived, authorized download reference (API_SPEC §8.5): the URL and the instant it expires.
 */
public record SignedUrl(String url, Instant expiresAt) {
}
