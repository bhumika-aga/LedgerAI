package com.ledgerai.documents;

import com.ledgerai.documents.domain.DocumentContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link DocumentContent}. Data access only — no business rules
 * (BACKEND_CODING_STANDARDS §4). The row exists (1:1) only once extraction has succeeded.
 */
public interface DocumentContentRepository extends JpaRepository<DocumentContent, UUID> {
    
    Optional<DocumentContent> findByDocumentId(UUID documentId);
}
