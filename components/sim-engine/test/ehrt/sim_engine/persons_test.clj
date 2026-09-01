(ns ehrt.sim-engine.persons-test
  "ADR-0173 section 2, arc 3a part 3: the engine half of the demographic
  fold -- the `:persons` config key, ruling A1's arrival selection, the
  queue-seeding pass, and the two kinds it mints.

  THE PERSON EVENTS HERE ARE HAND-AUTHORED, and that is a design choice
  rather than a shortcut. The engine's own contract is that person
  events arrive as DATA (`engine/run`'s `:persons` docstring): it folds
  a vector of maps and never learns whose they are, and it may not
  require the component that draws them at all (ADR-0172 limitations
  row 10, the one-way edge). So this namespace exercises the fold over
  a stream it controls exactly, which is the only way to put a
  `:person-death`, an `:at-t0` residence loss and all three
  `:demographic-update` causes into one small deterministic run. That
  the REAL stream composes with this engine is a separate gate over the
  real component, `ehrt.sim.persons-run-test`.

  Every population here is asserted non-empty before anything is
  asserted about it (`rulings.md#R-empty-population-is-red`): a fold
  test over a run that folded nothing agrees with everything."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-engine.config :as config]
            [ehrt.sim-engine.evolve :as evolve]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.state :as state]
            [ehrt.sim-engine.streams :as streams]
            [ehrt.sim-model.interface :as sim-model]))

;; --- the fixture population ----------------------------------------------

(def ^:private seed 15)

(def ^:private addr-a {:street "1 Fixture Way" :city "Springfield" :state "IL" :zip "62701"})
(def ^:private addr-b {:street "2 Fixture Way" :city "Shelbyville" :state "IL" :zip "62565"})

(defn- persona-for
  "The t0 Persona of person `id-tag`, drawn the way `ehrt.sim.run` draws
  it: off the `:person` family, one stream per id-tag."
  [id-tag]
  (sim-model/persona (streams/stream seed :person id-tag) {}))

(def ^:private pool
  {:population [{:person-id "p-a" :id-tag 1} {:person-id "p-b" :id-tag 2}]
   :personas {"p-a" (persona-for 1) "p-b" (persona-for 2)}
   :alive {}})

(def ^:private events
  "One stream carrying every shape the fold has to answer for: an
  `:at-t0` residence loss (a t0 CONDITION, not a change), all three
  `:demographic-update` causes, a `:coverage-change`, a person event
  with no demographic face at all, and a `:person-death` that must mint
  nothing."
  (let [pa (get-in pool [:personas "p-a"])]
    [{:event :residence-loss :person-id "p-b" :t 0 :event-id "p-b#0"
      :at-t0 true :prior-address addr-a}
     {:event :employment-change :person-id "p-a" :t 1800 :event-id "p-a#0"
      :employment {:status :employed :occupation-class :manual}}
     {:event :residence-move :person-id "p-a" :t 3600 :event-id "p-a#1"
      :address addr-b :prior-address (:address pa)}
     {:event :coverage-change :person-id "p-a" :t 7200 :event-id "p-a#2"
      :cause :employment
      :payer {:id "fixture-payer" :name "Fixture Health" :type :commercial}
      :prior-payer (:payer pa)}
     {:event :identity-correction :person-id "p-a" :t 10800 :event-id "p-a#3"
      :field :name :value {:family (get-in pa [:name :family]) :given "Corrected"}
      :prior-value (:name pa)}
     {:event :residence-loss :person-id "p-a" :t 14400 :event-id "p-a#4"
      :prior-address addr-b}
     {:event :person-death :person-id "p-b" :t 18000 :event-id "p-b#1"}]))

(def ^:private persons (assoc pool :events events))

(def ^:private brief-pathway
  {:name "brief" :steps [{:type :admission :location "Renal"} {:type :discharge}]})

(def ^:private facility
  {:id :persons-fixture
   :wards [{:id :renal :name "Renal" :beds 4 :surge-slots 2 :surge-format "%s-H%02d"
            :class :inpatient}]})

(defn- base [n] {:seed seed :patients n :arrival-gap 0 :pathway brief-pathway :facility facility})

(defn- run-with-persons [n] (run/run (assoc (base n) :persons persons)))

