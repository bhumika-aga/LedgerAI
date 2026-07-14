# ADR-008 — Object Storage for Documents

**Status:** Accepted
**Date:** 2026-07-14
**Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [DATABASE §1.3](../DATABASE.md#13-related-documents) · [DATABASE §14](../DATABASE.md#14-risks) · [SECURITY §9](../SECURITY.md#9-file-upload-security) · [ADR-002 (provider)](./ADR-002-Storage-Provider.md)

---

## Context

LedgerAI ingests financial documents (PDFs, scans, images) that can be large. We must decide **where the file bytes
live** and how the application references them. This is distinct from *which vendor* provides storage — that is
[ADR-002](./ADR-002-Storage-Provider.md).

---

## Decision

Store document **binaries in an external object store**; the PostgreSQL database holds only an **opaque storage
reference** plus metadata (filename, MIME type, size, status). Files are owner-scoped and accessed through *
*short-lived,
authorized references** (e.g., signed/expiring URLs), never public or enumerable
URLs ([SECURITY §9](../SECURITY.md#9-file-upload-security)).
The concrete provider is deferred to [ADR-002](./ADR-002-Storage-Provider.md); this pattern is provider-independent via
the domain's **Storage port**.

---

## Alternatives Considered

- **Store bytes in PostgreSQL (`bytea`/large objects).** Rejected: bloats the DB, slows backups, raises cost, and
  couples file size to database scaling — directly against the free-tier goal.
- **Store files on the application host's filesystem.** Rejected: ephemeral on Render, not durable or shared across
  instances, and complicates scaling.
- **Serve files by streaming raw bytes through the API.** Rejected as the default: wastes backend bandwidth/compute;
  returning a short-lived access reference is cheaper and scales
  better ([API_SPEC §8.5](../API_SPEC.md#85-download-metadata--access-link)).

---

## Consequences

### Advantages

- Keeps PostgreSQL small, fast, and cheap to back up ([DATABASE §14](../DATABASE.md#14-risks)).
- Durable, scalable file storage independent of the app host.
- Security via private buckets + expiring, owner-scoped access references.

### Disadvantages

- Two systems (DB + object store) must be kept consistent — risk of orphaned files.
- Requires compensating cleanup on failed uploads and on
  delete ([DATABASE §11](../DATABASE.md#11-transaction-boundaries)).

### Trade-offs

- We accept managing DB/storage consistency (external I/O is not transactional with Postgres) in exchange for a lean
  database and durable, cheap file storage.

---

## Future Reconsideration

The pattern is durable. Revisit only if requirements demand in-DB storage (they should not) or if a future feature (
e.g.,
document versioning) changes reference semantics — handled
additively ([DATABASE §13](../DATABASE.md#13-future-database-evolution)).
Provider selection is tracked separately in [ADR-002](./ADR-002-Storage-Provider.md).

---

## References

[DATABASE](../DATABASE.md) · [SECURITY](../SECURITY.md) · [API_SPEC](../API_SPEC.md) · [ADR-002](./ADR-002-Storage-Provider.md)
