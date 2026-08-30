(ns ehrt.sim-engine.engine
  "The discrete-event simulation core: a priority queue of pending
  events ordered by simulated time, a world of patient states (plus,
  from Milestone M1 on, the static facility/provider config decide
  needs to read), and the decide/evolve pair (`sim/ADR-0008`) that replaces
  a single fused transition function. Architecture mined from Google's
  Simulated Hospital (pkg/state WrappedQueue + pkg/hospital
  RunNextEventIfDue).

  Event-sourcing doctrine (`sim/ADR-0008`): the ground-truth log is the only
  primitive. `decide` (streams, t, world, patient-id, step) -> {:events
  :advance} consults the current world (every patient's state so far,
  plus facility/provider config -- read-only) and the run's seeded RNG
  streams (ADR-0171: one per patient, plus two run-scoped)
  to decide what happens, but never returns a new state -- this is
  where cross-patient coupling lives (a discharge's decide call may
  also emit a transfer event for a DIFFERENT patient, the bed-ready
  transfer for a boarding patient, docs/operational-models.md).
  `evolve` (patient-state, event) -> patient-state' is pure, total, and
  narrower: no RNG, no knowledge of world or of any patient but the one
  the event names. The ONLY path by which patient state changes is
  folding emitted events through `evolve`; docs/patient-state-model.md
  is PatientState's design spec, docs/sim-theory.md open question #3
  (state-history is derived, not primitive) is this ADR's resolution.

  Identity doctrine (`sim/ADR-0010`, M2a): `:patient-id` -- not `:mrn` -- is
  the fold key and the work-queue key. `:mrn` moves into state as
  {:mrns #{...} :active-mrn ...}, because a real hospital's MRN is
  exactly the identifier merge (M2b) changes; patient-id never
  reassigns and never rebinds. Every event carries `:participants`, a
  vector of {:patient-id :role} -- single-element with role :subject
  for every event type this project has today (the degenerate case);
  a patient's state folds exactly the events they participate in.
  `patient-id-for` is a PURE function of this run's seed and a
  patient's arrival ordinal -- deliberately off the seeded RNG stream,
  so identity generation adds no new stochastic draws for `sim/ADR-0009`'s
  seed-stability accounting to track (unlike NPI generation, which IS
  an RNG draw, `sim/ADR-0007`).

  Time doctrine (`sim/ADR-0011`, M2a): the engine clock (every event's :t) is
  now integer SECONDS from run start, not minutes. The pathway IR is
  NOT changed -- :delay's :from/:to stay minutes, authoring ergonomics
  -- the engine converts minutes -> seconds itself, at the one place a
  minute-denominated draw becomes a clock advance. A warm-up window
  (:warm-up-seconds, default 0) marks every event with `:t <
  warm-up-seconds` as `:warm-up true`; the log stays complete (no
  trimming here -- `sim/ADR-0011` leaves trimming, if any, to Package).

  Determinism doctrine: ALL randomness flows from java.util.Randoms
  DERIVED IN `run` FROM THE ONE SEED. No other entropy source (wall
  clock, hash ordering, nondeterministic seq realization) may
  influence output. Same config + seed => identical output, byte for
  byte once serialized -- WITHIN a version; see `sim/ADR-0009`
  for the cross-version seed-stability policy Milestone M1's new RNG
  draws (bed choice, attending sampling) triggered, and M2a's identity/
  time changes triggered again (documented once, per the M2a session
  plan, not per-commit).

  Stream partition (ADR-0171, arc 1): the path is now PLURAL. Until
  this arc there was exactly one Random and consumption order was
  GLOBAL EVENT ORDER, so adding, removing or reordering a single draw
  anywhere shifted every later draw for every later patient -- which is
  what made arcs 2-4 (newborns mid-run, new decide draws, emission
  chatter) each cost a corpus-wide reshuffle. `run` now derives five
  streams by family (`stream-family-tag`), keyed on a stable id rather
  than on construction order, and `decide` takes the stream MAP. A draw
  added to one patient's pathway now moves that patient. The run-scoped
  families (`:world`, `:facility`) are where cross-patient coupling
  still lives -- named and confined, not abolished: their draw counts
  are conditional on the population, so no per-patient stream can own
  them (ADR-0171 section 2(a)).

  The RNG-path law itself is unchanged: a measurement claiming to
  characterize the simulator's output must still draw from the real
  seeded, threaded path (`rulings.md#R-measure-claimed-population`).
  That path simply became plural.

  Step vocabulary: v0's :admission/:delay/:discharge, plus Milestone
  M1's :transfer (docs/operational-models.md's allocation ladder).
  Emission to HL7v2 is a separate namespace consuming the ground-truth
  log -- events here are format-free."
  (:require [ehrt.sim-model.interface :as sim-model]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-engine.assignment :as assignment]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.config :as config]
            [ehrt.sim-engine.decide :as decide]
            [ehrt.sim-engine.encounters :as encounters]
            [ehrt.sim-engine.evolve :as evolve]
            [ehrt.sim-engine.fold :as fold]
            [ehrt.sim-engine.log-index :as log-index]
            [ehrt.sim-engine.order-profiles :as order-profiles]
            [ehrt.sim-engine.person-fold :as person-fold]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.state :as state]
            [ehrt.sim-engine.streams :as streams]
            [ehrt.kernel.interface :as result]
            [malli.core :as m]
            [malli.util :as mu])
  (:import [java.util Random]))

