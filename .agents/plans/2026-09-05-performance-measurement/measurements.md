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

## Site 1 measured, 2026-09-06 -- `waiting-boarder` rides the fold

ADR-0180 site 1 landed at `75b4a868`, over a baseline of `bba3a63c`.
`decide`'s `waiting-boarder` is no longer a scan of `(:patients world)`:
`fold/apply-events` maintains `:boarder-index` and the function is an
ordered lookup over it. Output-identical by construction and by
measurement -- both cells below reproduced their pre-change log
BYTE-FOR-BYTE, and `bin/ground-truth-bracket` reported IDENTICAL on all
38 digested roots at both commits.

### Wall

**These are not the decade table's figures and must not be read against
them.** The decade above is the MEAN OF TWO timed JVMs per cell; these
are ONE each, generate only, taken on this session's machine under this
session's load. The `a7500-persons` baseline here is 256.15 s where the
committed table records 235.09 s for the same cell and seed -- same
code, different day. The comparison that carries anything is
before-against-after WITHIN this session, and that pair was run back to
back, same script, same warm-up shape.

| cell | before | after | delta | corrected delta |
|---|---|---|---|---|
| `a7500-persons` | 256.15 s | **189.79 s** | -66.36 s, **-25.9%** | **-26.7%** |
| `a2500-nopersons` | 34.85 s | **30.08 s** | -4.77 s, **-13.7%** | **-17.8%** |

`corrected` subtracts the 8.0 s fixed `sim run` startup cost measured
above -- the only column that says anything about the algorithm, and at
the small cell it is the difference between -13.7% and -17.8%.

**Both logs are byte-identical across the change.** `a7500-persons`
sha256 `3018299a0e0299c40ba73580793c8135674e8f988f9c332d18eedd45b70d3bd3`
and `a2500-nopersons`
`c22d65730b9006b1593de94461200084d2f4865b8cbd47c4893db5df98a55208`,
before and after.

**The index is not free in memory, and the smaller cell says so more
honestly than the larger.** Peak RSS at `a7500-persons` went 2,006 ->
2,151 MB (+145 MB, +7.2%); at `a2500-nopersons`, 945 -> 949 MB (+4 MB).
One sorted set per home ward holding one short vector per boarder is a
small structure, and a single-JVM RSS reading at a 3.88 GB heap is a
noisy instrument -- so the honest statement is that the index costs
memory in the direction and rough magnitude expected, not that it costs
exactly 145 MB.

### Profile

One JFR recording per side at 7,500 arrivals, `--stack-depth 2048`,
same script and same cell as the decade profile above
(`raw/a7500-persons.gen.profile.md` before,
`raw/a7500-persons-site1.gen.profile.md` after; 19,012 and 12,013
`jdk.ExecutionSample` samples). Inclusive share, so the columns do not
sum.

| site | before | after |
|---|---|---|
| `decide :discharge`'s `waiting-boarder` | 25.23% | **0.00%** |
| `decide` (the whole dispatch) | 62.02% | 47.22% |
| `fold/apply-events` | 7.56% | **9.76%** |
| `run/select-person` | 14.17% | 19.17% |
| `sim-model/occupancy-board` | 11.65% | 12.51% |
| `log-index/last-uncancelled-index` | 10.29% | 14.68% |

**0.00% is zero samples, not a rounding.** Not one of 12,013 samples
was taken anywhere beneath `waiting-boarder`, and it is gone from the
innermost-project-frame table too, where it had been the top row at
12.67%. That table's new top row is `select-person` at 16.14%.

**`apply-events` rose 2.2 points and that is the index's own cost,
charged where the charter said it would be.** The site did not become
free; its work moved from a per-call O(P) scan to a per-event O(log k)
update, and 25.23 points became 2.2.

**THE THREE RISING ROWS ARE NOT REGRESSIONS.** Every one of them is a
share of a smaller denominator: the phase itself shrank by a quarter,
so a site whose absolute cost did not move gains share. `select-person`
went 14.17% -> 19.17% having done exactly the same work, and it is
`roadmap.md#select-person-arrival-quadratic`, site 2 of this same
program. What the table says is that the remaining three sites are now
a LARGER fraction of a smaller wall, which is what R-order predicted
and the reason it sequenced them.

## Site 2 measured, 2026-09-06 -- `select-person` rides a rank/select sweep

ADR-0180 site 2 landed at `03db90a4`, over a baseline of `d9088a91` --
which is site 1's own close, so the "before" column here is the code the
section above ends at, not the code it starts from. `run/select-person`
is no longer a `filterv` over the whole `:population` per arrival:
`prelude` builds one `alive-sweep` for the whole arrival vector and the
function reads its count and its `k`th survivor. Output-identical by
construction and by measurement -- both cells below reproduced their
pre-change log BYTE-FOR-BYTE, and `bin/ground-truth-bracket` reported
IDENTICAL on all 38 digested roots at both commits.

### Wall

**Same caution as the site-1 section, and it bites harder here.** These
are ONE timed JVM per cell, generate only, this session's machine and
load. The `a7500-persons` cell reads 195.48 s at the SAME COMMIT where
the section above measured 189.79 s -- a 3% spread on identical code,
which is the size of the noise any single-JVM figure here carries. Only
before-against-after within this session says anything, and that pair
was run back to back, same script, same warm-up shape.

