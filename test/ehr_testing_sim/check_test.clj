(ns ehr-testing-sim.check-test
  "The invariant catalog's Milestone M1 additions (docs/operational-
  models.md, docs/patient-state-model.md's event-validity table):
  admission/transfer legality, transfer-from accuracy, no double
  occupancy, one-slot-per-admitted-patient, capacity, and surge-only-
  when-earlier-rungs-exhausted. Written before ehr-testing-sim.check
  grows these (ADR-0004 test-first)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehr-testing-sim.check :as check]
            [ehr-testing-sim.config :as config]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.result :as result]))

(def test-facility
  {:id :t :wards [{:id :ed :name "ED" :beds 0 :surge-slots 4
                   :surge-format "%s-H%02d" :class :ed}
                  {:id :renal :name "Renal" :beds 1 :surge-slots 1
                   :surge-format "%s-H%02d" :class :inpatient}]})

;; --- Event-validity rows (patient-state-model.md) -----------------------

(deftest admission-only-when-new-detects-double-admission
  (let [log [{:event :admission :t 0 :mrn "MRN000001" :home-ward "Renal"
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :admission :t 10 :mrn "MRN000001" :home-ward "Renal"
              :location {:ward "Renal" :bed "RENAL-02" :placement :licensed}}]]
    (is (seq (check/admission-only-when-new log)))))

(deftest admission-only-when-new-holds-for-legit-log
  (is (empty? (check/admission-only-when-new
               [{:event :admission :t 0 :mrn "MRN000001" :home-ward "Renal"
                 :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
                {:event :discharge :t 10 :mrn "MRN000001"}]))))

(deftest transfer-only-when-admitted-detects-transfer-before-admission
  (let [log [{:event :transfer :t 0 :mrn "MRN000001" :home-ward "Renal"
              :from nil :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]]
    (is (seq (check/transfer-only-when-admitted log)))))

(deftest transfer-from-matches-state-detects-lying-event
  (let [log [{:event :admission :t 0 :mrn "MRN000001" :home-ward "Renal"
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :transfer :t 10 :mrn "MRN000001" :home-ward "Cardiology"
              :from {:ward "WRONG" :bed "WRONG-01" :placement :licensed}
              :location {:ward "Cardiology" :bed "CARDIOLOGY-01" :placement :licensed}}]]
    (is (seq (check/transfer-from-matches-state log)))))

(deftest transfer-from-matches-state-holds-when-honest
  (let [log [{:event :admission :t 0 :mrn "MRN000001" :home-ward "Renal"
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :transfer :t 10 :mrn "MRN000001" :home-ward "Cardiology"
              :from {:ward "Renal" :bed "RENAL-01" :placement :licensed}
              :location {:ward "Cardiology" :bed "CARDIOLOGY-01" :placement :licensed}}]]
    (is (empty? (check/transfer-from-matches-state log)))))

;; --- Occupancy invariants ------------------------------------------------

(deftest no-double-occupancy-detects-collision
  (let [log [{:event :admission :t 0 :mrn "MRN000001" :home-ward "Renal"
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :admission :t 5 :mrn "MRN000002" :home-ward "Renal"
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]]
    (is (seq (check/no-double-occupancy log)))))

(deftest admitted-occupies-one-slot-detects-nil-location
  (let [log [{:event :admission :t 0 :mrn "MRN000001" :home-ward "Renal" :location nil}]]
    (is (seq (check/admitted-occupies-one-slot log)))))

(deftest occupancy-within-capacity-detects-overflow
  (let [ward {:id :renal :name "Renal" :beds 1 :surge-slots 0
              :surge-format "%s-H%02d" :class :inpatient}
        facility {:id :t :wards [ward]}
        ;; two patients both claim they hold licensed beds in a 1-bed
        ;; ward -- an impossible log a bug could still produce.
        log [{:event :admission :t 0 :mrn "MRN000001" :home-ward "Renal"
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :admission :t 5 :mrn "MRN000002" :home-ward "Renal"
              :location {:ward "Renal" :bed "RENAL-99" :placement :licensed}}]]
    (is (seq (check/occupancy-within-capacity log facility)))))

(deftest surge-only-when-earlier-rungs-exhausted-detects-premature-surge
  (let [facility test-facility
        ;; Renal has a free licensed bed, yet this admission claims surge.
        log [{:event :admission :t 0 :mrn "MRN000001" :home-ward "Renal" :forced false
              :location {:ward "Renal" :bed "RENAL-H01" :placement :surge}}]]
    (is (seq (check/surge-only-when-earlier-rungs-exhausted log facility)))))

(deftest surge-only-when-earlier-rungs-exhausted-allows-forced
  (let [facility test-facility
        log [{:event :admission :t 0 :mrn "MRN000001" :home-ward "Renal" :forced true
              :location {:ward "Renal" :bed "RENAL-H01" :placement :surge}}]]
    (is (empty? (check/surge-only-when-earlier-rungs-exhausted log facility)))))

;; --- check-all: facility-aware, backward-compatible arity ---------------

(deftest check-all-defaults-facility-when-omitted
  (let [{:keys [ground-truth]} (engine/run {:seed 42 :patients 3})]
    (is (result/ok? (check/check-all ground-truth)))))

(defspec every-m1-run-satisfies-the-invariant-catalog 150
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 12)]
    (let [facility {:id :t :wards [{:id :ed :name "ED" :beds 0 :surge-slots 15
                                     :surge-format "%s-H%02d" :class :ed}
                                    {:id :renal :name "Renal" :beds 1 :surge-slots 0
                                     :surge-format "%s-H%02d" :class :inpatient}]}
          {:keys [ground-truth]} (engine/run {:seed seed :patients patients :facility facility})]
      (result/ok? (check/check-all ground-truth facility)))))
