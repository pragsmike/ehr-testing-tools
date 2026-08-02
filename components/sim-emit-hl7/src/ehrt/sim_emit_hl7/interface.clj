(ns ehrt.sim-emit-hl7.interface
  "Re-exports exactly what residual `sim`'s own src and test trees call
  from outside this component -- determined by grep against
  `components/sim`'s own `identifiers.clj`/`run.clj` and its
  `churn_scenarios_test.clj`/`emit_state_test.clj`/
  `emitter_order_independence_test.clj`/`identifiers_test.clj`, not by
  interface-design judgment (sim split S3, `.agents/plans/2026-08-02-
  sim-split-plan.md`'s own AR-6 discipline, `notes/ADRs.md` ADR-0025).
  `emit-hl7`'s own 2-arg arity of `emit` has zero real external callers
  (confirmed by that same grep) and stays unexported; `v2-replay` and
  `site-profile` have NO real external caller at all -- both are fully
  internal to this component, invisible from outside it."
  (:require [ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]))

(def default-reference-date emit-hl7/default-reference-date)
(def default-utc-offset emit-hl7/default-utc-offset)

(defn control-id-for [ev] (emit-hl7/control-id-for ev))

(defn emit
  ([ground-truth reference-date utc-offset]
   (emit-hl7/emit ground-truth reference-date utc-offset))
  ([ground-truth reference-date utc-offset facility providers]
   (emit-hl7/emit ground-truth reference-date utc-offset facility providers))
  ([ground-truth reference-date utc-offset facility providers site-profile]
   (emit-hl7/emit ground-truth reference-date utc-offset facility providers site-profile)))
