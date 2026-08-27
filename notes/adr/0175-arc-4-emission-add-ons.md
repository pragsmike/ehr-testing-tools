## ADR-0175 — arc 4: emission add-ons (chatter, status ladders, charges, fan-out, transport)

**Status:** Accepted (design session 2026-08-27, HEAD `8439416`;
**RULED 2026-08-27: A1 B1 C1 D1 E1** -- the recommendation on every one
of the five, with no addition and no amendment). The design session was
a payload session under the de-scaffold moratorium: **no code landed
with this ADR**, and no `components/*/src` file was touched. Each
ruling is quoted below at the option it selected; the declined options
are kept verbatim and unstruck, because what was declined is the reason
the selection means anything. Execution begins with ruling A1's own
sweep, arc 4 sweep 1 (2026-08-27), which is where section 2(e) becomes
code. Arc 4 is the
program's last arc (`.agents/plans/2026-08-24-traffic-scale-program.md`,
"Arc 4 -- emission add-ons"), and it is the one arc that
**reshuffles nothing**: `rulings.md#R-mix-7` puts chatter and fan-out
DOWNSTREAM of the fact generators and makes mix ratios emission config.
The proof shape every later sweep owes is fixed here (section 4).

Every figure below was measured in this clone at `8439416`. Every
structure claim is read out of `ca.uhn.hapi/hapi-structures-v24` 2.6.0
-- the only HL7v2 structure library any classpath in this tree has
(`components/judge-v2-hapi/deps.edn`) -- by instantiating the class and
asking it, never from memory.

**The probe corpus, named once.** Where a message figure below says
"the probe corpus" it means: `bin/ehrt sim run --seed 202 --patients
100 --config demos/scenarios/ed-tuesday/config.edn --churn`, on this
clone at `8439416`. That is the ed-tuesday scenario file with its four
arc-3 opt-ins on (`:persons`, `:encounters`, `:bed-cycle`,
`:scheduling`) plus `--churn`. It is NOT `bin/demo-exerciser-ed-tuesday`'s
own invocation, which runs seed 20260811; the numbers here are this
session's own probe and are labelled as such rather than borrowed.
It produces **1,213 ground-truth events and 747 messages**.

### 1. Census from the tree

#### (i) Skeleton kinds with no wire entry, and the registry's stated reason

The contract declares **28 event kinds**
(`components/sim-engine/resources/sim-engine/event-schema.edn`,
`:event-schema-version "1.7.0"`). `emit_hl7/message-type-registry`
carries **14 entries**. **14 kinds render no message**, and every one
of the silences is already committed to in three places -- the kind's
own `:doc`, the registry's comment, and
`ehrt.sim-emit-hl7.event-conformance-test`'s
`the-kinds-this-emitter-deliberately-does-not-render-are-still-contract-kinds`,
whose pinned set is exactly these fourteen:

| kind | the registry's own reason | arc 4's disposition |
|---|---|---|
| `:registered` | none stated anywhere -- the ONE silence with no recorded reason | **A28 candidate**, section 2(a) |
| `:demographic-update` | truth-only; the change reaches the wire in the PID of every LATER message. An A08 "is real work this arc does not do and does not sketch" | **A08/A31**, section 2(a) |
| `:coverage-change` | same, via IN1 on the next admission | **IN1-only update**, section 2(a) |
| `:appointment`, `:reschedule`, `:appointment-cancel`, `:no-show` | SIU is v2.4 structure and MSH-12 says `"2.3"`, so an entry "would emit a structure the version field disclaims". A VERSION gap, not a preference | **blocked on ruling (A)**, section 2(e) |
| `:step-rejected` | `sim/ADR-0012`: truth about the run, never wire traffic | stays silent -- correct |
| `:outpatient-visit-end` | real ambulatory feeds send an A04 and no closing message | stays silent as ADT; **is a DFT trigger instant**, section 2(c) |
| `:procedure`, `:medication-order`, `:medication-end` | truth-only facts; a real shape is "its own future catalytic/segment-design work" | **FT1 charge lines**, section 2(c) |
| `:care-plan-start`, `:care-plan-end` | ADR-0029 R3: CarePlan's natural rendering is FHIR, not an invented v2 shape | stays silent -- correct |

`:registered` is the interesting row: it is the only kind whose silence
neither the registry's own comments nor the conformance gate's own
message explains. Section 2(a) proposes filling it and
section 3(B) prices it.

#### (ii) Every segment the emitter renders, and its blank-field count

Measured over the probe corpus's 747 messages by splitting each segment
on `|` and counting empty fields. "Always blank" means blank on every
instance in this corpus; "sometimes" means content-conditional.

| segment | instances | fields rendered | always blank | sometimes blank |
|---|---|---|---|---|
| MSH | 747 | 12 | 3 (MSH-5, -6, -8) -- 5 and 6 only because this corpus declares no `:msh` dialect | -- |
| EVN | 687 | 2 | 0 | -- |
| PID | 368 | 13 | 6 (2, 4, 6, 9, 10, 12) | 7, 8, 11, 13 |
| PV1 | 368 | 36 | **29** | 3, 6, 7, 19, 36 |
| IN1 | 106 | 4 | 1 (IN1-2) | -- |
| NPU | 385 | 2 | 0 | -- |
| MRG | 3 | 1 | 0 | -- |
| ORC | 60 | 2 | 0 | -- |
| OBR | 60 | 4 or 7 | 2 (OBR-2, -3) | 5, 6 |
| OBX | 150 | 14 | 6 (4, 9, 10, 11, 12, 13) | -- |

Two facts fall out of that table and both matter to arc 4.

