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
            [ehrt.person-simulator.hazards :as hz]
            [ehrt.person-simulator.interface :as ps]
            [ehrt.person-simulator.process :as process]
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

(deftest the-fifteen-kinds-are-the-closed-vocabulary-test
  (is (= #{:residence-move :residence-loss :employment-change :coverage-change
           :identity-correction
           :household-form :household-join :household-leave :pregnancy :delivery
           :occupational-injury :person-death :identity-unavailable
           :identity-resolution :person-registered}
         (set (map :event (fx/evs))))
      "the stream's kinds are not exactly ADR-0172 section 2's fifteen
       (`:residence-loss` is arc 3a's own, ADR-0173 section 2(b))"))

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

;; --- the residence sum (ADR-0173 section 2(b), the fifteenth kind) --------

(deftest residence-loss-carries-no-address-and-names-the-one-it-lost-test
  (testing "population is non-empty (R-empty-population-is-red)"
    (is (seq (fx/of-kind :residence-loss))
        "the witness stream carries no :residence-loss at all -- the fifteenth
         kind has no witness, and every law below would hold vacuously"))
  (testing "ADR-0173 section 2(b): a places row cannot express \"no residence\",
            so the LOSS carries no :address at all -- which is exactly what keeps
            limitations row 7's gate green verbatim rather than repaired"
    (let [losses (fx/of-kind :residence-loss)]
      (is (not-any? #(contains? % :address) losses)
          (str "a :residence-loss carries an :address: "
               (first (filter #(contains? % :address) losses))))
      (is (every? #(map? (:prior-address %)) losses)
          "a :residence-loss names no :prior-address -- then it reports nothing")))
  (testing "and every :prior-address it names is a places.edn row, the same pool
            row 7 holds every :residence-move to"
    (let [pool (set (map #(select-keys % [:street :city :state :zip]) process/places))
          bad (remove #(pool (:prior-address %)) (fx/of-kind :residence-loss))]
      (is (empty? bad)
          (str (count bad) " :residence-loss event(s) name a :prior-address that is"
               " not a places.edn row: " (vec (take 3 (map :prior-address bad))))))))

(deftest a-move-out-of-unhoused-is-housing-gained-and-names-no-prior-address-test
  (testing "ADR-0173 section 2(b): the return to housing is an ordinary
            `:residence-move` whose `:prior-address` is ABSENT -- absent, not nil,
            because the prior state is nowhere and not a row"
    (let [by-person (group-by :person-id
                              (filter #(#{:residence-move :residence-loss} (:event %))
                                      (fx/evs)))
          ;; every [loss, the next residence event that person had] pair
          gains (for [[_ es] by-person
                      :let [es (vec (sort-by (juxt :t :event-id) es))]
                      [a b] (map vector es (rest es))
                      :when (and (= :residence-loss (:event a))
                                 (= :residence-move (:event b)))]
                  b)]
      (is (seq gains)
          "no person in the witness population ever regained housing -- the
           rehousing hazard has no witness")
      (is (not-any? #(contains? % :prior-address) gains)
          (str (count (filter #(contains? % :prior-address) gains))
               " housing-gained move(s) name a :prior-address"))
      (is (every? #(map? (:address %)) gains)
          "a housing-gained move carries no :address -- then nothing was gained"))))

(deftest a-move-while-housed-still-names-its-prior-address-test
  (testing "the sum's other side: a move BETWEEN two rows is unchanged from arc 2b
            -- it names both, and `:prior-address` is what
            `demographic-update-reports-a-real-change` will read"
    (let [by-person (group-by :person-id
                              (filter #(#{:residence-move :residence-loss} (:event %))
                                      (fx/evs)))
          housed-moves (for [[_ es] by-person
                             :let [es (vec (sort-by (juxt :t :event-id) es))]
                             [i e] (map-indexed vector es)
                             :when (and (= :residence-move (:event e))
                                        (or (zero? i)
                                            (= :residence-move (:event (nth es (dec i))))))]
                         e)]
      (is (seq housed-moves))
      (is (every? #(map? (:prior-address %)) housed-moves)
          (str (count (remove #(map? (:prior-address %)) housed-moves))
               " housed-to-housed move(s) name no :prior-address")))))

(deftest the-t0-unhoused-fraction-is-honoured-at-t0-test
  (testing "ADR-0173 section 2(a): `:unhoused {:t0-fraction ..}` is a t0 STATE, and
            it reaches the stream as a `:residence-loss` at t0 carrying `:at-t0` --
            the engine folds a stream and has nowhere else to read an initial
            condition from"
    (let [t0-losses (filter :at-t0 (fx/of-kind :residence-loss))]
      (is (seq t0-losses) "nobody entered the witness population unhoused")
      (is (every? #(= (:t0 fx/config) (:t %)) t0-losses)
          "an :at-t0 :residence-loss did not land at t0")
      (is (= (count t0-losses) (count (distinct (map :person-id t0-losses))))
          "one person entered the run unhoused twice")
      (is (< (count t0-losses) (count fx/population))
          "every person entered unhoused -- the fraction is not being read")))
  (testing "the fraction is READ, not hardcoded: driven to both extremes it takes
            every person and no person"
    (let [at (fn [cfg] (->> (ps/persons (merge fx/config {:years 1} cfg) fx/stream)
                            (filter :at-t0)
                            count))]
      (is (zero? (at {:unhoused {:t0-fraction 0.0}}))
          "t0-fraction 0.0 still put somebody on the street")
      (is (= (count fx/population) (at {:unhoused {:t0-fraction 1.0}}))
          "t0-fraction 1.0 left somebody housed")
      (testing "and `:unhoused` ABSENT takes the ADR's own default, 0.02 -- not
                zero, because the author statement that binds this arc is about
                the population and not about one config's opt-in"
        (is (= 0.02 process/default-unhoused-t0-fraction))
        (is (= (at {:unhoused {:t0-fraction process/default-unhoused-t0-fraction}})
               (at {:unhoused nil}))
            "an absent :unhoused key did not behave as the default fraction")))))

(deftest a-newborn-of-an-unhoused-household-is-born-unhoused-test
  (testing "a newborn's Persona carries the household's LAST KNOWN address (the
            schema requires one), so a newborn delivered to a parent who is
            unhoused AT THAT INSTANT is corrected at its own birth rather than
            entering the run housed at an address nobody lives at"
    (let [;; the rehousing hazard is pinned OFF for this one law: at its real
          ;; rate every t0-unhoused person is back in a places row long before
          ;; any pregnancy of theirs reaches its delivery, and the law would
          ;; hold by having no subject (`rulings.md#R-empty-population-is-red`).
          evs (with-redefs [hz/rehousing-rate 0.0]
                (ps/persons (assoc fx/config :unhoused {:t0-fraction 1.0}) fx/stream))
          by-person (group-by :person-id evs)
          residence (fn [pid] (->> (get by-person pid)
                                   (filter #(#{:residence-move :residence-loss} (:event %)))
                                   (sort-by (juxt :t :event-id))
                                   vec))
          unhoused-at? (fn [pid t]
                         (= :residence-loss
                            (:event (last (filter #(<= (:t %) t) (residence pid))))))
          deliveries (filter #(= :delivery (:event %)) evs)
          to-unhoused (filter #(unhoused-at? (:person-id %) (:t %)) deliveries)]
      (is (seq deliveries) "no delivery in the all-unhoused run -- vacuous")
      (is (seq to-unhoused)
          "no delivery in the all-unhoused run happened while its parent was still
           unhoused -- everybody had been rehoused first, so this law is vacuous")
      (doseq [d to-unhoused]
        (let [nb (:newborn-person-id d)]
          (is (= :residence-loss (:event (first (residence nb))))
              (str nb "'s first residence event is not a :residence-loss -- it was"
                   " delivered to a parent unhoused at that instant and entered"
                   " the run housed")))))))
