(ns ehrt.sim-engine.dispense-derivation-test
  "ADR-0187: the `:dispense` engine key. With it, every medication order
  an outpatient visit wrote and left open gets ONE `:community`
  `:medication-dispense`, after the visit's own closing event. Without
  it, nothing here happens at all.

  The fill is not in the closer's own batch. `fold/apply-events` stamps
  a batch off the world as it stood BEFORE the batch, so an event beside
  the closer would carry the id of the visit that closer closes. It
  rides a queue entry at the closer's own instant instead, the way an
  order's result rides one (`decide :order`), and is decided once the
  close has been folded."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [clojure.walk :as walk]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-engine.config :as config]
            [ehrt.sim-engine.decide :as decide]
            [ehrt.sim-engine.event-schema :as event-schema]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.state :as state]
            [ehrt.sim-engine.streams :as streams]
            [ehrt.sim-model.interface :as sim-model]
            [malli.core :as m])
  (:import [java.util Random]))

;; --- the runs ---------------------------------------------------------

(defn- module-config
  "A module-only run, the shape the oracle's module roots use."
  [module-id file overrides]
  (let [module (:payload (patient-simulator/load-module
                          module-id (slurp (io/resource (str "sim/modules/" file ".json")))))]
    (merge {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
            :modules [(patient-simulator/singleton-closure module)]
            :module-assignment [{:module-id module-id :weight 1}]
            :module-horizon-days 36500}
           overrides)))

(def ^:private osteoporosis-config
  "The oracle's `osteoporosis` root, verbatim
  (`ehrt.oracle.digest/osteoporosis-pair`): 156 visits, each writing one
  order."
  (module-config "osteoporosis" "osteoporosis" {}))

(def ^:private sinusitis-config
  "The oracle's `sinusitis` root, verbatim: 22 visits, 7 of them writing
  an order."
  (module-config "sinusitis" "sinusitis" {:seed 1 :patients 30 :module-horizon-days 3650}))

(def ^:private order-a {:module "fills" :state :first-prescription})
(def ^:private order-b {:module "fills" :state :second-prescription})
(def ^:private a-concept {:system :rxnorm :code "834102" :display "Penicillin V Potassium 500 MG Oral Tablet"})

(def ^:private two-order-visit-config
  {:seed 11 :patients 3 :arrival-gap 90
   :pathways [{:weight 1
               :pathway {:name "two-orders"
                         :steps [{:type :outpatient-visit}
                                 {:type :medication-order :codes [a-concept] :citation order-a}
                                 {:type :delay :from 5 :to 5}
                                 {:type :medication-order :codes [a-concept] :citation order-b}
                                 {:type :delay :from 20 :to 20}
                                 {:type :outpatient-visit-end}]}}]})

(def ^:private admission-order-config
  {:seed 11 :patients 3 :arrival-gap 90
   :pathways [{:weight 1
               :pathway {:name "stay-order"
                         :steps [{:type :admission :location "Renal"}
                                 {:type :medication-order :codes [a-concept] :citation order-a}
                                 {:type :delay :from 60 :to 60}
                                 {:type :discharge}]}}]})

(def ^:private fill-everything-at-the-close
  "ADR-0190 made the community fill a draw: a share that is filled, and
  a lag. A facility that fills every prescription with no lag is
  ADR-0187's rule exactly, and the tests of that rule below run on it.
  The draw itself is `ehrt.sim-engine.community-fill-draw-test`'s."
  (assoc sim-model/default-facility :community-fill-share 1 :community-fill-minutes [0 0]))

(defn- keyed
  "`config` with the `:dispense` key, on a facility that fills every
  prescription at its visit's close."
  [config]
  (assoc config :dispense true :facility fill-everything-at-the-close))

(defn- subject-of [event] (:patient-id (first (:participants event))))

(defn- indexed-of [kind ground-truth]
  (keep-indexed (fn [i e] (when (= kind (:event e)) [i e])) ground-truth))

(defn- ok? [r]
  (result/ok? (check/check-all (:ground-truth r) (:facility r))))

(defn- violations [r]
  (pr-str (:violations (:payload (check/check-all (:ground-truth r) (:facility r))))))

;; --- step 1: what the key does -----------------------------------------

(deftest the-key-is-an-engine-config-key
  (is (contains? (set config/config-keys) :dispense)))

