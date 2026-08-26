## ADR-0173 — arc 3a: the engine folds the person stream (the demographic half)

**Status:** Accepted (design session 2026-08-25, HEAD `667d1a0`;
**RULED 2026-08-25: A1 B1 C1 D1 E1** -- the recommendation on every
one of the five, and `:residence-loss` lands FIRST, before the fold).
The design session was a payload session under the de-scaffold
moratorium and no engine code landed with this ADR; the fold itself
lands in the execution session that follows, which is where section
2's every subsection becomes code. Arc 3's roadmap row bundles three folds -- the
demographic timeline, scheduling state (`rulings.md#R-mix-5`) and the
bed-status cycle (`R-mix-6`). **This ADR designs the FIRST only**, plus
the two clinical hooks and the identification flow. Scheduling and
bed-status are arc 3b and are not touched here.

Binding on this design, author statement 2026-08-25, verbatim: *"our
population does include unhoused people showing up at, say, ED; and
unhoused unresponsive John Does."* Section 2(d) is where it lands, and
ruling E is the one question it leaves open.

### Context

`components/person-simulator` exists and **nothing calls it**
(ADR-0172, ruling F1; proven, not asserted -- `bin/regression-oracle`
reported IDENTICAL with no declaration). ADR-0171 partitioned the RNG
into five families, of which `:person` has zero draw sites. Arc 3 is
what makes the component's output reach a message, and it is the first
EXPECTED reshuffle since ADR-0171's own.

Two facts about the tree shape everything below, and both were probed
rather than assumed:

* **A Persona is sampled once, at the patient's first sight, and never
  resampled** -- `decide :registered` (`engine.clj:493`) draws 13
  variates from the `:patient` family and `evolve :registered`
  (`engine.clj:1179`) folds the result on with `(assoc patient
  :persona persona)`.
* **A patient id is minted from an arrival ORDINAL** --
  `patient-id-for` (`engine.clj:262`) is `(format "PID-%06d-%08x"
  ordinal (bit-and (mix64 seed ordinal) 0xffffffff))`, and `mrn-for`
  (`engine.clj:1807`) is `(format "MRN%06d" (inc i))` on the same
  ordinal. **Nothing in the tree can make a returning PERSON resolve
  to the same patient**, because identity is a function of the arrival
  and of nothing else. Section 2(a) is the answer.

### 1. The census, re-derived from the tree

ADR-0172 section 1 tabled thirty-one live `src` read sites at
`41081dd`. **All thirty-one still resolve line-exact**, checked one by
one at `667d1a0`: `git diff --stat 41081dd..ee573c4 -- .
':(exclude)components/person-simulator'` touches no `src` file at all
(15 files, all docs, registers, `deps.edn`, `workspace.edn` and one
docs-tooling gate). So the census is inherited whole rather than
rebuilt, and this section only RETAGS it and adds what the arc-3
question needs.

#### The retag: t0-only / state-at-event / state-at-render

ADR-0172's binary tag (t0-only vs time-varying) was the right tag for
SIZING the fold. It is the wrong tag for BUILDING it, because the
nineteen "time-varying" sites split into two groups that need entirely
different work:

* **state-at-event** -- the site renders or judges one event and needs
  the demographics as they stood AT THAT EVENT'S `:t`. Its input is
  the LOG. It needs a re-key, and it is the only group that does.
* **state-at-render** -- the site reads one patient's folded state and
  renders from it. Its input is `state-history` / `PatientState`.
  **It needs nothing**: fold a delta onto patient state and the site
  picks it up for free.

| group | sites | what arc 3a owes it |
|---|---|---|
| t0-only | 14: `engine.clj:513`; `check.clj:794`; `digest.clj:126`; `gmf_interpreter.clj:112/172/222/226/258/269/449/453/489/742/2180`; `census.clj:109`/`:364` | nothing (`:dob`/`:sex`/`:race` are immutable; the rest are t0 constructions) |
| state-at-event | `emit_hl7.clj:302` `personas-by-patient-id`, and the five sites reading out of it (`:237`, `:245`, `:254`, `:286`, `:390`) | the ONE re-key, section 2(b) |
| state-at-render | `emit_fhir.clj:114`/`:194`; `v2_replay.clj:381`; `board.clj:49` | nothing, once the fold lands |
| the fold seam itself | `engine.clj:493`, `engine.clj:1179`, `engine.clj:215`, `event_schema.clj:279` | the work of section 2(b) |
| the ordering law | `check.clj:781` `registered-is-every-patients-first-event` | must survive every new event kind (section 2(e)) |
| still time-varying, still declined | `gmf_interpreter.clj:756`, `:1005`/`:1047` (`:socioeconomic-category`, `:state`) | nothing: both sit behind config-gated optional Persona fields, and a walk that reads a t0 value is correct BY CONSTRUCTION once section 2(b) fixes the walk at t0 |

**Three refinements the tree forced, disclosed rather than absorbed**
(`rulings.md#R-stop-only-on-two-defensible-readings`; each is
mechanical with one defensible reading, so the tree wins and this is
the record):

1. **`emit_fhir` and the v2-replay projection read PATIENT STATE, not
   the log.** `patient-resource` (`emit_fhir.clj:115`) destructures
   `{:keys [patient-id active-mrn persona]}` off a folded state map,
   and `v2_replay.clj:381` does the same. ADR-0172 tagged both
   "time-varying" without distinguishing the SOURCE, which is the
   distinction that decides whether they need work. They do not.
2. **The emitter's exposure is one lookup shape and TWELVE threading
   sites.** ADR-0172 counted six READ sites, correctly. The edit
   surface is larger and shallower: `personas` is a parameter threaded
   through `emit` (`emit_hl7.clj:940`), `emit-with-offsets` (`:1016`),
   `event->messages` (`:889`/`:891`) and seven message builders
   (`:481`, `:514`, `:541`, `:692`, `:724`, `:818`, `:852`). Twelve
   signatures move; one lookup changes meaning. That is a cheaper
   change than "six rewrites" and a wider one than "one map".