;; --- moved to ehrt.sim-engine.state ---------------------------------------
;;
;; The patient accumulator -- `PatientState`, the five records it nests,
;; the state-at-t demographics and the two constructors -- now lives in
;; `ehrt.sim-engine.state`, extracted OUTPUT-IDENTICAL as the second step
;; of `roadmap.md#engine-namespace-extraction-and-apply-unification`
;; (author ruling C1(a); the census's own dependency order puts `state`
;; before `evolve`). Nothing moved changed: the forms there are this
;; file's own text, and the `M6 Task 1` header comment block that stood
;; here moved WITH them, because `PatientState`'s docstring cites it by
;; position.
;;
;; Under C1(a) THIS namespace stays the one every existing requirer
;; resolves against, so each of the thirteen vars keeps a delegating def
;; below, in the order it stood in. `ehrt.sim-engine.interface`
;; re-exports none of them and is untouched.
;;
;; The private `observation-value-fields` moved down there too and gets
;; NO delegating def: it is the census's one CYCLE BREAKER, it was
;; `defn-` here, and giving it a def would widen this namespace's public
;; surface. Its three call sites below -- `decide :observation`,
;; `evolve :observation`, `evolve :diagnostic-report` -- are
;; `state/`-qualified instead.

(def ConditionRecord
  "One condition, folded from a compiled encounter step's own
  :conditions annotations. Delegates to
  `ehrt.sim-engine.state/ConditionRecord`, which carries the contract
  and the pre-horizon scope note."
  state/ConditionRecord)

(def ObservationRecord
  "One observation -- a GMF `:observation` event, or one analyte/child
  flattened out of a `:result-available` or `:diagnostic-report` event.
  Delegates to `ehrt.sim-engine.state/ObservationRecord`, which carries
  the contract."
  state/ObservationRecord)

(def MedicationOrderRecord
  "One medication order, folded from :medication-order and closed by a
  citation-matching :medication-end. Delegates to
  `ehrt.sim-engine.state/MedicationOrderRecord`, which carries the
  contract."
  state/MedicationOrderRecord)

(def CarePlanRecord
  "One care plan, folded from :care-plan-start and closed by a
  citation-matching :care-plan-end. Delegates to
  `ehrt.sim-engine.state/CarePlanRecord`, which carries the contract."
  state/CarePlanRecord)

(def Demographics
  "STATE-AT-T demographics (ADR-0173 section 2(b)) -- what a patient's
  demographic facts are AT ONE INSTANT, as opposed to `:persona`, which
  is and stays the t0 sample. Delegates to
  `ehrt.sim-engine.state/Demographics`, which carries the contract, the
  three deliberate differences from `sim-model/Persona`, and arc 3a part
  4's optional-field reasoning."
  state/Demographics)

(def demographics-from-persona
  "A patient's INITIAL state-at-t demographics, read off their t0
  Persona; nil in, nil out. Delegates to
  `ehrt.sim-engine.state/demographics-from-persona`, which carries the
  contract."
  state/demographics-from-persona)

(def placeholder-demographics
  "The state-at-t demographics of a patient who arrived inside an open
  `:identity-unavailable` window -- the window's `:alias-name` and
  nothing else. Delegates to
  `ehrt.sim-engine.state/placeholder-demographics`, which carries the
  contract."
  state/placeholder-demographics)

(def PatientLocation
  "The {:ward :bed :placement} map an admitted patient's `:location`
  holds. Delegates to `ehrt.sim-engine.state/PatientLocation`, which
  carries the contract."
  state/PatientLocation)

(def EncounterRecord
  "ONE encounter (ADR-0174 section 2(a), arc 3b sweep 1). Delegates to
  `ehrt.sim-engine.state/EncounterRecord`, which carries the contract
  and the reasoning for the open record's deliberate thinness."
  state/EncounterRecord)

(def AppointmentRecord
  "ONE appointment (ADR-0174 section 2(b), arc 3b sweep 3). Delegates to
  `ehrt.sim-engine.state/AppointmentRecord`, which carries the contract
  and the at-most-one-terminal rule."
  state/AppointmentRecord)

