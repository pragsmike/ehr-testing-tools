(ns ehrt.corpus.board
  "The bed board (ADR-0014's own deferred surface, player board session
  2, `notes/ADRs.md` ADR-0067): folds a paced HL7 v2 stream into the
  SAME accumulator `ehrt.sim-emit-hl7.v2-replay/fold-message` already
  builds for the emitter-coherence property, and renders a state
  snapshot from it. `corpus` is this interface's first real external
  caller (AR-BB2-1) -- `fold-event` below is the call site the AR-6
  grep discipline now names.

  `fold-event` turns a genuinely foreign trigger (outside the
  emitter's own handled set -- a real feed's A08, A05, ...) into a
  counted, cued skip rather than a crash: the fold itself stays STRICT
  (`ehrt.sim-emit-hl7.v2-replay`'s own scope boundary, unchanged here),
  it is this caller that decides a foreign message is a display-layer
  event, not a fatal one (the pacer's own cue rule, ADR-0014).

  `render-snapshot` is pure: acc x instant-ms -> string, no clock, no
  IO -- the executor (bases/cli's board sink) supplies both."
  (:require [clojure.string :as str]
            [ehrt.sim-emit-hl7.interface :as sim-emit-hl7]))

(defn fold-event
  "acc x message -> {:acc acc' :unfolded? bool}. Wraps
  ehrt.sim-emit-hl7.interface/fold-message: a message whose own trigger
  is outside the emitter's handled set throws there (documented scope
  boundary, an ExceptionInfo carrying :trigger) -- caught here and
  reported as an unfolded skip (acc returned UNCHANGED) rather than
  propagated. Any other exception (a genuinely malformed message) is
  not this fn's concern and propagates unchanged."
  [acc message]
  (try
    {:acc (sim-emit-hl7/fold-message acc message) :unfolded? false}
    (catch clojure.lang.ExceptionInfo e
      (if (contains? (ex-data e) :trigger)
        {:acc acc :unfolded? true}
        (throw e)))))

(defn- occupied?
  "A merge tombstone (`:status :merged`) can still carry the
  merged-away mrn's own stale :location -- `fold-merge` absorbs into
  the survivor but never clears the merged-away entry's other fields
  (the wire-side mirror of the engine's own `:merged` arm, which
  touches :status alone). A tombstone is never occupying a bed
  regardless -- checked here, not just at the tally, live-probe-caught
  (player board, `notes/ADRs.md` ADR-0067)."
  [entry]
  (and (some? (:location entry)) (not= :merged (:status entry))))

(defn- patient-name
  [entry]
  (let [{:keys [family given]} (:name (:persona entry))]
    (str family (when (seq given) (str ", " given)))))

(defn- bed-line
  "A bootstrapped-from-A02 entry (foreign traffic opening mid-stream on
  a transfer alone, never an admit -- the SAME bootstrap-from-empty
  posture `ehrt.sim-emit-hl7.v2-replay` itself takes) can occupy a bed
  with no :class ever set; rendered as \"?\", the same never-throw
  leniency the ticker's own compact line already uses for a field it
  can't read, never a crash."
  [mrn entry]
  (let [{:keys [bed]} (:location entry)]
    (str "  " bed "  " (patient-name entry) "  MRN " mrn
         "  " (if-let [class (:class entry)] (name class) "?")
         (when-let [attending (:attending entry)] (str "  attending: " attending)))))

(defn- ward-block
  [ward entries]
  (str ward ":\n"
       (str/join "\n" (map (fn [[mrn entry]] (bed-line mrn entry))
                            (sort-by (fn [[_ entry]] (:bed (:location entry))) entries)))))

(defn- ward-groups
  "acc's own occupied entries, grouped by ward (sorted), each group's
  own [mrn entry] pairs sorted by bed within `ward-block`."
  [acc]
  (->> acc
       (filter (fn [[_ entry]] (occupied? entry)))
       (group-by (fn [[_ entry]] (:ward (:location entry))))
       (into (sorted-map))))

(defn- tally-line
  [acc]
  (let [entries (vals acc)
        active? (fn [entry] (not (#{:discharged :merged} (:status entry))))
        of-class (fn [class] (count (filter #(and (= class (:class %)) (active? %)) entries)))]
    (str "inpatients: " (of-class :inpatient)
         "  active outpatients: " (of-class :outpatient)
         "  discharged: " (count (filter #(= :discharged (:status %)) entries))
         "  merged: " (count (filter #(= :merged (:status %)) entries)))))

(defn render-snapshot
  "acc (the fold-event accumulator) x instant-ms (the snapshot's own
  absolute epoch millis) -> a rendered whiteboard string: a header
  naming the snapshot instant (ISO-8601, UTC), occupied beds grouped
  by ward (wards sorted, beds sorted within), one line per patient,
  then a one-line tally (inpatients / active outpatients / discharged
  / merged -- tombstones are counted here, never listed as occupying a
  bed). Operator voice throughout -- no citation, milestone, or
  internal-namespace token anywhere in the rendered text."
  [acc instant-ms]
  (let [header (str "-- board snapshot: " (java.time.Instant/ofEpochMilli instant-ms) " --")
        blocks (map (fn [[ward entries]] (ward-block ward entries)) (ward-groups acc))]
    (str/join "\n\n" (concat [header] blocks [(tally-line acc)]))))
