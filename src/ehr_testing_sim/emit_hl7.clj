(ns ehr-testing-sim.emit-hl7
  "EmitHL7 (docs/sim-theory.edn): pure log -> ER7 messages, the thin
  vertical slice from ground-truth-log to hl7v2-stream. v0 scope was
  ADT^A01 (admission) and ADT^A03 (discharge) only; Milestone M1
  (docs/operational-models.md) adds ADT^A02 (transfer, including bed-
  ready) alongside its step type, per the roadmap's own co-landing
  extension of that rule to this registry. MSH/EVN/PID/PV1 populated
  minimally -- on org.clojars.cmiles74/clojure-hl7-parser's own data
  structures (the only runtime dependency this stage adds).

  Consumes the ground-truth log ONLY: no RNG, no wall clock
  (determinism law). facility/providers are additional PINNED,
  non-random inputs (like :reference-date and :utc-offset already are)
  needed to render PV1-3/6's ward^^bed^facility shape and PV1-7's
  attending -- passing them doesn't touch the no-RNG/no-wall-clock
  doctrine, since none is sampled here, only rendered. Every timestamp
  is rendered from the pinned :reference-date run-config input plus
  the event's log-relative SECOND offset (ADR-0011; was minutes before
  M2a), suffixed with the pinned :utc-offset (ADR-0011: a fixed offset,
  never a timezone-database lookup, never per-event) -- never from
  System/currentTimeMillis or similar. PID-3 renders the event's own
  :active-mrn (ADR-0010: MRN moved into state; the emitter renders
  whichever MRN was active when the event happened, which until M2b's
  merge exists is always the patient's one and only MRN)."
  (:require [com.nervestaple.hl7-parser.parser :as parser]
            [clojure.string :as str]
            [ehr-testing-sim.config :as config]))

(def default-reference-date
  "Pinned default for the :reference-date run-config input (an ISO
  date string, midnight local time is the run's t=0). Arbitrary but
  fixed -- determinism does not care which date, only that every run
  states one explicitly (never buried in :invocation, per tools'
  ManifestV1 lesson)."
  "2024-01-01")