(defn- every-order-is-filled-once-after-its-visit-closed
  "The shape (a) asks for, over every order of a run: the order, later
  that patient's visit end, later still one community dispense naming
  the order by log index, at the end's instant or after it."
  [ground-truth]
  (let [log (vec ground-truth)
        orders (indexed-of :medication-order log)
        dispenses (indexed-of :medication-dispense log)]
    (is (seq orders) "the run writes orders at all")
    (is (= (count orders) (count dispenses)) "one fill per order")
    (is (= (mapv first orders) (sort (map (comp :order-event-id second) dispenses)))
        "and each order is named by exactly one of them")
    (doseq [[di d] dispenses
            :let [oi (:order-event-id d)
                  order (nth log oi)
                  closer (first (filter (fn [[i e]] (and (> i oi)
                                                         (= :outpatient-visit-end (:event e))
                                                         (= (subject-of d) (subject-of e))))
                                        (map-indexed vector log)))]]
      (is (= :community (:setting d)))
      (is (= (subject-of order) (subject-of d)))
      (is (= (:citation order) (:order-citation d)))
      (is (some? closer) "the visit that wrote the order closes in the log")
      (when closer
        (is (< (first closer) di) "the fill follows the visit's closing event in the log")
        (is (<= (:t (second closer)) (:t d)) "at that event's instant or later")))
    dispenses))

