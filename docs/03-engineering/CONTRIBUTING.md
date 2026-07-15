# Contributing — LedgerAI

> **Status:** Draft v1
> **Owner:** Principal Engineering Governance Lead
> **Last updated:** 2026-07-16
> **Upstream (frozen):
** [CLAUDE.md](../../CLAUDE.md) · [IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md) · [PRODUCT_DECISIONS](../00-product/PRODUCT_DECISIONS.md) · [ARCHITECTURE](../01-architecture/ARCHITECTURE.md)
> **Related:
** [IMPLEMENTATION_STATUS](./IMPLEMENTATION_STATUS.md) · [TESTING_STRATEGY](./TESTING_STRATEGY.md) · [BACKEND_CODING_STANDARDS](./BACKEND_CODING_STANDARDS.md) · [FRONTEND_CODING_STANDARDS](./FRONTEND_CODING_STANDARDS.md)

---

## 1. Purpose

### Why this document exists

A contributor arrives with an intention, not a ticket. They know what they want to change; what they do not yet know is
**whose decision it is** — and by the time that question surfaces in review, the change is already written, already
defended, and already expensive to route correctly.

**The build path is already settled.** [CLAUDE.md §5](../../CLAUDE.md) defines the implementation workflow;
[IMPLEMENTATION_PLAN §6](./IMPLEMENTATION_PLAN.md#6-development-workflow) defines the development workflow and
[§7](./IMPLEMENTATION_PLAN.md#7-definition-of-done) the Definition of Done;
[§9](./IMPLEMENTATION_PLAN.md#9-change-management) defines which document is updated when the source of truth changes;
[CLAUDE.md §3](../../CLAUDE.md) settles which document wins when two disagree. **This document adds no workflow, no
Definition of Done, and no precedence rule**, and it restates none of them.

Both of those workflows begin at the same place: *a requirement that already exists in a frozen document*. What no
document owns is the step **before** that — **routing**: given an intention, which document owns it, does it have a home
at all, and what does a contributor owe before writing anything? And neither workflow covers the changes that are not
features: a documentation correction, a terminology fix, a cross-reference repair, a release note, a maintenance edit.
Those are most of the commits a repository accumulates, and they are where authority quietly drifts.

The governing principle of this document:

> **Every change belongs to a document before it belongs to a branch.**
>
> A change whose owner was never identified does not become unowned — it becomes owned by whoever wrote it, in a place
> nobody thinks to look, and the repository learns a rule that no document contains.

It is **not** a product, architecture, design, or AI document, **not** a coding standard, and **not** a build guide. It
contains **no code, no tooling instructions, no git commands, no CI configuration**, and no product, architecture,
design, or AI specifics. It governs *how a change is routed, reviewed, documented, and merged without contradicting a
frozen document* — never what the change should say.

### The relationship in one line

> **The frozen documents define what the repository is. This document defines how contributors change it.** Any change
> that conflicts with a frozen document is routed to the owning document **first** — the code never wins an argument
> with its source of truth ([CLAUDE.md §2](../../CLAUDE.md)).

### Relationship to the governing documents

| Document                                                                                                                      | Its job                                     | The boundary with this document                                                                                                                                                                                                                                                                                                                                                                                              |
|-------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [CLAUDE.md](../../CLAUDE.md)                                                                                                  | **The engineering playbook**                | It owns the **documentation hierarchy** and **conflict resolution** (§3), the **Engineering Rules** (§4), the **implementation workflow** (§5), the **stop-and-ask conditions** (§8), and the **Definition of Complete** (§9). It binds Claude specifically; this document generalizes the same discipline to **every** contributor and adds nothing that contradicts it. Where they appear to disagree, **CLAUDE.md wins**. |
| [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)                                                                            | **Build order and the merge gate**          | It owns the **development workflow** ([§6](./IMPLEMENTATION_PLAN.md#6-development-workflow)), the **Definition of Done** ([§7](./IMPLEMENTATION_PLAN.md#7-definition-of-done)), **Change Management** ([§9](./IMPLEMENTATION_PLAN.md#9-change-management)), and the **Review Process**. This document governs the step **before** its workflow starts — routing — and hands off. It adds no stage and no gate.               |
| [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md)                                                                        | **Live execution state**                    | The one document expected to change routinely ([CLAUDE.md §3](../../CLAUDE.md)). This document requires that it is updated as work lands; it does not define what it records.                                                                                                                                                                                                                                                |
| [PRODUCT_VISION.md](../00-product/PRODUCT_VISION.md) · [PRD.md](../00-product/PRD.md)                                         | **Why the product exists; what it does**    | They own purpose, scope, and [boundaries](../00-product/PRODUCT_DECISIONS.md#2-product-boundaries). A contribution that would add, cut, or redefine product behavior is **theirs to decide first** — it is not a contribution decision that happens to touch product ([SRS](../00-product/SRS.md) carries the precise behavior).                                                                                             |
| [ARCHITECTURE.md](../01-architecture/ARCHITECTURE.md)                                                                         | **System design**                           | It owns module boundaries, dependency direction, and style; the [ADRs](../01-architecture/decisions/) own ratified decisions. A contribution that would alter one is an architecture change routed there — never a refactor that happens to move a boundary.                                                                                                                                                                 |
| [DESIGN_SYSTEM.md](../02-design/DESIGN_SYSTEM.md) · [UI_GUIDELINES.md](../02-design/UI_GUIDELINES.md)                         | **The visual language and its application** | With [COMPONENTS](../02-design/COMPONENTS.md) and [USER_FLOWS](../02-design/USER_FLOWS.md), they own what the product looks like, how it is assembled, what it says, and how it behaves. A contribution that would introduce appearance, tone, or flow that no design document grants is routed there.                                                                                                                       |
| The AI documents                                                                                                              | **How AI is built and governed**            | [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) owns AI behavior and orchestration; [AI_PROVIDERS](../04-ai/AI_PROVIDERS.md) providers; [PROMPTS](../04-ai/PROMPTS.md) prompts; [EVALUATION](../04-ai/EVALUATION.md) quality judgment; [RAG](../04-ai/RAG.md) retrieval, if it ever exists. Each has its **own review process**, and this document routes to them (§7) rather than duplicating any.                 |
| [TESTING_STRATEGY.md](./TESTING_STRATEGY.md)                                                                                  | **How the product is verified**             | It owns what is tested, at which level, and [Definition of Test Complete](./TESTING_STRATEGY.md#14-definition-of-test-complete). This document requires that tests accompany a change; **what** that means is entirely its.                                                                                                                                                                                                  |
| [BACKEND_CODING_STANDARDS.md](./BACKEND_CODING_STANDARDS.md) · [FRONTEND_CODING_STANDARDS.md](./FRONTEND_CODING_STANDARDS.md) | **How code is written**                     | They own structure, naming, layering, and their own review checklists. This document governs how a change is **routed and approved**, never how it is written. A review finding about code quality is theirs; a finding that a change had no owning document is this document's.                                                                                                                                             |

### What this document does not decide

To be unambiguous, because a contribution document is the most tempting place to accumulate authority it was never
given. This document does **not** define: the implementation workflow (CLAUDE.md §5), the development workflow or
Definition of Done ([IMPLEMENTATION_PLAN §6–§7](./IMPLEMENTATION_PLAN.md#6-development-workflow)), which document to
update when the source of truth changes ([§9](./IMPLEMENTATION_PLAN.md#9-change-management)), document precedence
(CLAUDE.md §3), coding standards, test expectations, or any product, architecture, design, or AI behavior.

It defines **routing, traceability, and the contributor's obligations around a change** — and nothing else.

---

## 2. Contribution Philosophy

These principles explain *why* contribution is governed the way it is. They are the reasoning behind the enforceable
rules that follow.

| Principle                                         | Why it exists                                                                                                                                                                                                                                                                      |
|---------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Preserve the source of truth**                  | The frozen documents are the only reason this repository means anything specific ([CLAUDE.md §2](../../CLAUDE.md)). A change that contradicts one does not create a disagreement — it creates a repository where the documentation is quietly wrong, and nobody knows which parts. |
| **Change deliberately**                           | Every change is a decision, including the ones that felt like cleanup. The dangerous change is never the one someone agonized over; it is the one nobody noticed they were making.                                                                                                 |
| **Review before merge**                           | Review is the only gate that catches what tooling cannot: a rule with no home, an authority quietly claimed, a boundary crossed. Nothing else in the repository is looking for those.                                                                                              |
| **Minimize drift**                                | Drift is not an event; it is an accumulation. Each step is individually defensible, and the sum is a system nobody designed — which is why it is never caught by asking whether *this* change is reasonable.                                                                       |
| **Traceability over convenience**                 | The cheapest moment to record why a change was made is while making it; the most expensive is eighteen months later, when the person is gone and the reason mattered. Convenience discounts a cost the repository pays with interest.                                              |
| **One owner per decision**                        | Two documents answering the same question is not redundancy — it is a future contradiction with a delivery date. The moment they disagree, both are cited, and the reader has no way to choose.                                                                                    |
| **Small, reviewable changes**                     | Review quality collapses with size ([IMPLEMENTATION_PLAN §2](./IMPLEMENTATION_PLAN.md#2-engineering-principles)). A large change is not reviewed more carefully because it matters more; it is approved by exhaustion, and its worst part is the part nobody reached.              |
| **Synchronize documentation with implementation** | Code and its governing document never diverge ([CLAUDE.md §7](../../CLAUDE.md)). A document that no longer describes the system is worse than none: it is trusted, and it is wrong, and both at once.                                                                              |
| **Respect frozen boundaries**                     | The [boundaries](../00-product/PRODUCT_DECISIONS.md#2-product-boundaries) are what LedgerAI decided **not** to be. They are crossed by useful, reasonable additions — never by obviously bad ones — which is exactly why they need a rule rather than judgment.                    |
| **Prefer explicit decisions over implied ones**   | An implied decision cannot be found, reviewed, or reversed; it is discovered later by someone reading code and inferring intent that was never formed. Explicitness is what makes a decision revisitable instead of archaeological.                                                |

---

## Contribution Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Every change MUST have a clear purpose.** *A change that cannot say what it is for cannot be reviewed against
  anything — the reviewer is reduced to checking whether it looks fine, which is how everything that looks fine gets
  merged.*
- **Every change MUST respect the frozen documents.** *They are the source of truth ([CLAUDE.md §2](../../CLAUDE.md)).
  A change that contradicts one is not a disagreement to be settled in the diff; it is routed to the owning document
  first, or it does not proceed (§8).*
- **Contributors MUST NOT introduce new requirements, architecture, or design by accident.** *The accidental kind is the
  common kind. Nobody sets out to add a requirement — they add a reasonable behavior, and the product now does something
  no document grants and no one can find ([CLAUDE.md §8](../../CLAUDE.md)).*
- **Changes that touch product behavior MUST be traced to the owning product documents.** *Behavior lives in
  [PRD](../00-product/PRD.md) and [SRS](../00-product/SRS.md). Behavior implemented but never documented is a
  requirement with no home — untestable, unfindable, and permanent.*
- **Changes that touch architecture MUST be traced to the owning architecture or ADR documents.** *Boundaries erode
  through changes that each look local ([ARCHITECTURE](../01-architecture/ARCHITECTURE.md)). An architectural decision
  made in a pull request is still an architectural decision; it is simply unrecorded.*
- **Changes that touch design MUST be traced to the owning design documents.** *Appearance is inherited, not invented
  ([DESIGN_SYSTEM](../02-design/DESIGN_SYSTEM.md)). One screen's exception is the product's second visual language, and
  it arrives looking like a small improvement.*
- **Changes that touch AI behavior, prompts, evaluation, or retrieval MUST be traced to the owning AI documents.** *AI
  behavior changes without a signature to break ([PROMPTS](../04-ai/PROMPTS.md)) — no type objects, no test necessarily
  fails, and the output still reads well. Its documents are the only gate it has.*
- **Changes SHOULD be small and reviewable.** *Small is not a courtesy to the reviewer; it is the condition under which
  review finds anything at all. Where a change cannot be small, it is at least **one concern** (§5).*
- **Documentation changes MUST accompany behavior changes where relevant.** *Documentation written "after" is
  documentation written never, or written by someone reconstructing intent from code. The change and its record are one
  unit, or they are already diverging ([IMPLEMENTATION_PLAN §9](./IMPLEMENTATION_PLAN.md#9-change-management)).*
- **Conflicting interpretations MUST be escalated before merge.** *A contributor who picks the convenient reading has
  not resolved the conflict — they have hidden it, and the repository now contains two truths and one silent vote
  (§8, [CLAUDE.md §8](../../CLAUDE.md)).*

**Why these rules exist.** A repository does not decay through bad changes; it decays through good ones that nobody
routed. The three failure modes are **repo drift** (the system and its documents describe different products, each
change individually reasonable), **hidden authority** (a rule that lives only in code, a comment, or a convention, and
is enforced by habit rather than decision), and **unreviewed behavior change** (behavior that arrived without anyone
deciding it should).

Each is invisible at the moment it happens. Nobody experiences drift as drift; they experience a small, sensible edit.
Nobody announces that they are claiming authority; they add a helpful default. These rules exist because the question
"who decided this?" must have an answer *while the answer still exists* — and the only reliable moment to establish it
is before the change is written.

---

## 3. Contribution Scope

**What this document governs.** Every change to this repository, routed by what it touches. The categories below are
**routing destinations**, not descriptions of the work — what each domain decides is entirely its own document's.

| Contribution type         | Routed to                                                                                                                                                                                                                                                                                 |
|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Product changes**       | [PRD](../00-product/PRD.md) and [SRS](../00-product/SRS.md), within [Vision](../00-product/PRODUCT_VISION.md) and the [boundaries](../00-product/PRODUCT_DECISIONS.md#2-product-boundaries). Scope decisions precede implementation, never follow it.                                     |
| **Architecture changes**  | [ARCHITECTURE](../01-architecture/ARCHITECTURE.md), the domain specs ([DATABASE](../01-architecture/DATABASE.md), [API_SPEC](../01-architecture/API_SPEC.md), [SECURITY](../01-architecture/SECURITY.md)), and an [ADR](../01-architecture/decisions/) where the decision is significant. |
| **Design changes**        | [DESIGN_SYSTEM](../02-design/DESIGN_SYSTEM.md), [UI_GUIDELINES](../02-design/UI_GUIDELINES.md), [COMPONENTS](../02-design/COMPONENTS.md), [USER_FLOWS](../02-design/USER_FLOWS.md).                                                                                                       |
| **AI changes**            | [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) and the AI documents — [AI_PROVIDERS](../04-ai/AI_PROVIDERS.md), [PROMPTS](../04-ai/PROMPTS.md), [EVALUATION](../04-ai/EVALUATION.md), [RAG](../04-ai/RAG.md).                                                                   |
| **Engineering changes**   | [BACKEND_CODING_STANDARDS](./BACKEND_CODING_STANDARDS.md), [FRONTEND_CODING_STANDARDS](./FRONTEND_CODING_STANDARDS.md), and [IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md) for build order and the merge gate.                                                                           |
| **Test changes**          | [TESTING_STRATEGY](./TESTING_STRATEGY.md). A change to *what is verified* is a testing decision, not a side effect of a feature.                                                                                                                                                          |
| **Documentation changes** | The owning document. **A documentation change that alters meaning is a change to what the repository decided** — governed exactly as the decision would be (§6), never as an edit.                                                                                                        |
| **Release changes**       | The release documents under [`docs/05-releases/`](../05-releases/). A release records what shipped; it never becomes the place a behavior is first described.                                                                                                                             |
| **Maintenance changes**   | The owning document, if the change touches meaning; otherwise the standards. Maintenance is where "while I was in here" lives — the most common way an unrelated decision arrives unnoticed (§5).                                                                                         |

**What is outside scope.** This document routes; it does not decide. **What** any document above decides is that
document's own — routing a change to its owner never settles what the owner should say, and a contributor who reads a
routing destination as an answer has misread this document. The governance artifacts it defers to rather than defines —
the workflows, the Definition of Done, Change Management, and document precedence — are named in §1 and not re-listed
here.

---

## 4. Repository Workflow

> **This section defines no workflow.** [CLAUDE.md §5](../../CLAUDE.md) and
> [IMPLEMENTATION_PLAN §6](./IMPLEMENTATION_PLAN.md#6-development-workflow) own the path a change follows, and a third
> description of the same path would be a third thing to keep in sync and a second answer to one question. What follows
> is the step that happens **before** either of them begins — and the handoff.

Both frozen workflows start from a requirement that **already exists** in a frozen document. Routing is how a change
gets to that starting line, or discovers that it cannot.

| Step                              | What it means                                                                                                                                                                                                                                                           |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Identify the change**           | State what would be different afterward, in terms of behavior, structure, appearance, or meaning — not in terms of the edit. "Refactor the client service" names an action; it does not say whether the product changes.                                                |
| **Determine the owning document** | Ask **whose decision is this?** before asking how to make it (§3). A change with no owning document has not found a gap in the documentation; it has found an undocumented requirement, and that is a stop ([CLAUDE.md §8](../../CLAUDE.md)).                           |
| **Update the source of truth**    | If the owning document does not already grant the change, it is updated **first** — with the approval that document requires ([IMPLEMENTATION_PLAN §9](./IMPLEMENTATION_PLAN.md#9-change-management)). Implementing first and documenting after inverts the authority.  |
| **Hand off to the build path**    | Once the change is granted by its document, it is a documented requirement, and the frozen workflows take over: [CLAUDE.md §5](../../CLAUDE.md) and [IMPLEMENTATION_PLAN §6](./IMPLEMENTATION_PLAN.md#6-development-workflow). **This document adds no stage to them.** |
| **Review the impact**             | Identify every document the change touches, not only the one it started in — and owe each of them their review (§7). A change that touches three domains is three reviews, not the one its author was thinking about.                                                   |
| **Verify traceability**           | The change can be walked back to the document that grants it, and that document can be walked forward to the change (§6). A link that exists in only one direction is not traceability; it is a coincidence.                                                            |
| **Merge only after approval**     | Against the gate [IMPLEMENTATION_PLAN §7](./IMPLEMENTATION_PLAN.md#7-definition-of-done) owns, plus every review the change owed (§7). Merging is the moment the repository adopts the change as its own; nothing is adopted on the strength of being finished.         |

**The workflow is driven by ownership, not convenience.** The order above is not a preference: updating the source of
truth *after* implementing produces a document written to match code — which is not a decision, it is a transcript, and
it will be defended as though it were a decision. The expensive step is always the one skipped because the change
"obviously" belonged somewhere; obviousness is what routing exists to check.

---

## 5. Branch and Pull Request Expectations

> **The branching model is not this document's.** How branches are named and where changes integrate is described
> outside this document; [IMPLEMENTATION_PLAN §6](./IMPLEMENTATION_PLAN.md#6-development-workflow) owns the merge
> discipline and [§2](./IMPLEMENTATION_PLAN.md#2-engineering-principles) the always-deployable expectation. This section
> states only the **governance** expectations that make a change reviewable — no tooling, no commands, no host
> specifics. Where the branching model and a frozen document appear to disagree, that is a conflict, and it is
> reconciled per §8 before either is relied on.

- **One concern per pull request, where practical.** *Concern*, not size: a change may be large and still be one
  decision. Two decisions in one request means the reviewer approves both to approve either, and the second one is never
  the one they were reading for.
- **Keep diffs focused.** Unrelated cleanup, renames, and reformatting hide the change inside noise. A reviewer who
  cannot see the decision cannot review it — and the larger the diff, the more confidently it is approved.
- **Include the relevant documents when a change crosses a boundary.** A change and its governing document are **one
  unit** ([IMPLEMENTATION_PLAN §9](./IMPLEMENTATION_PLAN.md#9-change-management)). Split across requests, one of them
  merges alone, and the repository is inconsistent in the interval — an interval that is never as short as intended.
- **Avoid bundling unrelated work.** "While I was in here" is how a decision nobody requested arrives inside a change
  everybody wanted. It is approved as part of something else, and it is discovered by whoever it breaks.
- **Do not merge speculative changes without approval.** Code kept because it might be needed is a commitment nobody
  made, and it accretes: it is maintained, tested, and eventually depended on. Speculation that matters is raised as a
  decision ([CLAUDE.md §8](../../CLAUDE.md)), not merged as an option.

---

## 6. Documentation Update Rules

**Documentation is part of the change, not a step after it.** A change is not "done and then documented" — the record is
what makes the change a *decision* rather than an event that happened to the code. Written afterward, it is
reconstruction: someone infers intent from a diff and writes it down with more confidence than they have.

**Which document to update, and when, is owned by
[IMPLEMENTATION_PLAN §9](./IMPLEMENTATION_PLAN.md#9-change-management)** — this section does not restate that table. It
governs the **discipline** of the update itself:

| Rule                                                     | Why                                                                                                                                                                                                                                                               |
|----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Frozen documents are updated before downstream ones**  | Downstream documents derive their authority from upstream ones ([CLAUDE.md §3](../../CLAUDE.md)). Updated in the wrong order, the downstream document is briefly the source of truth — and whoever reads it during that window is reading a decision nobody made. |
| **Ownership stays aligned with behavior**                | The document that decides a thing is the document that describes it. When behavior moves and its description does not, the repository keeps two answers: one that is authoritative and one that is true.                                                          |
| **Code and documents never diverge silently**            | Divergence discovered later is not a documentation bug; it is an unreviewed behavior change that has been in production ([CLAUDE.md §7](../../CLAUDE.md)). The silence is the failure — the divergence is just its evidence.                                      |
| **Cross-references are updated when their target moves** | A stale reference is worse than a missing one: it resolves, it looks authoritative, and it points somewhere that no longer says what it is cited for. Nobody checks a link that works.                                                                            |
| **Duplicate authority is not introduced**                | Two documents deciding one thing is a contradiction that has not happened yet. If a document needs to state another's rule, it **references** it (§8) — restating is how the copy drifts and both get cited.                                                      |
| **Terminology stays consistent**                         | A synonym for a defined term is a second concept, whether or not one was intended. Readers infer that different words mean different things — and they are usually right, which is exactly the problem when they are not.                                         |

**A documentation-only change can be a behavior change.** Editing what a document *decides* changes the repository as
surely as editing code, and it is reviewed as the decision it is (§7) — not as an edit. The most dangerous change in
this repository is a clarifying rewording that quietly settles a question the original left open.

---

## 7. Review and Approval Rules

**This document creates no review process.** Each domain owns its own, and each defines its own triggers and outcomes.
What this section owns is **routing**: identifying every review a change owes, so that none is discovered after merge.

**A change owes a review to every document it touches** — not only the one it started in. This is the routing table:

| If the change touches…                                 | It owes this review                                                                                                                                                                                                                                                                                                                                                                                                                          |
|--------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Product scope, requirements, or behavior**           | Product approval per [IMPLEMENTATION_PLAN §9](./IMPLEMENTATION_PLAN.md#9-change-management); [PRD](../00-product/PRD.md) and [SRS](../00-product/SRS.md) are updated before the code that depends on them.                                                                                                                                                                                                                                   |
| **Architecture, boundaries, or the API/DB contracts**  | [Review Process](./IMPLEMENTATION_PLAN.md#review-process) — architecture, database, and API triggers; an [ADR](../01-architecture/decisions/) where the decision is significant.                                                                                                                                                                                                                                                             |
| **Security, authorization, secrets, or data exposure** | [Security Review Process](../01-architecture/SECURITY.md#security-review-process). This review **blocks**.                                                                                                                                                                                                                                                                                                                                   |
| **AI behavior, grounding, or orchestration**           | [AI Review Process](../01-architecture/AI_ARCHITECTURE.md#ai-review-process).                                                                                                                                                                                                                                                                                                                                                                |
| **An AI provider**                                     | [Provider Review Process](../04-ai/AI_PROVIDERS.md#provider-review-process).                                                                                                                                                                                                                                                                                                                                                                 |
| **A prompt**                                           | [Prompt Review Process](../04-ai/PROMPTS.md#prompt-review-process).                                                                                                                                                                                                                                                                                                                                                                          |
| **AI quality, baselines, or findings**                 | [Evaluation Review Process](../04-ai/EVALUATION.md#evaluation-review-process).                                                                                                                                                                                                                                                                                                                                                               |
| **Retrieval** *(if it ever exists)*                    | [Retrieval Review Process](../04-ai/RAG.md#retrieval-review-process) — noting retrieval is deferred ([DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)).                                                                                                                                                                                                                                                                     |
| **The visual language**                                | [Design Review Process](../02-design/DESIGN_SYSTEM.md#design-review-process).                                                                                                                                                                                                                                                                                                                                                                |
| **A component's contract**                             | [Component Review Process](../02-design/COMPONENTS.md#component-review-process).                                                                                                                                                                                                                                                                                                                                                             |
| **Screen application, tone, or microcopy**             | [UI Review Process](../02-design/UI_GUIDELINES.md#ui-review-process).                                                                                                                                                                                                                                                                                                                                                                        |
| **A user flow**                                        | [User Flow Review Process](../02-design/USER_FLOWS.md#user-flow-review-process).                                                                                                                                                                                                                                                                                                                                                             |
| **What or how the product is verified**                | [Test Review Process](./TESTING_STRATEGY.md#test-review-process).                                                                                                                                                                                                                                                                                                                                                                            |
| **Backend or frontend code structure**                 | [Backend](./BACKEND_CODING_STANDARDS.md#backend-review-process) / [Frontend Review Process](./FRONTEND_CODING_STANDARDS.md#frontend-review-process).                                                                                                                                                                                                                                                                                         |
| **What a release contains or claims**                  | The release documents under [`docs/05-releases/`](../05-releases/) — **not yet authored**. A release **records** what shipped; the behavior it records was granted and reviewed elsewhere, before the release named it. Nothing is first decided in a release — so while those documents do not exist, such a change owes **no separate release review**, and waits for none: it owes the reviews its content already owed (the rows above). |
| **What a document decides** *(documentation-only)*     | The **owning document's** own review — whichever row above matches what the change decides. A documentation change that alters a decision is reviewed as that decision (§6), never as an edit.                                                                                                                                                                                                                                               |

**When an ADR is required.** When the decision is significant, precedent-setting, or hard to reverse
([CLAUDE.md §8](../../CLAUDE.md)). An ADR is not paperwork attached to a large change — it is the record of a decision,
and small changes embody decisive ones routinely. Ratified ADRs are **superseded by a new ADR**, never quietly edited
([IMPLEMENTATION_PLAN §9](./IMPLEMENTATION_PLAN.md#9-change-management)).

**Reviews are not ranked.** A change owing three reviews owes all three; satisfying the most demanding does not satisfy
the others, because they are looking for different failures. A security concern is never traded against any of them
([SECURITY](../01-architecture/SECURITY.md#security-review-process)).

*No individual or team is named here, deliberately.* Review is defined by **what must be examined**, not by who happens
to be available — an obligation attached to a person disappears with them.

---

## 8. Conflict Resolution

**The precedence rules are owned by [CLAUDE.md §3](../../CLAUDE.md)** — which document wins, and why. **This document
has no precedence rule of its own**, and does not summarize CLAUDE.md's: a summary is a copy, and a copy of a precedence
rule is the one thing in this repository that must never drift. Read it there.

What this section owns is **what a contributor does** when they hit a conflict:

| Situation                                       | What happens                                                                                                                                                                                                                                                                                                         |
|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Two documents disagree**                      | Apply precedence ([CLAUDE.md §3](../../CLAUDE.md)) to unblock the change — **and treat the conflict as a defect anyway.** Precedence resolves the change; it does not resolve the repository. The losing document is corrected, or the next contributor rediscovers the same conflict and may read it the other way. |
| **A document disagrees within another's scope** | Establish that it is a contradiction at all before escalating: a specific decision may be the *detailed form* of a general one rather than a conflict with it ([CLAUDE.md §3](../../CLAUDE.md)). What survives that check is escalated, never reconciled by the reader.                                              |
| **A frozen document appears to be wrong**       | Stop and raise it ([CLAUDE.md §8](../../CLAUDE.md)). It may genuinely be wrong — documents are not infallible, only authoritative. What is never available is working around it: a correct workaround is a contradiction that now has an implementation defending it.                                                |
| **A conflict is unresolved at review time**     | **The change stops.** Not paused pending a judgement call, not merged with a note — stopped. A change merged over an unresolved conflict has silently decided it, and the decision is now load-bearing.                                                                                                              |
| **The convenient reading is available**         | It is not taken. Choosing the wording that permits the change is not interpretation; it is an unrecorded amendment to a frozen document, made unilaterally, by whoever wanted the change most.                                                                                                                       |

**Conflicts are reconciled before merge, not tracked.** A known contradiction left in place is a decision to let the
next
reader resolve it — and they will resolve it differently, at a moment nobody is watching. The reconciliation is itself a
change, routed like any other (§4): it belongs to the document that must be corrected.

> **A known, unreconciled contradiction: the branching model.**
> [IMPLEMENTATION_PLAN §6](./IMPLEMENTATION_PLAN.md#6-development-workflow) and
> [`README.md`](../../README.md) do not agree about where changes integrate, and IMPLEMENTATION_PLAN cites the README
> for the branching strategy it disagrees with. It is recorded here so that no contributor assumes this document has
> quietly settled it — **it has not, and §5 deliberately does not.**
>
> Until it is reconciled in the documents that own it, a contributor MUST NOT resolve it ad hoc, in this file or in a
> change: not by picking the reading their work needs, and not by inferring one from what the repository currently does.
> Reconciling it is a change like any other (§4), owed to those documents and raised per
> [CLAUDE.md §8](../../CLAUDE.md).

---

## 9. Contributor Checklist

Every change is assessed against this checklist before it is proposed for merge. A "no" is a finding to resolve, not a
detail to defer.

> This is **not** the Definition of Done — that is
> [IMPLEMENTATION_PLAN §7](./IMPLEMENTATION_PLAN.md#7-definition-of-done)'s, and it asks whether the *work* is complete.
> This asks whether the change is **governed**: routed to its owner, traceable, and claiming no authority it was not
> given. Both must pass; neither substitutes for the other.

- [ ] **Owning document identified?** — The change's decision has a home, named (§3, §4).
- [ ] **Scope confirmed?** — It is inside the
  product [boundaries](../00-product/PRODUCT_DECISIONS.md#2-product-boundaries)
  and the granted requirements; nothing here is a feature that no document grants.
- [ ] **Frozen documents checked?** — Every document the change touches has been read, not assumed
  ([CLAUDE.md §2](../../CLAUDE.md)).
- [ ] **Traceability preserved?** — The change walks back to the document that grants it, and forward from that document
  to the change (§6).
- [ ] **No new hidden requirements?** — No behavior, rule, or default exists only in the code, a comment, or a
  convention ([CLAUDE.md §8](../../CLAUDE.md)).
- [ ] **No duplicate authority introduced?** — Nothing here re-decides what another document owns; obligations are
  referenced, not restated (§6).
- [ ] **Review path identified?** — Every review the change owes is named, and none was skipped because the change felt
  small (§7).
- [ ] **Documentation updated?** — The owning document reflects the change, updated before or with it
  ([IMPLEMENTATION_PLAN §9](./IMPLEMENTATION_PLAN.md#9-change-management)); [STATUS](./IMPLEMENTATION_STATUS.md)
  reflects
  what landed.
- [ ] **Cross-references validated?** — Links resolve and still say what they are cited for; nothing points at a moved
  target (§6).
- [ ] **Safe to merge?** — Every owed review returned, no conflict is outstanding (§8), and the
  [Definition of Done](./IMPLEMENTATION_PLAN.md#7-definition-of-done) holds.

---

## Contribution Review Process

> *Unnumbered governance section. It defines when a contribution is reviewed for **governance** — routing, traceability,
> and authority — and how contribution governance evolves. It does not replace any domain's review (§7); it is what
> catches a change that never reached one.*

**Review triggers** — a contribution review is required when any of the following occurs:

- **New feature work** — it needs a granted requirement before it needs an implementation
  ([SRS](../00-product/SRS.md)).
- **A behavior change** — anything a user would notice, however it arrived.
- **An architecture change** — boundaries, dependency direction, or style
  ([ARCHITECTURE](../01-architecture/ARCHITECTURE.md)).
- **A design change** — appearance, assembly, tone, or flow ([DESIGN_SYSTEM](../02-design/DESIGN_SYSTEM.md)).
- **An AI change** — behavior, provider, prompt, evaluation, or retrieval (§7).
- **A documentation-only change affecting meaning** — a change to what a document *decides* is a change to the
  repository (§6). Reformatting is not; rewording a rule is.
- **A conflict with a frozen document** — surfaced at any point, by anyone (§8).
- **A cross-document inconsistency** — two documents that no longer agree, whether this change caused it.
- **A release-impacting change** — anything that alters what a release contains or claims
  ([`docs/05-releases/`](../05-releases/)).

**Review outcomes** — each review resolves to exactly one:

- **Approved** — routed correctly, traceable, claiming no authority it was not given; it proceeds to the gate
  [IMPLEMENTATION_PLAN §7](./IMPLEMENTATION_PLAN.md#7-definition-of-done) owns.
- **Refinement required** — the intent is sound but something must change first: an unnamed owner, a missing document
  update, a stale cross-reference, an obligation restated instead of referenced.
- **Product review required** — it would add, cut, or redefine product behavior; routed to
  [PRD](../00-product/PRD.md)/[SRS](../00-product/SRS.md) before proceeding.
- **Architecture review required** — routed to the [Review Process](./IMPLEMENTATION_PLAN.md#review-process)'s
  architecture triggers.
- **Design review required** — routed to the owning design process (§7).
- **AI review required** — routed to the owning AI process (§7).
- **Evaluation review required** — the change's effect on AI quality is unmeasured; routed to
  [EVALUATION](../04-ai/EVALUATION.md#evaluation-review-process). **A behavior change is never approved on reasoning
  alone.**
- **Security review required** — routed to the
  [Security Review Process](../01-architecture/SECURITY.md#security-review-process). This outcome **blocks**.
- **ADR required** — the decision is significant, precedent-setting, or hard to reverse
  ([CLAUDE.md §8](../../CLAUDE.md)).

**Only Approved permits merge.** *Refinement required* returns the change to its author; every other outcome holds it
until the review it was routed to returns. A change held there is not approved by the passage of time, and a review that
was owed is not discharged by the change being urgent.

**Synchronization.** Contribution governance MUST remain synchronized with [CLAUDE.md](../../CLAUDE.md) and the frozen
documents — in particular [IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md), whose workflow, Definition of Done, and
Change Management this document defers to and MUST NOT duplicate or contradict. When this document and one of them
disagree, **they win** and this document is corrected ([CLAUDE.md §3](../../CLAUDE.md)). A change to how contribution
works that touches more than one document is reviewed by each, and MUST NOT be merged into one while leaving another
contradicting it.

---

## 10. Contribution Decision Summary

The load-bearing decisions behind contribution governance, recorded so they are not silently reversed. The
**implementation workflow** ([CLAUDE.md §5](../../CLAUDE.md)), the **development workflow** and **Definition of Done**
([IMPLEMENTATION_PLAN §6–§7](./IMPLEMENTATION_PLAN.md#6-development-workflow)), **Change Management**
([§9](./IMPLEMENTATION_PLAN.md#9-change-management)), and **document precedence** ([CLAUDE.md §3](../../CLAUDE.md)) are
decided elsewhere and are not restated here.

| Decision                                            | Chosen Approach                                                                                      | Alternatives                                                                        | Rationale                                                                                                                                                                                                                                                                              |
|-----------------------------------------------------|------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Source-of-truth first**                           | The owning document grants the change before the change is written                                   | Implement first and update the document to match                                    | A document written to match code is a transcript, not a decision — and it is defended as though someone had decided it. Inverting the order is how the repository stops being described by its documents while still appearing to be (§4).                                             |
| **Ownership-driven workflow**                       | Routing — *whose decision is this?* — precedes the build path, and this document adds no stage to it | Define a contribution workflow here; or let contributors follow the nearest example | Two workflows already exist ([CLAUDE.md §5](../../CLAUDE.md), [IMPLEMENTATION_PLAN §6](./IMPLEMENTATION_PLAN.md#6-development-workflow)) and both start from a granted requirement. A third would be a third thing to sync and a second answer to one question (§4).                   |
| **Routing, not re-deciding**                        | This document points at owning documents and their review processes; it decides nothing they decide  | Summarize the rules here so contributors have one place to look                     | A summary is a copy, and a copy drifts while still being cited. Convenience for the reader is bought with a contradiction for their successor — the exact failure this document exists to prevent (§7).                                                                                |
| **Cross-document traceability**                     | A change walks back to the document granting it, and that document walks forward to the change       | Record the rationale in commit history or the pull request                          | History says what changed, not what was granted or why. A one-directional link is a coincidence: the document that cannot reach its implementation cannot be verified against it, and drift becomes undetectable rather than merely undetected (§6).                                   |
| **Review before merge, routed by touch**            | A change owes a review to **every** document it touches; none is ranked above another                | Review by change size; or let the most demanding review stand for the rest          | Reviews look for different failures — a security review does not catch a design contradiction, and neither notices an unowned requirement. Size is not a proxy for impact: a one-line change routinely embodies a decisive one (§7).                                                   |
| **Documentation is part of the change**             | The document update ships with the change, not after it                                              | Track documentation separately and update in batches                                | Documentation deferred is documentation reconstructed — written by someone inferring intent from a diff, with more confidence than they have. The interval between code and record is the interval the repository is lying (§6).                                                       |
| **Escalation on conflict**                          | An unresolved conflict stops the change; the convenient reading is never taken                       | Let the contributor apply judgement and note the ambiguity                          | Applying judgement to a frozen contradiction is an unrecorded amendment made unilaterally by whoever wants the change most. Noting it defers the decision to the next reader, who will decide differently, unwatched (§8).                                                             |
| **Small, reviewable diffs**                         | One concern per change, with unrelated work excluded                                                 | Allow bundled changes where they are convenient to ship together                    | Review quality collapses with size, and bundling means approving one decision to approve another. "While I was in here" is how a decision nobody asked for arrives inside a change everybody wanted ([IMPLEMENTATION_PLAN §2](./IMPLEMENTATION_PLAN.md#2-engineering-principles), §5). |
| **No silent authority drift**                       | Nothing re-decides what another document owns; obligations are referenced, never restated            | Restate key rules locally so each document reads completely on its own              | Two documents answering one question is a contradiction with a delivery date. Once they disagree, both are authoritative and both are cited, and there is no rule that resolves it — because both were written as though they owned it (§6, §8).                                       |
| **A documentation change can be a behavior change** | Editing what a document decides is reviewed as the decision, not as an edit                          | Treat documentation edits as low-risk and review them lightly                       | The most dangerous change here is a clarifying rewording that settles a question the original deliberately left open. It carries no code, triggers no test, and changes what the product is required to do (§6).                                                                       |

---

*This document governs how contributors change the LedgerAI repository — how a change is routed, reviewed, documented,
and merged; it does not override the frozen documents under [`docs/`](../), and it defines no product, architecture,
design, or AI behavior. It operates inside [CLAUDE.md](../../CLAUDE.md), defers the build path and the merge gate to
[IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md), routes every change to the document that owns it and to that document's
own review process, and adds no authority of its own. When a change would contradict a frozen document, introduce
behavior no document grants, or alter a ratified decision, stop and raise it per [CLAUDE.md §8](../../CLAUDE.md) — the
repository is only as trustworthy as the least-routed change in it.*
