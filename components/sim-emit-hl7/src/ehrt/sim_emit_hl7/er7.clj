(ns ehrt.sim-emit-hl7.er7
  "The emitter's escaping and primitive-field layer: ER7's five-delimiter
  escape table and its encoder, the decode map and its single-pass
  decoder, the XPN/XAD/TN/CWE primitive renderers and their coded and
  blank siblings, the location and provider field composers, and the
  four forms that render a site profile's own Z-segment templates.

  Extracted VERBATIM from `emit_hl7.clj`, the FOURTH cluster of that
  file's namespace extraction (`.agents/plans/engine-extraction-
  census.md` sections 2 and 2a, `roadmap.md#engine-namespace-extraction-
  and-apply-unification`). It is the FIRST cluster of this file that is
  NOT a leaf, and the first anywhere in the emitter to depend on a
  SIBLING extraction rather than on `emit_hl7.clj`: `context-for-event`
  calls `demographics-at`, so `ehrt.sim-emit-hl7.timelines` -- the third
  cluster, already landed -- comes with it. That is the ONE cross-cluster
  edge; the other two requires are the HL7 parser and `clojure.string`,
  and no `:import` is owed, `money` naming `java.math.RoundingMode` in
  full.

  `emit_hl7.clj` remains the namespace every existing requirer resolves
  against (author ruling C1(a)) and keeps a delegating def of the TWO
  public movers below, `escape-er7` and `unescape-er7`. `interface.clj`
  re-exports neither: this is the first cluster of this file whose defs
  are owed to the TREE alone -- `v2_replay.clj`'s two reader call sites
  and four `emit_hl7_test.clj` sites.

  ELEVEN of the seventeen private movers are WIDENINGS -- `xpn-field`,
  `xad-field`, `tn-field`, `location-field`, `provider-field`,
  `provider-by-id`, `blank-fields`, `z-segments-for`, `cwe-field`,
  `coded-value-field` and `money`. Callers of every one of them stayed
  behind -- forty-one call sites across eighteen forms, eight in
  `segments` and ten in `messages` -- so all eleven are public here and
  `defn-` no longer, and none gains a delegating def, because widening
  `emit_hl7.clj`'s own public surface is not what C1(a) asks for.

  The other SIX stay PRIVATE, which is the first time this shape has
  arisen in the emitter: `er7-escape-table`, `er7-decode-map`,
  `context-for-event`, `render-z-field`, `z-segment-for` and
  `code-system->hl7-table-0396` have no caller outside this cluster at
  all -- every one of their callers travelled -- so census constraint 5
  read as `engine.clj`'s `weighted-pick` read it leaves them unwidened.

  `tn-field` is the one mover that is BOTH a widening and owed a def
  back. `v2_replay_test.clj` reaches it as `(#'emit-hl7/tn-field phone)`,
  a var access on a private var that no move can carry and that C1(a)
  forbids editing, so `emit_hl7.clj` keeps a `^:private` delegating def
  of it -- which leaves that file's public surface exactly the size it
  was.

  Three sentences of the moved prose stopped being true at the seam and
  are corrected in the move commit rather than a commit later. The Task 4
  header said the encode sites were BELOW it; two of the five --
  `in1-segment`'s payer name and `sch-segment`'s reason -- stayed behind,
  so the word is dropped. `render-z-field` said THIS NAMESPACE rendered
  every free-text field, and for the same two it does not, so it now says
  the emitter. `z-segments-for` said its call sites were BELOW; all eight
  stayed behind, so that word is dropped too. Nothing else differs,
  across 193 form-lines."
  (:require [com.nervestaple.hl7-parser.parser :as parser]
            [clojure.string :as str]
            [ehrt.sim-emit-hl7.timelines :as timelines]))

;; --- M4 Task 4: ER7 escaping (`sim/F9`) -----------------
;; org.clojars.cmiles74/clojure-hl7-parser implements NO escape-sequence
;; handling in either direction, verified directly against its own source:
;; pr-field/pr-content (the write path) concatenate field content into the
;; wire string with no encoding step at all; read-text's and read-
;; subcomponents' escape-handling branches (the read path) are commented-out
;; dead code, and `delimiter?` doesn't even exempt the escape character from
;; ending a token early. A literal |^~& character embedded in free text
;; therefore corrupts the message's own field/component boundaries on parse
;; unless something upstream escapes it, and even a properly-escaped value
;; comes back from `get-field-first-value` STILL escaped, never decoded.
;; escape-er7/unescape-er7 are this repo's own documented workaround:
;; encode on write (at every persona-derived free-text field), decode
;; on read (a consumer's own job, exactly like this repo's test suite does).

(def ^:private er7-escape-table
  "Order matters on ENCODE: the escape character itself is escaped
  FIRST, or the backslashes this table's own replacements introduce
  for |^~& would themselves get escaped a second time on a later pass."
  [[\\ "\\E\\"] [\| "\\F\\"] [\^ "\\S\\"] [\~ "\\R\\"] [\& "\\T\\"]])

(defn escape-er7
  "Encodes ER7's five reserved delimiter characters per the standard
  escape-sequence convention. Identity for any string containing none
  of the five -- the overwhelmingly common case (ordinary names,
  apostrophes, and hyphens need no escaping at all, ER7 or otherwise).
  Safe as five sequential single-CHARACTER replacements (unlike decode,
  below): each pass targets one literal input character never produced
  by an earlier pass's own replacement text (F/S/R/T/E are never
  themselves |^~&), so passes cannot collide."
  [s]
  (reduce (fn [acc [ch replacement]] (str/replace acc (str ch) replacement))
          s er7-escape-table))

