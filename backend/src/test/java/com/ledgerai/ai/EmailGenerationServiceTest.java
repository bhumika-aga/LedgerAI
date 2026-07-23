package com.ledgerai.ai;

import com.ledgerai.ai.config.EmailProperties;
import com.ledgerai.ai.domain.AiOutput;
import com.ledgerai.ai.domain.AiRequest;
import com.ledgerai.ai.domain.AiRequestStatus;
import com.ledgerai.ai.domain.AiRequestType;
import com.ledgerai.ai.dto.AiResponse;
import com.ledgerai.ai.port.AiCompletion;
import com.ledgerai.ai.port.AiPort;
import com.ledgerai.ai.port.AiPrompt;
import com.ledgerai.ai.port.AiUnavailableException;
import com.ledgerai.clients.ClientService;
import com.ledgerai.clients.domain.ClientStatus;
import com.ledgerai.clients.dto.ClientResponse;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.documents.DocumentContentView;
import com.ledgerai.documents.DocumentService;
import com.ledgerai.documents.domain.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmailGenerationService} — the AI Email business rules (API_SPEC §12; SRS §4.9;
 * BR-031/032/034; SRS §7.2; AI_ARCHITECTURE §11–§12; VR-007). Collaborators are mocked, so the provider is
 * never contacted; the focus is optional-context resolution, validation, lifecycle, activity, and failures.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailGenerationServiceTest {
    
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final String INSTRUCTION = "Write a follow-up email about the overdue invoice.";
    
    @Mock
    private ClientService clientService;
    @Mock
    private DocumentService documentService;
    @Mock
    private AiRequestRepository requestRepository;
    @Mock
    private AiOutputRepository outputRepository;
    @Mock
    private AiRequestLifecycleWriter requestLifecycleWriter;
    @Mock
    private EmailLifecycleWriter emailLifecycleWriter;
    @Mock
    private EmailPromptBuilder promptBuilder;
    @Mock
    private AiPort aiPort;
    @Mock
    private CurrentUserProvider currentUserProvider;
    
    private EmailGenerationService service;
    
    private static AiRequest requestedEmail(UUID documentId) {
        return AiRequest.createEmail(USER_ID, documentId, INSTRUCTION);
    }
    
    private static AiRequest completedEmail(UUID documentId) {
        AiRequest request = requestedEmail(documentId);
        request.markInProgress();
        request.markCompleted();
        return request;
    }
    
    private static AiRequest failedEmail() {
        AiRequest request = requestedEmail(null);
        request.markInProgress();
        request.markFailed("boom");
        return request;
    }
    
    private static ClientResponse client() {
        Instant now = Instant.now();
        return new ClientResponse(CLIENT_ID, "Acme Corp", null, null, ClientStatus.ACTIVE, null, now, now);
    }
    
    @BeforeEach
    void setUp() {
        service = new EmailGenerationService(clientService, documentService, requestRepository, outputRepository,
            requestLifecycleWriter, emailLifecycleWriter, promptBuilder, aiPort, currentUserProvider,
            new EmailProperties(4000));
        when(currentUserProvider.requireUserId()).thenReturn(USER_ID);
        when(promptBuilder.build(any(), any(), any())).thenReturn(new AiPrompt("system", "grounded"));
    }
    
    private void stubGeneration(AiRequest requested, String content) {
        when(requestLifecycleWriter.createEmailRequested(eq(USER_ID), any(), eq(INSTRUCTION)))
            .thenReturn(requested);
        when(aiPort.generate(any())).thenReturn(new AiCompletion(content));
        AiRequest completed = completedEmail(requested.getDocumentId());
        when(requestRepository.findById(requested.getId())).thenReturn(Optional.of(completed));
        when(outputRepository.findByAiRequestId(completed.getId()))
            .thenReturn(Optional.of(AiOutput.create(completed.getId(), content)));
    }
    
    @Test
    void generateFromInstructionOnlyProducesADraftWithNoContext() {
        AiRequest requested = requestedEmail(null);
        stubGeneration(requested, "Dear client, ...");
        
        AiResponse response = service.generate(INSTRUCTION, null, null);
        
        // No client/document context resolved; the draft comes from the instruction alone (best-effort).
        verifyNoInteractions(clientService);
        verifyNoInteractions(documentService);
        verify(promptBuilder).build(INSTRUCTION, null, null);
        verify(emailLifecycleWriter).completeWithDraftAndActivity(requested.getId(), "Dear client, ...",
            USER_ID, null, null);
        assertThat(response.type()).isEqualTo(AiRequestType.EMAIL);
        assertThat(response.status()).isEqualTo(AiRequestStatus.COMPLETED);
        assertThat(response.content()).isEqualTo("Dear client, ...");
    }
    
    @Test
    void generateResolvesClientAndDocumentContextAndGroundsThePrompt() {
        when(clientService.get(CLIENT_ID)).thenReturn(client());
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "Invoice total 4200"));
        AiRequest requested = requestedEmail(DOC_ID);
        stubGeneration(requested, "Dear Acme Corp, ...");
        
        service.generate(INSTRUCTION, CLIENT_ID, DOC_ID);
        
        // Client name and document text flow into the prompt; the activity carries both references.
        verify(promptBuilder).build(INSTRUCTION, "Acme Corp", "Invoice total 4200");
        verify(requestLifecycleWriter).createEmailRequested(USER_ID, DOC_ID, INSTRUCTION);
        verify(emailLifecycleWriter).completeWithDraftAndActivity(requested.getId(), "Dear Acme Corp, ...",
            USER_ID, CLIENT_ID, DOC_ID);
    }
    
    @Test
    void generateRejectsABlankInstructionWith422() {
        assertThatThrownBy(() -> service.generate("   ", null, null))
            .isInstanceOf(ValidationFailedException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void generateRejectsAnOverLengthInstructionWith422() {
        service = new EmailGenerationService(clientService, documentService, requestRepository, outputRepository,
            requestLifecycleWriter, emailLifecycleWriter, promptBuilder, aiPort, currentUserProvider,
            new EmailProperties(10));
        
        assertThatThrownBy(() -> service.generate("this instruction is far too long", null, null))
            .isInstanceOf(ValidationFailedException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void generatePropagates404WhenTheReferencedClientIsNotOwned() {
        when(clientService.get(CLIENT_ID)).thenThrow(new ResourceNotFoundException());
        
        assertThatThrownBy(() -> service.generate(INSTRUCTION, CLIENT_ID, null))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void generatePropagates404WhenTheReferencedDocumentIsNotOwned() {
        when(documentService.requireOwnedContentForAi(DOC_ID)).thenThrow(new ResourceNotFoundException());
        
        assertThatThrownBy(() -> service.generate(INSTRUCTION, null, DOC_ID))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void generateRejectsANonReadyReferencedDocumentWith409() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.PROCESSING, null));
        
        assertThatThrownBy(() -> service.generate(INSTRUCTION, null, DOC_ID))
            .isInstanceOf(DocumentNotReadyException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void generateMarksFailedWhenTheModelReturnsEmptyOutputAndRecordsNoActivity() {
        AiRequest requested = requestedEmail(null);
        when(requestLifecycleWriter.createEmailRequested(eq(USER_ID), any(), eq(INSTRUCTION)))
            .thenReturn(requested);
        when(aiPort.generate(any())).thenReturn(new AiCompletion("  "));
        when(requestRepository.findById(requested.getId())).thenReturn(Optional.of(failedEmail()));
        
        AiResponse response = service.generate(INSTRUCTION, null, null);
        
        // An empty response is an output-validation failure, never a fabricated draft (AI_ARCH §11).
        verify(requestLifecycleWriter).markFailed(eq(requested.getId()), any());
        verify(emailLifecycleWriter, never()).completeWithDraftAndActivity(any(), any(), any(), any(), any());
        assertThat(response.status()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(response.content()).isNull();
    }
    
    @Test
    void generateMarksFailedAndSurfaces503WhenProviderUnavailable() {
        AiRequest requested = requestedEmail(null);
        when(requestLifecycleWriter.createEmailRequested(eq(USER_ID), any(), eq(INSTRUCTION)))
            .thenReturn(requested);
        when(aiPort.generate(any())).thenThrow(new AiUnavailableException("down", null));
        
        assertThatThrownBy(() -> service.generate(INSTRUCTION, null, null))
            .isInstanceOf(AiUnavailableException.class);
        verify(requestLifecycleWriter).markFailed(eq(requested.getId()), any());
        verify(emailLifecycleWriter, never()).completeWithDraftAndActivity(any(), any(), any(), any(), any());
    }
}
