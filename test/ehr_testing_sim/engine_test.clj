(ns ehr-testing-sim.engine-test
  "Determinism and invariants over the engine. The properties here are
  the executable form of the problem statement's Guarantees section:
  same inputs + seed => identical output; every run satisfies the
  invariant catalog. Also: ADR-0008's decide/evolve split -- patient
  state is a fold of the log, proven as a property, plus a pinned-seed
  regression proving the refactor didn't change observable output for
  the v0 step set.

  M2a (ADR-0010): patient-id is the fold/queue key; :mrn moves into
  state as {:mrns :active-mrn}; every event carries :participants.
  M2a (ADR-0011): the engine clock is seconds; :delay's IR stays
  minutes, converted at decide-time; a warm-up window marks early
  events. See ehr-testing-sim.check-test for the invariant-catalog
  side of both."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.check :as check]
            [ehr-testing-sim.result :as result])
  (:import [java.util Random]))

(deftest same-seed-same-output
  (testing "byte-identical reruns"
    (let [config {:seed 42 :patients 5}]
      (is (= (engine/run config) (engine/run config)))
      ;; identical after serialization too -- the guarantee is about
      ;; the artifact, not just the in-memory value
      (is (= (pr-str (engine/run config)) (pr-str (engine/run config)))))))

(deftest different-seed-different-output
  ;; Not guaranteed for ALL seed pairs in principle, but for this
  ;; config the delay sampling makes collision practically impossible;
  ;; a failure here means the seed isn't actually reaching the RNG.
  (is (not= (engine/run {:seed 1 :patients 5})
            (engine/run {:seed 2 :patients 5}))))

(deftest walking-skeleton-shape
  (let [{:keys [ground-truth]} (engine/run {:seed 7 :patients 3})]
    (testing "each patient admits then discharges, in time order"
      (is (= 6 (count ground-truth)))
      (is (= #{"MRN000001" "MRN000002" "MRN000003"}
             (set (map :active-mrn ground-truth))))
      (is (apply <= (map :t ground-truth))))
    (testing "every event names exactly one participant (all M2a event
              types are single-subject -- ADR-0010)"
      (is (every? #(= [:subject] (mapv :role (:participants %))) ground-truth))
      (is (= 3 (count (set (map (comp :patient-id first :participants) ground-truth))))))))

(defspec every-run-satisfies-invariant-catalog 200
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 20)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients})]
      (result/ok? (check/check-all ground-truth)))))

(defspec determinism-holds-for-all-seeds 100
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 10)]
    (= (engine/run {:seed seed :patients patients})
       (engine/run {:seed seed :patients patients}))))

;; --- ADR-0008: the engine is event-sourced ------------------------------

(defspec patient-state-is-a-fold-of-the-log 100
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 15)]
    (let [{:keys [ground-truth state-history]} (engine/run {:seed seed :patients patients})
          by-patient-id (group-by (comp :patient-id first :participants) ground-truth)]
      (every?
       (fn [[patient-id events]]
         (let [mrn (:active-mrn (first events))]
           (= (rest (reductions engine/evolve (engine/initial-patient patient-id mrn) events))
              (get state-history patient-id))))
       by-patient-id))))

;; --- ADR-0010: patient-id, mrns-as-state, participants -------------------

