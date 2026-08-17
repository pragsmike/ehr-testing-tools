## ADR-0103 — Board boundary catch-up: the snapshot grid stops lagging behind stream-time jumps

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-11.

### Context

Author-reported 2026-08-10 (design channel): `ehrt play --board`
sometimes prints paired, identical `-- board snapshot: <same ts> --`
lines right after an idle-skip. This session fixes it. Per this
session's own driving prompt, the design-channel report is
transcript-witnessed but not cited as evidence — the red below is this
session's own, hermetic reproduction.

**The defect.** `board-sink` (`bases/cli/src/ehrt/cli/core.clj`)
tracks a snapshot grid anchored at the first message's own timestamp
(`first-ts`), spaced every `board-minutes * 60000` ms
(`boundary-span-ms`). Before this fix, crossing a boundary advanced
`next-boundary-ms` by exactly ONE span:

```clojure
(when (>= ts @next-boundary-ms)
  (render! ts)
  (swap! next-boundary-ms + boundary-span-ms))
```

After a stream-time jump larger than one span (the idle-skip case —
ADR-0014's own wallclock-cap mechanism, orthogonal to this cadence but
the thing that makes such jumps common), the boundary is left
arbitrarily far behind the stream. Every message from then on
satisfies `>= next-boundary-ms` and renders — including a second
message that lands in the SAME board window as the message that just
rendered, producing a duplicate snapshot. When that second message is
also the run's last message, `finalize-fn`'s own unconditional final
snapshot (rendered "regardless of boundary position," ADR-0067
AR-BB2-3, unchanged by this fix) fires immediately after at the exact
same timestamp — the paired-identical-lines shape the author observed.

**The invariant this fix restores** (now stated in `board-sink`'s own
docstring): boundaries live on a grid at `first-ts + k*span`. At most
one snapshot renders per grid window that contains messages, rendered
at the first message at or after each crossed boundary. After
rendering at `ts`, the next boundary is the smallest grid point
strictly greater than `ts` — computed arithmetically
(`first-ts + span * (1 + floor((ts - first-ts) / span))`, via
`quot`/`inc`/`+`), never by looping span-at-a-time across a gap. Empty
windows inside a gap render nothing — unchanged, already correct.

### Decision

**Author rulings, verbatim (2026-08-10):**

- **"c."** (choosing both: this bugfix now, the busy-tuesday/ED
  scenario redesign as its own next arc). This session is the bugfix
  HALF ONLY — no scenario/config/content changes land here.
- **Redesign direction** (chartering context for the next arc, NOT
  executed this session): *"Maybe weight the patient population
  toward immediate, emergent conditions like trauma/injuries? This
  would simulate an actual ED, which is where a lot of the activity
  and churn would happen."*

**Tag ceremony.** Design channel verified the ADR-0102 landing
(`0099f81`) by fresh public clone. `stable-20260810-marker-only-
footnotes` tagged at `0099f81`, pushed, peeled ref confirmed
`0099f81b76e6401581c2604e06d3d5cf511415c5` — exact match; remote had
not moved (`origin/main` was already at `0099f81` at session start).

**Red, reproduced hermetically.** Three new tests added to
`ehrt.cli.core-test`, run against the pre-fix tree via `clojure -M:poly
test :all component:cli`. The primary reproduction —
`play-command-board-boundary-catches-up-past-a-stream-time-jump-test`
— feeds `board-sink` (through `play-command`'s own public seam, the
same hermetic pattern every other board test in this file already
uses) three synthetic messages: `t0` (2026-08-01T00:00:00), `t0 + 5
days` (2026-08-06T00:00:00), and `t0 + 5 days + 30s`
(2026-08-06T00:00:30) — the last two sharing one 60-minute board
window. Red, verbatim:

```
FAIL in (play-command-board-boundary-catches-up-past-a-stream-time-jump-test) (core_test.clj:2812)
message 2 (5 days after message 1) crosses one boundary and renders; message 3, 30 stream-seconds later, sits in the SAME 60-minute board window and must not render again -- only the unconditional final snapshot follows it
expected: (= 2 (:snapshot-count (:payload r)))
  actual: (not (= 2 3))

FAIL in (play-command-board-boundary-catches-up-past-a-stream-time-jump-test) (core_test.clj:2814)
expected: (= 2 (count snapshot-ts))
  actual: (not (= 2 3))

FAIL in (play-command-board-boundary-catches-up-past-a-stream-time-jump-test) (core_test.clj:2815)
no duplicate, identical-timestamp snapshot pair -- the pre-fix bug's own observed shape
expected: (apply distinct? snapshot-ts)
  actual: (not (apply #object[clojure.core$distinct_QMARK_ 0x67b951ac "clojure.core$distinct_QMARK_@67b951ac"] ("2026-08-06T00:00:00Z" "2026-08-06T00:00:30Z" "2026-08-06T00:00:30Z")))
```

The third assertion's own failure message names the exact author-
observed shape: two identical `2026-08-06T00:00:30Z` timestamps in the
snapshot list (the buggy duplicate render, immediately followed by
`finalize-fn`'s own unconditional final render landing on the same
instant).

