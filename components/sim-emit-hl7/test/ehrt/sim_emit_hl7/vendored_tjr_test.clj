(ns ehrt.sim-emit-hl7.vendored-tjr-test
  "Post-Wave-D cleanup session (2026-08-02, ADR-0030 J3): the full
  compile-trajectory/engine/emit round trip for `total_joint_
  replacement.json` -- a DIFFERENT engine-closure gap than the other
  two closures' own files document (`ehrt.sim-emit-hl7.vendored-ear-
  infections-test`/`vendored-uti-test`'s own CallSubmodule throw):
  TJR's own `Joint_Replacement_Guard` needs `joint_replacement` seeded
  via `run-module`'s own `initial-attributes` arity (D2; H7's own
  rider; `ehrt.sim-trajectory.vendored-tjr-test`'s own docstring has
  the full delegated-content disclosure) or the walk blocks PERMANENTLY
  at age 0 -- `ehrt.sim.engine`'s own `:registered` decide method calls
  `run-module` at its bare 5-arity
  (`components/sim/src/ehrt/sim/engine.clj`), which has no
  `initial-attributes` slot at all (the 5-arity form never reaches the
  7-arity `initial-attributes` parameter). There is no config surface
  on `engine/run` to seed a walk-entry attribute today.

  Unlike the other two closures, this does NOT throw -- it fails
  SILENTLY: every patient's own compiled module content is empty
  (confirmed live this session, 300 patients: 300 `:registered` events,
  zero of any other kind, no `:pre-horizon-facts`). `ehrt.sim.check`'s
  own invariant catalog holds trivially (nothing to violate), and
  `ehrt.sim-emit-hl7.emit-hl7`'s own rendering emits nothing
  care-plan-related -- but NOT for the reason D2's own G3 disclosed
  silence assertion means (a real CarePlan event class producing zero
  HL7 messages by DESIGN, proven at the emit-hl7 unit-test layer
  against a synthetic fixture): here there is no CarePlan event at all
  to be silent about. Tests only, per J3: this file PINS the confirmed
  failure, not a fix."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim.engine :as engine]
            [ehrt.sim.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private tjr-json (slurp (io/resource "sim/modules/total_joint_replacement.json")))

(def ^:private tjr-module
  (:payload (gmf/load-module "total-joint-replacement" tjr-json)))

;; Small population, small horizon -- a round-trip proof, not a soak
;; test (J3). 60 years past DOB comfortably covers H4's own analytical
;; guard-jump (age 51) plus the post-op CarePlan cycle -- IF
;; joint_replacement were ever seeded, which engine/run has no way to
;; do; confirmed empirically this session that the horizon size makes
;; no difference to the outcome below.
(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [tjr-module] :module-assignment [{:module-id "total-joint-replacement" :weight 1}]
   :module-horizon-days 21900})

(deftest engine-run-produces-no-compiled-content-for-tjr
  (testing "PINS the confirmed engine gap (ADR-0030 J3) -- every
            patient's :registered event carries no compiled module
            content at all, because engine.clj has no way to seed
            joint_replacement and the Guard blocks permanently at age
            0. Expected to start FAILING once a future session wires
            engine.clj to carry initial-attributes through to
            run-module; update this test then, not leave it silently
            red."
    (let [{:keys [ground-truth] :as result} (engine/run run-config)
          kinds (into #{} (map :event) ground-truth)
          registered (filter #(= :registered (:event %)) ground-truth)]
      (is (= 300 (count ground-truth)) "one bare :registered event per patient, nothing else")
      (is (= #{:registered} kinds))
      (is (not-any? :pre-horizon-facts registered)
          "no patient's own history-phase content ever landed -- the walk never advanced past age 0")
      (testing "the invariant catalog holds trivially -- there is nothing to violate, not evidence the closure works"
        (is (result/ok? (check/check-all ground-truth (:facility result)))))
      (testing "no CarePlan (or any clinical) HL7 message is ever rendered -- NOT G3's own disclosed
                by-design silence (there is no CarePlan event to be silent about here at all)"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (empty? messages) "an all-:registered ground truth renders zero messages"))))))
