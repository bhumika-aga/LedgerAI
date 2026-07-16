# GitHub Copilot Instructions — LedgerAI

Guidance for GitHub Copilot when generating code in this repository. **This is not documentation, and not an
architecture specification.** It teaches Copilot how to produce code that fits LedgerAI's existing architecture,
standards, and constraints — and, more often, how to route to the document that already decides a question rather than
deciding it in code.

If anything here appears to conflict with a document under [`docs/`](../docs/) or [`CLAUDE.md`](../CLAUDE.md), **those
win.** This file points at the source of truth; it never replaces it.

---

## 1. Purpose

LedgerAI is a **documentation-first** repository: what the product is, how it is built, and how it behaves are decided
in 42 authored documents *before* any code is written. Code implements those decisions — it does not make them.

Copilot's job here is to **generate code consistent with what the documents already decided**, and to stop when a
suggestion would require a decision no document has made. A correct-looking suggestion that contradicts the architecture
is a defect, not a shortcut.

Copilot is **generating code, not making architecture decisions.** When a task seems to need a new requirement, a new
endpoint, a schema change, or a new architectural boundary, that is a signal to stop and defer to a human — see
[§10](#10-before-suggesting-code) and [CLAUDE.md §8](../CLAUDE.md).

---

## 2. Repository Structure

```
docs/
  00-product/        Why the product exists; what it does (Vision, Product Decisions, PRD, SRS)
  01-architecture/   System design & domain specs (Architecture, Database, API Spec, Security, AI Architecture, ADRs)
  02-design/         Visual language & behavior (Design System, Components, UI Guidelines, User Flows)
  03-engineering/    How code is built & shipped (Coding Standards, Testing, Implementation Plan/Status,
                     Contributing, Deployment, Lessons Learned)
  04-ai/             AI governance (AI Providers, Prompts, Evaluation, RAG)
  05-releases/       Changelog, Release Notes
CLAUDE.md            The engineering playbook — read first
```

Source code lives outside `docs/` and implements what these documents decide. Every meaningful concern already has an
owning document; find it before writing code (see [§5](#5-module-boundaries) and
[CONTRIBUTING §3](../docs/03-engineering/CONTRIBUTING.md)).

---

## 3. Documentation Hierarchy

Authority flows top-to-bottom; a higher document constrains every lower one. This is defined in full in
[CLAUDE.md §3](../CLAUDE.md) — do not restate or reinterpret it. In short:

```
Product Vision → Product Decisions → PRD → SRS → Architecture
→ Database · API Spec · Security · AI Architecture → ADRs → Implementation Plan → Implementation Status
```

Rules Copilot must follow:

- **When code and a document disagree, the document is right.** Fix the code, not the document — unless a human decides
  the document is wrong.
- **A specific ADR governs its narrow decision** even though ADRs sit low in the list.
- **Only [`IMPLEMENTATION_STATUS.md`](../docs/03-engineering/IMPLEMENTATION_STATUS.md) changes routinely.** Everything
  above it is frozen; do not generate changes that contradict a frozen document.

---

## 4. Architectural Principles

The architecture is defined in [ARCHITECTURE.md](../docs/01-architecture/ARCHITECTURE.md); the enforceable rules are in
[CLAUDE.md §4](../CLAUDE.md). Do not restate them. What Copilot must honor while generating code:

- **Layering:** thin controllers → logic in services → persistence in repositories. Business rules live in services,
  traceable to the [SRS](../docs/00-product/SRS.md) — never buried in controllers or UI.
- **Ports and adapters:** external providers (AI, OCR, Storage) are reached **only through their port**, never by
  calling a provider SDK from business logic.
- **Module independence:** cross-module interaction goes through a module's published service, never its internals.
- **Ownership is not optional:** per-user isolation is the core confidentiality control. Never generate a data-access
  path that bypasses ownership validation, and never expose another user's data — non-owned resources return `404`.
- **Keep it simple:** the simplest correct solution that fits the existing pattern wins over a cleverer one.

---

## 5. Module Boundaries

Modules are owned; a change belongs to a module's owning document before it belongs to code. The authoritative module
list is [ARCHITECTURE §8](../docs/01-architecture/ARCHITECTURE.md); routing for any change is
[CONTRIBUTING §7](../docs/03-engineering/CONTRIBUTING.md). Practical rules for Copilot:

- **Respect the boundary.** Do not reach across modules into internal classes; call the published service.
- **Do not move a boundary.** Relocating responsibility between modules is an architecture change, not a refactor — stop
  and defer ([CLAUDE.md §8](../CLAUDE.md)).
- **AI orchestration is the AI module's.** Summary, chat, email, and report generation are owned by the AI module behind
  its port ([AI_ARCHITECTURE.md](../docs/01-architecture/AI_ARCHITECTURE.md)). Do not scatter AI calls across features.
- **When unsure which module owns a change, stop and route it** — do not place it wherever the code happens to be open.

---

## 6. Coding Expectations

Full standards live in [BACKEND_CODING_STANDARDS.md](../docs/03-engineering/BACKEND_CODING_STANDARDS.md) and
[FRONTEND_CODING_STANDARDS.md](../docs/03-engineering/FRONTEND_CODING_STANDARDS.md), and the high-level expectations in
[CLAUDE.md §6](../CLAUDE.md). Copilot should **match the surrounding code** rather than introduce new idioms. In
particular:

- **Consistency over cleverness.** Reuse the established module structure, ports/adapters, error taxonomy, and DTO
  conventions. Prefer the existing pattern to a novel one, even a better novel one.
- **Small, single-responsibility** classes and functions; meaningful names; no abbreviations that are not universal.
- **Constructor injection;** dependencies explicit and testable. No hidden wiring.
- **Immutable DTOs cross boundaries;** persistence entities never leak past the API boundary.
- **Validate external input at the boundary;** fail fast with clear errors; never trust the client.
- **Tests come with the feature** — logic, validation, security/ownership, and key edge cases
  ([TESTING_STRATEGY.md](../docs/03-engineering/TESTING_STRATEGY.md)).
- **Match the file's comment density and style.** A comment should state a constraint the code cannot show — not narrate
  the change or explain it to a reviewer.

---

## 7. AI Development Guidelines

AI behavior is governed, not improvised. The owning documents are
[AI_ARCHITECTURE.md](../docs/01-architecture/AI_ARCHITECTURE.md), [AI_PROVIDERS.md](../docs/04-ai/AI_PROVIDERS.md),
[PROMPTS.md](../docs/04-ai/PROMPTS.md), [EVALUATION.md](../docs/04-ai/EVALUATION.md), and
[RAG.md](../docs/04-ai/RAG.md); the enforceable rules are [CLAUDE.md §7](../CLAUDE.md). Copilot must:

- **Reach AI/OCR/Storage providers only through their port** — never call a provider SDK from business logic.
- **Never hard-code a provider or model name in business logic;** the concrete provider is deferred and lives behind
  configuration and the port.
- **Keep prompt composition centralized;** do not scatter prompt strings across features, and do not author prompt text
  here — that is [PROMPTS.md](../docs/04-ai/PROMPTS.md)'s.
- **Preserve grounding, human-in-the-loop, and channel separation.** AI output is grounded in the user's document, is
  editable, is never treated as a system of record, and untrusted document/user content is kept in its own channel to
  resist prompt injection ([SECURITY §10](../docs/01-architecture/SECURITY.md)).
- **Never send more context than the task requires,** and never cross per-user isolation to assemble it.
- **AI quality is measured, not asserted.** A behavior change is evaluated
  per [EVALUATION.md](../docs/04-ai/EVALUATION.md);
  Copilot does not decide that AI output is "good enough."

---

## 8. Documentation Expectations

Documentation-first means the document changes *before or with* the code, never after.

- **If a change alters behavior, update the owning document in the same change** — API contract, schema, ADR, or status
  as applicable ([CONTRIBUTING §6](../docs/03-engineering/CONTRIBUTING.md),
  [IMPLEMENTATION_PLAN §9](../docs/03-engineering/IMPLEMENTATION_PLAN.md)).
- **Code and its governing document never diverge.** If they would, the code is wrong — or the document must be changed
  first, by a human.
- **Do not create a second source of truth.** Do not restate a rule, a decision, or a contract in code comments or a new
  doc; reference the document that owns it.
- **Update [`IMPLEMENTATION_STATUS.md`](../docs/03-engineering/IMPLEMENTATION_STATUS.md)** as work lands — it is the one
  document expected to change routinely.

---

## 9. Common Mistakes to Avoid

These are the antipatterns that cause architectural drift ([CLAUDE.md §10](../CLAUDE.md)):

- **Inventing anything the documents did not grant** — a requirement, an endpoint, a status, an error shape, a table, a
  column, a config key, a module, or a product feature. If it is not documented, it is not a requirement.
- **Bypassing ownership validation** or returning another user's data with anything other than `404`.
- **Calling a provider SDK directly** instead of through its port.
- **Putting business rules in controllers or UI** instead of services traceable to the SRS.
- **Leaking persistence entities** across the API boundary instead of DTOs.
- **Magic strings** where a named constant or enum exists (statuses, roles, types).
- **Giant, multi-concern changes.** Keep changes small, vertical, and reviewable.
- **Duplicating logic or authority** instead of reusing and referencing.
- **Widening scope silently** — "while I was here" changes that no one requested.

---

## 10. Before Suggesting Code

Before generating a suggestion, Copilot should confirm:

1. **Is this behavior documented?** If it is not in
   the [SRS](../docs/00-product/SRS.md)/[PRD](../docs/00-product/PRD.md),
   do not invent it — surface that it is undocumented.
2. **Which module and document own it?** Place the change where its owner is, not where the cursor is
   ([§5](#5-module-boundaries)).
3. **Does it fit the existing pattern?** Match the established structure, ports, DTOs, and error handling
   ([§6](#6-coding-expectations)).
4. **Does it cross a frozen contract?** Architecture, API, database, security, or AI-architecture changes are
   **stop-and-ask**, not autocomplete ([CLAUDE.md §8](../CLAUDE.md)).
5. **Does it preserve ownership and security?** No path bypasses per-user isolation
   ([SECURITY.md](../docs/01-architecture/SECURITY.md)).
6. **Do tests and docs come with it?** A feature is not complete without both ([§11](#11-review-checklist)).

If the honest answer to any of these is "this needs a decision," **stop and defer to a human** rather than generating
code that assumes one.

---

## 11. Review Checklist

A suggestion is fit to propose only when all hold ([CLAUDE.md §9](../CLAUDE.md),
[CONTRIBUTING §9](../docs/03-engineering/CONTRIBUTING.md)):

- [ ] **Documented** — implements a requirement the SRS/PRD grants; invents nothing.
- [ ] **In its module** — placed with its owning module, through published services only.
- [ ] **Pattern-consistent** — matches existing structure, naming, ports/adapters, DTO and error conventions.
- [ ] **Ownership-safe** — no per-user isolation bypass; non-owned resources return `404`.
- [ ] **Layered** — thin controller, logic in service, persistence in repository; no leaked entities.
- [ ] **AI-safe** — providers via port; grounding, channel separation, and minimal context preserved.
- [ ] **Tested** — logic, validation, security/ownership, and edge cases covered.
- [ ] **Documented change** — owning document updated when behavior changed; status updated as work lands.
- [ ] **No frozen-contract violation** — architecture, API, database, security, and ADRs respected.

---

## 12. Final Guidance

- **Documentation is the source of truth.** When in doubt, read the owning document; do not guess and do not invent.
- **Consistency beats cleverness.** Fit the existing architecture rather than improving on it in passing.
- **Traceability matters.** Every behavior should be walkable back to the document that granted it.
- **Maintainability and security are not optional** — per-user isolation and grounded, human-in-the-loop AI are the
  product's core promises.
- **Respect module ownership** and keep changes small and reviewable.
- **When behavior changes, the document changes with it.**
- **When a suggestion would require a decision, stop and defer to a human** ([CLAUDE.md §8](../CLAUDE.md)).

> Copilot's goal is not to generate code. It is to help build LedgerAI **incrementally while preserving the approved
> architecture.** Working code that violates the architecture is a failure, not a shortcut.