A second test, `play-command-board-grid-invariant-multiple-windows-
with-gaps-and-multi-message-windows-test` (six messages across four
grid windows at `--board 30`, two of the four windows deliberately
empty), reds the same way — 5 snapshots rendered instead of 3, again
ending in an identical-timestamp pair:

```
FAIL in (play-command-board-grid-invariant-multiple-windows-with-gaps-and-multi-message-windows-test) (core_test.clj:2833)
occupied windows: [0,30) (msgs 1-2, anchor, renders when window [30,60) is entered), [30,60) (msg 3, one render); [60,90) and [90,120) are EMPTY (no messages, no snapshot); [120,150) (msgs 4-6, one render at msg 4, msgs 5/6 stay in the same window); plus one unconditional final snapshot
expected: (= 3 (:snapshot-count (:payload r)))
  actual: (not (= 3 5))

FAIL in (play-command-board-grid-invariant-multiple-windows-with-gaps-and-multi-message-windows-test) (core_test.clj:2835)
expected: (= 3 (count snapshot-ts))
  actual: (not (= 3 5))

FAIL in (play-command-board-grid-invariant-multiple-windows-with-gaps-and-multi-message-windows-test) (core_test.clj:2836)
each of the three snapshots renders at a distinct instant -- no same-window duplicate
expected: (apply distinct? snapshot-ts)
  actual: (not (apply #object[clojure.core$distinct_QMARK_ 0x67b951ac "clojure.core$distinct_QMARK_@67b951ac"] ("2026-02-01T00:35:00Z" "2026-02-01T02:10:00Z" "2026-02-01T02:20:00Z" "2026-02-01T02:25:00Z" "2026-02-01T02:25:00Z")))
```

A third test (`...-boundary-computed-arithmetically-across-a-years-
long-gap-test`, a 10-year jump between exactly two messages) did NOT
red pre-fix — with only two messages, the buggy one-span advance and
the fixed arithmetic advance produce the same count (the lag is never
exposed because there is no third message left to reveal it). Included
anyway as the O(1)-computation witness once fixed (see below); its
own green run is not a red→green pair, disclosed here rather than
miscounted as one.

Full transcript (`clojure -M:poly test :all component:cli`, pre-fix):
`ehrt.cli.core-test` — 6 failures, 0 errors, all three above.

**Fix.** `maybe-snapshot!`'s post-render boundary update replaced with
the arithmetic formula, extracted into `next-boundary-after`:

```clojure
next-boundary-after (fn [ts]
                       (+ @first-ts (* boundary-span-ms
                                        (inc (quot (- ts @first-ts) boundary-span-ms)))))
```

`board-sink`'s own docstring states the invariant (quoted in Context,
above). The first-message anchor branch (`(+ ts boundary-span-ms)`) is
untouched — it already computes the same smallest-grid-point-above-ts
value for the `delta = 0` case, so the fix does not disturb the
already-correct first-message behavior. Nothing about `finalize-fn`'s
own unconditional-final-snapshot rule changed — that duplicate-on-
purpose behavior (ADR-0067 AR-BB2-3, re-confirmed live below) is a
different, established mechanism, not part of this defect.

**Green.** Same three tests, post-fix, `clojure -M:poly test :all
component:cli`: `ehrt.cli.core-test` runs clean (confirmed via a
targeted grep for `core-test`/`FAIL`/`ERROR` across the full run —
zero matches beyond the `Testing ehrt.cli.core-test` banner line
itself). Every pre-existing board test (the four from ADR-0067, the
two event-log-board tests from ADR-0100) stayed green throughout —
confirmed by the same full run, not assumed: the fix's arithmetic
reduces to the exact pre-fix formula whenever a crossed boundary is
less than one span behind the stream (the ordinary, no-gap case), so
no existing cadence changes.

**The busy-tuesday README re-witnessed.** Per Context's own
contingency, the exact generate+play commands in `demos/scenarios/
busy-tuesday/README.md`'s "Play" section were re-run post-fix (same
seed, `20260807`, same 200 patients, same config). The numbers DID
move: `:snapshot-count` drops from 68 to 48 — 20 of the run's 68
messages had been landing in a board window a prior message already
opened, each producing a genuine duplicate under the pre-fix cadence.
Every other figure in the closing summary (`:emitted 68, :skip-count
41, :clamped-count 0, :stream-span-ms 279155640000`) is unchanged —
expected, since the fix touches only snapshot cadence, never the
pacer or the fold. The README's own "What to look for" block is
updated in place with the corrected summary and a dated re-witness
note; the first rendered snapshot line (`2024-02-12T00:37:00Z`,
deterministic given the fixed seed) is unchanged and left as the
worked example.

**The glossary rider.** Landed in the same commit as the fix, per this
session's own disclosed judgment call (both entries are two-word,
mechanical prose edits, not worth a separate commit). `docs/
glossary.md`'s Baseline and Pack entries carried a stale
`` `notes/ADRs.md` `` prefix in front of a footnote marker that
actually targets `notes/tools/ADRs.md` — the ADR-0102 anomaly-closure
retargeted the marker's own definition but, by that session's own
disclosed scope ("moves citation targets, not content"), left the
surrounding prose exactly as ADR-0101 wrote it. Before/after, both
sites:

