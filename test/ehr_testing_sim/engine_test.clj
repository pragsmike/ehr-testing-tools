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
            [ehr-testing-sim.pathway :as pathway]
            [ehr-testing-sim.order-profiles :as order-profiles]
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
    (testing "each patient registers, admits, then discharges, in time
              order (M4: :registered is now every patient's first
              event, Persona's own stage boundary)"
      (is (= 9 (count ground-truth)))
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

;; --- ADR-0012: :step-rejected -- test helper --------------------------

(defn- assert-step-rejected!
  "ADR-0012: a decide-time rejection is no longer a silent no-op --
  exactly one :step-rejected event enters `outcome`'s :events, naming
  ONLY the attempting patient as :participants (never a possibly-
  nonexistent :with target -- participant-ids-exist-in-run stays sound)
  and carrying the documented `reason` keyword. The pre-existing
  :rejected outcome key (read directly by callers/tests, never entering
  the log) is unchanged by this milestone."
  [outcome patient-id reason]
  (is (= 1 (count (:events outcome))))
  (let [ev (first (:events outcome))]
    (is (= :step-rejected (:event ev)))
    (is (= reason (:reason ev)))
    (is (= [{:patient-id patient-id :role :subject}] (:participants ev))))
  (is (some? (:rejected outcome)))
  (is (= patient-id (:patient-id (:rejected outcome)))))

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

;; --- M2b: churn family ----------------------------------------------------

(def ^:private churn-facility
  "Two inpatient wards plus an ED with generous surge -- room for a
  bed-swap between a licensed and a surge occupant, and for admissions
  that never legitimately exhaust."
  {:id :churn-test
   :wards [{:id :ed :name "ED" :beds 0 :surge-slots 10
            :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 1 :surge-slots 1
            :surge-format "%s-H%02d" :class :inpatient}]})

(def ^:private churn-providers
  [{:id "1234567893" :name {:family "Chen" :given "A"} :role :attending
    :specialty "Nephrology" :wards [:renal :ed]}])

(defn- world-of
  [patients]
  {:patients patients :facility churn-facility :providers churn-providers :ground-truth []
   :order-profiles order-profiles/default-profiles})

(defn- fold-events
  "Test helper: applies `events` to `world`'s patients (every named
  participant, ADR-0010) and appends them to `world`'s :ground-truth --
  mirrors what engine/run's loop does each iteration, for scripted
  multi-step tests that drive decide/evolve directly."
  [world events]
  (-> (reduce (fn [w ev]
                (reduce (fn [w2 {:keys [patient-id]}]
                          (update-in w2 [:patients patient-id] engine/evolve ev))
                        w (:participants ev)))
              world events)
      (update :ground-truth into events)))

(defn- admit
  "Scripted-test helper: decides+folds an :admission for `patient-id`,
  returning the updated world."
  [world t patient-id location]
  (let [{:keys [events]} (engine/decide (Random. 1) t world patient-id
                                        {:type :admission :location location})]
    (fold-events world events)))

;; --- cancel-admit -> A11 --------------------------------------------------

(deftest cancel-admit-reverts-patient-to-new
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1" {:type :cancel-admit})
        world2 (fold-events world1 events)]
    (testing "one event, referencing the admission it cancels by log position"
      (is (= 1 (count events)))
      (is (= :cancel-admit (:event (first events))))
      (is (= 0 (:cancels-event-id (first events))))
      (is (= [{:patient-id "P1" :role :subject}] (:participants (first events)))))
    (testing "patient reverts to :new, no location/attending/class left behind"
      (let [p (get-in world2 [:patients "P1"])]
        (is (= :new (:status p)))
        (is (nil? (:location p)))
        (is (not (contains? p :class)))))))

(deftest cancel-admit-on-never-admitted-patient-is-a-structured-rejection-not-a-throw
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        outcome (engine/decide (Random. 1) 10 world0 "P1" {:type :cancel-admit})]
    (assert-step-rejected! outcome "P1" :illegal-cancel-admit)))

