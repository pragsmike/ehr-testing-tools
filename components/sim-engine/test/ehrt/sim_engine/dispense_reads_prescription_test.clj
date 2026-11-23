(ns ehrt.sim-engine.dispense-reads-prescription-test
  "Event schema 1.15.0: a dispense READS its order's prescription.

  * REFILLABLE: an order that is chronic, or that states a positive
    `:refills`.
  * BOUNDED: where `:refills` is stated, at most that many refills. A
    stated 0 means none, on a chronic order too. An order with no count
    runs on the continuation draw alone.
  * THE INTERVAL: where the order states a duration the tree can turn
    into days, the next fill is that many days later plus a drawn offset
    (`sim-model/refill-offset-days`), never less than one day. Otherwise
    the facility's `:refill-interval-days`.
  * TWO FIELDS on the dispense: `:days-supply` and `:quantity`, absent
    when the order states too little to compute them. An inpatient
    dispense is one day's supply."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-engine.event-schema :as es]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.streams :as streams]
            [ehrt.sim-model.interface :as sim-model]))

(def ^:private rx {:module "clinic" :state :prescribed})
(def ^:private stay-rx {:module "stay" :state :prescribed})
(def ^:private a-concept {:system :rxnorm :code "860975"
                          :display "24 HR Metformin hydrochloride 500 MG Extended Release Oral Tablet"})
(def ^:private day 86400)

(defn- order [& {:as fields}]
  (merge {:type :medication-order :codes [a-concept] :citation rx} fields))

(defn- visit [an-order]
  {:name "clinic"
   :steps [{:type :outpatient-visit}
           an-order
           {:type :delay :from 20 :to 20}
           {:type :outpatient-visit-end}]})

(defn- stay [an-order]
  {:name "stay"
   :steps [{:type :admission :location "Renal"}
           (assoc an-order :citation stay-rx)
           {:type :delay :from 180 :to 180}
           {:type :discharge}]})

(def ^:private every-fill-and-every-refill
  "Every prescription filled an hour after the close and every fill
  followed by another, with no tail: only a bound or an order's end
  stops a chain."
  {:community-fill-share 1 :community-fill-minutes [60 60] :refill-continuation 1})

(defn- visit-config [patients an-order options]
  {:seed 7 :patients patients :arrival-gap 30 :dispense true
   :facility (merge sim-model/default-facility every-fill-and-every-refill options)
   :pathways [{:weight 1 :pathway (visit an-order)}]})

