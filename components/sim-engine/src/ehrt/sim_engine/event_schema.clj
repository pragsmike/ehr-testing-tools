(ns ehrt.sim-engine.event-schema
  "The ground-truth event log's CONTRACT: an executable schema for the
  one primitive this project actually produces (`sim/ADR-0008` --
  the log is the only primitive; every emitter, check, and player is a
  projection of it).

  WHY THIS EXISTS. A consumer who wants to translate simulated traffic
  into a proprietary format we cannot know ahead of time needs the
  richest semantic form we have, which is the event log itself --
  `ehrt sim run --format ground-truth`, or `corpus generate sim`'s own
  byte-identical `events.edn` (`notes/ADRs.md` ADR-0100). Before this
  namespace, such a consumer reverse-engineered the event shape by
  reading `ehrt.sim-emit-hl7.emit-hl7` -- reading our HL7 emitter in
  order to write a not-HL7 emitter -- which made that emitter's own
  field choices the de facto contract and made a schema CHANGE
  indistinguishable from a schema BREAK. This namespace is the
  explicit contract that replaces that reading.

  DERIVED, NOT DESIGNED. Every kind, key, optionality, and value shape
  below comes from `.agents/plans/2026-08-16-event-log-census.md`: the
  21 `{:event ...}` construction sites in `engine.clj` reconciled
  against 4,997 events across eleven corpora, the two populations
  agreeing exactly. Where the census and the constructor disagree
  about how WIDE a field is, the constructor wins and the census is
  cited as the narrower observation -- `:admission`'s own `:reason`
  is the live example (see it below). This schema DESCRIBES the log;
  it does not change it. The shape defects the census found (S-1
  through S-6) are register rows for a follow-on, deliberately
  described here rather than fixed, because describing the current
  truth first and changing it afterwards under a versioned contract is
  the entire point of the tier this contract is published at.

  STABILITY TIER (author ruling Q-A (a), 2026-08-16): public and
  versioned. `schema-version` below is stamped into every `sim run`
  manifest as `:event-schema-version`, so a log carries the contract
  version it was produced under. Additive change (a new kind, a new
  OPTIONAL key) is non-breaking and does not bump. Any non-additive
  change does, and `ehrt.sim-engine.event-schema-test` enforces exactly
  that against the committed EDN export -- see `classify-change` for
  the mechanical definition, which is what makes the promise testable
  rather than aspirational.

  ARTIFACT SHAPE (author ruling Q-B (a), 2026-08-16): this Clojure
  source is the source of truth, AND it is exported to
  `resources/sim-engine/event-schema.edn` as plain data, with a parity
  test. Every referenced schema is INLINED in that export (nothing
  here uses a malli registry), so the EDN artifact is self-contained
  and a non-Clojure consumer can read the whole contract without
  running Clojure. JSON, when it lands, is a projection of that EDN
  under stated rules -- EDN is primary (author ruling, 2026-08-16).
  A SECOND resource, `event-schema-baseline.edn`, is the frozen
  last-versioned contract the change gate measures against -- see the
  comment above `export-resource-path` for why one file cannot do both
  jobs.

  NO RUNTIME COST. Nothing in the production path validates against
  these schemas. The engine does not validate what it emits; the
  emitters do not validate what they read. Validation happens in
  TESTS -- the property test over real corpora and generated worlds,
  and the three consumer-conformance tests -- which is what converts
  `sim-emit-hl7`, `sim-emit-fhir`, and `sim-check` from de facto
  consumers of an implicit shape into first consumers of an explicit
  one."
  (:require [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-model.interface :as sim-model]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.pprint :as pp]
            [clojure.walk]
            [malli.core :as m]
            [malli.util :as mu]))

