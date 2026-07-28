(ns ehrt.sim.config-test
  "docs/operational-models.md's config schemas, defaults, and the
  synthetic-NPI Luhn math (ADR-0007 decision (a): Luhn-valid, not
  obviously-fake). Written before ehrt.sim.config exists
  (ADR-0004 test-first)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim.config :as config])
  (:import [java.util Random]))

(deftest npi-check-digit-hand-computed
  (testing "the NPI standard's check digit: Luhn's algorithm applied to
            the 14-digit string \"80840\" + the 9-digit body, doubling
            every digit at an even 0-based index counting from the
            RIGHT of that 14-digit string (the about-to-be-appended
            check digit will occupy the new rightmost/index-0 position).

            Hand computation for body \"123456789\" (the well-known
            example NPI 1234567893's own body):
              14-digit string: 80840123456789
              reversed:        9 8 7 6 5 4 3 2 1 0 4 8 0 8
              index:           0 1 2 3 4 5 6 7 8 9 10 11 12 13
              doubled@even-i:  9->18->9  8  7->14->5  6  5->10->1  4
                               3->6      2  1->2      0  4->8      8
                               0->0      8
              summed:          9+8+5+6+1+4+6+2+2+0+8+8+0+8 = 67
              check digit:     (10 - (67 mod 10)) mod 10 = (10-7) mod 10 = 3
            matching the well-known valid NPI 1234567893's own check
            digit."
    (is (= 3 (config/npi-check-digit "123456789")))))

(deftest valid-npi?-hand-computed
  (testing "the full 10-digit NPI from the hand computation above validates"
    (is (true? (config/valid-npi? "1234567893"))))
  (testing "flipping the check digit invalidates it"
    (is (false? (config/valid-npi? "1234567892")))))

(defspec generate-npi-is-always-luhn-valid 100
  (prop/for-all [seed gen/large-integer]
    (let [rng (Random. ^long seed)]
      (config/valid-npi? (config/generate-npi rng)))))

(defspec generate-npi-is-deterministic-in-the-rng-stream 100
  (prop/for-all [seed gen/large-integer]
    (= (config/generate-npi (Random. ^long seed))
       (config/generate-npi (Random. ^long seed)))))

(deftest default-facility-validates
  (is (config/valid-facility? config/default-facility))
  (testing "one ED ward and two inpatient wards, per docs/operational-models.md"
    (is (= 1 (count (filter #(= :ed (:class %)) (:wards config/default-facility)))))
    (is (= 2 (count (filter #(= :inpatient (:class %)) (:wards config/default-facility)))))))

(deftest default-provider-templates-validate
  (is (every? config/valid-provider-template? config/default-provider-templates)))

(deftest materialize-providers-assigns-valid-unique-npis
  (let [rng (Random. 42)
        providers (config/materialize-providers rng config/default-provider-templates)]
    (is (every? config/valid-provider? providers))
    (is (every? #(config/valid-npi? (:id %)) providers))
    (is (= (count providers) (count (distinct (map :id providers)))))))

(deftest materialize-providers-is-deterministic
  (is (= (config/materialize-providers (Random. 7) config/default-provider-templates)
         (config/materialize-providers (Random. 7) config/default-provider-templates))))
