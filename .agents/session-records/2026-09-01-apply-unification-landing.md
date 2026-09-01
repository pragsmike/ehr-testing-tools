# 2026-09-01 — application-path unification, the landing: the last two pairs, one declared omission, and the P5 close

Driving prompt: `.agents/prompts/2026-09-01-apply-unification-landing.md`.
Predecessors: `2026-09-01-apply-unification-stage-1.md`,
`-stage-2.md`. Serves `roadmap.md#engine-namespace-extraction-and-apply-
unification`, which this session CLOSES.

**THE APPLY ARC IS CLOSED.** Sites end **13 / 12 / 13** of thirteen —
thirty-eight of the thirty-nine (site × accumulator) cells at full
product, and the thirty-ninth a **measured permanent omission** rather
than an unfinished pair. Twenty-one of the twenty-two pairs the census
inventoried are enabled; the twenty-second is a statement of fact.

## 0. Preflight, and the step-1 gate

Tip confirmed at `bf4616f`, worktree clean, ext4 clone of record
(`~/src/ehr-testing-tools`). Ceremony mode R30.

**Both enabling diffs re-verified against the stage-2 record's prepared
shapes before either was applied, and both match BYTE FOR BYTE.** Each
is one set literal in `fold.clj`, re-flowed, with no call-site change:
§4b's prepared diff is `replay-projection`'s, §4c's is
`reinstated-projection`'s. Neither needed a slot — `:encounter-stamp` is
a DECORATION, a `mapv` over the batch, and takes no accumulator key.
That is the step-1 gate, recorded.

## 1. 2 × A1 `:encounter-stamp` — landed INERT, `5b13dec`

The census predicted OUTPUT-MOVING (section 3b); stage 2's measurement
refuted it; ruling A1(b) disposes it (a) **plus the gate**. Landed with
the identity gate co-landed in the SAME commit — green at birth under
S1(a), red-first not applying to a co-landed invariant of a measured
fact.

**The suite: 4,755 / 24,193 / 410 → 4,763 / 24,219 / 412, and every unit
of that is accounted for**, which the prompt asked for explicitly:

| source | per run | × 2 project runs |
|---|---|---|
| the gate's 4 deftests | 4 | **+8 tests** |
| the gate's 12 `is` forms | 12 | **+24 assertions** |
| `test_source_live_path_lint_test`'s `doseq` over test SOURCES — one assertion per file, so a new test file adds one | 1 | **+2 assertions** |
| the gate namespace itself | 1 | **+2 namespaces** |

The `+2` from the live-path lint is the only term that is not simply the
new file's own content, and it was LOCATED rather than assumed: that
gate's `doseq` at `test_source_live_path_lint_test.clj:96` scans every
test source and asserts one `is` per path.

`.agents/state-derived.md` moved one line, 220 → 221 test namespaces,
regenerated and committed with the change that moved it.

## 2. 3 × A1 `:encounter-stamp` — landed INERT, `c172684`, site 3 at FULL PRODUCT

Same mechanism, same disposition, and deliberately not split from its
twin: they are one mechanism seen from two sites, and disposing them
differently would leave the two columns disagreeing about a concern that
behaves identically at both. Site 3 reaches **13 of 13**.

The gate gained site 3's column in this commit — driving the LIVE
`log-index/reinstated-state` (handed a world with no `:reinstate-index`,
which is what sends it down the replay fallback) rather than a
projection of it.

Suite: 4,763 / 24,219 / 412 → **4,765 / 24,239 / 412**. One deftest, ten
assertions, × 2 runs; no new file, so no namespace and no state-derived
move.

### 2a. THE GATE FAILED FIRST, and the failure is kept rather than smoothed over

The site-3 deftest's first form threw an **NPE at `fold.clj`'s
decoration line** — `Numbers.lt` on a nil, from `(< (:t ev)
warm-up-seconds)`. The shared `entries-under` helper was passing
`replay`'s accumulator verbatim, and `reinstated-projection` carries
`:warm-up-mark`, which needs a window `replay`'s accumulator has no
reason to hold.

**The fix is site 3's own declared value, not an invention of the test**:
the helper now passes `:warm-up-seconds 0`, exactly what
`log-index/reinstated-state` declares and census 3d already discloses.
Its docstring says why that is inert for the site-2 calls in the same
namespace — ruling A2(b) omits `:warm-up-mark` from `replay-projection`
PERMANENTLY, so those calls never reach the line that reads it.

**This is the arc's own thesis arriving uninvited.** The pair that has
no source for its parameter broke a test written about a different pair
entirely, because the two projections are no longer the same object. It
was caught by the suite, not by reasoning about the tree.

## 3. What the co-landed gate actually pins, and why it is not ceremony

`ehrt.sim-engine.apply-restamp-identity-test`, 5 deftests / 22
assertions. Ruling A1(b)'s reason for requiring it is that **the
inertness is not a property of the concern**: it rests on two mechanisms
that live elsewhere and that a later session could change without ever
learning that these two lines depend on them. So each is pinned **by its
counterfactual**, not merely exercised:

* **The `contains?` guard.** The gate hands `stamp-encounter` a world
  whose patient holds a **different** id, asserts the already-stamped
  event comes back untouched — **and asserts that the same world WOULD
  mint that different id onto the same event unstamped**. Without the
  second assertion the first proves only that the world was empty.
  Relax the guard toward `some?` and the test fails.
* **The whole-log-as-one-batch shape.** The gate replays a log with its
  ids **STRIPPED** under the enabled projection and asserts **not one id
  is minted back**, though the same events minted three on the way in at
  site 1. Re-batch `replay` per event, or evolve the world between
  batches, and ids reappear — the test fails.