;; --- cancel-transfer -> A12: the shadow-field dissolution -----------------

(deftest cancel-transfer-reinstates-prior-location-from-the-log-no-shadow-field
  (testing "the SimHospital PriorLocationForCancelTransfer dissolution
            (docs/patient-state-model.md, docs/event-sourcing.md):
            cancelling a transfer reinstates the pre-transfer location by
            QUERYING THE LOG, not by reading a shadow field the
            accumulator carries for this purpose."
    (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
          world1 (admit world0 0 "P1" "Renal")
          pre-transfer-location (get-in world1 [:patients "P1" :location])
          {transfer-events :events} (engine/decide (Random. 1) 10 world1 "P1"
                                                    {:type :transfer :location "ED"})
          world2 (fold-events world1 transfer-events)
          _ (is (not= pre-transfer-location (get-in world2 [:patients "P1" :location]))
                "sanity: the transfer actually moved the patient")
          {cancel-events :events} (engine/decide (Random. 1) 20 world2 "P1" {:type :cancel-transfer})
          world3 (fold-events world2 cancel-events)
          reinstated (get-in world3 [:patients "P1"])]
      (testing "one cancel-transfer event, referencing the transfer by log position"
        (is (= 1 (count cancel-events)))
        (is (= :cancel-transfer (:event (first cancel-events))))
        (is (= 1 (:cancels-event-id (first cancel-events)))))
      (testing "location and home-ward are reinstated exactly"
        (is (= pre-transfer-location (:location reinstated)))
        (is (= "Renal" (:home-ward reinstated))))
      (testing "the accumulator holds NO prior-location shadow field -- only :location itself"
        (is (not (contains? reinstated :prior-location)))
        (is (not (contains? reinstated :location-before-cancel)))
        (is (= #{:patient-id :mrns :active-mrn :status :class :home-ward :location :attending :admitted-at}
               (set (keys reinstated))))))))

(deftest cancel-transfer-on-never-transferred-patient-is-rejected
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        outcome (engine/decide (Random. 1) 10 world1 "P1" {:type :cancel-transfer})]
    (assert-step-rejected! outcome "P1" :illegal-cancel-transfer)))

;; --- cancel-discharge -> A13 -----------------------------------------------

(deftest cancel-discharge-reinstates-admitted-state
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        pre-discharge (get-in world1 [:patients "P1"])
        {discharge-events :events} (engine/decide (Random. 1) 10 world1 "P1" {:type :discharge})
        world2 (fold-events world1 discharge-events)
        {cancel-events :events} (engine/decide (Random. 1) 20 world2 "P1" {:type :cancel-discharge})
        world3 (fold-events world2 cancel-events)
        reinstated (get-in world3 [:patients "P1"])]
    (is (= :discharged (:status (get-in world2 [:patients "P1"]))))
    (is (= :admitted (:status reinstated)))
    (is (= (:location pre-discharge) (:location reinstated)))
    (is (= (:home-ward pre-discharge) (:home-ward reinstated)))))

(deftest cancel-discharge-on-never-discharged-patient-is-rejected
  (testing "docs/patient-state-model.md's own illegal example"
    (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
          world1 (admit world0 0 "P1" "Renal")
          outcome (engine/decide (Random. 1) 10 world1 "P1" {:type :cancel-discharge})]
      (assert-step-rejected! outcome "P1" :illegal-cancel-discharge))))

(deftest cancel-transfer-cannot-be-applied-twice-to-the-same-transfer
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {t-events :events} (engine/decide (Random. 1) 10 world1 "P1" {:type :transfer :location "ED"})
        world2 (fold-events world1 t-events)
        {c1-events :events} (engine/decide (Random. 1) 20 world2 "P1" {:type :cancel-transfer})
        world3 (fold-events world2 c1-events)
        second-attempt (engine/decide (Random. 1) 30 world3 "P1" {:type :cancel-transfer})]
    (assert-step-rejected! second-attempt "P1" :illegal-cancel-transfer)))

