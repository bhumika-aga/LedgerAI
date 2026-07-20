package com.ledgerai.ai;

import com.ledgerai.ai.domain.AiRequest;
import com.ledgerai.ai.domain.AiRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link AiRequest}. Data access only — no business rules (BACKEND_CODING_STANDARDS §4).
 *
 * <p>A document may accumulate several summary requests over time (regeneration, past failures); the
 * "current" summary is the most recent request for the document, so retrieval reads the latest by
 * creation time. Ownership is already enforced upstream via the document (the requester owns the
 * document's client), so finders key on {@code documentId} + {@code type}.
 */
public interface AiRequestRepository extends JpaRepository<AiRequest, UUID> {
    
    Optional<AiRequest> findFirstByDocumentIdAndTypeOrderByCreatedAtDesc(UUID documentId, AiRequestType type);
}
