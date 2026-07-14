# ADR-002 — Storage Provider

**Status:** Deferred
**Date:** 2026-07-14
**Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [PRODUCT_DECISIONS DD-001](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) · [ADR-008 (Object Storage pattern)](./ADR-008-Object-Storage.md) · [DATABASE §1.3](../DATABASE.md#13-related-documents) · [SECURITY §9](../SECURITY.md#9-file-upload-security)

---

## Context

Uploaded documents are stored in an **external object store**, with the database holding only a reference (the pattern
is
decided in [ADR-008](./ADR-008-Object-Storage.md)). This ADR concerns **which concrete provider** to use. The candidates
identified in the product plan are **Cloudinary** and **Supabase Storage**, both offering usable free tiers. The choice
depends on free-tier limits, file-handling ergonomics, security controls (private buckets, signed/expiring URLs), and
operational fit — details that warrant an evidence-based comparison rather than a snap decision.

---

## Decision

**Deferred.** The concrete storage provider is not yet selected. Because business logic depends only on the domain's
**Storage port** ([ADR-008](./ADR-008-Object-Storage.md)), the choice is reversible and can be made late without
affecting the rest of the system. This decision **MUST be resolved before the Document Upload module is built**
(Milestone 3), and the chosen provider MUST undergo a security
review ([SECURITY Review Process](../SECURITY.md#security-review-process)).

---

## Alternatives Considered

- **Cloudinary.** Strong media handling and transformations; generous free tier. To be evaluated for private/secure
  document handling and signed-URL access.
- **Supabase Storage.** S3-compatible object storage with a free tier that pairs naturally with a Postgres-centric
  stack; to be evaluated for limits and access controls.
- **Store binaries in PostgreSQL.** Rejected in [ADR-008](./ADR-008-Object-Storage.md) — DB bloat, cost, and backup
  weight; not reconsidered here.
- **Self-hosted object storage.** Rejected for MVP: operational overhead against the free-tier, low-ops goal.

---

## Consequences

### Advantages

- Deferring avoids a premature commitment while the port keeps the system provider-independent.
- The evaluation can use real requirements (limits, security, ergonomics) rather than guesses.

### Disadvantages

- An open decision remains on the critical path for Document Upload.
- Adapter-specific work (signed URLs, lifecycle) cannot be finalized until chosen.

### Trade-offs

- We accept a short-lived open question in exchange for a better-informed, low-risk choice enabled by the storage
  abstraction.

---

## Future Reconsideration

Resolve before Milestone 3. Revisit the chosen provider if free-tier limits are exceeded, security/compliance needs grow
(e.g., regional residency, customer-managed keys), or cost/ergonomics change materially. Switching means
adding/selecting
another Storage adapter — no business-logic change.

---

## References

[PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md) · [ADR-008](./ADR-008-Object-Storage.md) · [DATABASE](../DATABASE.md) · [SECURITY](../SECURITY.md)
