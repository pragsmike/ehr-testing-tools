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

(deftest a-discharge-before-the-turnaround-elapses-leaves-the-order-undispensed-inpatient
  ;; ADR-0190: such an order is a discharge prescription, so what the
  ;; community pharmacy does with it is the facility's two options'.
  ;; With the share at 0 the log is what it was under ADR-0189.
  (let [config (assoc (stay-config "Renal" [{:type :delay :from 5 :to 5} {:type :discharge}])
                      :dispense true)
        with-share (fn [share] (run/run (assoc config :facility (assoc sim-model/default-facility
                                                                       :community-fill-share share))))]
    (testing "never dispensed :inpatient: five minutes is under the inpatient default's floor of fifteen"
      (doseq [share [0 1]
              :let [r (with-share share)]]
        (is (= 3 (count (indexed-of :medication-order (:ground-truth r)))))
        (is (= 3 (count (indexed-of :discharge (:ground-truth r)))))
        (is (not-any? #(= :inpatient (:setting (second %)))
                      (indexed-of :medication-dispense (:ground-truth r))))
        (is (ok? r) (violations r))))
    (testing "and with no community fill, not dispensed at all"
      (is (empty? (indexed-of :medication-dispense (:ground-truth (with-share 0))))))
    (testing "with one, filled in the community (ehrt.sim-engine.community-fill-draw-test)"
      (is (= [:community :community :community]
             (mapv (comp :setting second) (indexed-of :medication-dispense (:ground-truth (with-share 1)))))))))

;; --- (d) ----------------------------------------------------------------

(deftest without-the-key-an-admissions-order-is-not-dispensed
  (doseq [ward ["Renal" "Emergency"]]
    (is (empty? (indexed-of :medication-dispense
                            (:ground-truth (run/run (stay-config ward long-stay))))))))

(deftest an-order-takes-three-draws-on-the-pharmacy-stream-and-none-without-the-key
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
        ;; ADR-0190: the turnaround, then the community fill's two --
        ;; whether a discharge prescription would be filled, and its lag.
        (let [[clo chi] sim-model/default-community-fill-minutes
              twin (Random. 2)
              _ (streams/rand-int-in twin lo hi)
              u (.nextDouble twin)
              lag (* 60 (streams/rand-int-in twin clo chi))]
          (is (= (when (< u sim-model/default-community-fill-share) lag)
                 (:community-lag-seconds (first (:steps followup))))
              "the step carries the lag, or nothing for a prescription never filled")
          (is (= (.nextLong twin) (.nextLong pharmacy))
              "the pharmacy stream moved by exactly three draws"))))
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

;; --- the oracle root's config ---------------------------------------------

;; The SAME config as the oracle's `dispense-inpatient` root
;; (`ehrt.oracle.digest/dispense-inpatient-pair`), kept in step by hand,
;; since sim-engine cannot depend on oracle. Twelve stays, four shapes by
;; ordinal, every delay fixed so the only draws that vary are pharmacy's.

