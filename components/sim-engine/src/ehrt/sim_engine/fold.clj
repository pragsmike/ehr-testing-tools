(ns ehrt.sim-engine.fold
  "The derived-state fold: `replay`, the bed index it maintains for
  `run` but does NOT maintain for itself, and that index's correction
  table -- `engine.clj`'s fifth extraction under
  `roadmap.md#engine-namespace-extraction-and-apply-unification` (the
  census's own dependency order, `.agents/plans/engine-extraction-
  census.md` section 3a: `fold` lands after `streams`, `state`,
  `encounters` and `evolve`, and before `log-index`, whose
  `reinstated-state` calls `replay`).

  THIS NAMESPACE OWNS AN APPLY SITE. `replay` is the census's apply
  site 2 (section 4c), and its divergence from `run`'s in-loop fold
  (section 4b) is six concerns wide: no encounter stamp, no warm-up
  mark, no bed index, and none of the three log indexes. That
  divergence is DOCUMENTED and RULED to be paid at application-path
  unification, not at this move. Nothing `replay` folds is added,
  removed or reordered here.

  Extracted OUTPUT-IDENTICAL: every form below is `engine.clj`'s own
  text, moved and not rewritten -- including one comment phrase that
  carries a stale `below` (`bed-correction-event-types`' \"the guard in
  `decide :bed-ready` below\", which pointed UP even where it stood,
  `decide :bed-ready` being 523 lines above it), moved verbatim rather
  than corrected inside a commit whose whole claim is that the moved
  text is unchanged. Unlike the four extractions before it this cluster
  has NO interior comment blocks at all: three forms, contiguous,
  nothing between them but blank lines.

  ONE VAR WAS PUBLIC in `engine.clj`, `replay`, and it keeps a
  delegating `(def replay fold/replay)` there under ruling C1(a) -- not
  a formality: `ehrt.sim-engine.interface` re-exports it at
  `interface.clj:89` (`(def replay engine/replay)`), and census
  constraint 4 requires that file to keep naming `engine/...`, so the
  delegating def is what keeps the brick's own public surface
  resolving. `engine.clj`'s `reinstated-state` called `replay` through
  it until the SIXTH extraction moved that form to
  `ehrt.sim-engine.log-index`, whose fallback now names `replay` here
  directly.

  The other two were `^:private` and `defn-`, so under constraint 5
  they become public HERE and get no def THERE. `run`'s one call site
  is `fold/update-beds`-qualified instead, which is the treatment the
  encounters move gave its own ten.

  Two edges, both taken DIRECTLY into the namespace that owns them
  rather than back through `engine.clj`'s delegating defs:
  `evolve/evolve` and `state/initial-patient`, both inside `replay`. It
  reaches nothing else -- not `streams`, not `encounters`, not
  `sim-model`, not malli, not `clojure.*`."
  (:require [ehrt.sim-engine.evolve :as evolve]
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
  derived -- this IS that derivation, generalized across patients)."
  [ground-truth]
  (loop [events ground-truth patients {} acc (transient [])]
    (if (empty? events)
      (persistent! acc)
      (let [event (first events)
            ;; ARC 3B SWEEP 2: a `:bed-status-change`'s participant names
            ;; a BED, not a patient. Filtering on `:patient-id` being
            ;; present is what keeps a nil-keyed phantom patient out of
            ;; every `world-before`/`world-after` this function hands to
            ;; `ehrt.sim-check.check`.
            participant-ids (mapv :patient-id (filter :patient-id (:participants event)))
            patients (reduce (fn [ps pid]
                                (if (contains? ps pid)
                                  ps
                                  (assoc ps pid (state/initial-patient pid (:active-mrn event)))))
                              patients participant-ids)
            patients' (reduce (fn [ps pid] (update ps pid evolve/evolve event)) patients participant-ids)
            subject-id (first participant-ids)]
        (recur (rest events) patients'
               (conj! acc {:event event :patient-id subject-id
                           :before (get patients subject-id) :after (get patients' subject-id)
                           :world-before patients :world-after patients'}))))))
