(ns ehrt.judge.pairing-test
  "Unit coverage for ehrt.judge.pairing itself (ADR-0088): the
  committed registry loads and validates, and `coverage`'s pure
  reduction is correct against synthetic data -- the tier-one
  execution loop (does every row actually witness its own class
  against a real fixture and a real judge) lives in
  pairing_conviction_test.clj instead, since it needs `corpus` and
  both v2 judge engines on the classpath."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.judge.pairing :as pairing]))

(deftest load-registry-returns-a-valid-non-empty-registry-test
  (let [rows (pairing/load-registry)]
    (is (seq rows))
    (is (every? #(re-matches #"\d{4}" (:adr (:witness %))) rows))
    (is (every? #(contains? #{:judge-v2-hapi :judge-v2-nist} (:judge %)) rows))))

(deftest coverage-reports-every-supplied-operator-id-including-zero-witness-ones-test
  (let [rows [{:operator {:id :op-a :version "1"} :judge :judge-v2-hapi
               :locator "X" :expected #{"c"} :fixture "f"
               :witness {:adr "0088" :date "2026-08-08"}}
              {:operator {:id :op-a :version "1"} :judge :judge-v2-nist
               :locator "X" :expected #{"c"} :fixture "f"
               :witness {:adr "0088" :date "2026-08-08"}}]]
    (testing "an operator with rows against both judges"
      (is (= #{:judge-v2-hapi :judge-v2-nist}
             (get (pairing/coverage rows [:op-a :op-b]) :op-a))))
    (testing "an operator with zero witnessed rows still appears, with an empty set"
      (is (= #{} (get (pairing/coverage rows [:op-a :op-b]) :op-b))))))
