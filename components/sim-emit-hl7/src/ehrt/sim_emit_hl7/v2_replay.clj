(ns ehrt.sim-emit-hl7.v2-replay
  "Task 2 (M6): the v2-replay accumulator -- parses a run's own emitted
  ER7 stream (the SAME org.clojars.cmiles74/clojure-hl7-parser structures
  ehrt.sim-emit-hl7.emit-hl7 renders through, plus that namespace's own
  unescape-er7) and folds it through `fold-message`, an INDEPENDENT
  reconstruction of patient state built ONLY from what a real downstream
  consumer could ever see on the wire -- never touching ground-truth,
  the engine, or the RNG. This is the wire-side half of the global
  emitter-coherence law (docs/sim-theory.md): at every message boundary,
  this accumulator's own state must agree with ehrt.sim-engine.engine's
  log-folded state, once both are passed through
  `project-to-wire-visible-fields` (below) -- the SAME projection
  applied to both sides, so 'what does the wire carry' is answered once,
  not maintained as two separately-hand-tuned shapes.

  Keyed by :active-mrn, not :patient-id -- the wire's only identity.
  Bootstrap-from-empty: a patient's first message self-initializes
  (every message carries full PID enrichment, ehrt.sim-emit-hl7.emit-hl7's
  own uniform-PID law), so no separate 'this mrn is new' step is needed.

  Two-participant messages, supported (player fold, ADR-0066, AR-BB1-1/2):
  bed-swap (A17) carries two PID/PV1 pairs in ONE message -- `fold-message`
  walks the message's own segments in order, pairing each PID with its
  own immediately-following PV1, and folds each pair independently as a
  location/attending update onto that PID-3's own entry (the A02
  treatment, per pair). Merge (A40) carries the surviving mrn on PID-3
  and the merged-away mrn on MRG-1 -- the surviving entry absorbs (a
  no-op on the wire, since PV1 rides blank on A40 and :mrns is truth-
  only, never wire-visible); the merged-away entry becomes a tombstone
  (`:status :merged`, every other field held over unchanged), mirroring
  ehrt.sim-engine.engine's own `evolve :merge` `:merged` arm exactly --
  the wire-side fold is NOT an independent invention of this shape.
  Bootstrap-from-empty holds for both: a never-seen mrn on either side of
  an A17/A40 self-initializes (foreign traffic may open mid-stream).

  General time (player fold, ADR-0066, AR-BB1-4): MSH-7 (and, via the
  same `t`, every observation's own :t) parses to an ABSOLUTE epoch
  instant read from the wire alone -- honoring an explicit trailing
  offset (±ZZZZ or Z) when present, treating a naive timestamp as
  UTC otherwise, lenient on truncated precision the same way
  ehrt.corpus.player/parse-dtm-lenient already is. No reference-date
  parameter travels through this namespace anymore -- a real downstream
  consumer never has one either."
  (:require [clojure.string :as str]
            [com.nervestaple.hl7-parser.parser :as parser]
            [com.nervestaple.hl7-parser.message :as message]
            [ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]))

;; --- General time: an HL7 DTM read on its own terms (player fold,
;; ADR-0066, AR-BB1-4) -- no reference-date, no pinned per-run offset
;; assumption; the wire is the only source of truth. ------------------

