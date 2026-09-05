# 2026-09-05 — performance measurement

**Nothing here changes engine code.** This directory is a measurement:
what `ehrt sim run --format ground-truth` and `ehrt sim check` actually
cost at scale, measured 2026-09-05, so that
[`roadmap.md`](../roadmap.md)'s `performance-residual-sites` row can be
ranked against figures rather than against inspection
(`rulings.md#R-measure-first`: *measurements before the fix; no engine
change this session*).

**It exists because the figures it replaces died in scratch.**
ADR-0169's generate slope **1.786** and check slope **1.814** were
measured 2026-08-29 on a configuration that no longer exists, and the
`docs/consuming-ground-truth.md#scale` paragraphs that still report them
say so in their own words. Everything in this directory is therefore
COMMITTED — the cell configs, the driver, the census script and the raw
`/usr/bin/time -v` output — which is `R-commit-cells`, and it is the
whole reason the directory is not a scratch tree.

## What the cells are

Two axes, kept apart, because they are two different questions.

**THE DECADE** — one facility, three arrival counts, `:persons` on and
off. This is the axis a slope is taken over.

| file | arrivals | wards | `:persons :count` |
|---|---|---|---|
| `cell-a2500-persons.edn` | 2,500 | ×1 | 5,000 |
| `cell-a2500-nopersons.edn` | 2,500 | ×1 | **absent** |
| `cell-a7500-persons.edn` | 7,500 | ×1 | 15,000 — **byte-identical to the provenance** |
| `cell-a7500-nopersons.edn` | 7,500 | ×1 | **absent** |
| `cell-a22500-persons.edn` | 22,500 | ×1 | 45,000 |
| `cell-a22500-nopersons.edn` | 22,500 | ×1 | **absent** |

**THE FACILITY AXIS** — one arrival count, three ward scales. What the
ward count costs on its own, which is only answerable with the arrival
count held still.

| file | arrivals | wards |
|---|---|---|
| `cell-a7500-persons.edn` | 7,500 | ×1 (the same file; not re-emitted under a second name) |
| `cell-a7500-w3-persons.edn` | 7,500 | ×3 |
| `cell-a7500-w9-persons.edn` | 7,500 | ×9 |

The provenance is
[`demos/scenarios/dense-7500/config.edn`](../../../demos/scenarios/dense-7500/config.edn),
and every one of the eight is DERIVED from it by
[`derive-cells.sh`](derive-cells.sh) rather than authored. Run it from
anywhere and it rewrites all eight:

```bash
.agents/plans/2026-09-05-performance-measurement/derive-cells.sh
```

The script CHECKS each rule rather than asserting it — the `:persons`
count, the `:persons` line present or gone, exactly one line of
difference between a pair, four ward lines per cell, Emergency scaled
by the right factor — and the strongest of those checks is that
`cell-a7500-persons.edn` is `cmp`-identical to the provenance.

## `:persons :count` is 2× the arrival count, and that is not optional

The provenance makes it a RULE, and says in its own comment what the
rule is for: a pool too small *would collide nearly every arrival onto
an already-registered person*.

**This directory learned that the hard way, and the evidence is
committed** ([`raw/probe-a22500-w3-persons-15000.time`](raw/)). Its
first cut held `:persons` at the literal 15,000 across the whole
decade, on the theory that a constant demographic term is a cleaner
thing to have underneath a slope over arrivals. At 22,500 arrivals that
puts the pool BELOW the arrival count, and the run refused after
**19m38s** with six self-check violations on one patient id — a
`:discharge` whose `:location` fails the schema, a second `:admission`
over an already-open encounter, and the three encounter-bracket
invariants that follow from it.

Worth recording on its own account: **nothing was silent.** The run
self-checked, emitted `{:status :error, :category :self-check-failed}`
naming all six violations with the patient id and log index, and exited
2. An out-of-contract configuration produced a refusal and no corpus,
which is the behaviour the self-check exists for.

## Ward scale is a separate axis, and the census is why

The session prompt specified the decade as 7,500 / 22,500 / 67,500
arrivals with the wards scaled ×1 / ×3 / ×9, for a stated reason: *so
no cell halts on `:capacity-exhausted`*. **That condition is already
met at ×1**, and [`measurements.md`](measurements.md) carries the
census that proves it — peak census 30.0% / 22.5% / 27.5% of the ×1
wards, with Medicine B never used at all.

Scaling with the arrivals would also have cost the measurement its
meaning. `:arrival-gap 2` fixes the arrival RATE and only `--patients`
grows, so concurrent census does not grow across the decade — the
provenance was built that way on purpose and says so. And ward scale is
not a free parameter: more beds is a different bed vocabulary, a
different `allocate` result and therefore a DIFFERENT LOG. Three points
at three facility sizes are three experiments, not one experiment at
three sizes.

So the decade is ×1 throughout, and the ward scale is measured at one
fixed arrival count where the answer means something
(`rulings.md#R-stop-only-on-two-defensible-readings` — a mechanical
conflict with one defensible reading is fix-forward with disclosure).

## Running one

```bash
.agents/plans/2026-09-05-performance-measurement/run-cells.sh \
  a7500-persons \
  .agents/plans/2026-09-05-performance-measurement/cell-a7500-persons.edn \
  7500 ~/perf-out
```

`<out-dir>` is a path OUTSIDE the worktree and must stay one: a
7,500-arrival ground-truth log is **67 MB** and the cells above it are
multiples of that. Only the timings, the digests and the per-kind
census come back into the repo, under [`raw/`](raw/).

The driver's own header states what its warm-up does and does not warm
— every timed run is a fresh JVM, so a warm-up cannot warm the JIT of
anything measured afterwards, and a 750-arrival warm-up therefore warms
the page cache exactly as well as a full repeat of the cell would.

## The other two instruments

[`profile-cell.sh`](profile-cell.sh) flight-records a cell's generate
and check phases and aggregates `jdk.ExecutionSample` two ways — self
time by top frame, and INCLUSIVE share for a list of named sites, which
is the one that answers whether `occupancy-board` still shows and at
what share. Zero new dependency: the recorder ships in the JVM and the
reader is `jfr`, a JDK 21 tool.

[`scenario-census.sh`](scenario-census.sh) runs
[`census-src/ehrt/perf/scenario_census.clj`](census-src/ehrt/perf/scenario_census.clj)
over a cell's log — results landing after their subject moved, was
discharged or was merged; merges absorbing a bed-holder; cancels
reinstating a bed; peak ward census against capacity. It is pure over
the log, so the same log yields the same counts.

## The measurements themselves

Are in [`measurements.md`](measurements.md). The raw
`/usr/bin/time -v` files, per-kind censuses and profile tables those are
computed from are in [`raw/`](raw/), one per timed JVM.
