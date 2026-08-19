## ADR-0155 — review-4 fix 1/5: the docsgen population is closed as a class, and the harness stops reporting successes it did not have

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-19.

### Context

ADR-0154 landed review 4's register and plan and **fixed nothing**: 72
rows, 10 rulings owed, 8 proposed fix sessions. The author ruled on
2026-08-18 — *"Q1 accept all recommendations. Q2 that order ok. Q3 pair
small ones."* — which makes every R4-Q ruling the plan's RECOMMENDED
option, fixes the session order at **G A E B C D F H**, and pairs them.
This is fix 1 of 5, the pair **G + A**.

The two halves share one shape, and it is the shape review 4 named as
its cross-dimension pattern: **a gate whose population is narrower than
the class it is read as enforcing.**

- **G** closes *populations*. A prerequisite dropped from `docsgen:`, or
  a path dropped from CI's diff list, left every gate green (L3-1,
  sub-agent-witnessed, mechanism coordinator-re-derived). The freeze
  that keeps the repo's only schema-change gate non-vacuous was enforced
  by a sentence in a header (L3-2).
- **A** closes *exit propagation*. A tracked skill taught an idiom whose
  block exits 0 (L2-2). `bin/preflight` printed `OK:` after a failed
  `gh` query and exited 0 unconditionally (L2-3/L2-4). A Makefile loop
  swallowed a converter failure (L2-5). Comments said "tees" where the
  code redirects (L2-10). `post-push-verify` folded `gh`'s stderr into a
  status field (L2-6).

### Step 0 — baseline, and five measurements taken BEFORE editing

`bin/preflight` (the COMMITTED one, at `7d998f0`): five green CI runs on
`main`, edit root not under `/mnt/`, tree clean, HEAD matches
`origin/main`, last `stable-*` tag `stable-20260818-repo-review-4` at
`0a07195`, HEAD not tagged (disclosed; no tag owed at Step 0). Exit 0.

Baseline `make test`, unpiped, `MAKE_EXIT` captured: **MAKE_EXIT=0, 348
zero-failure blocks / 3,960 tests / 17,758 assertions**, `grep -cE
'^(FAIL|ERROR) in'` = 0. Reconciles **exactly** with ADR-0154.
`clojure -M:poly check` OK (it is the first command of the target).

**(a) docsgen's actual write set, from ONE run in a scratch worktree at
`7d998f0`.** 52 tracked files written; `git status --porcelain`
afterwards empty but for the marker, so the committed tree was already
byte-fresh. Reconciled BOTH ways against the 19 diff-list paths:

| direction | result |
|---|---|
| written, not covered by the list | **ZERO** |
| list entries covering nothing written | **ONE** — `components/sim/docs/sim-theory-diagram.md` |

The one entry is **neither** of the two readings the prompt offered
(stale list vs. a generator that silently stopped). `Makefile:146` copies
that file only `cmp -s ... || cp`, so an already-fresh file keeps its
mtime and never enters an mtime-derived write set. The recipe still
NAMES it, as the `cp` destination, so the recipe-derived gate covers it.
Per-entry coverage sums to exactly 52 (22 use-case pages, 14 under
`demos/traces/`, 16 singletons).

**Consequence, disclosed because it inverts the prompt's expectation:
the diff list needed NO changes.** No path added, none removed. The
population was already correct; what was missing was anything that kept
it so. That is exactly the distinction ADR-0154's L3-15 drew — "the
current contents are right" is not "nothing keeps them right" — and it
is why this session's deliverable is a gate rather than an edit.

**(b) the five palgebra equations files vs three `.mermaid`.**
`lemon-pie` and `decision-monad` are the two unrendered. Read in full:
neither header claims a rendered output, and the `palgebra-examples`
recipe comment already said why — they "ship as equation sources only
(they are the vendoring surface, not the rendered-example surface)".
So the omission is INTENTIONAL, and per Step 1(v) the gate asserts a
DECLARED exception list rather than a blanket pairing.