(def schema-version
  "The event contract's own version, semver-shaped, stamped into every
  `sim run` manifest as `:event-schema-version` (`ehrt.sim.manifest`).

  Bump policy, enforced by `event-schema-test`:
  - PATCH/none for a purely additive change (a new event kind, a new
    optional key on an existing kind). Non-breaking: a consumer written
    against the older version keeps working, because nothing it read
    moved.
  - MINOR or MAJOR for anything else -- a key removed, an optional key
    made required, a value schema changed, a kind removed. A key or
    kind slated for removal is marked deprecated in `docs/formats.md`
    for one minor release BEFORE it goes, so a consumer gets a release
    in which to notice -- WAIVED (author ruling, 2026-08-18, ADR-0151)
    while the event contract has no consumer outside this repository:
    no Clojars publication, and no downstream repo pinning
    `:event-schema-version`. The waiver expires ON THE FIRST such
    consumer, at which point the clause above binds unamended and
    nothing further need be edited here for it to. Each removal made
    under the waiver says so in its own version note below, so the
    waiver leaves a trail rather than a silence.

  1.0.0 is the shape as of the event-log contract arc, describing the
  tree at `24f351d` -- not a redesign of it.

  1.1.0 (2026-08-18, ADR-0150) is the contract's FIRST non-additive
  change: `ResultEntry`'s `:units` renamed `:unit` (census S-6). MINOR
  rather than MAJOR because exactly one key of one nested schema moved
  and every other kind is untouched. DISCLOSED, because the policy
  above states it and this change does not honour it: the deprecation
  clause -- a key slated for removal is marked deprecated for one minor
  release BEFORE it goes -- was NOT run. 1.0.0 was published 2026-08-16,
  two days before this change, with `ResultEntry`'s own docstring
  already naming `:units` as a known defect (census row S-6); there is
  no consumer release in between for a deprecation window to protect.
  AMENDED 2026-08-18 (ADR-0151): this note's last sentence used to
  read -- a future removal with any distance from publication owes the
  window. The window is now WAIVED while no external consumer exists,
  so 1.1.0 is re-read as the first removal MADE UNDER THE WAIVER,
  disclosed. The original disclosure above is left standing, not
  rewritten.

  1.2.0 (2026-08-18, ADR-0151) is census S-1: `:reason` becomes
  `{:optional true}` on `:admission` and `:outpatient-visit`, and a
  module-compiled encounter -- which has no reason to give, because
  `compile_trajectory`'s own `encounter->step` never sets one -- now
  omits the key instead of emitting `:reason nil`. MINOR rather than
  MAJOR because one key of two kinds changed cardinality, no kind was
  removed, no value schema moved, and nothing a 1.1.0-era consumer read
  was renamed. `classify-change` calls a required key made optional
  BREAKING, which is the whole of what this bump buys; ADR-0150 wrote
  and proved this same fix and correctly STOPPED it rather than let it
  share S-6's bump. MADE UNDER THE WAIVER, disclosed: no deprecation
  release was run, because the waiver above holds -- the contract still
  has no consumer outside this repository. Note also that a 1.1.0-era
  log VALIDATES UNCHANGED against 1.2.0: `[:maybe ...]` is retained, so
  only the writer changed and the breaking direction is
  producer-side.

  1.3.0 (2026-08-26, ADR-0173 section 2(f), arc 3a part 3) is the
  demographic fold: TWO new kinds, `:demographic-update` and
  `:coverage-change`, plus one new OPTIONAL key, `:residence`, on
  `:registered`. `classify-change` calls all three ADDITIVE and is
  what says so rather than the ADR -- a new kind and a new optional key
  are the two shapes its own docstring names as non-breaking, and
  running it against the frozen 1.2.0 baseline returns
  `{:additive? true :breaking []}` (`event-schema-test`'s own
  assertion). So NO bump was OWED; this one is TAKEN, deliberately, and
  the reason is that `:event-schema-version` is a consumer's only
  handle on what a log it is holding can contain. A 1.2.0 log and a
  1.3.0 log are not interchangeable in the direction that matters to a
  reader -- a 1.3.0 log may carry two kinds a 1.2.0-era consumer has
  never seen and will dispatch on `:event` for. The policy above makes
  the bump optional for an additive change; it does not make it wrong,
  and MINOR is the semver level for new, backward-compatible
  functionality.

  A 1.2.0-ERA LOG VALIDATES UNCHANGED AGAINST 1.3.0, and here is how:
  the two kinds are new BRANCHES of the `:event` multi, so no existing
  branch's key set, optionality or value schema moves at all, and
  `:residence` is `{:optional true}` on `:registered`, so a
  `:registered` event that omits it validates exactly as before. The
  direction that breaks is the other one -- a 1.2.0 schema meeting a
  1.3.0 log fails to dispatch on two kinds -- which is what a version
  is FOR. MADE UNDER THE WAIVER, disclosed: no deprecation release was
  run, and none is owed here in any case, since nothing was removed.

  1.4.0 (2026-08-26, ADR-0173 sections 2(c)/2(d), arc 3a part 4) is the
  two clinical hooks and the identification flow. UNLIKE 1.3.0, THIS
  BUMP IS OWED, and `classify-change` is again what says so rather than
  the ADR. Run against the frozen 1.3.0 baseline it returns
  `:additive? false` with exactly four reasons, all of them on
  `:demographic-update` and all of them WIDENINGS:

    :demographic-update: key changed: :cause (value schema changed)
    :demographic-update: key changed: :field (value schema changed)
    :demographic-update: key changed: :prior-value (value schema changed)
    :demographic-update: key changed: :value (value schema changed)

  `:cause` gained `:identity-fill`, `:field` gained `:identity`, and
  `DemographicValue` gained the identity arm the other two need. Every
  other part-4 change IS additive and is reported as such: five new
  optional keys on `:registered` (`:person-id`, `:identity`,
  `:alias-name`, `:window-close-t`, `:mother-patient-id`), three more on
  `:demographic-update` (`:placeholder-event-id`, `:persona`,
  `:residence`), one on `:admission` (`:person-event-id`) and two on
  `:merge` (`:cause`, `:person-event-id`). No kind was added and none
  was removed: the vocabulary the fold grew is still the two 1.3.0
  declared.

  MINOR rather than MAJOR for the reason 1.1.0 and 1.2.0 give: the four
  moved value schemas are enum and union WIDENINGS, so a 1.3.0-era LOG
  validates unchanged against 1.4.0 -- every value a 1.3.0 producer
  could emit is still in range, and the breaking direction is the other
  one, a 1.3.0 schema meeting a 1.4.0 log and failing on a `:cause` it
  has never seen. That is what a version is FOR, and it is exactly why
  `classify-change` is deliberately conservative about widenings rather
  than reasoning about them.

  MADE UNDER THE WAIVER, disclosed: no deprecation release was run, and
  none is owed in any case, since nothing was removed.

  1.5.0 (2026-08-26, ADR-0174 section 2(a), arc 3b sweep 1) is the
  ENCOUNTER: one new optional key, `:encounter-id`, on every kind, plus
  the same key on `BedSwapSide` -- the per-side map inside a
  `:bed-swap`'s own `:swap`, which is where a two-patient event has to
  put an id that names one encounter per patient. No new kind, none
  removed, nothing renamed.

  THIS BUMP IS OWED, and `classify-change` is what says so rather than
  the ADR, which had recommended taking 1.5.0 whether owed or not. Run
  against the frozen 1.4.0 baseline it returns `:additive? false` with
  exactly ONE reason:

    :bed-swap: key changed: :swap (value schema changed)

  Read it precisely: the twenty-three top-level `:encounter-id` entries
  ARE additive and are reported as such -- a new optional key is one of
  the two shapes `classify-change`'s own docstring names as
  non-breaking. What it will not call additive is a change one level
  DOWN, inside `[:map-of :string BedSwapSide]`, because it compares a
  nested value schema whole rather than descending into it. That
  conservatism is deliberate (see 1.4.0's own last paragraph) and is
  taken at its word here rather than argued around.

  A 1.4.0-ERA LOG VALIDATES UNCHANGED AGAINST 1.5.0, and here is how:
  every key this version adds is `{:optional true}`, at both levels, so
  an event that omits it -- which is every event of every run that did
  not opt into `:encounters` -- validates exactly as before. The
  breaking direction is the other one, a 1.4.0 schema meeting a 1.5.0
  log carrying a key it has never seen inside a closed map, which is
  what a version is FOR.

  The key is OPTIONAL in the schema and MANDATORY in every
  run-produced encounter, and the thing that makes it mandatory is a
  gate, not this schema: `ehrt.sim-check.check/every-encounter-is-
  opened-and-closed-or-still-open`. Optional here so a 1.4.0-era log
  validates; required there so a live run cannot quietly stop stamping.

  MADE UNDER THE WAIVER, disclosed: no deprecation release was run, and
  none is owed in any case, since nothing was removed.

  1.6.0 (2026-08-27, ADR-0174 section 2(c), arc 3b sweep 2) is the BED
  CYCLE: one new kind, `:bed-status-change`, and one widened
  `Participant` -- which is now `[:or PatientParticipant
  BedParticipant]`, because the new kind's subject is a BED and beds
  have no patient-id.

  THIS BUMP IS OWED, and by twenty-three reasons rather than one.
  `classify-change` against the frozen 1.5.0 baseline returns
  `:additive? false` with exactly one entry per EXISTING kind:

    :admission: key changed: :participants (value schema changed)
    ... twenty-two more, one per kind, all identical in shape ...

  Read it precisely. The NEW KIND is additive and is correctly not in
  that list -- a new event kind is one of the two shapes
  `classify-change`'s own docstring names as non-breaking. What is
  reported is `:participants`, on every kind that already existed,
  because its value schema `[:vector Participant]` changed one level
  down -- the same conservatism that produced 1.5.0's single
  `:bed-swap` reason, applied here to twenty-three kinds at once. It is
  taken at its word rather than argued around.

  A 1.5.0-ERA LOG VALIDATES UNCHANGED AGAINST 1.6.0. Every participant
  such a log carries is a `PatientParticipant`, and
  `[:or PatientParticipant BedParticipant]` accepts every one of them
  through its first branch, unchanged. The widening is strictly a
  widening.

  AND THE BREAKING DIRECTION IS REAL AND IS NOT PAPERED OVER, which is
  the one thing separating this bump from 1.5.0's. A consumer written
  against 1.5.0 may reasonably have assumed `:patient-id` is present on
  EVERY participant of EVERY event -- the schema said so -- and a 1.6.0
  log carrying a `:bed-status-change` breaks that assumption on real
  data, not merely in the schema. The obligation is one line:
  PARTITION A LOG BY PARTICIPANTS THAT CARRY A `:patient-id`. This
  repository's own `ehrt.sim-check.check` took exactly that line, in
  three places, in the same change.

  MADE UNDER THE WAIVER, disclosed: no deprecation release was run.
  Nothing was removed, so the deprecation clause has nothing to
  protect here in any case -- but the participant widening is the
  first change under the waiver that a consumer could actually notice
  on the wire, and saying so is what the waiver's trail is for.

  1.7.0 (2026-08-27, ADR-0174 section 2(b), arc 3b sweep 3) is
  SCHEDULING: FOUR new kinds -- `:appointment`, `:reschedule`,
  `:appointment-cancel`, `:no-show` -- plus one new OPTIONAL key,
  `:appointment-id`, on the two encounter openers (`:admission` and
  `:outpatient-visit`).

  NO BUMP IS OWED, AND THIS ONE IS TAKEN -- exactly 1.3.0's situation
  and for exactly 1.3.0's reason. `classify-change` against the frozen
  1.6.0 baseline returns `{:additive? true :breaking []}`: a new event
  kind and a new optional key are the two shapes its own docstring
  names as non-breaking, and all five changes are one or the other.
  The bump is taken anyway because `:event-schema-version` is a
  consumer's only handle on what a log it is holding can CONTAIN, and a
  1.7.0 log may carry four kinds a 1.6.0-era consumer has never seen
  and will dispatch on `:event` for. MINOR is the semver level for new,
  backward-compatible functionality.

  A 1.6.0-ERA LOG VALIDATES UNCHANGED AGAINST 1.7.0, in the strong
  sense this time and not merely the widening sense 1.6.0 could claim:
  nothing existing moved at all. `:appointment-id` is optional on both
  openers and absent from every log produced without the `:scheduling`
  opt-in.

  NONE OF THE FOUR KINDS REACHES THE WIRE in 1.7.0, and that gap is
  declared here rather than left for a reader to discover. Ruling C:
  the SIU family (S12/S14/S15/S26) is v2.4 structure, and every message
  this repository emits carries MSH-12 `\"2.3\"`. Emitting a structure
  the version field disclaims would be worse than emitting nothing, so
  the kinds are ground truth only and the MSH-12/SIU question is ROWED
  for arc 4. A consumer reading the log sees appointments; a consumer
  reading the wire does not.

  MADE UNDER THE WAIVER, disclosed: no deprecation release was run, and
  none is owed in any case, since nothing was removed."
  "1.7.0")

;; --- shared leaf schemas --------------------------------------------------
;;
;; Named once here rather than repeated per kind. All are INLINED by
;; `m/form` in the EDN export (no registry), so the export stays
;; self-contained for a non-Clojure reader.

(def PatientParticipant
  "One patient's participation in an event (`sim/ADR-0010`): a
  patient's state folds exactly the events they participate in, so
  this vector -- never `:active-mrn` -- is what a consumer partitions
  a log by. Single-element with `:role :subject` for every kind except
  `:bed-swap` (two `:subject`s) and `:merge` (`:survivor` +
  `:merged`)."
  [:map {:closed true}
   [:patient-id :string]
   [:role [:enum :subject :survivor :merged]]])

(def BedParticipant
  "A BED's participation in an event (1.6.0, arc 3b sweep 2, ADR-0174
  section 2(c)). `:bed-status-change` is the first and so far only kind
  whose subject is not a patient -- housekeeping turning a room over
  belongs to nobody -- and this is the minimal widening that lets such
  an event exist at all.

  WHY IT HAD TO EXIST. `every-event-has-participants` requires a
  non-empty `:participants`, and a cycle that lived only in the engine's
  world would be a cycle no invariant could judge, which
  `R-skeleton-or-emission` forbids for anything downstream invariants
  must respect. ADR-0173 met the same wall for PERSON events and went
  the other way -- they never became log events at all. Arc 3b could not
  take that exit, so the vocabulary widened here, in ONE place.

  A CONSUMER'S OWN OBLIGATION, stated plainly because this is the
  breaking half of 1.6.0: `:patient-id` is no longer present on every
  participant of every event. Partition a log by participants that CARRY
  one."
  [:map {:closed true}
   [:bed-id :string]
   [:ward :string]
   [:role [:enum :subject]]])

