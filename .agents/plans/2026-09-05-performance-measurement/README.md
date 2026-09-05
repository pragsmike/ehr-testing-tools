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

| file | ward scale | `:persons` |
|---|---|---|
| `cell-x1-persons.edn` | ×1 — **byte-identical to the provenance** | `{:count 15000 :years 20}` |
| `cell-x3-persons.edn` | ×3 | same |
| `cell-x9-persons.edn` | ×9 | same |
| `cell-x1-nopersons.edn` | ×1 | **absent** |
| `cell-x3-nopersons.edn` | ×3 | **absent** |
| `cell-x9-nopersons.edn` | ×9 | **absent** |

The provenance is
[`demos/scenarios/dense-7500/config.edn`](../../../demos/scenarios/dense-7500/config.edn),
and every one of the six is DERIVED from it by
[`derive-cells.sh`](derive-cells.sh) rather than authored. Run it from
anywhere and it rewrites all six:

```bash
.agents/plans/2026-09-05-performance-measurement/derive-cells.sh
```

Two dials move and nothing else does. **Ward scale** multiplies every
`:beds N :surge-slots M` pair in the facility block, so a cell's ladder
has headroom its arrival count cannot exhaust — `:capacity-exhausted`
HALTS a run outright (ADR-0174), and a halted run measures the error
path instead of generation. **`:persons`** is present verbatim or its
one line is deleted, which is the same anchored whole-line cut
`config-nobed.edn` makes against `:bed-cycle`.

The script CHECKS each rule rather than asserting it — four ward lines
per cell, the `:persons` line present or gone, exactly one line of
difference between a pair — and the strongest of those checks is that
`cell-x1-persons.edn` is `cmp`-identical to the provenance, which is
what proves the ward rewrite is a pure multiply and not a reformat
riding along with it.

## What is NOT a dial, and why

**`:persons :count` stays at 15,000 in every persons-on cell, including
the ones above 7,500 arrivals.** The provenance's own comment makes
that value a RULE — *twice the arrival count* — so honouring the rule
would put 45,000 people under the ×3 cell and 135,000 under the ×9.
This session holds it FIXED instead, deliberately, and the reason is
that the demographic timeline is the one large cost in the run that
does not scale with arrivals: fixing it makes it a constant additive
term across the decade, which is exactly what a slope over arrivals
wants underneath it. The persons-off variant then removes the term
altogether, and the pair of slopes is what says how much of the curve
was ever arrival-driven.

This is a DISCLOSED departure from the provenance's stated rule, not an
oversight, and it is why these cells live here rather than in
`demos/scenarios/`. A cell file is a measurement instrument; it is not
a scenario anyone should generate a corpus from.

## Running one

```bash
.agents/plans/2026-09-05-performance-measurement/run-cells.sh \
  x1-persons .agents/plans/2026-09-05-performance-measurement/cell-x1-persons.edn 7500 ~/perf-out
```

`<out-dir>` is a path OUTSIDE the worktree and must stay one: a
7,500-arrival ground-truth log is **67 MB** and the cells above it are
multiples of that. Only the timings, the digests and the per-kind
census come back into the repo, under [`raw/`](raw/).

The driver's own header states what its warm-up does and does not warm
— every timed run is a fresh JVM, so a warm-up cannot warm the JIT of
anything measured afterwards, and a 750-arrival warm-up therefore warms
the page cache exactly as well as a full repeat of the cell would.

## The measurements themselves

Are in [`measurements.md`](measurements.md), with the cell table, the
slopes, the JFR profiles and the scenario census. The raw
`/usr/bin/time -v` files those are computed from are in [`raw/`](raw/),
one per timed JVM.
