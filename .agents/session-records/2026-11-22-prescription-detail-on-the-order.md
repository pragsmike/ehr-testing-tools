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
orders, and the rest), and `dispense` and `dispense-inpatient`, whose
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

## Step 2: red tests

Red at the base (`035fd527`, tests only), each for the reason stated:
the key is not declared, not carried and not refused.

| the prompt's test | where | red at the base because |
|---|---|---|
| (a) a module's dosage and duration reach the event with the same values; `as_needed` only carries only that; no block, no key | `ehrt.sim-engine.prescription-test`, on the oracle's `ear-infections-engine` and `osteoporosis` configs; `compile-trajectory-test` for the loader and the compile step | the event carries no `:prescription`; the loader leaves an instruction's system as a string and accepts a malformed block |
| (b) an authored order's block reaches the event | `prescription-test` | the event carries none |
| (c) the schema refuses a malformed dosage and an unknown key | `pathway-test` (the IR), `prescription-test` (the event), `compile-trajectory-test` (the loader) | the step and the event do not know the key: the IR refuses even a well-formed block, the event accepts any |
| (d) pre-horizon order facts | `compile-trajectory-test`: the fact does NOT carry it | green by construction; the record says why under step 1 |
| (e) no dispense, fill count or draw differs | `prescription-test`: 40 chronic orders under `:dispense`, with and without a block; erase the key and the logs are `=` | the equality is green by construction at the base (nothing is carried); its guard, that 40 orders carry the key, is red |

The no-block half of (a) (`osteoporosis`, 156 orders, no key) is green
by construction too.

## Step 3: the build

- `sim-model`: `Prescription`, exported, and on the IR's
  `:medication-order` step.
- `patient-simulator`: the loader declares `:prescription` and passes
  an instruction's code triplet through `normalize-code`; the
  interpreter puts the block on the trajectory event; the compile step
  carries it to the IR step.
- `sim-engine`: `decide :medication-order` selects it onto the event.
  `evolve`, the order record and every dispense path are untouched.
- Event schema 1.13.0 to 1.14.0. `classify-change` against the frozen
  1.13.0 baseline, before the re-freeze:

      {:additive? true, :breaking []}

  Baseline re-frozen; the export regenerated; the examples regenerated
  and unchanged, since the example fleet's orders state no block.

Two things the build ran into.

1. **A schema that is exported cannot hold a function.** The first
   draft used `[:fn pos?]` for a positive amount and `[:fn seq]` for a
   non-empty map. Both printed into `event-schema.edn` as `#object`,
   which the committed-export test cannot read back. They are now
   `[:> 0]` and `[:map-of {:min 1} :keyword :any]`, which are data.
2. **Two pinned corpora moved, as step 1 allowed for.** The sim
   brick's `arc0` gate pins four corpora by bytes and says a red is a
   STOP. `seed-424242-clinic-decade` differs in 1 of 1,774 events
   (index 508, a `uti/abx_tx` order) and `seed-5-clinic-decade` in 2 of
   1,412. Measured before re-pinning: erase `:prescription` from the
   new log and it is `=` to the committed baseline, in both. Re-pinned
   in its own commit with that proof in the pin's docstring, on the
   precedent of ADR-0188's re-pin. The other two write no medication
   order.

Bricks green after the build: sim-model, patient-simulator,
sim-engine, sim-check, sim, oracle. `poly check` OK.

## Step 4: dense-7500

All four configs gain the four regimens; the siblings still differ
from `config.edn` by their one line each (`2255d2254`, and the
`:persons` line), and the bare config is still the first 163,457
bytes, sha-256 `e2986d4a...` on both sides. Step counts, delays,
citations and weights are unchanged.

Each code and display was read from its module file today:

| code | display | module state | that state's own prescription |
|---|---|---|---|
| 1665021 | ceftriaxone 1000 MG Injection | `uti/abx_tx` `Ceftriaxone` | 1, once a day, 1 day |
| 313782 | Acetaminophen 325 MG Oral Tablet | `medications/otc_pain_reliever` `Adult_Acetaminophen` | as needed |
| 562508 | amoxicillin 875 MG / clavulanate 125 MG Oral Tablet | `uti/abx_tx` `AmxClav 875, 7 day` | 1, twice a day, 7 days |
| 860975 | 24 HR Metformin hydrochloride 500 MG Extended Release Oral Tablet | `metabolic_syndrome/medications` | none |