;; --- transfer-in-error -----------------------------------------------------

(deftest transfer-in-error-emits-a-transfer-then-its-own-cancellation-in-error
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        pre (get-in world1 [:patients "P1"])
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1"
                                        {:type :transfer-in-error :location "ED"})
        world2 (fold-events world1 events)
        after (get-in world2 [:patients "P1"])]
    (testing "two events, same instant: the transfer, then its A12"
      (is (= 2 (count events)))
      (is (= :transfer (:event (first events))))
      (is (= :cancel-transfer (:event (second events))))
      (is (true? (:in-error (second events))))
      (is (= (:t (first events)) (:t (second events)))))
    (testing "the cancel references the transfer that immediately preceded it
              (index 1: index 0 is world1's own :admission event)"
      (is (= 1 (:cancels-event-id (second events)))))
    (testing "net effect: the patient ends up exactly where they started"
      (is (= (:location pre) (:location after)))
      (is (= (:home-ward pre) (:home-ward after))))))

;; --- bed-swap -> A17: genuinely two-participant ----------------------------

(deftest bed-swap-exchanges-locations-between-two-admitted-patients
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")})
        world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal"))
        p1-before (get-in world1 [:patients "P1"])
        p2-before (get-in world1 [:patients "P2"])
        _ (is (not= (:location p1-before) (:location p2-before)) "sanity: different beds")
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1" {:type :bed-swap})
        world2 (fold-events world1 events)]
    (testing "one two-participant event, both roles :subject"
      (is (= 1 (count events)))
      (is (= :bed-swap (:event (first events))))
      (is (= #{"P1" "P2"} (set (map :patient-id (:participants (first events))))))
      (is (every? #(= :subject (:role %)) (:participants (first events)))))
    (testing "locations are exchanged; both remain placed and admitted"
      (is (= (:location p2-before) (get-in world2 [:patients "P1" :location])))
      (is (= (:location p1-before) (get-in world2 [:patients "P2" :location])))
      (is (= :admitted (get-in world2 [:patients "P1" :status])))
      (is (= :admitted (get-in world2 [:patients "P2" :status]))))))

(deftest bed-swap-with-no-eligible-peer-is-rejected
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        outcome (engine/decide (Random. 1) 10 world1 "P1" {:type :bed-swap})]
    (assert-step-rejected! outcome "P1" :illegal-bed-swap)))

;; --- merge -> A40: the identity payoff --------------------------------------

(deftest merge-absorbs-mrns-and-terminates-the-merged-stream
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")})
        world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal"))
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1" {:type :merge :with "P2"})
        world2 (fold-events world1 events)
        survivor (get-in world2 [:patients "P1"])
        merged (get-in world2 [:patients "P2"])]
    (testing "one two-participant event, roles :survivor/:merged"
      (is (= 1 (count events)))
      (is (= :merge (:event (first events))))
      (is (= #{[:survivor "P1"] [:merged "P2"]}
             (set (map (juxt :role :patient-id) (:participants (first events)))))))
    (testing "survivor absorbs the merged MRN -- inactive, retained in :mrns"
      (is (= "MRN000001" (:active-mrn survivor)))
      (is (= #{"MRN000001" "MRN000002"} (:mrns survivor))))
    (testing "the merged patient-id's stream ends with a terminal merged status"
      (is (= :merged (:status merged)))
      (is (= #{"MRN000002"} (:mrns merged)))
      (is (= "MRN000002" (:active-mrn merged))))))

(deftest merge-into-self-is-rejected
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        outcome (engine/decide (Random. 1) 10 world1 "P1" {:type :merge :with "P1"})]
    (assert-step-rejected! outcome "P1" :illegal-merge)))

(deftest merge-referencing-unknown-patient-id-is-rejected
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        outcome (engine/decide (Random. 1) 10 world1 "P1" {:type :merge :with "GHOST"})]
    (assert-step-rejected! outcome "P1" :illegal-merge)
    (testing "the ghost id never enters :participants (participant-ids-exist-in-run stays sound)"
      (is (= "GHOST" (:with (:attempted-step (first (:events outcome)))))))))

