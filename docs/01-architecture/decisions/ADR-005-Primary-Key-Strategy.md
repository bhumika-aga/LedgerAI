# ADR-005 — Primary Key Strategy (UUID v7)

**Status:** Accepted **Date:** 2026-07-14 **Owner:** Founding Engineer / Principal Database Architect
**Related Documents:
** [DATABASE §7](../DATABASE.md#7-primary-key-strategy) · [PRODUCT_DECISIONS PD-007](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions) · [API_SPEC §2.9](../API_SPEC.md#29-uuids) · [SECURITY §5](../SECURITY.md#5-authorization)

---

## Context

Every entity needs a primary key exposed in URLs and API payloads. LedgerAI stores confidential client data with strict
per-user isolation ([BR-004](../../00-product/SRS.md#5-business-rules)), so identifiers must not be guessable or reveal
volume. The scheme should also ease a future multi-tenancy/distribution path without central-sequence coordination.

---

## Decision

Use **UUID** primary keys for all entities, application-generated, preferring **UUID v7** (time-ordered). UUID v7 keeps
UUID's non-enumerability while recovering most of the index locality that random UUID v4 sacrifices. Identifiers appear
as UUID strings in the API ([API_SPEC §2.9](../API_SPEC.md#29-uuids)).

---

## Alternatives Considered

- **BIGSERIAL (sequential integers).** Rejected: sequential IDs are **enumerable** — they leak record counts and enable
  ID-guessing against confidential data — and a central sequence complicates future distribution/merging.
- **Random UUID v4.** Viable and non-enumerable, but random ordering hurts B-tree index locality and insert performance.
  UUID v7 gives the same security property with better locality.
- **Composite/natural keys.** Rejected: brittle, leak business data into keys, and complicate relationships.

---

## Consequences

### Advantages

- Non-enumerable IDs materially reduce cross-user probing risk and pair naturally with ownership authorization.
- Safe to generate application-side; merges cleanly across environments; additive multi-tenancy/distribution.
- UUID v7's time ordering preserves good index locality.

### Disadvantages

- 16 bytes vs. 8 for BIGSERIAL — modest storage/index overhead.
- UUID v7 tooling/support must be confirmed at implementation.

### Trade-offs

- We accept slightly larger keys for a real security property (non-enumerability) and distribution-friendliness — a
  deliberate choice not to trade security for premature optimization.

---

## Future Reconsideration

Revisit only if a measured performance problem attributable to key size/locality emerges at scale that UUID v7 does not
resolve — unlikely at MVP or moderate SaaS volumes. The API contract (UUID strings) would remain stable regardless.

---

## References

[DATABASE §7](../DATABASE.md#7-primary-key-strategy) · [API_SPEC](../API_SPEC.md) · [SECURITY](../SECURITY.md) · [PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md)
