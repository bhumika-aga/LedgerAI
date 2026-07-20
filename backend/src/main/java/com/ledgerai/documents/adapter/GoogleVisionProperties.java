package com.ledgerai.documents.adapter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for the Google Cloud Vision OCR adapter (ADR-009). All values are environment-driven
 * and server-held; the API key is a secret with no default and MUST be supplied by the environment,
 * never committed (SECURITY §13). Provider-specific and confined to the adapter package.
 *
 * @param apiUrl                  the Vision REST base, e.g. {@code https://vision.googleapis.com}
 * @param apiKey                  the server-side API key authorizing Vision requests
 * @param highConfidenceThreshold provider confidence at/above which extraction is rated {@code HIGH}
 */
@ConfigurationProperties(prefix = "ocr.google-vision")
public record GoogleVisionProperties(
    @DefaultValue("https://vision.googleapis.com") String apiUrl,
    String apiKey,
    @DefaultValue("0.8") double highConfidenceThreshold) {
}
