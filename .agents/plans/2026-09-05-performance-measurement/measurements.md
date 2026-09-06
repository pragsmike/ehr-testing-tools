# Measurements — 2026-09-05

Measured 2026-09-05 at `3114dbfe`, on the traffic-scale programme's own
reference machine (WSL2, 6c/12t i7-10750H, 15 GiB, OpenJDK 21.0.7, JVM
defaults as shipped — `MaxHeapSize` 3.88 GB, `bin/ehrt` sets no JVM
options). Cells, driver and raw `/usr/bin/time -v` output are this
directory's [`README.md`](README.md), [`run-cells.sh`](run-cells.sh)
and [`raw/`](raw/).

**One seed on one machine.** Read a slope as an order of magnitude and
a share as a ranking, never as a benchmark — the same caveat
`docs/consuming-ground-truth.md#scale` puts on its own table.

## What is being measured, and why it is not the Scale table

`docs/consuming-ground-truth.md#scale`'s wall is `ehrt corpus generate
sim` — generation **plus** self-check, HL7 rendering and spooling. The
prime audience's invocation is not that one. A QA team using this
simulator as a ground-truth oracle runs

```bash
ehrt sim run --format ground-truth > log.edn
ehrt sim check --config <the same config> < log.edn
```

and pays for none of the emission half. **Nothing measured that pair
until this session.** These figures are that pair.

## The decade

Ward scale ×1 throughout; `:persons :count` is 2× the arrival count
where present (the provenance's own rule) and the key is absent where
it is not. Each figure is the mean of **two timed JVMs**, one process
per run, `/usr/bin/time -v` around each. **Both timed runs of every one
of the eight cells produced a byte-identical log** (sha-256 of the
shipped writer's own bytes, in `raw/*.sha256`), and **every `sim check`
exited 0.**

| cell | arrivals | `:persons` | events | generate | check | gen peak RSS | check peak RSS |
|---|---|---|---|---|---|---|---|
| `a2500-nopersons` | 2,500 | — | 43,103 | **32.12 s** | **17.74 s** | 851 MB | 897 MB |
| `a2500-persons` | 2,500 | 5,000 | 53,942 | **47.73 s** | **20.48 s** | 989 MB | 940 MB |
| `a7500-nopersons` | 7,500 | — | 131,410 | **159.75 s** | **37.73 s** | 1,913 MB | 1,524 MB |
| `a7500-persons` | 7,500 | 15,000 | 167,197 | **235.09 s** | **43.86 s** | 2,091 MB | 1,887 MB |
| `a22500-nopersons` | 22,500 | — | 431,677 | **1,472.56 s** | **103.25 s** | 3,732 MB | 3,182 MB |
| `a22500-persons` | 22,500 | 45,000 | 533,147 | **2,329.46 s** | **130.57 s** | 3,973 MB | 3,759 MB |

Per-run figures, for the identity claim, are in
[`raw/results.tsv`](raw/results.tsv).

### Startup is 8 seconds, and at the bottom of the decade that is most of the check

A slope taken over raw walls is wrong by however much fixed process
cost each point carries, and here that cost is large relative to the
small cells. It was measured rather than assumed — the minimum of three
JVMs each doing the trivial version of the same work:

| phase | fixed cost | how |
|---|---|---|
| `sim run` | **8.0 s** | `--patients 1` on a decade cell's own config |
| `sim check` | **7.9 s** | an empty log on stdin, same `--config` |

At `a2500-nopersons` that is **45%** of the 17.74 s check wall. Slopes
are therefore reported both ways below, and the corrected column is the
one that says anything about the algorithm.

## Slopes

Log-log slope of wall against ARRIVALS between adjacent points, and
across the whole 9× decade. `corrected` subtracts the fixed cost above.

**Generate**

| variant | 2,500→7,500 | 7,500→22,500 | decade (9×) |
|---|---|---|---|
| `:persons` on, raw | 1.451 | 2.088 | 1.769 |
| `:persons` on, **corrected** | **1.587** | **2.116** | **1.851** |
| `:persons` off, raw | 1.460 | 2.023 | 1.741 |
| `:persons` off, **corrected** | **1.674** | **2.063** | **1.869** |

**Check**

| variant | 2,500→7,500 | 7,500→22,500 | decade (9×) |
|---|---|---|---|
| `:persons` on, raw | 0.694 | 0.993 | 0.843 |
| `:persons` on, **corrected** | **0.956** | **1.117** | **1.037** |
| `:persons` off, raw | 0.687 | 0.916 | 0.802 |
| `:persons` off, **corrected** | **1.009** | **1.058** | **1.034** |

### The two things these say

**CHECK IS NO LONGER QUADRATIC — it is linear.** ADR-0169 measured
**1.814** and named six invariants as 99.4% of the phase, with
`occupancy-within-capacity` alone at 54.9%. The corrected decade slope
is now **1.03–1.04**, in both variants and in every adjacent pair.
Arc 0's fold-carried incremental state did what it was commissioned to
do, and `642d70a`'s validator hoist took the constant down with it.
The roadmap row's *"the `engine/replay` share above must be re-read
against that wall"* is answered: at 533,147 events the whole check
phase is **130.57 s**, 122.67 s of it real work, and it grows in step
with the log.

**GENERATE IS THE QUADRATIC, AND IT IS STEEPENING.** The decade
aggregate, **1.851** with `:persons` and **1.869** without, is close
enough to ADR-0169's **1.786** to say that number was not wrong. But it
is an average over a curve that is not straight: the local slope rises
from **1.59–1.67** at the bottom of the decade to **2.12–2.06** at the
top. Quoting a single exponent understates what the next decade costs.
Normalising to EVENTS rather than arrivals — events per arrival is
itself climbing, 21.58 → 22.29 → 23.70 with `:persons` on — does not
remove it: **1.541** then **2.005**.

## 67,500 arrivals was not run, and the reason is measured

The session prompt's top cell was 67,500. It is unreachable on the
shipped configuration, and both bounds were measured rather than
extrapolated:

* **Heap.** `a22500-persons` peaks at **3,973 MB** RSS against a shipped
  `MaxHeapSize` of **3.88 GB** — the ceiling itself. There is no headroom
  at 3× that.
* **Wall.** At the measured top-of-decade slope of 2.12, 67,500 arrivals
  is ~**9.4 hours** per generate JVM, and the protocol here is two of
  them per cell plus a warm-up.

What is recorded instead is the decade **2,500 / 7,500 / 22,500** — the
same 9× span the prompt asked for, one decade lower, anchored on 7,500
so the figures reconcile with the published Scale table's own cell.

## The facility axis

Ward scale is the other dial, measured at **7,500 arrivals held fixed**,
`:persons` 15,000, so the only thing moving is the ward count.

| wards | beds+surge | events | generate | Δ vs ×1 |
|---|---|---|---|---|
| ×1 | 900 | 167,197 | 235.09 s | — |
| ×3 | 2,700 | 167,184 | 253.03 s | **+7.6%** |
| ×9 | 8,100 | 167,212 | 317.85 s | **+35.2%** |

Slope over ward scale is **0.137** — strongly sublinear, so the
allocator is not walking the whole facility per event. But **+35.2% is
not nothing**, and note the event counts: 167,197 / 167,184 / 167,212.
**Three different logs.** That is the measured form of the argument in
[`README.md`](README.md) for why the decade could not be taken at three
ward scales: more beds is a different bed vocabulary and a different
`allocate` result, so those would have been three experiments rather
than one experiment at three sizes.

## What `:persons` costs

| arrivals | events added | Δ events | generate added | Δ generate |
|---|---|---|---|---|
| 2,500 | +10,839 | +25.1% | +15.61 s | **+48.6%** |
| 7,500 | +35,787 | +27.2% | +75.34 s | **+47.2%** |
| 22,500 | +101,470 | +23.5% | +856.90 s | **+58.2%** |

The demographic timeline is roughly a quarter of the events and roughly
half the wall, and its share of the wall is **growing** at the top of
the decade. Both variants carry the same slope to within 0.02, so
`:persons` is a large constant factor on the same curve rather than a
second curve.

## A 7-event divergence, recorded and not chased -- RESOLVED 2026-09-06

**Resolution, added 2026-09-06.** It was a VERSION delta and not a path
divergence, derived in
[`../2026-09-05-7-event-divergence.md`](../2026-09-05-7-event-divergence.md):
at one tip the two invocations are byte-identical (`sim run --format
ground-truth` is `corpus generate sim`'s `events.edn` plus a trailing
newline), and the README's 167,190 is reproduced to the event at
`007deea6`, the commit before ADR-0179. The whole +7 is that ADR --
and with it, the first non-empty R-queue population in this tree. The
section below stands as this session wrote it.

## The divergence as this session recorded it

`demos/scenarios/dense-7500/README.md` reports **167,190** events for
`corpus generate sim` at `--seed 20260824 --patients 7500 --churn`
against `config.edn`. This session's `sim run --format ground-truth`
reports **167,197** on the same seed, the same flags, and a config file
that is `cmp`-identical to that one — a divergence of **7 events**
between the two invocations on identical inputs.

It is recorded here and NOT investigated: this session changes no engine
code (`R-measure-first`), and the question of which path is right is a
finding for a ruling, not a measurement. Both counts come from
`bin/event-census`'s own `Total:` line, so the counting method is not
the difference.

## Profiles

Recorded with JFR — zero new dependency, the recorder ships in the JVM
and `jfr` is a JDK 21 tool — by
[`profile-cell.sh`](profile-cell.sh), which also writes the aggregated
tables. Full tables per phase are in [`raw/`](raw/):
`a7500-persons.gen.profile.md`, `a22500-persons.gen.profile.md`,
`a7500-persons.check.profile.md`,
`a22500-persons.checkdeep.profile.md`.

**The `.jfr` recordings themselves are NOT committed** — 197 MB and
25 MB, against the 5 MB ceiling this session was given. The aggregated
tables are, and `profile-cell.sh` regenerates them from a fresh
recording.

### Two traps this profile fell into first, both worth stating

**`jfr print` truncates every stack to FIVE frames by default.** Not
the recording — the printer. A first aggregation run over these exact
recordings reported `replay` at **0.00%** of the check phase, and the
reading "arc 0 removed it entirely" was available and completely wrong:
the tool was showing the top five frames of each sample, and `replay`
sits far below that. `--stack-depth 2048` is in the script for this
reason and the figures below are all taken with it. **An inclusive
profile of Clojure taken at the default depth is not a weak measurement,
it is a wrong one**, and it fails in the direction of declaring outer
frames free.

**The recording's own `stackdepth` is 64 by default**, which is a
separate cap and a real one for Clojure. The check phase was re-recorded
at `stackdepth=2048` (`raw/a22500-persons.checkdeep.profile.md`) to
check whether the outer frames were being lost; they were not — 50.98%
against 50.97% for `apply-events` between the two recordings, so the
default depth was adequate here. The generate profile was NOT
re-recorded deep (forty minutes) and carries that as a caveat: its
innermost-frame table is unaffected either way.

### Generate — where the time actually is

Inclusive share: the fraction of samples taken anywhere beneath the
named function. **Shares do not sum to 100%** — a sample inside
`occupancy-board` under `decide` counts in both, which is the point.

| site | 7,500 | **22,500** | ADR-0169's figure |
|---|---|---|---|
| `decide` (the whole dispatch) | 62.02% | **70.35%** | — |
| `decide :discharge`'s `waiting-boarder` | 25.23% | **30.54%** | ~7.9%, rowed OUT |
| `run/select-person` | 14.17% | **19.98%** | **on no list at all** |
| `sim-model/occupancy-board` | 11.65% | **17.11%** | 8.1%, rowed OUT |
| `log-index/last-uncancelled-index` | 10.29% | **10.78%** | 5.9%, F-3 |
| `person-simulator` (what `:persons` costs) | 7.18% | 4.10% | — |
| in-run self-check (`sim-check`) | 9.88% | 3.52% | — |
| `fold/replay` | 6.52% | 2.32% | — |
| `patient-simulator` (the module walk) | 2.30% | 0.79% | — |
| malli | 1.96% | 0.54% | — |

**The three sites that are RISING are the three ADR-0169 declined to
fix.** `waiting-boarder`, `occupancy-board` and `last-uncancelled-index`
were rowed OUT under `rulings.md#R-move-not-improve` at estimated shares
of 7.9%, 8.1% and 5.9%. Measured at the top of the decade they are
**30.54%, 17.11% and 10.78%** — between two and four times their
estimates, and two of the three are still climbing between 7,500 and
22,500. The estimates were made by inspection; these are samples.

**`run/select-person` is a site nobody has named**, and at 19.98% it
is the second-largest in the generate phase. It is a `filterv` over the
WHOLE person population, taken once per arrival, to drop people whose
death instant has passed (`run.clj`, `select-person`). The provenance
config makes `:persons :count` **2× the arrival count** by rule, so
that filter is O(arrivals × persons) = **O(arrivals²)** by construction
— which is exactly the shape of the accelerating curve in the slope
table above, and it is absent from ADR-0169 because ADR-0169 profiled a
configuration in which the person layer did not yet exist.

Self-time (top frame) is in the `raw/` tables and is deliberately not
reproduced here: its top entries are `KeywordLookupSite$1.get` at
12.58% and `Util.equiv` at 9.62%, which are true and name no site. The
`raw/` files also carry a third table attributing each sample to its
innermost `ehrt.` frame, which is the truncation-immune version of the
one above.

### Check — the roadmap's replay claim, measured

| site | share of the check phase |
|---|---|
| `sim-check`, whole namespace | **78.23%** |
| **the `engine/replay` calls** | **50.97%** |
| `fold/apply-events` (what `replay` is now a projection of) | 50.98% |
| `evolve` (the per-event patient fold) | 8.56% |
| `occupancy-within-capacity` | **4.08%** |
| malli (after the `642d70a` hoist) | 1.38% |

**`roadmap.md#performance-residual-sites` is confirmed and slightly
understated.** It says the 14 independent `engine/replay` calls are
"~40% of the post-arc-0 7.26 s check phase". At 533,147 events they are
**50.97%**, and `apply-events` at 50.98% shows that essentially all of
`replay`'s cost is the fold it is now a projection of rather than
anything around it.

**`occupancy-within-capacity` was 54.9% of the check phase for ADR-0169
and is 4.08% now.** Arc 0 did what it was commissioned to do, and this
is the number that says so.

### What this ranks, as a recommendation for a ruling and not a decision

**The check phase is no longer where the money is.** It is 130.57 s
against generate's 2,329.46 s at the same cell — **5.3% of the pair's
wall** — and it is linear, so it does not get worse. Removing 13 of the
14 replay folds is a real win of roughly **65 s**, and it is worth
about one twentieth of what the same effort buys in generate.

Ranked by measured share of the top-of-decade generate phase:

1. **`decide :discharge`'s `waiting-boarder` — 30.54%.** A `filter` plus
   `sort-by` over every patient, per discharge, to find one boarder.
2. **`run/select-person` — 19.98%.** A full pool `filterv` per arrival,
   O(arrivals²) under the provenance's own `:persons` rule. **Not on any
   existing list**, so it needs a row before it needs a fix.
3. **`sim-model/occupancy-board` — 17.11%.** Folds every patient ever
   created because `init-world` seeds `:patients` with all of them.
4. **`last-uncancelled-index` — 10.78%**, and flat, so it is the least
   urgent of the four despite being the one ADR-0169 F-3 left admissible.
5. **The 14 `engine/replay` calls in `check.clj` — 50.97% of check**,
   which is 2.7% of the pair.

Nothing above is enacted. `rulings.md#R-measure-first` scopes this
session to measurement, and items 1-3 are all draw-affecting or
allocation-affecting changes that need their own equivalence proof.

## Scenario census

Not how long a cell took, but which SITUATIONS it contains — whether a
corpus this size reaches the cases the invariant catalog and the
mutation operators are written against. Produced by
[`scenario-census.sh`](scenario-census.sh) over
[`census-src/ehrt/perf/scenario_census.clj`](census-src/ehrt/perf/scenario_census.clj),
which folds `replay`'s own projection — the same
`{:event :before :after :world-before :world-after}` records
`check.clj`'s invariants read — and is **pure over the log**: the same
log yields the same counts.

| cell | events | results | after move | after discharge | **after merge** | merges | merge absorbs a bed-holder | cancels | reinstates a bed | moves a bed |
|---|---|---|---|---|---|---|---|---|---|---|
| `a2500-nopersons` | 43,103 | 3,398 | 1,386 | 401 | **0** | 266 | 40 | 759 | 14 | 731 |
| `a2500-persons` | 53,942 | 3,289 | 1,345 | 375 | **0** | 303 | 38 | 711 | 19 | 680 |
| `a7500-nopersons` | 131,410 | 10,406 | 4,235 | 1,256 | **0** | 778 | 68 | 2,277 | 51 | 2,176 |
| `a7500-persons` | 167,197 | 10,274 | 4,187 | 1,206 | **0** | 891 | 68 | 2,251 | 51 | 2,146 |
| `a7500-w3-persons` | 167,184 | 10,274 | 4,194 | 1,206 | **0** | 891 | 68 | 2,281 | 51 | 2,176 |
| `a7500-w9-persons` | 167,212 | 10,274 | 4,194 | 1,206 | **0** | 891 | 68 | 2,295 | 51 | 2,190 |
| `a22500-nopersons` | 431,677 | 34,677 | 13,838 | 4,125 | **0** | 2,520 | 142 | 6,842 | 187 | 6,486 |
| `a22500-persons` | 533,147 | 33,681 | 13,459 | 3,979 | **0** | 2,850 | 167 | 6,599 | 174 | 6,252 |

> **The three result columns above are SUPERSEDED — see
> [Correction, 2026-09-06](#correction-2026-09-06--the-merge-columns-predicate-was-wrong-and-the-column-is-not-zero)
> below.** The other eight columns are unchanged and were re-derived
> from the same logs. The table is kept as written because it is what
> this session found with the predicate it had.

A result's `:location` is stamped **at order time** — `decide.clj` says
so in its own words at the `result-event` binding — so comparing it
against the subject's state as the result LANDS is exactly "what
changed during the turnaround". The three columns are ordered
most-specific-first so a post-merge result is not also counted as a
merely-moved one.

### The merge column is zero on every cell, and that is the finding

> **SUPERSEDED 2026-09-06. The zero was the predicate, not the corpus,
> and this section's conclusion — that the gap is structural — is
> WITHDRAWN.** Kept verbatim below because the reasoning it models is
> the reasoning that produced the error: every sentence in it is
> sound given the column, and the column was measuring the wrong
> thing. See the correction that follows it.

`.agents/plans/2026-09-01-event-mutation-population-ledger.md` recorded
that the gated corpora contain **no result pending at a merge**, so the
bracket that would convict a defect there is blind, and ADR-0179's own
step-1 census (`.agents/plans/2026-09-05-adr-0179-merge-census.md`)
found the same zero across all 38 ground-truth-carrying oracle roots and
in the downstream fixture at 500 and 1,000 arrivals.

**It is still zero at 533,147 events**, in a corpus with **2,850
merges** of which **167 absorb a patient who was holding a bed**. That
is roughly 12× the largest population either of those records reached,
and it does not produce one witness.

So the gap is **structural, not a matter of scale**, and generating a
bigger corpus is not the way to close it. Something in the ordering
makes the two mutually exclusive — the obvious candidate being that a
patient with a `:result-followup` queued is not a merge candidate, or
that the merge resolves the queue before the result can land. **This is
named, not diagnosed**: `R-measure-first` scopes this session to
measurement, and which of those it is wants a reading of the merge
decide path rather than another run.

The rest of the columns are healthy and grow with the corpus, which is
what makes the zero meaningful rather than merely small: **merges
absorbing a bed-holder** go 40 → 68 → 167, and **cancels reinstating a
bed** 14 → 51 → 187, so the census is not silent about the neighbouring
cases.

### Correction, 2026-09-06 — the merge column's predicate was wrong, and the column is not zero

Re-derived this session (ADR-0180's charter session) over the SAME
eight logs, still in `~/perf-out`, with the same driver and the same
heap. Nothing about the corpus moved; the instrument did.

**What the predicate was.** `scenario_census.clj` counted a
`:result-available` whose own subject's status at the event was
`:merged`. ADR-0179's **R-queue** makes that unsatisfiable rather than
rare: a pending result FOLLOWS the survivor, so the subject a result
lands on is by construction the record that did NOT disappear, and a
log where it were `:merged` is one R-queue forbids. The column was
asking for a state the engine cannot produce, and it correctly answered
zero on every corpus at every scale.

**What the predicate is now.** R-inv's own condition, which is what
`result-references-existing-order-and-follows-it-in-time` had to be
widened to accept: a result whose **cited order names a different
subject**, joined to the result's own subject by a `:merge` at `:t` at
or before the result's `:t`. Derived from ADR-0179's wording rather
than borrowed from `check.clj` — those two functions are private there,
and this namespace's own population-closure rule is that an instrument
sourced from the checker can only confirm the checker agrees with
itself.

The census now takes the log alongside `replay`'s entries, because
`:order-event-id` is an INDEX into the log and no per-entry projection
can resolve it. It is still pure over the log.

| cell | events | results | after move | after discharge | **after merge** | merges | merge absorbs a bed-holder | cancels | reinstates a bed | moves a bed |
|---|---|---|---|---|---|---|---|---|---|---|
| `a2500-nopersons` | 43,103 | 3,398 | 1,372 | 400 | **15** | 266 | 40 | 759 | 14 | 731 |
| `a2500-persons` | 53,942 | 3,289 | 1,332 | 372 | **16** | 303 | 38 | 711 | 19 | 680 |
| `a7500-nopersons` | 131,410 | 10,406 | 4,219 | 1,252 | **20** | 778 | 68 | 2,277 | 51 | 2,176 |
| `a7500-persons` | 167,197 | 10,274 | 4,174 | 1,198 | **21** | 891 | 68 | 2,251 | 51 | 2,146 |
| `a7500-w3-persons` | 167,184 | 10,274 | 4,181 | 1,198 | **21** | 891 | 68 | 2,281 | 51 | 2,176 |
| `a7500-w9-persons` | 167,212 | 10,274 | 4,181 | 1,198 | **21** | 891 | 68 | 2,295 | 51 | 2,190 |
| `a22500-nopersons` | 431,677 | 34,677 | 13,813 | 4,120 | **30** | 2,520 | 142 | 6,842 | 187 | 6,486 |
| `a22500-persons` | 533,147 | 33,681 | 13,428 | 3,967 | **43** | 2,850 | 167 | 6,599 | 174 | 6,252 |

**The 7,500 persons cell reads 21, and 21 is the number ADR-0179's own
addendum independently derived** — by a different route, on the same
configuration (`cell-a7500-persons.edn` is
`demos/scenarios/dense-7500/config.edn` byte for byte, which
`derive-cells.sh` asserts as its strongest check, and the run
parameters are the same `--seed 20260824 --patients 7500 --churn`).
That addendum counted the results whose cited order names a different
subject directly off the log and got 21, of which 21 resolve through a
merge; this instrument folds `replay` and gets 21. Two instruments, one
answer.

**Every cell reconciles exactly, which is the check on the fix.** The
column's population came out of the two columns to its left, and the
arithmetic closes on all eight: `after move` and `after discharge` each
lose exactly the results that are now counted as crossing a merge —
14+1=15, 13+3=16, 16+4=20, 13+8=21, 13+8=21, 13+8=21, 25+5=30,
31+12=43. The eight columns from `merges` rightward are **identical**
to the original table, digit for digit, which is what says the change
reached only the result arm.

**The finding inverts.** The merge column is not zero and never was:
it grows 15/16 → 20/21 → 30/43 across the decade, and the corpus
reaches the scenario at every scale measured, down to 2,500 arrivals.
What was reported as a structural gap — "something in the ordering
makes the two mutually exclusive" — was a gap in the instrument.
`.agents/plans/2026-09-01-event-mutation-population-ledger.md`'s and
ADR-0179's own zero-population claims are NOT corrected by this: those
were counted over the 38 oracle roots and the downstream fixture at 500
and 1,000, they used the referential predicate, and they are true
there. This corrects one instrument on one set of eight cells.

**The ~40% ratio below survives** and is restated here rather than
edited in place: after-move against all results is 40.5% at 2,500,
40.6% at 7,500 and 39.9% at 22,500 on the persons cells, against the
40.8 / 40.8 / 40.0 the next section reports. The reclassified results
are three per thousand.

### Two ratios that hold across the decade

**Results landing after their subject moved are ~40% of all results**,
at every cell: 40.8% at 2,500, 40.8% at 7,500, 40.0% at 22,500. A
consumer that assumes a result's PV1 context is where the patient is
NOW is wrong about two results in five, at every scale this simulator
has been run at.

**`cancel-reinstates-bed` equals the `:cancel-discharge` count exactly,
and `cancel-moves-bed` equals `:cancel-transfer`**, on all eight cells.
That is the census agreeing with the vocabulary rather than a separate
fact, and it is here as a check on the instrument: an off-by-one in the
before/after comparison would break the identity.
