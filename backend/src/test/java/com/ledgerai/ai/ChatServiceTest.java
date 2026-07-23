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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ChatService} — the AI Chat business rules (API_SPEC §11; SRS §4.8; BR-010/033;
 * SRS §7.2; AI_ARCHITECTURE §11–§12; VR-007). Collaborators are mocked, so the provider is never
 * contacted; the focus is preconditions, question validation, lifecycle, activity, and failure mapping.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {
    
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String QUESTION = "What is the balance sheet total?";
    
    @Mock
    private DocumentService documentService;
    @Mock
    private AiRequestRepository requestRepository;
    @Mock
    private AiOutputRepository outputRepository;
    @Mock
    private AiRequestLifecycleWriter requestLifecycleWriter;
    @Mock
    private ChatLifecycleWriter chatLifecycleWriter;
    @Mock
    private ChatPromptBuilder promptBuilder;
    @Mock
    private AiPort aiPort;
    @Mock
    private CurrentUserProvider currentUserProvider;
    
    private ChatService service;
    
    private static AiRequest requestedChat() {
        return AiRequest.createChat(USER_ID, DOC_ID, QUESTION);
    }
    
    private static AiRequest completedChat() {
        AiRequest request = requestedChat();
        request.markInProgress();
        request.markCompleted();
        return request;
    }
    
    private static AiRequest failedChat() {
        AiRequest request = requestedChat();
        request.markInProgress();
        request.markFailed("boom");
        return request;
    }
    
    @BeforeEach
    void setUp() {
        service = new ChatService(documentService, requestRepository, outputRepository, requestLifecycleWriter,
            chatLifecycleWriter, promptBuilder, aiPort, currentUserProvider, new ChatProperties(4000));
        when(currentUserProvider.requireUserId()).thenReturn(USER_ID);
        when(promptBuilder.build(any(), any())).thenReturn(new AiPrompt("system", "grounded"));
    }
    
    private void documentReadyWithText() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "Balance sheet total 987654"));
    }
    
    @Test
    void askRejectsAnOverLengthQuestionWith422BeforeTouchingTheDocument() {
        service = new ChatService(documentService, requestRepository, outputRepository, requestLifecycleWriter,
            chatLifecycleWriter, promptBuilder, aiPort, currentUserProvider, new ChatProperties(10));
        
        assertThatThrownBy(() -> service.ask(DOC_ID, "this question is definitely longer than ten"))
            .isInstanceOf(ValidationFailedException.class);
        verify(documentService, never()).requireOwnedContentForAi(any());
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void askRejectsABlankQuestionWith422() {
        assertThatThrownBy(() -> service.ask(DOC_ID, "   "))
            .isInstanceOf(ValidationFailedException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void askRejectsANonReadyDocumentWith409() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.PROCESSING, null));
        
        assertThatThrownBy(() -> service.ask(DOC_ID, QUESTION))
            .isInstanceOf(DocumentNotReadyException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void askRejectsAReadyDocumentWithNoTextWith422() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "   "));
        
        assertThatThrownBy(() -> service.ask(DOC_ID, QUESTION))
            .isInstanceOf(NoExtractableTextException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void askProducesAGroundedAnswerAndRecordsActivityOnTheHappyPath() {
        documentReadyWithText();
        AiRequest requested = requestedChat();
        when(requestLifecycleWriter.createChatRequested(USER_ID, DOC_ID, QUESTION)).thenReturn(requested);
        when(aiPort.generate(any())).thenReturn(new AiCompletion("The balance sheet total is 987654."));
        AiRequest completed = completedChat();
        when(requestRepository.findById(requested.getId())).thenReturn(Optional.of(completed));
        when(outputRepository.findByAiRequestId(completed.getId()))
            .thenReturn(Optional.of(AiOutput.create(completed.getId(), "The balance sheet total is 987654.")));
        
        AiResponse response = service.ask(DOC_ID, QUESTION);
        
        // Completion + output + CHAT_MESSAGE_SENT activity are one atomic write (DATABASE §11).
        verify(chatLifecycleWriter)
            .completeWithAnswerAndActivity(requested.getId(), "The balance sheet total is 987654.", USER_ID, DOC_ID);
        assertThat(response.type()).isEqualTo(AiRequestType.CHAT);
        assertThat(response.status()).isEqualTo(AiRequestStatus.COMPLETED);
        assertThat(response.content()).isEqualTo("The balance sheet total is 987654.");
    }
    
    @Test
    void askPassesTheQuestionAndGroundedTextToThePromptBuilder() {
        documentReadyWithText();
        AiRequest requested = requestedChat();
        when(requestLifecycleWriter.createChatRequested(any(), any(), any())).thenReturn(requested);
        when(aiPort.generate(any())).thenReturn(new AiCompletion("answer"));
        when(requestRepository.findById(requested.getId())).thenReturn(Optional.of(completedChat()));
        when(outputRepository.findByAiRequestId(any()))
            .thenReturn(Optional.of(AiOutput.create(requested.getId(), "answer")));
        
        service.ask(DOC_ID, "  " + QUESTION + "  ");
        
        // The question is trimmed and grounded in the document text (AI_ARCHITECTURE §8/§9).
        verify(promptBuilder).build("Balance sheet total 987654", QUESTION);
        verify(requestLifecycleWriter).createChatRequested(USER_ID, DOC_ID, QUESTION);
    }
    
    @Test
    void askMarksFailedWhenTheModelReturnsEmptyOutputAndRecordsNoActivity() {
        documentReadyWithText();
        AiRequest requested = requestedChat();
        when(requestLifecycleWriter.createChatRequested(USER_ID, DOC_ID, QUESTION)).thenReturn(requested);
        when(aiPort.generate(any())).thenReturn(new AiCompletion("  "));
        when(requestRepository.findById(requested.getId())).thenReturn(Optional.of(failedChat()));
        
        AiResponse response = service.ask(DOC_ID, QUESTION);
        
        // An empty response is an output-validation failure, never a fabricated answer (AI_ARCH §11).
        verify(requestLifecycleWriter).markFailed(eq(requested.getId()), any());
        verify(chatLifecycleWriter, never()).completeWithAnswerAndActivity(any(), any(), any(), any());
        assertThat(response.status()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(response.content()).isNull();
    }
    
    @Test
    void askMarksFailedAndSurfaces503WhenProviderUnavailable() {
        documentReadyWithText();
        AiRequest requested = requestedChat();
        when(requestLifecycleWriter.createChatRequested(USER_ID, DOC_ID, QUESTION)).thenReturn(requested);
        when(aiPort.generate(any())).thenThrow(new AiUnavailableException("down", null));
        
        assertThatThrownBy(() -> service.ask(DOC_ID, QUESTION))
            .isInstanceOf(AiUnavailableException.class);
        verify(requestLifecycleWriter).markFailed(eq(requested.getId()), any());
        verify(chatLifecycleWriter, never()).completeWithAnswerAndActivity(any(), any(), any(), any());
    }
    
    @Test
    void historyChecksOwnershipAndReturnsTheThreadWithAnswers() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "text"));
        AiRequest completed = completedChat();
        Pageable pageable = PageRequest.of(0, 20);
        when(requestRepository.findByDocumentIdAndType(DOC_ID, AiRequestType.CHAT, pageable))
            .thenReturn(new PageImpl<>(List.of(completed), pageable, 1));
        when(outputRepository.findByAiRequestIdIn(List.of(completed.getId())))
            .thenReturn(List.of(AiOutput.create(completed.getId(), "A grounded answer.")));
        
        PageResponse<AiResponse> page = service.history(DOC_ID, pageable);
        
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().type()).isEqualTo(AiRequestType.CHAT);
        assertThat(page.content().getFirst().content()).isEqualTo("A grounded answer.");
        verify(documentService).requireOwnedContentForAi(DOC_ID);
    }
    
    @Test
    void historyDoesNotLoadOutputsWhenTheThreadHasNoCompletedExchanges() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "text"));
        Pageable pageable = PageRequest.of(0, 20);
        when(requestRepository.findByDocumentIdAndType(DOC_ID, AiRequestType.CHAT, pageable))
            .thenReturn(new PageImpl<>(List.of(failedChat()), pageable, 1));
        
        PageResponse<AiResponse> page = service.history(DOC_ID, pageable);
        
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().status()).isEqualTo(AiRequestStatus.FAILED);
        assertThat(page.content().getFirst().content()).isNull();
        verify(outputRepository, never()).findByAiRequestIdIn(any());
    }
}
