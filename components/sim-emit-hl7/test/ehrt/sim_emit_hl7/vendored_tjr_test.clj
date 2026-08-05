(ns ehrt.sim-emit-hl7.vendored-tjr-test
  "Engine closure-context session (2026-08-03, ADR-0033, J3 CLOSED): the
  full compile-trajectory/engine/emit round trip for `total_joint_
  replacement.json` -- a DIFFERENT engine-closure gap than the other
  two closures' own files documented (`ehrt.sim-emit-hl7.vendored-ear-
  infections-test`/`vendored-uti-test`'s own CallSubmodule throw):
  TJR's own `Joint_Replacement_Guard` needs `joint_replacement` seeded
  via `run-module`'s own `initial-attributes` arity (D2; H7's own
  rider; `ehrt.sim-trajectory.vendored-tjr-test`'s own docstring has
  the full delegated-content disclosure) or the walk blocks PERMANENTLY
  at age 0. This file's own previous version PINNED that silent-empty
  failure (`ehrt.sim-engine.engine`'s own `:registered` decide method called
  `run-module` at the bare 5-arity, which never reaches the
  `initial-attributes` parameter at all, and there was no config
  surface on `engine/run` to seed one). ADR-0033 AR-1 gives run-time
  config a `:module-initial-attributes`-shaped seed (attached per-entry
  directly on the closure here, the same direct-API shape a run-time
  caller not going through `ehrt.sim.run` uses); AR-3 wires
  `:registered` to thread it through. The seed value below is the SAME
  authored, provenance-cited value `ehrt.sim-trajectory.vendored-tjr-
  test` already supplies (D2 H7) -- reused, not re-derived."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private tjr-json (slurp (io/resource "sim/modules/total_joint_replacement.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def ^:private loaded-closure (gmf/load-closure "total-joint-replacement" tjr-json resolve-call-path))
(def ^:private tjr-closure (:payload loaded-closure))

;; D2 H7's own authored, provenance-cited seed (ehrt.sim-trajectory.
;; vendored-tjr-test's own docstring has the full delegated-content
;; disclosure: `joint_replacement` is set, in real Synthea, by two
;; sibling root modules this project does not vendor) -- reused
;; verbatim here, not re-derived.
(def ^:private seeded-closure (assoc tjr-closure :initial-attributes {:total-joint-replacement/joint-replacement "knee"}))

;; Small population, small horizon -- a round-trip proof, not a soak
;; test (J3). 60 years past DOB comfortably covers H4's own analytical
;; guard-jump (age 51) plus the post-op CarePlan cycle -- confirmed
;; empirically this session that the horizon size makes no difference
;; to the unseeded outcome, and is generous enough for the seeded one.
(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :module-assignment [{:module-id "total-joint-replacement" :weight 1}]
   :module-horizon-days 21900})

(deftest engine-run-produces-no-compiled-content-for-tjr-when-unseeded
  (testing "ADR-0033 AR-1's own no-validation ruling, disclosed not
            silently patched: :module-initial-attributes (or, at this
            direct-API layer, a closure's own :initial-attributes) is
            optional, and the engine invents nothing when it's absent
            -- every patient's :registered event still carries no
            compiled module content at all, because the Guard still
            blocks permanently at age 0 with joint_replacement unset.
            This is the SAME outcome this file's own previous version
            pinned as an engine GAP; ADR-0033 recharacterizes it as
            correct, documented behavior for an unseeded config, not a
            defect."
    (let [{:keys [ground-truth] :as result}
          (engine/run (assoc run-config :modules [tjr-closure]))
          kinds (into #{} (map :event) ground-truth)
          registered (filter #(= :registered (:event %)) ground-truth)]
      (is (= 300 (count ground-truth)) "one bare :registered event per patient, nothing else")
      (is (= #{:registered} kinds))
      (is (not-any? :pre-horizon-facts registered)
          "no patient's own history-phase content ever landed -- the walk never advanced past age 0")
      (testing "the invariant catalog holds trivially -- there is nothing to violate, not evidence the closure works"
        (is (result/ok? (check/check-all ground-truth (:facility result)))))
      (testing "no CarePlan (or any clinical) HL7 message is ever rendered -- there is no CarePlan event to be silent about here at all"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (empty? messages) "an all-:registered ground truth renders zero messages"))))))

(deftest engine-run-completes-real-tjr-closure-content-when-seeded
  (testing "load-clean sanity -- root plus all three called submodules"
    (is (result/ok? loaded-closure)))
  (testing "ADR-0033 (J3 closed): seeding joint_replacement via
            :initial-attributes lets the walk cross the compound Age
            guard (H4's own analytical jump, age 51) and land the full
            post-op CarePlan cycle for real, through the engine"
    (let [{:keys [ground-truth] :as result}
          (engine/run (assoc run-config :modules [seeded-closure]))
          kinds (into #{} (map :event) ground-truth)
          registered (filter #(= :registered (:event %)) ground-truth)
          care-plan-starts (filter #(= :care-plan-start (:event %)) ground-truth)]
      (is (some #{:care-plan-start :care-plan-end :medication-order} kinds)
          (str "expected real compiled clinical content across 300 patients, got " kinds))
      (is (seq care-plan-starts) "expected at least one Post_Op_CarePlan span across 300 patients")
      (is (some :pre-horizon-facts registered)
          "expected at least one patient's own history-phase content to ride :registered")
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full invariant catalog holds for a real closure-driven run")
      (testing "the closure's own non-CarePlan clinical content (MedicationOrder/Encounter/ConditionOnset,
                gmf-interpreter.md section 14's own state census) renders real HL7 -- CarePlan
                itself stays v2-silent by design (D2 R3), but it is not the only content this closure produces"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from this closure's real clinical content"))))))
