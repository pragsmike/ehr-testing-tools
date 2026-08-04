# Modeling and encoding clinical realities

Hospitals generate events that people outside the domain — and many
inside it — would never think to model. This document is the catalog
of those realities: what actually happens, how it shows up on the
wire, how this simulator models it, and why a tester should care.
Each entry follows the same four-part shape so the catalog stays
extensible and each entry doubles as a backlog item with its design
already sketched.

The shape:

1. **The reality** — what actually happens in hospitals.
2. **The wire truth** — how it appears in records and messages;
   what is standard HL7 and what is site-custom.
3. **Our model** — the states, events, attributes, and config that
   represent it (or will; entries may precede their milestones).
4. **Why testers care** — what this reality breaks in real systems,
   i.e. its value as generated test traffic.

Entries marked *(stub)* are captured-not-designed: real phenomena
recorded here so they aren't rediscovered, with modeling deferred to
a named or future milestone.

---

## Post-mortem events

**The reality.** A patient's death does not end their event stream.
The body transfers to the morgue, then releases to a funeral home;
some deaths fall under medical-examiner jurisdiction and get an
autopsy referral; and — the case almost nobody outside the field
expects — an organ donor may remain in the ICU for hours to days
*after declaration of death*, ventilated and on pressors for donor
management, followed by organ procurement, which is a full surgical
procedure (OR, surgical team, real procedure codes) performed on a
legally dead patient. During donor management, active orders and
results continue on a decedent, and billing responsibility for
donor-management activity typically shifts from the patient's payer
to the organ procurement organization — a payer change post-mortem.

**The wire truth.** Standard HL7v2 carries more of this than
expected: PID-29/PID-30 hold death datetime and indicator; PV1-36
discharge disposition uses table 0112, whose expired codes (20,
40–42) distinguish died-here from died-at-home from place-unknown;
the morgue is routinely a real *location*, so a genuine ADT^A02
transfers the decedent to MORGUE before the final A03; procurement
and autopsy have real SNOMED and ICD-10-PCS codes. Beyond that it
goes site-custom fast: decedent-affairs workflows, ME referral,
release-of-body tracking — typically Z-segments and house-defined
order types (see the site-profile entry, below).

**Our model.** `:expired` is a status reached via a death event or an
expired discharge disposition — and it is *clinically absorbing but
operationally alive*. The event-validity table expresses this
directly: therapeutic-intent event classes become illegal in
`:expired`, while an enumerable set remains legal — morgue and
funeral-home transfers, autopsy and specimen events, and, gated on a
`:donor` attribute, the donor-management and procurement family. This
is the entry that motivates the validity table's conditional-row
shape (status × event-class × attributes, not status × event-type
alone). Facility config gains a `:class :morgue` ward. The
post-mortem payer shift is recorded as an attribute-change event when
the donor pathway lands. Milestone: the mechanism (conditional
validity rows, morgue ward class, expired status) is near-term
state-model material; the donor pathway itself is later content.

**Why testers care.** Post-death activity is a classic
downstream-system rejection: many EHRs and interfaces hard-fail any
activity after PID-29's death datetime. A donor-management scenario —
orders, results, and an OR procedure all timestamped after declared
death, followed by a morgue A02 and a disposition-20 A03 — is
premium traffic precisely because it is *correct* and still breaks
naive consumers for the right reasons. So is the simpler case: a
morgue transfer arriving after a system already closed the encounter.

---

## Site-defined codes and Z-segments *(stub — design sketched, see roadmap)*

**The reality.** Every hospital defines its own custom fields and
code values, and they all do it differently — this is the
standards-sanctioned escape hatch (HL7's user-defined tables; whole
site-invented Z-segments) that makes the universal representation
unattainable, per the guide's opening chapters.

**The wire truth.** User-defined tables (patient class 0004,
disposition 0112, location types) where sites extend freely;
Z-segments (ZPI, ZDS, …) that every interface must route and no
universal parser can know. Simulated Hospital's own struct concedes
it: `Type string // values are defined per-trust`.

