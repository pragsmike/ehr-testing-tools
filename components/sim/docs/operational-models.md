# Operational resource models

This document specifies the design for the simulator's operational
resource models — the things a hospital allocates to patients that
aren't clinical content. It is the spec Milestone M1 of
[`.agents/plans/roadmap.md`](../../../.agents/plans/roadmap.md) implements
against, reviewed ahead of any code landing. It does not touch
`src/`; it describes what M1's code should do.

## The three-class resource taxonomy

Three models land in this document — facility (beds), providers, and
payers — but they are instances of only three *classes* of resource,
and every future resource this simulator ever models (equipment,
ORM/ORU order slots, transport, whatever comes after) will be one of
these three. Naming the classes up front, rather than letting each
model reinvent its own shape, is the point of writing this as one
document instead of three.

- **Exclusive resources** are occupancy-tracked, capacity-bounded, and
  invariant-bearing. A unit of the resource is held by at most one
  patient at a time; holding it is administratively meaningful (it
  shows up in messages, it can be transferred, it can run out). Beds
  are the instance in this document.
- **Shared resources** are assigned, not consumed. One instance serves
  many patients concurrently, with no capacity bound and no
  double-booking invariant to enforce. Providers are the instance:
  an attending has many patients at once, and nothing about that is a
  conflict.
- **Attribute pools** are sampled once at patient creation and carried
  as ordinary patient state thereafter. They are never tracked as a
  resource at all — there is no board, no assignment event, no
  capacity. Payers are the instance: a patient's payer is a fact about
  the patient, not a thing the hospital allocates.

The taxonomy is a spectrum of *how much the engine cares*: exclusive
resources need a projection and an invariant catalog; shared resources
need only a sampling rule; attribute pools need only a sampling rule
applied earlier and never touched again. Getting a future resource's
class right up front avoids over-building (giving a payer an occupancy
board nobody needs) or under-building (letting two patients silently
share a bed because nobody wrote the invariant).

## Facility model (beds — exclusive)

**Config.** `sim-config` carries a `:facility` key: a facility id plus
a vector of wards. Each ward is
`{:id :name :beds N :surge-slots M :surge-format "%s-H%02d" :class :inpatient|:ed}`.
Bed ids are *derived* from `:id` and `:beds` (e.g. a ward `:id :renal`
with `:beds 12` derives `RENAL-01` … `RENAL-12`) — they are never
enumerated in config, so growing a ward's capacity is a one-line edit,
not a list-maintenance chore. The shipped default config is small on
purpose: one ED ward and two inpatient wards, enough to exercise
transfers and surge without asking a config author to model a whole
hospital before the engine can run.

Pathways name *wards* — `{:type :admission :location "Renal"}` is a
statement of clinical intent, not an operational commitment. The
engine assigns the actual *bed* at execution time; that assignment is
an operational outcome, seeded like everything else. Bed choice among
the free beds in the target ward is a uniform draw from the run's one
RNG, same determinism law as every other stochastic choice in the
theory.

**Single source of truth.** The patient's own state carries its
current location (bed or ward, depending on status — see boarding,
below). The occupancy board — "what's in RENAL-04 right now" — is not
a second authoritative structure the engine maintains in parallel; it
is a **derived index**, a projection over the set of patient states,
computed or incrementally maintained but never a place new facts get
written first. The consistency law that makes this safe to state
plainly: **board ≡ fold over patient locations** — recomputing the
board from scratch by folding over every patient's current state must
equal whatever board the engine is actually using, at every point in
a run. This is a property test, not a code review claim. Every bed
invariant below (no double-occupancy, capacity bound, transfer-from
matches current state) is a check on the *projection*, not a
separate thing the engine has to keep in sync by discipline.

