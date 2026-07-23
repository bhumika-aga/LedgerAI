package com.ledgerai.ai;

import com.ledgerai.ai.config.EmailProperties;
import com.ledgerai.ai.domain.AiOutput;
import com.ledgerai.ai.domain.AiRequest;
import com.ledgerai.ai.domain.AiRequestStatus;
import com.ledgerai.ai.dto.AiResponse;
import com.ledgerai.ai.port.AiCompletion;
import com.ledgerai.ai.port.AiPort;
import com.ledgerai.ai.port.AiPrompt;
import com.ledgerai.ai.port.AiUnavailableException;
import com.ledgerai.clients.ClientService;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.documents.DocumentContentView;
import com.ledgerai.documents.DocumentService;
import com.ledgerai.documents.domain.DocumentStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * AI Email business rules and orchestration (SRS §4.9 AI Email Generation; API_SPEC §12; AI_ARCHITECTURE
 * §4–§12). Drafts a professional client email from an instruction plus <strong>optional</strong> client
 * and/or document context; the draft is editable and review-required and is <strong>never sent</strong>
 * (BR-034, FR-EMAIL-005).
 *
 * <p><strong>Reuse, not reimplementation.</strong> An email is a {@code EMAIL} {@link AiRequest} with its
 * draft the {@link AiOutput} (DATABASE §5.5–5.6) — the same persistence, lifecycle, and provider path as
 * summary/chat. Ownership is delegated to the published accessors of other modules
 * ({@link ClientService#get} for the optional client, {@link DocumentService#requireOwnedContentForAi} for
 * the optional document — no ownership logic duplicated, CLAUDE.md); the model is reached only through the
 * {@link AiPort}; the grounded prompt is composed centrally by {@link EmailPromptBuilder}; and the lifecycle
 * is written through the shared {@link AiRequestLifecycleWriter} plus {@link EmailLifecycleWriter}. No
 * provider type is visible here and the provider call sits <em>outside</em> any DB transaction (ADR-010).
 *
 * <p><strong>Preconditions</strong> (API_SPEC §12.1): an empty/over-length instruction is {@code 422}
 * (VR-007); a referenced client or document that is unknown/non-owned is {@code 404}; a referenced document
 * that is not {@code READY} is {@code 409}. Context is optional, so with neither client nor document the
 * draft is produced from the instruction alone (best-effort, SRS §4.9). <strong>Failure handling</strong>
 * (AI_ARCHITECTURE §11–§12): a provider outage marks the request {@code FAILED} and surfaces {@code 503}; an
 * empty model response is an output-validation failure (FAILED, recorded), never a fabricated draft.
 */
@Service
public class EmailGenerationService {
    
    private static final String NOT_READY_MESSAGE =
        "The referenced document is not ready. A document can only provide context once text extraction "
            + "has completed.";
    private static final String PROVIDER_UNAVAILABLE_MESSAGE =
        "The email draft could not be generated because the AI service was unavailable. Please try again.";
    private static final String INVALID_OUTPUT_REASON =
        "The AI service did not return a usable email draft. Please try again.";
    private static final String PROVIDER_UNAVAILABLE_REASON =
        "The AI service was unavailable while drafting the email. Please try again.";
    
    private final ClientService clientService;
    private final DocumentService documentService;
    private final AiRequestRepository requestRepository;
    private final AiOutputRepository outputRepository;
    private final AiRequestLifecycleWriter requestLifecycleWriter;
    private final EmailLifecycleWriter emailLifecycleWriter;
    private final EmailPromptBuilder promptBuilder;
    private final AiPort aiPort;
    private final CurrentUserProvider currentUserProvider;
    private final EmailProperties properties;
    
    public EmailGenerationService(ClientService clientService, DocumentService documentService,
                                  AiRequestRepository requestRepository, AiOutputRepository outputRepository,
                                  AiRequestLifecycleWriter requestLifecycleWriter,
                                  EmailLifecycleWriter emailLifecycleWriter, EmailPromptBuilder promptBuilder,
                                  AiPort aiPort, CurrentUserProvider currentUserProvider,
                                  EmailProperties properties) {
        this.clientService = clientService;
        this.documentService = documentService;
        this.requestRepository = requestRepository;
        this.outputRepository = outputRepository;
        this.requestLifecycleWriter = requestLifecycleWriter;
        this.emailLifecycleWriter = emailLifecycleWriter;
        this.promptBuilder = promptBuilder;
        this.aiPort = aiPort;
        this.currentUserProvider = currentUserProvider;
        this.properties = properties;
    }
    
    /**
     * API_SPEC §12.1 (FR-EMAIL-001): draft an email from the instruction and optional client/document
     * context. Processing is synchronous-with-status (ADR-013), so a terminal resource is returned directly
     * (the documented {@code 201} path). Each call is a new attempt/AI Request (API_SPEC §2.10 — generate is
     * not idempotent; regeneration/refinement, FR-EMAIL-003, is simply another call).
     */
    public AiResponse generate(String instruction, UUID clientId, UUID documentId) {
        String normalizedInstruction = normalizeInstruction(instruction);
        
        // Optional client context: ownership is enforced by the published accessor (404 if unknown/non-owned).
        String clientName = clientId == null ? null : clientService.get(clientId).name();
        
        // Optional document context: ownership (404) then readiness (409); its text grounds the draft.
        String extractedText = null;
        if (documentId != null) {
            DocumentContentView document = documentService.requireOwnedContentForAi(documentId);
            if (document.status() != DocumentStatus.READY) {
                throw new DocumentNotReadyException(NOT_READY_MESSAGE);
            }
            extractedText = document.extractedText();
        }
        
        UUID userId = currentUserProvider.requireUserId();
        AiRequest request = requestLifecycleWriter.createEmailRequested(userId, documentId, normalizedInstruction);
        requestLifecycleWriter.markInProgress(request.getId());
        
        // The provider call is outside any DB transaction (ADR-010); only the result is persisted.
        AiPrompt prompt = promptBuilder.build(normalizedInstruction, clientName, extractedText);
        try {
            AiCompletion completion = aiPort.generate(prompt);
            String content = completion == null ? null : completion.text();
            if (content == null || content.isBlank()) {
                // Output validation (AI_ARCHITECTURE §11): an empty response is a failure, not a draft.
                requestLifecycleWriter.markFailed(request.getId(), INVALID_OUTPUT_REASON);
            } else {
                // Completion + AIOutput + EMAIL_GENERATED activity commit atomically (DATABASE §11).
                emailLifecycleWriter.completeWithDraftAndActivity(
                    request.getId(), content, userId, clientId, documentId);
            }
        } catch (AiUnavailableException e) {
            // Record the failed attempt, then surface a 503 (graceful degradation, AI_ARCHITECTURE §12).
            requestLifecycleWriter.markFailed(request.getId(), PROVIDER_UNAVAILABLE_REASON);
            throw new AiUnavailableException(PROVIDER_UNAVAILABLE_MESSAGE, e);
        }
        
        return responseFor(reload(request.getId()));
    }
    
    private String normalizeInstruction(String instruction) {
        // @NotBlank at the boundary already rejects an empty instruction; enforce the [Assumption] max length
        // here (tunable config, VR-007) so it surfaces as the same 422 field error (API_SPEC §2.12).
        String trimmed = instruction == null ? "" : instruction.strip();
        if (trimmed.isEmpty()) {
            throw new ValidationFailedException(Map.of("instruction", "An instruction is required."));
        }
        if (trimmed.length() > properties.maxInstructionLength()) {
            throw new ValidationFailedException(
                Map.of("instruction", "Must be at most " + properties.maxInstructionLength() + " characters."));
        }
        return trimmed;
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