**(c) `docs/use-cases/*.md` vs case ids in `use-cases.edn`: 22 == 22.**
Prediction confirmed; no orphan today, so L3-9 was latent rather than
live. (The first measurement said 21, from a bad extraction regex: the
first case opens `[{:id`, bracket before brace. Corrected before use.)

**(d) the "tees" comment sites: 6 sites in 4 files**, exactly as the
plan said — `demo-exerciser-ed-tuesday:39,47`,
`demo-exerciser-clinic-decade:47`, `readme-what-you-get:38`,
`usecase-custom-emitter:35,52`. Three further `tee` hits are correctly
NOT touched and are named here so the census is closed rather than
sampled: `fence-census:63` (the literal token in a command allowlist),
`regen-traces:87` ("guarantee"), `verify-nist-lock:2` ("teeth").

**(e) `bin/preflight` with `gh` deliberately failing — the red witness,
captured verbatim from the COMMITTED script:**

```
-- 1. Last five CI runs on main --
FAIL: gh run list failed:
HTTP 401: Bad credentials (https://api.github.com/graphql)
OK: last five runs all green (or none found)
...
== bin/preflight complete ==
PREFLIGHT_EXIT=0
```

A failed CI query rendering as a green CI report, in the script whose
Step-0 job is to establish CI colour, exiting 0.

### What landed

**G (R4-Q10 (d)) — one closure gate over the whole population**, in a
new namespace `ehrt.docs-tooling.docsgen-closure-test`, replacing the
two per-artifact assertions the class had accumulated across ADR-0136,
ADR-0149 and ADR-0152 (each of which closed it for exactly one more
artifact). It derives each leaf's write set from what that leaf's own
recipe NAMES — `-o` flags, `:out`/`:index-out`/`:pages-dir` arguments,
`cp` destinations, and the closing `@echo "Regenerated ..."` — and
asserts set equality with CI's diff list in BOTH directions, plus a
per-leaf non-emptiness assertion so an extractor that quietly stopped
matching cannot make the claim vacuous (`R-empty-population-is-red`).

What it trusts is stated in its own docstring: a recipe that wrote a
file it never names would still escape. Option (a) — enumerating by
RUNNING docsgen — is the stronger property and belongs at integration
tier; this session ran it ONCE by hand, above, and reconciled.

The gate immediately found something the measurement had not:
`state-derived:` writes three files and its recipe named one, so both
record `INDEX.md` files sat on the diff list with no leaf claiming
them. The echo now names all three, which is load-bearing rather than
decorative now that something reads it.

Also: `state-derived: pipeline` declared (L3-4 — the generated →
generated edge that existed only as prerequisite ORDER, which `make -j8`
duly ignored, twice); `# EXAMPLES-WITHOUT-MERMAID: lemon-pie
decision-monad` declared in the recipe comment (L3-10);
`write-use-cases!` prunes (L3-9); `event-schema-freeze` asserted off
`docsgen`'s transitive prerequisites (L3-2).

**A (R4-Q2 (c), R4-Q3 (a)) — exit truthfulness**, in a new namespace
`ehrt.docs-tooling.exit-truthfulness-test`:

- `extraction-stage/SKILL.md:95` + mirror now teach `> file 2>&1;
  EXITCODE=$?; ...; exit "$EXITCODE"`.
- The MAKE_EXIT law gains one clause on **all four** surfaces
  (`build-session/SKILL.md`, its `HISTORY.md`, `.agents/state.md`,
  `rulings.md#R-full-suite-before-push`), per `R-law-surface-propagation`:
  *a wrapper that captures `MAKE_EXIT` ENDS with `exit "$MAKE_EXIT"`.*
- `bin/preflight`: `UNKNOWN:` on a query that could not measure, never
  falling through to `OK:`; non-zero exit on any
  `FINDING:`/`FAIL:`/`UNKNOWN:`.
- `bin/post-push-verify` check 3: `gh`'s stderr goes to its own file and
  its exit status is tested; a non-zero `gh` renders `UNKNOWN:`. The
  script still exits 0 — AR-CI-4 is untouched, exactly as R4-Q3 (a) says.
