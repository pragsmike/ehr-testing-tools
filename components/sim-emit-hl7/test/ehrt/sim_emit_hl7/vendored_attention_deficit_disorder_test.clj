(ns ehrt.sim-emit-hl7.vendored-attention-deficit-disorder-test
  "Vendoring batch 2 (2026-08-07, ADR-0071, AR-VB2-1/2): the full
  compile-trajectory/engine/emit round trip for
  `attention_deficit_disorder.json` -- a single-file closure (no
  CallSubmodule, no lookup tables, the census substance artifact's own
  `:closure-file-count 1`). Population/horizon sizing follows this
  repo's own 'measure, don't guess' discipline
  (`ehrt.sim-emit-hl7.vendored-sepsis-test`'s own docstring), confirmed
  empirically this session (seed 20260802, 300 patients, a 100-year
  horizon).

  Disclosed finding (test-configuration correction, not a module or
  interpreter issue): this closure's own `Behavior_Therapy` loop can
  keep a patient's CarePlan/medication open for DECADES (95% per-
  iteration continuation odds), so for a real fraction of a 300-patient
  population the opening (pre-horizon, folded onto `:registered`'s own
  `:pre-horizon-facts`) and closing (post-horizon, a real `:steps`
  event) halves of the SAME CarePlan/medication straddle the fixed
  `registration-t` boundary -- exactly the case `ehrt.sim-engine.engine-
  test`'s own `history-mode-straddling-encounter-...` property exists
  for (Wave H pre-roll, `notes/ADRs.md` ADR-0042). Confirmed empirically
  this session: the bare run-config (no `:history`) trips `ehrt.sim-
  check.check`'s own `:medication-end-references-existing-order-and-
  follows-it-in-time` invariant for 1 of 300 patients (a stale
  reference into the pre-horizon-folded prefix `compile-trajectory`'s
  own non-history path drops); `:history true` (ADR-0042's own opt-in,
  every prior batch-1/batch-2 root ran without it because no prior
  closure's own content ever straddled the boundary) resolves it --
  green, 0 violations, real post-straddle content lands."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private adhd-json (slurp (io/resource "sim/modules/attention_deficit_disorder.json")))

(def ^:private loaded-closure
  (gmf/load-closure "attention-deficit-disorder" adhd-json (constantly nil)))
(def ^:private adhd-closure (:payload loaded-closure))

(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [adhd-closure] :module-assignment [{:module-id "attention-deficit-disorder" :weight 1}]
   :module-horizon-days 36500 :history true})

(deftest engine-run-completes-real-attention-deficit-disorder-closure-content
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
