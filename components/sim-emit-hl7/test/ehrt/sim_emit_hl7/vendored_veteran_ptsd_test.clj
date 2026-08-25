(ns ehrt.sim-emit-hl7.vendored-veteran-ptsd-test
  "Vendoring batch 4 (2026-08-08, ADR-0090, AR-VB4-1/2/3): the full
  compile-trajectory/engine/check/emit round trip for `veteran_ptsd.
  json` -- a single-file closure, no called submodule, no lookup
  table. Its own `Initial` state Delays 21 years then branches on the
  upstream `veteran` Person attribute (`is not nil`/`is nil`); seeded
  here via `:initial-attributes {:veteran-ptsd/veteran true}`, the real
  established precedent for generic Attribute-condition gates (ADR-0033
  AR-1, `total_joint_replacement.json`'s own `vendored_tjr_test.clj`) --
  NOT `:persona-config` (see `vendored_veteran_lung_cancer_test.clj`'s
  own docstring for the full disclosed correction).

  Two deftests: the engine round trip (real compiled content, a clean
  invariant-catalog pass, real rendered HL7, at three well-mixed seeds)
  and a pinned count of `:suppressed-straddle-spans` (ADR-0086,
  AR-SF-7) -- this module's own long Delay chain (16 Delay states) and
  recurring CarePlan/Encounter cycle trips the straddle fix's own
  suppression arm at a real, non-negligible rate across all three
  seeds tried, the conviction arc's own measured-then-pinned law."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private loaded-module
  (patient-simulator/load-module "veteran-ptsd" (slurp (io/resource "sim/modules/veteran_ptsd.json"))))

(def ^:private seeded-closure
  (assoc (patient-simulator/singleton-closure (:payload loaded-module))
         :initial-attributes {:veteran-ptsd/veteran true}))

(def ^:private gate-seeds [20260802 1 42])

(deftest engine-run-completes-real-veteran-ptsd-closure-content
  (testing "load-clean sanity"
    (is (result/ok? loaded-module)))
  (doseq [seed gate-seeds]
    (testing (str "seed " seed ": real compiled clinical content, veteran attribute seeded via :initial-attributes")
      (let [run-config {:seed seed :patients 300 :pathway {:name "module-only" :steps []}
                         :modules [seeded-closure]
                         :module-assignment [{:module-id "veteran-ptsd" :weight 1}]
                         :module-horizon-days 36500}
            {:keys [ground-truth facility providers]} (engine/run run-config)
            kinds (into #{} (map :event) ground-truth)]
        (is (some #{:care-plan-start :procedure} kinds)
            (str "expected real compiled clinical content across 300 patients, got " kinds))
        (is (result/ok? (check/check-all ground-truth facility))
            "the full invariant catalog holds")
        (testing "real clinical content renders real HL7"
          (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" facility providers)]
            (is (seq messages) "expected at least one HL7 message rendered from real clinical content")))))))

;; --- The straddle counter, pinned (measured, not assumed zero) --------

(def ^:private pinned-suppressed-straddle-spans
  "RE-BASELINED (2026-08-14, ADR-0133): `veteran_ptsd.json`'s own two
  collision pairs (`PHQ2_Q9 Assessment`/`PHQ2_Q9_Assessment`, `Columbia
  Suicide Risk Assessment`/`Columbia_Suicide_Risk_Assessment`),
  previously silently dropping one member of each, now load all four
  as real, distinct, correctly-routed states -- a declared oracle-
  change consequence, not a regression (the fix's own Step 1 census
  predicted this root MOVES). Restoring the previously-orphaned branch
  also revealed two further, now-resolved gaps this same session fixed
  under explicit author license: a `max-steps` false-positive in the
  therapy-visit recurring-care loop (`ehrt.patient-simulator.gmf-
  interpreter/consume-step-budget`, reset-on-any-advance semantics) and
  an unhandled `:virtual` encounter class in `compile-trajectory`'s own
  `encounter->step`/`encounter-end->step`. Old values (vendoring batch
  4, ADR-0090): {20260802 14, 1 6, 42 7}.

  RE-PINNED AGAIN (2026-08-25, ADR-0171): the RNG stream partition moved
  every walk, and seed 1 -- the only non-zero of the three -- went to 0,
  which would have left this gate pinning three absences. An all-zero pin
  proves the counter is READ, never that it can COUNT. Swept under the
  LIVE engine over eight seeds: 3 and 4 each suppress one span, the other
  six suppress none. Seed 3 replaces seed 1 as the non-zero witness; the
  two zero controls stay. Previous values: {20260802 0, 1 1, 42 0}.

  `gate-seeds` above deliberately does NOT follow -- seed 1 is still a
  good round-trip seed, it is only as a straddle witness that it went
  inert."
  {20260802 0, 3 1, 42 0})

(deftest suppressed-straddle-spans-is-pinned-per-seed
  (testing "the pin is not all zeros -- `R-witness-population-is-counted`
            applied to a counter, the same guard
            `vendored_veteran_prostate_cancer_test` now carries."
    (is (pos? (reduce + (vals pinned-suppressed-straddle-spans)))
        (str "every pinned straddle-span total is zero -- this gate has gone "
             "vacuous: " (pr-str pinned-suppressed-straddle-spans))))
  (doseq [[seed expected-total] pinned-suppressed-straddle-spans]
    (testing (str "seed " seed ": the straddle fix's own counter, pinned")
      (let [total (atom 0)
            real-compile-trajectory patient-simulator/compile-trajectory]
        (with-redefs [patient-simulator/compile-trajectory
                      (fn [trajectory facility reg-t & more]
                        (let [compiled (apply real-compile-trajectory trajectory facility reg-t more)]
                          (swap! total + (or (:suppressed-straddle-spans compiled) 0))
                          compiled))]
          (engine/run {:seed seed :patients 300 :pathway {:name "module-only" :steps []}
                       :modules [seeded-closure]
                       :module-assignment [{:module-id "veteran-ptsd" :weight 1}]
                       :module-horizon-days 36500}))
        (is (= expected-total @total)
            (str "expected " expected-total " total suppressed straddle spans across 300 patients, got " @total))))))
