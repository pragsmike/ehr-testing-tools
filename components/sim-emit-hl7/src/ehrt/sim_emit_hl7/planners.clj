(ns ehrt.sim-emit-hl7.planners
  "The emitter's arc-4 add-on planners: `plan-latency`'s per-event
  transmit delays, `plan-chatter`'s event-driven and periodic
  re-statements, `plan-charges`' per-encounter DFT lines, and
  `plan-ladders`' order/result status rungs. Four pure functions that
  read the ground-truth log and return INSTRUCTIONS; `emit-wire`, which
  stayed behind at this move and left in cluster 8 for
  `ehrt.sim-emit-hl7.emit`, is what renders them.

  Extracted VERBATIM from `emit_hl7.clj`, the SEVENTH cluster of that
  file's namespace extraction (`.agents/plans/engine-extraction-
  census.md` sections 2 and 2a, `roadmap.md#engine-namespace-extraction-
  and-apply-unification`). It is the only cluster with ZERO INCOMING
  EDGES -- nothing in `emit_hl7.clj` ever called a planner -- so census
  2a placed it here by JUDGMENT rather than by the graph, free anywhere
  after its own three dependencies: `registry` (six edges), `timelines`
  (four) and `segments` (three), which are census 3b's three
  `planners`-as-caller rows reproduced exactly. It is also the first
  cluster in either file whose distinct (caller, callee) PAIRS and raw
  call SITES are the same number, thirteen and thirteen.

  It moved as ONE CONTIGUOUS BLOCK, another first: lines 302-731 of
  `emit_hl7.clj` were nothing but these eleven forms, the three banners
  heading them and the blank lines between, so the move is a single cut
  rather than a gather across regions.

  `emit_hl7.clj` remains the namespace every existing requirer resolves
  against (author ruling C1(a)) and keeps FOUR delegating defs:
  `plan-latency`, `plan-chatter`, `plan-charges` and `plan-ladders`.
  Every one is load-bearing at RUNTIME rather than only in prose.
  `interface.clj` re-exports all four and reaches them as
  `emit-hl7/plan-...`, so the chain `ehrt.sim.run` -> `interface.clj`
  -> the delegating def -> here must hold at every link, and C1(a)
  fences `interface.clj` from edits. Four `sim-emit-hl7` test files
  alias the IMPLEMENTATION namespace directly and call the same four
  names, so each def is owed twice over.

  The SIX private movers -- `chatter-trigger`, `event-driven-chatter`,
  `periodic-chatter`, `assign-restatement-ordinals`, `ladder-stage` and
  `rung-instant` -- ALL STAY PRIVATE. Census constraint 5, read as a
  PROHIBITION, has nothing to widen for: no caller stayed behind because
  no caller was ever outside these eleven forms. Six of six is the first
  time a cluster's whole private set survives intact.
  `restatement-day-seconds` travels public and gains no delegating def
  either -- it is a public `def` whose only two callers, `periodic-
  chatter` and `plan-charges`, travelled with it.

  SEVEN bare names in the moved text resolved only through
  `emit_hl7.clj`'s own delegating defs and are requalified here:
  `registry/chatter-event-kinds` and `segments/control-id-for` twice
  each, `registry/room-and-board-code`, `registry/order-status-ladder`
  and `registry/result-status-ladder` once each. Five names -- exactly
  the five cluster 6 named in advance -- and the class falls from
  sixty-four sites to seven because this cluster's heavier crossings
  (`timelines/encounter-spans`, `segments/charge-concept`,
  `registry/charge-closing-kinds`) were already qualified before the
  seam.

  THREE BANNER BLOCKS TRAVEL whole: ADR-0109's second clock, ARC 4
  SWEEP 2's re-statement chatter, ARC 4 SWEEP 3's status ladders.
  Three sentences inside them stopped being true at the seam and are
  corrected in the move commit rather than a commit later -- two said
  sampling and the renders-only doctrine belong to THIS NAMESPACE, one
  said to THIS FILE, and all three meant the EMITTER, which this
  namespace is not: it plans, and renders nothing. Nothing else differs,
  across 364 form-lines.

  DISCLOSED AND NOT FIXED (`rulings.md#R-move-not-improve`):
  `plan-charges` cites `ehrt.sim-engine.engine/stamp-encounter`, which
  has not resolved since the third engine session -- that form lives in
  `ehrt.sim-engine.encounters` and `engine.clj`'s facade does not
  re-export it. The citation was already false when this move relocated
  it, which is the STALE-BEFORE-THE-MOVE class reaching moved text for
  the first time."
  (:require [ehrt.sim-emit-hl7.registry :as registry]
            [ehrt.sim-emit-hl7.timelines :as timelines]
            [ehrt.sim-emit-hl7.segments :as segments]))

;; --- ADR-0109: the second clock -- GT x LatencyParams -> TimedWire -------
;; The extension point docs/dev/simulator-architecture.md section 5 named
;; and built nothing of: an arrow between `engine`'s own GT output and
;; the emitter's own `emitH` consumption of it, so a message's own
;; wire-emission instant can lag its clinical-event instant by a
;; realistic, sampled delay. Sampling itself stays OUT of the emitter
;; (`emit_hl7.clj`'s own renders-only doctrine, restated in that same
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
                    (when-let [control-id (segments/control-id-for ev)]
                      [control-id (long (Math/round (* 60.0 (+ from-minutes (* draw (- to-minutes from-minutes))))))])))))
        ground-truth))

;; --- ARC 4 SWEEP 2 (ADR-0175 design (a), ruling B1): re-statement chatter --
;; A08 / A31 / A28 / IN1-only. `plan-chatter` is `plan-latency`'s
;; sibling and keeps the emitter's renders-only doctrine intact: it
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
  (or (:trigger (registry/chatter-event-kinds kind))
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
                            :in1? (boolean (:in1? (registry/chatter-event-kinds kind)))})
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
                                   {:code registry/room-and-board-code
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
                     :ladder registry/result-status-ladder :basis :result}
                    {:family :orm :trigger "O01" :fractions (vec (:order-rungs ladders))
                     :ladder registry/order-status-ladder :basis :order}]
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
              :basis-control-id (segments/control-id-for basis-ev)
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