(def PatientState
  "The engine's per-patient accumulator -- what folding `evolve` over a
  patient's own event subsequence produces (`components/sim/docs/
  patient-state-model.md` is the full design spec, and names THIS var).
  Delegates to `ehrt.sim-engine.state/PatientState`, which carries the
  contract and the per-milestone field history."
  state/PatientState)

(def valid-patient?
  "Validates a patient accumulator against PatientState. Delegates to
  `ehrt.sim-engine.state/valid-patient?`, which carries the contract."
  state/valid-patient?)

(def initial-patient
  "The state a patient starts in when its arrival is scheduled --
  `evolve`'s fold origin, and the single place this shape is
  constructed. Delegates to `ehrt.sim-engine.state/initial-patient`,
  which carries the contract."
  state/initial-patient)

;; --- moved to ehrt.sim-engine.streams ------------------------------------
;;
;; The draw primitives, the deterministic id minting and (below, where
;; they stood) the RNG stream partition now live in
;; `ehrt.sim-engine.streams`, extracted OUTPUT-IDENTICAL as the first
;; step of `roadmap.md#engine-namespace-extraction-and-apply-unification`
;; (author rulings C1(a)/C2(b)). Nothing moved changed: the forms there
;; are this file's own text.
;;
;; Under C1(a) THIS namespace stays the one every existing requirer
;; resolves against -- `ehrt.sim-engine.interface`, and every test that
;; requires `ehrt.sim-engine.engine` directly -- so each var that was
;; PUBLIC here keeps a delegating def, in the order it stood in. The four
;; that were PRIVATE (`rand-int-in`, `uniform-choice`,
;; `minted-encounter-id-field`, `minted-appointment-id-field`) get none
;; and are called `streams/`-qualified at their own sites, which is what
;; keeps this namespace's public surface exactly what it was.

(def mix64
  "A fixed, fully-specified 64-bit mix of two longs, deliberately NOT an
  RNG draw; PUBLIC since ADR-0171 ruling A1. Delegates to
  `ehrt.sim-engine.streams/mix64`, which carries the contract."
  streams/mix64)

(def patient-id-for
  "The internal, deterministic patient-id (sim/ADR-0010): a PURE function
  of this run's seed and the patient's arrival ordinal. Delegates to
  `ehrt.sim-engine.streams/patient-id-for`, which carries the contract."
  streams/patient-id-for)

(def encounter-id-for
  "The internal, deterministic encounter-id (ADR-0174 ruling B1) --
  `patient-id-for`'s own contract one level down. Delegates to
  `ehrt.sim-engine.streams/encounter-id-for`, which carries it."
  streams/encounter-id-for)

(def next-encounter-ordinal
  "The 0-indexed ordinal this patient's NEXT encounter takes. Delegates to
  `ehrt.sim-engine.streams/next-encounter-ordinal`, which carries the
  monotonicity argument."
  streams/next-encounter-ordinal)

(def appointment-id-for
  "The internal, deterministic appointment-id (ADR-0174 section 2(b)).
  Delegates to `ehrt.sim-engine.streams/appointment-id-for`, which
  carries the contract."
  streams/appointment-id-for)

(def next-appointment-ordinal
  "The 0-indexed ordinal this patient's NEXT appointment takes. Delegates
  to `ehrt.sim-engine.streams/next-appointment-ordinal`, which carries
  the invariant and the way it was found to fail."
  streams/next-appointment-ordinal)

;; The encounter -- the opt-in gate `encounter-openable?`, the two
;; compiled-step sets and `gate-compiled-encounters`, and the
;; `stamp-encounter` that carries an open encounter's id onto its events
;; -- now lives in `ehrt.sim-engine.encounters`, extracted
;; OUTPUT-IDENTICAL as the third step of
;; `roadmap.md#engine-namespace-extraction-and-apply-unification` (author
;; ruling C1(a); the census's dependency order puts `encounters` after
;; `streams` and `state` and before `evolve`). The four lifecycle folds
;; `evolve` applies moved with them, from further down this file.
;;
;; NO delegating def is left behind, and that is not an omission: all ten
;; movers were PRIVATE here -- seven `defn-`, three `def ^:private` -- so
;; constraint 5 makes them public THERE and forbids a def HERE, which
;; would widen this namespace's public surface. C1(a) owes a delegating
;; def for a moved PUBLIC var, and this cluster had none.
;; `ehrt.sim-engine.interface` re-exports none of the ten and is
;; untouched. The one call site here, `stamp-encounter` inside `run`'s
;; in-loop fold, was `encounters/`-qualified and left with `run` at the
;; TENTH extraction; this file calls nothing at all now.

(def stream-scheme
  "The RNG stream partition's own version marker (ADR-0171 ruling D1),
  stamped top-level into every sim manifest as `:stream-scheme`. It is a
  DISCRIMINATOR, not a warranty.

  Delegates to `ehrt.sim-engine.streams/stream-scheme`, which carries the
  full contract. `docs/consuming-ground-truth.md`'s Determinism section names THIS
  var's docstring as the authority for `:stream-scheme`; this sentence is
  that citation's forwarding address, so the doc still resolves."
  streams/stream-scheme)

(def stream-seed
  "The seed of one stream: `(mix64 (mix64 master family-tag) id-tag)`
  (ADR-0171 section 2(b), ruling A1). Delegates to
  `ehrt.sim-engine.streams/stream-seed`, which carries the contract."
  streams/stream-seed)

(def stream
  "A fresh `java.util.Random` for one stream -- `stream-seed`'s value,
  handed to the one constructor the engine has ever used. Delegates to
  `ehrt.sim-engine.streams/stream`, which carries the contract and the
  reason that function is deliberately UNHINTED.

  THIS VAR, not the moved one, is what `run` calls -- still, from
  `ehrt.sim-engine.run`, where the TENTH extraction took it. The stream-
  locality gate (`engine-test/mutating-one-patients-stream-seed-moves-
  only-that-patient`) perturbs the partition by `with-redefs` on
  `ehrt.sim-engine.engine/stream`, so `run`'s four call sites must
  resolve HERE and not to `streams/stream`, which is a different var the
  redefinition does not reach. They cannot say so with a `:require` --
  that namespace is required BY this one, and the reverse edge would be
  a cycle -- so they stay bare and reach this var through a lazily
  resolved shim `ehrt.sim-engine.run` carries under its own banner. Do
  not `streams/`-qualify them there, and do not retire this def."
  streams/stream)

(def newborn-id-tag
  "The `:person`-family id-tag for a newborn (ADR-0171 section 2(c),
  ruling B1). Delegates to `ehrt.sim-engine.streams/newborn-id-tag`,
  which carries the contract."
  streams/newborn-id-tag)

