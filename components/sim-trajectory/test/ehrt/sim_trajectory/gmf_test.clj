(ns ehrt.sim-trajectory.gmf-test
  "Red tests for the GMF module loader (M5a Task 1, docs/gmf-interpreter.md
  section 1, sim/ADR-0013 point 6) -- written before ehrt.sim-trajectory.gmf exists
  (sim/ADR-0004 test-first). Covers: the hand-written fixture module loads and
  validates against the v1 subset; a module using a deferred state type is
  REJECTED with :unsupported-state-type (result-not-throw, never a throw);
  a module whose own SetAttribute writes a bare engine-reserved attribute
  name is REJECTED with :attribute-collision; the loaded set is listable
  (no hidden modules, docs/gmf-interpreter.md section 5)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.gmf :as gmf]))

(def fixture-clinic-json
  (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json")))

(deftest fixture-clinic-loads-and-validates
  (let [loaded (gmf/load-module "fixture-clinic" fixture-clinic-json)]
    (is (result/ok? loaded))
    (testing "every v1 state type appears at least once"
      (let [types (into #{} (map :type) (vals (:states (:payload loaded))))]
        (is (= #{:initial :terminal :simple :delay :guard :set-attribute :symptom
                 :condition-onset :condition-end :encounter :encounter-end
                 :procedure :observation :medication-order :medication-end}
               types))))
    (testing "small enough to hand-verify, per sim/ADR-0013 point 6"
      (is (<= 15 (count (:states (:payload loaded))) 30)))))

(deftest fixture-clinic-uses-all-four-transition-kinds
  (let [states (:states (:payload (gmf/load-module "fixture-clinic" fixture-clinic-json)))
        has-key? (fn [k] (some #(contains? % k) (vals states)))]
    (is (has-key? :direct-transition))
    (is (has-key? :distributed-transition))
    (is (has-key? :conditional-transition))
    (is (has-key? :complex-transition))))

(deftest fixture-clinic-concept-triplets-are-verbatim-and-real
  (testing "notes/facts-register.md F10-F12 -- no invented codes"
    (let [states (:states (:payload (gmf/load-module "fixture-clinic" fixture-clinic-json)))
          onset (get states :sinusitis-onset)
          encounter (get states :doctor-visit-encounter)
          observation (get states :take-temperature)
          med (get states :prescribe-amoxicillin)
          procedure (get states :sinus-surgery)]
      (is (= [{:system :snomed :code "36971009" :display "Sinusitis (disorder)"}] (:codes onset)))
      (is (= [{:system :snomed :code "185345009" :display "Encounter for symptom"}] (:codes encounter)))
      (is (= [{:system :loinc :code "8310-5" :display "Body temperature"}] (:codes observation)))
      (is (= [{:system :rxnorm :code "308191" :display "Amoxicillin 500 MG Oral Capsule"}] (:codes med)))
      (is (= [{:system :snomed :code "315618009"
               :display "FESS - Functional endoscopic sinus surgery - sphenoethmoidectomy"}]
             (:codes procedure))))))

(def deferred-state-type-json
  "A deliberately malformed module: uses ImagingStudy (docs/gmf-
  interpreter.md's own deferred-type table, still deferred -- ADR-0029
  R5, named OUT of GMF coverage Wave D with its own CHF trigger) --
  must be rejected, never thrown, never silently skipped.
  GMF coverage Wave B (2026-08-02, ADR-0027, D3): this test USED to name
  CallSubmodule as its own still-deferred example -- CallSubmodule joins
  v1 as a loadable state type this session (the loader now recognizes
  it and can discover its own :submodule call-paths, `gmf/load-closure`
  below); swapped to a type that is still genuinely deferred so this
  test keeps testing what its own docstring claims, not a stale premise.
  GMF coverage Wave D stage D1 (2026-08-02, ADR-0029): swapped AGAIN,
  from MultiObservation (this session's own example, now supported) to
  ImagingStudy, for the same reason -- a stale premise, not silently
  left to test what it no longer tests."
  (str "{\"name\": \"Bad Module\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Recurse\"},"
       "   \"Recurse\": {\"type\": \"ImagingStudy\", \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(deftest module-with-deferred-state-type-is-rejected
  (let [loaded (gmf/load-module "bad-module" deferred-state-type-json)]
    (is (result/rejected? loaded))
    (is (= :unsupported-state-type (:category loaded)))
    (is (= :recurse (:state (:payload loaded))))
    (is (= "ImagingStudy" (:raw-type (:payload loaded))))))

(def reserved-attribute-collision-json
  "A deliberately malformed module: SetAttribute writes the bare,
  non-namespaced engine-reserved key `donor` (docs/gmf-interpreter.md
  section 5; docs/patient-state-model.md's post-mortem entry) -- must be
  rejected at load time, before any state runs."
  (str "{\"name\": \"Bad Module 2\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Mark_Donor\"},"
       "   \"Mark_Donor\": {\"type\": \"SetAttribute\", \"attribute\": \"donor\", \"value\": true, \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(deftest module-writing-a-reserved-bare-attribute-is-rejected
  (let [loaded (gmf/load-module "bad-module-2" reserved-attribute-collision-json)]
    (is (result/rejected? loaded))
    (is (= :attribute-collision (:category loaded)))
    (is (= "donor" (:attribute (:payload loaded))))))

;; --- GMF coverage Wave C (2026-08-02, ADR-0028, C1): Death -----------------

(def death-json
  "A minimal Death-bearing module, the range time-form + codes cause-form
  stroke.json's own Death state uses (docs/gmf-interpreter.md section
  10) -- proves the real JSON->schema path, not just a hand-rolled map."
  (str "{\"name\": \"Death Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Die\"},"
       "   \"Die\": {\"type\": \"Death\","
       "             \"range\": {\"low\": 1, \"high\": 30, \"unit\": \"days\"},"
       "             \"codes\": [{\"system\": \"SNOMED-CT\", \"code\": \"230690007\", \"display\": \"Cerebrovascular accident (disorder)\"}],"
       "             \"direct_transition\": \"Terminal\"},"
       "   \"Terminal\": {\"type\": \"Terminal\"}"
       " }}"))

(deftest death-module-loads-and-validates
  (let [loaded (gmf/load-module "death-fixture" death-json)]
    (is (result/ok? loaded))
    (is (= :death (get-in (:payload loaded) [:states :die :type])))
    (is (= [{:system :snomed :code "230690007" :display "Cerebrovascular accident (disorder)"}]
           (get-in (:payload loaded) [:states :die :codes])))))

;; --- GMF coverage Wave D stage D1 (2026-08-02, ADR-0029): the
;; observation family -- MultiObservation/DiagnosticReport, plus
;; :observation's own new :value-code/:vital-sign fields -------------------

(def observation-family-json
  "A minimal closure exercising all three value-sourcing mechanisms
  (D1a-3) plus both ObservationGroup state types, the same field shapes
  sepsis.json's own Blood_Cultures/Record_Blood_Pressure/Record_Blood_
  Pressure_2/Pulse_Oximetry use -- proves the real JSON->schema path."
  (str "{\"name\": \"Observation Family Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Pulse_Oximetry\"},"
       "   \"Pulse_Oximetry\": {\"type\": \"Observation\", \"category\": \"vital-signs\", \"unit\": \"%\","
       "             \"codes\": [{\"system\": \"LOINC\", \"code\": \"59408-5\", \"display\": \"Oxygen saturation in Arterial blood by Pulse oximetry\"}],"
       "             \"vital_sign\": \"Oxygen Saturation\", \"direct_transition\": \"Blood_Cultures\"},"
       "   \"Blood_Cultures\": {\"type\": \"DiagnosticReport\","
       "             \"codes\": [{\"system\": \"LOINC\", \"code\": \"600-7\", \"display\": \"Bacteria identified in Blood by Culture\"}],"
       "             \"observations\": [{\"category\": \"laboratory\", \"unit\": \"\","
       "                 \"codes\": [{\"system\": \"LOINC\", \"code\": \"88262-1\", \"display\": \"Gram positive blood culture panel\"}],"
       "                 \"value_code\": {\"system\": \"SNOMED-CT\", \"code\": \"10828004\", \"display\": \"Positive (qualifier value)\"}}],"
       "             \"direct_transition\": \"Record_Blood_Pressure\"},"
       "   \"Record_Blood_Pressure\": {\"type\": \"MultiObservation\", \"category\": \"vital-signs\", \"number_of_observations\": 0,"
       "             \"codes\": [{\"system\": \"LOINC\", \"code\": \"85354-9\", \"display\": \"Blood pressure panel\"}],"
       "             \"observations\": [{\"category\": \"vital-signs\", \"unit\": \"mm[Hg]\","
       "                 \"codes\": [{\"system\": \"LOINC\", \"code\": \"8480-6\", \"display\": \"Systolic Blood Pressure\"}],"
       "                 \"range\": {\"low\": 90, \"high\": 120}}],"
       "             \"direct_transition\": \"Terminal\"},"
       "   \"Terminal\": {\"type\": \"Terminal\"}"
       " }}"))

(deftest observation-family-module-loads-and-validates
  (let [loaded (gmf/load-module "observation-family-fixture" observation-family-json)]
    (is (result/ok? loaded))))

(deftest standalone-observation-vital-sign-field-normalizes-verbatim
  (let [loaded (:payload (gmf/load-module "observation-family-fixture" observation-family-json))]
    (is (= "Oxygen Saturation" (get-in loaded [:states :pulse-oximetry :vital-sign]))
        "the raw vital-sign name is left untouched, no slug transform")))

(deftest diagnostic-report-state-normalizes-to-diagnostic-report-type-with-a-value-code-child
  (let [loaded (:payload (gmf/load-module "observation-family-fixture" observation-family-json))
        blood-cultures (get-in loaded [:states :blood-cultures])]
    (is (= :diagnostic-report (:type blood-cultures)))
    (is (= [{:system :loinc :code "600-7" :display "Bacteria identified in Blood by Culture"}]
           (:codes blood-cultures)))
    (is (= {:system :snomed :code "10828004" :display "Positive (qualifier value)"}
           (:value-code (first (:observations blood-cultures)))))))

(deftest multi-observation-state-normalizes-to-multi-observation-type-with-a-range-child
  (let [loaded (:payload (gmf/load-module "observation-family-fixture" observation-family-json))
        rbp (get-in loaded [:states :record-blood-pressure])]
    (is (= :multi-observation (:type rbp)))
    (is (= {:low 90 :high 120} (:range (first (:observations rbp)))))
    (is (= [{:system :loinc :code "8480-6" :display "Systolic Blood Pressure"}]
           (:codes (first (:observations rbp)))))))

(deftest loaded-modules-is-listable-no-hidden-modules
  (testing "docs/gmf-interpreter.md section 5's no-hidden-modules corollary"
    (let [registry (-> (gmf/empty-registry)
                        (gmf/register "fixture-clinic" (:payload (gmf/load-module "fixture-clinic" fixture-clinic-json)))
                        :payload)
          listed (gmf/loaded-modules registry)]
      (is (= 1 (count listed)))
      (is (= "fixture-clinic" (:id (first listed))))
      (is (= "Fixture Clinic" (:name (first listed))))
      (is (= 25 (:state-count (first listed))))
      (testing "declared attributes are namespaced by module id"
        (is (= #{:fixture-clinic/onset-logged :fixture-clinic/intake-branch
                 :fixture-clinic/medicated :fixture-clinic/nasal-congestion}
               (:attributes (first listed))))))))

(deftest registering-a-duplicate-module-id-is-rejected
  (testing "collision across loaded modules -- docs/gmf-interpreter.md
            section 1's Task 1.2 own phrasing, the registry-level half of
            the collision check (the other half is the reserved-attribute
            check above, per section 5)"
    (let [module (:payload (gmf/load-module "fixture-clinic" fixture-clinic-json))
          registry (:payload (gmf/register (gmf/empty-registry) "fixture-clinic" module))
          re-register (gmf/register registry "fixture-clinic" module)]
      (is (result/rejected? re-register))
      (is (= :module-id-collision (:category re-register))))))

;; --- GMF coverage Wave B (2026-08-02, ADR-0027, D3): loader closure
;; resolution -- gmf/load-closure ---------------------------------------

(def leaf-json
  (str "{\"name\": \"Leaf\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(def calls-leaf-json
  (str "{\"name\": \"Caller\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Call\"},"
       "  \"Call\": {\"type\": \"CallSubmodule\", \"submodule\": \"leaf\", \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(defn- resolver [paths] (fn [call-path] (get paths call-path)))

(deftest load-closure-resolves-root-plus-one-submodule
  (let [loaded (gmf/load-closure "caller" calls-leaf-json (resolver {"leaf" leaf-json}))]
    (is (result/ok? loaded))
    (is (= "caller" (:root (:payload loaded))))
    (is (= #{"caller" "leaf"} (into #{} (keys (:modules (:payload loaded))))))
    (is (= "leaf" (:id (get (:modules (:payload loaded)) "leaf"))))))

(def two-callers-share-leaf-json
  "Two DIFFERENT CallSubmodule states in the SAME module both name the
  SAME call-path -- confirms the shared/deduped resolution `resolve-
  closure`'s own docstring claims (Synthea's own Module.getModuleByPath
  cache, D3's own characterization)."
  (str "{\"name\": \"Caller\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"CallA\"},"
       "  \"CallA\": {\"type\": \"CallSubmodule\", \"submodule\": \"leaf\", \"direct_transition\": \"CallB\"},"
       "  \"CallB\": {\"type\": \"CallSubmodule\", \"submodule\": \"leaf\", \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(deftest load-closure-shares-a-submodule-called-from-two-places
  (let [resolve-calls (atom 0)
        resolve-fn (fn [call-path] (swap! resolve-calls inc) (get {"leaf" leaf-json} call-path))
        loaded (gmf/load-closure "caller" two-callers-share-leaf-json resolve-fn)]
    (is (result/ok? loaded))
    (is (= #{"caller" "leaf"} (into #{} (keys (:modules (:payload loaded))))))
    (is (= 1 @resolve-calls) "leaf is resolved once, not twice, despite two callers")))

(def calls-transitive-json
  (str "{\"name\": \"Mid\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Call\"},"
       "  \"Call\": {\"type\": \"CallSubmodule\", \"submodule\": \"leaf\", \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(def calls-mid-json
  (str "{\"name\": \"Root\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Call\"},"
       "  \"Call\": {\"type\": \"CallSubmodule\", \"submodule\": \"mid\", \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(deftest load-closure-resolves-transitively-two-levels-deep
  (let [loaded (gmf/load-closure "root" calls-mid-json (resolver {"mid" calls-transitive-json "leaf" leaf-json}))]
    (is (result/ok? loaded))
    (is (= #{"root" "mid" "leaf"} (into #{} (keys (:modules (:payload loaded))))))))

(deftest load-closure-rejects-when-a-submodule-is-not-found
  (let [loaded (gmf/load-closure "caller" calls-leaf-json (resolver {}))]
    (is (result/rejected? loaded))
    (is (= :submodule-not-found (:category loaded)))
    (is (= "leaf" (:call-path (:payload loaded))))))

(def calls-deferred-leaf-json
  "GMF coverage Wave D stage D1 (2026-08-02, ADR-0029): swapped from
  MultiObservation (now supported) to ImagingStudy (R5, still deferred),
  same reason as deferred-state-type-json above."
  (str "{\"name\": \"Leaf\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Bad\"},"
       "  \"Bad\": {\"type\": \"ImagingStudy\", \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(deftest load-closure-all-or-nothing-gate-extends-to-a-transitively-called-submodule
  (testing "D6/D3: a deferred-type use ANYWHERE in the closure rejects
            the WHOLE closure, citing which call-path failed"
    (let [loaded (gmf/load-closure "caller" calls-leaf-json (resolver {"leaf" calls-deferred-leaf-json}))]
      (is (result/rejected? loaded))
      (is (= :submodule-rejected (:category loaded)))
      (is (= "leaf" (:call-path (:payload loaded))))
      (is (= :unsupported-state-type (:category (:reason (:payload loaded))))))))

(def cyclic-a-json
  (str "{\"name\": \"A\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Call\"},"
       "  \"Call\": {\"type\": \"CallSubmodule\", \"submodule\": \"b\", \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(def cyclic-b-json
  (str "{\"name\": \"B\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Call\"},"
       "  \"Call\": {\"type\": \"CallSubmodule\", \"submodule\": \"a\", \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(deftest load-closure-rejects-a-cyclic-call-graph
  (testing "D3: a cyclic real-world closure is an ESCALATION with
            evidence, not a relaxation -- never silently broken by
            dropping an edge"
    (let [loaded (gmf/load-closure "a" cyclic-a-json (resolver {"a" cyclic-a-json "b" cyclic-b-json}))]
      (is (result/rejected? loaded))
      (is (= :cyclic-closure (:category loaded)))
      (is (= ["a" "b" "a"] (:cycle (:payload loaded)))))))

(deftest load-closure-with-no-call-submodule-states-is-just-the-root
  (let [loaded (gmf/load-closure "fixture-clinic" fixture-clinic-json (resolver {}))]
    (is (result/ok? loaded))
    (is (= #{"fixture-clinic"} (into #{} (keys (:modules (:payload loaded))))))))

;; --- GMF coverage Wave B (2026-08-02, ADR-0027): encounter-class loader
;; normalizations, disclosed addition -- both mandatory-path on
;; ear_infections.json's own closure (Step 1's own characterization) --

(def outpatient-encounter-class-json
  (str "{\"name\": \"Outpatient\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Visit\"},"
       "  \"Visit\": {\"type\": \"Encounter\", \"encounter_class\": \"outpatient\","
       "              \"codes\": [{\"system\": \"SNOMED-CT\", \"code\": \"185345009\", \"display\": \"Encounter for symptom\"}],"
       "              \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(deftest encounter-class-outpatient-is-aliased-onto-ambulatory
  (testing "a real GMF encounter_class value (ear_infections.json's own
            primary encounter) this loader's original 4-entry map didn't
            recognize -- same clinical concept :ambulatory already
            covers, per compile-trajectory's own encounter->step"
    (let [loaded (gmf/load-module "outpatient-mod" outpatient-encounter-class-json)]
      (is (result/ok? loaded))
      (is (= :ambulatory (:encounter-class (get-in (:payload loaded) [:states :visit])))))))

(def wellness-true-idiom-json
  (str "{\"name\": \"Wellness\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Visit\"},"
       "  \"Visit\": {\"type\": \"Encounter\", \"wellness\": true, \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(deftest wellness-true-boolean-idiom-normalizes-to-encounter-class-wellness-with-no-codes
  (testing "docs/gmf-interpreter.md section 8's own M7 finding
            (mTBI/atrial_fibrillation/osteoporosis/epilepsy/med_rec),
            confirmed MANDATORY-path on ear_infections.json too --
            :codes stays absent (code passthrough: never fabricate a
            concept the source module never carried)"
    (let [loaded (gmf/load-module "wellness-mod" wellness-true-idiom-json)]
      (is (result/ok? loaded))
      (let [visit (get-in (:payload loaded) [:states :visit])]
        (is (= :wellness (:encounter-class visit)))
        (is (not (contains? visit :codes)))))))

;; --- GMF coverage Wave B (2026-08-02, ADR-0027, D5): the fifth
;; transition kind, type_of_care_transition -- loader normalization ----

(def type-of-care-transition-json
  (str "{\"name\": \"CarePathways\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Pick\"},"
       "  \"Pick\": {\"type\": \"Simple\", \"type_of_care_transition\":"
       "            {\"ambulatory\": \"Ambulatory\", \"emergency\": \"ED\", \"telemedicine\": \"Telemedicine\"}},"
       "  \"Ambulatory\": {\"type\": \"Terminal\"},"
       "  \"ED\": {\"type\": \"Terminal\"},"
       "  \"Telemedicine\": {\"type\": \"Terminal\"}}}"))

(deftest type-of-care-transition-targets-normalize-to-state-name-keywords
  (let [loaded (gmf/load-module "care-pathways" type-of-care-transition-json)]
    (is (result/ok? loaded))
    (is (= {:ambulatory :ambulatory :emergency :ed :telemedicine :telemedicine}
           (:type-of-care-transition (get-in (:payload loaded) [:states :pick]))))))

;; --- GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3a, H2): the
;; sixth transition kind, lookup_table_transition -- loader normalization
;; plus closure DATA-FILE members (R4) -------------------------------------

(def lookup-table-transition-json
  (str "{\"name\": \"LookupCaller\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Pick\"},"
       "  \"Pick\": {\"type\": \"Simple\", \"lookup_table_transition\": ["
       "    {\"transition\": \"A\", \"default_probability\": 0.5, \"lookup_table_name\": \"t.csv\"},"
       "    {\"transition\": \"B\", \"default_probability\": 0.5, \"lookup_table_name\": \"t.csv\"}]},"
       "  \"A\": {\"type\": \"Terminal\"},"
       "  \"B\": {\"type\": \"Terminal\"}}}"))

(deftest lookup-table-transition-entries-normalize-transition-targets
  (let [loaded (gmf/load-module "lookup-caller" lookup-table-transition-json)]
    (is (result/ok? loaded))
    (is (= [{:transition :a :default-probability 0.5 :lookup-table-name "t.csv"}
            {:transition :b :default-probability 0.5 :lookup-table-name "t.csv"}]
           (:lookup-table-transition (get-in (:payload loaded) [:states :pick]))))))

(def t-csv "age,gender,A,B\n15-24,F,0.9,0.1\n15-24,M,0.2,0.8\n")

(defn- table-resolver [tables] (fn [table-name] (get tables table-name)))

(deftest load-closure-resolves-a-lookup-table-data-file-member
  (let [loaded (gmf/load-closure "lookup-caller" lookup-table-transition-json (resolver {})
                                  (table-resolver {"t.csv" t-csv}))]
    (is (result/ok? loaded))
    (is (= #{"t.csv"} (into #{} (keys (:tables (:payload loaded))))))
    (let [rows (get (:tables (:payload loaded)) "t.csv")]
      (is (= 2 (count rows)))
      (is (= [15 24] (:age-range (first rows))))
      (is (= {"gender" "F"} (:attributes (first rows))))
      (is (= {:a 0.9 :b 0.1} (:weights (first rows))))
      (is (= {:a 0.2 :b 0.8} (:weights (second rows)))))))

(deftest load-closure-rejects-a-missing-lookup-table
  (let [loaded (gmf/load-closure "lookup-caller" lookup-table-transition-json (resolver {})
                                  (table-resolver {}))]
    (is (result/rejected? loaded))
    (is (= :lookup-table-not-found (:category loaded)))
    (is (= "t.csv" (:table-name (:payload loaded))))))

(def bad-column-t-csv "age,eye_color,A,B\n15-24,brown,0.9,0.1\n")

(deftest load-closure-rejects-an-unrecognized-lookup-table-column
  (testing "H2's own specify-vs-delegate audit: a column outside
            age/gender is an ESCALATION, never silently generalized"
    (let [loaded (gmf/load-closure "lookup-caller" lookup-table-transition-json (resolver {})
                                    (table-resolver {"t.csv" bad-column-t-csv}))]
      (is (result/rejected? loaded))
      (is (= :unrecognized-lookup-table-column (:category loaded)))
      (is (= "eye_color" (:column (:payload loaded)))))))

(deftest load-closure-with-no-lookup-table-transition-has-empty-tables
  (let [loaded (gmf/load-closure "fixture-clinic" fixture-clinic-json (resolver {}))]
    (is (result/ok? loaded))
    (is (= {} (:tables (:payload loaded))))))

;; --- GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3b, H3):
;; attribute-weighted distributed_transition (NamedDistribution) --------

(def named-distribution-json
  "stroke.json's own Chance_of_Stroke shape, byte-confirmed against
  source (D3b): a distributed_transition entry's own :distribution is a
  NamedDistribution map, not a plain number."
  (str "{\"name\": \"NamedDist\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Roll\"},"
       "  \"Roll\": {\"type\": \"Simple\", \"distributed_transition\": ["
       "    {\"transition\": \"Onset\", \"distribution\": {\"attribute\": \"stroke_risk\", \"default\": 0}},"
       "    {\"transition\": \"Wait\", \"distribution\": 1}]},"
       "  \"Onset\": {\"type\": \"Terminal\"},"
       "  \"Wait\": {\"type\": \"Terminal\"}}}"))

(deftest named-distribution-loads-and-validates
  (let [loaded (gmf/load-module "named-dist" named-distribution-json)]
    (is (result/ok? loaded))
    (is (= {:attribute "stroke_risk" :default 0}
           (:distribution (first (get-in (:payload loaded) [:states :roll :distributed-transition])))))))

;; --- GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3c finding
;; 1): gmf_version 2's own distribution+unit timing encoding, a
;; disclosed loader-normalization addition (not one of H1-H4's own
;; three named mechanisms, the same "cheap, mechanical" precedent
;; ADR-0027's own Step 2e already established) --------------------------

(def gmf-v2-delay-json
  "uti/hpi.json's own History Taking / Dysuria shape, byte-confirmed
  against source (D3c finding 1): UNIFORM -> Delay's own top-level
  :range; EXACT -> Delay's own top-level :exact (no unit -- Symptom's
  own unitless severity)."
  (str "{\"name\": \"GmfV2\", \"gmf_version\": 2, \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Wait\"},"
       "  \"Wait\": {\"type\": \"Delay\", \"distribution\": {\"kind\": \"UNIFORM\", \"parameters\": {\"low\": 5, \"high\": 15}},"
       "            \"unit\": \"minutes\", \"direct_transition\": \"Sev\"},"
       "  \"Sev\": {\"type\": \"Symptom\", \"symptom\": \"Pain\","
       "           \"distribution\": {\"kind\": \"EXACT\", \"parameters\": {\"value\": 0.5}},"
       "           \"direct_transition\": \"Proc\"},"
       "  \"Proc\": {\"type\": \"Procedure\", \"codes\": [{\"system\": \"SNOMED-CT\", \"code\": \"1\", \"display\": \"Test\"}],"
       "            \"distribution\": {\"kind\": \"EXACT\", \"parameters\": {\"value\": 15}},"
       "            \"unit\": \"minutes\", \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(deftest gmf-v2-delay-uniform-translates-to-the-v1-range-shape
  (let [loaded (gmf/load-module "gmf-v2" gmf-v2-delay-json)]
    (is (result/ok? loaded))
    (let [wait (get-in (:payload loaded) [:states :wait])]
      (is (= {:low 5 :high 15 :unit "minutes"} (:range wait)))
      (is (not (contains? wait :distribution)))
      (is (not (contains? wait :unit))))))

(deftest gmf-v2-symptom-exact-translates-to-the-v1-exact-shape-no-unit
  (let [loaded (gmf/load-module "gmf-v2" gmf-v2-delay-json)]
    (is (result/ok? loaded))
    (is (= {:quantity 0.5} (:exact (get-in (:payload loaded) [:states :sev]))))))

(deftest gmf-v2-procedure-exact-translates-to-a-degenerate-duration-range
  (testing "Procedure's own :duration schema stays Range-only -- EXACT
            becomes :low = :high, not a widened schema (D3c finding 1)"
    (let [loaded (gmf/load-module "gmf-v2" gmf-v2-delay-json)]
      (is (result/ok? loaded))
      (is (= {:low 15 :high 15 :unit "minutes"} (:duration (get-in (:payload loaded) [:states :proc])))))))

;; --- GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3d finding
;; 1): SetAttribute's own :value-code field, a disclosed addition -----

(def set-attribute-value-code-json
  "TJR's own Pre_Procedure_Encounter_Reason shape, byte-confirmed
  against source (D3d finding 1)."
  (str "{\"name\": \"ValueCode\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Set\"},"
       "  \"Set\": {\"type\": \"SetAttribute\", \"attribute\": \"reason\","
       "           \"value_code\": {\"system\": \"SNOMED-CT\", \"code\": \"110466009\", \"display\": \"Pre-surgery evaluation (procedure)\"},"
       "           \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(deftest set-attribute-value-code-loads-and-normalizes-as-a-concept
  (let [loaded (gmf/load-module "value-code" set-attribute-value-code-json)]
    (is (result/ok? loaded))
    (is (= {:system :snomed :code "110466009" :display "Pre-surgery evaluation (procedure)"}
           (:value-code (get-in (:payload loaded) [:states :set]))))))

;; --- GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3d finding
;; 2): an embedded observation child's own :exact value mechanism -----

(def observation-child-exact-json
  "TJR's own PROMIS29_Total_Assessment shape, byte-confirmed against
  source (D3d finding 2): a FOURTH value-sourcing mechanism alongside
  range/value_code/vital_sign."
  (str "{\"name\": \"ChildExact\", \"states\": {"
       "  \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Assess\"},"
       "  \"Assess\": {\"type\": \"MultiObservation\", \"category\": \"survey\","
       "              \"codes\": [{\"system\": \"LOINC\", \"code\": \"1\", \"display\": \"Test panel\"}],"
       "              \"observations\": [{\"category\": \"survey\", \"unit\": \"{score}\","
       "                \"codes\": [{\"system\": \"LOINC\", \"code\": \"2\", \"display\": \"Test item\"}],"
       "                \"exact\": {\"quantity\": 1}}],"
       "              \"direct_transition\": \"Done\"},"
       "  \"Done\": {\"type\": \"Terminal\"}}}"))

(deftest observation-child-exact-loads-and-validates
  (let [loaded (gmf/load-module "child-exact" observation-child-exact-json)]
    (is (result/ok? loaded))
    (is (= {:quantity 1}
           (:exact (first (get-in (:payload loaded) [:states :assess :observations])))))))
