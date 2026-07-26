(ns ehr-testing-sim.emit-hl7
  "EmitHL7 (docs/sim-theory.edn): pure log -> ER7 messages, the thin
  vertical slice from ground-truth-log to hl7v2-stream. v0 scope:
  ADT^A01 (admission) and ADT^A03 (discharge) only, MSH/EVN/PID/PV1
  populated minimally -- on org.clojars.cmiles74/clojure-hl7-parser's
  own data structures (the only runtime dependency this stage adds).

  Consumes the ground-truth log ONLY: no RNG, no wall clock
  (determinism law). Every timestamp is rendered from the pinned
  :reference-date run-config input plus the event's log-relative
  minute offset (timestamp-anchoring law) -- never from
  System/currentTimeMillis or similar."
  (:require [com.nervestaple.hl7-parser.parser :as parser]))

(def default-reference-date
  "Pinned default for the :reference-date run-config input (an ISO
  date string, midnight local time is the run's t=0). Arbitrary but
  fixed -- determinism does not care which date, only that every run
  states one explicitly (never buried in :invocation, per tools'
  ManifestV1 lesson)."
  "2024-01-01")

(def message-type-registry
  "Event type -> HL7 message type/trigger: the emitter's own catalytic
  catalog (docs/sim-theory.edn, catalytic target 4). v0 covers exactly
  the two event types the engine emits today; a new engine step type
  earns an entry here in the same change that adds it (the same
  co-landing convention check.clj's catalog already follows)."
  {:admission {:type "ADT" :trigger "A01"}
   :discharge {:type "ADT" :trigger "A03"}})

(def ^:private hl7-timestamp-formatter
  (java.time.format.DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))

(defn- reference-instant
  [reference-date]
  (.atStartOfDay (java.time.LocalDate/parse reference-date)))

(defn hl7-timestamp
  "Renders the absolute HL7 timestamp for `minutes` (a log event's :t,
  minutes from the run's epoch) anchored to :reference-date -- the
  timestamp-anchoring law. Pure: reference-date + minutes in, string
  out, nothing else consulted."
  [reference-date minutes]
  (.format (.plusMinutes (reference-instant reference-date) minutes)
           hl7-timestamp-formatter))

(defn- msh-segment
  [{:keys [type trigger]} control-id ts]
  (parser/create-segment
   "MSH"
   (parser/create-field (parser/pr-delimiters parser/DEFAULT-DELIMITERS))
   (parser/create-field ["EHR-TESTING-SIM"])
   (parser/create-field ["SIM"])
   (parser/create-field [])
   (parser/create-field [])
   (parser/create-field [ts])
   (parser/create-field [])
   (parser/create-field [type trigger])
   (parser/create-field [control-id])
   (parser/create-field ["P"])
   (parser/create-field ["2.3"])))

(defn- evn-segment
  [trigger ts]
  (parser/create-segment
   "EVN"
   (parser/create-field [trigger])
   (parser/create-field [ts])))

(defn- pid-segment
  [mrn]
  (parser/create-segment
   "PID"
   (parser/create-field ["1"])
   (parser/create-field [])
   (parser/create-field [mrn])))

(defn- pv1-segment
  [location]
  (parser/create-segment
   "PV1"
   (parser/create-field ["1"])
   (parser/create-field ["I"])
   (parser/create-field (if location [location] []))))

(defn event->message
  "Renders one ground-truth event to an ER7 string, or nil when the
  event's :event isn't in `message-type-registry` (v0 scope: only
  :admission and :discharge produce messages)."
  [reference-date {:keys [event t mrn location]}]
  (when-let [type+trigger (message-type-registry event)]
    (let [ts (hl7-timestamp reference-date t)
          control-id (str mrn "-" (:trigger type+trigger) "-" t)]
      (parser/str-message
       (parser/create-message
        parser/DEFAULT-DELIMITERS
        (msh-segment type+trigger control-id ts)
        (evn-segment (:trigger type+trigger) ts)
        (pid-segment mrn)
        (pv1-segment location))))))

(defn emit
  "The stage function: ground-truth log -> vector of ER7 message
  strings, in log order. Pure function of `ground-truth` and
  `reference-date` alone (determinism law); events outside v0 scope
  are skipped, not errored -- the theory's laws bind the events this
  stage claims to handle, not every event type that may ever appear in
  a log."
  [ground-truth reference-date]
  (into [] (keep (partial event->message reference-date)) ground-truth))
