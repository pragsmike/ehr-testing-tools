(ns ehr-testing-sim.check-test
  "The invariant catalog's Milestone M1 additions (docs/operational-
  models.md, docs/patient-state-model.md's event-validity table):
  admission/transfer legality, transfer-from accuracy, no double
  occupancy, one-slot-per-admitted-patient, capacity, and surge-only-
  when-earlier-rungs-exhausted. Written before ehr-testing-sim.check
  grows these (ADR-0004 test-first).

  M2a (ADR-0010) additions: every hand-written log below now carries
  :participants (the fold-routing mechanism replay/check.clj need since
  :mrn is no longer the fold key), plus the two structural invariants
  ADR-0010 requires: every event has >=1 participant, and every
  participant id traces back to an :admission in the same log."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehr-testing-sim.check :as check]
            [ehr-testing-sim.config :as config]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.order-profiles :as order-profiles]
            [ehr-testing-sim.result :as result]))

(def test-facility
  {:id :t :wards [{:id :ed :name "ED" :beds 0 :surge-slots 4
                   :surge-format "%s-H%02d" :class :ed}
                  {:id :renal :name "Renal" :beds 1 :surge-slots 1
                   :surge-format "%s-H%02d" :class :inpatient}]})

(defn- subject
  "Test-fixture convenience: a single-participant :participants vector."
  [patient-id]
  [{:patient-id patient-id :role :subject}])

;; --- Event-validity rows (patient-state-model.md) -----------------------

(deftest admission-only-when-new-detects-double-admission
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :admission :t 10 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-02" :placement :licensed}}]]
    (is (seq (check/admission-only-when-new log)))))

(deftest admission-only-when-new-holds-for-legit-log
  (is (empty? (check/admission-only-when-new
               [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
                 :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
                {:event :discharge :t 10 :participants (subject "P1")}]))))

(deftest transfer-only-when-admitted-detects-transfer-before-admission
  (let [log [{:event :transfer :t 0 :home-ward "Renal" :participants (subject "P1")
              :from nil :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]]
    (is (seq (check/transfer-only-when-admitted log)))))

(deftest transfer-from-matches-state-detects-lying-event
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :transfer :t 10 :home-ward "Cardiology" :participants (subject "P1")
              :from {:ward "WRONG" :bed "WRONG-01" :placement :licensed}
              :location {:ward "Cardiology" :bed "CARDIOLOGY-01" :placement :licensed}}]]
    (is (seq (check/transfer-from-matches-state log)))))

(deftest transfer-from-matches-state-holds-when-honest
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :transfer :t 10 :home-ward "Cardiology" :participants (subject "P1")
              :from {:ward "Renal" :bed "RENAL-01" :placement :licensed}
              :location {:ward "Cardiology" :bed "CARDIOLOGY-01" :placement :licensed}}]]
    (is (empty? (check/transfer-from-matches-state log)))))

;; --- Occupancy invariants ------------------------------------------------

(deftest no-double-occupancy-detects-collision
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :admission :t 5 :home-ward "Renal" :participants (subject "P2")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]]
    (is (seq (check/no-double-occupancy log)))))

(deftest admitted-occupies-one-slot-detects-nil-location
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1") :location nil}]]
    (is (seq (check/admitted-occupies-one-slot log)))))

(deftest occupancy-within-capacity-detects-overflow
  (let [ward {:id :renal :name "Renal" :beds 1 :surge-slots 0
              :surge-format "%s-H%02d" :class :inpatient}
        facility {:id :t :wards [ward]}
        ;; two patients both claim they hold licensed beds in a 1-bed
        ;; ward -- an impossible log a bug could still produce.
        log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :admission :t 5 :home-ward "Renal" :participants (subject "P2")
              :location {:ward "Renal" :bed "RENAL-99" :placement :licensed}}]]
    (is (seq (check/occupancy-within-capacity log facility)))))

(deftest surge-only-when-earlier-rungs-exhausted-detects-premature-surge
  (let [facility test-facility
        ;; Renal has a free licensed bed, yet this admission claims surge.
        log [{:event :admission :t 0 :home-ward "Renal" :forced false :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-H01" :placement :surge}}]]
    (is (seq (check/surge-only-when-earlier-rungs-exhausted log facility)))))

(deftest surge-only-when-earlier-rungs-exhausted-allows-forced
  (let [facility test-facility
        log [{:event :admission :t 0 :home-ward "Renal" :forced true :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-H01" :placement :surge}}]]
    (is (empty? (check/surge-only-when-earlier-rungs-exhausted log facility)))))

;; --- ADR-0010: structural participant invariants -------------------------

(deftest every-event-has-participants-detects-empty-participants
  (is (seq (check/every-event-has-participants
            [{:event :admission :t 0 :home-ward "Renal" :participants []
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]))))

(deftest every-event-has-participants-holds-when-present
  (is (empty? (check/every-event-has-participants
               [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
                 :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]))))

(deftest participant-ids-exist-in-run-detects-unadmitted-participant
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             ;; P2 never appears in an :admission -- a stray/mistyped id.
             {:event :discharge :t 10 :participants (subject "P2")}]]
    (is (seq (check/participant-ids-exist-in-run log)))))

(deftest participant-ids-exist-in-run-holds-for-legit-log
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :discharge :t 10 :participants (subject "P1")}]]
    (is (empty? (check/participant-ids-exist-in-run log)))))

