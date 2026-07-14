# ADR-010 — AI Request Lifecycle

**Status:** Accepted
**Date:** 2026-07-14
**Owner:** Founding Engineer / Principal AI Architect
**Related Documents:
** [SRS §7.2](../../00-product/SRS.md#72-ai-request-lifecycle) · [AI_ARCHITECTURE §4](../AI_ARCHITECTURE.md#4-ai-request-lifecycle) · [DATABASE §5.5–5.6](../DATABASE.md#55-airequest) · [API_SPEC §2.11](../API_SPEC.md#211-async-ready-behavior)

---

## Context

AI actions (summary, chat, email, report) are long-running, can fail (provider timeout/outage/rate limit), and must
remain **non-blocking** with visible status ([NFR-002](../../00-product/SRS.md#9-non-functional-requirements)). The
processing mechanism (synchronous vs. background worker) is a **deferred**
decision ([DD-007](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)).
We need a model for AI work that is observable, retryable, and doesn't lock us into a processing mechanism now.

---

## Decision

Model every AI action as an explicit **AI Request lifecycle** — `REQUESTED → IN_PROGRESS → COMPLETED | FAILED`
([SRS §7.2](../../00-product/SRS.md#72-ai-request-lifecycle)) — persisted as an `AIRequest` record with its editable
result in a separate `AIOutput` ([DATABASE §5.5–5.6](../DATABASE.md#55-airequest)). The API is **async-ready**: a
generation endpoint returns `201` when synchronous or `202 Accepted` + poll when asynchronous, so the backend can move
to
background workers with **no contract change** ([API_SPEC §2.11](../API_SPEC.md#211-async-ready-behavior)). The provider
call sits **outside** the database transaction; only persistence of its result is transactional.

---

## Alternatives Considered

- **Synchronous, blocking request/response only.** Rejected: risks long request holds/timeouts, blocks the UI, and would
  force a breaking API change to adopt background processing later.
- **Fire-and-forget with no persisted state.** Rejected: loses observability, retry, and failed-attempt history; the
  user can't see progress or outcomes.
- **Commit to a full job/queue framework now.** Rejected for MVP: premature infrastructure and cost (DD-007). The
  explicit lifecycle + async-ready API provides the seam without the framework.

---

## Consequences

### Advantages

- Non-blocking UX with visible status; clean retry and failure semantics.
- Failed attempts are recorded (via `AIRequest`) for observability without bloating outputs.
- Background workers can be added later transparently (API and callers unchanged).

### Disadvantages

- The frontend must handle the `202` + poll path, not just synchronous responses.
- Two records (request + output) to manage per action.

### Trade-offs

- We accept modest client-side polling complexity now to avoid a future breaking change and to gain observability and
  graceful failure handling.

---

## Future Reconsideration

Resolve the concrete processing mechanism (sync-with-status vs. background workers) as volume/latency demands — see
[ADR-013](./ADR-013-Background-Processing.md). The lifecycle and async-ready contract themselves are durable; changing
the *mechanism* behind them does not require changing this model.

---

## References

[SRS §7.2](../../00-product/SRS.md#72-ai-request-lifecycle) · [AI_ARCHITECTURE](../AI_ARCHITECTURE.md) · [DATABASE](../DATABASE.md) · [API_SPEC](../API_SPEC.md) · [ADR-013](./ADR-013-Background-Processing.md)
