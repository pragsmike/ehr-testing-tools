(ns ehrt.conformance.sim-full-capability-gate-test
  "The FULL-CAPABILITY sibling of sim-gate-loop-test.clj (notes/ADRs.md
  ADR-0015): the legacy loop's own default pathway has never carried an
  :order step, a module assignment, or an outpatient encounter, so it
  cannot represent -- and, as sim's own capability keeps growing, cannot
  come to represent merely by rerunning it -- what `ehr gate` (v2 arm)
  makes of sim's ORM/ORU order-result cycle, M5b's module-driven
  outpatient trajectories, or churn's full trigger-code family, all
  landing in the SAME corpus. This loop runs
  projects/conformance/test-fixtures/sim-configs/full-capability.edn (an
  order-bearing CBC pathway for one cohort, an empty pathway plus the
  vendored sinusitis module for a second, disjoint cohort -- see that
  file's own header for why the two cohorts cannot share a patient
  population under this project's single-encounter-horizon invariant)
  through sim's `--config` passthrough, at --patients 60 and --churn on,
  the SAME gate-invocation shape (gate-dir -> judge.report/build-report)
  sim-gate-loop-test.clj already uses.

  Same assertion discipline as sim-gate-loop-test.clj: this test asserts
  the gate RUNS, produces a verdict per file, and the built report is
  well-formed. It does NOT assert all-pass and does NOT assert any file
  is rejected -- an ecological measurement, not a tautology, the same
  reasoning ADR-0013 gives for the legacy loop, extended here to a wider
  message-type breadth. The gate's report is compared against its own
  committed baseline
  (projects/conformance/test-fixtures/reports/sim-v2-full-capability-baseline.edn)
  via judge.report/diff-reports; a missing baseline is reported, not
  failed on. Runs unconditionally (ADR-0005): sim is an in-process
  mount now, never a sibling checkout that might be absent."
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.kernel.interface :as result]
            [ehrt.judge-v2-hapi.interface :as gate-v2]
            [ehrt.judge.interface :as report]
            [ehrt.conformance.sim-harness :as sim-harness]))

(def ^:private work-dir "target/sim-full-capability-gate")
(def ^:private baseline-path "projects/conformance/test-fixtures/reports/sim-v2-full-capability-baseline.edn")
(def ^:private config-path "projects/conformance/test-fixtures/sim-configs/full-capability.edn")

(defn- write-messages!
  "Same zero-padded, log-order file naming as sim-gate-loop-test.clj's
  own write-messages! -- gate-dir sorts by filename, so this keeps the
  report's file order deterministic."
  [dir messages]
  (.mkdirs (io/file dir))
  (dorun
   (map-indexed
    (fn [i message]
      (spit (io/file dir (format "msg-%03d.hl7" i)) message))
    messages)))

(deftest sim-v2-full-capability-gate-test
  (let [run-result (sim-harness/run! {:seed 42 :patients 60 :churn true :emit "hl7"
                                      :config config-path})]
    (when-not (result/ok? run-result)
      (throw (ex-info "sim-full-capability-gate: sim run failed" run-result)))
    (let [messages (:messages (:payload run-result))
          corpus-dir (str work-dir "/corpus")]
      (is (seq messages) "sanity: --emit hl7 produced at least one message")
      (write-messages! corpus-dir messages)
      (let [gate-result (gate-v2/gate-dir corpus-dir)]
        (is (result/ok? gate-result) "the gate ran to completion")
        (let [results (:results (:payload gate-result))
              current (report/build-report results {:gate :v2 :path corpus-dir
                                                     :source "ehr-testing-sim"
                                                     :seed 42 :patients 60 :churn true
                                                     :config config-path})]
          (is (= (count messages) (count results))
              "one verdict per emitted message")
          (is (report/report-valid? current)
              "the built report conforms to judge.report/Report")
          (if (.isFile (io/file baseline-path))
            (let [baseline (edn/read-string (slurp baseline-path))
                  diff (report/diff-reports baseline current)]
              (is (empty? (:files-added diff))
                  "same seed/config/churn should always emit the same file set")
              (is (empty? (:files-removed diff))
                  "same seed/config/churn should always emit the same file set")
              (println "SIM-FULL-CAPABILITY-GATE diff vs baseline:" (pr-str diff)))
            (println "SIM-FULL-CAPABILITY-GATE: no baseline yet at" baseline-path
                     "-- capture one from this run's report before relying on drift detection."))
          (println "SIM-FULL-CAPABILITY-GATE verdict totals:" (:totals current))
          (println "SIM-FULL-CAPABILITY-GATE by-code:" (:by-code current)))))))
