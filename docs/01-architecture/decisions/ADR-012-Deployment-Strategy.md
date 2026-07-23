# ADR-012 — Deployment Strategy (Vercel + Render + Neon)

**Status:** Accepted **Date:** 2026-07-14 **Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [PRODUCT_DECISIONS PD-009](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions) · [ARCHITECTURE §11](../ARCHITECTURE.md#11-scalability-strategy) · [ARCHITECTURE ADS-007](../ARCHITECTURE.md#16-architecture-decision-summary)

---

## Context

LedgerAI must run within free/low-cost tiers for MVP validation ([BG-5](../../00-product/PRD.md#4-goals)) with minimal
operational overhead for a small team. The system is a React SPA plus a Spring Boot modular monolith plus PostgreSQL. We
need a hosting topology that fits each part's needs and keeps ops simple.

---

## Decision

Deploy the **frontend (SPA) on Vercel**, the **backend (single Spring Boot deployable) on Render**, and the **database
on Neon (serverless PostgreSQL)** —
confirming [PD-009](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions)
and ADS-007. Each platform is chosen as the best-fit free tier for its workload; traffic is HTTPS end-to-end with the
SPA calling the backend across origins (handled by CORS + Bearer
tokens, [SECURITY §15](../SECURITY.md#15-cors-and-csrf)).

---

## Alternatives Considered

- **Single platform for everything (one PaaS hosting SPA + API + DB).** Rejected: no single free tier serves all three
  as well; specialized platforms (Vercel for SPA, Render for services, Neon for Postgres) each fit better.
- **Self-managed VPS/containers for all tiers.** Rejected for MVP: operational burden (patching, scaling, TLS, backups)
  unjustified at this stage and against the low-ops goal.
- **Serverless functions for the backend.** Rejected: the Spring Boot modular monolith is a long-lived service; a
  container host (Render) fits it better than function cold-starts for a stateful JVM app.

---

## Consequences

### Advantages

- Best-fit, low-cost hosting per workload; simple CI/CD; minimal ops.
- Clean separation of concerns; each tier scales/upgrades independently.
- Standard platforms keep portability (no deep proprietary lock-in).

### Disadvantages

- Free tiers bring cold starts, sleep, and quota limits to design around
  ([ARCHITECTURE §14](../ARCHITECTURE.md#14-architecture-risks)).
- Cross-origin frontend/backend requires deliberate CORS configuration.

### Trade-offs

- We accept free-tier constraints (cold starts, limits) for near-zero cost during validation, with a clear
  vertical-then- extract scaling path ([ARCHITECTURE §11](../ARCHITECTURE.md#11-scalability-strategy)).

---

## Future Reconsideration

Revisit when free-tier limits or cold-start latency materially affect users, or when scale warrants paid tiers,
background workers, read replicas, or moving hot modules to separate services. Because the stack is standard, migrating
hosts is low-friction; a change of topology would warrant an updated ADR.

---

## References

[PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md) · [ARCHITECTURE](../ARCHITECTURE.md) · [SECURITY](../SECURITY.md)
