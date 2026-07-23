# Changelog — LedgerAI

> **Status:** Draft v1 — **the record is empty.** Nothing has been released
> ([STATUS](../03-engineering/IMPLEMENTATION_STATUS.md): Phase 0, M0, Not Started). This document defines how the record
> works and holds it; it invents no history to fill it (§1, *The record today*).
> **Owner:** Principal Release Documentation Architect
> **Last updated:** 2026-07-16
> **Upstream (frozen):
> ** [CLAUDE.md](../../CLAUDE.md) · [CONTRIBUTING](../03-engineering/CONTRIBUTING.md) · [DEPLOYMENT](../03-engineering/DEPLOYMENT.md) · [IMPLEMENTATION_PLAN](../03-engineering/IMPLEMENTATION_PLAN.md) · [IMPLEMENTATION_STATUS](../03-engineering/IMPLEMENTATION_STATUS.md)
> **Related:
> ** [RELEASE_NOTES](./RELEASE_NOTES.md) · [LESSONS_LEARNED](../03-engineering/LESSONS_LEARNED.md) · [PRODUCT_DECISIONS](../00-product/PRODUCT_DECISIONS.md) · [ADRs](../01-architecture/decisions/)

---

## 1. Purpose

### Why this document exists

Every document in this repository is kept true by being **corrected**. When
[IMPLEMENTATION_STATUS](../03-engineering/IMPLEMENTATION_STATUS.md) disagrees with reality, *reality is right* and the
document is fixed. When a frozen document is wrong, it is amended through review. Correction is the mechanism by which
the whole repository stays honest.

This document is the one place where that mechanism would destroy the thing it protects.

The governing principle of this document:

> **Every other document is corrected. This one is only continued.**
>
> A record edited until it is tidy has stopped being evidence — and nothing announces the moment it became fiction. What
> happened does not change; only our account of it can, and that is the failure.