(defn- subject-of [event] (:patient-id (first (:participants event))))
(defn- of-kind [kind log] (filter #(= kind (:event %)) log))
(defn- ok? [r] (result/ok? (check/check-all (:ground-truth r) (:facility r))))
(defn- violations [r] (pr-str (:violations (:payload (check/check-all (:ground-truth r) (:facility r))))))
(defn- chains [log] (vals (group-by :order-event-id (of-kind :medication-dispense log))))
(defn- gaps-in-days [chain] (map #(/ (- %2 %1) day) (map :t chain) (rest (map :t chain))))

;; --- (a) (b) (c): what refills, and how far -------------------------------

(deftest a-non-chronic-order-stating-two-refills-is-filled-three-times-and-no-more
  (let [r (run/run (visit-config 5 (order :prescription {:refills 2}) {}))
        log (:ground-truth r)]
    (is (= 5 (count (chains log))))
    (doseq [chain (chains log)]
      (is (= [1 2 3] (map :fill-number chain)) "continuation is 1 and nothing else stops it: the count does"))
    (is (ok? r) (violations r))))

(deftest a-chronic-order-stating-no-refills-is-filled-once
  (let [r (run/run (visit-config 5 (order :chronic true :prescription {:refills 0 :as-needed true}) {:refill-tail-days 100}))
        fills (of-kind :medication-dispense (:ground-truth r))]
    (is (= [1 1 1 1 1] (map :fill-number fills)) "a stated 0 means none, chronic or not")
    (is (ok? r) (violations r))))

(deftest a-chronic-order-stating-no-count-runs-on-the-draw-alone
  (testing "at continuation 1 only the tail stops it"
    (let [r (run/run (visit-config 5 (order :chronic true) {:refill-interval-days [30 30] :refill-tail-days 100}))]
      (doseq [chain (chains (:ground-truth r))]
        (is (= [1 2 3 4] (map :fill-number chain))))
      (is (ok? r) (violations r))))
  (testing "and a prescription that states other things but no count changes nothing about that"
    (let [r (run/run (visit-config 5 (order :chronic true :prescription {:as-needed false})
                                   {:refill-interval-days [30 30] :refill-tail-days 100}))]
      (doseq [chain (chains (:ground-truth r))]
        (is (= [1 2 3 4] (map :fill-number chain))))))
  (testing "an order that is neither chronic nor states a positive count is filled once"
    (let [r (run/run (visit-config 5 (order :prescription {:refills 0}) {}))]
      (is (= [1 1 1 1 1] (map :fill-number (of-kind :medication-dispense (:ground-truth r))))))))

(deftest the-bound-and-the-draw-both-apply
  (let [r (run/run (visit-config 400 (order :chronic true :prescription {:refills 3}) {:refill-continuation 0.5}))
        lengths (frequencies (map count (chains (:ground-truth r))))]
    (is (= #{1 2 3 4} (set (keys lengths))) "the draw stops some early, the count stops the rest at four fills")
    (is (ok? r) (violations r))))

;; --- (d): the interval ------------------------------------------------------

(def ^:private fourteen-days {:duration {:quantity 14 :unit "days"}})

(deftest a-stated-supply-sets-the-refill-interval
  (testing "a 14-day supply refills 12 to 19 days later under the default offset"
    (let [r (run/run (visit-config 40 (order :chronic true :prescription fourteen-days) {:refill-tail-days 120}))
          gaps (mapcat gaps-in-days (chains (:ground-truth r)))]
      (is (< 100 (count gaps)))
      (is (every? #(<= 12 % 19) gaps))
      (is (< 4 (count (set gaps))) "drawn, not fixed")
      (is (ok? r) (violations r))))
  (testing "an order with no duration keeps the facility's range"
    (let [r (run/run (visit-config 40 (order :chronic true :prescription {:refills 4}) {:refill-interval-days [40 41]}))
          gaps (mapcat gaps-in-days (chains (:ground-truth r)))]
      (is (= 160 (count gaps)))
      (is (= #{40 41} (set gaps)))))
  (testing "a duration in a unit the tree cannot turn into days keeps it too"
    (let [r (run/run (visit-config 5 (order :prescription {:refills 2 :duration {:quantity 1 :unit "months"}})
                                   {:refill-interval-days [40 40]}))]
      (is (= #{40} (set (mapcat gaps-in-days (chains (:ground-truth r))))))))
  (testing "the facility's offset overrides the default"
    (let [r (run/run (visit-config 5 (order :prescription (assoc fourteen-days :refills 2)) {:refill-offset-days [3 3]}))]
      (is (= [17 17] (gaps-in-days (first (chains (:ground-truth r))))))))
  (testing "never less than one day, whatever the offset"
    (let [r (run/run (visit-config 5 (order :prescription (assoc fourteen-days :refills 2)) {:refill-offset-days [-30 -30]}))]
      (is (= [1 1] (gaps-in-days (first (chains (:ground-truth r)))))))))

(deftest a-thirty-day-supply-under-the-default-offset-draws-what-the-default-interval-draws
  (let [with (visit-config 60 (order :chronic true :prescription {:duration {:quantity 30 :unit "days"}})
                           {:refill-continuation 0.9 :refill-tail-days 400})
        without (visit-config 60 (order :chronic true) {:refill-continuation 0.9 :refill-tail-days 400})
        instants #(mapv (juxt :t :fill-number subject-of) (of-kind :medication-dispense (:ground-truth (run/run %))))]
    (is (< 100 (count (instants with))))
    (is (= (instants without) (instants with)) "30 + [-2, 5] is [28, 35], off the same draw")))

;; --- (e) (f): the two fields ------------------------------------------------

(defn- fields-of [dispense] (select-keys dispense [:days-supply :quantity]))

(defn- community-fields [prescription]
  (let [r (run/run (visit-config 3 (order :prescription prescription) {}))
        log (:ground-truth r)]
    (is (every? es/valid-event? log))
    (is (ok? r) (violations r))
    (set (map fields-of (of-kind :medication-dispense log)))))

(deftest a-community-dispense-carries-its-orders-supply-and-quantity
  (is (= #{{:days-supply 7 :quantity 14}}
         (community-fields {:dosage {:amount 1 :frequency 2 :period 1 :unit "days"} :duration {:quantity 7 :unit "days"}})))
  (testing "weeks are seven days each"
    (is (= #{{:days-supply 14 :quantity 42}}
           (community-fields {:dosage {:amount 1 :frequency 3 :period 1 :unit "days"} :duration {:quantity 2 :unit "weeks"}}))))
  (testing "a period in hours converts exactly: once every six hours is four a day"
    (is (= #{{:days-supply 10 :quantity 40}}
           (community-fields {:dosage {:amount 1 :frequency 1 :period 6 :unit "hours"} :duration {:quantity 10 :unit "days"}}))))
  (testing "a result that is not whole is kept, not rounded"
    (is (= #{{:days-supply 5 :quantity 2.5}}
           (community-fields {:dosage {:amount 1 :frequency 1 :period 2 :unit "days"} :duration {:quantity 5 :unit "days"}}))))
  (testing "a duration alone gives a supply and no quantity"
    (is (= #{{:days-supply 30}} (community-fields {:duration {:quantity 30 :unit "days"} :refills 0}))))
  (testing "as-needed alone gives neither, and so does a dosage with no duration"
    (is (= #{{}} (community-fields {:as-needed true})))
    (is (= #{{}} (community-fields {:dosage {:amount 2 :frequency 4 :period 1 :unit "days"} :as-needed true}))))
  (testing "a unit the tree cannot convert exactly leaves its field absent"
    (is (= #{{}} (community-fields {:dosage {:amount 1 :frequency 1 :period 1 :unit "days"} :duration {:quantity 1 :unit "months"}})))
    (is (= #{{:days-supply 7}}
           (community-fields {:dosage {:amount 1 :frequency 1 :period 1 :unit "months"} :duration {:quantity 7 :unit "days"}})))
    (is (= #{{}} (community-fields {:duration {:quantity 36 :unit "hours"}})) "a day and a half is not a whole number of days")
    (is (= #{{:days-supply 2}} (community-fields {:duration {:quantity 48 :unit "hours"}}))))
  (testing "an order with no prescription gives neither"
    (let [r (run/run (visit-config 3 (order) {}))]
      (is (= #{{}} (set (map fields-of (of-kind :medication-dispense (:ground-truth r)))))))))

(deftest every-fill-of-a-chain-carries-the-same-fields
  (let [r (run/run (visit-config 5 (order :prescription {:dosage {:amount 1 :frequency 1 :period 1 :unit "days"}
                                                         :duration {:quantity 30 :unit "days"} :refills 2})
                                 {}))
        fills (of-kind :medication-dispense (:ground-truth r))]
    (is (= 15 (count fills)))
    (is (= #{{:days-supply 30 :quantity 30}} (set (map fields-of fills))))))

(defn- inpatient-fields [prescription]
  (let [r (run/run {:seed 7 :patients 5 :arrival-gap 30 :dispense true :encounters true
                    :facility (merge sim-model/default-facility every-fill-and-every-refill)
                    :pathways [{:weight 1 :pathway (stay (order :prescription prescription))}]})
        dispenses (of-kind :medication-dispense (:ground-truth r))]
    (is (= 5 (count dispenses)))
    (is (every? #(= :inpatient (:setting %)) dispenses))
    (is (every? es/valid-event? (:ground-truth r)))
    (is (ok? r) (violations r))
    (set (map fields-of dispenses))))

(deftest an-inpatient-dispense-is-one-days-supply-whatever-the-orders-duration
  (is (= #{{:days-supply 1 :quantity 8}}
         (inpatient-fields {:dosage {:amount 2 :frequency 4 :period 1 :unit "days"} :as-needed true})))
  (is (= #{{:days-supply 1 :quantity 1}}
         (inpatient-fields {:dosage {:amount 1 :frequency 1 :period 1 :unit "days"} :duration {:quantity 5 :unit "days"}})))
  (testing "an order that states no dosage gives neither"
    (is (= #{{}} (inpatient-fields {:duration {:quantity 5 :unit "days"}})))
    (is (= #{{}} (inpatient-fields {:as-needed true})))))

(deftest the-event-schema-types-the-two-fields
  (let [dispense (first (of-kind :medication-dispense (:ground-truth (run/run (visit-config 1 (order) {})))))]
    (is (es/valid-event? dispense) "both are optional")
    (is (es/valid-event? (assoc dispense :days-supply 30 :quantity 30)))
    (is (es/valid-event? (assoc dispense :days-supply 5 :quantity 2.5)))
    (is (not (es/valid-event? (assoc dispense :days-supply 0))))
    (is (not (es/valid-event? (assoc dispense :days-supply 1.5))))
    (is (not (es/valid-event? (assoc dispense :quantity 0))))
    (is (not (es/valid-event? (assoc dispense :quantity "30"))))))

;; --- (h): through a merge ---------------------------------------------------

(deftest a-fill-carried-through-a-merge-keeps-its-bound-and-its-fields
  (let [seed 400
        survivor-id (streams/patient-id-for seed 0)
        absorbed-id (streams/patient-id-for seed 1)
        r (run/run {:seed seed :patients 2 :arrival-gap 0 :dispense true
                    ;; filled ten hours after the close; the merge is three
                    ;; hours in.
                    :facility (merge sim-model/default-facility every-fill-and-every-refill
                                     {:community-fill-minutes [600 600] :refill-offset-days [0 0]})
                    :pathways [{:patient-ordinal 0
                                :pathway {:name "survivor"
                                          :steps [{:type :admission :location "Renal"}
                                                  {:type :delay :from 180 :to 180}
                                                  {:type :merge :with absorbed-id}
                                                  {:type :delay :from 60 :to 60}
                                                  {:type :discharge}]}}
                               {:patient-ordinal 1
                                :pathway (visit (order :prescription {:dosage {:amount 1 :frequency 2 :period 1 :unit "days"}
                                                                      :duration {:quantity 14 :unit "days"} :refills 2}))}]})
        log (vec (:ground-truth r))
        merge-ev (first (of-kind :merge log))
        fills (of-kind :medication-dispense log)]
    (is (some? merge-ev))
    (is (= [1 2 3] (map :fill-number fills)) "two refills, and the bound still holds on the survivor")
    (is (= [14 14] (gaps-in-days fills)))
    (doseq [d fills]
      (is (< (:t merge-ev) (:t d)))
      (is (= survivor-id (subject-of d)))
      (is (= {:days-supply 14 :quantity 28} (fields-of d))))
    (is (ok? r) (violations r))))

;; --- (i): determinism -------------------------------------------------------

(deftest two-runs-at-one-revision-write-one-log
  (let [config {:seed 11 :patients 120 :arrival-gap 30 :dispense true :encounters true
                :facility (merge sim-model/default-facility {:refill-tail-days 400})
                :pathways [{:weight 2 :pathway (visit (order :chronic true
                                                             :prescription {:dosage {:amount 1 :frequency 1 :period 1 :unit "days"}
                                                                            :duration {:quantity 30 :unit "days"} :refills 11}))}
                           {:weight 1 :pathway (visit (order :prescription {:refills 2 :duration {:quantity 2 :unit "weeks"}}))}
                           {:weight 1 :pathway (stay (order :prescription {:dosage {:amount 2 :frequency 4 :period 1 :unit "days"}}))}]}
        a (run/run config)
        b (run/run config)]
    (is (= (pr-str (:ground-truth a)) (pr-str (:ground-truth b))))
    (is (some :quantity (of-kind :medication-dispense (:ground-truth a))))
    (is (some #(< 1 (:fill-number %)) (of-kind :medication-dispense (:ground-truth a))))
    (is (ok? a) (violations a))))