| cell | before | after | delta | corrected delta |
|---|---|---|---|---|
| `a7500-persons` | 195.48 s | **158.90 s** | -36.58 s, **-18.7%** | **-19.5%** |
| `a2500-nopersons` | 29.74 s | 29.45 s | -0.29 s, -1.0% | -1.3% |

`corrected` subtracts the 8.0 s fixed `sim run` startup measured above.

**`a2500-nopersons` IS THE NEGATIVE CONTROL AND ITS FLATNESS IS THE
RESULT.** That cell has no `:persons` key at all, so `select-person` is
never called and no sweep is ever built -- `prelude`'s `bindings` takes
its all-nil arm. A change confined to the person path must move that
cell by nothing, and -1.0% on a single JVM is nothing. Site 1's index
sat on the shared path and moved the same cell by -13.7%; the contrast
between the two rows is what says each index is where its charter put
it.

**Both logs are byte-identical across the change.** `a7500-persons`
sha256 `3018299a0e0299c40ba73580793c8135674e8f988f9c332d18eedd45b70d3bd3`
and `a2500-nopersons`
`c22d65730b9006b1593de94461200084d2f4865b8cbd47c4893db5df98a55208`,
before and after -- the same two digests the site-1 section records,
which makes them a three-commit-long byte-identity chain rather than a
pair.

**Peak RSS fell at the large cell and rose at the small one**: 2,389 ->
2,034 MB at `a7500-persons`, 928 -> 1,091 MB at `a2500-nopersons`. The
direction at the large cell is plausible on its own account -- the
`filterv` allocated a fresh vector of up to 15,000 person maps per
arrival and the sweep allocates one `long[]` for the run -- but the
small cell moved the OTHER way while executing none of this code at all,
which is exactly the demonstration that a single-JVM RSS reading against
a 3.88 GB heap cannot carry a claim of this size. Recorded, not claimed.

### Profile

One JFR recording per side at 7,500 arrivals, `--stack-depth 2048`, same
script and cell as above. The BEFORE column is
`raw/a7500-persons-site1.gen.profile.md` -- site 1's own after-profile,
recorded the same day on this machine at the commit this session
baselines against -- rather than a fresh recording of identical code.
After is `raw/a7500-persons-site2.gen.profile.md`; 12,013 and 10,914
`jdk.ExecutionSample` samples. Inclusive share, so the columns do not
sum.

| site | before | after |
|---|---|---|
| `run/select-person` | 19.17% | **0.01%** |
| `decide` (the whole dispatch) | 47.22% | 59.44% |
| `sim-model/occupancy-board` | 12.51% | 17.87% |
| `log-index/last-uncancelled-index` | 14.68% | 17.63% |
| `fold/apply-events` | 9.76% | 13.06% |
| `person-simulator` | 11.08% | 12.01% |

**0.01% is ONE sample of 10,914**, and the site is gone from the
innermost-project-frame table, where it had been the top row at 16.14%.
That table's new top row is `occupancy-board`'s own inner function at
11.87% -- site 3 of this program.

**Nothing here absorbed the 19 points the way `apply-events` absorbed
site 1's.** The sweep's whole cost is O(n) once at `prelude` plus
O(log n) per arrival, and at this cell that is beneath the sampler's
floor: there is no row for it to appear in. The four rows that rise do
so because the denominator shrank, and every one of them is doing
exactly the work it did before -- the same reading the site-1 section
made of its own three rising rows, and the same reason R-order
sequenced these sites rather than fixing them together.

## Site 3 measured, 2026-09-06 -- `occupancy-board` rides the fold

ADR-0180 site 3 landed at `24f7915e`, over a baseline of `2149db0a` --
site 2's own close, so the "before" column here is the code the section
above ends at. `sim-model/occupancy-board` is no longer an `into {}`
over the whole `:patients` map per placement: `fold/apply-events`
maintains the same map as `:board` off each event's own pre/post
participant pair, and the five sites that rebuilt it read the index --
four in `decide` plus
`log-index/bed-reoccupied-by-someone-else?`. `bed-ready-location`'s
second query shape, `(occupancy-board (dissoc (:patients world)
patient-id))`, is answered by masking that one index rather than by a
second one. Output-identical by construction and by measurement: both
cells below reproduced their pre-change log BYTE-FOR-BYTE, and
`bin/ground-truth-bracket` reported IDENTICAL on all 38 digested roots
at both commits.

### Wall

**Same caution as the two sections above.** One timed JVM per cell,
generate only, this session's machine and load. This session's
`a7500-persons` baseline reads 164.70 s where the site-2 section
measured 158.90 s at the very commit this one baselines against -- a
3.7% spread on identical code, which is the size of the noise any single
figure here carries. Only before-against-after within this session says
anything, and that pair was run back to back, same script, same warm-up
shape.

| cell | before | after | delta | corrected delta |
|---|---|---|---|---|
| `a7500-persons` | 164.70 s | **136.32 s** | -28.38 s, **-17.2%** | **-18.1%** |
| `a2500-nopersons` | 30.25 s | 29.61 s | -0.64 s, -2.1% | -2.9% |

`corrected` subtracts the 8.0 s fixed `sim run` startup measured above.

