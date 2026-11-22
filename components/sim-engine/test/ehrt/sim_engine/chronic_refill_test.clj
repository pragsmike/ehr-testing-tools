(ns ehrt.sim-engine.chronic-refill-test
  "ADR-0191: under the `:dispense` key a CHRONIC order's community fill
  is refilled. A medication order may carry `:chronic true`; every
  `:medication-dispense` carries a `:fill-number`, 1 for a first
  dispense. When a community fill of a chronic order is emitted, two
  draws on the ordering patient's `:pharmacy` stream say whether another
  follows and how many days later
  (`sim-model/refill-continuation`, `sim-model/refill-interval-days`);
  no refill is queued later than the last arrival plus
  `sim-model/refill-tail-days`.

  And a pending pharmacy step follows its patient through a merge: the
  fill is emitted for the survivor and cites the absorbed record's
  order."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.streams :as streams]
            [ehrt.sim-model.interface :as sim-model]))

(def ^:private rx {:module "clinic" :state :long-term})
(def ^:private stay-rx {:module "stay" :state :long-term})
(def ^:private a-concept {:system :rxnorm :code "860975"
                          :display "24 HR Metformin hydrochloride 500 MG Extended Release Oral Tablet"})
(def ^:private day 86400)

(defn- facility [options]
  (merge sim-model/default-facility options))

(def ^:private chronic-order {:type :medication-order :codes [a-concept] :citation rx :chronic true})

(defn- visit [order]
  {:name "clinic"
   :steps [{:type :outpatient-visit}
           order
           {:type :delay :from 20 :to 20}
           {:type :outpatient-visit-end}]})

(defn- visit-config [patients order options]
  {:seed 7 :patients patients :arrival-gap 30 :dispense true
   :facility (facility options)
   :pathways [{:weight 1 :pathway (visit order)}]})

