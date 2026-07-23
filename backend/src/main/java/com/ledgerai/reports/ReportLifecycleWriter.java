package com.ledgerai.reports;

import com.ledgerai.activity.ActivityService;
import com.ledgerai.reports.domain.Report;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The transactional boundary for report generation (DATABASE §11, ADR-010).
 *
 * <p>It is a separate bean on purpose: {@link ReportService} orchestrates across the external AI provider
 * call, which MUST NOT be held inside a DB transaction (ADR-010 — "the provider call sits outside the
 * database transaction; only persistence of its result is transactional"). This method persists the
 * generated draft and records the {@code REPORT_CREATED} activity <strong>together</strong>, in one
 * transaction (DATABASE §11 — "Insert/Update Report + insert Activity(REPORT_CREATED)"; the shared
 * {@link ActivityService} joins the same transaction). The report is document-scoped; {@code client_id} is
 * left null on the activity (Activity §5.8 allows it), matching the summary activity.
 */
@Component
public class ReportLifecycleWriter {
    
    private final ReportRepository reportRepository;
    private final ActivityService activityService;
    
    public ReportLifecycleWriter(ReportRepository reportRepository, ActivityService activityService) {
        this.reportRepository = reportRepository;
        this.activityService = activityService;
    }
    
    @Transactional
    public Report createDraft(UUID userId, UUID documentId, String title, String content) {
        Report saved = reportRepository.save(Report.createDraft(userId, documentId, title, content));
        activityService.recordReportCreated(userId, null, documentId);
        return saved;
    }
}