**Our model.** The *site profile* config layer: code-table overrides,
naming idioms (`:surge-format` was this layer's first citizen),
Z-segment templates binding fields to state paths — applied at the
emitter, since idiosyncrasy is mostly a rendering dialect, with the
reserved open `:attributes` map as the state-side carrier when a
custom field needs a fact standard state doesn't hold. Two site
profiles over one seed = the same ground truth in two hospitals'
accents; truth is invariant under dialect.

**Why testers care.** "How do I make it simulate *my* hospital" is
the first question domain experts ask; site-custom values are also
among the first things that break a naively-configured interface.

---

## Newborns and the Babyboy/Babygirl merge *(stub)*

**The reality.** Birth creates a patient who never arrived: an
admission with no prior existence, initially registered under a
placeholder name ("Babyboy JONES"), linked to the mother's record,
and later renamed — and frequently *merged* when registration created
duplicates in the delivery rush.

**The wire truth.** A04/A01 for the newborn, mother-baby linkage
fields, an A08 demographic-overhaul storm at naming, and the A34/A40
merge family cleaning up afterward.

**Our model / why testers care.** A natural churn generator (M2's
merge steps get an organic scenario rather than only an injected
one), and placeholder-name→rename→merge sequences are notorious
patient-matching test cases. Deferred until identity-under-merge
lands (M2 prerequisite work).

---

## Leave of absence *(stub)*

**The reality.** An admitted patient goes home for a weekend —
mid-admission — and comes back. The bed may be held or released.

**The wire truth.** ADT^A21 (leave of absence) / A22 (return), a
message pair most integration engines have never been tested against.

**Our model / why testers care.** A status sub-mode like boarding
(admitted, physically absent), one validity-table row-set, and an
occupancy question (held vs released bed) the projection can express
either way. Rarely-exercised message types are exactly what a
conformance gate should see at least once.

---

## ED diversion / waiting-room boarding *(stub)*

**The reality.** When a facility has no bed anywhere it can legally
place a new admission — every rung of the allocation ladder
exhausted, including ED surge — real hospitals don't crash; they
divert incoming ambulance traffic to another facility, or hold the
patient in the ED waiting room (not yet a bed of any kind) until
capacity frees up.

**The wire truth.** Diversion status is typically a site-operational
signal (bed-board systems, sometimes a Z-segment or an external
feed), not a patient-level ADT message; a waiting-room-held patient
generates no ADT^A01 at all until a bed is actually assigned —
registration may still occur, but admission does not.

**Our model / why testers care.** This session's ladder exhaustion
(`ehrt.sim.facility/allocate`) is deliberately a **result, not
a state**: it stops the run with a structured `:capacity-exhausted`
outcome rather than throwing, but does not yet model a *waiting*
patient who eventually gets placed once a bed frees up — that needs a
real `Pending`-family status (the validity table's `:pending-*` row
already anticipates it). Modeling the wait-then-place sequence is
M3+; a downstream consumer whose interface silently drops or
mishandles diversion/boarding signals is exactly the kind of
capacity-pressure edge case this simulator should eventually be able
to generate on purpose.

---

## Observation-vs-inpatient class flips *(stub)*

**The reality.** A patient's *class* changes mid-stay — outpatient/
observation converted to inpatient (or the reverse) for clinical and
billing reasons, sometimes retroactively.

**The wire truth.** ADT^A06/A07 (change patient class), among the
most billing-sensitive and least-tested messages in the ADT family.

**Our model / why testers care.** `:class` is already split from
`:status` in the accumulator (the SimHospital PV1-2 lesson), so a
class-change event is cheap; retroactive flips are premium
churn-adjacent traffic. Candidate for the M2 churn family or
immediately after.

---

## ED-to-inpatient conversion as a NEW encounter record, not a same-stay event *(stub — surfaced by M7's own vendored content)*

**The reality.** A patient admitted through the ED and taken to surgery
or an inpatient bed is, physically, one continuous hospital stay — but
many registration/billing systems open a genuinely SECOND encounter or
account record at the moment of conversion (ED account closes,
inpatient account opens), rather than transferring the same encounter
between locations. Real EHR/ADT practice is inconsistent about this:
some sites model it as a single encounter with a location change
(ADT^A02, the transfer this simulator already generates); others
genuinely close the ED account and register a new inpatient one,
because the two halves of the stay are billed, coded, and tracked
under separate visit numbers.

**The wire truth.** Site-dependent — either a single VisitID carried
through an A02 transfer, or two distinct VisitIDs joined only by the
patient's own MRN and a shared timestamp, with no standard segment
asserting "these are the same physical stay." A downstream system
naively assuming one encounter number per stay will misjoin or drop
the second half either way it's modeled.

**Our model.** Surfaced directly by a real vendored Synthea module,
not invented: `appendicitis.json` (`components/sim-trajectory/docs/gmf-interpreter.md`'s M7
survey) authors this exact pattern — an emergency `Encounter`
immediately followed by its own `EncounterEnd`, then a new inpatient
`Encounter` opens with zero elapsed time. This project's
`compile-trajectory` currently treats the FIRST `:encounter-end` as
closing the patient's only compiled encounter for the run
(`encounter-closed?`, M5b) and silently drops everything after —
correct for the lifetime-recurrence case that mechanism was built for,
wrong for this one. The right model is `:transfer` (ADT^A02, already a
real step type, `ehrt.sim.pathway`) for sites that model it as
one encounter, or a genuine second `:admission` under a new participant
identity for sites that model it as two — a real design decision, not
yet made, named in `.agents/plans/roadmap.md`'s own M7 deferred-ledger
bullet rather than decided here.

**Why testers care.** An interface that assumes "one VisitID per
physical stay" breaks on the two-account sites; an interface that
assumes "location changes, encounter number doesn't" breaks on the
new-account sites. Generating this traffic on purpose — once the
mechanism above is built — would be premium content for exactly the
same reason the newborn merge and post-mortem entries already are: it
is *correct* hospital behavior that still breaks naive consumers.

---

## Results pending at discharge *(stub — mechanism landed, M3)*

**The reality.** A lab result does not always come back before a
patient goes home. Turnaround time is asynchronous to the rest of a
patient's own course of care: a clinician can discharge a patient with
labs still pending, on the reasonable expectation that the result will
route to the ordering provider (or a follow-up encounter) once it
finalizes. This is routine, not exceptional — the alternative (holding
every patient hostage to their slowest pending lab) is not how
hospitals actually operate.

**The wire truth.** An ORU^R01 timestamped after the ADT^A03 that
closed the encounter it belongs to is completely legitimate traffic —
the result still carries the original order's context (patient,
ordering provider, specimen) via ORC/OBR, it simply arrives on its own
schedule, decoupled from the encounter's own ADT lifecycle.

