# Chronic orders refill; a pending fill follows its patient through a merge (ADR-0191, event schema 1.13.0)

2026-11-21. Base `29668820`. HEAD matched origin/main and the tree was
clean. `bin/preflight` exit 0, no finding. Event schema 1.12.0 at the
base.

Prompt: `.agents/prompts/2026-11-21-chronic-refills-and-fills-through-merges.md`.

## Step 1: measured before any edit

The tree was not touched. The base was exported with `git archive` to
a scratch directory and two `prn` lines were added there, to
`sim-engine/run.clj` only: the last arrival's time, and every
`:pharmacy-dispense` or `:community-fill` step the run loop drops
because its patient has been merged away. The three dense cells were
then run from that copy (seed 20260824, `--churn`). Their logs have the
shipped shas (`bd4dec16...` at 28,024), so the instrument moved
nothing.

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| first event, seconds | 0 | 0 | 0 |
| last arrival, seconds (days) | 46,380 (0.54) | 457,620 (5.30) | 1,691,820 (19.58) |
| last event, days | 7,392.52 | 7,353.10 | 7,394.74 |
| events after the last arrival | 26,609 of 29,655 | 88,546 of 126,741 | 286,660 of 502,436 |
| merges | 66 | 535 | 2,105 |
| pending `:community-fill` steps dropped at a merge | 3 | 48 | 157 |
| of them, order still active | 3 | 47 | 157 |
| pending `:pharmacy-dispense` steps dropped at a merge | 1 | 0 | 2 |
| orders written in an outpatient visit | 198 | 1,925 | 7,791 |
| of them filled | 152 (0.768) | 1,463 (0.760) | 5,961 (0.765) |
| orders of `dense-clinic-chronic` | 60 | 534 | 2,262 |
| of them filled | 48 | 406 | 1,747 |
| orders from a vendored module's chronic state | 0 | 0 | 0 |

Four things the prompt did not expect.

1. **The log already runs twenty years past the last arrival.** The
   arrival clock is short (half a day at 750, twenty days at 28,024),
   but `:persons {:years 20}` and the scheduled follow-ups put most of
   the log after it: births, later admissions and visits, demographic
   and coverage changes. So a 90-day tail does not lengthen a dense log
   at all, and the tail of the log does not hold refills only. Nearly
   every medication order, though, is written in the first thirty days.
2. **Merges drop fewer fills than ADR-0190's upper bound said**: 157 at
   28,024, not 224. The 224 counted every unfilled prescription whose
   patient was merged within a week of the close; about a fifth of
   those the draw had already left unfilled.
3. **Every dropped inpatient step (3 in all) belongs to a stay that was
   open at the merge, with the survivor admitted.** None is a discharge
   prescription in waiting.
4. **No vendored chronic order reaches a dense log.** The four modules
   that mark a state `chronic` (`osteoporosis`, the two inhaler
   submodules, `metabolic_syndrome/medications`) write none in these
   cells. Every refill dense will show comes from `dense-clinic-chronic`.

## Step 1: the prediction, written before any edit

With a tail of 90 days the cutoff is day 90.5, 95.3 and 109.6. A
chronic first fill lands between day 0 and about day 30 (median 11.8,
13.4, 21.7), and a refill follows every 28 to 35 days. So nearly every
chain has room for two refills, about a quarter of the 28,024 chains
for a third, and almost none for more. Each refill is followed by
another with chance 0.94.

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| visit prescriptions filled, after the merge fix | 155 | about 1,510 | about 6,118 |
| filled share of visit prescriptions | 0.783 | 0.784 | 0.785 |
| inpatient dispenses | 288 | 2,533 | 10,334 |
| chronic orders | 60 | 534 | 2,262 |
| chronic orders with a first fill | 49 | about 419 | about 1,792 |
| refills | 80 to 96 | 730 to 790 | 3,500 to 3,780 |
| refills per filled chronic order | 1.80 | 1.81 | 2.03 |
| most fills of one order | 3, rarely 4 | 3, rarely 4 | 4 |
| chains stopped by the tail | about 43 | about 370 | about 1,580 |
| chains stopped by the draw | about 6 | about 49 | about 210 |
| chains stopped by an end | 0 | 0 | 0 |
| last event, days | 7,392.52 | 7,353.10 | 7,394.74 |
| events | about 29,750 | about 127,550 | about 506,200 |

