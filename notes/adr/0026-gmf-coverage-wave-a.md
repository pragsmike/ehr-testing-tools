<!-- Attic file: notes/adr/0026-gmf-coverage-wave-a.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0026 — GMF coverage Wave A: condition vocabulary v1→v1.1, `sore_throat.json` vendored

**Status:** Accepted (author-ruled 2026-08-02, AR-1..AR-6 of the Wave A
session prompt; session executed same day).

### Context

The GMF coverage-expansion arc (`.agents/plans/2026-08-02-gmf-coverage-
plan.md`, Step 0 of this session) is the payoff milestone sim split S2
unblocked (ADR-0025) — Wave A is its first slice: extend
`ehrt.sim-trajectory.gmf-interpreter`'s condition vocabulary
(interpreter-only, no pathway-IR or sim-model changes) and vendor
`sore_throat.json`, the module survey's one state-type-clean candidate
blocked solely by a condition-vocabulary gap
(`components/sim-trajectory/docs/gmf-interpreter.md`'s own M7 appendix).
Determinism guard for a session that legitimately grows interpreter
behavior: no byte-identity oracle applies, so the regression oracle was
the existing vendored modules — fixed-seed walks of `sinusitis.json` and
`appendicitis.json` (10 seeds × 2 sexes each, hashed trajectory +
attributes) proven identical before and after every commit.

### Decision

**Five condition types join v1** (`ehrt.sim-trajectory.gmf-interpreter/
evaluate-condition`, `ehrt.sim-trajectory.gmf/condition-type->keyword`),
each ruled in because Step 1's own characterization confirmed its data
source already exists — AR-2's own membership bar — with no new
accumulator or IR home: `:or`/`:at-least` (boolean-disjunction and N-of-M
compound wrappers, the same recursive shape `:and` already establishes —
grounded against Synthea's own `Logic.java` `Or`/`AtLeast` classes at
`docs/gmf-interpreter.md`'s pinned commit); `:date` (a calendar-year
comparison against the interpreter's own virtual clock, `ctx`'s `:t`,
already threaded since M5a); `:observation`-as-a-condition-type (a log
query over already-emitted `:observation` trajectory events by concept,
the same shape `:active-condition`/`:active-medication` already
establish); and `:symptom`-as-a-condition-type. `Active Allergy`, AR-2's
fifth named candidate, needed NO new work — a real finding, not a
no-op: it already joined v1 at M5b (ADR-0013-era), and
`sore_throat.json`'s own `Active Allergy` checks use the identical
RxNorm-7984-Penicillin-V shape `sinusitis.json`'s already does.

**`:symptom`-as-a-condition is an emergent finding, not one of AR-2's
five NAMED candidates** — surfaced by Step 1's own characterization:
`sore_throat.json`'s ONLY real use of `:at-least` (`Determine_if_
Bacterial`, a modified-Centor-criteria gate) wraps `Symptom`/
`Observation`/`Age` sub-conditions exclusively, so `:at-least` has no
real branch coverage without it. Its own data source — the already-
accumulating `:attributes` map, written by the already-ratified,
already-built `Symptom` STATE (M5a, this document's own §1 flag,
disambiguated by a dated note added there this session) — clears AR-2's
same membership bar the five named candidates were judged against, so
it was built rather than dropped; recorded here as the finding it is,
not silently folded in.

**`Vital Sign`/`Active CarePlan` stay OUT, AR-2's own pre-ruling
confirmed, not merely assumed:** neither condition type appears
anywhere in `sore_throat.json` (grepped directly against the fetched
module, Step 1) — the pre-ruling never had to be tested against real
content this session, but nothing this session found contradicts it
either. Both still need a new accumulator/IR home before they could
join v1 (Wave D's own scope, the wave plan).

**`sore_throat.json` vendored** (`resources/modules/sore_throat.json`,
NOTICE entry, third vendored module after `sinusitis`/`appendicitis`) —
state-type clean (44/44 v1 types) since the M5-prep survey, blocked at
every prior survey pass by exactly the condition-vocabulary gap this
session closed. Test coverage (`vendored_sore_throat_test.clj`, written
test-first, RED for "resource not found" before vendoring) includes
AR-5's own obligation: real branch coverage through `Determine_if_
Bacterial`'s `At Least` compound, a well-mixed seed search finding real
vendored walks that cross BOTH its thresholds (`>=5` → `Prescribe_
Antibiotics`, a real downstream `MedicationOrder` event; `>=3`-but-not-
`>=5` → `Throat_Culture` only, a real `Procedure` event with no
antibiotic ordered).

**`stroke.json`'s own `Date` gap is resolved at the interpreter level
but the module stays deferred (AR-6).** Its `Death` state-type gap is a
load-bearing, semantically-real state — unlike `Device`/`DeviceEnd`
(M5b), never safely reducible to a consumed-internally pass-through —
so the loader's all-or-nothing gate still rejects it; wiring `Death` to
the existing `:expired`/post-mortem machinery is Wave C's own scope, not
reopened here. Recorded in `docs/gmf-interpreter.md`'s own survey row so
a future session doesn't re-litigate why `stroke.json` isn't vendored
despite its `Date` gap being gone.

**Verification baselines, every commit:** `poly check` clean; `poly
test :all skip:integration` 0 failures/0 errors at every checkpoint;
the fixed-seed regression oracle (above) byte-identical across all six
interpreter/vendoring commits, confirmed one final time at session
close. Commits, in order: `0b2c1b2` (wave plan + roadmap), `9176250`
(`:symptom`), `f99e87a` (`:at-least`+`:or`, one disclosed combined
commit), `5e3e72c` (`:date`), `6a35492` (`:observation`), `6a3e11b`
(condition vocabulary v1.1 docs), `a2cf68d` (`sore_throat.json`
vendored).

### Fence

This ADR covers Wave A only. Waves B (`CallSubmodule`), C (`Death`), and
D (state types needing IR + emitter homes) are not started — see
`.agents/plans/2026-08-02-gmf-coverage-plan.md` for their own scope and
trigger conditions, and `.agents/plans/roadmap.md`'s own Deferred rows.
No pathway-IR, `sim-model` schema, or engine change landed this session
— Wave A was interpreter-only, exactly as scoped.

### Deviation record

One disclosed deviation from Step 2's own "one condition type per
commit" instruction: `:at-least` and `:or` landed in a single commit
(`f99e87a`) — both are trivial `GroupedCondition` mirrors of the
already-built `:and`, share the identical loader change (broadening
recursive `:conditions` normalization), and splitting them would have
been an artificial two-way cut through one small diff; named in that
commit's own message, not silently folded in. The `:symptom`-condition
emergent finding (above) is a second, larger deviation from the
session prompt's own literal AR-2 candidate list — not treated as an
escalation-and-stop case, since Step 1's own membership test (data
source already exists) already authorized it and Step 2's own
instruction named `:symptom` at its list's end (in the STATE sense,
AR-3) without excluding a condition-side reading; recorded here as the
finding it is, per Step 1's own "if characterization shows a gap the
survey missed, record it" instruction — this is that record.

**[A] ratified 2026-08-02 (design channel), retro-ratification rider
(Wave B D8, ADR-0027): the `:symptom`-as-condition inclusion is
confirmed within AR-2's admission criterion.** Fix-forward note, no
body rewrite — the deviation record above stands as written; this
rider only settles, after the fact and by direct author ruling, that
the deviation it already disclosed was the right call, not merely an
authorized-but-unreviewed one.

---

