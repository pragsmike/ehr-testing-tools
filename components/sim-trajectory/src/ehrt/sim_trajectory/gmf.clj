(ns ehrt.sim-trajectory.gmf
  "The GMF module loader (Milestone M5a Task 1, docs/gmf-interpreter.md
  section 1). Parses a Synthea Generic Module Framework JSON module,
  normalizes it to this project's own idiom (kebab-case keyword keys and
  state-name references, code systems as :snomed/:loinc/:rxnorm/:icd10cm/
  :cvx keywords per sim-model/Concept), and validates it
  against the v1 subset docs/gmf-interpreter.md section 1 defines.

  Load-time enforcement, result-not-throw (ehrt.kernel.result):
  a module using a state type OUTSIDE v1's subset (Counter,
  ImagingStudy -- section 1's own deferred-type table;
  CallSubmodule/Device/DeviceEnd/Death all joined v1 across M5b and the
  GMF coverage waves, MultiObservation/DiagnosticReport joined v1 at
  GMF coverage Wave D stage D1, CarePlanStart/CarePlanEnd joined v1 at
  GMF coverage Wave D stage D2, ADR-0029)
  is REJECTED with
  :unsupported-state-type, never silently skipped and never thrown -- this
  is a stricter, mechanical gate than the informal 'read past what you
  don't execute' survey-reading section 1 also describes (that describes
  reading a module's states for SURVEY purposes, e.g. the design doc's own
  candidate-module appendix; this loader is the boundary a module crosses
  to actually be RUN, where `sim/ADR-0013` point 4's curation criterion applies
  in full: any deferred-type use fails it, full stop). A module whose own
  SetAttribute/Symptom writes a bare (non-namespaced) attribute name
  colliding with an engine-reserved key (`:donor`, docs/patient-state-
  model.md's post-mortem entry) is REJECTED with :attribute-collision
  (section 5). Every attribute write compiles to a MODULE-NAMESPACED
  keyword (section 5) -- `:fixture-clinic/onset-logged`, never a bare
  `:onset-logged` -- so cross-module collisions are structurally
  impossible, per that section's own argument; only a bare reserved-key
  write is a real, checkable collision.

  The loaded set is listable (`loaded-modules`) -- no hidden modules,
  section 5's own corollary applied to module content specifically."
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-model.interface :as sim-model]
            [malli.core :as m]))

;; --- Normalization: JSON's snake_case/CamelCase -> this project's kebab
;; keyword idiom -----------------------------------------------------------

(defn slug
  "Any raw GMF name string (a JSON key, a state name, an attribute or
  symptom name) -> this project's own lower-kebab form -- 'Check_Age_Guard'
  and 'Nasal Congestion' both become the same shape ('check-age-guard',
  'nasal-congestion'), so state-map keys, transition-target references,
  and attribute names all compare and namespace uniformly. Public: also
  reused by ehrt.sim-trajectory.gmf-interpreter to turn a Guard/conditional's
  raw :attribute name into the SAME module-namespaced key this loader's
  own `declared-attributes` computes (one transform, one place)."
  [s]
  (-> s str/lower-case (str/replace #"[_\s]+" "-")))

(defn- kebab-key
  "clojure.data.json's :key-fn -- applied to EVERY JSON object key at
  every depth, so 'direct_transition', 'condition_type', a state's own
  name ('Check_Age_Guard'), and every other JSON key normalize uniformly
  in one parse pass."
  [s]
  (keyword (slug s)))

(def ^:private gmf-type->keyword
  "v1's state-type subset (docs/gmf-interpreter.md section 1, `Symptom`
  ratified into v1 alongside the rest -- see that document's own
  closing ratification record). Any :type string NOT a key here is a
  deferred type (section 1's own table) -- `unsupported-state-type`,
  below, is exactly 'not a key in this map.'

  M5b finding: `Device`/`DeviceEnd` join v1 here too, as consumed-
  internally states structurally identical to `Simple` -- discovered
  necessary when the ratified vendored module (sinusitis.json,
  sim/ADR-0013/docs/gmf-interpreter.md's own recommendation) turned out to
  use them for its Nebulizer content, confined to the module's rare
  chronic-surgical tail exactly as that document's own survey predicted,
  but the M5a loader's own all-or-nothing gate ('any deferred-type use
  fails it, full stop') rejected the WHOLE module for two states with no
  clinical content this project's accumulator or IR has a home for yet
  (no equipment-tracking concept anywhere in docs/patient-state-model.md).
  Consumed-internally is the correct, minimal treatment: no trajectory
  event, no attribute write, ordinary transition resolution -- the same
  'reachable by simply not compiling that one branch's terminal states'
  disposition docs/gmf-interpreter.md's own appendix already named for
  this exact gap, now actually built rather than merely anticipated."
  {"Initial" :initial
   "Terminal" :terminal
   "Simple" :simple
   "Delay" :delay
   "Guard" :guard
   "SetAttribute" :set-attribute
   "Symptom" :symptom
   "ConditionOnset" :condition-onset
   "ConditionEnd" :condition-end
   "Encounter" :encounter
   "EncounterEnd" :encounter-end
   "Procedure" :procedure
   "Observation" :observation
   "MedicationOrder" :medication-order
   "MedicationEnd" :medication-end
   "Device" :device
   "DeviceEnd" :device-end
   ;; GMF coverage Wave B (2026-08-02, ADR-0027, D3): CallSubmodule joins
   ;; v1 as a LOADABLE state type -- the loader can now discover a
   ;; module's own :submodule call-paths (`call-submodule-paths`,
   ;; below), but the interpreter's own call/return mechanism (D1-D4)
   ;; is a separate, later commit.
   "CallSubmodule" :call-submodule
   ;; GMF coverage Wave C (2026-08-02, ADR-0028, C1/C2): Death joins v1
   ;; as a real, terminal trajectory-event-producing state -- unlike
   ;; Device/DeviceEnd (consumed-internally, no home for the content),
   ;; Death IS a real event the accumulator now has a home for
   ;; (:expired, docs/patient-state-model.md). The interpreter's own
   ;; handling (gmf-interpreter.clj) is the C1/C2 build; this loader
   ;; only makes the state TYPE loadable and validates its own shape.
   "Death" :death
   ;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 R2(a)): the
   ;; observation family joins v1 -- both extend Synthea's own private
   ;; ObservationGroup class (D1a-2, docs/gmf-interpreter.md section 11)
   ;; and compile into ONE shared pathway-IR step (:diagnostic-report),
   ;; but stay TWO distinct loadable state types here (this loader's own
   ;; job is validating the module JSON as authored, not the later
   ;; compile-time union).
   "MultiObservation" :multi-observation
   "DiagnosticReport" :diagnostic-report
   ;; GMF coverage Wave D stage D2 (2026-08-02, ADR-0029 R2(b)): the
   ;; CarePlan family joins v1 -- a paired span structurally identical
   ;; to MedicationOrder/MedicationEnd (State.java's own
   ;; CarePlanStart/CarePlanEnd classes, gmf-interpreter.md section 13).
   "CarePlanStart" :care-plan-start
   "CarePlanEnd" :care-plan-end
   ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-1/AR-2/AR-3): Counter/
   ;; ImagingStudy/SupplyList join v1 -- this document's own original
   ;; Deferred table entries (section 1), now built.
   "Counter" :counter
   "ImagingStudy" :imaging-study
   "SupplyList" :supply-list})

(def ^:private code-system->keyword
  "GMF's own code-system strings -> sim-model/Concept's
  :system keyword vocabulary (architecture.md's Terminology decisions:
  :snomed :loinc :rxnorm :icd10cm :cvx)."
  {"SNOMED-CT" :snomed
   "LOINC" :loinc
   "RxNorm" :rxnorm
   "ICD10-CM" :icd10cm
   "CVX" :cvx})