Reasons. The share of 0.80 is not reached even with the merge fix
because the draw itself came in low (1,510 of 1,925 at 7,500 is 1.7
standard deviations under 1,540). First fills take no new draw, so
their count moves only by the fills a merge used to drop. The inpatient
figures add the three carried dispenses. Nothing in dense ends a
chronic order.

## Step 2: red tests

New: `ehrt.sim-engine.chronic-refill-test` (nine tests); two schema
tests in `ehrt.sim-model.config-test` and one in
`ehrt.sim-model.pathway-test`; two in
`ehrt.sim-check.check-test`; two in
`ehrt.patient-simulator.compile-trajectory-test`. The three facility
options, their defaults and readers, and the pathway step's `:chronic`
went into `sim-model` first so the engine tests could compile; they
were then run against an unchanged engine. Red, 44 failures in all
nine engine tests:

| test | red for |
|---|---|
| (a) refilled each interval until the tail | no `:chronic` on the order event; fill numbers all nil; one fill per order |
| (b) continuation 0 | fill number nil |
| (c) non-chronic, unfilled, inpatient, discharge prescription | fill number nil; one fill where four are expected |
| (d) ended between fills | one fill, unnumbered, where two are expected |
| (e) defaults over 600 chronic orders | no fill followed by another; no interval |
| (f) a fill pending at a merge | no fill at all: the step died with its patient |
| an inpatient dispense pending at a merge | no dispense at all |
| (g) a vendored module's flag | no `:chronic` on any of the 156 order events |
| authored dispenses numbered | fill number nil |

One assertion in (g) was wrong as written and was corrected before
green: it named the state `:Prescribe_Bisphosphate`, and a citation
carries the loader's own key, `:prescribe-bisphosphate`.

Green by construction: `check-all` in every test (it was ok on the old
logs); "a chronic prescription that is never filled is never
refilled"; (i), the schema tests, which went in with the options. (h)
is two things: the event schema now requires `:fill-number`, so the
existing schema tests over the fixture fleet and the generated-run
property are what hold "every dispense in every fixture has one"; and
the numbering invariant's own test, written with the invariant.

`poly test` stops a brick at its first failing namespace, so later
failures surfaced one run at a time. Two existing tests were restated:
`engine-test`'s authored inpatient dispense pinned "there is no fill
number" and now pins fill number 1; `ehrt.sim.run-test`'s pinned
invariant catalog gains the new row.

## Step 3: what was built

- **Marker.** The loader declares `chronic` on a MedicationOrder state
  (it already kept the key, undeclared and unread). The interpreter
  puts `:chronic true` on the trajectory event, the compile on the
  step, `decide` on the event, `evolve` on the order's record. Absent
  everywhere when not chronic. A pre-horizon order's registration fact
  does not carry it.
- **Fill number.** Every `:medication-dispense` carries `:fill-number`.
  The subject's state counts dispenses per order (`:dispense-counts`,
  keyed by the order's log index); `decide :medication-dispense` writes
  one more than the count.
- **Chain.** `decide :community-fill`, once it has emitted a fill of a
  chronic order, takes two draws on the ordering patient's `:pharmacy`
  stream and queues the same step again at the fill's time plus the
  interval, unless the draw says stop or the instant is past the
  world's `:refill-horizon-t` (last arrival plus the tail). A refill
  declines on the same tests a first fill does.
- **Three facility options**: `:refill-interval-days` `[28 35]`,
  `:refill-continuation` `0.94`, `:refill-tail-days` `90`.
