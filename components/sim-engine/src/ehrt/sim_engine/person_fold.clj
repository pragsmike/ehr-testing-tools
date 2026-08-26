(ns ehrt.sim-engine.person-fold
  "ADR-0173 section 2(b), arc 3a part 3: how ONE person's own event
  stream reaches this engine.

  The engine folds a vector of maps and never learns whose they are.
  Person events arrive as DATA -- exactly the layering `:modules`
  already follows (`engine/run`'s own docstring: *\"this namespace does
  no file I/O of its own, `ehrt.sim.run`'s job\"*) -- so nothing here
  requires, or could require, the component that produces them: that
  component depends on `ehrt.sim-engine.interface` for its stream
  partition, and the reverse edge would be a cycle `clojure -M:poly
  check` refuses. ADR-0172 limitations row 10 is the gate.

  TWO THINGS HAPPEN TO A PERSON EVENT, and which one is decided purely
  by its `:t` against the instant its person's patient first arrives:

  * AT OR BEFORE that instant -- it is folded onto the person's t0
    Persona, and what reaches the log is the REGISTRATION: a patient
    registers with the demographics they have on the day they walk in,
    not the ones they had at the run's t0. Nothing is minted for it.
    An `:at-t0` `:residence-loss` is the extreme case of this and the
    reason ADR-0173 section 2(b) tables it as a t0 CONDITION: it lands
    before any arrival, and it is what makes a registration for an
    unhoused person render PID-11 absent from its very first message
    (ruling E1).
  * STRICTLY AFTER it -- it becomes an ordinary queue entry at its own
    `:t` (`engine/run`'s queue is a `sorted-map` keyed `[t seq-no]`
    and `schedule-followup` already inserts at an absolute instant, so
    the loop itself does not change), and mints a `:demographic-update`
    or a `:coverage-change`.

  WHY NOT REPLAY THE PRE-ARRIVAL ONES AS EVENTS. `check.clj`'s
  `registered-is-every-patients-first-event` is structural -- `replay`
  bootstraps a never-yet-seen participant off the FIRST event naming
  them -- so an event for a patient before their own `:registered` is
  not a thing this log can carry. Folding them into the registration is
  the only reading that loses nothing: the same state reaches the wire,
  one message earlier.

  THE VOCABULARY GROWS BY EXACTLY TWO. Fifteen person-event kinds map
  onto two ground-truth kinds; the other eleven mint nothing here
  (`:person-death` most notably -- ADR-0172 limitations row 4, ruling
  C1 of ADR-0173: a death outside care has no HL7v2 trigger this
  emitter writes, and what the fold owes instead is behavioural, that
  a dead person is not in the arrival candidate set). Person events are
  never themselves log events: they carry no `:patient-id` and could
  not satisfy `every-event-has-participants` without inventing a second
  participant vocabulary.")

(def demographic-kinds
  "The person-event kinds with a demographic face. Every other kind --
  `:employment-change`, the `:household-*` family, `:pregnancy`,
  `:person-death`, `:person-registered`, `:identity-unavailable`,
  `:identity-resolution`, `:occupational-injury`, `:delivery` -- mints
  nothing HERE. The last four are arc 3a part 4's (the two clinical
  hooks and the identification flow); the rest are state that exists to
  correlate these, and ADR-0173 section 2(b)'s own table says so."
  #{:residence-move :residence-loss :identity-correction :coverage-change})

(defn demographic-effect
  "What one person event changes about a demographic state, as
  `{:field .. :value ..}` -- nil for a person event with no demographic
  face at all.

  `:field` is a key of `engine/Demographics` verbatim, which is what
  lets the fold be one `assoc` rather than a case per kind. The
  residence SUM is where the two residence kinds meet: a move is
  `:housed` with a places row, a loss is `:unhoused` carrying the row
  it lost, and the return to housing is an ordinary move whose own
  `:prior-address` is absent (arc 3a part 1's shape, unchanged here)."
  [ev]
  (case (:event ev)
    :residence-move {:field :residence
                     :value {:status :housed :address (:address ev)}}
    :residence-loss {:field :residence
                     :value (cond-> {:status :unhoused}
                              (:prior-address ev)
                              (assoc :last-known-address (:prior-address ev)))}
    :identity-correction {:field (:field ev) :value (:value ev)}
    :coverage-change {:field :payer :value (:payer ev)}
    nil))

(defn state-at
  "One person's demographic state at instant `t`: their t0 Persona,
  Demographics-shaped, with every one of their OWN events at or before
  `t` folded on. `events` must already be t-ascending -- `persons`
  returns them that way and the engine never re-sorts them.

  Deliberately NOT `engine/demographics-from-persona` plus a reduce
  over that: this namespace is below `engine` in the require graph
  (`engine` reads it, not the other way round), so the seed is built
  here and `demographics-from-persona`'s own docstring names this as
  the one other constructor of the same shape."
  [persona events t]
  (reduce (fn [st ev]
            (if-let [{:keys [field value]} (demographic-effect ev)]
              (assoc st field value)
              st))
          {:name (:name persona)
           :sex (:sex persona)
           :dob (:dob persona)
           :phone (:phone persona)
           :ssn (:ssn persona)
           :payer (:payer persona)
           :residence {:status :housed :address (:address persona)}
           :identity :known}
          (take-while #(<= (:t %) t) events)))

(defn registration
  "What a bound person brings to their own first arrival at `t`:
  `{:persona <Persona> :residence <the residence sum>}`.

  The Persona is the t0 one with the folded fields written back --
  `:name`, `:dob` and `:payer` -- so `registered-persona-is-schema-
  valid` keeps asserting exactly what it asserts today, and so the
  fourteen t0-only census sites keep reading a Persona.

  `:address` is the one field that CANNOT round-trip, because
  `sim-model/Persona`'s own `:address` is required and non-nilable and
  widening it would move every `:registered` event in every corpus for
  a fact that belongs to state-at-t (ADR-0173 section 2(b)). So an
  unhoused person's Persona carries the row they LAST lived at, and the
  `:residence` sum beside it is what says they no longer live there.
  The emitter reads the sum, not the Persona, for PID-11."
  [persona events t]
  (let [st (state-at persona events t)
        residence (:residence st)]
    {:persona (assoc persona
                     :name (:name st)
                     :dob (:dob st)
                     :payer (:payer st)
                     :address (or (:address residence)
                                  (:last-known-address residence)
                                  (:address persona)))
     :residence residence}))

(defn wire-step
  "The engine STEP one person event mints, or nil. Engine-internal, never
  authorable pathway IR -- the same treatment `:registered` and
  `:result-followup` already get, so no `sim-model/Step` schema entry
  exists for either type and neither passes `sim-model/valid?`.

  `:t` rides the step because these steps are QUEUE-SEEDED at an
  absolute instant rather than reached by advancing through a pathway;
  `run` reads it to place the entry and `decide` never looks at it (it
  is handed the queue's own `t`).

  `:person-event-id` is a PROVENANCE STAMP, not a log reference --
  section 2(e) invariant 6 is the gate that keeps it one."
  [ev]
  (when-let [{:keys [field value]} (demographic-effect ev)]
    (if (= :coverage-change (:event ev))
      {:type :coverage-change :t (:t ev) :cause (:cause ev)
       :payer value :person-event-id (:event-id ev)}
      {:type :demographic-update :t (:t ev) :cause (:event ev)
       :field field :value value :person-event-id (:event-id ev)})))

(defn steps-after
  "Every step one person's events mint STRICTLY after `t`, in the order
  the events already stand in."
  [events t]
  (into [] (comp (filter #(> (:t %) t)) (keep wire-step)) events))

(defn deaths
  "person-id -> that person's own death instant, read off the stream's
  `:person-death` events. This is A1's arrival-candidate filter's whole
  input, and it is DATA the caller supplies rather than something the
  engine derives from the stream it is handed -- see `engine/run`'s
  `:persons` docstring for why the two are not the same stream."
  [events]
  (into {} (for [ev events :when (= :person-death (:event ev))]
             [(:person-id ev) (:t ev)])))
