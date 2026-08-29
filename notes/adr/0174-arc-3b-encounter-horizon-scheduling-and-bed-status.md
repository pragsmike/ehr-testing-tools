## ADR-0174 — arc 3b: the encounter horizon, scheduling state, and the bed-status cycle

**Status:** Accepted (design session 2026-08-26, HEAD `b9d4d77`;
**RULED 2026-08-26: A1 B1 C1 D1 E1** -- the recommendation on every one
of the five, with one addition the author made at ruling C: **ADT^A20
for bed status**, which C1 as recommended declined, lands in sweep 2
alongside the bed cycle it renders). A
payload session under the de-scaffold moratorium: **no engine code
lands with this ADR**, and no `components/*/src` file is touched.
Each ruling is quoted below at the option it selected; the declined
options are kept verbatim and unstruck, because what was declined is
the reason the selection means anything. Arc
3's roadmap row bundles three folds -- the demographic timeline
(ADR-0173, CLOSED), scheduling state (`rulings.md#R-mix-5`) and the
bed-status cycle (`R-mix-6`). **This ADR designs the LATTER TWO**, plus
a third thing neither ruling names and both need: the
single-encounter horizon that `.agents/plans/roadmap.md`'s
`[multi-encounter-horizon]` row leaves OWNER UNASSIGNED.

Every figure below was measured at `b9d4d77` in this clone. The two
corpora are `demos/scenarios/ed-tuesday` (seed 20260811, 100 arrivals,
`--churn`) and `demos/scenarios/clinic-decade` (seed 20260807, 200
arrivals), generated with the commands their own READMEs print.

### Context

`R-mix-5` and `R-mix-6` are both **state** rulings, decided by
`R-skeleton-or-emission`: *if downstream invariants or later messages'
content must respect it, it is skeleton.* `docs/dev/traffic-model.md`
:43-50 states what each owes:

* Scheduling -- *"appointment new/reschedule/cancel/no-show as skeleton
  events; arrivals split scheduled-correlated vs walk-in. Invariants: a
  cancel references its appointment; a scheduled encounter follows its
  appointment; a no-show has no encounter."*
* Bed status -- *"vacated→dirty→cleaning→ready, assignment gated on ready;
  world-level state interacting with boarding and the corpus-player
  bed-board sink."*

The second of scheduling's three invariants is the reason this ADR
opens with the horizon. **A scheduled encounter follows its appointment
is vacuously true in a world where no patient can have a second
encounter**, and no patient can, today, at either gated corpus:

| corpus | patients | encounter openers | max openers per patient | patients with 0 |
|---|---|---|---|---|
| ed-tuesday | 116 | 110 (`:admission` 110, `:outpatient-visit` 0) | **1** | 6 |
| clinic-decade | 230 | 118 (`:admission` 90, `:outpatient-visit` 28) | **1** | 112 |

That ceiling is not a coincidence of these seeds. It is
`check.clj:120`'s `admission-only-when-new` -- sim/ADR-0007 point 3
expressed as an invariant -- plus `evolve :discharge`
(`engine.clj:1763`), which sets `:status :discharged` and never returns
a patient to `:new`. `decide :person-encounter` (`engine.clj:898`)
names the same wall in its own docstring: *"THE `:new` GUARD IS THIS
PROJECT'S SINGLE-ENCOUNTER HORIZON, met a second time."*

This ADR argues the horizon belongs to 3b. Section 1 is the census the
argument rests on; ruling A is where the author decides.

### 1. The census, re-derived from the tree

#### (i) Where "encounter" is implicit

**`PatientState` (`engine.clj:258`) has no encounter.** It has
`:status`, `:class`, `:home-ward`, `:location`, `:attending`,
`:admitted-at`, `:discharged-at` -- seven fields that describe ONE
encounter, held directly on the patient, plus `:conditions`,
`:observations`, `:medication-orders`, `:care-plans`, which are
clinical content with no encounter scope at all. Tagged:

| field | `engine.clj` | tag |
|---|---|---|
| `:patient-id`, `:mrns`, `:active-mrn` | 290-292 | encounter-agnostic (patient identity; MRN is unchanged by this arc) |
| `:status` | 298 | **single-encounter-assumed** -- `:new`/`:admitted`/`:discharged` are encounter phases, `:merged`/`:expired` are patient terminals, all five in ONE enum |
| `:class` | 299 | **single-encounter-assumed** -- an encounter's patient class |
| `:home-ward`, `:location`, `:attending` | 301-306 | **single-encounter-assumed** -- the open encounter's placement |
| `:admitted-at`, `:discharged-at` | 315-316 | **single-encounter-assumed** -- one period, one pair |
| `:persona` | 307 | encounter-agnostic (t0 sample) |
| `:demographics` | 314 | encounter-agnostic (state-at-t, ADR-0173) |
| `:conditions`/`:observations`/`:medication-orders`/`:care-plans` | 317-320 | encounter-agnostic today, and that is itself the gap: nothing records WHICH encounter a condition was recorded during |

**`evolve` (23 methods, `engine.clj:1690`-1927).** Five are
single-encounter-assumed: `:admission` (1748, sets the whole seven-field
block plus `:admitted-at`), `:discharge` (1763, `:status :discharged`,
`:location nil`, `:discharged-at`), `:outpatient-visit` (1855, sets
`:status :admitted` `:class :outpatient` -- an ambulatory visit reuses
the SAME status vocabulary), `:outpatient-visit-end` (1861), and
`:cancel-discharge` (1788, reinstates the block). `:transfer` (1759)
and `:bed-swap` (1807) mutate the open encounter's placement.
The remaining sixteen are encounter-agnostic.

**`check.clj`: 35 invariants** (`catalog` 31 + `facility-catalog` 2 +
`warmup-catalog` 1 + `order-profiles-catalog` 1, counted by loading the
namespace). Eight are keyed on patient state where they mean encounter
state:

| invariant | `check.clj` | why single-encounter-assumed |
|---|---|---|
| `admission-only-when-new` | 120 | **THE horizon.** `(not= :new (:status before))` |
| `outpatient-visit-only-when-new` | 303 | the same rule, second copy |
| `discharge-follows-admission` | 63 | *"and not twice"* -- counts discharges against the FIRST admission's index, patient-wide |
| `transfer-only-when-admitted` | 128 | `:admitted` is an encounter phase |
| `order-only-when-admitted` | 567 | ditto |
| `clinical-content-only-when-admitted` | 642 | ditto, five kinds |
| `registered-is-every-patients-first-event` | 977 | correct as written, and it PINS the shape: a second encounter may not mint a second `:registered` |
| `expired-patient-retains-location` | 337 | `:expired` is a patient terminal that suppresses further encounters |

Encounter-agnostic and untouched by any encounter design: the four
structural ones (`timestamps-monotone` 54, `every-event-has-participants`
76, `participant-ids-exist-in-run` 85, `warm-up-mark-matches-window`
109), the six referential ones (589, 682, 740, 807, 836, 952), the
churn family (423, 466, 478, 498), the person-fold family (865, 898,
934), and the content ones (606, 621, 547, 990).

