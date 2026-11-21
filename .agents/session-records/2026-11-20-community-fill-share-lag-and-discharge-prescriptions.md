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

## Step 3: measured, beside the prediction

Seed 20260824, `--churn`, at `10ae3f71` (the engine change at
`c67d4a6f`, dense's two options written out at `10ae3f71`). Top-level
events only.

| | 750 predicted | 750 measured | 7,500 predicted | 7,500 measured | 28,024 predicted | 28,024 measured |
|---|---|---|---|---|---|---|
| orders written in an outpatient visit | 198 | 198 | 1,925 | 1,925 | 7,791 | 7,791 |
| community fills, visit orders | 148 to 168 | 152 | 1,500 to 1,570 | 1,463 | 6,150 to 6,300 | 5,961 |
| filled share of visit orders | 0.80 | 0.768 | 0.80 | 0.760 | 0.80 | 0.765 |
| unfilled, patient merged away within a week of the close | 0 to 3 | 6 | 5 to 30 | 66 | 20 to 100 | 224 |
| unfilled, nothing else in the log to say why | 30 to 50 | 40 | 350 to 420 | 396 | 1,480 to 1,640 | 1,606 |
| unfilled: ended first | 0 | 0 | 0 | 0 | 0 | 0 |
| unfilled: past the end of the run | 0 | 0 | 0 | 0 | 0 | 0 |
| discharge prescriptions (active, undispensed at a discharge) | 2 | 2 | 32 | 32 | 41 | 41 |
| community fills, discharge prescriptions | 0 to 2 | 0 | 20 to 30 | 27 | 28 to 38 | 33 |
| lag from the close, visit fills, minutes (min / median / max) | 60 / 5,000 / 10,080 | 76 / 5,423 / 10,044 | 60 / 5,070 / 10,080 | 63 / 4,990 / 10,078 | 60 / 5,070 / 10,080 | 63 / 4,961 / 10,078 |
| lag from the discharge, discharge prescriptions | -- | -- | -- | 1,238 / 7,230 / 10,042 | -- | 1,238 / 5,775 / 9,741 |
| fills carrying an `:encounter-id` | 0 | 0 | 0 | 0 | 0 | 0 |
| fills landing inside a later encounter | a handful | 0 | tens | 1 | under 200 | 15 |
| `:inpatient` dispenses | 287 | 287 | 2,533 | 2,533 | 10,332 | 10,332 |
| community fills, all | -- | 152 | -- | 1,490 | -- | 5,994 |
| events | about 29,660 | 29,655 | about 126,800 | 126,741 | about 502,700 | 502,436 |

**Where the prediction was wrong.**

1. **Merges take three to four prescriptions in a hundred, not one.**
   Visit fills came in below the predicted range at 7,500 and 28,024,
   and the filled share is 0.76 to 0.77. A patient absorbed by a merge
   during the lag takes the queued fill with them. The log does not
   record a draw, so it cannot separate "the draw said no" from "merged
   first" for a patient who was merged: the merged row is an upper
   bound on the merges' own share (about a fifth of those rows the draw
   would have left unfilled anyway), and the row under it a lower bound
   on the draw's. At 7,500: 0.80 x 1,925 = 1,540 expected, 1,463
   filled, 66 merged within the week.
2. **Fills inside a later encounter: 0, 1 and 15**, far under "tens"
   and "under 200". Few patients are back within a week.

The rest held: the discharge prescriptions are exactly the earlier
record's stay-closed-first orders (2 / 32 / 41), all vendored modules';
inpatient dispenses did not move by one; nothing ended first; nothing
fell off the end.

**What else moved: nothing in ground truth.** The 750 and 7,500 logs
before (`2edf7a4e`) and after, with their dispenses removed and their
`*-event-id` index fields dropped, are the same events in the same
order (29,216 and 122,718 events). `config-bare.edn`'s log is
byte-identical to the one before (`712b951f...`). `config-nobed.edn`
holds the same orders, dispenses and fills as `config.edn`. The
erasure test stayed green for ground truth.

**The inpatient half's instants.** A stay's second order sits three
draws further down the `:pharmacy` stream than it did, so its
turnaround is a different number. The spread is unchanged: ED 5 / 15 /
25, inpatient 15 / 38 / 60 at 7,500.

## Step 4: what the suite showed moved

**Oracle roots, predicted first:** the two that set the key,
`osteoporosis-dispense` and `dispense-inpatient`, and no other.
`bin/regression-oracle 7190220d 27ee84b6 --declared-digest-change` and
`bin/ground-truth-bracket` likewise: both exit 1 with a DIFFERS banner,
and in both the per-root lines differ for exactly those two roots.
`osteoporosis-dispense` `f463b5be...` to `aacf9366...`,
`dispense-inpatient` `5351110c...` to `cd5c606c...` (the oracle's
digests). `27ee84b6` updates both docstrings, and gives
`dispense-inpatient` share 1 and `[120 240]` so its three outrun orders
are discharge prescriptions by construction.

