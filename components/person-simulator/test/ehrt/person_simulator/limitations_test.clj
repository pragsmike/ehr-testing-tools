(ns ehrt.person-simulator.limitations-test
  "One test per row of `docs/limitations.md`, named as the row names
  it, each asserting that the limitation HOLDS -- a guard that goes RED
  the day the decline is silently lifted. That is the whole point of
  ADR-0172 section 4's last column: a limitation with a prose row and
  no gate is a limitation that drifts, which is ADR-0162's own lesson
  and ADR-0170's own species (a claim true when written that nothing
  keeps true).

  Ten of the eleven live here. Row 9's gate --
  `every-provisional-rate-is-tabled-test` -- lives in
  `ehrt.docs-tooling.person-simulator-charter-test` beside the
  citation machinery it needs, and the charter gate asserts that all
  eleven named gates exist somewhere, so neither file can drop one
  quietly."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set :as set]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [ehrt.person-simulator.clock :as clock]
            [ehrt.person-simulator.fixture :as fx]
            [ehrt.person-simulator.process :as process]))

;; --- row 1: twins and multiples are excluded ------------------------------

(deftest every-delivery-is-a-singleton-test
  (let [ds (fx/of-kind :delivery)]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq ds) "the witness stream carries no :delivery at all"))
    (is (every? #(and (string? (:newborn-person-id %))
                      (= 0 (:within-delivery-index %)))
                ds)
        "a :delivery names something other than exactly one newborn at index 0")
    (testing "one newborn per delivery, counted -- a second is red"
      (is (= (count ds) (count (distinct (map :newborn-person-id ds))))))
    (testing "and the newborn stream key reserves the pair, unwidened"
      (is (= (count ds)
             (count (filter #(= 0 (:within-delivery-index %)) ds)))))))

;; --- row 2: immigration and emigration are excluded -----------------------

(deftest the-person-population-is-closed-test
  (let [t0-ids (set (map :person-id fx/population))
        born (set (map :newborn-person-id (fx/of-kind :delivery)))
        seen (set (mapcat #(cons (:person-id %) (:participants %)) (fx/evs)))]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq seen)))
    (let [strays (remove #(or (t0-ids %) (born %)) seen)]
      (is (empty? strays)
          (str (count strays) " person-id(s) appear in the stream that neither"
               " started in the t0 population nor were born into it: " (vec (take 5 strays)))))
    (testing "and every newborn actually enters, so the closure is not vacuous"
      (is (= born (set (map :person-id (fx/of-kind :person-registered))))))))

;; --- row 3: foster placement and adoption are excluded --------------------

(deftest minors-join-households-only-by-birth-or-formation-test
  (let [births (into {} (for [e (fx/of-kind :person-registered)] [(:person-id e) (:t e)]))
        personas (fx/personas)
        joins (fx/of-kind :household-join)
        minor-joins (filter (fn [j]
                              (when-let [p (personas (:person-id j))]
                                (< (quot (- (:t j) (get births (:person-id j) 0))
                                         clock/seconds-per-year)
                                   (- 18 (:age p)))))
                            joins)]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq joins) "the witness stream carries no :household-join at all")
      (is (seq minor-joins) "no minor joins any household -- the gate would pass vacuously"))
    (testing "every minor's join is their own birth, and references a :household-form"
      (let [bad (remove (fn [j] (= (:t j) (get births (:person-id j)))) minor-joins)]
        (is (empty? bad)
            (str (count bad) " household join(s) by a minor at a time other than their own"
                 " birth -- foster placement or adoption has been added without a row: "
                 (vec (take 3 bad)))))
      (is (every? (fn [j] (= :household-form (:event (get @fx/by-id (:household-event-id j)))))
                  minor-joins)))))

;; --- row 4: a death outside care mints no wire event ----------------------

(deftest person-death-emits-no-ground-truth-event-test
  (let [engine-kinds (-> (edn/read-string
                          (slurp "components/sim-engine/resources/sim-engine/event-schema.edn"))
                         :schema (->> (drop 2) (map first) set))
        person-kinds (set (map :event (fx/evs)))]
    (testing "the engine's vocabulary parsed, non-empty (R-empty-population-is-red)"
      (is (= 21 (count engine-kinds))
          (str "expected the CLOSED 21-kind engine vocabulary, parsed " (count engine-kinds))))
    (testing "the witness stream carries deaths at all"
      (is (seq (fx/of-kind :person-death))))
    (testing "no person event is a ground-truth event kind -- a :person-death cannot
              become a :discharge with :disposition :expired, because this component
              mints no engine kind at all"
      (is (empty? (set/intersection engine-kinds person-kinds))
          (str "person events overlap the engine's closed vocabulary: "
               (set/intersection engine-kinds person-kinds))))
    (is (empty? (filter :disposition (fx/evs)))
        "a person event carries a :disposition -- the expired-discharge surface is
         the GMF death's alone (ruling C1)")))

;; --- row 5: name change and data-entry correction are collapsed -----------

(deftest identity-correction-carries-no-cause-test
  (let [cs (fx/of-kind :identity-correction)]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq cs)))
    (is (= #{:name :dob} (set (map :field cs)))
        "the :field vocabulary is not exactly the closed set {:name :dob}")
    (is (empty? (filter :cause cs))
        "an :identity-correction carries a :cause -- the A08-versus-A31 distinction
         has been added without a row")))

;; --- row 6: demographics reach the wire through ONE per-run lookup --------

(deftest personas-are-keyed-by-patient-id-alone-test
  ;; RE-POINTED 2026-08-26, arc 3a part 2. `personas-by-patient-id`
  ;; became `demographics-timeline` and the LOOKUP became
  ;; `(demographics-at demographics patient-id t)` -- twelve threading
  ;; signatures, one lookup shape, output-identical.
  ;;
  ;; The row did NOT go red on that change, and the prediction that it
  ;; would (ADR-0173 section 2(b), which assumed the re-key and the fold
  ;; land together) is what is corrected here. What row 6 states is that
  ;; a delta folded onto patient state is INVISIBLE to every message,
  ;; and that is still true: the re-key moved the lookup's SHAPE while
  ;; the builder's body still folds nothing but the `:registered`
  ;; event's own t0 `:persona`, so every `t` still answers with the same
  ;; value. The row stands and the STRIKE is owed by part 3, which is
  ;; where the fold arrives.
  ;;
  ;; The first assertion was re-pointed at the new symbol for a second
  ;; reason: left naming the old one, it would have kept passing on a
  ;; PROSE MENTION of `personas-by-patient-id` in the new builder's own
  ;; docstring -- a guard green on a comment, which is no guard.
  (let [src (slurp "components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj")]
    (testing "the builder is still there to check (R-empty-population-is-red)"
      (is (str/includes? src "(defn- demographics-timeline")))
    (is (str/includes? src "[(:patient-id (first (:participants ev))) (:persona ev)]")
        "emit-hl7's demographic lookup no longer folds the t0 :persona alone -- if
         its VALUE is now state-at-t, arc 3's fold has landed and limitations row 6
         should be STRUCK, not repaired")))

;; --- row 7: geography stays the 24-row places.edn pool --------------------

(deftest every-residence-address-is-a-places-row-test
  (let [pool (set (map #(select-keys % [:street :city :state :zip]) process/places))
        moves (fx/of-kind :residence-move)]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq moves) "the witness stream carries no :residence-move at all")
      (is (= 24 (count pool)) (str "expected the 24-row places pool, read " (count pool))))
    (let [bad (remove #(pool (:address %)) moves)]
      (is (empty? bad)
          (str (count bad) " residence move(s) carry an address that is not a places.edn"
               " row -- a synthesized address is red: " (vec (take 3 (map :address bad))))))))

;; --- row 8: household structure has no wire surface -----------------------

(defn- src-clj-files []
  (->> (concat (file-seq (io/file "components")) (file-seq (io/file "bases")))
       (filter #(.isFile %))
       (map #(str/replace (.getPath %) "\\" "/"))
       (filter #(str/ends-with? % ".clj"))
       (filter #(re-find #"/(src)/" %))))

(deftest no-emitter-writes-nk1-test
  (let [files (src-clj-files)]
    (testing "the scan sees a real tree (R-empty-population-is-red)"
      (is (< 50 (count files)) (str "only " (count files) " src .clj files scanned"))
      (is (some #(str/includes? (slurp %) "PID") files)
          "no src file mentions PID -- the scan is not reaching the emitters"))
    (let [hits (filter #(str/includes? (slurp %) "NK1") files)]
      (is (empty? hits)
          (str "NK1 now occurs in " (count hits) " src file(s) " (vec hits)
               " -- an emitter writes next-of-kin, which is the day households owe"
               " a rendering row rather than this gate")))))

;; --- row 10: the engine tells the person process nothing ------------------

(defn- component-src-files []
  (->> (file-seq (io/file "components/person-simulator/src"))
       (filter #(.isFile %))
       (map #(str/replace (.getPath %) "\\" "/"))
       (filter #(str/ends-with? % ".clj"))))

(deftest person-simulator-requires-no-engine-namespace-test
  (let [files (component-src-files)]
    (testing "the scan sees this component's src (R-empty-population-is-red)"
      (is (seq files)))
    (testing "the ONLY sim-engine namespace named anywhere in this component's src
              is the interface, and only for the stream-partition surface"
      (let [required (set (mapcat #(map second (re-seq #"\[(ehrt\.[a-z0-9.-]+)\s+:as" (slurp %)))
                                  files))
            named (filter #(str/starts-with? % "ehrt.sim-engine.") required)]
        (is (seq required) "no :require form parsed out of this component's src at all")
        (is (= #{"ehrt.sim-engine.interface"} (set named))
            (str "this component REQUIRES sim-engine namespace(s) beyond the interface: "
                 (set named))))
      ;; call POSITION only -- `(engine/foo`. A docstring naming
      ;; `engine/stream-seed` in prose is a citation, not a dependency, and a
      ;; gate that cannot tell them apart punishes the documentation this
      ;; component is otherwise asked to carry.
      (let [used (set (mapcat #(map second (re-seq #"\(engine/([a-z0-9-]+)" (slurp %))) files))]
        (is (= #{"stream" "newborn-id-tag"} used)
            (str "this component uses sim-engine vars beyond the stream-partition"
                 " surface: " used))))
    (testing "and NO sim-engine namespace requires this component -- the edge is
              structural, one-way, not a discipline"
      (let [engine-src (->> (file-seq (io/file "components/sim-engine/src"))
                            (filter #(.isFile %))
                            (map #(.getPath %))
                            (filter #(str/ends-with? % ".clj")))
            back-edges (filter #(str/includes? (slurp %) "person-simulator") engine-src)]
        (is (seq engine-src))
        (is (empty? back-edges)
            (str "sim-engine now names person-simulator in " (vec back-edges)
                 " -- a feedback edge v1 forbids"))))))

;; --- row 11: every pregnancy reaches a delivery ---------------------------

(deftest pregnancy-and-delivery-are-in-bijection-test
  (let [ps (group-by :person-id (fx/of-kind :pregnancy))
        ds (group-by :person-id (fx/of-kind :delivery))]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq ps) "the witness stream carries no :pregnancy at all"))
    (testing "per person, the counts are equal"
      (let [bad (for [pid (distinct (concat (keys ps) (keys ds)))
                      :let [np (count (get ps pid)) nd (count (get ds pid))]
                      :when (not= np nd)]
                  (str pid ": " np " pregnancies, " nd " deliveries"))]
        (is (empty? bad)
            (str (count bad) " person(s) whose pregnancies and deliveries are not in"
                 " bijection -- a loss, termination or non-delivery outcome has been"
                 " added without a row: " (vec bad)))))
    (testing "and each delivery's :pregnancy-event-id is distinct"
      (let [ids (map :pregnancy-event-id (fx/of-kind :delivery))]
        (is (= (count ids) (count (distinct ids))))
        (is (every? some? ids))))))

;; --- row 12: a parent may head more than one household --------------------

(deftest a-parent-may-head-more-than-one-household-test
  ;; Row 12 is a v1 ARTEFACT declined on purpose, not a law, so this
  ;; gate asserts the artefact is STILL THERE: red the day it is fixed,
  ;; which is the day the row should be struck. The births pass runs
  ;; after the walk that decides household transitions and cannot be
  ;; seen by it, so a parent with no household at their delivery gets
  ;; one constituted BY the birth and may form a second on their own
  ;; hazard later.
  (let [forms (fx/of-kind :household-form)
        by-head (group-by :head-person-id forms)
        multi (into (sorted-map) (filter (fn [[_ v]] (> (count v) 1)) by-head))]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq forms) "the witness stream carries no :household-form at all")
      (is (pos? (count multi))
          (str "no parent heads more than one household -- either the artefact was"
               " fixed (strike row 12) or the witness went empty, and this gate"
               " must not pass by going empty")))
    (testing "the witness count is pinned, counted from the filter's own input"
      ;; RE-PINNED 4 -> 3 at arc 3a's green: the nineteenth variate
      ;; reshuffled every person's second year onward, and one of the
      ;; four parents no longer forms a second household. Still `pos?`,
      ;; so the artefact this row asserts is still there.
      (is (= 3 (count multi))
          (str "expected 3 multi-household heads out of " (count by-head)
               " distinct heads over " (count forms) " :household-form events, read "
               (count multi) ": " (vec (keys multi)))))
    (testing "and every extra household is the one the BIRTH constituted"
      (let [bad (for [[head hs] multi
                      :when (not= 1 (count (filter #(str/ends-with? (:household-id %) "-birth") hs)))]
                  (str head ": " (mapv :household-id hs)))]
        (is (empty? bad)
            (str (count bad) " multi-household head(s) whose second household is NOT"
                 " birth-constituted -- that is a different artefact from row 12's: "
                 (vec bad)))))))

;; --- row 13: a household never loses its housing --------------------------

(deftest only-household-less-persons-become-unhoused-test
  ;; Row 13 (ADR-0173 section 2(b)). The residence sum is a PERSON's
  ;; state, and a household is this component's address-correlation
  ;; device: coupling them is what keeps ruling B1's propagation pass
  ;; honest, because a head's move is copied to every member verbatim
  ;; and a copy that said "housing gained" to a member who never lost
  ;; it would report a change that did not happen.
  ;;
  ;; So the walk mints a `:residence-loss` only for a person in NO
  ;; household, and suppresses the household hazard entirely while a
  ;; person is unhoused. The one way a household member is unhoused is
  ;; ruling A1's newborn, delivered to a parent who was unhoused at
  ;; that instant -- and that household is deliberately kept off the
  ;; join roster so nobody housed can join it.
  (let [evs (fx/evs)
        by-person (group-by :person-id evs)
        residence (fn [pid] (->> (get by-person pid)
                                 (filter #(#{:residence-move :residence-loss} (:event %)))
                                 (sort-by (juxt :t :event-id))
                                 vec))
        unhoused-at? (fn [pid t]
                       (= :residence-loss
                          (:event (last (filter #(<= (:t %) t) (residence pid))))))
        ;; [person-id household-id join-t leave-t-or-nil] for every membership,
        ;; read off the events exactly as the propagation pass reads them
        ;; the WALK's own household transitions. A household constituted BY a
        ;; birth is row 12's artefact and not this row's: the births pass runs
        ;; after the walk and cannot see the parent's housing, which is exactly
        ;; why rule 4 keeps such a household off the join roster instead.
        memberships (remove #(str/ends-with? (str (:household-id %)) "-birth")
                            (concat
                             (for [e evs :when (= :household-form (:event e))]
                               {:person-id (:head-person-id e) :t (:t e)
                                :household-id (:household-id e)})
                             (for [e evs :when (= :household-join (:event e))]
                               {:person-id (:person-id e) :t (:t e)
                                :household-id (:household-id e)})))
        newborns (set (map :newborn-person-id (fx/of-kind :delivery)))]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq (fx/of-kind :residence-loss))
          "the witness stream carries no :residence-loss at all")
      (is (seq memberships) "the witness stream carries no household membership"))
    (testing "nobody forms or joins a household while unhoused"
      (let [bad (remove #(or (newborns (:person-id %))
                             (not (unhoused-at? (:person-id %) (:t %))))
                        memberships)]
        (is (empty? bad)
            (str (count bad) " household transition(s) by a person unhoused at that"
                 " instant: " (vec (take 3 bad))))))
    (testing "and no person in a household ever loses housing -- a newborn born
              into an unhoused household is the one exception, and it is ruling
              A1's, not this row's"
      (let [in-household-at (fn [pid t]
                              (some #(and (= pid (:person-id %)) (<= (:t %) t))
                                    memberships))
            bad (for [l (fx/of-kind :residence-loss)
                      :when (and (not (:at-t0 l))
                                 (not (newborns (:person-id l)))
                                 (in-household-at (:person-id l) (:t l)))]
                  (select-keys l [:person-id :event-id :t]))]
        (is (empty? bad)
            (str (count bad) " :residence-loss event(s) for a person already in a"
                 " household: " (vec (take 3 bad))))))))
