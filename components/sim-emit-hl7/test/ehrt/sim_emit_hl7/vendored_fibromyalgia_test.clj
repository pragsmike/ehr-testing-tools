(ns ehrt.sim-emit-hl7.vendored-fibromyalgia-test
  "Vendoring batch 1 (2026-08-07, ADR-0070, AR-VB1-2/3): the full
  compile-trajectory/engine/emit round trip for `fibromyalgia.json` -- a
  single-file closure (no CallSubmodule, no lookup tables, the census
  substance artifact's own `:closure-file-count 1`); its own `Guard`
  state (`Fibromyalgia_Guard`, a bare `Date >= 1987` allow-condition)
  needs no external attribute seeding, unlike `total_joint_
  replacement.json`'s own `Joint_Replacement_Guard`. Population/horizon
  sizing follows this repo's own 'measure, don't guess' discipline
  (`ehrt.sim-emit-hl7.vendored-sepsis-test`'s own docstring), confirmed
  empirically this session (seed 20260802, 300 patients, a 100-year
  horizon)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private fibromyalgia-json (slurp (io/resource "sim/modules/fibromyalgia.json")))

(def ^:private loaded-closure (gmf/load-closure "fibromyalgia" fibromyalgia-json (constantly nil)))
(def ^:private fibromyalgia-closure (:payload loaded-closure))

(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [fibromyalgia-closure] :module-assignment [{:module-id "fibromyalgia" :weight 1}]
   :module-horizon-days 36500})

(deftest engine-run-completes-real-fibromyalgia-closure-content
  (testing "load-clean sanity -- single-file closure, no submodules"
    (is (result/ok? loaded-closure)))
  (testing "real compiled clinical content lands across 300 patients"
    (let [{:keys [ground-truth] :as result} (run/run run-config)
          kinds (into #{} (map :event) ground-truth)]
      (is (some #{:condition-onset :outpatient-visit :outpatient-visit-end :medication-order} kinds)
          (str "expected real compiled clinical content across 300 patients, got " kinds))
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full invariant catalog holds for a real closure-driven run")
      (testing "real clinical content renders real HL7"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from real clinical content"))))))
