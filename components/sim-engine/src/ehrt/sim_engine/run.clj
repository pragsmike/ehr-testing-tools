(ns ehrt.sim-engine.run
  "The driver: the queue primitive, the pre-loop, and `run` itself --
  `engine.clj`'s TENTH and last extraction under
  `roadmap.md#engine-namespace-extraction-and-apply-unification` (author
  ruling C1(a), and C4(b), which ruled this extraction on 2026-08-30).
  Six forms: `pop-min`, `placeholder-registration`, `select-person`,
  `prelude`, `person-plan` and `run`. With them gone `engine.clj` holds
  no executable code of its own: it is a PURE FACADE of forty-three
  delegating defs and nine `moved to` comment blocks.

  THE NAME. `ehrt.sim-engine.run` shadows nothing: `clojure.core` has a
  `run!` but no `run`, so `(defn run ...)` here needs no
  `:refer-clojure :exclude`, and `run!` stays reachable unqualified.
  `engine.clj` aliases this namespace as `run` and also defines a var
  named `run`; aliases and vars live in separate tables, so `run/run`
  resolves through the alias and `run` through the var. That is the same
  shape `(def decide decide/decide)` has carried since the ninth
  extraction.

  THIS NAMESPACE HOLDS THE PROGRAM'S SOLE EVENT PRODUCER AND ITS MAIN
  APPLY SITE. The census's section 4a records that `run`'s own
  `(decide ...)` call is the only expression in the tree that mints a
  ground-truth event; its section 4b enumerates the ten things `run`'s
  in-loop fold then does with what comes back. Both moved verbatim --
  nothing added, removed or reordered -- and the divergence between this
  apply site and `replay`'s (`ehrt.sim-engine.fold`) is documented in
  section 4c and RULED to be paid at application-path unification, not
  here.

  Extracted OUTPUT-IDENTICAL: every form below is `engine.clj`'s own
  text, moved and not rewritten. FOURTEEN call sites are qualified that
  were bare, and no other code line differs. Each of the fourteen named
  a delegating def in `engine.clj` -- `initial-patient`,
  `patient-id-for`, `decide`, `compile-patient`, the three
  `*-stay-minutes` tables, `evolve`, `assign-pathway`, `assign-module`,
  `valid-persons?` and `valid-scheduling?` -- and each now names the
  namespace that owns the form, which holds the SAME object that def
  holds. They could not stay bare: a facade may require its
  implementations, an implementation may not require its facade, and
  `engine.clj` requires this namespace to build its two delegating defs.

  THE ONE EXCEPTION IS `stream`, AND IT IS FORCED. Census constraint 1:
  `engine-test/mutating-one-patients-stream-seed-moves-only-that-patient`
  perturbs the RNG partition with `with-redefs` on the var
  `ehrt.sim-engine.engine/stream`, so `run` must go on calling THAT var.
  `streams/stream` is a different var and the redefinition does not
  reach it; the test would not fail silently -- its moved-ordinal set
  would collapse to `#{}` -- but it would fail. Requiring
  `ehrt.sim-engine.engine` here is the cycle the paragraph above
  forbids, so the four call sites stay bare and `stream` below is a
  private shim that resolves the engine's var once, lazily, and invokes
  it. Invoking a Var derefs it per call, so a `with-redefs` is seen
  exactly as it was when these lines stood in `engine.clj`, and the shim
  is the ONE form here that is not `engine.clj`'s own text.

  Its edges are the ones `engine.clj` already took, unchanged -- this
  namespace is the driver, so it reaches every sibling: `assignment`,
  `churn`, `config`, `decide`, `encounters`, `evolve`, `fold`,
  `log-index`, `order-profiles`, `person-fold`, `state` and `streams`,
  plus `sim-model` and `kernel`. No sibling reaches back, which is what
  makes this the last cluster and what let the other nine go first."
  (:require [ehrt.sim-model.interface :as sim-model]
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
            [ehrt.sim-engine.state :as state]
            [ehrt.sim-engine.streams :as streams]
            [ehrt.kernel.interface :as result])
  (:import [java.util Random]))

;; --- census constraint 1's shim, the one form here that did not move --------
;;
;; `engine.clj`'s `stream` is a delegating def of `streams/stream`, and
;; `engine_test/mutating-one-patients-stream-seed-moves-only-that-patient`
;; perturbs the RNG partition by `with-redefs` on THAT var. `prelude` and
;; `run` below call `stream` bare, exactly as they did when they stood in
;; `engine.clj`, and this pair is what keeps those four bare names
;; resolving to the engine's var without requiring the engine -- which
;; would be a load cycle, since `engine.clj` requires this namespace.
;;
;; `requiring-resolve` returns the VAR, not its value, and the delay
;; caches the var rather than what it held; invoking a Var derefs it on
;; every call. So a `with-redefs` in force during the call is seen here,
;; which is the whole point. `streams/stream` is deliberately UNHINTED
;; for the same test's sake (census constraint 3) and this shim adds no
;; hint either.

