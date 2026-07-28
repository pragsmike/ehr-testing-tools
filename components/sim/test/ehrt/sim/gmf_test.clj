(ns ehrt.sim.gmf-test
  "Red tests for the GMF module loader (M5a Task 1, docs/gmf-interpreter.md
  section 1, ADR-0013 point 6) -- written before ehrt.sim.gmf exists
  (ADR-0004 test-first). Covers: the hand-written fixture module loads and
  validates against the v1 subset; a module using a deferred state type is
  REJECTED with :unsupported-state-type (result-not-throw, never a throw);
  a module whose own SetAttribute writes a bare engine-reserved attribute
  name is REJECTED with :attribute-collision; the loaded set is listable
  (no hidden modules, docs/gmf-interpreter.md section 5)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.sim.gmf :as gmf]
            [ehrt.sim.result :as result]))

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
    (testing "small enough to hand-verify, per ADR-0013 point 6"
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
  "A deliberately malformed module: uses CallSubmodule (docs/gmf-
  interpreter.md's own deferred-type table) -- must be rejected, never
  thrown, never silently skipped."
  (str "{\"name\": \"Bad Module\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Recurse\"},"
       "   \"Recurse\": {\"type\": \"CallSubmodule\", \"submodule\": \"other\", \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(deftest module-with-deferred-state-type-is-rejected
  (let [loaded (gmf/load-module "bad-module" deferred-state-type-json)]
    (is (result/rejected? loaded))
    (is (= :unsupported-state-type (:category loaded)))
    (is (= :recurse (:state (:payload loaded))))
    (is (= "CallSubmodule" (:raw-type (:payload loaded))))))

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
