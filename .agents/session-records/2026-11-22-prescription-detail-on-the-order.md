# A medication order carries its prescription (ADR-0192, event schema 1.14.0)

2026-11-22. Base `19ba99df`. HEAD matched origin/main and the tree was
clean. `bin/preflight` exit 0, no finding. Event schema 1.13.0 at the
base.

Prompt: `.agents/prompts/2026-11-22-prescription-detail-on-the-order.md`.

## Step 1: measured and predicted, before any edit

No code was touched.

**The module census**, by parsing the 66 files under
`components/sim/resources/sim/modules`:

| | prompt | measured |
|---|---|---|
| MedicationOrder states | 134 | 134 |
| with a `prescription` | 83 | 83 |
| with none | 51 | 51 |
| with `dosage` (amount, frequency, period, unit) and `duration` (quantity, unit) | 32 | 32 |
| of those, with `instructions` | 9 | 9 |
| with `as_needed` | 51 | 51 (30 true, 21 false) |
| with `as_needed` ONLY | 51 | **19** |
| with `refills` | 33 (32 zero, one 6) | 33 (32 zero, one 6) |

The key sets, exactly: `{dosage duration}` 22, `{dosage duration
instructions}` 9, `{dosage duration refills}` 1, `{as_needed}` 19,
`{as_needed refills}` 32. So "51 with `as_needed` only" is 19 with it
alone and 32 with it beside `refills`; every `refills 0` sits on an
`as_needed` block, and the one `refills 6` on a dosed one. Every
numeric value is an integer. `dosage.unit` is the unit of the PERIOD
("days" 29, "hours" 3), not of the dose. `duration.unit` is "days" 20,
"weeks" 12. `instructions` is a vector of coded concepts (SNOMED), one
or two per state.

**What the loader hands on today** (read by loading a module): the
block arrives as `:prescription`, keys already kebab-cased at every
depth by the loader's one key function, values untouched. So an
instruction's `:system` is still the string `"SNOMED-CT"`, where every
other concept in the tree carries `:snomed`.

**Pre-horizon order facts will not carry it** (the prompt's test (d)
allows either). A `:registered` event's `:pre-horizon-facts` entries
are one closed map of four keys for seven fact kinds, and
`classify-change` calls any change to that value schema breaking: it
did so for 1.11.0, whose docstring quotes it. The ruling is one
additive bump. ADR-0191 left `:chronic` off those facts for the same
kind of reason. Owed, and counted below.

**The oracle, predicted before any instrument ran.** The 46 roots
were written out at the base and every medication order in them
resolved to its module state.

| root | in-log orders | of them citing a state with a block | pre-horizon order facts | of them with a block |
|---|---|---|---|---|
| `allergic-rhinitis` | 7 | 7 | 59 | 59 |
| `ear-infections-engine` | 29 | 29 | 627 | 627 |
| `ear-infections-history-engine` | 29 | 29 | 0 | 0 |
| `injuries` | 282 | 282 | 865 | 865 |
| `urinary-tract-infections-engine` | 117 | 113 | 169 | 162 |
| `urinary-tract-infections-history-engine` | 107 | 101 | 0 | 0 |
| `ear-infections` (a trajectory batch, not a log) | 478 trajectory orders | 478 | -- | -- |
| `asthma` | 0 | 0 | 12 | 12 |
| `dermatitis` | 0 | 0 | 14 | 14 |
| `hypothyroidism` | 0 | 0 | 1 | 1 |
| `total-joint-replacement-engine` | 0 | 0 | 127 | 127 |

**Predicted to move: seven roots**, the first seven rows. 561 in-log
orders gain a `:prescription` across the six engine roots, and all 478
orders of the `ear-infections` batch, which is the interpreter's own
trajectory and so shows the field whether or not the order predates
the horizon. **Predicted IDENTICAL: the other 39**, among them the
last four rows, whose only block-bearing orders are pre-horizon facts
(1,867 such facts across all roots stay as they are), every root whose
orders cite a state with no block (`bronchitis` 289, `osteoporosis`
and `osteoporosis-dispense` 156 each, `sore-throat`'s 266 trajectory
orders, eleven more), and `dispense` and `dispense-inpatient`, whose
27 orders are authored in the root and state no regimen. The HL7 half
of every root is predicted unchanged: nothing renders the field.

**The dense 750 cell** (the base log, sha-256 `7e45727b...`, 30,307
events): 488 in-log orders. 483 are authored by the four dense
pathways and gain a `:prescription` at step 4, when the config states
one. Of the 5 module-written orders, 2 cite a state with a block and
gain it at step 3; 3 do not. 143 pre-horizon order facts, 55 with a
block, none of which gains it. Predicted for step 3 alone: 2 events
differ, by one added key each. For step 4: 483 more, by `:codes` on
the two corrected drugs and `:prescription` on all four; the event
count, every dispense and every `:t` as at the base.

Not predicted, to be found by the suite: any gated corpus or pinned
fixture outside the oracle that holds an in-log order of a
block-bearing state.
