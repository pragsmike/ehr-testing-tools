# Patient state model

This document specifies the patient lifecycle state machine: the shape
of the accumulator `ehr-testing-sim.engine/evolve` folds the
ground-truth log into (ADR-0008), the states and transitions that
accumulator moves through, and the event-validity table that will
double as `check.clj`'s invariant skeleton now and `InjectChurn`'s
applicability oracle later (M2b). It formalizes what
[`docs/operational-models.md`](operational-models.md) describes in
prose — that document is authoritative on *policy* (the allocation
ladder, the taxonomy, the NPI decision); this one is authoritative on
the literal *shape* of the thing pathway generation and churn validity
will read and write.

## Durations rule (ratified)

All authored durations — pathway IR (`:delay`'s `:from`/`:to`) and
engine config (`:arrival-gap`, and any future churn-profile duration)
alike — are minutes; the engine's own clock is seconds (ADR-0011);
conversion happens exactly once, at the boundary where a minute-
denominated draw becomes a clock advance (decide-time).

## Design inputs: mined from upstream

Before formalizing the schema, two upstream sources were read
specifically for how each represents patient state — Synthea's
`Person`/`Module` (Java) and Simulated Hospital's `ir.PatientInfo`
(Go), verified against source 2026-07-26. One validates this project's
existing architecture; the other is both a checklist of fields still
missing and a cautionary tale about what happens without an event log.

**Synthea validates the accumulator/cursor split, and adds two
requirements.** Synthea splits patient state into workflow cursors and
a fact blackboard: `Person.attributes` is an open `Map<String,Object>`
shared by every concurrently running GMF module, and each module
stores its own current-state cursor inside that map, keyed by module
name. Clinical facts accumulate in a separate `HealthRecord`. This
project's shape already mirrors that split — the queue-carried
remaining pathway steps are the cursor, the patient accumulator is the
blackboard-plus-record — so this is read as validation, not as a
reason to change anything. Two things Synthea does that this project
must still account for:

1. **GMF guards query state-*visit history***, not a duplicated log.
   Conditional transitions like "PriorState X, within window" walk
   `Person.history`, the person's recorded trail of visited module
   states — a mutable-world approximation of an event log. When M5
   ports the GMF interpreter, `PriorState` guards compile to queries
   over *our* ground-truth log — typed, timestamped, and already
   authoritative (ADR-0008) — rather than to a second history
   structure. **The accumulator below does not carry its own visit
   history for this reason: the log is `Person.history` done right,
   and the interpreter reads the log directly when M5 lands.**
2. **An open `:attributes` sub-map is reserved now.** Synthea's
   modules coordinate through named blackboard attributes — one
   module sets a condition flag, another guards on it. This
   accumulator needs the same open extension point once the
   interpreter exists; it is reserved in the schema below
   (`:attributes`, `[:map-of :keyword :any]`) so nothing else claims
   the name between now and M5, even though nothing writes to it yet.

Also noted, for completeness rather than as a design constraint:
Synthea advances in fixed time ticks (roughly 7 days) because its
horizon is a lifetime. This project's encounter-horizon discrete-event
engine has no equivalent tick loop — GMF `Delay` states will compile
to scheduled wake-ups (M5), not ticks, when that milestone lands.

**Simulated Hospital's `ir.PatientInfo` is a field-by-field checklist,
and a cautionary tale about representing history without a log.**
Auditing this accumulator against `PatientInfo`'s fields surfaces real
gaps and one design lesson:

- **`Class`** (EMERGENCY / INPATIENT / OUTPATIENT / PREADMIT /
  RECURRING / OBSTETRICS, PV1-2) is tracked **separately from
  `Status`** in `PatientInfo`, and this accumulator does the same
  (`:class`, below) — lifecycle (`:status`: new/admitted/discharged)
  and registration category (`:class`) move independently. This is
  exactly the axis boarding needs: a boarding patient's `:class` is
  whatever it was at admission (typically `:inpatient`) throughout,
  regardless of which physical ward they're sitting in.
- **`VisitID`** per encounter (PV1-19) — not landed yet; flagged for
  whenever encounters become first-class (readmission scenarios, M2b
  or later). Not part of the schema below. This gap is now
  evidence-backed, not merely anticipated: the research pair's own
  mining (`docs/research/SimHospital-Synthea-limitations-considered.md`
  §5.4) surfaces SimHospital's own in-code admission that a first
  pending encounter "will never be finished, since only the latest
  Encounter is checked" — direct proof that a single-current-encounter
  assumption breaks once multiple pending encounters can overlap.
  Encounters-as-first-class (visit ids, readmission, and *multiple
  concurrent pending* encounters, not just one) is captured in
  `.agents/plans/roadmap.md` to land with or immediately before
  whichever future milestone introduces `:pending-*` step types, for
  exactly this reason.
- **Pending locations and expected admit/discharge/transfer
  datetimes** (the A14/A15-family pending events) carry *expected*
  times, a field class this project's events don't have yet. Flagged
  for the M2b churn milestone (`pending-*` step types), not designed
  here.
- **`ReadmissionIndicator`, attending doctor, account status** — small,
  cheap PV1 fields once each is seen; no design burden.

**The cautionary tale, worth recording as the worked example of why
the log-is-primitive decision (ADR-0008) pays for itself starting at
M2b, not "someday":** `PatientInfo`'s location alone is *six* fields —
`Location`, `PriorLocation`, `PriorLocationForCancelTransfer`,
`PendingLocation`, `PriorPendingLocation`, and a prior for that one
too. The in-code comment on the third explains why: normal flow clears
`PriorLocation` after a transfer completes, so later messages omit it
— but `CancelTransfer` has to reinstate the value that was just
cleared, so it needs its own shadow copy to undo the clearing. Every
cancellable mutation in that codebase grows a bespoke shadow field to
hold whatever it might later need to undo. That field zoo *is* the
mutable-state workaround for not having an event log: without one,
"what was true before the thing I need to cancel" has nowhere to live
except a hand-maintained prior-value field, one per cancellable
mutation.

In this project's event-sourced engine, `:transfer-in-error`'s (M2b)
`decide` reads the prior location by querying the log directly for
that patient's most recent location-setting event before the one being
cancelled, and the emitter derives PV1-6 (prior location) the same way
at message-build time. **One `:location` field in the accumulator,
plus a log query, replaces all six `PatientInfo` location fields.**
This is not a hoped-for benefit of ADR-0008 — it is the concrete
reason M2b's cancel-family step types are expected to be cheap rather
than each growing their own shadow state.

## The accumulator

`ehr-testing-sim.engine/PatientState` (malli), the type
`evolve`'s fold produces and `decide` reads:

| Field | Type | Notes |
|---|---|---|
| `:patient-id` | `:string` | **Landed, M2a (ADR-0010).** The fold key and work-queue key — replaces `:mrn` in that role. Internal, deterministic (a pure function of the run's seed and the patient's arrival ordinal — never an RNG draw, so identity generation adds no new stochastic consumption); never reassigned, never rebinds. |
| `:mrns` | `[:set :string]` | **Landed, M2a (ADR-0010).** Every MRN this patient-id has ever answered to — a singleton set until M2b's merge exists to grow it. MRN is now *state*, not identity: a real hospital's MRN is exactly what merge changes. |
| `:active-mrn` | `:string` | **Landed, M2a (ADR-0010).** Which member of `:mrns` is currently live; emitters render this everywhere PID/control-ids used to read a bare `:mrn`. Until M2b's merge lands, always the patient's one and only MRN. |
| `:status` | `[:enum :new :admitted :discharged :expired]` | Lifecycle. Boarding is **not** a separate status — see below. `:expired` (candidate, M2b+ — see `docs/clinical-realities.md`'s post-mortem entry) is **clinically absorbing but operationally alive**: reached via a death event or an expired discharge disposition, it is not a synonym for `:discharged` — a patient can be transferred (to a morgue ward, `:class :morgue`) or undergo autopsy/donor-management events while `:status = :expired`, exactly the way an `:admitted` patient can, before a final disposition-20 `:discharge` moves them to `:discharged`. See the event validity table below for what's legal in `:expired`. |
| `:class` | `[:enum :inpatient :emergency :outpatient :preadmit :recurring :obstetrics]`, optional until admission | PV1-2. Tracked separately from `:status` per `ir.PatientInfo`'s own separation (mined above) — registration category, not lifecycle. Distinct from a *ward's* `:class` (`:inpatient`/`:ed`, `docs/operational-models.md`'s facility config) — same word, two different things: one is what kind of patient this is, the other is what a ward is designated for. Set at admission, unchanged by transfer within M1's scope. |
| `:home-ward` | `:string`, ward id, nil until admission | The ward the pathway named — clinical intent (`docs/operational-models.md`'s own term for the rung-1/2 target). Diverges from `:location`'s ward exactly on rungs 3 (outlier) and 4 (boarding) of the allocation ladder. |
| `:location` | `[:map [:ward :string] [:bed :string] [:placement [:enum :licensed :surge]]]`, nil until admission | The patient's actual **physical** location, always concrete — never nil-bed, even while boarding (see worked example below). `:placement` is exactly the two values `docs/operational-models.md` specifies; ladder rungs 3 and 4 are distinguished from 1 and 2 not by a third placement value but by `:location`'s ward differing from `:home-ward` (see the table below). |
| `:attending` | `:string`, provider id, nil until admission | References `docs/operational-models.md`'s provider pool by id; rendered PV1-7 as `id^family^given` at emit time, not stored denormalized. |
| `:persona` | `ehr-testing-sim.persona/Persona`, nil until the `:registered` event folds | **Landed, M4.** Name, DOB, sex, address, phone, an SSN-shaped id, and payer — ALL of it, sampled together by `ehr-testing-sim.persona/persona` and folded in by the engine-internal `:registered` event every patient's step queue is now prepended with (never authorable IR, the same treatment `:result-followup` already gets). This RETIRES the standalone `:payer` field this table used to carry: there was no code actually populating it (always nil, an aspiration this document itself named as an engine-patient-init stand-in), so retiring it is a schema simplification, not a behavior removal — payer now lives at `(:payer (:persona patient))`. Never re-sampled after — the attribute-pool contract (ADR-0007) extended to every persona field, not just payer. |
| `:admitted-at` | `:int`, simulated **seconds** (was minutes pre-M2a — ADR-0011), nil until admission | The moment this patient was admitted. Landed with Milestone M1 for exactly one purpose: breaking ties among multiple patients boarding for the same ward — the bed-ready transfer relieves the longest-waiting one first (earliest `:admitted-at`, `:patient-id` as a further tiebreak — `:patient-id`'s zero-padded ordinal prefix keeps this tiebreak's lexical-order property `:mrn` used to give it for free). Not a SimHospital-style shadow field — set once, never rewritten. |
| `:attributes` | `[:map-of :keyword :any]`, default `{}` | **The namespacing rule is live, M5a; the engine's own accumulator still doesn't populate this field until M5b.** The open blackboard Synthea's modules coordinate through (mined above): `ehr-testing-sim.gmf`'s loader now REALLY enforces module-namespaced keys (`docs/gmf-interpreter.md` section 5 — a module's raw `SetAttribute`/`Symptom` name compiles to `:module-id/kebab-name`, never a bare keyword; a module writing a bare engine-reserved name, e.g. `donor`, is rejected at load time), and `ehr-testing-sim.gmf-interpreter`'s own `step` only ever writes through that exact transform — this is no longer a documented convention awaiting code, it is the shape the interpreter's own attribute-registry property test (`gmf-interpreter-test/attribute-writes-are-always-in-the-declared-registry`) checks directly. What's still M5b scope: folding a module instance's own attribute map INTO this accumulator field for a real, running patient (`RunModules` meeting `Execute`, `docs/sim-theory.edn`'s own `:trajectory` stage) — M5a's interpreter carries its own attributes map per module-instance, engine-adjacent but not yet engine-integrated. |

Deliberately absent, per the mining above: no visit-history field (the
log is the history — M5's interpreter queries it directly), no
`VisitID` (encounters aren't first-class yet), no shadow prior-location
fields (M2b's cancel-family reads priors from the log).

**Landed.** `ehr-testing-sim.engine/PatientState` carries every field
in the table above, including `:location`'s `{:ward :bed :placement}`
map shape — the allocation ladder (`ehr-testing-sim.facility/allocate`)
populates it for real as of Milestone M1. One field this table didn't
originally name turned out to be necessary once the ladder's cross-
patient coupling was implemented: `:admitted-at` (the simulated moment
of admission), used only to break ties among multiple patients
boarding for the same ward — the longest-waiting one (earliest
`:admitted-at`, `:patient-id` as a further tiebreak) is the one a
bed-ready transfer relieves. It is not a shadow/undo field in the
SimHospital sense above (mined section): it is a plain fact recorded
once at admission and never rewritten, exactly like `:status` or
`:class`.

**Landed, M2a.** ADR-0010's identity split (`:patient-id`/`:mrns`/
`:active-mrn`, above) and ADR-0011's time model are both implemented,
not just designed: `ehr-testing-sim.engine/run`'s work queue and
`world :patients` map are keyed by `:patient-id`; every ground-truth
event carries a `:participants` vector (`[{:patient-id ... :role
:subject}]` for every event type today — the degenerate single-
participant case ADR-0010 names) and a `:warm-up` boolean
(`t < :warm-up-seconds`, config default 0); every `:t` is seconds from
run start. **The pathway IR is unchanged by the seconds move:**
`:delay`'s `:from`/`:to` stay authored in minutes (authoring
ergonomics, `ehr-testing-sim.pathway`'s own docstring), and the engine
converts minutes to seconds at `decide`-time — the one place a
minute-denominated draw becomes a clock advance. `:arrival-gap` (an
engine-config input, not IR) took the same minutes-authored/seconds-
internal treatment for a concrete, discovered reason: leaving it in
raw seconds while dwell times scaled to minutes×60 clustered arrivals
far faster than patients discharged, exhausting `config/default-
facility`'s real usable capacity (16 concurrent — Cardiology's surge
sits unused when every patient's home-ward is Renal) at patient counts
the invariant-catalog property test already exercised.

**Boarding relief policy: FIFO by `:admitted-at`, ratified as the
default — not a law.** Relieving the longest-waiting boarder first is
this project's ratified default, chosen because it's the simplest
policy that `:admitted-at` alone can express without inventing
another field. It is deliberately **not** baked into the invariant
catalog as a correctness rule: real hospitals often relieve boarders
by acuity, service priority, or other clinical criteria, not strict
arrival order, and encoding FIFO as a law would make it impossible to
ever generate the (equally real) traffic of a hospital whose bed-ready
transfers *don't* follow arrival order. FIFO-by-`:admitted-at` is
therefore a **site-profile/Calibrate-territory config knob
candidate** — `docs/site-profiles.md` and the `Calibrate` stage
(`docs/sim-theory.edn`) are where an acuity-weighted or other
alternative relief policy would eventually be configured, the same
"policy, not law" treatment `docs/operational-models.md` already gives
the no-census-floor non-invariant for surge use.

(Pre-M1 staging note, kept for history: the session that first landed
`evolve`/`decide` — ADR-0008 — shipped `PatientState` with every field
above present except `:location`, which stayed in its v0 bare-string
shape until this ladder existed to populate the map shape meaningfully.
That staging is now resolved.)

### Worked example: boarding, precisely

A patient admitted to Renal, but Renal (and its surge) are full, boards
in an ED surge slot:

```
:status     :admitted
:class      :inpatient
:home-ward  "renal"
:location   {:ward "ed" :bed "ED-H03" :placement :surge}
```

`:location` is always where the patient physically *is* — this
deliberately departs from an earlier hypothesis (nil `:bed` while
administrative ward carries the target) floated ahead of this
document, because `docs/operational-models.md` itself is unambiguous
that boarding means "the patient's state simply carries a location
that isn't Renal while its status says Renal" — i.e. `:location` holds
the physical fact, and the administrative fact lives in the separate
`:home-ward` field this document introduces to make that prose
literal. `:home-ward "renal" ≠ :location.ward "ed"` **is** boarding —
no separate boolean flag is needed. The same mismatch with the
*other* member of the pair distinguishes outlier placement (rung 3:
`:home-ward` ≠ `:location.ward`, but `:location.ward`'s configured
`:class` is `:inpatient`, i.e. a real other ward, not ED) from
boarding (rung 4: `:location.ward`'s `:class` is `:ed`). Rungs 1 and 2
are the base case, `:home-ward = :location.ward`, distinguished from
each other only by `:placement`.

| Ladder rung | `:placement` | `:home-ward` vs `:location.ward` |
|---|---|---|
| 1. Home-ward licensed | `:licensed` | equal |
| 2. Home-ward surge | `:surge` | equal |
| 3. Other-ward licensed (outlier) | `:licensed` | differ; `:location.ward` is `:inpatient`-class |
| 4. Boarding | `:surge` | differ; `:location.ward` is `:ed`-class |

## The deterministic event id (M2b)

Cancel-family events (`:cancel-admit`, `:cancel-transfer`,
`:cancel-discharge`) reference the event they cancel. Two shapes were
on the table: a stamped `[patient-id seq-no]` tuple on every event, or
using the event's own **ordinal position in the ground-truth log**
(its 0-based index) as the id, computed rather than stored.

**Decision: the log-position index, stamped on nothing.** The
ground-truth log is already an immutable, append-only, totally
ordered vector (ADR-0008) — its own index IS a deterministic, unique,
stable identifier for every event it contains, for the lifetime of a
run, with zero schema change to existing event types. A cancel event
carries one new field of its own, `:cancels-event-id` (the target
event's index into `ground-truth`); the event being cancelled is
untouched. This was chosen over stamping an id onto every event
specifically to keep the M2a-pinned regression fixture
(`pinned_seed_42_patients_5.edn`) byte-identical: churn is opt-in
(ADR roadmap M2b), so a config with no churn steps must produce
*exactly* the same log it did before this milestone, and a global
`:event-id` field added to every event would perturb that fixture for
a reason that has nothing to do with churn actually running. Two-
participant events (`:bed-swap`, `:merge`) are identified the same
way — by their own log position — which is also what
`docs/patient-state-model.md`'s emitter-derivability law now keys on
for those types (a single MRN no longer uniquely identifies a
message, since both carry two).

`ehr-testing-sim.engine/decide` methods that need this (the cancel
family, `:transfer-in-error`) read it off `world`'s new `:ground-truth`
key — a persistent mirror of the log-so-far threaded through `world`
specifically so `decide` can query it directly (`nth`/`filter`/
`keep-indexed`), the same "query the log, no shadow field" move
`:transfer-in-error`'s prior-location lookup already makes (see the
worked example below).

## Rejected steps (M2b-surfaced capture, ADR-0012)

M2b's `InjectChurn` property-testing surfaced a gap in the log's own
claim to be authoritative (ADR-0002, ADR-0008): a step that is
statically legal per the applicability oracle above can still be
rejected at execution time by live world state the oracle had no
visibility into (e.g. a `:cancel-discharge` reinstatement targeting a
bed someone else's admission has since reclaimed). Today that
rejection is a bare no-op — no trace enters the log. [ADR-0012](../notes/ADRs.md#adr-0012)
records the decision to close this: a `:step-rejected` event
(`:participants`, the attempted step, a reason) enters the ground-truth
log on every such rejection. It is **truth about the run, never a
message-bearing event** — no `message-type-registry` entry, by design,
since no real ADT feed carries a message for an attempted action that
never became a real one — but `check.clj`'s invariant catalog and any
test harness reading the log directly may reference it, the same
glass-box-auditability rationale every other event type already
serves. ADR-0012 also captures a v2 refinement: cancel-reinstatement
should route back through `ehr-testing-sim.facility/allocate`'s
existing ladder rather than dead-ending as a no-op, because a real
hospital doesn't fail a cancellation when the original bed is gone —
it finds the patient a different one. Both pieces are captured here as
design, scheduled M3-adjacent; no code lands with this session.

## States and transitions

```mermaid
stateDiagram-v2
    [*] --> New
    New --> Admitted : A01 admission (ladder rungs 1-3)
    New --> Boarding : A01 admission (ladder rung 4)
    Admitted --> Admitted : A02 transfer (location changes, home-ward unchanged)
    Admitted --> Boarding : A02 transfer landing on rung 4
    Boarding --> Admitted : A02 bed-ready transfer (another patient's discharge frees a home-ward bed)
    Admitted --> Discharged : A03 discharge
    Boarding --> Discharged : A03 discharge (still boarding at time of discharge)
    Discharged --> [*]

    Admitted --> Pending : pending-* (M2b, planned)
    Pending --> Admitted : pending resolves (M2b, planned)
    Admitted --> CancelledOrInError : cancel-*, *-in-error (M2b, planned)
    CancelledOrInError --> Admitted : cancel-in-error reinstates (M2b, planned)
    Admitted --> Merged : merge (M2b, planned)
    Merged --> [*]

    note right of Boarding
        Sub-mode of Admitted, not a
        fourth top-level status.
        status stays :admitted;
        home-ward != location.ward
        (ED-class) is what boarding
        means. See the accumulator
        table above.
    end note
```

Boarding is drawn as its own box only because a diagram needs a node
to hang the bed-ready-transfer transition on; per the accumulator
table, it is **not** a fourth value of `:status` — it is the
`:home-ward ≠ :location.ward` (ED-class) condition holding while
`:status` is `:admitted`. `Pending`, `CancelledOrInError`, and `Merged`
are M2b's churn family, shown so this diagram doesn't need a redraw
when M2b lands additively — only new edges, no restructuring of what's
here.

**Not yet drawn, recorded in prose rather than redrawing the diagram
this session (docs-only session; the diagram redraw is a small,
mechanical follow-on):** `Admitted --> Expired` on a death event or an
expired discharge disposition, and `Expired --> Discharged` on the
final disposition-20 discharge. `Expired` would be its own top-level
status box, unlike `Boarding` — it is a real fourth value of `:status`
(see the accumulator table), not a condition over the existing three,
because a patient in `:expired` is neither `:admitted` in the ordinary
therapeutic sense nor `:discharged` yet. `docs/clinical-realities.md`'s
post-mortem entry motivates the status; the event validity table below
formalizes what's legal while in it.

## Event validity table

For each event type, the patient states in which the event is legal to
occur. This table does **double duty**, deliberately: it is the
skeleton `check.clj`'s invariant catalog implements directly (each row
becomes "event X's patient was in a legal state at the time," a
co-landing invariant per `AGENTS.md`), and it is the applicability
oracle `InjectChurn` (M2b) will consult to decide where a churn event
can be legally inserted into an existing pathway — the same predicate
answers "was this legal when it happened" and "would this be legal to
insert here," because both ask the same question about the same
state.

**Declared shape: status × event-class × attribute-conditions, not
status × event-type alone.** The table below started as a per-event-
*type* mapping (one row per `:admission`, `:transfer`, ...), which is
enough while every event type's legality depends only on `:status`. The
post-mortem entry (`docs/clinical-realities.md`) breaks that
assumption: whether an event is legal in `:expired` depends on what
*class* of event it is (therapeutic-intent vs. administrative/post-
mortem), and one of those classes is itself gated on a patient
*attribute* (`:donor`), not on status alone. The table's real shape is
therefore a predicate over three axes — `(status, event-class,
attribute-conditions) -> legal?` — and rows below are grouped by event-
class wherever a single event type doesn't map to a single class.

| Event / event-class | Legal when (status) | Attribute condition | Illegal example |
|---|---|---|---|
| `:admission` | `:status = :new` | — | Admitting an already-admitted or already-discharged patient. |
| `:transfer` (incl. bed-ready) | `:status = :admitted` (Admitted or Boarding) | — | Transferring a patient who hasn't been admitted yet, or who's already discharged. |
| `:discharge` | `:status = :admitted` (Admitted or Boarding) | — | Discharging a patient not currently admitted, or discharging twice. |
| `:pending-*` (M2b, planned) | `:status = :admitted`, not already pending | — | Double-pending; pending a non-admitted patient. |
| `:cancel-*` / `:*-in-error` (M2b, planned) | The event class being cancelled must exist in this patient's log and not already be cancelled | — | Cancelling an event that never happened, or cancelling twice. |
| `:merge` (M2b, planned) | Both patient-ids exist (ADR-0010); at least the surviving patient-id is `:admitted` or reachable | — | Merging into/from a patient-id that was never admitted, or a double merge. |
| `:order` (M3, **landed**) | `:status = :admitted` | — | Ordering labs for a patient not currently admitted (`ehr-testing-sim.check/order-only-when-admitted`). |
| `:result-available` (M3, **landed**) | No status restriction of its own — a result is asynchronous to the rest of the patient's own pathway (auto-paired at a profile-sampled turnaround, not blocking authored steps like `:discharge`) and may legitimately arrive after discharge; **pending labs at discharge is real clinical traffic, not a bug** — a case this milestone's own integration test surfaced before this row was narrowed to match it. Its own constraint is referential, not status-based: it must name a real, preceding `:order-placed` event for the same patient (`ehr-testing-sim.check/result-references-existing-order-and-follows-it-in-time`). | — | A result whose `:order-event-id` doesn't resolve to a real prior `:order-placed` event for the same patient, or that precedes it in time. |
| Therapeutic-intent classes (orders, meds, procedures with clinical intent) | **Illegal** when `:status = :expired` | — | An order or medication timestamped after a death event — the classic post-mortem rejection case (`docs/clinical-realities.md`). `:order` is this class's first LANDED instance (row above); the `:expired` half of this row is not yet checkable in code, since `:expired` isn't a landed `:status` value yet (see the accumulator table's own `:status` note) — `order-only-when-admitted` is written as the strict generalization ("legal only when `:admitted`") that already covers it once `:expired` lands, without inventing an unfalsifiable invariant against a status no event can yet produce. |
| Morgue/funeral-home transfer | Legal when `:status = :expired` (also legal pre-expiry as an ordinary `:transfer`) | — | A morgue transfer for a patient who is not `:expired`. |
| Autopsy / specimen events | Legal when `:status = :expired` | — | An autopsy event for a patient who is not `:expired`. |
| Donor-management / organ-procurement events | Legal when `:status = :expired` | **gated on `:donor` = true** in `:attributes` | A procurement event for a patient whose `:attributes` doesn't carry `:donor true` — the gate that makes this row conditional rather than a plain status check. |
| Leave-of-absence, A21/A22 (candidate, M2b+ stub) | `:status = :admitted`, not already on leave — a sub-mode of Admitted parallel to Boarding (`docs/clinical-realities.md`) | — | Returning from leave (A22) for a patient who was never marked on leave (A21). |
| Class-flip, A06/A07 (candidate, M2b+ stub) | `:status = :admitted` (Admitted or Boarding); `:class` changes, `:status` doesn't | — | A class-flip event for a patient who isn't currently admitted. |

**Facility config gains a `:class :morgue` ward.** The morgue/funeral-
home and autopsy rows above presuppose a real *location* a decedent can
be transferred to — `docs/clinical-realities.md`'s own wire-truth
section notes a genuine ADT^A02 transfers the decedent to MORGUE before
the final A03. `docs/operational-models.md`'s facility config
(`{:id :name :beds ... :class :inpatient|:ed}`) gains `:class :morgue`
as a third documented ward class — **config documentation only, no
code this session**; a morgue "ward" is a real occupancy-tracked
location by the existing exclusive-resource model (ADR-0007), it simply
never boards or admits in the ordinary sense.

The current `check.clj` catalog (`timestamps-monotone`,
`discharge-follows-admission`) already encodes the `:discharge` row's
constraint in a more specific form (admission strictly precedes
discharge, not merely "some admission exists"); M1 formalizes the
`:admission` and `:transfer` rows as new invariants in the same change
that lands the `:transfer` step type (`AGENTS.md`'s co-landing
convention). The post-mortem, leave-of-absence, and class-flip rows are
candidates for M2b (or immediately after, per `docs/clinical-
realities.md`'s own milestone notes) — recorded here as the applicability
oracle's target shape, not yet implemented in `check.clj`.

**Landed, M2a: two structural invariants over `:participants`, plus
the warm-up mark.** `check.clj`'s catalog gains
`every-event-has-participants` (every event names at least one
participant) and `participant-ids-exist-in-run` (every patient-id named
anywhere in `:participants` traces back to an `:admission` event
somewhere in the same log — catching a stray or mistyped id a future
churn-injection step could otherwise introduce), per ADR-0010. A third,
separately-parameterized invariant, `warm-up-mark-matches-window`
(ADR-0011), checks the pure predicate `:warm-up = (t < warm-up-
seconds)` against the run's own configured window — `check/check-all`
grew a third optional arg (`warm-up-seconds`, default 0) alongside the
existing `facility-config` one, both following the same "needs more
than just the log" pattern. (This landed choice — mark the log,
never silently trim it — is exactly the failure mode Synthea's own
export-window confusion warns against: content that appears to vanish
from a generated history with no marker explaining why, issues #1465
and #1040 per `docs/research/SimHospital-Synthea-limitations-
considered.md` §4.1/§4.2. A consumer here can always tell warm-up
traffic from steady-state traffic by querying `:warm-up`, rather than
wondering where events went.)