**The emitter. PV1-19 (visit number) is EMPTY, on every message this
project has ever produced.** `pv1-segment` (`emit_hl7.clj:482`) builds
fields 1-7 explicitly, then `(blank-fields 28)` -- fields 8 through 35
-- then PV1-36. Verified on the wire, not only in the source: the PV1
of `msg-001.hl7` from the ed-tuesday run above renders as
`PV1|1|I|Emergency^^ED-H09^general-hospital||||5761303028^Reyes^Priya|`
followed by 28 empty fields. **There is therefore no encounter
identifier anywhere on this project's v2 output.**

**The FHIR emitter has an encounter, and it is hardcoded to one.**
`encounter-resource` (`emit_fhir.clj:136`) mints
`:id (str patient-id "-encounter")` -- literally one Encounter per
patient -- with `:period` read from the single `:admitted-at`/
`:discharged-at` pair, and `condition-resources` (`emit_fhir.clj:148`)
references `Encounter/<patient-id>-encounter` for every condition.

**The compile layer already HAS encounters, and flattens them.**
`emittable_events.clj:101` maps a GMF `Encounter` state to an
`:admission` step (`:emergency`/`:inpatient`) or an `:outpatient-visit`
one (`:wellness`/`:ambulatory`/`:virtual`), and `EncounterEnd` to
`:discharge`/`:outpatient-visit-end`. A module that opens two
Encounters compiles two openers; the engine's `:new` guard is what
prevents the second from becoming an event. So the horizon is not a
missing CONCEPT upstream -- it is a lost one downstream.

#### (ii) Every bed-state read and write, and the ladder's rungs

**There is no bed state. There is a projection of patient state.**
`occupancy-board` (`facility.clj:44`) folds `patients` into
`bed-id -> patient-id`, keeping only patients with a `:bed`; its own
docstring calls this *"the consistency law stated as code: recomputing
from `patients` from scratch always equals this."* `free`
(`facility.clj:56`) is `(remove board ids)` -- **a bed is available iff
no patient is in it.** There is no third state and nowhere to put one.

Writers (the five that move a patient into or out of a bed):
`evolve :admission` (1748), `:transfer` (1759), `:discharge` (1763,
`:location nil` unless `:expired`), `:bed-swap` (1807),
`:cancel-transfer`/`:cancel-discharge` (1784/1788, reinstating).

Readers (every consumer of the board, all six):

| site | file:line | what it reads |
|---|---|---|
| `decide :admission` | `engine.clj:1049-1050` | board → `allocate` |
| `decide :transfer` | `engine.clj:1087-1089` | board → `allocate` |
| `decide :transfer-in-error` | `engine.clj:1269-1271` | board → `allocate` |
| `bed-ready-location` | `engine.clj:1137-1141` | board (minus the discharging patient) → `allocate`, plus its own `home-licensed-free?` |
| `decide :bed-swap`'s target check | `engine.clj:1984` | board → the live occupant |
| `exhausted-outcome` | `engine.clj:531-534` | board → `ward-census` diagnostics |

**The ladder** (`allocate`, `facility.clj:90`, four rungs per
`components/sim/docs/operational-models.md`): rung 1 free LICENSED bed
in the home ward; rung 2 free SURGE slot in the home ward; rung 3 free
licensed bed in another inpatient ward; rung 4 free surge slot in an ED
ward; else `{:exhausted true}`. `force-placement` overrides outright and
draws no RNG. Each rung's `choose` is one `:world` draw.

**Today "vacated" IS "ready", and that is measured, not inferred.**
`decide :discharge` (`engine.clj:1144`) emits the discharge AND, in the
same return, a `:bed-ready true` `:transfer` for the longest-waiting
boarder **at the same `t`**. In the ed-tuesday corpus both of that
run's two `:bed-ready` transfers carry the same `:t` as the discharge
of the same bed -- 2 of 2. Widening past the coupling: across the 22
beds that corpus touches there are **102 vacate→occupy transitions, 7
of them at a gap of ZERO seconds** (median gap 17,040 s ≈ 4.7 h). Seven
re-occupancies that no housekeeping cycle permits.

**How thin the exercise is.** ed-tuesday: 13 `:transfer`, 2
`:bed-ready`. clinic-decade: **0 transfers, 0 bed-ready** -- its
pathway is empty and every one of its 90 admissions is a hook, so it
never boards anybody. The oracle agrees and says so:
`digest.clj:703-706` -- *"THE CAPACITY WITNESS IS ONE ROOT DEEP.
`death-fixture` alone carries the oracle's single `:transfer` -- and
with it the only ADT^A02, the only `:bed-ready true`, and all 13 of its
ladder rung-3 placements. Rung 4, `:forced` and `:exhausted` are zero
across all 36."* A bed-cycle design that plans to be witnessed through
the bed-ready coupling is planning around 2 events in one corpus and a
single oracle root.

**The bed board exists and is message-fed.** `corpus/board.clj` (landed
2026-08-07, ADR-0067, *"Player board: the whiteboard exists"*) folds a
paced HL7 v2 stream through `v2-replay/fold-message` and renders
occupied beds grouped by ward plus a tally. It renders **only occupied
beds** (`occupied?`, `board.clj:37`: a `:location` present and not a
merge tombstone); a bed with no patient is simply absent from the
board, whatever the reason. `bases/cli`'s `--board` is **message input
only** -- `help.clj:256`: *":play-board-unsupported-for-events on an
event log"*.

> **DISCLOSED, for the author, not fixed here.**
> `.agents/plans/roadmap.md:143` says the ADR-0014 corpus-player slices
> *"(bed-board sink, `:mllp`, accumulator wiring) have never had a row
> in any register. UNPRICED and unscheduled."* The bed-board sink
> LANDED under ADR-0067 and ships as `--board`. The row's wording is
> the review-5 pattern -- a claim true when written that nothing keeps
> true. This session's fences allow two roadmap edits and this is not
> one of them, so it is reported rather than rewritten.
> **FIXED 2026-08-26** by the execution session that opened arc 3b's
> first sweep, in its own step-0 commit: the row now says the sink
> landed and prices what is actually left.

#### (iii) Every arrival source now, and which could be scheduled

Six sources reach `prelude` (`engine.clj:2270`). Counts are the two
corpora at `b9d4d77`.

