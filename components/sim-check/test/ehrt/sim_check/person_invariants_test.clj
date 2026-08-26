(ns ehrt.sim-check.person-invariants-test
  "ADR-0173 section 2(e), arc 3a part 3: the six invariants the
  demographic fold owes, each proved to FIRE on a mutated log and to be
  clean on a real one.

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
            [ehrt.sim-engine.engine :as engine]
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
   (let [pa (sim-model/persona (engine/stream 15 :person 1) {})]
     (engine/run
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
                     #'check/demographic-update-reports-a-real-change
                     #'check/no-demographic-event-after-a-patient-expires
                     #'check/person-scoped-provenance-is-a-stamp-not-a-reference]]
    (is (empty? (invariant (:ground-truth @clean-run)))
        (str (:name (meta invariant)) " fired on a clean fold"))))

(deftest the-six-are-registered-in-the-catalog-test
  (let [named (set (map (comp :name meta) check/catalog))]
    (doseq [n '[identity-fill-references-its-placeholder-registration
                identification-merge-survivor-is-the-persons-prior-patient
                every-placeholder-registration-is-resolved-or-still-open
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
                  [(ph 100) later])))))

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
