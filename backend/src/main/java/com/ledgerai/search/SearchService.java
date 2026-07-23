package com.ledgerai.search;

import com.ledgerai.common.dto.PageResponse;
import com.ledgerai.common.exception.ValidationFailedException;
import com.ledgerai.common.security.CurrentUserProvider;
import com.ledgerai.search.config.SearchProperties;
import com.ledgerai.search.dto.SearchResultResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Global Search business rules (API_SPEC §14; FR-SRCH). Keyword search over the extracted text of the
 * caller's own, non-deleted documents — reusing the OCR-produced {@code document_content} and its existing
 * full-text index (DATABASE §9). No semantic search, ranking model, or new index is introduced.
 *
 * <p><strong>Ownership</strong> is enforced at the query via {@link CurrentUserProvider} + the repository's
 * owner-scoped SQL (the caller only ever searches documents under clients they own, BR-004/006) — the same
 * owner-scoping-at-the-query approach the client list and activity timeline use; no ownership logic is
 * duplicated. <strong>Validation</strong> (VR-006): the {@code q} keywords are required and bounded; a
 * missing/blank or oversized query is a {@code 422} field error (API_SPEC §14.1), surfaced through the shared
 * validation model. A valid query with no matches yields an empty page (FR-SRCH-006).
 */
@Service
public class SearchService {
    
    private final DocumentSearchRepository searchRepository;
    private final CurrentUserProvider currentUserProvider;
    private final SearchProperties properties;
    
    public SearchService(DocumentSearchRepository searchRepository, CurrentUserProvider currentUserProvider,
                         SearchProperties properties) {
        this.searchRepository = searchRepository;
        this.currentUserProvider = currentUserProvider;
        this.properties = properties;
    }
    
    /**
     * API_SPEC §14.1: search the caller's document content for {@code q}, paged.
     */
    @Transactional(readOnly = true)
    public PageResponse<SearchResultResponse> search(String q, Pageable pageable) {
        String query = validateQuery(q);
        UUID userId = currentUserProvider.requireUserId();
        // Search defines no `sort` parameter (API_SPEC §14.1); relevance is fixed in the query, so strip any
        // incoming sort — a native full-text query cannot accept dynamic sorting, and none is documented.
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<SearchResultProjection> page = searchRepository.search(userId, query, unsorted);
        return PageResponse.from(page, hit -> toResult(hit, query));
    }
    
    private String validateQuery(String q) {
        String trimmed = q == null ? "" : q.strip();
        if (trimmed.isEmpty()) {
            // `q` is required keywords (API_SPEC §14.1); a missing/blank query is invalid (VR-006 → 422).
            throw new ValidationFailedException(Map.of("q", "A search query is required."));
        }
        if (trimmed.length() > properties.maxQueryLength()) {
            throw new ValidationFailedException(
                Map.of("q", "Must be at most " + properties.maxQueryLength() + " characters."));
        }
        return trimmed;
    }
    
    private SearchResultResponse toResult(SearchResultProjection hit, String query) {
        String body = hit.getBodyExcerpt();
        return new SearchResultResponse(
            hit.getDocumentId(),
            hit.getClientId(),
            hit.getTitle(),
            SearchSnippets.snippet(body),
            SearchSnippets.matchContext(body, query),
            Instant.ofEpochMilli(hit.getUpdatedAtEpochMs()));
    }
}
