# A dispense reads its order's prescription (event schema 1.15.0)

2026-11-23. Base `c383c76c`. HEAD matched origin/main and the tree was
clean. `bin/preflight` exit 0, no finding. Event schema 1.14.0 at the
base.

Prompt: `.agents/prompts/2026-11-23-dispense-reads-the-prescription.md`.

## Step 1: measured and predicted, before any edit

No code was touched. The logs read are the ones the ADR-0192 drill
wrote at `43ca836b`; nothing under `components`, `bin` or a dense
config differs between that commit and the base.

**The modules** (66 files, 134 MedicationOrder states), as the prompt
has it: 37 states are chronic; 32 of them are the inhaler states, each
authoring `refills: 0`; the other 5 author no block at all (four in
`metabolic_syndrome/medications`, one in `osteoporosis`). The one
positive count is hypothyroidism's 6, on a state not marked chronic.
Duration units are "days" (20 states) and "weeks" (12); dosage period
units are "days" (29) and "hours" (3: 4 and 6 hours). Every authored
amount and frequency is an integer.

**Orders in each log**, by (chronic, `:refills`, supply in days,
dosage). dense's chronic order states `:refills 5` at the base; step 4
makes it 11, and the prediction below is for 11.

| log | class | orders | filled in the community | inpatient dispenses |
|---|---|---|---|---|
| 750 | chronic, 5, 30 d, dosage (`dense-clinic-chronic`) | 60 | 48 | 0 |
| 750 | not chronic, 0, 7 d, dosage (`dense-clinic-acute`) | 135 | 105 | 0 |
| 750 | not chronic, unstated, 5 d, dosage (stay antibiotic) | 145 | 0 | 145 |
| 750 | not chronic, unstated, no duration, dosage (stay analgesia) | 143 | 0 | 143 |
| 750 | no block (module-written) | 5 | 3 | 0 |
| 7,500 | chronic, 5, 30 d, dosage | 534 | 424 | 0 |
| 7,500 | not chronic, 0, 7 d, dosage | 1,342 | 1,045 | 0 |
| 7,500 | not chronic, unstated, 5 d, dosage | 1,267 | 0 | 1,267 |
| 7,500 | not chronic, unstated, no duration, dosage | 1,266 | 0 | 1,266 |
| 7,500 | module-written, 10 d or 14 d, dosage | 3 | 3 | 0 |
| 7,500 | no block | 78 | 66 | 0 |
| 28,024 | chronic, 5, 30 d, dosage | 2,262 | 1,799 | 0 |
| 28,024 | not chronic, 0, 7 d, dosage | 5,470 | 4,276 | 0 |
| 28,024 | not chronic, unstated, 5 d, dosage | 5,168 | 0 | 5,168 |
| 28,024 | not chronic, unstated, no duration, dosage | 5,166 | 0 | 5,166 |
| 28,024 | module-written, 10 d or 14 d, dosage | 8 | 8 | 0 |
| 28,024 | no block | 92 | 73 | 0 |
| `osteoporosis-dispense` | chronic, unstated, no block | 156 | 134 | 0 |
| `dispense` | no block | 12 | 6 | 6 |
| `dispense-inpatient` | no block | 15 | 3 | 9 |

No log holds a dispense of an order that is not in the log.

**Predicted refills and chains.** The method: each base chain cut at
12 fills. That is exact only if every draw lands where it did, which
needs the 30-day supply's offset draw to equal today's interval draw
(`30 + rand-int-in(-2, 5)` against `rand-int-in(28, 35)`: the same
`nextInt(8)`, so it should) and no patient's later prescription to
shift on their `:pharmacy` stream. A cut chain takes fewer draws, so a
patient with a second prescription after a cut chain will shift; a
handful of first fills are expected to move.

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| refills now | 647 | 6,805 | 27,638 |
| refills predicted | 361 | 3,266 | 13,629 |
| fills per filled chronic order now: mean / median / max | 14.48 / 13 / 51 | 17.05 / 13 / 123 | 16.36 / 11 / 124 |
| predicted: mean / median / max | 8.52 / 12 / 12 | 8.70 / 12 / 12 | 8.58 / 11 / 12 |
| chains stopped by the bound | 25 | 221 | 891 |
| chains stopped otherwise (the draw, an ended or newer order, a death, a merge) | 23 | 203 | 908 |
| dispenses now | 1,091 | 10,876 | 44,128 |
| dispenses predicted | 805 | 7,337 | 30,119 |

