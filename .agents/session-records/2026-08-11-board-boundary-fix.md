# 2026-08-11 — Board boundary catch-up: the snapshot grid stops lagging behind stream-time jumps (ADR-0103)

## Scope

Fix session for a live defect in `ehrt play --board`'s snapshot
cadence, author-reported 2026-08-10 (design channel; report
transcript-witnessed, never cited as evidence — this session
reproduced its own red). The "c." ruling scoped this session to the
bugfix half only; the ED-weighted busy-tuesday redesign directions the
author also gave 2026-08-10 are recorded as a chartering ruling for a
separate, not-yet-opened arc, not executed here. Two commits: the fix
itself (`ad69fdc`, board sink + three new tests + the busy-tuesday
README re-witness + the glossary rider), and this close-phase
record/prompt-archive commit. This is ADR-0103.

## Red→green evidence highlights

`board-sink`'s post-render boundary update advanced `next-boundary-ms`
by exactly one span, regardless of how far behind the actual stream it
had fallen after a jump — every message from then on satisfied `>=
next-boundary-ms` and rendered, including a message sharing the SAME
board window as the message that just rendered. Two hermetic tests
reproduced this against the pre-fix tree (`clojure -M:poly test :all
component:cli`): a three-message jump case (3 snapshots rendered
instead of 2, the last two at an IDENTICAL timestamp — the author's own
observed "paired identical" shape) and a six-message, four-window grid
case with two deliberately empty windows (5 snapshots instead of 3,
same identical-pair tail). Both reds captured verbatim in ADR-0103.
Fixed by replacing the one-span advance with an arithmetic
smallest-grid-point-above-`ts` computation (`next-boundary-after`);
both tests green post-fix, confirmed via a targeted grep across the
full `ehrt.cli.core-test` run (zero `FAIL`/`ERROR` beyond the banner
line) — every pre-existing board test (ADR-0067, ADR-0100) stayed
green throughout, unsurprising since the fix's formula reduces to the
old one whenever the lag is under one span (the ordinary, no-gap
case).

**Full gate:** `clojure -M:poly check` OK; full local suite (`clojure
-M:poly test :all skip:integration`, unredirected capture) 596
occurrences of "0 failures, 0 errors" across the entire log, 0
FAIL/ERROR anywhere, exit 0, 3 minutes 52 seconds;
`ehrt.cli.cli-parse-guard-lint-test` confirmed green within that run
(line 1149); `bin/verify-nist-lock` OK, 6/6; oracle bracket
(`0099f81`→`ad69fdc`) all 34 roots IDENTICAL, soundness check clean.

## Judgment calls and their ratification status

- **The busy-tuesday README's witnessed numbers, re-run and updated in
  place.** Directly licensed by the driving prompt's own Context
  ("re-run that block's exact generate+play commands... update the
  witnessed numbers if they move"). They moved (68 → 48
  `:snapshot-count`) — not a judgment call, the prompt's own named
  contingency executing.
- **The glossary rider landed in the same commit as the fix**, per the
  prompt's own explicit "your call, disclosed": two two-word prose
  edits, judged not worth a separate commit; disclosed in ADR-0103's
  own Decision section.
- **The two O(1)-computation-witness test's own non-red status
  disclosed rather than miscounted as a red→green pair** — the
  years-long-gap test (only two messages) doesn't expose the bug
  pre-fix, since a single-span lag and a correctly-computed boundary
  agree when there's no third message to reveal the difference. Named
  explicitly in ADR-0103 rather than silently folded into the red
  evidence.

## Findings and HEAD landed

**One real, shipped defect found and fixed:** the board sink's
snapshot-cadence boundary tracking, described above. Confirmed live
too — the busy-tuesday demo's own real run recount (68 → 48) is a
second, independent witness beyond the hermetic tests.

**No other defects found.** The oracle's pure-identity prediction held
exactly across all 34 roots — the fix's blast radius stayed inside the
one sink function its own tests, plus two docs files.

**Tag paid forward:** `stable-20260810-marker-only-footnotes` tagged
at `0099f81` (Step 1, this session — the design channel's own verified
ADR-0102 landing, tag law case (i)), peeled ref verified exact match,
remote unmoved.

**HEAD landed:** the close-phase commit (this record's own commit),
pushed.
