# 2026-09-06 — ADR-0180 site 2: `select-person` rides a rank/select sweep

Site 2 of the generate-quadratic program, `roadmap.md#performance-residual-sites`
PRIORITY 1, rowed separately as `roadmap.md#select-person-arrival-quadratic`
PRIORITY 2 — which this session closes. Ceremony mode: R30 (commit and push at
each checkpoint), taken from the prompt. Prompt archived at
[`../prompts/2026-09-06-adr-0180-site-2-select-person.md`](../prompts/2026-09-06-adr-0180-site-2-select-person.md).
Reasoning-of-record: `notes/adr/0180-indexes-ride-the-fold.md`, section
`2. run/select-person`. No ADR was written and none was owed — this session
ENACTS a charter that was already ruled.

Rulings in force: **R-fold-carrier**, **R-equivalence**, **R-order**,
**R-positional**, **R-move-not-improve**, **R-test-fold**, **R-pins**,
**R-edit**, **R-cap**.

## 0. Preflight

`bin/preflight` ran first, exit 0, no findings: last five CI runs on `main` all
green, edit root `/home/mg/src/ehr-testing-tools` (not `/mnt/`), `core.fileMode`
true, `core.ignorecase` unset, working tree clean including untracked files,
local HEAD `d9088a91` equal to `origin/main`. Session start and every bracket
baseline is that sha.