**Scope qualifier, M5b (`components/patient-simulator/docs/gmf-interpreter.md` section 4 item 8):**
"every patient's current state," above, means every **inpatient/ED**
patient's — an outpatient (`:class :outpatient`) was never a candidate
for this board to include in the first place, not an exception carved
out of an otherwise universal law. `ehrt.sim.facility/
occupancy-board` already folds only patients carrying a `:bed`
(`get-in patient [:location :bed]`), which an outpatient patient never
does (`:location` stays nil for the visit's whole duration, the same
document's item 6) — the scope narrowing is true by the board's own
existing construction, this note just states it as a law rather than
leaving it implicit.

This is the house pattern, not a one-off for beds: **one authoritative
record (the patient's own state), everything else a projection with a
proven consistency law.** [`sim-theory.md`](sim-theory.md)'s open
question #3 — whether `state-history` is primitive or derived from
`ground-truth-log` — is the same question at the theory's own waist:
if replaying the log reconstructs history, history is a projection
too. The occupancy board is the concrete, near-term instance of the
same shape; when open question #3 resolves, expect the same argument
to apply almost verbatim.

**Allocation ladder.** Placing a patient tries rungs in order, each
rung's choice among its candidates seeded:

1. **Home-ward licensed** — a free bed in the ward the pathway named.
2. **Home-ward surge** — a free surge slot in that ward, once licensed
   beds are full.
3. **Other-ward licensed** — a free licensed bed in a different ward
   (outlier placement). This is recorded distinctly from a home-ward
   placement, since "medical patient boarding in a surgical bed" is
   itself an operationally interesting fact a downstream consumer may
   want to assert on.
4. **Boarding** — physically occupying an ED surge slot while
   administratively `:admitted` to the target ward. This is the rung
   that motivates a real state-model distinction: a boarding patient's
   *status* (admitted, to Renal) and *location* (physically, an ED
   surge slot) diverge, and both are true at once. Nothing about the
   occupancy projection breaks — the ED surge slot is occupied, and
   the patient's state simply carries a location that isn't Renal
   while its status says Renal.

Boarding decouples status from location deliberately, because that
decoupling is what makes the *next* piece of behavior possible without
a redesign: a bed-ready transfer (the future `:transfer` step, ADT^A02)
triggers when some *other* patient's discharge frees a bed in the
target ward. Two patients' timelines couple through the occupancy
projection — patient B's discharge event is what makes patient A's
boarding-to-transfer event schedulable. This is exactly the kind of
cross-patient coupling a pure per-patient fold doesn't give you for
free, which is why the projection has to exist as a real (if derived)
structure the engine consults, not just a debugging convenience.

**The bed-status cycle (arc 3b sweep 2, ADR-0174 section 2(c)).** A bed
is not free the instant its occupant leaves it. Behind the run-config
opt-in `:bed-cycle`, `world` carries a per-bed status —
`:ready | :occupied | :dirty | :cleaning` — and the ladder's own
definition of a *free* bed narrows from "nobody is in it" to
**"status is `:ready`"**. Every rung inherits that gate at once, because
all four ask one function (`sim-model/free`); the ladder's shape and its
per-rung seeded draw are untouched.

What a discharge does changes with it, and this is the one existing
behaviour arc 3b changes rather than extends:

```
today:   discharge@t  ──────────────────────────────►  bed-ready transfer@t
cycle:   discharge@t → dirty@t → cleaning@t+d1 → ready@t+d1+d2 → bed-ready transfer@t+d1+d2
```

The bed-ready transfer is decided at the **ready** instant, against the
board as it stands *then* — which is more correct independently of the
cycle, since today's coupling hands a boarder a bed in the same second
it is vacated, with no opportunity for the world to have changed.

`d1` and `d2` are drawn **independently** from the vacated ward's own
`:turnaround-minutes` `[lo hi]` (a `Ward` key, so an ED bay and an
inpatient room turn around at different rates), on the **`:facility`**
RNG family — the family for draws that read no patient state at all, so
a ward-config edit shifts no arrival gap and no bed choice.

Each leg is a `:bed-status-change` ground-truth event whose participant
names a **bed** rather than a patient, and each renders one **ADT^A20**
(`[MSH EVN NPU]`, no PID and no PV1) so `ehrt play --board` can show a
dirty bed instead of an invisible one.

Three arcs sit outside the cycle proper:

- **Reinstatement, `:dirty → :occupied`.** A `:cancel-discharge` or
  `:cancel-transfer` puts a patient back in a bed that has been dirty
  since they left it; the cancel restores the bed's status alongside the
  location.
- **Reinstatement, `:cleaning → :occupied`** — the same arc, one leg
  later. The turnaround has TWO in-flight legs and a reinstating cancel
  can land in either; a cancel arriving after housekeeping has started
  reoccupies a `:cleaning` bed rather than a `:dirty` one. The pending
  `:bed-ready` tick then finds a bed that is no longer `:cleaning` and
  emits nothing, which is why the engine needed no change when this was
  found. Added 2026-08-29 (ADR-0174 section 2(c) ratification 4) after
  the traffic-scale close saw it at 750 and 7,500 patients and at no
  shipped corpus.
- **Correction, `:occupied → :ready`.** A `:cancel-admit`, and the
  erroneously-taken bed of a `:cancel-transfer`, return their bed
  straight to `:ready` with no event and no turnaround — an occupancy a
  cancel retracts leaves no dirt behind it.

A **`:bed-swap` allocates nothing** and is outside all of this: both its
beds are occupied by construction, and the ladder is never consulted.

**Effective capacity falls**, and that is real rather than hidden. When
turnaround pushes a ward over, `allocate` returns `{:exhausted true}`
and the engine emits a documented `:step-rejected` — the failure is
visible, which is what makes this a tuning question and not a
correctness one.

**Surge naming is config, not code.** Real hospitals name their
overflow capacity in wildly site-specific ways — hallway slot numbers,
pseudo-rooms, chair codes, "H01" for hallway bed 1. Baking one
convention into code would make every generated corpus look like the
same fictional hospital. `:surge-format` is a format string
(`"%s-H%02d"` by default) applied to the ward id and slot number,
so a config author picks their own site's idiom without touching the
engine.

**Placement is recorded on events.** Every admission or transfer event
that allocates a bed carries `:placement :licensed` or `:placement
:surge`, and `:forced true` when an authored pathway supplied a
`:force-placement` hint. The hint exists for scriptable edge cases —
an author who wants to force a specific, otherwise-improbable
placement for a targeted test (e.g. "assert the merge-after-transfer
scenario specifically when the patient is boarding") shouldn't have to
fight the ladder to get there. A forced placement overrides the
ladder outright and is exempt from the surge-only-when-full invariant
below — recorded as an exemption, not silently allowed.

**Invariants to co-land with implementation** (per the
`check.clj` co-landing convention, `AGENTS.md`):

- No bed holds two patients at once.
- An admitted patient occupies exactly one bed (or is boarding — see
  above — in which case it occupies exactly one *physical* slot while
  its status names exactly one ward).
- A transfer event's declared from-location matches the patient's
  current state at the time of the event.
- Occupancy never exceeds a ward's declared capacity (licensed +
  surge slots).
- Surge placement only occurs when the earlier rungs (home-ward
  licensed, then home-ward surge before other-ward) are legitimately
  exhausted — unless the placement is `:forced true`. **Under
  `:bed-cycle` "exhausted" means no earlier-rung bed was `:ready`**, not
  "none was empty": a surge placement made while a rung-1 bed sits
  `:dirty` is legitimate. The claim is unchanged; the reading of
  *exhausted* is the ladder's own.
- Every event that **allocates** a bed targets a bed whose status
  immediately before was `:ready` (`:bed-swap` excluded — it allocates
  nothing).
- A bed reaching `:ready` was `:cleaning` immediately before, except at
  run start, where every bed is born ready.
- Every bed-status transition is one of the seven the cycle admits:
  `ready→occupied`, `occupied→dirty`, `dirty→cleaning`,
  `cleaning→ready`, the two reinstatements' `dirty→occupied` and
  `cleaning→occupied`, and the correction's `occupied→ready`.

**Deliberate non-invariant, recorded so it isn't rediscovered as a
bug:** there is **no census floor for surge use**. A real hospital's
surge-slot policy ("don't open the hallway beds below 90% census") is
a *policy*, and policy violations are exactly the kind of operational
noise this simulator exists to generate — future churn content, not a
correctness law the engine enforces. Baking a census floor into the
invariant catalog would make it impossible to ever generate the
messy-but-real traffic of a hospital ignoring its own policy.

## Scheduling (arc 3b sweep 3, ADR-0174 section 2(b))

**Not a scheduler.** Behind the run-config opt-in `:scheduling`, the
simulator carries appointments as **state** — booked, moved, cancelled,
no-showed, kept — and nothing else. There is no facility-level resource
calendar: no slots, no provider availability, no contention. That was a
real candidate and is deliberately not taken; it opens capacity
modelling on a second axis, and what this slice asks for is state, not
a scheduler. Named, not built.

**The split.** Every arrival is either **scheduled** or a **walk-in**,
decided by one Bernoulli per arrival ordinal drawn on the `:world`
stream, followed by a lead-time draw from `:lead-time-days`. Both are
taken in the pre-loop block, in ordinal order, immediately after the
person-selection uniform — so their count is fixed by `:patients`
alone, and retuning `:scheduled-fraction` moves the split and nothing
that happens later. A scheduled arrival's whole step list is carried
behind one gated `:appointment` step, exactly as a repeat arrival's is,
for exactly that reason: the visit behind a booking happens or it does
not, and half of it happening would be a discharge with no admission.

**The outcome is the patient's own.** Kept, rescheduled, cancelled or
no-showed is decided by **one uniform on that patient's own `:patient`
stream**, banded — cancel, then reschedule, then no-show, then kept as
the remainder. The bands are why an appointment cannot reach two
terminals: one draw cannot land in two bands. A second draw, the
reschedule offset, is taken **whether or not a reschedule fired**, so a
site retuning one rate shifts no other patient's stream and no other
appointment's own.

**The follow-up is the first producer of a scheduled second encounter.**
At `decide :discharge`, a Bernoulli and an interval on the same patient
stream book a return visit at the discharge instant; if kept, it opens
a second encounter that names its appointment. An **expired** discharge
books nothing. This is what makes
`scheduled-encounter-follows-its-appointment` mean anything — and it
means anything only because sweep 1's encounter horizon landed first.

```
walk-in:    arrival@t ─────────────────────────────► encounter@t
scheduled:  registered@t → appointment@t ──lead──► encounter@t+lead
no-show:    registered@t → appointment@t ──lead──► no-show@t+lead, and nothing opens
follow-up:  discharge@t → appointment@t ─interval─► second encounter@t+interval
```

**Invariants (`ehrt.sim-check.check`), all vacuous without the opt-in:**

- Every `:reschedule`, `:appointment-cancel` and `:no-show` names an
  appointment an `:appointment` **earlier in the same patient's log**
  minted. Same-patient is the point: an id resolving against somebody
  else's booking is the cross-patient reference this forbids.
- An opener carrying an `:appointment-id` has that appointment earlier
  in its own patient's log, at or before its `:t`, and **not already
  terminal**.
- No opener carries a **no-showed** appointment's id.
- An appointment reaches **at most one** terminal — kept, cancelled and
  no-showed are mutually exclusive.

**What this does NOT buy, stated rather than discovered: none of the
four kinds reaches the wire.** They map onto the SIU family
(S12/S14/S15/S26), which is v2.4 structure, and every message this
project emits carries MSH-12 `"2.3"`. Emitting a structure the version
field disclaims would be worse than emitting nothing, so the kinds are
ground truth only and the MSH-12 question is rowed for a later arc. A
consumer reading the log sees appointments; a consumer reading the wire
does not.

**And the capacity consequence, because scheduling ADDS arrivals.**
`allocate` returning `{:exhausted true}` does not produce a visible
rejection — it **halts the run**, and `run-command` surfaces
`:error :capacity-exhausted` with no corpus and no self-check at all.
So a corpus opting into `:scheduling` owes a **measured** ladder margin
first, and the margin that matters is not per-ward occupancy but the
union of beds the four rungs can reach: a ward at 100% of its own beds
is not exhausted while rung 3 still has one.

## Providers model (shared)

**Config.** `sim-config` carries a `:providers` pool:
`{:id :name {:family :given} :role :attending|:consulting|:referring :specialty :wards [...]}`.
Ids are deterministic synthetic identifiers generated from the run's
seed, same as bed ids and MRNs — no provider is ever looked up outside
this config-carried pool.

**Identifier decision — synthetic NPIs.** Two options were on the
table for provider identifiers:

- **(a) Valid, Luhn-checkable synthetic NPIs.** A National Provider
  Identifier is a 10-digit number whose check digit is computed by
  the Luhn algorithm applied to the digits prefixed with the constant
  `80840` (the CMS-assigned health-industry-number issuer prefix).
  Generating candidate 9-digit bodies from the run's seeded RNG and
  computing a correct check digit produces numbers that are
  *structurally* valid NPIs — they would pass any downstream parser
  or validator that checks NPI format — without being assigned to any
  real provider.
- **(b) An obviously-fake format** (e.g. a prefixed sentinel range
  that no real NPI could occupy).

**Decision: (a).** A generated 10-digit number that happens to
coincide with a real NPPES-assigned NPI is possible (the valid-NPI
body space is 10^9 — the check digit is determined by the nine body
digits, not free to vary — and NPPES has roughly a few million active
assignments, so collision probability is low but non-zero) and
**harmless**: NPPES is itself public data (the NPI
Registry is a public lookup service), so a coincidental match reveals
nothing not already public, and no PHI is implicated — an NPI
identifies a *provider*, not a patient. Recommending (a) over (b)
because realism is a stated product goal (`docs/problem-statement.md`
constraint 2, US conventions) and a downstream system that validates
NPI check digits (many EHR-adjacent systems do, since a malformed NPI
is itself a common real-world data-quality bug worth testing against)
should see a well-formed identifier, not a value it will reject before
the rest of the message is even exercised. The caveat — coincidence
with a real NPPES assignment is possible and understood to be
harmless — is recorded here rather than left implicit.

**Assignment.** At admission, an attending is sampled (seeded) from
the providers whose `:wards` includes the admitting ward — "ward-
eligible" providers. The chosen provider is carried as ordinary
patient state (not a projection; nothing about a provider assignment
needs a board) and rendered in PV1-7 as `id^family^given`, HL7's
usual attending-doctor field shape. Providers serve many patients
concurrently by construction: there is no occupancy invariant to
write for a provider, because "attending has 40 patients at once" is
never a bug. (Synthea's own encounter export maps its `provider`
column to an *organization*, not a clinician, leaving many encounters
without an identifiable physician — Synthea issue #547, per
`docs/research/SimHospital-Synthea-limitations-considered.md` §4.4 —
a gap this model's provider/facility split already avoids by
rendering a real per-patient attending, not an organization stand-in.)

**Captured, not designed — open questions:**

- **Attending reassignment on service transfer vs. location
  transfer.** A patient moving wards (a location transfer) may or may
  not imply a new attending (a service transfer); real hospitals do
  both independently. This model doesn't yet decide whether/when
  reassignment happens; it's flagged for whichever milestone lands
  the transfer step family in earnest.
- **Caseload as a realism knob.** Sampling uniformly among
  ward-eligible providers ignores caseload — a config knob that
  weights sampling by "how many patients does this provider already
  have" would produce more realistic clustering (a few attendings
  carrying most of a ward's census) but isn't designed here.

## Payers model (attribute pool)

**Config.** `sim-config` carries a `:payers` pool:
`{:id :name :type :medicare|:medicaid|:commercial|:self-pay :weight}`,
shipped with US-ish default weights (commercial and Medicare
dominant, Medicaid and self-pay smaller shares, in line with the
general shape of US payer-mix data).

**Sampling.** A payer is sampled once, at patient creation, and never
resampled or tracked thereafter — the defining property of an
attribute pool. Age-linked weighting is applied where it's trivial to
do so without inventing a distribution: Medicare dominance kicks in
at 65+, mirroring real US payer mix (Medicare eligibility itself
starts at 65). The distribution *idea* — payer selection correlated
with age — is mined from Synthea's own payer-assignment logic, a
tier-1 implementation source per
[`docs/third-party-sources.md`](third-party-sources.md); this model
does not claim to reproduce Synthea's exact tables, only the shape of
the age-correlation.

**Whose job this is. Landed, Milestone M4.** `Persona`
([`sim-theory.edn`](sim-theory.edn)) now samples payer alongside every
other per-patient attribute (demographics, etc.) — `ehrt.sim.persona/persona`,
folded into patient state by the engine-internal `:registered` event.
There was never an actual engine-patient-init stand-in coded (the
`:payer` field this section originally described stayed nil
throughout M1-M3); Persona landing simply makes the sampling this
section always specified real, in the place it was always meant to
live.

**Carried, rendered, never tracked.** The sampled payer rides on
patient state like any other attribute and renders in the IN1 segment
once that segment lands — a later milestone (M4 in the roadmap), not
this document's near-term scope. It is never occupancy-tracked and
never exclusive: two patients sharing "commercial" is not a
resource conflict, it's just two patients with the same attribute
value.

## Summary of decisions and open questions

Decisions made here, for the author to ratify or veto (see this
session's closing summary): the three-class taxonomy and its names;
bed ids as derived, never enumerated; the four-rung allocation ladder
and boarding's status/location decoupling; surge naming as config;
`:force-placement` as an invariant-exempting authoring escape hatch;
the no-census-floor non-invariant; synthetic NPIs generated
Luhn-valid (option a) with the NPPES-coincidence caveat; payer
sampling as Persona's eventual job, age-linked at 65+ for Medicare.

Open questions, recorded rather than silently decided: attending
reassignment semantics on service vs. location transfer; caseload as
a future realism knob for provider sampling.