The base logs cannot tell a chain the draw stopped from one a death or
a newer order stopped without replaying the fold, so "otherwise" is one
row. No chronic order in a dense log has a `:medication-end`. No tail
is stated, so none stops at a run's end.

The channel's figure for 28,024, "roughly 14,000", came from 7.8
refills per filled order times 1,799. The cut-chain figure is 13,629,
7.58 per filled order: chains that end early for another reason pull it
under the geometric sum.

**Predicted fields**, with the chronic order at 11:

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| community dispenses with `:days-supply` and `:quantity` | 409 + 105 = 514 | 3,690 + 1,045 + 3 = 4,738 | 15,428 + 4,276 + 8 = 19,712 |
| community dispenses with neither | 3 | 66 | 73 |
| inpatient dispenses with `:days-supply 1` and `:quantity` | 288 | 2,533 | 10,334 |
| inpatient dispenses with neither | 0 | 0 | 0 |

Values expected: chronic 30 and 30; acute 7 and 14; stay antibiotic 1
and 1; stay analgesia 1 and 8.

**Roots.** Three of the oracle's 46 roots hold a dispense: `dispense`,
`dispense-inpatient`, `osteoporosis-dispense`. None of their orders
carries a prescription, and none is refillable under the new rule that
is not under the old. **Predicted: no root moves, 46 identical.** That
also says the oracle does not exercise any of this; the tests and the
dense exerciser will have to.

**Pins predicted to move:** dense-7500's figures and literals; the
invariant count (48 to 49); the event-schema export, baseline and
version. The sim brick's four `arc0` corpora set no `:dispense` key
that I know of; to be confirmed by the suite, not assumed.

## Step 2: red tests

Written before any source edit, run at the base.

- `ehrt.sim-engine.dispense-reads-prescription-test` (new, 12 tests):
  **25 failures and 1 error across 10 tests**, each because the base
  ignores the prescription: a non-chronic order stating 2 refills was
  filled once; a chronic order stating 0 ran to the tail; a 14-day
  supply refilled 28 to 35 days later; no dispense carried either
  field; the schema refused both keys; the merge case kept neither the
  bound nor the fields. The one error was a `max` over an empty gap
  list, where a chain that should have refilled did not.
- Green at the base by construction, as the prompt asked to be said:
  (c) a chronic order with no count runs on the draw, and the 30-day
  equivalence test, which at the base compares two runs that both
  ignore the duration. (i), two runs writing one log, was red at the
  base only on its own guard that some dispense carries a quantity.
- `ehrt.sim-model` (config and pathway tests) and
  `ehrt.sim-check.check-test`: red as compile failures, the functions
  under test (`refill-offset-days`, `prescription-supply-days`,
  `prescription-quantity`, the new invariant) not existing. A first
  draft of the engine test also failed to compile on
  `sim-model/default-refill-offset-days`; that assertion was moved to
  the model's own test so the engine's red would be behavioural.

## Step 3: the build

- `sim-model`: `prescription-supply-days`, `prescription-quantity`
  (rational arithmetic; whole results integers, others doubles),
  `default-refill-offset-days` `[-2 5]`, `refill-offset-days`, and the
  facility schema entry (`[lo hi]` integers, `lo <= hi`).