Non-vacuity is asserted before either: 7 events, 6 carrying an id, 3
encounters, three discharges at indexes 2/4/6. A gate about re-stamping
proves nothing over a log with nothing stamped.

The fixture is `encounters-test`'s, rebuilt rather than shared, for that
namespace's own stated reason — a test namespace requiring another test
namespace makes two gates one gate.

## 4. 2 × A2 `:warm-up-mark` — OMITTED, PERMANENTLY, `ec52471`

Ruling A2(b), taken as the record's option (b). The declaration lands in
FOUR places, so that no reader who finds only one of them can read the
gap as unfinished work: census section 3d's row, a new census section
**3e** carrying the measurements and the full option set,
`replay-projection`'s own docstring, and the projection test's site-2
column — which states in as many words that a later session
"completing" this column has undone a decision rather than finished the
arc, and that this is why the matrix arithmetic reads 38 of 39 and not
39.

The three reasons, the middle one measured: a log carries no warm-up
window and only a run configuration has one; a declared 0 is **measurably
lossy** (`:warm-up true` → `false` at entry 0, 2 of 9 entries, first
differing byte 425, on a windowed log); and threading the window would be
an API change at 16 live call sites so `replay` could re-derive a value
the log already carries — the vacuous shape relocated, not removed.

Site 3's twin survives the same declared 0 only because it reads a
PATIENT STATE and `evolve` never reads `:warm-up`. That licence is
exactly what site 2, which returns the entries themselves, does not have.

## 5. The P5 close, `4e4db52`

The narrative migrated to `## Done` as one entry, slug unchanged so the
live citations still resolve — the de-scaffold ruling's "each keeps one
line at the bottom" is precisely this case. The `## Next` row retired.

**P6 `[event-stream-mutation]`'s row was verified rather than assumed**,
and it still reads as the prompt described: design ADR first; mutation
moves to the ground-truth event stream with emitters inheriting it;
ADR-0166's test-side mutations promoted to a shipped operator catalog
with `check` as oracle; file-level operators only for lowering-layer
faults. It gains one sentence — the injection point now EXISTS,
`fold/apply-events`, landed by stage 1, so the row waits on nothing,
though the design ADR still comes first.

**ONE PRECISION, disclosed rather than quietly resolved.** The prompt
says "the roadmap's head becomes P6". P6 is the successor of THIS ARC,
not of the file: the `## Next` head remains **P1
`[performance-residual-sites]`**. Priorities were NOT renumbered —
removing 5 leaves 1, 3, 4, 6, 9, 10, 11, still unique and ascending,
which is all the row contract asks, and renumbering would have rewritten
six rows this session had no business touching.

**Headroom, recorded as the gate asks.** `.agents/state-derived.md`
regenerated LAST and moved exactly three lines: Next 8 → 7, Done 35 →
36, and `:onboarding` **0 → 71** (the roadmap is 71 lines shorter).
Total roadmap rows unmoved at 71 — a row changed sections rather than
vanished. `:docs` reads **−2** and is **PRE-EXISTING**, unmoved by this
session and present at `bf4616f`; it is disclosed here rather than
claimed or fixed.

## 6. Verification

* **`clojure -M:poly check`** — `OK` at every run.
* **`make test`** — EXIT 0 at each of the three code/docs states, with
  every unit of the ledger's movement explained in §1 and §2. Final
  tree: **4,765 / 24,239 / 412**, zero failures, zero errors.
* **`bin/regression-oracle`** — **IDENTICAL, 41 roots, no declaration**,
  at `bf4616f..5b13dec` and at `5b13dec..c172684`. Per-commit, not
  per-span, since these two commits are the arc's load-bearing ones.
* **`bin/ground-truth-bracket`** — **IDENTICAL, 38 roots, no
  declaration**, `bf4616f..c172684` (3 roots skipped, no
  `:ground-truth` key).
* **AND BOTH VERDICTS ARE VACUOUS FOR THESE TWO PAIRS, which is why
  neither is offered as the evidence.** `ehrt.oracle.digest`'s own
  coverage note lists `engine/replay` among the paths NO root reaches,
  and every root's digest is `{:ground-truth :hl7}`. The dispositions
  rest on stage 2's DIRECT measurement (record §4b/§4c) and on the
  co-landed gate. The instruments were run because the prompt's
  stop-and-show fence needed them to be, and an IDENTICAL here means
  only that nothing ELSE moved — which is a real thing to know and not
  the thing being claimed.
* **`ehrt.docs-tooling.roadmap-lint-test`** — 20 tests / 32 assertions,
  0 failures, after the retirement.
* **`gitleaks`** — clean on every staged tree.

## 7. Fences honoured

No `interface.clj` edit. No accumulator logic edited — the two enabling
diffs are the two set literals and nothing else. No draw-order change.
The oracle never moved, so the stop-and-show never fired.

## 8. What this session deliberately did NOT do

* **It did not take option (c) on `:warm-up-mark`** — threading the
  window — though the measurement says that would reach full product at
  zero output cost. A2(b) ruled the omission permanent, and the price of
  (c) is named in census 3e so it is not re-proposed as a discovery.
* **It did not delete site 3**, which census 4d still says is the arc's
  cheapest deletion now that 2 × A4 gives it a first-class source.
* **It did not take the O(N)-per-cancel win** that 3 × A4 makes payable
  (ADR-0169's 35.3%-of-generate cost). Rewiring the fallback to read the
  index is a change to the SITE, not the enabling of a pair.
* **It did not fix `:log-mirror`'s reversal** on a world carrying no
  `:ground-truth` — rowed by stage 2, still rowed.
