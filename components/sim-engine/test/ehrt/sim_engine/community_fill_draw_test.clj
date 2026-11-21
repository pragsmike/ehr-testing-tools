(ns ehrt.sim-engine.community-fill-draw-test
  "ADR-0190: under the `:dispense` key a community fill is DRAWN. Each
  order an outpatient visit wrote and left open is filled with
  probability `:community-fill-share`, at the visit's close plus a lag
  drawn from `:community-fill-minutes`; both are facility options
  (`sim-model/community-fill-share`, `sim-model/community-fill-minutes`).
  An order ended before the lag elapses is not filled.

  A DISCHARGE PRESCRIPTION joins it: an order written during an
  admission that is still active, and was never dispensed `:inpatient`,
  when the stay is discharged is filled `:community` under the same two
  options, counted from the discharge.

  Both draws are on the patient's `:pharmacy` stream, so the erasure
  law still covers the key
  (`ehrt.sim-engine.dispense-derivation-test`)."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-model.interface :as sim-model]))

(def ^:private rx {:module "clinic" :state :prescription})
(def ^:private stay-rx {:module "stay" :state :prescription})
(def ^:private a-concept {:system :rxnorm :code "562251"
                          :display "Amoxicillin 250 MG / Clavulanate 125 MG Oral Tablet"})

(defn- facility [share minutes]
  (cond-> sim-model/default-facility
    (some? share) (assoc :community-fill-share share)
    minutes (assoc :community-fill-minutes minutes)))

(def ^:private visit
  {:name "clinic"
   :steps [{:type :outpatient-visit}
           {:type :medication-order :codes [a-concept] :citation rx}
           {:type :delay :from 20 :to 20}
           {:type :outpatient-visit-end}]})

(defn- visit-config [patients]
  {:seed 7 :patients patients :arrival-gap 30 :pathways [{:weight 1 :pathway visit}]})

(defn- subject-of [event] (:patient-id (first (:participants event))))
(defn- of-kind [kind log] (filter #(= kind (:event %)) log))
(defn- ok? [r] (result/ok? (check/check-all (:ground-truth r) (:facility r))))
(defn- violations [r] (pr-str (:violations (:payload (check/check-all (:ground-truth r) (:facility r))))))

(defn- instant-of
  "The `:t` of `subject`'s one event of `kind`."
  [kind log subject]
  (:t (first (filter #(and (= kind (:event %)) (= subject (subject-of %))) log))))

;; --- (a) ----------------------------------------------------------------

(deftest with-the-share-at-one-every-visit-order-is-filled-a-drawn-lag-after-the-close
  (let [r (run/run (assoc (visit-config 40) :dispense true :facility (facility 1 [30 90])))
        log (vec (:ground-truth r))
        orders (of-kind :medication-order log)
        fills (of-kind :medication-dispense log)
        lags (for [d fills] (- (:t d) (instant-of :outpatient-visit-end log (subject-of d))))]
    (is (= 40 (count orders)))
    (is (= 40 (count fills)) "every order is filled once")
    (is (every? #(= :community (:setting %)) fills))
    (is (= (set (map subject-of orders)) (set (map subject-of fills))))
    (is (every? #(<= (* 60 30) % (* 60 90)) lags) (pr-str (sort lags)))
    (is (< 1 (count (distinct lags))) "the lag is drawn, not fixed")
    (doseq [d fills]
      (is (= :medication-order (:event (nth log (:order-event-id d)))))
      (is (not (contains? d :citation))))
    (is (ok? r) (violations r))))

;; --- (b) ----------------------------------------------------------------

(deftest with-the-share-at-zero-no-order-is-filled
  (let [r (run/run (assoc (visit-config 40) :dispense true :facility (facility 0 [30 90])))
        log (:ground-truth r)]
    (is (= 40 (count (of-kind :medication-order log))))
    (is (empty? (of-kind :medication-dispense log)))
    (is (ok? r) (violations r))))

;; --- (c) ----------------------------------------------------------------

(deftest at-the-defaults-about-four-in-five-are-filled-inside-the-default-range-and-runs-agree
  (let [config (assoc (visit-config 2000) :dispense true)
        r (run/run config)
        log (vec (:ground-truth r))
        orders (count (of-kind :medication-order log))
        fills (of-kind :medication-dispense log)
        share (/ (count fills) (double orders))
        [lo hi] sim-model/default-community-fill-minutes
        lags (for [d fills] (- (:t d) (instant-of :outpatient-visit-end log (subject-of d))))]
    (is (= 2000 orders))
    ;; 2,000 Bernoulli(0.8) draws: one standard deviation is 0.009.
    (is (< 0.77 share 0.83) (str "filled share " share))
    (is (every? #(<= (* 60 lo) % (* 60 hi)) lags))
    (is (= (:ground-truth r) (:ground-truth (run/run config))) "two runs write one log")))

;; --- (d) ----------------------------------------------------------------

(deftest an-order-ended-during-the-lag-is-not-filled
  (let [ended (update visit :steps into [{:type :delay :from 10 :to 10}
                                         {:type :medication-end :order-citation rx}])
        r (run/run {:seed 7 :patients 12 :arrival-gap 30 :dispense true
                    :facility (facility 1 [60 60])
                    :pathways [{:weight 1 :pathway ended}]})
        log (:ground-truth r)]
    (is (= 12 (count (of-kind :medication-order log))))
    (is (= 12 (count (of-kind :medication-end log))))
    (is (empty? (of-kind :medication-dispense log)) "the order ended ten minutes into a sixty-minute lag")
    (is (ok? r) (violations r))))

;; --- (e) ----------------------------------------------------------------

(defn- stay [name & between]
  {:name name
   :steps (vec (concat [{:type :admission :location "Renal"}
                        {:type :medication-order :codes [a-concept] :citation stay-rx}]
                       between
                       [{:type :discharge}]))})

(deftest an-undispensed-order-at-discharge-is-a-discharge-prescription
  (let [at-discharge (stay "at-discharge")
        dispensed (stay "dispensed" {:type :delay :from 180 :to 180})
        ended (stay "ended" {:type :medication-end :order-citation stay-rx})
        r (run/run {:seed 7 :patients 30 :arrival-gap 30 :dispense true :encounters true
                    :facility (facility 1 [120 240])
                    :pathways (mapv (fn [i] {:patient-ordinal i
                                             :pathway (nth [at-discharge dispensed ended] (mod i 3))})
                                    (range 30))})
        log (vec (:ground-truth r))
        dispenses (of-kind :medication-dispense log)
        reason (fn [d] (:name (nth [at-discharge dispensed ended]
                                   (mod (parse-long (second (re-find #"^PID-(\d+)-" (subject-of d)))) 3))))
        by-pathway (group-by reason dispenses)]
    (is (= 30 (count (of-kind :medication-order log))))
    (testing "an order still active and undispensed when the stay is discharged"
      (let [fills (by-pathway "at-discharge")]
        (is (= 10 (count fills)))
        (is (every? #(= :community (:setting %)) fills))
        (is (every? #(not (contains? % :encounter-id)) fills) "a community fill belongs to no encounter")
        (doseq [d fills
                :let [lag (- (:t d) (instant-of :discharge log (subject-of d)))]]
          (is (<= (* 60 120) lag (* 60 240)) (str "lag from the discharge, " lag " s")))))
    (testing "an order pharmacy dispensed during the stay is not filled again"
      (let [ds (by-pathway "dispensed")]
        (is (= 10 (count ds)))
        (is (every? #(= :inpatient (:setting %)) ds))))
    (testing "an order the stay ended is not filled"
      (is (empty? (by-pathway "ended"))))
    (is (ok? r) (violations r))))

;; --- (g) ----------------------------------------------------------------

(deftest without-the-key-the-options-change-nothing
  (let [plain (run/run (visit-config 40))
        tuned (run/run (assoc (visit-config 40) :facility (facility 0.5 [5 9])))]
    (is (empty? (of-kind :medication-dispense (:ground-truth plain))))
    (is (= (:ground-truth plain) (:ground-truth tuned)))))

;; --- a fill that lands inside a later encounter -------------------------

(deftest a-fill-that-lands-inside-a-later-encounter-takes-no-encounter-id
  (let [then-admitted (update visit :steps into [{:type :delay :from 30 :to 30}
                                                 {:type :admission :location "Renal"}
                                                 {:type :delay :from 240 :to 240}
                                                 {:type :discharge}])
        r (run/run {:seed 7 :patients 6 :arrival-gap 600 :dispense true :encounters true
                    :facility (facility 1 [60 60])
                    :pathways [{:weight 1 :pathway then-admitted}]})
        log (vec (:ground-truth r))
        fills (of-kind :medication-dispense log)]
    (is (= 6 (count fills)))
    (doseq [d fills
            :let [admitted (instant-of :admission log (subject-of d))
                  discharged (instant-of :discharge log (subject-of d))]]
      (is (< admitted (:t d) discharged) "the fill lands while the later stay is open")
      (is (= :community (:setting d)))
      (is (not (contains? d :encounter-id))))
    (is (ok? r) (violations r))))
