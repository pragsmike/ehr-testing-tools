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

## Pharmacy turnaround (ADR-0189)

A ward also states how long its pharmacy takes. `:dispense-minutes`
`[lo hi]` is an optional `Ward` key: the minutes from a medication order
written during an admission on that ward to its `:inpatient`
`:medication-dispense`, drawn uniformly over the range, one draw per
order. It is read only when the run sets the `:dispense` engine key;
the key is the opt-in, and this is what tunes it.

```clojure
{:id :ed :name "Emergency" :beds 0 :surge-slots 6 :surge-format "%s-H%02d"
 :class :ed :turnaround-minutes [5 15] :dispense-minutes [5 25]}
```

A ward that states none takes its class's default:

| ward `:class` | default `:dispense-minutes` | basis |
|---|---|---|
| `:ed` | `[5 25]` | cabinet-stocked doses at a level I trauma centre: median order entry to cabinet dispense 9 to 15 minutes (J Pharm Pract, doi 10.1177/0897190017735390) |
| `:inpatient` | `[15 60]` | pharmacy-processed STAT orders at a community hospital: median 29 minutes, interquartile range 16 to 63 (Pharmacy Practice 2016;14(2):647, PMC4930852); pharmacy-prepared IV antibiotics: median order to delivery 43, later 27 minutes (J Pediatr Pharmacol Ther 2025;30(2):191) |

**The defaults are provisional.** They were chosen from a quick reading
of those three studies and are fitted to none of them. Uniform is the
engine's shape for a range, as it is for bed turnaround; the published
distributions are right-skewed. A site with its own pharmacy data
states its own range per ward.

The schema refuses a range that is not one: both ends are integers,
`0 <= lo <= hi`. `[0 0]` is legal and dispenses at the order's own
instant.

The ward is the one the patient is in **when the order is written**; a
transfer before the dispense does not change the draw. The draw is on
the patient's `:pharmacy` RNG family, which nothing else reads, so
setting the key or retuning a ward moves no other draw of the run.

**An order can go undispensed.** When the turnaround elapses, pharmacy
fills the order only if the patient is still in the same admission and
the order is still active. An order ended first, or a stay closed
first, gets no dispense: the order was stopped before pharmacy filled
it. A patient absorbed by a merge takes their unfilled orders with
them.

## The community pharmacy (ADR-0190)

A prescription a patient takes home is filled outside the hospital, or
not at all. Two optional `Facility` keys, beside `:wards`, say how
often and how soon. They sit on the facility because a community
pharmacy belongs to no ward. Like `:dispense-minutes` they are read
only when the run sets the `:dispense` engine key.

```clojure
{:id :metro-general
 :wards [...]
 :community-fill-share 0.80
 :community-fill-minutes [60 10080]}
```

| key | meaning | default |
|---|---|---|
| `:community-fill-share` | the probability, 0 to 1, that a prescription is filled at all | `0.80` |
| `:community-fill-minutes` | `[lo hi]` minutes from the visit's close, or the discharge, to the fill; uniform | `[60 10080]`, one hour to seven days |

**Which prescriptions.** Each medication order an outpatient visit
wrote and left open, counted from the visit's close. And each
**discharge prescription**: an order written during an admission that
is still active, and that pharmacy never dispensed, when the stay is
discharged, counted from the discharge. An order the stay ended, or one
already dispensed `:inpatient`, is not one.

**Both defaults are provisional.** They were chosen from a quick
reading of four studies and are fitted to none of them:

- 78% of 195,930 e-prescriptions were filled, and 72% of those for a
  new medication (Fischer et al., J Gen Intern Med 2010;25(4):284-90,
  PMID 20131023).
