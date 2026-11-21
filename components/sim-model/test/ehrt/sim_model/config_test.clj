(ns ehrt.sim-model.config-test
  "components/sim/docs/operational-models.md's config schemas, defaults, and the
  synthetic-NPI Luhn math (sim/ADR-0007 decision (a): Luhn-valid, not
  obviously-fake). Written before ehrt.sim-model.config exists
  (sim/ADR-0004 test-first)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-model.config :as config])
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
  (testing "one ED ward and two inpatient wards, per components/sim/docs/operational-models.md"
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

;; --- ADR-0109: LatencyProfile -----------------------------------------

(deftest latency-profile-validates-per-event-type-minute-ranges
  (is (config/valid-latency-profile? {:admission {:from-minutes 5 :to-minutes 45}}))
  (testing "fractional minutes are legal (bare number?, not :int)"
    (is (config/valid-latency-profile? {:result-available {:from-minutes 0.5 :to-minutes 2.5}})))
  (testing "the empty profile validates -- the identity input"
    (is (config/valid-latency-profile? {})))
  (testing "a non-keyword event-type key is invalid"
    (is (not (config/valid-latency-profile? {"admission" {:from-minutes 5 :to-minutes 45}}))))
  (testing "a range missing either bound is invalid"
    (is (not (config/valid-latency-profile? {:admission {:from-minutes 5}})))))

;; --- ADR-0189: a ward's pharmacy turnaround -----------------------------

(defn- ward-with [range]
  (assoc (first (:wards config/default-facility)) :dispense-minutes range))

(deftest dispense-minutes-is-the-wards-own-range-or-its-class-default
  (is (= [5 25] (:ed config/default-dispense-minutes)))
  (is (= [15 60] (:inpatient config/default-dispense-minutes)))
  (doseq [ward (:wards config/default-facility)]
    (is (= (get config/default-dispense-minutes (:class ward)) (config/dispense-minutes ward))
        "a ward that states none reads its class default"))
  (is (= [3 9] (config/dispense-minutes (ward-with [3 9])))))

(deftest an-invalid-dispense-range-is-refused-by-the-ward-schema
  (let [facility (fn [range] (assoc config/default-facility :wards [(ward-with range)]))]
    (is (config/valid-facility? (facility [10 30])))
    (is (config/valid-facility? (facility [0 0])) "a fixed, immediate turnaround is a range")
    (is (config/valid-facility? config/default-facility) "and the key is optional")
    (doseq [bad [[30 10] [-1 5] [5] [5 10 15] ["5" "10"] [1.5 2.5] 20 nil]]
      (is (not (config/valid-facility? (facility bad))) (pr-str bad)))))

;; --- ADR-0190: the community pharmacy's two options ---------------------

(deftest community-fill-options-are-the-facilitys-own-or-the-defaults
  (is (= 0.80 config/default-community-fill-share))
  (is (= [60 10080] config/default-community-fill-minutes))
  (is (= 0.80 (config/community-fill-share config/default-facility))
      "a facility that states none reads the default")
  (is (= [60 10080] (config/community-fill-minutes config/default-facility)))
  (is (= 0.5 (config/community-fill-share (assoc config/default-facility :community-fill-share 0.5))))
  (is (= 0 (config/community-fill-share (assoc config/default-facility :community-fill-share 0)))
      "a stated zero is zero, not the default")
  (is (= [5 9] (config/community-fill-minutes (assoc config/default-facility :community-fill-minutes [5 9])))))

(deftest an-invalid-community-fill-option-is-refused-by-the-facility-schema
  (let [with (fn [k v] (assoc config/default-facility k v))]
    (is (config/valid-facility? config/default-facility) "both keys are optional")
    (doseq [good [0 1 0.0 1.0 0.8]]
      (is (config/valid-facility? (with :community-fill-share good)) (pr-str good)))
    (doseq [bad [-0.1 1.01 2 "0.8" [0.8] nil]]
      (is (not (config/valid-facility? (with :community-fill-share bad))) (pr-str bad)))
    (is (config/valid-facility? (with :community-fill-minutes [60 10080])))
    (is (config/valid-facility? (with :community-fill-minutes [0 0])) "a fill at the close is a range")
    (doseq [bad [[30 10] [-1 5] [5] [5 10 15] ["5" "10"] [1.5 2.5] 20 nil]]
      (is (not (config/valid-facility? (with :community-fill-minutes bad))) (pr-str bad)))))