| # | source | where | ed-tuesday | clinic-decade | schedulable? |
|---|---|---|---|---|---|
| 1 | **walk-in ordinal** -- `:arrival-gap` staggered, one per configured patient | `engine.clj:2330` | 100 configured, **78 minted a patient** | 200 configured, **161 minted** | **YES** -- the primary candidate |
| 2 | **repeat arrival** -- an ordinal binding to a person who already has a patient | `engine.clj:2371`, `2437` | **22 queue NOTHING** | **39 queue NOTHING** | **YES, and this is the horizon's whole cost** |
| 3 | **delivery, newborn mint** -- a new patient whose first encounter is the birth | `engine.clj:2567` | 15 | 29 | NO -- a live birth is not scheduled in this model |
| 4 | **delivery, parent encounter** on an existing clinically-idle patient | `engine.clj:2576` | **1** | 17 | partially (an elective delivery is scheduled; out of scope here) |
| 5 | **occupational injury** -- mint (no prior patient) or encounter on an idle one | `engine.clj:2600` | 8 mints, 0 on-existing | 12 mints, 4 on-existing | NO -- an injury is by definition unscheduled |
| 6 | **identity-unavailable window** -- an unidentified ED presentation | `engine.clj:2589` | 15 mints | 28 mints | NO -- ruled (a) 2026-08-26 (ADR-0173 §2(d)) |

Reconciling: ed-tuesday 116 patients = 78 + 38 mints (15+15+8); 110
admissions = 38 mint encounters + 72 on arrival patients (71 pathway +
1 delivery hook). clinic-decade 230 = 161 + 69 mints (29+28+12); 118
openers = 69 mint admissions + 21 hook encounters on existing patients
(17 delivery + 4 injury) + 28 module `:outpatient-visit`s. Every
column closes.

**Rows 2, 4 and 5's on-existing arm all hit the same wall.**
`clinically-idle?` (`engine.clj:2521`) is a static pre-filter --
a hook may place an encounter only on a patient whose compiled queue is
otherwise empty -- and `decide :person-encounter`'s `:new` guard is its
runtime half. That is why ed-tuesday witnesses **one** parent-delivery
encounter against clinic-decade's 17: ed-tuesday's arrival patients all
walk an admission-bearing ED pathway, so none of them is idle.

> **These are the DEMO seeds, not the gate seeds.** `roadmap.md`'s
> `[multi-encounter-horizon]` row cites 0 and 23 for the same pair; that
> is `ed-202` and `cd-424242`, the gated-corpus seeds tabled in
> `.agents/session-records/2026-08-26-arc-3a-fold-part-4.md:168`. This
> ADR measures the demo seeds each scenario's own README prints (20260811
> and 20260807). Both readings say the same thing -- the ED corpus is
> starved of idle patients and the ambulatory one is not -- and neither
> supersedes the other.

The
horizon is not costing this project a hypothetical; it is costing it
**22% and 19.5% of its configured arrivals** plus every hook that lands
on a busy patient.

### 2. The design

#### (a) The encounter

**An `:encounter-id`, minted at each encounter opener, carried on every
event of that encounter.**

1. **Minting.** `decide :admission` and `decide :outpatient-visit` mint
   one. Derivation is ruling B; the recommendation is a pure function
   of the run seed, the patient's arrival ordinal and the patient's
   0-indexed encounter ordinal, OFF the RNG stream --
   `patient-id-for`'s own contract (`engine.clj:361`) applied one level
   down, so identity generation adds no draws for sim/ADR-0009's
   accounting to track.
2. **`PatientState` gains two fields.** `:encounter` -- the OPEN one, a
   record holding `{:encounter-id :ordinal :class :home-ward :location
   :attending :admitted-at :appointment-id}` -- and `:encounters`, the
   vector of CLOSED ones, accumulating exactly the way `:conditions`
   and `:care-plans` already do (`engine.clj:317-320`). The seven
   single-encounter-assumed fields of census (i) stay on `PatientState`
   as **the open encounter's projection**, unchanged in shape and
   unchanged in value while an encounter is open, so every reader in
   the emitter, the checks and the board is untouched by the field
   move. What changes is that `evolve :discharge` now has somewhere to
   put them instead of throwing them away.

   **One reader does break, and it is the right one.** `:admitted-at`/
   `:discharged-at` now describe the LATEST encounter rather than the
   only one, so `encounter-resource` (`emit_fhir.clj:136`), which reads
   that single pair, would render visit 2's period under visit 1's id.
   That is precisely the defect the next paragraph fixes; it is named
   here so the field move is not read as invisible.
3. **The guard becomes structural.** `admission-only-when-new` becomes
   `admission-only-when-no-open-encounter`: legal iff `(nil?
   (:encounter before))` **and** `(not (#{:merged :expired} (:status
   before)))`. Note what this buys and what it does not -- `:merged` and
   `:expired` stay absorbing, which is `no-events-after-merged-terminal`
   (`check.clj:498`) and `expired-patient-retains-location`
   (`check.clj:337`) preserved verbatim. `outpatient-visit-only-when-new`
   (`check.clj:303`) becomes the same predicate; the two rules were
   always one rule in two copies.
4. **`evolve :discharge` closes the encounter**: conj the open record
   (stamped with `:discharged-at`) onto `:encounters`, `dissoc`
   `:encounter`, and set `:status :discharged` as today. The
   `:expired` arm is untouched -- an expired patient's encounter stays
   OPEN, because their body stays in the bed, which is exactly what
   `expired-patient-retains-location` asserts. `:outpatient-visit-end`
   closes the same way.
5. **PV1-19 renders it.** One of `pv1-segment`'s 28 blanks becomes the
   visit number. Whether that reaches v1 is ruling C's second half; the
   field is named here because it is the only wire face the encounter
   has, and because *"produces traffic invisible to every consumer"*
   (`emit_hl7.clj:51`) is this repo's own test for a fact that has not
   really landed.
6. **MRN is unchanged.** `:mrns`/`:active-mrn` stay patient-scoped and
   the merge family is untouched. A second encounter reuses the
   patient's active MRN, which is the whole point: an MPI under test
   must see the same MRN twice.

**Which invariants split.**

| today | becomes |
|---|---|
| `admission-only-when-new` (120) | `admission-only-when-no-open-encounter`, per-encounter |
| `outpatient-visit-only-when-new` (303) | folded into the same predicate |
| `discharge-follows-admission` (63) | **splits in two.** Per-patient: no `:discharge` precedes the patient's first `:admission` (unchanged). Per-encounter (new): every `:discharge` names an `:encounter-id` opened by an `:admission` earlier in the same patient's log and not already closed -- which is where *"and not twice"* moves |
| `transfer-only-when-admitted` (128) | per-encounter: the transfer's `:encounter-id` is the open one |
| `order-only-when-admitted` (567), `clinical-content-only-when-admitted` (642) | per-encounter, and each gains an `:encounter-id` stamp so a condition recorded during visit 2 is not silently attributed to visit 1 |
| `registered-is-every-patients-first-event` (977) | **UNCHANGED, and load-bearing.** A second encounter mints no second `:registered`. This is the invariant that makes the design an encounter design rather than a duplicate-patient one |
| `expired-patient-retains-location` (337) | unchanged; `:expired` suppresses further encounters through the new guard's second clause |

