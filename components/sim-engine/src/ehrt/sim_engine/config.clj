(ns ehrt.sim-engine.config
  "`run`'s config surface: `config-keys` -- the canonical, documented
  list of every key `run` accepts -- and the two opt-in value schemas
  `:persons` and `:scheduling` carry, with the guard predicate each of
  them owes. `engine.clj`'s SEVENTH extraction under
  `roadmap.md#engine-namespace-extraction-and-apply-unification`, landed
  in the same session as `ehrt.sim-engine.assignment`: the census's own
  dependency order (`.agents/plans/engine-extraction-census.md` section
  3a) puts both in the LEAF rank alongside `streams` and `state`, and
  the six extractions before this one already took everything
  downstream, so neither depends on the other and both were free.

  A CODE-LEVEL LEAF. Not one of these five forms calls anything
  `engine.clj` defines: `config-keys` is a keyword vector,
  `Persons`/`Scheduling` are malli schemas, and the two predicates
  validate against those schemas. The only names crossing out of this
  namespace are `malli.core/validate` and `sim-model/Persona`, both of
  which `engine.clj` already required, so this move added no dependency
  to the brick.

  Extracted OUTPUT-IDENTICAL: every form below is `engine.clj`'s own
  text, moved and not rewritten -- including every one of `config-keys`'
  per-key comment paragraphs, which are what
  `docs/consuming-ground-truth.md` cites this list for and the reason
  that page's two references were repointed HERE by the move commit
  rather than left pointing at a delegating def that cannot carry them.
  Like `fold`, and unlike the four extractions between them, this
  cluster has no interior comment block for a banner to have to travel
  with -- five forms, contiguous, nothing between them but blank lines.

  ALL FIVE VARS WERE PUBLIC in `engine.clj` and all five keep a
  delegating def there under ruling C1(a). Two of those defs are
  load-bearing for `ehrt.sim-engine.interface`, which census constraint
  4 requires to keep naming `engine/...`: `config-keys` at
  `interface.clj:46` and `valid-persons?` at `:82`.
  `valid-scheduling?`'s def is owed instead to `scheduling_test.clj`,
  which C1(a) forbids this session to touch. `Persons` and `Scheduling`
  have no caller anywhere -- the first movers of this program for which
  that is true -- and keep defs anyway, because C1(a) says moved PUBLIC
  vars get them and narrowing that to \"public vars someone calls\"
  would be inventing an exception the ruling does not grant."
  (:require [ehrt.sim-model.interface :as sim-model]
            [malli.core :as m]))

(def config-keys
  "The canonical, documented list of every key `run`'s config map
  accepts (this def IS the documentation the M4 Task 0 plumbing-
  completeness test checks against -- a new key earns an entry here in
  the SAME change that teaches `run` to read it, never after).
  `ehrt.sim.run/run-command` must forward every one of these
  from its own opts through to `run` -- its own completeness test
  asserts the full set, not just today's known gaps, so a future key
  added here without a matching `run-command` forwarding update fails
  loudly instead of shipping CLI-invisible the way M3's `:pathways` did
  (caught only by the tools consumer loop, after the fact)."
  [:seed :patients :pathway :pathways :arrival-gap :warm-up-seconds
   :facility :providers :churn-profile :order-profiles :persona-config
   :modules :module-assignment :module-horizon-days :history :persons
   ;; ARC 3B SWEEP 1 (ADR-0174 ruling A1/E1): the encounter horizon's
   ;; own opt-in. Truthy lifts the single-encounter wall and mints an
   ;; `:encounter-id` at every opener; ABSENT ENTIRELY -- not false, not
   ;; nil -- is the byte-identical path, the same opt-in law `:persons`,
   ;; `:pathways`, `:churn-profile` and `:module-assignment` already
   ;; establish.
   :encounters
   ;; ARC 3B SWEEP 2 (ADR-0174 section 2(c), rulings C/D1/E1): the BED
   ;; CYCLE's own opt-in. Truthy builds the `:beds` index, gates
   ;; `allocate` on `:ready`, emits `:bed-status-change` and moves the
   ;; bed-ready transfer from the discharge instant to the READY instant;
   ;; ABSENT ENTIRELY is the byte-identical path, the same opt-in law
   ;; `:encounters` and `:persons` establish.
   :bed-cycle
   ;; ARC 3B SWEEP 3 (ADR-0174 section 2(b), rulings C/E1): SCHEDULING's
   ;; own opt-in. Present splits arrivals scheduled-vs-walk-in, mints
   ;; appointments as skeleton state, and schedules follow-up visits at
   ;; discharge; ABSENT ENTIRELY is the byte-identical path, the same
   ;; opt-in law `:bed-cycle`, `:encounters` and `:persons` establish.
   ;;
   ;; `R-mix-7` says a mix RATIO reshuffles nothing. This is NOT a mix
   ;; ratio -- it is a fact generator, so it DRAWS, and the reshuffle it
   ;; causes belongs entirely to the turn-on commit.
   :scheduling])

