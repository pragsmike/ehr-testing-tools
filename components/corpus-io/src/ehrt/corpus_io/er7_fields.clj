(ns ehrt.corpus-io.er7-fields
  "Lenient, dependency-free v2 field reads -- MSH-7 (and MSH-9/PID-3)
  read by field-splitting the raw segment text on the separator
  character MSH-1 itself declares, no HAPI dependency, no full HL7
  parse, for reading one field. Moved here verbatim from
  ehrt.corpus.player (ADR-0111, move-don't-improve micro-relocation):
  the new corpus-level batcher's own partition fn (ehrt.corpus-io.batch)
  needs MSH-7 extraction and lives in corpus-io by design (Part I of
  this session's own driving prompt), but corpus-io may never require
  ehrt.corpus.* (the corpus-io interface's own AR-2 directional rule) --
  so the one source of truth for this lenient reader moves down to
  where both callers can reach it, rather than being duplicated.
  ehrt.corpus.player re-exports the same four public names unchanged
  (parse-dtm-lenient, message-timestamp-ms, message-type-trigger,
  message-patient-id), so every existing caller and test is
  byte-identical, unmodified.

  Distinct from ehrt.corpus-io.er7 (the strict, round-trip-exact
  mutation substrate): that namespace never drops a token (limit -1
  splits throughout, an exact parse/serialize inverse pair); this one
  reads a single named field leniently, with no full parse tree and no
  round-trip obligation -- two different jobs over the same wire
  format, kept in two namespaces rather than one doing both."
  (:require [clojure.string :as str])
  (:import [java.time LocalDateTime ZoneOffset]))

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
  ehrt.corpus-io.framing's own message-internal segment split."
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
