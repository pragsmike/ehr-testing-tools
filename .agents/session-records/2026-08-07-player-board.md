# 2026-08-07 — player board

## Scope

Session 2 of the player arc: wire the fold ADR-0066 made total
(A17/A40 included, self-anchored in absolute epoch millis) into a real
display — `ehrt play PATH --board N`, a bed-state snapshot every N
stream-minutes. Executed the standing-approved component-graph change
(`v2-replay` exported through `ehrt.sim-emit-hl7.interface`, `corpus`
gaining its first real external edge into `sim-emit-hl7`), the
author's own rider ("Tests should build their own directories as
needed" — standing law), the `--board` flag, a board sink in
`bases/cli`, and a pure renderer in a new `ehrt.corpus.board`
namespace. Full detail: `notes/adr/0067-player-board.md`.

## Red→green evidence

Step 2 (red, witnessed) — `ehrt.corpus.board-test` (renderer +
`fold-event`, against stub bodies that threw "not yet implemented"):

```
ERROR in (render-snapshot-empty-accumulator-has-header-and-zero-tally-test) (board.clj:41)
ERROR in (render-snapshot-one-ward-lists-its-occupied-beds-test) (board.clj:41)
ERROR in (fold-event-a-foreign-trigger-is-a-counted-skip-not-a-crash-test) (board.clj:30)
ERROR in (render-snapshot-multi-ward-outpatient-discharge-merged-tombstone-test) (board.clj:41)
ERROR in (fold-event-folds-a-supported-trigger-through-the-real-accumulator-test) (board.clj:30)

Ran 5 tests containing 5 assertions.
0 failures, 5 errors.
```

`ehrt.cli.core-test`'s new `play-command-board-*` tests, run directly
(the full `poly test :all` run aborts after the first brick with a
failure, so this namespace was run in isolation via `clojure -M:dev:test`
to witness its own red state without waiting on that abort):

```
FAIL in (play-command-board-unfolded-trigger-is-a-counted-cued-skip-not-a-crash-test) (core_test.clj:2627)
FAIL in (play-command-board-unfolded-trigger-is-a-counted-cued-skip-not-a-crash-test) (core_test.clj:2628)
FAIL in (play-command-board-renders-a-snapshot-at-each-boundary-crossing-plus-a-final-one-test) (core_test.clj:2601)
FAIL in (play-command-board-renders-a-snapshot-at-each-boundary-crossing-plus-a-final-one-test) (core_test.clj:2603)
FAIL in (play-command-board-renders-exactly-one-final-snapshot-when-no-boundary-is-crossed-test) (core_test.clj:2613)
FAIL in (play-command-board-wins-over-ticker-when-both-are-given-test) (core_test.clj:2652)
FAIL in (play-command-board-wins-over-ticker-when-both-are-given-test) (core_test.clj:2653)

Ran 251 tests containing 721 assertions.
7 failures, 0 errors.
```

(The eighth new test, sink-precedence, passed here already — it only
asserts the ABSENCE of board keys, true before implementation too; a
real regression guard once green, not red evidence itself.)

Step 3 (green): both namespaces landed `0 failures, 0 errors`
(`ehrt.corpus.board-test` + `ehrt.cli.core-test` together: 256 tests,
749 assertions). Full workspace suite (`clojure -M:poly test :all
skip:integration`): 227 namespaces, `0 failures, 0 errors`, exit 0.
`clojure -M:poly check`: OK, both before Step 2's tests were written
(confirming no `deps.edn` edit was needed once `board.clj` genuinely
required the interface) and again after Step 3 landed.

## Live probe (AR-BB2-5)

