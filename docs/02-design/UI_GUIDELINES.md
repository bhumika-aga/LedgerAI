# UI Guidelines — LedgerAI MVP

> **Status:** Draft v1
> **Owner:** Principal UX Architect
> **Last updated:** 2026-07-15
> **Upstream (frozen):
** [PRD](../00-product/PRD.md) · [SRS](../00-product/SRS.md) · [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) · [SECURITY](../01-architecture/SECURITY.md)
> **Related:
** [DESIGN_SYSTEM](./DESIGN_SYSTEM.md) · [COMPONENTS](./COMPONENTS.md) · [USER_FLOWS](./USER_FLOWS.md) · [FRONTEND_CODING_STANDARDS](../03-engineering/FRONTEND_CODING_STANDARDS.md) · [CLAUDE.md](../../CLAUDE.md)

---

## 1. Purpose

### Why this document exists

The other design documents each answer their question in isolation. [DESIGN_SYSTEM](./DESIGN_SYSTEM.md) says what Error
*means*. [COMPONENTS](./COMPONENTS.md) says an Alert *exists* and what it must do. [USER_FLOWS](./USER_FLOWS.md) says
the user *reaches* a failure and must be offered a way forward. None of them says **what the screen actually tells the
accountant at that moment, in what words, and in what tone**. That is this document.

This document defines **how LedgerAI's visual language is applied in context** — the conventions governing tone,
microcopy, interaction norms, and page behavior that turn correct components, arranged in a correct sequence, into an
experience that feels like one product made by one team.

It is **not** a design system, **not** a component catalog, **not** a user-flow document, and **not** a code standard.
It contains **no visual values, no measurements, no code, no wireframes, and no mockups**. It describes *what the
product says and how it behaves in context* — never what it looks like or how it is built.

The governing principle of this document:

> **The product speaks with one voice.**
>
> A screen that invents its own tone, its own word for an existing concept, or its own way of confirming a decision is a
> defect against this document — even when every component it uses is correct and every step it follows is documented.
> Coherence is not the sum of correct parts.

### Relationship to the design layer

The design layer is four documents with four distinct jobs. They are complementary and MUST never contradict:

> **DESIGN_SYSTEM defines the visual language. COMPONENTS define reusable building blocks. USER_FLOWS define behavior
> and journeys. UI_GUIDELINES define how the visual language is applied in context.**

