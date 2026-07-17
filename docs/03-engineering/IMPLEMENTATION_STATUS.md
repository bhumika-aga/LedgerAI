# Implementation Status — LedgerAI MVP

> **Status:** Living document (engineering dashboard)
> **Owner:** Founding Engineer / Engineering Manager
> **Last updated:** 2026-07-17
> **Companion to:
> ** [IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md)

---

## 1. Purpose

This is the **live engineering dashboard** for LedgerAI. It tracks *what is currently happening* during implementation —
progress, blockers, decisions, and risks as they evolve.

> **[IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md) defines the execution strategy.**
> **IMPLEMENTATION_STATUS records the current execution state.**

| Document                                           | Role                                                                                                                       |
|----------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) | *Stable.* How LedgerAI should be built — phases, milestones, rules, Definition of Done. This document does not restate it. |
| **IMPLEMENTATION_STATUS.md** (this doc)            | *Living.* The current state of execution against the plan — the day-to-day dashboard.                                      |

It is operational, not architectural: it points at the plan and the frozen docs rather than duplicating them.

---

## 2. Project Summary

| Field                 | Value                       |
|-----------------------|-----------------------------|
| **Current Phase**     | Phase 2 — Client Management |
| **Current Milestone** | M2 — First document upload  |
| **Overall Progress**  | ~30%                        |
| **Current Sprint**    | Sprint 1                    |
| **Status**            | In Progress                 |
| **Last Updated**      | 2026-07-17                  |

---

## 3. Phase Tracker

Phases per [IMPLEMENTATION_PLAN §3](./IMPLEMENTATION_PLAN.md#3-build-order). Status ∈ {Not Started, In Progress,
Blocked, Completed}.

| Phase                               | Status      | Progress | Notes                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|-------------------------------------|-------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Phase 0 — Foundation                | In Progress | ~60%     | Done: backend + frontend scaffolds, CI, persistence infra (Flyway + Testcontainers), System Health slice. Remaining: Docker for local dev, env/secrets, OpenAPI wiring, live Neon connectivity.                                                                                                                                                                                                                                                         |
| Phase 1 — Authentication & Profile  | In Progress | ~80%     | Done: authentication slice — register/login/refresh/logout/me, JWT access + rotated refresh (httpOnly cookie, ADR-018), BCrypt, RFC 7807 errors, security config; frontend sign-in + session bootstrap; authorization foundation (ownership guard + principal abstraction); User Profile slice — `GET`/`PATCH /users/me` + profile page. Remaining: protected-route guard, preferences UI (needs documented keys), rate limiting (FR-AUTH-008, SHOULD). |
| Phase 2 — Client Management         | In Progress | ~60%     | Done: Client Management slice — `GET`/`POST`/`PATCH`/`DELETE /clients`, owner-scoped list with pagination/filter, soft-delete archive (DATABASE §8), VR-004; frontend list/detail/create/edit + route guard. Remaining: Document Upload (Phase 3 gate).                                                                                                                                                                                                 |
| Phase 3 — Documents, Storage & OCR  | Not Started | 0%       | Blocked-until: [ADR-002](../01-architecture/decisions/ADR-002-Storage-Provider.md) + OCR provider resolved.                                                                                                                                                                                                                                                                                                                                             |
| Phase 4 — AI Capabilities           | Not Started | 0%       | Blocked-until: AI provider (DD-002) resolved.                                                                                                                                                                                                                                                                                                                                                                                                           |
| Phase 5 — Search, Timeline & Launch | Not Started | 0%       | Search, timeline, polish, deploy.                                                                                                                                                                                                                                                                                                                                                                                                                       |

---

## 4. Module Tracker

All twelve MVP modules. Status ∈ {Not Started, In Progress, Blocked, Completed}.

| Module            | Phase | Status      | Depends On                          | Notes                                                                                                                                                                                         |
|-------------------|-------|-------------|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Authentication    | 1     | In Progress | Phase 0                             | Register/login/refresh/logout/me implemented + tested; rate limiting (FR-AUTH-008) deferred.                                                                                                  |
| User Profile      | 1     | In Progress | Authentication                      | `GET`/`PATCH /users/me` implemented + tested; isolation is structural (no `/users/{id}`). Preferences UI deferred — no document defines preference keys (FR-PROF-005, SHOULD).                |
| Client Management | 2     | In Progress | Authentication                      | Five documented endpoints implemented + tested; ownership via shared `OwnershipGuard`; archive is soft (status=ARCHIVED). Client-workspace child views (documents/activity) are later slices. |
| Document Upload   | 3     | Not Started | Client Management, Document Storage | Entry point of the core loop.                                                                                                                                                                 |
| Document Storage  | 3     | Not Started | Document Upload, ADR-002            | External object store; DB holds reference.                                                                                                                                                    |
| OCR               | 3     | Not Started | Document Storage, OCR provider      | Native-first; drives Ready/Failed.                                                                                                                                                            |
| AI Summary        | 4     | Not Started | OCR, AI provider                    | Grounded, editable.                                                                                                                                                                           |
| AI Chat           | 4     | Not Started | OCR, AI provider                    | Document-scoped Q&A.                                                                                                                                                                          |
| AI Email          | 4     | Not Started | AI provider                         | Draft only — never sent.                                                                                                                                                                      |
| Report Generation | 4     | Not Started | AI Summary                          | Single-document.                                                                                                                                                                              |
| Global Search     | 5     | Not Started | Document Storage, OCR               | Owner-scoped; excludes deleted.                                                                                                                                                               |
| Activity Timeline | 5     | Not Started | All action-producing modules        | Read-only, immutable.                                                                                                                                                                         |