- **Merge.** The run loop re-queues a pending `:pharmacy-dispense` or
  `:community-fill` on the survivor, marked with the patient who wrote
  the order. `decide` reads the order and its stay off that record and
  emits for the survivor. The dispense law resolves the order's patient
  through merges, as the result's law does.
- **New invariant**:
  `medication-dispense-fill-numbers-count-up-from-one`.
- **Schema 1.13.0.** `classify-change` against the frozen 1.12.0
  baseline, before the re-freeze:
  `{:additive? false, :breaking [":medication-dispense: new REQUIRED key: :fill-number (add it as {:optional true} to stay additive)"]}`.
  Baseline re-frozen, export and examples regenerated.

Gates: `brick:sim-engine`, `brick:sim-model`, `brick:sim-check`,
`brick:patient-simulator`, `brick:sim` green; `poly check` OK.

## Step 4: measured, beside the prediction

Seed 20260824, `--churn`, the working tree at the engine commit with
dense's marker and three options written out. Counts of causes come
from a scratch copy of that tree with `prn` lines in
`refill-followup`, `decide :community-fill` and the run loop's merge
branch; the logs that copy wrote are the logs the tree writes.

| | 750 predicted | 750 measured | 7,500 predicted | 7,500 measured | 28,024 predicted | 28,024 measured |
|---|---|---|---|---|---|---|
| visit prescriptions filled | 155 | 156 | about 1,510 | 1,511 | about 6,118 | 6,122 |
| filled share of visit prescriptions | 0.783 | 0.788 | 0.784 | 0.785 | 0.785 | 0.786 |
| inpatient dispenses | 288 | 288 | 2,533 | 2,533 | 10,334 | 10,334 |
| community fills, all | -- | 237 | -- | 2,298 | -- | 9,751 |
| chronic orders | 60 | 60 | 534 | 534 | 2,262 | 2,262 |
| chronic orders with a first fill | 49 | 48 | about 419 | 424 | about 1,792 | 1,799 |
| refills | 80 to 96 | 81 | 730 to 790 | 760 | 3,500 to 3,780 | 3,596 |
| refills per filled chronic order | 1.80 | 1.69 | 1.81 | 1.79 | 2.03 | 2.00 |
| chronic orders with 1 / 2 / 3 / 4 fills | -- | 6 / 4 / 37 / 1 | -- | 38 / 34 / 330 / 22 | -- | 127 / 149 / 1,122 / 401 |
| refill interval, days (min / median / max) | 28 / 31.5 / 35 | 28 / 32 / 35 | 28 / 31.5 / 35 | 28 / 31 / 35 | 28 / 31.5 / 35 | 28 / 31 / 35 |
| chains stopped by the tail | about 43 | 37 | about 370 | 345 | about 1,580 | 1,441 |
| chains stopped by the draw | about 6 | 11 | about 49 | 74 | about 210 | 342 |
| chains stopped by an end | 0 | 0 | 0 | 0 | 0 | 0 |
| chains stopped because a later order replaced theirs | not predicted | 0 | not predicted | 5 | not predicted | 16 |
| pharmacy steps carried to a survivor | 4 | 11 | 48 | 78 | 159 | 252 |
| dispenses whose subject is the survivor of the order's patient | -- | 17 | -- | 117 | -- | 470 |
| of them inpatient | 1 | 1 | 0 | 0 | 2 | 2 |
| of them refills | -- | 12 | -- | 70 | -- | 311 |
| fills carrying an `:encounter-id` (community) | 0 | 0 | 0 | 0 | 0 | 0 |
| last event, days | 7,392.52 | 7,392.52 | 7,353.10 | 7,353.10 | 7,394.74 | 7,394.74 |
| events | about 29,750 | 29,741 | about 127,550 | 127,549 | about 506,200 | 506,195 |

Where the prediction was wrong.

