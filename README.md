# LedgerAI

LedgerAI is an AI-powered document intelligence platform for accounting professionals — Chartered Accountants, CPAs,
auditors, and accounting associates. It turns hours of document work into minutes: upload a financial document, let it
be read and understood, then **summarize it, ask grounded questions about it, draft a client email, generate a report,
and search across everything** — all in one place. It runs alongside existing systems (Tally, QuickBooks, Xero, Zoho,
SAP, Oracle, Excel) and is deliberately **not** an ERP, bookkeeping, payroll, tax-filing, or system-of-record product.

The product scope and boundaries are owned by [Product Vision](docs/00-product/PRODUCT_VISION.md) and
[Product Decisions](docs/00-product/PRODUCT_DECISIONS.md). This repository is **documentation-first**: the frozen
documents under [`docs/`](docs/) are the single source of truth, and the code conforms to them — never the reverse. The
engineering playbook that governs how changes are made is [`CLAUDE.md`](CLAUDE.md).

> **Status:** the documented MVP is **fully implemented** (all 12 modules) and verified by an automated test suite.
> Deployment (container image, live database, provider keys) is the remaining step. Live execution state is tracked in
> [Implementation Status](docs/03-engineering/IMPLEMENTATION_STATUS.md).

---

## What LedgerAI does — the core loop

The whole product is one loop — **upload → understand → act** — delivered by twelve modules:

| #  | Module                | What it does                                                                                     | Endpoint(s)                                                            |
|----|-----------------------|--------------------------------------------------------------------------------------------------|------------------------------------------------------------------------|
| 1  | **Authentication**    | Register, login, token refresh, logout                                                           | `POST /auth/{register,login,refresh,logout}`                           |
| 2  | **User Profile**      | View / edit the signed-in professional's profile                                                 | `GET`/`PATCH /users/me`                                                |
| 3  | **Client Management** | CRUD + soft-delete (archive) of the professional's clients                                       | `GET`/`POST`/`PATCH`/`DELETE /clients[/{id}]`                          |
| 4  | **Document Upload**   | Upload a file under a client; list/get/delete; signed download link                              | `.../clients/{id}/documents`, `/documents/{id}[/download]`             |
| 5  | **Document Storage**  | Bytes persisted to object storage behind a port (opaque refs, short-lived signed URLs)           | *(internal — Supabase adapter)*                                        |
| 6  | **OCR Processing**    | Extract text: native-first (PDFBox), OCR fallback (Google Vision); drives the document lifecycle | `GET /documents/{id}/ocr-status`                                       |
| 7  | **AI Summary**        | Generate / view / edit a grounded summary of a `READY` document                                  | `POST`/`GET`/`PATCH /documents/{id}/summary`                           |
| 8  | **AI Chat**           | Ask grounded questions about a document; retains the Q&A thread                                  | `POST`/`GET /documents/{id}/chat`                                      |
| 9  | **AI Email**          | Draft a professional client email from an instruction + optional context (never sent)            | `POST /ai/emails`                                                      |
| 10 | **Report Generation** | Generate / manage a report from a single document                                                | `POST /documents/{id}/reports`, `GET`/`PATCH`/`DELETE /reports[/{id}]` |
| 11 | **Global Search**     | PostgreSQL full-text search across the professional's extracted document text                    | `GET /search`                                                          |
| 12 | **Activity Timeline** | Read-only, append-only log of what happened (per-account and per-client)                         | `GET /activities`                                                      |

