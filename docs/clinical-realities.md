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
(`ehr-testing-sim.facility/allocate`) is deliberately a **result, not
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

*Adding an entry: follow the four-part shape; link the milestone that
lands it or mark it (stub); if it motivates a mechanism (as
post-mortem motivates conditional validity rows), name the mechanism
explicitly so the design credit is traceable.*