1. **The draw stops more chains than predicted, the tail fewer.** I
   counted one stopping draw per chain. Every fill takes the draw,
   including the last one the tail allows, so a chain's last fill is
   charged to the draw whenever its own draw says stop, even where the
   tail would have stopped it anyway. The refill counts were not
   affected.
2. **More steps are carried than die today.** A carried first fill of a
   chronic order starts a chain on the survivor, and a refill pending
   at a merge is carried too: 6, 24 and 88 of the carried steps are
   refills, which the base has none of.
3. **A chain can stop a fourth way.** A patient who returns and is
   prescribed the same thing again holds a newer order of the same
   citation; the older order is no longer the latest, and its next fill
   declines (ADR-0190's rule for a first fill, 14 and 44 of those at
   7,500 and 28,024). The newer order's own chain carries on. 5 chains
   at 7,500 and 16 at 28,024 stop this way.

Held: the first-fill counts, the inpatient counts, the last event's
time (the tail lengthens no dense log), the refill totals.

The erasure law. `dispense-derivation-test`'s event-for-event
comparison stayed green throughout: delete every dispense, renumber,
and the keyless log is left. On dense the logs before and after this
session, less their dispenses, their index fields and the order
events' `:chronic`, were compared at 750 and 7,500 (below).
Both: 29,216 and 122,718 non-dispense events, equal event for event.

## Step 5: oracle roots, predicted before the instruments ran

Certain to move: the three roots that hold a dispense, for the fill
number (`dispense`, `osteoporosis-dispense`, `dispense-inpatient`),
and `osteoporosis`, whose 156 orders gain `:chronic true`. Possible:
`asthma` and `metabolic-syndrome-care`, if either log holds an
in-horizon order from an inhaler submodule or
`metabolic_syndrome/medications`; I did not know before running. Every
other root: IDENTICAL, which is the proof that with the key absent
nothing but the two fields differs.

`osteoporosis-dispense` reached no refill on the default tail: the
tail is measured from the last arrival, six days in, and the module
writes its orders across a hundred years. Measured on that config: 134
fills, all number 1. The root now states `:refill-tail-days 36500` and
nothing else, and is the one root reaching a refill: 134 first fills,
2,129 refills, fill numbers up to 85, 3,343 events, `check-all` ok.

## Step 5: oracle roots, measured

`bin/regression-oracle 29668820 334bceb4 --declared-digest-change` and
`bin/ground-truth-bracket` likewise. Both exit 1 with a DIFFERS
banner, as they do whenever a declared root moves; the per-root digest
lists are what was read. Both show the same four roots moving and the
other 42 identical:

- `dispense`, `dispense-inpatient`: the fill number.
- `osteoporosis`: `:chronic true` on its 156 orders.
- `osteoporosis-dispense`: both, and its refills.

`asthma` and `metabolic-syndrome-care` did not move: neither log holds
an in-horizon order from a state its module marks chronic.

I first ran `bin/usecase-ground-truth-oracle` by mistake for the second
instrument. It is a use-case exerciser, and it failed usefully:
`expected 47 invariants checked, got 48`. The count is pinned in the
use-case page and its source, the exerciser, the consumer doc and one
test docstring, and `make test` runs none of the first three. All
re-pinned in `ffdcf2f5`.

## Step 5: figures, describe, pins

The usual drill, 2026-11-21 at `ffdcf2f5`: one warm-up and two timed
runs per cell, `/usr/bin/time -v`, Windows-side load sampled before
each run (1 to 12; none above 20). Both timed runs of every cell wrote
a byte-identical `events.edn`. `config.edn` 7,500's first timed run
overlapped analysis jobs I had left running on the host (109.92 s
against 105.54 s); both are in the mean and the provenance says so.

