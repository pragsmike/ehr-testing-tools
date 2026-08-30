(ns ehrt.sim-engine.encounters
  "The encounter: the opt-in gate that decides whether one may open, the
  two compiled-step sets and the re-bracketing that puts a module's
  encounter behind one arrival, the stamp that carries an encounter's id
  onto its events, and the four lifecycle transitions `evolve` applies
  -- `engine.clj`'s third extraction under
  `roadmap.md#engine-namespace-extraction-and-apply-unification` (the
  census's own dependency order, `.agents/plans/engine-extraction-
  census.md` section 3a: `encounters` lands after `streams` and `state`
  and before `evolve`, which calls four of these).

  Extracted OUTPUT-IDENTICAL: every form below is `engine.clj`'s own
  text, moved and not rewritten -- including the `arc 3b sweep 1` header
  comment block above `open-encounter`, which introduces the fold's own
  forms and is part of the cluster for the same reason `state`'s
  `M6 Task 1` block was: a form-span census enumerates forms, and the
  gap between two forms is invisible to it.

  ALL TEN VARS WERE PRIVATE in `engine.clj` -- seven `defn-` and three
  `def ^:private` -- so under constraint 5 every one becomes public HERE
  and NONE gets a delegating def there. `engine.clj`'s public surface is
  therefore unchanged by construction rather than by inspection, ruling
  C1(a)'s delegating-def obligation being owed only for vars that were
  public. Its call sites qualify `encounters/` instead, and no test file
  changed.

  This namespace's ONLY edge is one call into `ehrt.sim-engine.streams`
  (`open-encounter` -> `next-encounter-ordinal`), taken DIRECTLY rather
  than back through `engine.clj`'s delegating def. It reaches nothing
  else -- not `sim-model`, not malli, not `clojure.*`."
  (:require [ehrt.sim-engine.streams :as streams]))

(defn encounter-openable?
  "Whether a NEW encounter may open on this patient right now -- the
  RUNTIME half of `admission-only-when-no-open-encounter`
  (`check.clj`'s is the same rule asserted over a finished log).

  Opted in (ADR-0174 section 2(a) item 3): legal iff no encounter is
  OPEN, and the patient is not in one of the two absorbing terminals.
  `:merged` and `:expired` stay absorbing, which is
  `no-events-after-merged-terminal` and `expired-patient-retains-
  location` preserved verbatim.

  ABSENT: `(= :new (:status patient))` -- this project's
  single-encounter horizon, the expression that was here before, so a
  run with no `:encounters` key behaves byte-for-byte as it always
  has."
  [world patient]
  (if (:encounter-minting world)
    (and (nil? (:encounter patient))
         (not (#{:merged :expired} (:status patient))))
    (= :new (:status patient))))

(def compiled-encounter-openers
  "The two STEP types that open an encounter. Named here beside
  `encounter-openable?` because `gate-compiled-encounters` (below) is
  the only reader; `check.clj`'s own `encounter-openers` is the same
  two names asked of EVENTS, over a finished log."
  #{:admission :outpatient-visit})

(def compiled-encounter-closers
  "And the two that close one. `:discharge` closes an `:admission`
  and `:outpatient-visit-end` closes an `:outpatient-visit`, but the
  span-finder below takes the FIRST closer of either kind rather than
  the matching one: `compile-trajectory`'s own
  `encounter-end->step` chooses the closer off the opener it is
  closing, so a compiled list cannot interleave them."
  #{:discharge :outpatient-visit-end})

(defn gate-compiled-encounters
  "TS-3 (roadmap.md#ts-3-outpatient-opens-over-an-encounter, ruled
  2026-08-29): re-bracket a COMPILED step list so each encounter it
  carries sits behind ONE `:repeat-arrival` step, opener through
  closer.

  WHY THIS AND NOT A GUARD IN THE OPENER'S OWN DECIDE. Every other
  producer of an encounter already obeys ADR-0174's law -- the whole
  arrival is prepended or none of it is (`decide :repeat-arrival`,
  `decide :appointment`, `decide :person-encounter` each say so in
  their own docstrings). The module-compiled list was the ONE producer
  that never got it: its steps are attached raw by `decide :registered`
  and popped straight into `decide :outpatient-visit`, so nothing ever
  asked `encounter-openable?` of them. Wrapping here puts the EXISTING,
  unchanged guard in charge of the whole span rather than adding a
  second copy of the same question to the two opener decides -- and a
  guard on the opener ALONE would be worse than the defect: the tail
  would still run, its clinical content would be stamped with whatever
  OTHER encounter was open, and its trailing closer would close that
  other encounter while passing every row in `check.clj`'s catalog.
  Measured, at the 2026-08-29 v2 10^5 cell; the session record has the
  step-by-step table.

  WHAT STAYS OUTSIDE THE WRAPPER, and it is the point: everything
  BEFORE the opener, including the compiled `:delay` that parks the
  whole encounter (1,676,160 minutes for TS-3's own patient). The guard
  must be asked at the instant the encounter would open, not at
  registration -- so the delay runs first and the wrapper is decided
  after it.

  `compile-trajectory` emits AT MOST ONE encounter per patient (its
  loop short-circuits on `encounter-closed?`), so the loop below finds
  at most one span today. It is written as a loop anyway rather than as
  a find-the-one, because a step list with two would otherwise leave
  the second unguarded silently, which is the failure this row already
  had once.

  NIL IN, NIL OUT and `[]` in, `[]` out -- `decide :registered`
  attaches `(:steps compiled)` verbatim for a patient with no closure,
  and `compile-patient-is-what-registered-attaches` reads that."
  [steps]
  (if-not (seq steps)
    steps
    (let [v (vec steps)
          n (count v)]
      (loop [i 0 out []]
        (if (>= i n)
          out
          (let [step (nth v i)]
            (if-not (compiled-encounter-openers (:type step))
              (recur (inc i) (conj out step))
              (let [close (first (keep #(when (compiled-encounter-closers (:type (nth v %))) %)
                                       (range (inc i) n)))
                    end (if close (inc close) n)]
                (recur end (conj out {:type :repeat-arrival :steps (subvec v i end)}))))))))))

(def two-encounter-event-types
  "The event kinds naming TWO patients, and therefore two encounters.
  `run`'s stamp skips them: one top-level `:encounter-id` cannot name
  both, and inventing a per-participant vocabulary for it is a
  participant-schema widening ADR-0174 reserves for sweep 2.

  `:bed-swap` carries each side's id inside its own `:swap` entry
  instead. `:merge` is NOT here, deliberately -- its message renders the
  SURVIVOR's PID/PV1 only, and the survivor is its first participant, so
  the ordinary stamp names exactly the encounter the wire shows."
  #{:bed-swap})

(defn stamp-encounter
  "Carry the open encounter's id onto an event of that encounter
  (ADR-0174 section 2(a): \"minted at each encounter opener, carried on
  every event of that encounter\").

  `world` here is the state BEFORE this batch, so the id is the one that
  was open when the event happened: a CLOSER is stamped with the
  encounter it closes, an opener already carries its own minted id and
  is left alone (`contains?`, not `some?` -- a key that is there is
  there), and an event after a discharge -- a pending lab result, a
  medication end at home -- is stamped with nothing, which is correct
  and is why this is a stamp and not a patient-wide field.

  With no `:encounters` opt-in NOTHING mints an id, so this function is
  the identity on every event of every legacy run, by construction."
  [world event]
  (if (or (contains? event :encounter-id) (two-encounter-event-types (:event event)))
    event
    (let [subject (:patient-id (first (:participants event)))
          patient (get-in world [:patients subject])]
      (if-let [id (or (:encounter-id (:encounter patient))
                      ;; A `:cancel-discharge` is decided while the
                      ;; encounter its own `:discharge` CLOSED is closed
                      ;; -- that is what it is undoing -- so the open
                      ;; record cannot name it. It belongs to that
                      ;; encounter all the same, and saying so is what
                      ;; lets `every-encounter-is-opened-and-closed-or-
                      ;; still-open` allow a second `:discharge` for one
                      ;; encounter: a reinstated stay is one encounter
                      ;; closed twice, not two encounters.
                      (when (= :cancel-discharge (:event event))
                        (let [last-enc (peek (:encounters patient))]
                          (when-not (:cancelled last-enc)
                            (:encounter-id last-enc)))))]
        (assoc event :encounter-id id)
        event))))

;; --- ADR-0174 section 2(a) (arc 3b sweep 1): the encounter, folded -------
;;
;; These three are the whole of the encounter's fold. They run on EVERY
;; log, opted in or not: the records cost no emitted byte without an id
;; (nothing renders them -- `sim-emit-fhir` keeps its legacy arm for a
;; patient whose records carry none), and folding them unconditionally
;; is what keeps `admission-only-when-no-open-encounter` a real
;; predicate on a legacy log rather than a vacuously-true one.

(defn open-encounter
  "The OPEN record an encounter opener writes -- id, ordinal, and the
  opener's own instant, and nothing else. The seven projection fields
  stay on `PatientState` and ARE this encounter's placement while it is
  open (`EncounterRecord`'s own docstring)."
  [patient {:keys [t encounter-id appointment-id]}]
  (cond-> {:ordinal (streams/next-encounter-ordinal patient) :admitted-at t}
    encounter-id (assoc :encounter-id encounter-id)
    ;; ARC 3B SWEEP 3 (ADR-0174 section 2(b)): a SCHEDULED opener names
    ;; the appointment it was kept against, and the open record carries
    ;; it too -- the ADR asks for both, because the state half is what a
    ;; consumer reads and the event half is what the invariant judges.
    appointment-id (assoc :appointment-id appointment-id)))

(defn close-encounter
  "Move the open record onto `:encounters`, snapshotting the projection
  off `patient` -- which the caller has ALREADY evolved, so the snapshot
  records the encounter as its closer leaves it (a discharged
  encounter's `:location` is nil, exactly as the discharged patient's
  is). An `:outpatient-visit` sets no `:admitted-at` on the patient at
  all, so the record's own opener instant is what stands."
  [patient]
  (if-let [enc (:encounter patient)]
    (-> patient
        (update :encounters (fnil conj [])
                (merge enc
                       (select-keys patient [:class :home-ward :location :attending
                                             :status :discharged-at])
                       (when-let [at (or (:admitted-at enc) (:admitted-at patient))]
                         {:admitted-at at})))
        (dissoc :encounter))
    patient))

(defn cancel-open-encounter
  "`:cancel-admit` un-does an admission, so its encounter never really
  happened -- but its ORDINAL was spent, and its id may already be on
  the cancelled `:admission` event in the log. The record is therefore
  kept, marked, rather than dropped."
  [patient]
  (if-let [enc (:encounter patient)]
    (-> patient
        (update :encounters (fnil conj []) (assoc enc :cancelled true))
        (dissoc :encounter))
    patient))

(defn reopen-encounter
  "`:cancel-discharge` un-does the close its `:discharge` performed: the
  last CLOSED, non-cancelled record becomes the open one again, keeping
  its own id and ordinal rather than minting a second encounter for one
  stay. Guarded on the shape rather than assumed -- a degenerate or
  hand-built log with no closed record, or whose last one was cancelled,
  keeps what it had."
  [patient]
  (let [encs (:encounters patient)
        last-enc (peek encs)]
    (if (and (nil? (:encounter patient)) (seq encs) (not (:cancelled last-enc)))
      (-> patient
          (assoc :encounter (select-keys last-enc [:encounter-id :ordinal :admitted-at]))
          (assoc :encounters (pop encs)))
      patient)))
