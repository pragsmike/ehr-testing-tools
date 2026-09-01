(ns ehrt.sim-emit-hl7.vendored-anemia-test
  "Fidelity payoff (2026-08-08, ADR-0083, AR-FP-1): the full
  compile-trajectory/engine/emit round trip for `anemia___unknown_
  etiology.json` -- a two-file closure sharing its called submodule,
  `anemia/anemia_sub.json`, with `hypothyroidism.json`'s own closure
  (vendored batch 2, `vendored_hypothyroidism_test.clj`'s own sibling
  shape). Deferred whole at vendoring batch 2 (ADR-0071) on a real
  `gmf-interpreter` dangling-`:encounter-end` gap; closed by the
  EncounterEnd fix (ADR-0082) -- this module's own in-session proof
  there found ZERO violations at all three of the deferral's own seeds
  (20260802, 1, 42), a result this test now pins as committed coverage.

  This module's own `Initial` state is the first Race-gated branch this
  vendoring arc has landed (ADR-0071's own finding) -- `:persona-config
  {:race-weights [...]}` (the same shape `ehrt.patient-simulator.census/
  default-persona-config` already uses) is required, or `sim-model/
  persona` never assocs `:race` and the closure's own `race-condition-
  holds?` throws `honest-absence`.

  Two deftests: the engine round trip (real compiled content, real
  invariant-catalog pass, real rendered HL7, at all three of the
  deferral's own seeds) and a pinned interpreter-layer count of
  `:suppressed-encounter-ends` -- the A5 arm's own zero-cost counter
  (ADR-0082 R2) -- across a well-mixed-seed walk sweep, so a future
  regression in the no-op arm shows up as a moved integer, not a
  silent pass."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7])
  (:import [java.util Random]))

(def ^:private anemia-json (slurp (io/resource "sim/modules/anemia___unknown_etiology.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def ^:private loaded-closure
  (gmf/load-closure "anemia-unknown-etiology" anemia-json resolve-call-path))
(def ^:private anemia-closure (:payload loaded-closure))

;; ADR-0071's own fix: no prior vendored root ever read `:race`, so no
;; prior root's own test needed this key -- the same shape
;; `ehrt.patient-simulator.census/default-persona-config` already uses.
(def ^:private race-weighted-persona-config
  {:race-weights [{:race "White" :weight 1.0} {:race "Black" :weight 1.0}
                  {:race "Hispanic" :weight 1.0} {:race "Asian" :weight 1.0}
                  {:race "Native" :weight 1.0} {:race "Other" :weight 1.0}]})

;; The deferral's own evidence parameters: the exact three seeds
;; ADR-0071 recorded failing (12/17/6 violations) and ADR-0082's own
;; in-session proof recorded clean (0/0/0, the invariant catalog having
;; evolved slightly since 2026-08-07) -- never re-derived, cited
;; verbatim from both records.
(def ^:private deferral-seeds [20260802 1 42])

(deftest engine-run-completes-real-anemia-closure-content
  (testing "load-clean sanity -- root plus the called anemia_sub submodule"
    (is (result/ok? loaded-closure)))
  (doseq [seed deferral-seeds]
    (testing (str "seed " seed ": real compiled clinical content, zero dangling-encounter-end violations")
      (let [run-config {:seed seed :patients 300 :pathway {:name "module-only" :steps []}
                         :modules [anemia-closure]
                         :module-assignment [{:module-id "anemia-unknown-etiology" :weight 1}]
                         :module-horizon-days 36500
                         :persona-config race-weighted-persona-config}
            {:keys [ground-truth facility providers] :as result} (run/run run-config)
            kinds (into #{} (map :event) ground-truth)]
        (is (some #{:outpatient-visit :outpatient-visit-end :diagnostic-report :procedure} kinds)
            (str "expected real compiled clinical content across 300 patients, got " kinds))
        (is (result/ok? (check/check-all ground-truth facility))
            "the full invariant catalog holds post-EncounterEnd-fix -- the deferral's own dangling-discharge violations are gone")
        (testing "real clinical content renders real HL7"
          (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" facility providers)]
            (is (seq messages) "expected at least one HL7 message rendered from real clinical content")))))))

;; --- The A5 arm, pinned (AR-FP-1: "pinning the A5 arm's behavior
;; forever") -----------------------------------------------------------
;;
;; `engine/run` never surfaces `:suppressed-encounter-ends` (it lives on
;; the walk-level ctx `ehrt.patient-simulator.gmf-interpreter/run-module`
;; returns, one layer below the engine) -- so this counts it directly at
;; the interpreter layer, the same call shape
;; `ehrt.patient-simulator.census`'s own `walk-one` uses. 150 well-mixed
;; seeds (the established mixer-RNG pattern, `mixed-seeds` below,
;; reused verbatim from `vendored_hypothyroidism_test.clj`'s own oracle
;; sibling) x both sexes = 300 walks per mixer seed, registered 25
;; years post-DOB, a 100-year horizon -- real, empirically-run totals,
;; not estimated: mixer 20260802 -> 33 suppressed across 33 walks;
;; mixer 1 -> 23 across 23; mixer 42 -> 20 across 20 (every affected
;; walk suppresses exactly once at this population).

(defn- mixed-seeds [n mixer-seed]
  (let [mixer (Random. mixer-seed)]
    (repeatedly n #(.nextLong mixer))))

(defn- run-walk [seed sex reg-offset-years horizon-years]
  (let [p (assoc (sim-model/persona (Random. seed) race-weighted-persona-config) :sex sex)
        reg-t (+ (patient-simulator/dob-epoch-day p) (* 365 reg-offset-years))
        end-t (+ reg-t (* 365 horizon-years))
        root (get (:modules anemia-closure) "anemia-unknown-etiology")]
    (patient-simulator/run-module root (Random. seed) p reg-t end-t (:modules anemia-closure) {} {})))

(def ^:private pinned-suppressed-encounter-ends
  {20260802 33, 1 23, 42 20})

(deftest suppressed-encounter-ends-is-pinned-per-mixer-seed
  (doseq [[mixer-seed expected-total] pinned-suppressed-encounter-ends]
    (testing (str "mixer seed " mixer-seed ": the A5 no-op arm's own counter, pinned")
      (let [walks (for [seed (mixed-seeds 150 mixer-seed)
                         sex [:male :female]]
                     (run-walk seed sex 25 100))
            total (reduce + (map #(get % :suppressed-encounter-ends 0) walks))]
        (is (= expected-total total)
            (str "expected " expected-total " total suppressed :encounter-end emissions across 300 walks, got " total))))))
