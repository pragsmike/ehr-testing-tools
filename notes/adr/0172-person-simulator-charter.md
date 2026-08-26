## ADR-0172 — arc 2: the person-simulator, chartered from the tree

**Status:** Accepted (design session 2026-08-25, HEAD `41081dd`;
**RULED 2026-08-25: A1 B1 C1 D1 E1 F1 G1** -- the recommendation on
every one of the seven). Payload session under the de-scaffold
moratorium; no component code lands with this ADR. **Clarified
2026-08-25 (arc 3a, ADR-0173):** `t0` in section 2's `initial-persona`
signature is the t0 CONTEXT map (`:master`/`:rng`, `:id-tag`,
`:death-t`, the persona config), not an instant -- ruling C1's "as a t0
parameter" read literally, which is what a two-argument function that
must DRAW requires. Section 5 records
each ruling where it lands, and arc 2b implements them: the component
lands ALONE, the engine does not call it (F1), so the corpus is
provably untouched.

Sibling to ADR-0162, deliberately and in the same shape: a mission
sentence at the front door, a dependency-direction paragraph, and one
table row per limitation declined ON PURPOSE, each with the gate that
goes red if the decline is silently lifted. ADR-0162's own lesson —
*a charter that is not gated is a charter that drifts* — is the reason
section 4's every row names a test rather than a promise.

### Context

`rulings.md#R-mix-1`..`#R-mix-4` charter a population process;
ADR-0168 section 4 names the component; the traffic-scale program plan
(`.agents/plans/2026-08-24-traffic-scale-program.md`, "Arc 2") sizes
it; `docs/dev/traffic-model.md` classifies its output as **skeleton**,
state-bearing, judged by the invariant catalog. ADR-0171 unblocked it:
the `:person` stream family exists with **zero draw sites**
(`engine.clj:309`), and `newborn-id-tag` (`engine.clj:366`) is exported
with no caller precisely so this arc inherits its key rather than
choosing one.

What the tree does today, in one sentence: **a Persona is sampled once,
at the patient's first sight, and never resampled.**
`decide :registered` (`engine.clj:493`) draws it from the `:patient`
stream — the method's own destructuring is `{rng :patient}`
(`engine.clj:484`) — attaches it to the `:registered` event
(`engine.clj:513`), and `evolve :registered` (`engine.clj:1179`) folds
it onto patient state with `(assoc patient :persona persona)`.
`engine.clj:182` states the contract in its own words: populated once,
*"never resampled after (the attribute-pool contract)"*. That single
line is what arc 2 supplies the alternative to and arc 3 replaces.

### 1. The census: every place demographics are read today

Thirty-one live read sites, `src` only, at `41081dd`. The last column is
the arc-3 question — whether a demographic timeline would change what
this site computes. **"t0-only"** means the site is correct against a
static Persona because it reads a genuinely immutable field (`:dob`,
`:sex`) or is definitionally a t0 construction; **"time-varying"** means
the site would read the wrong value the moment a delta lands and nothing
made it time-indexed.

#### sim-engine — the sampling and fold seam

| file:defn | Persona field | disposition |
|---|---|---|
| `engine.clj:493` `decide :registered` | whole (sampled) | **t0 seam.** Arc 2's hand-off point: this call becomes `initial-persona` |
| `engine.clj:513` `decide :registered`, event map | whole | t0-only; the event that carries the t0 state |
| `engine.clj:1179` `evolve :registered` | whole | **time-varying — the arc-3 fold seam.** The one function that would gain a per-delta sibling |
| `engine.clj:215` `PatientState` schema | whole, `[:maybe sim-model/Persona]` | time-varying; a timeline needs a shape this schema does not have |
| `event_schema.clj:279` `Event` `:registered` kind | whole, `[:persona sim-model/Persona]` | time-varying; every new person event kind widens the CLOSED 21-kind vocabulary (`event_schema.clj:263`) |

#### sim-check — the judged surface

| file:defn | Persona field | disposition |
|---|---|---|
| `check.clj:794` `registered-persona-is-schema-valid` | whole | t0-only today; a timeline owes a per-delta twin |
| `check.clj:781` `registered-is-every-patients-first-event` | none | **the ordering law every person event must respect** (see section 3) |

#### sim-emit-hl7 — the wire

