(ns ehrt.person-simulator.invariants-test
  "ADR-0172 section 3: the invariant shape every person event must
  satisfy, taken verbatim from `ehrt.sim-check.check`'s own
  referential family (`medication-end-references-existing-order-and-
  follows-it-in-time` and its care-plan twin, ADR-0163/0166) and its
  three parts --

  1. the referent EXISTS and is the RIGHT KIND;
  2. SAME SUBJECT: the target's participants include this event's
     subject (a household event names more than one person and every
     one of them is a participant, the shape `:bed-swap` and `:merge`
     already use);
  3. FOLLOWS IN TIME: `(> (:t target) (:t event))` is a violation.

  And the fourth part that matters more here than anywhere: the
  PRE-HORIZON ESCAPE. An `:identity-correction` with no
  `:corrects-event-id` corrects the t0 persona, which is definitionally
  prior to every event in the log. The reference is ABSENT, not
  dangling -- the same distinction, with the same justification,
  `check.clj:690` already gates twice.

  Arc 3's check obligations are one invariant per referential field in
  the table below, each a rename of an existing function body. This
  namespace is where the shape is proved before that rename."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.person-simulator.fixture :as fx]
            [ehrt.sim-model.interface :as sim-model]))

(def reference-kinds
  "ADR-0172 section 2's `:reference` column, as data: the referential
  field -> the kind its target must be."
  {:household-move-event-id :residence-move
   :employment-event-id     :employment-change
   :corrects-event-id       :identity-correction
   :household-event-id      :household-form
   :pregnancy-event-id      :pregnancy
   :unavailable-event-id    :identity-unavailable
   :delivery-event-id       :delivery})

(defn- references
  "Every [event field target-id] triple in the stream."
  []
  (for [e (fx/evs)
        [field _] reference-kinds
        :when (contains? e field)]
    [e field (get e field)]))

(deftest every-event-kind-carries-at-most-one-referential-field-test
  (testing "ADR-0172 section 3's consequence: every kind is either
            referent-free or carries EXACTLY ONE referential field, so
            arc 3's obligations are one invariant per field and not a
            graph"
    (let [bad (for [e (fx/evs)
                    :let [n (count (filter #(contains? e %) (keys reference-kinds)))]
                    :when (> n 1)]
                (select-keys e [:event :event-id]))]
      (is (empty? bad)
          (str (count bad) " event(s) carry more than one referential field: "
               (vec (take 5 bad)))))))

(deftest every-reference-resolves-to-an-event-of-the-right-kind-test
  (let [refs (references)]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq refs) "no event in the witness stream references anything at all")
      (is (= (set (keys reference-kinds)) (set (map second refs)))
          (str "not every referential field is exercised by the witness stream; "
               "missing: " (remove (set (map second refs)) (keys reference-kinds)))))
    (let [index @fx/by-id
          bad (for [[e field target-id] refs
                    :let [t (get index target-id)]
                    :when (or (nil? t) (not= (get reference-kinds field) (:event t)))]
                (str (:event-id e) " " field " -> " target-id
                     (if t (str " which is a " (:event t)) " which does not exist")))]
      (is (empty? bad)
          (str (count bad) " dangling or wrong-kind reference(s): " (vec (take 5 bad)))))))

(deftest every-reference-names-the-same-subject-test
  (let [index @fx/by-id
        bad (for [[e _ target-id] (references)
                  :let [t (get index target-id)]
                  :when (and t (not (some #{(:person-id e)} (:participants t))))]
              (str (:event-id e) " (" (:person-id e) ") -> " target-id
                   " whose participants are " (:participants t)))]
    (is (empty? bad)
        (str (count bad) " reference(s) whose target does not name this event's own"
             " subject as a participant: " (vec (take 5 bad))))))

(deftest every-reference-follows-its-referent-in-time-test
  (let [index @fx/by-id
        bad (for [[e _ target-id] (references)
                  :let [t (get index target-id)]
                  :when (and t (> (:t t) (:t e)))]
              (str (:event-id e) " at t=" (:t e) " -> " target-id " at t=" (:t t)))]
    (is (empty? bad)
        (str (count bad) " reference(s) pointing FORWARD in time: " (vec (take 5 bad))))))

(deftest an-absent-reference-is-the-pre-horizon-escape-not-a-dangling-one-test
  (let [cs (fx/of-kind :identity-correction)
        first-per-person (into {} (for [[pid es] (group-by :person-id cs)]
                                    [pid (:event-id (first (sort-by :t es)))]))
        rootless (filter #(and (= :name (:field %)) (nil? (:corrects-event-id %))) cs)]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq cs))
      (is (seq rootless)
          "no correction corrects the t0 persona -- the escape would be untested"))
    (testing "a correction with no :corrects-event-id corrects the t0 persona, which
              carries no :t of its own, so the follows-in-time law is satisfied by
              construction -- exactly `check.clj`'s own pre-horizon argument"
      (is (every? #(some? (:prior-value %)) rootless)
          "a rootless correction names no prior value -- then it corrects nothing"))))

(deftest the-stream-is-t-ascending-and-every-event-is-identified-test
  (let [es (fx/evs)]
    (is (seq es))
    (is (apply <= (map :t es)) "`persons` returned a stream that is not t-ascending")
    (is (= (count es) (count (distinct (map :event-id es))))
        "two events share an :event-id -- every reference would be ambiguous")
    (is (every? #(and (:person-id %) (:t %) (:event %) (seq (:participants %))) es))
    (is (every? #(some #{(:person-id %)} (:participants %)) es)
        "an event's own subject is not among its participants")))

(deftest the-fourteen-kinds-are-the-closed-vocabulary-test
  (is (= #{:residence-move :employment-change :coverage-change :identity-correction
           :household-form :household-join :household-leave :pregnancy :delivery
           :occupational-injury :person-death :identity-unavailable
           :identity-resolution :person-registered}
         (set (map :event (fx/evs))))
      "the stream's kinds are not exactly ADR-0172 section 2's fourteen"))

(deftest ruling-g1-mints-dispositions-only-test
  (testing "no placeholder-register, fill-in-place or merge-with-existing is minted
            here: the person stream carries the DISPOSITION and the engine mints the
            wire-visible fact, which is the only shape that keeps person -> engine
            one-way"
    (let [kinds (set (map :event (fx/evs)))]
      (is (not-any? kinds [:placeholder-register :fill-in-place :merge-with-existing]))
      (is (= #{:fill :merge} (set (map :branch (fx/of-kind :identity-resolution))))
          "the resolution branches are not exactly R-mix-4's fork"))))

(deftest every-derived-newborn-persona-is-schema-valid-test
  (let [ps (map :persona (fx/of-kind :person-registered))]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq ps) "no newborn Persona in the witness stream to validate"))
    (testing "ruling A1's DERIVED newborn is a Persona `sim-model` itself accepts
              -- derived is not a licence to invent a shape, and this is the one
              assertion standing between a household-derived field and a
              `:registered` event the engine could not carry"
      (let [bad (remove sim-model/valid-persona? ps)]
        (is (empty? bad)
            (str (count bad) " newborn Persona(s) fail sim-model/Persona: "
                 (vec (take 2 bad))))))
    (testing "and every one of them is age 0 at birth, with a household surname"
      (is (every? #(zero? (:age %)) ps))
      (is (every? #(seq (get-in % [:name :family])) ps)))))