- `Makefile:88-92`: BOTH `rm -rf target/use-cases` before the `mkdir -p`
  AND `|| exit 1` inside the loop (the prompt preferred both; they close
  different halves — the stale-`.mermaid` reuse and the swallowed
  failure).
- The six "tees" comments now say the wrapper CAPTURES BY REDIRECT and
  replays with `cat`, "never `tee`, which returns its OWN exit status
  (ADR-0152)".

### `bin/preflight`, before and after

| | before | after |
|---|---|---|
| failed `gh` query | `OK: last five runs all green (or none found)` | `UNKNOWN: CI status could not be determined -- the query above FAILED...` |
| exit code, that case | **0** | **1** |
| exit code, any FINDING/FAIL | **0** | **1** |
| `--help` | 0 | 0 (unchanged) |

**Caller census, because a fail-closed change is only safe if nothing
depended on the old contract:** `git grep bin/preflight` outside
records/ADRs returns prose only — `build-session/SKILL.md` (×2),
its `HISTORY.md`, `.agents/state.md` (×2), and a comment in
`post-push-verify`. **No Makefile target, no script, and
`bin/close-scaffold` in particular, invokes it.** Nothing mechanical
relied on exit 0. The build-session skill now states both halves: the
output is the artifact you disclose AND the exit code is load-bearing.

**One residue, deliberate:** `SKIP: gh not on PATH` stays exit-0. `gh`'s
absence is a property of the machine rather than of CI, and making it
fatal would leave preflight permanently red on any checkout without
`gh`. It prints no `OK:` either, so it cannot be misread as green.

### Every planted red, witnessed and withdrawn

Six plants. All were run against the GREEN baseline rather than inside
the red-first commit — **a disclosed reordering of Step 1**, because a
plant witnessed while four other assertions are already failing is not
an unambiguous witness of the planted claim.

| # | plant | red witnessed | withdrawn |
|---|---|---|---|
| 1 | drop `operators-doc` from `docsgen:` | reverse closure, naming `docs/operators.md` | byte-identical restore |
| 2 | drop `docs/operators.md` from the diff list | forward closure, same file | byte-identical restore |
| 3 | put `event-schema-freeze` on `docsgen:` | **TWO** gates: the freeze claim AND the leaf-declares claim (that target echoes "Froze", so declares no outputs) | byte-identical restore |
| 4 | plant `docs/use-cases/zz-planted-orphan.md` | page-set closure | file removed, 22 pages |
| 5 | restore the pre-fix taught idiom | lint names both mirrors | byte-identical restore |
| 6 | strip the `ANTI-PATTERN` marker, same bytes | lint names both mirrors | byte-identical restore |

**Assertion: none remains.** `git status --porcelain` carries only this
session's intended edits, and a tree-wide grep for the plant markers
returns nothing.

### Three things this session found rather than inherited

**1. `io-vocabulary-lint-test` caught the prune's first draft.** The
`write-use-cases!` prune used a bare `.listFiles`, and the full suite
went red (MAKE_EXIT=2) naming it. It now routes through
`ehrt.kernel.interface/list-files`, the same fix that namespace's own
docstring records catching in `docsgen/parse-adr-dir`'s first draft. The
result-or-loud distinction is load-bearing here in the OPPOSITE
direction to usual: a nil read as "empty directory" would prune nothing
and report success — reintroducing, inside the fix, the orphan class the
fix exists to close.

**2. The taught-idiom lint caught this ADR's own sibling text.** The
new HISTORY.md paragraph quotes ADR-0152's masking wrapper verbatim as a
NEGATIVE example, and the lint — correctly — could not tell "here is
what to do" from "here is what burned us". Rather than weaken the rule
or delete the example, the exemption is **declared**: a snippet whose
introducing prose carries the token `ANTI-PATTERN` is exempt, which
makes every negative example in the skill tree greppable. Plant 6 proves
the marker, not the content, is what exempts. This is the same move made
one step earlier for `EXAMPLES-WITHOUT-MERMAID`, and it is the session's
own small generalisation: an intentional exception that nothing declares
is indistinguishable from drift.