3. **Provenance stamps no demographic configuration, verified.**
   ADR-0172 named this without a proof line. Here it is:
   `ehrt.sim.run` builds `engine-params` as `(select-keys opts
   [:patients :arrival-gap :warm-up-seconds])` plus
   `:reference-date`/`:utc-offset` (`run.clj:391`), and
   `manifest/build` stamps that map verbatim (`manifest.clj:99`). So
   `:persona-config` reaches the engine and never reaches the
   artifact's face. Section 2(f) fixes it for `:persons` and for
   `:persona-config` together, because a fold whose configuration is
   invisible is the ADR-0170 species again.

Three files carrying the token `persona` were checked and are NOT read
sites, confirming ADR-0172's exclusion of them:
`site_profile.clj:155` (a docstring naming the binding path),
`patient_simulator/interface.clj:50-68` (arity pass-throughs), and
`sim_model/interface.clj:51-54` (the re-export of the definition
itself).

#### What the `:patient` family loses

The seam is one call: `(sim-model/persona rng (:persona-config
world))` at `engine.clj:493`, with `rng` destructured as `{rng
:patient}` at `:484`. `sim-model/persona`'s own contract
(`persona.clj:217`) is **13 draws, always, in this exact order**: sex;
age; birth month; birth day; given name; surname; address; phone area;
phone exchange; phone subscriber; SSN group; SSN serial; payer. Plus
**three config-gated** draws when the config supplies the pools --
race, socioeconomic category, `:state` -- so **16 for a run that
supplies demographic weights**, which the gated corpora do not.

When that call moves to the `:person` family, every `:patient`-family
draw that FOLLOWS it shifts by 13 positions. The complete list of what
follows, so the reshuffle is predicted rather than discovered --
exactly three decide methods draw from `:patient`, and nothing else in
the engine does:

| site | draws | position relative to the seam |
|---|---|---|
| `engine.clj:1813` `assign-pathway` | 1 per patient, only with `:pathways` | BEFORE (pre-loop) -- unmoved |
| `engine.clj:1835` `assign-module` | 1 per patient, only with `:module-assignment` | BEFORE (pre-loop) -- unmoved |
| `engine.clj:1819` `churn/inject` | 6 per gap, only with `:churn-profile` | BEFORE (pre-loop) -- unmoved |
| `engine.clj:493` `decide :registered`, the persona | **13 (16)** | **THE SEAM -- leaves the family** |
| `engine.clj:499` `decide :registered`, `run-module` | unbounded, module-dependent | AFTER -- shifts by 13 |
| `engine.clj:573` `decide :delay` | 1 per step, and ZERO when `:from` = `:to` (ADR-0171 section 2(d)) | AFTER -- shifts by 13 |
| `engine.clj:866` `decide :order` | profile-dependent | AFTER -- shifts by 13 |

So the predicted blast radius is: **every module-walking patient's
whole trajectory, and every non-degenerate `:delay` and `:order` in
every pathway.** A corpus with no modules, no `:pathways`, no
`:order` and only `:from` = `:to` delays would be byte-identical --
and none of the gated corpora is that corpus. Sweeping for
knife-edge fixtures BEFORE the reshuffle is therefore mandatory, the
lesson ADR-0171 paid for once and recorded in
`.agents/session-records/2026-08-25-arc-1b-stream-partition-migration.md`:
a fixture standing on a knife edge is emptied by a reshuffle silently,
and a gate over an empty fixture agrees with everything. The four thinnest are already named by arc 2b's own record:
`:identity-unavailable`, `:identity-resolution` and
`:occupational-injury` at 5 each in the person witness, and the
oracle's rung-3 capacity coverage, which is entirely death-fixture and
which ruling C1 was chosen to protect.

### 2. The design

#### (a) The population, and how a person becomes a patient

`run`'s config gains ONE key, `:persons`, and it joins `config-keys`
(`engine.clj:1582`) in the same change that teaches `run` to read it,
as that def's own docstring requires:

```clojure
:persons {:count       n           ; pool size; ABSENT ENTIRELY means no persons
          :years       y           ; horizon in whole years (person-simulator's own)
          :identification {:merge-fraction 0.35}   ; ruling D1 of ADR-0172
          :unhoused    {:t0-fraction 0.02}}        ; section (d)
```

**Absent entirely -- not merely nil, not `:count 0` -- is the
byte-identical path**, the same opt-in law `:pathways`,
`:churn-profile` and `:module-assignment` already establish and the
same one ruling D below is about.

**`run` does not require `person-simulator`, and cannot.**
`components/person-simulator` depends on `components/sim-engine` for
`stream` and `newborn-id-tag`; the reverse edge would be a cycle, and
`clojure -M:poly check` would refuse it. It would also turn
ADR-0172's limitations row 10 red -- *"no `sim-engine` namespace
requires `person-simulator`"*, a gate this arc must keep green because
its FIRST half (the one-way edge) is the structural fact the whole
charter rests on.

