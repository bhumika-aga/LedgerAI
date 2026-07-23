package com.ledgerai.ai;

import com.ledgerai.ai.domain.AiOutput;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link AiOutput}. Data access only — no business rules (BACKEND_CODING_STANDARDS §4).
 * The row exists (1:1) only once its {@link AiRequest} has reached {@code COMPLETED}.
 */
public interface AiOutputRepository extends JpaRepository<AiOutput, UUID> {
    
    Optional<AiOutput> findByAiRequestId(UUID aiRequestId);
    
    /**
     * Batch-loads the outputs for a page of requests (e.g. a chat thread, API_SPEC §11.2) so the answers
     * can be attached without an N+1 lookup per request.
     */
    List<AiOutput> findByAiRequestIdIn(Collection<UUID> aiRequestIds);
}