(def Persons
  "`run`'s ENGINE-FACING `:persons` value (ADR-0173 section 2(a), arc 3a
  part 3). Not the config-facing one: `ehrt.sim.run`'s own `:persons` is
  the small authored map the ADR tables (`{:count :years :identification
  :unhoused}`), and that namespace translates it into this -- exactly
  the two-layer treatment `:modules` already has, where the config side
  is names and the engine side is already-loaded closures. This
  namespace does no I/O and calls nothing outside itself; person events
  arrive as DATA.

    :population  [{:person-id .. :id-tag ..} ...] -- the POOL, in a
                 fixed order. Carried explicitly rather than derived
                 from `:events`, because a person with no events at all
                 in the horizon is still a person who can walk into an
                 ED, and deriving the pool from the stream would make
                 them unselectable.
    :personas    person-id -> that person's own t0 Persona. The seam the
                 `:patient` family loses (`compile-patient`'s 4-arity).
    :alive       person-id -> that person's own death instant. A1's
                 arrival-candidate filter's whole input. It is DATA and
                 not something derived from `:events`, and the reason is
                 ruling C1's ordering: a person BOUND to an arrival gets
                 the compiled trajectory's death instead of their own
                 drawn one, so the stream handed here has already had
                 those `:person-death` events removed. Filtering on the
                 stream would therefore filter on a fact the binding
                 itself produced. `ehrt.sim.run`'s own docstring carries
                 the two-pass resolution this key exists for.
    :events      the t-ascending person-event vector, verbatim.

  ABSENT ENTIRELY -- not nil, not an empty population -- is the
  byte-identical path, the same opt-in law `:pathways`,
  `:churn-profile` and `:module-assignment` already establish."
  [:map
   [:population [:vector [:map [:person-id :string] [:id-tag :int]]]]
   [:personas [:map-of :string sim-model/Persona]]
   [:alive [:map-of :string :int]]
   [:events [:vector [:map [:event :keyword] [:t :int] [:person-id :string]]]]])

(def Scheduling
  "`run`'s `:scheduling` value (ADR-0174 section 2(b), arc 3b sweep 3) --
  the six sub-keys the ADR names, and nothing else.

    :scheduled-fraction  P(an arrival was BOOKED rather than walked in).
                         `:world`, pre-loop, one Bernoulli per arrival
                         ordinal.
    :lead-time-days      [lo hi], the range an appointment is booked
                         AHEAD of its own visit. Drawn on `:world` in the
                         same pre-loop pass, and RE-USED as the range a
                         reschedule moves an appointment by, so a
                         reschedule needs no seventh key of its own.
    :no-show-rate        P(booked, never arrived). Emitted AT
                         `:scheduled-t`; opens nothing.
    :reschedule-rate     P(moved once before being kept). NOT terminal.
    :cancel-rate         P(cancelled before the visit). Terminal.
    :follow-up           {:rate p :interval-days [lo hi]} -- the return
                         visit booked at `decide :discharge`, and the
                         FIRST producer of a SCHEDULED second encounter
                         this repository has had.

  THE THREE OUTCOME RATES ARE BANDS OF ONE UNIFORM, not three
  independent Bernoullis, and that is what makes
  `appointment-reaches-at-most-one-terminal` true in the STATE rather
  than merely asserted over the log: cancelled, no-showed and kept
  cannot co-occur because one draw cannot land in two bands. They must
  therefore SUM TO AT MOST 1 -- `valid-scheduling?` enforces it, because
  a config whose rates sum past 1 would silently starve the last band.

  ABSENT ENTIRELY -- not nil, not a map of zeroes -- is the
  byte-identical path."
  [:map
   [:scheduled-fraction [:and number? [:>= 0] [:<= 1]]]
   [:lead-time-days [:tuple :int :int]]
   [:no-show-rate [:and number? [:>= 0] [:<= 1]]]
   [:reschedule-rate [:and number? [:>= 0] [:<= 1]]]
   [:cancel-rate [:and number? [:>= 0] [:<= 1]]]
   [:follow-up [:map
                [:rate [:and number? [:>= 0] [:<= 1]]]
                [:interval-days [:tuple :int :int]]]]])

(defn valid-scheduling?
  "Whether `run`'s `:scheduling` value is well-formed. Result-not-throw:
  `run` returns `result/error :invalid-scheduling` rather than blowing up
  inside the pre-loop, the same guard-clause-at-entry shape
  `:invalid-persons` and `:invalid-seed` (sim/ADR-0116 R9) already have.

  The band-sum check is HERE and not in the malli schema because malli
  cannot express a constraint ACROSS three sibling keys without a custom
  predicate, and a custom predicate in the schema would not survive the
  EDN export the way the per-key ranges do."
  [scheduling]
  (and (m/validate Scheduling scheduling)
       (<= (+ (:no-show-rate scheduling)
              (:reschedule-rate scheduling)
              (:cancel-rate scheduling))
           1.0)))

(defn valid-persons?
  "Whether `run`'s engine-facing `:persons` value is well-formed. Result-
  not-throw: `run` returns `result/error :invalid-persons` rather than
  blowing up inside the pre-loop, the same guard-clause-at-entry shape
  `:invalid-seed` already has (sim/ADR-0116 R9)."
  [persons]
  (m/validate Persons persons))
