# Architecture lineage & mining record

Durable knowledge for agent sessions. Facts below were verified by
reading the upstream sources on 2026-07-26; re-verify before relying
on details that may have moved.

## Upstream sources

**Google Simulated Hospital** (github.com/google/simhospital,
archived, Apache-2.0, Go) — mined for the *operational* model:

- `pkg/pathway`: ~30 scripted step types incl. ADT churn
  (Transfer, BedSwap, TransferInError, Cancel*, Pending*, Merge,
  DeleteVisit...). YAML pathways picked by `percentage_of_patients`
  via a distribution manager. Our IR step vocabulary derives from
  this list; churn realism is its key contribution.
- `pkg/state` + `pkg/hospital`: discrete-event core — priority queue
  of Event structs, patients map, `RunNextEventIfDue` loop. Our
  `engine.clj` is its functional reduction (pure fold, retained
  state history).
- `configs/`: order profiles (lab panels with ref ranges/units —
  codes admittedly synthetic), UK census names, locations, doctors.
  UK-centric: NHS numbers, mmol/L units, London ethnicities. We do
  NOT port UK config; US data comes from Synthea.
- `pkg/hospital/messages.go` + `pkg/message`: event→HL7v2 mapping
  (ADT^A01 etc.) — reference for our emitter, but emission is built
  on the cmiles74 parser's structures instead.

**Synthea** (github.com/synthetichealth/synthea, Apache-2.0, Java) —
mined for the *generative clinical* layer and US data:

- 85 disease modules as plain JSON (`src/main/resources/modules/`),
  each a probabilistic state machine (Generic Module Framework:
  Initial/Delay/Simple/ConditionOnset/MedicationOrder/Encounter...
  with direct/distributed/conditional/complex transitions guarded on
  age/sex/race/attributes). Verified: asthma module = 52 states.
- Modules embed real codes inline: SNOMED (conditions), LOINC
  (labs), RxNorm (meds). Apache-licensed → redistributable.
- US-centric throughout: census demographics, geography, SSNs, US
  units. This is our US data source.
- Plan: port the GMF *interpreter* (documented spec on their wiki),
  load their module JSON as data, compile module runs → our IR.
  A Synthea module is a pathway generator; a simhospital pathway is
  one sample from it.

**HL7v2 in Clojure:** org.clojars.cmiles74/clojure-hl7-parser 3.5.1
(verified from the project's project.clj). Group is
`org.clojars.cmiles74`, artifact `clojure-hl7-parser` — NOT the
GitHub repo name.

## Terminology decisions

- Concepts as `{:system :code :display}` in IR/log; emitters render
  natively (HL7v2 CWE, FHIR CodeableConcept). Codes are state, not
  format (ADR-0002).
- Systems: SNOMED CT, LOINC, RxNorm, ICD-10-CM, CVX. **Never CPT**
  (AMA-licensed). SNOMED→ICD-10-CM for DG1/billing via the NLM map,
  or carry both codes where Synthea provides them.

## Sibling-repo integration (ehr-testing-tools)

Verified conventions we mirror (their `src/ehr_testing_tools/`):
result-not-throw Result maps; thin printing CLI shell, `[group
action]` dispatch with injectable `-fn` keys; EDN canonical/`--json`
projection; exit codes 0/1/2/3 (their ADR-0004/0010); help data as
`{:group :doc :verbs [{:verb :doc :flags [...]}]}`; corpus manifests
ManifestV1_1 (`:stage :generator :seeds :engine-params :config
:invocation :canonicalizers-applied :environment`). Our mirrors:
`cli/help-group`, `manifest.clj`. Binding contract tests go in THEIR
test-integration tree (ADR-0001 here).

## Validation program

`docs/problem-statement.md` §Validation & Evidence is the claims/
proofs table (7 claims). Rows 1–3, 7 are CI-automatable now; the
invariant catalog (`check.clj`) is claim #3's engine and doubles as
the regression suite.
