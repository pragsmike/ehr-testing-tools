# 2026-08-03 — GMF census tool (ADR-0034): the frontier converted to data

## Scope

`.agents/plans/2026-08-02-gmf-parity-plan.md` §3 (ADR-0031 AR-1/AR-4):
built `ehrt.sim-trajectory.census`, a `sim-trajectory` dev entry point
(not a CLI verb), that walks the FULL upstream Synthea catalog at this
project's own pin, resolves each top-level module's transitive closure,
runs a seeded interpreter-layer smoke walk per module, and emits a
committed, re-runnable EDN artifact naming a load/walk verdict, gap
detail, and a wellness-timing substitution tag per module. Ran it once
against the pin, committed the artifact and a new dated summary section
in `docs/gmf-interpreter.md`, and recorded ADR-0034 plus the two AR-6
bookkeeping items. No engine or loader changes; no wave E/F/G work —
this session converts the parity frontier from narrative into data,
nothing more.

## Red→green evidence

- 5 co-landing unit tests / 20 assertions
  (`development/test/ehrt/sim_trajectory/census_test.clj`), one inline
  fixture per verdict class (`:ok-walked`/`:load-failed`/`:walk-failed`)
  plus the AR-3 substitution tag, all green via direct
  `clojure -M:dev:test` invocation (0 failures, 0 errors, run twice —
  before and after the load-closure try/catch hardening below).
- Full `clojure -M:poly test :all skip:integration`: 193 passes, 0
  failures, 0 errors, unchanged before and after this session's own
  commits (no product-brick code touched). `clojure -M:poly check`: OK
  at every checkpoint.
- The first full-catalog run itself was RED in the useful sense: it
  crashed outright (`No matching clause: GAUSSIAN`, inside
  `gmf/load-closure`, not inside this session's own code) rather than
  quietly producing a wrong artifact — caught immediately because the
  tool's own docstring claim ("never aborts on a module's failure") was
  taken as a testable invariant, not just prose. Fixed by wrapping
  `load-closure` in `try`/`catch` inside `census-one`; the re-run
  completed clean across all 85 modules.
- `gitleaks git --staged -v`: clean, both commits this session made.

## Judgment calls and their ratification status

- **Census parameters (3 seeds, mixer-seed `20260803`, registration age
  30, horizon 50 years, uniform persona config) — self-ratified per
  AR-4's own "the session picks, states it, and records it."** Not put
  to the author; a single global choice, not tuned per module, fully
  recorded in the artifact's own header (AR-5's re-runnable-to-the-byte
  requirement) so it's checkable, not just asserted.
- **Gap-detail schema (unrecognized state types / unresolved
  submodules / unresolved tables / bad lookup columns / attribute
  collisions / cyclic-closure / a catch-all `:other-rejections` bucket)
  — self-ratified, not put to the author.** AR-2 named the categories
  in prose ("unrecognized state types, transition kinds, condition
  types, unresolved attributes, unresolved submodules/tables, closure
  file count") without a schema; this session built one honestly
  mechanical to what `gmf/load-closure`'s own Result categories
  actually distinguish, rather than inventing a taxonomy the loader
  doesn't support. Transition-kind/condition-type gaps at LOAD time
  don't actually occur (the loader doesn't gate on them, only the
  interpreter does, at walk time) — so those two named categories from
  AR-2's prose surface via `:walk-failed`'s own exception data instead,
  not as a load-time gap field. Disclosed, not silently narrowed.
- **`load-closure`'s own defensive try/catch wrap — self-ratified as a
  census-tool change, explicitly NOT a loader fix, per the prompt's own
  fence.** The GAUSSIAN/EXPONENTIAL finding is real and load-bearing
  (11 modules), but the fence is unambiguous ("no loader changes... if
  observing requires changing the thing observed, that is an
  escalation") — the escalation-worthy fix belongs to `gmf.clj`, named
  in ADR-0034 and §15 for a future session, not taken here.
- **"Eight vendored roots" (the prompt's own Step 2 wording) corrected
  to seven — disclosed in the deviation-record appendix and ADR-0034,
  not silently adjusted.** Confirmed by direct listing and by
  `docs/gmf-interpreter.md`'s own pre-existing D3f prose, which already
  said seven. Not a STOP-AND-ESCALATE case: the fence's own escalation
  trigger is disagreement with a LANDED claim, and no landed claim ever
  said eight — this is an arithmetic correction to the prompt itself.

## Findings and HEAD landed

- **New, real loader gap:** `ehrt.sim-trajectory.gmf/gmf-v2-timing->v1`
  throws on `GAUSSIAN`/`EXPONENTIAL` `gmf_version 2` distribution kinds
  (11 modules combined) instead of returning `:rejected` — named in
  `docs/gmf-interpreter.md` §15 and `notes/ADRs.md` ADR-0034, not fixed.
- **Two new condition-vocabulary gaps:** `Race` (3 modules) and `Not`
  (1 module) condition types, never in this project's v1 vocabulary at
  any prior wave — named, not fixed.
- **`clojure -M:poly test :all skip:integration` does not run this
  session's own tests** (`development/`'s own `dev` poly-project has
  `:bricks-to-test []`) — the same pre-existing status
  `bin/oracle-src`'s own tooling has, disclosed rather than silently
  left unstated; verified instead by direct invocation.
- **`bin/regression-oracle`'s own checkout-only design is fragile
  across a producer-shape-changing session** — ADR-0033's own already-
  disclosed finding, given a roadmap Deferred row this session (AR-6a).
- **Census headline:** 85 modules, 40 `:ok-walked`, 39 `:load-failed`,
  6 `:walk-failed`, 0 `:out-of-scope-by-ruling`; 19 carry the
  `:wellness-timing` tag (vs. ADR-0031 AR-5(a)'s own hand-surveyed five
  — all five included, confirming the survey undercounted rather than
  erred). Ranked gap mechanisms and the full breakdown are in
  `docs/gmf-interpreter.md` §15; the artifact itself is
  `components/sim-trajectory/docs/census/2026-08-03-synthea-7e08387.edn`.
- Sanity anchors held: all seven vendored roots `:ok-walked`; all five
  named wellness modules tagged regardless of verdict. No
  STOP-AND-ESCALATE fired.

Commits: `6392363` (Step 1, tool + tests), `41c86a0` (Step 2, census run
+ doc summary), and this session's own closing records commit (HEAD at
push time — see `git log` for the exact sha; not self-referential here
per this repo's own established convention, `.agents/plans/roadmap.md`
line 592's own precedent).
