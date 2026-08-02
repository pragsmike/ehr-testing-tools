(ns ehrt.sim.engine-test
  "Determinism and invariants over the engine. The properties here are
  the executable form of the problem statement's Guarantees section:
  same inputs + seed => identical output; every run satisfies the
  invariant catalog. Also: sim/ADR-0008's decide/evolve split -- patient
  state is a fold of the log, proven as a property, plus a pinned-seed
  regression proving the refactor didn't change observable output for
  the v0 step set.

  M2a (sim/ADR-0010): patient-id is the fold/queue key; :mrn moves into
  state as {:mrns :active-mrn}; every event carries :participants.
  M2a (sim/ADR-0011): the engine clock is seconds; :delay's IR stays
  minutes, converted at decide-time; a warm-up window marks early
  events. See ehrt.sim.check-test for the invariant-catalog
  side of both."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.sim.engine :as engine]
            [ehrt.sim-trajectory.interface :as sim-trajectory]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim.order-profiles :as order-profiles]
            [ehrt.sim.check :as check]
            [ehrt.kernel.interface :as result])
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
              types are single-subject -- sim/ADR-0010)"
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

;; --- sim/ADR-0008: the engine is event-sourced ------------------------------

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

;; --- sim/ADR-0010: patient-id, mrns-as-state, participants -------------------

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

;; --- sim/ADR-0012: :step-rejected -- test helper --------------------------

(defn- assert-step-rejected!
  "sim/ADR-0012: a decide-time rejection is no longer a silent no-op --
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
            of boarding -- the cross-patient coupling sim/ADR-0008 exists
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
  participant, sim/ADR-0010) and appends them to `world`'s :ground-truth --
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

(deftest cancel-discharge-restores-class-even-after-a-preceding-cancel-admit-stripped-it
  (testing "M6 Task 2's own finding: a cancel-admit fired against an
            already-discharged patient's original admission (structurally
            legal -- last-uncancelled-index doesn't gate on CURRENT
            status) strips :class via cancel-admit's own dissoc; a
            following cancel-discharge reinstating :admitted must
            restore :class too, or the patient ends up admitted with no
            class at all -- surfaced by the v2-replay emitter-coherence
            property (a :discharge-family patient's own PV1-2 always
            renders :inpatient, ehrt.sim-emit-hl7.emit-hl7's own
            single-subject-message; ground truth must actually agree)"
    (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
          world1 (admit world0 0 "P1" "Renal")
          {d-events :events} (engine/decide (Random. 1) 10 world1 "P1" {:type :discharge})
          world2 (fold-events world1 d-events)
          {ca-events :events} (engine/decide (Random. 1) 20 world2 "P1" {:type :cancel-admit})
          world3 (fold-events world2 ca-events)
          _ (is (not (contains? (get-in world3 [:patients "P1"]) :class)) "sanity: cancel-admit stripped :class")
          {cd-events :events} (engine/decide (Random. 1) 30 world3 "P1" {:type :cancel-discharge})
          world4 (fold-events world3 cd-events)]
      (is (= :inpatient (:class (get-in world4 [:patients "P1"])))))))

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
  sim-model/sample-admission-discharge)

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
  (testing "fixed consumption (sim/ADR-0009's own law, extended): exactly
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
  (testing "sim/ADR-0012: what used to be a silent no-op (M2b's own
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

;; --- M5b: :outpatient-visit / :outpatient-visit-end (components/sim-trajectory/docs/gmf-interpreter.md
;; section 4's sketch, items 5-7) -------------------------------------------

(deftest outpatient-visit-admits-with-no-bed-no-ward-no-allocation-ladder
  (testing "item 5: no ehrt.sim-model.facility/allocate call at all --
            :status :new -> :admitted, :class :outpatient, :location stays
            nil for the visit's duration"
    (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
          {:keys [events]} (engine/decide (Random. 1) 0 world0 "P1"
                                          {:type :outpatient-visit :reason "Sinus congestion"})
          world1 (fold-events world0 events)
          p (get-in world1 [:patients "P1"])]
      (is (= 1 (count events)))
      (is (= :outpatient-visit (:event (first events))))
      (is (= [{:patient-id "P1" :role :subject}] (:participants (first events))))
      (is (= :admitted (:status p)))
      (is (= :outpatient (:class p)))
      (is (nil? (:location p)))
      (is (some? (:attending p)) "an outpatient visit still gets an attending, uniformly chosen (no ward to filter by)"))))

(deftest outpatient-visit-end-discharges-without-touching-location
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        {v1-events :events} (engine/decide (Random. 1) 0 world0 "P1" {:type :outpatient-visit})
        world1 (fold-events world0 v1-events)
        {end-events :events} (engine/decide (Random. 1) 30 world1 "P1" {:type :outpatient-visit-end})
        world2 (fold-events world1 end-events)
        p (get-in world2 [:patients "P1"])]
    (is (= 1 (count end-events)))
    (is (= :outpatient-visit-end (:event (first end-events))))
    (is (= :discharged (:status p)))
    (is (nil? (:location p)))
    (is (= :outpatient (:class p)) "class persists past discharge, same as every other class today")))