(def Participant
  "One participant in an event: a patient (`PatientParticipant`) or, as
  of 1.6.0, a bed (`BedParticipant`).

  `[:or ..]` rather than a `[:multi]`: the export is read by non-Clojure
  consumers and `m/form` renders a dispatch function as an opaque
  object, while an `:or` of two closed maps renders as data."
  [:or PatientParticipant BedParticipant])

(def Location
  "A physical placement: ward, bed, and which rung of the allocation
  ladder produced it (`docs/operational-models.md`)."
  [:map {:closed true}
   [:ward :string]
   [:bed :string]
   [:placement [:enum :licensed :surge]]])

(def PreHorizonFact
  "THE NESTED-`:event` HAZARD, named explicitly (census, 'The nested
  `:event` collision'; author ruling 2026-08-16: describe it in the
  schema as its own fact schema, do not rename anything this arc).

  A `:registered` event's `:pre-horizon-facts` are clinical facts that
  predate this run's own horizon window -- a medication still running,
  a condition still open -- carried as registration-time history
  rather than replayed as operational events
  (`ehrt.patient-simulator.compile-trajectory`'s own
  `pre-horizon-fact-types`). Each fact carries its OWN `:event` key,
  drawn from a DIFFERENT vocabulary than the log's, and four of its
  six values (`:medication-order`, `:medication-end`,
  `:care-plan-start`, `:care-plan-end`) are ALSO top-level log event
  kinds with entirely different key sets.

  A consumer that walks the EDN tree looking for `:event`, rather than
  iterating only the top-level vector, will therefore find these and
  mistake them for log events. That is the single most likely way a
  proprietary emitter gets this log wrong. This schema exists so the
  hazard is stated in the contract instead of discovered in
  production.

  `:codes` and `:references` are always PRESENT and frequently nil
  (3,471 and 5,650 of 7,236 observed) -- `compile-trajectory` conj-es
  them unconditionally rather than nil-dropping, so `[:maybe ...]`
  here is describing the tree, not permitting sloppiness."
  [:map {:closed true}
   [:event [:enum :condition-onset :condition-end
            :medication-order :medication-end
            :care-plan-start :care-plan-end]]
   [:codes [:maybe [:vector sim-model/Concept]]]
   [:citation sim-model/Citation]
   [:references [:maybe :int]]])

(def ResultEntry
  "One analyte inside a `:result-available` event's `:results`.

  `:unit`, SINGULAR since 2026-08-18 (ADR-0150, census S-6) -- the same
  spelling `:observation` and a `:diagnostic-report`'s children have
  always used for the same concept. The order-profile ANALYTE key it is
  built from remains `:units`, plural: that is a user-reachable
  `--config` surface, and `engine.clj`'s one result-construction site
  translates between them."
  [:map {:closed true}
   [:concept sim-model/Concept]
   [:unit :string]
   [:value number?]
   [:reference-range [:map {:closed true} [:low number?] [:high number?]]]
   [:abnormal-flag [:enum :normal :low :high]]])

(def BedSwapSide
  "One of the two patients in a `:bed-swap`, keyed by patient-id inside
  the event's own `:swap` map -- the one place in this log where a map
  KEY is data rather than a fixed field name. `:bed-swap` carries no
  top-level `:active-mrn` precisely because there are two.

  ARC 3B SWEEP 1: and, for the same reason, no top-level
  `:encounter-id` -- each side's own rides here, beside that side's own
  `:from`/`:to`/`:attending`. `emit-hl7`'s A17 renders two PID/PV1
  pairs and reads PV1-19 from the same entry it reads PV1-3 from."
  [:map {:closed true}
   [:active-mrn :string]
   [:from Location]
   [:to Location]
   [:attending :string]
   [:encounter-id {:optional true} :string]])

