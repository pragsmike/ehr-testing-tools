# Wire fidelity: order-less ORU PV1-3 from the patient's bed before the event

2026-09-30. Base `b1facded`. Ceremony: R30 (commit and push at each
checkpoint), taken from the session prompt, which stated no
prepare-only mode. The code commit was held locally and pushed together
with this record, so no red tip ever reached CI.

`bin/preflight` ran first, exit 0, no findings: last five CI runs on
main green; repo root `/home/mg/src/ehr-testing-tools` (not under
`/mnt/`), `core.fileMode` true, `core.ignorecase` unset; tree clean
including untracked; HEAD `b1facded` matched `origin/main`; HEAD not
tagged `stable-*` (disclosed, no tag owed).

## For downstream

> wire-only; ORU observations/reports during admissions now carry the
> patient's bed in PV1-3; nothing else moves; ground truth unchanged.

Code at `4195fe1b` and `832ad0a0` (the second is step 6's fix, below).

## Author ruling

2026-09-30, "next". The channel's expectations were checked against the
tree; one did not hold as worded (below).

## Where the tree differed from the prompt

1. **The lookup is keyed by LOG INDEX, not `t`.** The prompt framed
   PV1-3 as "the fold's most recent placement at or before `t`" and, in
   the same breath, required the fold to be PROVEN equal to `replay`'s
   before-state (step 1(b)). Those two disagree whenever a location-
   moving event is logged in the same second as, but after, an
   observation. Measured at the 750 cell: a `t`-keyed lookup disagrees
   with replay on **297 of the 1,019** observations/reports taken in a
   bed — 235 logged in the same second as, and before, the `:discharge`
   that ends their stay (a `t`-keyed read would blank a bed the patient
   was still in), 47 before a same-second `:bed-swap`, 12
   `:cancel-transfer`, 2 `:cancel-admit`, 1 `:cancel-discharge`. Only
   one reading satisfies the prompt's own invariant, so this is
   fix-forward WITH DISCLOSURE, not stop-and-report
   (`rulings.md#R-stop-only-on-two-defensible-readings`):
   `location-at` answers the most recent entry STRICTLY BEFORE the
   event's `::segments/log-index`, which both `emit` and `emit-wire`
   already stamp. `timelines` still requires nothing; the builders pass
   the index.
2. `timelines/location-at` is threaded as a new trailing `locations`
   argument of `event->messages` (a 13-arity; the 12-arity yesterday's
   `classes` added now delegates with nil), the same shape `classes`
   took. No caller outside `sim-emit-hl7` uses either arity.
3. The corpus-scale tests require `ehrt.sim-emit-hl7.timelines`
   directly from `ehrt.sim.run-test`, by the precedent of that file's
   existing `ehrt.sim-engine.fold` require; `poly check` OK.
4. `formats.md` and `consuming-ground-truth.md` do not state PV1-3, so
   neither is edited. The one doc that does describe an empty PV1-3 on
   an ORU (`components/sim/docs/patient-state-model.md:557`) is about
   the ORDER-LINKED ORM/ORU of an outpatient order, which this session
   does not touch.

## Step 1 — red, for the stated reasons only

Targeted runs at `b1facded` with the tests in place:

* (a) `emit_hl7_test/the-bed-an-order-less-oru-renders-follows-the-patients-location`:
  `actual: (not (= ["Renal^^RENAL-01^general-hospital"
  "Cardiology^^CARDIO-02^general-hospital"
  "Renal^^RENAL-01^general-hospital" nil] ["" "" "" ""]))` — all four
  PV1-3s empty. The fourth expected value was then corrected from `nil`
  to `""` (how `get-field-first-value` reads an empty field); the red
  reason is unchanged.
* (b)+(c) `ehrt.sim.run-test`: `Syntax error compiling at
  (ehrt/sim/run_test.clj:1985:19). No such var:
  timelines/location-timeline` — the fn did not exist.

## Step 2 — the fix (`4195fe1b`)

* `timelines/location-timeline`: one pass, `{pid [[log-index
  location] ...]}`, into EVERY patient participant as `apply-events`
  folds it: `:admission`/`:transfer`/`:cancel-transfer`/
  `:cancel-discharge` set the event's own `:location` (nil clears);
  `:discharge` clears unless `:disposition :expired`; `:cancel-admit`
  clears; `:bed-swap` sets each from `(get-in ev [:swap pid :to])`;
  `:merge` clears the `:merged` role only. The arm table was checked
  against every `defmethod evolve` in `sim-engine` (29 arms; these 8 are
  the only ones touching `:location`, and `state/initial-patient` seeds
  none).
* `location-at [locations pid log-index]`: nil when no timeline or no
  index — the byte both builders rendered before.
* `emit` and `emit-wire` compute it unconditionally beside
  `class-timeline`; only `observation-message` and
  `diagnostic-report-message` read it. `dft-message` still reads the
  closing event's own `:location`; ORM/ORU/ADT builders unchanged.
* `git diff b1facded 4195fe1b -- components/sim-engine`: empty.

Gates: 1(a)(b)(c) green in a targeted run. `test-vars` prints only
failures, so the figures were measured directly rather than trusted
from silence: at the 750 cell, 33,306 events, 1,020 order-less ORU
events, **1,019 with a bed** in replay's before-state (779 observation,
240 diagnostic-report) — the prompt's figure. `clojure -M:poly test :all
brick:sim-emit-hl7:sim skip:integration`: **POLY_EXIT=0**, 120 `Test
results` lines, none with a failure or error. `poly check` OK.

## Step 3 — measured, 750 cell, `b1facded` vs `4195fe1b`

Rendered in-process (`run-command {:seed 20260824 :patients 750 :churn
true :config "demos/scenarios/dense-7500/config.edn" :emit "hl7"}`) at a
scratch worktree of `b1facded` and at the fix; 40,291 messages both
sides, event-kind sequence identical.

* **1,019 messages change**: ORU^R01 from `:observation` 779,
  ORU^R01 from `:diagnostic-report` 240. Every change is PV1-3 empty ->
  populated on an observation/report whose replay before-state has a
  bed. Nothing else changes.
* **Blanking PV1-3 on exactly those 1,019 reproduces the before-wire
  byte for byte** (whole 40,291-message vector equal). A first pass of
  this check read false because the scratch splitter dropped each
  message's trailing `\r`; fixed in the script (split limit -1), not a
  wire difference.
* `gate v2` (HAPI), the 1,019 changed messages each side: **1,019/1,019
  pass both sides**, zero findings.
* `gate v2-nist` against `COVID19_ELR-v2.3.1`: 1,019 rejected both
  sides (as yesterday: the ELR profile is not this corpus's).
  **No new finding class** — the same seven codes both sides. Three
  existing classes GROW, by the same amount on every message and all at
  PV1-3, the field that is now present: `structure/O-Usage` +5
  (8,573 -> 13,668; PV1-3, -3.1, -3.3, -3.4, -3.4.1),
  `structure/Length Spec Error` +3 (28,000 -> 31,057; -3.1, -3.3,
  -3.4.1), `value-set/VS Not Found` +3 (4,316 -> 7,373; -3.1, -3.3,
  -3.4.1). None shrinks; no finding outside PV1-3 moves.

## Step 4 — the instruments, `b1facded` vs `4195fe1b`

**Predicted before either banner**, by loading `ehrt.oracle.digest` in
process and counting, per root, observation/report events whose
`replay` before-state has a `:location`: DIFFERS on `injuries` (51),
`sepsis` (25), `urinary-tract-infections-engine` (288),
`urinary-tract-infections-history-engine` (210); IDENTICAL on the other
38 (35 with zero such events, 3 with no `:hl7` key: `appendicitis`,
`ear-infections`, `sore-throat`).

* `bin/ground-truth-bracket b1facded 4195fe1b`, no flag —
  **IDENTICAL**: every digested root's `:ground-truth` matches, 39
  roots (3 skipped, no such key). Exit 0.
* `bin/regression-oracle b1facded 4195fe1b --declared-digest-change`
  (soundness: IDENTICAL outside the leading docstring) — **DIFFERS on
  exactly the four predicted**: `injuries`, `sepsis`,
  `urinary-tract-infections-engine`,
  `urinary-tract-infections-history-engine`; IDENTICAL on the other 38.
  Exit 1 is that script's DIFFERS banner, expected under the declared
  change. **Prediction matched exactly.**

## Step 5 — docs

ADR-0174 dated note beside yesterday's (PV1-3 on the order-less ORU
kinds derives from the patient's bed before the event, proven equal to
replay; the index-not-`t` finding; the oracle set).
`formats.md`/`consuming-ground-truth.md` unchanged (item 4 above). No
roadmap row. `make docsgen` with the code committed: exit 0, regenerated
nothing beyond this session's own edits.

## Step 6 — the suite

**First run: MAKE_EXIT=2.** `ehrt.docs-tooling.sim-purity-lint-test`
errored on two tests (11 passes, 0 failures, 2 errors), both
`java.lang.RuntimeException: Invalid token: ::segments/log-index`: that
tree-scanning gate reads sim src with a bare reader that cannot resolve
an alias-qualified keyword, and `4195fe1b`'s two builders read the index
as `(::segments/log-index ev)`. A gate in a brick this change does not
touch, so the targeted brick run could not see it — the reason the full
suite precedes a push (`rulings.md#R-full-suite-before-push`).

