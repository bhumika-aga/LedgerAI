# Design System — LedgerAI MVP

> **Status:** Draft v1
> **Owner:** Principal Design Systems Architect
> **Last updated:** 2026-07-15
> **Upstream (frozen):
** [PRD](../00-product/PRD.md) · [SRS](../00-product/SRS.md) · [ARCHITECTURE §6](../01-architecture/ARCHITECTURE.md#6-frontend-architecture) · [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) · [SECURITY](../01-architecture/SECURITY.md)
> **Related:
** [COMPONENTS](./COMPONENTS.md) · [USER_FLOWS](./USER_FLOWS.md) · [UI_GUIDELINES](./UI_GUIDELINES.md) · [FRONTEND_CODING_STANDARDS](../03-engineering/FRONTEND_CODING_STANDARDS.md) · [CLAUDE.md](../../CLAUDE.md)

---

## 1. Purpose

### Why this document exists

This document defines the **visual language of LedgerAI** — the shared, named vocabulary of color, type, space, shape,
depth, and motion from which every screen draws its appearance. Its goal is that an accountant moving between the
Clients list, a document view, and an AI summary never has the sense of having crossed into a different product. The
outcome it protects is **visual coherence**: one product, one language, one set of meanings.

It is **not** a UI mockup, **not** a developer style guide, and **not** a CSS specification. It contains **no color
values, no font names, no measurements, no token syntax, no code, no Figma references, and no Material UI APIs**. It
describes *what visual concepts exist, what they mean, and when each applies* — never how they are declared or rendered.

The governing principle of this document:

> **Appearance is inherited, not invented.**
>
> A screen that reaches for its own color, its own spacing, or its own emphasis — even one that looks correct in
> isolation — is a defect against this document, because the cost of a local visual decision is paid globally, by every
> user who has to relearn what a thing means.

Every rule, token, and review outcome below exists to make that one sentence true in practice.

### Relationship to the sibling design and engineering documents

The design layer is four documents with four distinct jobs. They are complementary and MUST never contradict:

| Document                                                                       | Its job                        | Relationship to this document                                                                                                                                                                                                                                                                                                                                                                                   |
|--------------------------------------------------------------------------------|--------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [COMPONENTS.md](./COMPONENTS.md)                                               | **Reusable building blocks**   | Defines the **structural vocabulary** — which components exist, what each is for, what it must and must not do. This document defines the **visual language those components are rendered in**. A Button's existence, states, and contract are COMPONENTS'; its color, type, spacing, and emphasis are this document's. Components inherit from the Design System; the Design System never defines a component. |
| [USER_FLOWS.md](./USER_FLOWS.md)                                               | **Behavior**                   | Defines the **sequences** a user moves through and what each step means. This document supplies the visual meaning those steps are expressed with — what "in progress", "failed", or "AI-generated" *look* like. Flows define the path; this document ensures each step along it reads consistently.                                                                                                            |
| [UI_GUIDELINES.md](./UI_GUIDELINES.md)                                         | **Application across screens** | Defines **how this language is applied** — tone, microcopy, and interaction norms. This document defines the vocabulary; UI_GUIDELINES governs its usage in context. Where this document says *what a semantic color means*, UI_GUIDELINES says *how to phrase the message beside it*.                                                                                                                          |
| [FRONTEND_CODING_STANDARDS.md](../03-engineering/FRONTEND_CODING_STANDARDS.md) | **Realization in code**        | Defines **how the visual language is implemented** — that tokens are the source of visual truth and never hard-coded per component ([§12](../03-engineering/FRONTEND_CODING_STANDARDS.md#12-styling-standards)). This document is the conceptual authority for *what* the tokens mean; that document is the enforcement authority for *how* they are used. The two are deliberately paired.                     |

In one line each:

> **COMPONENTS define building blocks. USER_FLOWS define behavior. DESIGN_SYSTEM defines the visual language.
> UI_GUIDELINES explain how that language is applied across screens.**

### Relationship to the frozen documents

This document introduces **no product behavior**. It serves the capabilities granted by [PRD](../00-product/PRD.md) and
[SRS](../00-product/SRS.md), expresses the AI transparency obligations of
[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md), and respects the disclosure limits of
[SECURITY](../01-architecture/SECURITY.md). Where a visual decision would imply new behavior, it stops and is raised per
[CLAUDE.md §8](../../CLAUDE.md).

---

## 2. Design Philosophy

These principles explain *why* LedgerAI's visual language is shaped the way it is. They are the reasoning behind the
enforceable rules that follow.

| Principle                    | Why it exists                                                                                                                                                                                                                                                                             |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Clarity over decoration**  | An accountant opens LedgerAI to finish work, not to admire it. Every visual element either helps the user understand something or costs them attention. Decoration that carries no meaning is a tax on comprehension, so the language spends emphasis only where meaning exists.          |
| **Consistency over novelty** | A predictable product is a trustworthy one. When the same meaning always looks the same way, recognition replaces reading and the user works faster. Novelty in one screen is bought with relearning in every other — a bad trade at any scale.                                           |
| **Professional appearance**  | The audience is Chartered Accountants, CPAs, and auditors handling confidential client financials. The interface sits beside Tally, QuickBooks, and Excel in their working day and must look like a credible professional instrument — restraint reads as competence.                     |
| **Accessibility by default** | Accessibility is a property of the language, not a per-screen task. If contrast, focus, and color-independence are decided once in the vocabulary, every screen inherits them and none can forget them. Retrofitting is costly, inconsistent, and always incomplete.                      |
| **Trust through simplicity** | The product handles confidential financial documents and presents machine-generated output about them. A simple, quiet interface makes what matters legible; visual complexity reads as noise at best and evasion at worst, and trust is this product's premise.                          |
| **Progressive disclosure**   | Showing everything at once overwhelms. The visual language provides the means to defer detail — hierarchy, grouping, depth — so each step stays comprehensible and complexity arrives only when the user needs it ([USER_FLOWS §2](./USER_FLOWS.md#2-user-flow-philosophy)).              |
| **Minimal cognitive load**   | Attention spent decoding an interface is attention not spent on the document. A small vocabulary, consistently applied, means the user learns the language once. Every additional variant is another thing to hold in mind while doing skilled work.                                      |
| **AI transparency**          | LedgerAI advises; the professional decides. The visual language MUST make AI-generated content unmistakable, its grounding visible, and its provisional nature obvious — the human-in-the-loop guarantee of [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md), expressed visually. |
| **Responsive by design**     | The interface adapts to the viewport it is given rather than assuming one. Adaptation is a property of how the language composes — relative measure, flexible arrangement, deliberate priority — not a set of exceptions bolted on afterwards.                                            |

---

## Design System Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Every interface MUST use design tokens.** *Tokens are the single source of visual truth; an interface that draws
  appearance from anywhere else has stepped outside the language and will drift from it independently
  ([FRONTEND_CODING_STANDARDS §12](../03-engineering/FRONTEND_CODING_STANDARDS.md#12-styling-standards)).*
- **Hard-coded visual styles MUST NOT become part of the design language.** *A one-off value is invisible to review,
  unowned, and unfindable when the language changes; it is how a design system silently forks into many.*
- **Components MUST inherit from the Design System.** *A component that carries its own appearance breaks the guarantee
  that a change to the language reaches every screen; inheritance is what makes the system a system
  ([COMPONENTS](./COMPONENTS.md)).*
- **Visual consistency MUST take precedence over individual creativity.** *No screen gets to look "a little different"
  for local convenience. Each deviation is individually defensible and collectively fatal — the cumulative effect is an
  incoherent product.*
- **Accessibility MUST override aesthetics.** *When a visual preference and an accessibility obligation conflict,
  accessibility wins without debate. An interface that cannot be perceived or operated is not beautiful; it is broken
  ([FRONTEND_CODING_STANDARDS §11](../03-engineering/FRONTEND_CODING_STANDARDS.md#11-accessibility-standards)).*
- **New visual patterns MUST undergo review.** *A pattern admitted without review becomes precedent by accident; the
  review is where the system decides what it is, rather than discovering later what it became (Design Review Process).*
- **Semantic meaning MUST remain consistent.** *If a semantic color, weight, or emphasis means one thing on one screen
  and something else on another, the user cannot trust any of them and must read everything. Meaning is the asset.*
- **Semantic token aliases MUST NOT duplicate existing meaning.** *Two names for one concept is two answers to the same
  question. Once an alias exists, screens divide between the names arbitrarily, reviewers cannot tell which is correct,
  and a change to the meaning has to be made — and remembered — in more than one place. The vocabulary drifts apart
  while every individual choice looks defensible. If an existing token already carries the meaning, that token is the
  answer; if it genuinely does not, the need is a new meaning and is reviewed as one (Design Review Process).*
- **Motion MUST support understanding rather than decoration.** *Motion earns its place only by explaining a
  relationship — where something came from, what changed, what is still working. Motion for delight costs time on every
  repetition and is the first thing to irritate a daily professional user.*
- **Motion MUST never delay task completion solely for visual effect.** *A professional repeating a task hundreds of
  times a day pays the cost of every deliberate pause, and pays it forever. Motion may accompany work; it MUST NOT
  become a gate the user waits behind. Where motion and speed conflict, speed wins — this is *clarity over decoration*
  applied to time, and it is what keeps the interface a professional instrument rather than a performance.*
- **New tokens MUST be additive rather than replacing existing ones.** *Redefining an existing token silently changes
  every screen that already relies on it. Growth is safe; mutation is not — the same additive discipline
  [DATABASE](../01-architecture/DATABASE.md) applies to schema.*
- **Design decisions MUST remain traceable.** *A visual choice whose reasoning is unrecorded cannot be reviewed,
  defended, or safely reversed; it degrades into "that's how it has always looked" (§16).*

**Why these rules exist.** A design system does not fail loudly. It fails through **visual drift** — one urgent
exception, one hard-coded value, one screen that needed to look slightly different, none of them wrong enough to stop.
Each is cheap in isolation; together, over a year, they produce a product that feels assembled rather than designed, in
which the user can no longer trust that the same appearance means the same thing. These rules exist because that decay
is gradual and invisible from inside any single change, so the discipline has to live in the review rather than in the
intention. They keep LedgerAI's visual language **one language** as the product grows.

---

## 3. Design Tokens

A **token** is a *named visual decision*. It gives a design choice a name and a meaning so that the choice is made once,
centrally, and referred to everywhere else by what it means rather than by what it is. Tokens are the mechanism by which
every rule in this document becomes enforceable: they make appearance reviewable, changeable in one place, and
impossible to fork by accident.

Tokens are **semantic, not literal**. A token is named for its *role* ("the color that means destructive") rather than
its *appearance* ("the red one"), because the role is stable while the appearance may change. That distinction is the
difference between a design system and a list of values.

> **Scope boundary:** this section defines what each token category is **for**. It deliberately defines **no values and
> no syntax** — no color values, font names, measurements, scales, or declaration format. Values and their expression in
> code are downstream of this document and governed by
> [FRONTEND_CODING_STANDARDS §12](../03-engineering/FRONTEND_CODING_STANDARDS.md#12-styling-standards).

| Token category         | What it names                            | Why it exists                                                                                                                                                                                                       |
|------------------------|------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Color**              | The semantic roles color plays (§4)      | Color carries meaning in this product — status, severity, provenance. Naming roles rather than hues keeps that meaning consistent and lets the palette evolve without re-teaching the user what a color means.      |
| **Typography**         | The steps of the type hierarchy (§5)     | Hierarchy is how a reader knows what matters before reading. A named scale makes that hierarchy a shared decision instead of a per-screen judgement call.                                                           |
| **Spacing**            | The increments of the spacing scale (§6) | Space is what groups and separates. A finite named scale produces visual rhythm and makes relatedness legible; arbitrary spacing makes grouping accidental.                                                         |
| **Border radius**      | The degrees of corner treatment          | Radius signals the character of a surface and distinguishes classes of element consistently. A small named set prevents shape from becoming a per-component opinion.                                                |
| **Elevation / shadow** | The levels of apparent depth             | Depth expresses layering and transience — what floats above, what is anchored. Named levels keep depth meaningful; ad hoc shadows make the interface's stacking arbitrary and unreadable.                           |
| **Motion**             | The roles and characters of transition   | Motion communicates change and relationship. Named motion keeps it purposeful and uniform, and makes it possible to honor reduced-motion preferences systematically rather than per-animation (§12).                |
| **Layer (z-index)**    | The ordered stacking contexts            | Overlays, dialogs, and transient feedback MUST stack predictably. A named layer order is what prevents the escalation of arbitrary values whose only rationale is "higher than the thing it kept appearing behind." |

**One principle spans every category:** a token's meaning is fixed even when its value is not. Screens depend on the
meaning; the system reserves the right to change the value. That contract is what allows the language to evolve without
a rewrite.

---

## 4. Color System

Color in LedgerAI is **semantic**. A color is defined by the *meaning it carries*, never by its hue. This section names
the meanings; it specifies **no values and no palette names** by design.

> **Color is never the only signal.** Every meaning below MUST also be carried by text, icon, or position. This is an
> accessibility obligation, not a preference: color-blind users, low-contrast environments, and print all lose color
> ([FRONTEND_CODING_STANDARDS §11](../03-engineering/FRONTEND_CODING_STANDARDS.md#11-accessibility-standards),
> [COMPONENTS §7](./COMPONENTS.md#7-component-states)).

| Semantic role   | What it means                                   | Where it is intended to be used                                                                                                                                                                                                                                                |
|-----------------|-------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Primary**     | The product's main action and identity          | The single most important action on a screen (submit, upload, generate) and primary emphasis in navigation and identity. Used sparingly — its authority comes from scarcity, matching one-primary-action-per-screen ([USER_FLOWS §2](./USER_FLOWS.md#2-user-flow-philosophy)). |
| **Secondary**   | A supporting, non-competing action              | Alternative or lesser actions presented beside a primary one (cancel, back, a secondary path). It MUST NOT compete with Primary for attention; competing emphasis creates hesitation at the moment the user needed certainty.                                                  |
| **Success**     | An operation completed as intended              | Confirmation of genuine completion — a client saved, a document reached Ready, a report exported. Reserved for completion; never for merely present or neutral information.                                                                                                    |
| **Warning**     | Attention needed; not yet a failure             | Conditions the user should notice before proceeding: a consequence worth stating, a degraded but usable result, a low-confidence extraction ([FR-OCR-006](../00-product/SRS.md#46-ocr-ocr)). It signals *caution*, not *error*.                                                |
| **Error**       | An operation failed, or input is invalid        | Validation failures ([SRS §6](../00-product/SRS.md#6-validation-rules)), failed uploads, failed extraction, failed AI generation. Always paired with an actionable message and a way forward; never a dead end ([USER_FLOWS §15](./USER_FLOWS.md#15-cross-flow-principles)).   |
| **Information** | Neutral context worth surfacing                 | Explanatory notices, in-progress conditions, non-urgent system context. Carries no judgement of good or bad — it exists so that neutral information is not forced to borrow Success or Warning and distort their meaning.                                                      |
| **Neutral**     | The substrate: content, surfaces, borders, text | The default of the interface — page and card surfaces, body text, separators, disabled treatments. Most of LedgerAI is neutral by design; that is precisely what allows a semantic color to mean something when it appears.                                                    |

**Why restraint is the rule.** Semantic color only works if it is rare. An interface where much is colored has taught
the user that color predicts nothing — and the one screen where color genuinely matters, a failed extraction or an
invalid figure, no longer stands out. Neutral is the default; semantic color is an exception that earns its place.

---

## 5. Typography System

Typography is LedgerAI's primary instrument of hierarchy. Before a user reads anything, type tells them what this screen
is, what matters on it, and where to start. The hierarchy below is a **finite, ordered set of roles**; it specifies **no
fonts, sizes, weights, or measurements**.

| Role           | What it is for                                                                                                                                                                                                    |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Display**    | The largest, rarest step — reserved for moments that orient rather than inform (an empty state's headline, a focal first-run moment). Used almost never; frequency would destroy its effect.                      |
| **Heading**    | The identity of a page or major region: what this screen is. Establishes the top of the hierarchy the reader scans first.                                                                                         |
| **Subheading** | The identity of a section within a page — a titled grouping of related content. Expresses structure beneath the Heading without competing with it.                                                                |
| **Body**       | The default for readable prose and content: document text, summaries, AI answers, descriptions. The workhorse of the product and the step optimized for sustained reading.                                        |
| **Label**      | The name of a control, field, column, or value — short, functional text that identifies rather than explains. Every input carries one ([COMPONENTS §4](./COMPONENTS.md#4-component-specifications)).              |
| **Caption**    | Secondary, subordinate detail: timestamps, helper text, metadata, attribution. Deliberately quiet — present for the reader who seeks it, never competing with content.                                            |
| **Monospace**  | Text whose *character alignment carries meaning*: extracted document text where layout is significant, identifiers, figures compared column-wise. Chosen for fidelity to the source, never for decorative effect. |

**Rules of the hierarchy:**

- Type roles MUST be chosen for **meaning, not appearance.** Selecting a role because it "looks the right size" detaches
  the hierarchy from the structure it exists to express, and the next reader inherits a lie about what matters.
- The hierarchy MUST remain **shallow and finite.** Every additional step is another distinction the reader must
  resolve; a hierarchy nobody can perceive is decoration with extra steps.
- Hierarchy MUST NOT be the **only** structural signal. Visual prominence means nothing to a screen reader; semantic
  structure carries it (§12).

---

## 6. Spacing System

Space is the quietest and most load-bearing part of the language. It is how the interface says *these things belong
together and that thing does not* — without a single word, border, or color. This section defines the concepts; it uses
**no measurements** and names no values.

**A single spacing scale governs all of it.** Every gap in LedgerAI MUST come from one finite, shared scale of
increments. That is the whole system: a small, ordered set of steps, applied consistently.

| Concept                | What it governs                                                                                                                                                                 |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Vertical rhythm**    | The consistent cadence of stacked content down a page — the repeating relationship between text, headings, and groups that lets the eye move without re-anchoring at each step. |
| **Horizontal spacing** | Separation across a row: between a label and its value, an icon and its text, adjacent actions. It establishes reading pairs and keeps related things visibly related.          |
| **Component spacing**  | The space *inside* a component and between its parts — the internal density that makes a Card or a Table row feel like one object rather than several.                          |
| **Page spacing**       | The frame: margins and gutters around and between regions, giving content a consistent home and the page a stable shape across every screen.                                    |

**Why one consistently-applied scale improves usability:**

- **Proximity is meaning.** Human perception groups by nearness before it reads. Consistent spacing makes grouping
  *accurate*; arbitrary spacing makes it *accidental*, and an accidental group is a misleading one.
- **Rhythm reduces effort.** A predictable cadence lets the eye move without re-measuring at every step. Irregular
  spacing forces constant micro-decisions about what belongs to what — a small tax paid on every screen, all day.
- **A finite scale removes a decision.** With a shared scale there is no per-screen judgment to make and no near-miss
  values that read as mistakes. Consistency becomes the path of least resistance rather than an act of discipline —
  which is the only way it survives contact with a deadline.
- **Space replaces ornament.** Well-spaced content needs fewer borders, boxes, and rules to be legible. Space is how the
  interface stays quiet while staying clear — *clarity over decoration*, made structural.

---

## 7. Iconography

Icons are **language, not decoration**. Every icon is a claim that a symbol is faster to recognize than a word — a claim
that is often false and always worth testing. This section defines principles only; it **chooses no icon library**.

- **Semantic meaning.** An icon MUST mean exactly one thing across the entire product. The same symbol carrying two
  meanings on two screens is worse than two words, because the user cannot know they have been misled.
- **Consistency.** Icons MUST read as one family — consistent, construction, and level of abstraction. A mixed
  set looks assembled rather than designed and undermines the professional appearance the product depends on.
- **Decorative vs functional.** The distinction MUST be explicit. A **functional** icon identifies an action or state,
  carries an accessible name, and is operable where relevant. A **decorative** icon adds no information and MUST be
  hidden from assistive technology — announcing it only adds noise for the user who can least afford it.
- **Never the sole carrier of meaning.** An icon MUST NOT be the only signal of an action or state. Recognition is not
  universal, symbols are culturally contingent, and assistive technology announces the accessible name — not the shape
  ([COMPONENTS §4](./COMPONENTS.md#4-component-specifications)).
- **Accessibility.** Functional icons MUST carry an accessible name stating the *action or meaning*, not the picture.
  Icon-only actions MUST remain fully operable and comfortably targetable, including on touch.
- **AI-specific indicators.** Any icon marking AI provenance MUST be used **only** for that purpose, consistently
  everywhere AI output appears, and MUST NOT be the only marker of it — AI provenance is always carried in text as well
  (§10). An indicator that appears in one place and not another teaches the user that its absence means nothing.

---

## 8. Layout Principles

Layout is how the visual language is arranged into a screen. These are principles of organization and priority, not
geometry: this section contains **no wireframes and no measurements**. The structural components layout is expressed
with — App Shell, Page Container, Section, Grid, Stack — are defined in [COMPONENTS](./COMPONENTS.md); this section
governs how they are *composed*.

**Page hierarchy.** Every screen MUST answer, in order and without effort: *where am I, what is here, what can I do?*
That order is expressed spatially — position and prominence establish it before any reading occurs. A screen whose most
prominent element is not its most important one is misleading regardless of how it looks.

**Content organization.** Related content is grouped and separated by space and hierarchy rather than by boxing
everything. Grouping SHOULD reflect the user's model of the work — a document beside its extracted content and its AI
output — rather than the system's internal structure. The screen is organized around the task, not the schema.

**One primary action per screen.** Layout MUST support the single-primary-action principle
([USER_FLOWS §2](./USER_FLOWS.md#2-user-flow-philosophy)): the intended next step is visually unambiguous. Competing
prominent actions create hesitation and error at exactly the moment the user needed to be certain.

| Surface      | How it is composed                                                                                                                                                                                                                                                                                                                                                                 |
|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Cards**    | A self-contained summary of **one** entity, scannable in a set. A Card carries only what supports recognition and the decision to open it — a Card that has become a dense report has stopped being a Card and needs a page.                                                                                                                                                       |
| **Tables**   | Structured, comparable rows where **alignment carries meaning**. Column order reflects priority; the most important column is not buried. Tables are for comparison — where the user reads one item rather than compares many, a List or Card is the honest choice.                                                                                                                |
| **Forms**    | A single, uninterrupted column with one clear path through, grouped into meaningful sections. Labels are always present and always visible; validation appears **beside the field it concerns**, never only at a distance ([SRS §6](../00-product/SRS.md#6-validation-rules)).                                                                                                     |
| **Dialogs**  | A focused interruption for **one** decision or task. A dialog states its purpose, its consequence, and its exits; confirming and cancelling actions are clearly distinguished, and the destructive one is never the effortless default ([COMPONENTS §4](./COMPONENTS.md#4-component-specifications)). A dialog is borrowed attention — long or multi-purpose content needs a page. |
| **Sidebars** | Persistent orientation: where the user is in the product and where else they can go. Stable across screens — a navigation region that rearranges destroys the mental map that makes it useful ([USER_FLOWS §15](./USER_FLOWS.md#15-cross-flow-principles)).                                                                                                                        |

**Responsive layouts.** Arrangement MUST adapt to the viewport rather than assume one, using flexible composition and
relative measure rather than fixed assumptions
([FRONTEND_CODING_STANDARDS §12](../03-engineering/FRONTEND_CODING_STANDARDS.md#12-styling-standards)). Adaptation is a
property of how a layout is built, not a set of exceptions added later (§11).

---

## 9. Feedback Patterns

Feedback is the product's answer to the user's constant, mostly unspoken question: *is it working, did it work, and what
do I do now?* The visual language standardizes these answers so the user learns each one **once**. The components that
carry them — Alert, Toast, Snackbar, Banner, Progress, Spinner, Skeleton — are defined in
[COMPONENTS](./COMPONENTS.md), and the standardized states they express are in
[COMPONENTS §7](./COMPONENTS.md#7-component-states). This section governs their **visual meaning and consistency**.

| Pattern                  | Its standardized presentation                                                                                                                                                                                                                                                                                                                                                                                                          |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Loading**              | Waiting is **always visible**; no operation goes silent. Presentation matches what is known: a shape-mirroring placeholder where content is arriving, an indeterminate indicator where duration is unknown, determinate progress only where progress is genuinely known. Never fake progress — a dishonest indicator is worse than an honest wait.                                                                                     |
| **Success**              | Brief, proportionate confirmation that an action completed — enough to close the loop, not enough to interrupt. Reserved for genuine completion; success that celebrates the routine becomes noise the user learns to dismiss unread.                                                                                                                                                                                                  |
| **Error**                | Failure is surfaced honestly, in **Error** semantics, at the scope of what failed — beside the field for validation, in context for an operation, at page level for a condition affecting everything. Always carries a way forward: retry, edit, or a clear exit ([USER_FLOWS §15](./USER_FLOWS.md#15-cross-flow-principles)). Error text MUST never expose internal or sensitive detail ([SECURITY](../01-architecture/SECURITY.md)). |
| **Warning**              | Caution before a consequence, or an honest note about a degraded-but-usable result. Distinct from Error: the user can proceed, and the language MUST make that difference immediately legible.                                                                                                                                                                                                                                         |
| **Information**          | Neutral context, presented quietly. It does not borrow the urgency of Warning or the finality of Error — a system that cries wolf in neutral situations has spent the credibility it needs for real ones.                                                                                                                                                                                                                              |
| **Empty states**         | A content area with no data MUST show an **explicit, informative** empty state — never a blank region, which is indistinguishable from a broken one. An empty state says what would be here, why it is not, and what to do about it, which makes it the natural home of a first action.                                                                                                                                                |
| **Disabled states**      | Communicates *present but not currently operable*, and the reason MUST be discoverable from context. A control disabled for reasons the user cannot deduce is a dead end that reads as a bug. Disabled treatment is visually quiet and MUST still be legible — invisible is not the same as unavailable.                                                                                                                               |
| **AI-generated content** | Always visibly marked as AI-generated, always distinguished from user content and source content, always presented as provisional (§10). This is the one feedback pattern carrying a **trust** obligation rather than merely an informational one.                                                                                                                                                                                     |

**The rule beneath all of them:** a pattern MUST mean the same thing everywhere. Feedback is the vocabulary the user
relies on most and reads most quickly — usually peripherally, mid-task. A Warning that means "failed" on one screen and
"proceed with care" on another destroys the value of every Warning in the product.

---

## 10. AI Presentation

This section is **LedgerAI-specific** and carries the product's central promise. LedgerAI advises; the professional
decides and remains professionally accountable for what they sign. The visual language is one of the mechanisms that
keeps that true — it MUST make AI output **identifiable, traceable, provisional, and editable** at a glance.

These are the visual expression of obligations already frozen in
[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md). This document adds **no AI behavior** and defines **no prompts
and no models**. The AI components themselves — AI Summary Card, AI Chat Panel, AI Response, Citation Block, Confidence
Indicator, Regenerate Button — are defined in [COMPONENTS](./COMPONENTS.md).

| Principle                     | What the visual language MUST do                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **AI-generated labels**       | AI content MUST be **visibly and consistently marked as AI-generated**, in text and not by visual treatment alone. The marking is uniform everywhere AI appears — summary, chat answer, email draft, report. Inconsistent marking teaches the user that unmarked content might also be generated, which destroys the signal entirely.                                                                                                                                                                                                                                                                                                       |
| **Editable output**           | AI output MUST **look like a draft the human owns**, not a finished verdict. Its editability is apparent without discovery, because output that appears authoritative invites acceptance without review — the precise failure the product exists to prevent ([BR-031](../00-product/SRS.md#5-business-rules)).                                                                                                                                                                                                                                                                                                                              |
| **Human review emphasis**     | The interface MUST keep the human's decision the visually consequential act. AI output is presented as **input to a decision**, never as the decision. No consequential action is ever taken from AI output without an explicit human step — an AI email is drafted and **never sent** ([BR-034](../00-product/SRS.md#5-business-rules)).                                                                                                                                                                                                                                                                                                   |
| **Source references**         | Where output is grounded in a document, its **grounding MUST be visible and traceable** back to the source, presented as text rather than implied by styling ([AI_ARCHITECTURE §9](../01-architecture/AI_ARCHITECTURE.md#9-grounding-strategy)). Traceability is what makes a claim checkable — and a professional's work is checkable by definition.                                                                                                                                                                                                                                                                                       |
| **Confidence representation** | Confidence is **qualitative and honest**. The language MUST NOT imply confidence exceeding the available evidence ([AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md)), MUST NOT invent numeric scores, precision, or certainty the system does not have, and MUST present an honest "not found in this document" as a legitimate, non-failure outcome. Where a low-confidence extraction is indicated ([FR-OCR-006](../00-product/SRS.md#46-ocr-ocr)), it is shown so the user can judge reliability. A fabricated number is a lie with a decimal point.                                                                             |
| **Provenance after editing**  | Where it aids the user's understanding, the interface **SHOULD preserve the distinction between the original AI-generated content and the user's subsequent edits.** Once a professional has revised a draft, *"what did the machine say, and what did I decide?"* is a question about their own accountability — and an interface that silently merges the two has erased the answer. This principle concerns **presentation only**: it prescribes no version history, no controls, and no storage behavior, which are owned elsewhere ([SRS §7.3](../00-product/SRS.md#73-report-lifecycle), [DATABASE](../01-architecture/DATABASE.md)). |
| **Regeneration**              | A fresh attempt MUST be visibly available wherever AI output appears, reinforcing that output is provisional. Regeneration MUST NOT silently discard the user's edits or completed work ([USER_FLOWS §7](./USER_FLOWS.md#7-ai-summary-flow-uf-07)).                                                                                                                                                                                                                                                                                                                                                                                         |
| **Failure communication**     | AI failure is surfaced **honestly**, in Error semantics, with a way forward and without exposing internal or provider detail ([AI_ARCHITECTURE §12](../01-architecture/AI_ARCHITECTURE.md#12-ai-failure-handling), [SECURITY](../01-architecture/SECURITY.md)). A silent or disguised AI failure is the most corrosive failure in the product, because it looks like an answer.                                                                                                                                                                                                                                                             |

**Why this is a visual obligation and not only a behavioral one.** Every safeguard in
[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) — grounding, citation, human-in-the-loop, honest failure — is
reversed at the last inch if the interface *renders* generated text exactly like verified text. Trust is established or
destroyed at the point of presentation, and that last inch is this document's responsibility.

---

## 11. Responsive Design

The interface adapts to the viewport it is given. Adaptation is about **priority**, not proportion: as space contracts,
what matters most MUST remain what is most reachable. A layout that reflows everything to fit while burying the primary
action has technically adapted and practically failed.

> **Scope boundary — this document defines no breakpoints.** Which viewport widths matter, and how they are declared,
> are implementation decisions governed by
> [FRONTEND_CODING_STANDARDS §12](../03-engineering/FRONTEND_CODING_STANDARDS.md#12-styling-standards). This section
> defines only the **usability intent** at each scale.

**The frozen scope.** [NFR-017](../00-product/SRS.md#9-non-functional-requirements) requires the web app to function on
current evergreen browsers and states it **SHOULD be responsive on desktop and tablet**. Those are the supported
responsive targets; this document does not extend that scope.

| Target      | Usability intent                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Desktop** | The **primary working environment**, where the real work happens: reading a document beside its extracted content and its AI output, working through tables, sustained review sessions. The language optimizes here for information density *with* clarity — the professional's screen is wide and their session is long.                                                                                                                                                                                                                                                  |
| **Tablet**  | A **fully supported** target ([NFR-017](../00-product/SRS.md#9-non-functional-requirements)). Content reflows and side-by-side arrangements stack rather than compress; primary actions stay reachable; touch targets stay comfortably operable. Reviewing and acting on work MUST remain genuinely possible, not merely technically rendered.                                                                                                                                                                                                                             |
| **Mobile**  | **Not an MVP responsive target.** NFR-017 scopes responsiveness to desktop and tablet, and no frozen document specifies mobile behavior or layout. The language MUST therefore not actively break at small viewports — relative measure and flexible composition mean the app degrades gracefully rather than shattering — but **no mobile-specific layout, navigation pattern, or experience is specified or promised here.** Making mobile a supported target is a product decision requiring a PRD/SRS change, not a design decision ([CLAUDE.md §8](../../CLAUDE.md)). |

**The principle underneath.** Responsiveness is a property of how the language composes — relative measure, flexible
arrangement, deliberate priority — not a set of per-screen exceptions. A layout built on fixed assumptions cannot be
made responsive afterward; it can only be rebuilt.

### Theming scope

Adapting to a viewport and adapting to a *theme* are different problems, but they fail the same way — by being
retrofitted
onto a language that assumed a single case. The position is therefore stated here explicitly:

- **Dark mode is intentionally outside MVP scope.** No frozen document specifies it, and this document does not
  introduce it. Its absence is a **deliberate scope boundary**, not an oversight and not a gap to be filled locally.
- **The semantic token model is designed so that additional themes can be introduced later without changing semantic
  meaning.** Because a color token names a *role* rather than a hue (§3, §4), a future theme resolves the same roles to
  different values. What "Error" **means** does not change; only what it resolves to would. This is the practical payoff
  of semantic naming, and it is why the boundary above costs nothing to hold.
- **Adding a theme is a future product decision, not a design decision.** It requires product approval through the
  normal change process ([CLAUDE.md §8](../../CLAUDE.md)) before any implementation. Until then, no screen, component,
  or token introduces theme-conditional appearance (§15).

---

## 12. Accessibility

Accessibility is a **property of the language**, decided once in the vocabulary and inherited by every screen composed
of it. LedgerAI's users work long hours in these screens; some rely on keyboards or assistive technology; all
eventually work tired, in poor light, on an imperfect display. Accessibility is what keeps the product usable in the
conditions it is actually used in.

This section states the **visual-language intent** only. [COMPONENTS §8](./COMPONENTS.md#8-accessibility) builds
accessibility into each component,
[FRONTEND_CODING_STANDARDS §11](../03-engineering/FRONTEND_CODING_STANDARDS.md#11-accessibility-standards) enforces it
in code, and [TESTING_STRATEGY §8](../03-engineering/TESTING_STRATEGY.md#8-ui-testing-strategy) verifies it. The product
target is **WCAG 2.1 AA for core flows** ([NFR-011](../00-product/SRS.md#9-non-functional-requirements)), which this
document neither restates nor may raise or lower; it states no conformance measurements of its own.

| Aspect                  | What the visual language MUST provide                                                                                                                                                                                                                                                                          |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Keyboard navigation** | Everything actionable is reachable and operable without a pointer, in an order that matches the visual hierarchy. Where reading order and focus order disagree, the layout is wrong — not the user.                                                                                                            |
| **Focus management**    | Focus is **always visibly indicated** and MUST never be suppressed for aesthetic reasons. A focus indicator subtle enough to be tasteful and invisible enough to be useless is a failure; this is precisely where *accessibility overrides aesthetics*.                                                        |
| **Screen readers**      | Meaning MUST be carried in text and semantics, not in visual treatment alone. Prominence, color, and position convey nothing to a screen reader; anything the sighted user learns from appearance must be available another way.                                                                               |
| **Contrast**            | Text and meaningful non-text elements MUST remain legible against their surfaces, including in the disabled and quiet treatments where contrast is most often quietly sacrificed. Contrast is a **constraint on the palette**, not a filter applied after it — a color that cannot meet it is not a candidate. |
| **Semantic structure**  | Visual hierarchy MUST be backed by real structure — headings that are headings, lists that are lists, landmarks that name regions. Type roles (§5) express hierarchy visually; they do not create it.                                                                                                          |
| **Reduced motion**      | Motion MUST honor a user's reduced-motion preference. Because motion is named and centralized (§3), the preference is respected systematically rather than per-animation — and since motion only ever supports understanding, removing it costs nothing essential.                                             |
| **Error communication** | Errors MUST be perceivable without color: text and position carry them, color only reinforces. An error identifiable only by a colored border does not exist for a substantial number of users ([SRS §6](../00-product/SRS.md#6-validation-rules)).                                                            |

---

## 13. Design Consistency Checklist

Every screen, component, or visual change — new or modified — is evaluated against this checklist before acceptance. A
"no" is a finding to resolve, not a detail to defer.

- [ ] **Uses approved tokens?** — Appearance is drawn from the token vocabulary (§3); nothing is hard-coded.
- [ ] **Matches semantic colors?** — Color carries its defined meaning (§4) and is never the sole signal.
- [ ] **Uses typography hierarchy?** — Type roles are chosen for meaning, from the defined hierarchy (§5).
- [ ] **Respects spacing system?** — All spacing comes from the shared scale (§6); no one-off gaps.
- [ ] **Accessible?** — Keyboard-operable, focus-visible, legible contrast, semantic structure, no color-only meaning
  (§12).
- [ ] **Responsive?** — Adapts by priority across the supported targets (§11).
- [ ] **Uses reusable components?** — Composed of [COMPONENTS](./COMPONENTS.md), not one-off UI.
- [ ] **Follows user flows?** — Serves a documented flow and its states ([USER_FLOWS](./USER_FLOWS.md)).
- [ ] **AI clearly identified?** — AI output is marked, traceable, provisional, and editable (§10).
- [ ] **No custom visual language introduced?** — No new color meaning, type step, spacing value, or pattern has entered
  without review.

---

## 14. Design System Ownership

Ownership exists to answer one question before a debate starts: **which document decides this?** A visual question with
no owner is settled locally, by whoever is closest to it, and that is precisely how a visual language fragments.

This section is **governance only**: §1 describes what each sibling document is *for*; this section states who *decides*
when a question could plausibly belong to more than one of them. It assigns authority and duplicates no content from the
documents it names.

**What this document owns.** The **visual language** and nothing else: the semantic meaning of color (§4), the
typography hierarchy (§5), the spacing system (§6), the principles for iconography (§7), layout composition (§8), the
visual meaning of feedback (§9) and of AI presentation (§10), responsive and theming intent (§11), the visual-language
obligations of accessibility (§12), and the token vocabulary through which all of it is expressed (§3). Where any of
these is in question, **this document decides.**

**What this document does not own.** It does not own **which components exist**, **what the user does**, **how the
language is worded in context**, **how any of it is built**, or **what the product is for**. It has no authority over
product behavior, and it MUST NOT be used to introduce any.

| A question about…                                                  | Is decided by                                                                                                                                                                                                                                                                                                                                              |
|--------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Whether a building block exists, or what it must do                | [COMPONENTS](./COMPONENTS.md). A need no component serves is a component gap, resolved through component review — **never** by styling around it here.                                                                                                                                                                                                     |
| Whether a step, state, or journey exists                           | [USER_FLOWS](./USER_FLOWS.md). This document gives a state its appearance; it never introduces one.                                                                                                                                                                                                                                                        |
| How something is worded, or how the language is applied in context | [UI_GUIDELINES](./UI_GUIDELINES.md). This document says what Error *means*; UI_GUIDELINES says how the message beside it is *worded*. Neither restates the other.                                                                                                                                                                                          |
| A value, a syntax, or how tokens are applied in code               | [FRONTEND_CODING_STANDARDS §12](../03-engineering/FRONTEND_CODING_STANDARDS.md#12-styling-standards). This document is the **conceptual** authority for what a token means; that document is the **enforcement** authority for how it is used.                                                                                                             |
| What the product is, does, or promises                             | The frozen documents — [PRODUCT_VISION](../00-product/PRODUCT_VISION.md), [PRODUCT_DECISIONS](../00-product/PRODUCT_DECISIONS.md), [PRD](../00-product/PRD.md), [SRS](../00-product/SRS.md), and the architecture documents. They **constrain this one absolutely**; when they and this document disagree, **they win** ([CLAUDE.md §3](../../CLAUDE.md)). |

**Cross-document changes require coordinated review.** The design layer's boundaries are clean but its changes are
frequently not: a single proposal often touches the visual language, a component, and a flow at once. Such a change MUST
be reviewed by **every owning document it touches**, and MUST NOT be merged into one while leaving another contradicting
it.

No document in the design layer may be changed in isolation in a way that makes a sibling wrong — a design system that
is internally consistent but disagrees with its own components is not a source of truth, only a fourth opinion. Where a
change reaches beyond the design layer into product behavior, it stops and is raised per
[CLAUDE.md §8](../../CLAUDE.md).

---

## Design Review Process

> *Unnumbered governance section. It defines when a visual change is reviewed and how the visual language evolves —
> deliberately, not by accident.*

**Review triggers** — a design review is required when any of the following occurs:

- **A new component** is proposed (it MUST draw its appearance from this language).
- **A new page** is introduced (it MUST compose existing patterns, not invent them).
- **A new interaction** is introduced (including any use of motion).
- **A new design token** is proposed.
- **A new status pattern** is proposed — any new way of expressing a state or condition.
- **A new AI presentation** is proposed — any change to how AI output is marked, grounded, or surfaced.
- **A significant visual redesign** is proposed.

**Review outcomes** — each review resolves to exactly one:

- **Approved** — the change conforms to the language and the rules; it is accepted as-is.
- **Refinement required** — the intent is sound but the expression must change (an accessibility gap, an inconsistent
  semantic, an unnecessary new variant) before acceptance.
- **New token required** — the need is legitimate and no existing token expresses it; a token is added **additively**,
  named semantically, and never by redefining an existing one.
- **Component update required** — the need is structural rather than visual; it is routed to the
  [Component Review Process](./COMPONENTS.md) rather than solved by local styling.
- **Architecture review required** — the change implies behavior or scope the frozen documents do not grant; it stops
  and is raised per [CLAUDE.md §8](../../CLAUDE.md) before proceeding.

**How the visual language evolves:** **incrementally, one reviewed decision at a time** — never by speculative up-front
expansion, and never by a change that quietly reaches every screen. New tokens are **additive**; existing meanings are
**stable**; a redefinition is treated as a breaking change and reviewed as one. The language stays deliberately small,
because a vocabulary nobody can hold in mind is one nobody applies consistently — and inconsistency was the only problem
it existed to solve.

**Synchronization with COMPONENTS and USER_FLOWS:** the three design documents move **together**. A visual change that
requires a new structural building block is a [COMPONENTS](./COMPONENTS.md) change and is reviewed there. A visual
change that implies a new step, state, or journey is a [USER_FLOWS](./USER_FLOWS.md) change and is reviewed there — and
if it implies new *product* behavior, it stops and is raised per [CLAUDE.md §8](../../CLAUDE.md). None of the three may
be changed in isolation in a way that leaves another contradicting it; when they disagree, the frozen documents above
them decide.

---

## 15. Future Design Evolution

Possibilities the visual language is **shaped to accommodate** — recorded so that the MVP's boundaries are understood as
deliberate rather than accidental, and so that a future need is met by an approved decision rather than an improvised
one.

> **Every item below is outside MVP scope and requires product approval before any implementation.** None is a
> commitment, a plan, or a schedule. Nothing here may be built, prototyped, or partially introduced on the strength of
> its appearance in this list; each would enter through the normal product change process and, where it affects a frozen
> document, per [CLAUDE.md §8](../../CLAUDE.md).

| Possibility                             | Why the language is ready for it                                                                                                                                                                                                                                                                                                 |
|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Additional visual themes**            | Color tokens name **roles, not hues** (§3, §4). A theme resolves the same roles to different values; the meaning of Error, Success, or Primary would not change (§11).                                                                                                                                                           |
| **Dark mode**                           | The specific theme most likely to be asked for, and explicitly out of scope today (§11). It is listed here to be clear that its absence is a scope boundary rather than an omission to be quietly filled.                                                                                                                        |
| **Localization-aware typography**       | [NFR-016](../00-product/SRS.md#9-non-functional-requirements) already establishes internationalization *readiness* — V1 targets a single primary language, with text and formats structured to permit future localization. A type hierarchy of named roles (§5) can be resolved per locale without redefining what a role means. |
| **Accessibility improvements**          | Accessibility constrains the language rather than decorating it (§12), so raising the bar is **additive**. The current target is WCAG 2.1 AA for core flows ([NFR-011](../00-product/SRS.md#9-non-functional-requirements)); exceeding it would require no structural change here.                                               |
| **Expanded responsive targets**         | Today's targets are desktop and tablet ([NFR-017](../00-product/SRS.md#9-non-functional-requirements)); mobile is explicitly not one (§11). Because adaptation is priority-based rather than fixed, a new target would be a product decision about *support*, not a rebuild of the language.                                     |
| **Higher-density professional layouts** | Accountants working long sessions may eventually want more information per screen. Because spacing comes from one shared scale (§6), density is a property of the scale rather than of each screen — but any change to it is a change to the language and is reviewed as one.                                                    |

**The point of recording these:** a design system is most often broken by an urgent need it never anticipated, met
locally and permanently. Naming these possibilities in advance does not commit the product to any of them; it commits
the *language* to being able to absorb them **additively**, through review, if the product ever approves them — which is
the same discipline as *new tokens are additive rather than replacing existing ones* (Design System Rules), applied at
the scale of the whole system.

---

## 16. Design System Decision Summary

The load-bearing decisions behind this visual language, recorded so they are not silently reversed.

| Decision                            | Chosen Approach                                                                                     | Alternatives                                                                       | Rationale                                                                                                                                                                                                                                                  |
|-------------------------------------|-----------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Semantic colors**                 | Color is named for the meaning it carries, never for its hue                                        | A literal palette named by appearance; per-feature color choices                   | Meaning stays stable while appearance may change, so the palette can evolve without re-teaching the user; literal names couple every screen to a value and make change a rewrite.                                                                          |
| **Token-driven styling**            | One named, semantic token vocabulary as the single source of visual truth                           | Per-component or per-screen styling; ad hoc values                                 | Tokens make appearance reviewable, changeable in one place, and impossible to fork by accident — the mechanism every other rule here depends on ([FRONTEND_CODING_STANDARDS §12](../03-engineering/FRONTEND_CODING_STANDARDS.md#12-styling-standards)).    |
| **Component reuse**                 | Components inherit appearance from the Design System                                                | Components carry their own appearance                                              | Inheritance is what makes a change to the language reach every screen; self-styled components fork the system silently and drift independently ([COMPONENTS](./COMPONENTS.md)).                                                                            |
| **Accessibility-first**             | Accessibility constrains the language and overrides aesthetics                                      | Aesthetics first, accessibility audited later                                      | Built into the vocabulary it is inherited by every screen and cannot be forgotten; retrofitted it is costly, inconsistent, and never finished ([NFR-011](../00-product/SRS.md#9-non-functional-requirements)).                                             |
| **Responsive-first**                | Adaptation is a property of how the language composes, by priority                                  | Fixed layouts with per-screen exceptions added later                               | A layout built on fixed assumptions cannot be made responsive afterwards, only rebuilt; priority-based adaptation keeps what matters reachable as space contracts ([NFR-017](../00-product/SRS.md#9-non-functional-requirements)).                         |
| **AI transparency**                 | AI output is visibly marked, traceable, provisional, and editable                                   | Present AI output rendered identically to verified content                         | Every AI safeguard is reversed at the last inch if generated text looks like verified text; trust is established at the point of presentation ([AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md), [BR-031](../00-product/SRS.md#5-business-rules)). |
| **Single theme, theme-ready**       | One theme for the MVP, with tokens named so that a future theme would change values but no meanings | Ship dark mode or theme support in the MVP; defer theming without preparing for it | No frozen document grants theming, so building it would invent scope ([CLAUDE.md §8](../../CLAUDE.md)); naming roles rather than hues (§3, §4) means a later theme — if the product ever approves one — is additive rather than a rewrite (§11, §15).      |
| **Minimal visual language**         | A deliberately small vocabulary, consistently applied                                               | A rich vocabulary with many variants per need                                      | A vocabulary nobody can hold in mind is one nobody applies consistently; scarcity is what lets a semantic signal mean anything when it appears.                                                                                                            |
| **Progressive disclosure**          | The language provides hierarchy, grouping, and depth to defer detail                                | Present all detail at once                                                         | Keeps each step comprehensible and cognitive load low; front-loading everything overwhelms and slows the professional down ([USER_FLOWS §2](./USER_FLOWS.md#2-user-flow-philosophy)).                                                                      |
| **Design consistency over novelty** | Visual consistency takes precedence over local creativity                                           | Allow per-screen visual expression                                                 | Each deviation is individually defensible and collectively fatal; consistency is what makes the product learnable and trustworthy, and it survives only as a rule rather than an aspiration.                                                               |

---

*This document defines LedgerAI's visual language; it does not override the frozen documents under
[`docs/`](../). It serves the product behavior defined in [PRD](../00-product/PRD.md) and [SRS](../00-product/SRS.md),
expresses the AI obligations of [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md), gives appearance to the
vocabulary in [COMPONENTS](./COMPONENTS.md), is sequenced into screens by [USER_FLOWS](./USER_FLOWS.md), is applied in
context by [UI_GUIDELINES](./UI_GUIDELINES.md), and is realized in code by
[FRONTEND_CODING_STANDARDS](../03-engineering/FRONTEND_CODING_STANDARDS.md). When a design decision is required, review
it through the process above and, when a change would imply new product behavior or contradict a frozen contract, stop
and raise it per [CLAUDE.md §8](../../CLAUDE.md).*
