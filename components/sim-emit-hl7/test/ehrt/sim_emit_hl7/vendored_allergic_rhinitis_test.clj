(ns ehrt.sim-emit-hl7.vendored-allergic-rhinitis-test
  "Vendoring batch 2 (2026-08-07, ADR-0071, AR-VB2-1/2): the full
  compile-trajectory/engine/emit round trip for `allergic_rhinitis.json`
  -- a two-file closure (root plus a called medication submodule,
  `medications/otc_antihistamine.json`, the census substance artifact's
  own `:closure-file-count 2`, no lookup tables).

  Disclosed population deviation from this batch's own 300-patient
  convention (AR-VB2-2's own 'deviate per module only if content
  demands it, disclosed' clause): `Not_Atopic`'s own low onset odds
  (2.9%) land the eligible fraction in `Delay_Until_Early_Mid_Childhood`
  (2-6 years old); since `engine.clj` anchors `registration-t` at a
  FIXED calendar instant (`sim-model/reference-today-epoch-day`), not
  DOB (the batch-1 `dementia` docstring's own finding), that childhood
  onset lands POST-registration (real-time, message-rendering horizon
  content) only for the sliver of a Persona-sampled population (default
  age range 0-90) young enough at `reference-today-epoch-day` for the
  onset window to still be ahead of them -- confirmed empirically this
  session: at 300 patients, every one of the (rare) onsets landed
  PRE-registration (real compiled content, `:pre-horizon-facts` on
  `:registered`, but zero rendered HL7 -- v2 wire traffic has nothing to
  say about a fact already true before the run's own window opens,
  `ehrt.sim-emit-hl7.vendored-tjr-test`'s own unseeded-case precedent);
  at 3000 patients (10x), four patients' own onsets land post-
  registration, producing real `:outpatient-visit`/`:medication-order`
  ground truth AND four rendered HL7 messages -- satisfying AR-VB2-2's
  own 'nonzero trajectory events AND nonzero rendered messages'
  requirement for real, not merely a larger draw of the same pre-
  horizon-only outcome."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private allergic-rhinitis-json (slurp (io/resource "sim/modules/allergic_rhinitis.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def ^:private loaded-closure
  (gmf/load-closure "allergic-rhinitis" allergic-rhinitis-json resolve-call-path))
(def ^:private allergic-rhinitis-closure (:payload loaded-closure))

(def ^:private run-config
  {:seed 20260802 :patients 3000 :pathway {:name "module-only" :steps []}
   :modules [allergic-rhinitis-closure] :module-assignment [{:module-id "allergic-rhinitis" :weight 1}]
   :module-horizon-days 36500})

(deftest engine-run-completes-real-allergic-rhinitis-closure-content
  (testing "load-clean sanity -- root plus the called medication submodule"
    (is (result/ok? loaded-closure)))
  (testing "real compiled clinical content lands across 3000 patients (this closure's own low-incidence,
            fixed-registration-instant interaction, disclosed above)"
    (let [{:keys [ground-truth] :as result} (engine/run run-config)
          registered (filter #(= :registered (:event %)) ground-truth)
          kinds (into #{} (map :event) ground-truth)]
      (is (some :pre-horizon-facts registered)
          "expected at least one patient's own history-phase content (childhood onset, pre-dating the fixed registration instant) to ride :registered")
      (is (some #{:outpatient-visit :outpatient-visit-end :medication-order} kinds)
          (str "expected at least one patient's own onset to land post-registration (real-time ground truth), got " kinds))
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full invariant catalog holds for a real closure-driven run")
      (testing "post-registration content emitted inside the called medication submodule renders real HL7"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from real clinical content"))))))