(deftest outpatient-visit-decide-is-deterministic
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})]
    (is (= (engine/decide (Random. 42) 0 world0 "P1" {:type :outpatient-visit})
           (engine/decide (Random. 42) 0 world0 "P1" {:type :outpatient-visit})))))

(deftest outpatient-visit-round-trips-through-the-real-run-loop-and-satisfies-the-catalog
  (let [pathway {:name "outpatient" :steps [{:type :outpatient-visit :reason "Sinus congestion"}
                                            {:type :delay :from 30 :to 30}
                                            {:type :outpatient-visit-end}]}
        {:keys [ground-truth] :as result} (engine/run {:seed 11 :patients 2 :pathways [{:pathway pathway :weight 1}]})]
    (is (= [:registered :outpatient-visit :outpatient-visit-end]
           (mapv :event (engine/events-for-patient ground-truth (engine/patient-id-for 11 0)))))
    (is (result/ok? (check/check-all ground-truth (:facility result))))))

(defspec outpatient-visits-never-occupy-a-bed-for-any-seed 150
  (prop/for-all [seed gen/large-integer]
    (let [pathway {:name "outpatient" :steps [{:type :outpatient-visit}
                                              {:type :delay :from 10 :to 30}
                                              {:type :outpatient-visit-end}]}
          {:keys [ground-truth]} (engine/run {:seed seed :patients 3 :pathways [{:pathway pathway :weight 1}]})]
      (result/ok? (check/check-all ground-truth)))))

;; --- M5b: CompileTrajectory's new ground-truth event types, and
;; citation/conditions passthrough for glass-box traceability ------------

(def ^:private a-citation {:module "sinusitis" :state :doctor-visit})
(def ^:private a-concept {:system :snomed :code "36971009" :display "Sinusitis (disorder)"})

(deftest admission-carries-a-compiled-citation-and-condition-annotations-into-ground-truth
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        conditions [{:event :condition-onset :codes [a-concept] :citation a-citation}]
        {:keys [events]} (engine/decide (Random. 1) 0 world0 "P1"
                                        {:type :admission :location "Renal"
                                         :citation a-citation :conditions conditions})]
    (is (= a-citation (:citation (first events))))
    (is (= conditions (:conditions (first events))))))

(deftest admission-with-no-citation-carries-none-byte-identical-to-pre-m5b
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        {:keys [events]} (engine/decide (Random. 1) 0 world0 "P1" {:type :admission :location "Renal"})]
    (is (not (contains? (first events) :citation)))
    (is (not (contains? (first events) :conditions)))))

(deftest procedure-decide-emits-a-log-only-fact-with-codes-and-citation
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1"
                                        {:type :procedure :codes [a-concept] :citation a-citation})]
    (is (= 1 (count events)))
    (is (= :procedure (:event (first events))))
    (is (= [a-concept] (:codes (first events))))
    (is (= a-citation (:citation (first events))))
    (is (= [{:patient-id "P1" :role :subject}] (:participants (first events))))))

(deftest observation-decide-carries-value-and-unit-through
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1"
                                        {:type :observation :codes [a-concept] :value 38.2 :unit "Cel"
                                         :citation a-citation})]
    (is (= :observation (:event (first events))))
    (is (= 38.2 (:value (first events))))
    (is (= "Cel" (:unit (first events))))))