- 17% of 150,565 new prescriptions in primary care were never filled
  (Tamblyn et al., CMAJ; <https://cmaj.ca/node/149155>).
- Of 32,586 new chronic-medication orders in an elderly US cohort, 75%
  were filled within 7 days and 81% within 30 (Franklin et al., J Gen
  Intern Med 2018, PMC6206334).
- After discharge from a general medicine service, 28% of patients with
  new prescriptions had not filled one or more at 7 days (PMC3642181).

**One share for every prescription is a simplification.** The studies
report shares that differ by drug class, by whether the medication is
new, and by setting.

**A uniform lag is cruder than the studies.** Real first fills cluster
in the first day or two and trail off over weeks. A uniform draw over
one hour to seven days puts the median at three and a half days and
lets nothing arrive later than seven. Uniform is the engine's shape for
a range, not a claim about that distribution.

The schema refuses a share outside 0 to 1 and a range that is not one
(`0 <= lo <= hi`, integers). A share of `1` with a range of `[0 0]`
fills every prescription at the instant its visit closes, which is the
rule this key had before these options existed (ADR-0187).

**The draws.** Two per prescription, on the patient's `:pharmacy` RNG
family: filled at all, then the lag. Both are always taken, so one
prescription's outcome, or a retuned option, moves no other draw. A
visit's orders are drawn when the visit closes. An admission order's
are drawn when the order is written, after its turnaround draw, since
whether it becomes a discharge prescription is not known until later.

**A prescription can go unfilled, and absence is the only record.**
Four ways: the share's draw; the order ended before the lag elapsed;
the patient died first; a newer order of the same citation replaced
it. (Until ADR-0191 a merge was a fifth: see below.) One
more for a discharge prescription: a cancelled discharge put the
patient back in the bed. No event marks an unfilled prescription. A
consumer must not read a `:medication-order` with no
`:medication-dispense` as an error.

**A community fill belongs to no encounter.** It carries no
`:encounter-id`, also when it lands while the same patient is inside a
later visit or stay.

## Refills, and a fill through a merge (ADR-0191)

A long-term medication is refilled. A medication order may be marked
**chronic**, and every dispense carries a **fill number**.

**The marker.** `:chronic true` on a `:medication-order` step and on
the event it produces. It is `true` or absent; absent means nobody
marked the order, and `false` is never written. Four vendored modules
carry Synthea's own flag and it now reaches the event (`osteoporosis`,
`metabolic_syndrome/medications`, `medications/emergency_inhaler`,
`medications/maintenance_inhaler`). A pathway author states it on the
step:

```clojure
{:type :medication-order
 :codes [{:system :rxnorm :code "860975"
          :display "24 HR Metformin hydrochloride 500 MG Extended Release Oral Tablet"}]
 :chronic true
 :citation {:module "clinic" :state :chronic-start}}
```

**The fill number.** `:fill-number` on every `:medication-dispense`,
authored, inpatient and community alike: which dispense of its order
this is, from 1. Several dispenses naming one `:order-event-id` are one
prescription filled and refilled.

**The chain.** Under the `:dispense` engine key, each community fill
of a chronic order may be followed by another. Three more optional
`Facility` keys say how:

```clojure
{:id :metro-general
 :wards [...]
 :community-fill-share 0.80
 :community-fill-minutes [60 10080]
 :refill-interval-days [28 35]
 :refill-continuation 0.94}
```

| key | meaning | default |
|---|---|---|
| `:refill-interval-days` | `[lo hi]` days from one fill to the next; uniform | `[28 35]` |
| `:refill-continuation` | the probability, 0 to 1, that a fill is followed by another | `0.94` |
| `:refill-tail-days` | a hard cutoff: no refill is queued later than the run's last arrival plus this many days | none; absent means no cutoff |

The schema refuses an interval that is not `1 <= lo <= hi` in whole
days, a chance outside 0 to 1, and a tail that is not a non-negative
whole number. A tail is stated or left out; `nil` is refused.

A refill takes no share draw and no lag: it lands one interval after
the fill before it. It is declined, and the chain ends, if the order
has ended by then, if a newer order of the same citation has replaced
it, or if the patient has died. An `:inpatient` dispense is never
refilled. A prescription that was never filled is never refilled. A
chronic discharge prescription is refilled as a clinic visit's is. A
`:medication-dispense` step you author starts no chain.

