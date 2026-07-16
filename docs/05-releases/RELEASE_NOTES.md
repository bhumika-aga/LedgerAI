# Release Notes — LedgerAI

> **Status:** Draft v1 — **the record is empty.** Nothing has been released
> ([STATUS](../03-engineering/IMPLEMENTATION_STATUS.md): Phase 0, M0, Not Started). This document defines how the record
> works and holds it; it invents no releases to fill it (§1, *The record today*).
> **Owner:** Principal Release Communications Architect
> **Last updated:** 2026-07-16
> **Upstream (frozen):
** [CLAUDE.md](../../CLAUDE.md) · [CHANGELOG](./CHANGELOG.md) · [PRD](../00-product/PRD.md) · [SRS](../00-product/SRS.md) · [UI_GUIDELINES](../02-design/UI_GUIDELINES.md) · [DEPLOYMENT](../03-engineering/DEPLOYMENT.md)
> **Related:
** [CONTRIBUTING](../03-engineering/CONTRIBUTING.md) · [LESSONS_LEARNED](../03-engineering/LESSONS_LEARNED.md) · [IMPLEMENTATION_STATUS](../03-engineering/IMPLEMENTATION_STATUS.md) · [PRODUCT_DECISIONS](../00-product/PRODUCT_DECISIONS.md)

---

## 1. Purpose

### Why this document exists

Every other document in this repository is read by someone who chose to read it — a contributor, a reviewer, someone
looking for an answer. They can be misled and recover; the cost lands inside the team.

This one is different.

The governing principle of this document:

> **This is the only document here that a user reads.**
>
> Everything else is the team talking to itself. This is the product talking to a Chartered Accountant who is deciding
> whether to keep trusting it with their client's documents — and who did not ask what changed.

That single fact settles most of what follows. Because a release note is **product surface**, it speaks in the
product's voice rather than inventing one (§2). Because it is read by a professional, it makes only claims the product's
documents already grant (Rules). And because its reader did not come looking, it earns their attention or wastes it —
there is no third outcome.

**What shipped is not what changed.** [CHANGELOG](./CHANGELOG.md) already holds the chronological record of changes, and
it states the boundary from its side: this document *"addresses an audience and explains shipped behavior in terms of
what that audience gains"*, while that one *"addresses a future reader of the repository and records the fact of a
change"* — **the same release produces both; they are not drafts of each other, and neither is a summary of the other.**
This document owns the audience-facing record, and only that.

It is **not** a changelog, **not** a lessons-learned document, **not** an issue tracker, **not** implementation history,
**not** product or architecture governance, and **not** a deployment guide. It contains **no issue identifiers, no
tickets, no scripts, no release tooling, no code**, and no product, architecture, design, or AI specifics beyond naming
who owns them. It records *what shipped, how it is grouped for its audience, and what a release means to the reader* —
never what the product should do, nor why it was built that way.

### The record today

**Nothing has been released.** [IMPLEMENTATION_STATUS](../03-engineering/IMPLEMENTATION_STATUS.md) reports Phase 0, M0,
0%, *Not Started* — so there is nothing to describe, and **the record is empty**.

Stated rather than left to inference, for the same reason the changelog states it: an empty record is indistinguishable
from a lost one. A reader finding no notes must be able to tell that **nothing has shipped** rather than that something
shipped and was never announced — a distinction that matters more here than anywhere else, because this reader is
deciding whether the product is being straight with them. The record begins at the first release and grows by §5 from
there — never backward, and never by reconstruction (Rules).

### Who the reader is

