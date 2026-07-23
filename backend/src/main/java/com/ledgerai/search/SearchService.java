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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global Search business rules (API_SPEC §14; SRS §4.11 FR-SRCH). Keyword search over the extracted text
 * of the caller's own, non-deleted documents — reusing the OCR-produced {@code document_content} and its
 * existing full-text index (DATABASE §9). No semantic search, ranking model, or new index is introduced.
 *
 * <p><strong>Ownership</strong> is enforced at the query via {@link CurrentUserProvider} + the repository's
 * owner-scoped SQL (the caller only ever searches documents under clients they own, BR-005) — the same
 * owner-scoping-at-the-query approach the client list and activity timeline use; no ownership logic is
 * duplicated.
 *
 * <p><strong>Query validation (VR-006).</strong> The documented semantics are: an <em>over-length</em>
 * query is rejected with a {@code 422} field error; an <em>empty</em> query is valid and yields a
 * <em>helpful empty state</em> — an empty page, not an error (VR-006, FR-SRCH-006, API_SPEC §14.1). A valid
 * non-empty query with no matches likewise yields an empty page. Only the length bound is a
 * {@code [Assumption]} and is externalized as configuration.
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
        String query = q == null ? "" : q.strip();
        // VR-006: reject only an over-length query (API_SPEC §14.1 → 422). Everything else is a valid search.
        if (query.length() > properties.maxQueryLength()) {
            throw new ValidationFailedException(
                Map.of("q", "Must be at most " + properties.maxQueryLength() + " characters."));
        }
        
        // Search defines no `sort` parameter (API_SPEC §14.1); relevance is fixed in the query, so strip any
        // incoming sort — a native full-text query cannot accept dynamic sorting, and none is documented.
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        
        // VR-006 / FR-SRCH-006: an empty query is valid and returns a helpful empty state (an empty page),
        // never an error. No database round-trip is needed.
        if (query.isEmpty()) {
            return new PageResponse<>(List.of(), unsorted.getPageNumber(), unsorted.getPageSize(), 0L, 0, false);
        }
        
        UUID userId = currentUserProvider.requireUserId();
        Page<SearchResultProjection> page = searchRepository.search(userId, query, unsorted);
        return PageResponse.from(page, hit -> toResult(hit, query));
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
