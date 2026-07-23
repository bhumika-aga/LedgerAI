package com.ledgerai.reports;

import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.reports.domain.ReportStatus;
import com.ledgerai.reports.dto.GenerateReportRequest;
import com.ledgerai.reports.dto.ReportResponse;
import com.ledgerai.reports.dto.UpdateReportRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The Reports module's endpoints (API_SPEC §13) — the five documented operations and nothing else.
 * Generation is nested under the source document; a report is thereafter a first-class resource under
 * {@code /reports}.
 *
 * <p>Thin by design (BACKEND_CODING_STANDARDS §4): it binds the documented request shape and delegates; it
 * never resolves the caller or checks ownership/readiness — that is the service's job (ARCHITECTURE §7.1).
 * Export/download is a client-side action on the returned content (API_SPEC §13.4); no export endpoint
 * exists.
 */
@RestController
public class ReportController {
    
    private final ReportService reportService;
    
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }
    
    /**
     * API_SPEC §13.1: generate a report from a document. Returns {@code 201} with the {@code DRAFT} report on
     * the synchronous path (ADR-013).
     */
    @PostMapping("/api/v1/documents/{documentId}/reports")
    public ResponseEntity<ReportResponse> generate(@PathVariable UUID documentId,
                                                   @RequestBody(required = false) GenerateReportRequest request) {
        String title = request == null ? null : request.title();
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.generate(documentId, title));
    }
    
    /**
     * API_SPEC §13.2: the caller's reports, paged, optionally filtered by {@code documentId} and {@code status}.
     */
    @GetMapping("/api/v1/reports")
    public ResponseEntity<PageResponse<ReportResponse>> list(
        @RequestParam(required = false) UUID documentId,
        @RequestParam(required = false) ReportStatus status,
        @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reportService.list(documentId, status, pageable));
    }
    
    /**
     * API_SPEC §13.3: a single report the caller owns.
     */
    @GetMapping("/api/v1/reports/{reportId}")
    public ResponseEntity<ReportResponse> get(@PathVariable UUID reportId) {
        return ResponseEntity.ok(reportService.get(reportId));
    }
    
    /**
     * API_SPEC §13.4: edit/save the report (partial update).
     */
    @PatchMapping("/api/v1/reports/{reportId}")
    public ResponseEntity<ReportResponse> update(@PathVariable UUID reportId,
                                                 @RequestBody UpdateReportRequest request) {
        return ResponseEntity.ok(
            reportService.update(reportId, request.title(), request.content(), request.status()));
    }
    
    /**
     * API_SPEC §13.5: hard-delete the report.
     */
    @DeleteMapping("/api/v1/reports/{reportId}")
    public ResponseEntity<Void> delete(@PathVariable UUID reportId) {
        reportService.delete(reportId);
        return ResponseEntity.noContent().build();
    }
}
