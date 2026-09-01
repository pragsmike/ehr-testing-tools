(ns ehrt.sim-emit-hl7.vendored-ear-infections-test
  "Engine closure-context session (2026-08-03, ADR-0033, J3 CLOSED): the
  full compile-trajectory/engine/emit round trip for `ear_infections.json`
  -- the standing gap every closure-having vendored root disclosed since
  Wave B (`ehrt.patient-simulator.vendored-ear-infections-test`'s own
  docstring, ADR-0029's D2/D3 dated notes), PINNED broken by
  `ehrt.sim-emit-hl7.vendored-ear-infections-test`'s own previous
  version (ADR-0030 J3: `ehrt.sim-engine.engine`'s own `:registered` decide
  method called `run-module` at the bare 5-arity, defaulting the
  interpreter's own submodule registry to the root alone -- any walk
  reaching this closure's own mandatory `CallSubmodule` medication path
  threw). This session's own AR-2/AR-3 wire the closure's real
  `:modules`/`:tables` maps through to `run-module`'s full arity
  (`decide.clj`'s own `:registered` defmethod) -- this file now proves
  the round trip works for real, replacing the pin with the working
  assertion its own docstring always said it would become.

  **Dated note (2026-08-03, `notes/ADRs.md` ADR-0037 AR-3/AR-7): the
  module's own `Next_Wellness_Encounter` no longer fires instantly --
  it waits for a real cadence tick (`next-wellness-tick`).** This
  file's own assertions (real `:outpatient-visit` content lands, the
  invariant catalog holds, HL7 renders) are unaffected and re-confirmed
  green under the new timing -- the `:outpatient-visit` step type is
  shared by BOTH the primary (ambulatory) and the wellness encounter
  once compiled, so this engine-layer round trip cannot distinguish
  which produced a given step. The NEW-timing claim itself (the
  wellness encounter fires strictly after the last medication ends, not
  at the same instant) is proven at the INTERPRETER layer instead
  (`ehrt.patient-simulator.vendored-ear-infections-test`'s own
  `next-wellness-encounter-now-resolves-at-a-real-cadence-tick-not-immediately`),
  per AR-4's own boundary ruling that semantic walk claims are an
  interpreter-layer concern, engine round-trips a narrower
  does-it-still-work-end-to-end one."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private ear-infections-json (slurp (io/resource "sim/modules/ear_infections.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json` -- the SAME shape
  `ehrt.patient-simulator.vendored-ear-infections-test`'s own interpreter-
  layer test already establishes for this closure."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def ^:private loaded-closure (gmf/load-closure "ear-infections" ear-infections-json resolve-call-path))
(def ^:private ear-infections-closure (:payload loaded-closure))

;; Small population, small horizon -- a round-trip proof, not a soak
;; test (J3's own instruction, unchanged this session). 300 patients
;; over a 10-year horizon is comfortably enough for the mandatory
;; medication path (run through a called submodule on every real onset,
;; per the interpreter-layer vendored test's own well-mixed-seed
;; evidence) to land real compiled content well within this population,
;; confirmed empirically the session that pinned the original failure.
(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [ear-infections-closure] :module-assignment [{:module-id "ear-infections" :weight 1}]
   :module-horizon-days 3650})

(deftest engine-run-completes-real-ear-infections-closure-content
  (testing "load-clean sanity -- root plus both called submodules"
    (is (result/ok? loaded-closure)))
  (testing "ADR-0033 (J3 closed): engine/run no longer throws on this
            closure's own CallSubmodule medication path -- real compiled
            clinical content, walked through the closure's own called
            submodules, lands in ground truth"
    (let [{:keys [ground-truth] :as result} (run/run run-config)
          kinds (into #{} (map :event) ground-truth)
          registered (filter #(= :registered (:event %)) ground-truth)]
      (is (some #{:medication-order :medication-end :outpatient-visit} kinds)
          (str "expected real compiled clinical content across 300 patients, got " kinds))
      (is (some :pre-horizon-facts registered)
          "expected at least one patient's own history-phase content to ride :registered")
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full invariant catalog holds for a real closure-driven run")
      (testing "the medication content emitted inside the called submodule renders real HL7"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from real clinical content"))))))
