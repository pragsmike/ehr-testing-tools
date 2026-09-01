(ns ehrt.person-simulator.consumption-test
  "Fixed draw consumption, and determinism.

  `rulings.md#R-fixed-draw-consumption`'s spirit in this component's
  terms: **the draw count of a person over N years is a function of N
  alone.** Not of what happened to them -- not of whether they moved,
  married, gave birth or died in year one. This is the same law
  `ehrt.sim-engine.assignment/assign-pathway`, `ehrt.sim-engine.churn/
  roll-gap` and `ehrt.sim-model.persona/persona`'s own 13-draw
  contract already state, and it is what a reshuffle-free arc 3 will
  need: a rate that changes, or a branch that is added, must not shift
  the stream for every person downstream of it.

  The counting instrument redefines `ehrt.sim-engine.interface/stream`
  -- the same mechanism the engine's own stream-locality test uses,
  and the reason `stream` is deliberately left unhinted in
  `streams.clj`. Nothing under `components/sim-engine` is changed by
  it."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set :as set]
            [ehrt.person-simulator.fixture :as fx]
            [ehrt.person-simulator.interface :as ps]
            [ehrt.person-simulator.process :as process]
            [ehrt.sim-engine.interface :as engine]))

(defn counting-random
  "A `java.util.Random` that delegates every draw to `inner` and counts
  it. Only the methods this component and `sim-model/persona` actually
  call are overridden; a draw through any other method would show up
  as a MISSING count, which is the failure direction worth having."
  [^java.util.Random inner counter]
  (proxy [java.util.Random] []
    (nextDouble [] (swap! counter inc) (.nextDouble inner))
    (nextInt
      ([] (swap! counter inc) (.nextInt inner))
      ([n] (swap! counter inc) (.nextInt inner (int n))))))

(defn draws-per-person
  "person-id -> how many variates that person's own `:person` stream
  handed out, over one `persons` call."
  [config stream]
  (let [counters (atom {})
        real engine/stream]
    (with-redefs [engine/stream
                  (fn [master family id-tag]
                    (let [c (atom 0)]
                      (swap! counters assoc id-tag c)
                      (counting-random (real master family id-tag) c)))]
      (ps/persons config stream))
    (into {} (for [[id-tag c] @counters] [id-tag @c]))))

(def sim-model-persona-draws
  "`ehrt.sim-model.persona/persona`'s own fixed consumption, with no
  config-gated demographic weights supplied. Named rather than
  inlined, because when a caller supplies `:race-weights` and friends
  this becomes 16 and the arithmetic below should say so out loud."
  13)

(deftest a-persons-draw-count-is-a-function-of-years-alone-test
  (let [counts (draws-per-person fx/config fx/stream)
        t0-tags (set (map :id-tag fx/population))
        t0-counts (select-keys counts t0-tags)
        expected (+ sim-model-persona-draws
                    process/draws-per-t0-person
                    (* (:years fx/config) process/draws-per-person-year))]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (= (count fx/population) (count t0-counts))
          "not every t0 person drew from their own :person stream"))
    (testing "every t0 person consumes 13 + 1 + 19*24, whatever happened to them --
              the Persona's thirteen, arc 3a's ONE t0 residence variate, and the
              nineteen-variate year block"
      (is (= #{expected} (set (vals t0-counts)))
          (str "t0 draw counts are not uniform: "
               (sort (frequencies (vals t0-counts))) ", expected " expected)))))

