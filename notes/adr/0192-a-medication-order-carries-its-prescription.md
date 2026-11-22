## ADR-0192 — a medication order carries its prescription: dose, duration, refills, as-needed

**Status:** Accepted 2026-11-22 (author ruling of 2026-11-22, quoted
below; build session 2026-11-22). Event schema **1.14.0**, additive;
describe report **1.4.0**, unchanged; stream scheme **"1.0"**,
unchanged.

### Context

A `:medication-order` carried its codes, a citation and an optional
`:chronic`. The vendored modules author more. 83 of their 134
MedicationOrder states hold a `prescription` block; the loader kept it
and nothing read it (ADR-0191 listed it as owed). And `dense-7500`'s
four authored orders, which write nearly every order in a dense log,
stated no regimen at all: of the 488 in-log orders at 750 arrivals,
483 are theirs.

The 2026-11-17 disclosure session left "the dropped prescription
fields" undecided between a declared limitation and a debt. This
settles it as a debt, and pays the first part.

Author ruling, verbatim, 2026-11-22: "Module's layout.", to: "whether
the field shape follows the modules' own layout, which is the cheaper
option, or a FHIR-like one ... I'd take the modules' layout and map it
at the emitter." It answered a proposal of two sessions, of which this
is the first: "Session 1: parts 1 and 2. One additive schema bump;
dense moves only by added fields and corrected drugs", with "Orders
with no authored detail would stay without it. I would not invent
defaults for them." and regimens "from general clinical knowledge",
labelled provisional, with "codes ... from ones already in the tree".

### Decision

**1. One optional `:prescription` map**, on the pathway IR's
`:medication-order` step and on the `:medication-order` event, defined
once as `sim-model/Prescription`.

| key | value | meaning |
|---|---|---|
| `:dosage` | `{:amount :frequency :period :unit}`, all four together | `:amount` of the drug, `:frequency` times every `:period` `:unit`s |
| `:duration` | `{:quantity :unit}` | how long the course runs |
| `:refills` | an integer, 0 or more | refills authorised |
| `:as-needed` | `true` or `false` | as written |
| `:instructions` | a vector of concepts | coded patient instructions |

Every key is optional. The map is closed at both depths and never
empty: an author who states nothing leaves the key out.

**2. The modules' layout, kebab-cased, and nothing else changed.** The
keys are the module file's own (`as_needed` becomes `:as-needed`). The
unit strings are the author's ("hours", "days", "weeks" in the
vendored set) and are not an enumeration. `dosage.unit` is the unit of
the PERIOD, not of the dose, as upstream has it: `{:amount 1
:frequency 2 :period 1 :unit "days"}` is one, twice a day. A FHIR
shape is the emitter's to build from this.

**3. Carried exactly as authored.** No unit is converted. No quantity
and no days-supply is derived. Nothing is defaulted: a key the author
did not write is absent, and an order of a state with no block carries
no `:prescription`. So absence means "the author stated none" and
never "none": an absent `:refills` is unstated, not zero. One
normalization is applied, the one every other concept in the tree
already gets: an instruction's code system is the tree's keyword
(`:snomed`), not the file's string.

A stated `:as-needed false` is kept. That departs from ADR-0178's
habit for an optional fact, which `:chronic` follows (true or absent).
The modules author `false` 21 times, and dropping it would make an
authored statement indistinguishable from silence.

**4. Why nothing is defaulted.** 51 states author no block and 19 more
author `as_needed` alone. A default dose for those would be this
project's invention sitting in ground truth beside the module's own
figures, with nothing to tell them apart. The author ruled against it.

**5. Nothing reads it.** No dispense, no fill count, no refill chain
and no draw differs between a run whose orders state a prescription
and one whose orders do not; a gated test erases the key from one log
and compares it with the other. The patient's order record in the
fold does not keep it, since nothing there would use it.

**6. A pre-horizon order does not carry it.** An order that predates
the horizon is a `:registered` event's `:pre-horizon-facts` entry: one
closed map of four keys shared by seven fact kinds. `classify-change`
calls any change to that value schema breaking (it did for 1.11.0),
and the ruling is one additive bump. So a medication the patient
arrives on has codes and no dose, as it has no `:chronic` (ADR-0191).
Counted at the base: 1,867 pre-horizon order facts across the oracle's
roots cite a state with a block, and 55 of the 143 in the dense 750
cell. Registered as a limitation; owed below.

**7. `dense-7500` authors a regimen on each of its four orders**, and
three drugs change to fit their pathways. Every code and display is
one a vendored module state already carries.

