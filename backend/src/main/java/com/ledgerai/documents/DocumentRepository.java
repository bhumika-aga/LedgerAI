package com.ledgerai.documents;

import com.ledgerai.documents.domain.Document;
import com.ledgerai.documents.domain.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link Document}. Data access only — no business rules (BACKEND_CODING_STANDARDS §4).
 *
 * <p>Every finder excludes soft-deleted rows by construction: FR-STOR-005 forbids returning a deleted
 * document through <em>any</em> retrieval path, so there is deliberately no plain {@code findById} in
 * the read paths and no finder that can surface {@code DELETED}. The partial index backs the default
 * per-client listing (DATABASE §9, §8).
 */
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    
    Page<Document> findByClientIdAndStatusNot(UUID clientId, DocumentStatus excluded, Pageable pageable);
    
    Page<Document> findByClientIdAndStatus(UUID clientId, DocumentStatus status, Pageable pageable);
    
    Optional<Document> findByIdAndStatusNot(UUID id, DocumentStatus excluded);
}
