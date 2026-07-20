package com.ledgerai.documents;

import com.ledgerai.documents.domain.DocumentStatus;

import java.util.UUID;

/**
 * A published, read-only projection of a document and its extracted text for AI grounding — the
 * documents module's public surface for other modules that need a document's grounded content (e.g. AI
 * Summary). Cross-module interaction goes through this published type, never the module's entities or
 * repositories (CLAUDE.md — keep modules independent; ARCHITECTURE §5).
 *
 * <p>It carries only what a grounded AI action needs: the document's identity, its lifecycle
 * {@code status} (so the caller can enforce the {@code READY} precondition, BR-010), and the extracted
 * text (which may be null/blank). It never exposes the storage reference or other internal metadata.
 */
public record DocumentContentView(UUID documentId, DocumentStatus status, String extractedText) {
}