| file:defn | Persona field | disposition |
|---|---|---|
| `emit_hl7.clj:254` `pid-segment` | `:name` `:dob` `:sex` `:address` `:phone` (PID-5/7/8/11/13) | **time-varying** — this is the A08/A31 surface |
| `emit_hl7.clj:237` `xpn-field` | `:name` | time-varying |
| `emit_hl7.clj:245` `xad-field` | `:address` | time-varying |
| `emit_hl7.clj:286` `in1-segment` | `:payer` (IN1), rides `:admission` alone | **time-varying** — the coverage-change surface |
| `emit_hl7.clj:302` `personas-by-patient-id` | whole | **time-varying, and the sharpest one.** A map keyed by patient-id ALONE, built once per `emit` call from the `:registered` events. Arc 3 must key it `(patient-id, t)` or the timeline never reaches the wire |
| `emit_hl7.clj:390` `context-for-event` | whole, into site-profile Z bindings | time-varying; reads its persona out of the map above |

#### sim-emit-fhir

| file:defn | Persona field | disposition |
|---|---|---|
| `emit_fhir.clj:114` `patient-resource` | `:name` `:sex` `:dob` `:address` `:phone` | time-varying |
| `emit_fhir.clj:194` `coverage-resource` | `:payer` | time-varying |

#### sim-emit-hl7 replay — the wire read BACK

| file:defn | Persona field | disposition |
|---|---|---|
| `v2_replay.clj:152` `parse-persona` | `:name` `:sex` `:dob` `:address` `:phone` off PID | **time-varying, and the inverse direction.** A replay fold that ignores a later A08 will show a stale address on the board |
| `v2_replay.clj:165` `parse-payer` | `:payer` off IN1 | time-varying |
| `v2_replay.clj:381` entry projection | persona subset; `:payer` gated on `admitted-at` | time-varying |

#### corpus / oracle

| file:defn | Persona field | disposition |
|---|---|---|
| `board.clj:49` `patient-name` | `:name` | time-varying (a rename must reach the bed board) |
| `digest.clj:126` `person` | whole, from a fixed `(Random. seed)` | t0-only **by construction** — a digest fixture, not a run |

#### patient-simulator — the consumer that reads persona AT A VIRTUAL TIME

This is the census's one genuinely interesting finding. Twelve sites
already read a persona *at a time* `t` inside a lifetime walk — they
just read it out of a static map. Most are safe because the field they
read is immutable; four are not.

