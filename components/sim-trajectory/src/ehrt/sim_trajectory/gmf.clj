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
   "CarePlanEnd" :care-plan-end})

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
  needs no `compile-trajectory` change."
  {"wellness" :wellness "ambulatory" :ambulatory "emergency" :emergency "inpatient" :inpatient
   "outpatient" :ambulatory})

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
   "Symptom" :symptom "Or" :or "At Least" :at-least "Date" :date "Observation" :observation})

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
        (update :conditions #(mapv normalize-condition %))))))

(defn- normalize-transition-entry
  [{:keys [transition condition distributions] :as entry}]
  (cond-> entry
    transition (assoc :transition (keyword (slug transition)))
    condition (assoc :condition (normalize-condition condition))
    distributions (assoc :distributions (mapv #(update % :transition (fn [t] (keyword (slug t)))) distributions))))

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
    (update :type-of-care-transition #(into {} (map (fn [[k t]] [k (keyword (slug t))])) %))))

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

(defn- normalize-state
  [state]
  (let [raw-type (:type state)
        kw-type (get gmf-type->keyword raw-type)]
    (if (nil? kw-type)
      {:unsupported-state-type {:raw-type raw-type}}
      (-> state
          (assoc :type kw-type)
          (cond-> (:codes state) (update :codes #(mapv normalize-code %))
                  (:code state) (update :code normalize-code)
                  (:allow state) (update :allow normalize-condition)
                  ;; GMF coverage Wave B (2026-08-02, ADR-0027): a second
                  ;; GMF wellness-encounter encoding this loader didn't
                  ;; recognize (docs/gmf-interpreter.md section 8's own
                  ;; M7 finding, mTBI/atrial_fibrillation/osteoporosis/
                  ;; epilepsy/med_rec -- now confirmed MANDATORY-path on
                  ;; ear_infections.json too, Step 1's own
                  ;; characterization): `"wellness": true` with no
                  ;; `encounter_class` key at all -> :encounter-class
                  ;; :wellness, the loader normalization that document's
                  ;; own prioritization table already named as "the
                  ;; cheapest fix in this table."
                  (and (= :encounter kw-type) (:wellness state) (not (:encounter-class state)))
                  (assoc :encounter-class :wellness)

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
                  ;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029):
                  ;; :value-code on a standalone :observation state
                  ;; (Capillary_Refill's own top-level shape); :observations
                  ;; on a :multi-observation/:diagnostic-report state (its
                  ;; own embedded children, D1a-2).
                  (:value-code state) (update :value-code normalize-code)
                  (:observations state) (update :observations #(mapv normalize-observation-child %)))
          normalize-transitions))))

(defn- normalize-states
  "Normalizes every state; short-circuits with the FIRST deferred-type
  state found (deterministic -- iterates in the module's own key order),
  since a module using even one deferred type fails load, full stop
  (this namespace's own docstring)."
  [raw-states]
  (reduce (fn [acc [state-name raw-state]]
            (let [normalized (normalize-state raw-state)]
              (if (:unsupported-state-type normalized)
                (reduced {:unsupported {:state state-name
                                        :raw-type (:raw-type (:unsupported-state-type normalized))}})
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
(def ^:private ObservationChild
  [:map
   [:category {:optional true} :string]
   [:unit {:optional true} :string]
   [:codes [:vector sim-model/Concept]]
   [:range {:optional true} Range]
   [:value-code {:optional true} sim-model/Concept]
   [:vital-sign {:optional true} :string]])

(def ^:private TransitionFields
  [[:direct-transition {:optional true} :keyword]
   [:distributed-transition {:optional true}
    [:vector [:map [:transition :keyword] [:distribution number?]]]]
   [:conditional-transition {:optional true}
    [:vector [:map [:transition {:optional true} :keyword] [:condition {:optional true} [:map-of :keyword :any]]]]]
   [:complex-transition {:optional true}
    [:vector [:map [:condition {:optional true} [:map-of :keyword :any]]
              [:distributions [:vector [:map [:transition :keyword] [:distribution number?]]]]]]]
   ;; GMF coverage Wave B (D5): no weights of its own (see
   ;; normalize-transitions' own comment) -- each of the three keys is
   ;; optional (a module may omit :telemedicine on an older care-
   ;; pathway authoring, real Synthea's own shape).
   [:type-of-care-transition {:optional true}
    [:map [:ambulatory {:optional true} :keyword]
     [:emergency {:optional true} :keyword]
     [:telemedicine {:optional true} :keyword]]]])

(defn- with-transitions [& kvs] (into [:map] (into (vec kvs) TransitionFields)))

(def GmfState
  [:multi {:dispatch :type}
   [:initial (into [:map [:type [:= :initial]]] TransitionFields)]
   [:terminal [:map [:type [:= :terminal]]]]
   [:simple (into [:map [:type [:= :simple]]] TransitionFields)]
   [:delay (with-transitions [:type [:= :delay]]
             [:range {:optional true} Range] [:exact {:optional true} Exact])]
   [:guard (with-transitions [:type [:= :guard]] [:allow [:map-of :keyword :any]])]
   [:set-attribute (with-transitions [:type [:= :set-attribute]] [:attribute :string] [:value {:optional true} :any])]
   [:symptom (with-transitions [:type [:= :symptom]] [:symptom :string]
               [:range {:optional true} Range] [:exact {:optional true} Exact])]
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
                 [:encounter-class [:enum :wellness :ambulatory :emergency :inpatient]]
                 [:codes {:optional true} [:vector sim-model/Concept]] [:reason {:optional true} :string])]
   [:encounter-end (into [:map [:type [:= :encounter-end]]] TransitionFields)]
   [:procedure (with-transitions [:type [:= :procedure]] [:codes [:vector sim-model/Concept]]
                 [:target-encounter {:optional true} :keyword] [:reason {:optional true} :string]
                 [:duration {:optional true} Range])]
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
             [:referenced-by-attribute {:optional true} :string])]])

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
  :attribute-collision (payload {:attribute name}) for a module whose own
  SetAttribute/Symptom writes a bare engine-reserved attribute name;
  :rejected :schema-invalid (payload {:explain ...}) for any other v1
  structural mismatch."
  [id json-text]
  (let [raw (json/read-str json-text :key-fn kebab-key)
        {:keys [states unsupported]} (normalize-states (:states raw))]
    (cond
      unsupported
      (result/rejected :unsupported-state-type unsupported)

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

  Returns a Result: :ok with {:root root-id :modules {root-id -> ...,
  call-path -> ...}} -- every call-path key is the submodule's own raw
  call-path string (also its own :id, section 5's own attribute-
  namespacing scope for LOAD-time declared-write collision checking --
  distinct from D1's own RUNTIME root-scoping, gmf-interpreter.md
  section 5's own dated note). The all-or-nothing gate (this
  namespace's own docstring, ADR-0013 point 4) extends over the WHOLE
  closure: :unsupported-state-type / :attribute-collision /
  :schema-invalid from ANY transitively-called submodule rejects the
  WHOLE closure (:rejected :submodule-rejected, payload {:call-path
  :reason}, `:reason` the submodule's own rejection Result -- always
  names which call-path failed and why, never silently which-one-of-
  many). :rejected :submodule-not-found (payload {:call-path}) when
  `resolve-fn` returns nil for a named call-path. :rejected
  :cyclic-closure (payload {:cycle [...]}) when the static call graph
  contains a cycle -- an ESCALATION-worthy finding (D3), never silently
  resolved by dropping an edge."
  [root-id root-json-text resolve-fn]
  (let [root-loaded (load-module root-id root-json-text)]
    (if-not (result/ok? root-loaded)
      root-loaded
      (let [root-module (:payload root-loaded)
            closure (resolve-closure resolve-fn [root-id] {root-id root-module} root-module)]
        (if (result/ok? closure)
          (result/ok {:root root-id :modules (:payload closure)})
          closure)))))

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