**PV1 is 29/36 blank.** `pv1-segment`'s own docstring calls the block
between PV1-7 and PV1-36 "the 28 fields"; sweep 1 spent one of them on
PV1-19. The remaining 27, plus PV1-4 and PV1-5, are what a positional
pad costs today. Arc 4 adds no PV1 field: nothing it proposes lives
there.

**MSH-5 and MSH-6 are blank on every message emitted under the default
profile** -- `site-profile/default-msh` ships `:receiving-app ""` and
`:receiving-facility ""`, and only an authored `:msh` dialect fills
them (the `site-profiles` trace's `messages-aldric.txt` does; every
other corpus in the tree does not). That is the field a real subscriber
routes on, and section 2(f) makes it fan-out's natural home: a
subscriber table that fills MSH-5/6 per subscriber costs no new segment
and no new field position, and reuses the override path
`effective-msh` already implements.

#### (iii) Every consumer that reads the wire, and what an unseen family does to it

| consumer | what it does with an MSH-9 it has never seen |
|---|---|
| `ehrt.sim-emit-hl7.v2-replay/evolve-entry` | **THROWS.** `(throw (ex-info "v2-replay: unsupported message trigger" {:trigger trigger}))`. Handled set: A01, A02, A03, A04, A11, A12, A13, A17, A20, A40, O01, R01 |
| `v2-replay/replay-messages` (the emitter-coherence property) | propagates the throw -- **the property test dies**, it does not fail softly |
| `ehrt.corpus.board/fold-event` | catches exactly that ExceptionInfo and returns `{:acc acc :unfolded? true}` -- a **counted, cued skip**. The bed board simply does not see the message |
| `ehrt.judge-v2-hapi.v2` (`gate v2`) | parses with a strict/default-validation HAPI `PipeParser`. See (iv): at MSH-12 `"2.3"` **every** message resolves to `ca.uhn.hl7v2.model.GenericMessage$V23`, so an unseen family is as structurally unchecked as a seen one |
| `ehrt.judge-v2-nist.v2` (`gate v2-nist`) | needs an explicit `--profile` bundle. The only bundle in the tree is `test-fixtures/v2-nist/COVID19_ELR-v2.3.1`, which describes somebody else's messages. **This tier cannot run over this project's own corpus at all today** |
| `ehrt.corpus.intake` | trigger-agnostic. It sniffs `:v2-er7` by format and hashes content; a new family is invisible to it, benignly |
| `ehrt.corpus.player` (`ehrt play`) | paces on MSH-7 and shows MSH-7/MSH-9/PID-3 on the compact line. A new family shows up as itself; a PID-less family (A20's shape) renders an empty PID-3 |

**The first row is the binding constraint on every add-on in this ADR.**
A message family that reaches the wire without a matching
`evolve-entry` arm kills `v2-replay-test`'s emitter-coherence property
outright. Every add-on below therefore co-lands its own fold arm --
even the trivial one (`"A08" entry`, folding the PID and nothing else).

**And one more, less obvious.** `emit_hl7_test`'s
`bidirectional-derivability` asserts `(= (count events) (count
messages))` over the triple `(PID-3, MSH-9, MSH-7)`. **That is a
bijection, and chatter is not one.** The property runs on
`engine/run {:seed .. :patients ..}` with no config at all, so an
opt-in add-on leaves it green by construction -- but the LAW it
encodes has to be restated before the first add-on lands, or the
project will be shipping messages under a law that says they cannot
exist. Section 4 states the replacement.

#### (iv) The vendored trigger/structure table, and what 2.3 changes

Read from `hapi-structures-v24` 2.6.0. `ca/uhn/hl7v2/parser/eventmap/2.4.properties`:

```
ADT_A04 -> ADT_A01      ADT_A28 -> ADT_A05      SIU_S13..S24, S26 -> SIU_S12
ADT_A08 -> ADT_A01      ADT_A31 -> ADT_A05
```

Structures, by instantiating each class and calling `getNames`:

```
ADT_A01  [MSH EVN PID PD1 ROL NK1 PV1 PV2 ROL2 DB1 OBX AL1 DG1 DRG PROCEDURE GT1 INSURANCE ACC UB1 UB2 PDA]
ADT_A05  [MSH EVN PID PD1 ROL NK1 PV1 PV2 ROL2 DB1 OBX AL1 DG1 DRG PROCEDURE GT1 INSURANCE ACC UB1 UB2]
DFT_P03  [MSH EVN PID PD1 ROL PV1 PV2 ROL2 DB1 COMMON_ORDER FINANCIAL DG1]
  FINANCIAL           [FT1 FINANCIAL_PROCEDURE FINANCIAL_COMMON_ORDER DG1 DRG GT1 INSURANCE]
SIU_S12  [MSH SCH NTE PATIENT RESOURCES]
  PATIENT             [PID PD1 PV1 PV2 OBX DG1]
  RESOURCES           [RGS SERVICE GENERAL_RESOURCE LOCATION_RESOURCE PERSONNEL_RESOURCE]
ACK      [MSH MSA ERR]
```

Field counts and the positions arc 4 names, by `numFields` and
`getNames` on the instantiated segment: NK1 37 (NK1-2 NK Name, NK1-3
Relationship, NK1-4 Address, NK1-5 Phone Number, NK1-7 Contact Role);
FT1 26 (FT1-4 Transaction Date, FT1-6 Transaction Type, FT1-7
Transaction Code, FT1-10 Transaction Quantity, FT1-11 Transaction
Amount - Extended, FT1-12 Transaction Amount - Unit, FT1-19 Diagnosis
Code - FT1, FT1-25 Procedure Code); SCH 27 (SCH-1 Placer Appointment
ID, SCH-2 Filler Appointment ID, SCH-7 Appointment Reason, SCH-9
Appointment Duration, SCH-11 Appointment Timing Quantity, SCH-25 Filler
Status Code); ORC 25 (**ORC-5 Order Status**); OBR 47 (**OBR-25 Result
Status**, OBR-22 Results Rpt/Status Chng - Date/Time); OBX 19
(**OBX-11 Observation Result Status**); MSA 6 (MSA-1 Acknowledgement
Code, MSA-2 Message Control ID, MSA-3 Text Message).

**What 2.3 changes: nothing this clone can check, and that is the
finding.** There is no v2.3 structure library on any classpath here and
no 2.3 eventmap in any jar or resource -- the same wall
`bed-status-message`'s docstring hit and recorded honestly. What IS
checkable is what HAPI actually does with a 2.3 message, and it is
worse than "unchecked":

```
PipeParser, exactly as ehrt.judge-v2-hapi.v2 constructs it:
  2.3 ADT^A01  ->  ca.uhn.hl7v2.model.GenericMessage$V23
  2.3 ADT^A08  ->  ca.uhn.hl7v2.model.GenericMessage$V23
  2.3 SIU^S12  ->  ca.uhn.hl7v2.model.GenericMessage$V23
  2.3 DFT^P03  ->  ca.uhn.hl7v2.model.GenericMessage$V23
  2.3 ADT^A20  ->  ca.uhn.hl7v2.model.GenericMessage$V23
```

**Every one of the probe corpus's 747 messages parses to
`GenericMessage$V23`.** With no v2.3 structures to resolve against,
HAPI falls back to a generic message: no segment order, no cardinality,
no required-segment check, no primitive typing. The base-structural
gate this project ships is, over this project's own output,
structurally vacuous. That is not a reason to panic -- `gate v2` still
catches encoding and delimiter damage on foreign corpora, which is what
its fixtures exercise -- but it does mean the version question is not
cosmetic. Section 2(e) has the measurement that settles it.

### 2. Design

Each item is classified by `rulings.md#R-skeleton-or-emission`
explicitly, before anything else: *if downstream invariants or later
messages' content must respect it, it is skeleton; if it is derivable
restatement, it is emission.*

#### (a) Re-statement chatter -- A08, A31, A28, IN1-only

**Classification.** No engine invariant reads an A08. No later message's
CONTENT changes because one was or was not sent -- the PID a later
message renders comes from `demographics-timeline`, which folds the
`:demographic-update` event, not the message. Every field of every
proposed message is `demographics-at` of a patient at an instant, which
is the definition of derivable restatement. **EMISSION.**

**Trigger selection, derived and not configured.** Real practice splits
A08 (visit-scoped patient update) from A31 (person-scoped) on whether
there is a visit. **The log already answers that on the event itself**:
with `:encounters` on, every encounter-scoped kind carries
`[:encounter-id {:optional true} :string]`, present exactly when the
event happened inside an open encounter. No fold is needed and no
second derivation can disagree with the first. So:

| basis | trigger | segments |
|---|---|---|
| `:demographic-update`, encounter open at `t` | ADT^A08 | MSH EVN PID PV1 |
| `:demographic-update`, no encounter open | ADT^A31 | MSH EVN PID |
| `:coverage-change` | ADT^A08 or A31 by the same rule, **IN1 only** -- PID unchanged, IN1 carrying the new payer | + IN1 |
| `:registered` | ADT^A28 | MSH EVN PID |
| periodic re-statement, at a config ratio | A08/A31 by the same rule | as above |

**And the split is lopsided, measured, in a way that changes what ruling
(B) actually buys.** Only **1 of 193** `:demographic-update` events and
**1 of 72** `:coverage-change` events in the probe corpus carries an
`:encounter-id` at all -- the person process walks a 20-year horizon
while the clinical content is one ED shift, so demographic churn happens
almost entirely BETWEEN encounters. Under the rule above the
event-driven half is therefore ~99.5% **A31**, with two A08s in the
whole corpus. That is not a defect of the rule; it is what the modelled
world is. It does mean the **A08 volume comes from the PERIODIC half**,
not the event-driven half -- a periodic re-statement fires while an
encounter is open, which is exactly where a real interface's A08 traffic
comes from. ADR-0168's stated motivation ("a known downstream consumer
wants high-volume demographic-update traffic (A08/A31 class)") is
therefore served by the periodic ratio, and a sweep that shipped only
the event-driven half would satisfy the letter of this design and almost
none of its point.

This does NOT lift `person-simulator` limitations row 5 (legal name
change vs data-entry correction, which the row says A08-vs-A31 usage
distinguishes in real practice and v1 does not). The split above keys
on ENCOUNTER STATE, not on cause; row 5 stands untouched, and saying so
here is the point -- a register row must not be struck by a side effect.

**Mechanism: `plan-chatter`, a sibling of `plan-latency`.** RNG x GT x
ChatterProfile -> a vector of render instructions
`{:at t :trigger "A08" :patient-id .. :basis i :ordinal k}`. `emit-wire`
renders each through the existing `pid-segment`/`pv1-segment`/
`in1-segment` builders and merges them into its own transmit-time sort.
Sampling stays OUT of `emit_hl7`'s render path, per that namespace's
renders-only doctrine; `plan-chatter` takes an explicitly-passed
`java.util.Random` and no wall clock, exactly as `plan-latency` does,
and obeys the same fixed-consumption law: **one draw per ground-truth
event, in log order, drawn and discarded for an event chatter does not
cover**, so adding a rule for kind X can never shift kind Y's draws.

**The RNG stream is already reserved.** `engine/stream-family-tag` has
`:emission 5`, and ADR-0171 ruling C1's own comment in
`ehrt.sim.run` says why in as many words: *"arc 4 adds chatter, fan-out
and status ladders to this side, and one `mix64` decorrelates them
before that lands."* Latency holds id-tag 0. Chatter takes id-tag 1,
the ladder 2. No new family, no new law.

**Control ids must change shape.** `control-id-for` builds
`mrn-trigger-t`. Two restatements of the same patient at the same
instant would collide, and MSH-10 uniqueness is what
`bidirectional-derivability` keys on. Chatter messages take
`mrn-trigger-t-<ordinal>`, ordinal counting within `(mrn, trigger, t)`
-- the same shape `:bed-status-change`'s arm already uses to
disambiguate two legs of one bed at one instant.

**Volume, measured.** The probe corpus carries 193
`:demographic-update` (causes: `:residence-move` 157,
`:identity-correction` 22, `:residence-loss` 7, `:identity-fill` 7;
fields: `:residence` 164, `:name` 18, `:identity` 7, `:dob` 4), 72
`:coverage-change`, and 103 `:registered`. At a 1.0 ratio with A28 on,
chatter alone adds **368 messages to 747, +49.3%**. At 1.0 without A28,
**+265, +35.5%**. The ratio is what makes that a knob rather than a
fact.

*Rejected.* **(1) Make A08 a ground-truth kind.** It is derivable
restatement, so `R-skeleton-or-emission` classifies it emission; and a
new kind draws, which reshuffles, which `R-mix-7` forbids for this arc.
**(2) Render the A08 inside `event->messages` as a second message for
`:demographic-update`.** Works for the event-driven half and cannot
work for the periodic half, which has no event to hang on; and it would
move `emit`, which ADR-0109 froze byte-for-byte as `emit-wire`'s own
oracle. **(3) Sample inside `emit`.** Violates the no-RNG-in-emit
doctrine. **(4) Skip A31, send A08 always.** Cheaper, and wrong in the
one case a consuming MPI cares about: a demographic change on a patient
with no open encounter is precisely the person-level update A31 exists
for.

#### (b) Status ladders -- ORC-5, OBR-25, OBX-11

**Classification.** `:result-available` carries `:order-event-id`, the
LOG INDEX of its own order. So `(t_order, t_result)` is in hand for
every result, and a rung at a fixed fraction of that interval is a pure
function of the log. No invariant reads a rung. **EMISSION.**

**Shape.** Between an order and its result, emit `r` restatements at
deterministic fractions of the interval (`k/(r+1)`, k = 1..r), each an
ORU^R01 carrying the SAME OBR as the final result with **OBR-25** set
to the in-progress code and **OBX-11** likewise, and the final
`:result-available` message gaining the final code in the same two
fields. ORM^O01 restatements carry **ORC-5**. Positions are cited from
the jar in section 1(iv); the message shapes are the ones
`oru-message`/`orm-message` already build.

**The code VALUES are not in this tree, and are handled the way the
tree already handles that.** `hapi-structures-v24` carries structures,
not HL7 tables; tables 0038/0123/0085 appear in no jar and no resource
here. So the ladder ships its vocabulary as a **site-profile code
table** -- `:order-status`, `:result-status`,
`:observation-result-status` beside the existing `:patient-class`,
`:discharge-disposition` and `:bed-status` tables in
`ehrt.sim-emit-hl7.site-profile` -- with authored defaults. That makes
the un-citable half declared data in one overridable place rather than
a constant asserted from memory, which is the same move sweep 2 made
for NPU-2.

**A vacuity warning that must not be discovered later.** The oracle's
committed `witnessed-event-kinds` contains **neither `:order-placed`
nor `:result-available`**, and `witnessed-message-types` contains no
`ORM^O01`. **No oracle root places an order.** A ladder turn-on will
therefore produce `IDENTICAL` across all 39 roots while changing every
order in every gated corpus. An `IDENTICAL` over a population that
contains none of the thing under test proves nothing --
`rulings.md#R-empty-population-is-red`'s own reasoning, one layer up.
The ladder's evidence must be the six gated corpora (the probe corpus
carries 30 order/result pairs), and its sweep owes that statement in
its own commit message rather than letting the oracle's silence read as
agreement.

