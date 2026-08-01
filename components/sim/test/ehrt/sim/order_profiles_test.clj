(ns ehrt.sim.order-profiles-test
  "Schema validity and sampling laws for the order-profiles catalytic
  (docs/sim-theory.edn's `order-profiles`, target 3 -- hashed repo-
  authored config). Written before ehrt.sim.order-profiles
  exists (sim/ADR-0004 test-first)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim.order-profiles :as order-profiles])
  (:import [java.util Random]))

(deftest default-order-profiles-loads-and-validates
  (is (order-profiles/valid-profiles? order-profiles/default-profiles))
  (testing "the starter set is exactly CBC and BMP"
    (is (= #{:cbc :bmp} (set (keys order-profiles/default-profiles))))))

(deftest no-cpt-codes-anywhere-in-any-profile
  (testing "docs/third-party-sources.md's standing constraint: CPT is
            AMA-licensed and excluded -- every concept in every profile
            (panel-level and analyte-level) is :loinc, never :cpt"
    (doseq [[_id profile] order-profiles/default-profiles]
      (is (= :loinc (:system (:concept profile))))
      (doseq [analyte (:analytes profile)]
        (is (= :loinc (:system (:concept analyte))))))))

(deftest every-analyte-has-a-real-loinc-code-and-us-units
  (testing "sanity spot-check against notes/facts-register.md F7"
    (let [cbc-codes (set (map (comp :code :concept) (:analytes (:cbc order-profiles/default-profiles))))
          bmp-codes (set (map (comp :code :concept) (:analytes (:bmp order-profiles/default-profiles))))]
      (is (= #{"6690-2" "789-8" "718-7" "4544-3" "777-3"} cbc-codes))
      (is (= #{"2339-0" "6299-2" "38483-4" "49765-1" "2947-0" "6298-4" "2069-3" "20565-8"} bmp-codes)))
    (is (every? #{"K/uL" "M/uL" "g/dL" "%" "mg/dL" "mmol/L"}
                (mapcat (fn [[_id p]] (map :units (:analytes p))) order-profiles/default-profiles)))))

(def ^:private wbc
  (first (:analytes (:cbc order-profiles/default-profiles))))

(deftest sample-analyte-value-is-deterministic
  (is (= (order-profiles/sample-analyte-value (Random. 42) wbc)
         (order-profiles/sample-analyte-value (Random. 42) wbc))))

(defspec sampled-values-always-fall-within-one-of-the-three-declared-ranges 200
  (prop/for-all [seed gen/large-integer
                 profile-id (gen/elements (keys order-profiles/default-profiles))]
    (let [profile (get order-profiles/default-profiles profile-id)
          rng (Random. seed)]
      (every? (fn [analyte]
                (let [v (order-profiles/sample-analyte-value rng analyte)
                      {:keys [reference-range]} analyte
                      {:keys [abnormal-low-range abnormal-high-range]} (:distribution analyte)]
                  (or (<= (:low reference-range) v (:high reference-range))
                      (<= (:low abnormal-low-range) v (:high abnormal-low-range))
                      (<= (:low abnormal-high-range) v (:high abnormal-high-range)))))
              (:analytes profile)))))

(deftest abnormal-flag-is-computed-from-value-vs-reference-range
  (testing "mini-law: the abnormal flag is DERIVED from value vs range,
            never sampled independently (docs/patient-state-model.md
            Task 4's own computed-flag mini-law, stated here at the
            pure-function level order-profiles provides)"
    (let [{:keys [low high]} (:reference-range wbc)]
      (is (= :normal (order-profiles/abnormal-flag (/ (+ low high) 2) (:reference-range wbc))))
      (is (= :low (order-profiles/abnormal-flag (- low 0.1) (:reference-range wbc))))
      (is (= :high (order-profiles/abnormal-flag (+ high 0.1) (:reference-range wbc))))
      (is (= :normal (order-profiles/abnormal-flag low (:reference-range wbc))) "boundary: low itself is in-range")
      (is (= :normal (order-profiles/abnormal-flag high (:reference-range wbc))) "boundary: high itself is in-range"))))

(defspec abnormal-flag-agrees-with-sample-analyte-value-for-any-draw 200
  (prop/for-all [seed gen/large-integer]
    (let [v (order-profiles/sample-analyte-value (Random. seed) wbc)
          flag (order-profiles/abnormal-flag v (:reference-range wbc))
          {:keys [low high]} (:reference-range wbc)]
      (case flag
        :normal (<= low v high)
        :low (< v low)
        :high (> v high)))))
