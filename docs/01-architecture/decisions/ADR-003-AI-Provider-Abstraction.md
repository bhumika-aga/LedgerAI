# ADR-003 — AI Provider Abstraction

**Status:** Accepted
**Date:** 2026-07-14
**Owner:** Founding Engineer / Principal AI Architect
**Related Documents:
** [PRODUCT_DECISIONS PD-010 / DD-002](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions) · [AI_ARCHITECTURE §6](../AI_ARCHITECTURE.md#6-provider-architecture) · [ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services)

---

## Context

AI capabilities (summary, chat, email, report) are core to LedgerAI, but the concrete AI/LLM provider is a **deferred**
decision ([DD-002](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)) and the market moves quickly. Binding
business logic to a specific vendor SDK would make the provider choice effectively irreversible and leak vendor concepts
into the domain. We need to be able to adopt, swap, or add providers without touching business logic.

---

## Decision

Access all AI capabilities through a **domain-owned AI port** (an interface expressed in domain terms), with each
provider implemented as an **adapter** containing request/response mappers. The active adapter is selected by
**configuration**. Business logic depends only on the port; no domain code imports a provider SDK. This realizes
[PD-010](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions) and the ports-and-adapters stance of
[ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services).

---

## Alternatives Considered

- **Direct provider SDK calls in services.** Rejected: vendor lock-in, irreversible choice, vendor types polluting the
  domain, and violation of the [Guiding Architectural Rules](../ARCHITECTURE.md#guiding-architectural-rules).
- **A heavyweight multi-provider gateway/framework.** Rejected for MVP: over-engineering; adds a dependency and
  complexity the MVP does not need. The thin port/adapter gives independence without the weight.
- **Commit to a single provider now, refactor later.** Rejected: "later" refactors of pervasive SDK calls are costly and
  risky; the abstraction is cheap to build up front and defers the actual provider choice safely.

---

## Consequences

### Advantages

- The provider decision (DD-002) stays genuinely reversible; a fallback/second provider is just another adapter.
- The domain stays clean, testable (mockable port), and provider-neutral.
- Enables per-capability model selection and future model routing behind the same seam.

### Disadvantages

- One layer of indirection (port + mappers) to build and maintain.
- The port must be designed well enough to fit multiple providers' capabilities.

### Trade-offs

- We accept a modest abstraction cost to secure provider independence — a clearly worthwhile trade for a core, fast-
  moving dependency.

---

## Future Reconsideration

The **abstraction** is a durable decision. What remains open is the concrete
provider ([DD-002](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)),
selected before AI Summary implementation and recorded then. Revisit the port's shape if future capabilities (RAG, tool
calling, agents) require richer contracts — extend the port additively.

---

## References

[PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md) · [AI_ARCHITECTURE](../AI_ARCHITECTURE.md) · [ARCHITECTURE](../ARCHITECTURE.md)
