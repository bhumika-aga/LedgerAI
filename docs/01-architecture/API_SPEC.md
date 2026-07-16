# API Specification — LedgerAI MVP

> **Status:** Draft v1
> **Owner:** Founding Engineer / Principal API Architect
> **Last updated:** 2026-07-14
> **API version:** v1
> **Upstream (frozen):
>
** [Product Vision](../00-product/PRODUCT_VISION.md) · [Product Decisions](../00-product/PRODUCT_DECISIONS.md) · [PRD](../00-product/PRD.md) · [SRS](../00-product/SRS.md) · [Architecture](./ARCHITECTURE.md) · [Database](./DATABASE.md)
> **Downstream:** [Security](./SECURITY.md) · [AI Architecture](./AI_ARCHITECTURE.md) · [ADRs](./decisions/)

---

## 1. Purpose

This document is the **complete REST API contract** for the LedgerAI MVP — every endpoint the React frontend consumes.
It is **implementation-ready but implementation-independent**: it defines URLs, methods, request/response shapes, status
codes, validation, and errors, with **no** Spring annotations, controller code, OpenAPI YAML, or JSON-schema artifacts.

It is the shared contract between the frontend and backend teams. It is documented
with [OpenAPI](../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions)
(PD-013) at implementation time; §"OpenAPI Organization" describes the grouping.

