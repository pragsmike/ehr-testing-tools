(ns ehr-testing-sim.engine-test
  "Determinism and invariants over the engine. The properties here are
  the executable form of the problem statement's Guarantees section:
  same inputs + seed => identical output; every run satisfies the
  invariant catalog."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.check :as check]
            [ehr-testing-sim.result :as result]))

(deftest same-seed-same-output
  (testing "byte-identical reruns"
    (let [config {:seed 42 :patients 5}]
      (is (= (engine/run config) (engine/run config)))
      ;; identical after serialization too -- the guarantee is about
      ;; the artifact, not just the in-memory value
      (is (= (pr-str (engine/run config)) (pr-str (engine/run config)))))))

(deftest different-seed-different-output
  ;; Not guaranteed for ALL seed pairs in principle, but for this
  ;; config the delay sampling makes collision practically impossible;
  ;; a failure here means the seed isn't actually reaching the RNG.
  (is (not= (engine/run {:seed 1 :patients 5})
            (engine/run {:seed 2 :patients 5}))))

(deftest walking-skeleton-shape
  (let [{:keys [ground-truth]} (engine/run {:seed 7 :patients 3})]
    (testing "each patient admits then discharges, in time order"
      (is (= 6 (count ground-truth)))
      (is (= #{"MRN000001" "MRN000002" "MRN000003"}
             (set (map :mrn ground-truth))))
      (is (apply <= (map :t ground-truth))))))

(defspec every-run-satisfies-invariant-catalog 200
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 20)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients})]
      (result/ok? (check/check-all ground-truth)))))

(defspec determinism-holds-for-all-seeds 100
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 10)]
    (= (engine/run {:seed seed :patients patients})
       (engine/run {:seed seed :patients patients}))))
