# ADR-003 — AI Provider Abstraction

**Status:** Accepted (pattern **and** provider)
**Date:** 2026-07-21 *(pattern accepted 2026-07-14; provider resolved here)*
**Owner:** Founding Engineer / Principal AI Architect
**Related Documents:
** [PRODUCT_DECISIONS PD-010 / DD-002](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions) · [AI_ARCHITECTURE §6](../AI_ARCHITECTURE.md#6-provider-architecture) · [AI_ARCHITECTURE §7](../AI_ARCHITECTURE.md#7-model-strategy) · [AI_ARCHITECTURE §8](../AI_ARCHITECTURE.md#8-prompt-architecture) · [AI_ARCHITECTURE §9](../AI_ARCHITECTURE.md#9-grounding-strategy) · [AI_ARCHITECTURE §11](../AI_ARCHITECTURE.md#11-ai-output-validation) · [AI_ARCHITECTURE §15](../AI_ARCHITECTURE.md#15-ai-data-privacy) · [ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services) · [SECURITY §10](../SECURITY.md#10-ai-security) · [SECURITY §13](../SECURITY.md#13-secrets-management) · [SRS §5](../../00-product/SRS.md#5-business-rules) · [SRS §8](../../00-product/SRS.md#8-error-handling) · [DATABASE §5](../DATABASE.md#5-entity-specifications) · [API_SPEC §10](../API_SPEC.md#10-ai-summary-module) · [ADR-009 (OCR Strategy)](./ADR-009-OCR-Strategy.md) · [ADR-002 (Storage Provider)](./ADR-002-Storage-Provider.md)

---

## Context

AI capabilities (summary, chat, email, report) are core to LedgerAI, but the concrete AI/LLM provider was a **deferred**
decision ([DD-002](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)) and the market moves quickly. Binding
business logic to a specific vendor SDK would make the provider choice effectively irreversible and leak vendor concepts
into the domain. We need to be able to adopt, swap, or add providers without touching business logic.

**This ADR was originally split:** the *abstraction* (a domain-owned AI port with config-selected adapters) was
**Accepted** and realizes [PD-010](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions); the *concrete
LLM provider* was **Deferred** ([DD-002](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)), to be resolved
before AI Summary implementation (Milestone 4). This revision **resolves the provider** so the AI Summary slice is no
longer blocked. It **selects a provider only** — the AI port, the deterministic pipeline, the prompt architecture, the
grounding strategy, the model-selection seam, and every security control are unchanged.

The requirements the provider must satisfy are already fixed by the documentation and are, for the most part,
**provider-independent** (the application, not the provider, enforces them):

- **Serve the MVP generative capabilities** — summary, chat, email, report — over the extracted text of a single Ready
  document ([AI Capability Map](../AI_ARCHITECTURE.md#3-ai-capability-map),
  [BR-035](../../00-product/SRS.md#5-business-rules)). The documented output is **editable natural-language text**
  persisted as `AIOutput` ([DATABASE §5](../DATABASE.md#5-entity-specifications)); it is **not** embeddings, vector
  indexes, tool calls, or agent state — those are explicit
  [future](../AI_ARCHITECTURE.md#16-future-ai-evolution) items
  ([DD-003/DD-004](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)), so any provider capability beyond
  grounded text generation would be unused complexity (CLAUDE.md — keep the simplest correct solution).
- **Follow instructions well enough to stay grounded and honest.** Outputs MUST be faithful to the provided text and
  MUST decline ("not found") rather than fabricate when the text does not support an answer
  ([BR-030](../../00-product/SRS.md#5-business-rules), [BR-033](../../00-product/SRS.md#5-business-rules),
  [Grounding Strategy](../AI_ARCHITECTURE.md#9-grounding-strategy)). This is the paramount AI-quality requirement.
- **Respect channel separation / resist prompt injection.** The provider must honor a distinct system channel over
  untrusted user and document channels so injected text cannot override system intent
  ([Prompt Architecture](../AI_ARCHITECTURE.md#8-prompt-architecture),
  [SECURITY §10](../SECURITY.md#10-ai-security), T-05).
- **Produce predictable, validatable shapes** (structured summary, email, report) so
  [output validation](../AI_ARCHITECTURE.md#11-ai-output-validation) can enforce them.
- **Reached only through the domain-owned AI port** so the choice stays reversible
  ([ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services)); the provider SDK/HTTP contract is confined to the
  adapter, and no provider type crosses the port.
- **Server-side, least-privilege credentials**, minimum-necessary content sent, no standing provider access, no content
  logged ([SECURITY §10](../SECURITY.md#10-ai-security), [SECURITY §13](../SECURITY.md#13-secrets-management),
  [NFR-018](../../00-product/SRS.md#9-non-functional-requirements),
  [AI Data Privacy](../AI_ARCHITECTURE.md#15-ai-data-privacy)).
- **Deployment-independent** — backend on Render, DB on Neon ([ADR-002](./ADR-002-Storage-Provider.md) context); the
  provider must be reachable over HTTPS without coupling to a specific cloud.
- **Cost-controllable** — a capable default model with a cheaper option available for simpler tasks, chosen behind the
  port ([Model Strategy](../AI_ARCHITECTURE.md#7-model-strategy),
  [AI Cost Management](../AI_ARCHITECTURE.md#13-ai-cost-management)).

**Candidate set.** The repository intentionally names no LLM vendor (AI_ARCHITECTURE is provider-neutral). The smallest
reasonable set of production-grade hosted LLM APIs that meet the documented requirement — grounded natural-language
generation with a strong system/user channel split, structured-output support, a large context window, a mature REST API
and an official Java SDK confinable to the adapter, and standard-tier terms that do not train on submitted content — is:
**Anthropic (Claude)**, **OpenAI (GPT)**, and **Google (Gemini)**. These are the three mature, widely-used frontier LLM
APIs a Java/Spring MVP can integrate today. Self-hosted/open-weight models (e.g. Llama-class served locally) are
considered under Alternatives but excluded from the primary set for the low-ops, quality-on-real-documents reasons
below. The decision is **not broadened** beyond selecting the LLM provider behind the existing AI port.

---

## Decision

**1. AI abstraction — unchanged (accepted 2026-07-14).** Access all AI capabilities through a **domain-owned AI port**
(an interface expressed in domain terms), with each provider implemented as an **adapter** containing request/response
mappers. The active adapter is selected by **configuration**. Business logic depends only on the port; no domain code
imports a provider SDK. This realizes [PD-010](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions) and
the ports-and-adapters stance of [ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services) and
[AI_ARCHITECTURE §6](../AI_ARCHITECTURE.md#6-provider-architecture).

**2. LLM provider — adopt Anthropic (Claude)** as the production AI provider implementing the AI adapter behind the AI
port.

The determining factor is **fit to the paramount documented AI requirement — grounded, non-fabricating, honest-"unknown"
generation with a strong system-instruction boundary — at the lowest integration and lock-in cost.** LedgerAI's
existential AI promise is trustworthy output that stays faithful to the document and declines rather than invents
([BR-030](../../00-product/SRS.md#5-business-rules), [BR-033](../../00-product/SRS.md#5-business-rules), the
["Grounded over Generative"](../AI_ARCHITECTURE.md#ai-design-principles) and "never fabricate certainty" design rules).
Anthropic's models are purpose-built for exactly this posture — high adherence to a system instruction such as *"answer
only from the provided document; if it is not supported, say so"*, and a strong data-vs-instruction boundary that helps
the document/user channels remain **data**, not commands ([SECURITY §10](../SECURITY.md#10-ai-security) prompt
injection). It also provides everything the other candidates provide and the requirements actually ask for: a large
context window that comfortably holds a single document's extracted text, structured/JSON output for
[validatable shapes](../AI_ARCHITECTURE.md#11-ai-output-validation), a mature REST API and an official Java SDK
confinable to the adapter, standard-tier terms that do **not** train on submitted content, and cloud-independent HTTPS
access from Render. OpenAI and Gemini are equally capable in the broad sense but do not win on the criterion that
matters most for *this* product, and Gemini's one genuine differentiator (a standing free tier) is unusable for
confidential financial documents (see Evaluation).

**Concrete model selection remains an implementation-time choice behind the port**, per
[Model Strategy §7](../AI_ARCHITECTURE.md#7-model-strategy): the MVP MAY use a single capable default Claude model, with
a cheaper Claude model available for simpler capabilities. This ADR introduces **no** model-routing decision — dynamic
per-request routing stays a documented [future](../AI_ARCHITECTURE.md#16-future-ai-evolution) item. It selects the
**provider** only; the AI port, pipeline, prompt architecture, grounding, validation, schema, API contract, and security
controls all remain as accepted.

---

## Evaluation

Providers assessed only against requirements already present in the repository. No new product requirements are
introduced. ("Structured output" is read as the documented need — predictable, validatable **natural-language** shapes
for `AIOutput` — not embeddings, tool calls, or agents, which the schema and boundaries exclude.)

| Criterion                                              | Source                                                                                                                                                           | Anthropic (Claude)                                                                   | OpenAI (GPT)                                                   | Google (Gemini)                                                                          |
|--------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|----------------------------------------------------------------|------------------------------------------------------------------------------------------|
| **Grounded, non-fabricating output**                   | [BR-030](../../00-product/SRS.md#5-business-rules), [BR-033](../../00-product/SRS.md#5-business-rules), [§9](../AI_ARCHITECTURE.md#9-grounding-strategy)         | **Strong** — high adherence to "answer only from the source; decline if unsupported" | Strong                                                         | Strong                                                                                   |
| **System/instruction boundary (injection resistance)** | [§8](../AI_ARCHITECTURE.md#8-prompt-architecture), [SECURITY §10](../SECURITY.md#10-ai-security) T-05                                                            | **Strong** — robust system-channel adherence; document text stays *data*             | Strong                                                         | Strong                                                                                   |
| **Structured / validatable output**                    | [§11](../AI_ARCHITECTURE.md#11-ai-output-validation)                                                                                                             | Structured/JSON output modes                                                         | Structured/JSON output modes                                   | Structured/JSON output modes                                                             |
| **Context window (single document)**                   | [§7](../AI_ARCHITECTURE.md#7-model-strategy)                                                                                                                     | Large — ample for one document's text with margin                                    | Large                                                          | Large (largest headline windows)                                                         |
| **Capability-specific model options**                  | [§7](../AI_ARCHITECTURE.md#7-model-strategy), [§13](../AI_ARCHITECTURE.md#13-ai-cost-management)                                                                 | Capable default + cheaper small model                                                | Capable default + cheaper small model                          | Capable default + cheaper small model                                                    |
| **REST API + Java SDK (adapter-confinable)**           | [§6](../AI_ARCHITECTURE.md#6-provider-architecture), [ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services)                                                 | Mature REST + official Java SDK                                                      | Mature REST + official Java SDK                                | Mature REST + official Java SDK                                                          |
| **Deployment fit (Render/Neon)**                       | [ADR-002](./ADR-002-Storage-Provider.md) ctx                                                                                                                     | HTTPS API, no cloud coupling                                                         | HTTPS API, no cloud coupling                                   | HTTPS via AI-Studio API; **Vertex path couples to GCP** (avoided)                        |
| **Privacy — no training on API data**                  | [NFR-018](../../00-product/SRS.md#9-non-functional-requirements), [SECURITY §10](../SECURITY.md#10-ai-security), [§15](../AI_ARCHITECTURE.md#15-ai-data-privacy) | Standard API tier does not train on submitted content (verify at integration)        | Standard API tier does not train on submitted content (verify) | **Paid tier** does not train; **free tier may use prompts** — unsuitable for client data |
| **Free-tier / low-ops fit**                            | DD-002, free-tier goal                                                                                                                                           | Paid API; low-cost small model; no ops                                               | Paid API; low-cost small model; no ops                         | Standing free tier — **but its data-use terms disqualify it for confidential documents** |
| **Provider lock-in**                                   | [PD-010](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions)                                                                                     | Low — behind the AI port; plain-text `AIOutput` is portable                          | Low behind port                                                | Low behind port                                                                          |
| **Server-side least-privilege creds**                  | [SECURITY §13](../SECURITY.md#13-secrets-management)                                                                                                             | Single server-held API key, externalized/rotatable                                   | Same                                                           | Same                                                                                     |
| **Failure/retry mapping**                              | [SRS §8](../../00-product/SRS.md#8-error-handling), [§12](../AI_ARCHITECTURE.md#12-ai-failure-handling)                                                          | Stateless request; errors map to domain taxonomy; bounded retry                      | Same                                                           | Same                                                                                     |
| **Auditability (no content logs)**                     | [§14](../AI_ARCHITECTURE.md#14-ai-observability), [NFR-013](../../00-product/SRS.md#9-non-functional-requirements)                                               | `AIRequest` lifecycle + metadata only                                                | Same                                                           | Same                                                                                     |

**Reading.** On the criteria where the three differ, the choice turns on the one that matters most for *this* product.
The prior provider ADRs ([ADR-002](./ADR-002-Storage-Provider.md), [ADR-009](./ADR-009-OCR-Strategy.md)) were decided
largely on **free-tier / low-ops fit** — but here that lever is neutralized: for confidential financial documents the
only privacy-acceptable posture is a standard/paid tier that does not train on submitted content, which all three offer
and which makes Gemini's standing free tier unusable (its free-tier prompts may be used to improve products —
incompatible with [NFR-018](../../00-product/SRS.md#9-non-functional-requirements) and "no standing access"). With cost,
context, structured output, SDK maturity, and deployment independence effectively equivalent on the paid tier, the
decisive documented criterion becomes **grounded, non-fabricating, injection-resistant generation** — the product's
existential AI-quality promise — on which Anthropic (Claude) is the strongest fit. No documented criterion favours
OpenAI or Gemini for this workload; **any of the three would satisfy the architecture behind the port**, which keeps the
choice low-risk and reversible.

---

## Security Review

Required because the AI provider receives document-derived content
([SECURITY Review Process](../SECURITY.md#security-review-process), [SECURITY §10](../SECURITY.md#10-ai-security),
[AI Review Process](../AI_ARCHITECTURE.md#ai-review-process)). Each concern is marked **provider capability** or
**application responsibility** (the adapter/service built in the AI slices). Most controls are application behaviour and
are provider-independent.

| Concern                                     | How it is satisfied                                                                                                                                                                                                                                                               | Owner                                                                   |
|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| **Confidentiality**                         | Document-derived text is confidential; sent to the provider over TLS solely to perform the requested action, held only server-side, never returned to the browser except as the reviewed `AIOutput`                                                                               | Provider (secure processing) + application (server-side only)           |
| **Data minimization**                       | Only the minimum content the action needs is sent — the relevant extracted text plus non-sensitive metadata; no unrelated user/client/account data ([NFR-018](../../00-product/SRS.md#9-non-functional-requirements), [§15](../AI_ARCHITECTURE.md#15-ai-data-privacy))            | Application (prompt builder sends minimal content)                      |
| **No standing provider access**             | Content crosses to the provider **per request** only; the provider holds no standing access to user data ([§15](../AI_ARCHITECTURE.md#15-ai-data-privacy))                                                                                                                        | Application (per-request calls) + provider (stateless request)          |
| **Provider training / customer-data usage** | The Anthropic **standard API tier does not use submitted content to train models**, per Anthropic's data-usage terms; this MUST be re-verified against current provider terms at integration time                                                                                 | Provider (documented terms) — **verify at integration**                 |
| **Encryption in transit**                   | HTTPS/TLS to the provider endpoint ([SECURITY §12](../SECURITY.md#12-data-security))                                                                                                                                                                                              | Provider (TLS) + application (enforce HTTPS)                            |
| **Prompt injection**                        | System, user, and document channels are kept distinct; document/user text is treated as **data, not instructions** ([§8](../AI_ARCHITECTURE.md#8-prompt-architecture)); the chosen model's strong system-boundary adherence reinforces this                                       | Application (channel separation) + provider (instruction adherence)     |
| **Context / ownership isolation**           | Each AI request is scoped to a single document the caller owns; ownership authorized via the existing `OwnershipGuard`/published client check; non-owned ⇒ `404` ([BR-004](../../00-product/SRS.md#5-business-rules), [SECURITY §5](../SECURITY.md#5-authorization))              | Application                                                             |
| **Grounding / no fabrication**              | Outputs grounded in the document; unsupported answers decline rather than invent ([BR-030/033](../../00-product/SRS.md#5-business-rules)); reinforced by [output validation](../AI_ARCHITECTURE.md#11-ai-output-validation)                                                       | Application (grounding + validation) + provider (instruction adherence) |
| **Output validation**                       | Empty/malformed/ungrounded/unsafe output is rejected or bounded-retried before persist ([§11](../AI_ARCHITECTURE.md#11-ai-output-validation)); never shown as a fabricated success                                                                                                | Application                                                             |
| **Credential management / least privilege** | A single server-held API key supplied via environment/config, never committed, rotatable without code change, never exposed to the browser or another provider ([SECURITY §13](../SECURITY.md#13-secrets-management))                                                             | Application                                                             |
| **No content logging**                      | Prompts, responses, and extracted text are never written to operational logs; only `AIRequest` lifecycle/metadata is recorded ([§14](../AI_ARCHITECTURE.md#14-ai-observability), [NFR-013](../../00-product/SRS.md#9-non-functional-requirements))                                | Application                                                             |
| **Failure handling**                        | Provider unavailability/timeout/rate-limit maps to a domain failure; the `AIRequest` transitions to `FAILED` with a clear, retryable reason, never a fabricated success ([§12](../AI_ARCHITECTURE.md#12-ai-failure-handling), [SRS §8](../../00-product/SRS.md#8-error-handling)) | Application (adapter → domain exception; service → state transition)    |

**Conclusion.** The provider must supply four things — TLS transport, strong system-instruction adherence, structured
output, and no-training on submitted content — all of which Anthropic (Claude) provides (the last **must be re-verified
against current Anthropic terms at integration time**, and is the one provider-dependent item). Every other control —
data minimization, ownership isolation, channel separation, output validation, credential handling, no-content-logging,
failure mapping — is application behaviour implemented identically for any provider, so the choice weakens no documented
control. Because provider capabilities and application responsibilities are cleanly separated behind the port, swapping
the provider later would not reopen any of these application controls.

---

## Alternatives Considered

**Abstraction (unchanged):**

- **Domain-owned AI port + config-selected adapters — chosen (2026-07-14).** Provider independence; reversible choice;
  domain stays clean and testable.
- **Direct provider SDK calls in services — rejected.** Vendor lock-in, irreversible choice, vendor types polluting the
  domain, violation of the [Guiding Architectural Rules](../ARCHITECTURE.md#guiding-architectural-rules).
- **A heavyweight multi-provider gateway/framework — rejected for MVP.** Over-engineering; adds a dependency and
  complexity the MVP does not need. The thin port/adapter gives independence without the weight.

**LLM provider:**

- **Anthropic (Claude) — chosen.** Strongest fit to the paramount documented requirement (grounded, non-fabricating,
  injection-resistant generation with a strong system boundary), while matching the others on context, structured
  output, SDK/REST maturity, privacy posture, and deployment independence; low lock-in behind the port; plain-text
  `AIOutput` is portable.
- **OpenAI (GPT) — rejected.** Equally capable frontier LLM and a valid adapter, but it does not win on the
  grounding/steerability criterion that matters most for a professional, defensibility-first product; no documented
  criterion favours it over Claude here. It remains a fully viable **fallback/second adapter** behind the port.
- **Google (Gemini) — rejected.** Strong models with the largest headline context windows, but its one genuine
  differentiator — a **standing free tier** — is disqualified for confidential financial documents because free-tier
  prompts may be used to improve the provider's products
  ([NFR-018](../../00-product/SRS.md#9-non-functional-requirements), "no standing access"). On the paid tier it offers
  no decisive documented advantage over Claude, and its lowest-friction path (Vertex AI) would couple deployment to GCP.
  Remains a valid alternative adapter if requirements change.
- **Self-hosted / open-weight model — rejected for MVP.** No per-call cost, but it must be hosted, tuned, secured, and
  operated (against the low-ops, free/low-cost goal), and typically trails frontier hosted models on grounded, honest
  generation over messy real-world financial text. Remains a valid **future** adapter behind the port if data-residency,
  cost, or on-prem needs ever demand local inference ([Local models](../AI_ARCHITECTURE.md#16-future-ai-evolution)).

---

## Consequences

### Advantages

- The provider decision (DD-002) is now **made and unblocked**: the AI Summary slice can implement the AI port's
  adapter, and the remaining generative capabilities (chat, email, report) follow behind the same seam.
- The provider chosen is the **strongest fit to the product's core AI-quality promise** — grounded, non-fabricating,
  injection-resistant output — which is where trust in a professional accounting tool is won or lost.
- **Provider-independent and low lock-in**: business logic depends only on the AI port; the plain-text `AIOutput` shape
  is portable to any LLM provider, so the choice stays genuinely reversible.
- **No new infrastructure and no scope creep**: a single hosted API reached over HTTPS from the existing Render/Neon
  stack; no embeddings, vector store, agents, or RAG are introduced (all remain documented future items).
- Enables **capability-specific model selection** (a cheaper model for simpler tasks) behind the same port, per
  [§7](../AI_ARCHITECTURE.md#7-model-strategy)/[§13](../AI_ARCHITECTURE.md#13-ai-cost-management), without a routing
  decision now.

### Disadvantages

- The MVP uses a **paid API** (no standing free tier that is privacy-acceptable for client data); cost is controlled by
  the [minimization levers](../AI_ARCHITECTURE.md#13-ai-cost-management) and capability-specific model choice, not by a
  free tier.
- One provider's terms (no-training on submitted content) are a **provider-dependent** assumption that must be
  re-verified at integration and on any provider change.
- The port must remain well-shaped enough to fit an alternative provider's request/response and error semantics.

### Trade-offs

- We accept a paid-tier provider in exchange for the **best fit to grounded, honest, injection-resistant generation, a
  privacy-acceptable data posture, mature SDK/REST integration, and low lock-in** — the properties the requirements
  actually ask for. The free-tier lever that decided the storage and OCR ADRs is deliberately set aside because it
  conflicts with confidentiality here.

### Migration implications if replaced later

Because business logic depends only on the **AI port** ([ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services),
[AI_ARCHITECTURE §6](../AI_ARCHITECTURE.md#6-provider-architecture)), replacing Anthropic means **adding/selecting
another AI adapter and choosing it by configuration — no change to services, controllers, rules, prompt architecture,
schema, or API**. No data migration is needed: persisted `AIOutput` records remain valid, and any output can be
**regenerated** from the still-grounded `DocumentContent` if desired. A provider switch is effectively a re-run behind
the port, not a migration. A fallback/second provider is just another adapter, consistent with the
[graceful-degradation](../AI_ARCHITECTURE.md#12-ai-failure-handling) posture.

### Interaction with the AI port and AI architecture

The port stays exactly as [AI_ARCHITECTURE §6](../AI_ARCHITECTURE.md#6-provider-architecture) defined it: a domain-owned
interface expressed in domain terms ("summarize this text", "answer this question about this text"). The Anthropic
adapter implements the port; its request mapper translates the centrally assembled, channel-separated prompt
([§8](../AI_ARCHITECTURE.md#8-prompt-architecture)) into the provider request, and its response mapper translates the
provider response and errors back into domain terms, mapping provider errors into the domain error taxonomy
([SRS §8](../../00-product/SRS.md#8-error-handling)). **No provider type crosses the port.** The deterministic pipeline,
grounding, output validation, observability, and privacy controls are all unchanged — this ADR fills the one open slot
(which vendor sits behind the adapter) and nothing else.

---

## Future Reconsideration

The **abstraction** remains a durable decision. The **provider** should be revisited if grounded-output quality proves
insufficient on real accounting documents (benchmark before any swap,
[AI Evaluation Strategy](../AI_ARCHITECTURE.md#ai-evaluation-strategy)), if cost at scale outgrows the minimization
levers, if a fallback/second provider is warranted for availability, or if data-residency / customer-managed-key /
on-prem needs arise (which could motivate a self-hosted adapter) — each added **additively behind the port**, with an
[AI architecture review](../AI_ARCHITECTURE.md#ai-review-process) and, for a provider change, a new/updated ADR. The
provider's no-training and data-handling terms MUST be re-verified at integration time and on any provider change.
Richer contracts for future capabilities (RAG, tool calling, agents) are extended **additively** on the port, not
retrofitted into the domain.

---

## References

[PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md) · [AI_ARCHITECTURE](../AI_ARCHITECTURE.md) · [ARCHITECTURE](../ARCHITECTURE.md) · [SECURITY](../SECURITY.md) · [SRS](../../00-product/SRS.md) · [DATABASE](../DATABASE.md) · [API_SPEC](../API_SPEC.md) · [ADR-009](./ADR-009-OCR-Strategy.md) · [ADR-002](./ADR-002-Storage-Provider.md)