New: `every-encounter-is-opened-and-closed-or-still-open` (an
`:encounter-id` appears on exactly one opener and at most one closer,
and every event carrying it lies between them in that patient's own
log) -- the same referential shape as
`medication-end-references-existing-order-and-follows-it-in-time`
(`check.clj:682`).

**FHIR follows for free.** `encounter-resource`'s id becomes
`(str patient-id "-encounter-" ordinal)` and it renders one resource
per `:encounters` entry plus the open one; `condition-resources`
references the encounter its condition was recorded during. This is a
real defect fix, not a widening: today two visits' conditions both
point at `<pid>-encounter`.

**Rejected.**

* *A new patient-id per encounter, linked by MRN.* This re-creates the
  duplication the MPI surface exists to TEST, and
  `registered-is-every-patients-first-event` would force a second
  `:registered` per visit -- which on the wire is an A28/A31, not an
  A01. It also throws away ADR-0173's whole result: a returning person
  resolves to the patient they already are.
* *No id; an encounter is the log-index span between an opener and its
  closer.* Free, and wrong twice. PV1-19 needs a VALUE, and every
  derived consumer (the FHIR Encounter, the board, any future SIU)
  would re-derive the span independently. ADR-0169's fold-carried index
  family (`:reinstate-index`, `:citation-index`, `:person-index`,
  `:registration-index`) exists precisely so a referential key is
  carried rather than scanned.
* *The log index of the opening event as the id.* Unique, free, and
  brittle: a visit number is a thing consumers persist, and any
  reshuffle would renumber every one of them. `:placeholder-event-id`
  and `:order-event-id` are indexes because they are REFERENCES; a
  visit number is an IDENTIFIER.
* *Leave the horizon at one encounter; 3b ships appointments only.*
  This is ruling A's alternative and is argued there.

#### (b) Scheduling

**Four skeleton kinds**, none of which renders a message in v1
(ruling C):

| kind | payload | folds to |
|---|---|---|
| `:appointment` | `:appointment-id`, `:scheduled-t`, `:appointment-class` (matching the encounter classes), `:reason`, optional `:citation` | opens an appointment record on `PatientState` |
| `:reschedule` | `:appointment-id` (the SAME one), `:prior-scheduled-t`, `:scheduled-t` | moves it |
| `:appointment-cancel` | `:appointment-id` | closes it, terminal |
| `:no-show` | `:appointment-id` | closes it, terminal, emitted AT `:scheduled-t` |

A reschedule keeps the id rather than minting a new one and referencing
the old: SCH-1/SCH-2 are stable placer/filler ids across the SIU family
(census (d)), and `:prior-value`/`:value` on one record is already this
repo's shape for a change (`demographic-update-reports-a-real-change`,
`check.clj:898`).

**A scheduled arrival references its appointment.** The encounter
opener carries `:appointment-id`, and `PatientState`'s open-encounter
record carries it too. That single field is what makes traffic-model's
second invariant non-vacuous -- **and it is non-vacuous only because
(a) exists**, since without a second encounter every appointment would
precede a patient's first and only visit.

**The split is WORLD config, and it draws.** A new run-config key
`:scheduling {:scheduled-fraction :lead-time-days :no-show-rate
:reschedule-rate :cancel-rate :follow-up}`, joining `run`'s
`config-keys` list (`engine.clj:2158`) with the forwarding that list's
own completeness test demands. `R-mix-7` says mix RATIOS reshuffle
nothing; this is not a mix ratio, it is a fact generator, so it draws.
**Which family, split two ways:**

* **`:world`** for the scheduled-vs-walk-in Bernoulli and the lead
  time. Arrivals are `:world` by the partition's own definition
  (`engine.clj:412`: *"arrivals, and every cross-patient decision"*),
  these draws POSITION an arrival, and their count is conditional on
  the population -- the exact reason `:world` is run-scoped. One
  Bernoulli plus one lead-time draw per arrival ordinal, taken in
  ordinal order in the pre-loop block, after the person-selection
  uniform ADR-0173 added.
* **`:patient`** for an appointment's OUTCOME -- kept, rescheduled,
  cancelled, no-showed. This is that patient's own trajectory, it reads
  no other patient's state, and under a per-patient stream a rate
  change *"cannot reach any other patient"* (`engine.clj`'s `:delay`
  comment, ADR-0171 §2(d)). It is also why the number of appointments a
  patient has may be data-dependent without breaching the
  fixed-consumption law, which exists so draw count never depends on
  ANOTHER patient's data.

This reshuffles. Arcs 1-3 are a declared-reshuffle era (ADR-0168's own
Consequences), so it is licensed rather than discovered -- but under
ruling E's dark-then-on landing the reshuffle belongs entirely to the
TURN-ON commit: the dark commit adds the config key and the machinery
and must prove the corpus byte-identical, exactly as ADR-0173 ruling
D1's two commits did.

**Return visits scheduled at discharge -- the first producer of second
encounters.** At `decide :discharge`, a follow-up Bernoulli plus an
interval draw on the discharging patient's own `:patient` stream. When
it fires: an `:appointment` event at the discharge instant, and a
queue-seeded scheduled `:outpatient-visit` at `:scheduled-t` -- into
the `sorted-map` keyed `[t seq-no]` at an absolute instant, the same
seam `schedule-followup` (`engine.clj:1415`, enqueued at
`engine.clj:3333`) and ADR-0173 §2(b)'s
queue-seeding pass already use. **Nothing about the main loop changes**,
which is the third arc in a row that has been true.

**Invariants** (traffic-model :45-47, plus one the tree owes):

1. `appointment-reference-resolves` -- every `:reschedule`,
   `:appointment-cancel` and `:no-show` names an `:appointment-id` that
   an `:appointment` earlier in the SAME patient's log minted.
2. `scheduled-encounter-follows-its-appointment` -- an opener carrying
   an `:appointment-id` has that appointment earlier in its own
   patient's log, at or before its `:t`, and not already terminal.
3. `no-show-has-no-encounter` -- no opener carries a no-showed
   appointment's id.
4. `appointment-reaches-at-most-one-terminal` -- kept, cancelled and
   no-showed are mutually exclusive. Not in traffic-model; owed,
   because 1-3 are each satisfiable by a log where an appointment is
   both cancelled and kept.

**Rejected.**

* *Appointments as `PatientState` fields only, no events.*
  `R-skeleton-or-emission` decides it: downstream invariants must
  respect them, so they are generated and judged, i.e. events.
* *A facility-level resource calendar -- slots, provider availability,
  contention.* This opens capacity modelling on a second axis and
  `R-mix-5` asks for STATE, not a scheduler. A real candidate for a
  later arc; named, not taken.
* *Derive the appointment from the encounter -- emit one retroactively
  before every scheduled arrival.* Makes the no-show unrepresentable,
  since a no-show is precisely an appointment with no encounter to
  derive from. This is `R-skeleton-or-emission`'s test failing in the
  most direct way available.

#### (c) The bed-status cycle