**THE SMALL CELL IS NOT A CONTROL HERE, AND ITS FLATNESS IS NOT THE
RESULT.** Site 2's `a2500-nopersons` row was a control: that cell has no
`:persons`, so the code site 2 replaced was never called at all. The
board is on the SHARED path and this cell does run it -- the -2.1% is a
real if small win, and the reason it is small is the shape of the
defect. The scan is O(patients) per placement over a `:patients` map
that is the whole arrival population from t 0, so its total cost goes as
arrivals squared: at 2,500 arrivals against 7,500 that is a ninth of the
work spread over a wall a fifth the size. A quadratic site pays back in
proportion to the square, which is exactly why this program is
sequenced by share at the TOP cell.

**Both logs are byte-identical across the change.** `a7500-persons`
sha256 `3018299a0e0299c40ba73580793c8135674e8f988f9c332d18eedd45b70d3bd3`
and `a2500-nopersons`
`c22d65730b9006b1593de94461200084d2f4865b8cbd47c4893db5df98a55208`,
before and after -- the same two digests the site-1 and site-2 sections
record, which makes them a five-commit-long byte-identity chain.

**Peak RSS fell at the large cell and rose at the small one**, again:
2,248 -> 2,197 MB at `a7500-persons`, 959 -> 1,102 MB at
`a2500-nopersons`. Recorded, not claimed, for the reason the section
above gives at length -- a single-JVM RSS reading against a 3.88 GB heap
cannot carry a claim of that size, and the cell that moved 15% the wrong
way while doing strictly less allocation is the demonstration rather
than an anomaly.

### Profile

One JFR recording per side at 7,500 arrivals, `--stack-depth 2048`, same
script and cell as above. The BEFORE column is
`raw/a7500-persons-site2.gen.profile.md` -- site 2's own after-profile,
recorded the same day on this machine at the commit this session
baselines against -- rather than a fresh recording of identical code.
After is `raw/a7500-persons-site3.gen.profile.md`; 10,914 and 9,238
`jdk.ExecutionSample` samples. Inclusive share, so the columns do not
sum.

| site | before | after |
|---|---|---|
| `sim-model/occupancy-board` | 17.87% | **0.00%** |
| `decide` (the whole dispatch) | 59.44% | 51.17% |
| `log-index/last-uncancelled-index` | 17.63% | 21.63% |
| `fold/apply-events` | 13.06% | 15.43% |
| `person-simulator` | 12.01% | 15.33% |
| `run/select-person` | 0.01% | 0.01% |
| `decide :discharge`'s boarder sort-by | 0.01% | 0.01% |

**0.00% is ZERO samples of 9,238**, and the site is gone from the
innermost-project-frame table, where it had held the top two rows it
appears in at all -- `occupancy_board$fn` 11.87% and
`occupancy_board.invokeStatic` 5.83%, 17.70 points between them. That
table's new top row is `last_uncancelled_index$fn` at 9.76%, which is
site 4.

**`apply-events` ABSORBED PART OF IT, and that is the index's own cost
charged where the charter said it would be**: 13.06% -> 15.43%, with
`fold$apply_events$fn` appearing in the innermost table at 4.76%. 17.9
points became 2.4. This is site 1's pattern and not site 2's -- site 2's
sweep was O(n) once at `prelude` and had no row to appear in, while both
in-fold indexes are per-event work that lands in the fold's own frame.

**Every other row that rises is doing exactly the work it did before.**
The denominator shrank by a sixth, and `licensed-bed-ids` newly appears
in the innermost table at 3.30% for the same reason -- `allocate`'s id
derivation was always there, behind a board scan that is no longer in
front of it. The one remaining site, `last-uncancelled-index`, is now
the largest named site under `decide` and is site 4 of this program.

## Site 4 measured, 2026-09-06 -- `last-uncancelled-index` rides the fold

ADR-0180 site 4 landed at `5ac15dce`, over a baseline of `228e2c62` --
site 3's own close, so the "before" column here is the code the section
above ends at. `log-index/last-uncancelled-index` no longer makes two
whole-log passes per cancel decide: `fold/apply-events` maintains the two
maps it used to build on every call as `:cancel-index` -- `[patient-id
event-type]` -> that patient's log indices of that type in log order, and
`cancel-type` -> the set of indices that class of cancel has consumed --
and the function is the ordered lookup over them, `fold/last-uncancelled`.
Output-identical by construction and by measurement: both cells below
reproduced their pre-change log BYTE-FOR-BYTE, and
`bin/ground-truth-bracket` reported IDENTICAL on all 38 digested roots at
both commits.

### Wall

**Same caution as the three sections above.** One timed JVM per cell,
generate only, this session's machine and load. This session's
`a7500-persons` baseline reads 144.77 s where the site-3 section measured
136.32 s at the very commit this one baselines against -- a 6.2% spread on
identical code, the largest of the four sessions' and the same instrument
noise every one of them recorded. Only before-against-after within this
session says anything, and that pair was run back to back, same script,
same warm-up shape.

| cell | before | after | delta | corrected delta |
|---|---|---|---|---|
| `a7500-persons` | 144.77 s | **112.29 s** | -32.48 s, **-22.4%** | **-23.7%** |
| `a2500-nopersons` | 30.44 s | **26.92 s** | -3.52 s, -11.6% | **-15.7%** |

