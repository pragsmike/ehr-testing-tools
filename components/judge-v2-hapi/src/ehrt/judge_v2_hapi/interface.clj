(ns ehrt.judge-v2-hapi.interface
  "The HAPI-backed HL7 v2 base-structural engine's own delegation
  surface -- extracted from ehrt.judge.interface (ADR-0011, the
  per-engine judge split; ehrt.judge.v2 -> ehrt.judge-v2-hapi.v2).
  Unqualified `gate-file`/`gate-dir`: the ehrt.judge.interface-era
  `v2-gate-file`/`v2-gate-dir` qualification existed only to
  disambiguate against ehrt.judge.fhir's own `gate-file`/`gate-dir` at
  ONE shared interface (ADR-0002/ADR-0008) -- now that each engine has
  its own interface, there is nothing left to qualify against here.
  (The tools facade re-applied `v2-gate-file`/`v2-gate-dir` at its own
  re-export layer until stage 3 dissolved those relays -- consumers
  call this interface's own names directly now, ADR-0018.)"
  (:require [ehrt.judge-v2-hapi.v2 :as v2]))

(def gate-file v2/gate-file)
(def gate-dir v2/gate-dir)