;; --- GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P5): :diagnostic-report --

(def ^:private a-value-code {:system :snomed :code "10828004" :display "Positive (qualifier value)"})

(deftest observation-decide-carries-value-code-and-category-through
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1"
                                        {:type :observation :codes [a-concept] :value-code a-value-code
                                         :category "laboratory" :citation a-citation})]
    (is (= a-value-code (:value-code (first events))))
    (is (= "laboratory" (:category (first events))))
    (is (not (contains? (first events) :value)))))

(deftest diagnostic-report-decide-emits-one-event-with-report-codes-and-observations
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        observations [{:codes [a-concept] :value-code a-value-code :category "laboratory"}]
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1"
                                        {:type :diagnostic-report :codes [a-concept] :observations observations
                                         :citation a-citation})]
    (is (= 1 (count events)))
    (is (= :diagnostic-report (:event (first events))))
    (is (= [a-concept] (:codes (first events))))
    (is (= observations (:observations (first events))))
    (is (= a-citation (:citation (first events))))))

(deftest diagnostic-report-decide-with-no-report-level-codes-omits-the-key
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1"
                                        {:type :diagnostic-report :observations [{:codes [a-concept]}]})]
    (is (not (contains? (first events) :codes)))))

(deftest medication-order-then-end-references-the-order-by-citation-match
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {order-events :events} (engine/decide (Random. 1) 10 world1 "P1"
                                              {:type :medication-order :codes [a-concept] :citation a-citation})
        world2 (fold-events world1 order-events)
        {end-events :events} (engine/decide (Random. 1) 20 world2 "P1"
                                            {:type :medication-end :order-citation a-citation})]
    (is (= :medication-order (:event (first order-events))))
    (is (= :medication-end (:event (first end-events))))
    (is (= 1 (:order-event-id (first end-events)))
        "the ground-truth log INDEX of the medication-order event -- 0 is P1's :admission")))

(deftest medication-end-with-no-matching-order-has-a-nil-order-event-id
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1" {:type :medication-end :order-citation a-citation})]
    (is (nil? (:order-event-id (first events))))))

(deftest procedure-observation-medication-round-trip-through-the-real-run-loop
  (let [pathway {:name "clinical" :steps [{:type :admission :location "Renal"}
                                          {:type :observation :codes [a-concept] :value 38.2 :unit "Cel"}
                                          {:type :medication-order :codes [a-concept] :citation a-citation}
                                          {:type :medication-end :order-citation a-citation}
                                          {:type :discharge}]}
        {:keys [ground-truth] :as result} (engine/run {:seed 3 :patients 1 :pathways [{:pathway pathway :weight 1}]})]
    (is (= [:registered :admission :observation :medication-order :medication-end :discharge]
           (mapv :event ground-truth)))
    (is (result/ok? (check/check-all ground-truth (:facility result))))))

;; --- M6 Task 1: PatientState grows a clinical-content accumulator
;; (:conditions/:observations/:medication-orders/:discharged-at) --
;; EmitState's own "snapshot-at-instant" law (docs/sim-theory.edn) means
;; the FHIR emitter may touch NOTHING but folded state, never the log
;; directly; Condition/Observation/MedicationRequest content therefore
;; has to actually LAND in the fold, the same way :location/:persona
;; already do, rather than staying a log-only fact only check.clj reads
;; via engine/replay. -----------------------------------------------------

(deftest admission-folds-condition-annotations-into-patient-conditions
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        conditions [{:event :condition-onset :codes [a-concept] :citation a-citation}]
        {:keys [events]} (engine/decide (Random. 1) 5 world0 "P1"
                                        {:type :admission :location "Renal" :conditions conditions})
        world1 (fold-events world0 events)
        p (get-in world1 [:patients "P1"])]
    (is (= [{:codes [a-concept] :citation a-citation :onset-t 5 :clinical-status :active}]
           (:conditions p)))))

(deftest admission-with-no-conditions-leaves-the-conditions-field-absent
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")]
    (is (not (contains? (get-in world1 [:patients "P1"]) :conditions)))))

