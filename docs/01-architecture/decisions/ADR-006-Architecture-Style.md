# ADR-006 — Architecture Style (Modular Monolith)

**Status:** Accepted
**Date:** 2026-07-14
**Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [ARCHITECTURE §3](../ARCHITECTURE.md#3-architecture-style) · [ARCHITECTURE §5](../ARCHITECTURE.md#5-backend-architecture) · [ARCHITECTURE ADS-001/003](../ARCHITECTURE.md#16-architecture-decision-summary)

---

## Context

LedgerAI is a twelve-module MVP built by a small team, deployed on a single backend host (Render free tier), that must
ship quickly, stay maintainable, keep strong module boundaries, and isolate external providers (AI/OCR/Storage) — while
leaving room to grow without a rewrite. We must choose an overall architecture style and a backend package philosophy.
(This ADR also covers the backend **package-organization** decision originally tracked as ADR-003-Package-Structure.)

---

## Decision

Adopt a **Modular Monolith** with **pragmatic layering inside each module** (Controller → Service → Repository) and
**ports-and-adapters isolation only at the external-service boundary** (AI, OCR, Storage). Backend packages are
organized
**domain-first** (by module: auth, users, clients, documents, ocr, ai, reports, search, timeline), layered internally;
cross-module interaction happens **only through published services**. This is ADS-001/003 from
[ARCHITECTURE §16](../ARCHITECTURE.md#16-architecture-decision-summary).

---

## Alternatives Considered

- **Pure layered (technical packages).** Rejected: organizing by layer scatters a feature across packages and lets
  boundaries erode as modules multiply.
- **Full Clean/Hexagonal across every module.** Rejected for MVP: interface/mapper ceremony over-engineers a 12-module
  MVP and slows delivery; we apply hexagonal isolation only where it pays (external providers).
- **Microservices from day one.** Rejected: operational and cost overhead unjustified at MVP scale; premature
  distribution. The modular monolith can peel off services later along existing seams if ever justified.

---

## Consequences

### Advantages

- One simple deployable (fits the free tier) with strong internal module boundaries.
- Fast to build and onboard; features are locatable by domain.
- Provider independence exactly where it matters; growth is additive (extract modules later).

### Disadvantages

- Requires discipline to keep module boundaries from eroding into a "big ball of mud."
- A single deployable scales as a unit until modules are extracted.

### Trade-offs

- We trade the independent scalability of microservices for simplicity and speed now, keeping future extraction possible
  via clean boundaries — the right trade for a free-tier MVP.

---

## Future Reconsideration

Revisit if specific modules develop load profiles that justify independent scaling/deployment, or team size grows enough
that separate service ownership helps. Extraction should follow existing module boundaries; any such change requires a
new ADR ([Guiding Architectural Rules](../ARCHITECTURE.md#guiding-architectural-rules)).

---

## References

[ARCHITECTURE](../ARCHITECTURE.md) · [PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md) · [CLAUDE.md](../../../CLAUDE.md)
