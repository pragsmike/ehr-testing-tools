(ns ehrt.sim-emit-hl7.interface
  "Re-exports exactly what residual `sim`'s own src and test trees call
  from outside this component -- determined by grep against
  `components/sim`'s own `identifiers.clj`/`run.clj` and its
  `churn_scenarios_test.clj`/`emit_state_test.clj`/
  `emitter_order_independence_test.clj`/`identifiers_test.clj`, not by
  interface-design judgment (sim split S3, `.agents/plans/2026-08-02-
  sim-split-plan.md`'s own AR-6 discipline, `notes/ADRs.md` ADR-0025).
  `emit-hl7`'s own 2-arg arity of `emit` has zero real external callers
  (confirmed by that same grep) and stays unexported; `site-profile`
  has NO real external caller at all -- fully internal to this
  component, invisible from outside it. `v2-replay` gains its first
  real external caller here (player board, `notes/ADRs.md` ADR-0067,
  AR-BB2-1): `corpus`'s own board sink calls `fold-message` to fold a
  paced HL7 v2 stream into the same accumulator shape the emitter-
  coherence property already reasons about -- `fold-message` is the
  entire surface that caller needs, so it is the entire surface
  exported here."
  (:require [ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]
            [ehrt.sim-emit-hl7.v2-replay :as v2-replay]))

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

(def beds-key
  "The A20 stream's own key inside a `fold-message` accumulator (arc 3b
  sweep 2). See ehrt.sim-emit-hl7.v2-replay/beds-key."
  v2-replay/beds-key)

(defn fold-message
  "acc x message -> acc'. See ehrt.sim-emit-hl7.v2-replay/fold-message."
  [acc message]
  (v2-replay/fold-message acc message))

;; --- ADR-0109: the second clock -- ehrt.sim.run's own new call site -------

(defn plan-latency
  "RNG x GT x LatencyProfile -> offsets. See
  ehrt.sim-emit-hl7.emit-hl7/plan-latency."
  [rng ground-truth latency-profile]
  (emit-hl7/plan-latency rng ground-truth latency-profile))

(defn emit-wire
  "GT x reference-date x utc-offset x facility x providers x
  site-profile x offsets [x emission] -> TimedWire. See
  ehrt.sim-emit-hl7.emit-hl7/emit-wire."
  ([ground-truth reference-date utc-offset facility providers site-profile offsets]
   (emit-hl7/emit-wire ground-truth reference-date utc-offset facility providers site-profile offsets))
  ([ground-truth reference-date utc-offset facility providers site-profile offsets emission]
   (emit-hl7/emit-wire ground-truth reference-date utc-offset facility providers site-profile offsets emission)))

;; --- ARC 4 SWEEP 2 (ADR-0175 design (a)): chatter, ehrt.sim.run's own
;; new call site -- `plan-latency`'s sibling, exported for the same
;; reason and at the same seam.

(defn plan-chatter
  "RNG x GT x ChatterProfile -> chatter render instructions. See
  ehrt.sim-emit-hl7.emit-hl7/plan-chatter."
  [rng ground-truth chatter-profile]
  (emit-hl7/plan-chatter rng ground-truth chatter-profile))

(def chatter-event-kinds
  "The three ground-truth kinds an event-driven chatter rule may cover.
  See ehrt.sim-emit-hl7.emit-hl7/chatter-event-kinds."
  emit-hl7/chatter-event-kinds)
