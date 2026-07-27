(ns ehr-testing-sim.pathway-test
  "Schema validity for the IR step vocabulary as it grows past the v0
  walking skeleton -- M1 adds :transfer and the optional
  :force-placement authoring escape hatch (docs/operational-models.md)."
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-sim.pathway :as pathway]))

(deftest sample-admission-discharge-still-valid
  (is (pathway/valid? pathway/sample-admission-discharge)))

(deftest transfer-step-is-valid-ir
  (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"}
                                          {:type :transfer :location "Cardiology"}
                                          {:type :discharge}]})))

(deftest force-placement-is-valid-on-admission-and-transfer
  (testing "admission"
    (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"
                                             :force-placement {:ward "Renal" :bed "RENAL-02"}}]})))
  (testing "transfer"
    (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"}
                                            {:type :transfer :location "Cardiology"
                                             :force-placement {:ward "Cardiology" :bed "CARDIOLOGY-01"}}]}))))

(deftest transfer-without-location-is-invalid
  (is (not (pathway/valid? {:name "t" :steps [{:type :transfer}]}))))

;; --- M2b: churn family IR ------------------------------------------------

(deftest cancel-family-steps-are-valid-ir
  (doseq [step-type [:cancel-admit :cancel-transfer :cancel-discharge]]
    (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"}
                                            {:type step-type}]})
        (str step-type " should be valid IR"))))

(deftest transfer-in-error-is-valid-ir
  (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"}
                                          {:type :transfer-in-error :location "Cardiology"}]})))

(deftest transfer-in-error-without-location-is-invalid
  (is (not (pathway/valid? {:name "t" :steps [{:type :transfer-in-error}]}))))

(deftest bed-swap-is-valid-ir-with-and-without-explicit-peer
  (is (pathway/valid? {:name "t" :steps [{:type :bed-swap}]}))
  (is (pathway/valid? {:name "t" :steps [{:type :bed-swap :with "PID-000001"}]})))

(deftest merge-is-valid-ir-with-and-without-explicit-peer
  (is (pathway/valid? {:name "t" :steps [{:type :merge}]}))
  (is (pathway/valid? {:name "t" :steps [{:type :merge :with "PID-000001"}]})))

;; --- M3-adjacent: per-patient pathway assignment (roadmap.md's M3 entry) --

(deftest weighted-pool-entry-is-valid-pathways-config
  (is (pathway/valid-pathways-config?
       [{:pathway pathway/sample-admission-discharge :weight 1}])))

(deftest explicit-ordinal-entry-is-valid-pathways-config
  (is (pathway/valid-pathways-config?
       [{:patient-ordinal 0 :pathway pathway/sample-admission-discharge}])))

(deftest mixed-weighted-and-explicit-entries-are-valid-pathways-config
  (is (pathway/valid-pathways-config?
       [{:pathway pathway/sample-admission-discharge :weight 2}
        {:pathway {:name "t" :steps [{:type :admission :location "Renal"}]} :weight 1}
        {:patient-ordinal 3 :pathway pathway/sample-admission-discharge}])))

(deftest entry-with-neither-weight-nor-ordinal-is-invalid
  (is (not (pathway/valid-pathways-config? [{:pathway pathway/sample-admission-discharge}]))))

(deftest entry-with-invalid-pathway-ir-is-invalid
  (is (not (pathway/valid-pathways-config? [{:pathway {:steps "not-a-pathway"} :weight 1}]))))

;; --- M3: order (result auto-pairs, never hand-authored -- see engine.clj) --

(deftest order-step-is-valid-ir
  (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"}
                                          {:type :order :profile :cbc}]})))

(deftest order-without-profile-is-invalid
  (is (not (pathway/valid? {:name "t" :steps [{:type :order}]}))))
