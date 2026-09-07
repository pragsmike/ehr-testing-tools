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
    FOURTEEN of fourteen, the ruled end state;
  * site 2, `replay` below -- `replay-projection`, twelve of fourteen;
  * site 3, `ehrt.sim-engine.log-index/reinstated-state`'s fallback --
    `reinstated-projection`, THIRTEEN of fourteen and its OWN literal
    since stage 2's de-alias commit.

  THE FOURTEENTH IS ADR-0180'S, not the apply-unification arc's:
  `:boarder-index`, site 1 of the generate-quadratic program, added
  2026-09-06. Every count above that used to read thirteen has moved by
  exactly one, and the two counts that did NOT move are the statement --
  sites 2 and 3 do not opt in, under the charter's own contract that an
  index is guarded by its projection membership and by nothing else, so
  a site that never reads one pays nothing for it.

  THOSE THREE COUNTS WERE STALE FROM STAGE 2 AND ARE CORRECTED HERE
  rather than left standing: they read 11/3/by-value, which was the
  stage-1 tree, while stage 2 had already enabled nineteen pairs and
  de-aliased site 3. Disclosed in this session's record, not absorbed.

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

(defn boarder-entry
  "A patient's BOARDER MEMBERSHIP, computed from ONE patient state and
  nothing else: `[home-ward [admitted-at patient-id]]` when that state
  is a boarder, nil when it is not.

  The predicate is `ehrt.sim-engine.decide/waiting-boarder`'s own
  filter, term for term -- `:admitted`, a non-nil location ward, and a
  location ward that is not the home ward -- and the key is that
  function's own `sort-by` key, `[(:admitted-at p) pid]`. That key is a
  TOTAL order because of the `patient-id` tiebreak, and the totality is
  what makes an index legal here at all: it is why a from-scratch scan
  of an UNORDERED `:patients` map and an ordered set agree by
  construction rather than by luck. An index keyed on `:admitted-at`
  alone would be a different function that happens to agree on logs
  with no simultaneous admissions (ADR-0180 site 1's equivalence
  argument, written before this session rather than asserted after it).

  ADR-0180's R-membership-from-post-state: membership is RECOMPUTED
  from a patient state, never derived from which event just happened.
  There is no add/remove logic per event kind here, so there is no
  eviction case to enumerate and none to miss."
  [pid p]
  (let [located (get-in p [:location :ward])]
    (when (and (= :admitted (:status p))
               (some? located)
               (not= (:home-ward p) located))
      [(:home-ward p) [(:admitted-at p) pid]])))

(defn update-boarders
  "The boarder index, folded one event forward (ADR-0180 site 1):
  `home-ward -> sorted-set of [admitted-at patient-id]`.

  ONE rule and no others: for each of this event's patient
  participants, take `boarder-entry` of its PRE-event state and of its
  POST-event state and reconcile the two. A membership that did not
  change writes nothing; one that did retires the old key and files the
  new. THE OLD KEY COMES FROM THE PRE-STATE and not from the set,
  because `:home-ward` and `:admitted-at` can both move under one
  event, and a key rebuilt from the post-state would strand the entry
  it was supposed to retire.

  Only participants are considered, and that is soundness rather than
  an optimisation: `evolve` folds an event into exactly its
  participants' states (sim/ADR-0010), so no other patient's
  `:status`, `:location`, `:home-ward` or `:admitted-at` can have moved
  under it. That is the same pre/post pair, and the same argument,
  `update-beds` above already runs on."
  [index patients-before patients-after participants]
  (reduce (fn [idx {:keys [patient-id]}]
            (let [before (boarder-entry patient-id (get patients-before patient-id))
                  after (boarder-entry patient-id (get patients-after patient-id))]
              (if (= before after)
                idx
                (cond-> idx
                  before (update (first before) (fnil disj (sorted-set)) (second before))
                  after (update (first after) (fnil conj (sorted-set)) (second after))))))
          (or index {})
          participants))

(defn first-boarder
  "THE INDEX'S ANSWER to `decide`'s `waiting-boarder` question: the
  longest-waiting boarder of `ward-name`, excluding `excluded-id`, or
  nil when the ward has none.

  IDENTITY of this id is the obligation, nil-vs-non-nil included -- not
  \"a legal boarder\", which is a strictly weaker claim an index can
  satisfy while moving every byte after it. A stale entry an index
  failed to evict emits a `:transfer` where the scan emitted nothing,
  and a missing entry drops one; both reshuffle everything downstream,
  because a non-nil answer takes a branch that draws twice (ADR-0180
  site 1: `bed-ready-location` on the world stream and `vacate-bed` on
  the facility stream).

  The exclusion stays a CALLER-SIDE filter over the ordered answer
  rather than a second index, for the reason the same section gives:
  the two callers pass different worlds and different exclusions, and
  both worlds have the same `:patients`, so one carrier serves both. At
  most one entry can carry `excluded-id`, so this reads at most two."
  [world excluded-id ward-name]
  (some (fn [[_ pid]] (when-not (= pid excluded-id) pid))
        (get (:boarder-index world) ward-name)))

(defn bed-of
  "A patient's BOARD MEMBERSHIP, computed from ONE patient state and
  nothing else: the bed that state names, or nil.

  It is `sim-model/occupancy-board`'s own `keep` body with the
  patient-id half dropped, term for term -- including the ABSENCE of a
  status test, which is the definition's own choice being mirrored
  rather than a simplification an index took on the way past. The board
  keeps every patient whose `:location` names a bed, whatever their
  `:status`, and so does this.

  ADR-0180's R-membership-from-post-state, the same way `boarder-entry`
  above satisfies it: membership is RECOMPUTED from a patient state and
  never derived from which event just happened, so there is no
  add/remove case per event kind to enumerate and none to miss."
  [p]
  (get-in p [:location :bed]))

