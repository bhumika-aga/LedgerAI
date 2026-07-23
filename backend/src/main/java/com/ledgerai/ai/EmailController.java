package com.ledgerai.ai;

import com.ledgerai.ai.dto.AiResponse;
import com.ledgerai.ai.dto.GenerateEmailRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The AI Email module's endpoint (API_SPEC §12) — the single documented operation and nothing else. Email
 * is <strong>not</strong> nested under a document because its context is optional (API_SPEC §12); there is
 * no get/list/edit/send endpoint (the draft is client-side editable, BR-031, and never sent, BR-034).
 *
 * <p>Thin by design (BACKEND_CODING_STANDARDS §4): it binds the request and delegates; it never resolves
 * the caller or checks ownership/readiness — those are the service's job (ARCHITECTURE §7.1). Generation
 * runs synchronously-with-status (ADR-013), so the documented synchronous {@code 201} path is used.
 */
@RestController
public class EmailController {
    
    private final EmailGenerationService emailGenerationService;
    
    public EmailController(EmailGenerationService emailGenerationService) {
        this.emailGenerationService = emailGenerationService;
    }
    
    /**
     * API_SPEC §12.1 (FR-EMAIL-001): draft a professional client email from the instruction and optional
     * client/document context. Returns {@code 201} with the resulting draft on the synchronous path. An
     * empty/invalid instruction is a {@code 422} (VR-007), a non-owned client/document a {@code 404}, a
     * referenced non-{@code READY} document a {@code 409}, a provider outage a {@code 503}.
     */
    @PostMapping("/api/v1/ai/emails")
    public ResponseEntity<AiResponse> generate(@Valid @RequestBody GenerateEmailRequest request) {
        AiResponse response = emailGenerationService.generate(
            request.instruction(), request.clientId(), request.documentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
