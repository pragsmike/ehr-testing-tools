# 2026-09-07 — ADR-0180 site 6: `decide :bed-swap` rides the swap view

Site 6 of the generate-quadratic program, the second of the three the 2026-09-06
addendum chartered — `roadmap.md#performance-residual-sites` PRIORITY 1. Ceremony
mode: R30 (commit and push at each checkpoint), taken from the prompt. Prompt
archived at
[`../prompts/2026-09-07-adr-0180-site-6-bed-swap.md`](../prompts/2026-09-07-adr-0180-site-6-bed-swap.md).
Reasoning-of-record: `notes/adr/0180-indexes-ride-the-fold.md`, its
`Addendum, 2026-09-06` section 6, the `:eligible-index` concern and R-hash-order.
No ADR was written and none was owed — this session ENACTS a charter already
ruled and was commissioned to make no correction to it, so ADR-0180's own text is
untouched.

Rulings in force: **R-fold-carrier**, **R-equivalence**, **R-no-second-path**,
**R-hash-order**, **R-read-throws**, **R-order-2**, **R-view-unchanged** (new with
this prompt), **R-move-not-improve**, **R-pins**, **R-edit**, **R-cap**.

## 0. Preflight

`bin/preflight` ran first, exit 0, no findings: last five CI runs on `main` all
green, edit root `/home/mg/src/ehr-testing-tools` (not `/mnt/`), `core.fileMode`
true, `core.ignorecase` unset, working tree clean including untracked files, local
HEAD `0b7976db` equal to `origin/main`. Session start and every bracket baseline
is that sha.

The instrument's own zero ran before any edit: `bin/ground-truth-bracket 0b7976db
0b7976db` reported IDENTICAL on all 38 digested roots (3 skipped, no
`:ground-truth` key).

Both baseline cells reproduced the digests sites 1–5 recorded: `a7500-persons`
sha256 `3018299a…0d3bd3` at 102.10 s, `a2500-nopersons` `c22d6573…a55208` at
26.45 s.

## 1. What landed

**`d16e38f4` — `engine: decide :bed-swap reads :eligible-index's swap view`.**
The whole change, and it is small on purpose. `eligible` in `decide :bed-swap`
became `(fold/swap-eligible world patient-id)`; the inline `(:patients world)`
scan was deleted with no fallback. Nothing else in the method moved. The three
disclosures that said this repoint had not happened were retired in the same
commit (section 3(a)), and `naive-swap-eligible` stopped being a COPY and became
a MOVE.

**`50c2a1f0` — `plans: site 6 measured`.** `measurements.md` gains a dated site-6
section; the roadmap P1 row drops to one remaining site and the P2 determinism row
records that site 6 preserved hash order as ruled.

## 2. The measurement

Both cells reproduced their pre-change logs BYTE-FOR-BYTE — the same two digests
as in section 0 — and `bin/ground-truth-bracket` reported IDENTICAL on all 38
digested roots at the clean tip and at `d16e38f4`. `50c2a1f0` is docs-only and
owed none.

| cell | before | after | delta |
|---|---|---|---|
| `a7500-persons` generate | 102.10 s | **87.95 s** | -14.15 s, **-13.9%** |
| `a2500-nopersons` generate | 26.45 s | **24.11 s** | -2.34 s, **-8.8%** |
| `a7500-persons` check | 48.04 s | 46.17 s | -1.87 s |
| `a2500-nopersons` check | 18.73 s | 18.55 s | -0.18 s |

JFR at 7,500 arrivals, inclusive share, `--stack-depth 2048`, **a MATCHED PAIR**
by site 5's own method — the before half recorded from a worktree at `0b7976db`
with the same driver and the same cell: `decide :bed-swap` **12.58% → 0.35%**, the
whole `decide` dispatch 29.18% → 18.73%. Allocation from the same recordings:
`decide :bed-swap` 6.98 GB (12.76%, 3,176 samples) → 0.003 GB (0.01%, 2), phase
total 54.7 → 47.0 GB. **Site 6 added no concern and no cost** — `:eligible-index`
is site 5's row and reads 0.22% → 0.11% of the phase on code neither session
touched.

**AND THE TOP OF THE DECADE.** One timed `a22500-nopersons` generate: **250.62 s →
149.41 s**, and **1,472.56 s → 149.41 s** across the whole six-site program,
**9.86x** — over a log BYTE-IDENTICAL to the committed 2026-09-05 digest,
`7d105743…78e0a` over the same 174,866,696 bytes.

