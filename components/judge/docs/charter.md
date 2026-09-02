# Charter — `judge`

> **Draft for the author's edit.** Derived from
> `src/ehrt/judge/interface.clj` and the five namespaces it delegates
> to, their own docstrings, and the ADRs those docstrings cite.
> **UNCLEAR** marks a contract the shipped surface does not settle.

## 1. Mission

Own the **verdict vocabulary** every judge engine reports in — reports,
findings, severity ordering, the verdict cache, the mutate↔judge
pairing registry, and stratified sampling for gating at scale — while
running **no validation engine of its own**.

ADR-0011, the per-engine judge split: the gate functions themselves
moved to `judge-v2-hapi`, `judge-v2-nist` and `judge-fhir-official`.
This interface **no longer re-exports them**; every consumer requires
each engine's own interface directly since stage 3 retired the `tools`
façade's qualified relays (ADR-0018).

## 2. Interface contract

### Reports (`judge.report`)

Qualified `report-*` because `report` and `finding` collide with each
other on `valid?` — a collision that survived their original partner
(`result/valid?`) leaving this component at the kernel/judge
extraction.

- `Report` — the report schema.
- `build-report` — findings → a Report.
- `diff-reports` — two Reports → their difference.
- `baseline-relative-report` — a Report expressed relative to a
  baseline, which is what regression-style gating compares.
- `report-valid?` — `(report-valid? r)`.

### Findings (`judge.finding`)

- `finding-valid?` — `(finding-valid? f)`.
- `worst-of` — the severity join: several findings → the worst.
  A **new re-export** at ADR-0011, found necessary only by actually
  running `poly check` after the move — `judge-fhir-official.fhir`
  genuinely calls it, which is the cross-brick internal-namespace
  reach Polylith forbids once the two live in different bricks.
  **Fixed by routing through this interface rather than narrowing the
  call away.**

### The verdict cache (`judge.verdict-cache`)

The other half of that same `poly check` finding: four functions
`judge-fhir-official` genuinely calls.

- `verdict-cache-key` — the cache key for a verdict.
- `verdict-cache-lookup` — read a cached verdict.
- `verdict-cache-store!` — write one.
- `verdict-cache-default-dir` — where the cache lives by default.

### Pairing-as-data (`judge.pairing`, ADR-0088, AR-PD-1)

The mutate↔judge **witnessed-row registry**: which mutation a given
judge is known to catch, held as data rather than as code.

- `PairingRow` — one witnessed row.
- `PairingRegistry` — the registry schema.
- `PairingJudgeId` — a judge's identity within it.
- `load-pairing-registry` — load the registry.
- `pairing-coverage` — the registry's coverage over a population.

### Stratified sampling (ADR-0175 design (h), ruling D1)

**A pure selection over corpus metadata**, exported for `bases/cli`'s
`gate v2 --sample-add-ons`. **The classification set is the caller's**
— which is what keeps this component free of any dependency on the
emitter whose registry defines it.

- `sampling-unknown-stratum` — the stratum an unclassifiable entry
  falls into.
- `sampling-header` — `(sampling-header content)` → the classifying
  header of one entry.
- `stratified-selection` — `(stratified-selection entries opts)` → the
  sampled selection.
- `render-strata` — `(render-strata strata)` → the strata, rendered.

## 3. Data shapes owned

| shape | what it fixes |
|---|---|
| `Report` | the verdict document every engine produces |
| the **finding** | one defect, with its severity |
| the **severity order** | via `worst-of` — the join that makes severities comparable |
| the **verdict cache** key and layout | |
| `PairingRegistry`, `PairingRow`, `PairingJudgeId` | the witnessed mutate↔judge rows |
| the **strata** | the sampling vocabulary, minus the classification set |

## 4. Invariants guaranteed

- **One verdict vocabulary across every engine.** Three engines, one
  `Report` shape, one severity join. That is the whole point of the
  brick surviving the per-engine split.
- **Baseline-relative comparison exists as a first-class operation**
  (`baseline-relative-report`, `diff-reports`), so gating can ask
  "worse than baseline?" rather than "clean?".
- **Sampling is pure and classification-agnostic.** The caller
  supplies the classification set; this component never learns the
  emitter's registry, and therefore never depends on the emitter.
- **Pairing is data.** Which judge witnesses which mutation is a
  loadable registry, not a code path (AR-PD-1).
- **Collisions are resolved by qualification, not by renaming the
  underlying function** — `report-valid?` / `finding-valid?` keep
  their namespaces' own names intact.

## 5. Non-goals

- **Runs no validation engine.** No HAPI, no NIST, no official FHIR
  validator. It defines what their answers look like.
- **Does not choose an engine, or know how many exist.** A consumer
  requires the engine it wants directly.
- **Owns no corpus.** It reads corpus metadata for sampling; it does
  not generate, intake or mutate.
- **Does not re-export the gate functions.** Deliberately, since
  ADR-0011 — the qualified relays are gone (ADR-0018).

## 6. Forbidden edges

Requires exactly `kernel` in `src`.

Must never require:

- **`judge-v2-hapi`, `judge-v2-nist`, `judge-fhir-official`** — the
  dependency runs the other way. All three engines require *this*
  brick for the verdict vocabulary; an edge back would be a cycle and
  would re-couple the vocabulary to the engines the split separated.
- **`sim-emit-hl7`** — named explicitly on the seam: the sampling
  surface exists in a caller-supplies-the-classification shape
  **specifically** so this component never depends on the emitter
  whose registry defines the strata.
- **`corpus`** — `corpus` requires this brick, not the reverse.
- **`bases/cli`**.

## UNCLEAR — the author's review queue

- **UNCLEAR-J1 — `judge` requires `kernel`, and `corpus` requires
  `judge`, but this interface exports no result-envelope vocabulary.**
  Every capability in this workspace is meant to answer in
  `:ok`/`:rejected`/`:error`, and a judge's verdict is the archetypal
  `:rejected` (the check ran and the answer is no). Whether a gate
  function returns a `kernel` result wrapping a `Report`, or a
  `Report` alone, is not answerable from this seam — the gate
  functions live on the three engine interfaces, and this brick
  publishes only the document. Worth stating once, wherever the
  answer is, because it is the first thing a consumer needs to know.
- **UNCLEAR-J2 — `sampling-header`'s name.** It is the only
  `sampling-*` var that is a function of one entry's *content*
  (`(sampling-header content)`), while its three siblings name
  strata, selection and rendering. Whether "header" means the ER7
  MSH header being read as the classifier, or a header row of the
  rendered strata table, is not stated on the seam.
