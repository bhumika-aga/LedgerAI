# ADR-011 — API Versioning (URI Path)

**Status:** Accepted **Date:** 2026-07-14 **Owner:** Founding Engineer / Principal API Architect
**Related Documents:
** [API_SPEC §2.1](../API_SPEC.md#21-base-url) · [API_SPEC §20](../API_SPEC.md#20-api-versioning-strategy) · [API_SPEC §API Lifecycle](../API_SPEC.md#api-lifecycle)

---

## Context

The React frontend and, eventually, third-party clients consume LedgerAI's REST API. The API will evolve over the
product's lifetime; we need a versioning approach that is discoverable, keeps clients reliable across releases, and
clearly separates non-breaking additions from breaking changes.

---

## Decision

Version the API in the **URI path**: the base URL is `/api/v1`. A version number increments **only** on a breaking
change; additive, backward-compatible changes (new endpoints, new optional fields, new enum values) ship within the same
version. Removal of an endpoint occurs only in a new major version after a deprecation window
([API_SPEC §20](../API_SPEC.md#20-api-versioning-strategy), [API Lifecycle](../API_SPEC.md#api-lifecycle)).

---

## Alternatives Considered

- **Header/media-type versioning (`Accept: application/vnd.ledgerai.v1+json`).** Rejected: less discoverable, harder to
  test by hand and to read in logs/caches; more ceremony for no MVP benefit.
- **Query-parameter versioning (`?version=1`).** Rejected: easy to omit, muddies caching, and blurs the resource
  identity.
- **No versioning.** Rejected: any future breaking change would silently break clients; unacceptable for a contract the
  frontend and future integrations depend on.

---

## Consequences

### Advantages

- Explicit, discoverable, cache- and log-friendly.
- Clear separation of additive vs. breaking change; clients pin to a stable base path.
- Simple mental model for both teams.

### Disadvantages

- A future major version means maintaining more than one path for a deprecation window.
- URI versioning is coarse-grained (whole-API), not per-resource.

### Trade-offs

- We accept coarse, whole-API versioning for maximum clarity and discoverability over the finer granularity of
  media-type versioning, which the MVP does not need.

---

## Future Reconsideration

Revisit only if the API grows to need independently-versioned resources or a public developer platform with strict
contract guarantees warrants media-type versioning. Introducing `/api/v2` follows the deprecation/sunset policy already
defined; it does not require replacing this ADR unless the versioning *mechanism* itself changes.

---

## References

[API_SPEC](../API_SPEC.md) · [DATABASE §Migration Strategy](../DATABASE.md#database-migration-strategy) · [SRS §14](../../00-product/SRS.md#14-requirement-versioning)
