(ns ehrt.sim-trajectory.vendored-sepsis-test
  "GMF coverage Wave D stage D1 payoff (2026-08-02, ADR-0029): the SIXTH
  real vendored module (resources/modules/sepsis.json), D1a's own
  characterization payoff (docs/gmf-interpreter.md section 11) -- a
  trivial one-file closure (zero CallSubmodule) whose only state-type
  gap was the observation family itself (MultiObservation x2,
  DiagnosticReport x1), closed by D1b's own implementation.

  Interpreter-layer coverage only -- the full engine/check/emit round
  trip (a real population run, the invariant catalog, and the emitted
  ORU's own structural shape) is
  ehrt.sim-emit-hl7.vendored-sepsis-test, the same layer split
  death_fixture_test.clj/engine_test.clj's own sinusitis-module wiring
  test already establish."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-trajectory.gmf-interpreter :as interp]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

(def sepsis-json (slurp (io/resource "sim/modules/sepsis.json")))

(deftest vendored-sepsis-loads-and-validates
  (let [loaded (gmf/load-module "sepsis" sepsis-json)]
    (is (result/ok? loaded)
        (str "expected the vendored module to validate against the v1 subset; got " (pr-str loaded)))
    (is (= :diagnostic-report (get-in (:payload loaded) [:states :blood-cultures :type])))
    (is (= :multi-observation (get-in (:payload loaded) [:states :record-blood-pressure :type])))))

(def ^:private sepsis (:payload (gmf/load-module "sepsis" sepsis-json)))

(defn- adult [seed] (assoc (sim-model/persona (Random. seed) {}) :sex :female))

;; registration-t = DOB (death-fixture-test.clj's own precedent): sepsis's
;; own onset gate (Age_Guard >= 18 years, THEN a 2-40 year Delay, D1a-1)
;; means real onset content can land anywhere from ~20 to ~58 years of
;; virtual age -- anchoring registration at DOB itself means NOTHING in
;; this walk is ever pre-horizon-dropped, regardless of where onset
;; actually lands, the same trick that test uses for the identical reason.
(defn- registration-t-for [persona] (interp/dob-epoch-day persona))

;; Age_Guard's own 18-year minimum plus Delay's own up-to-40-year range
;; (58 years) plus the clinical episode itself -- 70 years is comfortably
;; generous, the same "generous, not tight" posture vendored_appendicitis_
;; test.clj's own horizon-window-days already establishes for an
;; analogous age-delay-then-onset shape.
(def ^:private horizon-window-days (* 365 70))

(defn- walk-result [seed]
  (let [p (adult seed)
        reg-t (registration-t-for p)]
    (interp/run-module sepsis (Random. seed) p reg-t (+ reg-t horizon-window-days))))

;; The established mixer-RNG pattern (vendored_appendicitis_test.clj,
;; reused verbatim across every GMF coverage wave since) -- sequential
;; small seeds are NOT well-distributed for java.util.Random's own first
;; draw.
(defn- well-mixed-candidate-seeds [n mixer-seed]
  (let [mixer (Random. mixer-seed)]
    (repeatedly n #(.nextLong mixer))))

(deftest vendored-sepsis-walks-to-terminal-without-throwing-for-many-seeds
  (doseq [seed (take 30 (well-mixed-candidate-seeds 30 20260802))]
    (is (contains? #{:terminal :horizon-complete} (:status (walk-result seed)))
        (str "seed " seed " should reach Terminal or the horizon bound, never throw or block"))))

;; Age_Guard's own DISTRIBUTED_TRANSITION off it (6% Delay/onset, 94%
;; Terminal, D1a-1's own transition-kind sweep) means finding a real
;; onset needs many candidate seeds, the same statistical-search shape
;; vendored_appendicitis_test.clj's own onset search already establishes.

(defn- reaches-blood-cultures? [result] (some #(= :blood-cultures (:state %)) (:trajectory result)))

(deftest at-least-one-seed-reaches-the-diagnostic-report-emission
  (testing "D1a-2's own reachability trace: Blood_Cultures (DiagnosticReport)
            fires exactly once, unconditionally, the FIRST state after
            Sepsis_ED_Encounter opens -- reached by 100% of sepsis-onset
            patients, so finding ANY onset seed suffices"
    (let [seed (first (keep (fn [seed] (when (reaches-blood-cultures? (walk-result seed)) seed))
                            (well-mixed-candidate-seeds 500 20260802)))]
      (is (some? seed) "expected at least one well-mixed candidate seed to reach onset")
      (let [{:keys [trajectory]} (walk-result seed)
            dr-event (first (filter #(= :diagnostic-report (:event %)) trajectory))]
        (is (= :diagnostic-report (:event dr-event)))
        (is (= [{:system :loinc :code "600-7" :display "Bacteria identified in Blood by Culture"}]
               (:codes dr-event)))
        (testing "the embedded child carries a value_code (D1a-2: children
                  are embedded, inline content, never a reference)"
          (is (= 1 (count (:observations dr-event))))
          (is (= {:system :snomed :code "10828004" :display "Positive (qualifier value)"}
                 (:value-code (first (:observations dr-event))))))
        (testing "the SAME mandatory onset path also carries Capillary_Refill
                  (value_code, standalone :observation) and Pulse_Oximetry
                  (vital_sign-sourced, table-backed, units/range/flag present)"
          (let [capillary-refill (first (filter #(= :capillary-refill (:state %)) trajectory))
                pulse-ox (first (filter #(= :pulse-oximetry (:state %)) trajectory))]
            (is (= {:system :snomed :code "50427001" :display "Increased capillary filling time (finding)"}
                   (:value-code capillary-refill)))
            (is (<= 95 (:value pulse-ox) 100))
            (is (= "%" (:unit pulse-ox)))
            (is (= {:low 95 :high 100} (:reference-range pulse-ox)))
            (is (= :normal (:interpretation pulse-ox)))))))))

(defn- reaches-record-blood-pressure-2? [result]
  (some #(= :record-blood-pressure-2 (:state %)) (:trajectory result)))

(deftest at-least-one-seed-reaches-both-multi-observation-value-mechanisms
  (testing "D1a-3's own real texture: the SAME clinical concept (blood
            pressure) authored via BOTH mechanisms side by side --
            Record_Blood_Pressure's children use range, Record_Blood_
            Pressure_2's use vital_sign, both reached on the ICU branch"
    (let [seed (first (keep (fn [seed] (when (reaches-record-blood-pressure-2? (walk-result seed)) seed))
                            (well-mixed-candidate-seeds 3000 777)))]
      (is (some? seed) "expected at least one well-mixed candidate seed to reach the ICU branch")
      (let [{:keys [trajectory]} (walk-result seed)
            rbp1 (first (filter #(= :record-blood-pressure (:state %)) trajectory))
            rbp2 (first (filter #(= :record-blood-pressure-2 (:state %)) trajectory))]
        (is (= :diagnostic-report (:event rbp1)))
        (is (= :diagnostic-report (:event rbp2)))
        (testing "Record_Blood_Pressure: both children range-sourced"
          (is (every? #(<= 40 (:value %) 120) (:observations rbp1))))
        (testing "Record_Blood_Pressure_2: both children vital_sign/table-sourced"
          (is (every? #(and (some? (:reference-range %)) (= :normal (:interpretation %))) (:observations rbp2))))))))

(deftest vendored-sepsis-determinism-holds-for-the-same-seed
  (let [seed 1234567890123]
    (is (= (walk-result seed) (walk-result seed)))))