**3. `state_residue_test` fired on the law clause itself.**
`.agents/state.md` is capped at 120 lines and the new MAKE_EXIT clause
took it to 124. The gate's own message names the only two legal moves —
*"move history verbatim to `.agents/plans/state-history-2026-08.md` and
any countable claim into the generated `.agents/state-derived.md`.
Raising this cap is the move this arc exists to make unavailable."* So
the clause was folded INTO the existing paragraph and the dated anecdote
that paragraph carried ("It has paid off twice, most recently at this
session's own Step 0...") was retired to `state-history-2026-08.md`
verbatim, under its own dated heading. Net: 119 lines, the rule
strengthened, the history preserved, the cap untouched. Both this and
finding 1 were caught by a full unpiped run reporting **MAKE_EXIT=2** —
which is the law this session lands, paying for itself twice inside the
session that lands it.

### Deviations

1. **Plant-and-withdraw moved from Step 1 to after Step 2's green**, as
   above.
2. **No test asserts `bin/preflight` exits 0 in the ambient checkout**,
   which Step 3(ii) asked for. Checks 3-5 measure the AMBIENT
   environment — tree cleanliness, HEAD-vs-remote, reachability of
   origin — so that assertion would assert a property of the machine
   rather than of the script, and would go red on every `pull_request`
   run, whose checkout is a merge ref that cannot equal `origin/main`.
   Substituted: the CI assertions read only the `-- 1. --` section; the
   ambient run asserts `OK:` lines still print; and `--help` is the
   exit-0-is-still-reachable witness, being the one path whose outcome
   does not depend on the checkout.
3. **`post-push-verify`'s claim is BEHAVIORAL, not the text assertion
   first planned.** Checks 1-2 are fail-closed and run ahead of check 3,
   which appeared to make it unreachable in the per-push lane — until
   `post-push-verify-range-test` turned out to have solved exactly this
   for review 3's D1-6: a bare origin reached by path, an ASCII-only
   pushed history, the real script copied into the fixture's own `bin/`.
   Reusing that shape reproduces L2-6 verbatim and offline.
4. **`R-full-suite-before-push` was compacted** to gain its new clause
   inside the register's own 3-line row cap; the dropped words were the
   ADR-0149 f.3 back-reference. The rule is unchanged and strictly
   stronger.
5. **Three Makefile-recipe echo/text changes beyond the named edges** —
   `state-derived:`'s echo (so it names all three of its outputs) and
   the `palgebra-examples` / `use-cases` comment blocks. All are inside
   targets the fence names.

### Suite, CI, reading sets

Two pushes, each preceded by a full unpiped `make test` with MAKE_EXIT
captured, per `R-full-suite-before-push` and `R-red-pushed-with-green`
(each red commit pushed with its green successor, never alone):

| | MAKE_EXIT | blocks | tests | assertions |
|---|---|---|---|---|
| Step 0 baseline (= ADR-0154) | 0 | 348 | 3,960 | 17,758 |
| push 1, `7d998f0..bf0b381` | 0 | 350 | 3,972 | 17,828 |
| push 2, `bf0b381..f704c91` | 0 | 352 | 3,990 | 17,876 |

Every delta accounted for, per namespace: `+2` blocks / `+12` tests at
push 1 is `docsgen-closure-test`'s 6 deftests once per project
(conformance, ehrt-cli); its `+70` assertions are 34 x 2 plus
`io-vocabulary-lint-test` scanning one more `src` file x 2. Push 2's
`+2 / +18 / +48` is `exit-truthfulness-test`, 9 deftests and 24
assertions x 2. **No other namespace moved.** `clojure -M:poly check` OK
at every checkpoint.

CI, both verified by this session's own `gh run view`
(`R-session-verifies-ci-via-gh`): run **32250959906** at `bf0b381`
concluded **success** — the closure gate's own first live CI run,
freshness diff included — and run **32253127894** at `f704c91`
concluded **success**.

The close commit's own run is reported in the session record. Its full
`make test` before push: **MAKE_EXIT=0, 352 / 3,990 / 17,876**, 0
failures, identical to push 2's, as it should be: the close changes
records and generated docs, no test.

Reading sets, Step 0 -> close, no budget moved (`R-budget-stop`
untouched): `:onboarding` 1401 -> 1412 / 1530, `:corpus` 1801 -> 1807 /
2045, `:sim` 1247 -> 1253 / 1405, `:judge` 895 -> 901 / 1000, `:docs`
708 -> 714 / 785. The `+6` every set shares is
`build-session/SKILL.md`, the one path all five carry.

### An observation, out of fence, recorded for a later session

At this session's own first push, `post-push-verify` check 3 printed
`status= conclusion=<pending>` with an EMPTY status and no URL: the run
was not yet indexed, and `gh` returned something the `[ "$run_line" =
"null" ]` guard does not catch. Not a regression and not touched here —
the fence limits check 3 to the `UNKNOWN:` rendering — but it is the
same "no run yet" case wearing a shape the script does not recognise.

### Fences honoured

**READ-BACK — every file touched, `7d998f0..close`, against the fence
that named them. 32 files, no others:**

| fence item | files |
|---|---|
| `Makefile` (named edges only) | `Makefile` |
| docs-tooling tests | `docsgen_closure_test.clj`, `exit_truthfulness_test.clj` (new); `sim_theory_head_hop_test.clj`, `traces_fresh_test.clj` (parsers moved out, no claim changed) |
| + `usecases.clj` prune | `usecases.clj`, and `make_graph.clj` (new shared helper) |
| `bin/preflight`, `bin/post-push-verify` | both |
| the four "tees" scripts, COMMENTS ONLY | `demo-exerciser-ed-tuesday`, `demo-exerciser-clinic-decade`, `readme-what-you-get`, `usecase-custom-emitter` |
| `extraction-stage/SKILL.md` + mirror | both |
| `build-session/SKILL.md` (+HISTORY, mirror) | four files |
| registers | `state.md`, `rulings.md`, roadmap, the review-4 register and plan, `state-history-2026-08.md` |
| generated, by `make docsgen` | `state-derived.md`, `notes/ADRs.md`, both record `INDEX.md` |
| close artifacts | this ADR, the session record, the archived prompt |

**`test.yml` was in the fence and is UNCHANGED** — `git diff 7d998f0..
-- .github/workflows/test.yml` is empty. That is the diff-list delta:
**zero**, and Step 0(a) is why.

The four "tees" scripts changed by comment only — no executable line in
any of them differs, which is the fence A stated as "no change to what
any gate COMMAND does".

No `src` outside docs-tooling except `usecases.clj`, which the fence
names. No `digest.clj` or oracle touch.

**On the oracle, stated precisely, because `R-oracle-script-contract`
forbids dressing anything else as an oracle claim: `bin/regression-oracle`
was NOT run this session.** What is asserted is the FENCE, not a
verdict — the only `src` this session changed is
`docs-tooling/{usecases,make_graph}.clj`, neither of which is on any
path the oracle digests, and no sim, emit, judge, kernel or model file
was touched at all. That is a structural argument for the verdict being
unchanged, and it is deliberately not reported as an oracle result. Fix
session E owns the oracle.

No test deletions — the three
duplicated parsers moved to `ehrt.docs-tooling.make-graph` and both
original namespaces now require it, 14 tests / 57 assertions still
green. Every planted red withdrawn. Exit codes unpiped throughout, and
**this session's own wrappers end with `exit "$MAKE_EXIT"`** — the law
it lands, applied to itself.

### One line

The class ADR-0136 opened and that ADR-0149 and ADR-0152 each closed for
exactly one more artifact is closed **as a class**, by a gate that
derives the population from the recipes rather than from a hand list —
and the measurement that preceded it found the diff list already
correct, which is the whole point: the deliverable is the thing that
keeps it correct. Alongside it, the harness stops claiming successes it
did not have, in the one script every session is told to run first.
