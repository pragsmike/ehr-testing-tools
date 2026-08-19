# Session prompt — review-4 fix 1/5: closure gates (plan Session G + R4-Q10(d)) and harness truthfulness (plan Session A + R4-Q2(c), R4-Q3(a)) — ADR-0155

Archived verbatim (R-A). Authored 2026-08-18; executed 2026-08-19.
Session record: [`2026-08-18-review-4-fix-1-closure-and-harness.md`](../session-records/2026-08-18-review-4-fix-1-closure-and-harness.md).

---

## Context

Claude Code under R30 in ehr-testing-tools, first fix session of the
repo-review-4 arc. HEAD at handoff: 7d998f0 (ADR-0154 addendum; tree
clean; CI green at 0a07195 and 7d998f0; last tag
`stable-20260818-repo-review-4` @0a07195, no tag owed). The review
landed its register and plan and fixed nothing (ADR-0154). Author
rulings on the plan, 2026-08-18: "Q1 accept all recommendations. Q2
that order ok. Q3 pair small ones." So every R4-Q ruling is the plan's
RECOMMENDED option; the session order is G A E B C D F H; sessions are
paired. This prompt is G+A. Rows cited by id are rows of
`.agents/plans/2026-08-18-repo-review-findings.md`; sessions by letter
are `2026-08-18-repo-review-4-plan.md` Part 2. Quote the row, not this
prompt, when in doubt.

The shared principle of both halves: gates that can go silently green. G
closes populations (a prerequisite dropped from `docsgen:` or a path
from CI's diff list leaves every gate green -- L3-1 sub-agent-witnessed,
mechanism coordinator-re-derived; the baseline freeze that keeps the
only schema-change gate non-vacuous is enforced by a header sentence --
L3-2). A closes exit propagation (a taught idiom that exits 0 -- L2-2;
`bin/preflight` printing `OK:` after a failed `gh` query and exiting 0
unconditionally -- L2-3/L2-4; a Makefile loop that swallows a failure --
L2-5; comments that say "tees" where code redirects -- L2-10;
`post-push-verify` folding `gh` stderr into a status field -- L2-6).

### Channel anchors at 7d998f0 (re-derive every one)

* `Makefile:258` `docsgen:` has 12 leaves; `test.yml:41-60,102` states
  the "make target AND diff list, same commit" rule in prose; exactly
  two tests assert a docsgen prerequisite (`sim_theory_head_hop_test:
  175`, `traces_fresh_test:149`). `event-schema-freeze` appears in tests
  only inside failure strings (`event_schema_test.clj:132,137`).
* `Makefile:88-92` use-cases for-loop; `write-use-cases!` never prunes
  `pages-dir` (L3-9); `palgebra-examples` (:162-166) hardcodes three
  converter calls against FIVE
  `components/palgebra/examples/*-equations.txt` and three `.mermaid`
  (L3-10 -- two equations files have no mermaid; that is either intended
  or a finding: read them).
* L3-4: `docs/dev/pipeline.md` is an input to `state-derived.md`; the
  edge exists only as prerequisite ORDER on the `docsgen:` line.
* `.agents/skills/extraction-stage/SKILL.md:95` teaches
  `> file 2>&1; echo EXITCODE:$?`; mirror under `.claude/skills/`.
* `bin/preflight:72-74` (`gh run list` failure prints `FAIL:`), :100
  (`OK: last five runs all green (or none found)` reached after it),
  :162 (unconditional `exit 0`); no behavioral test covers
  `bin/preflight`. `bin/post-push-verify` check 3: `gh` stderr captured
  `2>&1` into the status field (L2-6). "tees" comments in
  `bin/demo-exerciser-clinic-decade`, `bin/usecase-custom-emitter`,
  `bin/demo-exerciser-ed-tuesday`, `bin/readme-what-you-get` (re-grep;
  the plan says six sites in four scripts).
* The MAKE_EXIT law's four surfaces: `build-session/SKILL.md:91` and
  HISTORY, the `.claude` mirror, plus whichever two the register L2-1
  names -- read the row.

## Read first

1. Register rows L3-1, L3-2, L3-4, L3-9, L3-10, L2-1..L2-6, L2-10; plan
   Part 1 R4-Q2, R4-Q3, R4-Q10 and Part 2 Sessions A and G.
2. `Makefile` whole (it is the subject); `test.yml` :35-145;
   `traces_fresh_test.clj`, `sim_theory_head_hop_test.clj` (the two
   prerequisite assertions and their make-graph helper -- REUSE it);
   `docsgen_test.clj`; `usecases.clj` `write-use-cases!`.
3. `bin/preflight`, `bin/post-push-verify`, the four "tees" scripts,
   `extraction-stage/SKILL.md` + mirror; `build-session/SKILL.md`
   (MAKE_EXIT section); `skill_mirror_currency_test`;
   `executable_bits_test` (the test-shape precedent for a bin-script
   behavioral test).
