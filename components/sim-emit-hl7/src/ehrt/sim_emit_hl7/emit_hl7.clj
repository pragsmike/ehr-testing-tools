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
  (:require [ehrt.sim-emit-hl7.hl7-time :as hl7-time]
            [ehrt.sim-emit-hl7.registry :as registry]
            [ehrt.sim-emit-hl7.er7 :as er7]
            [ehrt.sim-emit-hl7.segments :as segments]
            [ehrt.sim-emit-hl7.planners :as planners]
            [ehrt.sim-emit-hl7.emit :as emit]))

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
;; The THREE public movers kept a delegating def here, so
;; `interface.clj` (`default-reference-date`, `default-utc-offset`) and
;; the test tree resolved exactly as before. `hl7-timestamp`'s was owed
;; to the tree rather than to `interface.clj`, which never re-exported
;; it: thirteen call sites across `emit_hl7_test.clj`,
;; `result_clock_test.clj` and `latency_test.clj`, plus twenty bare-name
;; sites in this file that all left with `messages` in cluster 6.
;;
;; THE RULED REPOINT PASS RETIRED `hl7-timestamp` -- those thirteen sites
;; name `hl7-time/hl7-timestamp` outright now, and nothing else ever
;; reached it here. The two `interface.clj` re-exports remain.
;;
;; The FOUR private movers get no def -- that would widen this file's
;; public surface, which C1(a) does not ask for. `transmit-seconds` is
;; public in `hl7-time` instead, because eleven forms called it at the
;; time of that move. Ten of those left with `messages` in cluster 6;
;; `emit-wire` alone still named `hl7-time/transmit-seconds`, twice,
;; until cluster 8 took it too; no form in this file names it now.

(def default-reference-date hl7-time/default-reference-date)
(def default-utc-offset hl7-time/default-utc-offset)
;; --- moved to `ehrt.sim-emit-hl7.registry` (extraction cluster 2 of 8) ---
;;
;; Thirteen forms -- the message-type catalog, the three MSH-9
;; vocabularies derived from it, scheduling's kinds and SCH-25 states,
;; the charge tables, chatter's kind map and the two status ladders --
;; left this file for `registry.clj`. It is a LEAF: nothing in it calls
;; anything outside itself, so it takes no `:require` with it.
;;
;; The TEN public movers kept a delegating def here, so `interface.clj`
;; (which re-exports seven of them -- `skeleton-message-types`,
;; `add-on-message-types`, `emittable-message-types`, `siu-event-kinds`,
;; `siu-renders?`, `room-and-board-code`, `chatter-event-kinds`) and the
;; test tree resolved exactly as before. `message-type-registry`,
;; `order-status-ladder` and `result-status-ladder` were owed a def by
;; THIS FILE rather than by `interface.clj`, which never re-exported
;; them: thirty-four call sites across six test files for the first, and
;; none at all for the other two.
;;
;; THE RULED REPOINT PASS RETIRED EXACTLY THOSE THREE and kept the seven
;; `interface.clj` names. The split the banner drew is the split the
;; retirement made.
;;
;; The THREE private movers get no def -- that would widen this file's
;; public surface, which C1(a) does not ask for. `siu-filler-status` and
;; `charge-closing-kinds` are public in `registry` instead, because
;; `sch-segment`, `event->messages` and `plan-charges` still call them;
;; those three call sites name them `registry/...`. `final-result-stage`
;; had no caller anywhere in the tree and was DELETED by the ruled
;; repoint pass, which is what a form that arrived dead is owed.