**World-level per-bed state.** `world` gains `:beds`, a map
`bed-id -> {:status :ready|:occupied|:dirty|:cleaning, :since-t,
:last-patient-id}`, initialised `:ready` for every licensed bed and
surge slot the facility declares. This is the one place arc 3b adds
state the log cannot re-derive, and it is deliberate:
`occupancy-board`'s consistency law (*"recomputing from `patients` from
scratch always equals this"*) still holds **for the occupied half** --
`:occupied` iff a patient's `:location` names the bed -- while
`:dirty`/`:cleaning`/`:ready` have no patient to be derived from at
all. That asymmetry is what `R-mix-6`'s *"world-level"* means, and it
should be stated in the code rather than left for a reader to notice.

**`allocate` is gated on `:ready`.** `free` (`facility.clj:56`) changes
from *"not a key in `board`"* to *"status is `:ready`"*, taking the bed
map as a second argument. Every rung inherits the gate at once, so the
ladder's shape and its per-rung `choose` draw are untouched. All six
readers of census (ii) go through it; `bed-ready-location`'s own
`home-licensed-free?` (`engine.clj:1139`) uses the same predicate,
which matters because that function's docstring proves rung-1
availability by hand and the proof must be re-read under the new
predicate.

**`bed-ready-location` becomes the READY event's consumer, not the
discharge's.** This is the one existing behaviour arc 3b changes rather
than extends, and it is `R-mix-6` in one sentence:

```
today:   discharge@t  ──────────────────────────────►  bed-ready transfer@t
3b:      discharge@t → dirty@t → cleaning@t+d1 → ready@t+d1+d2 → bed-ready transfer@t+d1+d2
```

`decide :discharge` stops emitting the paired transfer. It sets the bed
`:dirty` and seeds the cycle; the transfer is decided at the READY
instant, against the board as it stands THEN -- which is more correct
than today independently of the cycle, because today's coupling picks
the longest-waiting boarder at the discharge instant and hands them a
bed they occupy immediately, with no opportunity for the world to have
changed. The seven zero-second re-occupancies measured in census (ii)
are what this removes.

**Durations from FACILITY** (ruling D): each `Ward` (`config.clj:19`)
gains `:turnaround-minutes` for the dirty→cleaning delay and the
cleaning→ready delay, drawn on the **`:facility`** stream -- the family
for *"draws that read no patient state at all"* (`engine.clj:420`),
kept distinct from `:world` by ADR-0171 ruling E1 exactly so that a
ward-config edit does not shift arrival gaps or bed choices.

**One kind, not three.** `:bed-status-change`, carrying `:bed`,
`:ward`, `:from`, `:to` and (on the `:dirty` transition only)
`:last-patient-id` -- the same *"one kind, many causes"* choice
ADR-0173 made for `:demographic-update` rather than minting a kind per
field. Three kinds would each need an `evolve`, a schema branch, an
oracle mover-set prediction and a place in the closed vocabulary.

**And here is what that costs, named rather than discovered: a
bed-status event has NO PATIENT.** `every-event-has-participants`
(`check.clj:76`) requires a non-empty `:participants`, and
`participant-ids-exist-in-run` (`check.clj:85`) destructures
`{:keys [patient-id]}` and reports a violation when the id is not in
the `:registered` set -- so a participant map with no `:patient-id`
yields `nil`, and `(contains? admitted-ids nil)` is false, i.e. **a
patient-less event goes RED today**, correctly. ADR-0173 met this exact
wall and went the other way: person events *"could not satisfy
`every-event-has-participants` without inventing a second participant
vocabulary"*, so they never became log events.

**Arc 3b must not take that exit, and the reason is the invariants, not
the consumer.** Every function in `check.clj` takes `[ground-truth]` --
the LOG. A bed cycle that lives only in `world` is a cycle nothing can
judge, and `R-skeleton-or-emission` classifies it skeleton precisely
because downstream invariants must respect it: skeleton means
*generated AND judged*. `R-mix-6`'s bed-board clause points the same
way but is weaker, since the board reads MESSAGES and section (d)
defers those. So the vocabulary widens, minimally and in one place:

* `:participants` gains a `{:bed-id .. :ward .. :role :subject}` shape.
* `participant-ids-exist-in-run` is scoped to participants that CARRY a
  `:patient-id` -- one `filter`, and the invariant keeps asserting
  exactly what it asserts today about every patient participant.
* `every-event-has-participants` is unchanged: a bed event has one.

**What this does NOT buy in 3b is the bed board.** The events reach
ground truth; no message carries them (section (d)), and `--board`
consumes messages only -- so a dirty bed stays invisible on the
whiteboard until a later slice. That gap is real and is rowed rather
than papered over; ruling C is where it can be closed early instead.

**Invariants.**

1. `no-assignment-to-a-non-ready-bed` -- every event that ALLOCATES a
   bed (`:admission`, `:transfer` including the bed-ready one, and
   `:transfer-in-error`, i.e. exactly the four `sim-model/allocate`
   call sites) targets a bed whose world status immediately before is
   `:ready`.

   **`:bed-swap` is excluded, and must be.** `decide :bed-swap`
   (`engine.clj:1294`) picks a peer who is already `:admitted` with a
   `:location`, then exchanges the two locations; it does not call
   `allocate` at all. **Both target beds are OCCUPIED by construction**, so an
   unqualified "assignment" reading of this invariant would go red on
   every swap in every corpus. A swap moves two occupants between two
   occupied beds and allocates nothing.

   **The reinstatements are the sharp case.** `:cancel-transfer` and
   `:cancel-discharge` put a patient back in a bed that has been
   `:dirty` since they left it, so a cancel must restore the bed's
   status alongside the patient's location, or every reinstating cancel
   goes red. That pairing belongs with `:reinstate-index`
   (`engine.clj`'s `reinstatable-event-types`, whose set is exactly
   `#{:transfer :discharge}` -- the two event classes that vacate),
   which is where the prior-state carry already lives.
2. `every-ready-follows-a-cleaning` -- a bed reaching `:ready` was
   `:cleaning` immediately before, except at run start, where every bed
   is born `:ready`.
3. `bed-cycle-transitions-are-legal` -- the transition relation is
   exactly ready→occupied, occupied→dirty, dirty→cleaning,
   cleaning→ready, plus the cancel reinstatement's dirty→occupied.
   Enumerated so a new writer cannot invent a fifth.
4. `occupancy-within-capacity` (`check.clj:352`) is **unchanged** --
   it counts `:location`-bearing patients against declared capacity and
   a dirty bed holds nobody. **But EFFECTIVE capacity falls**, and that
   is a real risk this design will not hide: ed-tuesday's own config
   header says its pacing was *"tuned against"* holding *"without
   capacity exhaustion"*. A turnaround delay can push it over. When it
   does, `allocate` returns `{:exhausted true}` and the engine emits a
   `:step-rejected` with a documented reason (`check.clj:547`) -- so
   the failure is VISIBLE, which is why this is a tuning question and
   not a correctness one. The landing commit owes a re-probe of both
   corpora at whatever turnaround the config ships.
