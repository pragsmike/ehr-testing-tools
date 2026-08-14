# 2026-08-14 -- Slug EDN round-trip fix + module-load injectivity guard (ADR-0131)

Ceremony log only -- the full narrative (census tables, prediction-vs-
actual, the ruling text, the escalation trigger) is `notes/adr/
0131-slug-edn-round-trip.md`. R30 (standing default; the driving
prompt did not state prepare-only).

## Step 0 -- Ceremony + conditional tag

`bin/preflight`: last five CI runs on `main` all green, including the
three commits the driving prompt's own conditional tag license named
(`b3483dc0`, `06aec016`, `ef15885a`); edit-root ext4; tree clean; local
HEAD matched `origin/main` at `ef15885ae4cd1b35ee052843734c5dd902523a86`;
last `stable-*` tag `stable-20260813-strip-executability` at `3b30aba`.
License satisfied -- `bin/tag-ceremony stable-20260813-busy-tuesday-
deferral ef15885a... --push`: created ANNOTATED, pushed, peeled ref
verified exact match. Oracle PRE-digest, all 35 roots, direct
`ehrt.oracle.digest/-main` invocation -- recorded in `notes/adr/
0131-*.md`.

## Step 1 (commit `e3813a5`, docs-only)

`docs: slug defect census and declared-oracle-change prediction
(ADR-0131)` -- `notes/adr/0131-slug-edn-round-trip.md` (new),
`notes/ADRs.md` (index line), `.agents/plans/roadmap.md` (slug row,
prediction recorded in place). Zero `src`/`test`. Pushed;
`bin/post-push-verify ef15885a e3813a5`: remote tip match OK, ASCII OK,
CI queued (reported once, per AR-CI-4).

## Step 2 -- red witness (no commit)

New tests added to `gmf_test.clj` against genuinely pre-fix code (no
fix applied yet -- natural sequencing, no stash isolation needed).
Witnessed RED: 10 failures + 1 error out of 220 assertions (the
collision-guard tests found empty-string warnings; the round-trip
property ERRORED mid-shrink on `RuntimeException: Invalid constituent
character: ~`; the two concrete comma/parens specimens failed on the
un-folded strings). Full output quoted in `notes/adr/0131-*.md` Step 2.

## Step 3 (commit `e1a9b9a`)

`fix: slug folds non-EDN-legal chars; module-load collision guard
(warn) -- emit-read identity restored (ADR-0131)` --
`components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` (the
`slug` fold-set widening; `raw-state-names`/`state-name-collision-
groups`/`handle-state-name-collision!`/`check-state-name-collisions!`;
wired into `load-module`), `components/sim-trajectory/test/ehrt/
sim_trajectory/gmf_test.clj` (the generative property tests, concrete
specimens, guard tests). Both new tests green (75 tests, 220
assertions, 0/0). Full `make test`: green, 632 "0 failures, 0 errors"
blocks (matches this session's own pre-fix baseline count -- no other
test moved, no count-lock tripped). `clojure -M:poly check`: OK.
`gitleaks git --staged -v`: clean. Pushed; `bin/post-push-verify
e3813a5 e1a9b9a`: remote tip match OK, ASCII OK, CI queued.

## Step 4 (commit `25e595c`)

`test: oracle re-baseline per declaration; busy-tuesday events.edn
playback witnessed green (ADR-0131)` -- `bin/regression-oracle ef15885a
e1a9b9a5`: soundness IDENTICAL outside `digest.clj`'s own `(ns ...)`
form; result DIFFERS, EXPECTED (a declared change, ADR-0071/ADR-0086
precedent). Movement matched Step 1's prediction EXACTLY: 3 roots
moved (`urinary-tract-infections-engine`, `-history-engine`,
`injuries`); `veteran-lung-cancer` byte-identical, confirming the
empirically-refined no-move prediction; 5 roots warned with zero byte
movement (`sleep-apnea` x4, `hypothyroidism` x1, `colorectal` x1,
`veteran-ptsd` x2 pairs -- `injuries` warns AND moves); 27 roots
silently untouched. No STOP condition -- full prediction-vs-actual
table in `notes/adr/0131-*.md`.

Acceptance: `bin/ehrt corpus generate sim --seed 20260807 --patients
200 --config demos/scenarios/busy-tuesday/config.edn --out-dir
out/scenarios/busy-tuesday` -- ok (4 collision warnings from
`sleep_apnea.json`, expected, disclosed). `bin/ehrt play out/scenarios/
busy-tuesday --board 60 --rate 100000` -- closing summary
`:emitted 68, :snapshot-count 48, :skip-count 41`, `inpatients: 0` on
every one of 48 snapshots -- byte-for-byte ADR-0130's own witnessed
figures, unchanged. `bin/ehrt play out/scenarios/busy-tuesday/
events.edn --rate 100000` -- THE command that failed in ADR-0130
(`:play-input-unreadable`) -- now completes: `:emitted 367,
:skip-count 49, :unparseable-count 0`, a first-witnessed figure (no
prior successful run to regress against). No README edit. `out/`
gitignored, left in place; tree clean per `git status --porcelain`.
Pushed; `bin/post-push-verify e1a9b9a 25e595c`: remote tip match OK,
ASCII OK, CI queued.

## Step 5 (this commit)

`notes/adr/0131-slug-edn-round-trip.md` (already landed across Steps
1/4, no further edit this step); `notes/ADRs.md` index line (landed
Step 1); `.agents/plans/roadmap.md` -- slug row CLOSED, new vendoring-
rider row (per-pair collision corrections across the 5 modules this
session's own census found, AR-VB2-R form, guard escalation to error
rides it), rename+exerciser row marked UNBLOCKED, Done pointer added
(`2026-08-14 -- slug-edn-round-trip -- ADR-0131`); `.agents/rulings.md`
"From ADR-0131" (Q1/Q2 verbatim); `.agents/state.md` citation-only
update (not an arc close, `state_staleness_tripwire_test.clj` untouched);
`bin/close-scaffold --expect-tag stable-20260813-busy-tuesday-
deferral@ef15885a...` -- verified locally and on remote, scaffolded
this record + the prompt archive. Final `make test` + `make
integration` run below; clean tree at close.

## Fence held

Touched only: `components/sim-trajectory/{src,test}` (`gmf.clj` +
`gmf_test.clj`); `notes/ADRs.md` + `notes/adr/0131-*.md`; `.agents/`
tree. No module JSONs, no README/docs edits (beyond the ADR/roadmap/
rulings/state records this ceremony itself requires), no docs-tooling,
no `bin/` edits, no Makefile, no skills.