(def skeleton-message-types registry/skeleton-message-types)
(def add-on-message-types registry/add-on-message-types)
(def emittable-message-types registry/emittable-message-types)
(def siu-event-kinds registry/siu-event-kinds)
(def siu-renders? registry/siu-renders?)
(def room-and-board-code registry/room-and-board-code)
(def chatter-event-kinds registry/chatter-event-kinds)
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
;; THREE delegating defs stood here. `control-id-for` is the cluster's
;; only PUBLIC mover and the one form `interface.clj` re-exports, so its
;; def was owed twice over and REMAINS. `msh-segment` and `pid-segment`
;; are PRIVATE movers and both widened -- eleven callers each stayed
;; behind -- but `emit_hl7_test.clj` reached them as
;; `(#'emit-hl7/msh-segment ...)` and `(#'emit-hl7/pid-segment ...)`, var
;; accesses on private vars that no move can carry and that C1(a) forbade
;; editing, so each got a `^:private` delegating def: the C7 extension
;; `tn-field` established one cluster earlier, applied twice, exactly
;; where cluster 4 predicted it.
;;
;; THE RULED REPOINT PASS RETIRED BOTH C7 DEFS. C12(b) lifted the fence
;; that created them, the two var accesses now read
;; `(#'segments/msh-segment ...)` and `(#'segments/pid-segment ...)`, and
;; a `^:private` def with no var access left to answer is nothing at all.
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
;; The TWO public movers kept a delegating def here, so `v2_replay.clj`'s
;; two reader call sites and the four `escape-er7`/`unescape-er7` sites in
;; `emit_hl7_test.clj` resolved exactly as before. `interface.clj`
;; re-exports neither -- the first cluster here whose defs were owed to
;; the TREE alone rather than to the interface, and the reason the two
;; parted ways when the ruled repoint pass came.
;;
;; THAT PASS RETIRED `escape-er7` AND KEPT `unescape-er7`, which is the
;; one asymmetry in the whole retirement. `escape-er7` was reached only
;; from `emit_hl7_test.clj`, which now names `er7/` outright.
;; `unescape-er7` has a SRC caller that the pass does not touch --
;; `v2_replay.clj`, a sibling implementation reaching this facade -- so
;; its def stays until something moves that.
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
;; `tn-field` was the exception, and the reason for a third def here. It
;; is a widening like the other ten, but `v2_replay_test.clj` reached it
;; as `(#'emit-hl7/tn-field phone)` -- a var access on a PRIVATE var,
;; which no move can carry and which C1(a) forbade editing -- so a
;; `^:private` delegating def kept that var here without widening this
;; file's public surface by a name. That was the C7 extension's first
;; case; `segments` took it twice one cluster later.
;;
;; THE RULED REPOINT PASS RETIRED IT. C12(b) lifted the fence, the var
;; access now reads `(#'er7/tn-field phone)`, and `v2_replay.clj`'s
;; namespace claim names `ehrt.sim-emit-hl7.er7` -- so neither of the two
;; things a move could not carry is here any more.

(def unescape-er7 er7/unescape-er7)
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
;; ONE delegating def stood here. `event->messages` is the cluster's only
;; PUBLIC mover, and `interface.clj` re-exports NONE of the thirteen, so
;; the def was owed to the TREE alone: five sites in `emit_hl7_test.clj`
;; and one in `sim-engine`'s `bed_cycle_test.clj`. No `^:private` def was
;; owed either -- all 106 `#'` sites in the tracked tree were re-read and
;; none named a mover.
;;
;; THE RULED REPOINT PASS RETIRED IT, and with it the last use of this
;; file's `ehrt.sim-emit-hl7.messages` require, which is dropped from the
;; `ns` above. Those six sites name
;; `ehrt.sim-emit-hl7.messages/event->messages` outright now.
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
;; `ladders_test.clj` aliased THIS namespace directly and called the same
;; four; the ruled repoint pass moved those reaches to `planners/`, so
;; what the four defs are owed to now is `interface.clj` alone -- which
;; is reason enough, and the reason all four survive the retirement.
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
;; That move left NO dead require: `registry`, `timelines` and
;; `segments` all still had in-file uses then, and `planners` was added,
;; so the `ns` above went from seven aliases to eight. What it ended was
;; this file's last piece of work of its own apart from the facade
;; itself: the three forms that outlived it -- `default-providers`,
;; `emit` and `emit-wire` -- were census 2's `facade` cluster exactly,
;; and cluster 8 took them too.

