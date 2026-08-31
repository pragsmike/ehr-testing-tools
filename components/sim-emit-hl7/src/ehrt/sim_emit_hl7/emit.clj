(ns ehrt.sim-emit-hl7.emit
  "The emitter's two stage functions and the provider pool they fall
  back on: `emit`, ground-truth log -> ER7 message strings in log
  order, and `emit-wire`, the split-clock sibling that renders the SAME
  messages sorted by transmit time. Both are pure functions of their
  arguments -- no RNG, no wall clock -- which is THIS NAMESPACE'S OWN
  RENDERS-ONLY DOCTRINE, the emitter-wide rule `emit_hl7.clj`'s `ns`
  docstring states and `planners.clj` is the counterpart to: a planner
  samples and returns instructions, and everything here renders them.

  Extracted VERBATIM from `emit_hl7.clj`, the EIGHTH and LAST cluster of
  that file's namespace extraction (`.agents/plans/engine-extraction-
  census.md` sections 2 and 2a, `roadmap.md#engine-namespace-extraction-
  and-apply-unification`). Census 2 calls it `facade` because it is what
  `emit_hl7.clj` was expected to keep; author ruling C11(a) moved it
  instead, so `emit_hl7.clj` ends a PURE FACADE the way `engine.clj` did
  under C4(b), and the name `facade` would now be the wrong one for
  either file. It is named for the form it holds, which is the same rule
  `ehrt.sim-engine.run` was named by.

  THE CALLER TRAVELS, and this is the only cluster of the eight where it
  does -- census 2a flagged that in advance as the shape that arrives
  last. `emit` and `emit-wire` are what called the other seven clusters,
  so their four bare names (`default-utc-offset` once, `event->messages`
  twice, `control-id-for` once) resolved only through `emit_hl7.clj`'s
  own delegating defs and are requalified here to `hl7-time/`,
  `messages/` and `segments/`. NO SHIM IS NEEDED and none is written:
  the engine's own caller-travels move had to reach one var back through
  its facade (census constraint 1's `stream`, a `with-redefs` target),
  and nothing here has that shape -- all 108 `#'` sites in the tracked
  tree were re-read, and no `resolve`/`ns-resolve`/`requiring-resolve`/
  `with-redefs`/`alter-var-root`/`intern`/`find-var` form anywhere names
  `emit`, `emit-wire` or `default-providers`. A facade may require its
  implementations and an implementation may not require its facade; here
  the implementation simply does not need to.

  `emit_hl7.clj` remains the namespace every existing requirer resolves
  against (author ruling C1(a)) and keeps TWO delegating defs, `emit`
  and `emit-wire`. Both are the brick's load-bearing entry points:
  `interface.clj` re-exports each as a `defn` wrapper -- `emit` at three
  arities, `emit-wire` at two -- calling `emit-hl7/emit`/`emit-hl7/emit-
  wire` at RUNTIME, so the chain `ehrt.sim.run` -> `interface.clj` -> the
  delegating def -> here must hold at every link, and C1(a) fences
  `interface.clj` from edits. Both are owed to the tree besides:
  `emit_hl7_test.clj` alone names `emit-hl7/emit` at sixty-one sites,
  and `components/oracle`'s own `digest.clj` -- the regression oracle's
  instrument -- calls it directly at `:228`.

  `default-providers` STAYS PRIVATE and gains NO delegating def. It is
  `^:private` here, its only two callers are `emit`'s own lower arities,
  and both travelled with it; census constraint 5, read as a
  PROHIBITION, has nothing to widen for. One of one, after cluster 7's
  six of six.

  NO BANNER TRAVELS and none is split: the three forms sat in TWO
  REGIONS of `emit_hl7.clj` -- `default-providers` and `emit` between
  the `messages` block and the `planners` banner, `emit-wire` after the
  planner defs -- and neither region carried a comment block of its own.
  SEVEN positional words in the moved prose were enumerated and every
  one survives untouched, so unlike clusters 5 and 7 this move corrects
  no sentence inside the text it moves. Nothing in these 136 form-lines
  differs from `emit_hl7.clj` but the four requalifications."
  (:require [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-emit-hl7.hl7-time :as hl7-time]
            [ehrt.sim-emit-hl7.timelines :as timelines]
            [ehrt.sim-emit-hl7.segments :as segments]
            [ehrt.sim-emit-hl7.messages :as messages]))

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
   (emit ground-truth reference-date hl7-time/default-utc-offset sim-model/default-facility default-providers))
  ([ground-truth reference-date utc-offset]
   (emit ground-truth reference-date utc-offset sim-model/default-facility default-providers))
  ([ground-truth reference-date utc-offset facility providers]
   (emit ground-truth reference-date utc-offset facility providers nil))
  ([ground-truth reference-date utc-offset facility providers site-profile]
   (let [demographics (timelines/demographics-timeline ground-truth)]
     (into [] (mapcat (partial messages/event->messages reference-date utc-offset facility providers demographics site-profile {}))
           ground-truth))))

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
                      (let [control-id (segments/control-id-for ev)
                            transmit-t (hl7-time/transmit-seconds offsets control-id (:t ev))]
                        (map-indexed
                         (fn [j message] [transmit-t i 0 j message])
                         (messages/event->messages reference-date utc-offset facility providers demographics
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
