(ns ehrt.sim-emit-hl7.vendored-veteran-lung-cancer-test
  "Vendoring batch 4 (2026-08-08, ADR-0090, AR-VB4-1/2/3): the full
  compile-trajectory/engine/check/emit round trip for `veteran_lung_
  cancer.json` -- a single-file closure, no called submodule, no
  lookup table. Its own `Initial` state direct-transitions to a
  `Guard` requiring the upstream `veteran` Person attribute `is not
  nil` -- this project's own census never sets that attribute (hence
  the 2026-08-03 wave-f census's own uniform 0-event `:blocked`
  verdict for this module, a stale prior map, not current evidence).

  Disclosed correction (NOTICE's own dated batch-4 entry has the full
  story): the session's driving prompt named `:persona-config` as the
  mechanism for attribute-gated modules; that mechanism only reaches
  PERSONA-level condition types (`Race`/`Socioeconomic`/`State`), never
  the generic `Attribute` condition type this module's own Guard uses.
  The real established precedent is `:initial-attributes` (ADR-0033
  AR-1, `total_joint_replacement.json`'s own `vendored_tjr_test.clj`) --
  a closure-level seed, root-namespaced exactly like every module-set
  workflow attribute (`gmf_interpreter.clj`'s own `attribute-condition-
  holds?` reads only `(:attributes ctx)`, keyed `<root-id>/<attribute>`,
  never the persona map)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.interface :as sim-trajectory]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private loaded-module
  (sim-trajectory/load-module "veteran-lung-cancer" (slurp (io/resource "sim/modules/veteran_lung_cancer.json"))))

(def ^:private seeded-closure
  (assoc (sim-trajectory/singleton-closure (:payload loaded-module))
         :initial-attributes {:veteran-lung-cancer/veteran true}))

;; This family's own 2-seed baseline (AR-VB4-1): clean at both, no
;; check-all violation, no gate flag -- the multi-seed-once-flagged law
;; (2-3 seeds, THREE only once flagged) does not fire here.
(def ^:private gate-seeds [20260802 1])

(deftest engine-run-completes-real-veteran-lung-cancer-closure-content
  (testing "load-clean sanity"
    (is (result/ok? loaded-module)))
  (doseq [seed gate-seeds]
    (testing (str "seed " seed ": real compiled clinical content, veteran attribute seeded via :initial-attributes")
      (let [run-config {:seed seed :patients 300 :pathway {:name "module-only" :steps []}
                         :modules [seeded-closure]
                         :module-assignment [{:module-id "veteran-lung-cancer" :weight 1}]
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
