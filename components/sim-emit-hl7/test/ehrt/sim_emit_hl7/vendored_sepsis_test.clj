(ns ehrt.sim-emit-hl7.vendored-sepsis-test
  "GMF coverage Wave D stage D1 payoff (2026-08-02, ADR-0029): the full
  engine/check/emit round trip for sepsis.json (resources/modules/
  sepsis.json), the same 'vendored'-module-shaped wiring test
  ehrt.sim-engine.engine-test's own sinusitis-module/death-fixture tests
  already establish (docs/gmf-interpreter.md section 12) -- interpreter-
  layer coverage (load-clean, the diagnostic-report emission itself,
  determinism) is ehrt.sim-trajectory.vendored-sepsis-test.

  Population/horizon sizing, empirically measured this session (the
  same 'measure, don't guess' discipline the death-fixture test's own
  docstring already establishes): engine.clj's own :registered event
  anchors registration-t at a FIXED calendar instant
  (sim-model/reference-today-epoch-day), not DOB -- sepsis's own onset
  gate (Age_Guard >= 18 years, then a 2-40 year Delay, D1a-1) means real
  onset content lands anywhere from ~20 to ~58 years of virtual age, so
  only a fraction of a randomly-sampled population has both (a) not yet
  onset relative to their own sampled age at the fixed registration
  instant and (b) a large enough :module-horizon-days to still catch it
  -- confirmed empirically this session (seed 20260802, 500 patients,
  a 100-year horizon: 31 :diagnostic-report events, 14 carrying a
  :value-code child, 14 table-sourced :observation events, 97 ORU
  messages total)."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]
            [com.nervestaple.hl7-parser.parser :as parser]
            [com.nervestaple.hl7-parser.message :as message]))

(def ^:private sepsis-module
  (:payload (gmf/load-module "sepsis" (slurp (clojure.java.io/resource "sim/modules/sepsis.json")))))

;; A large-enough population and horizon for the fixed-registration-anchor
;; interaction this namespace's own docstring measures -- 500/36500 days
;; (100 years) reliably yields dozens of :diagnostic-report events for
;; this seed; smaller values were tried first and under-yielded, per the
;; same empirical-tuning discipline death_fixture_test.clj's own
;; docstring records for its own module.
(def ^:private run-config
  {:seed 20260802 :patients 500 :pathway {:name "module-only" :steps []}
   :modules [(gmf/singleton-closure sepsis-module)] :module-assignment [{:module-id "sepsis" :weight 1}]
   :module-horizon-days 36500})

(deftest a-run-with-the-sepsis-module-configured-produces-diagnostic-report-content-for-real
  (let [{:keys [ground-truth] :as result} (engine/run run-config)
        dr-events (filter #(= :diagnostic-report (:event %)) ground-truth)
        value-code-dr-events (filter #(some :value-code (:observations %)) dr-events)
        table-sourced-observations (filter #(and (= :observation (:event %)) (:reference-range %) (:interpretation %))
                                           ground-truth)]
    (is (seq dr-events) "expected at least one :diagnostic-report event across 500 patients")
    (is (seq value-code-dr-events)
        "expected at least one :diagnostic-report event with a value_code-sourced child (Blood_Cultures)")
    (is (seq table-sourced-observations)
        "expected at least one vital_sign/table-sourced :observation event (Pulse_Oximetry)")
    (testing "the full invariant catalog holds for a real run -- clinical-
              content-only-when-admitted now covers :diagnostic-report too"
      (is (result/ok? (check/check-all ground-truth (:facility result)))))))

(deftest the-emitted-oru-for-a-diagnostic-report-event-is-structurally-correct
  (let [{:keys [ground-truth facility providers]} (engine/run run-config)
        dr-event (first (filter #(and (= :diagnostic-report (:event %)) (some :value-code (:observations %)))
                                ground-truth))
        messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" facility providers)
        control-id (emit-hl7/control-id-for dr-event)
        oru (first (filter #(re-find (re-pattern (str "\\Q" control-id "\\E")) %) messages))
        parsed (parser/parse oru)
        obx-segments (message/get-segments parsed "OBX")]
    (is (some? dr-event) "expected at least one value_code-carrying :diagnostic-report event")
    (is (some? oru) "expected a rendered ORU for that exact event, matched by its own MSH-10 control id")
    (testing "ORC-1: new order, present -- unlike :observation's own order-less shape (D1a-7)"
      (is (= "NW" (message/get-field-first-value parsed "ORC" 1))))
    (testing "OBR-4: the report-level codes, Blood_Cultures' own panel LOINC"
      (is (= "600-7^Bacteria identified in Blood by Culture^LN" (message/get-field-first-value parsed "OBR" 4))))
    (testing "one OBX per embedded child (D1a-2: never a reference to resolve)"
      (is (= (count (:observations dr-event)) (count obx-segments))))
    (testing "the value_code-sourced child's own OBX: CWE segment, SNOMED CT coding-system abbreviation"
      (is (= "CWE" (message/get-field-first-value parsed "OBX" 2)))
      (is (= "10828004^Positive (qualifier value)^SCT" (message/get-field-first-value parsed "OBX" 5))))))

(deftest emission-is-deterministic-for-the-same-seed
  (let [r1 (engine/run run-config)
        r2 (engine/run run-config)
        emit1 (emit-hl7/emit (:ground-truth r1) "2024-01-01" "+00:00" (:facility r1) (:providers r1))
        emit2 (emit-hl7/emit (:ground-truth r2) "2024-01-01" "+00:00" (:facility r2) (:providers r2))]
    (is (= emit1 emit2))))
