(ns ehrt.sim-emit-hl7.emit-hl7
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
  the event's log-relative SECOND offset (`sim/ADR-0011`; was minutes before
  M2a), suffixed with the pinned :utc-offset (`sim/ADR-0011`: a fixed offset,
  never a timezone-database lookup, never per-event) -- never from
  System/currentTimeMillis or similar. PID-3 renders the event's own
  :active-mrn (`sim/ADR-0010`: MRN moved into state; the emitter renders
  whichever MRN was active when the event happened, which until M2b's
  merge exists is always the patient's one and only MRN)."
  (:require [com.nervestaple.hl7-parser.parser :as parser]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-emit-hl7.hl7-time :as hl7-time]
            [ehrt.sim-emit-hl7.registry :as registry]
            [ehrt.sim-emit-hl7.timelines :as timelines]
            [ehrt.sim-emit-hl7.er7 :as er7]
            [ehrt.sim-emit-hl7.segments :as segments]))

;; --- moved to ehrt.sim-emit-hl7.hl7-time -----------------------------
;;
;; SEVEN forms left this file, from three regions: the two defaults
;; here; `hl7-timestamp-formatter`, `reference-instant`,
;; `hl7-offset-suffix` and `hl7-timestamp` from just above
;; `control-id-for`; and `transmit-seconds` from just above
;; `single-subject-message`. This is the first cluster of `emit_hl7.
;; clj`'s own namespace extraction, and a leaf: it called nothing else
;; in this file.
;;
;; The THREE public movers keep a delegating def below, so
;; `interface.clj` (`default-reference-date`, `default-utc-offset`) and
;; the test tree resolve exactly as before. `hl7-timestamp`'s def is
;; owed to the tree rather than to `interface.clj`, which never
;; re-exported it: thirteen `emit-hl7/hl7-timestamp` call sites across
;; `emit_hl7_test.clj`, `result_clock_test.clj` and `latency_test.clj`,
;; plus twenty bare-name sites in this file that keep resolving through
;; the def below -- a twenty-first left again with `segments`.
;;
;; The FOUR private movers get no def -- that would widen this file's
;; public surface, which C1(a) does not ask for. `transmit-seconds` is
;; public in `hl7-time` instead, because eleven forms here still call
;; it; those twelve call sites name it `hl7-time/transmit-seconds`.

(def default-reference-date hl7-time/default-reference-date)
(def default-utc-offset hl7-time/default-utc-offset)
(def hl7-timestamp hl7-time/hl7-timestamp)

;; --- moved to `ehrt.sim-emit-hl7.registry` (extraction cluster 2 of 8) ---
;;
;; Thirteen forms -- the message-type catalog, the three MSH-9
;; vocabularies derived from it, scheduling's kinds and SCH-25 states,
;; the charge tables, chatter's kind map and the two status ladders --
;; left this file for `registry.clj`. It is a LEAF: nothing in it calls
;; anything outside itself, so it takes no `:require` with it.
;;
;; The TEN public movers keep a delegating def below, so `interface.clj`
;; (which re-exports seven of them -- `skeleton-message-types`,
;; `add-on-message-types`, `emittable-message-types`, `siu-event-kinds`,
;; `siu-renders?`, `room-and-board-code`, `chatter-event-kinds`) and the
;; test tree resolve exactly as before. `message-type-registry`,
;; `order-status-ladder` and `result-status-ladder` are owed a def by
;; THIS FILE rather than by `interface.clj`, which never re-exported
;; them: thirty-four `emit-hl7/message-type-registry` call sites across
;; six test files, plus the bare-name sites below that keep resolving
;; through these defs.
;;
;; The THREE private movers get no def -- that would widen this file's
;; public surface, which C1(a) does not ask for. `siu-filler-status` and
;; `charge-closing-kinds` are public in `registry` instead, because
;; `sch-segment`, `event->messages` and `plan-charges` still call them;
;; those three call sites name them `registry/...`. `final-result-stage`
;; stays private there, having no caller anywhere in the tree.

(def message-type-registry registry/message-type-registry)
(def skeleton-message-types registry/skeleton-message-types)
(def add-on-message-types registry/add-on-message-types)
(def emittable-message-types registry/emittable-message-types)
(def siu-event-kinds registry/siu-event-kinds)
(def siu-renders? registry/siu-renders?)
(def room-and-board-code registry/room-and-board-code)
(def chatter-event-kinds registry/chatter-event-kinds)
(def order-status-ladder registry/order-status-ladder)
(def result-status-ladder registry/result-status-ladder)

