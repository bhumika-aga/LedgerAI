# UI Components — LedgerAI MVP

> **Status:** Draft v1
> **Owner:** Design Systems Architect / Frontend Principal Engineer
> **Last updated:** 2026-07-14
> **Upstream (frozen):
>
** [PRD](../00-product/PRD.md) · [SRS](../00-product/SRS.md) · [ARCHITECTURE §6](../01-architecture/ARCHITECTURE.md#6-frontend-architecture) · [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) · [SECURITY](../01-architecture/SECURITY.md)
> **Related:
>
** [FRONTEND_CODING_STANDARDS](../03-engineering/FRONTEND_CODING_STANDARDS.md) · [DESIGN_SYSTEM](./DESIGN_SYSTEM.md) · [USER_FLOWS](./USER_FLOWS.md) · [CLAUDE.md](../../CLAUDE.md)

---

## 1. Purpose

### Why this document exists

This document defines the **reusable conceptual UI building blocks** from which every LedgerAI screen is assembled. Its
goal is a product that feels like **one coherent application** — where an accountant learns an interaction once (how a
table behaves, how a destructive action is confirmed, how an AI answer is presented) and that knowledge transfers to
every screen. Consistency, reuse, accessibility, and predictable behavior are the outcomes it protects.

It is **not** a Figma specification, **not** a React component library, and **not** a catalogue of props. It contains
**no code, no Material UI APIs, no CSS, and no visual styling**. It describes *what each component is for, what it must
and must not do, and how components relate to one another* — the shared vocabulary that a screen is composed from.

The governing principle: **pages are assembled from reusable components, not built as one-off custom UI.** A screen that
invents its own button, its own table, or its own way of confirming a delete is a defect against this document, even if
it looks correct in isolation.

### Relationship to the frozen and sibling documents

| Document                                                                       | Relationship                                                                                                                                                                                                                                                                                                                                                            |
|--------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [FRONTEND_CODING_STANDARDS.md](../03-engineering/FRONTEND_CODING_STANDARDS.md) | Defines **how components are written in code** — feature-first structure, accessibility rules, state handling, testing. This document defines **which components exist and what they mean**; that document makes them real. When they touch the same concern (e.g. accessibility), this document states the intent and defers to the standards for enforcement.         |
| [DESIGN_SYSTEM.md](./DESIGN_SYSTEM.md)                                         | Defines the **visual language** — color, typography, spacing, elevation, tokens. This document defines the **structural vocabulary**. A component here (e.g. *Button*) draws its appearance from the design system; the two are complementary and must never contradict. DESIGN_SYSTEM is authored after this document and consumes it.                                 |
| [USER_FLOWS.md](./USER_FLOWS.md)                                               | Defines the **sequences** a user moves through (upload → OCR → summary → email). Those flows are expressed **using the components defined here**. If a flow needs an interaction no component provides, that gap is resolved by adding or extending a component through the review process below — never by improvising in the flow.                                    |
| [UI_GUIDELINES.md](./UI_GUIDELINES.md)                                         | Governs the **contextual application** of these components — tone, interaction conventions, messaging, and contextual behavior. This document defines **what a component is and what it must do**; that document defines **how it is used in context and what it says when used**. A component's contract is this document's; the words it carries are that document's. |
| [PRD.md](../00-product/PRD.md) · [SRS.md](../00-product/SRS.md)                | Define **what the product does**. Every component here exists to serve documented product behavior (the twelve MVP modules); no component introduces capability the product does not have.                                                                                                                                                                              |

---

## 2. Component Philosophy

These principles explain *why* LedgerAI standardizes its UI vocabulary. They are the reasoning behind the enforceable
rules that follow.

| Principle                           | Why it exists                                                                                                                                                                                                                                                           |
|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Reuse before creation**           | Every new one-off component is a new thing to learn, test, and keep accessible. Reaching for an existing component first keeps the surface small and the experience uniform. A new component is justified only when no existing one fits.                               |
| **Composition over configuration**  | A component overloaded with flags to cover every case becomes unpredictable and hard to reason about. Assembling a screen from small, focused pieces is clearer than configuring one large piece into many shapes. This mirrors the backend's composition-first stance. |
| **Accessibility by default**        | Accountants work long hours in these screens; some rely on keyboards or assistive technology. Accessibility built into the base component means every screen inherits it — it cannot be forgotten per-screen or bolted on later.                                        |
| **Consistency over creativity**     | A predictable product is a trustworthy product. When the same action always looks and behaves the same way, the user builds confidence and works faster. Local visual creativity that breaks that pattern costs more than it adds.                                      |
| **Small, focused components**       | A component with one clear responsibility is easy to name, test, reuse, and evolve. If a component is hard to name, it is doing too much — the same signal used throughout LedgerAI's engineering.                                                                      |
| **Predictable interactions**        | The same gesture should always produce the same kind of result. Predictability is what lets a user act without hesitation, which is the entire "hours to minutes" promise.                                                                                              |
| **Generic before feature-specific** | A generic component serves many screens; a feature component serves one. Preferring the generic form keeps the shared library broadly useful and prevents feature concerns from fragmenting the vocabulary.                                                             |

---

## Component Design Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Components MUST have a single responsibility.** *A component that does one thing is understandable, testable, and
  safely reusable; a multi-purpose component becomes a source of surprises and duplicated fixes.*
- **Shared components MUST remain feature-agnostic.** *A shared component that knows about "clients" or "invoices" can
  no longer be reused elsewhere and drags feature logic into the common layer, which is how a design system rots.*
- **Components MUST be accessible.** *Accessibility is not an enhancement; a component that cannot be operated by
  keyboard or announced to assistive technology is incomplete,
  per [FRONTEND_CODING_STANDARDS](../03-engineering/FRONTEND_CODING_STANDARDS.md).*
- **Components MUST support loading, disabled, and error states where those states are meaningful.** *Real screens wait
  on the network and can fail; a component that only models the happy path forces every screen to reinvent the unhappy
  ones inconsistently. "The happy path works" is not complete ([CLAUDE.md §9](../../CLAUDE.md)).*
- **Components MUST expose consistent interaction patterns.** *The same interaction meaning the same thing everywhere is
  what makes the product learnable; divergent behavior for the same gesture erodes trust.*
- **Components SHOULD compose smaller components.** *Building larger components from established smaller ones inherits
  their correctness and accessibility instead of re-deriving it.*
- **Components SHOULD avoid unnecessary configuration.** *Every option is a decision pushed onto the caller and a branch
  to test; fewer, clearer components beat one endlessly-configurable one.*
- **Feature components MUST NOT leak into shared components.** *The dependency direction is one-way: features depend on
  shared components, never the reverse. A leak inverts that and couples the whole library to one feature.*
- **Visual consistency MUST take precedence over local customization.** *A screen does not get to look "a little
  different" for convenience; the cumulative effect of many small deviations is an incoherent product.*

---

## 3. Component Categories

LedgerAI's vocabulary is organized into nine categories. The first six are **generic** — reusable in any product. The
last three (**AI**, **Document**, **Dialogs**) are **LedgerAI-shaped**: still reusable, but composed specifically for
the product's core loop of *upload → understand → act*. Component names below are conceptual, not code identifiers.

### Layout

Structural containers that establish the frame every screen lives in.

- **App Shell** — the outermost frame: persistent chrome (header, sidebar) wrapping the routed content region.
- **Header** — the top bar: product identity, global search entry, user/account menu.
- **Sidebar** — primary navigation between the major areas (Clients, Documents, Activity, etc.).
- **Page Container** — the standardized content region: consistent width, padding, and page title placement.
- **Section** — a titled grouping of related content within a page.
- **Grid** — responsive multi-column arrangement of items (e.g. a set of cards).
- **Stack** — consistent vertical or horizontal spacing between a run of elements.

### Navigation

Components that move the user between locations or steps.

- **Breadcrumb** — shows the user's position in the hierarchy and offers a path back up.
- **Tabs** — switches between peer views of the same subject without leaving the page.
- **Navigation Menu** — a grouped list of destinations (e.g. within the sidebar).
- **Pagination** — moves through a large result set in pages.
- **Stepper** — communicates progress through an ordered, multi-step task.

### Inputs

Components that collect data from the user. All obey the same validation and error conventions.

- **Text Field** — single-line text entry.
- **Text Area** — multi-line text entry (e.g. editing an AI draft).
- **Select** — choose one option from a defined set.
- **Date Picker** — choose a date.
- **Checkbox** — toggle an independent boolean.
- **Radio** — choose exactly one from a small mutually-exclusive set.
- **Switch** — toggle an immediate on/off setting.
- **Search Box** — a text input specialized for initiating a search.

### Actions

Components that let the user *do* something. They share one interaction and one disabled/loading model.

- **Button** — the primary triggerable action, with a clear hierarchy of emphasis (primary / secondary / subtle).
- **Icon Button** — a compact action represented by an icon, always with an accessible label.
- **Split Button** — a default action paired with a menu of related alternatives.
- **Floating Action Button** — a single, prominent primary action for a screen (used sparingly).

### Feedback

Components that tell the user what is happening or what happened.

- **Alert** — an inline, persistent message about the state of the current context.
- **Toast** — a brief, transient confirmation of an action's outcome.
- **Snackbar** — a transient message that may carry a single lightweight action (e.g. undo).
- **Banner** — a prominent, page-level notice (e.g. a system or account-wide condition).
- **Progress** — communicates determinate advancement of a known-length operation.
- **Spinner** — communicates indeterminate waiting.
- **Skeleton** — a placeholder that mirrors incoming content's shape while it loads.

### Data Display

Components that present information the user reads and acts on.

- **Card** — a self-contained summary of one entity, often the unit of a Grid.
- **Table** — structured rows and columns with consistent sorting, selection, and pagination behavior.
- **Badge** — a small status or count marker attached to another element.
- **Chip** — a compact, discrete piece of information, optionally removable (e.g. a tag or filter).
- **Avatar** — a visual representation of a user or entity.
- **List** — a vertical sequence of comparable items.
- **Timeline** — a chronological sequence of events, the display form for the Activity module.

### AI Components

The vocabulary for presenting model output. These components exist to make AI **grounded, transparent, and
human-in-the-loop** as mandated by [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) — never to present AI output
as unquestionable fact.

- **AI Summary Card** — presents a generated document summary as a reviewable, editable artifact, not a finished truth.
- **AI Chat Panel** — the conversational surface for document-scoped Q&A.
- **AI Response** — a single model answer, visibly distinguished from user content and carrying its provenance.
- **Citation Block** — surfaces the source grounding an AI answer, making claims traceable back to the document.
- **Confidence Indicator** — communicates the qualitative reliability of an AI output so the user calibrates trust.
- **Regenerate Button** — lets the user request a fresh attempt, reinforcing that AI output is provisional and editable.

### Document Components

The vocabulary for the document lifecycle — upload, storage, OCR, and viewing — the entry point of the core loop.

- **Upload Zone** — the drop/select target for adding a document, communicating accepted types and limits up front.
- **File Card** — a document's summary tile: name, type, and current processing state.
- **PDF Viewer** — displays an uploaded document for reading and reference alongside AI output.
- **OCR Status** — communicates a document's extraction state (e.g. processing, ready, failed) drawn from the document
  state model in [SRS](../00-product/SRS.md).
- **Metadata Panel** — displays a document's structured attributes.

### Dialogs

Modal surfaces that interrupt for a focused decision or task. They share one dismissal model and one focus-management
behavior.

- **Confirmation Dialog** — asks the user to confirm a consequential action before it proceeds.
- **Delete Dialog** — a specialized confirmation for destructive removal, stating consequences plainly.
- **Edit Dialog** — a focused surface for editing one entity's fields.
- **Full Screen Dialog** — an immersive surface for a substantial task (e.g. reviewing a document with its AI output).

---

## 4. Component Specifications

Specifications are given **per category** — the shared contract every component in that category obeys. Individual
components inherit their category's contract; where a single component adds obligations, they are noted. Descriptions
are behavioral and implementation-independent by design.

### Layout

- **Purpose:** Establish a consistent structural frame so every screen shares the same spatial rhythm and navigation
  chrome.
- **Responsibilities:** Position content; provide consistent spacing and width; host navigation regions; adapt
  responsively across viewport sizes.
- **Typical Usage:** App Shell wraps all authenticated screens; Page Container hosts each routed page; Grid and Stack
  arrange the content within.
- **Must:** Preserve consistent spacing and width; adapt to viewport size; expose semantic landmark regions (banner,
  navigation, main).
- **Must Not:** Contain business logic; know about any specific feature; hard-code content.
- **Accessibility:** Use landmark semantics so assistive technology can navigate by region; maintain a logical reading
  and focus order top-to-bottom.

### Navigation

- **Purpose:** Let the user understand where they are and move deliberately between locations and steps.
- **Responsibilities:** Represent current location; offer reachable destinations; communicate progress through
  multi-step tasks.
- **Typical Usage:** Sidebar Navigation Menu for top-level areas; Breadcrumb within a hierarchy; Tabs across peer views;
  Stepper across an ordered task; Pagination across long result sets.
- **Must:** Indicate the current location or step; keep destinations operable by keyboard; preserve order and grouping.
- **Must Not:** Hide the user's current position; reorder unpredictably; act as the only cue for state (never rely on
  color alone).
