# ADR-014 — Search Strategy

**Status:** Accepted (MVP); semantic/vector search **Deferred**
**Date:** 2026-07-14
**Owner:** Founding Engineer / Principal Database Architect
**Related Documents:
** [DATABASE §9](../DATABASE.md#9-indexing-strategy) · [SRS §4.11](../../00-product/SRS.md#411-global-search-srch) · [PRODUCT_DECISIONS DD-003](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)

---

## Context

Global Search must let a professional find documents and content across their own
clients ([FR-SRCH-001](../../00-product/SRS.md#411-global-search-srch)),
reflecting extracted text and metadata, owner-scoped, excluding deleted documents. It must fit the free-tier goal (no
extra search infrastructure) while leaving room for future semantic search.

---

## Decision

For the MVP, implement search using **PostgreSQL's built-in full-text search** (`tsvector` + a **GIN** index on
`document_content.extracted_text`), scoped to the owning user and filtered to non-deleted, Ready documents
([DATABASE §9](../DATABASE.md#9-indexing-strategy)). **Semantic/vector search (embeddings)** is **deferred**
([DD-003](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)) and can be added additively later without
changing
this design.

---

## Alternatives Considered

- **A dedicated external search engine (e.g., Elasticsearch/OpenSearch).** Rejected for MVP: significant extra
  infrastructure, cost, and ops — unjustified for keyword search at MVP scale, and against the free-tier goal.
- **A vector database / embeddings now.** Rejected for MVP: semantic search is valuable but not required for the MVP's
  keyword-find use case; it adds infrastructure and depends on the deferred AI
  provider ([DD-002](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)).
- **Naive `LIKE`/`ILIKE` scans.** Rejected: no ranking, poor performance at scale, no proper text analysis; PostgreSQL
  full-text search is strictly better with a GIN index.

---

## Consequences

### Advantages

- Zero additional infrastructure — search lives in the database we already run.
- Proper tokenization, ranking, and indexing sufficient for MVP keyword search.
- Owner-scoping and deleted-exclusion enforced by the same query conventions as everything else.

### Disadvantages

- Keyword (lexical) search only — no semantic/conceptual matching in the MVP.
- Full-text search tuning (configurations, ranking) has a learning curve.

### Trade-offs

- We accept lexical-only search for the MVP in exchange for zero extra infrastructure, with a clean additive path to
  semantic search when it earns its place.

---

## Future Reconsideration

Add semantic/vector search when users need conceptual matching or when RAG is introduced — likely via `pgvector` (an
embedding table keyed to `DocumentContent`) so full-text search continues alongside it
([DATABASE §13](../DATABASE.md#13-future-database-evolution), [AI_ARCHITECTURE §16](../AI_ARCHITECTURE.md#16-future-ai-evolution)).
That addition warrants its own (Accepted) ADR.

---

## References

[DATABASE](../DATABASE.md) · [SRS §4.11](../../00-product/SRS.md#411-global-search-srch) · [AI_ARCHITECTURE](../AI_ARCHITECTURE.md) · [PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md)
