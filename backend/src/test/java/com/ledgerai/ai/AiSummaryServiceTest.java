package com.ledgerai.ai;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AiSummaryService} — the AI Summary business rules (API_SPEC §10; BR-010; SRS
 * §7.2; AI_ARCHITECTURE §11–§12). Collaborators are mocked, so the provider is never contacted; the
 * focus is preconditions, lifecycle, and failure mapping.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiSummaryServiceTest {
    
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    
    @Mock
    private DocumentService documentService;
    @Mock
    private AiRequestRepository requestRepository;
    @Mock
    private AiOutputRepository outputRepository;
    @Mock
    private AiRequestLifecycleWriter lifecycleWriter;
    @Mock
    private SummaryPromptBuilder promptBuilder;
    @Mock
    private AiPort aiPort;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private com.ledgerai.activity.ActivityService activityService;
    
    @InjectMocks
    private AiSummaryService service;
    
    private static AiRequest requestedSummary() {
        return AiRequest.createSummary(USER_ID, DOC_ID);
    }
    
    private static AiRequest completedSummary() {
        AiRequest request = requestedSummary();
        request.markInProgress();
        request.markCompleted();
        return request;
    }
    
    private static AiRequest failedSummary() {
        AiRequest request = requestedSummary();
        request.markInProgress();
        request.markFailed("boom");
        return request;
    }
    
    @BeforeEach
    void grounding() {
        when(currentUserProvider.requireUserId()).thenReturn(USER_ID);
        when(promptBuilder.build(any())).thenReturn(new AiPrompt("system", "grounded"));
    }
    
    private void documentReadyWithText() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "Extracted document text"));
    }
    
    @Test
    void generateRejectsANonReadyDocumentWith409() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.PROCESSING, null));
        
        assertThatThrownBy(() -> service.generate(DOC_ID, false))
            .isInstanceOf(DocumentNotReadyException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void generateRejectsAReadyDocumentWithNoTextWith422() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "   "));
        
        assertThatThrownBy(() -> service.generate(DOC_ID, false))
            .isInstanceOf(NoExtractableTextException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void generateProducesACompletedSummaryOnTheHappyPath() {
        documentReadyWithText();
        AiRequest requested = requestedSummary();
        when(requestRepository.findFirstByDocumentIdAndTypeOrderByCreatedAtDesc(DOC_ID, AiRequestType.SUMMARY))
            .thenReturn(Optional.empty());
        when(lifecycleWriter.createRequested(USER_ID, DOC_ID)).thenReturn(requested);
        when(aiPort.generate(any())).thenReturn(new AiCompletion("Grounded summary."));
        AiRequest completed = completedSummary();
        when(requestRepository.findById(requested.getId())).thenReturn(Optional.of(completed));
        when(outputRepository.findByAiRequestId(completed.getId()))
            .thenReturn(Optional.of(AiOutput.create(completed.getId(), "Grounded summary.")));
        
        AiResponse response = service.generate(DOC_ID, false);
        
        verify(lifecycleWriter).completeWithOutput(requested.getId(), "Grounded summary.");
        assertThat(response.status()).isEqualTo(AiRequestStatus.COMPLETED);
        assertThat(response.content()).isEqualTo("Grounded summary.");
    }
    
    @Test
    void generateReturnsTheExistingSummaryWhenNotRegenerating() {
        documentReadyWithText();
        AiRequest existing = completedSummary();
        when(requestRepository.findFirstByDocumentIdAndTypeOrderByCreatedAtDesc(DOC_ID, AiRequestType.SUMMARY))
            .thenReturn(Optional.of(existing));
        when(outputRepository.findByAiRequestId(existing.getId()))
            .thenReturn(Optional.of(AiOutput.create(existing.getId(), "Previous summary.")));
        
        AiResponse response = service.generate(DOC_ID, false);
        
        assertThat(response.content()).isEqualTo("Previous summary.");
        verify(lifecycleWriter, never()).createRequested(any(), any());
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void generateRegeneratesEvenWhenACompletedSummaryExists() {
        documentReadyWithText();
        AiRequest requested = requestedSummary();
        when(lifecycleWriter.createRequested(USER_ID, DOC_ID)).thenReturn(requested);
        when(aiPort.generate(any())).thenReturn(new AiCompletion("Fresh summary."));
        AiRequest completed = completedSummary();
        when(requestRepository.findById(requested.getId())).thenReturn(Optional.of(completed));
        when(outputRepository.findByAiRequestId(completed.getId()))
            .thenReturn(Optional.of(AiOutput.create(completed.getId(), "Fresh summary.")));
        
        AiResponse response = service.generate(DOC_ID, true);
        
        // regenerate=true skips the existing-summary short-circuit entirely.
        verify(requestRepository, never())
            .findFirstByDocumentIdAndTypeOrderByCreatedAtDesc(any(), any());
        verify(aiPort).generate(any());
        assertThat(response.content()).isEqualTo("Fresh summary.");
    }
    
    @Test
    void generateMarksFailedAndSurfaces503WhenProviderUnavailable() {
        documentReadyWithText();
        AiRequest requested = requestedSummary();
        when(requestRepository.findFirstByDocumentIdAndTypeOrderByCreatedAtDesc(DOC_ID, AiRequestType.SUMMARY))
            .thenReturn(Optional.empty());
        when(lifecycleWriter.createRequested(USER_ID, DOC_ID)).thenReturn(requested);
        when(aiPort.generate(any())).thenThrow(new AiUnavailableException("down", null));
        
        assertThatThrownBy(() -> service.generate(DOC_ID, false))
            .isInstanceOf(AiUnavailableException.class);
        verify(lifecycleWriter).markFailed(eq(requested.getId()), any());
        verify(lifecycleWriter, never()).completeWithOutput(any(), any());
    }
    
    @Test
    void generateMarksFailedWhenTheModelReturnsEmptyOutput() {
        documentReadyWithText();
        AiRequest requested = requestedSummary();
        when(requestRepository.findFirstByDocumentIdAndTypeOrderByCreatedAtDesc(DOC_ID, AiRequestType.SUMMARY))
            .thenReturn(Optional.empty());
        when(lifecycleWriter.createRequested(USER_ID, DOC_ID)).thenReturn(requested);
        when(aiPort.generate(any())).thenReturn(new AiCompletion("  "));
        when(requestRepository.findById(requested.getId())).thenReturn(Optional.of(failedSummary()));
        
        AiResponse response = service.generate(DOC_ID, false);
        
        // An empty response is an output-validation failure, never a fabricated success (AI_ARCH §11).
        verify(lifecycleWriter).markFailed(eq(requested.getId()), any());
        verify(lifecycleWriter, never()).completeWithOutput(any(), any());
        assertThat(response.status()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(response.content()).isNull();
    }
    
    @Test
    void getReturns404WhenNoSummaryExists() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "text"));
        when(requestRepository.findFirstByDocumentIdAndTypeOrderByCreatedAtDesc(DOC_ID, AiRequestType.SUMMARY))
            .thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> service.get(DOC_ID)).isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void getReturnsTheSavedSummary() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "text"));
        AiRequest completed = completedSummary();
        when(requestRepository.findFirstByDocumentIdAndTypeOrderByCreatedAtDesc(DOC_ID, AiRequestType.SUMMARY))
            .thenReturn(Optional.of(completed));
        when(outputRepository.findByAiRequestId(completed.getId()))
            .thenReturn(Optional.of(AiOutput.create(completed.getId(), "Saved summary.")));
        
        AiResponse response = service.get(DOC_ID);
        
        assertThat(response.status()).isEqualTo(AiRequestStatus.COMPLETED);
        assertThat(response.content()).isEqualTo("Saved summary.");
    }
    
    @Test
    void editPersistsTheUserEditAndMarksItEdited() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "text"));
        AiRequest completed = completedSummary();
        when(requestRepository.findFirstByDocumentIdAndTypeOrderByCreatedAtDesc(DOC_ID, AiRequestType.SUMMARY))
            .thenReturn(Optional.of(completed));
        AiOutput output = AiOutput.create(completed.getId(), "Original.");
        when(outputRepository.findByAiRequestId(completed.getId())).thenReturn(Optional.of(output));
        when(lifecycleWriter.editOutput(eq(output), eq("My edit."))).thenAnswer(invocation -> {
            output.edit("My edit.");
            return output;
        });
        
        AiResponse response = service.edit(DOC_ID, "My edit.");
        
        assertThat(response.content()).isEqualTo("My edit.");
        assertThat(response.edited()).isTrue();
    }
    
    @Test
    void editReturns404WhenThereIsNoOutputToEdit() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "text"));
        AiRequest failed = failedSummary();
        when(requestRepository.findFirstByDocumentIdAndTypeOrderByCreatedAtDesc(DOC_ID, AiRequestType.SUMMARY))
            .thenReturn(Optional.of(failed));
        when(outputRepository.findByAiRequestId(failed.getId())).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> service.edit(DOC_ID, "My edit."))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
