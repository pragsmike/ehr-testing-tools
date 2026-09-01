(ns ehrt.sim-emit-hl7.vendored-dermatitis-test
  "Vendoring batch 2 (2026-08-07, ADR-0071, AR-VB2-1/2): the full
  compile-trajectory/engine/emit round trip for `dermatitis.json` -- this
  batch's own largest closure (root plus six called Observation
  submodules -- `dermatitis/early_moderate_eczema_obs.json`,
  `dermatitis/early_severe_eczema_obs.json`,
  `dermatitis/mid_moderate_eczema_obs.json`,
  `dermatitis/mid_severe_eczema_obs.json`,
  `dermatitis/moderate_cd_obs.json`, `dermatitis/severe_cd_obs.json` --
  the census substance artifact's own `:closure-file-count 7`, no
  lookup tables). Population/horizon sizing follows this repo's own
  'measure, don't guess' discipline (`ehrt.sim-emit-hl7.vendored-sepsis-
  test`'s own docstring), confirmed empirically this session (seed
  20260802, 300 patients, a 100-year horizon)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private dermatitis-json (slurp (io/resource "sim/modules/dermatitis.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def ^:private loaded-closure
  (gmf/load-closure "dermatitis" dermatitis-json resolve-call-path))
(def ^:private dermatitis-closure (:payload loaded-closure))

(def ^:private run-config
  ;; RE-SEEDED from 20260802 to 42 by ADR-0171's stream partition, with
  ;; the measurement that justifies it. Unlike `veteran_self_harm`,
  ;; dermatitis content is COMMON: swept under the LIVE engine at 300
  ;; patients, nine of ten seeds tried (1, 2, 3, 5, 7, 11, 42, 71, 202)
  ;; produce real `:outpatient-visit`/`:outpatient-visit-end` content
  ;; and one to six HL7 messages -- and 20260802, this fixture's old
  ;; seed, is now the single unlucky one, yielding
  ;; `#{:registered :care-plan-end}` and zero messages.
  ;;
  ;; 42 is taken because it is the RICHEST of the nine: seven event
  ;; kinds, including the `:medication-order`/`:medication-end` and
  ;; `:care-plan-start`/`:care-plan-end` pairs, and six rendered
  ;; messages -- so the Observation-submodule claim this test makes is
  ;; exercised further than it was before, not merely restored.
  {:seed 42 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [dermatitis-closure] :module-assignment [{:module-id "dermatitis" :weight 1}]
   :module-horizon-days 36500})

(deftest engine-run-completes-real-dermatitis-closure-content
  (testing "load-clean sanity -- root plus all six called Observation submodules"
    (is (result/ok? loaded-closure)))
  (testing "real compiled clinical content lands across 300 patients"
    (let [{:keys [ground-truth] :as result} (run/run run-config)
          kinds (into #{} (map :event) ground-truth)]
      (is (some #{:condition-onset :outpatient-visit :outpatient-visit-end} kinds)
          (str "expected real compiled clinical content across 300 patients, got " kinds))
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full invariant catalog holds for a real closure-driven run")
      (testing "content emitted inside a called Observation submodule renders real HL7"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from real clinical content"))))))
