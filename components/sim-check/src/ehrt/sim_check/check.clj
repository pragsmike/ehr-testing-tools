(ns ehrt.sim-check.check
  "The invariant catalog: internal-consistency claims about a
  ground-truth log, machine-checkable (validation program, claim #3 in
  docs/problem-statement.md). Runs standalone as `sim check` and in CI
  as the regression suite; the property tests in test/ drive the same
  functions over generated runs, so the catalog does double duty.

  Each invariant is a named function (ground-truth) -> seq of violation
  maps (empty = holds). `check-all` aggregates to a Result: :ok when
  every invariant holds, :rejected (:category :invariant-violation)
  otherwise -- the check ran and the answer is no, which is exactly
  what the result-not-throw doctrine's :rejected arm is for.

  v0 catalog is minimal; every new step type added to the engine MUST
  land with its invariants here in the same change.

  Milestone M1 (docs/operational-models.md) adds the facility-aware
  invariants (no double occupancy, one physical slot per admitted
  patient, capacity, surge-only-when-earlier-rungs-exhausted) plus the
  event-validity rows from docs/patient-state-model.md (admission only
  when :new, transfer only when :admitted, a transfer's declared
  :from matches the fold). These read patient/world state via
  ehrt.sim-engine.fold/replay -- the same fold `evolve` always was,
  reused rather than reimplemented (sim/ADR-0008).

  M2a (sim/ADR-0010) adds two structural invariants over :participants
  (every event has >=1, every participant id traces to an :admission
  in the same log) and moves every per-patient grouping from
  `:mrn` to `:participants`-derived patient-ids -- an event with more
  than one participant (M2b's bed-swap, merge) belongs to every
  participant's own sequence, not just one. M2a (sim/ADR-0011) adds the
  warm-up-mark invariant (config/check-warm-up.clj docstring companion
  below)."
  (:require [clojure.set]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-engine.interface :as order-profiles]))

(defn- events-by-patient
  "Every event each patient-id participates in, in log order -- the
  general, patient-phrased replacement for `(group-by :mrn ...)`. An
  event with multiple participants (M2b) appears in every participant's
  own sequence; today's event types are all single-participant, so this
  is presently equivalent to grouping by the sole participant, but is
  written the general way so M2b needs no rewrite here.

  ARC 3B SWEEP 2 (ADR-0174 section 2(c)): FILTER 1 OF 3. A
  `:bed-status-change`'s participant names a BED, not a patient, so
  `:patient-id` is absent from it -- and an unfiltered `update` here
  would open a nil-keyed bucket holding every bed event in the run,
  which every row below would then judge as if it were somebody's
  clinical log."
  [ground-truth]
  (reduce (fn [acc event]
            (reduce (fn [acc2 {:keys [patient-id]}]
                      (update acc2 patient-id (fnil conj []) event))
                    acc (filter :patient-id (:participants event))))
          {} ground-truth))

;; --- ADR-0174 section 2(a) (arc 3b sweep 1): the encounter, judged -----
;;
;; Every invariant below reads the encounter through these three, so
;; there is ONE reading of "which encounter is this event's" and not
;; one per row.

