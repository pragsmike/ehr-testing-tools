(ns ehrt.sim-check.interface
  "Public seam for `ehrt.sim-check` (sim split B, M4, `notes/ADRs.md`
  ADR-0043, `.agents/plans/2026-08-04-sim-split-b-plan.md`): the
  invariant catalog (`check`), extracted from `components/sim`.
  Contents are exactly the union of what residual sim's own src-scope
  callers (`interface.clj`'s own façade delegation, all four arities;
  `run.clj`'s 3-arity call) reach today, found by fresh call-position
  grep, not by interface-design judgment (the fat-component
  disclosure's own exception, ADR-0018's from-live-consumers
  precedent) -- test-scope callers repoint to this component's
  internal `check` namespace directly (Polylith permits reaching
  implementation from test), never through this seam."
  (:require [ehrt.sim-check.check :as check]))

(defn check-all
  ([ground-truth] (check/check-all ground-truth))
  ([ground-truth facility-config] (check/check-all ground-truth facility-config))
  ([ground-truth facility-config warm-up-seconds]
   (check/check-all ground-truth facility-config warm-up-seconds))
  ([ground-truth facility-config warm-up-seconds order-profiles-config]
   (check/check-all ground-truth facility-config warm-up-seconds order-profiles-config)))
