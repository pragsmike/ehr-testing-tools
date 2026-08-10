# What is this?

**Reproducible test data for EHR integrations — generated, broken on
purpose, and conformance-gated, end to end.** One workspace, offline,
deterministic, no real patient data anywhere in it.

## The problem

Teams that build, integrate, or test EHR-adjacent systems need
realistic hospital message traffic to develop against, and they can't
get it.

Real production feeds are the gold standard of realism but are
effectively unusable: protected health information gates access behind
HIPAA compliance, business associate agreements, and de-identification
processes that are slow, expensive, and legally risky — and even
cleared data can't be freely shared across teams, checked into
repositories, or published in a bug report.

The common fallback — hand-crafted test messages — fails in the
opposite direction. Manually written messages are sparse, static, and
unrealistically clean. They exercise the happy path and miss precisely
the phenomena that break real systems: administrative corrections,
cancellations, patient-record merges, bed moves, out-of-order events,
duplicate registrations, and the sheer volume and interleaving of
concurrent patient journeys. A test suite built from a folder of
sample messages gives false confidence — and even a folder of
realistic-looking messages proves nothing about whether the *system
under test* actually rejects what it should.

A complete test plan needs three layers, in order: a **generated**
corpus, clinically plausible and operationally messy, at volume; a
**mutation** layer that takes that good data and deliberately breaks
it, with full provenance, to prove the test suite can actually catch a
defect; and **conformance gates**, structurally upstream of any
semantic check, that verify a message or resource is legal before
anything asks whether it's *right*. Existing tools each cover part of
this: some generate clinically rich but operationally sterile records
(no admit/discharge/transfer traffic, no operational noise); others
produce realistic message flows but require every scenario scripted by
hand. None combine clinical plausibility, operational messiness, US
coding conventions, controlled defect injection, and structural
conformance gating in one place.

This workspace is that place.

## What it does

- **Generate** — a deterministic, seeded simulator produces clinically
  coherent, operationally messy hospital traffic (admissions,
  transfers, discharges, orders, results, the administrative churn
  real feeds are full of), or wraps Synthea for longitudinal
  population generation. Same seed and config, byte-identical output,
  always.
- **Mutate** — deliberately breaks generated (or foreign) data with a
  registered catalog of defect operators, each labelled with exactly
  what it changed, where, and which conformance constraint the result
  now violates — provenance a test suite can assert against, not a
  guess.
- **Gate** — conformance-checks a corpus against HL7 v2 (via HAPI) and
  FHIR (via the official validator) base-structural rules, upstream of
  any semantic judgment, with baseline-relative diffing so a change in
  verdicts is always reviewable.
- **Check** — the corpus's second judge alongside Gate: golden
  equivalence against an expected corpus, plus a small per-file
  assertion vocabulary, for callers with their own notion of "correct"
  beyond base-spec conformance.

Every stage's output is plain FHIR JSON, HL7v2 ER7 text, and EDN
manifests — readable from Python, SQL, or anything else. Clojure
inside; no Clojure skills required to use it.

## Audience

- **Integration engineers** building or configuring interface engines,
  message routers, and transformation pipelines who need continuous,
  varied HL7/FHIR traffic to develop and load-test against.
- **EHR and health-IT developers** implementing inbound interfaces
  (ADT consumers, results ingestion, patient-index/merge logic) who
  need adversarially messy input, not clean samples.
- **QA and test-automation teams** who need reproducible scenarios —
  including rare edge cases like merge-after-transfer-in-error — that
  can be pinned by seed and replayed in CI, plus defect-injected
  corpora that prove their own checks actually catch something.
- **Interface analysts and clinical informaticists** deciding whether
  to trust a claim this workspace makes, or judging a gate's findings
  against their own site's interface definition.
- **Standards and interoperability developers** validating conformance
  of parsers, validators, and converters.
- Secondarily: **health-data platform/analytics teams** and
  **researchers** needing populated pipelines without touching PHI.

## Constraints

1. **No PHI, ever.** All persons, identifiers, and clinical content are
   synthetic by construction, not by de-identification — including in
   test fixtures and docs.
2. **US conventions.** US demographics and geography; US identifier
   schemes (MRN, SSN-shaped, never real); US units of measure.