;; --- moved to `ehrt.sim-emit-hl7.segments` (extraction cluster 5 of 8) --
;;
;; FIFTEEN forms -- `control-id-for`, the thirteen HL7v2 segment builders
;; (MSH, EVN, PID, IN1, MRG, PV1, NPU, SCH, ORC, OBR, OBX, the observation
;; OBX and FT1) and `charge-concept` -- left this file for `segments.clj`,
;; from EIGHT regions, the most of any cluster: the three forms here;
;; `pid-segment`/`in1-segment` and `mrg-segment`/`pv1-segment` from just
;; below the ER7 defs; `npu-segment`; `sch-segment` from under the SIU^S12
;; header; the three order/result builders from under the M3 header;
;; `observation-obx-segment` from under the M5b header; and
;; `charge-concept`/`ft1-segment` from under the DFT^P03 header.
;;
;; NO BANNER TRAVELS, a first for this file. All four comment blocks that
;; head a moved region head a section this cluster SPLITS, and each of the
;; four names a MESSAGE type -- SIU^S12, ORM^O01+ORU^R01, :observation,
;; DFT^P03 -- whose builder stays. The M3 header set that precedent for
;; cluster 4; the other three follow it here.
;;
;; This is the first cluster to depend on THREE landed siblings at once --
;; `er7` (eighteen edges), `registry` (two) and `hl7-time` (one) -- and,
;; because of that, the first whose own moved text had to be REQUALIFIED:
;; five bare names that resolved here through the delegating defs above
;; now name their real homes over there. It has NO internal edge at all:
;; not one of the fifteen forms calls another, which is why every private
;; mover widens and none stays private.
;;
;; THREE delegating defs below. `control-id-for` is the cluster's only
;; PUBLIC mover and the one form `interface.clj` re-exports, so its def is
;; owed twice over. `msh-segment` and `pid-segment` are PRIVATE movers and
;; both widen -- eleven callers each stayed behind -- but
;; `emit_hl7_test.clj` reaches them as `(#'emit-hl7/msh-segment ...)` and
;; `(#'emit-hl7/pid-segment ...)`, var accesses on private vars that no
;; move can carry and that C1(a) forbids editing. A `^:private` delegating
;; def keeps each var here without widening this file's public surface by
;; a name, and their twenty-two call sites below keep resolving through
;; them unqualified. That is the C7 extension `tn-field` established one
;; cluster ago, applied twice, exactly where cluster 4 predicted it.
;;
;; The other TWELVE private movers are widenings too -- every one has a
;; caller that stayed behind -- so they are public in `segments` instead
;; and gain NO delegating def, because widening this file's own public
;; surface is not what C1(a) asks for. Their thirty-five call sites below
;; name them `segments/...`.
;;
;; This move is also the first in the emit phase to leave a DEAD REQUIRE
;; behind, and it leaves two: `clojure.string` and
;; `ehrt.sim-emit-hl7.site-profile` had no code use left here once the
;; fifteen went, and both are dropped from the `ns` above.

(def control-id-for segments/control-id-for)
(def ^:private msh-segment segments/msh-segment)
(def ^:private pid-segment segments/pid-segment)

;; --- moved to `ehrt.sim-emit-hl7.er7` (extraction cluster 4 of 8) --------
;;
;; NINETEEN forms -- the ER7 escape table and its encoder, the decode map
;; and its single-pass decoder, the XPN/XAD/TN/CWE/coded/location/
;; provider/blank primitive field composers, and the four Z-segment
;; template renderers -- left this file for `er7.clj`, from six regions:
;; the M4 Task 4 escaping section here; three field helpers from just
;; above `mrg-segment`; `blank-fields`; the site-profiles Task 3 section
;; from just above `single-subject-message`; the three coded-field forms
;; from the head of the M3 section; and `money` from just above
;; `ft1-segment`.
;;
;; It is the FIRST cluster of this file that is not a leaf, and the first
;; anywhere in the emitter to require a SIBLING extraction rather than
;; this file: `context-for-event` calls `timelines/demographics-at`, so
;; `er7.clj` takes `ehrt.sim-emit-hl7.timelines` with it. That is its one
;; cross-cluster edge.
;;
;; The TWO public movers keep a delegating def below, so `v2_replay.clj`'s
;; two reader call sites and the four `emit-hl7/escape-er7`/`unescape-er7`
;; sites in `emit_hl7_test.clj` resolve exactly as before. `interface.clj`
;; re-exports neither -- the first cluster here whose defs are owed to the
;; TREE alone rather than to the interface.
;;
;; ELEVEN private movers are widenings forced by callers that stayed
;; behind -- forty-one call sites across eighteen forms, of which
;; SEVENTEEN across ten forms are still below and twenty-six left again
;; with `segments` in cluster 5. They are public in `er7` instead, and
;; every one of the forty-one now names them `er7/...`. They gain NO
;; delegating def, because widening this file's own public surface is not
;; what C1(a) asks for.
;; SIX more -- `er7-escape-table`, `er7-decode-map`, `context-for-event`,
;; `render-z-field`, `z-segment-for` and `code-system->hl7-table-0396` --
;; have no caller outside the cluster at all, every one of their callers
;; having travelled, so they stay private there: census constraint 5 read
;; the way `engine.clj`'s `weighted-pick` read it.
;;
;; `tn-field` is the exception, and the reason for the third def below.
;; It is a widening like the other ten, but `v2_replay_test.clj` reaches
;; it as `(#'emit-hl7/tn-field phone)` -- a var access on a PRIVATE var,
;; which no move can carry and which C1(a) forbids editing. A `^:private`
;; delegating def keeps that var here without widening this file's public
;; surface by a name. `pid-segment`'s own call site resolved through it
;; unqualified until cluster 5 took `pid-segment` as well; what the def
;; stands for now is that var access and `v2_replay.clj:166`'s namespace
;; claim, which are the two things a move cannot carry.

