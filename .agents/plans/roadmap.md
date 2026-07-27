# Roadmap

**Status: accepted (2026-07-26).** ADR-0003 deferred writing a plan
"until first real use" of `.agents/plans/`; this is that first use.
Milestone order, scope, and the deliberate exclusions below are
author-ratified. Each milestone names the
[`sim-theory.edn`](../../docs/sim-theory.edn) stage(s) it advances and
the invariants that must co-land with it (`AGENTS.md`'s co-landing
convention: every new engine step type ships with its `check.clj`
invariants in the same change).

## M1 — Facility + providers models, transfer step, occupancy projection

Advances **Execute** (`:built`, growing its step vocabulary) via its
new `provider-pool` catalytic, plus the transfer step type.
**A02 emission is IN M1** — the co-landing convention (`AGENTS.md`)
extends to the emitter's `message-type-registry`, not just
`check.clj`: a step type without a registered message type produces
traffic that's invisible to every consumer downstream of `EmitHL7`,
which is exactly the kind of silent gap the co-landing rule exists to
prevent. The registry entry and derivability-law test coverage for
A02 land in the same change as the `:transfer` step itself, not in a
follow-on.
[`docs/operational-models.md`](../../docs/operational-models.md),
reviewed this session, is this milestone's spec — nothing here
redecides what that document already decided.

Co-landing invariants: no bed holds two patients; an admitted patient
occupies exactly one bed; a transfer's from-location matches the
patient's current state; occupancy never exceeds ward capacity;
surge placement only when earlier ladder rungs are exhausted (unless
`:forced true`). Plus the occupancy board's own consistency law as a
property test: board ≡ fold over patient locations.

## M2a — Engine prep: identity, participants, and the time model — **landed**