`corrected` subtracts the 8.0 s fixed `sim run` startup measured above.

**THE SMALL CELL IS NOT A CONTROL HERE EITHER**, and its -15.7% is the
largest small-cell move of the four sessions. The cancel decides are on
the SHARED path -- `:persons` has nothing to do with them -- so this cell
runs the site in full. It moves proportionally less than the large one for
the reason the site-3 section gives about its own: the scan was O(N) in
the LOG per cancel decide and the number of cancel decides is itself
proportional to the log, so its total cost goes as events squared. A third
of the arrivals is roughly a ninth of the work spread over a fifth of the
wall.

**Both logs are byte-identical across the change.** `a7500-persons` sha256
`3018299a0e0299c40ba73580793c8135674e8f988f9c332d18eedd45b70d3bd3` and
`a2500-nopersons`
`c22d65730b9006b1593de94461200084d2f4865b8cbd47c4893db5df98a55208`, before
and after -- the same two digests the site-1, site-2 and site-3 sections
record, which makes them a SEVEN-COMMIT byte-identity chain spanning the
whole program.

**Peak RSS rose at both cells this time**: 2,064 -> 2,303 MB at
`a7500-persons` (+11.6%) and 884 -> 903 MB at `a2500-nopersons` (+2.1%).
Recorded, not claimed, for the fourth session running -- and this is the
one site where a rise is the EXPECTED direction rather than a puzzle.
`:cancel-index` is the only one of ADR-0180's four indexes that NEVER
EVICTS: a log index is immutable once written, so the index only grows,
holding one integer per event per patient participant for the whole run.
The other three reconcile a membership and can shrink.

### Profile

One JFR recording per side at 7,500 arrivals, `--stack-depth 2048`, same
script and cell as above. The BEFORE column is
`raw/a7500-persons-site3.gen.profile.md` -- site 3's own after-profile,
recorded the same day on this machine at the commit this session baselines
against -- rather than a fresh recording of identical code. After is
`raw/a7500-persons-site4.gen.profile.md`; 9,238 and 7,470
`jdk.ExecutionSample` samples. Inclusive share, so the columns do not sum.

| site | before | after |
|---|---|---|
| `log-index/last-uncancelled-index` | 21.63% | **0.00%** |
| `decide` (the whole dispatch) | 51.17% | 36.85% |
| `fold/apply-events` | 15.43% | 20.05% |
| `person-simulator` | 15.33% | 20.19% |
| `patient-simulator` | 4.48% | 6.06% |
| `evolve` | 2.87% | 3.16% |
| `sim-model/occupancy-board` | 0.00% | 0.00% |
| `run/select-person` | 0.01% | 0.03% |
| `decide :discharge`'s boarder sort-by | 0.01% | 0.01% |

**0.00% is ZERO samples of 7,470**, and `last_uncancelled_index$fn` -- the
TOP ROW of the innermost-project-frame table after site 3, at 9.76% -- is
gone from that table entirely. The frame itself is still on the stack of
every cancel decide, because the function still exists and still answers
the question; nothing samples beneath it any more.

**`apply-events` absorbed 4.6 points of the 21.6**, 15.43% -> 20.05%: the
index's own cost charged where the charter said it would be, and the same
pattern sites 1 and 3 recorded for the same reason -- an in-fold index is
per-event work that lands in the fold's own frame. `fold$apply_events$fn`
and its two children hold 6.35%, 2.62% and 1.75% of the innermost table.

**Every other row that rises is doing exactly the work it did before.**
The denominator shrank by a fifth and `person-simulator`,
`patient-simulator` and `evolve` rise by about that fifth. `decide` is the
exception and FALLS, 51.17% -> 36.85%, because the work removed was inside
it.

### The program, site by site

ADR-0180's four sites are done. Each row's detail is that site's own dated
section above. Shares are inclusive share of the generate phase at 7,500
arrivals, each measured against the profile its own session baselined on
-- so the four "before" figures have four different denominators and are
NOT additive.

| site | landed | share before | after | cell wall, corrected |
|---|---|---|---|---|
| 1 `decide :discharge`'s `waiting-boarder` | `75b4a868` | 25.23% | 0.00% | -26.7% |
| 2 `run/select-person` | `03db90a4` | 19.17% | 0.01% | -19.5% |
| 3 `sim-model/occupancy-board` | `24f7915e` | 17.87% | 0.00% | -18.1% |
| 4 `log-index/last-uncancelled-index` | `5ac15dce` | 21.63% | 0.00% | -23.7% |

**Cumulatively, which is the figure the roadmap row now carries**: the
`a7500-persons` cell went **256.15 s -> 112.29 s**, -143.86 s and **-58.0%
corrected**; `a2500-nopersons` went **34.85 s -> 26.92 s**, **-29.5%
corrected**. `fold/apply-events` went 7.56% -> 20.05%, which is the three
in-fold indexes' whole cost, charged in one place and against a
denominator less than half the size it was.

**THE CUMULATIVE FIGURE IS A LOWER BOUND, NOT A BEST CASE.** Each session
re-baselined, and every re-baseline read HIGHER than the previous
session's after: 189.79 -> 195.48, 158.90 -> 164.70, 136.32 -> 144.77, a
drift of +3.0%, +3.7% and +6.2% on identical code. The machine got slower
across the day, so measuring the first session's before against the last
session's after understates the four sites by whatever that drift is
worth.

