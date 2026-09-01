(ns ehrt.sim-engine.apply-restamp-identity-test
  "THE CO-LANDED INVARIANT OF RULING A1(b): re-stamping an
  ALREADY-STAMPED log is the IDENTITY, which is what makes
  `:encounter-stamp` an inert pair at the two apply sites that replay a
  log rather than build one.

  WHY A GATE AND NOT A MEASUREMENT. The census predicted this pair
  OUTPUT-MOVING at both sites and stage 2's measurement REFUTED that --
  identity on all three of the oracle's encounter-carrying roots,
  `.agents/session-records/2026-09-01-apply-unification-stage-2.md`
  sections 4b and 4c. But the inertness is not a property of the
  concern; it rests on TWO MECHANISMS ELSEWHERE IN THE TREE, and a
  later session could change either without knowing this pair depends
  on it:

  * `encounters/stamp-encounter` guards on `contains?` -- \"a key that
    is there is there\", its own docstring -- so an event stamped
    inbound at site 1 is returned untouched even when the world offers a
    DIFFERENT id;
  * the whole log is ONE BATCH, so the world the decoration reads is the
    PRE-BATCH one, `{:patients {}}`, and an event carrying no id finds
    no patient and is stamped with nothing.

  Each mechanism is gated BY ITS COUNTERFACTUAL below, not merely
  exercised: the guard test offers an id that would be minted if the
  guard went, and the batch test replays a log whose ids have been
  STRIPPED, which would gain them back the moment the world stopped
  being pre-batch. A session that re-batches `replay` per event, or
  relaxes the guard to `some?`, fails here rather than in the corpus --
  where THE ORACLE CANNOT SEE IT AT ALL, `engine/replay` being on
  `ehrt.oracle.digest`'s own unreached list (record section 4a).

  THE FIXTURE IS `ehrt.sim-engine.encounters-test`'s, rebuilt here
  rather than shared, for that namespace's own stated reason: a test
  namespace requiring another test namespace makes two gates one gate.
  Seed 15, a ONE-PERSON pool so all four arrivals bind to the same
  person, a brief admit/stay/discharge pathway -- three encounters, and
  six of the seven events carry an id. Its non-vacuity is asserted
  rather than assumed."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-engine.encounters :as encounters]
            [ehrt.sim-engine.fold :as fold]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.streams :as streams]))

(def ^:private seed 15)

(def ^:private facility
  {:id :encounters-fixture
   :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 8
            :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 4 :surge-slots 2
            :surge-format "%s-H%02d" :class :inpatient}]})

(def ^:private pool
  {:population [{:person-id "q-a" :id-tag 1}]
   :personas {"q-a" (sim-model/persona (streams/stream seed :person 1) {})}
   :alive {}
   :events []})

(def ^:private brief-pathway
  {:name "brief" :steps [{:type :admission :location "Renal"}
                         {:type :delay :from 30 :to 30}
                         {:type :discharge}]})

(def ^:private gt
  "A STAMPED log -- `:encounters` opted in, so site 1 minted ids on the
  way in and this is exactly the shape a replay site is handed."
  (:ground-truth (run/run {:seed seed :patients 4 :arrival-gap 100
                           :facility facility :pathway brief-pathway
                           :persons pool :encounters true})))

(defn- entries-under
  "`replay`'s own fold, at an arbitrary projection -- the accumulator
  shape is `replay`'s, so the only variable is the projection."
  [projection log]
  (persistent!
   (:entries (fold/apply-events {:world {:patients {}}
                                 :entries (transient [])
                                 :log (transient [])}
                                log
                                projection))))

(deftest the-fixture-is-not-vacuous
  (testing "a gate on re-stamping proves nothing over a log with nothing
            stamped -- so the log's own ids are asserted first"
    (is (= 7 (count gt)))
    (is (= {:registered 1 :admission 3 :discharge 3}
           (frequencies (map :event gt))))
    (is (= 6 (count (filter #(contains? % :encounter-id) gt)))
        "six of the seven events carry an id -- three openers and three closers")
    (is (= 3 (count (distinct (keep :encounter-id gt))))
        "three encounters, one repeat-arriving patient")))

(deftest re-stamping-a-stamped-log-is-the-identity
  (testing "site 2 -- ruling A1(b)'s whole claim, at the site it enables.
            `replay-projection` carries `:encounter-stamp`; dropping it
            changes not one entry"
    (is (fold/replay-projection :encounter-stamp)
        "the pair is enabled -- otherwise this deftest gates nothing")
    (is (= (entries-under (disj fold/replay-projection :encounter-stamp) gt)
           (entries-under fold/replay-projection gt))
        "every entry identical, with the decoration and without it")
    (is (= (fold/replay gt)
           (entries-under (disj fold/replay-projection :encounter-stamp) gt))
        "and `replay` itself is that fold, so the claim is about the live path")))

(deftest the-contains-guard-is-mechanism-one
  (testing "an already-stamped event is left alone even when the world
            offers a DIFFERENT id -- the counterfactual is what makes
            this a gate and not an exercise"
    (let [stamped (first (filter #(and (contains? % :encounter-id)
                                       (= :discharge (:event %)))
                                 gt))
          subject (:patient-id (first (:participants stamped)))
          world {:patients {subject {:encounter {:encounter-id "ENC-DIFFERENT"}}}}]
      (is (some? stamped) "the fixture has a stamped closer to re-stamp")
      (is (= stamped (encounters/stamp-encounter world stamped))
          "`contains?`, not `some?` -- a key that is there is there")
      (is (= "ENC-DIFFERENT"
             (:encounter-id (encounters/stamp-encounter world (dissoc stamped :encounter-id))))
          "and THAT WORLD WOULD HAVE MINTED ONE -- so it is the guard
           doing the work here, not an empty world"))))

(deftest the-whole-log-is-one-batch-is-mechanism-two
  (testing "the decoration reads the PRE-BATCH world, which at a replay
            site is `{:patients {}}`, so a log with its ids STRIPPED
            gains none back. A session that re-batches per event, or
            folds the world between batches, fails here"
    (let [stripped (mapv #(dissoc % :encounter-id) gt)]
      (is (= 6 (count (filter #(contains? % :encounter-id) gt)))
          "the strip has something to strip")
      (is (zero? (count (keep :encounter-id (map :event (entries-under fold/replay-projection stripped)))))
          "not one id minted from an unstamped log, though the same
           events minted three on the way in at site 1"))))