(def ^:private encounter-openers
  "The two event kinds that OPEN an encounter -- and therefore mint its
  id (`ehrt.sim-engine.streams/encounter-id-for`)."
  #{:admission :outpatient-visit})

(def ^:private encounter-closers
  "The two event kinds that CLOSE one. `:cancel-admit` is deliberately
  absent: it un-does an admission rather than ending a stay, and the
  encounter it cancels is marked, never closed."
  #{:discharge :outpatient-visit-end})

(defn- encounter-id-of
  "The encounter-id an event carries FOR one patient: its top-level
  `:encounter-id`, or -- for a `:bed-swap`, the one kind that names two
  encounters at once -- that patient's own side of the `:swap`
  (`ehrt.sim-engine.event-schema/BedSwapSide`). nil on every event of
  every run that did not opt into `:encounters`, which is what makes
  each row below fall back to the patient-scoped rule it always was.

  THE TOP-LEVEL FIELD BELONGS TO THE EVENT'S FIRST PARTICIPANT AND TO
  NOBODY ELSE, and that is not a nicety -- it is what a population-scale
  probe of `demos/scenarios/ed-tuesday` at seed 202 found on the day
  this row landed. `run`'s stamp reads the first participant's own open
  encounter (`ehrt.sim-engine.encounters`'s `stamp-encounter`), while
  `events-by-patient` above puts a multi-participant event in EVERY
  participant's sequence -- so a `:merge` stamped with the SURVIVOR's
  encounter also appears in the MERGED patient's own log, where that id
  has no opener and never could. Two of them fired, correctly, against
  an invariant that was reading the field as if it named every
  participant at once."
  [event patient-id]
  (or (when (= patient-id (:patient-id (first (:participants event))))
        (:encounter-id event))
      (get-in event [:swap patient-id :encounter-id])))

(defn- carried-encounter-is-not-the-open-one?
  "Whether an event names an encounter that was NOT the subject's open
  one immediately before it -- the per-encounter half of the three
  validity rows ADR-0174's table moves per-encounter. An event carrying
  no id at all is not a violation: that is a legacy log, and the
  status half of each row is what judges it there."
  [event before patient-id]
  (when-let [id (encounter-id-of event patient-id)]
    (not= id (:encounter-id (:encounter before)))))

(defn timestamps-monotone
  "Within a patient, event times never decrease (log order is emission
  order, which the engine guarantees is time order)."
  [ground-truth]
  (for [[patient-id events] (events-by-patient ground-truth)
        [a b] (partition 2 1 events)
        :when (> (:t a) (:t b))]
    {:invariant :timestamps-monotone :patient-id patient-id :at [(:t a) (:t b)]}))

(defn discharge-follows-admission
  "No patient is discharged without a prior admission, and not twice."
  [ground-truth]
  (for [[patient-id events] (events-by-patient ground-truth)
        :let [kinds (mapv :event events)
              first-admit (.indexOf ^java.util.List kinds :admission)
              discharges (keep-indexed #(when (= :discharge %2) %1) kinds)]
        d discharges
        :when (or (neg? first-admit) (< d first-admit))]
    {:invariant :discharge-follows-admission :patient-id patient-id :at d}))

;; --- sim/ADR-0010: structural participant invariants -------------------------

(defn every-event-has-participants
  "Every event names at least one participant (sim/ADR-0010) -- a bug in a
  decide implementation or a future churn-injection step could
  otherwise emit an orphan event no patient's fold ever sees."
  [ground-truth]
  (for [event ground-truth
        :when (empty? (:participants event))]
    {:invariant :every-event-has-participants :event (:event event) :at (:t event)}))

(defn participant-ids-exist-in-run
  "Every patient-id named in any event's :participants is a patient-id
  this run actually created -- i.e. appears as a participant on at
  least one :registered event somewhere in the log. :registered is the
  ONE event type EVERY real patient this run creates always gets (M4),
  unconditionally -- a stricter, more universal proof than requiring an
  :admission/:outpatient-visit, which (M5b) a module-assigned patient
  can legitimately never get at all if their own disease process never
  produces an operational encounter inside this run's own configured
  horizon window. Catches a churn-injection or decide bug that names a
  stray or mistyped patient-id.

  ARC 3B SWEEP 2 (ADR-0174 section 2(c)): FILTER 2 OF 3, and this is the
  one the ADR names by hand. Scoped to participants that CARRY a
  `:patient-id`; the invariant keeps asserting exactly what it asserts
  today about every patient participant, and a bed participant -- which
  has no id to trace -- is simply not one of them. Without the filter a
  `{:bed-id .. :ward ..}` map yields `patient-id` nil, `(contains?
  admitted-ids nil)` is false, and every bed event in the run goes RED:
  the exact wall ADR-0174 predicted and the reason the participant
  vocabulary widened in one place instead of a second event stream
  being invented."
  [ground-truth]
  (let [admitted-ids (into #{}
                           (comp (filter #(= :registered (:event %)))
                                 (mapcat :participants)
                                 (map :patient-id))
                           ground-truth)]
    (for [event ground-truth
          {:keys [patient-id]} (filter :patient-id (:participants event))
          :when (not (contains? admitted-ids patient-id))]
      {:invariant :participant-ids-exist-in-run :patient-id patient-id :at (:t event)})))

;; --- sim/ADR-0011: the warm-up mark -------------------------------------------

(defn warm-up-mark-matches-window
  "The warm-up mark is exactly `t < warm-up-seconds` (sim/ADR-0011) -- a
  pure predicate over each event's own :t and the run's configured
  warm-up window, checkable without replay."
  [ground-truth warm-up-seconds]
  (for [event ground-truth
        :when (not= (boolean (:warm-up event)) (< (:t event) warm-up-seconds))]
    {:invariant :warm-up-mark-matches-window :at (:t event)}))

;; --- M1 event-validity rows (docs/patient-state-model.md) ---------------

(defn admission-only-when-no-open-encounter
  "docs/patient-state-model.md's event-validity table, RE-READ PER
  ENCOUNTER (ADR-0174 section 2(a) item 3, arc 3b sweep 1). Was
  `admission-only-when-new` -- `(not= :new (:status before))` -- which
  was this project's SINGLE-ENCOUNTER HORIZON (sim/ADR-0007 point 3)
  expressed as an invariant: `evolve :discharge` never returned a
  patient to `:new`, so a patient got one encounter, ever.

  An encounter opener is now legal iff NO encounter is open and the
  patient is not in one of the two absorbing terminals. What that buys
  is a second visit by the same patient, with the same MRN, which is the
  whole point of an MPI under test; what it deliberately does NOT buy is
  anything after `:merged` or `:expired` -- both stay absorbing, which
  is `no-events-after-merged-terminal` and
  `expired-patient-retains-location` preserved verbatim.

  IT ABSORBS `outpatient-visit-only-when-new`, which was the same rule's
  second copy. The two were always one rule written twice, and folding
  them is why the catalog is one shorter here and two longer overall.

  NOT VACUOUS ON A LEGACY LOG. `engine/evolve` folds the encounter
  records whether or not a run opted into `:encounters`, so
  `(:encounter before)` is a real predicate on a corpus generated before
  this sweep existed: an opener while an encounter is open fires here
  exactly as it did before."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (encounter-openers (:event event))
                   (or (some? (:encounter before))
                       (#{:merged :expired} (:status before))))]
    {:invariant :admission-only-when-no-open-encounter
     :event (:event event) :patient-id patient-id :at (:t event)}))

(defn discharge-closes-an-open-encounter
  "The PER-ENCOUNTER half of the split ADR-0174's table makes of
  `discharge-follows-admission` (which keeps the per-patient half above,
  unchanged): a closer closes an encounter that is OPEN.

  This is where *\"and not twice\"* actually lands. That phrase has been
  in `discharge-follows-admission`'s own docstring since v0 and was
  never in its code -- that function tests only that no discharge
  precedes the patient's FIRST admission -- so the claim it makes has
  been unenforced for its whole life. Measured, not assumed: the
  function's body is four lines and none of them counts anything."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (encounter-closers (:event event)) (nil? (:encounter before)))]
    {:invariant :discharge-closes-an-open-encounter
     :event (:event event) :patient-id patient-id :at (:t event)}))

(defn every-encounter-is-opened-and-closed-or-still-open
  "Every `:encounter-id` in the log is a real encounter of the patient
  carrying it: it appears on EXACTLY ONE opener, that opener is the
  first event of that patient's own log to carry it, and it is closed at
  most once more often than it is REINSTATED.

  The same referential shape as
  `medication-end-references-existing-order-and-follows-it-in-time`, and
  the reinstatement clause is the same accommodation the churn family
  already needs everywhere else: a `:cancel-discharge` un-does a close,
  so an encounter discharged, reinstated and discharged again is ONE
  encounter closed twice, not two encounters. Without that clause this
  row would go red on every reinstating cancel in every corpus.

  VACUOUS BY DESIGN on a log with no ids -- there is nothing to resolve
  in a corpus that did not opt into `:encounters`, and saying so here is
  better than a predicate that pretends otherwise. What is NOT vacuous
  there is every other row above, which reads the folded record rather
  than the id."
  [ground-truth]
  (apply
   concat
   (for [[patient-id events] (events-by-patient ground-truth)]
     (let [rows (vec (map-indexed (fn [i ev] [i ev (encounter-id-of ev patient-id)]) events))
           by-id (group-by (fn [[_ _ id]] id) (filter (fn [[_ _ id]] (some? id)) rows))]
       (for [[id id-rows] by-id
             :let [kind-of (fn [[_ ev _]] (:event ev))
                   openers (filterv (comp encounter-openers kind-of) id-rows)
                   closers (filterv (comp encounter-closers kind-of) id-rows)
                   reinstatements (filterv #(= :cancel-discharge (kind-of %)) id-rows)
                   reason (cond
                            (not= 1 (count openers)) :not-exactly-one-opener
                            (not= (ffirst id-rows) (ffirst openers)) :event-precedes-its-opener
                            (> (count closers) (inc (count reinstatements))) :closed-more-than-once
                            :else nil)]
             :when reason]
         {:invariant :every-encounter-is-opened-and-closed-or-still-open
          :patient-id patient-id :encounter-id id :reason reason
          :at (:t (second (first id-rows)))})))))

(defn transfer-only-when-admitted
  "docs/patient-state-model.md's event-validity table: :transfer
  (including bed-ready) is legal only when the patient's prior state
  is :admitted (Admitted or Boarding)."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (= :transfer (:event event))
                   (or (not= :admitted (:status before))
                       ;; ADR-0174's table, per-encounter: and the
                       ;; transfer's own `:encounter-id` is the OPEN one,
                       ;; so a transfer cannot be attributed to a visit
                       ;; that had already ended.
                       (carried-encounter-is-not-the-open-one? event before patient-id)))]
    {:invariant :transfer-only-when-admitted :patient-id patient-id :at (:t event)}))

(defn transfer-from-matches-state
  "A transfer event's declared :from matches the patient's actual
  location immediately beforehand (docs/operational-models.md)."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (= :transfer (:event event)) (not= (:from event) (:location before)))]
    {:invariant :transfer-from-matches-state :patient-id patient-id :at (:t event)}))

;; --- M1 facility invariants (docs/operational-models.md) ----------------

;; --- ADR-0169 (arc 0): the occupancy family, fold-carried --------------
;;
;; The 2026-08-24 throughput spike measured the four invariants below at
;; 92.1% of the whole check phase at 10^5 events, because each walked
;; `(vals world-after)` -- the ENTIRE patient population -- once per
;; event: O(N x P), and O(N x P x W) for `occupancy-within-capacity`,
;; whose ward loop made it 54.9% of the phase on its own.
;;
;; The fix carries the answer instead of recomputing it. `engine/replay`
;; already hands each record its own `world-before`/`world-after`, and
;; the DELTA between them is exactly this event's participants and
;; nobody else (`replay`'s own fold: `patients'` is `patients` with each
;; participant `evolve`d). So a fold that updates a per-bed / per-ward /
;; per-violator index from the participants alone is O(participants) per
;; event where the walk was O(P) -- and every index below is
;; SET-VALUED and updated by `disj` then `conj`, never by increment, so
;; it is self-correcting: a patient's first appearance (whose
;; `world-before` entry is a bare `initial-patient` with no `:location`)
;; costs a `disj` against a key it was never in, which is a no-op, and
;; no counter can drift negative.
;;
;; WHAT THE FOLD MAY EMIT, AND WHAT IT MAY NOT (ADR-0169's equivalence
;; obligation). Three of the four findings below name a `:bed` or a
;; `:patient-id` whose ORDER, in the original, came from the iteration
;; order of a Clojure hash map -- `(frequencies beds)` for the first,
;; `world-after` itself for the next two. A carried index with the same
;; keys need not iterate them in the same order (an array-map holds
;; insertion order below 8 entries; a hash-map does not hold it at all),
;; and "identical findings" is a claim about ORDER as well as content.
;; So those three use the carried index ONLY AS A GUARD -- "does this
;; event violate at all?", a question whose answer is a boolean and has
;; no order -- and, when it does, emit from the ORIGINAL EXPRESSION over
;; `world-after`, verbatim. Order-identity is then a theorem rather than
;; a hope, and the O(P) walk is paid only on events that actually
;; violate. `occupancy-within-capacity` is the exception and emits
;; straight from the index: its loop order comes from the `:wards`
;; VECTOR, not from a map, and its whole payload (`:occupied`,
;; `:capacity`) is scalar.
;;
;; Cost, honestly stated: on a CLEAN log -- every gated corpus, and the
;; case the gates exist to keep fast -- the walk is never paid and these
;; are O(N). On a log where some patient violates persistently, the
;; guard fires on every subsequent event and the O(N x P) walk returns.
;; That is a run that has already failed its self-check; the fast path
;; is the passing path, deliberately.
;;
;; The six ORIGINAL bodies are retained verbatim in
;; `ehrt.sim-check.check-test` as `naive-*` reference oracles, and
;; `fast-invariants-equal-their-naive-reference-implementations`
;; asserts `(= (naive-x log) (fast-x log))` over generated churn-bearing
;; runs. A future change to any of the six must move BOTH; the defspec
;; is what notices if it does not.

(defn- participants-of
  "The distinct patient-ids this event names -- the exact set of
  patients whose state `replay` changed at this record, and therefore
  the only entries any index below has to touch.

  ARC 3B SWEEP 2 (ADR-0174 section 2(c)): FILTER 3 OF 3, and the count
  stops at three. `engine/replay` filters the same way at source, so a
  bed participant changes no patient's state and there is nothing here
  for the six fold-carried invariants to reindex."
  [event]
  (distinct (map :patient-id (filter :patient-id (:participants event)))))

(defn- reindex-set
  "`index` with `pid` removed from its `old-key` bucket and added to its
  `new-key` bucket, buckets emptied to nothing rather than left as empty
  sets. A nil key is not a bucket -- `keep`/`filter` in the original
  bodies drop a nil bed and a nil ward alike, so nil is simply not
  indexed."
  [index old-key new-key pid]
  (let [dropped (if (and (some? old-key) (not= old-key new-key))
                  (let [remaining (disj (get index old-key) pid)]
                    (if (seq remaining) (assoc index old-key remaining) (dissoc index old-key)))
                  index)]
    (if (some? new-key)
      (update dropped new-key (fnil conj #{}) pid)
      dropped)))

(defn- reflag
  "`flags` (a set of currently-offending patient-ids) with `pid` added
  when `offending?` and removed otherwise -- idempotent, so a patient
  seen for the first time needs no special case."
  [flags pid offending?]
  (if offending? (conj flags pid) (disj flags pid)))

(defn- bed-of [patient] (get-in patient [:location :bed]))
(defn- ward-of [patient] (get-in patient [:location :ward]))

(defn- fold-records
  "Folds `f` over `(engine/replay ground-truth)`, threading `state` and
  concatenating whatever each step's `emit` produces, in record order.
  `f` is (state record) -> [state' findings]."
  [ground-truth init f]
  (loop [records (engine/replay ground-truth) state init acc (transient [])]
    (if (empty? records)
      (persistent! acc)
      (let [[state' findings] (f state (first records))]
        (recur (rest records) state' (reduce conj! acc findings))))))

(defn no-double-occupancy
  "No bed holds two patients at once, at any event boundary."
  [ground-truth]
  (fold-records
   ground-truth
   {:by-bed {} :dupes #{}}
   (fn [{:keys [by-bed dupes]} {:keys [event world-before world-after]}]
     (let [[by-bed' dupes']
           (reduce (fn [[idx dup] pid]
                     (let [old-bed (bed-of (get world-before pid))
                           new-bed (bed-of (get world-after pid))
                           idx' (reindex-set idx old-bed new-bed pid)
                           touched (remove nil? (distinct [old-bed new-bed]))]
                       [idx' (reduce (fn [d b]
                                       (if (> (count (get idx' b)) 1) (conj d b) (disj d b)))
                                     dup touched)]))
                   [by-bed dupes] (participants-of event))]
       [{:by-bed by-bed' :dupes dupes'}
        ;; Guard positive -> emit from the ORIGINAL expression, so the
        ;; order `frequencies` produces is the order that ships.
        (when (seq dupes')
          (let [beds (keep (comp :bed :location) (vals world-after))
                dupe-beds (->> beds frequencies (filter (comp #(> % 1) val)) (map key))]
            (for [bed dupe-beds]
              {:invariant :no-double-occupancy :bed bed :at (:t event)})))]))))


(defn- one-slot-offender?
  "The predicate `admitted-occupies-one-slot`'s own `:when` clause is,
  lifted so the fold and the emission cannot drift apart."
  [{:keys [status location class]}]
  (and (= status :admitted) (not= class :outpatient)
       (or (nil? location) (nil? (:bed location)))))

(defn admitted-occupies-one-slot
  "An admitted patient (Admitted or Boarding) occupies exactly one
  physical slot -- location and its bed are never nil while admitted.
  M5b: EXCEPT an outpatient (`:class :outpatient`) -- docs/patient-
  state-model.md's event-validity table's own conditional row (`:location
  = nil` is legal exactly when `:class = :outpatient`), the named,
  narrowly-gated exception to this rule (components/patient-simulator/docs/gmf-interpreter.md section
  4's item 6). `outpatient-patients-occupy-no-bed`, below, is this same
  fact's own converse: an outpatient patient's :location must ALWAYS be
  nil, never merely may be."
  [ground-truth]
  (fold-records
   ground-truth
   #{}
   (fn [flags {:keys [event world-after]}]
     (let [flags' (reduce (fn [fs pid] (reflag fs pid (one-slot-offender? (get world-after pid))))
                          flags (participants-of event))]
       [flags'
        (when (seq flags')
          (for [[patient-id patient] world-after
                :when (one-slot-offender? patient)]
            {:invariant :admitted-occupies-one-slot :patient-id patient-id :at (:t event)}))]))))

;; --- M5b: :outpatient-visit / :outpatient-visit-end (components/patient-simulator/docs/gmf-interpreter.md
;; section 4's sketch, item 8's own invariant list) --------------------------

;; ADR-0174 section 2(a) (arc 3b sweep 1): `outpatient-visit-only-when-
;; new` STOOD HERE and is gone, absorbed into
;; `admission-only-when-no-open-encounter` above, which now judges BOTH
;; openers. It was the same rule's second copy -- the two were always
;; one rule written twice -- and the tombstone is left rather than the
;; line silently vanishing, because a reader of this file's M5b section
;; would otherwise find the invariant its own comment above promises.

(defn- outpatient-with-bed? [{:keys [class location]}]
  (and (= class :outpatient) (some? location)))

(defn outpatient-patients-occupy-no-bed
  "The structural half of item 6's conditional validity row: `:class
  :outpatient => :location nil`, for the visit's entire duration -- an
  outpatient patient was never a candidate for the occupancy board to
  include in the first place (`sim-model/occupancy-board`
  already only folds patients with a `:bed` present, so this is checked
  here directly rather than assumed from that board's own omission)."
  [ground-truth]
  (fold-records
   ground-truth
   #{}
   (fn [flags {:keys [event world-after]}]
     (let [flags' (reduce (fn [fs pid] (reflag fs pid (outpatient-with-bed? (get world-after pid))))
                          flags (participants-of event))]
       [flags'
        (when (seq flags')
          (for [[patient-id patient] world-after
                :when (outpatient-with-bed? patient)]
            {:invariant :outpatient-patients-occupy-no-bed :patient-id patient-id :at (:t event)}))]))))

;; --- GMF coverage Wave C (2026-08-02, ADR-0028, C3): :expired --------------

(defn expired-patient-retains-location
  "The converse of admitted-occupies-one-slot: an :expired patient
  (docs/patient-state-model.md's own 'clinically absorbing but
  operationally alive' fact) retains its :location -- the body stays
  wherever it was at the moment of death until a LATER, out-of-this-
  wave's-scope administrative event (morgue transfer, final
  disposition-20 discharge) moves it. Checked at the exact moment an
  expired-disposition :discharge fires (ehrt.sim-engine.engine's own
  :disposition field, riding the compiled step through, sim-model/
  pathway.clj) -- never nil immediately after."
  [ground-truth]
  (for [{:keys [event patient-id after]} (engine/replay ground-truth)
        :when (and (= :discharge (:event event)) (= :expired (:disposition event)) (nil? (:location after)))]
    {:invariant :expired-patient-retains-location :patient-id patient-id :at (:t event)}))


;; --- ARC 3B SWEEP 2 (ADR-0174 section 2(c)): the BED-STATUS CYCLE, judged
;;
;; A cycle that lived only in `world` would be a cycle nothing can
;; judge, and `R-skeleton-or-emission` classifies it skeleton precisely
;; because downstream invariants must respect it -- skeleton means
;; generated AND judged. So the three rows below read the LOG, like
;; every other function in this namespace.
;;
;; THE FOLD BELOW IS DELIBERATELY NOT `ehrt.sim-engine.fold`'s OWN
;; `update-beds`, though it computes the same thing. This namespace is
;; the independent judge; calling the engine's own index-builder here
;; would prove only that the engine agrees with itself, which is the
;; vacuous-gate shape this repository has already been bitten by twice.
;; `engine/replay` is reused -- it is the state fold, not the bed
;; arithmetic -- and the bed arithmetic is written out here.

(def ^:private bed-allocating-event-types
  "The event kinds that ALLOCATE a bed: exactly the kinds the four
  `sim-model/allocate` call sites produce. `:admission`, and `:transfer`
  in all three of its flavours -- an ordinary pathway transfer, the
  bed-ready one, and the one `:transfer-in-error` emits before
  cancelling it.

  `:bed-swap` IS NOT HERE, AND MUST NOT BE. `decide :bed-swap` picks a
  peer who is already `:admitted` with a `:location` and exchanges the
  two locations; it never calls `allocate` at all, and BOTH target beds
  are OCCUPIED by construction. An unqualified \"assignment\" reading of
  the row below would therefore go red on every swap in every corpus --
  ADR-0174 section 2(c)'s own words, kept here because this set is where
  a future writer would add it."
  #{:admission :transfer})

(def ^:private legal-bed-transitions
  "The bed-status transition relation, enumerated so a new writer cannot
  invent a seventh (ADR-0174 section 2(c), invariant 3).

  The cycle itself:      ready -> occupied -> dirty -> cleaning -> ready
  The reinstatement arcs: dirty -> occupied, cleaning -> occupied
  The correction arc:    occupied -> ready

  THE SECOND REINSTATEMENT ARC IS A SEVENTH, and it was found by
  VOLUME rather than by reading: the traffic-scale close of 2026-08-29
  (its section 9, TS-1) ran the arc-4 add-on configuration at 750 and
  7,500 patients and this relation refused 2 and 16 transitions
  respectively -- zero in every corpus this repository ships, because
  the window is one `:turnaround-minutes` draw wide and the gated
  corpora are thin on churn. ADR-0174 section 2(c) carves the
  reinstatement out of `:dirty` ONLY, and `ehrt.sim-engine.engine`'s
  own comment says so -- a `:cancel-discharge` can reinstate a patient
  into a bed whose cycle is already in flight -- but the cycle has
  TWO in-flight legs, and a reinstating cancel can land in the second
  just as easily as the first. THE ENGINE IS CORRECT AND THIS
  ENUMERATION WAS INCOMPLETE: `decide :bed-ready`'s own guard sees the
  non-`:cleaning` bed the cancel leaves behind and emits nothing, so
  the bed ends up correctly occupied and no other row disagreed.
  Ratified into ADR-0174 section 2(c) as its fourth ratification,
  2026-08-29. Carried in the same three places the sixth arc is, and
  gated by `ehrt.sim-engine.bed-cycle-test`'s own authored witness.

  THE CORRECTION ARC IS A SIXTH THE ADR DOES NOT NAME, and it is
  disclosed rather than quietly added. ADR-0174 enumerated the cycle's
  four legs plus the cancel classes that RE-OCCUPY a bed
  (`:cancel-discharge`, and `:cancel-transfer` restoring a prior
  location); it did not reach the two that VACATE one. A
  `:cancel-admit`, and a `:cancel-transfer`'s own erroneously-taken bed,
  both leave a bed with nobody in it and no dirt to clean -- the
  occupancy they retract did not happen. Without this arc that bed would
  stay `:occupied` for the rest of the run and its ward would silently
  lose capacity, which no reading of section 2(c) intends. The reason
  lives in `ehrt.sim-engine.fold`'s own `bed-correction-event-types`
  and is repeated here because this is where a reader checks the
  relation.

  A same-status \"transition\" is not in the relation and is never
  emitted: the fold below records a transition only when the status
  actually moves."
  #{[:ready :occupied]
    [:occupied :dirty]
    [:dirty :cleaning]
    [:cleaning :ready]
    [:dirty :occupied]
    [:cleaning :occupied]
    [:occupied :ready]})

(def ^:private bed-correction-event-types
  "The two kinds whose vacate returns a bed straight to `:ready` --
  `ehrt.sim-engine.fold`'s own set, restated here because this
  namespace reconstructs the index independently and may not read the
  engine's."
  #{:cancel-admit :cancel-transfer})

(defn- bed-cycle-log?
  "Whether this log carries a bed cycle at all.

  THE THREE ROWS BELOW ARE VACUOUS ON A LOG THAT DOES NOT, and that is
  stated rather than left to be discovered. A run without `:bed-cycle`
  emits no `:bed-status-change`, so a bed it vacates never returns to
  `:ready` in any log-derived reading -- and judging such a log against
  the relation above would report every second occupant of every bed as
  a violation. The gate is the same shape sweep 1's own
  `carried-encounter-is-not-the-open-one?` uses for a legacy log: the
  rule applies where the mechanism exists."
  [ground-truth]
  (boolean (some #(= :bed-status-change (:event %)) ground-truth)))

(defn- bed-fold
  "Every bed-status transition this LOG implies, in log order --
  `{:bed :from :to :at :event :declared-from}`. Reconstructed here and
  not read off the engine (see this section's own opening comment).

  Two rules, the same two the cycle has:

  * a `:bed-status-change` moves its own bed to its own `:to`;
  * every other event moves beds by its participants' LOCATION delta --
    a bed newly named goes `:occupied`, and a bed newly left goes
    `:ready` only under `bed-correction-event-types`. A bed left by a
    real vacate is untouched here, because that event's batch carries
    the `:bed-status-change` that turns it `:dirty`.

  Every bed starts `:ready` -- `initial-beds`' own posture, and the
  reason invariant 2's \"except at run start\" clause needs no special
  case: a bed born ready reaches `:ready` through no transition at all.

  Returns `{:transitions [..] :before [..] :records [..]}` -- `:before`
  is the bed index as it stood immediately BEFORE each record, parallel
  to `engine/replay`'s own record seq, and `:records` is that seq
  itself. Invariant 5 (`surge-only-when-earlier-rungs-exhausted`) needs
  the index and not the transitions, because its question is about the
  beds a placement PASSED OVER, which no transition names -- and it gets
  the records back from here so that reading the index costs it no
  SECOND `engine/replay` (`roadmap.md#performance-residual-sites` counts
  those calls, and this sweep adds three, not four)."
  [ground-truth]
  (loop [records (engine/replay ground-truth)
         all-records records beds {} acc (transient []) befores (transient [])]
    (if (empty? records)
      {:transitions (persistent! acc) :before (persistent! befores) :records all-records}
      (let [{:keys [event world-before world-after]} (first records)
            t (:t event)
            ;; THE ENTRY IS A MAP, `{:status ..}`, and not a bare
            ;; keyword. `sim-model/free` -- which invariant 5 hands this
            ;; index to -- reads `(:status (get beds id))`, so a bare
            ;; keyword makes EVERY bed read as not-ready and the whole
            ;; row goes silent on a cycle log while looking clean. That
            ;; is exactly what it did until the hand-built
            ;; dirty-vs-ready case below caught it: the index shape is
            ;; part of the contract with `free`, not a private detail of
            ;; this fold.
            step (fn [[bs found] bed to declared]
                   (let [from (get-in bs [bed :status] :ready)]
                     (if (= from to)
                       [bs found]
                       [(assoc bs bed {:status to})
                        (conj found {:bed bed :from from :to to :at t
                                     :event (:event event) :declared-from declared})])))
            [beds' found]
            (if (= :bed-status-change (:event event))
              (step [beds []] (:bed event) (:to event) (:from event))
              (reduce (fn [state pid]
                        (let [before (get-in world-before [pid :location :bed])
                              after (get-in world-after [pid :location :bed])
                              state' (if (and after (not= after before))
                                       (step state after :occupied nil)
                                       state)]
                          (if (and before (not= after before)
                                   (bed-correction-event-types (:event event)))
                            (step state' before :ready nil)
                            state')))
                      [beds []]
                      (participants-of event)))]
        (recur (rest records) all-records beds' (reduce conj! acc found) (conj! befores beds))))))

(defn- bed-transitions
  "`bed-fold`'s transition half -- the three rows below read only that."
  [ground-truth]
  (:transitions (bed-fold ground-truth)))

(defn- log-derived-bed-fold
  "`bed-fold`, or nil when this log carries no cycle. A nil `:before`
  is what `sim-model/free` reads as \"no index\" and is therefore
  exactly the pre-sweep predicate, which is what a legacy log must still
  be judged by."
  [ground-truth]
  (when (bed-cycle-log? ground-truth)
    (bed-fold ground-truth)))

(defn no-assignment-to-a-non-ready-bed
  "ADR-0174 section 2(c), invariant 1: every event that ALLOCATES a bed
  targets a bed whose status immediately before was `:ready`.

  This is `R-mix-6`'s whole point expressed as a judgement -- an
  allocation into a bed housekeeping has not turned yet is precisely
  what the cycle exists to make impossible, and `sim-model/free`'s
  `:ready` gate is what the engine does about it.

  `:bed-swap` is EXCLUDED, and `bed-allocating-event-types`' own
  docstring carries the reason: a swap allocates nothing and both its
  beds are occupied by construction.

  VACUOUS on a log with no `:bed-status-change` -- see `bed-cycle-log?`."
  [ground-truth]
  (when (bed-cycle-log? ground-truth)
    (for [{:keys [bed from to at event]} (bed-transitions ground-truth)
          :when (and (= :occupied to)
                     (bed-allocating-event-types event)
                     (not= :ready from))]
      {:invariant :no-assignment-to-a-non-ready-bed :bed bed :at at :status from})))

(defn every-ready-follows-a-cleaning
  "ADR-0174 section 2(c), invariant 2: a bed REACHING `:ready` was
  `:cleaning` immediately before.

  Two exemptions, both structural rather than granted:

  * run start -- every bed is BORN `:ready` and reaches it through no
    transition, so `bed-transitions` records nothing to judge;
  * the correction arc -- a `:cancel-admit`'s bed returns to `:ready`
    with no housekeeping because the occupancy it retracts did not
    happen. Judged by `bed-cycle-transitions-are-legal` instead, which
    is where that arc is enumerated.

  VACUOUS on a log with no `:bed-status-change` -- see `bed-cycle-log?`."
  [ground-truth]
  (when (bed-cycle-log? ground-truth)
    (for [{:keys [bed from to at event]} (bed-transitions ground-truth)
          :when (and (= :ready to)
                     (= :bed-status-change event)
                     (not= :cleaning from))]
      {:invariant :every-ready-follows-a-cleaning :bed bed :at at :status from})))

(defn bed-cycle-transitions-are-legal
  "ADR-0174 section 2(c), invariant 3: every bed-status transition is
  one of the seven in `legal-bed-transitions`, and a `:bed-status-change`
  event's DECLARED `:from` is the status the log says the bed was
  actually in.

  The second clause is not in the ADR and is owed by it: `:from` is a
  field the emitter renders (NPU-2's predecessor on the wire is nothing,
  but a consumer of ground truth reads it), so an event that declares a
  transition it did not make is a defect this row can see and no other
  can.

  VACUOUS on a log with no `:bed-status-change` -- see `bed-cycle-log?`."
  [ground-truth]
  (when (bed-cycle-log? ground-truth)
    (let [transitions (bed-transitions ground-truth)]
      (concat
       (for [{:keys [bed from to at]} transitions
             :when (not (legal-bed-transitions [from to]))]
         {:invariant :bed-cycle-transitions-are-legal :bed bed :at at :from from :to to})
       (for [{:keys [bed from at declared-from event]} transitions
             :when (and (= :bed-status-change event) (not= declared-from from))]
         {:invariant :bed-cycle-transitions-are-legal :bed bed :at at
          :declared declared-from :actual from})))))

(defn occupancy-within-capacity
  "Occupancy never exceeds a ward's declared capacity (licensed +
  surge slots).

  ADR-0169: the one member of the fold-carried family that emits
  STRAIGHT from its index rather than falling back to a walk of
  `world-after`. It may, because nothing about its output depends on map
  iteration order -- the loop runs over the `:wards` VECTOR of the
  facility config, in config order, and the only carried value in the
  finding is `:occupied`, a count. The 54.9%-of-the-phase site becomes
  O(W) per event instead of O(P x W)."
  [ground-truth facility-config]
  (fold-records
   ground-truth
   {}
   (fn [by-ward {:keys [event world-before world-after]}]
     (let [by-ward' (reduce (fn [idx pid]
                              (reindex-set idx
                                           (ward-of (get world-before pid))
                                           (ward-of (get world-after pid))
                                           pid))
                            by-ward (participants-of event))]
       [by-ward'
        (for [ward (:wards facility-config)
              :let [cap (+ (:beds ward) (:surge-slots ward))
                    occ (count (get by-ward' (:name ward)))]
              :when (> occ cap)]
          {:invariant :occupancy-within-capacity :ward (:name ward) :at (:t event)
           :occupied occ :capacity cap})]))))

(defn- earlier-rungs-exhausted?
  "Whether the ladder's earlier rungs were legitimately exhausted at
  `board`, for a placement targeting `target-ward-name` on behalf of
  `home-ward-name`: rung 2 (home surge) requires only rung 1 (home
  licensed) exhausted; rung 4 (boarding, target is a DIFFERENT,
  ED-class ward) requires rungs 1-3 all exhausted.

  ARC 3B SWEEP 2 (ADR-0174 section 2(c), invariant 5): THIS ROW CHANGES
  MEANING under the bed cycle and had to be re-read rather than
  re-typed. \"Rung 1 was exhausted\" now means NO RUNG-1 BED WAS READY,
  not \"no rung-1 bed was empty\" -- a bed whose last occupant left ten
  minutes ago is empty and is not available, and a surge placement made
  while it sits `:dirty` is legitimate, not a violation.

  The three `(remove board ...)` calls therefore became three
  `sim-model/free` calls, which is the SAME predicate `allocate`'s own
  rungs ask -- ADR-0174 names this function specifically as one that
  must not carry a second copy of the rule. With `beds` nil (a legacy
  log, or any log with no `:bed-status-change` in it) `free` is
  `(remove board ids)` verbatim and every pre-sweep judgement is
  unchanged, byte for byte."
  [facility-config board beds home-ward-name target-ward-name]
  (let [home-ward (sim-model/ward-by-name facility-config home-ward-name)
        home-licensed-free? (boolean (seq (sim-model/free (sim-model/licensed-bed-ids home-ward) board beds)))
        home-surge-free? (boolean (seq (sim-model/free (sim-model/surge-slot-ids home-ward) board beds)))]
    (if (= home-ward-name target-ward-name)
      (not home-licensed-free?)
      (let [other-inpatient (remove #(= (:id %) (:id home-ward))
                                     (filter #(= :inpatient (:class %)) (:wards facility-config)))
            other-licensed-free? (boolean
                                   (some #(seq (sim-model/free (sim-model/licensed-bed-ids %) board beds))
                                         other-inpatient))]
        (and (not home-licensed-free?) (not home-surge-free?) (not other-licensed-free?))))))

(defn surge-only-when-earlier-rungs-exhausted
  "Surge placement (rung 2 or 4) only occurs when the earlier rungs are
  legitimately exhausted -- unless :forced true (docs/operational-
  models.md's own exemption for the authoring escape hatch).

  ARC 3B SWEEP 2: the CLAIM is unchanged and the READING of \"exhausted\"
  is not -- see `earlier-rungs-exhausted?` above. The bed index it now
  consults is reconstructed from this log alone
  (`log-derived-bed-index`), never read off the engine."
  [ground-truth facility-config]
  (let [folded (log-derived-bed-fold ground-truth)
        beds-before (:before folded)
        records (or (:records folded) (engine/replay ground-truth))]
    (for [[idx {:keys [event world-before patient-id]}] (map-indexed vector records)
          :when (and (#{:admission :transfer} (:event event))
                     (= :surge (get-in event [:location :placement]))
                     (not (:forced event))
                     (not (earlier-rungs-exhausted? facility-config
                                                    (sim-model/occupancy-board world-before)
                                                    (when beds-before (nth beds-before idx))
                                                    (:home-ward event)
                                                    (get-in event [:location :ward]))))]
      {:invariant :surge-only-when-earlier-rungs-exhausted :patient-id patient-id :at (:t event)})))

;; --- M2b: churn family (docs/patient-state-model.md's event-validity
;; table's cancel-*/bed-swap/merge rows; sim/ADR-0010's cross-participant
;; coherence) -------------------------------------------------------------

(def ^:private cancel-target-type
  "Cancel event type -> the event type it must reference."
  {:cancel-admit :admission :cancel-transfer :transfer :cancel-discharge :discharge})

(defn cancel-references-existing-uncancelled-event
  "The event-validity table's cancel-* row: the event class being
  cancelled must exist in this patient's log, be the RIGHT class, and
  not already be cancelled by an earlier cancel of the same kind.
  Structural -- checks any log directly, independent of whether decide
  itself already enforces this (docs/patient-state-model.md)."
  [ground-truth]
  ;; ADR-0169: `cancelled-earlier?` used to re-walk the WHOLE log per
  ;; cancel -- O(C x N), 4.9% of the check phase at 10^5 and quadratic
  ;; in churn density. The question it asks ("did an EARLIER cancel of
  ;; MY kind already name my target?") is answerable from a set carried
  ;; forward by the same single ascending pass the outer `for` already
  ;; makes, so it is. Everything else is verbatim; the emission order is
  ;; the same ascending-index order, because it is the same one pass.
  ;;
  ;; The carried set is keyed by [event-type target-idx] over EVERY
  ;; event, not only cancels: the original's inner predicate requires
  ;; `(= (:event event) (:event ev2))`, and only a same-typed ev2 can
  ;; match a cancel, so indexing the rest is inert -- and indexing it
  ;; anyway is what makes the equivalence need no argument about which
  ;; event types can carry a `:cancels-event-id`.
  (let [indexed (vec ground-truth)]
    (loop [idx 0 seen #{} acc (transient [])]
      (if (>= idx (count indexed))
        (persistent! acc)
        (let [event (nth indexed idx)
              key [(:event event) (:cancels-event-id event)]
              acc' (if (contains? cancel-target-type (:event event))
                     (let [target-idx (:cancels-event-id event)
                           target (get indexed target-idx)
                           expected-type (get cancel-target-type (:event event))
                           patient-id (:patient-id (first (:participants event)))
                           cancelled-earlier? (contains? seen key)]
                       (if (or (nil? target)
                               (not= expected-type (:event target))
                               (not (some #(= patient-id (:patient-id %)) (:participants target)))
                               cancelled-earlier?)
                         (conj! acc {:invariant :cancel-references-existing-uncancelled-event
                                     :patient-id patient-id :at (:t event)})
                         acc))
                     acc)]
          (recur (inc idx) (conj seen key) acc'))))))

(defn bed-swap-both-admitted-before-swap
  "Both bed-swap participants were :admitted immediately beforehand
  (docs/operational-models.md's own admitted-when-placed rule, extended
  to the genuinely-two-participant case -- sim/ADR-0010)."
  [ground-truth]
  (for [{:keys [event world-before]} (engine/replay ground-truth)
        :when (= :bed-swap (:event event))
        {:keys [patient-id]} (:participants event)
        :let [before (get world-before patient-id)]
        :when (not= :admitted (:status before))]
    {:invariant :bed-swap-both-admitted-before-swap :patient-id patient-id :at (:t event)}))

(defn merge-survivor-absorbs-merged-mrns
  "docs/patient-state-model.md's identity payoff: the merge's stated
  surviving MRN must be one the survivor already answered to (not an
  arbitrary string); the survivor's post-merge :active-mrn is exactly
  that; and the survivor's post-merge :mrns is a superset of what the
  merged patient answered to beforehand (retired, not discarded)."
  [ground-truth]
  (for [{:keys [event world-before world-after]} (engine/replay ground-truth)
        :when (= :merge (:event event))
        :let [{:keys [participants surviving-mrn]} event
              survivor-id (:patient-id (first (filter #(= :survivor (:role %)) participants)))
              merged-id (:patient-id (first (filter #(= :merged (:role %)) participants)))
              survivor-before (get world-before survivor-id)
              merged-before (get world-before merged-id)
              survivor-after (get world-after survivor-id)]
        :when (not (and (contains? (:mrns survivor-before) surviving-mrn)
                        (= surviving-mrn (:active-mrn survivor-after))
                        (clojure.set/subset? (:mrns merged-before) (:mrns survivor-after))))]
    {:invariant :merge-survivor-absorbs-merged-mrns :patient-id survivor-id :at (:t event)}))

(defn no-events-after-merged-terminal
  "The merged patient-id's stream ends with its own merge event -- no
  later event in the log names it as a participant (docs/patient-
  state-model.md, sim/ADR-0010)."
  [ground-truth]
  ;; ADR-0169: the inner full-log loop per merge was O(M x N), 2.4% of
  ;; the check phase at 10^5. Two ascending passes replace it -- first
  ;; the merges and the ids they retire, then, for those ids ONLY, where
  ;; in the log they appear -- and the emission then reads the answer off
  ;; the second pass.
  ;;
  ;; The nesting ORDER is what has to survive, and does: the original is
  ;; merge-major (outer `for` over merges, ascending) then log-order
  ;; (inner `for` over later events, ascending), so the loop below stays
  ;; merge-major and each occurrence vector is built in ascending index
  ;; order by construction. A single forward pass would have emitted the
  ;; same findings in EVENT-major order -- a different sequence whenever
  ;; two merges both have violations, which is exactly the case the
  ;; naive-reference defspec generates.
  (let [indexed (vec ground-truth)
        merges (into [] (comp (map-indexed vector)
                              (filter (fn [[_ ev]] (= :merge (:event ev))))
                              (map (fn [[i ev]]
                                     [i (:patient-id (first (filter #(= :merged (:role %))
                                                                    (:participants ev))))])))
                     indexed)
        merged-ids (into #{} (map second) merges)
        occurrences (if (empty? merged-ids)
                      {}
                      ;; DISTINCT participant ids per event, deliberately:
                      ;; the original's inner `for` tested each later event
                      ;; ONCE with `some`, so an event naming the same
                      ;; patient-id twice owes one finding, not two.
                      (reduce-kv (fn [m i ev]
                                   (reduce (fn [m2 pid]
                                             (if (contains? merged-ids pid)
                                               (update m2 pid (fnil conj []) [i (:t ev)])
                                               m2))
                                           m (distinct (map :patient-id (:participants ev)))))
                                 {} indexed))]
    (for [[merge-idx merged-id] merges
          [later-idx later-t] (get occurrences merged-id)
          :when (> later-idx merge-idx)]
      {:invariant :no-events-after-merged-terminal :patient-id merged-id :at later-t})))

;; --- sim/ADR-0012: :step-rejected -- truth about the run, checked structurally
;; (never a message-bearing event -- no message-type-registry entry, by
;; design; see ehrt.sim-engine.decide/documented-step-rejection-reasons) --

(defn step-rejected-reason-is-documented
  "sim/ADR-0012's own invariant: every :step-rejected event's :reason is one
  of the documented enum (ehrt.sim-engine.decide/documented-step-
  rejection-reasons) -- a rejection with an undocumented reason would
  mean a new decide-time rejection path shipped without updating the
  enum, the co-landing convention extended to this event type."
  [ground-truth]
  (for [event ground-truth
        :when (and (= :step-rejected (:event event))
                   (not (contains? engine/documented-step-rejection-reasons (:reason event))))]
    {:invariant :step-rejected-reason-is-documented :reason (:reason event) :at (:t event)}))

;; --- M3: order/result (docs/patient-state-model.md's event-validity
;; table's therapeutic-intent-class row -- orders/results illegal when
;; :status = :expired; written here as "legal only when :admitted", the
;; strict generalization -- GMF coverage Wave C (2026-08-02, ADR-0028)
;; landed :expired for real (engine.clj's own PatientState status enum);
;; this row now covers it automatically, by construction, exactly as
;; anticipated below, no new invariant needed) -----------------------------

(defn order-only-when-admitted
  "Therapeutic-intent class (docs/patient-state-model.md's event-
  validity table): :order-placed is legal only when the patient's prior
  state is :admitted -- covers :new/:discharged/:merged/:expired, since
  none of the four is ever :admitted.

  Deliberately NOT extended to :result-available: a result's own
  turnaround is asynchronous to the rest of the patient's pathway (the
  patient's OTHER steps, including :discharge, are not blocked waiting
  for it -- decide.clj's :order docstring), so a result legitimately
  arriving after discharge is a real, common clinical pattern (pending
  labs at discharge), not a bug -- an engine/run integration test
  surfaced exactly this case during this milestone's own development,
  which is why this invariant is scoped to the order alone rather than
  generalized to both event types. result-references-existing-order-
  and-follows-it-in-time already guarantees a result's own order was
  itself legitimate."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (= :order-placed (:event event))
                   (or (not= :admitted (:status before))
                       ;; ADR-0174's table, per-encounter (see
                       ;; `transfer-only-when-admitted`'s own note).
                       (carried-encounter-is-not-the-open-one? event before patient-id)))]
    {:invariant :order-only-when-admitted :patient-id patient-id :at (:t event)}))

(defn result-references-existing-order-and-follows-it-in-time
  "Every :result-available event's :order-event-id is a real
  :order-placed event in this same log, for the SAME patient, at or
  before the result's own :t (co-landing invariant, Milestone M3)."
  [ground-truth]
  (let [indexed (vec ground-truth)]
    (for [[idx event] (map-indexed vector indexed)
          :when (= :result-available (:event event))
          :let [target-idx (:order-event-id event)
                target (get indexed target-idx)
                patient-id (:patient-id (first (:participants event)))]
          :when (or (nil? target)
                    (not= :order-placed (:event target))
                    (not (some #(= patient-id (:patient-id %)) (:participants target)))
                    (> (:t target) (:t event)))]
      {:invariant :result-references-existing-order-and-follows-it-in-time :patient-id patient-id :at (:t event)})))

(defn result-analytes-match-order-profile
  "Every :result-available event's :results analyte-concept set is
  EXACTLY its own :profile's analyte set (`order-profiles` -- default
  ehrt.sim-engine.order-profiles/default-profiles, the same 'needs more
  than just the log' pattern facility-catalog/warmup-catalog already
  follow) -- catches a result that dropped, added, or substituted an
  analyte relative to what its own profile declares."
  [ground-truth order-profiles]
  (for [event ground-truth
        :when (= :result-available (:event event))
        :let [expected (into #{} (map :concept) (:analytes (get order-profiles (:profile event))))
              actual (into #{} (map :concept) (:results event))]
        :when (not= expected actual)]
    {:invariant :result-analytes-match-order-profile :profile (:profile event) :at (:t event)}))

(defn abnormal-flags-consistent-with-value-vs-range
  "The computed-truth mini-law (Milestone M3 Task 4), checked from the
  log directly: every result entry's :abnormal-flag equals
  ehrt.sim-engine.order-profiles/abnormal-flag applied to its own value
  and reference-range -- a flag that disagrees with its own value is a
  bug, not a legitimate finding."
  [ground-truth]
  (for [event ground-truth
        :when (= :result-available (:event event))
        {:keys [value reference-range abnormal-flag]} (:results event)
        :when (not= abnormal-flag (order-profiles/abnormal-flag value reference-range))]
    {:invariant :abnormal-flags-consistent-with-value-vs-range :profile (:profile event) :at (:t event)}))

;; --- M5b: CompileTrajectory's new event types (components/patient-simulator/docs/gmf-interpreter.md
;; section 1's table) -- :procedure/:observation/:medication-order are the
;; therapeutic-intent class (docs/patient-state-model.md's event-validity
;; table row), the same "legal only when :admitted" scoping :order-placed
;; already gets; :medication-end is deliberately NOT included, same reason
;; :result-available isn't -- a medication legitimately continues (and
;; ends) after discharge (a patient still taking a prescription at home).

(defn clinical-content-only-when-admitted
  "Therapeutic-intent class, extended to M5b's compiled clinical content:
  :procedure/:observation/:medication-order are legal only when the
  patient's prior state is :admitted. GMF coverage Wave D stage D1
  (ADR-0029): :diagnostic-report joins the set -- the same
  therapeutic-intent-class scoping every other compiled clinical event
  type already gets. GMF coverage Wave D stage D2 (ADR-0029): :care-plan-
  start joins too -- grounded directly against Synthea's own State.java
  ('CarePlanStart states may only be processed during an Encounter'),
  the SAME real constraint :procedure's own doc comment already states;
  :care-plan-end is deliberately NOT included, same reason
  :medication-end isn't -- a care plan legitimately continues (and
  ends) after discharge."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (#{:procedure :observation :medication-order :diagnostic-report :care-plan-start} (:event event))
                   (or (not= :admitted (:status before))
                       ;; ADR-0174's table, per-encounter: and the stamp
                       ;; names the OPEN encounter, so a condition
                       ;; recorded during visit 2 is not silently
                       ;; attributed to visit 1.
                       (carried-encounter-is-not-the-open-one? event before patient-id)))]
    {:invariant :clinical-content-only-when-admitted :patient-id patient-id :at (:t event)}))

(defn- pre-horizon-medication-order-citations-by-patient
  "patient-id -> the set of :citation values riding that patient's own
  :registered event as a :medication-order entry in :pre-horizon-facts
  -- the compile layer's designed straddle case (components/patient-
  simulator/docs/trajectory-computation.md, 'History phase'): an order
  crossed during history phase is real, ongoing therapeutic content,
  promoted to a registration-time fact rather than dropped, while its
  own end can legitimately land in horizon phase as a normal
  ground-truth event with nothing in top-level :medication-order to
  resolve :order-event-id against."
  [ground-truth]
  (into {}
        (keep (fn [event]
                (when (= :registered (:event event))
                  (when-let [citations (seq (into #{}
                                                   (comp (filter #(= :medication-order (:event %)))
                                                         (map :citation))
                                                   (:pre-horizon-facts event)))]
                    [(:patient-id (first (:participants event))) (set citations)]))))
        ground-truth))

(defn medication-end-references-existing-order-and-follows-it-in-time
  "Every :medication-end event's :order-event-id is a real
  :medication-order event in this same log, for the SAME patient, at or
  before the end's own :t -- the same shape result's own referential
  invariant already establishes for :order-placed/:result-available --
  OR, when no such ground-truth event exists, the SAME patient's own
  :registered event carries a matching :medication-order entry in its
  own :pre-horizon-facts (the designed order/end straddle,
  trajectory-computation.md's 'History phase'). A pre-horizon fact
  carries no :t of its own, so the follows-in-time law is satisfied by
  construction in that branch: the fact is definitionally prior to
  registration, and every ground-truth event -- this :medication-end
  included -- comes after it, since :registered is always a patient's
  first event."
  [ground-truth]
  (let [indexed (vec ground-truth)
        pre-horizon-citations (pre-horizon-medication-order-citations-by-patient ground-truth)]
    (for [[idx event] (map-indexed vector indexed)
          :when (= :medication-end (:event event))
          :let [target-idx (:order-event-id event)
                target (get indexed target-idx)
                patient-id (:patient-id (first (:participants event)))
                pre-horizon-referent? (and (nil? target)
                                            (contains? (get pre-horizon-citations patient-id)
                                                       (:order-citation event)))]
          :when (and (not pre-horizon-referent?)
                     (or (nil? target)
                         (not= :medication-order (:event target))
                         (not (some #(= patient-id (:patient-id %)) (:participants target)))
                         (> (:t target) (:t event))))]
      {:invariant :medication-end-references-existing-order-and-follows-it-in-time :patient-id patient-id :at (:t event)})))

(defn- pre-horizon-care-plan-start-citations-by-patient
  "patient-id -> the set of :citation values riding that patient's own
  :registered event as a :care-plan-start entry in :pre-horizon-facts
  -- the exact twin of `pre-horizon-medication-order-citations-by-
  patient` above, and PROBED rather than inferred from that twin
  (ADR-0166 step 7): `ehrt.patient-simulator.compile-trajectory`'s own
  `pre-horizon-fact-types` carries `:care-plan-start`/`:care-plan-end`
  in the SAME 'ongoing therapeutic content' class it carries
  `:medication-order`/`:medication-end` in (ADR-0029 D2), the compile
  loop promotes any of them to a registration fact through ONE shared
  clause, and `ehrt.sim-engine.event-schema/PreHorizonFact` declares
  all six in its own `:event` enum. A care plan opened years before
  registration and still open is a registration-time fact; its end can
  legitimately land in horizon phase with nothing in top-level
  :care-plan-start to resolve :start-event-id against."
  [ground-truth]
  (into {}
        (keep (fn [event]
                (when (= :registered (:event event))
                  (when-let [citations (seq (into #{}
                                                   (comp (filter #(= :care-plan-start (:event %)))
                                                         (map :citation))
                                                   (:pre-horizon-facts event)))]
                    [(:patient-id (first (:participants event))) (set citations)]))))
        ground-truth))

(defn care-plan-end-references-existing-start-and-follows-it-in-time
  "Every :care-plan-end event's :start-event-id is a real
  :care-plan-start event in this same log, for the SAME patient, at or
  before the end's own :t -- OR, when no such ground-truth event
  exists, the SAME patient's own :registered event carries a matching
  :care-plan-start entry in its own :pre-horizon-facts (the designed
  start/end straddle, trajectory-computation.md's 'History phase').
  The mirror of `medication-end-references-existing-order-and-follows-
  it-in-time` above, on the twin span the engine's own `decide
  :care-plan-end` resolves the identical way.

  ORIGIN (ADR-0163, ADR-0166): `:care-plan-end` was the ONE paired
  terminal event type this catalog did not cover, and the silence was
  load-bearing. Seed 5 over `demos/scenarios/clinic-decade` exited 0
  while its log carried TWO :care-plan-end events with no citation at
  all (`PID-000045-03ebff87` at t 3636360, `PID-000187-899c715a` at t
  27417360) -- found by hand, by a session reading the log, because no
  invariant was asking. `:medication-end`'s own twin defect, in the
  same run family, exited 2. This invariant is why the next one would
  not need finding by hand."
  [ground-truth]
  (let [indexed (vec ground-truth)
        pre-horizon-citations (pre-horizon-care-plan-start-citations-by-patient ground-truth)]
    (for [event ground-truth
          :when (= :care-plan-end (:event event))
          :let [target-idx (:start-event-id event)
                target (get indexed target-idx)
                patient-id (:patient-id (first (:participants event)))
                pre-horizon-referent? (and (nil? target)
                                            (contains? (get pre-horizon-citations patient-id)
                                                       (:care-plan-citation event)))]
          :when (and (not pre-horizon-referent?)
                     (or (nil? target)
                         (not= :care-plan-start (:event target))
                         (not (some #(= patient-id (:patient-id %)) (:participants target)))
                         (> (:t target) (:t event))))]
      {:invariant :care-plan-end-references-existing-start-and-follows-it-in-time
       :patient-id patient-id :at (:t event)})))

;; --- arc 3a part 3: the person-fold family (ADR-0173 section 2(e)) --------
;;
;; SIX, and they land TOGETHER even though only three of them can fire
;; on anything this arc's own commits produce. Three are over the
;; identification flow, which is part 4's -- and a gate written after
;; the code it constrains is a gate written to agree with it. Each of
;; the three is proved red by MUTATION instead (`ehrt.sim-check.person-
;; invariants-test`), which is the only honest proof available for an
;; invariant whose producer has not landed.
;;
;; The first is `medication-end-references-existing-order-and-follows-
;; it-in-time`'s own body, verbatim in shape: resolve an index into this
;; same log, require the right kind, the same patient, and a `:t` at or
;; before the referring event's. The pre-horizon escape is inherited
;; too, and for the same reason it exists there: a reference that is
;; ABSENT is not a reference that DANGLES.

(defn- registrations-by-patient
  "patient-id -> that patient's own `:registered` event. One scan, shared
  by the three identification invariants below, none of which can say
  anything without it."
  [ground-truth]
  (into {}
        (keep (fn [ev]
                (when (= :registered (:event ev))
                  [(:patient-id (first (:participants ev))) ev])))
        ground-truth))

(defn identity-fill-references-its-placeholder-registration
  "Every `:demographic-update` with `:cause :identity-fill` carries a
  `:placeholder-event-id` indexing a real `:registered` event in this
  same log, for the SAME patient, carrying `:identity :placeholder`, at
  or before the fill's own `:t`.

  Verbatim the `:medication-end` shape, one level of vocabulary across.
  ADR-0173 section 2(d) is what mints the reference: an arrival landing
  inside an open identity-unavailable window registers on a FRESH
  patient-id and MRN with a placeholder name and no address, and the
  window's close fills every field in. `:identity-fill` is arc 3a part
  4's cause, declared by nothing this arc produces -- so this invariant
  is born green over a corpus with no fills in it, and its own red is a
  mutation."
  [ground-truth]
  (let [indexed (vec ground-truth)]
    (for [event ground-truth
          :when (and (= :demographic-update (:event event))
                     (= :identity-fill (:cause event)))
          :let [target (get indexed (:placeholder-event-id event))
                patient-id (:patient-id (first (:participants event)))]
          :when (or (nil? target)
                    (not= :registered (:event target))
                    (not= :placeholder (:identity target))
                    (not (some #(= patient-id (:patient-id %)) (:participants target)))
                    (> (:t target) (:t event)))]
      {:invariant :identity-fill-references-its-placeholder-registration
       :patient-id patient-id :at (:t event)})))

(defn identification-merge-survivor-is-the-persons-prior-patient
  "A `:merge` with `:cause :identification` names as `:merged` a patient
  whose own `:registered` carries `:identity :placeholder`, and as
  `:survivor` a patient whose own `:registered` carries the SAME
  `:person-id`.

  The merge itself composes with churn's and does not duplicate it --
  same kind, same participant roles, same MRN payload, so
  `merge-survivor-absorbs-merged-mrns`, `no-events-after-merged-
  terminal` and the run loop's own `:merged` short-circuit all apply
  unchanged. `:cause :identification` is the ONE thing that
  distinguishes it, and this invariant is what makes that marker mean
  something. Arc 3a part 4 mints both `:cause :identification` and the
  `:person-id` stamp this reads; a churn merge carries neither and is
  not examined."
  [ground-truth]
  (let [registrations (registrations-by-patient ground-truth)]
    (for [event ground-truth
          :when (and (= :merge (:event event)) (= :identification (:cause event)))
          :let [by-role (into {} (map (juxt :role :patient-id)) (:participants event))
                merged (get registrations (:merged by-role))
                survivor (get registrations (:survivor by-role))]
          :when (or (nil? merged) (nil? survivor)
                    (not= :placeholder (:identity merged))
                    (nil? (:person-id merged))
                    (not= (:person-id merged) (:person-id survivor)))]
      {:invariant :identification-merge-survivor-is-the-persons-prior-patient
       :patient-id (:merged by-role) :at (:t event)})))

(defn- placeholder-registrations
  "Every placeholder `:registered` in the log, as
  `{:patient-id :at :window-close-t}`. Factored because three readers
  need the same set: the two invariants below and
  `placeholder-dispositions`."
  [ground-truth]
  (into []
        (for [ev ground-truth
              :when (and (= :registered (:event ev)) (= :placeholder (:identity ev)))]
          {:patient-id (:patient-id (first (:participants ev)))
           :at (:t ev)
           :window-close-t (:window-close-t ev)})))

(defn- consuming-merges
  "patient-id -> `{:i <log index> :t <instant>}` of the FIRST merge not
  caused by identification that named them `:merged`.

  An identification merge is excluded because it is a RESOLUTION and is
  counted as one; everything else -- churn's M2b `:merge`, and any
  future merge kind -- absorbs the record without claiming to have
  identified anybody, which is the failure shape
  `every-placeholder-registration-is-resolved-or-still-open` documents.

  The INDEX rides alongside the instant because
  `no-resolution-after-a-placeholder-is-consumed` has to order a
  resolution against the merge that consumed it, and two events at the
  same `:t` are ordered by the log and by nothing else."
  [ground-truth]
  (persistent!
   (first
    (reduce (fn [[acc i] ev]
              [(if (and (= :merge (:event ev)) (not= :identification (:cause ev)))
                 (reduce (fn [a p]
                           (if (and (= :merged (:role p))
                                    (not (contains? a (:patient-id p))))
                             (assoc! a (:patient-id p) {:i i :t (:t ev)})
                             a))
                         acc (:participants ev))
                 acc)
               (inc i)])
            [(transient {}) 0] ground-truth))))

(defn- identity-resolutions
  "patient-id -> `:fill` or `:identification-merge`, whichever resolved
  that placeholder's identity."
  [ground-truth]
  (into {}
        (concat
         (for [ev ground-truth
               :when (and (= :merge (:event ev)) (= :identification (:cause ev)))
               p (:participants ev)
               :when (= :merged (:role p))]
           [(:patient-id p) :identification-merge])
         (for [ev ground-truth
               :when (and (= :demographic-update (:event ev))
                          (= :identity-fill (:cause ev)))]
           [(:patient-id (first (:participants ev))) :fill]))))

(defn every-placeholder-registration-is-resolved-or-still-open
  "A placeholder registration either gets its fill or its identification
  merge, or is CONSUMED -- absorbed whole by a merge that never claimed
  to have identified anybody -- or the run ENDED before its window was
  due to close.

  NEVER \"or not at all\": a placeholder left dangling by a horizon is
  real traffic -- an unidentified patient whose identity nobody had
  established by the time the simulated feed stopped -- and an invariant
  that forbade it would be wrong about the world rather than about the
  log. `:window-close-t` is what carries the horizon clause; part 4's
  placeholder registration mints it, and a placeholder carrying none
  cannot be judged either way, so it is left alone.

  THE CONSUMED CLAUSE IS 2026-08-29's, and it is a statement about the
  WORLD rather than a relaxation for the engine's convenience
  (`roadmap.md#ts-4-placeholder-unresolved`). The failure shape it
  admits is a real and named one: **an erroneous merge eats a John
  Doe.** An unidentified record is absorbed into some other patient's
  before anybody establishes whose it was, and the identity question it
  was carrying does not survive -- not because the feed stopped, but
  because a clerk merged it away. That is one of the characteristic
  ways an MPI fails, the corpus is telling the truth about it, and the
  engine did nothing wrong in producing it: churn merged two records,
  which is what churn does.

  THE WITNESS, so a later reader can see the shape rather than trust
  this paragraph. At the `nobed` and `v2` 10^5 add-on cells, seed
  20260824, `PID-007500-e98926c1` registers a placeholder John Doe at
  t=37017 with `:window-close-t 382617`, is admitted as \"Unidentified
  patient\" and discharged, and at t=177420 is drawn out of a
  666-strong eligible set as the `:merged` participant of an ORDINARY
  churn `:merge` carrying no `:cause`. Its own `:identity-fill`, seeded
  on it at t=382617, is then never decided at all -- the run loop
  short-circuits a queue entry whose patient is already `:merged` -- so
  nothing is minted after the merge and nothing ever can be. One
  violation in 129,415 events, and it was the last thing standing
  between the traffic-scale programme and two MEASURED cells.

  CONSUMPTION CLOSES THE WINDOW ONLY UP TO ITS DUE INSTANT. A merge
  AFTER the close does not retroactively excuse a placeholder that was
  already dangling when identification came due -- that log really does
  show an unresolved window, and the clause is deliberately too narrow
  to hide it.

  ITS OTHER HALF is `no-resolution-after-a-placeholder-is-consumed`
  below: once consumed, nothing may fill or identification-merge the
  record afterwards. Read the two together -- this one says a
  consumption ENDS the identity question, and that one says it stays
  ended."
  [ground-truth]
  (let [last-t (:t (last ground-truth))
        resolved (identity-resolutions ground-truth)
        consumed (consuming-merges ground-truth)]
    (for [{:keys [patient-id at window-close-t]} (placeholder-registrations ground-truth)
          :let [consumed-t (:t (get consumed patient-id))]
          :when (and (some? window-close-t) (some? last-t) (<= window-close-t last-t)
                     (not (contains? resolved patient-id))
                     (not (and consumed-t (<= consumed-t window-close-t))))]
      {:invariant :every-placeholder-registration-is-resolved-or-still-open
       :patient-id patient-id :at at})))

(defn no-resolution-after-a-placeholder-is-consumed
  "Once a merge has absorbed a placeholder record, nothing fills it and
  nothing identification-merges it.

  THE OTHER HALF of the consumed clause above, and the reason that
  clause is safe. Making a consumption count as the end of an identity
  question is only sound if the question really is over: a fill landing
  on a record that was merged away would be the log claiming to have
  identified somebody whose record no longer exists, and the invariant
  above would have gone quiet on exactly the run where that happened.

  IT IS A LATENT ENGINE DEFECT MADE PERMANENTLY VISIBLE, not a
  hypothetical. `decide :identity-fill`'s own outcome function refuses
  only on `(not= :placeholder (:identity (:demographics patient)))`,
  and `evolve :merge` sets `:status :merged` while leaving the
  demographics -- alias and all -- exactly as they were. So a
  consumed placeholder still LOOKS fillable to that decide, and the
  only thing standing between it and a `:demographic-update` on a
  merged patient is the run loop's `:merged` short-circuit, which is
  one `if` in `run` and nothing asserts it from outside.
  `decide :identification-merge` has the same gap from the other side:
  it guards the SURVIVOR's status and never the placeholder's own.
  Measured at both 10^5 add-on cells: 1,062 resolution steps seeded,
  1,061 decided, and the one that never ran is the consumed
  placeholder's -- so the short-circuit does hold today, and this is
  what says so tomorrow.

  DELIBERATELY OVERLAPPING `no-events-after-merged-terminal`, which
  forbids ANY later event naming a merged patient and therefore
  subsumes this on today's catalog. Disclosed rather than quietly
  duplicated: the two would be separated the day a merged patient is
  allowed any trailing event at all, and the clause above depends on
  THIS one specifically, not on the general rule that happens to imply
  it now."
  [ground-truth]
  (let [consumed (consuming-merges ground-truth)
        placeholders (into #{} (map :patient-id) (placeholder-registrations ground-truth))]
    (for [[i ev] (map-indexed vector ground-truth)
          :let [kind (cond (and (= :demographic-update (:event ev))
                                (= :identity-fill (:cause ev)))
                           :fill
                           (and (= :merge (:event ev)) (= :identification (:cause ev)))
                           :identification-merge)]
          :when kind
          p (:participants ev)
          :let [patient-id (:patient-id p)
                consumed-at (get consumed patient-id)]
          :when (and (placeholders patient-id)
                     (some? consumed-at)
                     (> i (:i consumed-at)))]
      {:invariant :no-resolution-after-a-placeholder-is-consumed
       :patient-id patient-id :at (:t ev) :resolution kind})))

(defn placeholder-dispositions
  "The census behind the two invariants above: every placeholder
  registration in a log, classified by what became of it.

    {:total n :unjudgeable n :resolved-by-fill n
     :resolved-by-identification-merge n :consumed-by-churn n
     :still-open n :dangling n}

  WHY A COUNT AND NOT ONLY A GATE. `consumed-by-churn` is a shape the
  catalog now TOLERATES, and a tolerated shape that nothing counts is
  indistinguishable from a shape that never happens -- which is how a
  clause added for one witness quietly becomes a clause covering a
  hundred. This is the column that makes the difference visible, and
  `roadmap.md#ts-4-placeholder-unresolved` is why it exists.

  `:unjudgeable` is a placeholder carrying no `:window-close-t` (the
  engine withholds one from a window that never resolves -- the person
  died inside it); `:still-open` is one whose close instant is past the
  end of the log. Neither is a defect and neither is counted as one.
  The classes are disjoint and sum to `:total`; `:dangling` is exactly
  what `every-placeholder-registration-is-resolved-or-still-open`
  reports."
  [ground-truth]
  (let [last-t (:t (last ground-truth))
        resolved (identity-resolutions ground-truth)
        consumed (consuming-merges ground-truth)
        classify (fn [{:keys [patient-id window-close-t]}]
                   (let [consumed-t (:t (get consumed patient-id))]
                     (cond
                       (= :fill (get resolved patient-id)) :resolved-by-fill
                       (= :identification-merge (get resolved patient-id))
                       :resolved-by-identification-merge
                       (nil? window-close-t) :unjudgeable
                       (and consumed-t (<= consumed-t window-close-t)) :consumed-by-churn
                       (or (nil? last-t) (> window-close-t last-t)) :still-open
                       :else :dangling)))
        counted (frequencies (map classify (placeholder-registrations ground-truth)))]
    (merge {:total (reduce + (vals counted))
            :unjudgeable 0 :resolved-by-fill 0 :resolved-by-identification-merge 0
            :consumed-by-churn 0 :still-open 0 :dangling 0}
           counted)))

(defn demographic-update-reports-a-real-change
  "The prior values a demographic event carries equal the folded state
  IMMEDIATELY BEFORE it, and differ from the values it reports.

  Arc 2b's own lesson promoted from the person side to the wire side --
  *an event that reports no change is not an event* (`b4f1115`). A
  `:demographic-update` whose `:value` equals its `:prior-value` renders
  an A08 that changes no PID field, which is traffic with no message in
  it; and a `:prior-value` that disagrees with the fold is a claim about
  this patient's history that the log itself contradicts.

  `:coverage-change` is the SAME law on `:payer` and is checked here
  rather than in a seventh invariant nobody named: one kind reports a
  demographic change and the other an insurance change, and the honesty
  obligation does not know the difference.

  A hand-authored event carrying no prior at all is not examined for the
  first half -- `:prior-value`/`:prior-payer` are `{:optional true}` in
  the contract, and a correction of a field never previously set has no
  prior to report."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :let [[prior-key value-key field]
              (case (:event event)
                :demographic-update [:prior-value :value (:field event)]
                :coverage-change [:prior-payer :payer :payer]
                nil)]
        :when (and field
                   (or (= (get event prior-key) (get event value-key))
                       (and (contains? event prior-key)
                            (some? (:demographics before))
                            (not= (get event prior-key)
                                  (get (:demographics before) field)))))]
    {:invariant :demographic-update-reports-a-real-change
     :patient-id patient-id :at (:t event)}))

(defn no-demographic-event-after-a-patient-expires
  "No `:demographic-update`, `:coverage-change` or `:registered` for a
  patient whose state is already `:expired`.

  ADR-0173 ruling C1's behavioural half. The person process outlives the
  patient by design -- its horizon is years and a run's is hours -- so
  the fold has to stop somewhere, and a patient the log has already
  discharged as expired is where. It is also the gate on the arrival
  candidate set: a dead person is not selectable, so a `:registered`
  after an expiry is the one shape that would prove the alive-filter had
  stopped working."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (#{:demographic-update :coverage-change :registered} (:event event))
                   (= :expired (:status before)))]
    {:invariant :no-demographic-event-after-a-patient-expires
     :patient-id patient-id :at (:t event)}))

(defn person-scoped-provenance-is-a-stamp-not-a-reference
  "`:person-event-id` is a STAMP -- the person stream's own
  `\"<person-id>#<n>\"` string -- and never a log index.

  The distinction is load-bearing and this catalog is where it is kept.
  Every OTHER `-event-id` key in this log (`:order-event-id`,
  `:start-event-id`, `:placeholder-event-id`) IS a log index, resolved
  by `nth` into the same vector, and three invariants above do exactly
  that. `:person-event-id` cannot be: person events are not log events,
  so resolving it would be a dangling reference BY CONSTRUCTION rather
  than by accident.

  Born green, and red the day someone mints an integer here -- which is
  the only way an invariant could come to `nth` it. A string is not
  indexable, so the type is the guard, and asserting the type is
  asserting that no invariant in this catalog can treat it as an index."
  [ground-truth]
  (for [event ground-truth
        :when (and (contains? event :person-event-id)
                   (not (string? (:person-event-id event))))]
    {:invariant :person-scoped-provenance-is-a-stamp-not-a-reference
     :patient-id (:patient-id (first (:participants event))) :at (:t event)}))

;; --- M4: Persona (docs/sim-theory.edn's :persona stage) -------------------

(defn registered-is-every-patients-first-event
  "docs/sim-theory.edn's :persona stage lands as the engine-internal
  :registered event, prepended to every patient's step queue
  (ehrt.sim-engine.run/run) -- structurally, that means it must be
  the FIRST event naming any given patient-id, every time, or
  ehrt.sim-engine.fold/replay's own bootstrap (which seeds a
  never-yet-seen participant's initial state off the first event
  naming them, sim/ADR-0010) would silently seed from the wrong event."
  [ground-truth]
  (for [[patient-id events] (events-by-patient ground-truth)
        :when (not= :registered (:event (first events)))]
    {:invariant :registered-is-every-patients-first-event :patient-id patient-id :at (:t (first events))}))

(defn registered-persona-is-schema-valid
  "Every :registered event's :persona validates against
  sim-model/Persona -- the schema round-trip co-landing
  invariant for M4's new persona resource type."
  [ground-truth]
  (for [event ground-truth
        :when (and (= :registered (:event event)) (not (sim-model/valid-persona? (:persona event))))]
    {:invariant :registered-persona-is-schema-valid :at (:t event)}))


;; --- ARC 3B SWEEP 3 (ADR-0174 section 2(b)): SCHEDULING's own four.
;;
;; ALL FOUR ARE VACUOUS ON A LOG WITH NO `:appointment`, and that is
;; stated here rather than left to be discovered -- the same treatment
;; `bed-cycle-log?` gives the cycle's three. A run that did not opt into
;; `:scheduling` mints no appointment, so there is nothing to resolve,
;; nothing to follow and nothing to no-show.
;;
;; THE SECOND IS NON-VACUOUS ONLY BECAUSE SWEEP 1 LANDED, which the ADR
;; says in as many words: without a SECOND encounter every appointment
;; would trivially precede its patient's first and only visit, and the
;; row would assert nothing about anything. `scheduling-test` asserts
;; the COUNT of openers it actually judges, not merely that it is green.

(defn- scheduling-log?
  "Whether this log carries appointments at all."
  [ground-truth]
  (boolean (some #(= :appointment (:event %)) ground-truth)))

(defn- appointment-fold
  "Per patient, per appointment-id, what the LOG says happened to it --
  `{[patient-id appointment-id] {:booked-at .. :terminals [..] :order ..}}`.

  Reconstructed from the log and not read off the engine, for this
  namespace's own standing reason: a check that asked the engine what it
  did would agree with the engine by construction."
  [ground-truth]
  (persistent!
   (reduce
    (fn [acc [i ev]]
      (let [pid (:patient-id (first (:participants ev)))
            aid (:appointment-id ev)
            k [pid aid]]
        (case (:event ev)
          :appointment (assoc! acc k {:booked-at i :scheduled-t (:scheduled-t ev) :terminals []})
          (:appointment-cancel :no-show)
          (if-let [rec (get acc k)]
            (assoc! acc k (update rec :terminals conj {:kind (:event ev) :at i}))
            acc)
          acc)))
    (transient {})
    (map-indexed vector ground-truth))))

(defn appointment-reference-resolves
  "ADR-0174 section 2(b), invariant 1: every `:reschedule`,
  `:appointment-cancel` and `:no-show` names an `:appointment-id` that an
  `:appointment` EARLIER in the SAME patient's log minted.

  Same-patient is the whole point: an id that resolves against somebody
  else's appointment is exactly the cross-patient reference this row
  exists to forbid. VACUOUS on a log with no appointments.

  A PURE LEFT FOLD, not a scan with an accumulator beside it -- this
  namespace may not call `atom`/`volatile!` at all
  (`sim-purity-lint-test`, ADR-0108), and the reading state here is a
  set that grows in log order, which is exactly what `reduce` is."
  [ground-truth]
  (when (scheduling-log? ground-truth)
    (:violations
     (reduce
      (fn [{:keys [booked] :as acc} ev]
        (let [pid (:patient-id (first (:participants ev)))
              k [pid (:appointment-id ev)]]
          (case (:event ev)
            :appointment (update acc :booked conj k)
            (:reschedule :appointment-cancel :no-show)
            (if (contains? booked k)
              acc
              (update acc :violations conj
                      {:invariant :appointment-reference-resolves
                       :event (:event ev) :t (:t ev)
                       :patient-id pid :appointment-id (:appointment-id ev)}))
            acc)))
      {:booked #{} :violations []}
      ground-truth))))

(defn scheduled-encounter-follows-its-appointment
  "ADR-0174 section 2(b), invariant 2: an opener carrying an
  `:appointment-id` has that appointment earlier in its OWN patient's
  log, at or before its `:t`, and NOT already terminal.

  Not merely present-earlier: an opener keeping an appointment a cancel
  already closed is the defect that makes invariant 3 insufficient on
  its own, and an opener firing BEFORE its own `:scheduled-t` is a visit
  that jumped its appointment.

  NON-VACUOUS ONLY BECAUSE SWEEP 1 LANDED -- the ADR says so in as many
  words. Without a SECOND encounter every appointment would trivially
  precede its patient's first and only visit, and this row would assert
  nothing about anything. `scheduling-test` asserts the COUNT of openers
  it judges rather than merely that it is green."
  [ground-truth]
  (when (scheduling-log? ground-truth)
    (:violations
     (reduce
      (fn [{:keys [state] :as acc} ev]
        (let [pid (:patient-id (first (:participants ev)))
              k [pid (:appointment-id ev)]]
          (case (:event ev)
            :appointment (assoc-in acc [:state k] {:scheduled-t (:scheduled-t ev)})
            :reschedule (update-in acc [:state k] merge {:scheduled-t (:scheduled-t ev)})
            (:appointment-cancel :no-show) (assoc-in acc [:state k :terminal] (:event ev))
            (:admission :outpatient-visit)
            (if-not (:appointment-id ev)
              acc
              (let [rec (get state k)]
                (if (or (nil? rec)
                        (some? (:terminal rec))
                        (> (:scheduled-t rec) (:t ev)))
                  (update acc :violations conj
                          {:invariant :scheduled-encounter-follows-its-appointment
                           :event (:event ev) :t (:t ev)
                           :patient-id pid :appointment-id (:appointment-id ev)
                           :appointment rec})
                  acc)))
            acc)))
      {:state {} :violations []}
      ground-truth))))

(defn no-show-has-no-encounter
  "ADR-0174 section 2(b), invariant 3: no opener carries a NO-SHOWED
  appointment's id.

  Weaker than invariant 2 on its own -- 2 already forbids an opener
  against any terminal appointment -- and kept as its own row because it
  is the one the design's whole no-show argument rests on: a no-show is
  precisely an appointment with no encounter to derive it from, which is
  why appointments could not be retro-derived from encounters."
  [ground-truth]
  (when (scheduling-log? ground-truth)
    (let [no-showed (into #{} (for [ev ground-truth
                                    :when (= :no-show (:event ev))]
                                [(:patient-id (first (:participants ev))) (:appointment-id ev)]))]
      (for [ev ground-truth
            :when (and (#{:admission :outpatient-visit} (:event ev))
                       (:appointment-id ev)
                       (contains? no-showed [(:patient-id (first (:participants ev)))
                                             (:appointment-id ev)]))]
        {:invariant :no-show-has-no-encounter :t (:t ev)
         :patient-id (:patient-id (first (:participants ev)))
         :appointment-id (:appointment-id ev)}))))

(defn appointment-reaches-at-most-one-terminal
  "ADR-0174 section 2(b), invariant 4 -- the one the ADR marks OWED
  because rows 1-3 are each satisfiable by a log where an appointment is
  both cancelled AND kept.

  Kept, cancelled and no-showed are mutually exclusive. In the engine
  they are bands of ONE uniform and so cannot co-occur; this row is what
  says that over a log the engine did not necessarily write.

  A `:reschedule` is deliberately NOT counted: it moves an appointment
  and leaves it open, which is why it keeps its own id."
  [ground-truth]
  (when (scheduling-log? ground-truth)
    (let [kept (frequencies (for [ev ground-truth
                                  :when (and (#{:admission :outpatient-visit} (:event ev))
                                             (:appointment-id ev))]
                              [(:patient-id (first (:participants ev))) (:appointment-id ev)]))
          closed (reduce (fn [m ev]
                           (if (#{:appointment-cancel :no-show} (:event ev))
                             (update m [(:patient-id (first (:participants ev)))
                                        (:appointment-id ev)]
                                     (fnil conj []) (:event ev))
                             m))
                         {} ground-truth)]
      (for [[k terminals] (merge-with into
                                      (into {} (for [[k n] kept] [k (vec (repeat n :kept))]))
                                      closed)
            :when (> (count terminals) 1)]
        {:invariant :appointment-reaches-at-most-one-terminal
         :patient-id (first k) :appointment-id (second k)
         :terminals terminals}))))

(def catalog
  "The full invariant catalog needing only a ground-truth log, in
  reporting order."
  [#'timestamps-monotone
   #'discharge-follows-admission
   #'every-event-has-participants
   #'participant-ids-exist-in-run
   #'admission-only-when-no-open-encounter
   ;; ADR-0174 section 2(a) (arc 3b sweep 1): the two encounter rows,
   ;; registered beside the guard they split from rather than appended
   ;; at the end -- the catalog is documented as being in REPORTING
   ;; order, so a reader comparing the three should find them adjacent
   ;; (the same placement argument ADR-0166's twin span made).
   #'discharge-closes-an-open-encounter
   #'every-encounter-is-opened-and-closed-or-still-open
   #'transfer-only-when-admitted
   #'transfer-from-matches-state
   #'no-double-occupancy
   #'admitted-occupies-one-slot
   #'cancel-references-existing-uncancelled-event
   #'bed-swap-both-admitted-before-swap
   #'merge-survivor-absorbs-merged-mrns
   #'no-events-after-merged-terminal
   #'step-rejected-reason-is-documented
   #'order-only-when-admitted
   #'result-references-existing-order-and-follows-it-in-time
   #'abnormal-flags-consistent-with-value-vs-range
   #'registered-is-every-patients-first-event
   #'registered-persona-is-schema-valid
   #'outpatient-patients-occupy-no-bed
   #'clinical-content-only-when-admitted
   #'medication-end-references-existing-order-and-follows-it-in-time
   ;; ADR-0166: the twin span. Added beside its mirror, not appended at
   ;; the end, because a reader comparing the two should find them
   ;; adjacent -- the reason the catalog is documented as being in
   ;; reporting order.
   #'care-plan-end-references-existing-start-and-follows-it-in-time
   #'expired-patient-retains-location
   ;; ADR-0173 section 2(e) (arc 3a part 3): the person-fold family, six,
   ;; registered together and in the order the ADR tables them. Three
   ;; are over part 4's identification flow and fire on nothing this arc
   ;; produces -- they land now because a gate written after its
   ;; producer is a gate written to agree with it.
   #'identity-fill-references-its-placeholder-registration
   #'identification-merge-survivor-is-the-persons-prior-patient
   #'every-placeholder-registration-is-resolved-or-still-open
   ;; TS-4 (2026-08-29, `roadmap.md#ts-4-placeholder-unresolved`): the
   ;; consumed clause's other half, placed beside the invariant it makes
   ;; safe rather than appended at the end, for the reason this catalog
   ;; is documented as being in reporting order.
   #'no-resolution-after-a-placeholder-is-consumed
   #'demographic-update-reports-a-real-change
   #'no-demographic-event-after-a-patient-expires
   #'person-scoped-provenance-is-a-stamp-not-a-reference
   ;; ARC 3B SWEEP 2 (ADR-0174 section 2(c)): the bed cycle's own three.
   ;; All three are VACUOUS on a log with no `:bed-status-change` --
   ;; `bed-cycle-log?`'s own docstring says why, and says it where a
   ;; reader counting the catalog will look.
   #'no-assignment-to-a-non-ready-bed
   #'every-ready-follows-a-cleaning
   #'bed-cycle-transitions-are-legal
   ;; ARC 3B SWEEP 3 (ADR-0174 section 2(b)): scheduling's own FOUR --
   ;; three the ADR tables plus the one it marks OWED, because rows 1-3
   ;; are each satisfiable by a log where an appointment is both
   ;; cancelled and kept. All four are VACUOUS on a log with no
   ;; `:appointment` -- `scheduling-log?` says so where a reader counting
   ;; the catalog will look.
   #'appointment-reference-resolves
   #'scheduled-encounter-follows-its-appointment
   #'no-show-has-no-encounter
   #'appointment-reaches-at-most-one-terminal])

(def facility-catalog
  "Invariants that need the facility config, not just the log (checked
  separately from `catalog` because their function signature differs
  -- `check-all` runs both)."
  [#'occupancy-within-capacity
   #'surge-only-when-earlier-rungs-exhausted])

(def warmup-catalog
  "Invariants that need the run's configured warm-up window (sim/ADR-0011),
  not just the log -- same reason `facility-catalog` is separate."
  [#'warm-up-mark-matches-window])

(def order-profiles-catalog
  "Invariants that need the order-profiles config, not just the log --
  same reason `facility-catalog` is separate (Milestone M3)."
  [#'result-analytes-match-order-profile])

(defn check-all
  "Runs every invariant in the catalog over a ground-truth log.
  `facility-config` (default sim-model/default-facility) is needed by the
  capacity/surge-ladder invariants; `warm-up-seconds` (default 0) is
  needed by the warm-up-mark invariant; `order-profiles-config`
  (default ehrt.sim-engine.order-profiles/default-profiles, Milestone
  M3) is needed by result-analytes-match-order-profile. Existing
  1-arg/2-arg/3-arg call sites are unaffected."
  ([ground-truth] (check-all ground-truth sim-model/default-facility 0 order-profiles/default-profiles))
  ([ground-truth facility-config] (check-all ground-truth facility-config 0 order-profiles/default-profiles))
  ([ground-truth facility-config warm-up-seconds]
   (check-all ground-truth facility-config warm-up-seconds order-profiles/default-profiles))
  ([ground-truth facility-config warm-up-seconds order-profiles-config]
   (let [base-violations (into [] (mapcat #(% ground-truth)) catalog)
         facility-violations (into [] (mapcat #(% ground-truth facility-config)) facility-catalog)
         warmup-violations (into [] (mapcat #(% ground-truth warm-up-seconds)) warmup-catalog)
         order-profiles-violations (into [] (mapcat #(% ground-truth order-profiles-config)) order-profiles-catalog)
         violations (-> base-violations
                        (into facility-violations)
                        (into warmup-violations)
                        (into order-profiles-violations))]
     (if (empty? violations)
       (result/ok {:invariants-checked (-> (mapv (comp :name meta) catalog)
                                            (into (mapv (comp :name meta) facility-catalog))
                                            (into (mapv (comp :name meta) warmup-catalog))
                                            (into (mapv (comp :name meta) order-profiles-catalog)))
                   :events (count ground-truth)})
       (result/rejected :invariant-violation {:violations violations})))))