5. `surge-only-when-earlier-rungs-exhausted` (`check.clj:400`)
   **changes meaning and must be re-read.** "Rung 1 was exhausted" now
   means "no rung-1 bed was READY", not "no rung-1 bed was empty".
   ADR-0153's own seed-202 counterexample lives here.

**What the bed board consumes.** Today it renders occupied beds only
and a bed with no patient is invisible. Under the cycle it can render
three more states -- and it can render them **only if a message
carries them**, because `--board` is message-input-only
(`help.clj:256`). See (d).

**Rejected.**

* *A phantom housekeeping occupant holding the bed.* It would enter
  `occupancy-within-capacity`, `no-double-occupancy` and every census
  the board tallies, and it needs a patient-id -- the same fabrication
  ADR-0173 refused when it declined to give a John Doe a real DOB.
* *A per-bed unavailability WINDOW list -- bed X unavailable [t1,t2).*
  Cannot express "cleaning started, not finished" at a snapshot
  instant, which is exactly what the board renders, and makes
  `every-ready-follows-a-cleaning` unstateable.
* *Cleaning duration drawn per-discharge on the `:world` stream.*
  Simpler, and it makes turnaround a property of the DISCHARGE rather
  than of the ward, which is backwards; it also puts the draw on the
  stream arrivals live on. This is ruling D's alternative.


