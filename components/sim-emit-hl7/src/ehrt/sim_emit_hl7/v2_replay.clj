(ns ehrt.sim-emit-hl7.v2-replay
  "Task 2 (M6): the v2-replay accumulator -- parses a run's own emitted
  ER7 stream (the SAME org.clojars.cmiles74/clojure-hl7-parser structures
  ehrt.sim-emit-hl7.emit-hl7 renders through, plus that namespace's own
  unescape-er7) and folds it through `fold-message`, an INDEPENDENT
  reconstruction of patient state built ONLY from what a real downstream
  consumer could ever see on the wire -- never touching ground-truth,
  the engine, or the RNG. This is the wire-side half of the global
  emitter-coherence law (docs/sim-theory.md): at every message boundary,
  this accumulator's own state must agree with ehrt.sim.engine's
  log-folded state, once both are passed through
  `project-to-wire-visible-fields` (below) -- the SAME projection
  applied to both sides, so 'what does the wire carry' is answered once,
  not maintained as two separately-hand-tuned shapes.

  Keyed by :active-mrn, not :patient-id -- the wire's only identity.
  Bootstrap-from-empty: a patient's first message self-initializes
  (every message carries full PID enrichment, ehrt.sim-emit-hl7.emit-hl7's
  own uniform-PID law), so no separate 'this mrn is new' step is needed.

  Scope boundary, documented not silent (the same 'deferred with a
  contract note' treatment EmitState's own CDA arm gets): bed-swap (A17)
  and merge (A40) are genuinely two-participant messages -- two PID/PV1
  pairs in ONE message, and merge additionally REASSIGNS which mrn is
  active mid-run. Reconstructing wire identity across either is real,
  separate engineering scope. `fold-message` raises a clear
  `:unsupported-trigger` on either, rather than silently mis-folding --
  ehrt.sim-emit-hl7.v2-replay-test's own property runs churn EXCLUDING
  both for exactly this reason."
  (:require [clojure.string :as str]
            [com.nervestaple.hl7-parser.parser :as parser]
            [com.nervestaple.hl7-parser.message :as message]
            [ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]))

;; --- Reading the pinned clock backwards ------------------------------------

(def ^:private hl7-datetime-formatter
  (java.time.format.DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))

(defn- hl7-instant->seconds
  "The inverse of ehrt.sim-emit-hl7.emit-hl7/hl7-timestamp: an HL7
  timestamp (\"yyyyMMddHHmmss+ZZZZ\") anchored to `reference-date` ->
  seconds since that reference instant. Only the first 14 characters
  (the naive local timestamp) are read -- the zone suffix is always the
  SAME pinned :utc-offset every message in one run shares, never
  per-event, so it carries no information the fold needs (sim/ADR-0011)."
  [reference-date ts]
  (let [local (java.time.LocalDateTime/parse (subs ts 0 14) hl7-datetime-formatter)
        reference (.atStartOfDay (java.time.LocalDate/parse reference-date))]
    (.getSeconds (java.time.Duration/between reference local))))

(defn- hl7-date->iso
  "\"yyyyMMdd\" -> \"yyyy-MM-dd\" -- the inverse of
  ehrt.sim-emit-hl7.emit-hl7/pid-segment's own dash-stripping."
  [raw]
  (str (subs raw 0 4) "-" (subs raw 4 6) "-" (subs raw 6 8)))

;; --- Safe field/component access -------------------------------------------

(defn- component
  "`get-field-component` is 0-based on its OWN component-index argument
  (unlike every field-index argument in this library, which is 1-based
  -- confirmed against the library directly, not assumed) and throws
  IndexOutOfBoundsException past a field's actual component count (a
  short/blank field, e.g. no bed on an outpatient PV1-3) rather than
  returning nil. Normalized here to nil-safe, blank-safe access, and to
  a plain string regardless of whether the library hands back a bare
  value or a single-element seq (observed both, depending on the
  underlying field's own shape)."
  [parsed segment-id field-index component-index]
  (let [v (try (message/get-field-component parsed segment-id field-index component-index)
               (catch IndexOutOfBoundsException _ nil))
        v (if (sequential? v) (first v) v)]
    (when (seq v) (emit-hl7/unescape-er7 (str v)))))

(defn- blank->nil [s] (when (seq s) s))

;; --- PID/PV1/IN1 reconstruction ---------------------------------------------

(defn- parse-persona
  "Every message carries this uniformly (ehrt.sim-emit-hl7.emit-hl7/
  pid-segment's own docstring) -- PID-13/:ssn/:age are never rendered,
  the same exclusion `project-to-wire-visible-fields` states from the
  ground-truth side."
  [parsed]
  {:name {:family (component parsed "PID" 5 0) :given (component parsed "PID" 5 1)}
   :sex (case (blank->nil (message/get-field-first-value parsed "PID" 8)) "F" :female "M" :male nil)
   :dob (hl7-date->iso (message/get-field-first-value parsed "PID" 7))
   :address {:street (component parsed "PID" 11 0) :city (component parsed "PID" 11 2)
             :state (component parsed "PID" 11 3) :zip (component parsed "PID" 11 4)}
   :phone (blank->nil (message/get-field-first-value parsed "PID" 13))})

(defn- parse-payer
  "IN1 rides admission alone (ehrt.sim-emit-hl7.emit-hl7/in1-segment's own
  docstring) -- nil on every other message, the wire-side mirror of
  `project-to-wire-visible-fields`'s own admitted-at gate."
  [parsed]
  (when (seq (message/get-segments parsed "IN1"))
    {:id (blank->nil (message/get-field-first-value parsed "IN1" 3))
     :name (some-> (message/get-field-first-value parsed "IN1" 4) emit-hl7/unescape-er7)}))

