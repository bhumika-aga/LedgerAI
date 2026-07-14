# ADR-015 — Observability

**Status:** Accepted (baseline)
**Date:** 2026-07-14
**Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [ARCHITECTURE §9.7](../ARCHITECTURE.md#9-cross-cutting-concerns) · [SECURITY §16](../SECURITY.md#16-logging-and-audit) · [AI_ARCHITECTURE §14](../AI_ARCHITECTURE.md#14-ai-observability) · [SRS NFR-013/014](../../00-product/SRS.md#9-non-functional-requirements)

---

## Context

The team must be able to diagnose failures, track reliability, and watch AI cost/quality — without exposing confidential
content. LedgerAI depends on external providers (AI/OCR/Storage) whose failures must be visible, and it processes
sensitive documents that MUST NOT leak into logs ([NFR-013](../../00-product/SRS.md#9-non-functional-requirements)). We
need an observability baseline that is useful yet cheap and privacy-preserving for the MVP.

---

## Decision

Establish a **baseline observability** approach: structured application logging, meaningful error logging, basic health
and error-rate visibility, a request **correlation id** (`traceId`) propagated to RFC 7807 error responses
([API_SPEC §2.12](../API_SPEC.md#212-error-model--rfc-7807-problem-details)), and AI metrics (latency, token usage,
cost,
success/error rates) built from **metadata only** ([AI_ARCHITECTURE §14](../AI_ARCHITECTURE.md#14-ai-observability)).
**Sensitive content is never logged** — no passwords/tokens/secrets, no document text, no AI prompt/response content
([SECURITY §16](../SECURITY.md#16-logging-and-audit)). The user-facing, immutable **Activity timeline** provides the
audit
trail ([NFR-012](../../00-product/SRS.md#9-non-functional-requirements)).

---

## Alternatives Considered

- **A full observability platform (distributed tracing, APM, dashboards) from day one.** Rejected for MVP: cost and
  setup overhead beyond what early validation needs; the baseline can grow into this.
- **Verbose logging including request/response bodies.** Rejected outright: would log confidential document content and
  secrets — a direct violation of NFR-013 and the security model.
- **Minimal/no structured logging.** Rejected: would make failures (especially provider issues) hard to diagnose and
  undermine reliability.

---

## Consequences

### Advantages

- Enough signal to diagnose failures and track reliability/cost with near-zero infrastructure.
- Privacy-preserving by construction (metadata/metrics, not content).
- `traceId` correlation ties API errors, logs, and AI request records together.

### Disadvantages

- No deep distributed tracing/APM yet — some complex issues are harder to root-cause.
- Metrics are coarse until a richer platform is added.

### Trade-offs

- We accept coarser visibility for the MVP in exchange for low cost and guaranteed confidentiality, with a clear upgrade
  path.

---

## Future Reconsideration

Introduce richer observability (tracing, APM, metrics dashboards, alerting) as scale, team size, or reliability targets
grow — an additive change. Any such expansion MUST preserve the "no sensitive content in telemetry" rule; a material
change to the telemetry model would warrant an updated ADR and a security review.

---

## References

[ARCHITECTURE §9](../ARCHITECTURE.md#9-cross-cutting-concerns) · [SECURITY §16](../SECURITY.md#16-logging-and-audit) · [AI_ARCHITECTURE §14](../AI_ARCHITECTURE.md#14-ai-observability) · [SRS](../../00-product/SRS.md)
