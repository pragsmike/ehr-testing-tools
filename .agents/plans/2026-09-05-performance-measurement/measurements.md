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

## An unexplained 7-event divergence, recorded and not chased

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

<!-- PROFILES -->

## Scenario census

<!-- CENSUS -->