;; --- ADR-0011: the warm-up mark -------------------------------------------

(deftest warm-up-mark-matches-window-detects-mismarked-event
  (let [log [{:event :admission :t 5 :warm-up false :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]]
    ;; t=5 < warm-up-seconds=10, so :warm-up should be true -- it's false.
    (is (seq (check/warm-up-mark-matches-window log 10)))))

(deftest warm-up-mark-matches-window-holds-when-correct
  (let [log [{:event :admission :t 5 :warm-up true :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :discharge :t 20 :warm-up false :participants (subject "P1")}]]
    (is (empty? (check/warm-up-mark-matches-window log 10)))))

(deftest engine-run-warm-up-seconds-marks-exactly-the-window
  (let [{:keys [ground-truth]} (engine/run {:seed 42 :patients 5 :warm-up-seconds 100})]
    (is (empty? (check/warm-up-mark-matches-window ground-truth 100)))
    (testing "a nonzero window actually marks at least one early event for this seed"
      (is (some :warm-up ground-truth)))))

;; --- M2b: churn family invariants (co-landed with the step types) ------

(deftest cancel-references-existing-uncancelled-event-detects-phantom-target
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             ;; :cancels-event-id 5 doesn't exist in this log at all.
             {:event :cancel-admit :t 10 :cancels-event-id 5 :participants (subject "P1")}]]
    (is (seq (check/cancel-references-existing-uncancelled-event log)))))

(deftest cancel-references-existing-uncancelled-event-detects-wrong-type
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             ;; cancel-DISCHARGE pointed at an :admission event -- type mismatch.
             {:event :cancel-discharge :t 10 :cancels-event-id 0 :participants (subject "P1")}]]
    (is (seq (check/cancel-references-existing-uncancelled-event log)))))

(deftest cancel-references-existing-uncancelled-event-detects-double-cancel
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :cancel-admit :t 10 :cancels-event-id 0 :participants (subject "P1")}
             {:event :cancel-admit :t 20 :cancels-event-id 0 :participants (subject "P1")}]]
    (is (seq (check/cancel-references-existing-uncancelled-event log)))))

(deftest cancel-references-existing-uncancelled-event-holds-for-legit-cancel
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :cancel-admit :t 10 :cancels-event-id 0 :participants (subject "P1")}]]
    (is (empty? (check/cancel-references-existing-uncancelled-event log)))))

(deftest bed-swap-both-admitted-before-swap-detects-a-non-admitted-participant
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             ;; P2 was never admitted.
             {:event :bed-swap :t 10
              :participants [{:patient-id "P1" :role :subject} {:patient-id "P2" :role :subject}]
              :swap {"P1" {:from {:ward "Renal" :bed "RENAL-01" :placement :licensed} :to nil}
                     "P2" {:from nil :to {:ward "Renal" :bed "RENAL-01" :placement :licensed}}}}]]
    (is (seq (check/bed-swap-both-admitted-before-swap log)))))

(deftest bed-swap-both-admitted-before-swap-holds-for-legit-swap
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :admission :t 5 :home-ward "Renal" :participants (subject "P2")
              :location {:ward "Renal" :bed "RENAL-H01" :placement :surge}}
             {:event :bed-swap :t 10
              :participants [{:patient-id "P1" :role :subject} {:patient-id "P2" :role :subject}]
              :swap {"P1" {:from {:ward "Renal" :bed "RENAL-01" :placement :licensed}
                          :to {:ward "Renal" :bed "RENAL-H01" :placement :surge}}
                     "P2" {:from {:ward "Renal" :bed "RENAL-H01" :placement :surge}
                          :to {:ward "Renal" :bed "RENAL-01" :placement :licensed}}}}]]
    (is (empty? (check/bed-swap-both-admitted-before-swap log)))))