So the person events reach the engine as **data**, exactly the way
`:modules` already does: *"this namespace does no file I/O of its own,
`ehrt.sim.run`'s job, the same layering
`:facility`/`:providers`/`:order-profiles` already follow"*
(`engine.clj`'s own `run` docstring). `ehrt.sim.run` -- which may
require both -- calls `person-simulator/persons` and hands `run` the
result. The engine folds a vector of maps and never learns whose they
are. Row 10 stays green verbatim, both halves; F1's "nothing calls it"
is lifted by a caller that is not a `sim-engine` namespace.

**Arrival selection.** Ruling A. The recommendation: `run` draws, per
arrival ordinal and in ordinal order, ONE uniform from the `:world`
stream to pick that arrival's person from the pool. `:world` is the
family whose own definition names this draw -- *"arrivals, and every
cross-patient decision"* (`stream-family-tag`, `engine.clj:298`) --
and the candidate set is the pool, which is shared, so no per-patient
stream can own it.

The candidate set is **persons alive at that arrival instant**, the
same filtered-eligibles shape `decide :merge` (`engine.clj:837`)
already uses. One draw either way, whether or not the filter removed
anyone: fixed consumption. Without the filter, an arrival could land
on a person whose own `:person-death` already fired, which is a defect
with a wire face (a registration for someone the ground truth says is
dead).

**Repeat arrivals, and the fold index.** A person selected twice is
the point of having a pool. The engine carries

```
:person-index {person-id {:patient-id .. :first-ordinal .. :active-mrn ..
                          :placeholders #{patient-id ...}}}
```

in `world`, the carried-state precedent `:reinstate-index` and
`:citation-index` set (`engine.clj:1882`/`:1889`, ADR-0169): written
inside the same pass that produces `world'`, read by `decide`.

* **First arrival mints.** `patient-id-for seed ordinal` and
  `mrn-for ordinal` on THAT ordinal, unchanged, so a run whose pool is
  larger than its arrival count mints exactly the ids it mints today.
* **A later arrival resolves.** The queue entry carries the ALREADY
  MINTED patient-id; `:registered` is NOT prepended (it is prepended
  only on a first arrival), so `registered-is-every-patients-first-
  event` (`check.clj:781`) stays true by construction rather than by
  luck. The second encounter's steps simply continue that patient's
  log.
* **One `:patient` stream per PATIENT, keyed by the first arrival
  ordinal.** A patient's draws are one continuing sequence -- `run`'s
  own reason for building `patient-rngs` up front -- and a second
  stream for a second encounter would break that. The consequence,
  stated: a repeat arrival's own ordinal `i` has a `:patient` stream
  that is never used. It is still CONSTRUCTED (the `mapv` at
  `engine.clj:1790` is unconditional and draw-free), so nothing
  shifts.
* **`:pathways` / `:module-assignment` draws stay per ARRIVAL
  ordinal**, on the resolved patient's stream, so their consumption
  stays a function of `:patients` alone. A repeat arrival's resolved
  closure is DISCARDED (the module walk happens once, at first
  registration); the draw is taken either way.

#### (b) The fold

**The fold is a queue-seeding pass, not a change to the main loop.**
This falls out of the tree rather than being chosen: `run`'s queue is
a `sorted-map` keyed `[t seq-no]` (`engine.clj:1847`), and
`schedule-followup` (`engine.clj:1994`) already inserts an entry at an
ABSOLUTE instant. Person events that reach the wire become ordinary
queue entries at their own `:t`. Nothing about the loop changes.

Which person events reach the wire, and as what:

| person event | engine mints | fold effect |
|---|---|---|
| `:person-registered` (newborn) | `:registered` at the delivery `:t`, on a NEW patient | mints the newborn patient |
| `:residence-move` | `:demographic-update` `:cause :residence-move` | `:address` in state-at-t; a move with NO `:prior-address` is a RETURN to housing and sets `:residence {:status :housed}` |
| `:residence-loss` (added 2026-08-25, part 1) | `:demographic-update` `:cause :residence-loss` | `:residence {:status :unhoused :last-known-address ..}` in state-at-t. An `:at-t0` one is the person's INITIAL condition and lands before any arrival, so a placeholder-free registration for an unhoused person renders PID-11 absent from its first message (ruling E1) |
| `:identity-correction` | `:demographic-update` `:cause :identity-correction` | `:name` or `:dob` in state-at-t |
| `:coverage-change` | `:coverage-change` | `:payer` in state-at-t |
| `:identity-unavailable` | nothing at its own `:t` -- it opens a window (section (d)) | window state |
| `:identity-resolution` | `:demographic-update` `:cause :identity-fill`, or `:merge` | section (d) |
| `:occupational-injury` | an ED arrival cause (section (c)) | -- |
| `:delivery` | an admission for the parent (section (c)) | -- |
| `:employment-change`, `:household-*`, `:pregnancy` | **nothing** | state only; they exist to correlate the above |
| `:person-death` | **nothing** -- ADR-0172 limitations row 4, ruling C below | stops the person's other events |

So the CLOSED 21-kind vocabulary (`event_schema.clj:263`) grows by
**two**: `:demographic-update` and `:coverage-change`. Deliberately
two and not fifteen -- the person process's fifteen kinds (fourteen
when this table was first written; `:residence-loss` landed 2026-08-25
and folds onto the SAME `:demographic-update`, so the vocabulary still
grows by exactly two) are the engine's INPUT, in the same relation to ground truth that pathway IR
and a compiled trajectory already have. **Person events are never
themselves log events.** They carry no `:patient-id` and could not
satisfy `every-event-has-participants` (`check.clj:76`) or
`participant-ids-exist-in-run` (`check.clj:85`) without inventing a
second participant vocabulary.

Their provenance still rides: every minted event carries
`:person-event-id`, the person stream's own `"<person-id>#<n>"`
string. It is a PROVENANCE STAMP, not a log reference, and section (e)
says what may and may not be asserted about it.

**The three engine-side shapes.**

1. `PatientState` (`engine.clj:166`) gains `:demographics` -- the
   state-at-t map. `evolve :registered` initializes it from
   `:persona`; a new `evolve` sibling per new kind updates it.
   `:persona` itself is NOT mutated: it stays the t0 record, so all
   fourteen t0-only census sites are untouched and
   `registered-persona-is-schema-valid` (`check.clj:794`) keeps
   asserting exactly what it asserts today.
2. `emit_hl7.clj:302` `personas-by-patient-id` becomes
   `demographics-at`: `(patient-id, log-index) -> demographic state`,
   folded from the log by the emitter, which keeps the emitter's
   log-only input -- *"a stage's own state is recoverable by scanning
   the log, no second input needed"* -- and lifts ADR-0172 limitations
   row 6, which then gets struck rather than re-tabled. Its own gate,
   `personas-are-keyed-by-patient-id-alone-test`, goes red in exactly
   this change, which is what it was written for.