| pathway and state | was | is | prescription |
|---|---|---|---|
| `dense-inpatient` `:antibiotic-start` | Penicillin V Potassium 500 MG Oral Tablet (834102) | ceftriaxone 1000 MG Injection (1665021, `uti/abx_tx`) | one dose a day, five days |
| `dense-inpatient` `:analgesia-start` | Alendronic acid 10 MG Oral Tablet (904419) | Acetaminophen 325 MG Oral Tablet (313782, `medications/otc_pain_reliever`) | two tablets four times a day, as needed |
| `dense-clinic-acute` `:antibiotic-start` | Amoxicillin 250 MG / Clavulanate 125 MG Oral Tablet (562251) | amoxicillin 875 MG / clavulanate 125 MG Oral Tablet (562508, `uti/abx_tx`) | one tablet twice a day, seven days, no refills |
| `dense-clinic-chronic` `:chronic-start` | unchanged | 24 HR Metformin hydrochloride 500 MG Extended Release Oral Tablet (860975) | one tablet a day, 30 days, five refills |

**The regimens are provisional.** They come from general clinical
knowledge and no source, and no clinician wrote them. `uti/abx_tx`'s
own ceftriaxone state authors a single day; five days here is the
design channel's figure for an inpatient course.

### The schema

`classify-change` against the frozen 1.13.0 baseline, before the
re-freeze:

    {:additive? true, :breaking []}

A 1.13.0 log validates unchanged against 1.14.0. The bump is taken
because a 1.14.0 log may carry a key that a consumer validating
`:medication-order` as a closed map has not seen. Made under the
deprecation waiver; nothing was removed.

### What a consumer must now expect

- **An order may carry `:prescription`, and most module-written orders
  will not.** Treat absence as unstated.
- **Nothing is computed.** A `:medication-dispense` carries no amount.
  A chronic order that authorises five refills may show twenty, because
  the chain still runs on the facility's interval and continuation.
- **`:dosage`'s `:unit` is the period's.**
- **A pre-horizon order has no prescription.**

### What moved, predicted and measured

Predicted before any edit, from the 46 roots written out at the base:
seven roots move and 39 do not.

| root | orders predicted to gain it | measured |
|---|---|---|
| `allergic-rhinitis` | 7 | 7 |
| `ear-infections-engine` | 29 | 29 |
| `ear-infections-history-engine` | 29 | 29 |
| `injuries` | 282 | 282 |
| `urinary-tract-infections-engine` | 113 | 113 |
| `urinary-tract-infections-history-engine` | 101 | 101 |
| `ear-infections` (a trajectory batch) | 478 | 478 |

`bin/regression-oracle 19ba99df 43ca836b --declared-digest-change`
reports those seven and no other; `bin/ground-truth-bracket` the six
that are logs. Each moved root equals its base once `:prescription` is
erased from its orders, and the HL7 half of every root is unchanged.
The declaration is of an added key.

Not predicted: two of the sim brick's four pinned corpora
(`seed-424242-clinic-decade`, 1 event of 1,774; `seed-5-clinic-decade`,
2 of 1,412) hold an order of a block-bearing state. Re-pinned on the
same proof.

`dense-7500` at 750 arrivals, base against tip: 30,307 events both;
485 differ, every one a medication order, by `:codes` and
`:prescription` only (483 authored, 2 module-written); every dispense
and every `:t` is equal.

`dense-7500` re-measured by the usual drill at `43ca836b`: **no event
count and no message count moved in any cell.** `config.edn` at 7,500
is still 133,594 events and 125,947 messages; the 28,024 cell still
530,238 events and 32,080 subjects. Its log is 199,000,793 bytes,
sha-256
`83b33fc12054a54347eede3cfbbacaf6157fcaa6db0d09abf7ffec9007660277`;
before this ADR it was 197,001,464 bytes, `37582a56...`, kept in
`figures.edn` under `:pre-adr-0192`. The bare cell's log changed too:
its orders are the same authored ones.

### Downstream

Event schema **1.14.0**, additive. Corpora that move: every corpus
holding an in-log order of a module state that authors a
`prescription`, by that one added key; and `dense-7500`, by the added
key on its four authored orders and by three corrected drug codes. No
count, instant, dispense or message moves anywhere. A retained corpus
is a valid artifact of its pin, at 1.13.0.

### Owed next

- **A quantity and a days-supply on a dispense**, taken from the
  order's prescription.
- **The refill interval taken from the supply, and the chain bounded by
  the authorised refills.** Both unruled, and both replace ruled
  defaults (ADR-0191's interval and continuation).
- **Dosage on the FHIR MedicationRequest.** The emitter renders codes,
  status and authored time today.
- **`dense-inpatient`'s care plan, procedure and report codes**, which
  still describe fracture care, a bone immobilization and a bone
  density scan in an abdominal-pain stay.
- **A pre-horizon order's prescription** (decision 6): a widened
  pre-horizon fact, which is a breaking bump.
- Carried from ADR-0191 and its addendum: a per-order refill bound;
  refills for chronic orders that predate the horizon; a 90-day supply;
  a non-uniform first-fill lag; a refusal for a continuation of 1 with
  no tail; the mutation column for the dispense.

### What this does not do

- Changes no dispense, no fill and no refill.
- Defaults no dose for an order whose author wrote none.
- Converts no unit and validates no unit string.
- Renders the prescription on any wire, HL7 v2 or FHIR.
- Puts the prescription on a pre-horizon order fact.