(deftest a-module-compiled-visits-order-is-filled-in-the-community-after-the-visit-closes
  (let [r (run/run (keyed osteoporosis-config))
        dispenses (every-order-is-filled-once-after-its-visit-closed (:ground-truth r))]
    (is (= 156 (count dispenses)))
    (is (not-any? #(contains? (second %) :citation) dispenses)
        "a derived fill is no module state's and no step's: it cites nothing")
    (is (ok? r) (violations r))))

(deftest a-derived-fill-does-not-carry-the-closed-visits-encounter-id
  (let [r (run/run (keyed (assoc osteoporosis-config :encounters true)))
        log (vec (:ground-truth r))
        dispenses (every-order-is-filled-once-after-its-visit-closed log)]
    (is (= 156 (count dispenses)))
    (is (every? #(contains? (nth log (:order-event-id (second %))) :encounter-id) dispenses)
        "the orders are stamped with their visit, so the question is a real one")
    (is (not-any? #(contains? (second %) :encounter-id) dispenses)
        "the fill is after the visit, so it is stamped with no encounter")
    (is (ok? r) (violations r))))

(deftest two-orders-in-one-visit-are-filled-in-the-orders-own-order
  (let [r (run/run (keyed two-order-visit-config))
        log (vec (:ground-truth r))
        dispenses (every-order-is-filled-once-after-its-visit-closed log)]
    (is (= 6 (count dispenses)))
    (doseq [[_ per-patient] (group-by (comp subject-of second) dispenses)]
      (is (= [order-a order-b] (mapv (comp :order-citation second) per-patient)))
      (is (apply < (map (comp :order-event-id second) per-patient))))
    (is (ok? r) (violations r))))

(deftest an-order-written-during-an-admission-gets-no-community-fill
  ;; ADR-0189: it gets an `:inpatient` dispense instead, which
  ;; `ehrt.sim-engine.inpatient-dispense-derivation-test` covers.
  (let [r (run/run (assoc admission-order-config :dispense true))
        dispenses (map second (indexed-of :medication-dispense (:ground-truth r)))]
    (is (= 3 (count (indexed-of :medication-order (:ground-truth r)))))
    (is (= 3 (count dispenses)))
    (is (= #{:inpatient} (set (map :setting dispenses))))
    (is (ok? r) (violations r))))

(deftest without-the-key-nothing-is-derived
  (doseq [cfg [osteoporosis-config sinusitis-config two-order-visit-config admission-order-config]]
    (is (empty? (indexed-of :medication-dispense (:ground-truth (run/run cfg)))))))

(deftest an-order-ended-inside-its-visit-gets-no-derived-fill
  (testing "the dispense law bounds a fill by its order's end, so an order
            the visit itself ended is not one the visit left to fill"
    (let [cfg {:seed 11 :patients 2 :arrival-gap 90 :dispense true
               :facility fill-everything-at-the-close
               :pathways [{:weight 1
                           :pathway {:name "ended-in-visit"
                                     :steps [{:type :outpatient-visit}
                                             {:type :medication-order :codes [a-concept] :citation order-a}
                                             {:type :medication-order :codes [a-concept] :citation order-b}
                                             {:type :delay :from 10 :to 10}
                                             {:type :medication-end :order-citation order-a}
                                             {:type :delay :from 10 :to 10}
                                             {:type :outpatient-visit-end}]}}]}
          r (run/run cfg)
          dispenses (indexed-of :medication-dispense (:ground-truth r))]
      (is (= [order-b order-b] (mapv (comp :order-citation second) dispenses)))
      (is (ok? r) (violations r)))))

(deftest deriving-a-fill-takes-two-draws-per-order-on-the-pharmacy-stream
  (let [patient (assoc (state/initial-patient "P1" "MRN000001")
                       :status :admitted :class :outpatient :attending "prov-1"
                       :encounter {:ordinal 0 :admitted-at 100}
                       :medication-orders [{:citation order-b :ordered-t 40 :status :active}
                                           {:citation order-a :ordered-t 110 :status :active}])
        world {:patients {"P1" patient} :ground-truth []
               :facility (assoc sim-model/default-facility :community-fill-share 1)}
        closing {:type :outpatient-visit-end}
        streams (fn [others pharmacy] (assoc (streams/one-stream others) :pharmacy pharmacy))]
    (testing "with the key: the closing event alone, and one queue entry per
              order this visit wrote, at the close plus that order's drawn lag"
      (let [others (Random. 1) pharmacy (Random. 2)
            decided (decide/decide (streams others pharmacy) 130 (assoc world :dispense true) "P1" closing)
            [lo hi] sim-model/default-community-fill-minutes
            twin (Random. 2)
            _ (.nextDouble twin)
            lag (* 60 (streams/rand-int-in twin lo hi))]
        (is (= [:outpatient-visit-end] (mapv :event (:events decided))))
        (is (= [{:t (+ 130 lag) :patient-id "P1"
                 :steps [{:type :community-fill :order-citation order-a :ordered-t 110}]}]
               (:schedule-followup decided))
            "the order from before this visit opened is not this visit's")
        (is (= (.nextLong (Random. 1)) (.nextLong others)) "no other family's stream moved")
        (is (= (.nextLong twin) (.nextLong pharmacy))
            "the pharmacy stream moved by exactly two draws: filled at all, then the lag")))
    (testing "a prescription the draw leaves unfilled queues nothing, and takes the same two draws"
      (let [pharmacy (Random. 2)
            unfilled (assoc-in world [:facility :community-fill-share] 0)
            decided (decide/decide (streams (Random. 1) pharmacy) 130 (assoc unfilled :dispense true) "P1" closing)
            twin (doto (Random. 2) (.nextDouble) (streams/rand-int-in 60 10080))]
        (is (= #{:events :advance} (set (keys decided))))
        (is (= (.nextLong twin) (.nextLong pharmacy)))))
    (testing "without it: the decide's answer is the one it always gave"
      (let [decided (decide/decide (streams/one-stream (Random. 1)) 130 world "P1" closing)]
        (is (= #{:events :advance} (set (keys decided))))))
    (testing "with it and nothing to fill: no queue entry either"
      (let [bare (assoc-in world [:patients "P1" :medication-orders] [])
            decided (decide/decide (streams/one-stream (Random. 1)) 130 (assoc bare :dispense true) "P1" closing)]
        (is (= #{:events :advance} (set (keys decided))))))))

;; --- step 3: THE ERASURE LAW, the key's contract ------------------------
;;
;; The key adds events and draws only on the `:pharmacy` stream, which
;; nothing else reads (ADR-0189, ADR-0190). So a run with it is the keyless
;; run with dispenses inserted: delete every `:medication-dispense`,
;; renumber each log-index-bearing field past the deletions, and what is
;; left is the keyless log, EVENT FOR EVENT -- the whole log compared for
;; equality, not its counts.
;;
;; None of these runs authors a dispense, so deleting the kind deletes
;; exactly what the key derived.

(defn- schema-reference-fields
  "Every `*-event-id` field the LIVE event schema declares as an int or a
  nilable int -- a log index -- walked out of `event-schema/Event`. The
  same walk, on the same schema, as
  `ehrt.corpus.event-mutate-test/schema-reference-fields`, which is the
  tree's own enumeration (and a private test fn in a brick this one
  cannot require). A reference field added to the schema is renumbered
  here without this test being touched."
  []
  (let [acc (atom #{})]
    (walk/postwalk
     (fn [form]
       (when (and (vector? form) (<= 2 (count form) 3) (keyword? (first form)))
         (let [k (first form) t (last form)]
           (when (and (re-find #"-event-id$" (name k))
                      (contains? #{:int [:maybe :int]} t))
             (swap! acc conj k))))
       form)
     (m/form event-schema/Event))
    @acc))

(defn- erase-dispenses
  "The log with every `:medication-dispense` deleted and every reference
  field renumbered to the index its target has once they are gone."
  [ground-truth]
  (let [log (vec ground-truth)
        fields (schema-reference-fields)
        ;; old index -> new index, for the events that survive.
        new-index (loop [i 0 kept 0 acc (transient {})]
                    (if (= i (count log))
                      (persistent! acc)
                      (if (= :medication-dispense (:event (nth log i)))
                        (recur (inc i) kept acc)
                        (recur (inc i) (inc kept) (assoc! acc i kept)))))
        renumber (fn [event]
                   (walk/postwalk
                    (fn [form]
                      (if (and (map-entry? form) (contains? fields (key form)) (int? (val form)))
                        (clojure.lang.MapEntry. (key form) (get new-index (val form) ::dangling))
                        form))
                    event))]
    (into [] (comp (remove #(= :medication-dispense (:event %))) (map renumber)) log)))

(def ^:private churned-visits-config
  "Authored outpatient visits under churn and every opt-in the engine
  has, so the erased log holds reference fields other than a
  dispense's (`:cancels-event-id`) and events at the fills' own
  instants."
  {:seed 20261118 :patients 40 :arrival-gap 45 :encounters true :bed-cycle true
   :churn-profile {:cancel-admit 0.2 :cancel-transfer 0.2 :cancel-discharge 0.2
                   :transfer-in-error 0.1 :bed-swap 0.1 :merge 0.1}
   :pathways [{:weight 1 :pathway (get-in two-order-visit-config [:pathways 0 :pathway])}
              {:weight 1
               :pathway {:name "stay"
                         :steps [{:type :admission :location "Renal"}
                                 {:type :medication-order :codes [a-concept] :citation order-a}
                                 {:type :delay :from 30 :to 90}
                                 {:type :transfer :location "Cardiology"}
                                 {:type :delay :from 30 :to 90}
                                 {:type :discharge}]}}]})

(deftest erasing-the-derived-dispenses-leaves-the-keyless-run-event-for-event
  (testing "the walk finds the schema's reference fields at all"
    (is (contains? (schema-reference-fields) :order-event-id))
    (is (contains? (schema-reference-fields) :cancels-event-id)))
  (doseq [[label cfg expected-fills]
          ;; The fills are counted at the DEFAULT share and lag
          ;; (ADR-0190), so the law is held under real draws: 156, 7,
          ;; 156 and 6 orders are written, in that order.
          [["the oracle's osteoporosis root" osteoporosis-config 134]
           ["the oracle's sinusitis root" sinusitis-config 7]
           ["osteoporosis, with :encounters" (assoc osteoporosis-config :encounters true) 134]
           ["two authored orders per visit" two-order-visit-config 3]
           ["authored visits and stays under churn" churned-visits-config nil]]]
    (testing label
      (let [keyless (:ground-truth (run/run cfg))
            keyed (:ground-truth (run/run (assoc cfg :dispense true)))
            fills (count (filter #(= :medication-dispense (:event %)) keyed))
            erased (erase-dispenses keyed)]
        (is (pos? fills) "the key derived something, so the erasure is not of nothing")
        (when expected-fills (is (= expected-fills fills)))
        (is (= (+ (count keyless) fills) (count keyed)))
        (is (not-any? #(some #{::dangling} (tree-seq coll? seq %)) erased)
            "no surviving event referenced a deleted one")
        (is (= (count keyless) (count erased)))
        (when-let [[i a b] (first (keep-indexed (fn [i [a b]] (when (not= a b) [i a b]))
                                                 (map vector keyless erased)))]
          (is false (str "first differing event, index " i ":\n keyless " (pr-str a)
                         "\n erased  " (pr-str b))))
        (is (= keyless erased) "the whole log, event for event")))))
