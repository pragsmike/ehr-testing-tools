(ns ehrt.sim-engine.fold
  "The derived-state fold and THE APPLY CHOKE POINT: `apply-events`,
  `replay`, the bed index, and the two policy sets the in-fold indexes
  are keyed on -- `engine.clj`'s fifth extraction under
  `roadmap.md#engine-namespace-extraction-and-apply-unification` (the
  census's own dependency order, `.agents/plans/engine-extraction-
  census.md` section 3a: `fold` lands after `streams`, `state`,
  `encounters` and `evolve`, and before `log-index`, whose
  `reinstated-state` calls into here).

  THIS NAMESPACE OWNS THE APPLY PATH. Until stage 1 of the
  application-path unification it owned an apply SITE -- `replay`, the
  census's site 2 -- and the other two folded their own way. It now owns
  the ONE fold all three run, `apply-events`, with each site passing the
  accumulator stack it already ran as an explicit declared subset of
  `full-algebra`:

  * site 1, `ehrt.sim-engine.run`'s in-loop fold -- `run-loop-projection`,
    eleven of thirteen;
  * site 2, `replay` below -- `replay-projection`, three of thirteen;
  * site 3, `ehrt.sim-engine.log-index/reinstated-state`'s fallback --
    `reinstated-projection`, site 2's by value.

  STAGE 1 CHANGED NO BEHAVIOUR, by construction rather than by
  assertion: the choke point's order of operations is site 1's,
  unchanged, and every concern is guarded by its own projection
  membership and by nothing else. NOTHING IS ENABLED OR DISABLED HERE.
  The twenty-two omitted (site x accumulator) pairs, and the cone
  prediction for each -- three OUTPUT-MOVING, nineteen INERT -- are
  `.agents/plans/apply-unification-census.md` sections 2 and 3; stage 2
  enables them one commit each, and a delta against a prediction is a
  FINDING.

  TWO FORMS ARRIVED FROM `ehrt.sim-engine.log-index` with stage 1,
  `reinstatable-event-types` and `cited-opening-event-types`, each
  leaving a delegating def there under ruling C1(a). They are apply-site
  policy rather than log queries -- each names which events an in-fold
  index RECORDS, and site 1 was their only live code consumer -- and
  `log-index` requires THIS namespace, so naming them from here while
  they lived there would have closed a require cycle. The census's
  section 4a carries that derivation and the two homes it rejected.

  Extracted OUTPUT-IDENTICAL originally: `bed-correction-event-types`
  and `update-beds` below are `engine.clj`'s own text, moved and not
  rewritten -- including one comment phrase that carries a stale `below`
  (`bed-correction-event-types`' \"the guard in `decide :bed-ready`
  below\", which pointed UP even where it stood, `decide :bed-ready`
  being 523 lines above it), moved verbatim rather than corrected inside
  a commit whose whole claim is that the moved text is unchanged.

  ONE VAR WAS PUBLIC in `engine.clj`, `replay`, and it keeps a
  delegating `(def replay fold/replay)` there under ruling C1(a) -- not
  a formality: `ehrt.sim-engine.interface` re-exports it at
  `interface.clj:89` (`(def replay engine/replay)`), and census
  constraint 4 requires that file to keep naming `engine/...`, so the
  delegating def is what keeps the brick's own public surface
  resolving.

  Four edges, all taken DIRECTLY into the namespace that owns them
  rather than back through `engine.clj`'s delegating defs:
  `evolve/evolve` and `state/initial-patient` (the two `replay` always
  had, now inside `apply-events`), and `encounters/stamp-encounter`,
  which arrived with the choke point -- site 1's decoration edge, which
  `run` used to hold. It reaches nothing else -- not `streams`, not
  `log-index`, not `sim-model`, not malli, not `clojure.*`."
  (:require [ehrt.sim-engine.encounters :as encounters]
            [ehrt.sim-engine.evolve :as evolve]
            [ehrt.sim-engine.state :as state]))

(def bed-correction-event-types
  "The two kinds that leave a bed empty by SAYING IT WAS NEVER FILLED --
  `:cancel-admit` (the admission did not happen) and `:cancel-transfer`
  (the transfer did not happen). Their bed goes straight back to
  `:ready`, with no `:bed-status-change` event and no turnaround: an
  occupancy a cancel retracts leaves no dirt behind it, and pretending
  otherwise would charge a correction the housekeeping cost of a real
  stay.

  ADR-0174's invariant 3 enumerates ready->occupied, occupied->dirty,
  dirty->cleaning, cleaning->ready and the reinstatement's
  dirty->occupied. A SEVENTH, cleaning->occupied, joined it on
  2026-08-29 (ADR-0174 section 2(c) ratification 4, traffic-scale close
  section 9 TS-1): the turnaround has TWO in-flight legs and a
  reinstating cancel can land in either. That arc changes nothing HERE
  -- neither of this set's two kinds produces it, and the guard in
  `decide :bed-ready` below already handles the bed it leaves -- it is
  named so a reader of this comment finds the whole relation.
  THE CORRECTION ARC, occupied->ready, IS A SIXTH, and
  the ADR does not name it -- it enumerated the cycle's own transitions
  and the two cancel classes that RE-OCCUPY, and did not reach the two
  that VACATE. Disclosed rather than smuggled: without it a cancelled
  admission's bed stays `:occupied` for the rest of the run and the
  ward silently loses capacity, which no reading of section 2(c)
  intends.

  `:transfer-in-error` is deliberately not a THIRD member. Its own
  decide emits an ordinary `:transfer` plus that transfer's
  `:cancel-transfer`, atomically at one instant, so the pair is already
  handled by the `:cancel-transfer` entry -- and the bed it came FROM
  is never dirtied at all, because `decide :transfer-in-error` does not
  call `vacate-bed`."
  #{:cancel-admit :cancel-transfer})

(defn update-beds
  "The bed index, folded one event forward (ADR-0174 section 2(c)).

  Two rules and no others:

  * a `:bed-status-change` writes its own `:to`, which is the whole of
    the cycle's three legs;
  * every other event is read through its participants' LOCATION delta
    -- a bed newly named becomes `:occupied`, and a bed newly left
    becomes `:ready` only under `bed-correction-event-types` above. A
    bed left by a real vacate is untouched HERE, because the
    `:bed-status-change` its own decide emitted in the SAME batch is
    what turns it `:dirty`.

  A `:bed-swap` needs no case of its own and gets none: each side's
  post-event bed is named by the other participant, so both come out
  `:occupied`, which is what they are."
  [beds ev patients-before patients-after]
  (if (= :bed-status-change (:event ev))
    (let [{:keys [bed to t last-patient-id]} ev]
      (update beds bed merge (cond-> {:status to :since-t t}
                               last-patient-id (assoc :last-patient-id last-patient-id))))
    (reduce (fn [bs {:keys [patient-id]}]
              (let [before (get-in patients-before [patient-id :location :bed])
                    after (get-in patients-after [patient-id :location :bed])]
                (cond-> bs
                  (and after (not= after before))
                  (update after merge {:status :occupied :since-t (:t ev) :last-patient-id patient-id})

                  (and before (not= after before) (bed-correction-event-types (:event ev)))
                  (update before merge {:status :ready :since-t (:t ev)}))))
            beds
            (filter :patient-id (:participants ev)))))

(def reinstatable-event-types
  "The event classes a cancel decide reinstates state FROM, and therefore
  the only ones `run`'s `:reinstate-index` records (ADR-0169).

  `:cancel-transfer` restores `:home-ward`/`:location`; `:cancel-discharge`
  restores those plus `:attending`. `:cancel-admit` is deliberately
  ABSENT: its own decide reads nothing but the live patient's
  `:active-mrn`, so it never queried the log for prior state and has
  nothing to carry. `:transfer-in-error` is absent for the opposite
  reason -- it emits its transfer and that transfer's cancel in ONE
  decide, off the live pre-transfer patient -- there is no intervening
  event for anything to have queried yet, its own comment -- so it too
  never replayed. Both were checked rather than assumed: the arc's scope
  named them as candidates.

  MOVED HERE from `ehrt.sim-engine.log-index` by the apply-unification
  pass's stage 1, with a delegating def left there under ruling C1(a).
  It is APPLY-SITE POLICY, not a log query: it names which events the
  `:reinstate-index` accumulator records, its only live code consumer is
  `apply-events` below, and `log-index` requires THIS namespace -- so
  leaving it there and naming it here would close a require cycle.
  `.agents/plans/apply-unification-census.md` section 4a carries the
  derivation and the two rejected alternatives."
  #{:transfer :discharge})

(def cited-opening-event-types
  "The two event classes whose LAST citation-matching occurrence a
  terminal step resolves against: a `:medication-end` resolves its
  `:order-citation` to a `:medication-order`, a `:care-plan-end` its
  `:care-plan-citation` to a `:care-plan-start`. ADR-0169's carrier
  records these and nothing else.

  MOVED HERE with `reinstatable-event-types` above, for the same reason
  and under the same ruling."
  #{:medication-order :care-plan-start})

;; --- The apply choke point (P5 stage 1, `.agents/plans/apply-
;; unification-census.md`). ONE fold, three projected call sites: `run`'s
;; in-loop fold (census site 1), `replay` below (site 2), and
;; `ehrt.sim-engine.log-index/reinstated-state`'s fallback (site 3).
;; Stage 1 changes NO behaviour: each site passes exactly the accumulator
;; stack it already ran, as an explicit declared subset of
;; `full-algebra`. Stage 2 enables omitted (site x accumulator) pairs one
;; commit each, against the census's own cone predictions.

(def full-algebra
  "The THIRTEEN concerns the three apply sites perform between them --
  the census's section 1 inventory, as the vocabulary a projection is a
  subset of. Their grains are not uniform and the census says so:
  `:encounter-stamp`/`:warm-up-mark` are DECORATIONS (a pre-pass over
  the batch, off the world as it stands BEFORE it);
  `:log-mirror`/`:log-accumulator`/`:state-history` are PER-BATCH (a
  post-pass off the world as it stands AFTER it -- which is why
  `:state-history` appends the post-BATCH state, not the post-event
  one, census correction C2); the remaining eight are per-event.

  `apply-events` reads this set for nothing: it is the closure the
  three projections below are subsets of, and the population
  `ehrt.sim-engine.apply-projection-test` checks them against."
  #{:encounter-stamp :warm-up-mark :log-ordinal :reinstate-index
    :citation-index :registration-index :patient-bootstrap
    :patient-state :bed-index :log-mirror :log-accumulator
    :state-history :replay-entries})

(def run-loop-projection
  "Census site 1 -- `ehrt.sim-engine.run`'s in-loop fold. THE FULL
  THIRTEEN, and the first of the three sites to reach the ruled end
  state. Stage 2 enabled its two omitted pairs in census order,
  `:patient-bootstrap` then `:replay-entries`, both section 3a and both
  predicted INERT.

  WHY IT IS INERT, and it is a property of `run` rather than of the
  concern: nothing reaches this fold unregistered. `prelude` seeds every
  patient with `state/initial-patient` before the loop runs, and `decide
  :registered` is every patient's first event, so the bootstrap branch
  finds every participant already in `(:patients w)` and returns `w`
  unchanged. IF IT EVER FIRES, that is worth more than the pair -- it
  means an unregistered participant reached the log.

  `:replay-entries` is inert here for a different reason -- not that its
  branch never fires, but that nothing READS what it accumulates.
  `final-result` merges `:ground-truth`, `:state-history`, `:facility`
  and `:providers` and nothing else, and no caller of
  `ehrt.sim-engine.interface/run` asks for more, so the entries land in
  a transient the call site never realises. It costs allocation -- one
  map per event carrying two whole patient-map snapshots -- and moves no
  byte."
  full-algebra)

(def replay-projection
  "Census site 2 -- `replay` below. THREE of the thirteen at stage 1.
  Stage 2 enables its eight INERT pairs one commit each in census order,
  and each bullet below names why that pair moved no output -- the cone
  the census's section 3b predicted, as the commit that took it found
  it:

  * `:log-ordinal` -- INERT BY VALUE, not merely unread. `base-idx` is
    `(count (:ground-truth world))` and replay's world is `{:patients
    {}}`, so the enabled branch computes `(count nil)` = 0, which is
    exactly the 0 the disabled branch substituted. The two arms are the
    same number. It is enabled because the three indexes below key on
    it, and for no other reason.

  * `:reinstate-index` -- accumulates CORRECTLY and is read by nobody.
    Its input is the pre-event subject state, which is exactly the
    `:before` replay already computes, so the index is right; it
    publishes into the returned world, and `replay` returns
    `(:entries ...)` and discards that world. Its value is that site 3's
    `nth` now has a first-class source here -- census 4d's own cheapest
    deletion, which stage 2 does not take.

  * `:citation-index` -- same shape: accumulates, nothing reads it.
    `log-index/last-cited-index` consults a `:citation-index` when the
    world carries one and falls back to a whole-log scan otherwise, but
    it is handed `run`'s world and never a replay-built one, so even
    that path does not reach this.

  * `:registration-index` -- same shape again. `ehrt.sim-check.check`'s
    own registration invariants walk the ENTRIES rather than asking for
    an index, so enabling this hands nothing to the consumer that would
    most plausibly have wanted it.

  * `:bed-index` -- INERT BY GUARD, and the guard is unreachable here:
    the concern fires only when `(:beds w-next)` is truthy, replay's
    world starts `{:patients {}}`, and nothing `evolve` does puts a
    `:beds` key on it. THE PAIR THE ARC WAS MIS-SOLD ON -- census
    correction C3: `ehrt.sim-check.check` deliberately does NOT call
    `update-beds`, on vacuous-gate grounds, so no consumer waits.

  * `:log-mirror` -- a pure duplicate: replay is HANDED the log, and
    this accumulates a second copy of it into a world it does not
    return. ONE OBSERVATION THE CENSUS DID NOT MAKE: `(into
    (:ground-truth world) events)` over a world carrying no
    `:ground-truth` is `(into nil events)`, which builds a REVERSED
    list, not a vector in log order. Unread, so inert -- but a consumer
    must seed the world with `:ground-truth []` before reading it.

  * `:log-accumulator` -- the same duplicate in TRANSIENT form, and
    the one pair at this site that needs a SLOT: the concern is
    `(reduce conj! (:log acc) events)` and `conj!` on nil throws, so
    `replay`'s acc now carries `:log (transient [])`. Never persisted,
    never read.

  THE TWO IT DOES NOT GET are the DECORATIONS `:encounter-stamp` and
  `:warm-up-mark`, the only two of section 3b predicted OUTPUT-MOVING --
  the concerns applied on the way IN, which a re-fold of an existing log
  RECOMPUTES rather than accumulates. That is the whole of section 4c's
  'replay cannot do them'. Stage 2 PREPARES them and does not land them;
  the author disposes."
  #{:log-ordinal :reinstate-index :citation-index :registration-index
    :patient-bootstrap :patient-state :bed-index :log-mirror
    :log-accumulator :replay-entries})

(def reinstated-projection
  "Census site 3 -- `ehrt.sim-engine.log-index/reinstated-state`'s
  replay fallback. THE SAME THREE CONCERNS as site 2 at this commit, and
  inherited rather than chosen -- but a LITERAL now, not the alias stage
  1 wrote.

  THIS IS THE STAGE-2 COMMIT CENSUS CORRECTION C5 SAID WOULD SAY SO, and
  it is FORCED rather than chosen. The alias made sites 2 and 3 one
  object, so no (site x accumulator) pair at either could be enabled
  without silently enabling its twin at the other -- and the ruled
  granularity is ONE PAIR PER COMMIT. It cannot be worked around by
  ordering, because the two columns must genuinely DIVERGE: 3 x
  `:warm-up-mark` is predicted INERT (census 3c -- `:warm-up` is a key on
  the EVENT, `evolve` never reads it, so it cannot reach the patient
  state site 3 reads) while its site-2 twin 2 x `:warm-up-mark` is
  predicted OUTPUT-MOVING and does not land at all.

  NOTHING IS ENABLED HERE: the set's VALUE is unchanged, only its
  identity, which is why this commit is output-identical."
  #{:patient-bootstrap :patient-state :replay-entries})

(defn apply-events
  "THE APPLY CHOKE POINT. `acc x events x projection -> acc'`.

  `projection` is a subset of `full-algebra`; every concern is guarded
  by its own membership test and by nothing else. `acc` is a map of the
  accumulator slots that projection needs, plus the parameters those
  concerns take:

  | slot | held for | shape |
  |---|---|---|
  | `:world` | always | the world map; `(:patients ...)` is what the per-event concerns read and write |
  | `:log` | `:log-accumulator` | the TRANSIENT log accumulator, in and out as a transient -- `run` persists it at `final-result`, never here |
  | `:state-history` | `:state-history` | `{patient-id [state ...]}` |
  | `:entries` | `:replay-entries` | the TRANSIENT entries accumulator |
  | `:warm-up-seconds` | `:warm-up-mark` | parameter, threaded unchanged |

  `:log-mirror` needs no slot of its own -- it publishes into
  `(:world acc')` under `:ground-truth`, which is where a mid-run
  `decide` reads the log back from. `:log-ordinal` needs none either:
  its base is derived from `(:world acc)` on entry.

  THE ORDER IS `run`'s, unchanged, and that is what makes stage 1
  output-identical by construction rather than by assertion: decorate
  the batch off the PRE-batch world; take the log ordinal off the
  PRE-batch world; one per-event reduce; then the per-batch post-pass
  off the POST-reduce world.

  TWO SUBJECT NOTIONS coexist here and are not interchangeable (census
  correction C4). `subject` is the FIRST participant's `:patient-id`,
  which the two index concerns key on and which is nil for a
  `:bed-status-change` (whose first participant names a bed) -- an event
  neither of those concerns can see. `subject-id` is the first
  participant that HAS a patient-id, which is what a replay entry's
  `:patient-id`/`:before`/`:after` mean. They coincide on every event
  whose first participant is a patient and diverge on every event whose
  first participant is not."
  [acc events projection]
  (let [{:keys [world warm-up-seconds]} acc
        ;; DECORATIONS. Off `world` as it stands BEFORE this batch, for
        ;; the same reason `:reinstate-index` is written inside the fold
        ;; below -- the pre-event state exists at this point and nowhere
        ;; later. Per event, not once for the batch: a `:discharge`
        ;; decide can emit a bed-ready `:transfer` for a DIFFERENT
        ;; patient, whose own open encounter is the one that transfer
        ;; belongs to.
        events (cond->> events
                 (projection :encounter-stamp)
                 (mapv (partial encounters/stamp-encounter world))

                 (projection :warm-up-mark)
                 (mapv (fn [ev] (assoc ev :warm-up (< (:t ev) warm-up-seconds)))))
        base-idx (if (projection :log-ordinal) (count (:ground-truth world)) 0)
        ;; ADR-0169: the patient-state fold and the reinstate index are
        ;; built in ONE pass, because the index's value IS this fold's
        ;; accumulator one step early -- `w` before `ev` is applied. A
        ;; `:discharge` decide can emit two events (the discharge, then a
        ;; bed-ready :transfer for a DIFFERENT patient), so the subject is
        ;; read off each event rather than assumed, and the state is
        ;; captured per event rather than once for the batch.
        ;;
        ;; `:patient-bootstrap` runs FIRST of the per-event concerns, so
        ;; the "pre-event world" the three indexes and a replay entry all
        ;; see is the BOOTSTRAPPED one. That is `replay`'s own prior
        ;; semantics (its `:before`/`:world-before` were the bootstrapped
        ;; map), and no site holds bootstrap and an index together at
        ;; stage 1, so the choice is inert today and stated for stage 2.
        [world' ridx' cidx' gidx' entries']
        (reduce (fn [[w ridx cidx gidx entries] [offset ev]]
                  (let [idx (+ base-idx offset)
                        subject (:patient-id (first (:participants ev)))
                        ;; ARC 3B SWEEP 2: the participant filter. A
                        ;; `:bed-status-change`'s participant names a BED,
                        ;; not a patient, and a nil-keyed phantom patient
                        ;; must not reach `ehrt.sim-check.check`.
                        participants (filter :patient-id (:participants ev))
                        w (if (projection :patient-bootstrap)
                            (reduce (fn [w2 {:keys [patient-id]}]
                                      (if (contains? (:patients w2) patient-id)
                                        w2
                                        (assoc-in w2 [:patients patient-id]
                                                  (state/initial-patient patient-id (:active-mrn ev)))))
                                    w participants)
                            w)
                        ridx' (if (and (projection :reinstate-index)
                                       (reinstatable-event-types (:event ev)))
                                (assoc ridx idx (get-in w [:patients subject]))
                                ridx)
                        cidx' (if (and (projection :citation-index)
                                       (cited-opening-event-types (:event ev))
                                       (some? (:citation ev)))
                                (reduce (fn [ci {:keys [patient-id]}]
                                          (assoc ci [(:event ev) patient-id (:citation ev)] idx))
                                        cidx (:participants ev))
                                cidx)
                        ;; ADR-0173 section 2(d): one more index off the
                        ;; SAME fold, for the same reason the two above
                        ;; are here -- the log index exists at this point
                        ;; and nowhere later.
                        gidx' (if (and (projection :registration-index)
                                       (= :registered (:event ev)))
                                (assoc gidx subject idx)
                                gidx)
                        ;; ARC 3B SWEEP 2: the bed index folded in the
                        ;; SAME pass for the same reason the three indexes
                        ;; above are -- the pre-event and post-event
                        ;; patient maps both exist here and nowhere later.
                        w-next (if (projection :patient-state)
                                 (reduce (fn [w2 {:keys [patient-id]}]
                                           (update-in w2 [:patients patient-id] evolve/evolve ev))
                                         w participants)
                                 w)
                        entries' (if (projection :replay-entries)
                                   (let [subject-id (:patient-id (first participants))]
                                     (conj! entries
                                            {:event ev :patient-id subject-id
                                             :before (get (:patients w) subject-id)
                                             :after (get (:patients w-next) subject-id)
                                             :world-before (:patients w)
                                             :world-after (:patients w-next)}))
                                   entries)]
                    [(cond-> w-next
                       (and (projection :bed-index) (:beds w-next))
                       (assoc :beds (update-beds (:beds w-next) ev
                                                 (:patients w) (:patients w-next))))
                     ridx' cidx' gidx' entries']))
                [world (:reinstate-index world) (:citation-index world)
                 (:registration-index world) (:entries acc)]
                (map-indexed vector events))
        world'' (cond-> world'
                  (projection :log-mirror)
                  (assoc :ground-truth (into (:ground-truth world) events))

                  (projection :reinstate-index) (assoc :reinstate-index ridx')
                  (projection :citation-index) (assoc :citation-index cidx')
                  (projection :registration-index) (assoc :registration-index gidx'))
        ;; ARC 3B SWEEP 2: same filter, same reason -- a bed participant
        ;; has no patient whose history to append to, and a nil key here
        ;; would put a phantom patient in `:state-history` for
        ;; `patient-state-is-a-fold-of-the-log` to trip over. Read off
        ;; `world'`, the POST-BATCH world: two events of one batch
        ;; touching one patient append that patient's FINAL state twice,
        ;; which is what this fold has always done (census correction C2).
        state-history' (if (projection :state-history)
                         (reduce (fn [sh ev]
                                   (reduce (fn [sh2 {:keys [patient-id]}]
                                             (update sh2 patient-id (fnil conj [])
                                                     (get-in world' [:patients patient-id])))
                                           sh (filter :patient-id (:participants ev))))
                                 (:state-history acc) events)
                         (:state-history acc))]
    (cond-> (assoc acc :world world'')
      (projection :log-accumulator) (assoc :log (reduce conj! (:log acc) events))
      (projection :state-history) (assoc :state-history state-history')
      (projection :replay-entries) (assoc :entries entries'))))

(defn replay
  "Replays `ground-truth` through `evolve`, returning a parallel seq of
  {:event :patient-id :before :after :world-before :world-after} --
  `:patient-id` is a convenience view of the event's PRIMARY (first)
  participant, since every check.clj invariant needs at most one
  patient's pre/post state even once M2b's bed-swap/merge span two
  (cross-participant invariants read world-before/world-after
  directly instead). Every participant in :participants folds via
  `evolve`, not just the primary one -- sim/ADR-0010: a patient's state
  folds exactly the events they participate in. `world-before`/
  `world-after` are the full {patient-id -> patient-state} map
  immediately before/after this event (sim/ADR-0008: state-history is
  derived -- this IS that derivation, generalized across patients).

  APPLY SITE 2, and since stage 1 of the unification pass it is a
  PROJECTION of `apply-events` above rather than a fold of its own:
  three of the thirteen concerns, named by `replay-projection`. Nothing
  it folds was added, removed or reordered -- what was a hand-written
  loop is the same fold under the choke point's own guards."
  [ground-truth]
  (persistent!
   (:entries (apply-events {:world {:patients {}}
                            :entries (transient [])
                            ;; `:log-accumulator`'s slot, since stage 2
                            ;; enabled that pair at this site. Never
                            ;; persisted and never read -- `conj!` on
                            ;; nil would throw, which is the whole of
                            ;; why the slot exists.
                            :log (transient [])}
                           ground-truth
                           replay-projection))))
