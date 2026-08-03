(ns ehrt.sim-model.persona-test
  "Persona (docs/sim-theory.edn's `:persona` stage, Milestone M4):
  seeded/pure sampling, the fixed-RNG-consumption law, and the
  age-linked payer co-landing invariant (docs/operational-models.md --
  Medicare dominant at 65+, only checkable once age is a real field)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-model.persona :as persona])
  (:import [java.util Random]))

(deftest persona-is-deterministic-for-a-fixed-seed
  (is (= (persona/persona (Random. 42) {})
         (persona/persona (Random. 42) {}))))

(defspec different-seeds-usually-differ 50
  (prop/for-all [seed gen/large-integer]
    (not= (persona/persona (Random. seed) {})
          (persona/persona (Random. (inc seed)) {}))))

(defspec every-sampled-persona-is-schema-valid 200
  (prop/for-all [seed gen/large-integer]
    (persona/valid-persona? (persona/persona (Random. seed) {}))))

(deftest ssn-uses-the-obviously-synthetic-never-issued-area-prefix
  (dotimes [seed 20]
    (is (clojure.string/starts-with? (:ssn (persona/persona (Random. seed) {})) "900-"))))

(deftest phone-and-ssn-match-their-formats
  (let [p (persona/persona (Random. 7) {})]
    (is (re-matches #"\d{3}-\d{3}-\d{4}" (:phone p)))
    (is (re-matches #"\d{3}-\d{2}-\d{4}" (:ssn p)))
    (is (re-matches #"\d{4}-\d{2}-\d{2}" (:dob p)))))

(defn- counting-random
  "A java.util.Random that counts every nextDouble/nextInt call made on
  it -- the direct way to test 'fixed consumption regardless of
  content' (counting actual method calls), rather than replaying a
  synthetic skip sequence that would have to independently guess
  persona's own mix of nextInt vs nextDouble calls to mean anything."
  [seed]
  (let [calls (atom 0)]
    {:calls calls
     :rng (proxy [Random] [(long seed)]
            (nextDouble []
              (swap! calls inc)
              (proxy-super nextDouble))
            (nextInt
              ([] (swap! calls inc) (proxy-super nextInt))
              ([n] (swap! calls inc) (proxy-super nextInt n))))}))

(defspec persona-consumes-a-fixed-number-of-rng-draws-regardless-of-content 100
  (testing "sim/ADR-0009's own fixed-consumption law, extended to Persona:
            13 draws, always, regardless of which age range (and
            therefore which decade bucket and payer pool) is
            configured -- content never changes draw COUNT"
    (prop/for-all [seed gen/large-integer
                   age-a (gen/choose 0 90)
                   age-b (gen/choose 0 90)]
      (let [[age-min age-max] (sort [age-a age-b])
            {:keys [rng calls]} (counting-random seed)]
        (persona/persona rng {:age-min age-min :age-max age-max})
        (= 13 @calls)))))

(deftest sixty-five-plus-personas-are-mostly-medicare
  (testing "docs/operational-models.md's age-linkage co-landing
            invariant: Medicare dominance at 65+, checkable now that
            age is a real sampled field"
    (let [payers (for [seed (range 300)]
                  (:type (:payer (persona/persona (Random. seed) {:age-min 65 :age-max 65}))))
          medicare-share (/ (count (filter #(= :medicare %) payers)) (double (count payers)))]
      (is (> medicare-share 0.7) (str "expected Medicare dominance at 65+, got " medicare-share)))))

(deftest under-65-personas-are-rarely-medicare
  (let [payers (for [seed (range 300)]
                (:type (:payer (persona/persona (Random. seed) {:age-min 20 :age-max 40}))))
        medicare-share (/ (count (filter #(= :medicare %) payers)) (double (count payers)))]
    (is (< medicare-share 0.15) (str "expected Medicare to be a small minority under 65, got " medicare-share))))

(deftest payer-never-carries-its-own-pool-weight
  (testing "the sampled payer is carried, rendered, never re-tracked -- no :weight leaking into patient/message-facing data"
    (is (not (contains? (:payer (persona/persona (Random. 1) {})) :weight)))))

;; --- GMF coverage Wave F (2026-08-03, ADR-0036 AR-4/AR-5): race/ses ----

(def ^:private race-weights [{:race "White" :weight 60.0} {:race "Black" :weight 40.0}])
(def ^:private ses-weights [{:category "High" :weight 30.0} {:category "Middle" :weight 70.0}])

(deftest race-and-socioeconomic-category-are-absent-with-no-config
  (let [p (persona/persona (Random. 1) {})]
    (is (not (contains? p :race)))
    (is (not (contains? p :socioeconomic-category)))))

(deftest race-and-socioeconomic-category-are-sampled-when-config-supplies-weights
  (let [p (persona/persona (Random. 1) {:race-weights race-weights :socioeconomic-weights ses-weights})]
    (is (contains? #{"White" "Black"} (:race p)))
    (is (contains? #{"High" "Middle"} (:socioeconomic-category p)))))

(deftest race-only-config-samples-race-without-socioeconomic-category
  (let [p (persona/persona (Random. 1) {:race-weights race-weights})]
    (is (contains? p :race))
    (is (not (contains? p :socioeconomic-category)))))

(deftest race-and-socioeconomic-category-are-conditionally-drawn
  (testing "AR-5's own identity hazard: absent config draws zero extra
            beyond the pre-existing 13; present config draws exactly 2
            more, 15 total -- the SAME direct method-call-counting
            technique the pre-existing fixed-consumption spec above
            uses, not a value-replay guess"
    (let [{:keys [rng calls]} (counting-random 1)]
      (persona/persona rng {})
      (is (= 13 @calls) "no config supplied -- byte-identical to every persona sampled before this ADR"))
    (let [{:keys [rng calls]} (counting-random 1)]
      (persona/persona rng {:race-weights race-weights :socioeconomic-weights ses-weights})
      (is (= 15 @calls) "both weights supplied -- exactly two more draws"))
    (let [{:keys [rng calls]} (counting-random 1)]
      (persona/persona rng {:race-weights race-weights})
      (is (= 14 @calls) "only :race-weights supplied -- exactly one more draw"))))

(defspec every-sampled-persona-with-race-and-ses-config-is-schema-valid 100
  (prop/for-all [seed gen/large-integer]
    (persona/valid-persona? (persona/persona (Random. seed) {:race-weights race-weights
                                                              :socioeconomic-weights ses-weights}))))