### 22,500 arrivals: the program measured at the top of the decade

ONE timed generate of `a22500-nopersons`, this session's machine, at
`5ac15dce`.

| | generate | peak RSS | bytes | sha256 |
|---|---|---|---|---|
| decade table, 2026-09-05 at `3114dbfe` | 1,472.56 s | 3,732 MB | 174,866,696 | `7d105743...78e0a` |
| this session, at `5ac15dce` | **396.18 s** | 3,599 MB | 174,866,696 | `7d105743...78e0a` |

**THE LOG IS BYTE-IDENTICAL TO THE COMMITTED DIGEST**, and that is the
half of this row that carries a claim rather than an impression.
`raw/a22500-nopersons.sha256` records
`7d105743ebd3ac118e035bb7ca9acb9a6491bfb3a8cc7cab95a5626924478e0a` from
2026-09-05, both of that session's two timed JVMs; this session's single
JVM produced the same digest over the same 174,866,696 bytes. So the whole
four-site program is output-identical AT THE TOP OF THE MEASURED DECADE
and not only on the two cells each session bracketed -- a 431,677-event
log, an order of magnitude past the 38 oracle roots and the two cells,
agreeing to the byte.

**The WALL half is an order of magnitude and nothing finer.** The two
figures differ in protocol as well as in code: the decade row is the MEAN
OF TWO timed JVMs taken with `run-cells.sh`'s own warm-up, and this is ONE
JVM whose page cache was warmed by the 7,500 profile run before it. Read
as such it is -1,076.38 s, **-73.1%**, a 3.7x speedup on identical output
at the cell where the four sites summed to 78.4 points of inclusive share.
The within-session before/after pairs above remain the only figures in
this document precise enough to attribute.

**Peak RSS FELL here**, 3,732 -> 3,599 MB, where both 2026-09-06 cells
rose. Recorded, not explained: a single-JVM RSS reading against a 3.88 GB
`MaxHeapSize` is a measurement of when the collector chose to run, and
this session has now seen it move both ways on the same change.

## Post-program profile, 2026-09-06 -- CPU, allocation and the live set

Four `jdk.ExecutionSample` recordings at `63d5ee64`, after all four
ADR-0180 sites: `a7500-persons` generate (10,081 samples) and check
(3,399), `a22500-nopersons` generate (32,394) and check (8,630). The
allocation tables below come off the SAME four recordings --
`settings=profile` enables `jdk.ObjectAllocationSample` at a `300/s`
throttle with stack traces already, so nothing was re-recorded and the
CPU and allocation views describe one run rather than two. The 22,500
generate under JFR produced 174,866,696 bytes digesting to
`7d105743...78e0a`, the committed figure, so the profiled corpus is the
measured one.

### (a) Where the CPU is now

Inclusive share, 22,500-arrival generate. The four sites are gone from
the decide loop and two frames nobody has rowed are what is left.

| site | share of generate | ADR-0180's own figure |
|---|---|---|
| `decide` (the whole dispatch) | **72.28%** | 70.35% pre-program |
| in-run self-check (`sim-check`) | **18.79%** | -- |
| `fold/apply-events` | 14.60% | 7.56% -> 20.05% at 7,500 |
| `sim-model/occupancy-board` | 0.48% | site 3, was 17.11% |
| `log-index/last-uncancelled-index` | 0.01% | site 4, was 10.78% |
| `decide :discharge`'s `waiting-boarder` | 0.01% | site 1, was 30.54% |
| `run/select-person` | 0.00% | site 2, was 19.98% |

**`occupancy-board`'s residual 0.48% is not site 3 leaking.** All 157 of
its samples are under `sim-check` and NONE under `decide`, counted
directly: generate ends with an in-run self-check
(`components/sim/src/ehrt/sim/run.clj:783`), and that check calls the
from-scratch definitions the sites' own gates keep alive. Site 3 cleared
the decide loop completely. `last-uncancelled-index`'s 4 samples and
`waiting-boarder`'s 2 ARE under `decide`, at 0.01% each -- at the noise
floor of a 32,394-sample recording, recorded rather than explained.

**The two frames that are left are the same shape ADR-0180 fixed, and
neither is on its census.** The project-frame table's top rows are
gensym-named, so they are resolved here to source once:

| frame | self | inclusive | resolves to |
|---|---|---|---|
| `decide$eval10066$fn__10069` (+ inner) | 8.70% | **34.06%** | `decide :merge`, `decide.clj:1449` |
| `decide$eval10038$fn__10041` (+ inner) | 14.57% | **31.38%** | `decide :bed-swap`, `decide.clj:1414` |

`decide :bed-swap` builds `eligible` with a `remove`/`filter`/`mapv`
over the WHOLE `:patients` map, once per bed-swap. `decide :merge` does
the same, and then answers `already-merged?` with a `some` over the
ENTIRE `:ground-truth` log -- which is precisely the shape site 4
removed for cancels, surviving under a different method's name.

At 7,500 WITH `:persons` the ranking is different and the top row is
also unrowed: `person-simulator` at 22.80% inclusive, whose
`process/walk-person` is the single largest self frame at 8.79%.