(def default-utc-offset
  "Pinned default for the :utc-offset run-config input (ADR-0011): a
  fixed ISO-style offset (\"+00:00\"), no DST, no timezone database.
  Rendered in HL7v2's own colon-free zone-suffix convention
  (\"+0000\") -- see `hl7-timestamp`."
  "+00:00")

(def message-type-registry
  "Event type -> HL7 message type/trigger: the emitter's own catalytic
  catalog (docs/sim-theory.edn, catalytic target 4). A new engine step
  type earns an entry here in the same change that adds it (the same
  co-landing convention check.clj's catalog already follows -- and, per
  Milestone M1's roadmap note, extended to this registry too: a step
  type without a message-type entry produces traffic invisible to every
  consumer downstream of this stage)."
  {:admission {:type "ADT" :trigger "A01"}
   :discharge {:type "ADT" :trigger "A03"}
   :transfer {:type "ADT" :trigger "A02"}})

(def ^:private hl7-timestamp-formatter
  (java.time.format.DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))

(defn- reference-instant
  [reference-date]
  (.atStartOfDay (java.time.LocalDate/parse reference-date)))

(defn- hl7-offset-suffix
  "ISO-style offset (\"+00:00\", \"-05:00\") rendered in HL7v2's own
  zone-suffix convention: colon-free (\"+0000\", \"-0500\")."
  [utc-offset]
  (str/replace utc-offset ":" ""))

(defn hl7-timestamp
  "Renders the absolute HL7 timestamp for `seconds` (a log event's :t,
  SECONDS from the run's epoch -- ADR-0011, was minutes before M2a)
  anchored to :reference-date, suffixed with :utc-offset in HL7's own
  colon-free zone convention -- the timestamp-anchoring law, extended
  to state which fixed offset the naive wall-clock arithmetic is
  asserted to be in (no timezone database, no DST: the arithmetic
  itself never shifts across zones, ADR-0011). Pure: reference-date +
  seconds + utc-offset in, string out, nothing else consulted."
  [reference-date seconds utc-offset]
  (str (.format (.plusSeconds (reference-instant reference-date) seconds) hl7-timestamp-formatter)
       (hl7-offset-suffix utc-offset)))

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
  [active-mrn]
  (parser/create-segment
   "PID"
   (parser/create-field ["1"])
   (parser/create-field [])
   (parser/create-field [active-mrn])))

(defn- location-field
  "Renders a location map as ward^^bed^facility (PV1-3/PV1-6's shared
  shape, docs/operational-models.md's transfer/A02 spec: 'PV1-3 renders
  ward^^bed with facility in PV1-3.4'). nil location (no prior, or a
  v0 event with no location at all) -> an empty field, same as v0's
  own nil-location handling."
  [facility-name location]
  (if-let [ward (:ward location)]
    (parser/create-field [ward "" (or (:bed location) "") facility-name])
    (parser/create-field [])))

(defn- provider-field
  "PV1-7: id^family^given. nil provider -> empty field."
  [provider]
  (if provider
    (parser/create-field [(:id provider) (get-in provider [:name :family]) (get-in provider [:name :given])])
    (parser/create-field [])))

(defn- provider-by-id
  [providers id]
  (first (filter #(= id (:id %)) providers)))

(defn- pv1-segment
  "PV1-6 (prior location) is read directly off the CURRENT event's own
  :from -- present only on :transfer events -- never a separately
  maintained prior-location field on patient state (docs/patient-
  state-model.md's Simulated Hospital lesson: one :location field plus
  the log's own facts replaces a shadow-field zoo)."
  [facility-name location from provider]
  (parser/create-segment
   "PV1"
   (parser/create-field ["1"])
   (parser/create-field ["I"])
   (location-field facility-name location)
   (parser/create-field [])
   (parser/create-field [])
   (location-field facility-name from)
   (provider-field provider)))

(defn event->message
  "Renders one ground-truth event to an ER7 string, or nil when the
  event's :event isn't in `message-type-registry`."
  [reference-date utc-offset facility providers
   {:keys [event t active-mrn location from attending]}]
  (when-let [type+trigger (message-type-registry event)]
    (let [ts (hl7-timestamp reference-date t utc-offset)
          control-id (str active-mrn "-" (:trigger type+trigger) "-" t)
          facility-name (name (:id facility))
          provider (provider-by-id providers attending)]
      (parser/str-message
       (parser/create-message
        parser/DEFAULT-DELIMITERS
        (msh-segment type+trigger control-id ts)
        (evn-segment (:trigger type+trigger) ts)
        (pid-segment active-mrn)
        (pv1-segment facility-name location from provider))))))

(def ^:private default-providers
  "A fixed, arbitrary reference-seed provider pool -- purely a fallback
  default for callers that don't care about exact NPI values (`emit`'s
  lower arities). A real run threads back its OWN materialized
  providers (ehr-testing-sim.engine/run's :providers) instead, so its
  messages' PV1-7 matches its own ground-truth log's :attending ids."
  (config/materialize-providers (java.util.Random. 0) config/default-provider-templates))

(defn emit
  "The stage function: ground-truth log -> vector of ER7 message
  strings, in log order. Pure function of its arguments alone
  (determinism law); events outside `message-type-registry` are
  skipped, not errored -- the theory's laws bind the events this stage
  claims to handle, not every event type that may ever appear in a
  log. `utc-offset`/`facility`/`providers` default for standalone
  convenience; callers rendering a specific run's log should pass back
  that SAME run's :utc-offset/:facility/:providers (ehr-testing-sim.run
  does)."
  ([ground-truth reference-date]
   (emit ground-truth reference-date default-utc-offset config/default-facility default-providers))
  ([ground-truth reference-date utc-offset]
   (emit ground-truth reference-date utc-offset config/default-facility default-providers))
  ([ground-truth reference-date utc-offset facility providers]
   (into [] (keep (partial event->message reference-date utc-offset facility providers)) ground-truth)))
