# ADR-002 — Storage Provider

**Status:** Accepted **Date:** 2026-07-17 *(originally deferred 2026-07-14; resolved here)*
**Owner:** Founding Engineer / Principal Architect
**Related Documents:
** [PRODUCT_DECISIONS DD-001](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) · [ADR-008 (Object Storage pattern)](./ADR-008-Object-Storage.md) · [ADR-012 (Deployment)](./ADR-012-Deployment-Strategy.md) · [DATABASE §1.3](../DATABASE.md#13-related-documents) · [SECURITY §9](../SECURITY.md#9-file-upload-security) · [SECURITY §13](../SECURITY.md#13-secrets-management) · [ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services)

---

## Context

Uploaded documents are stored in an **external object store**, with the database holding only an opaque reference — the
**pattern** is decided and unchanged in [ADR-008](./ADR-008-Object-Storage.md). This ADR concerns **which concrete
provider** backs the domain's **Storage port**; it selects a provider only and does **not** redesign storage
architecture.

The candidates named by [DD-001](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions) are **Cloudinary** and
**Supabase Storage**, both offering usable free tiers. This decision was deferred so the choice could be made against
real requirements rather than a snap
judgement; [ADR-002 required resolution before the Document Upload module is built (Milestone 3)](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions),
and a security review. Both are provided here.

The requirements this provider must satisfy are already fixed by the documentation and are **provider-independent**
([SECURITY §9](../SECURITY.md#9-file-upload-security)):

- Confidential document bytes held in an **external object store**, never in PostgreSQL or on the app host
  ([ADR-008](./ADR-008-Object-Storage.md), [DATABASE §1.3](../DATABASE.md#13-related-documents)).
- **Private, owner-scoped** files accessed only through **short-lived, authorized references** (signed/expiring URLs);
  **no public or enumerable URLs** ([SECURITY §9](../SECURITY.md#9-file-upload-security),
  [API_SPEC §8.5](../API_SPEC.md#85-download-metadata--access-link)).
- **Opaque internal storage keys**, never derived from user input (path-traversal guard,
  [NFR-009](../../00-product/SRS.md#9-non-functional-requirements)).
- Provider credentials held **server-side only**, least-privilege, never exposed to the browser
  ([SECURITY §13](../SECURITY.md#13-secrets-management)).
- Fits the free-tier, low-ops deployment target — backend on Render, database on Neon PostgreSQL
  ([ADR-012](./ADR-012-Deployment-Strategy.md)) — and remains reversible behind the Storage port
  ([ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services)).

---

## Decision

**Adopt Supabase Storage as the production storage provider** backing the Storage port defined by
[ADR-008](./ADR-008-Object-Storage.md).

The determining factor is **architectural fit**: LedgerAI needs a private object store with opaque keys and short-lived
signed URLs (ADR-008 + SECURITY §9), and Supabase Storage is exactly that — **S3-compatible object storage with
private-by-default buckets and signed, expiring download URLs**. Cloudinary is a **media-transformation and delivery**
platform whose core capabilities (image/video transformation, public CDN delivery) are unused by a
confidential-financial-document workload, and whose default delivery posture is public — the opposite of the
confidentiality-first requirement. Choosing the object-store-native provider means the adapter maps directly onto the
documented pattern instead of working against a media CDN's defaults.

This ADR **selects a provider only**. It does not change the Storage port, the object-storage pattern, the API contract,
the schema, or the security controls — all of which remain as already accepted.

---

## Evaluation

Each candidate is assessed against criteria already established in the repository documentation (or naturally derived
from it). No new product requirements are introduced.

| Criterion                             | Source                                                                                            | Supabase Storage                                                                               | Cloudinary                                                                             |
|---------------------------------------|---------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| **Private document storage**          | [SECURITY §9](../SECURITY.md#9-file-upload-security)                                              | Private-by-default buckets; native fit                                                         | Optimized for public CDN delivery; private/authenticated delivery is against the grain |
| **Object durability**                 | [ADR-008](./ADR-008-Object-Storage.md), FR-STOR-001                                               | Managed, durable object storage                                                                | Durable managed storage                                                                |
| **Signed / expiring download URLs**   | [API_SPEC §8.5](../API_SPEC.md#85-download-metadata--access-link)                                 | First-class signed URLs with TTL                                                               | Signed/authenticated URLs exist, but the default model is public delivery              |
| **Upload workflow**                   | [API_SPEC §8.1](../API_SPEC.md#81-upload-document)                                                | Standard object PUT/POST via S3-compatible API                                                 | Upload API is media-ingest oriented                                                    |
| **Ownership isolation**               | [BR-004](../../00-product/SRS.md#5-business-rules), [SECURITY §5](../SECURITY.md#5-authorization) | Enforced by the app (owner check in service) + opaque keys; provider offers bucket/RLS scoping | Enforced by the app; provider isolation is delivery-token based                        |
| **Security model**                    | [SECURITY §9](../SECURITY.md#9-file-upload-security)                                              | Object-store security model matches the documented controls                                    | Media-delivery security model requires extra care to keep everything private           |
| **Encryption**                        | [SECURITY §12](../SECURITY.md#12-data-security)                                                   | Encryption at rest and in transit (HTTPS)                                                      | Encryption at rest and in transit (HTTPS)                                              |
| **Operational complexity**            | free-tier / low-ops goal ([ARCHITECTURE §11](../ARCHITECTURE.md#11-scalability-strategy))         | Low; single managed service, S3 semantics                                                      | Low; but feature surface far exceeds needs                                             |
| **Local development**                 | [SECURITY §13](../SECURITY.md#13-secrets-management)                                              | S3-compatible API ⇒ substitutable with any S3-compatible local store; dev uses own config      | Requires the hosted service or mocks                                                   |
| **Deployment fit**                    | [ADR-012](./ADR-012-Deployment-Strategy.md)                                                       | Pairs cleanly with a Postgres-centric, Render/Neon deployment                                  | Independent SaaS; works, but no synergy                                                |
| **Scalability**                       | [ARCHITECTURE §11](../ARCHITECTURE.md#11-scalability-strategy)                                    | Scales as managed object storage                                                               | Scales as a media CDN                                                                  |
| **Vendor lock-in**                    | [PD-010](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions), TC-004              | **Low** — S3-compatible API is portable to AWS S3 / R2 / MinIO via the same adapter shape      | **Higher** — proprietary media API; the transformation features are non-portable       |
| **Cost characteristics**              | free-tier goal (DD-001)                                                                           | Usable free tier sufficient for MVP scale; storage-metered                                     | Usable free tier; credit-metered across storage + transforms + bandwidth               |
| **Future OCR pipeline compatibility** | [ADR-009](./ADR-009-OCR-Strategy.md), OCR port                                                    | OCR adapter fetches bytes via a short-lived reference — provider-agnostic                      | Same; no advantage                                                                     |
| **Future AI pipeline compatibility**  | [ADR-003](./ADR-003-AI-Provider-Abstraction.md), AI port                                          | AI operates on extracted text, not the raw file — storage-provider-agnostic                    | Same; no advantage                                                                     |
| **Auditability**                      | [ARCHITECTURE §9.8](../ARCHITECTURE.md#9-cross-cutting-concerns)                                  | Activity timeline records document actions in-app; provider access is server-mediated          | Same in-app; provider adds media-delivery logs not needed here                         |
| **Disaster recovery**                 | [DATABASE §14](../DATABASE.md#14-risks)                                                           | Managed durability; DB holds the reference, object store holds bytes — recover independently   | Managed durability; same split                                                         |

**Reading of the evaluation.** On the criteria that actually distinguish the two — private-by-default storage,
signed-URL fit, object-store semantics, portability/lock-in, and deployment synergy — Supabase Storage matches the
documented architecture directly, while Cloudinary's strengths (transformations, public CDN delivery) are irrelevant or
counter to the confidentiality requirement. On the criteria where they are equivalent (durability, encryption, OCR/AI
compatibility, DR), neither wins. No documented criterion favours Cloudinary for this workload.

---

## Security Review

Required by this ADR ([SECURITY Review Process](../SECURITY.md#security-review-process)). Many controls are
**application behaviour** and are therefore identical regardless of provider; SECURITY §9 states the upload controls are
provider-independent. Each item below marks whether it is satisfied by the **provider capability** or by **application
behaviour** (the adapter/service built in the Document Management slice).

| Concern                    | How it is satisfied                                                                                                                                                                 | Owner                                                                                                                                          |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| **Confidentiality**        | Private buckets; no public/enumerable URLs; bytes never in the DB or app host                                                                                                       | Provider capability (private buckets) + application (opaque keys, no public exposure)                                                          |
| **Integrity**              | HTTPS in transit; encryption at rest; `size_bytes`/`mime_type` recorded and checked                                                                                                 | Provider (encryption/transport) + application (metadata validation)                                                                            |
| **Availability**           | Managed, durable object storage; `503` surfaced when storage is unavailable ([API_SPEC §8.1/§8.5](../API_SPEC.md#81-upload-document))                                               | Provider (durability) + application (error mapping)                                                                                            |
| **Least privilege**        | Server holds a single least-privilege credential; browser never receives it                                                                                                         | Application ([SECURITY §13](../SECURITY.md#13-secrets-management))                                                                             |
| **Secret management**      | Credentials via environment/config only, never committed, rotatable without code change                                                                                             | Application ([SECURITY §13](../SECURITY.md#13-secrets-management))                                                                             |
| **URL expiration**         | Downloads return a **short-lived** signed URL with an explicit `expiresAt` ([API_SPEC §8.5](../API_SPEC.md#85-download-metadata--access-link))                                      | Provider (signed-URL TTL) + application (TTL policy)                                                                                           |
| **Upload authorization**   | Upload requires a valid access token and ownership of the target client before any store call                                                                                       | Application ([SECURITY §5](../SECURITY.md#5-authorization), `OwnershipGuard`)                                                                  |
| **Download authorization** | Ownership verified in the service before a signed URL is minted; the URL is owner-scoped and short-lived                                                                            | Application (ownership) + provider (signed URL)                                                                                                |
| **Ownership isolation**    | Every document reached only through a client the caller owns; non-owned ⇒ `404` ([BR-004](../../00-product/SRS.md#5-business-rules), [SECURITY §5](../SECURITY.md#5-authorization)) | Application                                                                                                                                    |
| **Public exposure risk**   | Buckets are private; internal keys are opaque and not derived from user input; original filenames are metadata only                                                                 | Provider (private buckets) + application (opaque keys, path-traversal guard, [NFR-009](../../00-product/SRS.md#9-non-functional-requirements)) |

**Conclusion of the review.** The provider must supply three things — private buckets, encryption at rest/in transit,
and signed URLs with a TTL — all of which Supabase Storage provides natively. Every remaining control is application
behaviour that the Document Management slice implements the same way for any provider, so the provider choice does not
weaken any documented control. Virus/malware scanning remains a documented **future** enhancement, not part of this
decision ([SECURITY §9](../SECURITY.md#9-file-upload-security), §19).

---

## Alternatives Considered

- **Supabase Storage — chosen.** S3-compatible object storage; private-by-default buckets; signed, expiring URLs; opaque
  keys; low operational surface; portable (low lock-in) via the S3 API. Matches ADR-008 and SECURITY §9 directly.
- **Cloudinary — rejected.** A media-transformation and CDN-delivery platform. Its differentiating features
  (transformations, fast public delivery) are unused by a confidential-document workload, its default delivery posture
  is public (requiring effort to keep strictly private), and its proprietary API increases lock-in. It would work, but
  it is the wrong tool: we would be adopting a media CDN to store private financial documents and then disabling the
  parts that make it a media CDN.
- **Store binaries in PostgreSQL.** Rejected in [ADR-008](./ADR-008-Object-Storage.md) — DB bloat, cost, backup weight.
  Not reconsidered here.
- **Self-hosted object storage.** Rejected for MVP — operational overhead against the free-tier, low-ops goal.

---

## Consequences

### Advantages

- The adapter maps directly onto the documented pattern (object store + opaque reference + signed URL); no fighting a
  media CDN's defaults.
- **Private-by-default** posture aligns with confidentiality-first requirements
  ([SECURITY §9](../SECURITY.md#9-file-upload-security)).
- **S3-compatible API lowers lock-in**: the same adapter shape ports to AWS S3, Cloudflare R2, or MinIO later.
- Pairs cleanly with the Postgres-centric Render/Neon deployment ([ADR-012](./ADR-012-Deployment-Strategy.md)) and the
  free-tier, low-ops goal.
- Unblocks **DD-001 / Milestone 3**: the Document Management slice can now implement the Storage port's adapter.

### Disadvantages

- Forgoes Cloudinary's on-the-fly transformations (e.g., thumbnails/previews). These are **not** an MVP requirement; if
  document previews are needed later, they can be produced in the OCR/AI pipeline or a dedicated adapter — not a reason
  to choose a media CDN now.
- DB and object store must be kept consistent (orphaned-file risk) — but this is inherent to the ADR-008 pattern, not to
  this provider, and is handled by compensating cleanup ([DATABASE §11](../DATABASE.md#11-transaction-boundaries)),
  unchanged by this decision.

### Trade-offs

- We accept a storage provider with fewer media features in exchange for a **direct architectural fit, a stronger
  default privacy posture, and lower lock-in** — the properties the documented requirements actually ask for.

### Migration implications if replaced later

Because business logic depends only on the **Storage port** ([ADR-008](./ADR-008-Object-Storage.md),
[ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services)), replacing Supabase Storage means **adding/selecting
another Storage adapter and selecting it by configuration — no change to services, controllers, or rules**. A provider
switch additionally requires a one-time data migration: copy the stored objects to the new provider and repoint each
`document.storage_reference` ([DATABASE §5.3](../DATABASE.md#5-entity-specifications)). The S3-compatibility of the
chosen provider makes even that path cheap, since any S3-compatible target reuses the same adapter surface.

---

## Future Reconsideration

Revisit if free-tier limits are exceeded at scale, if security/compliance needs grow (e.g., regional data residency or
customer-managed keys), or if cost/ergonomics change materially. Reconsideration means selecting a different Storage
adapter — no business-logic change — plus the one-time object migration described above.

---

## References

[PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md) · [ADR-008](./ADR-008-Object-Storage.md) · [ADR-012](./ADR-012-Deployment-Strategy.md) · [DATABASE](../DATABASE.md) · [SECURITY](../SECURITY.md) · [ARCHITECTURE](../ARCHITECTURE.md) · [API_SPEC](../API_SPEC.md)
