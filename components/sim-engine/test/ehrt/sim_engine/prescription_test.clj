(ns ehrt.sim-engine.prescription-test
  "ADR-0192 (event schema 1.14.0): a `:medication-order` event carries
  its `:prescription` -- dose, duration, refills, as-needed,
  instructions -- in the vendored modules' own layout, exactly as the
  module or the pathway author wrote it, and absent when they wrote
  none. NOTHING READS IT: no dispense, no fill count and no draw differs
  between a run whose orders state a prescription and one whose orders
  do not."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-engine.event-schema :as es]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-model.interface :as sim-model]))

(def ^:private rx-citation {:module "clinic" :state :long-term})
(def ^:private a-concept {:system :rxnorm :code "860975"
                          :display "24 HR Metformin hydrochloride 500 MG Extended Release Oral Tablet"})
(def ^:private a-prescription
  {:dosage {:amount 1 :frequency 1 :period 1 :unit "days"} :duration {:quantity 30 :unit "days"} :refills 5})

(defn- of-kind [kind log] (filter #(= kind (:event %)) log))
(defn- ok? [r] (result/ok? (check/check-all (:ground-truth r) (:facility r))))

(defn- resolve-call-path [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

;; --- (a) a vendored module's block reaches the event ----------------------

(deftest a-vendored-modules-prescription-reaches-the-order-event-as-authored
  (let [closure (:payload (patient-simulator/load-closure
                           "ear-infections" (slurp (io/resource "sim/modules/ear_infections.json")) resolve-call-path))
        ;; The oracle's `ear-infections-engine` root, verbatim.
        r (run/run {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                    :modules [closure] :module-assignment [{:module-id "ear-infections" :weight 1}]
                    :module-horizon-days 3650})
        log (:ground-truth r)
        orders (of-kind :medication-order log)
        by-state (group-by (comp :state :citation) orders)]
    (is (= 29 (count orders)))
    (is (every? :prescription orders) "every order this root writes cites a state with a block")
    (testing "dosage and duration, with the values the module file states"
      (let [amox (:prescription (first (:adult-amoxicillin by-state)))]
        (is (= {:amount 1 :frequency 3 :period 1 :unit "days"} (:dosage amox)))
        (is (= {:quantity 2 :unit "weeks"} (:duration amox)))
        (is (= [:snomed] (map :system (:instructions amox))))))
    (testing "a block that states only as_needed carries only that"
      (let [otc (filter #(= "medications/otc_pain_reliever" (:module (:citation %))) orders)]
        (is (seq otc))
        (is (every? #(= {:as-needed true} (:prescription %)) otc))))
    (is (every? es/valid-event? log))
    (is (ok? r))))

(deftest an-order-of-a-state-with-no-block-carries-no-prescription-key
  (let [module (:payload (patient-simulator/load-module
                          "osteoporosis" (slurp (io/resource "sim/modules/osteoporosis.json"))))
        r (run/run {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                    :modules [(patient-simulator/singleton-closure module)]
                    :module-assignment [{:module-id "osteoporosis" :weight 1}]
                    :module-horizon-days 36500})
        orders (of-kind :medication-order (:ground-truth r))]
    (is (= 156 (count orders)))
    (is (not-any? #(contains? % :prescription) orders) "absent, never nil and never an empty map")))

;; --- (b) an authored order's block reaches the event ----------------------

(defn- visit-config [order]
  {:seed 7 :patients 40 :arrival-gap 30 :dispense true
   :pathways [{:weight 1
               :pathway {:name "clinic"
                         :steps [{:type :outpatient-visit}
                                 order
                                 {:type :delay :from 20 :to 20}
                                 {:type :outpatient-visit-end}]}}]})

(def ^:private bare-order {:type :medication-order :codes [a-concept] :citation rx-citation :chronic true})

(deftest an-authored-orders-prescription-reaches-the-event
  (let [r (run/run (visit-config (assoc bare-order :prescription a-prescription)))
        orders (of-kind :medication-order (:ground-truth r))]
    (is (= 40 (count orders)))
    (is (every? #(= a-prescription (:prescription %)) orders))
    (is (every? es/valid-event? (:ground-truth r)))
    (is (ok? r))))

;; --- (c) the event schema refuses a malformed block -----------------------

(deftest the-event-schema-refuses-a-malformed-prescription
  (let [order (first (of-kind :medication-order (:ground-truth (run/run (visit-config bare-order)))))
        valid? (fn [rx] (es/valid-event? (assoc order :prescription rx)))]
    (is (es/valid-event? order) "the key is optional")
    (is (valid? a-prescription))
    (is (valid? {:as-needed false}))
    (is (not (valid? {:dosage {:amount 1 :frequency 1 :period 1}})) "a dosage with no unit")
    (is (not (valid? {:dosage {:amount 1 :frequency 0 :period 1 :unit "days"}})))
    (is (not (valid? {:refills 5 :days-supply 30})) "an unknown key")
    (is (not (valid? {})))
    (is (not (valid? nil)))))

;; --- (e) nothing reads it ---------------------------------------------------

(deftest stating-a-prescription-moves-no-dispense-no-fill-count-and-no-draw
  (let [without (:ground-truth (run/run (visit-config bare-order)))
        with (:ground-truth (run/run (visit-config (assoc bare-order :prescription a-prescription))))
        fills (of-kind :medication-dispense with)]
    (is (< 40 (count fills)) "a chronic order under the key: first fills and refills, so the comparison is of something")
    (is (some #(< 1 (:fill-number %)) fills))
    (is (= (count without) (count with)))
    (is (= without (mapv #(dissoc % :prescription) with))
        "erase the key and the two logs are one: every dispense, every fill number, every instant")
    (is (= 40 (count (filter :prescription with))) "and only the orders carry it")
    (is (some #(< 6 (:fill-number %)) fills) "a chain runs past the 5 refills the order states")))