| file:defn | Persona field | disposition |
|---|---|---|
| `gmf_interpreter.clj:112` `dob-epoch-day` | `:dob` | t0-only (immutable) |
| `gmf_interpreter.clj:172` `initial-context` | whole, into ctx at DOB | t0-only (constructs the walk's origin) |
| `gmf_interpreter.clj:222` `age-years-at` | `:dob` | t0-only |
| `gmf_interpreter.clj:226` `age-months-at` | `:dob` | t0-only |
| `gmf_interpreter.clj:258` `wellness-cadence-band` | `:dob` | t0-only |
| `gmf_interpreter.clj:269` `next-wellness-tick` | `:dob` | t0-only |
| `gmf_interpreter.clj:449` `age-condition-holds?` | `:dob` | t0-only |
| `gmf_interpreter.clj:453` `gender-condition-holds?` | `:sex` | t0-only |
| `gmf_interpreter.clj:489` `date-condition-holds?` | `:dob` | t0-only |
| `gmf_interpreter.clj:742` `race-condition-holds?` | `:race` | t0-only (immutable in this model) |
| `gmf_interpreter.clj:756` `socioeconomic-status-condition-holds?` | `:socioeconomic-category` | **time-varying** — employment moves it |
| `gmf_interpreter.clj:1005` `lookup-column-value`; `:1047` `lookup-table-row-matches?` | `:race` `:state` `:socioeconomic-category` | **`:state` and `:socioeconomic-category` time-varying** — a residence move changes `:state` |
| `gmf_interpreter.clj:2180` `run-module` | whole | t0-only (the walk's entry) |
| `census.clj:109` `default-persona-config`; `:364` | whole | t0-only by construction |

**What the census tells arc 3.** Nineteen of thirty-one sites are
time-varying. Six of those nineteen are in one component
(`sim-emit-hl7`) and five of the six read their persona from the single
map at `emit_hl7.clj:302` — so the emitter's whole exposure is **one
lookup shape**, not five rewrites. The patient-simulator's twelve
reduce to **two** (`:756`, and `:state` at `:1005`/`:1047`), and both
sit behind the config-gated optional fields that
`limitations.md`'s own "honest absence" row already declares. That is
the arc-3 fold sizing this section was asked for, and it is smaller
than the raw count suggests.

**Two premise mismatches against the prompt, disclosed rather than
absorbed** (`rulings.md#R-stop-only-on-two-defensible-readings`; each
is mechanical with one defensible reading — the tree wins):

- **There is no NK1 rendering.** The prompt's census asked for
  "emit-hl7's PID/IN1/NK1". `NK1` occurs in **no `src` file anywhere in
  the tree**. Its only live occurrences are a test's list of
  deliberately-unresolvable locator paths
  (`components/corpus/test/ehrt/corpus/locators_doc_test.clj:168`,
  beside `PID-99`) and four descriptive lines in a vendored research
  reference (`components/corpus/docs/research/
  HL7v2_ER7_MLLP_Reference.md`). No emitter writes an NK1 segment. **The
  household/next-of-kin work this arc charters therefore has no wire
  surface today at all** — which is a genuine finding for the
  limitations table (row 8), not a gap to fill in passing.
- **Provenance reads no demographics.** `:persona-config` is a run
  config key threaded into `init-world` (`engine.clj:1858`) and read at
  `engine.clj:493`; it is never stamped into a manifest, and
  `components/sim/src` contains no occurrence of "persona" at all. So
  the census has no provenance row, and a corpus's demographic
  configuration is **not** distinguishable on its artifact's face —
  unlike `:event-schema-version` and ADR-0171's `:stream-scheme`. Named
  here; not fixed here.

### 2. The front door

#### The mission sentence

> **The person process exists so that demographic and identity traffic
> is realistic; a person's life is relevant only inasmuch as it changes
> a message.**

Deliberately the same shape as `patient-simulator`'s own, and for the
same reason: it is the sentence that settles arguments about scope
before they start. It is why this component models a residence move
(one A08 and a changed PID-11) and does not model a commute; why it
models an employment change (a coverage change, an IN1, and an
occupational-injury hazard) and does not model a job title.

Under ADR-0162's precedent it lands **twice** — in
`components/person-simulator/docs/limitations.md` and in
`interface.clj`'s own SCOPE section — so a reader who never opens the
docs still meets it at the component's one public namespace, and a gate
asserts both copies verbatim.

#### Dependency direction

Identical to the sibling's, and load-bearing for ruling C. The engine
CONSUMES the person stream; the person process knows nothing of
encounters, beds, wards or messages. It depends on `sim-model`
(`Persona`, `places`, the payer pools) and `kernel`, and on
`sim-engine`'s stream-partition surface for `stream`/`newborn-id-tag`
only. **`components/person-simulator` must not require any other
`sim-engine` namespace, and no `sim-engine` namespace may require it in
v1** — that is limitations row 10's gate, and it is what makes "engine
-> person: none in v1" a structural fact rather than a discipline.

#### The interface

Mirroring `patient_simulator/interface.clj`'s shape — a thin re-export
namespace whose docstring opens with SCOPE:

```clojure
(persons config stream)                  ; -> [PersonEvent], t-ordered
(initial-persona person-id t0)           ; -> Persona   (the t0 state)
(initial-persona person-id t0 birth-ctx) ; -> Persona   (a newborn, ruling A)
```

`persons` takes the run's `:person`-family stream map and a config, and
returns a **timed, t-ascending vector of events** — data, never state.
The engine folds them; the component folds nothing. `initial-persona`
is the replacement for the `(sim-model/persona rng ...)` call at
`engine.clj:493`, and in v1 it *is* that call, so a wired arc 2b that
consumes no events is byte-identical to today (ruling F).

#### The event vocabulary

Fourteen kinds at arc 2b; **FIFTEEN since arc 3a** (ADR-0173 section
2(b) adds `:residence-loss`, the person-side half of the residence
sum). Every one carries `:person-id` and `:t`; the
`:reference` column is the field that names its antecedent, and section
3 states the invariant those fields must satisfy.

| kind | payload | reference |
|---|---|---|
| `:residence-move` | `:address` (a `places.edn` row), `:prior-address` -- ABSENT when the move is a RETURN to housing, because the prior state was nowhere and not a row | `:household-move-event-id` when propagated (ruling B) |
| `:residence-loss` (arc 3a) | `:prior-address` and **no `:address` at all**; `:at-t0` for a person who entered the run with no residence rather than losing one | — |
| `:employment-change` | `:status` in `{:employed :unemployed :retired :student}`, `:occupation-class` | — |
| `:coverage-change` | `:payer` (a `Payer`), `:prior-payer`, `:cause` in `{:employment :age-65 :loss :eligibility}` | `:employment-event-id` when `:cause` is `:employment` |
| `:identity-correction` | `:field` in `{:name :dob}`, `:value`, `:prior-value` | `:corrects-event-id`, or ABSENT when it corrects the t0 persona |
| `:household-form` | `:household-id`, `:head-person-id`, `:member-person-ids` | — |
| `:household-join` | `:household-id` | `:household-event-id` |
| `:household-leave` | `:household-id` | `:household-event-id` |
| `:pregnancy` | `:expected-delivery-t`, `:parity-index` | — |
| `:delivery` | `:newborn-person-id`, `:parity-index`, `:within-delivery-index` (pinned `0`) | `:pregnancy-event-id` |
| `:occupational-injury` | `:injury-class` | `:employment-event-id` |
| `:person-death` | `:cause` (optional) | — |
| `:identity-unavailable` | `:until-t`, `:alias-name` | — |
| `:identity-resolution` | `:branch` in `{:fill :merge}`, `:surviving-person-id` when `:merge` | `:unavailable-event-id` |
| `:person-registered` | `:persona` (the t0 state), for a person entering mid-run | `:delivery-event-id` for a newborn |

**Where `placeholder-register` / `fill-in-place` / `merge-with-existing`
went, and why** (ruling G). The prompt names those three as person
events. They cannot be, without breaking the direction this charter
just declared: a placeholder registration is conditioned on an
**arrival** — an unidentified patient presenting to an ED — and the
person process has no encounters to condition on. Emitting a
`:placeholder-register` at a drawn time would require the engine to
schedule an arrival to match it, which is person-driving-engine control
flow, strictly worse than the feedback edge v1 forbids.

So the person stream carries the **disposition** and the engine mints
the **wire-visible fact**: `:identity-unavailable` opens a window
(this person, from `:t` to `:until-t`, would present without usable
identification, under `:alias-name`), and `:identity-resolution` says
which branch R-mix-4's fork takes when it closes. The engine, at an
arrival landing inside an open window, mints the placeholder
registration; at the window's close it mints either the fill burst
(A08/A31) or the merge (A40, composing with `churn`'s existing
`:merge` into the post-merge-shadow surface
`traffic-model.md` names as the highest-value injectable class). Both
R-mix-4 branches are in scope, as ruled; only the seam moved, and it
moved to keep the direction one-way. This is ruling G because the
alternative is defensible and the prompt asked for the other shape.

#### Processes and their hazard rates

R-mix-1: bespoke hazard-rate processes, **never GMF modules**. Each
below is a rate per person-year.

> **Every rate in this section is AUTHORED, PROVISIONAL — a
> general-knowledge order of magnitude, with no table read and no
> source cited.** Stated plainly per ADR-0168's own error-ledger note:
> promoting these to fact without a named measurement would be the
> unearned-specificity class this channel's ledger tracks. Ruling E is
> what to do about it, and limitations row 9 is the gate that keeps
> them from becoming folklore.

| process | rate / person-year | conditioning |
|---|---|---|
| residence move | ~0.11 | age-tilted: highest 20-34, falling steeply after 55 |
| employment change | ~0.20 | working age only; a retirement hazard concentrated 62-67 |
| coverage change | mostly DERIVED, not drawn | follows employment change; **deterministic at age 65**, which the payer pools already encode (`persona.clj:78` vs `:89`) |
| name change | ~0.02 | adult only |
| identity correction | ~0.01 | **not a world rate** — an authored registrar-error knob, and honestly labelled as one |
| household form / join / leave | ~0.08 | adult only |
| pregnancy | ~0.055 | `:sex :female` and age 15-44 **only**; zero elsewhere |
| delivery | not a hazard | deterministic ~280 days after its `:pregnancy`, plus a gestation jitter draw |
| death | age-conditioned, Gompertz-shaped | ~0.0009 at 30, ~0.02 at 70, ~0.15 at 90 |
| occupational injury | ~0.028 | employed person-years only; zero otherwise |
| identity unavailable | ~0.004 | a defect-surface knob, not a world rate |
| residence loss (arc 3a) | ~0.006 | HOUSED person-years only, and only for a person in NO household (limitations row 13) |
| rehousing (arc 3a) | ~1.2 | UNHOUSED person-years only; read off the SAME variate the move hazard uses, so the return to housing costs no second draw |

**Fixed consumption applies unchanged.** Every hazard draws its
per-interval variate whether or not the event fires, and every branch
draws whether or not it is taken — the same law
`engine/assign-pathway`, `churn/roll-gap` and `sim-model/persona`'s own
13-draw contract already state. In particular ruling D's fill/merge
ratio is compared against a draw that happens either way, so the
consumption does not depend on the ratio's value.

#### Streams

Every draw comes from `(engine/stream master :person id-tag)`
(`engine.clj:354`), never from the `:patient`, `:world`, `:facility` or
`:emission` families. This is what makes arc 2b corpus-neutral: a
family with zero draw sites today cannot move a byte of any existing
corpus no matter how much it draws, and ruling F turns that into a
proof obligation rather than a hope.

- **Adults present at t0**: `id-tag` is the person's arrival ordinal —
  the same key `patient-id-for` (`engine.clj:262`) already uses, so a
  person who becomes a patient has one id in both families.
- **Newborns**: `id-tag` is
  `(engine/newborn-id-tag parent-id-tag parity-index 0)`
  (`engine.clj:366`), the pair ruling B1 of ADR-0171 fixed, with
  `within-delivery-index` pinned at `0` while multiples are a named
  limitation. **This arc adopts that key as given; it does not
  re-derive it.**
- **Household moves**: drawn ONCE from the HEAD of household's
  `:person` stream, per ruling B.

#### Clinical hooks

**person -> engine, exactly two**, as ADR-0168 section 4 ruled:

- `:occupational-injury` — the engine treats it as an arrival cause
  with an injury pathway, joining the ADR-0107 injuries family.
- `:delivery` — the engine treats it as an admission for the parent and
  as the newborn's **first encounter** (`traffic-model.md`: *"the
  newborn's first encounter is the birth"*), with the mother-baby link.

**engine -> person: none.** What that forbids, said outright:

- A GMF `:death` cannot end a person's residence, employment or
  coverage process. Ruling C is the resolution.
- An admission cannot delay a move, a job change or a delivery: a
  person can be modelled as moving house on a day they are an inpatient.
- A discharge disposition cannot become a residence fact (no
  discharge-to-SNF changing an address).
- An `:identity-unavailable` window cannot be *caused* by the clinical
  state that would really cause it (unconsciousness, trauma); it is
  drawn independently and the engine correlates it with an arrival.

#### The mortality collision (the seam ruling C decides)

Today a GMF `Death` state compiles to no death event kind at all:
`compile_trajectory.clj:326` `death->step` reuses `:discharge` with
`:disposition :expired` and cause-of-death `:codes` — the wire shape,
and a declared limitation of the sibling charter. A death whose
trajectory falls before registration is dropped outright
(`pre-horizon-dropped-types`, `compile_trajectory.clj:344`), and a
death outside any encounter compiles to nothing.

So the collision is concrete: the compiled trajectory is fixed at t0
(`engine.clj:493`, inside `decide :registered`) and may already contain
an expired discharge, while the person process draws its own death
hazard from a stream that knows nothing about it. Ruling C picks which
one is authoritative and what the engine sees.

### 3. The invariant shape every person event must satisfy

`check.clj`'s referential family — `medication-end-references-existing-
order-and-follows-it-in-time` (`check.clj:682`) and its care-plan twin
(`check.clj:740`), ADR-0163/0166 — is the shape, and it has three parts
this arc must reproduce exactly:

1. **The referent exists and is the right kind.** `:order-event-id`
   indexes into the same log and the target's `:event` must match.
2. **Same subject.** The target's participants must include the same
   patient. Person events are subject-scoped the same way; a household
   event names more than one person and every one of them is a
   participant, the shape `:bed-swap` and `:merge` already use.
3. **Follows in time.** `(> (:t target) (:t event))` is a violation.

And a fourth part that matters more here than anywhere: **the
pre-horizon escape.** A `:medication-end` whose order lies before the
horizon has no event to reference, and the invariant satisfies itself
against the `:registered` event's own `:pre-horizon-facts`, on the
argument that *"A pre-horizon fact carries no :t of its own, so the
follows-in-time law is satisfied by construction"* (`check.clj:690`).

**Person events inherit that escape verbatim**, and it is why
`initial-persona` returns state rather than an event: an
`:identity-correction` with no `:corrects-event-id` corrects the t0
persona, which is definitionally prior to every event in the log
because `:registered` is always a patient's first event
(`check.clj:781`). The reference is absent, not dangling — the same
distinction, with the same justification, already gated twice.

Consequently every person event kind in section 2 is either
referent-free or carries exactly one referential field, and arc 3's
check obligations are one invariant per referential field in that
table, each a rename of an existing function body.

### 4. Deliberate limitations, and the gate for each

The table below is what lands in
`components/person-simulator/docs/limitations.md`, gated by
`ehrt.docs-tooling.person-simulator-charter-test` on the ADR-0162
pattern: the mission sentence verbatim in two places, every citation
resolving by anchored TEXT and never by line number, and every
limitation marker in this component's own `src` covered by a row.
ADR-0162's own hard-won correction applies from the start — **a
citation must anchor exactly one place in its file**, or a marker that
quotes its own citation blesses itself.

The last column is what this session was asked for: **the test that
goes red if the limitation is silently lifted.** Each is a test that
can be born red, which is the only kind worth writing.

| # | Limitation | The gate |
|---|---|---|
| 1 | **Twins and multiples are excluded.** One `:pregnancy` yields one `:delivery` yields one newborn. The key is already reserved: `newborn-id-tag`'s `within-delivery-index` (`engine.clj:366`). | `every-delivery-is-a-singleton-test` — every `:delivery` carries exactly one `:newborn-person-id` and `:within-delivery-index` `0`. A second newborn on any delivery is red. |
| 2 | **Immigration and emigration are excluded.** The population is CLOSED: no person enters except by birth, none leaves except by death. | `the-person-population-is-closed-test` — every `:person-id` appearing in the stream is either in the t0 population or is the `:newborn-person-id` of a `:delivery` in the same stream. The shape of `participant-ids-exist-in-run` (`check.clj:85`). |
| 3 | **Foster placement and adoption are excluded.** Household membership is birth- or cohabitation-derived only. | `minors-join-households-only-by-birth-or-formation-test` — a `:household-join` for a person under 18 references a `:delivery` or a `:household-form`. |
| 4 | **A death outside care mints no wire event.** `:person-death` stops the person's other processes and nothing else. The only death that reaches a message is the GMF one's expired discharge. | `person-death-emits-no-ground-truth-event-test` — folding a stream containing `:person-death` leaves the count of `:discharge` events with `:disposition :expired` unchanged. |
| 5 | **A legal name change and a data-entry correction are collapsed.** Both are `:identity-correction`; only real HL7v2 practice distinguishes them (A08 vs A31 usage), and v1 does not. | `identity-correction-carries-no-cause-test` — `:field` is a closed set `{:name :dob}` and no `:cause` key exists. Red the day the distinction is added without a row. |
| 6 | **Demographics reach the wire through ONE per-run lookup.** `personas-by-patient-id` (`emit_hl7.clj:302`) is keyed by patient-id alone. Until arc 3 re-keys it, a delta folded onto patient state is invisible to every message. | `personas-are-keyed-by-patient-id-alone-test` — asserts the map's key shape. Red the day it becomes `(patient-id, t)`, which is exactly when the row should be struck. |
| 7 | **Geography stays the 24-row `places.edn` pool** (R-mix-3). A residence move draws a whole new row from the same flat weighted pool: no adjacency, no distance, no local-versus-cross-country move. | `every-residence-address-is-a-places-row-test` — every `:residence-move` `:address` is a member of `sim-model/places` (`persona.clj:64`). A synthesized address is red. Population asserted non-empty first (`rulings.md#R-empty-population-is-red`). |
| 8 | **Household structure has no wire surface.** No emitter writes an NK1 segment; `NK1` occurs in no `src` file anywhere in the tree. Households exist to correlate moves and coverage, not to be rendered. | `no-emitter-writes-nk1-test` — the emitter's segment vocabulary contains no NK1. Red the day one is added, which is the day households owe a rendering row. |
| 9 | **Every hazard rate is authored-provisional.** No cited table stands behind any number in section 2. | `every-provisional-rate-is-tabled-test` — each rate constant in `src` carries a `PROVISIONAL` marker and every marker is covered by a citation into its own comment block. The ADR-0162 drift mechanism, with a fourth token. |
| 10 | **The engine tells the person process nothing.** No feedback edge in v1; the four consequences are named in section 2. | `person-simulator-requires-no-engine-namespace-test` — the component's `ns` forms name no `sim-engine` namespace but the stream-partition surface, and no `sim-engine` namespace requires `person-simulator`. A structural fact, not a discipline. |
| 11 | **Every pregnancy reaches a delivery.** No loss, no termination, no non-delivery outcome. | `pregnancy-and-delivery-are-in-bijection-test` — per person, `:pregnancy` and `:delivery` counts are equal and each delivery's `:pregnancy-event-id` is distinct. |
| 12 | **A parent may head more than one household.** A parent with no household at their delivery gets one constituted BY the birth; if their own household hazard fires later, they head TWO. | `a-parent-may-head-more-than-one-household-test` -- red the day the artefact is fixed, which is the day this row should be struck. Three such parents in the component's own witness population, pinned, with `pos?` asserted separately so the gate cannot pass by going empty. |
| 13 | **A household never loses its housing.** Only a person in NO household becomes unhoused, and an unhoused person forms or joins none. | `only-household-less-persons-become-unhoused-test` -- every `:residence-loss` belongs to a person outside any household at that instant, and no household transition is minted for a person unhoused at that instant. Ruling A1's newborn, delivered to a parent unhoused at that instant, is the one exception and is named as such. |

Row 13 arrived with arc 3a and was NOT anticipated here: ADR-0173
section 2(b) designed `:residence-loss` as a person-side kind and said
nothing about households, and the tree answered that ruling B1's
propagation pass copies a head's move to every member verbatim -- so a
member who could lose housing on their own would receive copies
reporting a change they never had. Coupling the two is what keeps that
copy honest. Recorded here rather than absorbed
(`rulings.md#R-stop-only-on-two-defensible-readings`: one defensible
reading, so the tree wins and this is the record).

Row 6 is the one to read twice. It is a limitation of the **engine and
emitter**, not of this component, and it is tabled here because this
component's whole output is invisible until it is lifted. A charter that
tabled only its own gaps would let arc 2b ship a stream nothing reads
and call it done.

### 5. Rulings needed

Lettered, with a recommendation on each. **All seven were ruled by the
author on 2026-08-25, and every one took the recommendation: A1 B1 C1
D1 E1 F1 G1.** Each ruling is quoted below at the option it selected;
the rejected options are kept verbatim, unstruck, because what was
declined is the reason the selection means anything.

**A. Are newborns full persons from birth?**
**RULED A1** (author, 2026-08-25): *"a newborn is a full person with its own `:person` stream, its own Persona, and its own ADT."* Arc 2b derives the newborn Persona from the household and draws fewer than 13.

* **A1 — yes: a newborn is a full person with its own `:person` stream, its own Persona, and its own ADT. RECOMMENDED** (and the channel's lean). `traffic-model.md` already classifies the mother-baby link and the newborn's first encounter as skeleton, and ADR-0171 already minted the key. The design consequence to accept: a newborn's Persona is **derived, not sampled** — surname and address from the household, `:dob` from the delivery `:t`, `:payer` from the parent's current coverage — so `initial-persona` needs the three-arity in section 2, and the newborn's own stream draws only `:sex` and the fields the household does not determine. Fewer than 13 draws, deliberately: a newborn is not a sampled adult.
* A2 — a newborn is an attribute of `:delivery` and not a person. Cheaper, and it forbids the mother-baby link, the newborn's own ADT and every downstream birth-traffic defect surface — i.e. it declines the reason the delivery hook exists.

**B. Household propagation: whose stream draws a family move?**
**RULED B1** (author, 2026-08-25): *"the HEAD of household's stream draws once; every member gets a `:residence-move` referencing the head's."*

* **B1 — the HEAD of household's stream draws once; every member gets a `:residence-move` referencing the head's. RECOMMENDED.** One draw per household move regardless of household size, so a member joining or leaving does not change the head's draw sequence, and a non-member's stream is untouched. The cost, stated: a member's address becomes a function of another person's stream — the same coupling ADR-0171 section 2(a) named for the WORLD family, but confined to one household and keyed on a stable id rather than on population state.
* B2 — every member draws their own move from their own stream, correlated by a shared household hazard. Keeps each person's address a function of their own stream; makes a household move non-atomic (members can disagree) unless the head's variate is threaded in as a parameter anyway, at which point B1 is the same design with more draws.
* B3 — a sixth `:household` stream family. Rejected: ADR-0171 fixed five families and a sixth is a scheme change owing its own `:stream-scheme` bump and migration, to buy a key the head's id already provides.

**C. Mortality: person-process death versus GMF `:death`.**
**RULED C1** (author, 2026-08-25): *"the GMF death is authoritative for anything wire-visible, always."* `initial-persona` takes the compiled death instant as a t0 PARAMETER -- as data, never a require on `patient-simulator`.

* **C1 — the GMF death is authoritative for anything wire-visible, always. RECOMMENDED.** `initial-persona` receives the compiled trajectory's death instant, if any, as a **t0 parameter** — not a runtime feedback edge, since the compile and the persona construction already happen in the same instant at `engine.clj:493`. The person process then draws its death hazard as always (fixed consumption) and **discards its own draw for any person whose trajectory carries a death**, truncating its other hazards at that instant instead. `:person-death` is minted only for persons with no compiled death: those who never became patients, and those whose trajectory has none. What the engine sees: exactly what it sees today — a `:discharge` with `:disposition :expired` — plus, for the second class, a person whose processes simply stop. **Cost: none to any corpus**; no death fixture moves, which matters because the oracle's rung-3 capacity coverage is entirely death-fixture.
* C2 — the person process wins: its death suppresses the GMF `:death` step. Blanks or reshuffles every death fixture in the gated corpora and the oracle's only capacity-pressure coverage, to buy a death v1 can render no differently.
* C3 — earliest wins, both processes live. Requires knowing at draw time which is earlier, i.e. the runtime engine->person edge v1 forbids. Available only if that ban is lifted, which is a different ruling.

**D. Fill-versus-merge weighting (R-mix-4).**
**RULED D1** (author, 2026-08-25): *"a config ratio with an authored default: `:identification {:merge-fraction 0.35}`."*

* **D1 — a config ratio with an authored default: `:identification {:merge-fraction 0.35}`. RECOMMENDED.** The merge branch is what composes with `churn`'s `:merge` into the post-merge-shadow surface `traffic-model.md` calls the highest-value injectable class for MPI-consumer testing, and a tester generating a corpus to exercise exactly that needs to dial it up. Consumption is unaffected: one draw, compared against the threshold, whichever branch is taken. The default value is authored-provisional and inherits limitations row 9's marker.
* D2 — a fixed constant in `src`. One fewer config key; makes the corpus a tester most wants unreachable without a code change.

**E. Hazard-rate sources: authored-provisional now, or sourced before 2b?**
**RULED E1** (author, 2026-08-25): *"land 2b with authored-provisional rates, each carrying an in-source `PROVISIONAL` marker covered by limitations row 9."*

* **E1 — land 2b with authored-provisional rates, each carrying an in-source `PROVISIONAL` marker covered by limitations row 9. RECOMMENDED.** The mission sentence is the argument: these rates exist to make traffic realistic, and traffic realism is insensitive to whether the move rate is 0.11 or 0.13, while it is very sensitive to whether moves happen at all. The marker-and-row mechanism means an unsourced rate stays visible forever instead of silently becoming folklore — which is precisely the ADR-0170 species this repo has already been bitten by: a claim true when written that nothing keeps true.
* E2 — source the rates before 2b lands. Buys real numbers; costs a research session ahead of a component that would otherwise be corpus-neutral and shippable, and licenses nothing that E1 forbids later (a sourced rate replacing a provisional one is a draw-affecting change only after arc 3 folds the stream, which is exactly when it is cheapest).

**F. Does arc 2b land the component alone, or with arc 3's first fold hook?**
**RULED F1** (author, 2026-08-25): *"the component alone; the engine does not call it."* This is arc 2b's proof obligation, not merely its shape: `bin/regression-oracle` must report IDENTICAL with no declaration.

* **F1 — the component alone; the engine does not call it. RECOMMENDED, and provable.** The `:person` family has zero draw sites (`engine.clj:309`), so a component drawing only from it moves nothing — and with the engine not calling it at all, `make test` and `bin/regression-oracle` must be **IDENTICAL**, the equivalence-proof shape ADR-0169 established for a whole arc. That turns 2b's green into evidence rather than an absence of red, and it leaves arc 3's first fold as the single change whose reshuffle is expected and can therefore be judged.
* F2 — 2b lands the component plus arc 3's first fold hook. One fewer session boundary; forfeits the identical-corpus proof, and mixes a new component's own defects with a deliberate reshuffle in one commit, so a surprise in the diff has two possible causes.

**G. Where the identification flow's three wire events are minted.**
**RULED G1** (author, 2026-08-25): *"person-side disposition, engine-side minting."* Arc 2b mints `:identity-unavailable` and `:identity-resolution` only; no placeholder, fill or merge event is minted in this component.

* **G1 — person-side disposition, engine-side minting** (section 2's `:identity-unavailable` / `:identity-resolution`, with the engine minting placeholder-register, fill-in-place and merge-with-existing). **RECOMMENDED.** It is the only shape that keeps person->engine one-way, because a placeholder registration is conditioned on an arrival the person process cannot see. Both R-mix-4 branches stay in scope, as ruled; only the seam moved.
* G2 — the person stream emits all three directly, and the engine schedules an arrival to match. Matches the prompt's literal vocabulary; makes the person process drive engine control flow, which is strictly stronger than the feedback edge v1 forbids, and makes an arrival's existence depend on a `:person`-family draw.

### Consequences

* Arc 2b has a front door, a closed event vocabulary, a stream key it
  inherits rather than chooses, and eleven limitations each with a test
  that can be born red. This ADR still carries no code; arc 2b's own
  commits carry it, under the seven rulings above.
* Arc 3's fold work is sized: nineteen time-varying read sites, of
  which the emitter's six collapse to one lookup shape and the
  patient-simulator's twelve collapse to two.
* `rulings.md` is FROZEN (de-scaffold ruling, 2026-08-25). Nothing here
  becomes a rulings row; section 4's every law lands as a **gate** in
  arc 2b's own commit or not at all — the same disposition ADR-0171
  took.
* The two premise mismatches in section 1 (no NK1 surface; no
  provenance record of `:persona-config`) are recorded and **not
  fixed**: this session's fence forbids touching `components/*/src`.
  The first has a limitations row; the second has neither a row nor a
  gate, deliberately, because it is a provenance question and not this
  component's.