(def one-stream
  "Every family bound to ONE `Random` -- the degenerate stream map a
  caller with no `run` behind it needs. Delegates to
  `ehrt.sim-engine.streams/one-stream`, which carries the contract."
  streams/one-stream)

(def events-for-patient
  "Every event `patient-id` participates in, in log order -- the
  patient-phrased replacement for what a single :mrn-keyed lookup used
  to mean before sim/ADR-0010's :participants existed. Delegates to
  `ehrt.sim-engine.log-index/events-for-patient`, which carries the
  contract."
  log-index/events-for-patient)

;; --- moved to ehrt.sim-engine.decide ---------------------------------------
;;
;; The decision half of the `decide`/`evolve` pair -- the `decide`
;; multimethod, its thirty-two methods and the twenty-five helpers they
;; share -- now lives in `ehrt.sim-engine.decide`, extracted
;; OUTPUT-IDENTICAL as the NINTH and largest step of
;; `roadmap.md#engine-namespace-extraction-and-apply-unification` (author
;; ruling C1(a); the census's dependency order puts `decide` last, after
;; every other cluster it reads). Nothing moved changed: the forms there
;; are this file's own text, every interior comment block travelling with
;; the forms it introduces. Two prose lines were restated because they
;; carried a POSITIONAL claim about `replay`, which does not travel --
;; `decide :registered`'s "(below)" and `bed-status-change`'s "below";
;; both now name `ehrt.sim-engine.fold/replay` outright.
;;
;; THIS MOVE MOVED THE RUN'S SOLE EVENT PRODUCER. The census's section 4a
;; records that `run`'s own `(decide ...)` call is the ONLY expression in
;; the tree that mints a ground-truth event. When this banner was written
;; that call site stayed right where it was, below, naming `decide`
;; unqualified through the delegating def. The TENTH extraction took the
;; call site too, to `ehrt.sim-engine.run`, where it names
;; `decide/decide` -- the same MultiFn this def holds, reached one hop
;; shorter. The producer and the production path are together again, and
;; neither changed.
;;
;; The two reinstating cancels, `decide :cancel-transfer` and
;; `:cancel-discharge`, stood BELOW the `evolve` and `replay` defs rather
;; than with their siblings, because they were written to sit after
;; `replay` when `replay` was a real `defn` here. They moved too, and are
;; in the same relative order over there.
;;
;; Under C1(a) THIS namespace stays the one every existing requirer
;; resolves against, so the seven vars that were PUBLIC here keep
;; delegating defs below, in the order they stood in. A delegating `def`
;; of a multimethod shares the one MultiFn object, so all thirty-two
;; methods registered over there dispatch through this `decide` too.
;; Two are load-bearing for `ehrt.sim-engine.interface`, which census
;; constraint 4 requires to keep naming `engine/...`: `compile-patient`
;; at its `:62` and `documented-step-rejection-reasons` at its `:93`.
;; `decide` and `person-entry` are owed to the test tree instead, which
;; C1(a) forbids touching. The three `*-stay-minutes` tables had NO
;; caller outside this file at all -- their defs were what kept
;; `prelude`'s own three unqualified references below resolving. The
;; TENTH extraction took `prelude` and qualified all three, so those
;; three defs now have no caller anywhere. They stay: retirement is the
;; ruled repoint pass's business, not this file's.
;;
;; EIGHTEEN OF THE NINETEEN PRIVATE MOVERS STAY `defn-` over there, the
;; `weighted-pick` precedent at scale: constraint 5's prohibition is the
;; obligation, and widening is owed only where a caller stays behind.
;; Exactly one did -- `prelude` calls `days->seconds` -- so exactly one
;; mover widened, and that call site was `decide/`-qualified below. The
;; TENTH extraction took `prelude` to `ehrt.sim-engine.run`, so the
;; widening outlived the caller that forced it. Nothing in this file
;; calls anything at all now.

