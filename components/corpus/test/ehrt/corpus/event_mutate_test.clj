(ns ehrt.corpus.event-mutate-test
  "The CLOSED ORACLE LOOP for event-stream mutation (ADR-0176, all nine
  questions ruled (a) on 2026-09-01), run over the WHOLE event catalog:
  inject defect class X into a real generated ground-truth log, and
  `check` must report finding class X and NOTHING ELSE.

  This is the acceptance surface the whole row exists for, and it is
  stated here as ADR-0176 section 2(iv) states it, step for step:

    1. (check-all L)         => :ok                  the parent is clean
    2. (mutate L op s)       => L'                   total, pure, ONE site
    3. every event schema-valid                      Q9(a)
    4. (check-all L')        => :rejected
    5. observed finding set  =  declared set         Q5(a), EQUALITY
    6. (mutate L op s) twice => the same L'           determinism
    7. L' not= L                                     it actually did something

  The spine session (2026-09-01) proved that loop on ONE operator. This
  file is the same loop PARAMETERIZED over the twelve the catalog now
  carries, so a thirteenth is a row in `loop-rows` rather than a new
  test, and a regression in any one of them names itself.

  THE POPULATION, and the finding that fixed it. ADR-0176 section 2(iv)
  named `bin/ground-truth-bracket`'s own gated corpora as the natural
  population for this gate, and its dated addendum (a) records that they
  carry ZERO candidate sites: every engine-layer oracle root runs a
  `module-only` pathway, and none of the log-index reference fields
  appears in any of them. The reference fields are minted by the FULL
  sim path -- scheduling, identification, medication spans -- which
  those roots do not exercise.

  So the population here is TWO real runs, over the two opt-in demo
  configs that are the only distinct logs in the tree carrying candidate
  sites at all. Both are measured clean before anything is injected, and
  the measurement behind the choice is
  `.agents/plans/2026-09-01-event-mutation-population-ledger.md`.

  ED-TUESDAY IS CHECKED WITH ITS OWN FACILITY, and that is not a
  convenience. `check-all`'s 1-arity defaults `facility-config` to
  `sim-model/default-facility` (6 ED surge slots); ed-tuesday's config
  bumps that ward to 16 so its busy-shift pacing holds, so the 1-arity
  reports its own clean log as violating `:occupancy-within-capacity`.
  The corpus is sound and the checker is config-starved -- rowed on
  `roadmap.md` as a real consumer-facing gap, since `ehrt sim check`
  exposes no way to pass one. Step 1 below is what makes step 5's
  equality a statement about the OPERATOR rather than about the corpus,
  so it has to be a real clean baseline, not an approximate one.

  Hermeticity, declared: this is the ONE non-hermetic namespace in this
  component, deliberately -- the same licence `sim_adapter_test.clj`'s
  own `run-default-calls-the-real-run-command-test` takes, and for a
  stronger reason. `rulings.md#R-measure-claimed-population` requires a
  measurement claiming to characterize the simulator's output to draw
  from the real seeded, threaded path, and this gate's whole claim is
  about a real log. A hand-scripted fixture would prove the operators
  against a fixture, which is exactly the gap ADR-0176 section 1(e)
  says this row closes."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.walk :as walk]
            [malli.core :as m]
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

;; ---- the two populations ------------------------------------------

(defn- config [path] (edn/read-string (slurp path)))

(defn- run-log
  [cfg seed patients]
  (let [r (sim/run-command (merge {:seed seed :patients patients} cfg))]
    (assert (sim/ok? r) "the population run must succeed")
    (vec (get-in r [:payload :ground-truth]))))

(def ^:private populations
  "Each population is a real run plus the facility `check` must be given
  to see it clean. `delay` rather than a fixture so the cost is paid
  only if a test in this namespace actually runs.

  The patient counts are the smallest measured to carry every operator's
  own population, not the demos' documented ones -- the ledger measures
  at the documented invocation, this gate runs at the cheapest one that
  still exercises every row."
  {:clinic-decade
   (delay (let [cfg (config "demos/scenarios/clinic-decade/config.edn")]
            ;; No facility override in this config, so the default IS
            ;; its facility and the 1-arity is the honest call.
            {:log (run-log cfg 5 60) :facility nil}))
   :ed-tuesday
   (delay (let [cfg (config "demos/scenarios/ed-tuesday/config.edn")]
            {:log (run-log cfg 5 40) :facility (:facility cfg)}))})

(defn- pop-of [k] @(get populations k))

(defn- check-all
  [{:keys [facility]} events]
  (if facility (sim/check-all events facility) (sim/check-all events)))

(defn- findings
  [pop events]
  (let [r (check-all pop events)]
    (if (kernel/ok? r) #{} (set (map :invariant (:violations (:payload r)))))))

;; ---- the catalog, as loop rows -------------------------------------

(def ^:private identity-fill-invariant
  :identity-fill-references-its-placeholder-registration)
(def ^:private result-order-invariant
  :result-references-existing-order-and-follows-it-in-time)

(def ^:private loop-rows
  "One row per registered event operator. `:findings` is the set the
  operator DECLARES and the set `check` must report exactly (Q5(a)).

  `:moves-t?` marks the operators that edit an event's `:t`: their
  mutants are deliberately not `run-t-monotone?`, which is the defect,
  so step 3 asserts per-event schema validity for them and whole-log
  monotonicity only for the rest.

  `:effect` is the structural shape of the edit, and it is asserted:
  `:one-event` leaves the log's length and every other event alone,
  `:drop` shortens it by exactly one."
  [;; --- column D: :placeholder-event-id on an identity fill, whose
   ;;     target is the patient's own placeholder :registered.
   ;;     Five shapes, one per disjunct of the convicting invariant.
   {:id :phantom-placeholder-event-id :population :clinic-decade
    :findings #{identity-fill-invariant} :effect :one-event}
   {:id :null-placeholder-event-id :population :clinic-decade
    :findings #{identity-fill-invariant} :effect :one-event}
   {:id :cross-patient-placeholder-event-id :population :clinic-decade
    :findings #{identity-fill-invariant} :effect :one-event}
   {:id :wrong-kind-placeholder-event-id :population :clinic-decade
    :findings #{identity-fill-invariant} :effect :one-event}
   {:id :inverted-span-placeholder-event-id :population :clinic-decade
    :findings #{identity-fill-invariant :timestamps-monotone}
    :effect :one-event :moves-t? true}

   ;; --- column B1: :order-event-id on a :result-available, whose
   ;;     target is the :order-placed it answers. FOUR shapes, not
   ;;     five: the field is a plain :int here, so nulling it produces
   ;;     a schema-INVALID mutant and Q9(a) excludes it -- the loop
   ;;     would close on Malli instead of on `check`.
   {:id :phantom-order-event-id :population :ed-tuesday
    :findings #{result-order-invariant} :effect :one-event}
   {:id :cross-patient-order-event-id :population :ed-tuesday
    :findings #{result-order-invariant} :effect :one-event}
   {:id :wrong-kind-order-event-id :population :ed-tuesday
    :findings #{result-order-invariant} :effect :one-event}
   {:id :inverted-span-order-event-id :population :ed-tuesday
    :findings #{result-order-invariant :timestamps-monotone}
    :effect :one-event :moves-t? true}

   ;; --- the structural three. ADR-0176 section 2(i) gives each ONE
   ;;     convicting invariant; its addendum (c) records all three
   ;;     claims refuted by measurement, and these are the NARROWED
   ;;     operators that replace them, each with the set measured
   ;;     identical at every sampled site of both logs.
   {:id :clock-skew :population :clinic-decade
    :findings #{:timestamps-monotone} :effect :one-event :moves-t? true}
   {:id :drop-registration :population :clinic-decade
    :findings #{:participant-ids-exist-in-run
                :registered-is-every-patients-first-event}
    :effect :drop}
   {:id :orphan-participant :population :clinic-decade
    :findings #{:clinical-content-only-when-admitted
                :every-encounter-is-opened-and-closed-or-still-open
                :participant-ids-exist-in-run
                :registered-is-every-patients-first-event}
    :effect :one-event}])

(def ^:private seed 424242)

(defn- op [id] (operators/lookup id "1"))

(defn- differing-indices
  [a b]
  (keep-indexed (fn [i [x y]] (when (not= x y) i)) (map vector a b)))

;; ---- step 1: every parent is clean ---------------------------------

(deftest every-population-checks-clean-test
  ;; Not ceremony (ADR-0176 section 2(iv)): this is what makes the
  ;; finding-set equality below a statement about the OPERATOR rather
  ;; than about the corpus.
  (doseq [k (keys populations)]
    (testing (str k)
      (let [pop (pop-of k)]
        (is (pos? (count (:log pop))))
        (is (= #{} (findings pop (:log pop)))
            "the parent must be clean, or step 5 is a statement about the corpus")))))

;; ---- registration shape, every row ---------------------------------

(deftest every-event-operator-declares-the-event-shape-test
  (doseq [{:keys [id findings]} loop-rows]
    (testing (str id)
      (let [entry (op id)]
        (is (some? entry) "must be in the one registry (ADR-0176 Q2(a))")
        (is (= :event (:format entry)) "the third value of the existing :format discriminator")
        (is (false? (:locator-required? entry)) "an event operator selects its own site by draw")
        (is (true? (:seed-consuming? entry)))
        (is (= findings (:expected-findings entry))
            "the defect class, named in check's own closed vocabulary")
        (is (= :violates (:type (:contract entry))))
        (is (string? (:doc entry)))
        (is (fn? (:candidate-sites entry)))))))

;; ---- the loop itself, every row ------------------------------------

(deftest the-closed-oracle-loop-holds-for-every-operator-test
  (doseq [{:keys [id population findings effect moves-t?]} loop-rows]
    (testing (str id)
      (let [pop (pop-of population)
            l (:log pop)
            entry (op id)
            r (mutate/mutate l entry seed)]
        (is (kernel/ok? r) "step 2 -- the log must offer this operator a site")
        (when (kernel/ok? r)
          (let [mutant (:mutant (:payload r))]
            (testing "step 7 -- it actually did something (the ADR-0165 lesson)"
              (is (not= mutant l)))
            (testing "step 3 -- Q9(a), the mutant stays schema-valid"
              (is (every? engine/valid-event? mutant))
              (if moves-t?
                (is (not (engine/run-t-monotone? mutant))
                    "an operator that moves a :t is SUPPOSED to break monotonicity -- that is the defect")
                (is (engine/run-t-monotone? mutant))))
            (testing "the structural shape of the edit"
              (case effect
                :one-event
                (do (is (= (count l) (count mutant)) "one-event operators move no event")
                    (is (= 1 (count (differing-indices l mutant)))
                        "step 2 -- ONE site, one draw (Q3(a))")
                    (let [site (first (differing-indices l mutant))]
                      (is (= (pr-str (into [] (keep-indexed #(when (not= %1 site) %2) l)))
                             (pr-str (into [] (keep-indexed #(when (not= %1 site) %2) mutant))))
                          "byte-identity of everything the mutation did not touch")))
                :drop
                (is (= (dec (count l)) (count mutant))
                    "a drop removes exactly one event")))
            (testing "step 6 -- same seed, same mutant"
              (is (= mutant (:mutant (:payload (mutate/mutate l entry seed))))))
            (testing "the input is not mutated in place"
              (is (= l (:log (pop-of population)))))
            (testing "steps 4 and 5 -- the loop closes, on EQUALITY (Q5(a))"
              ;; A subset check would let a cascade hide behind a
              ;; declared finding, which is the exact failure the
              ;; post-run injection contract (Q1(a)) exists to avoid.
              (let [r' (check-all pop mutant)]
                (is (kernel/rejected? r'))
                (is (= :invariant-violation (:category r')))
                (is (= findings (set (map :invariant (:violations (:payload r')))))
                    "observed = declared, exactly")))))))))

;; ---- lineage, every row (ADR-0176 section 2(iii)) -------------------

(deftest lineage-records-parent-operator-seed-and-site-for-every-operator-test
  (doseq [{:keys [id population]} loop-rows]
    (testing (str id)
      (let [l (:log (pop-of population))
            entry (op id)
            {:keys [mutant lineage]} (:payload (mutate/mutate l entry seed))
            t (:transformation lineage)]
        (is (lineage/valid? lineage))
        (is (lineage/valid-content-hash? lineage) "self-verifying :id")
        (is (= :mutate (:stage lineage)))
        (is (= (mutate/event-content-hash l) (:parent lineage)))
        (is (= (mutate/event-content-hash mutant) (:produced lineage)))
        (is (= {:id id :version "1"} (:operator t)))
        (is (= seed (:seed t)) "the operator's own seed -- the new slot (Q4(a))")
        (is (int? (:site t)) "the site, so the injection is exact")
        (is (= (:contract entry) (:contract t)))
        (is (= (:expected-findings entry) (:expected-findings t)))
        (is (not (contains? t :locator))
            "no :locator slot -- an event operator is handed no locator")))))

;; ---- determinism in the seed, and only in the seed ------------------

(deftest a-different-seed-selects-a-different-site-test
  ;; The operator's own seed, independent of the run's master seed
  ;; (Q4(a)) -- so two seeds over one log are two injections. A
  ;; one-site draw that ignores its seed is a fault injector reporting
  ;; success while injecting the same thing every time.
  (let [l (:log (pop-of :clinic-decade))
        entry (op :clock-skew)
        sites (into #{}
                    (map (fn [s] (:site (:transformation (:lineage (:payload (mutate/mutate l entry s)))))))
                    (range 1 17))]
    (is (< 1 (count sites)))))

;; ---- population closure (rulings.md#R-empty-population-is-red) ------

(deftest a-log-with-no-candidate-site-is-reported-not-tolerated-test
  ;; An operator that silently mutates nothing is the ADR-0165 silence
  ;; one layer up. A log offering no site is a REJECTION, never an :ok
  ;; carrying the input back unchanged. Asserted for EVERY operator, not
  ;; only the spine's -- a `:candidate-sites` that returns everything on
  ;; a one-event log is a predicate that is not doing its job.
  (let [bare [{:event :registered :t 0
               :participants [{:patient-id "P1" :role :subject}]}]]
    (doseq [{:keys [id]} loop-rows]
      (testing (str id)
        (let [r (mutate/mutate bare (op id) 1)]
          (is (kernel/rejected? r))
          (is (= :no-candidate-site (:category r)))
          (is (= id (:operator-id (:payload r)))))))))

;; ---- the derivation gate (ADR-0176 Q8(a), ADR-0166 one layer up) ----

(defn- schema-reference-fields
  "Every `*-event-id` field the LIVE event schema declares as an int (so
  a log index), paired with the event kind that carries it, walked out
  of `engine/Event` itself rather than hand-listed.

  `:person-event-id` excludes itself, and does so on the right ground:
  the catalog rules it a stamp and not a reference
  (`person-scoped-provenance-is-a-stamp-not-a-reference`), and the
  schema types it `:string`. Filtering on int-ness rather than on the
  name is what makes that exclusion a property of the schema instead of
  a special case in this test."
  []
  (let [acc (atom #{})]
    (walk/postwalk
     (fn [form]
       ;; A schema entry is [k type] or [k props type], so the type is
       ;; always the last element -- reading it that way rather than
       ;; destructuring a fixed arity is what lets an OPTIONAL nilable
       ;; field (:placeholder-event-id, optional and nilable) be seen
       ;; alongside a required plain one (:cancels-event-id).
       (when (and (vector? form) (<= 2 (count form) 3) (keyword? (first form)))
         (let [k (first form) t (last form)]
           (when (and (re-find #"-event-id$" (name k))
                      (contains? #{:int [:maybe :int]} t))
             (swap! acc conj k))))
       form)
     (m/form engine/Event))
    @acc))

(def ^:private declared-population-gaps
  "Reference fields the schema declares that NO log this repository can
  generate carries a single site for -- convictable in principle,
  unwitnessable today. Author ruling Q10(a) (2026-09-01) says to record
  these rather than ship operators that can never fire, and the
  measurement is
  `.agents/plans/2026-09-01-event-mutation-population-ledger.md`
  section 6.

  This set is what keeps the derivation honest: a reference field is
  either COVERED by an operator or DECLARED empty here, and a fifth one
  arriving turns this test red instead of silently going uncovered --
  which is ADR-0166's own error ledger (a referential invariant left
  unmirrored onto its structural twin for three weeks) applied one layer
  up."
  #{:cancels-event-id :start-event-id})

(deftest every-schema-reference-field-is-covered-or-declared-empty-test
  (let [fields (schema-reference-fields)
        covered (into #{}
                      (comp (filter #(= :event (:format %)))
                            (keep :reference-field))
                      (operators/entries))]
    (is (seq fields) "the walk must find the schema's reference fields at all")
    (doseq [f fields]
      (testing (str f)
        (is (or (contains? covered f) (contains? declared-population-gaps f))
            (str f " is a log-index reference field with neither an operator nor a "
                 "declared population gap. Add operators for it, or record it in "
                 "declared-population-gaps with the measurement behind it."))))
    (testing "and nothing is declared empty that an operator actually covers"
      (is (empty? (set/intersection covered declared-population-gaps))))))

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
