(ns ehrt.judge-v2-hapi.interface
  "The HAPI-backed HL7 v2 base-structural engine's own delegation
  surface -- extracted from ehrt.judge.interface (ADR-0011, the
  per-engine judge split; ehrt.judge.v2 -> ehrt.judge-v2-hapi.v2).
  Unqualified `gate-file`/`gate-dir`: the ehrt.judge.interface-era
  `v2-gate-file`/`v2-gate-dir` qualification existed only to
  disambiguate against ehrt.judge.fhir's own `gate-file`/`gate-dir` at
  ONE shared interface (ADR-0002/ADR-0008) -- now that each engine has
  its own interface, there is nothing left to qualify against here.
  ehrt.tools.interface re-applies its own `v2-gate-file`/`v2-gate-dir`
  qualification at its own re-export layer, unchanged, for zero
  behavior change to its downstream callers."
  (:require [ehrt.judge-v2-hapi.v2 :as v2]))

(def gate-file v2/gate-file)
(def gate-dir v2/gate-dir)
