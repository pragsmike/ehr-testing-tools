(ns ehrt.sim-emit-hl7.vendored-asthma-test
  "Vendoring batch 1 (2026-08-07, ADR-0070, AR-VB1-2/3): the full
  compile-trajectory/engine/emit round trip for `asthma.json` -- a
  three-file closure (root plus two called medication submodules,
  `medications/emergency_inhaler.json`/`medications/maintenance_
  inhaler.json`, the census substance artifact's own
  `:closure-file-count 3`) whose real therapeutic content is entirely
  `lookup_table_transition`-driven (D3a/H2's own mechanism, eight
  product-distribution CSVs total across both submodules) -- this
  closure is this vendoring batch's own first NEW lookup-table-bearing
  root since the UTI closure (ADR-0029 D3). Population/horizon sizing
  follows this repo's own 'measure, don't guess' discipline
  (`ehrt.sim-emit-hl7.vendored-sepsis-test`'s own docstring), confirmed
  empirically this session (seed 20260802, 300 patients, a 100-year
  horizon)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private asthma-json (slurp (io/resource "sim/modules/asthma.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(defn- resolve-table-name
  "D3a/H2's own real caller shape -- a thin io/resource wrapper over
  `sim/modules/lookup_tables/<table-name>`."
  [table-name]
  (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))

(def ^:private loaded-closure
  (gmf/load-closure "asthma" asthma-json resolve-call-path resolve-table-name))
(def ^:private asthma-closure (:payload loaded-closure))

(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [asthma-closure] :module-assignment [{:module-id "asthma" :weight 1}]
   :module-horizon-days 36500})

(deftest engine-run-completes-real-asthma-closure-content
  (testing "load-clean sanity -- root plus both called medication submodules AND all eight lookup tables"
    (is (result/ok? loaded-closure)))
  (testing "real compiled clinical content lands across 300 patients"
    (let [{:keys [ground-truth] :as result} (engine/run run-config)
          kinds (into #{} (map :event) ground-truth)]
      (is (some #{:condition-onset :outpatient-visit :outpatient-visit-end :medication-order} kinds)
          (str "expected real compiled clinical content across 300 patients, got " kinds))
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full invariant catalog holds for a real closure-driven run")
      (testing "content emitted inside a called medication submodule (lookup-table-sourced) renders real HL7"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from real clinical content"))))))
