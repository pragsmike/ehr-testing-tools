# Synthetic Hospital Traffic for EHR Testing — Problem Statement

## The Problem

Teams that build, integrate, or test EHR-adjacent systems need realistic hospital message traffic to develop against, and they cannot get it.

Real production feeds are the gold standard of realism but are effectively unusable: they contain protected health information, so access is gated by HIPAA compliance, business associate agreements, and de-identification processes that are slow, expensive, and legally risky. Even when access is granted, production data cannot be freely shared across teams, checked into repositories, or published in bug reports.

The common fallback — hand-crafted test messages — fails in the opposite direction. Manually written messages are sparse, static, and unrealistically clean. They exercise the happy path and miss precisely the phenomena that break real systems: administrative corrections, cancellations, patient-record merges, bed moves, out-of-order events, duplicate registrations, and the sheer volume and interleaving of concurrent patient journeys. A test suite built from a folder of sample messages gives false confidence.

Existing synthetic-data approaches each cover only part of the need. Some produce clinically rich but operationally sterile records — complete patient histories with no admit/discharge/transfer traffic, no message-level realism, and no operational noise. Others produce realistic operational message flows but require every clinical scenario to be scripted by hand, so generating varied traffic at scale demands proportional manual authoring effort. None combine clinical plausibility, operational messiness, US coding and identifier conventions, and minimal-input generation in one tool.

The gap: **there is no way to turn a small set of parameters into an unbounded stream of clinically coherent, operationally realistic, properly coded, safely shareable hospital events.**

## Audience

- **Integration engineers** building or configuring interface engines, message routers, and transformation pipelines who need continuous, varied HL7 traffic to develop and load-test against.
- **EHR and health-IT developers** implementing inbound interfaces (ADT consumers, results ingestion, patient-index/merge logic) who need adversarially messy input, not clean samples.
- **QA and test-automation teams** who need reproducible scenarios — including rare edge cases like merge-after-transfer-in-error — that can be pinned by seed and replayed in CI.
- **Health-data platform and analytics teams** who need populated pipelines and databases without touching PHI.
- **Standards and interoperability developers** validating conformance of parsers, validators, and converters.
- Secondarily: **researchers and ML practitioners** needing synthetic longitudinal patient event data.

## Environment Expectations

- Runs as a library or standalone process on the JVM; usable from Clojure and, by extension, any JVM language.
- Operates entirely offline and self-contained: no connection to real clinical systems, no terminology-server dependency at runtime, no network requirement.
- Suitable for laptops and CI runners alike; no special hardware.
- Output is consumed by systems under test through ordinary channels — files, streams, or network delivery (e.g., MLLP) — and by test harnesses as data structures.
- Contains no real patient data by construction, so artifacts can be committed, shared, and published without review.

## Constraints

1. **No PHI, ever.** All persons, identifiers, and clinical content are synthetic by construction, not by de-identification.
2. **US conventions.** US demographics and geography; US identifier schemes (MRN, SSN); US units of measure; US coding systems.
3. **Standard terminology.** Clinical concepts carry real codes from freely usable systems — SNOMED CT, LOINC, RxNorm, ICD-10-CM, CVX. No AMA-licensed CPT content.
4. **Clinical plausibility.** Generated trajectories must be medically coherent: conditions onset with realistic incidence by age/sex/demographics; orders, results, and medications follow from conditions; values fall in plausible ranges with plausible abnormals.
5. **Operational realism.** Traffic must include the administrative churn of real hospitals — transfers, bed swaps, corrections, cancellations, error-entries, merges — at configurable rates.
6. **Minimal input.** A useful stream must be obtainable from a small parameter set; realism must not require hand-authoring scenarios. Hand-authored scenarios remain possible for targeted edge-case testing.
7. **Reproducibility.** Identical parameters and seed produce identical output; any interesting stream can be pinned and replayed.
8. **Format-agnostic core.** The simulation models patient state and events independently of any wire format; concrete formats are renderings. Event-based output (HL7v2) is required first; state-based outputs (FHIR, CDA) must be addable without altering the core.
9. **Permissive licensing.** The library and its bundled content must be redistributable under open-source terms.

## Black-Box Description

### Inputs

A configuration (data, not code) specifying:

- **Population & time:** number of patients or arrival rate; simulated time window (historical batch, real-time paced, or accelerated).
- **Demographics:** distribution parameters for age, sex, ethnicity, geography (defaults provided).
- **Clinical content selection:** which disease/care modules are active and their relative weights (defaults provided).
- **Facility model:** wards, bed capacities, departments, provider pool (defaults provided).
- **Churn profile:** probabilities for operational-noise events — transfers, corrections, cancellations, merges, bed swaps.
- **Output selection:** which formats/message families to emit and delivery target (file, stream, network).
- **Random seed.**

