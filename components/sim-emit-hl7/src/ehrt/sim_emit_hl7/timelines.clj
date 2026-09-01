(ns ehrt.sim-emit-hl7.timelines
  "State-at-instant views over the ground-truth log: the per-patient
  demographic timeline and its state-at-t lookup, the per-patient
  active-MRN timeline and its own, and the encounter-interval census
  the arc-4 planners read.

  Extracted VERBATIM from `emit_hl7.clj`, the THIRD cluster of that
  file's namespace extraction (`.agents/plans/engine-extraction-
  census.md` sections 2 and 2a, `roadmap.md#engine-namespace-extraction-
  and-apply-unification`). It is the THIRD and last LEAF, and as strict
  a one as `registry`: no form here calls anything outside this
  namespace, none calls another form INSIDE it either, and none reaches
  a Java class, so it needs NO `:require` and no `:import` at all. The
  five are five independent folds and lookups over one argument.

  `emit_hl7.clj` remains the namespace every existing requirer resolves
  against (author ruling C1(a)), and this is the first cluster of THIS
  FILE to owe it no delegating def at all, a standing the engine's
  `encounters` cluster already set. Every mover was `defn-`, so there is
  no public var to re-export, and `interface.clj` names none of them. All
  five are also WIDENINGS -- every caller of every one of them stayed
  behind, nineteen call sites across sixteen forms in `er7`, `messages`,
  `planners` and `facade` -- so all five are public here and `defn-` no
  longer, and none gains a delegating def, because widening
  `emit_hl7.clj`'s own public surface is not what C1(a) asks for.

  Two sentences of the moved prose stopped being true at the seam and
  are corrected in the move commit rather than a commit later.
  `demographics-at` said its callers were in THIS namespace; they all
  stayed behind, so it now says the emitter. `encounter-spans` said the
  periodic half was BELOW it; `periodic-chatter` stayed behind too, so
  the word is dropped. Nothing else differs, across 151 form-lines.")

(defn demographics-timeline
  "The demographic state this emitter renders from, derived directly
  from the log's own events (sim/ADR-0012's own precedent: a stage's own
  state is recoverable by scanning the log, no second input needed).
  Computed once per `emit` call and threaded down to every segment
  builder that needs it, so PID enrichment applies uniformly across
  every message type, not just admission. Read ONLY through
  `demographics-at`.

  `{patient-id [[t state] ...]}`, t-ascending, one entry per event that
  MOVED that patient's demographics. `:registered` seeds it; ADR-0173
  section 2(b)'s two kinds fold onto it.

  THE VALUE IS PERSONA-SHAPED, deliberately, and this is the one design
  choice here worth stating. `ehrt.sim-engine.state/Demographics` is the
  ENGINE's state-at-t shape, and it carries a residence SUM where a
  Persona carries an `:address`. This namespace may not depend on
  sim-engine at all (`components/sim-emit-hl7` depends on
  `components/sim-model` and nothing else, AGENTS.md's own dependency
  constraint), and -- more to the point -- a site profile's Z-segment
  templates bind `[:persona ...]` paths against this exact value
  (`context-for-event`), so changing its SHAPE would silently break
  every authored site profile in the field. So the fold writes back into
  a Persona: `:address` is ABSENT, not nil-valued and not sentinel-
  valued, for a patient who has nowhere to live. `pid-segment` renders
  an absent address as an empty PID-11, which is ruling E1 on the wire.

  ARC 3A PART 3 IS WHERE THE FOLD ARRIVED. Before it, this function
  returned `{patient-id persona}` and every `t` answered with the t0
  sample -- the shape ADR-0172 limitations row 6 was written about. That
  row is STRUCK by this change, not repaired, and its gate is deleted:
  a delta folded onto patient state is no longer invisible to a message.

  ARC 3A PART 4 ADDS THE PLACEHOLDER AND ITS FILL. A `:registered`
  carrying `:identity :placeholder` seeds the window's ALIAS NAME and
  nothing else -- no DOB, no sex, no phone, no address -- even though
  the event's own `:persona` says who the patient really is. That gap
  between what ground truth knows and what the wire may claim is the
  whole of the identification flow's point (ADR-0173 section 2(d)), and
  this is the one function that enforces it. The `:identity-fill` then
  RE-SEEDS from the persona the fill carries, so every message after it
  renders the identified patient and every message before it renders
  the John Doe."
  [ground-truth]
  (letfn [(hide-address [state residence]
            (cond-> state
              (and residence (not= :housed (:status residence))) (dissoc :address)))
          (seed [ev]
            (if (= :placeholder (:identity ev))
              ;; PERSONA-SHAPED, with one field in it. `pid-segment`
              ;; renders every absent field empty, so this is a PID
              ;; carrying an MRN and a John Doe name and nothing else.
              {:name (:alias-name ev)}
              (when-let [persona (:persona ev)]
                (hide-address persona (:residence ev)))))
          (fold [state ev]
            (case (:event ev)
              :demographic-update
              (if (= :identity-fill (:cause ev))
                (hide-address (:persona ev) (:residence ev))
                (case (:field ev)
                  :residence (let [address (:address (:value ev))]
                               (if address (assoc state :address address) (dissoc state :address)))
                  :name (assoc state :name (:value ev))
                  :dob (assoc state :dob (:value ev))
                  state))
              :coverage-change (assoc state :payer (:payer ev))
              state))]
    (reduce (fn [acc ev]
              (let [patient-id (:patient-id (first (:participants ev)))]
                (case (:event ev)
                  :registered (assoc acc patient-id [[(:t ev) (seed ev)]])
                  (:demographic-update :coverage-change)
                  (if-let [timeline (get acc patient-id)]
                    (assoc acc patient-id
                           (conj timeline [(:t ev) (fold (second (peek timeline)) ev)]))
                    acc)
                  acc)))
            {}
            ground-truth)))

(defn demographics-at
  "One patient's demographic state AS IT STOOD AT `t` -- the single
  lookup shape every PID-rendering site in the emitter goes through.

  The LAST entry at or before `t`, which is what makes a message render
  the demographics the patient had when the event happened rather than
  the ones they ended the run with. A patient with no `:registered` in
  this log at all -- a hand-built fixture, a sliced log -- answers nil,
  and `pid-segment` falls back to its pre-M4 three-field segment."
  [demographics patient-id t]
  (when-let [timeline (get demographics patient-id)]
    (loop [entries timeline state nil]
      (if-let [[et estate] (first entries)]
        (if (<= et t) (recur (rest entries) estate) state)
        state))))

(defn encounter-spans
  "{encounter-id {:t0 :t1 :opener :opener-index}} -- one entry per
  `:encounter-id` this log carries, each encounter's interval read as
  [its first stamped event, its last stamped event] and nothing else.

  NO STATE MACHINE, deliberately, and for ADR-0175 section 2(a)'s own
  reason: `ehrt.sim-engine.engine/stamp-encounter` mints the id at the
  opener and carries it on every event of that encounter, so grouping
  BY the stamp cannot disagree with reading it, while a second
  admission/discharge fold could. Measured consequence, at seed 202:
  an encounter whose `:discharge` is undone by a `:cancel-discharge`
  and never re-closed carries the stamp for 1,433 more days, and is
  genuinely open for all of them -- a fold keyed on `:discharge` would
  have called it closed and been wrong.

  Empty for every run that did not opt into `:encounters`: nothing
  mints an id, so nothing groups, so the periodic half has no census
  and produces nothing."
  [ground-truth]
  (reduce (fn [acc [i ev]]
            (let [eid (:encounter-id ev)]
              (if (nil? eid)
                acc
                (if-let [span (get acc eid)]
                  (assoc acc eid (assoc span :t1 (max (long (:t1 span)) (long (:t ev)))))
                  (assoc acc eid {:t0 (:t ev) :t1 (:t ev) :opener ev :opener-index i})))))
          {}
          (map-indexed vector ground-truth)))

(defn mrn-timeline
  "{patient-id [[t active-mrn] ...]}, t-ascending, one entry per event
  that MOVED that patient's active MRN -- `demographics-timeline`'s
  shape, for the one field a periodic re-statement cannot read off a
  basis event because it has none. A merge is the only thing that moves
  an MRN today (3 of them at seed 202), and a restatement rendered
  after one must carry the survivor's."
  [ground-truth]
  (reduce (fn [acc ev]
            (let [pid (:patient-id (first (:participants ev)))
                  mrn (:active-mrn ev)]
              (if (and pid mrn)
                (let [tl (get acc pid [])]
                  (if (= mrn (second (peek tl)))
                    acc
                    (assoc acc pid (conj tl [(:t ev) mrn]))))
                acc)))
          {}
          ground-truth))

(defn mrn-at
  "The active MRN as it stood at `t` -- `demographics-at`'s own lookup
  shape over `mrn-timeline`'s output."
  [timeline patient-id t]
  (when-let [entries (get timeline patient-id)]
    (loop [entries entries mrn nil]
      (if-let [[et emrn] (first entries)]
        (if (<= (long et) (long t)) (recur (rest entries) emrn) mrn)
        mrn))))
