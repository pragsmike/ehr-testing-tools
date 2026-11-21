# A community fill is drawn: a share that is filled, a lag, and discharge prescriptions (ADR-0190)

2026-11-20. Base `7190220d`. HEAD matched origin/main and the tree was
clean. `bin/preflight` exit 0, no finding. Event schema 1.12.0.

Prompt: `.agents/prompts/2026-11-20-community-fill-share-lag-and-discharge-prescriptions.md`.

## Step 1: red tests

New: `ehrt.sim-engine.community-fill-draw-test` (seven tests) and two
schema tests in `ehrt.sim-model.config-test`. The first run could not
compile (`No such var: config/default-community-fill-share`), which
says nothing about behaviour, so the two options, their defaults and
their readers went into `sim-model` first and the engine tests were run
again against an unchanged engine. Red, 13 failures in six tests:

| test | red for |
|---|---|
| (a) share 1, range [30 90] | every lag 0; one distinct lag |
| (b) share 0 | 40 fills where none is expected |
| (c) defaults over 2,000 orders | share 1.0 outside (0.77, 0.83); every lag 0 |
| (d) order ended during the lag | filled at the close, before its end |
| (e) discharge prescription | 0 community fills where 10 are expected |
| a fill inside a later encounter | the fill lands at the close, before the admission |

Green by construction: (g), without the key the options change
nothing; the "dispensed inpatient, not filled again" and "ended in the
stay" arms of (e); `check-all` in (a), (d) and (e), which was ok on the
old logs too. (f), the schema tests, were red only as the compile
error.

## Step 2: the build

- **Options.** `:community-fill-share` (a number, 0 to 1) and
  `:community-fill-minutes` (`[lo hi]`, minutes, 0 <= lo <= hi) on the
  FACILITY, beside `:wards`. Defaults 0.80 and `[60 10080]`
  (`sim-model/default-community-fill-share`, `-minutes`), each read
  through one function. Validated by the `Facility` schema, as
  `:dispense-minutes` is; like that key, nothing at run time enforces
  the schema.
- **A visit's orders.** `decide :outpatient-visit-end` takes two draws
  per open order the visit wrote, on the patient's `:pharmacy` stream:
  filled at all, then the lag, both always taken. A filled order gets
  one queue entry at the close plus its lag, holding an internal
  `:community-fill` step. When that is decided the order must still be
  active and the patient alive.
- **Discharge prescriptions.** Decided where the queued
  `:pharmacy-dispense` step fires, not in `decide :discharge`: that
  step is the only thing that can dispense the order inpatient, so its
  firing is the first instant "never dispensed" is known. If the order
  is still active and its stay has been discharged, a `:community-fill`
  is queued at the discharge plus the lag. The two draws are taken at
  the order, so an admission order takes three draws where it took one.
- **No encounter id.** `encounters/stamp-encounter` now leaves every
  `:community` dispense unstamped. This is the one path that differs
  with the key absent: an AUTHORED community dispense placed inside an
  open encounter under `:encounters` used to be stamped. No shipped
  config does that.

Existing tests restated, each because the rule it pinned changed:
`dispense-derivation-test` runs ADR-0187's rule on a facility with
share 1 and lag `[0 0]`, which is that rule exactly; its draw test now
pins two draws per order; its erasure test runs at the defaults and
its fill counts are re-measured (134 of 156, 7 of 7, 134, 3 of 6).
`inpatient-dispense-derivation-test` pins three draws per order; its
outrun-by-discharge case and the root's `:outrun` orders are discharge
prescriptions. **The erasure law held throughout**: only the count
pins in that test went red, never the event-for-event comparison.

Gates: `brick:sim-engine` and `brick:sim-model` green; `poly check` 0.

## Step 3: the prediction, written before any dense run

dense-7500 will state both options at their defaults. From the
2026-11-20 outpatient-pathways record's counts:

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| orders written in an outpatient visit (unchanged) | 198 | 1,925 | 7,791 |
| community fills, visit orders | 148 to 168 | 1,500 to 1,570 | 6,150 to 6,300 |
| unfilled: the share's draw | 30 to 50 | 350 to 420 | 1,480 to 1,640 |
| unfilled: ended first | 0 | 0 | 0 |
| unfilled: merged away during the lag | 0 to 3 | 5 to 30 | 20 to 100 |
| unfilled: past the end of the run | 0 | 0 | 0 |
| admission orders undispensed at a discharge, before | 2 | 32 | 41 |
| community fills, discharge prescriptions | 0 to 2 | 20 to 30 | 28 to 38 |
| lag, minutes (min / median / max) | about 60 / 5,000 / 10,080 | 60 / about 5,070 / 10,080 | 60 / about 5,070 / 10,080 |
| fills carrying an `:encounter-id` | 0 | 0 | 0 |
| fills landing inside a later encounter | a handful | tens | under 200 |
| `:inpatient` dispenses | 287 | 2,533 | 10,332 |
| events | about 29,660 | about 126,800 | about 502,700 |

Reasons. The clinic pathways never end an order, so none ends first.
Nothing falls off the end: the run loop drains its queue, so a fill is
emitted however late it lands. Inpatient dispense COUNTS do not move:
every dense order that was dispensed has a turnaround shorter than the
time to its stay's close whatever the draw. Their instants do move for
a stay's second order, which now sits three draws further down the
stream. Nothing else in ground truth moves (the erasure law); messages
move by a handful, as under ADR-0189.