3. **Standard terminology, no licensed vocabularies.** Clinical
   concepts carry real codes from freely usable systems — SNOMED CT,
   LOINC, RxNorm, ICD-10-CM, CVX. No AMA-licensed CPT content, ever
   (`sim/F4`).
4. **Offline and deterministic.** No network access at execution time;
   any externally-fetched artifact (engines, validators, runtimes) is
   acquired once, pinned, and cached locally. Identical inputs and
   pinned artifacts always produce identical output — a change in
   result always means a change in inputs, code, or pinned artifacts,
   never environmental drift.
5. **JVM, Clojure-native, usable without Clojure.** A CLI (`bin/ehrt`)
   is the supported surface for everyone who isn't writing Clojure;
   the same capabilities are callable in-process for anyone who is.
6. **Permissive licensing.** Redistributable under open-source terms
   throughout the dependency tree — verified, not assumed (see
   `notes/facts-register.md`).
7. **Encounter horizon (generation).** The simulator's scope is
   hospital-operations traffic across a single encounter — admission
   through discharge and its immediate churn — not a patient's
   lifelong longitudinal history, which Synthea already serves and
   this workspace can also wrap directly[^sim-adr-0007].

## Validation & evidence

A skeptical reader — an interface analyst, informaticist, or QA lead
deciding whether to trust this traffic — is entitled to proof, not
assertion, for every claim above. Each has a distinct proof strategy
this workspace actually carries out, not just states:

| Claim | Proof strategy |
|---|---|
| Syntactic validity | Every emitted message/resource is gated by independent tooling this workspace didn't write (HAPI, the official FHIR validator) — never graded by its own homework (`sim/F6`). |
| Terminology correctness | Coded elements cross-checked against official code-system releases; codes travel as `{:system :code :display}` triplets from source data to every emitter, never invented[^sim-adr-0002]. |
| Internal consistency | Property-based testing over the ground-truth log — invariants (identifier stability, no discharge-before-admit, results follow orders, timestamps monotone) machine-checked across thousands of randomized runs, not asserted once. |
| Clinical plausibility | Provenance to established, peer-reviewed prior art (Synthea's GMF modules) rather than re-arguing clinical content from scratch. |
| Operational realism | Pedigree (Simulated Hospital's own NHS-deployment-informed design), anchoring to published throughput data, and site-tunable churn rates so a team can calibrate against their own feed's statistics instead of trusting defaults. |
| Fitness as a test instrument | Ecological validation: real defects surfaced by running generated/mutated output through real consumers, documented as case studies — not a synthetic-data claim taken on faith. |
| Safety (no PHI) | Proof by construction: deterministic rules plus public statistical tables, no real patient records anywhere in the generation path — memorization or leakage is structurally impossible, not merely improbable. |

**Glass-box auditability.** All clinical content is inspectable data,
not opaque code or model weights — an expert can open a module and
read its codes, incidence probabilities, and state transitions
directly. **Reproducibility as evidence:** because identical config +
seed yields byte-identical output, every validation artifact
(conformance reports, invariant checks) is independently re-runnable
by the skeptic on their own machine, not a claim about past testing.

## Scope — what this deliberately does not do

- **Semantic correctness checking.** Properties, metamorphic relations,
  and golden-case comparison remain the caller's own code, written
  against their own transforms.
- **Full terminology validation against licensed vocabularies** (e.g.
  complete SNOMED CT bindings) — that imports licensing and
  distribution problems this workspace does not take on.
- **Production message routing or integration-engine functionality** —
  these are test-time tools, not runtime infrastructure.
- **A hosted, public validation service.** Local deployment is the
  target.

## Relationship to `ehr-testing-guide`

The two exist for different purposes: [`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide)
teaches the testing method — what correctness means for a lossy
transform, how a corpus should be layered, where validation sits
relative to semantic checks; this workspace makes that method
runnable. See [`docs/dev/AUDIENCES.md`](dev/AUDIENCES.md) for the
fuller map, including why the guide doesn't cite this workspace yet.

Get started: [`README.md`](../README.md#quickstart).

[^sim-adr-0002]: Design record [sim/ADR-0002](../notes/sim/ADRs.md).
[^sim-adr-0007]: Design record [sim/ADR-0007](../notes/sim/ADRs.md).