(def Residence
  "WHERE A PATIENT LIVES, AS A SUM (ADR-0173 section 2(b), arc 3a). A
  places row cannot express the absence of a residence at all, and
  `sim-model/Persona`'s own `:address` is required and non-nilable --
  widening it would move every `:registered` event in every corpus for
  a fact that belongs to state-at-t and not to a t0 sample. So the
  distinction lives here, in a three-armed sum, and NOT in the Persona.

  `:unhoused` and `:unknown` are deliberately different facts. Not
  knowing where somebody lives is not the same as their having nowhere
  to live, and ruling E1 keeps the distinction in GROUND TRUTH even
  though the wire renders PID-11 absent for both: HL7 v2 offers no code
  for either (Table 0190 has no no-fixed-address type, and the v3
  `Homeless` value set is a LIVING ARRANGEMENT concept, not an
  address), so any literal would be one site's local convention, which
  belongs in a site profile and not in the emitter's body.

  `:last-known-address` is optional on the `:unhoused` arm because a
  person can enter a run with no residence at all (`:at-t0`), having
  lost nothing."
  [:multi {:dispatch :status}
   [:housed [:map {:closed true}
             [:status [:= :housed]]
             [:address (mu/get sim-model/Persona :address)]]]
   [:unhoused [:map {:closed true}
               [:status [:= :unhoused]]
               [:last-known-address {:optional true} (mu/get sim-model/Persona :address)]]]
   [:unknown [:map {:closed true} [:status [:= :unknown]]]]])

(def DemographicValue
  "What a `:demographic-update` carries as its `:value` (and, when it
  reports one, its `:prior-value`): whichever of the three
  `engine/Demographics` fields a person event can move. The field is
  named alongside it, so a consumer dispatches on `:field` rather than
  on the value's own shape.

  1.4.0 (ADR-0173 section 2(d), arc 3a part 4) adds the identity arm.
  An `:identity-fill` reports ONE fact -- this record now belongs to a
  known person -- so its `:field` is `:identity` and its value is the
  identity state itself, with the demographics that follow riding the
  event's own `:persona`. Six separate field updates would have been
  six events reporting one thing."
  [:or Residence (mu/get sim-model/Persona :name) (mu/get sim-model/Persona :dob)
   [:enum :known :placeholder]])

