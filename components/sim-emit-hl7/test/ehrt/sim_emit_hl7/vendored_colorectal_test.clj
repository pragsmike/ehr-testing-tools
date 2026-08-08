(ns ehrt.sim-emit-hl7.vendored-colorectal-test
  "Colorectal payoff (2026-08-08, ADR-0087, AR-CP-2): the full
  compile-trajectory/engine/check/emit round trip for
  `colorectal_cancer.json` -- a two-file closure sharing its called
  submodule, `anemia/anemia_sub.json`, with `hypothyroidism.json`'s own
  closure (vendored batch 2) and `anemia___unknown_etiology.json`'s own
  closure (vendored via the fidelity payoff, ADR-0083). Deferred whole
  at vendoring batch 3 (ADR-0072) on a diagnosis later overturned by a
  trajectory scan (ADR-0083, AR-FP-2): colorectal's own violations sat
  byte-identical before and after the EncounterEnd fix, its real
  blocker a separate, undiagnosed `:clinical-content-only-when-
  admitted`/`:discharge-follows-admission` defect one compile layer
  downstream. The colorectal investigation (ADR-0085) diagnosed that
  defect to `compile-trajectory`'s own legacy pre-horizon drop clauses:
  a straddling encounter (opened pre-horizon, closed and/or containing
  clinical content post-horizon) produced compiled clinical-content and
  discharge steps with no matching compiled admission. The straddle fix
  (ADR-0086) closed the gap structurally -- this test now pins the
  result at the SAME three seeds the investigation itself used
  (20260802, 1, 42), a result this test now pins as committed coverage.

  Unlike `anemia___unknown_etiology.json`'s own `Initial` state, this
  module's `Initial` state is NOT Race-gated (confirmed by inspection,
  ADR-0082) -- no `:persona-config` override needed, the plain default.

  Two deftests: the engine round trip (real compiled content, real
  invariant-catalog pass, real rendered HL7, at all three of the
  investigation's own seeds) and a pinned count of
  `:suppressed-straddle-spans` (ADR-0086, AR-SF-7) -- the straddle
  fix's own zero-cost counter -- across the SAME three-seed, 300-patient
  population the round trip above already exercises. Unlike the A5 arm's
  own interpreter-layer-only counter (`:suppressed-encounter-ends`,
  never surfaced by `engine/run`), `:suppressed-straddle-spans` lives on
  `compile-trajectory`'s own return map -- reachable through the SAME
  `engine/run` population via `with-redefs` interception at the
  `ehrt.sim-trajectory.interface/compile-trajectory` boundary (the exact
  technique the colorectal investigation itself used, ADR-0085 AR-CI-2)
  rather than a separately-constructed interpreter-layer walk sweep, so
  the counter is measured against the SAME real straddling patients the
  investigation traced by name (`PID-000239-c79b3f7f` at seed 20260802,
  `PID-000038-f5560829` at seed 42), not a synthetic re-sample that
  might miss them."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-trajectory.interface :as sim-trajectory]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private colorectal-json (slurp (io/resource "sim/modules/colorectal_cancer.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def ^:private loaded-closure
  (gmf/load-closure "colorectal-cancer" colorectal-json resolve-call-path))
(def ^:private colorectal-closure (:payload loaded-closure))

;; The investigation's own evidence parameters: the exact three seeds
;; ADR-0085 traced (violations at 20260802/42, clean at 1) and this
;; session's own live re-measurement post-fix (0/0/0 -- the straddle
;; fix having extinguished them, ADR-0086) -- never re-derived, cited
;; verbatim from both records.
(def ^:private deferral-seeds [20260802 1 42])

(deftest engine-run-completes-real-colorectal-closure-content
  (testing "load-clean sanity -- root plus the called anemia_sub submodule"
    (is (result/ok? loaded-closure)))
  (doseq [seed deferral-seeds]
    (testing (str "seed " seed ": real compiled clinical content, zero straddling-encounter violations")
      (let [run-config {:seed seed :patients 300 :pathway {:name "module-only" :steps []}
                         :modules [colorectal-closure]
                         :module-assignment [{:module-id "colorectal-cancer" :weight 1}]
                         :module-horizon-days 36500}
            {:keys [ground-truth facility providers] :as result} (engine/run run-config)
            kinds (into #{} (map :event) ground-truth)]
        (is (some #{:outpatient-visit :outpatient-visit-end :observation :procedure} kinds)
            (str "expected real compiled clinical content across 300 patients, got " kinds))
        (is (result/ok? (check/check-all ground-truth facility))
            "the full invariant catalog holds post-straddle-fix -- the investigation's own dangling-admission violations are gone")
        (testing "real clinical content renders real HL7"
          (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" facility providers)]
            (is (seq messages) "expected at least one HL7 message rendered from real clinical content")))))))

;; --- The straddle counter, pinned (AR-CP-2: "pinning this module's own
;; straddling patients forever") ----------------------------------------
;;
;; `:suppressed-straddle-spans` lives on `compile-trajectory`'s own
;; return map (ADR-0086, AR-SF-7) -- unlike the A5 arm's own
;; interpreter-layer-only `:suppressed-encounter-ends`, `engine/run`
;; DOES reach this layer for every patient (`engine.clj`'s own
;; `:registered` decide method calls `compile-trajectory` directly), so
;; interception at that one call boundary, across the SAME three-seed
;; 300-patient population the round trip above already exercises, is
;; both simpler and more faithful than a separately-constructed
;; interpreter-layer walk sweep: it counts spans against the SAME real
;; straddling patients the investigation traced by name, not a
;; synthetic re-sample that might miss the (rare, 2-of-900-patients)
;; branch entirely.

(def ^:private pinned-suppressed-straddle-spans
  {20260802 1, 1 0, 42 1})

(deftest suppressed-straddle-spans-is-pinned-per-seed
  (doseq [[seed expected-total] pinned-suppressed-straddle-spans]
    (testing (str "seed " seed ": the straddle fix's own counter, pinned")
      (let [total (atom 0)
            real-compile-trajectory sim-trajectory/compile-trajectory]
        (with-redefs [sim-trajectory/compile-trajectory
                      (fn [trajectory facility reg-t & more]
                        (let [compiled (apply real-compile-trajectory trajectory facility reg-t more)]
                          (swap! total + (or (:suppressed-straddle-spans compiled) 0))
                          compiled))]
          (engine/run {:seed seed :patients 300 :pathway {:name "module-only" :steps []}
                       :modules [colorectal-closure]
                       :module-assignment [{:module-id "colorectal-cancer" :weight 1}]
                       :module-horizon-days 36500}))
        (is (= expected-total @total)
            (str "expected " expected-total " total suppressed straddle spans across 300 patients, got " @total))))))
