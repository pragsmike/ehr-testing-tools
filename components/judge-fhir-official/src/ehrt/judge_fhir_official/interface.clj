(ns ehrt.judge-fhir-official.interface
  "The official HL7 FHIR validator engine's own delegation surface --
  extracted from ehrt.judge.interface (ADR-0011, the per-engine judge
  split; ehrt.judge.fhir -> ehrt.judge-fhir-official.fhir). Unqualified
  `gate-file`/`gate-dir`/`gate-batch`: the ehrt.judge.interface-era
  `fhir-gate-file`/`fhir-gate-dir`/`fhir-gate-batch` qualification
  existed only to disambiguate against ehrt.judge.v2's own
  `gate-file`/`gate-dir` at ONE shared interface (ADR-0002/ADR-0008) --
  now that each engine has its own interface, there is nothing left to
  qualify against here. ehrt.tools.interface re-applies its own
  `fhir-gate-file`/`fhir-gate-dir`/`fhir-gate-batch` qualification at
  its own re-export layer, unchanged, for zero behavior change to its
  downstream callers."
  (:require [ehrt.judge-fhir-official.fhir :as fhir]))

(def gate-file fhir/gate-file)
(def gate-dir fhir/gate-dir)
(def gate-batch fhir/gate-batch)