(deftest double-merge-of-the-same-patient-id-is-rejected
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")
                          "P3" (engine/initial-patient "P3" "MRN000003")})
        world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal") (admit 6 "P3" "ED"))
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1" {:type :merge :with "P2"})
        world2 (fold-events world1 events)
        outcome (engine/decide (Random. 1) 20 world2 "P3" {:type :merge :with "P2"})]
    (assert-step-rejected! outcome "P3" :illegal-merge)))

;; --- M2b: InjectChurn wiring ------------------------------------------

(def ^:private active-churn-profile
  {:cancel-admit 0.05 :cancel-transfer 0.1 :cancel-discharge 0.05
   :transfer-in-error 0.1 :bed-swap 0.1 :merge 0.05})

(defspec every-churned-run-satisfies-the-invariant-catalog 150
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 2 12)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients
                                              :facility churn-facility :providers churn-providers
                                              :churn-profile active-churn-profile})]
      (result/ok? (check/check-all ground-truth churn-facility)))))

(deftest absent-churn-profile-does-not-perturb-the-no-churn-path
  (testing "byte-identical whether :churn-profile is omitted or
            explicitly nil (opt-in: nothing about this path changed
            unless a real profile is supplied)"
    (is (= (engine/run {:seed 42 :patients 5})
           (engine/run {:seed 42 :patients 5 :churn-profile nil})))))

(deftest churn-profile-actually-produces-churn-events-for-some-seed
  (testing "sanity that active-churn-profile is not accidentally inert"
    (is (some (fn [seed]
                (let [{:keys [ground-truth]} (engine/run {:seed seed :patients 8
                                                          :facility churn-facility :providers churn-providers
                                                          :churn-profile active-churn-profile})]
                  (some #{:cancel-admit :cancel-transfer :cancel-discharge :bed-swap :merge}
                        (map :event ground-truth))))
              (range 1 50)))))

;; --- M3-adjacent: per-patient pathway assignment (roadmap.md's M3 entry,
;; SimHospital's percentage_of_patients analogue) -------------------------

(def ^:private pathway-a
  "Two clinical events (admission, discharge) once rendered -- :delay
  itself emits none."
  pathway/sample-admission-discharge)

(def ^:private pathway-b
  "One clinical event -- distinguishable from pathway-a by shape alone."
  {:name "b" :steps [{:type :admission :location "Renal"}]})

(deftest assign-pathway-with-a-single-weighted-entry-always-picks-it
  (let [config [{:pathway pathway-a :weight 1}]]
    (doseq [i (range 20)]
      (is (= pathway-a (engine/assign-pathway (Random. i) config i))))))

(deftest assign-pathway-explicit-ordinal-overrides-the-weighted-pool
  (let [config [{:pathway pathway-a :weight 1}
                {:patient-ordinal 2 :pathway pathway-b}]]
    (is (= pathway-a (engine/assign-pathway (Random. 1) config 0)))
    (is (= pathway-b (engine/assign-pathway (Random. 1) config 2)))))

(deftest assign-pathway-is-deterministic-for-a-fixed-seed-and-ordinal
  (let [config [{:pathway pathway-a :weight 1} {:pathway pathway-b :weight 1}]]
    (is (= (engine/assign-pathway (Random. 99) config 4)
           (engine/assign-pathway (Random. 99) config 4)))))

(deftest weighted-pathway-assignment-favors-the-heavier-entry-overwhelmingly
  (testing "an extreme weight ratio (1e6:1e-6) should never pick the
            light entry across a range of rng seeds -- a statistical
            sanity check on `weighted-pick`'s math, not a flaky test
            (the light entry's selection probability is ~1e-12 per draw)"
    (let [config [{:pathway pathway-a :weight 1000000} {:pathway pathway-b :weight 0.000001}]]
      (is (every? #(= pathway-a (engine/assign-pathway (Random. %) config 0))
                  (range 200))))))

(def ^:private single-admission-renal
  "Same step SHAPE as single-admission-renal-tagged below (one
  :admission to the same ward) so swapping between them changes no
  downstream RNG consumption (bed/attending choice depend on step type
  and target ward, never on inert fields like :reason) -- isolating
  assign-pathway's OWN fixed-consumption law from the unrelated (and
  expected) fact that pathways of DIFFERENT shape consume different
  amounts of RNG once executed."
  {:name "solo" :steps [{:type :admission :location "Renal"}]})