**Our model.** `:order`'s `decide` samples its paired `:result-
available` event's full turnaround atomically at order-time but rides
`:schedule-followup` (`ehrt.sim-engine.engine`) to enter the log at its
own correct future position — nothing in that mechanism blocks the
patient's other steps, including `:discharge`, on the pending result.
This surfaced as a real finding during M3's own integration testing,
not a design decided up front: the first draft of the `check.clj`
event-validity invariant for order/result events generalized
"admitted-only" to *both* `:order-placed` and `:result-available`,
which is wrong — a result arriving after a legitimate discharge would
then read as a bug. The correction (`docs/patient-state-model.md`'s
event-validity table, the `:result-available` row) scopes the
admitted-only constraint to `:order-placed` alone; a result's own
constraint is purely referential (it must name a real, preceding
`:order-placed` event for the same patient), never status-based. This
is the same invariant-scoping lesson `check.clj`'s
`order-only-when-admitted` docstring and
`result-references-existing-order-and-follows-it-in-time` now encode
in code, cited here as the clinical-realities catalog entry that
motivated it.

**Why testers care.** Post-discharge results are a classic downstream
breaker: an interface that assumes "this MRN's encounter is closed,
therefore no more messages for it" will silently drop, misroute, or
error on a perfectly legitimate late-arriving ORU — exactly the kind
of naive-consumer failure this simulator exists to surface on purpose,
generated here as ordinary traffic (no special flag needed), not as a
contrived edge case.

---

*Adding an entry: follow the four-part shape; link the milestone that
lands it or mark it (stub); if it motivates a mechanism (as
post-mortem motivates conditional validity rows), name the mechanism
explicitly so the design credit is traceable.*