(def ^:private inpatient-producer-config
  (let [penicillin {:system :rxnorm :code "834102" :display "Penicillin V Potassium 500 MG Oral Tablet"}
        amoxicillin {:system :rxnorm :code "562251"
                     :display "Amoxicillin 250 MG / Clavulanate 125 MG Oral Tablet"}
        order-in (fn [pathway state] {:module pathway :state state})
        ;; ordered in the ED, on the class default [5 25]
        ed-stay {:name "ordered-in-ed"
                 :steps [{:type :admission :location "Emergency"}
                         {:type :medication-order :codes [penicillin] :citation (order-in "ordered-in-ed" :antibiotic)}
                         {:type :delay :from 90 :to 90}
                         {:type :medication-end :order-citation (order-in "ordered-in-ed" :antibiotic)}
                         {:type :discharge}]}
        ;; an inpatient ward on the class default [15 60]
        renal-stay {:name "ordered-on-renal"
                    :steps [{:type :admission :location "Renal"}
                            {:type :medication-order :codes [penicillin] :citation (order-in "ordered-on-renal" :antibiotic)}
                            {:type :delay :from 90 :to 90}
                            {:type :medication-end :order-citation (order-in "ordered-on-renal" :antibiotic)}
                            {:type :discharge}]}
        ;; an inpatient ward stating its own range, [40 50]
        cardiology-stay {:name "ordered-on-cardiology"
                         :steps [{:type :admission :location "Cardiology"}
                                 {:type :medication-order :codes [amoxicillin]
                                  :citation (order-in "ordered-on-cardiology" :antibiotic)}
                                 {:type :delay :from 90 :to 90}
                                 {:type :medication-end :order-citation (order-in "ordered-on-cardiology" :antibiotic)}
                                 {:type :discharge}]}
        ;; two orders pharmacy never reaches: one ended at its own
        ;; instant, one whose stay closes 5 minutes on, under the floor
        ;; of 15. ADR-0190: the second is a discharge prescription, and
        ;; this facility fills every one, two to four hours after the
        ;; discharge.
        short-stay {:name "never-dispensed"
                    :steps [{:type :admission :location "Renal"}
                            {:type :medication-order :codes [penicillin] :citation (order-in "never-dispensed" :stopped)}
                            {:type :medication-end :order-citation (order-in "never-dispensed" :stopped)}
                            {:type :medication-order :codes [amoxicillin] :citation (order-in "never-dispensed" :outrun)}
                            {:type :delay :from 5 :to 5}
                            {:type :discharge}]}
        shapes [ed-stay renal-stay cardiology-stay short-stay]]
    {:seed 20261119 :patients 12 :arrival-gap 90
     :dispense true :encounters true
     :facility (assoc (facility-with "Cardiology" [40 50])
                      :community-fill-share 1 :community-fill-minutes [120 240])
     :pathways (mapv (fn [i] {:patient-ordinal i :pathway (nth shapes (mod i 4))}) (range 12))}))

(deftest the-inpatient-root-holds-each-case-by-name
  (let [{:keys [ground-truth] :as r} (run/run inpatient-producer-config)
        log (vec ground-truth)
        orders (indexed-of :medication-order log)
        dispenses (mapv second (indexed-of :medication-dispense log))
        order-of (fn [d] (nth log (:order-event-id d)))
        lag-minutes (fn [d] (/ (- (:t d) (:t (order-of d))) 60))
        inpatient (filterv #(= :inpatient (:setting %)) dispenses)
        community (filterv #(= :community (:setting %)) dispenses)
        by-pathway (group-by #(:module (:citation (order-of %))) inpatient)
        dispensed (set (map :order-event-id dispenses))
        undispensed (remove (comp dispensed first) orders)
        discharge-of (fn [d] (first (filter #(and (= :discharge (:event %))
                                                  (= (subject-of d) (subject-of %)))
                                            log)))]
    (is (= {:registered 12 :admission 12 :discharge 12
            :medication-order 15 :medication-end 12 :medication-dispense 12}
           (frequencies (map :event log))))
    (is (= 9 (count inpatient)))
    (is (= {"ordered-in-ed" 3 "ordered-on-renal" 3 "ordered-on-cardiology" 3}
           (update-vals by-pathway count))
        "three orders dispensed in each of the three wards")
    (testing "each inside the range of the ward the order was written on"
      (is (every? #(<= 5 (lag-minutes %) 25) (by-pathway "ordered-in-ed")) "the ED class default")
      (is (every? #(<= 15 (lag-minutes %) 60) (by-pathway "ordered-on-renal")) "the inpatient class default")
      (is (every? #(<= 40 (lag-minutes %) 50) (by-pathway "ordered-on-cardiology")) "Cardiology's own range"))
    (testing "the order ended at its own instant is never filled"
      (is (= {:stopped 3}
             (frequencies (map #(:state (:citation (second %))) undispensed)))))
    (testing "the order outrun by the discharge is a discharge prescription (ADR-0190)"
      (is (= [:outrun :outrun :outrun] (mapv #(:state (:citation (order-of %))) community)))
      (doseq [d community
              :let [lag (/ (- (:t d) (:t (discharge-of d))) 60)]]
        (is (<= 120 lag 240) "filled two to four hours after the discharge, this facility's range")
        (is (not (contains? d :encounter-id)) "a community fill belongs to no encounter")))
    (testing "each inpatient dispense carries the id of the stay that wrote its order"
      (doseq [d inpatient]
        (is (some? (:encounter-id d)))
        (is (= (:encounter-id (order-of d)) (:encounter-id d)))))
    (is (ok? r) (violations r))))
