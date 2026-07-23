package com.ledgerai.ai;

import com.ledgerai.ai.config.ChatProperties;
import com.ledgerai.ai.domain.AiOutput;
import com.ledgerai.ai.domain.AiRequest;
import com.ledgerai.ai.domain.AiRequestStatus;
import com.ledgerai.ai.domain.AiRequestType;
import com.ledgerai.ai.dto.AiResponse;
import com.ledgerai.ai.port.AiCompletion;
import com.ledgerai.ai.port.AiPort;
import com.ledgerai.ai.port.AiPrompt;
import com.ledgerai.ai.port.AiUnavailableException;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.documents.DocumentContentView;
import com.ledgerai.documents.DocumentService;
import com.ledgerai.documents.domain.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI Chat business rules and orchestration (SRS §4.8 AI Chat; API_SPEC §11; AI_ARCHITECTURE §4–§12).
 *
 * <p>MVP chat is <strong>document-scoped</strong> (API_SPEC §11; DATABASE §3.1 chat note): a question is a
 * {@code CHAT} {@link AiRequest} on a document, its grounded answer the request's {@link AiOutput}. There
 * is no separate conversation resource; the "thread" (FR-CHAT-004) is simply the document's {@code CHAT}
 * exchanges in chronological order.
 *
 * <p><strong>Reuse, not reimplementation.</strong> Ownership + grounding text come from the published
 * {@link DocumentService#requireOwnedContentForAi} (the same accessor AI Summary and Reports use — no
 * ownership logic duplicated, CLAUDE.md); the model is reached only through the {@link AiPort}; the
 * grounded prompt is composed centrally by {@link ChatPromptBuilder}; and the lifecycle is written through
 * the shared {@link AiRequestLifecycleWriter} plus {@link ChatLifecycleWriter}. No provider type is visible
 * here and the provider call sits <em>outside</em> any DB transaction (ADR-010).
 *
 * <p><strong>Preconditions</strong> (mirroring AI Summary, BR-010 / AI_ARCHITECTURE §9): a non-owned or
 * unknown document is {@code 404}; a non-{@code READY} document is {@code 409} (FR-CHAT-007); a
 * {@code READY} document with no extracted text is {@code 422}; an empty/over-length question is {@code 422}
 * (VR-007). <strong>Failure handling</strong> (AI_ARCHITECTURE §11–§12): a provider outage marks the
 * request {@code FAILED} and surfaces {@code 503}; an empty model response is an output-validation failure
 * (FAILED, recorded), never a fabricated answer.
 */
@Service
public class ChatService {
    
    private static final String NOT_READY_MESSAGE =
        "The document is not ready. Questions can only be answered once text extraction has completed.";
    private static final String NO_TEXT_MESSAGE =
        "The document has no extractable text to answer questions from.";
    private static final String PROVIDER_UNAVAILABLE_MESSAGE =
        "The answer could not be generated because the AI service was unavailable. Please try again.";
    private static final String INVALID_OUTPUT_REASON =
        "The AI service did not return a usable answer. Please try again.";
    private static final String PROVIDER_UNAVAILABLE_REASON =
        "The AI service was unavailable while answering the question. Please try again.";
    
    private final DocumentService documentService;
    private final AiRequestRepository requestRepository;
    private final AiOutputRepository outputRepository;
    private final AiRequestLifecycleWriter requestLifecycleWriter;
    private final ChatLifecycleWriter chatLifecycleWriter;
    private final ChatPromptBuilder promptBuilder;
    private final AiPort aiPort;
    private final CurrentUserProvider currentUserProvider;
    private final ChatProperties properties;
    
    public ChatService(DocumentService documentService, AiRequestRepository requestRepository,
                       AiOutputRepository outputRepository, AiRequestLifecycleWriter requestLifecycleWriter,
                       ChatLifecycleWriter chatLifecycleWriter, ChatPromptBuilder promptBuilder, AiPort aiPort,
                       CurrentUserProvider currentUserProvider, ChatProperties properties) {
        this.documentService = documentService;
        this.requestRepository = requestRepository;
        this.outputRepository = outputRepository;
        this.requestLifecycleWriter = requestLifecycleWriter;
        this.chatLifecycleWriter = chatLifecycleWriter;
        this.promptBuilder = promptBuilder;
        this.aiPort = aiPort;
        this.currentUserProvider = currentUserProvider;
        this.properties = properties;
    }
    
