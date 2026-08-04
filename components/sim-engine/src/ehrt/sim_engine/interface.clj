(ns ehrt.sim-engine.interface
  "Public seam for `ehrt.sim-engine` (sim split B, M2, `notes/ADRs.md`
  ADR-0043, `.agents/plans/2026-08-04-sim-split-b-plan.md`): the
  discrete-event simulation core (`engine`) plus its two catalytic
  config namespaces (`churn`'s InjectChurn, `order-profiles`),
  extracted from `components/sim`. Contents are exactly the union of
  what residual sim's own src-scope callers (`run`, `check`,
  `emit-state`, `identifiers`) reach today, found by fresh grep, not by
  interface-design judgment (the fat-component disclosure's own
  exception, ADR-0018's from-live-consumers precedent) -- test-scope
  callers repoint to this component's internal namespaces directly
  (Polylith permits reaching implementation from test), never through
  this seam.

  Step 1 landing (leaf slice, churn + order-profiles only -- engine.clj
  itself, and the vars it alone needs, move in Step 2): the
  orchestration surface (run.clj's own churn wiring) and the acceptance
  surface (check.clj's own order-profiles defaults). `inject` and
  `sample-analyte-value` are here ONLY as a transitional accommodation
  for residual sim's own `engine.clj`, which reaches this component's
  churn/order-profiles from src scope for this one commit before moving
  itself in Step 2 (AR-M2-1) -- re-derived (and, here, removed) once
  that move lands, since no OTHER src-scope caller outside this
  component ever needed them."
  (:require [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.order-profiles :as order-profiles]))

;; --- orchestration surface (run.clj's own churn wiring) -------------------

(def default-churn-profile churn/default-churn-profile)
(def sample-profile churn/sample-profile)

;; --- acceptance surface (check.clj's own order-profiles defaults) ---------

(def default-profiles order-profiles/default-profiles)
(def abnormal-flag order-profiles/abnormal-flag)

;; --- transitional only: residual sim's engine.clj, until Step 2 moves it
;; into this component and these two entries are removed again (see the
;; namespace docstring's own note) ------------------------------------------

(def inject churn/inject)
(def sample-analyte-value order-profiles/sample-analyte-value)