(defn update-board
  "The occupancy board, folded one event forward (ADR-0180 site 3):
  `bed-id -> patient-id`, whose from-scratch definition is
  `sim-model/occupancy-board` and stays that.

  ONE rule and no others, `update-boarders`' own: for each of this
  event's patient participants, take `bed-of` of its PRE-event state and
  of its POST-event state and reconcile the two. A bed that did not move
  writes nothing; one that did retires the old key and files the new.

  THE RETIREMENT IS GUARDED BY THE VALUE -- `(= patient-id (get idx
  before))` -- and the guard is LOAD-BEARING, not defensive. A
  `:bed-swap`'s two participants exchange beds under ONE event, so
  whichever is reconciled first files its new bed over a key the other
  participant still legitimately holds; unguarded, the second would then
  retire the key the first had just filed and the pair would come out
  one bed short. Guarded, that stale-looking write is corrected by the
  other participant's own `assoc` and the board lands exactly where a
  from-scratch scan of the post-event map lands -- in either participant
  order. The same argument covers a vacate paired with an arrival into
  the bed being vacated.

  Only participants are considered, and that is soundness rather than an
  optimisation, for the reason `update-boarders` above gives: `evolve`
  folds an event into exactly its participants' states (sim/ADR-0010),
  so no other patient's `:location` can have moved under it. That is the
  same pre/post pair, and the same argument, `update-beds` runs on.

  ON A DOUBLE-OCCUPANCY WORLD THE TWO ANSWERS DIVERGE, and this is
  NAMED, NOT FIXED (ADR-0180's R-double-occupancy). `occupancy-board`'s
  `into {}` keeps whichever of two patients sharing a bed comes LAST in
  the `:patients` map's own seq order; this index keeps whichever wrote
  the bed last in EVENT order. `ehrt.sim-check.check`'s
  `no-double-occupancy` convicts any log that reaches that state, so it
  is unreachable in a log the checker passes -- the index is specified
  on no-double-occupancy worlds and the equality law quantifies over
  generated logs. On an input the checker would have convicted anyway it
  answers a hash order's question with an event order's answer, which is
  said here rather than left for a reader to discover."
  [index patients-before patients-after participants]
  (reduce (fn [idx {:keys [patient-id]}]
            (let [before (bed-of (get patients-before patient-id))
                  after (bed-of (get patients-after patient-id))]
              (if (= before after)
                idx
                (cond-> idx
                  (and before (= patient-id (get idx before))) (dissoc before)
                  after (assoc after patient-id)))))
          (or index {})
          participants))

(defn board-without
  "THE INDEX'S ANSWER to `decide`'s SECOND board question: the board as
  it stands with `patient-id` removed from `:patients` --
  `bed-ready-location`'s own `(sim-model/occupancy-board (dissoc
  (:patients world) patient-id))`, which asks which beds are held once
  the patient whose discharge is the occasion is out of the way.

  A SECOND QUERY SHAPE IS WHERE AN INDEX MOST EASILY DIVERGES FROM ITS
  DEFINITION -- ADR-0180 site 3 says exactly that about this call site
  -- so it is written as a MASK over the one index rather than as a
  second index. A patient holds at most one bed (`:location` carries a
  single `:bed`), so at most one entry of the board can name them, and
  dropping their own bed's key when the board agrees they hold it is the
  whole of the removal.

  THE VALUE GUARD IS WHAT MAKES IT EXACT rather than defensive, in the
  same sense `update-board`'s is: where the board names some OTHER
  occupant of `patient-id`'s bed, that occupant is still there after the
  `dissoc`, so leaving the key is what the definition does. The residual
  divergence runs the other way and is R-double-occupancy's: where the
  board names `patient-id` and a second patient shares the bed, the
  definition keeps the bed under that second patient's id and this mask
  drops the key. Unreachable in a log `no-double-occupancy` passes;
  written down because it is not unreachable in one it convicts."
  [world patient-id]
  (let [board (:board world)
        bed (bed-of (get-in world [:patients patient-id]))]
    (if (and bed (= patient-id (get board bed)))
      (dissoc board bed)
      board)))

(defn update-cancel-index
  "The cancel index, folded one event forward (ADR-0180 site 4): the TWO
  MAPS `log-index/last-uncancelled-index`'s two whole-log passes used to
  build ON EVERY CALL, maintained here once per event instead.

  * `:by-patient` -- `[patient-id event-type]` -> the log indices of
    that patient's events of that type, APPENDED IN LOG ORDER, one entry
    per patient participant. It is the scan's `(some #(= patient-id
    (:patient-id %)) (:participants ev))` predicate turned inside out:
    the scan asked every event whether it named a given patient, and
    this files every event under the patients it names. An event with
    more than one patient participant appears in every one of their
    vectors, which is `log-index/events-for-patient`'s own rule.

  * `:cancelled` -- `cancel-type` -> the set of log indices that class
    of cancel has already consumed, read off the event's OWN
    `:cancels-event-id`. That is the very field `decide` writes back
    into the event this fold is folding, which is why the index needs
    nothing but the event to maintain.

  THIS IS AN EVENT-DERIVED INDEX, NOT A STATE-DERIVED ONE, and that is
  its one structural difference from ADR-0180's other three.
  `boarder-entry` and `bed-of` recompute membership from a PATIENT STATE
  under R-membership-from-post-state, because what they index can move
  under an event that does not mention it. Nothing here can: a log index
  is immutable once written and an event's own `:cancels-event-id` never
  changes, so this index only ever GROWS. There is no eviction case at
  all -- none to enumerate, and none to miss.

  AN EVENT WITH NO `:cancels-event-id` KEY CONTRIBUTES NOTHING to
  `:cancelled`, where the scan's `(map :cancels-event-id)` over a
  cancel-type event lacking the key would have contributed `nil` to its
  set. The two agree because that set is only ever asked about an
  integer log index, so a `nil` in it could never change an answer --
  and keying on the KEY'S PRESENCE rather than on a cancel-type
  whitelist is what keeps this concern from carrying a second copy of
  `decide`'s own event vocabulary for the whitelist to drift from."
  [index idx ev participants]
  (let [event-type (:event ev)]
    (cond-> (reduce (fn [i pid]
                      (update-in i [:by-patient [pid event-type]] (fnil conj []) idx))
                    (or index {})
                    (distinct (map :patient-id participants)))
      (contains? ev :cancels-event-id)
      (update-in [:cancelled event-type] (fnil conj #{}) (:cancels-event-id ev)))))

(defn last-uncancelled
  "THE INDEX'S ANSWER to the applicability query the event-validity
  table's cancel-* row asks: the log index of the most recent
  `event-type` event naming `patient-id` that no `cancel-type` event has
  already consumed, or nil when there is none.
  `log-index/last-uncancelled-index` is the reader-facing name and
  carries the definition's own prose; this is the lookup behind it.

  LAST, NOT FIRST, and walking from the END is the whole of the
  difference. The scan took `(last (keep-indexed ...))`, so a patient
  re-admitted after a `:cancel-admit` has TWO `:admission` entries and
  the next cancel must find the SECOND. Reading the vector forwards
  would find the FIRST uncancelled index -- a different function that
  agrees on every patient who was admitted once, which is most of them.

  IDENTITY IS THE OBLIGATION, nil included. ADR-0180 site 4 records that
  this query draws nothing and is not allocation-affecting; that owes it
  no draw-ORDER argument and does not make it cheap to get wrong. A nil
  where the scan returned an integer turns a legal cancel into a
  `:step-rejected`, which changes the event stream and therefore every
  draw after it.

  THE READ IS RAW (`rulings.md#R-raw-read`, site 3's standing choice):
  a world carrying no `:cancel-index` THROWS rather than falling back to
  the scan. A fallback would be a SECOND IMPLEMENTATION of this answer,
  which is exactly the condition ADR-0169's F-3 admitted this site on
  not having -- and here a silent nil is a WRONG answer rather than
  merely a slow one, so failing loudly is the only honest missing-key
  behaviour. `run`'s `init-world` seeds the empty index for the same
  reason it seeds `:board`: `decide` runs BEFORE the batch that would
  open it.

  The scan is not gone, it MOVED: `ehrt.sim-engine.cancel-index-test`
  keeps it verbatim as `naive-last-uncancelled-index`
  (`rulings.md#R-move-not-improve`) and a pinned-seed property asserts
  the two agree at every intermediate world, for every patient and every
  one of the three (event-type, cancel-type) pairs `decide` asks."
  [world patient-id event-type cancel-type]
  (let [index (:cancel-index world)]
    (when (nil? index)
      (throw (ex-info "no :cancel-index on this world -- ADR-0180 site 4 reads the index and never rebuilds the scan"
                      {:patient-id patient-id :event-type event-type
                       :cancel-type cancel-type})))
    (let [cancelled (get (:cancelled index) cancel-type)]
      (some (fn [i] (when-not (contains? cancelled i) i))
            (rseq (get (:by-patient index) [patient-id event-type] []))))))

