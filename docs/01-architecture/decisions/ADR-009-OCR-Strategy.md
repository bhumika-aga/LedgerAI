# ADR-009 — OCR Strategy

**Status:** Accepted (pattern **and** provider)
**Date:** 2026-07-17 *(pattern accepted 2026-07-14; provider resolved here)*
**Owner:** Founding Engineer / Principal AI Architect
**Related Documents:
** [SRS §4.6](../../00-product/SRS.md#46-ocr-ocr) · [AI_ARCHITECTURE §5](../AI_ARCHITECTURE.md#5-ai-pipeline) · [ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services) · [SECURITY §10](../SECURITY.md#10-ai-security) · [SECURITY §13](../SECURITY.md#13-secrets-management) · [DATABASE §5.4](../DATABASE.md#5-entity-specifications) · [API_SPEC §9](../API_SPEC.md#9-ocr-module) · [ADR-013 (Background Processing)](./ADR-013-Background-Processing.md) · [ADR-002 (Storage Provider)](./ADR-002-Storage-Provider.md)

---

## Context

Many accounting documents are scans or images, so text extraction (OCR) is in MVP scope
([PD-014](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions)). Extracted text is the grounded source
for every AI capability ([BR-030](../../00-product/SRS.md#5-business-rules)), so extraction quality directly determines
AI quality. Documents vary: some already contain selectable text; some are pure images. We must decide the extraction
approach and how the OCR provider is integrated.

**This ADR was originally split:** the *strategy* (native-first extraction through a domain-owned OCR port) was
**Accepted**, while the *concrete OCR provider* was **Deferred**
([DD-002](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)), to be resolved before the OCR/AI-summary
milestone. This revision **resolves the provider** so the OCR Processing slice is no longer blocked, and fills the one
remaining implementation gap the native-first strategy left open — which mechanism performs the native (embedded-text)
extraction before OCR is invoked. It **selects a provider and a native mechanism only**; the OCR architecture
(native-first, port/adapter) is unchanged.

The requirements the provider must satisfy are already fixed by the documentation:

- **Produce machine-readable *Extracted Text*** from scanned/image documents (SRS §4.6). The documented output is plain
  extracted text plus a **quality signal** — `extraction_quality ∈ HIGH | LOW | UNKNOWN` and `char_count`
  ([DATABASE §5.4](../DATABASE.md#5-entity-specifications)). It is **not** structured tables/forms/key-value pairs;
  nothing in the schema or API stores those, so premium form/table extraction would be unused complexity (CLAUDE.md —
  keep the simplest correct solution).
- **Support the uploaded types**: PDF, PNG, JPEG ([DocumentProperties]/VR-005 allow-list).
- **Surface low-confidence and failure**, never hide them (FR-OCR-003, FR-OCR-005, FR-OCR-006) — the provider must
  expose a **confidence signal** to derive the quality value.
- **Reached only through the domain-owned OCR port** so the choice stays reversible
  ([ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services)); the provider SDK/HTTP contract is confined to the
  adapter.
- **Server-side, least-privilege credentials**, minimum-necessary content sent to the provider
  ([SECURITY §10](../SECURITY.md#10-ai-security), [SECURITY §13](../SECURITY.md#13-secrets-management),
  [NFR-018](../../00-product/SRS.md#9-non-functional-requirements)).
- **Free-tier, low-ops** deployment fit — backend on Render, DB on Neon ([ADR-002](./ADR-002-Storage-Provider.md)
  context).
- **No async framework required**: [ADR-013](./ADR-013-Background-Processing.md) permits MVP to run
  **synchronously-with-status** behind the existing service boundary, so extraction can run during processing without a
  new job/queue mechanism.

**Candidate set.** The repository intentionally names no OCR vendor (AI_ARCHITECTURE is provider-neutral). The smallest
reasonable set of production-grade cloud OCR services that extract text from scanned financial documents and return a
per-block/word **confidence** score (needed for the quality signal) is: **Google Cloud Vision** (Document Text
Detection), **AWS Textract** (DetectDocumentText), and **Azure AI Document Intelligence** (Read). These are the three
mature, widely-used cloud OCR APIs that meet the documented requirement with a usable free tier; self-hosted engines
(e.g. Tesseract-as-a-service) are considered under Alternatives but excluded from the primary set for the low-ops,
accuracy-on-real-scans reasons below. The decision is **not broadened** beyond production-grade OCR text extraction.

---

## Decision

**1. OCR provider — adopt Google Cloud Vision (Document Text Detection)** as the production OCR provider implementing
the external OCR adapter behind the OCR port.

The determining factor is **fit to the documented output with the least complexity**. LedgerAI needs *plain extracted
text plus a confidence signal* (DATABASE §5.4), not structured tables/forms. Google Cloud Vision's
`DOCUMENT_TEXT_DETECTION` returns exactly that — full document text with per-symbol/word/block confidence to derive
`extraction_quality` — over a single, mature REST endpoint, with a **perpetual free tier** (first 1,000 units/month)
that suits the free-tier goal, and no coupling to the deployment cloud (it is reached over HTTPS from Render). AWS
Textract and Azure Document Intelligence are equally capable but their differentiators (table/form/key-value extraction)
are unused by this schema, and their free tiers are time-boxed or smaller.

**2. Native extraction mechanism — adopt Apache PDFBox** (`org.apache.pdfbox`, JVM library) as the embedded-text
extractor that runs **before** OCR under the native-first strategy (BR-014, FR-OCR-002).

For a PDF that already contains selectable text, PDFBox's text stripper extracts it in-process — no external call, no
cost, higher fidelity than re-OCR'ing — and the document goes straight to `READY` (native path). Only when native
extraction yields no usable text (image-only PDFs) or the input is an image (PNG/JPEG, which carry no embedded text) is
the OCR provider invoked. PDFBox is pure-Java, matches the Java 21 backend, and needs no service.

This ADR **selects a provider and a native mechanism only**. It does not change the OCR port, the native-first strategy,
the state machine, the API contract, the schema, or the security controls — all remain as accepted.

---

## Evaluation

Providers assessed only against requirements already present in the repository. No new product requirements are
introduced. ("Structured extraction" is read as the documented need — machine-readable *text* — not tables/forms, which
the schema does not store.)

| Criterion                              | Source                                                                               | Google Cloud Vision                                                     | AWS Textract                                        | Azure AI Document Intelligence                      |
|----------------------------------------|--------------------------------------------------------------------------------------|-------------------------------------------------------------------------|-----------------------------------------------------|-----------------------------------------------------|
| **Scanned financial-doc extraction**   | SRS §4.6                                                                             | Strong general OCR on scans/photos                                      | Strong; document-optimized                          | Strong; document-optimized                          |
| **Native-first compatibility**         | ADR-009, BR-014                                                                      | Provider only invoked when native yields no text — orthogonal           | Same                                                | Same                                                |
| **OCR accuracy (printed text)**        | FR-OCR-003                                                                           | High                                                                    | High                                                | High                                                |
| **"Structured text" = extracted text** | DATABASE §5.4                                                                        | Full text + confidence — exact fit                                      | Also returns tables/forms (unused)                  | Also returns tables/forms (unused)                  |
| **Image + PDF support**                | VR-005 (PDF/PNG/JPEG)                                                                | Yes                                                                     | Yes                                                 | Yes                                                 |
| **Confidence for quality signal**      | FR-OCR-006, DATABASE §5.4                                                            | Per-symbol/word/block confidence                                        | Per-block confidence                                | Per-line/word confidence                            |
| **Deployment fit (Render/Neon)**       | [ADR-002](./ADR-002-Storage-Provider.md) ctx                                         | HTTPS API, no cloud coupling                                            | HTTPS API, no cloud coupling                        | HTTPS API, no cloud coupling                        |
| **Operational complexity**             | free-tier/low-ops goal                                                               | Low — single REST call                                                  | Low–moderate                                        | Low–moderate                                        |
| **Scalability**                        | [ARCHITECTURE §11](../ARCHITECTURE.md#11-scalability-strategy)                       | Managed, scales on demand                                               | Managed                                             | Managed                                             |
| **Cost characteristics**               | DD-002 (free tier)                                                                   | **Perpetual** 1,000 units/mo free, then per-page                        | Free tier time-boxed (first months)                 | Free tier smaller (per-month pages)                 |
| **Provider lock-in**                   | [PD-010](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions), TC-004 | Low — behind OCR port; plain-text output is portable                    | Low behind port, but richer output invites coupling | Low behind port, but richer output invites coupling |
| **API maturity**                       | —                                                                                    | Very mature, stable                                                     | Very mature                                         | Mature                                              |
| **Retry characteristics**              | FR-OCR-005 (retryable)                                                               | Stateless request; safe to retry                                        | Stateless (sync API)                                | Stateless (sync Read)                               |
| **Future AI-pipeline compatibility**   | AI_ARCHITECTURE §5, [ADR-003](./ADR-003-AI-Provider-Abstraction.md)                  | AI consumes extracted text, provider-agnostic                           | Same                                                | Same                                                |
| **Auditability**                       | [ARCHITECTURE §9.8](../ARCHITECTURE.md#9-cross-cutting-concerns)                     | In-app activity + provider request logs                                 | Same                                                | Same                                                |
| **Disaster recovery**                  | [DATABASE §14](../DATABASE.md#14-risks)                                              | Text re-extractable from the retained file (storage is source of truth) | Same                                                | Same                                                |

**Reading.** On the criteria that differentiate the three, Google Cloud Vision wins on the two that matter for *this*
schema: a **perpetual free tier** (vs. time-boxed/smaller) and an **exact text-first fit** with the least integration
surface. Textract and Azure add table/form extraction the schema does not store — capability that is either unused or an
invitation to couple to a richer output shape (raising lock-in). Where all three are equivalent (accuracy, image/PDF
support, retry, DR, AI compatibility), none wins. No documented criterion favours Textract or Azure for this workload.

---

## Security Review

Required because OCR providers receive document content
([SECURITY Review Process](../SECURITY.md#security-review-process),
[SECURITY §10](../SECURITY.md#10-ai-security)). Each concern is marked **provider capability** or **application
responsibility** (the adapter/service built in the OCR slice). Most controls are application behaviour and are
provider-independent.

| Concern                                     | How it is satisfied                                                                                                                                                                                       | Owner                                                                               |
|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| **Confidentiality**                         | Document content is confidential; sent to the provider over TLS solely to extract text, held only server-side                                                                                             | Provider (secure processing) + application (server-side only, never to browser)     |
| **Minimum-necessary transfer**              | Only the file bytes needed for extraction are sent — no user, client, or account metadata (NFR-018, SECURITY §10)                                                                                         | Application (adapter sends bytes only)                                              |
| **Encryption in transit**                   | HTTPS to the provider endpoint                                                                                                                                                                            | Provider (TLS) + application (enforce HTTPS)                                        |
| **Encryption at rest**                      | Bytes live in the storage provider (ADR-002/008); extracted text in Neon Postgres (encrypted at rest)                                                                                                     | Provider (Google processing) + application (no local persistence of bytes)          |
| **Credential management**                   | The Google service credential is supplied via environment/config, never committed, rotatable without code change (SECURITY §13)                                                                           | Application                                                                         |
| **Least privilege**                         | A single server-held credential scoped to the OCR/Vision capability only; never exposed to the browser or AI provider                                                                                     | Application                                                                         |
| **Ownership isolation**                     | OCR runs only for a document the caller owns; status reads authorize via the existing `OwnershipGuard`/published client check; non-owned ⇒ `404` (BR-004, SECURITY §5)                                    | Application                                                                         |
| **Data retention**                          | Google Cloud Vision does **not** retain request images after processing for `DOCUMENT_TEXT_DETECTION` by default; LedgerAI retains only the extracted text it needs (DATABASE §5.4)                       | Provider (no retention, per Google docs) + application (retain only extracted text) |
| **Provider training / customer-data usage** | Google Cloud (paid/standard API) does **not** use submitted content to train its models, per Google Cloud's data-usage terms; this MUST be re-verified against current provider terms at integration time | Provider (documented terms) — **verify at integration**                             |
| **Failure handling**                        | Provider unavailability/timeout maps to a domain failure; extraction failure transitions the document to `FAILED` with a non-revealing reason, retryable (FR-OCR-005), never presented as `READY`         | Application (adapter → domain exception; service → state transition)                |

**Conclusion.** The provider must supply three things — TLS transport, a confidence signal, and no-retention/no-training
of submitted content — all of which Google Cloud Vision provides (the last two **must be re-verified against current
Google Cloud terms at integration time**, and are the one provider-dependent item). Every other control is application
behaviour implemented identically for any provider, so the choice weakens no documented control. Malware scanning of
uploads remains a documented **future** item (SECURITY §9, §19), unaffected here.

---

## Native Extraction (companion to the native-first strategy)

ADR-009 mandates native-first extraction but left the *mechanism* unspecified. **Apache PDFBox** fills that gap:

- **What it does:** extracts embedded selectable text from PDFs in-process. If a PDF yields usable text, OCR is skipped
  and the document reaches `READY` via the native path with `extraction_method = NATIVE` (BR-014). Image inputs
  (PNG/JPEG) have no embedded text and always route to OCR (`extraction_method = OCR`).
- **Why it fits:** pure-Java, matches the Java 21 backend, no external service or cost, higher fidelity than re-OCR'ing
  text that is already digital — directly serving FR-OCR-002's "use native text extraction and skip OCR" goal.
- **Why it needs no separate ADR:** it is an **in-process library choice implementing an already-accepted strategy**,
  not a new architectural decision. It introduces no external dependency boundary, no network/security surface, no
  contract, and no lock-in (it is swappable for Apache Tika or another extractor with no architectural change) — the
  same category as choosing Jackson for JSON or BCrypt for hashing, which are library selections, not ADRs. It sits on
  the domain side of the OCR port as a local strategy step, invoked before the port is consulted.

---

## Alternatives Considered

**OCR provider:**

- **Google Cloud Vision (Document Text Detection) — chosen.** Text-first output matching the schema, perpetual free
  tier, single mature REST endpoint, low integration surface, provider-independent behind the port.
- **AWS Textract — rejected.** Equally capable OCR, but its differentiators (table/form/key-value extraction) are unused
  by the documented `document_content` schema, and its free tier is time-boxed. Would work; adds capability we do not
  store and would pay for.
- **Azure AI Document Intelligence (Read) — rejected.** Same reasoning as Textract; strong document OCR but a smaller
  free tier and a form/document orientation beyond the documented text-only need.
- **Self-hosted Tesseract — rejected for MVP.** No per-call cost, but it must be hosted, tuned, and operated (against
  the low-ops, free-tier goal), and typically yields lower accuracy on real-world financial scans/photos than the
  managed services. Remains a valid **future** adapter behind the port if cost or data-residency ever demands on-prem
  OCR.

**Native mechanism:**

- **Apache PDFBox — chosen.** In-process PDF text extraction; minimal, direct fit.
- **Apache Tika — viable alternative, not chosen now.** Wraps PDFBox plus many formats; more than the PDF/PNG/JPEG
  allow-list needs today. Reserved as a drop-in if the allow-list broadens.
- **Send every document straight to OCR (skip native) — rejected.** Contradicts ADR-009's accepted native-first strategy
  and BR-014; wasteful and lower-quality for PDFs that already contain accurate text.

---

## Consequences

### Advantages

- The OCR adapter maps directly onto the documented output: text + confidence → `extracted_text` + `extraction_quality`
    - `char_count`, with no unused premium features.
- **Native-first is fully realized**: digital PDFs extract locally and free; only images and image-only PDFs incur an
  OCR call — best quality at lowest cost (the ADR-009 goal).
- **Provider-independent and low lock-in**: business logic depends only on the OCR port; the plain-text output shape is
  portable to any OCR provider.
- **No new infrastructure**: runs synchronously-with-status under the existing service boundary (ADR-013), on the
  Render/Neon free-tier stack.
- **Unblocks DD-002 / the OCR milestone**: the OCR Processing slice can now implement the port's adapter.

### Disadvantages

- Google Cloud Vision plain OCR does not return layout/tables; if a *future* feature needs structured extraction, it
  requires a richer adapter (Textract/Azure/Document AI) — an additive change behind the port, not a rework now.
- Two extraction paths (native vs. OCR) to implement and test — inherent to native-first (ADR-009), not to this
  provider.
- OCR quality is bounded by input quality — poor scans may still extract poorly; surfaced via the quality signal and the
  `FAILED` path, never hidden.

### Trade-offs

- We accept a text-only OCR provider (no table/form extraction) in exchange for an **exact fit to the documented schema,
  a perpetual free tier, the lowest integration surface, and lower lock-in** — the properties the requirements actually
  ask for.

### Migration implications if replaced later

Because business logic depends only on the **OCR port** ([ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services)),
replacing Google Cloud Vision means **adding/selecting another OCR adapter and choosing it by configuration — no change
to services, controllers, rules, schema, or API**. No data migration is needed: extracted text already persisted stays
valid, and any document can be **re-extracted from its retained file** (the storage provider is the source of truth), so
a provider switch is effectively a re-run, not a migration. PDFBox is likewise swappable (e.g. for Tika) as a local
library change.

### Interaction with the OCR port

The port stays exactly as ADR-009 defined it: a domain-owned interface expressed in domain terms ("extract text from
this document's bytes; return text + a quality/confidence signal"). Native extraction (PDFBox) runs on the domain side
**before** the port is consulted; the Google Cloud Vision adapter implements the port for the OCR path and maps the
provider's response/confidence and errors back into domain terms (including the error taxonomy of
[SRS §8](../../00-product/SRS.md#8-error-handling) and the `FAILED` transition). No provider type crosses the port.

---

## Future Reconsideration

Revisit if extraction quality proves insufficient (e.g. add layout/table-aware extraction via a richer adapter), if
handwriting/multi-language support becomes required, if free-tier limits are exceeded at scale, or if data-residency /
customer-managed-key needs arise (which could motivate a self-hosted adapter) — each added **additively behind the
port**, no business-logic change. The provider's data-retention and no-training terms MUST be re-verified at integration
time and on any provider change.

---

## References

[SRS §4.6](../../00-product/SRS.md#46-ocr-ocr) · [AI_ARCHITECTURE](../AI_ARCHITECTURE.md) · [ARCHITECTURE](../ARCHITECTURE.md) · [SECURITY](../SECURITY.md) · [DATABASE](../DATABASE.md) · [API_SPEC](../API_SPEC.md) · [ADR-013](./ADR-013-Background-Processing.md) · [ADR-002](./ADR-002-Storage-Provider.md) · [ADR-003](./ADR-003-AI-Provider-Abstraction.md) · [PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md)
