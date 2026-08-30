(ns ehrt.sim-engine.log-index
  "Queries over the ground-truth log: the whole-log scans a `decide`
  reaches for when it needs to know what ALREADY HAPPENED, and the
  reinstatement machinery the two reinstating cancels are built on --
  `engine.clj`'s sixth extraction under
  `roadmap.md#engine-namespace-extraction-and-apply-unification` (the
  census's own dependency order, `.agents/plans/engine-extraction-
  census.md` section 3a: `log-index` lands after `streams`, `state`,
  `encounters`, `evolve` and `fold`, and before `decide`).

  THE MOST SCATTERED CLUSTER OF THE SIX. Its ten forms sat in FOUR
  non-contiguous regions of `engine.clj` -- `events-for-patient` alone
  above `defmulti decide`; the M2b churn pair; the ADR-0164 citation
  pair; and the five-form reinstatement block -- and they are gathered
  here in that same source order, so a reader who knows the old file can
  still walk it. Every form below is `engine.clj`'s own text, moved and
  not rewritten.

  THIS NAMESPACE OWNS AN APPLY SITE. `reinstated-state`'s fallback,
  `(:before (nth (fold/replay ground-truth) idx))`, is the census's
  apply site 3 (section 4d) -- the one a unification pass can most
  cheaply delete. It is moved VERBATIM: the `(contains? world
  :reinstate-index)` guard and the index read are the same two
  expressions in the same order, and nothing it folds is added, removed
  or reordered. The ruled unification pays its divergence from the other
  two apply sites later, not here.

  ONE VAR WAS PUBLIC in `engine.clj`, `events-for-patient`, and it keeps
  a delegating `(def events-for-patient log-index/events-for-patient)`
  there under ruling C1(a). Unlike the `evolve` and `fold` extractions'
  defs, this one is NOT owed to `ehrt.sim-engine.interface`: not one of
  the ten movers is on that seam's re-export list, and census constraint
  4 names none of them. What makes it load-bearing is
  `components/sim-engine/test/ehrt/sim_engine/engine_test.clj`, which
  calls `engine/events-for-patient` at TEN sites and which C1(a) forbids
  this session to touch.

  The other nine were `defn-` or `^:private` -- NINE, where the census's
  section-1 rendering shows five, because that rendering drops
  `^:private` from every `def` -- so under constraint 5 they become
  public HERE and get no def THERE. `engine.clj`'s thirteen remaining
  call sites are `log-index/`-qualified instead, which is the treatment
  the `encounters` and `fold` moves gave their own.

  Two edges, both taken DIRECTLY into the namespace that owns them
  rather than back through `engine.clj`'s delegating defs:
  `fold/replay` inside `reinstated-state`, and
  `sim-model/occupancy-board` inside `bed-reoccupied-by-someone-else?`.
  `fold/replay` IS the value `engine.clj`'s own `replay` def holds, so
  this is the same function reached one hop shorter, not a different
  one. Nothing else in the moved text resolved in `engine.clj` at all:
  `evolve`, `state` and `streams` are absent from this cluster
  entirely.

  COVERAGE, disclosed rather than implied: the oracle's 41 roots reach
  no cancel decide, and the gated corpora resolve zero citations, so a
  regression bracket reporting IDENTICAL across this move proves
  nothing about six of these ten forms. What covers them is the suite --
  `engine_test.clj`'s cancel family driving `decide` against hand-built
  worlds, plus `ehrt.sim.run-test/cancel-decides-reinstate-exactly-what-
  replay-would-hand-back` and `citation-resolution-matches-the-whole-
  log-scan`, the two post-hoc equivalence proofs ADR-0169 left
  standing."
  (:require [ehrt.sim-engine.fold :as fold]
            [ehrt.sim-model.interface :as sim-model]))

(defn events-for-patient
  "Every event `patient-id` participates in, in log order -- the
  patient-phrased replacement for what a single :mrn-keyed lookup used
  to mean before sim/ADR-0010's :participants existed. An event with more
  than one participant (M2b's bed-swap, merge) appears in every
  participant's own sequence, not just one."
  [ground-truth patient-id]
  (filterv (fn [event] (some #(= patient-id (:patient-id %)) (:participants event)))
           ground-truth))

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
  named them as candidates."
  #{:transfer :discharge})

(defn last-uncancelled-index
  "Index into `ground-truth` of the most recent `event-type` event
  naming `patient-id` that is NOT already the target of an earlier
  `cancel-type` event -- the applicability query the event-validity
  table's cancel-* row asks ('the event class being cancelled must
  exist in this patient's log and not already be cancelled'). nil when
  no such event exists, which decide turns into a structured rejection
  rather than a throw."
  [ground-truth patient-id event-type cancel-type]
  (let [already-cancelled (into #{}
                                (comp (filter #(= cancel-type (:event %)))
                                      (map :cancels-event-id))
                                ground-truth)]
    (last (keep-indexed (fn [i ev]
                          (when (and (= event-type (:event ev))
                                     (some #(= patient-id (:patient-id %)) (:participants ev))
                                     (not (already-cancelled i)))
                            i))
                        ground-truth))))

(def cited-opening-event-types
  "The two event classes whose LAST citation-matching occurrence a
  terminal step resolves against: a `:medication-end` resolves its
  `:order-citation` to a `:medication-order`, a `:care-plan-end` its
  `:care-plan-citation` to a `:care-plan-start`. ADR-0169's carrier
  records these and nothing else."
  #{:medication-order :care-plan-start})

(defn last-cited-index
  "Index into the log of the LAST `opening-type` event carrying
  `citation` and naming `patient-id` as a participant -- nil when there
  is none, and nil when `citation` itself is nil.

  Exactly what ADR-0164's two `keep-indexed` scans computed, and
  therefore exactly what `:medication-end`'s `:order-event-id` and
  `:care-plan-end`'s `:start-event-id` still are. ADR-0169 (arc 0)
  replaces the scan with a lookup: the two were 21.3% and 10.9% of the
  generate phase at 10^5 events, 32.2% combined, and each walked the
  WHOLE log once per terminal step. ADR-0164 scoped them by patient --
  it added the participant predicate INSIDE the same full-length
  `keep-indexed` -- which made them correct without making them shorter;
  this is the shortening, and it changes no answer.

  `run` carries `{[opening-type patient-id citation] last-index}`,
  written as events are appended, so a later occurrence simply
  overwrites an earlier one and the stored value IS `last`'s answer.
  Only events with a NON-NIL `:citation` are recorded: a nil citation
  could never be returned anyway, since both call sites are already
  guarded by `(when <citation> ...)`.

  FALLS BACK to the scan it replaces when `world` carries no
  `:citation-index` KEY -- a hand-built world, as most of engine-test
  uses. Same fallback rule as `reinstated-state`, and for the same
  reason: on the key, never on a missing entry, so a carrier that
  `run` built but failed to populate shows up as a changed corpus rather
  than as a silent replay.

  Proven post hoc against the scan itself by
  `ehrt.sim.run-test/citation-resolution-matches-the-whole-log-scan` on
  every gated corpus, seed 424242 (ADR-0163's own run) included."
  [world ground-truth opening-type patient-id citation]
  (when citation
    (if (contains? world :citation-index)
      (get (:citation-index world) [opening-type patient-id citation])
      (last (keep-indexed (fn [i ev] (when (and (= opening-type (:event ev))
                                                (= citation (:citation ev))
                                                (some #(= patient-id (:patient-id %))
                                                      (:participants ev)))
                                       i))
                          ground-truth)))))

;; --- M2b cancel-transfer/cancel-discharge: the reinstatement machinery
;; the two reinstating cancels are built on (docs/patient-state-
;; model.md's shadow-field dissolution: the reinstated prior state is
;; QUERIED FROM THE LOG at decide-time, never a field the accumulator
;; carries for this purpose alone).

(defn bed-reoccupied-by-someone-else?
  "Whether `location`'s bed is CURRENTLY held by a patient other than
  `patient-id` -- the reinstatement guard cancel-transfer/cancel-
  discharge both need: the log-derived prior location was free WHEN it
  was vacated, but time has passed since, and another patient's own
  allocation (a later admission, a bed-ready transfer) may have
  legitimately claimed it in the meantime. Reinstating into an
  occupied bed would violate no-double-occupancy, so this is checked
  against the LIVE occupancy board (world, not the log) at decide-time
  -- the same board :admission/:transfer already consult."
  [world patient-id location]
  (when-let [bed (:bed location)]
    (let [occupant (get (sim-model/occupancy-board (:patients world)) bed)]
      (and (some? occupant) (not= occupant patient-id)))))

(def status-a-cancel-target-leaves
  "The `:status` the event a reinstating cancel targets leaves its
  SUBJECT in -- the one status a legal cancel of that class can still
  find on the patient when it finally runs.

  A `:transfer` moves an `:admitted` patient and leaves them
  `:admitted`; a `:discharge` leaves them `:discharged`, which is
  precisely the state a `:cancel-discharge` exists to undo. Written as
  a two-entry table rather than a status test per method because the
  ASYMMETRY is the whole point of `subject-superseded?` below: the same
  `:discharged` that makes a cancel-transfer illegal is what makes a
  cancel-discharge legal, and a guard that missed that would reject
  every cancel-discharge in the repository."
  {:cancel-transfer :admitted
   :cancel-discharge :discharged})

(def statuses-that-supersede-a-reinstatement
  "The three statuses that say the subject is no longer a patient the
  hospital is holding: `:discharged` (they left), `:expired` (they
  died), `:merged` (their record was absorbed into another's).

  `:new` is deliberately ABSENT. It is what `evolve :cancel-admit`
  writes, and a cancel-admit is a correction of the record rather than
  an event in the patient's life -- `ehrt.sim-engine.engine-test/
  cancel-discharge-restores-class-even-after-a-preceding-cancel-admit-
  stripped-it` is the M6 Task 2 finding that says so, and it stays
  legal here. MEASURED, so the exclusion is a decision and not an
  oversight: the `nobed` 10^5 cell at seed 20260824 carries 2
  cancel-transfers against a `:new` subject alongside its 61 against a
  `:discharged` one, and this guard leaves those 2 alone -- named in
  `.agents/session-records/2026-08-29-ts-5-superseded-cancel.md` as an
  adjacent case this change deliberately does not reach.

  `:merged` is unreachable through `run`, which ends a merged patient's
  queue before their next step is ever decided (the `:merged` branch at
  the top of the run loop). It is named anyway, for a `decide` driven
  by hand and for the reader who would otherwise have to prove the
  omission safe."
  #{:discharged :expired :merged})

(defn subject-superseded?
  "Whether the state a cancel of `kind` would reinstate onto `patient`
  has already been superseded by a LATER event in that patient's own
  life -- the TS-5 guard (traffic-scale defect 5, 2026-08-29).

  A reinstating cancel restores the state its target event displaced:
  correct as an undo of THAT event, and read from the log at the
  target's own index, which predates everything after it. Nothing
  asked what had happened to the subject SINCE. So a churn
  `:cancel-transfer` landing after the patient's discharge -- in the
  same batch, at the same `t`, as the witness below -- put
  `:location`/`:home-ward` back onto a `:discharged` patient, and
  nothing ever vacated that bed again.

  ONE MEASUREMENT decides which mechanism this is, and it was taken
  before this guard was written: the subject's `:status` in `world` at
  the instant `decide` runs. At the row's own witness (`PID-004302-
  fa1ab125`, `nobed` 10^5, seed 20260824) it reads `:discharged`, with
  `:location` already nil -- the discharge IS in the world when the
  cancel is decided. So this is not a batch-ordering problem that no
  decide-time test could see; it is a reinstatement applied without
  asking the subject's current status, and a decide-time test is
  exactly what catches it.

  `bed-reoccupied-by-someone-else?` cannot stand in for this and never
  could: at the same witness the board reads NIL for SURGERY-91. The
  patient is gone, not displaced, so the bed they would be reinstated
  into is genuinely empty -- which is the whole of why the double-
  occupancy guard passes a reinstatement no one should make."
  [patient kind]
  (let [status (:status patient)]
    (and (statuses-that-supersede-a-reinstatement status)
         (not= status (status-a-cancel-target-leaves kind)))))

(defn reinstated-state
  "The state patient `patient-id` was in immediately BEFORE the log event
  at `idx` -- the prior location/home-ward/attending a reinstating cancel
  restores. Exactly `(:before (nth (replay ground-truth) idx))`, and
  proven so post hoc, twice: `ehrt.sim.run-test/cancel-decides-reinstate-
  exactly-what-replay-would-hand-back` recomputes it against `replay`
  itself on every gated corpus, and `ehrt.sim-engine.engine-test/cancel-
  reinstatement-survives-the-fold-carried-index` does the same over
  churn-driven generated runs -- which it must, because only ONE of the
  four gated corpora carries a reinstating cancel at all.

  ADR-0169 (arc 0), the largest single generator-side cost the 2026-08-24
  throughput spike measured -- 35.3% of the generate phase at 10^5
  events, larger than both ADR-0164 citation scans combined. Both cancel
  decides used to evaluate `(nth (replay ground-truth) idx)` literally:
  a full `evolve` re-simulation of the ENTIRE log, materialising a vector
  of N maps carrying `:world-before`/`:world-after`, in order to read ONE
  element at an index the caller already held, and then discard the rest.
  Once per cancel event, so O(N) with allocation per cancel and quadratic
  in churn density.

  The run loop already computes that state: it is the patient's entry in
  `world` at the instant the event was appended, and `world`'s
  `:patients` is folded through the SAME `evolve` over the SAME events in
  the SAME order that `replay` folds. So `run` now records it, for
  `:transfer` and `:discharge` events only (the two reinstatable classes
  -- `:cancel-admit` reads no prior state at all, and
  `:transfer-in-error` decides its own cancel atomically off the live
  patient, neither of them touching the log), under the log index of the
  event itself. The read is a map lookup.

  FALLS BACK to the replay it replaces when `world` carries no
  `:reinstate-index` KEY -- a world built by hand rather than by `run`,
  which is how most of engine-test drives `decide` directly. The fallback
  is on the key's presence, never on a missing entry: a world that `run`
  built and an entry that is nevertheless absent is a DEFECT, and letting
  it read nil (which changes the emitted event, which the byte-identity
  gate then fails) is the behaviour that surfaces it. Silently replaying
  instead would hide it."
  [world ground-truth patient-id idx]
  (if (contains? world :reinstate-index)
    (get (:reinstate-index world) idx)
    (:before (nth (fold/replay ground-truth) idx))))
