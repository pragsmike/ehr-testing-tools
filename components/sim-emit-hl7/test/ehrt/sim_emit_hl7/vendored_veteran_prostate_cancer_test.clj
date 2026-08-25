(ns ehrt.sim-emit-hl7.vendored-veteran-prostate-cancer-test
  "Vendoring batch 4 (2026-08-08, ADR-0090, AR-VB4-1/2/3): the full
  compile-trajectory/engine/check/emit round trip for `veteran_
  prostate_cancer.json` -- a single-file closure, no called submodule,
  no lookup table. Its own `veteran_status` state gates on the
  upstream `veteran` Person attribute `is not nil`; seeded here via
  `:initial-attributes {:veteran-prostate-cancer/veteran true}`, the
  real established precedent for generic Attribute-condition gates
  (ADR-0033 AR-1, `total_joint_replacement.json`'s own `vendored_tjr_
  test.clj`) -- NOT `:persona-config`, which only reaches PERSONA-level
  condition types (see `vendored_veteran_lung_cancer_test.clj`'s own
  docstring for the full disclosed correction).

  Two deftests: the engine round trip (real compiled content, a clean
  invariant-catalog pass, real rendered HL7, at three well-mixed seeds)
  and a pinned count of `:suppressed-straddle-spans` (ADR-0086,
  AR-SF-7) -- this module's own real straddling patients trip the
  straddle fix's own suppression arm at a nonzero rate (measured, not
  assumed zero), the conviction arc's own measured-then-pinned law."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private loaded-module
  (patient-simulator/load-module "veteran-prostate-cancer" (slurp (io/resource "sim/modules/veteran_prostate_cancer.json"))))

(def ^:private seeded-closure
  (assoc (patient-simulator/singleton-closure (:payload loaded-module))
         :initial-attributes {:veteran-prostate-cancer/veteran true}))

(def ^:private gate-seeds [20260802 1 42])

(deftest engine-run-completes-real-veteran-prostate-cancer-closure-content
  (testing "load-clean sanity"
    (is (result/ok? loaded-module)))
  (doseq [seed gate-seeds]
    (testing (str "seed " seed ": real compiled clinical content, veteran attribute seeded via :initial-attributes")
      (let [run-config {:seed seed :patients 300 :pathway {:name "module-only" :steps []}
                         :modules [seeded-closure]
                         :module-assignment [{:module-id "veteran-prostate-cancer" :weight 1}]
                         :module-horizon-days 36500}
            {:keys [ground-truth facility providers]} (engine/run run-config)
            kinds (into #{} (map :event) ground-truth)]
        (is (some #{:diagnostic-report :medication-order :care-plan-start :procedure} kinds)
            (str "expected real compiled clinical content across 300 patients, got " kinds))
        (is (result/ok? (check/check-all ground-truth facility))
            "the full invariant catalog holds")
        (testing "real clinical content renders real HL7"
          (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" facility providers)]
            (is (seq messages) "expected at least one HL7 message rendered from real clinical content")))))))

;; --- The straddle counter, pinned (measured, not assumed zero) --------
;;
;; `:suppressed-straddle-spans` lives on `compile-trajectory`'s own
;; return map (ADR-0086, AR-SF-7); `engine.clj`'s own `:registered`
;; decide method calls `compile-trajectory` directly for every patient,
;; so interception at that boundary (the colorectal payoff's own
;; technique, ADR-0087 AR-CP-2) counts it against the SAME real
;; population the round trip above already exercises.

(def ^:private pinned-suppressed-straddle-spans
  "Seed -> the total this run suppresses. RE-PINNED by ADR-0171's stream
  partition, which moved every one of these numbers.

  It was `{20260802 2, 1 0, 42 0}`. Post-partition all three of those
  seeds count ZERO, which would have left this gate pinning nothing but
  absences -- an all-zero pin proves the counter is READ, never that it
  can COUNT. Swept under the LIVE engine over fifteen seeds: 2, 4, 6 and
  9 each suppress exactly one span, the other eleven suppress none, so
  the phenomenon is real and rare rather than gone. 20260802 is replaced
  by 2, the smallest non-zero seed; 1 and 42 stay as the zero controls
  they already were.

  `gate-seeds` above deliberately does NOT follow: 20260802 still
  produces real clinical content and is still a good round-trip seed --
  it is only as a straddle witness that it went inert."
  {2 1, 1 0, 42 0})

(deftest suppressed-straddle-spans-is-pinned-per-seed
  (testing "the pin is not all zeros -- `R-witness-population-is-counted`
            applied to a counter. A gate whose every pinned value is 0
            proves the counter is read, never that it can count, and
            ADR-0171's reshuffle put it one seed away from exactly that."
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
                       :module-assignment [{:module-id "veteran-prostate-cancer" :weight 1}]
                       :module-horizon-days 36500}))
        (is (= expected-total @total)
            (str "expected " expected-total " total suppressed straddle spans across 300 patients, got " @total))))))
