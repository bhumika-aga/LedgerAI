# Prompts — LedgerAI MVP

> **Status:** Draft v1
> **Owner:** Principal Prompt Architect
> **Last updated:** 2026-07-15
> **Upstream (frozen):
** [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) · [AI_PROVIDERS](./AI_PROVIDERS.md) · [SECURITY](../01-architecture/SECURITY.md) · [SRS](../00-product/SRS.md) · [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md)
> **Related:
** [EVALUATION](./EVALUATION.md) · [RAG](./RAG.md) · [CLAUDE.md](../../CLAUDE.md)

---

## 1. Purpose

### Why this document exists

A prompt is the only place in LedgerAI where product behavior is expressed in prose. Everything else the product does is
typed, tested, reviewed, and versioned; a prompt is a paragraph — and a paragraph edited to fix one summary can change
every summary, silently, with no compiler to object and no diff anyone thought to read.

**What prompts must achieve is already settled.**
[AI_ARCHITECTURE §8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture) owns the channel model prompts are
composed of; [§9](../01-architecture/AI_ARCHITECTURE.md#9-grounding-strategy) owns grounding;
[§10](../01-architecture/AI_ARCHITECTURE.md#10-hallucination-mitigation) owns hallucination defenses; the
[AI Design Rules](../01-architecture/AI_ARCHITECTURE.md#ai-design-rules) bind every capability to them.

What no document yet owns is **the prompt as an engineering artifact**: who owns one, how it is composed and reused, how
it is versioned, how a change reaches production, and how one is retired. This document owns that.

The governing principle of this document:

> **A prompt is an engineering artifact, not a text field.**
>
> It has an owner, a version, a documented purpose, and a review — or it is an undocumented behavior change waiting to
> happen, indistinguishable from a typo until a professional acts on its output.

It is **not** a prompt library, **not** an evaluation report, **not** a retrieval design, and **not** implementation. It
contains **no prompt text, no provider or model names, no code, no syntax, no token budgets, and no evaluation data**.
It governs *how prompts are designed, structured, versioned, reviewed, and evolved* — never what any prompt says.

### Relationship to the AI documents

| Document                                                    | Its job                                     | The boundary with this document                                                                                                                                                                                                                                                                                                                                                                  |
|-------------------------------------------------------------|---------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [AI_ARCHITECTURE.md](../01-architecture/AI_ARCHITECTURE.md) | **AI behavior and the orchestration model** | It owns the **channel model** ([§8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture)) and the behavioral obligations prompts serve — grounding, safety, validation. This document owns **the discipline of authoring within that model**: composition, reuse, versioning, review. It restates neither the channels nor the obligations; it governs the artifact that expresses them. |
| [AI_PROVIDERS.md](./AI_PROVIDERS.md)                        | **Provider selection and governance**       | Prompts are provider-neutral (§2). Where a provider needs prompt shaping to behave, that is a **provider-specific assumption** recorded there ([AI_PROVIDERS §4](./AI_PROVIDERS.md#4-provider-capability-model)) — never a licence to fork the prompts here. A provider change that would require forking is a change to both documents and is reviewed by both.                                 |
| [EVALUATION.md](./EVALUATION.md)                            | **How quality is measured**                 | This document defines when a prompt change **requires** a judgement (§5, Prompt Review Process) and what a version is judged as (§6). It does **not** define how quality is measured, scored, or reported — that is EVALUATION's. This document consumes a verdict; it does not produce one.                                                                                                     |
| [RAG.md](./RAG.md)                                          | **Retrieval design, if and when it exists** | Retrieval is deferred ([DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)). If it arrives it changes **what context reaches a prompt**, not how prompts are governed. RAG would own the retrieval; the context it supplies would enter through the existing channel model and this document's review unchanged.                                                                   |
| [SECURITY.md](../01-architecture/SECURITY.md)               | **The security posture**                    | It owns AI security, including prompt injection and the treatment of untrusted input ([§10](../01-architecture/SECURITY.md#10-ai-security)). This document owns only the **authoring consequence**: a prompt is composed so those controls hold, and a prompt that weakens one is not a style question but a security finding (§8).                                                              |
| **The ADRs**                                                | **Ratified decisions**                      | [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) established provider independence; [ADR-010](../01-architecture/decisions/ADR-010-AI-Request-Lifecycle.md) the request lifecycle. This document operates **inside** them and MUST NOT alter one. A prompt need that would is an ADR, not a prompt review ([CLAUDE.md §8](../../CLAUDE.md)).                           |

In one line each:

> **AI_ARCHITECTURE defines the AI behavior and orchestration model. AI_PROVIDERS defines provider selection and
> governance. PROMPTS defines prompt design, structure, lifecycle, and review. EVALUATION defines how prompt quality is
> measured. RAG defines how retrieved context is incorporated, if and when it exists.**

### Relationship to the frozen product documents

This document introduces **no product behavior**. Prompts *express* behavior the frozen documents already grant; they
never *define* it. Where a prompt would need to state a rule, that rule belongs in [SRS](../00-product/SRS.md) and the
prompt derives from it (§2). Where a prompt would introduce behavior no document grants, it stops and is raised per
[CLAUDE.md §8](../../CLAUDE.md).

---

## 2. Prompt Philosophy

These principles explain *why* prompt governance is shaped the way it is. They are the reasoning behind the enforceable
rules that follow.

| Principle                    | Why it exists                                                                                                                                                                                                                                                                           |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Clarity over cleverness**  | A prompt that works for reasons nobody can articulate cannot be maintained, only preserved. Clever phrasing that happens to help is indistinguishable from superstition the moment the person who wrote it leaves — and it is the first thing to break when anything else changes.      |
| **Task-oriented prompts**    | A prompt exists to accomplish one documented capability ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)). A prompt serving several tasks is several prompts sharing a body, and it fails all of them slowly rather than one of them visibly.           |
| **Prompt neutrality**        | Prompts are written for the task, not for whatever provider is active. A prompt tuned to one provider's quirks is that provider's lock-in expressed in prose — the form the port cannot protect against ([AI_PROVIDERS](./AI_PROVIDERS.md)).                                            |
| **Human-readable structure** | A prompt is reviewed by people, and review is the only gate it has. Structure that a reader can follow is what makes review real rather than ceremonial; a wall of accreted instructions is approved by exhaustion.                                                                     |
| **Reuse over duplication**   | The same obligation stated in four prompts is four places to fix and three places to forget. Duplication is how capabilities drift apart while every individual prompt still looks correct.                                                                                             |
| **Context discipline**       | Everything sent is sent at a cost — in privacy exposure, in attention diluted across irrelevant content, and in the space left for invention. Context is decided by what the task requires, never by what is available ([NFR-018](../00-product/SRS.md#9-non-functional-requirements)). |
| **Provider independence**    | The provider is deliberately deferred ([DD-002](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)). Prompts are the easiest place for that deferral to leak, because a provider accommodation looks like ordinary wording and no type system objects.                            |
| **Grounded output**          | Grounding is the product's central promise ([BR-030](../00-product/SRS.md#5-business-rules)). It is architecture's obligation, but it is *carried* by prompts — a prompt that quietly permits invention defeats the layered defenses above it.                                          |
| **Safety by design**         | Untrusted document and user content flows through prompts ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)). Safety is a property of how a prompt is composed, decided when it is authored — it cannot be added afterwards to a prompt that blurred its channels.         |
| **Reviewability**            | A prompt is the one artifact that changes product behavior without changing code. If it is not reviewable, it is not governed — and the change nobody reviewed is the one that reaches a real client's document.                                                                        |

---

## Prompt Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Every prompt MUST have a clear owner.** *An unowned prompt is edited by whoever is nearest the symptom, and its
  accumulated intent lives nowhere. Ownership is what makes "why does it say that?" answerable.*
- **Every prompt MUST serve a documented use case.** *A prompt with no capability behind it is undocumented product
  behavior by definition ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)); the product
  does what its documents grant, and a prompt is not a grant.*
- **Prompt content MUST remain provider-neutral.** *A provider-tuned phrase converts a configuration change into a
  rewrite, and it hides in prose where review is least likely to catch it — undoing the independence
  [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) exists to protect.*
- **Prompt structure SHOULD be reusable where the task is shared.** *A shared obligation expressed once is corrected
  once. Where capabilities genuinely differ, they differ deliberately (§4) — not by having drifted.*
- **Prompt assumptions MUST be documented.** *Every prompt assumes something: about the content, the task, the reader,
  the shape expected back. Undocumented, those assumptions are indistinguishable from requirements to the next
  maintainer, who preserves them faithfully long after they stopped being true.*
- **Prompt changes MUST be reviewed before production use.** *A prompt change is a behavior change with no type to
  check it and no test that fully substitutes for judgment. Review is the only gate it has (Prompt Review Process).*
- **Prompt fragments MUST NOT fork silently across capabilities.** *A fragment copied to be tweaked "just here" is the
  moment one shared obligation becomes two divergent ones. Forking MAY be correct; forking **silently** never is.*
- **Prompt text MUST NOT embed product behavior that belongs in frozen documents.** *A rule stated only in a prompt is a
  requirement nobody can find, review, or test. Behavior lives in [SRS](../00-product/SRS.md); the prompt derives from
  it and never becomes its only home.*
- **Prompt output expectations MUST be grounded in product rules.** *What a prompt asks for must match what the product
  promises and what validation
  accepts ([AI_ARCHITECTURE §11](../01-architecture/AI_ARCHITECTURE.md#11-ai-output-validation));
  a prompt requesting a shape the product does not honor manufactures failure downstream.*
- **Prompt decisions MUST remain traceable.** *A phrase whose reason is unrecorded cannot be changed safely — so it is
  never changed, only added to. That is how prompts calcify into text nobody dares touch (§6).*

**Why these rules exist.** Prompt drift is silent by construction. A prompt has no signature to break, no test that
proves it still means what it meant, and no reviewer who reads it as carefully the tenth time. Behavior changes one
clause at a time, each reasonable, until a capability no longer does what its documentation says — and the
documentation is not wrong, the prose is.

These rules exist because the three failure modes are invisible until they are expensive: **drift** (behavior moves
without a decision), **accidental provider lock-in** (accommodation that looks like ordinary wording), and
**undocumented behavior change** (a rule that exists only in a paragraph). Each is cheap to prevent at authoring time
and costly to discover in production, on a real client's document.

---

## 3. Prompt Architecture

**The channel model is owned by
[AI_ARCHITECTURE §8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture)** — which channels exist, what each
carries, and why their separation is a **security control** rather than tidiness. This section restates none of it and
MUST NOT redefine, rename, merge, or add a channel.

**What a prompt is here.** For governance purposes a prompt is not a string but a **composed, versioned artifact**: an
assembly of parts, authored deliberately, that expresses one documented capability. It is composed centrally, never in
feature code ([CLAUDE.md §7](../../CLAUDE.md),
[AI Design Rules](../01-architecture/AI_ARCHITECTURE.md#ai-design-rules)).

**The authoring concerns, mapped onto the frozen channels.** The channels are architecture's; what follows is the
*authoring* view of what must be decided within them. Each concern below is a thing a prompt author is responsible for,
and a thing a reviewer can ask about — it is **not** a new channel.

| Authoring concern          | What must be decided                                                                                                                                                                                                                         |
|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **System framing**         | The fixed, non-negotiable guidance that holds regardless of input. It is stable across requests and is where the product's obligations live rather than being re-litigated per task.                                                         |
| **Role framing**           | Who the assistant is being asked to be, expressed once and consistently. Inconsistent role framing across capabilities is how one product acquires several personalities.                                                                    |
| **Task framing**           | What this specific capability is asking for — the one job. If task framing cannot be stated briefly, the prompt is serving more than one task (§2).                                                                                          |
| **User context**           | The user's question or instruction, carried as **untrusted input** ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)). The authoring concern is that it stays data and never becomes instruction.                               |
| **Document context**       | The grounded content the output must derive from, likewise carried as data. The authoring concern is *what* is included and how it is delimited — never that its boundary is optional.                                                       |
| **Grounding constraints**  | How the prompt holds the output to the supplied content, including declining when the content does not support an answer. The obligation is [§9](../01-architecture/AI_ARCHITECTURE.md#9-grounding-strategy)'s; carrying it is the prompt's. |
| **Safety constraints**     | How the prompt preserves the separation between instruction and data, so the composition itself resists injection rather than relying on the model's goodwill.                                                                               |
| **Output constraints**     | The shape the product expects and validation will accept ([§11](../01-architecture/AI_ARCHITECTURE.md#11-ai-output-validation)). A prompt asking for a shape the product does not honor has manufactured a downstream failure.               |
| **Formatting constraints** | The presentation the capability needs, kept distinct from the *content* obligations above so a formatting change never quietly becomes a behavior change.                                                                                    |

> **Why the mapping rather than a second model:** two documents describing the same parts under different names is how
> the two drift, and the reader is left asking which is authoritative. The channels are the architecture; these are the
> decisions made while filling them. When the two appear to disagree, **AI_ARCHITECTURE §8 wins** and this document is
> corrected.

---

## 4. Prompt Composition

Prompts are **assembled, not written whole**. Composition is what turns a shared obligation into a single artifact
rather than a convention everyone is trusted to remember.

| Layer                                | Its role                                                                                                                                                                                                                                        |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Stable core instructions**         | What holds for every capability — the product's standing obligations. Authored once, changed rarely, and reviewed most heavily, because a change here reaches every capability at once.                                                         |
| **Capability-specific instructions** | What distinguishes this one task from the others ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)). This layer is where capabilities are *allowed* to differ, and therefore where difference must be justified. |
| **Context-specific instructions**    | What varies per request. It is the most volatile layer and MUST NOT be where standing obligations are restated — an obligation stated per request is an obligation that can be omitted per request.                                             |
| **Safety instructions**              | The composition that keeps untrusted content as data ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)). Belongs to the stable core: safety that varies by capability is safety that has a gap.                                    |
| **Output instructions**              | What shape is expected back, aligned with what validation accepts ([§11](../01-architecture/AI_ARCHITECTURE.md#11-ai-output-validation)).                                                                                                       |
| **Reusable fragments**               | A shared obligation expressed once and composed into every prompt that needs it — the mechanism that makes "reuse over duplication" enforceable rather than aspirational.                                                                       |
| **Prompt boundaries**                | Where one part ends and the next begins. Boundaries are load-bearing: they are what make the parts separately reviewable, separately reusable, and separately versionable (§6).                                                                 |

**Why composition is a maintainability property, not a style.** A prompt written whole is correct exactly once. When an
obligation changes — and they change, because the product's rules change — a composed prompt has **one** place to
correct and a written-whole prompt has as many places as it was copied to. The second kind is not fixed; it is *mostly*
fixed, and the capability that was missed drifts away from the others while continuing to look reasonable in isolation.

**Divergence MAY be correct; silent divergence never is.** Where a capability genuinely needs different wording, that is
a decision recorded against it (§6), not a fragment quietly copied and adjusted. The rule is not that prompts must be
identical — it is that difference must be **chosen**.

---

## 5. Prompt Lifecycle

A prompt is not written and then simply present. It occupies a **stage**, and moves between stages only through review.
Naming the stages makes the difference between *trying wording* and *shipping behavior* explicit — because that boundary
is where undocumented behavior change forms.

| Stage         | What it means                                                                                                                                                                                                                                                            |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Proposed**  | A need is identified and the capability it serves named ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)). A need with no documented capability behind it does not proceed; it is raised per [CLAUDE.md §8](../../CLAUDE.md).            |
| **Drafted**   | Composed from the layers (§4), with its owner named and its assumptions written down. A draft carries no standing: nothing may depend on it and it MUST NOT reach production.                                                                                            |
| **Reviewed**  | Assessed against the checklist (§9), with quality judged per [EVALUATION](./EVALUATION.md) where the change warrants it. The outcome is a finding, not yet permission: a draft that fails the checklist does not proceed, and the finding is recorded.                   |
| **Approved**  | Judged fit and permitted for production use, with the decision recorded (§10). Approval attaches to a **specific version** (§6) — approving a prompt is never approving its future edits.                                                                                |
| **Versioned** | Assigned an identifier and its change history recorded (§6). This is what makes an output attributable after the fact: without it, "which prompt produced this?" has no answer.                                                                                          |
| **Deployed**  | In production for a capability. Only an approved version may be deployed; if any other path can reach production, approval is advisory.                                                                                                                                  |
| **Monitored** | Observed against the expectations it was approved on ([AI_ARCHITECTURE §14](../01-architecture/AI_ARCHITECTURE.md#14-ai-observability)). Drift from those expectations is a review trigger, not a curiosity — including drift caused by something other than the prompt. |
| **Revised**   | Changed through review, producing a **new version** rather than mutating the deployed one (§6). A revision that silently replaces its predecessor destroys the attribution the version existed to provide.                                                               |
| **Retired**   | No longer used. It MUST NOT be reachable, and its record stays — *why we stopped saying it that way* is the most valuable thing to know when the wording is proposed again, which it will be.                                                                            |

**Experimentation versus production use.** Both are legitimate; conflating them is not. **Experimentation** is bounded,
non-production, uses no real client content ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)), and creates
no dependency — it produces a *finding*. **Production use** requires an approved version, a named owner, documented
assumptions, and observability.

A prompt MUST NOT reach production by accumulation — a phrase tried in an experiment, kept because it helped, carried
into a branch, and shipped without anyone deciding. That path produces behavior no document describes and no one
approved, and the lifecycle exists to interrupt it.

---

## 6. Prompt Versioning

**Prompt versions are governed assets, not text blobs.** The distinction is not ceremony: a version is what makes an
output attributable, a regression bisectable, and a change reversible. Without it, "the summary got worse last week" is
unanswerable — and the AI request record ([DATABASE §5.5](../01-architecture/DATABASE.md#55-airequest)) can say *that*
a request happened without anyone able to say *what asked it*.

| Aspect                         | The expectation                                                                                                                                                                                                                                                                  |
|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Version identifiers**        | Every production prompt is identified, and the identifier is **stable and never reused** — the same discipline [SRS](../00-product/SRS.md) applies to requirement IDs. A reused identifier makes every historical reference ambiguous.                                           |
| **Change history**             | What changed, why, who owned it, and what it was judged on. The *why* is the part that decays first and matters most; without it a later maintainer can only add, never remove.                                                                                                  |
| **Compatibility expectations** | A version states what callers and validation may rely on ([§11](../01-architecture/AI_ARCHITECTURE.md#11-ai-output-validation)). What is not stated is not promised — an incidental behavior depended upon is a breaking change waiting to be blamed on the wrong thing.         |
| **Backward compatibility**     | A revision that preserves the stated expectations is compatible and replaces its predecessor after review. Compatibility is a claim about the **stated** contract, never about output being identical — identical output is not a thing a probabilistic system can promise (§8). |
| **Breaking changes**           | A revision that changes the stated shape, the obligations, or what downstream may rely on is **breaking**, regardless of how small the edit looks. Prompt edits are unusually deceptive here: a clause is a smaller diff than a signature and a larger change than one.          |
| **Deprecation**                | A version still in use but no longer intended for new work, with its successor named. Deprecation is a **recorded decision with a reason**, never an informal preference for the newer one.                                                                                      |
| **Replacement**                | A version supersedes another for a capability. The replaced version does not vanish: it stays on record so the transition is traceable and so an output produced under it remains explicable.                                                                                    |

**Why this is not over-engineering for prose.** A prompt is the only artifact that changes behavior with no signature to
break and no compiler to object. Everything else in the product gets versioning for free from the tools; prompts get it
only if it is a decision. This is not more governance than code receives — it is the same governance, applied to the one
artifact whose tooling does not supply it.

---

## 7. Prompt Variables and Context

**What may reach a prompt.** Context is admitted because the task requires it, never because it is available. Each kind
below enters through the channel
model ([AI_ARCHITECTURE §8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture))
and this section governs only the **discipline of deciding what goes in**.

| Context                        | The discipline                                                                                                                                                                                                                             |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Document content**           | The grounded source the output derives from, scoped to the owning user's document ([BR-004](../00-product/SRS.md#5-business-rules), [BR-035](../00-product/SRS.md#5-business-rules)). Included as far as the task requires and no further. |
| **User intent**                | What the user asked for. Carried as data, never as instruction — this is a composition property (§3), not a hope about the model.                                                                                                          |
| **Capability-specific inputs** | What this one task legitimately needs. If a capability needs an input no other does, that is a difference to justify (§4), not to add quietly.                                                                                             |
| **State or metadata**          | Non-sensitive context that aids interpretation. Admitted only where it changes the output for the better — metadata included "for completeness" is payload with no purpose.                                                                |
| **Grounding context**          | The basis the output must be traceable to. Its presence is what makes the product's central promise checkable rather than asserted.                                                                                                        |
| **Constraints**                | The obligations and limits the task operates under, derived from product rules rather than restated as prompt-local inventions (§2).                                                                                                       |
| **Formatting expectations**    | The shape needed back, kept distinct from content obligations so the two can change independently (§3).                                                                                                                                    |

**What MUST NOT reach a prompt.** These are not stylistic exclusions; each is a control failing:

- **Content the task does not require.** Data minimization is a security obligation
  ([NFR-018](../00-product/SRS.md#9-non-functional-requirements),
  [AI_ARCHITECTURE §15](../01-architecture/AI_ARCHITECTURE.md#15-ai-data-privacy)), not a tidiness preference.
  Everything
  sent crosses an untrusted boundary.
- **Another user's content, ever.** Per-user isolation does not weaken at this boundary
  ([BR-004](../00-product/SRS.md#5-business-rules)); no prompt composes context from more than the owning user's
  material.
- **Secrets, credentials, or internal configuration.** These are never context
  ([SECURITY §13](../01-architecture/SECURITY.md#13-secrets-management)). A prompt is transmitted to an external system
  by definition.
- **Internal system detail, architecture, or reasoning about our own controls.** It is not needed for the task, and it
  can surface in output (§8).
- **Product rules that belong in the frozen documents.** A rule reaching the model only as prompt context is a rule with
  no other home (Rules).

---

## 8. Prompt Safety and Grounding

**The obligations here are already owned.
** [AI_ARCHITECTURE §9](../01-architecture/AI_ARCHITECTURE.md#9-grounding-strategy)
owns grounding; [§10](../01-architecture/AI_ARCHITECTURE.md#10-hallucination-mitigation) owns the layered defenses;
[§11](../01-architecture/AI_ARCHITECTURE.md#11-ai-output-validation) owns validation; the
[AI Design Rules](../01-architecture/AI_ARCHITECTURE.md#ai-design-rules) and
[BR-030–BR-033](../00-product/SRS.md#5-business-rules) bind them. **This document defines no model behavior and restates
none of these.**

This section owns one thing: **the authoring obligation that follows from them.** Grounding and safety are architectural
guarantees, but they are *carried* by prompts — a defense the prompt quietly undermines is a defense that was never
there. What follows is what an author is accountable for and a reviewer can ask about.

| The frozen obligation                    | What the prompt author is accountable for                                                                                                                                                                                                                                                                         |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Grounded answers only**                | The composition holds the output to the supplied content and gives it no licence to reach beyond it. Wording that invites general knowledge — even helpfully — has widened grounding without a decision.                                                                                                          |
| **Refusal when content is insufficient** | Declining is composed as a **legitimate, expected outcome**, not an edge case or a failure. If a prompt makes refusal feel like failing the task, it has created pressure to guess.                                                                                                                               |
| **No fabrication**                       | Nothing in the composition rewards a plausible answer over an honest absence. "Unknown" is always preferable to invention, and the prompt must not make it the harder path.                                                                                                                                       |
| **No invention of facts**                | The prompt asks for what the content supports, never for completeness the content cannot supply. A request to "fill in" is a request to invent, however it is phrased.                                                                                                                                            |
| **No overstatement of certainty**        | The composition does not ask for confidence the evidence cannot carry, and never for a number to express it. Fabricated precision is a lie with a decimal point.                                                                                                                                                  |
| **No leaking internal reasoning**        | The prompt does not ask for internal deliberation to be surfaced, and does not carry internal system detail that could reach output (§7, [SECURITY](../01-architecture/SECURITY.md)).                                                                                                                             |
| **Human review where appropriate**       | The composition presents output as a professional's starting point, consistent with the product's human-in-the-loop guarantee ([BR-031](../00-product/SRS.md#5-business-rules), [BR-032](../00-product/SRS.md#5-business-rules)). A prompt that asks for a verdict is at odds with a product that offers a draft. |

**Safety is composed, not requested.** The separation of instruction from data is a property of **how the prompt is
assembled** ([AI_ARCHITECTURE §8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture),
[SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)) — not an instruction politely asking untrusted content to
behave. A prompt that relies on asking has no defense; it has a request.

---

## 9. Prompt Review Checklist

Every prompt — new or revised — is evaluated against this checklist before acceptance. A "no" is a finding to resolve,
not a detail to defer.

- [ ] **Use case documented?** — It serves a granted capability, named
  ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)).
- [ ] **Owner identified?** — A named owner accountable for its intent (§5).
- [ ] **Structure clear?** — Composed of the layers, with boundaries a reviewer can follow (§4).
- [ ] **Provider-neutral?** — No wording tuned to a provider's quirks; no provider or model named (§2).
- [ ] **Grounded output supported?** — Grounding held, refusal composed as legitimate, invention unrewarded (§8).
- [ ] **No product-behavior leakage?** — No rule lives only here; each derives from a frozen document (Rules).
- [ ] **No duplication of frozen docs?** — Obligations are derived and referenced, not restated as prompt-local text.
- [ ] **Version tracked?** — Identified, with its change history and the reason for the change recorded (§6).
- [ ] **Review complete?** — Assumptions documented; evaluation obtained where the change warranted it (§5).
- [ ] **Safe to deploy?** — Channel separation preserved; nothing in §7's exclusion list reaches the prompt (§7, §8).

---

## Prompt Review Process

> *Unnumbered governance section. It defines when a prompt is reviewed and how prompt governance evolves —
> deliberately, not by accident.*

**Review triggers** — a prompt review is required when any of the following occurs:

- **A new prompt** is proposed.
- **A prompt change** — any edit to an approved version, however small. Size is not a proxy for impact here.
- **A new AI capability** is introduced — it needs a prompt, and the prompt needs an owner and a use case.
- **A provider change** — an active or fallback provider
  changes ([AI_PROVIDERS §5](./AI_PROVIDERS.md#5-provider-lifecycle)); prompts are re-examined for neutrality and for
  assumptions that were the old provider's.
- **An evaluation regression** — measured quality drops against what a version was approved on.
- **A security concern** — anything touching injection resistance, channel separation, or what reaches a prompt.
- **A product rule change** — a rule a prompt derives from changes in [SRS](../00-product/SRS.md); the prompt follows,
  never the reverse.
- **An architecture change** — the channel model, pipeline, or validation changes
  ([AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md)).

**Review outcomes** — each review resolves to exactly one:

- **Approved** — fit for production at a specific version; the decision and rationale are recorded (§6, §10).
- **Refinement required** — the intent is sound but something must change first: an undocumented assumption, an unclear
  boundary, a duplicated obligation.
- **Evaluation required** — the change is plausible but its effect is unmeasured; it is routed to
  [EVALUATION](./EVALUATION.md) and returns as a finding. **A behavior change is never approved on reasoning alone.**
- **Provider review required** — the prompt appears to depend on provider behavior; it is routed to the
  [Provider Review Process](./AI_PROVIDERS.md#provider-review-process). A prompt that only works on one provider is a
  provider finding, not a prompt style.
- **Security review required** — injection resistance, channel separation, or context exposure is in question; it is
  routed to the security review process
  ([SECURITY](../01-architecture/SECURITY.md#ai-changes--review-required-for)). This outcome **blocks**.
- **Architecture review required** — the prompt implies a change to the channel model, the pipeline, or validation; it
  is raised per [CLAUDE.md §8](../../CLAUDE.md) before proceeding.
- **ADR required** — the decision is significant, precedent-setting, or hard to reverse.

Every outcome resolves to a lifecycle position (§5). Only **Approved** advances a prompt past *Reviewed*; **Refinement
required** returns it to *Drafted*; every other outcome holds it at *Reviewed* until the review it was routed to
returns, and a prompt held there is not approved by the passage of time. *Reviewed* is a stage a prompt passes through,
never one it rests in.

**Synchronization.** Prompt governance MUST remain synchronized with
[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md), [AI_PROVIDERS](./AI_PROVIDERS.md),
[EVALUATION](./EVALUATION.md), [RAG](./RAG.md), [SECURITY](../01-architecture/SECURITY.md), and the relevant
[ADRs](../01-architecture/decisions/). When this document and one of them disagree, **they win** and this document is
corrected ([CLAUDE.md §3](../../CLAUDE.md)). A change touching more than one is reviewed by each document it touches and
MUST NOT be merged into one while leaving another contradicting it — a prompt change that forces a provider
accommodation, invalidates an evaluation baseline, or alters retrieved context is a change to those documents too.

---

## 10. Prompt Decision Summary

The load-bearing decisions behind prompt governance, recorded so they are not silently reversed. The **channel model**
is [AI_ARCHITECTURE §8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture)'s decision and is not restated
here; these are the governance decisions built on top of it.

| Decision                               | Chosen Approach                                                                                         | Alternatives                                                                 | Rationale                                                                                                                                                                                                                                                                                                       |
|----------------------------------------|---------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Prompts as governed artifacts**      | A prompt has an owner, a version, documented assumptions, and a review before production                | Treat prompts as configuration or copy, editable like text                   | A prompt is the only artifact that changes behavior with no signature to break and no compiler to object. Everything else gets governance from its tooling; prompts get it only by decision (§5, §6).                                                                                                           |
| **Prompt neutrality**                  | Prompts are written for the task; provider accommodations are recorded as provider-specific assumptions | Tune prompts to whichever provider is active                                 | A provider-tuned phrase is lock-in in prose — the one form the port cannot protect against, because no type system objects to wording. It would silently undo [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) and [DD-002](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions). |
| **Composed, not written whole**        | Prompts are assembled from layered parts, with shared obligations expressed once                        | Author each prompt independently; copy and adapt as capabilities appear      | A written-whole prompt is correct once; when an obligation changes, a composed prompt has one place to fix and a copied one has as many as it was copied to — and the one that is missed drifts while still looking reasonable (§4).                                                                            |
| **Deliberate divergence**              | Capabilities may differ, but difference is a recorded decision, never a quiet copy-and-tweak            | Forbid divergence entirely; or allow fragments to fork freely as needs arise | Forbidding divergence forces capabilities into one shape that fits none; allowing silent forks turns one obligation into several nobody chose. The governable middle is that difference must be **chosen** (§4).                                                                                                |
| **Versioned prompts**                  | Versions are identified, never reused, with change history and stated compatibility                     | Keep the latest text only; rely on source history for the record             | Source history says what changed, not what was promised or why. Without a stated version an output is unattributable and a regression is unbisectable — the AI request record can say a request happened but not what asked it (§6).                                                                            |
| **Behavior stays in frozen documents** | Prompts derive from product rules; a rule never lives only in a prompt                                  | Let prompts carry behavior directly where it is faster                       | A rule stated only in prose nobody indexes is a requirement that cannot be found, reviewed, or tested, and it contradicts the frozen document silently. Prompts express behavior; they never grant it ([SRS](../00-product/SRS.md)).                                                                            |
| **Grounding carried by composition**   | Refusal is composed as a legitimate outcome and invention is never rewarded                             | State grounding as an instruction and trust the model to comply              | Grounding is architecture's guarantee but the prompt carries it; a composition that makes declining feel like failure creates pressure to guess and defeats the layered defenses above it ([BR-033](../00-product/SRS.md#5-business-rules), §8).                                                                |
| **Safety by composition, not request** | Instruction/data separation is structural, per the channel model                                        | Instruct the model to ignore instructions found in user or document content  | Asking untrusted content to behave is a request, not a defense. Separation is what lets document and user text be treated as data and resist injection ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)).                                                                                         |
| **Context discipline**                 | Only what the task requires reaches a prompt; exclusions are control failings, not preferences          | Send generous context and let the model select what matters                  | Everything sent crosses an untrusted boundary and dilutes attention across irrelevance, widening the space for invention. Minimization is a security obligation ([NFR-018](../00-product/SRS.md#9-non-functional-requirements)), not tidiness (§7).                                                             |

---

*This document governs how LedgerAI's prompts are designed, structured, versioned, reviewed, and evolved; it does not
override the frozen documents under [`docs/`](../). It operates inside
[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md), stays neutral to the providers governed by
[AI_PROVIDERS](./AI_PROVIDERS.md), expresses only behavior granted by [PRD](../00-product/PRD.md) and
[SRS](../00-product/SRS.md), respects [SECURITY](../01-architecture/SECURITY.md), consumes findings from
[EVALUATION](./EVALUATION.md), and anticipates [RAG](./RAG.md) without depending on it. When a prompt decision is
required, review it through the process above and, when a prompt would imply new product behavior or alter a ratified
decision, stop and raise it per [CLAUDE.md §8](../../CLAUDE.md).*
