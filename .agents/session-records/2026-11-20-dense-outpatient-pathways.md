# dense-7500 authors outpatient visits: three weighted pathways, half of pathway arrivals

2026-11-20. Base `77cc8f54`. HEAD matched origin/main and the tree was
clean. `bin/preflight` exit 0, no finding.

Prompt: `.agents/prompts/2026-11-20-dense-outpatient-pathways.md`.

## Step 1: the prediction, written before any edit

**The edit predicted.** Three pathways join `config.edn`'s weighted
pool at weights 50 / 20 / 30 beside the existing 45 / 35 / 20, so each
arrival that draws a weighted pathway takes a clinic visit with
probability one half:

- `dense-clinic-acute`: outpatient visit, delay, one observation, one
  medication order, delay, visit end.
- `dense-clinic-chronic`: the same with a long-term medication.
- `dense-clinic-review`: the same with no order.

**The baseline, counted from the logs the 2026-11-19 drill left**
(seed 20260824, `--churn`, at `b6fb4ab3`; top-level events only):

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| arrivals that draw a weighted pathway (arrivals less the walker ordinals) | 657 | 6,563 | 27,087 |
| admissions opened by the three pathways (abdominal / chest / appendicitis) | 547 (247 / 184 / 116) | 5,387 (2,466 / 1,851 / 1,070) | 22,113 (10,072 / 7,682 / 4,359) |
| the same per weighted arrival | 0.833 | 0.821 | 0.816 |
| outpatient visits already in the log | 720 | 2,328 | 8,098 |
| of them `Follow-up` visits the `:scheduling` key books at a discharge | 674 | 1,929 | 7,626 |
| of them vendored modules' | 46 | 399 | 472 |
| orders written in an outpatient visit; `:community` fills | 3; 3 | 48; 48 | 59; 59 |
| orders written in an admission; `:inpatient` dispenses | 493; 491 | 4,943; 4,912 | 20,168; 20,121 |
| events | 33,819 | 172,867 | 691,758 |
| subjects (distinct participant ids) | 2,797 | 8,522 | 32,081 |

Two things in that table the prompt does not say. Dense already holds
outpatient visits, thousands of them: the follow-ups `:scheduling`
books and the modules' own. And only about 82 in 100 weighted arrivals
open an encounter at all: `:scheduling` books 70% of arrivals, and 8%
of bookings are cancelled and 15% are no-shows.

**Assumptions.** A clinic arrival is realised at the rate an admission
arrival is today (0.82), since `:scheduling` books an arrival whatever
its opener. The three existing pathways each lose half their arrivals.
A follow-up is booked only at a `:discharge` (read in
`decide :discharge`), so follow-up visits fall with the discharges:
about 0.24 follow-up visits per pathway discharge lost. Every order
written in a visit is filled once, at the visit's close, except the few
whose patient is merged away between the order and the close.

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| clinic visits, the three new pathways | 274 | 2,694 | 11,057 |
| acute / chronic / review | 137 / 55 / 82 | 1,347 / 539 / 808 | 5,528 / 2,211 / 3,317 |
| outpatient visits, all | about 930 | about 4,380 | about 16,500 |
| orders written in an outpatient visit: acute pathway | 137 | 1,347 | 5,528 |
| chronic pathway | 55 | 539 | 2,211 |
| review pathway | 0 | 0 | 0 |
| vendored modules | about 3 | about 48 | about 59 |
| `:community` fills | 185 to 200 | 1,850 to 1,960 | 7,600 to 7,850 |
| orders written in an admission | about 248 | about 2,486 | about 10,105 |
| `:inpatient` dispenses | 240 to 252 | 2,430 to 2,500 | 9,950 to 10,150 |
| community fills per inpatient dispense | about 0.78 | about 0.78 | about 0.77 |
| events | about 29,000 | about 125,000 | about 496,000 |
| subjects | 2,797, give or take 1% | 8,522, give or take 1% | 32,081, give or take 1% |

**The events row, at 7,500.** Half of the pathway stays go, and with
them (rounded): 2,690 admissions and as many discharges; 3,700
transfers, 1,070 cancelled transfers and 1,100 bed swaps, taking churn
on a stay to scale with the stays; 17,000 bed status changes; 10,290
order and result events; 2,457 admission orders, their dispenses and
2,446 ends; 3,925 observations; 2,694 procedures; 1,225 diagnostic
reports; 3,530 care-plan events; and about 2,300 events of follow-up
bookings and visits. Coming in: 5,388 visit openers and closers, 2,694
observations, 1,886 orders and 1,886 fills. Net about -47,700. The
other two cells scale that by their pathway admissions (0.1015 and
4.105). This row is the weakest: the bed-status and churn terms are
proportions, not counts.