(deftest two-persons-with-different-outcomes-consume-identically-test
  (let [counts (draws-per-person fx/config fx/stream)
        t0-ids (set (map :person-id fx/population))
        tag-of (into {} (for [p fx/population] [(:person-id p) (:id-tag p)]))
        profile (into {} (for [[pid es] (group-by :person-id (fx/evs))
                               :when (t0-ids pid)]
                           [pid (frequencies (map :event es))]))
        ;; the loudest life in the witness population, and the shortest one
        busiest (key (apply max-key #(reduce + (vals (val %))) profile))
        died (first (for [e (fx/of-kind :person-death)
                          :when (t0-ids (:person-id e))]
                      (:person-id e)))]
    (testing "the witness stream really does hold two different outcomes
              (R-empty-population-is-red)"
      (is (some? died) "nobody died -- the comparison would be vacuous")
      (is (some? busiest))
      (is (not= died busiest))
      (is (not= (get profile died) (get profile busiest))
          "the two chosen persons had the same outcome after all"))
    (is (= (get counts (tag-of died)) (get counts (tag-of busiest)))
        (str "a person who died (" died ", " (get profile died) ", "
             (get counts (tag-of died)) " draws) and the busiest person in the"
             " population (" busiest ", " (get profile busiest) ", "
             (get counts (tag-of busiest)) " draws) did not consume identically"
             " -- consumption depends on outcome, which is exactly what the"
             " fixed-consumption law forbids"))))

(deftest a-newborn-draws-fewer-than-thirteen-test
  (let [counts (draws-per-person fx/config fx/stream)
        t0-tags (set (map :id-tag fx/population))
        newborn-tags (remove t0-tags (keys counts))
        ;; a newborn born in year y is walked for (years - y - 1) years
        persona-draws (fn [tag] (mod (get counts tag) process/draws-per-person-year))]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq newborn-tags) "no newborn drew from its own stream"))
    (testing "ruling A1: a newborn's Persona is DERIVED from the household, not
              sampled -- four draws (sex, given name, SSN group, SSN serial), and
              deliberately fewer than the thirteen a sampled adult costs. A
              newborn draws no t0 residence variate either: its housing is its
              household's, which is why the modulus below is 4 and not 5"
      (is (= #{4} (set (map persona-draws newborn-tags)))
          (str "newborn persona draw counts: " (sort (distinct (map persona-draws newborn-tags)))))
      (is (< 4 sim-model-persona-draws)))))

(deftest the-same-config-and-seed-give-an-identical-vector-test
  (is (= (ps/persons fx/config fx/stream) (ps/persons fx/config fx/stream)))
  (is (= (ps/persons fx/config fx/stream) (fx/evs))))

(deftest different-person-id-tags-give-disjoint-sequences-test
  (testing "two distinct :person id-tags share no draw -- the partition's whole
            promise, and the reason a birth anywhere in a run perturbs no other
            person's stream"
    (let [seqs (into {} (for [tag [1 2 3 7 61]]
                          [tag (vec (repeatedly 40 #(.nextDouble (engine/stream fx/master :person tag))))]))]
      (doseq [[a sa] seqs [b sb] seqs :when (< a b)]
        (is (empty? (set/intersection (set sa) (set sb)))
            (str "id-tags " a " and " b " share a draw")))))
  (testing "and shifting every id-tag moves the whole stream"
    (let [shifted (assoc fx/config :population
                         (mapv #(update % :id-tag + 1000) fx/population))]
      (is (not= (map (juxt :person-id :event :t) (ps/persons shifted fx/stream))
                (map (juxt :person-id :event :t) (fx/evs)))))))

(deftest every-draw-comes-from-the-person-family-test
  (let [families (atom [])
        real engine/stream]
    (with-redefs [engine/stream (fn [master family id-tag]
                                  (swap! families conj family)
                                  (real master family id-tag))]
      (ps/persons fx/config fx/stream))
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq @families)))
    (is (= #{:person} (set @families))
        (str "this component drew from a stream family other than :person: "
             (set @families)))))

(deftest ruling-c1-a-compiled-death-suppresses-the-person-death-not-the-draw-test
  (let [victim (:person-id (first (fx/of-kind :person-death)))
        instant 1000
        cfg (assoc fx/config :deaths {victim instant})
        with-compiled (ps/persons cfg fx/stream)
        counts-before (draws-per-person fx/config fx/stream)
        counts-after (draws-per-person cfg fx/stream)
        tag (:id-tag (first (filter #(= victim (:person-id %)) fx/population)))]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (some? victim) "nobody died in the witness stream"))
    (testing "the person whose COMPILED trajectory carries a death mints none of
              their own, and their processes stop at that instant"
      (is (empty? (filter #(= victim (:person-id %))
                          (filter #(= :person-death (:event %)) with-compiled))))
      (is (every? #(<= (:t %) instant)
                  (filter #(= victim (:person-id %)) with-compiled))))
    (testing "and the draw is made either way -- fixed consumption does not depend
              on which death wins"
      (is (= (get counts-before tag) (get counts-after tag))))))
