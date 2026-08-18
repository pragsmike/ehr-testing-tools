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
            [malli.core :as m]))

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
  rewritten."
  "1.1.0")

;; --- shared leaf schemas --------------------------------------------------
;;
;; Named once here rather than repeated per kind. All are INLINED by
;; `m/form` in the EDN export (no registry), so the export stays
;; self-contained for a non-Clojure reader.

(def Participant
  "One patient's participation in an event (`sim/ADR-0010`): a
  patient's state folds exactly the events they participate in, so
  this vector -- never `:active-mrn` -- is what a consumer partitions
  a log by. Single-element with `:role :subject` for every kind except
  `:bed-swap` (two `:subject`s) and `:merge` (`:survivor` +
  `:merged`)."
  [:map {:closed true}
   [:patient-id :string]
   [:role [:enum :subject :survivor :merged]]])

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
  (`ehrt.sim-trajectory.compile-trajectory`'s own
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
  top-level `:active-mrn` precisely because there are two."
  [:map {:closed true}
   [:active-mrn :string]
   [:from Location]
   [:to Location]
   [:attending :string]])

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
  it has changed."
  [k props & entries]
  (into [:map (assoc props :closed true)
         [:event [:= k]]
         [:t :int]
         [:participants [:vector Participant]]
         [:warm-up :boolean]]
        entries))

(def Event
  "One ground-truth event. Dispatches on `:event`; the 21 kinds below
  are the CLOSED vocabulary, source and census agreed.

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
           [:pre-horizon-facts {:optional true} [:vector PreHorizonFact]])]

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
           ;; Always PRESENT, nil for every module-compiled encounter
           ;; (census S-1, a register row, not fixed here).
           [:reason [:maybe [:or :string sim-model/Concept]]]
           [:citation {:optional true} sim-model/Citation]
           [:conditions {:optional true} [:vector sim-model/ConditionAnnotation]])]

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

    [:merge
     (kind :merge
           {:doc "Two patient records are found to be one person; the survivor absorbs the merged record's MRNs (HL7v2 A40)."
            :transition "Survivor keeps its status and gains the merged MRNs; the merged patient becomes :merged and emits nothing further."}
           [:surviving-mrn :string]
           [:merged-mrn :string]
           [:merged-mrns [:set :string]])]

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
           ;; Always present, nil in all 221 observed -- see S-1 on
           ;; :admission above; same key, same register row.
           [:reason [:maybe [:or :string sim-model/Concept]]]
           [:citation {:optional true} sim-model/Citation]
           [:conditions {:optional true} [:vector sim-model/ConditionAnnotation]])]

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
           ;; `ehrt.sim-trajectory.gmf` deliberately never declared for
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
           [:attempted-step AttemptedStep])]]))

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