The prompt expects about 13,500 visits and 9,500 orders at 28,024.
This prediction is lower, 11,057 and 7,739, by the bookings that never
become a visit.

**Not predicted, flagged.**

- Whether `--churn` and a clinic visit get along. No shipped scenario
  runs churn over an authored outpatient pathway at this size. A
  self-check failure here would be a finding, not a count.
- What a repeat arrival does when its pathway is a clinic visit and the
  person is already in a bed.
- The mutation gate. Its dense populations change shape; an operator
  may lose or gain its sites.
- Bed occupancy. Half the stays go, so census should be about half the
  README's table. Not derived further.

## Step 2: the edit

`2edf7a4e`. Three pathways added after `dense-surgical`; `git diff` on
`config.edn` removes two comment lines (the code-provenance list and
"the three weighted pathways" in the module cohort's comment, each
re-added with the new names) and no step of the existing three. The
three siblings were re-derived by the README's commands.

- `dense-clinic-acute`, weight 50: visit ("Sinusitis"), delay 5-15,
  body temperature, amoxicillin / clavulanate (`sinusitis.json`'s
  `562251`) cited `:antibiotic-start`, delay 5-20, visit end.
- `dense-clinic-chronic`, weight 20: visit ("Diabetes review"), the
  same shape, metformin (`metabolic_syndrome/medications.json`'s
  `860975`, which that module marks chronic) cited `:chronic-start`.
- `dense-clinic-review`, weight 30: visit ("Routine review"), the same
  shape with no order.

Gates: `sim check` exit 0 over the new 750 cell; two runs of it wrote
one log (sha-256 `bce6306d...`); every run exited 0, so
`incompatible-assignments` rejected no ordinal. Later in the drill,
every cell's warm-up and two timed runs wrote one byte-identical log.

A second config commit followed the measurement: a dated comment under
the ward block, saying the census it describes is now halved. Comment
only; the 750 log's digest was re-checked after it.

## Step 3: measured, beside the prediction

Seed 20260824, `--churn`, at `2edf7a4e`. Counted over top-level events.

| | 750 predicted | 750 measured | 7,500 predicted | 7,500 measured | 28,024 predicted | 28,024 measured |
|---|---|---|---|---|---|---|
| clinic visits, the three new pathways | 274 | 281 | 2,694 | 2,712 | 11,057 | 11,075 |
| acute / chronic / review | 137 / 55 / 82 | 135 / 60 / 86 | 1,347 / 539 / 808 | 1,342 / 534 / 836 | 5,528 / 2,211 / 3,317 | 5,472 / 2,262 / 3,341 |
| outpatient visits, all | about 930 | 947 | about 4,380 | 4,455 | about 16,500 | 16,932 |
| orders in a visit: acute pathway | 137 | 135 | 1,347 | 1,342 | 5,528 | 5,470 |
| chronic pathway | 55 | 60 | 539 | 534 | 2,211 | 2,262 |
| review pathway | 0 | 0 | 0 | 0 | 0 | 0 |
| vendored modules | about 3 | 3 | about 48 | 49 | about 59 | 59 |
| `:community` fills | 185 to 200 | 198 | 1,850 to 1,960 | 1,925 | 7,600 to 7,850 | 7,791 |
| orders written in an admission | about 248 | 290 | about 2,486 | 2,565 | about 10,105 | 10,375 |
| `:inpatient` dispenses | 240 to 252 | 287 | 2,430 to 2,500 | 2,533 | 9,950 to 10,150 | 10,332 |
| community fills per inpatient dispense | about 0.78 | 0.69 | about 0.78 | 0.76 | about 0.77 | 0.75 |
| events | about 29,000 | 29,701 | about 125,000 | 127,176 | about 496,000 | 504,233 |
| subjects | 2,797 | 2,797 | 8,522 | 8,522 | 32,081 | 32,081 |

`describe` counts one subject fewer in each cell (2,796; 32,080 at
28,024): it counts subjects of events, and this table counts every
participant id.

**Where the prediction was wrong.**

1. **Inpatient orders and dispenses, high by 2% to 17%.** The weighted
   draw gave `dense-inpatient` more than half of its old arrivals: at
   7,500 its admissions went 2,466 to 1,268 where 1,233 was predicted,
   and at 750, 247 to 145 where 123 was. `dense-fast` took the
   shortfall (1,851 to 921; 184 to 69). A draw, not a mechanism: the
   three stay pathways together are 2,721 against 2,694 predicted.
2. **750's ratio, 0.69 against 0.78**, for the same reason.
3. **Events, 1.7% to 2.4% above.** The follow-up term was too large
   (follow-up visits fell 593 at 7,500 where about 645 was predicted)
   and chatter rose with the reshuffle (`:demographic-update` +605,
   `:coverage-change` +393), which the prediction held flat.
