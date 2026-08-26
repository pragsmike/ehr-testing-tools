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
            [ehrt.sim-engine.order-profiles :as order-profiles]
            [ehrt.sim-engine.person-fold :as person-fold]))

;; --- orchestration surface (run.clj's own engine + churn wiring;
;; config-keys is also read by identifiers.clj, mirroring run's own
;; config-forwarding) --------------------------------------------------------

(def run engine/run)
(def config-keys engine/config-keys)
;; ADR-0173 ruling C1 (arc 3a): one patient's Persona + compiled module
;; trajectory, drawn from that patient's own `:patient` stream and
;; INDEPENDENT of when that patient arrives. Exported with no caller
;; today, deliberately: arc 3a part 3 has `ehrt.sim.run` call it to
;; obtain each patient's compiled DEATH instant before the run, which is
;; what the person component's own `persons` front door needs as its
;; `:deaths` parameter -- the engine owns the stream positioning, so a
;; caller outside this component never reimplements it.
;;
;; Named that way, and not by namespace, ON PURPOSE: ADR-0172
;; limitations row 10's reverse-edge half is a bare token scan over this
;; component's whole src, so even a prose citation of that component's
;; name here reads as a feedback edge. The forward half of the same gate
;; distinguishes a citation from a dependency; the reverse half does
;; not, and this arc may not widen it (arc 3a part 2's own fence).
(def compile-patient engine/compile-patient)
;; ADR-0173 section 2(a)/ruling C1 (arc 3a part 3). `run` gained one
;; config key, `:persons`, whose value is DATA a caller built -- the
;; same two-layer treatment `:modules` already has, where the config
;; side is names and this side is already-resolved structures.
;;
;; `person-plan` is the export ruling C1's ordering problem needs: the
;; compiled trajectory's death instant is a t0 parameter of the process
;; that produces the person stream, keyed by PERSON, while this engine
;; mints a patient id from an arrival ORDINAL and binds the two with a
;; `:world`-family draw taken at a pinned position inside the run. A
;; caller cannot key those deaths without asking which person each
;; arrival bound to, and this is that question -- answered by the same
;; pre-loop `run` itself uses, so the two cannot disagree.
;;
;; `person-deaths` and `valid-persons?` are the other two halves of that
;; contract: how `:alive` is read off a stream, and what a well-formed
;; `:persons` value is.
(def person-plan engine/person-plan)
(def person-deaths person-fold/deaths)
(def valid-persons? engine/valid-persons?)
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

;; --- stream-partition surface (ADR-0171, arc 1) ---------------------------
;;
;; Ruling A1 promotes `mix64` from private to this seam, because the
;; partition derives every stream seed with it and a consumer outside
;; this component now needs the same derivation: `ehrt.sim.run` builds
;; the EMISSION family's stream here rather than reusing the master seed
;; verbatim (ruling C1), and `ehrt.sim.manifest` stamps the scheme marker
;; (ruling D1).
;;
;; `newborn-id-tag` is exported with no caller today, deliberately: arc 2
;; owns the newborn path, and ruling B1 fixed its key NOW so arc 2
;; inherits the pair (parity-index, within-delivery-index) rather than
;; choosing a bare parity index and owing a second reshuffle when
;; multiples stop being a v1 limitation.

(def mix64 engine/mix64)
(def stream-scheme engine/stream-scheme)
(def stream-seed engine/stream-seed)
(def stream engine/stream)
(def newborn-id-tag engine/newborn-id-tag)
