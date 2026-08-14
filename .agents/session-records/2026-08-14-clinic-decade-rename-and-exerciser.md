# 2026-08-14 -- Scenario rename busy-tuesday -> clinic-decade + exerciser completion (ADR-0132)

Ceremony log only -- the full narrative (the rename mapping, the
frozen/live classification, the exerciser evidence) is `notes/adr/
0132-clinic-decade-rename-and-exerciser.md`. R30 (standing default;
the driving prompt did not state prepare-only).

## Step 0 -- Ceremony + tag

`bin/preflight`: last five CI runs on `main` all green (`c27bdd3d`,
`25e595c4`, `e1a9b9a5`, `e3813a53`, `ef15885a`); edit-root ext4; tree
clean; local HEAD matched `origin/main` at
`c27bdd3dad529fc66e4a41d7ac32910c9541ea25`; last `stable-*` tag
`stable-20260813-busy-tuesday-deferral`. License satisfied:
`bin/tag-ceremony stable-20260814-slug-fix c27bdd3d... --push` --
created ANNOTATED, pushed, peeled ref verified exact match. Oracle
same-ref sanity check (`bin/regression-oracle c27bdd3d c27bdd3d`):
IDENTICAL, all 35 roots. Verified live (not assumed) that no digested
artifact embeds the scenario path string: zero `busy.tuesday`/
`busy_tuesday` hits under `components/oracle/`, and every oracle root's
own module closure resolves through `components/sim/resources/sim/
modules/`, never `demos/scenarios/`.

## Step 1 (commit `214b0ec`)

