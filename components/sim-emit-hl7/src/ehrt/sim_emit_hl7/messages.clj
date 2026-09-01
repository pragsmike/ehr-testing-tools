(ns ehrt.sim-emit-hl7.messages
  "The emitter's message layer: the twelve per-kind message builders --
  single-subject ADT, bed swap, bed status, SIU, merge, ORM, ORU, the
  :observation and :diagnostic-report ORUs, DFT, chatter and ladder
  rungs -- plus `event->messages`, the dispatcher that renders one
  ground-truth event to a vector of 0+ ER7 strings.

  Extracted VERBATIM from `emit_hl7.clj`, the SIXTH cluster of that
  file's namespace extraction (`.agents/plans/engine-extraction-
  census.md` sections 2 and 2a, `roadmap.md#engine-namespace-extraction-
  and-apply-unification`). It is the HEAVIEST cluster in the file --
  thirteen forms, 578 form-lines -- and the most connected: 122 distinct
  cross-seam calls into FIVE landed siblings, `segments` (62),
  `hl7-time` (21), `er7` (16), `registry` (13) and `timelines` (10),
  which are census 3b's five `messages`-as-caller rows reproduced
  exactly. The HL7 parser comes with it; `clojure.string`, `sim-model`
  and `site-profile` do not, and no `:import` is owed.

  Because five siblings had already landed, SIXTY-FOUR bare names in
  the moved text resolved only through `emit_hl7.clj`'s own delegating
  defs and had to be requalified to their real homes:
  `hl7-time/hl7-timestamp` (20), `segments/msh-segment` (11),
  `segments/pid-segment` (11), `segments/control-id-for` (10),
  `registry/message-type-registry` (10), `registry/siu-event-kinds` and
  `registry/siu-renders?`. Cluster 5 opened that class at five names and
  five sites; a facade may require its implementations but an
  implementation may not require its facade, so the deeper a cluster
  sits the more of it there is.

  TEN of the twelve private movers STAY PRIVATE, the largest such set
  in the EMITTER -- the engine's `decide` cluster left eighteen of
  nineteen, and is the only larger one. Every caller of
  `single-subject-message`, `bed-swap-message`, `bed-status-message`,
  `siu-message`, `merge-message`, `orm-message`, `oru-message`,
  `observation-message`, `diagnostic-report-message` and `dft-message`
  travelled with them -- `event->messages` calls all ten and
  `ladder-message` two of them -- so census constraint 5, read as a
  PROHIBITION, leaves every one unwidened.

  `emit_hl7.clj` remains the namespace every existing requirer resolves
  against (author ruling C1(a)) and keeps ONE delegating def:
  `event->messages`, the cluster's only public mover. It is owed to the
  TREE rather than to `interface.clj`, which re-exports none of the
  thirteen -- `emit_hl7_test.clj` calls it at five sites and
  `sim-engine`'s `bed_cycle_test.clj` at one. `chatter-message` and
  `ladder-message` widen instead of keeping a def, because `emit-wire`
  stayed behind and calls both; their two call sites there now name
  them `messages/...`. NO `^:private` def is owed: all 106 `#'` sites
  in the tracked tree were re-read and none names a mover, and neither
  does any `resolve`/`with-redefs`/`alter-var-root` form anywhere.

  FIVE BANNER BLOCKS TRAVEL -- the exact four cluster 5 left behind
  because their builders stayed, plus the D1 ORC+OBR note over the
  diagnostic report. Each now heads a section wholly this cluster's:
  SIU^S12, M3 (ORM^O01 + ORU^R01), M5b (:observation), that note, and
  ARC 4 SWEEP 2 (DFT^P03). The moved prose carries no `below`, no `this
  file` and no `this namespace`; its sixteen positional words all
  resolve inside their own form or inside the travelling set, so unlike
  cluster 5's the prose travels UNTOUCHED. Nothing in these 578
  form-lines differs from `emit_hl7.clj` but the sixty-four
  requalifications and two `defn-` markers."
  (:require [com.nervestaple.hl7-parser.parser :as parser]
            [ehrt.sim-emit-hl7.hl7-time :as hl7-time]
            [ehrt.sim-emit-hl7.registry :as registry]
            [ehrt.sim-emit-hl7.timelines :as timelines]
            [ehrt.sim-emit-hl7.er7 :as er7]
            [ehrt.sim-emit-hl7.segments :as segments]))

