# Session record -- arc 3a, part 2: the fold's refactors, output-identical

**Date:** 2026-08-26
**Prompt:** [`.agents/prompts/2026-08-26-arc-3a-fold-refactors.md`](../prompts/2026-08-26-arc-3a-fold-refactors.md)
**Base:** `c45ddb9` -- **Tip:** ``dd4f9f7` (code) / this record (docs)`
**Mode:** payload session under the de-scaffold moratorium. R30 ceremony,
no tag, CI green at the tip as the close marker.

## What this session is

ADR-0173 section 2's refactor half, ruled by the author into a session
of its own: no `:persons` key, no person event reaching the engine, no
new event kind, no contract bump. Every commit owes the ADR-0169 proof
instead of red-before-green -- `bin/regression-oracle c45ddb9 HEAD`
IDENTICAL with no declaration -- and a moved byte is a STOP.

Six commits, all five prompt steps landed.

| commit | what |
|---|---|
| `c1902b6` | C1 -- the compile moves from arrival to run start |
| `2cc2a01` | 2(a) -- `:person-index`, carried and empty |
| `d711132` | 2(b) -- `PatientState` gains `:demographics` |
| `11d2d50` | the row-10 red, fixed and disclosed |
| `2393b48` | 2(b) -- the emitter re-key, output-identical |
| `dd4f9f7` | one assertion per gated run, not one per patient |

## Step 1 -- the t-independence table, re-derived from the tree

The prompt's condition for moving the walk at all: every input to the
module walk and to `compile-trajectory` must be arrival-time-independent,
or STOP. All eight are, and this is the table, each line probed against
the live tree rather than inherited from the ADR.

