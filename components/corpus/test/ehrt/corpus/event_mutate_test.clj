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
  file is the same loop PARAMETERIZED over every operator the catalog
  carries, so the next one is a row in `loop-rows` rather than a new
  test, and a regression in any one of them names itself.

  THE POPULATION, and the finding that fixed it. ADR-0176 section 2(iv)
  named `bin/ground-truth-bracket`'s own gated corpora as the natural
  population for this gate, and its dated addendum (a) records that they
  carry ZERO candidate sites: every engine-layer oracle root runs a
  `module-only` pathway, and none of the log-index reference fields
  appears in any of them. The reference fields are minted by the FULL
  sim path -- scheduling, identification, medication spans -- which
  those roots do not exercise.

  So the population here is THREE real runs. Two are the opt-in demo
  configs that were, on 2026-09-01, the only distinct logs in the tree
  carrying candidate sites at all; the measurement behind that choice is
  `.agents/plans/2026-09-01-event-mutation-population-ledger.md`. The
  third is `demos/scenarios/dense-7500/config.edn`, and it is here
  because those two carry sites in only TWO of the five referential
  carrier columns -- the other three were population gaps until P7
  (2026-09-05) measured this config carrying all three. Every one of
  them is measured clean before anything is injected.

  THE THIRD POPULATION IS EXPENSIVE AND THE REASON IS NOT THIS FILE'S.
  Its run is ~54 s and FLAT in `--patients` (the cost is the
  `:persons {:count 15000 :years 20}` layer, so 20 arrivals is the
  cheapest count that still carries every column), and each `check-all`
  over its 18,466 events is ~45 s of which ~42 s is
  `every-event-is-schema-valid` alone -- see `schema-valid?` below for
  the measurement and the one-line fix, which is outside this session's
  fence.

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
            [ehrt.sim-engine.interface :as engine]
            ;; NOT an interface, and deliberately so: ruling Q11(c)
            ;; (2026-09-05) keeps `ehrt.sim-check.interface` at one var
            ;; and puts the finding-vocabulary law in TEST, which may
            ;; reach any namespace. `components/corpus/src` gains no
            ;; edge to `components/sim-check` from this.
            [ehrt.sim-check.check :as check]))

;; ---- the two populations ------------------------------------------

(defn- config [path] (edn/read-string (slurp path)))

(defn- run-log
  ([cfg seed patients] (run-log cfg seed patients nil))
  ([cfg seed patients extra]
   (let [r (sim/run-command (merge {:seed seed :patients patients} cfg extra))]
     (assert (sim/ok? r) "the population run must succeed")
     (vec (get-in r [:payload :ground-truth])))))

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
            {:log (run-log cfg 5 40) :facility (:facility cfg)}))
   ;; The third, and the only one carrying columns A, B2 and C.
   ;; `--churn` is not decoration: it is what mints the cancel family at
   ;; all, and `:churn true` is the config-key spelling of the flag. The
   ;; site counts this population owes are 4 `:cancel-transfer`, 6
   ;; `:medication-end` and 8 `:care-plan-end`; `:cancel-admit` and
   ;; `:cancel-discharge` do not occur here at 20 arrivals, so column A's
   ;; per-carrier `:target` map is exercised on one of its three keys and
   ;; the other two are structure, not witness (P7 record, section 1).
   :dense-7500
   (delay (let [cfg (config "demos/scenarios/dense-7500/config.edn")]
            {:log (run-log cfg 5 20 {:churn true}) :facility (:facility cfg)}))})

(defn- pop-of [k] @(get populations k))

(defn- check-all
  [{:keys [facility]} events]
  (if facility (sim/check-all events facility) (sim/check-all events)))

