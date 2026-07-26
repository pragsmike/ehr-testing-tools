(ns ehr-testing-sim.run-test
  "The `sim run` capability's result-not-throw contract. Milestone M2b,
  Task 0: allocation-ladder exhaustion is a structured outcome, not a
  thrown exception -- ehr-testing-sim.facility/allocate returns
  {:exhausted true ...} instead of throwing, ehr-testing-sim.engine/run
  halts the loop and echoes it back, and run-command surfaces it as
  :error :capacity-exhausted with the patient, ward, and census in the
  payload (docs/clinical-realities.md's ED-diversion stub names the
  modeling gap this leaves for M3+: a real waiting/diversion state)."
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-sim.run :as run]
            [ehr-testing-sim.result :as result]))

(def ^:private one-bed-no-ed-facility
  "A facility with exactly one bed and no ED ward at all -- the second
  admission has nowhere to go at any ladder rung, guaranteeing
  exhaustion deterministically rather than relying on a lucky seed."
  {:id :tiny
   :wards [{:id :renal :name "Renal" :beds 1 :surge-slots 0
            :surge-format "%s-H%02d" :class :inpatient}]})

(deftest run-command-surfaces-capacity-exhaustion-as-a-structured-error
  (let [r (run/run-command {:seed 1 :patients 2 :facility one-bed-no-ed-facility})]
    (testing "an :error, not a thrown exception reaching the caller"
      (is (result/error? r))
      (is (= :capacity-exhausted (:category r))))
    (testing "payload carries patient, ward, and census"
      (let [{:keys [patient-id ward census]} (:payload r)]
        (is (string? patient-id))
        (is (= "Renal" ward))
        (is (= {"Renal" {:occupied 1 :capacity 1}} census))))))