(def plan-latency planners/plan-latency)
(def plan-chatter planners/plan-chatter)
(def plan-charges planners/plan-charges)
(def plan-ladders planners/plan-ladders)

;; --- moved to `ehrt.sim-emit-hl7.emit` (extraction cluster 8 of 8) ------
;;
;; THREE forms -- `default-providers`, `emit` and `emit-wire` -- left this
;; file for `emit.clj`, from TWO regions: `default-providers` and `emit`
;; from between the `event->messages` def above and the `planners` banner,
;; and `emit-wire` from below the planner defs. NO BANNER TRAVELLED,
;; because neither region carried one.
;;
;; Census 2 called this cluster `facade` and expected THIS FILE to keep
;; it. Author ruling C11(a) moved it instead, so the census word went with
;; the forms rather than with the file, and the namespace over there is
;; named for `emit` -- the rule that named `ehrt.sim-engine.run` too.
;;
;; WITH THEM GONE THIS FILE IS A PURE FACADE: its `ns`, its delegating
;; defs and seven explanatory comment blocks, and no executable code of
;; its own. That is the shape `engine.clj` reached under ruling C4(b),
;; and reaching it here closed the extraction phase of
;; `roadmap.md#engine-namespace-extraction-and-apply-unification`. The
;; two files stood at 741 and 383 lines, twenty-six defs here and
;; forty-three there; the ruled repoint pass then retired nine of these
;; and thirty-one of those, leaving SEVENTEEN here and twelve there.
;;
;; THE CALLER TRAVELS, which no cluster before this one did and which
;; census 2a named in advance as the shape that could only arrive last.
;; `emit` and `emit-wire` are this file's own CALLERS -- census 3b gives
;; them four rows, into `messages` (4 edges), `timelines` (3), `hl7-time`
;; (2) and `segments` (1), and all four reproduce exactly -- so four bare
;; names that resolved here through the delegating defs are qualified over
;; there instead: `hl7-time/default-utc-offset` once,
;; `messages/event->messages` twice, `segments/control-id-for` once.
;;
;; NO SHIM WAS NEEDED, and that is the difference from the engine's own
;; caller-travels move rather than an oversight. `ehrt.sim-engine.run` had
;; to reach `stream` back through `engine.clj` because a test perturbs
;; that var by `with-redefs` (census constraint 1), and an implementation
;; may not require its facade. Nothing here has that shape: all 108 `#'`
;; sites in the tracked tree were re-read, and no `resolve`, `ns-resolve`,
;; `requiring-resolve`, `with-redefs`, `alter-var-root`, `intern` or
;; `find-var` form anywhere names `emit`, `emit-wire` or
;; `default-providers`.
;;
;; TWO delegating defs below, and they are the brick's load-bearing entry
;; points. `interface.clj` re-exports both as `defn` wrappers that call
;; them at RUNTIME -- `emit` at three arities, `emit-wire` at two -- so
;; the chain `ehrt.sim.run` -> `interface.clj` -> the def -> `emit.clj`
;; must hold at every link, and `components/oracle`'s `digest.clj` -- the
;; regression oracle's own instrument -- takes exactly that chain at its
;; `:228`, where its own `emit-hl7` alias names `interface.clj` rather
;; than this file. `emit_hl7_test.clj` reached the def directly at
;; sixty-one sites until the ruled repoint pass moved them to
;; `emit/emit`; both defs survive that pass on `interface.clj` alone.
;;
;; `default-providers` was `^:private` here and stays private over there
;; with NO def. Both its callers are `emit`'s own lower arities and both
;; travelled, so census constraint 5, read as a PROHIBITION, has nothing
;; to widen for.
;;
;; This move leaves TWO DEAD REQUIRES behind -- `ehrt.sim-model.interface`
;; and `ehrt.sim-emit-hl7.timelines`, whose every use in this file was
;; inside the three movers -- and adds one, so the `ns` above goes from
;; eight aliases to seven. NOT ONE of the twenty-six defs below has an
;; in-file caller any more, which is what a pure facade means measured
;; rather than asserted.

(def emit emit/emit)
(def emit-wire emit/emit-wire)
