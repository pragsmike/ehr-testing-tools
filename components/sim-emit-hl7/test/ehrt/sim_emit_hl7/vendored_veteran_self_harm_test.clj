(ns ehrt.sim-emit-hl7.vendored-veteran-self-harm-test
  "Vendoring batch 4 (2026-08-08, ADR-0090, AR-VB4-1/2/3): the full
  compile-trajectory/engine/check/emit round trip for `veteran_self_
  harm.json` -- a two-file closure calling its own `veterans/veteran_
  suicide_probabilities.json` submodule (a genuinely new closure
  member, itself SetAttribute/Simple only -- no clinical content of its
  own, feeding probability attributes back to the caller). The root's
  own `Initial` state Guards directly on the upstream `veteran` Person
  attribute (`is not nil`); seeded here via `:initial-attributes
  {:veteran-self-harm/veteran true}`, the real established precedent
  for generic Attribute-condition gates (ADR-0033 AR-1,
  `total_joint_replacement.json`'s own `vendored_tjr_test.clj`) -- NOT
  `:persona-config` (see `vendored_veteran_lung_cancer_test.clj`'s own
  docstring for the full disclosed correction). Namespacing note: the
  called submodule's own `Attribute` conditions resolve under the
  SAME root-scoped namespace (`gmf_interpreter.clj`'s own `root-id`,
  D1) as the caller, so no separate seed is needed for the submodule."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private veteran-self-harm-json (slurp (io/resource "sim/modules/veteran_self_harm.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def ^:private loaded-closure
  (gmf/load-closure "veteran-self-harm" veteran-self-harm-json resolve-call-path))

(def ^:private seeded-closure
  (assoc (:payload loaded-closure) :initial-attributes {:veteran-self-harm/veteran true}))

;; This family's own 2-seed baseline (AR-VB4-1): clean at both, no
;; check-all violation, no gate flag.
;;
;; RE-DERIVED from [20260802 1] by ADR-0171's stream partition, with the
;; measurement that justifies it. `veteran_self_harm.json`'s clinical
;; content is genuinely RARE: swept under the LIVE engine at 300
;; patients, seeds 20260802, 1-10, 12, 20260825 and 71 all produce
;; `#{:registered}` and nothing else, while 11, 42 and 202 produce the
;; full `#{:registered :admission :procedure :discharge}` -- three of
;; seventeen. The two old gate seeds happened to land on content before
;; the reshuffle and no longer do, so the content assertion below went
;; red rather than going quiet, which is the assertion doing its job.
;;
;; This is NOT a coverage regression in the module: the same sweep shows
;; content still reachable at the same rate, and both replacement seeds
;; are check-all clean with no gate flag, exactly as AR-VB4-1 requires.
;; It IS a live reminder that a 2-seed baseline over a rare-event module
;; is one reshuffle away from vacuity in either direction -- the reason
;; this test asserts CONTENT and not merely cleanliness.
(def ^:private gate-seeds [11 42])

(deftest engine-run-completes-real-veteran-self-harm-closure-content
  (testing "load-clean sanity -- root plus the called veteran_suicide_probabilities submodule"
    (is (result/ok? loaded-closure)))
  (doseq [seed gate-seeds]
    (testing (str "seed " seed ": real compiled clinical content, veteran attribute seeded via :initial-attributes")
      (let [run-config {:seed seed :patients 300 :pathway {:name "module-only" :steps []}
                         :modules [seeded-closure]
                         :module-assignment [{:module-id "veteran-self-harm" :weight 1}]
                         :module-horizon-days 36500}
            {:keys [ground-truth facility providers]} (engine/run run-config)
            kinds (into #{} (map :event) ground-truth)]
        (is (some #{:admission :procedure :discharge} kinds)
            (str "expected real compiled clinical content across 300 patients, got " kinds))
        (is (result/ok? (check/check-all ground-truth facility))
            "the full invariant catalog holds")
        (testing "real clinical content renders real HL7"
          (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" facility providers)]
            (is (seq messages) "expected at least one HL7 message rendered from real clinical content")))))))
