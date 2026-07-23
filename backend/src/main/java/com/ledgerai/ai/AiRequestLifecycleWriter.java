package com.ledgerai.ai;

import com.ledgerai.ai.domain.AiOutput;
import com.ledgerai.ai.domain.AiRequest;
import com.ledgerai.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The transactional boundary for AI Request lifecycle writes during generation (DATABASE §11, ADR-010).
 *
 * <p>It is a separate bean on purpose: {@link AiSummaryService} orchestrates across the external
 * provider call, which MUST NOT be held inside a DB transaction (ADR-010 — "the provider call sits
 * outside the database transaction; only persistence of its result is transactional"). Each atomic
 * state change is delegated here so it commits in its own transaction through the Spring proxy, which is
 * also what makes intermediate states ({@code REQUESTED}, {@code IN_PROGRESS}) observable via the
 * summary poll while generation runs. Only {@link #completeWithOutput} is multi-write and is atomic
 * (request transitioned to {@code COMPLETED} together with its {@code AIOutput}, DATABASE §11).
 */
@Component
public class AiRequestLifecycleWriter {
    
    private final AiRequestRepository requestRepository;
    private final AiOutputRepository outputRepository;
    
    public AiRequestLifecycleWriter(AiRequestRepository requestRepository, AiOutputRepository outputRepository) {
        this.requestRepository = requestRepository;
        this.outputRepository = outputRepository;
    }
    
    @Transactional
    public AiRequest createRequested(UUID userId, UUID documentId) {
        return requestRepository.save(AiRequest.createSummary(userId, documentId));
    }
    
    /**
     * Creates a new {@code CHAT} request at {@code REQUESTED} carrying the user's question (AI Chat,
     * SRS §4.8). Parallel to {@link #createRequested} for summaries — the same lifecycle, a different
     * {@link AiRequestType} and a retained prompt.
     */
    @Transactional
    public AiRequest createChatRequested(UUID userId, UUID documentId, String question) {
        return requestRepository.save(AiRequest.createChat(userId, documentId, question));
    }
    
    /**
     * Creates a new {@code EMAIL} request at {@code REQUESTED} carrying the user's instruction (AI Email,
     * SRS §4.9). Parallel to the summary/chat creators — the same lifecycle, a different
     * {@link AiRequestType}, and an optional {@code documentId} (email context is optional, API_SPEC §12.1).
     */
    @Transactional
    public AiRequest createEmailRequested(UUID userId, UUID documentId, String instruction) {
        return requestRepository.save(AiRequest.createEmail(userId, documentId, instruction));
    }
    
    @Transactional
    public void markInProgress(UUID requestId) {
        AiRequest request = require(requestId);
        request.markInProgress();
        requestRepository.save(request);
    }
    
    /**
     * Atomically transitions the request to {@code COMPLETED} and persists its editable output
     * (DATABASE §5.6, §11) — the two are one unit of work, so a request is never {@code COMPLETED}
     * without its output, nor an output without a completed request.
     */
    @Transactional
    public void completeWithOutput(UUID requestId, String content) {
        AiRequest request = require(requestId);
        request.markCompleted();
        requestRepository.save(request);
        outputRepository.save(AiOutput.create(requestId, content));
    }
    
    @Transactional
    public void markFailed(UUID requestId, String reason) {
        AiRequest request = require(requestId);
        request.markFailed(reason);
        requestRepository.save(request);
    }
    
    /**
     * Persists a user edit to a completed summary's output (BR-031). Transactional on its own.
     */
    @Transactional
    public AiOutput editOutput(AiOutput output, String content) {
        output.edit(content);
        return outputRepository.save(output);
    }
    
    private AiRequest require(UUID requestId) {
        return requestRepository.findById(requestId).orElseThrow(ResourceNotFoundException::new);
    }
}
