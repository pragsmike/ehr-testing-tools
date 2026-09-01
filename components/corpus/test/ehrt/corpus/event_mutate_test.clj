(ns ehrt.corpus.event-mutate-test
  "The CLOSED ORACLE LOOP for event-stream mutation (ADR-0176, all nine
  questions ruled (a) on 2026-09-01): inject defect class X into a real
  generated ground-truth log, and `check` must report finding class X
  and NOTHING ELSE.

  This is the acceptance surface the whole row exists for, and it is
  stated here as ADR-0176 section 2(iv) states it, step for step:

    1. (check-all L)         => :ok                  the parent is clean
    2. (mutate L op s)       => L'                   total, pure, ONE site
    3. every event schema-valid                      Q9(a)
    4. (check-all L')        => :rejected
    5. observed finding set  =  declared set         Q5(a), EQUALITY
    6. (mutate L op s) twice => the same L'          determinism
    7. L' not= L                                     it actually did something

  THE POPULATION, and a finding that moved it. ADR-0176 section 2(iv)
  named `bin/ground-truth-bracket`'s own gated corpora as the natural
  population for this gate. Probed at 7096394 before this file was
  written, they carry ZERO candidate sites: every engine-layer oracle
  root runs a `module-only` pathway, and NONE of the four log-index
  reference fields (`:cancels-event-id`, `:order-event-id`,
  `:start-event-id`, `:placeholder-event-id`) appears in any of them.
  The reference fields are minted by the FULL sim path -- scheduling,
  identification, medication spans -- which the oracle's engine-layer
  roots do not exercise. So the population here is a real
  `ehrt.sim.interface/run-command` run over `demos/scenarios/
  clinic-decade`'s own config, the same scenario ADR-0166's own
  `:care-plan-end` finding came off, at 60 patients rather than 200
  because that is the smaller population measured to carry the larger
  number of candidate sites (31 of them, versus 29 at 200).

  Hermeticity, declared: this is the ONE non-hermetic namespace in this
  component, deliberately -- the same licence `sim_adapter_test.clj`'s
  own `run-default-calls-the-real-run-command-test` takes, and for a
  stronger reason. `rulings.md#R-measure-claimed-population` requires a
  measurement claiming to characterize the simulator's output to draw
  from the real seeded, threaded path, and this gate's whole claim is
  about a real log. A hand-scripted fixture would prove the operator
  against a fixture, which is exactly the gap ADR-0176 section 1(e)
  says this row closes."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus.lineage :as lineage]
            [ehrt.corpus.mutate :as mutate]
            [ehrt.corpus.operators :as operators]
            ;; corpus -> sim / sim-engine, both dependency-legal
            ;; (AGENTS.md Constraints: corpus may depend on sim; neither
            ;; sim nor sim-engine may depend on anything corpus-derived,
            ;; so no cycle) -- ADR-0176 Q2(a) names this edge by name.
            [ehrt.sim.interface :as sim]
            [ehrt.sim-engine.interface :as engine]))

(def ^:private operator-id :phantom-placeholder-event-id)

(def ^:private clean-log
  "One real run, computed once. `delay` rather than a fixture so the
  cost is paid only if a test in this namespace actually runs."
  (delay
    (let [cfg (edn/read-string (slurp "demos/scenarios/clinic-decade/config.edn"))
          r (sim/run-command (merge {:seed 5 :patients 60} cfg))]
      (assert (sim/ok? r) "the population run must succeed")
      (vec (get-in r [:payload :ground-truth])))))

(defn- op [] (operators/lookup operator-id "1"))

(defn- differing-indices
  [a b]
  (keep-indexed (fn [i [x y]] (when (not= x y) i)) (map vector a b)))

;; ---- step 1: the parent is clean ----------------------------------

(deftest the-unmutated-log-checks-clean-test
  ;; Not ceremony (ADR-0176 section 2(iv)): this is what makes the
  ;; finding-set equality below a statement about the OPERATOR rather
  ;; than about the corpus.
  (let [r (sim/check-all @clean-log)]
    (is (kernel/ok? r))
    (is (pos? (count @clean-log)))))

;; ---- registration -------------------------------------------------

