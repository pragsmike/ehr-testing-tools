(ns ehrt.judge.interface
  "Judge's own delegation surface, extracted from ehrt.tools.interface
  (ADR-0002 R14 named hole H4; ADR-0008). Sized by grep of actual
  external callers -- tools' own check.clj/check_test.clj (report
  aggregation, finding/report validity) and, transitively, the CLI via
  ehrt.tools.interface's own re-export.

  gate-file/gate-dir collide across judge.v2 and judge.fhir the same
  way they did inside ehrt.tools.interface before this extraction
  (ADR-0002) -- qualified v2-/fhir- here too, not re-solved
  differently now that they live in their own component.
  `report-valid?`/`finding-valid?` are qualified from the start (unlike
  ehrt.tools.interface's own pre-extraction `valid?`/`report-valid?`
  split, where only one side needed qualifying against `result/valid?`)
  because both collide with EACH OTHER now that result's own `valid?`
  has left this component entirely -- no unqualified winner to pick."
  (:require [ehrt.judge.v2 :as v2]
            [ehrt.judge.fhir :as fhir]
            [ehrt.judge.report :as report]
            [ehrt.judge.finding :as finding]))

;; judge.v2 (collides with judge.fhir on gate-file/gate-dir -- qualified v2-*)
(def v2-gate-file v2/gate-file)
(def v2-gate-dir v2/gate-dir)

;; judge.fhir (collides with judge.v2 on gate-file/gate-dir -- qualified fhir-*)
(def fhir-gate-file fhir/gate-file)
(def fhir-gate-dir fhir/gate-dir)
(def fhir-gate-batch fhir/gate-batch)

;; judge.report (collides with judge.finding on valid? -- qualified report-*)
(def Report report/Report)
(def baseline-relative-report report/baseline-relative-report)
(def build-report report/build-report)
(def diff-reports report/diff-reports)
(def report-valid? report/valid?)

;; judge.finding (collides with judge.report on valid? -- qualified finding-*)
(def finding-valid? finding/valid?)