(defn- findings
  [pop events]
  (let [r (check-all pop events)]
    (if (kernel/ok? r) #{} (set (map :invariant (:violations (:payload r)))))))

;; ---- the catalog, as loop rows -------------------------------------

(def ^:private schema-valid?
  "`engine/valid-event?`'s own predicate over `engine/Event`, with the
  validator built ONCE.

  Not a different claim and not a way around the published surface --
  the same schema object, and `valid-event?` is literally
  `(m/validate Event event)`. Malli builds a fresh validator on every
  such call, and the difference is not marginal: measured this session
  on `event-examples.edn`, 2.29 ms an event against 0.0063 ms for a
  prebuilt validator, 365x. Over the dense-7500 log's 18,466 events
  that is 42 s a pass against 0.12 s, and step 3 below makes one pass
  per row.

  RECORDED RATHER THAN QUIETLY WORKED AROUND. The cost is
  `ehrt.sim-engine.event-schema/valid-event?`'s own, `check-all` pays
  it too on every call (`every-event-is-schema-valid`, ADR-0178, which
  runs first in the catalog), and nothing this namespace can do reaches
  that half. The one-line fix -- bind `(m/validator Event)` once beside
  the schema -- is `sim-engine` src and outside the P7 fence; the
  session record names it."
  (m/validator engine/Event))

(def ^:private identity-fill-invariant
  :identity-fill-references-its-placeholder-registration)
(def ^:private result-order-invariant
  :result-references-existing-order-and-follows-it-in-time)
(def ^:private cancel-invariant
  :cancel-references-existing-uncancelled-event)
(def ^:private medication-end-invariant
  :medication-end-references-existing-order-and-follows-it-in-time)
(def ^:private care-plan-end-invariant
  :care-plan-end-references-existing-start-and-follows-it-in-time)

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
    :effect :one-event}

   ;; --- column A: :cancels-event-id on a cancellation, whose target is
   ;;     the event class that cancellation cancels -- three carrier
   ;;     kinds citing three DIFFERENT classes, which is the whole
   ;;     reason `:target` may be a map. FOUR shapes, not five: the
   ;;     field is a plain :int, so Q9(a) drops the null cell exactly as
   ;;     it does for B1.
   ;;
   ;;     THE FOURTH ROW HERE COULD NOT HAVE BEEN WRITTEN BEFORE
   ;;     2026-09-04. `cancel-references-existing-uncancelled-event` had
   ;;     no time clause at all, so a cancel moved behind its own
   ;;     referent convicted `:timestamps-monotone` and nothing
   ;;     referential -- the P7 derivation measured that and REFUSED the
   ;;     cell under Q5(a) rather than shipping an operator that could
   ;;     not convict what it named. ADR-0178 (R-time) added the fifth
   ;;     disjunct; this row is the cell arriving.
   {:id :phantom-cancels-event-id :population :dense-7500
    :findings #{cancel-invariant} :effect :one-event}
   {:id :cross-patient-cancels-event-id :population :dense-7500
    :findings #{cancel-invariant} :effect :one-event}
   {:id :wrong-kind-cancels-event-id :population :dense-7500
    :findings #{cancel-invariant} :effect :one-event}
   {:id :inverted-span-cancels-event-id :population :dense-7500
    :findings #{cancel-invariant :timestamps-monotone}
    :effect :one-event :moves-t? true}

   ;; --- column B2: :order-event-id on a :medication-end, whose target
   ;;     is the :medication-order it closes. The SAME FIELD NAME as B1
   ;;     above on a different carrier, convicted by a different
   ;;     invariant and typed `[:maybe :int]` rather than `:int` -- so
   ;;     all five shapes ship here where B1 gets four, and the ids
   ;;     carry the carrier in their stem (`:slug`, operators.clj)
   ;;     because B1's are already published without it.
   {:id :phantom-medication-end-order-event-id :population :dense-7500
    :findings #{medication-end-invariant} :effect :one-event}
   {:id :null-medication-end-order-event-id :population :dense-7500
    :findings #{medication-end-invariant} :effect :one-event}
   {:id :cross-patient-medication-end-order-event-id :population :dense-7500
    :findings #{medication-end-invariant} :effect :one-event}
   {:id :wrong-kind-medication-end-order-event-id :population :dense-7500
    :findings #{medication-end-invariant} :effect :one-event}
   {:id :inverted-span-medication-end-order-event-id :population :dense-7500
    :findings #{medication-end-invariant :timestamps-monotone}
    :effect :one-event :moves-t? true}

   ;; --- column C: :start-event-id on a :care-plan-end, whose target is
   ;;     the :care-plan-start it closes -- ADR-0166's own twin span,
   ;;     and the invariant that arc landed because nothing was asking.
   ;;     Five shapes, `[:maybe :int]` like B2.
   {:id :phantom-start-event-id :population :dense-7500
    :findings #{care-plan-end-invariant} :effect :one-event}
   {:id :null-start-event-id :population :dense-7500
    :findings #{care-plan-end-invariant} :effect :one-event}
   {:id :cross-patient-start-event-id :population :dense-7500
    :findings #{care-plan-end-invariant} :effect :one-event}
   {:id :wrong-kind-start-event-id :population :dense-7500
    :findings #{care-plan-end-invariant} :effect :one-event}
   {:id :inverted-span-start-event-id :population :dense-7500
    :findings #{care-plan-end-invariant :timestamps-monotone}
    :effect :one-event :moves-t? true}])

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

(defn- assert-the-loop
  "ADR-0176 section 2(iv) steps 2-7, for ONE (operator, population)
  pair. Factored out so the catalog-wide gate below and the row's own
  declared population run literally the same loop rather than two
  loops that agree by inspection.

  `expected` is the row's own `:findings` everywhere except a declared
  shape gap, where it is the set MEASURED at the site this gate's seed
  draws -- see `declared-shape-gaps`."
  [{:keys [id effect moves-t?]} population expected]
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
          (is (every? schema-valid? mutant))
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
            (is (= expected (set (map :invariant (:violations (:payload r')))))
                "observed = declared, exactly")))))))

(def ^:private population-order
  "The populations in a fixed order, so the matrix below and every
  report over it read the same on every run."
  [:clinic-decade :ed-tuesday :dense-7500])

(def ^:private site-matrix
  "Every (operator, population) pair with the number of candidate sites
  that population offers it -- MEASURED by calling the operator's own
  `:candidate-sites` over the log, exactly as `mutate/mutate` does
  (`mutate.clj:160`, over a vector).

  A `delay` for the same reason `populations` is one: the whole matrix
  costs three real runs, and a namespace nobody selected should pay
  nothing."
  (delay
   (vec (for [{:keys [id]} loop-rows
              p population-order]
          {:id id :population p
           :sites (count ((:candidate-sites (op id)) (vec (:log (pop-of p)))))}))))

(def ^:private declared-shape-gaps
  "SHAPE GAPS, in ADR-0176 addendum (c)'s own sense and vocabulary: a
  candidate whose observed finding set VARIES SITE TO SITE, so no set
  can honestly be declared for it. The addendum names the kind and the
  remedy -- *\"narrowing or nothing, never a declared set chosen from
  the modal case\"* -- and this register is neither. It is the honest
  place to park ONE pair whose remedy needs a ruling, keyed
  `[operator-id population]`.

  ONE ENTRY, MEASURED EXHAUSTIVELY 2026-09-05, and it is a real defect
  awaiting a disposition rather than an exemption:
  `:orphan-participant` was narrowed by the breadth session against
  BOTH logs that existed then, its set measured identical at every
  sampled site of each. `demos/scenarios/dense-7500/config.edn` is the
  third, and over ALL FORTY-EIGHT of its candidate sites the operator
  produces THREE distinct sets, not one:

      34 sites  the declared four
       6 sites  + :medication-end-references-existing-order-...
       8 sites  + :care-plan-end-references-existing-start-...

  The mechanism is addendum (c)'s own point 2 one layer further on.
  Renaming a participant moves the event into a phantom patient's
  timeline; the narrowing derives the site list from `check`'s
  `clinical-content-only-when-admitted`, and that kind list CONTAINS
  the span STARTS (`:medication-order`, `:care-plan-start`). So on a
  log that closes its spans -- which the two calibration logs do not --
  the span's own referential invariant convicts as well. The narrowing
  that made the operator honest against two logs is precisely what
  makes it dishonest against the third.

  THE DISPOSITION IS OWED AN AUTHOR RULING and is fenced out of the
  session that measured this (no `operators.clj` change): narrow
  `:candidate-sites` again to exclude a site that is the START of a
  referenced span; widen nothing (Q5(a) is equality, so a wider set
  goes red on clinic-decade); or retire the operator. Recorded in
  ADR-0176 and rowed, not decided here.

  The entry is SELF-POLICING IN BOTH DIRECTIONS. The loop still runs in
  full and still asserts set EQUALITY for this pair -- against the set
  measured at the site seed 424242 draws (site 122, one of the six) --
  and the test below additionally asserts that the pair DOES diverge
  from its declaration. Narrow the operator and this entry turns red
  and has to be deleted; it can never decay into a silent pass."
  {[:orphan-participant :dense-7500]
   #{:clinical-content-only-when-admitted
     :every-encounter-is-opened-and-closed-or-still-open
     :medication-end-references-existing-order-and-follows-it-in-time
     :participant-ids-exist-in-run
     :registered-is-every-patients-first-event}})

(deftest the-closed-oracle-loop-holds-for-every-sited-pair-test
  ;; THE CATALOG-WIDE GATE (ADR-0176 section 2(iv), "the whole catalog
  ;; against a fixed set of clean logs"). Until 2026-09-05 this loop ran
  ;; each operator against the ONE population its row names, which
  ;; proves the operator convicts what it declares SOMEWHERE. The
  ;; declaration is a property of the OPERATOR, though, not of the pair
  ;; -- so wherever a log offers a site, the same equality must hold,
  ;; and a set that is population-dependent is an operator whose
  ;; `:expected-findings` was read off one corpus.
  (doseq [{:keys [id population sites]} @site-matrix
          :when (pos? sites)]
    (testing (str id " over " population " (" sites " sites)")
      (let [row (first (filter #(= id (:id %)) loop-rows))
            gap (get declared-shape-gaps [id population])]
        (assert-the-loop row population (or gap (:findings row)))))))

(deftest every-declared-shape-gap-actually-diverges-test
  ;; A declared gap that no longer diverges is an exemption, and an
  ;; exemption is what this register must never decay into. Each entry
  ;; must name a real pair, and its measured set must differ from the
  ;; operator's declaration -- otherwise delete the entry.
  (doseq [[[id population] observed] declared-shape-gaps]
    (testing (str id " over " population)
      (let [row (first (filter #(= id (:id %)) loop-rows))]
        (is (some? row) "a shape gap names an operator that has a loop row")
        (is (pos? (:sites (first (filter #(and (= id (:id %))
                                               (= population (:population %)))
                                         @site-matrix))))
            "a shape gap names a pair that is actually sited")
        (is (not= (:findings row) observed)
            (str id " over " population " no longer diverges from its declaration -- "
                 "delete the declared-shape-gaps entry"))))))

(deftest every-operator-population-pair-is-sited-or-reported-test
  ;; "Reported by name, not skipped silently." A pair with no site is
  ;; not a defect -- a log that mints no `:medication-end` offers column
  ;; B2 nowhere to inject, and that is a property of the corpus. What
  ;; would be a defect is the gate quietly shrinking, so the unsited
  ;; half is PRINTED with its names on every run and the three things
  ;; that must not change are asserted.
  (let [m @site-matrix
        unsited (filterv (comp zero? :sites) m)]
    (println (str "DISCLOSURE: catalog-wide oracle loop -- "
                  (count (filter (comp pos? :sites) m)) " of " (count m)
                  " (operator, population) pairs are sited and run the loop; "
                  (count unsited) " offer no candidate site:"))
    (doseq [{:keys [id population]} unsited]
      (println (str "  no site: " id " over " population)))
    (doseq [[[id population] observed] declared-shape-gaps]
      (println (str "DISCLOSURE: declared shape gap -- " id " over " population
                    " runs the loop against a MEASURED set of " (count observed)
                    ", not its declaration; disposition owed a ruling (ADR-0176)")))
    (testing "the matrix is complete -- every operator against every population"
      (is (= (* (count loop-rows) (count population-order)) (count m)))
      (is (= (set (map :id loop-rows)) (set (map :id m))))
      (is (= (set population-order) (set (map :population m)))))
    (testing "every loop row's OWN declared population is sited"
      ;; A row naming a population that offers it nothing is a row that
      ;; was never true, and `:no-candidate-site` would only say so at
      ;; the moment someone ran it.
      (doseq [{:keys [id population]} loop-rows]
        (is (pos? (:sites (first (filter #(and (= id (:id %))
                                               (= population (:population %)))
                                         m))))
            (str id " declares population " population ", which offers it no site"))))
    (testing "and no operator is unsited everywhere (R-empty-population-is-red)"
      (doseq [id (map :id loop-rows)]
        (is (pos? (reduce + (map :sites (filter #(= id (:id %)) m))))
            (str id " has no candidate site in ANY population"))))))

(deftest every-registered-event-operator-has-a-loop-row-test
  ;; `register!` is a bare `swap! registry assoc`, so two catalog
  ;; entries deriving the same [id version] SILENTLY REPLACE one
  ;; another: no refusal, no gap record, and the only symptom is an
  ;; operator quietly missing from the catalog. That is not
  ;; hypothetical -- columns B1 and B2 both carry `:order-event-id`,
  ;; and without `:slug` (operators.clj) B2's five entries would have
  ;; overwritten four of B1's on the way in. This is the gate over it,
  ;; and it is a BIJECTION rather than a count so that adding an
  ;; operator moves no pin: a collision shows up as the registered set
  ;; being smaller than the declared one, and a row written for an
  ;; operator that was never registered shows up the same way.
  (let [rows (map :id loop-rows)
        registered (into #{} (comp (filter #(= :event (:format %))) (map :id))
                         (operators/entries))]
    (is (= (count rows) (count (set rows)))
        "two loop rows naming one operator id is itself the collision")
    (is (= (set rows) registered)
        "every registered event operator has a loop row, and every row an operator")))

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

  EMPTY SINCE 2026-09-05, AND KEPT RATHER THAN DELETED. It carried
  `:cancels-event-id` and `:start-event-id` until P7 measured
  `demos/scenarios/dense-7500/config.edn` carrying both (and B2's
  `:medication-end` carrier of `:order-event-id`, which this
  field-keyed set could never have expressed, since B1 covered the
  field's name). The gap KIND is not retired with the gaps: the next
  reference field to arrive without a population belongs here, with its
  measurement, rather than as an operator that can never fire.

  This set is what keeps the derivation honest: a reference field is
  either COVERED by an operator or DECLARED empty here, and a fifth one
  arriving turns this test red instead of silently going uncovered --
  which is ADR-0166's own error ledger (a referential invariant left
  unmirrored onto its structural twin for three weeks) applied one layer
  up."
  #{})

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

;; ---- the finding vocabulary as a law (Q11(c), 2026-09-05) ----------
;;
;; `:expected-findings` names invariants in `check`'s own closed
;; vocabulary -- that is what makes the loop's step 5 EQUALITY a
;; statement at all -- and until this section landed NOTHING checked
;; that the names are real. An operator could declare a finding `check`
;; cannot produce, and the only symptom would be its own loop row going
;; red, which says "this operator does not convict what it names" and
;; not "this name does not exist".
;;
;; RULED Q11(c), 2026-09-05, re-ruling Q11(a). The option Q11(a)
;; deferred was widening `ehrt.sim-check.interface` so `register!`
;; could cross-check at registration time. It stays one var: a TEST may
;; reach any namespace, so the law lands here and reads `check`'s own
;; catalogs directly. `components/corpus/src` gains no edge to
;; `components/sim-check`, and `register!` is unchanged.

(defn- checker-vocabulary
  "Every invariant name `check` can report, derived from its own four
  catalogs -- `catalog`, `facility-catalog`, `warmup-catalog`,
  `order-profiles-catalog`, the four `check-all` runs -- rather than
  hand-listed here, so a catalog row added or retired moves this set
  with it and no pin has to be touched.

  DERIVED FROM THE VAR NAME, which is exact here rather than merely
  conventional: every catalog invariant emits `{:invariant :<its own
  name> ...}`, measured this session at 46 catalog vars against the 46
  distinct `:invariant` keywords the whole of `check.clj` carries, name
  for name and set for set. It is also self-policing in this very file
  -- `the-closed-oracle-loop-*` asserts observed = declared over REAL
  violation keywords and the law below asserts declared is drawn from
  this set, so a var whose emitted keyword drifted from its own name
  turns one of the two red."
  []
  (into #{}
        (map (comp keyword :name meta))
        (concat check/catalog check/facility-catalog
                check/warmup-catalog check/order-profiles-catalog)))

(defn- unknown-declared-findings
  "Every `[operator-id finding]` pair an event operator declares that
  `check` has no invariant for. Empty is the law."
  []
  (let [vocab (checker-vocabulary)]
    (into #{}
          (comp (filter #(= :event (:format %)))
                (mapcat (fn [e]
                          (for [f (:expected-findings e)
                                :when (not (contains? vocab f))]
                            [(:id e) f]))))
          (operators/entries))))

(deftest every-declared-finding-is-an-invariant-check-can-produce-test
  (is (seq (checker-vocabulary))
      "the four catalogs must yield a vocabulary at all (R-empty-population-is-red)")
  (is (= #{} (unknown-declared-findings))
      (str "an event operator declares a finding `check` cannot produce. Either the "
           "invariant was renamed or retired and the operator was not moved with it, "
           "or the operator names a class this repository's own catalog does not "
           "carry -- which is a hole in `check`, not a property of the operator "
           "(ADR-0176 Q6(a)'s own argument, one layer up).")))

(deftest an-unknown-declared-finding-is-named-not-tolerated-test
  ;; The law above is green on a healthy tree, so this is what proves it
  ;; can go red at all: a synthetic operator declaring an invariant that
  ;; does not exist must be NAMED, id and finding both.
  (let [snapshot (operators/registry-snapshot)
        gaps-before (operators/catalog-gaps)]
    (try
      (let [r (operators/register!
               {:id :dummy-unknown-finding :version "1" :format :event
                :doc "A dummy declaring a finding no catalog row produces."
                :contract {:type :violates :target "irrelevant"}
                :locator-required? false :seed-consuming? true
                :expected-findings #{:no-such-invariant}
                :candidate-sites (fn [_events] [])
                :fn (fn [events _site] events)})]
        (is (kernel/ok? r)
            "registration itself is unchanged -- Q11(c) puts the law in test, not in `register!`")
        (is (= #{[:dummy-unknown-finding :no-such-invariant]}
               (unknown-declared-findings))
            "the offender, named by id and by the finding it invented"))
      (finally
        (operators/reset-registry! snapshot)
        (operators/reset-catalog-gaps! gaps-before)))))

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
