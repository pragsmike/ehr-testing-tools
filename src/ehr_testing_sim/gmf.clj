(ns ehr-testing-sim.gmf
  "The GMF module loader (Milestone M5a Task 1, docs/gmf-interpreter.md
  section 1). Parses a Synthea Generic Module Framework JSON module,
  normalizes it to this project's own idiom (kebab-case keyword keys and
  state-name references, code systems as :snomed/:loinc/:rxnorm/:icd10cm/
  :cvx keywords per ehr-testing-sim.pathway/Concept), and validates it
  against the v1 subset docs/gmf-interpreter.md section 1 defines.

  Load-time enforcement, result-not-throw (ehr-testing-sim.result):
  a module using a state type OUTSIDE v1's subset (CallSubmodule, Counter,
  MultiObservation, Death, Device/DeviceEnd, CarePlanStart/CarePlanEnd --
  section 1's own deferred-type table) is REJECTED with
  :unsupported-state-type, never silently skipped and never thrown -- this
  is a stricter, mechanical gate than the informal 'read past what you
  don't execute' survey-reading section 1 also describes (that describes
  reading a module's states for SURVEY purposes, e.g. the design doc's own
  candidate-module appendix; this loader is the boundary a module crosses
  to actually be RUN, where ADR-0013 point 4's curation criterion applies
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
            [ehr-testing-sim.pathway :as pathway]
            [ehr-testing-sim.result :as result]
            [malli.core :as m]))

;; --- Normalization: JSON's snake_case/CamelCase -> this project's kebab
;; keyword idiom -----------------------------------------------------------

(defn slug
  "Any raw GMF name string (a JSON key, a state name, an attribute or
  symptom name) -> this project's own lower-kebab form -- 'Check_Age_Guard'
  and 'Nasal Congestion' both become the same shape ('check-age-guard',
  'nasal-congestion'), so state-map keys, transition-target references,
  and attribute names all compare and namespace uniformly. Public: also
  reused by ehr-testing-sim.gmf-interpreter to turn a Guard/conditional's
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
  ADR-0013/docs/gmf-interpreter.md's own recommendation) turned out to
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
   "DeviceEnd" :device-end})

(def ^:private code-system->keyword
  "GMF's own code-system strings -> ehr-testing-sim.pathway/Concept's
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
  these are a CLOSED v1 vocabulary, not free-form names."
  {"wellness" :wellness "ambulatory" :ambulatory "emergency" :emergency "inpatient" :inpatient})

(def ^:private condition-type->keyword
  "v1's condition predicates (docs/gmf-interpreter.md section 2): age,
  sex (Gender), attribute, PriorState -- plus, M5b, the log-query family
  `Active Condition`/`Active Medication` join as the architecturally-
  same-shape extension that document's own condition-vocabulary-gap
  note already named as the natural next step ('the identical shape to
  PriorState's own query, just keyed on a concept rather than a module
  state name'), `And` as a recursive compound wrapper, and `Active
  Allergy` as a documented, always-false simplification (this project's
  Persona has no allergy concept to query yet -- see ehr-testing-sim.gmf-
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
   "Active Allergy" :active-allergy "And" :and})

(defn- normalize-code
  "GMF's own code triplet -> ehr-testing-sim.pathway/Concept. M5b: :code
  is coerced to a string regardless of its own JSON type -- the vendored
  sinusitis.json carries at least one unquoted-JSON-number code value
  (Prescribe_Alternative_Antibiotic's own RxNorm code), and
  pathway/Concept requires a string. This is a representation
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

        (and (= :and condition-type) (:conditions condition))
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
    (:complex-transition state) (update :complex-transition #(mapv normalize-transition-entry %))))

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
                  (:encounter-class state) (update :encounter-class
                                                    (fn [c] (get encounter-class->keyword c (keyword (slug c)))))
                  (:condition-onset state) (update :condition-onset (fn [t] (keyword (slug t))))
                  (:medication-order state) (update :medication-order (fn [t] (keyword (slug t))))
                  (:device state) (update :device (fn [t] (keyword (slug t))))
                  (:target-encounter state) (update :target-encounter (fn [t] (keyword (slug t)))))
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

(def ^:private TransitionFields
  [[:direct-transition {:optional true} :keyword]
   [:distributed-transition {:optional true}
    [:vector [:map [:transition :keyword] [:distribution number?]]]]
   [:conditional-transition {:optional true}
    [:vector [:map [:transition {:optional true} :keyword] [:condition {:optional true} [:map-of :keyword :any]]]]]
   [:complex-transition {:optional true}
    [:vector [:map [:condition {:optional true} [:map-of :keyword :any]]
              [:distributions [:vector [:map [:transition :keyword] [:distribution number?]]]]]]]])

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
   [:condition-onset (with-transitions [:type [:= :condition-onset]] [:codes [:vector pathway/Concept]]
                        [:target-encounter {:optional true} :keyword])]
   [:condition-end (with-transitions [:type [:= :condition-end]] [:condition-onset {:optional true} :keyword])]
   [:encounter (with-transitions [:type [:= :encounter]]
                 [:encounter-class [:enum :wellness :ambulatory :emergency :inpatient]]
                 [:codes [:vector pathway/Concept]] [:reason {:optional true} :string])]
   [:encounter-end (into [:map [:type [:= :encounter-end]]] TransitionFields)]
   [:procedure (with-transitions [:type [:= :procedure]] [:codes [:vector pathway/Concept]]
                 [:target-encounter {:optional true} :keyword] [:reason {:optional true} :string]
                 [:duration {:optional true} Range])]
   [:observation (with-transitions [:type [:= :observation]] [:codes [:vector pathway/Concept]]
                   [:category {:optional true} :string] [:unit {:optional true} :string]
                   [:range {:optional true} Range])]
   [:medication-order (with-transitions [:type [:= :medication-order]] [:codes [:vector pathway/Concept]]
                        [:reason {:optional true} :string])]
   [:medication-end (with-transitions [:type [:= :medication-end]] [:medication-order {:optional true} :keyword])]
   ;; M5b: consumed-internally, like :simple -- see gmf-type->keyword's
   ;; own docstring note. :code is singular (GMF's own Device shape, one
   ;; equipment concept per state -- unlike :codes' plural elsewhere).
   [:device (with-transitions [:type [:= :device]] [:code {:optional true} pathway/Concept])]
   [:device-end (with-transitions [:type [:= :device-end]] [:device {:optional true} :keyword])]])

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

;; --- M5b: per-patient module assignment -- SimHospital's own percentage_of_
;; patients analogue, the SAME shape ehr-testing-sim.pathway/PathwaysConfig
;; already established for authored pathways (docs/gmf-interpreter.md's own
;; Task 4: module assignment composes with :pathways, both just IR entering
;; the union). ehr-testing-sim.engine/assign-module is this schema's own
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
