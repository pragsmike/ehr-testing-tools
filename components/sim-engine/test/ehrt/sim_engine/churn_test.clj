(ns ehrt.sim-engine.churn-test
  "InjectChurn (docs/sim-theory.edn): the pathway-ir x churn-profile ->
  operational-pathway IR->IR transform. Written before
  ehrt.sim-engine.churn exists (sim/ADR-0004 test-first). The stage's own
  laws (docs/sim-theory.edn's :churn entry) become property tests here:
  IR endomorphism, the clinical-steps invariant (only inserts, never
  removes/reorders/alters), and single-seeded-RNG determinism."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

(def ^:private sample-pathway
  {:name "admit-delay-discharge"
   :steps [{:type :admission :location "Renal"}
           {:type :delay :from 60 :to 120}
           {:type :discharge}]})

(def ^:private zero-profile churn/default-churn-profile)

(def ^:private always-cancel-discharge
  (assoc churn/default-churn-profile :cancel-discharge 1.0))

(def ^:private always-everything
  (into {} (map (fn [[k _]] [k 1.0])) churn/default-churn-profile))

;; --- config schema ----------------------------------------------------

(deftest default-churn-profile-is-valid-and-all-zero
  (is (churn/valid-churn-profile? churn/default-churn-profile))
  (is (every? zero? (vals churn/default-churn-profile))))

;; --- zero-probability identity ------------------------------------------

(deftest zero-profile-is-the-identity-transform
  (is (= sample-pathway (churn/inject sample-pathway zero-profile (Random. 1)))))

(defspec zero-profile-is-the-identity-transform-for-any-seed 100
  (prop/for-all [seed gen/large-integer]
    (= sample-pathway (churn/inject sample-pathway zero-profile (Random. ^long seed)))))

;; --- IR endomorphism ------------------------------------------------------

(deftest inject-output-is-valid-pathway-ir
  (is (sim-model/valid? (churn/inject sample-pathway always-everything (Random. 1)))))

(defspec inject-always-produces-valid-ir 100
  (prop/for-all [seed gen/large-integer
                 cancel-admit-p (gen/double* {:min 0.0 :max 1.0 :NaN? false})
                 cancel-transfer-p (gen/double* {:min 0.0 :max 1.0 :NaN? false})
                 cancel-discharge-p (gen/double* {:min 0.0 :max 1.0 :NaN? false})
                 tie-p (gen/double* {:min 0.0 :max 1.0 :NaN? false})
                 swap-p (gen/double* {:min 0.0 :max 1.0 :NaN? false})
                 merge-p (gen/double* {:min 0.0 :max 1.0 :NaN? false})]
    (let [profile {:cancel-admit cancel-admit-p :cancel-transfer cancel-transfer-p
                   :cancel-discharge cancel-discharge-p :transfer-in-error tie-p
                   :bed-swap swap-p :merge merge-p}]
      (sim-model/valid? (churn/inject sample-pathway profile (Random. ^long seed))))))

;; --- clinical-steps invariant: only inserts, strip recovers the input -----

(deftest strip-recovers-the-original-pathway
  (let [churned (churn/inject sample-pathway always-everything (Random. 1))]
    (testing "sanity: churn actually inserted something"
      (is (> (count (:steps churned)) (count (:steps sample-pathway)))))
    (is (= sample-pathway (churn/strip churned)))))

(defspec strip-of-inject-recovers-the-input-for-any-profile-and-seed 100
  (prop/for-all [seed gen/large-integer
                 p (gen/double* {:min 0.0 :max 1.0 :NaN? false})]
    (let [profile (into {} (map (fn [[k _]] [k p])) churn/default-churn-profile)]
      (= sample-pathway (churn/strip (churn/inject sample-pathway profile (Random. ^long seed)))))))

;; --- validity-table applicability oracle ----------------------------------

(deftest never-inserts-cancel-discharge-before-any-discharge-exists
  (testing "even at probability 1.0, no :cancel-discharge appears before
            the pathway's own :discharge step -- docs/patient-state-
            model.md's event-validity table gates it"
    (let [churned (churn/inject sample-pathway always-cancel-discharge (Random. 1))
          steps (:steps churned)
          discharge-idx (.indexOf ^java.util.List (mapv :type steps) :discharge)
          cancel-discharge-idxs (keep-indexed #(when (= :cancel-discharge %2) %1) (mapv :type steps))]
      (is (pos? (count cancel-discharge-idxs)) "sanity: at least one got inserted, after the discharge")
      (is (every? #(> % discharge-idx) cancel-discharge-idxs)))))

(deftest never-inserts-cancel-transfer-cancel-admit-or-bed-swap-into-a-pathway-with-no-admission
  (testing "a pathway that never admits offers no legal insertion point
            for anything that requires an existing/current admission"
    (let [no-admission {:name "just-a-delay" :steps [{:type :delay :from 1 :to 2}]}
          churned (churn/inject no-admission always-everything (Random. 1))]
      (is (= no-admission churned)))))

(defspec every-cancel-transfer-follows-an-uncancelled-transfer 200
  (prop/for-all [seed gen/large-integer]
    (let [pathway {:name "t" :steps [{:type :admission :location "Renal"}
                                     {:type :transfer :location "Cardiology"}
                                     {:type :delay :from 1 :to 2}
                                     {:type :discharge}]}
          churned (churn/inject pathway always-everything (Random. ^long seed))
          types (mapv :type (:steps churned))
          transfer-idx (.indexOf ^java.util.List types :transfer)]
      (every? #(> % transfer-idx) (keep-indexed #(when (= :cancel-transfer %2) %1) types)))))

;; --- determinism -----------------------------------------------------------

(deftest inject-is-deterministic-for-a-fixed-seed
  (is (= (churn/inject sample-pathway always-everything (Random. 42))
         (churn/inject sample-pathway always-everything (Random. 42)))))

(defspec inject-is-deterministic-for-any-seed 100
  (prop/for-all [seed gen/large-integer]
    (= (churn/inject sample-pathway always-everything (Random. ^long seed))
       (churn/inject sample-pathway always-everything (Random. ^long seed)))))

;; --- concrete: a churned transfer-in-error carries a real location -------

(deftest inserted-transfer-in-error-carries-the-most-recent-known-location
  (let [only-tie (assoc churn/default-churn-profile :transfer-in-error 1.0)
        churned (churn/inject sample-pathway only-tie (Random. 1))
        tie-step (first (filter #(= :transfer-in-error (:type %)) (:steps churned)))]
    (is (some? tie-step))
    (is (= "Renal" (:location tie-step)))))