(def empty-eligible-index
  "THE ELIGIBLE INDEX'S SEED, and it is a `PersistentHashMap` rather
  than `{}` for a reason ADR-0180's addendum measured rather than
  asserted (R-empty-carrier, 2026-09-07).

  `{}` and `(hash-map)` read as `PersistentArrayMap`, which iterates in
  INSERTION order and only becomes a `PersistentHashMap` at its ninth
  entry. `:patients` has been a hash map since t 0 -- `run`'s
  `init-world` builds it with `into {}` over the whole population -- so
  a sub-map grown from `{}` would iterate its first eight members in
  ELIGIBILITY order against a parent iterating in HASH order, and
  `merge-eligible` below owes vector identity including ORDER. That bites
  the FIRST merge of every run, not some exotic corner; measured, five
  minted ids give array-map order `[0 1 2 3 4]` against hash-map order
  `[2 1 4 0 3]`.

  So the carrier is this object, both where `update-eligible` opens one
  and where `run`'s `init-world` seeds one, and the two name the SAME
  var rather than each writing a literal that looks right."
  clojure.lang.PersistentHashMap/EMPTY)

(defn eligible-entry
  "A patient's ELIGIBLE MEMBERSHIP together with the bed-swap view's own
  flag, computed from ONE patient state and nothing else: `true` when
  that state is merge-eligible AND bed-swap-eligible, `false` when it is
  merge-eligible only, nil when it is not a member at all.

  The membership predicate is `decide :merge`'s own `never-mergeable?`
  negated, term for term -- `:new` (never admitted, so no `:admission`
  event exists for `participant-ids-exist-in-run` to find) and `:merged`
  (already merged away) are the two statuses that are never legal merge
  targets. The VALUE is `decide :bed-swap`'s own filter, term for term:
  `:admitted` with a non-nil `:location`.

  ONE SUB-MAP AND TWO VIEWS rather than two sub-maps, and the reason is
  the containment ADR-0180's addendum asks this session to assert rather
  than assume: `:admitted` is not in `#{:new :merged}`, so every
  bed-swap-eligible patient is merge-eligible, and `evolve :merge`'s
  `:merged` arm `dissoc`es `:location` so a merged patient fails the
  bed-swap predicate twice over. The containment is what lets one
  carrier answer both, and one carrier is not an economy -- it means the
  hash-order argument in `merge-eligible` below is made ONCE, for one
  structure, rather than twice for two that could drift apart.

  A nil patient is NOT a member: participants without a `:patient-id`
  (the bed participants) and ids absent from the map both read nil here,
  where an unguarded status test would file them under a nil key as
  `false`. The from-scratch definitions iterate `:patients` and can
  reach no such key.

  ADR-0180's R-membership-from-post-state, the same way `boarder-entry`
  and `bed-of` above satisfy it: membership is RECOMPUTED from a patient
  state, never derived from which event just happened, so there is no
  add/remove case per event kind to enumerate and none to miss."
  [p]
  (when (and (some? p) (not (#{:new :merged} (:status p))))
    (and (= :admitted (:status p)) (some? (:location p)))))

(defn update-eligible
  "The eligible index, folded one event forward (ADR-0180 site 5):
  `patient-id -> bed-swap-eligible?` over the merge-eligible patients.

  ONE rule and no others, `update-boarders`' and `update-board`'s own:
  for each of this event's patient participants, take `eligible-entry`
  of its PRE-event state and of its POST-event state and reconcile the
  two. An entry that did not change writes nothing; one that did either
  retires the key or files the new value.

  THE nil TEST IS NOT A TRUTH TEST, and `cond->` is deliberately not
  used here: `false` is a MEMBER -- merge-eligible, not bed-swap-
  eligible -- and only nil is an absence. That is the one place this
  concern's shape differs from its two neighbours above, whose entries
  are truthy whenever they exist.

  Only participants are considered, and that is soundness rather than an
  optimisation, for the reason both neighbours give: `evolve` folds an
  event into exactly its participants' states (sim/ADR-0010), so no
  other patient's `:status` or `:location` can have moved under it.

  THE SEED IS `empty-eligible-index` AND NOT `{}` -- see that var. A
  caller that hands this function an array-map keeps an array-map, which
  is why `run` seeds the same object rather than a literal."
  [index patients-before patients-after participants]
  (reduce (fn [idx {:keys [patient-id]}]
            (let [before (eligible-entry (get patients-before patient-id))
                  after (eligible-entry (get patients-after patient-id))]
              (if (= before after)
                idx
                (if (nil? after)
                  (dissoc idx patient-id)
                  (assoc idx patient-id after)))))
          (or index empty-eligible-index)
          participants))

(defn merge-eligible
  "THE INDEX'S ANSWER to `decide :merge`'s candidate question: every
  merge-eligible patient except `excluded-id`, as a VECTOR.

  VECTOR IDENTITY IS THE OBLIGATION -- same elements, same count, SAME
  ORDER -- because `streams/uniform-choice` is positional
  (`(nth candidates (.nextInt rng (count candidates)))`,
  `streams.clj:47-49`). The draw SEQUENCE is fixed either way, one
  `.nextInt` per merge step with a non-empty candidate list and no
  `:with`; what the count and the positions decide is the draw's
  RESOLUTION, so a structure answering the same SET in a different order
  would rebind merges and move every byte after them.

  THE ORDER ARGUMENT, and it is the whole of why an index is legal here
  (ADR-0180's R-hash-order, addendum 2026-09-07). `PersistentHashMap`'s
  seq is a depth-first walk of a trie indexed by 5-bit slices of the
  key's `hasheq`, and every node type iterates its slots in ascending
  slot order -- so the relative order of two non-colliding keys is fixed
  by their hash bits alone, independent of which other keys are present
  and of the order they were inserted. A sub-map of `:patients`
  therefore iterates its keys in the same relative order `:patients`
  does, and `(keys sub-map)` equals `(filter member? (keys :patients))`,
  which is the from-scratch scan's answer.

  THE HOLE, named rather than discovered later: keys whose FULL 32-bit
  `hasheq` collides land in a `HashCollisionNode`, whose array is
  ordered by INSERTION -- so at such a node a sub-map filled in
  eligibility order can differ from a parent filled in registration
  order. Patient-ids are one per arrival, and the addendum measures the
  first colliding pair of the shipped seed at 45,000 arrivals: every
  committed cell of the measured decade is collision-free, so both
  bracketed cells and all 38 oracle roots are BLIND to it. The detection
  is `ehrt.sim-engine.eligible-index-test`, which keeps the from-scratch
  definition verbatim and asserts equality at every replay entry, and
  which pins what the two sides actually answer at a hand-built
  collision rather than asserting they agree there.

  Sorting the candidates would close both holes and is NOT licensed
  here: it moves every churn-bearing golden root, so it is a declared
  oracle change with its own roadmap row
  (`roadmap.md#determinism-hash-order-dependence`), never a site
  session's judgment call.

  THE EXCLUSION STAYS A CALLER-SIDE REMOVE over the index's answer
  rather than a second index, the shape `first-boarder` uses and for the
  same reason: an index keyed on the subject would be a different index
  per caller. At most one entry can carry `excluded-id`.

  THE READ IS RAW AND A MISSING INDEX THROWS (R-read-throws, ADR-0180's
  addendum; `last-uncancelled`'s own choice above). nil is not a legal
  answer: an absent index read as an empty candidate list turns every
  legal merge into a `:step-rejected` and moves every draw after it."
  [world excluded-id]
  (let [index (:eligible-index world)]
    (when (nil? index)
      (throw (ex-info "no :eligible-index on this world -- ADR-0180 site 5 reads the index and never rebuilds the scan"
                      {:excluded-id excluded-id :view :merge})))
    (into [] (comp (map key) (remove #(= excluded-id %))) index)))

(defn swap-eligible
  "THE INDEX'S ANSWER to `decide :bed-swap`'s candidate question: every
  patient that is `:admitted` with a `:location`, except `excluded-id`,
  as a VECTOR -- the same carrier `merge-eligible` above reads, filtered
  to the entries whose value is `true`.

  EVERY SENTENCE OF `merge-eligible`'s contract holds here unchanged:
  vector identity including order, the same positional
  `streams/uniform-choice` draw, the same hash-order argument and the
  same collision hole, the same caller-side exclusion, the same throw on
  a missing index. Filtering by value cannot disturb the order argument,
  which is a statement about the key set alone.

  `decide :bed-swap` READS THIS, and has since ADR-0180 site 6
  (2026-09-07). Site 5 built the view and
  `ehrt.sim-engine.eligible-index-test` asserted it equal to that
  method's own inline scan at every replay entry, so the repoint
  inherited a view that was already proven rather than proving one of
  its own; the scan was deleted with no fallback in the same commit.
  This concern now meets R-no-second-path at BOTH views -- the sentence
  that stood here until site 6 disclosed the one place it did not."
  [world excluded-id]
  (let [index (:eligible-index world)]
    (when (nil? index)
      (throw (ex-info "no :eligible-index on this world -- ADR-0180 site 5 reads the index and never rebuilds the scan"
                      {:excluded-id excluded-id :view :bed-swap})))
    (into [] (comp (filter val) (map key) (remove #(= excluded-id %))) index)))

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
  "The FOURTEEN concerns the three apply sites perform between them --
  the apply-unification census's section 1 inventory of THIRTEEN, plus
  `:boarder-index`, which ADR-0180 site 1 added on 2026-09-06 as the
  first of the generate-quadratic program's indexes to ride this fold
  -- as the vocabulary a projection is a subset of. Their grains are not uniform and the census says so:
  `:encounter-stamp`/`:warm-up-mark` are DECORATIONS (a pre-pass over
  the batch, off the world as it stands BEFORE it);
  `:log-mirror`/`:log-accumulator`/`:state-history` are PER-BATCH (a
  post-pass off the world as it stands AFTER it -- which is why
  `:state-history` appends the post-BATCH state, not the post-event
  one, census correction C2); the remaining NINE are per-event,
  `:boarder-index` among them.

  `apply-events` reads this set for nothing: it is the closure the
  three projections below are subsets of, and the population
  `ehrt.sim-engine.apply-projection-test` checks them against.

  ADR-0180'S CONTRACT FOR EVERY INDEX IT ADDS, and the reason this set
  can grow without the two projections below growing with it: each new
  index is guarded by its own projection-membership test and by nothing
  else, which is what keeps a site that does not opt in paying nothing
  for an index it never reads. Site 1 opts into `:boarder-index`; sites
  2 and 3 do not, and a boarder index is in neither a replay entry nor
  a patient state.

  AND `:board` MAKES IT FIFTEEN -- ADR-0180 site 3, 2026-09-06, the
  occupancy board `bed-id -> patient-id` that `sim-model/occupancy-
  board` recomputes from the whole `:patients` map on demand. It rides
  this fold under exactly the contract the paragraph above states, and
  the two sites that decline it decline it for the same two reasons:
  `replay` returns entries and `reinstated-state` returns a patient
  state, and an occupancy board is in neither.

  AND `:cancel-index` MAKES IT SIXTEEN -- ADR-0180 site 4, 2026-09-06,
  the two maps `log-index/last-uncancelled-index` rebuilt from the whole
  log on EVERY cancel decide. It rides this fold under the same contract
  the paragraph above states, and the two sites that decline it decline
  it for the same shape of reason the other two indexes are declined:
  `replay` returns entries and `reinstated-state` returns a patient
  state, and a log index of a patient's cancellable events is in
  neither.

  IT IS ALSO THE FIRST OF ADR-0180'S FOUR THAT READS THE EVENT rather
  than the pre/post patient pair, which puts it with `:citation-index`
  and `:registration-index` structurally even though it is this
  charter's and not the census's. `update-cancel-index`'s own docstring
  says why that makes it the only one of the four with no eviction
  case.

  AND `:eligible-index` MAKES IT SEVENTEEN -- ADR-0180 site 5,
  2026-09-07, the ONE sub-map of `:patients` from which BOTH churn
  candidate views are read: `decide :merge`'s eligible list and `decide
  :bed-swap`'s. It rides this fold under the same contract, and it is
  back on the pre/post patient pair the first three read rather than on
  the event. The two sites that decline it decline it for the same
  reason: `replay` returns entries and `reinstated-state` returns a
  patient state, and a candidate sub-map is in neither.

  IT IS THE FIRST OF THE FIVE WHOSE ANSWER'S ORDER IS LOAD-BEARING.
  `streams/uniform-choice` resolves positionally, so `merge-eligible`
  owes VECTOR identity and not set identity -- which is why that
  function's docstring carries a hash-order argument the other four
  needed no equivalent of."
  #{:encounter-stamp :warm-up-mark :log-ordinal :reinstate-index
    :citation-index :registration-index :patient-bootstrap
    :patient-state :bed-index :boarder-index :board :cancel-index
    :eligible-index :log-mirror :log-accumulator :state-history
    :replay-entries})

(def run-loop-projection
  "Census site 1 -- `ehrt.sim-engine.run`'s in-loop fold. THE FULL
  FOURTEEN, and the first of the three sites to reach the ruled end
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

  `:boarder-index` is the fourteenth and the one pair here that is NOT
  inert: it is the whole point of ADR-0180 site 1, and this is the only
  site that reads it. `decide :discharge` and `decide :bed-ready` ask
  `waiting-boarder` its question against the world this fold returns,
  which is why the carrier can be here at all -- the pre-event and
  post-event patient maps both exist at this point and nowhere later,
  the same sentence the bed index is here under.

  `:board` IS THE FIFTEENTH AND IS NOT INERT EITHER, for the same shape
  of reason (ADR-0180 site 3). Four `decide` methods and
  `log-index/bed-reoccupied-by-someone-else?` ask their occupancy
  question against the world this fold returns, and until site 3 each of
  them rebuilt the whole board from `(:patients world)` to ask it. THIS
  SITE ALONE MUST SEED IT: `decide` runs BEFORE the batch that would
  open the index, and `sim-model/free`'s `(remove board ids)` is not
  defined on a missing one, so `run`'s `init-world` carries `:board {}`
  -- which is the board its own seeded patients actually have, every one
  of them `state/initial-patient` and naming no location.

  `:cancel-index` IS THE SIXTEENTH AND IS NOT INERT EITHER (ADR-0180
  site 4). The three cancel `decide` methods ask
  `log-index/last-uncancelled-index` its question against the world this
  fold returns, and until site 4 each of them walked the WHOLE log TWICE
  to answer it. Same sentence as the two above, with the carrier's own
  inputs changed: the log index and the event both exist at this point
  and nowhere later. THIS SITE SEEDS IT, as it seeds `:board`, and for a
  reason of the same kind: `decide` runs BEFORE the batch that would
  open the index and `last-uncancelled` throws on a missing one
  (`rulings.md#R-raw-read`) rather than answering nil, because nil is
  this query's 'no such event' answer and would be a wrong one.

  `:eligible-index` IS THE SEVENTEENTH AND IS NOT INERT EITHER
  (ADR-0180 site 5). `decide :merge` asks its candidate question against
  the world this fold returns, and until site 5 it rebuilt the whole
  candidate list from `(:patients world)` to ask it -- and scanned the
  WHOLE log a second time beside that, which site 5 deletes rather than
  indexes (R-already-merged). Same sentence as the three above. THIS
  SITE SEEDS IT, as it seeds `:board` and `:cancel-index`, and for the
  same reason: `decide` runs BEFORE the batch that would open the index
  and `merge-eligible` THROWS on a missing one rather than answering an
  empty candidate list, which would be a legal-looking answer that
  rejects every merge. `decide :bed-swap` reads the SECOND view of the
  same sub-map, and has since site 6 (2026-09-07); its own inline scan
  was deleted by that repoint, so neither view has a second
  implementation in `src`.

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
  "Census site 2 -- `replay` below. TWELVE OF FOURTEEN, and twelve of
  the apply-unification arc's own THIRTEEN: three at stage 1, twelve
  once ruling A1(b) added the decoration `:encounter-stamp` to
  the eight INERT pairs stage 2 enabled one commit each in census order.

  THE FOURTEENTH IS ADR-0180'S `:boarder-index` (site 1, 2026-09-06)
  AND THIS SITE DOES NOT OPT IN. `replay` returns ENTRIES; a boarder
  index is not in one, and nothing at this site would read it. The
  charter's contract is that each of its indexes is guarded by its own
  projection membership and by nothing else, precisely so that a site
  which does not opt in pays nothing for an index it never reads -- and
  this site is 50.97% of the check phase (`.agents/plans/2026-09-05-
  performance-measurement/measurements.md:274-281`), which is what makes
  paying nothing the load-bearing half of that sentence rather than a
  formality. It is NOT the arc's thirty-ninth cell and does not touch
  that count: the omission the next paragraphs are about is a different
  one, ruled for a different reason.

  THE FIFTEENTH, `:board` (ADR-0180 site 3, 2026-09-06), IS OPTED IN,
  SINCE SITE 7 (2026-09-07, ruling R-board-in-entry) -- the ONE of
  ADR-0180's five in-fold indexes this site takes, where the paragraph
  above refuses the fourteenth in the words the sixteenth and
  seventeenth are refused in below. The argument did not weaken; the
  READER moved. `ehrt.sim-check.check`'s
  `surge-only-when-earlier-rungs-exhausted` always asked the board's
  question over these entries, and asked it of
  `sim-model/occupancy-board` -- the DEFINITION -- against an entry's
  `:world-before`. That is an `into {}` over EVERY patient in that
  world. It was affordable only because this site's world holds just
  the patients seen so far; once `check-all` can be HANDED `run`'s own
  projection, whose world holds every patient from t 0, the same read
  costs O(all patients) per surge event -- the quadratic site 3 removed
  from `decide`, reappearing in the checker. Carrying the index instead
  costs this site one `update-board` per event, which site 6 measured
  at 0.19%-0.21% of a generate phase, and costs the reader nothing. Two
  consequences ride with it and are stated where a reader will look for
  them: a replay ENTRY now carries a `:board` key, and `replay`'s own
  call site SEEDS `:board {}` in the world it starts from. It is not
  one of the apply-unification arc's thirty-nine cells either.

  NOR THE SIXTEENTH, `:cancel-index` (ADR-0180 site 4, 2026-09-06), and
  the argument is the same one a third time: `replay` returns ENTRIES, a
  log index of a patient's cancellable events is not in one, and nothing
  at this site would read it. `ehrt.sim-check.check` asks no
  last-uncancelled question at all -- the cancel family reaches it as
  events to be checked, never as a query to be answered -- so declining
  costs the check phase nothing and saves it a per-event map update over
  50.97% of its wall. Not one of the arc's thirty-nine cells either.

  NOR THE SEVENTEENTH, `:eligible-index` (ADR-0180 site 5, 2026-09-07),
  and the argument is the same one a fourth time: `replay` returns
  ENTRIES, a sub-map of the merge-eligible patients is not in one, and
  nothing at this site would read it. `ehrt.sim-check.check` asks no
  candidate question at all -- churn reaches it as events to be checked,
  never as a draw to be resolved -- so declining costs the check phase
  nothing. Not one of the arc's thirty-nine cells either.

  Each bullet below names why that pair moved no output -- the cone the
  census's section 3b predicted, as the commit that took it found it,
  except the first, whose cone predicted a MOVE and was refuted by
  measurement:

  * `:encounter-stamp` -- THE PAIR THE CENSUS PREDICTED OUTPUT-MOVING
    HERE, and the prediction is REFUTED BY MEASUREMENT: the re-stamp is
    the IDENTITY on all three of the oracle's encounter-carrying roots
    -- `encounter-horizon` 170 events, `chatter-charges` 477,
    `scheduling` 487 -- with no divergent entry on any of them
    (`.agents/session-records/2026-09-01-apply-unification-stage-2.md`
    section 4b, which measured it directly because the oracle cannot
    see this site at all). TWO INDEPENDENT MECHANISMS make it so and
    either alone would carry it: `encounters/stamp-encounter` guards on
    `contains?` -- 'a key that is there is there', its own docstring --
    so an event stamped inbound at site 1 is left alone; AND the
    decoration reads the world as it stands BEFORE the batch, which
    here is `{:patients {}}`, since `replay` hands the whole log to
    `apply-events` as ONE batch, so an event carrying no id finds no
    patient and is stamped with nothing. BOTH ARE THINGS A LATER
    SESSION COULD CHANGE without knowing this pair rests on them,
    which is why ruling A1(b) co-landed
    `ehrt.sim-engine.apply-restamp-identity-test` with this line rather
    than trusting a measurement to stay true.

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

  * `:state-history` -- duplicates, in a NARROWER shape, what
    `:replay-entries` already returns: `replay`'s own docstring below
    says the entries vector IS state-history derived and generalized
    across patients (sim/ADR-0008). Builds from a nil seed into a fresh
    map, and no consumer asks for a separate `{patient-id -> [state
    ...]}`.

  THE ONE IT DOES NOT GET is the DECORATION `:warm-up-mark`, the other
  of section 3b's two OUTPUT-MOVING predictions and the one measurement
  CONFIRMED. **THAT OMISSION IS PERMANENT** -- ruling A2(b), 2026-09-01
  -- and this is the ONE cell of the arc's thirty-nine that its ruled
  end state does not claim. It is not an unfinished pair:

  * a log DOES NOT CARRY A WARM-UP WINDOW, and this concern needs one;
    only a run configuration has it, which is the whole of section 4c's
    'replay cannot do them';
  * declaring 0 instead is MEASURABLY LOSSY, not merely inelegant --
    on a windowed log it flips `:warm-up true` -> `false` at entry 0,
    2 of 9 entries, first differing byte 425. The log's own marks are
    authoritative and `replay` is RIGHT not to touch them. (Site 3's
    twin survives a declared 0 only because it reads a PATIENT STATE
    and `evolve` never reads `:warm-up` -- a licence this site, which
    returns the entries themselves, does not have.);
  * and THREADING the window would be an API change to the most-called
    function in this arc, at 16 live call sites, so that `replay` could
    re-derive a value the log already carries -- the vacuous shape
    relocated rather than removed.

  So this projection ends at TWELVE of thirteen DELIBERATELY, and that
  count is a statement about what a replay site can honestly do rather
  than a formality it has yet to complete. Measurements and the full
  option set: `.agents/session-records/2026-09-01-apply-unification-
  stage-2.md` section 4d, and census section 3e."
  #{:encounter-stamp :log-ordinal :reinstate-index :citation-index
    :registration-index :patient-bootstrap :patient-state :bed-index
    :log-mirror :log-accumulator :state-history :replay-entries
    ;; ADR-0180 site 7, 2026-09-07 -- the fifteenth, and the reason is
    ;; the docstring's own paragraph above, not this line.
    :board})

(def reinstated-projection
  "Census site 3 -- `ehrt.sim-engine.log-index/reinstated-state`'s
  replay fallback. THREE of the thirteen at stage 1, inherited from site
  2 rather than chosen, and a LITERAL rather than the alias stage 1
  wrote since stage 2's de-alias commit (census correction C5, which
  asked for exactly that commit).

  SITE 3'S READ IS ONE ELEMENT OF ONE KEY -- `(:before (nth entries
  idx))`, a PATIENT STATE and not an event -- which narrows every cone
  below relative to its site-2 twin. Stage 2 enabled its nine INERT
  pairs one commit each in census order, and ruling A1(b) added the
  tenth, the DECORATION `:encounter-stamp`, whose OUTPUT-MOVING
  prediction measurement refuted -- so this projection holds all
  THIRTEEN of the apply-unification arc's own concerns, the ruled end
  state of that arc, which site 1 has held since stage 2's first span:

  * `:encounter-stamp` -- THE ONE PAIR SECTION 3c PREDICTED
    OUTPUT-MOVING HERE, and the prediction is REFUTED BY MEASUREMENT.
    The cone was real as far as it went -- `evolve` does fold
    `:encounter-id` into conditions, observations, medication orders and
    care plans, so a DIFFERENT id at `idx` would reach the `:before` the
    two reinstating cancel decides restore, and from there the emitted
    events -- but it never asked whether a re-stamp PRODUCES a different
    id, and it does not. Measured directly across the same three
    encounter-carrying roots as its site-2 twin, reading this site's own
    `:before` projection: no divergence at any index, including the 59
    `:transfer`/`:discharge` indexes of `encounter-horizon` that a
    reinstating cancel could actually ask about
    (`.agents/session-records/2026-09-01-apply-unification-stage-2.md`
    section 4c). THE TWO MECHANISMS ARE ITS SITE-2 TWIN'S, UNCHANGED --
    `stamp-encounter`'s `contains?` guard and the pre-batch world, which
    here too is `{:patients {}}` -- and they are gated, for this site's
    read as well as site 2's, by
    `ehrt.sim-engine.apply-restamp-identity-test`. The two columns are
    one mechanism seen from two sites and are disposed the same way,
    which is why ruling A1(b) took both.

  * `:warm-up-mark` -- INERT because `:warm-up` is a key on the EVENT
    and `evolve` never reads it (no `warm-up` occurrence in
    `evolve.clj`), so it cannot reach the patient state this site
    reads. THE ONLY PAIR OF THE NINE WHERE THE SITE-2 TWIN IS
    OUTPUT-MOVING and this one is not -- the whole reason the de-alias
    commit had to come first. Its `:warm-up-seconds` parameter has no
    source in a log, so the call site DECLARES 0.

  * `:log-ordinal` -- INERT BY VALUE, exactly as at site 2: the
    fallback's world is `{:patients {}}`, so `(count (:ground-truth
    world))` is `(count nil)` = 0, the same number the disabled branch
    substituted.

  * `:reinstate-index` -- THE PAIR THE ARC EXISTS FOR, and inert
    because the two answers are proven equal. Enabling it here makes the
    fallback's own answer available as a LOOKUP rather than an `nth`
    over a materialised replay -- which is what `run` already does. The
    win is the O(N)-per-cancel cost ADR-0169 measures at 35.3% of the
    generate phase, not an output move; taking that win is a later
    session's, not a pair.

  * `:citation-index` -- not read at this site: the read is a patient
    state, and a citation index is not in one.

  * `:registration-index` -- not read at this site, same reason.

  * `:bed-index` -- inert twice over: `reinstated-state` returns a
    patient state and a bed index is not in one, AND the guard `(:beds
    w-next)` is never truthy from this site's empty world. The
    reinstatement's own bed question is asked separately, against the
    LIVE board -- `bed-reoccupied-by-someone-else?` below says so.

  * `:log-mirror` -- a pure duplicate of a log the fallback is HANDED,
    into a world it does not return, and carrying the same
    `(into nil events)` reversal its site-2 twin's commit recorded.

  * `:log-accumulator` -- the same duplicate in TRANSIENT form, and the
    second and last pair here that needs a SLOT: `conj!` on nil throws,
    so the call site carries `:log (transient [])`. Never persisted,
    never read.

  * `:state-history` -- duplicates, in a narrower shape, what
    `:replay-entries` already returns, and this site reads ONE element
    of that. Builds from a nil seed; no slot.

  * `:boarder-index` -- ADR-0180 site 1, 2026-09-06, and the ONE
    concern of the fourteen this site does not name. Inert twice over
    for the reasons its two neighbours here are: `reinstated-state`
    returns a PATIENT STATE and a boarder index is not in one, and this
    site's world is `{:patients {}}` so there would be nothing in it to
    index. Not opting in is what the charter's own contract asks for --
    an index is guarded by its projection membership and by nothing
    else, so a site that never reads one pays nothing for it.

  * `:board` -- ADR-0180 site 3, 2026-09-06, the fifteenth, and inert
    twice over for the two reasons its site-1 twin above is: a
    `reinstated-state` is a PATIENT STATE and an occupancy board is not
    in one, and this site's world is `{:patients {}}` so there would be
    nothing in it to index. The reinstatement's own bed question is
    asked separately against the LIVE board, which is
    `bed-reoccupied-by-someone-else?` -- and since site 3 that function
    reads the run world's `:board` rather than rebuilding the
    definition, which changes nothing here: the world it reads is
    `run`'s, never this fallback's.

  * `:cancel-index` -- ADR-0180 site 4, 2026-09-06, the sixteenth, and
    inert twice over for the two reasons both twins above are: a
    `reinstated-state` is a PATIENT STATE and a log index is not in one,
    and this site's world is `{:patients {}}` with no `:ground-truth`,
    so `:log-ordinal`'s base is 0 here and an index built over it would
    number a log this site was HANDED rather than the one it is a
    projection of. The cancel decides' own last-uncancelled question is
    asked against `run`'s world, one frame above this fallback, and
    never inside it.

  * `:eligible-index` -- ADR-0180 site 5, 2026-09-07, the seventeenth,
    and inert twice over for the two reasons all three twins above are:
    a `reinstated-state` is a PATIENT STATE and a candidate sub-map is
    not in one, and this site's world is `{:patients {}}` so there would
    be nothing in it to index. The churn decides' own candidate question
    is asked against `run`'s world, one frame above this fallback, and
    never inside it.

  THERE IS NOTHING OF THE UNIFICATION ARC'S OWN THAT IT DOES NOT GET.
  Site 3 names every one of that arc's thirteen, its ruled end state,
  and it was the SECOND of the three sites to reach it -- site 2 keeps
  one measured, permanent omission, `:warm-up-mark`, for a reason that
  does not apply here (`replay-projection` above, and record section
  4d). ADR-0180's fourteenth through seventeenth are a different
  question and are answered above."
  #{:encounter-stamp :warm-up-mark :log-ordinal :reinstate-index
    :citation-index :registration-index :patient-bootstrap
    :patient-state :bed-index :log-mirror :log-accumulator
    :state-history :replay-entries})

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
  its base is derived from `(:world acc)` on entry. Nor do the FIVE
  world-carried indexes, `:bed-index`, `:boarder-index`, `:board`,
  `:cancel-index` and `:eligible-index`: each reads and writes its own
  key of `(:world acc)`, and the last four tolerate that key's ABSENCE
  on entry, which is what makes the run loop's first batch legal against
  a seeded world that carries no index yet (`ehrt.sim-engine.run`'s
  `init-world` seeds every patient `:status :new`, so the index it does
  not carry is the empty one). `:board`, `:cancel-index` and
  `:eligible-index` are nonetheless SEEDED at that call site and
  `:boarder-index` is not, and the asymmetry is a property of the
  READERS rather than of the concerns: `first-boarder` reads a missing
  key as nil, which IS the answer; `sim-model/free` calls the board as a
  predicate and a missing one would throw; and `last-uncancelled`,
  `merge-eligible` and `swap-eligible` throw on a missing one
  DELIBERATELY (`rulings.md#R-raw-read`; ADR-0180's R-read-throws),
  because an empty answer is a LEGAL answer for each of those queries
  and reading a missing index as one would be wrong rather than absent.
  `run` decides its first `:admission` before the batch that would open
  any of them.

  THE ELIGIBLE INDEX'S SEED IS THE ONE THAT ALSO HAS A CLASS. `{}` is a
  `PersistentArrayMap` below nine entries and iterates in insertion
  order, where `merge-eligible`'s answer owes the hash order `:patients`
  has had since t 0 -- so that seed is `empty-eligible-index` and not a
  literal, at `init-world` and here (R-empty-carrier).

  THE SENTENCE ABOVE READ 'the two world-carried indexes' WHILE LISTING
  THREE until this edit -- a count left behind by site 3's own addition.
  It is corrected here rather than carried, because site 4 had to
  rewrite the sentence to name a fourth either way.

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
                                             :world-after (:patients w-next)
                                             ;; ADR-0180 site 7: the
                                             ;; occupancy board as of the
                                             ;; PRE-event world, read off
                                             ;; `w` and not rebuilt, which
                                             ;; is what lets the one
                                             ;; whole-world reader in
                                             ;; `ehrt.sim-check.check` stop
                                             ;; walking `:world-before`.
                                             ;; UNGUARDED, and nil at a
                                             ;; site whose projection does
                                             ;; not carry `:board`: the
                                             ;; entry then says the board
                                             ;; is not known here, which is
                                             ;; true, rather than that it
                                             ;; is empty, which is not.
                                             :board (:board w)}))
                                   entries)]
                    [(cond-> w-next
                       (and (projection :bed-index) (:beds w-next))
                       (assoc :beds (update-beds (:beds w-next) ev
                                                 (:patients w) (:patients w-next)))

                       ;; ADR-0180 site 1: the boarder index, off the
                       ;; SAME pre/post participant pair the bed index
                       ;; above reads, and guarded by its projection
                       ;; membership and by NOTHING else -- the
                       ;; charter's own contract, and the reason sites 2
                       ;; and 3 can decline it and pay nothing.
                       (projection :boarder-index)
                       (assoc :boarder-index
                              (update-boarders (:boarder-index w-next)
                                               (:patients w) (:patients w-next)
                                               participants))

                       ;; ADR-0180 site 3: the occupancy board, off the
                       ;; SAME pre/post participant pair the two indexes
                       ;; above read -- a third map built in this one
                       ;; pass, not a third pass over `:patients`.
                       (projection :board)
                       (assoc :board
                              (update-board (:board w-next)
                                            (:patients w) (:patients w-next)
                                            participants))

                       ;; ADR-0180 site 4: the cancel index, and the one
                       ;; of the four that reads the EVENT rather than
                       ;; the pre/post patient pair -- so it takes `idx`
                       ;; and `ev`, both of which exist here and nowhere
                       ;; later, which is the same sentence
                       ;; `:citation-index` and `:registration-index`
                       ;; are in this fold under.
                       (projection :cancel-index)
                       (assoc :cancel-index
                              (update-cancel-index (:cancel-index w-next)
                                                   idx ev participants))

                       ;; ADR-0180 site 5: the eligible index, back on
                       ;; the SAME pre/post participant pair the first
                       ;; three read after site 4's detour through the
                       ;; event -- one sub-map serving BOTH churn
                       ;; candidate views, `decide :merge`'s (site 5)
                       ;; and `decide :bed-swap`'s (site 6).
                       (projection :eligible-index)
                       (assoc :eligible-index
                              (update-eligible (:eligible-index w-next)
                                               (:patients w) (:patients w-next)
                                               participants)))
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
  three of `full-algebra`'s concerns at stage 1, twelve after stage 2
  and ruling A1(b), and THIRTEEN since ADR-0180 site 7 added `:board`
  -- `replay-projection` is the set and is where that count lives.
  Nothing it folds was added, removed or reordered -- what was a
  hand-written loop is the same fold under the choke point's own
  guards.

  THE WORLD IS SEEDED `{:patients {} :board {}}`, and the second key is
  not decoration. `update-board` seeds itself from nil, so the board is
  correct from the first event either way -- but the entry minted AT
  that first event reads `(:board w)` off the world as it stood BEFORE
  it, which without a seed is nil, and `sim-model/free` (the one thing
  that reads this board) calls it as a PREDICATE: `(remove nil ids)`
  throws. `run`'s own `init-world` carries the same seed for the same
  reason, one frame further out (ADR-0180 site 3)."
  [ground-truth]
  (persistent!
   (:entries (apply-events {:world {:patients {} :board {}}
                            :entries (transient [])
                            ;; `:log-accumulator`'s slot, since stage 2
                            ;; enabled that pair at this site. Never
                            ;; persisted and never read -- `conj!` on
                            ;; nil would throw, which is the whole of
                            ;; why the slot exists.
                            :log (transient [])}
                           ground-truth
                           replay-projection))))