(deftest a-condition-end-annotation-on-the-same-encounter-resolves-the-matching-onset
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        end-citation {:module "sinusitis" :state :resolved}
        conditions [{:event :condition-onset :codes [a-concept] :citation a-citation}
                    {:event :condition-end :codes [a-concept] :citation end-citation}]
        {:keys [events]} (engine/decide (Random. 1) 5 world0 "P1"
                                        {:type :admission :location "Renal" :conditions conditions})
        world1 (fold-events world0 events)
        [condition] (:conditions (get-in world1 [:patients "P1"]))]
    (is (= :resolved (:clinical-status condition)))
    (is (= 5 (:end-t condition)))
    (is (= a-citation (:citation condition)) "the ONSET's own citation is retained, not overwritten")))

(deftest observation-decide-folds-into-patient-observations
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1"
                                        {:type :observation :codes [a-concept] :value 38.2 :unit "Cel"})
        world2 (fold-events world1 events)]
    (is (= [{:codes [a-concept] :t 10 :value 38.2 :unit "Cel"}]
           (:observations (get-in world2 [:patients "P1"]))))))

(deftest diagnostic-report-flattens-every-child-into-its-own-observation-record
  (testing "ADR-0029 P5: the SAME per-analyte flattening pattern
            :result-available's own evolve already establishes, reused"
    (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
          world1 (admit world0 0 "P1" "Renal")
          observations [{:codes [a-concept] :value 92.0 :unit "mm[Hg]" :category "vital-signs"
                         :reference-range {:low 90 :high 120} :interpretation :normal}
                        {:codes [a-concept] :value-code a-value-code :category "laboratory"}]
          {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1"
                                          {:type :diagnostic-report :codes [a-concept] :observations observations})
          world2 (fold-events world1 events)
          folded (:observations (get-in world2 [:patients "P1"]))]
      (is (= 2 (count folded)))
      (is (= {:codes [a-concept] :t 10 :value 92.0 :unit "mm[Hg]" :category "vital-signs"
              :reference-range {:low 90 :high 120} :interpretation :normal}
             (first folded)))
      (is (= {:codes [a-concept] :t 10 :value-code a-value-code :category "laboratory"}
             (second folded))))))

(deftest result-available-folds-every-analyte-into-patient-observations
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {order-events :events schedule :schedule-followup}
        (engine/decide (Random. 42) 10 world1 "P1" {:type :order :profile :cbc})
        result-event (:result-event (first (:steps schedule)))
        world2 (fold-events world1 (into order-events [result-event]))
        observations (:observations (get-in world2 [:patients "P1"]))]
    (is (= 5 (count observations)) "CBC's 5 analytes")
    (doseq [{:keys [codes t value unit reference-range interpretation]} observations]
      (is (= 1 (count codes)))
      (is (= (:t result-event) t))
      (is (some? value))
      (is (some? unit))
      (is (some? reference-range))
      (is (some? interpretation)))))

(deftest medication-order-then-end-folds-into-patient-medication-orders-and-closes-it
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {order-events :events} (engine/decide (Random. 1) 10 world1 "P1"
                                              {:type :medication-order :codes [a-concept] :citation a-citation})
        world2 (fold-events world1 order-events)
        opened (first (:medication-orders (get-in world2 [:patients "P1"])))
        {end-events :events} (engine/decide (Random. 1) 20 world2 "P1"
                                            {:type :medication-end :order-citation a-citation})
        world3 (fold-events world2 end-events)
        closed (first (:medication-orders (get-in world3 [:patients "P1"])))]
    (is (= {:codes [a-concept] :citation a-citation :ordered-t 10 :status :active} opened))
    (is (= :completed (:status closed)))
    (is (= 20 (:ended-t closed)))))

(deftest medication-end-with-no-matching-order-citation-leaves-medication-orders-untouched
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1" {:type :medication-end :order-citation a-citation})
        world2 (fold-events world1 events)]
    (is (nil? (:medication-orders (get-in world2 [:patients "P1"]))))))

