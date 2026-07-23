package com.ledgerai.search;

import com.ledgerai.documents.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Keyword search over already-extracted document content (API_SPEC §14; FR-SRCH). Data access only — no
 * business rules (BACKEND_CODING_STANDARDS §4). It is bound to the {@link Document} entity but returns a
 * narrow read {@link SearchResultProjection}.
 *
 * <p>Uses PostgreSQL's built-in full-text search (DATABASE §9) and <strong>reuses the existing GIN index</strong>
 * {@code gin_document_content_extracted_text} (migration V5): the {@code WHERE} predicate is written with the
 * exact indexed expression {@code to_tsvector('english', coalesce(extracted_text, ''))}, so the index is used.
 * No new index or migration is introduced. {@code websearch_to_tsquery} parses raw user input safely (it never
 * throws on operator syntax) — plain keyword search, not semantic search.
 *
 * <p>Every row is <strong>owner-scoped</strong> at the query by the owning client's {@code user_id}
 * (BR-004/006) — the same owner-scoping-at-the-query approach the client list and activity timeline use, not a
 * duplicate of the ownership guard — and soft-deleted documents are excluded ({@code status <> 'DELETED'},
 * BR-013). Results are ordered by full-text relevance ({@code ts_rank}) with a deterministic tiebreak so paging
 * is stable; §14.1 defines no {@code sort} parameter, so relevance is the resource default (API_SPEC §2.5).
 */
public interface DocumentSearchRepository extends JpaRepository<Document, UUID> {
    
    @Query(value = """
        SELECT d.id                                AS "documentId",
               d.client_id                         AS "clientId",
               d.original_filename                 AS "title",
               left(dc.extracted_text, 4000)       AS "bodyExcerpt",
               cast(extract(epoch FROM d.updated_at) * 1000 as bigint) AS "updatedAtEpochMs"
        FROM document d
             JOIN client c ON c.id = d.client_id
             JOIN document_content dc ON dc.document_id = d.id
        WHERE c.user_id = :userId
          AND d.status <> 'DELETED'
          AND to_tsvector('english', coalesce(dc.extracted_text, '')) @@ websearch_to_tsquery('english', :q)
        ORDER BY ts_rank(to_tsvector('english', coalesce(dc.extracted_text, '')),
                         websearch_to_tsquery('english', :q)) DESC,
                 d.updated_at DESC,
                 d.id
        """,
        countQuery = """
            SELECT count(*)
            FROM document d
                 JOIN client c ON c.id = d.client_id
                 JOIN document_content dc ON dc.document_id = d.id
            WHERE c.user_id = :userId
              AND d.status <> 'DELETED'
              AND to_tsvector('english', coalesce(dc.extracted_text, '')) @@ websearch_to_tsquery('english', :q)
            """,
        nativeQuery = true)
    Page<SearchResultProjection> search(@Param("userId") UUID userId, @Param("q") String q, Pageable pageable);
}
