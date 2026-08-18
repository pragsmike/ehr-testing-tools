# Archived prompt: surge-policy-self-check-202 (2026-08-18)

Session prompt -- surge-policy self-check failure, seed 202 under
`--churn` with the ed-tuesday facility: repro test, diagnosis, fix --
ADR-0153

## Context

Claude Code under R30 in ehr-testing-tools. HEAD at handoff: c1a40d0
(ADR-0152 addendum; tree clean; CI green at 1e261f5 and c509e46 per
addendum; last tag `stable-20260818-sim-theory-edn-hop` @c509e46, no
tag owed). Roadmap row `roadmap.md#surge-policy-self-check-202` (OPEN
PRIORITY 6): "seed 202 under `--churn` with the ed-tuesday facility
exits `:status :error :category :self-check-failed`, violation
`:surge-only-when-earlier-rungs-exhausted` at `t 78480`. Reproducible.
Found while running the event-log census, wholly outside that arc and
disclosed rather than pursued (census S-5). Wanted: a repro test, then
the fix." Author ruling 2026-08-18: "(a) now" -- this row, this
session.

Channel probe at c1a40d0 (re-derive):

* The invariant: `sim-check/check.clj:245-258`
  `surge-only-when-earlier-rungs-exhausted` -- for every replayed
  `:admission`/`:transfer` with `[:location :placement] :surge` and not
  `:forced`, requires `earlier-rungs-exhausted?` (:227-243) at
  `world-before`'s board: rung 2 (target = home ward) needs home
  licensed exhausted; rung 4 (target != home) needs home licensed +
  home surge + every other inpatient ward's licensed exhausted.
* The allocator: `sim-model/facility.clj:92-143` `allocate` -- rung 1
  home licensed, rung 2 home surge, rung 3 other-inpatient licensed,
  rung 4 ED-class surge, else `{:exhausted true}`; `force-placement`
  bypasses; result-not-throw.
* Callers: `engine.clj:400` (admission), :423 (transfer), :553
  (transfer-in-error), each `(sim-model/allocate rng facility board
  location force-placement)` where `board` = `occupancy-board` of the
  live `patients` (which on a transfer INCLUDES the moving patient's
  own current bed) and `location` = the STEP's target ward name, which
  the event then records as `:home-ward`.
* Replay: `engine.clj:1091` `replay` folds every participant through
  `evolve`; `world-before` is the full patients map.
* Census reproduced it once (`census.md:682-687`) but recorded only
  "seed 202 under `--churn` with the ed-tuesday facility", not the full
  argv; the census's ed-tuesday shape was `corpus generate sim --seed
  <n> --patients 100 --reference-date 2026-08-11 --churn --config
  demos/scenarios/ed-tuesday/config.edn` (`census.md:44`). Start from
  that with `--seed 202`; if it does not reproduce, vary `--patients`
  and `--reference-date`, then `sim run` vs `corpus generate sim`; if
  no reproduction within a bounded search (say 12 runs, logged), STOP
  -- the row's premise fails and the author decides.
* No oracle root runs under `--churn` (`digest.clj` has no churn root
  -- grep confirms at Step 0). So an ENGINE or ALLOCATOR fix is
  predicted oracle-IDENTICAL, but the prediction is against the tree,
  not this sentence: assert it.
* `run.clj:93-105`: `:self-check-failed` is the config-reachable
  category; the exit is the run refusing to write a corpus it cannot
  vouch for -- correct behaviour if the log IS wrong, wrong behaviour
  if the CHECKER is.

The session's first job is diagnosis, not the fix: the fix's HOME
depends on which of three hypotheses the evidence supports, and they
are not equally weighted -- H3 changes what the checker vouches for;
H1/H2 change what runs produce.

* H1 allocator: `allocate` returned `:surge` while a licensed bed the
  ladder ranks earlier was free at THAT board (a rung ordering / `free`
  / `choose` defect in `facility.clj`).
* H2 engine: the allocator was right for its inputs but the inputs were
  wrong -- the `location` handed in (a churn `:transfer` targeting a
  ward that is not the patient's home ward, so the event's `:home-ward`
  is the TARGET; the checker's rung-4 branch then judges it against the
  patient's actual home), or `force-placement` set but `:forced` false
  on the event, or the moving patient's own bed counted as occupied in
  a way the checker's `world-before` does not mirror.
* H3 checker/replay: engine and checker disagree on the board at
  `t 78480` -- `replay`'s `world-before` after a churn event
  (`:bed-swap`, `:cancel-*`, `:transfer-in-error`, merge) differs from
  the `patients` map the engine decided from (an `evolve` that the
  engine's live path applies but replay does not, or vice versa), so
  the invariant fires on a board that never existed.

Decide with evidence: the offending event verbatim; the engine's
decision-time board vs the checker's replayed `world-before` board at
that `t` (dump both, diff them -- if they differ, H3 is live); the
allocator's inputs (`location`, `force-placement`, home ward) and its
rung trace. Write the diagnosis and the ruled-out hypotheses into the
ADR BEFORE writing the fix.

## Read first

1. `check.clj` :200-262 and `:560-575` (catalog); `check_test.clj`
   :90-112 (the two existing surge tests -- your red test is their
   sibling); `facility.clj` :80-150; `engine.clj` :396-440, :548-600,
   :960-975 (`evolve :bed-swap`), :1091-1130; `run.clj` :85-110.
2. `docs/operational-models.md` allocation-ladder section (the LAW the
   invariant encodes -- if the law and the checker disagree, that is a
   fourth reading and a STOP); `docs/patient-state-model.md` event-
   validity table (churn rows).
3. Census S-5 (:682-691); ADR-0150 §Step 4 (the rowing); the M2b churn
   ADR (`sim/ADR-0010`, cross-participant coherence) and M6 Task 0
   (result-not-throw, `:self-check-failed` recategorization) -- find
   their numbers via `notes/ADRs.md`.
4. `rulings.md#R-session-verifies-ci-via-gh`,
   `#R-full-suite-before-push`, `#R-red-pushed-with-green`,
   `#R-stop-only-on-two-defensible-readings`,
   `#R-register-hygiene-at-close`; build-session skill; `:sim` reading
   set.

