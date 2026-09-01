(ns ehrt.sim-emit-hl7.vendored-dementia-test
  "Vendoring batch 1 (2026-08-07, ADR-0070, AR-VB1-2/3): the full
  compile-trajectory/engine/emit round trip for `dementia.json` -- a
  single-file closure (no CallSubmodule, no lookup tables, the census
  substance artifact's own `:closure-file-count 1`). Population/horizon
  sizing follows this repo's own 'measure, don't guess' discipline
  (`ehrt.sim-emit-hl7.vendored-sepsis-test`'s own docstring): decide.clj
  anchors registration-t at a FIXED calendar instant
  (`sim-model/reference-today-epoch-day`), not DOB, so `Initial`'s own
  0.4/0.6 `AlzheimersGene` split plus the downstream onset delay only
  lands real content for the fraction of a sampled population whose
  virtual age at that fixed instant, plus horizon, actually reaches
  onset -- confirmed empirically this session (seed 20260802, 300
  patients, a 100-year horizon)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private dementia-json (slurp (io/resource "sim/modules/dementia.json")))

(def ^:private loaded-closure (gmf/load-closure "dementia" dementia-json (constantly nil)))
(def ^:private dementia-closure (:payload loaded-closure))

(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [dementia-closure] :module-assignment [{:module-id "dementia" :weight 1}]
   :module-horizon-days 36500})

(deftest engine-run-completes-real-dementia-closure-content
  (testing "load-clean sanity -- single-file closure, no submodules"
    (is (result/ok? loaded-closure)))
  (testing "real compiled clinical content lands across 300 patients"
    (let [{:keys [ground-truth] :as result} (run/run run-config)
          kinds (into #{} (map :event) ground-truth)]
      (is (some #{:condition-onset :encounter :encounter-end :medication-order} kinds)
          (str "expected real compiled clinical content across 300 patients, got " kinds))
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full invariant catalog holds for a real closure-driven run")
      (testing "real clinical content renders real HL7"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from real clinical content"))))))
