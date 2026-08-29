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
            [ehrt.sim-emit-hl7.fan-out :as fan-out]
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

(defn plan-charges
  "GT x ChargesProfile -> {:lines ... :skipped ...}. See
  ehrt.sim-emit-hl7.emit-hl7/plan-charges."
  [ground-truth charges]
  (emit-hl7/plan-charges ground-truth charges))

;; --- ARC 4 SWEEP 3 (ADR-0175 design (b)): status ladders, ehrt.sim.run's
;; own new call site. NO RNG in the signature, and that is the design
;; rather than an omission -- a rung is a fixed fraction of an interval
;; the log already carries.

(defn plan-ladders
  "GT x LadderProfile -> {:rungs [...] :final #{...}}. See
  ehrt.sim-emit-hl7.emit-hl7/plan-ladders."
  [ground-truth ladders]
  (emit-hl7/plan-ladders ground-truth ladders))

(def skeleton-message-types
  "Every MSH-9 the registry produces, as TYPE^TRIGGER -- `gate v2`'s
  sampling policy's own skeleton half. See
  ehrt.sim-emit-hl7.emit-hl7/skeleton-message-types."
  emit-hl7/skeleton-message-types)

(def room-and-board-code
  "The reserved price-table key for a per-inpatient-day charge line.
  See ehrt.sim-emit-hl7.emit-hl7/room-and-board-code."
  emit-hl7/room-and-board-code)

(def chatter-event-kinds
  "The three ground-truth kinds an event-driven chatter rule may cover.
  See ehrt.sim-emit-hl7.emit-hl7/chatter-event-kinds."
  emit-hl7/chatter-event-kinds)

;; --- ARC 4 SWEEP 4 (ADR-0175 ruling B1): SIU. There is no `plan-siu`
;; to expose and that is the point -- `:siu` has no planner, no stream
;; and no draw; `ehrt.sim.run` forwards the profile verbatim into
;; `emit-wire`'s `:emission` map and `event->messages` reads it per
;; event.

(def siu-event-kinds
  "The four ground-truth kinds SIU^S12 renders, derived from the
  registry. See ehrt.sim-emit-hl7.emit-hl7/siu-event-kinds."
  emit-hl7/siu-event-kinds)

(defn siu-renders?
  "SiuProfile x event-kind -> boolean. See
  ehrt.sim-emit-hl7.emit-hl7/siu-renders?."
  [siu event]
  (emit-hl7/siu-renders? siu event))

;; --- ARC 4 SWEEP 5 (ADR-0175 design (f), ruling B1): fan-out. A filter
;; over an already-rendered stream, called from `ehrt.sim.run` once the
;; message vector exists -- the only seam at which the base spool is
;; complete and nothing has been written yet.

(def add-on-message-types
  "Every MSH-9 arc 4's emission add-ons produce. See
  ehrt.sim-emit-hl7.emit-hl7/add-on-message-types."
  emit-hl7/add-on-message-types)

(def emittable-message-types
  "The whole vocabulary this emitter can put on a wire, and therefore
  the allow-list a `:fan-out` filter may name. See
  ehrt.sim-emit-hl7.emit-hl7/emittable-message-types."
  emit-hl7/emittable-message-types)

(defn plan-fan-out
  "messages x subscribers x site-profile -> one plan entry per
  subscriber. See ehrt.sim-emit-hl7.fan-out/plan, whose docstring
  carries the SUBSEQUENCE LAW and the PV1-less rule."
  [messages subscribers site-profile]
  (fan-out/plan messages subscribers site-profile))

(defn mask-msh
  "message x overrides x replacement-fn -> the message with exactly the
  named MSH fields rewritten -- the mask half of the subsequence law,
  exported so a gate can ERASE the same fields on both sides rather
  than reimplement the mask. See ehrt.sim-emit-hl7.fan-out/mask-msh."
  [message overrides replacement-fn]
  (fan-out/mask-msh message overrides replacement-fn))