(deftest the-event-operator-is-registered-with-a-declared-finding-set-test
  (let [entry (op)]
    (is (some? entry) "the spine operator must be in the one registry (ADR-0176 Q2(a))")
    (is (= :event (:format entry)) "the third value of the existing :format discriminator")
    (is (false? (:locator-required? entry)) "an event operator selects its own site by draw")
    (is (true? (:seed-consuming? entry)))
    (is (= #{:identity-fill-references-its-placeholder-registration}
           (:expected-findings entry))
        "the defect class, named in check's own closed vocabulary")
    (is (= :violates (:type (:contract entry))))
    (is (string? (:doc entry)))))

;; ---- steps 2, 6, 7: total, pure, deterministic, ONE site ----------

(deftest mutation-changes-exactly-one-site-and-nothing-else-test
  (let [l @clean-log
        r (mutate/mutate l (op) 424242)]
    (is (kernel/ok? r))
    (let [mutant (:mutant (:payload r))]
      (testing "step 7 -- it actually did something (the ADR-0165 lesson)"
        (is (not= mutant l)))
      (testing "same length, same order -- a referential operator moves no event"
        (is (= (count l) (count mutant))))
      (testing "step 2 -- ONE site, one draw (Q3(a))"
        (is (= 1 (count (differing-indices l mutant)))))
      (testing "byte-identity of everything the mutation did not touch"
        (let [site (first (differing-indices l mutant))]
          (is (= (pr-str (into [] (keep-indexed #(when (not= %1 site) %2) l)))
                 (pr-str (into [] (keep-indexed #(when (not= %1 site) %2) mutant)))))
          (testing "and at the site itself, only the reference field moved"
            (is (= (dissoc (nth l site) :placeholder-event-id)
                   (dissoc (nth mutant site) :placeholder-event-id))))))
      (testing "the input is not mutated in place"
        (is (= l @clean-log))))))

(deftest mutation-is-deterministic-in-its-own-seed-test
  (let [l @clean-log]
    (testing "step 6 -- same seed, same mutant"
      (is (= (:mutant (:payload (mutate/mutate l (op) 424242)))
             (:mutant (:payload (mutate/mutate l (op) 424242))))))
    (testing "a different seed selects a different site"
      ;; The operator's own seed, independent of the run's master seed
      ;; (Q4(a)) -- so two seeds over one log are two injections. A
      ;; one-site draw that ignores its seed is a fault injector
      ;; reporting success while injecting the same thing every time.
      (let [sites (into #{}
                        (map (fn [s]
                               (let [m (:mutant (:payload (mutate/mutate l (op) s)))]
                                 (first (differing-indices l m)))))
                        (range 1 17))]
        (is (< 1 (count sites)))))))

;; ---- step 3: Q9(a), the mutant stays schema-valid ------------------

(deftest the-mutant-is-schema-valid-test
  ;; Q9(a): check is the declared oracle, so a mutant convicted by the
  ;; Malli schema instead would close the loop on the wrong instrument
  ;; and the operator's own :expected-findings would name nothing that
  ;; fires.
  (let [mutant (:mutant (:payload (mutate/mutate @clean-log (op) 424242)))]
    (is (every? engine/valid-event? mutant))
    (is (engine/run-t-monotone? mutant))))

;; ---- steps 4 and 5: the loop closes, on EQUALITY -------------------

(deftest check-reports-exactly-the-declared-finding-set-test
  (let [mutant (:mutant (:payload (mutate/mutate @clean-log (op) 424242)))
        r (sim/check-all mutant)]
    (testing "step 4"
      (is (kernel/rejected? r))
      (is (= :invariant-violation (:category r))))
    (testing "step 5 -- EQUALITY, not containment (Q5(a))"
      ;; A subset check would let a cascade hide behind a declared
      ;; finding, which is the exact failure the post-run injection
      ;; contract (Q1(a)) exists to avoid.
      (is (= (:expected-findings (op))
             (set (map :invariant (:violations (:payload r)))))))))

;; ---- lineage (ADR-0176 section 2(iii)) -----------------------------

(deftest lineage-records-parent-operator-seed-and-site-test
  (let [l @clean-log
        {:keys [mutant lineage]} (:payload (mutate/mutate l (op) 424242))
        t (:transformation lineage)]
    (is (lineage/valid? lineage))
    (is (lineage/valid-content-hash? lineage) "self-verifying :id")
    (is (= :mutate (:stage lineage)))
    (testing "parent identity"
      (is (= (mutate/event-content-hash l) (:parent lineage)))
      (is (= (mutate/event-content-hash mutant) (:produced lineage))))
    (testing "operator id and version"
      (is (= {:id operator-id :version "1"} (:operator t))))
    (testing "the operator's own seed -- the new slot (Q4(a))"
      (is (= 424242 (:seed t))))
    (testing "the site, so the injection is exact"
      (is (= (first (differing-indices l mutant)) (:site t))))
    (testing "the contract and the declared finding set ride along"
      (is (= (:contract (op)) (:contract t)))
      (is (= (:expected-findings (op)) (:expected-findings t))))
    (testing "no :locator slot -- an event operator is handed no locator"
      (is (not (contains? t :locator))))))

;; ---- population closure (rulings.md#R-empty-population-is-red) -----

(deftest a-log-with-no-candidate-site-is-reported-not-tolerated-test
  ;; An operator that silently mutates nothing is the ADR-0165 silence
  ;; one layer up. A log offering no site is a REJECTION, never an :ok
  ;; carrying the input back unchanged.
  (let [r (mutate/mutate [{:event :registered :t 0
                           :participants [{:patient-id "P1" :role :subject}]}]
                         (op) 1)]
    (is (kernel/rejected? r))
    (is (= :no-candidate-site (:category r)))
    (is (= operator-id (:operator-id (:payload r))))))

;; ---- Q6: an operator the catalog cannot convict --------------------

(deftest an-unconvictable-event-operator-is-refused-and-recorded-as-a-gap-test
  ;; Q6(a): refuse registration, and record the candidate as a CATALOG
  ;; GAP. The v2 catalog's own precedent (operators.clj's docstring,
  ;; "recorded as dropped, not shipped unconvictable") with one
  ;; difference that matters -- `check`'s catalog is this repository's
  ;; OWN, so an unconvictable event operator is evidence of a hole in
  ;; it, and ADR-0166 is the standing proof such holes sit unnoticed
  ;; for weeks.
  (let [snapshot (operators/registry-snapshot)
        gaps-before (operators/catalog-gaps)]
    (try
      (let [r (operators/register!
               {:id :dummy-unconvictable :version "1" :format :event
                :doc "A dummy that declares no finding at all."
                :contract {:type :violates :target "nothing this repository's own catalog can convict"}
                :locator-required? false :seed-consuming? true
                :expected-findings #{}
                :candidate-sites (fn [_events] [])
                :fn (fn [events _site] events)})]
        (is (kernel/rejected? r))
        (is (= :unconvictable-operator (:category r)))
        (is (nil? (operators/lookup :dummy-unconvictable "1"))
            "refused means NOT in the registry")
        (testing "and recorded as a catalog gap rather than dropped silently"
          (let [gaps (operators/catalog-gaps)
                gap (first (filter #(= :dummy-unconvictable (:id %)) gaps))]
            (is (= (inc (count gaps-before)) (count gaps)))
            (is (some? gap))
            (is (= :event (:format gap)))
            (is (= :no-declared-finding (:reason gap))))))
      (finally
        (operators/reset-registry! snapshot)
        (operators/reset-catalog-gaps! gaps-before)))))

(deftest an-event-operator-must-declare-its-seed-consumption-test
  ;; ADR-0176 section 2(i): registration states, and the registry
  ;; validates, `:seed-consuming? true` and `:locator-required? false`
  ;; -- an event operator selects its own site by draw rather than
  ;; being handed one.
  (let [snapshot (operators/registry-snapshot)
        gaps-before (operators/catalog-gaps)]
    (try
      (let [r (operators/register!
               {:id :dummy-locator-driven :version "1" :format :event
                :doc "A dummy that asks to be handed a locator."
                :contract {:type :violates :target "irrelevant"}
                :locator-required? true :seed-consuming? false
                :expected-findings #{:timestamps-monotone}
                :candidate-sites (fn [_events] [])
                :fn (fn [events _site] events)})]
        (is (kernel/rejected? r))
        (is (= :invalid-operator (:category r))))
      (finally
        (operators/reset-registry! snapshot)
        (operators/reset-catalog-gaps! gaps-before)))))