1. **Baseline** (Register line) — before: `` Register: `notes/
   ADRs.md`[^tools-adr-0013]/[^tools-adr-0015] (tools' pre-merge
   sequence, `notes/tools/ADRs.md`). `` — after: `` Register: the
   design record[^tools-adr-0013]/[^tools-adr-0015] (tools' pre-merge
   sequence, `notes/tools/ADRs.md`). ``
2. **Pack** — before: `` **Pack.** Retired mechanism (`notes/
   ADRs.md`[^tools-adr-0006]-era, tools' pre-merge sequence) `` —
   after: `` **Pack.** Retired mechanism (the design record[^tools-
   adr-0006]-era, tools' pre-merge sequence) ``

Matches the generic-referent idiom ADR-0102's own reworded-sentences
list already established (`"the design record[^adr-NNNN]"`) rather
than inventing a new phrasing. No gate change — the hardened
`no-visible-adr-token-in-prose-test` (ADR-0102) scans for `ADR-\d{4}`
substrings, not doc-name path text, so it neither caught this anomaly
nor is affected by fixing it.

**Fix commit:** `ad69fdc` — "fix: board snapshot boundary catches up
past stream jumps (ADR-0103)."

### Oracle bracket

Pure identity was the prediction (one CLI sink fn, its tests, one demo
README, two glossary prose lines — no oracle-path namespace touched).
`bin/regression-oracle 0099f81 ad69fdc`: soundness check IDENTICAL
outside `digest.clj`'s own `(ns ...)` form; all 34 roots' SHA-256
digests IDENTICAL between baseline and target. Matches the prediction
exactly — no STOP-AND-REPORT needed.

### Full gate

`clojure -M:poly check`: OK. Full local suite (`clojure -M:poly test
:all skip:integration`, unredirected capture): 596 occurrences of "0
failures, 0 errors" across the entire log, 0 FAIL/ERROR anywhere, exit
0, 3 minutes 52 seconds. `ehrt.cli.cli-parse-guard-lint-test` confirmed
green within that same run (line 1149 of the captured log — 4 tests,
22 assertions, 0 failures/errors). `bin/verify-nist-lock`: OK, 6
hit-nexus-sourced coordinates matched.

### Fences

Touched exactly the list the driving prompt named: `bases/cli/{src,
test}` (the board sink and its three new tests),
`demos/scenarios/busy-tuesday/README.md` (the witnessed block only),
`docs/glossary.md` (the two rider lines), `notes/adr/0103-*.md`,
`notes/ADRs.md`, `notes/adr/README.md`, `.agents/*` close-phase files.
`components/corpus/src/ehrt/corpus/board.clj` untouched —
`render-snapshot` is pure and was never the defect; the cadence bug
lived entirely in the CLI executor's own `board-sink`. No scenario,
config, module, sim, or content change — the ED-weighted redesign is a
separate, not-yet-opened arc per the "c." ruling. No history rewrite.

### Deviations, dated 2026-08-11

No deviations from the driving prompt's own steps, fences, or
rulings. The busy-tuesday README's own witnessed numbers DID move
post-fix (68 -> 48 snapshots) — the prompt's own named contingency for
that outcome (update, don't merely attest) is what executed, not a
departure from it.

### Consequence

`ehrt play --board N` now renders at most one snapshot per occupied
grid window, always — a stream-time jump of any size, including a
years-long one, is caught up in O(1) by arithmetic rather than lagging
behind and over-rendering. The board's own `:snapshot-count` in the
result envelope now counts real, distinct windows by construction. The
busy-tuesday demo's own witnessed numbers are corrected to match the
fixed cadence. The two `docs/glossary.md` prose sites ADR-0102's own
citation-retarget left slightly orphaned now read cleanly. A Next row
opens for the busy-tuesday/ED scenario redesign the author's own
2026-08-10 ruling charters — awaiting its own design pass, not
started here.

### Index line

```
- 2026-08-11 — board-boundary-fix — ADR-0103
```

(appended to `.agents/plans/roadmap.md`'s own Done section; a new Next
row records the redesign arc's own charter, awaiting-design-pass.)

`notes/adr/README.md`'s own file count corrects 100→101, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Board boundary catch-up: the snapshot grid stops lagging behind stream-time jumps — the `--board` sink's post-render boundary update advanced by exactly one span, leaving it arbitrarily far behind after any stream-time jump larger than a span (the idle-skip case) and rendering a duplicate, identical-timestamp snapshot for every message sharing a board window with the message that just rendered; fixed to an arithmetic smallest-grid-point-above-ts computation, hermetically red-then-green on two new tests (the author-observed paired-identical shape, and a four-window grid-invariant case with two deliberately empty windows); the busy-tuesday demo's own witnessed snapshot count is re-run and corrected (68 → 48); two stale `notes/ADRs.md` prose prefixes in `docs/glossary.md`'s Baseline/Pack entries (an ADR-0102 residual finding) are dropped; the oracle holds pure identity across all 34 roots
