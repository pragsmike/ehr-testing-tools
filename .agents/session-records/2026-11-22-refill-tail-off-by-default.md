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

## Step 2: red tests

Red at the base (`0d8e7067`, tests only), for the stated reasons:

    FAIL in (refill-options-are-the-facilitys-own-or-the-defaults) (config_test.clj:134)
    expected: (nil? (resolve (quote ehrt.sim-model.config/default-refill-tail-days)))
      actual: (not (nil? #'ehrt.sim-model.config/default-refill-tail-days))
    FAIL in (refill-options-are-the-facilitys-own-or-the-defaults) (config_test.clj:138)
    expected: (nil? (config/refill-tail-days config/default-facility))
      actual: (not (nil? 90))
    FAIL in (with-no-tail-stated-the-mean-chain-is-the-continuation-draws-own) (chronic_refill_test.clj:205)
    mean refills per chain 1.9222462203023758
    expected: (< (- expected 2.5) mean (+ expected 2.5))
      actual: (not (< 13.166666666666652 1.9222462203023758 18.16666666666665))
    FAIL in (with-no-tail-stated-a-chain-runs-past-day-ninety-and-stops-only-at-its-orders-end) (chronic_refill_test.clj:186)
    expected: (= [1 2 3 4 5 6 7] (map :fill-number fills))
      actual: (not (= [1 2 3 4 5 6 7] (1 2 3)))
    FAIL in (with-no-tail-stated-a-chain-runs-past-day-ninety-and-stops-only-at-its-orders-end) (chronic_refill_test.clj:188)
    expected: (> (:t (last fills)) (+ last-arrival (* 90 day)))
      actual: (not (> 5188800 7777200))

The last two repeat once per order, five times each.

| the prompt's test | where | at the base |
|---|---|---|
| (a) no tail, continuation 1: past day 90, stopped only by the order's end | `chronic-refill-test`, new | red |
| (b) no tail, default continuation: mean chain within 2.5 of 0.94 / 0.06 = 15.67 | `chronic-refill-test`, new, 600 patients | red (1.92) |
| (c) a stated tail still stops a chain where it did | `a-chronic-orders-fill-is-number-one-and-is-refilled-each-interval-until-the-tail`, unchanged | green by construction |
| (d) the schema accepts an absent tail and refuses a negative one | `config-test`; the reader's nil and the missing default are the red half | schema half green by construction |
| (e) two runs at one revision are identical | inside (b), on the no-tail config | green by construction (asserted on a red test) |

The tolerance in (b): one chain's refill count has a standard
deviation near 16.2, some 480 chains are filled, so the mean's is near
0.74 and 2.5 is about three of those. Measured after the change: the
test is green.

## Step 3: the change

- `sim-model`: `default-refill-tail-days` is gone, from `config` and
  from the interface. `refill-tail-days` returns the facility's own
  value or nil. The schema entry is unchanged: optional, a
  non-negative integer when present, and a stated `nil` is still
  refused (leave the key out).
- `sim-engine`: `run` sets the world's `:refill-horizon-t` to nil when
  no tail is stated; `refill-followup` treats nil as no bound. The two
  draws are still taken first, so no draw moves.
- `event_schema.clj` and its three resources are untouched;
  `event-schema-test`'s `classify-change` of the frozen baseline
  against the live schema is green and reports no change.
- `dense-7500`: all four configs drop the line. The README's three
  re-derived literals moved with the comment's line count
  (`2220d2219`, 160,789 bytes, `8e4c3aef...`).
- `osteoporosis-dispense` drops its `:facility`.

**Tests that pinned the old default without saying so.** Three tests in
`dispense-derivation-test` run `osteoporosis` under `:dispense` on the
default refill options. Its orders are chronic, and under the 90-day
default nothing was refilled over its hundred-year horizon, so "one
fill per order" held by accident of the tail. Two of them are tests of
ADR-0187's first-fill rule and their facility now states
`:refill-continuation 0`. The third is the erasure law, which should
hold under real draws: it now holds over 2,263 fills where it held
over 134, and that is the stronger test.

## Step 5, first: the oracle's mover set, predicted before the instruments ran

Two roots run with `:dispense`. `dispense-inpatient` writes no chronic
order. `osteoporosis-dispense` drops a tail that bounded nothing.
**Predicted: no root moves, 46 of 46 IDENTICAL**, and no declaration
is owed.

## Step 4: measured, beside the prediction

Seed 20260824, `--churn`, at `5860f8b7`. Causes are counted from a
scratch copy of that tree with `prn` lines in `refill-followup`,
`decide :community-fill` and the run loop's merge branch.

| | 750 predicted | 750 measured | 7,500 predicted | 7,500 measured | 28,024 predicted | 28,024 measured |
|---|---|---|---|---|---|---|
| refills | about 700 (500 to 900) | 647 | about 6,510 (5,900 to 7,100) | 6,805 | about 27,600 (26,400 to 28,900) | 27,638 |
| refills per filled chronic order, mean | 14.5 | 13.48 | 15.4 | 16.05 | 15.3 | 15.36 |
| median | 11 | 12 | 11 | 12 | 11 | 10 |
| max | about 60 | 50 | about 95 | 122 | about 118 | 123 |
| filled chronic orders | 48 | 48 | 424 | 424 | 1,799 | 1,799 |
| chains stopped by the draw | 48 | 48 | 419 | 419 | 1,783 | 1,783 |
| chains stopped by a newer order | 0 | 0 | 5 | 5 | 16 | 16 |
| by the order's end / a death / a merge | 0 | 0 | 0 | 0 | 0 | 0 |
| last refill, day | about 1,900 | 1,609.10 | about 3,000 | 3,922.79 | about 3,700 | 3,947.89 |
| refills after day 110 | -- | 544 | -- | 5,856 | -- | 24,016 |
| last event, day | 7,392.52 | 7,392.52 | 7,353.10 | 7,353.10 | 7,394.74 | 7,394.74 |
| events | about 30,360 | 30,307 | about 133,300 | 133,594 | about 530,200 | 530,238 |
| community fills, all | -- | 803 | -- | 8,343 | -- | 33,794 |
| inpatient dispenses | 288 | 288 | 2,533 | 2,533 | 10,334 | 10,334 |
| visit prescriptions filled | 156 | 156 | 1,511 | 1,511 | 6,122 | 6,122 |
| pharmacy steps carried at a merge | 11, a few more | 11 | 78, a few more | 78 | 252, a few more | 252 |
| dispenses whose subject is the survivor of the order's patient | not predicted | 94 | not predicted | 904 | not predicted | 2,731 |
| of them refills | -- | 89 | -- | 857 | -- | 2,572 |

No chain outran the log: the last refill is on day 3,948 of 7,395 at
28,024, and the last event did not move in any cell.

Where the prediction was wrong, and what it did not see.

1. **The longest chains are longer than the mode I quoted** at 7,500
   and 28,024 (122 and 123 refills against 95 and 118), and the last
   refill is later by the same amount. I said those rows were loose;
   they were.
2. **No more steps are carried at a merge, not "a few more".** The
   count is the base's exactly. Every carry happens in the first weeks,
   as before.
3. **Dispenses after a merge rose sixfold, which I did not predict**:
   470 to 2,731 at 28,024. A chain carried to a survivor in its first
   weeks now runs for a year or more on the survivor, and every one of
   those refills has a subject that is not its order's. About one
   refill in eleven is such an event.
4. **One later prescription of the same patient can move.** The
   channel expected that only the chains the tail stopped would change.
   Every chain's own draws do stay where they were: at 750 and 7,500
   and 28,024 every other order's old fills are a prefix of its new
   ones, 37, 345 and 1,441 chains are longer, and the log less its
   dispenses is unchanged. But a patient has ONE `:pharmacy` stream. A
   chain that now runs on takes draws the tail used to leave untaken,
   so a prescription the same patient is written later draws its share
   and its lag further down the stream. Measured: at 7,500 one such
   prescription (a `sore_throat` order on day 1,159) is filled 2.1 days
   earlier than before; at 28,024 one (an opioid order on day 1,033) is
   filled 3.2 days later, and one first fill exists that did not (hence
   530,238 events where the refills alone give 530,237). None at 750.

Held: the refill totals, the draw's and the newer order's counts
exactly, the last event.

**`osteoporosis-dispense`**: the oracle instruments below.

## Step 5: oracle roots, measured

`bin/regression-oracle b1186f18 0b8ba8f0` and `bin/ground-truth-bracket
b1186f18 0b8ba8f0`, bare, both stopped at their soundness check with
exit 1: `digest.clj` differs outside its leading docstring, because
`osteoporosis-dispense` dropped its `:facility` line. That is the
instrument doing its job, and I had not predicted it. Both were rerun
with `--declared-digest-change`; the declaration is of that one source
line and of no digest.

    bin/regression-oracle:    IDENTICAL: every root's digest matches between b1186f18 and 0b8ba8f0   (exit 0)
    bin/ground-truth-bracket: IDENTICAL: every digested root's :ground-truth matches between b1186f18 and 0b8ba8f0 (43 roots)   (exit 0)

`osteoporosis-dispense` before and after: `c343671c...` on the first
instrument, `6f061987...` on the second, the same on both sides. The
36,500-day tail bounded nothing, measured.

The prediction held: no root moves.

## Step 5: figures, describe, pins

The usual drill, 2026-11-22 at `0b8ba8f0`: one warm-up and two timed
runs per cell, `/usr/bin/time -v`, Windows-side load sampled before
every run (0 to 12 over the twelve samples, 14 before the 28,024 run).
Nothing else ran on the host. Every cell's three runs wrote one
`events.edn`.

| cell | events | messages | msg/event | wall | peak RSS |
|---|---|---|---|---|---|
| `config.edn` 7,500 | 133,594 | 125,947 | 0.9428 | 103.23 s | 1,614 MiB |
| `config-nobed.edn` 7,500 | 108,077 | 100,467 | 0.9296 | 90.81 s | 1,473 MiB |
| `config-bare.edn` 7,500 | 67,780 | 40,207 | 0.5932 | 39.46 s | 1,034 MiB |
| `config.edn` 750 | 30,307 | 31,162 | 1.0282 | 52.12 s | 1,039 MiB |

28,024, one run, `-Xmx8g`: 530,238 events, 32,080 subjects,
197,001,464 bytes, 320.55 s, 3,573 MiB, sha-256
`37582a56807463e7dd64395555e22a6e82522c5fa83f4add8686eca460827b46`.
`dd4516c0...` is kept in `figures.edn` under `:pre-no-tail`, labelled
as the value before this change.

The bare cell's log is byte-identical to yesterday's (`9f8b2141...`).
Messages moved by +5, -12, 0 and +4 with no wire rendering of a
dispense, as on every earlier change to this key: an added event moves
the emitter's draws.

`ehrt sim describe` (1.4.0, unchanged) over the new logs:

| | `config.edn` 7,500 | nobed 7,500 | bare 7,500 | `config.edn` 750 | 28,024 |
|---|---|---|---|---|---|
| events | 133,594 | 108,077 | 67,780 | 30,307 | 530,238 |
| subjects | 8,521 | 8,521 | 7,500 | 2,796 | 32,080 |
| `:medication-dispense-inpatient` | 2,533 | 2,533 | 0 | 288 | 10,334 |
| `:medication-dispense-community` | 8,343 | 8,343 | 0 | 803 | 33,794 |
| `:medication-refill` | 6,805 | 6,805 | 0 | 647 | 27,638 |
| `:dispense-after-merge` | 904 | 904 | 0 | 94 | 2,731 |

The refill measure needed no change: it counts a `:fill-number` above
1 and agrees with the scratch count in every cell.

Pins: none of the invariant count, the describe version or the event
schema moved, and `bin/usecase-ground-truth-oracle` is untouched. What
moved by hand: the three README literals the exerciser re-derives, the
two first-fill tests' facility, and the erasure test's expected fills
(step 3).

## Step 6: docs and the addendum

- `components/sim/docs/operational-models.md`: the refill section says
  the tail is optional and absent by default, what stops a chain, why
  a long-horizon scenario should leave the tail out, that a run
  without one ends when its chains do, and that a continuation of 1
  wants a tail.
- `docs/consuming-ground-truth.md`: the option row, the refill
  passage, a new paragraph on refills running for years, the dense
  counts, the Scale table and its history.
- `docs/future-features.md`, the root `README.md` (0.9428), the
  scenario README and `figures.edn`.
- **An addendum to ADR-0191, not a new ADR.** The tree's rule
  (the de-scaffold ruling of 2026-08-25) wants an ADR for a design
  decision and none for process. This is a design decision, but it
  reverses one clause of an ADR a day old on that ADR's own measured
  finding, and the ADR listed it under "Owed next". A reader who lands
  on ADR-0191 must not be told the default is 90; a second ADR would
  leave that text standing. So the ADR carries an "Amended" line under
  its status and the addendum at its foot.

## Premises the prompt got wrong

1. **"Every chain the draw or an end stopped under the 90-day tail is
   unchanged; the chains the tail stopped run on."** True of the
   chains. Not the whole of what moves: a patient has one `:pharmacy`
   stream, so a later prescription of a patient whose chain now runs
   on draws further down it. One fill moved at 7,500; one moved and
   one appeared at 28,024.
2. **"`osteoporosis-dispense` drops its 36,500; its log should be
   IDENTICAL."** It is. But both instruments refuse to run bare on the
   change, since the root's source line is itself a difference in
   `digest.clj`; the declaration flag is owed for the source, not for
   a digest.
3. **The change is confined to "the option's reader, default and
   docstrings, and the two configs."** Three engine tests ran
   `osteoporosis` under `:dispense` on the default and pinned one fill
   per order. They had been green by accident of the tail.
4. **"A chain also ends when its patient ... is merged away without a
   survivor to carry it."** There is no such case in the code: the run
   loop reaches that branch only for a `:merged` record, and a merged
   record always names its survivor.
5. **"dense 28,024 goes from 3,596 refills to something like
   27,000."** 27,638. The arithmetic was right; it is listed because
   the prompt asked for it to be measured.
6. **Not a premise, a consequence nobody stated:** dispenses whose
   subject is not their order's go from 470 to 2,731 at 28,024, and a
   continuation of 1 with no tail describes a run that does not end.

## Downstream line

**Event schema 1.13.0, unchanged.** `:refill-tail-days` is now
optional and absent by default: under `:dispense`, a chronic
prescription is refilled every 28 to 35 days until its 0.94 draw says
stop or its order ends, and no longer only within 90 days of the last
arrival. `dense-7500` states no tail. **Expect many more refills,
spread over years**: at 7,500, 8,343 community fills, 6,805 of them
refills, the last on day 3,923; at 28,024, 33,794 and 27,638, the last
on day 3,948. A filled chronic order is refilled 15 to 16 times on
average, up to 123. **About one dispense in twelve now names an order
written under a record since merged into its patient** (904 at 7,500,
2,731 at 28,024). Nothing but dispenses moved in any dense log; the
no-key cell is byte-identical. Pin: the tip of this session. New
28,024 log: 197,001,464 bytes, sha-256
`37582a56807463e7dd64395555e22a6e82522c5fa83f4add8686eca460827b46`. A
corpus retained from the earlier pin (`dd4516c0...`) is a valid
artifact of it. A scenario that wants the old behaviour states
`:refill-tail-days 90` on its facility.
