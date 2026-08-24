# Archived prompt: generator-side-coverage-and-care-plan-end-invariant (2026-08-23)

Archived verbatim, as delivered to the session.

---

# Session prompt — generator-side event-type coverage gate + care-plan-end referential invariant, two commits

## Context

Arc follow-on to ADR-0163/0164 (tip 68af03b at drafting time). The seed-424242
defect was invisible to the suite because the invariant catalog's trigger
population was empty: exactly one population-scale self-check gate existed
pre-arc (seed 202, 100 patients, ed-tuesday), clinic-decade was live-probed once
and never gated, and empirically the gated runs produce zero-to-one
:medication-end events. No meter measures which event types the gated runs
actually exercise. ADR-0160's coverage gate is judge-side only.

This session lands the generator-side analogue: a per-push gate asserting the
gated scenario runs collectively produce every event type the vendored closures
can emit, plus (ride-along, Q1) the missing :care-plan-end referential
invariant, whose absence let seed 5 carry unpaired care-plan ends silently
until ADR-0163's session found them by hand.

Born-red warning (review-4 watch-list W-1, ADR-0160's own lesson): the coverage
gate must run to green in-session at the per-push tier. No gate lands that has
never executed green.

## Author rulings (verbatim)

Coverage instrument: (a) — event-type production coverage gate over the gated
runs; explicit waivers otherwise.
P1: (a) — emittable set derived as a declared state-type→event-type table,
co-landed with a test asserting it matches the interpreter's actual dispatch.
P2: (a) — shared once-fixture generating each gated corpus once, all gates
reading it. THE one sanctioned improvement this session; existing gate
assertions move verbatim.
P3: (a) — a type resisting a bounded seed-hunt is waived with a named queue
row; gate lands green.
P4: (a) — Q1's care-plan-end invariant rides along as commit 2.

## Read first

1. notes/adr/0163-unpaired-end-step-drop.md and 0164 — this arc's substrate
2. components/sim/test/ehrt/sim/run_test.clj — the three gated runs
   (seed-202, seed-424242, seed-5) whose assertions move verbatim onto the
   fixture
3. components/patient-simulator/src/ehrt/patient_simulator/gmf_interpreter.clj
   — state-type dispatch (the P1 table's ground truth)
4. components/sim-check/src/ehrt/sim_check/check.clj —
   medication-end-references-existing-order-and-follows-it-in-time (478) and
   pre-horizon-medication-order-citations-by-patient (~460): commit 2's mirror
   pair
5. compile_trajectory.clj — the designed care-plan straddle (the guard tests
   from ADR-0163 name it) — commit 2's pre-horizon branch must probe, not
   assume, how care-plan pre-horizon facts are cited
6. notes/adr/0160-* — W-1, so the gate's landing shape avoids it

## Steps

0. Environment: penny, WSL, JDK 21. HEAD is 68af03b or a descendant not
   touching the namespaces above; else ESCALATE. Baseline `make test` green
   (expect ~28–31min; Q2's slowdown is open — record wall-clock).

--- Commit 1: coverage gate ---

1. P1 table: derive state-type→event-type from the interpreter's dispatch by
   reading it. Land as data (edn or def) plus a test that walks the dispatch
   and fails on divergence. INVARIANT: the test must fail if a new state type
   is added to the interpreter without a table row — demonstrate by temporary
   mutation, then revert.

2. P2 fixture: one shared once-fixture generating the three gated corpora;
   move the three existing gates' assertions onto it VERBATIM (diff must show
   relocation, not rewrite). Suite-time delta recorded.

3. Coverage probe (measurement of record): union of event types across the
   three corpora vs the P1 table's emittable set for each run's closure.
   Record the full types×runs matrix in the session record. Expect holes —
   plausibly including :medication-end itself, since ADR-0163 now drops
   seed-424242's only one.

4. RED: the coverage gate, asserting every emittable type produced at least
   once across gated corpora, waivers as named data rows. Watch it fail
   listing the missing types.

5. Coverage hunt, bounded: for each missing type, seek a small deterministic
   run (scenario, seed, minimal patients) producing it PAIRED and
   self-check-clean; add to the fixture + gated set. Budget: if a type
   resists ~30min of hunting, waive it (P3a) with a queue row naming the
   type. No waived-to-green shortcuts for types a found seed covers.

6. GREEN: gate passes at the per-push tier with the final coverage matrix +
   waivers. Oracle sweep: `make docsgen` diff (expect none; disclose).
   ADR-0165: instrument, P1 table rationale, matrix as landed, waivers,
   fixture, suite-time delta. Commit 1:
   `test(sim): generator-side event-type coverage gate -- gated runs must
   collectively produce every emittable event type; shared corpus fixture;
   coverage matrix and waivers as landed (ADR-0165)`

--- Commit 2: care-plan-end invariant ---

7. Probe: how do care-plan pre-horizon facts land on :registered (mirror of
   medication fact promotion)? Read the mechanism, record file:line. The
   invariant's pre-horizon escape must match what the compiler actually
   promotes, not what the medication twin suggests.

8. RED: (a) scripted-log unit tests mirroring the medication invariant's
   shape — wrong-patient target, missing target, time-order, pre-horizon
   escape (R5's direct-assertion convention where the scaffold lacks
   :registered); (b) if step 3's matrix shows a gated corpus with care-plan
   events, assert the invariant clean over it. Watch (a) fail against a
   deliberately-corrupted scripted log BEFORE the invariant exists only in
   the trivial sense — the real red is: write the invariant, verify it
   REJECTS the corrupted log and ACCEPTS the sound one.

9. GREEN: `care-plan-end-references-existing-start-and-follows-it-in-time`
   added to check.clj and the catalog, docstring citing ADR-0163's finding
   (seed 5's silent unpaired ends) as its origin. DECLARED ORACLE CHANGE:
   full suite rerun re-judges every gated corpus under the widened catalog.
   Any gated run newly failing is a FINDING — stop and report with the
   violating events; do not adjust the invariant or the corpus.

10. ADR-0166: invariant, probe result, oracle-change declaration and its
    outcome, Q1 ruling. Commit 2:
    `feat(sim-check): care-plan-end referential invariant -- mirror of the
    medication-end pair, pre-horizon escape per the compiler's actual fact
    promotion; declared oracle widening, all gated corpora re-judged clean
    (ADR-0166)`

11. Final `make test` green over the completed tree. Session record: matrix,
    hunt log, waivers, wall-clocks (Q2 evidence), deviations. Roadmap: close
    this row; add waiver rows if any. Self-archive prompt. Local commits
    only; no push, no tag.

## Fences

- check.clj changes ONLY in commit 2 and ONLY the new invariant + catalog
  entry. Existing invariants untouched. Vendored modules byte-identical.
- Existing gate assertions relocate verbatim (P2); any assertion change is
  ESCALATE.
- One sanctioned improvement total: the P2 fixture. Nothing else improves in
  passing.
- Waivers are data rows with queue rows, never silent.
- STOP-AND-REPORT where two readings are defensible; mechanical conflicts
  fix-forward with disclosure. Step 9's newly-failing-corpus case is a
  FINDING, full stop.
