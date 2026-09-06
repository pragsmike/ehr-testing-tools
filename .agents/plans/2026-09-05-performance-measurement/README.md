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

## Ward scale is a separate axis, and NOT for the reason first written

The session prompt specified the decade as 7,500 / 22,500 / 67,500
arrivals with the wards scaled ×1 / ×3 / ×9, for a stated reason: *so
no cell halts on `:capacity-exhausted`*.

**That reason was sound, and this directory's first cut said otherwise
on one cell's evidence.** The 7,500 census put peak ward census at
30.0% / 22.5% / 27.5% of the ×1 wards, which read as enormous headroom,
and the provenance's own design note — arrival RATE is fixed by
`:arrival-gap 2`, so census should be constant however many arrivals
follow — appeared to settle it. The 22,500 census settles it the other
way:

| ward | 2,500 | 7,500 | **22,500** |
|---|---|---|---|
| Emergency | 25.5% | 30.0% | **67.3%** |
| Surgery | 28.5% | 31.5% | **72.5%** |
| Medicine A | 16.7% | 23.3% | **48.3%** |

(`:persons`-off cells, so the three are one series.) **Census more than
doubles between 7,500 and 22,500**, and the fixed-rate argument does not
save it: the run is not in steady state at these lengths. 7,500
arrivals is 5.2 simulated days against a `:module-horizon-days` of
1,825, so the module cohort's long trajectories are still accumulating
and the census is still filling. Surgery at **72.5%** at 22,500 is the
last comfortable cell, and the prompt's pairing of 67,500 with ×9 wards
looks well judged rather than redundant.

**Nothing halted.** All eight cells exited 0, so every figure in
[`measurements.md`](measurements.md) is taken on a facility with room
to spare. What is retracted is the CLAIM that scaling was pointless,
not any measurement.

**The decade is still ×1, and the reason is the other one.** Ward scale
is not a free parameter: more beds is a different bed vocabulary, a
different `allocate` result, and therefore a different log —
`measurements.md`'s facility axis measures 167,197 / 167,184 / 167,212
events at ×1 / ×3 / ×9 on otherwise identical inputs. Three points
taken at three facility sizes are three experiments, not one experiment
at three sizes, and a log-log slope across them measures nothing in
particular. That argument never depended on the capacity one
(`rulings.md#R-stop-only-on-two-defensible-readings` — a mechanical
conflict with one defensible reading is fix-forward with disclosure).

Worth keeping in view for whoever takes the fix session: the ×3 and ×9
cells hold peak census at 69 and 70 against the ×1 cell's 66, so
**adding beds does not change how many patients are in them.** Census
here is demand-driven, and the ladder's headroom is the only thing ward
scale buys.

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
