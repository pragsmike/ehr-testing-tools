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