**Fix, `832ad0a0`**: `segments/log-index-of`, the read half of
`stamp-log-index`, and the two builders call it. The purity-lint
namespace re-run alone: 5 tests, 14 passes, 0 failures, 0 errors; the
hand-built PV1-3 test green. `bin/regression-oracle 4195fe1b 832ad0a0`
(no flag): **IDENTICAL** on every root, exit 0.

**Second run**, full `make test`, unpiped, wrapper ending `exit
"$MAKE_EXIT"`, after `make docsgen` with this record and the prompt
archive on disk and staged: **MAKE_EXIT=0** — 432 `Test
results` lines, 29,277 passes, 0 failures, 0 errors. (This record's
two result figures were filled in after that run; nothing else changed
between the run and the commit.)

## Process disclosures

* A hand-rolled `until grep ... POLY_EXIT` waiter was started for the
  brick run, against build-session step 13, and stopped with
  `TaskStop` before it did anything; the brick job's own completion
  notification was used instead.
* Background processes this session started: the brick run, the
  bracket/oracle run, two `make test` runs and the second oracle run
  (all completed), and that waiter (stopped). The
  scratch worktree at `b1facded` was removed and pruned.

## Findings, not acted on

1. `test-vars`-style targeted runs report only failures; a targeted
   green is silence, not a count. This session measured the figures
   the tests guard separately (step 2).

CI green on the pushed tip is this session's close marker.
