(ns ehrt.sim-emit-hl7.vendored-med-rec-test
  "Vendoring batch 3 (2026-08-07, ADR-0072, AR-VB3-1/2): the full
  compile-trajectory/engine/emit round trip for `med_rec.json` -- a
  single-file closure (no CallSubmodule, no lookup tables, the census
  substance artifact's own `:closure-file-count 1`, `:event-counts
  [269 273 275]` across its own three-seed sample -- this batch's own
  highest-volume module). Population/horizon sizing follows this
  repo's own 'measure, don't guess' discipline
  (`ehrt.sim-emit-hl7.vendored-sepsis-test`'s own docstring), confirmed
  empirically this session (seed 20260802, 300 patients, a 100-year
  horizon)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private med-rec-json (slurp (io/resource "sim/modules/med_rec.json")))

(def ^:private loaded-closure
  (gmf/load-closure "med-rec" med-rec-json (constantly nil)))
(def ^:private med-rec-closure (:payload loaded-closure))

(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [med-rec-closure] :module-assignment [{:module-id "med-rec" :weight 1}]
   :module-horizon-days 36500})

(deftest engine-run-completes-real-med-rec-closure-content
  (testing "load-clean sanity -- single-file closure, no submodules"
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
