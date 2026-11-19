(ns ehrt.sim-engine.inpatient-dispense-derivation-test
  "ADR-0189: the `:dispense` engine key derives the INPATIENT dispense
  too. With the key, an order written during an admission is dispensed
  `:inpatient` after a pharmacy turnaround drawn from the range of the
  ward the patient is in when the order is written
  (`sim-model/dispense-minutes`: the ward's own `:dispense-minutes`, or
  its class's default).

  The dispense rides a queue entry at the order's instant plus the
  draw. When that entry is decided and the order is no longer active,
  or the admission that wrote it is no longer the open one, nothing is
  emitted: the order was stopped before pharmacy filled it.

  The draw is on the `:pharmacy` stream family, the patient's own, so
  no other draw of the run moves and ADR-0187's erasure law still
  covers the whole key
  (`ehrt.sim-engine.dispense-derivation-test`)."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-engine.decide :as decide]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.state :as state]
            [ehrt.sim-engine.streams :as streams]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

(def ^:private order-a {:module "stay-fills" :state :first-order})
(def ^:private a-concept {:system :rxnorm :code "834102" :display "Penicillin V Potassium 500 MG Oral Tablet"})

(defn- stay-config
  "Three patients, each admitted to `ward`, ordered for at once, and
  discharged after `tail` further steps."
  [ward tail]
  {:seed 11 :patients 3 :arrival-gap 240
   :pathways [{:weight 1
               :pathway {:name "stay-order"
                         :steps (into [{:type :admission :location ward}
                                       {:type :medication-order :codes [a-concept] :citation order-a}]
                                      tail)}}]})

(def ^:private long-stay [{:type :delay :from 180 :to 180} {:type :discharge}])

(defn- subject-of [event] (:patient-id (first (:participants event))))

(defn- indexed-of [kind ground-truth]
  (keep-indexed (fn [i e] (when (= kind (:event e)) [i e])) ground-truth))

(defn- ok? [r]
  (result/ok? (check/check-all (:ground-truth r) (:facility r))))

(defn- violations [r]
  (pr-str (:violations (:payload (check/check-all (:ground-truth r) (:facility r))))))

(defn- each-order-dispensed-once-inside-its-stay
  "Shape (a), over every order of a run in which every order is written
  during an admission: one `:inpatient` dispense per order, naming it by
  log index, later than it by `[lo hi]` minutes, and before that
  patient's discharge in the log."
  [ground-truth [lo hi]]
  (let [log (vec ground-truth)
        orders (indexed-of :medication-order log)
        dispenses (indexed-of :medication-dispense log)]
    (is (seq orders) "the run writes orders at all")
    (is (= (mapv first orders) (sort (map (comp :order-event-id second) dispenses)))
        "each order is named by exactly one dispense")
    (doseq [[di d] dispenses
            :let [oi (:order-event-id d)
                  order (nth log oi)
                  lag (- (:t d) (:t order))
                  closer (first (filter (fn [[i e]] (and (> i oi) (= :discharge (:event e))
                                                         (= (subject-of d) (subject-of e))))
                                        (map-indexed vector log)))]]
      (is (= :inpatient (:setting d)))
      (is (= (subject-of order) (subject-of d)))
      (is (= (:citation order) (:order-citation d)))
      (is (not (contains? d :citation)) "a derived dispense cites no module state and no step")
      (is (<= (* 60 lo) lag (* 60 hi)) (str "the lag, " lag " s, is inside the ward's range"))
      (is (some? closer) "the stay closes in the log")
      (when closer
        (is (< di (first closer)) "the dispense precedes the discharge in the log")))
    dispenses))

;; --- (a) ----------------------------------------------------------------

(deftest an-order-written-during-an-admission-is-dispensed-inpatient-inside-the-stay
  (testing "an inpatient ward, on its class default"
    (let [r (run/run (assoc (stay-config "Renal" long-stay) :dispense true))
          dispenses (each-order-dispensed-once-inside-its-stay
                     (:ground-truth r) (:inpatient sim-model/default-dispense-minutes))]
      (is (= 3 (count dispenses)))
      (is (ok? r) (violations r))))
  (testing "the ED, on its own class default"
    (let [r (run/run (assoc (stay-config "Emergency" long-stay) :dispense true))
          dispenses (each-order-dispensed-once-inside-its-stay
                     (:ground-truth r) (:ed sim-model/default-dispense-minutes))]
      (is (= 3 (count dispenses)))
      (is (ok? r) (violations r)))))

(deftest a-derived-inpatient-dispense-carries-the-stays-encounter-id
  (let [r (run/run (assoc (stay-config "Renal" long-stay) :dispense true :encounters true))
        log (vec (:ground-truth r))
        dispenses (each-order-dispensed-once-inside-its-stay
                   log (:inpatient sim-model/default-dispense-minutes))]
    (is (= 3 (count dispenses)))
    (doseq [[_ d] dispenses]
      (is (some? (:encounter-id d)))
      (is (= (:encounter-id (nth log (:order-event-id d))) (:encounter-id d))
          "the stay that wrote the order is the stay the dispense is stamped with"))
    (is (ok? r) (violations r))))

;; --- (b) ----------------------------------------------------------------

(defn- facility-with [ward-name range]
  (update sim-model/default-facility :wards
          (fn [wards] (mapv #(cond-> % (= ward-name (:name %)) (assoc :dispense-minutes range)) wards))))

(deftest a-wards-own-range-overrides-its-class-default
  (let [r (run/run (assoc (stay-config "Renal" long-stay)
                          :dispense true :facility (facility-with "Renal" [7 7])))
        dispenses (each-order-dispensed-once-inside-its-stay (:ground-truth r) [7 7])]
    (is (= 3 (count dispenses))))
  (testing "and the other wards keep their class default"
    (let [r (run/run (assoc (stay-config "Cardiology" long-stay)
                            :dispense true :facility (facility-with "Renal" [7 7])))]
      (is (= 3 (count (each-order-dispensed-once-inside-its-stay
                       (:ground-truth r) (:inpatient sim-model/default-dispense-minutes))))))))

;; --- (c) ----------------------------------------------------------------

(deftest an-order-ended-before-the-turnaround-elapses-is-not-dispensed
  (let [r (run/run (assoc (stay-config "Renal" (into [{:type :medication-end :order-citation order-a}] long-stay))
                          :dispense true))]
    (is (= 3 (count (indexed-of :medication-order (:ground-truth r)))))
    (is (= 3 (count (indexed-of :medication-end (:ground-truth r)))))
    (is (empty? (indexed-of :medication-dispense (:ground-truth r)))
        "the order and its end share an instant, so pharmacy never fills it")
    (is (ok? r) (violations r))))

(deftest a-discharge-before-the-turnaround-elapses-leaves-the-order-undispensed
  (let [r (run/run (assoc (stay-config "Renal" [{:type :delay :from 5 :to 5} {:type :discharge}])
                          :dispense true))]
    (is (= 3 (count (indexed-of :medication-order (:ground-truth r)))))
    (is (= 3 (count (indexed-of :discharge (:ground-truth r)))))
    (is (empty? (indexed-of :medication-dispense (:ground-truth r)))
        "five minutes is under the inpatient default's floor of fifteen")
    (is (ok? r) (violations r))))

;; --- (d) ----------------------------------------------------------------

(deftest without-the-key-an-admissions-order-is-not-dispensed
  (doseq [ward ["Renal" "Emergency"]]
    (is (empty? (indexed-of :medication-dispense
                            (:ground-truth (run/run (stay-config ward long-stay))))))))

(deftest the-turnaround-is-one-draw-on-the-pharmacy-stream-and-none-without-the-key
  (let [patient (assoc (state/initial-patient "P1" "MRN000001")
                       :status :admitted :class :inpatient :attending "prov-1"
                       :home-ward "Renal" :location {:ward "Renal" :bed "REN-01"}
                       :admitted-at 100 :encounter {:ordinal 0 :admitted-at 100})
        world {:patients {"P1" patient} :ground-truth [] :facility sim-model/default-facility}
        ordering {:type :medication-order :codes [a-concept] :citation order-a}
        streams (fn [others pharmacy] (assoc (streams/one-stream others) :pharmacy pharmacy))]
    (testing "with the key: the order event, and one queue entry later by the drawn lag"
      (let [others (Random. 1) pharmacy (Random. 2)
            decided (decide/decide (streams others pharmacy) 400 (assoc world :dispense true) "P1" ordering)
            [lo hi] (:inpatient sim-model/default-dispense-minutes)
            expected-lag (* 60 (streams/rand-int-in (Random. 2) lo hi))
            followup (:schedule-followup decided)]
        (is (= [:medication-order] (mapv :event (:events decided))))
        (is (= (+ 400 expected-lag) (:t followup)))
        (is (= "P1" (:patient-id followup)))
        (is (= 1 (count (:steps followup))))
        (is (= order-a (:order-citation (first (:steps followup)))))
        (is (= (.nextLong (Random. 1)) (.nextLong others)) "no other family's stream moved")
        (is (= (.nextLong (doto (Random. 2) (streams/rand-int-in lo hi))) (.nextLong pharmacy))
            "the pharmacy stream moved by exactly one draw")))
    (testing "without it: the decide's answer is the one it always gave, and nothing is drawn"
      (let [others (Random. 1) pharmacy (Random. 2)
            decided (decide/decide (streams others pharmacy) 400 world "P1" ordering)]
        (is (= #{:events :advance} (set (keys decided))))
        (is (= (.nextLong (Random. 1)) (.nextLong others)))
        (is (= (.nextLong (Random. 2)) (.nextLong pharmacy)))))
    (testing "with it, in an outpatient visit: nothing here -- the community half fills at the visit's close"
      (let [visiting (assoc-in world [:patients "P1" :class] :outpatient)
            decided (decide/decide (streams (Random. 1) (Random. 2)) 400 (assoc visiting :dispense true) "P1" ordering)]
        (is (= #{:events :advance} (set (keys decided))))))))

;; --- (e) ----------------------------------------------------------------

(deftest two-runs-at-one-revision-are-identical
  (let [cfg (assoc (stay-config "Renal" long-stay) :dispense true :encounters true :patients 12)
        a (:ground-truth (run/run cfg))
        b (:ground-truth (run/run cfg))]
    (is (= 12 (count (indexed-of :medication-dispense a))) "the comparison is of runs that dispense")
    (is (= a b))
    (is (= (pr-str a) (pr-str b)))))
