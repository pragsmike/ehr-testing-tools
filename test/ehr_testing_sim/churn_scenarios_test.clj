(ns ehr-testing-sim.churn-scenarios-test
  "M2b's scripted regression fleet: authored, hand-driven scenarios
  exercising the compound cases the catalog names --
  docs/clinical-realities.md's newborn-merge entry (merge-while-
  boarding), transfer-in-error then cancel-then-retransfer, and a
  bed-swap between a licensed and a surge occupant. Each scenario
  asserts the EXACT ground-truth event sequence AND the exact rendered
  HL7v2 message sequence, not just that some invariant holds -- these
  are regression fixtures, driving decide/evolve directly (the same
  scripted-two-patient pattern ehr-testing-sim.engine-test's
  bed-ready-transfer-scripted-two-patients already established, since
  ehr-testing-sim.engine/run has no per-patient pathway mechanism for
  authoring a SPECIFIC two-patient interaction -- decide/evolve driven
  by hand IS the authored pathway here)."
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.emit-hl7 :as emit-hl7]
            [com.nervestaple.hl7-parser.parser :as parser]
            [com.nervestaple.hl7-parser.message :as message])
  (:import [java.util Random]))

(def ^:private ref-date "2024-01-01")
(def ^:private utc-offset "+00:00")

(def ^:private one-bed-one-surge-facility
  "Renal: exactly one licensed bed and one surge slot -- a second
  admission takes the surge slot (bed-swap fixture); a THIRD admission
  boards in ED (merge-while-boarding fixture)."
  {:id :scenario-fleet
   :wards [{:id :ed :name "ED" :beds 0 :surge-slots 4
            :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 1 :surge-slots 1
            :surge-format "%s-H%02d" :class :inpatient}]})

(def ^:private providers
  [{:id "1234567893" :name {:family "Chen" :given "A"} :role :attending
    :specialty "Nephrology" :wards [:renal :ed]}])

(defn- world-of
  [patients]
  {:patients patients :facility one-bed-one-surge-facility :providers providers :ground-truth []})

(defn- fold-events
  [world events]
  (-> (reduce (fn [w ev]
                (reduce (fn [w2 {:keys [patient-id]}]
                          (update-in w2 [:patients patient-id] engine/evolve ev))
                        w (:participants ev)))
              world events)
      (update :ground-truth into events)))

(defn- step!
  "Decides+folds `step` for `patient-id` at `t`, asserting it neither
  exhausted nor rejected (a scripted scenario's own steps are all meant
  to be legal) -- returns the updated world."
  [world t patient-id step]
  (let [{:keys [events exhausted rejected] :as outcome} (engine/decide (Random. 1) t world patient-id step)]
    (assert (nil? exhausted) (str "unexpected exhaustion: " exhausted))
    (assert (nil? rejected) (str "unexpected rejection: " rejected))
    (fold-events world events)))

(defn- triggers
  "The ordered A-trigger sequence a ground-truth log renders to, for
  compact exact-sequence assertions."
  [ground-truth facility providers]
  (mapv #(second (re-find #"\^(A\d+)" %))
        (emit-hl7/emit ground-truth ref-date utc-offset facility providers)))

;; --- Scenario 1: merge-while-boarding --------------------------------------
;; docs/clinical-realities.md's newborn Babyboy/Babygirl entry: "a natural
;; churn generator... merge steps get an organic scenario" -- here scripted
;; directly rather than emerging from a pathway, but the same shape: a
;; patient created while boarding gets merged into another identity.