3. **The demographic state is a Persona-shaped map plus a residence
   SUM**, because a places row cannot express "no residence":

   ```clojure
   {:name .. :sex .. :dob .. :phone .. :ssn .. :payer ..
    :residence {:status :housed   :address {..places row..}}
    ;; or       {:status :unhoused :last-known-address {..} }
    :identity  :known}          ; or :placeholder
   ```

   `sim-model/Persona` is NOT widened. Its `:address` is required and
   non-nilable (`persona.clj:107`), and widening it would move every
   `:registered` event in every corpus for a fact that belongs to
   state-at-t and not to a t0 sample.

**The person-side half of the residence sum is a person-simulator
change, and it is the one this arc owes that component.** `:residence-
move` today always carries a `places.edn` row, and limitations row 7's
gate is `(remove #(pool (:address %)) moves)` -- an absent `:address`
is `nil`, `(pool nil)` is `nil`, so an unhoused `:residence-move`
would go RED against a gate that is doing its job. The minimal shape
that keeps row 7 green verbatim: **one new kind, `:residence-loss`**
(no `:address`, carries `:prior-address`), with the return to housing
an ordinary `:residence-move` whose `:prior-address` is absent. The
fourteen kinds become fifteen; ADR-0172 section 2's table and arc 2b's
counted witness both move, and both are gates that must move in the
same commit.

#### (c) The two hooks

ADR-0168 section 4's two, unchanged.

* **`:delivery`** -> an admission for the parent at the delivery `:t`,
  and the newborn's FIRST encounter (`traffic-model.md`: *"the
  newborn's first encounter is the birth"*), with the mother-baby
  link. The newborn is an ADDITIONAL patient: arrival ordinal
  `(+ patients k)` in delivery-`:t` order, so `patient-id-for` and
  `mrn-for` are unchanged functions of an ordinal and the newborn's
  `:patient` stream is `(stream seed :patient (+ patients k))` --
  order-free, and disjoint from every t0 arrival's.
* **`:occupational-injury`** -> an ED arrival cause with an injury
  pathway, joining the ADR-0107 injuries family. It selects the
  injured person's own patient, or mints one if this is their first
  contact.

Both hooks CREATE arrivals that `:patients` does not count. Said
plainly because it changes what a config means: with `:persons`
present, `:patients` is the number of SELECTED arrivals, and the run's
patient count is that plus the newborns plus the injury arrivals.

#### (d) Identification, and the author statement

ADR-0172 ruling G1 put the disposition on the person side and the
minting on the engine side. Here is the minting.

**An arrival landing inside an open `:identity-unavailable` window**
mints a PLACEHOLDER registration: a `:registered` event on a FRESH
patient-id and a FRESH MRN (its own arrival ordinal, so no id space is
invented), carrying `:identity :placeholder` and a demographic state
of `{:name <the window's :alias-name>, :residence {:status :unknown},
:identity :placeholder}` -- **no address, no DOB, no sex, no payer**.
The window's `:alias-name` is already `{:family "Doe" :given
"Unknown"}` in the landed component (`process.clj:452`). This is the
author's *"unhoused unresponsive John Does"*: it reaches the wire as a
PID with a placeholder name and no address, and it does so whether or
not the person behind it already has a patient record.

**At the window's close**, `:identity-resolution`'s branch:

* `:fill` -> `:demographic-update` with `:cause :identity-fill` on the
  placeholder patient, SAME MRN, filling every field from the person's
  real demographics at that instant. It carries
  `:placeholder-event-id`, the log index of the placeholder
  `:registered`. If the person had no prior patient, the placeholder
  patient becomes theirs and the fold index is updated.
* `:merge` -> the placeholder is merged into the person's prior
  patient. Ruling B is which MRN; the recommendation is a fresh one,
  merged.

**The merge composes with churn's, and does not duplicate it.** The
event is `:merge` -- the same kind, same participant roles
(`:survivor`/`:merged`), same `:surviving-mrn`/`:merged-mrn`/
`:merged-mrns` payload that `decide :merge` (`engine.clj:828`) builds
and that `evolve :merge` (`engine.clj:1246`) folds -- plus
`:cause :identification`, optional and absent for every churn merge.
So `merge-survivor-absorbs-merged-mrns` (`check.clj:478`),
`no-events-after-merged-terminal` (`check.clj:498`), the run loop's own
`:merged` short-circuit (`engine.clj:1913`) and the post-merge-shadow
surface all apply unchanged.

What does NOT get reused is `decide :merge`'s own legality guard. Its
`never-mergeable?` excludes `:new` -- *"never admitted -- no
`:admission` event exists yet for `participant-ids-exist-in-run` to
find"* -- and a placeholder patient who arrived, was registered and
was never admitted is exactly `:new`. That premise is stale for a
run-produced world (every patient has a `:registered`), but relaxing
it would move churn corpora, which this arc has no license to do for
an unrelated reason. So arc 3a adds a step type
`:identification-merge` with its own guard (the placeholder exists, is
not already merged, is not the survivor) that emits the SAME event.
One new decide method; zero change to churn's.

**A merge with no survivor degenerates to a fill.** A person whose
first-ever contact is an unidentified arrival has no prior patient to
merge into. The branch draw is consumed either way (ADR-0172's fixed-
consumption law), and the corpus gets a fill. Named here because
silently emitting a merge with a null survivor is the defect this
sentence exists to prevent.

**Unhoused, which is a different thing entirely.** An unhoused person
is IDENTIFIED. They have a name, a DOB, a payer and a `:residence
{:status :unhoused}`. Their registration is an ordinary `:registered`
with `:identity :known`; what differs is one rendering decision, and
that is ruling E. Two states that both produce an empty PID-11 today
would be indistinguishable, and the whole point of carrying the sum in
ground truth is that a consumer testing an MPI can tell "we do not
know where they live" from "they have nowhere to live."

**DEVIATION, execution session 2026-08-26 (part 4). THE RULE ABOVE
WAS UNREACHABLE AS WRITTEN, and the tree measured it rather than
argued it.** The antecedent -- *an arrival landing inside an open
`:identity-unavailable` window* -- asks two independent processes to
coincide, and at the person process's own rates they do not. Over a
200-person, ten-year walk at seed 424242 the process opens **9
windows covering 11,491,200 of 63,072,000,000 person-seconds --
0.018% of the horizon** -- and the EARLIEST opens at **t 36,118,094
(day 418)**, while every t0 arrival of a scenario at `:arrival-gap`
5 has happened inside the first 60,000 seconds (~17 h). The two
intervals cannot meet, so the coincidence alone mints nothing.

**Ruled (a), 2026-08-26: an `:identity-unavailable` window is itself
an unidentified ED presentation and MINTS the arrival.** It is not a
state somebody is quietly in waiting to be arrived at; it is the
presentation -- which is what the author's *"unhoused unresponsive
John Does"* describes -- so it mints an ED arrival the same way
`:occupational-injury` does. The coincidence rule above STANDS,
implemented exactly as written, and is JOINED by its own antecedent
rather than replaced by it; everything downstream in this subsection
(the placeholder `:registered`, the fresh MRN, the `:fill`/`:merge`
fork at the window's close, `:identification-merge`, the
degenerate-to-fill case) is unchanged and now actually reachable.
`person-fold/hook-kinds` carries the measurement. One defensible
reading once the numbers were in, so fix-forward with disclosure
(`rulings.md#R-stop-only-on-two-defensible-readings`); the full
figures are in
`.agents/session-records/2026-08-26-arc-3a-fold-part-4.md`.