(def ^:private encounter-class->keyword
  "GMF's own encounter-class strings (docs/gmf-interpreter.md section 4) --
  kept as their own map (distinct from `slug`'s generic transform) since
  these are a CLOSED v1 vocabulary, not free-form names.

  GMF coverage Wave B (2026-08-02, ADR-0027): \"outpatient\" is a real,
  distinct GMF encounter-class STRING (`ear_infections.json`'s own
  primary encounter, Step 1's own characterization) this project's own
  §4 table never separately named -- aliased onto the SAME `:ambulatory`
  keyword `\"ambulatory\"` already maps to, not a new keyword of its
  own: `ehrt.sim-trajectory.compile-trajectory`'s own `encounter->step`
  (confirmed by direct read) already treats `:wellness`/`:ambulatory`
  identically (both compile to `:outpatient-visit`), so this is a
  genuine same-concept vocabulary alias, not an invented mapping, and
  needs no `compile-trajectory` change.

  GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3f finding,
  found vendoring uti/ambulatory_path.json's own Telephone_Encounter):
  \"virtual\" is a real, distinct GMF encounter-class STRING -- a
  genuinely NEW keyword, `:virtual`, NOT aliased onto `:ambulatory`
  (unlike \"outpatient\"): a phone/remote encounter is a different
  clinical modality from an in-person one, and this session's own
  vendoring never exercises `compile-trajectory`'s encounter mapping
  for this closure (the standing, disclosed interpreter-layer-only
  fence, `ehrt.sim-trajectory.vendored-uti-test`'s own docstring) --
  whether `:virtual` compiles the SAME way `:ambulatory` does, or needs
  its own IR treatment, is a decision for whichever future session
  first exercises a closure through the full compile-trajectory
  pipeline, not this one."
  {"wellness" :wellness "ambulatory" :ambulatory "emergency" :emergency "inpatient" :inpatient
   "outpatient" :ambulatory "virtual" :virtual})

(def ^:private condition-type->keyword
  "v1's condition predicates (docs/gmf-interpreter.md section 2): age,
  sex (Gender), attribute, PriorState -- plus, M5b, the log-query family
  `Active Condition`/`Active Medication` join as the architecturally-
  same-shape extension that document's own condition-vocabulary-gap
  note already named as the natural next step ('the identical shape to
  PriorState's own query, just keyed on a concept rather than a module
  state name'), `And` as a recursive compound wrapper, and `Active
  Allergy` as a documented, always-false simplification (this project's
  Persona has no allergy concept to query yet -- see ehrt.sim-trajectory.gmf-
  interpreter/evaluate-condition's own docstring note). Discovered
  load-bearing, not merely convenient: the ratified vendored module
  (sinusitis.json) uses `And`/`Active Medication`/`Active Condition` on
  `Wait_for_condition_to_resolve`, a state EVERY patient who ever reaches
  the module's own Doctor_Visit encounter passes through -- not an
  excludable tail the way Device/DeviceEnd is, so leaving this gap
  unresolved would mean the vendored module throws for virtually every
  patient who ever onsets, not merely fails to cover a rare branch."
  {"Age" :age "Gender" :gender "Attribute" :attribute "PriorState" :prior-state
   "Active Condition" :active-condition "Active Medication" :active-medication
   "Active Allergy" :active-allergy "And" :and
   ;; GMF coverage Wave A (2026-08-02, .agents/plans/2026-08-02-gmf-
   ;; coverage-plan.md): :symptom is an emergent finding, not one of that
   ;; session's own named candidates (At Least/Or/Date/Observation/Active
   ;; Allergy) -- required for :at-least's only real vendored use
   ;; (sore_throat.json's Determine_if_Bacterial); see
   ;; ehrt.sim-trajectory.gmf-interpreter/symptom-condition-holds?'s own
   ;; docstring for the full account.
   "Symptom" :symptom "Or" :or "At Least" :at-least "Date" :date "Observation" :observation
   ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-4): `Not` (recursive
   ;; negation), `Race`, and `Socioeconomic Status` -- Logic.java's own
   ;; Race/SocioeconomicStatus classes (source-grounded), and the boolean
   ;; wrapper `And`/`Or`/`At Least` already establish the recursive shape
   ;; for. Listed here EXPLICITLY even though the slug fallback below
   ;; would already produce the same keywords -- this map is this
   ;; project's own grep-able vocabulary registry, not merely a
   ;; convenience transform."
   "Not" :not "Race" :race "Socioeconomic Status" :socioeconomic-status})

(defn- normalize-code
  "GMF's own code triplet -> sim-model/Concept. M5b: :code
  is coerced to a string regardless of its own JSON type -- the vendored
  sinusitis.json carries at least one unquoted-JSON-number code value
  (Prescribe_Alternative_Antibiotic's own RxNorm code), and
  sim-model/Concept requires a string. This is a representation
  normalization, the same kind `slug`/keywordizing already apply to
  every other GMF field this loader touches -- the code's own digits
  pass through unchanged (code passthrough law), only their Clojure
  type does, never a translation or invention of the value itself."
  [{:keys [system code display]}]
  (cond-> {:system (get code-system->keyword system (keyword (slug system))) :code (str code)}
    display (assoc :display display)))

(defn- normalize-condition
  "A leaf condition map ({:condition-type ...}) -> the same shape with
  :condition-type keywordized to v1's vocabulary, :codes normalized
  (Concept triplets, same as every other state's own :codes) for the
  concept-keyed predicates (Active Condition/Active Medication/Active
  Allergy), and :conditions recursively normalized for And's own nested
  sub-conditions. Compound conditions OUTSIDE this vocabulary (`At
  Least`, boolean `Or`) stay out of v1's scope (docs/gmf-interpreter.md
  section 2's own gap note) -- passed through unrecognized rather than
  validated here; the interpreter is where an actually-unsupported
  condition type surfaces, at evaluation time, not at load time (section
  1's own state-type gate is the load-time enforcement point; conditions
  are a narrower, later concern this loader does not gate)."
  [condition]
  (when condition
    (let [condition-type (get condition-type->keyword (:condition-type condition)
                               (keyword (slug (:condition-type condition))))]
      (cond-> (assoc condition :condition-type condition-type)
        (and (= :prior-state condition-type) (:name condition))
        (update :name (fn [n] (keyword (slug n))))

        (:codes condition)
        (update :codes #(mapv normalize-code %))

        ;; GMF coverage Wave A (2026-08-02): :or/:at-least share :and's own
        ;; recursive sub-condition shape -- without this, a nested
        ;; sub-condition's own :condition-type stays an un-normalized raw
        ;; string, and evaluate-condition's case dispatch (keywords only)
        ;; would never match it.
        (and (#{:and :or :at-least} condition-type) (:conditions condition))
        (update :conditions #(mapv normalize-condition %))

        ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-4): `Not` wraps a
        ;; SINGLE nested condition under :condition (singular -- Logic.
        ;; java's own field name, source-grounded), never the plural
        ;; :conditions vector And/Or/At-Least share -- a distinct
        ;; recursive clause, the same reason those three already needed
        ;; their own (without this, a nested Not condition's own
        ;; :condition-type stays an un-normalized raw string).
        (and (= :not condition-type) (:condition condition))
        (update :condition normalize-condition)))))

(defn- normalize-transition-entry
  [{:keys [transition condition distributions] :as entry}]
  (cond-> entry
    transition (assoc :transition (keyword (slug transition)))
    condition (assoc :condition (normalize-condition condition))
    distributions (assoc :distributions (mapv #(update % :transition (fn [t] (keyword (slug t)))) distributions))))

(defn- normalize-lookup-table-entry
  "GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3a, H2):
  :transition normalizes the SAME way every other transition-entry
  target already does (`normalize-transition-entry`); :lookup-table-name
  stays verbatim (a CSV filename, D3a's own citation)."
  [{:keys [transition] :as entry}]
  (cond-> entry transition (assoc :transition (keyword (slug transition)))))

(defn- normalize-transitions
  [state]
  (cond-> state
    (:direct-transition state) (update :direct-transition (fn [t] (keyword (slug t))))
    (:distributed-transition state) (update :distributed-transition #(mapv normalize-transition-entry %))
    (:conditional-transition state) (update :conditional-transition #(mapv normalize-transition-entry %))
    (:complex-transition state) (update :complex-transition #(mapv normalize-transition-entry %))
    ;; GMF coverage Wave B (2026-08-02, ADR-0027, D5): the fifth
    ;; transition kind -- a fixed {:ambulatory :emergency :telemedicine}
    ;; map, each value a raw target-state-name string (never a weight --
    ;; real Synthea's own weights live entirely in an external resource
    ;; this project has no analog for, docs/gmf-interpreter.md section
    ;; 9's own D5 account) -- normalized the SAME way :direct-transition
    ;; already is, one key at a time.
    (:type-of-care-transition state)
    (update :type-of-care-transition #(into {} (map (fn [[k t]] [k (keyword (slug t))])) %))
    ;; GMF coverage Wave D stage D3 (D3a, H2): the sixth kind.
    (:lookup-table-transition state)
    (update :lookup-table-transition #(mapv normalize-lookup-table-entry %))))

(defn- normalize-observation-child
  "GMF coverage Wave D stage D1 (2026-08-02, ADR-0029): one embedded
  MultiObservation/DiagnosticReport child -- :codes (same as every
  other state's own) and :value-code (a single Concept, the same
  normalize-code as :codes' own elements) normalized; :range/:vital-
  sign carry no code system of their own, untouched. `:vital-sign`'s
  raw value stays exactly as authored (this table's own lookup key,
  see gmf-interpreter's own sample-observation-extra), never slugged."
  [child]
  (cond-> child
    (:codes child) (update :codes #(mapv normalize-code %))
    (:value-code child) (update :value-code normalize-code)))

(defn- normalize-imaging-instance
  "GMF coverage Wave F (2026-08-03, ADR-0036 AR-2): :sop-class is a
  Concept triplet, the same normalize-code every other coded field
  already gets; :title stays verbatim (a free-text label, not a code)."
  [instance]
  (cond-> instance
    (:sop-class instance) (update :sop-class normalize-code)))

(defn- normalize-imaging-series
  "GMF coverage Wave F (2026-08-03, ADR-0036 AR-2): :body-site/:modality
  are Concept triplets; :instances recurses one level, the same nested-
  vector shape :observations already establishes for MultiObservation/
  DiagnosticReport children."
  [series]
  (cond-> series
    (:body-site series) (update :body-site normalize-code)
    (:modality series) (update :modality normalize-code)
    (:instances series) (update :instances #(mapv normalize-imaging-instance %))))

;; --- GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3c finding 1):
;; gmf_version 2's own uniform stochastic-timing encoding -- a
;; top-level `distribution: {kind: EXACT|UNIFORM, parameters: {...}}`
;; plus a sibling top-level `unit`, replacing Delay's own top-level
;; range/exact keys, Procedure's own duration field, and Symptom's own
;; top-level range/exact severity keys, one state at a time, author's
;; choice (confirmed field-by-field against both v1 and v2 examples of
;; the SAME state type, docs/gmf-interpreter.md section 14's own D3c
;; finding 1). A loader normalization, not a new interpreter mechanism
;; -- the same disposition Wave B's own encounter-class/wellness
;; findings already established. ------------------------------------------

(defn- gmf-v2-timing->v1
  "UNIFORM -> the existing Range shape; EXACT -> the existing Exact
  shape (`unit` absent for Symptom's own unitless severity)."
  [{:keys [kind parameters]} unit]
  (case kind
    "UNIFORM" (cond-> {:low (:low parameters) :high (:high parameters)} unit (assoc :unit unit))
    "EXACT" (cond-> {:quantity (:value parameters)} unit (assoc :unit unit))))

(defn- apply-gmf-v2-timing
  "Delay/Symptom: the translated shape writes to the SAME top-level
  :range/:exact key `resolve-time-advance`/the :symptom interpreter
  case already read (`:range` for UNIFORM, `:exact` for EXACT)."
  [state]
  (let [dist (:distribution state)
        v1-shape (gmf-v2-timing->v1 dist (:unit state))
        target-key (if (= "UNIFORM" (:kind dist)) :range :exact)]
    (assoc (dissoc state :distribution :unit) target-key v1-shape)))

(defn- apply-gmf-v2-procedure-duration
  "Procedure's own :duration field is declared Range-only ({:low :high
  :unit}) in this loader's own schema -- unlike Delay/Symptom, it has
  no separate Exact form. An EXACT-kind v2 duration translates into a
  DEGENERATE Range (:low = :high = the exact value) rather than
  widening the schema -- numerically identical to a true exact
  quantity, and consistent with the pre-existing v1 flat-:duration
  encoding (`appendicitis.json`/`sepsis.json`).

  FIXED (2026-08-03, notes/ADRs.md ADR-0032 AR-2): this loader's own
  disclosed, unrelated `resolve-time-advance`/:duration gap (D3c finding
  1's own dated note -- `:duration` was passed to `resolve-time-advance`
  as a flat map, which destructures :range/:exact KEYS from it and found
  neither, silently never advancing time for ANY Procedure, v1 or v2) is
  now fixed at `emit-and-advance`'s own call site (gmf-interpreter.clj),
  not here -- this translation's own flat-map output was already
  correct, the bug was downstream of it."
  [state]
  (let [{:keys [kind parameters]} (:distribution state)
        unit (:unit state)
        shape (case kind
                "UNIFORM" {:low (:low parameters) :high (:high parameters)}
                "EXACT" {:low (:value parameters) :high (:value parameters)})]
    (assoc (dissoc state :distribution :unit) :duration (cond-> shape unit (assoc :unit unit)))))

;; --- ADR-0035 (Wave F0): GAUSSIAN/EXPONENTIAL/TRIANGULAR join the v2
;; distribution vocabulary alongside UNIFORM/EXACT -- ported verbatim
;; from Synthea's own Distribution.java (fetched-source pin
;; 7e08387c68a7f0e21d13076609a159fd473fc902, ADR-0035 AR-1), across THREE
;; contexts (Delay/Symptom timing, Procedure duration, SetAttribute
;; value, ADR-0035 AR-2) rather than D3c's original two. UNIFORM/EXACT
;; keep their existing v1-collapse (`gmf-v2-timing->v1`/`apply-gmf-v2-
;; procedure-duration`, above) completely untouched (AR-5, "no churn")
;; -- the three new kinds, and SetAttribute's own (all-five) distribution
;; field, normalize instead into ONE self-contained shape,
;; `SampledDistribution` (schema section, below): `{:kind :exact|
;; :uniform|:gaussian|:exponential|:triangular :parameters {...kebab-
;; keyed...} :round bool :unit {:optional}}` -- sampled at INTERPRETER
;; time (`ehrt.sim-trajectory.gmf-interpreter`'s own `sample-
;; distribution`), never collapsed into Range/Exact (no such shape
;; exists for a Gaussian/Exponential/Triangular draw). ---------------------

(def ^:private v1-collapse-kinds
  "UNIFORM/EXACT -- the two kinds `gmf-v2-timing->v1`/`apply-gmf-v2-
  procedure-duration` already translate into the pre-existing Range/
  Exact shapes (D3c finding 1, untouched by this ADR)."
  #{"UNIFORM" "EXACT"})

(def ^:private distribution-kind->keyword
  "Every kind this loader recognizes at ALL (ADR-0035 AR-1's five-kind
  closed vocabulary, Distribution.java's own `Kind` enum, source-
  confirmed) -- a raw :kind string outside this map's own keys is what
  `invalid-distribution-kind?` (below) catches and rejects cleanly
  (AR-2), never a fall-through `case` throw."
  {"EXACT" :exact "UNIFORM" :uniform "GAUSSIAN" :gaussian
   "EXPONENTIAL" :exponential "TRIANGULAR" :triangular})

(defn- normalize-distribution-parameters
  "Distribution.java's own per-kind `parameters` map (AR-1's required-
  parameters table, `validate()` source-confirmed) -- kebab-keyed onto
  this project's own idiom (`standarddeviation`, `kebab-key`'s own
  camelCase-blind transform of JSON's `standardDeviation`, renamed here
  to the readable `:standard-deviation` this project's other kebab keys
  already use). Optional keys (:min/:max on GAUSSIAN) are OMITTED, never
  assoc'd as an explicit nil -- `load-module`'s own :remarks precedent:
  'optional means the KEY may be absent, not that a present value may be
  nil.' A required key genuinely absent from the raw JSON stays nil here
  -- `SampledDistribution`'s own per-kind schema (below) is what turns
  that into a real :schema-invalid rejection, the same disposition every
  other structural gap in this loader already gets."
  [kind-kw {:keys [value low high mean standarddeviation min max mode]}]
  (case kind-kw
    :exact {:value value}
    :uniform {:low low :high high}
    :gaussian (cond-> {:mean mean :standard-deviation standarddeviation}
                (some? min) (assoc :min min)
                (some? max) (assoc :max max))
    :exponential {:mean mean}
    :triangular {:min min :mode mode :max max}))

(defn- normalize-distribution
  "The raw v2 `:distribution` map (`:kind` a raw string, `:parameters` a
  raw kebab-keyed-by-`kebab-key` map, `:round` a raw boolean or absent)
  -> `SampledDistribution`'s own shape (below): :kind keywordized,
  :parameters normalized (`normalize-distribution-parameters`), :round
  ALWAYS a boolean (missing/nil coerced to `false`, never left absent --
  the interpreter's own `sample-distribution` reads it unconditionally),
  :unit folded in only when the caller supplies one (Delay/Procedure's
  own top-level :unit field -- SetAttribute has none)."
  [{:keys [kind round parameters]} & [unit]]
  (let [kind-kw (get distribution-kind->keyword kind)]
    (cond-> {:kind kind-kw
             :parameters (normalize-distribution-parameters kind-kw parameters)
             :round (boolean round)}
      unit (assoc :unit unit))))

(defn- state-distribution-kind
  "The raw :kind string on `state`'s own top-level :distribution, or nil
  when `state` carries no such field -- the one predicate both
  `invalid-distribution-kind?` and `normalize-state`'s own dispatch
  clauses below share."
  [state]
  (get-in state [:distribution :kind]))

(def ^:private distribution-timing-state-types
  "The state TYPES a top-level v2 :distribution can appear on as a TIMING
  value, this session's own three timing contexts (ADR-0035 AR-2) --
  SetAttribute is checked separately below (not a timing context: no
  :unit folding, and it competes with :value/:value-code, guarded by
  `set-attribute-value-conflict?`, not this set)."
  #{:delay :symptom :procedure})

(defn- invalid-distribution-kind?
  "ADR-0035 AR-2: a state carrying a top-level :distribution whose own
  :kind is OUTSIDE `distribution-kind->keyword`'s five-kind vocabulary
  -- on any of this session's own four contexts (the three timing types
  plus :set-attribute) -- is a clean, load-time REJECTION candidate
  (`normalize-state`'s own early-return branch), never a `case` fall-
  through throw the way `gmf-v2-timing->v1`/`apply-gmf-v2-procedure-
  duration` used to (the census's own `gmf_version 2` loader-exception
  finding, ADR-0034's execution note, this ADR's own Context)."
  [state kw-type]
  (when-let [kind (state-distribution-kind state)]
    (and (or (distribution-timing-state-types kw-type) (= :set-attribute kw-type))
         (nil? (get distribution-kind->keyword kind)))))

(defn- attribute-value-sources
  "Which of SetAttribute's three mutually-exclusive-in-practice value
  sources `state` actually carries -- :distribution/:value/:value-code,
  any present (ADR-0035 AR-4: upstream's own real precedent is 'a
  distribution present means sample it,' never a silent priority order
  among the three; `contains?` for :value/:value-code since a legitimate
  authored value can be falsy -- `false`, `0`, `\"\"` -- and must not be
  mistaken for absence)."
  [state]
  (into #{} (keep identity)
        [(when (map? (:distribution state)) :distribution)
         (when (contains? state :value) :value)
         (when (contains? state :value-code) :value-code)]))

(defn- set-attribute-value-conflict?
  "ADR-0035 AR-4: a SetAttribute state carrying :distribution ALONGSIDE
  :value or :value-code is a load-time REJECTION (`normalize-state`'s
  own early-return branch) -- 'record a load-time rejection rather than
  guessing,' never a silently-chosen precedence order. (:value and
  :value-code coexisting WITHOUT :distribution is pre-existing,
  untouched behavior -- `step`'s own :set-attribute case already
  prioritizes :value-code there, unrelated to this session's own fence.)"
  [state kw-type]
  (and (= :set-attribute kw-type)
       (map? (:distribution state))
       (or (contains? state :value) (contains? state :value-code))))

(defn- apply-new-timing-distribution
  "GAUSSIAN/EXPONENTIAL/TRIANGULAR on Delay/Symptom/Procedure (ADR-0035
  AR-2/AR-5): normalized into `SampledDistribution`'s own shape, kept
  as its own :distribution key (never collapsed into Range/Exact -- no
  such shape exists for these three kinds) -- the state's own top-level
  :unit (Delay/Procedure; Symptom carries none, its own severity is
  unitless, `gmf-v2-timing->v1`'s own docstring precedent) folds INTO
  the distribution map and is dissoc'd from the state, the same 'unit
  travels with its own timing shape, never left as a stray top-level
  field' discipline the v1-collapse path already establishes."
  [state]
  (assoc (dissoc state :unit) :distribution (normalize-distribution (:distribution state) (:unit state))))

(defn- normalize-set-attribute-distribution
  "SetAttribute's own :distribution (ADR-0035 AR-2/AR-4): normalized the
  SAME way `apply-new-timing-distribution` normalizes Delay/Symptom/
  Procedure's, minus :unit folding (SetAttribute carries none -- the
  110+-instance catalog survey behind this ADR confirmed none exist).
  All FIVE kinds pass through here (unlike the timing contexts' own v1-
  collapse split) -- SetAttribute never had a pre-existing UNIFORM/EXACT
  translation to leave untouched; this is entirely new code, free to
  normalize uniformly."
  [state]
  (update state :distribution normalize-distribution))

(defn- effective-state-type
  "GMF coverage Wave G (2026-08-03, ADR-0037 AR-3): `kw-type` (the raw
  `gmf-type->keyword` lookup) UNLESS `state` is the `wellness: true`,
  no-`:encounter-class` Encounter idiom (Wave B's own M7 finding) -- in
  which case the loaded STATE TYPE is `:wellness-wait`, not `:encounter`.
  A distinct type, not a synthesized `:encounter-class`, because it is a
  genuine BLOCK-then-attach cycle at the interpreter layer (`gmf-
  interpreter.clj`'s own `wellness-wait-step`), not an ordinary
  Encounter with a class value -- retiring Wave B's own create-now
  substitution (`normalize-state`'s own dated retirement comment, where
  this override is applied)."
  [kw-type state]
  (if (and (= :encounter kw-type) (:wellness state) (not (:encounter-class state)))
    :wellness-wait
    kw-type))

(defn- normalize-state
  [state]
  (let [raw-type (:type state)
        kw-type (get gmf-type->keyword raw-type)]
    (cond
      (nil? kw-type)
      {:unsupported-state-type {:raw-type raw-type}}

      (invalid-distribution-kind? state kw-type)
      {:invalid-distribution-kind {:kind (state-distribution-kind state)}}

      (set-attribute-value-conflict? state kw-type)
      {:set-attribute-value-conflict {:sources (attribute-value-sources state)}}

      :else
      (-> state
          ;; GMF coverage Wave B (2026-08-02, ADR-0027): a second GMF
          ;; wellness-encounter encoding this loader didn't recognize
          ;; (docs/gmf-interpreter.md section 8's own M7 finding,
          ;; mTBI/atrial_fibrillation/osteoporosis/epilepsy/med_rec --
          ;; confirmed MANDATORY-path on ear_infections.json too, Step
          ;; 1's own characterization): `"wellness": true` with no
          ;; `encounter_class` key at all. Originally normalized to
          ;; `:encounter-class :wellness` on an ordinary `:encounter`
          ;; state (a create-now substitution: this loader fired an
          ;; IMMEDIATE :outpatient-visit where upstream's own
          ;; `State.java` Encounter.process wellness branch, pin
          ;; 7e08387c68a7f0e21d13076609a159fd473fc902, creates nothing
          ;; and BLOCKS until the engine's hardcoded EncounterModule
          ;; opens its own next separately-scheduled wellness encounter
          ;; -- ADR-0031 AR-5(b)'s dated disclosure, live in the
          ;; vendored ear_infections.json walk's own Next_Wellness_
          ;; Encounter).
          ;;
          ;; RETIRED (2026-08-03, notes/ADRs.md ADR-0037 AR-3): the
          ;; substitution above is GONE -- `effective-state-type`
          ;; (below) now maps this same raw shape onto its own DISTINCT
          ;; state type, `:wellness-wait` (schema section, above), which
          ;; the interpreter's own `wellness-wait-step` genuinely waits
          ;; on (`next-wellness-tick`, ADR-0037 AR-1/AR-2) rather than
          ;; creating an encounter on the spot. Kept as history, not
          ;; deleted outright, per this project's own fix-forward-with-
          ;; disclosure discipline.
          (assoc :type (effective-state-type kw-type state))
          (cond-> (:codes state) (update :codes #(mapv normalize-code %))
                  (:code state) (update :code normalize-code)
                  (:allow state) (update :allow normalize-condition)
                  (:encounter-class state) (update :encounter-class
                                                    (fn [c] (get encounter-class->keyword c (keyword (slug c)))))
                  (:condition-onset state) (update :condition-onset (fn [t] (keyword (slug t))))
                  (:medication-order state) (update :medication-order (fn [t] (keyword (slug t))))
                  (:device state) (update :device (fn [t] (keyword (slug t))))
                  (:target-encounter state) (update :target-encounter (fn [t] (keyword (slug t))))
                  ;; GMF coverage Wave D stage D2 (2026-08-02, ADR-0029):
                  ;; :careplan (CarePlanEnd's own state-name reference to
                  ;; the CarePlanStart it closes, State.java's own field
                  ;; name verbatim) normalizes the SAME way :medication-
                  ;; order/:device already do; :activities (CarePlanStart's
                  ;; own Concept vector) the same way top-level :codes
                  ;; already does, above.
                  (:careplan state) (update :careplan (fn [t] (keyword (slug t))))
                  (:activities state) (update :activities #(mapv normalize-code %))
                  ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-1): Counter's
                  ;; own :action ("increment"/"decrement") normalizes to a
                  ;; keyword the SAME way every other closed two-value GMF
                  ;; vocabulary already does here (:encounter-class, above).
                  (:action state) (update :action (fn [a] (keyword (slug a))))
                  ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-2): ImagingStudy's
                  ;; own :procedure-code (a single Concept) and :series (embedded,
                  ;; recursively normalized).
                  (:procedure-code state) (update :procedure-code normalize-code)
                  (:series state) (update :series #(mapv normalize-imaging-series %))
                  ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-3): SupplyList's
                  ;; own :supplies -- each component's :code normalized, :quantity
                  ;; untouched (already a plain int).
                  (:supplies state) (update :supplies #(mapv (fn [c] (update c :code normalize-code)) %))
                  ;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029):
                  ;; :value-code on a standalone :observation state
                  ;; (Capillary_Refill's own top-level shape); :observations
                  ;; on a :multi-observation/:diagnostic-report state (its
                  ;; own embedded children, D1a-2).
                  (:value-code state) (update :value-code normalize-code)
                  (:observations state) (update :observations #(mapv normalize-observation-child %))
                  ;; GMF coverage Wave D stage D3 (D3c finding 1): the
                  ;; gmf_version 2 timing encoding -- dispatched on
                  ;; kw-type, BEFORE normalize-transitions (a state's
                  ;; own top-level :distribution, never the DIFFERENT,
                  ;; nested :distribution H3 already handles inside
                  ;; :distributed-transition's own entries). ADR-0035:
                  ;; restricted to `v1-collapse-kinds` (UNIFORM/EXACT)
                  ;; now that a THIRD sibling clause (below) exists for
                  ;; the other three kinds -- by the time normalize-state
                  ;; reaches this cond-> (past the invalid-distribution-
                  ;; kind? early return, above), :kind is guaranteed one
                  ;; of the five recognized strings, so "not a v1-collapse
                  ;; kind" below correctly means "one of the three new
                  ;; ones," never an unrecognized one.
                  (and (map? (:distribution state)) (v1-collapse-kinds (:kind (:distribution state))) (#{:delay :symptom} kw-type))
                  apply-gmf-v2-timing

                  (and (map? (:distribution state)) (v1-collapse-kinds (:kind (:distribution state))) (= :procedure kw-type))
                  apply-gmf-v2-procedure-duration

                  ;; ADR-0035 AR-2/AR-5: GAUSSIAN/EXPONENTIAL/TRIANGULAR
                  ;; on Delay/Symptom/Procedure -- kept as a normalized
                  ;; :distribution map, never collapsed (no Range/Exact
                  ;; equivalent exists for these three kinds).
                  (and (map? (:distribution state)) (distribution-timing-state-types kw-type)
                       (not (v1-collapse-kinds (:kind (:distribution state)))))
                  apply-new-timing-distribution

                  ;; ADR-0035 AR-2/AR-4: SetAttribute's own :distribution
                  ;; -- all five kinds, `set-attribute-value-conflict?`
                  ;; (above) already gated out the ambiguous case.
                  (and (map? (:distribution state)) (= :set-attribute kw-type))
                  normalize-set-attribute-distribution)
          normalize-transitions))))

(defn- normalize-states
  "Normalizes every state; short-circuits with the FIRST deferred-type
  state found (deterministic -- iterates in the module's own key order),
  since a module using even one deferred type fails load, full stop
  (this namespace's own docstring). ADR-0035: two more short-circuiting
  categories join :unsupported-state-type here, the SAME 'first found,
  deterministic order' discipline -- an unrecognized v2 distribution
  :kind (:invalid-distribution-kind) and a SetAttribute state carrying
  more than one of :distribution/:value/:value-code
  (:set-attribute-value-conflict)."
  [raw-states]
  (reduce (fn [acc [state-name raw-state]]
            (let [normalized (normalize-state raw-state)]
              (cond
                (:unsupported-state-type normalized)
                (reduced {:unsupported {:state state-name
                                        :raw-type (:raw-type (:unsupported-state-type normalized))}})

                (:invalid-distribution-kind normalized)
                (reduced {:invalid-distribution {:state state-name
                                                  :kind (:kind (:invalid-distribution-kind normalized))}})

                (:set-attribute-value-conflict normalized)
                (reduced {:value-conflict {:state state-name
                                           :sources (:sources (:set-attribute-value-conflict normalized))}})

                :else
                (update acc :states assoc state-name normalized))))
          {:states {}}
          raw-states))

;; --- Attributes registry (section 5) --------------------------------------

(def engine-reserved-attribute-names
  "The bare (non-namespaced) attribute names this project's OWN
  engine-internal logic reserves -- currently just `donor`
  (docs/patient-state-model.md's post-mortem entry). A module writing
  one of these AS ITS OWN RAW (pre-namespace) attribute name is rejected
  at load time (section 5)."
  #{"donor"})

(defn- raw-attribute-writes
  "Every raw (pre-namespace) attribute name a module's own SetAttribute/
  Symptom states write -- SetAttribute's :attribute field, Symptom's own
  :symptom field (section 1: Symptom is structurally identical to
  SetAttribute, a leaf write into a module-namespaced key holding the
  sampled severity)."
  [states]
  (keep (fn [[_ state]]
          (case (:type state)
            :set-attribute (:attribute state)
            :symptom (:symptom state)
            ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-1): Counter is a
            ;; third attribute-writing leaf, section 5's own collision check
            ;; extended the same way Symptom already joined SetAttribute.
            :counter (:attribute state)
            nil))
        states))

(defn- reserved-attribute-collision
  [states]
  (first (filter engine-reserved-attribute-names (raw-attribute-writes states))))

(defn declared-attributes
  "Every namespaced attribute keyword `module`'s own SetAttribute/Symptom
  states write (section 5) -- `grep`-able listability, the mechanism
  `loaded-modules` exposes below."
  [module]
  (into #{}
        (map (fn [raw] (keyword (:id module) (slug raw))))
        (raw-attribute-writes (:states module))))

;; --- Schema (v1 subset, post-normalization) -------------------------------

(def ^:private Range [:map [:low number?] [:high number?] [:unit {:optional true} :string]])
(def ^:private Exact [:map [:quantity number?] [:unit {:optional true} :string]])

;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 R2(a), D1a-2): a
;; MultiObservation/DiagnosticReport state's own :observations array is
;; a list of EMBEDDED, INLINE observation content -- confirmed directly
;; against real sepsis.json JSON (Blood_Cultures/Record_Blood_Pressure):
;; each entry carries :category/:unit/:codes and exactly one of
;; :range/:value-code/:vital-sign, but NO :type and NO transitions of
;; its own (unlike a standalone Observation state) -- children are
;; never separately-cited states, only content the parent state carries.
;; GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3d finding 2): a
;; FOURTH value-sourcing mechanism, `:exact` -- a literal, SPECIFIED
;; value (TJR's own `PROMIS29_Total_Assessment`, `functional_status_
;; assessments.json`), the same shape Delay/Death's own `:exact` field
;; already uses. Zero rng, mirroring `Delay`'s own `:exact` handling.
(def ^:private ObservationChild
  [:map
   [:category {:optional true} :string]
   [:unit {:optional true} :string]
   [:codes [:vector sim-model/Concept]]
   [:range {:optional true} Range]
   [:exact {:optional true} Exact]
   [:value-code {:optional true} sim-model/Concept]
   [:vital-sign {:optional true} :string]])

;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-2): ImagingStudy's own
;; embedded Series/Instance content -- State.java's own `HealthRecord.
;; ImagingStudy.Series`/`.Instance` classes, source-grounded and
;; confirmed against real module JSON (`congestive_heart_failure.json`,
;; `lung_cancer.json`). :title/:sop-class ride along, declared for
;; validation only -- the interpreter's own `imaging-study-extra` never
;; reads either (glass-box scope: procedure code, modality, drawn
;; counts, AR-2's own ruling), the same "declared, dead past the
;; loader" treatment several other v1 fields already establish.
(def ^:private ImagingInstance
  [:map [:title {:optional true} :string] [:sop-class {:optional true} sim-model/Concept]])

(def ^:private ImagingSeries
  [:map
   [:body-site {:optional true} sim-model/Concept]
   [:modality sim-model/Concept]
   [:instances [:vector ImagingInstance]]
   [:min-number-instances {:optional true} :int]
   [:max-number-instances {:optional true} :int]])

;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-3): SupplyList's own
;; per-component shape -- State.java's own private `SupplyComponent`
;; class (source-grounded, confirmed against `sleep_apnea.json`).
(def ^:private SupplyComponent [:map [:code sim-model/Concept] [:quantity :int]])

;; GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3b, H3): a
;; distributed_transition entry's own :distribution may be a plain
;; number (v1, unchanged) or a NamedDistribution map -- real Synthea's
;; own attribute-sourced weight with a JSON-specified fallback
;; (Transition.java's own `attribute`/`default` field names verbatim,
;; D3b's own source citation -- stroke.json's own Chance_of_Stroke gate,
;; ADR-0028, byte-confirmed against source here).
(def ^:private Distribution [:or number? [:map [:attribute :string] [:default number?]]])

(def ^:private TransitionFields
  [[:direct-transition {:optional true} :keyword]
   [:distributed-transition {:optional true}
    [:vector [:map [:transition :keyword] [:distribution Distribution]]]]
   [:conditional-transition {:optional true}
    [:vector [:map [:transition {:optional true} :keyword] [:condition {:optional true} [:map-of :keyword :any]]]]]
   ;; GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3f finding,
   ;; found vendoring uti/ambulatory_path.json): a complex_transition
   ;; entry is EITHER a direct :transition OR a weighted :distributions
   ;; list, never both required -- confirmed against Transition.java's
   ;; own ComplexTransitionOption/ComplexTransition.follow (`option.
   ;; transition != null ? ... : option.distributions`), a real
   ;; either/or this loader's schema previously required :distributions
   ;; on every entry, unconditionally.
   [:complex-transition {:optional true}
    [:vector [:map [:condition {:optional true} [:map-of :keyword :any]]
              [:transition {:optional true} :keyword]
              [:distributions {:optional true} [:vector [:map [:transition :keyword] [:distribution number?]]]]]]]
   ;; GMF coverage Wave B (D5): no weights of its own (see
   ;; normalize-transitions' own comment) -- each of the three keys is
   ;; optional (a module may omit :telemedicine on an older care-
   ;; pathway authoring, real Synthea's own shape).
   [:type-of-care-transition {:optional true}
    [:map [:ambulatory {:optional true} :keyword]
     [:emergency {:optional true} :keyword]
     [:telemedicine {:optional true} :keyword]]]
   ;; GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3a, H2): the
   ;; sixth transition kind -- :lookup-table-name is a relative CSV
   ;; filename verbatim (never slugged, the same "file reference, not a
   ;; semantic identifier" disposition :submodule already established,
   ;; D3), resolved as a closure DATA-FILE member (R4) by
   ;; `load-closure`'s own `table-resolve-fn`.
   [:lookup-table-transition {:optional true}
    [:vector [:map [:transition :keyword] [:default-probability number?]
              [:lookup-table-name :string]]]]])

(defn- with-transitions [& kvs] (into [:map] (into (vec kvs) TransitionFields)))

;; ADR-0035 (Wave F0) AR-1/AR-5: SampledDistribution -- the normalized
;; shape `normalize-distribution` (above) produces for GAUSSIAN/
;; EXPONENTIAL/TRIANGULAR on Delay/Symptom/Procedure, and for ALL FIVE
;; kinds on SetAttribute. A `:multi` dispatch on :kind, the SAME pattern
;; `GmfState` itself already uses one level up -- each branch declares
;; ONLY its own kind's required parameters (AR-1's own table,
;; Distribution.java's `validate()`, source-confirmed), so a distribution
;; missing a required parameter fails as :schema-invalid, the same
;; disposition every other structural gap in this loader already gets.
(def ^:private ExactParams [:map [:value number?]])
(def ^:private UniformParams [:map [:low number?] [:high number?]])
(def ^:private GaussianParams
  [:map [:mean number?] [:standard-deviation number?]
   [:min {:optional true} number?] [:max {:optional true} number?]])
(def ^:private ExponentialParams [:map [:mean number?]])
(def ^:private TriangularParams [:map [:min number?] [:mode number?] [:max number?]])

(defn- with-round-and-unit [& kvs] (into [:map] (into (vec kvs) [[:round :boolean] [:unit {:optional true} :string]])))

(def ^:private SampledDistribution
  [:multi {:dispatch :kind}
   [:exact (with-round-and-unit [:kind [:= :exact]] [:parameters ExactParams])]
   [:uniform (with-round-and-unit [:kind [:= :uniform]] [:parameters UniformParams])]
   [:gaussian (with-round-and-unit [:kind [:= :gaussian]] [:parameters GaussianParams])]
   [:exponential (with-round-and-unit [:kind [:= :exponential]] [:parameters ExponentialParams])]
   [:triangular (with-round-and-unit [:kind [:= :triangular]] [:parameters TriangularParams])]])

(def GmfState
  [:multi {:dispatch :type}
   [:initial (into [:map [:type [:= :initial]]] TransitionFields)]
   [:terminal [:map [:type [:= :terminal]]]]
   [:simple (into [:map [:type [:= :simple]]] TransitionFields)]
   [:delay (with-transitions [:type [:= :delay]]
             [:range {:optional true} Range] [:exact {:optional true} Exact]
             [:distribution {:optional true} SampledDistribution])]
   [:guard (with-transitions [:type [:= :guard]] [:allow [:map-of :keyword :any]])]
   ;; GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3d finding 1):
   ;; :value-code (TJR's own Pre_Procedure_Encounter_Reason/Home_Health_
   ;; Reason_Knee/Hip states) -- a Concept, the same normalize-code as
   ;; :observation's own :value-code already gets (the generic
   ;; normalize-state clause already handles it, no new loader code).
   [:set-attribute (with-transitions [:type [:= :set-attribute]] [:attribute :string] [:value {:optional true} :any]
                     [:value-code {:optional true} sim-model/Concept]
                     ;; ADR-0035 AR-2/AR-4: SetAttribute's own :distribution
                     ;; -- `set-attribute-value-conflict?` (above) already
                     ;; gates out ambiguous co-occurrence with :value/
                     ;; :value-code at LOAD time, before this schema is
                     ;; ever checked.
                     [:distribution {:optional true} SampledDistribution])]
   [:symptom (with-transitions [:type [:= :symptom]] [:symptom :string]
               [:range {:optional true} Range] [:exact {:optional true} Exact]
               [:distribution {:optional true} SampledDistribution])]
   [:condition-onset (with-transitions [:type [:= :condition-onset]] [:codes [:vector sim-model/Concept]]
                        [:target-encounter {:optional true} :keyword])]
   [:condition-end (with-transitions [:type [:= :condition-end]] [:condition-onset {:optional true} :keyword])]
   ;; GMF coverage Wave B (2026-08-02, ADR-0027): :codes is {:optional
   ;; true} -- a real `"wellness": true`-idiom Encounter (above) can
   ;; carry NO codes key at all (`ear_infections.json`'s own
   ;; Next_Wellness_Encounter, Step 1's own characterization); the same
   ;; "don't fabricate what was never actually said" disposition M5b's
   ;; own finding 6 already established for ConditionAnnotation's own
   ;; :codes field. Safe: `compile-trajectory`'s own encounter->step
   ;; (confirmed by direct read) never reads :codes off an encounter
   ;; event at all.
   [:encounter (with-transitions [:type [:= :encounter]]
                 [:encounter-class [:enum :wellness :ambulatory :emergency :inpatient :virtual]]
                 [:codes {:optional true} [:vector sim-model/Concept]] [:reason {:optional true} :string])]
   ;; GMF coverage Wave G (2026-08-03, ADR-0037 AR-3): a `wellness: true`
   ;; Encounter with no `:encounter-class` loads as this DISTINCT state
   ;; type, `:wellness-wait` -- not `:encounter` with a synthesized
   ;; `:encounter-class :wellness` (Wave B's own create-now
   ;; normalization, `normalize-state`'s own dated retirement comment,
   ;; below). :codes stays optional for the same real-content reason the
   ;; Wave B comment already gave (`ear_infections.json`'s own
   ;; `Next_Wellness_Encounter` carries none); :reason is NOT
   ;; validation-only dead weight here the way it is on every other
   ;; Encounter-shaped state (gmf.clj's own D2 disclosure) -- the
   ;; interpreter's own `wellness-wait-step` genuinely threads it into
   ;; the emitted event.
   [:wellness-wait (with-transitions [:type [:= :wellness-wait]]
                     [:codes {:optional true} [:vector sim-model/Concept]]
                     [:reason {:optional true} :string])]
   [:encounter-end (into [:map [:type [:= :encounter-end]]] TransitionFields)]
   [:procedure (with-transitions [:type [:= :procedure]] [:codes [:vector sim-model/Concept]]
                 [:target-encounter {:optional true} :keyword] [:reason {:optional true} :string]
                 [:duration {:optional true} Range]
                 [:distribution {:optional true} SampledDistribution])]
   ;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029, D1a-3/D1a-RULING
   ;; Q2+Q3): :value-code (a coded/qualitative finding) and :vital-sign
   ;; (a named-vital-sign lookup, the raw JSON string left UNTOUCHED --
   ;; unlike :attribute/:symptom, this is a lookup key into this
   ;; project's own curated reference table, sim-trajectory/vital-
   ;; signs.edn, never a module-authored identifier to slug/namespace)
   ;; join :range as the three value-sourcing mechanisms this closure
   ;; needs, side by side on the same state type (D1a-3's own finding:
   ;; real Synthea authors mix idioms even within one module).
   [:observation (with-transitions [:type [:= :observation]] [:codes [:vector sim-model/Concept]]
                   [:category {:optional true} :string] [:unit {:optional true} :string]
                   [:range {:optional true} Range]
                   [:value-code {:optional true} sim-model/Concept]
                   [:vital-sign {:optional true} :string])]
   ;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 R2(a), D1a-2):
   ;; both extend Synthea's own private ObservationGroup class -- :codes
   ;; optional (a MultiObservation/DiagnosticReport state with no
   ;; report-level code is real, source-grounded, D1a-2), :category
   ;; MultiObservation-only at the Java level but declared here on both
   ;; for uniformity (harmless when absent, the same tolerant-map
   ;; convention this schema already follows elsewhere) -- no
   ;; :number-of-observations-equivalent field (D1a-2: DEAD JSON, the
   ;; children vector's own count already is the count; the loaded
   ;; module map still carries the raw key verbatim, unvalidated,
   ;; harmless, same disposition :assign-to-attribute's own unused-field
   ;; precedent already establishes).
   [:multi-observation (with-transitions [:type [:= :multi-observation]]
                          [:codes {:optional true} [:vector sim-model/Concept]]
                          [:category {:optional true} :string]
                          [:observations [:vector ObservationChild]])]
   [:diagnostic-report (with-transitions [:type [:= :diagnostic-report]]
                          [:codes {:optional true} [:vector sim-model/Concept]]
                          [:observations [:vector ObservationChild]])]
   ;; GMF coverage Wave B (2026-08-02, ADR-0027): :assign-to-attribute /
   ;; :referenced-by-attribute -- an alternative to the fixed state-name
   ;; citation (:medication-order below) for when the SAME MedicationEnd
   ;; could be ending any one of several polymorphic orders (Step 1's
   ;; own characterization, ear_infections.json's closure) -- the
   ;; interpreter (ehrt.sim-trajectory.gmf-interpreter, its own
   ;; :medication-order/:medication-end step handling) resolves both,
   ;; this loader only declares the fields (kebab-cased automatically by
   ;; `kebab-key`, the raw string VALUE left untouched -- it names an
   ;; attribute, slug-normalized at INTERPRETER time same as :attribute/
   ;; :symptom already are, not at load time).
   [:medication-order (with-transitions [:type [:= :medication-order]] [:codes [:vector sim-model/Concept]]
                        [:reason {:optional true} :string] [:assign-to-attribute {:optional true} :string])]
   [:medication-end (with-transitions [:type [:= :medication-end]] [:medication-order {:optional true} :keyword]
                       [:referenced-by-attribute {:optional true} :string])]
   ;; GMF coverage Wave D stage D2 (2026-08-02, ADR-0029 R2(b), G1): the
   ;; SAME paired-span shape as :medication-order/:medication-end, one
   ;; entry up -- grounded directly against Synthea's own State.java
   ;; (CarePlanStart/CarePlanEnd classes, gmf-interpreter.md section
   ;; 13). :reason declared here for VALIDATION only (a real GMF field,
   ;; the vendored closure authors it) -- the SAME "declared at the
   ;; loader, dead past this loader" treatment :medication-order's own
   ;; :reason field already establishes (its real three-way attribute/
   ;; PriorState/ConditionOnset resolution is not ported). :assign-to-
   ;; attribute/:referenced-by-attribute (real fields on CarePlanStart/
   ;; CarePlanEnd per source) stay UNDECLARED here -- the declared D2
   ;; vendoring scope (total_joint_replacement.json) exercises neither,
   ;; the same "declare only when a real closure needs it" discipline
   ;; :medication-order's own :assign-to-attribute field followed at
   ;; Wave B.
   [:care-plan-start (with-transitions [:type [:= :care-plan-start]] [:codes [:vector sim-model/Concept]]
                       [:activities {:optional true} [:vector sim-model/Concept]]
                       [:reason {:optional true} :string])]
   [:care-plan-end (with-transitions [:type [:= :care-plan-end]] [:careplan {:optional true} :keyword])]
   ;; M5b: consumed-internally, like :simple -- see gmf-type->keyword's
   ;; own docstring note. :code is singular (GMF's own Device shape, one
   ;; equipment concept per state -- unlike :codes' plural elsewhere).
   [:device (with-transitions [:type [:= :device]] [:code {:optional true} sim-model/Concept])]
   [:device-end (with-transitions [:type [:= :device-end]] [:device {:optional true} :keyword])]
   ;; GMF coverage Wave B (D3): :submodule is the raw call-path string
   ;; verbatim from the module's own JSON (e.g. "medications/
   ;; ear_infection_antibiotic") -- never kebab-slugged, since it is a
   ;; relative FILE PATH (the search path this document's own D3
   ;; establishes, `sim/modules/<call-path>.json`), not a semantic
   ;; identifier this loader normalizes elsewhere.
   [:call-submodule (with-transitions [:type [:= :call-submodule]] [:submodule :string])]
   ;; GMF coverage Wave C (2026-08-02, ADR-0028, C1): three time forms
   ;; (:range/:exact -- the SAME shapes :delay/:procedure duration
   ;; already use -- or neither, meaning immediate) and, of the three
   ;; real cause-of-death forms State.java's own Death class declares,
   ;; only :codes (verbatim, code passthrough law) -- :condition-onset/
   ;; :referenced-by-attribute are accepted here (an open map, no schema
   ;; failure) but UNBUILT at the interpreter (gmf-interpreter.clj's own
   ;; :death case throws, the same disposition an unsupported condition
   ;; type already gets) -- no vendored module needs either yet
   ;; (docs/gmf-interpreter.md section 10's own C1 account).
   [:death (with-transitions [:type [:= :death]] [:codes {:optional true} [:vector sim-model/Concept]]
             [:range {:optional true} Range] [:exact {:optional true} Exact]
             [:condition-onset {:optional true} :keyword]
             [:referenced-by-attribute {:optional true} :string])]
   ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-1): Counter -- a third
   ;; attribute-writing leaf state, structurally SetAttribute-shaped
   ;; (State.java's own Counter class, source-grounded): :amount is
   ;; optional -- absent OR authored as 0 both mean "default to 1, legacy
   ;; compatibility" (the interpreter's own concern, `gmf-interpreter.clj`'s
   ;; :counter case; this schema only validates the field's own TYPE, not
   ;; its runtime default).
   [:counter (with-transitions [:type [:= :counter]] [:attribute :string]
               [:action [:enum :increment :decrement]] [:amount {:optional true} number?])]
   ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-2): ImagingStudy --
   ;; State.java's own ImagingStudy class (source-grounded, real modules
   ;; confirmed against `congestive_heart_failure.json`/`lung_cancer.json`
   ;; at the pin). :min-number-series/:max-number-series bound a single
   ;; series-count draw over the WHOLE study (`gmf-interpreter.clj`'s own
   ;; `imaging-study-extra`); each series' own :min-number-instances/
   ;; :max-number-instances bound a separate, independent draw PER
   ;; materialized series -- no vendored module this session exercises the
   ;; study-level bounds (disclosed, ADR-0036's own execution note), only
   ;; the per-series ones.
   [:imaging-study
    (with-transitions [:type [:= :imaging-study]]
      [:procedure-code sim-model/Concept]
      [:series [:vector ImagingSeries]]
      [:min-number-series {:optional true} :int]
      [:max-number-series {:optional true} :int])]
   ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-3): SupplyList --
   ;; State.java's own SupplyList class (source-grounded). Compiles to a
   ;; log-only trajectory fact, never an IR step (`compile-trajectory`'s
   ;; own explicit :supply-list clause, the ConditionEnd no-open-encounter
   ;; precedent verbatim, ADR-0036's own AR-3).
   [:supply-list (with-transitions [:type [:= :supply-list]] [:supplies [:vector SupplyComponent]])]])

(def GmfModule
  [:map
   [:id :string]
   [:name :string]
   [:remarks {:optional true} [:vector :string]]
   [:states [:map-of :keyword GmfState]]])

(defn valid-module? [module] (m/validate GmfModule module))
(defn explain-module [module] (m/explain GmfModule module))

;; --- Loading ---------------------------------------------------------------

(defn load-module
  "Parses and validates `json-text` (a GMF module's raw JSON) as `id`
  (never derived from a filename here -- the caller's own concern,
  e.g. a directory loader stripping '.json'; keeping this function pure
  over its two explicit arguments is what makes it directly testable
  against inline JSON strings, as this namespace's own red tests do).

  Returns a Result: :ok with the normalized module map ({:id :name
  :remarks :states}); :rejected :unsupported-state-type (payload {:state
  :raw-type}) for a module using a deferred GMF state type; :rejected
  :unsupported-distribution-kind (payload {:state :kind}, ADR-0035 AR-2)
  for a state whose top-level v2 :distribution names a :kind outside
  the five Distribution.java defines -- a clean rejection where the
  loader used to THROW (the census's own `gmf_version 2` loader-
  exception finding, ADR-0034); :rejected :set-attribute-value-conflict
  (payload {:state :sources}, ADR-0035 AR-4) for a SetAttribute state
  carrying more than one of :distribution/:value/:value-code; :rejected
  :attribute-collision (payload {:attribute name}) for a module whose own
  SetAttribute/Symptom writes a bare engine-reserved attribute name;
  :rejected :schema-invalid (payload {:explain ...}) for any other v1
  structural mismatch."
  [id json-text]
  (let [raw (json/read-str json-text :key-fn kebab-key)
        {:keys [states unsupported invalid-distribution value-conflict]} (normalize-states (:states raw))]
    (cond
      unsupported
      (result/rejected :unsupported-state-type unsupported)

      invalid-distribution
      (result/rejected :unsupported-distribution-kind invalid-distribution)

      value-conflict
      (result/rejected :set-attribute-value-conflict value-conflict)

      (reserved-attribute-collision states)
      (result/rejected :attribute-collision {:attribute (reserved-attribute-collision states)})

      :else
      (let [module (cond-> {:id id :name (:name raw) :states states}
                     ;; M5b: only assoc :remarks when the module actually
                     ;; HAS one -- the vendored sinusitis.json carries no
                     ;; top-level :remarks (only per-state ones, a separate,
                     ;; already-supported field this loader never validates
                     ;; the shape of), and an explicit nil under an
                     ;; {:optional true} key still fails [:vector :string]
                     ;; (optional means the KEY may be absent, not that a
                     ;; present value may be nil) -- the fixture module
                     ;; happened to always carry one, so M5a never
                     ;; exercised this path.
                     (:remarks raw) (assoc :remarks (:remarks raw)))]
        (if (valid-module? module)
          (result/ok module)
          (result/rejected :schema-invalid {:explain (explain-module module)}))))))

;; --- GMF coverage Wave B (2026-08-02, ADR-0027, D3): loader closure
;; resolution -- CallSubmodule's own transitive closure, resolved and
;; gated at load time, before any interpretation happens ------------------

(defn- call-submodule-paths
  "Every DISTINCT :submodule call-path `module`'s own CallSubmodule
  states name -- the module's own direct out-edges in the closure's
  call graph (D3)."
  [module]
  (into #{} (keep (fn [[_ state]] (when (= :call-submodule (:type state)) (:submodule state))))
        (:states module)))

(defn- resolve-closure
  "DFS worklist over the call graph rooted at `module`'s own
  CallSubmodule out-edges, extending `modules` (call-path -> loaded
  module, seeded by the caller with whatever is already resolved) and
  checking `stack` (the current DFS path's own call-paths) for a repeat
  -- D3's own acyclicity check. A call-path already IN `modules` is
  shared/deduped, not re-resolved -- the same caching-by-path behavior
  Synthea's own `Module.getModuleByPath` establishes (confirmed by
  direct read of `CallSubmodule.process()`, Wave B's own D5/D7
  characterization step) -- a submodule called from two different
  places in the closure loads exactly once. Returns a Result: :ok with
  the fully-extended `modules` map, or the FIRST rejection encountered
  (this function's own docstring on `load-closure`, below, names each
  category)."
  [resolve-fn stack modules module]
  (reduce
   (fn [result call-path]
     (let [modules (:payload result)]
       (cond
         ;; `stack` (the DFS path CURRENTLY in progress) must be checked
         ;; BEFORE `modules` (everything RESOLVED so far, root included
         ;; from the very start) -- root is pre-seeded into `modules`
         ;; before its own children ever resolve, so a cycle back to
         ;; root would otherwise be masked as "already resolved, dedup"
         ;; instead of caught as a cycle. A bug found live by this
         ;; commit's own red test (`load-closure-rejects-a-cyclic-call-
         ;; graph`), not merely anticipated.
         (some #{call-path} stack)
         (reduced (result/rejected :cyclic-closure {:cycle (conj (vec stack) call-path)}))

         (contains? modules call-path) result

         :else
         (let [json-text (resolve-fn call-path)]
           (if (nil? json-text)
             (reduced (result/rejected :submodule-not-found {:call-path call-path}))
             (let [loaded (load-module call-path json-text)]
               (if-not (result/ok? loaded)
                 (reduced (result/rejected :submodule-rejected {:call-path call-path :reason loaded}))
                 (let [sub (resolve-closure resolve-fn (conj stack call-path)
                                            (assoc modules call-path (:payload loaded))
                                            (:payload loaded))]
                   (if (result/ok? sub) sub (reduced sub))))))))))
   (result/ok modules)
   (call-submodule-paths module)))

;; --- GMF coverage Wave D stage D3 (2026-08-02, ADR-0029 R4, D3a, H2):
;; closure DATA-FILE members -- lookup-table CSVs, resolved and parsed
;; alongside the module closure, not only JSON submodules -------------------

(def ^:private recognized-lookup-table-columns
  "The only lookup-table attribute-column names this loader resolves
  (H2's own specify-vs-delegate audit, D3a): both vendored tables
  (`uti.csv`/`uti_recurrence.csv`) declare only `age`/`gender`, both
  persona-backed and buildable. Real Synthea's own `LookupTableTransition`
  also special-cases a `time` column (a date range) -- unexercised by
  either vendored table, NAMED UNBUILT here rather than silently
  generalized (installed ≠ used, H1)."
  #{"gender"})

(defn- lookup-table-transition-names
  "Every closure member's own :lookup-table-transition entries, gathered
  into {table-name -> #{declared transition keyword}} -- the data-file
  analogue of `call-submodule-paths` (D3a: which CSV a state names,
  paired with the transition SET its own JSON entries declare, so a
  table's own header can be split into attribute columns vs. weight
  columns by NAME, not by position)."
  [modules]
  (reduce (fn [acc [_ module]]
            (reduce (fn [acc [_ state]]
                      (reduce (fn [acc {:keys [transition lookup-table-name]}]
                                (update acc lookup-table-name (fnil conj #{}) transition))
                              acc (:lookup-table-transition state)))
                    acc (:states module)))
          {} modules))

(defn- parse-csv-line [line] (str/split line #","))

(defn- parse-lookup-table
  "Parses `csv-text` (a small, plain-comma, unquoted lookup table -- the
  same shape both vendored UTI tables use, real Synthea's own
  `SimpleCSV.parse`'s ordinary case; a small in-house splitter rather
  than a new external dependency for two trivial files, D3a) into a
  vector of rows: {:age-range [low high]|nil, :attributes {column
  value}, :weights {transition-kw number}}. `transition-keywords` (this
  table's own declared entry set, `lookup-table-transition-names`,
  above) is what tells a header column apart as a WEIGHT column (its
  slugged name is one of these keywords) versus an ATTRIBUTE column --
  never guessed from cell contents or column position. An attribute
  column outside `age`/`recognized-lookup-table-columns` is REJECTED
  (H2's own specify-vs-delegate audit), the same 'never silently
  skipped' disposition `:unsupported-state-type` already establishes.

  GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3f finding,
  found vendoring `uti_recurrence.csv`): a leading UTF-8 byte-order-mark
  (U+FEFF, confirmed byte-for-byte in the upstream file itself, verbatim
  -- `uti.csv` carries none) is stripped from `csv-text` before parsing
  -- `slurp`'s own UTF-8 decoding does NOT auto-strip a BOM the way some
  other language runtimes do, and Java's own `CSVReader`/`SimpleCSV`
  utilities (confirmed by Synthea's own `Utilities.readResource`, D3a)
  are exactly what a real Synthea run relies on to handle this
  transparently. Stripping it here is a representation fix, the same
  kind `slug`/`normalize-code`'s own type-coercion already establish --
  never a change to any real cell value."
  [csv-text transition-keywords]
  (let [csv-text (cond-> csv-text (= 0xFEFF (int (first csv-text))) (subs 1))
        lines (remove str/blank? (str/split-lines csv-text))
        header (parse-csv-line (first lines))
        weight-cols (filter #(contains? transition-keywords (keyword (slug %))) header)
        attr-cols (remove (set weight-cols) header)
        bad-col (first (remove #(or (= % "age") (recognized-lookup-table-columns %)) attr-cols))]
    (if bad-col
      (result/rejected :unrecognized-lookup-table-column {:column bad-col})
      (result/ok
       (mapv (fn [line]
               (let [row (zipmap header (parse-csv-line line))]
                 {:age-range (when-let [v (get row "age")]
                               (mapv #(Long/parseLong %) (str/split v #"-")))
                  :attributes (into {} (map (fn [c] [c (get row c)])) (remove #(= % "age") attr-cols))
                  :weights (into {} (map (fn [c] [(keyword (slug c)) (Double/parseDouble (get row c))])) weight-cols)}))
             (rest lines))))))

(defn- resolve-tables
  "Resolves every distinct lookup-table name `table-name->transitions`
  (`lookup-table-transition-names`) names, via `table-resolve-fn`
  (caller-supplied, the SAME pure/testable discipline `resolve-fn`
  already establishes for submodules) -- the all-or-nothing gate (D3)
  extends to data-file members: an unresolvable name (:rejected
  :lookup-table-not-found) or an unparseable table (`parse-lookup-
  table`'s own rejection) rejects the WHOLE closure."
  [table-resolve-fn modules table-name->transitions root-id]
  (reduce
   (fn [result [table-name transition-kws]]
     (let [tables (:tables (:payload result))]
       (if-let [csv-text (table-resolve-fn table-name)]
         (let [parsed (parse-lookup-table csv-text transition-kws)]
           (if (result/ok? parsed)
             (result/ok {:root root-id :modules modules :tables (assoc tables table-name (:payload parsed))})
             (reduced parsed)))
         (reduced (result/rejected :lookup-table-not-found {:table-name table-name})))))
   (result/ok {:root root-id :modules modules :tables {}})
   table-name->transitions))

(defn load-closure
  "Resolves `root-id`'s own TRANSITIVE CallSubmodule closure (D3): loads
  `root-json-text` as `root-id`, then recursively resolves every
  :submodule call-path it (or any transitively-called submodule) names,
  fetching each one's own JSON text via `(resolve-fn call-path)` --
  caller-supplied so this stays pure/testable over inline JSON strings,
  the same discipline `load-module` already establishes (no
  clojure.java.io dependency here; a real caller's own resolve-fn is a
  thin `io/resource` wrapper over the D3 search path,
  `sim/modules/<call-path>.json`).

  GMF coverage Wave D stage D3 (2026-08-02, ADR-0029 R4, D3a, H2): the
  optional trailing `table-resolve-fn` argument (purely additive -- the
  3-arity delegates to this one with a resolve-fn that always returns
  nil, so a closure naming no lookup tables never invokes it) resolves
  the closure's own DATA-FILE members the same way `resolve-fn` resolves
  its JSON ones -- a real caller's own table-resolve-fn is a thin
  `io/resource` wrapper over `sim/modules/lookup_tables/<table-name>`
  (the table name already carries its own `.csv` extension, D3a).

  Returns a Result: :ok with {:root root-id :modules {root-id -> ...,
  call-path -> ...} :tables {table-name -> parsed-table}} -- every
  call-path key is the submodule's own raw call-path string (also its
  own :id, section 5's own attribute-namespacing scope for LOAD-time
  declared-write collision checking -- distinct from D1's own RUNTIME
  root-scoping, gmf-interpreter.md section 5's own dated note); `:tables`
  is empty when the closure names none. The all-or-nothing gate (this
  namespace's own docstring, ADR-0013 point 4) extends over the WHOLE
  closure, JSON and data-file members alike: :unsupported-state-type /
  :attribute-collision / :schema-invalid from ANY transitively-called
  submodule rejects the WHOLE closure (:rejected :submodule-rejected,
  payload {:call-path :reason}, `:reason` the submodule's own rejection
  Result -- always names which call-path failed and why, never silently
  which-one-of-many). :rejected :submodule-not-found (payload {:call-
  path}) when `resolve-fn` returns nil for a named call-path. :rejected
  :cyclic-closure (payload {:cycle [...]}) when the static call graph
  contains a cycle -- an ESCALATION-worthy finding (D3), never silently
  resolved by dropping an edge. :rejected :lookup-table-not-found
  (payload {:table-name}) when `table-resolve-fn` returns nil for a
  named table; :rejected :unrecognized-lookup-table-column (payload
  {:column}) when a table's own header names an attribute column
  outside `age`/`recognized-lookup-table-columns` (H2's own specify-vs-
  delegate audit)."
  ([root-id root-json-text resolve-fn]
   (load-closure root-id root-json-text resolve-fn (constantly nil)))
  ([root-id root-json-text resolve-fn table-resolve-fn]
   (let [root-loaded (load-module root-id root-json-text)]
     (if-not (result/ok? root-loaded)
       root-loaded
       (let [root-module (:payload root-loaded)
             closure (resolve-closure resolve-fn [root-id] {root-id root-module} root-module)]
         (if-not (result/ok? closure)
           closure
           (let [modules (:payload closure)]
             (resolve-tables table-resolve-fn modules (lookup-table-transition-names modules) root-id))))))))

(defn singleton-closure
  "Wraps an already-loaded, standalone `module` (no closure resolution
  performed or needed) in `load-closure`'s own `:ok` payload shape --
  `{:root (:id module) :modules {id module} :tables {}}` -- so an
  engine-facing `:modules` entry is ALWAYS closure-shaped (ADR-0033
  AR-2), whether or not the module actually calls a submodule. Plain
  data, not a Result: there is nothing here that can fail (the module
  is already loaded and validated)."
  [module]
  {:root (:id module) :modules {(:id module) module} :tables {}})

;; --- M5b: per-patient module assignment -- SimHospital's own percentage_of_
;; patients analogue, the SAME shape sim-model/PathwaysConfig
;; already established for authored pathways (docs/gmf-interpreter.md's own
;; Task 4: module assignment composes with :pathways, both just IR entering
;; the union). ehrt.sim.engine/assign-module is this schema's own
;; resolver -- kept there, not here, mirroring assign-pathway's own placement
;; (the resolver needs a seeded RNG; the schema doesn't) -------------------

(def ModuleAssignment
  [:or
   [:map {:closed true} [:module-id :string] [:weight [:or :int :double]]]
   [:map {:closed true} [:patient-ordinal :int] [:module-id :string]]])

(def ModulesConfig
  [:vector ModuleAssignment])

(defn valid-modules-config? [config] (m/validate ModulesConfig config))
(defn explain-modules-config [config] (m/explain ModulesConfig config))

;; --- Registry (no hidden modules, section 5) -------------------------------

(defn empty-registry [] {})

(defn register
  "Adds `module` (an already-loaded, :ok'd module map) to `registry` under
  `id`. :rejected :module-id-collision when `id` is already registered --
  the registry-level half of section 5's own collision language (the
  other half, a bare reserved-attribute write, is caught by `load-module`
  itself, per-module, above)."
  [registry id module]
  (if (contains? registry id)
    (result/rejected :module-id-collision {:id id})
    (result/ok (assoc registry id module))))

(defn loaded-modules
  "The full loaded set, listable -- no hidden modules (section 5's own
  corollary): every registered module's id, name, state count, and
  declared (namespaced) attribute set."
  [registry]
  (mapv (fn [[id module]]
          {:id id :name (:name module) :state-count (count (:states module))
           :attributes (declared-attributes module)})
        registry))