The audience is the professional the product exists for: **Chartered Accountants, CPAs, auditors, and accounting
associates** ([PRODUCT_VISION §6](../00-product/PRODUCT_VISION.md#6-target-users),
[PRD §6](../00-product/PRD.md#6-target-users)) — in the MVP, a single professional per account. Defining them is those
documents' job, not this one's. What follows from it is this one's: the reader is an expert in their own domain and a
non-expert in ours, they are mid-task when they read this, and they are accountable to a client for whatever they do
next.

### The voice is not this document's to invent

[UI_GUIDELINES](../02-design/UI_GUIDELINES.md#3-tone-and-voice) owns how LedgerAI speaks: *professional, calm, direct,
respectful, confident without being overbearing — the voice of a competent colleague who explains something once,
accurately, and then gets out of the way.* Its governing principle is that **the product speaks with one voice**.

A release note is the same product speaking to the same professional. So it **adopts** that voice rather than defining a
second one — not because that document governs this one, but because a second answer to "how does LedgerAI sound?"
invented somewhere nobody thought to look is precisely how one voice becomes two. Where a note needs a word for a
concept, it uses the product's word ([UI_GUIDELINES §4](../02-design/UI_GUIDELINES.md#4-microcopy-principles)); where it
needs a tone, it has one already.

This is the reason "release notes" and "marketing" are different crafts here. *Routine work is not celebrated* is
already the product's rule; positioning claims belong to
[PRODUCT_VISION §12](../00-product/PRODUCT_VISION.md#12-competitive-positioning) and are not made here (§2).

### Relationship to the governing documents

| Document                                                               | Its job                                 | The boundary with this document                                                                                                                                                                                                                                                                                                                        |
|------------------------------------------------------------------------|-----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [CHANGELOG.md](./CHANGELOG.md)                                         | **What changed**                        | It records the **fact** of each change, for a repository reader, and owns the chronology of changes ([§8](./CHANGELOG.md#8-chronology-and-versioning)). This describes **what shipped and why it matters**, for the professional. A note is **derived from** the changelog and never a summary of it: one release, two records, two readers (§7).      |
| [LESSONS_LEARNED.md](../03-engineering/LESSONS_LEARNED.md)             | **Reusable engineering knowledge**      | It holds what the work **taught the team** — internal, generalized, and about how the work went. Nothing there is ever addressed to a user, and nothing here is ever a lesson. The two never meet in a release.                                                                                                                                        |
| [CONTRIBUTING.md](../03-engineering/CONTRIBUTING.md)                   | **How changes are routed and approved** | It routes and approves a change **before** it exists ([§4](../03-engineering/CONTRIBUTING.md#4-repository-workflow)). By the time a note is written, the change was approved long ago — a note is never how something becomes legitimate, and describing an unapproved change well does not make it shipped.                                           |
| [DEPLOYMENT.md](../03-engineering/DEPLOYMENT.md)                       | **How verified changes become live**    | It owns promotion, rollback, and observation ([§5](../03-engineering/DEPLOYMENT.md#5-promotion-lifecycle), [§7](../03-engineering/DEPLOYMENT.md#7-rollback-philosophy)). This says nothing about *how* anything is deployed. It says what the reader now has — and, when something is **withdrawn**, that too, because that is also what shipped (§4). |
| [IMPLEMENTATION_STATUS.md](../03-engineering/IMPLEMENTATION_STATUS.md) | **Live execution state**                | It answers *"where are we now?"* for the team, and is **corrected** when it disagrees with reality ([§13](../03-engineering/IMPLEMENTATION_STATUS.md#13-update-rules)). This answers *"what do I now have?"* for the user, and — like the changelog — is **never corrected once published**, only superseded (§5).                                     |
| **The [ADRs](../01-architecture/decisions/)**                          | **Ratified architectural decisions**    | An ADR is a decision and its reasoning, written for engineers. A note never restates, summarizes, or explains one: users are not the audience for architecture, and an architectural decision paraphrased for a professional is a claim nobody ratified in words nobody reviewed (Rules, §4).                                                          |
| [PRODUCT_DECISIONS.md](../00-product/PRODUCT_DECISIONS.md)             | **Product scope decisions**             | It owns what was decided, deferred, and rejected, and the [boundaries](../00-product/PRODUCT_DECISIONS.md#2-product-boundaries) the product does not cross. A note describes what shipped **inside** them; it never announces a capability the boundaries exclude, however well it would read.                                                         |
| [PRD.md](../00-product/PRD.md) · [SRS.md](../00-product/SRS.md)        | **What the product does, precisely**    | They grant the behavior and own what it is worth ([PRD §15](../00-product/PRD.md#15-release-scope-moscow)). A note **describes** granted behavior in the reader's terms; it never grants any. If a note would say the product does something no document says it does, the note is wrong — that is not a wording problem (Rules).                      |

In one line each:

> **RELEASE_NOTES describe what shipped and for whom. CHANGELOG records what changed. LESSONS_LEARNED records reusable
> engineering knowledge. CONTRIBUTING governs how changes are routed and approved. DEPLOYMENT governs how verified
> changes become live. IMPLEMENTATION_STATUS records live execution state. ADRs record architectural decisions. Product
> decisions record product scope decisions.**

---

## 2. Release Notes Philosophy

These principles explain *why* the audience-facing record is governed the way it is. They are the reasoning behind the
enforceable rules that follow.

| Principle                              | Why it exists                                                                                                                                                                                                                                                                     |
|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Audience first**                     | The reader did not come looking; they were interrupted mid-task. Every note is written from what *they* now need to know — not from what the team did, which is the natural thing to write and the thing they did not ask.                                                        |
| **Shipped truth**                      | A note describes what is actually live. A note describing what was *intended* is not optimistic, it is false — and the reader discovers it by trying, which is the most expensive way for anyone to learn anything about this product.                                            |
| **Release clarity**                    | The reader's first question is *does this apply to what I have?* A note that cannot be placed in a release is information they cannot act on, however accurate (§8).                                                                                                              |
| **Traceability over flourish**         | A note must be checkable against what actually shipped (§7). Flourish is the first thing that makes it unverifiable, because it describes an impression rather than a change — and impressions cannot be wrong, only disappointing.                                               |
| **Concise but useful**                 | Both, and neither wins by default. The reader will give this a few seconds; the note that survives is the one that says what changed for them and stops. Length here is not thoroughness — it is a transfer of work from writer to reader (§6).                                   |
| **No hidden changes**                  | Something a professional would want to know, left out because it is awkward, teaches them that the record is curated. Once suspected, every note is discounted — including the true ones, and including the important ones (§4).                                                  |
| **No invented releases**               | A release that did not happen, a date that was not real, a capability described before it shipped. This is the failure that ends trust outright: the reader has no way to verify anything here, so the document works only while it has never once been wrong.                    |
| **No duplicate authority**             | A note describes behavior; it never defines it. A note that becomes the clearest description of what the product does has become the place people look — and it is not reviewed, versioned, or owned as a specification ([SRS](../00-product/SRS.md)).                            |
| **User value over internal detail**    | The reader does not care what it took. Internal detail is not merely irrelevant to them; it crowds out the sentence they needed and signals the note was written for us (§4).                                                                                                     |
| **Stable meaning over marketing tone** | *Routine work is not celebrated* ([UI_GUIDELINES §3](../02-design/UI_GUIDELINES.md#3-tone-and-voice)). Enthusiasm reads as a sales pitch to a professional evaluating a working instrument — it makes small things sound large, and then the large ones have nothing left to say. |

---

## Release Notes Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Every release note entry MUST correspond to a real release.** *A note is a statement to a user about what they now
  have. Once one is false, the reader cannot verify any of the others — they have no access to the repository — so the
  document's entire value rests on it never having happened.*
- **Every note MUST be traceable to the changelog and owning documents.** *The note is the reader's view; the
  [changelog](./CHANGELOG.md) and the owning documents are the evidence. A note that cannot be checked against them is
  an assertion the team cannot audit either (§7).*
- **Every note MUST describe what shipped, not how it was built.** *How it was built is neither knowable nor useful to
  the reader, and including it signals the note was written for the writer. What shipped is the only thing they can act
  on.*
- **Every note SHOULD be understandable by the intended audience.** *An expert in accounting, not in our
  system ([PRD §6](../00-product/PRD.md#6-target-users)). A note requiring internal vocabulary has been written to the
  wrong person, and the register that fixes it already
  exists ([UI_GUIDELINES §3](../02-design/UI_GUIDELINES.md#3-tone-and-voice)).*
- **Every note MUST NOT become a second source of truth** for product, architecture, design, AI, deployment, or
  knowledge decisions. *A well-written note is often the clearest description of a behavior anywhere — which is exactly
  what makes it dangerous. It is unreviewed as a specification, and it is what people will find first.*
- **Every note MUST NOT restate changelog entries in full.** *That produces a changelog with worse formatting and a
  second chronology to keep in sync. The note answers a different question, for a different reader — if it can be
  swapped for the changelog, one of the two is unnecessary (§1).*
- **Every note MUST NOT invent release content or release history.** *Not to fill a gap, not to make a release feel
  substantial, and not to describe something almost shipped. To this reader every note is unfalsifiable; that is exactly
  why inventing one is not a small liberty (§2).*
- **Every note MUST preserve release grouping.** *A change attributed to the wrong release tells the reader they have
  something they do not, or lack something they have. Both are worse than saying nothing (§8).*
- **Every note SHOULD remain concise.** *The reader gives this seconds. Concision is not politeness here; it is the
  condition under which the note is read at all, and an unread note and an absent note are the same note (§6).*
- **Every note MUST remain reviewable against its source.** *Checked against what shipped, not against whether it reads
  well. A note is most persuasive precisely when nobody can check it — which is its normal condition, for everyone
  except us (§5).*

**Why these rules exist.** These notes are the one thing here a user can neither verify nor cross-check. They see the
note; they do not see the changelog, the review, or the decision behind it. That asymmetry is the whole reason this
document is governed at all, and it fails in three ways. **False release history** — a note that is wrong, early, or
invented, believed completely because the reader has no alternative and discovered only when they act on it.
**Duplicated authority** — a note that describes behavior so well it becomes the specification people cite, unreviewed
and unversioned. **Release-note drift** — the notes and what actually shipped diverging, through omissions, optimism, or
gaps, until the document describes a product the reader does not have.

Each looks like good communication at the time. Nobody sets out to mislead a professional; they smooth an awkward
omission, describe something a little ahead of itself, or explain a behavior so clearly it becomes the reference. These
rules exist because this reader extends trust to the product on the strength of this document — and trust that has never
been tested is indistinguishable from trust that has been earned, right up until it is.

---

## 3. Release Notes Model

**What a note is.** For governance purposes a note is not a paragraph but a **claim, addressed to a professional, about
what a real release gives them** — grouped so they can tell whether it applies, and traceable so the team can verify it.
Its usefulness is measured entirely by what the reader can do differently for having read it.

The parts below are what a note must be able to answer. **This is not a schema**: how they are arranged, labelled, or
formatted is not this document's business, and a rigid structure fixed here would be obeyed long after it stopped
fitting.

| Part                              | What it carries                                                                                                                                                                                                                                                    |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Release identifier**            | The grouping the reader uses to ask *do I have this?* — recorded from what the frozen documents already provide, never invented to make a note look finished (§8).                                                                                                 |
| **Date**                          | When the release reached the reader. Not when it was merged, decided, or written — those are other events with other dates, and only one of them is the reader's ([DEPLOYMENT §5](../03-engineering/DEPLOYMENT.md#5-promotion-lifecycle)).                         |
| **Audience-facing summary**       | What is now different, in the reader's terms and the product's voice ([UI_GUIDELINES §3](../02-design/UI_GUIDELINES.md#3-tone-and-voice)). It describes behavior the frozen documents grant; it never explains the engineering, and never argues the value.        |
| **Visible impact**                | What the reader can now do, must now do, or should expect to be different. This is the part they came for and the part most often missing — a summary that describes a change without naming its consequence has answered *what happened* and not *what it means*. |
| **Linked changelog grouping**     | The [changelog](./CHANGELOG.md) entries this release covers. It is what makes the note checkable and what keeps it from becoming a second record (§7).                                                                                                             |
| **Linked owning document or ADR** | Where relevant: the document that granted the behavior, or the [ADR](../01-architecture/decisions/) behind it. For the team's verification, not for the reader's education — a note never explains an ADR (§7).                                                    |

**A note is complete when the reader knows what is different for them and can tell whether it applies.** That is the
whole test. Not whether it is thorough, and not whether it reads well: a graceful note that leaves the professional
unsure what changed has failed at the only thing it was for.

---

## 4. Audience and Impact

**What belongs in the record.** The test is a single question — *would this professional want to know?* — and it is
applied honestly, including when the answer is inconvenient (§2).

| Category                                                 | The discipline                                                                                                                                                                                                                                                                                                                                                        |
|----------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **User-visible product changes**                         | The ordinary case: something the professional would notice. Described as behavior the frozen documents grant ([SRS](../00-product/SRS.md)), in their terms, never as a feature announcement.                                                                                                                                                                          |
| **Significant AI behavior changes**                      | AI output changes with **no signature to break** ([PROMPTS](../04-ai/PROMPTS.md)) — the professional may never notice it changed, only that their work feels different. That invisibility is exactly why it is told, and why it is never overstated: the product assists and is reviewed, never a system of record ([BR-032](../00-product/SRS.md#5-business-rules)). |
| **Significant design changes**                           | Where the interface a professional relies on has moved. They built habits here; a change that costs them a moment is worth a sentence, however small it looks from inside.                                                                                                                                                                                            |
| **Architecture changes that affect users**               | Only where the reader would notice — and then described **as its effect**, never as architecture. The [ADRs](../01-architecture/decisions/) hold the decision; users are not their audience (§1).                                                                                                                                                                     |
| **Engineering changes affecting release behavior**       | Where the professional's experience of the product changes, even without a feature changing. Rare, and mostly invisible; when it is not invisible, it is theirs to know.                                                                                                                                                                                              |
| **Deployment or rollback release impact**                | Including that something was **withdrawn** ([DEPLOYMENT §7](../03-engineering/DEPLOYMENT.md#7-rollback-philosophy)). A capability a professional used and no longer has is the single most important thing this document can tell them — and the hardest to volunteer.                                                                                                |
| **Documentation changes that alter meaning for readers** | Where what the product tells a professional has changed in substance, not in wording. If the guidance they follow now means something different, that shipped too.                                                                                                                                                                                                    |

**What is outside scope.** Not unimportant — owned elsewhere, or not the reader's:

- **Internal implementation detail** — how anything was built. Not knowable, not useful, and it crowds out the sentence
  they needed (Rules).
- **Issue tracking and task lists** — what is planned or in progress. That is
  [STATUS](../03-engineering/IMPLEMENTATION_STATUS.md)'s, and it is live state for the team, not a promise to a user.
- **The complete record of changes** — [CHANGELOG](./CHANGELOG.md)'s. This document is deliberately incomplete (§6).
- **Decisions and their reasoning** — the [ADRs](../01-architecture/decisions/) and
  [PRODUCT_DECISIONS](../00-product/PRODUCT_DECISIONS.md).
- **What the work taught** — [LESSONS_LEARNED](../03-engineering/LESSONS_LEARNED.md)'s, and never a user's concern.
- **Positioning and competitive claims
  ** — [PRODUCT_VISION §12](../00-product/PRODUCT_VISION.md#12-competitive-positioning)'s.
  A release note reports; it does not persuade (§2).
- **Roadmap and intent** — what is coming. A note describes what shipped; a promise made here is a commitment nobody
  approved ([PRODUCT_DECISIONS](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)).

---

## 5. Release Note Lifecycle

A note has stages because the difference between *a draft the team is still shaping* and *a statement a professional has
read and acted on* is the difference between text that can be fixed and text that cannot.

| Stage          | What it means                                                                                                                                                                                                                                                                        |
|----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Proposed**   | A shipped change is judged to matter to the reader (§4, §6). Most do not, and deciding so is the work — a note for everything is a note nobody finishes reading, which costs the important ones their audience.                                                                      |
| **Drafted**    | Written for the reader: what is different, what it means, which release (§3). It carries nothing yet; nobody may act on it, and it is not a commitment.                                                                                                                              |
| **Reviewed**   | Checked against **what actually shipped** — the [changelog](./CHANGELOG.md) and the owning documents (§7) — and against the audience it is for. Reading well is not the check; being true is, and only one of those is obvious on the page.                                          |
| **Approved**   | Judged accurate, traceable, and fit for the reader. Approval attaches to **this note as written**; approving a note is never approving the change, which shipped under its own approval long before ([CONTRIBUTING](../03-engineering/CONTRIBUTING.md#7-review-and-approval-rules)). |
| **Published**  | The reader has it. **From this point it is not edited** — someone may already have acted on it, and a note quietly corrected leaves them holding a version of events that no longer exists and that they cannot detect (§1).                                                         |
| **Superseded** | A later note corrects it. Both stand, in order. That we said something and later corrected it is a fact about this product's honesty, and it is the fact a professional weighs most heavily ([CHANGELOG §5](./CHANGELOG.md#5-entry-lifecycle)).                                      |
| **Archived**   | Grouped away as a release recedes — old, not untrue. Archiving changes **prominence**, never content or order. An archived note that cannot be read is a deleted one with extra steps.                                                                                               |

**Every stage has an exit.** *Proposed* ends when the change is judged to matter or set aside as invisible to the
reader.
*Reviewed* ends in approval or a finding — a note that cannot be checked against what shipped does not publish, because
an unverifiable note is the one failure this document cannot survive (Rules). *Published* is terminal: it ends only into
*Superseded* or *Archived*, and **never back into an editable state**.

**Draft versus published record.** Before *Published*, a note is a **draft**: rewrite it, argue about it, discard it —
nothing depends on it. After *Published*, it is **a statement made to a professional**, and it can only be added to.
The two look identical and behave oppositely, which is why the boundary is named rather than assumed. Everything this
document protects lives on one side of it.

---

## 6. Release Note Granularity

**Release notes are a communication artifact, not a complete record.** This is the distinction that makes them useful,
and the one most easily lost: completeness is [CHANGELOG](./CHANGELOG.md)'s job and it does it better, exhaustively, for
a reader who wants it. A note that aspires to completeness has become a worse changelog and stopped being a note — while
the professional it was written for has stopped reading before the part that mattered.

| Guidance                                       | What it means                                                                                                                                                                                                                            |
|------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Note meaningful user impact**                | Something the professional would notice, act on, or want to know. That is the entire inclusion test, and it is about **them** — not about how much the change cost, mattered internally, or is worth showing.                            |
| **Avoid trivial internal-only noise**          | Invisible changes are not news. Every one included dilutes the notes that are, and teaches the reader that this document is mostly not for them — after which the one that was for them is missed.                                       |
| **Group related changes into one narrative**   | A release is a story about what the professional now has, not a list of what happened to it. Several changes serving one improvement are one thing to the reader, and dividing them makes them reassemble it.                            |
| **Avoid duplicating changelog detail**         | The note says what it means; the changelog says what changed and how to find it (§7). Reproducing that detail creates a second record that will drift — and this is the copy the reader trusts, because it is the only one they can see. |
| **Prioritize what the audience needs to know** | Order and emphasis carry meaning. What is buried is not read; what leads is what the reader believes the release was. Both are editorial decisions, and both are made deliberately or made by accident.                                  |

**Deliberate incompleteness is the point, and it is not permission to omit.** A note leaves out what the reader does not
need — never what they would want to know but we would rather not say (§2). The first is editing; the second is the
curation that, once suspected, discounts every note in the document.

---

## 7. Cross-Referencing

**A note is the reader's view; the sources are the evidence.** The reader will follow none of these — that is the point.
They exist so the **team** can verify, before publication, that what is being told to a professional is true.

| Reference                      | What it establishes                                                                                                                                                                                                                                     |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Linked changelog grouping**  | Which recorded changes this release covers ([CHANGELOG §8](./CHANGELOG.md#8-chronology-and-versioning)). This is the primary link: it is what makes a note derived rather than composed, and what stops it becoming a second, prettier record (§1).     |
| **Owning document**            | Which document granted the behavior being described ([CONTRIBUTING §4](../03-engineering/CONTRIBUTING.md#4-repository-workflow)). It is how a reviewer checks that a note describes granted behavior rather than a well-phrased claim nobody approved.  |
| **Relevant ADR**               | Where a ratified decision is behind the change ([ADRs](../01-architecture/decisions/)). For verification only — the note never explains it, and users are not its audience (§4).                                                                        |
| **Deployment record**          | That it actually went live, and when ([DEPLOYMENT §5](../03-engineering/DEPLOYMENT.md#5-promotion-lifecycle)). *Merged* and *shipped* are different events; the reader only ever experiences the second, and only that one may be dated in a note (§3). |
| **Traceability to the source** | Whatever else lets a reviewer reach what actually shipped. The requirement is the **property**, not any particular reference: a note must be checkable by someone who did not write it.                                                                 |

**Traceability matters because the reader cannot do it themselves.** Every other document here is read by people who can
check it — they have the repository, the reviews, the decisions. This reader has the note and nothing else. The
references are how the team discharges the duty the reader cannot: they are the only mechanism standing between *this is
true* and *this sounded right to whoever wrote it on release day*.

*How a reference is written is not this document's business* — only that it exists and resolves.

---

## 8. Chronology and Release Grouping

**Newest first**, matching [CHANGELOG §8](./CHANGELOG.md#8-chronology-and-versioning). Two reasons, and the second is
the
stronger one. The reader's question is almost always *what changed recently* — they are catching up, not studying the
product's history, and a record that grows forever must not require reaching its end. And a reader moving between the
two records should never have to reorient: consistency across them costs nothing and is noticed only when absent.

The unit differs even though the order does not: the changelog is chronological **by change**; this is chronological
**by release**. A release groups changes, so this record is coarser and derived — it never dates a change, only a
release (§3).

| Concern                             | The discipline                                                                                                                                                                                                                                            |
|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Date consistency**                | A date means one thing throughout: when the release reached the reader (§3). Mixing in merge or decision dates makes the order unreliable while every note stays individually true — undetectable by inspection, and wrong in exactly the reader's terms. |
| **Release boundaries**              | Where one release ends and the next begins — a fact, recorded. Never adjusted afterward so a change falls more tidily on one side of it (§2).                                                                                                             |
| **Avoiding gaps**                   | A release with no note tells the reader nothing shipped. Where that is untrue, the omission is a false statement made by silence — and it is corrected by publishing, never by backfilling as though it had been there (Rules).                           |
| **Avoiding duplicate releases**     | One release appears once. The same release described twice leaves the reader unable to tell which they have, which is the one thing this grouping exists to answer.                                                                                       |
| **A release spanning many changes** | The normal case, and this document's natural shape: a release is a **narrative** over many changes, not one per change (§6). The changelog keeps each change separately dated and traceable; this keeps what they add up to for the professional.         |

### There is no product version scheme, and this document does not invent one

The frozen documents own versioning of their **own artifacts** — the API
([API_SPEC §20](../01-architecture/API_SPEC.md#20-api-versioning-strategy)), requirements
([SRS §14](../00-product/SRS.md#14-requirement-versioning)), prompts
([PROMPTS §6](../04-ai/PROMPTS.md#6-prompt-versioning)). **No document defines a product or release version scheme**,
and
[CHANGELOG §8](./CHANGELOG.md#8-chronology-and-versioning) already establishes that introducing one is an **ADR**,
raised
per [CLAUDE.md §8](../../CLAUDE.md) — not a decision made by a release document because a reader would like a number.

Until such a scheme exists, releases are grouped by what the frozen documents already provide: the **milestones**
([IMPLEMENTATION_PLAN §4](../03-engineering/IMPLEMENTATION_PLAN.md#4-milestones)), where M6 is the MVP/beta of
[PRD §15](../00-product/PRD.md#15-release-scope-moscow). A note identifies its release only from that grouping. An
invented identifier is worse here than anywhere else in the repository: it is a number a professional would use to
decide whether something applies to them, and it would mean nothing.

---

## 9. Release Notes Checklist

Every note is assessed against this checklist before publication. A "no" is a finding to resolve, not a detail to
defer —
after publication there is no revision, only supersession (§5).

- [ ] **Real release?** — It shipped, to real readers; nothing here is anticipated, imminent, or nearly done (Rules).
- [ ] **Audience impact clear?** — The reader can tell what is different for them, not only that something changed (§3).
- [ ] **Source identified?** — The changelog grouping, and the owning document or ADR where relevant (§7).
- [ ] **Release grouping correct?** — Attributed to the release that actually carries it, from the grouping the frozen
  documents provide (§8).
- [ ] **Chronology preserved?** — Dated by when it reached the reader, in order, creating no gap or duplicate (§8).
- [ ] **No duplicate authority?** — It describes granted behavior; it defines none, and restates no decision, ADR, or
  changelog entry (Rules).
- [ ] **No invented history?** — Nothing described ahead of shipping, smoothed, or filled in; what is unknown is left
  unclaimed (§2).
- [ ] **Concise and readable?** — In the product's
  voice ([UI_GUIDELINES §3](../02-design/UI_GUIDELINES.md#3-tone-and-voice)),
  in the reader's vocabulary, and short enough to be read in the seconds it will get (§6).
- [ ] **Review complete?** — Checked against **what shipped**, not against whether it reads well (§5).
- [ ] **Consistent with changelog and sources?** — It agrees with [CHANGELOG](./CHANGELOG.md) and the owning documents
  about the same release; a disagreement is a finding, not a difference of emphasis (§7).

---

## Release Notes Review Process

> *Unnumbered governance section. It defines when a note is reviewed and how release-note governance evolves —
> deliberately, not by accretion.*

**Review triggers** — a release-note review is required when any of the following occurs:

- **A new release** — the grouping itself, and every note inside it, checked against what shipped (§8).
- **A meaningful change set** — something a professional would notice reached them (§6).
- **A release packaging change** — what a release contains, or how it is grouped or identified.
- **A user-visible product change** — anything altering what the reader experiences ([SRS](../00-product/SRS.md)).
- **An AI change** — a prompt, provider, model, evaluation, or retrieval change reaching production. Invisible to the
  reader and consequential to their work, which is the combination that most needs telling (§4).
- **A design change** — the interface a professional has built habits around has moved
  ([UI_GUIDELINES](../02-design/UI_GUIDELINES.md#ui-review-process)).
- **A deployment impact** — including a withdrawal
  ([DEPLOYMENT](../03-engineering/DEPLOYMENT.md#deployment-review-process)). What the reader had and no longer has is
  always news.
- **A documentation change affecting meaning** — what the product tells a professional now means something different
  ([CONTRIBUTING §6](../03-engineering/CONTRIBUTING.md#6-documentation-update-rules)).
- **A cross-document inconsistency** — a note and the changelog, or a note and an owning document, disagree about the
  same release. Already realized, not a risk.
- **A superseded release note** — new information contradicts a published one; corrected by publishing, never by editing
  (§5).

**Review outcomes** — each review resolves to exactly one:

- **Approved** — true, traceable, in the reader's terms, and safe to publish as written (§9).
- **Refinement required** — the release is real but the note is not publishable: an unclear impact, an unnamed source,
  a claim beyond what the documents grant, a register written for us.
- **Changelog review required** — the note has no grouping to derive from, or disagrees with one; routed to the
  [Changelog Review Process](./CHANGELOG.md#changelog-review-process). **A note is never the fix for a missing record.**
- **Documentation review required** — the note reveals that an owning document does not describe what shipped; routed
  there ([CONTRIBUTING §4](../03-engineering/CONTRIBUTING.md#4-repository-workflow)). **The note is never the fix for a
  document being wrong.**
- **Product review required** — the note would describe behavior no document grants, or cross a
  [boundary](../00-product/PRODUCT_DECISIONS.md#2-product-boundaries); routed to
  [PRD](../00-product/PRD.md)/[SRS](../00-product/SRS.md) before anything is said to a reader.
- **Architecture review required** — routed to the
  [Review Process](../03-engineering/IMPLEMENTATION_PLAN.md#review-process)'s architecture triggers.
- **AI review required** — routed to the owning AI process
  ([CONTRIBUTING §7](../03-engineering/CONTRIBUTING.md#7-review-and-approval-rules)).
- **Deployment review required** — routed to the
  [Deployment Review Process](../03-engineering/DEPLOYMENT.md#deployment-review-process).
- **ADR required** — the decision is significant, precedent-setting, or hard to reverse
  ([CLAUDE.md §8](../../CLAUDE.md)) — including any proposal to introduce a product version scheme (§8).

Only **Approved** publishes a note; **Refinement required** returns it to its author; every other outcome holds it until
the review it was routed to returns. A note held there is not published by the passage of time — and **no outcome edits
a published note**, which is not an available result at any stage (§5).

**Synchronization.** Release-note governance MUST remain synchronized with [CLAUDE.md](../../CLAUDE.md),
[CHANGELOG](./CHANGELOG.md), [CONTRIBUTING](../03-engineering/CONTRIBUTING.md),
[DEPLOYMENT](../03-engineering/DEPLOYMENT.md), [UI_GUIDELINES](../02-design/UI_GUIDELINES.md),
[PRD](../00-product/PRD.md)/[SRS](../00-product/SRS.md), and the relevant [ADRs](../01-architecture/decisions/). When
this document and one of them disagree, **they win** and this document is corrected
([CLAUDE.md §3](../../CLAUDE.md)) — *this document*, meaning its governance. **A published note is never corrected to
agree with anything**; if a note and a document disagree about what shipped, that is a finding about both, resolved by
publishing here and correcting there (§5).

---

## 10. Release Notes Decision Summary

The load-bearing decisions behind the audience-facing record, recorded so they are not silently reversed. **The
product's voice** ([UI_GUIDELINES §3](../02-design/UI_GUIDELINES.md#3-tone-and-voice)), **the audience**
([PRD §6](../00-product/PRD.md#6-target-users)), **the behavior described** ([SRS](../00-product/SRS.md)), **the
chronological record of changes** ([CHANGELOG](./CHANGELOG.md)), and **release grouping**
([IMPLEMENTATION_PLAN §4](../03-engineering/IMPLEMENTATION_PLAN.md#4-milestones)) are owned elsewhere and are not
restated here.

| Decision                               | Chosen Approach                                                                          | Alternatives                                                              | Rationale                                                                                                                                                                                                                                                                                    |
|----------------------------------------|------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Audience-facing release record**     | Notes are written for the professional, from what they now need to know                  | Publish the changelog to users; it is complete and already written        | The changelog answers *what changed* for someone looking. This reader was interrupted mid-task and asks *what is different for me* — a question completeness does not answer and length actively obstructs (§1, §6).                                                                         |
| **Separate from the changelog**        | One release, two records, two readers; neither summarizes the other                      | Derive notes mechanically from changelog entries                          | A mechanical derivation is a changelog with worse formatting: it carries what changed and not what it means, which is the only thing the note was for. They share a source and answer different questions ([CHANGELOG](./CHANGELOG.md), §1).                                                 |
| **The voice is adopted, not invented** | Notes speak in the product's existing voice                                              | Define a release-note tone suited to announcements                        | *The product speaks with one voice* ([UI_GUIDELINES](../02-design/UI_GUIDELINES.md#3-tone-and-voice)) — and a second answer to "how does LedgerAI sound?", invented where nobody thought to look, is exactly how one voice becomes two (§1).                                                 |
| **Change linked to source**            | Every note traces to the changelog grouping and the document that granted the behavior   | Trust the writer, who knows what shipped                                  | The reader cannot verify anything: they have the note and nothing else. The references are how the team discharges a duty the reader cannot — the only thing between *this is true* and *this sounded right on release day* (§7).                                                            |
| **Grouped by release**                 | Notes are grouped by release, dated by when it reached the reader                        | Group by feature area, or by change                                       | The reader's first question is *do I have this?*, and only a release grouping answers it. A change attributed to the wrong release tells them they have something they do not — worse than saying nothing (§8).                                                                              |
| **Deliberately incomplete**            | Notes cover what matters to the reader; the complete record lives elsewhere              | Cover every change so nothing is hidden                                   | Completeness here is not thoroughness; it is dilution. Every invisible change included teaches the reader this is not for them — and the one that *was* for them is then missed. Incompleteness is never a licence to omit the awkward (§6, §2).                                             |
| **No invented history**                | Nothing is described before it ships; gaps are corrected by publishing, never backfilled | Describe imminent changes; backfill missed releases for continuity        | To this reader every note is unfalsifiable — they cannot check. That is precisely why inventing one is not a small liberty: the document works only while it has never once been wrong, and the reader learns otherwise by acting on it (§2, Rules).                                         |
| **Published notes are never edited**   | Corrections are published; the original stands, superseded                               | Correct notes in place so readers always see the accurate version         | Someone may already have acted on it. A note quietly corrected leaves them holding a version of events that no longer exists and that they cannot detect — and *that we corrected it* is the fact a professional weighs most heavily ([CHANGELOG §5](./CHANGELOG.md#5-entry-lifecycle), §5). |
| **Describes, never defines**           | Notes describe behavior the frozen documents grant                                       | Let notes explain behavior fully, since they are the clearest description | A well-written note is often the clearest description of a behavior anywhere — which is what makes it dangerous. It is unreviewed as a specification, unversioned, and the first thing people find ([SRS](../00-product/SRS.md), Rules).                                                     |
| **Governance over narrative**          | A note is reviewed against what shipped, not against how it reads                        | Trust editorial judgement; these are communications, not specifications   | Reading well and being true are independent, and only one is visible on the page. This is the only document here a user reads and cannot check — which makes it the last place to substitute craft for verification (§5, §9).                                                                |

---

*This document holds LedgerAI's audience-facing record of what shipped — what a release gives the professional, grouped
so they can tell whether it applies, and traceable so the team can verify it before saying it; it does not override the
frozen documents under [`docs/`](../), and it grants, decides, and defines nothing. It describes what shipped without
recording what changed ([CHANGELOG](./CHANGELOG.md) does), without deciding what the product should do
([PRD](../00-product/PRD.md) and [SRS](../00-product/SRS.md) do), without explaining how it was built (the
[ADRs](../01-architecture/decisions/) do), without teaching from
it ([LESSONS_LEARNED](../03-engineering/LESSONS_LEARNED.md)
does), and without inventing a voice ([UI_GUIDELINES](../02-design/UI_GUIDELINES.md#3-tone-and-voice) owns it). The
record is currently empty because nothing has shipped, and it will be filled by things shipping — never by anyone
deciding what should have. When a note would describe behavior no document grants, require a version scheme, or correct
a published statement in place, stop and raise it per [CLAUDE.md §8](../../CLAUDE.md) — this is the only document here
that a user reads, and they cannot check a word of it.*
