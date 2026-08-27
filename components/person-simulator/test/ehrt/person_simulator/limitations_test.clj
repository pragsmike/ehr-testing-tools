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
      ;; 23 since contract 1.3.0 (ADR-0173, arc 3a part 3): the fold's
      ;; own `:demographic-update` and `:coverage-change`; 24 since 1.6.0
      ;; (ADR-0174 section 2(c), arc 3b sweep 2): the bed cycle's own
      ;; `:bed-status-change`.
      (is (= 24 (count engine-kinds))
          (str "expected the CLOSED 24-kind engine vocabulary, parsed " (count engine-kinds))))
    (testing "the witness stream carries deaths at all"
      (is (seq (fx/of-kind :person-death))))
    ;; REWRITTEN 2026-08-26 (arc 3a part 3). This assertion used to be
    ;; NAME-disjointness -- no person-event kind may share a name with a
    ;; ground-truth kind -- and it went RED on `:coverage-change`, which
    ;; ADR-0173 section 2(b)'s own fold table names on BOTH sides
    ;; deliberately. The name is shared by design; what row 4 actually
    ;; says is not about names.
    ;;
    ;; It says a person event is never ITSELF a log event, and the
    ;; structural fact behind that is the one now asserted: a person
    ;; event carries no `:patient-id` and no log-shaped `:participants`
    ;; (the engine's are `{:patient-id .. :role ..}` maps; this
    ;; component's are bare person-id strings), so it could not satisfy
    ;; `every-event-has-participants` or `participant-ids-exist-in-run`
    ;; without inventing a second participant vocabulary -- ADR-0173
    ;; section 2(b)'s own words. The engine mints `:coverage-change`
    ;; FROM a person event of the same name; the two are different
    ;; events, and the row is about the second one not being the first.
    (testing "no person event is ITSELF a ground-truth event -- a :person-death
              cannot become a :discharge with :disposition :expired, because
              nothing this component emits has a patient in it at all"
      (is (empty? (filter :patient-id (fx/evs)))
          "a person event carries a :patient-id -- it is claiming to be a log event")
      (is (empty? (remove #(every? string? (:participants %)) (fx/evs)))
          "a person event carries log-shaped :participants -- the engine's are
           {:patient-id .. :role ..} maps and this component's are bare person ids")
      (is (empty? (filter :active-mrn (fx/evs)))
          "a person event carries an :active-mrn -- only a log event has one"))
    (testing "and the kind names the two vocabularies share are exactly the ones
              ADR-0173 section 2(b) designed them to share, no more"
      (is (= #{:coverage-change} (set/intersection engine-kinds person-kinds))
          (str "the shared-name set moved: " (set/intersection engine-kinds person-kinds)
               " -- a NEW overlap is a person kind that has quietly become an"
               " engine kind, which is what row 4 forbids")))
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

;; --- row 6: STRUCK 2026-08-26 (arc 3a part 3, ADR-0173) -------------------
;;
;; "Demographics reach the wire through ONE per-run lookup keyed by
;; patient-id alone; until arc 3 re-keys it, a delta folded onto patient
;; state is invisible to every message." The fold landed, and the row's
;; substance is now FALSE BY DESIGN: `emit_hl7.clj`'s
;; `demographics-timeline` folds `:demographic-update` and
;; `:coverage-change` into a t-ascending per-patient timeline, and
;; `demographics-at` answers state-at-t. Its gate,
;; `personas-are-keyed-by-patient-id-alone-test`, is DELETED with it
;; rather than repaired: a gate whose limitation no longer exists can
;; only ever assert something untrue or something vacuous. The row is
;; gone from both tables and ADR-0172 section 4 says so where it stood.
;;
;; What replaced it as a gate is
;; `demographics-at-answers-state-at-t-test` (sim-emit-hl7), which is
;; the positive law rather than the negative one.

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
      ;; POSITION-MATCHED, both halves, since arc 3a part 3. This half used
      ;; to be a bare `str/includes?` over the whole of sim-engine's src, so
      ;; the same PROSE the forward half deliberately protects ("a docstring
      ;; naming `engine/stream-seed` in prose is a citation, not a
      ;; dependency") this half forbade -- and it went red twice in part 2 on
      ;; a docstring and a comment, neither a `:require` and neither a call.
      ;; A dependency is a REQUIRE or a fully-qualified CALL; nothing else
      ;; is, and the gate now says exactly that.
      (let [engine-src (->> (file-seq (io/file "components/sim-engine/src"))
                            (filter #(.isFile %))
                            (map #(str/replace (.getPath %) "\\" "/"))
                            (filter #(str/ends-with? % ".clj")))
            sources (into {} (map (juxt identity slurp)) engine-src)
            ;; require position: `[ehrt.person-simulator... :as x]` or
            ;; `[ehrt.person-simulator...]`, the same `:require`-vector scan
            ;; the forward half already runs, read the other way round.
            required (for [[f src] sources
                           ns- (map second (re-seq #"\[(ehrt\.[a-z0-9.-]+)[\s\]]" src))
                           :when (str/starts-with? ns- "ehrt.person-simulator")]
                       (str f ": (:require " ns- ")"))
            ;; call position: a fully-qualified `(ehrt.person-simulator.../x`
            ;; needs no alias and no require form, so the require scan alone
            ;; would miss it.
            called (for [[f src] sources
                         v (map second (re-seq #"\((ehrt\.person-simulator[a-z0-9.-]*)/" src))]
                     (str f ": (" v "/...)"))
            back-edges (vec (concat required called))]
        (is (seq engine-src))
        (is (seq sources) "no sim-engine src file was read (R-empty-population-is-red)")
        (is (empty? back-edges)
            (str "sim-engine now DEPENDS on this component at " back-edges
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