(def decide
  "Decides what happens when a patient is due to execute a step: the
  `decide` half of the `decide`/`evolve` pair (`sim/ADR-0008`).
  Delegates to `ehrt.sim-engine.decide/decide`, which carries the
  contract -- and, being a `defmulti`, IS the same `MultiFn` object, so
  every method registered there dispatches through this var too."
  decide/decide)

(def compile-patient
  "The whole of a patient's run-start compile -- persona draw, module
  walk and trajectory compile -- resolved BEFORE the loop (ADR-0173
  ruling C1). Delegates to `ehrt.sim-engine.decide/compile-patient`,
  which carries the contract -- and which `ehrt.sim-engine.interface`
  re-exports through THIS var."
  decide/compile-patient)

(def delivery-stay-minutes
  "Stay-length band by delivery kind. Delegates to
  `ehrt.sim-engine.decide/delivery-stay-minutes`, which carries the
  contract."
  decide/delivery-stay-minutes)

(def injury-stay-minutes
  "Stay-length band by injury severity. Delegates to
  `ehrt.sim-engine.decide/injury-stay-minutes`, which carries the
  contract."
  decide/injury-stay-minutes)

(def unidentified-stay-minutes
  "Stay-length band for an unidentified arrival. Delegates to
  `ehrt.sim-engine.decide/unidentified-stay-minutes`, which carries the
  contract."
  decide/unidentified-stay-minutes)

(def documented-step-rejection-reasons
  "Every `:reason` a `:step-rejected` event may carry (sim/ADR-0012).
  Delegates to `ehrt.sim-engine.decide/documented-step-rejection-
  reasons`, which carries the contract -- and which
  `ehrt.sim-engine.interface` re-exports through THIS var."
  decide/documented-step-rejection-reasons)

(def person-entry
  "This world's person-index entry for a patient, or nil. Delegates to
  `ehrt.sim-engine.decide/person-entry`, which carries the contract."
  decide/person-entry)

;; --- moved to ehrt.sim-engine.evolve --------------------------------------
;;
;; The fold -- `evolve` itself, its twenty-seven methods, and the four
;; private helpers they share (`fold-condition-annotation`,
;; `fold-conditions`, `resolve-appointment`, `keep-appointment`) -- now
;; lives in `ehrt.sim-engine.evolve`, extracted OUTPUT-IDENTICAL as the
;; fourth step of
;; `roadmap.md#engine-namespace-extraction-and-apply-unification` (author
;; ruling C1(a); the census's own dependency order puts `evolve` after
;; `streams`, `state` and `encounters` and before `fold`). Nothing moved
;; changed: the forms there are this file's own text, and the arc-3b
;; banner that stood at the head of the methods moved WITH them, because
;; its last sentence is a positional claim about the methods themselves.
;;
;; Under C1(a) THIS namespace stays the one every existing requirer
;; resolves against, so the ONE var that was public here -- the
;; `defmulti` -- keeps a delegating def below, in the place it stood.
;; A delegating `def` of a multimethod shares the one multifn object, so
;; every method registered over there dispatches through this var too.
;; `replay` and `run` both called `evolve` unqualified through it when
;; this banner was written; the FIFTH extraction moved `replay` to
;; `ehrt.sim-engine.fold` (see the banner below) and the TENTH moved
;; `run` to `ehrt.sim-engine.run`, and each now takes the edge directly
;; as `evolve/evolve` rather than back through this def. Same multifn,
;; one hop shorter. The four helpers were PRIVATE and get none, which is
;; what keeps this namespace's public surface exactly what it was.
;; `ehrt.sim-engine.interface` re-exports none of the five and is
;; untouched.