**The comparison at 750**, the base log (sha-256 `7e45727b...`)
against the tip's (`c95ba061...`), seed 20260824, `--churn`:

| | base | tip |
|---|---|---|
| events | 30,307 | 30,307 |
| differing events | -- | 485, every one a `:medication-order` |
| equal once `:codes` and `:prescription` are set aside | -- | yes, the whole log |
| every `:t` equal | -- | yes |
| every dispense equal (1,091) | -- | yes |
| non-order events equal | -- | yes |
| orders carrying `:prescription` | 0 | 485 of 488 |
| pre-horizon order facts carrying it | 0 | 0 |

The 485: `dense-inpatient` `:antibiotic-start` 145 and
`:analgesia-start` 143, `dense-clinic-acute` 135 (all three with a new
code and a prescription), `dense-clinic-chronic` 60 (a prescription
only), and 2 module-written acetaminophen orders (`{:as-needed
true}`). Predicted: 483 and 2. The three orders without a
prescription cite module states that author none.

## Step 5: oracle roots, measured against step 1

`bin/regression-oracle 19ba99df 43ca836b --declared-digest-change`
and `bin/ground-truth-bracket` with the same arguments. The soundness
check passed on both (`digest.clj` is untouched). Both exit 1 with
DIFFERS, which is what a declared move looks like.

| | predicted | measured |
|---|---|---|
| roots moved, `bin/regression-oracle` | 7 | 7: `allergic-rhinitis`, `ear-infections`, `ear-infections-engine`, `ear-infections-history-engine`, `injuries`, `urinary-tract-infections-engine`, `urinary-tract-infections-history-engine` |
| roots moved, `bin/ground-truth-bracket` | the 6 that are logs | those 6 |
| roots identical | 39 | 39 |

The roots were then written out at the tip and compared with the
base's by value. Orders carrying a `:prescription`: 7, 29, 29, 282,
113, 101 and (the batch) 478, each the predicted number. **Every one
of the 46 roots equals its base once `:prescription` is erased from
its medication orders**, and the HL7 half of each moved root is
unchanged. The prediction held in full.

## Step 5: figures and pins

The usual drill at `43ca836b`, 2026-11-22: one warm-up and two timed
runs per cell, Windows-side load 0 to 6 before every run, nothing else
on the host. Every cell's three runs wrote one `events.edn`.

| cell | events | messages | msg/event | wall | peak RSS |
|---|---|---|---|---|---|
| `config.edn` 7,500 | 133,594 | 125,947 | 0.9428 | 103.88 s | 1,701 MiB |
| `config-nobed.edn` 7,500 | 108,077 | 100,467 | 0.9296 | 92.08 s | 1,527 MiB |
| `config-bare.edn` 7,500 | 67,780 | 40,207 | 0.5932 | 41.22 s | 1,109 MiB |
| `config.edn` 750 | 30,307 | 31,162 | 1.0282 | 52.88 s | 1,052 MiB |

28,024, one run, `-Xmx8g`: 530,238 events, 32,080 subjects,
199,000,793 bytes, 322.70 s, 3,486 MiB, sha-256
`83b33fc12054a54347eede3cfbbacaf6157fcaa6db0d09abf7ffec9007660277`.

**No event count and no message count moved in any cell**; `ehrt sim
describe` gives the same events, subjects and refill measures as this
morning (6,805, 6,805, 0, 647 and 27,638). The 28,024 log grew by
1,999,329 bytes, which is the prescriptions. `37582a56...` is kept in
`figures.edn` under `:pre-adr-0192`, labelled as the value before this
ADR. The bare cell's log is new bytes too: the bare config authors the
same four orders.

Pins moved by hand: the two `arc0` corpora (step 3) and the scenario
README's three re-derived literals (step 4).

## Steps 6 and 7: docs and the ADR

- `docs/consuming-ground-truth.md`: a paragraph on the field, with its
  layout, that absence means the author stated none, and that nothing
  computes a quantity or a days-supply; the manifest row reads 1.14.0.