| input | why it cannot differ between run start and arrival |
|---|---|
| `rng` | ONE stream per patient (`run`'s `patient-rngs`), and **exactly three** `decide` methods draw from the `:patient` family -- `:registered` (`:484`), `:delay` (`:573`), `:order` (`:866`); every other method takes `_streams`. All three read the ACTING patient's own stream (`(assoc base-streams :patient (get streams-by-pid patient-id))`), so patient i's stream is read by nothing but patient i's own decides -- including the decides that emit events for OTHER patients (a bed-ready `:transfer` off a discharge draws from the discharging patient's stream). `:delay`/`:order` are steps that FOLLOW `:registered` in that patient's own queue, so at arrival the stream stands exactly where the pre-loop draws left it: the position it stands at when `run` now compiles. |
| `(:persona-config world)` | set once in `init-world`; the run loop only ever `assoc`s `:ground-truth`/`:reinstate-index`/`:citation-index` on `world` and `update-in`s `[:patients pid]`. Read directly off the loop body, not assumed. |
| `closure` | resolved pre-loop by `module-for` and carried on the `:registered` STEP -- immutable queue data |
| `(:modules closure)` / `(:root closure)` / `(:initial-attributes closure)` / `(:tables closure)` | pure data inside that closure |
| `reg-t` | `sim-model/reference-today-epoch-day` is `(.toEpochDay (LocalDate/of (inc reference-birth-year) 1 1))` -- a fixed calendar anchor computed from a CONSTANT. Not a clock: a run straddling midnight could not move it, which is the failure mode a `LocalDate/now` would have had. |
| `horizon-end-t` | `reg-t` + `(:module-horizon-days world)`, run config |
| `(:facility world)` | run config, never re-`assoc`ed -- bed occupancy lives in `[:patients pid :location]`, not here |
| `history?` | `(:history world)`, run config |

The move is therefore in TIME and not in any stream, and the pinned
pre-loop order is now stated in `run`'s own docstring as ADR-0173
requires: **`module-for`, `pathway-for`, `churn/inject`, then
`compile-patient`.** Note the order: the ADR's parenthetical names it
"assign-pathway, assign-module, churn/inject", and the TREE evaluates
`module-for` first -- `(into [{:type :registered :closure (module-for i)}]
(steps-for i))` evaluates its arguments left to right. The docstring
pins what the tree does.

One consequence, named rather than discovered: a patient whose
`:registered` is never decided (an `:exhausted` run ends the loop early)
is compiled anyway. That costs a walk and moves no byte -- the draws
land on that patient's own stream, which nothing else ever reads.

### The gates

* `engine-test/the-registered-compile-is-arrival-time-independent` --
  `decide :registered` for the same patient off two identically-seeded
  FRESH streams at t=0, t=86400 and t=400 days, twelve seeds, with and
  without a real sinusitis closure. Everything but `:t` must be `=` and
  byte-equal. **Witnessed green against the ARRIVAL-time compile before
  the move** (src stashed back to `c45ddb9`: 95 tests / 590 assertions,
  0 failures), which is ADR-0169's own operational clause -- a gate
  witnessed passing before it has anything to catch.
* `run-test/every-gated-run-compiles-the-same-persona-at-any-arrival-time`
  -- all four gated runs re-run at `:arrival-gap` 97, which moves every
  arrival instant through the WORLD family and touches no `:patient`
  draw at all. Every patient's `:registered` event equal but for `:t` and
  the `:warm-up` derived from it.
* `engine-test/compile-patient-is-what-registered-attaches` -- the
  export and the decide agree, off the same stream position.

**One assertion went red while being written, disclosed rather than
filtered.** The non-vacuity check first read `every?` on
`:prepend-steps`; a sinusitis walk falling entirely in the history phase
compiles registration-facts and NO horizon steps. Weakened to `some`,
with the finding recorded at the assertion.

## Step 4 -- the thirteen signatures, and two corrections to the ADR

`personas-by-patient-id` -> `demographics-timeline`, and ONE lookup
shape, `(demographics-at demographics patient-id t)`. Every read site
already had its event in hand, so the `t` costs no threading beyond the
rename. Line numbers are the parameter vector's, at `2393b48`:

| | | | |
|---|---|---|---|
| `demographics-at` (new) | `:344` | `context-for-event` | `:431` |
| `z-segments-for` | `:473` | `single-subject-message` | `:515` |
| `bed-swap-message` | `:548` | `merge-message` | `:575` |
| `orm-message` | `:726` | `oru-message` | `:758` |
| `observation-message` | `:852` | `diagnostic-report-message` | `:886` |
| `event->messages` (both arities) | `:923`/`:925` | `emit` | `:974` |
| `emit-wire` | `:1050` | | |

**THIRTEEN, not twelve, and the second name is wrong in the ADR.**
ADR-0173 section 1 refinement 2 counts twelve and names
`emit-with-offsets` at `:1016`. There is no `emit-with-offsets` in this
namespace at all -- the second builder call site is `emit-wire`. And the
thirteenth signature is `z-segments-for`, which threads the map through
to `context-for-event` without reading it. Both corrections are from the
tree.

Eight read sites, all `(get personas ...)` before: the Z-segment
context, `single-subject-message`'s own `persona` binding, bed-swap's
two PIDs, merge's survivor PID, and the four `(pid-segment active-mrn
...)` calls. `pid-segment` itself is UNCHANGED -- it renders a
persona-shaped map, and today that is exactly what the lookup returns.

## The finding: row 6 did NOT go red, and the prediction is what was wrong

The prompt says `personas-are-keyed-by-patient-id-alone-test` "must go
RED on the re-key by design". **It did not, and it should not have.**

ADR-0173 section 2(b) and its Consequences both predict the red, and
both were written assuming the re-key and the FOLD land together. Split
as ruling D1 requires, the re-key alone leaves the row's substance
untouched: what row 6 states is that *a delta folded onto patient state
is invisible to every message*, and that is still true, because nothing
folds yet. The builder's body still folds nothing but the `:registered`
event's own t0 `:persona`, so every `t` answers with the same value.
**The row stands; the STRIKE is owed by part 3.**

What DID need changing is the row's own non-vacuity guard. It read
`(str/includes? src "personas-by-patient-id")`, and after the rename it
would have kept passing on a PROSE MENTION of the old name in the new
builder's docstring -- a guard green on a comment, which is no guard
(`project_two_live_vacuous_gates`' own species). It now names
`(defn- demographics-timeline`. The second assertion is unchanged: the
builder's body is still the right red-trigger, and it is still what
must move when the fold lands.

## The other finding: limitations row 10 is an ASYMMETRIC gate

`person-simulator-requires-no-engine-namespace-test` went RED at the two
commits before `11d2d50`:

```
FAIL in (person-simulator-requires-no-engine-namespace-test)
sim-engine now names person-simulator in
  ["components/sim-engine/src/ehrt/sim_engine/interface.clj"
   "components/sim-engine/src/ehrt/sim_engine/engine.clj"]
  -- a feedback edge v1 forbids
```

Both hits were PROSE: a docstring and a comment citing that component's
own `persons` front door as the future caller of `compile-patient`.
Neither is a `:require`; neither is a call.

The gate's FORWARD half -- which vars this component reaches -- matches
CALL POSITION only, with its own comment saying why: *"a docstring
naming `engine/stream-seed` in prose is a citation, not a dependency,
and a gate that cannot tell them apart punishes the documentation this
component is otherwise asked to carry."* Its REVERSE half is a bare
`str/includes?` over the whole of `components/sim-engine/src`. The same
prose it protects in one direction it forbids in the other.

Widening the reverse half was NOT this session's to do (the prompt's own
fence: no change to `person-simulator` except one named test assertion,
and a structural gate is the last thing to weaken from inside the arc it
constrains). The citations are reworded to name the component in prose
instead of by namespace, the gate stays green VERBATIM, and both sites
now say out loud why they are phrased that way. **A candidate for a
later session: make the reverse half match the forward half's own stated
principle.**

The scheduling lesson is `rulings.md#R-full-suite-before-push`'s, paid
for again: a tree-scanning gate lives in a brick OTHER than the one you
changed. `brick:sim-engine`, `brick:sim`, `brick:sim-check` and
`brick:sim-emit-fhir` were all green while this was red in
`brick:person-simulator`.

## Gates

| gate | result |
|---|---|
| `clojure -M:poly check` | **OK** at every commit -- no `sim-engine` -> `person-simulator` edge exists or was added |
| `make test` | **MAKE_EXIT=0**, 4,132 tests / 18,953 assertions, 0 failures, 0 errors |
| `make integration` | **INT_EXIT=0**, 0 `FAIL:` lines, 1,508 tests / 5,066 assertions -- +6 tests / +253 assertions vs the part-1 record's 1,502 / 4,813, which is the SAME six deftests once more (the integration project runs these bricks too) |
| `bin/regression-oracle c45ddb9 <tip>` | **IDENTICAL**, exit 0, no declaration, 35 roots -- run at FOUR points (after C1, after 2(a), after 2(b), after the re-key), never once |
| `git diff --stat c45ddb9 HEAD` on `arc0_gated_*`, `pinned_seed_42`, both conformance baselines, `demos/traces` | **empty** |
| `make traces` | regenerated, `git status --porcelain` EMPTY -- no trace moved |
| CI at the pushed tip `c7bca76` | **run 32966670638, conclusion success** -- the close marker (`rulings.md#R-session-verifies-ci-via-gh`, kept as the marker after the tag was retired). One run at this sha, the `test` workflow; no separate Integration run was scheduled for it |

**The suite delta reconciles exactly.** Baseline, measured this session
at `c45ddb9` and not inherited: `engine-test` 94 tests / 419 assertions,
`run-test` 32 tests / 152 assertions. At the tip: `engine-test` 99 / 651,
`run-test` 33 / 173. Each namespace runs TWICE under `poly test :all`
(two projects), so the whole-suite delta is `2 x (5+1)` = **+12 tests**
and `2 x (232+21)` = **+506 assertions**. That lands on
4,120 -> 4,132 tests and 18,447 -> 18,953 assertions, and 4,120 / 18,447
is exactly the figure the part-1 record carries -- an independent
confirmation that the two sessions are counting the same way.

The six new deftests, named:

| test | where |
|---|---|
| `the-registered-compile-is-arrival-time-independent` | engine-test |
| `compile-patient-is-what-registered-attaches` | engine-test |
| `the-person-index-falls-back-on-the-key-never-on-a-missing-entry` | engine-test |
| `registered-seeds-demographics-from-the-persona-and-leaves-persona-alone` | engine-test |
| `demographics-schema-carries-the-residence-sum` | engine-test |
| `every-gated-run-compiles-the-same-persona-at-any-arrival-time` | run-test |

The oracle line, verbatim, at the tip:

```
--- declared-digest-change: no (soundness: yes outside the leading docstring) ---
IDENTICAL: every root's digest matches between c45ddb9 and HEAD
```

## What part 3 inherits

* `sim-engine.interface/compile-patient` -- exported, no caller, ready
  for `ehrt.sim.run` to obtain each patient's compiled DEATH instant
  before the run.
* `:person-index` and `person-entry` -- carried, empty, reader-guarded.
* `PatientState`'s `:demographics` and the `Demographics` schema,
  including the residence SUM's three arms (`:housed`, `:unhoused`,
  `:unknown`) and `:identity`.
* `demographics-at` -- one lookup shape, one body to fill.

Still owed by parts 3 and 4, unchanged from the part-1 sizing table
except where this session closed a row: 2(a) config and selection, the
fold and its queue-seeding pass, the two new event kinds and the
contract bump (with `make event-schema-examples`'s fifth fixture run
ahead of them), 2(c)'s two hooks, 2(d) identification, 2(e)'s six
invariants, 2(f) provenance. Plus the two corrections above: row 6's
STRIKE, and -- as a candidate, not an obligation -- row 10's asymmetry.
