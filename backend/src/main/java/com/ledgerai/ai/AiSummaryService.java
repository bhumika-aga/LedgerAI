package com.ledgerai.ai;

import com.ledgerai.activity.ActivityService;
import com.ledgerai.ai.domain.AiOutput;
import com.ledgerai.ai.domain.AiRequest;
import com.ledgerai.ai.domain.AiRequestStatus;
import com.ledgerai.ai.domain.AiRequestType;
import com.ledgerai.ai.dto.AiResponse;
import com.ledgerai.ai.port.AiCompletion;
import com.ledgerai.ai.port.AiPort;
import com.ledgerai.ai.port.AiPrompt;
import com.ledgerai.ai.port.AiUnavailableException;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.documents.DocumentContentView;
import com.ledgerai.documents.DocumentService;
import com.ledgerai.documents.domain.DocumentStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * AI Summary business rules and orchestration (SRS §4.7 AI Summary; API_SPEC §10; AI_ARCHITECTURE §4–§12).
 *
 * <p><strong>Grounding + preconditions.</strong> A summary is generated only for a document the caller
 * owns (ownership reused from the documents module via the published
 * {@link DocumentService#requireOwnedContentForAi}, never re-implemented — CLAUDE.md) that is
 * {@code READY} (BR-010) and has extracted text to ground in (AI_ARCHITECTURE §9). A non-owned/unknown
 * document is {@code 404}; a non-{@code READY} document is {@code 409}; a {@code READY} document with no
 * text is {@code 422}.
 *
 * <p><strong>Provider isolation.</strong> The model is reached only through the {@link AiPort}; the
 * grounded prompt is composed centrally by {@link SummaryPromptBuilder}. No provider type is visible
 * here. The provider call sits <em>outside</em> any database transaction (ADR-010); each lifecycle write
 * is delegated to {@link AiRequestLifecycleWriter} so only persistence is transactional and intermediate
 * states are observable via the summary poll (API_SPEC §2.11).
 *
 * <p><strong>Failure handling</strong> (AI_ARCHITECTURE §11–§12). A provider outage marks the request
 * {@code FAILED} and surfaces {@code 503}; an empty/invalid model response is an output-validation
 * failure — the request is marked {@code FAILED} with a clear reason and returned as a terminal resource
 * (the poll target shows it), never a fabricated success.
 */
@Service
public class AiSummaryService {
    
    private static final String NOT_READY_MESSAGE =
        "The document is not ready. A summary can only be generated once text extraction has completed.";
    private static final String NO_TEXT_MESSAGE =
        "The document has no extractable text to summarize.";
    private static final String PROVIDER_UNAVAILABLE_MESSAGE =
        "The summary could not be generated because the AI service was unavailable. Please try again.";
    private static final String INVALID_OUTPUT_REASON =
        "The AI service did not return a usable summary. Please try again.";
    private static final String PROVIDER_UNAVAILABLE_REASON =
        "The AI service was unavailable while generating the summary. Please try again.";
    
    private final DocumentService documentService;
    private final AiRequestRepository requestRepository;
    private final AiOutputRepository outputRepository;
    private final AiRequestLifecycleWriter lifecycleWriter;
    private final SummaryPromptBuilder promptBuilder;
    private final AiPort aiPort;
    private final CurrentUserProvider currentUserProvider;
    private final ActivityService activityService;
    
    public AiSummaryService(DocumentService documentService, AiRequestRepository requestRepository,
                            AiOutputRepository outputRepository, AiRequestLifecycleWriter lifecycleWriter,
                            SummaryPromptBuilder promptBuilder, AiPort aiPort,
                            CurrentUserProvider currentUserProvider, ActivityService activityService) {
        this.documentService = documentService;
        this.requestRepository = requestRepository;
        this.outputRepository = outputRepository;
        this.lifecycleWriter = lifecycleWriter;
        this.promptBuilder = promptBuilder;
        this.aiPort = aiPort;
        this.currentUserProvider = currentUserProvider;
        this.activityService = activityService;
    }
    
    /**
     * API_SPEC §10.1 (FR-SUMM-001): generate the summary of a {@code READY} document, grounded in its
     * extracted text. When a completed summary already exists and {@code regenerate} is false, the
     * existing one is returned (idempotent); otherwise a fresh attempt runs. Processing is
     * synchronous-with-status (ADR-013), so a terminal resource is returned directly.
     */
    public AiResponse generate(UUID documentId, boolean regenerate) {
        DocumentContentView document = documentService.requireOwnedContentForAi(documentId);
        if (document.status() != DocumentStatus.READY) {
            throw new DocumentNotReadyException(NOT_READY_MESSAGE);
        }
        String extractedText = document.extractedText();
        if (extractedText == null || extractedText.isBlank()) {
            throw new NoExtractableTextException(NO_TEXT_MESSAGE);
        }
        
        if (!regenerate) {
            AiRequest existing = latestSummary(documentId);
            if (existing != null && existing.getStatus() == AiRequestStatus.COMPLETED) {
                return responseFor(existing);
            }
        }
        
        UUID userId = currentUserProvider.requireUserId();
        AiRequest request = lifecycleWriter.createRequested(userId, documentId);
        lifecycleWriter.markInProgress(request.getId());
        
        // The provider call is outside any DB transaction (ADR-010); only the result is persisted.
        AiPrompt prompt = promptBuilder.build(extractedText);
        try {
            AiCompletion completion = aiPort.generate(prompt);
            String content = completion == null ? null : completion.text();
            if (content == null || content.isBlank()) {
                // Output validation (AI_ARCHITECTURE §11): an empty response is a failure, not a success.
                lifecycleWriter.markFailed(request.getId(), INVALID_OUTPUT_REASON);
            } else {
                lifecycleWriter.completeWithOutput(request.getId(), content);
                // API_SPEC §10.1: emit SUMMARY_GENERATED only on a successful generation (not on the
                // existing-summary short-circuit, nor on a failed/empty attempt). The summary is
                // document-scoped; client_id is left null (Activity §5.8 allows it).
                activityService.recordSummaryGenerated(userId, null, documentId);
            }
        } catch (AiUnavailableException e) {
            // Record the failed attempt, then surface a 503 (graceful degradation, AI_ARCHITECTURE §12).
            lifecycleWriter.markFailed(request.getId(), PROVIDER_UNAVAILABLE_REASON);
            throw new AiUnavailableException(PROVIDER_UNAVAILABLE_MESSAGE, e);
        }
        
        return responseFor(reload(request.getId()));
    }
    
    /**
     * API_SPEC §10.2 (FR-SUMM-004): the saved summary and its status for a document — also the async poll
     * target. Ownership first (404 if unknown/non-owned); {@code 404} if no summary exists.
     */
    public AiResponse get(UUID documentId) {
        documentService.requireOwnedContentForAi(documentId);
        AiRequest request = latestSummary(documentId);
        if (request == null) {
            throw new ResourceNotFoundException();
        }
        return responseFor(request);
    }
    
    /**
     * API_SPEC §10.3 (FR-SUMM, BR-031): persist the user's edit to the summary content (human-in-the-loop).
     * Ownership first; {@code 404} if no summary/output exists. Blank content is rejected at the boundary
     * ({@code 422}, EditSummaryRequest).
     */
    public AiResponse edit(UUID documentId, String content) {
        documentService.requireOwnedContentForAi(documentId);
        AiRequest request = latestSummary(documentId);
        if (request == null) {
            throw new ResourceNotFoundException();
        }
        AiOutput output = outputRepository.findByAiRequestId(request.getId())
                              .orElseThrow(ResourceNotFoundException::new);
        AiOutput edited = lifecycleWriter.editOutput(output, content);
        return AiResponse.from(request, edited);
    }
    
    private AiRequest latestSummary(UUID documentId) {
        return requestRepository
                   .findFirstByDocumentIdAndTypeOrderByCreatedAtDesc(documentId, AiRequestType.SUMMARY)
                   .orElse(null);
    }
    
    private AiRequest reload(UUID requestId) {
        return requestRepository.findById(requestId).orElseThrow(ResourceNotFoundException::new);
    }
    
    private AiResponse responseFor(AiRequest request) {
        AiOutput output = request.getStatus() == AiRequestStatus.COMPLETED
                              ? outputRepository.findByAiRequestId(request.getId()).orElse(null)
                              : null;
        return AiResponse.from(request, output);
    }
}
