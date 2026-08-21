(ns ehrt.sim-emit-hl7.vendored-hypothyroidism-test
  "Vendoring batch 2 (2026-08-07, ADR-0071, AR-VB2-1/2): the full
  compile-trajectory/engine/emit round trip for `hypothyroidism.json` --
  a two-file closure (root plus a called submodule shared with the
  `anemia-unknown-etiology` closure, `anemia/anemia_sub.json`, the
  census substance artifact's own `:closure-file-count 2`, no lookup
  tables). Population/horizon sizing follows this repo's own 'measure,
  don't guess' discipline (`ehrt.sim-emit-hl7.vendored-sepsis-test`'s
  own docstring), confirmed empirically this session (seed 20260802,
  300 patients, a 100-year horizon)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private hypothyroidism-json (slurp (io/resource "sim/modules/hypothyroidism.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def ^:private loaded-closure
  (gmf/load-closure "hypothyroidism" hypothyroidism-json resolve-call-path))
(def ^:private hypothyroidism-closure (:payload loaded-closure))

(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [hypothyroidism-closure] :module-assignment [{:module-id "hypothyroidism" :weight 1}]
   :module-horizon-days 36500})

(deftest engine-run-completes-real-hypothyroidism-closure-content
  (testing "load-clean sanity -- root plus the called anemia_sub submodule"
    (is (result/ok? loaded-closure)))
  (testing "real compiled clinical content lands across 300 patients"
    (let [{:keys [ground-truth] :as result} (engine/run run-config)
          kinds (into #{} (map :event) ground-truth)]
      (is (some #{:condition-onset :outpatient-visit :outpatient-visit-end :medication-order} kinds)
          (str "expected real compiled clinical content across 300 patients, got " kinds))
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full invariant catalog holds for a real closure-driven run")
      (testing "real clinical content renders real HL7"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from real clinical content"))))))
