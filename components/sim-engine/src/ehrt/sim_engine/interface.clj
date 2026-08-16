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

  Step 2 landing (engine.clj moves in, completing this component):
  `inject` and `sample-analyte-value`, Step 1's transitional
  accommodation for residual sim's own `engine.clj`, are REMOVED here
  -- engine.clj now lives inside this component and reaches churn/
  order-profiles as sibling internal namespaces, not through this seam
  (no OTHER src-scope caller outside this component ever needed
  either var, so nothing else repoints). Three documented sections
  below, each named for the residual-sim caller(s) it serves:

  - orchestration surface -- what `run.clj` (and, for `config-keys`,
    `identifiers.clj`, which mirrors run's own config-forwarding) drive
    the engine with: `run`, `config-keys`, `default-churn-profile`,
    `sample-profile`.
  - state-reader surface -- what `emit-state.clj` and `identifiers.clj`
    fold over the engine's own output: `replay` (also read by `check`,
    below -- one def, several callers, not duplicated per section).
  - acceptance surface -- what `check.clj` validates a run's
    ground-truth log against: `documented-step-rejection-reasons`,
    `default-profiles`, `abnormal-flag` (plus `replay`, shared with the
    state-reader surface above)."
  (:require [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-engine.event-schema :as event-schema]
            [ehrt.sim-engine.order-profiles :as order-profiles]))

;; --- orchestration surface (run.clj's own engine + churn wiring;
;; config-keys is also read by identifiers.clj, mirroring run's own
;; config-forwarding) --------------------------------------------------------

(def run engine/run)
(def config-keys engine/config-keys)
(def default-churn-profile churn/default-churn-profile)
(def sample-profile churn/sample-profile)

;; --- state-reader surface (emit-state.clj and identifiers.clj folding
;; the engine's own output) --------------------------------------------------

(def replay engine/replay)

;; --- acceptance surface (check.clj's own invariant catalog) ----------------

(def documented-step-rejection-reasons engine/documented-step-rejection-reasons)

;; --- contract surface (event-log contract arc, 2026-08-16) ----------------
;;
;; The ground-truth event log's own schema, and the version `ehrt.sim.
;; manifest` stamps into every run's manifest as :event-schema-version
;; (author ruling Q-A (a): the log is a public, versioned contract, so a
;; consumer holding an events.edn can tell which contract produced it).
;;
;; `Event` is exported here for the CONSUMER-CONFORMANCE tests in
;; sim-emit-hl7, sim-emit-fhir, and sim-check -- the three built-in
;; consumers validating their own INPUT against the explicit contract
;; instead of against a shape reverse-engineered from our HL7 emitter.
;; Nothing in any production path validates: the contract costs no
;; runtime.

(def event-schema-version event-schema/schema-version)
(def Event event-schema/Event)
(def valid-event? event-schema/valid-event?)
(def explain-event event-schema/explain-event)
(def run-t-monotone? event-schema/run-t-monotone?)
(def default-profiles order-profiles/default-profiles)
(def abnormal-flag order-profiles/abnormal-flag)