- **Accessibility:** Expose current-page/current-step state to assistive technology; support keyboard traversal and
  activation.

### Inputs

- **Purpose:** Collect user data reliably and communicate its validity consistently.
- **Responsibilities:** Accept input; reflect value; surface validation state; pair with a clear label and, when
  invalid, an actionable message.
- **Typical Usage:** Forms across Authentication, Profile, Client Management; Text Area for editing AI drafts; Search
  Box for Global Search.
- **Must:** Have an associated, programmatically-linked label; show error state with a message when validation fails
  ([SRS §6](../00-product/SRS.md#6-validation-rules)); support disabled state; validate at the boundary before
  submission.
- **Must Not:** Convey validity by color alone; silently discard input; trust client-side validation as the only check —
  the server remains authoritative.
- **Accessibility:** Associate label and error message with the control; announce validation errors; remain fully
  keyboard-operable.

### Actions

- **Purpose:** Let the user trigger operations with a predictable, uniform interaction.
- **Responsibilities:** Communicate action emphasis; reflect disabled and in-progress states; prevent duplicate
  triggering while an action is underway.
- **Typical Usage:** Button for form submission and primary actions; Icon Button in dense toolbars; Split Button where a
  default action has variants.
- **Must:** Convey a clear emphasis hierarchy; show a loading state for asynchronous actions and block re-triggering
  during it; carry an accessible name even when icon-only.
- **Must Not:** Depend on icon shape alone to convey meaning; leave the user uncertain whether an action is in progress;
  perform destructive actions without a confirmation path.
- **Accessibility:** Be reachable and activatable by keyboard; expose disabled and busy state to assistive technology.

### Feedback

- **Purpose:** Keep the user continuously informed of system state and action outcomes.
- **Responsibilities:** Communicate progress, success, warning, and error conditions at the appropriate scope (inline,
  transient, or page-level).
- **Typical Usage:** Skeleton and Spinner while loading; Toast/Snackbar to confirm an action; Alert/Banner for
  persistent conditions; Progress for known-length operations such as upload.
- **Must:** Match message scope to severity (transient for confirmations, persistent for conditions needing action);
  pair any status with text, not color alone; keep transient feedback available long enough to read.
- **Must Not:** Rely on color alone; steal focus for non-critical messages; expose internal or sensitive system detail
  in error text ([SECURITY](../01-architecture/SECURITY.md)).
- **Accessibility:** Announce important status changes via live regions; ensure error and success are distinguishable
  without color.

### Data Display

- **Purpose:** Present information clearly so the user can read, compare, and act.
- **Responsibilities:** Render entities and collections consistently; handle empty, loading, and error content states;
  offer consistent sorting/selection where applicable.
- **Typical Usage:** Card in Grids of clients or documents; Table for structured lists; Timeline for the Activity
  module; Badge/Chip for status and tags.
- **Must:** Provide an explicit empty state; show a loading state while data is pending; keep interactive display
  elements (sortable headers, row actions) keyboard-operable.
- **Must Not:** Present another user's data — display is always ownership-scoped upstream
  ([SECURITY §5](../01-architecture/SECURITY.md#5-authorization)); render a blank screen where an empty state belongs.
- **Accessibility:** Use correct semantics (a Table is a table, a List is a list); associate headers with cells; keep
  row actions reachable by keyboard.

### AI Components

- **Purpose:** Present model output as **grounded, transparent, and provisional**, keeping the human in control.
- **Responsibilities:** Distinguish AI content from user content; surface grounding and confidence; make output editable
  and regenerable; communicate the AI request lifecycle (in progress, complete, failed).
- **Typical Usage:** AI Summary Card in the document view; AI Chat Panel for Q&A; Citation Block beneath AI answers.
- **Must:** Visibly mark content as AI-generated; allow the user to edit before use and to regenerate; represent
  in-progress and failed AI states; present citations where the output is grounded in a document
  ([AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md)).
- **Must Not:** Present AI output as authoritative or final; auto-perform a consequential action from AI output (e.g. an
  AI Email is drafted, **never sent** — [BR-034]); display or log prompt/response content in ways
  [SECURITY](../01-architecture/SECURITY.md) forbids.
- **Accessibility:** Announce when a response has arrived; keep the response region navigable; ensure confidence and
  citations are conveyed as text, not by visual treatment alone.

### Document Components

- **Purpose:** Support the document lifecycle — add, store, extract, view — the entry point of the core loop.
- **Responsibilities:** Communicate accepted input and limits; reflect a document's processing and OCR state; present
  the document for reading alongside its derived output.
- **Typical Usage:** Upload Zone to add documents; File Card in the document list; OCR Status on each document; PDF
  Viewer in the document view.
- **Must:** State accepted types and size limits before upload; reflect the document/OCR state accurately from the
  server-owned state model ([SRS](../00-product/SRS.md)); surface upload and processing errors clearly.
- **Must Not:** Imply an upload or extraction succeeded before the server confirms it; expose storage internals or
  provider detail; assume a fixed provider ([ADR-002](../01-architecture/decisions/ADR-002-Storage-Provider.md) is
  deferred).
- **Accessibility:** Make the Upload Zone operable by keyboard and not drag-only; announce state transitions
  (processing → ready/failed); label document controls.

### Dialogs

- **Purpose:** Focus the user on a single decision or task by temporarily interrupting the flow.
- **Responsibilities:** Trap and manage focus; present a clear title, body, and unambiguous actions; state consequences
  for destructive choices.
- **Typical Usage:** Confirmation/Delete Dialog before consequential actions; Edit Dialog for a single entity; Full
  Screen Dialog for document review.
- **Must:** Move focus into the dialog on open and restore it on close; be dismissable by keyboard; clearly separate the
  confirming action from the cancelling one; state what a destructive action will do.
- **Must Not:** Trap the user with no dismissal path; make the destructive action the effortless default; obscure the
  consequence of confirming.
- **Accessibility:** Announce the dialog and its title; constrain focus to the dialog while open; support dismissal via
  keyboard.

---

## 5. Component Hierarchy

Components form a strict layering. Dependencies point **upward only** — a lower layer never depends on a higher one.
This is the same dependency-direction discipline the backend enforces, applied to the UI.

- **Atomic components** — the indivisible primitives (Button, Text Field, Badge, Spinner). They depend on nothing but
  the design system's tokens.
- **Composite components** — assemblies of atomics into a richer, still-generic unit (Card, Table, Dialog, Search Box).
  They compose atomics and remain feature-agnostic.
- **Feature components** — components that bind generic pieces to a specific LedgerAI capability (AI Summary Card,
  Upload Zone, Client Card). They may depend on composites and atomics; they must not be depended on by them.
- **Pages** — full screens that arrange feature and generic components into a complete view, expressed by
  [USER_FLOWS](./USER_FLOWS.md). Pages are the only layer that knows about routing and page-level data.

```mermaid
flowchart TD
    A["Atomic components<br/>Button · Text Field · Badge · Spinner"]
    C["Composite components<br/>Card · Table · Dialog · Search Box"]
    F["Feature components<br/>AI Summary Card · Upload Zone · Client Card"]
    P["Pages<br/>full screens / routes"]
    A --> C
    C --> F
    F --> P
    A --> P
    C --> P
    classDef shared fill: #e6f0ff, stroke: #3366cc, color: #11294d;
    classDef feature fill: #fff2e6, stroke: #cc7a33, color: #4d2f11;
class A, C shared;
class F,P feature;
```

> **Reading the diagram:** the shared layer (atomic + composite) is broadly reusable and feature-agnostic. The feature
> and page layers are LedgerAI-specific. Arrows are *depends-on*; there is no arrow from a higher layer back down into a
> lower one, and none from feature into shared.

---

## 6. Component Lifecycle

A component moves through a defined lifecycle. The purpose of naming it is to make the **promotion decision explicit** —
when something local becomes shared — rather than letting the shared library grow by accident.

- **Design** — the need is identified and checked against this document: does an existing component already serve it? A
  new component is proposed only when none fits.
- **Implementation** — the component is built
  to [FRONTEND_CODING_STANDARDS](../03-engineering/FRONTEND_CODING_STANDARDS.md), with its states and accessibility
  included from the start.
- **Review** — it is evaluated against the Review Checklist (§9) and the Component Review Process below.
- **Reuse** — once proven, it is used wherever it applies instead of new one-off UI.
- **Evolution** — it changes over time through review, never by silent divergence in one screen.

**When a component should become shared:** a component should be **promoted from feature-local to shared** when it is
needed by a second feature *and* it can be expressed feature-agnostically. Two conditions must both hold — a single use
does not justify promotion (premature generalization couples the library to one case), and a component that cannot shed
its feature knowledge must stay local (forcing it into the shared layer would leak feature concerns, violating the
Component Design Rules). Until both hold, it stays feature-local.

---

## 7. Component States

Every interactive component draws from one standardized set of states, so a state *means* the same thing everywhere. A
component implements the states that are meaningful for it; where a state applies, it must behave as described.

| State        | Expectation                                                                                                                               |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| **Default**  | The resting state: fully operable, no interaction in progress.                                                                            |
| **Hover**    | Signals that an element is interactive as the pointer is over it. Never the *only* signal of interactivity (keyboard users get no hover). |
| **Focus**    | Always visibly indicated when the element holds keyboard focus. Focus visibility is mandatory, not optional.                              |
| **Active**   | The momentary pressed/engaged state during interaction, confirming the input was received.                                                |
| **Disabled** | Communicates that the element is present but not currently operable, and why is discoverable from context. Not reachable by tab.          |
| **Loading**  | An asynchronous operation is underway; the component reflects it and prevents duplicate triggering until it resolves.                     |
| **Error**    | A validation or operation failure is present, shown with an actionable message and never by color alone.                                  |
| **Empty**    | A collection or content area has no data; an explicit, informative empty state is shown instead of a blank region.                        |

> **Why standardize:** if every component invented its own loading or error treatment, the user would relearn "is this
> working?" on every screen. One shared set of states is what makes the whole product feel predictable.

---

## 8. Accessibility

Accessibility is a **default of the vocabulary**, not a per-screen task. Because screens are composed from these
components, an accessible component makes every screen that uses it accessible by inheritance. This section states the
intent; [FRONTEND_CODING_STANDARDS](../03-engineering/FRONTEND_CODING_STANDARDS.md) is the authority that enforces it in
code.

- **Keyboard navigation** — every interactive component is fully operable without a pointer, in a logical tab order.
- **Focus visibility** — the focused element is always clearly indicated; focus is never suppressed.
- **ARIA where appropriate** — used to convey roles, states, and relationships **only where native semantics are
  insufficient** — never as a substitute for correct semantics.
- **Semantic HTML** — components use the element that matches their meaning (a button is a button, a list is a list), so
  assistive technology understands them without extra work.
- **Touch targets** — interactive targets are large enough to operate comfortably on touch devices.
- **Announcements** — meaningful state changes (an AI response arriving, a validation error, an upload completing) are
  announced to assistive technology via live regions rather than shown visually only.

> Cross-reference: [FRONTEND_CODING_STANDARDS](../03-engineering/FRONTEND_CODING_STANDARDS.md) defines the concrete
> accessibility obligations and how they are verified; this document ensures accessibility is designed into the
> component
> from the first, not retrofitted.

---

## 9. Review Checklist

Every component — new or modified — is evaluated against this checklist before it is accepted. A "no" is a finding to
resolve, not a detail to defer.

- [ ] **Reusable?** — Could this serve more than the one screen it was built for, where that is the intent?
- [ ] **Accessible?** — Keyboard-operable, focus-visible, correctly announced, semantic.
- [ ] **Generic?** — Free of feature knowledge if it is (or is becoming) a shared component.
- [ ] **Consistent?** — Interaction, states, and behavior match the existing vocabulary.
- [ ] **Tested?** — Behavior, states, and accessibility are covered
  per [TESTING_STRATEGY](../03-engineering/TESTING_STRATEGY.md).
- [ ] **Responsive?** — Adapts correctly across viewport sizes.
- [ ] **Matches design system?** — Draws appearance from [DESIGN_SYSTEM](./DESIGN_SYSTEM.md) tokens, not local styling.
- [ ] **No duplicated functionality?** — Does not reinvent something an existing component already provides.

---

## Component Review Process

> *Unnumbered governance section. It defines when a component is reviewed and how the shared library evolves —
deliberately,
> not by accident.*

**Review triggers** — a review is required when any of the following occurs:

- **A new shared component** is proposed for the common library.
- **An existing component is modified** in a way that changes its behavior, states, or contract.
- **A new feature component** is introduced within a feature.
- **A design system change** alters tokens or visual language that components depend on.

**Review outcomes** — each review resolves to exactly one:

- **Approved** — the component meets the checklist and rules; it enters or remains in the library as-is.
- **Refactor required** — the intent is sound but the component must change (accessibility gap, hidden feature coupling,
  duplicated behavior) before acceptance.
- **Keep feature-local** — the component is legitimate but not yet generic enough or broadly needed enough to be shared;
  it stays within its feature.
- **Promote to shared** — a feature-local component has met both promotion conditions (§6) and is generalized into the
  shared library.

**How the shared library evolves incrementally:** the library grows **one reviewed promotion at a time**, never by
speculative up-front generalization. Components are born feature-local, earn reuse, and are promoted only when a second
real need appears and feature knowledge can be shed. This keeps the shared vocabulary small, genuinely reusable, and
free of premature abstraction — the same vertical, incremental discipline
the [IMPLEMENTATION_PLAN](../03-engineering/IMPLEMENTATION_PLAN.md)
applies to features.

---

## 10. Component Decision Summary

The load-bearing decisions behind this vocabulary, recorded so they are not silently reversed.

| Decision                            | Chosen Approach                                                             | Alternatives                                                    | Rationale                                                                                                                               |
|-------------------------------------|-----------------------------------------------------------------------------|-----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| **Composition over configuration**  | Assemble screens from small focused components                              | One large, heavily-configurable component per concern           | Composition stays predictable and testable; a mega-configurable component becomes an untestable source of surprises.                    |
| **Shared library**                  | A common, feature-agnostic component library all screens draw from          | Each feature owns its own UI components independently           | A shared library is what produces a coherent, learnable product; per-feature UI fragments the experience and duplicates effort.         |
| **Feature isolation**               | Feature concerns live in feature components; shared components stay generic | Allow feature logic into shared components for convenience      | One-way dependency keeps the shared layer reusable; leaking feature knowledge downward is how a design system decays.                   |
| **Accessibility-first**             | Accessibility built into every base component                               | Add accessibility per-screen or in a later pass                 | Built-in accessibility is inherited by every screen and cannot be forgotten; retrofitting is costly and inconsistent.                   |
| **Generic before feature-specific** | Prefer the generic component; specialize only when necessary                | Build purpose-specific components first                         | Generic components serve many screens and keep the library broadly useful; premature specialization narrows reuse.                      |
| **Standardized states**             | One shared set of component states (§7) with consistent meaning             | Let each component define its own loading/error/empty treatment | A user learns a state once and recognizes it everywhere; divergent state treatments force constant relearning and erode predictability. |
| **Reuse before creation**           | Reach for an existing component before building a new one                   | Freely create new components per need                           | Every new component is more to learn, test, and keep accessible; reuse keeps the surface small and the experience uniform.              |

---

*This document defines LedgerAI's UI vocabulary; it does not override the frozen documents under
[`docs/`](../). It serves the product behavior defined in [PRD](../00-product/PRD.md) and [SRS](../00-product/SRS.md),
draws its appearance from [DESIGN_SYSTEM](./DESIGN_SYSTEM.md), is realized in code by
[FRONTEND_CODING_STANDARDS](../03-engineering/FRONTEND_CODING_STANDARDS.md), and is sequenced into screens by
[USER_FLOWS](./USER_FLOWS.md). When a component decision is required, review it through the process above and, when in
doubt, consult the source of truth.*
