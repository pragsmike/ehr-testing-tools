(ns ehrt.conformance.sim-gate-loop-test
  "Claim #6's first motion (validation claim #6 in ehr-testing-sim's own
  problem statement -- fitness as a test instrument): sim-generated
  traffic judged by `ehr gate` (v2 arm), for real. Not a conformance
  verdict ON sim -- an ecological measurement of what this repo's
  base-structural v2 judge (judge.v2, HAPI's own default-validation
  parse plus DefaultValidator) makes of sim's own minimal wire output,
  the same gate-invocation shape contract_pairing_test.clj and
  baseline_gating_test.clj already use for the real FHIR validator
  (gate-dir -> judge.report/build-report), applied here to the v2 judge
  over a sim-generated corpus instead of a Synthea-generated one.

  Assertion discipline (read carefully, per this session's own prompt):
  this test asserts the gate RUNS, produces a verdict per file, and the
  built report is well-formed. It does NOT assert all-pass -- and, a
  measured fact rather than an assumption, it also does not assert any
  file is rejected. At seed 42/churn on/20 patients, EVERY message
  currently PASSES judge.v2: sim's v0 wire output is already
  well-formed enough at this base-structural tier (no conformance
  profile, no terminology in play here -- judge.v2's own docstring
  already notes there is little left for DefaultValidator to catch once
  HAPI's own parse-time primitive-type checking has run). That absence
  of rejections is itself the ecological finding this loop currently
  produces, not a test-authoring gap -- recorded honestly in the
  session's own triage block rather than papered over with a forced
  rejection.

  The gate's report is compared against a committed baseline artifact
  (projects/conformance/test-fixtures/reports/sim-v2-gate-baseline.edn, this
  repo's --baseline convention -- same shape as
  test-fixtures/reports/pre-split-baseline.edn) via
  judge.report/diff-reports, so a sim-side fix (or regression) shows up
  as a verdict delta here instead of a silent no-op; a missing baseline
  is reported, not failed on (bootstrapping this suite on a machine
  that hasn't captured one yet is not itself a defect). Runs
  unconditionally (ADR-0005): sim is an in-process mount now, never a
  sibling checkout that might be absent."
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.kernel.interface :as result]
            [ehrt.judge-v2-hapi.interface :as gate-v2]
            [ehrt.judge.interface :as report]
            [ehrt.conformance.sim-harness :as sim-harness]))

(def ^:private work-dir "target/sim-gate-loop")
(def ^:private baseline-path "projects/conformance/test-fixtures/reports/sim-v2-gate-baseline.edn")

(defn- write-messages!
  "Writes each ER7 message string to its own .hl7 file under dir,
  zero-padded and index-ordered so filenames sort in the same order
  sim's own :messages vector was emitted in (log order) -- gate-dir
  sorts by filename, so this keeps the report's file order
  deterministic and legible rather than an accident of lexical .hl7
  sorting on unpadded indices."
  [dir messages]
  (.mkdirs (io/file dir))
  (dorun
   (map-indexed
    (fn [i message]
      (spit (io/file dir (format "msg-%03d.hl7" i)) message))
    messages)))

(deftest sim-v2-gate-loop-test
  (let [run-result (sim-harness/run! {:seed 42 :patients 20 :churn true :emit "hl7"})]
    (when-not (result/ok? run-result)
      (throw (ex-info "sim-gate-loop: sim run failed" run-result)))
    (let [messages (:messages (:payload run-result))
          corpus-dir (str work-dir "/corpus")]
      (is (seq messages) "sanity: --emit hl7 produced at least one message")
      (write-messages! corpus-dir messages)
      (let [gate-result (gate-v2/gate-dir corpus-dir)]
        (is (result/ok? gate-result) "the gate ran to completion")
        (let [results (:results (:payload gate-result))
              current (report/build-report results {:gate :v2 :path corpus-dir
                                                     :source "ehr-testing-sim"
                                                     :seed 42 :patients 20 :churn true})]
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
              (println "SIM-GATE-LOOP diff vs baseline:" (pr-str diff)))
            (println "SIM-GATE-LOOP: no baseline yet at" baseline-path
                     "-- capture one from this run's report before relying on drift detection."))
          (println "SIM-GATE-LOOP verdict totals:" (:totals current))
          (println "SIM-GATE-LOOP by-code:" (:by-code current)))))))