(def ^:private er7-decode-map
  {\E \\ \F \| \S \^ \R \~ \T \&})

(defn unescape-er7
  "Decodes ER7 escape sequences back to literal characters -- the
  consumer-side half of this namespace's own documented workaround for
  the parser's read-side gap (see this section's header comment).

  MUST be a single regex pass, not five sequential string replacements
  the way `escape-er7` is -- a property-test failure caught exactly
  this during Milestone M4's own authoring: encoding \"|E|\" produces
  \"\\F\\E\\F\\\" (backslash F backslash E backslash F backslash), and
  five SEPARATE global replaces are each blind to what the others
  already consumed, so the first pass (decoding \\E\\ back to a literal
  backslash) spuriously matches the backslash-E-backslash formed by the
  BOUNDARY between the two adjacent, unrelated \\F\\ tokens -- decoding
  it wrong. A single regex scan matches real three-character tokens
  left to right, consuming each match's characters before continuing,
  so two adjacent tokens can never accidentally spell a third."
  [s]
  (str/replace s #"\\[EFRST]\\" (fn [^String match] (str (er7-decode-map (.charAt match 1))))))

(defn xpn-field
  "XPN (Extended Person Name), PID-5: family^given. Free text from
  ehrt.sim-model.persona -- escaped per ER7 (see this file's Task 4
  section) before it ever reaches a field, since the library itself
  never will."
  [{:keys [family given]}]
  (parser/create-field [(escape-er7 family) (escape-er7 given)]))

(defn xad-field
  "XAD (Extended Address), PID-11: street^other-designation^city^state^zip.
  Other-designation (apt/suite) is always empty -- resources/demographics'
  vendored places carry no such field, same simplification the address
  table's own header notes. Free text escaped per ER7, same reasoning
  as `xpn-field`."
  [{:keys [street city state zip]}]
  (parser/create-field [(escape-er7 street) "" (escape-er7 city) (escape-er7 state) (escape-er7 zip)]))

(defn tn-field
  "TN (Telephone Number), PID-13. The persona's own `:phone` is
  `NNN-NNN-NNNN` (`ehrt.sim-model.persona`'s contract regex,
  `^\\d{3}-\\d{3}-\\d{4}$`); the WIRE carries `(NNN)NNN-NNNN`.

  ARC 4 SWEEP 1 (`notes/adr/0175-arc-4-emission-add-ons.md` ruling A1,
  commit 1 of 2). This is a conformance fact, not a formatting
  preference. HAPI's v2.4 TN primitive rule wants the parenthesised
  area code, and `PipeParser` enforces primitives DURING the parse
  rather than after -- so at MSH-12 \"2.4\" the persona's own shape does
  not produce a warning, it throws, and the message resolves to no
  structure at all. ADR-0175 section 2(e) measured it: 346 of the probe
  corpus's 747 messages died exactly here, and with this one field
  reformatted all 747 resolve into real v2.4 structures. Probed
  directly against the vendored jar: `(303)292-0567` OK,
  `(303)292-0567X1234` OK, `\"\"` OK; `492-292-0567`, `3032920567` and
  `(303) 292-0567` all FAIL.

  GROUND TRUTH DOES NOT MOVE. `persona`'s regex and its three phone
  draws are untouched, and `bin/ground-truth-bracket` proves that
  per commit rather than this docstring asserting it. Rendering is
  where a wire convention belongs -- the same seam PID-11's XAD and
  PID-7's date already use.

  A phone that does NOT match the contract shape renders VERBATIM
  rather than being mangled into a guess. Every persona this emitter
  has ever been handed matches, so this branch moves no existing
  message; it exists because a silent reformat of an unrecognised
  value would be a worse failure than passing it through."
  [phone]
  (parser/create-field
   [(str/replace phone #"^(\d{3})-(\d{3})-(\d{4})$" "($1)$2-$3")]))

(defn location-field
  "Renders a location map as ward^^bed^facility (PV1-3/PV1-6's shared
  shape, docs/operational-models.md's transfer/A02 spec: 'PV1-3 renders
  ward^^bed with facility in PV1-3.4'). nil location (no prior, or a
  v0 event with no location at all) -> an empty field, same as v0's
  own nil-location handling."
  [facility-name location]
  (if-let [ward (:ward location)]
    (parser/create-field [ward "" (or (:bed location) "") facility-name])
    (parser/create-field [])))

(defn provider-field
  "PV1-7: id^family^given. nil provider -> empty field."
  [provider]
  (if provider
    (parser/create-field [(:id provider) (get-in provider [:name :family]) (get-in provider [:name :given])])
    (parser/create-field [])))

(defn provider-by-id
  [providers id]
  (first (filter #(= id (:id %)) providers)))

(defn blank-fields
  [n]
  (repeat n (parser/create-field [])))

;; --- Milestone site-profiles Task 3: Z-segment templates -- THE SEAM -----
;; A site's fully custom fields (docs/site-profiles.md), bound declaratively
;; to state/persona/event paths rather than hard-coded engine knowledge of
;; what any particular site's Z-segment means.

(defn- context-for-event
  "The per-render lookup context a Z-segment template's `:path` bindings
  resolve against (get-in): the event map itself (so [:location :ward],
  [:attending], [:t], etc. all resolve directly, the same paths
  docs/site-profiles.md's own examples name) plus :persona -- looked up
  off the SAME `demographics` map `emit` computes once per call, the primary
  (first) participant's persona for a genuinely multi-participant event
  (bed-swap, merge), the same simplification `ehrt.sim-engine.fold/
  replay`'s own :patient-id convenience view already makes."
  [demographics event]
  (assoc event :persona (timelines/demographics-at demographics
                                         (:patient-id (first (:participants event)))
                                         (:t event))))

(defn- render-z-field
  "One Z-segment field: `:path` looked up (get-in -- nil-safe through a
  missing intermediate map, so an unbound path never throws) in
  `context`, `:literal` as a fixed fallback, an EMPTY field when
  neither resolves to a value -- Task 3's own never-throw requirement.
  Escaped per ER7, same as every other free-text-carrying field the
  emitter renders (persona names, addresses, payer names)."
  [context {:keys [path literal]}]
  (let [value (if path (get-in context path) literal)
        rendered (cond (nil? value) nil (keyword? value) (name value) :else (str value))]
    (parser/create-field (if rendered [(escape-er7 rendered)] []))))

(defn- z-segment-for
  [context template]
  (apply parser/create-segment (:segment template)
         (mapv (partial render-z-field context) (:fields template))))

(defn z-segments-for
  "0+ rendered Z-segments for `event` -- one per `site-profile`'s own
  :z-segments template whose :trigger set names this event's :event, in
  the profile's own template order. Rendered AFTER every standard
  segment at every call site (Task 3's own ordering requirement,
  achieved by `concat`-ing this vector onto the end of each message's
  segment list). No site-profile, or a profile with no :z-segments,
  renders none -- an empty vector `concat`s as a no-op, so absent-
  profile output is untouched by this function's existence.

  ADR-0150 (2026-08-18): every call site hands this the WHOLE event.
  `single-subject-message` used to synthesize a seven-key subset
  ({:event :t :active-mrn :location :from :attending :participants})
  while the six other families passed `ev`, so an ADT-family template
  bound to `:reason`, `:home-ward`, `:disposition`, `:warm-up` or any
  other key rendered empty -- and silently, `render-z-field` treating
  an unbound path and an unreachable one identically. The context a
  template resolves against is now the same map on every family, which
  is the only shape `docs/site-profiles.md`'s own path examples can be
  read literally against."
  [site-profile demographics event]
  (let [context (context-for-event demographics event)]
    (into []
          (comp (filter #(contains? (:trigger %) (:event event)))
                (map (partial z-segment-for context)))
          (:z-segments site-profile))))

(defn cwe-field
  "CWE (Coded With Exceptions): identifier^text^coding-system. \"LN\" is
  LOINC's own HL7v2 Table 0396 coding-system abbreviation -- the coded-
  triplet's :system rendered natively (sim/ADR-0002), not translated.
  Every existing call site (OBR-4/OBX-3) is always a LOINC panel/
  analyte concept, so this stays hardcoded -- `coded-value-field`,
  below, is the system-aware sibling a value_code-sourced OBX-5 needs."
  [{:keys [code display]}]
  (parser/create-field [code display "LN"]))

;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P6): a value_code-
;; sourced observation (Blood_Cultures' own embedded child, Capillary_
;; Refill) is this project's first field ever to carry a SNOMED CT-coded
;; VALUE, not just a LOINC-coded concept -- `cwe-field` itself stays
;; LOINC-hardcoded and untouched.
(def ^:private code-system->hl7-table-0396
  "HL7v2 Table 0396 coding-system abbreviations for sim-model/Concept's
  own :system vocabulary (sim/ADR-0002)."
  ;; ARC 4 SWEEP 2 (ADR-0175 design (c)): `:local` joins for the ONE
  ;; charge line no log fact carries a code for -- the per-inpatient-day
  ;; room-and-board line, whose code this project mints for itself
  ;; (`room-and-board-code`). "L" is Table 0396's own abbreviation for a
  ;; local general code. It is additive: no Concept anywhere in this
  ;; tree carries `:system :local`, so no existing field moves.
  {:loinc "LN" :snomed "SCT" :rxnorm "RXNORM" :icd10cm "I10" :cvx "CVX" :local "L"})

(defn coded-value-field
  "OBX-5 for a value_code-sourced observation: identifier^text^coding-
  system, the SAME CWE shape `cwe-field` renders for OBR-4/OBX-3, but
  system-aware."
  [{:keys [system code display]}]
  (parser/create-field [code display (get code-system->hl7-table-0396 system (name system))]))

(defn money
  "A price rendered for the wire. BigDecimal at scale 2, never
  `String/format`: `%.2f` reads the DEFAULT LOCALE for its decimal
  separator, so a host configured for de-DE would render `1800,00` and
  the corpus would stop being a function of its own inputs. Determinism
  is law here for the same reason it is in the engine."
  [amount]
  (.toPlainString (.setScale (bigdec amount) 2 java.math.RoundingMode/HALF_UP)))