This is not a novel stance here. [PRODUCT_DECISIONS §9](../00-product/PRODUCT_DECISIONS.md#9-change-log) already holds
it for product decisions: *add a new entry, mark the prior one superseded, and log it — never rewrite history in place.*
This document applies the same discipline to the repository's changes, and owns nothing else.

**What changed is not what shipped, what was decided, or what we learned** — and each of those already has an owner.
What no document holds is the **chronological record of the changes themselves**: what changed, when, and how it is
grouped so a future reader can find it. This document owns that, and only that.

It is **not** release notes, **not** a lessons-learned document, **not** an issue tracker, **not** architecture or
product governance, and **not** implementation history. It contains **no issue identifiers, no tickets, no scripts, no
release tooling, no code**, and no product, architecture, design, or AI specifics beyond naming who owns them. It
records *what changed, when it changed, and how that change is grouped for traceability* — never what the change
decided.

### The record today

**Nothing has been released.** [IMPLEMENTATION_STATUS](../03-engineering/IMPLEMENTATION_STATUS.md) reports Phase 0, M0,
0%, *Not Started* — so there is nothing to record, and **the record is empty**.

This is stated rather than left to inference for one reason: an empty changelog is indistinguishable from a lost one. A
future reader finding no entries must be able to tell that **nothing happened** rather than that something happened and
was never written down. The record begins at the first release and grows by §5 from there — never backward, and never by
reconstruction (Rules).

### Relationship to the governing documents

| Document                                                               | Its job                                 | The boundary with this document                                                                                                                                                                                                                                                                                                                                |
|------------------------------------------------------------------------|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [RELEASE_NOTES.md](./RELEASE_NOTES.md)                                 | **What shipped, and for whom**          | **Not yet authored.** It addresses an **audience** and explains shipped behavior in terms of what that audience gains. This document addresses a **future reader of the repository** and records the fact of a change. The same release produces both; they are not drafts of each other, and neither is a summary of the other.                               |
| [LESSONS_LEARNED.md](../03-engineering/LESSONS_LEARNED.md)             | **Reusable engineering knowledge**      | It already states the boundary: this document *"records the fact of a change"*; that one records **what the change taught**. An entry here ages out of relevance once nobody runs that version; a lesson does not. A lesson is never recorded here, and a change is never a lesson (§4).                                                                       |
| [CONTRIBUTING.md](../03-engineering/CONTRIBUTING.md)                   | **How changes are routed and approved** | It routes and approves a change **before** it exists ([§4](../03-engineering/CONTRIBUTING.md#4-repository-workflow)). This document records it **after**. An entry is therefore never how a change gets approved, and a change that skipped routing does not become legitimate by being written down here.                                                     |
| [DEPLOYMENT.md](../03-engineering/DEPLOYMENT.md)                       | **How verified changes become live**    | It owns promotion, rollback, and what is observed ([§5](../03-engineering/DEPLOYMENT.md#5-promotion-lifecycle), [§7](../03-engineering/DEPLOYMENT.md#7-rollback-philosophy)). This document records that a change went live — including that it was **withdrawn**, which is itself a change and is appended, never used to erase the entry that preceded it.   |
| [IMPLEMENTATION_STATUS.md](../03-engineering/IMPLEMENTATION_STATUS.md) | **Live execution state**                | It answers *"where are we now?"* — and it is **corrected** when wrong ([§13](../03-engineering/IMPLEMENTATION_STATUS.md#13-update-rules)); its entries stop mattering once superseded. This answers *"what happened, ever?"* and is **never corrected, only continued** (§1). One is a dashboard; this is a record. Neither is a substitute for the other.     |
| **The [ADRs](../01-architecture/decisions/)**                          | **Ratified architectural decisions**    | An ADR **is** the decision, with its reasoning and its *Future Reconsideration*. An entry records that one was made or superseded and points to it — it never restates one, because a summary of a decision is a second version of it that nobody ratified (Rules, §7).                                                                                        |
| [PRODUCT_DECISIONS.md](../00-product/PRODUCT_DECISIONS.md)             | **Product scope decisions**             | It owns what was decided, deferred, and rejected, and it keeps its **own** change log ([§9](../00-product/PRODUCT_DECISIONS.md#9-change-log)) for how those decisions evolve. This document does not duplicate that log and never records a product judgement — only that a change occurred, and where the decision lives.                                     |
| [CLAUDE.md](../../CLAUDE.md)                                           | **The engineering playbook**            | It owns the hierarchy and precedence (§3) and the stop conditions (§8). This document grants nothing and decides nothing, so it sits below everything and adds no precedence rule of its own. Where an entry and a governing document disagree, **the document is right about the decision** — and the entry is corrected by appending, never by editing (§5). |

In one line each:

> **CHANGELOG records what changed. RELEASE_NOTES describe what shipped and for whom. LESSONS_LEARNED records reusable
> engineering knowledge. CONTRIBUTING governs how changes are routed and approved. DEPLOYMENT governs how verified
> changes become live. IMPLEMENTATION_STATUS records live execution state. ADRs record architectural decisions. Product
> decisions record product scope decisions.**

---

## 2. Changelog Philosophy

These principles explain *why* the record is governed the way it is. They are the reasoning behind the enforceable rules
that follow.

| Principle                                | Why it exists                                                                                                                                                                                                                                                       |
|------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Chronological truth**                  | The record's only claim is that these things happened, in this order. Everything else it might offer — narrative, judgement, tidiness — is available elsewhere and is bought by weakening the one claim nothing else makes.                                         |
| **Traceability over brevity**            | An entry exists to be followed to its source. Shortened past that point it becomes a fragment nobody can act on: it names something that changed and offers no way to find out what it was, which is a citation with the reference removed (§7).                    |
| **Release history over narrative**       | A narrative explains; it also selects, and selection is where a record quietly becomes an argument. The value of history is that it holds the things nobody thought were important — which is what narrative is for removing.                                       |
| **Concise but complete**                 | Both, and the tension is the discipline. Detail belongs to the owning document; the entry carries enough to be found and no more. An entry that reproduces its source has become a copy that will drift from it (§6).                                               |
| **Change visibility**                    | An unrecorded change is one nobody can discover after the fact — the code shows *what is* and never *what became*. The change most worth finding is always the one that nobody thought worth writing down.                                                          |
| **Version clarity**                      | A reader's first question is *does this apply to what I am running?*, and it is unanswerable without a grouping. An entry with no version or release attached is true and useless (§8).                                                                             |
| **No hidden edits**                      | A record that can be quietly altered is not evidence — it is the current opinion of whoever edited it last, formatted as history. That the alteration was an improvement is beside the point: the reader cannot tell, and so must trust nothing (§1).               |
| **No invented history**                  | Reconstructed entries are the most dangerous content this document can hold: they are plausible, well-formed, indistinguishable from real ones, and wrong. A gap is honest. A gap filled from memory is a lie with the same formatting as the truth.                |
| **One entry per meaningful change set**  | Too granular and the record becomes a ledger nobody reads; too coarse and changes hide inside batches. The unit is what a future reader would search for as a single thing — not what happened to be committed together (§6).                                       |
| **Preserve the record of what happened** | Corrections append; they never overwrite ([PRODUCT_DECISIONS §9](../00-product/PRODUCT_DECISIONS.md#9-change-log)). That we were wrong, and when we found out, is frequently worth more than the correction — and it is exactly what rewriting destroys first (§5). |

---

## Changelog Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Every changelog entry MUST correspond to a real change.** *An entry is a claim that something happened. Once one is
  false, every other entry is only as trustworthy as the reader's willingness to check — and nobody checks a record,
  which is the entire reason to keep one.*
- **Every entry MUST be traceable to the owning document or review.** *The entry is a pointer, not the thing. Without
  its source it cannot be verified, understood, or acted on, and it decays into a rumor with a date attached (§7).*
- **Every entry MUST state what changed.** *An entry that names an area without saying what became different has
  recorded that activity occurred. That is not history; it is a heartbeat.*
- **Every entry SHOULD identify the release or version grouping if known.** *The reader's first question is whether it
  applies to what they are running. "If known" is deliberate: LedgerAI has no product version scheme, and this document
  does not invent one (§8).*
- **Every entry MUST NOT become a second source of truth** for product, architecture, design, AI, deployment, or
  knowledge decisions. *A decision summarized here is a second version of it that nobody ratified — and it is the
  version people find first, because a changelog is skimmable and a decision document is not.*
- **Every entry MUST NOT restate ADRs, lessons, or release notes in full.** *A copy drifts from its original while
  continuing to be cited, and there is no rule that says which is right. The entry points; the source says (§7).*
- **Every entry MUST preserve chronology.** *Order is the record's only structure and its only claim. Once it can be
  rearranged, the record answers "what happened" and no longer "what happened first" — which is the question that
  matters when something is wrong (§8).*
- **Every entry MUST remain reviewable against its source.** *An entry nobody could check is not a record; it is an
  assertion that has been left alone long enough to look like one.*
- **Entries SHOULD be concise.** *A record is judged on whether it is read. Length is what makes a changelog into an
  archive — technically complete and functionally absent (§6).*
- **Entries MUST NOT invent history.** *Not for gaps, not for tidiness, not to make a release legible in hindsight. A
  missing entry is a known unknown; an invented one is an unknown, and it will be cited with total confidence by someone
  who had no way to know (§2).*

**Why these rules exist.** A changelog fails in three ways, and all three leave it looking healthy. **False history** —
an entry that is wrong, invented, or reconstructed, and is believed precisely because records are the thing nobody
double-checks. **Duplicated authority** — an entry that summarizes a decision well enough to be quoted instead of it,
becoming a second, unratified source of truth that drifts quietly from the first. **Changelog drift** — the record and
what actually happened diverging, through edits, omissions, or gaps, until the document describes a project that never
existed.

Each is invisible from inside. Nobody sets out to falsify a record; they tidy an entry, summarize helpfully, or fill a
gap they are fairly sure about. These rules exist because this document's only value is that it can be trusted without
being verified — and that property is destroyed by exactly the small, well-intentioned improvements that make every
other document better.

---

## 3. Changelog Model

**What an entry is.** For governance purposes an entry is not a line of text but a **dated, traceable claim about one
meaningful change**: what changed, when, where it belongs, and how to reach the thing that actually decided it. It is a
pointer with a date — its usefulness comes from where it leads, never from what it says.

The conceptual parts below are what an entry must be able to answer. **This is not a schema**: how they are arranged,
labeled, or formatted is not this document's business, and a rigid structure fixed here would be obeyed long after it
stopped fitting.

| Part                              | What it carries                                                                                                                                                                                                                                         |
|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Date**                          | When the change occurred — the fact that makes the record a chronology rather than a list. It is the date of the change, not of the writing, and the two diverge exactly when someone records late (§5).                                                |
| **Version or release identifier** | The grouping a reader uses to ask *does this apply to me?* — where one exists (§8). Recorded when known; **never invented to make an entry look complete**.                                                                                             |
| **Change summary**                | What became different, in enough words to be recognized and searched for, and no more. Not why, not how, not whether it was wise — those belong to the document that decided it.                                                                        |
| **Affected area**                 | Where in the product or repository the change lands, so a reader scanning for their concern can find it. An area is a signpost, not a taxonomy; precision here is [ARCHITECTURE](../01-architecture/ARCHITECTURE.md)'s vocabulary, not a new one.       |
| **Owning document**               | Which document granted or records the decision behind the change. This is what keeps the entry a pointer rather than a claim — and it is the part most often omitted, because at writing time everyone already knows (Rules, §7).                       |
| **Related review or ADR**         | Where relevant: the review that approved it or the [ADR](../01-architecture/decisions/) that decided it. "Relevant" is not "available" — a change carrying a ratified decision without naming it has hidden the most important thing about itself (§7). |

**An entry is complete when a reader who was not there can find what actually happened.** That is the whole test. It is
not whether the entry is well written, nor whether it is thorough — a beautiful entry that dead-ends is worth less than
a plain one that leads somewhere.

---

## 4. Change Scope

**What belongs in the record.** Anything a future reader would need to discover happened. In every case the entry
records the **fact**; the owning document keeps the substance:

| Category                                    | The discipline                                                                                                                                                                                                                                                |
|---------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **User-visible product changes**            | What a professional would notice. The entry records that behavior changed and points to [SRS](../00-product/SRS.md)/[PRD](../00-product/PRD.md); *what the behavior is* is theirs, and *what it means for the user* is [RELEASE_NOTES](./RELEASE_NOTES.md)'s. |
| **Architecture changes**                    | Structure, boundaries, or contracts moved ([ARCHITECTURE](../01-architecture/ARCHITECTURE.md)). Where a ratified decision is behind it, the entry names the [ADR](../01-architecture/decisions/) and stops there (§7).                                        |
| **Design changes**                          | The visual language, components, flows, or what the product says ([DESIGN_SYSTEM](../02-design/DESIGN_SYSTEM.md), [UI_GUIDELINES](../02-design/UI_GUIDELINES.md)). Recorded because they are user-visible, even when no code behavior changed.                |
| **AI changes**                              | A prompt, provider, model, evaluation, or retrieval change reaching production. These change behavior with **no signature to break** ([PROMPTS](../04-ai/PROMPTS.md)) — which makes the record the only place their history is legible at all.                |
| **Engineering changes**                     | Structure, standards, or verification that a future contributor would need to know changed. Not every refactor; the ones that alter what contributors must do (§6).                                                                                           |
| **Deployment changes**                      | A change to how changes become live ([DEPLOYMENT](../03-engineering/DEPLOYMENT.md)) — a gate, an environment, a promotion or rollback rule. A **withdrawal** is also a change, and it is appended, never used to remove what it withdrew (§5).                |
| **Documentation changes affecting meaning** | A change to what a document *decides* is a change to the repository ([CONTRIBUTING §6](../03-engineering/CONTRIBUTING.md#6-documentation-update-rules)). Reformatting is not; rewording a rule is (§6).                                                       |
| **Release packaging changes**               | What a release contains, how it is grouped or identified. This is the record's own structure changing, and it is the one thing here that is entirely a release concern.                                                                                       |

**What is outside scope.** Not because it is unimportant, but because recording it here would either duplicate an owner
or bury the record:

- **Implementation detail** — how something was built. The entry records that it changed; the code and its documents
  hold how.
- **Issue tracking and task lists** — what is planned, assigned, or in progress. That is
  [STATUS](../03-engineering/IMPLEMENTATION_STATUS.md)'s ground, and it is *live state*, not history.
- **Decisions themselves** — the [ADRs](../01-architecture/decisions/) and
  [PRODUCT_DECISIONS](../00-product/PRODUCT_DECISIONS.md) hold them, and PRODUCT_DECISIONS keeps its own change log
  ([§9](../00-product/PRODUCT_DECISIONS.md#9-change-log)) for how they evolve.
- **What a change taught** — [LESSONS_LEARNED](../03-engineering/LESSONS_LEARNED.md)'s.
- **What shipped, for its audience** — [RELEASE_NOTES](./RELEASE_NOTES.md)'s.
- **Trivial changes** — formatting, typos, and mechanical edits that change no meaning (§6).

---

## 5. Entry Lifecycle

An entry has stages because the difference between *a draft someone is still adjusting* and *the record* is the
difference between a document that can be edited and one that cannot (§1).

| Stage          | What it means                                                                                                                                                                                                                                                 |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Proposed**   | A change is judged meaningful enough to record (§6). Most changes are not, and deciding so is the work — an entry proposed for everything produces a ledger, and a ledger is not read.                                                                        |
| **Written**    | Drafted with its parts (§3): what changed, when, where it belongs, and what to follow. It carries nothing yet and nobody may cite it.                                                                                                                         |
| **Reviewed**   | Checked against the checklist (§9) and, crucially, **against its source** — the only check that matters, and the only one that cannot be done later once memory has faded (Rules).                                                                            |
| **Approved**   | Judged accurate and traceable. Approval attaches to **this entry as written**; approving an entry is never approving the change it describes, which was approved long before ([CONTRIBUTING](../03-engineering/CONTRIBUTING.md#7-review-and-approval-rules)). |
| **Published**  | Part of the record. **From this point it is not edited** (§1). It may be superseded, never revised — this is the stage boundary the whole document exists to protect.                                                                                         |
| **Superseded** | A later entry corrects it. Both stand: the original and the correction, in order. *What we thought, and when we found out otherwise*, is the part a record uniquely preserves — and rewriting is the one action that destroys it irrecoverably (§2).          |
| **Archived**   | Grouped away as a release recedes — old, not untrue. Archiving changes an entry's **prominence**, never its content or its order. An archived entry that cannot be read is a deleted one with extra steps.                                                    |

**Every stage has an exit.** *Proposed* ends when the change is judged meaningful or set aside as trivial (§6).
*Reviewed* ends in approval or in a finding — an entry that cannot be checked against its source does not get published,
because an unverifiable entry is the one failure this record cannot survive. *Published* is terminal: it ends only into
*Superseded* or *Archived*, and **never back into an editable state**.

**In-progress entry versus released record.** An entry before *Published* is a **draft**: it can be rewritten,
corrected, argued over, or discarded, and nothing depends on it. After *Published* it is **the record**: it can only be
added to. The two look identical and behave oppositely, which is why the boundary is named rather than assumed.
Everything this document protects lives on one side of it.

---

## 6. Entry Granularity

**The record is a record of significance, not a line-by-line ledger.** Both failure modes destroy it and only one is
obvious. Too coarse, and a change hides inside a batch nobody can decompose. Too granular — the common failure — and the
record is complete, unreadable, and therefore unread, which produces exactly the same outcome as having no record while
costing more.

| Guidance                                    | What it means                                                                                                                                                                                                               |
|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Meaningful change sets are captured**     | The unit is what a future reader would look for as **one thing** — not what happened to be delivered together. Delivery grouping is an accident of scheduling; the reader's question is not.                                |
| **Trivial changes need not be highlighted** | Formatting, typos, and mechanical edits that change no meaning. The test is meaning, not size: a one-word change to a rule is meaningful, and a large reformatting is not (§4).                                             |
| **Decision changes belong in the record**   | When a decision is made, superseded, or reversed, that is precisely what a future reader is looking for. The entry records that it happened and points to the decision; it never carries the decision (Rules).              |
| **Grouped changes may be consolidated**     | Where several changes belong to one release and one concern, one entry may carry them — if it stays followable. Consolidation is for the reader's benefit; the moment it hides something, it has become the coarse failure. |
| **Accidental duplication is avoided**       | One change, one entry. The same change recorded twice — under two areas, or in two releases — makes the record's own chronology unreliable, which is the only thing it offers (§8).                                         |

**When in doubt, ask what a reader would search for.** Not what was done, not what was hard, and not what took the most
effort — those produce a work log. The record answers a question asked in the future by someone with a problem: *when
did this change, and what changed it?* An entry that would not be found by that question is not helping, whatever it
cost to write.

---

## 7. Cross-Referencing

**An entry is a pointer.** Its value is entirely in where it leads: the entry says a change happened, and the source
says what the change *was* and why it was allowed. Strip the reference and what remains is a dated assertion — true,
perhaps, and unusable.

| Reference                  | What it establishes                                                                                                                                                                                                                                                                   |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Owning document**        | Which document granted the change ([CONTRIBUTING §4](../03-engineering/CONTRIBUTING.md#4-repository-workflow)). This is what makes the entry checkable, and it is the reference most often skipped — at writing time it is obvious, and it is obvious to nobody later.                |
| **Relevant ADR**           | Where a ratified decision is behind the change ([ADRs](../01-architecture/decisions/)). Naming it is how the entry avoids becoming a summary of it (Rules) — an entry that describes an architectural decision without naming the ADR has replaced it for anyone skimming.            |
| **Deployment record**      | Where the change went live, or was withdrawn ([DEPLOYMENT §5](../03-engineering/DEPLOYMENT.md#5-promotion-lifecycle)). *Merged* and *live* are different events with different dates, and conflating them makes the chronology wrong in the only way that matters during an incident. |
| **Release note linkage**   | Where the change was communicated to an audience ([RELEASE_NOTES](./RELEASE_NOTES.md)). The two describe one release from different sides; the link is what lets a reader move between *what changed* and *what it meant*.                                                            |
| **Traceability to source** | Whatever else lets a reader reach the change itself. The requirement is the **property**, not any particular reference: the entry must be followable to the thing it describes.                                                                                                       |

**Traceability matters for a reader who is not here yet.** Everyone writing an entry already knows what it refers to —
that is precisely why references get omitted, and precisely why the omission is invisible until the knowledge is gone.
The reader who needs the reference is the one with a problem, under time pressure, years later, asking *when did this
change and who allowed it?* They cannot ask anyone. The reference is the entire answer, and it costs nothing at writing
time and cannot be recovered afterward.

*How a reference is written is not this document's business* — only that it exists and resolves.

---

## 8. Chronology and Versioning

**Order is the record's only structure**, and it carries the one claim nothing else in the repository makes: that these
things happened, in this sequence. Everything in this section protects that.

**Newest first.** The record is read in reverse chronological order, and this is a deliberate choice of this document.
The dominant question is *what changed recently* — asked by someone upgrading, debugging, or catching up — and a record
that grows forever must not require reaching its end to answer it. The record is also read most often at its newest
point and least often at its oldest, and putting the rarely-read part behind the frequently-read one is a cost paid on
every read. Oldest-first is defensible for a document read as a narrative from the start; this one is not read that way,
and never will be.

| Concern                             | The discipline                                                                                                                                                                                                                                                               |
|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Date consistency**                | Dates mean one thing throughout — when the change occurred, not when it was written (§3). Mixed meanings make the ordering unreliable while every individual entry stays true, which is undetectable by inspection.                                                          |
| **Version boundaries**              | Where one grouping ends and the next begins. A boundary is a fact about the release, recorded — never adjusted afterward so entries fall more tidily on one side of it.                                                                                                      |
| **Release grouping**                | Entries are grouped by whatever the frozen documents define — for LedgerAI today, the **milestones** ([IMPLEMENTATION_PLAN §4](../03-engineering/IMPLEMENTATION_PLAN.md#4-milestones)), where M6 is the MVP/beta of [PRD §15](../00-product/PRD.md#15-release-scope-moscow). |
| **Avoiding gaps**                   | A missing entry is a hole in the only claim this document makes. When one is found, it is recorded as found — with what is actually known — and **never reconstructed to look continuous** (Rules).                                                                          |
| **Avoiding duplicates**             | One change appears once. The same change under two groupings makes both wrong about when it happened (§6).                                                                                                                                                                   |
| **A release spanning many changes** | The normal case: a release is a grouping of entries, not an entry. Each change keeps its own date and its own traceability, and the grouping sits over them — collapsing a release into one entry destroys the chronology inside it, which is where incidents live.          |

### There is no product version scheme, and this document does not invent one

The frozen documents own versioning of **their own artifacts** — the API
([API_SPEC §20](../01-architecture/API_SPEC.md#20-api-versioning-strategy): URI versioning, new major only for breaking
changes), requirements ([SRS §14](../00-product/SRS.md#14-requirement-versioning)), flows
([USER_FLOWS](../02-design/USER_FLOWS.md#flow-versioning)), and prompts
([PROMPTS §6](../04-ai/PROMPTS.md#6-prompt-versioning)). **No document defines a product or release version scheme**,
and this one does not fill that gap: choosing one would be significant, precedent-setting, and hard to reverse — an
**ADR**, raised per [CLAUDE.md §8](../../CLAUDE.md), not a changelog decision made because a column looked empty.

Until such a scheme exists, entries are grouped by what the frozen documents already provide — the milestones above —
and the version part of an entry (§3) is recorded **only when known**. An invented version is worse than an absent one:
absent is honest, and invented is a claim the repository never made, in the field readers trust most.

---

## 9. Changelog Review Checklist

Every entry is assessed against this checklist before it is published. A "no" is a finding to resolve, not a detail to
defer — after publication there is no revision, only supersession (§5).

- [ ] **Corresponds to a real change?** — It happened; nothing here is reconstructed, anticipated, or inferred (Rules).
- [ ] **Source identified?** — The owning document, and the review or ADR where relevant (§7).
- [ ] **Chronology preserved?** — Dated by when it occurred, in order, creating no gap or duplicate (§8).
- [ ] **Ownership traceable?** — A reader can reach the document that granted the change, not merely the area it touched
  (§3, §7).
- [ ] **No duplicated authority?** — It restates no decision, ADR, lesson, or release note; it points to them (Rules).
- [ ] **No invented history?** — Nothing filled in for tidiness or completeness; what is unknown is left unclaimed (§2).
- [ ] **Wording concise?** — Enough to be recognized and searched for; the substance stays with its owner (§6).
- [ ] **Review complete?** — Checked **against its source**, not only for plausibility (§5).
- [ ] **Safe to publish?** — Accurate as written, because publication is the point after which it cannot be revised
  (§5).
- [ ] **Consistent with release records?** — It agrees with what [RELEASE_NOTES](./RELEASE_NOTES.md) and
  [STATUS](../03-engineering/IMPLEMENTATION_STATUS.md) say about the same release; a disagreement is a finding, not a
  formatting difference (§8).

---

## Changelog Review Process

> *Unnumbered governance section. It defines when an entry is reviewed and how changelog governance evolves —
> deliberately, not by accretion.*

**Review triggers** — a changelog review is required when any of the following occurs:

- **A new release** — the grouping itself is recorded, and every entry inside it is checked against its source (§8).
- **A meaningful change set** — the unit an entry records (§6).
- **A release packaging change** — what a release contains, how it is grouped or identified.
- **A documentation change affecting meaning** — a change to what a document *decides*
  ([CONTRIBUTING §6](../03-engineering/CONTRIBUTING.md#6-documentation-update-rules)). Reformatting is not a trigger;
  rewording a rule is.
- **An architecture change** — structure, boundaries, or a ratified decision
  ([ARCHITECTURE](../01-architecture/ARCHITECTURE.md)).
- **An AI change** — a prompt, provider, model, evaluation, or retrieval change reaching production (§4).
- **A deployment change** — a change to how changes become live, or a withdrawal
  ([DEPLOYMENT](../03-engineering/DEPLOYMENT.md#deployment-review-process)).
- **A product behavior change** — anything a professional would notice ([SRS](../00-product/SRS.md)).
- **A cross-document inconsistency** — the record and another document disagree about the same release. This is a defect
  already realized, not a risk of one.
- **A superseded entry** — new information contradicts a published one; it is corrected by appending (§5).

**Review outcomes** — each review resolves to exactly one:

- **Approved** — accurate, traceable, and safe to publish as written (§9).
- **Refinement required** — the change is real but the entry is not yet publishable: an unnamed source, a summary that
  has become a second version of a decision, a date that means the wrong thing.
- **Release note review required** — the change needs communicating to an audience, which is a different act with a
  different owner; routed to [RELEASE_NOTES](./RELEASE_NOTES.md) (**not yet authored** — while it does not exist, such a
  change owes no separate release-note review and waits for none).
- **Documentation review required** — the entry reveals that an owning document does not describe what happened; routed
  there ([CONTRIBUTING §4](../03-engineering/CONTRIBUTING.md#4-repository-workflow)). **The record is never the fix for
  a document being wrong.**
- **Architecture review required** — routed to the
  [Review Process](../03-engineering/IMPLEMENTATION_PLAN.md#review-process)'s architecture triggers.
- **AI review required** — routed to the owning AI process
  ([CONTRIBUTING §7](../03-engineering/CONTRIBUTING.md#7-review-and-approval-rules)).
- **Deployment review required** — routed to the
  [Deployment Review Process](../03-engineering/DEPLOYMENT.md#deployment-review-process).
- **ADR required** — the change carries a decision that is significant, precedent-setting, or hard to reverse
  ([CLAUDE.md §8](../../CLAUDE.md)) — including any proposal to introduce a product version scheme (§8).

Only **Approved** publishes an entry; **Refinement required** returns it to its author; every other outcome holds it
until the review it was routed to returns. An entry held there is not published by the passage of time — and **no
outcome edits a published entry**, which is not an available result at any stage (§5).

**Synchronization.** Changelog governance MUST remain synchronized with [CLAUDE.md](../../CLAUDE.md),
[CONTRIBUTING](../03-engineering/CONTRIBUTING.md), [DEPLOYMENT](../03-engineering/DEPLOYMENT.md),
[RELEASE_NOTES](./RELEASE_NOTES.md), [IMPLEMENTATION_STATUS](../03-engineering/IMPLEMENTATION_STATUS.md), and the
relevant [ADRs](../01-architecture/decisions/). When this document and one of them disagree, **they win** and this
document is corrected ([CLAUDE.md §3](../../CLAUDE.md)) — *this document*, meaning its governance. **A published entry
is never corrected to agree with anything**; if the record and a document disagree about what happened, that is a
finding about both, and it is resolved by appending here and correcting there (§5).

---

## 10. Changelog Decision Summary

The load-bearing decisions behind the record, recorded so they are not silently reversed. **Product decisions and their
evolution** ([PRODUCT_DECISIONS §9](../00-product/PRODUCT_DECISIONS.md#9-change-log)), **architectural decisions**
([ADRs](../01-architecture/decisions/)), **live state** ([STATUS](../03-engineering/IMPLEMENTATION_STATUS.md)),
**artifact versioning** ([API_SPEC §20](../01-architecture/API_SPEC.md#20-api-versioning-strategy),
[SRS §14](../00-product/SRS.md#14-requirement-versioning)), and **release grouping**
([IMPLEMENTATION_PLAN §4](../03-engineering/IMPLEMENTATION_PLAN.md#4-milestones)) are owned elsewhere and are not
restated here.

| Decision                                | Chosen Approach                                                                     | Alternatives                                                        | Rationale                                                                                                                                                                                                                                                                                                               |
|-----------------------------------------|-------------------------------------------------------------------------------------|---------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Appended, never amended**             | A published entry is never edited; corrections are appended and the original stands | Correct entries in place so the record is always accurate           | A record edited until it is tidy has stopped being evidence, and nothing announces when it became fiction. *What we thought, and when we found out otherwise*, is the part only a record preserves — and rewriting destroys it first ([PRODUCT_DECISIONS §9](../00-product/PRODUCT_DECISIONS.md#9-change-log), §1, §5). |
| **Chronological record**                | Order is the record's structure and its only claim                                  | Organize by area, component, or theme for easier scanning           | Every other organization is available elsewhere and better there. Order is the one thing no other document offers, and it is what matters when something is wrong and the question is *what happened first* (§8).                                                                                                       |
| **Change over narrative**               | Entries record that a change occurred; they explain nothing                         | Write a readable story of each release                              | Narrative selects, and selection turns a record into an argument. The value of history is that it keeps what nobody thought was important — which is exactly what narrative removes (§2).                                                                                                                               |
| **Traceable entries**                   | Every entry points to the document, review, or ADR behind it                        | Keep entries self-contained so readers need not follow links        | Self-contained means duplicated, and a duplicate drifts from its source while still being cited — with no rule saying which is right. The entry is a pointer; its whole value is where it leads (§7, Rules).                                                                                                            |
| **One entry per meaningful change set** | The unit is what a future reader would search for as one thing                      | One entry per change; or one entry per release                      | Per-change produces a ledger that is complete and unread — the same outcome as no record, at higher cost. Per-release hides changes inside batches nobody can decompose. The reader's question sets the unit (§6).                                                                                                      |
| **No invented history**                 | Gaps are recorded as gaps; nothing is reconstructed                                 | Reconstruct missing entries from available evidence                 | A reconstructed entry is plausible, well-formed, indistinguishable from a real one, and wrong. A gap is a known unknown; a filled gap is an unknown unknown, cited with total confidence by someone with no way to check (§2, Rules).                                                                                   |
| **Version recorded, never invented**    | The version grouping is recorded only when known                                    | Assign version identifiers here so every entry is complete          | No frozen document defines a product version scheme, and inventing one is an ADR, not a changelog decision. An invented version is a claim the repository never made, placed in the field readers trust most (§8, [CLAUDE.md §8](../../CLAUDE.md)).                                                                     |
| **Release-aware grouping**              | Entries group by what the frozen documents already define — today, the milestones   | Define a release-grouping scheme here                               | Grouping exists ([IMPLEMENTATION_PLAN §4](../03-engineering/IMPLEMENTATION_PLAN.md#4-milestones)); a second scheme would be a second answer with no rule choosing between them. Using an owner's vocabulary costs nothing and cannot drift from it (§8).                                                                |
| **Newest first**                        | The record reads in reverse chronological order                                     | Oldest first, so the record reads as a narrative from the beginning | The dominant question is *what changed recently*, and a record that grows forever must not require reaching its end to answer it. It is read most at its newest point and least at its oldest; the other order taxes every read (§8).                                                                                   |
| **Governance over recollection**        | An entry is reviewed against its source before publication                          | Trust the author, who was there and knows what happened             | The author's memory is the one thing that cannot be checked later, and it is at its most confident exactly when it is unverifiable. This record's only value is that it can be trusted **without** being verified — which requires that it once was (§5, §9).                                                           |

---

*This document holds LedgerAI's chronological record of meaningful changes — what changed, when, and how it is grouped
so a future reader can find it; it does not override the frozen documents under [`docs/`](../), and it grants, decides,
and explains nothing. It records what shipped without describing it ([RELEASE_NOTES](./RELEASE_NOTES.md) does), what
changed without deciding it (the [ADRs](../01-architecture/decisions/) and
[PRODUCT_DECISIONS](../00-product/PRODUCT_DECISIONS.md) do), what happened without teaching from
it ([LESSONS_LEARNED](../03-engineering/LESSONS_LEARNED.md) does), and what occurred without tracking what is
occurring ([IMPLEMENTATION_STATUS](../03-engineering/IMPLEMENTATION_STATUS.md) does). The record is currently empty
because nothing has been released, and it will be filled by things happening — never by anyone deciding what should
have. When an entry would require a version scheme, restate a decision, or correct history in place, stop and raise it
per
[CLAUDE.md §8](../../CLAUDE.md): every other document here is corrected; this one is only continued.*
