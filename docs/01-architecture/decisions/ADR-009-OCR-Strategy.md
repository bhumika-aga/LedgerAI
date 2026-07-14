# ADR-009 — OCR Strategy

**Status:** Accepted (pattern); provider **Deferred**
**Date:** 2026-07-14
**Owner:** Founding Engineer / Principal AI Architect
**Related Documents:
** [SRS §4.6](../../00-product/SRS.md#46-ocr-ocr) · [AI_ARCHITECTURE §5](../AI_ARCHITECTURE.md#5-ai-pipeline) · [ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services)

---

## Context

Many accounting documents are scans or images, so text extraction (OCR) is in MVP
scope ([PD-014](../../00-product/PRODUCT_DECISIONS.md#3-accepted-product-decisions)).
Extracted text is the grounded source for every AI capability ([BR-030](../../00-product/SRS.md#5-business-rules)), so
extraction quality directly determines AI quality. Documents vary: some already contain selectable text; some are pure
images. We must decide the extraction approach and how the OCR provider is integrated.

---

## Decision

Adopt a **native-first extraction strategy**: use direct text extraction when a document already contains selectable
text, and apply **OCR only for scans/images** ([BR-014](../../00-product/SRS.md#5-business-rules)). OCR is reached
through a **domain-owned OCR port** (same ports-and-adapters pattern as
AI/Storage, [ARCHITECTURE §10](../ARCHITECTURE.md#10-external-services)),
so the concrete OCR provider is swappable and its selection is **deferred**. Extraction runs during document processing
and drives the Ready/Failed state transitions; low-quality/failed extraction is surfaced, never hidden
([FR-OCR-003](../../00-product/SRS.md#46-ocr-ocr)).

---

## Alternatives Considered

- **OCR every document unconditionally.** Rejected: wasteful and lower-quality for documents that already have accurate
  embedded text; native extraction is faster, cheaper, and more accurate.
- **Bind directly to a specific OCR SDK/service.** Rejected: vendor lock-in; violates provider independence. The port
  keeps the choice reversible.
- **Skip OCR in the MVP (native text only).** Rejected: would break the product promise for the large share of inputs
  that are scans — OCR is explicitly in MVP scope.

---

## Consequences

### Advantages

- Best available text quality per document at lower cost (native when possible, OCR when needed).
- Provider-independent; OCR can be swapped or combined with the AI provider later.
- Quality signals let users judge reliability and let AI actions gate on Ready content.

### Disadvantages

- Two extraction paths to implement and test (native vs. OCR).
- OCR quality is bounded by input quality — poor scans may still extract poorly.

### Trade-offs

- We accept the complexity of a dual path and a deferred provider in exchange for higher quality, lower cost, and
  provider independence.

---

## Future Reconsideration

Select the concrete OCR provider before the OCR/AI-summary milestone (may be combined with the AI provider decision,
[DD-002](../../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)). Revisit the strategy if extraction quality proves
insufficient (e.g., add layout/table-aware extraction) or if handwriting/multi-language support becomes required — added
additively behind the port.

---

## References

[SRS §4.6](../../00-product/SRS.md#46-ocr-ocr) · [AI_ARCHITECTURE](../AI_ARCHITECTURE.md) · [ARCHITECTURE](../ARCHITECTURE.md) · [PRODUCT_DECISIONS](../../00-product/PRODUCT_DECISIONS.md)