    /**
     * API_SPEC §11.1 (FR-CHAT-001): answer a question about a {@code READY} document, grounded in its
     * extracted text. Processing is synchronous-with-status (ADR-013), so a terminal resource is returned
     * directly (the documented {@code 201} path). Each call is a new attempt/AI Request (API_SPEC §2.10 —
     * generate is not idempotent).
     */
    public AiResponse ask(UUID documentId, String question) {
        String normalizedQuestion = normalizeQuestion(question);
        DocumentContentView document = documentService.requireOwnedContentForAi(documentId);
        if (document.status() != DocumentStatus.READY) {
            throw new DocumentNotReadyException(NOT_READY_MESSAGE);
        }
        String extractedText = document.extractedText();
        if (extractedText == null || extractedText.isBlank()) {
            throw new NoExtractableTextException(NO_TEXT_MESSAGE);
        }
        
        UUID userId = currentUserProvider.requireUserId();
        AiRequest request = requestLifecycleWriter.createChatRequested(userId, documentId, normalizedQuestion);
        requestLifecycleWriter.markInProgress(request.getId());
        
        // The provider call is outside any DB transaction (ADR-010); only the result is persisted.
        AiPrompt prompt = promptBuilder.build(extractedText, normalizedQuestion);
        try {
            AiCompletion completion = aiPort.generate(prompt);
            String content = completion == null ? null : completion.text();
            if (content == null || content.isBlank()) {
                // Output validation (AI_ARCHITECTURE §11): an empty response is a failure, not an answer.
                requestLifecycleWriter.markFailed(request.getId(), INVALID_OUTPUT_REASON);
            } else {
                // Completion + AIOutput + CHAT_MESSAGE_SENT activity commit atomically (DATABASE §11).
                chatLifecycleWriter.completeWithAnswerAndActivity(request.getId(), content, userId, documentId);
            }
        } catch (AiUnavailableException e) {
            // Record the failed attempt, then surface a 503 (graceful degradation, AI_ARCHITECTURE §12).
            requestLifecycleWriter.markFailed(request.getId(), PROVIDER_UNAVAILABLE_REASON);
            throw new AiUnavailableException(PROVIDER_UNAVAILABLE_MESSAGE, e);
        }
        
        return responseFor(reload(request.getId()));
    }
    
    /**
     * API_SPEC §11.2 (FR-CHAT-004): the document's chat thread — its {@code CHAT} exchanges in the order
     * the {@link Pageable} requests (default {@code createdAt,asc}). Ownership first (404 if the document is
     * unknown/non-owned); the thread is then read owner-safely because access is already established via the
     * document. Answers are batch-loaded to avoid an N+1 per exchange.
     */
    public PageResponse<AiResponse> history(UUID documentId, Pageable pageable) {
        documentService.requireOwnedContentForAi(documentId);
        Page<AiRequest> page = requestRepository.findByDocumentIdAndType(documentId, AiRequestType.CHAT, pageable);
        Map<UUID, AiOutput> outputs = loadOutputs(page.getContent());
        return PageResponse.from(page, request -> AiResponse.from(request, outputs.get(request.getId())));
    }
    
    private String normalizeQuestion(String question) {
        // @NotBlank at the boundary already rejects an empty question; enforce the [Assumption] max length
        // here (tunable config, VR-007) so it surfaces as the same 422 field error (API_SPEC §2.12).
        String trimmed = question == null ? "" : question.strip();
        if (trimmed.isEmpty()) {
            throw new ValidationFailedException(Map.of("question", "A question is required."));
        }
        if (trimmed.length() > properties.maxQuestionLength()) {
            throw new ValidationFailedException(
                Map.of("question", "Must be at most " + properties.maxQuestionLength() + " characters."));
        }
        return trimmed;
    }
    
    private Map<UUID, AiOutput> loadOutputs(List<AiRequest> requests) {
        List<UUID> completedIds = requests.stream()
                                      .filter(request -> request.getStatus() == AiRequestStatus.COMPLETED)
                                      .map(AiRequest::getId)
                                      .toList();
        if (completedIds.isEmpty()) {
            return Map.of();
        }
        return outputRepository.findByAiRequestIdIn(completedIds).stream()
                   .collect(Collectors.toMap(AiOutput::getAiRequestId, Function.identity()));
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
