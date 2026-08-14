# 2026-08-14 -- Exact-name state resolution: collision fix, restoration cascade (ADR-0133)

Ceremony log only -- the full narrative (census tables, the twelve-
category field inventory, the ordering-defect find, the capture-
avoidance proof, both mid-session STOP-AND-REPORTs and their rulings,
the prediction-vs-actual table) is `notes/adr/0133-exact-name-
resolution.md`. R30 (standing default; the driving prompt did not
state prepare-only).

## Step 0 -- Ceremony + licensed tag

`bin/preflight`: last five CI runs on `main` all green; edit-root
ext4; tree clean; local HEAD matched `origin/main` at `c3b6fbc`
(ADR-0132's own close); last `stable-*` tag `stable-20260814-slug-fix`
at `c27bdd3`, HEAD not yet tagged. License satisfied: `bin/tag-
ceremony stable-20260814-clinic-decade c3b6fbc... --push` -- created
ANNOTATED, pushed, peeled ref verified exact match.

## Step 1 (commit `ded3569`, docs-only)

`docs: exact-name resolution census and declared-oracle-change
prediction (ADR-0133)` -- `notes/adr/0133-exact-name-resolution.md`
(new), `notes/ADRs.md` (index line), `.agents/plans/roadmap.md`
(vendoring-rider row, prediction recorded in place). Re-derived the
10-pair/5-module collision census from scratch (exact match to
ADR-0131); found the driving prompt's own channel-walker field
inventory undercounted (2,839 real name-valued refs across twelve
categories, not ~2,331 across seven); found a live, load-bearing
ordering defect (`clojure.data.json` loses key order past 8 object
entries, confirmed by decompiling the library); proved capture-
avoidance by construction (a double-hyphen disambiguation suffix `--N`
that `slug`'s own fold can never produce). Declared prediction: 5
roots MOVE, 30 stay identical. Pushed; `bin/post-push-verify c3b6fbc
ded3569`: remote tip match OK, ASCII OK, CI queued.

## Step 2 -- red-then-green, two STOP-AND-REPORTs, four commits

Core fix landed red-then-green clean (84/84 gmf-test green). Mid-
implementation, `ehrt.docs-tooling.sim-purity-lint-test` caught a
mutable-state `atom` (ADR-0108's own two-exception purity census) --
redesigned around a pure sentinel-value walk before any test ran
against it, no separate red/green cycle needed (the redesign happened
before the atom-based version was ever tested green).

Verifying against `make test` surfaced TWO STOP-AND-REPORTs, both
relayed to the author and both ruled -- "the restoration cascade":

- **STOP 1**: `veteran-ptsd` population tests threw `run-module
  exceeded max-steps`. Ruled: the module is correctly authored; the
  trip is ADR-0105's own population-count zero-advance semantics
  false-firing on a real, legal, long-lived recurring-care loop,
  unmasked by the restoration. Licensed: `consume-step-budget`
  switches to the OTHER ADR-0105-licensed semantics (reset on any
  advance). Checkpoint-isolated red (disposable stash) before green; a
  new synthetic `bounded-burst-module` proves the distinction; existing
  zero-advance-spin tests still throw post-fix.
- **STOP 2**: past the interpreter fix, both `veteran-ptsd` tests still
  threw -- `IllegalArgumentException: No matching clause: :virtual` in
  `compile-trajectory`'s own `encounter->step`. A decision ADR-0029
  D3f's own `gmf.clj` docstring had explicitly deferred to "whichever
  future session first exercises a closure through the full compile-
  trajectory pipeline" -- this one. Licensed: `:virtual` aliases to
  `:outpatient-visit`/`:outpatient-visit-end` at BOTH dispatch sites
  (the Wave B "outpatient" precedent). Checkpoint-isolated red before
  green; `gmf.clj`'s own docstring updated to record the resolution.

Four checkpoint commits, each red-before-green witnessed via
disposable-stash isolation where the fix landed alongside pre-existing
code:

1. `91dc34c` -- `fix: exact-name state resolution -- raw-name table,
   deterministic disambiguation, strict-miss rejection; guard becomes
   disclosure (ADR-0133)` -- `gmf.clj`/`gmf_test.clj`.
2. `017f696` -- `fix: max-steps resets on any time advance, not a
   lifetime population count (ADR-0133, licensed widening)` --
   `gmf_interpreter.clj`/`gmf_interpreter_test.clj`.
3. `53555be` -- `fix: :virtual encounters compile to outpatient-visit,
   both dispatch sites (ADR-0133, licensed widening)` --
   `compile_trajectory.clj`/`compile_trajectory_test.clj`.
4. `69e1652` -- `test: re-baseline three pinned trajectory-content
   counters (ADR-0133)` -- three `sim-emit-hl7` vendored test files'
   own pinned values, moved as a direct, predicted consequence of 1-3
   (`colorectal`/`veteran-ptsd`'s own `suppressed-straddle-spans`,
   `injuries`'s own `synthesized-encounter-ends`), disclosed inline
   old-vs-new.

Full local suite green throughout (335 tests / 904 assertions across
the seven directly-touched namespaces after commit 4; `clojure -M:poly
test :all skip:integration`: 632 "0 failures, 0 errors" blocks,
matching this session's own pre-fix baseline count exactly). `clojure
-M:poly check`: OK, checked after every commit. `gitleaks git --staged
-v`: clean before every commit. Pushed; `bin/post-push-verify ded3569
69e1652`: remote tip match OK, ASCII OK, CI queued.

## Step 3 (commit `0d32d20`, docs-only)

`test: oracle re-baseline per declaration; restored-state trajectories
witnessed (ADR-0133)` -- `bin/regression-oracle c3b6fbc 69e1652`:
soundness IDENTICAL outside `digest.clj`'s own `(ns ...)` form; result
DIFFERS, EXPECTED. 4 of 5 predicted movers matched exactly
(`colorectal`/`injuries`/`sleep-apnea`/`veteran-ptsd`); `hypothyroidism`
predicted MOVE but stayed byte-identical -- investigated and explained
(both its own collision-pair members are `:exact`-severity Symptom
states, zero RNG draws, zero emitted events, neither symptom name read
by any condition anywhere in the module -- restored, real, but
structurally unobservable to anything the digest measures), not a bug,
not a new STOP-worthy layer. Corrected split: 4 movers, 31 identical.
All 10 predicted disambiguation disclosures fired with the exact
predicted content; zero occurrences of the old `WARN:` text anywhere
in the run. Restored content witnessed directly: 47 real ground-truth
events at `veteran-ptsd`'s own exact oracle parameters citing a
previously-orphaned state, including real `:outpatient-visit`/
`:observation`/`:outpatient-visit-end` triples citing `:telehealth-
visit`. No STOP condition beyond the two already ruled in Step 2.
Pushed; `bin/post-push-verify 69e1652 0d32d20`: remote tip match OK,
ASCII OK, CI queued.

## Step 4 (this commit)

`notes/adr/0133-exact-name-resolution.md` (Step 4 close section added
this step); `notes/ADRs.md` index line rewritten to a full closed
account (was Step-1-only); `.agents/plans/roadmap.md` -- vendoring-
rider row CLOSED, superseding its own original per-module-JSON-edit
framing, full restoration-cascade account inline; `.agents/rulings.md`
"From ADR-0133" (all four rulings verbatim -- the two from the driving
prompt's own "Author rulings," the two mid-session STOP-AND-REPORT
resolutions -- each with its own "Executed exactly as ruled" account);
`bin/close-scaffold --expect-tag stable-20260814-clinic-decade@c3b6fbc...`
-- verified locally and on remote, scaffolded this record + the prompt
archive. Final `make test` run below; clean tree at close.

## Fence held

Touched: `components/sim-trajectory/{src,test}` (`gmf.clj`/
`gmf_test.clj`/`gmf_interpreter.clj`/`gmf_interpreter_test.clj`/
`compile_trajectory.clj`/`compile_trajectory_test.clj`); three
`sim-emit-hl7` vendored test files (pinned-value re-baseline only);
`notes/ADRs.md` + `notes/adr/0133-*.md`; `.agents/` tree. Zero module
JSON touched anywhere (ADR-0071 vendoring preserved verbatim, NOTICE
hashes unmoved). Zero `slug`-function changes (ADR-0131's own fold
stays settled law -- this session changed RESOLUTION, not folding).
Every src change outside `gmf.clj` itself was a mid-session
STOP-AND-REPORT, explicitly author-licensed before landing, never a
unilateral scope expansion. No skill edits, no `bin/` edits, no
Makefile, no docs-tooling.