## Author rulings, verbatim

* "(a) now." (2026-08-18) -- `[surge-policy-self-check-202]`, this
  session: repro test, then fix.
* Tag: no tag owed at Step 0. This session's own close tag: pay
  in-session if its tip run concludes success while open, else next
  Step 0 -- say which.

## Step 0

Fresh clone, tip c1a40d0; `bin/preflight`; baseline `make test`
unpiped, MAKE_EXIT captured, reconcile vs ADR-0152's 348 blocks / 3,956
tests / 17,730 assertions; `poly check`; reading sets vs baselines.
Then: reproduce (bounded search above; record the exact argv that fires
and the full `:self-check` payload); confirm no oracle root runs under
churn; grep the ladder law in `operational-models.md` and quote the
sentence the checker encodes.

## Step 1 -- diagnosis (ADR text, no src)

Instrument in a scratch REPL / `target/`, not in src: capture the
offending event, both boards, the allocator's inputs and rung trace at
`t 78480`. Name the hypothesis the evidence supports and the two it
rules out, with the artifact for each. If the evidence supports NONE
cleanly, or supports H3 AND one of H1/H2 (checker wrong AND log wrong),
STOP -- two defensible fix homes is the author's call.

## Step 2 -- red

A MINIMAL repro test in the brick that owns the fix (sim-model for H1,
sim-engine for H2, sim-check for H3): NOT "run seed 202 for 100
patients" -- extract the smallest facility + step sequence that
reproduces the misfire (the two existing surge tests at `check_test.clj:
98-109` are the shape: a hand-built log + facility). Plus, at the run
level, one test asserting `sim run` with the exact reproducing argv
returns `:status :ok`; this one may be integration-tier if it takes
more than a few seconds (say which and why). Commit: "test: red --
surge placement / self-check misfire at seed 202 under churn, minimal
repro (ADR-0153, S-5)"

## Step 3 -- green

The fix, in the diagnosed home, smallest change that makes the minimal
repro pass without changing any other existing surge/capacity test's
outcome. If the fix is H1/H2 (engine or allocator): predict then assert
`bin/regression-oracle c1a40d0 HEAD` IDENTICAL; ALSO run the census's
eleven-corpus shapes that use churn (`census.md:40-50` table --
ed-tuesday at its documented seed, and any other churn row) and record
whether their ground truth moved (declared, not silent -- these are not
oracle roots but they are documented demos; the `demos/traces/` gate
will catch the traced ones via `make traces`). If the fix is H3
(checker): the log did NOT change; the invariant's docstring and
`operational-models.md`'s law sentence must still agree -- if the fix
narrows what the checker vouches for, say exactly what is no longer
checked. Full `make test` before push; push red+green together. Commit:
"fix: <home> -- <one-line cause>; seed 202 churn self-check passes;
<oracle IDENTICAL | checker-only> (ADR-0153, S-5)"

## Step 4 -- register hygiene

Roadmap `#surge-policy-self-check-202` -> CLOSED under `## Done`;
census S-5 marked closed, dated, ADR cited. If diagnosis exposed a
class (e.g. "churn `:transfer` events record the target as
`:home-ward`"), and the fix did not close the class, one NEW row naming
it -- not a fixed-in-passing.

## Close (self-archive FIRST)

Archive to `.agents/prompts/2026-08-18-surge-policy-self-check-202.md`;
open the session record; then ADR-0153 (repro argv; diagnosis with
artifacts; hypotheses ruled out; the fix; oracle result; demo-corpus
movers), roadmap, session record with `gh run view` id/conclusion, full
`make test` reconciled per namespace vs Step 0, `bin/post-push-verify`,
tag per ruling. Commit: "docs: ADR-0153 -- surge-policy self-check 202,
close"

## Fences

src: ONE of `facility.clj` / `engine.clj` (decide sites or replay) /
`check.clj`, per diagnosis, plus tests -- not two homes; NO invariant
removed from the catalog; NO change to `operational-models.md`'s law
without a STOP; oracle IDENTICAL for H1/H2 (assert), untouched for H3;
NO event-schema change; NO `--churn` semantics change beyond the cause;
`demos/traces/` moves only via `make traces` and only if declared; no
test deletions; exit codes unpiped; `out/` cleared before runs;
anchored register edits; R-RP. READ-BACK: the ADR states the offending
event before/after the fix, and for H1/H2 the seed-202 run's event
count and self-check status before/after.