Every module is owner-scoped: a professional only ever sees their own clients, documents, and AI output. All API paths
are prefixed `/api/v1`; the full contract is [API_SPEC](docs/01-architecture/API_SPEC.md), and a ready-to-run
[Postman collection](postman/) mirrors it (see [API & Postman](#api--postman)).

---

## Key concepts behind the implementation

These are the ideas that shape every module — understanding them explains the whole codebase.

- **Documentation-first, frozen source of truth.** Product/architecture docs are written and approved *before* code.
  Code implements them exactly; when in doubt, the document wins ([`CLAUDE.md`](CLAUDE.md) §2–§3).
- **Modular monolith with hard boundaries.** The backend is one deployable, split into independent modules (`auth`,
  `users`, `clients`, `documents`, `ai`, `reports`, `search`, `activity`, plus shared `common`). Modules never reach
  into each other's internals — cross-module interaction goes only through **published services** returning DTOs (e.g.
  `DocumentService.requireOwnedContentForAi`, `ClientService.get`). There are no cross-module repository imports.
- **Ports & adapters (hexagonal) for every external provider.** Business logic depends only on domain-owned, provider-
  neutral **ports** — `AiPort`, `OcrPort`, `StoragePort`. The concrete providers (Anthropic, Google Vision, Supabase)
  live only in **adapters** selected by configuration and confined behind the port; **no provider SDK/type ever leaks
  into the core**, and tests run against in-memory ports (so no test ever contacts a real provider). Swapping a provider
  is an adapter change, nothing more ([ADR-003](docs/01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md),
  [ADR-008](docs/01-architecture/decisions/ADR-008-Object-Storage.md),
  [ADR-009](docs/01-architecture/decisions/ADR-009-OCR-Strategy.md)).
- **Ownership-based authorization, not RBAC.** Authentication (a valid Bearer JWT) is enforced by a stateless filter;
  **authorization is ownership**, enforced in the service layer via a shared `OwnershipGuard`. A resource the caller
  does not own returns **`404`** (never `403`) so existence is never revealed (SECURITY §5, BR-004). There is no role
  model — that is explicitly future.
- **Grounded, human-in-the-loop AI.** Every AI capability is **grounded** in the document's extracted text; prompts are
  composed centrally and **channel-separated** (fixed system instructions vs. the document/question as *data*), which is
  the structural defense against prompt injection. The model is told to say "the document does not state this" rather
  than fabricate. All AI output is **editable and review-required**, and email is **draft-only — never sent**
  (BR-031/032/033/034; AI_ARCHITECTURE §8–§12).
- **Synchronous-with-status AI lifecycle.** An AI action is an `AIRequest` with a lifecycle
  (`REQUESTED → IN_PROGRESS → COMPLETED | FAILED`) and its editable output an `AIOutput`. The MVP runs generation
  synchronously but exposes the status so the seam is **async-ready** with no contract change (ADR-013). The provider
  call always happens **outside** the database transaction; only persistence of its result is transactional (ADR-010) —
  and on success the request, its output, and the activity commit atomically.
- **Flyway is the sole schema authority.** The schema is owned entirely by ordered migrations `V1…V8`; Hibernate never
  generates or alters it (`ddl-auto: none`). This guarantees the running schema always matches
  [DATABASE](docs/01-architecture/DATABASE.md) (ADR-016).
- **One unified error model (RFC 7807).** Every failure — validation (`422`), not-found (`404`), conflict (`409`),
  provider-unavailable (`503`), auth (`401`) — is returned as an `application/problem+json` document by a single global
  handler, including failures raised inside the security filter chain.
- **Fail-fast, fully externalized configuration.** No secret has a default; a missing datasource or JWT secret makes the
  app refuse to start rather than run insecurely. Every tunable ([Assumption] limit) is an environment variable, never a
  code constant.

---

## Architecture at a glance

```text
                    React SPA (Vite, MUI, React Query)
                      │  Bearer access token + httpOnly refresh cookie
                      ▼
  ┌───────────────────────────────────────────────────────────────┐
  │  Spring Boot backend (modular monolith)                        │
  │                                                                │
  │   Controllers ──▶ Services (business rules, OwnershipGuard) ──▶ Repositories ──▶ PostgreSQL
  │        │                     │                                        ▲              (Flyway V1–V8)
  │        │ RFC 7807            │ ports (provider-neutral)               │
  │        ▼                     ▼                                        │
  │  GlobalExceptionHandler   AiPort / OcrPort / StoragePort              │
  │                              │  (adapters, @Profile("!test"))         │
  └──────────────────────────────┼────────────────────────────────────────┘
                                  ▼
                Anthropic (AI) · Google Vision (OCR) · Supabase (Storage)
```

Layering is strict: **thin controllers → business logic in services → persistence in repositories**; DTOs cross the API
boundary (entities never do). See [ARCHITECTURE](docs/01-architecture/ARCHITECTURE.md).

---

## Technology stack

| Layer            | Technology                                                                                                                                                          |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Frontend         | React 19, TypeScript, Vite 6, Material UI, React Router, TanStack React Query, Axios                                                                                |
| Backend          | Java 21, Spring Boot 3.5, Spring Security (OAuth2 resource server, JWT), Spring Data JPA / Hibernate, Maven                                                         |
| Database         | PostgreSQL (schema via Flyway)                                                                                                                                      |
| Auth             | JWT access token (body) + rotated refresh token (httpOnly, `SameSite=Strict` cookie), BCrypt ([ADR-018](docs/01-architecture/decisions/ADR-018-Token-Transport.md)) |
| AI provider      | Anthropic, behind `AiPort` ([ADR-003](docs/01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md))                                                           |
| OCR provider     | Google Cloud Vision + Apache PDFBox (native), behind `OcrPort` ([ADR-009](docs/01-architecture/decisions/ADR-009-OCR-Strategy.md))                                  |
| Storage provider | Supabase Storage, behind `StoragePort` ([ADR-002](docs/01-architecture/decisions/ADR-002-Storage-Provider.md))                                                      |
| Tests            | JUnit 5, Mockito, Spring MVC Test, **Testcontainers** (real PostgreSQL); Vitest + Testing Library (frontend)                                                        |

There is intentionally **no** semantic search, vector database, embeddings, or RAG — search is plain PostgreSQL
full-text search, and those capabilities remain deferred (Product Decisions §4).

---

## Repository structure

```text
LedgerAI/
├── CLAUDE.md      # Engineering playbook: how to work inside this repository
├── README.md      # This file — the entry point
├── LICENSE        # Apache License 2.0
├── backend/       # Spring Boot backend (Maven) — the 12-module application
├── frontend/      # React + Vite single-page app
├── postman/       # Postman collection + environment for the API
├── docs/          # Frozen source of truth (product, architecture, design, engineering, AI, releases)
└── .github/       # Governance and CI (workflows, templates, CODEOWNERS)
```

Backend modules live under `backend/src/main/java/com/ledgerai/` (`auth`, `users`, `clients`, `documents`, `ai`,
`reports`, `search`, `activity`, `common`); each frontend feature lives under `frontend/src/features/`.

---

## Getting started

**Prerequisites:** Java 21, Maven, Node.js + npm, and (for integration tests / running locally) Docker.

### 1. Fast tests — no infrastructure required

```bash
# Backend unit/web tests (291 tests)
cd backend && mvn test

# Frontend tests + typecheck + production build (126 tests)
cd frontend && npm ci && npm run typecheck && npm run test && npm run build
```

### 2. Full integration suite — requires Docker

Testcontainers automatically starts a disposable `postgres:16-alpine`, Flyway migrates it, and the `*IT` tests exercise
the real database and full HTTP stack. **No manual database setup is needed.**

```bash
cd backend && mvn verify          # unit/web + all Testcontainers integration tests
```

> If Docker is not running, the 24 Testcontainers integration tests **self-skip** (they are annotated
> `@Testcontainers(disabledWithoutDocker = true)`). A skip is **not** a pass — run them with Docker (or in CI) to
> validate the database and end-to-end behavior.

### 3. Run the whole app locally

```bash
# a) Start PostgreSQL (or use a native Postgres 16)
docker run --name ledgerai-pg -p 5432:5432 \
  -e POSTGRES_USER=ledgerai -e POSTGRES_PASSWORD=ledgerai -e POSTGRES_DB=ledgerai \
  -d postgres:16-alpine

# b) Run the backend (Flyway applies V1–V8 on startup; serves on :8080)
cd backend
export DATABASE_URL=jdbc:postgresql://localhost:5432/ledgerai
export DATABASE_USERNAME=ledgerai
export DATABASE_PASSWORD=ledgerai
export JWT_SECRET='local-dev-secret-at-least-32-bytes-long!'   # HS256 requires ≥ 32 bytes
export AUTH_COOKIE_SECURE=false                                # allow the refresh cookie over http://localhost
mvn spring-boot:run
# health check:  curl http://localhost:8080/actuator/health   → {"status":"UP"}

# c) Run the frontend (serves on :5173, which the backend's default CORS already trusts)
cd frontend
npm ci
echo 'VITE_API_BASE_URL=http://localhost:8080/api/v1' > .env.local
npm run dev
```

**Provider keys are optional to start the app** — the AI/OCR/storage features degrade gracefully to `503` until set. To
exercise them, export: `AI_ANTHROPIC_API_KEY` (summary/chat/email/report), `OCR_GOOGLE_VISION_API_KEY` (scanned images),
and `STORAGE_URL` / `STORAGE_BUCKET` / `STORAGE_SERVICE_KEY` (upload/download). A text PDF + `AI_ANTHROPIC_API_KEY`
is enough to demo the full loop; native PDF extraction (PDFBox) needs no key.

### Configuration

All configuration is environment-driven ([
`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml)). Secrets have **no
defaults**. The ones that must be provided in production:

| Variable                                                 | Purpose                                                       |
|----------------------------------------------------------|---------------------------------------------------------------|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL connection                                         |
| `JWT_SECRET`                                             | HS256 signing key (**≥ 32 bytes**)                            |
| `CORS_ALLOWED_ORIGINS`                                   | Trusted frontend origin(s) (default: `http://localhost:5173`) |
| `STORAGE_URL`, `STORAGE_BUCKET`, `STORAGE_SERVICE_KEY`   | Supabase Storage                                              |
| `OCR_GOOGLE_VISION_API_KEY`                              | Google Cloud Vision                                           |
| `AI_ANTHROPIC_API_KEY`                                   | Anthropic                                                     |
| `VITE_API_BASE_URL` (frontend)                           | Backend API root (or same-origin)                             |

---

## API & Postman

The API contract is defined by [API_SPEC](docs/01-architecture/API_SPEC.md). A runnable
[Postman collection](postman/LedgerAI.postman_collection.json) and
[environment](postman/LedgerAI.postman_environment.json) cover every endpoint, grouped by module, with example request
bodies and **expected** responses (derived from API_SPEC; run them against your local instance to capture live results).

Quick start in Postman: import both files, select the *LedgerAI Local* environment, run **Auth → Register** (or
**Login**) — it stores the returned `accessToken` into the environment automatically, so every other request is
authenticated. Typical flow: Register → Create client → Upload document → poll OCR status → Generate summary / Ask a
question / Draft an email / Generate a report → Search → Activity timeline.

---

## Testing

| Suite                                | Command                               | Count            | Needs Docker |
|--------------------------------------|---------------------------------------|------------------|:------------:|
| Backend unit + web                   | `cd backend && mvn test`              | 291              |      no      |
| Backend integration (Testcontainers) | `cd backend && mvn verify`            | 24 `*IT` classes |   **yes**    |
| Frontend                             | `cd frontend && npm run test`         | 126              |      no      |
| Frontend typecheck / build           | `npm run typecheck` / `npm run build` | —                |      no      |

The testing approach (pyramid, coverage expectations, Testcontainers) is defined by
[Testing Strategy](docs/03-engineering/TESTING_STRATEGY.md).

---

## Documentation

The frozen documents under [`docs/`](docs/) define the product and architecture; read them in this order:

1. [Product Vision](docs/00-product/PRODUCT_VISION.md) → [Product Decisions](docs/00-product/PRODUCT_DECISIONS.md) →
   [PRD](docs/00-product/PRD.md) → [SRS](docs/00-product/SRS.md)
2. [Architecture](docs/01-architecture/ARCHITECTURE.md) and its domain specs —
   [Database](docs/01-architecture/DATABASE.md), [API Spec](docs/01-architecture/API_SPEC.md),
   [Security](docs/01-architecture/SECURITY.md), [AI Architecture](docs/01-architecture/AI_ARCHITECTURE.md) — plus the
   [ADRs](docs/01-architecture/decisions/)
3. [Engineering](docs/03-engineering/) — [Implementation Plan](docs/03-engineering/IMPLEMENTATION_PLAN.md),
   [Implementation Status](docs/03-engineering/IMPLEMENTATION_STATUS.md),
   [Testing Strategy](docs/03-engineering/TESTING_STRATEGY.md), and coding standards
4. [AI](docs/04-ai/), [Design](docs/02-design/), [Releases](docs/05-releases/)

Every change follows the same path — **documentation first, then implementation, tests, review, merge** — with each step
a gate; changes touching a frozen contract require explicit approval
([Contributing](docs/03-engineering/CONTRIBUTING.md),
[`CLAUDE.md`](CLAUDE.md)).

---

## Security

[`.github/SECURITY.md`](.github/SECURITY.md) is the responsible-disclosure policy (how to report a vulnerability).
[`docs/01-architecture/SECURITY.md`](docs/01-architecture/SECURITY.md) is the product's security architecture — the
authorization model, controls, and threat considerations the implementation satisfies.

---

## License

Licensed under the Apache License 2.0. See [`LICENSE`](LICENSE) for the full text.
