# LedgerAI

LedgerAI is an AI-powered document intelligence platform for accounting professionals — Chartered Accountants, CPAs,
auditors, and accounting associates. It helps them understand financial documents, ask questions, extract information,
draft client emails, generate reports, and search across their work. It runs alongside existing systems (Tally,
QuickBooks, Xero, Zoho, SAP, Oracle, Excel) and is deliberately **not** an ERP, bookkeeping, payroll, tax-filing, or
system-of-record product. The product scope and boundaries are owned by
[Product Vision](docs/00-product/PRODUCT_VISION.md) and
[Product Decisions](docs/00-product/PRODUCT_DECISIONS.md).

---

## Repository philosophy

This repository is documentation-first and architecture-first. Documentation is written and approved before
implementation, architecture precedes code, and every change must trace back to an approved document. The frozen
documents under [`docs/`](docs/) are the single source of truth; code conforms to them, never the reverse.

How Claude Code and contributors must operate inside this constraint — what to obey, when to stop, and what "done"
means — is defined by [`CLAUDE.md`](CLAUDE.md). Read it before making any change. This README does not restate either;
it points to them.

---

## Repository structure

```text
LedgerAI/
├── CLAUDE.md      # Engineering playbook: how to work inside this repository
├── LICENSE        # Apache License 2.0
├── README.md      # This file — the entry point
├── docs/          # Frozen source of truth (product, architecture, design, engineering, AI, releases)
├── .github/       # Repository governance and automation (issue/PR templates, CODEOWNERS, workflows)
├── backend/       # Backend application — scaffold, not yet populated
├── frontend/      # Frontend application — scaffold, not yet populated
└── docker/        # Container configuration — scaffold, not yet populated
```

The `docs/` tree is organized by concern:

| Directory                                        | Concern                                                                 | 
|--------------------------------------------------|-------------------------------------------------------------------------|
| [`docs/00-product/`](docs/00-product/)           | Why the product exists, what was decided, and what it must do           |
| [`docs/01-architecture/`](docs/01-architecture/) | System design, domain specifications, and Architecture Decision Records |
| [`docs/02-design/`](docs/02-design/)             | Design system, components, UI guidelines, and user flows                |
| [`docs/03-engineering/`](docs/03-engineering/)   | How the product is built, tested, contributed to, and deployed          |
| [`docs/04-ai/`](docs/04-ai/)                     | AI providers, prompts, evaluation, and retrieval governance             |
| [`docs/05-releases/`](docs/05-releases/)         | Changelog and release notes                                             |

The `backend/`, `frontend/`, and `docker/` directories exist as scaffolds and are intentionally empty until
implementation begins.

---

## Documentation roadmap

New contributors should read in this order. Each item links to the document that owns the concern.

1. [Product Vision](docs/00-product/PRODUCT_VISION.md) — why LedgerAI exists and its boundaries.
2. [Product Decisions](docs/00-product/PRODUCT_DECISIONS.md) — what was accepted, deferred, and rejected.
3. [PRD](docs/00-product/PRD.md) — product requirements and scope.
4. [SRS](docs/00-product/SRS.md) — precise behavior, business rules, validation, and state.
5. [Architecture](docs/01-architecture/ARCHITECTURE.md) — system design and guiding rules, plus the domain
   specifications ([Database](docs/01-architecture/DATABASE.md), [API Spec](docs/01-architecture/API_SPEC.md),
   [Security](docs/01-architecture/SECURITY.md), [AI Architecture](docs/01-architecture/AI_ARCHITECTURE.md)) and the
   [Architecture Decision Records](docs/01-architecture/decisions/).
6. [Design](docs/02-design/) — [Design System](docs/02-design/DESIGN_SYSTEM.md),
   [Components](docs/02-design/COMPONENTS.md), [UI Guidelines](docs/02-design/UI_GUIDELINES.md), and
   [User Flows](docs/02-design/USER_FLOWS.md).
7. [Engineering](docs/03-engineering/) — [Implementation Plan](docs/03-engineering/IMPLEMENTATION_PLAN.md),
   [Implementation Status](docs/03-engineering/IMPLEMENTATION_STATUS.md),
   [Contributing](docs/03-engineering/CONTRIBUTING.md), [Testing Strategy](docs/03-engineering/TESTING_STRATEGY.md), and
   coding standards ([backend](docs/03-engineering/BACKEND_CODING_STANDARDS.md),
   [frontend](docs/03-engineering/FRONTEND_CODING_STANDARDS.md)).
8. [AI](docs/04-ai/) — [AI Providers](docs/04-ai/AI_PROVIDERS.md), [Prompts](docs/04-ai/PROMPTS.md),
   [Evaluation](docs/04-ai/EVALUATION.md), and [RAG](docs/04-ai/RAG.md).
9. [Releases](docs/05-releases/) — [Changelog](docs/05-releases/CHANGELOG.md) and
   [Release Notes](docs/05-releases/RELEASE_NOTES.md).

---

## Technology stack