**Volume, measured.** 30 pairs in the probe corpus; at `r = 1` that is
**+30 messages, +4.0%**.

*Rejected.* **(1) Ladder rungs as skeleton events.** They would draw and
reshuffle; and no invariant reads them. **(2) Rungs at sampled times.**
A sampled rung is still emission, but it costs a second RNG consumer
for no realism the fixed fractions do not already buy, and it makes the
rung un-derivable from the log alone. **(3) Put the status on the
existing final ORU only.** That is not a ladder -- it is a field, and
it moves every existing result message's bytes for nothing.

#### (c) DFT^P03 charges

**Classification.** A charge line restates a fact already in the log (a
procedure, an order, an occupied bed-day). The AMOUNT is not in the log
-- but an amount derived from a code via a config table is a pure
function of `(log, config)`, which is what `:site-profile` and
`:latency` already are. **EMISSION**, conditional on the price table
being emission config and never ground truth.

**Shape.** One DFT^P03 per encounter close (`:discharge`,
`:outpatient-visit-end`), carrying `[MSH EVN PID PV1]` then one FT1 per
chargeable fact of that encounter: `:procedure` (which carries
`:codes`) -> FT1-25 Procedure Code; `:order-placed` (which carries
`:concept`) -> FT1-7 Transaction Code; one room-and-board line per
inpatient day between admission and discharge. FT1-4 is the
transaction date, FT1-6 the transaction type, FT1-11/FT1-12 the
extended and unit amounts, all cited from the jar above.

