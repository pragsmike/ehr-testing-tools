(ns ehrt.sim-trajectory.vendored-sore-throat-test
  "GMF coverage Wave A payoff (2026-08-02, AR-5,
  `.agents/plans/2026-08-02-gmf-coverage-plan.md`): the THIRD real
  vendored module (resources/modules/sore_throat.json). Blocked at every
  prior survey (docs/gmf-interpreter.md's M5-prep and M7 appendices) by a
  mandatory-path condition-vocabulary gap on `Determine_if_Bacterial`'s
  own `At Least`-N-of compound (a modified-Centor-criteria gate wrapping
  `Symptom`/`Observation`/`Age` sub-conditions) -- state-type clean (44/44
  v1 types) the whole time, per the M7 survey's own row.

  Written test-first (sim/ADR-0004): the FIRST version of this file, run
  before `resources/modules/sore_throat.json` existed, went RED for the
  expected reason (the resource does not exist yet). Once Wave A's own
  condition-type commits landed (:symptom/:at-least/:or/:date/
  :observation, plus the already-built :active-allergy), every assertion
  below is expected to go GREEN -- this module needed no interpreter
  extension beyond the condition vocabulary itself, unlike sinusitis.json's
  own M5b vendoring."
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-trajectory.gmf-interpreter :as interp]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

(def sore-throat-json (slurp (io/resource "sim/modules/sore_throat.json")))
(def sinusitis-json (slurp (io/resource "sim/modules/sinusitis.json")))
(def appendicitis-json (slurp (io/resource "sim/modules/appendicitis.json")))
(def fixture-clinic-json (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json")))

(deftest vendored-sore-throat-loads-and-validates
  (let [loaded (gmf/load-module "sore-throat" sore-throat-json)]
    (is (result/ok? loaded)
        (str "expected the vendored module to validate against the v1 subset; got " (pr-str loaded)))))

(deftest full-vendored-set-registers-together-with-zero-attribute-collisions
  (testing "the registry's first real FOUR-module load: fixture-clinic (test
            fixture) + sinusitis + appendicitis + sore-throat (three real
            vendored modules) -- module-namespaced attributes
            (docs/gmf-interpreter.md section 5) make cross-module
            collisions structurally impossible, checked here for the full
            real vendored set"
    (let [sore-throat (:payload (gmf/load-module "sore-throat" sore-throat-json))
          sinusitis (:payload (gmf/load-module "sinusitis" sinusitis-json))
          appendicitis (:payload (gmf/load-module "appendicitis" appendicitis-json))
          fixture-clinic (:payload (gmf/load-module "fixture-clinic" fixture-clinic-json))
          registry (-> (gmf/empty-registry)
                       (gmf/register "fixture-clinic" fixture-clinic) :payload
                       (gmf/register "sinusitis" sinusitis) :payload
                       (gmf/register "appendicitis" appendicitis) :payload
                       (gmf/register "sore-throat" sore-throat))]
      (is (result/ok? registry))
      (let [listed (gmf/loaded-modules (:payload registry))]
        (is (= #{"fixture-clinic" "sinusitis" "appendicitis" "sore-throat"} (into #{} (map :id) listed)))
        (is (empty? (apply set/intersection (map :attributes listed))))))))

(def ^:private sore-throat (:payload (gmf/load-module "sore-throat" sore-throat-json)))

(defn- person [seed sex] (assoc (sim-model/persona (Random. seed) {}) :sex sex))

;; Onset is a monthly-tick, sub-1%-probability distributed_transition off
;; Potential_Infection (the SAME shape sinusitis.json's own onset loop
;; uses) -- 25 years of registration offset gives real room for it to fire
;; at least once over a realistic history-phase horizon.
(defn- registration-t-for [persona] (+ (interp/dob-epoch-day persona) (* 365 25)))
(def ^:private horizon-window-days (* 365 10))

(defspec vendored-sore-throat-walks-to-a-bounded-horizon-without-throwing 200
  (prop/for-all [seed gen/large-integer
                 sex (gen/elements [:female :male])]
    (let [p (person seed sex)
          reg-t (registration-t-for p)
          result (interp/run-module sore-throat (Random. seed) p reg-t (+ reg-t horizon-window-days))]
      (contains? #{:terminal :blocked :horizon-complete} (:status result)))))

;; --- Branch coverage through Determine_if_Bacterial's own `At Least`
;; compound (AR-5's own obligation): the module's modified-Centor gate has
;; TWO real thresholds -- >=5 of 6 criteria (Prescribe_Antibiotics, whose
;; own MedicationOrder events downstream carry Penicillin V or its
;; allergy-alternate RxNorm codes) and >=3 (Throat_Culture, a real
;; :procedure event, SNOMED 117015009) -- confirmed by reading the
;; vendored file directly (Step 1). Neither threshold-crossing state
;; itself emits a trajectory event (both are Simple, consumed internally,
;; docs/gmf-interpreter.md section 1's own table) -- branch coverage is
;; observed through each branch's own DOWNSTREAM real event instead.

(def ^:private antibiotic-codes #{"834061" "834102" "284215" "212446"})
(def ^:private culture-code "117015009")

(defn- well-mixed-candidate-seeds [n mixer-seed]
  (let [mixer (Random. mixer-seed)]
    (repeatedly n #(.nextLong mixer))))

(defn- walk-result [seed sex]
  (let [p (person seed sex)
        reg-t (registration-t-for p)]
    (interp/run-module sore-throat (Random. seed) p reg-t (+ reg-t horizon-window-days))))

(defn- medication-codes [events] (into #{} (mapcat (fn [e] (map :code (:codes e)))) (filter #(= :medication-order (:event %)) events)))
(defn- procedure-codes [events] (into #{} (mapcat (fn [e] (map :code (:codes e)))) (filter #(= :procedure (:event %)) events)))

(deftest at-least-5-of-6-criteria-branch-reaches-prescribe-antibiotics
  (testing "the >=5 threshold entry of Determine_if_Bacterial's own
            conditional_transition (docs/gmf-interpreter.md section 2's
            own :at-least dispatch, real vendored content)"
    (let [seed (first (keep (fn [seed]
                               (let [result (walk-result seed :male)]
                                 (when (seq (set/intersection antibiotic-codes (medication-codes (:trajectory result))))
                                   seed)))
                             (well-mixed-candidate-seeds 4000 20260802)))]
      (is (some? seed) "expected at least one well-mixed candidate seed to reach the >=5 branch")
      (let [result (walk-result seed :male)]
        (is (contains? #{:terminal :blocked :horizon-complete} (:status result)))
        (is (seq (set/intersection antibiotic-codes (medication-codes (:trajectory result)))))))))

(deftest at-least-3-of-6-criteria-branch-reaches-throat-culture-without-antibiotics
  (testing "the >=3-but-not->=5 threshold entry -- Throat_Culture fires
            (a real :procedure event) but the culture's own downstream
            Active-Condition check does not confirm streptococcal, so no
            antibiotic is ever ordered on this branch"
    (let [seed (first (keep (fn [seed]
                               (let [result (walk-result seed :male)
                                     events (:trajectory result)]
                                 (when (and (contains? (procedure-codes events) culture-code)
                                            (empty? (set/intersection antibiotic-codes (medication-codes events))))
                                   seed)))
                             (well-mixed-candidate-seeds 4000 20260802)))]
      (is (some? seed) "expected at least one well-mixed candidate seed to reach the >=3-only branch")
      (let [result (walk-result seed :male)
            events (:trajectory result)]
        (is (contains? (procedure-codes events) culture-code))
        (is (empty? (set/intersection antibiotic-codes (medication-codes events))))))))
