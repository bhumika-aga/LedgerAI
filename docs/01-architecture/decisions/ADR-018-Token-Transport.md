# ADR-018 — Refresh-Token Transport

**Status:** Accepted **Date:** 2026-07-17 **Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [SECURITY §7 (Token Security)](../SECURITY.md#7-token-security) · [SECURITY §8 (Session Security)](../SECURITY.md#8-session-security) · [SECURITY §15 (CORS and CSRF)](../SECURITY.md#15-cors-and-csrf) · [API_SPEC §3 (Authentication)](../API_SPEC.md#3-authentication) · [API_SPEC §17.2 (AuthTokens)](../API_SPEC.md#17-common-dtos) · [ADR-001 (Authentication Strategy)](./ADR-001-Authentication-Strategy.md)

---

## Context

[SECURITY §7](../SECURITY.md#7-token-security) fixes the token model (short-lived Bearer access JWT; long-lived, hashed,
rotating refresh token) but explicitly leaves **one detail open**: "Exact transport (httpOnly cookie vs. body)
is finalized alongside the frontend." [AuthTokens](../API_SPEC.md#17-common-dtos) encodes both forms — its
`refreshToken` field is "present only when not delivered via cookie." The choice determines the login/refresh response
shape, how the browser stores the refresh credential, whether a session survives a full page reload
([SECURITY §8](../SECURITY.md#8-session-security)), and whether CORS-credentials and CSRF defenses are required
([SECURITY §15](../SECURITY.md#15-cors-and-csrf)). It is a security-architecture decision that must be made before the
authentication slice can take shape, so it is recorded here rather than invented in code.

---

## Decision

Deliver the **refresh token as a secure, httpOnly cookie**; deliver the **access token in the response body**.

- **Access token** — returned in the JSON body as `AuthTokens { accessToken, tokenType: "Bearer", expiresIn }` and held
  only in memory by the frontend; never written to persistent, JS-readable storage
  ([SECURITY §7](../SECURITY.md#7-token-security)). `refreshToken` is therefore omitted from the body.
- **Refresh token** — set by the server as a cookie that is `HttpOnly`, `Secure`, and `SameSite`, scoped by `Path` to
  the authentication endpoints. It is never readable by JavaScript, mitigating XSS theft. Its raw value is only ever in
  the cookie; the server stores only its hash ([DATABASE §5.9](../DATABASE.md#59-refreshtoken)).
- **Refresh / session restoration** — on load, or when the access token has expired, the SPA calls
  `POST /auth/refresh`; the browser attaches the cookie automatically, the server rotates the refresh token (issuing a
  new cookie) and returns a fresh access token, satisfying seamless renewal and cross-reload persistence
  ([SECURITY §8](../SECURITY.md#8-session-security)).
- **CSRF** — the refresh cookie uses `SameSite` as its primary CSRF defense
  ([SECURITY §15](../SECURITY.md#15-cors-and-csrf)); all other endpoints are Bearer-only and carry no ambient cookie
  authority. **CORS** allows credentials for the configured trusted origin (s) only; wildcard-with-credentials is
  forbidden.
- Cookie attributes (`Secure`, `SameSite`, `Path`, name), token lifetimes, the JWT signing secret, and allowed origins
  are **configuration**, not code constants ([SECURITY §13](../SECURITY.md#13-secrets-management)).

This ADR selects the transport only; it invents no claims, endpoints, or schema beyond what SECURITY.md, API_SPEC.md,
and DATABASE.md already define.

---

## Alternatives Considered

- **Both tokens in the response body (Bearer-only, no cookie).** Simpler and CSRF-free, but a browser-held refresh token
  then lives in JS-readable storage — contrary to the [SECURITY §7](../SECURITY.md#7-token-security)
  recommendation — or is lost on reload, failing the cross-reload persistence
  of [SECURITY §8](../SECURITY.md#8-session-security). Rejected as the weaker security posture for the product's
  confidentiality promise.
- **Both tokens in httpOnly cookies (cookie-session style).** Would make every request ambient-authority and reopen
  broad CSRF exposure ([SECURITY §15](../SECURITY.md#15-cors-and-csrf)); contrary to the stateless-Bearer stance of
  [ARCHITECTURE §9.1](../ARCHITECTURE.md#9-cross-cutting-concerns). Rejected.

---

## Consequences

### Advantages

- The refresh credential is unreadable by JavaScript (XSS-resistant) while the session survives reloads via rotation.
- Protected endpoints stay stateless and Bearer-only, so they carry no CSRF exposure.
- Matches the AuthTokens contract's cookie form exactly; no new API shape is introduced.

### Disadvantages

- A split-origin deployment (frontend and backend on different registrable domains) requires `SameSite=None; Secure`
  for the cookie to be sent cross-site, which weakens the SameSite CSRF defense and then requires an explicit CSRF-token
  defense on the cookie'd flows. Exact cookie attributes are deployment-tuned configuration; the same-origin/proxy
  development setup uses a stricter `SameSite`.

### Trade-offs

- We accept cookie-handling and CORS-credentials configuration in exchange for keeping the long-lived credential out of
  JavaScript's reach — the right trade for a product whose core promise is confidentiality.

---

## References

[SECURITY.md](../SECURITY.md) · [API_SPEC.md](../API_SPEC.md) · [DATABASE.md](../DATABASE.md) · [ADR-001](./ADR-001-Authentication-Strategy.md)
