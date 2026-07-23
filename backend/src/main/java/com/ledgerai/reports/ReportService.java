package com.ledgerai.reports;

import com.ledgerai.ai.DocumentNotReadyException;
import com.ledgerai.ai.NoExtractableTextException;
import com.ledgerai.ai.ReportPromptBuilder;
import com.ledgerai.ai.port.AiCompletion;
import com.ledgerai.ai.port.AiPort;
import com.ledgerai.ai.port.AiPrompt;
import com.ledgerai.ai.port.AiUnavailableException;
import com.ledgerai.common.dto.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Report business rules and orchestration (API_SPEC §13; SRS §4.10; DATABASE §5.7, §11).
 *
 * <p><strong>Orchestrates, does not reimplement.</strong> Generation reuses the documents module's published
 * grounded-content access ({@link DocumentService#requireOwnedContentForAi}), the AI module's centralized
 * {@link ReportPromptBuilder} + domain-owned {@link AiPort}, and the shared {@code ActivityService} (via
 * {@link ReportLifecycleWriter}). The report content is AI-generated but stored as a first-class
 * {@link Report} row — not an {@code AIOutput} — because DATABASE §11 gives report generation its own
 * transaction boundary (insert Report + insert Activity(REPORT_CREATED)).
 *
 * <p><strong>Ownership.</strong> Generation authorizes via the owning document (the caller must own it, else
 * {@code 404}); reads/writes by id authorize via the shared {@link OwnershipGuard} on the report's
 * {@code userId} ({@code 404} for unknown/non-owned); listing is owner-scoped at the query. No ownership
 * logic is duplicated.
 *
 * <p><strong>Preconditions/validation.</strong> A report is generated only for a {@code READY} document
 * (BR-010/BR-035 → {@code 409}) that has extracted text ({@code 422}); the AI provider call sits outside the
 * DB transaction (ADR-010) and a provider failure or unusable (empty) response surfaces as a retryable
 * {@code 503}. User-supplied title/content are length-validated (VR-008 → {@code 422}).
 */
@Service
public class ReportService {
    
    private static final String NOT_READY_MESSAGE =
        "The document is not ready. A report can only be generated once text extraction has completed.";
    private static final String NO_TEXT_MESSAGE =
        "The document has no extractable text to generate a report from.";
    private static final String GENERATION_FAILED_MESSAGE =
        "The report could not be generated because the AI service was unavailable. Please try again.";
    private static final String EMPTY_OUTPUT_MESSAGE =
        "The AI service did not return a usable report. Please try again.";
    
    private final DocumentService documentService;
    private final ReportPromptBuilder promptBuilder;
    private final AiPort aiPort;
    private final ReportLifecycleWriter lifecycleWriter;
    private final ReportRepository reportRepository;
    private final OwnershipGuard ownershipGuard;
    private final CurrentUserProvider currentUserProvider;
    private final ReportProperties properties;
    
    public ReportService(DocumentService documentService, ReportPromptBuilder promptBuilder, AiPort aiPort,
                         ReportLifecycleWriter lifecycleWriter, ReportRepository reportRepository,
                         OwnershipGuard ownershipGuard, CurrentUserProvider currentUserProvider,
                         ReportProperties properties) {
        this.documentService = documentService;
        this.promptBuilder = promptBuilder;
        this.aiPort = aiPort;
        this.lifecycleWriter = lifecycleWriter;
        this.reportRepository = reportRepository;
        this.ownershipGuard = ownershipGuard;
        this.currentUserProvider = currentUserProvider;
        this.properties = properties;
    }
    
    /**
     * API_SPEC §13.1 (FR-RPT-001): generate a report from a {@code READY} document, grounded in its extracted
     * text. Synchronous-with-status (ADR-013), so the created {@code DRAFT} report is returned directly.
     */
    public ReportResponse generate(UUID documentId, String titleHint) {
        String title = normalizeTitle(titleHint);
        DocumentContentView document = documentService.requireOwnedContentForAi(documentId);
        if (document.status() != DocumentStatus.READY) {
            throw new DocumentNotReadyException(NOT_READY_MESSAGE);
        }
        String extractedText = document.extractedText();
        if (extractedText == null || extractedText.isBlank()) {
            throw new NoExtractableTextException(NO_TEXT_MESSAGE);
        }
        
        UUID userId = currentUserProvider.requireUserId();
        
        // The provider call is outside any DB transaction (ADR-010); only the result is persisted.
        AiPrompt prompt = promptBuilder.build(extractedText, title);
        String content;
        try {
            AiCompletion completion = aiPort.generate(prompt);
            content = completion == null ? null : completion.text();
        } catch (AiUnavailableException e) {
            throw new AiUnavailableException(GENERATION_FAILED_MESSAGE, e);
        }
        if (content == null || content.isBlank()) {
            // An empty response is an unusable generation (AI_ARCHITECTURE §11); surface as a retryable 503
            // rather than persisting an empty report (Report.content is required).
            throw new AiUnavailableException(EMPTY_OUTPUT_MESSAGE, null);
        }
        
        Report saved = lifecycleWriter.createDraft(userId, documentId, title, content);
        return ReportResponse.from(saved);
    }
    
    /**
     * API_SPEC §13.2 (FR-RPT): the caller's reports, paged, optionally filtered by document and status.
     */
    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> list(UUID documentId, ReportStatus status, Pageable pageable) {
        UUID userId = currentUserProvider.requireUserId();
        Page<Report> page = reportRepository.findOwned(userId, documentId, status, pageable);
        return PageResponse.from(page, ReportResponse::from);
    }
    
    /**
     * API_SPEC §13.3: a single report the caller owns.
     */
    @Transactional(readOnly = true)
    public ReportResponse get(UUID reportId) {
        return ReportResponse.from(requireOwnedReport(reportId));
    }
    
    /**
     * API_SPEC §13.4 (FR-RPT-003): edit/save a report (human-in-the-loop, BR-031). Partial update; a
     * {@code status} of {@code DRAFT}/{@code SAVED} may be applied.
     */
    @Transactional
    public ReportResponse update(UUID reportId, String title, String content, String statusValue) {
        Report report = requireOwnedReport(reportId);
        validateEdit(title, content);
        ReportStatus status = parseStatus(statusValue);
        report.applyUpdate(normalizeTitle(title), content, status);
        return ReportResponse.from(reportRepository.save(report));
    }
    
    /**
     * API_SPEC §13.5: hard-delete a report the caller owns (DATABASE §8). A repeat delete of a removed
     * report is {@code 404} (the row is gone).
     */
    @Transactional
    public void delete(UUID reportId) {
        reportRepository.delete(requireOwnedReport(reportId));
    }
    
    private Report requireOwnedReport(UUID reportId) {
        return ownershipGuard.requireOwned(reportRepository.findById(reportId), Report::getUserId);
    }
    
    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String trimmed = title.strip();
        if (trimmed.length() > properties.maxTitleLength()) {
            throw new ValidationFailedException(
                Map.of("title", "Must be at most " + properties.maxTitleLength() + " characters."));
        }
        return trimmed;
    }
    
    private void validateEdit(String title, String content) {
        // title length is enforced by normalizeTitle; validate content here (VR-008).
        if (content != null) {
            if (content.isBlank()) {
                throw new ValidationFailedException(Map.of("content", "Report content is required."));
            }
            if (content.length() > properties.maxContentLength()) {
                throw new ValidationFailedException(
                    Map.of("content", "Must be at most " + properties.maxContentLength() + " characters."));
            }
        }
    }
    
    private ReportStatus parseStatus(String statusValue) {
        if (statusValue == null) {
            return null;
        }
        try {
            return ReportStatus.valueOf(statusValue);
        } catch (IllegalArgumentException e) {
            throw new ValidationFailedException(Map.of("status", "Must be one of DRAFT, SAVED."));
        }
    }
}
