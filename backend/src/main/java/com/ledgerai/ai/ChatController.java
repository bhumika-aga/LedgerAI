package com.ledgerai.ai;

import com.ledgerai.ai.dto.AiResponse;
import com.ledgerai.ai.dto.AskQuestionRequest;
import com.ledgerai.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The AI Chat module's endpoints (API_SPEC §11) — the two documented operations and nothing else. Chat is
 * document-scoped in MVP, so both are addressed under the owning document (there is no conversation
 * resource, API_SPEC §11).
 *
 * <p>Thin by design (BACKEND_CODING_STANDARDS §4): it binds the request and delegates; it never resolves
 * the caller or checks ownership/readiness — those are the service's job (ARCHITECTURE §7.1). Generation
 * runs synchronously-with-status (ADR-013), so the documented synchronous {@code 201} path is used; the
 * chat thread is read via the GET endpoint (API_SPEC §11.2).
 */
@RestController
public class ChatController {
    
    private final ChatService chatService;
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * API_SPEC §11.1 (FR-CHAT-001): ask a grounded question about the document. Returns {@code 201} with
     * the resulting exchange on the synchronous path. An empty/invalid question is a {@code 422} (VR-007),
     * a non-{@code READY} document a {@code 409}, a provider outage a {@code 503}.
     */
    @PostMapping("/api/v1/documents/{documentId}/chat")
    public ResponseEntity<AiResponse> ask(@PathVariable UUID documentId,
                                          @Valid @RequestBody AskQuestionRequest request) {
        AiResponse response = chatService.ask(documentId, request.question());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * API_SPEC §11.2 (FR-CHAT-004): the document's chat thread, chronological by default
     * ({@code createdAt,asc}), paged.
     */
    @GetMapping("/api/v1/documents/{documentId}/chat")
    public ResponseEntity<PageResponse<AiResponse>> history(
        @PathVariable UUID documentId,
        @SortDefault(sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(chatService.history(documentId, pageable));
    }
}
