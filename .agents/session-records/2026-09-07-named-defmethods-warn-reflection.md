# 2026-09-07 — named `defmethod`s for `decide` and `evolve`; reflection warnings on

A scaffolding rider between site 6 and site 7 of the generate-quadratic program
(`roadmap.md#performance-residual-sites` PRIORITY 1), not a site of its own: it
changes how site 7 will READ a profile, and changes nothing a profile measures.
Ceremony mode: R30 (commit and push at each checkpoint), taken from the prompt.
Prompt archived at
[`../prompts/2026-09-07-named-defmethods-warn-reflection.md`](../prompts/2026-09-07-named-defmethods-warn-reflection.md).
No ADR was written and none was owed — under the de-scaffold ruling an ADR is for a
payload-behaviour or contract decision, and this session changes neither. The two
author rulings it enacts, D2 (`R-named-methods`) and D4 (`R-warn-reflection`), are
verbatim in the prompt archive.

Rulings in force: **R-named-methods** and **R-warn-reflection** (both new with this
prompt), **R-move-not-improve**, **R-pins**, **R-edit**, **R-cap**, **R-sentinel**.
No measurement was taken and none was owed.

## 0. Preflight

`bin/preflight` ran first, exit 0, no findings: last five CI runs on `main` all green,
edit root `/home/mg/src/ehr-testing-tools` (not `/mnt/`), `core.fileMode` true,
`core.ignorecase` unset, working tree clean including untracked files, local HEAD
`836c764b` equal to `origin/main`. Session start and every bracket baseline is that
sha.

