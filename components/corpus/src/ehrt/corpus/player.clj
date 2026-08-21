(ns ehrt.corpus.player
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
  no HAPI dependency, no full HL7 parse, for reading one field. The
  reader itself moved to ehrt.corpus-io.er7-fields (ADR-0111,
  move-don't-improve micro-relocation: the corpus batcher's own
  partition fn lives in corpus-io by design and needs this same MSH-7
  extraction, and corpus-io may never require ehrt.corpus.* -- see that
  namespace's own docstring); parse-dtm-lenient/message-timestamp-ms/
  message-type-trigger/message-patient-id are re-exported below,
  unchanged in behavior, so every existing caller and test stays
  byte-identical."
  (:require [ehrt.corpus-io.interface :as corpus-io]))

(def default-rate
  "Stream-seconds per wallclock-second (--rate). 60 = one stream-hour
  per wallclock-minute; 1 = real time."
  60)

(def default-idle-cap-ms
  "Default --idle-cap, in ms (5 seconds): the wallclock cap on any
  single inter-event wait."
  5000)

;; ---- lenient segment/field reads: moved to ehrt.corpus-io.er7-fields
;; (ADR-0111) -- re-exported here unchanged so every existing caller
;; (this namespace's own message-timestamp-ms callers included) and
;; every existing test stays byte-identical. ----

(def parse-dtm-lenient corpus-io/parse-dtm-lenient)
(def message-timestamp-ms corpus-io/message-timestamp-ms)
(def message-type-trigger corpus-io/message-type-trigger)
(def message-patient-id corpus-io/message-patient-id)

(defn event-timestamp-ms
  "event (a compiled ground-truth event map -- ehrt.patient-simulator.
  compile-trajectory's own shape, ADR-0100) -> its own :t (seconds
  from the sim run's own epoch, sim/ADR-0011), scaled to ms -- or nil
  when :t is missing or not a number. The sim event-log adapter's own
  :timestamp-fn for `plan` (bases/cli), the event-input counterpart to
  message-timestamp-ms above."
  [event]
  (when (number? (:t event))
    (long (* 1000 (:t event)))))

;; ---- wire-format framing for a data sink (ADR-0014's own byte-
;; identity requirement) -- reuses ehrt.corpus-io.framing/encode
;; directly, never a second implementation of the :er7-multi separator
;; convention. Pure: returns bytes, writes nothing. ----

(defn frame-event
  "event (one raw ER7 message string) -> kernel/ok the bytes it
  contributes under :er7-multi framing (the message itself plus its
  own trailing separator) -- appending N single-event calls' own
  bytes, in order, onto one file produces bytes byte-identical to one
  batch `framing/encode :er7-multi events` call over the same events,
  since :er7-multi's own encode is exactly item+separator per item,
  concatenated in order (ehrt.corpus-io.framing/encode-er7-multi).
  This is what lets paced file emission satisfy ADR-0014's own
  byte-identity requirement without a second framing implementation."
  [event]
  (corpus-io/encode :er7-multi [(.getBytes ^String event "UTF-8")]))

;; ---- the plan: the entire time computation, no clock, no IO ----

(defn plan
  "events (a seq of raw ER7 message strings, in their OWN order --
  never sorted here; order is a semantic property of the input) x
  {:rate :idle-cap-ms :timestamp-fn} -> {:plan [[wait-ms event] ...]
                           :clamped-count n :unparseable-count n
                           :skip-count n :capped-indices #{...}}.

  :timestamp-fn (ADR-0100's own injectable timestamp-extraction seam,
  continuing the :tty?-fn/:sleep-fn injection lineage rather than a
  second pacer) defaults to message-timestamp-ms -- the original MSH-7
  path, byte-identical for every existing caller. The sim event-log
  adapter (bases/cli) injects event-timestamp-ms instead; `plan` itself
  never inspects an event/message beyond calling this one function on
  it, so the seam is the entire adaptation.

  wait-ms for the first event is always 0. For every later event, the
  wait is (delta-ms / rate) against the PRECEDING event's own effective
  timestamp (deltas compound across a run, exactly like real elapsed
  time would). A negative delta (an out-of-order or duplicate
  timestamp) is clamped to zero and counted (:clamped-count). An event
  whose own timestamp-fn returns nil paces at zero delta and is counted
  (:unparseable-count) -- its own timestamp is treated as identical to
  its predecessor's for every LATER delta too, so one bad timestamp
  doesn't corrupt every subsequent gap. Every wait is capped to
  idle-cap-ms (applied AFTER dividing by rate, since \"wait\" means
  wallclock wait); a wait that was actually capped is tallied
  separately (:skip-count, :capped-indices -- indices into :plan) from
  a clamped one -- a capped wait is never also a clamped one."
  [events {:keys [rate idle-cap-ms timestamp-fn]
           :or {rate default-rate idle-cap-ms default-idle-cap-ms timestamp-fn message-timestamp-ms}}]
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
            ts (timestamp-fn event)
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
