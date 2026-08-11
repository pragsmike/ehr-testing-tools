# 2026-08-11 — ed-tuesday: the scripted ED scenario (ADR-0104)

## Scope

Execution session for option A of the author's own 2026-08-10
"C-with-A-first" ruling: a new sibling scenario,
`demos/scenarios/ed-tuesday/`, contrasting busy-tuesday's own
population-scale, sparse, ambulatory-heavy mix with a day-scale
scripted single ED shift — real, weighted admissions/transfers/
discharges driving visible inpatient census and real churn on a
`--board`. Option B (vendoring upstream's injuries family) is not
touched — it stays a separate future batch under the standing
vendoring ceremony, anchored in the roadmap. Two commits: the scenario
itself (`51f0e68` — `config.edn`, `README.md`, two cross-reference
lines), and this close-phase record/prompt-archive commit. This is
ADR-0104.

## Verification highlights (Step 2)

Read `ehrt.sim.run/incompatible-assignments` and its two helper
predicates end to end, plus `ehrt.sim-engine.engine/assign-pathway`/
`assign-module` and `run`'s own docstring, plus
`ehrt.sim-trajectory.gmf/ModuleAssignment`, BEFORE writing any config.
Finding: `assign-pathway` and `assign-module` draw independently per
ordinal; a second weighted `:module-assignment` pool covering the same
ordinals as an ED-weighted `:pathways` pool carries a genuine
RNG-coincident collision risk the static check cannot see (it only
reports CERTAIN conflicts, and a mixed pathway pool is never certain
either way). `ModuleAssignment`'s own pool-member schema has no "no
module" option, so a non-empty module pool always resolves every
ordinal it covers to some real module. Outcome A: a sanctioned,
provably conflict-free shape exists — disjoint explicit-ordinal
cohorts (both an explicit empty-pathway `:pathways` override AND an
explicit `:module-assignment` entry, never a `{:weight}` pool member,
for the module-tail ordinals) — not merely one the static check
happens not to flag, but one where the RNG genuinely never gets a
chance to collide. Full citations and the finding's own prose live in
`notes/adr/0104-ed-tuesday-scenario.md`.

## Authoring and live-probe highlights

A first-draft config (default-adjacent facility, `:arrival-gap 10`,
heavier admit weights, 240 patients) exhausted ED capacity at patient
23 even after doubling Emergency's surge slots. Re-tuned by a
Little's-law estimate (concurrent occupancy ≈ arrival-rate × dwell)
and re-probed clean: `:arrival-gap 30`, admit weights rebalanced to a
~15% admission fraction, inpatient dwell shortened, Emergency's surge
bumped to 16, 100 patients. The shipped run (seed 20260811) produced
no `:capacity-exhausted` and no `:self-check-failed` — confirmed live,
not estimated.

The ambulatory module tail (8 explicit ordinals,
sore_throat/sinusitis/bronchitis/ear_infections) produced ZERO live
`:outpatient-visit` events at the shipped 90-day horizon. Re-probed at
14 and 3650 days (busy-tuesday's own horizon) to separate a tuning
mistake from a structural property: only the 3650-day run produced any
live content, exactly 1 of 8. The mechanism (`sore_throat.json`'s own
monthly-`Delay`-gated ~0.5-1% onset probability) is read from the
module's own source and disclosed in both `config.edn`'s header and
the scenario's own README — not silently retuned toward a horizon that
would defeat the day-scale contrast this scenario exists to draw.

**Full gate:** `clojure -M:poly check` OK; full local suite (`clojure
-M:poly test :all skip:integration`, unredirected capture) 596
occurrences of "0 failures, 0 errors" across the entire log, 0
FAIL/ERROR anywhere, exit 0, 3 minutes 54 seconds — the same figure
ADR-0103 reported, consistent with zero test/src namespace changes;
`ehrt.docs-tooling.invocation-lint-test` green (4 tests, 197
assertions — this scenario's own README commands resolve under the
fence-path machinery); `ehrt.cli.cli-parse-guard-lint-test` green (4
tests, 22 assertions); `bin/verify-nist-lock` OK, 6/6. Oracle bracket
(`741b2f6` → `51f0e68`) all 34 roots IDENTICAL, soundness check clean
— matched the pure-identity prediction exactly.

## Judgment calls and their ratification status

- **Emergency ward's surge slots bumped 6 → 16 in the scenario's own
  `:facility`, versus `sim-model/default-facility`.** Directly
  licensed by the driving prompt's own instruction ("exact
  trajectories, weights, delays are yours to author against the
  schema, tuned so a board... shows inpatients rising and falling");
  the default's own 6 surge slots is a "small on purpose" testing
  default (`sim-model/config.clj`'s own docstring), not a ruled
  ceiling for scenario authors — disclosed, not a deviation.
- **The module tail's own module assignment is a hand-authored
  round-robin, not RNG-weighted.** A direct, disclosed consequence of
  Step 2's own finding (the conflict-free shape requires explicit
  per-ordinal entries, which have no pool for the RNG to draw from) —
  named explicitly in `notes/adr/0104-ed-tuesday-scenario.md`'s own
  Deviations section rather than left implicit.
- **`:module-horizon-days` stayed at 90 despite producing zero live
  tail content**, rather than raised toward a horizon known (from the
  3650-day diagnostic run) to occasionally produce one. Directly
  licensed by the driving prompt's own named contingency ("if tuning
  can't produce it, that is a finding, not a silent retune loop") and
  its own "day-scale... module horizon short" instruction.

## Findings and HEAD landed

**No engine or interpreter defect found or touched.** Step 2's own
finding is a genuine config-authoring hazard (a second weighted module
pool colliding with a weighted pathway pool), not a bug — the static
`incompatible-assignment` check behaves exactly as its own docstring
describes; this session's own contribution is naming the sanctioned
workaround shape and recording it for the next scenario author.

**One disclosed, live-probed empirical finding:** the four
already-vendored "everyday-ambulatory" quick modules are genuinely
low-incidence per patient at any short (day/week/month-scale) horizon
— consistent with, and more extreme than, busy-tuesday's own
already-disclosed sparse-traffic finding at population scale.

**Tag paid forward:** `stable-20260810-board-boundary-catchup` tagged
at `741b2f6` (Step 1, this session — the design channel's own verified
ADR-0103 landing, tag law case (i)), peeled ref verified exact match,
remote unmoved.

**HEAD landed:** the close-phase commit (this record's own commit),
pushed.
