package com.ledgerai.documents;

/**
 * A document upload, expressed without web types so the service stays free of HTTP concerns
 * (BACKEND_CODING_STANDARDS §4). The controller reads the multipart file into this command; the
 * {@code declaredContentType} is the client's claim, which the validator does <em>not</em> trust on its
 * own (SECURITY §9).
 */
public record UploadCommand(String originalFilename, String declaredContentType, byte[] content) {
}
