# 2026-09-05 — performance measurement: generate vs check slopes, profile, scenario census

Measurement session. **No engine code changed**, which is
`rulings.md#R-measure-first` taken as the session's own scope. Ceremony
mode: **R30** (commit at each checkpoint). Prompt archived at
[`../prompts/2026-09-05-performance-measurement.md`](../prompts/2026-09-05-performance-measurement.md).
Everything measured is committed under
[`../plans/2026-09-05-performance-measurement/`](../plans/2026-09-05-performance-measurement/)
— cells, driver, profiler, census script and raw `/usr/bin/time -v`
output — which is `R-commit-cells`, and the reason for it is that the
2026-08-29 figures this session replaces died in session scratch.

Rulings in force: **R-measure-first**, **R-commit-cells**,
**R-summarize-widen**, **R-edit** (standing), **R-cap** (standing).

## 0. Preflight

`bin/preflight` ran FIRST and exited **0 with no findings**: last five
CI runs on `main` all green; edit root `/home/mg/src/ehr-testing-tools`,
not under `/mnt/`; `core.fileMode` true; `core.ignorecase` unset;
working tree clean including untracked; local HEAD `3114dbfe` equal to
`origin/main`. One DISCLOSED line, not a finding: HEAD is not tagged
`stable-*`. No tag was paid.

## 1. Asks to disposition

| ask | disposition |
|---|---|
| Step 1: six cells from dense-7500's `config.edn`, two variants, README naming the derivation | **DONE, THEN CORRECTED TO EIGHT** — `d69349bf`; see section 2 |
| Step 1 gate: `sim run` of the smallest cell exits 0 | **GREEN** — 246.20 s, 2,265 MB RSS, 167,197 events |
| Step 2: driver, warm-up + two timed generate and two timed check JVMs per cell, `time -v`, event count, per-kind census, heap max | **DONE** — `6493d69e`, `run-cells.sh`, 8 cells |
| Step 2 invariant: both timed runs of a cell byte-identical | **HELD, 8 of 8** — `raw/*.sha256`, all `IDENTICAL=yes` |
| Step 2 gate: the table has all cells | **GREEN** — `measurements.md`, plus a facility axis the prompt's ward dial became |
| Step 3: JFR profile of the 67,500 generate and check runs | **DONE AT 22,500, NOT 67,500** — 67,500 is unreachable and the bound is measured; section 3 |
| Step 3: top-15 self frames, which ADR-0169 sites appear, with share | **DONE** — `c6a64683`, four recordings, three aggregations each |
| Step 3: commit `.jfr` only if < 5 MB | **NOT COMMITTED** — 197 MB and 25 MB; the aggregated tables are |
| Step 4: committed Clojure script over `replay`'s projection, five columns, per cell | **DONE** — `ab7a4ff0`, all 8 cells |
| Step 4 invariant: pure over the log | **HELD** — no clock, no draw; re-running a cell reproduces its counts |
| Step 4 gate: merge column zero on every cell, or name the first witness | **ZERO ON EVERY CELL**, at 533,147 events — section 4 |
| Step 5: `future-features.md` summarize entry widened, paid by compaction in the same file | **WIDENED; PAID IN PART** — net **+7 lines**, disclosed in section 6 |
| Step 5: roadmap P1 row re-ranked with the step-2/3 figures | **DONE** — the "NOT re-profiled" sentence is gone and four sites are ranked |
| Step 5 gate: `make test` | see section 5 |
| Step 6: record, prompt archive, close ceremony, push, CI, close marker | this record |

## 2. Three corrections this session made to its own work

Each was caught by running something, not by rereading something, and
each is committed with the evidence rather than quietly fixed.