(def evolve
  "Folds one ground-truth event into ONE patient it names:
  (patient-state, event) -> patient-state'. Delegates to
  `ehrt.sim-engine.evolve/evolve`, which carries the contract and every
  one of its twenty-seven methods -- the same multifn object, so a
  method registered there is dispatched through this var."
  evolve/evolve)

;; --- moved to ehrt.sim-engine.fold ----------------------------------------
;;
;; The derived-state fold -- `replay`, `update-beds` and the correction
;; table `bed-correction-event-types` -- now lives in
;; `ehrt.sim-engine.fold`, extracted OUTPUT-IDENTICAL as the fifth step of
;; `roadmap.md#engine-namespace-extraction-and-apply-unification` (author
;; ruling C1(a); the census's own dependency order puts `fold` after
;; `evolve` and before `log-index`, whose `reinstated-state` calls
;; `replay`). Nothing moved changed: the three forms there are this
;; file's own text, and this cluster had no interior comment block for a
;; banner to have to travel with -- the first of the five for which that
;; is true.
;;
;; THIS MOVE MOVES AN APPLY SITE. `replay` is the census's apply site 2
;; (section 4c), and it does not do six of the ten things `run`'s own
;; in-loop fold does (section 4b): no encounter stamp, no warm-up mark,
;; no bed index, and none of the three log indexes. That divergence is
;; documented and is RULED to be paid at application-path unification,
;; not here. Nothing `replay` folds was added, removed or reordered.
;;
;; Under C1(a) THIS namespace stays the one every existing requirer
;; resolves against, so the ONE var that was public here -- `replay` --
;; keeps a delegating def below, in the place it stood. That def is not a
;; formality: `ehrt.sim-engine.interface` re-exports `replay` at its
;; `:89` (`(def replay engine/replay)`), and census constraint 4 requires
;; that file to keep naming `engine/...`, so this def is what keeps the
;; brick's own public surface resolving. `reinstated-state` no longer
;; calls through it: the SIXTH extraction moved that form to
;; `ehrt.sim-engine.log-index`, whose fallback now names `fold/replay`
;; directly -- the same function object this def holds, reached one hop
;; shorter.
;;
;; `update-beds` was `defn-` and `bed-correction-event-types` was
;; `^:private`, so under constraint 5 they become public THERE and get no
;; def HERE -- that would widen this namespace's public surface, which
;; C1(a) does not ask for. `run`'s one `update-beds` call site was
;; `fold/`-qualified instead, and travelled with `run` to
;; `ehrt.sim-engine.run` at the TENTH extraction.
;;
;; `ehrt.sim-check.check` deliberately reimplements both the bed index
;; and the correction table rather than calling these -- it is the
;; independent judge, and calling the engine's own index-builder would
;; prove only that the engine agrees with itself. Its own three prose
;; attributions were repointed to `ehrt.sim-engine.fold` by this move,
;; because a private mover has no delegating def to forward them.

(def replay
  "Replays a ground-truth log through `evolve` from an empty world,
  returning a parallel seq of {:event :patient-id :before :after
  :world-before :world-after}. Delegates to
  `ehrt.sim-engine.fold/replay`, which carries the contract -- and which
  `ehrt.sim-engine.interface` re-exports through THIS var."
  fold/replay)

;; --- moved to ehrt.sim-engine.assignment -----------------------------------
;;
;; Weighted per-patient pathway and module assignment -- `weighted-pick`
;; and the two assigners built on it -- now lives in
;; `ehrt.sim-engine.assignment`, extracted OUTPUT-IDENTICAL as the
;; eighth step of
;; `roadmap.md#engine-namespace-extraction-and-apply-unification` (author
;; ruling C1(a); the census's dependency order puts `assignment` in the
;; LEAF rank, so it was free from the start). Nothing moved changed: the
;; three forms there are this file's own text, and BOTH interior comment
;; blocks travelled with the forms they introduce -- including the
;; second's positional "the SAME shape/law as `assign-pathway` just
;; above", which is still true over there because the order is
;; preserved.
;;
;; Under C1(a) THIS namespace stays the one every existing requirer
;; resolves against, so the two vars that were PUBLIC here keep
;; delegating defs below, in the order they stood in. Neither is on
;; `ehrt.sim-engine.interface`'s re-export list and census constraint 4
;; names neither: what makes them load-bearing is `engine_test.clj`'s
;; seven `engine/assign-pathway` and three `engine/assign-module` call
;; sites, which C1(a) forbids touching. `run`'s own two call sites
;; called both unqualified through these until the TENTH extraction took
;; `run` to `ehrt.sim-engine.run`, where they name `assignment/` direct;
;; the test tree's ten sites are what keeps these defs load-bearing.
;;
;; `weighted-pick` gets NO def here -- that would widen this namespace's
;; public surface, which C1(a) does not ask for and constraint 5
;; forbids outright. It also STAYS `defn-` over there, the first
;; private mover of the eight extractions to do so: constraint 5's
;; other half, that a private mover "becomes public in its new
;; namespace", was in every earlier cluster FORCED by call sites left
;; behind here, and `weighted-pick`'s only two callers travel with it.
;; Nothing here needs qualifying, because nothing here calls it. Its one
;; cross-brick prose attribution, `sim_model/persona.clj`'s docstring
;; naming "ehrt.sim-engine.engine's own private weighted-pick", was
;; repointed by this move -- a private mover has no def to forward a
;; citation, the same class the fold and log-index moves paid in
;; `check.clj` and `churn.clj` -- and that sentence cites the PRIVACY,
;; so widening would have falsified the repoint in the same commit that
;; made it.