#### (e) The new invariant family in `check.clj`

`check.clj`'s referential family -- ADR-0163/0166's
`medication-end-references-existing-order-and-follows-it-in-time`
(`check.clj:682`) and its care-plan twin (`check.clj:740`) -- is the
shape, and ADR-0172 section 3 already committed this arc to
reproducing its three parts and its pre-horizon escape. Six
invariants; the first is that function body renamed, and the other five
are ordinary catalog members over the two new kinds:

1. `identity-fill-references-its-placeholder-registration` -- a
   `:demographic-update` with `:cause :identity-fill` carries a
   `:placeholder-event-id` indexing a real `:registered` event, for the
   SAME patient, with `:identity :placeholder`, at or before its own
   `:t`. Verbatim the medication-end shape.
2. `identification-merge-survivor-is-the-persons-prior-patient` -- a
   `:merge` with `:cause :identification` names as `:merged` a patient
   whose `:registered` carries `:identity :placeholder`, and as
   `:survivor` a patient whose `:registered` carries the SAME
   `:person-id`.
3. `every-placeholder-registration-is-resolved-or-still-open` -- a
   placeholder patient either has its fill/merge, or the run ended
   before the window's close. **Never "or not at all"** without that
   second clause: a placeholder left dangling by a horizon is real
   traffic, and an invariant that forbade it would be wrong.
4. `demographic-update-reports-a-real-change` -- the prior values it
   carries equal the folded state immediately before it. Arc 2b's own
   lesson, promoted from the person side to the wire side: *an event
   that reports no change is not an event* (`b4f1115`).
5. `no-demographic-event-after-a-patient-expires` -- no
   `:demographic-update`, `:coverage-change` or `:registered` for a
   patient whose state is `:expired`.
6. `person-scoped-provenance-is-a-stamp-not-a-reference` -- the gate
   that keeps `:person-event-id` honest. It asserts that
   `:person-event-id` is NEVER treated as a log index by any invariant
   in the catalog. Born green, red the day someone resolves it, which
   would be a dangling reference by construction because person events
   are not log events.

**The pre-horizon escape is inherited verbatim**, exactly as ADR-0172
section 3 says: an `:identity-correction` with no antecedent corrects
the t0 persona, which is definitionally prior to every event in the
log because `:registered` is always a patient's first event. The
reference is ABSENT, not dangling -- the same distinction, with the
same justification, now gated three times.

#### (f) Provenance

`:persons` and `:persona-config` are both stamped. `ehrt.sim.run`'s
`engine-params` (`run.clj:391`) grows both keys, so
`manifest/build` (`manifest.clj:99`) carries them without a schema
change -- `ManifestV1_1` is an open map, the same additive seam
`:event-schema-version` and `:stream-scheme` already ride.

The event contract bumps **1.2.0 -> 1.3.0**, once, in the fold's own
commit. Two new kinds and a handful of optional keys are additive, and
`classify-change` (`event_schema.clj:665`) is what says so rather than
this ADR. The deprecation window stays waived: still no external
consumer.

### 3. Rulings needed

Lettered, with a recommendation on each, in ADR-0172's shape --
recommended option first, declined options kept unstruck. **All five
were ruled by the author on 2026-08-25, and every one took the
recommendation: A1 B1 C1 D1 E1.** Each ruling is quoted below at the
option it selected; the rejected options are kept verbatim, unstruck,
because what was declined is the reason the selection means anything.
The author added one sequencing instruction that is not a lettered
ruling and binds the execution session anyway: **`:residence-loss`
lands FIRST**, as its own commit, before any engine code -- so the
person-side half of the residence sum (section 2(b)) is a landed,
gated fact by the time the fold reads it.

**A. Arrival selection: uniform from the pool via WORLD, or a
person-side arrival propensity?**

**RULED A1** (author, 2026-08-25): uniform from the pool, one
`:world` draw per arrival ordinal, over the persons alive at that
instant. The frequent-flyer that A2 would have bought is declined for
v1 rather than lost -- A2 stays available, and cheaply, precisely
because A1 landed first. Section 2(a) is where this lands.

* **A1 -- uniform from the pool, one `:world` draw per arrival
  ordinal, over the persons alive at that instant. RECOMMENDED.** It
  is the family's own definition (`stream-family-tag`: *"arrivals, and
  every cross-patient decision"*), it is one fixed draw whether or not
  the alive-filter removed anyone, and it keeps an arrival's EXISTENCE
  a `:world` fact. The cost, stated: nobody is more likely to show up
  at an ED than anyone else, so the corpus has no frequent-flyer.
