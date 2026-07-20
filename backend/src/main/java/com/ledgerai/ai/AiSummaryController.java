package com.ledgerai.ai;

import com.ledgerai.ai.dto.AiResponse;
import com.ledgerai.ai.dto.EditSummaryRequest;
import com.ledgerai.ai.dto.GenerateSummaryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The AI Summary module's endpoints (API_SPEC §10) — the three documented operations and nothing else
 * (no chat, email, or report endpoints exist here). Summaries are addressed under the owning document.
 *
 * <p>Thin by design (BACKEND_CODING_STANDARDS §4): it binds the request and delegates; it never resolves
 * the caller or checks ownership/readiness — those are the service's job (ARCHITECTURE §7.1). Generation
 * runs synchronously-with-status (ADR-013), so the documented synchronous {@code 201} path is used; the
 * async-ready {@code 202} + poll contract is honored by the GET endpoint (API_SPEC §2.11).
 */
@RestController
public class AiSummaryController {
    
    private final AiSummaryService summaryService;
    
    public AiSummaryController(AiSummaryService summaryService) {
        this.summaryService = summaryService;
    }
    
    /**
     * API_SPEC §10.1: generate (or, with {@code regenerate}, re-generate) the document's summary.
     * Returns {@code 201} with the resulting resource on the synchronous path.
     */
    @PostMapping("/api/v1/documents/{documentId}/summary")
    public ResponseEntity<AiResponse> generate(@PathVariable UUID documentId,
                                               @RequestBody(required = false) GenerateSummaryRequest request) {
        boolean regenerate = request != null && request.isRegenerate();
        AiResponse response = summaryService.generate(documentId, regenerate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * API_SPEC §10.2: the saved summary and its status (also the async poll target).
     */
    @GetMapping("/api/v1/documents/{documentId}/summary")
    public ResponseEntity<AiResponse> get(@PathVariable UUID documentId) {
        return ResponseEntity.ok(summaryService.get(documentId));
    }
    
    /**
     * API_SPEC §10.3: persist the user's edit to the summary content (human-in-the-loop, BR-031).
     */
    @PatchMapping("/api/v1/documents/{documentId}/summary")
    public ResponseEntity<AiResponse> edit(@PathVariable UUID documentId,
                                           @Valid @RequestBody EditSummaryRequest request) {
        return ResponseEntity.ok(summaryService.edit(documentId, request.content()));
    }
}