(deftest initial-patient-carries-mrns-set-and-active-mrn
  (let [p (engine/initial-patient "PID-000000" "MRN000001")]
    (is (= "PID-000000" (:patient-id p)))
    (is (= #{"MRN000001"} (:mrns p)))
    (is (= "MRN000001" (:active-mrn p)))
    (is (= :new (:status p)))))

(deftest patient-id-for-is-deterministic-and-ordinal-ordered
  (testing "same seed+ordinal => same id; lexical order matches ordinal
            order (load-bearing for the bed-ready tiebreak)"
    (is (= (engine/patient-id-for 42 3) (engine/patient-id-for 42 3)))
    (is (< (compare (engine/patient-id-for 42 0) (engine/patient-id-for 42 1)) 0))
    (is (< (compare (engine/patient-id-for 42 8) (engine/patient-id-for 42 9)) 0))))

(defspec patient-id-for-differs-by-seed 100
  (prop/for-all [seed gen/large-integer]
    (or (= seed 1) (not= (engine/patient-id-for 1 0) (engine/patient-id-for seed 0)))))

(deftest events-for-patient-collects-only-that-patients-events
  (let [{:keys [ground-truth]} (engine/run {:seed 7 :patients 2})
        ids (distinct (map (comp :patient-id first :participants) ground-truth))
        [id-a id-b] ids]
    (is (every? #(= id-a (:patient-id (first (:participants %))))
                (engine/events-for-patient ground-truth id-a)))
    (is (= (count ground-truth)
           (+ (count (engine/events-for-patient ground-truth id-a))
              (count (engine/events-for-patient ground-truth id-b)))))))

;; --- M1: facility, providers, transfer, bed-ready coupling --------------

(def ^:private crowded-facility
  "One inpatient ward (Renal) with exactly one licensed bed and no
  surge, so a second concurrent admission always boards; one ED ward
  with generous surge (sized comfortably above the property test's
  patient-count ceiling so 'facility exhausted' -- a 5th rung this
  ladder deliberately doesn't have, docs/operational-models.md -- never
  fires and the property stays about the coupling, not total capacity)."
  {:id :crowded-test
   :wards [{:id :ed :name "ED" :beds 0 :surge-slots 20
            :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 1 :surge-slots 0
            :surge-format "%s-H%02d" :class :inpatient}]})

(def ^:private test-providers
  [{:id "1234567893" :name {:family "Chen" :given "A"} :role :attending
    :specialty "Nephrology" :wards [:renal :ed]}])

(deftest bed-ready-transfer-scripted-two-patients
  (testing "B boards in ED surge because Renal's one bed is taken; A's
            discharge frees RENAL-01, which bed-ready-transfers B out
            of boarding -- the cross-patient coupling ADR-0008 exists
            to make possible."
    (let [world0 {:patients {"P1" (engine/initial-patient "P1" "MRN000001")
                              "P2" (engine/initial-patient "P2" "MRN000002")}
                  :facility crowded-facility
                  :providers test-providers}
          rng (Random. 1)
          {a-events :events} (engine/decide rng 0 world0 "P1"
                                             {:type :admission :location "Renal"})
          world1 (update-in world0 [:patients "P1"]
                             #(reduce engine/evolve % a-events))
          {b-events :events} (engine/decide rng 10 world1 "P2"
                                             {:type :admission :location "Renal"})
          world2 (update-in world1 [:patients "P2"]
                             #(reduce engine/evolve % b-events))
          b-after-admission (get-in world2 [:patients "P2"])]
      (testing "A got the one licensed bed"
        (is (= {:ward "Renal" :bed "RENAL-01" :placement :licensed}
               (get-in world1 [:patients "P1" :location]))))
      (testing "B is boarding: home-ward Renal, physically in ED surge"
        (is (= "Renal" (:home-ward b-after-admission)))
        (is (= "ED" (get-in b-after-admission [:location :ward])))
        (is (= :surge (get-in b-after-admission [:location :placement]))))
      (let [{discharge-events :events} (engine/decide rng 100 world2 "P1" {:type :discharge})]
        (testing "A's discharge ALSO emits B's bed-ready transfer, same t"
          (is (= 2 (count discharge-events)))
          (is (= :discharge (:event (first discharge-events))))
          (let [transfer (second discharge-events)]
            (is (= :transfer (:event transfer)))
            (is (true? (:bed-ready transfer)))
            (is (= [{:patient-id "P2" :role :subject}] (:participants transfer)))
            (is (= "MRN000002" (:active-mrn transfer)))
            (is (= 100 (:t transfer)))
            (is (= "Renal" (get-in transfer [:location :ward])))
            (is (= "RENAL-01" (get-in transfer [:location :bed])))
            (let [world3 (-> world2
                             (update-in [:patients "P1"] #(reduce engine/evolve % [(first discharge-events)]))
                             (update-in [:patients "P2"] #(reduce engine/evolve % [transfer])))]
              (testing "B is no longer boarding"
                (is (= "Renal" (get-in world3 [:patients "P2" :home-ward])))
                (is (= "Renal" (get-in world3 [:patients "P2" :location :ward])))))))))))

(defn- boarding?
  [patient]
  (and (= :admitted (:status patient))
       (not= (:home-ward patient) (get-in patient [:location :ward]))))

(defspec bed-ready-transfer-relieves-the-longest-waiting-boarder 150
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 3 10)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients :facility crowded-facility})]
      (every?
       (fn [{:keys [event world-before patient-id]}]
         (or (not= :discharge (:event event))
             (let [vacated-ward (get-in world-before [patient-id :location :ward])
                   waiting (->> world-before
                                (remove (fn [[pid _]] (= pid patient-id)))
                                (filter (fn [[_ p]] (and (boarding? p) (= vacated-ward (:home-ward p)))))
                                (sort-by (fn [[pid p]] [(:admitted-at p) pid]))
                                (map first))]
               ;; the specific longest-waiting boarder must get a
               ;; bed-ready transfer AT THIS SAME instant -- checked
               ;; against the full ground-truth log rather than this
               ;; discharge's own replay record, since the transfer is
               ;; a SEPARATE, immediately-following ground-truth event
               ;; (same t, next in log order), not a second effect
               ;; folded into the discharge's own world-after.
               (or (empty? waiting)
                   (some #(and (= :transfer (:event %))
                               (true? (:bed-ready %))
                               (= (first waiting) (:patient-id (first (:participants %))))
                               (= (:t event) (:t %)))
                         ground-truth)))))
       (engine/replay ground-truth)))))

(deftest replay-tracks-before-after-and-world
  (let [{:keys [ground-truth]} (engine/run {:seed 7 :patients 2 :facility crowded-facility})
        records (engine/replay ground-truth)]
    (testing "one record per ground-truth event, in order"
      (is (= (count ground-truth) (count records)))
      (is (= ground-truth (map :event records))))
    (testing "world-after of the last record has every patient at their final fold"
      (let [final-world (:world-after (last records))]
        (is (= 2 (count final-world)))))))

(deftest pinned-seed-survives-decide-evolve-refactor
  (testing "the fixture pins the POST-M2a baseline (ADR-0009/ADR-0010/
            ADR-0011 -- identity/participants and the seconds clock
            both perturbed the pre-M2a baseline this test used to pin,
            regenerated ONCE per the M2a session plan, not a bug).
            This test now guards against FUTURE undocumented drift."
    (let [baseline (edn/read-string
                    (slurp (io/resource "ehr_testing_sim/fixtures/pinned_seed_42_patients_5.edn")))
          current (select-keys (engine/run {:seed 42 :patients 5}) [:ground-truth])]
      (is (= baseline current)))))