**Ratifications (2026-08-27).** Sweep 2 landed section (c) and left three
judgment calls unratified in its own record
(`.agents/session-records/2026-08-27-arc-3b-bed-cycle.md`, "Judgment calls,
and their ratification status"). All three are RATIFIED here, and this
section is amended to say what the tree does rather than what it was
drafted to do.

1. **A SIXTH transition arc, `:occupied -> :ready`, joins item 3's
   relation.** Item 3 enumerates five arcs *"so a new writer cannot invent
   a fifth"* and does not reach the two cancel classes that VACATE a bed.
   `:cancel-admit`, and `:cancel-transfer`'s own erroneously-taken bed,
   return straight to `:ready` with no event and no turnaround: an
   occupancy a cancel RETRACTS never happened, so it leaves no dirt behind
   it. Without the arc the bed stays `:occupied` for the rest of the run
   and its ward silently loses capacity, which no reading of this section
   intends. Carried in three places rather than added quietly --
   `bed-correction-event-types`, `legal-bed-transitions`, and
   `components/sim/docs/operational-models.md`. The relation is now SIX arcs, and the
   enumeration stands against a seventh on the same terms.

2. **`:turnaround-minutes` is ONE key drawn TWICE, `[lo hi]` per leg.**
   Ruling D1 gives each `Ward` a `:turnaround-minutes` for *"the
   dirty->cleaning delay and the cleaning->ready delay"* without saying
   whether that is one value or two. It ships as a `[lo hi]` range that
   EACH LEG draws from independently, so a ward's whole turnaround runs
   `[2*lo, 2*hi]`. Two keys would have made a config author state a
   decomposition of housekeeping that no real site reports separately.
   `{:optional true}` with a per-class fallback (`{:ed [5 15] :inpatient
   [15 30]}`), so a facility config written before sweep 2 keeps
   validating.

3. **Item 5's re-read was required, not optional.** Item 5 says
   `surge-only-when-earlier-rungs-exhausted` *"changes meaning and must be
   re-read"*, and the tree agreed mechanically: left unmodified the row
   fired twice on the first opted-in run, because a surge placement made
   while a rung-1 bed sits empty-but-`:dirty` is legitimate under the
   cycle and a violation without it. Its three `(remove board ...)` calls
   are now three `sim-model/free` calls -- the same predicate the ladder
   itself asks. The CLAIM is unchanged; only the reading of "exhausted"
   moved, and it moved to the ladder's own.

4. **A SEVENTH transition arc, `:cleaning -> :occupied`, joins item 3's
   relation (2026-08-29).** Ratification 1 above grew the relation from
   five arcs to six and closed with *"the enumeration stands against a
   seventh on the same terms"*. It is now owed, on exactly those terms.
   Item 3 carves the reinstatement out of `:dirty` ONLY, and
   `engine.clj`'s own comment reads the same way -- *"a
   `:cancel-discharge` can reinstate a patient into a bed whose cycle is
   already in flight (the dirty->occupied arc ADR-0174's invariant 3
   carves out)"*. **But the cycle has TWO in-flight legs**, and a
   reinstating cancel can land in the second (`:cleaning`) as easily as
   in the first (`:dirty`).

   **The engine is correct; this enumeration was incomplete.**
   `update-beds`' second rule reads the participant's location delta and
   writes `:occupied` without consulting the bed's prior status, and the
   pending `:bed-ready` tick then finds a bed that is no longer
   `:cleaning` and emits nothing -- its guard doing exactly what it is
   for. The bed ends up correctly occupied. Only the check-side relation
   disagreed, so this ratification is CHECK-SIDE ONLY: no corpus moves,
   no event changes, and `bin/ground-truth-bracket` reads IDENTICAL
   across it.

   **Found by VOLUME, not by reading**, which is the part worth keeping.
   The traffic-scale close (2026-08-29,
   `.agents/session-records/2026-08-29-traffic-scale-close.md` section 9,
   TS-1) ran the arc-4 add-on configuration at 750 and 7,500 patients and
   `bed-cycle-transitions-are-legal` refused **2 of 16,322 events** and
   **16 of 171,925** respectively. **Zero in every corpus this repository
   ships**: the window is one `:turnaround-minutes` draw wide and the
   gated corpora are thin on churn. Both witnesses at 750 are
   `:cancel-transfer`, and one of them reinstates its patient into a bed
   that a DIFFERENT patient has occupied and vacated in the meantime --
   so the arc is not merely "the same occupant returns", and the
   enumeration must not be re-narrowed to that.

   Carried in the same three places the sixth arc is --
   `legal-bed-transitions`, `engine.clj`'s own comments, and
   `components/sim/docs/operational-models.md` -- and gated by an
   AUTHORED witness in `ehrt.sim-engine.bed-cycle-test`, whose fixture is
   counted (`pos?` on the fold's own `[:cleaning :occupied]` arcs) rather
   than assumed, because a fold that recorded no transition would make
   the row pass for the wrong reason. **The relation is now SEVEN arcs,
   and the enumeration stands against an eighth on the same terms.**

**And item 4's premise is WITHDRAWN: `:exhausted` is not visible, it is
FATAL.** Item 4 argues the effective-capacity risk is acceptable because
*"`allocate` returns `{:exhausted true}` and the engine emits a
`:step-rejected` with a documented reason -- so the failure is VISIBLE."*
It does not. `decide` translates exhaustion into an `:exhausted` outcome
(`exhausted-outcome`, `engine.clj:728`) that the run loop HALTS on --
`engine.clj:3959` returns `final-result` instead of recurring, and the
loop's own comment beside it says a `:rejected` outcome *"is NOT a
run-halting condition, unlike `:exhausted`"*. `run-command` then surfaces
`result/error :capacity-exhausted` (`run.clj:573`) and SKIPS `check-all`
entirely, so an exhausted run yields no corpus and no self-check, not a
flagged one. `:step-rejected` belongs to a different family (an illegal
cancel/bed-swap/merge). Sweep 2 hit this for real: three candidate
configurations for its new oracle root exhausted the ladder outright and
were rejected before one was found that contends without dying.

The consequence for any later slice that ADDS ARRIVALS -- section (b)'s
scheduling first among them -- is that capacity headroom must be MEASURED
before an opt-in, not discovered by a red gate. Whether exhaustion should
instead degrade to a visible rejection is a real question and a separate
one; it is rowed, not answered here.

#### (d) The contract: what reaches the wire

**Verified from this tree's own resolved dependencies, not from
memory.** `components/judge-v2-hapi/deps.edn:9` pulls
`ca.uhn.hapi/hapi-structures-v24` 2.6.0. Reading that jar:

* `ca/uhn/hl7v2/parser/eventmap/2.4.properties` maps **`SIU_S13`,
  `SIU_S14`, `SIU_S15`, `SIU_S16`, `SIU_S17`, `SIU_S18`, `SIU_S19`,
  `SIU_S20`, `SIU_S21`, `SIU_S22`, `SIU_S23`, `SIU_S24`, `SIU_S26`**
  all to the structure `SIU_S12`. So S12/S13/S15/S26 are real v2.4
  trigger events sharing one structure.
* `SIU_S12`'s segment names, by instantiation, are **`[MSH SCH NTE
  PATIENT RESOURCES]`**. Its `SCH` segment has **27 fields**, of which
  **SCH-1 and SCH-2 are `EI`** -- the placer and filler appointment
  ids. That is the wire home for `:appointment-id`, and the reason (b)
  keeps one id across a reschedule.
* **`ADT_A20`'s segments are `[MSH EVN NPU]`.** `NPU` has **exactly two
  fields: NPU-1 `PL` (the same location datatype PV1-3 renders) and
  NPU-2 `IS` (bed status).** **There is no PID and no PV1.**

That last line is the whole of (d)'s difficulty. Every message this
project emits goes through `single-subject-message`'s PID/PV1 pair;
ADT^A20 is the first message type in scope that has no patient at all,
which is the wire-side mirror of the participant problem in (c).

**And a version question this tree cannot answer.** The emitter
declares MSH-12 **`"2.3"`** (`site_profile.clj:58`, a site-profile
default, verified on the wire in the ed-tuesday messages above), while
the only structure library in the tree is v2.4. Whether SIU^S26 is a
legal trigger in 2.3 is not checkable from this clone, and this ADR
will not assert it from memory. A new message family therefore owes
either a version decision or an in-tree source for 2.3's trigger table.

**Recommendation, therefore: NOTHING new reaches the wire in arc 3b.**

| family | v1 disposition |
|---|---|
| `:appointment`, `:reschedule`, `:appointment-cancel`, `:no-show` | **skeleton only.** This is `R-mix-5`'s own text -- *"skeleton events carrying invariants, not rendered chatter"* -- taken literally |
| `:bed-status-change` | **skeleton only.** The board's visibility of dirty beds becomes a rowed follow-on, not a silent gap |
| `:encounter-id` on existing kinds | ground truth in 3b; **PV1-19 is ruling C's second half** |

What a message family would cost, from `message-type-registry`'s own
comment (`emit_hl7.clj:106-111`): a message-type registration, a
control-id derivation, a derivability-property row and a
`witnessed-message-types` claim -- and, from `digest.clj:733`, a
prediction of which of the oracle's 36 roots move, made BEFORE editing.
ADR-0173 declined the A08 for `:demographic-update` on exactly these
grounds and named it *"a candidate for a later arc"*; arc 3b declines
SIU and A20 the same way.

**Version bump, by `classify-change` (`event_schema.clj:896`), not by
preference.** New event kinds are ADDITIVE. New `{:optional true}` keys
on existing kinds are ADDITIVE. A new REQUIRED key on an existing kind
is BREAKING. So:

* `:encounter-id` lands `{:optional true}` on every encounter-scoped
  kind, and is enforced instead by the invariant of (a) -- optional in
  the schema so a 1.4.0-era log validates unchanged, mandatory in every
  run-produced log because a gate says so.
* On that shape `classify-change` reports `{:additive? true :breaking
  []}` and **no bump is mechanically owed.**
* Take **1.5.0 anyway**, for the reason 1.3.0 and 1.4.0 were both taken
  against the same verdict: `:event-schema-version` is a consumer's
  only handle on what a log they hold can contain, and a 1.5.0 log may
  carry five kinds a 1.4.0-era consumer has never seen. The bump is a
  disclosure, not a break, and the version note must carry the
  validates-unchanged argument the way 1.3.0's and 1.4.0's do.

### 3. Rulings needed

**(A) Is the encounter horizon lifted in arc 3b, or deferred to its own
arc?**

**RULED A1, 2026-08-26.** *Recommendation: **A1 -- lifted, in 3b, and
FIRST.*** Three grounds,
all measured. (i) `R-mix-5`'s second invariant is vacuous without it:
max encounter openers per patient is **1** at both gated corpora, so
*"a scheduled encounter follows its appointment"* would gate a set that
cannot contain a second visit -- the `project_two_live_vacuous_gates`
shape, landing a new one on purpose. (ii) The follow-up-at-discharge
hazard is the natural first producer of second encounters and has
nowhere to put one. (iii) The horizon already costs **22 of 100 and 39
of 200 configured arrivals**, plus every hook landing on a busy
patient (ed-tuesday: 1 parent-delivery encounter, against
clinic-decade's 17).

*Alternative **A2 -- deferred.*** 3b ships appointments that only ever
precede a patient's first and only encounter. Cheaper by one sweep and
one reshuffle; the cost is a knowingly-vacuous invariant and a
`[multi-encounter-horizon]` row left OWNER UNASSIGNED for a third
consecutive arc.

**(B) `:encounter-id` derivation.**

**RULED B1, 2026-08-26.** *Recommendation: **B1 -- a pure function of
seed × arrival ordinal × encounter ordinal, off the RNG stream***, mirroring `patient-id-for`
(`engine.clj:361`) exactly: `(format "ENC-%06d-%02d-%08x" ordinal
enc-ordinal (bit-and (mix64 (mix64 seed ordinal) enc-ordinal)
0xffffffff))`. `mix64` is this repo's own, and PUBLIC since ADR-0171 ruling A1
(`engine.clj:344`);
`stream-family-tag`'s docstring is explicit that a hash this repo does
not own must not be load-bearing, so the derivation takes the ORDINAL
the patient-id already encodes rather than hashing the patient-id
string. Hook-minted patients take ordinals `(+ patients k)` and are
covered unchanged. Zero new draws.

*Alternatives.* **B2** a run-scoped monotonic counter -- simplest, but
order-dependent, which is the property ADR-0171 rejected
`SplittableRandom`'s split order for. **B3** the opening event's log
index -- free and unique, but a visit number is an identifier consumers
persist, and every reshuffle would renumber all of them.

**(C) Which scheduling kinds reach the wire in v1 -- and does PV1-19
render?**

**RULED C1, 2026-08-26, PLUS ADT^A20.** The four scheduling kinds stay
skeleton-only and PV1-19 renders, as recommended; the author added
ADT^A20 for the bed-status cycle, which this recommendation had
declined. The A20 belongs to sweep 2 -- the bed cycle it renders -- not
to sweep 1, and it carries the two costs (d) names: the message
family's own four items, and the MSH-12 2.3-vs-v2.4 question, which
sweep 2 owes an answer to rather than an assertion from memory.

*Recommendation: **C1 -- none of the four, and PV1-19 DOES render.***
The scheduling kinds stay skeleton-only, which is `R-mix-5`'s own
wording. PV1-19 is the exception because it is a field on messages this
project ALREADY emits, not a new message family: it costs no
message-type registration, no control-id derivation, no
`witnessed-message-types` claim and no new oracle root -- it moves
every existing ADT message's PV1 by one populated field, which is a
predictable, single-line mover-set (all 33 engine-layer oracle roots
that emit any ADT). And it is the difference between an encounter
concept that a consumer can test against and one that is *"traffic
invisible to every consumer"*.

*Alternatives.* **C2** SIU^S12 for `:appointment` only -- buys a real
scheduling feed, costs the four items above plus the unresolved
MSH-12 2.3-vs-v2.4 question. **C3** nothing at all, PV1-19 included --
maximally conservative, and it leaves the encounter with no wire face
in a project whose own registry comment calls that a failure mode.

**(D) Bed-cycle durations: FACILITY or WORLD?**

**RULED D1, 2026-08-26.** *Recommendation: **D1 -- FACILITY, per-ward,
drawn on the `:facility` stream.*** `Ward` (`config.clj:19`) already carries per-ward `:beds`,
`:surge-slots` and `:surge-format`; turnaround is a property of that
ward's housekeeping in exactly the same way. `:facility` is already
defined as the family for draws *"that read no patient state at all"*
and was separated from `:world` by ADR-0171 ruling E1 **so that adding
a ward or a provider template does not shift arrival gaps or bed
choices** -- putting turnaround in `:world` would forfeit precisely
that.

*Alternative **D2 -- WORLD, one global distribution.*** One config key
instead of a per-ward one, and it makes an ED bay and an inpatient room
turn around at the same rate, which the ladder's own per-ward structure
already says they do not.

**(E) Landing order: one sweep or three?**

**RULED E1, 2026-08-26.** *Recommendation: **E1 -- three sweeps,
encounter FIRST and ALONE***,
each D1-style dark-then-on (land the machinery with `:persons`-style
opt-in absent, prove the corpus byte-identical, then turn it on in a
second commit). Order: **encounter → bed-status → scheduling.**
Grounds: scheduling references `:encounter-id` and its whole point is a
second encounter, so it cannot go first; bed-status is independent of
scheduling and changes an existing event's TIMING, which is easiest to
attribute when nothing else is moving; landing all three together makes
any corpus delta un-attributable across three reshuffles. Arc 3a's own
part 2 / part 3 / part 4 split is the precedent, and it worked -- each
part's defects were found because the surface under test was small.

*Alternative **E2 -- one sweep.*** One reshuffle instead of three, one
oracle mover-set prediction instead of three. The cost is that the
first red gate has three candidate causes.

### What this ADR does NOT design

Named so a later reader does not mistake silence for a decision:
an ADT^A20 or SIU message family (ruling C's alternatives); the bed
board's rendering of dirty and cleaning beds, which needs one of those
or a lift of `--board`'s message-only restriction and belongs with
`roadmap.md#corpus-player-slices`; A05/A14 pre-admit as a scheduled
encounter's precursor; a resource calendar with provider contention;
and the encounter scoping of `:conditions`/`:observations`/
`:medication-orders`/`:care-plans`, which section 2(a) makes POSSIBLE
by stamping `:encounter-id` on the events but does not itself
restructure.

### Consequences

* Arc 3b has three sweeps, five new event kinds
  (`:appointment`, `:reschedule`, `:appointment-cancel`, `:no-show`,
  `:bed-status-change`), one new carried field (`:encounter-id`), two
  new `PatientState` fields, one new world map (`:beds`), **ten
  invariants touched or added** -- one guard rewritten (absorbing a
  second copy of itself), one split in two, one new encounter-
  referential, four scheduling, three bed-cycle -- and a predicted
  blast radius rather than a discovered one.
* **The participant vocabulary widens for the first time.** A
  `:bed-status-change` has no patient, and
  `participant-ids-exist-in-run` (`check.clj:85`) must be scoped to
  participants carrying a `:patient-id` or it goes red on every bed
  event. This is the one place arc 3b breaks a shape ADR-0173
  deliberately preserved, and what forces it is that `check.clj` judges
  the LOG: a bed cycle held only in `world` is a cycle no invariant can
  see.
* **`surge-only-when-earlier-rungs-exhausted` changes meaning**, from
  "no earlier-rung bed was empty" to "no earlier-rung bed was READY",
  and `bed-ready-location`'s hand proof of rung-1 availability
  (`engine.clj:1129-1133`) must be re-read under the new predicate
  before that function is edited.
* **Effective capacity falls** the moment a turnaround delay is
  non-zero. Both gated corpora were tuned to hold without exhaustion;
  the turn-on commit owes a re-probe, and an exhaustion shows up as a
  `:step-rejected` with a documented reason rather than silently.
* **The oracle's capacity witness is one root deep** (`digest.clj:703`:
  `death-fixture` alone carries the only `:transfer`, the only ADT^A02,
  the only `:bed-ready true` and all 13 rung-3 placements). Arc 3b's
  bed-cycle work is therefore gated by a single root, and the mover-set
  prediction for any bed change is that root plus whatever new fixtures
  the arc adds. Predict before editing.
* **Three reshuffles, licensed not discovered** -- ADR-0168's
  declared-reshuffle era covers arcs 1-3. Each sweep takes exactly one.
* `rulings.md` is FROZEN (de-scaffold ruling, 2026-08-25). Nothing here
  becomes a rulings row; the invariants land as gates in arc 3b's own
  commits or not at all.
* **`[multi-encounter-horizon]` gets an owner if ruling A goes A1**, and
  the roadmap row says so pending that ruling. Ruling A went A1 on
  2026-08-26, so that row's owner is arc 3b sweep 1.