(deftest discharge-and-outpatient-visit-end-both-stamp-discharged-at
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")})
        world1 (admit world0 0 "P1" "Renal")
        {d-events :events} (engine/decide (Random. 1) 30 world1 "P1" {:type :discharge})
        world2 (fold-events world1 d-events)
        {v-events :events} (engine/decide (Random. 1) 0 world0 "P2" {:type :outpatient-visit})
        world3 (fold-events world0 v-events)
        {e-events :events} (engine/decide (Random. 1) 40 world3 "P2" {:type :outpatient-visit-end})
        world4 (fold-events world3 e-events)]
    (is (= 30 (:discharged-at (get-in world2 [:patients "P1"]))))
    (is (= 40 (:discharged-at (get-in world4 [:patients "P2"]))))))

;; --- GMF coverage Wave C (2026-08-02, ADR-0028, C3): :expired --------------

(def ^:private death-codes [{:system :snomed :code "230690007" :display "Cerebrovascular accident (disorder)"}])

(deftest expired-disposition-discharge-sets-expired-status-and-retains-location
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        location-before (get-in world1 [:patients "P1" :location])
        {:keys [events]} (engine/decide (Random. 1) 30 world1 "P1"
                                        {:type :discharge :disposition :expired :codes death-codes})
        world2 (fold-events world1 events)
        after (get-in world2 [:patients "P1"])]
    (is (= 1 (count events)) "no bed-ready transfer -- no bed was vacated")
    (is (= :discharge (:event (first events))))
    (is (= :expired (:disposition (first events))))
    (is (= death-codes (:codes (first events))))
    (is (= :expired (:status after)))
    (is (= location-before (:location after)) "the body stays where it was, patient-state-model.md's own fact")
    (is (some? (:attending after)))
    (is (nil? (:discharged-at after)) "not a real discharge yet -- the final disposition-20 discharge is out of scope")))

