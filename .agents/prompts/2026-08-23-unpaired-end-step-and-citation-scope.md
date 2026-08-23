# Archived prompt: unpaired-end-step-and-citation-scope (2026-08-23)

Session prompt -- two commits: the real defect (an unpaired
`:medication-end` compiled from a nil-resolved
`referenced_by_attribute`, tripping the lifecycle invariant on
clinic-decade seed 424242) and a separately-evidenced latent defect
(unscoped decide-time citation resolution).

## Context

Claude Code under R30 in `ehr-testing-tools`, on penny. HEAD at
handoff: `7a3ffd84` (session record addendum; tree clean; CI green at
tip; last tag `stable-20260821-patient-simulator-charter` @ `6ce2160c`,
no tag owed). Ceremony mode was **local commits only** -- no push, no
tag -- by the prompt's own close-out instruction.

The prompt's "Read first" item 1 names a prior session's report
"archived alongside this prompt". That report is **not present** in
this repository; the prompt's own Context section restates its trace,
and the session re-derived every link independently (see the session
record's deviation 1).

## The prompt, verbatim

# Session prompt -- unpaired end-step drop (real defect) + patient-scoped citation resolution (latent), two commits

## Context

Prior session (see its archived report alongside this prompt) reproduced the
consumer-reported failure at 7a3ffd84c3e75fbdba03b1177f4923a8af6d649d:
seed 424242 clinic-decade exits 2 violating
`:medication-end-references-existing-order-and-follows-it-in-time`, one violation,
patient PID-000089-c02fd3a8 at :t 5629740, 13.89s. Seed 5 exits 0 (zero
medication-ends generated). The violating event carries `:order-citation nil` and
`:order-event-id nil`.

Established mechanism (session-traced, evidence of record):
1. UTI module's `End UTI Tx` MedicationEnd uses `referenced_by_attribute: "UTI_Tx"`,
   written only by MedicationOrder states in submodule `uti/abx_tx.json`. The walk
   went telemed -> referral-to-ambulatory -> end without entering abx_tx.
2. gmf_interpreter.clj:1925 resolves the unwritten attribute to nil (deliberate,
   documented departure from upstream's fail-loud).
3. compile_trajectory.clj:271 -- `referenced-event` nil -> `cond->` skips
   `:order-citation` -> unpaired `:medication-end` step compiled. No drop rule covers
   a reference that never existed (existing rule covers antecedents dropped FROM the
   trajectory).
4. decide :medication-end -> `:order-event-id nil`; checker correctly fires (nil
   target, no citation to match pre-horizon facts).

Separately, a REAL BUT LATENT defect (read fact, both prior sessions): the
decide-time citation scans at engine.clj:849 (:medication-end) and :885
(:care-plan-end) have no patient filter; citations `{:module :state}` are not
patient-qualified; both reproduction runs contained 3-4 cross-patient citation
collisions. Zero cross-patient resolutions occurred in either run -- this defect did
NOT cause the reported failure and its fix is justified by direct engine-level
assertion only.

Error ledger entries to record in the ADRs (channel-owned, restated):
- Original landing: unearned specificity -- assumed a MedicationEnd's referenced
  order always fired on the same walk.
- Design channel, this arc: unearned specificity -- attributed the observed failure
  to the unscoped scan from mechanism plausibility without tracing the violating
  event.

## Author rulings (verbatim)

R1: (3) -- both defects, separate commits with separate ADRs, real defect first.
R2: (a) -- compile-time drop: an end-step whose referenced_by_attribute resolved to
no referent (and therefore carries no citation) is dropped in compile_trajectory,
extending the existing "no orphaned reference" principle to "no reference ever
existed." Raw trajectory keeps the event. Interpreter nil-resolution unchanged.
Vendored modules untouched.
R3: (a) -- :medication-end plus :care-plan-end as declared twins; PROBE whether
:condition-end shares the identical nil-referent shape and report; extend to it only
if the probe confirms.
R4: Q1(a)/Q2(a) stand for commit 2 -- same-patient predicate in both decide-time
scans -- justified by the engine-level direct assertion, explicitly NOT by the
424242 reproduction. The ADR must say so.
R5: where the world-of/admit/fold-events scaffold cannot produce :registered, assert
the single invariant function directly per engine_test.clj:1135's own convention;
disclose in the session record.

## Read first

1. Prior session's archived report (this arc) -- the trace is the evidence of record
2. components/patient-simulator/src/ehrt/patient_simulator/compile_trajectory.clj --
   `medication-end->step` (271), `referenced-event`, the history-phase/AR-2 drop
   rules and their docstrings, `care-plan-end` twin
3. components/patient-simulator/src/ehrt/patient_simulator/gmf_interpreter.clj --
   attribute resolution (~1925), the documented nil-resolution departure (~1677),
   and :condition-end's reference shape (R3 probe)
4. components/sim-engine/src/ehrt/sim_engine/engine.clj -- decide :medication-end
   (849), decide :care-plan-end (885), evolve pair
5. components/sim-check/src/ehrt/sim_check/check.clj:478-508
6. engine_test.clj:1135 (R5 convention); existing compile_trajectory test namespaces

## Steps

0. Environment: penny, WSL, JDK 21. Verify HEAD is 7a3ffd84... or a descendant whose
   intervening commits touch none of the four namespaces above; else ESCALATE.
   Baseline `make test` green (expect ~27min on this machine); if red, ESCALATE.

1. Re-confirm the failure is live at HEAD: seed 424242 run exits 2 with exactly the
   one known violation; seed 5 exits 0. If either differs from the prior session's
   record: FINDING -- stop and report.

2. R3 probe: does :condition-end resolve a referenced_by_attribute the same way,
   such that a never-written attribute compiles an unpaired end-step? Read the
   actual mechanism; record the answer with file:line in the session record. Extend
   the commit-1 fix to it ONLY if confirmed identical.

--- Commit 1: real defect ---

3. RED: minimized compile_trajectory-level failing tests first:
   a. A trajectory whose :medication-end has nil references and no matching order
      anywhere (not pre-horizon, not dropped -- never existed) -> assert the compiled
      steps contain NO :medication-end, and the checker invariant is clean over the
      resulting log (R5 convention if scaffold-limited).
   b. Same for :care-plan-end. c. :condition-end only if step 2 confirmed.
   d. Guard test: the DESIGNED straddle (order pre-horizon -> fact promotion, end in
      horizon WITH citation) still compiles the end-step -- the drop must key on
      absent referent/citation, never on pre-horizon straddling.
   Watch a-c fail on current code; d must pass before AND after.

4. GREEN: in compile_trajectory, drop an end-step whose referent resolution yielded
   nothing and which therefore carries no citation. Follow the existing drop-rule
   idiom (docstring citing the principle's extension; counter/bookkeeping only if an
   existing sibling mechanism does the same -- mirror, don't invent). No interpreter
   change, no module change, no checker change.

5. Scenario gate: seed 424242 run now exits 0; seed 5 still exits 0. Add the 424242
   clinic-decade run as a gated scenario test per the repo's slow/demo conventions
   (13.89s, inside the 60s gate); seed 5 as control if the convention supports it
   cheaply, else record it in the ADR only.

6. Oracle sweep for commit 1: regenerate `make docsgen` artifacts
   (demos/traces/emit-state/ground-truth.edn, demos/traces/order-result/
   ground-truth.edn, components/sim-engine/resources/sim-engine/event-examples.edn)
   and diff. Disclose each changed path in ADR-0163. If changes extend beyond
   removed unpaired end-steps and knock-on indices: ESCALATE.

7. ADR-0163: defect, trace summary (cite prior session's report), rulings R2/R3
   verbatim, probe result, fix, regression shape, oracle sweep, BOTH error-ledger
   entries. Commit 1 (fix + tests + regenerated artifacts co-landed):
   `fix(patient-simulator): drop end-steps whose referenced order never fired --
   nil-resolved referenced_by_attribute compiled unpaired :medication-end, tripping
   the lifecycle invariant; clinic-decade seed-424242 cleared (ADR-0163)`

--- Commit 2: latent defect ---

8. RED: engine-level failing tests -- two patients, shared {:module :state} citation,
   B's order interleaved between A's order and A's end; assert A's end's
   :order-event-id indexes A's OWN order (direct assertion, R5 as needed). Twin test
   for :care-plan-end/:start-event-id. Watch both fail.

9. GREEN: add the same-patient participant condition to both decide-time scan
   predicates. No other logic; no RNG changes. Full `make test` green.

10. Oracle sweep for commit 2: re-run the docsgen diff. Expected: no changes (traces
    are small; prior sweep found 3 :order-event-id occurrences each -- verify none
    involved cross-patient resolution). Disclose either way in ADR-0164.

11. ADR-0164: latent defect, read-fact citations, collision counts from step 1's
    runs, EXPLICIT statement that this fix is justified by direct assertion and not
    by the 424242 reproduction (R4), rulings verbatim. Commit 2:
    `fix(sim-engine): patient-scope citation resolution in decide :medication-end
    and :care-plan-end -- latent cross-patient {:module :state} collision, direct
    engine-level assertion; not the seed-424242 cause (ADR-0164)`

12. Final `make test` green over the completed tree. Session record: both runs'
    numbers, probe result, R5 disclosures, corrected make-test wall-clock (26m39s
    baseline; update state.md's stale ~14.5min figure). Update roadmap/state.md per
    arc close. Self-archive this prompt. Local commits only; no push, no tag.

## Fences

- check.clj untouched in both commits. Citation shape untouched. Vendored modules
  byte-identical (ADR-0071). Interpreter nil-resolution behavior untouched. No seed
  special-casing, no invariant weakening, no history rewrites.
- The drop rule must not fire on the designed straddle (step 3d is the guard).
- One sanctioned improvement per commit beyond the rulings: none sanctioned.
- STOP-AND-REPORT binds where two readings are both defensible; mechanical
  conflicts fix-forward with disclosure. The step-1 re-confirmation and step-2
  probe are findings gates: reality disagreeing with the prior session's record is
  a FINDING, not an escalation of a sound check.