* A2 -- a person-side arrival propensity: each person carries a
  drawn rate and arrivals are sampled from it. Buys the frequent-flyer
  (a real and valuable MPI test shape) and costs the thing ADR-0172
  ruling G2 was rejected for: an arrival's existence becomes a
  function of a `:person`-family draw, which is the person process
  driving engine control flow. The charter did not draw this hazard,
  and adding it here would be adding a hazard rate in an engine arc.
  Available later, cheaply, if A1 lands first.

**B. The placeholder's MRN: fresh and merged, or provisional and
overwritten?**

**RULED B1** (author, 2026-08-25): a FRESH MRN, merged later. The
post-merge shadow is the point; section 2(d)'s `:identification-merge`
step type is what mints it, riding churn's own `:merge` event shape.

* **B1 -- a FRESH MRN, merged later. RECOMMENDED.** It composes with
  `churn`'s `:merge` into the post-merge-shadow surface
  `traffic-model.md` calls the highest-value injectable class for
  MPI-consumer testing: two MRNs really did exist, one really was
  retired, and a consumer that indexed the first has a shadow to
  reconcile. It is also the only branch that produces an A40 at all.
* B2 -- a provisional MRN overwritten in place at resolution. One
  fewer event, no merge, and a corpus in which the defect surface this
  whole flow exists to produce never appears. It also makes the fill
  and merge branches nearly indistinguishable on the wire, which
  wastes ADR-0172 ruling D1's `:merge-fraction` knob.

**C. Does `:person-death` for a person with no compiled death ever
reach the wire?**

**RULED C1** (author, 2026-08-25): no -- ADR-0172 limitations row 4
is confirmed as it stands. `person-death-emits-no-ground-truth-event-test`
stays green, and what the fold owes instead is behavioural: a dead
person is not in section 2(a)'s arrival candidate set, gated by section
2(e) invariant 5.

* **C1 -- no. CONFIRM ADR-0172 limitations row 4 as it stands.
  RECOMMENDED.** A death outside care has no HL7v2 trigger this
  emitter writes: the only death that reaches a message is an A03
  whose PV1-36 carries an expired disposition, and that requires an
  encounter to close. Minting anything else would be inventing a wire
  shape. What the fold owes instead is behavioural, and section 2(a)
  already has it: a dead person is not in the arrival candidate set,
  and section 2(e) invariant 5 is the gate.
* C2 -- lift the row: a person-death mints a `:death-notification`
  ground-truth event, rendered as nothing today and available to a
  future emitter. Buys a ground-truth fact with no reader, at the cost
  of a 22nd event kind whose whole content is unrenderable, and of
  turning `person-death-emits-no-ground-truth-event-test` red for a
  gain no message can show.

**D. One declared sweep, or land dark and turn it on?**

**RULED D1** (author, 2026-08-25): two commits. Commit 1 lands the
entire fold with `:persons` absent from every gated corpus and
`bin/regression-oracle` reporting IDENTICAL with no declaration;
commit 2 turns `:persons` on and re-pins in ONE declared sweep. The
dual-path `decide :registered` is accepted as a named cost that lives
until arc 3b.

* **D1 -- two commits. RECOMMENDED.** Commit 1 lands the entire fold
  -- config key, index, decide/evolve methods, the emitter re-key,
  the six invariants, the two new event kinds -- with `:persons`
  ABSENT from every gated corpus. Absent means the persona is still
  drawn at `engine.clj:493` from `:patient`, through the guard-then-
  original-expression shape this repo's own equivalence-proof pattern
  uses, so `bin/regression-oracle` must report IDENTICAL with no
  declaration. Commit 2 turns `:persons` on and re-pins. Two sweeps,
  and the second one's diff has exactly ONE possible cause. The cost
  is a dual-path `decide :registered` that lives until arc 3b, and
  that cost is worth naming: it is a second code path, which is what
  `rulings.md#R-move-not-improve` is usually wary of.
* D2 -- one commit, one declared sweep. Cheaper, and no dual path.
  Forfeits the proof: a surprise in the re-pinned corpus would have
  two possible causes (the fold's own defects, and the deliberate
  reshuffle) mixed in one diff -- which is precisely the argument
  ADR-0172 ruling F1 made for arc 2b, and it has not weakened.

**E. Unhoused rendering: PID-11 absent, or a sentinel string?**

**RULED E1** (author, 2026-08-25): PID-11 ABSENT on the wire in v1,
with the distinction carried in ground truth as `:residence {:status
:unhoused}` versus `:unknown`. A sentinel belongs in a site profile,
not in `xad-field`'s body.

The evidence, since this ruling asked for it. Real registration
systems do NOT converge, and none of the three conventions in the
literature is an absent address:

* A curated-registry study of homelessness identification in the EHR
  names, as the standard identifiers used historically, *"homeless
  check box or keyword 'homeless' in patient address field"*, and
  separately *"geocoded patient addresses corresponding to addresses
  of regional emergency shelters, transitional housing programs, or
  homeless service providers"*. Read directly from the paper;
  a search-result summary that claimed the same paper counted BLANK
  addresses as homeless does not survive reading it, and is not
  relied on here.
* A 2025 address-change study names as criteria a *"residential
  address that indicates 'undomiciled' or 'homeless'"*, a congregate
  living facility, or the hospital's own address.
* HL7 v2 itself offers nothing: Table 0190 (address type) has no
  homeless or no-fixed-address code, and there is no US Core address
  extension for it. The v3 `Homeless` value set is a LIVING
  ARRANGEMENT concept, not an address.

So a sentinel is what the field really does, and every sentinel is one
site's local convention.

