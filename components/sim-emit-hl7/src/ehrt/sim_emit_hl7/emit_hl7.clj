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
            [ehrt.sim-emit-hl7.messages :as messages]
            [ehrt.sim-emit-hl7.planners :as planners]))

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
;; six test files. There were bare-name sites below too, resolving
;; through these defs; cluster 7 took the last five of them, so all ten
;; defs now stand for the test tree and `interface.clj` alone.
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
;; them `segments/...`; cluster 6 took thirty-four and cluster 7 took the
;; last, `plan-charges`' `segments/charge-concept`. None remains.
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
;; the EMITTER -- the engine's `decide` left eighteen of nineteen, and is
;; the only larger one. `event->messages` calls all ten and
;; `ladder-message` calls two of them, so every caller travelled and
;; census constraint 5, read as a PROHIBITION, leaves them unwidened.
;; The other TWO, `chatter-message` and `ladder-message`, widen because
;; `emit-wire` stayed behind and calls both; they gain no def, and their
;; two call sites in `emit-wire` name them `messages/...`.
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
;; thirteen movers. `emit_hl7.clj` built no message text of its own after
;; that move -- it planned, and delegated the rendering. Cluster 7 took
;; the planning too, so what it does now is neither: it delegates both.

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

;; --- moved to `ehrt.sim-emit-hl7.planners` (extraction cluster 7 of 8) --
;;
;; ELEVEN forms -- `plan-latency`, `plan-chatter` and its four helpers,
;; `restatement-day-seconds`, `plan-charges`, and `plan-ladders` with its
;; two -- left this file for `planners.clj`, from ONE region: everything
;; between `emit` and `emit-wire`, banners and blank lines included. Every
;; prior cluster gathered its movers from three regions or more; this one
;; was already a contiguous block, which is the shape a cluster with no
;; incoming edge tends to have.
;;
;; NOTHING HERE EVER CALLED A PLANNER. Census 2a placed this cluster
;; seventh by judgment, not by the graph: it has ZERO incoming edges, so
;; it was free anywhere after `registry`, `timelines` and `segments`. The
;; consequence is that this file gains a `:require` on `planners` that no
;; call site needs -- the four delegating defs below are its only reason
;; to exist, and they are reason enough.
;;
;; THREE BANNERS TRAVEL, whole: ADR-0109's second clock, ARC 4 SWEEP 2's
;; re-statement chatter and ARC 4 SWEEP 3's status ladders. Each headed a
;; section wholly this cluster's, so none had to be split.
;;
;; FOUR delegating defs below, and they are the most load-bearing set the
;; emit phase has produced. `interface.clj` re-exports all four -- the
;; heaviest such share of a cluster's publics anywhere in this file, four
;; of five -- and calls them as `emit-hl7/plan-...`, so `ehrt.sim.run`
;; reaches a planner through interface, def and namespace in that order.
;; `charges_test.clj`, `chatter_test.clj`, `latency_test.clj` and
;; `ladders_test.clj` alias THIS namespace directly and call the same four,
;; so every def is owed twice over.
;;
;; The SIX private movers stay private -- `chatter-trigger`,
;; `event-driven-chatter`, `periodic-chatter`,
;; `assign-restatement-ordinals`, `ladder-stage` and `rung-instant`. Not
;; one caller stayed behind, because not one caller was ever outside the
;; cluster, so census constraint 5 read as a PROHIBITION leaves six of six
;; unwidened: the first cluster in either file whose whole private set
;; survives. `restatement-day-seconds` is public and travels public, with
;; no def, having no caller outside the eleven either.
;;
;; SEVEN bare names had to be requalified in the MOVED text, five names:
;; `registry/chatter-event-kinds` and `segments/control-id-for` twice each,
;; `registry/room-and-board-code`, `registry/order-status-ladder` and
;; `registry/result-status-ladder` once. They are exactly the five cluster
;; 6 predicted by name. Sixty-four sites there, seven here -- depth drives
;; the class, but so does how much of a cluster was already qualified.
;;
;; This move leaves NO dead require: `registry`, `timelines` and
;; `segments` all keep in-file uses, and `planners` is added, so the `ns`
;; above goes from seven aliases to eight. What it does end is this file's
;; last piece of work of its own. Everything left below `emit-wire`
;; delegates, and the three forms that remain -- `default-providers`,
;; `emit` and `emit-wire` -- are census 2's `facade` cluster exactly.

(def plan-latency planners/plan-latency)
(def plan-chatter planners/plan-chatter)
(def plan-charges planners/plan-charges)
(def plan-ladders planners/plan-ladders)

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
