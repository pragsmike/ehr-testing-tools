# Traffic model: event families, classification, and rulings

Doctrine for what ehrt generates versus renders as hospital traffic
scales. Anchor: ADR-0168. Companion plan:
`.agents/plans/2026-08-24-traffic-scale-program.md`. Rulings cited as
R-mix-N are in `.agents/rulings.md`.

## The classification principle

If downstream invariants or later messages' content must respect it, it
is **skeleton** — ground truth, generated, judged by the invariant
catalog. If it is derivable restatement of skeleton state, it is
**emission** — rendered downstream, unjudged, volume-tunable by config.
Mix ratios (how much chatter per skeleton delta) are therefore emission
config and reshuffle nothing.

## Families

### Skeleton (state-bearing)

- **Demographic/identity timeline** — the A08/A31/A28/A29 family:
  residence moves, insurance/carrier changes, name/DOB corrections,
  staged-registration fills (quick-reg completed later), link/unlink
  (A24/A37), account/visit moves (A44/A45). Requires demographics to
  become time-varying state (today: Persona sampled once, static — see
  components/sim-model persona). Source process: the person-simulator
  (ADR-0168 §4). Invariant shapes: a fill follows its quick-reg; a
  correction references what it corrects; a payer change references its
  employment change — the referential family check.clj now implements
  twice (medication ADR-0163/0166 lineage).
- **Identity churn already landed** — the six churn step types
  (:cancel-admit :cancel-transfer :cancel-discharge :transfer-in-error
  :bed-swap :merge), ehrt.sim-engine.churn: the ADT-movement-error
  family, pathway-IR insertion, clinical steps untouched.
- **Unidentified-arrival flow** (R-mix-4): placeholder registration
  (alias, no coverage) → later identification forking fill-in-place
  (A08/A31 burst) or merge-with-existing-MRN; the second branch composes
  with :merge into the post-merge-shadow defect surface (below).
- **Result corrections and order cancels** — ORU final→corrected;
  ORC cancel/discontinue/hold mid-flight. Content-bearing.
- **Pending ADT states** — A15/A16 pending transfer/discharge and their
  cancels A25/A26: interact with bed state.
- **Scheduling** (R-mix-5, ruled state) — appointment new/reschedule/
  cancel/no-show as skeleton events; arrivals split scheduled-correlated
  vs walk-in. Invariants: a cancel references its appointment; a
  scheduled encounter follows its appointment; a no-show has no
  encounter.
- **Bed-status/housekeeping cycle** (R-mix-6, ruled state) —
  vacated→dirty→cleaning→ready, assignment gated on ready; world-level
  state interacting with boarding and the corpus-player bed-board sink.
- **Patient-class changes** — A06/A07 observation↔inpatient.
- **Person/population events** — birth (pregnancy→delivery, R-mix-2),
  death, household formation/moves; mother-baby link at delivery; the
  newborn's first encounter is the birth.

### Emission (derivable restatement)

- **Order/result status ladders** — received/in-progress/preliminary
  interpolated deterministically between skeleton order and result.
- **Charges (DFT P03)** — one-to-many off clinical events plus per-diem
  room-charge clock; the dominant volume multiplier in real engines.
- **Routine A08/A31 re-statements** — no-delta re-sends; chatter volume
  on top of the skeleton timeline's content.
- **Account lifecycle (BAR), coverage-verification echoes.**
- **Fan-out/routing** (R-mix-7) — subscriber table duplicating each
  message per interface with distinct addressing; converges with the
  corpus-player MLLP/bed-board slices.
- **Duplicates/resends and delivery disorder** — with the ADR-0110
  latency machinery, emission-side.

## Named defect surfaces the mix exists to enable

- **Post-merge shadow**: late messages under the pre-merge MRN after an
  A40 — requires merge + demographic timeline + emission lag; the
  highest-value single injectable class for MPI-consumer testing.
- **Staged-registration races**: content arriving against a not-yet-
  filled registration.
- **Correction races**: downstream acting on final before corrected.
- **Charge reversals** mismatched to their clinical retraction.

## What this document is not

Not a plan (see the program plan for arcs, sequencing, estimates), not a
schema (event shapes land with their arcs), not exhaustive (families the
program declines — queries/QRY, master-file MFN, device data — are out of
scope until a consumer names them; add here with a ruling when one does).
