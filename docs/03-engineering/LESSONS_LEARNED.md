# Lessons Learned — LedgerAI

> **Status:** Draft v1
> **Owner:** Principal Engineer — Engineering Knowledge
> **Last updated:** 2026-07-16
> **Upstream (frozen):
> ** [CLAUDE.md](../../CLAUDE.md) · [IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md) · [IMPLEMENTATION_STATUS](./IMPLEMENTATION_STATUS.md) · [PRODUCT_DECISIONS](../00-product/PRODUCT_DECISIONS.md) · [SECURITY](../01-architecture/SECURITY.md)
> **Related:
> ** [CONTRIBUTING](./CONTRIBUTING.md) · [DEPLOYMENT](./DEPLOYMENT.md) · [TESTING_STRATEGY](./TESTING_STRATEGY.md) · [
`docs/05-releases/`](../05-releases/)

---

## 1. Purpose

### Why this document exists

A repository forgets in a particular way. It never forgets *what* was decided — that is written down, ratified, and
cited. It forgets **what it cost to find out**: the approach that looked obviously right and quietly wasn't, the
constraint nobody understood until it bit, the second time somebody proposed the thing that failed the first time and
nobody in the room had been there.

**Knowledge preservation is already well owned here, and this document claims none of it.** Rejected ideas and why they
were rejected are [PRODUCT_DECISIONS §5](../00-product/PRODUCT_DECISIONS.md#5-rejected-ideas)'s — *recorded so they are
not revisited without new information*. Architectural decisions and when to revisit them are the
[ADRs](../01-architecture/decisions/)'. Anticipated problems are the Risks sections'
([PLAN §8](./IMPLEMENTATION_PLAN.md#8-risks),
[ARCHITECTURE §14](../01-architecture/ARCHITECTURE.md#14-architecture-risks),
[STATUS §9](./IMPLEMENTATION_STATUS.md#9-active-risks)). Known shortcuts are
[STATUS §8](./IMPLEMENTATION_STATUS.md#8-technical-debt)'s; decisions made in flight are
[STATUS §10](./IMPLEMENTATION_STATUS.md#10-decision-log)'s. Incident timelines and root causes are
[SECURITY §18](../01-architecture/SECURITY.md#18-incident-response)'s. Why a prompt, provider, evaluation, or retrieval
approach was abandoned is each AI document's *Retired* stage.

What none of them holds is **generalizable knowledge from completed work that is not yet anyone's rule**: not a
decision, not a risk someone predicted, not a shortcut to repay, not an incident — a thing that turned out to be true,
learned by doing, that no document yet enforces and perhaps never will.

The governing principle of this document:

> **A lesson that stays a lesson has not been learned.**
>
> Knowledge that lives only here changes nothing: it is read by the people who already have it and missed by everyone
> who needs it. Recording is where learning **starts**. It ends when the lesson becomes a rule, a checklist item, a
> test, or an ADR — enforced by a document that can actually stop someone.

This is why **a lesson never becomes authority by being recorded here**. This document has no power to require anything.
Its influence is entirely indirect: a lesson persuades an owning document to change, and *that* document does the
requiring (§7). A lesson cited as a reason someone must do something has been misread, and the person citing it has
skipped the step where somebody with authority agreed.

Some knowledge genuinely cannot become a rule — it is context, not policy: the reasoning behind a rule that already
exists, or a pattern too weak to legislate but too real to lose. That is the exception this document exists to hold, not
the default it should settle into.

It is **not** a retrospective, **not** a postmortem, **not** a changelog, **not** release notes, **not** implementation
history, **not** architecture documentation, **not** issue tracking, and **not** decision-making. It contains **no
timelines, no incidents, no identifiers, no history, no attribution to individuals, no tooling, no code, no examples,
and no metrics**. It governs *how durable engineering knowledge is extracted, recorded, routed, and retired* — never
what any team should do about it.

### The question this document answers

> **When something teaches us something worth keeping, where does that learning belong?**

Usually: **somewhere else.** This document's main job is to answer *where* — and to hold what has no home yet.

### Relationship to the governing documents

| Document                                                   | Its job                                  | The boundary with this document                                                                                                                                                                                                                                                                                                                                                                                                                         |
|------------------------------------------------------------|------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md)     | **Progress and live execution state**    | It records **what is true now** — phases, debt ([§8](./IMPLEMENTATION_STATUS.md#8-technical-debt)), risks ([§9](./IMPLEMENTATION_STATUS.md#9-active-risks)), decisions in flight ([§10](./IMPLEMENTATION_STATUS.md#10-decision-log)). It is chronological and disposable: entries stop mattering once superseded. This document records **what remains true after the work is over**. It is neither a tracker nor a log, and it is never chronological. |
| [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)         | **The build path**                       | It owns build order, the merge gate, and the risks anticipated **in advance** ([§8](./IMPLEMENTATION_PLAN.md#8-risks)). A lesson is what was learned **in retrospect** — often that a predicted risk was wrong, or that the real one was never listed. A lesson may argue for changing that document; it never edits it (§7).                                                                                                                           |
| [`CHANGELOG.md`](../05-releases/CHANGELOG.md)              | **What changed**                         | **Not yet authored.** It records the fact of a change. This document records what the change **taught**, which is a different thing and usually survives it: a changelog entry ages out of relevance the moment nobody runs that version; a lesson does not.                                                                                                                                                                                            |
| [`RELEASE_NOTES.md`](../05-releases/RELEASE_NOTES.md)      | **What shipped**                         | **Not yet authored.** It describes shipped behavior to its audience. This document is internal, generalized, and about **how the work went** — never about what the product does.                                                                                                                                                                                                                                                                       |
| **The [ADRs](../01-architecture/decisions/)**              | **Ratified architectural decisions**     | An ADR **decides**; a lesson **informs**. Every ADR already carries its own reasoning and a *Future Reconsideration* — so a lesson never restates one, and a lesson that has hardened into a decision is written as an **ADR**, not preserved here as a stronger opinion (Rules).                                                                                                                                                                       |
| [PRODUCT_DECISIONS.md](../00-product/PRODUCT_DECISIONS.md) | **What was decided, deferred, rejected** | [§5 Rejected Ideas](../00-product/PRODUCT_DECISIONS.md#5-rejected-ideas) already preserves *why we are not doing that*, so it is not revisited without new information; [§4](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) preserves what waits and why. Product knowledge belongs there. This document holds **engineering** knowledge, and never a product judgement.                                                                      |
| [SECURITY.md](../01-architecture/SECURITY.md)              | **The security posture**                 | [§18](../01-architecture/SECURITY.md#18-incident-response) owns incident response and the **timeline and root cause** — and it routes lessons *here* explicitly. This document receives the **lesson**, never the incident: no timeline, no narrative, nothing that identifies an event or a person. Security findings feed controls and the threat model first; that is SECURITY's, not this document's.                                               |
| [CONTRIBUTING.md](./CONTRIBUTING.md)                       | **How changes are routed**               | It routes **changes**; this document routes **knowledge**. They meet at the handoff: acting on a lesson is a change, and it enters [CONTRIBUTING §4](./CONTRIBUTING.md#4-repository-workflow) like any other — a lesson is never a shortcut past routing, and being obviously right does not make it one.                                                                                                                                               |
| [CLAUDE.md](../../CLAUDE.md)                               | **The engineering playbook**             | It owns the hierarchy and precedence (§3), the rules (§4), and the stop conditions (§8). A lesson sits **below everything**: it has no place in the hierarchy because it grants nothing. Where a lesson and any document appear to disagree, **the document wins** and the lesson is wrong, out of date, or was never general (§8).                                                                                                                     |

In one line each:

> **IMPLEMENTATION_STATUS records progress. CHANGELOG records what changed. RELEASE_NOTES describe what shipped. ADRs
> record architectural decisions. CONTRIBUTING governs future changes. This document governs reusable engineering
> learning.**

---

## 2. Learning Philosophy

These principles explain *why* learning is governed the way it is. They are the reasoning behind the enforceable rules
that follow.

| Principle                                       | Why it exists                                                                                                                                                                                                                                                                       |
|-------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Learn once**                                  | The cost of discovery is paid once and can be reused indefinitely, or paid again by everyone who arrives later. Nothing about a repository makes the second option obvious while it is happening — it feels like ordinary work.                                                     |
| **Avoid repeated mistakes**                     | The same mistake recurs not because people are careless but because the reason it was a mistake left with the person who made it. A team without preserved reasoning is not inexperienced; it is **repeatedly** inexperienced, on a cycle set by turnover.                          |
| **Preserve reasoning**                          | Conclusions age; reasoning travels. "Do it this way" is unusable the moment circumstances differ, because nobody can tell whether the difference matters. *Why* is what makes a lesson applicable to a situation it did not anticipate.                                             |
| **Distinguish facts from opinions**             | Both are useful and they are not interchangeable. An opinion recorded as a fact acquires authority nobody granted it, and it is quoted with a confidence its evidence never supported — most durable bad practice began as a defensible preference somebody wrote down (§3).        |
| **Record durable lessons**                      | Most of what a project teaches is about *this* week: this defect, this constraint, this workaround. It is real and it does not keep. Recording it dilutes what does keep, and the dilution is what makes a knowledge document stop being read (§6).                                 |
| **Avoid local optimizations becoming doctrine** | A thing that worked once, in one context, for reasons nobody isolated, becomes "how we do it here" purely by being written down. That is how a repository accumulates rules whose original condition disappeared and whose cost is now paid forever.                                |
| **Document causes rather than symptoms**        | A symptom teaches only how to recognize its recurrence; a cause teaches how to prevent the family it belongs to. Symptoms are also what everyone remembers, because they are what everyone experienced — so this has to be a rule, not an intention.                                |
| **Separate observation from decision**          | Seeing something is not deciding about it. Fusing them means the decision inherits the observation's credibility without inheriting its review — and it is made by whoever noticed, which is precisely who has the least distance from it (§3).                                     |
| **Improve governance**                          | A lesson's highest use is fixing the rule that let the mistake happen, not warning the next person to be careful. Care does not scale and does not survive a deadline; a rule that catches the mistake does (§7).                                                                   |
| **Remove accidental complexity**                | Completed work reveals which complexity was essential and which was accumulated — a distinction that is invisible in advance and obvious afterward. That window closes fast: within a release, the accidental has become "how it works" and is defended as though it were designed. |

---

## Lessons Learned Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Lessons MUST come from completed work.** *Learning drawn from work still in flight is a prediction wearing the
  credibility of experience. The thing that looked like the lesson at the midpoint is routinely not the lesson — and it
  is quoted forever with the authority of hindsight it never had.*
- **Lessons MUST be supported by evidence.** *Without it, a lesson is a preference that survived long enough to be
  quoted. Evidence is also the only thing that lets a lesson be **refuted** later; an unevidenced lesson cannot be
  removed, because there is nothing to show has changed (§8).*
- **Lessons MUST distinguish observation from recommendation.** *An observation is what happened; a recommendation is
  what someone should do about it. The first belongs here. The second is a proposed rule, and it belongs to the document
  that would enforce it, through review (§3, §7).*
- **Lessons MUST identify the owning document.** *A lesson with no owner is knowledge nobody will ever act on: it has no
  route into a rule, no reviewer, and no way to be reconciled when it goes stale. Naming the owner is what makes it
  actionable rather than literary (§7).*
- **Lessons MUST NOT redefine requirements.** *Behavior lives in [SRS](../00-product/SRS.md) and
  [PRD](../00-product/PRD.md). A lesson that quietly restates a requirement differently has created a second, unranked
  source of truth — one that is persuasive precisely because it reads as hard-won.*
- **Lessons MUST NOT redefine architecture.** *Structure is [ARCHITECTURE](../01-architecture/ARCHITECTURE.md)'s and the
  [ADRs](../01-architecture/decisions/)'. A lesson may be the best argument for changing one; it is never the change,
  and the gap between this is the whole of this document's discipline.*
- **Lessons MUST NOT replace ADRs.** *A lesson that has hardened into a decision **is** a decision, and decisions are
  ratified where decisions live ([CLAUDE.md §8](../../CLAUDE.md)). Left here, it is an unratified decision with an
  unusually good story, and it will be followed as though someone approved it.*
- **Lessons SHOULD improve future work.** *A lesson nobody could act on is a memoir. The test is not whether it is true
  or interesting — it is whether a future contributor would do something differently for having read it (§6).*
- **Lessons SHOULD remain generally applicable.** *Knowledge that only applies to the exact situation that produced it
  is that situation's record, not a lesson — and it belongs to whatever tracks that
  situation ([STATUS §8](./IMPLEMENTATION_STATUS.md#8-technical-debt)), not here (§3).*
- **Lessons MUST remain traceable.** *A lesson cited without what it came from cannot be checked, dated, or challenged —
  and stale knowledge is most persuasive exactly when it is most wrong, because it has been true for so long that nobody
  remembers testing it (§8).*

**Why these rules exist.** Lessons preserve knowledge; they do not create authority. That distinction is the entire
document, and it fails in three ways. **A lesson becomes a rule nobody ratified** — quoted in review, obeyed, never
approved, and impossible to argue with because arguing looks like refusing to learn. **A lesson becomes doctrine** — a
local result, generalized once, defended forever, its original condition long gone. **A lesson becomes a second source
of truth** — describing behavior or structure in words that no longer match the documents that own them, and the lesson
is the one people actually read.

Each failure looks like diligence at the time. Nobody sets out to legislate through a knowledge document; they write
down something true and useful, and someone later treats it as binding because it was written down and nothing said it
wasn't. These rules exist because a lesson's persuasiveness is entirely uncoupled from its authority — and only one of
those two things is written on the page.

---

## 3. What Qualifies As A Lesson

Most of what a project produces is not a lesson. The distinctions below are the ones that decide where knowledge goes,
and every one of them is routinely collapsed:

| Kind               | What it is                                                                                | Where it belongs                                                                                                                                                                                                                                                                                                              |
|--------------------|-------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Defect**         | Something was wrong and was fixed. A fact about one thing at one time.                    | The change that fixed it, and a test that keeps it fixed ([TESTING_STRATEGY](./TESTING_STRATEGY.md)). **Not here.** A bug is not a lesson; what it *revealed* might be.                                                                                                                                                       |
| **Observation**    | Something was noticed. A single data point, possibly a coincidence.                       | Here, but **as an observation** and not yet a lesson (§5). One occurrence is an anecdote; generalizing it early is how doctrine forms.                                                                                                                                                                                        |
| **Pattern**        | The same observation, more than once, across different situations.                        | Here — the raw material a lesson is made from. A pattern is what promotes an observation from *interesting* to *investigable*.                                                                                                                                                                                                |
| **Lesson**         | Generalized, evidence-backed knowledge from completed work, with a named owning document. | **Here.** This is the whole of what this document holds.                                                                                                                                                                                                                                                                      |
| **Recommendation** | What someone should do about a lesson — a proposed rule, standard, or constraint.         | **The owning document**, through its review. A recommendation recorded here is a rule that skipped ratification and will be obeyed anyway (Rules, §7).                                                                                                                                                                        |
| **Future work**    | Something that ought to be done or reconsidered later.                                    | [PRODUCT_DECISIONS §4](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) / [§7](../00-product/PRODUCT_DECISIONS.md#7-future-candidate-features), an ADR's *Future Reconsideration*, or [STATUS §8](./IMPLEMENTATION_STATUS.md#8-technical-debt). **Not here** — this document holds what was learned, never a backlog. |

**The line that matters most is between observation and recommendation.** "Ownership checks were the thing reviewers
missed most often" is an observation, and it is this document's. "Reviewers must therefore check ownership first" is a
rule, and it is the reviewing document's — reachable only through that document's review. Recording the second here
produces a requirement that no reviewer approved, that no document lists, and that is nonetheless followed, because it
is written down and sounds like experience.

**A lesson is not a strong opinion with a story attached.** The distinguishing property is evidence from completed work
(Rules) — not conviction, seniority, or how well it is phrased.

---

## 4. Sources Of Learning

Learning arrives from wherever work actually happens. In every case **the source document owns the detail, and this
document holds only what generalizes** — a lesson summarizes an outcome; it never replaces, restates, or competes with
the record it came from.

| Source                     | What it can teach, and the boundary                                                                                                                                                                                                                                     |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Implementation**         | Which complexity was essential and which accumulated — visible only once the work is done (§2). The work itself is [STATUS](./IMPLEMENTATION_STATUS.md)'s; what it revealed about building things here is a lesson.                                                     |
| **Testing**                | What the tests failed to catch, and why — usually more informative than what they caught. What *should* be tested is [TESTING_STRATEGY](./TESTING_STRATEGY.md)'s; a lesson may argue for changing it (§7).                                                              |
| **Deployment**             | What promotion, observation, or withdrawal revealed ([DEPLOYMENT](./DEPLOYMENT.md)). The gates and rollback rules are that document's; a lesson never adjusts one — it argues for adjusting it.                                                                         |
| **AI evaluation**          | What measurement showed about AI behavior over time ([EVALUATION](../04-ai/EVALUATION.md)). Findings, baselines, and verdicts are entirely that document's; a lesson holds only what generalizes **beyond** a subject or a baseline.                                    |
| **Security review**        | What a control, review, or incident taught. [SECURITY §18](../01-architecture/SECURITY.md#18-incident-response) owns the timeline and root cause and routes the lesson here — the lesson arrives **stripped**: no timeline, no narrative, nothing identifying an event. |
| **Architecture review**    | What a boundary or dependency turned out to cost. Structure is [ARCHITECTURE](../01-architecture/ARCHITECTURE.md)'s and the [ADRs](../01-architecture/decisions/)'; a lesson that has become a decision is written as an ADR (Rules).                                   |
| **Documentation work**     | Where the documents were unclear, duplicated, or contradictory — the failure mode that is invisible until someone tries to follow them. Corrections are routed per [CONTRIBUTING](./CONTRIBUTING.md#4-repository-workflow); the lesson is what the confusion revealed.  |
| **Production operation**   | What running the system taught that no environment predicted (§2). Health and signals are [DEPLOYMENT §8](./DEPLOYMENT.md#8-deployment-observability)'s; the lesson is the generalization, never the event.                                                             |
| **Contributor experience** | Where the process itself misled people — the same wrong turn taken by different people is evidence about the **documents**, not about the people. This is recorded as a property of the process; **never** attributed to anyone (Writing standards, §6).                |

**A lesson never becomes the reason its source is not consulted.** It summarizes an outcome so the outcome is not lost;
it is not a substitute for the document that owns the detail, and it is not the place to look when precision matters.

---

## 5. Lesson Lifecycle

Not every observation becomes a lesson — most should not. The stages exist so that the discipline of *not yet* is
available, and so that the difference between *noticing* and *knowing* is visible rather than assumed.

| Stage            | What it means                                                                                                                                                                                                                                                        |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Observed**     | Something was noticed in completed work. It carries no weight yet and nothing depends on it. Most knowledge ends here, correctly — the discipline is recording it without promoting it.                                                                              |
| **Investigated** | The cause was sought rather than assumed. Cause, not symptom (§2) — the first explanation is usually the most available one, not the true one, and it is the one everybody already believes.                                                                         |
| **Validated**    | The evidence supports the conclusion, and would still support it if the person who found it were not the one presenting it (Rules).                                                                                                                                  |
| **Generalized**  | It is established that this applies beyond the situation that produced it. This is the stage most often **skipped**, and skipping it is exactly how a local result becomes doctrine (§2). Failing here is a success: the knowledge stays specific, where it belongs. |
| **Documented**   | Written as a lesson: observation separated from recommendation, evidence identified, owning document named (§3, §6). It has authority over nothing (§1).                                                                                                             |
| **Adopted**      | The owning document changed because of it — a rule, a checklist item, a test, an ADR (§7). **This is the success state.** The lesson remains as the *reasoning* behind that rule, which is what makes the rule safe to revisit later rather than merely obeyed.      |
| **Superseded**   | Replaced by better knowledge. The prior lesson is marked superseded, **never edited into agreement**: what changed our minds is itself the most valuable thing on the page (§8).                                                                                     |
| **Archived**     | No longer applicable — its context is gone, or it was adopted and the rule now carries it. It is retained, not deleted: an archived lesson is what answers *"why don't we do it that way?"*, and that question always returns.                                       |

**Every stage has an exit.** *Observed* ends when it is investigated or set aside as noise. *Investigated* ends in a
cause or an honest admission that none was found — which is itself recordable and often the real lesson. *Generalized*
ends in a lesson **or** in the finding that it does not generalize, which routes the knowledge to whatever owns the
specific case (§3). *Documented* ends in *Adopted* or in a standing lesson that no rule can carry (§1). Nothing sits
between stages indefinitely: knowledge that cannot move is either not understood yet or not worth keeping, and saying
which is part of the work.

**Not every observation becomes a lesson, and the filter is the point.** A document that records everything noticed is
not a knowledge base; it is a diary, and it is read the way diaries are read — by its author, once. What protects a
lesson's weight is how many observations never became one.

---

## 6. Writing Good Lessons

These are properties of a **useful** lesson, not a house style. How it is phrased is nobody's business here; whether it
survives contact with a future contributor is.

| Property                     | What it means, and why it fails without it                                                                                                                                                                                              |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Objective**                | It describes what was, not who was involved or how it felt. Attribution converts knowledge into a record about people, and the reliable consequence is that the next finding is not reported at all.                                    |
| **Evidence-backed**          | It says what supports it (Rules). Evidence is what a future reader uses to decide whether it still applies to circumstances this lesson never saw.                                                                                      |
| **Reusable**                 | A future contributor could act on it in a situation that is *similar*, not identical. Knowledge that requires the original circumstances to recur is that situation's record (§3).                                                      |
| **Technology-independent**   | It survives the thing that produced it. Knowledge expressed in terms of a specific tool, library, or version retires with it — and the underlying truth, which usually outlives it by years, retires too because nobody separated them. |
| **Concise**                  | Long enough to carry the reasoning, short enough to be read. A knowledge document is judged on whether anyone reads it, and nothing else — an unread lesson and a missing lesson are the same lesson.                                   |
| **Actionable**               | A reader could do something differently. If not, it is an observation (§3) — legitimate, but it should not be dressed as more.                                                                                                          |
| **Attributable to a source** | It names where it came from — the work, review, or evaluation, never the person (§4). Without it the lesson cannot be checked or dated, and it becomes unfalsifiable simply by aging.                                                   |
| **Durable**                  | It will still be true after the code, the module, and the release that produced it are gone. Durability is what separates a lesson from a note, and it is the property that cannot be added later.                                      |

**A lesson is written for someone who was not there.** That reader has no context, no memory of the situation, and no
way to ask. Everything the lesson relies on but does not say is lost on them — and they are the entire audience, because
everyone who *was* there already knows.

---

## 7. Applying Lessons

**Lessons influence the owning documents; they never override them.** This is the mechanism by which a lesson does
anything at all — and it is deliberately indirect. A lesson has no force. What it can do is make the case, to the
document that has force, that something should change.

| A lesson can inform…       | Through                                                                                                                                                                                                                                                                                                                                                                                                      |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Future implementation**  | Contributors reading it before doing similar work. This is the weakest form of influence — it depends on someone happening to read it at the right moment — which is why *Adopted* (§5) exists and is the goal.                                                                                                                                                                                              |
| **Architecture proposals** | Being the evidence in an [ADR](../01-architecture/decisions/). The ADR decides; the lesson is why. An architectural change that cites only a lesson has skipped the ratification the lesson exists to inform ([CLAUDE.md §8](../../CLAUDE.md)).                                                                                                                                                              |
| **Documentation updates**  | Being the reason a document is corrected — routed like any change ([CONTRIBUTING §4](./CONTRIBUTING.md#4-repository-workflow)). Where a lesson reveals that documents were unclear or contradictory, the fix belongs in them, not in a warning here (§8).                                                                                                                                                    |
| **Review expectations**    | Becoming a checklist item in the document that owns the review ([CONTRIBUTING §7](./CONTRIBUTING.md#7-review-and-approval-rules)). A repeated miss is evidence the checklist is wrong — not evidence reviewers should try harder (§2).                                                                                                                                                                       |
| **Engineering standards**  | Becoming a rule in the [backend](./BACKEND_CODING_STANDARDS.md) or [frontend](./FRONTEND_CODING_STANDARDS.md) standards, through their review. A standard is enforceable; a lesson quoted at someone is not, however right it is.                                                                                                                                                                            |
| **AI governance**          | Becoming a rule, criterion, or trigger in the document that owns it ([AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md#ai-review-process), [PROMPTS](../04-ai/PROMPTS.md#prompt-review-process), [EVALUATION](../04-ai/EVALUATION.md#evaluation-review-process)). AI behavior changes without a signature to break — a lesson there is especially tempting to apply directly, and especially wrong to. |
| **Deployment governance**  | Becoming a gate, a safety criterion, or a signal in [DEPLOYMENT](./DEPLOYMENT.md#deployment-review-process). A lesson from a bad promotion belongs in the gate that would have caught it — never as a caution people are expected to remember under pressure.                                                                                                                                                |

**Adoption is what learning looks like when it worked.** Until a lesson is carried by a document that can stop someone,
it depends entirely on the right person reading the right page at the right time — which is not a mechanism, it is a
hope. The lesson does not disappear on adoption: it stays as the rule's reasoning, which is what lets a future team
retire the rule intelligently instead of preserving it out of superstition (§5).

**A lesson is never a shortcut past routing.** Acting on one is a change, and it enters
[CONTRIBUTING §4](./CONTRIBUTING.md#4-repository-workflow) like any other. That a lesson is obviously correct is not a
reason to skip the step — it is the most common reason people do.

---

## 8. Maintaining Lessons

Knowledge decays, and a lesson has no natural expiry: nothing breaks when it stops being true. It simply keeps being
quoted, with its persuasiveness intact and its evidence long gone — which is why it is the only thing here that must be
maintained deliberately.

| Concern                            | The discipline                                                                                                                                                                                                                                                               |
|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Reviewing outdated lessons**     | A lesson is re-examined when its evidence, context, or owning document has moved (Lesson Review Process). The dangerous lesson is not the one that is obviously stale — it is the one still cited comfortably, whose original condition disappeared without anyone noticing. |
| **Superseding lessons**            | Better knowledge replaces it, and the prior lesson is **marked superseded rather than rewritten** (§5). Editing it into agreement erases the fact that we were wrong, which is usually more useful than the correction itself.                                               |
| **Removing obsolete lessons**      | A lesson whose context is gone is **archived, not deleted** (§5). Deletion loses the answer to *"why don't we do it that way?"* — and that question is always asked by someone proposing exactly that.                                                                       |
| **Avoiding contradictory lessons** | Two lessons that cannot both be right are not a debate to preserve. They are reconciled: the evidence changed, one never generalized, or they were always about different things (§3).                                                                                       |
| **Synchronizing with owners**      | A lesson tracks the document it names (Rules). When that document changes, the lesson is re-examined — a lesson describing a rule that no longer exists is a second source of truth with a plausible voice.                                                                  |

**Contradictory lessons indicate a governance defect elsewhere.** This is the most useful thing this document produces.
Two contradictory lessons are almost never a disagreement about the lessons: they are two teams, at two times, correctly
learning from **documents that do not agree** — or from a question no document ever answered, resolved twice,
differently. The contradiction here is the *symptom*. The defect is upstream, and it is found by asking what both
lessons were reading. Reconciling them without fixing that produces a third lesson, later, saying the same thing again.

**Where a lesson and a governing document disagree, the document wins** ([CLAUDE.md §3](../../CLAUDE.md)) — and the
lesson is not merely overruled, it is **wrong, stale, or was never general**, and it is corrected as such. A lesson is
not evidence against a frozen document; a lesson that seems to be is an argument for changing that document, raised per
[CLAUDE.md §8](../../CLAUDE.md).

---

## 9. Lessons Checklist

Every lesson is assessed against this checklist before it is recorded. A "no" is a finding to resolve, not a detail to
defer.

- [ ] **Evidence identified?** — What supports it is stated, and would support it without its author present (Rules).
- [ ] **Source documented?** — The work, review, or evaluation it came from is named; no individual is (§4, §6).
- [ ] **Ownership identified?** — The document that would carry it as a rule is named, even if it never does (Rules,
  §7).
- [ ] **Durable?** — It will outlive the code, module, and release that produced it (§6).
- [ ] **Reusable?** — A contributor could act on it in a similar situation, not only an identical one (§6).
- [ ] **Technology-independent?** — It survives the tool or version that produced it (§6).
- [ ] **No duplicated authority?** — It restates no requirement, no architecture, and no ADR; a recommendation has been
  routed to its owner rather than recorded here (§3, Rules).
- [ ] **Synchronized with governing documents?** — It contradicts none of them; if it appears to, that is raised, not
  written down (§8).
- [ ] **Future value established?** — A future contributor would do something differently for having read it; otherwise
  it is an observation, recorded as one (§3).
- [ ] **Ready to record?** — Observation and recommendation are separated, and it has been generalized rather than
  assumed to generalize (§5).

---

## Lesson Review Process

> *Unnumbered governance section. It defines when knowledge is examined for whether it is a lesson, and how this
> document's own governance evolves — deliberately, not by accretion.*

**Review triggers** — a lesson review is required when any of the following occurs:

- **A significant implementation finding** — completed work revealed something that generalizes (§4).
- **A testing discovery** — something the tests missed, or caught in a way that says more about the strategy than the
  defect ([TESTING_STRATEGY](./TESTING_STRATEGY.md#test-review-process)).
- **A deployment incident** — a promotion, observation, or withdrawal taught something
  ([DEPLOYMENT](./DEPLOYMENT.md#deployment-review-process)). The event is that document's; only the generalization is
  examined here.
- **A security finding** — routed from [SECURITY §18](../01-architecture/SECURITY.md#18-incident-response), **stripped
  of timeline and identity**. Controls and the threat model are updated first; the lesson never substitutes for either.
- **An architecture review** — a boundary or dependency cost something that was not predicted
  ([ARCHITECTURE](../01-architecture/ARCHITECTURE.md#14-architecture-risks)).
- **An AI evaluation result** — a finding that generalizes beyond its subject or baseline
  ([EVALUATION](../04-ai/EVALUATION.md#evaluation-review-process)).
- **A documentation correction** — the documents were unclear, duplicated, or contradictory, and someone was misled by
  them (§8).
- **A repeated contributor mistake** — the same wrong turn taken more than once. This is evidence about the **documents
  or the process**, never about the contributors (§4).
- **A process improvement** — a change to how work is done that is worth generalizing rather than adopting silently.
- **A superseded lesson** — new knowledge contradicts a recorded one (§8).

**Review outcomes** — each review resolves to exactly one:

- **Approved** — it is a lesson: evidenced, generalized, owned, and recorded with no authority (§1).
- **Refinement required** — the knowledge is real but not yet a lesson: unstated evidence, a symptom rather than a
  cause, an unnamed owner, or a recommendation that has not been separated from its observation (§3).
- **Documentation update required** — the lesson's actual home is a document, and it is routed there
  ([CONTRIBUTING §4](./CONTRIBUTING.md#4-repository-workflow)). **This outcome is the goal, not a rejection** (§5, §7).
- **Architecture review required** — it bears on structure or a ratified decision; raised per
  [CLAUDE.md §8](../../CLAUDE.md) and routed to the owning process.
- **Security review required** — it touches controls, exposure, or the threat model; routed to the
  [Security Review Process](../01-architecture/SECURITY.md#security-review-process). This outcome **blocks**, and the
  lesson waits for it — a security finding is never published here ahead of the control that answers it.
- **AI review required** — routed to the owning AI process
  ([CONTRIBUTING §7](./CONTRIBUTING.md#7-review-and-approval-rules)).
- **ADR required** — the knowledge has hardened into a decision, and decisions are ratified where decisions live
  ([CLAUDE.md §8](../../CLAUDE.md)). A lesson is never left standing in place of an ADR (Rules).
- **Archived** — it does not generalize, its context is gone, or it is now carried by a rule. Retained, never deleted
  (§5, §8).

Only **Approved** records a lesson; **Refinement required** returns it to its author; every other outcome routes it, and
a lesson routed elsewhere does not become recordable by the passage of time. **No outcome here grants a lesson
authority** — that is not an available result, at any stage, for any lesson.

**Synchronization.** Lessons MUST remain synchronized with the documents they name, and MUST NOT become competing
sources of truth. When a lesson and a governing document disagree, **the document wins** and the lesson is corrected,
superseded, or archived ([CLAUDE.md §3](../../CLAUDE.md), §8). A lesson that has been adopted tracks the rule that
carries it: if that rule changes, the lesson is re-examined, because a lesson explaining a rule that no longer exists is
the most convincing wrong answer in the repository.

---

## 10. Lesson Decision Summary

The load-bearing decisions behind knowledge governance, recorded so they are not silently reversed. **Rejected ideas**
([PRODUCT_DECISIONS §5](../00-product/PRODUCT_DECISIONS.md#5-rejected-ideas)), **architectural decisions**
([ADRs](../01-architecture/decisions/)), **anticipated risks** ([PLAN §8](./IMPLEMENTATION_PLAN.md#8-risks)), **known
debt** ([STATUS §8](./IMPLEMENTATION_STATUS.md#8-technical-debt)), and **incident response**
([SECURITY §18](../01-architecture/SECURITY.md#18-incident-response)) are owned elsewhere and are not restated here.

| Decision                                 | Chosen Approach                                                                              | Alternatives                                                                  | Rationale                                                                                                                                                                                                                                                                  |
|------------------------------------------|----------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Durable learning**                     | Only knowledge that outlives the work that produced it is recorded                           | Record everything learned; let readers judge relevance                        | Most of what a project teaches is about this week and does not keep. Recording it dilutes what does — and dilution is precisely why knowledge documents stop being read, after which nothing in them is preserved at all (§2, §6).                                         |
| **Evidence before lesson**               | A lesson states what supports it, from completed work                                        | Allow experienced judgement to stand as a lesson on its own                   | Without evidence a lesson cannot be **refuted** — there is nothing to show has changed — so it never leaves, and it accrues authority purely by surviving. Unevidenced knowledge is a preference that outlived the argument (Rules, §8).                                   |
| **Observations are not decisions**       | Observation and recommendation are separated; the recommendation goes to the owning document | Record the recommendation with the observation, where the context is freshest | Fused, the recommendation inherits the observation's credibility without its review, and is made by whoever noticed — the person with the least distance from it. That is a rule nobody ratified, obeyed because it was written down (§3).                                 |
| **Lessons do not own behavior**          | A lesson has no authority; it persuades a document that has authority                        | Let proven lessons bind directly — they are, after all, what was learned      | A lesson's persuasiveness is uncoupled from its authority, and only one is visible on the page. Binding lessons directly creates requirements with no home, no reviewer, and no way to argue back without appearing to refuse to learn (§1, §7).                           |
| **Reusable over historical**             | Lessons generalize; history stays with the documents that record it                          | Keep a narrative record of what happened, as context                          | History answers *what occurred*; a lesson answers *what to do differently*, and only the second helps someone who was not there. [STATUS](./IMPLEMENTATION_STATUS.md), the changelog, and incident records already hold the first — better, and with dates (§1, §3).       |
| **Synchronize with source documents**    | A lesson names its owning document and is re-examined when that document moves               | Let lessons stand independently once recorded                                 | An unsynchronized lesson describes a world that has moved on, in a voice that sounds like experience. It is the most convincing wrong answer available, because nothing about it looks stale (§8).                                                                         |
| **Supersede rather than contradict**     | Superseded lessons are marked, not rewritten; obsolete ones are archived, not deleted        | Edit lessons to stay correct; delete what no longer applies                   | *What changed our minds* is usually worth more than the correction, and an archived lesson is what answers "why don't we do it that way?" — always asked by someone about to do it that way (§5, §8).                                                                      |
| **Governance before convenience**        | Acting on a lesson is a change, routed like any other                                        | Let obviously-correct lessons be applied directly                             | "Obviously correct" is the most common reason routing gets skipped, and the resulting change is reviewed by nobody — because everyone assumed the lesson already was the review ([CONTRIBUTING](./CONTRIBUTING.md#4-repository-workflow), §7).                             |
| **Contradiction is a signal, not noise** | Contradictory lessons are treated as evidence of a defect upstream                           | Reconcile them here and move on                                               | Two teams rarely learn contradictory things from the same reality; they learn them from documents that disagree, or from a question no document answered. Reconciling only the lessons leaves the cause intact — and it produces the same contradiction again, later (§8). |

---

*This document governs how durable engineering knowledge is extracted from completed work, recorded, routed, and
retired; it does not override the frozen documents under [`docs/`](../), and **it grants nothing**. A lesson is not a
requirement, not an architecture, not a decision, and not a rule — it is the reasoning that may persuade the document
that owns one. It holds no history ([IMPLEMENTATION_STATUS](./IMPLEMENTATION_STATUS.md) and the release documents do),
no rejected ideas ([PRODUCT_DECISIONS §5](../00-product/PRODUCT_DECISIONS.md#5-rejected-ideas) does), no ratified
decisions (the [ADRs](../01-architecture/decisions/) do), and no
incidents ([SECURITY §18](../01-architecture/SECURITY.md#18-incident-response) does). When a lesson would change a
requirement, an architecture, or a ratified decision, stop and raise it per [CLAUDE.md §8](../../CLAUDE.md) — a lesson
that stays a lesson has not been learned.*
