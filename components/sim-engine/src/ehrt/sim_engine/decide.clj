(ns ehrt.sim-engine.decide
  "The decision half of the `decide`/`evolve` pair (`sim/ADR-0008`): the
  `decide` multimethod, its thirty-two methods, and the twenty-five
  helpers they share -- `engine.clj`'s NINTH extraction under
  `roadmap.md#engine-namespace-extraction-and-apply-unification` (the
  census's own dependency order, `.agents/plans/engine-extraction-
  census.md` section 3a: `decide` lands last of the nine, after
  `streams`, `state`, `config`, `assignment`, `encounters`, `evolve`,
  `fold` and `log-index`).

  THIS NAMESPACE IS THE RUN'S SOLE EVENT PRODUCER. The census's section
  4a records that `(decide ...)` in `run`'s loop is the ONLY expression
  in the tree that mints a ground-truth event; every step -- authored,
  churn-injected, module-compiled or person-seeded -- reaches the log by
  being decided here. `run` keeps the call site and calls `decide`
  unqualified through the delegating def below, so the producer moved
  and the production path did not.

  TWO NON-CONTIGUOUS REGIONS, in their own source order. Fifty-six forms
  stood in one block, from `defmulti decide` down to `decide
  :care-plan-end`; the two reinstating cancels, `:cancel-transfer` and
  `:cancel-discharge`, stood further down the file, BELOW the `evolve`
  and `replay` delegating defs the fourth and fifth extractions left
  behind -- because they were written to sit after `replay` when
  `replay` was a real `defn` there. They are gathered here in that same
  order, so a reader who knows the old file can still walk it.

  ONE MULTIFN, NOT TWO. `engine.clj` keeps `(def decide decide/decide)`,
  which holds the same `MultiFn` object this `defmulti` creates, so
  every method registered here dispatches through that var too -- the
  treatment the `evolve` extraction established and this one repeats at
  thirty-two methods.

  SEVEN VARS WERE PUBLIC in `engine.clj` and all seven keep a delegating
  def there under ruling C1(a), in the order they stood in: `decide`,
  `compile-patient`, `delivery-stay-minutes`, `injury-stay-minutes`,
  `unidentified-stay-minutes`, `documented-step-rejection-reasons` and
  `person-entry`. Two are load-bearing for `ehrt.sim-engine.interface`,
  which census constraint 4 requires to keep naming `engine/...`:
  `compile-patient` at its `:62` and `documented-step-rejection-reasons`
  at its `:93`. `decide` and `person-entry` were owed to the test tree
  instead -- `engine_test.clj` alone called `decide` through the facade
  at ninety
  sites, behind C1(a)'s fence on test files; the ruled repoint pass
  lifted it, moved those sites to `decide/`, and retired both. The three
  `*-stay-minutes` tables had no caller outside `engine.clj` at all:
  their defs were load-bearing for `prelude`, which named all three
  unqualified. The TENTH extraction moved `prelude` to
  `ehrt.sim-engine.run`, where all three are `decide/`-qualified, so
  those three defs now have no caller anywhere and are retirement
  candidates for the ruled repoint pass.

  EIGHTEEN OF THE NINETEEN PRIVATE MOVERS STAY `defn-`, which is the
  `weighted-pick` precedent (extraction 8) applied at scale: constraint
  5's prohibition is the obligation, and its first sentence describes
  the widenings earlier clusters were FORCED into by call sites left
  behind. Here exactly one caller stayed behind, so exactly one mover
  widens: `days->seconds`, which `prelude` calls for the follow-up
  interval and reaches as `decide/days->seconds`. That call site is now
  in `ehrt.sim-engine.run`, so the widening outlived the residue that
  forced it. Nothing else calls a private mover of this cluster.

  Extracted OUTPUT-IDENTICAL: every form below is `engine.clj`'s own
  text, moved and not rewritten, with two prose exceptions and no code
  one. `decide :registered`'s comment said `replay` was \"(below)\" and
  `bed-status-change`'s docstring said `replay` and the run loop's folds
  were \"below\"; neither is in this namespace, so both are restated to
  name `ehrt.sim-engine.fold/replay` without the positional half. Every
  interior comment block travelled with the forms it introduces.

  Its edges, all taken directly into the namespace that owns them, as
  they already were in `engine.clj`: `streams` (the four private draw
  and minting primitives), `log-index` (eleven crossings, the census's
  single biggest row), `encounters` (`encounter-openable?`,
  `gate-compiled-encounters`), `state` (`observation-value-fields`, the
  cycle breaker), plus `sim-model`, `patient-simulator` and
  `order-profiles`. Nothing here resolves in `engine.clj`: every
  occurrence of a name defined elsewhere -- `run`, `prelude`, `evolve`,
  `replay`, `initial-patient`, `assign-pathway`, `one-stream` -- is
  docstring or comment prose, verified form by form. `run` and
  `prelude` left too, with the tenth extraction.

  COVERAGE, disclosed rather than implied. `bin/regression-oracle` and
  `bin/ground-truth-bracket` exercise this namespace heavily -- it
  produces every event in every root -- but neither bracket reaches a
  CANCEL decide: no gated corpus emits `:cancel-admit`,
  `:cancel-transfer`, `:cancel-discharge` or `:transfer-in-error`, and
  none resolves a citation. An IDENTICAL bracket across this move is
  therefore real evidence for fifty-two of the fifty-eight forms and no
  evidence at all for `rejected-outcome`, `documented-step-rejection-
  reasons` and the four cancel-family methods. What covers those is the
  suite: `engine_test.clj`'s cancel family driving `decide` against
  hand-built worlds, `ehrt.sim.run-test`'s reinstatement tests, and
  `ehrt.sim-check`'s own conformance walk over the documented reasons.

  TWO CLAIMS IN THE MOVED TEXT WERE ALREADY STALE AT THIS TIP and are
  recorded rather than repaired (`rulings.md#R-move-not-improve`, whose
  prose half applies): `defmulti decide`'s docstring says
  \"(`stream-family-tag` above)\", but `stream-family-tag` was a PRIVATE
  streams mover and left `engine.clj` at the FIRST extraction; and the
  bed-status banner says \"exactly like `:encounter-minting` above it\",
  whose only other occurrence in `engine.clj` is in `run`, below. Both
  were false before this move and are equally false after it."
  (:require [ehrt.sim-model.interface :as sim-model]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-engine.encounters :as encounters]
            [ehrt.sim-engine.log-index :as log-index]
            [ehrt.sim-engine.order-profiles :as order-profiles]
            [ehrt.sim-engine.state :as state]
            [ehrt.sim-engine.streams :as streams])
  (:import [java.util Random]))