**Scope guardrails:** endpoints exist only for the twelve approved MVP
modules ([SRS §4](../00-product/SRS.md#4-functional-requirements)).
No endpoint crosses a [product boundary](../00-product/PRODUCT_DECISIONS.md#2-product-boundaries) or implements a
[Non-Goal](../00-product/PRD.md#5-non-goals). In particular, there is **no email-sending endpoint
** ([BR-034](../00-product/SRS.md#5-business-rules))
and report generation is **single-document** ([BR-035](../00-product/SRS.md#5-business-rules)).

---

## 2. Global API Conventions

### 2.1 Base URL

```txt
/api/v1
```

**Versioning philosophy:** the API is versioned in the **URI path** (`/api/v1`). A version increments only on a
**breaking** change; additive, backward-compatible changes ship within the same version (see
[§API Versioning Strategy](#20-api-versioning-strategy)). URI versioning is chosen for its discoverability and cache/log
friendliness; the trade-off is recorded in [§22](#22-api-decision-summary).

### 2.2 Resource Naming

| Rule                                   | Detail                                                                                                                                                                     |
|----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Plural resource names**              | Collections are plural nouns: `/clients`, `/documents`, `/reports`.                                                                                                        |
| **kebab-case paths**                   | Multi-word path segments use kebab-case: `/chat-messages` (if ever needed).                                                                                                |
| **Nouns, not verbs**                   | Actions are expressed by HTTP methods, not path verbs. Rare, genuinely non-CRUD sub-resources use a noun (e.g., `/summary`, `/search`).                                    |
| **Nested only where ownership exists** | Nesting reflects the ownership hierarchy from [DATABASE §4](./DATABASE.md#4-entity-relationship-diagram): documents belong to clients; summaries/chat belong to documents. |
| **camelCase JSON**                     | Request/response bodies use camelCase field names (frontend-friendly); the DB's snake_case is never exposed.                                                               |

Examples:

```txt
/clients
/clients/{clientId}/documents
/documents/{documentId}/summary
```

### 2.3 HTTP Methods

| Method     | Use                                                                                       | Idempotent     | Body |
|------------|-------------------------------------------------------------------------------------------|----------------|------|
| **GET**    | Retrieve a resource or collection. Never mutates.                                         | Yes            | No   |
| **POST**   | Create a resource, or trigger a non-idempotent action (e.g., generate).                   | No             | Yes  |
| **PUT**    | Full replacement of a mutable resource.                                                   | Yes            | Yes  |
| **PATCH**  | Partial update of a resource.                                                             | No (generally) | Yes  |
| **DELETE** | Remove a resource (soft or hard per [DATABASE §8](./DATABASE.md#8-soft-delete-strategy)). | Yes            | No   |

> MVP mutations use **PATCH** for partial edits (profile, client, report). PUT is reserved and not required by any MVP
> endpoint.

### 2.4 Status Codes

| Code                          | Meaning                                                      | When used                                                                                                            |
|-------------------------------|--------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| **200 OK**                    | Success with body.                                           | Reads; successful updates returning the resource.                                                                    |
| **201 Created**               | Resource created.                                            | Register, create client, upload document, synchronous AI result created.                                             |
| **202 Accepted**              | Accepted for async processing.                               | Long-running AI/OCR when processed asynchronously; caller polls status ([§2.11](#211-async-ready-behavior)).         |
| **204 No Content**            | Success, no body.                                            | Logout, delete, archive (no returned entity).                                                                        |
| **400 Bad Request**           | Malformed request/syntax.                                    | Unparseable body, wrong types.                                                                                       |
| **401 Unauthorized**          | Missing/invalid/expired authentication.                      | No/invalid access token ([BR-020](../00-product/SRS.md#5-business-rules)).                                           |
| **403 Forbidden**             | Authenticated but not authorized.                            | Accessing another user's resource ([BR-004](../00-product/SRS.md#5-business-rules)).                                 |
| **404 Not Found**             | Resource does not exist **or** is not visible to the caller. | Unknown id, soft-deleted document, cross-user resource (see note).                                                   |
| **409 Conflict**              | State conflict.                                              | Duplicate email on register; acting on a resource in an incompatible state (e.g., summarize a non-`READY` document). |
| **422 Unprocessable Entity**  | Well-formed but fails validation.                            | Field validation failures ([SRS §6](../00-product/SRS.md#6-validation-rules)); carries `validationErrors`.           |
| **429 Too Many Requests**     | Rate limit exceeded.                                         | Abuse/throttling ([§21](#21-security-considerations)).                                                               |
| **500 Internal Server Error** | Unexpected server fault.                                     | Unhandled errors ([SRS §8 Unknown](../00-product/SRS.md#8-error-handling)).                                          |
| **503 Service Unavailable**   | Dependency unavailable/degraded.                             | AI/OCR/Storage provider outage ([NFR-004](../00-product/SRS.md#9-non-functional-requirements)).                      |

> **404 vs 403 for cross-user access:** to avoid leaking existence of another user's data, the API **SHOULD** return
> **404** for resources the caller does not own, reserving **403** for cases where the resource's existence is already
> known to the caller. Detailed policy lives in [SECURITY.md](./SECURITY.md).

### 2.5 Pagination

Collection endpoints are paginated with query parameters:

| Param  | Type    | Default          | Description                                                   |
|--------|---------|------------------|---------------------------------------------------------------|
| `page` | integer | `0`              | Zero-based page index.                                        |
| `size` | integer | `20`             | Items per page (max `100` **[Assumption]**).                  |
| `sort` | string  | resource default | `field,(asc\|desc)`, e.g., `sort=createdAt,desc`. Repeatable. |

Responses use the [`PageResponse`](#19-common-dtos) envelope carrying `content` plus pagination metadata
(`page`, `size`, `totalElements`, `totalPages`, `hasNext`).

### 2.6 Filtering

Filters are expressed as query parameters named after response fields, e.g., `?status=ACTIVE` on clients,
`?status=READY` on documents. Multiple filters combine with **AND**. Unknown filter params are ignored (forward-
compatible). Filtering never bypasses ownership scoping.

### 2.7 Search

Keyword search uses the `q` parameter: `?q=<keywords>` (URL-encoded). Search is owner-scoped, excludes soft-deleted
documents ([BR-013](../00-product/SRS.md#5-business-rules)), and reflects extracted content and metadata
([FR-SRCH-003](../00-product/SRS.md#411-global-search-srch)). Details in [§14 Search](#14-search).

### 2.8 Date/Time

All timestamps are **ISO-8601 in UTC** with a trailing `Z` (e.g., `2026-07-14T09:30:00Z`). Clients render local time;
the API never emits zone-ambiguous values. Maps to PostgreSQL
`timestamptz` ([DATABASE §5](./DATABASE.md#5-entity-specifications)).

### 2.9 UUIDs

All resource identifiers are **UUIDs** (string form, e.g., `9f1c…`),
per [DATABASE §7](./DATABASE.md#7-primary-key-strategy).
Path variables named `{…Id}` are UUIDs. Malformed UUIDs yield `400`. Non-enumerable IDs are a deliberate security
property, not incidental.

### 2.10 Idempotency

| Operation class                                 | Idempotent?                                    |
|-------------------------------------------------|------------------------------------------------|
| GET, PUT, DELETE, archive                       | Yes — repeating produces the same end state.   |
| POST create (register, client, document upload) | No — repeats create duplicates unless guarded. |
| POST generate (summary, chat, email, report)    | No — each call is a new attempt/AI Request.    |

**Optional `Idempotency-Key`:** create endpoints (`POST /clients`, `POST …/documents`, register) **MAY** accept an
`Idempotency-Key` request header (a client-generated UUID). If two requests arrive with the same key within a short
window, the server returns the original result instead of creating a duplicate. This is optional for the MVP client but
part of the contract so it can be adopted without a breaking change.

### 2.11 Async-ready behavior

Because the processing mechanism for long-running OCR/AI work is a deferred decision
([DD-007](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)), generation endpoints are specified to be
**async-ready**:

- If the result is produced **synchronously**, the endpoint returns **`201`** with the completed resource.
- If processing is **asynchronous**, the endpoint returns **`202 Accepted`** with the resource in a non-terminal status
  (`REQUESTED`/`IN_PROGRESS` or document `PROCESSING`), and the client polls the corresponding **GET** (e.g., the
  summary, the OCR status) until it reaches `COMPLETED`/`READY` or `FAILED`.

Both are valid under this contract; the frontend MUST handle `202` + poll. This lets the backend adopt background
workers
later with **no contract change**.

### 2.12 Error Model — RFC 7807 Problem Details

All error responses use **RFC 7807 `application/problem+json`**. The [`ProblemDetails`](#19-common-dtos) object:

| Field              | Type                  | Always   | Description                                                                                                                 |
|--------------------|-----------------------|----------|-----------------------------------------------------------------------------------------------------------------------------|
| `type`             | string (URI)          | Yes      | Stable URI identifying the error kind (e.g., `/problems/validation-error`).                                                 |
| `title`            | string                | Yes      | Short, human-readable summary of the error kind.                                                                            |
| `status`           | integer               | Yes      | HTTP status code.                                                                                                           |
| `detail`           | string                | Yes      | Human-readable, non-technical explanation ([SRS §8](../00-product/SRS.md#8-error-handling)) — no stack traces or internals. |
| `instance`         | string (URI)          | Yes      | URI reference of the specific occurrence (usually the request path).                                                        |
| `timestamp`        | string (ISO-8601 UTC) | Yes      | When the error occurred.                                                                                                    |
| `traceId`          | string                | Yes      | Correlation id for support/observability ([NFR-014](../00-product/SRS.md#9-non-functional-requirements)).                   |
| `validationErrors` | array                 | When 422 | Field-level failures: `{ field, message }[]` ([SRS §6](../00-product/SRS.md#6-validation-rules)).                           |

No framework-specific fields are ever included. Example:

```json
{
  "type": "/problems/validation-error",
  "title": "Validation failed",
  "status": 422,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/clients",
  "timestamp": "2026-07-14T09:30:00Z",
  "traceId": "b7f3a9c2-1e4d-4b0a-9c11-3d2e6f7a8b90",
  "validationErrors": [
    {
      "field": "name",
      "message": "Client name is required."
    }
  ]
}
```

---

## API Design Rules

These rules are binding on **every future endpoint**. The conventions in [§2](#2-global-api-conventions) describe *how*
the current API is shaped; these rules ensure every new addition stays shaped the same way.

### Resource-Oriented Design

- APIs MUST expose **resources** rather than actions whenever practical.
- URLs MUST represent **nouns**, not verbs.
- Resource ownership SHOULD be reflected through URL hierarchy **only** when it represents a true ownership
  relationship.
- Endpoint naming MUST remain **consistent** across modules.

### Backward Compatibility

- Existing response fields MUST NOT change meaning.
- Existing fields MUST NOT be removed within an API version.
- New response fields MUST be optional.
- Clients MUST safely ignore unknown response fields.
- Breaking changes MUST require a new API version ([§20](#20-api-versioning-strategy)).

### Ownership & Authorization

- Every protected endpoint MUST validate resource ownership ([BR-004](../00-product/SRS.md#5-business-rules)).
- APIs MUST never expose another user's resources.
- Error responses MUST follow the approved security policy ([SECURITY.md](./SECURITY.md); see the `404`-for-non-owned
  stance in [§2.4](#24-status-codes)).

### DTO Principles

- Request and response DTOs MUST remain **independent of persistence models**.
- Internal database fields MUST never leak through the API (e.g., `storageReference`, `passwordHash`).
- Sensitive fields MUST never be serialized.
- DTOs SHOULD remain stable even if the database evolves.

### Mutation Rules

- Every mutation SHOULD generate an activity entry when appropriate ([Activity module](#15-activity-module-activities)).
- Mutations SHOULD be idempotent whenever business semantics allow ([§2.10](#210-idempotency)).
- Business validation MUST occur **before** persistence ([SRS §6](../00-product/SRS.md#6-validation-rules)).

### Consistency

- Similar resources SHOULD expose similar endpoint patterns.
- Pagination, filtering, searching, sorting, and error handling MUST remain consistent throughout the API.
- New modules MUST adopt the same conventions defined in this document.

These rules exist to preserve **long-term consistency as the API grows** and to prevent **design drift between modules
**.
An API that behaves the same way everywhere is one a frontend or third-party client can learn once and trust across
every
module; each rule above closes a common path by which multi-module APIs quietly become inconsistent.

---

## 3. Authentication

LedgerAI uses **JWT access tokens + refresh tokens
** ([PD-008](../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions),
[ADR-001](./decisions/ADR-001-Authentication-Strategy.md)). Full controls are in [SECURITY.md](./SECURITY.md); the
contract:

| Aspect                   | Contract                                                                                                                                                |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Access token**         | Short-lived JWT presented on every authenticated request.                                                                                               |
| **Refresh token**        | Longer-lived credential used only to obtain a new access token; persisted as a hash server-side ([DATABASE §5.9](./DATABASE.md#59-refreshtoken)).       |
| **Authorization header** | `Authorization: Bearer <accessToken>` on all protected endpoints.                                                                                       |
| **Refresh flow**         | When the access token expires, the client calls `POST /auth/refresh` with the refresh token to obtain a new access token (and a rotated refresh token). |
| **Logout**               | `POST /auth/logout` revokes the presented refresh token ([FR-AUTH-005](../00-product/SRS.md#41-authentication-auth)).                                   |

> The transport of the refresh token (e.g., httpOnly cookie vs. body) and access-token storage are **security decisions
> **
> specified in [SECURITY.md](./SECURITY.md), not here. This contract references the tokens abstractly.

---

## 4. API Modules — overview

| #  | Module         | Base path                                                  | SRS                                                       |
|----|----------------|------------------------------------------------------------|-----------------------------------------------------------|
| 5  | Authentication | `/auth`                                                    | [§4.1](../00-product/SRS.md#41-authentication-auth)       |
| 6  | User           | `/users/me`                                                | [§4.2](../00-product/SRS.md#42-user-profile-prof)         |
| 7  | Clients        | `/clients`                                                 | [§4.3](../00-product/SRS.md#43-client-management-clnt)    |
| 8  | Documents      | `/clients/{clientId}/documents`, `/documents/{documentId}` | [§4.4–4.5](../00-product/SRS.md#44-document-upload-upld)  |
| 9  | OCR            | `/documents/{documentId}/ocr-status`                       | [§4.6](../00-product/SRS.md#46-ocr-ocr)                   |
| 10 | AI Summary     | `/documents/{documentId}/summary`                          | [§4.7](../00-product/SRS.md#47-ai-summary-summ)           |
| 11 | AI Chat        | `/documents/{documentId}/chat`                             | [§4.8](../00-product/SRS.md#48-ai-chat-chat)              |
| 12 | AI Email       | `/ai/emails`                                               | [§4.9](../00-product/SRS.md#49-ai-email-generation-email) |
| 13 | Reports        | `/reports`, `/documents/{documentId}/reports`              | [§4.10](../00-product/SRS.md#410-report-generation-rpt)   |
| 14 | Search         | `/search`                                                  | [§4.11](../00-product/SRS.md#411-global-search-srch)      |
| 15 | Activity       | `/activities`                                              | [§4.12](../00-product/SRS.md#412-activity-timeline-tmln)  |

> All endpoints except `POST /auth/register` and `POST /auth/login` require
> authentication ([FR-AUTH-006](../00-product/SRS.md#41-authentication-auth)).
> All data access is ownership-scoped ([BR-004](../00-product/SRS.md#5-business-rules)).

---

## 5. Authentication Module (`/auth`)

### 5.1 Register

- **Purpose:** Create a new professional account ([FR-AUTH-001](../00-product/SRS.md#41-authentication-auth)).
- **Method / URL:** `POST /api/v1/auth/register`
- **Auth required:** No
- **Path variables:** —
- **Query params:** —
- **Request body:** `{ email, password, fullName? }`
- **Response body:** `{ user: UserResponse, tokens: AuthTokens }` (or `UserResponse` with tokens set via cookie — see
  SECURITY.md)
- **Success codes:** `201 Created`
- **Error codes:** `409` (email exists, [BR-021](../00-product/SRS.md#5-business-rules)), `422` (validation)
- **Validation:** [VR-001](../00-product/SRS.md#6-validation-rules) — email format, password policy, required fields.
- **Notes:** Password is never returned; stored as BCrypt hash ([BR-022](../00-product/SRS.md#5-business-rules)). MAY
  accept `Idempotency-Key`.

### 5.2 Login

- **Purpose:** Authenticate and establish a session ([FR-AUTH-002](../00-product/SRS.md#41-authentication-auth)).
- **Method / URL:** `POST /api/v1/auth/login`
- **Auth required:** No
- **Request body:** `{ email, password }`
- **Response body:** `{ user: UserResponse, tokens: AuthTokens }`
- **Success codes:** `200 OK`
- **Error codes:** `401` (invalid credentials — **generic, non-revealing**
  message, [BR-020](../00-product/SRS.md#5-business-rules)), `422` (missing fields), `429` (too many
  attempts, [FR-AUTH-008](../00-product/SRS.md#41-authentication-auth))
- **Validation:** [VR-002](../00-product/SRS.md#6-validation-rules).
- **Notes:** Response MUST NOT reveal whether the email exists.

### 5.3 Refresh Token

- **Purpose:** Obtain a new access token using a valid refresh
  token ([FR-AUTH-004](../00-product/SRS.md#41-authentication-auth)).
- **Method / URL:** `POST /api/v1/auth/refresh`
- **Auth required:** No (presents refresh token, not access token)
- **Request body:** `{ refreshToken }` *(or refresh token supplied via httpOnly cookie — see SECURITY.md)*
- **Response body:** `{ tokens: AuthTokens }`
- **Success codes:** `200 OK`
- **Error codes:** `401` (invalid/expired/revoked refresh token)
- **Notes:** Refresh tokens are **rotated** on use; the prior token is
  revoked ([DATABASE §12](./DATABASE.md#12-data-retention)).

### 5.4 Logout

- **Purpose:** End the session by revoking the refresh
  token ([FR-AUTH-005](../00-product/SRS.md#41-authentication-auth)).
- **Method / URL:** `POST /api/v1/auth/logout`
- **Auth required:** Yes
- **Request body:** `{ refreshToken }` *(or cookie)*
- **Response body:** —
- **Success codes:** `204 No Content`
- **Error codes:** `401`
- **Notes:** Idempotent — logging out an already-revoked token still returns `204`.

### 5.5 Current User

- **Purpose:** Return the authenticated user's identity.
- **Method / URL:** `GET /api/v1/auth/me`
- **Auth required:** Yes
- **Response body:** `UserResponse`
- **Success codes:** `200 OK`
- **Error codes:** `401`
- **Notes:** Convenience endpoint for session bootstrap; mirrors `GET /users/me`.

---

## 6. User Module (`/users/me`)

### 6.1 Get Profile

- **Purpose:** Retrieve the current user's profile ([FR-PROF-001](../00-product/SRS.md#42-user-profile-prof)).
- **Method / URL:** `GET /api/v1/users/me`
- **Auth required:** Yes
- **Response body:** `UserResponse`
- **Success codes:** `200 OK`
- **Error codes:** `401`
- **Notes:** A user can only ever read their own profile ([FR-PROF-004](../00-product/SRS.md#42-user-profile-prof));
  there is no `/users/{id}` in MVP.

### 6.2 Update Profile

- **Purpose:** Update editable profile fields and
  preferences ([FR-PROF-002](../00-product/SRS.md#42-user-profile-prof)).
- **Method / URL:** `PATCH /api/v1/users/me`
- **Auth required:** Yes
- **Request body:** `{ fullName?, professionalDetails?, preferences? }`
- **Response body:** `UserResponse`
- **Success codes:** `200 OK`
- **Error codes:** `401`, `422` (validation)
- **Validation:** [VR-003](../00-product/SRS.md#6-validation-rules).
- **Notes:** Email/password changes are out of MVP scope; not exposed here.

---

## 7. Clients Module (`/clients`)

### 7.1 List Clients

- **Purpose:** List the user's clients ([FR-CLNT-002](../00-product/SRS.md#43-client-management-clnt)).
- **Method / URL:** `GET /api/v1/clients`
- **Auth required:** Yes
- **Query params:** `page`, `size`, `sort`, `status` (`ACTIVE`|`ARCHIVED`), `q` (optional name filter)
- **Response body:** `PageResponse<ClientResponse>`
- **Success codes:** `200 OK`
- **Error codes:** `401`
- **Notes:** Defaults to `ACTIVE`; owner-scoped.

### 7.2 Get Client

- **Purpose:** Retrieve a single client ([FR-CLNT-002](../00-product/SRS.md#43-client-management-clnt)).
- **Method / URL:** `GET /api/v1/clients/{clientId}`
- **Auth required:** Yes
- **Path variables:** `clientId` (UUID)
- **Response body:** `ClientResponse`
- **Success codes:** `200 OK`
- **Error codes:** `401`, `404` (unknown or not owned)

### 7.3 Create Client

- **Purpose:** Create a client ([FR-CLNT-001](../00-product/SRS.md#43-client-management-clnt)).
- **Method / URL:** `POST /api/v1/clients`
- **Auth required:** Yes
- **Request body:** `{ name, contactDetails?, notes? }`
- **Response body:** `ClientResponse`
- **Success codes:** `201 Created`
- **Error codes:** `401`, `422` (validation)
- **Validation:** [VR-004](../00-product/SRS.md#6-validation-rules). Duplicate names are **allowed** but MAY return a
  soft warning ([BR-024](../00-product/SRS.md#5-business-rules)) — not an error.
- **Notes:** Emits `CLIENT_CREATED` activity. MAY accept `Idempotency-Key`.

### 7.4 Update Client

- **Purpose:** Edit client fields ([FR-CLNT-003](../00-product/SRS.md#43-client-management-clnt)).
- **Method / URL:** `PATCH /api/v1/clients/{clientId}`
- **Auth required:** Yes
- **Path variables:** `clientId`
- **Request body:** `{ name?, contactDetails?, notes? }`
- **Response body:** `ClientResponse`
- **Success codes:** `200 OK`
- **Error codes:** `401`, `404`, `422`

### 7.5 Archive Client

- **Purpose:** Archive (soft-deactivate) a client ([FR-CLNT-004](../00-product/SRS.md#43-client-management-clnt)).
- **Method / URL:** `DELETE /api/v1/clients/{clientId}`
- **Auth required:** Yes
- **Path variables:** `clientId`
- **Response body:** —
- **Success codes:** `204 No Content`
- **Error codes:** `401`, `404`
- **Notes:** **Archive, not hard delete** — sets `status=ARCHIVED`; the client's documents are **retained
  ** ([BR-002](../00-product/SRS.md#5-business-rules)). `DELETE` here is the archive action
  per [DATABASE §8](./DATABASE.md#8-soft-delete-strategy). Idempotent.

---

## 8. Documents Module

Documents are nested under their owning client for creation/listing ([BR-001](../00-product/SRS.md#5-business-rules)); a
single document is addressed directly for read/delete.

### 8.1 Upload Document

- **Purpose:** Upload a file to a client and begin
  processing ([FR-UPLD-001](../00-product/SRS.md#44-document-upload-upld)).
- **Method / URL:** `POST /api/v1/clients/{clientId}/documents`
- **Auth required:** Yes
- **Path variables:** `clientId`
- **Request body:** `multipart/form-data` — `file` (binary), optional metadata fields.
- **Response body:** `DocumentResponse` (initial `status` = `UPLOADED` or `PROCESSING`)
- **Success codes:** `201 Created`
- **Error codes:** `401`, `404` (client not found/owned), `409` (client archived — optional policy), `413`/`422` (
  oversized/unsupported — see note), `422` (validation), `503` (storage unavailable)
- **Validation:** [VR-005](../00-product/SRS.md#6-validation-rules) — allowed types and max
  size \* \*[Assumption; finalized in SRS/architecture]\*\*.
- **Notes:** The binary goes to the Storage Provider; the DB stores a
  reference ([DATABASE §1.3](./DATABASE.md#13-related-documents)). Rejections use `422` with a Problem Details reason (a
  stricter `413 Payload Too Large` MAY be used for size). Emits `DOCUMENT_UPLOADED`. Processing/OCR proceed
  asynchronously; poll [OCR status](#91-get-ocr-status). MAY accept `Idempotency-Key`.

### 8.2 List Documents

- **Purpose:** List a client's documents ([FR-CLNT-006](../00-product/SRS.md#43-client-management-clnt)).
- **Method / URL:** `GET /api/v1/clients/{clientId}/documents`
- **Auth required:** Yes
- **Query params:** `page`, `size`, `sort`, `status`
- **Response body:** `PageResponse<DocumentResponse>`
- **Success codes:** `200 OK`
- **Error codes:** `401`, `404`
- **Notes:** Excludes soft-deleted (`DELETED`) documents by default ([BR-013](../00-product/SRS.md#5-business-rules)).

### 8.3 Get Document

- **Purpose:** Retrieve a single document's metadata and state.
- **Method / URL:** `GET /api/v1/documents/{documentId}`
- **Auth required:** Yes
- **Path variables:** `documentId`
- **Response body:** `DocumentResponse`
- **Success codes:** `200 OK`
- **Error codes:** `401`, `404` (unknown, not owned, or deleted)

### 8.4 Delete Document

- **Purpose:** Delete a document ([FR-STOR-004](../00-product/SRS.md#45-document-storage-stor)).
- **Method / URL:** `DELETE /api/v1/documents/{documentId}`
- **Auth required:** Yes
- **Path variables:** `documentId`
- **Response body:** —
- **Success codes:** `204 No Content`
- **Error codes:** `401`, `404`
- **Notes:** Soft-deletes (`status=DELETED`); removes it from listings, search, and AI
  actions ([BR-012/013](../00-product/SRS.md#5-business-rules)); the external file SHOULD be removed from storage. Emits
  `DOCUMENT_DELETED`. Idempotent.

### 8.5 Download Metadata / Access Link

- **Purpose:** Obtain the means to view/download the original
  file ([FR-STOR-003](../00-product/SRS.md#45-document-storage-stor)).
- **Method / URL:** `GET /api/v1/documents/{documentId}/download`
- **Auth required:** Yes
- **Path variables:** `documentId`
- **Response body:** `{ downloadUrl, expiresAt, mimeType, originalFilename, sizeBytes }` — a time-limited access
  reference to the stored file.
- **Success codes:** `200 OK`
- **Error codes:** `401`, `404`, `503` (storage unavailable)
- **Notes:** Returns **download metadata** (a short-lived, owner-scoped URL), not raw bytes streamed through the API —
  consistent with the external-storage design. The exact link mechanics are a storage
  concern ([ADR-002](./decisions/ADR-002-Storage-Provider.md)).

---

## 9. OCR Module

OCR has no user-triggered endpoint (it runs automatically during
processing, [FR-OCR-001](../00-product/SRS.md#46-ocr-ocr));
only its status is exposed.

### 9.1 Get OCR Status

- **Purpose:** Report extraction/processing status for a document ([FR-OCR-003](../00-product/SRS.md#46-ocr-ocr)).
- **Method / URL:** `GET /api/v1/documents/{documentId}/ocr-status`
- **Auth required:** Yes
- **Path variables:** `documentId`
- **Response body:** `{ documentId, status, extractionMethod?, extractionQuality?, failureReason? }` where `status` ∈
  document lifecycle ([SRS §7.1](../00-product/SRS.md#71-document-lifecycle)).
- **Success codes:** `200 OK`
- **Error codes:** `401`, `404`
- **Notes:** This is the **poll endpoint** for upload/processing ([§2.11](#211-async-ready-behavior)). A document is
  usable by AI features only when `status = READY` ([BR-010](../00-product/SRS.md#5-business-rules)).

---

## 10. AI Summary Module

### 10.1 Generate Summary

- **Purpose:** Produce an AI summary of a `READY` document ([FR-SUMM-001](../00-product/SRS.md#47-ai-summary-summ)).
- **Method / URL:** `POST /api/v1/documents/{documentId}/summary`
- **Auth required:** Yes
- **Path variables:** `documentId`
- **Request body:** — (none required) *(optional `{ regenerate: true }` to force a new attempt)*
- **Response body:** `AIResponse` (type `SUMMARY`, with `status` and, when complete, editable `content`)
- **Success codes:** `201 Created` (synchronous) **or** `202 Accepted` (async —
  poll [Get Summary](#102-get-existing-summary))
- **Error codes:** `401`, `404`, `409` (document not `READY`, [BR-010](../00-product/SRS.md#5-business-rules)), `422` (
  no extractable text), `503` (AI provider unavailable)
- **Notes:** Grounded in extracted content ([BR-030](../00-product/SRS.md#5-business-rules)); result is **editable
  ** ([BR-031](../00-product/SRS.md#5-business-rules)) and AI-assisted, subject to
  review ([BR-032](../00-product/SRS.md#5-business-rules)). Emits `SUMMARY_GENERATED`.

### 10.2 Get Existing Summary

- **Purpose:** Retrieve the saved summary (and its status) for a
  document ([FR-SUMM-004](../00-product/SRS.md#47-ai-summary-summ)).
- **Method / URL:** `GET /api/v1/documents/{documentId}/summary`
- **Auth required:** Yes
- **Path variables:** `documentId`
- **Response body:** `AIResponse`
- **Success codes:** `200 OK`
- **Error codes:** `401`, `404` (no summary exists / document not found)
- **Notes:** Doubles as the async **poll** target; returns terminal (`COMPLETED`/`FAILED`) or in-progress status.

### 10.3 Edit Summary

- **Purpose:** Persist user edits to the summary (human-in-the-loop, [BR-031](../00-product/SRS.md#5-business-rules)).
- **Method / URL:** `PATCH /api/v1/documents/{documentId}/summary`
- **Auth required:** Yes
- **Request body:** `{ content }`
- **Response body:** `AIResponse` (`edited=true`)
- **Success codes:** `200 OK`
- **Error codes:** `401`, `404`, `422`
- **Notes:** Included because the SRS requires all AI output to be editable; without it the human-in-the-loop rule is
  unenforceable via the API.

---

## 11. AI Chat Module

MVP chat is **document-scoped** ([SRS §4.8](../00-product/SRS.md#48-ai-chat-chat)); modeled as `CHAT` AI requests on a
document ([DATABASE §3.1](./DATABASE.md#31-justification-for-entities-beyond-the-raw-list)). No separate conversation
resource in MVP.

### 11.1 Ask Question

- **Purpose:** Ask a grounded question about a `READY` document ([FR-CHAT-001](../00-product/SRS.md#48-ai-chat-chat)).
- **Method / URL:** `POST /api/v1/documents/{documentId}/chat`
- **Auth required:** Yes
- **Path variables:** `documentId`
- **Request body:** `{ question }`
- **Response body:** `AIResponse` (type `CHAT`; `content` is the grounded answer)
- **Success codes:** `201 Created` (synchronous) **or** `202 Accepted` (async — poll history)
- **Error codes:** `401`, `404`, `409` (document not `READY`), `422` (empty/invalid
  question, [VR-007](../00-product/SRS.md#6-validation-rules)), `503`
- **Notes:** Answers are grounded; when unsupported by the document, the answer says so rather than
  fabricating ([BR-033](../00-product/SRS.md#5-business-rules)). Emits chat activity.

### 11.2 Get Chat History

- **Purpose:** Retrieve the question/answer thread for a document ([FR-CHAT-004](../00-product/SRS.md#48-ai-chat-chat)).
- **Method / URL:** `GET /api/v1/documents/{documentId}/chat`
- **Auth required:** Yes
- **Path variables:** `documentId`
- **Query params:** `page`, `size`, `sort` (default `createdAt,asc`)
- **Response body:** `PageResponse<AIResponse>` (the document's `CHAT` exchanges, chronological)
- **Success codes:** `200 OK`
- **Error codes:** `401`, `404`
- **Notes:** Scoped to the document's context per MVP model.

---

## 12. AI Email Module (`/ai/emails`)

### 12.1 Generate Email

- **Purpose:** Draft a professional client email ([FR-EMAIL-001](../00-product/SRS.md#49-ai-email-generation-email)).
- **Method / URL:** `POST /api/v1/ai/emails`
- **Auth required:** Yes
- **Request body:** `{ instruction, clientId?, documentId? }` — instruction plus optional client/document context (a
  referenced document MUST be `READY`).
- **Response body:** `AIResponse` (type `EMAIL`; editable `content`)
- **Success codes:** `201 Created` **or** `202 Accepted`
- **Error codes:** `401`, `404` (referenced client/document not found/owned), `409` (referenced document not `READY`),
  `422` (invalid instruction, [VR-007](../00-product/SRS.md#6-validation-rules)), `503`
- **Notes:** Draft only — **the API never sends email** ([BR-034](../00-product/SRS.md#5-business-rules)). Editable and
  review-required. Not nested under a document because email context is optional. Emits `EMAIL_GENERATED`.

---

## 13. Reports Module (`/reports`)

Reports are generated from a single `READY` document ([BR-035](../00-product/SRS.md#5-business-rules)) and thereafter
managed as first-class resources.

### 13.1 Generate Report

- **Purpose:** Generate a report from a document ([FR-RPT-001](../00-product/SRS.md#410-report-generation-rpt)).
- **Method / URL:** `POST /api/v1/documents/{documentId}/reports`
- **Auth required:** Yes
- **Path variables:** `documentId`
- **Request body:** `{ title? }` *(optional generation hints)*
- **Response body:** `ReportResponse` (initial `status` `DRAFT`)
- **Success codes:** `201 Created` **or** `202 Accepted`
- **Error codes:** `401`, `404`, `409` (document not `READY`), `422`, `503`
- **Notes:** Single-document only. Editable ([BR-031](../00-product/SRS.md#5-business-rules)); AI-assisted,
  review-required. Emits `REPORT_CREATED`.

### 13.2 List Reports

- **Purpose:** List the user's reports ([FR-RPT](../00-product/SRS.md#410-report-generation-rpt)).
- **Method / URL:** `GET /api/v1/reports`
- **Auth required:** Yes
- **Query params:** `page`, `size`, `sort`, `documentId` (filter), `status` (`DRAFT`|`SAVED`)
- **Response body:** `PageResponse<ReportResponse>`
- **Success codes:** `200 OK`
- **Error codes:** `401`
- **Notes:** Owner-scoped.

### 13.3 Get Report

- **Method / URL:** `GET /api/v1/reports/{reportId}`
- **Auth required:** Yes
- **Path variables:** `reportId`
- **Response body:** `ReportResponse`
- **Success codes:** `200 OK`
- **Error codes:** `401`, `404`

### 13.4 Update Report

- **Purpose:** Edit/save a report ([FR-RPT-003](../00-product/SRS.md#410-report-generation-rpt)).
- **Method / URL:** `PATCH /api/v1/reports/{reportId}`
- **Auth required:** Yes
- **Path variables:** `reportId`
- **Request body:** `{ title?, content?, status? }` (`status` may move `DRAFT`→`SAVED`)
- **Response body:** `ReportResponse`
- **Success codes:** `200 OK`
- **Error codes:** `401`, `404`, `422`
- **Notes:** Export/download is a client-side action on the returned content; a dedicated export endpoint is a future
  addition, not required by the MVP contract.

### 13.5 Delete Report

- **Method / URL:** `DELETE /api/v1/reports/{reportId}`
- **Auth required:** Yes
- **Path variables:** `reportId`
- **Response body:** —
- **Success codes:** `204 No Content`
- **Error codes:** `401`, `404`
- **Notes:** Hard delete ([DATABASE §8](./DATABASE.md#8-soft-delete-strategy)). Idempotent.

---

## 14. Search Module (`/search`)

### 14.1 Global Search

- **Purpose:** Search across the user's documents and
  content ([FR-SRCH-001](../00-product/SRS.md#411-global-search-srch)).
- **Method / URL:** `GET /api/v1/search`
- **Auth required:** Yes
- **Query params:** `q` (required keywords), `page`, `size`, optional `type` (e.g., `DOCUMENT`) for future scoping
- **Response body:** `PageResponse<SearchResultResponse>` — each result carries
  `{ documentId, clientId, title/snippet, matchContext }` for
  navigation ([FR-SRCH-002](../00-product/SRS.md#411-global-search-srch))
- **Success codes:** `200 OK`
- **Error codes:** `401`, `422` (invalid/oversized query, [VR-006](../00-product/SRS.md#6-validation-rules))
- **Notes:** Owner-scoped; excludes `DELETED` documents ([BR-013](../00-product/SRS.md#5-business-rules)); reflects
  extracted text + metadata. Empty/no-result queries return an empty
  page ([FR-SRCH-006](../00-product/SRS.md#411-global-search-srch)).

---

## 15. Activity Module (`/activities`)

### 15.1 Get Timeline

- **Purpose:** Retrieve the chronological activity
  timeline ([FR-TMLN-002](../00-product/SRS.md#412-activity-timeline-tmln)).
- **Method / URL:** `GET /api/v1/activities`
- **Auth required:** Yes
- **Query params:** `page`, `size`, `sort` (default `createdAt,desc`), optional `clientId` (per-client view)
- **Response body:** `PageResponse<ActivityResponse>`
- **Success codes:** `200 OK`
- **Error codes:** `401`
- **Notes:** **Read-only** — no create/update/delete
  endpoints ([FR-TMLN-004](../00-product/SRS.md#412-activity-timeline-tmln), [BR-016](../00-product/SRS.md#5-business-rules)).
  Owner-scoped; supports account-level and per-client views.

---

## 16. Endpoint Summary

| Module    | Method | Path                                 | Auth |
|-----------|--------|--------------------------------------|------|
| Auth      | POST   | `/auth/register`                     | No   |
| Auth      | POST   | `/auth/login`                        | No   |
| Auth      | POST   | `/auth/refresh`                      | No   |
| Auth      | POST   | `/auth/logout`                       | Yes  |
| Auth      | GET    | `/auth/me`                           | Yes  |
| User      | GET    | `/users/me`                          | Yes  |
| User      | PATCH  | `/users/me`                          | Yes  |
| Clients   | GET    | `/clients`                           | Yes  |
| Clients   | GET    | `/clients/{clientId}`                | Yes  |
| Clients   | POST   | `/clients`                           | Yes  |
| Clients   | PATCH  | `/clients/{clientId}`                | Yes  |
| Clients   | DELETE | `/clients/{clientId}` (archive)      | Yes  |
| Documents | POST   | `/clients/{clientId}/documents`      | Yes  |
| Documents | GET    | `/clients/{clientId}/documents`      | Yes  |
| Documents | GET    | `/documents/{documentId}`            | Yes  |
| Documents | DELETE | `/documents/{documentId}`            | Yes  |
| Documents | GET    | `/documents/{documentId}/download`   | Yes  |
| OCR       | GET    | `/documents/{documentId}/ocr-status` | Yes  |
| Summary   | POST   | `/documents/{documentId}/summary`    | Yes  |
| Summary   | GET    | `/documents/{documentId}/summary`    | Yes  |
| Summary   | PATCH  | `/documents/{documentId}/summary`    | Yes  |
| Chat      | POST   | `/documents/{documentId}/chat`       | Yes  |
| Chat      | GET    | `/documents/{documentId}/chat`       | Yes  |
| Email     | POST   | `/ai/emails`                         | Yes  |
| Reports   | POST   | `/documents/{documentId}/reports`    | Yes  |
| Reports   | GET    | `/reports`                           | Yes  |
| Reports   | GET    | `/reports/{reportId}`                | Yes  |
| Reports   | PATCH  | `/reports/{reportId}`                | Yes  |
| Reports   | DELETE | `/reports/{reportId}`                | Yes  |
| Search    | GET    | `/search`                            | Yes  |
| Activity  | GET    | `/activities`                        | Yes  |

---

## 17. Common DTOs

> Shape descriptions only — no language-specific classes. Field names are camelCase; ids are UUID strings; timestamps
> are ISO-8601 UTC.

### 17.1 UserResponse

`{ id, email, fullName?, professionalDetails?, preferences?, createdAt, updatedAt }` — never includes `passwordHash`.

### 17.2 AuthTokens

`{ accessToken, tokenType: "Bearer", expiresIn, refreshToken? }` — `refreshToken` present only when not delivered via
cookie (see [SECURITY.md](./SECURITY.md)).

### 17.3 ClientResponse

`{ id, name, contactDetails?, notes?, status, archivedAt?, createdAt, updatedAt }`.

### 17.4 DocumentResponse

`{ id, clientId, originalFilename, mimeType, sizeBytes, status, extractionMethod?, failureReason?, createdAt, updatedAt }` —
never exposes the internal `storageReference`.

### 17.5 AIResponse

`{ id, type, status, documentId?, prompt?, content?, edited, failureReason?, createdAt, updatedAt }` — unifies Summary,
Chat, and Email results (mirrors AIRequest + AIOutput, [DATABASE §5.5–5.6](./DATABASE.md#55-airequest)). `content`
present when `status = COMPLETED`.

### 17.6 ReportResponse

`{ id, documentId, title?, content, status, createdAt, updatedAt }`.

### 17.7 SearchResultResponse

`{ documentId, clientId, title, snippet, matchContext, updatedAt }`.

### 17.8 ActivityResponse

`{ id, actionType, summary?, clientId?, documentId?, metadata?, createdAt }` — read-only.

### 17.9 PageResponse<T>

`{ content: T[], page, size, totalElements, totalPages, hasNext }`.

### 17.10 ProblemDetails

As defined in [§2.12](#212-error-model--rfc-7807-problem-details).

---

## 18. Validation

Request validation is enforced at the API boundary and maps directly to the SRS validation rules; failures return
**`422`** with `validationErrors`.

| Endpoint(s)                              | Rule                                              |
|------------------------------------------|---------------------------------------------------|
| `POST /auth/register`                    | [VR-001](../00-product/SRS.md#6-validation-rules) |
| `POST /auth/login`                       | [VR-002](../00-product/SRS.md#6-validation-rules) |
| `PATCH /users/me`                        | [VR-003](../00-product/SRS.md#6-validation-rules) |
| `POST/PATCH /clients…`                   | [VR-004](../00-product/SRS.md#6-validation-rules) |
| `POST …/documents` (upload)              | [VR-005](../00-product/SRS.md#6-validation-rules) |
| `GET /search`                            | [VR-006](../00-product/SRS.md#6-validation-rules) |
| `POST …/summary`, `…/chat`, `/ai/emails` | [VR-007](../00-product/SRS.md#6-validation-rules) |
| `POST …/reports`                         | [VR-008](../00-product/SRS.md#6-validation-rules) |

Concrete limits (sizes, lengths) are the SRS `[Assumption]` values, finalized in the SRS/architecture — not redefined
here.

---

## 19. Security Considerations (high level)

Detailed controls are in [SECURITY.md](./SECURITY.md). Contract-level stance:

| Concern                 | Stance                                                                                                                                                               |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Authorization**       | Every protected endpoint requires a valid access token; unauthenticated → `401`.                                                                                     |
| **Ownership checks**    | Every resource access is scoped to the owner ([BR-004](../00-product/SRS.md#5-business-rules)); non-owned resources → `404`.                                         |
| **Upload restrictions** | Type/size validation ([VR-005](../00-product/SRS.md#6-validation-rules)); safe file handling ([NFR-009](../00-product/SRS.md#9-non-functional-requirements)).        |
| **Sensitive fields**    | `passwordHash`, raw tokens, and internal `storageReference` are never returned.                                                                                      |
| **Rate limiting**       | Auth and generation endpoints SHOULD be throttled; exceed → `429`.                                                                                                   |
| **Prompt safety**       | AI inputs validated; only necessary content sent to providers ([NFR-018](../00-product/SRS.md#9-non-functional-requirements)); outputs grounded and review-required. |
| **Transport**           | HTTPS only; `Bearer` tokens in the `Authorization` header.                                                                                                           |

---

## API Lifecycle

Every endpoint moves through a defined set of lifecycle states over its lifetime. This governs *how* an endpoint
changes;
the [Versioning Strategy](#20-api-versioning-strategy) governs *where* those changes are allowed to land.

| State            | Description                                                             |
|------------------|-------------------------------------------------------------------------|
| **Experimental** | Available for evaluation; the contract MAY change before stabilization. |
| **Stable**       | Fully supported contract for production use.                            |
| **Deprecated**   | Still supported but scheduled for removal in a future major version.    |
| **Sunset**       | An official removal date has been announced.                            |
| **Removed**      | The endpoint is no longer available.                                    |

> All MVP (`v1`) endpoints defined in this document are **Stable** unless explicitly marked otherwise.

### Deprecation Policy

- Deprecated endpoints SHOULD include clear documentation of their status and preferred replacement.
- Clients SHOULD migrate before the announced sunset date.
- Deprecation MUST never silently remove functionality.

### Removal Policy

- Endpoint removal MUST occur only in a **new major API version** ([§20](#20-api-versioning-strategy)).
- Removal SHOULD be communicated well in advance.
- Migration guidance SHOULD accompany every breaking removal.

### Compatibility Philosophy

LedgerAI favors **long-lived, stable APIs and incremental evolution** over frequent breaking changes. Endpoints are
meant
to be relied upon for the long term: the API grows by adding capabilities and deprecating gracefully, not by churning
contracts. This keeps the frontend and any future third-party clients reliable across releases — a stable contract is a
feature in its own right.

---

## 20. API Versioning Strategy

| Aspect                     | Policy                                                                                                                                                         |
|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **URI versioning**         | Version in the path (`/api/v1`). New major version only for breaking changes.                                                                                  |
| **Backward compatibility** | Additive changes (new endpoints, new optional fields, new enum values) are **non-breaking** and ship within `v1`. Clients MUST ignore unknown response fields. |
| **Deprecation policy**     | A deprecated endpoint/field is announced, marked in OpenAPI, and MAY return a `Deprecation`/`Sunset` header; it remains functional through a defined window.   |
| **Sunset strategy**        | Removal happens only in a new major version after the deprecation window; never silently within a version.                                                     |

This mirrors the additive-evolution discipline of [DATABASE](./DATABASE.md#database-migration-strategy) and
[SRS §14](../00-product/SRS.md#14-requirement-versioning).

---

## 21. OpenAPI Organization

The generated OpenAPI document groups operations by **tag**, one per module, in this order:

```txt
Authentication · Users · Clients · Documents · OCR · AI · Reports · Search · Activity
```

- **AI** groups Summary, Chat, and Email operations under one tag (they share the `AIResponse` model and provider
  boundary).
- Shared schemas (`ProblemDetails`, `PageResponse`, and the `*Response` DTOs) are defined once and referenced.
- Security schemes declare the `Bearer` JWT. The spec is served for interactive docs at implementation time (PD-013).

---

## 22. Future API Evolution

**Future, not MVP.** Recorded so they can be added **additively** within versioning
policy ([§20](#20-api-versioning-strategy)).
None are implemented now ([boundaries](../00-product/PRODUCT_DECISIONS.md#2-product-boundaries)).

| Future capability                | Additive shape                                                                                                                                 |
|----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| **Integrations** (Tally/QB/Xero) | `/integrations`, `/integrations/{id}/…` ([DD-006](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)).                                   |
| **Team collaboration**           | `/workspaces`, membership/role endpoints; ownership model extends to shared scope.                                                             |
| **Multi-tenancy**                | Tenant scoping via token claims/headers; no path change required.                                                                              |
| **Batch operations**             | `POST /documents/batch`, bulk status endpoints.                                                                                                |
| **Webhooks**                     | `/webhooks` subscriptions for async completion notifications.                                                                                  |
| **Background jobs**              | `/jobs/{jobId}` status resources (formalizes the async poll model, [DD-007](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)).         |
| **RAG / semantic search**        | New query params or a `/search` mode; response shape stays compatible ([DD-003/004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)). |
| **Rich conversations**           | `/conversations`, `/conversations/{id}/messages` superseding document-scoped chat.                                                             |

---

## 23. API Decision Summary

| Decision                   | Chosen Approach                                                  | Alternatives                 | Rationale                                                                                                                     | Related ADR   |
|----------------------------|------------------------------------------------------------------|------------------------------|-------------------------------------------------------------------------------------------------------------------------------|---------------|
| **API style**              | REST over HTTP/JSON                                              | GraphQL; RPC                 | Simplicity, discoverability, tooling, matches PD/architecture.                                                                | —             |
| **Versioning**             | URI path (`/api/v1`)                                             | Header/media-type versioning | Discoverable, cache/log-friendly, explicit ([§20](#20-api-versioning-strategy)).                                              | ADR (pending) |
| **Error model**            | RFC 7807 Problem Details                                         | Ad-hoc error JSON            | Standard, machine- and human-readable, consistent ([§2.12](#212-error-model--rfc-7807-problem-details)).                      | —             |
| **Authentication**         | JWT access + refresh (`Bearer`)                                  | Server sessions              | Stateless across Vercel/Render; PD-008 ([ADR-001](./decisions/ADR-001-Authentication-Strategy.md)).                           | ADR-001       |
| **Pagination**             | `page`/`size`/`sort` + `PageResponse`                            | Cursor pagination            | Simple, sufficient for MVP volumes; cursor is a future additive option.                                                       | —             |
| **Identifiers**            | UUIDs in paths/bodies                                            | Sequential ids               | Non-enumerable, matches [DATABASE §7](./DATABASE.md#7-primary-key-strategy).                                                  | ADR (pending) |
| **Idempotency**            | Idempotent GET/PUT/DELETE; optional `Idempotency-Key` on creates | No idempotency guarantees    | Safe retries; duplicate protection without complexity ([§2.10](#210-idempotency)).                                            | —             |
| **Resource ownership**     | Nesting under owner + owner-scoped access; `404` for non-owned   | Flat, unscoped resources     | Enforces [BR-004](../00-product/SRS.md#5-business-rules); prevents existence leaks.                                           | —             |
| **Async-ready generation** | `201` sync **or** `202` + poll                                   | Force-sync only              | Lets background workers be added with no contract change ([DD-007](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)). | ADR (pending) |
| **No email sending**       | Draft-only email endpoint                                        | Send email from API          | Enforces [BR-034](../00-product/SRS.md#5-business-rules) / MVP scope.                                                         | —             |

---

*This is the shared frontend/backend API contract for the LedgerAI MVP — endpoints, shapes, and errors, with no
framework code or OpenAPI YAML. It MUST remain consistent with the frozen Product Vision, Product Decisions, PRD, SRS,
Architecture, and Database. Security specifics are elaborated in [SECURITY.md](./SECURITY.md); AI internals in
[AI_ARCHITECTURE.md](./AI_ARCHITECTURE.md).*
