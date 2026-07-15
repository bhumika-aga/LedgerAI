# Evaluation — LedgerAI MVP

> **Status:** Draft v1
> **Owner:** Principal AI Evaluation Architect
> **Last updated:** 2026-07-15
> **Upstream (frozen):
** [AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md) · [TESTING_STRATEGY](../03-engineering/TESTING_STRATEGY.md) · [SRS](../00-product/SRS.md) · [PRD](../00-product/PRD.md) · [SECURITY](../01-architecture/SECURITY.md) · [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md)
> **Related:
** [AI_PROVIDERS](./AI_PROVIDERS.md) · [PROMPTS](./PROMPTS.md) · [RAG](./RAG.md) · [CLAUDE.md](../../CLAUDE.md)

---

## 1. Purpose

### Why this document exists

Every other gate in LedgerAI has something that fails loudly. Code has a compiler, an API has a contract, a schema has a
migration. AI quality has none of these: a worse summary looks exactly like a better one until a professional acts on
it, and the only thing standing between "this reads well" and "this is right" is a judgment somebody made and wrote
down.

**What quality means is already settled.** The
[AI Quality Principles](../01-architecture/AI_ARCHITECTURE.md#ai-quality-principles) define the bar every AI capability
must meet — accuracy over fluency, predictability, honesty about uncertainty, professional polish. The
[AI Evaluation Strategy](../01-architecture/AI_ARCHITECTURE.md#ai-evaluation-strategy) names the **dimensions** quality
is measured along. [TESTING_STRATEGY §7](../03-engineering/TESTING_STRATEGY.md#7-ai-testing-strategy) settles that
evaluation is a **separate, non-deterministic track** from application tests, and that provider, model, and prompt
changes are evaluated before rollout.

What no document yet owns is **how a measurement becomes a decision**: what a result is compared against, what makes a
comparison trustworthy, who judges when the numbers disagree with each other, and how a finding stays attributable long
enough to be re-examined. [AI_PROVIDERS](./AI_PROVIDERS.md) and [PROMPTS](./PROMPTS.md) both **consume a verdict** and
both explicitly decline to define how one is produced. This document owns that.

The governing principle of this document:

> **A measurement is evidence; a verdict is a decision.**
>
> A number that nobody attached to a capability, a baseline, and a name is not a finding — it is a rumor with a decimal
> point, and it will be cited as proof by the first person who needs it to be.

It is **not** a benchmark suite, **not** a set of datasets, **not** a scorecard, **not** provider governance, **not**
prompt design, and **not** implementation. It contains **no provider or model names, no prompt text, no datasets or
dataset schemas, no benchmark numbers, no scoring formulas, no thresholds, and no retrieval or orchestration design**.
It governs *what is measured, when judgment is required, how baselines work, and how results are judged* — never what
any particular result is.

### Relationship to the AI documents

| Document                                                     | Its job                                     | The boundary with this document                                                                                                                                                                                                                                                                                                                                                                                                                    |
|--------------------------------------------------------------|---------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [AI_ARCHITECTURE.md](../01-architecture/AI_ARCHITECTURE.md)  | **AI behavior and the orchestration model** | It owns the **quality bar** ([AI Quality Principles](../01-architecture/AI_ARCHITECTURE.md#ai-quality-principles)) and the **evaluation dimensions** ([AI Evaluation Strategy](../01-architecture/AI_ARCHITECTURE.md#ai-evaluation-strategy)). This document owns the **discipline of judging against them**: baselines, comparison, verdicts, traceability. It sets no bar and adds no dimension.                                                 |
| [TESTING_STRATEGY.md](../03-engineering/TESTING_STRATEGY.md) | **How the product is verified**             | It owns the **split** between deterministic application tests and non-deterministic evaluation, and where evaluation sits in the test strategy and Definition of Done ([§7](../03-engineering/TESTING_STRATEGY.md#7-ai-testing-strategy)). This document owns **how an evaluation result is judged once that track has run**. It governs the verdict, not the track.                                                                               |
| [AI_PROVIDERS.md](./AI_PROVIDERS.md)                         | **Provider selection and governance**       | It owns what a provider is judged *against* ([§6](./AI_PROVIDERS.md#6-provider-selection-criteria)) and the gates that require judgement ([§5](./AI_PROVIDERS.md#5-provider-lifecycle)); it states that measurement is this document's. This document supplies the finding and **never** makes the selection — choosing a provider on the evidence is that document's decision.                                                                    |
| [PROMPTS.md](./PROMPTS.md)                                   | **Prompt design, structure, and lifecycle** | It owns when a prompt change **requires** a judgement ([Prompt Review Process](./PROMPTS.md#prompt-review-process)) and what a version is judged as ([§6](./PROMPTS.md#6-prompt-versioning)). This document produces the judgement it consumes, and never edits a prompt — a finding is an input to prompt review, not a prompt change.                                                                                                            |
| [RAG.md](./RAG.md)                                           | **Retrieval design, if and when it exists** | Retrieval is deferred ([DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)). If it arrives it changes **what context reaches a capability**, which changes what a result means but not how results are judged. Retrieval would become a thing evaluated (§4) and a reason a baseline is re-established (§7) — not a new way of evaluating.                                                                                           |
| [SECURITY.md](../01-architecture/SECURITY.md)                | **The security posture**                    | It owns AI security, including injection resistance and what may reach a model ([§10](../01-architecture/SECURITY.md#10-ai-security)). Evaluation observes real output on real inputs and is therefore **subject** to it: an evaluation is not a licence to route content anywhere it could not otherwise go (§5).                                                                                                                                 |
| **The ADRs**                                                 | **Ratified decisions**                      | [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) established provider independence; [ADR-010](../01-architecture/decisions/ADR-010-AI-Request-Lifecycle.md) the request lifecycle; [ADR-015](../01-architecture/decisions/ADR-015-Observability.md) observability. This document operates **inside** them. A finding that argues for changing one is an ADR, not an evaluation result ([CLAUDE.md §8](../../CLAUDE.md)). |

In one line each:

> **AI_ARCHITECTURE defines the AI behavior and orchestration model. AI_PROVIDERS defines provider selection and
> governance. PROMPTS defines prompt design, structure, lifecycle, and review. EVALUATION defines how AI quality is
> measured and judged. RAG defines how retrieved context is incorporated, if and when it exists.**

### Relationship to the frozen product documents

This document introduces **no product behavior and no quality bar**. What "good" means is
[SRS](../00-product/SRS.md)'s and
the [AI Quality Principles](../01-architecture/AI_ARCHITECTURE.md#ai-quality-principles)';
what "good enough to ship" means for the product is [PRD §11](../00-product/PRD.md#11-success-metrics)'s. Evaluation
measures against those and reports. Where an evaluation appears to demand a different bar, that is a change to the
document that owns the bar, raised per [CLAUDE.md §8](../../CLAUDE.md) — never a criterion quietly adjusted until the
result improves.

---

## 2. Evaluation Philosophy

[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md#evaluation-philosophy) already carries an **Evaluation
Philosophy** — the principles for how evaluation is *run*: repeatable, continuous, benchmarked before rollout,
regression-tested, evolving with the product. It is frozen and it wins. The principles below are not a second copy of
it; they explain **why evaluation is *governed* the way this document governs it**, and they are the reasoning behind
the enforceable rules that follow. Where the two appear to disagree, AI_ARCHITECTURE's wins and this document is
corrected.

| Principle                        | Why it exists                                                                                                                                                                                                                                                                                                       |
|----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Truth over confidence**        | A model's certainty is a property of its wording, not of the evidence. Output that hedges correctly is better than output that asserts wrongly, and any evaluation that rewards decisiveness will select for the failure mode the product exists to avoid ([BR-033](../00-product/SRS.md#5-business-rules)).        |
| **Grounded over fluent**         | Fluency is the easiest thing to notice and the least correlated with being right. A reviewer reading for polish will pass an eloquent fabrication and fail a plain, correct answer — so grounding is judged explicitly, never inferred from how the output reads ([BR-030](../00-product/SRS.md#5-business-rules)). |
| **Repeatable over anecdotal**    | "I tried it and it seemed better" is unfalsifiable and therefore unarguable — it cannot be re-run, contradicted, or trusted six months later. Repeatability is what makes a result evidence rather than a memory.                                                                                                   |
| **Comparative over absolute**    | An absolute score answers a question nobody asked. The decisions this document serves are always comparative — *is this version better than the one in production?* — and a number with nothing beside it cannot answer that (§7).                                                                                  |
| **Regression-aware**             | AI change is overwhelmingly change that improves one thing. The risk is never the improvement; it is the unexamined dimension that quietly got worse while attention was elsewhere, which is exactly what nobody thinks to measure.                                                                                 |
| **Human-judged where necessary** | Some questions are trade-offs, not measurements — a more accurate output that is harder to edit may be worse for the professional using it. A metric cannot hold two goods against each other; only a person accountable for the call can (§8).                                                                     |
| **Capability-specific**          | LedgerAI's AI actions demand different things ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)): a summary, an answer, a draft email, a report. Quality averaged across them describes none of them, and hides the one that broke.                                                  |
| **Traceable results**            | A finding whose capability, baseline, and inputs were not recorded cannot be re-examined when it is challenged — and it *will* be challenged, at the moment it is most inconvenient. Untraceable evidence is eventually indistinguishable from opinion.                                                             |
| **Production-relevant**          | Evaluation exists to predict what a professional will experience on a real document, not to produce a good result on a convenient one. An evaluation that no longer resembles production measures the evaluation.                                                                                                   |
| **Continuous improvement**       | Quality is expected to improve as models, providers, and prompts evolve ([AI Quality Principles](../01-architecture/AI_ARCHITECTURE.md#ai-quality-principles)). Improvement that cannot be demonstrated is indistinguishable from drift — including drift downward.                                                 |

---

## Evaluation Rules

> *Unnumbered governance section. These are enforceable rules, not preferences. Each protects a specific guarantee — the
> rationale follows each rule.*

- **Every evaluated capability MUST have a documented purpose.** *A capability with no granted purpose has no standard
  to be measured against ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)); evaluating
  it invents the standard as it goes, and the standard invented is always the one the output already meets.*
- **Every evaluation MUST state what is being judged.** *"Did it get better?" is not a question — better at what, for
  which capability, against what? An evaluation that does not name its subject cannot be contradicted, and cannot be
  re-run.*
- **Every evaluation MUST be repeatable enough to compare over time.** *AI output is non-deterministic
  ([TESTING_STRATEGY §7](../03-engineering/TESTING_STRATEGY.md#7-ai-testing-strategy)); a result that cannot be
  re-produced closely enough to compare is a single sample, and a single sample of a probabilistic process is an
  anecdote wearing a number.*
- **Every evaluation SHOULD use the same baseline when possible.** *Comparison requires a fixed point. A baseline
  changed in the same breath as the thing being measured produces a result that is arithmetically valid and
  epistemically worthless (§7).*
- **Evaluation criteria MUST be traceable to product requirements or architecture decisions.** *A criterion with no
  source is a preference with authority. Criteria derive from [SRS](../00-product/SRS.md), the
  [AI Quality Principles](../01-architecture/AI_ARCHITECTURE.md#ai-quality-principles), or a ratified decision — never
  from what was convenient to measure.*
- **Evaluation changes MUST be reviewed before they affect production decisions.** *Changing how quality is measured
  changes what passes. An unreviewed criterion change is a silent change to the product's standard, made by whoever
  edited the criterion (Evaluation Review Process).*
- **Evaluation MUST distinguish prompt changes from provider changes where relevant.** *When both move together, the
  result attributes to neither. The next question is always "was it the prompt or the provider?" and an evaluation that
  cannot answer it has to be run again ([PROMPTS](./PROMPTS.md), [AI_PROVIDERS](./AI_PROVIDERS.md)).*
- **Evaluation MUST NOT be reduced to a single number unless that number is explicitly meaningful.** *A composite score
  is a set of trade-offs someone made silently and then hid behind arithmetic. It will keep rising while the dimension
  that matters falls, and it is the number that gets quoted (§6).*
- **Evaluation MUST NOT substitute for product requirements or architecture decisions.** *Evidence informs a decision;
  it does not become one. A measurement cannot grant behavior, change a rule, or overturn an ADR — those are raised per
  [CLAUDE.md §8](../../CLAUDE.md).*
- **Evaluation findings MUST remain traceable.** *A finding cited without its capability, baseline, inputs, and date is
  an assertion with a provenance nobody can check — and stale evidence is most persuasive precisely when it is most
  wrong ([ADR-015](../01-architecture/decisions/ADR-015-Observability.md)).*

**Why these rules exist.** Evaluation fails in ways that look like success. The three failure modes are **metric drift**
(the criteria move toward what the system already does, one reasonable adjustment at a time, until they certify it),
**hidden baselines** (the comparison point is unrecorded or was quietly re-established, so every change appears to be
an improvement), and **false confidence** (a number that was never valid for the question it is now being used to
settle).

Each is invisible from inside. Nobody experiences metric drift as lowering the bar; they experience it as fixing an
unfair test. Nobody notices a hidden baseline; they notice that the numbers look good. These rules exist because the
alternative is not the absence of evaluation — it is evaluation that reliably approves whatever it is shown, which is
worse than none, because it is trusted.

---

## 3. Evaluation Model

**The evaluation dimensions are owned by
[AI_ARCHITECTURE — AI Evaluation Strategy](../01-architecture/AI_ARCHITECTURE.md#ai-evaluation-strategy)** — which
dimensions exist and what each assesses. This section restates none of them and **MUST NOT redefine, rename, merge, or
add a dimension**. The **quality bar** those dimensions are judged against is likewise
[AI_ARCHITECTURE's](../01-architecture/AI_ARCHITECTURE.md#ai-quality-principles); this document measures against the
bar and never sets it.

**What an evaluation is here.** For governance purposes an evaluation is not a run but a **judgment with a record**: a
named subject, a stated question, a baseline it was compared against, the dimensions examined, and a verdict somebody
is accountable for. A run that produced output but not that record has not evaluated anything.

**The judging concerns, mapped onto the frozen dimensions.** The dimensions are architecture's; what follows is the
*judging* view of what must be decided within each — what a reviewer must be able to ask, and what an honest answer
requires. Each is a responsibility, **not** a new dimension.

| Frozen dimension                            | What judging it requires                                                                                                                                                                                                                                                                                                                                                           |
|---------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Grounding accuracy**                      | Whether each assertion traces to the supplied content, judged against the content rather than against plausibility ([§9](../01-architecture/AI_ARCHITECTURE.md#9-grounding-strategy)). Factual support is this dimension's substance: an output can be entirely true and still ungrounded, and that is a failure here.                                                             |
| **Hallucination rate**                      | Whether unsupported assertions occur, and whether the honest "not found" is produced when the content does not support an answer ([§10](../01-architecture/AI_ARCHITECTURE.md#10-hallucination-mitigation), [BR-033](../00-product/SRS.md#5-business-rules)). A declined answer is a **pass**, and any judging that scores it as a miss inverts the product's intent.              |
| **User acceptance and edit rate**           | Whether output is usable as-is or heavily edited ([PRD §11](../00-product/PRD.md#11-success-metrics)). This is where usefulness, editability, and fitness for human review are judged — an output the professional must rewrite has failed even when every sentence is correct ([BR-031](../00-product/SRS.md#5-business-rules), [BR-032](../00-product/SRS.md#5-business-rules)). |
| **Latency**                                 | Whether time-to-output meets responsiveness expectations ([§14](../01-architecture/AI_ARCHITECTURE.md#14-ai-observability), [NFR-001/002](../00-product/SRS.md#9-non-functional-requirements)). Judged as the user perceives it — against what the interface promises while waiting, not as an isolated duration.                                                                  |
| **Failure rate**                            | How often requests fail or return invalid output ([§12](../01-architecture/AI_ARCHITECTURE.md#12-ai-failure-handling)). Frequency is only half: *how* it fails is judged too, because a clean, recoverable failure and a confident wrong answer are not the same event ([NFR-004/005](../00-product/SRS.md#9-non-functional-requirements)).                                        |
| **Cost efficiency**                         | Spend per successful, accepted output ([§13](../01-architecture/AI_ARCHITECTURE.md#13-ai-cost-management)). Judged relative to accepted output, never to output produced — work the professional discards was never cheap. *How much* is [§13](../01-architecture/AI_ARCHITECTURE.md#13-ai-cost-management)'s decision; this document reports, it does not budget.                 |
| **Consistency across repeated evaluations** | Whether quality is stable for similar inputs across runs and releases ([AI Quality Principles](../01-architecture/AI_ARCHITECTURE.md#ai-quality-principles)). Judged as stability of *quality*, never as sameness of *text* (§7).                                                                                                                                                  |
| **Prompt regression testing**               | Whether a prompt change preserved what the prior version was approved on ([§8](../01-architecture/AI_ARCHITECTURE.md#8-prompt-architecture), [PROMPTS §6](./PROMPTS.md#6-prompt-versioning)). This is the dimension that catches the improvement that cost something elsewhere.                                                                                                    |

**Output shape** is evaluated as part of this track
([TESTING_STRATEGY §7.2](../03-engineering/TESTING_STRATEGY.md#72-ai-evaluation-non-deterministic)) but is first an
architectural obligation: what the product accepts is
[§11](../01-architecture/AI_ARCHITECTURE.md#11-ai-output-validation)'s, and validation rejects what does not conform.
Evaluation asks validation cannot — whether conforming output is *reliably* conforming, or conforms only
when the input is kind.

> **Why the mapping rather than a second model:** two documents naming the same dimensions differently is how the two
> drift, leaving the reader to guess which is authoritative. The dimensions are the architecture's; these are the
> judgments made while examining them. When the two appear to disagree, **AI_ARCHITECTURE's AI Evaluation Strategy
> wins** and this document is corrected.

---

## 4. Evaluation Scope

**What can be evaluated.** Anything whose change can alter what a professional receives:

- **A prompt** and specifically **a prompt version** — the unit a verdict attaches to
  ([PROMPTS §6](./PROMPTS.md#6-prompt-versioning)). "The prompt improved" is not a finding; a named version is.
- **A provider choice or a provider swap** — including a fallback, which is the provider most likely to be evaluated
  only after it has already served a professional ([AI_PROVIDERS §5](./AI_PROVIDERS.md#5-provider-lifecycle)).
- **A model change within a provider** — a different model is a different subject, even under an unchanged provider
  ([AI_ARCHITECTURE §7](../01-architecture/AI_ARCHITECTURE.md#7-model-strategy)).
- **Capability-specific behavior** — a capability is evaluated as itself, never as a share of an average
  ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)).
- **Output shape reliability** — whether the shape the product accepts is produced dependably
  ([§11](../01-architecture/AI_ARCHITECTURE.md#11-ai-output-validation)).
- **Grounding behavior** — including whether refusal happens when it
  should ([§9](../01-architecture/AI_ARCHITECTURE.md#9-grounding-strategy)).
- **Safety behavior** — whether channel separation holds against content that tries to escape it
  ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)). A safety finding is routed, never traded off against
  a quality gain.
- **Fallback behavior** — what a professional actually receives when the primary path fails
  ([§12](../01-architecture/AI_ARCHITECTURE.md#12-ai-failure-handling)). Degraded is a legitimate outcome; *silently*
  degraded is not ([NFR-004](../00-product/SRS.md#9-non-functional-requirements)).

**What is outside scope.** Not because these are unimportant, but because each is owned elsewhere and evaluating them
here would create a second authority:

| Outside scope                                   | Owned by                                                                                                                                                                                                                                                                            |
|-------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Deterministic machinery** around the model    | [TESTING_STRATEGY §7.1](../03-engineering/TESTING_STRATEGY.md#71-application-tests-deterministic) — orchestration, retries, port usage, and validation are *tested*, not evaluated. Asserting them here would make a probabilistic track responsible for a deterministic guarantee. |
| **Retrieval and orchestration design**          | [RAG](./RAG.md) (deferred, [DD-004](../00-product/PRODUCT_DECISIONS.md#4-deferred-decisions)) and [AI_ARCHITECTURE §5](../01-architecture/AI_ARCHITECTURE.md#5-ai-pipeline). Their *effect* is evaluable; their *design* is not this document's.                                    |
| **The provider decision**                       | [AI_PROVIDERS §6](./AI_PROVIDERS.md#6-provider-selection-criteria). This document says which is better on the evidence; that document decides which is used, weighing things evidence does not settle.                                                                              |
| **The prompt itself**                           | [PROMPTS](./PROMPTS.md). A finding is an input to prompt review, never an edit.                                                                                                                                                                                                     |
| **The quality bar and product success metrics** | [AI Quality Principles](../01-architecture/AI_ARCHITECTURE.md#ai-quality-principles) and [PRD §11](../00-product/PRD.md#11-success-metrics). Evaluation measures against the bar; moving the bar is a change to those documents.                                                    |
| **Cost limits**                                 | [AI_ARCHITECTURE §13](../01-architecture/AI_ARCHITECTURE.md#13-ai-cost-management). Evaluation reports spend; it does not set what is affordable.                                                                                                                                   |

---

## 5. Evaluation Lifecycle

An evaluation has stages because the difference between *looking at output* and *establishing a fact the product will
be changed on* is exactly where false confidence forms. Naming the stages makes that boundary explicit.

| Stage        | What it means                                                                                                                                                                                                                                    |
|--------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Proposed** | A question is raised and the decision it serves named. An evaluation with no decision behind it produces a number nobody needed and everyone later cites.                                                                                        |
| **Planned**  | The subject (§4), the dimensions (§3), the baseline (§7), and the inputs are fixed **before the run**. Deciding what counts as better after seeing the output is not evaluation; it is rationalization with a record.                            |
| **Executed** | The evaluation is run as planned, within [SECURITY](../01-architecture/SECURITY.md#10-ai-security)'s constraints. A run that deviated from its plan is reportable as such — the deviation is often the finding.                                  |
| **Reviewed** | The result is judged against the criteria (§6) and the checklist (§9), with human judgment where the question requires it (§8). The outcome is a **finding**, not a decision: it says what is true, never what to do about it.                   |
| **Recorded** | The finding, its subject, baseline, inputs, date, and the judgment are written down and attributable ([ADR-015](../01-architecture/decisions/ADR-015-Observability.md)). An unrecorded finding stops being evidence the moment it is questioned. |
| **Compared** | The finding is set against the baseline and prior findings for the same subject (§7). This is where a regression becomes visible, and it is the only stage that can see one.                                                                     |
| **Repeated** | Re-run when the subject, the baseline, or production reality changes. Repeatability is what distinguishes a finding from a memory — and a finding never re-run is a claim aging quietly.                                                         |
| **Retired**  | No longer used for decisions, because its subject, baseline, or relevance has moved on. Its record stays: *why we stopped measuring it that way* is the most valuable thing to know when someone proposes measuring it that way again.           |

**Every stage has an exit.** *Proposed* ends when a decision is named or the question is dropped. *Planned* ends when
the plan is fixed or the evaluation is abandoned as unanswerable. *Reviewed* resolves to exactly one outcome
(Evaluation Review Process) and never rests: a finding held for another review does not become approved by the passage
of time. *Recorded* is permanent. An evaluation that cannot leave a stage is raised, not left there.

**Experimentation versus production evaluation.** Both are legitimate; conflating them is not. **Experimentation** is
bounded, non-production, uses no real client content
([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security), [NFR-018](../00-product/SRS.md#9-non-functional-requirements)),
and creates no dependency — it produces a *hypothesis*. **Production evaluation** has a plan fixed in advance, a
recorded baseline, a named judge, and a traceable finding — it produces *evidence a decision may rest on*.

A finding MUST NOT reach a production decision by promotion — a promising experiment, quoted in review because it was
the only number available, becoming the basis for a rollout it was never designed to support. An experiment that
matters is re-run as an evaluation. The cost of doing so is a day; the cost of not doing so is a decision nobody can
defend later.

---

## 6. Evaluation Criteria

§3 names **what is measured**. This section governs **how a dimension becomes a criterion something can be judged
against** — and it deliberately does not re-list the dimensions, because a second list is a second authority.

**The anatomy of a criterion.** A criterion is fit to judge on only when all the following are stated:

| It must state      | Why                                                                                                                                                                                                                                                                                                                               |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Its capability** | The same dimension means different things across capabilities: a declined answer is excellent behavior for a document-scoped question and a defect in a summary that had the content ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)). A criterion without a capability is a criterion for none. |
| **Its source**     | The frozen rule, quality principle, or decision it derives from (Rules). A criterion whose source is "we thought this mattered" cannot be argued with — and cannot be corrected when the product's standard changes.                                                                                                              |
| **Its direction**  | What counts as **worse**, explicitly. Direction seems obvious until a dimension moves in a way nobody predicted, and at that moment the direction is decided by whoever wants the change to ship.                                                                                                                                 |
| **Its judge**      | Whether it resolves by measurement or by human judgment (§8), and who is accountable for the call. "The evaluation said so" is not accountability; an evaluation says nothing.                                                                                                                                                    |
| **Its threshold**  | What is good enough — **recorded per capability at review time, never in this document**. Thresholds are contextual and they change; a number frozen here would be obeyed long after it stopped being right.                                                                                                                      |

**Criteria compose; they do not collapse.** A verdict rests on several criteria at once, and they routinely disagree —
that disagreement is information, and it is the first thing a composite score destroys. A single number is permitted
**only** where that number is explicitly meaningful for the decision at hand, and where what it trades off is stated
(Rules). Absent that, criteria are reported as they are: several, sometimes conflicting, and resolved by a judgment
that is written down.

**Review burden is itself a criterion.** An output the professional must verify line by line has not saved them the
hours the product exists to save ([PRD](../00-product/PRD.md), [BR-032](../00-product/SRS.md#5-business-rules)) — even
when it is entirely accurate. Correctness that does not reduce work is a cost the evaluation must be able to see.

**No criterion overrides a safety finding.** Injection resistance, channel separation, and context exposure are not
dimensions to be traded against quality ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)); a concern
there routes to security review and **blocks** (Evaluation Review Process).

---

## 7. Baselines and Regression

**A baseline is a contract for comparison, not a guarantee of sameness.** It records what a subject was judged to do
when it was approved — the dimensions examined, the inputs used, the verdict reached, and the date. It exists so that
the next question, *did this change make things worse?*, has an answer that is not a recollection.

**Why baselines matter.** Without one, every result is absolute and therefore unfalsifiable: the numbers can only be
compared to intuition, and intuition adjusts to whatever it has been looking at. The baseline is the fixed point that
makes a regression *visible* rather than merely *felt* six weeks later, in a support conversation.

**Why identical output is not the goal.** AI output is non-deterministic
([TESTING_STRATEGY §7](../03-engineering/TESTING_STRATEGY.md#7-ai-testing-strategy)); the same input can produce
different words and the frozen bar asks for **reasonably consistent** outputs, not identical ones
([AI Quality Principles](../01-architecture/AI_ARCHITECTURE.md#ai-quality-principles)). A baseline that demands textual
sameness fails every honest run and passes nothing useful — and the pressure it creates is to loosen the baseline until
it certifies anything. Baselines hold **quality stable**, never text.

**How regression is recognized.** A regression is a dimension moving in the worse direction against the recorded
baseline, for the same subject and comparable inputs — regardless of whether other dimensions improved, and regardless
of whether the change was intended to touch that dimension. **An improvement elsewhere is not a defense**; it is the
trade-off that must be stated and judged (§6, §8). Non-determinism means a single worse run is not yet a regression:
what distinguishes them is repeatability (§5), not the strength of anyone's opinion.

**How prompt and provider changes affect baselines.** A baseline belongs to a **subject**, and a prompt version and a
provider are different subjects ([PROMPTS §6](./PROMPTS.md#6-prompt-versioning),
[AI_PROVIDERS §5](./AI_PROVIDERS.md#5-provider-lifecycle)). A change to either invalidates the comparison unless the
other is held still — which is why the two are distinguished (Rules). When both must move, the evaluation says so and
reports a finding that attributes to the pair, never one that quietly credits whichever change was hoped to help.

**How baseline changes are approved.** Re-establishing a baseline is a **reviewed decision**, never a side effect of a
release (Evaluation Review Process). It requires a stated reason — the capability changed, production reality moved,
the prior inputs stopped being representative — and the prior baseline is **retained**, not overwritten. A baseline
replaced silently is how a regression becomes the new normal without anyone having decided it: the numbers look fine,
because the question changed.

---

## 8. Human Review and Qualitative Judgment

Measurement answers *what happened*. It does not answer *whether that is acceptable*, and the gap between those two is
where this product's quality actually lives.

**Human judgment is required when:**

- **Output is ambiguous** — technically supported by the content, but open to a reading the professional did not
  intend. No metric distinguishes "correct" from "correct and misleading."
- **Criteria trade off against each other** — one dimension improved and another declined. Only a person can decide
  which the professional would rather have, and only a person can be accountable for deciding wrong (§6).
- **The decision is capability-specific** — what "good" means for a client email is not what it means for a report, and
  the difference is professional judgment, not arithmetic
  ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)).
- **The change is safety-sensitive** — anything touching injection resistance, channel separation, or what reaches a
  model ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)). These are judged, then routed; they are never
  averaged.
- **Providers cannot be separated by numbers** — where candidates are close, or better on different dimensions, the
  choice is a judgment about which strengths this product needs
  ([AI_PROVIDERS §6](./AI_PROVIDERS.md#6-provider-selection-criteria)).
- **Output is technically correct but operationally poor** — accurate, grounded, and still not what a Chartered
  Accountant would send a client. This failure is invisible to every dimension in §3 and obvious to any professional
  reading it.

**Why human review remains necessary.** LedgerAI assists a professional who stakes their name on the output
([BR-032](../00-product/SRS.md#5-business-rules)); the standard is therefore *professional acceptability*, which is not
a measurable property of text. Every attempt to fully proxy it selects for what the proxy rewards — and the proxy
always rewards confident, fluent, complete-looking output, which is precisely the failure mode the frozen quality bar
exists to prevent.

Human judgment is **not** an escape hatch from the rules. A judgment is subject to the same discipline as a
measurement: it names its subject, states its reasoning, and is recorded and attributable (Rules). "It felt better" is
not a finding, whoever says it.

---

## 9. Evaluation Review Checklist

Every evaluation — new or repeated — is assessed against this checklist before its finding is acted on. A "no" is a
finding to resolve, not a detail to defer.

- [ ] **Purpose documented?** — The decision this evaluation serves is named (§5).
- [ ] **Capability identified?** — The subject is a specific capability and, where relevant, a specific version
  (§4, [AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)).
- [ ] **Criteria defined?** — Each states its capability, source, direction, judge, and threshold (§6).
- [ ] **Baseline recorded?** — The comparison point exists, is stated, and was not re-established in the same change
  (§7).
- [ ] **Repeatability considered?** — The result can be re-produced closely enough to compare; a single sample is not
  reported as a trend (§5, Rules).
- [ ] **Regression risk assessed?** — Dimensions the change was *not* aimed at were examined, not assumed
  (§3, §7).
- [ ] **Prompt/provider distinction handled?** — Attribution is unambiguous, or the finding says explicitly that it is
  not (Rules).
- [ ] **Human review completed?** — Where the question is a trade-off, an ambiguity, or safety-sensitive, a person
  judged it and is named (§8).
- [ ] **Result traceable?** — Subject, baseline, inputs, date, and judgment recorded and attributable
  ([ADR-015](../01-architecture/decisions/ADR-015-Observability.md)).
- [ ] **Safe to act on?** — The finding supports the decision being made with it, and no safety concern is outstanding
  (§6, [SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)).

---

## Evaluation Review Process

> *Unnumbered governance section. It defines when an evaluation is reviewed and how evaluation governance evolves —
> deliberately, not by accident.*

**Review triggers** — an evaluation review is required when any of the following occurs:

- **A new prompt version** ([PROMPTS §6](./PROMPTS.md#6-prompt-versioning)).
- **A prompt change** — any edit to an approved version; size is not a proxy for impact
  ([PROMPTS](./PROMPTS.md#prompt-review-process)).
- **A new provider** is proposed ([AI_PROVIDERS §5](./AI_PROVIDERS.md#5-provider-lifecycle)).
- **A provider or model change** — including a fallback, and including a change made by the provider to a model already
  in use ([AI_ARCHITECTURE §7](../01-architecture/AI_ARCHITECTURE.md#7-model-strategy)).
- **A new AI capability** is introduced — it needs criteria and a baseline before it has a history.
- **A baseline change** — proposed re-establishment of a comparison point (§7).
- **A regression concern** — a dimension moved the wrong way, whether measured or reported from production
  ([§14](../01-architecture/AI_ARCHITECTURE.md#14-ai-observability)).
- **A safety concern** — anything touching injection resistance, channel separation, or context exposure
  ([SECURITY §10](../01-architecture/SECURITY.md#10-ai-security)).
- **An architecture change** — the pipeline, channel model, validation, or the dimensions themselves change
  ([AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md)).
- **A product rule change** — a rule a criterion derives from changes in [SRS](../00-product/SRS.md); the criterion
  follows, never the reverse.

**Review outcomes** — each review resolves to exactly one. (These are *dispositions of the review*; they are distinct
from what an evaluation **determines**, which is
[AI_ARCHITECTURE's](../01-architecture/AI_ARCHITECTURE.md#review-outcomes).)

- **Approved** — the finding is sound and may be acted on; it is recorded with its subject and baseline (§5, §7).
- **Refinement required** — the question is sound but the evaluation is not yet trustworthy: an unstated baseline, an
  unrepeatable result, a criterion with no source, ambiguous attribution.
- **Provider review required** — the finding bears on provider selection, capability, or a swap; it is routed to the
  [Provider Review Process](./AI_PROVIDERS.md#provider-review-process). **This document supplies evidence; it never
  selects a provider.**
- **Prompt review required** — the finding bears on prompt content, structure, or a version; it is routed to the
  [Prompt Review Process](./PROMPTS.md#prompt-review-process). **A finding is never applied as a prompt edit here.**
- **Security review required** — injection resistance, channel separation, or context exposure is in question; it is
  routed to the security review process
  ([SECURITY](../01-architecture/SECURITY.md#ai-changes--review-required-for)). This outcome **blocks**.
- **Architecture review required** — the finding implies a change to the dimensions, the pipeline, the quality bar, or
  validation; it is raised per [CLAUDE.md §8](../../CLAUDE.md) before proceeding.
- **ADR required** — the decision is significant, precedent-setting, or hard to reverse.

Every outcome resolves to a lifecycle position (§5). Only **Approved** allows a finding to be acted on; **Refinement
required** returns it to *Planned* or *Executed* depending on what was unsound; every other outcome holds it at
*Reviewed* until the review it was routed to returns. A finding held there is not approved by the passage of time.

**Synchronization.** Evaluation governance MUST remain synchronized with
[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md),
[TESTING_STRATEGY](../03-engineering/TESTING_STRATEGY.md), [AI_PROVIDERS](./AI_PROVIDERS.md),
[PROMPTS](./PROMPTS.md), [RAG](./RAG.md), [SECURITY](../01-architecture/SECURITY.md), and the relevant
[ADRs](../01-architecture/decisions/). When this document and one of them disagree, **they win** and this document is
corrected ([CLAUDE.md §3](../../CLAUDE.md)). A change touching more than one is reviewed by each document it touches and
MUST NOT be merged into one while leaving another contradicting it — a criterion change that redefines what a provider
is judged on, invalidates a prompt version's approval, or alters what the test strategy expects is a change to those
documents too.

---

## 10. Evaluation Decision Summary

The load-bearing decisions behind evaluation governance, recorded so they are not silently reversed. The **quality bar**
and the **evaluation dimensions** are
[AI_ARCHITECTURE's](../01-architecture/AI_ARCHITECTURE.md#ai-evaluation-strategy) decisions and are not restated here;
these are the governance decisions built on top of them.

| Decision                                      | Chosen Approach                                                                                       | Alternatives                                                                  | Rationale                                                                                                                                                                                                                                                                                      |
|-----------------------------------------------|-------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Evaluation as governance, not a scorecard** | Evaluation produces findings that feed reviews; the verdict is a judgment somebody owns               | Publish a quality dashboard and treat the numbers as the decision             | A scorecard answers whoever reads it. Numbers do not know what they traded off, and a decision with no accountable name behind it cannot be defended when it turns out wrong (§5, §8).                                                                                                         |
| **Repeatable baselines**                      | Every subject has a recorded baseline; re-establishing one is reviewed and the prior is retained      | Compare against the last release; or re-baseline whenever the product changes | A baseline that moves with the product certifies the product. Retaining the prior is what makes "when did this get worse?" answerable at all — silent re-baselining is how a regression becomes the new normal (§7).                                                                           |
| **Capability-specific evaluation**            | Criteria, baselines, and thresholds are set per capability                                            | One quality standard across all AI capabilities                               | LedgerAI's capabilities demand different things; a summary and a client email fail differently. An average across them describes none and conceals the one that broke ([AI_ARCHITECTURE §3](../01-architecture/AI_ARCHITECTURE.md#3-ai-capability-map)).                                       |
| **Human judgment alongside metrics**          | People judge trade-offs, ambiguity, and professional acceptability; measurement judges the rest       | Fully automate evaluation for objectivity and throughput                      | The standard is what a professional will put their name on ([BR-032](../00-product/SRS.md#5-business-rules)) — not a measurable property of text. Every proxy for it rewards confident, fluent output, which is the failure mode the quality bar exists to prevent (§8).                       |
| **Regression-aware review**                   | Dimensions the change did not target are examined; an improvement elsewhere is not a defense          | Evaluate what the change was intended to affect                               | Almost every AI change improves its target. The risk is the unexamined dimension that quietly declined — measuring only the intention guarantees it is never found until a professional finds it (§7).                                                                                         |
| **Prompt/provider distinction**               | Findings attribute to a prompt version or a provider; when both move, the finding says so             | Evaluate the system end-to-end and attribute to whatever changed              | An unattributable result must be re-run, because the next question is always which one it was. Conflating them also lets a provider accommodation hide inside a prompt result, undoing [ADR-003](../01-architecture/decisions/ADR-003-AI-Provider-Abstraction.md) (Rules).                     |
| **No composite score by default**             | Criteria are reported as several, with trade-offs stated; one number only where explicitly meaningful | Combine criteria into a single quality score for comparability                | A composite hides the trade-offs someone chose behind arithmetic nobody re-derives, and it keeps rising while the dimension that matters falls. It is also the number that gets quoted in the decision (§6).                                                                                   |
| **Production-relevant evaluation**            | Inputs and criteria are held to what production actually looks like                                   | Use stable curated inputs indefinitely for clean comparability                | Inputs frozen forever eventually measure the evaluation rather than the product; the results stay comparable and stop being true. Relevance decays quietly, which is why drifting away from production is a trigger to re-baseline (§7).                                                       |
| **Traceable findings**                        | Subject, baseline, inputs, date, and judgment are recorded and attributable                           | Record conclusions; keep the detail informal                                  | A conclusion without provenance cannot be re-examined when challenged, and stale evidence is most persuasive exactly when it is most wrong. Traceability is what lets a finding be *retired* rather than silently outlived ([ADR-015](../01-architecture/decisions/ADR-015-Observability.md)). |

---

*This document governs how LedgerAI's AI quality is measured and judged; it does not override the frozen documents under
[`docs/`](../). It measures against the bar set by
[AI_ARCHITECTURE](../01-architecture/AI_ARCHITECTURE.md#ai-quality-principles) and along the dimensions it owns, runs as
the track [TESTING_STRATEGY](../03-engineering/TESTING_STRATEGY.md#7-ai-testing-strategy) defines, supplies findings to
[AI_PROVIDERS](./AI_PROVIDERS.md) and [PROMPTS](./PROMPTS.md) without making their decisions, respects
[SECURITY](../01-architecture/SECURITY.md), and anticipates [RAG](./RAG.md) without depending on it. When an evaluation
finding would imply new product behavior, a different quality bar, or a change to a ratified decision, stop and raise it
per [CLAUDE.md §8](../../CLAUDE.md).*
