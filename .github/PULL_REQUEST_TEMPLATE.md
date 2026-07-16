<!--
LedgerAI is documentation-first: every change belongs to an owning document before it belongs to code.
Fill in each section and complete the checklist. Routing, review, and workflow rules live in
docs/03-engineering/CONTRIBUTING.md — this template only asks you to confirm you followed them.
-->

## What changed

<!-- A short description of the change. -->

## Owning document

<!-- Which document granted this change? Name and link it (SRS, an ADR, a design / AI / engineering doc).
     If no document owns it, stop — this may be undocumented behavior to raise first. -->

-

## Documents that need updating

<!-- Check every domain whose document this change affects, and update it in this PR. -->

- [ ] Product (PRD / SRS)
- [ ] Architecture / ADR
- [ ] Design
- [ ] AI
- [ ] Testing
- [ ] Deployment
- [ ] Release (changelog / release notes)
- [ ] None — no behavior changed

## Tests

<!-- What tests were added or updated? "None" is valid only if no behavior changed. -->

-

## Review required

<!-- Check every domain this change should be reviewed by; each is routed to its owning review process. -->

- [ ] Product review
- [ ] Architecture review
- [ ] Design review
- [ ] AI review
- [ ] Security review
- [ ] Deployment review
- [ ] Release review
- [ ] ADR required
- [ ] None of the above

## Checklist

- [ ] I identified the owning document for this change.
- [ ] I checked that no existing document already owns this decision (I am not creating a parallel or duplicate
  authority).
- [ ] I did not introduce undocumented behavior.
- [ ] I updated documentation where behavior changed.
- [ ] I added or updated tests where relevant.
- [ ] A changelog / release-notes update was made if this change is release-affecting (or is not needed).
- [ ] No frozen document is contradicted by this change.
- [ ] This change creates no second source of truth (no rule or decision restated in code or a new document).
- [ ] I linked the relevant documents or ADRs.
- [ ] This change is safe to merge.