| Document                                                                       | Its job                      | The boundary with this document                                                                                                                                                                                                                                                                                                    |
|--------------------------------------------------------------------------------|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [DESIGN_SYSTEM.md](./DESIGN_SYSTEM.md)                                         | The **visual language**      | It owns the vocabulary; this document owns its **usage**. It decides what Error *means* and how it is signalled; this document decides what the message beside it *says*. This document introduces no visual meaning and MUST NOT be used to work around one ([DESIGN_SYSTEM §14](./DESIGN_SYSTEM.md#14-design-system-ownership)). |
| [COMPONENTS.md](./COMPONENTS.md)                                               | **Reusable building blocks** | It decides which components exist and what each must do; this document decides **how they are used in context** — which one suits a situation, and what it says when used. A need no component serves is a component gap, resolved through component review, never improvised here.                                                |
| [USER_FLOWS.md](./USER_FLOWS.md)                                               | **Behavior and journeys**    | It decides the steps, states, and outcomes of a journey; this document decides **how each step communicates**. Flows define the path; this document shapes how walking it feels. It never adds a step, state, or outcome.                                                                                                          |
| [FRONTEND_CODING_STANDARDS.md](../03-engineering/FRONTEND_CODING_STANDARDS.md) | **Realization in code**      | It defines how the interface is built and enforces these expectations in code. This document is the **conceptual** authority for the convention; that document is the **enforcement** authority. Structure, state, and implementation are its concern, never this document's.                                                      |

### Relationship to the frozen product documents

This document introduces **no product behavior**. It governs only how documented behavior is *expressed*.

[PRD](../00-product/PRD.md) grants the capabilities. [SRS](../00-product/SRS.md) defines their precise behavior — the
business rules, the validation ([§6](../00-product/SRS.md#6-validation-rules)), the state models
([§7](../00-product/SRS.md#7-state-models)), and the error categories and recovery expectations
([§8](../00-product/SRS.md#8-error-handling)) that this document words but never redefines. Where a UI convention would
imply behavior the frozen documents do not grant, it stops and is raised per [CLAUDE.md §8](../../CLAUDE.md).

---

## 2. UI Philosophy

These principles explain *why* LedgerAI's conventions are shaped the way they are. They are the reasoning behind the
enforceable rules that follow.

| Principle                          | Why it exists                                                                                                                                                                                                                                                           |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Clarity over cleverness**        | A clever interface is one the user has to decode. Accountants are working, often under deadline, frequently tired. Wording that is plain to the point of dullness is a feature: it is read correctly the first time, by everyone, including the reader who is skimming. |
| **Human-centered professionalism** | The audience is Chartered Accountants, CPAs, and auditors doing skilled, accountable work. The product addresses them as competent professionals — never scolding, never congratulating them for routine work, never performing enthusiasm they did not ask for.        |
| **Consistency across surfaces**    | The same situation MUST be communicated the same way everywhere. When wording and behavior are consistent the user learns the product once; when they vary, every screen is a fresh negotiation and nothing transfers.                                                  |
| **Minimal friction**               | The promise is "hours to minutes." Every avoidable confirmation, field, and sentence is taxed against it. The product asks for what the goal genuinely requires and nothing more.                                                                                       |
| **Progressive disclosure**         | Detail arrives when it is needed, not before. This keeps each step comprehensible and is why complexity in LedgerAI is deep rather than wide ([USER_FLOWS §2](./USER_FLOWS.md#2-user-flow-philosophy)).                                                                 |
| **Respect for attention**          | Attention is the scarcest resource on the screen, and it is spent rather than renewed. Every interruption, notice, and animation draws from a finite budget the user would rather spend on the document. Silence is the default; speaking is the exception.             |
| **Predictable interaction**        | The same gesture MUST always produce the same kind of result. Predictability is what lets a user act without hesitating — and hesitation, repeated across a working day, is exactly the cost the product exists to remove.                                              |
| **Honest messaging**               | The product says what is true, including when the truth is a failure or an absence. A reassuring message that is wrong costs more than an uncomfortable one that is right, because it spends trust that cannot be re-earned cheaply.                                    |
| **AI transparency**                | LedgerAI advises; the professional decides and signs. Wherever AI speaks, the interface makes clear that it is AI speaking, what it is based on, and that the human owns the result ([AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md)).                         |
| **Accessible by default**          | Accessibility is a property of how the product is built and worded, not a pass applied afterwards. A message that only makes sense in color, or a control reachable only by pointer, is not a lesser experience — it is an absent one.                                  |

---

## UI Guidelines Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Every screen MUST communicate its purpose clearly.** *A user who has to infer what a screen is for has already been
  slowed by it. The purpose is stated, not implied by layout or discovered by clicking.*
- **Every message MUST be concise, factual, and user-centered.** *Messages are read mid-task, often peripherally. A
  message written from the system's point of view ("the operation failed") describes the system's experience; the user
  needs to know what happened to their work and what to do about it.*
- **UI copy MUST avoid jargon unless it is already user-meaningful in the frozen documents.** *The product's domain
  vocabulary — Client, Document, Summary, Report, Activity — is shared with the user and is safe. Its architectural
  vocabulary is not, and leaking it makes the user pay to understand our internals (§4).*
- **Empty states MUST explain what is missing and what to do next.** *A blank region is indistinguishable from a broken
  one. An empty state is often the user's first encounter with a feature and is therefore the cheapest place to teach
  it ([USER_FLOWS §15](./USER_FLOWS.md#15-cross-flow-principles)).*
- **Loading states MUST make progress or waiting explicit.** *OCR and AI generation take real time, and silence is
  indistinguishable from a hang. Long-running operations MUST show status and MUST NOT block the interface
  ([NFR-002](../00-product/SRS.md#9-non-functional-requirements)).*
- **Error states MUST explain the issue and the next action.** *An error that names a problem without a way forward is a
  dead end. Every failure carries recovery — retry, edit, or a clear exit
  ([SRS §8](../00-product/SRS.md#8-error-handling)).*
- **Confirmation dialogs MUST state the consequence plainly.** *A confirmation the user cannot evaluate is a formality
  that trains them to click through. The dialog states what will happen, in the words of the thing it happens to
  ([USER_FLOWS §15](./USER_FLOWS.md#15-cross-flow-principles)).*
- **AI-generated content MUST remain clearly identified and editable.** *The human-in-the-loop guarantee of
  [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md), expressed in words: output that is not visibly AI's, or not
  visibly the user's to change, invites acceptance without review ([BR-031](../00-product/SRS.md#5-business-rules)).*
- **Navigation MUST remain predictable.** *The same navigational gesture behaves the same way in every context, so the
  user never relearns how to move ([USER_FLOWS §15](./USER_FLOWS.md#15-cross-flow-principles)).*
- **Contextual help SHOULD appear only where it reduces uncertainty.** *Help attached to something already clear is
  noise, and noise teaches the user to ignore the next one — which may have mattered. Help earns its place by removing a
  specific doubt.*
- **UI behavior MUST remain aligned with the design system, components, and flows.** *This document is the last of the
  four and therefore the easiest place to quietly contradict the other three. A convention here that disagrees with them
  is a defect in this document, not a local exception (UI Review Process).*

**Why these rules exist.** Tone, wording, and interaction detail are the parts of a product nobody owns by default.
Components get reviewed; visual language gets reviewed; a sentence written at six in the evening to unblock a release
does not — and it is permanent the moment it ships.

These rules exist because incoherence accumulates in exactly that space, one defensible sentence at a time, until the
product reads as though several teams built it in different years. That is the impression the accountant forms before
they form any other, and it is the one this document exists to prevent.

---

## 3. Tone and Voice

LedgerAI's voice is **professional, calm, direct, respectful, and confident without being overbearing**. It is the voice
of a competent colleague who explains something once, accurately, and then gets out of the way.

| Quality                        | What it means in practice                                                                                                                                                                         |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Professional**               | The register of a working instrument used beside Tally, QuickBooks, and Excel. No slang, no humor at the user's expense, no personality competing with the work.                                  |
| **Calm**                       | The product does not raise its voice. Urgency is conveyed by *what* is said, never by exclamation or alarm — a system that sounds alarmed about small things cannot be believed about large ones. |
| **Direct**                     | The most important fact comes first. The user should be able to stop reading after the first sentence and still know what happened.                                                               |
| **Respectful**                 | The user is a professional who knows their domain better than the product does. The product never explains their job to them, and never implies a mistake was carelessness.                       |
| **Confident, not overbearing** | The product states what it knows plainly and what it does not know honestly. It neither hedges everything into vagueness nor asserts more certainty than it has (§8).                             |

**The voice is constant; the register shifts with the situation.** How the product sounds never changes — but how much
it says, and how much weight it carries, does:

| Situation                | How the voice adapts                                                                                                                                                                                                                           |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Success**              | Brief and unremarkable. Confirmation closes the loop and gets out of the way; routine work is not celebrated. Enthusiasm here spends attention the user did not agree to spend.                                                                |
| **Errors**               | Factual and non-blaming. State what happened, then what to do. Never "you entered an invalid value" where "this date must be in the past" says the same thing and is actionable. Never internals ([SECURITY](../01-architecture/SECURITY.md)). |
| **Warnings**             | Measured. A warning means *proceed with care*, not *you have failed*, and the register must make that distinction audible — the user acts on the difference.                                                                                   |
| **Empty states**         | Helpful and welcoming, never apologetic. Nothing has gone wrong; there is simply nothing here yet. This is the product's best teaching moment and should read as an invitation.                                                                |
| **AI content**           | Provisional. The register makes clear this is a starting point the human owns — offered, not asserted. AI never speaks with the product's own authority (§8).                                                                                  |
| **Confirmation prompts** | Neutral and precise. State the consequence and let the professional decide. A prompt that argues for one option is not asking; it is steering.                                                                                                 |
| **Destructive actions**  | Plain and unambiguous about what is lost and whether it can be recovered. Never softened — softening a destructive consequence to seem friendly is how a user agrees to something they did not understand.                                     |

> This section governs **register**, not appearance: it says how the product sounds, not how it looks. Where a situation
> carries a visual meaning as well, [DESIGN_SYSTEM](./DESIGN_SYSTEM.md) governs the appearance and this document governs
> the wording. Neither restates the other.

---

## 4. Microcopy Principles

Microcopy is the product's highest-traffic writing: read constantly, mid-task, at speed, and usually only once. It
carries more of the experience than any other text, and it is the text most often written last.

**The principles that govern all of it:**

- **Say what happens.** A label describes the outcome of using it, not the mechanism behind it. The user should be able
  to predict the result before acting.
- **Avoid ambiguity.** If a message can be read two ways it will be — by the reader least able to afford the wrong one.
  Precision costs a word; ambiguity costs a mistake.
- **Avoid internal jargon.** The user shares the product's *domain* vocabulary, not its *architectural* vocabulary. They
  know Client, Document, Summary, Report, and Activity. They do not know — and MUST NOT be shown — the terms the system
  uses to talk to itself.
- **Avoid unnecessary words.** Every word is read by every user, every time. Politeness that adds length without adding
  meaning is not politeness; it is a tax.
- **Avoid blame.** The interface never implies the user erred through carelessness. State the condition, not the fault.
- **Preserve trust.** Never overstate what happened. "Saved" MUST mean saved. A message that is optimistic ahead of the
  server is a lie the user discovers later, at the worst possible moment.
- **Write complete, self-contained messages.** Each message stands alone as a sentence rather than being assembled from
  fragments that only cohere in one language — which also keeps user-facing text structured to permit future
  localization ([NFR-016](../00-product/SRS.md#9-non-functional-requirements)).

**Applied to each kind of text:**

| Kind                  | What governs it                                                                                                                                                                                                                                                |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Labels**            | Name the thing in the user's terms. Short, stable, and identical wherever the same concept appears — a concept with two names is two concepts to the reader.                                                                                                   |
| **Helper text**       | Present only where a genuine doubt exists: a constraint that cannot be inferred, a format that matters. Helper text restating the label is noise wearing a helpful expression.                                                                                 |
| **Placeholders**      | An optional hint, never a carrier of meaning. A placeholder MUST NOT substitute for a label — it disappears exactly when the user needs it and is not a reliable label for assistive technology ([COMPONENTS §4](./COMPONENTS.md#4-component-specifications)). |
| **Error messages**    | What happened, then what to do. Specific to the field or operation that failed, in plain language, without internals ([SRS §8](../00-product/SRS.md#8-error-handling), [SECURITY](../01-architecture/SECURITY.md)).                                            |
| **Confirmation text** | The consequence, stated plainly: what will happen, to what, and whether it can be undone. The user must be able to decide from the dialog alone.                                                                                                               |
| **Button labels**     | The action, in a verb the user would use, matching the sentence that prompted it. A button labelled generically where the consequence is specific is where accidental destruction happens.                                                                     |
| **Tooltips**          | Supplementary, never essential. A tooltip MUST NOT be the only place meaning lives; it is unavailable to many users and unreachable in many contexts. If it is essential, it is not a tooltip.                                                                 |
| **Status text**       | The current state, in the user's terms and consistent with the state model that owns it ([SRS §7](../00-product/SRS.md#7-state-models)). The same state MUST read identically everywhere it appears.                                                           |
| **AI labels**         | Unambiguous attribution: this came from AI. Worded consistently everywhere AI appears, because inconsistent marking teaches the user that unmarked content might also be generated (§8).                                                                       |

---

## 5. Interaction Conventions

The norms every screen inherits, so that a gesture learned once transfers everywhere. The behaviors below are the
**contextual conventions**; the journeys they serve are owned by [USER_FLOWS](./USER_FLOWS.md) and the components that
express them by [COMPONENTS](./COMPONENTS.md). This section adds no step, state, or outcome to any flow.

| Convention                          | The norm                                                                                                                                                                                                                                                                                |
|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Primary action placement**        | One primary action per screen, consistently and predictably placed, visually unambiguous ([USER_FLOWS §2](./USER_FLOWS.md#2-user-flow-philosophy)). Where a second action is offered it MUST NOT compete for the same emphasis.                                                         |
| **Cancel behavior**                 | Cancel always means *leave without changing anything* — everywhere, without exception. Where unsaved work would be lost, cancelling confirms first; but cancel never itself becomes the destructive act.                                                                                |
| **Save behavior**                   | The user is told the outcome, and the outcome is the truth. A save is confirmed only once the server has confirmed it; a pending save never presents itself as complete ([NFR-005](../00-product/SRS.md#9-non-functional-requirements)).                                                |
| **Destructive action confirmation** | Consequential and destructive actions are confirmed, the consequence stated plainly, and the destructive option is never the effortless default ([USER_FLOWS §15](./USER_FLOWS.md#15-cross-flow-principles)). Where removal is reversible, say so; where it is not, say that instead.   |
| **Retry behavior**                  | Where an operation can fail transiently, retry is offered *in place*, without making the user rebuild their context. Retry MUST NOT silently duplicate work — the user needs to know whether they are repeating or resuming.                                                            |
| **Navigation recovery**             | Refreshes, transient interruptions, and accidental navigation restore the user's context where practical rather than forcing a restart ([USER_FLOWS §15](./USER_FLOWS.md#15-cross-flow-principles)). The user returns to where they were, not to the beginning.                         |
| **Unsaved changes handling**        | Work in progress is never discarded silently. Where navigation would lose unsaved edits, the product says so and offers a choice — the contextual expression of *users MUST never lose work silently* ([USER_FLOWS](./USER_FLOWS.md)).                                                  |
| **Progressive disclosure**          | Advanced or secondary detail is revealed on demand rather than presented up front. What is deferred MUST remain discoverable: progressive disclosure hides complexity, never capability.                                                                                                |
| **Editability of AI output**        | AI output is editable wherever it appears, and its editability is apparent without discovery ([BR-031](../00-product/SRS.md#5-business-rules)). No consequential action follows from AI output without an explicit human step (§8).                                                     |
| **Consistent route behavior**       | Moving to a location behaves the same way regardless of the route taken. Protected areas behave consistently when a session is absent or expired, returning the user to sign-in rather than to a broken or empty screen ([USER_FLOWS §4](./USER_FLOWS.md#4-authentication-flow-uf-01)). |

---

## 6. Page-Level Guidance

How each kind of surface **behaves and communicates** — not how it is arranged, which is
[DESIGN_SYSTEM §8](./DESIGN_SYSTEM.md#8-layout-principles). Each surface below realizes a documented flow and adds
nothing to it.

| Surface               | How it should behave                                                                                                                                                                                                                                                                                                                          |
|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Dashboard**         | Orients and offers the next task ([UF-03](./USER_FLOWS.md#3-flow-catalog)). It answers *what needs my attention and where do I start*, and it is honest when the answer is "nothing yet" — a new account's dashboard is an empty state with a first action, not a wall of zeroes. It composes several areas without becoming any one of them. |
| **List pages**        | Let the user find and choose. They show only the user's own records, communicate the current filter or query honestly, and give an explicit empty state that distinguishes *nothing exists yet* from *nothing matched*. Those are different situations and MUST NOT read the same.                                                            |
| **Detail pages**      | Establish what this is before offering what can be done with it. Available actions reflect the record's actual state — an action that cannot succeed is not offered as though it can ([SRS §7](../00-product/SRS.md#7-state-models)).                                                                                                         |
| **Forms**             | One clear path with one clear outcome. Input survives validation failures and interruptions; the user is never returned to an empty form after an error (§9).                                                                                                                                                                                 |
| **Review surfaces**   | Where the professional reads a document alongside what was derived from it. Source and derived content MUST remain visibly distinct — the entire value of the surface is the user's ability to check one against the other.                                                                                                                   |
| **AI surfaces**       | Always identify AI as the author, show what the output is grounded in, keep it editable, and present it as provisional (§8). Progress is visible while generating; failure is honest rather than silent.                                                                                                                                      |
| **Search results**    | Answer quickly, scoped to the user's own data. Say what was searched for, distinguish *no matches* from *nothing to search yet*, and never present a blank region as a result ([UF-11](./USER_FLOWS.md#11-search-flow-uf-11)).                                                                                                                |
| **Activity timeline** | Present an honest, read-only record ([UF-12](./USER_FLOWS.md#12-activity-timeline-flow-uf-12)). It is a record, not a feed: it does not editorialize, celebrate, or invite interaction with the past beyond navigating to what still exists.                                                                                                  |
| **Profile pages**     | Self-only, plain, and predictable ([UF-13](./USER_FLOWS.md#13-profile-flow-uf-13)). Changes are reported as saved only when they are, and cancelling is safe.                                                                                                                                                                                 |
| **Logout path**       | Ends the session unambiguously and returns the user to an unauthenticated state ([UF-14](./USER_FLOWS.md#14-logout-flow-uf-14)). Afterwards the product behaves as signed out — protected content is not reachable by navigating back, and nothing cached is shown as though the session persisted.                                           |

---

## 7. Messaging Patterns

**The categories of failure, their user-visible behavior, and their recovery expectations are owned by
[SRS §8](../00-product/SRS.md#8-error-handling). Their visual treatment is owned by
[DESIGN_SYSTEM §9](./DESIGN_SYSTEM.md#9-feedback-patterns).** This section owns neither. It governs only **what each
kind of message must communicate in words**, so the same situation is worded the same way on every screen.

| Message kind              | What it must communicate                                                                                                                                                                                                                               |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Loading**               | That the system is working and — where it is honestly known — how far along it is. Never invent precision the system does not have; an indeterminate wait stated honestly beats a fabricated estimate.                                                 |
| **Success**               | That the specific thing the user did is done. Named, brief, past tense. Generic success after a specific action leaves the user checking whether it meant theirs.                                                                                      |
| **Warning**               | What deserves attention, why it matters, and that the user may still proceed. The distinction from an error MUST be unmistakable from the wording alone.                                                                                               |
| **Error**                 | What happened, in the user's terms, and what to do next. Never internals, never blame, never a dead end ([SECURITY](../01-architecture/SECURITY.md)).                                                                                                  |
| **Empty**                 | What would appear here, why it is not here yet, and the action that changes that. It MUST distinguish *nothing exists yet* from *nothing matched your query* — the same blank region, an entirely different user situation.                            |
| **Disabled**              | Why the control is unavailable and what would make it available. A disabled control whose reason is undiscoverable reads as a bug and is a dead end in disguise.                                                                                       |
| **AI-generated**          | That AI produced this, worded consistently everywhere it appears (§8).                                                                                                                                                                                 |
| **Grounded**              | What the output is based on, in a form the user can follow back to the source ([AI_ARCHITECTURE §9](../01-architecture/AI_ARCHITECTURE.md#9-grounding-strategy)). Grounding stated but not traceable is a claim, not evidence.                         |
| **Not found in document** | That the document does not support an answer — an honest, legitimate outcome, **not** a failure and not an apology. This is the product working correctly, and the wording MUST NOT imply otherwise ([BR-033](../00-product/SRS.md#5-business-rules)). |
| **Action unavailable**    | That the action does not apply in the current state, and what state would allow it. Distinct from an error: nothing failed.                                                                                                                            |
| **Retry available**       | That the attempt can be repeated, and whether repeating resumes or restarts. The user needs to know what they are about to do again.                                                                                                                   |

---

## 8. AI Presentation in Context

**The visual obligations of AI presentation are owned by [DESIGN_SYSTEM §10](./DESIGN_SYSTEM.md#10-ai-presentation);
the AI behavior itself is owned by [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md).** This section owns the
**contextual and verbal** dimension only — what an AI surface says and how it conducts itself on a real screen. It
defines no prompts and no models.

The premise everything below serves: the professional signs the work, so the interface must never let them mistake the
machine's suggestion for their own judgment.

| Expectation                   | In context                                                                                                                                                                                                                                                                                                                          |
|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Visible AI labeling**       | Every AI surface identifies AI as the author, in words, using consistent wording everywhere. Inconsistent labeling is worse than none: it teaches the user that unlabeled content might also be generated.                                                                                                                          |
| **Grounded / cited output**   | The output shows what it is based on, traceably back to the document ([AI_ARCHITECTURE §9](../01-architecture/AI_ARCHITECTURE.md#9-grounding-strategy)). A professional's work is checkable by definition; output that cannot be checked cannot be used.                                                                            |
| **Editable drafts**           | AI output reads as a draft the human owns. Editability is apparent without discovery, because output that looks finished invites acceptance without review ([BR-031](../00-product/SRS.md#5-business-rules)).                                                                                                                       |
| **Human review emphasis**     | The human's decision is the consequential act; AI output is input to it. No consequential action follows from AI output without an explicit human step — an AI email is drafted and **never sent** ([BR-034](../00-product/SRS.md#5-business-rules)).                                                                               |
| **Regeneration behavior**     | A fresh attempt is available wherever AI output appears, reinforcing that output is provisional. Regeneration never silently discards the user's edits ([USER_FLOWS §7](./USER_FLOWS.md#7-ai-summary-flow-uf-07)); the wording makes clear what will happen to existing work *before* it happens.                                   |
| **Failure communication**     | AI failure is stated honestly, with a way forward, and without provider or internal detail ([AI_ARCHITECTURE §12](../01-architecture/AI_ARCHITECTURE.md#12-ai-failure-handling)). A disguised AI failure is the most corrosive failure in the product, because it looks like an answer.                                             |
| **Provenance after editing**  | Where it aids understanding, the distinction between the original AI output and the user's subsequent edits is preserved ([DESIGN_SYSTEM §10](./DESIGN_SYSTEM.md#10-ai-presentation)). *What did the machine say, and what did I decide?* is a question about the professional's own accountability.                                |
| **Never overstate certainty** | The interface never implies confidence beyond the evidence and never invents a number to express it ([AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md)). Where reliability is qualified — such as a low-confidence extraction ([FR-OCR-006](../00-product/SRS.md#46-ocr-ocr)) — it is said plainly so the user can judge it. |

---

## 9. Form and Validation Guidance

**The validation rules themselves are owned by [SRS §6](../00-product/SRS.md#6-validation-rules); their implementation
by [FRONTEND_CODING_STANDARDS §9](../03-engineering/FRONTEND_CODING_STANDARDS.md#9-forms--validation).** This section
owns the **experience of being validated** — when the product speaks, and how.

| Aspect                | The convention                                                                                                                                                                                                             |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Required fields**   | Indicated before submission, consistently, and never by a single visual mark alone. The user should know what is required while filling the form, not after failing it.                                                    |
| **Validation timing** | Validate at a moment that helps rather than harasses. Correcting a field the user has not finished typing is criticism of unfinished work; the natural moment is when they leave the field, or on submission.              |
| **Inline errors**     | Appear beside the field they concern, describing the constraint rather than the violation. The message says what is needed, not merely that what is there is wrong.                                                        |
| **Submit behavior**   | One clear submit, one clear outcome. While in progress the state is visible and re-submission is prevented; the user learns the result without wondering whether it went through.                                          |
| **Preserving input**  | Input survives validation failures, transient errors, and navigation. The user is never returned to an empty form — retyping is the most avoidable and least forgivable cost an interface can impose.                      |
| **Disabled submit**   | Where submit is unavailable, the reason is discoverable. A permanently disabled button with no explanation is a dead end; where the reason cannot be made discoverable, allow the attempt and explain the failure instead. |
| **Unsaved changes**   | Leaving with unsaved edits prompts before discarding. Silence here is the fastest way to lose trust, because the loss is invisible until it matters (§5).                                                                  |
| **Field-level help**  | Only where a real constraint is not otherwise knowable. Help on an obvious field trains the user to skip help on the field that needed it (§4).                                                                            |

> **The server remains authoritative.** Client-side feedback is a courtesy that makes a form pleasant to complete; it
> never decides whether input is acceptable, and the interface MUST NOT present it as though it had
> ([SRS §6](../00-product/SRS.md#6-validation-rules)).

---

## 10. Accessibility in Context

**Accessibility is already owned four times over:** [DESIGN_SYSTEM §12](./DESIGN_SYSTEM.md#12-accessibility) sets the
visual-language intent, [COMPONENTS §8](./COMPONENTS.md#8-accessibility) builds it into each component,
[FRONTEND_CODING_STANDARDS §11](../03-engineering/FRONTEND_CODING_STANDARDS.md#11-accessibility-standards) enforces it
in code, and [TESTING_STRATEGY §8](../03-engineering/TESTING_STRATEGY.md#8-ui-testing-strategy) verifies it. The product
target is WCAG 2.1 AA for core flows ([NFR-011](../00-product/SRS.md#9-non-functional-requirements)).

This section restates none of that. It owns the one part the other four cannot cover: **accessibility decided in
context rather than in a component** — the choices a screen makes about what it says and how it composes accessible
parts. A product can be assembled entirely from accessible components and still be unusable.

| In context                 | What the screen is responsible for                                                                                                                                                                                              |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Focus visibility**       | Never suppressed for appearance, and never lost by a screen's own choices — after a dialog closes or content replaces content, focus lands somewhere sensible rather than at the top of the document.                           |
| **Keyboard operability**   | Every action a screen offers is reachable without a pointer, in an order matching how the screen reads. Where reading order and focus order disagree, the screen is wrong.                                                      |
| **Semantic interaction**   | Controls behave as what they appear to be. A thing that looks like a button, acts like a button, and is announced as a button is the whole requirement.                                                                         |
| **Readable labels**        | Every control carries a name saying what it does, not what it looks like. Icon-only actions carry that name whether or not it is visible (§4).                                                                                  |
| **Readable error states**  | Errors are announced, associated with what they concern, and legible as text — an error identifiable only by a colored border does not exist for a substantial number of users.                                                 |
| **Non-color-only meaning** | No status, validity, or provenance is carried by color alone; text or position carries it and color reinforces ([DESIGN_SYSTEM §4](./DESIGN_SYSTEM.md#4-color-system)).                                                         |
| **Respectful motion**      | Motion honors a reduced-motion preference and never gates task completion for effect. Where motion and speed conflict, speed wins ([DESIGN_SYSTEM](./DESIGN_SYSTEM.md#design-system-rules)).                                    |
| **Predictable navigation** | Movement behaves identically across the product and nothing relocates under the user mid-task. Predictability is an accessibility property before it is a comfort ([USER_FLOWS §15](./USER_FLOWS.md#15-cross-flow-principles)). |

---

## 11. Review Checklist

Every screen, message, or interaction — new or changed — is evaluated against this checklist before acceptance. A "no"
is a finding to resolve, not a detail to defer.

- [ ] **Purpose clear?** — The screen states what it is for without being decoded.
- [ ] **Tone appropriate?** — The register matches the situation (§3); nothing celebrates the routine or softens a
  consequence.
- [ ] **Message concise?** — Every word earns its place; no jargon the user does not already share (§4).
- [ ] **Action obvious?** — One primary action, unambiguous and predictably placed (§5).
- [ ] **Error recoverable?** — Every failure explains itself and offers a way forward (§7).
- [ ] **AI clearly labeled?** — AI output is identified, grounded, editable, and provisional (§8).
- [ ] **Accessible in context?** — Focus lands sensibly, every action is keyboard-reachable, and no meaning is carried
  by color alone on this screen (§10).
- [ ] **Aligned with design system?** — Uses the documented visual meanings and introduces none
  ([DESIGN_SYSTEM](./DESIGN_SYSTEM.md)).
- [ ] **Aligned with user flow?** — Realizes a documented journey and adds no step, state, or outcome
  ([USER_FLOWS](./USER_FLOWS.md)).
- [ ] **No contradictory UI behavior?** — The same gesture, state, and word mean here what they mean everywhere else.

---

## UI Review Process

> *Unnumbered governance section. It defines when a UI convention is reviewed and how these guidelines evolve —
> deliberately, not by accident.*

**Review triggers** — a UI review is required when any of the following occurs:

- **A new screen** is introduced.
- **A new message pattern** is proposed — a new way of saying something the product already says, or of saying something
  new.
- **A new interaction** is introduced.
- **A new AI presentation** is proposed — any change to how AI output is identified, grounded, or surfaced.
- **A new form behavior** is proposed.
- **A new navigation behavior** is proposed.
- **A significant UX change** is proposed.

**Review outcomes** — each review resolves to exactly one:

- **Approved** — the convention conforms to these guidelines and the frozen documents; it is accepted as-is.
- **Refinement required** — the intent is sound but the expression must change (tone, wording, an unexplained dead end,
  an inconsistent gesture) before acceptance.
- **Design system review required** — the need is visual rather than contextual; it is routed to the
  [Design Review Process](./DESIGN_SYSTEM.md#design-review-process) rather than worded around here.
- **Component update required** — the need is structural; it is routed to the
  [Component Review Process](./COMPONENTS.md) rather than improvised on one screen.
- **Flow update required** — the need implies a new step, state, or outcome; it is routed to the
  [User Flow Review Process](./USER_FLOWS.md) rather than introduced here.
- **Implementation change required** — the convention is right but the interface does not yet honor it; it is routed to
  the [Frontend Review Process](../03-engineering/FRONTEND_CODING_STANDARDS.md#frontend-review-process). This document
  does not prescribe the fix.
- **Architecture review required** — the change implies behavior, capability, or scope the frozen documents do not
  grant. A **requirement** gap is raised against [PRD](../00-product/PRD.md) / [SRS](../00-product/SRS.md) and an
  **architectural** one against [ARCHITECTURE](../01-architecture/ARCHITECTURE.md) and its
  [ADRs](../01-architecture/decisions/); either way it stops and is raised per [CLAUDE.md §8](../../CLAUDE.md) before
  proceeding.

**How these guidelines evolve:** with the product, and **never ahead of it**. A convention here may only express
behavior the [PRD](../00-product/PRD.md) and [SRS](../00-product/SRS.md) already grant; when a guideline and a frozen
document disagree, the frozen document wins and the guideline is corrected.

**Synchronization with the design layer:** these guidelines MUST remain synchronized with
[DESIGN_SYSTEM](./DESIGN_SYSTEM.md), [COMPONENTS](./COMPONENTS.md), and [USER_FLOWS](./USER_FLOWS.md). A change that
touches more than one of the four is reviewed by each document it touches and MUST NOT be merged into one while leaving
another contradicting it — the same coordinated review the design layer applies throughout
([DESIGN_SYSTEM §14](./DESIGN_SYSTEM.md#14-design-system-ownership)).

---

## 12. UI Guidelines Decision Summary

The load-bearing decisions behind these conventions, recorded so they are not silently reversed.

| Decision                             | Chosen Approach                                                                                                                 | Alternatives                                                                                                  | Rationale                                                                                                                                                                                                                                                                                                                                                                                   |
|--------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Professional tone**                | One calm, direct, professional voice addressing a competent expert                                                              | A friendly/casual product personality; a neutral system-log voice                                             | The interface sits beside Tally, QuickBooks, and Excel in skilled, accountable work; personality competes with the task, while a system voice ignores the human doing it.                                                                                                                                                                                                                   |
| **Domain vocabulary only**           | User-facing copy uses the product's domain vocabulary as the frozen documents define it, and never its architectural vocabulary | Surface system terms where they are precise; maintain a separate consumer glossary alongside the frozen terms | The user already shares the domain vocabulary, so it needs no translation and stays correct for free; system terms make the user pay to understand our internals, and a parallel glossary is a second source of truth that drifts. It also fixes where a new user-facing term enters: through [PRD](../00-product/PRD.md) / [SRS](../00-product/SRS.md), never invented at the screen (§4). |
| **Concise microcopy**                | Say what happens, in as few words as carry the meaning                                                                          | Fuller explanatory prose; terse system shorthand                                                              | Microcopy is read constantly, mid-task, usually once. Length is paid by every user every time; shorthand shifts the cost of understanding onto the reader.                                                                                                                                                                                                                                  |
| **Recovery-first messaging**         | Every failure states what happened and what to do next                                                                          | Report the failure and stop; hide failures to seem seamless                                                   | Document and AI operations fail routinely; a failure without a way forward is a dead end that loses trust and work ([SRS §8](../00-product/SRS.md#8-error-handling), [NFR-005](../00-product/SRS.md#9-non-functional-requirements)).                                                                                                                                                        |
| **AI speaks in its own voice**       | AI is always identified, grounded, editable, and provisional in its own words                                                   | Present AI output in the product's own authoritative voice                                                    | The professional signs the work; output speaking with the product's authority invites acceptance without review and defeats every upstream safeguard ([AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md), [BR-031](../00-product/SRS.md#5-business-rules)).                                                                                                                           |
| **Consistent confirmation dialogs**  | One confirmation convention, always stating the consequence plainly                                                             | Confirm case-by-case as each screen sees fit; rely on undo instead                                            | A confirmation the user cannot evaluate trains them to click through, which weakens every later confirmation — including the one that mattered.                                                                                                                                                                                                                                             |
| **Contextual help only when needed** | Help appears only where it removes a specific doubt                                                                             | Comprehensive helper text and tooltips throughout                                                             | Help attached to the obvious teaches users to ignore help, and the ignored instance is eventually the necessary one. Scarcity is what keeps help useful.                                                                                                                                                                                                                                    |
| **Contextual accessibility**         | Contextual accessibility is a condition of acceptance, not a later pass                                                         | Ship the interaction and audit accessibility afterwards                                                       | A product can be assembled from accessible components and still be unusable; contextual choices are where that happens, and only review at this level catches it ([NFR-011](../00-product/SRS.md#9-non-functional-requirements)).                                                                                                                                                           |

---

*This document defines how LedgerAI's visual language is applied in context; it does not override the frozen documents
under [`docs/`](../). It expresses the behavior granted by [PRD](../00-product/PRD.md) and [SRS](../00-product/SRS.md),
applies the visual language of [DESIGN_SYSTEM](./DESIGN_SYSTEM.md), uses the vocabulary of
[COMPONENTS](./COMPONENTS.md), serves the journeys of [USER_FLOWS](./USER_FLOWS.md), and is realized in code by
[FRONTEND_CODING_STANDARDS](../03-engineering/FRONTEND_CODING_STANDARDS.md). When a UI decision is required, review it
through the process above and, when a convention would imply new product behavior or contradict a frozen contract, stop
and raise it per [CLAUDE.md §8](../../CLAUDE.md).*