**This is what `event->messages` was already shaped for.** Its docstring
says a vector is returned because "a future many-messages-per-event
type is now a shape this stage already accommodates". A DFT at a
`:discharge` is the second message for an event that already renders an
ADT^A03 -- the first real use of that accommodation.

**Volume, measured.** 114 `:discharge` + 34 `:outpatient-visit-end` =
**148 closes, +19.8%** at one DFT per close.

*Rejected.* **(1) Charges as ground truth.** Invented money is not a
clinical fact, no invariant reads it, and minting it would reshuffle.
**(2) One DFT per chargeable event.** Real financial feeds batch per
account or per encounter; per-event would be more messages and less
realism. **(3) Derive amounts from a random draw.** Rejected on the
same ground as (b)(2), with more force: a price that changes per run is
not a price.

#### (d) NK1 from household state -- **NOT IN ARC 4**

**Classification, and the block.** NK1 content would be derivable
restatement -- of state the emitter cannot see. Measured, three ways:

1. `ehrt.sim-engine.person-fold/demographic-kinds` is
   `#{:residence-move :residence-loss :identity-correction
   :coverage-change}`, and its own docstring says the `:household-*`
   family "mints nothing HERE".
2. `engine/run` returns `{:ground-truth :state-history :facility
   :providers}` (plus `:exhausted`). **`:person-index` is world state
   and is not in it.** `:state-history` is per-patient `evolve` state;
   no household anywhere.
