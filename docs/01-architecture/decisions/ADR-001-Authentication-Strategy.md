# ADR-001 — Authentication Strategy (JWT + Refresh Tokens)

**Status:** Accepted **Date:** 2026-07-14 **Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [PRODUCT_DECISIONS PD-008](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions) · [SRS §4.1](../../00-product/SRS.md#41-authentication-auth) · [SECURITY §4](../SECURITY.md#4-authentication) · [ARCHITECTURE §4.1](../ARCHITECTURE.md#41-authentication-flow-high-level) · [API_SPEC §3](../API_SPEC.md#3-authentication)

---

## Context

LedgerAI protects confidential client financial documents, so every request must be authenticated. The backend (Render)
and frontend (Vercel) are deployed separately, and the architecture is a modular monolith that must scale without
sticky, server-held session state. We need an authentication mechanism that is stateless at request time, works cleanly
across the cross-origin split, and supports revocation.

---

## Decision

Use **JWT access tokens plus refresh tokens**. A short-lived, stateless access token is presented as
`Authorization: Bearer <token>` on every protected request and validated by a single security filter. A longer-lived
refresh token obtains new access tokens; it is **rotated** on every use and stored server-side **as a hash** to enable
revocation and cleanup ([DATABASE §5.9](../DATABASE.md#59-refreshtoken)).

---

## Alternatives Considered

- **Server-side sessions (cookie + session store).** Rejected: introduces stateful, shared session storage that
  complicates horizontal scaling and the Vercel/Render split, and adds infrastructure cost against the free-tier goal.
- **Access tokens only (no refresh).** Rejected: forces a bad trade — either long-lived access tokens (large theft
  window) or frequent re-login (poor UX). Refresh tokens decouple session length from access-token exposure.
- **Third-party managed auth (external IdP/SSO).** Rejected for MVP: adds an external dependency and cost, and exceeds
  MVP needs (single professional per account). Reserved as a future option
  ([SECURITY §19](../SECURITY.md#19-future-security-evolution)).

---

## Consequences

### Advantages

- Stateless access validation scales trivially across instances and the deployment split.
- Short access-token lifetime bounds the value of a stolen token; refresh rotation bounds long-term exposure.
- Revocation is possible via the hashed refresh-token records.

### Disadvantages

- Access tokens cannot be individually revoked before expiry (mitigated by short lifetimes).
- Correct token handling (storage, rotation, transport) is subtle and must be implemented carefully.

### Trade-offs

- We accept limited access-token revocability in exchange for statelessness and scale. Refresh-token rotation + hashing
  restores revocation where it matters most.

---

## Future Reconsideration

Revisit if: we introduce teams/enterprise SSO, require immediate global session revocation, adopt passkeys/WebAuthn, or
move to an architecture where a managed auth provider becomes clearly cheaper/safer. Token transport specifics (httpOnly
cookie vs. body) are finalized with the frontend and do not require a new ADR.

---

## References

[PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md) · [SRS](../../00-product/SRS.md) · [SECURITY](../SECURITY.md) · [API_SPEC](../API_SPEC.md) · [DATABASE](../DATABASE.md)