`refactor: rename busy-tuesday scenario to clinic-decade -- full
live-reference sweep (ADR-0132)` -- `git mv demos/scenarios/
busy-tuesday demos/scenarios/clinic-decade`; full live-reference sweep
across 18 files (both moved-and-edited scenario files, 4 cross-ref
READMEs, 2 config-comment sites, `bases/cli/src/ehrt/cli/help.clj`'s
sourced `play` example, `components/corpus/docs/use-cases.edn` +
its regenerated `docs/use-cases/play-a-generated-corpus-back-over-
time.md` companion, 3 docs-tooling comment sites, 3 test marker-fixture
files, `bin/readme-what-you-get`'s own comment, `.agents/plans/
roadmap.md`'s 19 live mentions). Repo-wide residue grep after every
edit: zero hits outside frozen classes (notes/adr bodies, session
records, prompt archives, the ADR/ruling registers' own existing
lines) plus this session's own disclosed mapping sentence. `make test`
green (`poly check` OK; `poly test :all skip:integration`: 632 "0
failures, 0 errors" blocks, unchanged count; `verify-nist-lock` OK).
`gitleaks git --staged -v`: clean. Pushed; `bin/post-push-verify
c27bdd3d 214b0ec`: remote tip match OK, ASCII OK, CI queued (reported
once, AR-CI-4).

## Step 2 -- red witness (no separate commit; witnessed inline before Step 2's own commit)

`bin/demo-exerciser-clinic-decade` written; script temporarily moved
aside to witness RED against its own absence:
`{:ok? false, :readme-count 5, :script-count 0, :divergence {...
:script :ehrt.docs-tooling.demo-exerciser-fresh/missing}}`. Restored,
GREEN: `{:ok? true, :readme-count 5, :script-count 5, :divergence
nil}`. Exec bit set via `git update-index --chmod=+x`, verified
`100755` via `git ls-files -s` both before and after.

## Step 2 (commit `20770dc`)

`feat: clinic-decade exerciser -- register row, script, integration
wiring; all three commands witnessed (ADR-0132)` -- `bin/demo-
exerciser-clinic-decade` (new, 100755, adapted from ADR-0130's own
drafted Appendix, one disclosed regex fix for a markdown line-wrap the
drafted script never actually hit); `exercised-sources.edn` new row
(`:demo-exerciser-fresh`, explicit `:marker-open`/`:marker-close`);
`exercised_sources_test.clj` count-lock 7 -> 8 + new dedicated test;
`strip_fresh_test.clj` new live-delegation test; `Makefile` integration
line + help text. Executed end-to-end in-session, real artifacts (seed
20260807, 200 patients): command 1 (generate) ok, 4 expected
`sleep_apnea.json` collision warnings; command 2 (`--board` play)
closing summary `{:emitted 68, :snapshot-count 48, :skip-count 41,
...}`, `inpatients: 0` on all 48 snapshots -- byte-for-byte
ADR-0130/ADR-0131's own witnessed figures; command 3 (events.edn play)
`{:emitted 367, :skip-count 49, :unparseable-count 0, ...}` -- the same
first-witnessed figures ADR-0131's own acceptance section recorded.
Full run wallclock: 504s. `make test` green (632 blocks, unchanged);
`verify-nist-lock` OK; `poly check` OK; `gitleaks` clean. Pushed;
`bin/post-push-verify 214b0ec 20770dc`: remote tip match OK, ASCII OK,
CI queued.

## Step 3 (this commit)

`notes/adr/0132-clinic-decade-rename-and-exerciser.md` (new, the full
narrative); `notes/ADRs.md` index line; `.agents/plans/roadmap.md` --
the rename+exerciser row marked CLOSED (ADR-0132), citing R3's full
discharge across every shipped scenario README; `.agents/rulings.md`
"From ADR-0132" (the name ruling verbatim); `.agents/state.md`
citation-only update (not an arc close, `state_staleness_tripwire_test.
clj` untouched); `bin/close-scaffold --expect-tag
stable-20260814-slug-fix@c27bdd3dad529fc66e4a41d7ac32910c9541ea25` --
verified locally and on remote, scaffolded this record + the prompt
archive.

**Oracle bracket**, official: `bin/regression-oracle c27bdd3d 20770dc7`
(Step 0's own baseline vs Step 2's own tip) -- IDENTICAL, all 35 roots,
matching Step 0's own verified prediction exactly.

**Count-lock probe caught a real one**: `reading-set-budget-test` went
red mid-Step-3, `:onboarding` at 2338 against its own 2335-line budget
(this session's own roadmap-row close text + two new README index
lines, atop routine churn since ADR-0125). Routine, not a STOP (same
class ADR-0107/0115/0125/0128 each hit and fixed inline) -- re-derived
per the standing formula (actual x1.15, rounded up to nearest 5): 2338
x 1.15 = 2688.7 -> 2690. `.agents/reading-sets.edn` budget moved 2335
-> 2690, dated comment recording the full per-path breakdown. Re-run
green: 5 tests, 15 assertions, 0 failures.

Final `make test` + `make integration` run after this commit lands
(the build-session skill's own checkpoint-isolation precedent,
ADR-0129's discovered practice: a small records-only checkpoint commit
ahead of the final `make integration` run, whenever that run's own
tree-clean postcondition would otherwise fail solely because this
session's own in-progress `.agents/` files are still uncommitted --
exactly the case here, confirmed live: an interim `make integration`
run mid-session correctly FAILED its own tree-clean check on these
same in-progress record files, with every named invariant otherwise
matching, before this commit landed them).

## Fence held

Committed Step 1: `demos/scenarios/**`, `bases/cli/src/ehrt/cli/
help.clj` (name string only) + regenerated docsgen companion,
`components/corpus/docs/use-cases.edn`, `components/docs-tooling/
resources/docs-tooling/exercised-sources.edn` (comment only),
`components/docs-tooling/src/ehrt/docs_tooling/{demo_exerciser_fresh,
strip_fresh}.clj` (docstrings only), `components/docs-tooling/test/
ehrt/docs_tooling/{demo_exerciser_fresh_test,strip_fresh_test,
citation_gate_test}.clj` (fixture strings/comment only), `bin/readme-
what-you-get` (comment only), `.agents/plans/roadmap.md`, root
`README.md`, `demos/README.md`, `demos/scenarios/README.md`.

Committed Step 2: `bin/demo-exerciser-clinic-decade` (new, 100755),
`Makefile`, `exercised-sources.edn` (new row), `exercised_sources_test.
clj` + `strip_fresh_test.clj` (count-lock + new tests). Zero module
JSONs, zero engine/sim `src`, zero README/figure edits either
commit.

Committed this step: `notes/adr/0132-*.md`, `notes/ADRs.md`, `.agents/
plans/roadmap.md`, `.agents/rulings.md`, `.agents/state.md`, `.agents/
session-records/2026-08-14-*.md`, `.agents/prompts/2026-08-14-*.md`,
both directories' own README index lines. Zero `src`/`test`/module-
JSON/README-figure edit -- records-only.