(def ^:private legit-merge-log
  [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
    :active-mrn "MRN000001" :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
   {:event :admission :t 5 :home-ward "Renal" :participants (subject "P2")
    :active-mrn "MRN000002" :location {:ward "Renal" :bed "RENAL-H01" :placement :surge}}
   {:event :merge :t 10
    :participants [{:patient-id "P1" :role :survivor} {:patient-id "P2" :role :merged}]
    :surviving-mrn "MRN000001" :merged-mrn "MRN000002" :merged-mrns #{"MRN000002"}}])

(deftest merge-survivor-absorbs-merged-mrns-holds-for-legit-merge
  (is (empty? (check/merge-survivor-absorbs-merged-mrns legit-merge-log))))

(deftest merge-survivor-absorbs-merged-mrns-detects-wrong-active-mrn
  (let [log (update-in legit-merge-log [2] assoc :surviving-mrn "MRN999999")]
    (is (seq (check/merge-survivor-absorbs-merged-mrns log)))))

(deftest no-events-after-merged-terminal-holds-for-legit-log
  (is (empty? (check/no-events-after-merged-terminal legit-merge-log))))

(deftest no-events-after-merged-terminal-detects-a-zombie-event
  (let [log (conj legit-merge-log
                  {:event :discharge :t 20 :participants (subject "P2")})]
    (is (seq (check/no-events-after-merged-terminal log)))))

;; --- ADR-0012: :step-rejected -------------------------------------------

(deftest step-rejected-reason-is-documented-detects-an-undocumented-reason
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :step-rejected :t 10 :participants (subject "P1")
              :attempted-step {:type :cancel-admit} :reason :not-a-real-reason}]]
    (is (seq (check/step-rejected-reason-is-documented log)))))

(deftest step-rejected-reason-is-documented-holds-for-every-real-reason
  (is (empty? (check/step-rejected-reason-is-documented
               (for [reason engine/documented-step-rejection-reasons]
                 {:event :step-rejected :t 10 :participants (subject "P1")
                  :attempted-step {:type :cancel-admit} :reason reason})))))

;; --- M3: order/result -----------------------------------------------------

(def ^:private cbc-profile (:cbc order-profiles/default-profiles))

(defn- legit-order-result-log
  []
  [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
    :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
   {:event :order-placed :t 10 :profile :cbc :concept (:concept cbc-profile) :participants (subject "P1")}
   {:event :result-available :t 100 :profile :cbc :order-event-id 1 :concept (:concept cbc-profile)
    :participants (subject "P1")
    :results (mapv (fn [a] {:concept (:concept a) :units (:units a) :value (:low (:reference-range a))
                            :reference-range (:reference-range a) :abnormal-flag :normal})
                    (:analytes cbc-profile))}])

(deftest order-only-when-admitted-holds-for-legit-log
  (is (empty? (check/order-only-when-admitted (legit-order-result-log)))))

(deftest order-only-when-admitted-detects-order-before-admission
  (let [log [{:event :order-placed :t 0 :profile :cbc :concept (:concept cbc-profile) :participants (subject "P1")}]]
    (is (seq (check/order-only-when-admitted log)))))

(deftest order-only-when-admitted-allows-a-result-arriving-after-discharge
  (testing "async turnaround: a result legitimately arrives after
            discharge (pending labs at discharge is real clinical
            traffic) -- NOT a violation"
    (let [log (conj (legit-order-result-log)
                    {:event :discharge :t 200 :participants (subject "P1")}
                    {:event :result-available :t 300 :profile :cbc :order-event-id 1
                     :concept (:concept cbc-profile) :participants (subject "P1")
                     :results []})]
      (is (empty? (check/order-only-when-admitted log))))))

(deftest result-references-existing-order-and-follows-it-in-time-holds-for-legit-log
  (is (empty? (check/result-references-existing-order-and-follows-it-in-time (legit-order-result-log)))))

(deftest result-references-existing-order-and-follows-it-in-time-detects-phantom-order
  (let [log [{:event :result-available :t 10 :profile :cbc :order-event-id 5
              :concept (:concept cbc-profile) :participants (subject "P1") :results []}]]
    (is (seq (check/result-references-existing-order-and-follows-it-in-time log)))))

(deftest result-references-existing-order-and-follows-it-in-time-detects-time-travel
  (let [log (update (legit-order-result-log) 2 assoc :t 5)] ;; before its own order's t=10
    (is (seq (check/result-references-existing-order-and-follows-it-in-time log)))))

(deftest result-analytes-match-order-profile-holds-for-legit-log
  (is (empty? (check/result-analytes-match-order-profile (legit-order-result-log) order-profiles/default-profiles))))

(deftest result-analytes-match-order-profile-detects-a-missing-analyte
  (let [log (update-in (legit-order-result-log) [2 :results] #(vec (rest %)))]
    (is (seq (check/result-analytes-match-order-profile log order-profiles/default-profiles)))))

(deftest abnormal-flags-consistent-with-value-vs-range-holds-for-legit-log
  (is (empty? (check/abnormal-flags-consistent-with-value-vs-range (legit-order-result-log)))))

(deftest abnormal-flags-consistent-with-value-vs-range-detects-a-lying-flag
  (let [log (update-in (legit-order-result-log) [2 :results 0] assoc :abnormal-flag :high)]
    (is (seq (check/abnormal-flags-consistent-with-value-vs-range log)))))

(deftest engine-run-with-order-profiles-satisfies-check-all
  (let [pathway {:name "cbc" :steps [{:type :admission :location "Renal"}
                                     {:type :order :profile :cbc}
                                     {:type :discharge}]}
        {:keys [ground-truth]} (engine/run {:seed 3 :patients 4 :pathways [{:pathway pathway :weight 1}]})]
    (is (result/ok? (check/check-all ground-truth)))))

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
