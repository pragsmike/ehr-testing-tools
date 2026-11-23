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