3. `NK1` occurs in no `src` file in the tree --
   `person-simulator`'s `no-emitter-writes-nk1-test` asserts exactly
   that, and it is green.

So there is no household fact in the emitter's input, and every route
to one is out of this arc's fence:

*Rejected.* **(1) Put household membership into ground truth.** That is
a fact-generator change -- arc-3 species work, and this session's own
fence says STOP. **(2) Thread a household index into `emit` as a
non-log emission input, the way `:site-profile` rides.** Rejected on a
stronger ground than the fence: it would put content in a MESSAGE that
the LOG does not contain, so a consumer holding
`ehrt sim run --format ground-truth` could no longer reproduce the
wire. That is the derivability law itself, not a convention. **(3)
Infer co-residence from identical PID-11 addresses.** Unsound, and
measurably so: geography is a **24-row** `places.edn` pool
(`components/sim-model/resources/sim-model/demographics/places.edn`,
counted), so the probe corpus's 89 distinct MRNs render only **22
distinct PID-11 values**: 19 of those 22 addresses are shared by more
than one MRN, **86 of the 89 MRNs sit on an address shared with a
stranger**, and one address carries 7. Address identity is not
household identity here, and it is not close.

**Recommendation: ADR-0172 limitations row 8 STANDS, and arc 4 lifts no
row of it.** The day households owe a rendering row is the day they
reach the log; that is the correct trigger and it has not fired.

#### (e) MSH-12: settle it

**Measured, over the probe corpus's 747 messages, through the judge's
own `PipeParser` with `ValidationContextFactory/defaultValidation`:**

| MSH-12 | parses | fails | resolved structures |
|---|---|---|---|
| `"2.3"` (as shipped) | 747 | 0 | `GenericMessage$V23` x 747 |
| `"2.4"`, nothing else changed | 401 | **346** | ADT_A20 385, ADT_A01 8, ADT_A03 8 |
| `"2.4"`, PID-13 blanked | **747** | **0** | see below |

**All 346 failures are one cause, and it is not structural:**

```
ca.uhn.hl7v2.validation.ValidationException: Validation failed:
Primitive value '492-292-0567' requires to be empty or a US phone
number at PID-13(0)
```

The persona phone regex in the contract is `^\d{3}-\d{3}-\d{4}$` and
`pid-segment` renders it verbatim. HAPI's v2.4 TN primitive rule wants
the parenthesised area code; probed directly: `"(303)292-0567"` OK,
`"(303)292-0567X1234"` OK, `""` OK, `"492-292-0567"` FAIL,
`"3032920567"` FAIL, `"(303) 292-0567"` FAIL.

With PID-13 blanked, all 747 resolve into real v2.4 structures and
nothing else fails:

```
ADT^A01 -> ADT_A01 (114)   ADT^A11 -> ADT_A09 (1)    ADT^A20 -> ADT_A20 (385)
ADT^A02 -> ADT_A02 (22)    ADT^A12 -> ADT_A09 (7)    ADT^A40 -> ADT_A39 (3)
ADT^A03 -> ADT_A03 (114)   ADT^A13 -> ADT_A01 (1)    ORM^O01 -> ORM_O01 (30)
ADT^A04 -> ADT_A01 (34)    ADT^A17 -> ADT_A17 (6)    ORU^R01 -> ORU_R01 (30)
```

**So the version flip is not one field. It is a conformance event, and
it is one emission-side field away from being free.** Rendering PID-13
as `(NNN)NNN-NNNN` is a change to `pid-segment` alone; the ground-truth
persona keeps its own `NNN-NNN-NNNN` shape and the event contract does
not move at all.

*Rejected.* **(1) Keep `"2.3"` and emit only what 2.3 defines.**
Unfalsifiable in this clone -- there is no 2.3 trigger table to define
"what 2.3 defines" against -- and it preserves a gate that resolves
every message to a generic structure. Choosing it means choosing to
keep the base-structural tier vacuous over our own corpus. **(2)
Declare 2.4 and ship the phone as-is.** Ships a corpus of which 46.3%
fails a gate this project itself distributes. **(3) Add
`hapi-structures-v23` so the 2.3 claim becomes checkable.** Honest, and
the only route that would VERIFY the status quo -- but it adds a
dependency to the judge tier for a version we would then be arguing to
leave, and 2.3's own TN rule would very likely redden the same 346
messages (unmeasured, and named as unmeasured). Kept on the table for
the author because it is the one option that tests rather than rules.

