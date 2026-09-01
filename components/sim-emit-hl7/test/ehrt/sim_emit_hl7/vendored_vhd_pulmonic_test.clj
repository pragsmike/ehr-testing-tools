(ns ehrt.sim-emit-hl7.vendored-vhd-pulmonic-test
  "Vendoring batch 3 (2026-08-07, ADR-0072, AR-VB3-1/2): the full
  compile-trajectory/engine/emit round trip for `vhd_pulmonic.json` --
  a closure of root plus the called `heart/vhd_risks.json` submodule
  (shared with `vhd_tricuspid`, landed once, reused there) plus two
  lookup tables (`vhd_ps.csv`/`vhd_pr.csv`, `lookup_table_transition`-
  driven severity/prognosis content) -- the census substance artifact's
  own `:closure-file-count 2` UNDERCOUNTS this closure the same way
  `asthma.json`'s own batch-1 finding did (JSON-only metric, two CSVs
  not counted; fresh-enumerated here, not read off the artifact).
  `:event-counts [3 3 3]` -- the smallest content-producing closure
  vendored to date, invariant across all three census seeds.
  Population/horizon sizing follows this repo's own 'measure, don't
  guess' discipline (`ehrt.sim-emit-hl7.vendored-sepsis-test`'s own
  docstring), confirmed empirically this session (seed 20260802, 300
  patients, a 100-year horizon)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private vhd-pulmonic-json (slurp (io/resource "sim/modules/vhd_pulmonic.json")))

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
  (gmf/load-closure "vhd-pulmonic" vhd-pulmonic-json resolve-call-path resolve-table-name))
(def ^:private vhd-pulmonic-closure (:payload loaded-closure))

(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [vhd-pulmonic-closure] :module-assignment [{:module-id "vhd-pulmonic" :weight 1}]
   :module-horizon-days 36500})

(deftest engine-run-completes-real-vhd-pulmonic-closure-content
  (testing "load-clean sanity -- root plus the shared vhd_risks submodule AND both lookup tables"
    (is (result/ok? loaded-closure)))
  (testing "real compiled clinical content lands across 300 patients"
    (let [{:keys [ground-truth] :as result} (run/run run-config)
          kinds (into #{} (map :event) ground-truth)]
      (is (some #{:outpatient-visit :outpatient-visit-end} kinds)
          (str "expected real compiled clinical content across 300 patients, got " kinds))
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full invariant catalog holds for a real closure-driven run")
      (testing "real clinical content renders real HL7"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from real clinical content"))))))