- `docs/formats.md`: the stability note names 1.14.0; the generated
  event-kind table follows the schema.
- `docs/future-features.md`: the order paragraph and the list of what
  is not carried.
- `components/patient-simulator/docs/limitations.md`: one new row, the
  pre-horizon gap, cited into `compile_trajectory.clj`. `gmf.clj`'s
  comment says what is declared and carried.
- The scenario README: the pathway table, a table of the four
  regimens, where each code came from, and that `dense-inpatient`'s
  other codes still describe a fracture.
- ADR-0192, Accepted.

## Premises the prompt got wrong

1. **"51 with `as_needed` only."** 19. The other 32 hold `as_needed`
   beside `refills`.
2. **"Correcting two drugs."** Three codes change. The prompt's own
   regimen list gives `dense-clinic-acute` the 875 MG strength
   (562508) where it had the 250 MG (562251). That one did fit its
   pathway: it was `sinusitis.json`'s own antibiotic for a visit whose
   reason is sinusitis. I followed the regimen list, which names the
   code and its source, and the change is one line to reverse.
3. **"Penicillin V 500 MG Oral Tablet."** The display was "Penicillin
   V Potassium 500 MG Oral Tablet" (834102).
4. **The ceftriaxone regimen.** `uti/abx_tx`'s own state authors one
   dose for one day; the channel's five days is kept, and the config
   and the ADR say whose figure it is.
5. **"The loader keeps [the block] and nothing reads it."** It kept it
   unvalidated, with an instruction's code system still the file's
   string. Declaring it means the loader now refuses a malformed block
   and normalizes that one field.
6. **"Each key optional."** At the top level. A `:dosage` is all four
   of its keys or it is refused, and a `:duration` both of its: every
   vendored block states them together, and half a dosage says
   nothing.
7. **Carried "exactly as authored" against the tree's habit for an
   optional flag.** `:chronic` is true or absent (ADR-0178's rule).
   `:as-needed false` is kept as written, since the modules author it
   21 times.
8. **Test (d), pre-horizon facts.** They cannot carry it under an
   additive bump: `classify-change` calls a widened pre-horizon fact
   breaking. Registered as a limitation and listed as owed.
9. **"The roots that will move."** The prompt asked for the list; it
   did not expect the pinned corpora outside the oracle. Two of the
   sim brick's four moved, by 1 and 2 events.
10. **"59 of some 20,000 orders at 28,024 are module-written."** 100
    of 18,166 are. 59 is the number of those whose state authors no
    block; the other 41 carry one. So 18,107 of the 18,166 orders in
    that log carry a `:prescription`, and 371 of its 1,062 pre-horizon
    order facts cite a state with a block and do not.
11. Held as stated: 134 states, 83 with a block, 32 dosed, 9 with
    instructions, 33 with refills; `classify-change` calls the bump
    additive; no draw, no new event, no dispense or refill change; the
    swap of drug codes changes nothing downstream of the orders.

## Downstream line

**Event schema 1.14.0, additive: a 1.13.0 log validates unchanged.**
A `:medication-order` may now carry `:prescription`: `:dosage
{:amount :frequency :period :unit}` (the unit is the period's),
`:duration {:quantity :unit}`, `:refills`, `:as-needed`,
`:instructions`, each optional, exactly as the module or the pathway
author wrote it. **Absent means the author stated none**; most
module-written orders have none, and a medication the patient arrives
on never does. **Nothing is computed from it yet**: no quantity or
days-supply on a dispense, and refills still run on the facility's
interval and continuation, so an order authorising five refills may
show more. `dense-7500`'s four authored orders now state a regimen,
all provisional, and three of them name a different drug: ceftriaxone
and acetaminophen in the stay, the 875 MG amoxicillin / clavulanate in
the acute clinic visit. **No count, instant, dispense or message moved
in any dense cell**; only medication orders differ. Pin: the tip of
this session. New 28,024 log: 199,000,793 bytes, sha-256
`83b33fc12054a54347eede3cfbbacaf6157fcaa6db0d09abf7ffec9007660277`. A
corpus retained from the earlier pin (`37582a56...`) is a valid
artifact of it, at 1.13.0.
