# 2026-09-05 — the 7-event divergence: one path or one version?

Derivation session. **No engine code changed**, which is the prompt's
own fence. Ceremony mode: **R30** (commit at each checkpoint). Prompt
archived at
[`../prompts/2026-09-05-7-event-divergence.md`](../prompts/2026-09-05-7-event-divergence.md).
The derivation is committed at
[`../plans/2026-09-05-7-event-divergence.md`](../plans/2026-09-05-7-event-divergence.md).

Rulings in force: **R-derive-first** (*"diff the two paths at ONE tip
before any claim about either"*), **R-edit**, **R-cap**, **R-pins**
(standing).

## 0. Preflight

`bin/preflight` ran FIRST and exited **0 with no findings**: edit root
`/home/mg/src/ehr-testing-tools`, not under `/mnt/`; `core.fileMode`
true; `core.ignorecase` unset; working tree clean including untracked;
local HEAD `28bf36b4` equal to `origin/main`. Two DISCLOSED lines,
neither a finding: the CI run at `28bf36b4` was still PENDING (AR-CI-4
— not awaited, not counted red; the four before it are green), and HEAD
is not tagged `stable-*`. No tag was paid.

## 1. Asks to disposition

| ask | disposition |
|---|---|
| Step 1: A = `sim run --format ground-truth`, B = `corpus generate sim --out-dir`'s ground-truth artefact, one tip, one JVM each, both counted by `bin/event-census` | **DONE** — both exit 0, both **167,197 events, 28 kinds** |
| Step 1 channel expectation: the out-dir carries a ground-truth file the census can read | **CONFIRMED** — `events.edn`, `generators.clj:241` |
| Step 2: verdict, with the diff as evidence | **VERSION DELTA** — A is B plus one trailing newline; §2 |
| Step 2 (version-delta arm): regenerate at `007deea6` in a second clone, diff per event, first divergent index | **DONE** — 167,190, the README's own figure to the event; first divergent index **1,180**; §3 |
| Step 2: the delta explained entirely by ADR-0179's bed release | **PARTLY — and the correction is the session's headline.** R-bed explains four of the five moving kinds; the fifth is **R-queue**, which ADR-0179 recorded as having an empty population in every corpus it could see. §4 |
| Step 3: re-measure dense-7500's README rows and the Scale table at the tip with the README's own protocol | **DONE, AND WIDENED** — all four cells, both derived-arithmetic paragraphs, and the five-column referential table; §5 and deviation D2 |
| Step 3: add ADR-0179's declared-oracle-change list the roots it did not have, as an addendum | **DONE** — `notes/adr/0179-*.md`, *Addendum, 2026-09-06* |
| Step 3 gate: `make test` | **GREEN** — §6 |
| Fence: derivation only, no engine change | **HELD** — no `components/*/src` or `bases/*/src` file is touched by either commit |

## 2. Step 1 — the two paths do not diverge, and the law holds

At `28bf36b4`, seed 20260824, `--patients 7500 --churn`, against
`demos/scenarios/dense-7500/config.edn`, one JVM per invocation:

| | invocation | bytes | sha256 | census |
|---|---|---:|---|---|
| **A** | `sim run --format ground-truth` | 67,324,581 | `3018299a…` | 167,197 events, 28 kinds |
| **B** | `corpus generate sim --out-dir` → `events.edn` | 67,324,580 | `09695985…` | 167,197 events, 28 kinds |

`cmp` says they differ; the difference is **one byte**. A's first
67,324,580 bytes hash to B's whole-file digest — **A is B plus a
trailing newline**, the CLI writing a line to stdout against
`generators.clj:241`'s `(spit ... (pr-str ground-truth))`. That is the
documented envelope difference step 1 allowed for, and the only one.

**ADR-0175 section 4's emission-does-not-move-ground-truth law holds on
this configuration.** Had step 1 gone the other way the session would
have stopped there; it did not, so step 2 was live and step 3 followed.

## 3. Step 2 — the delta is ADR-0179, derived and not attributed

`007deea6` (the commit before ADR-0179's first code commit), in a
**second clone** — `~/src/ehr-7event-007deea6`, `git repack -a -d` plus
the alternates file removed so it shares no object store, not a
worktree — with that checkout's `config.edn` `cmp`-identical to the
tip's. Between the two commits the only `components/`/`bases/` diff is
ADR-0179's own.

**167,190 events**, which is the README's own figure to the event, and
reached from the OTHER path (`sim run`, not `corpus generate`) — a
second, independent witness to §2. Per kind:

| kind | `007deea6` | `28bf36b4` | delta |
|---|---:|---:|---:|
| `:result-available` | 10,253 | 10,274 | **+21** |
| `:cancel-transfer` | 2,125 | 2,146 | **+21** |
| `:transfer` | 7,338 | 7,340 | **+2** |
| `:bed-status-change` | 41,415 | 41,399 | **-16** |
| `:step-rejected` | 200 | 179 | **-21** |
| the other 23 kinds | — | — | **0** |

**165,918 of the 167,190 compared positions differ; the first is index
1,180** — one re-allocated bed re-indexes every later bed id, the same
shape ADR-0179 measured on `encounter-horizon` (110 of 173 positions
from three added events).

## 4. The finding: R-queue is not blind here

ADR-0179, verbatim: *"The oracle is BLIND to R-queue … Zero absorbed
patient-ids had a follow-up pending, in every corpus this repository
gates."* True as scoped, and `dense-7500` is not in that scope —
`bin/demo-exerciser-dense-7500` is wired into **neither `make test` nor
`make integration`**, which is also why the README's figures could go
stale for two days without a red gate.

| | `007deea6` | `28bf36b4` |
|---|---:|---:|
| `:order-placed` | 10,274 | 10,274 |
| `:result-available` | **10,253** | **10,274** |
| results whose cited order names a DIFFERENT subject | **0** | **21** |
| …of those, resolved by a `:merge` at `t` ≤ the result's `t`, survivor = the result's subject | 0 | **21** |

All 21 satisfy R-inv's condition exactly. **The 750-arrival cell is the
cleanest witness in the tree**: `:order-placed` 1,028 on both sides,
nothing else in the log moving at all, and its entire three-event delta
is three recovered results. All four committed cells become
order/result-equinumerous and none of them was before — 21, 20, 70 and
3 results recovered.

This is not a contradiction of ADR-0179 and is not a defect: it is the
ruling working, on a population the ADR could not see. Recorded as its
own dated addendum on that ADR rather than as a correction to it.

## 5. Step 3 — the re-measurement

Protocol as the README states it and as `2026-09-04-dense-7500-scale-cell-b`
ran it: one warm-up run, then two timed runs per cell, one JVM per run,
a fresh spool target per run, seed 20260824, `/usr/bin/time -v` around
each, cells strictly sequential. **Every one of the eight timed runs
exits 0, and both runs of every cell produced identical event and
message counts.**

### 5a. Per-run appendix

| cell | config | arrivals | run | exit | process wall | peak RSS | events | messages |
|---|---|---|---|---|---|---|---|---|
| 1 | `config.edn` | 7,500 | 1 | 0 | 280.31 s | 2,499.0 MB | 167,197 | 222,819 |
| 1 | `config.edn` | 7,500 | 2 | 0 | 276.97 s | 2,330.1 MB | 167,197 | 222,819 |
| 2 | `config-nobed.edn` | 7,500 | 1 | 0 | 234.64 s | 2,054.6 MB | 125,642 | 165,466 |
| 2 | `config-nobed.edn` | 7,500 | 2 | 0 | 227.65 s | 2,301.2 MB | 125,642 | 165,466 |
| 3 | `config-bare.edn` | 7,500 | 1 | 0 | 139.40 s | 1,737.9 MB | 100,868 | 65,457 |
| 3 | `config-bare.edn` | 7,500 | 2 | 0 | 144.83 s | 1,822.7 MB | 100,868 | 65,457 |
| 4 | `config.edn` | 750 | 1 | 0 | 54.38 s | 1,217.4 MB | 33,306 | 40,291 |
| 4 | `config.edn` | 750 | 2 | 0 | 54.36 s | 1,238.1 MB | 33,306 | 40,291 |

Host sampled at every cell boundary: 14 GiB free throughout,
one-minute load average 0.89 to 1.63, all of it this session's runs.

### 5b. The means, which are what the docs now carry

| cell | arrivals | events | messages | msg/event | process wall | peak RSS |
|---|---|---|---|---|---|---|
| `config.edn` | 7,500 | 167,197 | 222,819 | **1.3327** | 278.64 s | 2,415 MB |
| `config-nobed.edn` | 7,500 | 125,642 | 165,466 | **1.3170** | 231.14 s | 2,178 MB |
| `config-bare.edn` | 7,500 | 100,868 | 65,457 | **0.6489** | 142.12 s | 1,780 MB |
| `config.edn` | 750 | 33,306 | 40,291 | **1.2097** | 54.37 s | 1,228 MB |

### 5c. What moved in the docs

- `demos/scenarios/dense-7500/README.md` — the four-cell table; the
  protocol paragraph (date, tip, and the warm-up sentence corrected to
  what both sessions actually did); a new paragraph naming ADR-0179 as
  the cause with all four deltas; the bed-cycle arithmetic (41,555
  events, **24.9%** of the log, 57,353 messages, **1.3802** each); the
  messages-per-event pair (1.2097 → 1.3327); and the five referential
  carrier columns, of which **A moved 2,230 → 2,251 and B1 10,253 →
  10,274** while B2, C and D did not.
- `docs/consuming-ground-truth.md` — the Scale table's three rows and
  its basis sentence. The ADR reference goes in a footnote definition,
  not in prose: `no-visible-adr-token-in-prose-test` caught the first
  attempt, which is the gate doing its job.
- `notes/adr/0179-merge-transfer-semantics.md` — *Addendum,
  2026-09-06*: the declared list was oracle-scoped by construction, and
  R-queue's first non-empty population.
- `.agents/plans/2026-09-05-performance-measurement/measurements.md` —
  the "unexplained divergence" section retitled RESOLVED with a pointer;
  the section itself left exactly as that session wrote it.

## 6. Gates

| gate | result |
|---|---|
| `bin/preflight` | **0**, no findings (§0) |
| Step 1: both invocations exit 0 | **GREEN** |
| Step 2: the record states the verdict with the diff as evidence | **GREEN** — VERSION DELTA |
| `index-completeness-test` after the plan file landed | **GREEN** — 6 tests, 47 assertions |
| `link-footnote-gate-test` | RED on the first docs edit, **GREEN** after the footnote |
| `make docsgen` diff | only `.agents/state-derived.md`, `:onboarding` actual 1468 → 1469, headroom 62 → **61** |
| `make test` | **GREEN** at the step-3 tree -- every suite 0 failures, 0 errors, exit 0 |

## 7. Deviations, disclosed

**D1 — one warm-up run, not one per cell.** The README says "warm-up
plus two timed runs per cell"; both this session and cell-b ran ONE
warm-up for the battery. Here it was `bin/demo-exerciser-dense-7500`,
which does the full 7,500 generate and the 750 run, and which **exited
1** — at the witnessed-figures step, on the README's own stale 222,748,
and nowhere else, which is exactly where it had to fail before the edit
it precedes. Taken as the warm-up on the substance and disclosed rather
than described as a pass. The README's protocol sentence now says what
was actually done.

**D2 — scope widened past "three README rows and the Scale table".**
The 750 row, both derived-arithmetic paragraphs and the referential
table all carry figures from the same 2026-09-04 measurement, and two
of the referential columns moved. Leaving them stale in a file being
re-witnessed in the same commit was not defensible; fixed, and named
here.

**D3 — the baseline the prompt named is not the basis the README had.**
The README was measured 2026-09-04 at HEAD; `007deea6` is a day later,
and ADR-0178 (`:window-close-t` absent, never nil) landed in between,
touching `decide.clj` and the event schema. Rather than assume it moved
nothing, **all four cells were regenerated at `007deea6` and all four
reproduced the README's figures to the event** — so every delta is
derived, and ADR-0178's count-neutrality on this scenario is a
measurement rather than an inference.

**D4 — peak RSS moved more than the counts did.** Cell 1 reads 2,415 MB
against 2026-09-04's 2,209 MB, on a figure whose own two runs here
differ by 169 MB. Reported as measured, not investigated: it is a GC
high-water mark under a 3.88 GB default heap, and nothing in this
session's scope explains or needs it.

## 8. Findings, one line each

- **`bin/demo-exerciser-dense-7500` is in neither `make test` nor
  `make integration`.** It is the only gate over that README's figures,
  it re-derives them from the README at runtime rather than hard-coding
  them, and it takes ~340 s. That is why an engine change could move
  four committed cells and no gate went red. Naming it, not fixing it:
  wiring a six-minute generate into a suite is a cost decision.
- **`docs/consuming-ground-truth.md`'s Scale table has no gate at all** —
  it restates three of the README's rows and nothing checks the two
  agree.

## 9. Background processes

Five long-running jobs, all foreground-waited to completion inside
their own `wsl` invocations and none surviving this session: the step-1
pair, the step-2 regeneration, the exerciser warm-up, the eight-cell
battery, the three `007deea6` baselines, and two `make test` runs. No
`until` waiter was hand-rolled. `ps -eo` at close shows no `java` this
session started.

## 10. Close

Two payload commits -- `4ddf62c2` (the derivation) and `fa0ef0b4` (the
re-measurement) -- both docs-only, plus this record and the close
marker. No count pin moved, so R-pins is not
engaged. No roadmap row changed: nothing this session touched is a row,
and the de-scaffold ruling puts a payload session's finding in its own
record rather than in a register — §8 is that line.
