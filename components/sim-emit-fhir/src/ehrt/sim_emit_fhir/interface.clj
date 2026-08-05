(ns ehrt.sim-emit-fhir.interface
  "Re-exports exactly what residual `sim`'s own src calls from outside
  this component -- determined by grep against `components/sim`'s own
  `run.clj`/`identifiers.clj` (sim split B, M3, `.agents/plans/2026-08-04-
  sim-split-b-plan.md`'s own AR-3 discipline, `notes/ADRs.md` ADR-0043).
  `snapshot-at` has NO real external caller (confirmed by that same
  grep -- its two mentions in `identifiers.clj` are docstring prose, not
  calls) and stays unexported, fully internal to this component; test-
  scope reaches it (and every other internal def) directly."
  (:require [ehrt.sim-emit-fhir.emit-fhir :as emit-fhir]))

(defn bundle-run
  [ground-truth reference-date utc-offset run-id t]
  (emit-fhir/bundle-run ground-truth reference-date utc-offset run-id t))