(defn- subject-of [event] (:patient-id (first (:participants event))))
(defn- of-kind [kind log] (filter #(= kind (:event %)) log))
(defn- ok? [r] (result/ok? (check/check-all (:ground-truth r) (:facility r))))
(defn- violations [r] (pr-str (:violations (:payload (check/check-all (:ground-truth r) (:facility r))))))
(defn- fills-by-order [log] (group-by :order-event-id (of-kind :medication-dispense log)))

(def ^:private every-fill-and-every-refill
  "Every prescription filled an hour after the close; every fill followed
  by another exactly thirty days later; refills until a hundred days
  past the last arrival."
  {:community-fill-share 1 :community-fill-minutes [60 60]
   :refill-continuation 1 :refill-interval-days [30 30] :refill-tail-days 100})

;; --- (a) ----------------------------------------------------------------

(deftest a-chronic-orders-fill-is-number-one-and-is-refilled-each-interval-until-the-tail
  (let [r (run/run (visit-config 5 chronic-order every-fill-and-every-refill))
        log (vec (:ground-truth r))
        orders (of-kind :medication-order log)
        last-arrival (apply max (map :t (of-kind :registered log)))
        cutoff (+ last-arrival (* 100 day))]
    (is (= 5 (count orders)))
    (is (every? #(true? (:chronic %)) orders) "the order event carries the marker")
    (doseq [[order-idx fills] (fills-by-order log)]
      (is (= :medication-order (:event (nth log order-idx))))
      (is (= [1 2 3 4] (map :fill-number fills)) "an hour after the close, then +30, +60 and +90 days")
      (is (every? #(= :community (:setting %)) fills))
      (is (= [(* 30 day) (* 30 day) (* 30 day)] (map - (rest (map :t fills)) (map :t fills))))
      (is (<= (:t (last fills)) cutoff))
      (is (> (+ (:t (last fills)) (* 30 day)) cutoff) "the next one would land past the tail"))
    (is (= 5 (count (fills-by-order log))))
    (is (ok? r) (violations r))))

;; --- (b) ----------------------------------------------------------------

(deftest with-continuation-at-zero-a-chronic-order-is-filled-once
  (let [r (run/run (visit-config 5 chronic-order (assoc every-fill-and-every-refill :refill-continuation 0)))
        fills (of-kind :medication-dispense (:ground-truth r))]
    (is (= 5 (count fills)))
    (is (= #{1} (set (map :fill-number fills))))
    (is (ok? r) (violations r))))

;; --- (c) ----------------------------------------------------------------

(deftest only-a-filled-chronic-community-prescription-refills
  (testing "an order not marked chronic is filled once"
    (let [r (run/run (visit-config 5 (dissoc chronic-order :chronic) every-fill-and-every-refill))
          log (:ground-truth r)]
      (is (not-any? #(contains? % :chronic) (of-kind :medication-order log)) "absent, never false")
      (is (= [1 1 1 1 1] (map :fill-number (of-kind :medication-dispense log))))
      (is (ok? r) (violations r))))
  (testing "a chronic prescription that is never filled is never refilled"
    (let [r (run/run (visit-config 5 chronic-order (assoc every-fill-and-every-refill :community-fill-share 0)))]
      (is (empty? (of-kind :medication-dispense (:ground-truth r))))))
  (testing "an inpatient dispense of a chronic order is number 1 and is not refilled"
    (let [r (run/run {:seed 7 :patients 5 :arrival-gap 30 :dispense true :encounters true
                      :facility (facility every-fill-and-every-refill)
                      :pathways [{:weight 1
                                  :pathway {:name "stay"
                                            :steps [{:type :admission :location "Renal"}
                                                    (assoc chronic-order :citation stay-rx)
                                                    {:type :delay :from 180 :to 180}
                                                    {:type :discharge}]}}]})
          dispenses (of-kind :medication-dispense (:ground-truth r))]
      (is (= 5 (count dispenses)))
      (is (every? #(= :inpatient (:setting %)) dispenses))
      (is (= #{1} (set (map :fill-number dispenses))))
      (is (ok? r) (violations r))))
  (testing "a chronic discharge prescription is refilled as a visit's is"
    (let [r (run/run {:seed 7 :patients 5 :arrival-gap 30 :dispense true :encounters true
                      :facility (facility every-fill-and-every-refill)
                      :pathways [{:weight 1
                                  :pathway {:name "stay"
                                            :steps [{:type :admission :location "Renal"}
                                                    (assoc chronic-order :citation stay-rx)
                                                    {:type :discharge}]}}]})
          log (vec (:ground-truth r))]
      (is (= 5 (count (fills-by-order log))))
      (doseq [[_ fills] (fills-by-order log)]
        (is (every? #(= :community (:setting %)) fills))
        (is (= [1 2 3 4] (map :fill-number fills))))
      (is (ok? r) (violations r)))))

;; --- (d) ----------------------------------------------------------------

(deftest an-order-ended-between-fills-stops-the-chain
  (let [ended (update (visit chronic-order) :steps into
                      [{:type :delay :from (* 45 24 60) :to (* 45 24 60)}
                       {:type :medication-end :order-citation rx}])
        r (run/run {:seed 7 :patients 5 :arrival-gap 30 :dispense true
                    :facility (facility every-fill-and-every-refill)
                    :pathways [{:weight 1 :pathway ended}]})
        log (vec (:ground-truth r))]
    (is (= 5 (count (of-kind :medication-end log))))
    (doseq [[_ fills] (fills-by-order log)]
      (is (= [1 2] (map :fill-number fills)) "the order ends forty-five days in; the fill at sixty never happens"))
    (is (= 5 (count (fills-by-order log))))
    (is (ok? r) (violations r))))

;; --- (e) ----------------------------------------------------------------

(deftest at-the-defaults-about-ninety-four-fills-in-a-hundred-are-followed-by-another-and-runs-agree
  (let [;; A tail long enough that the draw, and not the tail, ends
        ;; nearly every chain.
        config (visit-config 600 chronic-order {:refill-tail-days 36500})
        r (run/run config)
        log (vec (:ground-truth r))
        chains (vals (fills-by-order log))
        fills (count (of-kind :medication-dispense log))
        followed (reduce + (map (comp dec count) chains))
        [lo hi] sim-model/default-refill-interval-days
        gaps (for [c chains [a b] (partition 2 1 (map :t c))] (- b a))]
    (is (< 400 (count chains) 560) "about four prescriptions in five are filled at all")
    (is (every? #(= (range 1 (inc (count %))) (map :fill-number %)) chains))
    ;; Several thousand Bernoulli(0.94) draws: one standard deviation is
    ;; under 0.003.
    (is (< 0.93 (/ followed (double fills)) 0.95) (str followed " of " fills))
    (is (every? #(<= (* lo day) % (* hi day)) gaps))
    (is (< 1 (count (distinct gaps))) "the interval is drawn")
    (is (= (:ground-truth r) (:ground-truth (run/run config))) "two runs write one log")))

;; --- 2026-11-22: the tail is optional, and absent by default --------------
;;
;; ADR-0191's addendum. With no `:refill-tail-days` stated nothing bounds
;; a chain but its own draw and its order's end.

(def ^:private no-tail (dissoc every-fill-and-every-refill :refill-tail-days))

(deftest with-no-tail-stated-a-chain-runs-past-day-ninety-and-stops-only-at-its-orders-end
  (let [ended (update (visit chronic-order) :steps into
                      [{:type :delay :from (* 200 24 60) :to (* 200 24 60)}
                       {:type :medication-end :order-citation rx}])
        r (run/run {:seed 7 :patients 5 :arrival-gap 30 :dispense true
                    :facility (facility no-tail)
                    :pathways [{:weight 1 :pathway ended}]})
        log (vec (:ground-truth r))
        last-arrival (apply max (map :t (of-kind :registered log)))]
    (is (= 5 (count (fills-by-order log))))
    (doseq [[_ fills] (fills-by-order log)]
      (is (= [1 2 3 4 5 6 7] (map :fill-number fills))
          "every thirty days to day 180; the order ends at day 200 and the fill at 210 never happens")
      (is (> (:t (last fills)) (+ last-arrival (* 90 day))) "past where the old default stopped it"))
    (is (ok? r) (violations r))))

(deftest with-no-tail-stated-the-mean-chain-is-the-continuation-draws-own
  (let [config (visit-config 600 chronic-order {})
        r (run/run config)
        log (vec (:ground-truth r))
        chains (vals (fills-by-order log))
        refills (map (comp dec count) chains)
        mean (/ (reduce + refills) (double (count chains)))
        p sim-model/default-refill-continuation
        expected (/ p (- 1 p))]
    (is (not (contains? (:facility config) :refill-tail-days)) "the default facility states no tail")
    (is (< 400 (count chains) 560))
    ;; 0.94 / 0.06 = 15.67 refills a chain. One chain's standard
    ;; deviation is about 16.2, so the mean of some 480 has one near
    ;; 0.74; the tolerance is about three of those.
    (is (< (- expected 2.5) mean (+ expected 2.5)) (str "mean refills per chain " mean))
    (is (every? #(= (range 1 (inc (count %))) (map :fill-number %)) chains))
    (is (= (:ground-truth r) (:ground-truth (run/run config))) "two runs write one log")
    (is (ok? r) (violations r))))

;; --- (f) ----------------------------------------------------------------

(deftest a-fill-pending-at-a-merge-is-emitted-for-the-survivor
  (let [seed 400
        survivor-id (streams/patient-id-for seed 0)
        absorbed-id (streams/patient-id-for seed 1)
        r (run/run {:seed seed :patients 2 :arrival-gap 0 :dispense true
                    ;; filled ten hours after the close; the merge is three
                    ;; hours in.
                    :facility (facility (assoc every-fill-and-every-refill :community-fill-minutes [600 600]))
                    :pathways [{:patient-ordinal 0
                                :pathway {:name "survivor"
                                          :steps [{:type :admission :location "Renal"}
                                                  {:type :delay :from 180 :to 180}
                                                  {:type :merge :with absorbed-id}
                                                  {:type :delay :from 60 :to 60}
                                                  {:type :discharge}]}}
                               {:patient-ordinal 1 :pathway (visit chronic-order)}]})
        log (vec (:ground-truth r))
        order (first (of-kind :medication-order log))
        merge-ev (first (of-kind :merge log))
        fills (of-kind :medication-dispense log)]
    (is (= absorbed-id (subject-of order)))
    (is (some? merge-ev))
    (is (= [1 2 3 4] (map :fill-number fills)) "the first fill and its refills all survive the merge")
    (doseq [d fills]
      (is (< (:t merge-ev) (:t d)))
      (is (= survivor-id (subject-of d)) "emitted for the survivor")
      (is (= (:surviving-mrn merge-ev) (:active-mrn d)))
      (is (= :community (:setting d)))
      (is (= (.indexOf log order) (:order-event-id d)) "and cites the absorbed record's order")
      (is (= rx (:order-citation d))))
    (is (ok? r) (violations r))))

(deftest an-inpatient-dispense-pending-at-a-merge-goes-to-the-survivors-open-stay
  (let [seed 400
        survivor-id (streams/patient-id-for seed 0)
        absorbed-id (streams/patient-id-for seed 1)
        r (run/run {:seed seed :patients 2 :arrival-gap 0 :dispense true :encounters true
                    :facility (-> (facility every-fill-and-every-refill)
                                  (update :wards (fn [ws] (mapv #(assoc % :dispense-minutes [60 60]) ws))))
                    :pathways [{:patient-ordinal 0
                                :pathway {:name "survivor"
                                          :steps [{:type :admission :location "Renal"}
                                                  {:type :delay :from 10 :to 10}
                                                  {:type :merge :with absorbed-id}
                                                  {:type :delay :from 240 :to 240}
                                                  {:type :discharge}]}}
                               {:patient-ordinal 1
                                :pathway {:name "absorbed"
                                          :steps [{:type :admission :location "Renal"}
                                                  {:type :medication-order :codes [a-concept] :citation stay-rx}
                                                  {:type :delay :from 300 :to 300}
                                                  {:type :discharge}]}}]})
        log (vec (:ground-truth r))
        order (first (of-kind :medication-order log))
        admission (first (filter #(= survivor-id (subject-of %)) (of-kind :admission log)))
        dispenses (of-kind :medication-dispense log)]
    (is (= absorbed-id (subject-of order)))
    (is (= 1 (count dispenses)))
    (let [d (first dispenses)]
      (is (= :inpatient (:setting d)))
      (is (= 1 (:fill-number d)))
      (is (= survivor-id (subject-of d)))
      (is (= (:encounter-id admission) (:encounter-id d)) "the survivor's own open stay")
      (is (= (.indexOf log order) (:order-event-id d))))
    (is (ok? r) (violations r))))

;; --- (g) ----------------------------------------------------------------

(deftest a-vendored-modules-chronic-flag-reaches-the-order-event
  (let [module (:payload (patient-simulator/load-module
                          "osteoporosis" (slurp (io/resource "sim/modules/osteoporosis.json"))))
        ;; The oracle's `osteoporosis` root, verbatim.
        r (run/run {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
                    :modules [(patient-simulator/singleton-closure module)]
                    :module-assignment [{:module-id "osteoporosis" :weight 1}]
                    :module-horizon-days 36500})
        orders (of-kind :medication-order (:ground-truth r))]
    (is (= 156 (count orders)))
    (is (every? #(= :prescribe-bisphosphate (:state (:citation %))) orders))
    (is (every? #(true? (:chronic %)) orders) "`osteoporosis.json` marks Prescribe_Bisphosphate chronic")))

;; --- an authored dispense is numbered too --------------------------------

(deftest authored-dispenses-of-one-order-are-numbered-in-order-without-the-key
  (let [r (run/run {:seed 7 :patients 3 :arrival-gap 30
                    :pathways [{:weight 1
                                :pathway {:name "authored"
                                          :steps [{:type :outpatient-visit}
                                                  chronic-order
                                                  {:type :outpatient-visit-end}
                                                  {:type :medication-dispense :setting :community :order-citation rx}
                                                  {:type :delay :from 60 :to 60}
                                                  {:type :medication-dispense :setting :community :order-citation rx}]}}]})
        log (vec (:ground-truth r))]
    (doseq [[_ fills] (fills-by-order log)]
      (is (= [1 2] (map :fill-number fills)) "no key, so no chain: the two the author wrote"))
    (is (= 3 (count (fills-by-order log))))
    (is (ok? r) (violations r))))
