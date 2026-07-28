# Mining notes: upstream-limitations research → repo actions

Companion to the research report in `docs/research/` (SimHospital and
Synthea: limitations, practitioner experience; retrieved 2026-07-26).
This distills the report into specific repo actions, each tagged with
where it lands. The report is the evidence; cite it rather than
re-verifying its sources.

## A. Accept the correction (edit: docs/event-sourcing.md)

The report's central finding tempers our narrative and we adopt the
temper: event sourcing dissolves the *state-machinery* failure class —
now with more receipts than our docs cite (SimHospital's `PastVisits`
pop, pending-location shuffles, and the in-code admission that a first
pending encounter "will never be finished, since only the latest
Encounter is checked") — but the dominant Synthea complaints are
**clinical fidelity**, which storage architecture does not fix.
Action: add a scope paragraph to `event-sourcing.md`: architecture
carries reproducibility, correction/supersession, audit, and the
coherence laws (validation claims 3, 7); realism (claims 4–5) rests
on content provenance (Synthea modules), calibration, and capacity
modeling — different mechanisms, deliberately separate. Then cite the
report's §9 design target ("immutable replayable ledger with explicit
correction/supersession semantics; deterministic simulation time and
identifiers; separately materialized current-state views; composable
reactions across modules; empirically validated domain models") as
independent convergence with this repo's architecture + roadmap.

## B. Determinism-threat catalog (edit: event-sourcing.md or a test)

Synthea's reproducibility saga (issues #682/#1342, PRs #756/#1237)
shows a seed is necessary, not sufficient. Their failure modes, our
defenses, and the two gaps to close:

| Threat (theirs) | Our status |
|---|---|
| UUID/id generation divergence | Defended: deterministic patient-ids, control ids |
| Unordered-collection iteration | Mostly defended (sorted queue); **gap: add a guard test that emitters never iterate unsorted maps/sets when building segments** |
| Reference *date* vs full timestamp | Defended: relative seconds + explicit reference-date + fixed utc-offset, all in the manifest |
| Locale/OS differences | Partially defended (locale/tz recorded in manifest); revisit at CI time |
| Cross-format id divergence (CDA vs FHIR vs CSV) | **Gap: name "same event ids and patient ids across every emitter" as an explicit sub-law of emitter coherence, testable at M6** |

## C. IR transforms as the composition layer (edit: sim-theory.md note)

Synthea's composition pain — cross-cutting augmentation requires
editing every module (#780); modules surprising users via global
execution and hidden hard-coded Java lifecycle behavior (#941, #1126)
— is answered structurally by our pipeline: **IR→IR transforms between
CompileTrajectory and Execute are the cross-cutting composition
mechanism; InjectChurn is the first instance, not a special case.**
"Attach vitals to every emergency encounter" is a transform, written
once, touching no module. Action: one paragraph in `sim-theory.md`
naming the pattern. Corollary for M5 (roadmap note): **no hidden
modules** — lifecycle behavior (birth, death, aging) is explicit and
listable, never always-on and invisible.

## D. Overlapping encounters: evidence-backed requirement (roadmap)

The "never be finished" comment is upstream proof that
single-current-encounter assumptions break real workflows. The
encounters capture (visit ids, readmission, PV1-19) upgrades from
nice-to-have to documented-failure-avoidance, and must support
*multiple concurrent pending* encounters. Land the capture before or
with the milestone that introduces pending-admission steps.

## E. Demand-validated roadmap items (roadmap annotations)

Upstream users asked for, and did not get: IN1 (SimHospital #3 — our
M4), custom segments GT1/ZG1 (#21 — our site-profiles milestone), HL7
version switching (#17 — our MSH-12-as-config item, previously noted
in the design channel; add to the site-profiles or M3 scope), FHIR
output (#11 — our M6), US phone formatting (#21 — Persona/M4 detail).
Annotate each roadmap item with its upstream-demand citation — cheap
provenance that these milestones answer real requests.

## F. Positioning (README/positioning doc, when one exists)

1. **Capacity realism is a differentiator**: Synthea's own COVID
   paper concedes it "did not constrain care or supplies by
   capacity"; this simulator's boarding, exhaustion, and bed-ready
   coupling *emerge from* capacity. Say so.
2. **The gap is open**: the report's negative finding — no project
   positions itself as an event-sourced successor to either tool —
   framed honestly (an opportunity, not a converged market).
3. **The log player answers a documented need**: SyntheaWeb's stated
   "interpretability gap" (CLI + raw JSON as barriers) is the same
   need the player/bed-board serves.

## G. Smaller notes (file where indicated)

- Providers: Synthea's encounter `provider` column mapped to an
  *organization*, leaving encounters clinician-less (#547) — our
  provider/facility split already avoids this; one line in
  operational-models.md's providers section citing it.
- Warm-up/window: their export-window confusion (#1465, #1040 —
  content silently vanishing from windows) is the failure mode our
  mark-don't-trim choice prevents; one line where warm-up is
  documented.
- Medications (M5+): OHDSI found Synthea med data unreliable without
  the Medication Diversification Tool — when meds land, single-source
  distributions need diversification; roadmap note now so it isn't
  relearned.
- Installation friction is a first-class adoption risk for both
  upstreams (Bazel rot; JDK/Gradle "nightmare") — keep this repo's
  cold-start to one documented command, and treat SETUP.md (deferred
  trigger) as owed before any external user.