(defmulti decide
  "Decides what happens when patient `patient-id` is due to execute
  `step` at simulated time t (SECONDS from the run's epoch, sim/ADR-0011).
  Consults `world` ({:patients {patient-id -> patient-state} :facility
  .. :providers ..} -- read-only) and the seeded RNGs to make stochastic
  and cross-patient choices; returns {:events [<ground-truth
  event>...] :advance <seconds>}. NEVER returns or implies a new
  patient state -- state changes only by folding the returned events
  through `evolve` (sim/ADR-0008). Pure given the RNGs (they are the only
  stateful arguments, and their consumption order is fixed by the
  deterministic event ordering).

  ADR-0171: the first argument is a STREAM MAP, not one `Random` --
  `{:patient <this patient's stream> :world <the run's> :facility <the
  run's>}` -- and each method draws from the family its census row
  names (`stream-family-tag` above). `run` builds the real, partitioned
  map; a caller with no run behind it wraps its own `Random` in
  `one-stream`, which collapses the families back to the pre-partition
  single stream."
  (fn [_streams _t _world _patient-id step] (:type step)))

(defn- exhausted-outcome
  "Task 0: result-not-throw for allocation-ladder exhaustion --
  sim-model/allocate no longer throws, so decide translates its
  structured {:exhausted true} into a decide-level outcome the run loop
  halts on and run-command (ehrt.sim.run) surfaces as :error
  :capacity-exhausted, payload {:patient-id :ward :census}."
  [patient-id home-ward-name facility board]
  {:events [] :advance 0
   :exhausted {:patient-id patient-id :ward home-ward-name
               :census (sim-model/ward-census facility board)}})

;; --- M4: Persona (docs/sim-theory.edn's :persona stage) -------------------
;; :registered is engine-internal, never authorable pathway IR -- the same
;; treatment :result-followup already gets (pathway.clj's own docstring):
;; `run` prepends it to every patient's step queue itself, so no
;; sim-model/Step schema entry exists for it and it never
;; passes through sim-model/valid?. Its decide call is the ACTUAL Persona
;; stage boundary; folding it into Execute's own step-queue mechanism
;; rather than a separate pipeline stage is this milestone's own documented
;; theory-flip note (docs/sim-theory.edn, docs/sim-theory.md) -- the
;; stage's contract ("samples once, from the run's seeded RNG, in
;; fixed order" -- ADR-0171: from THIS patient's :patient-family stream) is satisfied by this event exactly, not merely gestured at.

;; M5b Task 4: persona -> run-module -> CompileTrajectory -> IR, the ACTUAL
;; RunModules/CompileTrajectory stage boundary, folded into THIS SAME
;; engine-internal step for the same reason Persona itself was (M4's own
;; documented theory-flip note): a patient's assigned module is consumed at
;; the same init moment this event already owns. `step`'s own :closure (set,
;; per patient, by `run`'s eager `registered-steps-for` -- mirroring how
;; :pathways' own per-patient resolution already happens eagerly, ahead of
;; the main loop) is nil for the (default, opt-in) case of no module
;; assignment -- byte-identical to pre-M5b :registered output, no new draw,
;; the same "absent means untouched" law :pathways/:churn-profile already
;; establish. `registration-t` is `sim-model/reference-today-epoch-day` --
;; components/patient-simulator/docs/gmf-interpreter.md section 3's own "that patient's own :registered
;; event time," expressed in the SAME calendar anchor every persona's own
;; DOB is already computed against (persona.clj's own docstring note).
;; `:module-horizon-days` bounds the walk (`run-module`'s own optional
;; `horizon-end-t`) -- REQUIRED for any real vendored module (M5b's own
;; finding, components/patient-simulator/docs/gmf-interpreter.md section 8 item 5: a module with no
;; Terminal state and no Guard to block on would otherwise run until the
;; interpreter's own max-steps backstop throws).

;; ADR-0033 AR-2/AR-3 (2026-08-03, J3 closed): `:closure` -- ALWAYS
;; closure-shaped when present (`ehrt.patient-simulator.gmf/load-closure`'s
;; own :ok payload, `{:root :modules :tables}`, plus an optional
;; :initial-attributes an authoring-time config may attach, AR-1) --
;; replaces the pre-ADR-0033 bare :module. `run-module` is now called at
;; its FULL 7-arity, threading the closure's own `:modules` (submodule
;; registry) and `:tables` (lookup-table members) straight through to the
;; interpreter -- the previous bare 5-arity call defaulted `modules` to
;; `{root root-module}` (the root alone) and `tables`/`initial-attributes`
;; to `{}`, which is EXACTLY the singleton-closure/no-seed case: this
;; change is draw-neutral and byte-neutral for every pre-ADR-0033 run
;; (AR-4), and only NEWLY reaches a closure's own called submodules/
;; tables/seed for a root that actually has them.

(defn compile-patient
  "One patient's Persona and compiled module trajectory, drawn from that
  patient's OWN `:patient` stream. Returns `{:persona p :compiled c}`;
  `:compiled` is nil for a patient with no assigned closure.

  ARRIVAL-TIME INDEPENDENT, and that is the whole reason this is a
  function rather than a `let` inside `decide :registered` (ADR-0173
  ruling C1: `ehrt.sim.run` must be able to obtain every patient's
  compiled death instant BEFORE the run, because the person component's
  own `persons` front door takes the whole population at once -- named
  in prose rather than by namespace, because ADR-0172 limitations row
  10's reverse-edge half is a bare token scan over this component's
  src). There is no `t` parameter here because nothing below could read
  one. Every input, enumerated:

  | input | why it cannot differ between run start and arrival |
  |---|---|
  | `rng` | ONE stream per patient (`run`'s `patient-rngs`), and exactly three `decide` methods draw from the `:patient` family -- `:registered`, `:delay`, `:order` -- all three on the ACTING patient's own stream. `:delay` and `:order` are steps that follow `:registered` in that patient's own queue, so at arrival the stream stands exactly where the pre-loop draws left it. See `run`'s docstring for the pinned pre-loop order. |
  | `(:persona-config world)` | set once in `init-world`; the run loop only ever `assoc`s `:ground-truth`/`:reinstate-index`/`:citation-index` and `update-in`s `[:patients pid]` |
  | `closure` | resolved pre-loop by `run`'s own `module-for` and carried on the `:registered` STEP -- immutable queue data |
  | `(:modules closure)` / `(:root closure)` / `(:initial-attributes closure)` / `(:tables closure)` | pure data inside that closure |
  | `reg-t` | `sim-model/reference-today-epoch-day` is `(LocalDate/of (inc reference-birth-year) 1 1)` -- a FIXED calendar anchor computed from a constant, not a clock and not the arrival instant |
  | `horizon-end-t` | `reg-t` plus `(:module-horizon-days world)`, run config |
  | `(:facility world)` | run config; never re-`assoc`ed by the loop (bed occupancy lives in `[:patients pid :location]`, not here) |
  | `history?` | `(:history world)`, run config |

  So the ONLY thing that moves when the call moves is WHEN it happens in
  wall-clock terms; the stream position it reads from is unchanged, which
  is what makes the move byte-identical. `the-registered-compile-is-
  arrival-time-independent` (engine-test) and `every-gated-run-compiles-
  the-same-persona-at-any-arrival-time` (ehrt.sim.run-test) are the
  gates, and `bin/regression-oracle` is the proof.

  `world` here is any map carrying the four config keys above -- the live
  `world` at `decide` time, or the equivalent map `run` builds before the
  loop exists.

  THE 4-ARITY (ADR-0173 section 1, arc 3a part 3) is the seam the
  `:patient` family loses. `supplied-persona`, when non-nil, IS this
  patient's Persona and NO `sim-model/persona` draw is made -- the
  arrival was bound to a person, and that person's Persona was drawn
  from the `:person` family instead. Thirteen draws (sixteen with
  demographic weights) leave the `:patient` stream, and every
  `:patient` draw that FOLLOWS the seam -- the module walk here,
  `decide :delay`, `decide :order` -- shifts by that much. That is the
  whole of arc 3a's predicted blast radius, and it is why `:persons`
  ABSENT ENTIRELY has to stay the byte-identical path: nil in, and this
  is the 3-arity verbatim."
  ([rng world closure] (compile-patient rng world closure nil))
  ([rng world closure supplied-persona]
   (let [persona (or supplied-persona (sim-model/persona rng (:persona-config world)))
         history? (boolean (:history world))
         compiled (when closure
                    (let [root-module (get (:modules closure) (:root closure))
                          reg-t (sim-model/reference-today-epoch-day)
                          horizon-end-t (+ reg-t (:module-horizon-days world))
                          {:keys [trajectory]} (patient-simulator/run-module
                                                 root-module rng persona reg-t horizon-end-t
                                                 (:modules closure)
                                                 (or (:initial-attributes closure) {})
                                                 (or (:tables closure) {})
                                                 history?)]
                      ;; ADR-0042 AR-1/AR-3: `history?` threads straight
                      ;; through to compile-trajectory's own new 4-arity --
                      ;; false stays the plain legacy path (byte-identical
                      ;; to every pre-H run, since that arity's own body is
                      ;; nothing but a call to the unchanged 3-arg one).
                      (patient-simulator/compile-trajectory trajectory (:facility world) reg-t history?)))]
     {:persona persona :compiled compiled})))

(defmethod decide :registered
  [{rng :patient} t world patient-id {:keys [closure]}]
  ;; :active-mrn is REQUIRED here, not merely conventional: :registered
  ;; is now every patient's FIRST event, and `ehrt.sim-engine.fold/replay`
  ;; bootstraps
  ;; a never-yet-seen participant's initial state via `(initial-patient
  ;; pid (:active-mrn event))` off the FIRST event naming them -- every
  ;; other event type already carries :active-mrn for exactly this
  ;; reason (a convention this event must honor, not just a rendering
  ;; nicety), or `replay`'s own bootstrap (and every check.clj invariant
  ;; built on it) silently seeds `:mrns #{nil}`.
  ;;
  ;; ADR-0173 C1: the persona draw and the module walk now happen at RUN
  ;; START, not here -- `run` calls `compile-patient` for every arrival
  ;; ordinal, in ordinal order, immediately after the pre-loop
  ;; `:patient`-family draws, and carries the result in
  ;; `:compiled-patients`. This method ATTACHES what was pre-compiled.
  ;; FALLS BACK to compiling in place when `world` carries no
  ;; `:compiled-patients` KEY -- a hand-built world, as most of
  ;; engine-test uses. Same fallback rule as `reinstated-state` and
  ;; `last-cited-index`, and for the same reason: on the KEY, never on a
  ;; missing entry, so a carrier `run` built but failed to populate shows
  ;; up as a changed corpus rather than as a silent recompile.
  ;; ADR-0173 section 2(b) (arc 3a part 3): `:residence` rides the event
  ;; ONLY for an arrival bound to a person who is not housed at their own
  ;; registration instant -- a nil-dropping `cond->`, the same shape
  ;; `citation-fields` and `reason-field` already use, so a run with no
  ;; `:persons` key emits the identical bytes it always has. It is a SUM
  ;; and not a nilable address because `sim-model/Persona`'s own
  ;; `:address` is required and non-nilable: the Persona keeps the row
  ;; last lived at, and this says whether anybody lives there (ruling E1
  ;; -- the wire renders PID-11 absent, ground truth keeps the
  ;; distinction between `:unhoused` and `:unknown`).
  ;;
  ;; ARC 3A PART 4 adds four optional keys, all of them ABSENT for every
  ;; run with no `:persons`, so this method's bytes are unchanged there.
  ;; `:person-id` is the provenance stamp
  ;; `identification-merge-survivor-is-the-persons-prior-patient` reads
  ;; on BOTH sides of an identification merge. The other three ride only
  ;; a PLACEHOLDER registration (ADR-0173 section 2(d)): the arrival
  ;; landed inside an open `:identity-unavailable` window, so the wire
  ;; gets the window's alias and an `:unknown` residence, and
  ;; `:window-close-t` is what lets
  ;; `every-placeholder-registration-is-resolved-or-still-open` tell a
  ;; dangling placeholder from one the horizon simply ended inside.
  ;;
  ;; `:persona` RIDES A PLACEHOLDER REGISTRATION UNCHANGED, and that is
  ;; deliberate. Ground truth knows who this patient is -- an
  ;; unidentified arrival is still somebody -- so the record stays
  ;; truthful and `registered-persona-is-schema-valid` keeps asserting
  ;; exactly what it asserts today. What `:identity :placeholder` buys
  ;; is that the FOLD (and therefore every message) renders the alias
  ;; instead: `evolve :registered` seeds `placeholder-demographics`
  ;; rather than `demographics-from-persona`.
  (let [{:keys [persona compiled residence person-id identity alias-name window-close-t
                mother-patient-id]}
        (if (contains? world :compiled-patients)
          (get (:compiled-patients world) patient-id)
          (compile-patient rng world closure))
        placeholder? (= :placeholder identity)]
    {:events [(cond-> {:event :registered :t t
                       :active-mrn (get-in world [:patients patient-id :active-mrn])
                       :persona persona
                       :participants [{:patient-id patient-id :role :subject}]}
                (seq (:registration-facts compiled)) (assoc :pre-horizon-facts (:registration-facts compiled))
                (and residence (not= :housed (:status residence))) (assoc :residence residence)
                person-id (assoc :person-id person-id)
                mother-patient-id (assoc :mother-patient-id mother-patient-id)
                placeholder? (assoc :identity :placeholder
                                    :alias-name alias-name
                                    :window-close-t window-close-t
                                    :residence {:status :unknown}))]
     :advance 0
     ;; TS-3: through the gate, never raw --
     ;; `ehrt.sim-engine.encounters/gate-compiled-encounters`'s own
     ;; docstring says why the re-bracket belongs here and not in the two
     ;; opener decides.
     :prepend-steps (encounters/gate-compiled-encounters (:steps compiled))}))

(defn- citation-fields
  "M5b: :citation/:conditions ride through onto the ground-truth event
  ONLY when the compiled step actually carries them (glass-box
  traceability, components/patient-simulator/docs/gmf-interpreter.md section 6 obligations 1/3) --
  `select-keys` + a nil-dropping `into {}` keeps a hand-authored step
  (never compiled, carries neither key) producing the EXACT same event
  shape it always has, byte-identical, no perturbation for any pathway
  that predates M5b."
  [step]
  (into {} (filter val) (select-keys step [:citation :conditions])))

(defn- reason-field
  "S-1 (ADR-0151): `:reason` rides onto the ground-truth event ONLY
  when the step actually carries one, the same nil-dropping shape
  `citation-fields` uses -- and a SIBLING of it rather than a widening
  of it, deliberately. `citation-fields` scopes itself to glass-box
  TRACEABILITY of what the compiler supplied
  (components/patient-simulator/docs/gmf-interpreter.md section 6
  obligations 1/3); `:reason` is clinical content a HAND-AUTHORED step
  supplies, and `compile_trajectory`'s own `encounter->step` never sets
  one. Same shape, different reason to exist, so two functions.

  Before this, both encounter decides merged `:reason` unconditionally,
  so every module-compiled encounter emitted `:reason nil` -- present
  and empty, which is the one shape that tells a consumer nothing
  (census S-1: `:outpatient-visit` 221/221, `:admission` 48/692, those
  48 exactly the 48 carrying a citation). Dropping the key made
  `:reason` `{:optional true}` on both kinds, which `classify-change`
  calls breaking, which is why this is the whole of what the event
  contract's 1.1.0 -> 1.2.0 bump buys."
  [step]
  (into {} (filter val) (select-keys step [:reason])))

(defn- person-stamp-field
  "Arc 3a part 4: `:person-event-id` rides onto an encounter event ONLY
  when the step that produced it came from a person-stream HOOK -- the
  same nil-dropping shape `reason-field` and `citation-fields` use, and
  a third sibling rather than a widening of either, for the same reason
  they are two: this one scopes itself to PERSON provenance, which is
  neither glass-box compiler traceability nor authored clinical content.

  It is a STAMP and never a log index -- `check.clj`'s
  `person-scoped-provenance-is-a-stamp-not-a-reference` is the gate --
  and it is what makes a hook-created encounter COUNTABLE in a corpus
  without joining it back to the person stream by guesswork."
  [step]
  (into {} (filter val) (select-keys step [:person-event-id])))

;; --- arc 3a part 3: the two kinds the person stream mints ----------------
;;
;; Both steps are QUEUE-SEEDED at their own absolute `:t` by `run` (the
;; queue is already a `sorted-map` keyed `[t seq-no]`, and
;; `schedule-followup` already inserts at an absolute instant, so the
;; main loop does not change at all). Both are engine-internal, never
;; authorable pathway IR -- the same treatment `:registered` and
;; `:result-followup` get, so neither has a `sim-model/Step` entry and
;; neither passes `sim-model/valid?`.
;;
;; THE PRIOR VALUE IS READ OFF THE PATIENT, NOT OFF THE PERSON EVENT.
;; The person event carries its own `:prior-address`/`:prior-value`, and
;; using it would make section 2(e) invariant 4 (`demographic-update-
;; reports-a-real-change`) a tautology over a field this method copied.
;; Reading `world` instead makes the wire's own claim true by
;; construction, and leaves the invariant guarding the day a future
;; decide stops doing it -- which is what an invariant is for.
;;
;; AN EVENT THAT REPORTS NO CHANGE IS NOT AN EVENT (`b4f1115`, promoted
;; from the person side to the wire side): when the value is already the
;; folded state's, nothing is emitted. The step is consumed either way,
;; and no RNG is touched by either method, so nothing shifts.

(defn- demographic-target
  "The patient a queue-seeded person step may write to, or nil when it
  may not write at all. `:expired` is the one status that refuses:
  section 2(e) invariant 5 forbids a demographic event after a patient
  expires, and the run loop's own `:merged` short-circuit already ends a
  merged patient's stream before any step of theirs is decided."
  [world patient-id]
  (let [patient (get-in world [:patients patient-id])]
    (when (and patient
               (some? (:demographics patient))
               (not= :expired (:status patient)))
      patient)))

(defmethod decide :demographic-update
  [_streams t world patient-id {:keys [cause field value person-event-id]}]
  (let [patient (demographic-target world patient-id)
        prior (get (:demographics patient) field)]
    (if (or (nil? patient) (= prior value))
      {:events [] :advance 0}
      {:events [{:event :demographic-update :t t
                 :active-mrn (:active-mrn patient)
                 :cause cause
                 :field field
                 :value value
                 :prior-value prior
                 :person-event-id person-event-id
                 :participants [{:patient-id patient-id :role :subject}]}]
       :advance 0})))

(defmethod decide :coverage-change
  [_streams t world patient-id {:keys [cause payer person-event-id]}]
  (let [patient (demographic-target world patient-id)
        prior (:payer (:demographics patient))]
    (if (or (nil? patient) (= prior payer))
      {:events [] :advance 0}
      {:events [{:event :coverage-change :t t
                 :active-mrn (:active-mrn patient)
                 :cause cause
                 :payer payer
                 :prior-payer prior
                 :person-event-id person-event-id
                 :participants [{:patient-id patient-id :role :subject}]}]
       :advance 0})))

;; --- arc 3a part 4: the two clinical hooks and the identification flow ----
;;
;; ADR-0173 sections 2(c) and 2(d). THREE new step types, all
;; engine-internal and all QUEUE-SEEDED at an absolute `:t` by `run`,
;; exactly like part 3's two. None of them adds an event KIND: a hook
;; produces the ordinary `:admission`/`:delay`/`:discharge` triple, a
;; fill produces a `:demographic-update`, and an identification merge
;; produces a `:merge` in churn's own shape. The vocabulary the fold
;; grew is still exactly two.
;;
;; NONE OF THE THREE DRAWS. `:person-encounter` prepends steps and
;; emits nothing itself; the fill and the merge read `world` and emit.
;; So the hooks change WHICH patients exist and what happens to them,
;; and change no stream's consumption for any patient that would have
;; existed anyway.

(def delivery-stay-minutes
  "How long a birth encounter lasts, in MINUTES -- two days, the
  ordinary post-partum stay. A CONSTANT and not a range, deliberately:
  `decide :delay` skips the draw entirely when `:from` = `:to`
  (ADR-0171 section 2(d)), so a hook-created encounter costs no
  `:patient`-family draw and cannot shift a stream that would have
  existed without it.

  BOUNDED AT ALL is the load-bearing part. ADR-0173 section 2(c) says
  `:delivery` mints an admission and stops there; an admission with no
  discharge holds a licensed bed for the REST OF THE RUN, and with
  `:persons` present a run's horizon is the person process's own -- ten
  years by default, not the hours a clinical pathway spans. One
  unclosed birth per delivery would exhaust any facility this repo
  ships. So the hook mints an ENCOUNTER, which is what a birth is."
  2880)

(def injury-stay-minutes
  "How long an occupational-injury ED encounter lasts, in MINUTES --
  four hours. Same constant-not-a-range reasoning as
  `delivery-stay-minutes` above, and the same bounded-encounter one."
  240)

(def unidentified-stay-minutes
  "How long an UNIDENTIFIED ED presentation lasts, in MINUTES -- twelve
  hours, longer than an ordinary injury visit because nobody can
  discharge a patient they cannot name. Same constant-not-a-range and
  bounded-encounter reasoning as the two above."
  720)

(defn- hook-ward
  "Which ward a hook-created encounter admits to, by CLASS rather than
  by name: an occupational injury is an ED presentation, a birth is an
  inpatient one. Read off this run's own facility, so a config that
  renames its wards -- `demos/scenarios/ed-tuesday` does -- still gets
  a real one, and a facility carrying neither class falls back to its
  first ward rather than to a literal no facility need contain."
  [facility want]
  (let [named (fn [c] (:name (first (filter #(= c (:class %)) (:wards facility)))))]
    (or (named want) (named :inpatient) (named :ed) (:name (first (:wards facility))))))

(defmethod decide :person-encounter
  ;; ADR-0173 section 2(c). The step carries WHAT the encounter is; this
  ;; method decides WHETHER it may happen at all, and prepends the
  ;; ordinary three-step encounter when it may.
  ;;
  ;; THE GUARD WAS THIS PROJECT'S SINGLE-ENCOUNTER HORIZON, met a
  ;; second time -- `(not= :new (:status patient))`, which
  ;; `check.clj`'s `admission-only-when-new` (sim/ADR-0007 point 3)
  ;; asserted over the finished log. ARC 3B SWEEP 1 LIFTS IT, behind
  ;; the `:encounters` opt-in: `encounter-openable?` is the same
  ;; question asked of the ENCOUNTER rather than of the patient, and
  ;; with no opt-in it IS the `:new` test, verbatim. So a hook landing
  ;; on a patient who has already been DISCHARGED now opens their second
  ;; encounter -- and one landing while their first is still OPEN still
  ;; mints nothing, which is the half of the old rule that survives.
  ;; `run` also refuses
  ;; these statically, before the run, for a patient whose own queue
  ;; contains an encounter at all (`prelude`'s `encounter-free?`); this
  ;; guard is the runtime half, and the two are deliberately both
  ;; present -- a static analysis that turns out to be wrong shows up
  ;; here as a skipped encounter rather than as a red invariant.
  ;;
  ;; THE WHOLE TRIPLE IS PREPENDED OR NONE OF IT IS. A `:delay` and a
  ;; `:discharge` queued behind an admission that did not happen would
  ;; be a discharge with no admission, which
  ;; `discharge-follows-admission` correctly calls a defect -- so the
  ;; encounter is one decision, not three steps that each guard
  ;; themselves.
  [_streams _t world patient-id {:keys [reason ward-class stay-minutes person-event-id]}]
  (let [patient (get-in world [:patients patient-id])]
    (if-not (encounters/encounter-openable? world patient)
      {:events [] :advance 0}
      {:events [] :advance 0
       :prepend-steps [{:type :admission
                        :location (hook-ward (:facility world) ward-class)
                        :reason reason
                        :person-event-id person-event-id}
                       {:type :delay :from stay-minutes :to stay-minutes}
                       {:type :discharge}]})))

(defmethod decide :repeat-arrival
  ;; ARC 3B SWEEP 1 (ADR-0174 section 2(a), ruling A1). A REPEAT ARRIVAL
  ;; -- an arrival ordinal whose person already has a patient -- queued
  ;; NOTHING before this sweep, because a second `:admission` for a
  ;; `:discharged` patient violated `admission-only-when-new`
  ;; (ADR-0173's own first tabled deviation, and 22 of ed-tuesday's 100
  ;; configured arrivals plus 39 of clinic-decade's 200).
  ;;
  ;; With `:encounters` on, that arrival's whole step list is queued
  ;; behind THIS one step, which is the runtime guard: THE WHOLE ARRIVAL
  ;; IS PREPENDED OR NONE OF IT IS, exactly as `decide :person-encounter`
  ;; prepends its whole triple or nothing. A repeat arrival landing while
  ;; the patient's FIRST encounter is still open opens no second one --
  ;; a delay and a discharge queued behind an admission that did not
  ;; happen would be a discharge closing the wrong encounter, so the
  ;; arrival is one decision rather than a list of steps that each guard
  ;; themselves.
  ;;
  ;; It emits NO event and it mints no second `:registered`, which is
  ;; what keeps `registered-is-every-patients-first-event` true by
  ;; construction: a second encounter is a second VISIT by one patient,
  ;; not a second patient.
  ;;
  ;; ARC 3B SWEEP 3: a SCHEDULED repeat arrival is booked behind an
  ;; `:appointment`, so this step can arrive already stamped -- and the
  ;; stamp has to reach the OPENER inside, not stop here, or an opener
  ;; would carry no reference and `scheduled-encounter-follows-its-
  ;; appointment` would go quietly vacuous on exactly the arrivals it
  ;; most needs to judge.
  [_streams _t world patient-id {:keys [steps appointment-id]}]
  (let [patient (get-in world [:patients patient-id])
        stamped (if (and appointment-id (seq steps))
                  (into [(assoc (first steps) :appointment-id appointment-id)] (rest steps))
                  steps)]
    (if (encounters/encounter-openable? world patient)
      {:events [] :advance 0 :prepend-steps stamped}
      {:events [] :advance 0})))


;; --- ARC 3B SWEEP 3 (ADR-0174 section 2(b)): SCHEDULING as skeleton
;; STATE. Four kinds, none of which renders a message in v1 (ruling C --
;; MSH-12 says "2.3" and the SIU structures are v2.4, which stays rowed
;; for arc 4).
;;
;; ONE STEP CARRIES A WHOLE ARRIVAL, exactly as `:repeat-arrival`'s does
;; and for exactly its reason: the visit behind an appointment happens or
;; it does not, and a `:delay` plus a `:discharge` queued behind an
;; opener that never fired would be a discharge with no admission. So the
;; appointment is ONE decision, not a list of steps that each guard
;; themselves.
;;
;; TWO `:patient` DRAWS PER APPOINTMENT, ALWAYS, IN THIS ORDER -- one
;; uniform for the outcome and one for the reschedule offset, the second
;; taken whether or not the first selected a reschedule. That is the
;; FIXED-CONSUMPTION law (`turnaround-seconds`' own choice, and
;; deliberately unlike `decide :delay`'s dead-draw skip): a site that
;; retunes `:cancel-rate` must not shift any OTHER patient's stream, and
;; under a per-patient stream it cannot shift this one's either.
;;
;; THE THREE RATES ARE BANDS OF THE ONE UNIFORM, which is what makes
;; `appointment-reaches-at-most-one-terminal` true by construction: one
;; draw cannot land in two bands, so cancelled/no-showed/kept are
;; mutually exclusive in the STATE and not merely asserted over the log.

(defn- appointment-outcome
  "Which band this appointment's outcome uniform fell in. The order is
  part of the contract, because moving a boundary moves every band after
  it: cancel, then reschedule, then no-show, then kept as the remainder.
  `valid-scheduling?` is what guarantees the remainder is not negative."
  [u {:keys [cancel-rate reschedule-rate no-show-rate]}]
  (cond (< u cancel-rate)                                          :cancelled
        (< u (+ cancel-rate reschedule-rate))                      :rescheduled
        (< u (+ cancel-rate reschedule-rate no-show-rate))         :no-show
        :else                                                      :kept))

(defn days->seconds [d] (* 86400 (long d)))

(defmethod decide :appointment
  ;; The step carries WHAT is being booked (`:lead-seconds`,
  ;; `:appointment-class`, `:reason`) and WHAT RUNS at `:scheduled-t` if
  ;; it is kept (`:steps`). Both of this sweep's two producers build the
  ;; same step: the pre-loop, for an arrival the scheduled-vs-walk-in
  ;; Bernoulli made a booking, and `decide :discharge`, for a follow-up.
  ;;
  ;; A RESCHEDULE SHARES THE BOOKING'S INSTANT, and that is stated rather
  ;; than hidden: booking and re-booking are one decide, so the two events
  ;; land in one batch. Log ORDER distinguishes them -- the `:appointment`
  ;; is emitted first, which is what
  ;; `appointment-reference-resolves` reads -- and `:prior-scheduled-t`
  ;; carries what moved. Placing the reschedule partway to the original
  ;; instant would need a second queue entry and would buy no invariant.
  [{rng :patient} t world patient-id {:keys [lead-seconds appointment-class reason steps]}]
  (let [patient (get-in world [:patients patient-id])
        scheduling (:scheduling world)
        [lo hi] (:lead-time-days scheduling)
        appointment-id (:appointment-id (streams/minted-appointment-id-field world patient-id))
        ;; THE TWO DRAWS. Both, always, in this order.
        u (.nextDouble ^Random rng)
        move-days (streams/rand-int-in rng lo hi)
        outcome (appointment-outcome u scheduling)
        scheduled-t (+ t lead-seconds)
        moved-t (+ scheduled-t (days->seconds move-days))
        final-t (if (= :rescheduled outcome) moved-t scheduled-t)
        booking (cond-> {:event :appointment :t t :active-mrn (:active-mrn patient)
                         :appointment-id appointment-id
                         :scheduled-t scheduled-t
                         :appointment-class appointment-class
                         :participants [{:patient-id patient-id :role :subject}]}
                  reason (assoc :reason reason))
        terminal (fn [kind]
                   {:event kind :t t :active-mrn (:active-mrn patient)
                    :appointment-id appointment-id
                    :participants [{:patient-id patient-id :role :subject}]})
        ;; THE CARRIED STEPS GO BEHIND `:repeat-arrival`, ALWAYS, and this
        ;; is the one thing here that was FOUND rather than designed.
        ;;
        ;; The visit behind a booking runs at `:scheduled-t`, not at the
        ;; booking instant -- so whether an encounter may open then is a
        ;; question about a world THIS decide cannot see. Prepending the
        ;; opener unguarded produced exactly the defect the guard exists
        ;; for: `bin/demo-exerciser-ed-tuesday` went
        ;; `:self-check-failed` on `admission-only-when-no-open-encounter`
        ;; for a follow-up visit that opened while its patient's own
        ;; encounter was still open, with a cascade of
        ;; `outpatient-patients-occupy-no-bed` behind it.
        ;;
        ;; `:repeat-arrival` is ALREADY that guard, asked at the right
        ;; instant, and it already propagates this stamp inward -- so the
        ;; steps are routed through it rather than through a second copy
        ;; of the same question. A step list that is already wrapped (the
        ;; pre-loop's own repeat-arrival case) is left alone rather than
        ;; double-wrapped.
        ;;
        ;; AN APPOINTMENT WHOSE VISIT THE GUARD REFUSES STAYS OPEN and
        ;; reaches no terminal, which is correct and not a gap: a booking
        ;; whose visit could not happen is a real thing, and
        ;; `appointment-reaches-at-most-one-terminal` asks for AT MOST
        ;; one, never exactly one.
        stamped (when (seq steps)
                  (let [inner (if (= :repeat-arrival (:type (first steps)))
                                (vec steps)
                                [{:type :repeat-arrival :steps (vec steps)}])]
                    (into [(assoc (first inner) :appointment-id appointment-id)]
                          (rest inner))))]
    (case outcome
      :cancelled  {:events [booking (terminal :appointment-cancel)] :advance 0}
      :no-show    {:events [booking] :advance lead-seconds
                   :prepend-steps [{:type :no-show :appointment-id appointment-id}]}
      :rescheduled {:events [booking
                             {:event :reschedule :t t :active-mrn (:active-mrn patient)
                              :appointment-id appointment-id
                              :prior-scheduled-t scheduled-t
                              :scheduled-t moved-t
                              :participants [{:patient-id patient-id :role :subject}]}]
                    :advance (- final-t t)
                    :prepend-steps stamped}
      {:events [booking] :advance lead-seconds :prepend-steps stamped})))

(defmethod decide :no-show
  ;; Emitted AT `:scheduled-t` (ADR-0174 section 2(b)'s own table) and
  ;; opening nothing -- which is precisely why a no-show cannot be
  ;; DERIVED from an encounter, the alternative that section rejects.
  ;; It draws nothing: its outcome was decided at the booking.
  [_streams t world patient-id {:keys [appointment-id]}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [{:event :no-show :t t :active-mrn (:active-mrn patient)
               :appointment-id appointment-id
               :participants [{:patient-id patient-id :role :subject}]}]
     :advance 0}))

(defn- identity-fill-outcome
  "The `:fill` branch's own outcome, factored because TWO decides reach
  it: `:identity-fill` directly, and `:identification-merge` when the
  world refuses the merge (ADR-0173 section 2(d)'s \"a merge with no
  survivor degenerates to a fill\", answered at DECIDE time and not only
  at queue-seeding time -- the survivor can be merged away or expire
  between the two)."
  [t world patient-id {:keys [persona residence person-event-id]}]
  (let [patient (demographic-target world patient-id)]
    (if (or (nil? patient) (not= :placeholder (:identity (:demographics patient))))
      {:events [] :advance 0}
      {:events [(cond-> {:event :demographic-update :t t
                         :active-mrn (:active-mrn patient)
                         :cause :identity-fill
                         :field :identity
                         :value :known
                         :prior-value :placeholder
                         :placeholder-event-id (get-in world [:registration-index patient-id])
                         :persona persona
                         :person-event-id person-event-id
                         :participants [{:patient-id patient-id :role :subject}]}
                  (and residence (not= :housed (:status residence)))
                  (assoc :residence residence))]
       :advance 0})))

(defmethod decide :identity-fill
  ;; ADR-0173 section 2(d), the `:fill` branch of `:identity-resolution`:
  ;; the placeholder patient KEEPS their patient-id and their MRN, and
  ;; every demographic field is filled in from the person's real
  ;; demographics at this instant.
  ;;
  ;; ONE EVENT, NOT SEVEN. A fill is not six independent field changes
  ;; that happen to coincide; it is one fact -- this record now belongs
  ;; to a known person -- so `:field` is `:identity` and `:value` is
  ;; `:known`, with the demographics themselves riding as the `:persona`
  ;; the record should have had all along. `evolve` (now
  ;; `ehrt.sim-engine.evolve`) rebuilds the whole state from it, and
  ;; `demographic-update-reports-a-real-change`
  ;; still has something true to check: `:prior-value` is `:placeholder`,
  ;; which is exactly what the fold says it was.
  ;;
  ;; `:placeholder-event-id` IS A LOG INDEX -- the one referential key
  ;; this arc mints -- and it comes from `run`'s fold-carried
  ;; `:registration-index` rather than from a scan, the same shape
  ;; ADR-0169 gave `:citation-index`. A hand-built world carrying no
  ;; such KEY answers nil, which
  ;; `identity-fill-references-its-placeholder-registration` reports as
  ;; a dangling reference -- correctly, because it would be one.
  [_streams t world patient-id step]
  (identity-fill-outcome t world patient-id step))

(defmethod decide :identification-merge
  ;; ADR-0173 section 2(d), the `:merge` branch. The event is churn's own
  ;; `:merge` -- same kind, same `:survivor`/`:merged` roles, same
  ;; `:surviving-mrn`/`:merged-mrn`/`:merged-mrns` payload -- so
  ;; `merge-survivor-absorbs-merged-mrns`, `no-events-after-merged-
  ;; terminal`, the run loop's own `:merged` short-circuit and the whole
  ;; post-merge shadow surface apply verbatim. `:cause :identification`
  ;; is the ONLY thing that distinguishes it, and
  ;; `identification-merge-survivor-is-the-persons-prior-patient` is
  ;; what makes that marker mean something.
  ;;
  ;; A SEPARATE DECIDE, AND NOTHING ADDED TO CHURN'S LOTTERY. `decide
  ;; :merge`'s own `never-mergeable?` excludes `:new`, and a placeholder
  ;; patient who registered and was never admitted is exactly `:new`;
  ;; relaxing that would move every churn corpus for an unrelated
  ;; reason, which this arc has no licence to do. So this is one more
  ;; decide method with its own guard, and `churn/inject`'s step-type
  ;; set and roll order are untouched.
  ;;
  ;; THE STEP IS QUEUED ON THE PLACEHOLDER, not on the survivor, and
  ;; that placement is load-bearing rather than incidental. The run
  ;; loop SHORT-CIRCUITS a queue entry whose patient is already
  ;; `:merged`, so a step queued on the survivor would vanish silently
  ;; the moment churn merged that survivor away -- leaving the
  ;; placeholder unresolved past its own close instant, which is a real
  ;; violation of `every-placeholder-registration-is-resolved-or-still-
  ;; open` and was found exactly that way, at population scale, by a
  ;; corpus probe rather than by reasoning. Queued on the placeholder,
  ;; the same event still names both patients and the DEGENERATE case
  ;; is reachable.
  ;;
  ;; A MERGE THE WORLD REFUSES DEGENERATES TO A FILL. Section 2(d)
  ;; states that rule for the case with no survivor at all, and
  ;; `prelude` applies it there; this is the same rule at decide time,
  ;; for a survivor who existed when the step was seeded and has since
  ;; been merged away or expired. Either the placeholder is joined to
  ;; the person's other record or it is filled in place -- what it may
  ;; never be is silently left dangling.
  [_streams t world patient-id {:keys [survivor-patient-id person-event-id] :as step}]
  (let [{:keys [patients]} world
        survivor (get patients survivor-patient-id)
        merged (demographic-target world patient-id)]
    (if (or (nil? survivor) (nil? merged)
            (= patient-id survivor-patient-id)
            (#{:merged :expired} (:status survivor))
            (not= :placeholder (:identity (:demographics merged))))
      (identity-fill-outcome t world patient-id step)
      {:events [{:event :merge :t t
                 :cause :identification
                 :person-event-id person-event-id
                 :participants [{:patient-id survivor-patient-id :role :survivor}
                                {:patient-id patient-id :role :merged}]
                 :surviving-mrn (:active-mrn survivor)
                 :merged-mrn (:active-mrn merged)
                 :merged-mrns (:mrns merged)}]
       :advance 0})))


;; --- ARC 3B SWEEP 2 (ADR-0174 section 2(c)): the BED-STATUS CYCLE ---------
;;
;; `world` gains `:beds`, bed-id -> {:status :since-t :last-patient-id},
;; every licensed bed and surge slot born `:ready`. It is the one place
;; arc 3b keeps state the log cannot re-derive, and the asymmetry is
;; deliberate and worth stating IN THE CODE rather than leaving for a
;; reader to notice: `occupancy-board`'s consistency law -- recomputing
;; from `patients` from scratch always equals this -- still holds for the
;; OCCUPIED half (`:occupied` iff some patient's `:location` names the
;; bed), while `:dirty`, `:cleaning` and `:ready` have no patient to be
;; derived from at all. That is what ADR-0174's "world-level" means.
;;
;; NIL UNLESS THE RUN OPTED IN, exactly like `:encounter-minting` above
;; it: no index, no `:ready` gate (`sim-model/free` falls back to
;; "nobody is in it"), no `:bed-status-change` event, no turnaround
;; draw, and therefore the bytes this engine has always produced.

(defn- bed-status
  "`bed`'s status in this world's index, or nil when the run carries no
  index at all. The ONE reader -- every branch below asks through it."
  [world bed]
  (get-in world [:beds bed :status]))

(defn- bed-status-change
  "One `:bed-status-change` event (ADR-0174 section 2(c): ONE kind, many
  causes, the same choice ADR-0173 made for `:demographic-update`).

  ITS PARTICIPANT NAMES A BED AND NOT A PATIENT, which is the
  vocabulary widening this sweep pays for. `every-event-has-participants`
  is satisfied -- a bed event has one -- and every patient-keyed reader
  in the tree filters on `:patient-id` being PRESENT rather than
  assuming it (`ehrt.sim-check.check`'s own `events-by-patient`,
  `participants-of` and `participant-ids-exist-in-run`;
  `ehrt.sim-engine.fold/replay` and `run`'s own two in-loop folds).

  `:last-patient-id` rides the `:dirty` transition ALONE: it is who left
  the bed, and the two later legs of the cycle are housekeeping's, not
  anybody's."
  [t bed ward from to last-patient-id]
  (cond-> {:event :bed-status-change :t t :bed bed :ward ward :from from :to to
           :participants [{:bed-id bed :ward ward :role :subject}]}
    last-patient-id (assoc :last-patient-id last-patient-id)))

(defn- turnaround-seconds
  "One leg of `ward-name`'s turnaround, in SECONDS, drawn on the
  `:facility` stream (ADR-0174 ruling D1: the family for draws that read
  no patient state at all, kept distinct from `:world` by ADR-0171
  ruling E1 precisely so that a ward-config edit does not shift arrival
  gaps or bed choices).

  FIXED CONSUMPTION, always one draw -- deliberately UNLIKE `decide
  :delay`, which skips an arithmetically-dead `lo` = `hi` draw. That
  skip was paid for by a whole-corpus reshuffle in its own commit;
  drawing unconditionally here means a site tuning one ward to a fixed
  turnaround shifts no other ward's cycle and no other patient's stream."
  [facility-rng facility ward-name]
  (let [[lo hi] (sim-model/turnaround-minutes (sim-model/ward-by-name facility ward-name))]
    (* 60 (streams/rand-int-in facility-rng lo hi))))

(defn- vacate-bed
  "Seeds `location`'s bed into the turnaround cycle: the `:dirty`
  transition NOW, and a queue entry for the `:cleaning` leg at
  `t` + d1. Returns `{:events [..] :schedule-followup {..}}`, or nil
  when this run carries no bed index or the location names no bed.

  nil IS THE WHOLE OF THE NO-OPT-IN PATH. Every caller merges this in
  rather than branching on the opt-in itself, so the expression that
  runs with `:bed-cycle` absent is the one that was there before this
  sweep, character for character.

  THE TWO VACATING CLASSES AND NO OTHERS call this -- `:discharge` and
  `:transfer` (`reinstatable-event-types`' own set, and for the same
  reason: they are what vacates). `:cancel-admit` and `:cancel-transfer`
  also leave a bed empty and deliberately do NOT come here: they are
  CORRECTIONS, and an occupancy a cancel says did not happen leaves no
  dirt behind it. Those two return their bed straight to `:ready`, in
  the run loop's own fold, with no event and no cycle -- named here so
  the omission reads as a decision."
  [facility-rng world t location last-patient-id]
  (when-let [bed (and (:beds world) (:bed location))]
    (let [ward (:ward location)]
      {:events [(bed-status-change t bed ward (bed-status world bed) :dirty last-patient-id)]
       :schedule-followup {:t (+ t (turnaround-seconds facility-rng (:facility world) ward))
                           :patient-id nil
                           :steps [{:type :bed-cleaning :bed bed :ward ward}]}})))

(defn- waiting-boarder
  "The longest-waiting BOARDER of `ward-name` -- an admitted patient
  who IS IN A BED, whose `:home-ward` is this ward, and whose current
  `:location` is somewhere else -- or nil. Extracted verbatim from
  `decide :discharge`, where it was inline, because the bed cycle asks
  the same question at a different instant: the READY event, not the
  discharge.

  `excluded-id` is the patient whose own departure is being processed,
  who must not be considered their own boarder. nil excludes nobody,
  which is what the READY instant wants: by then the patient who left
  is discharged and holds no location at all.

  TS-2 (traffic-scale close, 2026-08-29, section 9): THE `some?` CLAUSE
  IS THE FIX, and it took a 10^5 run to need it. A boarder is an
  admitted patient sitting in a bed that is not their home ward's; a
  patient in NO bed is boarding nowhere, and asking `not=` of a nil
  location silently answered yes.

  The population that answered yes wrongly is exactly the OPEN
  OUTPATIENT ENCOUNTERS. `evolve :outpatient-visit` sets `:status
  :admitted` (M5b re-used the existing status values rather than
  inventing one, `:class :outpatient` being the distinguishing fact)
  and sets no `:location`; `evolve :discharge` nils `:location` but
  leaves `:home-ward` standing. So a patient discharged from Medicine A
  who returns for a follow-up visit is, for the duration of that visit,
  `:admitted` with a nil location and a stale Medicine A home ward --
  and this predicate pulled them into the next bed that freed there,
  emitting a `:transfer` with `:from nil`. Worse, their STALE
  `:admitted-at` is from the earlier inpatient stay, so
  `sort-by` ranked them AHEAD of every genuine boarder.

  `some?` and not `(not= :outpatient (:class p))` deliberately: the
  invariant `admitted-occupies-one-slot` already says an admitted
  patient's location is never nil EXCEPT for an outpatient, so over
  `:admitted` patients the two predicates select the same set -- and
  this one says what a boarder IS rather than which class happens to
  be the exception today.

  DRAWS. This function consumes none, and the branch it now takes more
  often is the pre-existing zero-draw one: both callers already emit
  nothing and draw nothing when `waiting-boarder` returns nil (`decide
  :bed-ready`'s `{:events [ready-event]}`, and `decide :discharge`'s
  own nil `waiting-id`). So no draw is SKIPPED differently -- a draw
  that should never have been made is not made. For a genuine boarder
  nothing changes at all: removing a non-boarder from the candidate set
  cannot reorder the rest. Where a false boarder was outranking a real
  one, the real one now gets the bed, which is the correction itself
  and not a side effect of it. `bin/ground-truth-bracket` is what says
  whether any shipped corpus was reaching this at all."
  [world excluded-id ward-name]
  (->> (:patients world)
       (remove (fn [[pid _]] (= pid excluded-id)))
       (filter (fn [[_ p]] (and (= :admitted (:status p))
                                (some? (get-in p [:location :ward]))
                                (not= (:home-ward p) (get-in p [:location :ward]))
                                (= ward-name (:home-ward p)))))
       (sort-by (fn [[pid p]] [(:admitted-at p) pid]))
       ffirst))

(defn- appointment-ref-field
  "What an opener carrying a scheduled arrival's own appointment merges
  in: `{:appointment-id ...}`, or `{}` for a walk-in. The step is stamped
  by `decide :appointment` (never by the pre-loop), so an opener can only
  carry an id an `:appointment` event already minted EARLIER in this
  patient's own log -- which is `scheduled-encounter-follows-its-
  appointment` holding by construction rather than by assertion."
  [step]
  (if-let [id (:appointment-id step)] {:appointment-id id} {}))

(defmethod decide :admission
  [{world-rng :world facility-rng :facility} t world patient-id
   {:keys [location force-placement] :as step}]
  ;; ADR-0171: the bed choice is WORLD (its candidate set is `free`
  ;; against a board built from EVERY patient), the attending is
  ;; FACILITY (ward-eligible providers, no patient state read) -- ruling
  ;; E1's split is by what the draw READS, not by what it is named after.
  (let [{:keys [facility providers patients]} world
        board (sim-model/occupancy-board patients)
        ;; ARC 3B SWEEP 2: `(:beds world)` is nil with no `:bed-cycle`
        ;; opt-in, and `allocate`'s own 6-arity is then the 5-arity
        ;; verbatim -- one of the four `allocate` call sites
        ;; `no-assignment-to-a-non-ready-bed` names.
        alloc (sim-model/allocate world-rng facility board (:beds world) location force-placement)]
    (if (:exhausted alloc)
      (exhausted-outcome patient-id location facility board)
      (let [ward-id (:id (sim-model/ward-by-name facility (:home-ward alloc)))
            attending (sim-model/choose-attending facility-rng providers ward-id)
            active-mrn (get-in patients [patient-id :active-mrn])]
        {:events [(merge {:event :admission :t t :active-mrn active-mrn :attending attending
                          :participants [{:patient-id patient-id :role :subject}]}
                         alloc (reason-field step) (citation-fields step)
                         (person-stamp-field step)
                         ;; ARC 3B SWEEP 1: an opener MINTS its encounter
                         ;; id (ADR-0174 ruling B1); every later event of
                         ;; that encounter is stamped with it by `run`'s
                         ;; own loop, off the open record.
                         (streams/minted-encounter-id-field world patient-id)
                         ;; ARC 3B SWEEP 3: and NAMES the appointment it
                         ;; was kept against, when it had one.
                         (appointment-ref-field step))]
         :advance 0}))))

(defmethod decide :delay
  [{rng :patient} _t _world _patient-id {:keys [from to]}]
  ;; :from/:to are authored in MINUTES (pathway.clj IR, unchanged --
  ;; sim/ADR-0011 decision 1's authoring-ergonomics carve-out); the engine
  ;; converts to SECONDS here, the one place a minute-denominated draw
  ;; becomes a clock advance.
  ;;
  ;; ADR-0171 section 2(d): when :from = :to the draw is ARITHMETICALLY
  ;; DEAD -- `rand-int-in` evaluates `(.nextInt rng 1)`, which is always
  ;; 0 -- so it is skipped, and the step advances by the authored
  ;; constant. Free in outcome, costly in stream position, hence
  ;; draw-affecting, hence landed in the partition's own commit and
  ;; never before or after it (one reshuffle, ruling F1).
  ;;
  ;; This does NOT breach the fixed-consumption law `assign-pathway` and
  ;; `churn/roll-gap` state. That law exists so draw count never depends
  ;; on DATA; :from = :to is not data but the authored SHAPE of a step,
  ;; as visible as the step itself, and under a per-patient stream it
  ;; cannot reach any other patient.
  {:events []
   :advance (* 60 (if (= from to) from (streams/rand-int-in rng from to)))})

(defmethod decide :transfer
  ;; ARC 3B SWEEP 2 (ADR-0174 ruling D1): `:facility` joins because the
  ;; TURNAROUND draw for the bed this transfer vacates is a facility
  ;; draw -- it reads a ward's config and no patient state at all.
  [{world-rng :world facility-rng :facility} t world patient-id {:keys [location force-placement]}]
  (let [{:keys [facility patients]} world
        board (sim-model/occupancy-board patients)
        patient (get patients patient-id)
        alloc (sim-model/allocate world-rng facility board (:beds world) location force-placement)]
    (if (:exhausted alloc)
      (exhausted-outcome patient-id location facility board)
      ;; ARC 3B SWEEP 2: a transfer VACATES the bed it came from, so it
      ;; seeds that bed's turnaround cycle exactly as a discharge does
      ;; -- `vacate-bed` is nil with no `:bed-cycle`, and `cond->`/
      ;; `merge` then leave this expression the one that was here.
      (let [vacated (vacate-bed facility-rng world t (:location patient) patient-id)]
        (merge {:events (into [(merge {:event :transfer :t t :active-mrn (:active-mrn patient)
                                       :from (:location patient)
                                       :attending (:attending patient) :bed-ready false
                                       :participants [{:patient-id patient-id :role :subject}]}
                                      alloc)]
                              (:events vacated))
                :advance 0}
               (select-keys vacated [:schedule-followup]))))))

(defn- death-disposition-fields
  "Wave C (2026-08-02, ADR-0028, C3): :disposition/:codes ride onto the
  ground-truth :discharge event ONLY when the compiled step actually
  carries them (compile-trajectory.clj's own death->step, the two new
  optional fields sim-model/pathway.clj's :discharge schema gained) --
  the same nil-dropping merge `citation-fields` already establishes,
  applied to this step type's own two new fields."
  [step]
  (into {} (filter val) (select-keys step [:disposition :codes])))

(defn- bed-ready-location
  "Where a bed-ready transfer actually places `waiting-id`, once
  `patient-id`'s discharge has vacated `vacated-location`.

  Normally the just-vacated bed itself: that specific bed becoming ready
  IS the coupling (components/sim/docs/operational-models.md's own \"patient B's
  discharge event is what makes patient A's boarding-to-transfer event
  schedulable\"). But the coupling names the bed WITHIN its rung -- it
  never licenses a rung the allocation ladder would not have reached.
  A vacated SURGE slot is rung 2, legal only \"once licensed beds are
  full\" (same document, ladder rung 2), and a licensed bed in the
  boarder's home ward -- which is this ward, since that is how
  `waiting-id` was chosen -- can be free at this instant: some OTHER
  coupling can vacate one with no boarder pulled into it (a bed-ready
  transfer's own origin bed triggers no second search), and under
  `--churn` a :cancel-admit or :cancel-transfer can vacate one outright.
  Handing over the surge slot then places on rung 2 with rung 1 free,
  which is exactly what `ehrt.sim-check.check/surge-only-when-earlier-
  rungs-exhausted` forbids (ADR-0153, seed 202 under `--churn` at
  `t 78480`). In that case the ladder decides, drawing its own seeded
  bed choice the way every other placement does.

  `allocate` can never come back `:exhausted` here: the vacated bed is
  in `waiting-id`'s own home ward, so rung 1 or rung 2 always has at
  least that one candidate -- and since rung 1 is free by the branch
  we are in, the result is always a licensed bed in that same ward.

  ARC 3B SWEEP 2 (ADR-0174 section 2(c)), and THE PROOF ABOVE HAD TO BE
  RE-READ UNDER THE NEW PREDICATE, not merely re-typed. `free` now means
  `:ready` when the run carries a bed index, so \"rung 1 or rung 2 always
  has at least that one candidate\" is a claim about the VACATED bed's
  own status -- and it holds, because with the cycle on this function is
  reached only from `decide :bed-ready`, which passes a world in which
  that bed has just BECOME `:ready`. With the cycle off, `beds` is nil,
  `free` is `(remove board ids)` verbatim, and the original proof stands
  unchanged word for word.

  `home-licensed-free?` goes through `sim-model/free` rather than
  repeating `(remove board ...)`: ADR-0174 names this probe specifically
  as one that must ask the same question the ladder asks."
  [world-rng world patient-id waiting-id vacated-location]
  (let [facility (:facility world)
        beds (:beds world)
        home-ward-name (get-in world [:patients waiting-id :home-ward])
        board (sim-model/occupancy-board (dissoc (:patients world) patient-id))
        home-ward (sim-model/ward-by-name facility home-ward-name)
        home-licensed-free? (boolean (seq (sim-model/free (sim-model/licensed-bed-ids home-ward) board beds)))]
    (if (and (= :surge (:placement vacated-location)) home-licensed-free?)
      (:location (sim-model/allocate world-rng facility board beds home-ward-name nil))
      vacated-location)))

(defn- bed-ready-transfer-event
  "The paired bed-ready `:transfer` -- `waiting-id` pulled into the bed
  `vacated-location` names, decided at `t`. Extracted verbatim from
  `decide :discharge`, where it was an inline map, because ARC 3B SWEEP
  2 moves WHEN it is decided (the READY instant, not the discharge) and
  changes nothing about WHAT it is."
  [world-rng world excluded-id waiting-id vacated-location t]
  (let [location (bed-ready-location world-rng world excluded-id waiting-id vacated-location)]
    {:event :transfer :t t
     :active-mrn (:active-mrn (get-in world [:patients waiting-id]))
     :from (:location (get-in world [:patients waiting-id]))
     :attending (:attending (get-in world [:patients waiting-id]))
     :home-ward (get-in world [:patients waiting-id :home-ward])
     :location location
     :placement (:placement location)
     :forced false
     :bed-ready true
     :participants [{:patient-id waiting-id :role :subject}]}))

(defmethod decide :discharge
  ;; ARC 3B SWEEP 2 (ADR-0174 section 2(c)): `:facility` joins for the
  ;; turnaround draw, and this is the ONE existing behaviour arc 3b
  ;; CHANGES rather than extends. With `:bed-cycle` on, the paired
  ;; bed-ready transfer is no longer emitted here at all -- the bed goes
  ;; `:dirty` and the transfer is decided at the READY instant, against
  ;; the board as it stands THEN. That is more correct independently of
  ;; the cycle: today's coupling picks the longest-waiting boarder at
  ;; the discharge instant and hands them a bed they occupy in the same
  ;; second, with no opportunity for the world to have changed.
  ;;
  ;; ARC 3B SWEEP 3 (ADR-0174 section 2(b)): and `:patient` joins, for the
  ;; FOLLOW-UP -- the first producer of a SCHEDULED second encounter this
  ;; repository has had. TWO draws, always, whenever the run opted into
  ;; `:scheduling`: the follow-up Bernoulli and the interval, the second
  ;; taken whether or not the first fired. Same fixed-consumption law the
  ;; appointment's own two draws follow, and for the same reason.
  [{world-rng :world facility-rng :facility rng :patient} t world patient-id step]
  (let [patient (get-in world [:patients patient-id])
        ;; C3: an expired-disposition discharge vacates NO bed --
        ;; patient-state-model.md's own "clinically absorbing but
        ;; operationally alive" fact -- so the bed-ready-transfer
        ;; coupling below MUST NOT fire; unguarded, it would double-
        ;; occupy a bed no-double-occupancy already forbids.
        expired? (= :expired (:disposition step))
        discharge-event (merge {:event :discharge :t t :active-mrn (:active-mrn patient)
                                 :location (:location patient) :attending (:attending patient)
                                 :participants [{:patient-id patient-id :role :subject}]}
                                (citation-fields step)
                                (death-disposition-fields step))
        vacated-ward (get-in patient [:location :ward])
        vacated-location (:location patient)
        ;; The cycle takes the coupling over: with an index present the
        ;; bed goes `:dirty` here and NOBODY is pulled into it yet.
        vacated (when-not expired? (vacate-bed facility-rng world t vacated-location patient-id))
        waiting-id (when (and (not expired?) (nil? (:beds world)))
                     (waiting-boarder world patient-id vacated-ward))
        ;; A follow-up is booked at the DISCHARGE INSTANT and its visit
        ;; runs at an ABSOLUTE later one, so it rides `:schedule-followup`
        ;; exactly as the auto-paired `:result` and the bed cycle's own
        ;; two legs do. An EXPIRED discharge books nothing: a return visit
        ;; for somebody who died is the one shape this must not produce.
        follow-up (when-let [{:keys [rate interval-days]} (:follow-up (:scheduling world))]
                    (let [u (.nextDouble ^Random rng)
                          days (streams/rand-int-in rng (first interval-days) (second interval-days))]
                      (when (and (< u rate) (not expired?))
                        {:t t :patient-id patient-id
                         :steps [{:type :appointment
                                  :lead-seconds (days->seconds days)
                                  :appointment-class :outpatient
                                  :reason "Follow-up"
                                  :steps [{:type :outpatient-visit :reason "Follow-up"}
                                          {:type :delay :from 20 :to 20}
                                          {:type :outpatient-visit-end}]}]})))
        ;; TWO followups can now be owed by ONE decide -- the bed the
        ;; discharge dirtied, and the return visit it booked -- so this
        ;; hands the loop a VECTOR. See the loop's own comment: it takes
        ;; one map or many, and one map is what every other site still
        ;; hands it.
        followups (into [] (remove nil?) [(:schedule-followup vacated) follow-up])]
    (cond->
     {:events (cond-> [discharge-event]
                waiting-id
                (conj (bed-ready-transfer-event world-rng world patient-id waiting-id vacated-location t))
                (seq (:events vacated))
                (into (:events vacated)))
      :advance 0}
      (seq followups) (assoc :schedule-followup followups))))

;; --- ARC 3B SWEEP 2: the cycle's own two steps. Neither names a
;; patient: their queue entries carry `:patient-id` nil, they draw on
;; no `:patient` stream, and the run loop reaches them exactly as it
;; reaches an `:order`'s auto-paired `:result` -- through
;; `schedule-followup`, at their own `[t seq-no]`.
;;
;; EACH LEG GUARDS ON THE STATUS IT EXPECTS, and that guard is not
;; belt-and-braces: a `:cancel-discharge` can reinstate a patient into
;; a bed whose cycle is already in flight (the dirty->occupied arc
;; ADR-0174's invariant 3 carves out), and the tick that then fires
;; must do NOTHING rather than drag an occupied bed to `:cleaning`.
;;
;; THE CYCLE HAS TWO IN-FLIGHT LEGS AND THE CANCEL CAN LAND IN EITHER.
;; The sentence above says `dirty->occupied` because that is the leg
;; ADR-0174 carved out; a cancel landing after `:cleaning` has begun
;; produces `cleaning->occupied`, which the traffic-scale close of
;; 2026-08-29 found at volume (section 9, TS-1) and which is now the
;; relation's SEVENTH arc. THESE TWO GUARDS ARE WHY NO ENGINE CHANGE
;; WAS OWED: `decide :bed-ready` below sees a non-`:cleaning` bed and
;; emits nothing, so the bed stays correctly occupied and only the
;; check-side enumeration was incomplete.

(defmethod decide :bed-cleaning
  [{facility-rng :facility} t world _patient-id {:keys [bed ward]}]
  (if (= :dirty (bed-status world bed))
    {:events [(bed-status-change t bed ward :dirty :cleaning nil)]
     :advance 0
     :schedule-followup {:t (+ t (turnaround-seconds facility-rng (:facility world) ward))
                         :patient-id nil
                         :steps [{:type :bed-ready :bed bed :ward ward}]}}
    {:events [] :advance 0}))

(defmethod decide :bed-ready
  [{world-rng :world facility-rng :facility} t world _patient-id {:keys [bed ward]}]
  (if-not (= :cleaning (bed-status world bed))
    {:events [] :advance 0}
    ;; The transfer below is decided against a world in which this bed
    ;; is ALREADY `:ready` -- `world` is the state before this batch, and
    ;; the READY event is the first event OF the batch. Without this the
    ;; ladder would refuse the very bed whose readiness is the occasion.
    (let [world' (assoc-in world [:beds bed :status] :ready)
          ready-event (bed-status-change t bed ward :cleaning :ready nil)
          vacated-location {:ward ward :bed bed
                            :placement (sim-model/bed-placement (:facility world) bed)}
          waiting-id (waiting-boarder world' nil ward)]
      (if (nil? waiting-id)
        {:events [ready-event] :advance 0}
        (let [transfer (bed-ready-transfer-event world-rng world' nil waiting-id vacated-location t)
              ;; A boarder pulled into a ready bed VACATES their own,
              ;; which seeds a second cycle -- the ED surge slot they
              ;; were holding is now dirty in its turn.
              vacated (vacate-bed facility-rng world' t (:location (get-in world' [:patients waiting-id]))
                                  waiting-id)]
          (merge {:events (into [ready-event transfer] (:events vacated))
                  :advance 0}
                 (select-keys vacated [:schedule-followup])))))))

;; --- M2b: churn family (docs/patient-state-model.md's event-validity
;; table; docs/event-sourcing.md's shadow-field dissolution) ---------------

(def documented-step-rejection-reasons
  "The closed enum every :step-rejected event's :reason must be drawn
  from (sim/ADR-0012's own invariant: 'every rejection's reason is from a
  documented enum') -- check.clj's step-rejected-reason-is-documented
  validates every log against exactly this set, so a new rejection path
  earns an entry here in the same change (the co-landing convention,
  extended to this event type)."
  #{:illegal-cancel-admit
    :illegal-cancel-transfer :illegal-cancel-transfer-bed-reoccupied
    :illegal-cancel-transfer-subject-superseded
    :illegal-cancel-discharge :illegal-cancel-discharge-bed-reoccupied
    :illegal-cancel-discharge-subject-superseded
    :illegal-bed-swap :illegal-merge})

(defn- rejected-outcome
  "sim/ADR-0012 (M3): a decide-time rejection is no longer a silent no-op --
  a :step-rejected ground-truth event now enters `:events` (folded via
  `evolve`'s own identity method for this type, now in
  `ehrt.sim-engine.evolve`, and logged like any other event) alongside
  the pre-existing :rejected key callers
  already read directly off decide's return value. `:participants`
  names ONLY `patient-id`, the one attempting the step -- never a
  possibly-nonexistent :with target named in `step` itself (that stays
  in :attempted-step, plain data no invariant needs to resolve to a
  real patient), so participant-ids-exist-in-run stays sound for every
  rejection, including one that names a typo'd or never-admitted peer.
  No RNG is drawn here: decide already drew everything it was going to
  draw before discovering the rejection (determinism note, sim/ADR-0012)."
  [reason patient-id t step extra]
  (let [event {:event :step-rejected :t t
               :participants [{:patient-id patient-id :role :subject}]
               :attempted-step step
               :reason reason}]
    {:events [event] :advance 0 :rejected (merge {:reason reason :patient-id patient-id} extra)}))

(defmethod decide :cancel-admit
  [_streams t world patient-id step]
  (let [ground-truth (:ground-truth world)
        idx (log-index/last-uncancelled-index ground-truth patient-id :admission :cancel-admit)]
    (if (nil? idx)
      (rejected-outcome :illegal-cancel-admit patient-id t step nil)
      (let [patient (get-in world [:patients patient-id])]
        {:events [{:event :cancel-admit :t t :active-mrn (:active-mrn patient)
                   :cancels-event-id idx
                   :participants [{:patient-id patient-id :role :subject}]}]
         :advance 0}))))

(defmethod decide :transfer-in-error
  [{world-rng :world} t world patient-id {:keys [location force-placement]}]
  (let [{:keys [facility patients ground-truth]} world
        board (sim-model/occupancy-board patients)
        patient (get patients patient-id)
        alloc (sim-model/allocate world-rng facility board (:beds world) location force-placement)]
    (if (:exhausted alloc)
      (exhausted-outcome patient-id location facility board)
      ;; Both events are decided ATOMICALLY, in the same decide call --
      ;; the transfer, then its own immediate correction (A12, in-error).
      ;; The cancel's reinstated home-ward/location come straight off the
      ;; CURRENT (pre-transfer) patient state, not a log query: there is
      ;; no intervening event for anything to have queried yet.
      (let [transfer-idx (count ground-truth)
            transfer-event (merge {:event :transfer :t t :active-mrn (:active-mrn patient) :from (:location patient)
                                    :attending (:attending patient) :bed-ready false
                                    :participants [{:patient-id patient-id :role :subject}]}
                                   alloc)
            cancel-event {:event :cancel-transfer :t t :active-mrn (:active-mrn patient)
                          :cancels-event-id transfer-idx :in-error true
                          :home-ward (:home-ward patient) :location (:location patient)
                          :participants [{:patient-id patient-id :role :subject}]}]
        {:events [transfer-event cancel-event] :advance 0}))))

(defmethod decide :bed-swap
  [{world-rng :world} t world patient-id {:keys [with] :as step}]
  (let [{:keys [patients]} world
        self (get patients patient-id)
        eligible (->> patients
                     (remove (fn [[pid _]] (= pid patient-id)))
                     (filter (fn [[_ p]] (and (= :admitted (:status p)) (some? (:location p)))))
                     (mapv first))
        peer-id (cond
                  with with
                  (seq eligible) (streams/uniform-choice world-rng eligible)
                  :else nil)
        peer (get patients peer-id)]
    (if (or (nil? peer-id) (nil? peer) (not= :admitted (:status peer)) (nil? (:location peer)))
      (rejected-outcome :illegal-bed-swap patient-id t step {:with with})
      ;; ARC 3B SWEEP 1: a bed-swap names TWO encounters, and one
      ;; top-level `:encounter-id` cannot carry both -- so it carries
      ;; NEITHER (`run`'s stamp skips this kind), and each side's id
      ;; rides that side's own `:swap` entry, beside the `:active-mrn`,
      ;; `:from`, `:to` and `:attending` that are already per-patient
      ;; there. `bed-swap-message` renders two PID/PV1 pairs and reads
      ;; PV1-19 from the same place it reads PV1-3.
      {:events [{:event :bed-swap :t t
                 :participants [{:patient-id patient-id :role :subject}
                                {:patient-id peer-id :role :subject}]
                 :swap {patient-id (cond-> {:active-mrn (:active-mrn self) :from (:location self)
                                            :to (:location peer) :attending (:attending self)}
                                     (:encounter-id (:encounter self))
                                     (assoc :encounter-id (:encounter-id (:encounter self))))
                        peer-id (cond-> {:active-mrn (:active-mrn peer) :from (:location peer)
                                         :to (:location self) :attending (:attending peer)}
                                  (:encounter-id (:encounter peer))
                                  (assoc :encounter-id (:encounter-id (:encounter peer))))}}]
       :advance 0})))

(defmethod decide :merge
  [{world-rng :world} t world patient-id {:keys [with] :as step}]
  (let [{:keys [patients ground-truth]} world
        survivor (get patients patient-id)
        ;; :new (never admitted -- no :admission event exists yet for
        ;; participant-ids-exist-in-run to find) and :merged (already
        ;; merged away) are never legal merge targets, dynamically
        ;; picked OR explicitly named via :with.
        never-mergeable? (fn [p] (#{:new :merged} (:status p)))
        eligible (->> patients
                     (remove (fn [[pid _]] (= pid patient-id)))
                     (remove (fn [[_ p]] (never-mergeable? p)))
                     (mapv first))
        merged-id (cond
                    with with
                    (seq eligible) (streams/uniform-choice world-rng eligible)
                    :else nil)
        merged (get patients merged-id)
        already-merged? (some (fn [ev]
                                (and (= :merge (:event ev))
                                     (some #(and (= :merged (:role %)) (= merged-id (:patient-id %)))
                                           (:participants ev))))
                              ground-truth)]
    (if (or (nil? merged-id) (= patient-id merged-id) (nil? merged)
            (never-mergeable? merged) already-merged?)
      (rejected-outcome :illegal-merge patient-id t step {:with with})
      {:events [{:event :merge :t t
                 :participants [{:patient-id patient-id :role :survivor}
                                {:patient-id merged-id :role :merged}]
                 :surviving-mrn (:active-mrn survivor)
                 :merged-mrn (:active-mrn merged)
                 :merged-mrns (:mrns merged)}]
       :advance 0})))

;; --- M3: order/result (auto-paired, docs/sim-theory.edn's order-profiles
;; catalytic) ---------------------------------------------------------------

(defmethod decide :order
  [{rng :patient} t world patient-id {:keys [profile]}]
  (let [{:keys [patients ground-truth order-profiles]} world
        patient (get patients patient-id)
        prof (get order-profiles profile)
        order-idx (count ground-truth)
        order-event {:event :order-placed :t t :active-mrn (:active-mrn patient)
                     :profile profile :concept (:concept prof)
                     :location (:location patient) :attending (:attending patient)
                     :participants [{:patient-id patient-id :role :subject}]}
        ;; :turnaround-minutes is authored (in the profile) the same
        ;; minutes-authored way :delay's IR is (docs/patient-state-
        ;; model.md's durations rule); converted to seconds here, the
        ;; same one place :delay's own decide method already converts.
        turnaround-seconds (* 60 (streams/rand-int-in rng (get-in prof [:turnaround-minutes :from])
                                              (get-in prof [:turnaround-minutes :to])))
        results (mapv (fn [analyte]
                        (let [value (order-profiles/sample-analyte-value rng analyte)]
                          ;; ADR-0150 (census S-6): the EVENT key is `:unit`,
                          ;; singular, matching :observation and a
                          ;; :diagnostic-report's children. The order-profile
                          ;; ANALYTE key stays `:units` -- it is a
                          ;; user-reachable `--config` surface (docs/cli.md,
                          ;; `:order-profiles`) and renaming it would break
                          ;; every config a user already wrote. Translated
                          ;; here, the same one-place translation `evolve
                          ;; :result-available` already performs downstream.
                          {:concept (:concept analyte) :unit (:units analyte) :value value
                           :reference-range (:reference-range analyte)
                           ;; Computed truth, not sampled (Task 4's mini-law):
                           ;; the flag is DERIVED from value vs range, here
                           ;; and nowhere else.
                           :abnormal-flag (order-profiles/abnormal-flag value (:reference-range analyte))}))
                      (:analytes prof))
        result-t (+ t turnaround-seconds)
        ;; :location/:attending are the patient's state AT ORDER TIME
        ;; (decide has no access to a FUTURE fold -- sim/ADR-0008 -- and the
        ;; result event's own values were already fully computed atomically
        ;; back here); PV1 context for both messages reflects where the
        ;; specimen was ordered, the same convention real order/result
        ;; pairs use when a patient's location changes between the two.
        result-event {:event :result-available :t result-t :active-mrn (:active-mrn patient)
                      :profile profile :order-event-id order-idx :concept (:concept prof)
                      :location (:location patient) :attending (:attending patient)
                      :results results
                      :participants [{:patient-id patient-id :role :subject}]}]
    ;; The result event is fully computed NOW (all its RNG draws happen
    ;; in this one decide call, same "decided atomically" precedent
    ;; transfer-in-error already sets) but is NEVER returned directly in
    ;; :events -- a future-t event spliced into THIS call's :events
    ;; would enter ground-truth at this call's OWN log position, ahead
    ;; of other patients' events with SMALLER :t that get processed
    ;; later in wall-loop order, breaking the log's global t-ordering
    ;; (engine-test's own `(apply <= (map :t ground-truth))` sanity
    ;; check, and the derivability law any emitter/consumer relies on).
    ;; Instead it rides `:schedule-followup`: the run loop enqueues it
    ;; as a genuine future queue entry, so it enters ground-truth at its
    ;; own correct global [t seq-no] position, the same way every other
    ;; scheduled event does.
    {:events [order-event] :advance 0
     :schedule-followup {:t result-t :patient-id patient-id
                         :steps [{:type :result-followup :result-event result-event}]}}))

(defmethod decide :result-followup
  [_streams _t _world _patient-id {:keys [result-event]}]
  {:events [result-event] :advance 0})

;; --- M5b: :outpatient-visit / :outpatient-visit-end (components/patient-simulator/docs/gmf-interpreter.md
;; section 4's sketch, items 5-7) --------------------------------------------

(defmethod decide :outpatient-visit
  [{facility-rng :facility} t world patient-id step]
  ;; Item 5: NO sim-model/allocate call -- an outpatient encounter occupies
  ;; no bed, so there is no ladder to consult. Still gets an attending
  ;; (real ambulatory visits have a treating provider) -- chosen uniformly
  ;; among ALL providers, not ward-filtered (there is no ward), the same
  ;; "no ward-scoping concept, choose uniformly among everyone" treatment
  ;; bed-swap/merge's own peer selection already establishes.
  (let [{:keys [providers patients]} world
        patient (get patients patient-id)
        attending (:id (streams/uniform-choice facility-rng providers))]
    {:events [(merge {:event :outpatient-visit :t t :active-mrn (:active-mrn patient)
                      :attending attending
                      :participants [{:patient-id patient-id :role :subject}]}
                     (reason-field step) (citation-fields step)
                     ;; ARC 3B SWEEP 1: the second opener, minting the
                     ;; same way `:admission` does.
                     (streams/minted-encounter-id-field world patient-id)
                     ;; ARC 3B SWEEP 3: and the opener a FOLLOW-UP
                     ;; produces, so this is the headline reference --
                     ;; a second encounter that is SCHEDULED.
                     (appointment-ref-field step))]
     :advance 0}))

(defmethod decide :outpatient-visit-end
  [_streams t world patient-id step]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :outpatient-visit-end :t t :active-mrn (:active-mrn patient)
                      :attending (:attending patient)
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

;; --- M5b: CompileTrajectory's new ground-truth event types (docs/gmf-
;; interpreter.md section 1's table) -- each is a real, glass-box-cited
;; ground-truth event; none carries or changes PatientState (the log
;; itself is the record, sim/ADR-0008, the same "no PatientState field for
;; it" treatment :order-placed/:result-available already get). None
;; consumes RNG -- their content was already fully sampled by the GMF
;; interpreter (M5a); CompileTrajectory/the engine only replay it.

(defmethod decide :procedure
  [_streams t world patient-id {:keys [codes] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :procedure :t t :active-mrn (:active-mrn patient) :codes codes
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defmethod decide :observation
  [_streams t world patient-id {:keys [codes] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :observation :t t :active-mrn (:active-mrn patient) :codes codes}
                     (state/observation-value-fields step)
                     {:participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

;; --- GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P5): both
;; MultiObservation and DiagnosticReport compile to this SAME step type
;; -- ONE ground-truth event for the whole state, carrying the full
;; :observations vector, mirroring how the compiled IR step itself
;; bundles children (never one event per child).

(defmethod decide :diagnostic-report
  [_streams t world patient-id {:keys [codes observations] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :diagnostic-report :t t :active-mrn (:active-mrn patient) :observations observations}
                     (when codes {:codes codes})
                     {:participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defmethod decide :medication-order
  [_streams t world patient-id {:keys [codes] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :medication-order :t t :active-mrn (:active-mrn patient) :codes codes
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defn person-entry
  "What `world`'s `:person-index` holds for one person -- the patient a
  returning person resolves to, and what has been minted for them so far
  (`init-world`'s own comment carries the entry shape). nil when this
  person has not been seen before.

  ADR-0173 section 2(a) (arc 3a). Lands AHEAD of its caller,
  deliberately, and it is the reader that makes the carried index's
  hand-built-world contract real: FALLS BACK to nil when `world` carries
  no `:person-index` KEY -- on the KEY, never on a missing entry, the
  same rule `reinstated-state` and `last-cited-index` already follow, so
  a carrier `run` built but failed to populate shows up as a changed
  corpus rather than as a silent miss. Part 3's arrival selection is
  what writes it; part 4 grows each entry a `:placeholders` set -- every
  unidentified record minted for that person, before any of them is
  filled or merged. `run` still seeds the key EMPTY for a run with no
  `:persons`."
  [world person-id]
  (when (contains? world :person-index)
    (get (:person-index world) person-id)))

(defmethod decide :medication-end
  [_streams t world patient-id {:keys [order-citation] :as step}]
  ;; Resolved by CITATION match against ground-truth, never a pathway-
  ;; position index (pathway.clj's own :medication-end docstring) -- the
  ;; same glass-box, position-independent resolution ConditionEnd's own
  ;; trajectory-level :references already models, one level down at the
  ;; ground-truth log.
  (let [{:keys [ground-truth patients]} world
        patient (get patients patient-id)
        ;; ADR-0164: SAME PATIENT, too. A citation is `{:module :state}`
        ;; -- a module coordinate, not a patient-qualified one -- so two
        ;; patients walking the same module cite identically, and an
        ;; unfiltered `last` over the whole log hands this end whichever
        ;; patient's order came LAST. The participant predicate is the
        ;; one `ehrt.sim-engine.log-index/last-uncancelled-index`
        ;; already uses for exactly this reason, and the one
        ;; check.clj's own medication-end invariant tests the
        ;; resolved target against.
        order-event-id (log-index/last-cited-index world ground-truth :medication-order
                                                   patient-id order-citation)]
    ;; M6 Task 1: `:order-citation` now rides the event itself, alongside
    ;; the already-resolved `:order-event-id` -- `evolve`'s own fold-time
    ;; medication-orders match needs the CITATION (position-independent),
    ;; never the log-position index `:order-event-id` carries (meaningless
    ;; to a fold that never sees the whole log).
    {:events [(merge {:event :medication-end :t t :active-mrn (:active-mrn patient)
                      :order-event-id order-event-id :order-citation order-citation
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

;; GMF coverage Wave D stage D2 (2026-08-02, ADR-0029 R2(b)): the SAME
;; decide/evolve shape :medication-order/:medication-end establish,
;; two defmethod-pairs up.

(defmethod decide :care-plan-start
  [_streams t world patient-id {:keys [codes activities] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :care-plan-start :t t :active-mrn (:active-mrn patient) :codes codes}
                     (when activities {:activities activities})
                     {:participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defmethod decide :care-plan-end
  [_streams t world patient-id {:keys [care-plan-citation] :as step}]
  ;; Resolved by CITATION match against ground-truth, never a pathway-
  ;; position index -- the same glass-box, position-independent
  ;; resolution :medication-end already models.
  (let [{:keys [ground-truth patients]} world
        patient (get patients patient-id)
        ;; ADR-0164: SAME PATIENT, too -- the twin of the scan
        ;; :medication-end already carries, for the identical reason.
        start-event-id (log-index/last-cited-index world ground-truth :care-plan-start
                                                   patient-id care-plan-citation)]
    {:events [(merge {:event :care-plan-end :t t :active-mrn (:active-mrn patient)
                      :start-event-id start-event-id :care-plan-citation care-plan-citation
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defmethod decide :cancel-transfer
  [_streams t world patient-id step]
  (let [ground-truth (:ground-truth world)
        idx (log-index/last-uncancelled-index ground-truth patient-id :transfer :cancel-transfer)]
    (if (nil? idx)
      (rejected-outcome :illegal-cancel-transfer patient-id t step nil)
      (let [patient (get-in world [:patients patient-id])
            {:keys [home-ward location]} (log-index/reinstated-state world ground-truth patient-id idx)]
        (cond
          ;; TS-5, and asked BEFORE the bed: whether the subject is
          ;; still in the hospital at all is prior to whether anyone
          ;; else has taken the bed they left. The ordering is visible
          ;; -- 9 of the 61 superseded-subject cancel-transfers at
          ;; `nobed` 10^5 were already being rejected as
          ;; `-bed-reoccupied` and now carry this reason instead --
          ;; and it is the right way round: the reason a log carries
          ;; should name why the step could never have happened, not
          ;; the second thing that would also have stopped it.
          (log-index/subject-superseded? patient :cancel-transfer)
          (rejected-outcome :illegal-cancel-transfer-subject-superseded patient-id t step
                            {:status (:status patient)})

          (log-index/bed-reoccupied-by-someone-else? world patient-id location)
          (rejected-outcome :illegal-cancel-transfer-bed-reoccupied patient-id t step {:location location})

          :else
          {:events [{:event :cancel-transfer :t t :active-mrn (:active-mrn patient)
                     :cancels-event-id idx :home-ward home-ward :location location
                     :participants [{:patient-id patient-id :role :subject}]}]
           :advance 0})))))

(defmethod decide :cancel-discharge
  [_streams t world patient-id step]
  (let [ground-truth (:ground-truth world)
        idx (log-index/last-uncancelled-index ground-truth patient-id :discharge :cancel-discharge)]
    (if (nil? idx)
      (rejected-outcome :illegal-cancel-discharge patient-id t step nil)
      (let [patient (get-in world [:patients patient-id])
            {:keys [home-ward location attending]} (log-index/reinstated-state world ground-truth patient-id idx)]
        (cond
          ;; TS-5 again, and asymmetric: `:discharged` is the status a
          ;; cancel-discharge EXISTS to find, so only `:expired` and
          ;; `:merged` supersede one. All 55 cancel-discharges at
          ;; `nobed` 10^5 read `:discharged` here and every one of them
          ;; stays legal.
          (log-index/subject-superseded? patient :cancel-discharge)
          (rejected-outcome :illegal-cancel-discharge-subject-superseded patient-id t step
                            {:status (:status patient)})

          (log-index/bed-reoccupied-by-someone-else? world patient-id location)
          (rejected-outcome :illegal-cancel-discharge-bed-reoccupied patient-id t step {:location location})

          :else
          {:events [{:event :cancel-discharge :t t :active-mrn (:active-mrn patient)
                     :cancels-event-id idx :home-ward home-ward :location location :attending attending
                     :participants [{:patient-id patient-id :role :subject}]}]
           :advance 0})))))