The instrument's own zero ran before anything else: `bin/ground-truth-bracket
d9088a91 d9088a91` reported IDENTICAL on all 38 digested roots (3 skipped, no
`:ground-truth` key).

Both baseline cells reproduced the digests site 1 recorded, which is what makes
this session's before-column the same corpus as that one's after-column:
`a7500-persons` sha256 `3018299a…0d3bd3` at 195.48 s, `a2500-nopersons`
`c22d6573…a55208` at 29.74 s.

## 1. What landed

**`1e15f2d3` — `engine: rank/select sweep with its from-scratch law`.**
The carrier and its gate; the SITE UNTOUCHED. `run.clj` gains five forms —
`fenwick-ones` and `fenwick-clear!` (private), `alive-sweep`, `sweep-advance!`
and `sweep-nth` (public, so the law can drive them). `alive-sweep` builds a
Fenwick tree over population indices, all-ones at t0, beside the run's deaths
sorted by instant; `sweep-advance!` moves a cursor as `t` does; `sweep-nth` is
the descent that answers the `k`th survivor in the population's own index
order. `ehrt.sim-engine.alive-sweep-test` is new and carries the law.
`persons_test` gains the population-is-fixed assertion.

**`03db90a4` — `engine: select-person reads the rank/select sweep`.**
The repoint. `select-person` takes a `sweep` where it took `population` and
`alive`; the draw is still taken unconditionally at the same point in the
`:world` sequence, and the `min` clamp is the shipped one character for
character. `prelude` builds the sweep once inside the `if persons` arm.

**`59994ef5` — `plans: site 2 measured`.** `measurements.md` gains a dated
site-2 section; the P1 row's site-2 clause gains the figures; the P2 row
rotates to a CLOSED line; the two JFR reports land in `raw/`.

## 2. The measurement

Both cells reproduced their pre-change log BYTE-FOR-BYTE — the same two digests
as in section 0 — and `bin/ground-truth-bracket` reported IDENTICAL on all 38
digested roots at the clean tip, at `1e15f2d3` and at `03db90a4`.

| cell | before | after | delta | corrected |
|---|---|---|---|---|
| `a7500-persons` | 195.48 s | **158.90 s** | -36.58 s, -18.7% | **-19.5%** |
| `a2500-nopersons` | 29.74 s | 29.45 s | -0.29 s, -1.0% | -1.3% |

JFR at 7,500 arrivals, inclusive share, `--stack-depth 2048`:
`select-person` **19.17% -> 0.01%** — one sample of 10,914, and gone from the
innermost-project-frame table where it had been the top row at 16.14%. That
table's new top row is `occupancy-board`'s own inner function at 11.87%, which
is site 3. `decide` 47.22% -> 59.44%, `occupancy-board` 12.51% -> 17.87%,
`last-uncancelled-index` 14.68% -> 17.63%, `apply-events` 9.76% -> 13.06%: all
rising on a smaller denominator, none of them doing more work than before.

Two things the `measurements.md` section says out loud:

* **`a2500-nopersons` is the negative control and its flatness IS the result.**
  That cell has no `:persons` key, so `select-person` is never called and no
  sweep is ever built. Site 1's index sat on the shared path and moved the same
  cell by -13.7%; this one may not, and did not. The contrast between the two
  rows is what says each index is where its charter put it.
* **The 3% spread on identical code is the instrument's own noise.** This
  session's `a7500-persons` baseline reads 195.48 s at the very commit where
  the site-1 section measured 189.79 s. Only before-against-after within one
  session carries anything.

**Nothing absorbed the 19 points the way `apply-events` absorbed site 1's.**
The sweep is O(n) once at `prelude` plus O(log n) per arrival, and at this cell
that is beneath the sampler's floor — there is no row for it to appear in.

Peak RSS fell at the large cell (2,389 -> 2,034 MB) and ROSE at the small one
(928 -> 1,091 MB) while executing none of this code. Both readings are
recorded; neither is claimed. A single-JVM RSS figure against a 3.88 GB heap
cannot carry a claim of that size, and the control cell moving the wrong way is
the demonstration rather than an anomaly.

## 3. The law, and that it is not vacuous

`ehrt.sim-engine.alive-sweep-test` keeps `select-person`'s `filterv` verbatim as
`naive-candidates` and the rest of its body as `naive-select`
(R-move-not-improve). A pinned-seed property (`{:num-tests 300 :seed 20260906}`)
asserts at EVERY arrival that the two candidate VECTORS are `=` — same
elements, same order, same count — and that the selected id is the same id.
Both sides are driven by scripted `Random`s replaying one uniform sequence, so
the comparison runs through the draw rather than only beside it.

The generator draws deaths and arrivals from the same small range, which makes
ties in death instant and deaths landing exactly ON an arrival instant common
rather than rare; the draws include `Math/nextDown 1.0`, the largest uniform
`.nextDouble` can return.

`the-corpus-actually-reaches-every-shape` is the non-vacuity gate over two
hand-built pinned cases: the corpus reaches an arrival where the filter removed
SOME people, one where it removed EVERYONE (the nil binding), an arrival AT a
death instant, and a selection landing on the LAST candidate. Three further
tests pin the tie case, the empty pool (`select-person`'s fixed-consumption law,
reaching the structure), and the `nil?` arm that makes a person absent from
`:alive` immortal.

**The population-is-fixed fact is asserted, not assumed.** ADR-0180 says a
session here should assert it; `persons_test`'s new
`the-arrival-pool-is-the-input-population-and-nothing-the-run-mints-test` drives
the delivery fixture — the one thing in the tree that mints a person mid-run —
and asserts both that the newborn exists and that every arrival still bound
inside the INPUT population.

## 4. Judgment calls

**(a) Step 2 landed NO commit, because the sweep found nothing to fix.**
R-test-fold is standing for sites 2-4, and the sweep ran: `persons_test.clj`
hand-rolls no fold at all — it drives `run/run` and `run/person-plan`
end to end — and `engine_test.clj`'s `fold-events` already routes through
`fold/apply-events`, site 1 having rewritten it. The one `engine_test.clj`
residue, `patient-state-is-a-fold-of-the-log`'s `reductions evolve/evolve` at
`:91`, was examined and deliberately LEFT: it is the from-scratch definition of
the ADR-0008 event-sourcing property, and routing it through the choke point
would compare `apply-events` with itself. No assertion was weakened and no test
was found red or vacuous, so there was nothing to commit. Named here rather
than silently skipped.

**(b) The before-profile is site 1's own after-profile, reused rather than
re-recorded.** `raw/a7500-persons-site1.gen.profile.md` was recorded today, on
this machine, at `d9088a91`'s code — the exact state this session baselines
against — so re-recording it would have produced a second sample of the same
population at the cost of five minutes. The section names the file rather than
leaving a reader to infer it.

**(c) The sweep's mutable state is a `long[]`, and that is forced.**
`ehrt.docs-tooling.sim-purity-lint-test` forbids `atom`/`ref`/`agent`/
`volatile!`/`set-validator!` anywhere in the seven sim-family bricks' `src`,
with two named exceptions this is not one of. An array is outside that census
and inside R-positional's own carve-out ("a mutable array local to prelude is
acceptable since nothing escapes"). The docstring says so at the site, so a
later reader does not read it as an oversight.

**(d) Two sweep vars are PUBLIC in `run.clj` and `select-person` stays
private.** The law has to drive the replacement directly, which needs
`alive-sweep`/`sweep-advance!`/`sweep-nth` reachable; the site itself is
reached through `#'run/select-person`, the same idiom `bed_cycle_test` uses for
`#'check/bed-transitions`. `run.clj` is an implementation namespace behind
`ehrt.sim-engine.interface`, so nothing in the frozen-surface gate moves.

