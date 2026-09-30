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

## 8. Close

Pushed `3114dbfe..3f786c08`, seven commits. `bin/post-push-verify`:
remote tip matches HEAD, every commit message in range pure ASCII.
**CI run 34008092622 concluded `success` at
`3f786c08be80ba9af9053626a662b0051b6bbe93`.** No tag was paid — CI green
at the tip is the close marker.

## Correction 2026-09-30

One attribution in this record is wrong, and this section corrects it; the
sections above stand as written.

**Section 2(a) attributes the 22,500-arrival refusal to the pool.** It says
`:persons` held at 15,000 "broke the 22,500 cell" because the pool sat below the
arrival count. It did not. That refusal was the same-instant encounter-opener
race that `c282409f` fixed on 2026-09-10
(`.agents/session-records/2026-09-10-same-instant-openers.md`), and the pool was
not the cause.

**The evidence is a bracket on the exact refused configuration.** The probe ran
the first-cut cell `cell-x3-persons.edn` (x3 wards, `:persons {:count 15000}`),
which `d69349bf` deleted. It was recovered byte for byte from `3985e828` (sha-256
`c17d3a8c...`), and `config.edn` has not changed since that commit. It was then
re-run at `--seed 20260824 --patients 22500 --churn --format ground-truth`,
`-Xmx8g`, with `/usr/bin/time -v`:

| tree | exit | result | receipt under `.agents/plans/2026-09-05-performance-measurement/raw/` |
|---|---|---|---|
| `913be926` (the fix's parent) | **2** | `:self-check-failed`, six violations on one patient id | `bracket-a22500-w3-persons-15000-913be926.{time,refusal.edn}` |
| `c282409f` (the fix) | **0** | log sha-256 `c0e172bc...` | `bracket-a22500-w3-persons-15000-c282409f.{time,sha256}` |
| `dac3d9c8` (today's tip) | **0** | log sha-256 `c0e172bc...`, byte-identical to the row above | `rerun-a22500-w3-persons-15000-xmx8g.{time,sha256}` |

The refusal at `913be926` has this record's own signature: six violations, one
patient (`PID-007317-...`): a schema-invalid `:discharge`,
`admission-only-when-no-open-encounter`,
`discharge-closes-an-open-encounter`,
`every-encounter-is-opened-and-closed-or-still-open`, and
`clinical-content-only-when-admitted` twice. The one-commit fix clears it, and
nothing after that commit moves a byte of this cell.

**The x1 cell the 2026-09-30 prompt named passes as well.**
`cell-a22500-persons.edn` with its pool cut to 15,000 (a scratch copy, sha-256
`62c16a29...`, NOT committed) exits 0 at `dac3d9c8` with log sha-256
`db05a7b0...` (`rerun-a22500-persons-15000-xmx8g.{time,sha256}`). The prompt named
this cell as the rerun, but this x1 cell was never refused. That is why the x3
bracket above was run as well, and the x3 bracket is what carries the correction.

**What survives.** The 2x rule is a FIDELITY rule and nothing more: a pool smaller
than the arrival count collides arrivals onto already-registered people, which
`config.edn`'s own comment says and which stays true. It is not a correctness
precondition, and a run with too small a pool does not refuse because of it.
`config.edn`'s comment claims fidelity only and is unchanged. Two sentences in
this directory's `README.md` and `derive-cells.sh`'s header ("HOLDING `:persons`
AT 15,000 BROKE THE RUN") repeat the wrong attribution. The README gains a pointer
to this section, and the script's comment is left for an errata sweep.
