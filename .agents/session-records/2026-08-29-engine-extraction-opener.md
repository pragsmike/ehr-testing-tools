# Engine namespace extraction, opener: the census and the streams cluster

Session record, 2026-08-29. HEAD at start `517a96d`, confirmed against
`origin/main` by `bin/preflight` (exit 0, no findings; the one
DISCLOSED line was "HEAD is not currently tagged stable-*", which the
de-scaffold ruling made expected). Ceremony R30, taken from the prompt.
Program: `roadmap.md#engine-namespace-extraction-and-apply-unification`
(P5), author rulings C1(a) and C2(b).

Three commits: the census, the extraction, and this record.

## 0. Step 1 -- the tip, and what the channel got right

`517a96d`, and the working clone was already there and clean, so no
fresh clone was taken; every line number in the census was re-derived
from that tree with a form-span script rather than transcribed. The P5
row's own sizes still hold at this sha: `engine.clj` 4,884 lines,
`emit_hl7.clj` 2,498. What the row calls "4,884 at `da21a28`" is
unchanged here.

## 1. Step 2 -- the census (`179076b`)

`.agents/plans/engine-extraction-census.md`, 573 lines. Its method is
mechanical and repeatable, which is the point: a script parses every
line beginning `(` into a `(name, start, end)` span, asserts the
partition covers all 157 forms of `engine.clj` and all 86 of
`emit_hl7.clj` exactly once, then scans each form's body -- line
comments and string literals stripped -- for whole-symbol references to
every other top-level name, and reports the pairs that cross a cluster
boundary.

**Gate.** 157 + 86 forms, each in exactly one cluster; the 32 `decide`
methods and 27 `evolve` methods each named in their own cluster's list.
`make test` exit 0 at that tree: 4,751 tests, 24,109 assertions, 408
namespaces, 15m33s wall.

**The census's first run was RED**, disclosed here rather than
smoothed over: `state-derived-md-matches-a-fresh-render-test`. The
`.agents/plans/README.md` index line the census owes moved the
`:onboarding` reading set from 1,437 to 1,438 lines, and
`.agents/state-derived.md` is generated from exactly that. `make
state-derived` produced a one-line diff and nothing else.

### Corrections to the channel's cluster read

The prompt asked for these, one sentence each.

1. **The apply paths are three, not three event sources.** The P5 row
   phrases unification as "decide-drawn / module-compiled /
   churn-injected events through one apply choke point"; against the
   live tree, module-compiled and churn-injected work enters as STEPS,
   and `decide` at `engine.clj:4747` is the only expression in the tree
   that produces a ground-truth event, so what wants unifying is the
   three places an event is APPLIED.
2. **`prelude` has no pre-loop apply path.** The prompt named one; it
   computes queue entries and pre-loop facts (`seeded-steps` at
   `:4209`, consumed at `:4571-4574`) and folds nothing, so the census
   lists it under step injection rather than under apply.
3. **`replay` is not the in-loop fold minus nothing.** It omits six of
   the ten things the in-loop fold does -- the encounter stamp, the
   warm-up mark, the bed index, and all three log indexes -- which is
   the concrete content of what "one apply path" would have to
   reconcile, and is why `check.clj` writes its own bed arithmetic
   (`check.clj:527`).
4. **A third apply exists and is the cheapest to delete:**
   `reinstated-state`'s `(nth (replay ground-truth) idx)` fallback at
   `engine.clj:3271`, reached only by a hand-built world with no
   `:reinstate-index`.
5. **`streams` is a leaf, provably.** It has no outgoing seam edge at
   all, which is what makes ruling C2(b)'s choice of it as the first
   extraction verifiable rather than asserted.
6. **The engine graph has exactly one cycle, and one breaker.**
   `decide` -> `log-index` -> `fold` -> `evolve` -> `decide` closes
   only through the private `observation-value-fields`
   (`engine.clj:2360-2376`), which both `decide :observation` and
   `evolve :observation`/`:diagnostic-report` call; moving that one
   form down makes the rest a DAG.
7. **`emit_hl7.clj` needs no breaker** -- its eight clusters are
   already acyclic -- but two assignments are judgement calls and the
   census says so: `transmit-seconds` reads as a planner's helper and
   is placed in `hl7-time` because it is pure `t`-plus-offset
   arithmetic with ten callers among the message builders, and
   `context-for-event` is placed with the Z-segment renderers rather
   than the segment builders, which is what keeps `er7`'s only outgoing
   edge a single one.

## 2. Step 3 -- the extraction (`16fe24c`)

`ehrt.sim-engine.streams`, the name the prompt proposed and the census
agreed with. Sixteen forms, `engine.clj`'s own text: `rand-int-in`,
`uniform-choice`, `mix64`, `patient-id-for`, `encounter-id-for`,
`next-encounter-ordinal`, `appointment-id-for`,
`next-appointment-ordinal`, `minted-appointment-id-field`,
`minted-encounter-id-field`, `stream-scheme`, `stream-family-tag`,
`stream-seed`, `stream`, `newborn-id-tag`, `one-stream`.

Eleven delegating defs stay in `engine.clj`, in the order the originals
stood in. The five vars that were private stay private in effect: four
(`rand-int-in`, `uniform-choice`, and the two `minted-*-id-field`s)
become public in `streams` because a sibling has to call them, get no
delegating def, and are `streams/`-qualified at their thirteen call
sites; `stream-family-tag` stays `^:private` in `streams`, since
`stream-seed` moved with it. `interface.clj` is untouched. No test file
changed.

### The constraint that shaped the whole extraction

