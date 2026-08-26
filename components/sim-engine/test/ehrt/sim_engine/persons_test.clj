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
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-model.interface :as sim-model]))

;; --- the fixture population ----------------------------------------------

(def ^:private seed 15)

(def ^:private addr-a {:street "1 Fixture Way" :city "Springfield" :state "IL" :zip "62701"})
(def ^:private addr-b {:street "2 Fixture Way" :city "Shelbyville" :state "IL" :zip "62565"})

(defn- persona-for
  "The t0 Persona of person `id-tag`, drawn the way `ehrt.sim.run` draws
  it: off the `:person` family, one stream per id-tag."
  [id-tag]
  (sim-model/persona (engine/stream seed :person id-tag) {}))

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

(defn- run-with-persons [n] (engine/run (assoc (base n) :persons persons)))

(defn- of-kind [ground-truth kind] (filterv #(= kind (:event %)) ground-truth))

;; --- 2(a): the config key -------------------------------------------------

(deftest persons-is-a-config-key-run-command-must-forward-test
  (testing "`:persons` joins `config-keys` in the same change that teaches
            `run` to read it -- that def's own docstring's law"
    (is (contains? (set engine/config-keys) :persons))))

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
        (let [r (engine/run (assoc (base 2) :persons bad))]
          (is (result/error? r) (str label " was accepted"))
          (is (= :invalid-persons (:category r)) label))))))

;; --- 2(a): ruling A1, selection -------------------------------------------

(deftest each-arrival-binds-a-living-person-by-one-world-draw-test
  (let [plan (engine/person-plan (assoc (base 8) :persons persons))
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
  (let [with-death (fn [t] (engine/person-plan
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
        with-empty (engine/run (assoc (base 6) :persons empty-pool))
        without (engine/run (base 6))]
    (testing "both runs produced the same events for the same patients"
      (is (pos? (count (:ground-truth without))))
      (is (= (map :event (:ground-truth without)) (map :event (:ground-truth with-empty)))))
    (testing "but the bed the world chose moved, because a draw was consumed"
      (is (not= (map :location (of-kind (:ground-truth without) :admission))
                (map :location (of-kind (:ground-truth with-empty) :admission)))
          "an empty `:persons` pool consumed no `:world` draw -- consumption is
           not fixed, and a pool going empty would silently reshuffle nothing"))))

(deftest a-second-arrival-of-the-same-person-resolves-to-the-same-patient-test
  (let [plan (engine/person-plan (assoc (base 8) :persons persons))
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
        gt (:ground-truth (engine/run (assoc (base 4) :persons
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
        gt (:ground-truth (engine/run (assoc (base 4) :persons
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
        seeded (engine/evolve (engine/initial-patient "PID-x" "MRN1")
                              {:event :registered :t 0 :persona pa})]
    (testing "seeded from the Persona"
      (is (= {:status :housed :address (:address pa)} (:residence (:demographics seeded)))))
    (testing "a residence delta writes exactly `:residence`"
      (let [after (engine/evolve seeded {:event :demographic-update :t 1 :field :residence
                                         :value {:status :unhoused}})]
        (is (= {:status :unhoused} (:residence (:demographics after))))
        (is (= (dissoc (:demographics seeded) :residence) (dissoc (:demographics after) :residence)))
        (is (= pa (:persona after)) "the t0 Persona was mutated")))
    (testing "a coverage change writes exactly `:payer`"
      (let [payer {:id "x" :name "X" :type :commercial}
            after (engine/evolve seeded {:event :coverage-change :t 1 :payer payer})]
        (is (= payer (:payer (:demographics after))))
        (is (= (dissoc (:demographics seeded) :payer) (dissoc (:demographics after) :payer)))))
    (testing "and both are total over a patient that never registered -- a
              one-field demographic state would claim every OTHER field is
              unknown, which is worse than none"
      (let [bare (engine/initial-patient "PID-y" "MRN2")]
        (is (= bare (engine/evolve bare {:event :demographic-update :t 1
                                         :field :residence :value {:status :unhoused}})))
        (is (= bare (engine/evolve bare {:event :coverage-change :t 1 :payer {}})))))))

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
   :personas {"p-a" (sim-model/persona (engine/stream seed :person 1) {:age-min 0 :age-max 0})
              "p-b" (sim-model/persona (engine/stream seed :person 2) {:age-min 0 :age-max 0})}
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
        plan (engine/person-plan (assoc cfg :persons newborn-pool))
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
      (let [bare (engine/person-plan cfg)]
        (is (every? nil? (:bindings bare)))
        (is (empty? (:person-index bare)))
        (is (empty? (:deaths bare)))))))