---

## 5. Deferred Decisions Tracker

Unresolved architectural decisions gating downstream phases.
See [IMPLEMENTATION_PLAN §3](./IMPLEMENTATION_PLAN.md#3-build-order).

| Decision                                                                                                 | Current Status | Required Before               | Owner | Notes                                                                                                                  |
|----------------------------------------------------------------------------------------------------------|----------------|-------------------------------|-------|------------------------------------------------------------------------------------------------------------------------|
| [ADR-002 Storage Provider](../01-architecture/decisions/ADR-002-Storage-Provider.md) (DD-001)            | Deferred       | Phase 3 (Document Storage)    | —     | Cloudinary vs. Supabase; evidence-based comparison + security review.                                                  |
| AI Provider (DD-002)                                                                                     | Deferred       | Phase 4 (AI Summary)          | —     | Behind AI port ([ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md)); benchmark before rollout. |
| OCR Provider (DD-002)                                                                                    | Deferred       | Phase 3 (OCR)                 | —     | Behind OCR port ([ADR-009](../01-architecture/decisions/ADR-009-OCR-Strategy.md)); may combine with AI provider.       |
| Background Processing ([ADR-013](../01-architecture/decisions/ADR-013-Background-Processing.md), DD-007) | Deferred       | When scale demands (post-MVP) | —     | Async-ready seam already in place; MVP MAY run sync-with-status.                                                       |

---

## 6. Milestone Tracker

Milestones per [IMPLEMENTATION_PLAN §4](./IMPLEMENTATION_PLAN.md#4-milestones). Status ∈ {Not Started, In Progress,
Completed}.

| Milestone                    | Status      | Completed Date | Notes                                                      |
|------------------------------|-------------|----------------|------------------------------------------------------------|
| M1 — Authentication complete | Not Started | —              | Register/login/refresh/logout; profile; protected routing. |
| M2 — First document upload   | Not Started | —              | Client + upload + storage.                                 |
| M3 — OCR working             | Not Started | —              | Extraction reaches Ready; failures surface.                |
| M4 — First AI summary        | Not Started | —              | Grounded, editable summary.                                |
| M5 — First AI chat           | Not Started | —              | Grounded Q&A; honest "not found."                          |
| M6 — Beta ready              | Not Started | —              | All modules meet Definition of Done; deployed.             |

---

## 7. Current Sprint

**Sprint 1**

| Section               | Entries                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Objective**         | Establish the foundation and validate the full stack end to end.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| **Planned Tasks**     | Docker for local dev; env/secrets; OpenAPI wiring; live Neon connectivity.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| **Completed Tasks**   | Backend scaffold; frontend scaffold; CI (build/test/docs); persistence infra (ADR-016/017); System Health validation slice; Authentication slice (register/login/refresh/logout/me); authorization foundation (principal abstraction, ownership guard, 401/403/404 mapping); User Profile slice (`GET`/`PATCH /users/me` + profile page). Client Management slice (`/clients` CRUD + archive, list/detail/create/edit UI, route guard).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| **Blocked Tasks**     | *(none yet)*                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| **Engineering Notes** | System Health slice uses the existing Actuator `/actuator/health` (ADR-015); no first-party health API endpoint was added. Authentication follows ADR-018 (refresh token in httpOnly cookie, access token in body); logout requires the access token per API_SPEC §5.4, refresh does not per §5.3. Login rate limiting (FR-AUTH-008, `429`) is a SHOULD and is deferred to a hardening pass. The authorization foundation is reusable infrastructure only — ownership is enforced in the service layer via a shared guard (SECURITY §5, BR-004), non-owned resources answer `404` (never `403`), and no role/permission model was introduced (RBAC remains future per SECURITY §19). No method-security annotations were added: no frozen document specifies them, and ownership is not expressible as a role check. User Profile: the `user` aggregate stays in `auth` and the `users` module reaches it through a published, DTO-only `UserAccountService` (ARCHITECTURE §5.1 forbids cross-module internals access). No migration was needed — V2 already created `professional_details` and `preferences`. `UserResponse` gained the `professionalDetails`/`preferences` fields API_SPEC §17.1 already specified (additive, non-breaking per §20). VR-003 lengths are externalized as `users.profile.*` because the SRS marks them [Assumption] and they are not finalized; `preferences` is stored as an opaque jsonb blob since no document defines its keys. |

---

## 8. Technical Debt

Known shortcuts/issues to revisit. *(Empty — no debt recorded yet.)*

| Issue    | Impact | Priority | Resolution |
|----------|--------|----------|------------|
| *(none)* |        |          |            |

---

## 9. Active Risks

Seeded from [IMPLEMENTATION_PLAN §8](./IMPLEMENTATION_PLAN.md#8-risks). Status ∈ {Open, Monitoring, Mitigated, Closed}.

| Risk                                     | Impact                         | Mitigation                                                                                                                            | Status |
|------------------------------------------|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|--------|
| AI provider selection/integration delays | Blocks Phase 4                 | AI port keeps Phases 0–3 provider-agnostic; resolve DD-002 with benchmarking before Phase 4                                           | Open   |
| OCR complexity / quality                 | Weakens all AI                 | Native-first ([ADR-009](../01-architecture/decisions/ADR-009-OCR-Strategy.md)); Failed state + quality signals; test real scans early | Open   |
| Scope creep                              | Dilutes MVP, delays launch     | "Never implement undocumented features"; product boundaries + decision gate                                                           | Open   |
| Authentication bugs                      | Security-critical              | Build auth first with security tests; follow ADR-001/SECURITY §4; security review                                                     | Open   |
| Free-tier limits (cold starts, quotas)   | Latency/capacity ceilings      | Lightweight monolith; async-ready; vertical-then-extract scaling; baseline observability                                              | Open   |
| Storage/DB divergence (orphaned files)   | Cost, inconsistency            | Compensating cleanup on failed upload/delete; future reconciliation job                                                               | Open   |
| Big-PR integration risk                  | Hidden defects, stalled review | Vertical slices + small PRs + always-green main                                                                                       | Open   |

---

## 10. Decision Log

Chronological record of decisions made during implementation. *(Empty — no in-flight decisions logged yet.)*

| Date       | Decision                                                          | Reference                                                                     |
|------------|-------------------------------------------------------------------|-------------------------------------------------------------------------------|
| 2026-07-16 | Adopted Flyway as the database migration tool                     | [ADR-016](../01-architecture/decisions/ADR-016-Database-Migration-Tooling.md) |
| 2026-07-16 | Adopted Testcontainers as the integration-test database mechanism | [ADR-017](../01-architecture/decisions/ADR-017-Integration-Test-Database.md)  |
| 2026-07-17 | Refresh token in httpOnly cookie, access token in response body   | [ADR-018](../01-architecture/decisions/ADR-018-Token-Transport.md)            |

---

## 11. Weekly Progress Log

*(Empty — add one row per week as work begins.)*

| Week     | Achievements | Challenges | Next Goals |
|----------|--------------|------------|------------|
| *(none)* |              |            |            |

---

## 12. Completion Checklist

MVP completeness against [IMPLEMENTATION_PLAN §10](./IMPLEMENTATION_PLAN.md#10-success-criteria). All unchecked at
start.

- [ ] Authentication
- [ ] Profile
- [ ] Clients
- [ ] Upload
- [ ] Storage
- [ ] OCR
- [ ] Summary
- [ ] Chat
- [ ] Email
- [ ] Reports
- [ ] Search
- [ ] Timeline
- [ ] Deployment
- [ ] Testing
- [ ] Documentation

---

## 13. Update Rules

This document is **living** and MUST always reflect the current implementation state. Update it:

- **After every completed milestone** — mark the milestone Completed with its date; advance Current Milestone (§2, §6).
- **After every sprint** — roll the Current Sprint (§7) and add a Weekly Progress Log entry (§11).
- **After every ADR** — record it in the Decision Log (§10); update the Deferred Decisions Tracker (§5) if it resolves
  one.
- **After every major feature** — update the Module (§4) and Phase (§3) trackers and the Completion Checklist (§12).
- **After every production deployment** — update Project Summary (§2) and the relevant trackers.

Whenever any of the above happens, also refresh **Last Updated** and **Overall Progress**
in [Project Summary §2](#2-project-summary).

> This dashboard is expected to change constantly. If it ever disagrees with reality, **reality is right and the
> dashboard is stale** — fix it. It never overrides the frozen documents or
> the [IMPLEMENTATION_PLAN](./IMPLEMENTATION_PLAN.md);
> it only reports progress against them.
