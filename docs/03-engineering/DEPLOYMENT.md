# Deployment — LedgerAI

> **Status:** Draft v1
> **Owner:** Principal Release / Deployment Architect
> **Last updated:** 2026-07-16
> **Upstream (frozen):
** [CLAUDE.md](../../CLAUDE.md) · [IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md) · [TESTING_STRATEGY](./TESTING_STRATEGY.md) · [SECURITY](../01-architecture/SECURITY.md) · [ARCHITECTURE](../01-architecture/ARCHITECTURE.md) · [ADR-012](../01-architecture/decisions/ADR-012-Deployment-Strategy.md) · [ADR-015](../01-architecture/decisions/ADR-015-Observability.md)
> **Related:
** [CONTRIBUTING](./CONTRIBUTING.md) · [IMPLEMENTATION_STATUS](./IMPLEMENTATION_STATUS.md) · [EVALUATION](../04-ai/EVALUATION.md) · [
`docs/05-releases/`](../05-releases/)

---

## 1. Purpose

### Why this document exists

Every gate in this repository protects a decision *before* it is real. Deployment is where the protection ends: the
moment a change stops being a claim about what the product will do and becomes what a Chartered Accountant experiences
on a document that matters to their client.

**Where LedgerAI runs is already settled.**
[ADR-012](../01-architecture/decisions/ADR-012-Deployment-Strategy.md) owns the hosting topology and the platforms —
this document names none of them and re-decides none of it. **What must be verified first** is
[TESTING_STRATEGY](./TESTING_STRATEGY.md)'s. **When a change is finished** is
[IMPLEMENTATION_PLAN §7](./IMPLEMENTATION_PLAN.md#7-definition-of-done)'s. **How a change is routed** is
[CONTRIBUTING](./CONTRIBUTING.md)'s. **What is safe** is [SECURITY](../01-architecture/SECURITY.md)'s.

What no document owns is the step between *merged* and *live*: **promotion** — the decision to move a verified change
into an environment where someone real depends on it, what makes that decision safe, what happens when it was wrong,
and what is watched afterward. [IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md) explicitly defers this: *"Detailed
testing, deployment, and contribution practices live in their own documents."* This document owns that, and only that.

The governing principle of this document:

> **Deployment publishes decisions; it never makes them.**
>
> Everything live was decided somewhere else — or it was decided *by the deployment*, by whoever was holding it, at the
> moment least suited to deciding anything and with the least appetite for turning back.

It is **not** a CI document, **not** a tooling or infrastructure-as-code document, **not** a runbook, and **not**
implementation. It contains **no commands, no CI syntax, no scripts, no infrastructure definitions, no platform or
vendor names, no environment variable names, and no code**. It governs *how a verified change becomes live, and what is
owed before and after* — never what the change does.

### Routine and governed are not opposites

[IMPLEMENTATION_PLAN §6](./IMPLEMENTATION_PLAN.md#6-development-workflow) settles that **deployment is routine, not an
event**, and this document does not make it one. Routine means frequent and unceremonious. Governed means it passes
gates that were agreed in advance. **The gates are what make it routine:** a promotion nobody has to deliberate over is
one where the deliberation already happened and was written down. Ceremony is what a team invents when it does not trust
its gates.

What is never routine is promoting something that has **not** passed them — and that is the only thing this document
slows down.

### Relationship to the governing documents

| Document                                               | Its job                                | The boundary with this document                                                                                                                                                                                                                                                                                                                                                                           |
|--------------------------------------------------------|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [CLAUDE.md](../../CLAUDE.md)                           | **The engineering playbook**           | It owns the hierarchy and precedence (§3), the stop-and-ask conditions (§8), and the Definition of Complete (§9) — which ends at *`main` stays green and deployable*. This document begins where that ends and adds no rule of its own about which document wins.                                                                                                                                         |
| [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)     | **Build order and the merge gate**     | It owns the **build path** ([§6](./IMPLEMENTATION_PLAN.md#6-development-workflow)) and the **Definition of Done** ([§7](./IMPLEMENTATION_PLAN.md#7-definition-of-done)) — the gate a change passes to *merge*. This document owns the gate a merged change passes to go **live**. It adds no stage to that workflow and no item to that gate.                                                             |
| [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md) | **Live execution state**               | The one document expected to change routinely ([CLAUDE.md §3](../../CLAUDE.md)). It records what has landed; this document requires that a promotion is reflected there, and defines none of what it records.                                                                                                                                                                                             |
| [TESTING_STRATEGY.md](./TESTING_STRATEGY.md)           | **What must be verified**              | It owns what is tested, at which level, in which environment ([§13](./TESTING_STRATEGY.md#13-test-environments)), and [Definition of Test Complete](./TESTING_STRATEGY.md#14-definition-of-test-complete). This document **consumes** that verdict as a gate; it defines no test and lowers no bar.                                                                                                       |
| [SECURITY.md](../01-architecture/SECURITY.md)          | **The safety constraints**             | It owns the constraints deployment operates inside — secrets ([§13](../01-architecture/SECURITY.md#13-secrets-management)), logging ([§16](../01-architecture/SECURITY.md#16-logging-and-audit)), and the rest. A promotion never relaxes one to ship; a security concern **blocks** (Deployment Review Process).                                                                                         |
| [ARCHITECTURE.md](../01-architecture/ARCHITECTURE.md)  | **System design**                      | It owns the structure being deployed and the scaling path ([§11](../01-architecture/ARCHITECTURE.md#11-scalability-strategy)); [ADR-012](../01-architecture/decisions/ADR-012-Deployment-Strategy.md) owns the **hosting topology**. This document operates inside both. A promotion needing either to change is an architecture change, not a deployment decision.                                       |
| [CONTRIBUTING.md](./CONTRIBUTING.md)                   | **How changes are routed**             | It routes a change to its owning document *before* a branch exists ([§4](./CONTRIBUTING.md#4-repository-workflow)) and names every review it owes ([§7](./CONTRIBUTING.md#7-review-and-approval-rules)). By the time this document applies, that routing has already happened — or the change is not eligible to be promoted (§6).                                                                        |
| The release documents                                  | **What a release contains and claims** | [`docs/05-releases/`](../05-releases/) — **not yet authored**. A release **records** what shipped; this document governs the act of shipping. Neither decides behavior: what a release records was granted elsewhere, before the release named it.                                                                                                                                                        |
| **The ADRs**                                           | **Ratified decisions**                 | [ADR-012](../01-architecture/decisions/ADR-012-Deployment-Strategy.md) the topology; [ADR-015](../01-architecture/decisions/ADR-015-Observability.md) baseline observability; [ADR-013](../01-architecture/decisions/ADR-013-Background-Processing.md) background processing. This document operates **inside** them; a deployment need that would alter one is an ADR ([CLAUDE.md §8](../../CLAUDE.md)). |

In one line each:

> **IMPLEMENTATION_PLAN defines the build path. IMPLEMENTATION_STATUS records current execution state. TESTING_STRATEGY
> defines what must be verified before deployment. SECURITY defines the safety constraints deployment must respect.
> CONTRIBUTING defines how changes are routed before deployment ever happens. DEPLOYMENT defines how a verified change
> becomes live.**

### Relationship to the frozen product documents

This document introduces **no product behavior**. It grants nothing, changes no rule, and relaxes no constraint. Where a
deployment appears to require different behavior, that is a change to the document that owns the behavior, raised per
[CLAUDE.md §8](../../CLAUDE.md) — never a decision made under the pressure of a release that is already in flight.

---

## 2. Deployment Philosophy

These principles explain *why* deployment is governed the way it is. They are the reasoning behind the enforceable rules
that follow.

| Principle                                | Why it exists                                                                                                                                                                                                                                                                 |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Deploy only what is approved**         | Approval is what separates a decision from an opinion that shipped. Anything promoted without it was decided by the promotion — and the repository will describe a product that no longer exists ([CLAUDE.md §2](../../CLAUDE.md)).                                           |
| **Promote deliberately**                 | Promotion is the point of no return for someone else: after it, the cost of being wrong is paid by a professional mid-task, not by a contributor mid-review. That asymmetry is why the decision belongs *before* the act, not during it.                                      |
| **Minimize blast radius**                | Every promotion is a hypothesis about correctness. The question is never whether one will be wrong — it is how much is exposed when one is, and whether anyone can tell which change did it.                                                                                  |
| **Preserve rollback ability**            | The value of a way back is decided *before* it is needed and can only be spent once. A release that cannot be undone has converted a recoverable mistake into a forward-only problem, at the moment judgment is worst (§7).                                                   |
| **Keep environments comparable**         | Verification predicts production only to the extent the two resemble each other ([TESTING_STRATEGY §13](./TESTING_STRATEGY.md#13-test-environments)). As they drift apart, the tests keep passing and stop meaning anything — and nothing announces the moment that happened. |
| **Favor repeatable promotion**           | A promotion performed differently each time cannot be reasoned about, compared, or trusted. Repeatability is what makes "it worked last time" evidence rather than a memory.                                                                                                  |
| **Observe before expanding**             | Confidence is not a substitute for looking. Most bad releases are indistinguishable from good ones for the first few minutes — expanding on the strength of nothing having *visibly* broken is deciding without the evidence that was about to arrive (§8).                   |
| **Fail safely**                          | Failure is a normal operating condition, not an exception ([NFR-004/005](../00-product/SRS.md#9-non-functional-requirements)). What is decided in advance is how it fails; what is decided in the moment is whatever hurts least to say.                                      |
| **Keep release behavior traceable**      | When something is wrong in production, the only question is *what changed* — and it must be answerable in minutes, not reconstructed. Traceability is cheap while promoting and impossible afterward ([ADR-015](../01-architecture/decisions/ADR-015-Observability.md)).      |
| **Never let deployment invent behavior** | Deployment is the last gate and the one under the most pressure, which makes it the most tempting place to settle a question nobody settled earlier. A behavior first decided at promotion has no home, no review, and no reader (Rules).                                     |

---

## Deployment Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Every deployment MUST have a documented purpose.** *A promotion that cannot say what it delivers cannot be verified
  against anything, and cannot be reversed intelligently — because nobody can say what would be lost by reversing it.*
- **Every deployment MUST trace back to frozen documents.** *What goes live is what the documents granted
  ([CLAUDE.md §2](../../CLAUDE.md)). Anything else is production behavior with no source of truth — undocumented,
  unreviewable, and discovered by whoever it surprises.*
- **A deployment MUST NOT introduce unreviewed behavior.** *Behavior that first appears at promotion has bypassed every
  review the repository owns ([CONTRIBUTING §7](./CONTRIBUTING.md#7-review-and-approval-rules)). It is not a shortcut
  through the process; it is the absence of one.*
- **A deployment SHOULD be repeatable across environments.** *A promotion that behaves differently per environment
  makes verification meaningless: the thing that passed is not the thing that shipped, and the difference is discovered
  in production.*
- **Deployment promotion MUST follow verified gates.** *The gates are [TESTING_STRATEGY](./TESTING_STRATEGY.md)'s and
  [IMPLEMENTATION_PLAN §7](./IMPLEMENTATION_PLAN.md#7-definition-of-done)'s, and they are not negotiable by urgency.
  A gate waived under pressure is not a gate; it is a formality that was never load-bearing.*
- **Production deployment MUST respect security constraints.** *They are structural
  ([SECURITY](../01-architecture/SECURITY.md)). Confidentiality is this product's existential promise
  ([BR-004](../00-product/SRS.md#5-business-rules)); a release is never the reason to hold it loosely for an afternoon.*
- **Rollback MUST be possible for any deployment that can alter behavior.** *A change that cannot be withdrawn is a
  permanent decision disguised as a release. Where a way back is genuinely impossible, that fact is known and accepted
  **before** promoting, never discovered after (§7).*
- **Deployment decisions MUST remain traceable.** *"What changed?" is the first question of every incident, and the only
  one that matters in the first minutes. Unrecorded, it is answered by reconstruction — slowly, under pressure, and
  sometimes wrongly ([ADR-015](../01-architecture/decisions/ADR-015-Observability.md)).*
- **A deployment SHOULD minimize blast radius.** *Exposure is the one variable still controllable once correctness is
  uncertain. Reducing it is not timidity; it is the difference between a finding and an incident.*
- **A deployment MUST NOT become a substitute for product, architecture, or testing decisions.** *"We will see how it
  behaves in production" is a decision to let users perform the review. Where a question is genuinely open, it is raised
  ([CLAUDE.md §8](../../CLAUDE.md)) — not resolved by exposing a professional to the answer.*

**Why these rules exist.** Deployment fails in ways nothing else catches, because everything else has already passed.
The
three failure modes are **silent production drift** (what runs and what the documents describe diverge, one promotion at
a time, each individually fine), **unsafe promotion** (a gate skipped because the change was small, the fix was urgent,
or the release was already announced), and **deployment becoming an authority it was never given** (a question settled
at promotion because that was the moment someone had to choose).

Each is invisible from inside. Nobody experiences drift as drift; they experience a working system. Nobody skips a gate
they believe is load-bearing; they skip the one that has never yet caught anything. And nobody announces that they are
deciding product behavior at deploy time — they make a reasonable call, under pressure, that no document records and no
reviewer sees. These rules exist because production is where the repository's claims stop being claims.

---

## 3. Environment Model

**The verification environments are owned by
[TESTING_STRATEGY §13](./TESTING_STRATEGY.md#13-test-environments)** — what each is for, and that none uses production
data or credentials ([§12](./TESTING_STRATEGY.md#12-test-data-strategy)). This section restates none of it and
**MUST NOT redefine, rename, merge, or add an environment**. **Where each environment runs** is
[ADR-012](../01-architecture/decisions/ADR-012-Deployment-Strategy.md)'s and is not named here.

What this section adds is the **promotion view**: what each environment is worth as evidence, and what crossing into it
costs.

| Environment    | Its role in promotion                                                                                                                                                                                                                                                                     |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Local**      | Where a change is made and first believed. It is evidence about the author's intent and nothing else — it proves the change can work, never that it does. No promotion decision rests here.                                                                                               |
| **CI**         | The automated gate ([TESTING_STRATEGY §13](./TESTING_STRATEGY.md#13-test-environments)) — deterministic, impersonal, and **green to merge**. It is the strongest evidence available about the machinery, and it says nothing about quality that only a person or an evaluation can judge. |
| **Staging**    | The production-like environment, and the **last place being wrong is free**. Its entire value is resemblance to production: as it diverges, its verdict weakens silently and nothing reports that it has. Divergence is a finding (§8), not a fact of life.                               |
| **Production** | Where a real professional's real client document is at stake. It is the only environment whose failures are someone else's problem, which is why it is the only one whose entry is a **decision** rather than a step (§5).                                                                |

**Environments are separated so that being wrong is survivable.** The separation is not procedural tidiness: it is the
mechanism that keeps the cost of a mistake proportional to how far it travelled. An environment that borrows
production's data, credentials, or trust has quietly removed that protection while still appearing to provide it —
which is worse than not having the environment at all, because the confidence remains.

---

## 4. Deployment Scope

**What deployment governance covers.** Anything whose promotion can change what a professional experiences:

| In scope                                      | The discipline                                                                                                                                                                                                                                                                                                                                 |
|-----------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Application code**                          | The ordinary case, and the one the gates were designed around ([IMPLEMENTATION_PLAN §7](./IMPLEMENTATION_PLAN.md#7-definition-of-done)).                                                                                                                                                                                                       |
| **Documentation-backed behavior changes**     | A change is promotable because a document granted it. The document is not a formality attached to the release; it is the reason the release is allowed ([CONTRIBUTING §4](./CONTRIBUTING.md#4-repository-workflow)).                                                                                                                           |
| **AI prompt, provider, or retrieval changes** | Only once approved by the process that owns them ([PROMPTS](../04-ai/PROMPTS.md#prompt-review-process), [AI_PROVIDERS](../04-ai/AI_PROVIDERS.md#provider-review-process), [RAG](../04-ai/RAG.md#retrieval-review-process)). These alter behavior with no signature to break — promotion is not their review, and never becomes it.             |
| **Database changes**                          | As governed by the [Migration Strategy](../01-architecture/DATABASE.md#database-migration-strategy), which owns compatibility during a rolling deployment and reversibility. This document defers to it entirely and adds no migration rule.                                                                                                   |
| **Configuration changes**                     | Configuration is externalized by design ([BACKEND_CODING_STANDARDS §11](./BACKEND_CODING_STANDARDS.md#11-configuration-standards), [SECURITY §13](../01-architecture/SECURITY.md#13-secrets-management)). It is in scope precisely **because** it changes behavior without changing code — the one promotion that looks like nothing happened. |
| **Release notes and changelog updates**       | Where relevant ([`docs/05-releases/`](../05-releases/), **not yet authored**). A release records; it never grants.                                                                                                                                                                                                                             |

**What is outside scope.** Each is owned elsewhere, and claiming it here would create a second authority:

- **The hosting topology and platforms** — [ADR-012](../01-architecture/decisions/ADR-012-Deployment-Strategy.md)'s.
  Changing it warrants an updated ADR, not a deployment decision.
- **CI systems, scripts, pipelines, and platform configuration** — implementation. This document governs *whether and
  when* a change is promoted, never *by what mechanism*.
- **What is tested and how** — [TESTING_STRATEGY](./TESTING_STRATEGY.md)'s. This document consumes its verdict.
- **When a change is done** — [IMPLEMENTATION_PLAN §7](./IMPLEMENTATION_PLAN.md#7-definition-of-done)'s.
- **What any change should do** — the owning product, architecture, design, or AI document. Deployment publishes
  decisions; it never makes them.

---

## 5. Promotion Lifecycle

Promotion has stages because the distance between *a change exists* and *a professional depends on it* is where every
avoidable production incident is decided.

| Stage                       | What it means                                                                                                                                                                                                                                                                                         |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Approved for deployment** | The change is granted by its document, has discharged every review it owed ([CONTRIBUTING §7](./CONTRIBUTING.md#7-review-and-approval-rules)), and meets the [Definition of Done](./IMPLEMENTATION_PLAN.md#7-definition-of-done). Approval is a **precondition** of promotion, never a product of it. |
| **Prepared**                | What is being promoted is identified, along with what it changes, what it depends on, and how it would be withdrawn (§7). A promotion whose contents are known only as "the latest" cannot be reasoned about or reversed selectively.                                                                 |
| **Verified**                | The gates have returned green ([TESTING_STRATEGY](./TESTING_STRATEGY.md#14-definition-of-test-complete)), in an environment close enough to production for the result to mean something (§3). A stale verification is not evidence; it is a claim about a system that has moved.                      |
| **Promoted**                | Moved into the next environment. This is the governed act: the point where the decision stops being reversible for free, and the last one made by someone who understands the change.                                                                                                                 |
| **Observed**                | Watched against what it was expected to do (§8). Observation is not a courtesy interval before declaring success — it is the only part of promotion that can discover the promotion was wrong.                                                                                                        |
| **Stabilized**              | Behaving as expected, long enough to be believed rather than hoped. Exposure widens from here — never before, and never because nothing has *visibly* broken yet.                                                                                                                                     |
| **Rolled back or retired**  | Withdrawn because it was wrong (§7), or superseded because it was replaced. Either way the record stays: *what we ran, when, and why it stopped* is what the next incident will need, and the next incident is the moment nobody can reconstruct it.                                                  |

**Every stage has an exit.** *Approved* ends when the change is promoted or the approval lapses because what it was
granted against has moved. *Observed* ends in *Stabilized* or in a withdrawal decision — it never ends by attention
drifting elsewhere, which is the failure mode it exists to prevent. A promotion that cannot leave a stage is raised, not
left there.

**Staging promotion versus production promotion.** Both are promotions; only one is a commitment. **Staging promotion**
is an act of **verification** — being wrong there is the environment working as designed, it costs a finding, and it is
the last time that is true. **Production promotion** is an act of **exposure** — being wrong there is paid by a
professional, in their work, on their client's document, and the cost is not recovered by fixing it. They share a
mechanism and share nothing else. Treating them as one act because they look alike is how the caution that belongs to
the second gets spent on the first, and then feels excessive by the time it matters.

**Promotion is a governed decision, not a mechanical afterthought** — and, per §1, that is exactly what lets it be
routine.

---

## 6. Release Safety

Safety criteria for promotion. Each is a **question with an owner elsewhere** — this section neither answers them nor
lowers any bar; it requires that each has been answered before exposure.

| Criterion                           | What must be true, and who owns it                                                                                                                                                                                                                                                         |
|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Verification completed**          | The gates are green per [Definition of Test Complete](./TESTING_STRATEGY.md#14-definition-of-test-complete), and recent enough to describe what is actually being promoted.                                                                                                                |
| **Traceability preserved**          | What is being promoted can be walked back to the documents that granted it ([CONTRIBUTING §4](./CONTRIBUTING.md#4-repository-workflow)) — before it goes live, while the answer is free.                                                                                                   |
| **Security requirements satisfied** | No constraint is relaxed to ship ([SECURITY](../01-architecture/SECURITY.md#security-review-process)). A security concern **blocks**; it is never a risk weighed against a date.                                                                                                           |
| **Rollback path available**         | The way back is known and *believed* — or its absence is known, accepted, and recorded **before** promoting (§7).                                                                                                                                                                          |
| **No unresolved conflicts**         | No frozen-document contradiction is outstanding on what is being promoted ([CONTRIBUTING §8](./CONTRIBUTING.md#8-conflict-resolution)). Promoting over one silently decides it — and production then defends the decision.                                                                 |
| **Documentation aligned**           | The documents describe what is about to run ([IMPLEMENTATION_PLAN §9](./IMPLEMENTATION_PLAN.md#9-change-management)); [STATUS](./IMPLEMENTATION_STATUS.md) reflects what landed. Divergence discovered later is not a documentation bug; it is unreviewed behavior in production.          |
| **Review obligations discharged**   | Every review the change owed has returned ([CONTRIBUTING §7](./CONTRIBUTING.md#7-review-and-approval-rules)) — not the ones its author remembered, and not the most demanding one standing in for the rest.                                                                                |
| **Operational risk understood**     | What could go wrong, who would notice, and how ([§8](#8-deployment-observability), [ARCHITECTURE §14](../01-architecture/ARCHITECTURE.md#14-architecture-risks)). "Nothing should go wrong" is a prediction, not an understanding — and it is the one always made before the ones that do. |

**Safety is a precondition, not a judgement made at the moment of promoting.** Each criterion is decidable in advance
and
cheap to check then. Assessed *while* promoting — with the change ready, the reviewer waiting, and the release
announced — every one of them is answered by whoever wants to proceed, and every answer is yes.

---

## 7. Rollback Philosophy

**First, the term.** Three different things in this repository are called *rollback*, and conflating them is how the
wrong one gets planned for:

| Term                        | What it means                                                                                                                                                                                                                                                                |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Transaction rollback**    | A failed atomic unit reverting entirely, so no partial write is presented as success ([BACKEND_CODING_STANDARDS §9](./BACKEND_CODING_STANDARDS.md#9-transaction-guidelines), [DATABASE §11](../01-architecture/DATABASE.md#11-transaction-boundaries)). Not this document's. |
| **Migration reversibility** | A forward schema change having a clear path back ([Migration Strategy](../01-architecture/DATABASE.md#database-migration-strategy)). Not this document's, and it constrains this one.                                                                                        |
| **Deployment rollback**     | Withdrawing a promoted change from an environment. **This section, and only this.**                                                                                                                                                                                          |

**Rollback is a governed response, not an admission of failure.** A rollback is the system doing what it was designed to
do: converting an unknown into a known state at the lowest cost available. Treating it as a defeat is how teams talk
themselves into forward-fixing something they do not understand yet — under time pressure, on production, with the
evidence still arriving. The most expensive incidents are rarely the ones rolled back quickly; they are the ones
somebody was too invested to withdraw.

| Response                 | When it is right                                                                                                                                                                                                                                                       |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Quick rollback**       | The default when behavior is wrong and the cause is not yet understood. Understanding is worth more than continuity, and it is cheaper to acquire once nobody is being harmed while acquiring it.                                                                      |
| **Partial rollback**     | Where blast radius was contained well enough that only part must be withdrawn (§2). This is what minimizing exposure buys — a promotion that cannot be partially withdrawn is one that was never partially exposed.                                                    |
| **Forward fix**          | Legitimate **only** when the cause is understood, the fix is small, and it passes the same gates as any other change (§6). It is not a faster rollback; it is a new promotion made under worse conditions, and it is chosen on evidence — never on reluctance to undo. |
| **Neither is available** | A state to discover *before* promoting, never during (§6). A change that can be neither withdrawn nor safely fixed forward was a permanent decision, and it should have been reviewed as one.                                                                          |

**Rollback decision ownership.** The decision belongs to whoever is accountable for the change being live — named
*before* promotion, not sought during an incident. An unowned rollback decision defaults to the person least willing to
make it, which is usually the person whose change it is.

**Rollback traceability.** A rollback is a change to what is running and is recorded as one: what was withdrawn, when,
why, and what it reverted to ([ADR-015](../01-architecture/decisions/ADR-015-Observability.md),
[STATUS](./IMPLEMENTATION_STATUS.md)). An unrecorded rollback leaves the repository describing something that is no
longer running — the same drift as an unrecorded promotion, in the opposite direction and with less attention on it.

**Rollback after AI-related changes.** Withdrawing a prompt, provider, or retrieval change restores prior *behavior*,
not prior *outputs* — AI output is
non-deterministic ([TESTING_STRATEGY §7](./TESTING_STRATEGY.md#7-ai-testing-strategy)),
and what a professional already received, acted on, or sent does not roll back at all. The rollback restores the
version; it does not unsay what was said ([EVALUATION §7](../04-ai/EVALUATION.md#7-baselines-and-regression) is how the
behavioral difference is judged, and what is running is a fact its baselines depend on).

**Rollback after data-related changes.** Bounded by
the [Migration Strategy](../01-architecture/DATABASE.md#database-migration-strategy),
which requires that a rolling deployment tolerates both versions during a migration. **Code rolls back; data that has
already been written does not un-write itself.** This asymmetry is why schema change is incremental and reversible by
design there rather than by heroics here — and why a rollback path for a data change is confirmed before promotion (§6),
when it is still a design question instead of an emergency.

---

## 8. Deployment Observability

**The baseline is owned by [ADR-015](../01-architecture/decisions/ADR-015-Observability.md)** — structured logging,
health and error-rate visibility, correlation identifiers, AI metrics from **metadata only**, and that sensitive content
is never logged ([SECURITY §16](../01-architecture/SECURITY.md#16-logging-and-audit),
[AI_ARCHITECTURE §14](../01-architecture/AI_ARCHITECTURE.md#14-ai-observability)). This section adds no signal and
weakens no privacy constraint. It owns only **what a promotion is watched for, and what the watching is for**.

| Watched for                      | What it would tell you                                                                                                                                                                                                                                      |
|----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Service health**               | Whether the system is up and responding as expected. The easiest signal to collect and the least informative: healthy is not correct, and the worst releases are usually healthy.                                                                           |
| **Behavior drift**               | Whether the product is doing what its documents say it does. This is what a promotion most plausibly broke, and it is invisible to health checks — nothing errors when a change quietly does the wrong thing correctly.                                     |
| **AI output quality regression** | Whether AI behavior moved against what it was approved on ([EVALUATION §7](../04-ai/EVALUATION.md#7-baselines-and-regression)). It degrades without failing: no error, no alert, output that still reads well. A drop is a review trigger, not a curiosity. |
| **Security anomalies**           | Whether anything is behaving in a way the threat model did not anticipate ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)). These are the findings that never age well and never wait for a convenient moment.                               |
| **Verification failures**        | Whether something green before promotion is failing after it. This is the most valuable signal available, because it says the *verification* was wrong — and everything downstream of it was trusted on that basis.                                         |
| **User-facing issues**           | Whether a professional's work is affected. The signal that matters most and arrives last: by the time it is visible here, the cost has already been paid by the person the product exists to help.                                                          |
| **Release stability**            | Whether *this* release is behaving, attributably — not whether the system is broadly fine. Stability that cannot be attributed to a release cannot inform a decision about that release (§5).                                                               |

**Observability supports decisions; it is not monitoring for its own sake.** Each signal above exists to answer one of
exactly two questions: *do we widen exposure, or do we withdraw?* (§5, §7). A signal nobody would act on is not
observability — it is a dashboard, and dashboards are how a team feels informed while the decision goes unmade. If a
promotion's signals cannot distinguish those two outcomes, the promotion cannot be observed, and it should not be
expanded on the grounds that nothing looked wrong.

---

## 9. Deployment Checklist

Every promotion is assessed against this checklist before it happens. A "no" is a finding to resolve, not a detail to
defer.

> This is **not** the [Definition of Done](./IMPLEMENTATION_PLAN.md#7-definition-of-done) — that asks whether the *work*
> is complete, and it is the gate to **merge**. This asks whether the change is safe to make **live**. Both must pass;
> neither substitutes for the other.

- [ ] **Change approved?** — Granted by its document, with every owed review discharged
  ([CONTRIBUTING §7](./CONTRIBUTING.md#7-review-and-approval-rules)).
- [ ] **Owning documents updated?** — They describe what is about to run
  ([IMPLEMENTATION_PLAN §9](./IMPLEMENTATION_PLAN.md#9-change-management)).
- [ ] **Tests passed?** — Green per [Definition of Test Complete](./TESTING_STRATEGY.md#14-definition-of-test-complete),
  recently enough to describe what is being promoted (§6).
- [ ] **Security implications reviewed?** — No constraint relaxed to ship; no security concern outstanding
  ([SECURITY](../01-architecture/SECURITY.md#security-review-process)).
- [ ] **AI implications reviewed?** — Prompt, provider, or retrieval changes approved by their own processes, and their
  evaluation obtained where the change warranted it (
  §4, [EVALUATION](../04-ai/EVALUATION.md#evaluation-review-process)).
- [ ] **Release impact understood?** — What this delivers, and what it changes for someone already using the product
  ([`docs/05-releases/`](../05-releases/) where relevant).
- [ ] **Rollback path known?** — The way back is identified and believed, its owner named — or its absence is recorded
  and accepted (§7).
- [ ] **Observability considered?** — The signals that would reveal this promotion was wrong exist, and someone would
  act
  on them (§8).
- [ ] **Traceability preserved?** — What is running can be walked back to what granted it, afterward and under pressure
  (§6).
- [ ] **Safe to promote?** — Every criterion in §6 holds, and no conflict is unresolved
  ([CONTRIBUTING §8](./CONTRIBUTING.md#8-conflict-resolution)).

---

## Deployment Review Process

> *Unnumbered governance section. It defines when deployment governance itself is reviewed — not when an individual
> release is approved, which is §6. It is what catches a promotion path that has quietly changed.*

**Review triggers** — a deployment review is required when any of the following occurs:

- **A new deployment path** — any way for a change to reach an environment that did not exist before. An unreviewed path
  is an unreviewed gate.
- **An environment change** — anything altering what an environment is, or how closely it resembles production
  ([TESTING_STRATEGY §13](./TESTING_STRATEGY.md#13-test-environments), §3).
- **A promotion rule change** — a gate added, removed, reordered, or made conditional.
- **A rollback rule change** — anything altering what can be withdrawn, or by whom (§7).
- **A security concern** — anything touching secrets, exposure, or the constraints deployment operates inside
  ([SECURITY](../01-architecture/SECURITY.md#10-ai-security)).
- **A release-impacting change** — anything altering what a release contains or claims
  ([`docs/05-releases/`](../05-releases/)).
- **An AI-related change** — a prompt, provider, model, or retrieval change reaching an environment (§4).
- **A database-related change** — any migration, per the
  [Migration Strategy](../01-architecture/DATABASE.md#database-migration-strategy).
- **An architecture change** — the structure deployed, the scaling path, or the topology
  ([ADR-012](../01-architecture/decisions/ADR-012-Deployment-Strategy.md)).
- **A documentation mismatch** — what runs and what the documents describe have diverged, however discovered. This is
  drift already realized, not a risk of it.

**Review outcomes** — each review resolves to exactly one:

- **Approved** — the deployment governance is sound as proposed; the decision and rationale are recorded.
- **Refinement required** — the intent is sound but something must change first: an unstated rollback path, a gate that
  is conditional in practice, an environment whose divergence is unmeasured.
- **Architecture review required** — it implies a change to the structure, the scaling path, or the topology; raised per
  [CLAUDE.md §8](../../CLAUDE.md) and routed to the
  [Review Process](./IMPLEMENTATION_PLAN.md#review-process)'s architecture triggers.
- **Security review required** — routed to the
  [Security Review Process](../01-architecture/SECURITY.md#security-review-process). This outcome **blocks**.
- **AI review required** — routed to the owning AI process
  ([CONTRIBUTING §7](./CONTRIBUTING.md#7-review-and-approval-rules)).
- **Testing review required** — it bears on what must be verified or where; routed to the
  [Test Review Process](./TESTING_STRATEGY.md#test-review-process). **A gate is never relaxed by a deployment decision.
  **
- **Release review required** — it bears on what a release contains or claims; routed to the release documents
  ([`docs/05-releases/`](../05-releases/), **not yet authored** — while they do not exist, such a change owes no
  separate
  release review and waits for none; it owes the reviews its content already owed).
- **ADR required** — the decision is significant, precedent-setting, or hard to reverse
  ([CLAUDE.md §8](../../CLAUDE.md)).

Only **Approved** permits the governance change to take effect; **Refinement required** returns it to its author; every
other outcome holds it until the review it was routed to returns. A change held there is not approved by the passage of
time, and a review that was owed is not discharged by a release being urgent.

**Synchronization.** Deployment governance MUST remain synchronized with [CLAUDE.md](../../CLAUDE.md),
[IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md), [TESTING_STRATEGY](./TESTING_STRATEGY.md),
[SECURITY](../01-architecture/SECURITY.md), [CONTRIBUTING](./CONTRIBUTING.md), the release documents, and the relevant
[ADRs](../01-architecture/decisions/) — in particular
[ADR-012](../01-architecture/decisions/ADR-012-Deployment-Strategy.md), which owns the topology this document never
names. When this document and one of them disagree, **they win** and this document is corrected
([CLAUDE.md §3](../../CLAUDE.md)). A change touching more than one is reviewed by each document it touches and MUST NOT
be merged into one while leaving another contradicting it.

---

## 10. Deployment Decision Summary

The load-bearing decisions behind deployment governance, recorded so they are not silently reversed. The **hosting
topology** ([ADR-012](../01-architecture/decisions/ADR-012-Deployment-Strategy.md)), the **verification environments and
gates** ([TESTING_STRATEGY](./TESTING_STRATEGY.md)), the **merge gate**
([IMPLEMENTATION_PLAN §7](./IMPLEMENTATION_PLAN.md#7-definition-of-done)), the **migration rules**
([DATABASE](../01-architecture/DATABASE.md#database-migration-strategy)), and the **observability baseline**
([ADR-015](../01-architecture/decisions/ADR-015-Observability.md)) are decided elsewhere and are not restated here.

| Decision                                         | Chosen Approach                                                                                 | Alternatives                                                                          | Rationale                                                                                                                                                                                                                                                                  |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Governed promotion**                           | Promotion is a decision with preconditions, made before the act                                 | Treat promotion as the mechanical last step of a green build                          | The gates are what let deployment be *routine, not an event* ([IMPLEMENTATION_PLAN §6](./IMPLEMENTATION_PLAN.md#6-development-workflow)) — deliberation done in advance and encoded. Ceremony is what replaces gates that were never load-bearing (§1, §5).                |
| **Safety-first deployment**                      | Every safety criterion is decidable and decided **before** exposure                             | Assess risk at promotion time, when the full picture is available                     | At promotion time the change is ready, the reviewer is waiting, and the release is announced. Every criterion assessed then is answered by whoever wants to proceed, and every answer is yes (§6).                                                                         |
| **Rollback as a first-class concern**            | A way back is designed, owned, and confirmed before promoting; rollback is a normal response    | Roll forward by default; treat rollback as an exceptional failure path                | A rollback path is decided before it is needed or it does not exist. Treating withdrawal as defeat is how teams forward-fix what they do not yet understand, under pressure, on production — which is how the expensive incidents are made (§7).                           |
| **Environment separation**                       | Environments are isolated, and none borrows production's data, credentials, or trust            | Share resources between environments where convenient                                 | Separation is what keeps the cost of a mistake proportional to how far it travelled. An environment that borrows production's trust has removed that protection while still appearing to provide it — the confidence remains and the safety does not (§3).                 |
| **Comparability is a property to defend**        | Divergence between staging and production is a finding                                          | Accept drift as normal and rely on production monitoring to catch what staging misses | Verification predicts production only as far as they resemble each other. As they drift, the tests keep passing and stop meaning anything — and nothing announces the moment the verdict became worthless (§3, §8).                                                        |
| **Release traceability**                         | What is running can be walked back to what granted it, and to the promotion that made it live   | Rely on version history and reconstruct after an incident                             | "What changed?" is the first question of every incident and the only one that matters in the first minutes. It is cheap to record while promoting and answered by archaeology afterward — slowly, and sometimes wrongly (§6, §7).                                          |
| **Observe before expanding**                     | Exposure widens only on evidence of stability, never on absence of visible failure              | Promote fully and monitor; widen on schedule                                          | Bad releases are indistinguishable from good ones for the first few minutes. Expanding because nothing has *visibly* broken is deciding without the evidence that was about to arrive — and the blast radius is the only variable still under control (§5, §8).            |
| **Deployment does not invent behavior**          | Nothing is first decided at promotion; a question that surfaces there is raised, not resolved   | Let deployment settle small ambiguities pragmatically                                 | Deployment is the last gate and the one under the most pressure, which makes it the most tempting place to settle what nobody settled earlier. Behavior first decided there has no home, no review, and no reader ([CLAUDE.md §8](../../CLAUDE.md), Rules).                |
| **Deployment follows approval, not speculation** | Promotion delivers what was approved; "see how it behaves in production" is not a plan          | Ship uncertain changes to real users to learn faster                                  | That is a decision to let a professional perform the review, on their client's document, without being asked. Where a question is genuinely open it is raised — the product's promise is confidentiality and trust, and neither survives being used as a test (§2, Rules). |
| **The topology is not this document's**          | The platforms and topology are named nowhere here; this document governs the *act* of promoting | Describe the deployment landscape here so contributors have one place to look         | [ADR-012](../01-architecture/decisions/ADR-012-Deployment-Strategy.md) owns it, and a copy would be a second place to update when it changes — with no rule saying which is right. A topology change warrants an updated ADR, not an edit here (§4).                       |

---

*This document governs how a verified LedgerAI change becomes live — how it is promoted, observed, and withdrawn; it
does not override the frozen documents under [`docs/`](../), and it grants no product behavior. It begins where
[IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md)'s merge gate ends, consumes the verdict
[TESTING_STRATEGY](./TESTING_STRATEGY.md) produces, operates inside the constraints
[SECURITY](../01-architecture/SECURITY.md) sets and the topology
[ADR-012](../01-architecture/decisions/ADR-012-Deployment-Strategy.md) fixes, assumes the routing
[CONTRIBUTING](./CONTRIBUTING.md) requires has already happened, and records what it does where
[IMPLEMENTATION_STATUS](./IMPLEMENTATION_STATUS.md) and the release documents can be trusted. When a promotion would
require new behavior, a relaxed constraint, or a changed topology, stop and raise it per
[CLAUDE.md §8](../../CLAUDE.md) — production is where this repository's claims stop being claims.*