(def assign-pathway
  "Resolves the pathway a `sim-model/PathwaysConfig` assigns to patient
  ordinal `i`, ALWAYS consuming exactly one `.nextDouble`. Delegates to
  `ehrt.sim-engine.assignment/assign-pathway`, which carries the
  contract and the fixed-consumption argument behind it."
  assignment/assign-pathway)

(def assign-module
  "Resolves the module id a `ehrt.patient-simulator.gmf/ModulesConfig`
  assigns to patient ordinal `i`, or nil when neither an override nor a
  pool covers it, ALWAYS consuming exactly one `.nextDouble`. Delegates
  to `ehrt.sim-engine.assignment/assign-module`, which carries the
  contract."
  assignment/assign-module)

;; --- moved to ehrt.sim-engine.config ---------------------------------------
;;
;; `run`'s config surface -- `config-keys` and the two opt-in value
;; schemas `:persons` and `:scheduling` carry, with their guard
;; predicates -- now lives in `ehrt.sim-engine.config`, extracted
;; OUTPUT-IDENTICAL as the seventh step of
;; `roadmap.md#engine-namespace-extraction-and-apply-unification` (author
;; ruling C1(a); the census's dependency order puts `config` in the LEAF
;; rank, so it was free from the start). Nothing moved changed: the five
;; forms there are this file's own text, `config-keys`' per-key comments
;; included, and this cluster had no interior comment block for a banner
;; to have to travel with.
;;
;; Under C1(a) THIS namespace stays the one every existing requirer
;; resolves against, so all five vars -- every one of them PUBLIC here --
;; keep a delegating def below, in the order they stood in. Two are
;; load-bearing for `ehrt.sim-engine.interface`, which census constraint
;; 4 requires to keep naming `engine/...`: `config-keys` at its `:46`
;; and `valid-persons?` at its `:82`. `valid-scheduling?`'s is owed to
;; `scheduling_test.clj` instead, which C1(a) forbids touching.
;; `Persons` and `Scheduling` have no caller at all and keep defs
;; anyway: C1(a) owes a def for a moved PUBLIC var, and reading that as
;; "public vars someone calls" would be an exception the ruling does not
;; grant. `run`'s own two guard call sites called `valid-persons?` and
;; `valid-scheduling?` unqualified through these until the TENTH
;; extraction took `run` to `ehrt.sim-engine.run`, where both are
;; `config/`-qualified; `interface.clj` and `scheduling_test.clj` are
;; what keeps these defs load-bearing now.

(def config-keys
  "The canonical, documented list of every key `run`'s config map
  accepts. Delegates to `ehrt.sim-engine.config/config-keys`, which
  carries the list itself AND the per-key comment paragraph each entry
  owes -- the comments are `;;` lines inside that vector, so they are
  the one thing a delegating def cannot bring along, and
  `docs/consuming-ground-truth.md` names them THERE for that reason."
  config/config-keys)

(def Persons
  "`run`'s ENGINE-FACING `:persons` value (ADR-0173 section 2(a), arc 3a
  part 3) -- not the config-facing one `ehrt.sim.run` translates from.
  Delegates to `ehrt.sim-engine.config/Persons`, which carries the
  contract for all four sub-keys."
  config/Persons)

(def Scheduling
  "`run`'s `:scheduling` value (ADR-0174 section 2(b), arc 3b sweep 3) --
  the six sub-keys the ADR names, and nothing else. Delegates to
  `ehrt.sim-engine.config/Scheduling`, which carries the contract and
  the band-sum argument `valid-scheduling?` enforces."
  config/Scheduling)

(def valid-scheduling?
  "Whether `run`'s `:scheduling` value is well-formed. Delegates to
  `ehrt.sim-engine.config/valid-scheduling?`, which carries the
  result-not-throw contract and why the band-sum check lives outside the
  malli schema."
  config/valid-scheduling?)