| cell | events | messages | msg/event | wall | peak RSS |
|---|---|---|---|---|---|
| `config.edn` 7,500 | 127,549 | 125,942 | 0.9874 | 107.73 s | 1,679 MiB |
| `config-nobed.edn` 7,500 | 102,032 | 100,479 | 0.9848 | 92.51 s | 1,464 MiB |
| `config-bare.edn` 7,500 | 67,780 | 40,207 | 0.5932 | 41.50 s | 1,013 MiB |
| `config.edn` 750 | 29,741 | 31,158 | 1.0476 | 53.82 s | 1,083 MiB |

28,024, one run, `-Xmx8g`: 506,195 events, 32,080 subjects,
190,088,591 bytes, 324.77 s, 3,541 MiB, sha-256
`dd4516c020a3b0730425e1d3bab02840f2d5d65f9e19a4ede077c873f286a42e`.
`bd4dec16...` is kept in `figures.edn` under `:pre-adr-0191`, and the
7,500 figures before this session under `:provenance :prior`.

`config-bare.edn`'s counts did not move and its bytes did: its 638
`dense-clinic-chronic` orders gained `:chronic true`.

**Describe 1.4.0.** The catalog's `:measure` group took two rows
without a new mechanism: `:medication-refill` and
`:dispense-after-merge`. `describe` over the new cells:

| | 7,500 | no-bed | bare | 750 | 28,024 |
|---|---|---|---|---|---|
| events | 127,549 | 102,032 | 67,780 | 29,741 | 506,195 |
| subjects | 8,521 | 8,521 | 7,500 | 2,796 | 32,080 |
| `:medication-dispense-inpatient` | 2,533 | 2,533 | 0 | 288 | 10,334 |
| `:medication-dispense-community` | 2,298 | 2,298 | 0 | 237 | 9,751 |
| `:medication-refill` | 760 | 760 | 0 | 81 | 3,596 |
| `:dispense-after-merge` | 117 | 117 | 0 | 17 | 470 |

They agree with the scratch census on every count.

Pins moved: the invariant catalog count, 47 to 48 (`ffdcf2f5`); the
describe golden and version; `engine-test`'s "no fill number";
dense's derivation literals. The dense README's dispense table lost
two rows this session did not re-measure (order-to-dispense turnaround
by ward class), and its carrier-column table is left at its
2026-11-20 measurement: the erasure comparison shows no non-dispense
event moved.

## Step 6 and 7: docs and ADR

`operational-models.md` gains "Refills, and a fill through a merge
(ADR-0191)" beside the community pharmacy. `formats.md`'s event section
is regenerated; its Stability and describe passages were edited by
hand. `consuming-ground-truth.md`: rule 2's exception, the `:dispense`
row, the dispense passages (several dispenses per order, grouping, a
dispense whose patient is the survivor, what the tail does not mean),
the Scale table. `future-features.md`, the root README's one ratio,
the dense README. ADR-0191, Accepted, amends ADR-0190.

## Premises the prompt got wrong

1. **"The dispense law resolving the order through the merge as the
   end's law already does."** The end's law does not resolve through
   merges. The result's law does (ADR-0179), and the dispense law now
   uses the same function.
2. **"1.13.0, additively."** `:fill-number` is required, and
   `classify-change` calls a new required key breaking. Bumped to
   1.13.0 with that output quoted; a 1.12.0 log holding a dispense does
   not validate against 1.13.0.
3. **"The log's tail past the last arrival holds refills only."**
   Dense already runs twenty years past its last arrival. The tail
   lengthens no dense log, and every refill sits in the first 110
   days.
4. **The tail in a long-horizon run.** Measured from the arrival
   clock, it leaves an order written years in with no refill at all.
   `osteoporosis-dispense` reached none on the default; it now states
   a long tail. Not ruled; named in the ADR.
5. **"`chronic` is not declared."** True, and the loader kept the key
   anyway; nothing read it.
6. **"Pending `:pharmacy-dispense` steps ... re-queued."** The
   community half is a different step, `:community-fill`, and that is
   where nearly all of them are (157 of 159 at 28,024). Both are
   carried.
