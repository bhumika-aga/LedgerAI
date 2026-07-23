# ADR-007 — Frontend Architecture

**Status:** Accepted **Date:** 2026-07-14 **Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [ARCHITECTURE §6](../ARCHITECTURE.md#6-frontend-architecture) · [ARCHITECTURE ADS-005](../ARCHITECTURE.md#16-architecture-decision-summary) · [PRODUCT_DECISIONS PD-006](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions) · [API_SPEC](../API_SPEC.md)

---

## Context

The approved frontend stack is **React + TypeScript + Vite + Material UI + React Query + Axios
** ([PD-006](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions)), deployed on Vercel. The app is
data-heavy with long-running AI/OCR operations that must remain **non-blocking** with visible status
([NFR-002](../../00-product/SRS.md#9-non-functional-requirements)). We must decide how to organize the frontend, manage
state, and talk to the API.

---

## Decision

- **Feature-first organization** mirroring the backend domains (auth, clients, documents, chat, reports, search,
  timeline, profile).
- **Server state via React Query** (fetching, caching, background refresh, loading/error status — ideal for async AI/OCR
  polling); **minimal local UI state**; **no heavyweight global store** in the MVP.
- **A single centralized API layer** (Axios) that attaches auth tokens, handles refresh, and normalizes errors to the
  RFC 7807 taxonomy ([API_SPEC §2.12](../API_SPEC.md#212-error-model--rfc-7807-problem-details)).
- **Layered components:** route/page → feature → shared/presentational, with data fetching in feature-level hooks.

This is ADS-005 from [ARCHITECTURE §16](../ARCHITECTURE.md#16-architecture-decision-summary).

---

## Alternatives Considered

- **Global state library (e.g., Redux) up front.** Rejected for MVP: most state is *server* state, which React Query
  handles better; a global store would add boilerplate and complexity (KISS).
- **Type-organized folders (all components/, all hooks/).** Rejected: scatters a feature; feature-first keeps a
  capability understandable end-to-end and mirrors the backend.
- **Raw fetch calls in components.** Rejected: cross-cutting concerns (auth, refresh, error normalization) would be
  duplicated; a centralized API layer localizes them.

---

## Consequences

### Advantages

- Async-heavy UX (AI/OCR) is handled idiomatically with built-in loading/error/polling.
- Onboarding is easy; features map 1:1 to backend modules.
- Cross-cutting API concerns live in one place.

### Disadvantages

- React Query patterns have a learning curve for those new to it.
- Deferring a global store may require introducing one later if genuinely shared client state emerges.

### Trade-offs

- We accept adding a global store *only if needed later* rather than paying its cost now — favoring simplicity for the
  MVP.

---

## Future Reconsideration

Introduce a dedicated global-state solution only when real, broadly-shared client state appears. Revisit component/state
structure if the app grows well beyond the MVP feature set. Concrete folder structure is an implementation detail and
does not require a new ADR.

---

## References

[ARCHITECTURE §6](../ARCHITECTURE.md#6-frontend-architecture) · [API_SPEC](../API_SPEC.md) · [PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md)
