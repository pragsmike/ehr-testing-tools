(ns ehrt.sim-emit-hl7.vendored-metabolic-syndrome-care-test
  "Vendoring batch 3 (2026-08-07, ADR-0072, AR-VB3-1/2): the full
  compile-trajectory/engine/emit round trip for
  `metabolic_syndrome_care.json` -- this batch's own largest closure
  (root plus four called `metabolic_syndrome/` submodules --
  `medications.json`, `kidney_conditions.json`, `amputations.json`,
  `dme_supplies.json` -- plus the shared `anemia/anemia_sub.json`,
  re-verified byte-identical at this pin before reuse, no new NOTICE
  row for it; the census substance artifact's own
  `:closure-file-count 6` fresh-enumerated and confirmed EXACT this
  time, no CSVs anywhere in this closure). This is
  `metabolic_syndrome_disease.json`'s own sibling in the family the
  curation plan named 'the metabolic-syndrome pair' -- `_care` is the
  content-producer landed here (`:event-counts [139 139 139]`);
  `_disease` is `:zero-on-every-seed`, RECORDED not-vendorable-under-
  the-gate (ADR-0072). Population/horizon sizing follows this repo's
  own 'measure, don't guess' discipline
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

(def ^:private metabolic-syndrome-care-json (slurp (io/resource "sim/modules/metabolic_syndrome_care.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def ^:private loaded-closure
  (gmf/load-closure "metabolic-syndrome-care" metabolic-syndrome-care-json resolve-call-path))
(def ^:private metabolic-syndrome-care-closure (:payload loaded-closure))

(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [metabolic-syndrome-care-closure] :module-assignment [{:module-id "metabolic-syndrome-care" :weight 1}]
   :module-horizon-days 36500})

(deftest engine-run-completes-real-metabolic-syndrome-care-closure-content
  (testing "load-clean sanity -- root plus all four metabolic_syndrome/ submodules AND the shared anemia_sub"
    (is (result/ok? loaded-closure)))
  (testing "real compiled clinical content lands across 300 patients"
    (let [{:keys [ground-truth] :as result} (run/run run-config)
          kinds (into #{} (map :event) ground-truth)]
      (is (some #{:condition-onset :outpatient-visit :outpatient-visit-end :medication-order} kinds)
          (str "expected real compiled clinical content across 300 patients, got " kinds))
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full invariant catalog holds for a real closure-driven run")
      (testing "content emitted inside a called submodule renders real HL7"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from real clinical content"))))))