**`apply-events`, un-summed.** The concern breakdown charges the one
fold ADR-0180 loaded four indexes into:

| concern | % of `apply-events`, 22,500 gen | % of the phase |
|---|---|---|
| `:patient-state` (the `evolve` multimethod) | 17.61% | 2.57% |
| `:encounter-stamp` | 4.78% | 0.70% |
| `:cancel-index` (site 4) | 2.60% | 0.38% |
| `:bed-index` | 1.29% | 0.19% |
| `:boarder-index` (site 1) | 0.57% | 0.08% |
| `:board` (site 3) | 0.47% | 0.07% |
| residual -- the ten inline concerns and the fold itself | 72.68% | 10.61% |

**All four ADR-0180 indexes together cost 0.72% of the generate phase**
where the sites they replaced were 78.4 points of inclusive share. In
the check phase all four read 0.00%, which is the instrument agreeing
with `replay-projection`: that site declines them.

### (b) The live set, and what the peak actually is

One `a22500-nopersons` generate under `-Xmx8g` with `-Xlog:gc*`.
**Byte-identical to the committed digest** (`7d105743...78e0a`,
174,866,696 bytes), 402.35 s against 396.18 s at the default heap.

| | default heap | `-Xmx8g` |
|---|---|---|
| `MaxHeapSize` | 3.88 GB (the JVM DEFAULT -- no `-Xmx` anywhere in this tree) | 8 GB |
| peak RSS | 3,599 MB | **4,780 MB** |
| peak PRE-GC used | -- | 3,863 MB |
| Full GCs | -- | **none, in 480 collections** |

**Peak RSS ROSE when the budget rose, on a byte-identical run.** That
settles what the 3.6 GB reading is: it measures the budget G1 was given,
not the memory the run needs.

The post-collection floor -- the deepest reclaim in each 40-second
window, which is the live set:

| window | min post-GC | | window | min post-GC |
|---|---|---|---|---|
| 0-40s | 7 MB | | 200-240s | 382 MB |
| 40-80s | 111 MB | | 240-280s | 407 MB |
| 80-120s | 199 MB | | 280-320s | **432 MB** |
| 120-160s | 272 MB | | 320-360s | 702 MB |
| 160-200s | 337 MB | | 360-400s | 792 MB |

It grows monotonically with events and reaches **432 MB by the end of
the simulation loop**, its 40-second increments falling from ~100 MB to
~25 MB. Growth is with events and bounded; it is NOT linear, and the
deceleration is not explained here -- the estimator is a min-over-window
through a collector that reclaims a different amount each time, so the
series ranks the growth rather than fitting it.

**THE 3,863 MB PEAK IS NEITHER THE WORLD'S HISTORY NOR THE RENDERER.**
Dated off the profile's own sample timestamps rather than assumed:

| phase | first sample | last sample |
|---|---|---|
| `decide` (the simulation loop) | 7s | **364s** |
| in-run self-check (`sim-check`) | ~0 before 360s | **461s** |
| render (`cli/sim-ground-truth-bare-text`) | **461s** | 471s |

The heap balloons from 323 s and collapses at 396 s back to its
pre-323 s level, in a 402 s run -- the same window, at the same relative
position, as the in-run `check-all`. So the peak is a TRANSIENT held by
the self-check while it folds the whole 431,677-event log, released when
it finishes. Rendering the 175 MB corpus is the last 11 s and about 2%.

Live-set COMPOSITION, from the 112 `jdk.OldObjectSample` events the same
recording carries -- surviving objects with the stack that allocated
them. It is a bounded sample, so it ranks and does not measure: 35 of
112 were allocated at **`fold.clj:924`**, the `:warm-up-mark`
decoration's `(assoc ev :warm-up ...)`, which mints the event map that
lands in `:ground-truth`. The surviving objects are overwhelmingly small
`Object[8..32]` arrays -- `PersistentArrayMap` backing arrays and
`PersistentVector` nodes. **What survives is the log**, not the indexes:
no ADR-0180 index frame appears among the survivors at all.

### Allocation, from the same four recordings

| | 7,500 (`:persons`) | 22,500 (no `:persons`) |
|---|---|---|
| samples | 43,065 | 134,379 |
| estimated total allocation | 61.4 GB | **217.4 GB** |
| `decide :bed-swap` | 11.59% | **29.98%** |
| `decide :merge` | 6.96% | **21.01%** |
| `sim-model/licensed-bed-ids`'s inner fn | 6.91% | 5.25% |
| `fold/apply-events`' own frames | ~12% | ~9% |

**The same two methods that hold a third of the CPU allocate half of
everything.** The class table agrees rather than merely accompanying:
`PersistentVector$ChunkedSeq`, `PersistentHashMap$ArrayNode$Seq`,
`LazySeq` and `PersistentHashMap$NodeSeq` are **34.7%** between them at
22,500, which is what seq-over-hashmap scanning allocates and what an
indexed answer would not.

The two cells' totals are NOT a scaling law: 7,500 carries `:persons`
and 22,500 does not, so they are different configurations and the ratio
between them means nothing.

### (c) The check phase's fourteen replays, measured again

