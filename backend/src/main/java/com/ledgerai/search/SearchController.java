package com.ledgerai.search;

import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.search.dto.SearchResultResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Search module's single endpoint (API_SPEC §14) — global keyword search over the caller's document
 * content. Read-only; there are no write operations.
 *
 * <p>Thin by design (BACKEND_CODING_STANDARDS §4): it binds the documented query parameters and delegates;
 * it never resolves the caller or checks ownership — that is the service's job (ARCHITECTURE §7.1). {@code q}
 * is bound as optional at the web layer so a missing/blank value becomes a {@code 422} validation error
 * (API_SPEC §14.1) rather than a framework {@code 400}. {@code type} is accepted for the documented future
 * scoping (only document-content search exists today) and is not otherwise interpreted. {@code page}/{@code size}
 * bind through the shared pagination resolver (API_SPEC §2.5); §14.1 defines no {@code sort} parameter.
 */
@RestController
public class SearchController {
    
    private final SearchService searchService;
    
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    
    @GetMapping("/api/v1/search")
    public ResponseEntity<PageResponse<SearchResultResponse>> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String type,
        Pageable pageable) {
        return ResponseEntity.ok(searchService.search(q, pageable));
    }
}
