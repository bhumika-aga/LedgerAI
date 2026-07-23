# ADR-004 — Primary Database (PostgreSQL / Neon)

**Status:** Accepted **Date:** 2026-07-14 **Owner:** Founding Engineer / Principal Database Architect
**Related Documents:
** [PRODUCT_DECISIONS PD-007](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions) · [DATABASE](../DATABASE.md) · [ARCHITECTURE §4](../ARCHITECTURE.md#4-high-level-system-architecture)

---

## Context

LedgerAI needs a durable, reliable primary datastore for structured, relational working data (users, clients, document
metadata, AI requests/outputs, activity) with strong integrity guarantees, some semi-structured fields (preferences,
activity metadata), and text search over extracted content. It must fit the free/low-cost goal
([BG-5](../../00-product/PRD.md#4-goals))
and support additive evolution toward multi-tenancy.

---

## Decision

Use **PostgreSQL**, hosted on **Neon** (serverless Postgres with a generous free tier), as the single primary database.
This confirms [PD-007](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions). PostgreSQL provides ACID
guarantees, rich constraints/foreign keys, `jsonb` for semi-structured data, `citext` for case-insensitive uniqueness,
and built-in full-text search (used for MVP search, [ADR-014](./ADR-014-Search-Strategy.md)).

---

## Alternatives Considered

- **A NoSQL document store (e.g., MongoDB-style).** Rejected: the domain is strongly relational (ownership hierarchies,
  referential integrity); we would reinvent constraints and joins in application code.
- **MySQL/MariaDB.** Reasonable, but PostgreSQL's `jsonb`, `citext`, full-text search, and extension ecosystem (e.g.,
  future `pgvector`) fit LedgerAI's roadmap better.
- **A managed cloud-proprietary database.** Rejected: risks lock-in and cost; standard PostgreSQL keeps portability.

---

## Consequences

### Advantages

- Strong integrity and relational modeling match the domain directly.
- `jsonb`/`citext`/full-text search cover MVP needs without extra infrastructure.
- Neon's serverless free tier fits the cost goal; standard Postgres avoids lock-in and eases future migration.
- A clean, additive path to `pgvector`/embeddings later ([ADR-014](./ADR-014-Search-Strategy.md), future).

### Disadvantages

- Serverless free tiers can have cold starts / connection limits to design around.
- Relational schemas require deliberate migration discipline
  ([DATABASE §Migration Strategy](../DATABASE.md#database-migration-strategy)).

### Trade-offs

- We accept serverless free-tier constraints for near-zero cost and strong guarantees, keeping standard Postgres for
  portability over any proprietary performance edge.

---

## Future Reconsideration

Revisit hosting (not the engine) if Neon's limits are exceeded — migrating to another managed PostgreSQL is low-friction
precisely because we use standard Postgres. Revisit the engine only under a fundamental shift in data model or scale
that PostgreSQL cannot serve, which is not foreseeable for this product.

---

## References

[PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md) · [DATABASE](../DATABASE.md) · [ADR-014](./ADR-014-Search-Strategy.md)