| | 7,500 check | 22,500 check |
|---|---|---|
| `fold/apply-events` | 49.90% | 49.85% |
| the `engine/replay` calls | 49.90% | **49.85%** |
| `sim-check`, whole namespace | 73.46% | 75.71% |
| EDN parse of the input log | -- | **20.12%** |
| `occupancy-within-capacity` | 3.24% | 3.59% |

Essentially unmoved from the 50.97% ADR-0180's R-order recorded, which
is expected -- no site touched check.

**The EDN parse row is a correction to this document's own instrument.**
The named-site list held `clojure.lang.LispReader` and reported 0.35%;
`cli/read-ground-truth-stdin` uses `clojure.edn/read`, i.e.
`EdnReader`. The real figure is 20.12%, and one fifth of the check
phase is parsing rather than checking. The project-frame table caught
it, which is the argument for carrying both.

### (d) The ranking -- a recommendation for a ruling, not a decision

**1. Check's fourteen `engine/replay` calls (R-order site 5) -- and it
is worth far more than the row says.** ADR-0180 prices it at "~65 s at
the top cell, about one twentieth of what the same effort buys in
generate", and that pricing counted only the standalone check phase. It
is wrong in the cheap direction, for three reasons this session
measured. (i) The SAME `check/check-all` runs inside generate
(`sim/run.clj:783`) and is 18.79% of that phase -- roughly 97 s of the
472 s profiled run. (ii) It is the source of the 3,863 MB peak heap, so
it, and not the world's history, is what makes 67,500 arrivals
unreachable on shipped defaults. (iii) The standalone phase is 49.85%.
One change; three payoffs; and the carrier already exists, because
`replay` is a projection of `apply-events` and `check.clj:929` already
accepts pre-folded entries.

**2. `decide :merge` and `decide :bed-swap` -- 34.06% and 31.38% of
generate, half of all allocation, on no census.** Both are the shape
R-fold-carrier was written for: a scan over a population only ever
appended to. `:merge`'s `already-merged?` is the stronger case -- a
`some` over the whole log per merge, which is site 4's own defect under
another method's name, and an index of merged patient-ids is a smaller
change than site 4 was. These belong on the roadmap row as sites 6 and
7 rather than in a new program.

**3. A memory program is NOT justified, and this session's own
measurement is why.** The world's live set is 432 MB at the end of the
loop and under 1 GB including the log, growing with events and showing
no runaway retention; the survivors are the log's event maps, not the
indexes; and the peak that would motivate streaming belongs to a
transient the self-check holds. Streaming the log, or making the world
drop its own history, would buy at most a few hundred megabytes against
a 3.4 GB transient that item 1 removes. **Revisit only if item 1 lands
and the peak stays high.**

**4. Named constant-factor sites, if anything.** `sim-model/licensed-bed-ids`'s
inner fn is 5.25% of allocation and 2.88% self CPU at 22,500 and looks
recomputed rather than carried; the 20.12% EDN parse in check is real
but is the file format's price, not a defect. ADR-0180's own "not in
scope" ruling on constant factors still stands and nothing here asks to
reopen it.

Fenced, deliberately: this session changed no engine code (R-measure-first),
and every item above is a recommendation for the design channel.

## Site 5 measured, 2026-09-07 -- `decide :merge` rides the `:eligible-index`

Baseline `16f825b7`, the addendum's own close. Two code commits:
`24ed60a1` (the concern and its from-scratch law, `decide` untouched)
and `b8c8ff2d` (the repoint, `already-merged?` deleted, plus the
AGENTS.md rider). One index this time serving TWO views, of which site 5
repoints one -- `decide :bed-swap` is site 6 and its view arrives here
proven rather than arriving with its own session.

### Output identity

`bin/ground-truth-bracket 16f825b7 <sha>` reported IDENTICAL on all 38
digested roots at BOTH code commits (3 skipped, no `:ground-truth`
key), and the instrument's own zero at the clean tip did too. Both timed
cells reproduced their pre-change logs BYTE-FOR-BYTE -- `a7500-persons`
sha256 `3018299a…0d3bd3` over 167,197 events, `a2500-nopersons`
`c22d6573…a55208` over 43,103 -- and so did the top-of-decade cell,
`7d105743…78e0a` over 174,866,696 bytes and 431,677 events, the same
digest sites 4 and the 2026-09-05 measurement recorded.

The suite read 424 `Test results:` lines and 28,041 passes at both code
commits -- IDENTICAL COUNTS ACROSS THE REPOINT, which is what an
output-identical refactor looks like from here. The +2 lines and +106
passes against site 4's 422/27,935 are the new law namespace and
`apply-projection-test`'s three new negative assertions.

### Wall

| cell | before | after | delta |
|---|---|---|---|
| `a7500-persons` generate | 110.95 s | **96.98 s** | -13.97 s, **-12.6%** |
| `a2500-nopersons` generate | 26.97 s | **24.74 s** | -2.23 s, **-8.3%** |
| `a7500-persons` check | 46.38 s | 47.30 s | +0.92 s |
| `a2500-nopersons` check | 18.12 s | 17.93 s | -0.19 s |

The check column is the control: `replay-projection` declines this
index as it declines the other three, so the check phase should not
move, and it does not beyond run-to-run spread. Peak RSS FELL at both
generate cells (2,353 -> 2,010 MB; 969 -> 933 MB), which is the
expected direction for the first ADR-0180 index whose repoint deletes
an allocation as well as a scan.