**(e) `ADR-0180` itself was NOT edited**, for the reason site 1 gave: its
sequencing list is a statement about when the charter was written, not live
status, and the roadmap is the register that carries what has landed.

## 5. Findings

**The `min` clamp is unreachable, and it moved verbatim anyway.** ADR-0180 says
the clamp "at a draw very close to 1.0 is what keeps the index in range". In
IEEE-754 double arithmetic it never fires: for `draw` the largest value
`.nextDouble` can return (`Math/nextDown 1.0`), `(long (* draw n))` was `< n`
for every `n` in 1..200,000 — zero hits, probed this session. The clamp is
therefore defensive rather than load-bearing. Nothing was changed on the
strength of that: R-move-not-improve says the body moves character for
character, and it did. Recorded because the ADR's own sentence overstates it
and a later reader should know which claim the code rests on.

**A new test FILE costs a `state-derived` regeneration**, and the first `make
test` of this session went red on exactly one assertion —
`state-derived-md-matches-a-fresh-render-test`, test namespaces 223 -> 224 —
with the whole rest of the suite green. `make state-derived` fixed it and the
suite was re-run in full rather than the one gate re-run alone.

**DISCLOSED, NOT FIXED, unchanged from site 1.** `components/sim-emit-hl7`'s
test tree still carries the hand-rolled `fold-events`/`admit` pair
(`emit_hl7_test.clj:163-176`). It is the same class R-test-fold names, it is in
another brick behind another gate, and this session's prompt fences step 2 to
`persons_test.clj` and `engine_test.clj`. It asks no question site 2 touches.

## 6. Suite figures

| point | test-result lines | passes | failures | errors |
|---|---|---|---|---|
| `1e15f2d3` | 418 | 27,797 | 0 | 0 |
| `03db90a4` | 418 | 27,797 | 0 | 0 |
| `59994ef5` | 418 | 27,797 | 0 | 0 |

The baseline at `d9088a91` was 416 lines and 27,753 passes (site 1's record,
section 6). The +2 lines are the one new namespace counted once per project it
belongs to; the +44 passes are its own assertions plus `persons_test`'s new
test. Nothing moved at the repoint or the measurement, which is what an
output-identical refactor should look like from here.

`:onboarding` reading-set headroom went 26 -> 35 against an unchanged 1,530
budget: the roadmap edit PAID nine lines by rotating a fifteen-line OPEN row
into a 475-character CLOSED one (R-cap is 480).

## 7. Background processes

Ten harness-tracked background jobs, each of which exited on its own with its
exit code recorded: three `bin/ground-truth-bracket` runs (the instrument's own
zero at the clean tip, then one per CODE commit -- the docs commit owed none),
two timed cell runs (before, after), four `make test` runs (the first red on the
state-derived gate alone, three green), and one JFR profile run. One further
background entry is a `sleep` waiter the harness moved off the foreground when
it outran its timeout; it exited on its own too. Everything else --
`bin/preflight`, two `make state-derived` runs, two `clojure -M:poly test
brick:sim-engine` runs, one reflection-warning probe and one clamp-reachability
probe -- ran in the foreground. None was left running at close.

## 8. HEAD landed

`59994ef5` is the last payload commit; this record and its prompt archive land
on top of it, and a close-marker commit follows once CI is verified green with
`gh run view`. Baseline for every bracket in this session was `d9088a91`.

## 9. CI

All five commits went out in ONE push, so GitHub Actions ran once, at the tip.
Verified with `gh run view` rather than assumed:

| commit | run | conclusion |
|---|---|---|
| `8cc2d187` (tip, covering `1e15f2d3`, `03db90a4`, `59994ef5`) | 34045391743 | **success** |

`bin/post-push-verify` ran immediately after the push: remote tip matches HEAD,
every commit message in `d9088a91..8cc2d187` is pure ASCII, and the CI run was
reported once rather than awaited (AR-CI-4) -- this section is where it was
awaited.

CI green at the tip is the marker this arc closed; no tag was paid (the
de-scaffold ruling, 2026-08-25). Site 3, `sim-model/occupancy-board`, is next
under R-order, and this session's own after-profile is its baseline: it is now
the top row of the innermost-project-frame table at 11.87%.