(defn- single-subject-message
  "Renders one single-participant ground-truth event to an ER7 string,
  or nil when the event's :event isn't in `message-type-registry`. Every
  type this covers (:admission/:discharge/:transfer and M2b's cancel-*
  family) carries its own :active-mrn/:location/:from/:attending
  directly -- cancel events reinstate these AT DECIDE-TIME by querying
  the log (docs/patient-state-model.md), so this renderer needs no
  event-type-specific branching to show the reinstated facts. M4: IN1
  rides ONLY :admission (`in1-segment`'s own docstring); every type
  here gets PID enrichment uniformly via `demographics`. Milestone
  site-profiles: PV1-36 disposition rides ONLY :discharge (the same
  single-event-type gate IN1 already established for admission), and
  every segment renders through `site-profile`'s own dialect/code-table/
  Z-segment surfaces -- `z-segments-for`'s own docstring.

  ADR-0109's split clock (this session's own field audit, docs/dev/
  simulator-architecture.md section 5 / notes/adr/0109-*.md): MSH-7 is
  TRANSMIT time (`clinical-ts` shifted by `offsets`), EVN-2 is CLINICAL
  time (`clinical-ts`, unshifted) -- the only two timestamp-bearing
  fields this builder ever renders. `offsets` is {} at every plain-
  `emit` call site, so `transmit-ts` = `clinical-ts` always there,
  byte-identical to this builder's pre-ADR-0109 shape."
  [reference-date utc-offset facility providers demographics site-profile offsets
   {:keys [event t active-mrn location from attending participants] :as ev}]
  (when-let [type+trigger (registry/message-type-registry event)]
    (let [control-id (segments/control-id-for ev)
          clinical-ts (hl7-time/hl7-timestamp reference-date t utc-offset)
          transmit-ts (hl7-time/hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
          facility-name (name (:id facility))
          provider (er7/provider-by-id providers attending)
          persona (timelines/demographics-at demographics (:patient-id (first participants)) t)
          disposition-state (when (= :discharge event) :discharged-to-home)
          ;; M5b: the only two event types this project ever renders
          ;; :outpatient for -- every other type here is still :inpatient
          ;; (this project's own sole class before this milestone).
          patient-class (if (#{:outpatient-visit :outpatient-visit-end} event) :outpatient :inpatient)]
      (parser/str-message
       (apply parser/create-message
        parser/DEFAULT-DELIMITERS
        (segments/msh-segment site-profile type+trigger control-id transmit-ts)
        (segments/evn-segment (:trigger type+trigger) clinical-ts)
        (segments/pid-segment active-mrn persona)
        (segments/pv1-segment site-profile patient-class facility-name location from provider disposition-state
                     (:encounter-id ev))
        (concat (when (and (= :admission event) (:payer persona))
                  [(segments/in1-segment (:payer persona))])
                (er7/z-segments-for site-profile demographics ev)))))))

(defn- bed-swap-message
  "A17 (swap patients): ONE message per ground-truth event, carrying
  BOTH patients' PID/PV1 pairs -- the real HL7v2 A17 shape, and why the
  emitter-derivability law now keys on the event's own log position
  rather than a single :active-mrn (a bed-swap message has two).
  ADR-0109's split clock (see `single-subject-message`'s own docstring):
  ONE `control-id` covers the whole event (`control-id-for`'s own
  :bed-swap arm), so both patients' PID/PV1 pairs ride the SAME
  transmit-shifted MSH-7 and the SAME unshifted EVN-2."
  [reference-date utc-offset facility providers demographics site-profile offsets
   {:keys [t participants swap] :as ev}]
  (let [type+trigger (registry/message-type-registry :bed-swap)
        control-id (segments/control-id-for ev)
        clinical-ts (hl7-time/hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-time/hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        [p1 p2] (mapv :patient-id participants)
        ;; ARC 3B SWEEP 1: PV1-19 comes from the same per-side entry
        ;; PV1-3 does -- a two-patient event names two encounters and
        ;; carries neither at top level (`BedSwapSide`'s own docstring).
        {mrn1 :active-mrn from1 :from to1 :to att1 :attending enc1 :encounter-id} (get swap p1)
        {mrn2 :active-mrn from2 :from to2 :to att2 :attending enc2 :encounter-id} (get swap p2)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (segments/msh-segment site-profile type+trigger control-id transmit-ts)
      (segments/evn-segment (:trigger type+trigger) clinical-ts)
      (segments/pid-segment mrn1 (timelines/demographics-at demographics p1 t))
      (segments/pv1-segment site-profile :inpatient facility-name to1 from1 (er7/provider-by-id providers att1) nil enc1)
      (segments/pid-segment mrn2 (timelines/demographics-at demographics p2 t))
      (segments/pv1-segment site-profile :inpatient facility-name to2 from2 (er7/provider-by-id providers att2) nil enc2)
      (er7/z-segments-for site-profile demographics ev)))))

(defn- bed-status-message
  "A20 (bed status update): `[MSH EVN NPU]`, and NOTHING ELSE -- no PID,
  no PV1. This is the first message this project emits that names no
  patient, which is why it is a sibling of `single-subject-message`
  rather than a branch inside it: that builder's contract is a PID/PV1
  pair per subject, and an A20 has no subject to pair.

  THE STRUCTURE IS VERIFIED FROM THIS TREE'S OWN RESOLVED DEPENDENCIES,
  not from memory. `components/judge-v2-hapi/deps.edn` pulls
  `ca.uhn.hapi/hapi-structures-v24` 2.6.0; that jar carries
  `ca/uhn/hl7v2/model/v24/message/ADT_A20.class` and
  `ca/uhn/hl7v2/model/v24/segment/NPU.class`, and A20 does NOT appear in
  `ca/uhn/hl7v2/parser/eventmap/2.4.properties` -- i.e. it is not
  aliased onto another structure, it has its own. NPU's two fields are
  NPU-1 `PL` (the datatype PV1-3 already renders) and NPU-2 `IS`.

  AND THE VERSION QUESTION, NOW SETTLED -- this paragraph is kept and
  amended rather than deleted, because what it could NOT check is the
  reason the question was worth asking.

  It used to read: MSH-12 stays `\"2.3\"`, and A20-in-2.3 is the
  AUTHOR'S RULING (ADR-0174 ruling C) rather than something this clone
  can verify, because there is NO 2.3 trigger table here -- the only
  structure library on any classpath is v2.4, `~/.m2` holds
  `hapi-structures-v24` alone, and neither `hapi-base` nor any resource
  in this repository carries a 2.3 eventmap. All of that is still true
  of 2.3.

  WHAT CHANGED (arc 4 sweep 1, ADR-0175 ruling A1, 2026-08-27):
  `site-profile/default-msh` declares `\"2.4\"`. The unverifiable claim
  is retired rather than answered -- this message family no longer
  needs A20-in-2.3 to be legal, because it no longer says 2.3. What was
  checkable all along (A20 in 2.4, own structure, not aliased in
  `2.4.properties`) is now what the version field actually declares,
  and `ehrt.conformance.v2-structure-resolution-test` asserts the
  resolution over a whole corpus rather than this docstring asserting
  it. A site that must speak 2.3 keeps `{:msh {:version \"2.3\"}}` and
  inherits the same unverifiability, knowingly."
  [reference-date utc-offset facility _providers _demographics site-profile offsets
   {:keys [t bed ward to] :as ev}]
  (let [type+trigger (registry/message-type-registry :bed-status-change)
        control-id (segments/control-id-for ev)
        clinical-ts (hl7-time/hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-time/hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))]
    (parser/str-message
     (parser/create-message
      parser/DEFAULT-DELIMITERS
      (segments/msh-segment site-profile type+trigger control-id transmit-ts)
      (segments/evn-segment (:trigger type+trigger) clinical-ts)
      (segments/npu-segment site-profile facility-name {:ward ward :bed bed} to)))))


;; --- ARC 4 SWEEP 4 (ADR-0175 ruling B1, 2026-08-28): SIU^S12 -----------
;; Scheduling's four ground-truth kinds reach the wire, behind `:siu`.

(defn- siu-message
  "SIU^S12 (and S14/S15/S26, all one structure): `[MSH SCH PID (PV1)]`,
  plus whatever Z-segments the site profile binds to this event.

  A SIBLING BUILDER, NOT A BRANCH IN `single-subject-message`, and the
  reason is not A20's. A20 had no patient at all; SIU has one, so the
  PID/PV1 pair that builder's contract is built around DOES exist here.
  It still cannot share it, on three structural counts, each of which
  alone would force a branch: SIU carries NO EVN (that segment is ADT's,
  and `single-subject-message` renders it unconditionally); its SCH sits
  BEFORE the PID, and that builder has no seam ahead of the patient; and
  its PV1 is CONDITIONAL, while that builder always renders one. Three
  branches inside a builder whose docstring says `a PID/PV1 pair per
  subject` is a different builder wearing the first one's name.

  PV1 RIDES ONLY AN OPEN ENCOUNTER, and that is the whole of the
  condition: `:encounter-id` is stamped on any event that happened while
  an encounter was open (`engine/stamp-encounter`), so a booking made
  from a hospital bed would carry one and a booking made from home does
  not. When PV1 is rendered it is the CURRENT encounter's, carrying its
  visit number in PV1-19; PV1-3/PV1-6/PV1-7 are blank because an
  appointment names no location and no attending.

  NO RUN IN THIS REPOSITORY REACHES THAT BRANCH TODAY, and it is
  measured rather than hoped: `:encounter-id` is on ZERO of the 72
  appointment-family events of the `scheduling` oracle root, zero of
  seed-202-ed-tuesday's 64, and zero of seed-424242-clinic-decade's 56.
  IT IS STRUCTURAL, not a seed's luck. Both of this project's two
  booking producers decide OUTSIDE an open encounter: the pre-loop books
  an arrival before that patient has any encounter at all, and a
  follow-up is a step `decide :discharge` PREPENDS, so its own `decide
  :appointment` runs after the discharge it followed has already closed
  the encounter (`engine/stamp-encounter` reads the world BEFORE the
  batch, and by then there is none open).

  THE BRANCH IS KEPT ANYWAY, and it is exercised by a hand-built event
  rather than by a population (`siu-test`), which is the honest form for
  a rule that is right and currently unreachable. The alternative --
  rendering PV1 unconditionally -- would put a visit segment on a
  notification about a visit that has not happened, on every S12 this
  project emits. The population fact is itself gated: no SIU message of
  any real run carries a PV1, asserted with this measurement as its
  reason, so the day the engine starts stamping a mid-stay booking the
  gate says so instead of the rendering changing silently.

  MSH-7 IS TRANSMIT TIME, the same split clock every builder here
  follows -- and unlike a ladder rung, an SIU has no basis message to
  borrow a lag from: it is its own event, so it takes its own event's
  offset under `control-id-for`'s SIU arm."
  [reference-date utc-offset facility _providers demographics site-profile offsets
   {:keys [event t active-mrn participants encounter-id scheduled-t] :as ev}]
  (let [type+trigger (registry/message-type-registry event)
        control-id (segments/control-id-for ev)
        transmit-ts (hl7-time/hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        persona (timelines/demographics-at demographics (:patient-id (first participants)) t)
        scheduled-ts (when scheduled-t (hl7-time/hl7-timestamp reference-date scheduled-t utc-offset))]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (segments/msh-segment site-profile type+trigger control-id transmit-ts)
      (segments/sch-segment site-profile ev scheduled-ts)
      (segments/pid-segment active-mrn persona)
      (concat (when encounter-id
                [(segments/pv1-segment site-profile :inpatient facility-name nil nil nil nil encounter-id)])
              (er7/z-segments-for site-profile demographics ev))))))

(defn- merge-message
  "A40 (merge patient): PID carries the SURVIVING mrn, MRG-1 carries the
  prior (merged-away) one (docs/patient-state-model.md's identity
  payoff) -- ONE message per merge event. ADR-0109's split clock (see
  `single-subject-message`'s own docstring): `control-id-for`'s own
  :merge arm keys on the surviving mrn."
  [reference-date utc-offset facility _providers demographics site-profile offsets
   {:keys [t surviving-mrn merged-mrn participants] :as ev}]
  (let [type+trigger (registry/message-type-registry :merge)
        control-id (segments/control-id-for ev)
        clinical-ts (hl7-time/hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-time/hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        survivor-id (:patient-id (first (filter #(= :survivor (:role %)) participants)))]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (segments/msh-segment site-profile type+trigger control-id transmit-ts)
      (segments/evn-segment (:trigger type+trigger) clinical-ts)
      (segments/pid-segment surviving-mrn (timelines/demographics-at demographics survivor-id t))
      (segments/pv1-segment site-profile :inpatient facility-name nil nil nil nil (:encounter-id ev))
      (segments/mrg-segment merged-mrn)
      (er7/z-segments-for site-profile demographics ev)))))

;; --- M3: ORM^O01 + ORU^R01 (docs/sim-theory.edn's order-profiles
;; catalytic, docs/operational-models.md) -----------------------------------


(defn- orm-message
  "ORM^O01: order placed. No EVN segment -- EVN is an ADT-specific
  segment (HL7v2 convention), not part of the order-message family.
  ADR-0109's split clock: MSH-7 is this builder's ONLY timestamp field
  (ADR-0109's field audit: OBR-7 and ORC-9, HL7v2's own clinical-time
  candidates for this message family, are not rendered by
  `orc-segment`/`obr-segment` at all), so it is unconditionally
  transmit time -- there is no clinical-time field here to keep
  unshifted.

  STILL TRUE after ADR-0142 (2026-08-16), and deliberately so. That
  session gave `obr-segment` a 3-arity rendering OBR-7 and put it on
  all three ORU shapes; this builder keeps calling the 2-arity, so
  ORM^O01 stays BYTE-FROZEN. An order's clinical-time story is a
  different question from a result's -- OBR-7 on an order would mean
  specimen/observation time for an observation that has not happened
  yet, and ORC-9 (transaction time) is the field that would actually
  be owed -- and it is a named revisit, not a silent ride-along.
  Author ruling Q3, 2026-08-16: \"Results only; ORM byte-frozen.\"

  ARC 4 SWEEP 3 (ADR-0175 design (b)) adds an optional `status` --
  `{:stage <keyword> :control-id <this rung's own> :basis-control-id
  <the order event's>}` -- and BYTE-FREEZE SURVIVES IT, because nil
  status is every pre-ladder call site: an `:order-placed` event still
  renders exactly what it rendered yesterday. What a status buys is the
  ORM^O01 RUNG: the same builder, over the same order event with `:t`
  replaced by the rung's own instant, with ORC-5 filled in.

  THE OFFSET IS LOOKED UP UNDER `:basis-control-id`, the ORDER's own
  control id, never the rung's -- sweep 2's DFT finding
  (`.agents/session-records/2026-08-28-arc-4-sweep-2-chatter-charges.md`
  finding 2) generalised: a derived message rides its basis event's own
  lag, and keying on an id no `:latency` profile mints would silently
  give the rung a zero offset and let it overtake the order it restates.
  Since the rung's instant is strictly AFTER the order's and both carry
  the same offset, a rung can never precede its own ORM^O01."
  ([reference-date utc-offset facility providers demographics site-profile offsets ev]
   (orm-message reference-date utc-offset facility providers demographics site-profile offsets ev nil))
  ([reference-date utc-offset facility providers demographics site-profile offsets
    {:keys [t active-mrn location attending concept participants] :as ev}
    {:keys [stage] :as status}]
   (let [type+trigger (registry/message-type-registry :order-placed)
         control-id (or (:control-id status) (segments/control-id-for ev))
         transmit-ts (hl7-time/hl7-timestamp reference-date
                                    (hl7-time/transmit-seconds offsets
                                                      (or (:basis-control-id status) control-id)
                                                      t)
                                    utc-offset)
         facility-name (name (:id facility))
         provider (er7/provider-by-id providers attending)]
     (parser/str-message
      (apply parser/create-message
       parser/DEFAULT-DELIMITERS
       (segments/msh-segment site-profile type+trigger control-id transmit-ts)
       (segments/pid-segment active-mrn (timelines/demographics-at demographics (:patient-id (first participants)) t))
       (segments/pv1-segment site-profile :inpatient facility-name location nil provider nil (:encounter-id ev))
       (if stage (segments/orc-segment control-id site-profile stage) (segments/orc-segment control-id))
       (segments/obr-segment 1 concept)
       (er7/z-segments-for site-profile demographics ev))))))

(defn- oru-message
  "ORU^R01: result available -- OBR (order context) plus one OBX per
  analyte, in the same order the profile's own :results carries them
  (derived straight from the log, ehrt.sim-engine.order-profiles'
  sampling order -- no re-sorting here).

  ADR-0109's split clock, CORRECTED IN PLACE 2026-08-16 (ADR-0142):
  ADR-0109's field audit recorded OBR-7 and OBX-14 as not rendered by
  `obr-segment`/`obx-segment` at all, which made MSH-7 this builder's
  only timestamp field and therefore unconditionally transmit time.
  ADR-0142 renders both: MSH-7 is TRANSMIT time (shifted by `offsets`),
  OBR-7 and every OBX-14 are CLINICAL time (`clinical-ts`, this event's
  own `:t`, never shifted) -- the same two-clock split
  `single-subject-message` has always had via EVN-2, now on the result
  wire, so a downstream receiver handed a late result can back-date it.

  ARC 4 SWEEP 3 (ADR-0175 design (b)) adds the optional `status`
  -- `{:stage <keyword> :control-id ... :basis-control-id ...}` -- and
  it renders in TWO fields at once, OBR-25 and every OBX-11, because
  0123 and 0085 are the report-level and analyte-level halves of one
  statement. Nil status is byte-frozen and is what every un-laddered
  result still takes.

  THIS BUILDER IS USED FOR BOTH ENDS OF THE LADDER, which is the whole
  point of restatement: a rung is this same call over this same result
  event with `:t` replaced by the rung's instant and `:stage`
  `:preliminary`, and the terminal message is this same call with the
  event's own `:t` and `:stage` `:final`. The OBX ANALYTE VALUES ARE
  THEREFORE THE SAME on a rung as on the final message, and that is
  disclosed rather than smoothed over: this project's log holds ONE
  result per order, so there is no intermediate value to restate. A
  preliminary that carries the value it will confirm is what HL7's own
  \"P\" means -- a verified early result that may still change -- and
  inventing a different number for a rung would be minting a fact, which
  is exactly what `rulings.md#R-skeleton-or-emission` forbids emission
  from doing."
  ([reference-date utc-offset facility providers demographics site-profile offsets ev]
   (oru-message reference-date utc-offset facility providers demographics site-profile offsets ev nil))
  ([reference-date utc-offset facility providers demographics site-profile offsets
    {:keys [t active-mrn location attending concept results participants] :as ev}
    {:keys [stage] :as status}]
   (let [type+trigger (registry/message-type-registry :result-available)
         control-id (or (:control-id status) (segments/control-id-for ev))
         clinical-ts (hl7-time/hl7-timestamp reference-date t utc-offset)
         transmit-ts (hl7-time/hl7-timestamp reference-date
                                    (hl7-time/transmit-seconds offsets
                                                      (or (:basis-control-id status) control-id)
                                                      t)
                                    utc-offset)
         facility-name (name (:id facility))
         provider (er7/provider-by-id providers attending)
         obx-segments (map-indexed (fn [i r] (segments/obx-segment (inc i) clinical-ts r site-profile stage))
                                   results)]
     (parser/str-message
      (apply parser/create-message
       parser/DEFAULT-DELIMITERS
       (segments/msh-segment site-profile type+trigger control-id transmit-ts)
       (segments/pid-segment active-mrn (timelines/demographics-at demographics (:patient-id (first participants)) t))
       (segments/pv1-segment site-profile :inpatient facility-name location nil provider nil (:encounter-id ev))
       (segments/orc-segment control-id)
       (if stage
         (segments/obr-segment 1 concept clinical-ts site-profile stage)
         (segments/obr-segment 1 concept clinical-ts))
       (concat obx-segments (er7/z-segments-for site-profile demographics ev)))))))

;; --- M5b: :observation -> ORU^R01, OBX only (components/patient-simulator/docs/gmf-interpreter.md
;; section 1's table) -------------------------------------------------------

;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P6): a real
;; DiagnosticReport panel's own OBX shares `observation-obx-segment`'s
;; own field set verbatim ("sharing observation-obx-segment's simpler
;; field set" -- P6's own text) -- reused directly, not a near-duplicate
;; builder.

(defn- observation-message
  "ORU^R01 with a SINGLE OBX and no ORC/OBR -- a legal, real HL7v2 shape
  for an unsolicited observation not tied to any originating order
  (unlike :result-available's own order-linked ORU, docs/operational-
  models.md).

  ADR-0109's split clock, CORRECTED IN PLACE 2026-08-16 (ADR-0142):
  ADR-0109 recorded OBX-14 as not rendered by
  `observation-obx-segment`, making MSH-7 this builder's only timestamp
  field. ADR-0142 renders it: MSH-7 is TRANSMIT time, OBX-14 is
  CLINICAL time (this event's own `:t`, never shifted). There is no
  OBR here at all -- this shape carries no ORC/OBR -- so OBR-7 is not
  owed and not rendered."
  [reference-date utc-offset facility providers demographics site-profile offsets
   {:keys [t active-mrn location attending participants] :as ev}]
  (let [type+trigger (registry/message-type-registry :observation)
        control-id (segments/control-id-for ev)
        clinical-ts (hl7-time/hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-time/hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        provider (er7/provider-by-id providers attending)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (segments/msh-segment site-profile type+trigger control-id transmit-ts)
      (segments/pid-segment active-mrn (timelines/demographics-at demographics (:patient-id (first participants)) t))
      (segments/pv1-segment site-profile :inpatient facility-name location nil provider nil (:encounter-id ev))
      (segments/observation-obx-segment 1 clinical-ts ev)
      (er7/z-segments-for site-profile demographics ev)))))

;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P6): ORC+OBR
;; present (unlike :observation's own order-less shape) -- a real
;; DiagnosticReport panel IS an ORU^R01 with order context, D1a-7's own
;; account. ORC-1/ORC-2/OBR-4 reused unchanged (both already generic on
;; control-id/concept); ONE `observation-obx-segment` per embedded
;; child, `set-id` from vector position, the SAME `map-indexed` shape
;; `oru-message` already uses for :results.

(defn- diagnostic-report-message
  "ADR-0109's split clock, CORRECTED IN PLACE 2026-08-16 (ADR-0142):
  ADR-0109 recorded OBR-7/OBX-14 as not rendered, making MSH-7 this
  builder's only timestamp field, the same as
  `orm-message`/`oru-message`/`observation-message`. ADR-0142 renders
  both on this ORU shape: MSH-7 is TRANSMIT time, OBR-7 and every
  embedded child's own OBX-14 are CLINICAL time (this event's own `:t`,
  never shifted). `orm-message` is the one of that list that does NOT
  change -- author ruling Q3, \"Results only; ORM byte-frozen\"."
  [reference-date utc-offset facility providers demographics site-profile offsets
   {:keys [t active-mrn location attending codes observations participants] :as ev}]
  (let [type+trigger (registry/message-type-registry :diagnostic-report)
        control-id (segments/control-id-for ev)
        clinical-ts (hl7-time/hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-time/hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        provider (er7/provider-by-id providers attending)
        obx-segments (map-indexed (fn [i o] (segments/observation-obx-segment (inc i) clinical-ts o)) observations)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (segments/msh-segment site-profile type+trigger control-id transmit-ts)
      (segments/pid-segment active-mrn (timelines/demographics-at demographics (:patient-id (first participants)) t))
      (segments/pv1-segment site-profile :inpatient facility-name location nil provider nil (:encounter-id ev))
      (segments/orc-segment control-id)
      (segments/obr-segment 1 (first codes) clinical-ts)
      (concat obx-segments (er7/z-segments-for site-profile demographics ev))))))


;; --- ARC 4 SWEEP 2 (ADR-0175 design (c), ruling B1): DFT^P03 charges ------
;; One DFT per encounter CLOSE, carrying [MSH EVN PID PV1] then one FT1
;; per chargeable fact of that encounter. A charge line restates a fact
;; the log already holds -- a procedure, an order, an occupied bed-day.
;; The AMOUNT is not in the log, and an amount derived from a code via a
;; config table is a pure function of (log, config), which is exactly
;; what `:site-profile` and `:latency` already are: EMISSION, on the
;; explicit condition that the price table is emission config and never
;; ground truth. THE ENGINE NEVER READS IT -- `:charges` reaches no
;; member of `ehrt.sim-engine.config/config-keys`, and a missing price
;; is a COUNTED SKIP here, never a read-back into the log for something
;; to bill instead.

(defn- dft-message
  "DFT^P03 for one encounter close. `DFT_P03`'s own segment order is
  [MSH EVN PID PD1 ROL PV1 PV2 ROL2 DB1 COMMON_ORDER FINANCIAL DG1]
  with FINANCIAL leading on FT1, so MSH EVN PID PV1 FT1+ is that order
  with the optional groups omitted.

  MSH-10 is `mrn-P03-t`: the trigger is part of every control id this
  emitter mints, so a DFT can never collide with the ADT^A03 rendered
  from the SAME event at the same instant.

  THE OFFSET IS LOOKED UP UNDER THE BASIS EVENT'S OWN CONTROL ID, not
  under the DFT's, and the two are deliberately different keys. A DFT is
  a SECOND message for one ground-truth event, so ADR-0109's split clock
  says it lags by that event's own lag; keying the lookup on
  `mrn-P03-t` would find no entry, silently give the DFT a zero offset,
  and make the financial message overtake the ADT^A03 it accompanies.
  Measured before this was fixed, on the ed-tuesday latency demo:
  MRN000002's DFT transmitted at 01:24:00 and its A03 at 02:10:37, 46
  minutes later, with no configuration anywhere asking for that. It was
  an accident of keying, not a decision -- there is no `:latency` entry
  a config author could write for a message family that has no event
  kind of its own, so `never late` was an undeclared special case."
  [reference-date utc-offset facility providers demographics site-profile offsets lines
   {:keys [event t active-mrn location attending participants] :as ev}]
  (let [control-id (str active-mrn "-P03-" t)
        clinical-ts (hl7-time/hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-time/hl7-timestamp reference-date
                                   (hl7-time/transmit-seconds offsets (segments/control-id-for ev) t)
                                   utc-offset)
        facility-name (name (:id facility))
        provider (er7/provider-by-id providers attending)
        persona (timelines/demographics-at demographics (:patient-id (first participants)) t)
        patient-class (if (= :outpatient-visit-end event) :outpatient :inpatient)]
    (parser/str-message
     (apply parser/create-message
            parser/DEFAULT-DELIMITERS
            (segments/msh-segment site-profile {:type "DFT" :trigger "P03"} control-id transmit-ts)
            (segments/evn-segment "P03" clinical-ts)
            (segments/pid-segment active-mrn persona)
            (segments/pv1-segment site-profile patient-class facility-name location nil provider
                         nil (:encounter-id ev))
            (map-indexed (fn [i line] (segments/ft1-segment reference-date utc-offset (inc i) line))
                         lines)))))

(defn event->messages
  "Renders one ground-truth event to a vector of 0+ ER7 message strings
  -- most types render exactly one message; M2b's genuinely two-
  participant types (:bed-swap A17, :merge A40) still render exactly
  ONE message (both patients' data in one message, the real HL7 shape),
  so this is 0-or-1 for every type today, but returns a vector (not a
  single nilable message) since a future many-messages-per-event type
  is now a shape this stage already accommodates. Events outside
  `message-type-registry` render an empty vector, not an error.

  ADR-0109: `offsets` ({control-id -> offset-seconds}, `plan-latency`'s
  own output) threads to every builder for the split-clock rendering
  (each builder's own docstring has the per-type field-audit detail);
  the lower arities pass {} -- unconditionally the identity input,
  since `hl7-time/transmit-seconds` (a sibling namespace) falls back to a 0
  offset for any control-id absent from the map."
  ([reference-date utc-offset facility providers ev]
   (event->messages reference-date utc-offset facility providers {} nil {} {} ev))
  ([reference-date utc-offset facility providers demographics ev]
   (event->messages reference-date utc-offset facility providers demographics nil {} {} ev))
  ([reference-date utc-offset facility providers demographics site-profile offsets ev]
   (event->messages reference-date utc-offset facility providers demographics site-profile offsets {} ev))
  ([reference-date utc-offset facility providers demographics site-profile offsets charges ev]
   (event->messages reference-date utc-offset facility providers demographics site-profile offsets
                    charges nil ev))
  ([reference-date utc-offset facility providers demographics site-profile offsets charges
    ladder-status ev]
   (event->messages reference-date utc-offset facility providers demographics site-profile offsets
                    charges ladder-status nil ev))
  ;; ARC 4 SWEEP 3 (ADR-0175 design (b)): `ladder-status` is THIS
  ;; event's own terminal status -- `{:stage :final}` for a
  ;; `:result-available` whose order actually grew a rung, nil for every
  ;; other event and every un-laddered order. It is passed per event
  ;; rather than looked up from a set of control ids on purpose:
  ;; `control-id-for` is not injective over `:result-available` (two
  ;; results for one patient at one second mint the same MSH-10, a
  ;; PRE-EXISTING collision this sweep neither introduces nor fixes),
  ;; and a ladder keyed on a non-injective id would put final codes on
  ;; the wrong twin. `emit-wire` has the log index in hand and passes
  ;; the decision, not the key.
  ;; ARC 4 SWEEP 4 (ADR-0175 ruling B1): `siu` is the `:siu` emission
  ;; profile (nil = off), and it is the ONE argument here that can turn
  ;; a REGISTERED kind back into no message at all. Every other entry in
  ;; `message-type-registry` renders unconditionally; scheduling's four
  ;; render only when asked, which is what keeps `:siu` absent
  ;; byte-identical to every corpus this project has ever shipped. The
  ;; gate lives here rather than in the registry because three other
  ;; readers of that map -- `control-id-for`, `skeleton-message-types`
  ;; and the conformance vocabulary check -- all want the four present
  ;; unconditionally.
  ([reference-date utc-offset facility providers demographics site-profile offsets charges
    ladder-status siu {:keys [event] :as ev}]
   (let [registered (cond
                      (not (registry/message-type-registry event)) []
                      (contains? registry/siu-event-kinds event)
                      (if (registry/siu-renders? siu event)
                        [(siu-message reference-date utc-offset facility providers demographics
                                      site-profile offsets ev)]
                        [])
                      (= :bed-status-change event) [(bed-status-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
                      (= :bed-swap event) [(bed-swap-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
                      (= :merge event) [(merge-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
                      (= :order-placed event) [(orm-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
                      (= :result-available event)
                      [(oru-message reference-date utc-offset facility providers demographics
                                    site-profile offsets ev ladder-status)]
                      (= :observation event) [(observation-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
                      (= :diagnostic-report event) [(diagnostic-report-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
                      :else [(single-subject-message reference-date utc-offset facility providers demographics site-profile offsets ev)])
         ;; ARC 4 SWEEP 2 (ADR-0175 design (c)): THE FIRST REAL USE of
         ;; the many-messages-per-event shape this function's own
         ;; docstring above has always accommodated. A `:discharge`
         ;; renders its ADT^A03 and then the DFT^P03 that closes the
         ;; encounter's account; an `:outpatient-visit-end` renders NO
         ;; ADT at all -- its registry silence is deliberate and stands
         ;; -- and the DFT is the only message it ever produces, which
         ;; is why the charge branch sits OUTSIDE the registry guard
         ;; above rather than inside the `cond`.
         lines (when (registry/charge-closing-kinds event)
                 (get charges [(:encounter-id ev) (:t ev)]))]
     (cond-> registered
       (seq lines) (conj (dft-message reference-date utc-offset facility providers demographics
                                      site-profile offsets lines ev))))))

(defn chatter-message
  "Renders one `plan-chatter` instruction to an ER7 string. Every field
  of it is `demographics-at` of a patient at an instant -- the
  definition of derivable restatement -- plus, for an A08, the PV1 of
  the encounter the instant falls inside, read off that encounter's own
  opener.

  ONE CLOCK, not two: chatter carries no latency offset (an offset is
  keyed on a ground-truth event's control-id, and a restatement has no
  event), so MSH-7 and EVN-2 are the same instant here. That is the
  identity `emit-wire`'s own interleave test asserts -- turning chatter
  on moves no non-chatter message's bytes at all."
  [reference-date utc-offset facility providers demographics site-profile spans
   {:keys [at trigger control-id active-mrn patient-id encounter-id in1?]}]
  (let [ts (hl7-time/hl7-timestamp reference-date at utc-offset)
        persona (timelines/demographics-at demographics patient-id at)
        opener (:opener (get spans encounter-id))
        facility-name (name (:id facility))]
    (parser/str-message
     (apply parser/create-message
            parser/DEFAULT-DELIMITERS
            (segments/msh-segment site-profile {:type "ADT" :trigger trigger} control-id ts)
            (segments/evn-segment trigger ts)
            (segments/pid-segment active-mrn persona)
            (concat
             (when (= "A08" trigger)
               [(segments/pv1-segment site-profile
                             (if (= :outpatient-visit (:event opener)) :outpatient :inpatient)
                             facility-name (:location opener) nil
                             (er7/provider-by-id providers (:attending opener))
                             nil encounter-id)])
             (when (and in1? (:payer persona))
               [(segments/in1-segment (:payer persona))]))))))

(defn ladder-message
  "Renders one `plan-ladders` instruction to an ER7 string -- the SAME
  builder the message it restates uses, over the SAME event, with `:t`
  replaced by the rung's own instant and a `status` carrying the rung's
  stage and control id.

  THAT IS THE WHOLE MECHANISM, and it is why a rung cannot say anything
  the final message does not: it is not a second rendering path, it is
  the first one called again at an earlier instant. The ORC and OBR a
  rung carries are the ORC and OBR of the message it restates, field
  for field, because they come out of the same builder over the same
  event."
  [reference-date utc-offset facility providers demographics site-profile offsets ground-truth
   {:keys [at family stage control-id basis-control-id basis]}]
  (let [ev (assoc (nth ground-truth basis) :t at)
        status {:stage stage :control-id control-id :basis-control-id basis-control-id}]
    (if (= :orm family)
      (orm-message reference-date utc-offset facility providers demographics site-profile
                   offsets ev status)
      (oru-message reference-date utc-offset facility providers demographics site-profile
                   offsets ev status))))
