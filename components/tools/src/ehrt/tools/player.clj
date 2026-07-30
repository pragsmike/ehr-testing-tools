(ns ehrt.tools.player
  "The corpus player's pure core (ADR-0014): plans a paced playback of a
  time-ordered event stream, with no clock and no IO. `plan` is the
  entire time-computation surface -- fully property-testable without a
  wallclock. The executor (bases/cli) folds the plan through an
  injected sleep-fn and a sink; nothing in this namespace ever sleeps
  or writes.

  `ehrt play` at rate Infinity, ticker sink, is exactly `ehrt show`
  (ADR-0013/ADR-0014's own identity): ordinary division of a delta by
  an arbitrarily large (or literally infinite) rate yields an
  arbitrarily small (or exactly zero) wait, with no special-cased
  sentinel needed -- the identity holds by construction.

  Field reads are lenient and dependency-free: MSH-7 (and, for the
  ticker's compact line, MSH-9/PID-3) are read by field-splitting the
  raw segment text on the separator character MSH-1 itself declares --
  no HAPI dependency, no full HL7 parse, for reading one field."
  (:require [clojure.string :as str])
  (:import [java.time LocalDateTime ZoneOffset]))

(def default-rate
  "Stream-seconds per wallclock-second (--rate). 60 = one stream-hour
  per wallclock-minute; 1 = real time."
  60)

(def default-idle-cap-ms
  "Default --idle-cap, in ms (5 seconds): the wallclock cap on any
  single inter-event wait."
  5000)

;; ---- lenient segment/field reads (no HAPI dependency) ----

(defn- first-segment
  "The first segment of a message (up to its own first CR), or the
  whole message when it carries no segment break at all."
  [message]
  (let [i (.indexOf ^String message "\r")]
    (if (neg? i) message (subs message 0 i))))

(defn- segment-starting-with
  "The first segment in message whose own 3-char id is prefix, or nil.
  Segments are CR-separated; the boundary rule matches
  ehrt.tools.corpus.framing's own message-internal segment split."
  [message prefix]
  (first (filter #(str/starts-with? % prefix) (str/split message #"\r"))))

(defn- msh-separator-char
  "MSH-1 IS the field separator -- the character immediately after the
  3-char \"MSH\" literal. nil for anything too short to have one."
  [msh-segment]
  (when (>= (count msh-segment) 4)
    (str (nth msh-segment 3))))

(defn- split-on-separator
  [segment sep-char]
  (str/split segment (re-pattern (java.util.regex.Pattern/quote sep-char)) -1))

(defn- msh-field
  "1-based MSH-N -> its raw string value, or nil when absent. MSH-1 is
  the separator itself (never returned as a value here -- N=1 has no
  caller this namespace needs); splitting the segment (minus its own
  leading \"MSH\") on that separator yields fields 2..N directly."
  [msh-segment n]
  (when-let [sep (msh-separator-char msh-segment)]
    (let [fields (split-on-separator (subs msh-segment 3) sep)]
      ;; fields[0] is the empty artifact before the first separator
      ;; (there is no text before MSH-1, which IS that separator);
      ;; fields[k] for k>=1 is MSH-(k+1).
      (nth fields (dec n) nil))))

(defn- segment-field
  "1-based field N of a non-MSH segment (index 0 is the segment id
  itself, e.g. \"PID\") -- unlike MSH, an ordinary segment's own id is
  not a separator, so splitting on the message-wide separator character
  directly yields the segment id at index 0 and field N at index N."
  [segment sep-char n]
  (nth (split-on-separator segment sep-char) n nil))

(def ^:private dtm-pattern
  #"^(\d{4})(\d{2})?(\d{2})?(\d{2})?(\d{2})?(\d{2})?")

(defn parse-dtm-lenient
  "Lenient HL7 v2 DTM parse: accepts any YYYY[MM[DD[HH[MM[SS]]]]] prefix
  (an optional trailing fraction/zone is ignored, matched or not),
  returns epoch milliseconds (UTC) for whatever precision was actually
  present -- an absent trailing component defaults to the start of its
  own unit (month 1, day 1, hour/min/sec 0). nil for nil/blank/garbage
  that doesn't even start with a 4-digit year."
  [dtm]
  (when (and dtm (seq dtm))
    (when-let [[_ y mo d h mi s] (re-find dtm-pattern dtm)]
      (try
        (.toEpochMilli
         (.toInstant
          (.atZone (LocalDateTime/of (Integer/parseInt y)
                                      (if mo (Integer/parseInt mo) 1)
                                      (if d (Integer/parseInt d) 1)
                                      (if h (Integer/parseInt h) 0)
                                      (if mi (Integer/parseInt mi) 0)
                                      (if s (Integer/parseInt s) 0))
                    ZoneOffset/UTC)))
        (catch Exception _ nil)))))

(defn message-timestamp-ms
  "message (a raw ER7 message string) -> epoch ms from its own MSH-7,
  or nil when MSH-7 is absent or unparseable."
  [message]
  (parse-dtm-lenient (msh-field (first-segment message) 7)))

(defn message-type-trigger
  "message -> its MSH-9 value (e.g. \"ADT^A01^ADT_A01\"), or nil."
  [message]
  (msh-field (first-segment message) 9))

(defn message-patient-id
  "message -> its PID-3 value (the first repetition, as a raw string --
  e.g. \"445566^^^CGH^MR\"), or nil when the message carries no PID
  segment. Uses the SAME separator character MSH-1 declares, read once
  from the message's own MSH segment (an ER7 message declares one
  separator set for the whole message, never per-segment)."
  [message]
  (let [msh (first-segment message)]
    (when-let [sep (msh-separator-char msh)]
      (when-let [pid (segment-starting-with message "PID")]
        (segment-field pid sep 3)))))

;; ---- the plan: the entire time computation, no clock, no IO ----

(defn plan
  "events (a seq of raw ER7 message strings, in their OWN order --
  never sorted here; order is a semantic property of the input) x
  {:rate :idle-cap-ms} -> {:plan [[wait-ms event] ...]
                           :clamped-count n :unparseable-count n
                           :skip-count n :capped-indices #{...}}.

  wait-ms for the first event is always 0. For every later event, the
  wait is (delta-ms / rate) against the PRECEDING event's own effective
  timestamp (deltas compound across a run, exactly like real elapsed
  time would). A negative delta (an out-of-order or duplicate
  timestamp) is clamped to zero and counted (:clamped-count). A
  message whose own MSH-7 is missing or unparseable paces at zero delta
  and is counted (:unparseable-count) -- its own timestamp is treated
  as identical to its predecessor's for every LATER delta too, so one
  bad timestamp doesn't corrupt every subsequent gap. Every wait is
  capped to idle-cap-ms (applied AFTER dividing by rate, since \"wait\"
  means wallclock wait); a wait that was actually capped is tallied
  separately (:skip-count, :capped-indices -- indices into :plan) from
  a clamped one -- a capped wait is never also a clamped one."
  [events {:keys [rate idle-cap-ms] :or {rate default-rate idle-cap-ms default-idle-cap-ms}}]
  (loop [remaining events
         idx 0
         prev-ts nil
         clamped 0
         unparseable 0
         skipped 0
         capped-indices #{}
         out []]
    (if (empty? remaining)
      {:plan out :clamped-count clamped :unparseable-count unparseable
       :skip-count skipped :capped-indices capped-indices}
      (let [event (first remaining)
            ts (message-timestamp-ms event)
            unparseable? (nil? ts)
            delta (when (and prev-ts ts) (- ts prev-ts))
            clamped? (and delta (neg? delta))
            raw-wait-ms (cond
                          (nil? prev-ts) 0
                          unparseable? 0
                          clamped? 0
                          :else (long (/ delta rate)))
            capped? (> raw-wait-ms idle-cap-ms)
            wait-ms (min raw-wait-ms idle-cap-ms)
            effective-ts (or ts prev-ts)]
        (recur (rest remaining)
               (inc idx)
               effective-ts
               (cond-> clamped clamped? inc)
               (cond-> unparseable unparseable? inc)
               (cond-> skipped capped? inc)
               (cond-> capped-indices capped? (conj idx))
               (conj out [wait-ms event]))))))