(Identity/participants per ADR-0010 and the time model per ADR-0011,
including the warm-up mark, are implemented and test-first — 73 tests
/ 156 assertions green; the seeded arrival process is the one sketched
item not built, explicitly a stretch M2b doesn't depend on.)

Split out of the original single M2 milestone (this session) because
its two decisions — [ADR-0010](../../notes/ADRs.md#adr-0010) (patient
identity + the `:participants` event shape) and
[ADR-0011](../../notes/ADRs.md#adr-0011) (seconds granularity, a
pinned UTC offset, a seeded arrival process, a warm-up window) — are
both **engine refactors that M2b's actual churn step types depend on**,
not churn content themselves. Landing them first means M2b's merge and
bed-swap steps are written directly against `:patient-id`/
`:participants` and the new clock, rather than against `:mrn` and
minutes with a mid-milestone migration. Both refactors are seam-able:
each can land, be tested, and be reviewed independently of the other,
and neither blocks on M2b existing yet.

**Seed perturbation, expected twice over, both already covered by
existing policy.** M1 already perturbed pinned-seed output once
(ADR-0009, the allocation-ladder and provider-sampling draws). M2a's
time-granularity change (ADR-0011 decision 1, minutes → seconds) shifts
every timestamp-consuming draw again — a second, equally expected
instance of exactly the pattern ADR-0009 already names and accepts:
same config + seed stays byte-identical *within* a generator version,
and each milestone that grows the engine's stochastic surface is
expected to regenerate pinned fixtures, not treated as a regression to
chase down. `:patient-id` generation (ADR-0010) is a further, third
draw-order change from the same milestone; the same policy covers it.

Co-landing invariants: the fold-key/queue-key rename (`:mrn` →
`:patient-id`, `docs/patient-state-model.md`'s accumulator gaining
`{:mrns :active-mrn}`) touches every existing `evolve`/`decide` method
and test helper, so the determinism and invariant-catalog property
tests must stay green across the rename, not just for new step types;
`:participants` becomes a real field on every event (single-element
vector for today's step types) with its own schema round-trip test.

## M2b — Churn family — **landed**

Lands **InjectChurn** (`:built` in the theory; `;; NEXT` moved to
`Execute`'s own further growth, M3): cancel-admit/cancel-transfer/
cancel-discharge (A11/A12/A13), transfer-in-error (A02+A12, in-error
marked), bed-swap (A17, genuinely two-participant), and merge (A40,
the identity payoff) — 134 tests / 318 assertions green, coverage
94.76%/97.25% (up from the M2a baseline 91.72%/94.69%). `churn-profile`
is real config (`ehr-testing-sim.churn/ChurnProfile`) rather than a
named-but-unbuilt resource, wired into `sim run` via `--churn` or an
explicit `:churn-profile`. Task 0's two ratified items landed alongside:
the durations rule (one line, `docs/patient-state-model.md`) and
result-not-throw capacity exhaustion (`ehr-testing-sim.facility/allocate`
no longer throws; `run-command` surfaces `:error :capacity-exhausted`),
plus an ED-diversion/waiting-room-boarding stub entry in
`docs/clinical-realities.md`. `docs/patient-state-model.md`'s conditional
validity rows (status × event-class × attribute-conditions, added M2a)
name two further candidate step families for a future milestone, as
**stretch/candidate steps, not landed this session**: leave-of-absence
(A21/A22) and observation/inpatient class-flip (A06/A07), both from
`docs/clinical-realities.md`'s stub catalog.

**Capture: encounters as first-class, M2b-surfaced.** Neither `:pending-*`
step types nor `VisitID`/PV1-19 landed this session (both flagged in
`docs/patient-state-model.md`'s mining section as pre-existing gaps,
not new ones). The research pair's own mining (group D,
`docs/research/SimHospital-Synthea-limitations-considered.md` §5.4)
upgrades this from nice-to-have to evidence-backed: SimHospital's own
in-code admission that a first pending encounter "will never be
finished, since only the latest Encounter is checked" is direct proof
that single-current-encounter assumptions break real workflows once
multiple pending encounters can overlap. The capture — visit ids
(PV1-19), readmission, and support for *multiple concurrent pending*
encounters, not just one — must land **with or immediately before**
whichever future milestone actually introduces `:pending-*` step
types, since a pending-admission mechanism designed against a
single-current-encounter assumption would reproduce the exact failure
this evidence warns against.

Co-landing invariants, landed: `cancel-references-existing-uncancelled-event`
(a cancelled event must reference an event it cancels, of the right
type, not already cancelled), `bed-swap-both-admitted-before-swap`,
`merge-survivor-absorbs-merged-mrns`, `no-events-after-merged-terminal`
— all expressed as `:participants` cross-participant coherence checks
per ADR-0010. The IR-endomorphism, clinical-steps-preserved, and
zero-probability-identity laws stated on `:churn` in the EDN are now
property tests (`churn-test`), not just claims. One design decision
surfaced only by property-testing InjectChurn against the full
invariant catalog, recorded here rather than in an ADR since it's an
internal robustness fix, not a wire/contract change: a churn-inserted
step that is STATICALLY legal (per the applicability oracle) can still
be REJECTED at execution time by live world state InjectChurn has no
visibility into (e.g. a bed a cancel-discharge would reinstate into was
reclaimed by someone else's admission in the meantime) — such a
rejection is a no-op for that one step, not a run-halting condition,
and InjectChurn's own state model treats `:cancel-discharge`
conservatively (never assumes it succeeds) for exactly this reason.

## M3 — Order profiles + order/result steps — **landed**

Advanced **Execute** via its `order-profiles` catalytic (declared
comment-only ahead of this milestone, target 3 — hashed US-units
config, now real): `:order` (author-facing IR, auto-pairs its own
`:result-followup` after a profile-sampled turnaround — the ergonomics
choice recorded over a hand-authored `:result{:order-ref ...}}` step,
`docs/patient-state-model.md`) and ORM^O01/ORU^R01 emission in
`EmitHL7` — 181 tests / 503 assertions green, coverage 95.35%/97.75%
(up from the M2b baseline 94.78%/97.25%). `resources/order-profiles.edn`
ships a small hand-curated CBC + BMP starter set, real LOINC codes
verified directly against loinc.org (`notes/facts-register.md` F7), US
conventional units, typical adult reference ranges, and a per-analyte
value distribution (normal + abnormal tails). This is the milestone
that repairs the capture gap named in an earlier theory
sync — Simulated Hospital's order profiles and the ORM/ORU cycle were
discussed from the project's first session but, until now, lived in
no planning artifact.

**The run-loop mechanism this needed, recorded here since it's a real
engine extension, not just a new step type:** `decide`'s outcome may
now carry `:schedule-followup` — a genuine future `[t seq-no]` queue
entry the run loop enqueues, distinct from returning events directly.
An `:order`'s result is fully computed (all its RNG draws, including
every analyte's sampled value and computed abnormal flag) atomically at
order-decide-time, the same "decided atomically" precedent
`:transfer-in-error` already set — but splicing that future-timestamped
event directly into the order's own `:events` would enter
`ground-truth` at the order's own log position, ahead of other
patients' smaller-`:t` events the run loop hasn't processed yet yet,
breaking the log's global time ordering. `:schedule-followup` instead
asks the run loop to enqueue it for real, so it lands at its own
correct position, the same as any other scheduled event.

**A real finding, corrected before landing:** this milestone's own
integration test surfaced that a patient can legitimately be discharged
before their result arrives (async turnaround, `:order`'s own
`:advance 0` doesn't block the rest of the pathway on the pending
result) — pending labs at discharge are real, common clinical traffic,
not a bug. The event-validity invariant this milestone co-lands
(`order-only-when-admitted`) is therefore scoped to `:order-placed`
alone, not generalized to `:result-available` as first drafted.

Co-landing invariants: `result-references-existing-order-and-follows-
it-in-time` (every result's `:order-event-id` names a real order for
the same patient, at or before the result's own time);
`result-analytes-match-order-profile` (a result's analyte set is
exactly its own profile's); `abnormal-flags-consistent-with-value-vs-
range` (the computed-truth mini-law, checked from the log directly);
`order-only-when-admitted` (the therapeutic-intent-class validity row,
scoped per the finding above). Order/result message types register in
`message-type-registry` the same way ADT types already do.

**Task 0's consumer-loop triage, landed alongside:** `manifest/build`
and `MirroredManifest` both omitted `:schema-version` — correlated
drift the mirror could not self-detect, since it validates its own
output against its own copy of the schema (a mirror cannot catch itself
agreeing with its own mistake, the reason the BINDING contract test
lives in tools' own integration tree). Fixed both sides to mirror
tools' `ManifestV1_1` exactly (`:schema-version "1.1"`); verified
against tools' `sim-manifest-contract-test`, run as a subprocess, green.

**ADR-0012 (`:step-rejected`), landed alongside.** Decide-time
rejections (the cancel-family's own reinstatement guards, bed-swap/
merge's no-eligible-peer cases) now append a `:step-rejected` ground-
truth event — participants (the attempting patient only, never a
possibly-nonexistent `:with` target), the attempted step, and a
documented reason keyword (`ehr-testing-sim.engine/documented-step-
rejection-reasons`, its own catalog-style enum, co-landed with a
`step-rejected-reason-is-documented` invariant). No
`message-type-registry` entry, by design — truth about the run, never
wire traffic. Rejected steps consume no RNG beyond what `decide` had
already drawn before discovering the rejection.

**M3-adjacent capture, landed alongside: per-patient pathway
assignment.** `ehr-testing-sim.engine/run` gained `:pathways`
(`ehr-testing-sim.pathway/PathwaysConfig`): weighted-pool entries
(`{:pathway :weight}`, a sampled mixture across the patient population)
and/or explicit per-ordinal entries (`{:patient-ordinal :pathway}`, a
scripted assignment), `assign-pathway`'s own input — SimHospital's
`percentage_of_patients`-style analogue this project had discussed
without capturing until now. `run`'s existing single-`:pathway` config
is the degenerate case and stays byte-identical (the pinned fixture is
untouched — `:pathways` absent entirely, not merely all-zero, means no
new draw, exactly the same opt-in shape M2b's `:churn-profile` already
established). The M2b scripted-scenario fleet
(`ehr_testing_sim/churn_scenarios_test.clj`) now runs end-to-end through
`engine/run` via explicit assignments, computing peer patient-ids with
the pure `patient-id-for`; `engine-test`'s own
`bed-ready-transfer-scripted-two-patients` is kept as the one direct
decide/evolve-driven API-level regression, per this session's own
migration note. This is also **M5's own prerequisite**:
`CompileTrajectory` produces a *distinct* pathway per patient, so this
assignment layer is infrastructure M5 needs regardless of where it
landed first.

## M4 — Persona + demographics tables; payer sampling; PID/IN1 enrichment — **landed**

Lands **Persona** (`:planned` → `:built`) for real: demographics
sampling from vendored, hashed tables (`demographics-tables`, target
3), plus the `payer-pool` catalytic this session recorded as a
comment-only forward reference on `:persona` — this is the milestone
that turns that comment into a real `:catalytic` wire. PID gains
demographic fields; IN1 lands as a segment for the first time,
carrying the sampled payer (`docs/operational-models.md`'s payers
model). **Upstream-demand citation**
(`docs/research/SimHospital-Synthea-limitations-considered.md` §5.3):
SimHospital issue #3 is a user unable to find a way to include the IN1
insurance segment — this milestone is the answer. PID's demographic
enrichment also carries US phone formatting, named unavailable and
uneditable in SimHospital issue #21 — the same issue that requests
GT1/ZG1 (below, site-profiles' citation).

**The wiring fix, first (Task 0).** This session opened by fixing a
real gap the tools consumer loop surfaced: M3's `:pathways` reached
`ehr-testing-sim.engine/run` from a direct API caller (engine-test)
but never from `ehr-testing-sim.run/run-command` — CLI-invisible
despite 181 green tests and a demo. `ehr-testing-sim.engine/config-keys`
is now the canonical, documented list of every key `engine/run`
accepts; `run-command` forwards the full set (a plumbing-completeness
test asserts this with sentinel values, not just the known gap), and
`--config FILE.edn` is the passthrough vehicle for the data-heavy keys
(`:pathway`/`:pathways`/`:order-profiles`/`:churn-profile`) that have
no flag of their own. `AGENTS.md` gained a standing rule: demos and
verification commands run through the CLI surface (`clojure -M:cli
...`), never engine internals — the reason this class of gap is caught
by review next time, not just by a downstream consumer.

**Implementation shape, honestly recorded.** Persona's equation
(`sim-config → persona`) is satisfied by an engine-internal
`:registered` event `ehr-testing-sim.engine/run` prepends to every
patient's step queue (never authorable IR, the same treatment
`:result-followup` already gets) — folded into Execute's own
step-queue mechanism rather than a structurally separate pipeline
pass, since persona is needed at the same init moment Execute already
owns. `docs/sim-theory.edn`'s `:persona` stage records this in its own
`:contract` string rather than pretending the diagram and the code
agree on shape, the same discipline M2a/M3 already established for
their own engine-internal landings. `resources/demographics/` (given
names by sex+decade, surnames, places) are SMALL and HAND-CURATED, not
extracted from Synthea — no `../` checkout was available this session
(`resources/demographics/NOTICE` records the decision precisely).

**A real bug, caught by the fixed-consumption property test.** Persona's
own docstring first claimed 14 fixed RNG draws per persona; the
property test (`persona-consumes-a-fixed-number-of-rng-draws-regardless-of-content`,
a call-counting `java.util.Random` proxy, not a synthetic skip
sequence) caught the true count at 13 before this landed. A second,
more consequential bug surfaced the same way: `:registered` initially
carried no `:active-mrn`, which silently broke
`ehr-testing-sim.engine/replay`'s own bootstrap (it seeds a
never-yet-seen participant's initial state from the FIRST event naming
them) for every patient, surfacing as a real `merge-survivor-absorbs-
merged-mrns` invariant violation in the churned-run property test
before the fix — not a `:registered`-specific check, the EXISTING M2b
merge invariant, catching a gap in the NEW code. `:registered` now
carries `:active-mrn` like every other event type, for exactly this
reason.

Co-landing invariants: `registered-is-every-patients-first-event` and
`registered-persona-is-schema-valid` (structural — every persona
resource this run creates is schema-valid and is genuinely each
patient's first event, the fold-bootstrap correctness the merge-bug
above depended on); the age-linked payer property
(`persona-test/sixty-five-plus-personas-are-mostly-medicare` and its
under-65 converse) checks Medicare dominance at 65+ statistically,
since payer sampling is a weighted pool, not a hard per-event rule.
**PID/IN1 enrichment, landed alongside.** PID gains XPN name (family^given),
DOB (HL7 date), sex (Table 0001), XAD address, and phone, uniformly across
every message type (not admission-only); IN1 lands as a segment for the
first time, carrying the sampled payer's id/name, riding ONLY the admission
message (the real HL7v2 convention) — SimHospital issue #3's own request
(`docs/research/SimHospital-Synthea-limitations-considered.md` §5.3),
answered. GT1 stays site-profiles territory, as scoped.

**The ER7 escaping property, and a verified parser finding
(`notes/facts-register.md` F9).** `org.clojars.cmiles74/clojure-hl7-parser`
3.5.1 implements NO escape-sequence handling in either direction —
confirmed by reading its own shipped source (the read-side decoder exists
but its call sites are commented-out dead code; the write side never
encodes at all). `ehr-testing-sim.emit-hl7/escape-er7`/`unescape-er7` are
this repo's own documented workaround. The property test itself caught a
second, self-inflicted bug in the workaround's first draft: naive five-pass
sequential decoding let two adjacent encoded tokens spuriously spell a
third at their boundary (encoding `"|E|"` produces a string a five-pass
decode misreads); fixed to a single regex pass.

207 tests / 582 assertions green, coverage 95.91%/98.00% (up from the M3
baseline 95.35%/97.75%). The pinned fixture regenerated once (Persona's
`:registered` event, prepended to every patient's step queue, shifts the
entire downstream RNG stream — ADR-0009's own accept-and-record policy,
documented in the fixture's own header). `docs/demos/` seeded with two
CLI-produced traces (order/result post-Task-0, and Persona-enriched);
`docs/sim-theory.edn`'s diagram companion files updated for the new
`payer-pool` wire.

## Site profiles — code tables, MSH dialect, Z-segments — **landed**

Lands the config layer `docs/site-profiles.md` designed (M4's own
session): a `site-profile` value (`ehr-testing-sim.site-profile/
SiteProfile`, all keys optional) as a real catalytic on **EmitHL7**
(`docs/sim-theory.edn`, target 3) — no new stage, one new wire, the
same shape `order-profiles`/`provider-pool` joining `Execute` already
established. **The default-profile identity is this milestone's own
determinism anchor**, property-tested (`emit-hl7-test/default-profile-
is-the-absent-profile`): no profile arg, an explicit `nil`, and `{}`
all render identically. Ships an MSH dialect (version, sending/
receiving app+facility — **SimHospital issue #17's own citation**,
this roadmap's earlier scope note, answered: version selection is now
a configured field, not a hard-coded emitter constant); PV1-2/PV1-36
code-table overrides (rendering-time substitutions over the SAME
underlying state value, `ehr-testing-sim.site-profile/code-for`); and
a Z-segment template DSL (field→state/persona/event-path bindings,
rendered after standard segments on their own declared trigger,
escaped per ER7, an unbound path rendering empty rather than throwing
— **SimHospital issue #21's own citation**, answered for the custom-
segment scope item). The parser round-trip check this milestone's own
spec asked for (`emit-hl7-test/parser-round-trips-messages-bearing-an-
unknown-z-segment`) passed clean — `org.clojars.cmiles74/clojure-hl7-
parser` is segment-name-agnostic on read, so no facts-register finding
was needed here.

**The invariance property, proven both structurally and statistically.**
The milestone's own thesis — two site profiles over one seed produce
the same ground truth in two accents — is checked two ways: the strong
half (ground truth) structurally, since `:site-profile` is not a
member of `ehr-testing-sim.engine/config-keys` and is therefore
incapable of reaching `Execute` at all, not merely untested; the weak
half (messages) via a property test over 100 random seeds/patient
counts, comparing a default and a deliberately gaudy second profile
(HL7 2.5.1, a renamed sending facility, custom patient-class/
disposition codes, a `ZPI` payer Z-segment) after masking exactly the
declared dialect surfaces (MSH-3/4/5/6/12, PV1-2/PV1-36, Z-segment
lines) — the masking function itself is a deliverable, since it is the
precise, executable statement of what a dialect may touch and nothing
more. A CLI-produced two-profile demo (`docs/demos/site-profiles/`)
carries the same event rendered under both profiles side by side, with
ground-truth identity verified programmatically at generation time, not
merely asserted in prose.

**`:naming :surge-format` migrates to the profile — the one documented
exception.** Every other component binds at `EmitHL7`'s own render
call sites (Task 3's seam, `docs/site-profiles.md`); surge bed ids,
by contrast, are already baked into ground truth at DECIDE time
(`ehr-testing-sim.facility/surge-slot-ids`, pre-dating this milestone),
so `ehr-testing-sim.site-profile/apply-naming` is a facility-config
transform a caller applies BEFORE `engine/run`, never auto-wired into
it and never read by `EmitHL7` — a config-level compatibility shim,
not a fifth emit-time dialect surface, named here so it isn't
mistaken for one.

**`;; NEXT` stays on `RunModules`, Milestone M5, per the ratified
order** — this milestone added a catalytic wire to an already-`:built`
stage (`EmitHL7`), not a new stage of its own, so there was never a
marker to move; recorded here so a future session doesn't go looking
for one.

230 tests / 625 assertions green, coverage 95.90%/98.06% (flat against
the M4 baseline 95.91%/98.00% — a 0.01pp forms difference, well within
noise, no regression to justify). The pinned fixture
(`test/ehr_testing_sim/fixtures/pinned_seed_42_patients_5.edn`) is
untouched this session — `:site-profile` never reaches `engine/run`,
so there was no perturbation to regenerate against, the first
milestone since ADR-0009 was ratified where that's true by
construction rather than by luck.

**Note for the author: the tools-side gate-loop baseline review
(`ehr-testing-tools`' own consumer-loop Task 3, its own committed
verdict baseline artifact) is now genuinely due again.** Sim's default
(no-profile) message shape changed twice since that baseline was last
recorded — M3/M4's own deltas (order/result messages, PID/IN1
enrichment) plus this session's own default-path addition (PV1-36 now
present, blank, on every non-discharge message; populated on discharge
where before this field didn't exist at all). Re-run tools' Task 3
procedure when convenient to confirm the gate's verdict on the new
default shape and record any delta against the committed baseline —
noted here, not actioned, since that review lives in tools' own
repo/session per ADR-0001's dependency direction.

## M5 — GMF interpreter port + CompileTrajectory

**Split into M5a/M5b, per this session's own docs/ADR-prep pass
(`docs/gmf-interpreter.md`, ADR-0013) — the same "sequential,
shapes-then-content" precedent M2a/M2b already established** (M2a
landed the engine-refactor shapes churn's actual content would need;
M2b landed the content against those shapes). M5a lands the
interpreter itself, pure and engine-adjacent, against a fixture this
project controls end to end; M5b lands the part that actually touches
real upstream content and the emitter-facing consequences of a new
encounter class.

**Design captured ahead of either sub-milestone's build** (docs/ADR
session, 2026-07-26 — no code or resources landed with it).
[`docs/gmf-interpreter.md`](../../docs/gmf-interpreter.md) is the v1
design doc: the state-type/transition subset the interpreter executes
(deferring `CallSubmodule`/`Counter`/`MultiObservation`/`Death`, and,
per its own candidate-module survey, `Device`/`DeviceEnd` and
`CarePlanStart`/`CarePlanEnd`; `Symptom` joins v1 as a write-only
state), the history/horizon two-phase run per patient, the
GMF-encounter-class → ADT mapping (including the new
`:outpatient-visit` step pair), and the module-namespaced `:attributes`
discipline. [ADR-0013](../../notes/ADRs.md#adr-0013) resolves
`sim-theory.md`'s open question #1 — `gmf-module-set` vendors (target
3, a small curated hashed subset in `resources/modules/`) rather than
pins a lockfile (target 1) — the decision ADR-0003 deferred since
scaffold day, decided now that `docs/gmf-interpreter.md`'s survey of
three real Synthea modules gives it something concrete to be decided
against. **All eight of both documents' author-review recommendations
are ratified, 2026-07-26** (the `Symptom` addition, the history-phase
fast-forward granularity, the pre-horizon mark-don't-trim choice, the
outpatient-visit validity/invariant sketch — split into its own four
items, `docs/gmf-interpreter.md`'s own closing ratification record —
and `sinusitis.json` as the first module to vendor): M5a (below) is
built directly against this ratified design, not against a still-open
recommendation.

### M5a — the interpreter itself: state machine, history/horizon, hand-written fixture — **landed**

Lands **RunModules the LIBRARY** — `ehr-testing-sim.gmf` (the module
loader) and `ehr-testing-sim.gmf-interpreter` (`step`/`walk-module`/
`run-module`) — pure and engine-adjacent: the GMF state-machine walk
(`docs/gmf-interpreter.md` §1–3), the `PriorState`-as-log-query
compilation (§2 — no `engine.clj` signature change needed; the
interpreter's own accumulating trajectory stands in for `world`'s
`:ground-truth` mirror, per that section's own "same event shape, so
M5b swaps the source, not the logic" note), and the history/horizon
two-phase run per patient (§3, `run-module` — genuinely ONE continuous
walk marking events `:pre-horizon` by the pure predicate `t <
registration-t`, not two separately-driven passes). Tested end to end
against `test/ehr_testing_sim/fixtures/fixture-clinic.json`, the one
hand-written GMF-JSON fixture ADR-0013 point 6 names — this project's
own authored content, not vendored, so the interpreter's own red tests
depend on nothing outside this repo's control.

**`docs/sim-theory.edn`'s own `:trajectory` stage stays `:planned`,
corrected from this entry's own original scope note above (which had
read "Lands RunModules (:planned → :built)").** A library existing is
not the same as this repo's own run actually wiring persona → modules →
trajectory together — that wiring, and the flip to `:built`, is M5b's
job (`docs/gmf-interpreter.md` §7, "Implementation status," and
`sim-theory.edn`'s own updated `:contract` note, both added this
session). No real Synthea module content lands in M5a; no
`ground-truth-log`/`hl7v2-stream` consequence yet, since
`CompileTrajectory` (M5b) is what turns a trajectory into pathway IR
the engine can execute.

**Four implementation decisions, recorded honestly as this session's
own filling-in of what §1–§3 left unspecified, not silent divergence**
(`docs/gmf-interpreter.md` §7 has the full reasoning for each): (1) a
blocked Guard's own re-check mechanism — an analytic virtual-clock jump
for a failing `:age`/`>=` condition (zero rng draws), block otherwise;
(2) `Observation`'s own sampled value — one uniform draw within a
state's own `:range`, when present; (3) `EncounterEnd` references "the
most recently opened Encounter for this module," not a tracked open/
closed set (correct for every module this session's own fixture and
properties exercise; a future module with genuinely overlapping
encounters would need more); (4) virtual time is an interpreter-internal
`epoch-day` (`java.time.LocalDate`), not the engine's own seconds clock
— M5b's own mapping job.

Co-landing invariants/properties: code-passthrough (every coded concept
a trajectory event carries is verbatim from its source module, 150
trials green); glass-box traceability (every trajectory event cites a
real module id and state name, 150 trials green); attribute writes only
through the declared, module-namespaced registry (150 trials green);
determinism, both for a bare `walk-module` run and across the full
history/horizon `run-module` (150 trials each); the phase boundary is
exactly `t < registration-t`, the same pure-predicate shape ADR-0011's
own warm-up mark already established (150 trials green) — all checkable
against the hand-written fixture alone, ahead of any vendored content
existing.

**Task 0's ratification flips landed alongside, per this session's own
scope.** All eight author-review recommendations flagged across
`docs/gmf-interpreter.md` and its appendix (the `Symptom` addition, the
history-phase fast-forward granularity, the pre-horizon mark-don't-trim
choice, `sinusitis.json` as the first module to vendor, and the four
sub-items of the `:outpatient-visit` sketch — including item 6, the
`:location` nil exception, now scheduled to land M5b as a genuine
conditional row in `docs/patient-state-model.md`'s event-validity table
rather than staying prose-only) are ACCEPTED, 2026-07-26 — see that
document's own closing "Ratification record."

273 tests / 716 assertions green (up from the site-profiles baseline
230/625), coverage 96.00%/98.18% overall (up from 95.90%/98.06% —
`ehr-testing-sim.gmf` 98.69%/99.29%, `ehr-testing-sim.gmf-interpreter`
93.79%/98.26%). The pinned regression fixture
(`test/ehr_testing_sim/fixtures/pinned_seed_42_patients_5.edn`) is
untouched — M5a never reaches `engine.clj`'s own config surface, so
there was no perturbation to regenerate against, the same by-
construction guarantee the site-profiles milestone first established
for its own catalytic.

### M5b — CompileTrajectory + the first vendored module + outpatient steps

Lands **CompileTrajectory** (`:planned → :built`), the actual vendored
module (`docs/gmf-interpreter.md`'s appendix recommendation, ADR-0013's
curation criterion — `resources/modules/` + its `NOTICE` file, real
provenance this time, not this repo's own hand-curated content), and
the new IR step types M5a's trajectory events need somewhere to land:
`:procedure`, `:observation`, `:medication-order`/`:medication-end`,
and `:outpatient-visit`/`:outpatient-visit-end` (`docs/gmf-
interpreter.md` §4's own sketch — the new step pair also activates
`:class :outpatient`'s allocation-free path and the occupancy board's
scope qualifier, both named there for author ratification). `EmitHL7`
gains an A04 registry entry for `:outpatient-visit`;
`:outpatient-visit-end` deliberately gets none, the same "real
ground-truth event, no wire message" shape `:step-rejected` (ADR-0012)
already established. This is also where end-to-end validation against
a real vendored module actually happens — a hand-written fixture
(M5a) proves the interpreter's own mechanics; a real Synthea module
proves the whole pipeline against content this project didn't author.

Co-landing invariants: clinical-content-preserving compilation (every
trajectory event maps to at least one IR step, none dropped or
reordered against clinical causality — `docs/gmf-interpreter.md` §1's
table is the per-state-type mapping this invariant checks against);
appends-provenance (every compiled IR step cites the trajectory event
it realizes); `outpatient-visit-only-when-new`,
`outpatient-patients-occupy-no-bed`, and the occupancy board's
inpatient/ED-scoped consistency law (`docs/gmf-interpreter.md` §4).

**Medications, M5b note.** An OHDSI evaluation found Synthea's own
medication data unreliable without its separate Medication
Diversification Tool, citing limited model diversity as the root cause
(`docs/research/SimHospital-Synthea-limitations-considered.md` §4.3,
Wagner and Blacketer). Whenever medications land in this project
(M5b's `:medication-order`/`:medication-end` or later), single-source
distributions are expected to need the same kind of diversification
step — recorded now so this isn't relearned the hard way once real
medication content exists here.

**No hidden modules (`sim-theory.md`'s IR-transforms-as-composition-
layer note), governing both sub-milestones.** Synthea's built-in Java
lifecycle modules run always-on and invisibly, surprising users who
tried to run only their own custom module set (discussion #1126,
`docs/research/SimHospital-Synthea-limitations-considered.md` §4.2).
This project's own lifecycle behavior (birth, aging, death, whatever
the GMF port needs) must be an explicit, listable stage or IR→IR
transform — the same composition mechanism `InjectChurn` already
established, not a special-cased always-on pass a config author has no
way to see or disable. `docs/gmf-interpreter.md` §5's module-namespaced
`:attributes` discipline is this law's concrete extension into module
*data*, not just module *execution* — named here so a future session
reads both halves as one commitment, not two.

## M6 — EmitState (FHIR snapshots first); emitter-coherence property

Lands **EmitState** (`:planned`): state-document rendering from
`state-history`, FHIR resources before CDA. This is also the natural
point to resolve `sim-theory.md`'s open question #3 — whether
`state-history` is primitive or derived from `ground-truth-log` —
since EmitState existing is exactly the precondition that question's
own deferral names. **Upstream-demand citation**
(`docs/research/SimHospital-Synthea-limitations-considered.md` §5.3):
SimHospital issue #11, a 2024 user requesting FHIR output because
Synthea already had it and it was easier to use — the issue stayed
open through the project's archival, so this milestone answers a real,
still-unmet request rather than a hypothetical one.

Co-landing invariants: snapshot-at-instant (a state-document is a
pure function of `state-history` at a queried instant, no access to
the log, engine, or RNG); the cross-emitter **emitter-coherence**
property — replaying `hl7v2-stream` reconstructs `state-history`, and
a FHIR snapshot at instant *t* agrees with the state implied by
messages up to *t* — becomes a real property test for the first time
once there are two emitters to check against each other.

## Later / triggers

Not sequenced, because each is gated on a condition rather than on
the milestone before it:

- **Calibrate** (`:planned`, feedback stage) — waits on a real
  `sim-corpus` and a site's `feed-statistics` to calibrate against;
  premature before Package and a first external consumer exist.
- **CI + integration validation** (independent-parser round-tripping
  via NIST/HAPI, per `docs/third-party-sources.md` Tier 2) — triggers
  once this repo has a public GitHub remote (ADR-0003's existing
  trigger) or another CI-capable remote.
- **SETUP.md** — deferred trigger: owed before this repo has any user
  besides its own author, not written speculatively now. Installation
  friction is a first-class adoption risk for both mined upstreams —
  SimHospital's Bazel/Go build breakage across several issues, and an
  InterSystems team calling Synthea's JDK/Gradle setup a "nightmare"
  even after successfully using the tool
  (`docs/research/SimHospital-Synthea-limitations-considered.md` §5.2,
  §4.5). This repo's cold-start already fits in one documented command
  (`AGENTS.md`'s Quick start); SETUP.md exists to keep that true for
  someone who isn't this repo's author, not to add ceremony ahead of
  need.
- **Packs demotion** — `pack-push` stays the active session-end
  ceremony (ADR-0006) until this repo's GitHub remote goes *public*;
  demoting it to dormant, the way `ehr-testing-tools` did at its own
  ADR-0008, is recorded as its own ADR when that happens, not folded
  into this roadmap.
- **Site profiles** — **landed**, see its own section above (between
  M4 and M5). GT1/ZG1 (SimHospital issue #21's own second half — the
  same issue cited there for the Z-segment-template scope item) and
  version-driven segment *restructuring* (beyond the MSH-12 literal)
  stay future, per `docs/site-profiles.md`'s own honestly-updated
  today/future table.

## Consumer plan: sim doesn't validate itself in a vacuum

This roadmap's milestones describe what this repo builds; two items
describe how this repo's output gets exercised by consumers outside
it, named here so they aren't lost between repos.

- **Tools as first consumer.** An integration-tree item belongs **in
  `ehr-testing-tools`**, not here (ADR-0001's dependency direction: sim
  never depends on tools, but tools already may depend on sim): `ehr
  gate` judging a sim-generated corpus end to end, as a real exercise
  of tools' Gate machinery against this project's own traffic rather
  than only hand-crafted fixtures. This can share its test harness with
  the manifest contract test [`ADR-0001`](../../notes/ADRs.md#adr-0001)
  already assigns to tools' integration tree (the binding
  cross-repo contract tests live where both codebases share a
  classpath). **Noted here, built there** — this roadmap does not
  schedule tools' own work, only records the dependency so a future
  session in either repo knows the item exists.
- **Blaze as M6's ecological target.** `samply/blaze` (a Clojure FHIR
  server) is named as the natural first real-world consumer for M6's
  **EmitState** output (`docs/sim-theory.edn`) — a same-language FHIR
  server this project's state-documents can be loaded into and queried
  against, giving the emitter-coherence property test a genuine
  external system to check against rather than only this repo's own
  parsing round-trip. This is a target for M6's own validation work,
  not a dependency this repo takes on; recorded here so M6 doesn't have
  to rediscover the natural fit from scratch.

## The adversarial-traffic exclusion

**This simulator generates coherent truth; it deliberately never
generates delivery incoherence.** Out-of-order arrival, dropped
messages, malformed segments, and duplicate delivery are real
phenomena a downstream interface must survive — but they are failures
of a *transport* or *delivery* layer acting on a coherent stream, not
facts about the hospital the stream describes. This project's own laws
(ADR-0002's ground-truth-log primacy, the emitter-coherence law,
`InjectChurn`'s own IR-endomorphism and clinical-steps-preserved laws)
forbid the engine from ever emitting a stream that contradicts itself
— which means this engine structurally **cannot** be the place
out-of-order or dropped-message traffic comes from, on purpose, by the
same laws that make its output trustworthy in the first place.

That is exactly why this territory belongs to `ehr-testing-tools`'
`corpus mutate` instead, operating on a sim-generated corpus **after**
this project has produced it: sequence-reorder, segment-mangle, and
duplicate-delivery operators, each with recorded lineage back to the
coherent corpus it mutated. This isn't a gap sim leaves for tools to
fill reluctantly — it's the intended division of labor, now written
down as a reason rather than left to be inferred: sim's job is
producing ground truth a mutation operator can trust was coherent
*before* mutation, precisely because sim itself never introduces
incoherence as a side effect of its own generation.

## Deliberate exclusions

Recorded so they read as decisions, not gaps someone might otherwise
try to fill in:

- **Lifelong birth-to-death records.** This simulator's scope is
  hospital-operations traffic over an *encounter horizon* — one
  admission through its discharge and immediate churn — not a
  patient's full longitudinal history. Synthea already serves the
  longitudinal need well (that's precisely why it's mined as a
  tier-1 source rather than reimplemented); duplicating its scope
  here would be redundant, not additive.
- **CPT codes.** AMA-licensed; excluded by the standing constraint
  (`docs/problem-statement.md` §3, `AGENTS.md` Constraints), not a
  future milestone.
- **Delivery/transport** (file, stream, MLLP pacing). Below this
  theory's level of description — `sim-theory.md`'s own open question
  #5 already names this as deliberately absent unless paced emission
  ever acquires laws of its own.
