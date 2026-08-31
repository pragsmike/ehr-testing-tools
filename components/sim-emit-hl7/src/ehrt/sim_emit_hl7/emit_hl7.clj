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
  (:require [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-emit-hl7.hl7-time :as hl7-time]
            [ehrt.sim-emit-hl7.registry :as registry]
            [ehrt.sim-emit-hl7.timelines :as timelines]
            [ehrt.sim-emit-hl7.er7 :as er7]
            [ehrt.sim-emit-hl7.segments :as segments]
            [ehrt.sim-emit-hl7.messages :as messages]))

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
;; `emit_hl7_test.clj`, `result_clock_test.clj` and `latency_test.clj`.
;; It had twenty bare-name sites in this file too, and a twenty-first
;; that left with `segments`; all twenty left with `messages` in cluster
;; 6, so the def now stands for the test tree alone.
;;
;; The FOUR private movers get no def -- that would widen this file's
;; public surface, which C1(a) does not ask for. `transmit-seconds` is
;; public in `hl7-time` instead, because eleven forms called it at the
;; time of that move. Ten of those left with `messages` in cluster 6;
;; `emit-wire` alone still names `hl7-time/transmit-seconds`, twice.

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
;; NO BANNER TRAVELLED with this cluster, a first for this file. All four
;; comment blocks heading a moved region headed a section this cluster
;; SPLIT, and each of the four named a MESSAGE type -- SIU^S12,
;; ORM^O01+ORU^R01, :observation, DFT^P03 -- whose builder stayed. The M3
;; header set that precedent for cluster 4. All four then travelled with
;; `messages` in cluster 6, when the builders left too.
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
;; def keeps each var here without widening this file's public surface by
;; a name. That is the C7 extension `tn-field` established one cluster
;; ago, applied twice, exactly where cluster 4 predicted it. Their
;; twenty-two call sites resolved through these defs unqualified until
;; cluster 6 took every one of them; like `tn-field`'s, both defs now
;; stand for a test's var access and a namespace claim alone.
;;
;; The other TWELVE private movers are widenings too -- every one had a
;; caller that stayed behind -- so they are public in `segments` instead
;; and gain NO delegating def, because widening this file's own public
;; surface is not what C1(a) asks for. Thirty-five call sites below named
;; them `segments/...`; cluster 6 took thirty-four, and `plan-charges`'
;; `segments/charge-concept` is the one that remains.
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
;; twenty-six left again with `segments` in cluster 5 and the remaining
;; SEVENTEEN, across ten forms, left with `messages` in cluster 6. They
;; are public in `er7` instead, every one of the forty-one names them
;; `er7/...` from wherever it now sits, and NOT ONE call site is left in
;; this file. They gain NO delegating def, because widening this file's
;; own public surface is not what C1(a) asks for.
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

;; --- moved to `ehrt.sim-emit-hl7.messages` (extraction cluster 6 of 8) --
;;
;; THIRTEEN forms -- the twelve per-kind message builders (single-subject
;; ADT, bed swap, bed status, SIU, merge, ORM, ORU, the :observation and
;; :diagnostic-report ORUs, DFT, chatter and ladder rungs) and
;; `event->messages`, which dispatches one ground-truth event to them --
;; left this file for `messages.clj`, from THREE regions: everything from
;; here to just above `default-providers`; `chatter-message` from between
;; `plan-chatter` and `plan-charges`; and `ladder-message` from just above
;; `emit-wire`. It is the heaviest cluster in the file at 578 form-lines,
;; and the most connected: 122 distinct cross-seam calls into five landed
;; siblings, census 3b's five `messages`-as-caller rows exactly.
;;
;; FIVE BANNERS TRAVEL, the exact inverse of cluster 5. The four comment
;; blocks that session left behind -- SIU^S12, ORM^O01+ORU^R01,
;; :observation and DFT^P03 -- each headed a section it SPLIT, and each
;; named a message type whose builder stayed. The builders are what
;; leaves now, so all four sections are wholly this cluster's and all
;; four blocks go with them, along with the D1 ORC+OBR note that heads
;; the diagnostic report.
;;
;; ONE delegating def below. `event->messages` is the cluster's only
;; PUBLIC mover, and `interface.clj` re-exports NONE of the thirteen, so
;; the def is owed to the TREE alone: five `emit-hl7/event->messages`
;; sites in `emit_hl7_test.clj` and one in `sim-engine`'s
;; `bed_cycle_test.clj`. No `^:private` def is owed either -- all 106
;; `#'` sites in the tracked tree were re-read and none names a mover.
;;
;; TEN of the twelve private movers STAY PRIVATE, the largest such set in
;; either file. `event->messages` calls all ten and `ladder-message` calls
;; two of them, so every caller travelled and census constraint 5, read as
;; a PROHIBITION, leaves them unwidened. The other TWO, `chatter-message`
;; and `ladder-message`, widen because `emit-wire` stayed behind and calls
;; both; they gain no def, and their two call sites in `emit-wire` name
;; them `messages/...`.
;;
;; SIXTY-FOUR bare names had to be requalified in the MOVED text, the
;; class cluster 5 opened at five: `hl7-time/hl7-timestamp` (20),
;; `segments/msh-segment` (11), `segments/pid-segment` (11),
;; `segments/control-id-for` (10), `registry/message-type-registry` (10),
;; `registry/siu-event-kinds` and `registry/siu-renders?`. Every one of
;; them resolved here only through the delegating defs above, and an
;; implementation may not require its facade.
;;
;; This move leaves ONE dead require behind: `com.nervestaple.hl7-parser.
;; parser`, whose thirty-three call sites in this file were all inside the
;; thirteen movers. `emit_hl7.clj` builds no message text of its own any
;; more -- it plans, and delegates the rendering.

(def event->messages messages/event->messages)

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
                              (messages/chatter-message reference-date utc-offset facility providers
                                               demographics site-profile spans ins)])
                           chatter)
         ladder-rungs (map (fn [ins]
                             [(hl7-time/transmit-seconds offsets (:basis-control-id ins) (:at ins))
                              (:basis ins) 2 (:seq ins)
                              (messages/ladder-message reference-date utc-offset facility providers
                                              demographics site-profile offsets ground-truth ins)])
                           rungs)]
     (->> (concat base restatements ladder-rungs)
          (sort-by (fn [[transmit-t i lane sub _]] [transmit-t i lane sub]))
          (mapv peek)))))