**AND THE TOP OF THE DECADE.** One timed `a22500-nopersons` generate:
**396.18 s -> 250.62 s**, a further 1.58x over a byte-identical log, and
**1,472.56 s -> 250.62 s** across the whole program -- 5.9x. Peak RSS
3,575 MB against site 4's 3,599 MB.

### Profile

JFR at 7,500 with `:persons`, inclusive share, `--stack-depth 2048`,
and this is a MATCHED PAIR: the before half was recorded from a
worktree at `16f825b7` with the same driver and the same cell, so the
two columns are one scale rather than two.

| frame | before (7,145 samples) | after (6,208) |
|---|---|---|
| `decide :merge` | **12.40%** | **0.43%** |
| `decide :bed-swap` | 11.07% | 12.00% |
| `decide` (whole dispatch) | 37.30% | 28.46% |
| `fold/apply-events` | 20.35% | 23.02% |
| `person-simulator` | 18.77% | 21.25% |

`decide :bed-swap` is UNTOUCHED this session and its absolute sample
count fell with the phase (791 -> 745); its share rose because the
phase got shorter. It is now the recording's top project frame, which
is site 6 sitting in the instrument.

**HOW THE `decide` FRAMES WERE ATTRIBUTED, because they are gensym-named
and the post-program session resolved its two by hand.** A `defmethod`'s
compiled class carries the eval ordinal of the top-level form that
produced it, which shifts with the classpath -- but the GAPS between one
file's methods are fixed by that file, so a single offset maps a
`clojure -M:dev` resolution of all 32 `decide` methods onto a whole
recording. Both recordings resolved at offset **+2999** independently,
matching 21 and 24 of the 32 methods (the rest have zero samples), which
is the attribution checking itself rather than being asserted.

**The concern's own cost.** `profile-cell.sh`'s breakdown gains a row
for `update-eligible`, and both recordings were re-aggregated so the
pair carries it:

| concern | before, % of phase | after, % of phase |
|---|---|---|
| `:patient-state` (the `evolve` multimethod) | 3.83% | 4.45% |
| `:encounter-stamp` | 1.06% | 1.16% |
| `:cancel-index` (site 4) | 0.46% | 0.52% |
| `:bed-index` | 0.36% | 0.34% |
| **`:eligible-index` (site 5)** | **0.00%** | **0.21%** |
| `:board` (site 3) | 0.07% | 0.16% |
| `:boarder-index` (site 1) | 0.24% | 0.14% |

**Site 5 traded 12.40 points of the phase for 0.21.** ADR-0180's four
in-fold indexes together now cost 1.03% of this cell's generate phase.

**Allocation, from the same two recordings.** Estimated total
allocation 59.9 GB -> 54.1 GB, -9.7%.

| allocating method | before | after |
|---|---|---|
| `decide :merge` | 5.10 GB, **8.51%** | 0.14 GB, **0.26%** |
| `decide :bed-swap` | 6.87 GB, 11.46% | 7.47 GB, 13.82% |

`decide :bed-swap`'s absolute rise is the throttled estimator's spread
across two runs on code that did not change and a log that is identical
to the byte; recorded rather than explained away.

### The law, and the clause it was blind to

`ehrt.sim-engine.eligible-index-test` keeps both `decide` scans VERBATIM
and asserts them `=` -- vectors, ORDER included -- at every replay entry
of churn-bearing generated logs. Red->green, by rebinding the shipped
functions and re-running the namespace:

| mutation | failures |
|---|---|
| shipped | **0** |
| carrier grown from `{}` (array-map) | 1 |
| membership from the event, never evicts | 7 |
| merge view SORTED | 6 |
| swap flag ignores `:location` | 0 -> **3** |
| restored | **0** |

The last row is the finding. Dropping the `(some? (:location p))` term
failed ZERO assertions against the property as first written, because no
generated log reaches an `:admitted` patient without a location:
`evolve :admission`, `:transfer` and `:bed-swap` all write one and
`:discharge` clears the location and the status together. That makes the
term unfalsifiable by corpus, not unnecessary -- the index is specified
to equal the DEFINITION on every world, and the definition tests both
terms -- so the world is now constructed by hand
(`an-admitted-patient-with-no-location-is-in-the-merge-view-only`,
`c83ea961`) and the mutation fails 3.

The array-map row is honest about its own weakness: that mutation only
bites where the seed is ABSENT, and the trace seeds it, so the
generated property cannot see it. The dedicated
`the-carrier-is-a-hash-map-from-empty` is what catches it, by folding
one batch under each seed and comparing the two answers directly.

### What the gates cannot see, said before it is asked

The hasheq-collision hole is REAL at this program's scale and every
instrument used here is blind to it: the addendum measured the first
colliding patient-id pair at 45,000 arrivals, and the largest committed
cell is 22,500. `at-a-hasheq-collision-the-two-orders-diverge` builds
that pair by hand and pins what each side answers -- the definition in
registration order, the index in eligibility order -- so the divergence
is a recorded fact and not a discovery waiting for someone at 45,000
arrivals. Retiring it means SORTING the candidates, which moves every
churn-bearing golden root; that is `roadmap.md#determinism-hash-order-
dependence` and a declared oracle change, not a site session's call.