The instrument's own zero ran before any edit: `bin/ground-truth-bracket 836c764b
836c764b` reported IDENTICAL on all 38 digested roots (3 skipped, no `:ground-truth`
key).

## 1. What landed

**`36af9103` — `engine: name every decide and evolve method for stable profile
attribution (D2)`.** Fifty-nine `defmethod` forms gain a name in the fn-tail and
nothing else: `decide-<kind>` on all 32 `decide` methods, `evolve-<kind>` on all 27
`evolve` methods, the kind spelled exactly as the dispatch keyword's own name. Bodies,
arglists and docstrings untouched.

**`87a9b502` — `dev: warn-on-reflection in the dev user namespace (D4)`.**
`development/src/user.clj`, twenty-six lines of which one is executable. The `:dev`
alias already put `development/src` on the classpath (`deps.edn:18`); the directory
held only a `.keep` until now.

**This record, its prompt archive, the step-4 comment on `profile-cell.sh`, and the
two generated `INDEX.md` files with the two `state-derived.md` counts that move with
them.** A close-marker commit follows once CI is verified green with `gh run view`.

## 2. The names, and that they are really in the classes

`defmethod` expands to `(fn ~@fn-tail)`, so an unnamed method compiles to a gensym
class — `ehrt.sim_engine.decide$eval7081$fn__7084` — carrying an eval ordinal and no
clue which kind it implements. That is what sites 5 and 6 had to resolve against
source to attribute a profile frame, and site 6 had to do it TWICE for one matched
pair, because deleting two anonymous fns from `decide :bed-swap` shifted every later
ordinal in `decide.clj` by 13.

The rewrite was one `sed -E` run from a script (R-edit) that counted 32 and 27 before
and after, refused any form already carrying a name token, and checked every name
against its own dispatch keyword. The invariant the prompt named holds exactly:
`git diff --stat` is two files, 59 insertions and 59 deletions; a mechanical pass over
`git diff -U0` showed 58 of the 59 hunks to be literally `-(defmethod X :k` /
`+(defmethod X :k X-k`, and the 59th is `evolve :procedure`, the one-liner whose
arglist shares its line, which took the same single token and nothing else.

**Verified by a real load, not a grep.** `clojure -M:dev` walked both
`.getMethodTable`s and read `(.getName (class f))` for every method: 32/32 and 27/27
now contain their own name. Samples —
`ehrt.sim_engine.decide$eval7054$decide_bed_swap__7057`,
`ehrt.sim_engine.decide$eval7068$decide_merge__7071`,
`ehrt.sim_engine.evolve$eval6547$evolve_merge__6549`.

**The eval ordinal is still there and is still unstable.** Naming does not remove
`eval<n>`; it removes the NEED to read it. A per-kind frame can now be matched the way
every other row in `profile-cell.sh` is matched — `decide\$decide_bed_swap[.$]` — and
the ordinal resolution is retired rather than made reliable.

`bin/ground-truth-bracket` reported IDENTICAL against `836c764b` on all 38 digested
roots over this commit's tree, which is the whole output claim: a name in a class file
cannot move a fact.

## 3. The reflection warnings, listed and not fixed

`development/src/user.clj` turns `*warn-on-reflection*` on, and a `-M:dev:test` process
then loaded all 38 `sim-engine` and `sim-check` namespaces (src and test) and RAN
their tests: 404 tests, 2,577 assertions, 0 failures, 0 errors. The compiler emitted
**57 reflection warnings**. Every one is listed here; none is fixed, per D4.

**Eleven are this repository's own code.**

| file | line:col | what could not be resolved |
|---|---|---|
| `ehrt/kernel/artifact.clj` | 206:12 | `java.lang.ProcessBuilder` ctor |
| `ehrt/kernel/invocation.clj` | 46:14 | `java.lang.ProcessBuilder` ctor |
| `ehrt/sim_emit_hl7/hl7_time.clj` | 59:8 | method `format` (target class unknown) |
| `ehrt/sim_emit_hl7/hl7_time.clj` | 59:17 | method `plusSeconds` (target class unknown) |
| `ehrt/sim_emit_hl7/v2_replay.clj` | 80:21 | static `of` on `java.time.LocalDateTime` (int, then five unknowns) |
| `ehrt/sim_emit_hl7/v2_replay.clj` | 91:11 | field `toEpochMilli` |
| `ehrt/sim_emit_hl7/v2_replay.clj` | 91:26 | field `toInstant` |
| `ehrt/sim_emit_hl7/v2_replay.clj` | 91:38 | method `atZone` (target class unknown) |
| `ehrt/sim_model/facility.clj` | 136:19 | method `nextInt` (target class unknown) |
| `ehrt/sim_engine/churn_scenarios_test.clj` | 226:14 | method `indexOf` (target class unknown) |
| `ehrt/sim_engine/churn_scenarios_test.clj` | 228:23 | method `indexOf` (target class unknown) |

**Forty-six are a third-party jar**, `org.clojars.cmiles74/clojure-hl7-parser 3.5.1`
(`components/sim-emit-hl7/deps.edn`), whose Clojure sources ship in the jar and are
therefore compiled — and warned about — in this process: 43 in
`com/nervestaple/hl7_parser/parser.clj`, 2 in `message.clj`, 1 in `util.clj`, almost
all of them `read`/`unread` on an untyped `PushbackReader`. Nothing in this repository
can fix those, and the surface is not selective about whose code it compiles. Worth
knowing before anyone reads "57" as a repo figure.

## 4. Findings

**(a) THE HOT PATH THIS WHOLE PROGRAM HAS BEEN OPTIMIZING IS REFLECTION-FREE.** Zero
warnings in `decide.clj`, `evolve.clj`, `fold.clj`, `log_index.clj`, `streams.clj`,
`run.clj` or `check.clj` — the seven files sites 1-6 touched and the one site 7 will.
That is a real negative result: whatever is left of the generate curve, it is not
reflective dispatch in the fold.

**(b) `sim-model/choose` REFLECTS ON EVERY SEEDED DRAW.**
`facility.clj:136` is `(.nextInt rng (count candidates))` inside `choose`, the uniform
seeded choice "consuming exactly one RNG draw regardless of candidate count" — and
`rng` carries no type hint, so that call is reflective at every draw it serves. It is
the only warning of the eleven that sits on a per-event path. Recorded, not fixed
(D4); one `^java.util.Random` would close it, and a session that adds it owes the
bracket, because `choose` is a determinism site.

**(c) `v2_replay.clj:80-91` IS FOUR WARNINGS IN TWO LINES**, all `java.time` on
untyped locals. The wire side, not the ground-truth side.

## 5. Judgment calls

**(a) `alter-var-root`, NOT `set!`, AND THE RULING STILL SAYS WHAT IT MEANT.** D4 says
"`development/src/user.clj` sets `*warn-on-reflection*` true". A literal `set!` there
sets nothing that outlives the file: `clojure.lang.Compiler/load` pushes a thread
binding for that var around every file it loads, this one included, and pops it on the
way out, so the flag would be true for the rest of `user.clj` and false again before
the first `require` of real code. Writing the var's ROOT is what survives. I probed
both readings in a throwaway project outside this repo before choosing —
`alter-var-root` gives `*warn-on-reflection* = true` at the script's first line and a
reflective call site warns on a subsequent `require`. Read as naming the effect rather
than the form, which is how every other ruling in this program reads. The file's own
docstring carries the reasoning so the next reader does not re-derive it.

**(b) I PUT STEP 5'S PROMPT ARCHIVE IN THE TREE DURING STEP 3, AND THE INDEX GATE
CAUGHT ME.** `ehrt.docs-tooling.state-derived-test` failed step 3's `make test` at 66
of 424 result lines: "`.agents/prompts/INDEX.md` is stale -- a file was added or
removed without regenerating." The gate was right and the failure was mine, not the
code's. I removed the archive, re-ran the gate on step 3's own scope alone, and put
the archive back for step 5 where it belongs. Recorded rather than quietly re-run,
because the ONE-GATE-PER-STEP discipline is what made it visible: a step-5 artifact
sitting in a step-3 gate is exactly the confusion that discipline exists to prevent.

**(c) THE STEP-2 COMMIT WAS AMENDED TWICE, MESSAGE ONLY, AND THE SECOND AMEND WAS TO
FIX A CITATION THE FIRST ONE INVALIDATED.** `bin/ground-truth-bracket` needs a
committed ref, so the bracket ran after the commit and its verdict was amended in
(`rulings.md#R-amend-unpushed-message-only`, unpushed, message only). But the amend
rotates the sha, so the message then cited `fc0f8bce`, a commit no longer on the
branch. The fix was to anchor the claim to the TREE, `ec31eec1`, which the amend does
not touch, and to say plainly which sha the bracket ran under and why that sha is
gone. A message that cites its own commit's sha cannot survive its own amendment.

**(d) NO ROADMAP ROW MOVED, DELIBERATELY.** This session is not a site and closes
nothing on `roadmap.md#performance-residual-sites`; under
`rulings.md#R-register-hygiene-at-close` a session updates the rows it actually
changed and nothing else, and under the de-scaffold ruling a finding is a line in a
record, not a row. Findings (a) and (b) above are therefore here and not there.

## 6. Suite figures

`make test` at the step-2 tree: 424 `Test results:` lines, **28,057 passes / 0
failures / 0 errors**, 22 min 18 s, `bin/verify-nist-lock` OK on all six
hit-nexus-sourced coordinates. Identical in all four figures to site 6's close and to
site 5's — naming 59 methods adds no assertion and removes none.

`make test` at the step-3 tree: 424 `Test results:` lines, **28,057 passes / 0
failures / 0 errors**, 21 min 46 s — unmoved from the step-2 tree in all four
figures, as a dev-only file that no test path loads must be.

`clojure -M:poly check` OK at both.

## 7. Background processes

Five background jobs, all terminated, none left running: two
`bin/ground-truth-bracket` runs (the instrument's own zero at `836c764b`, and
`836c764b` against the step-2 tree) and three `make test` runs (step 2; step 3's first
attempt, which the index gate failed; step 3's rerun). Each wrote its own
`EXIT=` sentinel line and each was read from that line rather than from a
notification, per R-sentinel — which earned its keep here: the harness reported step
3's FAILED run as "exit code 0", that being the wrapper's code and not `make`'s, and
the sentinel said `EXIT=2`. The close-out CI wait is one poller that notifies once.

## 8. HEAD landed

`87a9b502` is the last payload commit; this record and its prompt archive land on
top of it, and a close-marker commit follows once CI is verified green. Baseline for
every bracket in this session was `836c764b`. The two payload commits went out in
TWO pushes rather than one, because step 2's gate and step 3's gate are separate
suite runs and R30 commits and pushes at each checkpoint — so GitHub Actions ran
twice, and section 9 verifies both.

## 9. CI

Verified with `gh run view` rather than assumed:

| commit | run | conclusion |
|---|---|---|
| `36af9103` (step 2) | 34126516658 | **success** |
| `87a9b502` (step 3) | RUN3 | **CONC3** |
| `TIPSHA` (this record) | RUNTIP | **CONCTIP** |