Seed sweep for A17/A40 traffic (`bin/ehrt corpus generate sim --seed N
--patients 8 --churn`, then grep for `ADT^A17`/`ADT^A40`): seed 42
(the prompt's own suggestion) produced neither; seeds 1/2/3/5/7/11/13/
17/19/23 were swept — seed 5 produced both (one A17, one A40).

`bin/ehrt play <seed-5 corpus> --board 60 --rate 100000` (post-fix,
below):

```
-- board snapshot: 2024-01-01T01:39:00Z --

Renal:
  RENAL-02  Nguyen, Lisa  MRN MRN000003  inpatient  attending: 7785861205
  RENAL-03  Moore, Sophia  MRN MRN000002  inpatient  attending: 7244654126

inpatients: 2  active outpatients: 0  discharged: 1  merged: 0
-- board snapshot: 2024-01-01T02:03:00Z --
...
-- board snapshot: 2024-01-01T05:54:00Z --

Renal:
  RENAL-03  Brown, Sandra  MRN MRN000008  inpatient  attending: 7244654126

inpatients: 1  active outpatients: 0  discharged: 6  merged: 1
-- board snapshot: 2024-01-01T07:19:00Z --

inpatients: 0  active outpatients: 0  discharged: 7  merged: 1
-- board snapshot: 2024-01-01T07:19:00Z --

inpatients: 0  active outpatients: 0  discharged: 7  merged: 1
{:status :ok, :payload {:unparseable-count 0, :snapshot-count 7, :skip-count 0,
 :rate 100000.0, :idle-cap-ms 5000, :wallclock-ms 408, :stream-span-ms 26340000,
 :clamped-count 0, :emitted 21, :unfolded-count 0, :sink "ticker"}}
```

Seven snapshots (six boundary crossings across the ~7.3-hour stream
span plus the unconditional final one, which coincided exactly with
the last boundary crossing and rendered a second time, per the
ruling's own "regardless of boundary position" wording); the A40
merge correctly tallied (`merged: 1` from the second-to-last snapshot
onward) and — post-fix — never listed as occupying a bed.

`bin/ehrt play <seed-5 corpus> --board 60 --rate 100000 --sink
file:board-sink-out.hl7` vs. a plain `--sink file:plain-sink-out.hl7`
run at the same rate: `diff` reported no difference (byte-identical),
and the board-mode result envelope carried neither `:snapshot-count`
nor `:unfolded-count` — `--board` correctly ignored when `--sink` is
given.

## Live-probe-caught bug

The FIRST live-probe run (pre-fix) showed a merged patient (Kim,
James, `MRN000007`) still listed under a ward as occupying
`RENAL-H01` at every snapshot from the merge onward, alongside
`merged: 1` in the same tally line — a direct contradiction of
AR-BB2-4's own "tombstones are COUNTED, never listed as occupying
anything." Root cause: `fold-merge` (`v2_replay.clj`, frozen, read
only) absorbs the merged-away entry into the survivor but never clears
the merged-away entry's OTHER fields — a merged-away mrn that carried
a real `:location` before the merge keeps it, stale. `render-snapshot`'s
own `occupied?` checked only `(:location entry)`; fixed to also
exclude `:status :merged`. The renderer's own multi-ward unit test was
strengthened to build its tombstone fixture with exactly this shape
(a merged entry carrying a stale `:location`) so a regression is
caught by the unit suite next time, not only by a live probe.

## Judgment calls and their ratification status

- **`components/corpus/deps.edn` needed no edit.** The prompt's own
  literal text named that file gaining a `poly/sim-emit-hl7
  {:local/root ...}` entry. `clojure -M:poly check` — the prompt's own
  nominated arbiter — was green with zero `deps.edn` changes anywhere,
  once `board.clj` genuinely required the interface. Fresh grep of
  every `components/*/deps.edn` in this repo confirms none EVER
  declares a `poly/*` local-root edge (that wiring lives only in the
  root `deps.edn`'s aliases and each `projects/*/deps.edn`), and every
  file that already carried `poly/corpus` already carried
  `poly/sim-emit-hl7` too, transitively, since `sim`'s own src has
  required it since the sim split. Judgment call under the prompt's
  own arbiter clause, not a deviation from it — recorded in
  `notes/adr/0067-player-board.md` as a finding, not silently skipped.
- **Where the renderer/fold-event pair lives.** The prompt left this
  as "fresh-read judgment, disclosed": a new `ehrt.corpus.board`
  namespace was chosen over folding it into `player.clj` (deliberately
  clock/IO-free, no reason to gain a `sim-emit-hl7` require) or
  `display.clj` (its own placement is a separate, still-open roadmap
  question the prompt explicitly said not to entangle).
- **The board sink's own internal shape** (`board-sink` in
  `bases/cli/src/ehrt/cli/core.clj`, closing over five atoms:
  accumulator, first-ts, next-boundary-ms, snapshot-count,
  unfolded-count) was not prescribed beyond the returned
  `{:sink-fn :cue-fn :finalize-fn :snapshot-count-fn
  :unfolded-count-fn}` shape the prompt's own AR-BB2-3 implied by
  naming the seams it must honor (`cue-fn`, `println-fn`,
  `message-timestamp-ms`). `finalize-fn` (called once by
  `play-command` after `run-plan!` completes) was this session's own
  mechanism for "one final snapshot at stream end regardless of
  boundary position" — not named verbatim in the prompt, a direct
  reading of AR-BB2-3's own requirement.
- **`?` for a nil `:class`.** Not named in the prompt. Discovered via
  a real fixture in the existing `two-message-blob` test helper
  (`adt-a02-transfer.hl7` opens a NEW mrn via A02 alone, bootstrap-
  from-empty, which never sets `:class`) — the ticker's own
  established "a field it can't read renders as `?`, never a thrown
  exception" convention was extended here rather than inventing a new
  one.

## Findings and HEAD landed

- **Enumeration (AR-BB2-R) found no additional offender.** The
  `config/synthea/synthea.properties` hits are a TRACKED fixture (`git
  ls-files config/` confirms), explicitly out of scope by the ruling's
  own carve-out. Every `docs-tooling` lint test walking a repo
  directory (`docs/`, `components/`, `.agents/skills`, `notes/prompts`,
  `.`) is walking the TRACKED repo tree as its own literal test
  subject — not the `config/`-class hazard (an author-held scratch
  file whose presence is incidental to the test, not its point). Full
  table in `notes/adr/0067-player-board.md`.
- **Two synthetic-fixture NPEs, fixed in the test helpers, not
  `v2_replay.clj`.** `hl7-date->iso` (frozen src) has no nil guard on
  PID-7 (DOB); both `board_test.clj`'s and `core_test.clj`'s own `msh`/
  `board-message` helpers were missing that field, tripping it on the
  first hand-built message. Fixed by adding a DOB literal to both
  helpers, not by touching frozen src.
- HEAD landed: this session's own closing commit (docs-only: this
  record, the prompt archive, `notes/ADRs.md`'s index line,
  `notes/adr/0067-player-board.md`, both READMEs, and the roadmap's
  own Done pointer plus the two now-closed Next rows removed), on top
  of `6ee8f79` (Step 3, the green landing), `c6d2b19` (Step 2, red),
  and `df6034f` (Step 1, the rider).
