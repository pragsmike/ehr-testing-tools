(ns ehrt.sim.ladders-run-test
  "ARC 4 SWEEP 3 (`notes/adr/0175-arc-4-emission-add-ons.md` design (b),
  ruling B1, landed DARK): `:ladders` as a `:config` passenger, at
  POPULATION scale.

  This is the half `ehrt.sim-emit-hl7.ladders-test` cannot reach --
  that brick may not depend on `components/sim`, so its log is hand
  built. What only a real run shows is the thing the ladder is priced
  on: an order whose result arrives at a profile-sampled turnaround,
  many of them, at instants nobody chose.

  GROUND TRUTH DOES NOT MOVE, asserted here rather than argued: the
  same run with and without `:ladders` produces the identical
  `:ground-truth`. `bin/ground-truth-bracket` makes that claim across
  every engine-layer oracle root per commit; this makes it per test
  run, without a worktree."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]
            [ehrt.sim.run :as run]))

(def ^:private base-opts
  "Small and fast, and shaped like `order-pathway`, the oracle root this
  sweep's step 0 added: two orders a stay, one either side of the
  delay, so one encounter carries two independent order->result
  intervals."
  {:seed 202 :patients 8 :arrival-gap 90
   :pathways [{:pathway {:name "workup"
                         :steps [{:type :admission :location "Renal"}
                                 {:type :order :profile :cbc}
                                 {:type :delay :from 180 :to 900}
                                 {:type :order :profile :bmp}
                                 {:type :discharge}]}
               :weight 1}]
   :emit "hl7"})

(def ^:private ladders {:rungs [0.25 0.5] :order-rungs [0.1]})

(defn- run-payload [opts] (:payload (run/run-command opts)))

(defn- msh-9 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 8))
(defn- msh-10 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 9))
(defn- segment [m nm] (first (filter #(str/starts-with? % (str nm "|")) (str/split m #"\r"))))
(defn- segments-named [m nm] (filterv #(str/starts-with? % (str nm "|")) (str/split m #"\r")))
(defn- field [seg n] (nth (str/split seg #"\|" -1) n ""))
(defn- rung? [m] (= 4 (count (str/split (msh-10 m) #"-"))))

(deftest ladders-are-emission-only-and-ground-truth-does-not-move
  (let [without (run-payload (dissoc base-opts :emit))
        with (run-payload (assoc (dissoc base-opts :emit) :ladders ladders))]
    (is (seq (:ground-truth without)))
    (is (pos? (count (filter #(= :order-placed (:event %)) (:ground-truth without))))
        "rulings.md#R-empty-population-is-red: a run with no order would make every
         assertion in this namespace vacuous")
    (is (= (:ground-truth without) (:ground-truth with))
        "the whole claim of arc 4: an emission add-on moves no ground truth")
    (testing "and the key provably cannot reach the engine at all"
      (is (not (contains? (set engine/config-keys) :ladders))))))

(deftest a-malformed-ladder-is-an-error-and-never-a-silent-no-op
  (testing "a fraction authored as a percentage reads as a config error"
    (is (= :invalid-ladders (:category (run/run-command (assoc base-opts :ladders {:rungs [25]}))))))
  (testing "so does a typo'd key, because the schema is closed"
    (is (= :invalid-ladders
           (:category (run/run-command (assoc base-opts :ladders {:rung [0.5]}))))))
  (testing "and an endpoint is not a rung"
    (is (= :invalid-ladders (:category (run/run-command (assoc base-opts :ladders {:rungs [0]})))))
    (is (= :invalid-ladders (:category (run/run-command (assoc base-opts :ladders {:rungs [1]})))))))

(deftest every-non-ladder-message-is-byte-equal-at-population-scale
  (let [without (run-payload base-opts)
        with (run-payload (assoc base-opts :ladders ladders))
        laddered-finals (into #{} (map msh-10)
                              (filterv #(and (not (rung? %))
                                             (= "ORU^R01" (msh-9 %))
                                             (= "F" (field (segment % "OBR") 25)))
                                       (:messages with)))]
    (is (pos? (count (:messages without))))
    (is (seq laddered-finals) "the terminal messages this sweep DECLARES it moves")
    (is (= (filterv #(not (laddered-finals (msh-10 %))) (:messages without))
           (filterv #(and (not (rung? %)) (not (laddered-finals (msh-10 %)))) (:messages with)))
        "every message that is neither a rung nor a laddered order's own terminal result is
         byte-equal AND in the same position -- the ladder inserts, it never re-times")))

(deftest the-ladder-witness-table-at-population-scale
  (let [{:keys [ground-truth messages]} (run-payload (assoc base-opts :ladders ladders))
        orders (filterv #(= :order-placed (:event %)) ground-truth)
        results (filterv #(= :result-available (:event %)) ground-truth)
        plan (emit-hl7/plan-ladders ground-truth ladders)
        rungs (filterv rung? messages)
        oru-rungs (filterv #(= "ORU^R01" (msh-9 %)) rungs)
        orm-rungs (filterv #(= "ORM^O01" (msh-9 %)) rungs)]
    (testing "the populations are real"
      (is (pos? (count orders)))
      (is (pos? (count results)))
      (is (pos? (count rungs))))
    (testing "one rung per (result, fraction, family), since no interval here is zero-length"
      (is (= (* 2 (count results)) (count oru-rungs)))
      (is (= (count results) (count orm-rungs)))
      (is (= (count (:rungs plan)) (count rungs))))
    (testing "every ORU rung says P in both fields and every terminal says F"
      (is (every? #(= "P" (field (segment % "OBR") 25)) oru-rungs))
      (is (every? (fn [m] (every? #(= "P" (field % 11)) (segments-named m "OBX"))) oru-rungs))
      (is (every? #(= "SC" (field (segment % "ORC") 5)) orm-rungs)
          "one order rung per order, so the ladder never leaves its first stage here"))
    (testing "the rung identity tuple is injective over a real population"
      (let [tuples (mapv (juxt :active-mrn :trigger :at :ordinal) (:rungs plan))]
        (is (= (count tuples) (count (set tuples))))))
    (testing "and MSH-10 is unique across the whole wire"
      (let [ids (mapv msh-10 messages)]
        (is (= (count ids) (count (set ids))))))))

(deftest a-run-that-places-no-order-gains-no-rung
  (let [no-orders (assoc base-opts
                         :pathways [{:pathway {:name "stay"
                                               :steps [{:type :admission :location "Renal"}
                                                       {:type :delay :from 180 :to 900}
                                                       {:type :discharge}]}
                                     :weight 1}])
        without (run-payload no-orders)
        with (run-payload (assoc no-orders :ladders ladders))]
    (testing "the run really has no orders -- that is the case under test"
      (is (zero? (count (filter #(= :order-placed (:event %)) (:ground-truth with))))))
    (is (pos? (count (:messages without))))
    (is (= (:messages without) (:messages with))
        "three of the six gated corpora place no order at all; for them the ladder is
         byte-identical to absent, and that is asserted rather than assumed")))
