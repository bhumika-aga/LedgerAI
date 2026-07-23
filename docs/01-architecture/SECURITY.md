# Security Architecture — LedgerAI MVP

> **Status:** Draft v1
> **Owner:** Founding Engineer / Principal Security Architect
> **Last updated:** 2026-07-14
> **Upstream (frozen):
> ** [Product Vision](../00-product/PRODUCT_VISION.md) · [Product Decisions](../00-product/PRODUCT_DECISIONS.md) · [PRD](../00-product/PRD.md) · [SRS](../00-product/SRS.md) · [Architecture](./ARCHITECTURE.md) · [Database](./DATABASE.md) · [API Spec](./API_SPEC.md)
> **Related:** [AI Architecture](./AI_ARCHITECTURE.md) · [ADRs](./decisions/)

---

## Security Philosophy

Security in LedgerAI is **structural, not additive** — it is expressed in the architecture, data model, and API
contract, not bolted on afterward. LedgerAI handles **confidential client financial documents**; a breach of
confidentiality is an existential product failure, so the following principles are binding.

| Principle                         | What it means                                                                                               | Why it exists                                                                                                  |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| **Secure by Default**             | The safe configuration is the default; opting *out* is deliberate, never accidental.                        | Most breaches come from insecure defaults, not exotic attacks.                                                 |
| **Least Privilege**               | Every actor (user, service, provider) gets the minimum access needed, for the minimum time.                 | Limits blast radius when any single component is compromised.                                                  |
| **Defense in Depth**              | Multiple independent layers (transport, authn, authz, validation, storage isolation) each enforce security. | No single control failing should expose data.                                                                  |
| **Zero Trust**                    | No request is trusted by origin; every request is authenticated and authorized on its own merits.           | The Vercel/Render split and external providers mean there is no trusted internal network to lean on.           |
| **Privacy by Design**             | Only necessary data is collected, stored, and shared; deletion is a first-class capability.                 | Confidentiality is the product's core promise ([NFR-010](../00-product/SRS.md#9-non-functional-requirements)). |
| **Human-in-the-loop for AI**      | AI output is assistive and MUST be reviewed by the professional; it is never authoritative.                 | Trust and accuracy in a professional context ([BR-031/032](../00-product/SRS.md#5-business-rules)).            |
| **Fail Securely**                 | On error, deny access and reveal nothing; never fail *open*.                                                | A failure must not become an authorization bypass or information leak.                                         |
| **Explicit Ownership Validation** | Every data access is checked against the owning user, in code, every time.                                  | Per-user isolation is the primary confidentiality control ([BR-004](../00-product/SRS.md#5-business-rules)).   |
| **Secure Configuration**          | Secrets and security-relevant settings are externalized, never in source.                                   | Prevents the single most common credential-leak vector.                                                        |
| **Auditability**                  | Significant actions are recorded immutably.                                                                 | Detection, investigation, and trust ([NFR-012](../00-product/SRS.md#9-non-functional-requirements)).           |

These principles are implementation-independent; the sections below apply them. This document describes **what**
guarantees LedgerAI provides and **how** they are achieved architecturally — **not** Spring Security configuration.

---

## 1. Purpose

### 1.1 Scope

The complete **security architecture** for the LedgerAI MVP: threat model, authentication, authorization, data
protection, file and AI security, API security, secrets, logging, and the supporting policies. It is the authoritative
security reference for all future development. It is **implementation-independent** — no framework configuration, no
code.

### 1.2 Audience

Backend/frontend engineers, security reviewers, QA, and anyone extending LedgerAI. Every contributor is expected to
uphold the principles above.

### 1.3 Related Documents

| Document                                                  | Relevance                                                                                                                               |
|-----------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| [ARCHITECTURE.md](./ARCHITECTURE.md)                      | Security stance ([§12](./ARCHITECTURE.md#12-security-architecture-high-level)), ports/adapters, cross-cutting concerns.                 |
| [DATABASE.md](./DATABASE.md)                              | Ownership columns, password/token hashing, soft-delete, retention.                                                                      |
| [API_SPEC.md](./API_SPEC.md)                              | Auth, status codes (incl. `404`-for-non-owned), RFC 7807, rate limiting.                                                                |
| [SRS.md](../00-product/SRS.md)                            | Security NFRs ([§9](../00-product/SRS.md#9-non-functional-requirements)), business rules ([§5](../00-product/SRS.md#5-business-rules)). |
| [AI_ARCHITECTURE.md](./AI_ARCHITECTURE.md)                | AI provider boundary, grounding, data minimization (downstream).                                                                        |
| [ADR-001](./decisions/ADR-001-Authentication-Strategy.md) | JWT authentication decision.                                                                                                            |

---

## 2. Security Goals

| #    | Goal                                                                                        | Basis                                                             |
|------|---------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| SG-1 | **Protect client data** from unauthorized access or disclosure.                             | [NFR-007/010](../00-product/SRS.md#9-non-functional-requirements) |
| SG-2 | **Preserve confidentiality** — a user's data is visible only to that user.                  | [BR-004](../00-product/SRS.md#5-business-rules)                   |
| SG-3 | **Maintain integrity** — data and AI artifacts are not tampered with or silently corrupted. | [NFR-005/006](../00-product/SRS.md#9-non-functional-requirements) |
| SG-4 | **Ensure availability** appropriate to an early product; degrade gracefully.                | [NFR-004](../00-product/SRS.md#9-non-functional-requirements)     |
| SG-5 | **Prevent unauthorized access** at every layer (transport, authn, authz).                   | Defense in Depth                                                  |
| SG-6 | **Minimize attack surface** — expose only the approved API; no extra endpoints.             | [API_SPEC](./API_SPEC.md)                                         |
| SG-7 | **Enable auditing** of significant and security-relevant actions.                           | [NFR-012](../00-product/SRS.md#9-non-functional-requirements)     |
| SG-8 | **Support future compliance** without re-architecture.                                      | [§19](#19-future-security-evolution)                              |

---

## Security Design Rules

These rules are **non-negotiable**: every future feature and architectural decision MUST follow them. Where the sections
below describe the current controls, these rules bind whatever is built next.

### Trust Boundaries

- The **server** MUST be the ultimate source of trust.
- Client-side validation MUST **never** be relied upon for security (it is a UX aid only).
- Every external request MUST be treated as **untrusted input** (Zero Trust).
- Security decisions MUST never depend on client-provided state.

### Authentication & Authorization

- Every protected request MUST be **authenticated before authorization**.
- Every authorization decision MUST perform **ownership validation
  ** ([§5](#5-authorization), [BR-004](../00-product/SRS.md#5-business-rules)).
- Authorization MUST be enforced at the **business/service layer
  ** ([ARCHITECTURE §7.1](./ARCHITECTURE.md#71-standard-request)), not from URL structure.
- Security MUST **fail closed** whenever authorization cannot be determined.

### Sensitive Data

- Sensitive data MUST be **minimized** wherever practical (Privacy by Design).
- Secrets MUST **never** be logged ([§16](#16-logging-and-audit)).
- Passwords, refresh tokens, and API keys MUST never be exposed through APIs ([§12](#12-data-security)).
- AI prompts and confidential client documents MUST be handled according to the platform's privacy principles
  ([§10](#10-ai-security)).

### External Providers

- Every new AI, OCR, Storage, or third-party provider MUST undergo a **security review before adoption
  ** ([Security Review Process](#security-review-process)).
- External providers MUST receive only the **minimum data required
  ** ([NFR-018](../00-product/SRS.md#9-non-functional-requirements)).
- Provider credentials MUST remain **server-side** ([§13](#13-secrets-management)).
- Provider integrations MUST occur only through the **approved architectural abstraction layers**
  (ports/adapters, [ARCHITECTURE §10](./ARCHITECTURE.md#10-external-services)).

### Engineering Practices

- New features SHOULD include **threat modeling** before implementation.
- Security-sensitive architectural changes SHOULD result in an **ADR**.
- Security controls SHOULD be **reusable** rather than duplicated (a shared control is a consistently-correct control).
- Temporary security exceptions MUST be **documented and explicitly approved** — never silent.

These rules exist to **prevent security drift** as the platform evolves and to ensure **consistent engineering decisions
across all modules**. A control applied unevenly is a gap waiting to be found; binding every future change to the same
rules keeps the security posture uniform no matter which module grows next.

---

## 3. Threat Model

High-level threats, their risk, and architectural mitigations. This is a living model, expanded as the product grows.

| ID   | Threat                             | Description                                                    | Risk                                                 | Mitigation                                                                                                                                                                                                                                     |
|------|------------------------------------|----------------------------------------------------------------|------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| T-01 | **Account compromise**             | Attacker gains a user's account.                               | High — full access to that user's clients/documents. | Strong password hashing ([§6](#6-password-policy)); rate-limited login; non-revealing errors ([BR-020](../00-product/SRS.md#5-business-rules)); future MFA ([§19](#19-future-security-evolution)).                                             |
| T-02 | **Credential theft**               | Passwords stolen via phishing/reuse/leak.                      | High.                                                | BCrypt hashing; no plaintext storage ([BR-022](../00-product/SRS.md#5-business-rules)); HTTPS everywhere; encourage strong passwords.                                                                                                          |
| T-03 | **JWT / token theft**              | Access/refresh token stolen (XSS, interception).               | High.                                                | Short-lived access tokens; refresh rotation + revocation ([§7](#7-token-security)); HTTPS; security headers ([§14](#14-security-headers)); hashed refresh tokens at rest.                                                                      |
| T-04 | **File upload attacks**            | Malicious file (malware, oversized, spoofed type) uploaded.    | Medium–High.                                         | Type/size validation ([§9](#9-file-upload-security)); storage isolation; safe filename handling; future AV scanning.                                                                                                                           |
| T-05 | **Prompt injection**               | Document/user content manipulates AI into unintended behavior. | Medium–High.                                         | Prompt/context isolation; data minimization; grounding; human review ([§10](#10-ai-security)).                                                                                                                                                 |
| T-06 | **AI hallucination**               | AI produces plausible but false content.                       | Medium — professional harm if trusted blindly.       | Grounding in document content ([BR-030](../00-product/SRS.md#5-business-rules)); "not found" over fabrication ([BR-033](../00-product/SRS.md#5-business-rules)); mandatory human review ([BR-031/032](../00-product/SRS.md#5-business-rules)). |
| T-07 | **Unauthorized document access**   | User A reaches User B's data.                                  | High.                                                | Explicit ownership validation on every access ([§5](#5-authorization)); `404` for non-owned ([API_SPEC §2.4](./API_SPEC.md#24-status-codes)).                                                                                                  |
| T-08 | **Data leakage**                   | Sensitive data exposed via responses, logs, or errors.         | High.                                                | DTOs never expose internal fields; no secrets/PII in logs ([§16](#16-logging-and-audit)); RFC 7807 errors with no internals.                                                                                                                   |
| T-09 | **Injection attacks** (SQLi, etc.) | Malicious input alters queries/behavior.                       | High.                                                | Parameterized data access; strict input validation ([SRS §6](../00-product/SRS.md#6-validation-rules)); no string-built queries.                                                                                                               |
| T-10 | **Dependency vulnerabilities**     | Vulnerable third-party library exploited.                      | Medium.                                              | Dependency scanning and updates ([§17](#17-dependency-security)).                                                                                                                                                                              |
| T-11 | **Secrets exposure**               | Credentials leaked via source/logs/config.                     | High.                                                | Externalized secrets; never committed; not logged ([§13](#13-secrets-management)).                                                                                                                                                             |
| T-12 | **Denial of service**              | Resource exhaustion via floods or heavy AI/OCR jobs.           | Medium.                                              | Rate limiting ([§11](#11-api-security)); size limits; async processing seam; graceful degradation ([NFR-004](../00-product/SRS.md#9-non-functional-requirements)).                                                                             |

---

## 4. Authentication

LedgerAI authenticates with **JWT access tokens + refresh tokens
** ([PD-008](../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions),
[ADR-001](./decisions/ADR-001-Authentication-Strategy.md)). Architecture only — no framework config.

| Aspect                          | Requirement                                                                                                                                                                                                                             |
|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **JWT access tokens**           | Short-lived, stateless JWTs presented as `Authorization: Bearer <token>` ([API_SPEC §3](./API_SPEC.md#3-authentication)). Validated on every protected request.                                                                         |
| **Refresh tokens**              | Longer-lived; used only to obtain a new access token. Persisted **as a hash** ([DATABASE §5.9](./DATABASE.md#59-refreshtoken)); never stored raw.                                                                                       |
| **Token rotation**              | Each refresh MUST issue a new refresh token and revoke the prior one (rotation), limiting the window of a stolen token.                                                                                                                 |
| **Token expiration**            | Access tokens expire quickly; refresh tokens have a bounded lifetime ([FR-AUTH-004](../00-product/SRS.md#41-authentication-auth)). Exact durations are a security-tuning parameter set in configuration, not fixed product commitments. |
| **Session lifecycle**           | Session = a valid access token, renewable via refresh while the refresh token is valid ([§8](#8-session-security)).                                                                                                                     |
| **Logout**                      | Revokes the presented refresh token ([FR-AUTH-005](../00-product/SRS.md#41-authentication-auth)); idempotent.                                                                                                                           |
| **Password hashing**            | Passwords stored only as **BCrypt** hashes ([BR-022](../00-product/SRS.md#5-business-rules)); see [§6](#6-password-policy).                                                                                                             |
| **Password reset (future)**     | Not in MVP. A secure, token-based reset flow is a documented future addition ([§19](#19-future-security-evolution)).                                                                                                                    |
| **Email verification (future)** | Not in MVP. Reserved for future onboarding hardening.                                                                                                                                                                                   |

Authentication failures MUST be **non-revealing** ([BR-020](../00-product/SRS.md#5-business-rules)) and MUST fail
closed.

---

## 5. Authorization

Authorization is **ownership-based**, mirroring the data model's ownership hierarchy
([DATABASE §4](./DATABASE.md#4-entity-relationship-diagram)):

```txt
User ──owns──> Client ──owns──> Document ──owns──> { DocumentContent, AIRequest ──> AIOutput }
User ──owns──> Report (via Document) · Activity · RefreshToken
```

| Rule                                  | Requirement                                                                                                    |
|---------------------------------------|----------------------------------------------------------------------------------------------------------------|
| **User owns Clients**                 | A Client is accessible only to its owning User.                                                                |
| **Client owns Documents**             | A Document is reachable only through a Client the User owns ([BR-001](../00-product/SRS.md#5-business-rules)). |
| **Document owns AI artifacts**        | Summaries, chat, and extracted content inherit the Document's (and thus User's) ownership.                     |
| **Reports follow Document ownership** | A Report is accessible only to the User who owns its source Document.                                          |

**Ownership validation:** every protected operation MUST verify that the authenticated User owns the target resource
(and the full parent chain) **before** acting — enforced in the service layer
([ARCHITECTURE §7.1](./ARCHITECTURE.md#71-standard-request)), not merely at the URL. This is the primary confidentiality
control ([SG-2](#2-security-goals)) and MUST NOT be assumed from path structure alone.

**Why cross-user resources return `404` (not `403`):** returning `403 Forbidden` for a resource the caller does not own
would *confirm the resource exists*, leaking information (e.g., that a given document id is valid). Returning **`404 Not
Found`** reveals nothing about existence. Therefore, for resources the caller does not own, the API returns `404`;
`403` is reserved for cases where the resource's existence is already legitimately known to the caller
([API_SPEC §2.4](./API_SPEC.md#24-status-codes)). This is a deliberate confidentiality trade-off favoring
non-disclosure.

---

## 6. Password Policy

| Aspect                 | Policy                                                                                                                                                                                                                                                |
|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Minimum complexity** | Passwords MUST meet a defined strength policy (length + complexity). Exact thresholds are the [SRS VR-001 `[Assumption]`](../00-product/SRS.md#6-validation-rules) value, finalized in configuration — **not invented here** as a product commitment. |
| **Storage**            | Only a one-way hash is stored; the plaintext is never persisted, logged, or returned ([BR-022](../00-product/SRS.md#5-business-rules)).                                                                                                               |
| **Hashing**            | **BCrypt** with an appropriate work factor (adaptive cost tuned over time). BCrypt is chosen for its adaptive, salted design.                                                                                                                         |
| **Password reuse**     | LedgerAI does **not** reuse or transmit passwords across services; users SHOULD be encouraged toward unique passwords. Breach-list checking is a possible future enhancement.                                                                         |
| **Future MFA**         | Multi-factor authentication is **not** in the MVP; it is a prioritized future addition ([§19](#19-future-security-evolution)).                                                                                                                        |

---

## 7. Token Security

| Token                     | Handling                                                                                                                                                                    |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Access token**          | Short-lived JWT; carries only the minimal claims needed to identify the User and authorize requests. Never contains secrets or sensitive PII.                               |
| **Refresh token**         | Longer-lived; stored server-side **as a hash** ([DATABASE §5.9](./DATABASE.md#59-refreshtoken)) so a database read cannot yield usable tokens.                              |
| **Rotation**              | Every use of a refresh token issues a new one and revokes the old (one-time-use), so a captured refresh token is quickly invalidated.                                       |
| **Revocation**            | Refresh tokens are revoked on logout, on rotation, and MAY be revoked in bulk for a User if compromise is suspected.                                                        |
| **Expiration philosophy** | Short access-token lifetime limits stolen-token value; refresh rotation bounds long-term exposure. Balance usability against exposure; err toward shorter access lifetimes. |

**Storage recommendations (browser):** access tokens SHOULD be kept out of persistent, JS-readable storage where
practical; a refresh token, if browser-held, SHOULD be delivered as a secure, **httpOnly** cookie to mitigate XSS theft.
Exact transport (httpOnly cookie vs. body) is finalized alongside the frontend, consistent with
[API_SPEC §3](./API_SPEC.md#3-authentication). Because this is a token (not cookie-session) API, CSRF exposure is
limited (see [§15](#15-cors-and-csrf)).

---

## 8. Session Security

| Aspect                   | Requirement                                                                                                                                                                                 |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Login**                | Establishes a session by issuing access + refresh tokens on valid credentials ([FR-AUTH-002](../00-product/SRS.md#41-authentication-auth)).                                                 |
| **Logout**               | Revokes the refresh token, ending renewal; the short-lived access token expires shortly after ([FR-AUTH-005](../00-product/SRS.md#41-authentication-auth)).                                 |
| **Expiration**           | Sessions persist across reloads but expire after the defined access/refresh lifetimes ([FR-AUTH-003](../00-product/SRS.md#41-authentication-auth)).                                         |
| **Concurrent sessions**  | Multiple valid sessions/devices MAY exist (each with its own refresh token). This is acceptable for the single-professional MVP; per-session revocation is supported via the token records. |
| **Session invalidation** | Revoking refresh tokens invalidates the ability to renew; suspected compromise MAY trigger bulk revocation for the User.                                                                    |

Sessions are **stateless** at the access-token layer (no server session store), consistent with the architecture's
stateless-JWT stance ([ARCHITECTURE §9.1](./ARCHITECTURE.md#9-cross-cutting-concerns)).

---

## 9. File Upload Security

Uploads are a high-risk surface (T-04). Files are stored in the **external Storage Provider**, not in the database or on
the application host ([DATABASE §1.3](./DATABASE.md#13-related-documents)).

| Control                             | Requirement                                                                                                                                                                                                                                                                             |
|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **MIME validation**                 | The content type MUST be validated against an allow-list of supported types ([VR-005](../00-product/SRS.md#6-validation-rules)); do not trust the client-declared type alone.                                                                                                           |
| **Extension validation**            | The filename extension MUST be consistent with the allowed types; mismatches are rejected.                                                                                                                                                                                              |
| **Size validation**                 | Enforce a maximum size ([VR-005](../00-product/SRS.md#6-validation-rules)) before storing; oversized uploads are rejected ([API_SPEC §8.1](./API_SPEC.md#81-upload-document)).                                                                                                          |
| **Virus/malware scanning (future)** | Not in MVP; anti-malware scanning of uploads is a documented future enhancement ([§19](#19-future-security-evolution)).                                                                                                                                                                 |
| **Storage isolation**               | Stored files are owner-scoped and accessed only via short-lived, authorized references ([API_SPEC §8.5](./API_SPEC.md#85-download-metadata--access-link)); no public/enumerable URLs.                                                                                                   |
| **Filename handling**               | Original filenames are treated as untrusted data — stored as metadata, never used to build storage paths or executed; guards against **path traversal** ([NFR-009](../00-product/SRS.md#9-non-functional-requirements)). Internal storage keys are opaque, not derived from user input. |
| **Metadata validation**             | Uploaded metadata is validated and never reflected unsanitized.                                                                                                                                                                                                                         |

The concrete storage provider is a deferred decision ([DD-001](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions),
[ADR-002](./decisions/ADR-002-Storage-Provider.md)); these controls are provider-independent.

---

## 10. AI Security

**AI is the highest-nuance security surface in LedgerAI.** The platform sends document-derived content to an external AI
provider and returns AI output to professionals. The controls below are mandatory. See
[AI_ARCHITECTURE.md](./AI_ARCHITECTURE.md) for the provider boundary and mechanics.

| Concern                       | Requirement                                                                                                                                                                                                                                                                                      |
|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Prompt injection**          | Document content and user questions are **untrusted input**. The system MUST treat retrieved document text as data, not as instructions, and MUST NOT let it override system intent. Instructions and untrusted content are kept in distinct roles.                                              |
| **Prompt isolation**          | System instructions, user input, and document content occupy separate, clearly delimited channels so injected text cannot masquerade as system instruction.                                                                                                                                      |
| **Context isolation**         | Each AI request is scoped to a single owning User's document(s); one user's content MUST NOT leak into another user's AI context ([BR-004](../00-product/SRS.md#5-business-rules)). No cross-user context sharing.                                                                               |
| **Data minimization**         | Only the **minimum content necessary** for a request is sent to the provider ([NFR-018](../00-product/SRS.md#9-non-functional-requirements)); providers receive no standing access to user data.                                                                                                 |
| **Provider trust**            | The AI provider is a **least-privilege external dependency** reached through the domain's AI port ([ARCHITECTURE §10](./ARCHITECTURE.md#10-external-services)). No secrets or unrelated data are exposed to it.                                                                                  |
| **Hallucination mitigation**  | Outputs MUST be **grounded** in provided content ([BR-030](../00-product/SRS.md#5-business-rules)); when unsupported, the system says so rather than fabricating ([BR-033](../00-product/SRS.md#5-business-rules)).                                                                              |
| **Grounding**                 | AI responses are derived from the document's extracted text, and SHOULD indicate their basis where applicable ([FR-CHAT-002](../00-product/SRS.md#48-ai-chat-chat)).                                                                                                                             |
| **Human review**              | All AI output is **assistive, editable, and review-required** ([BR-031/032](../00-product/SRS.md#5-business-rules)); LedgerAI never treats AI output as authoritative or as a system of record, and never auto-acts (e.g., no auto-send email, [BR-034](../00-product/SRS.md#5-business-rules)). |
| **PII handling**              | Financial documents may contain PII; it is sent to the provider only as needed for the requested action, never logged, and subject to the same confidentiality controls as all client data.                                                                                                      |
| **Prompt logging philosophy** | Prompts and AI content MUST NOT be logged in plaintext as operational logs ([NFR-013](../00-product/SRS.md#9-non-functional-requirements)). AI *requests* are recorded as domain records (`AIRequest`) for lifecycle/audit, but sensitive content is not emitted to logs.                        |

---

## 11. API Security

Detailed contract in [API_SPEC.md](./API_SPEC.md); the security-relevant guarantees:

| Control                  | Requirement                                                                                                                                                                       |
|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **HTTPS**                | All traffic MUST be over HTTPS/TLS; no plaintext transport ([§12](#12-data-security)).                                                                                            |
| **Authorization header** | Protected endpoints require `Authorization: Bearer <accessToken>`; missing/invalid → `401`.                                                                                       |
| **Rate limiting**        | Authentication and AI/generation endpoints SHOULD be rate-limited; exceed → `429` ([API_SPEC §2.4](./API_SPEC.md#24-status-codes)).                                               |
| **Input validation**     | All external input is validated at the boundary ([SRS §6](../00-product/SRS.md#6-validation-rules)); invalid → `422` with field errors.                                           |
| **RFC 7807 errors**      | Errors use Problem Details with **no** stack traces or internals ([API_SPEC §2.12](./API_SPEC.md#212-error-model--rfc-7807-problem-details)) — avoids information leakage (T-08). |
| **Idempotency**          | Safe methods are idempotent; creates MAY use `Idempotency-Key` to prevent duplicates ([API_SPEC §2.10](./API_SPEC.md#210-idempotency)).                                           |
| **Ownership checks**     | Every data-bearing endpoint enforces ownership ([§5](#5-authorization)).                                                                                                          |

---

## 12. Data Security

| Aspect                         | Requirement                                                                                                                                                                                                                                              |
|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Encryption in transit**      | TLS/HTTPS for all client↔server and server↔provider traffic.                                                                                                                                                                                             |
| **Encryption at rest**         | Database and stored files SHOULD be encrypted at rest by the managed providers (PostgreSQL/Neon; Storage Provider). LedgerAI relies on provider-managed encryption for the MVP; customer-managed keys are future ([§19](#19-future-security-evolution)). |
| **Sensitive fields**           | `passwordHash`, refresh `token_hash`, and internal `storage_reference` are never returned by the API ([API_SPEC §17](./API_SPEC.md#17-common-dtos)).                                                                                                     |
| **Password hashes**            | BCrypt, one-way ([§6](#6-password-policy)).                                                                                                                                                                                                              |
| **Refresh token hashes**       | Refresh tokens stored hashed, never raw ([§7](#7-token-security)).                                                                                                                                                                                       |
| **Database backups**           | Backups (provider-managed) inherit the same confidentiality/encryption expectations; access to backups is restricted.                                                                                                                                    |
| **Secure deletion philosophy** | Deleting a Document removes it from all user surfaces and SHOULD promptly remove the external file ([DATABASE §12](./DATABASE.md#12-data-retention)); spent refresh tokens are purged. Retain for audit only what carries no undue privacy risk.         |

---

## 13. Secrets Management

| Rule                      | Requirement                                                                                                                                                                                    |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Environment variables** | All secrets (DB credentials, JWT signing keys, provider API keys) are provided via **environment/config**, never hard-coded ([ARCHITECTURE §9.6](./ARCHITECTURE.md#9-cross-cutting-concerns)). |
| **Never commit secrets**  | Secrets MUST NOT be committed to source control; the repository MUST ignore secret files; leaked secrets MUST be rotated immediately.                                                          |
| **Key rotation**          | Signing keys and provider credentials SHOULD be rotatable without code changes; rotation SHOULD be periodic and on suspected compromise.                                                       |
| **Provider credentials**  | AI/OCR/Storage credentials are held only by the server, scoped to least privilege, and never exposed to the browser or the AI provider.                                                        |
| **Local development**     | Developers use their own local/secret configuration; production secrets are never shared into development. No vendor-specific tooling is mandated here.                                        |

---

## 14. Security Headers

The application SHOULD return the following response headers (values tuned in configuration):

| Header                                | Purpose                                                                                             |
|---------------------------------------|-----------------------------------------------------------------------------------------------------|
| **Content-Security-Policy (CSP)**     | Restricts sources of scripts/styles/etc., mitigating XSS and injection (defense against T-03/T-08). |
| **Strict-Transport-Security (HSTS)**  | Forces HTTPS for future requests, preventing downgrade/interception.                                |
| **X-Content-Type-Options: nosniff**   | Stops MIME-type sniffing that can turn benign responses into executable content.                    |
| **Referrer-Policy**                   | Limits leakage of URLs (which may carry context) to third parties.                                  |
| **Permissions-Policy**                | Disables unneeded browser features (camera, geolocation, etc.), reducing surface.                   |
| **X-Frame-Options / frame-ancestors** | Prevents clickjacking by disallowing framing of the app.                                            |

These are recommendations at the architecture level; exact directives are set during implementation and deployment.

---

## 15. CORS and CSRF

| Aspect                                       | Stance                                                                                                                                                                                                                                                                                                                                                         |
|----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Allowed origins**                          | CORS MUST restrict access to the known frontend origin(s) only; wildcard origins with credentials are forbidden. Exact domains are set in configuration, not fixed here.                                                                                                                                                                                       |
| **Credentials**                              | If cookies are used for the refresh token, CORS MUST be configured to allow credentials only for the trusted origin.                                                                                                                                                                                                                                           |
| **CSRF philosophy**                          | A **token-based (Bearer) API is not automatically CSRF-vulnerable**, because the browser does not attach a Bearer token automatically the way it attaches cookies. If any credential is stored in a cookie (e.g., an httpOnly refresh cookie), CSRF defenses (e.g., same-site cookies, CSRF tokens on state-changing requests) MUST be applied to those flows. |
| **Why JWT APIs differ from session cookies** | Cookie-session APIs are ambient-authority (the browser sends the cookie on any request to the origin), which is the root of CSRF. Bearer tokens require explicit attachment by the client, removing the ambient-authority vector for token-only endpoints.                                                                                                     |

---

## 16. Logging and Audit

Two distinct streams: **operational logs** (diagnostics) and the **Activity audit trail** (user-facing, immutable,
[DATABASE §5.8](./DATABASE.md#58-activity)).

**Security-relevant events that SHOULD be recorded** (audit/operational as appropriate):

- Authentication events: login success/failure, logout.
- Failed login attempts (for rate-limiting/abuse detection).
- Token issuance, refresh, and **revocation**.
- AI *requests* (as `AIRequest` domain records — lifecycle/audit, not prompt contents).
- Significant user actions on the [Activity timeline](./API_SPEC.md#15-activity-module-activities) (uploads,
  generations, deletions).
- Administrative events — **future** (no admin surface in MVP).

**What MUST NOT be logged** ([NFR-013](../00-product/SRS.md#9-non-functional-requirements)):

- Passwords (plaintext or hash), or any credential.
- Access or refresh tokens (or their raw values).
- Document contents, extracted text, or AI prompt/response **content**.
- Secrets, API keys, or PII beyond the minimum needed to identify an event.

Every log/audit entry SHOULD carry a correlation id (
`traceId`, [API_SPEC §2.12](./API_SPEC.md#212-error-model--rfc-7807-problem-details))
to support investigation without exposing sensitive content.

---

## 17. Dependency Security

| Practice                         | Requirement                                                                                                                                                                                      |
|----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Dependency updates**           | Dependencies SHOULD be kept current; security patches applied promptly.                                                                                                                          |
| **Known-vulnerability scanning** | The dependency graph (backend and frontend) SHOULD be scanned for known CVEs regularly and before releases.                                                                                      |
| **Supply-chain awareness**       | New dependencies SHOULD be vetted (maintenance, provenance, necessity); minimize count to reduce surface, consistent with the "no unnecessary frameworks" stance ([CLAUDE.md](../../CLAUDE.md)). |
| **Version pinning**              | Dependency versions SHOULD be pinned/locked for reproducible builds; updates are deliberate and reviewed, not implicit.                                                                          |

---

## 18. Incident Response

High-level lifecycle only (no operational runbooks):

| Phase                    | Intent                                                                                                                                                                   |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Detection**            | Notice anomalies via logs, audit trail, error/observability signals, and abuse indicators ([§16](#16-logging-and-audit)).                                                |
| **Containment**          | Limit impact — e.g., revoke affected tokens/sessions, rotate compromised secrets, disable an affected path.                                                              |
| **Recovery**             | Restore normal service, validate integrity, and confirm the vector is closed.                                                                                            |
| **Post-incident review** | Document the timeline and root cause; feed lessons into controls, the threat model ([§3](#3-threat-model)), and [LESSONS_LEARNED](../03-engineering/LESSONS_LEARNED.md). |

A formal, detailed incident-response process is a future operational addition; this section fixes the intent.

---

## Security Review Process

Security is evaluated continuously, not once. The following changes **trigger a security review** before they ship; the
review applies the [Security Design Rules](#security-design-rules) and updates the [Threat Model §3](#3-threat-model) as
needed.

### Architecture Changes — review required for

- Authentication architecture changes.
- Authorization model changes.
- New trust boundaries.
- Module ownership changes.

### Infrastructure Changes — review required for

- New external providers.
- Storage architecture changes.
- Secrets management changes.
- Deployment architecture changes.

### Data Changes — review required for

- Database schema changes involving sensitive data.
- Introduction of new personal or financial information.
- Data retention policy changes.
- Encryption strategy changes.

### API Changes — review required for

- Authentication endpoints.
- Authorization behavior.
- Public API additions.
- Breaking API changes.

### AI Changes — review required for

- Prompt architecture.
- Grounding strategy.
- New models.
- Provider changes.
- Context handling.
- AI safety controls.

### Review Outcomes

A security review SHOULD:

- **Identify new threats** introduced by the change.
- **Assess existing mitigations** for adequacy against those threats.
- **Recommend additional controls** where appropriate.
- **Produce an ADR** when an architectural security decision changes.
- **Track significant findings** until they are resolved.

Security is a **continuous engineering discipline**, not a one-time design activity. Reviews are expected to accompany
meaningful architectural evolution, so that the platform's security posture keeps pace with what it is being asked to
protect.

---

## 19. Future Security Evolution

**Non-MVP.** Recorded so they can be added additively without re-architecture; none are built now
([boundaries](../00-product/PRODUCT_DECISIONS.md#2-product-boundaries)).

| Future capability                    | Note                                                                                                                |
|--------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| **MFA**                              | Second factor at login.                                                                                             |
| **Passkeys / WebAuthn**              | Passwordless, phishing-resistant authentication.                                                                    |
| **SSO**                              | Enterprise identity federation (OAuth/OIDC/SAML).                                                                   |
| **Enterprise RBAC**                  | Roles/permissions for teams (extends the ownership model, [§5](#5-authorization)).                                  |
| **Audit exports**                    | Exportable audit trails for compliance.                                                                             |
| **Customer-managed encryption keys** | Tenant-controlled keys for at-rest encryption.                                                                      |
| **Compliance certifications**        | e.g., SOC 2 and similar, as the business requires ([SG-8](#2-security-goals)).                                      |
| **Regional data residency**          | Storing data in specific regions/jurisdictions ([DD-005](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)). |
| **Anti-malware upload scanning**     | Scanning uploads for malicious content ([§9](#9-file-upload-security)).                                             |
| **Automated key rotation**           | Scheduled rotation of signing keys/credentials.                                                                     |

---

## 20. Security Decision Summary

| Decision                  | Chosen Approach                                  | Alternatives                              | Rationale                                                                                                                         | Related ADR                                               |
|---------------------------|--------------------------------------------------|-------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| **Authentication**        | Stateless **JWT access tokens**                  | Server-side sessions                      | Stateless scaling across the Vercel/Render split; PD-008 ([§4](#4-authentication)).                                               | [ADR-001](./decisions/ADR-001-Authentication-Strategy.md) |
| **Session renewal**       | **Refresh tokens** with rotation                 | Long-lived access tokens; sticky sessions | Short access lifetime + bounded refresh exposure ([§7](#7-token-security)).                                                       | ADR-001                                                   |
| **Authorization**         | **Ownership-based** checks in the service layer  | Role-only checks; URL-trust               | Per-user isolation is the core confidentiality control ([§5](#5-authorization), [BR-004](../00-product/SRS.md#5-business-rules)). | —                                                         |
| **Non-owned resources**   | Return **`404`** (not `403`)                     | `403 Forbidden`                           | Avoids leaking resource existence ([§5](#5-authorization)).                                                                       | —                                                         |
| **Password hashing**      | **BCrypt** (salted, adaptive)                    | Plaintext; fast hashes (SHA-only)         | Adaptive cost resists brute force ([§6](#6-password-policy), [BR-022](../00-product/SRS.md#5-business-rules)).                    | —                                                         |
| **Transport**             | **HTTPS only**                                   | Mixed/plaintext                           | Confidentiality/integrity in transit ([§12](#12-data-security)).                                                                  | —                                                         |
| **File storage**          | **External object storage**, DB stores reference | Store bytes in DB; on-host files          | Isolation, reduced surface, no DB bloat ([§9](#9-file-upload-security), [DATABASE §1.3](./DATABASE.md#13-related-documents)).     | [ADR-002](./decisions/ADR-002-Storage-Provider.md)        |
| **AI trust model**        | **Human-reviewed**, assistive AI                 | AI as authority / auto-act                | Trust, accuracy, boundary preservation ([§10](#10-ai-security), [BR-031/032](../00-product/SRS.md#5-business-rules)).             | —                                                         |
| **AI grounding**          | **Grounded** responses; decline over fabricate   | Ungrounded generation                     | Reduces hallucination harm ([§10](#10-ai-security), [BR-030/033](../00-product/SRS.md#5-business-rules)).                         | —                                                         |
| **Error model**           | **RFC 7807**, no internals                       | Verbose/stack-trace errors                | Consistent, non-leaking errors ([§11](#11-api-security)).                                                                         | —                                                         |
| **Refresh token storage** | Store **hash** + expiry + revocation             | Store raw token; stateless-only           | DB read cannot yield usable tokens; enables revocation ([§7](#7-token-security), [DATABASE §5.9](./DATABASE.md#59-refreshtoken)). | ADR-001                                                   |

---

*This is the authoritative security reference for the LedgerAI MVP — architecture and guarantees, not framework
configuration. It MUST remain consistent with the frozen Product Vision, Product Decisions, PRD, SRS, Architecture,
Database, and API Spec. Major decisions are summarized in [§20](#20-security-decision-summary) and formalized as ADRs;
AI-specific mechanics are elaborated in [AI_ARCHITECTURE.md](./AI_ARCHITECTURE.md).*
