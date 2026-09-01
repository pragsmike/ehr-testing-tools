(ns ehrt.sim-model.facility-test
  "The occupancy board (a derived projection, never written directly --
  sim/ADR-0008/components/sim/docs/operational-models.md) and the four-rung allocation
  ladder. Written before ehrt.sim-model.facility exists (sim/ADR-0004
  test-first)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-model.config :as config]
            [ehrt.sim-model.facility :as facility])
  (:import [java.util Random]))

(def renal-ward
  {:id :renal :name "Renal" :beds 3 :surge-slots 2
   :surge-format "%s-H%02d" :class :inpatient})

(deftest bed-ids-are-derived-never-enumerated
  (testing "licensed bed ids: ward id uppercased, 2-digit index"
    (is (= ["RENAL-01" "RENAL-02" "RENAL-03"] (facility/licensed-bed-ids renal-ward))))
  (testing "surge slot ids: the ward's own :surge-format"
    (is (= ["RENAL-H01" "RENAL-H02"] (facility/surge-slot-ids renal-ward)))))

(deftest occupancy-board-is-a-fold-over-patient-locations
  (testing "the board is exactly bed -> mrn for every patient with a location"
    (let [patients {"MRN000001" {:mrn "MRN000001" :status :admitted
                                  :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
                     "MRN000002" {:mrn "MRN000002" :status :new}}]
      (is (= {"RENAL-01" "MRN000001"} (facility/occupancy-board patients))))))

(deftest allocation-ladder-rung-1-home-licensed
  (let [facility {:id :t :wards [renal-ward]}
        rng (Random. 1)
        alloc (facility/allocate rng facility {} "Renal" nil)]
    (is (= "Renal" (:home-ward alloc)))
    (is (= :licensed (get-in alloc [:location :placement])))
    (is (= "Renal" (get-in alloc [:location :ward])))
    (is (contains? (set (facility/licensed-bed-ids renal-ward)) (get-in alloc [:location :bed])))
    (is (false? (:forced alloc)))))

(deftest allocation-ladder-rung-2-home-surge-once-licensed-full
  (let [facility {:id :t :wards [renal-ward]}
        board (into {} (map vector (facility/licensed-bed-ids renal-ward) (repeat "someone")))
        alloc (facility/allocate (Random. 1) facility board "Renal" nil)]
    (is (= :surge (get-in alloc [:location :placement])))
    (is (= "Renal" (get-in alloc [:location :ward])))
    (is (contains? (set (facility/surge-slot-ids renal-ward)) (get-in alloc [:location :bed])))))

(deftest allocation-ladder-rung-3-other-ward-outlier
  (let [cardiology (assoc renal-ward :id :cardiology :name "Cardiology")
        facility {:id :t :wards [renal-ward cardiology]}
        board (merge (into {} (map vector (facility/licensed-bed-ids renal-ward) (repeat "x")))
                     (into {} (map vector (facility/surge-slot-ids renal-ward) (repeat "x"))))
        alloc (facility/allocate (Random. 1) facility board "Renal" nil)]
    (testing "outlier: home-ward differs from location.ward, a real (inpatient-class) other ward"
      (is (= "Renal" (:home-ward alloc)))
      (is (= "Cardiology" (get-in alloc [:location :ward])))
      (is (= :licensed (get-in alloc [:location :placement]))))))

(deftest allocation-ladder-rung-4-boarding-in-ed-surge
  (let [cardiology (assoc renal-ward :id :cardiology :name "Cardiology")
        ed {:id :ed :name "ED" :beds 0 :surge-slots 2 :surge-format "%s-H%02d" :class :ed}
        facility {:id :t :wards [renal-ward cardiology ed]}
        full (fn [ward] (into {} (map vector (concat (facility/licensed-bed-ids ward)
                                                       (facility/surge-slot-ids ward))
                                       (repeat "x"))))
        board (merge (full renal-ward) (full cardiology))
        alloc (facility/allocate (Random. 1) facility board "Renal" nil)]
    (testing "boarding: home-ward differs from location.ward, which is ED-class"
      (is (= "Renal" (:home-ward alloc)))
      (is (= "ED" (get-in alloc [:location :ward])))
      (is (= :surge (get-in alloc [:location :placement])))
      (is (contains? (set (facility/surge-slot-ids ed)) (get-in alloc [:location :bed]))))))

(deftest allocation-ladder-force-placement-overrides-and-is-exempt
  (let [facility {:id :t :wards [renal-ward]}
        board (into {} (map vector (facility/licensed-bed-ids renal-ward) (repeat "x"))) ;; licensed full
        alloc (facility/allocate (Random. 1) facility board "Renal" {:ward "Renal" :bed "RENAL-H02"})]
    (is (true? (:forced alloc)))
    (is (= "RENAL-H02" (get-in alloc [:location :bed])))
    (is (= :surge (get-in alloc [:location :placement])))))

(deftest allocate-returns-structured-exhaustion-not-throw
  (testing "every rung exhausted (licensed AND surge full, no ED/other
            ward at all) -- result-not-throw, not an exception
            (docs/clinical-realities.md's diversion stub)"
    (let [facility {:id :t :wards [renal-ward]}
          board (into {} (map vector (concat (facility/licensed-bed-ids renal-ward)
                                              (facility/surge-slot-ids renal-ward))
                                     (repeat "x")))
          alloc (facility/allocate (Random. 1) facility board "Renal" nil)]
      (is (true? (:exhausted alloc)))
      (is (= "Renal" (:home-ward alloc))))))

(deftest ward-census-reports-occupied-and-capacity-per-ward
  (let [facility {:id :t :wards [renal-ward]}
        board (into {} (map vector (facility/licensed-bed-ids renal-ward) (repeat "x")))]
    (is (= {"Renal" {:occupied 3 :capacity 5}} (facility/ward-census facility board)))))

(defspec allocate-never-returns-an-occupied-bed 100
  (prop/for-all [seed gen/large-integer]
    (let [facility {:id :t :wards [renal-ward
                                    {:id :ed :name "ED" :beds 0 :surge-slots 2
                                     :surge-format "%s-H%02d" :class :ed}]}
          board (into {} (map vector (facility/licensed-bed-ids renal-ward) (repeat "x")))
          alloc (facility/allocate (Random. ^long seed) facility board "Renal" nil)]
      (not (contains? board (get-in alloc [:location :bed]))))))

(deftest choose-attending-samples-only-ward-eligible-providers
  (let [providers (config/materialize-providers (Random. 3) config/default-provider-templates)
        attending-id (facility/choose-attending (Random. 5) providers :renal)
        attending (first (filter #(= (:id %) attending-id) providers))]
    (is (some #{:renal} (:wards attending)))))

(defspec choose-attending-is-deterministic 100
  (prop/for-all [seed gen/large-integer]
    (let [providers (config/materialize-providers (Random. 1) config/default-provider-templates)]
      (= (facility/choose-attending (Random. ^long seed) providers :renal)
         (facility/choose-attending (Random. ^long seed) providers :renal)))))