* **E1 -- PID-11 ABSENT on the wire in v1, with the distinction
  carried in GROUND TRUTH (`:residence {:status :unhoused}` versus
  `:unknown`). RECOMMENDED.** Three reasons. The author statement says
  no address. The evidence above says any literal we pick -- HOMELESS,
  UNDOMICILED, a shelter row -- is one site's convention rendered as
  if it were a standard, and this repo has a seam for exactly that
  (site profiles, `docs/site-profiles.md`), so a sentinel belongs
  there and not hardcoded in `xad-field`. And ground truth loses
  nothing: the sum type is in the log, so a consumer can distinguish
  the two states even while the wire cannot.
* E2 -- a sentinel XAD now, `HOMELESS` in the street component.
  Matches what most real systems emit, and makes the corpus
  immediately useful to a consumer that greps for it. Costs: it puts
  free text with no code system behind it into the emitter's own body,
  where no site can override it; and it renders an unhoused patient
  and an unidentified one differently for a reason the emitter cannot
  actually see, since the emitter would have to read the residence sum
  anyway -- at which point E1's ground truth is doing the work and the
  literal is decoration.
* E3 -- render the last known address with `XAD-7` marked `BA`
  (bad address). Closest to what a real HL7 feed from a system with an
  address history looks like. Rejected for v1 only because
  `xad-field` (`emit_hl7.clj:245`) writes five components and none of
  them is the type, so it is an emitter widening this arc has no
  reason to make first.

### Every ruled row of ADR-0172, checked against the fold

The session's own STOP condition. All seven are honourable as the tree
stands; two took work, and the work is what shaped this design.

| ruling | honoured how |
|---|---|
| A1 newborns are full persons | section 2(c): own patient, own `:patient` stream at ordinal `patients + k`, own ADT, Persona from `:person-registered` |
| B1 the head draws a family move | untouched -- propagation is person-side and already landed; the engine folds each member's own `:residence-move` |
| **C1 GMF death authoritative, compiled death as a t0 parameter** | **took work -- see below** |
| D1 `:merge-fraction` config | rides `:persons {:identification ...}` straight through |
| E1 authored-provisional rates | untouched |
| **F1 the component lands alone** | **took work -- the fold could not live in `sim-engine` at all; section 2(a)** |
| G1 person-side disposition, engine-side minting | section 2(d) mints all three: placeholder, fill, merge |

**C1, and the ordering problem it creates.** C1 gives
`initial-persona` the compiled trajectory's death instant as a t0
parameter, and `person-simulator/persons` takes it as `:deaths
{person-id -> instant}`. But `persons` is a WHOLE-POPULATION front
door (household propagation is a cross-person pass, ruling B1), so it
must be called once, before the run -- while the compile lives inside
`decide :registered`, at the patient's arrival. Persona -> compile ->
death -> walk -> persona.

It resolves, exactly, and the resolution is forced:

1. The persona is drawn first. `initial-persona`'s own docstring
   settles that this is legal: *":death-t shapes no field of the
   returned Persona"*. Thirteen draws from the person's `:person`
   stream.
2. **The module walk moves from arrival-time to run-start.** Nothing
   stops it: reading `decide :registered` (`engine.clj:483-517`), the
   only `world` values it touches are `:persona-config`,
   `:module-horizon-days`, `:history` and `:facility` -- all config --
   and its `reg-t` is `(sim-model/reference-today-epoch-day)`, a fixed
   CALENDAR anchor, not the arrival instant. The walk is already
   independent of when the patient arrives.
3. The compiled death instant is then **exactly computable up front**,
   which is the part that could have failed and does not.
   `compile_trajectory.clj:441` emits its bridging delays as
   `{:type :delay :from gap-minutes :to gap-minutes}` -- and
   ADR-0171 section 2(d) made `:from` = `:to` DRAW-FREE. So every
   advance in the compiled prefix is a constant, and the death instant
   is `arrival-t + 60 * (sum of gap-minutes before the first
   :discharge carrying :disposition :expired)`. No simulation needed.
   Had compiled delays been ranges, C1's literal reading would have
   been structurally impossible and this session would have stopped.
4. `persons` is then called with real `:deaths`, per C1, and a person
   with no compiled death keeps their own drawn one.

The engine exports the compile as a function so `ehrt.sim.run` can
call it with the engine's own `:patient` stream rather than
reimplementing the positioning; `decide :registered` then attaches
what was pre-compiled. The order of the `:patient` stream's pre-loop
draws (`assign-pathway`, `assign-module`, `churn/inject`, then the
walk) must be PINNED in that commit's own docstring, because it is the
one thing about the reshuffle that is a choice rather than a
consequence.

### Consequences

* Arc 3a has a config key, a fold that is a queue-seeding pass, two
  new event kinds, six invariants, and a predicted blast radius
  (section 1) rather than a discovered one.
* **ADR-0172 limitations row 6 is struck by this arc, not re-tabled**
  -- `personas-are-keyed-by-patient-id-alone-test` goes red in the
  same commit that re-keys the lookup, which is what that gate was
  written to do. Row 7's gate stays green because the residence sum
  lands as a new kind rather than as a nil `:address`. Rows 1, 2, 3,
  5, 8, 9, 10, 11 and 12 are untouched. Row 4 is ruling C.

  **CORRECTED 2026-08-26, twice, both by the tree.** (a) The row did
  NOT go red at the re-key (part 2, `2393b48`), and should not have:
  split as ruling D1 requires, the re-key moved the lookup's SHAPE
  while its VALUE was still the t0 Persona, so what the row STATES --
  a delta folded onto patient state is invisible to every message --
  survived the change that was supposed to falsify it. This bullet was
  written assuming the re-key and the fold land together. (b) The
  strike was therefore paid in PART 3, and paid by DELETING the gate
  rather than watching it go red: with the fold landed the row's
  substance is false by design, and a gate over a limitation that no
  longer exists can only assert something untrue or something vacuous.
  Its successor is a positive law, `demographics-at-answers-state-at-t-
  test` in `sim-emit-hl7`. ADR-0172 section 4 carries the strike where
  the row stood, and the surviving rows keep their numbers.