(defn- of-kind [ground-truth kind] (filterv #(= kind (:event %)) ground-truth))

;; --- 2(a): the config key -------------------------------------------------

(deftest persons-is-a-config-key-run-command-must-forward-test
  (testing "`:persons` joins `config-keys` in the same change that teaches
            `run` to read it -- that def's own docstring's law"
    (is (contains? (set config/config-keys) :persons))))

(deftest run-accepts-a-persons-payload-and-rejects-a-malformed-one-test
  (testing "a well-formed payload runs"
    (let [r (run-with-persons 3)]
      (is (seq (:ground-truth r)) "the accepting run produced no events at all")))
  (testing "and every malformed shape is result/error :invalid-persons, never a throw"
    (doseq [[label bad] [["not a map" :nope]
                         ["no population" (dissoc persons :population)]
                         ["no personas" (dissoc persons :personas)]
                         ["no alive map" (dissoc persons :alive)]
                         ["no events" (dissoc persons :events)]
                         ["a population entry with no id-tag"
                          (assoc persons :population [{:person-id "p-a"}])]
                         ["a persona that is not one"
                          (assoc persons :personas {"p-a" {:name "who?"}})]
                         ["the AUTHORED config shape, not the engine one"
                          {:count 4 :years 10}]]]
      (testing label
        (let [r (run/run (assoc (base 2) :persons bad))]
          (is (result/error? r) (str label " was accepted"))
          (is (= :invalid-persons (:category r)) label))))))

;; --- 2(a): ruling A1, selection -------------------------------------------

(deftest each-arrival-binds-a-living-person-by-one-world-draw-test
  (let [plan (run/person-plan (assoc (base 8) :persons persons))
        bindings (:bindings plan)]
    (testing "every arrival ordinal got a binding, and they are real persons"
      (is (= 8 (count bindings)))
      (is (pos? (count (remove nil? bindings)))
          "no arrival bound to anybody -- the selection never ran")
      (is (every? #{"p-a" "p-b"} (remove nil? bindings))))
    (testing "the pool is SHARED, so a person can be selected more than once"
      (is (pos? (- (count (remove nil? bindings)) (count (distinct (remove nil? bindings)))))
          "eight arrivals over a two-person pool selected nobody twice"))))

(deftest a-dead-person-is-not-in-the-arrival-candidate-set-test
  ;; ADR-0173 ruling C1's behavioural half: what the fold owes instead of
  ;; a wire event for a death. `:alive` is the filter's whole input, and
  ;; ALIVE means strictly after -- a person whose death instant is the
  ;; arrival instant is dead at it. Every arrival here is at t=0
  ;; (`:arrival-gap 0`), so a death at 0 excludes and a death at 1 does
  ;; not, and both directions are asserted.
  (let [with-death (fn [t] (run/person-plan
                            (assoc (base 8) :persons (assoc persons :alive {"p-b" t}))))
        dead (with-death 0)
        still-alive (with-death 1)]
    (testing "dead AT the arrival instant: never selected"
      (is (not-any? #{"p-b"} (:bindings dead))
          "an arrival bound to a person whose own death already fired")
      (is (pos? (count (filter #{"p-a"} (:bindings dead))))
          "the filter removed EVERYBODY -- this proves nothing about the filter"))
    (testing "dead one second LATER: still a candidate, so the filter is not
              simply refusing anyone who carries a death at all"
      (is (pos? (count (filter #{"p-b"} (:bindings still-alive))))))
    (testing "and the draw is still one per arrival either way"
      (is (= 8 (count (:bindings dead)) (count (:bindings still-alive)))))))

(deftest the-selection-draw-is-taken-whether-or-not-anyone-is-eligible-test
  ;; FIXED CONSUMPTION, the same law `:pathways`/`:module-assignment`
  ;; already state. An empty pool binds nobody and still takes its draw,
  ;; so the run's LATER `:world` draws (bed allocation) move -- which is
  ;; the only observable a fixed-consumption claim can have.
  (let [empty-pool {:population [] :personas {} :alive {} :events []}
        with-empty (run/run (assoc (base 6) :persons empty-pool))
        without (run/run (base 6))]
    (testing "both runs produced the same events for the same patients"
      (is (pos? (count (:ground-truth without))))
      (is (= (map :event (:ground-truth without)) (map :event (:ground-truth with-empty)))))
    (testing "but the bed the world chose moved, because a draw was consumed"
      (is (not= (map :location (of-kind (:ground-truth without) :admission))
                (map :location (of-kind (:ground-truth with-empty) :admission)))
          "an empty `:persons` pool consumed no `:world` draw -- consumption is
           not fixed, and a pool going empty would silently reshuffle nothing"))))

(deftest a-second-arrival-of-the-same-person-resolves-to-the-same-patient-test
  (let [plan (run/person-plan (assoc (base 8) :persons persons))
        index (:person-index plan)
        r (run-with-persons 8)
        registered (of-kind (:ground-truth r) :registered)
        patient-ids (map #(:patient-id (first (:participants %))) registered)]
    (testing "`:person-index` grew, person-id -> the patient they resolve to"
      (is (pos? (count index)) "the index is empty -- nothing was ever bound")
      (is (every? (fn [[_ e]] (and (string? (:patient-id e)) (int? (:first-ordinal e))
                                   (string? (:active-mrn e))))
                  index)))
    (testing "eight arrivals, but only as many patients as persons bound"
      (is (pos? (count registered)))
      (is (= (count (distinct patient-ids)) (count patient-ids))
          "a patient registered twice -- the repeat did not resolve")
      (is (< (count registered) 8)
          "eight arrivals minted eight patients: the repeats minted new ids")
      (is (= (set patient-ids) (set (map (comp :patient-id val) index)))))
    (testing "and every patient's FIRST event is still its `:registered`"
      (is (every? (fn [[_ evs]] (= :registered (:event (first evs))))
                  (group-by #(:patient-id (first (:participants %))) (:ground-truth r)))))))

;; --- 2(b): the fold -------------------------------------------------------

(deftest the-fold-is-queue-seeded-in-t-order-among-the-engines-own-events-test
  (let [r (run-with-persons 8)
        gt (:ground-truth r)
        folded (filterv #(#{:demographic-update :coverage-change} (:event %)) gt)]
    (testing "the fold produced events at all"
      (is (pos? (count folded)) "no person event reached the log"))
    (testing "the whole log is t-ascending -- the person events entered the
              SAME sorted queue, they were not appended"
      (is (apply <= (map :t gt))))
    (testing "and they interleave: engine events stand on both sides of them"
      (let [idx (map first (keep-indexed (fn [i e] (when (#{:demographic-update :coverage-change}
                                                          (:event e))
                                                     [i])) gt))]
        (is (pos? (count idx)))
        (is (some pos? idx) "every folded event landed at the head of the log")
        (is (every? #(< 0 %) idx)
            "a folded event landed before its patient's own `:registered`")))
    (testing "every folded event carries its person-side provenance stamp"
      (is (every? #(string? (:person-event-id %)) folded)))))

(deftest demographics-fold-onto-patient-state-and-leave-the-persona-alone-test
  (let [r (run-with-persons 8)
        gt (:ground-truth r)
        updates (of-kind gt :demographic-update)
        by-patient (group-by #(:patient-id (first (:participants %))) gt)]
    (is (pos? (count updates)))
    (doseq [[patient-id evs] by-patient
            :when (some #(= :demographic-update (:event %)) evs)]
      (let [states (get (:state-history r) patient-id)
            final (last states)
            last-residence (last (filter #(and (= :demographic-update (:event %))
                                               (= :residence (:field %)))
                                         evs))]
        (testing (str patient-id ": the fold wrote state-at-t")
          (is (some? (:demographics final)))
          (when last-residence
            (is (= (:value last-residence) (:residence (:demographics final)))
                "the last residence delta is not what the patient's state ended at")))
        (testing (str patient-id ": `:persona` is the t0 record and was NOT mutated")
          (is (= (:persona (first (filter #(= :registered (:event %)) evs)))
                 (:persona final))))))))

(deftest a-person-death-mints-nothing-test
  ;; ADR-0172 limitations row 4, confirmed by ADR-0173 ruling C1.
  (let [r (run-with-persons 8)
        gt (:ground-truth r)
        death (first (filter #(= :person-death (:event %)) events))]
    (testing "the stream really does carry a death (R-empty-population-is-red)"
      (is (some? death)))
    (is (empty? (filter #(= (:event-id death) (:person-event-id %)) gt))
        "a `:person-death` reached the log as something")
    (is (empty? (filter #(and (= :discharge (:event %)) (= :expired (:disposition %))) gt))
        "a `:person-death` became an expired discharge -- the GMF death is the
         only death with a wire face (ruling C1)")))

(deftest a-registration-bound-to-an-unhoused-person-carries-the-residence-sum-test
  ;; Ruling E1, on the ground-truth side. `p-b`'s `:at-t0` residence loss
  ;; lands before every arrival, so it is a t0 CONDITION folded into the
  ;; registration rather than an event queued after it.
  (let [gt (:ground-truth (run-with-persons 8))
        registered (of-kind gt :registered)
        unhoused (filter :residence registered)]
    (is (pos? (count registered)))
    (is (pos? (count unhoused))
        "no registration carried a residence sum -- the t0 condition was dropped")
    (doseq [ev unhoused]
      (is (= :unhoused (:status (:residence ev))))
      (is (= addr-a (:last-known-address (:residence ev))))
      (testing "and the Persona still carries an address, because Persona's own
                `:address` is required and non-nilable"
        (is (some? (:address (:persona ev))))))
    (testing "a registration for a HOUSED person carries no `:residence` at all --
              absent, not a `:housed` arm, so no existing event moves"
      (is (pos? (count (remove :residence registered)))))))

(deftest an-event-that-reports-no-change-is-not-an-event-test
  ;; The person event moves the address to the value the patient already
  ;; has, so the step is consumed and nothing is minted.
  (let [pa (get-in pool [:personas "p-a"])
        no-op [{:event :residence-move :person-id "p-a" :t 3600 :event-id "p-a#0"
                :address (:address pa) :prior-address (:address pa)}
               {:event :residence-move :person-id "p-a" :t 7200 :event-id "p-a#1"
                :address addr-b :prior-address (:address pa)}]
        gt (:ground-truth (run/run (assoc (base 4) :persons
                                             (assoc pool :events no-op
                                                    :population [{:person-id "p-a" :id-tag 1}]
                                                    :personas {"p-a" pa}))))
        updates (of-kind gt :demographic-update)]
    (testing "the real change minted one event"
      (is (= 1 (count updates)))
      (is (= addr-b (get-in (first updates) [:value :address]))))
    (testing "and every minted event's prior differs from its value"
      (is (every? #(not= (:value %) (:prior-value %)) updates)))))

(deftest the-prior-value-is-the-folded-state-not-the-person-events-own-claim-test
  ;; The person event lies about its prior; the wire reports the truth.
  (let [pa (get-in pool [:personas "p-a"])
        lying [{:event :residence-move :person-id "p-a" :t 3600 :event-id "p-a#0"
                :address addr-b
                :prior-address {:street "nowhere" :city "nowhere" :state "ZZ" :zip "00000"}}]
        gt (:ground-truth (run/run (assoc (base 4) :persons
                                             (assoc pool :events lying
                                                    :population [{:person-id "p-a" :id-tag 1}]
                                                    :personas {"p-a" pa}))))
        update (first (of-kind gt :demographic-update))]
    (is (some? update))
    (is (= {:status :housed :address (:address pa)} (:prior-value update))
        "the wire copied the person event's own prior instead of reading the fold")))

;; --- the evolve siblings, directly ----------------------------------------

(deftest evolve-writes-one-demographics-field-and-is-total-test
  (let [pa (get-in pool [:personas "p-a"])
        seeded (evolve/evolve (state/initial-patient "PID-x" "MRN1")
                              {:event :registered :t 0 :persona pa})]
    (testing "seeded from the Persona"
      (is (= {:status :housed :address (:address pa)} (:residence (:demographics seeded)))))
    (testing "a residence delta writes exactly `:residence`"
      (let [after (evolve/evolve seeded {:event :demographic-update :t 1 :field :residence
                                         :value {:status :unhoused}})]
        (is (= {:status :unhoused} (:residence (:demographics after))))
        (is (= (dissoc (:demographics seeded) :residence) (dissoc (:demographics after) :residence)))
        (is (= pa (:persona after)) "the t0 Persona was mutated")))
    (testing "a coverage change writes exactly `:payer`"
      (let [payer {:id "x" :name "X" :type :commercial}
            after (evolve/evolve seeded {:event :coverage-change :t 1 :payer payer})]
        (is (= payer (:payer (:demographics after))))
        (is (= (dissoc (:demographics seeded) :payer) (dissoc (:demographics after) :payer)))))
    (testing "and both are total over a patient that never registered -- a
              one-field demographic state would claim every OTHER field is
              unknown, which is worse than none"
      (let [bare (state/initial-patient "PID-y" "MRN2")]
        (is (= bare (evolve/evolve bare {:event :demographic-update :t 1
                                         :field :residence :value {:status :unhoused}})))
        (is (= bare (evolve/evolve bare {:event :coverage-change :t 1 :payer {}})))))))

;; --- ruling C1: the compiled death, keyed by person -----------------------

(def ^:private death-module
  "A hand-authored GMF module that reaches Death deterministically. The
  400-day lead-in is the fleet fixture's own load-bearing one: the walk
  starts at the patient's DOB and everything before the run's
  registration instant is history, so an encounter authored at t=0 is
  dropped for most sampled ages."
  {:id "persons-death-mod" :name "Persons Death Fixture"
   :states {:initial {:type :initial :direct-transition :lead-in}
            :lead-in {:type :delay :exact {:quantity 400 :unit "days"}
                      :direct-transition :the-visit}
            :the-visit {:type :encounter :encounter-class :emergency
                        :codes [{:system :snomed :code "50849002"
                                 :display "Emergency room admission (procedure)"}]
                        :direct-transition :the-end-of-it}
            :the-end-of-it {:type :death :exact {:quantity 1 :unit "days"}
                            :codes [{:system :snomed :code "410429000" :display "Cardiac arrest"}]
                            :direct-transition :done}
            :done {:type :terminal}}})

(def ^:private newborn-pool
  "The same two people, with NEWBORN Personas. The death module's
  400-day lead-in is measured from the patient's DOB, and everything
  before the run's registration instant is history -- so an adult
  Persona's whole walk is pre-horizon-dropped and no death compiles at
  all. This is the fleet fixture's own load-bearing note, met here from
  the person side rather than through `:persona-config`, because with
  `:persons` present the Persona comes from the PERSON and
  `:persona-config` no longer reaches it."
  {:population (:population pool)
   :personas {"p-a" (sim-model/persona (streams/stream seed :person 1) {:age-min 0 :age-max 0})
              "p-b" (sim-model/persona (streams/stream seed :person 2) {:age-min 0 :age-max 0})}
   :alive {}
   :events events})

(deftest person-plan-keys-the-compiled-death-by-person-test
  ;; ADR-0173 ruling C1's ordering resolution, and the part of it that
  ;; could have failed: the compiled death instant must be computable
  ;; UP FRONT, which it is only because `compile-trajectory` emits
  ;; bridging delays as `{:from g :to g}` and ADR-0171 section 2(d) made
  ;; `:from` = `:to` draw-free.
  (let [cfg (assoc (base 6)
                   :pathway {:name "module-only" :steps []}
                   :persona-config {:age-min 0 :age-max 0}
                   :modules [(patient-simulator/singleton-closure death-module)]
                   :module-assignment [{:module-id "persons-death-mod" :weight 1}]
                   :module-horizon-days 1200)
        plan (run/person-plan (assoc cfg :persons newborn-pool))
        deaths (:deaths plan)]
    (testing "the plan bound arrivals at all (R-empty-population-is-red)"
      (is (pos? (count (remove nil? (:bindings plan))))))
    (testing "and every bound person whose arrival compiles a death is keyed by PERSON"
      (is (pos? (count deaths))
          "no compiled death was found -- ruling C1's `:deaths` would always be empty")
      (is (every? #{"p-a" "p-b"} (keys deaths)))
      (is (every? int? (vals deaths))))
    (testing "a death instant is at or after the arrival that produced it, which is
              what makes the alive-filter's conservatism safe"
      (is (every? #(<= 0 %) (vals deaths))))
    (testing "and with no `:persons` at all the plan binds nobody and keys nothing"
      (let [bare (run/person-plan cfg)]
        (is (every? nil? (:bindings bare)))
        (is (empty? (:person-index bare)))
        (is (empty? (:deaths bare)))))))

;; --- arc 3a part 4: the two hooks and the identification flow -------------
;;
;; ADR-0173 sections 2(c) and 2(d). Same posture as part 3's fixtures
;; above: the person events are HAND-AUTHORED, because the engine's own
;; contract is that they arrive as DATA and because a fixture whose
;; coverage depends on a hazard firing is a fixture that can silently
;; stop covering. That the REAL stream reaches all of this is gated
;; separately, over the real component, in `ehrt.sim.persons-run-test`.

(def ^:private p4-seed 15)

(def ^:private p4-arrivals
  "This fixture's own arrival instants, PINNED by construction rather
  than assumed: seed 15, `:arrival-gap` 100, four patients. Every window
  below is placed against these, so a change to the `:world` family
  moves them and the placements go red instead of quietly missing."
  [0 4620 8160 9900])

(defn- p4-persona [id-tag] (sim-model/persona (streams/stream p4-seed :person id-tag) {}))

(def ^:private p4-pool
  "ONE person, so every arrival binds to them and no `:world` draw
  decides which arrival lands unidentified."
  {:population [{:person-id "q-a" :id-tag 1}]
   :personas {"q-a" (p4-persona 1)}
   :alive {}})

(def ^:private p4-facility
  "The part-3 fixture facility plus an ED ward, because `hook-ward`
  chooses by CLASS: an occupational injury and an unidentified arrival
  are ED presentations, a birth is an inpatient one. A facility with no
  ED at all falls back to its inpatient ward, which is a real fallback
  and is asserted separately below."
  {:id :persons-fixture-p4
   :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 8 :surge-format "%s-H%02d"
            :class :ed}
           {:id :renal :name "Renal" :beds 4 :surge-slots 2 :surge-format "%s-H%02d"
            :class :inpatient}]})

(def ^:private p4-newborn-persona
  (sim-model/persona (streams/stream p4-seed :person 4) {:age-min 0 :age-max 0}))

(defn- p4-window
  [n open-t until-t]
  {:event :identity-unavailable :person-id "q-a" :t open-t :event-id (str "q-a#" n)
   :until-t until-t :alias-name {:family "Doe" :given "Unknown"}})

(defn- p4-resolution
  [n branch unavailable-id t]
  (cond-> {:event :identity-resolution :person-id "q-a" :t t :event-id (str "q-a#" n)
           :branch branch :unavailable-event-id unavailable-id}
    (= :merge branch) (assoc :surviving-person-id "q-a")))

(def ^:private p4-delivery
  [{:event :delivery :person-id "q-a" :t 20000 :event-id "q-a#8"
    :newborn-person-id "q-a/b0" :parity-index 0 :within-delivery-index 0
    :pregnancy-event-id "q-a#7" :participants ["q-a" "q-a/b0"]}
   {:event :person-registered :person-id "q-a/b0" :t 20000 :event-id "q-a/b0#0"
    :persona p4-newborn-persona :delivery-event-id "q-a#8"
    :participants ["q-a/b0" "q-a"]}])

(defn- p4-run
  "A four-arrival run over the one-person pool, with an EMPTY pathway so
  every patient stays clinically idle and a hook has somewhere to land."
  ([events] (p4-run events {}))
  ([events extra]
   (run/run (merge {:seed p4-seed :patients 4 :arrival-gap 100
                       :pathway {:name "empty" :steps []}
                       :facility p4-facility
                       :persons (assoc p4-pool :events (vec events))}
                      extra))))

(defn- placeholder-registrations [gt]
  (filterv #(and (= :registered (:event %)) (= :placeholder (:identity %))) gt))

(defn- fills [gt]
  (filterv #(and (= :demographic-update (:event %)) (= :identity-fill (:cause %))) gt))

(defn- identification-merges [gt]
  (filterv #(and (= :merge (:event %)) (= :identification (:cause %))) gt))

(defn- hook-admissions [gt]
  (filterv #(and (= :admission (:event %)) (:person-event-id %)) gt))

(deftest the-fixtures-own-arrival-instants-are-what-the-windows-are-placed-against-test
  ;; R-empty-population-is-red's cousin: every placement below is a
  ;; number chosen against these instants, so this is the one assertion
  ;; that makes the rest of the file non-vacuous rather than lucky.
  (let [plan (run/person-plan {:seed p4-seed :patients 4 :arrival-gap 100})]
    (is (= 4 (count (:bindings plan))))
    (is (= p4-arrivals
           (mapv :t (filter #(= :registered (:event %))
                            (:ground-truth (run/run {:seed p4-seed :patients 4 :arrival-gap 100
                                                        :pathway {:name "empty" :steps []}
                                                        :facility p4-facility})))))
        "this fixture's pinned arrival instants moved -- every window placement below
         is now measuring something else")))

;; --- 2(d): the placeholder registration ----------------------------------

(deftest an-identity-window-mints-an-unidentified-ed-arrival-test
  ;; ADR-0173 section 2(d), met at the rate the process actually
  ;; produces. `person-fold/hook-kinds` carries the measurement that
  ;; forced this reading; here is the behaviour it buys.
  (let [gt (:ground-truth (p4-run [(p4-window 0 100000 200000)
                                   (p4-resolution 1 :fill "q-a#0" 200000)]))
        ph (placeholder-registrations gt)]
    (is (= 1 (count ph)) "the window minted no unidentified arrival")
    (let [ev (first ph)
          pid (:patient-id (first (:participants ev)))]
      (testing "it is an ADDITIONAL patient, at ordinal (+ patients k)"
        (is (= (streams/patient-id-for p4-seed 4) pid)
            "the placeholder was not minted at ordinal (+ patients 0)")
        (is (= "MRN000005" (:active-mrn ev))
            "the MRN is not a function of the same ordinal"))
      (testing "and it registers as a John Doe, with the window's own close instant"
        (is (= {:family "Doe" :given "Unknown"} (:alias-name ev)))
        (is (= {:status :unknown} (:residence ev)))
        (is (= 200000 (:window-close-t ev)))
        (is (= "q-a" (:person-id ev))))
      (testing "ground truth still knows WHO they are -- only the wire does not"
        (is (= (get-in p4-pool [:personas "q-a"]) (:persona ev))))
      (testing "and the presentation is an ED encounter, stamped with its own person event"
        (let [admit (first (filter #(= pid (:patient-id (first (:participants %))))
                                   (hook-admissions gt)))]
          (is (some? admit) "the unidentified arrival never presented anywhere")
          (is (= "Unidentified patient" (:reason admit)))
          (is (= "q-a#0" (:person-event-id admit)))
          (is (= "Emergency" (:home-ward admit))))))))

(deftest a-placeholder-folds-to-the-alias-and-nothing-else-test
  (let [r (p4-run [(p4-window 0 100000 200000)])
        gt (:ground-truth r)
        ev (first (placeholder-registrations gt))
        pid (:patient-id (first (:participants ev)))
        state (first (get (:state-history r) pid))]
    (is (some? state))
    (is (= {:name {:family "Doe" :given "Unknown"}
            :residence {:status :unknown}
            :identity :placeholder}
           (:demographics state))
        "a placeholder's demographic state carries a fact the hospital does not have")))

(deftest an-arrival-coinciding-with-an-open-window-is-a-placeholder-too-test
  ;; ADR-0173 section 2(d) AS WRITTEN. Unreachable by luck at the
  ;; process's own rates -- which is why the window is also a hook --
  ;; but implemented, and this is where it is proved.
  (let [gt (:ground-truth (p4-run [(p4-window 0 1000 6000)
                                   (p4-resolution 1 :fill "q-a#0" 6000)]))
        ph (placeholder-registrations gt)
        coincident (filter #(= 4620 (:t %)) ph)]
    (is (= 1 (count coincident))
        "arrival 1 (t 4620) is inside the window (1000, 6000) and did not register
         as a placeholder")
    (is (= (streams/patient-id-for p4-seed 1)
           (:patient-id (first (:participants (first coincident)))))
        "a coincident placeholder minted a NEW id space instead of using its own ordinal")
    (testing "and the identified arrival before it still minted the person's real patient"
      (is (= 1 (count (filter #(and (= :registered (:event %)) (nil? (:identity %))
                                    (= 0 (:t %)))
                              gt)))))))

;; --- 2(d): the two resolutions -------------------------------------------

(deftest an-identity-fill-keeps-the-mrn-and-references-its-placeholder-test
  (let [gt (:ground-truth (p4-run [(p4-window 0 100000 200000)
                                   (p4-resolution 1 :fill "q-a#0" 200000)]))
        ph (first (placeholder-registrations gt))
        pid (:patient-id (first (:participants ph)))
        fill (first (fills gt))]
    (is (some? fill) "the window resolved `:fill` and nothing was minted")
    (is (= pid (:patient-id (first (:participants fill))))
        "the fill landed on somebody other than the placeholder")
    (is (= (:active-mrn ph) (:active-mrn fill)) "the fill changed the MRN")
    (is (= 200000 (:t fill)) "the fill did not land at the window's close")
    (testing "it references the placeholder registration by LOG INDEX"
      (is (= ph (nth gt (:placeholder-event-id fill)))))
    (testing "and it reports the one fact it is: an identity became known"
      (is (= [:identity :known :placeholder]
             [(:field fill) (:value fill) (:prior-value fill)]))
      (is (= (get-in p4-pool [:personas "q-a"]) (:persona fill))))))

(deftest an-identification-merge-is-churns-merge-shape-with-a-cause-test
  (let [gt (:ground-truth (p4-run [(p4-window 0 100000 200000)
                                   (p4-resolution 1 :merge "q-a#0" 200000)]))
        ph (first (placeholder-registrations gt))
        placeholder-id (:patient-id (first (:participants ph)))
        survivor-id (streams/patient-id-for p4-seed 0)
        m (first (identification-merges gt))]
    (is (some? m) "the window resolved `:merge` and nothing was minted")
    (testing "the SURVIVOR is the person's prior patient and the MERGED is the placeholder"
      (is (= [{:patient-id survivor-id :role :survivor}
              {:patient-id placeholder-id :role :merged}]
             (:participants m))))
    (testing "and every field churn's own merge carries is here, with nothing missing"
      (is (= #{:event :t :cause :person-event-id :participants
               :surviving-mrn :merged-mrn :merged-mrns :warm-up}
             (set (keys m))))
      (is (= "MRN000001" (:surviving-mrn m)))
      (is (= (:active-mrn ph) (:merged-mrn m)))
      (is (contains? (:merged-mrns m) (:active-mrn ph))))
    (testing "no fill was minted as well -- one resolution, not two"
      (is (empty? (fills gt))))
    (testing "and churn's own lottery gained nothing: this run has no churn profile
              at all, so the merge could only have come from the identification step"
      (is (= 1 (count (filter #(= :merge (:event %)) gt)))))))

(deftest a-merge-with-no-survivor-degenerates-to-a-fill-test
  ;; ADR-0173 section 2(d) names this explicitly, because silently
  ;; emitting a merge with a null survivor is the defect the sentence
  ;; exists to prevent. The window here opens at t 0 and closes after
  ;; every arrival, so EVERY arrival of this one-person pool is a
  ;; placeholder and the person never acquires an identified patient at
  ;; all -- there is nothing for a merge to survive into.
  (let [gt (:ground-truth (p4-run [(p4-window 0 0 200000)
                                   (p4-resolution 1 :merge "q-a#0" 200000)]))
        ph (placeholder-registrations gt)]
    (is (pos? (count ph)) "no placeholder was minted, so this proves nothing")
    (is (= (count ph) (count (filter #(= :registered (:event %)) gt)))
        "some arrival registered as an IDENTIFIED patient, so a survivor exists
         after all and this is no longer the degenerate case")
    (is (empty? (identification-merges gt))
        "a merge was emitted with no prior patient to survive it")
    (is (pos? (count (fills gt)))
        "the merge did not degenerate to a fill -- the placeholder is left dangling")
    (testing "and the fills land on the placeholders themselves"
      (is (= (set (map #(:patient-id (first (:participants %))) ph))
             (set (map #(:patient-id (first (:participants %))) (fills gt)))))))
  (testing "the control: with an identified arrival ahead of the window, the SAME
            stream produces a merge instead"
    (let [gt (:ground-truth (p4-run [(p4-window 0 1000 6000)
                                     (p4-resolution 1 :merge "q-a#0" 6000)]))]
      (is (pos? (count (identification-merges gt))))
      (is (empty? (fills gt))))))

(deftest a-window-with-no-resolution-leaves-its-placeholder-unjudgeable-test
  ;; `every-placeholder-registration-is-resolved-or-still-open` has TWO
  ;; escapes, and this is the one the engine reaches for whenever a
  ;; window never resolves in the stream it was handed -- the person
  ;; died inside it, or their own horizon ended inside it. The
  ;; placeholder is minted, nothing resolves it, and it carries NO
  ;; `:window-close-t`, which is that invariant's own "a placeholder
  ;; carrying none cannot be judged either way, so it is left alone".
  ;;
  ;; The OTHER escape -- a close instant that is genuinely still in the
  ;; future when the feed stops -- is a hand-authored-log case now, and
  ;; is exercised by the mutation gates in
  ;; `ehrt.sim-check.person-invariants-test`. It is nearly unreachable
  ;; from a real run by construction: a resolution is QUEUED at its own
  ;; `:until-t`, so a run whose window resolves has already reached it.
  (let [gt (:ground-truth (p4-run [(p4-window 0 100000 900000)]))
        ph (placeholder-registrations gt)]
    (is (= 1 (count ph)))
    (is (empty? (fills gt)))
    (is (empty? (identification-merges gt)))
    (is (nil? (:window-close-t (first ph)))
        "an unresolved window promised a close instant it cannot keep")
    (is (< (:t (last gt)) 900000)
        "the run outlived the window, so this fixture is no longer the
         unresolved case at all")))

;; --- 2(c): the delivery hook ---------------------------------------------

(deftest a-delivery-mints-the-newborn-as-an-additional-patient-test
  (let [gt (:ground-truth (p4-run p4-delivery))
        newborn (first (filter :mother-patient-id gt))]
    (is (some? newborn) "no newborn registered")
    (testing "ordinal (+ patients k), so id and MRN stay functions of an ordinal"
      (is (= (streams/patient-id-for p4-seed 4)
             (:patient-id (first (:participants newborn)))))
      (is (= "MRN000005" (:active-mrn newborn))))
    (testing "the mother-baby link names the parent's own patient"
      (is (= (streams/patient-id-for p4-seed 0) (:mother-patient-id newborn)))
      (is (some? (first (filter #(and (= :registered (:event %))
                                      (= (:mother-patient-id newborn)
                                         (:patient-id (first (:participants %)))))
                                gt)))
          "the link names a patient with no registration in this log"))
    (testing "and it is a LINK, not a participant -- a birth does not re-register
              the mother"
      (is (= 1 (count (:participants newborn)))))
    (testing "the newborn's first encounter IS the birth"
      (let [nb-id (:patient-id (first (:participants newborn)))
            own (filterv #(= nb-id (:patient-id (first (:participants %)))) gt)]
        (is (= [:registered :admission :discharge] (mapv :event own)))
        (is (= "Live birth" (:reason (second own))))
        (is (= "q-a#8" (:person-event-id (second own))))))))

(deftest a-delivery-admits-the-parent-when-they-are-clinically-idle-test
  (let [gt (:ground-truth (p4-run p4-delivery))
        parent-id (streams/patient-id-for p4-seed 0)
        parent (filterv #(= parent-id (:patient-id (first (:participants %)))) gt)]
    (is (= [:registered :admission :discharge] (mapv :event parent)))
    (is (= "Delivery" (:reason (second parent))))
    (is (= 20000 (:t (second parent))) "the admission is not at the delivery instant"))
  (testing "and NOT when their own queue already carries an encounter -- the
            single-encounter horizon (`admission-only-when-new`), answered
            statically because a decide cannot see a later admission coming"
    (let [gt (:ground-truth (p4-run p4-delivery
                                    {:pathway {:name "brief"
                                               :steps [{:type :admission :location "Renal"}
                                                       {:type :delay :from 600 :to 600}
                                                       {:type :discharge}]}}))
          parent-id (streams/patient-id-for p4-seed 0)
          parent (filterv #(= parent-id (:patient-id (first (:participants %)))) gt)]
      (is (= [:registered :admission :discharge] (mapv :event parent))
          "the parent got a second encounter, which no log may carry")
      (is (nil? (:person-event-id (second parent)))
          "the admission the parent got is the delivery's, not their own pathway's")
      (testing "the NEWBORN's own encounter is untouched by that refusal"
        (is (some? (first (filter :mother-patient-id gt))))))))

;; --- 2(c): the occupational-injury hook ----------------------------------

(deftest an-occupational-injury-presents-at-the-ed-test
  (let [gt (:ground-truth (p4-run [{:event :occupational-injury :person-id "q-a" :t 30000
                                    :event-id "q-a#9" :injury-class :laceration}]))
        pid (streams/patient-id-for p4-seed 0)
        own (filterv #(= pid (:patient-id (first (:participants %)))) gt)]
    (is (= [:registered :admission :discharge] (mapv :event own)))
    (is (= "Occupational injury: laceration" (:reason (second own))))
    (is (= "Emergency" (:home-ward (second own))) "an injury did not present at the ED")
    (is (= "q-a#9" (:person-event-id (second own))))))

(deftest an-injury-to-a-person-no-arrival-bound-mints-their-first-patient-test
  ;; ADR-0173 section 2(c): "It selects the injured person's own patient,
  ;; or mints one if this is their first contact."
  (let [two-person (assoc p4-pool
                          :population [{:person-id "q-a" :id-tag 1} {:person-id "q-b" :id-tag 2}]
                          :personas {"q-a" (p4-persona 1) "q-b" (p4-persona 2)}
                          ;; q-b is alive but never selectable: every arrival is
                          ;; at or after t 0 and this death fires at 0.
                          :alive {"q-b" 0})
        gt (:ground-truth (run/run
                           {:seed p4-seed :patients 4 :arrival-gap 100
                            :pathway {:name "empty" :steps []}
                            :facility p4-facility
                            :persons (assoc two-person :events
                                            [{:event :occupational-injury :person-id "q-b" :t 30000
                                              :event-id "q-b#0" :injury-class :fracture}])}))]
    (testing "a person the ground truth says is DEAD mints nothing -- the same alive
              filter ruling A1 puts on arrival selection"
      (is (empty? (hook-admissions gt))
          "an injury minted a patient for somebody already dead")))
  (let [two-person (assoc p4-pool
                          :population [{:person-id "q-a" :id-tag 1} {:person-id "q-b" :id-tag 2}]
                          :personas {"q-a" (p4-persona 1) "q-b" (p4-persona 2)}
                          :alive {"q-b" 20000})
        gt (:ground-truth (run/run
                           {:seed p4-seed :patients 1 :arrival-gap 100
                            :pathway {:name "empty" :steps []}
                            :facility p4-facility
                            :persons (assoc two-person :events
                                            [{:event :occupational-injury :person-id "q-b" :t 10000
                                              :event-id "q-b#0" :injury-class :fracture}])}))
        minted (first (hook-admissions gt))]
    (is (some? minted) "an unbound person's injury minted no patient at all")
    (is (= (streams/patient-id-for p4-seed 1)
           (:patient-id (first (:participants minted))))
        "the minted patient did not take ordinal (+ patients 0)")
    (is (= "Occupational injury: fracture" (:reason minted)))))

;; --- fixed consumption: a hook costs no existing patient a draw ----------

(deftest hook-patients-consume-no-patient-family-draw-test
  ;; The claim, and the only observable it can have: adding hook events
  ;; to a stream leaves every event of every patient that would have
  ;; existed anyway BYTE-IDENTICAL. A hook patient's Persona comes from
  ;; the person side and it walks no module, and its `:from` = `:to`
  ;; delay is draw-free (ADR-0171 section 2(d)), so it reads its own
  ;; stream zero times.
  (let [without (:ground-truth (p4-run []))
        with (:ground-truth (p4-run [{:event :occupational-injury :person-id "q-a" :t 30000
                                      :event-id "q-a#9" :injury-class :strain}]))
        existing (set (map #(:patient-id (first (:participants %))) without))
        with-existing (filterv #(existing (:patient-id (first (:participants %)))) with)]
    (is (pos? (count without)) "the control run produced nothing")
    (is (pos? (count (hook-admissions with))) "the hook run produced no hook at all")
    (is (= without (take (count without) with-existing))
        "a hook moved an existing patient's own events")
    (is (= (pr-str without) (pr-str (vec (take (count without) with-existing))))
        "a hook moved an existing patient's own BYTES")))

;; --- ADR-0173's first tabled deviation, COUNTED -------------------------

(deftest repeat-arrivals-resolve-and-queue-nothing-without-the-encounters-opt-in-test
  ;; ADR-0173's own first tabled deviation, made VISIBLE rather than
  ;; left silent. A second `:admission` for a patient whose status is
  ;; `:discharged` violated `admission-only-when-new`, which was this
  ;; project's single-encounter horizon (sim/ADR-0007 point 3) expressed
  ;; as an invariant, so a repeat arrival queued NOTHING. What the
  ;; repeat is FOR survived even then: the person resolves to the
  ;; patient they already are.
  ;;
  ;; RENAMED 2026-08-26 by arc 3b sweep 1 (ADR-0174 ruling A1), which
  ;; LIFTS that horizon -- and this row keeps gating, unchanged in
  ;; substance, the path where the lift was not opted into. It is the
  ;; ABSENT half of the opt-in law, and `ehrt.sim-engine.encounters-
  ;; test/a-repeat-arrival-with-no-open-encounter-opens-a-second-one` is
  ;; the PRESENT half over this same fixture: same four arrivals, same
  ;; one person, three encounters instead of one.
  (let [r (p4-run [] {:pathway {:name "brief"
                                :steps [{:type :admission :location "Renal"}
                                        {:type :delay :from 30 :to 30}
                                        {:type :discharge}]}})
        gt (:ground-truth r)
        registered (filterv #(= :registered (:event %)) gt)]
    (testing "four arrivals over a ONE-person pool: three of them are repeats"
      (is (= 4 (count p4-arrivals)))
      (is (= 1 (count registered))
          (str "the repeat arrivals did not resolve -- " (count registered)
               " patients registered for one person")))
    (testing "and they queued nothing: the run carries exactly ONE encounter"
      (is (= 1 (count (filter #(= :admission (:event %)) gt))))
      (is (= 1 (count (filter #(= :discharge (:event %)) gt)))))
    (testing "the fold index resolves the person to that one patient"
      (let [index (:person-index (run/person-plan
                                  {:seed p4-seed :patients 4 :arrival-gap 100
                                   :persons (assoc p4-pool :events [])}))]
        (is (= 1 (count index)))
        (is (= (streams/patient-id-for p4-seed 0) (:patient-id (get index "q-a"))))
        (is (= 0 (:first-ordinal (get index "q-a"))))))))

;; --- the two population-scale defects, gated as units --------------------
;;
;; Both were found by a corpus probe during arc 3a part 4's own opt-in --
;; `clinic-decade` seed 5 over an 800-person pool exited
;; `:self-check-failed` -- and neither was reachable from any fixture in
;; this file until it was written for them. That is the shape
;; `rulings.md#R-empty-population-is-red` is about, one level up: a gate
;; over a case the fixtures cannot produce is a gate that proves nothing.

(deftest a-window-nobody-lives-to-close-mints-no-due-instant-test
  ;; ADR-0173 section 2(d), corrected by the tree. The person opens an
  ;; identity window and DIES inside it, so the process emits no
  ;; `:identity-resolution` -- correctly, since they did not live to see
  ;; one. The placeholder is then unresolvable forever, and a
  ;; `:window-close-t` promising a resolution that can never come would
  ;; make `every-placeholder-registration-is-resolved-or-still-open` fire
  ;; on the most characteristic John Doe outcome there is.
  (let [gt (:ground-truth (p4-run [(p4-window 0 1000 900000)
                                   {:event :person-death :person-id "q-a" :t 500000
                                    :event-id "q-a#1"}]
                                  {:persons (assoc p4-pool
                                                   :alive {"q-a" 500000}
                                                   :events [(p4-window 0 1000 900000)])}))
        ph (placeholder-registrations gt)]
    (is (pos? (count ph)) "the window minted no placeholder, so this proves nothing")
    (is (every? #(nil? (:window-close-t %)) ph)
        "a window with no resolution still promised a close instant")
    (is (every? #(= {:family "Doe" :given "Unknown"} (:alias-name %)) ph)
        "the alias is still minted -- only the DUE instant is withheld")
    (testing "and a window that DOES resolve still carries its close, so nothing
              is weakened: the engine withholds the promise it cannot keep and
              keeps the one it can"
      (let [resolved (:ground-truth (p4-run [(p4-window 0 1000 6000)
                                             (p4-resolution 1 :fill "q-a#0" 6000)]))]
        (is (every? #(= 6000 (:window-close-t %))
                    (placeholder-registrations resolved)))))))

(deftest a-merge-the-world-refuses-degenerates-to-a-fill-test
  ;; The SECOND half of the same finding. The resolution is queued on the
  ;; PLACEHOLDER rather than on the survivor, because the run loop
  ;; short-circuits a queue entry whose patient is already `:merged` --
  ;; so a step queued on the survivor would vanish the moment anything
  ;; merged that survivor away, leaving the placeholder dangling past its
  ;; own due close. Here the survivor EXPIRES instead, which the merge
  ;; guard refuses for the same reason and by the same branch.
  (let [expiring {:name "expiring"
                  :steps [{:type :admission :location "Renal"}
                          {:type :delay :from 10 :to 10}
                          {:type :discharge :disposition :expired}]}
        gt (:ground-truth (p4-run [(p4-window 0 1000 6000)
                                   (p4-resolution 1 :merge "q-a#0" 6000)]
                                  {:pathways [{:patient-ordinal 0 :pathway expiring}
                                              {:patient-ordinal 1 :pathway {:name "empty" :steps []}}
                                              {:patient-ordinal 2 :pathway {:name "empty" :steps []}}
                                              {:patient-ordinal 3 :pathway {:name "empty" :steps []}}]}))
        survivor-id (streams/patient-id-for p4-seed 0)]
    (testing "the survivor really did expire before the window closed"
      (let [death (first (filter #(and (= :discharge (:event %)) (= :expired (:disposition %))) gt))]
        (is (some? death) "the fixture's expiring pathway produced no expired discharge")
        (is (= survivor-id (:patient-id (first (:participants death)))))
        (is (< (:t death) 6000) "the expiry lands after the window closes, so the
                                 merge would have been legal and this proves nothing")))
    (is (empty? (identification-merges gt))
        "a merge was emitted naming an expired survivor")
    (is (pos? (count (fills gt)))
        "the refused merge minted NOTHING -- the placeholder is left dangling past
         its own due close, which is exactly the violation this fallback exists
         to prevent")
    (testing "and the fill landed on the placeholder, not on the survivor"
      (is (every? #(not= survivor-id (:patient-id (first (:participants %)))) (fills gt))))))