(def AttemptedStep
  "The pathway-IR step a `:step-rejected` event declined to perform,
  carried verbatim. Deliberately OPEN and typed only on `:type`: it is
  whatever `ehrt.sim-model.pathway/Step` shape the caller attempted,
  including one naming a peer patient-id (`:with`) that may not exist
  -- which is exactly why it stays plain data no invariant has to
  resolve (`engine.clj`'s own `rejected-outcome` docstring)."
  [:map [:type :keyword]])

;; --- the event schema -----------------------------------------------------

(def common-entries
  "The four keys carried by EVERY event of EVERY kind, factored once.

  Derived, not assumed: the census computed this set across all 4,997
  observed events. `:active-mrn` is NOT among them -- it is absent
  from `:bed-swap`, `:merge`, and `:step-rejected` -- which is the
  single correction most likely to save a consumer a bad afternoon."
  [[:event :keyword]
   [:t :int]
   [:participants [:vector Participant]]
   [:warm-up :boolean]])

(defn- kind
  "One branch of `Event`: a closed map carrying the four common keys
  plus this kind's own. `props` must carry `:doc` (one sentence, the
  kind's meaning) and `:transition` (the state transition it drives) --
  both live HERE, with the data, because `docs/formats.md`'s event-log
  section is GENERATED from this schema and must not be able to drift
  from it.

  Closed on purpose: an unexpected key is schema drift, and a
  contract that silently tolerates drift cannot tell a consumer when
  it has changed.

  `:encounter-id` (arc 3b sweep 1, ADR-0174 section 2(a)) is declared
  here rather than kind by kind because ANY kind can occur during an
  open encounter, and because the alternative -- listing the twenty-two
  that can -- is the hand-enumeration failure mode ADR-0158 caught
  twice. It is `{:optional true}` on purpose and enforced by an
  invariant instead (`every-encounter-is-opened-and-closed-or-still-
  open`): optional in the schema so a pre-1.4.0 log validates unchanged,
  mandatory in every run-produced encounter because a gate says so. It
  is NOT in `common-entries` above, which is the set carried by every
  event of every kind -- this one rides only an event that happened
  while an encounter was open, so a run with no `:encounters` key
  carries it nowhere, and `:registered` (always a patient's FIRST event,
  before any encounter) carries it never."
  [k props & entries]
  (into [:map (assoc props :closed true)
         [:event [:= k]]
         [:t :int]
         [:participants [:vector Participant]]
         [:warm-up :boolean]
         [:encounter-id {:optional true} :string]]
        entries))

(def Event
  "One ground-truth event. Dispatches on `:event`; the 24 kinds below
  are the CLOSED vocabulary. Twenty-one are the census's own, source
  and corpora agreed; two more landed with the demographic fold
  (1.3.0, ADR-0173) and one with the bed cycle (1.6.0, ADR-0174), each
  declared here beside its producer.

  `:t` monotonicity is deliberately NOT expressed here. It is a
  RUN-level property -- true within a run, meaningless across a
  concatenation of two, and nothing in an event marks a run boundary
  -- so it lives in `run-t-monotone?` below and is asserted over a
  whole log, never per event."
  (into
   [:multi {:dispatch :event
            :doc "One ground-truth event. The log is a vector of these, in run order."}]
   [[:registered
     (kind :registered
           {:doc "A patient enters the run: identity assigned, demographics sampled, any pre-horizon clinical history attached."
            :transition "Creates the patient's fold origin; :status stays :new."}
           [:active-mrn :string]
           [:persona sim-model/Persona]
           ;; Present only when the patient's compiled module produced
           ;; registration facts -- `engine.clj`'s own `cond->`.
           [:pre-horizon-facts {:optional true} [:vector PreHorizonFact]]
           ;; 1.3.0 (ADR-0173 ruling E1, arc 3a part 3): present ONLY for
           ;; an arrival bound to a person who is not HOUSED at their own
           ;; registration instant. Absent -- every event of every run
           ;; with no `:persons` key -- means housed at the Persona's own
           ;; `:address`, which is what `:persona` has always meant.
           [:residence {:optional true} Residence]
           ;; 1.4.0 (ADR-0173 sections 2(c)/2(d), arc 3a part 4).
           ;;
           ;; `:person-id` is the person-process id of whoever this
           ;; patient is -- present for every registration an arrival
           ;; BOUND to a person produced, and what
           ;; `identification-merge-survivor-is-the-persons-prior-patient`
           ;; reads on both sides of an identification merge. It is a
           ;; STAMP into a different id space, never a patient-id: the
           ;; two spaces are deliberately unlike so a reader cannot join
           ;; them by string equality.
           [:person-id {:optional true} :string]
           ;; The next three ride a PLACEHOLDER registration and nothing
           ;; else: an arrival that landed inside an open
           ;; `:identity-unavailable` window -- the author's own
           ;; "unhoused unresponsive John Does". `:alias-name` is what
           ;; the wire renders in PID-5; `:residence` above is
           ;; `:unknown`, so PID-11 is absent; `:persona` still carries
           ;; the truth, because ground truth knows who an unidentified
           ;; patient is even while the modelled hospital does not.
           ;; `:window-close-t` is when identification is DUE, and is
           ;; what lets `every-placeholder-registration-is-resolved-or-
           ;; still-open` tell a dangling placeholder from one the run
           ;; simply ended inside.
           [:identity {:optional true} [:enum :placeholder]]
           [:alias-name {:optional true} (mu/get sim-model/Persona :name)]
           [:window-close-t {:optional true} :int]
           ;; The mother-baby link (ADR-0173 section 2(c),
           ;; `docs/dev/traffic-model.md`: "the newborn's first encounter
           ;; is the birth"). Present on a NEWBORN's registration, naming
           ;; the parent's own patient. A plain field and NOT a second
           ;; `:participants` entry, deliberately: participants are who
           ;; the event's state fold applies to, and a birth does not
           ;; re-register the mother.
           [:mother-patient-id {:optional true} :string])]

    [:admission
     (kind :admission
           {:doc "A patient is admitted to a bed, allocated by the ward ladder."
            :transition ":new -> :admitted; sets :location, :home-ward, :attending, :admitted-at."}
           [:active-mrn :string]
           [:attending :string]
           [:home-ward :string]
           [:location Location]
           [:forced :boolean]
           ;; WIDER THAN THE CENSUS, on purpose. All 692 observed
           ;; admissions carried either a string or nil -- but the
           ;; reason rides straight through from the step, and
           ;; `sim-model/Step`'s own `:admission` declares
           ;; `[:or :string Concept]`. The constructor wins over the
           ;; observation: a Concept reason is emittable today by a
           ;; hand-authored pathway, and a schema that rejected it
           ;; would be describing our corpora rather than our engine.
           ;; OPTIONAL since 1.2.0 (census S-1 fixed, ADR-0151): a
           ;; hand-authored step's reason rides through and the key is
           ;; PRESENT; a module-compiled encounter has no reason to
           ;; give, so the key is ABSENT rather than present-and-nil.
           ;; `[:maybe ...]` is retained deliberately -- a 1.1.0-era
           ;; log still validates, so this is a widening for readers
           ;; and only the WRITER changed.
           [:reason {:optional true} [:maybe [:or :string sim-model/Concept]]]
           [:citation {:optional true} sim-model/Citation]
           [:conditions {:optional true} [:vector sim-model/ConditionAnnotation]]
           ;; 1.4.0 (ADR-0173 section 2(c)): present when this admission
           ;; came from a person-stream HOOK -- a delivery or an
           ;; occupational injury -- and absent for every other
           ;; admission. A provenance STAMP, gated as one by
           ;; `person-scoped-provenance-is-a-stamp-not-a-reference`, and
           ;; what makes hook-created traffic countable in a corpus.
           [:person-event-id {:optional true} :string]
           ;; 1.7.0 (ADR-0174 section 2(b)): present when this opener was
           ;; KEPT against a booking, absent on every walk-in. That single
           ;; field is what makes `scheduled-encounter-follows-its-
           ;; appointment` non-vacuous -- and it is non-vacuous only
           ;; because sweep 1's encounter horizon landed, since without a
           ;; SECOND encounter every appointment would trivially precede
           ;; its patient's first and only visit.
           [:appointment-id {:optional true} :string])]

    [:transfer
     (kind :transfer
           {:doc "An admitted patient moves to another bed, either by a pathway step or because a bed they were waiting for came free."
            :transition "Stays :admitted; rewrites :location and :home-ward."}
           [:active-mrn :string]
           [:attending :string]
           [:from Location]
           [:location Location]
           [:home-ward :string]
           [:forced :boolean]
           [:bed-ready :boolean]
           ;; Present IFF :bed-ready is true -- 165 observed, no
           ;; exceptions. The bed-ready transfer (emitted by another
           ;; patient's discharge) carries it; the ordinary one does
           ;; not, because its placement already rides :location.
           [:placement {:optional true} [:enum :licensed :surge]])]

    [:discharge
     (kind :discharge
           {:doc "A patient leaves; an expired disposition marks a death, which vacates no bed."
            :transition ":admitted -> :discharged (or :expired); sets :discharged-at."}
           [:active-mrn :string]
           [:attending :string]
           [:location Location]
           [:citation {:optional true} sim-model/Citation]
           ;; The death path. Rides only when the compiled step carries
           ;; it -- 1 of 689 observed, and 0 in anything the docs
           ;; teach, which is why the census had to build a corpus
           ;; specifically to reach it.
           [:disposition {:optional true} [:enum :expired]]
           [:codes {:optional true} [:vector sim-model/Concept]])]

    [:cancel-admit
     (kind :cancel-admit
           {:doc "An admission is retracted as never having happened (HL7v2 A11)."
            :transition ":admitted -> :new; clears :location and :home-ward."}
           [:active-mrn :string]
           [:cancels-event-id :int])]

    [:cancel-transfer
     (kind :cancel-transfer
           {:doc "A transfer is retracted and the patient reinstated to where they were (HL7v2 A12)."
            :transition "Stays :admitted; restores the pre-transfer :location and :home-ward."}
           [:active-mrn :string]
           [:cancels-event-id :int]
           [:home-ward [:maybe :string]]
           [:location [:maybe Location]]
           ;; Present only for the atomic transfer-in-error pair, where
           ;; the cancel was decided in the same call as its transfer.
           [:in-error {:optional true} :boolean])]

    [:cancel-discharge
     (kind :cancel-discharge
           {:doc "A discharge is retracted and the patient reinstated (HL7v2 A13)."
            :transition ":discharged -> :admitted; restores :location, :home-ward, :attending."}
           [:active-mrn :string]
           [:cancels-event-id :int]
           [:home-ward [:maybe :string]]
           [:location [:maybe Location]]
           [:attending [:maybe :string]])]

    [:bed-swap
     (kind :bed-swap
           {:doc "Two admitted patients exchange beds in one atomic event (HL7v2 A17)."
            :transition "Both stay :admitted; each takes the other's :location."}
           ;; No :active-mrn: two subjects, two MRNs, both inside :swap.
           [:swap [:map-of :string BedSwapSide]])]

    ;; 1.6.0 (arc 3b sweep 2, ADR-0174 section 2(c)): ONE kind for the
    ;; whole bed cycle, many causes -- the same choice 1.3.0 made for
    ;; `:demographic-update` rather than minting a kind per transition.
    ;; Three kinds would each need an `evolve`, a schema branch, an
    ;; oracle mover-set prediction and a place in this closed vocabulary.
    [:bed-status-change
     (kind :bed-status-change
           {:doc "A bed changes housekeeping status: vacated to :dirty, then :cleaning, then :ready (HL7v2 A20)."
            :transition "Changes no patient's state at all; the bed's own status moves :from -> :to."}
           ;; No :active-mrn and no patient participant: this event's
           ;; subject is the BED (`BedParticipant`).
           [:bed :string]
           [:ward :string]
           [:from [:enum :ready :occupied :dirty :cleaning]]
           [:to [:enum :ready :occupied :dirty :cleaning]]
           ;; Who LEFT the bed -- carried on the `:dirty` transition
           ;; alone, because the two later legs are housekeeping's and
           ;; belong to nobody. It is a plain id, never a participant:
           ;; the patient's own state folds nothing from this event.
           [:last-patient-id {:optional true} :string])]

    ;; --- 1.7.0 (arc 3b sweep 3, ADR-0174 section 2(b)): SCHEDULING, as
    ;; four skeleton kinds. NONE OF THE FOUR RENDERS A MESSAGE in this
    ;; version (ruling C): the SIU family is v2.4 structure and every
    ;; message this repo emits carries MSH-12 "2.3", which is a real gap
    ;; and is ROWED for arc 4 rather than papered over by emitting a
    ;; structure the version field disclaims.
    ;;
    ;; They are EVENTS and not `PatientState` fields alone because
    ;; `R-skeleton-or-emission` decides it: downstream invariants must
    ;; respect them, so they are generated AND judged.

    [:appointment
     (kind :appointment
           {:doc "A future visit is booked for a patient (HL7v2 SIU^S12 -- deliberately unrendered in 1.7.0, see ruling C)."
            :transition "Opens the patient's :appointment record; terminal only when an encounter keeps it, a cancel closes it, or a no-show closes it."}
           [:active-mrn :string]
           [:appointment-id :string]
           ;; The instant the visit is DUE, always in the future of :t.
           [:scheduled-t :int]
           [:appointment-class [:enum :inpatient :emergency :outpatient
                                :preadmit :recurring :obstetrics]]
           [:reason {:optional true} :string]
           [:citation {:optional true} sim-model/Citation])]

    [:reschedule
     (kind :reschedule
           {:doc "A booked appointment moves to a different instant (HL7v2 SIU^S14 -- deliberately unrendered in 1.7.0)."
            :transition "Moves :scheduled-t on the OPEN record and is NOT terminal; the id is kept rather than re-minted."}
           [:active-mrn :string]
           ;; THE SAME ID, never a new one pointing back at the old.
           ;; SCH-1/SCH-2 are stable placer/filler ids across the SIU
           ;; family, and :prior-value/:value on ONE record is already
           ;; this repo's shape for a change
           ;; (`demographic-update-reports-a-real-change`).
           [:appointment-id :string]
           [:prior-scheduled-t :int]
           [:scheduled-t :int])]

    [:appointment-cancel
     (kind :appointment-cancel
           {:doc "A booked appointment is cancelled before its instant (HL7v2 SIU^S15 -- deliberately unrendered in 1.7.0)."
            :transition "Closes the open record TERMINALLY, outcome :cancelled. No encounter follows."}
           [:active-mrn :string]
           [:appointment-id :string])]

    [:no-show
     (kind :no-show
           {:doc "A booked appointment's instant arrives and the patient does not (HL7v2 SIU^S26 -- deliberately unrendered in 1.7.0)."
            :transition "Closes the open record TERMINALLY, outcome :no-show, and opens NOTHING -- which is exactly why a no-show cannot be derived from an encounter."}
           [:active-mrn :string]
           [:appointment-id :string])]

    [:merge
     (kind :merge
           {:doc "Two patient records are found to be one person; the survivor absorbs the merged record's MRNs (HL7v2 A40)."
            :transition "Survivor keeps its status and gains the merged MRNs; the merged patient becomes :merged and emits nothing further."}
           [:surviving-mrn :string]
           [:merged-mrn :string]
           [:merged-mrns [:set :string]]
           ;; 1.4.0 (ADR-0173 section 2(d)): an IDENTIFICATION merge --
           ;; a placeholder record absorbed into the person's prior
           ;; patient -- carries `:cause :identification` and the
           ;; resolution's own provenance stamp. A CHURN merge carries
           ;; neither, which is what keeps the two families
           ;; distinguishable while everything else about the event is
           ;; deliberately identical: same kind, same roles, same MRN
           ;; payload, so every merge invariant applies unchanged.
           [:cause {:optional true} [:enum :identification]]
           [:person-event-id {:optional true} :string])]

    [:order-placed
     (kind :order-placed
           {:doc "A diagnostic order is placed against an order profile."
            :transition "No state change; the log itself is the record."}
           [:active-mrn :string]
           [:profile :keyword]
           [:concept sim-model/Concept]
           [:location Location]
           [:attending :string])]

    [:result-available
     (kind :result-available
           {:doc "An order's results come back, one entry per analyte, with abnormal flags already computed against each reference range."
            :transition "Appends to the patient's :observations accumulator."}
           [:active-mrn :string]
           [:profile :keyword]
           ;; Index into THIS log of the :order-placed event this
           ;; answers -- a log position, meaningless outside the log
           ;; it came from.
           [:order-event-id :int]
           [:concept sim-model/Concept]
           [:location Location]
           [:attending :string]
           [:results [:vector ResultEntry]])]

    [:outpatient-visit
     (kind :outpatient-visit
           {:doc "An ambulatory encounter opens; it occupies no bed (HL7v2 A04)."
            :transition ":new -> :admitted with :class :outpatient and a nil :location -- the one sanctioned admitted-without-a-bed case."}
           [:active-mrn :string]
           [:attending :string]
           ;; OPTIONAL since 1.2.0 -- see :admission above; same key,
           ;; same fix (census S-1, ADR-0151). All 221 observed were
           ;; module-compiled and so emit no key at all now.
           [:reason {:optional true} [:maybe [:or :string sim-model/Concept]]]
           [:citation {:optional true} sim-model/Citation]
           [:conditions {:optional true} [:vector sim-model/ConditionAnnotation]]
           ;; 1.7.0 (ADR-0174 section 2(b)): present when this opener was
           ;; KEPT against a booking, absent on every walk-in. That single
           ;; field is what makes `scheduled-encounter-follows-its-
           ;; appointment` non-vacuous -- and it is non-vacuous only
           ;; because sweep 1's encounter horizon landed, since without a
           ;; SECOND encounter every appointment would trivially precede
           ;; its patient's first and only visit.
           [:appointment-id {:optional true} :string])]

    [:outpatient-visit-end
     (kind :outpatient-visit-end
           {:doc "An ambulatory encounter closes. Deliberately renders no HL7 message -- many real ambulatory feeds send an A04 and nothing else."
            :transition ":admitted -> :discharged; sets :discharged-at."}
           [:active-mrn :string]
           [:attending [:maybe :string]]
           [:citation {:optional true} sim-model/Citation])]

    [:procedure
     (kind :procedure
           {:doc "A procedure is performed, cited back to the module state that produced it."
            :transition "No state change; the log itself is the record."}
           [:active-mrn :string]
           [:codes [:vector sim-model/Concept]]
           [:citation {:optional true} sim-model/Citation])]

    [:observation
     (kind :observation
           {:doc "An unsolicited clinical finding, not tied to any order -- a single measured or coded value."
            :transition "Appends one entry to the patient's :observations accumulator."}
           [:active-mrn :string]
           [:codes [:vector sim-model/Concept]]
           ;; The value family: every field optional, because a module
           ;; Observation state with no range carries a concept and
           ;; nothing else rather than a fabricated value.
           [:value {:optional true} number?]
           [:unit {:optional true} :string]
           [:value-code {:optional true} sim-model/Concept]
           [:category {:optional true} :string]
           [:reference-range {:optional true} [:map {:closed true} [:low number?] [:high number?]]]
           [:interpretation {:optional true} [:enum :normal :low :high]]
           [:citation {:optional true} sim-model/Citation])]

    [:diagnostic-report
     (kind :diagnostic-report
           {:doc "A panel of observations reported together as one document -- ONE event carrying all children, never one event per child."
            :transition "Appends one :observations entry per child."}
           [:active-mrn :string]
           [:observations [:vector sim-model/ObservationEntry]]
           [:codes {:optional true} [:vector sim-model/Concept]]
           [:citation {:optional true} sim-model/Citation])]

    [:medication-order
     (kind :medication-order
           {:doc "A medication is prescribed."
            :transition "Opens an :active entry in the patient's :medication-orders accumulator."}
           [:active-mrn :string]
           [:codes [:vector sim-model/Concept]]
           [:citation {:optional true} sim-model/Citation])]

    [:medication-end
     (kind :medication-end
           {:doc "A medication course ends, resolved to its order by CITATION rather than by log position."
            :transition "Closes the matching :medication-orders entry."}
           [:active-mrn :string]
           ;; Nilable by design: the order may legitimately be a
           ;; :pre-horizon-facts entry on :registered rather than a
           ;; log event, in which case there is no index to point at
           ;; (`check.clj`'s own straddle allowance). Census S-3
           ;; records that both observed events had it nil.
           [:order-event-id [:maybe :int]]
           [:order-citation [:maybe sim-model/Citation]]
           [:citation {:optional true} sim-model/Citation])]

    [:care-plan-start
     (kind :care-plan-start
           {:doc "A care plan is opened, optionally listing its planned activities."
            :transition "Opens an :active entry in the patient's :care-plans accumulator."}
           [:active-mrn :string]
           [:codes [:vector sim-model/Concept]]
           [:activities {:optional true} [:vector sim-model/Concept]]
           [:citation {:optional true} sim-model/Citation])]

    [:care-plan-end
     (kind :care-plan-end
           {:doc "A care plan closes, resolved to its start by CITATION rather than by log position."
            :transition "Closes the matching :care-plans entry."}
           [:active-mrn :string]
           ;; Both nilable for the same reason `:medication-end`'s pair
           ;; is: the start may be a `:pre-horizon-facts` entry rather
           ;; than a log event. The census saw all seven observed
           ;; events with both nil -- but that was the vendored
           ;; modules, not the mechanism: those closures cite their
           ;; start by `referenced_by_attribute`, a resolution
           ;; `ehrt.patient-simulator.gmf` deliberately never declared for
           ;; the CarePlan family. A `:careplan`-citing module resolves
           ;; both, which `event-schema-test`'s own fixture proves.
           ;; Census register row S-2.
           [:start-event-id [:maybe :int]]
           [:care-plan-citation [:maybe sim-model/Citation]]
           [:citation {:optional true} sim-model/Citation])]

    [:step-rejected
     (kind :step-rejected
           {:doc "A step was attempted and declined as illegal for this patient's current state -- truth about the run, never wire traffic."
            :transition "None, by construction: evolve folds it as identity."}
           ;; No :active-mrn: nothing became a real action.
           ;; The enum is READ FROM THE ENGINE, not from the census.
           ;; Only 1 of the 7 documented reasons occurred across five
           ;; churn seeds (census S-4), and a schema narrowed to what
           ;; happened to occur would reject a legal log.
           [:reason (into [:enum] (sort engine/documented-step-rejection-reasons))]
           [:attempted-step AttemptedStep])]

    ;; --- 1.3.0: the two kinds the person stream mints ------------------
    ;;
    ;; ADR-0173 section 2(b). The person process has FIFTEEN event kinds;
    ;; this vocabulary grows by exactly TWO, and the gap is the design.
    ;; Person events are the engine's INPUT, in the same relation to
    ;; ground truth that pathway IR and a compiled trajectory already
    ;; have -- they carry no `:patient-id` and could not satisfy
    ;; `every-event-has-participants` or `participant-ids-exist-in-run`
    ;; without inventing a second participant vocabulary. Eleven of the
    ;; fifteen mint nothing at all; four fold onto these two.

    [:demographic-update
     (kind :demographic-update
           {:doc "One demographic fact about a patient changed between encounters: an address, a legal name, a corrected date of birth. Deliberately renders no HL7 message of its own in 1.3.0 -- the change is visible in the PID of every message the patient receives after it."
            :transition "Writes one field of :demographics; :persona (the t0 sample) is untouched."}
           [:active-mrn :string]
           ;; Which person-side fact caused it. `:identity-fill` LANDED
           ;; WITH ITS PRODUCER in 1.4.0 (arc 3a part 4), which is what
           ;; 1.3.0's own note here said would happen -- declaring it
           ;; earlier would have been a schema describing a future.
           [:cause [:enum :residence-move :residence-loss :identity-correction
                    :identity-fill]]
           [:field [:enum :residence :name :dob :identity]]
           [:value DemographicValue]
           ;; The folded state immediately before this event. OPTIONAL
           ;; because a hand-authored log may carry none, and because a
           ;; correction of a field never previously set has no prior.
           ;; `check.clj`'s `demographic-update-reports-a-real-change` is
           ;; what makes it honest when it IS present.
           [:prior-value {:optional true} DemographicValue]
           ;; A PROVENANCE STAMP, not a log reference: the person
           ;; stream's own "<person-id>#<n>" string. `check.clj`'s
           ;; `person-scoped-provenance-is-a-stamp-not-a-reference` is
           ;; the gate that keeps it one.
           [:person-event-id :string]
           ;; 1.4.0 (ADR-0173 section 2(d)): the three keys an
           ;; `:identity-fill` carries and no other cause does.
           ;;
           ;; `:placeholder-event-id` IS a log index -- the one
           ;; referential key this arc mints -- pointing at the
           ;; `:registered` event that opened this record as a
           ;; placeholder; `identity-fill-references-its-placeholder-
           ;; registration` is its gate, and it is the exact shape
           ;; `:medication-end`'s `:order-event-id` already has.
           ;; `:persona` is who the patient turned out to be, and
           ;; `:residence` rides beside it only when they are not housed
           ;; -- the same nil-dropping pair `:registered` carries, for
           ;; the same reason (a Persona's `:address` is required and
           ;; non-nilable, so the sum lives outside it).
           [:placeholder-event-id {:optional true} [:maybe :int]]
           [:persona {:optional true} sim-model/Persona]
           [:residence {:optional true} Residence])]

    [:coverage-change
     (kind :coverage-change
           {:doc "A patient's insurance coverage changed: a new payer, with the payer they held before it. Deliberately renders no HL7 message of its own in 1.3.0 -- the change is visible in the IN1 of the next admission message the patient receives."
            :transition "Writes :payer in :demographics; :persona (the t0 sample) is untouched."}
           [:active-mrn :string]
           [:cause [:enum :employment :age-65 :loss :eligibility]]
           [:payer (mu/get sim-model/Persona :payer)]
           [:prior-payer {:optional true} (mu/get sim-model/Persona :payer)]
           [:person-event-id :string])]]))

(def GroundTruth
  "A whole run's log. `:t` monotonicity is asserted separately, by
  `run-t-monotone?` -- see `Event`'s own docstring for why it cannot
  be a per-event constraint."
  [:vector Event])

(defn valid-event? [event] (m/validate Event event))
(defn explain-event [event] (m/explain Event event))
(defn valid-ground-truth? [ground-truth] (m/validate GroundTruth ground-truth))

(defn run-t-monotone?
  "The RUN-level time property: within one run, event times never
  decrease. True of the empty log and of any single-event log.

  This is the guarantee that makes `ehrt corpus play` able to pace a
  log by `:t` alone, and the reason a consumer may stream the vector
  in order rather than sorting it. It is NOT true of two runs
  concatenated, and nothing in an event marks a run boundary."
  [ground-truth]
  (or (< (count ground-truth) 2)
      (apply <= (map :t ground-truth))))

;; --- the committed EDN export, and the change gate ------------------------

;; TWO artifacts, and the reason is not obvious enough to leave
;; implicit. `event-schema.edn` is the CURRENT contract -- regenerated
;; by `make docsgen` on every change and diffed by CI, so the published
;; EDN can never lag the Clojure source. `event-schema-baseline.edn` is
;; the FROZEN last-versioned contract -- regenerated only when
;; `schema-version` is bumped.
;;
;; One file cannot do both jobs. If the gate compared the source
;; against an artifact regenerated in the same commit, the diff would
;; be empty by construction and the gate would be theatre: it could
;; only ever confirm the schema agrees with itself, the same failure
;; mode `ehrt.sim.manifest`'s own retired mirror is the standing
;; lesson for. The baseline is the only thing here that holds still
;; long enough to be a contract.

(def export-resource-path "sim-engine/event-schema.edn")
(def baseline-resource-path "sim-engine/event-schema-baseline.edn")

(defn- portable
  "Rewrites `m/form` output into something `clojure.edn/read-string`
  can actually read.

  ONE rule today, and it is not cosmetic: a compiled regex renders as
  a `#\"...\"` literal, which is a CLOJURE reader feature, not EDN.
  `clojure.edn/read-string` rejects it outright -- found by the parity
  test failing to read this namespace's own export, not by reasoning
  -- so an artifact carrying one would be unreadable by exactly the
  non-Clojure consumer it exists for. `Persona`'s `:dob`, `:phone`,
  and `:ssn` each carry one.

  Rewritten to `[:re \"<pattern>\"]` with the pattern as a plain
  string, which malli accepts identically, so the export stays
  loadable AS A SCHEMA by a Clojure consumer while becoming readable
  as DATA by everyone else. The pattern syntax is
  `java.util.regex`; `docs/formats.md` says so, because a consumer in
  another language needs to know whose regex dialect they are being
  handed.

  Also makes the artifact comparable at all: two `Pattern` objects
  with identical source are never `=`, so a parity test over
  un-normalized forms could not have worked either."
  [form]
  (clojure.walk/postwalk
   (fn [x] (if (instance? java.util.regex.Pattern x) (str x) x))
   form))

(defn export
  "The contract as plain, self-contained data -- what gets written to
  both resources (author ruling Q-B (a)). Every referenced schema is
  inlined by `m/form`, so a non-Clojure consumer reads the whole
  contract without a registry and without running Clojure -- and
  `portable` (above) makes that literally true rather than nearly
  true."
  []
  {:event-schema-version schema-version
   :schema (portable (m/form Event))})

(defn committed-export
  "The current contract as committed. Must equal `export` exactly --
  `event-schema-test`'s own parity assertion, and `make docsgen`'s job
  to keep true."
  []
  (edn/read-string (slurp (io/resource export-resource-path))))

(defn committed-baseline
  "The last VERSIONED contract, frozen. What the change gate compares
  the live schema against, and the only artifact here that deliberately
  does not track the source."
  []
  (edn/read-string (slurp (io/resource baseline-resource-path))))

(def ^:private export-header
  ";; GENERATED -- do not edit by hand.\n;;\n;; The ground-truth event log's contract, exported as plain,\n;; self-contained data from ehrt.sim-engine.event-schema (author\n;; ruling Q-B (a), 2026-08-16: the Clojure source is the source of\n;; truth AND it is exported to EDN, with a parity test). Every\n;; referenced schema is inlined -- no malli registry -- so a\n;; non-Clojure consumer can read the whole contract without running\n;; Clojure. EDN is primary; JSON, when it lands, is a projection of\n;; this file under rules stated in docs/formats.md.\n;;\n;; Regenerate with `make event-schema-export` (also run by `make\n;; docsgen`, so CI's freshness diff catches a stale export).\n")

(def ^:private baseline-header
  ";; GENERATED and FROZEN -- do not edit by hand, and do not\n;; regenerate as a matter of routine.\n;;\n;; This is the last VERSIONED event contract: the baseline\n;; ehrt.sim-engine.event-schema-test measures the live schema against\n;; to decide whether a change was additive (non-breaking, no version\n;; bump owed) or not (bump owed). It is deliberately NOT on `make\n;; docsgen`: an artifact regenerated alongside the source it is meant\n;; to check would make the gate compare the schema against itself.\n;;\n;; Re-freeze ONLY when bumping schema-version, with\n;; `make event-schema-freeze`.\n")

(defn write-export!
  "Writes `export` to the source tree. `make event-schema-export`."
  [_]
  (spit "components/sim-engine/resources/sim-engine/event-schema.edn"
        (str export-header (with-out-str (pp/pprint (export))))))

(defn write-baseline!
  "Freezes `export` as the change gate's baseline. `make
  event-schema-freeze` -- run ONLY when bumping `schema-version`."
  [_]
  (spit "components/sim-engine/resources/sim-engine/event-schema-baseline.edn"
        (str baseline-header (with-out-str (pp/pprint (export))))))

(defn- branches
  "form of a [:multi ...] -> {kind {key [optional? value-form]}}."
  [multi-form]
  (into {}
        ;; `(rest multi-form)` leads with the properties map
        ;; ({:dispatch :event ...}), which must be skipped BEFORE
        ;; destructuring -- sequential destructuring of a map throws
        ;; rather than yielding nil, so a `:when` guard alone is too
        ;; late.
        (for [branch (rest multi-form)
              :when (vector? branch)
              :let [[k map-form] branch]
              :when (keyword? k)]
          [k (into {}
                   (for [entry (rest map-form)
                         :when (vector? entry)]
                     (let [[key & more] entry
                           props (when (map? (first more)) (first more))
                           value (if props (second more) (first more))]
                       [key [(boolean (:optional props)) value]])))])))

(defn classify-change
  "Compares a baseline export against a candidate one and returns
  `{:additive? bool :breaking [reason ...]}` -- the mechanical
  definition of the stability promise `schema-version` makes.

  ADDITIVE (non-breaking, no bump owed): a new event kind; a new
  OPTIONAL key on an existing kind. A consumer written against the
  baseline keeps working, because nothing it already read moved.

  BREAKING (bump owed): a kind removed; a key removed; an optional key
  made required; a required key made optional; any key's value schema
  changed; a new REQUIRED key on an existing kind (which invalidates
  every event a baseline-era producer emitted).

  Deliberately conservative -- a genuinely-widening value change (say
  `:string` to `[:or :string Concept]`) is reported as breaking rather
  than reasoned about. A false alarm costs a version bump and a
  sentence in `docs/formats.md`; a missed break costs a consumer."
  [baseline candidate]
  (let [b (branches (:schema baseline))
        c (branches (:schema candidate))
        removed-kinds (set/difference (set (keys b)) (set (keys c)))
        breaking
        (concat
         (for [k (sort removed-kinds)]
           (str "event kind removed: " k))
         (apply concat
                (for [k (sort (set/intersection (set (keys b)) (set (keys c))))
                      :let [bk (get b k) ck (get c k)]]
                  (concat
                   (for [key (sort (set/difference (set (keys bk)) (set (keys ck))))]
                     (str k ": key removed: " key))
                   (for [key (sort (set/difference (set (keys ck)) (set (keys bk))))
                         :when (not (first (get ck key)))]
                     (str k ": new REQUIRED key: " key
                          " (add it as {:optional true} to stay additive)"))
                   (for [key (sort (set/intersection (set (keys bk)) (set (keys ck))))
                         :let [[b-opt b-val] (get bk key)
                               [c-opt c-val] (get ck key)]
                         :when (or (not= b-opt c-opt) (not= b-val c-val))]
                     (str k ": key changed: " key
                          (cond
                            (and b-opt (not c-opt)) " (optional -> required)"
                            (and (not b-opt) c-opt) " (required -> optional)"
                            :else " (value schema changed)")))))))]
    {:additive? (empty? breaking)
     :breaking (vec breaking)}))