4. ADR-0136, 0148 (`R-empty-population-is-red`), 0149, 0152 (the class's
   prior one-at-a-time closures); ADR-0154 §D2, §L-2, §L-3.
5. `rulings.md#R-full-suite-before-push`, `#R-red-pushed-with-green`,
   `#R-session-verifies-ci-via-gh`, `#R-law-surface-propagation`,
   `#R-oracle-script-contract` (untouched here -- E's), AR-CI-4;
   build-session skill; `:docs` reading set.

## Author rulings, verbatim

* "Q1 accept all recommendations. Q2 that order ok. Q3 pair small ones."
  (2026-08-18) -- hence: R4-Q10 (d): recipe-derived closure gate now,
  PLUS the two-line freeze assertion regardless; R4-Q2 (c):
  `bin/preflight` fail-closed AND an `UNKNOWN:` branch, the `UNKNOWN:`
  half non-negotiable; R4-Q3 (a): `post-push-verify` stays advisory per
  AR-CI-4, but a non-zero `gh` renders `UNKNOWN:` instead of folding
  stderr into the status field.
* Tag: no tag owed at Step 0. This session's own close tag: pay
  in-session if its tip run concludes success while open, else next Step
  0 -- say which.

## Step 0

Fresh clone, tip 7d998f0; `bin/preflight` (note: this session changes
preflight -- run the COMMITTED one at Step 0, record its output
verbatim, then the new one at close and compare); baseline `make test`
unpiped, MAKE_EXIT captured, reconcile vs ADR-0154's 348 blocks / 3,960
tests / 17,758 assertions; `poly check`; reading sets vs baselines. Then
measure before editing: (a) docsgen's actual write set from ONE run in a
scratch worktree (`make docsgen` then `git status --porcelain` union the
paths each recipe names) vs the 19/20 diff-list paths -- record the set
difference both ways; (b) the five palgebra equations files vs three
mermaid -- which two are unrendered and why (header says?); (c)
`docs/use-cases/*.md` count vs case ids in `use-cases.edn` (predict
equal today; confirm); (d) the "tees" comment sites (grep, count); (e)
`bin/preflight` output with `gh` deliberately unreachable (`PATH`
without gh, or `GH_TOKEN=bad`) -- capture the false `OK:` line verbatim
as the red witness.

## Step 1 -- G red (docs-tooling tests)

In ONE new test namespace (or extend `docsgen_test` -- say which): (i)
R4-Q10(b) closure: every output path named in a `docsgen` leaf's recipe
appears on CI's diff list, and every diff-list path is produced by some
leaf -- set equality, derived from the Makefile recipes and `test.yml`
parsed from the tree (no hand list; `R-empty-population-is-red` applies:
assert both sets non-empty). Plant-and-withdraw red: temporarily drop
one leaf from `docsgen:` and one path from the list, witness red,
restore. (ii) L3-2: `event-schema-freeze` ∉ transitive prerequisites of
`docsgen` (same helper). Plant red: add it, witness, remove. (iii) L3-4:
`state-derived` depends on `pipeline` either as a declared prerequisite
or asserted recipe order -- choose DECLARED (`state-derived: pipeline`)
unless it breaks `make -j`; test asserts the edge exists. (iv) L3-9:
`set(docs/use-cases/*.md) == set(case ids)` -- plant an orphan page,
witness red. (v) L3-10: pair set of `*-equations.txt` ↔ `*-flow*.mermaid`
under `components/palgebra/examples/` equals what `palgebra-examples`
renders -- if Step 0(b) found the two unrendered files are intentional
(e.g. narrative-only), the test asserts the declared exception list, not
a blanket pairing; record the decision.

Commit: "test: red -- docsgen/diff-list closure, freeze off docsgen,
state-derived←pipeline, use-cases page closure, palgebra example pairing
(ADR-0155, review-4 G)"

## Step 2 -- G green

Makefile: `state-derived: pipeline` (or the ordering assertion);
`palgebra-examples` derived from the directory or its exception list
declared in the recipe comment; `write-use-cases!` prunes `pages-dir` to
the case set (and the test asserts it); `test.yml` diff list reconciled
to the measured write set from Step 0(a) -- every path the run wrote and
the list lacks is ADDED (disclose each; it is a population the gate
could not see until now), every listed path no leaf writes is a STOP
(two readings: stale list vs. a generator that silently stopped
writing). `make docsgen` twice: idempotent, freshness clean. Full `make
test` before push; push red+green together.

Commit: "feat: docsgen population closed -- recipe-derived diff-list
gate, freeze asserted off docsgen, declared state-derived edge,
use-cases page pruning, palgebra pairing (ADR-0155, R4-Q10 d)"

## Step 3 -- A red

(i) A lint over `.agents/skills/**` and `.claude/skills/**`: a taught
shell line that captures a status with `$?` into an `echo` and is not
followed (same fence) by `exit "$VAR"` / `exit $?` is red; red on
`extraction-stage/SKILL.md:95` today. (ii) FIRST behavioral test for
`bin/preflight` (shape: `executable_bits_test` / the exerciser fresh
tests): run it with `gh` made to fail and assert the output contains
`UNKNOWN:` and NOT `OK: last five`, and the exit code is non-zero; run
it in the normal clone and assert exit 0 with `OK:` lines. (iii)
`post-push-verify` check 3: with `gh` failing, output line contains
`UNKNOWN:` and the script still exits 0 (AR-CI-4 preserved) -- test it.
(iv) `Makefile:88-92` loop: a test that a failing converter inside the
loop fails the target (plant a malformed `.txt` in a scratch
`target/use-cases`, or assert the recipe text carries `|| exit 1` --
prefer behavior).

Commit: "test: red -- taught exit idioms must exit, preflight and
post-push-verify report UNKNOWN on gh failure, use-cases loop fails loud
(ADR-0155, review-4 A)"

## Step 4 -- A green

`extraction-stage/SKILL.md:95` + mirror: `EXITCODE=$?; … exit
"$EXITCODE"` shape (mirror byte-copied; `skill_mirror_currency_test`
green). L2-1: one clause on each of the law's four surfaces -- a wrapper
capturing `MAKE_EXIT` ENDS with `exit "$MAKE_EXIT"` (same sentence, all
surfaces, `R-law-surface-propagation`). `bin/preflight`: `UNKNOWN:` on
any failed query path, never falling through to `OK:`; exit non-zero on
any `FINDING:`/`FAIL:`/`UNKNOWN:` (R4-Q2 c) -- and UPDATE every caller
that relied on exit 0: grep `bin/preflight` in Makefile, skills,
scripts, `close-scaffold`; the build-session skill's Step 0 text says
preflight's output is the artifact AND its exit is now load-bearing (say
both). `bin/post-push-verify` check 3: detect non-zero `gh`, print
`UNKNOWN:` (R4-Q3 a); exit unchanged. `Makefile:88-92`: `|| exit 1` in
the loop (or `rm -rf target/use-cases` before `mkdir -p` -- say which
and why; prefer both). L2-10: reword the "tees" comments at every site
to "redirects" -- comments only. Full `make test`; push red+green
together.