(def valid-persons?
  "Whether `run`'s engine-facing `:persons` value is well-formed.
  Delegates to `ehrt.sim-engine.config/valid-persons?`, which carries
  the contract -- and which `ehrt.sim-engine.interface` re-exports
  through THIS var."
  config/valid-persons?)

;; --- moved to ehrt.sim-engine.run -----------------------------------------
;;
;; The driver -- `pop-min`, `placeholder-registration`, `select-person`,
;; `prelude`, `person-plan` and `run` -- now lives in
;; `ehrt.sim-engine.run`, extracted OUTPUT-IDENTICAL as the TENTH and
;; LAST step of
;; `roadmap.md#engine-namespace-extraction-and-apply-unification` (author
;; ruling C1(a); ruling C4(b) ruled this extraction on 2026-08-30, the
;; question the ninth extraction's record put to the author). Nothing
;; moved changed: the forms there are this file's own text, and every
;; interior comment block travelled with the form it introduces --
;; including the positional ones, because the six forms are over there in
;; the order they stood in here.
;;
;; TWO REGIONS. `pop-min` stood alone between `assign-module` above and
;; the `config` banner below; the other five stood in one block from
;; `placeholder-registration` to the end of the file. They are gathered
;; in that same relative order.
;;
;; WITH THEM GONE THIS FILE IS A PURE FACADE: its `ns`, forty-three
;; delegating defs and nine explanatory comment blocks, and no
;; executable code of its own. That is the shape ruling C4(b) chose, and
;; it is a different shape from the nine moves before it, every one of
;; which left real code behind.
;;
;; THIS MOVE MOVES THE PROGRAM'S SOLE EVENT PRODUCER AND ITS MAIN APPLY
;; SITE -- the census's sections 4a and 4b, `run`'s `(decide ...)` call
;; and the ten-step in-loop fold that follows it. Both moved verbatim.
;; Nothing was added, removed or reordered, and the divergence between
;; that apply site and `replay`'s stays exactly where the census records
;; it, to be paid at application-path unification.
;;
;; Under C1(a) THIS namespace stays the one every existing requirer
;; resolves against, so the two vars that were PUBLIC here -- `person-
;; plan` and `run` -- keep delegating defs below, in the order they stood
;; in. Both are load-bearing for `ehrt.sim-engine.interface`, which
;; census constraint 4 requires to keep naming `engine/...`: `run` at its
;; `:45` and `person-plan` at its `:80`. Both are owed to the test tree
;; besides, which C1(a) forbids touching -- `run` at a scale no earlier
;; mover approached, forty-nine `(engine/run ...)` call forms in
;; `engine_test.clj` alone, and `person-plan` at seven in
;; `persons_test.clj`.
;;
;; THE OTHER FOUR MOVERS WERE PRIVATE AND STAY `defn-`, the
;; `weighted-pick` precedent (extraction 8) once more: constraint 5's
;; prohibition is the obligation, and widening is owed only where a
;; caller stays behind. Nothing stays behind -- there is no code here to
;; call anything -- so nothing widens. `engine/pop-min`,
;; `engine/prelude`, `engine/select-person` and
;; `engine/placeholder-registration` do not resolve.
;;
;; `stream` IS THE ONE VAR THE MOVED TEXT STILL REACHES THROUGH THIS
;; FILE. Census constraint 1 requires `run`'s four `stream` call sites to
;; resolve to `ehrt.sim-engine.engine/stream`, the var
;; `engine_test/mutating-one-patients-stream-seed-moves-only-that-patient`
;; perturbs. A facade may require its implementations and an
;; implementation may not require its facade, so `ehrt.sim-engine.run`
;; reaches this var through a lazily-resolved shim instead of a
;; `:require`. Its own banner over there carries the mechanism. Every
;; OTHER bare name the moved text carried -- `initial-patient`,
;; `patient-id-for`, `decide`, `compile-patient`, the three
;; `*-stay-minutes` tables, `evolve`, `assign-pathway`, `assign-module`,
;; `valid-persons?`, `valid-scheduling?` -- is qualified over there to
;; the namespace that owns it, which holds the same object the def here
;; holds. Fourteen call sites, and no other code line differs.

(def person-plan
  "ADR-0173 ruling C1's own resolution, exported: for a config `run`
  would accept, the arrival-ordinal bindings, the compiled deaths keyed
  by person, and the person index. Delegates to
  `ehrt.sim-engine.run/person-plan`, which carries the contract -- and
  which `ehrt.sim-engine.interface` re-exports through THIS var."
  run/person-plan)

(def run
  "Runs the simulation: the priority queue, the decide/evolve loop and
  the ground-truth log it produces. Delegates to
  `ehrt.sim-engine.run/run`, which carries the contract, the full config
  key list and the pinned draw order -- and which
  `ehrt.sim-engine.interface` re-exports through THIS var."
  run/run)