**Recommendation: declare 2.4.** As its own sweep, in two commits in
this order, so each digest delta has exactly one cause: PID-13's
rendering first (declared change, bytes move), MSH-12 second (declared
change, every message's bytes move once). MSH-12 is already a
site-profile field (`site-profile/effective-msh`), so a site that must
speak 2.3 keeps `{:msh {:version "2.3"}}` and gets exactly today's
bytes.

#### (f) Fan-out -- the subscriber table

**Classification.** A filter over an already-rendered stream. It creates
no content, reads no state, and draws nothing. **EMISSION**, and the
purest instance of it in this ADR.

**Shape.** `:fan-out [{:name :adt-feed :filter {:message-types #{"ADT^A01"
"ADT^A02" "ADT^A03"} :patient-class #{:inpatient}} :msh {:receiving-app
"ADT-CONSUMER" :receiving-facility "WEST"}}]`, riding `:config` the way
`:site-profile` and `:latency` do. Each subscriber writes its OWN spool
-- a `dir:` Sink under the corpus root, one directory per `:name`, the
composability the source/sink design already gives for free.

**MSH-5/MSH-6 are the payoff and they are free.** Section 1(ii)
measured them blank on every message emitted under the default profile.
A per-subscriber `:msh` override fills them through the mechanism
`effective-msh` already implements, so a subscriber's spool is routable
on its face without one new field position.

**One rule that must be written down rather than discovered.** A
patient-class filter reads PV1-2, and ADT^A20 has no PV1 at all
(`[MSH EVN NPU]`, and 385 of the probe corpus's 747 messages are A20).
A class filter therefore EXCLUDES every PV1-less message unless the
subscriber names its trigger explicitly in `:message-types`. Stated as
a rule, this is sane routing; discovered as a behaviour, it is a
silently empty bed-management feed.

**The law worth gating:** every subscriber's spool is a SUBSEQUENCE of
`emit-wire`'s own output, in the same order, with the same bytes. A
property test over a generated subscriber table -- and it owes a RED
WITNESS, because a property that has never failed is a property nobody
has tested. The mutation that produces one is exactly the rejected
alternative below: render each subscriber's stream separately, and the
byte-equality half goes red the moment two subscribers disagree about
one MSH-10. Capture that run and put its output in the sweep's record,
per this repo's standing born-red discipline.

**The accumulator-wiring row, corrected.** `roadmap.md#corpus-player-slices`
lists "accumulator wiring" as unbuilt. Most of it is built:
`ehrt.corpus.board` folds a paced stream through
`sim-emit-hl7/fold-message`, which IS the accumulator, and ships as
`ehrt play PATH --board`. What is actually left is exposing the
accumulator's own final state as an output rather than only as a
rendered whiteboard -- which a per-subscriber spool makes nearly free.
Recorded here rather than left as a row whose name overstates it, which
is the repo-review-5 species.

*Rejected.* **(1) Render each subscriber's stream separately.** N times
the cost, and it permits two subscribers to hold different bytes for
one MSH-10. **(2) Route at the transport instead of on disk.** Against
`corpus-io.spool`'s own law -- every corpus is replayable, and
determinism claims attach to the spool, not the wire.

#### (g) Transport -- `:mllp` as a sink kind

**Classification. NOTHING here is skeleton, and nothing here is even
emission content** -- it is delivery. It renders no field and derives
no value.

**What exists.** `:mllp` is already a `Framing` value in
`ehrt.corpus-io.source-sink/Framing`, with a byte-exact codec in
`framing.clj` and round-trip, charset-law, concrete-example and
malformed-input tests. **What does not exist is a socket.**
`known-sink-kinds` is `#{:dir :file :stdout :blaze}` and
`implemented-sink-kinds` is `#{:dir :file :stdout}`.

**The name collision, resolved explicitly.** `:mllp` as a `:framing`
means "these bytes are VT/FS-CR framed"; `:mllp` as a `:kind` would
mean "send these to a socket". They live in different fields of the
same map and never collide mechanically, but they WILL collide in a
reader's head. Proposal: the sink kind is `:mllp`, and a `:mllp` sink
IMPLIES `:framing :mllp` -- declaring any other framing on it is a
construction-time error, not a silent override. One sentence in
`source_sink.clj`'s own docstring pays for the collision.

**What is judged.** Two things, and they are the two ADR-0014 named
when it deferred this: **framing** (already property-tested, and the
sink reuses the codec rather than re-implementing it) and **ACK
pairing** -- MSA-1 the acknowledgement code, MSA-2 echoing the sent
MSH-10, per `ACK`'s `[MSH MSA ERR]` structure and MSA's six fields,
both read from the jar. The pairing law: for every message sent, an ACK
whose MSA-2 equals that message's MSH-10, and no ACK for a message
never sent.

**Player surface: extend the designator, do not add a flag.** The prompt
sketched `--transport`. `ehrt play --sink` already takes a designator
and its own help text already says "dir: and blaze: are recognized but
deferred" -- so `--sink mllp://host:port` costs no new flag, no new
help group, and inherits ADR-0017's designator vocabulary and its
round-trip parse/print law. Disclosed as a deviation from the prompt's
own sketch, chosen because the seam exists.

**ADR-0014's own assessment stands and should be quoted at whoever picks
this up:** building the MLLP sink was "found to cross three namespace
boundaries rather than one", and "a half-built network sink with no ACK
handling and untested lifecycle is a worse outcome than a clearly named
deferral." Nothing since has narrowed that. It is the most expensive
item in this ADR and the only one that is not a pure function.

#### (h) Gating at scale

**Measured, this clone, single host, warm JVM.** `bin/ehrt gate v2` over
the probe corpus spooled one message per file (747 files): 16.004 s and
16.515 s wall. The same command over a one-file directory: 12.221 s and
12.389 s. Fixed cost is therefore ~12.3 s (JVM start, HAPI context,
CLI); marginal cost is **(16.26 - 12.31) / 746 = 5.3 ms/message, about
189 messages/second**. At 10^6 messages that is **~88 minutes for the
base-structural tier alone**. Labelled ESTIMATE for the projection and
MEASURED for the two rates; host quietness was not sampled on the
Windows side at the moment of the figure, so this is an
order-of-magnitude price, not a figure of record.

The profile tier is **unmeasured, because it cannot run**: `gate v2-nist`
requires `--profile`, and the only bundle in the tree describes COVID-19
ELR messages. Writing a profile bundle for this project's own message
families is its own body of work and is not proposed here.

**Proposed policy.**

* **Full width on skeleton-kind messages.** Every message whose basis is
  one of the registry's kinds is gated, always. That is 747 of 747
  today, and stays every message the arcs 1-3 contract produces.
* **Stratified sampling on add-on messages.** One stratum per MSH-9. Per
  stratum, gate `min(n, cap)` messages.
* **The sample is DERIVED, not drawn.** Sort each stratum by MSH-10 --
  a total order over a value this project mints itself -- and take the
  first `cap`. No RNG, no seed to thread, no
  `R-no-derivation-through-nondeterminism` exposure, and any reader can
  recompute the selection from the corpus alone.
* **No silent caps.** The gate reports, per stratum, `n` and how many
  were gated. A truncation nobody prints reads as full coverage.

**The gate that asserts the sample is drawn deterministically**, born
red on a shuffled input: select over a corpus, shuffle the corpus, select
again, assert set equality -- and assert every stratum non-empty first
(`rulings.md#R-empty-population-is-red`), because a selector that
returns nothing passes a set-equality check trivially. That last clause
is the whole reason to write the gate this way rather than the obvious
way.

### 3. Rulings needed

Each is lettered, with a recommendation. When the author rules, the
declined options stay here unstruck -- what was declined is what makes
the selection mean something (ADR-0174's own convention).

**(A) MSH-12: 2.3 or 2.4?**

**RULED A1, 2026-08-27.** *Recommendation: **A1 -- declare 2.4, in its
own sweep, PID-13's rendering first.*** Section 2(e)'s measurement is
what settles it: at `"2.3"` all 747 probe-corpus messages resolve to
`GenericMessage$V23`, so the base-structural tier this project ships is
vacuous over this project's own output; at `"2.4"` with PID-13 rendered
`(NNN)NNN-NNNN`, all 747 resolve into real v2.4 structures. Landed by
arc 4 sweep 1.


* **A1 (RECOMMENDED) -- declare 2.4, in its own sweep, PID-13's
  rendering first.** Two commits, each a declared digest change with one
  cause. Buys: real structure resolution for every message (the table in
  2(e)), and it unblocks SIU, which is the only thing standing between
  scheduling and the wire. Costs: every message's bytes move once; six
  gated corpora and 36 engine-layer oracle roots re-pin.
* **A2 -- keep 2.3, emit only what 2.3 defines.** No bytes move.
  Requires an in-tree 2.3 trigger table to be meaningful, which does not
  exist; without one this is a rule nothing can check, and the SIU
  family stays permanently ground-truth-only.
* **A3 -- add `hapi-structures-v23` and re-decide with 2.3 checkable.**
  The only option that tests rather than rules. Costs a judge-tier
  dependency and a session, and likely reddens the same 346 messages
  under 2.3's own TN rule.

**(B) Which add-ons are v1?**

**RULED B1, 2026-08-27.** *Recommendation: **B1 -- chatter
(A08/A31/A28/IN1) and DFT^P03 first; status ladders second; SIU only
after (A); fan-out and MLLP as the player slices, priced separately.***
NK1 is out of v1 entirely, per section 2(d).


* **B1 (RECOMMENDED) -- chatter (A08/A31/A28/IN1) and DFT^P03 first;
  status ladders second; SIU only after (A); fan-out and MLLP as the
  player slices, priced separately.** The channel's lean, adjusted in
  one place: **NK1 is dropped from v1 entirely** -- section 2(d) shows
  it has no source, and pricing it would be pricing arc-3 work under an
  arc-4 fence. The remaining v1 is measured at **+516 messages on 747,
  +69%** (chatter 368 at ratio 1.0 with A28; DFT 148), before ratios.
* **B2 -- chatter only in v1, everything else deferred.** Smallest
  first sweep. It leaves DFT for a sweep that may never come, and DFT
  is the one item in this design that carries the FINANCIAL half of a
  hospital feed at all -- a whole message family this project has never
  emitted, versus a ladder that decorates one it already does.
* **B3 -- ladders first, chatter second.** Rejected as a
  recommendation for one measured reason: **no oracle root places an
  order**, so the ladder's first sweep would have no oracle evidence at
  all, and starting an arc with its least-observable item is how a
  vacuous gate gets shipped.

**(C) Chatter ratios: run config or a separate emission profile file?**

**RULED C1, 2026-08-27.** *Recommendation: **C1 -- `:chatter` rides
`:config`, exactly as `:latency` and `:site-profile` do.***


* **C1 (RECOMMENDED) -- `:chatter` rides `:config`, exactly as
  `:latency` and `:site-profile` do.** One precedent, already load-bearing,
  already documented in `ehrt.sim.run`'s own docstring, already
  excluded from `engine/config-keys` so it provably cannot reach
  `engine/run`. Absent entirely is the byte-identical path.
* **C2 -- a separate `emission-profile.edn`, a fourth file beside
  pathway/order-profile/site-profile.** Cleaner conceptually; costs a
  new file format, a new loader, a new validation surface and a new
  provenance question, to hold what is three keys.
* **C3 -- fold the ratios into the site profile.** Tempting, since site
  profiles already own rendering dialect. Rejected: a site profile
  binds at emit time and changes no message COUNT; chatter changes how
  many messages exist. Putting a volume knob in a dialect file would
  break the one-sentence description that makes site profiles
  comprehensible.

**(D) Gating policy at scale.**

**RULED D1, 2026-08-27.** *Recommendation: **D1 -- full on skeleton
kinds, MSH-10-ordered stratified sampling on add-ons, per-stratum counts
printed, and a born-red determinism gate.*** Section 2(h).


* **D1 (RECOMMENDED) -- full on skeleton kinds, MSH-10-ordered
  stratified sampling on add-ons, per-stratum counts printed, and a
  born-red determinism gate.** Section 2(h).
* **D2 -- full width always.** ~88 minutes per 10^6 messages on the
  base tier, which the program plan already calls hours-class. Honest,
  and it will simply not be run.
* **D3 -- sample uniformly across the whole corpus.** Simpler; at the
  probe corpus's mix a uniform 5% sample draws ~19 A20s and, in
  expectation, less than one A13. Stratification is what keeps a rare
  family from vanishing.

**(E) Landing order and the per-add-on shape.**

**RULED E1, 2026-08-27.** *Recommendation: **E1 -- every add-on gets
its OWN opt-in key, absent by default, and lands in two commits: DARK
then ON.*** Ground truth digests must be IDENTICAL at BOTH commits.
**Arc 4 sweep 1 found that the oracle cannot state that half on its
own**: `digest.clj`'s `-main` writes the `{:ground-truth :hl7}` pair as
ONE file per root, so `bin/regression-oracle` hashes the two halves
together and an emission-only change makes every engine-layer root
DIFFER. Section 4's sentence *"`bin/regression-oracle <base> <target>`
reports `IDENTICAL` on every root's `:ground-truth`"* therefore names
something the script could not do when it was written. The instrument
E1 needs is `bin/ground-truth-bracket <a> <b>`, landed by sweep 1 as
its own output-identical commit: same worktrees, same classpath, same
`digest.clj`, digesting `(:ground-truth root)` alone across the 36
engine-layer roots. Every arc-4 sweep runs BOTH brackets --
ground-truth IDENTICAL, message digests declared.


* **E1 (RECOMMENDED) -- every add-on gets its OWN opt-in key, absent by
  default, and lands in two commits: DARK (mechanism plus gates, oracle
  IDENTICAL, no corpus opts in) then ON (the corpora opt in, and the
  ONLY thing that moves is emitted messages).** The arc-0 shape applied
  to the wire, and the shape arcs 3a and 3b both landed under. Ground
  truth digests must be IDENTICAL at BOTH commits -- that is the whole
  claim of arc 4 -- and the message digests move, declared, at the
  second.
* **E2 -- one key for all of arc 4 (`:emission-add-ons`).** Fewer keys;
  makes every turn-on delta have several possible causes at once, which
  is exactly what arcs 3a and 3b refused.
* **E3 -- land on by default, no opt-in.** No.

### 4. Consequences

**The proof shape, fixed here for every arc-4 sweep.** Ground truth is
byte-identical: `bin/regression-oracle <base> <target>` reports
`IDENTICAL` on every root's `:ground-truth`, and any digest movement is
on the `:hl7` half alone and is DECLARED. `engine/run`'s output does not
move because nothing arc 4 proposes reaches `engine/config-keys` -- the
`:latency` precedent, verbatim. A sweep that finds a ground-truth digest
moving has found a bug in its own design, not a result.

**The derivability law must be restated before the first add-on lands.**
`emit_hl7_test/bidirectional-derivability` encodes it as a bijection
between log events and messages. Chatter is not a bijection: a periodic
A08 has no event of its own, and a DFT is a second message for an event
that already has one. The replacement law: **every message is derivable
from `(log, emission-config)`, and every message maps to exactly one
`(basis-event-index, trigger, ordinal)` triple, which is what its MSH-10
carries.** The existing property stays green untouched (it runs with no
config, so no add-on fires), and the new law gets its own property over
a config that does fire. Writing the new one is the first sweep's first
commit, not an afterthought in its last.

**Every add-on co-lands a `v2-replay/evolve-entry` arm.** Section 1(iii):
an unhandled trigger throws, and the throw kills the emitter-coherence
property rather than failing it softly. The arms are small -- an A08
folds the PID and nothing else -- but they are not optional.

**`witnessed-message-types` moves on every turn-on, and the mover set
must be predicted before editing.** `digest.clj`'s committed set is
`#{"ADT^A01" "ADT^A02" "ADT^A03" "ADT^A04" "ADT^A11" "ADT^A12"
"ADT^A13" "ADT^A17" "ADT^A20" "ADT^A40" "ORU^R01"}`, and
`projects/integration`'s `oracle_coverage_test` asserts it exactly. Of
the 39 roots, four carry the arc-3 opt-ins (`demographic-fold`,
`encounter-horizon`, `bed-cycle`, `scheduling`), so chatter's movers
are expected to be a subset of those four -- **expected, not asserted:
re-derive it against the tree at implementation time rather than citing
this sentence.** That gate lives in `make integration`, which is not in
`make test` and not in per-push CI.

**What this ADR deliberately does not do.** It proposes no event kind,
no `components/*/src` change, no field on any existing kind, and no
change to `engine/config-keys`. It lifts no `person-simulator`
limitations row -- row 5 and row 8 both stand, and section 2(a) and
2(d) say why in the places a later reader will look. And it settles the
MSH-12 question with a measurement rather than a preference, which is
what ADR-0174 section 2(d) said the next arc would owe.

See `.agents/session-records/2026-08-27-arc-4-design.md`.