**Figures**, by the usual drill (warm-up and two timed runs per cell,
Windows-side load 0 to 14 at every start, every cell's three runs one
byte-identical log):

| cell | events | messages | msg/event | wall | peak RSS |
|---|---|---|---|---|---|
| `config.edn` 7,500 | 126,741 | 125,961 | 0.9938 | 105.08 s | 1,687 MiB |
| `config-nobed.edn` 7,500 | 101,224 | 100,498 | 0.9928 | 93.17 s | 1,487 MiB |
| `config-bare.edn` 7,500 | 67,780 | 40,207 | 0.5932 | 41.25 s | 1,114 MiB |
| `config.edn` 750 | 29,655 | 31,165 | 1.0509 | 54.22 s | 1,082 MiB |

The 28,024 cell, one run, `-Xmx8g`: 502,436 events, 32,080 subjects
(`describe`), 188,719,656 bytes, 325.65 s, 3,215 MiB, sha-256
`bd4dec16e8358184c3a3fc0f3b7bf93c91d322fd04ca206acbcf2dcd24c5222f`.
`d50929d2...` is kept under `:pre-adr-0190`. The all-keys wall is 22%
below the one measured this afternoon on a log that barely moved;
walls here are not good to better than that.

`bin/demo-exerciser-dense-7500` rewrote `:asserted` to the drill's own
counts; its one FAIL was the tree-clean postcondition, with the docs
uncommitted. `describe` over the new 750 cell: 29,655 events, 2,796
subjects, inpatient row observed 287, community row observed 152,
report 1.3.0.

Pins and mutation populations: see Close for what the full suite
showed.

## Step 5 and 6: docs and the ADR

`components/sim/docs/operational-models.md` has a new section, "The
community pharmacy (ADR-0190)", beside "Pharmacy turnaround": the two
options, defaults, the four sources, the word provisional, the
uniform-lag caveat, and the ways a prescription goes unfilled. The
consumer doc and `future-features.md` say what is now true, including
that a prescription with no fill is not an error. ADR-0190, Accepted,
amends ADR-0187 and ADR-0189.

## What this prompt got wrong, and what the session found

1. **"At `decide :discharge`, each of the patient's orders ... with no
   dispense."** The state does not record that an order was dispensed,
   and finding out at the discharge means a new index or a log scan.
   The rule is decided where the queued pharmacy step fires, which is
   the only thing that can dispense the order: if the order is still
   active then and the stay discharged, it is a discharge
   prescription. Same orders, same lag from the discharge.
2. **"Both draws ... in a fixed order and number per order."** Held,
   with a cost the prompt did not name: an admission order's two
   community draws are taken at the order, so every admission order
   takes three draws and later inpatient turnarounds of the same
   patient are different numbers. Counts did not move.
3. **"A lag past the run's last event is not emitted; report how many
   fall off the end."** None can: the run loop drains its queue.
4. **"Validated as `:dispense-minutes` is."** True, and that means by
   the schema only. Nothing validates a run's facility against it.
5. **"Make it impossible if the tree allows it cheaply."** It did: one
   clause in `stamp-encounter`. It applies to authored community
   dispenses too, the one path that differs with the key absent.
6. **Not in the prompt: merges during the lag** cost three to four
   fills in a hundred (above).
7. **Not in the prompt: the two keyed oracle roots' meaning.**
   `dispense-inpatient` would have filled its outrun orders by a draw;
   it now states its options so the case is by construction.
8. **Not in the prompt: a cancelled discharge and a death during the
   lag.** Both decline the fill.
9. Held as stated: no schema change; the erasure law; inpatient
   dispenses unchanged in count; defaults 0.80 and `[60 10080]`.

## Downstream line

Under `:dispense`, a community fill is now a draw. A prescription
written at an outpatient visit is filled with probability
`:community-fill-share` (default 0.80), `:community-fill-minutes`
(default 60 to 10,080) after the visit closes; an admission order
still active and undispensed at its discharge is filled the same way,
counted from the discharge. Both are facility options, and
`dense-7500` writes both out. **A `:medication-order` with no
`:medication-dispense` is not an error, and a fill can trail its visit
by a week.** A community fill never carries `:encounter-id`. `dense-7500`
at 7,500: 1,490 community fills (1,463 of 1,925 visit orders, 27 of 32
discharge prescriptions) and 2,533 inpatient dispenses; at 28,024,
5,994 and 10,332. Nothing but dispenses moved. Pin: the tip of this
session. Event schema 1.12.0, unchanged. New 28,024 log: 188,719,656
bytes, sha-256
`bd4dec16e8358184c3a3fc0f3b7bf93c91d322fd04ca206acbcf2dcd24c5222f`. A
retained corpus is a valid artifact of the earlier pin.
