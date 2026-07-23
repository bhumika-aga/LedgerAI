# RAG — Retrieval Governance — LedgerAI MVP

> **Status:** Draft v1 — **governs a deferred capability.** Retrieval does not exist in the MVP
> ([DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)). This document is **dormant**: it binds nothing
> today and takes effect only if and when retrieval is approved (§1, *Applicability*).
> **Owner:** Principal Retrieval Architect
> **Last updated:** 2026-07-15
> **Upstream (frozen):
> ** [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) · [ARCHITECTURE](../01-architecture/ARCHITECTURE.md) · [SECURITY](../01-architecture/SECURITY.md) · [SRS](../00-product/SRS.md) · [DATABASE](../01-architecture/DATABASE.md) · [ADR-014](../01-architecture/decisions/ADR-014-Search-Strategy.md)
> **Related:
> ** [AI_PROVIDERS](./AI_PROVIDERS.md) · [PROMPTS](./PROMPTS.md) · [EVALUATION](./EVALUATION.md) · [CLAUDE.md](../../CLAUDE.md)

---

## 1. Purpose

### Why this document exists

Retrieval decides what the model is allowed to see. Everything downstream — the grounding, the answer, the
professional's decision — is conditioned on a selection made before the model was ever called, by logic nobody
experiences as a product decision.

The governing principle of this document:

> **Retrieval is a product decision wearing infrastructure clothing.**
>
> What is retrieved becomes what the professional is told. A relevance heuristic nobody reviewed can change every answer
> the product gives — and when it does, it will look like the model's fault.

**Retrieval is deferred, and that is precisely why this document is written now.**
[DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) postpones the RAG strategy; it is coupled to
[DD-002](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) (provider) and
[DD-003](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) (vector database), and its decision point is **during
AI Chat design**. That means retrieval, if it arrives, arrives *inside a feature milestone*, under delivery pressure, at
exactly the moment nobody has time to invent governance for it. A deferral is only survivable if the rules that will
constrain the decision exist before the decision does. This document is those rules, waiting.

