(ns ehrt.sim-check.person-invariants-test
  "ADR-0173 section 2(e), arc 3a part 3: the invariants the demographic
  fold owes, each proved to FIRE on a mutated log and to be clean on a
  real one. SIX at arc 3a; a SEVENTH,
  `no-resolution-after-a-placeholder-is-consumed`, joined them
  2026-08-29 with TS-4's consumed clause
  (`roadmap.md#ts-4-placeholder-unresolved`), because that clause is
  only sound if a consumption really does end the identity question.

  THREE OF THE SIX HAVE NO PRODUCER YET. The identification flow --
  placeholder registrations, the fill, the identification merge -- is arc
  3a part 4's, and its invariants land here anyway. That ordering is the
  point rather than an accident: a gate written after the code it
  constrains is a gate written to AGREE with that code, and ADR-0170's
  own species (a claim true when written that nothing keeps true) is what
  this catalog family exists against. What the three can be shown to do
  today is fire on a hand-built log that breaks them, which is exactly
  the red-by-mutation proof a landed producer would not improve on.

  Every mutated log here is minimal and hand-built. The clean side is a
  REAL `engine/run` with a real fold, so the pair is: this is what a
  violation looks like, and this is a run that has none."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.streams :as streams]
            [ehrt.sim-model.interface :as sim-model]))

;; --- a minimal, hand-built log --------------------------------------------

(def ^:private pid-a "PID-000000-aaaaaaaa")
(def ^:private pid-b "PID-000001-bbbbbbbb")

(def ^:private persona (sim-model/persona (java.util.Random. 7) {}))

(defn- subject [pid] [{:patient-id pid :role :subject}])

(defn- registered
  ([pid mrn] (registered pid mrn {}))
  ([pid mrn extra]
   (merge {:event :registered :t 0 :active-mrn mrn :persona persona
           :participants (subject pid) :warm-up false}
          extra)))

(defn- update-ev [pid t extra]
  (merge {:event :demographic-update :t t :active-mrn "MRN000001"
          :cause :residence-move :field :residence
          :value {:status :housed :address (:address persona)}
          :person-event-id "PERSON-000000#1"
          :participants (subject pid) :warm-up false}
         extra))