* The person process owes one change: `:residence-loss`, a fifteenth
  kind. It is a `components/person-simulator/src` change riding an
  engine arc, and it moves that component's own charter table, its
  counted witness and ADR-0172 section 2 -- all in the same commit or
  not at all.
* **DEVIATION, execution session 2026-08-25.** `:residence-loss` cost
  one thing this design did not price: a THIRTEENTH limitations row,
  *a household never loses its housing*. The tree forced it. Ruling
  B1's propagation pass copies a head's `:residence-move` to every
  member VERBATIM, so a member who could lose housing on their own
  would receive copies reporting a change they never had -- a
  housing-gained move naming no `:prior-address`, delivered to
  somebody who never stopped having one. Coupling housing to household
  membership is what keeps the copy honest, and it costs no draw. Two
  other consequences fell out of the same coupling and are recorded in
  the row: ruling A1's newborn, delivered into an unhoused household,
  is the one member who CAN be unhoused (it is delivered into the
  state rather than losing anything), and a household constituted by
  such a birth is kept off the join roster so nobody housed can join
  it. One defensible reading, so the tree wins and this is the record
  (`rulings.md#R-stop-only-on-two-defensible-readings`).
* **DEVIATIONS, execution session 2026-08-26 (part 3, the fold).** Five,
  each with one defensible reading and so fix-forward with disclosure
  (`rulings.md#R-stop-only-on-two-defensible-readings`).

  1. **A REPEAT ARRIVAL QUEUES NO STEPS.** Section 2(a) reads *"the
     second encounter's steps simply continue that patient's log"*. The
     tree refuses: a second `:admission` for a patient whose status is
     `:discharged` violates `check.clj`'s `admission-only-when-new`,
     which is this project's single-encounter horizon (sim/ADR-0007
     point 3) expressed as an invariant. Lifting that horizon is not an
     arc-3a change. What a repeat arrival is FOR survives untouched --
     the person resolves to the patient they already are, and every
     later demographic event of theirs lands on that one patient. Its
     `:patient`-stream draws are still taken, so consumption stays a
     function of `:patients` alone.
  2. **`:persons` IS A TWO-LAYER KEY, and section 2(a)'s block is the
     CONFIG side.** `ehrt.sim.run`'s `:persons` is the authored
     `{:count :years :identification :unhoused}` map; `engine/run`'s is
     `{:population :personas :alive :events}`. Exactly the treatment
     `:modules` already has (names there, loaded closures here), and
     forced: the engine may not require the component that draws the
     stream, so somebody has to translate, and `run` cannot be that
     somebody.
  3. **RULING C1's ORDERING NEEDED A THIRD PASS, and the cycle is
     broken at the ALIVE FILTER.** As written, aliveness depends on
     deaths, deaths depend on the person-to-arrival binding, and the
     binding depends on aliveness. `ehrt.sim.run` therefore calls
     `persons` TWICE: pass one with no compiled deaths, whose
     `:person-death` events -- each person's OWN drawn death, a
     function of nothing but their own `:person` stream -- become the
     `:alive` map; `engine/person-plan` then answers the binding from
     fixed data; and pass two runs with the real `:deaths`. What the
     filter is conservative about is stated in that function's own
     docstring: a person whose drawn death precedes an arrival is not
     selectable for it even though binding them would have replaced
     that death with a later compiled one. The direction the filter
     exists to forbid is closed absolutely, because a compiled death is
     by construction at or after the arrival that produced it.
  4. **THE 1.2.0 -> 1.3.0 BUMP WAS NOT OWED.** Section 2(f) says
     `classify-change` is what decides, and it does: against the frozen
     1.2.0 baseline it returns `{:additive? true :breaking []}`, so the
     policy's own rule is PATCH/none. The bump is taken anyway and
     deliberately, because `:event-schema-version` is a consumer's only
     handle on what a log they hold can contain, and a 1.3.0 log may
     carry two kinds a 1.2.0-era consumer has never seen. The version
     note in `event_schema.clj` carries the reasoning and the
     validates-unchanged argument.
  5. **THE TWO KINDS REACH GROUND TRUTH, NOT A NEW MESSAGE TYPE.**
     Section 2 specifies the emitter's work as the re-key and nothing
     more, and an ADT^A08 for `:demographic-update` (or an A31, or an
     IN1 update for `:coverage-change`) is a message-type registration,
     a control-id derivation, a derivability property and a
     `witnessed-message-types` claim -- none of which this ADR
     designs. What row 6's strike actually rests on is
     `demographics-at`: every message a patient receives AFTER a delta
     renders the changed values, gated by
     `demographics-at-answers-state-at-t-test`. The A08 is a candidate
     for a later arc, named here rather than assumed.
  6. **ROW 4's GATE ASSERTED THE WRONG THING, and `:coverage-change`
     is what found it.** `person-death-emits-no-ground-truth-event-test`
     asserted that no person-event kind SHARES A NAME with a
     ground-truth kind, and section 2(b)'s own fold table names
     `:coverage-change` on both sides deliberately -- so the gate went
     red on a name this ADR chose. Found by `make test`, in a brick
     other than any this session had touched, which is
     `rulings.md#R-full-suite-before-push` paid for again. The
     assertion is rewritten to the STRUCTURAL claim row 4 was always
     making: a person event carries no `:patient-id`, no `:active-mrn`
     and no log-shaped `:participants`, so it could not satisfy
     `every-event-has-participants` without inventing a second
     participant vocabulary -- this ADR's own sentence. The shared-name
     set is pinned at exactly `#{:coverage-change}`, so a NEW overlap
     still goes red.

* `rulings.md` is FROZEN (de-scaffold ruling, 2026-08-25). Nothing
  here becomes a rulings row; section 2(e)'s six invariants land as
  gates in arc 3a's own commits or not at all.
* Arc 3b (scheduling state `R-mix-5`, bed-status cycle `R-mix-6`)
  inherits the fold index, the `:demographics` field and the
  queue-seeding pass, and needs none of them designed again.