**Where retrieval would live is already settled** and this document does not re-decide it:
[ARCHITECTURE §15](../01-architecture/ARCHITECTURE.md#15-future-evolution) places RAG **inside the AI module, behind the
existing AI port**, with callers unaffected;
[AI_ARCHITECTURE §16](../01-architecture/AI_ARCHITECTURE.md#16-future-ai-evolution) lists it as a future path;
[AI_ARCHITECTURE §8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture) already defines the channels retrieved
context would enter through; [ADR-014](../01-architecture/decisions/ADR-014-Search-Strategy.md) owns search strategy and
defers semantic search; [DATABASE §13](../01-architecture/DATABASE.md#13-future-database-evolution) owns the additive
schema path. What no document owns is **what may be retrieved, how a selection is judged, and how a retrieval change is
reviewed**. This document owns that, and only that.

It is **not** a prompt document, **not** provider governance, **not** an evaluation document, **not** a search design,
and **not** implementation. It contains **no retrieval code, no query syntax, no chunk sizes, no token counts, no
embedding or storage engine details, no scoring or ranking formulas, no dataset schemas, no prompt text, and no provider
or model names**.

### Applicability — this document is dormant

**No retrieval exists in LedgerAI today, and none is planned by this document.** Every MVP AI capability operates on a
single owned document ([BR-030](../00-product/SRS.md#5-business-rules),
[BR-035](../00-product/SRS.md#5-business-rules)); nothing is selected, because there is nothing to select from.

Until [DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) is resolved, exactly two things here bind:

1. **The boundary in §3** — what the MVP already calls *document retrieval* is **not** retrieval in this document's
   sense, and this document MUST NOT be read as governing it.
2. **Introducing retrieval is an ADR, not a task.** Resolving DD-004 is significant, precedent-setting, and hard to
   reverse; it is raised per [CLAUDE.md §8](../../CLAUDE.md) and recorded as a decision
   ([ADR-014](../01-architecture/decisions/ADR-014-Search-Strategy.md) anticipates it). This document governs what
   happens *after* that decision — it does not make it, pre-empt it, or imply it.

Everything below is written in the conditional it deserves: **if retrieval is approved, this is how it is governed.**

### Relationship to the AI documents

| Document                                                    | Its job                                     | The boundary with this document                                                                                                                                                                                                                                                                                                                                                                                                                             |
|-------------------------------------------------------------|---------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [AI_ARCHITECTURE.md](../01-architecture/AI_ARCHITECTURE.md) | **AI behavior and the orchestration model** | It owns the **pipeline** ([§5](../01-architecture/AI_ARCHITECTURE.md#5-ai-pipeline)), the **channels** retrieved context enters through ([§8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture)), and **grounding** ([§9](../01-architecture/AI_ARCHITECTURE.md#9-grounding-strategy)). This document owns the **discipline of selecting** what fills them; it adds no channel and no pipeline stage.                                            |
| [ARCHITECTURE.md](../01-architecture/ARCHITECTURE.md)       | **System design**                           | It places retrieval **inside the AI module behind the existing AI port** ([§15](../01-architecture/ARCHITECTURE.md#15-future-evolution)) — callers unaffected. This document operates inside that placement and MUST NOT relocate retrieval, expose it through a new boundary, or make it a caller's concern.                                                                                                                                               |
| [AI_PROVIDERS.md](./AI_PROVIDERS.md)                        | **Provider selection and governance**       | Retrieval is provider-neutral. A provider that behaves only with a particular retrieval shape is a **provider-specific assumption** recorded there ([§4](./AI_PROVIDERS.md#4-provider-capability-model)) — never a reason to shape retrieval around a provider. The vector-store question is [DD-003](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)'s, not this document's.                                                                      |
| [PROMPTS.md](./PROMPTS.md)                                  | **Prompt design, structure, and lifecycle** | It owns **what may reach a prompt** and what must not ([§7](./PROMPTS.md#7-prompt-variables-and-context)). This document owns **what may be retrieved as a source** — an earlier and separate filter. Retrieved context is subject to PROMPTS §7 on entry, and retrieval **never relies on that filter to catch what it should not have selected** (§4).                                                                                                    |
| [EVALUATION.md](./EVALUATION.md)                            | **How AI quality is measured and judged**   | Retrieval changes what context reaches a capability, which changes what a result means. A retrieval change is therefore a thing **evaluated** ([§4](./EVALUATION.md#4-evaluation-scope)) and a reason a baseline is re-established ([§7](./EVALUATION.md#7-baselines-and-regression)). This document produces no verdict and defines no criterion of quality.                                                                                               |
| [SECURITY.md](../01-architecture/SECURITY.md)               | **The security posture**                    | It owns per-user isolation ([§5](../01-architecture/SECURITY.md#5-authorization)) and AI security, including that retrieved document content is **untrusted input** ([§10](../01-architecture/SECURITY.md#10-ai-security)). This document owns only the **retrieval consequence**: selection happens within those controls, never around them.                                                                                                              |
| **The ADRs**                                                | **Ratified decisions**                      | [ADR-014](../01-architecture/decisions/ADR-014-Search-Strategy.md) owns search strategy and defers semantic search; [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) established provider independence; [ADR-010](../01-architecture/decisions/ADR-010-AI-Request-Lifecycle.md) the request lifecycle. This document operates **inside** them; a retrieval need that would alter one is an ADR ([CLAUDE.md §8](../../CLAUDE.md)). |

In one line each:

> **AI_ARCHITECTURE defines the AI behavior and orchestration model. AI_PROVIDERS defines provider selection and
> governance. PROMPTS defines prompt design, structure, lifecycle, and review. EVALUATION defines how AI quality is
> measured and judged. RAG defines how retrieved context is selected, governed, and incorporated when retrieval
> exists.**

### Relationship to the frozen product documents

This document introduces **no product behavior and no capability**. Retrieval serves capabilities the frozen documents
already grant ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)); it never creates one.

Two frozen limits bound anything retrieval could ever do here, and neither is this document's to relax:

- **[BR-004](../00-product/SRS.md#5-business-rules)** — a user accesses only their own material. Retrieval does not
  weaken per-user isolation; there is no retrieval scope in which another user's content is a candidate.
- **[BR-035](../00-product/SRS.md#5-business-rules)** — V1 operates on a **single document; no multi-document
  reasoning** ([PRD §5](../00-product/PRD.md#5-non-goals)). Cross-document retrieval is therefore **not available to be
  designed** under the current rules: it would require BR-035 to change first, in [SRS](../00-product/SRS.md), raised
  per [CLAUDE.md §8](../../CLAUDE.md) — never enabled as a retrieval improvement.

A retrieval capability that needs either rule to bend is not a retrieval decision. It is a product decision, and it goes
where product decisions go.

---

## 2. RAG Philosophy

These principles explain *why* retrieval governance is shaped the way it is. They are the reasoning behind the
enforceable rules that follow.

| Principle                                             | Why it exists                                                                                                                                                                                                                                                                                                                                  |
|-------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Retrieval only when needed**                        | Retrieval is deferred because the MVP does not need it ([DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)), not because it was overlooked. It is *expected* of AI products, which is exactly why it gets adopted as an assumption rather than chosen as an answer to a real problem.                                           |
| **Grounded context over convenience**                 | The output must derive from the source ([BR-030](../00-product/SRS.md#5-business-rules)). Context that is easy to fetch is not thereby the context the answer should rest on — and the model cannot tell the difference.                                                                                                                       |
| **Minimum necessary context**                         | Everything retrieved crosses an untrusted boundary and is paid for in privacy exposure ([NFR-018](../00-product/SRS.md#9-non-functional-requirements), [AI_ARCHITECTURE §15](../01-architecture/AI_ARCHITECTURE.md#15-ai-data-privacy)). Retrieval is the layer most tempted to send more, because more feels safer and costs nothing visible. |
| **Traceable sources**                                 | Grounding is only checkable if a claim can be walked back to what it came from ([BR-033](../00-product/SRS.md#5-business-rules)). Untraceable context makes the product's central promise unfalsifiable — the answer looks grounded and nobody can confirm it (§8).                                                                            |
| **Relevance over volume**                             | More context dilutes attention across irrelevance and widens the space for invention. Volume is the easiest thing to increase and the easiest to mistake for thoroughness; it degrades quality in the direction that looks like effort.                                                                                                        |
| **Deterministic retrieval behavior**                  | The pipeline around the model is **deterministic by design** — retrieval is named there explicitly ([AI Design Principles](../01-architecture/AI_ARCHITECTURE.md#ai-design-principles)). The model is the probabilistic part; if selection is also unpredictable, no result is reproducible and no regression is attributable.                 |
| **Separation of retrieval and prompting**             | Retrieval *selects*; the prompt *composes* ([PROMPTS §3](./PROMPTS.md#3-prompt-architecture)). Fused, a relevance tweak becomes a silent prompt change reviewed by nobody who reviews prompts — and the reverse.                                                                                                                               |
| **Security by design**                                | Retrieved content is untrusted input ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)). Retrieval widens what reaches the model, so it widens the injection surface; that is a property of the design, decided when selection is designed, never patched afterward.                                                              |
| **Context supports the task, it does not replace it** | Retrieval supplies material; it does not answer. Treating retrieved text as the answer moves product behavior into selection logic, where no rule is written and no reviewer looks ([BR-032](../00-product/SRS.md#5-business-rules)).                                                                                                          |
| **Retrieval must remain reviewable**                  | A relevance change has no visible signature: no contract breaks, no test fails, and every individual answer still looks reasonable. If it is not reviewable, it is not governed — and it changes every answer at once.                                                                                                                         |

---

## RAG Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Retrieval MUST serve a documented AI capability.** *A retrieval with no capability behind it is undocumented product
  behavior ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)). The product does what its
  documents grant, and a retrieval design is not a grant.*
- **Retrieval MUST remain separate from prompt design.** *They are reviewed by different processes for different
  risks ([PROMPTS](./PROMPTS.md#prompt-review-process)). Fused, a change to either escapes the review of the other, and
  the escape is invisible.*
- **Retrieved context MUST be traceable to an approved source.** *A claim that cannot be walked back to its origin
  cannot be verified, corrected, or withdrawn — and in the output it is indistinguishable from one that was
  invented ([BR-030](../00-product/SRS.md#5-business-rules), §8).*
- **Retrieval SHOULD minimize unnecessary context.** *Minimization is a security
  obligation ([NFR-018](../00-product/SRS.md#9-non-functional-requirements)), not tidiness. Retrieval is where "just
  include it, it might help" is most persuasive and least examined.*
- **Retrieval MUST respect security and confidentiality constraints.** *Per-user
  isolation ([BR-004](../00-product/SRS.md#5-business-rules), [SECURITY §5](../01-architecture/SECURITY.md#5-authorization))
  does not weaken because a candidate scored well. Relevance is never an authorization.*
- **Retrieval MUST NOT become a hidden source of product behavior.** *A rule enforced by what selection happens to
  return is a rule with no home in [SRS](../00-product/SRS.md) — unfindable, untestable, and changed by whoever tunes
  relevance next.*
- **Retrieval changes MUST be reviewed before production use.** *A relevance change alters every answer at once, with no
  signature to break and no test that fully substitutes for judgment. Review is the only gate it has (Retrieval Review
  Process).*
- **Retrieval SHOULD preserve determinism where possible.** *The orchestration around the model is deterministic by
  design ([AI Design Principles](../01-architecture/AI_ARCHITECTURE.md#ai-design-principles)). Non-deterministic
  selection makes a result unreproducible and a regression unattributable — the model is already the probabilistic
  variable, and one is enough.*
- **Retrieval MUST NOT silently change the meaning of prompts.** *The same prompt over different context is a different
  instruction. A retrieval change that alters what a capability effectively asks is a behavior change wearing an
  infrastructure ticket ([PROMPTS §7](./PROMPTS.md#7-prompt-variables-and-context)).*
- **Retrieval decisions MUST remain traceable.** *A selection rule whose reason is unrecorded cannot be changed safely —
  so it is never changed, only added to, until nobody can say why anything is
  retrieved ([ADR-015](../01-architecture/decisions/ADR-015-Observability.md)).*

**Why these rules exist.** Retrieval fails quietly and at scale. The three failure modes are **context drift** (what
gets selected shifts as content, tuning, and sources evolve, until a capability is answering from material nobody
chose), **hidden behavior change** (product behavior migrating into selection logic, where it is enforced by ranking
rather than stated in a rule), and **leakage of unreviewed information** (material reaching the model because it was
reachable, not because it was approved).

Each is invisible from inside. Nobody experiences context drift as a regression; they experience individually plausible
answers. Nobody notices behavior living in relevance logic; they notice that changing the ranking changed the product.
And leakage is discovered by the person it leaks to. These rules exist because retrieval is the one layer where a small,
well-intentioned tuning change is indistinguishable from a change to what the product says.

---

## 3. Retrieval Model

**First, the boundary that matters most.** The MVP pipeline already contains a step called **document retrieval**
([AI_ARCHITECTURE §4](../01-architecture/AI_ARCHITECTURE.md#4-ai-request-lifecycle),
[§5](../01-architecture/AI_ARCHITECTURE.md#5-ai-pipeline)): loading the owning user's **Ready** document and its
extracted content ([BR-010](../00-product/SRS.md#5-business-rules)). **That is not retrieval in this document's sense,
and this document does not govern it.**

The distinction is **selection**:

|                       | **Direct context loading** (MVP, exists today)                                        | **Retrieval** (deferred, governed here)                                                        |
|-----------------------|---------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| **What it does**      | Loads *the* document the request names                                                | *Chooses* context from a set of candidates                                                     |
| **Judgment involved** | None — the document is given by the request                                           | Relevance, ranking, and inclusion are decided                                                  |
| **Owned by**          | [AI_ARCHITECTURE §4–§5](../01-architecture/AI_ARCHITECTURE.md#4-ai-request-lifecycle) | This document, if approved ([DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)) |

**Where there is no choice, there is no retrieval.** Governance attaches to the act of *selecting*, because selection is
where judgment enters the pipeline unannounced. Loading a named document is deterministic and needs no relevance
opinion; nothing in this document applies to it.

**What a retrieval is here.** For governance purposes a retrieval is not a query but a **reviewed selection policy**: a
documented decision about which sources are eligible, how candidates are chosen among them, and what is assembled from
the result — serving one documented capability, owned by someone, and reviewable as a whole.

**The conceptual parts.** These are the decisions a retrieval design is made of. They are named here so a reviewer can
ask about each; two of them are owned in depth by later sections and are not elaborated twice.

| Part                     | What it decides                                                                                                                                                                                                                                                               |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Source selection**     | Which sources are **eligible** at all (§4). This is an authorization and approval question before it is a relevance question — an ineligible source is not a low-scoring candidate, it is not a candidate.                                                                    |
| **Query formation**      | What the retrieval is actually asking for, derived from the task and the user's intent. The user's words are **untrusted input** ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)); using them to form a query never means letting them widen its scope.        |
| **Candidate selection**  | Which eligible material is considered. Its honesty depends on the candidate set being what the capability's grounding assumes — a candidate set quietly broadened is a grounding change nobody reviewed.                                                                      |
| **Ranking**              | The order of preference among candidates, and therefore what survives when not everything can be included. Ranking is where relevance becomes an opinion the product asserts; the opinion is reviewed, not merely its inputs (§6).                                            |
| **Filtering**            | What is removed before assembly — for authorization, for approval status, for relevance. Filtering is a **control**, not an optimization: authorization filtering is not a stage that may be reordered or skipped for cost ([BR-004](../00-product/SRS.md#5-business-rules)). |
| **Context assembly**     | Turning what survived into the context a capability receives. Owned in depth by **§7** — named here because it is where selection stops and construction begins.                                                                                                              |
| **Source attribution**   | Preserving what each piece of context came from, through every transformation above. Owned in depth by **§8** — named here because attribution is decided *during* retrieval or not at all.                                                                                   |
| **Retrieval boundaries** | The limits the whole policy operates inside: the owning user, the granted capability, the approved sources, the frozen rules (§1). Boundaries are not the last filter applied; they are the shape of the candidate set from the start.                                        |

**How retrieved context enters the model.** Through the **existing channels** —
[AI_ARCHITECTURE §8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture) already defines **Document Context**
for grounded source material and **Retrieved Metadata** for non-sensitive interpretive context. This document **MUST NOT
redefine, rename, merge, or add a channel**, and retrieval does not acquire one by growing. Channel separation is a
security control; retrieval that needs its own channel to work is an architecture change, raised per
[CLAUDE.md §8](../../CLAUDE.md).

---

## 4. Retrieval Scope

**What may be retrieved**, if retrieval is approved — every item bounded by the owning user
([BR-004](../00-product/SRS.md#5-business-rules)) and by the single-document rule while it stands
([BR-035](../00-product/SRS.md#5-business-rules), §1):

| Source                          | The discipline                                                                                                                                                                                                                                                                |
|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **The user's own documents**    | Eligible only as the frozen rules allow. Under BR-035, "the user's documents" is not a candidate set for reasoning — the single document the request names is. Widening this is a product change, never a retrieval improvement (§1).                                         |
| **Client-associated documents** | Eligible on the same terms and by the same isolation. Association with a client is an organizing fact, **not** a grant of cross-document reasoning over that client's material.                                                                                               |
| **Extracted document content**  | The grounded substance answers derive from ([BR-030](../00-product/SRS.md#5-business-rules)), available only for a **Ready** document ([BR-010](../00-product/SRS.md#5-business-rules)). Content from a document that is not ready is not a weak candidate; it is ineligible. |
| **Metadata**                    | Non-sensitive interpretive context — the kind [AI_ARCHITECTURE §8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture)'s **Retrieved Metadata** channel exists for. Admitted where it improves the output, never as a way to send more while calling it small.       |
| **Approved reference material** | Material a capability legitimately needs that is not the user's content. "Approved" is the operative word: approval is a **recorded decision** with an owner, not a property something acquires by being present and useful.                                                  |
| **Approved internal context**   | Where a capability demonstrably requires it. Same standard, and a higher bar: internal context that shapes output is product behavior, and product behavior belongs in [SRS](../00-product/SRS.md) before it belongs in a retrieval policy (Rules).                           |

**What is outside scope.** Each of these is excluded **at retrieval**, not filtered later:

- **Another user's content, ever** — there is no scoring in which it is a candidate
  ([BR-004](../00-product/SRS.md#5-business-rules), [SECURITY §5](../01-architecture/SECURITY.md#5-authorization)).
- **Unapproved material** — approval precedes eligibility. Material retrieved because it was reachable is the leak this
  document exists to prevent.
- **Deleted material** — a deleted document MUST NOT be retrievable through *any* path
  ([BR-012](../00-product/SRS.md#5-business-rules)); retrieval is a path.
- **Secrets, credentials, and configuration** — never context, at any layer
  ([SECURITY §13](../01-architecture/SECURITY.md#13-secrets-management)).
- **Internal system detail and reasoning about our own controls** — not needed for any granted task, and it can surface
  in output.
- **Prompt text and prompt internals** — prompts are composed, not retrieved
  ([PROMPTS §3](./PROMPTS.md#3-prompt-architecture)). A prompt assembled from retrieved fragments is a prompt outside
  prompt review.
- **Provider internals** — retrieval is provider-neutral; provider behavior is [AI_PROVIDERS](./AI_PROVIDERS.md)'s and
  never a source.

> **Retrieval MUST NOT rely on the prompt layer to filter what it should not have selected.**
> [PROMPTS §7](./PROMPTS.md#7-prompt-variables-and-context) governs what may reach a prompt, and it is a real control —
> but it is the **second** one. A source excluded at retrieval can never leak; a source retrieved and then filtered is
> one refactor, one new capability, or one missed review away from leaking. Two filters are defense in depth only while
> each is complete on its own; the moment either is designed to depend on the other, there is one filter with a gap in
> it.

---

## 5. Retrieval Lifecycle

A retrieval has stages because the difference between *trying a relevance idea* and *changing what every answer is based
on* is invisible without them.

| Stage          | What it means                                                                                                                                                                                                                                                                                         |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Proposed**   | A need is identified and the capability it serves named ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)), together with why existing context is insufficient. "It would probably help" is not a need; it is an expectation borrowed from other products.             |
| **Approved**   | Judged fit against the criteria (§6) and the checklist (§10), with sources approved and the decision recorded. Approval attaches to a **specific policy** — approving retrieval for a capability is never approving its future tuning.                                                                |
| **Configured** | Sources, boundaries, and assembly are set as approved, externally rather than in feature code ([SECURITY §13](../01-architecture/SECURITY.md#13-secrets-management)). Configuration that can silently exceed what was approved is not configuration; it is an unreviewed change surface.              |
| **Executed**   | Retrieval runs for a granted capability, inside the AI module behind the port ([ARCHITECTURE §15](../01-architecture/ARCHITECTURE.md#15-future-evolution)). Only an approved policy may run; if any other path can retrieve, approval is advisory.                                                    |
| **Observed**   | Watched against what it was approved on ([AI_ARCHITECTURE §14](../01-architecture/AI_ARCHITECTURE.md#14-ai-observability)) — what gets selected, what comes back empty, what is filtered. Retrieval that cannot be observed cannot be shown to have drifted, and drift is its characteristic failure. |
| **Reviewed**   | Assessed when a trigger fires (Retrieval Review Process), with quality judged per [EVALUATION](./EVALUATION.md) where the change warrants it. The outcome is a **finding**, not permission.                                                                                                           |
| **Revised**    | Changed through review, producing a **new approved policy** rather than a quiet adjustment to the running one. A revision that silently replaces its predecessor destroys the attribution that made the prior result meaningful.                                                                      |
| **Retired**    | No longer used. It MUST NOT remain reachable, and its record stays: *why we stopped retrieving that way* is the most valuable thing to know when it is proposed again — and retrieval ideas are proposed again.                                                                                       |

**Every stage has an exit.** *Proposed* ends when the need is granted or declined. *Reviewed* resolves to exactly one
outcome (Retrieval Review Process) and never rests: a policy held for another review is not approved by the passage of
time. A retrieval that cannot leave a stage is raised, not left there.

**Experimentation versus production retrieval.** Both are legitimate; conflating them is not. **Experimentation** is
bounded, non-production, uses no real client content ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security),
[NFR-018](../00-product/SRS.md#9-non-functional-requirements)), and creates no dependency — it produces a *finding*.
**Production retrieval** requires an approved policy, approved sources, a named owner, observability, and an evaluation
where the change warrants one.

Retrieval MUST NOT reach production by accumulation — a source added for an experiment, kept because it helped, carried
forward because removing it might hurt. That is how a candidate set nobody approved becomes the basis of every answer,
one reasonable step at a time.

---

## 6. Retrieval Selection Criteria

§3 names the parts a retrieval is made of. This section governs **how a retrieval choice is judged** — the questions a
reviewer asks of a proposed policy. It defines no formula and no threshold: thresholds are contextual, and a number
frozen here would be obeyed long after it stopped being right.

| Criterion             | What it means                                                                                                                                                                                                                                                                                    |
|-----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Relevance**         | Whether what is selected is what the task actually needs. Judged against the task, never against how much came back — a policy that returns more is easier to build and harder to fault, which is why it wins by default unless someone checks.                                                  |
| **Traceability**      | Whether every piece of retrieved context can be walked back to an approved source, through assembly (§7, §8). A policy that cannot answer "where did this come from?" cannot support grounding at all.                                                                                           |
| **Grounding support** | Whether the selection makes the capability's grounding promise ([BR-030](../00-product/SRS.md#5-business-rules)) more *checkable*, not merely more likely to be met. Context that happens to contain the answer is not the same as context the answer is grounded in.                            |
| **Source quality**    | Whether the material is fit to base professional output on. Retrieval inherits the authority of what it selects: material that is stale, provisional, or wrong does not become sound by being relevant.                                                                                          |
| **Security fit**      | Whether the policy operates inside the controls rather than around them ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)) — and whether it widens the injection surface, since everything retrieved is untrusted input.                                                            |
| **Privacy fit**       | Whether it sends only what the task requires ([NFR-018](../00-product/SRS.md#9-non-functional-requirements), [AI_ARCHITECTURE §15](../01-architecture/AI_ARCHITECTURE.md#15-ai-data-privacy)). Exposure is the sum of everything a policy *can* select, not the average of what it usually does. |
| **Consistency**       | Whether comparable requests select comparably. Inconsistent selection makes every downstream result unreproducible and every regression unattributable (Rules).                                                                                                                                  |
| **Maintainability**   | Whether the policy can be understood and changed by someone who did not write it. Retrieval logic that works for unarticulated reasons is preserved rather than maintained — and it is never removed, only added to.                                                                             |
| **Task fit**          | Whether *this* capability needs *this* retrieval. Capabilities differ ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)); a policy shared because it was convenient serves the capability it was designed for and quietly fails the others.                       |
| **User value**        | Whether the professional's output is genuinely better. Retrieval that improves a measure while producing output that is harder to trust or verify has spent the product's confidentiality budget to make the answer worse ([BR-032](../00-product/SRS.md#5-business-rules)).                     |

**No criterion overrides a security or isolation finding.** These are not weights to balance against relevance
([BR-004](../00-product/SRS.md#5-business-rules)); a concern there routes to security review and **blocks** (Retrieval
Review Process).

---

## 7. Chunking and Context Assembly

Assembly is where retrieval stops selecting and starts **constructing what the model will treat as the truth**. It is
the least visible part of retrieval and the most capable of changing meaning, because nothing here looks like a decision
about content — it looks like plumbing.

| Concern                    | The discipline                                                                                                                                                                                                                                                                    |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Source boundaries**      | Where one source ends and another begins MUST survive assembly. Material from two sources presented as continuous is a fabrication the *retrieval* performed — the model did not invent the join, and it will reason across it as though it were real.                            |
| **Chunk boundaries**       | Where source material is divided. A division is a claim that the parts are separately meaningful; made carelessly it is the first place meaning is lost, and the loss is silent because each part still reads well.                                                               |
| **Preserving meaning**     | A piece of content must mean, in isolation, what it meant in place. A figure severed from its qualification, a total from its period, a clause from its condition — each is individually accurate and collectively misleading, which is the worst thing this product can produce. |
| **Avoiding fragmentation** | Context split so finely that no piece carries enough to be understood. The model receives many true fragments and no grounds to relate them, so it relates them itself — which is invention performed on request.                                                                 |
| **Avoiding duplication**   | The same material arriving twice. Repetition reads as corroboration to a model and as emphasis to a reader; it manufactures confidence that nothing in the source supports.                                                                                                       |
| **Context ordering**       | Order is interpreted whether or not it was meant to be. What comes first is weighted; ordering by convenience asserts a priority nobody chose, and it will be consistent enough to look intentional.                                                                              |
| **Limiting noise**         | Everything included that the task does not need dilutes what it does, and widens the space in which invention is plausible. Noise is not neutral filler; it is the material an ungrounded answer gets built from.                                                                 |

**Why assembly matters.** Grounding is a promise about the relationship between output and source
([BR-030](../00-product/SRS.md#5-business-rules)). Assembly *is* that relationship: by the time the model is called, the
"source" is whatever assembly constructed, and the model has no access to what was cut, split, reordered, or repeated.
An output can be perfectly faithful to assembled context and completely wrong about the document — and every grounding
check will pass, because the check compares the answer to the context, and the context is what went wrong.

Assembly is therefore reviewed as **content**, not as implementation detail. Its numbers — sizes, limits, orderings —
are set where implementation decisions are recorded, never here; what this document requires is that they are decisions
someone made, stated, and can be asked about.

---

## 8. Citations and Attribution

**Attribution is decided during retrieval or not at all.** Provenance that was not carried through selection, filtering,
and assembly cannot be reconstructed afterward — by then the context is text, and where each part came from is a
question the system can no longer answer.

| Concern                                             | The discipline                                                                                                                                                                                                                                                                  |
|-----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Source references**                               | Every piece of retrieved context carries what it came from. Not for display — for **verifiability**: it is what makes grounding a checkable claim rather than an assurance ([BR-030](../00-product/SRS.md#5-business-rules)).                                                   |
| **Traceable provenance**                            | The reference survives ranking, filtering, chunking, and assembly (§7). Provenance is lost at transformations, which is exactly where nobody is watching for it.                                                                                                                |
| **Citation support**                                | Retrieval's job is to make citation **possible**. Whether a capability cites, and how that reads, is the capability's and the interface's ([PROMPTS](./PROMPTS.md), [UI_GUIDELINES](../02-design/UI_GUIDELINES.md)). This document defines no syntax, no markup, and no format. |
| **Source visibility in output**                     | Where a capability shows sources, they are shown because the professional needs to **verify** — the standard is *can this be checked*, not *does this look substantiated*. An unverifiable citation is worse than none: it borrows trust it cannot repay.                       |
| **Preserving traceability through transformations** | When context is summarized, merged, or reshaped before use, attribution follows or the transformation is not permitted. A transformation that loses provenance has converted grounded material into unattributable text — the thing retrieval exists to avoid producing.        |

**Attribution is part of trust, not a feature of it.** LedgerAI assists a professional who stakes their name on the
output ([BR-032](../00-product/SRS.md#5-business-rules)); their obligation is to verify, and verification requires
knowing what a claim rests on. An answer that cannot be traced is not a slightly worse answer — it is one the
professional cannot responsibly use, however correct it happens to be. Retrieval either preserves that capacity or
quietly removes it.

---

## 9. Retrieval Failure Handling

Retrieval failures are **normal operating conditions**, not exceptions. How they are handled decides whether the product
degrades honestly or fabricates confidently — and the honest path is always the one that has to be decided in advance,
because in the moment there is always an answer available if nobody insisted otherwise.

| Failure                         | What it means, and how it is handled                                                                                                                                                                                                                                                                                                                                                               |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **No relevant source found**    | **Not a failure — a finding.** The supported response is to decline or qualify ([BR-033](../00-product/SRS.md#5-business-rules), [AI_ARCHITECTURE §10](../01-architecture/AI_ARCHITECTURE.md#10-hallucination-mitigation)). Empty retrieval MUST NOT become permission to proceed unsupported; "found nothing" and "here is an answer anyway" are the same event only when nobody chose otherwise. |
| **Source unavailable**          | A real failure, surfaced as one ([AI_ARCHITECTURE §12](../01-architecture/AI_ARCHITECTURE.md#12-ai-failure-handling), [NFR-005](../00-product/SRS.md#9-non-functional-requirements)). Unavailable is not empty: proceeding as though nothing existed converts an outage into a confident, wrong answer.                                                                                            |
| **Partial retrieval**           | Some of what was expected is present. The response is grounded in what was actually retrieved and says so where it matters; degraded is legitimate, **silently** degraded is not ([NFR-004](../00-product/SRS.md#9-non-functional-requirements)). The professional cannot compensate for a gap nobody told them about.                                                                             |
| **Stale retrieval**             | Material no longer reflects its source. Dangerous precisely because it is *available and plausible* — it fails no check and produces an answer that was true once, which is the hardest kind to catch and the easiest to act on.                                                                                                                                                                   |
| **Conflicting sources**         | Sources disagree. The conflict is surfaced or the request declines; retrieval MUST NOT silently pick a winner. Choosing between contradictory professional material is exactly the judgment the product exists to support, not to make ([BR-032](../00-product/SRS.md#5-business-rules)).                                                                                                          |
| **Low-confidence retrieval**    | Nothing selected is clearly adequate. Treated as closer to *no relevant source* than to *good enough*: the pressure at this moment is always to proceed, and yielding to it is how a product becomes fluent and untrustworthy. Related in kind to low-confidence extraction ([FR-OCR-006](../00-product/SRS.md#46-ocr-ocr)).                                                                       |
| **Security-rejected retrieval** | Something was excluded by a control ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)). The exclusion **stands**; it is never relaxed to improve an answer, and a rejection that recurs is a review trigger, not an obstacle to route around.                                                                                                                                         |

**Failure means refusal or degradation, decided per capability.** A summary missing part of its source and a client
email missing part of its source are not equally acceptable
([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)); what each does when retrieval falls
short is decided when the retrieval is approved, and recorded — never improvised by whichever code path runs first. What
is never available is the third option: proceeding as though retrieval succeeded.

---

## 10. Retrieval Review Checklist

Every retrieval — new or revised — is assessed against this checklist before acceptance. A "no" is a finding to resolve,
not a detail to defer.

- [ ] **Capability documented?** — It serves a granted capability, named
  ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)).
- [ ] **Source policy clear?** — Every eligible source is approved, and the approval is recorded with an owner (§4).
- [ ] **Retrieval purpose clear?** — Why existing context is insufficient is stated (§5).
- [ ] **Context boundaries defined?** — Owner, capability, and frozen limits bound the candidate set from the start —
  not as a final filter (§3, §4).
- [ ] **Security fit verified?** — Operates inside the controls; no reliance on the prompt layer to catch what retrieval
  should not have selected (§4, [SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)).
- [ ] **Privacy fit verified?** — Only what the task requires; judged on what the policy *can* select, not what it
  usually does (§6, [NFR-018](../00-product/SRS.md#9-non-functional-requirements)).
- [ ] **Attribution considered?** — Provenance is carried through ranking, filtering, and assembly (§7, §8).
- [ ] **Prompt interaction reviewed?** — The prompt's meaning over this context is unchanged, or the change went through
  prompt review ([PROMPTS](./PROMPTS.md#prompt-review-process)).
- [ ] **Evaluation impact considered?** — Baselines affected are identified and re-established through review, never
  silently ([EVALUATION §7](./EVALUATION.md#7-baselines-and-regression)).
- [ ] **Safe to deploy?** — Failure behavior is decided per capability, degradation is visible, and no security finding
  is outstanding (§9).

---

## Retrieval Review Process

> *Unnumbered governance section. It defines when retrieval is reviewed and how retrieval governance evolves —
> deliberately, not by accident.*

**Review triggers** — a retrieval review is required when any of the following occurs:

- **A new retrieval use case** is proposed — including the first, which is
  [DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) itself and is an **ADR** (§1).
- **A new source type** — eligibility is an approval decision, never an extension of an existing policy (§4).
- **A retrieval change** — any change to sources, candidate set, ranking, filtering, or assembly, however small. Size is
  not a proxy for impact: a ranking tweak changes every answer at once.
- **A prompt change affecting retrieval** — a prompt whose meaning depends on what context arrives
  ([PROMPTS §7](./PROMPTS.md#7-prompt-variables-and-context)).
- **A provider change affecting retrieval** — an active or fallback provider changes
  ([AI_PROVIDERS §5](./AI_PROVIDERS.md#5-provider-lifecycle)); retrieval is re-examined for neutrality and for
  assumptions that were the old provider's.
- **An evaluation regression** — measured quality drops against what a policy was approved on
  ([EVALUATION](./EVALUATION.md#evaluation-review-process)).
- **A security concern** — anything touching isolation, injection surface, or what reaches a model
  ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)).
- **A product rule change** — a rule retrieval operates under changes in [SRS](../00-product/SRS.md); retrieval follows,
  never the reverse.
- **An architecture change** — the pipeline, the channel model, or the port changes
  ([AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md)).

**Review outcomes** — each review resolves to exactly one:

- **Approved** — fit for production as a specific policy; the decision and rationale are recorded (§5).
- **Refinement required** — the intent is sound but something must change first: an unapproved source, an undefined
  boundary, provenance lost at a transformation, unstated failure behavior.
- **Prompt review required** — the change alters what a capability effectively asks; it is routed to the
  [Prompt Review Process](./PROMPTS.md#prompt-review-process). **A retrieval change is never applied as a prompt edit
  here.**
- **Evaluation required** — the change is plausible but its effect on output is unmeasured; it is routed to
  [EVALUATION](./EVALUATION.md#evaluation-review-process) and returns as a finding. **A behavior change is never
  approved on reasoning alone.**
- **Security review required** — isolation, injection surface, or context exposure is in question; it is routed to the
  security review process ([SECURITY](../01-architecture/SECURITY.md#ai-changes--review-required-for)). This outcome
  **blocks**.
- **Provider review required** — the retrieval appears to depend on provider behavior; it is routed to the
  [Provider Review Process](./AI_PROVIDERS.md#provider-review-process). Retrieval that works only under one provider is
  a provider finding, not a retrieval design.
- **Architecture review required** — the change implies a new channel, a pipeline stage, a different placement, or a
  caller-visible boundary; it is raised per [CLAUDE.md §8](../../CLAUDE.md) before proceeding.
- **ADR required** — the decision is significant, precedent-setting, or hard to reverse. **Introducing retrieval at all
  is always this outcome** (§1).

Every outcome resolves to a lifecycle position (§5). Only **Approved** advances a policy toward *Configured*;
**Refinement required** returns it to *Proposed*; every other outcome holds it at *Reviewed* until the review it was
routed to returns. A policy held there is not approved by the passage of time.

**Synchronization.** Retrieval governance MUST remain synchronized with
[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md), [ARCHITECTURE](../01-architecture/ARCHITECTURE.md),
[AI_PROVIDERS](./AI_PROVIDERS.md), [PROMPTS](./PROMPTS.md), [EVALUATION](./EVALUATION.md),
[SECURITY](../01-architecture/SECURITY.md), and the relevant [ADRs](../01-architecture/decisions/). When this document
and one of them disagree, **they win** and this document is corrected ([CLAUDE.md §3](../../CLAUDE.md)). A change
touching more than one is reviewed by each document it touches and MUST NOT be merged into one while leaving another
contradicting it — a retrieval change that alters what a prompt effectively asks, invalidates an evaluation baseline, or
requires a provider accommodation is a change to those documents too.

---

## 11. Retrieval Decision Summary

The load-bearing decisions behind retrieval governance, recorded so they are not silently reversed. Retrieval's
**placement** ([ARCHITECTURE §15](../01-architecture/ARCHITECTURE.md#15-future-evolution)), the **channel model**
([AI_ARCHITECTURE §8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture)), the **search strategy**
([ADR-014](../01-architecture/decisions/ADR-014-Search-Strategy.md)), and the **vector-store question**
([DD-003](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)) are decided elsewhere and are not restated here.

| Decision                                       | Chosen Approach                                                                                        | Alternatives                                                                         | Rationale                                                                                                                                                                                                                                                                                                 |
|------------------------------------------------|--------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Governance written before the capability**   | Write retrieval governance while retrieval is still deferred; introducing it is an ADR                 | Write this when retrieval is built; or leave retrieval to the AI Chat milestone      | [DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) resolves *inside a feature milestone*, under delivery pressure — the moment governance is least likely to be written and most needed. A deferral is survivable only if its constraints exist before the decision does (§1).             |
| **Retrieval as a governed capability**         | A retrieval is an owned, approved, reviewable policy serving one documented capability                 | Treat retrieval as infrastructure tuning, changed like a query                       | Selection decides what the professional is told. Infrastructure tuning that changes every answer at once is a product change with no reviewer — and it will be attributed to the model (§5).                                                                                                              |
| **Selection is the boundary**                  | Governance attaches to *choosing* context; loading a named document is not retrieval                   | Govern all context loading; or govern nothing until a vector store exists            | The MVP pipeline already says "document retrieval" and means something else entirely. Without the line drawn, this document either seizes territory [AI_ARCHITECTURE §4–§5](../01-architecture/AI_ARCHITECTURE.md#4-ai-request-lifecycle) owns, or implies retrieval exists (§3).                         |
| **Traceable sources**                          | Every retrieved item carries approved provenance, preserved through every transformation               | Retrieve content and reconstruct sources later where needed                          | Provenance not carried through assembly cannot be recovered — by then it is text. Without it, grounding is an assurance rather than a checkable claim, and the professional cannot discharge their obligation to verify (§8).                                                                             |
| **Minimum necessary context**                  | Only what the task requires is eligible and selected                                                   | Retrieve generously and let the model select what matters                            | Everything retrieved crosses an untrusted boundary, dilutes attention, and widens the space for invention. Minimization is a security obligation ([NFR-018](../00-product/SRS.md#9-non-functional-requirements)), and retrieval is where "more is safer" is most persuasive (§6).                         |
| **Filter at the source, not at the prompt**    | Exclusions are enforced at retrieval; the prompt layer is a second, independent control                | Retrieve broadly and rely on prompt-layer filtering to exclude what must not be sent | Two filters are defense in depth only while each is complete alone. The moment either is designed to depend on the other, there is one filter with a gap — and the gap is discovered by whoever the content leaks to (§4).                                                                                |
| **Context assembly is content**                | Boundaries, ordering, and division are reviewed as decisions about meaning                             | Treat chunking and assembly as implementation detail                                 | By the time the model is called, assembly *is* the source. Output can be perfectly faithful to assembled context and wrong about the document — and every grounding check passes, because the check never sees what assembly cut (§7).                                                                    |
| **Retrieval governance is prompt-independent** | Retrieval and prompts are governed and reviewed separately                                             | Govern them together, since they jointly determine what the model sees               | They fail differently and are reviewed for different risks. Fused, a relevance change escapes prompt review and a prompt change escapes retrieval review — each invisible to the process that would have caught it (Rules).                                                                               |
| **Review-based change control**                | Any change to sources, ranking, filtering, or assembly is reviewed before production                   | Allow tuning within an approved policy without further review                        | "Tuning" is where behavior change hides, because it has no signature: no contract breaks, no test fails, and every answer still looks reasonable. Unreviewed relevance is unreviewed product behavior (Rules, §5).                                                                                        |
| **Frozen limits are not retrieval's to relax** | Isolation and the single-document rule bound retrieval absolutely; changing them is a product decision | Let retrieval scope expand where a capability would benefit                          | [BR-004](../00-product/SRS.md#5-business-rules) is the product's existential promise and [BR-035](../00-product/SRS.md#5-business-rules) is scope ([PRD §5](../00-product/PRD.md#5-non-goals)). A capability needing either to bend is a product change, raised per [CLAUDE.md §8](../../CLAUDE.md) (§1). |

---

*This document governs how LedgerAI's retrieved context would be selected, governed, and incorporated **if and when
retrieval is approved**; it does not override the frozen documents under [`docs/`](../), and it does not commit the
product to retrieval — [DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) remains deferred and resolving
it is an ADR. It operates inside [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) and the placement fixed by
[ARCHITECTURE §15](../01-architecture/ARCHITECTURE.md#15-future-evolution), stays neutral to the providers governed by
[AI_PROVIDERS](./AI_PROVIDERS.md), leaves prompt composition to [PROMPTS](./PROMPTS.md), submits its effects to
[EVALUATION](./EVALUATION.md), and respects [SECURITY](../01-architecture/SECURITY.md). When a retrieval decision would
imply new product behavior, cross a frozen limit, or alter a ratified decision, stop and raise it per
[CLAUDE.md §8](../../CLAUDE.md).*