`engine/stream` had to stay the var `run` calls through.
`engine-test/mutating-one-patients-stream-seed-moves-only-that-patient`
(`engine_test.clj:2505`) perturbs the partition by `with-redefs` on
`ehrt.sim-engine.engine/stream`; `streams/`-qualifying `run`'s four
call sites (`:3483`, `:3488`, `:3872`, `:4356` after the move) would
have made that gate run over an unperturbed simulation. It would have
gone RED rather than silent -- the moved set would have been `#{}`
against an expected `#{3}` -- but the fix is the delegating var, and
its docstring now says so out loud. `person-simulator`'s
`consumption_test.clj:44,:142` redefine `ehrt.sim-engine.interface/
stream`, a different var, and were never at risk.

### Two comments corrected rather than moved verbatim, disclosed

"Verbatim" is this commit's whole claim, so the two exceptions are
named. `stream`'s own UNHINTED note read "the locality test's whole
mechanism is redefining this var"; after the move "this var" names the
wrong one, and it now names `engine.clj`'s delegating def explicitly.
And the `stream-scheme` delegating def carries a forwarding sentence,
because `docs/consuming-ground-truth.md`'s Determinism section names
THAT var's docstring as the authority for `:stream-scheme` -- the
warranty this session was fenced from moving stays put only if the
citation keeps resolving.

### The fix-forward, and the gate the census failed to predict

`ehrt.docs-tooling.person-simulator-charter-test/every-charter-
citation-resolves-test` went red: two rows of
`components/person-simulator/docs/limitations.md` pin snippets BY PATH
into `engine.clj`, and both snippets -- row 1's "pinned at 0 for as
long as" (from `newborn-id-tag`) and row 10's "arc 2's demographic/
life-arc layer. ZERO draw sites" (from `stream-family-tag`) -- are
inside the moved text. One defensible reading, so fix-forward with
disclosure (`rulings.md#R-stop-only-on-two-defensible-readings`), not a
STOP: both citation paths are repointed to `streams.clj`, and both
still resolve exactly once, which is what the sibling
`every-charter-citation-anchors-exactly-one-place-test` requires.

**The census did not predict this, and that is the session's own
finding about its own method.** Its section 5 listed six constraints,
all derived from `sim-engine`'s and `person-simulator`'s CODE; a
snippet pinned by path from another brick's DOC is invisible to a
call-graph census. Section 5 gains a seventh row in this commit saying
so, with the recipe: before moving a form, grep the repo for a
distinctive phrase from its docstring, not only for its name.

### Gates

* **Suite.** `make test` exit 0: 4,751 tests and 408 namespaces,
  IDENTICAL to the census commit's own run; 24,111 assertions against
  24,109. The +2 is structural, not behaviour:
  `ehrt.docs-tooling.io-vocabulary-lint-test` is a `doseq` over every
  source file with one `is` per file, it runs in two projects, and the
  tree gained one file. A per-namespace diff of the two runs shows that
  namespace and no other moved. 15m43s wall against 15m33s.
* **`bin/regression-oracle 179076b 16fe24c`** -- the script's own
  output: `IDENTICAL: every root's digest matches between 179076b and
  16fe24c`, `declared-digest-change: no (soundness: yes outside the
  leading docstring)`. No declaration was owed and none was made.
* **`bin/ground-truth-bracket 179076b 16fe24c`** -- `IDENTICAL: every
  digested root's :ground-truth matches between 179076b and 16fe24c (38
  roots)`, coverage `38 roots carry :ground-truth and are digested; 3
  skipped (no such key): appendicitis.edn, ear-infections.edn,
  sore-throat.edn`.

Both brackets IDENTICAL with no declaration is the strongest shape a
pure-refactor commit can report, and it is what ruling S1(a)'s
equivalence proof asks for in place of red-before-green.

* **CI.** `gh run view 33288633533` -- workflow `test`, head `179076bb`,
  completed, conclusion **success** (the census). `gh run view
  33289768550` -- workflow `test`, head `16fe24cd`, completed,
  conclusion **success** (the extraction).

## 3. What the next session takes

The census's dependency order, not a free choice. `state` before
`evolve`, `evolve` before `fold`, and `observation-value-fields` moved
down out of `decide` before either -- otherwise a session creates a
namespace it has to un-create. The natural next cluster is `state` (13
forms, 365 lines, leaf), then `encounters`, then `evolve`.

Application-path unification stays last, against section 4 of the
census, and it is also `roadmap.md#event-stream-mutation`'s injection
point.

## 4. Disclosures

* No fresh clone; the existing clone was at `517a96d`, clean, and
  `bin/preflight` exited 0. Every line number was still re-derived.
* The full suite was started once and killed before completion, on
  purpose: two comments in the new file said things that stopped being
  true when the file was created, and shipping them would have been the
  exact "claim true when written that nothing keeps true" pattern
  `roadmap.md`'s review-5 rows exist to name. The comments were fixed
  and the suite restarted from the top.
* `make test` runs `poly test :all skip:integration`, so the
  integration tier did not execute here; that is the standing W-1
  disclosure, not new to this session.
* `.agents/state-derived.md` records `:docs` reading set at 787 lines
  against a 785 budget -- headroom -2. It is PRE-EXISTING (identical in
  both the expected and actual halves of the first red), no reading set
  this session used, and nothing this session did moved it. Named
  rather than left for the next session to rediscover.
* `poly check` is green, and `clojure -M:dev` loads
  `ehrt.sim-engine.engine` and `ehrt.sim-engine.interface` and
  evaluates `patient-id-for`, `stream-scheme` and `stream-seed`
  through the delegating defs.
