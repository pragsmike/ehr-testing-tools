(ns ehrt.sim-emit-hl7.hl7-time
  "The emitter's reference clock: the pinned :reference-date and
  :utc-offset defaults, MSH-7's own timestamp rendering, and ADR-0109's
  second-clock shift.

  Extracted VERBATIM from `emit_hl7.clj`, the FIRST cluster of that
  file's namespace extraction (`.agents/plans/engine-extraction-
  census.md` sections 2 and 2a, `roadmap.md#engine-namespace-extraction-
  and-apply-unification`). It is a LEAF: nothing here calls anything
  else in the emitter, which is what made it the first move.

  `emit_hl7.clj` remains the namespace every existing requirer resolves
  against (author ruling C1(a)) and keeps a delegating def of each of
  the three public forms below. `transmit-seconds` is the one widening
  this move forced: eleven forms that stayed behind call it, so it is
  public here and `defn-` no longer -- and it gains NO delegating def,
  because widening `emit_hl7.clj`'s own public surface is not what
  C1(a) asks for."
  (:require [clojure.string :as str]))

(def default-reference-date
  "Pinned default for the :reference-date run-config input (an ISO
  date string, midnight local time is the run's t=0). Arbitrary but
  fixed -- determinism does not care which date, only that every run
  states one explicitly (never buried in :invocation, per tools'
  ManifestV1 lesson)."
  "2024-01-01")

(def default-utc-offset
  "Pinned default for the :utc-offset run-config input (`sim/ADR-0011`): a
  fixed ISO-style offset (\"+00:00\"), no DST, no timezone database.
  Rendered in HL7v2's own colon-free zone-suffix convention
  (\"+0000\") -- see `hl7-timestamp`."
  "+00:00")

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
  SECONDS from the run's epoch -- sim/ADR-0011, was minutes before M2a)
  anchored to :reference-date, suffixed with :utc-offset in HL7's own
  colon-free zone convention -- the timestamp-anchoring law, extended
  to state which fixed offset the naive wall-clock arithmetic is
  asserted to be in (no timezone database, no DST: the arithmetic
  itself never shifts across zones, sim/ADR-0011). Pure: reference-date +
  seconds + utc-offset in, string out, nothing else consulted."
  [reference-date seconds utc-offset]
  (str (.format (.plusSeconds (reference-instant reference-date) seconds) hl7-timestamp-formatter)
       (hl7-offset-suffix utc-offset)))

(defn transmit-seconds
  "ADR-0109's own second clock: `t` (the event's clinical instant, log-
  relative seconds) shifted by `offsets`' own entry for this event's
  `control-id`, or unshifted (offset 0) when `control-id` has no entry
  -- absent/nil/{} `offsets` is therefore the identity input for every
  event, the mechanism the identity property (emit-hl7-test) rests on.
  `offsets` is plain data here (never an RNG) -- sampling stays out of
  emit, per the emitter's own renders-only doctrine (docs/dev/
  simulator-architecture.md section 5); `plan-latency` is the one place
  offsets are ever sampled, upstream of this function."
  [offsets control-id t]
  (+ t (long (get offsets control-id 0))))