## 3. Judgment calls

**(a) THE PROMPT'S OWN CITATION DID NOT RESOLVE, and the disclosure was retired in
three places rather than one.** The prompt names "the docstring at fold.clj:87".
`fold.clj:87` is inside the namespace docstring's edge list and says nothing about
this; the sentence quoted is `eligible_index_test.clj:87-88`, and the same claim
also stands at `fold.clj:593-599` (inside `swap-eligible`'s own docstring) and
`fold.clj:776-779`. All four were false the moment the repoint landed, so all four
were corrected. This is the one place the session had to choose rather than
follow, and section 3(b) is why it was not a stop.

**(b) R-VIEW-UNCHANGED WAS READ AS PROTECTING THE VIEW'S LAW, NOT ITS PROSE, and
`fold/swap-eligible`'s docstring was edited.** Read literally — no edit to that
form at all — the ruling would have left `src` asserting "`decide :bed-swap` DOES
NOT READ THIS YET" one line above a caller that reads it, while the two sentences
saying the same thing OUTSIDE the form were corrected. That is an internal
contradiction inside one file, not a defensible resting state. The ruling's own
stated reason is that "the view's law was proven at site 5 and a change reopens
it", and a docstring carries no law: the body, the arity, the throw and the
`(filter val)` are untouched, and the law namespace was neither weakened nor
re-run against a changed reference. One defensible reading, so fix-forward WITH
DISCLOSURE rather than STOP
(`rulings.md#R-stop-only-on-two-defensible-readings`). The repoint did not NEED
the view changed — that is the clause that would have forced a stop — and it was
not changed.

**(c) The before half of the profile was resolved against its own tree, and site
5's single offset would have been wrong.** See finding (a).

**(d) The before column is this session's own re-baseline, not site 5's after
column.** 102.10 s against site 5's 96.98 s on identical code, and 26.45 s against
24.74 s — the 3-6% re-baseline drift the P1 row already records. Quoting site 5's
number as this session's baseline would have credited site 6 with 9.03 s it did
not earn.

**(e) `eligible` is still bound EAGERLY**, as at site 5 and for the same reason:
the scan was eager, R-move-not-improve applies to the shape as well as the text,
and a lazy read would hide a missing seed rather than surface it. The `:with`
path, which never reads `eligible`, therefore throws on a world with no index —
exactly as `decide :merge` has since site 5, and the two scripted-test `world-of`
builders already seed.

**(f) No new test was written, and none was owed.** Site 5 built and proved this
view against `decide :bed-swap`'s own scan precisely so that site 6 would not have
to. The law namespace is unchanged in substance — only the four sentences that
described the interim state moved — and the suite's counts confirm it: 424 lines
and 28,057 passes at this tip, identical to site 5's close in all four figures.

## 4. Findings

**(a) THE GENSYM ATTRIBUTION NEEDED TWO RESOLUTIONS, WHERE SITE 5 NEEDED ONE, and
assuming otherwise would have misread the before half.** Deleting the two
anonymous fns of the scan shifts every LATER gensym ordinal in `decide.clj` by
**13** — `:merge` is `eval7081$fn__7084` before the repoint and `eval7068$fn__7071`
after — so the two recordings do not share a method map even though they share an
offset. Each half was resolved against the source it was recorded from (the after
half in the main clone, the before half in the worktree), and both landed at
**+3001** independently. `:bed-swap` is `eval10055$fn__10058` in both, which is
what makes the 12.58% and the 0.35% the same frame.

**(b) THE DELETED SCAN'S OWN LAMBDAS ARE VISIBLE IN THE INSTRUMENT, which is a
structural check the wall cannot give.** The before recording carries
`eval10055$fn__10058$fn__10063` at 3.98% and `…$fn__10070` at 2.44% — the
`remove` and the `filter` of the scan. The after recording carries neither, at
any share. The right frames went, not merely some frames.

**(c) PEAK RSS ROSE AT BOTH GENERATE CELLS over a run that allocates 14.1% LESS**
— 2,175 → 2,340 MB at 7,500 and 886 → 972 MB at 2,500, where site 5's fell.
Recorded, not explained away: peak RSS is a GC-schedule figure and the live set is
`-Xlog:gc*`'s question, which was not run at these two cells. At the top of the
decade, where the peak is taken across a whole run, it FELL: 3,575 → 3,196 MB.

**(d) A STANDALONE `sim check` REPLAYS TWENTY TIMES, AT BOTH COMMITTED CELLS —
site 7's own price, measured here.** Counted at the var `check.clj` calls through
(`ehrt.sim-engine.interface/replay`), over each cell's own log, so the figure is
INVOCATIONS and not call sites. The addendum priced site 7 at "seventeen per
`check-all`, twenty with the bed cycle on"; both committed timed cells carry a bed
cycle, so **twenty is the number that ships** and seventeen is a case neither cell
exercises. The in-run `check-all` is **34.71%** of generate at 7,500 after this
site, up from 29.29% on the same code because the phase got shorter.

**(e) THE CHECK PHASE IS A CLEAN CONTROL AND STAYED ONE.** `replay` reads 47.00% →
47.88% of the check phase across the pair, and the two check walls moved less than
run-to-run spread. Site 6 is a generate-phase change and the instrument agrees.

**(f) THE COLLISION HOLE IS UNCHANGED AND STILL UNSEEN BY EVERY GATE HERE.** Site
6 reads the same sub-map site 5 built, so R-hash-order's hole rides along
untouched: the first colliding patient-id pair of the shipped seed is at 45,000
arrivals, the largest committed cell is 22,500, and
`at-a-hasheq-collision-the-two-orders-diverge` remains the only place it is
visible. Nothing this session did narrows or widens it.

## 5. The law, and that it did not move

`ehrt.sim-engine.eligible-index-test` is unchanged in substance. It still keeps
`decide :bed-swap`'s `eligible` construction VERBATIM as `naive-swap-eligible` and
asserts `fold/swap-eligible` equal to it — vectors, ORDER included — at every
replay entry of churn-bearing generated logs, plus the hand-built world of
`an-admitted-patient-with-no-location-is-in-the-merge-view-only` that no generated
log reaches. What changed is only that the definition is now a MOVE rather than a
COPY: after this repoint `src` holds no second implementation of any of the three
naive definitions, and the concern meets R-no-second-path at both of its views.

No red→green table is owed and none is offered: this session added no gate. The
law's own red→green, including the mutation that convicts a SORTED merge view, is
site 5's record, section 5.

## 6. Suite figures

| point | test-result lines | passes | failures | errors |
|---|---|---|---|---|
| `d16e38f4` (the repoint) | 424 | 28,057 | 0 | 0 |
| docs tip (over `50c2a1f0`) | 424 | 28,057 | 0 | 0 |

Site 5's close was 424 / 28,057 / 0 / 0 (that record, section 6). **NOTHING MOVED
AT ALL** — which for a repoint onto an already-proven view is the whole expected
result, and is why section 3(f) says no test was owed.

`:onboarding` reading-set headroom went **15 → 16** against an unchanged 1,530
budget, on the roadmap edit's net -1 line. `:docs` is untouched and stays at its
standing headroom of 0.

## 7. Background processes

Harness-tracked background jobs, each of which exited on its own with its exit
code recorded: two `bin/ground-truth-bracket` runs (the instrument's own zero at
the clean tip, then one at the single CODE commit — `50c2a1f0` owed none, being
docs-only), two timed cell-pair runs (before, after), one combined
JFR-pair-plus-`a22500` run, and two `make test` runs. Everything else —
`bin/preflight`, `make docsgen`, the gensym attribution pass, the replay-count
probe, one allocation re-aggregation and a three-namespace smoke run — ran in the
foreground.

**R-sentinel, honestly.** Every wait for a harness-tracked job was a completion
notification or a single bounded `sleep` inside one foreground call, never a poll
loop that wakes the session per iteration — the failure mode site 4's record named
at 72 waiters. The CI wait at close is one background job that notifies once.

At close, `ps` reports no `java`, no `make` and no `sleep` belonging to this
session, and `git worktree list` shows only the main clone — the baseline-profile
worktree was removed as soon as its recordings were read.

## 8. HEAD landed

The measurement commit `50c2a1f0` is the last payload commit; this record and its
prompt archive land on top of it, and a close-marker commit follows once CI is
verified green with `gh run view`. Baseline for every bracket in this session was
`0b7976db`.

## 9. CI

Filled in at the close-marker commit.