(def ^:private single-admission-renal-tagged
  {:name "solo-tagged" :steps [{:type :admission :location "Renal" :reason "tagged"}]})

(deftest explicit-override-does-not-perturb-other-patients-downstream-draws
  (testing "fixed consumption (ADR-0009's own law, extended): exactly
            one assign-pathway draw per patient whether the outcome is
            explicit or weighted -- adding an explicit override (here,
            to a same-shaped pathway, isolating the assignment
            mechanism's own draw from the unrelated fact that a
            DIFFERENTLY-shaped pathway naturally consumes different
            downstream RNG once executed) for one patient must not
            shift RNG consumption, and therefore output, for any OTHER
            patient"
    (let [seed 42
          pool [{:pathway single-admission-renal :weight 1}]
          overridden (conj pool {:patient-ordinal 2 :pathway single-admission-renal-tagged})
          run1 (engine/run {:seed seed :patients 5 :pathways pool})
          run2 (engine/run {:seed seed :patients 5 :pathways overridden})]
      (doseq [i [0 1 3 4]]
        (let [id (engine/patient-id-for seed i)]
          (is (= (engine/events-for-patient (:ground-truth run1) id)
                 (engine/events-for-patient (:ground-truth run2) id))))))))

(deftest pathways-config-assigns-distinct-pathways-per-patient-through-run
  (testing "M3-adjacent: engine/run consumes :pathways end-to-end (not
            just via the pure assign-pathway helper) -- two explicit
            per-ordinal assignments, no weighted pool needed"
    (let [config [{:patient-ordinal 0 :pathway pathway-a}
                  {:patient-ordinal 1 :pathway pathway-b}]
          {:keys [ground-truth]} (engine/run {:seed 1 :patients 2 :pathways config})
          id0 (engine/patient-id-for 1 0)
          id1 (engine/patient-id-for 1 1)]
      (is (= [:registered :admission :discharge] (mapv :event (engine/events-for-patient ground-truth id0))))
      (is (= [:registered :admission] (mapv :event (engine/events-for-patient ground-truth id1)))))))