4. **Subjects did not move at all**, in any cell. Predicted to within
   1%; they are the same count to the patient.

**The rest of step 3.**

- **Order-to-fill lag.** The fill lands at the instant the visit
  closes: close-to-fill is 0 seconds for all 1,925 fills at 7,500.
  Measured from the ORDER it is the pathway's second delay: 0 to 20
  minutes, median 12 (the zeros are the vendored modules', whose
  visits have no length).
- **Orders written in a visit and not filled: none**, in any cell.
  At 7,500 the log holds 4,455 visit openers and 4,452 closers; the
  three unclosed visits wrote no order that went unfilled.
- **Undispensed admission orders**: 32 at 7,500 (all vendored modules',
  stay closed first), 3 at 750 (2 modules', 1 merged away), 43 at
  28,024 (41 modules', 2 merged away). Lags inside the ward ranges:
  ED 5 / 15 / 25, inpatient 15 / 38 / 60 at 7,500.
- **`config-nobed.edn` at 7,500** holds the same order and dispense
  counts as `config.edn` (4,490 / 2,565 / 2,533 / 1,925).
- **Bed occupancy at 7,500 against the README's census.** The README's
  bound was Emergency 106.0, Medicine A 100.4, Surgery 173.3. Counted
  from `config-bare.edn`'s log over the middle nine tenths of its stay
  admissions, mean census (peak):

  | ward | bound, before | counted, before | bound, now | counted, now |
  |---|---|---|---|---|
  | Emergency | 106.0 | 123.8 (163) | 53.0 | 65.1 (90) |
  | Medicine A | 100.4 | 98.6 (127) | 50.2 | 48.7 (73) |
  | Surgery | 173.3 | 141.6 (185) | 86.6 | 69.2 (92) |

  Halved, as flagged. Surgery no longer reaches its surge rung (peak 92
  of 160 licensed, where it peaked at 185). The wards were left as they
  are. Under `:scheduling` the all-keys cell's mean census is 10.5 /
  7.7 / 12.3.

**The flagged unknowns.** Churn and a clinic visit get along: every
run exited 0 and self-checked clean. `:step-rejected` fell (192 to 73
at 7,500).

## Step 4: why `injuries` orders are never dispensed

The channel's guess is right. `injuries.json` opens an emergency
encounter, and each branch calls its prescribing submodules
(`medications/otc_pain_reliever`, `medications/moderate_opioid_pain_reliever`,
`medications/ear_infection_antibiotic`) as the last thing before its
`EncounterEnd`, with no delay state between. The engine opens an
emergency encounter as an admission, placed in Emergency. From the
census's all-`injuries` log: 214 module-cited admissions closed, 188
of them with an order, 210 orders, and all 210 carry their discharge's
instant. (109 of those stays are zero-length; the rest run a day to
six weeks, and the order is still at the closing instant.) Emergency's
turnaround range is `[5 25]` minutes, so the stay has always closed
when it elapses, and the inpatient half's rule leaves the order
undispensed. The community half fills only what an outpatient visit
wrote. These are ED-discharge prescriptions, which ADR-0187 and
ADR-0189 already name as owed. Nothing fixed.

## Step 5: figures, pins and docs

Drill as last time: warm-up and two timed runs per cell, Windows-side
load 1 to 17 at every start, every cell's three runs one byte-identical
log.

| cell | events | messages | msg/event | wall | peak RSS |
|---|---|---|---|---|---|
| `config.edn` 7,500 | 127,176 | 125,948 | 0.9903 | 134.18 s | 1,601 MiB |
| `config-nobed.edn` 7,500 | 101,659 | 100,487 | 0.9885 | 102.41 s | 1,493 MiB |
| `config-bare.edn` 7,500 | 67,780 | 40,207 | 0.5932 | 44.08 s | 887 MiB |
| `config.edn` 750 | 29,701 | 31,160 | 1.0491 | 63.05 s | 934 MiB |

The 28,024 cell, one run, `-Xmx8g`: 504,233 events, 32,080 subjects
(`describe`), 189,203,716 bytes, 363.49 s, 3,313 MiB, sha-256
`d50929d2616fa9834a4b1906747a9b3451bd4f8ca4d52dbcc36618dbc79b9c76`.
`26e31b4f...` is kept under `:pre-clinic`.

Two things about the walls. The all-keys wall ROSE (127.35 to 134.18 s)
on a log a quarter smaller; not explained. And the 750 cell's two
timed runs were 71.34 and 54.75 s, a wide pair; the mean is what the
method quotes.

`bin/demo-exerciser-dense-7500` rewrote `:asserted` to the drill's own
counts; its one FAIL was the tree-clean postcondition, with the docs
uncommitted. `describe` over the new 750 cell: 29,701 events, report
1.3.0. Over the 7,500 run's envelope the six pathway rows read
assigned 1,513 / 1,110 / 653 stay and 1,643 / 638 / 1,006 clinic,
which is 3,287 clinic of 6,563 weighted arrivals, 50.1%.

**What the suite showed moved** (one discovery run of `make test`,
`MAKE_EXIT=2`): two floors in `ehrt.sim.run-test`, both over the dense
750 cell, both `(not (<= 1018 550))`. Re-pinned to the measured 550,
and the outpatient floor raised from 1 to 282 (`c8190c1e`). **The
mutation gate did not move**: `brick:corpus` was green in that run with
its four populations as they are.

**Re-measured in the docs because they had moved**, beyond the figures
gate: the README's census passage, its carrier-column table (A 1,187,
B1 5,250, B2 2,528, C 1,787, D 929, by `operators.clj`'s own
`resolving-sites`), the bed cycle's share (25,517 events, 20.1%), and
in the consumer doc the dispense passage, one `describe` module row
(180 observed, 91 subjects) and two predicate counts (5,250; 24,952).

## Step 6: docs and the ADR

The scenario README has a new section, "The pathway mix, and where to
set it": the six weights, the word provisional, the AHA figures as
context. No ADR: a scenario edit. A dated addendum to ADR-0189 records
the change, the `injuries` trace and what is owed.

## What this prompt got wrong, and what the session found

1. **"Roughly 13,500 pathway arrivals take an outpatient visit, about
   9,500 write an order" at 28,024.** 11,075 and 7,732. About 13,400
   arrivals are ASSIGNED a clinic pathway; `:scheduling` books 70% of
   arrivals and cancellations and no-shows take 18% of the visits.
2. **"Inpatient dispenses fall to about half of 20,121."** 10,332,
   51%. Right.
3. **"Events fall."** By 27%, to 504,233.
4. **"Order-to-fill lag (expected zero under the shipped rule)."**
   Zero from the visit's close. From the order it is the pathway's
   second delay, 5 to 20 minutes.
5. **"The mutation gate's populations if an operator loses or gains
   its sites."** None did.
6. **Not in the prompt: dense already held thousands of outpatient
   visits**, the follow-ups `:scheduling` books at a discharge (1,929
   at 7,500 before, 1,336 now). They write no order.
7. **Not in the prompt: the ward sizing.** With half the stays gone
   the wards are sized for twice their traffic, and Surgery no longer
   reaches surge. Left alone and said so in the config and README.
8. **Not in the prompt: two floors in the sim brick** pinned to the
   dense 750 cell's inpatient volume.
9. **Not in the prompt: stale dated figures in the scenario README and
   the consumer doc** that earlier sessions had left (the
   carrier-column table from 2026-09-06, two predicate counts). All
   re-measured here.
10. **The front door's "at 10^5 events"** now sits over a keyless cell
    of 67,780 events. The sentence asks to be read as an order of
    magnitude and was left.
11. The channel's guess about `injuries` was right (step 4).
12. Held as stated: no engine change; walker ordinals unaffected (937
    assigned in both envelopes); every code already in the tree.

## Downstream line

`dense-7500` now holds authored outpatient visits and community fills:
at 7,500, 2,712 clinic visits, 1,925 `:community` fills and 2,533
`:inpatient` dispenses; at 28,024, 11,075 visits, 7,791 fills and
10,332 dispenses. Every cell's ground truth moved and shrank. Pin: the
tip of this session. Event schema 1.12.0, unchanged. New 28,024 log:
189,203,716 bytes, sha-256
`d50929d2616fa9834a4b1906747a9b3451bd4f8ca4d52dbcc36618dbc79b9c76`. The
mix is three weights in `demos/scenarios/dense-7500/config.edn`'s
`:pathways` (50 / 20 / 30 for acute, chronic and review visits, against
45 / 35 / 20 for the stays), all provisional; change them and re-derive
the three siblings by the README's commands. A retained corpus is a
valid artifact of the earlier pin.

## Close

Gates at `b012a2e5`, clean tree, nothing written into it while either ran:

- `make test`, unpiped to a log: `MAKE_EXIT=0`; poly execution time 32 minutes 14 seconds.
- `make integration`: `MAKE_EXIT=0`; poly execution time 21 minutes 16 seconds.

Commits over `77cc8f54`: `affd8630` (prediction), `2edf7a4e` (the three
pathways and siblings), `c8190c1e` (two sim-brick floors), `5b8439e3`
(config comment), `0894ee7b` (figures and docs), `b012a2e5` (ADR-0189
addendum, record, prompt archived), then this close.
Pushed `77cc8f54..2889a7eb`; `bin/post-push-verify` clean. CI run
44853039450 (`test`) green at `2889a7eb` -- the close marker.
