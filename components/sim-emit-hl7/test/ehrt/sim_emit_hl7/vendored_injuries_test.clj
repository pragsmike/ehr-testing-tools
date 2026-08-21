(ns ehrt.sim-emit-hl7.vendored-injuries-test
  "Injuries arc close (2026-08-11, notes/ADRs.md ADR-0107): the full
  compile-trajectory/engine/check/emit round trip for `injuries.json`'s
  own eight-file closure -- deferred WHOLE by ADR-0070 (2026-08-07, a
  `max-steps` exhaustion at `broken_jaw.json`'s own dangling
  `dental_referral` gate), re-deferred narrower by ADR-0106 (2026-08-11,
  a SEPARATE `nested :encounter` gap surfaced once ADR-0105 closed the
  max-steps leg), now lands: both legs closed (ADR-0105's horizon/
  budget fix, this arc's own ADR-0107 auto-close fix).

  No attribute gate on the root (`injuries.json`'s own `Initial` state
  direct-transitions to `Wait_For_Injury`, ADR-0106's own finding,
  re-confirmed here by the absence of any `:initial-attributes` seed
  below) -- unlike the veteran family's own `veteran` Person-attribute
  gate (`vendored_veteran_lung_cancer_test.clj`)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private injuries-json (slurp (io/resource "sim/modules/injuries.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`, the same shape
  `ehrt.patient-simulator.vendored-injuries-test`'s own interpreter-layer
  test already establishes for this closure."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def ^:private loaded-closure (gmf/load-closure "injuries" injuries-json resolve-call-path))
(def ^:private injuries-closure (:payload loaded-closure))

;; NOT the 36500-day (100-year) horizon most batch-4 round-trip tests
;; use -- `census.clj`'s own 50-year default-horizon-years (18250 days)
;; instead, matching every probe this arc has ever run this closure at
;; (ADR-0070/ADR-0105/ADR-0106). ADR-0106's own dated finding [C]
;; (`notes/adr/0106-*.md`) already named why: `broken_jaw.json`'s own
;; `Wait for Dental Visit` loop (mean ~4562 cycles to cross 50 years,
;; well under the interpreter's 10000-step budget) is "unreachable...
;; by concentration, not by design" at 50 years, but a 100-year horizon
;; needs mean ~9124 cycles (~18248 steps at 2 steps/cycle) -- OVER
;; budget, confirmed live: `engine/run` at 36500 days throws
;; `run-submodule exceeded max-steps` at `:wait-for-dental-visit`, a
;; real, disclosed boundary this session's own probe hit, not a defect
;; ADR-0107 needed to (or should) fix -- max-steps's own budget is a
;; deliberate backstop, not a promise every horizon is safe.
(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [injuries-closure] :module-assignment [{:module-id "injuries" :weight 1}]
   :module-horizon-days 18250})

(deftest engine-run-completes-real-injuries-closure-content
  (testing "load-clean sanity -- root plus all seven called submodules"
    (is (result/ok? loaded-closure)))
  (testing "both deferral legs closed: engine/run no longer throws on
            this closure's own max-steps (ADR-0105) or nested-encounter
            (ADR-0107) branches -- real compiled clinical content lands
            in ground truth across the whole population"
    (let [{:keys [ground-truth] :as result} (engine/run run-config)
          kinds (into #{} (map :event) ground-truth)]
      (is (some #{:encounter :encounter-end :condition-onset :medication-order :outpatient-visit} kinds)
          (str "expected real compiled clinical content across 300 patients, got " kinds))
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full invariant catalog holds for a real closure-driven run")
      (testing "the closure's own real content renders real HL7"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from real clinical content"))))))

;; --- The auto-close counter, pinned (measured, not assumed zero) ------
;;
;; `:synthesized-encounter-ends` lives on `run-module`'s own return ctx
;; (ADR-0107), not on `compile-trajectory`'s -- interception happens at
;; the `patient-simulator/run-module` boundary itself (`engine.clj`'s own
;; `:registered` decide method calls it directly, one walk per patient),
;; the same "intercept at the real call boundary" technique the
;; colorectal/veteran-prostate-cancer payoffs already established one
;; layer downstream, applied here one layer up.

(def ^:private pinned-synthesized-encounter-ends
  "ADR-0106's own arithmetic (2/120 well-mixed seeds at the direct-
  interpreter layer, ~1.7%) predicts firing at 300 patients with
  ~99.4% likelihood -- measured here at this test's own seed, not
  assumed.

  RE-BASELINED (2026-08-14, ADR-0133): `injuries.json`'s own two
  collision pairs (`End DME`/`End_DME`, `Postoperative Care`/
  `Postoperative_Care`), previously silently dropping one member of
  each, now load all four as real, distinct, correctly-routed states
  -- a declared oracle-change consequence, not a regression (the
  fix's own Step 1 census predicted this root MOVES). Old value: 4."
  5)

(deftest synthesized-encounter-ends-is-pinned-at-population-scale
  (testing "seed 20260802: the auto-close fix's own counter, pinned --
            the arc's own closing witness at engine-round-trip scale"
    (let [total (atom 0)
          real-run-module patient-simulator/run-module]
      (with-redefs [patient-simulator/run-module
                    (fn [module rng persona registration-t & more]
                      (let [ctx (apply real-run-module module rng persona registration-t more)]
                        (swap! total + (or (:synthesized-encounter-ends ctx) 0))
                        ctx))]
        (engine/run run-config))
      (is (= pinned-synthesized-encounter-ends @total)
          (str "expected " pinned-synthesized-encounter-ends
               " total synthesized encounter-ends across 300 patients, got " @total)))))