(defn- parse-location
  [parsed]
  (when-let [ward (component parsed "PV1" 3 0)]
    {:ward ward :bed (component parsed "PV1" 3 2)}))

(defn- parse-attending
  [parsed]
  (component parsed "PV1" 7 0))

(def ^:private hl7-class->keyword {"I" :inpatient "O" :outpatient})

(defn- parse-class
  [parsed]
  (get hl7-class->keyword (blank->nil (message/get-field-first-value parsed "PV1" 2))))

;; --- OBX/observations reconstruction ----------------------------------------

(defn- seg-field-components
  "The full component vector at STANDARD (1-based) HL7 `field-index` of
  one already-fetched segment (ehrt.sim-emit-hl7.v2-replay's own callers
  use this for OBX repetitions -- message/get-segments returns one
  segment per analyte; the whole-message get-field-component API
  addresses only 'the first' repetition). `get-segment-field`'s own
  indexing is the SAME convention its own docstring states for the
  whole-message API's MSH gotcha, confirmed directly (not assumed):
  index 0 is the segment's own 3-letter id; field N is OBX-N in real
  HL7 numbering, e.g. index 3 is OBX-3 (the concept), never a 0-based
  position into the field vector."
  [segment field-index]
  (try (message/get-segment-field segment field-index)
       (catch IndexOutOfBoundsException _ nil)))

(defn- seg-field
  [segment field-index]
  (first (seg-field-components segment field-index)))

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

(def unsupported-triggers
  "This accumulator's own documented scope boundary (this namespace's
  own header comment) -- the genuinely two-participant message family."
  #{"A17" "A40"})

(defn- initial-entry
  [parsed]
  {:active-mrn (message/get-field-first-value parsed "PID" 3)
   :persona (parse-persona parsed)})

(defn- evolve-entry
  "(entry, trigger, parsed, t) -> entry'. Pure and total, mirroring
  ehrt.sim.engine/evolve's own shape one layer up the wire --
  dispatch on the message's own trigger, never mutate anything but the
  ONE mrn-keyed entry this message is about."
  [entry trigger parsed t]
  (case trigger
    "A01" (cond-> (assoc entry :status :admitted :class :inpatient
                         :location (parse-location parsed) :attending (parse-attending parsed)
                         :admitted-at t)
            (parse-payer parsed) (update :persona assoc :payer (parse-payer parsed)))
    "A04" (assoc entry :status :admitted :class :outpatient :attending (parse-attending parsed))
    "A02" (assoc entry :location (parse-location parsed) :attending (parse-attending parsed))
    "A03" (assoc entry :status :discharged :location nil :discharged-at t)
    "A11" (-> entry (assoc :status :new) (dissoc :class :location :attending :admitted-at))
    "A12" (assoc entry :location (parse-location parsed))
    "A13" (-> entry (assoc :status :admitted :class (or (parse-class parsed) (:class entry))
                           :location (parse-location parsed) :attending (parse-attending parsed))
              (dissoc :discharged-at))
    "O01" entry
    "R01" (update entry :observations (fnil into []) (parse-observations parsed t))
    (throw (ex-info "v2-replay: unsupported message trigger (documented scope boundary)"
                    {:trigger trigger}))))

(defn fold-message
  "acc x message x reference-date -> acc'. Parses `message`, extracts
  its own trigger (MSH-9) and instant (MSH-7, via `hl7-instant->seconds`),
  and folds it into `acc`'s entry for that message's own PID-3 -- a
  never-yet-seen mrn self-initializes (bootstrap-from-empty)."
  [acc message reference-date]
  (let [parsed (parser/parse message)
        [_type trigger] (str/split (message/get-field-first-value parsed "MSH" 9) #"\^")
        t (hl7-instant->seconds reference-date (message/get-field-first-value parsed "MSH" 7))
        mrn (message/get-field-first-value parsed "PID" 3)
        entry (or (get acc mrn) (initial-entry parsed))]
    (assoc acc mrn (evolve-entry entry trigger parsed t))))

(defn replay-messages
  "The stage function: a run's own emitted ER7 stream -> {mrn ->
  reconstructed-state}, folded left to right via `fold-message`."
  [messages reference-date]
  (reduce #(fold-message %1 %2 reference-date) {} messages))

;; --- The projection function: the formal definition of what the wire
;; carries (M6 Task 2's own deliverable, sibling of ehrt.sim.
;; site-profile's masking function) ------------------------------------------

(defn project-to-wire-visible-fields
  "Projects a folded PatientState (ehrt.sim.engine's own
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
