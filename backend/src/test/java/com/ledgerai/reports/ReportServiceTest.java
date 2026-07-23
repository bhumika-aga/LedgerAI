package com.ledgerai.reports;

import com.ledgerai.ai.DocumentNotReadyException;
import com.ledgerai.ai.NoExtractableTextException;
import com.ledgerai.ai.ReportPromptBuilder;
import com.ledgerai.ai.port.AiCompletion;
import com.ledgerai.ai.port.AiPort;
import com.ledgerai.ai.port.AiPrompt;
import com.ledgerai.ai.port.AiUnavailableException;
import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.ResourceNotFoundException;
import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.common.security.OwnershipGuard;
import com.ledgerai.documents.DocumentContentView;
import com.ledgerai.documents.DocumentService;
import com.ledgerai.documents.domain.DocumentStatus;
import com.ledgerai.reports.config.ReportProperties;
import com.ledgerai.reports.domain.Report;
import com.ledgerai.reports.domain.ReportStatus;
import com.ledgerai.reports.dto.ReportResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ReportService} — generation preconditions/failure mapping (API_SPEC §13.1; BR-010/035;
 * VR-008), owner-scoped reads/writes, and edit/delete. Collaborators are mocked, so no provider or database
 * is touched.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {
    
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID REPORT_ID = UUID.randomUUID();
    private final ReportProperties properties = new ReportProperties(200, 100_000);
    @Mock
    private DocumentService documentService;
    @Mock
    private ReportPromptBuilder promptBuilder;
    @Mock
    private AiPort aiPort;
    @Mock
    private ReportLifecycleWriter lifecycleWriter;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private OwnershipGuard ownershipGuard;
    @Mock
    private CurrentUserProvider currentUserProvider;
    
    private ReportService service() {
        return new ReportService(documentService, promptBuilder, aiPort, lifecycleWriter, reportRepository,
            ownershipGuard, currentUserProvider, properties);
    }
    
    private void documentReadyWithText() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "Extracted report source text"));
        when(currentUserProvider.requireUserId()).thenReturn(USER_ID);
        when(promptBuilder.build(any(), any())).thenReturn(new AiPrompt("system", "grounded"));
    }
    
    private Report draft(String content) {
        return Report.createDraft(USER_ID, DOC_ID, "Q4", content);
    }
    
    @Test
    void generateRejectsANonReadyDocumentWith409() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.PROCESSING, null));
        
        assertThatThrownBy(() -> service().generate(DOC_ID, "Q4"))
            .isInstanceOf(DocumentNotReadyException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void generateRejectsAReadyDocumentWithNoTextWith422() {
        when(documentService.requireOwnedContentForAi(DOC_ID))
            .thenReturn(new DocumentContentView(DOC_ID, DocumentStatus.READY, "   "));
        
        assertThatThrownBy(() -> service().generate(DOC_ID, "Q4"))
            .isInstanceOf(NoExtractableTextException.class);
        verify(aiPort, never()).generate(any());
    }
    
    @Test
    void generateProducesADraftReportOnTheHappyPath() {
        documentReadyWithText();
        when(aiPort.generate(any())).thenReturn(new AiCompletion("The generated report body."));
        when(lifecycleWriter.createDraft(USER_ID, DOC_ID, "Q4", "The generated report body."))
            .thenReturn(draft("The generated report body."));
        
        ReportResponse response = service().generate(DOC_ID, "Q4");
        
        verify(lifecycleWriter).createDraft(USER_ID, DOC_ID, "Q4", "The generated report body.");
        assertThat(response.status()).isEqualTo(ReportStatus.DRAFT);
        assertThat(response.content()).isEqualTo("The generated report body.");
    }
    
    @Test
    void generateSurfacesProviderUnavailableAs503() {
        documentReadyWithText();
        when(aiPort.generate(any())).thenThrow(new AiUnavailableException("down", null));
        
        assertThatThrownBy(() -> service().generate(DOC_ID, "Q4"))
            .isInstanceOf(AiUnavailableException.class);
        verify(lifecycleWriter, never()).createDraft(any(), any(), any(), any());
    }
    
    @Test
    void generateSurfacesAnEmptyModelResponseAs503() {
        documentReadyWithText();
        when(aiPort.generate(any())).thenReturn(new AiCompletion("   "));
        
        assertThatThrownBy(() -> service().generate(DOC_ID, "Q4"))
            .isInstanceOf(AiUnavailableException.class);
        verify(lifecycleWriter, never()).createDraft(any(), any(), any(), any());
    }
    
    @Test
    void generateRejectsAnOverLengthTitleWith422() {
        assertThatThrownBy(() -> service().generate(DOC_ID, "x".repeat(201)))
            .isInstanceOf(ValidationFailedException.class);
        verify(documentService, never()).requireOwnedContentForAi(any());
    }
    
    @Test
    void listIsOwnerScopedAndMapsResults() {
        when(currentUserProvider.requireUserId()).thenReturn(USER_ID);
        when(reportRepository.findOwned(eq(USER_ID), eq(DOC_ID), eq(ReportStatus.DRAFT), any()))
            .thenReturn(new PageImpl<>(List.of(draft("body"))));
        
        PageResponse<ReportResponse> page =
            service().list(DOC_ID, ReportStatus.DRAFT, PageRequest.of(0, 20));
        
        verify(reportRepository).findOwned(eq(USER_ID), eq(DOC_ID), eq(ReportStatus.DRAFT), any());
        assertThat(page.content()).singleElement()
            .satisfies(r -> assertThat(r.content()).isEqualTo("body"));
    }
    
    @Test
    void getReturnsAnOwnedReport() {
        when(ownershipGuard.requireOwned(any(), any())).thenReturn(draft("body"));
        
        assertThat(service().get(REPORT_ID).content()).isEqualTo("body");
    }
    
    @Test
    void getForANonOwnedReportIs404() {
        when(ownershipGuard.requireOwned(any(), any())).thenThrow(new ResourceNotFoundException());
        
        assertThatThrownBy(() -> service().get(REPORT_ID)).isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void updateAppliesAPartialEditAndSaves() {
        Report report = draft("original");
        when(ownershipGuard.requireOwned(any(), any())).thenReturn(report);
        when(reportRepository.save(report)).thenReturn(report);
        
        ReportResponse response = service().update(REPORT_ID, "New title", "Edited body.", "SAVED");
        
        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.content()).isEqualTo("Edited body.");
        assertThat(response.status()).isEqualTo(ReportStatus.SAVED);
    }
    
    @Test
    void updateRejectsBlankContentWith422() {
        when(ownershipGuard.requireOwned(any(), any())).thenReturn(draft("original"));
        
        assertThatThrownBy(() -> service().update(REPORT_ID, null, "  ", null))
            .isInstanceOf(ValidationFailedException.class);
        verify(reportRepository, never()).save(any());
    }
    
    @Test
    void updateRejectsAnInvalidStatusWith422() {
        when(ownershipGuard.requireOwned(any(), any())).thenReturn(draft("original"));
        
        assertThatThrownBy(() -> service().update(REPORT_ID, null, null, "PUBLISHED"))
            .isInstanceOf(ValidationFailedException.class);
        verify(reportRepository, never()).save(any());
    }
    
    @Test
    void deleteRemovesAnOwnedReport() {
        Report report = draft("body");
        when(ownershipGuard.requireOwned(any(), any())).thenReturn(report);
        
        service().delete(REPORT_ID);
        
        verify(reportRepository).delete(report);
    }
    
    @Test
    void deleteForANonOwnedReportIs404() {
        when(ownershipGuard.requireOwned(any(), any())).thenThrow(new ResourceNotFoundException());
        
        assertThatThrownBy(() -> service().delete(REPORT_ID)).isInstanceOf(ResourceNotFoundException.class);
        verify(reportRepository, never()).delete(any(Report.class));
    }
}