(deftest merge-while-boarding
  ;; Renal has one licensed bed and one surge slot: P1 takes the licensed
  ;; bed (rung 1), P2 takes the surge slot (rung 2, still home-ward Renal,
  ;; NOT boarding), and P3 -- with both Renal rungs now exhausted -- boards
  ;; in ED surge (rung 4: home-ward Renal, physically ED). P3 is then
  ;; merged into P1 while still boarding.
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")
                          "P3" (engine/initial-patient "P3" "MRN000003")})
        world1 (-> world0
                  (step! 0 "P1" {:type :admission :location "Renal"})   ;; licensed
                  (step! 5 "P2" {:type :admission :location "Renal"})  ;; surge
                  (step! 6 "P3" {:type :admission :location "Renal"})) ;; boards (ED)
        p3-before-merge (get-in world1 [:patients "P3"])
        _ (testing "sanity: P3 is genuinely boarding at this point"
            (is (= "Renal" (:home-ward p3-before-merge)))
            (is (= "ED" (get-in p3-before-merge [:location :ward]))))
        world2 (step! world1 10 "P1" {:type :merge :with "P3"})
        survivor (get-in world2 [:patients "P1"])
        merged (get-in world2 [:patients "P3"])]
    (testing "exact ground-truth event sequence"
      (is (= [:admission :admission :admission :merge] (mapv :event (:ground-truth world2)))))
    (testing "the merged patient was boarding at the moment of merge, and stays merged, not reverted to any prior status"
      (is (= :merged (:status merged)))
      (is (= #{"MRN000001" "MRN000003"} (:mrns survivor)))
      (is (= "MRN000001" (:active-mrn survivor))))
    (testing "exact rendered message sequence: A01 A01 A01 A40"
      (is (= ["A01" "A01" "A01" "A40"]
             (triggers (:ground-truth world2) one-bed-one-surge-facility providers))))
    (testing "the A40 carries PID=survivor, MRG=merged's prior mrn"
      (let [a40 (last (emit-hl7/emit (:ground-truth world2) ref-date utc-offset one-bed-one-surge-facility providers))
            parsed (parser/parse a40)]
        (is (= "MRN000001" (message/get-field-first-value parsed "PID" 3)))
        (is (= "MRN000003" (message/get-field-first-value parsed "MRG" 1)))))))

;; --- Scenario 2: transfer-in-error, then cancel-then-retransfer -----------

(deftest transfer-in-error-then-cancel-then-retransfer
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (step! world0 0 "P1" {:type :admission :location "Renal"})
        ;; transfer-in-error: an erroneous transfer to ED, corrected atomically
        world2 (step! world1 10 "P1" {:type :transfer-in-error :location "ED"})
        after-error (get-in world2 [:patients "P1"])
        ;; a genuine, INTENDED transfer to ED follows -- the "retransfer"
        world3 (step! world2 20 "P1" {:type :transfer :location "ED"})
        after-retransfer (get-in world3 [:patients "P1"])]
    (testing "the in-error correction left the patient exactly where they started"
      (is (= "Renal" (:home-ward after-error)))
      (is (= "Renal" (get-in after-error [:location :ward]))))
    (testing "the retransfer that follows is a REAL, uncorrected move"
      (is (= "ED" (get-in after-retransfer [:location :ward]))))
    (testing "exact ground-truth event sequence: admission, transfer(err), cancel-transfer(err), transfer(real)"
      (is (= [:admission :transfer :cancel-transfer :transfer] (mapv :event (:ground-truth world3))))
      (is (= [nil true nil] (mapv :in-error (rest (:ground-truth world3))))))
    (testing "exact rendered message sequence: A01 A02 A12 A02"
      (is (= ["A01" "A02" "A12" "A02"]
             (triggers (:ground-truth world3) one-bed-one-surge-facility providers))))))

;; --- Scenario 3: bed-swap between a licensed and a surge occupant ---------

(deftest bed-swap-between-licensed-and-surge-occupant
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")})
        world1 (-> world0
                  (step! 0 "P1" {:type :admission :location "Renal"})   ;; licensed (rung 1)
                  (step! 5 "P2" {:type :admission :location "Renal"})) ;; surge (rung 2)
        p1-before (get-in world1 [:patients "P1"])
        p2-before (get-in world1 [:patients "P2"])
        _ (testing "sanity: one licensed, one surge, same ward"
            (is (= :licensed (get-in p1-before [:location :placement])))
            (is (= :surge (get-in p2-before [:location :placement]))))
        world2 (step! world1 10 "P1" {:type :bed-swap :with "P2"})
        p1-after (get-in world2 [:patients "P1"])
        p2-after (get-in world2 [:patients "P2"])]
    (testing "placements are exchanged, home-wards untouched, both still admitted"
      (is (= (:location p2-before) (:location p1-after)))
      (is (= (:location p1-before) (:location p2-after)))
      (is (= :surge (get-in p1-after [:location :placement])))
      (is (= :licensed (get-in p2-after [:location :placement])))
      (is (= :admitted (:status p1-after) (:status p2-after))))
    (testing "exact ground-truth event sequence"
      (is (= [:admission :admission :bed-swap] (mapv :event (:ground-truth world2)))))
    (testing "exact rendered message sequence: A01 A01 A17"
      (is (= ["A01" "A01" "A17"]
             (triggers (:ground-truth world2) one-bed-one-surge-facility providers))))))