- `sim-engine`: the order record keeps `:prescription`
  (`state.clj`, `evolve.clj`). `decide :medication-dispense` adds the
  two fields off the ordering patient's record. `decide
  :community-fill` hands a refillable order to `refill-followup`, which
  takes its two draws, picks the gap by the order's supply, and applies
  the bound after the draws.
- `sim-check`: the invariant
  `medication-dispense-fills-stay-within-the-orders-stated-refills`,
  49th in the catalog.
- Event schema 1.14.0 to 1.15.0. `classify-change` against the frozen
  1.14.0 baseline, before the re-freeze:

      {:additive? true, :breaking []}

  Baseline re-frozen, export regenerated; `make event-schema-examples`
  changed nothing.

**One existing test was replaced, not adjusted.**
`prescription-test/stating-a-prescription-moves-no-dispense-no-fill-count-and-no-draw`
(ADR-0192) asserted that a chain runs past the five refills its order
states. That is what this session makes false. It is now two tests: a
prescription stating no count, duration or dosage still moves nothing
once erased; and a stated five refills stops the chain at six fills.

Gates: bricks `sim-model`, `sim-check`, `sim-engine` green; `poly
check` OK.

## Step 4: dense-7500, measured against step 1

`:refills 11` on `dense-clinic-chronic`, `:refill-offset-days [-2 5]`
written beside the other refill options, the three siblings re-derived
by their rules (`diff` line `2265d2264`; bare prefix 164,162 bytes,
sha-256 `7de9e9d3...`).

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| refills, base | 647 | 6,805 | 27,638 |
| refills, predicted | 361 | 3,266 | 13,629 |
| refills, measured | **361** | **3,266** | **13,629** |
| fills per filled chronic order, predicted mean / median / max | 8.52 / 12 / 12 | 8.70 / 12 / 12 | 8.58 / 11 / 12 |
| measured | 8.52 / 12 / 12 | 8.70 / 12 / 12 | 8.58 / 11 / 12 |
| chains at the bound, predicted and measured | 25 | 221 | 891 |
| chains stopped otherwise | 23 | 203 | 908 |
| dispenses, predicted | 805 | 7,337 | 30,119 |
| dispenses, measured | 805 | 7,337 | **30,118** |
| events, base to new | 30,307 to 30,021 | 133,594 to 130,055 | 530,238 to 516,228 |
| community dispenses with both fields, predicted | 514 | 4,738 | 19,712 |
| measured | 514 | 4,738 | 19,711 |
| community dispenses with neither | 3 | 66 | 73 |
| inpatient dispenses with `:days-supply 1` and a quantity | 288 | 2,533 | 10,334 |
| inpatient dispenses with neither | 0 | 0 | 0 |

Field values as expected: chronic 30 and 30; acute 7 and 14; stay
antibiotic 1 and 1; stay analgesia 1 and 8; and the module-written
fills their module's own (10 and 20, 14 and 28, 14 and 56, 14 and 84).

**The differences, each explained.**

- **28,024 holds one dispense fewer than predicted.** Two first fills
  left and one arrived (7,500: one and one; 750: none). These are the
  shifts step 1 said it could not count: a cut chain takes fewer draws
  on its patient's `:pharmacy` stream, so a prescription the same
  patient is written later draws its share and lag from a different
  place. One discharge prescription at 28,024 is among them (34 filled
  in the community before, 33 now).
- **Every refill in a new log is in the base log**, same order, number,
  instant and subject: the 30-day supply's offset draw lands exactly
  where the interval draw did, at all three counts. Refill gaps are 28
  to 35 whole days.
- **No inpatient dispense moved.**
- **No non-pharmacy event moved.** Compared positionally 969 (750),
  866 (7,500) and 3,389 (28,024) non-dispense events differ, every one
  a `:demographic-update` and only in `:placeholder-event-id`, a log
  index; with indexes renumbered over the non-dispense events the 750
  logs are equal. The renumbering was run at 750 only.
- **Messages moved, which step 1 did not predict**: 125,947 to 125,917
  (7,500), 100,467 to 100,486 (nobed), 40,207 unchanged (bare), 31,162
  to 31,152 (750). At 750 the whole difference is ADT^A08, 894 to 884,
  and every other message type has the count it had. A dispense renders
  no message; the wire planner takes one draw per ground-truth event in
  log order, so removing events shifts the chatter draws of those after
  them. ADR-0189 found the same mechanism.
- **The bare cell's log is new bytes** (sha `0ecd7008...` to
  `942099d9...`) with every count unchanged: it sets no `:dispense`,
  and its order events now state 11 where they stated 5.

## Step 5: what moved

**Oracle.** `bin/regression-oracle c383c76c e8cf7244`: exit 0,
"IDENTICAL: every root's digest matches", no declaration made.
`bin/ground-truth-bracket c383c76c e8cf7244`: exit 0, "IDENTICAL: every
digested root's :ground-truth matches" (43 roots), no declaration.
Predicted: no root moves. Held. The oracle exercises none of this
session's behaviour, which the ADR names as owed.

**Figures**, by the usual drill at `ee33ac47` on 2026-11-23: one
warm-up and two timed runs per cell, every pair of timed runs writing a
byte-identical `events.edn`; Windows-side load sampled before each run,
0 to 14.

| cell | events | messages | msg/event | wall | peak RSS |
|---|---|---|---|---|---|
| `config.edn`, 7,500 | 130,055 | 125,917 | 0.9682 | 104.41 s | 1,706 MiB |
| `config-nobed.edn`, 7,500 | 104,538 | 100,486 | 0.9612 | 93.40 s | 1,505 MiB |
| `config-bare.edn`, 7,500 | 67,780 | 40,207 | 0.5932 | 41.00 s | 1,084 MiB |
| `config.edn`, 750 | 30,021 | 31,152 | 1.0377 | 53.26 s | 1,073 MiB |

28,024, one run, `-Xmx8g`: 516,228 events, 32,080 subjects (`sim
describe`), 195,875,969 bytes, 337.51 s, 3,042 MiB, sha-256
`1795158ea3362431d3a6b8533ad409fb9508530d914f5025c711a501089ecaf4`.
`83b33fc1...` is kept in `figures.edn` under `:pre-adr-0193`.

`sim describe` over the new 750 cell: `:status :ok`,
`:medication-refill` observed 361.

**Pins.** The invariant catalog 48 to 49 in the sim brick's pin,
`bin/usecase-ground-truth-oracle`, and the consumer docs and use-case
page. The sim brick's four `arc0` corpora did not move.

## Premises the prompt got wrong, or that the tree corrected

1. **"the 28,024 log shows up to 123 fills against it."** The longest
   chain is 123 REFILLS, 124 fills.
2. **"each oracle root that sets the key"**: three roots hold a
   dispense and none of their orders carries a prescription. So step
   5's "oracle roots against the prediction" is the empty comparison,
   and no oracle root exercises this session's behaviour.
3. **"For a 30-day supply that is the same 28 to 35, so dense's
   intervals should not move -- measure whether the draw lands
   identically."** It does, exactly: every refill kept is at its base
   instant.
4. **"refills fall from 27,638 to roughly 14,000."** 13,629: 7.58 per
   filled order, not 7.8, because some chains end early for another
   reason.
5. **"`osteoporosis-dispense` ... expect it identical."** Identical.
6. **Not said by the prompt: message counts move.** By -30, +19, 0 and
   -10, through the wire planner's per-event draws.
7. **Not said: an authored `:medication-dispense` step reads the
   prescription too.** One decide serves all three producers; an
   authored dispense carries the fields and counts against the bound
   in the new invariant. No shipped config authors a dispense of an
   order that states a prescription.
8. **Not said: an inpatient dispense of an order whose dosage period
   is in a unit that does not convert carries neither field**, not a
   bare `:days-supply 1`. The pair is kept whole.
9. **"a non-integer result is kept as authored arithmetic gives it
    ... say what type the schema gives it."** A positive number: an
    integer when whole, a double otherwise. The arithmetic is rational
    and a Clojure ratio is never written to the log.
10. **An existing ADR-0192 test had to be replaced** (step 3).
11. **The prompt's line ranges** for `decide.clj` and `config.clj` held.

## Downstream line

**Event schema 1.15.0, additive: a 1.14.0 log validates unchanged.** A
`:medication-dispense` may now carry `:days-supply` and `:quantity`,
read off its order's `:prescription` and absent when the order states
too little. On a community fill `:days-supply` is the order's duration
in days and `:quantity` is dose times frequency over that supply; **on
an inpatient dispense `:days-supply` is 1 by assumption**, with one
day's doses. `:quantity` is an integer when whole and a double
otherwise. **Chains changed**: an order is refilled when it is chronic
or states a positive `:refills`; **a stated `:refills` bounds its
fills, and a stated 0 means never**, so a module-written inhaler order
is filled once. Where an order states a duration the next fill comes
that many days later, two days early to five late
(`:refill-offset-days`, a new facility option). **Nothing renews an
order whose refills run out.** `dense-7500`'s chronic order states 11
refills: its refills fell by about half (6,805 to 3,266 at 7,500;
27,638 to 13,629 at 28,024), every chain stops by twelve fills, and
the last refill is about a year in. Message counts moved slightly with
the event counts. `sim check` has a 49th invariant holding the bound.
Pin: the tip of this session. New 28,024 log: 195,875,969 bytes,
sha-256
`1795158ea3362431d3a6b8533ad409fb9508530d914f5025c711a501089ecaf4`. A
corpus retained from the earlier pin (`83b33fc1...`) is a valid
artifact of it, at 1.14.0.

## Close

At `36697a0e`, tree clean, nothing written into the tree while either
ran:

- `make test`, unpiped to a full log: **MAKE_EXIT=0**, poly execution
  time 33 minutes 46 seconds.
- `make integration`: **MAKE_EXIT=0**, poly execution time 19 minutes
  21 seconds.

Commits over `c383c76c`: `310efcd5` (step 1 and the prompt archive),
`2c9e2cbe` (the build, schema 1.15.0), `3092c0a8` (the catalog pins at
49), `ee33ac47` (dense's `:refills 11` and the offset option),
`db286a87` (figures and docs), `e8cf7244` (ADR-0193), `36697a0e` (this
record), and this close.
