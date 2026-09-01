(ns ehrt.sim-engine.encounters-test
  "ADR-0174 section 2(a), arc 3b sweep 1: the ENCOUNTER HORIZON, lifted.

  What this namespace gates, in the ADR's own order:

  * the id (`encounter-id-for`, ruling B1) -- a pure function of seed x
    arrival ordinal x encounter ordinal, off every RNG stream;
  * the fold (`:encounter`/`:encounters` on `PatientState`) -- and that
    the seven projection fields it does NOT move keep their shape and
    their value while an encounter is open;
  * the guard (`admission-only-when-no-open-encounter`'s runtime half) --
    a repeat arrival with no open encounter opens a SECOND one, and one
    landing while the first is still open opens nothing;
  * THE OPT-IN LAW: with no `:encounters` key the whole log is
    byte-identical, which is what makes this sweep's dark commit
    provable rather than merely believed.

  The fixture is `ehrt.sim-engine.persons-test`'s own -- seed 15, four
  arrivals, a ONE-PERSON pool so every arrival binds to the same person
  and three of the four are repeats -- rebuilt here rather than shared,
  because a test namespace requiring another test namespace makes two
  gates one gate. Its arrival instants are asserted, not assumed."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-check.check :as check]
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
  "ONE person, so every one of the four arrivals binds to them and three
  of the four are REPEATS -- the population this sweep exists for."
  {:population [{:person-id "q-a" :id-tag 1}]
   :personas {"q-a" (sim-model/persona (streams/stream seed :person 1) {})}
   :alive {}
   :events []})

(def ^:private brief-pathway
  "Admit, stay thirty minutes, discharge. The stay is what makes the
  fourth arrival's own guard case real: arrivals land at 0, 4620, 8160
  and 9900, and the third encounter is still open at 9900."
  {:name "brief" :steps [{:type :admission :location "Renal"}
                         {:type :delay :from 30 :to 30}
                         {:type :discharge}]})

(defn- base [pathway events]
  {:seed seed :patients 4 :arrival-gap 100 :facility facility
   :pathway pathway
   :persons (assoc pool :events (vec events))})

(defn- of-kind [gt k] (filterv #(= k (:event %)) gt))
(defn- ids [gt] (vec (distinct (keep :encounter-id gt))))

;; --- ruling B1: the id ---------------------------------------------------

(deftest encounter-id-is-a-pure-function-of-seed-ordinal-and-encounter-ordinal
  (testing "the derivation is `patient-id-for`'s, one level down, and is
            spelled out here rather than trusted -- a change to it is a
            change to every visit number every consumer has persisted"
    (is (= (format "ENC-%06d-%02d-%08x" 7 2
                   (bit-and (streams/mix64 (streams/mix64 seed 7) 2) 0xffffffff))
           (streams/encounter-id-for seed 7 2))))
  (testing "distinct across every axis, and stable"
    (is (= (streams/encounter-id-for seed 7 2) (streams/encounter-id-for seed 7 2)))
    (is (not= (streams/encounter-id-for seed 7 2) (streams/encounter-id-for seed 7 3)))
    (is (not= (streams/encounter-id-for seed 7 2) (streams/encounter-id-for seed 8 2)))
    (is (not= (streams/encounter-id-for seed 7 2) (streams/encounter-id-for (inc seed) 7 2))))
  (testing "the prefix is its own id space -- never confusable with a
            patient-id or an MRN"
    (is (re-matches #"ENC-\d{6}-\d{2}-[0-9a-f]{8}" (streams/encounter-id-for seed 7 2)))))

(deftest minting-draws-nothing
  (testing "ruling B1's whole reason: identity generation adds no draw
            for sim/ADR-0009's accounting to track, so a run with
            `:encounters` on produces the SAME events at the SAME
            instants when no second encounter is available to open"
    (let [cfg {:seed 42 :patients 6}
          dark (:ground-truth (run/run cfg))
          on (:ground-truth (run/run (assoc cfg :encounters true)))]
      (is (= (count dark) (count on)))
      (is (= (pr-str dark) (pr-str (mapv #(dissoc % :encounter-id) on)))
          "with no repeat arrival to open a second encounter, the stamp is
           the ONLY difference -- not one instant, allocation or draw moved"))))

;; --- THE OPT-IN LAW ------------------------------------------------------

(deftest absent-encounters-is-byte-for-byte-todays-run
  (testing "ADR-0174's opt-in law, the same one `:persons` and
            `:churn-profile` already establish: ABSENT means today, not
            `false`, not nil"
    (let [r (run/run (base brief-pathway []))
          gt (:ground-truth r)]
      (is (not-any? #(contains? % :encounter-id) gt)
          "no `:encounter-id` is minted anywhere without the opt-in")
      (is (= 1 (count (of-kind gt :admission)))
          "the wall stands: three of the four arrivals are repeats and queue nothing")
      (is (= :ok (:status (check/check-all gt facility)))))))

(deftest the-encounter-is-folded-even-without-the-opt-in
  (testing "the records cost no emitted byte and are folded all the same
            -- which is what keeps `admission-only-when-no-open-
            encounter` a real predicate on a corpus generated before
            this sweep existed, rather than a vacuously-true one"
    (let [r (run/run (base brief-pathway []))
          pid (streams/patient-id-for seed 0)
          final (last (get (:state-history r) pid))]
      (is (nil? (:encounter final)) "closed by the discharge")
      (is (= 1 (count (:encounters final))))
      (is (nil? (:encounter-id (first (:encounters final))))
          "folded, but never minted -- the id is what the opt-in buys"))))

;; --- the fold: two fields added, seven left exactly where they were ------

(deftest the-open-encounter-is-a-thin-record-and-the-projection-stays-put
  (let [r (run/run (assoc (base brief-pathway []) :encounters true))
        pid (streams/patient-id-for seed 0)
        history (get (:state-history r) pid)
        admitted (first (filter #(= :admitted (:status %)) history))]
    (testing "the OPEN record carries id, ordinal and the opener's instant"
      (is (= #{:encounter-id :ordinal :admitted-at} (set (keys (:encounter admitted))))))
    (testing "and the seven single-encounter-assumed fields are unchanged
              in shape and in value while it is open -- ADR-0174 section
              2(a) item 2, which is why no reader in the emitters, the
              checks or the board had to move"
      (is (= :admitted (:status admitted)))
      (is (= :inpatient (:class admitted)))
      (is (= "Renal" (:home-ward admitted)))
      (is (= #{:ward :bed :placement} (set (keys (:location admitted)))))
      (is (string? (:attending admitted)))
      (is (= (:admitted-at (:encounter admitted)) (:admitted-at admitted)))
      (is (nil? (:discharged-at admitted))))
    (testing "and the CLOSED record is that projection, snapshot as the
              discharge leaves it"
      (let [closed (first (:encounters (last history)))]
        (is (= :discharged (:status closed)))
        (is (nil? (:location closed)) "a discharged encounter vacated its bed")
        (is (= (:admitted-at admitted) (:admitted-at closed)))
        (is (int? (:discharged-at closed)))
        (is (= 0 (:ordinal closed)))))))

(deftest an-expired-patients-encounter-stays-open
  (testing "ADR-0174 section 2(a) item 4: the `:expired` arm is
            untouched, because the body stays in the bed -- which is
            exactly what `expired-patient-retains-location` asserts"
    (let [r (run/run (assoc (base {:name "dies"
                                      :steps [{:type :admission :location "Renal"}
                                              {:type :discharge :disposition :expired}]}
                                     [])
                               :encounters true))
          pid (streams/patient-id-for seed 0)
          final (last (get (:state-history r) pid))]
      (is (= :expired (:status final)))
      (is (some? (:encounter final)) "still open")
      (is (empty? (:encounters final)))
      (is (some? (:location final))))))

;; --- the horizon, lifted -------------------------------------------------

(deftest a-repeat-arrival-with-no-open-encounter-opens-a-second-one
  (testing "the INVERSE of ADR-0173's own first tabled deviation, which
            `repeat-arrivals-resolve-and-queue-nothing-without-the-
            encounters-opt-in-test` still gates
            on the absent path"
    (let [gt (:ground-truth (run/run (assoc (base brief-pathway []) :encounters true)))]
      (is (pos? (count (of-kind gt :admission))))
      (is (= 3 (count (of-kind gt :admission)))
          "arrivals at 0, 4620 and 8160 each open one; the fourth is the
           guard's own case, below")
      (is (= 3 (count (of-kind gt :discharge))))
      (is (= 3 (count (ids gt))) "three encounters, three distinct ids")
      (is (= [(streams/encounter-id-for seed 0 0)
              (streams/encounter-id-for seed 0 1)
              (streams/encounter-id-for seed 0 2)]
             (ids gt))
          "and the ordinals count that patient's own encounters, in order")
      (is (= :ok (:status (check/check-all gt facility)))))))

(deftest a-repeat-arrival-landing-inside-an-open-encounter-opens-nothing
  (testing "the half of the old rule that survives. The fourth arrival
            lands at 9900, sixty seconds before the third encounter's
            own discharge at 9960 -- so its WHOLE step list is dropped,
            not just its admission, because a delay and a discharge
            queued behind an admission that did not happen would close
            the wrong encounter"
    (let [gt (:ground-truth (run/run (assoc (base brief-pathway []) :encounters true)))]
      (is (= 3 (count (of-kind gt :admission))) "four arrivals, three encounters")
      (is (= 3 (count (of-kind gt :discharge)))
          "and no orphan discharge -- the whole arrival is prepended or none of it is"))))

(deftest a-second-encounter-mints-no-second-registration
  (testing "ADR-0174 section 2(a): this is what makes the design an
            ENCOUNTER design rather than a duplicate-patient one.
            `registered-is-every-patients-first-event` is UNCHANGED and
            is asserted here to still hold"
    (let [gt (:ground-truth (run/run (assoc (base brief-pathway []) :encounters true)))]
      (is (= 1 (count (of-kind gt :registered))) "one person, one patient, one registration")
      (is (empty? (check/registered-is-every-patients-first-event gt)))
      (is (= 1 (count (distinct (map (comp :patient-id first :participants) gt))))
          "and one patient-id across the whole log -- a returning person
           resolves to the patient they already are, keeping their MRN")
      (is (= 1 (count (distinct (keep :active-mrn gt))))))))

(deftest a-hook-encounter-on-an-already-discharged-patient-opens-a-second-one
  (testing "the runtime guard `decide :person-encounter` carries is the
            SAME predicate, so the two clinical hooks inherit the lift:
            an occupational injury landing after this patient's earlier
            injury closed used to mint nothing"
    (let [injuries [{:event :occupational-injury :person-id "q-a" :t 20000
                     :event-id "q-a#1" :injury-class :strain}
                    {:event :occupational-injury :person-id "q-a" :t 200000
                     :event-id "q-a#2" :injury-class :burn}]
          cfg (base {:name "empty" :steps []} injuries)
          dark (:ground-truth (run/run cfg))
          on (:ground-truth (run/run (assoc cfg :encounters true)))]
      (is (= 1 (count (of-kind dark :admission))) "the second hook hit the wall")
      (is (= 2 (count (of-kind on :admission))))
      (is (= [(streams/encounter-id-for seed 0 0) (streams/encounter-id-for seed 0 1)]
             (ids on))
          "and BOTH openers mint -- a hook-driven encounter is an encounter")
      (is (= 1 (count (of-kind on :registered))))
      (is (= :ok (:status (check/check-all on facility)))))))

;; --- the stamp -----------------------------------------------------------

(deftest every-event-of-an-encounter-carries-its-id-and-nothing-else-does
  (let [gt (:ground-truth (run/run (assoc (base brief-pathway []) :encounters true)))
        by-kind (group-by :event gt)]
    (testing "an opener mints, a closer carries the encounter it closes"
      (is (every? :encounter-id (:admission by-kind)))
      (is (every? :encounter-id (:discharge by-kind)))
      (is (= (mapv :encounter-id (:admission by-kind))
             (mapv :encounter-id (:discharge by-kind)))
          "paired, in order"))
    (testing "and `:registered` carries none -- it is always a patient's
              FIRST event, before any encounter exists"
      (is (not-any? #(contains? % :encounter-id) (:registered by-kind))))
    (testing "each id appears on exactly one opener and one closer"
      (is (empty? (check/every-encounter-is-opened-and-closed-or-still-open gt))))))

;; --- the whole catalog, over the lifted corpus ---------------------------

(deftest the-lifted-corpus-satisfies-the-whole-invariant-catalog
  (testing "not one row of the catalog goes red on a corpus carrying
            three encounters for one patient -- which is the claim the
            eight per-encounter rewrites exist to make good"
    (doseq [[label cfg] [["brief pathway, three encounters" (base brief-pathway [])]
                         ["two hook encounters"
                          (base {:name "empty" :steps []}
                                [{:event :occupational-injury :person-id "q-a" :t 20000
                                  :event-id "q-a#1" :injury-class :strain}
                                 {:event :occupational-injury :person-id "q-a" :t 200000
                                  :event-id "q-a#2" :injury-class :burn}])]]]
      (let [r (check/check-all (:ground-truth (run/run (assoc cfg :encounters true))) facility)]
        (is (= :ok (:status r)) (str label ": " (pr-str (:violations (:payload r)))))))))
