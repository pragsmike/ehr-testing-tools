(ns ehrt.sim.persons-run-test
  "ADR-0173 section 2(a)/(f), arc 3a part 3: `ehrt.sim.run`'s half of the
  demographic fold.

  THIS IS THE GATE OVER THE REAL COMPONENT. `ehrt.sim-engine.persons-test`
  exercises the fold over a hand-authored person stream, deliberately, so
  that one small deterministic run can carry every shape the engine has
  to answer for. What it cannot show is that the stream the person
  process actually draws composes with this engine at all -- that the two
  vocabularies line up, that a real walk's events land on real patients,
  and that a run built this way survives the whole invariant catalog.
  That is what this namespace is for, and it calls
  `ehrt.person-simulator.interface/persons` for real, through
  `run-command`, exactly as a caller would.

  It is also the gate on ADR-0172 ruling F1 being LIFTED: `ehrt.sim.run`
  is the caller the charter said would arrive in arc 3, and it is not a
  `sim-engine` namespace, so limitations row 10's one-way edge is
  untouched by it."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-check.interface :as check]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim.run :as run]))

;; --- the witness config ---------------------------------------------------

(def ^:private payers-under-65
  [{:id "commercial-ppo" :name "Commercial PPO" :type :commercial :weight 8.0}
   {:id "medicaid" :name "Medicaid" :type :medicaid :weight 5.0}
   {:id "self-pay" :name "Self-Pay" :type :self-pay :weight 2.0}])

(def ^:private payers-65-plus
  [{:id "medicare-65" :name "Medicare" :type :medicare :weight 9.0}
   {:id "medicare-adv" :name "Medicare Advantage" :type :medicare :weight 3.0}])

(def ^:private witness-config
  "Twenty-six arrivals over a fourteen-person pool walking thirty years.

  Every number here is load-bearing and none of it is decoration. The
  pool is SMALLER than the arrival count so repeats are certain by
  pigeonhole, which is what makes the fold index observable at all.
  Thirty years is what it takes for a residence move and a coverage
  change to fire at the rates the process draws -- the counted witnesses
  below are the proof, and they are asserted `pos?` rather than pinned,
  because a pinned count would turn every future hazard retune into a
  false red here while a `pos?` catches the failure that matters
  (`rulings.md#R-empty-population-is-red`).

  `:t0-fraction 0.60` is NOT the process's own 0.02 default, and the
  reason is the one arc 3a part 1 already recorded for the person
  component's own witness: at fourteen people the default draws 0.28
  unhoused persons in expectation, so a witness over it proves the t0
  path by going empty. The default is asserted separately, where it
  lives.

  The payer pools ride `:persona-config`, which is where a run's ONE
  pool set lives: patients and persons draw coverage from the same
  pools, and letting them diverge would produce a corpus whose
  registrations and whose coverage changes disagree about which payers
  exist."
  {:seed 42 :patients 26 :arrival-gap 60
   :persona-config {:payers-under-65 payers-under-65 :payers-65-plus payers-65-plus}
   :persons {:count 14 :years 30 :unhoused {:t0-fraction 0.60}}})

(defn- ok-payload [r]
  (is (result/ok? r) (str "run-command was not :ok -- " (:category r) " " (pr-str (:payload r))))
  (:payload r))

(defn- of-kind [gt kind] (filterv #(= kind (:event %)) gt))

;; --- the plumbing: called iff `:persons` is present ------------------------

(deftest run-command-builds-the-person-stream-only-when-persons-is-present-test
  (let [captured (atom nil)
        stub (fn [engine-opts] (reset! captured engine-opts)
               {:ground-truth [] :facility nil :providers nil})]
    (testing "absent entirely -- the byte-identical path -- nothing is built and
              `:persons` never reaches the engine at all"
      (run/run-command {:seed 1 :patients 3} {:engine-run-fn stub})
      (is (not (contains? @captured :persons))))
    (testing "present, the engine receives the TRANSLATED payload, never the
              authored map -- the same two-layer key `:modules` already is"
      (run/run-command (assoc witness-config :patients 6) {:engine-run-fn stub})
      (let [persons (:persons @captured)]
        (is (some? persons) "`:persons` did not reach the engine")
        (is (engine/valid-persons? persons)
            "what reached the engine is not a well-formed engine-facing payload")
        (is (= 14 (count (:population persons))))
        (is (= 14 (count (:personas persons))))
        (testing "the events are the person process's own, t-ascending"
          (is (pos? (count (:events persons))) "the person walk produced no events")
          (is (apply <= (map :t (:events persons))))
          (is (every? :person-id (:events persons)))
          (is (every? :event-id (:events persons))))))))

(deftest run-command-hands-the-engine-compiled-deaths-keyed-by-person-test
  ;; ADR-0173 ruling C1. `:deaths` is empty for a config whose patients
  ;; compile no expiring discharge -- which is every config without a
  ;; death-bearing GMF module -- so what is asserted here is the WIRING:
  ;; the stream handed to the engine is pass TWO, built after
  ;; `engine/person-plan` answered the binding question, and it differs
  ;; from pass one exactly when a compiled death exists to differ over.
  ;; `ehrt.sim-engine.persons-test/person-plan-keys-the-compiled-death-by-
  ;; person-test` is the counted witness for a non-empty `:deaths`.
  (let [captured (atom nil)
        stub (fn [engine-opts] (reset! captured engine-opts)
               {:ground-truth [] :facility nil :providers nil})]
    (run/run-command (assoc witness-config :patients 6) {:engine-run-fn stub})
    (let [persons (:persons @captured)
          plan (engine/person-plan (assoc @captured :persons persons))]
      (testing "the payload the engine got is one `engine/person-plan` can read"
        (is (pos? (count (remove nil? (:bindings plan))))
            "no arrival bound to anybody -- the plan cannot have keyed anything")
        (is (map? (:deaths plan))))
      (testing "`:alive` is pass ONE's own drawn deaths, which is the filter's
                input and the reason the two passes do not chase each other"
        (is (= (:alive persons) (engine/person-deaths
                                 (:events (run/engine-persons
                                           (assoc @captured :persons
                                                  (:persons witness-config)))))
               )
            "the alive map is not reproducible from a fresh translation")))))

(deftest run-command-rejects-a-malformed-persons-config-test
  (doseq [[label bad] [["not a map" 12]
                       ["no count" {:years 3}]
                       ["a zero count" {:count 0}]
                       ["a negative count" {:count -1}]
                       ["a zero year horizon" {:count 4 :years 0}]]]
    (testing label
      (let [r (run/run-command (assoc witness-config :persons bad))]
        (is (result/error? r) (str label " was accepted"))
        (is (= :invalid-persons (:category r)) label)))))

;; --- 2(f): provenance -----------------------------------------------------

(deftest the-manifest-stamps-persons-and-persona-config-iff-present-test
  (testing "present: both keys reach the artifact's own face"
    (let [params (:engine-params (:manifest (ok-payload (run/run-command witness-config))))]
      (is (= (:persons witness-config) (:persons params))
          "`:persons` is stamped as the AUTHORED map -- a manifest describes a
           configuration, and the population itself is the corpus")
      (is (= (:persona-config witness-config) (:persona-config params)))))
  (testing "absent: neither key appears, so no non-person corpus grows a key
            that means nothing to it"
    (let [params (:engine-params (:manifest (ok-payload (run/run-command {:seed 1 :patients 2}))))]
      (is (not (contains? params :persons)))
      (is (not (contains? params :persona-config))))))

;; --- the real stream, folded ----------------------------------------------

(deftest a-real-person-stream-reaches-the-log-as-the-two-kinds-test
  (let [gt (:ground-truth (ok-payload (run/run-command witness-config)))
        updates (of-kind gt :demographic-update)
        coverage (of-kind gt :coverage-change)
        registered (of-kind gt :registered)]
    (testing "counted witnesses -- every one pos?, none pinned"
      (is (pos? (count registered)) "no patient registered at all")
      (is (pos? (count updates)) "a real person walk produced no :demographic-update")
      (is (pos? (count coverage)) "a real person walk produced no :coverage-change"))
    (testing "the whole log is t-ascending: the fold entered the SAME sorted queue"
      (is (apply <= (map :t gt))))
    (testing "every folded event names one patient and carries its person stamp"
      (doseq [ev (concat updates coverage)]
        (is (= 1 (count (:participants ev))))
        (is (string? (:person-event-id ev)))
        (is (re-find #"^PERSON-\d+#\d+$" (:person-event-id ev)))))
    (testing "and every one reports a real change"
      (is (every? #(not= (:value %) (:prior-value %)) updates))
      (is (every? #(not= (:payer %) (:prior-payer %)) coverage)))
    (testing "the pool is smaller than the arrival count, so repeats resolved"
      (is (< (count registered) (:patients witness-config))))
    (testing "a person unhoused at t0 registers with the residence SUM, and their
              Persona still carries an address (Persona's own `:address` is
              required and non-nilable)"
      (let [unhoused (filter :residence registered)]
        (is (pos? (count unhoused))
            "no registration carried a residence sum -- the t0 condition was dropped")
        (is (every? #(= :unhoused (:status (:residence %))) unhoused))
        (is (every? #(some? (:address (:persona %))) unhoused))))))

(deftest a-persons-run-satisfies-the-whole-invariant-catalog-test
  ;; run-command already refuses a run whose self-check fails
  ;; (`:self-check-failed`), so reaching :ok is itself the assertion --
  ;; but it is re-run here explicitly, because the six invariants
  ;; ADR-0173 section 2(e) adds are new and a reader should see them
  ;; named on a real fold rather than inferred from an absence of red.
  (let [gt (:ground-truth (ok-payload (run/run-command witness-config)))
        checked (check/check-all gt)]
    (is (pos? (count gt)))
    (is (result/ok? checked) (str "invariant violations: " (pr-str (:payload checked))))
    (testing "and the six new ones actually ran"
      (let [names (set (:invariants-checked (:payload checked)))]
        (doseq [n '[identity-fill-references-its-placeholder-registration
                    identification-merge-survivor-is-the-persons-prior-patient
                    every-placeholder-registration-is-resolved-or-still-open
                    demographic-update-reports-a-real-change
                    no-demographic-event-after-a-patient-expires
                    person-scoped-provenance-is-a-stamp-not-a-reference]]
          (is (contains? names n) (str n " is not in the catalog check-all ran")))))))

(deftest a-persons-run-is-deterministic-test
  ;; sim/ADR-0002: determinism is law, and the fold's TWO calls into the
  ;; person process are the place it could most easily have been broken
  ;; -- both passes derive their own streams from the master seed, so
  ;; neither continues the other.
  (let [a (:ground-truth (ok-payload (run/run-command witness-config)))
        b (:ground-truth (ok-payload (run/run-command witness-config)))]
    (is (pos? (count a)))
    (is (= a b))))
