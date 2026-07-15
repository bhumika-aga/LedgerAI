# AI Providers — LedgerAI MVP

> **Status:** Draft v1
> **Owner:** Principal AI Architect
> **Last updated:** 2026-07-15
> **Upstream (frozen):
** [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) · [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) · [SECURITY](../01-architecture/SECURITY.md) · [ARCHITECTURE](../01-architecture/ARCHITECTURE.md) · [SRS](../00-product/SRS.md)
> **Related:
** [PROMPTS](./PROMPTS.md) · [EVALUATION](./EVALUATION.md) · [RAG](./RAG.md) · [CLAUDE.md](../../CLAUDE.md)

---

## 1. Purpose

### Why this document exists

LedgerAI's concrete AI provider is a **deliberately deferred decision
** ([DD-002](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)). The *abstraction* that makes that deferral safe
is already settled: [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) decided that AI is
reached through a domain-owned port with per-provider adapters,
and [AI_ARCHITECTURE §6](../01-architecture/AI_ARCHITECTURE.md#6-provider-architecture) defines that architecture.

What no document yet owns is the **governance around the deferral**: how a provider becomes a candidate, how it is
judged, who approves it, what "active" means, and how it is replaced or retired without the product noticing. DD-002
will be resolved once — and then, over the product's life, resolved again. This document owns that process.

The governing principle of this document:

> **A provider is a replaceable part, never a foundation.**
>
> The moment any product behavior, requirement, or user-visible promise depends on *which* provider is active, the
> deferral has failed and the choice has become irreversible in practice — whatever the architecture diagram says.

It is **not** a prompt document, **not** a RAG document, **not** an evaluation report, and **not** implementation. It
contains **no vendor names, no pricing, no benchmarks, no SDK or API detail, no credentials, no environment
definitions, and no prompt text**. It governs *how providers are selected, governed, and swapped* — never how one is
integrated.

### Relationship to the AI documents

| Document                                                    | Its job                                     | The boundary with this document                                                                                                                                                                                                                                                                                                                                                 |
|-------------------------------------------------------------|---------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [AI_ARCHITECTURE.md](../01-architecture/AI_ARCHITECTURE.md) | **AI behavior and the orchestration model** | It owns how AI is used and how the abstraction is shaped — the port, adapters, mappers, pipeline, grounding, lifecycle, and failure model. This document owns **which provider sits behind that port and how it got there**. This document restates none of that architecture; it governs the choice the architecture makes swappable.                                          |
| [PROMPTS.md](./PROMPTS.md)                                  | **Prompt content and structure**            | Prompts are composed centrally and provider-neutrally ([AI_ARCHITECTURE §8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture)). This document never specifies prompt text. Where a provider needs prompt shaping to behave, that is a **provider-specific assumption** this document requires be documented (§4) — not a licence to fork the prompts.                |
| [EVALUATION.md](./EVALUATION.md)                            | **How quality is measured**                 | This document defines the criteria a provider is judged *against* (§6) and the lifecycle gates that require judgement (§5). It does **not** define how measurement is performed, scored, or reported — that is EVALUATION's. This document consumes a verdict; it does not produce one.                                                                                         |
| [RAG.md](./RAG.md)                                          | **Retrieval design, if and when it exists** | Retrieval strategy is deferred ([DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)) and coupled to the provider decision. If retrieval arrives it may impose new capability requirements (§4); those enter through this document's review, and the port is extended **additively** ([ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md)). |
| [SECURITY.md](../01-architecture/SECURITY.md)               | **The security posture**                    | It owns the AI security controls ([§10](../01-architecture/SECURITY.md#10-ai-security)) and secrets handling ([§13](../01-architecture/SECURITY.md#13-secrets-management)). This document owns only the **provider-selection consequences** of those controls: that a candidate which cannot satisfy them is not a candidate (§10).                                             |
| **The ADRs**                                                | **Ratified decisions**                      | [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) decided the abstraction; [ADR-010](../01-architecture/decisions/ADR-010-AI-Request-Lifecycle.md) the request lifecycle. This document operates **inside** those decisions and MUST NOT alter one. A change that would is an ADR, not a provider review ([CLAUDE.md §8](../../CLAUDE.md)).            |

In one line each:

> **AI_ARCHITECTURE defines the AI behavior and orchestration model. AI_PROVIDERS defines how providers are selected,
> governed, and swapped. PROMPTS defines prompt content and structure. EVALUATION defines how provider and model quality
> is measured. RAG defines retrieval design, if and when it exists.**

### Relationship to the frozen documents

This document introduces **no product behavior** and **no architecture**. It serves the capabilities granted by
[PRD](../00-product/PRD.md) and [SRS](../00-product/SRS.md), operates within
[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) and the ports-and-adapters stance of
[ARCHITECTURE §10](../01-architecture/ARCHITECTURE.md#10-external-services), and realizes
[PD-010](../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions). Where a provider need would require new
behavior, a new capability, or a change to a ratified decision, it stops and is raised per
[CLAUDE.md §8](../../CLAUDE.md).

---

## 2. AI Provider Philosophy

These principles explain *why* provider governance is shaped the way it is. They are the reasoning behind the
enforceable rules that follow.

| Principle                       | Why it exists                                                                                                                                                                                                                                                                                                  |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Provider neutrality**         | The AI market moves faster than this product will. A choice that is correct today is a liability the moment it is assumed rather than configured. Neutrality is what keeps a fast-moving external dependency from setting the product's pace.                                                                  |
| **Port-based integration**      | A single, domain-expressed seam is what makes every other principle here enforceable. Without one boundary, provider concerns spread to the places that call AI, and "swap the provider" silently becomes "rewrite the features" ([ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md)). |
| **Replaceability**              | A provider that cannot be removed is a provider that owns the product. Replaceability must be a property that is *maintained and tested*, not a claim made once when the abstraction was built and never exercised.                                                                                            |
| **Security by default**         | Confidential client financials cross this boundary. A provider is an untrusted external system by construction, and security fitness is a condition of candidacy — not something negotiated after a favourite has been chosen ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)).                 |
| **Privacy by design**           | The accountant's duty of confidentiality does not pause at our network edge. What leaves the system is decided by what the task requires, never by what is convenient to send.                                                                                                                                 |
| **Deterministic orchestration** | The model is probabilistic; everything around it must not be. Lifecycle, validation, retries, and failure handling are deterministic so that the one uncertain element is contained rather than compounded ([AI_ARCHITECTURE §11](../01-architecture/AI_ARCHITECTURE.md#11-ai-output-validation)).             |
| **Capability over brand**       | Providers are judged on what they demonstrably do for *our* use cases, not on reputation, popularity, or benchmark standing. Reputation is a claim about someone else's workload.                                                                                                                              |
| **Observable behavior**         | A provider whose behavior cannot be observed cannot be governed — regression is only detectable if the baseline was visible. Unobservable behavior degrades silently, which is the worst failure mode for a product built on trust.                                                                            |
| **Graceful degradation**        | External dependencies fail; this one fails probabilistically as well as operationally. The product degrades honestly and stays usable rather than presenting a provider's bad day as the accountant's problem ([NFR-004](../00-product/SRS.md#9-non-functional-requirements)).                                 |
| **Long-term portability**       | Portability is a property that decays without maintenance. Every convenience taken against a specific provider is a small mortgage on the next migration, and the bill arrives when the choice is least convenient to revisit.                                                                                 |

---

## AI Provider Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Every provider MUST be accessed through the approved AI abstraction layer.** *The port is the single seam at which
  provider concerns stop. Any second path around it makes the abstraction decorative, because the escape hatch becomes
  the norm under deadline ([AI_ARCHITECTURE §6](../01-architecture/AI_ARCHITECTURE.md#6-provider-architecture)).*
- **No provider-specific SDK logic MUST leak into product code.** *A vendor type in the domain is lock-in that has
  already happened; it converts a configuration change into a refactor and violates the
  [Guiding Architectural Rules](../01-architecture/ARCHITECTURE.md#guiding-architectural-rules) and
  [PD-010](../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions).*
- **Provider selection MUST be configurable, not hard-coded.** *A choice compiled into the product is a choice that
  requires a release to revisit — and one that cannot differ safely between environments
  ([ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md)).*
- **Provider behavior MUST be evaluated against documented capabilities.** *A capability that is assumed rather than
  demonstrated is discovered in production, on a real client's document. The capability model (§4) is the thing a
  candidate is measured against.*
- **Providers MUST support the product's security and privacy constraints.** *These constraints are not negotiable
  against convenience, price, or quality. A provider that cannot meet them is not a cheaper option; it is not an option
  ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)).*
- **Providers SHOULD be swappable without requiring product redesign.** *This is the test of whether the abstraction is
  real. If swapping requires reworking features, portability was a claim rather than a property — and the deferral of
  [DD-002](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) bought nothing.*
- **Provider-specific assumptions MUST be documented before adoption.** *Every provider imposes some accommodation.
  Undocumented, those accommodations are indistinguishable from requirements to the next maintainer, who preserves them
  faithfully into a migration that did not need them.*
- **Provider failure MUST degrade gracefully.** *Failure is certain, so it is designed rather than discovered. The user
  is told honestly and offered a way forward; the product never disguises a provider's failure as an answer
  ([AI_ARCHITECTURE §12](../01-architecture/AI_ARCHITECTURE.md#12-ai-failure-handling),
  [BR-033](../00-product/SRS.md#5-business-rules)).*
- **New providers MUST undergo review before use.** *A provider admitted informally becomes load-bearing before anyone
  decides it should be. Review is where the decision is made deliberately rather than discovered later (Provider Review
  Process).*
- **Provider decisions MUST remain traceable.** *A provider whose rationale is unrecorded cannot be re-evaluated,
  defended, or safely reversed; it survives on the fact that it is already there — which is the weakest possible reason
  (§12).*

**Why these rules exist.** Vendor lock-in is rarely chosen. It accumulates: one convenient SDK call outside the port,
one capability assumed rather than demonstrated, one accommodation made and never written down. Each is individually
reasonable and none feels like a decision.

These rules exist because the loss is only visible at the moment it is most expensive to fix — when the provider must
change, and it emerges that the abstraction was never exercised. They keep the deferred choice
([DD-002](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)) genuinely reversible and every provider change
reviewable.

---

## 3. Provider Abstraction

**The abstraction itself is owned by [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) (the
decision) and [AI_ARCHITECTURE §6](../01-architecture/AI_ARCHITECTURE.md#6-provider-architecture) (the architecture —
port, adapters, request/response mapping, configuration, error normalization).** This section restates none of it. It
establishes only what the abstraction means *for provider governance*.

**What a provider is here.** A **provider** is an external system that performs an AI action on content LedgerAI sends
it, per request, and returns a result. It is an **untrusted external boundary**, a **replaceable part**, and — for the
purposes of this document — a *candidate for a role*, not a component of the system. Providers are reached through the
port; nothing else about them is the product's business.

**Why the boundary exists at all, in governance terms.** The port is what makes provider choice a *decision* rather than
an *inheritance*. Because business logic depends only on the port, a provider can be judged on its merits and replaced
on its demerits. Without that seam, every provider question becomes an architecture question, and the answer is always
"too expensive to change."

**What the abstraction exposes.** Behavior expressed in domain terms — the AI capabilities the product actually needs
([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)), their lifecycle
([§4](../01-architecture/AI_ARCHITECTURE.md#4-ai-request-lifecycle)), and failures already normalized into the domain's
error taxonomy ([SRS §8](../00-product/SRS.md#8-error-handling)). What crosses the port is meaningful to the product.

**What the abstraction hides, deliberately.** Everything that identifies the provider: its interface shape, its
vocabulary, its error forms, its parameters, its packaging, and its idiosyncrasies. These are not hidden for tidiness —
they are hidden because **anything visible becomes depended upon**. A provider detail that reaches product code stops
being a detail and becomes a constraint on every future provider.

> **Scope boundary:** this document governs **provider selection and lifecycle**, never provider internals. How an
> adapter is built is [AI_ARCHITECTURE §6](../01-architecture/AI_ARCHITECTURE.md#6-provider-architecture) and the
> engineering standards; *whether a provider should be adopted, kept, or retired* is this document.

---

## 4. Provider Capability Model

Capabilities are what a provider is judged on — the vocabulary of §5's gates and §6's criteria. This model is
**conceptual and comparative, never numeric**: it states what must be true, and leaves *how well* to
[EVALUATION](./EVALUATION.md) and *how much* to
[AI_ARCHITECTURE §13](../01-architecture/AI_ARCHITECTURE.md#13-ai-cost-management).

**Capability is assessed per use case, never globally.** LedgerAI's AI actions differ in what they demand
([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)): a summary, a document-scoped answer,
an email draft, and a report do not stress a provider identically. "Provider X is good" is not a finding. "Provider X is
sufficient for this capability, on our content, under our constraints" is.

| Capability                   | What it means for LedgerAI                                                                                                                                                                                                                                                                                                                                   |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Structured output**        | Returns results the system can parse and validate reliably, rather than prose that must be guessed at. Structure that only usually holds is structure the system cannot depend on ([AI_ARCHITECTURE §11](../01-architecture/AI_ARCHITECTURE.md#11-ai-output-validation)).                                                                                    |
| **Plain-language output**    | Produces prose an accounting professional would accept as a starting point — the register the product speaks in ([UI_GUIDELINES](../02-design/UI_GUIDELINES.md)). Fluency is not the bar; usability by a professional is.                                                                                                                                    |
| **Grounded responses**       | Can be constrained to supplied content and will decline rather than invent when the content does not support an answer. This is the product's central promise, so it is a **threshold capability**, not a desirable one ([AI_ARCHITECTURE §9](../01-architecture/AI_ARCHITECTURE.md#9-grounding-strategy), [BR-033](../00-product/SRS.md#5-business-rules)). |
| **Long-context support**     | Accepts enough of a real financial document, with margin, to serve the action. Where context is insufficient the pipeline compensates ([AI_ARCHITECTURE §5](../01-architecture/AI_ARCHITECTURE.md#5-ai-pipeline)); a provider requiring aggressive compensation is bearing less of the load than it appears to.                                              |
| **Tool or function support** | Relevant **only if** a documented capability requires it. It is recorded here so a future need is judged rather than assumed, and so it never becomes a reason to reach around the port. No current capability depends on it.                                                                                                                                |
| **Consistent formatting**    | Comparable requests yield comparably shaped results. Inconsistent shape is paid for downstream in validation and repair, and it makes every other assessment noisier.                                                                                                                                                                                        |
| **Stable behavior**          | Behavior does not shift without notice. A provider that silently changes underneath the product converts a working system into a regression nobody committed — the failure mode observability exists to catch (§5).                                                                                                                                          |
| **Latency expectations**     | Responds within what the action can tolerate. AI operations are long-running by design and must show progress without blocking ([NFR-002](../00-product/SRS.md#9-non-functional-requirements)); this is a fitness question, not a speed contest.                                                                                                             |
| **Reliability expectations** | Available and consistent enough to serve the action, and degrades in ways the product can handle (§8). A provider that fails unpredictably is harder to accommodate than one that fails often but honestly.                                                                                                                                                  |
| **Privacy protections**      | Handles content consistently with the product's confidentiality obligations, including retention and use of what is sent. A gap here disqualifies regardless of every other strength (§10).                                                                                                                                                                  |
| **Traceability support**     | Its behavior can be attributed and observed well enough to diagnose an outcome after the fact ([AI_ARCHITECTURE §14](../01-architecture/AI_ARCHITECTURE.md#14-ai-observability)). A provider that cannot be observed cannot be governed.                                                                                                                     |

**Capability support is a claim until it is demonstrated on our content.** A published capability is a starting
hypothesis; §5's evaluation gate exists to convert it into a finding or reject it.

---

## 5. Provider Lifecycle

A provider is not chosen once and then simply present. It occupies a **stage**, and moves between stages only through
review. Naming the stages makes the difference between *trying something* and *depending on something* explicit —
because that boundary is where lock-in silently forms.

| Stage          | What it means                                                                                                                                                                                                                                                                                                                                                                                                              |
|----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Proposed**   | A candidate has been identified and a reason recorded. It carries no standing: nothing may depend on it, and it MUST NOT be reachable from product code.                                                                                                                                                                                                                                                                   |
| **Reviewed**   | Assessed against the capability model (§4) and the selection criteria (§6), with quality measured per [EVALUATION](./EVALUATION.md). Its provider-specific assumptions are documented here. The outcome is a finding, not yet permission: a candidate that fails a threshold (§6) does not proceed, and the finding is recorded so it is reconsidered only if the reason it failed has changed.                            |
| **Approved**   | Judged fit and permitted for production use, with the decision and its rationale recorded (§12). Approval is **permission, not activation** — an approved provider may sit unused indefinitely, which is exactly what a ready fallback is.                                                                                                                                                                                 |
| **Configured** | Selected for an environment by configuration (§7). Configuration MUST NOT be able to activate a provider that is not approved; that path would let an unreviewed provider reach production without a decision.                                                                                                                                                                                                             |
| **Monitored**  | Active and observed. Its behavior is watched against the expectations it was approved on ([AI_ARCHITECTURE §14](../01-architecture/AI_ARCHITECTURE.md#14-ai-observability)); a drift from those expectations is a review trigger, not a curiosity.                                                                                                                                                                         |
| **Restricted** | Still active but deliberately limited — to certain capabilities, or as fallback only — because a concern was found that does not warrant removal. Restriction is a **recorded decision with a reason**, never an informal avoidance — and it is a transition, not a resting place: it ends when the concern is resolved (returning the provider to unrestricted use) or when it is not (moving it to Replaced or Retired). |
| **Replaced**   | Superseded by another provider for a role. The replaced provider does not vanish: it remains documented so the transition is traceable, and it may remain approved as a fallback.                                                                                                                                                                                                                                          |
| **Retired**    | No longer permitted. Its configuration is removed and it MUST NOT be reachable. The record of it stays, because *why we stopped* is the most valuable thing to know when the question returns.                                                                                                                                                                                                                             |

**Experimentation versus production use.** Both are legitimate; conflating them is not. **Experimentation** is bounded,
non-production, uses no real client content ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)), and creates
no dependency — it produces a *finding*. **Production use** requires Approved status, a recorded decision, documented
assumptions, defined fallback, and observability.

A provider MUST NOT reach production by accumulation — by working well in an experiment, then a demo, then a branch,
until it is load-bearing and nobody remembers approving it. That is the specific path by which the deferred choice
becomes an accident, and the lifecycle exists to interrupt it.

---

## 6. Provider Selection Criteria

The criteria a provider is judged against. They are **weighed together, per capability** (§4) — no single criterion
elects a provider, and the first three are **thresholds**: failing any one disqualifies regardless of strength
elsewhere.

| Criterion                              | What is being judged                                                                                                                                                                                                                                                                                                                          |
|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Capability fit** *(threshold)*       | Does it actually do what this use case needs, demonstrated on our content? Grounding in particular is a threshold, because a provider that cannot decline cannot serve this product at any price.                                                                                                                                             |
| **Security fit** *(threshold)*         | Can it be used within [SECURITY](../01-architecture/SECURITY.md), including how credentials and the boundary are handled? Not negotiable against quality or cost.                                                                                                                                                                             |
| **Privacy fit** *(threshold)*          | Can confidential client content be sent under our obligations, including retention and use? A gap here ends the assessment (§10).                                                                                                                                                                                                             |
| **Operational reliability**            | Availability, consistency, and honest degradation under real conditions. Weighed as a first-class property, not a footnote — an excellent provider that is unavailable is an unavailable provider.                                                                                                                                            |
| **Portability**                        | How much of itself does adoption embed in us? A provider requiring accommodations that only it satisfies is charging a migration fee in advance, payable later.                                                                                                                                                                               |
| **Supportability**                     | When it behaves unexpectedly, can we find out why and get it addressed? Diagnosability is what separates an incident from an outage of unknown duration.                                                                                                                                                                                      |
| **Maintainability**                    | What does keeping it working cost over time — churn in its interface, its behavior, its terms? A provider that changes underneath us spends our attention indefinitely.                                                                                                                                                                       |
| **Cost awareness**                     | Cost is weighed, never decisive alone: the cheapest provider that fails a threshold costs infinitely more than the alternative. The levers and the cost stance are [AI_ARCHITECTURE §13](../01-architecture/AI_ARCHITECTURE.md#13-ai-cost-management)'s; this document only requires that cost be *weighed and recorded*, not that it decide. |
| **Graceful degradation**               | How well does it fail? A provider that fails clearly and promptly is more usable than one that fails ambiguously or hangs (§8).                                                                                                                                                                                                               |
| **Compatibility with the abstraction** | Does it fit the port as it exists, or does it demand the port bend to its shape? A provider that requires the seam to be reshaped for it is reshaping it against every other provider.                                                                                                                                                        |

**Selection is contextual and MAY differ by capability.** Nothing requires one provider to serve every AI action; the
architecture explicitly permits capability-specific selection
([AI_ARCHITECTURE §7](../01-architecture/AI_ARCHITECTURE.md#7-model-strategy)). Each such choice is a decision in its
own right, reviewed and recorded on its own merits — and each additional distinct provider is a real cost in surface and
governance, so difference must be **justified, not merely permitted** (§9).

---

## 7. Provider Configuration Strategy

**Configuration is what makes the choice reversible in practice rather than in principle.** A provider that requires a
code change to swap is hard-coded in effect, whatever the abstraction claims. This section states the *strategy*; the
mechanism is [AI_ARCHITECTURE §6](../01-architecture/AI_ARCHITECTURE.md#6-provider-architecture) and secrets are
[SECURITY §13](../01-architecture/SECURITY.md#13-secrets-management)'s. No variables, keys, or syntax appear here.

- **Selection is configuration, not code.** The active provider is chosen by configuration at deployment time. Product
  code neither names a provider nor branches on which one is active — a conditional on provider identity is lock-in
  wearing a flag's clothing.
- **Configuration selects only from the approved.** The set of activatable providers is the set of **Approved** ones
  (§5). Configuration is the switch, never the decision; if configuration alone can introduce a provider, the review
  gate is advisory.
- **There is an explicit default.** Every environment resolves to a defined provider deliberately, not by falling
  through to whatever happens to be present. Implicit defaults are how an experiment becomes production.
- **Fallback is configured, not improvised.** Where fallback exists it is declared in advance, from approved providers,
  with the conditions that trigger it (§8). Fallback invented during an incident is untested at the worst moment.
- **Environments are separate but parity-preserving.** Environments MAY differ in provider, and the difference is
  deliberate and recorded. But the further a lower environment drifts from production's provider, the less it verifies —
  a difference is a **known reduction in confidence**, not a free convenience.
- **Switching is safe by construction.** Because the domain depends only on the port, switching an approved provider is
  a configuration change with no product redesign. This is the property the abstraction exists to buy, and it is only
  real if it is exercised.
- **Rollout is deliberate and reversible.** A provider change is introduced so that its effect is observable, and it can
  be undone — a provider swap changes product behavior in ways types cannot catch and tests only partly can.

---

## 8. Provider Failure Handling

**The failure model is owned by [AI_ARCHITECTURE §12](../01-architecture/AI_ARCHITECTURE.md#12-ai-failure-handling)
and the error taxonomy by [SRS §8](../00-product/SRS.md#8-error-handling); what the user is told is
[UI_GUIDELINES](../02-design/UI_GUIDELINES.md)'s.** This section owns only the **provider-governance** dimension: what
provider failure means for selection, fallback, and review.

| Failure                   | The governance expectation                                                                                                                                                                                                                                                        |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Transient failure**     | Expected. Retries are **bounded** ([AI_ARCHITECTURE §11](../01-architecture/AI_ARCHITECTURE.md#11-ai-output-validation)) so a failing provider cannot multiply cost or delay. Persistent "transient" failure is a reliability signal and a review trigger.                        |
| **Unavailable provider**  | Surfaced as a failure with a way forward, or served by a declared fallback (§7). It is never disguised as a poor answer, and the AI request reaches a defined failed state ([§4](../01-architecture/AI_ARCHITECTURE.md#4-ai-request-lifecycle)).                                  |
| **Malformed output**      | Output failing validation is a **failure, not a result**. It is never repaired into something plausible and presented as grounded — the shortest path from a provider defect to a fabricated answer, which the product forbids ([BR-033](../00-product/SRS.md#5-business-rules)). |
| **Timeout-like failure**  | Bounded and treated as failure rather than an indefinite wait. Unbounded waiting is indistinguishable from a hang, and blocks the interface the product promises never to block ([NFR-002](../00-product/SRS.md#9-non-functional-requirements)).                                  |
| **Degraded service**      | The hardest case: the provider responds, but worse. It will not announce itself, so it is caught by observation against approved expectations (§5) — degradation that only users notice is a governance failure, not a provider one.                                              |
| **Fallback behavior**     | Fallback is **explicit, pre-approved, and to an approved provider** — never a silent substitution. A user MUST NOT be quietly served by a different provider than the system believes is active; that is provider drift, and it makes every later diagnosis unreliable.           |
| **User-visible behavior** | Failure is honest, actionable, and free of provider or internal detail ([SECURITY](../01-architecture/SECURITY.md)). The user is never told *which vendor* failed — that is our internal concern and disclosing it leaks our architecture.                                        |

**No silent substitution, no hidden drift.** These are one rule seen from two sides. Substitution the system does not
record, and drift nobody notices, both produce the same end state: **nobody can say what produced a given output**. For
a product whose promise is that AI output is grounded, traceable, and checkable, that is the failure beneath all the
others — every safeguard upstream assumes we know what answered.

---

## 9. Multi-Provider Strategy

Multi-provider support exists for **resilience and portability**. It is not a feature, not a selling point, and not an
invitation to accumulate providers.

- **A primary provider serves each capability.** One provider is the deliberate, recorded choice for a given capability
  — the default state is *one*, and it is the simplest thing that works.
- **Fallback providers exist to be ready, not to be used.** A fallback is **Approved but idle** (§5), declared in
  advance with its trigger conditions (§7). Its value is that it can be activated without a decision being made under
  pressure — a fallback first considered during an incident is not a fallback.
- **Diversification is justified per capability, never assumed.** Because the port permits capability-specific selection
  ([AI_ARCHITECTURE §7](../01-architecture/AI_ARCHITECTURE.md#7-model-strategy)), different providers *may* serve
  different capabilities. Each one MUST earn that on its own merits (§6).

**Why restraint is the rule.** Every additional provider is another adapter to maintain, another set of assumptions to
document, another behavior to observe, another failure mode, another security and privacy surface — permanently. The
architecture makes adding one *possible*; that is precisely why the discipline must live in review rather than in
difficulty. **Multi-provider capability is insurance, and insurance is worth exactly what it costs to maintain.** Two
providers kept genuinely ready are worth more than four half-maintained, because a fallback that has not been exercised
is a hypothesis.

---

## 10. Provider Security and Privacy

**Owned upstream:** [SECURITY §10](../01-architecture/SECURITY.md#10-ai-security) sets the AI security controls,
[§13](../01-architecture/SECURITY.md#13-secrets-management)
secrets, [§16](../01-architecture/SECURITY.md#16-logging-and-audit)
logging, and [AI_ARCHITECTURE §15](../01-architecture/AI_ARCHITECTURE.md#15-ai-data-privacy) the AI privacy posture.
This section restates none of them. It owns one thing: **these are conditions of candidacy** (§6), assessed before a
provider is judged on anything else.

| Expectation                        | What it means when assessing a provider                                                                                                                                                                                                                                   |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Confidential document handling** | The content crossing this boundary is a professional's confidential client financials. A provider unable to hold that content under our obligations fails candidacy — no strength elsewhere compensates.                                                                  |
| **Prompt and response privacy**    | The prompt and the response are as sensitive as the document, because the prompt *contains* it. A provider's treatment of both is assessed, not assumed.                                                                                                                  |
| **Data minimization**              | Only what the action requires crosses the boundary ([NFR-018](../00-product/SRS.md#9-non-functional-requirements)). Minimization is decided by the task, never by what is convenient to send.                                                                             |
| **Access control expectations**    | Providers receive content **per request** and hold **no standing access** to user data. A provider requiring durable access to our data is not compatible with this product.                                                                                              |
| **Tenant isolation assumptions**   | AI context is scoped to the owning user's documents; no cross-user content ever shares a request ([BR-004](../00-product/SRS.md#5-business-rules)). Per-user isolation is the product's existential promise and does not weaken at this boundary.                         |
| **Retention expectations**         | **No favourable retention behavior is assumed.** What a provider retains, for how long, and what it may use it for is established — not inferred, and not hoped for. An unverified retention posture is an unacceptable one.                                              |
| **Logging boundaries**             | Prompts and responses are not logged in ways the security posture forbids ([SECURITY §16](../01-architecture/SECURITY.md#16-logging-and-audit)). Observability (§4) MUST be achieved within that boundary — the need to diagnose never licenses recording client content. |

**These are thresholds, not trade-offs.** They are listed first in assessment for a reason: a provider that fails one is
disqualified, and the cheaper, faster, or better-quality alternative that cannot meet them is not a temptation to weigh.
It is simply not a candidate.

---

## 11. Provider Review Checklist

Every provider — new, changed, restricted, or retired — is evaluated against this checklist before the decision is
accepted. A "no" is a finding to resolve, not a detail to defer.

- [ ] **Provider abstraction respected?** — Reached only through the port; no second path around it (§3).
- [ ] **No product-code leakage?** — No provider SDK, type, vocabulary, or identity conditional appears in product code
  (Rules).
- [ ] **Capability fit verified?** — Demonstrated on our content for each capability it will serve, not assumed from
  published claims (§4).
- [ ] **Security fit verified?** — Assessed against [SECURITY §10](../01-architecture/SECURITY.md#10-ai-security);
  credentials handled per [§13](../01-architecture/SECURITY.md#13-secrets-management) (§10).
- [ ] **Privacy fit verified?** — Confidentiality, minimization, isolation, and retention established rather than
  assumed (§10).
- [ ] **Fallback behavior documented?** — What happens when it fails, and which approved provider takes over, decided in
  advance (§8).
- [ ] **Observability considered?** — Its behavior can be observed against the expectations it was approved on, within
  the logging boundary (§5, §10).
- [ ] **No lock-in assumptions?** — Provider-specific assumptions are documented, and none has become a product
  requirement (§6).
- [ ] **Traceability complete?** — The decision, its rationale, its stage, and its alternatives are recorded (§5, §12).

---

## Provider Review Process

> *Unnumbered governance section. It defines when a provider decision is reviewed and how provider governance evolves —
> deliberately, not by accident.*

**Review triggers** — a provider review is required when any of the following occurs:

- **A new provider** is proposed.
- **A provider change** — a different provider is proposed for a capability, or an active one changes stage (§5).
- **A provider fallback change** — fallback is added, removed, or its trigger conditions change.
- **A new AI capability** is introduced — it MUST be assessed against the capability model rather than assumed served.
- **A security or privacy change** — in our posture or in a provider's, including retention or handling terms.
- **A performance or reliability concern** — observed behavior drifts from what the provider was approved on.
- **A provider retirement** is proposed.

**Review outcomes** — each review resolves to exactly one:

- **Approved** — the provider is fit for the proposed stage and capability; the decision and rationale are recorded.
- **Refinement required** — the intent is sound but something must change first: an undocumented assumption, an
  unstated fallback, missing observability.
- **Security review required** — a security or privacy question is unresolved; it is routed to the security review
  process ([SECURITY](../01-architecture/SECURITY.md#ai-changes--review-required-for)). This outcome **blocks**: a
  threshold cannot be waived by another reviewer.
- **Architecture review required** — the proposal implies a change to the port, the pipeline, or a ratified decision; it
  is raised per [CLAUDE.md §8](../../CLAUDE.md) before proceeding.
- **Evaluation required** — the claim is plausible but undemonstrated; it is routed to [EVALUATION](./EVALUATION.md) and
  returns as a finding. **A capability is never approved on a published claim alone.**
- **ADR required** — the decision is significant, precedent-setting, or hard to reverse (adopting the first provider and
  resolving [DD-002](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) is exactly this). It is recorded as an ADR
  alongside [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md), which the choice operates
  inside and does not replace.

**Provider governance is continuous.** A provider is not decided once. It is judged, watched, and re-judged: approval is
a statement about what was true at review, and providers change underneath products silently. The stages (§5) exist so
that "still appropriate?" has somewhere to be asked and answered.

**Synchronization:** this governance MUST remain synchronized with
[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md), [SECURITY](../01-architecture/SECURITY.md), and the relevant
[ADRs](../01-architecture/decisions/). When this document and one of them disagree, **they win** and this document is
corrected ([CLAUDE.md §3](../../CLAUDE.md)). As [PROMPTS](./PROMPTS.md) and [EVALUATION](./EVALUATION.md) are authored,
provider governance stays in step with them: a provider change that would require forking prompts, or that invalidates
an evaluation baseline, is a change to those documents too and is reviewed with them.

---

## 12. AI Provider Decision Summary

The load-bearing decisions behind provider governance, recorded so they are not silently reversed. The **decision to
abstract** is [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md)'s and is not restated here;
these are the governance decisions built on top of it.

| Decision                                  | Chosen Approach                                                                                       | Alternatives                                                                                       | Rationale                                                                                                                                                                                                                                                                                               |
|-------------------------------------------|-------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Vendor neutrality**                     | No vendor is named or assumed anywhere in the product or its documents until a provider is approved   | Name the likely provider now and design around it; stay neutral in docs but assume one in practice | Naming a provider before deciding one makes the decision by default; [DD-002](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) is deferred deliberately, and a deferral that leaks an assumption is not a deferral ([PD-010](../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions)). |
| **Capability-based provider choice**      | Providers are judged on demonstrated capability for a specific use case, on our content               | Choose on reputation, benchmarks, popularity, or a single global "best" provider                   | Reputation and benchmarks describe someone else's workload; our content is scanned financial documents with grounding as a threshold. A provider good in general may be unfit here, and the reverse (§4, §6).                                                                                           |
| **Threshold criteria**                    | Capability, security, and privacy fit are thresholds; failing one disqualifies regardless of strength | Weigh every criterion together so a strong provider can offset a weak area                         | Confidentiality is the product's existential promise and grounding its central one. Anything offsettable is eventually offset — under deadline, by someone reasonable, for a good reason (§6, §10).                                                                                                     |
| **Configuration-driven selection**        | The active provider is chosen by configuration, from approved providers only                          | Compile the choice in and change it by release; let configuration activate any provider            | A choice requiring a release is hard-coded in effect; a configuration that can activate an unreviewed provider makes review advisory. Together these keep the choice reversible *and* governed (§7).                                                                                                    |
| **Staged provider lifecycle**             | A provider occupies a recorded stage and moves only through review                                    | Adopt providers ad hoc; treat "in use" as the only state                                           | Lock-in accumulates through experiment → demo → branch → load-bearing, with no decision anywhere. Naming stages puts a gate on the one transition that matters: trying something becoming depending on it (§5).                                                                                         |
| **Additive provider expansion**           | Providers are added as adapters behind the existing port, never by reshaping it for one               | Bend the port to accommodate a desirable provider                                                  | A port shaped around one provider is that provider's interface with a different name, and it silently disqualifies the next candidate. Additive expansion is what [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) requires (§3, §9).                                         |
| **Explicit fallback, never substitution** | Fallback is pre-approved and declared; substitution is never silent and drift is never hidden         | Fail over automatically to whatever is available; retry elsewhere quietly to preserve the illusion | If the system cannot say what produced an output, grounding and traceability are unverifiable and every upstream safeguard rests on an assumption. Honest failure is cheaper than an unattributable answer (§8, [BR-033](../00-product/SRS.md#5-business-rules)).                                       |
| **Replacement without redesign**          | Swapping an approved provider is a configuration change requiring no product change, and is exercised | Accept that swapping needs rework, and treat portability as a design intention                     | Portability decays unless maintained; an abstraction never exercised is a claim. Keeping the swap genuinely cheap is what makes the deferred decision reversible rather than nominally reversible (§7, §9).                                                                                             |

---

*This document governs how LedgerAI selects, evaluates, and swaps AI providers; it does not override the frozen
documents under [`docs/`](../). It operates inside
[ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) and
[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md), respects
[SECURITY](../01-architecture/SECURITY.md), consumes findings from [EVALUATION](./EVALUATION.md), stays neutral to
[PROMPTS](./PROMPTS.md), and anticipates [RAG](./RAG.md) without depending on it. When a provider decision is required,
review it through the process above and, when a change would imply new product behavior or alter a ratified decision,
stop and raise it per [CLAUDE.md §8](../../CLAUDE.md).*