**What stops a chain.** With no tail stated, which is the default:

- the continuation draw. At 0.94 a filled chronic order is refilled
  15.7 times on average (0.94 / 0.06), about sixteen months of fills.
  In `dense-7500` half stop within ten to twelve refills, and the
  longest runs to 123, over ten years;
- the order's end (a `:medication-end` naming it);
- a newer order of the same citation, which takes over with a chain
  of its own;
- the patient's death.

A merge stops nothing: a refill pending when its patient's record is
absorbed is carried to the surviving record, and the chain runs on
there (below). One case is not covered: a record that had already
expired when it was absorbed is `:merged`, not `:expired`, so a refill
carried from it is not declined for the death.

**Both defaults are provisional.**

- A 30-day supply is the traditional retail unit. Ninety-day fills are
  real and are not modelled: they ran at 12 to 15% across four drug
  classes in a post-infarction cohort of 353,259 patients
  (<https://scholars.duke.edu/individual/pub1470204>).
- About half of patients who start a chronic medication discontinue it
  (Franklin et al., J Gen Intern Med 2018, PMC6206334, as background).
  0.94 compounded over twelve refills is about one half.
- One interval and one chance for every chronic order and every fill
  is a simplification: real discontinuation is front-loaded.

**The tail is optional, and absent by default.** `:refill-tail-days`
is for a scenario that wants a hard cutoff. Stated, no refill is
queued for an instant later than the last arrival plus that many days.
The arrival clock is the measure because it is fixed by the
configuration before anything is decided: `--patients` arrivals, each
up to `:arrival-gap` minutes after the one before.

- **Leave it out of a long-horizon scenario.** The arrival clock is
  short (under 20 days in `dense-7500`'s 28,024-arrival cell), and a
  module with a long horizon, a booked visit or a returning person can
  write an order well past it. With a tail stated, an order written
  after the bound is filled and never refilled, and every refill there
  is sits in the weeks after the last arrival. From 2026-11-21 to
  2026-11-22 a tail of 90 days was the default, and it confined
  `dense-7500`'s refills to its first 110 days of twenty years.
- **Without a tail a run ends when its chains do.** Refills are events
  like any other and the run carries on until the last one. At the
  default continuation that is a few years for the longest chain in a
  few thousand: `dense-7500`'s last refill is on day 3,948 at 28,024
  arrivals, inside a log that runs to day 7,395 on `:persons` anyway.
  A scenario whose other events all fall near its arrivals will grow
  by those years.
- **State a tail with a continuation of 1.** A chain that always
  continues, on an order nothing ever ends, never stops, and the run
  does not finish. The schema does not refuse the combination.

**The draws.** Two per community fill of a chronic order, on the
`:pharmacy` RNG family of the patient who wrote the order: another
fill or not, then the interval. Both are always taken, and the tail
is tested after them, so stating or dropping a tail moves no chain's
draws. A patient has one `:pharmacy` stream, though: a chain that runs
longer takes more of it, so a prescription the same patient is written
later draws its share and lag further down the stream, and its fill
can move. Measured in `dense-7500` when the default tail went: one
such prescription at 7,500 arrivals, two at 28,024.

**A pending fill follows its patient through a merge.** A merge
corrects a record; the person still collects the medication. A queued
inpatient dispense or community fill whose patient is absorbed is
carried to the surviving record: the dispense is the survivor's event,
under the surviving MRN, and it still names the absorbed record's
order. A chronic chain carries on for the survivor. A carried
inpatient dispense goes to the survivor only if the survivor is an
inpatient at that instant, and then carries the survivor's own
`:encounter-id`; the absorbed record's stay never closes, so there is
no other stay to dispense into.

**A chain stops without an event.** The patient stopped (the draw),
the order ended, a newer order replaced it, or a stated tail was
reached.
As with a missing first fill, absence is the only record.

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
