# ADR-013 — Background Processing

**Status:** Deferred
**Date:** 2026-07-14
**Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [PRODUCT_DECISIONS DD-007](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) · [ARCHITECTURE §9.10](../ARCHITECTURE.md#9-cross-cutting-concerns) · [ADR-010 (AI Request Lifecycle)](./ADR-010-AI-Request-Lifecycle.md) · [API_SPEC §2.11](../API_SPEC.md#211-async-ready-behavior)

---

## Context

OCR and AI actions are long-running and would ideally run as asynchronous background work at scale. However, MVP volumes
may not justify the added infrastructure and complexity of a job/queue framework. The system is already designed to be
**async-ready** — explicit request lifecycles ([ADR-010](./ADR-010-AI-Request-Lifecycle.md)) and a `201`/`202`+poll API
contract ([API_SPEC §2.11](../API_SPEC.md#211-async-ready-behavior)) — so the *mechanism* can be chosen later.

---

## Decision

**Deferred.** The concrete background-processing mechanism (e.g., in-process async, a job table with workers, or an
external queue) is **not** selected for the MVP. The MVP MAY process synchronously-with-status behind the existing
service boundaries; a background-worker mechanism can be introduced later **without** changing the API contract or
business logic, because the async-ready seam already
exists ([ARCHITECTURE §9.10](../ARCHITECTURE.md#9-cross-cutting-concerns)).

---

## Alternatives Considered

- **Adopt a full queue/worker framework now.** Rejected for MVP: premature infrastructure, cost, and operational
  overhead before volume justifies it.
- **Never process asynchronously (always synchronous).** Rejected as a long-term stance: long AI/OCR jobs will
  eventually strain synchronous request handling; the design must leave room for async.
- **A simple database-backed job table + poller.** A strong *future* candidate (low infra, fits the free tier), recorded
  here but not committed for the MVP.

---

## Consequences

### Advantages

- Avoids premature infrastructure and cost; keeps the MVP simple.
- The async-ready design means adopting background processing later is additive and non-breaking.

### Disadvantages

- Synchronous-with-status processing has practical limits (latency, connection/thread usage) under heavier load.
- Leaves an open decision that must be made before scale demands it.

### Trade-offs

- We accept the ceiling of synchronous processing for the MVP in exchange for simplicity, knowing the seam to add
  workers
  is already in place.

---

## Future Reconsideration

Resolve when processing latency, throughput, or free-tier request limits become real constraints — likely early in the
"Growing SaaS" stage ([ARCHITECTURE §11](../ARCHITECTURE.md#11-scalability-strategy)). A database-backed job table is
the
expected first step. Selecting the mechanism will produce an updated (Accepted) ADR.

---

## References

[PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md) · [ARCHITECTURE](../ARCHITECTURE.md) · [ADR-010](./ADR-010-AI-Request-Lifecycle.md) · [API_SPEC](../API_SPEC.md)
