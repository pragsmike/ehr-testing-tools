# The refill tail is optional and off by default (ADR-0191 addendum)

2026-11-22. Base `b1186f18`. HEAD matched origin/main and the tree was
clean. `bin/preflight` exit 0, no finding. Event schema 1.13.0 at the
base.

Prompt: `.agents/prompts/2026-11-22-refill-tail-off-by-default.md`.

## Step 1: the prediction, written before any edit

No code was touched. The prediction is arithmetic over the logs the
previous session wrote at its tip (seed 20260824, `--churn`; sha-256
`dd4516c0...` at 28,024), read again here for four things that session
did not count.

| read from the base logs | 750 | 7,500 | 28,024 |
|---|---|---|---|
| events naming an expired patient | 0 | 0 | 0 |
| `:medication-end` of a `dense-clinic-chronic` order | 0 | 0 | 0 |
| latest chronic order, day | 27.4 | 42.5 | 55.7 |
| chronic orders after day 110 | 0 | 0 | 0 |
| merges after day 110 | 21 | 128 | 516 |

So in dense nothing ends a chronic order and nobody dies. A chain can
stop on its draw, or because a newer order of the same citation
replaced its own, and every such newer order is written by day 56,
inside the old tail. The chains that already stopped those two ways
keep their draws and do not move.

The chains the tail stopped (37, 345 and 1,441, from the previous
record's step 4) are the ones whose last fill drew "continue" and
whose next instant fell past the bound. Each now takes that next
refill, and after it one more for every draw under 0.94: an expected
1 + 0.94 / 0.06 = 16.67 more refills per such chain, with a standard
deviation near 16.2.

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| refills today | 81 | 760 | 3,596 |
| refills predicted | about 700 (500 to 900) | about 6,510 (5,900 to 7,100) | about 27,600 (26,400 to 28,900) |
| refills per filled chronic order, mean | about 14.5 | about 15.4 | about 15.3 |
| refills per filled chronic order, median | 11 | 11 | 11 |
| refills per filled chronic order, max | about 60 | about 95 | about 118 |
| chains stopped by the draw | 48 | 419 | 1,783 |
| chains stopped by a newer order | 0 | 5 | 16 |
| chains stopped by the order's end | 0 | 0 | 0 |
| chains stopped by a death | 0 | 0 | 0 |
| chains stopped by a merge | 0 | 0 | 0 |
| last refill, day | about 1,900 | about 3,000 | about 3,700 |
| last event, day | 7,392.52, unchanged | 7,353.10, unchanged | 7,394.74, unchanged |
| events | about 30,360 | about 133,300 | about 530,200 |

The maxima and the last refill's day are the mode of the longest of N
geometric chains (ln N / 0.0619 refills at 31.5 days each) and are
loose: one long chain moves them by hundreds of days. The last event
moves only if some chain passes about 234 refills, which has a chance
near 1,441 x 0.94^234, under one in a thousand at 28,024.

A merge stops no chain: the pending refill is carried to the survivor.
With chains now alive for a year and more, a few more steps will be
carried than the 11, 78 and 252 of the base; merges after day 110 are
rare enough (516 in twenty years at 28,024) that I expect single
figures more.

`osteoporosis-dispense`: predicted IDENTICAL without its 36,500-day
tail, since the latest instant that tail allowed is a hundred years
out and the root's horizon is far short of it.

A hazard seen while reading, not yet measured: a run ends when its
queue drains, and nothing else ends it. With no tail, a facility that
states `:refill-continuation 1` and a chronic order that never ends
describe a run that does not finish. The schema accepts a continuation
of 1 today.