Optionally: hand-authored scenario scripts for specific patients, in the same representation the generator produces.

### Outputs

- **A time-ordered stream of HL7v2 messages** (ADT, ORM/OMG, ORU, and related families) representing interleaved patient journeys across the facility — syntactically valid, internally consistent (identifiers, encounter numbers, and timestamps cohere across a patient's messages), and carrying standard terminology codes.
- **A ground-truth trajectory log:** for each patient, the canonical sequence of state changes and events that produced the messages — enabling test assertions ("the system under test should now believe patient X is in bed Y with condition Z") independent of message parsing.
- *(Later, same core:)* **state-based renderings** — FHIR resources or CDA documents representing any patient's state at any simulated moment.

### Guarantees

- Same inputs + seed ⇒ byte-identical output.
- Every emitted message is derivable from the ground-truth log, and vice versa.
- No output artifact contains information about any real person.

## Validation & Evidence

A skeptical domain expert — an interface analyst, clinical informaticist, or QA lead deciding whether to trust this traffic — is entitled to proof for each claim the tool makes. Each claim below has a distinct proof strategy; together they form the validation program.

| # | Claim | What the skeptic asks | Proof strategy |
|---|-------|----------------------|----------------|
| 1 | **Syntactic validity** | "Do these messages actually parse?" | Validate the emitted corpus with independent tooling not written by us: NIST HL7v2 conformance tools and the HAPI parser. Round-trip through at least one parser other than our own emitter's counterpart. Publish the conformance report per release. |
| 2 | **Terminology correctness** | "Are these real codes, used correctly?" | Automated cross-check of every emitted coded element against official code-system releases (LOINC table, SNOMED CT RF2, RxNorm, ICD-10-CM, CVX): code exists, display text matches, units are valid for the analyte, reference ranges match. Plus provenance: codes originate in clinically-reviewed upstream modules, not invention. |
| 3 | **Internal consistency** | "Does the stream contradict itself?" | Property-based testing over the ground-truth log: define invariants (identifier stability, no discharge-before-admit, results follow orders, merges reference existing MRNs, timestamps monotone per patient, encounter numbers cohere) and machine-check them across millions of generated events under randomized configurations. Report the invariant catalog and event volumes checked. |
| 4 | **Clinical plausibility** | "Would a clinician find these trajectories credible?" | Provenance to established prior art: clinical content derives from Synthea's Generic Module Framework modules, peer-reviewed (Walonoski et al., JAMIA 2018) and publicly scrutinized for years — we inherit that validation rather than re-arguing it. Supplement with statistical comparison of output distributions (demographics, condition incidence by age/sex, lab value distributions) against public references: census data, CDC prevalence statistics. |
| 5 | **Operational realism** | "Does this look like a real ADT feed?" | Weakest provenance available anywhere, addressed three ways: (a) pedigree — the operational event vocabulary derives from Simulated Hospital, built by Google Health against real NHS deployment experience; (b) anchoring — length-of-stay, admission-mix, and throughput parameters tied to published data (AHRQ/HCUP, ED throughput studies); (c) calibration — churn rates and event mixes are configurable, so a site can tune the generator to match statistics observed on its own feed, replacing "trust our defaults" with "match your hospital." |
| 6 | **Fitness as a test instrument** | "Does passing against this traffic predict surviving production?" | Ecological validation: run the output through established open-source consumers (interface engines, HAPI-based pipelines), document what it exercises and what it breaks, and publish case studies of real defects surfaced — especially by churn sequences (merge after transfer-in-error, cancelled discharge) that hand-crafted test data never contains. |
| 7 | **Safety (no PHI)** | "Could any of this be a real person?" | Proof by construction: the generator is deterministic rules plus public statistical tables, with no real patient records anywhere in its inputs or training. Unlike ML-based synthetic data, memorization or leakage of real records is structurally impossible, not merely improbable. |

### Cross-Cutting Arguments

**Provenance.** The two load-bearing knowledge sources are established, reviewed prior art: Synthea (clinical content and coding; MITRE; peer-reviewed and widely used in research and industry) and Simulated Hospital (operational model; Google Health). The novel contribution — composing a generative clinical layer with an operational messaging layer — adds orchestration, not new clinical claims, so the surface requiring fresh validation is deliberately small.

**Glass-box auditability.** All clinical content is inspectable data, not opaque code or model weights: an expert can open the asthma module and read its SNOMED codes, incidence probabilities, and state transitions directly. The engine is deterministic and seeded, so any output event is traceable to the module state and random draw that produced it. "Don't trust us — read the data" is a stronger posture than any black-box generator can offer.

**Reproducibility as evidence.** Because identical config + seed yields byte-identical output, every validation artifact — conformance reports, invariant checks, distribution comparisons — is independently re-runnable by the skeptic on their own machine. Validation is not a claim about past testing; it is a standing, repeatable demonstration.
