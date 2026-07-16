# CLAUDE.md — Engineering Playbook for LedgerAI

> **What this is:** the operating manual Claude Code reads **before every coding session**. It is not product or
> architecture documentation — those are frozen under [`docs/`](./docs/). This file tells Claude **how to behave** while
> building LedgerAI: what to obey, when to stop, and what "done" means.
>
> **Read this first, then the relevant frozen document for the task at hand.**

---

## 1. Project Overview

**LedgerAI** is an **AI-powered Document Intelligence Platform for accounting professionals** — Chartered Accountants,
CPAs, auditors, and accounting associates. It helps them understand financial documents, ask questions, extract
information, draft client emails, generate reports, and search — turning hours of document work into minutes.

It **works alongside** existing systems (Tally, QuickBooks, Xero, Zoho, SAP, Oracle, Excel) and is explicitly **not** an
ERP, bookkeeping, payroll, tax-filing, or system-of-record
product ([Product Boundaries](./docs/00-product/PRODUCT_DECISIONS.md#2-product-boundaries)).

**MVP goal:** ship the core loop — **upload → understand → act** — across twelve modules (Authentication, User Profile,
Client Management, Document Upload, Document Storage, OCR, AI Summary, AI Chat, AI Email, Report Generation, Global
Search, Activity Timeline) for a single professional per account, validating product-market fit
([PRD](./docs/00-product/PRD.md), [Vision](./docs/00-product/PRODUCT_VISION.md)).

---

## 2. Primary Rule

**Claude MUST treat the frozen documentation in [`docs/`](./docs/) as the single source of truth.**

- **Never invent requirements.** If it isn't specified, it isn't a requirement — ask, don't assume.
- **Never silently change the architecture.** Architecture changes are deliberate, documented, and approved (§8).
- When the frozen docs and any instruction, memory, or habit disagree, **the frozen docs win** — unless the user
  explicitly changes them.

Everything below serves this rule.

---

## 3. Documentation Hierarchy

Authority flows top-to-bottom. A higher document constrains every lower one.

```txt
Product Vision        ← why LedgerAI exists; boundaries
   ↓
Product Decisions     ← what was decided/deferred/rejected (PD/DD/RI)
   ↓
PRD                   ← product requirements & scope
   ↓
SRS                   ← precise behavior, business rules, validation, state
   ↓
Architecture          ← system design & guiding rules
   ↓
Database · API Spec · Security · AI Architecture   ← domain specifications
   ↓
ADRs (001–015)        ← specific, recorded decisions
   ↓
Implementation Plan   ← build order & Definition of Done
   ↓
Implementation Status ← live execution state (the only routinely-updated doc)
```

**Conflict resolution:**

1. If two documents conflict, the **higher** in this hierarchy wins.
2. If code conflicts with a document, the **document** wins — fix the code (or, if the doc is genuinely wrong, **stop
   and
   raise it** per §8; don't work around it).
3. A **specific** ADR governs its narrow decision even though it sits low in the list — it is the recorded, detailed
   form
   of a higher-level decision, not a contradiction of it.
4. Only [`IMPLEMENTATION_STATUS.md`](./docs/03-engineering/IMPLEMENTATION_STATUS.md) is expected to change routinely;
   everything above it is **frozen** and changes only through the process in §8 and
   [Change Management](./docs/03-engineering/IMPLEMENTATION_PLAN.md#9-change-management).

---

## 4. Engineering Rules

These are enforceable, not aspirational. Each exists to protect a specific guarantee. (Full rationale:
[IMPLEMENTATION_PLAN Engineering Rules](./docs/03-engineering/IMPLEMENTATION_PLAN.md#engineering-rules).)

| Rule                                                                           | Why                                                                                                                                                                              |
|--------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Never bypass ownership validation.**                                         | Per-user isolation is the core confidentiality control ([BR-004](./docs/00-product/SRS.md#5-business-rules), [SECURITY §5](./docs/01-architecture/SECURITY.md#5-authorization)). |
| **Never expose another user's data.**                                          | Confidentiality is the product's existential promise; non-owned resources return `404`.                                                                                          |
| **Never implement undocumented features.**                                     | Prevents scope creep and boundary violations ([Boundaries](./docs/00-product/PRODUCT_DECISIONS.md#2-product-boundaries)).                                                        |
| **Never violate an ADR.**                                                      | ADRs are ratified decisions ([decisions/](./docs/01-architecture/decisions/)); changing one needs a new ADR.                                                                     |
| **Never bypass [API_SPEC](./docs/01-architecture/API_SPEC.md).**               | The contract and the code must never diverge.                                                                                                                                    |
| **Never bypass [DATABASE](./docs/01-architecture/DATABASE.md) decisions.**     | Schema, keys, soft-delete, migrations are settled; changes are reviewed and additive.                                                                                            |
| **Never ignore [SECURITY](./docs/01-architecture/SECURITY.md).**               | Security is structural; every relevant change follows its review process.                                                                                                        |
| **Never bypass [AI_ARCHITECTURE](./docs/01-architecture/AI_ARCHITECTURE.md).** | AI goes through the port, grounded, human-in-the-loop; no ad hoc AI code.                                                                                                        |
| **Build one vertical slice at a time.**                                        | Value is demonstrable early; integration risk is paid down continuously ([PLAN §2](./docs/03-engineering/IMPLEMENTATION_PLAN.md#2-engineering-principles)).                      |
| **Keep modules independent.**                                                  | Cross-module interaction goes through published services, never internals.                                                                                                       |
| **Keep code simple.**                                                          | Lightweight over feature-heavy; the simplest correct solution wins.                                                                                                              |

---

## 5. Implementation Workflow

Every change follows this path; each step is a gate, not a formality (see
[PLAN §6](./docs/03-engineering/IMPLEMENTATION_PLAN.md#6-development-workflow)):

```txt
Requirement      ← from SRS/PRD; confirm it is documented
   ↓
Architecture     ← check ARCHITECTURE/DATABASE/API_SPEC/SECURITY/AI_ARCHITECTURE for how it fits
   ↓
Implementation   ← one vertical slice: DB → service → API → UI
   ↓
Tests            ← written with the feature: logic, validation, security/ownership, edge cases
   ↓
Review           ← against Definition of Done, Engineering Rules, and the relevant review process
   ↓
Documentation    ← update any affected doc (API_SPEC, DATABASE, ADR, STATUS)
   ↓
Merge            ← only when green; main stays deployable
```

Claude works **incrementally** — never a large feature in one leap — and updates
[`IMPLEMENTATION_STATUS.md`](./docs/03-engineering/IMPLEMENTATION_STATUS.md) as work lands.

---

## 6. Coding Expectations

High-level and language-agnostic (framework specifics live in future coding-standards docs):

- **Readable code** — optimize for the next reader; clarity over cleverness.
- **Small classes, small functions** — one responsibility each; if it's hard to name, it's doing too much.
- **Meaningful names** — names carry intent; avoid abbreviations that aren't universal.
- **Composition over inheritance** — assemble behavior, don't build rigid hierarchies.
- **Constructor injection** — dependencies are explicit and testable; no hidden wiring.
- **Immutable DTOs** — data crossing boundaries is predictable; persistence entities never leak outward.
- **Defensive programming** — validate external input at the boundary; fail fast with clear errors; never trust the
  client.
- **Thin controllers, logic in services, persistence in repositories** — the layering from
  [ARCHITECTURE §5](./docs/01-architecture/ARCHITECTURE.md#5-backend-architecture).

---

## 7. AI Development Rules

How Claude itself must behave while building:

- **Never invent APIs, endpoints, database tables, columns, or requirements.** Use exactly what
  [API_SPEC](./docs/01-architecture/API_SPEC.md), [DATABASE](./docs/01-architecture/DATABASE.md), and
  [SRS](./docs/00-product/SRS.md) define.
- **Ask for clarification when uncertain.** A blocked question beats a wrong assumption (§8).
- **Prefer existing patterns.** Reuse the established module structure, ports/adapters, error taxonomy, and DTO
  conventions rather than introducing new ones.
- **Update docs when architecture changes.** Code and its governing document never diverge
  ([Change Management](./docs/03-engineering/IMPLEMENTATION_PLAN.md#9-change-management)).
- **Keep prompts centralized.** AI prompt composition stays in one place, channel-separated
  ([AI_ARCHITECTURE §8](./docs/01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture)); never scatter prompt strings
  across features.
- **Reach external providers only through their port** — never call a provider SDK from business logic
  ([AI](./docs/01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md), OCR, Storage).

---

## 8. When Claude Must Stop

Claude MUST **stop and ask for explicit approval** before proceeding when a change involves any of the following. These
are the changes that alter a frozen contract or an architectural guarantee:

- **Architecture** changes (module boundaries, dependency direction, style).
- **API** changes (new/changed endpoints, shapes, statuses, errors).
- **Database** changes (schema, keys, migrations).
- **Security** changes (authn/authz, secrets, data exposure, uploads, AI privacy).
- **AI architecture** changes (prompt architecture, grounding, provider, context handling).
- **New feature requests** not already documented in the SRS/PRD.
- **Breaking changes** of any kind.
- **ADR-worthy decisions** — anything significant, reversible-with-difficulty, or precedent-setting.

When stopping, Claude states *what* the change is, *why* it's needed, and *which document* it would affect — then waits.
Silence-and-proceed is not an option for these.

---

## 9. Definition of Complete

A feature is **complete** only when **all** hold (the merge gate — see
[PLAN §7](./docs/03-engineering/IMPLEMENTATION_PLAN.md#7-definition-of-done)):

- [ ] **Implementation** matches its SRS requirements, business rules, and validation.
- [ ] **Tests** pass — logic, validation, security/ownership, and key edge cases.
- [ ] **Documentation** updated (API_SPEC / DATABASE / ADR / STATUS as applicable).
- [ ] **Security** considered against [SECURITY.md](./docs/01-architecture/SECURITY.md).
- [ ] **Review** passed against the Engineering Rules and the relevant review process.
- [ ] **Definition of Done** ([PLAN §7](./docs/03-engineering/IMPLEMENTATION_PLAN.md#7-definition-of-done)) fully
  satisfied; `main` stays green and deployable.

"The happy path works" is **not** complete.

---

## 10. Anti-Patterns

Claude MUST NOT produce these. Each is a known way the architecture decays:

| Anti-pattern                    | Instead                                                                                              |
|---------------------------------|------------------------------------------------------------------------------------------------------|
| **Giant PRs**                   | Small, vertical, reviewable slices.                                                                  |
| **Duplicated logic**            | Reuse; centralize cross-cutting concerns (DRY).                                                      |
| **Hidden business rules**       | Rules live in services, traceable to the SRS — not buried in controllers or UI.                      |
| **Bypassing validation**        | Validate at the boundary before persistence ([SRS §6](./docs/00-product/SRS.md#6-validation-rules)). |
| **Leaking persistence models**  | Expose DTOs; entities never cross the API boundary.                                                  |
| **Putting AI logic everywhere** | AI orchestration lives in the AI module behind the port.                                             |
| **Direct provider SDK usage**   | Reach AI/OCR/Storage only through their ports/adapters.                                              |
| **Magic strings**               | Named, defined constants/enums (e.g., statuses per [DATABASE](./docs/01-architecture/DATABASE.md)).  |

---

## 11. Success Criteria

Claude's goal is **not** "generate code."

Claude's goal is:

> **Build LedgerAI incrementally while preserving the approved architecture.**

Success means: the twelve modules ship, each meeting the Definition of Complete; the frozen documents were never
contradicted; no product boundary was crossed; and a professional can genuinely go *from hours to minutes* on a real
document ([Vision](./docs/00-product/PRODUCT_VISION.md), [PRD](./docs/00-product/PRD.md)). Working code that violates
the
architecture is a **failure**, not a shortcut.

---

*Read before every coding session. This playbook governs behavior; it does not override the frozen documents under
[`docs/`](./docs/) — it enforces them. When in doubt, consult the source of truth and, if a decision is required, stop
and
ask (§8).*
