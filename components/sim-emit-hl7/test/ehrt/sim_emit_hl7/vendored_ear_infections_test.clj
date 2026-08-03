(ns ehrt.sim-emit-hl7.vendored-ear-infections-test
  "Post-Wave-D cleanup session (2026-08-02, ADR-0030 J3): the full
  compile-trajectory/engine/emit round trip for `ear_infections.json`
  -- the standing gap every closure-having vendored root has disclosed
  since Wave B (`ehrt.sim-trajectory.vendored-ear-infections-test`'s own
  docstring, ADR-0029's D2/D3 dated notes) but never actually run,
  H6's own instruction ('a full engine/check run') never fulfilled
  until this session tried it for real.

  Tests only, per J3's own instruction: this round trip does NOT work
  today, and this file PINS the confirmed failure mode rather than
  patching the engine. `ehrt.sim.engine`'s own `:registered` decide
  method calls `ehrt.sim-trajectory.interface/run-module` at its
  5-arity (`components/sim/src/ehrt/sim/engine.clj`, the
  `:registered` defmethod) -- which defaults the interpreter's own
  `modules` (submodule registry) argument to `{(:id module) module}`,
  the ROOT ALONE, never the closure's own called submodules. There is
  no config surface on `engine/run` to supply a closure's submodule
  registry at all today -- `:modules` accepts a bare vector of
  already-loaded ROOT modules (`engine.clj`'s own docstring), nothing
  more. Any walk that reaches a `CallSubmodule` state therefore throws
  `call-submodule-step`'s own 'names a call-path missing from the
  resolved closure' `ex-info`
  (`ehrt.sim-trajectory.gmf-interpreter/call-submodule-step`) -- a
  real, load-bearing defect this closure's own real, mandatory content
  (`ear_infections.json`'s medication path runs entirely through two
  called submodules) trips reliably, confirmed live this session with
  a 300-patient population at the same seed/horizon this file pins.

  ESCALATION, not a fix: closing this gap is real interpreter-wiring
  work (`engine.clj` needs a way to carry a closure's own `modules`/
  `tables` maps alongside its root, mirroring what
  `ehrt.sim-trajectory.interface/run-module`'s own optional arities
  already support at the interpreter layer) outside this session's own
  ruled scope (ADR-0030 J3: 'tests only ... do not patch the engine
  under this prompt'). Named in the roadmap's own Deferred section."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim.engine :as engine]))

(def ^:private ear-infections-json (slurp (io/resource "sim/modules/ear_infections.json")))

(def ^:private ear-infections-module
  (:payload (gmf/load-module "ear-infections" ear-infections-json)))

;; Small population, small horizon -- a round-trip proof, not a soak
;; test (J3's own instruction). 300 patients over a 10-year horizon is
;; comfortably enough for the mandatory medication path (run through a
;; called submodule on every real onset, per the interpreter-layer
;; vendored test's own well-mixed-seed evidence) to trip the missing-
;; registry throw well within this population, confirmed empirically
;; this session.
(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [ear-infections-module] :module-assignment [{:module-id "ear-infections" :weight 1}]
   :module-horizon-days 3650})

(deftest engine-run-throws-on-ear-infections-callsubmodule-content
  (testing "PINS the confirmed engine gap (ADR-0030 J3) -- engine/run
            cannot drive this closure past its first CallSubmodule
            state, because it never threads the closure's own
            submodule registry through to the interpreter. This
            assertion is expected to start FAILING the moment a future
            session wires engine.clj to carry a closure's modules/
            tables maps -- that is the desired outcome, and this test
            must be updated (not silently left red) when it happens."
    (let [ex (try (engine/run run-config) (catch clojure.lang.ExceptionInfo e e))]
      (is (instance? clojure.lang.ExceptionInfo ex)
          "expected engine/run to throw for this closure, not complete")
      (is (re-find #"CallSubmodule names a call-path missing from the resolved closure"
                   (ex-message ex)))
      (is (= "ear-infections" (:caller (ex-data ex))))
      (is (contains? #{"medications/ear_infection_antibiotic" "medications/otc_pain_reliever"}
                     (:call-path (ex-data ex)))))))