(def ^:private dtm-prefix-pattern
  "The SAME lenient YYYY[MM[DD[HH[MI[SS]]]]] prefix contract
  ehrt.corpus.player/parse-dtm-lenient already implements (read there,
  aligned here, never extracted across the component boundary this
  session -- a shared helper, if ever wanted, is later scope,
  disclosed as a finding not taken)."
  #"^(\d{4})(\d{2})?(\d{2})?(\d{2})?(\d{2})?(\d{2})?")

(def ^:private dtm-offset-pattern
  "A trailing explicit zone offset (a sign followed by 4 digits, e.g.
  \"+0000\"/\"-0500\") or the bare \"Z\" -- searched anywhere after the
  date/time prefix (a fractional-seconds component, if present, may
  sit between the two), anchored to the string's own end so it never
  mismatches into the middle of a longer field."
  #"([+-]\d{4}|Z)$")

(defn- hl7-instant->millis
  "An HL7 timestamp -> absolute epoch milliseconds, read from the wire
  alone: an explicit trailing offset is honored when present, a naive
  timestamp is treated as UTC otherwise. nil for nil/blank/garbage that
  doesn't even start with a 4-digit year (parse-dtm-lenient's own
  contract, matched here)."
  [ts]
  (when (and ts (seq ts))
    (when-let [[_ y mo d h mi s] (re-find dtm-prefix-pattern ts)]
      (try
        (let [local (java.time.LocalDateTime/of (Integer/parseInt y)
                                                  (if mo (Integer/parseInt mo) 1)
                                                  (if d (Integer/parseInt d) 1)
                                                  (if h (Integer/parseInt h) 0)
                                                  (if mi (Integer/parseInt mi) 0)
                                                  (if s (Integer/parseInt s) 0))
              offset (if-let [[_ raw] (re-find dtm-offset-pattern ts)]
                       (if (= raw "Z")
                         java.time.ZoneOffset/UTC
                         (java.time.ZoneOffset/of (str (subs raw 0 3) ":" (subs raw 3))))
                       java.time.ZoneOffset/UTC)]
          (.toEpochMilli (.toInstant (.atZone local offset))))
        (catch Exception _ nil)))))

(defn- hl7-date->iso
  "\"yyyyMMdd\" -> \"yyyy-MM-dd\" -- the inverse of
  ehrt.sim-emit-hl7.emit-hl7/pid-segment's own dash-stripping.

  nil/BLANK IN, nil OUT (arc 3a part 4, 2026-08-26). PID-7 was
  unconditionally populated until the identification flow landed: a
  PLACEHOLDER registration renders an empty PID-7, because an
  unidentified patient has no known date of birth and the wire may not
  invent one. This function threw a NullPointerException on the first
  such message, which reached a user as `ehrt play` dying mid-stream on
  a real corpus -- found by replaying `demos/scenarios/clinic-decade`
  after its own opt-in, not by reasoning about it. Every other reader
  in this namespace was already nil- and blank-safe; this was the one
  that was not."
  [raw]
  (when (and raw (>= (count raw) 8))
    (str (subs raw 0 4) "-" (subs raw 4 6) "-" (subs raw 6 8))))

;; --- Safe field/component access -------------------------------------------

(defn- seg-field-components
  "The full component vector at STANDARD (1-based) HL7 `field-index` of
  one already-fetched segment -- `message/get-segments` returns one
  segment per repetition/participant (OBX's own analytes, A17's own two
  PID/PV1 pairs), so this is the ONE primitive every segment-scoped
  reader in this namespace builds on, never the whole-message
  `get-field-component` API, which flattens across EVERY same-id
  segment in the message and would silently mis-index a multi-segment
  message (confirmed against the library directly: `get-field` maps
  `get-segment-field` over `get-segments`, then `get-field-component`
  flattens the result before indexing -- exactly the mis-indexing this
  namespace's own A17 pairing must not risk). `get-segment-field`'s own
  indexing is the SAME convention its own docstring states for the
  whole-message API's MSH gotcha, confirmed directly (not assumed):
  index 0 is the segment's own 3-letter id; field N is field N in real
  HL7 numbering, e.g. index 3 is OBX-3 (the concept), never a 0-based
  position into the field vector. nil-safe on a nil `segment` too
  (confirmed against the library's own `get-segment-field`: `(:id nil)`
  and `(count (:fields nil))` both resolve to nil/0, never throw) --
  every reader below stays total even when a message carries no PV1 at
  all (e.g. A03/A11/R01)."
  [segment field-index]
  (try (message/get-segment-field segment field-index)
       (catch IndexOutOfBoundsException _ nil)))

(defn- seg-field
  [segment field-index]
  (first (seg-field-components segment field-index)))

(defn- segment-component
  "The component-index equivalent of `seg-field`, at STANDARD (1-based)
  HL7 `field-index`, 0-based `component-index` -- confirmed against the
  library directly, not assumed. Normalized to nil-safe, blank-safe
  access, and to a plain string regardless of whether the library hands
  back a bare value or a single-element seq (observed both, depending
  on the underlying field's own shape)."
  [segment field-index component-index]
  (let [v (nth (seg-field-components segment field-index) component-index nil)
        v (if (sequential? v) (first v) v)]
    (when (seq v) (emit-hl7/unescape-er7 (str v)))))

(defn- first-segment
  [parsed segment-id]
  (first (message/get-segments parsed segment-id)))

(defn- blank->nil [s] (when (seq s) s))

;; --- PID/PV1/IN1 reconstruction: every reader below is SEGMENT-scoped
;; (never whole-message-scoped) so the SAME reader serves both a
;; single-PID/PV1 message and one pair out of A17's own two -----------------

(defn- parse-persona
  "Every message carries this uniformly (ehrt.sim-emit-hl7.emit-hl7/
  pid-segment's own docstring) -- PID-13/:ssn/:age are never rendered,
  the same exclusion `project-to-wire-visible-fields` states from the
  ground-truth side."
  [pid-seg]
  {:name {:family (segment-component pid-seg 5 0) :given (segment-component pid-seg 5 1)}
   :sex (case (blank->nil (seg-field pid-seg 8)) "F" :female "M" :male nil)
   :dob (hl7-date->iso (seg-field pid-seg 7))
   :address {:street (segment-component pid-seg 11 0) :city (segment-component pid-seg 11 2)
             :state (segment-component pid-seg 11 3) :zip (segment-component pid-seg 11 4)}
   :phone (blank->nil (seg-field pid-seg 13))})

(defn- parse-payer
  "IN1 rides admission alone (ehrt.sim-emit-hl7.emit-hl7/in1-segment's own
  docstring) -- nil on every other message, the wire-side mirror of
  `project-to-wire-visible-fields`'s own admitted-at gate. Whole-message
  scoped (unlike every reader above): IN1 is never repeated in a message
  this emitter renders (A17/A40 carry none at all)."
  [parsed]
  (when (seq (message/get-segments parsed "IN1"))
    {:id (blank->nil (message/get-field-first-value parsed "IN1" 3))
     :name (some-> (message/get-field-first-value parsed "IN1" 4) emit-hl7/unescape-er7)}))

(defn- parse-location
  [pv1-seg]
  (when-let [ward (segment-component pv1-seg 3 0)]
    {:ward ward :bed (segment-component pv1-seg 3 2)}))

(defn- parse-attending
  [pv1-seg]
  (segment-component pv1-seg 7 0))

(def ^:private hl7-class->keyword {"I" :inpatient "O" :outpatient})

(defn- parse-class
  [pv1-seg]
  (get hl7-class->keyword (blank->nil (seg-field pv1-seg 2))))

(defn- parse-range
  [raw]
  (when (seq raw)
    (let [[lo hi] (str/split raw #"-")]
      {:low (Double/parseDouble lo) :high (Double/parseDouble hi)})))

(def ^:private hl7-flag->keyword {"N" :normal "L" :low "H" :high})

(defn- parse-obx
  "OBX-3 concept (code^display^system, a CWE field -- the SECOND
  component is the concept's own display text, dropped by a plain
  `seg-field` first-component read), OBX-5 value, OBX-6 units, OBX-7
  reference range, OBX-8 abnormal flag -- ehrt.sim-emit-hl7.emit-hl7/
  obx-segment's own field layout, standard HL7 numbering."
  [t seg]
  (let [[code display] (seg-field-components seg 3)]
    (cond-> {:codes [(cond-> {:system :loinc :code code} (seq display) (assoc :display display))] :t t}
      (blank->nil (seg-field seg 5)) (assoc :value (Double/parseDouble (seg-field seg 5)))
      (blank->nil (seg-field seg 6)) (assoc :unit (seg-field seg 6))
      (blank->nil (seg-field seg 7)) (assoc :reference-range (parse-range (seg-field seg 7)))
      (blank->nil (seg-field seg 8)) (assoc :interpretation (get hl7-flag->keyword (seg-field seg 8))))))

(defn- parse-observations
  [parsed t]
  (mapv (partial parse-obx t) (message/get-segments parsed "OBX")))

;; --- fold-message: one ER7 message -> the accumulator's next state --------

(defn- initial-entry
  [pid-seg]
  {:active-mrn (seg-field pid-seg 3)
   :persona (parse-persona pid-seg)})

(defn- evolve-entry
  "(entry, trigger, parsed, t) -> entry'. Pure and total, mirroring
  ehrt.sim-engine.engine/evolve's own shape one layer up the wire --
  dispatch on the message's own trigger, never mutate anything but the
  ONE mrn-keyed entry this message is about. Single-PID/PV1 triggers
  only -- A17/A40 (genuinely two-participant) are dispatched by
  `fold-message` directly to `fold-bed-swap`/`fold-merge`, never here."
  [entry trigger parsed t]
  (let [pv1-seg (first-segment parsed "PV1")]
    (case trigger
      "A01" (cond-> (assoc entry :status :admitted :class :inpatient
                           :location (parse-location pv1-seg) :attending (parse-attending pv1-seg)
                           :admitted-at t)
              (parse-payer parsed) (update :persona assoc :payer (parse-payer parsed)))
      "A04" (assoc entry :status :admitted :class :outpatient :attending (parse-attending pv1-seg))
      "A02" (assoc entry :location (parse-location pv1-seg) :attending (parse-attending pv1-seg))
      "A03" (assoc entry :status :discharged :location nil :discharged-at t)
      "A11" (-> entry (assoc :status :new) (dissoc :class :location :attending :admitted-at))
      "A12" (assoc entry :location (parse-location pv1-seg))
      "A13" (-> entry (assoc :status :admitted :class (or (parse-class pv1-seg) (:class entry))
                             :location (parse-location pv1-seg) :attending (parse-attending pv1-seg))
                (dissoc :discharged-at))
      "O01" entry
      "R01" (update entry :observations (fnil into []) (parse-observations parsed t))
      (throw (ex-info "v2-replay: unsupported message trigger" {:trigger trigger})))))

(defn- pid-pv1-pairs
  "Walks `parsed`'s own segments IN ORDER, pairing each PID with its own
  immediately-following PV1 -- A17's own two PID/PV1 pairs (ADR-0066,
  AR-BB1-1). Every other segment (MSH/EVN leading, Z-segments trailing)
  is simply not paired, never aborts the walk -- a message with fewer
  than two well-formed pairs yields fewer than two pairs, no error --
  `fold-bed-swap` folds whatever pairs are actually present."
  [parsed]
  (loop [segs (:segments parsed) pairs []]
    (cond
      (empty? segs) pairs
      (and (= "PID" (:id (first segs))) (= "PV1" (:id (second segs))))
      (recur (drop 2 segs) (conj pairs [(first segs) (second segs)]))
      :else (recur (rest segs) pairs))))

(defn- fold-bed-swap
  "A17: each PID/PV1 pair folds independently onto that pair's OWN
  PID-3 -- the A02 treatment (location/attending), per pair.
  Bootstrap-from-empty holds per pair (never-seen MRNs self-initialize
  from that pair's own PID, exactly like every other trigger)."
  [acc parsed]
  (reduce
   (fn [acc [pid-seg pv1-seg]]
     (let [mrn (seg-field pid-seg 3)
           entry (or (get acc mrn) (initial-entry pid-seg))]
       (assoc acc mrn (assoc entry :location (parse-location pv1-seg) :attending (parse-attending pv1-seg)))))
   acc (pid-pv1-pairs parsed)))

(defn- fold-merge
  "A40: PID-3 carries the surviving mrn, MRG-1 the merged-away one
  (mirroring ehrt.sim-emit-hl7.emit-hl7/merge-message's own docstring).
  The surviving entry absorbs -- bootstrap-or-keep, unchanged otherwise,
  since PV1 rides blank on A40 and :mrns is truth-only (never
  wire-visible). The merged-away entry becomes a tombstone: `:status
  :merged`, every other field held over unchanged -- the wire-side
  mirror of ehrt.sim-engine.engine's own `evolve :merge` `:merged` arm,
  which touches `:status` alone. A merged-away mrn never seen before
  (no persona to bootstrap from -- MRG-1 carries only the mrn string)
  still gets a minimal tombstone; the engine's own merge eligibility
  rule (`never-mergeable?`) means this path is never exercised by
  legal traffic, only defensive for a genuinely foreign stream."
  [acc parsed]
  (let [pid-seg (first-segment parsed "PID")
        survivor-mrn (seg-field pid-seg 3)
        merged-mrn (message/get-field-first-value parsed "MRG" 1)
        survivor-entry (or (get acc survivor-mrn) (initial-entry pid-seg))
        merged-entry (-> (or (get acc merged-mrn) {:active-mrn merged-mrn})
                         (assoc :status :merged))]
    (-> acc
        (assoc survivor-mrn survivor-entry)
        (assoc merged-mrn merged-entry))))

(def beds-key
  "The one non-MRN key this accumulator carries: bed-id -> {:ward
  :status :at}, from the A20 stream (arc 3b sweep 2, ADR-0174 ruling C).

  NAMESPACED ON PURPOSE. `acc` is otherwise `{mrn -> entry}` and every
  reader of it -- `ehrt.corpus.board`'s own tally and ward grouping, the
  emitter-coherence property's per-participant lookup -- keys by MRN. A
  namespaced keyword cannot collide with an MRN string, and an entry
  under it carries no `:location` and no `:status` from the patient
  vocabulary, so every one of those readers passes over it unchanged.
  A stream with no A20 in it never grows the key at all."
  ::beds)

(def ^:private hl7-bed-status->keyword
  "NPU-2 (HL7v2 Table 0116) back to the engine's own four bed states --
  the inverse of `ehrt.sim-emit-hl7.site-profile/standard-bed-status-
  codes`, which is where the forward mapping and its reasoning live.

  A code outside this map is kept as its own RAW STRING rather than
  dropped: a real feed, or this project under a site profile that
  overrides the table, may legitimately send one, and the board would
  rather render an unknown status than lose the bed."
  {"O" :occupied "U" :ready "K" :dirty "H" :cleaning})

(defn- fold-bed-status
  "An A20 folded: `[MSH EVN NPU]`, no PID and no PV1, so this is the one
  trigger that touches no patient entry at all. NPU-1 is the bed's PL --
  the same `ward^^bed^facility` shape PV1-3 carries, read with the same
  component reader -- and NPU-2 is the status."
  [acc parsed t]
  (let [npu (first-segment parsed "NPU")
        ward (segment-component npu 1 0)
        bed (segment-component npu 1 2)
        raw (blank->nil (seg-field npu 2))
        status (get hl7-bed-status->keyword raw raw)]
    (if (nil? bed)
      acc
      (assoc-in acc [beds-key bed] {:ward ward :status status :at t}))))

(defn fold-message
  "acc x message -> acc'. Parses `message`, extracts its own trigger
  (MSH-9) and instant (MSH-7, via `hl7-instant->millis` -- an absolute
  epoch instant, no reference-date), and folds it into `acc`. A17/A40
  are genuinely two-participant and dispatched directly to
  `fold-bed-swap`/`fold-merge`; A20 names a BED and no patient at all
  and goes to `fold-bed-status`; every other trigger folds onto `acc`'s
  entry for that message's own PID-3 -- a never-yet-seen mrn
  self-initializes (bootstrap-from-empty)."
  [acc message]
  (let [parsed (parser/parse message)
        [_type trigger] (str/split (message/get-field-first-value parsed "MSH" 9) #"\^")
        t (hl7-instant->millis (message/get-field-first-value parsed "MSH" 7))]
    (case trigger
      "A17" (fold-bed-swap acc parsed)
      "A40" (fold-merge acc parsed)
      "A20" (fold-bed-status acc parsed t)
      (let [pid-seg (first-segment parsed "PID")
            mrn (seg-field pid-seg 3)
            entry (or (get acc mrn) (initial-entry pid-seg))]
        (assoc acc mrn (evolve-entry entry trigger parsed t))))))

(defn replay-messages
  "The stage function: a run's own emitted ER7 stream -> {mrn ->
  reconstructed-state}, folded left to right via `fold-message`."
  [messages]
  (reduce fold-message {} messages))

;; --- The projection function: the formal definition of what the wire
;; carries (M6 Task 2's own deliverable, sibling of ehrt.sim.
;; site-profile's masking function) ------------------------------------------

(defn project-to-wire-visible-fields
  "Projects a folded PatientState (ehrt.sim-engine.engine's own
  accumulator, OR this namespace's own reconstructed entry -- the SAME
  function applies to both sides of the emitter-coherence property, by
  design, so the comparison is never two independently hand-tuned
  shapes) down to exactly what ehrt.sim-emit-hl7.emit-hl7's own rendering
  choices make visible on the wire.

  Excluded, and why -- each a real, load-bearing reason, not merely
  'not implemented yet':
  - :patient-id -- an internal identifier this project's own emitter
    never renders anywhere; the wire's only identity is :active-mrn.
  - :mrns (the full set), :home-ward -- SimHospital-lesson truth-only
    facts (docs/patient-state-model.md): a real feed shows only the
    CURRENT mrn/location, never the administrative intent behind a
    boarding assignment.
  - :location's own :placement -- no PV1 field distinguishes licensed
    from surge (docs/operational-models.md); only the physical
    ward/bed strings are wire-visible.
  - :conditions, :medication-orders -- ehrt.sim-emit-hl7.emit-hl7 renders
    NO segment for either (DG1 is gated on the snomed-icd10-map
    catalytic, docs/sim-theory.md's Catalytic resolution table, not
    built; :medication-order/:medication-end carry no message-type-
    registry entry at all, that namespace's own comment). Genuinely
    truth-only, not a gap this property should paper over.
  - :persona's own :ssn/:age -- PID never carries either
    (ehrt.sim-emit-hl7.emit-hl7/pid-segment's own fixed field list).
  - :persona's own :payer -- wire-visible ONLY once an :admission
    message has actually carried IN1 (IN1 rides admission alone) --
    gated here on `:admitted-at` being non-nil, the real wire's own
    gate, not a static field selector.
  - Every :observations entry's :codes is narrowed to its FIRST concept
    only, with :system normalized to :loinc -- a genuine, PRE-EXISTING
    ehrt.sim-emit-hl7.emit-hl7 finding, not introduced by this property:
    `cwe-field` renders every CWE field's 3rd component as the literal
    \"LN\" regardless of a concept's actual :system, and
    `observation-obx-segment` reads only `(first codes)`, for ANY
    observation. Triaged as the emitter under-rendering, not this
    projection over-claiming -- recorded here as a documented scope
    boundary (a real future ehrt.sim-emit-hl7.emit-hl7 fix, out of this
    session's own test-first seam), not silently patched over.
  - Events with no message-type-registry entry at all (:step-rejected,
    :registered's own pre-horizon facts, :outpatient-visit-end's own
    missing closing message, :procedure) never generate a message
    boundary for this property to check -- a structural consequence of
    the property iterating ONLY over
    ehrt.sim-emit-hl7.emit-hl7/message-type-registry-covered events,
    exactly the boundary set a real downstream consumer would ever see,
    not a separate exclusion this function has to state."
  [{:keys [active-mrn status class location attending admitted-at discharged-at persona observations]}]
  (cond-> {:active-mrn active-mrn :status status}
    class (assoc :class class)
    location (assoc :location (select-keys location [:ward :bed]))
    attending (assoc :attending attending)
    admitted-at (assoc :admitted-at admitted-at)
    discharged-at (assoc :discharged-at discharged-at)
    persona (assoc :persona
                   (cond-> (select-keys persona [:name :sex :dob :address :phone])
                     (and admitted-at (:payer persona)) (assoc :payer (select-keys (:payer persona) [:id :name]))))
    (seq observations)
    (assoc :observations
           (mapv (fn [{:keys [codes t value unit reference-range interpretation]}]
                   (cond-> {:codes (when (seq codes) [(assoc (select-keys (first codes) [:code :display]) :system :loinc)])
                            :t t}
                     (some? value) (assoc :value (double value))
                     unit (assoc :unit unit)
                     reference-range (assoc :reference-range {:low (double (:low reference-range))
                                                               :high (double (:high reference-range))})
                     interpretation (assoc :interpretation interpretation)))
                 observations))))