(defn- fires?
  "Whether `invariant` reports at least one violation over `log` -- and
  the violations it reports all name itself, so a test cannot pass on
  some OTHER invariant's finding."
  [invariant log]
  (let [vs (vec (invariant log))]
    (and (seq vs) (every? #(= (keyword (:name (meta invariant))) (:invariant %)) vs))))

;; --- the clean side: one real run with a real fold ------------------------

(def ^:private addr-b {:street "2 Fixture Way" :city "Shelbyville" :state "IL" :zip "62565"})

(def ^:private clean-run
  (delay
   (let [pa (sim-model/persona (streams/stream 15 :person 1) {})]
     (run/run
      {:seed 15 :patients 4 :arrival-gap 0
       :pathway {:name "brief" :steps [{:type :admission :location "Renal"}
                                       {:type :discharge}]}
       :facility {:id :check-fixture
                  :wards [{:id :renal :name "Renal" :beds 4 :surge-slots 2
                           :surge-format "%s-H%02d" :class :inpatient}]}
       :persons {:population [{:person-id "p-a" :id-tag 1}]
                 :personas {"p-a" pa}
                 :alive {}
                 :events [{:event :residence-move :person-id "p-a" :t 3600
                           :event-id "p-a#0" :address addr-b :prior-address (:address pa)}
                          {:event :coverage-change :person-id "p-a" :t 7200
                           :event-id "p-a#1" :cause :employment
                           :payer {:id "x" :name "X Health" :type :commercial}
                           :prior-payer (:payer pa)}]}}))))

(deftest the-clean-run-actually-folded-something-test
  ;; R-empty-population-is-red, first: every "no violations" assertion
  ;; below is worth nothing over a run that folded nothing.
  (let [gt (:ground-truth @clean-run)]
    (is (pos? (count (filter #(= :demographic-update (:event %)) gt))))
    (is (pos? (count (filter #(= :coverage-change (:event %)) gt))))))

(deftest the-whole-person-family-is-clean-on-a-real-fold-test
  (doseq [invariant [#'check/identity-fill-references-its-placeholder-registration
                     #'check/identification-merge-survivor-is-the-persons-prior-patient
                     #'check/every-placeholder-registration-is-resolved-or-still-open
                     #'check/no-resolution-after-a-placeholder-is-consumed
                     #'check/demographic-update-reports-a-real-change
                     #'check/no-demographic-event-after-a-patient-expires
                     #'check/person-scoped-provenance-is-a-stamp-not-a-reference]]
    (is (empty? (invariant (:ground-truth @clean-run)))
        (str (:name (meta invariant)) " fired on a clean fold"))))

(deftest the-person-family-is-registered-in-the-catalog-test
  (let [named (set (map (comp :name meta) check/catalog))]
    (doseq [n '[identity-fill-references-its-placeholder-registration
                identification-merge-survivor-is-the-persons-prior-patient
                every-placeholder-registration-is-resolved-or-still-open
                no-resolution-after-a-placeholder-is-consumed
                demographic-update-reports-a-real-change
                no-demographic-event-after-a-patient-expires
                person-scoped-provenance-is-a-stamp-not-a-reference]]
      (is (contains? named n) (str n " is not in `catalog`, so check-all never runs it")))))

;; --- 1: the fill's reference ----------------------------------------------

(deftest identity-fill-references-its-placeholder-registration-test
  (let [placeholder (registered pid-a "MRN000001" {:identity :placeholder})
        fill (fn [extra] (update-ev pid-a 100 (merge {:cause :identity-fill} extra)))]
    (testing "a fill pointing at its own placeholder registration is clean"
      (is (empty? (check/identity-fill-references-its-placeholder-registration
                   [placeholder (fill {:placeholder-event-id 0})]))))
    (testing "and every way of breaking the reference fires"
      (doseq [[label log]
              [["no reference at all" [placeholder (fill {})]]
               ["a reference past the end of the log" [placeholder (fill {:placeholder-event-id 9})]]
               ["a reference to an event of the wrong kind"
                [placeholder (update-ev pid-a 50 {}) (fill {:placeholder-event-id 1})]]
               ["a reference to a registration that is not a placeholder"
                [(registered pid-a "MRN000001") (fill {:placeholder-event-id 0})]]
               ["a reference to ANOTHER patient's placeholder"
                [(registered pid-b "MRN000002" {:identity :placeholder})
                 (fill {:placeholder-event-id 0})]]
               ["a reference that comes AFTER the fill in time"
                [(assoc placeholder :t 500) (fill {:placeholder-event-id 0})]]]]
        (is (fires? #'check/identity-fill-references-its-placeholder-registration log)
            label)))
    (testing "a `:demographic-update` with any OTHER cause is not examined"
      (is (empty? (check/identity-fill-references-its-placeholder-registration
                   [placeholder (update-ev pid-a 100 {})]))))))

;; --- 2: the identification merge ------------------------------------------

(defn- merge-ev [extra]
  (merge {:event :merge :t 200 :surviving-mrn "MRN000002" :merged-mrn "MRN000001"
          :merged-mrns #{"MRN000001"}
          :participants [{:patient-id pid-b :role :survivor}
                         {:patient-id pid-a :role :merged}]
          :warm-up false}
         extra))

(deftest identification-merge-survivor-is-the-persons-prior-patient-test
  (let [ph (registered pid-a "MRN000001" {:identity :placeholder :person-id "p-a"})
        prior (registered pid-b "MRN000002" {:person-id "p-a"})]
    (testing "a well-formed identification merge is clean"
      (is (empty? (check/identification-merge-survivor-is-the-persons-prior-patient
                   [ph prior (merge-ev {:cause :identification})]))))
    (testing "and every way of breaking it fires"
      (doseq [[label log]
              [["the merged patient is not a placeholder"
                [(dissoc ph :identity) prior (merge-ev {:cause :identification})]]
               ["the survivor is a different person"
                [ph (assoc prior :person-id "p-b") (merge-ev {:cause :identification})]]
               ["neither carries a person stamp"
                [(dissoc ph :person-id) (dissoc prior :person-id)
                 (merge-ev {:cause :identification})]]
               ["the merged patient never registered"
                [prior (merge-ev {:cause :identification})]]]]
        (is (fires? #'check/identification-merge-survivor-is-the-persons-prior-patient log)
            label)))
    (testing "a CHURN merge carries no `:cause` and is not examined -- the two
              compose, and this invariant may not narrow churn's own legality"
      (is (empty? (check/identification-merge-survivor-is-the-persons-prior-patient
                   [(dissoc ph :identity) prior (merge-ev {})]))))))

;; --- 3: the placeholder is resolved, or still open ------------------------

(deftest every-placeholder-registration-is-resolved-or-still-open-test
  (let [ph (fn [close-t] (registered pid-a "MRN000001"
                                     (cond-> {:identity :placeholder}
                                       close-t (assoc :window-close-t close-t))))
        later {:event :discharge :t 1000 :active-mrn "MRN000001"
               :participants (subject pid-a) :warm-up false}]
    (testing "resolved by a fill: clean"
      (is (empty? (check/every-placeholder-registration-is-resolved-or-still-open
                   [(ph 100) (update-ev pid-a 100 {:cause :identity-fill
                                                   :placeholder-event-id 0}) later]))))
    (testing "resolved by an identification merge: clean"
      (is (empty? (check/every-placeholder-registration-is-resolved-or-still-open
                   [(ph 100) (registered pid-b "MRN000002")
                    (merge-ev {:cause :identification}) later]))))
    (testing "STILL OPEN -- the run ended before the window's close -- is clean,
              and this is the clause that must never be dropped: a placeholder
              left dangling by a horizon is real traffic"
      (is (empty? (check/every-placeholder-registration-is-resolved-or-still-open
                   [(ph 99999) later]))))
    (testing "a placeholder with no window at all cannot be judged either way"
      (is (empty? (check/every-placeholder-registration-is-resolved-or-still-open
                   [(ph nil) later]))))
    (testing "but a window that CLOSED inside the run with nothing resolving it fires"
      (is (fires? #'check/every-placeholder-registration-is-resolved-or-still-open
                  [(ph 100) later])))
    ;; --- the consumed clause (2026-08-29, TS-4) --------------------------
    ;;
    ;; `roadmap.md#ts-4-placeholder-unresolved`, reduced to the smallest
    ;; log that carries it: a placeholder, an ORDINARY churn merge inside
    ;; its window naming it `:merged`, and nothing else. This block was
    ;; RED before the clause landed -- the merge carries no `:cause`, so
    ;; the pre-2026-08-29 `resolved` set never saw it -- and the fixture
    ;; is deliberately the shape the 10^5 add-on cells produced rather
    ;; than a shape invented to pass.
    (testing "CONSUMED -- an ordinary churn merge absorbs the record inside its
              own window -- is clean: an erroneous merge eating a John Doe is a
              real MPI failure, and the identity question does not survive it"
      (is (empty? (check/every-placeholder-registration-is-resolved-or-still-open
                   [(ph 300) (registered pid-b "MRN000002") (merge-ev {}) later]))))
    (testing "and the clause is CAUSE-blind, not merge-blind: a merge landing
              AFTER the due close does not retroactively excuse a placeholder
              that was already dangling when identification came due"
      (is (fires? #'check/every-placeholder-registration-is-resolved-or-still-open
                  [(ph 100) (registered pid-b "MRN000002") (merge-ev {}) later])))
    (testing "a merge naming the placeholder as SURVIVOR consumes nothing --
              its own record is the one that lives, so its window stays open"
      (is (fires? #'check/every-placeholder-registration-is-resolved-or-still-open
                  [(ph 300) (registered pid-b "MRN000002")
                   (merge-ev {:participants [{:patient-id pid-a :role :survivor}
                                             {:patient-id pid-b :role :merged}]})
                   later])))))

;; --- 3b: and once consumed, it stays consumed (TS-4's other half) ---------

(deftest no-resolution-after-a-placeholder-is-consumed-test
  ;; The second-latent-defect check made permanent. `decide
  ;; :identity-fill` refuses only on the DEMOGRAPHICS still saying
  ;; `:placeholder`, and `evolve :merge` leaves the demographics alone
  ;; while setting `:status :merged` -- so a consumed placeholder still
  ;; looks fillable to that decide, and only the run loop's `:merged`
  ;; short-circuit stops it. Nothing asserted that from outside until
  ;; this gate.
  (let [ph (registered pid-a "MRN000001" {:identity :placeholder :window-close-t 300
                                          :person-id "p-a"})
        prior (registered pid-b "MRN000002" {:person-id "p-a"})
        churn-merge (merge-ev {})                      ; t=200, pid-a :merged
        fill (fn [t] (update-ev pid-a t {:cause :identity-fill :placeholder-event-id 0}))]
    (testing "consumed, and nothing follows: clean"
      (is (empty? (check/no-resolution-after-a-placeholder-is-consumed
                   [ph prior churn-merge]))))
    (testing "a fill BEFORE the consuming merge is an ordinary resolution: clean"
      (is (empty? (check/no-resolution-after-a-placeholder-is-consumed
                   [ph prior (fill 100) churn-merge]))))
    (testing "a fill AFTER it fires -- the log would be claiming to have
              identified somebody whose record no longer exists"
      (is (fires? #'check/no-resolution-after-a-placeholder-is-consumed
                  [ph prior churn-merge (fill 250)])))
    (testing "and so does an identification merge after it"
      (is (fires? #'check/no-resolution-after-a-placeholder-is-consumed
                  [ph prior churn-merge (merge-ev {:cause :identification :t 250})])))
    (testing "a fill at the SAME instant as the merge but LATER in the log fires --
              two events at one `:t` are ordered by the log and by nothing else"
      (is (fires? #'check/no-resolution-after-a-placeholder-is-consumed
                  [ph prior churn-merge (fill 200)])))
    (testing "a merged patient who was never a placeholder is not this
              invariant's business"
      (is (empty? (check/no-resolution-after-a-placeholder-is-consumed
                   [(registered pid-a "MRN000001") prior churn-merge (fill 250)]))))))

;; --- 3c: and the tolerated shape is COUNTED -------------------------------

(deftest placeholder-dispositions-counts-every-class-test
  ;; `consumed-by-churn` is a shape the catalog now TOLERATES, and a
  ;; tolerated shape nothing counts is indistinguishable from one that
  ;; never happens. This is the column that keeps the difference visible;
  ;; the assertions below are what keep the column from going vacuous.
  (let [pid-c "PID-000002-cccccccc"
        pid-d "PID-000003-dddddddd"
        pid-e "PID-000004-eeeeeeee"
        pid-f "PID-000005-ffffffff"
        ph (fn [pid mrn close-t]
             (registered pid mrn (cond-> {:identity :placeholder}
                                   close-t (assoc :window-close-t close-t))))
        log [(ph pid-a "MRN000001" 300)                  ; consumed at t=200
             (ph pid-b "MRN000002" 100)                  ; filled at t=100
             (ph pid-c "MRN000003" 100)                  ; identification-merged
             (ph pid-d "MRN000004" nil)                  ; unjudgeable
             (ph pid-e "MRN000005" 99999)                ; still open
             (ph pid-f "MRN000006" 100)                  ; dangling
             (update-ev pid-b 100 {:cause :identity-fill :placeholder-event-id 1})
             (merge-ev {:cause :identification :t 150
                        :participants [{:patient-id pid-e :role :survivor}
                                       {:patient-id pid-c :role :merged}]})
             (merge-ev {})                               ; churn, t=200, pid-a :merged
             {:event :discharge :t 1000 :active-mrn "MRN000001"
              :participants (subject pid-f) :warm-up false}]
        census (check/placeholder-dispositions log)]
    (testing "every class is exercised, so no column is pinned vacuously
              (R-empty-population-is-red, applied per column)"
      (doseq [k [:resolved-by-fill :resolved-by-identification-merge
                 :consumed-by-churn :unjudgeable :still-open :dangling]]
        (is (pos? (get census k)) (str k " has no witness in this fixture"))))
    (testing "the classes are disjoint and sum to the total"
      (is (= 6 (:total census)))
      (is (= (:total census)
             (reduce + (map census [:resolved-by-fill :resolved-by-identification-merge
                                    :consumed-by-churn :unjudgeable :still-open
                                    :dangling])))))
    (testing "`:dangling` is exactly what the invariant reports"
      (is (= (:dangling census)
             (count (check/every-placeholder-registration-is-resolved-or-still-open log)))))))

;; --- 4: a real change -----------------------------------------------------

(deftest demographic-update-reports-a-real-change-test
  (let [reg (registered pid-a "MRN000001")
        housed {:status :housed :address (:address persona)}
        moved {:status :housed :address addr-b}]
    (testing "a delta whose prior IS the folded state and whose value differs: clean"
      (is (empty? (check/demographic-update-reports-a-real-change
                   [reg (update-ev pid-a 100 {:value moved :prior-value housed})]))))
    (testing "an event that reports NO change fires"
      (is (fires? #'check/demographic-update-reports-a-real-change
                  [reg (update-ev pid-a 100 {:value housed :prior-value housed})])))
    (testing "a prior the fold contradicts fires"
      (is (fires? #'check/demographic-update-reports-a-real-change
                  [reg (update-ev pid-a 100 {:value moved
                                             :prior-value {:status :unknown}})])))
    (testing "a delta carrying no prior at all is not examined for the fold half --
              `:prior-value` is optional in the contract"
      (is (empty? (check/demographic-update-reports-a-real-change
                   [reg (dissoc (update-ev pid-a 100 {:value moved}) :prior-value)]))))
    (testing "`:coverage-change` is the SAME law on `:payer`, and fires the same way"
      (let [cov (fn [extra] (merge {:event :coverage-change :t 100 :active-mrn "MRN000001"
                                    :cause :employment :person-event-id "PERSON-000000#2"
                                    :participants (subject pid-a) :warm-up false}
                                   extra))
            other {:id "x" :name "X" :type :commercial}]
        (is (empty? (check/demographic-update-reports-a-real-change
                     [reg (cov {:payer other :prior-payer (:payer persona)})])))
        (is (fires? #'check/demographic-update-reports-a-real-change
                    [reg (cov {:payer (:payer persona) :prior-payer (:payer persona)})]))
        (is (fires? #'check/demographic-update-reports-a-real-change
                    [reg (cov {:payer other :prior-payer other})]))))))

;; --- 5: nothing after an expiry -------------------------------------------

(deftest no-demographic-event-after-a-patient-expires-test
  (let [reg (registered pid-a "MRN000001")
        admit {:event :admission :t 10 :active-mrn "MRN000001" :attending "1"
               :home-ward "Renal" :location {:ward "Renal" :bed "R1" :placement :licensed}
               :forced false :participants (subject pid-a) :warm-up false}
        expired {:event :discharge :t 20 :active-mrn "MRN000001" :disposition :expired
                 :location {:ward "Renal" :bed "R1" :placement :licensed} :attending "1"
                 :participants (subject pid-a) :warm-up false}]
    (testing "a delta BEFORE the expiry is clean"
      (is (empty? (check/no-demographic-event-after-a-patient-expires
                   [reg admit (update-ev pid-a 15 {:value {:status :unhoused}}) expired]))))
    (testing "and each of the three kinds AFTER it fires"
      (doseq [[label ev]
              [["a demographic update" (update-ev pid-a 30 {:value {:status :unhoused}})]
               ["a coverage change" {:event :coverage-change :t 30 :active-mrn "MRN000001"
                                     :cause :loss :payer {:id "x" :name "X" :type :self-pay}
                                     :person-event-id "PERSON-000000#3"
                                     :participants (subject pid-a) :warm-up false}]
               ["a second registration" (assoc (registered pid-a "MRN000001") :t 30)]]]
        (is (fires? #'check/no-demographic-event-after-a-patient-expires
                    [reg admit expired ev])
            label)))))

;; --- 6: the stamp is not a reference --------------------------------------

(deftest person-scoped-provenance-is-a-stamp-not-a-reference-test
  (let [reg (registered pid-a "MRN000001")]
    (testing "a string stamp is clean, and cannot be `nth`-ed into this log"
      (is (empty? (check/person-scoped-provenance-is-a-stamp-not-a-reference
                   [reg (update-ev pid-a 100 {:person-event-id "PERSON-000000#7"})]))))
    (testing "an INTEGER fires -- the only shape from which an invariant could
              come to resolve it as a log index"
      (is (fires? #'check/person-scoped-provenance-is-a-stamp-not-a-reference
                  [reg (update-ev pid-a 100 {:person-event-id 0})])))
    (testing "and so does anything else that is not a string"
      (is (fires? #'check/person-scoped-provenance-is-a-stamp-not-a-reference
                  [reg (update-ev pid-a 100 {:person-event-id :not-a-string})])))
    (testing "an event carrying no stamp at all is not examined"
      (is (empty? (check/person-scoped-provenance-is-a-stamp-not-a-reference
                   [reg (dissoc (update-ev pid-a 100 {}) :person-event-id)]))))))

;; --- and check-all runs them for real -------------------------------------

(deftest check-all-rejects-a-run-that-breaks-one-of-the-six-test
  (let [gt (:ground-truth @clean-run)
        broken (conj (vec gt)
                     (update-ev (:patient-id (first (:participants (first gt)))) 999999
                                {:person-event-id 0}))]
    (is (result/ok? (check/check-all gt)))
    (let [r (check/check-all broken)]
      (is (result/rejected? r))
      (is (some #(= :person-scoped-provenance-is-a-stamp-not-a-reference (:invariant %))
                (:violations (:payload r)))))))

;; --- arc 3a part 4: the identification family, on a REAL run -------------
;;
;; Three of the six above landed in part 3 with NO PRODUCER, and their
;; own docstrings said so. Part 4 is the producer, so the clean side of
;; each is no longer a run that could not have violated it: it is a run
;; that mints placeholder registrations, fills and identification merges
;; and violates none of them.
;;
;; Every population here is counted before it is judged
;; (`rulings.md#R-empty-population-is-red`): an emptiness claim over a
;; corpus with no placeholders in it is the exact shape repo review 5
;; found twice.

(def ^:private p4-facility
  {:id :check-fixture-p4
   :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 8 :surge-format "%s-H%02d"
            :class :ed}
           {:id :renal :name "Renal" :beds 4 :surge-slots 2 :surge-format "%s-H%02d"
            :class :inpatient}]})

(def ^:private identification-run
  "Four arrivals over a ONE-person pool at seed 15, `:arrival-gap` 100 --
  whose arrival instants are `[0 4620 8160 9900]` -- carrying two
  identity windows and a delivery.

  * Arrival 0 is IDENTIFIED and mints the person's canonical patient.
  * Window one (t 1000 - 6000) covers arrival 1, so that arrival
    registers as a placeholder, AND mints an unidentified ED
    presentation of its own; both resolve `:merge` into arrival 0's
    patient.
  * Window two (t 7000 - 9000) covers arrival 2 and does the same,
    resolving `:fill`.
  * The delivery at t 20000 mints the newborn and admits the parent.
  * Arrival 3 (t 9900) falls outside both windows, so it RESOLVES to
    the canonical patient and queues nothing."
  (delay
   (let [persona-c (sim-model/persona (streams/stream 15 :person 3) {})]
     (run/run
      {:seed 15 :patients 4 :arrival-gap 100
       :pathway {:name "empty" :steps []}
       :facility p4-facility
       :persons
       {:population [{:person-id "p-c" :id-tag 3}]
        :personas {"p-c" persona-c}
        :alive {}
        :events [{:event :identity-unavailable :person-id "p-c" :t 1000 :event-id "p-c#0"
                  :until-t 6000 :alias-name {:family "Doe" :given "Unknown"}}
                 {:event :identity-resolution :person-id "p-c" :t 6000 :event-id "p-c#1"
                  :branch :merge :unavailable-event-id "p-c#0" :surviving-person-id "p-c"}
                 {:event :identity-unavailable :person-id "p-c" :t 7000 :event-id "p-c#2"
                  :until-t 9000 :alias-name {:family "Doe" :given "Unknown"}}
                 {:event :identity-resolution :person-id "p-c" :t 9000 :event-id "p-c#3"
                  :branch :fill :unavailable-event-id "p-c#2"}
                 {:event :delivery :person-id "p-c" :t 20000 :event-id "p-c#4"
                  :newborn-person-id "p-c/b0" :parity-index 0 :within-delivery-index 0
                  :pregnancy-event-id "p-c#x" :participants ["p-c" "p-c/b0"]}
                 {:event :person-registered :person-id "p-c/b0" :t 20000
                  :event-id "p-c/b0#0"
                  :persona (sim-model/persona (streams/stream 15 :person 4) {:age-min 0 :age-max 0})
                  :delivery-event-id "p-c#4" :participants ["p-c/b0" "p-c"]}]}}))))

(deftest the-identification-run-actually-mints-all-three-shapes-test
  (let [gt (:ground-truth @identification-run)]
    (is (pos? (count (filter #(and (= :registered (:event %)) (= :placeholder (:identity %))) gt)))
        "no placeholder registration -- every identification claim below is vacuous")
    (is (pos? (count (filter #(and (= :demographic-update (:event %))
                                   (= :identity-fill (:cause %))) gt)))
        "no fill")
    (is (pos? (count (filter #(and (= :merge (:event %)) (= :identification (:cause %))) gt)))
        "no identification merge")
    (is (pos? (count (filter :mother-patient-id gt)))
        "no newborn, so the delivery hook is untested here")
    (is (pos? (count (filter #(and (= :admission (:event %)) (:person-event-id %)) gt)))
        "no hook-created encounter")))

(deftest the-whole-person-family-is-clean-on-the-identification-run-test
  (doseq [invariant [#'check/identity-fill-references-its-placeholder-registration
                     #'check/identification-merge-survivor-is-the-persons-prior-patient
                     #'check/every-placeholder-registration-is-resolved-or-still-open
                     #'check/no-resolution-after-a-placeholder-is-consumed
                     #'check/demographic-update-reports-a-real-change
                     #'check/no-demographic-event-after-a-patient-expires
                     #'check/person-scoped-provenance-is-a-stamp-not-a-reference]]
    (is (empty? (invariant (:ground-truth @identification-run)))
        (str (:name (meta invariant)) " fired on a real identification run"))))

(deftest the-whole-catalog-is-clean-on-the-identification-run-test
  ;; Not just the person family: the identification merge is churn's own
  ;; `:merge` SHAPE, so `merge-survivor-absorbs-merged-mrns`,
  ;; `no-events-after-merged-terminal` and the whole post-merge shadow
  ;; surface have to be clean over it without a line of change. That is
  ;; the claim ADR-0173 section 2(d) makes, and this is where it is
  ;; checked rather than assumed.
  (is (= :ok (:status (check/check-all (:ground-truth @identification-run) p4-facility 0)))
      (str "violations: "
           (pr-str (:payload (check/check-all (:ground-truth @identification-run)
                                              p4-facility 0))))))

(deftest nothing-is-consumed-on-a-real-identification-run-test
  ;; The other half of `placeholder-dispositions-counts-every-class-test`,
  ;; here because it needs the real fixture: the consumed shape exists in
  ;; the scratch traffic-scale 10^5 cells and in NO shipped corpus. A
  ;; census over 38 oracle roots and the six gated/demo corpora found
  ;; zero, and only two of those 44 have both a placeholder and a churn
  ;; merge at all. Pinned so the day a shipped corpus starts producing
  ;; the shape is a finding rather than a silent tolerance.
  (let [census (check/placeholder-dispositions (:ground-truth @identification-run))]
    (is (pos? (:total census)) "the fixture minted no placeholder at all")
    (is (zero? (:consumed-by-churn census)))
    (is (zero? (:dangling census)))))

(deftest an-unresolvable-placeholder-is-not-a-violation-test
  ;; `every-placeholder-registration-is-resolved-or-still-open`'s escape
  ;; clauses, PRODUCED rather than only mutated into existence. A
  ;; placeholder nobody ever identifies is real traffic -- the
  ;; unidentified patient whose window the feed outlived, or who died
  ;; inside it -- and an invariant that forbade it would be wrong about
  ;; the world rather than about the log.
  ;;
  ;; The ENGINE withholds `:window-close-t` from a window that never
  ;; resolves in the stream it was handed, so what this run exercises is
  ;; the "carries none, cannot be judged" clause; the "close instant
  ;; still in the future" clause is a hand-authored-log shape and is
  ;; exercised by the mutation gates above.
  (let [persona-c (sim-model/persona (streams/stream 15 :person 3) {})
        gt (:ground-truth
            (run/run
             {:seed 15 :patients 4 :arrival-gap 100
              :pathway {:name "empty" :steps []}
              :facility p4-facility
              :persons {:population [{:person-id "p-c" :id-tag 3}]
                        :personas {"p-c" persona-c}
                        :alive {}
                        :events [{:event :identity-unavailable :person-id "p-c" :t 1000
                                  :until-t 9999999 :event-id "p-c#0"
                                  :alias-name {:family "Doe" :given "Unknown"}}]}}))
        ph (filterv #(and (= :registered (:event %)) (= :placeholder (:identity %))) gt)]
    (is (pos? (count ph)) "no placeholder was minted")
    (is (every? #(not (contains? % :window-close-t)) ph)
        "an unresolved window promised a close instant it cannot keep, which is
         what would make this placeholder judgeable and therefore a violation --
         asserted as ABSENCE, the one form that can tell the engine's intent
         from `:window-close-t nil` (ADR-0178)")
    (is (empty? (check/every-placeholder-registration-is-resolved-or-still-open gt))
        "an unresolvable placeholder was reported as dangling")))