(deftest absent-pathways-key-is-unperturbed-by-this-milestone
  (testing "the degenerate case (roadmap.md's M3 entry): a run with NO
            :pathways key at all takes the SAME code path it always
            has -- no new draw, byte-identical to the pinned fixture
            (pinned-seed-survives-decide-evolve-refactor, below, is the
            fixture-level proof; this is the same guarantee stated
            directly against this milestone's own new option)"
    (is (= (engine/run {:seed 42 :patients 5})
           (engine/run {:seed 42 :patients 5 :pathways nil})))))

(deftest churned-run-surfaces-rejected-churn-steps-as-step-rejected-events
  (testing "ADR-0012: what used to be a silent no-op (M2b's own
            conservative cancel-discharge-reinstatement guard, and
            InjectChurn's other decide-time rejections generally) is now
            VISIBLE in the ground-truth log -- across enough seeds at
            least one churned run surfaces a :step-rejected event"
    (let [aggressive-profile {:cancel-admit 1.0 :cancel-transfer 1.0 :cancel-discharge 1.0
                              :transfer-in-error 1.0 :bed-swap 1.0 :merge 1.0}
          runs (for [seed (range 1 40)]
                 (:ground-truth (engine/run {:seed seed :patients 6
                                             :facility churn-facility :providers churn-providers
                                             :churn-profile aggressive-profile})))]
      (is (some (fn [gt] (some #(= :step-rejected (:event %)) gt)) runs)))))

;; --- M3: order/result (auto-paired) --------------------------------------

(deftest order-decide-emits-order-placed-and-schedules-a-followup
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        outcome (engine/decide (Random. 1) 10 world1 "P1" {:type :order :profile :cbc})]
    (testing "one event now: order-placed"
      (is (= 1 (count (:events outcome))))
      (let [ev (first (:events outcome))]
        (is (= :order-placed (:event ev)))
        (is (= 10 (:t ev)))
        (is (= :cbc (:profile ev)))
        (is (= [{:patient-id "P1" :role :subject}] (:participants ev)))))
    (testing "a follow-up is scheduled -- the result, NOT emitted directly
              here (that would break global time-monotonicity across
              patients, see engine.clj's :order docstring)"
      (is (some? (:schedule-followup outcome)))
      (let [{:keys [t patient-id steps]} (:schedule-followup outcome)]
        (is (> t 10) "strictly after the order, per a positive turnaround")
        (is (= "P1" patient-id))
        (is (= 1 (count steps)))
        (is (= :result-followup (:type (first steps))))
        (let [result-event (:result-event (first steps))]
          (is (= :result-available (:event result-event)))
          (is (= t (:t result-event)))
          (is (= 1 (:order-event-id result-event)) "the order-placed event's OWN index -- 0 is P1's :admission")
          (is (= :cbc (:profile result-event)))
          (is (= 5 (count (:results result-event))) "CBC's 5 analytes")
          (doseq [{:keys [value reference-range abnormal-flag]} (:results result-event)]
            (is (= abnormal-flag (order-profiles/abnormal-flag value reference-range)))))))))

(deftest order-decide-is-deterministic
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")]
    (is (= (engine/decide (Random. 42) 10 world1 "P1" {:type :order :profile :cbc})
           (engine/decide (Random. 42) 10 world1 "P1" {:type :order :profile :cbc})))))

(deftest order-followup-produces-result-available-through-the-real-run-loop
  (testing "M3-adjacent pathway-assignment mechanism (Task 1) makes this
            an end-to-end engine/run test, not a hand-driven scripted one"
    (let [pathway {:name "cbc-order" :steps [{:type :admission :location "Renal"}
                                             {:type :order :profile :cbc}
                                             {:type :delay :from 120 :to 120}
                                             {:type :discharge}]}
          {:keys [ground-truth]} (engine/run {:seed 7 :patients 1 :pathways [{:pathway pathway :weight 1}]})
          kinds (mapv :event ground-truth)]
      (testing "order-placed and result-available both appear, result strictly after order, before discharge"
        (is (= [:registered :admission :order-placed :result-available :discharge] kinds))
        (let [[_ _ order result _] ground-truth]
          (is (< (:t order) (:t result) (:t (last ground-truth))))
          (is (= 2 (:order-event-id result))))))))

(defspec order-and-result-round-trip-through-run-for-any-seed 100
  (prop/for-all [seed gen/large-integer]
    (let [pathway {:name "cbc-order" :steps [{:type :admission :location "Renal"}
                                             {:type :order :profile :cbc}
                                             {:type :discharge}]}
          {:keys [ground-truth]} (engine/run {:seed seed :patients 3 :pathways [{:pathway pathway :weight 1}]})]
      (result/ok? (check/check-all ground-truth)))))

(deftest pinned-seed-survives-decide-evolve-refactor
  (testing "the fixture pins the POST-M4 baseline (ADR-0009 -- Persona's
            :registered event, prepended to every patient's step queue,
            perturbed the post-M2a baseline this test used to pin,
            regenerated ONCE per the M4 session plan, not a bug).
            This test now guards against FUTURE undocumented drift."
    (let [baseline (edn/read-string
                    (slurp (io/resource "ehr_testing_sim/fixtures/pinned_seed_42_patients_5.edn")))
          current (select-keys (engine/run {:seed 42 :patients 5}) [:ground-truth])]
      (is (= baseline current)))))