**(a) `:persons` held at 15,000 broke the 22,500 cell.** The first cut
of the cells held the provenance's literal `:persons {:count 15000}`
across the whole decade, reasoning that a constant demographic term is
a cleaner thing to have under a slope over arrivals. The provenance
makes that value a RULE — twice the arrival count — and says in its own
comment what the rule prevents: a pool too small *would collide nearly
every arrival onto an already-registered person*. At 22,500 arrivals
the pool is BELOW the arrival count, and the run refused after
**19m38s** with six self-check violations on one patient id. The
reasoning was mine and it was wrong against a comment I had already
read. `raw/probe-a22500-w3-persons-15000.time` is that run.

**Worth recording separately: nothing was silent.** `sim run` emitted
`{:status :error, :category :self-check-failed}` naming all six
violations with the patient id and the log index, and exited 2. An
out-of-contract configuration produced a refusal and no corpus.

**(b) "Concurrent census does not grow across the decade" was wrong.**
Written into `README.md` and `derive-cells.sh` on the 7,500 census (peak
30.0% / 22.5% / 27.5% of the ×1 wards) plus the provenance's fixed-rate
design note. The 22,500 census refutes it — Emergency **67.3%**, Surgery
**72.5%** — because 7,500 arrivals is 5.2 simulated days against a
`:module-horizon-days` of 1,825, so the run is nowhere near steady state
and the module cohort is still accumulating. **The prompt's ward
headroom was a real precaution, not a redundant one.** Retracted in
`ab7a4ff0`; no measurement moved, because nothing halted.

**(c) `jfr print` truncates every stack to five frames by default.** The
first aggregation reported `replay` at **0.00%** of the check phase, and
"arc 0 removed it entirely" was an available and completely wrong
reading. `--stack-depth 2048` fixes it and the true figure is **50.97%**.
A separate cap — the RECORDING's own `stackdepth`, default 64 — was
tested by re-recording the check phase at 2048; it made no difference
here (50.98% vs 50.97%), so the default was adequate. Both traps fail
silently toward declaring outer frames free.

## 3. What the measurement says

**Check is no longer the quadratic.** Startup-corrected decade slope
**1.03–1.04** against ADR-0169's **1.814**, in both `:persons` variants
and every adjacent pair. `occupancy-within-capacity`, 54.9% of the phase
for ADR-0169, is **4.08%**. Arc 0 and `642d70a` both landed.

**Generate is the quadratic and it is steepening.** Decade aggregate
**1.851**, close enough to ADR-0169's 1.786 that the old number was not
wrong — but it averages a curve whose local slope rises from **1.59** at
the bottom to **2.12** at the top, so one exponent understates the next
decade.

**The roadmap's replay claim is confirmed and understated**: the 14
`engine/replay` calls are **50.97%** of check, not ~40%. But check is
130.57 s against generate's 2,329.46 s — **5.3% of the pair's wall** —
so the whole win there is ~65 s.

**The three sites ADR-0169 declined to fix are the three that are
rising**, at two to four times their inspection estimates:
`waiting-boarder` **30.54%** (rowed at ~7.9%), `occupancy-board`
**17.11%** (8.1%), `last-uncancelled-index` **10.78%** (5.9%).

**A site on no list is the second largest.** `run/select-person` is
**19.98%** of generate — a `filterv` over the WHOLE person population
per arrival, O(arrivals²) under the provenance's own 2× `:persons` rule.
ADR-0169 could not have seen it: it profiled a configuration in which
the person layer did not exist. **It needs a row before it needs a fix.**

**67,500 arrivals is unreachable on shipped defaults**, and both bounds
are measured rather than extrapolated: 3,973 MB peak RSS against a
3.88 GB `MaxHeapSize`, and ~9.4 hours per generate JVM at the measured
top-of-decade slope. The decade recorded is 2,500 / 7,500 / 22,500 — the
same 9× span one decade lower, anchored on 7,500 so it reconciles with
the published Scale table's own cell.

## 4. The merge column, and why the zero is the finding