Commit: "fix: exit truthfulness -- taught idiom exits its status,
preflight fail-closed with UNKNOWN on query failure, post-push-verify
renders UNKNOWN, use-cases loop fails loud, 'tees' comments corrected
(ADR-0155, R4-Q2 c, R4-Q3 a)"

## Step 5 -- register hygiene

Register rows L3-1, L3-2, L3-4, L3-9, L3-10, L2-1, L2-2, L2-3, L2-4,
L2-5, L2-6, L2-10: disposition cell → `FIXED ADR-0155` by DATED APPEND
in the cell (review 3's arcs overwrote cells in place and ADR-0154 had
to re-derive against the first commit -- do not repeat that: append,
keep the original token visible). Plan Part 2: Sessions A and G marked
landed. Roadmap `#repo-review-4` stays OPEN (arc), one line: "fix 1/5
(G+A) landed ADR-0155" -- the row is AT its six-line cap; compact a
line, don't add one. New `rulings.md` rows ONLY if a fix made law that
no row states (the preflight exit semantics probably do: one row,
`R-preflight-fail-closed`, citing ADR-0155 and R4-Q2).

## Close (self-archive FIRST)

Archive to
`.agents/prompts/2026-08-18-review-4-fix-1-closure-and-harness.md`; open
the session record; then ADR-0155 (Step 0 measures (a)-(e) vs what
landed; the diff-list additions disclosed; the preflight before/after
outputs; every plant-and-withdraw red witnessed and removed -- assert
none remains), registers, session record with `gh run view`
id/conclusion, full `make test` reconciled per namespace vs Step 0,
`bin/post-push-verify` (the NEW one -- say so), tag per ruling.

Commit: "docs: ADR-0155 -- review-4 fix 1/5: closure gates and harness
truthfulness, close"

## Fences

Files: `Makefile` (`docsgen:` edges, `palgebra-examples`,
`state-derived:`, use-cases loop -- NO recipe command changes beyond
those named), `test.yml` diff list + comment, docs-tooling tests (+
`usecases.clj` prune), `bin/preflight`, `bin/post-push-verify`, the four
"tees" scripts (comments ONLY), `extraction-stage/SKILL.md` + mirror,
`build-session/SKILL.md` (+HISTORY, mirror) for L2-1 and the preflight
exit note, registers; NO `src` outside docs-tooling; NO change to what
any gate COMMAND does (A's fence); NO `digest.clj` / oracle touch (that
is E); oracle IDENTICAL (assert once); no test deletions; every planted
red withdrawn; exit codes unpiped -- and this session's own wrappers END
with `exit "$MAKE_EXIT"` (the law it lands, applied to itself); anchored
register edits, dated appends not overwrites; R-RP. READ-BACK: the ADR
lists files touched vs this list, the diff-list delta, and preflight's
exit code before/after.