(def ^:private engine-stream
  "The var `ehrt.sim-engine.engine/stream`, resolved on first use."
  (delay (requiring-resolve 'ehrt.sim-engine.engine/stream)))

(defn- stream
  "A fresh `java.util.Random` for one stream, THROUGH the engine facade's
  own var -- census constraint 1. See the banner above."
  [master family id-tag]
  (@engine-stream master family id-tag))

(defn- pop-min
  "Removes and returns the earliest queue entry. Queue is a sorted-map
  keyed by [t seq-no] -- the seq-no tiebreak makes ordering total, so
  RNG consumption order (and thus output) is fully determined."
  [queue]
  (let [[k v] (first queue)]
    [k v (dissoc queue k)]))

(defn- placeholder-registration
  "What a PLACEHOLDER registration adds to a patient's compiled entry
  (ADR-0173 section 2(d)): the window's alias, and its close instant.

  `:window-close-t` RIDES ONLY A WINDOW THAT ACTUALLY RESOLVES, and
  that is a correction the tree forced rather than a choice. The key
  means *identification is DUE at this instant*, and
  `every-placeholder-registration-is-resolved-or-still-open` reads it
  as exactly that: a placeholder past its own close with no fill and no
  merge is a defect. But a window can fail to resolve for a reason that
  is not a defect at all -- the person DIED inside it, and the person
  process correctly emits no `:identity-resolution` for somebody who
  did not live to see one. Found at population scale, not by reasoning:
  `clinic-decade` seed 5 over an 800-person pool exited
  `:self-check-failed` on that invariant for PID-000208-f8f59cb6, whose
  own person opened a window at t 62,829,345 due to close at
  65,248,545 and died at 64,751,457 -- 497,088 seconds short of it.

  An unidentified patient who dies before anybody establishes who they
  were is the most characteristic John Doe outcome there is, and an
  invariant that forbade it would be wrong about the world rather than
  about the log. So the ENGINE declines to promise a close instant it
  already knows will never come, and the invariant's own existing
  clause -- *a placeholder carrying none cannot be judged either way, so
  it is left alone* -- is what covers it. Nothing is weakened: a window
  that DOES resolve still carries its close, so a resolution the engine
  failed to mint still goes red, which is the failure the invariant
  exists for."
  [entry window]
  (cond-> (assoc entry :identity :placeholder :alias-name (:alias-name window))
    (some? (:branch window)) (assoc :window-close-t (:until-t window))))

(defn- select-person
  "A1: ONE uniform from the `:world` stream picks this arrival's person
  out of the persons ALIVE at that instant.

  `:world` is the family whose own definition names this draw --
  *\"arrivals, and every cross-patient decision\"* (`stream-family-tag`)
  -- and the candidate set is the POOL, which is shared, so no
  per-patient stream could own it.

  FIXED CONSUMPTION: the draw is taken whether or not the filter
  removed anyone, and whether or not it removed EVERYONE. An arrival
  with no living candidate binds to nobody and is an ordinary,
  person-free arrival -- the same dual path a run with no `:persons` key
  takes, reached one arrival at a time instead of run-wide.

  Without the filter an arrival could land on a person whose own
  `:person-death` already fired, which is a defect with a wire face: a
  registration for somebody the ground truth says is dead."
  [^Random rng population alive t]
  (let [candidates (filterv (fn [{:keys [person-id]}]
                              (let [d (get alive person-id)]
                                (or (nil? d) (> d t))))
                            population)
        draw (.nextDouble rng)]
    (when (seq candidates)
      (:person-id (nth candidates
                       (min (dec (count candidates))
                            (long (* draw (count candidates)))))))))

(defn- prelude
  "Everything `run` computes before its loop starts, in ONE place because
  two callers must agree on it EXACTLY.

  `run` is the first. The second is ADR-0173 ruling C1's own ordering
  problem: the compiled trajectory's death instant is a t0 parameter of
  the person process, so a caller has to know every arrival's compiled
  death -- and which PERSON each arrival bound to -- BEFORE the person
  stream it will hand back to `run` can be built. Both facts are
  pre-loop facts, and `person-plan` (below) is the export that hands
  them out. Reimplementing the pre-loop outside this namespace was the
  alternative, and it would have put the pinned `:patient`-family draw
  order in two places.

  THE PINNED `:patient`-FAMILY DRAW ORDER is `run`'s own docstring's,
  unchanged: `module-for`, `pathway-for`, `churn/inject`, then
  `compile-patient`, per arrival ordinal in ordinal order. The `:world`
  family's order is arrival gaps first, then -- only with `:persons` --
  one selection uniform per arrival ordinal.

  BYTE-IDENTICAL WITH `:persons` ABSENT, by construction rather than by
  care: `bindings` is all-nil, no selection draw is taken, every arrival
  is its own patient's first, `compile-patient` gets a nil persona and
  so draws its own, and no person step is seeded. Every expression below
  is then the expression that was there before arc 3a part 3."
  [{:keys [seed patients pathway pathways arrival-gap facility churn-profile
           persona-config modules module-assignment module-horizon-days history persons
           encounters scheduling]
    ;; The SAME defaults `run`'s own parameter list declares, restated
    ;; here because `person-plan` is a second entry point and a caller
    ;; that omitted `:patients` would otherwise reach `(range nil)`.
    ;; `run` passes its own already-defaulted values through, so these
    ;; only ever apply on the `person-plan` path.
    :or {patients 1
         pathway sim-model/sample-admission-discharge
         arrival-gap 60
         facility sim-model/default-facility
         persona-config {}
         modules []
         module-horizon-days 90
         history false}}]
  (let [;; ADR-0171: FIVE families, not one shared Random. Each stream
        ;; is derived from the master seed and a stable id -- never from
        ;; a counter, never from construction order -- so a draw added
        ;; to one patient's pathway moves that patient and no one else.
        ;; `stream-family-tag`'s own docstring carries which draw site
        ;; belongs to which family; the two run-scoped families take
        ;; id-tag 0.
        world-rng (stream seed :world 0)
        ;; One stream per arrival ordinal, built up front. A patient's
        ;; stream must PERSIST across their own decides (a `Random` is
        ;; stateful, and a patient's draws are one continuing sequence),
        ;; so these are constructed once here rather than per decide.
        patient-rngs (mapv (fn [i] (stream seed :patient i)) (range patients))
        ;; Stagger arrivals: :arrival-gap is authored in MINUTES (same
        ;; carve-out as :delay's IR, and for the same calibration
        ;; reason -- see `run`'s docstring); the engine converts to
        ;; SECONDS here. Consume RNG in patient order (fixed). WORLD:
        ;; who arrives when is a fact about the run, not about any one
        ;; patient, and these draws happen before any decide, so their
        ;; count and order are fixed by `:patients` alone.
        arrivals (vec (reductions + 0 (repeatedly (dec patients)
                                                  #(* 60 (streams/rand-int-in world-rng 0 arrival-gap)))))
        mrn-for (fn [i] (format "MRN%06d" (inc i)))
        pid-for (fn [i] (streams/patient-id-for seed i))
        ;; ADR-0173 section 2(a), ruling A1. AFTER the arrival gaps, so a
        ;; run with no `:persons` leaves this stream exactly where it has
        ;; always stood by the time the loop's own `:world` draws start.
        bindings (if persons
                   (mapv (fn [t] (select-person world-rng (:population persons) (:alive persons) t))
                         arrivals)
                   (vec (repeat patients nil)))
        ;; ARC 3B SWEEP 3 (ADR-0174 section 2(b)): the scheduled-vs-walk-in
        ;; SPLIT, on `:world`, in ORDINAL ORDER, AFTER the person-selection
        ;; uniform above -- exactly where the ADR puts it, and for the
        ;; reason it gives: an arrival is `:world` by the partition's own
        ;; definition (*"arrivals, and every cross-patient decision"*,
        ;; `stream-family-tag`), these two draws POSITION an arrival, and
        ;; their count is conditional on the population, which is the exact
        ;; reason `:world` is run-scoped rather than per-patient.
        ;;
        ;; TWO DRAWS PER ORDINAL, ALWAYS, both taken before any of them is
        ;; consulted -- so the split's own stream position is fixed by
        ;; `:patients` alone, exactly as the arrival gaps above are, and a
        ;; site retuning `:scheduled-fraction` shifts no bed choice.
        ;;
        ;; A run with no `:scheduling` key takes NEITHER, which is what
        ;; leaves this stream where it has always stood by the time the
        ;; loop's own `:world` draws start.
        scheduled-arrivals
        (if scheduling
          (let [[lo hi] (:lead-time-days scheduling)
                f (:scheduled-fraction scheduling)]
            (mapv (fn [_]
                    (let [u (.nextDouble ^Random world-rng)
                          days (streams/rand-int-in world-rng lo hi)]
                      (when (< u f) (decide/days->seconds days))))
                  (range patients)))
          (vec (repeat patients nil)))
        ;; What a SCHEDULED arrival's step list becomes: the whole arrival
        ;; carried behind ONE `:appointment` step, exactly as
        ;; `:repeat-arrival` already carries one behind one step and for
        ;; exactly its reason -- the visit behind a booking happens or it
        ;; does not, and half of it happening would be a discharge with no
        ;; admission. A WALK-IN is returned untouched, which is the opt-in
        ;; law expressed one arrival at a time.
        ;;
        ;; `:appointment-class` MATCHES THE ENCOUNTER CLASSES (the ADR's
        ;; own table) and is read off the OPENER TYPE, never off the ward:
        ;; `evolve :admission` sets `:class :inpatient` and `evolve
        ;; :outpatient-visit` sets `:class :outpatient`, unconditionally
        ;; and regardless of where the bed is. Deriving it from the ward
        ;; would make the booking disagree with the encounter it books.
        opener-class (fn [steps]
                       (let [first-step (first steps)
                             t (:type first-step)]
                         (case t
                           :admission :inpatient
                           :outpatient-visit :outpatient
                           :repeat-arrival (recur (:steps first-step))
                           :outpatient)))
        book (fn [i steps]
               (let [lead (nth scheduled-arrivals i)]
                 (if (and lead (seq steps))
                   [{:type :appointment
                     :lead-seconds lead
                     :appointment-class (opener-class steps)
                     :steps (vec steps)}]
                   (vec steps))))
        ;; person-id -> the ordinal that person FIRST arrived at. A person
        ;; selected twice is the point of having a pool: the second
        ;; arrival resolves to the patient the first one minted.
        events-by-person (group-by :person-id (:events persons))
        ;; ADR-0173 section 2(d) (arc 3a part 4): an arrival landing
        ;; inside an open `:identity-unavailable` window is a PLACEHOLDER
        ;; arrival -- an unresponsive John Doe, in the author's own
        ;; words. It mints its OWN patient at its OWN ordinal even when
        ;; the person behind it already has a record, because the whole
        ;; point is that nobody yet knows the two are the same somebody;
        ;; the identification flow is what joins them afterwards, by a
        ;; fill or by a merge.
        windows-of (fn [p] (person-fold/identification-windows (get events-by-person p)))
        arrival-windows (mapv (fn [i]
                                (when-let [p (nth bindings i)]
                                  (person-fold/window-open-at (windows-of p) (nth arrivals i))))
                              (range patients))
        placeholder? (fn [i] (some? (nth arrival-windows i)))
        ;; A PLACEHOLDER ARRIVAL IS NEVER A PERSON'S CANONICAL PATIENT,
        ;; so it is skipped here rather than claiming the person: an
        ;; identified arrival that follows it still mints their real
        ;; record, and their later demographic events land on that one.
        first-ordinal (persistent!
                       (reduce (fn [acc i]
                                 (let [p (nth bindings i)]
                                   (if (or (nil? p) (placeholder? i) (contains? acc p))
                                     acc
                                     (assoc! acc p i))))
                               (transient {}) (range (count bindings))))
        ;; A patient id is minted from an ARRIVAL ORDINAL and always has
        ;; been (`patient-id-for`), so a repeat arrival is resolved by
        ;; minting from the FIRST ordinal rather than by inventing a
        ;; second id space. A run whose pool is larger than its arrival
        ;; count mints exactly the ids it mints today.
        ;;
        ;; The `(get first-ordinal p i)` DEFAULT is part 4's: a
        ;; placeholder arrival is bound to a person who has no entry
        ;; here, and it owns itself.
        owner-ordinal (fn [i]
                        (if-let [p (nth bindings i)]
                          (if (placeholder? i) i (get first-ordinal p i))
                          i))
        first-arrival? (fn [i] (= i (owner-ordinal i)))
        pid-of (fn [i] (pid-for (owner-ordinal i)))
        firsts (filterv first-arrival? (range patients))
        ;; ADR-0173 section 2(b): what a bound person brings to their own
        ;; first arrival. Their pre-arrival events are folded onto their
        ;; t0 Persona rather than replayed as log events, because
        ;; `registered-is-every-patients-first-event` is structural.
        registration-of (fn [i]
                          (when-let [p (nth bindings i)]
                            (person-fold/registration (get (:personas persons) p)
                                                      (get events-by-person p)
                                                      (nth arrivals i))))
        registrations (mapv registration-of (range patients))
        ;; M3-adjacent: :pathways ABSENT entirely -- the pinned-fixture
        ;; path -- means every patient gets the same plain :pathway, no
        ;; assign-pathway call, no new draw (see `run`'s docstring).
        pathway-for (if pathways
                      (fn [i] (assignment/assign-pathway (nth patient-rngs i) pathways i))
                      (fn [_i] pathway))
        ;; InjectChurn (M2b): ONLY when :churn-profile is actually
        ;; present does this stage run at all -- absent, `steps-for` is
        ;; a no-op and consumes no RNG (see the docstring's fixture note).
        steps-for (if churn-profile
                    (fn [i] (:steps (churn/inject (pathway-for i) churn-profile (nth patient-rngs i))))
                    (fn [i] (:steps (pathway-for i))))
        ;; M5b Task 4: module-assignment is resolved eagerly, the SAME
        ;; point :pathways' own assign-pathway draw already occupies (one
        ;; more fixed-consumption draw per patient, ONLY when
        ;; :module-assignment is actually present -- absent entirely,
        ;; `module-for` draws nothing, byte-identical to pre-M5b (see
        ;; `run`'s own docstring)). ADR-0173 ruling C1 (2026-08-26): the
        ;; MODULE WALK itself (persona-dependent -- an unbounded number of
        ;; draws) no longer waits for :registered decide-time; it happens
        ;; in `compiled-patients` below, together with the persona sampling
        ;; it depends on, immediately after this stage's own draws.
        ;; ADR-0033 AR-2: `:modules` entries are closure-shaped -- keyed
        ;; by each closure's own `:root`, not a bare module's `:id`
        ;; (byte-identical for a singleton closure, whose :root IS the
        ;; module's own :id).
        closures-by-root (into {} (map (fn [c] [(:root c) c])) modules)
        module-for (if module-assignment
                     (fn [i] (get closures-by-root (assignment/assign-module (nth patient-rngs i) module-assignment i)))
                     (fn [_i] nil))
        ;; M4: :registered is prepended to EVERY patient's step queue,
        ;; ahead of whatever InjectChurn produced -- engine-internal,
        ;; never seen by InjectChurn's own applicability oracle (it
        ;; operates on `pathway-for`'s output, before this prepend), the
        ;; same "not authorable IR" treatment :result-followup gets. M5b:
        ;; carries this patient's own resolved closure (nil, absent
        ;; :module-assignment). ADR-0173 C1: the closure still rides the
        ;; step (the hand-built-world fallback in `decide :registered`
        ;; reads it), but the walk + compile it feeds now happens in
        ;; `compiled-patients`, below.
        ;;
        ;; ARC 3A PART 3, DISCLOSED: a REPEAT arrival prepends no
        ;; `:registered` -- ADR-0173 section 2(a)'s own rule, and what
        ;; keeps `registered-is-every-patients-first-event` true by
        ;; construction rather than by luck -- and queues no steps
        ;; either. The ADR reads "the second encounter's steps simply
        ;; continue that patient's log"; the tree refuses, and the
        ;; refusal is one-sided. A second admission for a patient whose
        ;; status is `:discharged` violates `check.clj`'s own
        ;; `admission-only-when-new`, which is this project's
        ;; single-encounter horizon (sim/ADR-0007 point 3) expressed as
        ;; an invariant. Lifting that horizon is not an arc-3a change.
        ;; What a repeat arrival IS for stands untouched: the person
        ;; resolves to the patient they already are, and every later
        ;; demographic event of theirs lands on that one patient rather
        ;; than on a stranger.
        registered-entry-for (fn [i]
                               ;; `module-for` FIRST, then `steps-for` --
                               ;; the exact evaluation order the previous
                               ;; `(into [{... (module-for i)}] (steps-for
                               ;; i))` had, preserved deliberately: it is
                               ;; the pinned pre-loop order of this
                               ;; patient's own `:patient` stream (see
                               ;; `run`'s docstring). The resolved closure
                               ;; is returned ALONGSIDE the steps so
                               ;; `compiled-patients` below can read it
                               ;; without a second `module-for` call,
                               ;; which would draw again.
                               (let [closure (module-for i)
                                     steps (steps-for i)]
                                 {:closure closure
                                  :steps (cond
                                           (first-arrival? i)
                                           (into [{:type :registered :closure closure}]
                                                 (book i steps))
                                           ;; ARC 3B SWEEP 1: the wall,
                                           ;; lifted -- but behind ONE
                                           ;; gated step, never spliced
                                           ;; in raw (`decide
                                           ;; :repeat-arrival`'s own
                                           ;; docstring). Note what does
                                           ;; NOT move: `module-for` and
                                           ;; `steps-for` above are
                                           ;; called for every ordinal
                                           ;; either way, so the opt-in
                                           ;; changes ZERO pre-loop
                                           ;; draws and the whole
                                           ;; reshuffle belongs to the
                                           ;; loop.
                                           (and encounters (seq steps))
                                           (book i [{:type :repeat-arrival :steps (vec steps)}])

                                           :else [])}))
        initial-entries (into [] (map-indexed (fn [i _] (registered-entry-for i))) arrivals)
        ;; ADR-0173 ruling C1: the four run-config values `compile-patient`
        ;; reads, gathered here because the compile now happens BEFORE
        ;; `init-world` exists. Identical to what `decide :registered`
        ;; would have read off `world` at arrival -- none of the four is
        ;; ever re-`assoc`ed by the run loop (`compile-patient`'s own
        ;; t-independence table says so key by key).
        compile-inputs {:persona-config persona-config
                        :module-horizon-days module-horizon-days
                        :facility facility
                        :history history}
        ;; ADR-0173 ruling C1: every patient's Persona and compiled module
        ;; trajectory, drawn HERE rather than at that patient's arrival,
        ;; in arrival-ordinal order, immediately after `initial-entries`
        ;; took this run's pre-loop `:patient`-family draws. Each patient
        ;; reads its OWN stream, so the move changes WHEN the draw happens
        ;; and not WHERE in any stream it lands -- the byte-identity
        ;; argument, in one line.
        ;;
        ;; A patient whose `:registered` is never decided (an `:exhausted`
        ;; run ends the loop early) is compiled here anyway. That costs a
        ;; walk and moves no byte: the draws land on that patient's own
        ;; stream, which nothing else ever reads. ARC 3A PART 3 adds one
        ;; more of those: a REPEAT arrival's own compile is drawn and
        ;; DISCARDED (the walk happens once, at first registration), and
        ;; the draw is taken either way so consumption stays a function
        ;; of `:patients` alone.
        compiled (mapv (fn [i]
                         (decide/compile-patient (nth patient-rngs i) compile-inputs
                                          (:closure (nth initial-entries i))
                                          (:persona (nth registrations i))))
                       (range patients))
        ;; --- ADR-0173 section 2(c), arc 3a part 4: THE TWO HOOKS -------
        ;;
        ;; Both hooks CREATE traffic `:patients` does not count, said
        ;; plainly because it changes what a config means: with
        ;; `:persons` present, `:patients` is the number of SELECTED
        ;; arrivals, and the run's patient count is that plus the
        ;; newborns plus the injury arrivals.
        ;;
        ;; A hook may only put an encounter on a patient who is
        ;; CLINICALLY IDLE -- whose whole step queue, authored and
        ;; compiled, is their `:registered` and nothing else. This is
        ;; the single-encounter horizon again (`admission-only-when-
        ;; new`, sim/ADR-0007 point 3), and it has to be answered
        ;; STATICALLY as well as at decide time: a birth encounter
        ;; landing on a patient whose own module admits them LATER would
        ;; leave that later admission illegal, and the decide-time guard
        ;; cannot see the future. `decide :person-encounter` carries the
        ;; runtime half, so a wrong answer here costs a skipped
        ;; encounter rather than a red invariant.
        clinically-idle? (fn [i]
                           (and (empty? (:steps (:compiled (nth compiled i))))
                                (every? #(= :registered (:type %))
                                        (:steps (nth initial-entries i)))))
        hook-events (if persons (person-fold/hooks (:events persons)) [])
        newborn-personas (if persons (person-fold/newborn-personas (:events persons)) {})
        persona-of (fn [p] (or (get (:personas persons) p) (get newborn-personas p)))
        bound-persons (into #{} (remove nil?) bindings)
        ;; The SAME alive filter ruling A1 puts on arrival selection, put
        ;; on hook minting for the same reason: a patient minted for
        ;; somebody the ground truth already says is dead is a defect
        ;; with a wire face. Half-open on the left, exactly as
        ;; `select-person` is -- a person whose death instant IS the
        ;; hook's instant is dead at it.
        alive-at? (fn [p t] (let [d (get (:alive persons) p)] (or (nil? d) (> d t))))
        ;; One pass over the hooks, in the order the stream already
        ;; stands in (t-ascending -- the component's own front-door
        ;; contract). Each hook contributes at most one ADDITIONAL
        ;; patient and at most one encounter on an EXISTING one.
        ;;
        ;;   :delivery            -> the newborn is an additional patient
        ;;                           whose first encounter is the birth
        ;;                           (`traffic-model.md`), plus the
        ;;                           parent's own delivery admission when
        ;;                           the parent is clinically idle.
        ;;   :occupational-injury -> an ED encounter on the injured
        ;;                           person's own patient; or, for a
        ;;                           person NO arrival ever bound, an
        ;;                           additional patient of their own
        ;;                           ("or mints one if this is their
        ;;                           first contact", section 2(c)).
        hook-plan
        (reduce
         (fn [{:keys [mints encounters] :as acc} h]
           (let [p (:person-id h)
                 own (get first-ordinal p)
                 after-own-arrival? (and own (> (:t h) (nth arrivals own)))
                 encounter (fn [pid cause reason ward-class stay]
                             {:patient-id pid :t (:t h)
                              :steps [{:type :person-encounter :t (:t h) :cause cause
                                       :reason reason :ward-class ward-class
                                       :stay-minutes stay :person-event-id (:event-id h)}]})]
             (case (:event h)
               :delivery
               (let [nb (:newborn-person-id h)
                     nb-persona (get newborn-personas nb)
                     acc (if (and nb-persona after-own-arrival? (alive-at? nb (:t h)))
                           (update acc :mints conj
                                   {:person-id nb :t (:t h) :cause :delivery
                                    :reason "Live birth"
                                    :ward-class :inpatient
                                    :stay-minutes decide/delivery-stay-minutes
                                    :person-event-id (:event-id h)
                                    :mother-patient-id (pid-for own)})
                           acc)]
                 (if (and after-own-arrival? (clinically-idle? own))
                   (update acc :encounters conj
                           (encounter (pid-for own) :delivery "Delivery"
                                      :inpatient decide/delivery-stay-minutes))
                   acc))
               ;; ADR-0173 section 2(d), as an ARRIVAL rather than as a
               ;; coincidence -- `person-fold/hook-kinds` carries the
               ;; measurement that forced it. The window itself is the
               ;; unidentified presentation, so it mints a patient whether
               ;; or not this person already has one: that is the whole
               ;; case, and the resolution below is what joins the two
               ;; records back together (a merge) or fills this one in (a
               ;; fill).
               :identity-unavailable
               (if (alive-at? p (:t h))
                 (update acc :mints conj
                         {:person-id p :t (:t h) :cause :identity-unavailable
                          :reason "Unidentified patient"
                          :ward-class :ed
                          :stay-minutes decide/unidentified-stay-minutes
                          :person-event-id (:event-id h)
                          :placeholder-window (person-fold/window-open-at
                                               (windows-of p) (:t h))})
                 acc)
               :occupational-injury
               (cond
                 (and after-own-arrival? (clinically-idle? own))
                 (update acc :encounters conj
                         (encounter (pid-for own) :occupational-injury
                                    (str "Occupational injury: " (name (or (:injury-class h) :unspecified)))
                                    :ed decide/injury-stay-minutes))
                 ;; No arrival ever bound this person, so this injury IS
                 ;; their first contact with the system.
                 (and (nil? own) (not (bound-persons p)) (some? (persona-of p))
                      (alive-at? p (:t h)))
                 (update acc :mints conj
                         {:person-id p :t (:t h) :cause :occupational-injury
                          :reason (str "Occupational injury: " (name (or (:injury-class h) :unspecified)))
                          :ward-class :ed
                          :stay-minutes decide/injury-stay-minutes
                          :person-event-id (:event-id h)})
                 :else acc)
               acc)))
         {:mints [] :encounters []}
         hook-events)
        ;; ADDITIONAL PATIENTS take ordinals `(+ patients k)` in hook-`:t`
        ;; order, so `patient-id-for` and `mrn-for` stay pure functions
        ;; of an ordinal (ADR-0173 section 2(c)) and the newborn's own
        ;; `:patient` stream is `(stream seed :patient (+ patients k))`
        ;; -- order-free, and disjoint from every t0 arrival's. A person
        ;; who ALREADY has a patient never mints a second one here, so
        ;; the list carries no silent duplicates.
        mints (into [] (map-indexed (fn [k m] (assoc m :ordinal (+ patients k))))
                    (:mints hook-plan))
        hook-rngs (mapv (fn [{:keys [ordinal]}] (stream seed :patient ordinal)) mints)
        ;; A hook patient draws NOTHING: their Persona comes from the
        ;; person side and they walk no module, so `compile-patient`'s
        ;; 4-arity with a nil closure is a pure construction. Their
        ;; `:patient` stream is built all the same, so every patient in
        ;; the run has one.
        mint-registrations (mapv (fn [{:keys [person-id t]}]
                                   (person-fold/registration (persona-of person-id)
                                                             (get events-by-person person-id)
                                                             t))
                                 mints)
        mint-patients (into {}
                            (map-indexed
                             (fn [k {:keys [ordinal person-id mother-patient-id placeholder-window]}]
                               (let [reg (nth mint-registrations k)]
                                 [(pid-for ordinal)
                                  (cond-> {:persona (:persona reg) :compiled nil
                                           :residence (:residence reg)
                                           :person-id person-id}
                                    mother-patient-id (assoc :mother-patient-id mother-patient-id)
                                    placeholder-window
                                    (as-> e (placeholder-registration e placeholder-window)))])))
                            mints)
        ;; --- ADR-0173 section 2(d): THE IDENTIFICATION FLOW ------------
        ;;
        ;; `base-owner` is who a person's canonical patient is BEFORE any
        ;; placeholder resolves: their first identified arrival, or --
        ;; for a newborn, or somebody whose first contact was an injury
        ;; -- the additional patient a hook minted for them.
        base-owner (merge
                    (into {} (for [[p i] first-ordinal]
                               [p {:patient-id (pid-for i) :first-ordinal i
                                   :active-mrn (mrn-for i) :t (nth arrivals i)}]))
                    ;; A PLACEHOLDER mint is not a person's canonical
                    ;; patient any more than a placeholder ARRIVAL is:
                    ;; nobody yet knows whose record it is. The fill
                    ;; promotion below is what can make it one.
                    (into {} (for [{:keys [person-id ordinal t placeholder-window]} mints
                                   :when (nil? placeholder-window)]
                               [person-id {:patient-id (pid-for ordinal) :first-ordinal ordinal
                                           :active-mrn (mrn-for ordinal) :t t}])))
        ;; EVERY placeholder record this run mints, from BOTH sources:
        ;; a t0 arrival that coincided with an open window (section
        ;; 2(d) as written), and a window that minted its own
        ;; unidentified arrival (the same section, met at the rate the
        ;; process actually produces -- `person-fold/hook-kinds` carries
        ;; the measurement).
        placeholders
        (into (into [] (for [i (range patients) :when (placeholder? i)]
                         {:patient-id (pid-for i) :ordinal i :person-id (nth bindings i)
                          :window (nth arrival-windows i) :t (nth arrivals i)}))
              (for [{:keys [ordinal person-id t placeholder-window]} mints
                    :when placeholder-window]
                {:patient-id (pid-for ordinal) :ordinal ordinal :person-id person-id
                 :window placeholder-window :t t}))
        ;; One resolution per placeholder record. A `:merge` with NO
        ;; survivor DEGENERATES TO A FILL -- named in section 2(d) rather
        ;; than left implicit, because silently emitting a merge with a
        ;; null survivor is the defect that sentence exists to prevent.
        resolutions
        (into []
              (for [{:keys [patient-id ordinal person-id window]} placeholders
                    :let [survivor (get base-owner person-id)
                          mergeable? (and (= :merge (:branch window))
                                          survivor
                                          (not= (:patient-id survivor) patient-id)
                                          (<= (:t survivor) (:until-t window)))]
                    :when (some? (:branch window))]
                {:patient-id patient-id :ordinal ordinal :person-id person-id :window window
                 :branch (if mergeable? :merge :fill)
                 :survivor-patient-id (:patient-id survivor)}))
        ;; A person whose ONLY contact was an unidentified arrival gets
        ;; that record as their canonical patient once it is filled --
        ;; section 2(d)'s "the placeholder patient becomes theirs and the
        ;; fold index is updated". The fold starts at the FILL instant
        ;; and not at the arrival: a demographic change reported for
        ;; somebody nobody had identified yet would be a claim the
        ;; modelled hospital was in no position to make.
        person-owner (reduce (fn [acc {:keys [patient-id ordinal person-id window branch]}]
                               (if (or (not= :fill branch) (contains? acc person-id))
                                 acc
                                 (assoc acc person-id
                                        {:patient-id patient-id :first-ordinal ordinal
                                         :active-mrn (mrn-for ordinal) :t (:until-t window)})))
                             base-owner resolutions)
        placeholders-by-person (reduce (fn [acc {:keys [person-id patient-id]}]
                                         (update acc person-id (fnil conj #{}) patient-id))
                                       {} placeholders)
        person-index (into {}
                           (for [[p e] person-owner]
                             [p (assoc (dissoc e :t)
                                       :placeholders (get placeholders-by-person p #{}))]))
        compiled-patients (merge
                           (into {} (for [i firsts]
                                      [(pid-for i)
                                       (cond-> (nth compiled i)
                                         (nth registrations i)
                                         (assoc :residence (:residence (nth registrations i)))
                                         (nth bindings i)
                                         (assoc :person-id (nth bindings i))
                                         (placeholder? i)
                                         (as-> e (placeholder-registration
                                                  e (nth arrival-windows i))))]))
                           mint-patients)
        ;; ADR-0173 section 2(b): THE FOLD IS A QUEUE-SEEDING PASS, not a
        ;; change to the main loop. A person event strictly after its
        ;; person's own first arrival becomes an ordinary queue entry at
        ;; its own `:t`, exactly the way `schedule-followup` already
        ;; inserts one at an absolute instant.
        ;;
        ;; PART 4 widened WHAT "their own first arrival" means, and
        ;; nothing else: it is now `person-owner`'s instant, which for a
        ;; newborn is their birth, for an injury-first patient their
        ;; injury, and for a filled placeholder the fill.
        person-steps (vec (for [[p {:keys [patient-id t]}] (sort-by key person-owner)
                                step (person-fold/steps-after (get events-by-person p) t)]
                            {:patient-id patient-id :steps [step] :t (:t step)}))
        ;; The additional patients' own arrivals, and the encounters both
        ;; hooks put on existing ones.
        mint-steps (vec (for [{:keys [ordinal t cause reason ward-class stay-minutes person-event-id]} mints]
                          {:patient-id (pid-for ordinal) :t t
                           :steps [{:type :registered :closure nil}
                                   {:type :person-encounter :t t :cause cause :reason reason
                                    :ward-class ward-class :stay-minutes stay-minutes
                                    :person-event-id person-event-id}]}))
        ;; BOTH branches are queued ON THE PLACEHOLDER, and the merge
        ;; carries the fill's payload alongside its survivor: `decide
        ;; :identification-merge` degenerates to the fill when the world
        ;; refuses the merge, and it can only do that if the payload
        ;; rode the step. See that method's own docstring for the
        ;; population-scale failure that put it there.
        resolution-steps
        (vec (for [{:keys [patient-id person-id window branch survivor-patient-id]} resolutions
                   :let [reg (person-fold/registration (persona-of person-id)
                                                       (get events-by-person person-id)
                                                       (:until-t window))
                         payload {:t (:until-t window)
                                  :persona (:persona reg) :residence (:residence reg)
                                  :person-event-id (:resolution-event-id window)}]]
               {:patient-id patient-id :t (:until-t window)
                :steps [(if (= :merge branch)
                          (assoc payload :type :identification-merge
                                 :survivor-patient-id survivor-patient-id)
                          (assoc payload :type :identity-fill))]}))
        ;; ONE list, in a fixed category order -- the demographic fold,
        ;; then the hooks' own arrivals, then the encounters they put on
        ;; EXISTING patients, then the identification resolutions.
        ;; `[t seq-no]` is the queue's key, so `:t` decides everything
        ;; that matters and this order only settles ties,
        ;; deterministically.
        seeded-steps (-> person-steps
                         (into mint-steps)
                         (into (:encounters hook-plan))
                         (into resolution-steps))]
    {:world-rng world-rng
     :patient-rngs (into patient-rngs hook-rngs)
     :arrivals arrivals
     :mrn-for mrn-for
     :pid-for pid-for
     :bindings bindings
     :first-ordinal first-ordinal
     :firsts firsts
     :pid-of pid-of
     :person-index person-index
     :registrations registrations
     :initial-entries initial-entries
     :compiled compiled
     :compiled-patients compiled-patients
     :mints mints
     :placeholders placeholders
     :resolutions resolutions
     :hook-patient-ordinals (mapv :ordinal mints)
     :person-steps person-steps
     :seeded-steps seeded-steps}))

(defn person-plan
  "ADR-0173 ruling C1's own resolution, exported. Returns, for a config
  `run` would accept:

    {:bindings   [person-id-or-nil ...]  -- by arrival ordinal
     :deaths     {person-id <compiled death instant>}
     :person-index {person-id {:patient-id .. :first-ordinal ..
                               :active-mrn ..}}}

  WHY IT EXISTS. Ruling C1 gives the person process the COMPILED
  trajectory's death instant as a t0 parameter, keyed by person. The
  engine mints a patient id from an arrival ordinal, and the binding
  from person to ordinal is a `:world`-family draw taken at a pinned
  position inside this run -- so a caller cannot key those deaths
  without asking the engine which person each arrival bound to. This is
  that question, answered by the same code `run` itself uses, so the two
  cannot disagree.

  THE DEATH INSTANT IS EXACTLY COMPUTABLE, which is the part that could
  have failed. `compile-trajectory` emits its bridging delays as
  `{:type :delay :from g :to g}`, and ADR-0171 section 2(d) made
  `:from` = `:to` DRAW-FREE -- so every advance in a compiled prefix is
  a constant and the death instant is the arrival instant plus sixty
  times the gap-minutes standing before the first `:discharge` carrying
  `:disposition :expired`. No simulation needed. Had compiled delays
  been ranges, this would have been structurally impossible.

  A person who binds to no arrival, or whose bound arrival compiles no
  expiring discharge, is ABSENT from `:deaths` and keeps their own drawn
  death.

  ARC 3A PART 4, DISCLOSED: so is a person whose only patient a HOOK
  minted -- a newborn, an unidentified presentation, or a first-contact
  injury arrival. Those patients exist only because the person STREAM
  produced them, and the stream is what `:deaths` is computed to
  produce, so feeding their compiled deaths back would re-open ruling
  C1's cycle at a second point and need a THIRD pass. Two passes is what
  C1 resolved to and what `ehrt.sim.run/engine-persons` implements; a
  third is not designed. The conservatism runs the safe way, as the
  alive filter's does: such a person keeps their own DRAWN death, which
  the stream already respects by emitting nothing after it."
  [config]
  (let [{:keys [arrivals bindings compiled person-index first-ordinal]} (prelude config)
        death-of (fn [i]
                   (loop [steps (:steps (:compiled (nth compiled i))) t (nth arrivals i)]
                     (when-let [step (first steps)]
                       (cond
                         (and (= :discharge (:type step)) (= :expired (:disposition step))) t
                         (= :delay (:type step)) (recur (rest steps) (+ t (* 60 (long (:from step)))))
                         :else (recur (rest steps) t)))))]
    {:bindings bindings
     :person-index person-index
     :deaths (into {} (for [[p i] first-ordinal
                            :let [d (death-of i)]
                            :when d]
                        [p d]))}))

(defn run
  "Runs the simulation. config:
    :seed             long (required, non-negative -- sim/ADR-0116: a
                      negative :seed returns result/error :invalid-seed
                      rather than running)
    :patients         number of patients (default 1)
    :pathway          a pathway IR map (default sim-model/sample-admission-discharge)
    :pathways         M3-adjacent: sim-model/PathwaysConfig
                      -- a vector of weighted-pool ({:pathway :weight})
                      and/or explicit ({:patient-ordinal :pathway})
                      entries, `assign-pathway`'s own input. When
                      present, EVERY patient's pathway comes from this
                      (not :pathway, which is then ignored) and consumes
                      one additional RNG draw per patient (fixed
                      consumption, see `assign-pathway`'s docstring).
                      ABSENT ENTIRELY (not merely nil) -- the default --
                      means every patient gets the plain :pathway
                      config, exactly as before this option existed: no
                      new draw, byte-identical output, the reason the
                      pinned fixture (no :pathways key) survives this
                      milestone untouched.
    :arrival-gap      max MINUTES between successive patient arrivals
                      (default 60; actual gaps sampled from the seeded
                      RNG). Stays minutes, converted to seconds at the
                      point arrivals are computed -- symmetric to
                      :delay's own minutes-authored/seconds-internal
                      split (sim/ADR-0011), and empirically necessary: an
                      earlier draft left this in raw seconds while
                      :delay's dwell times (minutes*60) stayed
                      comparatively huge, so arrivals clustered far
                      faster than patients discharged and blew past
                      sim-model/default-facility's real usable capacity
                      (16 concurrent, not its nominal 18 -- Cardiology's
                      surge sits unused when every patient's home-ward
                      is Renal) at patient counts the property tests
                      already exercised. Keeping both minutes-scaled
                      preserves the calibration that made that headroom
                      real.
    :warm-up-seconds  events with :t less than this get :warm-up true
                      (default 0; sim/ADR-0011 -- the log stays complete,
                      no trimming here)
    :facility         facility config (default sim-model/default-facility)
    :providers        provider templates (default sim-model/default-provider-templates;
                       NPIs are generated from THIS run's seed -- sim/ADR-0007)
    :order-profiles   M3: ehrt.sim-engine.order-profiles/OrderProfiles map
                      (default order-profiles/default-profiles) -- :order
                      steps look up their :profile key here.
    :churn-profile    ehrt.sim-engine.churn/ChurnProfile map (default nil
                       -- churn OFF). M2b: when present, InjectChurn runs
                       ONCE PER PATIENT (in arrival-ordinal order, a fixed
                       point in the draw sequence) against THIS PATIENT's
                       own `:patient`-family stream (ADR-0171) -- churn's
                       rows are PATIENT-scoped, so injecting churn into
                       patient N's pathway reaches no other patient --
                       between building each patient's step queue and the
                       main loop. Absent entirely (not merely all-zero),
                       this stage never runs and consumes no RNG: the
                       reason a config with no :churn-profile key
                       reproduces byte-identical pre-M2b output (the
                       pinned fixture; churn is opt-in, sim/ADR-0009's
                       accept-and-record policy doesn't even apply here
                       since nothing about this path changed).
    :persona-config   M4: sim-model/persona's own config
                      map ({:age-min :age-max :payers-under-65
                      :payers-65-plus}, all optional -- see that
                      function's docstring for defaults). EVERY
                      patient's step queue is prepended with an
                      engine-internal `:registered` step (never
                      authorable IR, the same treatment
                      :result-followup already gets) that samples
                      exactly one persona per patient, always -- this is
                      NOT opt-in the way :churn-profile/:pathways are:
                      Persona is a landed part of Execute's own step
                      vocabulary now (docs/sim-theory.edn), so this
                      milestone's own fixture regeneration is expected
                      and documented (sim/ADR-0009 policy), not guarded
                      against the way M2b/M3's opt-in additions were.
    :modules          M5b, hard-switched at ADR-0033 (AR-2): a vector of
                      ALREADY-LOADED, CLOSURE-SHAPED entries
                      (ehrt.patient-simulator.gmf/load-closure's own :ok
                      payload -- {:root :modules :tables}, plus an
                      optional :initial-attributes a caller's own config
                      may attach, AR-1) -- this namespace does no file
                      I/O of its own, ehrt.sim.run's job, the same
                      layering :facility/:providers/:order-profiles
                      already follow. Looked up by :root against
                      :module-assignment's own resolution. A standalone
                      module with no CallSubmodule embeds as the
                      singleton closure ({:root id :modules {id module}
                      :tables {}}, ehrt.patient-simulator.gmf/singleton-
                      closure) -- draw-neutral and byte-neutral versus
                      the pre-ADR-0033 bare-module shape (AR-4).
    :module-assignment M5b: ehrt.patient-simulator.gmf/ModulesConfig -- the
                      SAME weighted-pool/explicit-ordinal shape
                      :pathways already establishes, `assign-module`'s
                      own input, ONE additional fixed RNG draw per
                      patient when present (assign-pathway's own fixed-
                      consumption, sim/ADR-0009, law, extended). ABSENT
                      ENTIRELY (not merely nil or []) -- the default --
                      means no patient ever walks a module: no draw, no
                      :closure carried on any :registered step, BYTE-
                      IDENTICAL to pre-M5b output (the pinned fixture;
                      the same opt-in law :pathways/:churn-profile
                      already establish). A patient's own compiled
                      module content is PREPENDED onto whatever
                      :pathway/:pathways already queued for them, never
                      a replacement -- both are just IR entering the SAME
                      queue (the pathway-ir union, docs/sim-theory.edn).
                      A caller wanting MODULE-ONLY patients must pass an
                      explicit empty pathway (`{:name ... :steps []}`):
                      the DEFAULT :pathway (sample-admission-discharge)
                      otherwise still runs AFTER the module's own compiled
                      content, and usually conflicts with it (the module's
                      own encounter already admitted/discharged this
                      patient-id; the default pathway assumes a fresh
                      :new patient) -- exactly the same compose-don't-
                      second-guess-the-author posture InjectChurn already
                      takes toward whatever pathway it's handed.
    :module-horizon-days M5b: how many days past this run's own
                      registration instant (sim-model/reference-today-
                      epoch-day) an assigned module's own walk runs
                      before stopping (`ehrt.patient-simulator.gmf-interpreter/
                      run-module`'s own optional `horizon-end-t` bound)
                      -- REQUIRED to be finite for any real module walk
                      to terminate (components/patient-simulator/docs/gmf-interpreter.md section 8
                      item 5's own finding: a vendored module may have
                      no Terminal state and no Guard to block on).
                      Ignored entirely when no patient has an assigned
                      module.
    :history          Wave H pre-roll (2026-08-04, ADR-0042 AR-3): opt-
                      in gate for the interpreter's own `:phase` mint
                      and CompileTrajectory's new uniform-drop/straddle-
                      inheritance path (`ehrt.patient-simulator.gmf-
                      interpreter/run-module`'s and `ehrt.patient-simulator.
                      compile-trajectory/compile-trajectory`'s own
                      docstrings have the mechanism). Default `false` --
                      absent, the pre-H `:pre-horizon`/`:registration-
                      facts` mechanics run unchanged, byte-identical to
                      every pre-H run (ADR-0042 AR-5's own pure-identity
                      bracket). Ignored entirely when no patient has an
                      assigned module (there is no history phase to gate
                      without one).

  THE PRE-LOOP `:patient`-FAMILY DRAW ORDER, PINNED (ADR-0173 ruling C1
  requires this to be stated here, because it is the one thing about the
  compile's new position that is a CHOICE rather than a consequence).
  For each arrival ordinal i, in ordinal order, before the run loop
  starts, patient i's own `:patient` stream is drawn from in exactly
  this order:

    1. `module-for`   -- `assign-module`, 1 draw, only with :module-assignment
    2. `pathway-for`  -- `assign-pathway`, 1 draw, only with :pathways
    3. `steps-for`    -- `churn/inject`, only with :churn-profile
    4. `compile-patient` -- the Persona (13 draws, 16 with demographic
       weights) and then the module walk (unbounded), only ever in that
       order and only for a patient with an assigned closure

  Steps 1-3 are `initial-entries`; step 4 is `compiled-patients`. Steps
  1-3 are exactly where they have always been. Step 4 is where ADR-0173
  C1 MOVED the persona draw and the module walk to, out of `decide
  :registered` -- a move in TIME only: patient i's stream is read by
  nothing but patient i's own decides, so the sequence each stream sees
  is unchanged and every run stays byte-identical (`compile-patient`'s
  own docstring carries the input-by-input argument;
  `bin/regression-oracle` carries the proof). Everything after step 4 is
  loop-time: `decide :delay` and `decide :order`, the only other two
  `:patient`-family draw sites in this namespace.

  Returns {:ground-truth [event ...] :state-history {patient-id [state
  ...]} :facility .. :providers [materialized-provider ...]}. The
  facility and MATERIALIZED providers (real NPIs, not just templates)
  are echoed back so a caller rendering this run's log
  (ehrt.sim-emit-hl7.emit/emit needs facility + providers for PV1)
  uses the EXACT config this run allocated against, not a fresh default
  that might not even share ward names. ground-truth is format-free,
  ordered by [t seq-no]; emitters consume it and test assertions target
  it directly (a first-class output, per the problem statement).
  state-history is DERIVED (sim/ADR-0008; sim-theory.md open question #3)
  -- (get state-history patient-id) is exactly (rest (reductions evolve
  (initial-patient patient-id mrn) (events for patient-id))), proven as
  a property test (engine-test/patient-state-is-a-fold-of-the-log)
  rather than assumed; the engine computes it as a byproduct of the
  loop below because decide needs live world state to make its next
  decision, not because it's a second source of truth."
  [{:keys [seed patients pathway pathways arrival-gap warm-up-seconds facility providers churn-profile order-profiles
           persona-config modules module-assignment module-horizon-days history persons encounters
           bed-cycle scheduling]
    :or {patients 1
         pathway sim-model/sample-admission-discharge
         arrival-gap 60
         warm-up-seconds 0
         facility sim-model/default-facility
         providers sim-model/default-provider-templates
         order-profiles order-profiles/default-profiles
         persona-config {}
         modules []
         module-horizon-days 90
         history false}}]
  {:pre [(some? seed) (sim-model/valid? pathway)
         (or (nil? pathways) (sim-model/valid-pathways-config? pathways))]}
  (if (neg? seed)
    ;; sim/ADR-0116 (R9): the seed contract is non-negative longs -- a
    ;; negative seed reaches the invariant catalog unvalidated otherwise
    ;; (the engine-test flake investigation's own shrunk counterexample,
    ;; R8/R9), so this is a guard clause at entry, not a throw.
    (result/error :invalid-seed {:key :seed :value seed :expected "a non-negative integer"})
    ;; ADR-0173 section 2(a) (arc 3a part 3): `:persons` is the ONE key
    ;; here whose value is a whole data structure a caller built rather
    ;; than a scalar or a schema'd config, so it is checked at entry the
    ;; same result-not-throw way `:seed` is -- a malformed pool would
    ;; otherwise surface as a nil-pointer somewhere inside the pre-loop,
    ;; which is the failure mode sim/ADR-0116 R9 was written against.
    (if (and (some? persons) (not (config/valid-persons? persons)))
      (result/error :invalid-persons
                    {:key :persons
                     :expected (str "{:population [{:person-id <string> :id-tag <int>} ...] "
                                    ":personas {person-id Persona} :alive {person-id <int>} "
                                    ":events [{:event <keyword> :t <int> :person-id <string>} ...]}")})
    (if (and (some? scheduling) (not (config/valid-scheduling? scheduling)))
      ;; ARC 3B SWEEP 3: `:scheduling` is the second key whose value is a
      ;; whole authored map rather than a scalar, so it is checked at
      ;; entry the same result-not-throw way `:persons` and `:seed` are.
      ;; The band-sum half matters most: rates summing past 1 would
      ;; silently starve the last band rather than fail.
      (result/error :invalid-scheduling
                    {:key :scheduling
                     :expected (str "{:scheduled-fraction <0..1> :lead-time-days [lo hi] "
                                    ":no-show-rate <0..1> :reschedule-rate <0..1> "
                                    ":cancel-rate <0..1> :follow-up {:rate <0..1> "
                                    ":interval-days [lo hi]}}, the three outcome rates "
                                    "summing to at most 1")})
    (let [;; ADR-0173 section 2(a) (arc 3a part 3): the pre-loop lives in
        ;; `prelude`, one function, because `ehrt.sim.run` must be able to
        ;; ask the same question `run` answers -- which person each
        ;; arrival bound to, and what that arrival compiled -- BEFORE the
        ;; run (`person-plan`). `:persons` ABSENT makes every one of its
        ;; person-aware branches the expression that was there before.
        {:keys [world-rng patient-rngs arrivals mrn-for pid-for firsts pid-of
                person-index initial-entries compiled-patients seeded-steps
                hook-patient-ordinals]}
        (prelude {:seed seed :patients patients :pathway pathway :pathways pathways
                  :arrival-gap arrival-gap :facility facility :churn-profile churn-profile
                  :persona-config persona-config :modules modules
                  :module-assignment module-assignment
                  :module-horizon-days module-horizon-days :history history
                  :persons persons :encounters encounters
                  ;; ARC 3B SWEEP 3: the split's own two `:world` draws
                  ;; happen inside `prelude`, so `person-plan` -- the
                  ;; second caller -- sees the IDENTICAL stream position
                  ;; `run` does. That agreement is the whole reason the
                  ;; pre-loop lives in one function.
                  :scheduling scheduling})
        facility-rng (stream seed :facility 0)
        ;; Provider NPIs are generated from this run's seed (sim/ADR-0007),
        ;; drawn once up front -- before arrival staggering -- so
        ;; provider identity is as deterministic and as fixed-order as
        ;; everything else this RNG produces. FACILITY (ADR-0171 ruling
        ;; E1): it reads no patient state, so adding a provider template
        ;; no longer shifts arrival gaps or bed choices.
        materialized-providers (sim-model/materialize-providers facility-rng providers)
        ;; An arrival with no steps at all is not queued: that is a REPEAT
        ;; arrival of a person already registered (ADR-0173 section 2(a)),
        ;; and an empty queue entry would reach `decide` with a nil step.
        arrival-queue (into (sorted-map)
                            (for [i (range patients)
                                  :when (seq (:steps (nth initial-entries i)))]
                              [[(nth arrivals i) i]
                               {:patient-id (pid-of i) :steps (:steps (nth initial-entries i))}]))
        ;; ADR-0173 section 2(b): the fold, seeded into the SAME sorted
        ;; queue at each person event's own absolute `:t`. Seq numbers
        ;; start where the arrivals' end, so an event landing on an
        ;; arrival instant still sorts after that arrival's own
        ;; `:registered`, and the loop's own counter starts past all of
        ;; them.
        ;;
        ;; PART 4 puts the two hooks' own entries -- an additional
        ;; patient's whole arrival, and an encounter on an existing one
        ;; -- and the identification resolutions into this SAME list, at
        ;; their own `:t`. `prelude` fixes their order; the queue's
        ;; `[t seq-no]` key is what actually decides.
        init-queue (into arrival-queue
                         (map-indexed (fn [k {:keys [patient-id steps t]}]
                                        [[t (+ patients k)] {:patient-id patient-id :steps steps}]))
                         seeded-steps)
        seq-start (+ patients (count seeded-steps))
        init-world {:patients (into {} (for [i (concat firsts hook-patient-ordinals)]
                                         [(pid-for i) (state/initial-patient (pid-for i) (mrn-for i))]))
                    :facility facility
                    :providers materialized-providers
                    :order-profiles order-profiles
                    :persona-config persona-config
                    :module-horizon-days module-horizon-days
                    :history history
                    ;; Task 1 (M2b): cancel-family/transfer-in-error decide
                    ;; methods query the log directly for the event they
                    ;; reinstate from (docs/patient-state-model.md's
                    ;; shadow-field dissolution) -- a PERSISTENT mirror of
                    ;; the log-so-far, kept alongside (not instead of) the
                    ;; transient `ground-truth` accumulator below so decide
                    ;; can `nth`/`filter`/`keep-indexed` over it (transients
                    ;; aren't seqable). Always a prefix of the final log.
                    :ground-truth []
                    ;; ADR-0169 (arc 0): log index -> the state that
                    ;; event's subject was in immediately BEFORE it, for
                    ;; the two REINSTATABLE classes only (:transfer,
                    ;; :discharge). What `reinstated-state` reads instead
                    ;; of replaying the whole log per cancel. Written
                    ;; below, inside the same fold that produces `world'`,
                    ;; because that fold is where the pre-event state
                    ;; exists -- a second pass could not see it.
                    ;;
                    ;; The KEY's presence is what tells `reinstated-state`
                    ;; this world came from `run`; a hand-built world has
                    ;; no such key and keeps the replay path.
                    :reinstate-index {}
                    ;; ADR-0169 (arc 0), the ADR-0164 scans' own carrier:
                    ;; [opening-type patient-id citation] -> the LAST log
                    ;; index carrying that combination. A later occurrence
                    ;; overwrites an earlier one, which is precisely what
                    ;; the `last` in the scan it replaces meant. Read by
                    ;; `last-cited-index`.
                    :citation-index {}
                    ;; ADR-0173 ruling C1 (arc 3a): patient-id -> that
                    ;; patient's `compile-patient` result, drawn at RUN
                    ;; START (in `prelude`) rather than at the patient's
                    ;; own arrival. Read by `decide :registered`, which
                    ;; falls back to compiling in place when this KEY is
                    ;; absent -- the hand-built-world tolerance
                    ;; `:reinstate-index` and `:citation-index` already
                    ;; establish.
                    :compiled-patients compiled-patients
                    ;; ADR-0173 section 2(a) (arc 3a): person-id -> the
                    ;; patient that person resolves to:
                    ;;
                    ;;   {person-id {:patient-id .. :first-ordinal ..
                    ;;               :active-mrn ..}}
                    ;;
                    ;; The engine mints a patient id from an ARRIVAL
                    ;; ORDINAL (`patient-id-for`), so nothing in the tree
                    ;; can make a returning PERSON resolve to the same
                    ;; patient without a carried index. This is that
                    ;; index. EMPTY with no `:persons` key, and part 4
                    ;; grows each entry a `:placeholders` set when the
                    ;; identification flow lands. Same carried-state
                    ;; precedent -- and the same hand-built-world
                    ;; tolerance, on the KEY and never on a missing entry
                    ;; -- as `:reinstate-index`, `:citation-index` and
                    ;; `:compiled-patients` above.
                    :person-index person-index
                    ;; ADR-0173 section 2(d) (arc 3a part 4): patient-id
                    ;; -> the LOG INDEX of that patient's own
                    ;; `:registered` event, written in the same fold that
                    ;; produces `world'`. `decide :identity-fill` reads it
                    ;; for `:placeholder-event-id`, which is the ONE
                    ;; referential key this arc mints -- and it is read
                    ;; from a carried index rather than found by a scan,
                    ;; the shape ADR-0169 gave `:citation-index` for
                    ;; exactly this reason. Same hand-built-world
                    ;; tolerance, on the KEY and never on a missing
                    ;; entry.
                    :registration-index {}
                    ;; ARC 3B SWEEP 1 (ADR-0174 ruling B1): what an
                    ;; opener needs to MINT an `:encounter-id` -- this
                    ;; run's seed and every patient's arrival ordinal --
                    ;; carried rather than re-derived, because a
                    ;; patient-id is a STRING and parsing the ordinal
                    ;; back out of it would make the id format
                    ;; load-bearing in a second place.
                    ;;
                    ;; NIL UNLESS THE RUN OPTED IN, which is
                    ;; indistinguishable from absent to every reader of
                    ;; it and is exactly what a hand-built world already
                    ;; looks like. That nil IS the opt-in law: no
                    ;; minting, no `:encounter-id` on any event, and
                    ;; therefore the bytes this engine has always
                    ;; produced. Same tolerance as the four indexes above
                    ;; -- on the KEY, never on a missing ordinal entry.
                    :encounter-minting (when encounters
                                         {:seed seed
                                          :ordinals (into {} (for [i (concat firsts hook-patient-ordinals)]
                                                               [(pid-for i) i]))})
                    ;; ARC 3B SWEEP 2 (ADR-0174 section 2(c)): every
                    ;; licensed bed and surge slot the facility declares,
                    ;; born `:ready` at t 0.
                    ;;
                    ;; NIL UNLESS THE RUN OPTED IN, which is
                    ;; indistinguishable from absent to every reader --
                    ;; `sim-model/free` falls back to "nobody is in it",
                    ;; `vacate-bed` returns nil, and no tick is ever
                    ;; queued. Same tolerance as every index above: on
                    ;; the KEY, never on a missing bed entry.
                    :beds (when bed-cycle (sim-model/initial-beds facility))
                    ;; ARC 3B SWEEP 3 (ADR-0174 section 2(b)): the six
                    ;; sub-keys, verbatim, plus the minting index that
                    ;; carries `appointment-id-for`'s two arguments.
                    ;;
                    ;; NIL UNLESS THE RUN OPTED IN, on the same law as
                    ;; the three above: no split, no appointment, no
                    ;; follow-up, no draw, and therefore the bytes this
                    ;; engine has always produced.
                    :scheduling scheduling
                    :appointment-minting (when scheduling
                                           {:seed seed
                                            :ordinals (into {} (for [i (concat firsts hook-patient-ordinals)]
                                                                 [(pid-for i) i]))})}
        mark-warmup (fn [ev] (assoc ev :warm-up (< (:t ev) warm-up-seconds)))
        ;; ADR-0171: what `decide` receives. The two run-scoped families
        ;; are fixed for the whole loop; `:patient` is swapped per queue
        ;; entry below off this map, keyed by patient-id because that is
        ;; what a queue entry carries (including the :order follow-ups
        ;; scheduled mid-run).
        base-streams {:world world-rng :facility facility-rng}
        ;; ARC 3A PART 3: keyed by the FIRST arrival ordinal, so a
        ;; patient's draws stay one continuing sequence (`run`'s own
        ;; reason for building `patient-rngs` up front). The consequence,
        ;; stated: a repeat arrival's own ordinal has a `:patient` stream
        ;; that is never used. It is still CONSTRUCTED -- the `mapv` in
        ;; `prelude` is unconditional and draw-free -- so nothing shifts.
        ;; PART 4: hook-minted patients take ordinals `(+ patients k)`
        ;; and their streams sit at those indices, appended by `prelude`.
        ;; They draw nothing -- no persona (it comes from the person
        ;; side), no module walk, and a `:from` = `:to` delay is
        ;; draw-free -- so each one is constructed and never read.
        streams-by-pid (into {} (for [i (concat firsts hook-patient-ordinals)]
                                  [(pid-for i) (nth patient-rngs i)]))
        final-result (fn [ground-truth state-history extra]
                       (merge {:ground-truth (persistent! ground-truth)
                               :state-history state-history
                               :facility facility
                               :providers materialized-providers}
                              extra))]
    ;; Past every seq-no the queue was SEEDED with -- the arrivals' own
    ;; ordinals and then one per queue-seeded person step -- so a
    ;; mid-run `schedule-followup` can never collide with a person
    ;; event's key. With no `:persons` this is `patients`, verbatim.
    (loop [queue init-queue
           seq-no seq-start
           world init-world
           ground-truth (transient [])
           state-history {}]
      (if (empty? queue)
        (final-result ground-truth state-history nil)
        (let [[[t _] {:keys [patient-id steps]} queue'] (pop-min queue)
              [step & remaining] steps]
          (if (= :merged (get-in world [:patients patient-id :status]))
            ;; M2b: a merge (decided while processing a DIFFERENT
            ;; patient's step -- the survivor's) can end this patient-
            ;; id's stream mid-pathway, asynchronously to their own
            ;; queue. "The merged patient-id's stream ends with a
            ;; terminal merged-into event" (docs/patient-state-model.md)
            ;; means exactly this: their own remaining queued steps are
            ;; abandoned here, never decided, never emitting further
            ;; events -- the run loop's own enforcement of
            ;; no-events-after-merged-terminal, not just a check.clj
            ;; invariant asserted after the fact.
            (recur queue' seq-no world ground-truth state-history)
            (let [{:keys [events advance exhausted schedule-followup prepend-steps]}
                  (decide/decide (assoc base-streams :patient (get streams-by-pid patient-id))
                          t world patient-id step)]
              ;; A :rejected decide outcome (an illegal cancel/bed-swap/
              ;; merge -- Task 1's validity-table enforcement) is NOT a
              ;; run-halting condition, unlike :exhausted: it means THIS
              ;; one step doesn't happen (already :events [] :advance 0),
              ;; not that the simulation can no longer proceed at all.
              ;; This matters for InjectChurn (M2b): a churned step can be
              ;; legal when INSERTED (per the applicability oracle, a
              ;; static analysis) yet collide with live world state by
              ;; the time it actually executes (e.g. a bed a cancel-
              ;; discharge would reinstate into has since been reclaimed
              ;; by someone else's admission) -- that step is simply
              ;; skipped, and the patient's OWN remaining steps proceed
              ;; normally. check.clj remains the independent safety net
              ;; for any log, authored or generated.
              (cond
                exhausted (final-result ground-truth state-history {:exhausted exhausted})
                :else
            (let [;; ARC 3B SWEEP 1: the encounter stamp rides here, off
                  ;; `world` as it stands BEFORE this batch, for the same
                  ;; reason `:reinstate-index` is written in the fold
                  ;; below -- the pre-event state exists at this point
                  ;; and nowhere later. Per event, not once for the
                  ;; batch: a `:discharge` decide can emit a bed-ready
                  ;; `:transfer` for a DIFFERENT patient, whose own open
                  ;; encounter is the one that transfer belongs to.
                  events (mapv (comp mark-warmup (partial encounters/stamp-encounter world)) events)
                  base-idx (count (:ground-truth world))
                  ;; ADR-0169: the patient-state fold and the reinstate
                  ;; index are built in ONE pass, because the index's
                  ;; value IS this fold's accumulator one step early --
                  ;; `w` before `ev` is applied. A `:discharge` decide can
                  ;; emit two events (the discharge, then a bed-ready
                  ;; :transfer for a DIFFERENT patient), so the subject is
                  ;; read off each event rather than assumed to be
                  ;; `patient-id`, and the state is captured per event
                  ;; rather than once for the batch.
                  [world' reinstate' citations' registrations']
                  (reduce (fn [[w ridx cidx gidx] [offset ev]]
                            (let [idx (+ base-idx offset)
                                  subject (:patient-id (first (:participants ev)))
                                  ridx' (if (log-index/reinstatable-event-types (:event ev))
                                          (assoc ridx idx (get-in w [:patients subject]))
                                          ridx)
                                  cidx' (if (and (log-index/cited-opening-event-types (:event ev))
                                                 (some? (:citation ev)))
                                          (reduce (fn [ci {:keys [patient-id]}]
                                                    (assoc ci [(:event ev) patient-id (:citation ev)] idx))
                                                  cidx (:participants ev))
                                          cidx)
                                  ;; ADR-0173 section 2(d): one more index
                                  ;; off the SAME fold, for the same
                                  ;; reason the two above are here -- the
                                  ;; log index exists at this point and
                                  ;; nowhere later.
                                  gidx' (if (= :registered (:event ev))
                                          (assoc gidx subject idx)
                                          gidx)]
                              ;; ARC 3B SWEEP 2: the participant filter,
                              ;; and the bed index folded in the SAME
                              ;; pass for the same reason the three
                              ;; indexes above are -- the pre-event and
                              ;; post-event patient maps both exist here
                              ;; and nowhere later.
                              (let [w-next (reduce (fn [w2 {:keys [patient-id]}]
                                                     (update-in w2 [:patients patient-id] evolve/evolve ev))
                                                   w (filter :patient-id (:participants ev)))]
                                [(cond-> w-next
                                   (:beds w-next)
                                   (assoc :beds (fold/update-beds (:beds w-next) ev
                                                                  (:patients w) (:patients w-next))))
                                 ridx' cidx' gidx'])))
                          [world (:reinstate-index world) (:citation-index world)
                           (:registration-index world)]
                          (map-indexed vector events))
                  world'' (assoc world'
                                 :ground-truth (into (:ground-truth world) events)
                                 :reinstate-index reinstate'
                                 :citation-index citations'
                                 :registration-index registrations')
                  ground-truth' (reduce conj! ground-truth events)
                  ;; ARC 3B SWEEP 2: same filter, same reason -- a bed
                  ;; participant has no patient whose history to append
                  ;; to, and a nil key here would put a phantom patient
                  ;; in `:state-history` for `patient-state-is-a-fold-of-
                  ;; the-log` to trip over.
                  state-history' (reduce (fn [sh ev]
                                            (reduce (fn [sh2 {:keys [patient-id]}]
                                                      (update sh2 patient-id (fnil conj [])
                                                              (get-in world' [:patients patient-id])))
                                                    sh (filter :patient-id (:participants ev))))
                                          state-history events)
                  ;; M3: :order's decide may ask for a follow-up queue
                  ;; entry (the auto-paired :result -- see engine's :order
                  ;; docstring for why it rides the REAL queue instead of
                  ;; being spliced into :events directly). Scheduled at
                  ;; its own [t seq-no], same as any other event -- this
                  ;; is what keeps ground-truth in true global time order
                  ;; even though the result's CONTENT was fully decided
                  ;; back when the order was placed.
                  ;;
                  ;; ARC 3B SWEEP 3 (ADR-0174 section 2(b)): ONE decide can
                  ;; now owe TWO -- `decide :discharge` under `:scheduling`
                  ;; dirties a bed AND books a follow-up -- so a decide may
                  ;; hand back a SEQUENCE here. A single map is normalised
                  ;; to a one-element sequence and takes the identical
                  ;; path, which is why every other site is untouched and
                  ;; why this cannot drift into two readings.
                  ;;
                  ;; THE ADR SAID NOTHING ABOUT THE MAIN LOOP WOULD CHANGE
                  ;; and this contradicts it, minimally and on purpose: its
                  ;; section 2(b) was written before sweep 2 gave the
                  ;; discharge a followup of its own, so the collision it
                  ;; did not foresee is real. Each entry still lands at its
                  ;; own `[t seq-no]`, in the order the decide listed them.
                  [queue'' seq-no']
                  (reduce (fn [[q n] sf]
                            [(assoc q [(:t sf) n] (select-keys sf [:patient-id :steps]))
                             (inc n)])
                          [queue' seq-no]
                          (cond (nil? schedule-followup) nil
                                (map? schedule-followup) [schedule-followup]
                                :else schedule-followup))
                  ;; M5b Task 4: :registered's own decide call may ask for
                  ;; compiled module steps to run BEFORE whatever was
                  ;; already queued (this patient's own authored pathway,
                  ;; if any) -- spliced onto the FRONT of `remaining`,
                  ;; never replacing it: module-compiled and authored IR
                  ;; are both just steps entering the SAME queue (the
                  ;; pathway-ir union, docs/sim-theory.edn), not two
                  ;; competing sources.
                  remaining' (into (vec prepend-steps) remaining)]
              (if (seq remaining')
                (recur (assoc queue'' [(+ t advance) seq-no'] {:patient-id patient-id :steps remaining'})
                       (inc seq-no') world'' ground-truth' state-history')
                (recur queue'' seq-no' world'' ground-truth' state-history'))))))))))))))