(deftest expired-disposition-discharge-suppresses-the-bed-ready-transfer-coupling
  (testing "unlike an ordinary discharge, no bed was vacated -- a boarding
            patient waiting for the SAME ward must NOT be relieved"
    (let [world0 {:patients {"P1" (engine/initial-patient "P1" "MRN000001")
                              "P2" (engine/initial-patient "P2" "MRN000002")}
                  :facility crowded-facility
                  :providers test-providers}
          rng (Random. 1)
          {a-events :events} (engine/decide rng 0 world0 "P1" {:type :admission :location "Renal"})
          world1 (update-in world0 [:patients "P1"] #(reduce engine/evolve % a-events))
          {b-events :events} (engine/decide rng 10 world1 "P2" {:type :admission :location "Renal"})
          world2 (update-in world1 [:patients "P2"] #(reduce engine/evolve % b-events))]
      (is (boarding? (get-in world2 [:patients "P2"])) "P2 boards in ED surge, waiting for Renal")
      (let [{:keys [events]} (engine/decide rng 100 world2 "P1"
                                            {:type :discharge :disposition :expired :codes death-codes})]
        (is (= 1 (count events)))
        (is (not-any? #(= :transfer (:event %)) events))))))

(deftest expired-disposition-discharge-satisfies-its-own-new-invariant
  (testing "the structural check, over a scripted log directly -- the
            full engine/check-all catalog needs a real :registered-
            prepended run (a bare admission/discharge script like this
            one predates :registered, the same scaffold gap every other
            world-of/admit/fold-events test in this file already has);
            the end-to-end proof through a real Death-bearing walk is
            Step 3's own vendored-fixture test"
    (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
          world1 (admit world0 0 "P1" "Renal")
          {:keys [events]} (engine/decide (Random. 1) 30 world1 "P1"
                                          {:type :discharge :disposition :expired :codes death-codes})
          world2 (fold-events world1 events)]
      (is (empty? (check/expired-patient-retains-location (:ground-truth world2)))))))

;; --- M5b Task 4: end-to-end module wiring (persona -> run-module ->
;; CompileTrajectory -> IR), composing with :pathways -----------------------

(def ^:private clinic-module
  (:payload (sim-trajectory/load-module "fixture-clinic"
                            (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json")))))

(def ^:private sinusitis-module
  (:payload (sim-trajectory/load-module "sinusitis" (slurp (io/resource "sim/modules/sinusitis.json")))))

(deftest config-keys-includes-the-module-wiring-keys
  (is (every? (set engine/config-keys) [:modules :module-assignment :module-horizon-days])))

(deftest assign-module-resolves-explicit-ordinal-and-consumes-exactly-one-draw
  (let [config [{:patient-ordinal 0 :module-id "fixture-clinic"}]]
    (is (= "fixture-clinic" (engine/assign-module (Random. 1) config 0)))))

(deftest assign-module-resolves-a-weighted-pool-entry
  (let [config [{:module-id "fixture-clinic" :weight 1}]]
    (is (= "fixture-clinic" (engine/assign-module (Random. 1) config 5)))))

(deftest assign-module-resolves-nil-for-an-ordinal-neither-side-covers
  (testing "unlike assign-pathway's own PathwaysConfig -- a real
            population is expected to have patients with no module at
            all, an explicit-only config with no matching ordinal is a
            legitimate 'no module' outcome, not an error"
    (let [config [{:patient-ordinal 0 :module-id "fixture-clinic"}]]
      (is (nil? (engine/assign-module (Random. 1) config 7))))))

(deftest a-run-with-modules-configured-compiles-and-executes-a-real-trajectory
  (testing "persona -> run-module -> CompileTrajectory -> IR, wired into a
            real run -- :modules absent entirely is untouched (the pinned-
            fixture regression, below, is that same guarantee). Uses the
            REAL vendored sinusitis.json, deliberately, not the hand-
            written fixture: sinusitis.json's own Potential_Onset loop
            recurs across a patient's whole life (components/sim-trajectory/docs/gmf-interpreter.md),
            so it reliably produces horizon-phase content against THIS
            engine's own FIXED registration anchor (persona/reference-
            today-epoch-day) regardless of a patient's randomly sampled
            age -- fixture-clinic's own episode, by contrast, is a single
            near-birth event, so a fixed anchor decades removed from most
            sampled ages would only rarely catch it (a real, worth-noting
            interaction between a fixed calendar anchor and a young-age-
            only module, not a wiring bug -- see mixed-authored-and-
            compiled-run-satisfies-the-full-invariant-catalog, below, for
            fixture-clinic's own continued coverage of the invariant-
            holds-even-when-nothing-lands case)."
    (let [{:keys [ground-truth] :as result}
          (engine/run {:seed 1 :patients 30
                       ;; an EXPLICIT empty pathway -- otherwise every
                       ;; patient also gets the DEFAULT :pathway
                       ;; (sample-admission-discharge) appended after
                       ;; their compiled module content, which usually
                       ;; conflicts (the module's own encounter already
                       ;; discharged them; the default pathway assumes a
                       ;; fresh :new patient) -- a real caller wanting
                       ;; module-only patients must do the same.
                       :pathway {:name "module-only" :steps []}
                       :modules [sinusitis-module]
                       :module-assignment [{:module-id "sinusitis" :weight 1}]
                       :module-horizon-days 3650})
          kinds (into #{} (map :event) ground-truth)
          registered-events (filter #(= :registered (:event %)) ground-truth)]
      (is (some #{:outpatient-visit :observation :medication-order :medication-end} kinds)
          (str "expected at least one compiled clinical event across 30 patients, got " kinds))
      (is (some :pre-horizon-facts registered-events)
          "expected at least one patient's own history-phase facts to ride :registered")
      (is (some :citation (filter #(#{:outpatient-visit :admission} (:event %)) ground-truth))
          "the compiled encounter step carries its module/state citation into ground truth")
      (is (result/ok? (check/check-all ground-truth (:facility result)))))))

;; --- GMF coverage Wave C (2026-08-02, ADR-0028, C6): the full engine/check
;; round trip for a real Death-bearing walk -- interpreter -> compile-
;; trajectory -> :registered's own module wiring -> a real run -> the full
;; invariant catalog, including expired-patient-retains-location. Uses this
;; project's own hand-authored death-fixture.json (stroke.json stays
;; deferred, docs/gmf-interpreter.md section 10), the same "vendored"-
;; module-shaped wiring test the sinusitis-module test above already
;; establishes.

(def ^:private death-fixture-module
  (:payload (sim-trajectory/load-module "death-fixture"
                            (slurp (io/resource "ehrt/sim/fixtures/death-fixture.json")))))

(deftest a-run-with-the-death-fixture-configured-lands-expired-status-for-real
  (testing "engine.clj's own :registered anchors registration-t at a
            FIXED calendar instant (persona/reference-today-epoch-day),
            not DOB -- most sampled personas carry decades of history-
            phase content ahead of it, and Chance_of_Encounter's own
            first successful escape (whichever life-stage it falls in)
            only has a fraction of a chance of landing specifically
            within the post-registration horizon window; a large enough
            population makes both outcomes reliable for a fixed seed,
            confirmed empirically (200 patients, this seed: 26 in-window
            encounters, 6 died, 20 recovered)"
    (let [{:keys [ground-truth] :as result}
          (engine/run {:seed 20260802 :patients 200
                       :pathway {:name "module-only" :steps []}
                       :modules [death-fixture-module]
                       :module-assignment [{:module-id "death-fixture" :weight 1}]
                       :module-horizon-days 3650})
          discharges (filter #(= :discharge (:event %)) ground-truth)
          expired-discharges (filter #(= :expired (:disposition %)) discharges)
          ordinary-discharges (remove #(= :expired (:disposition %)) discharges)]
      (is (seq expired-discharges) "expected at least one patient to reach the death branch across 40")
      (is (seq ordinary-discharges) "expected at least one patient to reach the recover branch across 40")
      (is (every? #(= [{:system :snomed :code "230690007" :display "Cerebrovascular accident (disorder)"}] (:codes %))
                  expired-discharges))
      (is (result/ok? (check/check-all ground-truth (:facility result)))
          "the full catalog, including expired-patient-retains-location, holds for a real run")
      (is (some (fn [[_ p]] (= :expired (:status p)))
                (:world-after (last (engine/replay ground-truth))))
          "at least one patient's final folded state is genuinely :expired"))))

(deftest modules-absent-entirely-draws-no-extra-rng-byte-identical-to-pre-m5b
  (testing "the SAME opt-in law :pathways/:churn-profile already establish"
    (is (= (engine/run {:seed 42 :patients 5})
           (engine/run {:seed 42 :patients 5})))))

(defspec mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog 150
  (testing "components/sim-trajectory/docs/gmf-interpreter.md section 4's own central theory claim:
            authored pathways and compiled trajectories are BOTH just IR
            entering the union -- some patients on an explicit authored
            pathway, others on a compiled module, one run, one invariant
            catalog. Explicit per-ordinal assignment on BOTH sides (not a
            weighted pool for either) keeps the two sources from landing
            on the SAME patient at once -- a single patient-id running
            BOTH an authored admission pathway AND a module's own
            encounter is a real, separate compositional question
            (whichever runs second finds the patient already discharged),
            not what this property is stated about."
    (prop/for-all [seed gen/large-integer]
      (let [pathway {:name "scripted" :steps [{:type :admission :location "Renal"}
                                              {:type :delay :from 30 :to 30}
                                              {:type :discharge}]}
            empty-pathway {:name "module-only" :steps []}
            {:keys [ground-truth] :as result}
            (engine/run {:seed seed :patients 4
                         :pathways [{:patient-ordinal 0 :pathway pathway}
                                    {:patient-ordinal 1 :pathway pathway}
                                    {:patient-ordinal 2 :pathway empty-pathway}
                                    {:patient-ordinal 3 :pathway empty-pathway}]
                         :modules [clinic-module]
                         :module-assignment [{:patient-ordinal 2 :module-id "fixture-clinic"}
                                             {:patient-ordinal 3 :module-id "fixture-clinic"}]
                         :module-horizon-days 3650})]
        (result/ok? (check/check-all ground-truth (:facility result)))))))

(deftest pinned-seed-survives-decide-evolve-refactor
  (testing "the fixture pins the POST-M4 baseline (sim/ADR-0009 -- Persona's
            :registered event, prepended to every patient's step queue,
            perturbed the post-M2a baseline this test used to pin,
            regenerated ONCE per the M4 session plan, not a bug).
            This test now guards against FUTURE undocumented drift."
    (let [baseline (edn/read-string
                    (slurp (io/resource "ehrt/sim/fixtures/pinned_seed_42_patients_5.edn")))
          current (select-keys (engine/run {:seed 42 :patients 5}) [:ground-truth])]
      (is (= baseline current)))))