`:result-after-merge` is **0 on all eight cells**, including one with
**533,147 events, 2,850 merges and 167 merges that absorb a
bed-holding patient**. That is roughly 12× the largest population either
`.agents/plans/2026-09-01-event-mutation-population-ledger.md` or
`.agents/plans/2026-09-05-adr-0179-merge-census.md` reached, and it does
not produce one witness.

**So the gap is structural, not a matter of scale**, and generating a
bigger corpus is not the way to close it. Named, not diagnosed —
`R-measure-first` scopes this session to measurement, and which
mechanism makes the two mutually exclusive wants a reading of the merge
decide path rather than another run.

## 5. Gates

| step | gate | result |
|---|---|---|
| 1 | `sim run` of the smallest cell exits 0 | **0** — 246.20 s, 2,265 MB peak RSS, 167,197 events |
| 2 | the table has all cells | **8 of 8**, and every `sim check` in it exited **0** |
| 2 | both timed runs of a cell byte-identical | **8 of 8 `IDENTICAL=yes`** (`raw/*.sha256`, sha-256 of the shipped writer's own bytes) |
| 3 | profile table present for both phases | **4 recordings**, three aggregations each |
| 4 | merge column zero on every cell, or first witness named | **zero on all 8**, at up to 533,147 events |
| 4 | census pure over the log | held — no clock, no draw |
| 5 | `make test` | **GREEN** — 414 `Test results:` lines, **27,667 assertions, 0 failures, 0 errors** |

`make state-derived` was run before `make test` and again before the
final push, so `.agents/state-derived.md` and both generated `INDEX.md`
files carry this session's own additions.

Two instrument bugs were found and fixed DURING the session, both in
this directory's own scripts and neither in shipped code: the `local`
expansion order that killed `profile-cell.sh` after a good recording,
and `jfr print`'s five-frame default. Section 2(c) has the second; both
are commented at the site so the next reader does not re-find them.

## 6. Findings, for a ruling

1. **`run/select-person` is unrowed and is 19.98% of generate.** Every
   other site in the ranking has a home in ADR-0169; this one has none.
2. **A 7-event divergence between two invocations on identical inputs.**
   `demos/scenarios/dense-7500/README.md` reports 167,190 events for
   `corpus generate sim`; `sim run --format ground-truth` reports
   **167,197** on the same seed, the same flags, a `cmp`-identical
   config and the same counting tool. Recorded, not chased.
3. **`R-summarize-widen` was paid in part.** `docs/future-features.md`
   is **+7 lines** net (193 → 200) after compacting its two intro
   paragraphs, the Scale-ergonomics preamble, two `*Today:*` paragraphs
   and the Streaming entry. I stopped rather than shave the
   layer-boundary and content-fault prose, which is tight and
   load-bearing. **The `:docs` reading set is unmoved at 785/785**:
   its five paths are `AGENTS.md` (375),
   `components/docs-tooling/src/ehrt/docs_tooling/interface.clj` (19),
   `docs/dev/architecture.md` (187), `docs/dev/README.md` (58) and
   `.agents/skills/build-session/SKILL.md` (146) — and
   `docs/future-features.md` is not among them, so the ruling's stated
   decrease-only mechanism is not engaged by this edit. Flagged rather
   than assumed.
4. **`.agents/plans/`'s index gate is file-only.** This session's
   deliverable is the first DIRECTORY under `.agents/plans/`, and
   `ehrt.docs-tooling.index-completeness` enumerates that directory with
   `real-files` and checks both directions — so a star bullet naming a
   directory reads as a ghost, and the directory is named in prose
   instead. Not a defect this session should fix unilaterally.

## 7. Background processes

Five background jobs were started (one probe run, one eight-cell driver,
three profile/aggregate batches, one census batch, one `make test`).
**All five ran to completion and none was left running**; no `sleep`
waiter was hand-rolled against a job, and each was waited on through the
harness. No server, watcher or daemon was started at any point.
