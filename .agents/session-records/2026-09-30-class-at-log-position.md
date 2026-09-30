# Errata: class-at keyed by log position, proven equal to replay

2026-09-30. Base `233e1bcd`. Ceremony: R30 (commit and push at each
checkpoint), taken from the session prompt, which stated no
prepare-only mode. The code commit was held locally and pushed together
with this record, so no red tip ever reached CI.

`bin/preflight` ran LATE -- after the step 1/2 edits, not first (a
process slip, disclosed). Its only finding was this session's own four
modified files; everything else OK: last five CI runs on main green,
repo root `/home/mg/src/ehr-testing-tools`, `core.fileMode` true,
`core.ignorecase` unset, HEAD `233e1bcd` matched `origin/main`, HEAD not
tagged `stable-*` (disclosed, no tag owed). The session-start
`git status` was clean.

## For downstream

> no wire byte moves; PV1-2 on order-less ORUs is now keyed by log
> position, which changes nothing in any shipped corpus; ground truth
> unchanged.

Code at `ded0a311`.

## Author ruling

2026-09-30, errata: `class-timeline` becomes `{pid [[log-index class]
...]}` and `class-at` reads the class held BEFORE the event's own
index, exactly as `location-at` does; the `:inpatient` fallback stays.

## Where the tree differed from the prompt

1. **Step 3's branch rule did not describe the outcome.** The prompt
   tied the oracle to step 1: green at baseline -> IDENTICAL, red
   anywhere -> DIFFERS on exactly those roots. Step 1 was RED on every
   root, yet no wire byte could move: every divergence was an opener
   (`:outpatient-visit`, `:admission`) reading its own class, and only
   `:observation`/`:diagnostic-report` render through `class-at`. The
   step-1 roots are also the sim brick's gated corpora, not the oracle's
   roots. So the oracle outcome was PREDICTED in process (below) rather
   than read off step 1, and the prediction -- IDENTICAL, no flag --
   held. Fix-forward with disclosure: only one reading is consistent
   with the tree.
2. `class-timeline` now folds into every patient participant (it read
   the first one), for `location-timeline`'s "arm for arm with evolve"
   parity. Every class-setting event carries one patient participant
   today, so this moves nothing; it is disclosed because it is a
   change beyond the ruling's two sentences.
3. `class-at` answers the fallback for a nil index as well as a nil
   timeline, as `location-at` answers nil.

## Step 1 -- red at baseline, the finding

`ehrt.sim.run-test/the-class-timeline-is-replays-class-at-every-event`,
run at `233e1bcd` with the t-keyed call
(`(timelines/class-at classes pid (:t ev))`), every patient participant
of every event, replay's before-state `:class` with nil read as
`:inpatient`:

| root | (event, participant) pairs diverging | by event |
|---|---|---|
| `seed-202-ed-tuesday` | 33 | `:outpatient-visit` 33 |
| `seed-424242-clinic-decade` | 64 | `:outpatient-visit` 61, `:admission` 3 |
| `seed-5-clinic-decade` | 48 | `:outpatient-visit` 47, `:admission` 1 |
| `adhd-seed-45` | 1 | `:outpatient-visit` 1 |
| `dense-7500-750` | 717 | `:outpatient-visit` 716, `:admission` 1 |

Zero on `:observation` or `:diagnostic-report`. The existing PV1-2 test
(`order-less-oru-pv1-2-is-the-patients-class-at-t`, whose own expected
fold was already by log order) was green in the same run.

A hand-built emitter case,
`emit_hl7_test/the-class-an-order-less-oru-renders-is-keyed-by-log-position`
(an observation before and one after an `:outpatient-visit`, all at
t=100), run against `233e1bcd`'s src (stashed): `actual: (not (= ["I"
"O"] ["O" "O"]))`.

## Step 2 -- the fix (`ded0a311`)

* `timelines/class-timeline`: `{pid [[log-index class] ...]}` over
  `(map-indexed vector ground-truth)`; arms unchanged
  (`:admission`/`:cancel-discharge` I, `:outpatient-visit` O,
  `:cancel-admit` cleared), checked against `evolve`: those four are the
  only arms that touch `:class`.
* `class-at [classes pid log-index]`: the opener strictly before the
  index; `:inpatient` when none, cleared, or nil timeline/index.
* `observation-message` and `diagnostic-report-message` pass
  `(segments/log-index-of ev)`, the accessor `location-at` already uses
  (the sim-purity lint cannot read an alias-qualified keyword).
* Docstrings/comments restated (`messages.clj`, `emit_hl7_test.clj`).

Gates: the four targeted tests (class replay-equality, the hand-built
same-second case, the PV1-2 corpus test, the location replay-equality)
0 failures by a counting `do-report` wrapper. `clojure -M:poly test
brick:sim-emit-hl7 :all skip:integration`: EXIT=0, 98 `Test results`
lines, none with a failure or error, `emit-hl7-test` present;
`brick:sim` likewise: EXIT=0, 38 lines, `run-test` present. `git diff --
components/sim-engine`: 0 lines.

## Step 3 -- the instruments, `233e1bcd` vs `ded0a311`

**Predicted first**, in process over `@#'ehrt.oracle.digest/roots`: per
root, order-less ORU events whose class differs between the old rule
(t-keyed, at or before, first participant) and the new: **0 on all 39
rendering roots**; 3 carry no `:hl7` (`appendicitis`, `ear-infections`,
`sore-throat`).

* `bin/ground-truth-bracket 233e1bcd ded0a311`, no flag --
  **IDENTICAL** on 39 roots (3 skipped, no such key). Exit 0.
* `bin/regression-oracle 233e1bcd ded0a311`, no flag -- **IDENTICAL:
  every root's digest matches**. Exit 0. Prediction matched.

## Step 4 -- docs and the suite

ADR-0174's 2026-09-30 PV1-2 note amended in place: its rule sentence
now reads by log position (the original "at the event's `t`" wording
quoted), and a dated errata paragraph states the position rule, why `t`
is wrong, the step-1 figures and the no-wire-move result.
`formats.md`/`consuming-ground-truth.md` do not state the PV1-2 key, so
neither moves. No roadmap row.

Full `make test`, unpiped, `MAKE_EXIT` captured, after `make docsgen`
with this record and the prompt archive on disk and staged: **MAKE_EXIT=0**
-- 432 `Test results` lines, 29,291 passes, 0 failures, 0 errors. (This
record's result figures were filled in after that run; nothing else
changed between the run and the commit.)

## Process disclosures

* `bin/preflight` late (above).
* Background processes this session started: the brick run, the
  prediction/bracket/oracle run, `make test`, two Monitors (all
  completed) and one redundant `until` waiter on the brick log, stopped
  with `TaskStop` before it reported.

CI green on the pushed tip is this session's close marker.