(def escape-er7 er7/escape-er7)
(def unescape-er7 er7/unescape-er7)
(def ^:private tn-field er7/tn-field)

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
  (when-let [type+trigger (message-type-registry event)]
    (let [control-id (control-id-for ev)
          clinical-ts (hl7-timestamp reference-date t utc-offset)
          transmit-ts (hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
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
        (msh-segment site-profile type+trigger control-id transmit-ts)
        (segments/evn-segment (:trigger type+trigger) clinical-ts)
        (pid-segment active-mrn persona)
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
  (let [type+trigger (message-type-registry :bed-swap)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
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
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (segments/evn-segment (:trigger type+trigger) clinical-ts)
      (pid-segment mrn1 (timelines/demographics-at demographics p1 t))
      (segments/pv1-segment site-profile :inpatient facility-name to1 from1 (er7/provider-by-id providers att1) nil enc1)
      (pid-segment mrn2 (timelines/demographics-at demographics p2 t))
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
  (let [type+trigger (message-type-registry :bed-status-change)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))]
    (parser/str-message
     (parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
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
  (let [type+trigger (message-type-registry event)
        control-id (control-id-for ev)
        transmit-ts (hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        persona (timelines/demographics-at demographics (:patient-id (first participants)) t)
        scheduled-ts (when scheduled-t (hl7-timestamp reference-date scheduled-t utc-offset))]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (segments/sch-segment site-profile ev scheduled-ts)
      (pid-segment active-mrn persona)
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
  (let [type+trigger (message-type-registry :merge)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        survivor-id (:patient-id (first (filter #(= :survivor (:role %)) participants)))]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (segments/evn-segment (:trigger type+trigger) clinical-ts)
      (pid-segment surviving-mrn (timelines/demographics-at demographics survivor-id t))
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
   (let [type+trigger (message-type-registry :order-placed)
         control-id (or (:control-id status) (control-id-for ev))
         transmit-ts (hl7-timestamp reference-date
                                    (hl7-time/transmit-seconds offsets
                                                      (or (:basis-control-id status) control-id)
                                                      t)
                                    utc-offset)
         facility-name (name (:id facility))
         provider (er7/provider-by-id providers attending)]
     (parser/str-message
      (apply parser/create-message
       parser/DEFAULT-DELIMITERS
       (msh-segment site-profile type+trigger control-id transmit-ts)
       (pid-segment active-mrn (timelines/demographics-at demographics (:patient-id (first participants)) t))
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
   (let [type+trigger (message-type-registry :result-available)
         control-id (or (:control-id status) (control-id-for ev))
         clinical-ts (hl7-timestamp reference-date t utc-offset)
         transmit-ts (hl7-timestamp reference-date
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
       (msh-segment site-profile type+trigger control-id transmit-ts)
       (pid-segment active-mrn (timelines/demographics-at demographics (:patient-id (first participants)) t))
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
  (let [type+trigger (message-type-registry :observation)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        provider (er7/provider-by-id providers attending)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (pid-segment active-mrn (timelines/demographics-at demographics (:patient-id (first participants)) t))
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
  (let [type+trigger (message-type-registry :diagnostic-report)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (hl7-time/transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        provider (er7/provider-by-id providers attending)
        obx-segments (map-indexed (fn [i o] (segments/observation-obx-segment (inc i) clinical-ts o)) observations)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (pid-segment active-mrn (timelines/demographics-at demographics (:patient-id (first participants)) t))
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
;; member of `ehrt.sim-engine.engine/config-keys`, and a missing price
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
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date
                                   (hl7-time/transmit-seconds offsets (control-id-for ev) t)
                                   utc-offset)
        facility-name (name (:id facility))
        provider (er7/provider-by-id providers attending)
        persona (timelines/demographics-at demographics (:patient-id (first participants)) t)
        patient-class (if (= :outpatient-visit-end event) :outpatient :inpatient)]
    (parser/str-message
     (apply parser/create-message
            parser/DEFAULT-DELIMITERS
            (msh-segment site-profile {:type "DFT" :trigger "P03"} control-id transmit-ts)
            (segments/evn-segment "P03" clinical-ts)
            (pid-segment active-mrn persona)
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
                      (not (message-type-registry event)) []
                      (contains? siu-event-kinds event)
                      (if (siu-renders? siu event)
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

(def ^:private default-providers
  "A fixed, arbitrary reference-seed provider pool -- purely a fallback
  default for callers that don't care about exact NPI values (`emit`'s
  lower arities). A real run threads back its OWN materialized
  providers (ehrt.sim-engine.engine/run's :providers) instead, so its
  messages' PV1-7 matches its own ground-truth log's :attending ids."
  (sim-model/materialize-providers (java.util.Random. 0) sim-model/default-provider-templates))

(defn emit
  "The stage function: ground-truth log -> vector of ER7 message
  strings, in log order. Pure function of its arguments alone
  (determinism law); events outside `message-type-registry` are
  skipped, not errored -- the theory's laws bind the events this stage
  claims to handle, not every event type that may ever appear in a
  log. `utc-offset`/`facility`/`providers` default for standalone
  convenience; callers rendering a specific run's log should pass back
  that SAME run's :utc-offset/:facility/:providers (ehrt.sim.run
  does). `site-profile` (Milestone site-profiles) is the LAST, optional
  argument: absent (the 5-arg arity), nil, or {} all render identically
  -- the default-profile identity property (docs/site-profiles.md, this
  milestone's own determinism anchor) -- since :site-profile reaches no
  stage but this one's own render call sites, never ground-truth-log or
  check.clj (ehrt.sim-engine.engine/config-keys has no such key).

  ADR-0109: this function's own output is BYTE-FROZEN -- always calls
  `event->messages` with offsets {}, so every transmit instant equals
  its own clinical instant and this function's bytes/order never move,
  regardless of anything ADR-0109 added elsewhere in this namespace.
  `emit-wire`, below, is the split-clock sibling that actually shifts
  MSH-7; this function is the oracle `emit-wire`'s own identity
  property is checked against."
  ([ground-truth reference-date]
   (emit ground-truth reference-date default-utc-offset sim-model/default-facility default-providers))
  ([ground-truth reference-date utc-offset]
   (emit ground-truth reference-date utc-offset sim-model/default-facility default-providers))
  ([ground-truth reference-date utc-offset facility providers]
   (emit ground-truth reference-date utc-offset facility providers nil))
  ([ground-truth reference-date utc-offset facility providers site-profile]
   (let [demographics (timelines/demographics-timeline ground-truth)]
     (into [] (mapcat (partial event->messages reference-date utc-offset facility providers demographics site-profile {}))
           ground-truth))))

;; --- ADR-0109: the second clock -- GT x LatencyParams -> TimedWire -------
;; The extension point docs/dev/simulator-architecture.md section 5 named
;; and built nothing of: an arrow between `engine`'s own GT output and
;; this namespace's own `emitH` consumption of it, so a message's own
;; wire-emission instant can lag its clinical-event instant by a
;; realistic, sampled delay. Sampling itself stays OUT of this namespace
;; (this file's own renders-only doctrine, restated in that same
;; section) -- `plan-latency` is a pure function of an explicitly-passed
;; RNG, never an atom or a wall clock, and its OWN output (`offsets`) is
;; the only thing `emit-wire` ever consumes; `emit-wire` itself takes no
;; RNG at all.

(defn plan-latency
  "RNG x GT x LatencyProfile (ehrt.sim-model.config/LatencyProfile) ->
  offsets ({control-id -> offset-seconds}). Fixed RNG consumption (the
  RNG-path law, `rulings.md#R-measure-claimed-population`'s own
  underlying discipline; `ehrt.sim-engine.engine/assign-pathway`'s own
  worked example -- cited BY NAME, never by line: that line moved twice
  already, once under arc 0's refactor and once under ADR-0171's, which
  is exactly the species ADR-0170 named -- is the precedent this
  function follows): ALWAYS consumes exactly one `.nextDouble` per ground-truth
  event, in log order, regardless of whether that event's own :event
  type is covered by `latency-profile` -- draw-and-discard for an
  uncovered type, so adding one profile entry for event type X can
  never shift any OTHER event's own draw, covered or not.

  A covered event (`latency-profile` has an entry for its :event type)
  samples its own offset uniformly from that entry's
  {:from-minutes :to-minutes} range via the ALREADY-consumed draw,
  converted to whole seconds (`sim/ADR-0011`'s own minutes-authored/
  seconds-engine convention, mirrored here). An uncovered event, or one
  with no `control-id-for` at all (outside `message-type-registry` --
  :bed-swap/:merge's own two-participant control-ids are covered the
  same as every single-subject one, since `control-id-for` already
  handles both), contributes no entry to the returned map.

  Absent/nil/{} `latency-profile` still draws (and discards) once per
  event and returns {} -- `emit-wire` called with THIS function's own
  {} output renders byte-identical to `emit` (the identity property,
  emit-hl7-test), the same three-way absent/nil/{} agreement
  `ehrt.sim-emit-hl7.site-profile`'s own default-profile identity
  already established for site profiles."
  [^java.util.Random rng ground-truth latency-profile]
  (into {}
        (keep (fn [ev]
                (let [draw (.nextDouble rng)
                      {:keys [from-minutes to-minutes]} (get latency-profile (:event ev))]
                  (when (and from-minutes to-minutes)
                    (when-let [control-id (control-id-for ev)]
                      [control-id (long (Math/round (* 60.0 (+ from-minutes (* draw (- to-minutes from-minutes))))))])))))
        ground-truth))

;; --- ARC 4 SWEEP 2 (ADR-0175 design (a), ruling B1): re-statement chatter --
;; A08 / A31 / A28 / IN1-only. `plan-chatter` is `plan-latency`'s
;; sibling and keeps this namespace's renders-only doctrine intact: it
;; takes an explicitly-passed `java.util.Random`, never an atom and
;; never a wall clock, and its OWN output -- a vector of render
;; instructions -- is the only thing `emit-wire` ever consumes.
;;
;; CHATTER ADDS NO `message-type-registry` ENTRY, deliberately. A
;; registry entry is a claim that one ground-truth event renders one
;; message; a periodic re-statement has no event of its own, and the
;; fourteen kinds that registry deliberately leaves silent stay silent
;; there (`ehrt.sim-emit-hl7.event-conformance-test`'s pinned set does
;; not move). What reaches the wire here is derivable RESTATEMENT of
;; demographic state the log already carries -- `rulings.md#R-skeleton-
;; or-emission` classifies that emission, which is why it may ride
;; `:config` and may never reach `ehrt.sim-engine.engine/config-keys`.

(def restatement-day-seconds
  "One patient-day, in seconds -- the periodic re-statement census's own
  grid. The period is deliberately NOT a second knob:
  `:rate-per-patient-day` is the whole configuration surface, and a
  tunable period would let two configs express one volume two ways."
  86400)

(defn- chatter-trigger
  [kind ev]
  (or (:trigger (chatter-event-kinds kind))
      (if (:encounter-id ev) "A08" "A31")))

(defn- event-driven-chatter
  "The first of `plan-chatter`'s two passes, and the one that obeys the
  fixed-consumption law literally: ALWAYS exactly one `.nextDouble` per
  ground-truth event, in log order, drawn and discarded for an event no
  chatter rule covers -- so adding a rule for kind X can never shift
  kind Y's draws. `plan-latency`'s own law, same words, same reason."
  [^java.util.Random rng ground-truth chatter]
  (loop [evs ground-truth i 0 acc []]
    (if-let [ev (first evs)]
      (let [draw (.nextDouble rng)
            kind (:event ev)
            rate (get chatter kind)
            patient-id (:patient-id (first (:participants ev)))]
        (recur (rest evs) (inc i)
               (if (and (number? rate) (< draw (double rate)) patient-id (:active-mrn ev))
                 (conj acc {:at (:t ev)
                            :basis i
                            :kind kind
                            :periodic? false
                            :trigger (chatter-trigger kind ev)
                            :encounter-id (:encounter-id ev)
                            :patient-id patient-id
                            :active-mrn (:active-mrn ev)
                            :in1? (boolean (:in1? (chatter-event-kinds kind)))})
                 acc)))
      acc)))

(defn- periodic-chatter
  "The second pass -- the PERIODIC half, and the one ADR-0175 section
  2(a) says the program's A08 volume actually comes from. The
  event-driven half above is ~99.5% A31 in every corpus measured, for a
  modelled-world reason and not a defect: the person process walks
  twenty years while the clinical content is one shift, so demographic
  churn happens almost entirely BETWEEN encounters. A periodic
  re-statement fires while an encounter is OPEN, which is exactly where
  a real interface's A08 traffic comes from.

  THE CENSUS IS PATIENT-DAYS OF CARE, which is this project's own
  reading of the term elsewhere (ADR-0175 section 2(c) prices a DFT's
  room-and-board lines per inpatient DAY): one draw per (encounter,
  started day), over the encounter intervals `encounter-spans` derives.
  Every instant it produces is inside an open encounter by
  construction, so `chatter-trigger`'s rule answers A08 for all of
  them -- the rule is applied, not bypassed.

  FIXED CONSUMPTION HOLDS ACROSS BOTH PASSES: the number of draws taken
  here is a pure function of the LOG (the patient-day census), never of
  the config, so a run with `:restatement` absent draws and discards
  exactly as many times as one with it present.

  `:rate-per-patient-day` may exceed 1: the whole part is a guaranteed
  count and the fraction is the one Bernoulli draw, so r = 2.5 means
  two restatements every patient-day and a third on half of them. The
  n messages of one slot are spaced evenly across it."
  [^java.util.Random rng spans mrns chatter]
  (let [r (double (or (get-in chatter [:restatement :rate-per-patient-day]) 0.0))
        whole (long (Math/floor r))
        frac (- r whole)]
    (loop [ss (sort-by :opener-index (vals spans)) acc []]
      (if-let [{:keys [t0 t1 opener opener-index]} (first ss)]
        (let [patient-id (:patient-id (first (:participants opener)))
              slots (max 1 (inc (quot (- (long t1) (long t0)) restatement-day-seconds)))
              encounter-id (:encounter-id opener)]
          (recur (rest ss)
                 (loop [k 0 acc acc]
                   (if (>= k slots)
                     acc
                     (let [draw (.nextDouble rng)
                           n (+ whole (if (< draw frac) 1 0))
                           slot-start (+ (long t0) (* k restatement-day-seconds))
                           slot-len (max 0 (- (min (long t1) (+ slot-start restatement-day-seconds))
                                              slot-start))]
                       (recur (inc k)
                              (if (or (zero? n) (nil? patient-id))
                                acc
                                (into acc
                                      (for [j (range n)
                                            :let [at (+ slot-start (quot (* j slot-len) n))]]
                                        {:at at
                                         :basis opener-index
                                         :kind :restatement
                                         :periodic? true
                                         :trigger "A08"
                                         :encounter-id encounter-id
                                         :patient-id patient-id
                                         :active-mrn (timelines/mrn-at mrns patient-id at)
                                         :in1? false})))))))))
        acc))))

(defn- assign-restatement-ordinals
  "Stamps `:ordinal` and `:control-id` onto a vector of restatement
  instructions, the ordinal counting within `(active-mrn, trigger, at)`
  -- EXTRACTED from `assign-chatter-ordinals` (arc 4 sweep 2) verbatim
  so the ladder and chatter mint control ids by one construction rather
  than two.

  MSH-10 is `mrn-trigger-t-<ordinal>` for every restatement this
  emitter makes. A ground-truth event's own id has NO ordinal suffix,
  so a restatement id can never collide with one; the trigger keeps
  chatter's A08/A31/A28 apart from the ladder's O01/R01; and the
  ordinal is what keeps two restatements of one patient at one instant
  apart. THE FOUR-PART KEY IS THE IDENTITY TUPLE, not ADR-0175 section
  4's three-part `(basis-event-index, trigger, ordinal)` -- sweep 2
  measured that triple non-injective (two periodic restatements inside
  one patient-day share a basis, a trigger and an ordinal and differ
  only in the instant) and the ladder must not worsen it. It does not:
  two rungs of one order differ in `at` by construction, and two rungs
  of two orders for one patient at one instant differ in the ordinal."
  [instructions]
  (first
   (reduce (fn [[acc seen] ins]
             (let [k [(:active-mrn ins) (:trigger ins) (:at ins)]
                   n (get seen k 0)]
               [(conj acc (assoc ins
                                 :ordinal n
                                 :control-id (str (:active-mrn ins) "-" (:trigger ins)
                                                  "-" (:at ins) "-" n)))
                (assoc seen k (inc n))]))
           [[] {}]
           instructions)))

(defn plan-chatter
  "RNG x GT x ChatterProfile (ehrt.sim-model.config/ChatterProfile) ->
  a vector of render instructions, each
  `{:at :basis :kind :trigger :encounter-id :patient-id :active-mrn
    :in1? :ordinal :control-id :periodic?}`.

  Two passes, both with LOG-DETERMINED draw counts (`event-driven-
  chatter` and `periodic-chatter` each carry the argument): one draw
  per ground-truth event, then one draw per patient-day of care. A
  config that turns one rule off still draws for it, so two configs
  differing in one rule produce identical draws for everything else --
  the property arc 4 owes and `emit_hl7_test` asserts.

  Absent/nil/{} `chatter` still draws (and discards) the full census
  and returns [] -- `emit-wire` called with THIS function's own []
  output renders byte-identical to one called with no chatter at all,
  the same three-way absent/nil/{} agreement `plan-latency` and
  `ehrt.sim-emit-hl7.site-profile` already established."
  [^java.util.Random rng ground-truth chatter]
  (let [spans (timelines/encounter-spans ground-truth)
        mrns (timelines/mrn-timeline ground-truth)
        event-driven (event-driven-chatter rng ground-truth chatter)
        periodic (periodic-chatter rng spans mrns chatter)]
    (assign-restatement-ordinals (into event-driven periodic))))

(defn- chatter-message
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
  (let [ts (hl7-timestamp reference-date at utc-offset)
        persona (timelines/demographics-at demographics patient-id at)
        opener (:opener (get spans encounter-id))
        facility-name (name (:id facility))]
    (parser/str-message
     (apply parser/create-message
            parser/DEFAULT-DELIMITERS
            (msh-segment site-profile {:type "ADT" :trigger trigger} control-id ts)
            (segments/evn-segment trigger ts)
            (pid-segment active-mrn persona)
            (concat
             (when (= "A08" trigger)
               [(segments/pv1-segment site-profile
                             (if (= :outpatient-visit (:event opener)) :outpatient :inpatient)
                             facility-name (:location opener) nil
                             (er7/provider-by-id providers (:attending opener))
                             nil encounter-id)])
             (when (and in1? (:payer persona))
               [(segments/in1-segment (:payer persona))]))))))

(defn plan-charges
  "GT x ChargesProfile (ehrt.sim-model.config/ChargesProfile) ->
  `{:lines {[encounter-id closer-t] [line ...]} :skipped {code n}}`.

  NO RNG AT ALL, unlike `plan-latency` and `plan-chatter`: a charge is
  a pure function of the log and the price table. ADR-0175 section
  2(c)'s own rejected option (3) is why -- `a price that changes per
  run is not a price`.

  KEYED BY (encounter, closer instant), not by encounter alone, because
  an encounter can close TWICE: a `:discharge` undone by a
  `:cancel-discharge` and later re-discharged is ONE encounter closed
  twice (`ehrt.sim-engine.engine/stamp-encounter`'s own account), and
  each close bills the facts that had happened by then. Keying by
  encounter alone would have billed the first close for bed-days it had
  not yet incurred.

  THE SKIP CENSUS IS THE POINT of the `:skipped` half. A code the table
  does not price produces no line and is COUNTED, so a table that
  silently covers a third of a corpus's facts reads as a number rather
  than as a short DFT nobody looks at. Nothing here ever falls back to
  ground truth for a price.

  Absent/nil/{} `charges` plans nothing and skips nothing -- the
  byte-identical path."
  [ground-truth charges]
  (if-not (map? charges)
    {:lines {} :skipped {}}
    (let [price-table (or (:price-table charges) {})
          spans (timelines/encounter-spans ground-truth)
          by-encounter (reduce (fn [acc ev]
                                 (if-let [eid (:encounter-id ev)]
                                   (update acc eid (fnil conj []) ev)
                                   acc))
                               {}
                               ground-truth)]
      (reduce
       (fn [plan [eid {:keys [t0 opener]}]]
         (let [evs (get by-encounter eid)
               inpatient? (= :admission (:event opener))]
           (reduce
            (fn [plan closer]
              (let [close-t (long (:t closer))
                    clinical (for [ev evs
                                   :let [concept (segments/charge-concept ev)]
                                   :when (and concept (<= (long (:t ev)) close-t))]
                               (assoc concept
                                      :at (:t ev)
                                      :procedure? (= :procedure (:event ev))))
                    bed-days (when inpatient?
                               (let [days (max 1 (long (Math/ceil (/ (double (- close-t (long t0)))
                                                                     (double restatement-day-seconds)))))]
                                 (for [k (range days)]
                                   {:code room-and-board-code
                                    :display "Room and board, per day"
                                    :system :local
                                    :at (+ (long t0) (* k restatement-day-seconds))
                                    :procedure? false})))
                    candidates (concat clinical bed-days)
                    priced? #(contains? price-table (:code %))
                    lines (mapv (fn [line]
                                  (let [{:keys [amount display]} (get price-table (:code line))]
                                    (assoc line
                                           :quantity 1
                                           :amount amount
                                           :display (or display (:display line) ""))))
                                (filter priced? candidates))]
                (-> plan
                    (cond-> (seq lines) (assoc-in [:lines [eid close-t]] lines))
                    (update :skipped
                            (fn [m] (reduce (fn [m l] (update m (:code l) (fnil inc 0)))
                                            m
                                            (remove priced? candidates)))))))
            plan
            (filter #(registry/charge-closing-kinds (:event %)) evs))))
       {:lines {} :skipped {}}
       (sort-by (comp :opener-index val) spans)))))

;; --- ARC 4 SWEEP 3 (ADR-0175 design (b), ruling B1): status ladders --------
;; ORM^O01 restatements carrying ORC-5, ORU^R01 restatements carrying
;; OBR-25/OBX-11, at fixed fractions of an order's own
;; `:order-placed` -> `:result-available` interval.
;;
;; THERE IS NO DRAW HERE, AT ALL, AND THAT HOLDS ALL THE WAY.
;; `plan-ladders` takes no `java.util.Random` and consumes nothing from
;; the `:emission` family: `:result-available` carries
;; `:order-event-id`, the LOG INDEX of its own order, so both ends of
;; the interval are in the log and a rung at a fixed fraction of it is a
;; pure function of `(log, ladder-config)` -- the same standing this
;; namespace's `plan-charges` already has, and one step stronger than
;; `plan-chatter`'s (which draws, and therefore owes the
;; fixed-consumption law). ADR-0175 section 2(b)'s rejected option (2)
;; is the reason the fractions are not sampled: a sampled rung costs a
;; second RNG consumer for no realism the fixed fractions do not buy,
;; and it makes the rung un-derivable from the log alone. Nothing below
;; needs a fixed-consumption law because nothing below consumes.
;;
;; LADDER RUNGS ADD NO `message-type-registry` ENTRY, for chatter's own
;; reason: a registry entry claims that one ground-truth event renders
;; one message, and a rung is a restatement of an order that has not
;; finished. What reaches the wire is a family the registry ALREADY
;; carries (ORM^O01, ORU^R01), which is why -- unlike chatter and unlike
;; the DFT -- this sweep co-lands no new `v2-replay/evolve-entry` arm:
;; both triggers have been handled there since M3.

(defn- ladder-stage
  "Rung `k`'s stage: the ladder's `k`th entry, or its last once `k` runs
  past the end. Never nil, never an index error, whatever the config's
  rung count."
  [ladder k]
  (nth ladder (min (long k) (dec (count ladder)))))

(defn- rung-instant
  "`t0 + round(f * (t1 - t0))`, as a long. `Math/round` rather than a
  truncation so a rung at 0.5 of an odd interval lands where a reader
  would put it, and long arithmetic throughout so no rung instant can
  depend on double formatting."
  [t0 t1 f]
  (+ (long t0) (Math/round (* (double f) (double (- (long t1) (long t0)))))))

(defn plan-ladders
  "GT x LadderProfile (ehrt.sim-model.config/LadderProfile) ->
  `{:rungs [instruction ...] :final #{result-control-id ...}}`.

  An instruction is `{:at :family :trigger :basis :basis-control-id
  :active-mrn :stage :seq :ordinal :control-id}`. `:basis` is the LOG
  INDEX of the event the rung restates -- the ORDER for an ORM rung,
  the RESULT for an ORU rung -- and `:basis-control-id` is that event's
  own control id, which is the key `emit-wire` looks the latency offset
  up under. A rung therefore rides the lag of the message it restates
  and can never overtake it: an ORM rung's instant is strictly after
  its order's and an ORU rung's strictly before its result's, and each
  carries the same offset as its basis.

  `:final` IS PER-ORDER, NOT PER-CONFIG. It holds the LOG INDEX of
  every `:result-available` that actually grew a rung, and those are the
  only terminal messages that carry OBR-25/OBX-11. An order whose
  interval admits no rung renders exactly the bytes it rendered before
  ladders existed, which is what makes `no rung => no byte change` an
  assertable property rather than a hope.

  INDICES, NOT CONTROL IDS, and the difference is load-bearing:
  `control-id-for` is not injective over `:result-available` -- two
  results for one patient at one second mint the same MSH-10, which is a
  pre-existing collision (`:bed-status-change`'s own arm of
  `control-id-for` is the shape that fixes this class, and doing it here
  would move every existing corpus's bytes, so it is rowed rather than
  smuggled into an emission sweep). A ladder keyed on that id would put
  final codes on the wrong twin.

  A RUNG MUST LAND STRICTLY INSIDE THE INTERVAL. `(< t0 rung-t t1)` is
  checked after rounding, not before, so a fraction that rounds onto
  either endpoint produces no rung rather than a duplicate of a message
  that already exists at that instant. Zero-length intervals (an order
  and its result at the same second) therefore ladder not at all.

  Absent/nil/{} `ladders` plans nothing -- the byte-identical path,
  the same three-way agreement `plan-chatter`, `plan-latency` and
  `ehrt.sim-emit-hl7.site-profile` already have. NO RNG: see this
  section's own header."
  [ground-truth ladders]
  (if-not (map? ladders)
    {:rungs [] :final #{}}
    (let [evs (vec ground-truth)
          families [{:family :oru :trigger "R01" :fractions (vec (:rungs ladders))
                     :ladder result-status-ladder :basis :result}
                    {:family :orm :trigger "O01" :fractions (vec (:order-rungs ladders))
                     :ladder order-status-ladder :basis :order}]
          instructions
          (vec
           (for [[j result] (map-indexed vector evs)
                 :when (= :result-available (:event result))
                 :let [i (:order-event-id result)
                       order (when (and (integer? i) (< -1 (long i) (count evs)))
                               (nth evs (long i)))]
                 :when (= :order-placed (:event order))
                 :let [t0 (:t order) t1 (:t result)]
                 {:keys [family trigger fractions ladder basis]} families
                 [k f] (map-indexed vector fractions)
                 :let [at (rung-instant t0 t1 f)
                       basis-ev (if (= :order basis) order result)
                       basis-index (if (= :order basis) (long i) j)]
                 :when (< (long t0) at (long t1))]
             {:at at
              :family family
              :trigger trigger
              :basis basis-index
              :basis-control-id (control-id-for basis-ev)
              :active-mrn (:active-mrn basis-ev)
              :result-index j
              :order-index (long i)
              :stage (ladder-stage ladder k)
              :seq k}))
          ;; SORTED BEFORE THE ORDINALS ARE STAMPED, and the sort is
          ;; part of the contract rather than tidiness: the ordinal
          ;; disambiguates two rungs at one instant, so which of them is
          ;; 0 must be a function of the log and not of the order the
          ;; comprehension above happened to walk its two families in.
          sorted (vec (sort-by (juxt :at :basis :family :seq) instructions))
          stamped (assign-restatement-ordinals sorted)]
      {:rungs stamped
       :final (into #{} (comp (filter #(= :oru (:family %))) (map :result-index))
                    stamped)})))

(defn- ladder-message
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

(defn emit-wire
  "GT x reference-date x utc-offset x facility x providers x
  site-profile x offsets [x emission] -> TimedWire: the SAME messages
  `emit` would render, split-clock (each builder's own ADR-0109
  docstring has the per-type detail: MSH-7 shifted by `offsets`, every
  clinical-time field -- EVN-2 where present -- unshifted), returned
  SORTED BY TRANSMIT TIME rather than log order -- out-of-order
  clinical arrival (a lagged admission whose transmit instant lands
  after a later event's own) falls out of this sort, not out of any
  special-cased reordering logic. Ties (equal transmit seconds) break
  on original log position, stable -- the identity property's own
  mechanism: absent/nil/{} `offsets` makes every transmit second equal
  its own log-order `:t`, and since ground truth is already
  `:t`-nondecreasing (`sim-engine`'s own priority-queue invariant), the
  stable tie-break reproduces `emit`'s exact order, and therefore its
  exact bytes.

  `offsets` is plain data (`plan-latency`'s own output, or hand-built)
  -- this function takes no RNG at all, per this namespace's own
  renders-only doctrine.

  ARC 4 SWEEP 2 adds the optional 8th argument, `emission`:
  `{:chatter <plan-chatter's own output> :charges <plan-charges's own
  :lines>}`. Absent, nil, or {} is the byte-identical path -- the
  seven-argument arity below is exactly that, so no existing caller
  moves. The sort key is `[transmit-t log-index lane sub]`: `lane` 0 is
  every message a ground-truth event renders, in `event->messages`' own
  order (so a `:discharge`'s ADT^A03 still precedes the DFT^P03 that
  closes the same encounter), `lane` 1 is chatter, and `sub` is the
  ordinal within each. Chatter carries no offset, so a chatter
  message's transmit instant is its own `:at` and the latency plan for
  every non-chatter message is untouched.

  ARC 4 SWEEP 3 (ADR-0175 design (b)) adds `:ladders` to `emission`:
  `plan-ladders`' own `{:rungs [...] :final #{...}}`. The rungs take
  LANE 2, and the `:final` set -- LOG INDICES -- decides, per event,
  whether `event->messages` renders a terminal status. That is the one
  place this sweep moves an existing message's bytes: a terminal ORU^R01
  whose order grew a rung gains OBR-25 and OBX-11.

  ARC 4 SWEEP 4 (ADR-0175 ruling B1) adds `:siu` to `emission`, and it
  is unlike the three above in one way worth naming: it adds no lane.
  Scheduling's four kinds are GROUND-TRUTH events with registry entries,
  so an SIU rides LANE 0 at its own event's own log index, exactly where
  that event's ADT would ride if it had one. What `:siu` switches is
  whether that lane-0 slot is filled at all. Absent or nil is today
  byte-for-byte at every corpus, because every one of them renders zero
  SIU messages without it.

  A LADDER RUNG DOES CARRY AN OFFSET, unlike a chatter restatement, and
  the difference is not an inconsistency. Chatter has no basis event to
  take a lag from -- a periodic A08 restates a patient, not an event --
  while a rung restates one specific message whose own lag is in the
  plan, so it is looked up under `:basis-control-id` and the rung rides
  it. The consequence is the ordering law the ladder needs: an ORM rung
  transmits after its own order and an ORU rung before its own result,
  under every latency profile, because each pair shares one offset and
  the rung's instant is strictly inside the interval."
  ([ground-truth reference-date utc-offset facility providers site-profile offsets]
   (emit-wire ground-truth reference-date utc-offset facility providers site-profile offsets {}))
  ([ground-truth reference-date utc-offset facility providers site-profile offsets
    {:keys [chatter charges ladders siu]}]
   (let [demographics (timelines/demographics-timeline ground-truth)
         offsets (or offsets {})
         chatter (or chatter [])
         charges (or charges {})
         ground-truth (vec ground-truth)
         rungs (:rungs ladders)
         final-result-indices (or (:final ladders) #{})
         spans (when (seq chatter) (timelines/encounter-spans ground-truth))
         base (->> ground-truth
                   (map-indexed
                    (fn [i ev]
                      (let [control-id (control-id-for ev)
                            transmit-t (hl7-time/transmit-seconds offsets control-id (:t ev))]
                        (map-indexed
                         (fn [j message] [transmit-t i 0 j message])
                         (event->messages reference-date utc-offset facility providers demographics
                                          site-profile offsets charges
                                          (when (contains? final-result-indices i) {:stage :final})
                                          siu ev)))))
                   (apply concat))
         restatements (map (fn [ins]
                             [(:at ins) (:basis ins) 1 (:ordinal ins)
                              (chatter-message reference-date utc-offset facility providers
                                               demographics site-profile spans ins)])
                           chatter)
         ladder-rungs (map (fn [ins]
                             [(hl7-time/transmit-seconds offsets (:basis-control-id ins) (:at ins))
                              (:basis ins) 2 (:seq ins)
                              (ladder-message reference-date utc-offset facility providers
                                              demographics site-profile offsets ground-truth ins)])
                           rungs)]
     (->> (concat base restatements ladder-rungs)
          (sort-by (fn [[transmit-t i lane sub _]] [transmit-t i lane sub]))
          (mapv peek)))))