7. **An inpatient dispense through a merge** has no stay of its own to
   land in: the absorbed record's stay never closes. It is dispensed to
   the survivor if the survivor is an inpatient then, under the
   survivor's encounter. All three such steps in the measured cells
   are.
8. **A carried chronic fill starts a chain on the survivor**, and
   refills pending at a merge are carried too, so more steps are
   carried than died at the base.
9. **A chain can stop a fourth way**: a newer order of the same
   citation replaces the old one.
10. **No vendored chronic order reaches a dense log.** The marker
    reaches the event for four modules, and dense walks none of their
    chronic states in-horizon.
11. Not in the prompt: the invariant count is pinned in a use-case
    exerciser `make test` does not run.

Held as stated: two draws per fill on the `:pharmacy` stream; the
three options and their defaults; inpatient dispenses do not refill; a
discharge prescription does; a prescription never filled gets no
refill; the erasure law.

## Downstream line

**Event schema 1.13.0.** Two fields: `:fill-number` on every
`:medication-dispense` (required, from 1), and an optional `:chronic
true` on a `:medication-order`. Under `:dispense`, a community fill of
a chronic order is now refilled every 28 to 35 days, each fill
followed by another with chance 0.94, none later than the last arrival
plus 90 days (`:refill-interval-days`, `:refill-continuation`,
`:refill-tail-days` on the facility; `dense-7500` writes all three
out). **One order may have several dispenses: group by
`:order-event-id`, order by `:fill-number`.** A fill pending when its
patient is merged away is emitted for the survivor, so **a dispense's
patient may be the survivor of its order's patient**. A chain ends
without an event. `:chronic` absent means unmarked, not short-term.
`dense-7500` at 7,500: 2,533 inpatient dispenses and 2,298 community
fills, 760 of them refills, 117 dispenses after a merge; at 28,024,
10,334 and 9,751, 3,596 refills, 470 after a merge. Describe report
1.4.0 counts both. Pin: the tip of this session. New 28,024 log:
190,088,591 bytes, sha-256
`dd4516c020a3b0730425e1d3bab02840f2d5d65f9e19a4ede077c873f286a42e`. A
retained corpus is a valid artifact of the earlier pin, at 1.12.0.

## Close

Gates, each to a full log with its exit code captured, nothing written
into the tree while either ran.

| run | tip | result | poly execution time |
|---|---|---|---|
| `make test`, first | `f18626f1` | **MAKE_EXIT=2** | -- |
| `make test`, second | `aaaed90a` | MAKE_EXIT=0 | 33 minutes 22 seconds |
| `make integration` | `aaaed90a` | MAKE_EXIT=0 | 18 minutes 49 seconds |

The first run failed on one assertion: `ehrt.sim.run-test` pins the
describe text header, and read "describe 1.3.0". The pin sits in
the same namespace as the catalog pin fixed in `ffdcf2f5`, and that
brick's green run was before the describe bump (`e35ce7cd`); I did not
run the brick again after the bump, so the full suite was the first
run to see it. I swept the tree for any other pin of the report version, found
none, restated the header pin in `aaaed90a` and ran the whole suite
again. No mutation population and no conformance pin moved; the
figures exerciser and the README exercisers under `make integration`
were green on the first run at `aaaed90a`.

Commits over `29668820`:

    516cf579 engine, schema 1.13.0
    a9e4547e dense-7500 config: the marker and three options
    334bceb4 oracle root: osteoporosis-dispense states a long tail
    ffdcf2f5 pins: the invariant catalog is 48 deep
    e35ce7cd describe 1.4.0
    5ef656b4 dense-7500 figures and docs
    f18626f1 ADR-0191, record, prompt archived
    aaaed90a pins: the describe text header reads 1.4.0

Pushed `29668820..559a4233`; `bin/post-push-verify 29668820 559a4233`
passed its tip and ASCII checks. CI run **44937339617** concluded
`success` at `559a4233` (`gh run watch --exit-status` 0, confirmed with
`gh run view`). Closed.