The approved stack is fixed by the [Architecture](docs/01-architecture/ARCHITECTURE.md) and its Architecture Decision
Records. This section lists only what has been decided; it defines nothing.

| Layer          | Technology                                                                                                                        |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------|
| Frontend       | React, TypeScript, Vite, Material UI                                                                                              |
| Backend        | Java 21, Spring Boot 3, Spring Security, Spring Data JPA / Hibernate, Maven                                                       |
| Database       | PostgreSQL                                                                                                                        |
| API            | REST, documented with OpenAPI                                                                                                     |
| Authentication | JWT access tokens with refresh tokens ([ADR-001](docs/01-architecture/decisions/ADR-001-Authentication-Strategy.md))              |
| Hosting        | Vercel (frontend), Render (backend), Neon (PostgreSQL) — [ADR-012](docs/01-architecture/decisions/ADR-012-Deployment-Strategy.md) |

Some technologies are **deliberately deferred** and must not be assumed. These are recorded in
[Product Decisions §4](docs/00-product/PRODUCT_DECISIONS.md#4-deferred-decisions):

- **Storage provider** (DD-001) — to be resolved before Document Upload, in
  [ADR-002](docs/01-architecture/decisions/ADR-002-Storage-Provider.md).
- **AI / LLM provider** (DD-002) — the codebase commits to a provider-agnostic abstraction first; the concrete provider
  choice follows.
- **Vector database** (DD-003) and **RAG strategy** (DD-004) — deferred until retrieval earns its place.

---

## Development prerequisites

Only the tooling implied by the approved stack is listed. Precise versions beyond those fixed below are established by
the build configuration as implementation begins.

- **Java 21** — the backend language and runtime.
- **Maven** — the backend build tool.
- **Node.js and npm** — the frontend toolchain (React + Vite).
- **Docker** — for the containerized local and deployment configuration.
- **Git** — version control.

---

## Local development

Implementation has not started; the `backend/`, `frontend/`, and `docker/` directories are empty scaffolds. There are
therefore no build or run commands to document yet, and none are invented here.

The intended flow is the one the architecture prescribes: a modular Spring Boot backend, a feature-organized React SPA,
PostgreSQL for persistence, and container configuration under `docker/`. Concrete setup and run instructions will be
added to this section once the corresponding code lands, following the build order in the
[Implementation Plan](docs/03-engineering/IMPLEMENTATION_PLAN.md). Until then, treat the documentation as the product
and read it first.

---

## Repository workflow

Every change follows the same path — **documentation first, then implementation, testing, review, and merge** — with
each step acting as a gate. Changes that touch a frozen contract (architecture, API, database, security, or AI) require
explicit approval before proceeding.

This is summarized, not defined, here. The authoritative process — how a change is routed, reviewed, and approved —
lives in [Contributing](docs/03-engineering/CONTRIBUTING.md), and the behavioral rules that govern it live in
[`CLAUDE.md`](CLAUDE.md).

---

## Documentation ownership

Each concern is owned by exactly one area of `docs/`, and changes are routed to that owner:

| Concern      | Owner                                            |
|--------------|--------------------------------------------------|
| Product      | [`docs/00-product/`](docs/00-product/)           |
| Architecture | [`docs/01-architecture/`](docs/01-architecture/) |
| Design       | [`docs/02-design/`](docs/02-design/)             |
| Engineering  | [`docs/03-engineering/`](docs/03-engineering/)   |
| AI           | [`docs/04-ai/`](docs/04-ai/)                     |
| Release      | [`docs/05-releases/`](docs/05-releases/)         |

Review routing for these areas is expressed in [`.github/CODEOWNERS`](.github/CODEOWNERS). The ownership model itself is
defined by the documents; this table only points to them.

---

## Quality gates

Repository automation enforces hygiene without restating any document. Documentation integrity (empty files, missing
headings, broken relative links) is validated in CI; Markdown, spelling, and external links are checked; path-based
labels and stale-item management run automatically. Review routing is expressed through
[`.github/CODEOWNERS`](.github/CODEOWNERS), and every change is reviewed via the pull-request process before merge. The
automation lives under [`.github/`](.github/); its configuration is the source, and it is not described in detail here.

---

## Security

Two documents cover two different concerns. [`.github/SECURITY.md`](.github/SECURITY.md) is the responsible-disclosure
policy — how to report a vulnerability in this repository. [
`docs/01-architecture/SECURITY.md`](docs/01-architecture/SECURITY.md)
is the product's security architecture — the controls, authorization model, and threat considerations that the
implementation must satisfy. The first tells you how to report; the second defines what must be true.

---

## License

Licensed under the Apache License 2.0. See [`LICENSE`](LICENSE) for the full text.

---

## Status

Documentation is complete and frozen. Implementation has not yet started — the repository is at **Phase 0 (M0 — Project
Setup), 0% complete**, as tracked in
[Implementation Status](docs/03-engineering/IMPLEMENTATION_STATUS.md), the only routinely updated document.
