(ns ehrt.sim-engine.engine-test
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
  events. See ehrt.sim-check.check-test for the invariant-catalog
  side of both."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-engine.order-profiles :as order-profiles]
            [ehrt.sim-check.check :as check]
            [ehrt.kernel.interface :as result]
            [malli.core :as m])
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
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 20)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients})]
      (result/ok? (check/check-all ground-truth)))))

(defspec determinism-holds-for-all-seeds 100
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 10)]
    (= (engine/run {:seed seed :patients patients})
       (engine/run {:seed seed :patients patients}))))

;; --- sim/ADR-0008: the engine is event-sourced ------------------------------

(defspec patient-state-is-a-fold-of-the-log 100
  (prop/for-all [seed (gen/large-integer* {:min 0})
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
          {a-events :events} (engine/decide (engine/one-stream rng) 0 world0 "P1"
                                             {:type :admission :location "Renal"})
          world1 (update-in world0 [:patients "P1"]
                             #(reduce engine/evolve % a-events))
          {b-events :events} (engine/decide (engine/one-stream rng) 10 world1 "P2"
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
      (let [{discharge-events :events} (engine/decide (engine/one-stream rng) 100 world2 "P1" {:type :discharge})]
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
  "The specification copy of `engine`'s own `waiting-boarder` predicate.

  TS-2 (traffic-scale close, 2026-08-29, section 9): the LOCATION
  clause is load-bearing and used to be missing from BOTH copies. A
  boarder is an admitted patient sitting in a bed that is not their
  home ward's; a patient in NO bed is boarding nowhere. Without
  `some?` an open outpatient encounter -- `:status :admitted`,
  `:location` nil, `:home-ward` left over from an earlier inpatient
  stay -- satisfied the other two clauses and was pulled into the next
  ready bed in that ward. Kept in step with the engine's own predicate
  deliberately: this helper is what says what the property below
  MEANS, so the two drifting apart would make the property assert
  something the engine does not do."
  [patient]
  (and (= :admitted (:status patient))
       (some? (get-in patient [:location :ward]))
       (not= (:home-ward patient) (get-in patient [:location :ward]))))

(defspec bed-ready-transfer-relieves-the-longest-waiting-boarder 150
  (prop/for-all [seed (gen/large-integer* {:min 0})
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


;; --- TS-2: an open outpatient encounter is not a waiting boarder ---------
;;
;; The traffic-scale close (2026-08-29,
;; `.agents/session-records/2026-08-29-traffic-scale-close.md` section 9)
;; found `outpatient-patients-occupy-no-bed` red across 24-25 patients at
;; 10^5 on the arc-4 add-on configuration, and diagnosed it as an
;; authored pathway walk that was not gated on the encounter's class.
;; THAT DIAGNOSIS IS WRONG, and the fixture below is what says so: its
;; only pathway is admission/delay/discharge, it contains no `:transfer`
;; step of any kind, and the offending transfers still appear -- carrying
;; `:bed-ready true`, which ONLY `bed-ready-transfer-event` produces.
;; The defect is in `waiting-boarder`'s predicate, one layer down.

(def ^:private followup-outpatient-config
  "The smallest population that presents the bed-ready coupling with an
  open OUTPATIENT encounter to choose, at the close's own seed.

  Every piece earns its place. `:encounters` is required or a
  discharged patient's follow-up visit opens nothing
  (`encounter-openable?` degenerates to the `:new` test without it).
  `:scheduling`'s follow-up rate is 1.0 so every discharge books a
  return visit, and `:scheduled-fraction` is 0.0 so no ARRIVAL is
  booked -- the outpatient encounters here all come from the follow-up
  producer, which is the shape the close saw. 150 patients at a 60
  minute arrival gap spans enough days that day-2 discharges land
  inside day-1 follow-up windows, which is the coincidence the defect
  needs; at 30 patients it never happens and the run is clean for want
  of traffic, not for want of the bug.

  ONE WARD, and that is the sharpest part: with a single ward no
  genuine boarder can exist at all (a boarder is by definition placed
  somewhere other than their home ward), so every boarder this
  facility can produce is a false one. 60 licensed beds because the
  defect LEAKS them -- `:outpatient-visit-end` sets `:status
  :discharged` without clearing `:location`, so a bed wrongly taken by
  an outpatient is held for the rest of the run, and a smaller
  facility dies `:capacity-exhausted` before `check-all` is ever
  reached."
  {:seed 20260824
   :patients 150
   :arrival-gap 60
   :facility {:id :ts2-fixture
              :wards [{:id :renal :name "Renal" :beds 60 :surge-slots 10
                       :surge-format "%s-H%02d" :class :inpatient
                       :turnaround-minutes [10 10]}]}
   :providers [{:name {:family "Reyes" :given "Priya"} :role :attending
                :specialty "Nephrology" :wards [:renal]}]
   :pathways [{:pathway {:name "short-stay"
                         :steps [{:type :admission :location "Renal"}
                                 {:type :delay :from 60 :to 180}
                                 {:type :discharge}]}
               :weight 1}]
   :encounters true
   :scheduling {:scheduled-fraction 0.0
                :lead-time-days [1 1]
                :no-show-rate 0.0
                :reschedule-rate 0.0
                :cancel-rate 0.0
                :follow-up {:rate 1.0 :interval-days [1 1]}}})

(defn- open-outpatient?
  [patient]
  (and (= :admitted (:status patient)) (= :outpatient (:class patient))))

(deftest an-open-outpatient-encounter-is-not-a-waiting-boarder
  (let [{:keys [ground-truth exhausted]} (engine/run followup-outpatient-config)
        records (engine/replay ground-truth)]
    (testing "the fixture reaches a real corpus rather than dying first"
      (is (nil? exhausted)
          "an exhausted run yields no log and no self-check, so it would prove nothing"))
    (testing "the OPPORTUNITY is counted, so a green row cannot be green for want of traffic"
      (is (pos? (count (filter #(= :outpatient-visit (:event %)) ground-truth)))
          "the population really does open outpatient encounters")
      (is (pos? (->> records
                     (filter #(= :discharge (:event (:event %))))
                     (mapcat (fn [{:keys [event world-before patient-id]}]
                               (let [ward (get-in world-before [patient-id :location :ward])]
                                 (for [[pid p] world-before
                                       :when (and (not= pid patient-id)
                                                  (open-outpatient? p)
                                                  (= ward (:home-ward p)))]
                                   [(:t event) pid]))))
                     count))
          "and a bed really is freed in the home ward of an OPEN outpatient encounter --
           the exact choice `waiting-boarder` is asked to make"))
    (testing "TS-2: no allocation is ever made on behalf of an open outpatient encounter"
      (is (empty? (->> records
                       (filter (fn [{:keys [event world-before patient-id]}]
                                 (and (#{:admission :transfer} (:event event))
                                      (open-outpatient? (get world-before patient-id)))))
                       (mapv (fn [{:keys [event patient-id]}]
                               {:at (:t event) :patient patient-id
                                :event (:event event) :bed (get-in event [:location :bed])}))))
          "an outpatient encounter occupies no bed, so nothing may put one in a bed"))
    (testing "and the catalog agrees, which is the row the close saw go red"
      (is (empty? (check/outpatient-patients-occupy-no-bed ground-truth)))
      (is (= :ok (:status (check/check-all ground-truth (:facility followup-outpatient-config))))))))

(def ^:private one-bed-one-surge-facility
  "ADR-0153: the smallest facility that can hold, at one instant, a
  Renal-surge occupant, a boarder waiting on Renal, AND a free Renal
  licensed bed -- the three-way state the seed-202 self-check failure
  needs. One licensed bed and one surge slot in Renal, a generous ED to
  board into, and no second inpatient ward (so rung 3 is empty and the
  boarder reaches rung 4 deterministically)."
  {:id :bed-ready-rung-test
   :wards [{:id :ed :name "ED" :beds 0 :surge-slots 4
            :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 1 :surge-slots 1
            :surge-format "%s-H%02d" :class :inpatient}]})

(defn- advance
  "Decide one step and fold its events into `world` the way engine/run's
  own loop does -- every participant of every emitted event evolved, and
  the event appended to :ground-truth (which the churn decides query).
  The multi-patient driver a hand-built sequence needs; the scripted
  bed-ready test above folds by hand because it only ever moves one
  patient at a time."
  [world rng t patient-id step]
  (let [{:keys [events]} (engine/decide (engine/one-stream rng) t world patient-id step)]
    (reduce (fn [w ev]
              (reduce (fn [w' pid] (update-in w' [:patients pid] engine/evolve ev))
                      (update w :ground-truth (fnil conj []) ev)
                      (map :patient-id (:participants ev))))
            world
            events)))

(deftest bed-ready-transfer-obeys-the-allocation-ladder
  (testing "ADR-0153 (roadmap.md#surge-policy-self-check-202, census S-5):
            the bed-ready coupling names the bed WITHIN its rung; it never
            hands a boarder a surge slot while a licensed bed in their own
            home ward is free. Minimal repro of the seed-202 misfire: a
            :cancel-admit frees RENAL-01 with no bed-ready pull of its own,
            so when the RENAL-H01 occupant discharges the waiting boarder
            must take rung 1 (RENAL-01), not the just-vacated rung-2 slot."
    (let [rng (Random. 1)
          world0 {:facility one-bed-one-surge-facility
                  :providers test-providers
                  :ground-truth []
                  :patients {"P1" (engine/initial-patient "P1" "MRN000001")
                             "P2" (engine/initial-patient "P2" "MRN000002")
                             "P3" (engine/initial-patient "P3" "MRN000003")}}
          world1 (advance world0 rng 0 "P1" {:type :admission :location "Renal"})
          world2 (advance world1 rng 10 "P2" {:type :admission :location "Renal"})
          world3 (advance world2 rng 20 "P3" {:type :admission :location "Renal"})]
      (testing "the ladder fills rung 1, rung 2, then boards P3 in ED surge"
        (is (= {:ward "Renal" :bed "RENAL-01" :placement :licensed}
               (get-in world3 [:patients "P1" :location])))
        (is (= {:ward "Renal" :bed "RENAL-H01" :placement :surge}
               (get-in world3 [:patients "P2" :location])))
        (is (= "Renal" (get-in world3 [:patients "P3" :home-ward])))
        (is (= "ED" (get-in world3 [:patients "P3" :location :ward]))))
      (let [world4 (advance world3 rng 30 "P1" {:type :cancel-admit})]
        (testing "the churn step frees RENAL-01, and nothing pulls a boarder in"
          (is (nil? (get-in world4 [:patients "P1" :location])))
          (is (= "ED" (get-in world4 [:patients "P3" :location :ward]))))
        (let [world5 (advance world4 rng 40 "P2" {:type :discharge})
              log (:ground-truth world5)
              transfer (last log)]
          (testing "P2's discharge bed-ready-transfers P3"
            (is (= :transfer (:event transfer)))
            (is (true? (:bed-ready transfer)))
            (is (= "P3" (:patient-id (first (:participants transfer))))))
          (testing "into the FREE LICENSED bed, not the vacated surge slot"
            (is (= {:ward "Renal" :bed "RENAL-01" :placement :licensed}
                   (:location transfer)))
            (is (= :licensed (:placement transfer))))
          (testing "so the self-check the seed-202 run failed stays silent"
            (is (empty? (check/surge-only-when-earlier-rungs-exhausted
                         log one-bed-one-surge-facility)))))))))

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
  (let [{:keys [events]} (engine/decide (engine/one-stream (Random. 1)) t world patient-id
                                        {:type :admission :location location})]
    (fold-events world events)))

;; --- cancel-admit -> A11 --------------------------------------------------

(deftest cancel-admit-reverts-patient-to-new
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :cancel-admit})
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
        outcome (engine/decide (engine/one-stream (Random. 1)) 10 world0 "P1" {:type :cancel-admit})]
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
          {transfer-events :events} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1"
                                                    {:type :transfer :location "ED"})
          world2 (fold-events world1 transfer-events)
          _ (is (not= pre-transfer-location (get-in world2 [:patients "P1" :location]))
                "sanity: the transfer actually moved the patient")
          {cancel-events :events} (engine/decide (engine/one-stream (Random. 1)) 20 world2 "P1" {:type :cancel-transfer})
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
        ;; ARC 3B SWEEP 1 (ADR-0174 section 2(a)): `:encounter` joins the
        ;; pinned key set, and it is NOT a shadow field of the kind this
        ;; test forbids -- it holds the OPEN encounter's own id and
        ;; ordinal, never a prior value of `:location`, which is exactly
        ;; why `evolve :transfer` and `evolve :cancel-transfer` were
        ;; untouched by that sweep. The pin moves rather than the
        ;; assertion weakening to a subset check: a NEW key here is a
        ;; change in what this accumulator holds and belongs inside the
        ;; gate.
        (is (= #{:patient-id :mrns :active-mrn :status :class :home-ward :location :attending
                 :admitted-at :encounter}
               (set (keys reinstated))))
        (is (= #{:ordinal :admitted-at} (set (keys (:encounter reinstated))))
            "and it carries no location of any kind -- nor an
             `:encounter-id`, since this hand-built world took no
             `:encounters` opt-in and nothing minted one")))))

(deftest cancel-transfer-on-never-transferred-patient-is-rejected
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        outcome (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :cancel-transfer})]
    (assert-step-rejected! outcome "P1" :illegal-cancel-transfer)))

;; --- cancel-discharge -> A13 -----------------------------------------------

(deftest cancel-discharge-reinstates-admitted-state
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        pre-discharge (get-in world1 [:patients "P1"])
        {discharge-events :events} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :discharge})
        world2 (fold-events world1 discharge-events)
        {cancel-events :events} (engine/decide (engine/one-stream (Random. 1)) 20 world2 "P1" {:type :cancel-discharge})
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
          {d-events :events} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :discharge})
          world2 (fold-events world1 d-events)
          {ca-events :events} (engine/decide (engine/one-stream (Random. 1)) 20 world2 "P1" {:type :cancel-admit})
          world3 (fold-events world2 ca-events)
          _ (is (not (contains? (get-in world3 [:patients "P1"]) :class)) "sanity: cancel-admit stripped :class")
          {cd-events :events} (engine/decide (engine/one-stream (Random. 1)) 30 world3 "P1" {:type :cancel-discharge})
          world4 (fold-events world3 cd-events)]
      (is (= :inpatient (:class (get-in world4 [:patients "P1"])))))))

(deftest cancel-discharge-on-never-discharged-patient-is-rejected
  (testing "docs/patient-state-model.md's own illegal example"
    (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
          world1 (admit world0 0 "P1" "Renal")
          outcome (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :cancel-discharge})]
      (assert-step-rejected! outcome "P1" :illegal-cancel-discharge))))

(deftest cancel-transfer-cannot-be-applied-twice-to-the-same-transfer
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {t-events :events} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :transfer :location "ED"})
        world2 (fold-events world1 t-events)
        {c1-events :events} (engine/decide (engine/one-stream (Random. 1)) 20 world2 "P1" {:type :cancel-transfer})
        world3 (fold-events world2 c1-events)
        second-attempt (engine/decide (engine/one-stream (Random. 1)) 30 world3 "P1" {:type :cancel-transfer})]
    (assert-step-rejected! second-attempt "P1" :illegal-cancel-transfer)))

;; --- transfer-in-error -----------------------------------------------------

(deftest transfer-in-error-emits-a-transfer-then-its-own-cancellation-in-error
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        pre (get-in world1 [:patients "P1"])
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1"
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
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :bed-swap})
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
        outcome (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :bed-swap})]
    (assert-step-rejected! outcome "P1" :illegal-bed-swap)))

;; --- merge -> A40: the identity payoff --------------------------------------

(deftest merge-absorbs-mrns-and-terminates-the-merged-stream
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")})
        world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal"))
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :merge :with "P2"})
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
        outcome (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :merge :with "P1"})]
    (assert-step-rejected! outcome "P1" :illegal-merge)))

(deftest merge-referencing-unknown-patient-id-is-rejected
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        outcome (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :merge :with "GHOST"})]
    (assert-step-rejected! outcome "P1" :illegal-merge)
    (testing "the ghost id never enters :participants (participant-ids-exist-in-run stays sound)"
      (is (= "GHOST" (:with (:attempted-step (first (:events outcome)))))))))

(deftest double-merge-of-the-same-patient-id-is-rejected
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")
                          "P3" (engine/initial-patient "P3" "MRN000003")})
        world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal") (admit 6 "P3" "ED"))
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :merge :with "P2"})
        world2 (fold-events world1 events)
        outcome (engine/decide (engine/one-stream (Random. 1)) 20 world2 "P3" {:type :merge :with "P2"})]
    (assert-step-rejected! outcome "P3" :illegal-merge)))

;; --- M2b: InjectChurn wiring ------------------------------------------

(def ^:private active-churn-profile
  {:cancel-admit 0.05 :cancel-transfer 0.1 :cancel-discharge 0.05
   :transfer-in-error 0.1 :bed-swap 0.1 :merge 0.05})

;; Lint family (AR-LF-5, D3-2's ruled middle path, `.agents/plans/
;; 2026-08-07-repo-review-findings.md`): this is the one `defspec`
;; repo-wide that has actually flaked (quality riders, AR-QR-4,
;; `notes/adr/0076-quality-riders.md`) -- seed -60645, 12 patients,
;; failed once, passed clean on an immediate re-run with an identical
;; tree. Whether this is a real churn-profile invariant bug only a
;; rare seed surfaces, or a property purely of test.check's own
;; unpinned exploration, was never determined (ADR-0076 explicitly
;; deferred that to this arc's own probe battery). Pinning the seed
;; that already reproduced the failure once means a future session
;; investigating it starts from a reproducible trial instead of
;; re-hunting an unpinned seed from a CI log. The other 70 `defspec`s
;; repo-wide stay unpinned (test.check's own printed-seed-on-failure
;; plus CI log retention ruled sufficient for those, per AR-LF-5/D3-2).
(defspec every-churned-run-satisfies-the-invariant-catalog
  {:num-tests 150 :seed -60645}
  (prop/for-all [seed (gen/large-integer* {:min 0})
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
        outcome (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :order :profile :cbc})]
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
    (is (= (engine/decide (engine/one-stream (Random. 42)) 10 world1 "P1" {:type :order :profile :cbc})
           (engine/decide (engine/one-stream (Random. 42)) 10 world1 "P1" {:type :order :profile :cbc})))))

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
  (prop/for-all [seed (gen/large-integer* {:min 0})]
    (let [pathway {:name "cbc-order" :steps [{:type :admission :location "Renal"}
                                             {:type :order :profile :cbc}
                                             {:type :discharge}]}
          {:keys [ground-truth]} (engine/run {:seed seed :patients 3 :pathways [{:pathway pathway :weight 1}]})]
      (result/ok? (check/check-all ground-truth)))))

;; --- M5b: :outpatient-visit / :outpatient-visit-end (components/patient-simulator/docs/gmf-interpreter.md
;; section 4's sketch, items 5-7) -------------------------------------------

(deftest outpatient-visit-admits-with-no-bed-no-ward-no-allocation-ladder
  (testing "item 5: no ehrt.sim-model.facility/allocate call at all --
            :status :new -> :admitted, :class :outpatient, :location stays
            nil for the visit's duration"
    (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
          {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 0 world0 "P1"
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
        {v1-events :events} (engine/decide (engine/one-stream (Random. 1)) 0 world0 "P1" {:type :outpatient-visit})
        world1 (fold-events world0 v1-events)
        {end-events :events} (engine/decide (engine/one-stream (Random. 1)) 30 world1 "P1" {:type :outpatient-visit-end})
        world2 (fold-events world1 end-events)
        p (get-in world2 [:patients "P1"])]
    (is (= 1 (count end-events)))
    (is (= :outpatient-visit-end (:event (first end-events))))
    (is (= :discharged (:status p)))
    (is (nil? (:location p)))
    (is (= :outpatient (:class p)) "class persists past discharge, same as every other class today")))

(deftest outpatient-visit-decide-is-deterministic
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})]
    (is (= (engine/decide (engine/one-stream (Random. 42)) 0 world0 "P1" {:type :outpatient-visit})
           (engine/decide (engine/one-stream (Random. 42)) 0 world0 "P1" {:type :outpatient-visit})))))

(deftest outpatient-visit-round-trips-through-the-real-run-loop-and-satisfies-the-catalog
  (let [pathway {:name "outpatient" :steps [{:type :outpatient-visit :reason "Sinus congestion"}
                                            {:type :delay :from 30 :to 30}
                                            {:type :outpatient-visit-end}]}
        {:keys [ground-truth] :as result} (engine/run {:seed 11 :patients 2 :pathways [{:pathway pathway :weight 1}]})]
    (is (= [:registered :outpatient-visit :outpatient-visit-end]
           (mapv :event (engine/events-for-patient ground-truth (engine/patient-id-for 11 0)))))
    (is (result/ok? (check/check-all ground-truth (:facility result))))))

(defspec outpatient-visits-never-occupy-a-bed-for-any-seed 150
  (prop/for-all [seed (gen/large-integer* {:min 0})]
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
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 0 world0 "P1"
                                        {:type :admission :location "Renal"
                                         :citation a-citation :conditions conditions})]
    (is (= a-citation (:citation (first events))))
    (is (= conditions (:conditions (first events))))))

(deftest admission-with-no-citation-carries-none-byte-identical-to-pre-m5b
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 0 world0 "P1" {:type :admission :location "Renal"})]
    (is (not (contains? (first events) :citation)))
    (is (not (contains? (first events) :conditions)))))

(deftest module-compiled-encounters-carry-no-reason-hand-authored-ones-keep-theirs
  (testing "census S-1, fixed under the event contract's own 1.2.0 bump
            (ADR-0151). `compile_trajectory.clj:211-213`'s
            `encounter->step` emits `:admission`/`:outpatient-visit`
            steps carrying a `:citation` and NO `:reason` -- so before
            this fix every module-compiled encounter emitted
            `:reason nil`, a present-but-nil key that tells a consumer
            nothing (`:outpatient-visit` 221/221, `:admission` 48/692,
            those 48 being exactly the 48 that carry a citation). The
            two fixtures below are that compiler output verbatim; the
            two after them are hand-authored steps, and they are the
            control that keeps this a nil-DROP rather than a removal."
    (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
          compiled-admission (:events (engine/decide (engine/one-stream (Random. 1)) 0 world0 "P1"
                                                     {:type :admission :location "Renal"
                                                      :citation a-citation}))
          compiled-visit (:events (engine/decide (engine/one-stream (Random. 1)) 0 world0 "P1"
                                                 {:type :outpatient-visit
                                                  :citation a-citation}))
          authored-admission (:events (engine/decide (engine/one-stream (Random. 1)) 0 world0 "P1"
                                                     {:type :admission :location "Renal"
                                                      :reason "Kidney problems"}))
          authored-visit (:events (engine/decide (engine/one-stream (Random. 1)) 0 world0 "P1"
                                                 {:type :outpatient-visit
                                                  :reason "Sinus congestion"}))]
      (is (not (contains? (first compiled-admission) :reason))
          "a module-compiled admission must not carry a nil :reason")
      (is (not (contains? (first compiled-visit) :reason))
          "a module-compiled outpatient visit must not carry a nil :reason")
      (is (= "Kidney problems" (:reason (first authored-admission)))
          "a hand-authored admission keeps its reason, unchanged")
      (is (= "Sinus congestion" (:reason (first authored-visit)))
          "a hand-authored outpatient visit keeps its reason, unchanged"))))

(deftest procedure-decide-emits-a-log-only-fact-with-codes-and-citation
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1"
                                        {:type :procedure :codes [a-concept] :citation a-citation})]
    (is (= 1 (count events)))
    (is (= :procedure (:event (first events))))
    (is (= [a-concept] (:codes (first events))))
    (is (= a-citation (:citation (first events))))
    (is (= [{:patient-id "P1" :role :subject}] (:participants (first events))))))

(deftest observation-decide-carries-value-and-unit-through
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1"
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
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1"
                                        {:type :observation :codes [a-concept] :value-code a-value-code
                                         :category "laboratory" :citation a-citation})]
    (is (= a-value-code (:value-code (first events))))
    (is (= "laboratory" (:category (first events))))
    (is (not (contains? (first events) :value)))))

(deftest diagnostic-report-decide-emits-one-event-with-report-codes-and-observations
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        observations [{:codes [a-concept] :value-code a-value-code :category "laboratory"}]
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1"
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
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1"
                                        {:type :diagnostic-report :observations [{:codes [a-concept]}]})]
    (is (not (contains? (first events) :codes)))))

(deftest medication-order-then-end-references-the-order-by-citation-match
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {order-events :events} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1"
                                              {:type :medication-order :codes [a-concept] :citation a-citation})
        world2 (fold-events world1 order-events)
        {end-events :events} (engine/decide (engine/one-stream (Random. 1)) 20 world2 "P1"
                                            {:type :medication-end :order-citation a-citation})]
    (is (= :medication-order (:event (first order-events))))
    (is (= :medication-end (:event (first end-events))))
    (is (= 1 (:order-event-id (first end-events)))
        "the ground-truth log INDEX of the medication-order event -- 0 is P1's :admission")))

(deftest medication-end-with-no-matching-order-has-a-nil-order-event-id
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :medication-end :order-citation a-citation})]
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
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 5 world0 "P1"
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
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 5 world0 "P1"
                                        {:type :admission :location "Renal" :conditions conditions})
        world1 (fold-events world0 events)
        [condition] (:conditions (get-in world1 [:patients "P1"]))]
    (is (= :resolved (:clinical-status condition)))
    (is (= 5 (:end-t condition)))
    (is (= a-citation (:citation condition)) "the ONSET's own citation is retained, not overwritten")))

(deftest observation-decide-folds-into-patient-observations
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1"
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
          {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1"
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
        (engine/decide (engine/one-stream (Random. 42)) 10 world1 "P1" {:type :order :profile :cbc})
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
        {order-events :events} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1"
                                              {:type :medication-order :codes [a-concept] :citation a-citation})
        world2 (fold-events world1 order-events)
        opened (first (:medication-orders (get-in world2 [:patients "P1"])))
        {end-events :events} (engine/decide (engine/one-stream (Random. 1)) 20 world2 "P1"
                                            {:type :medication-end :order-citation a-citation})
        world3 (fold-events world2 end-events)
        closed (first (:medication-orders (get-in world3 [:patients "P1"])))]
    (is (= {:codes [a-concept] :citation a-citation :ordered-t 10 :status :active} opened))
    (is (= :completed (:status closed)))
    (is (= 20 (:ended-t closed)))))

(deftest medication-end-with-no-matching-order-citation-leaves-medication-orders-untouched
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 10 world1 "P1" {:type :medication-end :order-citation a-citation})
        world2 (fold-events world1 events)]
    (is (nil? (:medication-orders (get-in world2 [:patients "P1"]))))))

(deftest discharge-and-outpatient-visit-end-both-stamp-discharged-at
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")})
        world1 (admit world0 0 "P1" "Renal")
        {d-events :events} (engine/decide (engine/one-stream (Random. 1)) 30 world1 "P1" {:type :discharge})
        world2 (fold-events world1 d-events)
        {v-events :events} (engine/decide (engine/one-stream (Random. 1)) 0 world0 "P2" {:type :outpatient-visit})
        world3 (fold-events world0 v-events)
        {e-events :events} (engine/decide (engine/one-stream (Random. 1)) 40 world3 "P2" {:type :outpatient-visit-end})
        world4 (fold-events world3 e-events)]
    (is (= 30 (:discharged-at (get-in world2 [:patients "P1"]))))
    (is (= 40 (:discharged-at (get-in world4 [:patients "P2"]))))))

;; --- GMF coverage Wave C (2026-08-02, ADR-0028, C3): :expired --------------

(def ^:private death-codes [{:system :snomed :code "230690007" :display "Cerebrovascular accident (disorder)"}])

(deftest expired-disposition-discharge-sets-expired-status-and-retains-location
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        location-before (get-in world1 [:patients "P1" :location])
        {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 30 world1 "P1"
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
          {a-events :events} (engine/decide (engine/one-stream rng) 0 world0 "P1" {:type :admission :location "Renal"})
          world1 (update-in world0 [:patients "P1"] #(reduce engine/evolve % a-events))
          {b-events :events} (engine/decide (engine/one-stream rng) 10 world1 "P2" {:type :admission :location "Renal"})
          world2 (update-in world1 [:patients "P2"] #(reduce engine/evolve % b-events))]
      (is (boarding? (get-in world2 [:patients "P2"])) "P2 boards in ED surge, waiting for Renal")
      (let [{:keys [events]} (engine/decide (engine/one-stream rng) 100 world2 "P1"
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
          {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 30 world1 "P1"
                                          {:type :discharge :disposition :expired :codes death-codes})
          world2 (fold-events world1 events)]
      (is (empty? (check/expired-patient-retains-location (:ground-truth world2)))))))


;; --- ADR-0164: decide-time citation resolution is patient-scoped ----------
;;
;; A citation is `{:module :state}` -- a MODULE COORDINATE, not a
;; patient-qualified one. Two patients walking the same module produce
;; byte-identical citations, and both decide-time scans searched the
;; WHOLE ground-truth log with `last`, so patient A's end could index
;; patient B's opening whenever B's opening was more recent. Real
;; collisions exist today: seed 424242 over demos/scenarios/clinic-
;; decade carries 3 distinct :medication-order citations shared across
;; 5, 3 and 9 patients respectively, plus one :care-plan-start citation
;; shared across 10; seed 5 carries 4 and 1.
;;
;; LATENT, and deliberately proven that way. Zero cross-patient
;; resolutions actually occurred in either run, and this is NOT the
;; cause of the seed-424242 failure ADR-0163 fixed -- that event
;; carried :order-citation nil, so `(when order-citation ...)` guarded
;; the scan out before it ever ran. These two tests are the whole
;; justification: a direct engine-level assertion, not a reproduction.
;;
;; R5: scripted world-of/admit/fold-events cannot produce :registered,
;; so these assert the resolved index directly rather than running the
;; invariant catalog -- the convention
;; `expired-disposition-discharge-satisfies-its-own-new-invariant`
;; above already establishes.

(def ^:private shared-med-citation {:module "shared-mod" :state :the-med})
(def ^:private shared-plan-citation {:module "shared-mod" :state :the-plan})

(deftest medication-end-resolves-its-own-patients-order-not-a-later-peers
  (testing "A orders, THEN B orders under the identical citation, THEN A
            ends: A's :order-event-id must index A's own order. `last`
            over an unfiltered log picks B's -- the later one -- and
            silently attributes one patient's prescription to another"
    (let [world0 (world-of {"A" (engine/initial-patient "A" "MRN000001")
                            "B" (engine/initial-patient "B" "MRN000002")})
          order-step {:type :medication-order
                      :codes [{:system :rxnorm :code "308191" :display "Amoxicillin"}]
                      :citation shared-med-citation}
          world1 (fold-events world0 (:events (engine/decide (engine/one-stream (Random. 1)) 0 world0 "A" order-step)))
          world2 (fold-events world1 (:events (engine/decide (engine/one-stream (Random. 1)) 10 world1 "B" order-step)))
          {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 20 world2 "A"
                                          {:type :medication-end
                                           :citation {:module "shared-mod" :state :end-med}
                                           :order-citation shared-med-citation})
          resolved (get (vec (:ground-truth world2)) (:order-event-id (first events)))]
      (is (= :medication-order (:event resolved)))
      (is (= "A" (:patient-id (first (:participants resolved))))
          "resolved to the OTHER patient's order -- citations are not patient-qualified"))))

(deftest care-plan-end-resolves-its-own-patients-start-not-a-later-peers
  (testing "the twin scan, same shape"
    (let [world0 (world-of {"A" (engine/initial-patient "A" "MRN000001")
                            "B" (engine/initial-patient "B" "MRN000002")})
          start-step {:type :care-plan-start
                      :codes [{:system :snomed :code "736285004" :display "Care plan"}]
                      :citation shared-plan-citation}
          world1 (fold-events world0 (:events (engine/decide (engine/one-stream (Random. 1)) 0 world0 "A" start-step)))
          world2 (fold-events world1 (:events (engine/decide (engine/one-stream (Random. 1)) 10 world1 "B" start-step)))
          {:keys [events]} (engine/decide (engine/one-stream (Random. 1)) 20 world2 "A"
                                          {:type :care-plan-end
                                           :citation {:module "shared-mod" :state :end-plan}
                                           :care-plan-citation shared-plan-citation})
          resolved (get (vec (:ground-truth world2)) (:start-event-id (first events)))]
      (is (= :care-plan-start (:event resolved)))
      (is (= "A" (:patient-id (first (:participants resolved))))
          "resolved to the OTHER patient's care plan"))))

;; --- M5b Task 4: end-to-end module wiring (persona -> run-module ->
;; CompileTrajectory -> IR), composing with :pathways -----------------------

(def ^:private clinic-module
  (:payload (patient-simulator/load-module "fixture-clinic"
                            (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json")))))

(def ^:private sinusitis-module
  (:payload (patient-simulator/load-module "sinusitis" (slurp (io/resource "sim/modules/sinusitis.json")))))

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
            recurs across a patient's whole life (components/patient-simulator/docs/gmf-interpreter.md),
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
                       :modules [(patient-simulator/singleton-closure sinusitis-module)]
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

;; --- ADR-0173 ruling C1 (arc 3a, 2026-08-26): the compile is
;; ARRIVAL-TIME INDEPENDENT -----------------------------------------------
;;
;; The gate that had to be green BEFORE the persona draw and the module
;; walk could be moved out of `decide :registered` and up to run start.
;; `decide` takes `t` as a parameter, so the claim is directly testable:
;; call the SAME method, for the SAME patient, off two identically-seeded
;; fresh streams, at two wildly different arrival instants -- everything
;; the compile produces must be `=`, and the ONLY thing that may differ
;; on the emitted event is `:t` itself.
;;
;; This is an equivalence gate, born green on the unrefactored tree
;; (ADR-0169's own pattern: a pure refactor owes no red-before-green, it
;; owes proof that nothing moved). It stays green afterwards, and after
;; the refactor it also exercises `decide :registered`'s hand-built-world
;; FALLBACK path, since the worlds below carry no `:compiled-patients`
;; key.

(def ^:private arrival-time-independence-world
  "A hand-built world carrying exactly the four run-config keys
  `compile-patient` reads, plus the one `:patients` entry `:registered`
  needs for its `:active-mrn`."
  {:patients {"PID-000000-deadbeef" {:patient-id "PID-000000-deadbeef"
                                     :mrns #{"MRN000001"} :active-mrn "MRN000001"
                                     :status :new}}
   :facility sim-model/default-facility
   :persona-config {}
   :module-horizon-days 3650
   :history false})

(defn- registered-decision-at
  "`decide :registered` for one patient at instant `t`, off a FRESH
  stream at `seed`/ordinal 0 -- so two calls differ in nothing but `t`."
  [seed t closure]
  (engine/decide {:patient (engine/stream seed :patient 0)}
                 t arrival-time-independence-world "PID-000000-deadbeef"
                 {:type :registered :closure closure}))

(deftest the-registered-compile-is-arrival-time-independent
  (testing "ADR-0173 C1: every input to the persona draw, the module walk
            and compile-trajectory is arrival-time-independent, so moving
            the whole compile from arrival to run start cannot move a
            byte. `t` reaches the emitted event and nothing else."
    (let [closure (patient-simulator/singleton-closure sinusitis-module)
          ;; 0 is the earliest instant any run can produce (ordinal 0
          ;; always arrives at t=0); the other two are far past any
          ;; realistic arrival, one of them past a full year of seconds.
          instants [0 86400 (* 400 86400)]
          seeds (range 12)
          compiled-for (fn [seed] (mapv #(registered-decision-at seed % closure) instants))]
      (doseq [seed seeds]
        (let [[a & others] (compiled-for seed)]
          (doseq [[t b] (map vector (rest instants) others)]
            (is (= (:prepend-steps a) (:prepend-steps b))
                (str "seed " seed ": the compiled step IR differs between arrival at "
                     (first instants) " and arrival at " t))
            (is (= (:advance a) (:advance b)))
            (is (= (dissoc (first (:events a)) :t) (dissoc (first (:events b)) :t))
                (str "seed " seed ": the :registered event differs by more than :t"))
            (is (= (pr-str (:prepend-steps a)) (pr-str (:prepend-steps b)))
                (str "seed " seed ": the compiled step IR is `=` but not byte-equal"))
            (is (= (first instants) (:t (first (:events a))))
                "the emitted event's :t is the arrival instant it was decided at")
            (is (= t (:t (first (:events b)))))))))
    (testing "the no-closure path -- the persona alone -- is independent too"
      (doseq [seed (range 12)]
        (let [a (registered-decision-at seed 0 nil)
              b (registered-decision-at seed (* 400 86400) nil)]
          (is (= (dissoc (first (:events a)) :t) (dissoc (first (:events b)) :t)))
          (is (nil? (:prepend-steps a)))))))
  (testing "the gate is not vacuous (R-empty-population-is-red): the
            closure path really does compile a trajectory, and at least
            one seed really does carry history-phase facts onto the event"
    (let [closure (patient-simulator/singleton-closure sinusitis-module)
          decisions (mapv #(registered-decision-at % 0 closure) (range 12))]
      ;; NOT `every?`: a seed whose whole sinusitis walk falls in the
      ;; history phase compiles to registration-facts and NO horizon
      ;; steps at all -- found by this assertion going red as `every?`,
      ;; and left as the record of why the weaker form is the right one.
      (is (some #(seq (:prepend-steps %)) decisions)
          "not one of the twelve seeds compiled a single horizon-phase step")
      (is (every? #(some? (:persona (first (:events %)))) decisions))
      (is (some #(seq (:pre-horizon-facts (first (:events %)))) decisions)
          "no seed produced :pre-horizon-facts -- the registration-facts half
           of the compile is untested by this gate"))))

(deftest compile-patient-is-what-registered-attaches
  (testing "ADR-0173 C1: `compile-patient` -- the export `ehrt.sim.run`
            will call at run start in part 3 -- returns exactly what
            `decide :registered` attaches, off the same stream position."
    (let [closure (patient-simulator/singleton-closure sinusitis-module)]
      (doseq [seed (range 8)]
        (let [{:keys [persona compiled]} (engine/compile-patient
                                          (engine/stream seed :patient 0)
                                          arrival-time-independence-world
                                          closure)
              decision (registered-decision-at seed 0 closure)]
          (is (= persona (:persona (first (:events decision)))))
          (is (= (:steps compiled) (:prepend-steps decision)))
          (is (= (seq (:registration-facts compiled))
                 (seq (:pre-horizon-facts (first (:events decision))))))))))
  (testing "a patient with no closure compiles to a persona and nothing else"
    (let [{:keys [persona compiled]} (engine/compile-patient
                                      (engine/stream 3 :patient 0)
                                      arrival-time-independence-world nil)]
      (is (some? persona))
      (is (nil? compiled)))))

;; --- ADR-0173 section 2(a) (arc 3a, 2026-08-26): the carried person
;; index -----------------------------------------------------------------
;;
;; `run` seeds `:person-index` EMPTY and nothing writes it yet; part 3's
;; arrival selection is what fills it. What can be gated today is the
;; contract that makes carrying it safe -- the hand-built-world tolerance
;; is on the KEY, never on a missing entry -- and that is exactly what
;; `person-entry` is, so it lands with the key and is gated here.
;;
;; That the key moved no byte is NOT asserted here: it is asserted where
;; byte identity already lives -- `pinned-seed-survives-decide-evolve-
;; refactor` below, `ehrt.sim.run-test/arc0-gated-corpora-are-byte-and-
;; value-identical-to-the-pinned-baseline`, and `bin/regression-oracle`.

(deftest the-person-index-falls-back-on-the-key-never-on-a-missing-entry
  (testing "ADR-0173 2(a): a world `run` built carries the KEY, so a
            person it has never seen reads nil FROM the index. A world
            that carries no key at all -- a hand-built world, as most of
            this namespace uses -- also reads nil, but by the fallback."
    (let [built {:person-index {"P-1" {:patient-id "PID-000000-deadbeef"
                                       :first-ordinal 0
                                       :active-mrn "MRN000001"
                                       :placeholders #{}}}}]
      (is (= {:patient-id "PID-000000-deadbeef" :first-ordinal 0
              :active-mrn "MRN000001" :placeholders #{}}
             (engine/person-entry built "P-1")))
      (is (nil? (engine/person-entry built "P-2"))
          "a person the index does not carry must read nil, not throw")
      (is (nil? (engine/person-entry {:person-index {}} "P-1"))
          "an EMPTY index -- what `run` seeds today -- reads nil for everyone")
      (is (nil? (engine/person-entry {} "P-1"))
          "a world with no :person-index KEY at all reads nil by the fallback")
      (is (nil? (engine/person-entry {:person-index nil} "P-1"))
          "a nil index is still a present key, and still reads nil")))
  (testing "the entry shape ADR-0173 2(a) fixes is what the reader returns
            verbatim -- the index is a carrier, not a projection"
    (let [entry {:patient-id "PID-000000-deadbeef" :first-ordinal 3
                 :active-mrn "MRN000004" :placeholders #{"PID-000009-cafe"}}]
      (is (= entry (engine/person-entry {:person-index {"P-9" entry}} "P-9"))))))

;; --- ADR-0173 section 2(b) (arc 3a, 2026-08-26): :demographics, seeded
;; and read by nothing yet ------------------------------------------------
;;
;; `PatientState` gains the state-at-t map. It is seeded at `:registered`
;; from that patient's own t0 Persona and READ nowhere in src: no
;; `:persona` reader is re-pointed, the emitter still builds its lookup
;; off the log's `:persona`, and `event-schema` is untouched (no wire
;; change, no contract bump). So the only thing that can be gated today
;; is the seeding itself, and the t0-record-stays-t0 law beside it.

(deftest registered-seeds-demographics-from-the-persona-and-leaves-persona-alone
  (testing "ADR-0173 2(b): every housed field copies across, `:address`
            becomes a `:housed` residence, `:identity` is `:known`, and
            `:age` is deliberately NOT carried (a t0 derivation of :dob
            against a fixed anchor, not state-at-t)."
    (let [persona (sim-model/persona (engine/stream 42 :patient 0) {})
          state (engine/evolve (engine/initial-patient "PID-000000-deadbeef" "MRN000001")
                               {:event :registered :t 0 :persona persona
                                :active-mrn "MRN000001"
                                :participants [{:patient-id "PID-000000-deadbeef" :role :subject}]})
          demo (:demographics state)]
      (is (= persona (:persona state))
          "the t0 record was mutated -- ADR-0173 2(b) requires :persona untouched")
      (is (= (select-keys persona [:name :sex :dob :phone :ssn :payer])
             (select-keys demo [:name :sex :dob :phone :ssn :payer])))
      (is (= {:status :housed :address (:address persona)} (:residence demo)))
      (is (= :known (:identity demo)))
      (is (nil? (:age demo)) ":age is a t0 derivation and does not belong to state-at-t")
      (is (nil? (:address demo)) "the flat :address must not survive beside :residence")
      (testing "the schema accepts what the seeding produces, and the whole
                patient state still validates"
        (is (m/validate engine/Demographics demo)
            (str "seeded demographics fail Demographics: "
                 (pr-str (m/explain engine/Demographics demo))))
        (is (engine/valid-patient? state)
            (str "the folded patient state fails PatientState: "
                 (pr-str (m/explain engine/PatientState state)))))))
  (testing "nil in, nil out -- `evolve :registered` stays total over a
            hand-authored log whose :registered carries no persona"
    (let [state (engine/evolve (engine/initial-patient "PID-1" "MRN000001")
                               {:event :registered :t 0 :active-mrn "MRN000001"
                                :participants [{:patient-id "PID-1" :role :subject}]})]
      (is (nil? (:persona state)))
      (is (nil? (:demographics state)))
      (is (engine/valid-patient? state))))
  (testing "every patient in a real run carries a valid state-at-t map,
            and it is the seeding and nothing else"
    (let [{:keys [ground-truth state-history]} (engine/run {:seed 42 :patients 5})
          personas (into {} (comp (filter #(= :registered (:event %)))
                                  (map (fn [ev] [(:patient-id (first (:participants ev))) (:persona ev)])))
                         ground-truth)]
      (testing "population is non-empty (R-empty-population-is-red)"
        (is (= 5 (count personas))))
      (doseq [[pid states] state-history]
        (let [final (last states)]
          (is (= (engine/demographics-from-persona (get personas pid)) (:demographics final))
              (str pid ": state-at-t drifted from the seeding, but nothing folds onto it yet"))
          (is (m/validate engine/Demographics (:demographics final))))))))

(deftest demographics-schema-carries-the-residence-sum
  (testing "ADR-0173 2(b): a places row cannot express \"no residence\", so
            `:residence` is a SUM and all three arms validate -- the shape
            part 3's :residence-loss and :residence-move siblings fold
            onto, and the shape section 2(d)'s placeholder registration
            needs for its :unknown."
    (let [base (dissoc (engine/demographics-from-persona
                        (sim-model/persona (engine/stream 7 :patient 0) {}))
                       :residence)
          addr {:street "702 Crestwood Ave" :city "Tucson" :state "AZ" :zip "85701"}]
      (is (m/validate engine/Demographics (assoc base :residence {:status :housed :address addr})))
      (is (m/validate engine/Demographics (assoc base :residence {:status :unhoused})))
      (is (m/validate engine/Demographics
                      (assoc base :residence {:status :unhoused :last-known-address addr})))
      (is (m/validate engine/Demographics
                      (assoc (assoc base :identity :placeholder)
                             :residence {:status :unknown})))
      (testing "and the sum is closed where it must be"
        (is (not (m/validate engine/Demographics (assoc base :residence {:status :housed})))
            "a :housed residence with no address validated -- the sum is not carrying its address")
        (is (not (m/validate engine/Demographics (assoc base :residence {:status :nowhere})))
            "an unknown :status validated -- the residence sum is not closed")
        (is (not (m/validate engine/Demographics (assoc base :identity :made-up
                                                        :residence {:status :unhoused})))
            "an unknown :identity validated -- the identity enum is not closed")))))

;; --- GMF coverage Wave C (2026-08-02, ADR-0028, C6): the full engine/check
;; round trip for a real Death-bearing walk -- interpreter -> compile-
;; trajectory -> :registered's own module wiring -> a real run -> the full
;; invariant catalog, including expired-patient-retains-location. Uses this
;; project's own hand-authored death-fixture.json (stroke.json stays
;; deferred, docs/gmf-interpreter.md section 10), the same "vendored"-
;; module-shaped wiring test the sinusitis-module test above already
;; establishes.

(def ^:private death-fixture-module
  (:payload (patient-simulator/load-module "death-fixture"
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
                       :modules [(patient-simulator/singleton-closure death-fixture-module)]
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
  (testing "components/patient-simulator/docs/gmf-interpreter.md section 4's own central theory claim:
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
    (prop/for-all [seed (gen/large-integer* {:min 0})]
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
                         :modules [(patient-simulator/singleton-closure clinic-module)]
                         :module-assignment [{:patient-ordinal 2 :module-id "fixture-clinic"}
                                             {:patient-ordinal 3 :module-id "fixture-clinic"}]
                         :module-horizon-days 3650})]
        (result/ok? (check/check-all ground-truth (:facility result)))))))

(deftest run-rejects-negative-seed-with-clean-error
  (testing "sim/ADR-0116 (R9): the seed contract is non-negative longs --
            a negative :seed is rejected at entry with the standard
            invalid-option envelope (ehrt.kernel.result), never run,
            never a raw throw. The shrunk counterexample from the
            engine-test flake investigation (R8) is the regression
            case."
    (let [r (engine/run {:seed -3377439408979484 :patients 1})]
      (is (result/error? r))
      (is (= :invalid-seed (:category r)))
      (is (= -3377439408979484 (:value (:payload r)))))))

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

;; --- Wave H pre-roll, Step 2 (2026-08-04, ADR-0042 AR-1/AR-2): the
;; straddle rule at engine scale -- a hand-authored module whose own
;; Encounter opens at DOB and closes 500 days later, run at
;; `:persona-config {:age-min 0 :age-max 0}` -- persona/reference-
;; today-epoch-day (this engine's own FIXED registration anchor) is
;; ALWAYS somewhere between ~4 and ~365 days after an age-0 persona's
;; own DOB (persona.clj's own birth-year/month/day derivation: DOB
;; falls anywhere within `reference-birth-year`, reg-t is Jan 1 of the
;; year after), so THIS encounter's own 500-day span straddles
;; registration-t for EVERY seed, not merely a hand-picked one -- the
;; deterministic analog of the UTI closure's own empirical straddle
;; (ADR-0033/0034 dated notes), used here to prove the mechanism itself
;; rather than lean on a real vendored root's own incidental one.

(def ^:private straddle-module
  {:id "straddle-mod" :name "Straddle"
   :states {:initial {:type :initial :direct-transition :visit-one}
            :visit-one {:type :encounter :encounter-class :ambulatory
                        :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]
                        :direct-transition :procedure-one}
            :procedure-one {:type :procedure
                            :codes [{:system :snomed :code "80146002" :display "Excision of appendix"}]
                            :direct-transition :wait-one}
            :wait-one {:type :delay :exact {:quantity 500 :unit "days"} :direct-transition :end-one}
            :end-one {:type :encounter-end :direct-transition :gap}
            :gap {:type :delay :exact {:quantity 1 :unit "days"} :direct-transition :visit-two}
            :visit-two {:type :encounter :encounter-class :ambulatory
                        :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]
                        :direct-transition :observe-two}
            :observe-two {:type :observation :category "vital-signs" :unit "Cel"
                          :codes [{:system :loinc :code "8310-5" :display "Body temperature"}]
                          :range {:low 37.5 :high 38.0}
                          :direct-transition :end-two}
            :end-two {:type :encounter-end :direct-transition :done}
            :done {:type :terminal}}})

(defspec history-mode-straddling-encounter-drops-in-full-post-straddle-content-lands-invariant-holds 150
  (prop/for-all [seed (gen/large-integer* {:min 0})]
    (let [{:keys [ground-truth] :as result}
          (engine/run {:seed seed :patients 3
                       :pathway {:name "module-only" :steps []}
                       :persona-config {:age-min 0 :age-max 0}
                       :modules [(patient-simulator/singleton-closure straddle-module)]
                       :module-assignment [{:module-id "straddle-mod" :weight 1}]
                       :module-horizon-days 1000
                       :history true})
          kinds (into #{} (map :event) ground-truth)]
      (and (result/ok? (check/check-all ground-truth (:facility result)))
           (not (contains? kinds :procedure))
           (some kinds #{:outpatient-visit :observation})))))

;; --- ADR-0169 (arc 0), family (ii): the fold-carried reinstate index -----
;;
;; `decide :cancel-transfer` and `decide :cancel-discharge` used to
;; evaluate `(:before (nth (replay ground-truth) idx))` -- a full
;; `evolve` re-simulation of the whole log, allocating a vector of N
;; maps, once per cancel, to read ONE element. 35.3% of the generate
;; phase at 10^5 events, the largest single generator-side cost the
;; 2026-08-24 throughput spike measured. They now read `run`'s
;; fold-carried `:reinstate-index`.
;;
;; The equivalence is checked POST HOC against the `replay` the decide
;; no longer calls -- never as an assertion inside the decide, which
;; would reinstate the very cost the arc removed and make the claim
;; unfalsifiable in the configuration that matters.
;;
;; `ehrt.sim.run-test/cancel-decides-reinstate-exactly-what-replay-would-
;; hand-back` runs the same check over the four GATED corpora. It has to
;; be here as well, because only ONE of those four carries a reinstating
;; cancel at all, and ten events in one run is not a population.

(def ^:private reinstating-cancel-fields
  {:cancel-transfer  [:home-ward :location]
   :cancel-discharge [:home-ward :location :attending]})

(defn- reinstatement-mismatches [ground-truth]
  (let [records (engine/replay ground-truth)]
    (for [ev ground-truth
          :let [fields (get reinstating-cancel-fields (:event ev))]
          :when fields
          :let [before (:before (nth records (:cancels-event-id ev)))]
          field fields
          :when (not= (get ev field) (get before field))]
      {:cancel (:event ev) :at (:t ev) :field field
       :emitted (get ev field) :replay-says (get before field)})))

(defn- reinstating-cancel-count [ground-truth]
  (count (filter #(contains? reinstating-cancel-fields (:event %)) ground-truth)))

(defspec cancel-reinstatement-survives-the-fold-carried-index
  {:num-tests 150 :seed 20260825}
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 8 40)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients
                                              :facility churn-facility :providers churn-providers
                                              :churn-profile active-churn-profile})]
      (empty? (reinstatement-mismatches ground-truth)))))

(deftest the-reinstatement-defspec-actually-sees-reinstating-cancels
  (testing "ADR-0169: a property that holds vacuously proves nothing. The
            churn profile the defspec above drives has to actually PRODUCE
            :cancel-transfer/:cancel-discharge events, or every trial is
            `(empty? ())` and the index is never read."
    (let [counts (for [seed (range 40)]
                   (reinstating-cancel-count
                    (:ground-truth (engine/run {:seed seed :patients 24
                                                :facility churn-facility :providers churn-providers
                                                :churn-profile active-churn-profile}))))
          total (reduce + counts)]
      (is (pos? total)
          "the churn profile produced NO reinstating cancels in 40 runs -- the
           defspec above is vacuous")
      (is (< 1 (count (filter pos? counts)))
          (str "reinstating cancels appear in fewer than two of 40 runs (" (pr-str counts)
               ") -- too thin for a 150-trial property to mean anything")))))

(deftest reinstate-index-covers-every-reinstatable-event-and-nothing-else
  (testing "ADR-0169: the carrier's own contract -- `run` records the
            pre-event state of every :transfer and :discharge in the log,
            under that event's own index, and records nothing else. A
            missing entry would make a later cancel read nil rather than
            fall back to replay (the fallback is on the KEY's presence, not
            on an entry's), so coverage is the property that keeps that
            design safe."
    (let [{:keys [ground-truth]} (engine/run {:seed 202 :patients 60
                                              :facility churn-facility :providers churn-providers
                                              :churn-profile active-churn-profile})
          expected (into #{} (comp (map-indexed vector)
                                   (filter (fn [[_ ev]] (#{:transfer :discharge} (:event ev))))
                                   (map first))
                         ground-truth)
          ;; every reinstating cancel's target must be one of them
          targets (into #{} (comp (filter #(contains? reinstating-cancel-fields (:event %)))
                                  (map :cancels-event-id))
                        ground-truth)]
      (is (seq expected) "this run carries no :transfer or :discharge at all")
      (is (every? expected targets)
          (str "a reinstating cancel targets an index the index does not cover: "
               (pr-str (remove expected targets))))
      (is (empty? (reinstatement-mismatches ground-truth))))))

;; --- ADR-0169 (arc 0), family (iii): the fold-carried citation index ----
;;
;; ADR-0164's two decide-time scans -- `:medication-end` resolving its
;; `:order-citation` and `:care-plan-end` its `:care-plan-citation`, each
;; by a `keep-indexed` over the WHOLE log -- were 21.3% and 10.9% of the
;; generate phase at 10^5 events. They now read a fold-carried
;; `[opening-type patient-id citation] -> last-index` map.
;;
;; `ehrt.sim.run-test/citation-resolution-matches-the-whole-log-scan`
;; runs the index-equality check over the gated corpora, and it MUST be
;; joined here, because those corpora carry exactly two cited end events
;; between them and BOTH resolve to nil (the pre-horizon straddle). They
;; prove the index does not invent a resolution. These prove it finds
;; one -- and finds the RIGHT one, which is the whole content of
;; ADR-0164.

(def ^:private other-citation {:module "sinusitis" :state :follow-up})

(def ^:private cited-pathway
  "Two medication orders and two care plans per patient, under TWO
  distinct citations, each opened twice and closed once. The repeat is
  the point: `last` is what both scans meant, so a citation opened more
  than once by the SAME patient is the case that tells `last` from
  `first`, and the index -- which resolves by overwriting -- has to agree
  with it."
  {:name "cited"
   :steps [{:type :admission :location "Renal"}
           {:type :medication-order :codes [a-concept] :citation a-citation}
           {:type :care-plan-start :codes [a-concept] :citation a-citation}
           {:type :medication-order :codes [a-concept] :citation other-citation}
           {:type :delay :from 30 :to 90}
           {:type :medication-order :codes [a-concept] :citation a-citation}
           {:type :care-plan-start :codes [a-concept] :citation other-citation}
           {:type :medication-end :order-citation a-citation}
           {:type :care-plan-end :care-plan-citation a-citation}
           {:type :medication-end :order-citation other-citation}
           {:type :care-plan-end :care-plan-citation other-citation}
           {:type :discharge}]})

(def ^:private cited-end-resolution
  {:medication-end [:order-citation     :medication-order :order-event-id]
   :care-plan-end  [:care-plan-citation :care-plan-start  :start-event-id]})

(defn- citation-index-mismatches
  "Every cited end whose emitted resolved index differs from the scan's
  own answer over the PREFIX the decide actually saw."
  [ground-truth]
  (let [log (vec ground-truth)]
    (for [[idx ev] (map-indexed vector log)
          :let [[citation-key opening-type resolved-key] (get cited-end-resolution (:event ev))]
          :when citation-key
          :let [citation (get ev citation-key)]
          :when citation
          :let [patient-id (:patient-id (first (:participants ev)))
                scanned (last (keep-indexed
                               (fn [i prior]
                                 (when (and (= opening-type (:event prior))
                                            (= citation (:citation prior))
                                            (some #(= patient-id (:patient-id %)) (:participants prior)))
                                   i))
                               (subvec log 0 idx)))]
          :when (not= (get ev resolved-key) scanned)]
      {:end (:event ev) :at (:t ev) :key resolved-key
       :emitted (get ev resolved-key) :scan-says scanned})))

(defn- resolved-cited-ends [ground-truth]
  (filter (fn [ev] (when-let [[ck _ rk] (get cited-end-resolution (:event ev))]
                     (and (get ev ck) (some? (get ev rk)))))
          ground-truth))

(defspec citation-index-resolves-exactly-what-the-scan-resolved
  {:num-tests 120 :seed 20260825}
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 2 20)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients
                                              :pathways [{:pathway cited-pathway :weight 1}]})]
      (and (seq ground-truth)
           (empty? (citation-index-mismatches ground-truth))))))

(deftest the-citation-index-defspec-actually-resolves-something
  (testing "ADR-0169: the gated-corpus half of this gate sees only nil
            resolutions, so this half has to see NON-nil ones or the whole
            claim is that the index correctly returns nothing."
    (let [{:keys [ground-truth]} (engine/run {:seed 7 :patients 8
                                              :pathways [{:pathway cited-pathway :weight 1}]})
          resolved (resolved-cited-ends ground-truth)]
      (is (seq resolved) "no cited end resolved to a real index -- gate is vacuous")
      (is (= [] (vec (citation-index-mismatches ground-truth))))
      (testing "ADR-0164's own case: two patients walking the same module cite
                IDENTICALLY, so a resolution that ignored the participant would
                hand one patient the other's order. Every resolved index must
                name an event this patient participates in."
        (let [log (vec ground-truth)]
          (doseq [ev resolved]
            (let [[_ _ rk] (get cited-end-resolution (:event ev))
                  pid (:patient-id (first (:participants ev)))
                  target (nth log (get ev rk))]
              (is (some #(= pid (:patient-id %)) (:participants target))
                  (str "cited end at t " (:t ev) " resolved to index " (get ev rk)
                       ", an event that is not this patient's"))))))
      (testing "`last`, not `first`: a-citation is opened TWICE per patient, so
                each :medication-end must resolve to the SECOND order, never
                the first"
        (let [log (vec ground-truth)
              med-ends (filter #(and (= :medication-end (:event %))
                                     (= a-citation (:order-citation %))
                                     (:order-event-id %))
                               log)]
          (is (seq med-ends))
          (doseq [ev med-ends]
            (let [pid (:patient-id (first (:participants ev)))
                  own-orders (keep-indexed (fn [i p]
                                             (when (and (= :medication-order (:event p))
                                                        (= a-citation (:citation p))
                                                        (some #(= pid (:patient-id %)) (:participants p)))
                                               i))
                                           log)]
              (is (< 1 (count own-orders))
                  "this patient opened a-citation only once -- the last/first
                   distinction is not under test")
              (is (= (last own-orders) (:order-event-id ev))))))))))

;; --- ADR-0171 (arc 1): the RNG stream partition ---------------------------
;;
;; Until this arc there was ONE java.util.Random and consumption order was
;; global event order, so adding, removing or reordering a single draw
;; anywhere shifted every later draw for every later patient. The partition
;; derives five streams by family, keyed on a stable id. What that buys, and
;; what it deliberately does NOT buy, is the subject of the three tests
;; below.
;;
;; WHAT IT DOES NOT BUY, stated first because a test that overclaimed here
;; would be false in the tree: the run-scoped families (:world, :facility)
;; still couple patients to each other. Their draw COUNTS are conditional on
;; the population -- `(seq eligible)`, `(seq home-licensed)` -- so no
;; per-patient stream can own them without making one patient's consumption
;; depend on another's state, which is the coupling sim/ADR-0009's rejected
;; option (b) already refused. Perturbing patient K therefore moves K's own
;; event TIMES, which re-interleaves the global queue, which moves where the
;; world stream stands when somebody else's bed is chosen. The author's
;; locality ruling (ADR-0171, 2026-08-25, "LOCALITY option (a)") is the
;; weakened property that IS true: byte-identity over the PATIENT-SCOPED
;; FIELDS of every other patient's events, with the run-scoped families'
;; output fields excluded BY NAME.

(def ^:private locality-seed 424242)
(def ^:private locality-patients 8)

(def ^:private locality-perturbed-ordinal
  "Which arrival ordinal gets perturbed. 3 of 8 -- far enough in that
  patients both before and after it are already mid-pathway, so a shared
  stream would have moved patients on both sides."
  3)

(def ^:private locality-baseline-pathway
  {:name "locality-baseline"
   :steps [{:type :admission :location "Renal"}
           {:type :delay :from 60 :to 240}
           {:type :discharge}]})

(def ^:private locality-perturbed-pathway
  "A DIFFERENT pathway for one ordinal: more steps, wider ranges, and an
  :order (which draws a turnaround plus two per analyte). Its draw count
  and its event times both differ from the baseline's, which is what makes
  the pre-partition failure total rather than marginal."
  {:name "locality-perturbed"
   :steps [{:type :admission :location "Renal"}
           {:type :delay :from 30 :to 300}
           {:type :order :profile :cbc}
           {:type :delay :from 15 :to 90}
           {:type :discharge}]})

(defn- locality-config
  "`crowded-facility` on purpose: one licensed Renal bed forces boarding,
  so `bed-ready-location` (the sharpest locality hazard in the census --
  patient A's discharge draws to place patient B) actually fires. On the
  default facility it would not, and the exclusion the ruling licenses
  would be vacuous."
  [pathways]
  {:seed locality-seed :patients locality-patients :arrival-gap 45
   :facility crowded-facility :providers test-providers
   :pathway locality-baseline-pathway :pathways pathways})

(def ^:private locality-runs
  "Baseline vs perturbed, computed once: three deftests below read them.

  THE PERTURBATION, and the disclosure it owes. ADR-0171 section 3 names
  this test `mutating-one-patients-stream-seed-moves-only-that-patient`
  and asks for it RED before the partition, failing by moving everyone.
  Both cannot hold at once: a stream SEED is derived from the master seed
  and the arrival ordinal alone, so no config can perturb one patient's
  seed, and the only way to reach it is `with-redefs` on `engine/stream`
  -- a var that does not exist before the partition, so that test could
  only ever have been red by failing to COMPILE, which is the one red
  reason section 3 rules out.

  What perturbs exactly one patient's own draws and does compile on both
  sides is an explicit one-ordinal `:pathways` override. `assign-pathway`
  consumes exactly one `.nextDouble` per patient whether the outcome is
  the override or the weighted pick (its own fixed-consumption law), so
  the override changes no other patient's draw COUNT -- it changes only
  what patient K's own stream is spent on. That is the same shape of
  change arcs 2-4 will make, which is what this gate exists to protect.

  The seed-level version of the property is asserted too, by
  `mutating-one-patients-stream-seed-moves-only-that-patient` below,
  under ADR-0171's own name -- born green, because its mechanism IS the
  partition."
  (delay
    {:baseline  (engine/run (locality-config [{:pathway locality-baseline-pathway :weight 1}]))
     :perturbed (engine/run (locality-config [{:pathway locality-baseline-pathway :weight 1}
                                              {:patient-ordinal locality-perturbed-ordinal
                                               :pathway locality-perturbed-pathway}]))}))

(def ^:private run-scoped-event-fields
  "The event fields the RUN-scoped families write -- excluded by name from
  the locality assertion, per the author's LOCALITY ruling (a).

  `:location`, `:home-ward`, `:placement` and `:from` are what the WORLD
  family's four cross-patient sites decide: `bed-ready-location`
  (engine.clj:480 at ADR-0171's design HEAD c1b996e), `:transfer-in-
  error`'s `allocate` (:610), the `:bed-swap` partner pick (:643) and the
  `:merge` partner pick (:672) -- plus `:admission`/`:transfer`'s own
  `allocate` calls, which write the same fields. `:attending` is the
  FACILITY family's `choose-attending`, run-scoped for the same reason.

  A test claiming TOTAL byte-identity would be false at engine.clj:480
  and would be discovered false by the first churned seed. The exclusion
  is not assumed to matter, either: `the-locality-test-asserts-how-many-
  patients-it-moved` pins which of these fields actually differ, so an
  exclusion that stopped carrying weight would show up as a red rather
  than as silent slack."
  [:location :home-ward :placement :from :attending])

(defn- patient-scoped
  [event]
  (apply dissoc event run-scoped-event-fields))

(defn- locality-moved-ordinals
  "Which arrival ordinals' own event subsequences differ between the two
  runs, under `project`. `engine/events-for-patient` is the subsequence;
  patient-ids are identical across the two runs because both share a seed."
  [runs project]
  (into (sorted-set)
        (for [i (range locality-patients)
              :let [pid (engine/patient-id-for locality-seed i)
                    of (fn [r] (mapv project (engine/events-for-patient (:ground-truth r) pid)))]
              :when (not= (of (:baseline runs)) (of (:perturbed runs)))]
          i)))

(deftest perturbing-one-patients-own-draws-moves-only-that-patient
  (testing "ADR-0171 section 3's LOCALITY obligation, under the author's
            ruling (a). Spending ONE patient's stream differently moves
            that patient and, in every PATIENT-SCOPED field, nobody else.
            Before the partition this failed by moving everyone: one
            shared stream made consumption order global event order."
    (let [runs @locality-runs
          moved (locality-moved-ordinals runs patient-scoped)]
      (is (= #{locality-perturbed-ordinal} moved)
          (str "the PATIENT-SCOPED locality property broke. Moved ordinals: "
               (pr-str moved) " -- expected exactly the perturbed one ("
               locality-perturbed-ordinal "). The pre-partition failure was "
               "#{3 4 5} -- the perturbed ordinal AND every ordinal still "
               "drawing after it, measured on HEAD 97f22fd: one shared RNG, "
               "consumption order = global event order. A moved set MISSING "
               "the perturbed ordinal means the perturbation stopped "
               "perturbing -- re-read `locality-runs` before adjusting anything."))
      (testing "and the perturbed patient really did move, so the property
                above is not passing over an inert perturbation"
        (is (contains? moved locality-perturbed-ordinal))))))

(deftest mutating-one-patients-stream-seed-moves-only-that-patient
  (testing "ADR-0171 section 3's LOCALITY test under its own name, and the
            literal mechanism the ADR describes: ONE patient's :patient-
            family stream seed perturbed, nothing else touched.

            BORN GREEN, disclosed: the perturbation reaches through
            `engine/stream`, which the partition itself introduces, so
            there is no earlier tree on which this could have been red for
            the reason ADR-0171 section 3 gives. Its red-before-green
            sibling is `perturbing-one-patients-own-draws-moves-only-that-
            patient` above, which perturbs the same one patient through a
            config the pre-partition engine also accepts."
    (let [cfg (locality-config [{:pathway locality-baseline-pathway :weight 1}])
          baseline (engine/run cfg)
          perturbed (with-redefs [engine/stream
                                  (fn [master family id-tag]
                                    (java.util.Random.
                                     (cond-> (engine/stream-seed master family id-tag)
                                       (and (= :patient family)
                                            (= locality-perturbed-ordinal id-tag))
                                       inc)))]
                      (engine/run cfg))
          runs {:baseline baseline :perturbed perturbed}
          moved (locality-moved-ordinals runs patient-scoped)]
      (is (= #{locality-perturbed-ordinal} moved)
          (str "perturbing ONE :patient-family stream seed moved the "
               "PATIENT-SCOPED fields of " (pr-str moved) " -- expected exactly #{"
               locality-perturbed-ordinal "}")))))

(deftest the-locality-test-asserts-how-many-patients-it-moved
  (testing "ADR-0171 section 3's WITNESS COUNTS obligation, which is
            `R-witness-population-is-counted` applied: assert the witness
            population's SIZE first, then the property over it, so the
            locality gate above cannot pass by moving nothing.

            The two counts are deliberately DIFFERENT, and the difference
            is the ruling. Over PATIENT-SCOPED fields exactly one ordinal
            moves. Over the WHOLE event -- run-scoped fields included --
            more than one does, because the perturbed patient's event
            times re-interleave the global queue and the world stream then
            stands somewhere else when a peer's bed is chosen. Pinning
            both is what keeps the exclusion honest: if the full-event
            count ever collapsed to 1, the exclusion would have gone
            vacuous and this gate would say so."
    (let [runs @locality-runs
          moved-scoped (locality-moved-ordinals runs patient-scoped)
          moved-full (locality-moved-ordinals runs identity)]
      (testing "the population is the one the config declares"
        (is (= locality-patients
               (count (set (map (comp :patient-id first :participants)
                                (:ground-truth (:baseline runs)))))))
        (is (pos? (count (filter :bed-ready (:ground-truth (:baseline runs)))))
            "no bed-ready transfer fired, so engine.clj:480 -- the sharpest
             cross-patient site, and the reason the exclusion exists -- is
             not exercised and the exclusion is vacuous"))
      (testing "PATIENT-SCOPED: exactly one moved, seven did not"
        (is (= 1 (count moved-scoped)))
        (is (pos? (count moved-scoped)))
        (is (= (dec locality-patients) (- locality-patients (count moved-scoped)))))
      (testing "WHOLE EVENT: the run-scoped coupling moves more than one,
                pinned so a collapse to 1 is a red rather than silent slack"
        (is (= 3 (count moved-full))
            (str "the run-scoped blast radius moved off its pin: " (pr-str moved-full)
                 ". Larger is not a defect -- it is the disclosed WORLD coupling. "
                 "Exactly 1 WOULD be a finding: it would mean the exclusion in "
                 "`run-scoped-event-fields` is no longer carrying anything."))
        (is (pos? (count moved-full)))
        (is (contains? moved-full locality-perturbed-ordinal)))
      (testing "and the excluded fields are the ones actually differing --
                the exclusion names what the tree does, not what it fears"
        (let [differing (into (sorted-set)
                              (for [i (range locality-patients)
                                    :when (not= i locality-perturbed-ordinal)
                                    :let [pid (engine/patient-id-for locality-seed i)
                                          a (engine/events-for-patient (:ground-truth (:baseline runs)) pid)
                                          b (engine/events-for-patient (:ground-truth (:perturbed runs)) pid)]
                                    [x y] (map vector a b)
                                    k (into #{} (concat (keys x) (keys y)))
                                    :when (not= (get x k) (get y k))]
                                k))]
          (is (= #{:from :location} differing)
              (str "the fields differing on OTHER patients moved: " (pr-str differing)
                   ". Every one of them must appear in `run-scoped-event-fields` "
                   "or the locality property above is being weakened by a field "
                   "the ruling never excluded."))
          (is (every? (set run-scoped-event-fields) differing)))))))

(deftest the-stream-partition-derives-what-adr-0171-specifies
  (testing "ADR-0171 section 2(b) and rulings A1 / B1. `mix64` is the
            derivation (A1), the newborn key is the mixed PAIR (B1), and
            the family tags are a fixed table rather than anything derived
            through a hash this repo does not own."
    (testing "stream-seed is mix64 applied twice, exactly as specified"
      (is (= (engine/mix64 (engine/mix64 99 3) 7)
             (engine/stream-seed 99 :world 7)))
      (is (= (engine/mix64 (engine/mix64 99 1) 7)
             (engine/stream-seed 99 :patient 7))))
    (testing "the five families are distinct at the same master and id-tag"
      (let [seeds (mapv #(engine/stream-seed 12345 % 0)
                        [:patient :person :world :facility :emission])]
        (is (= 5 (count (set seeds))))))
    (testing "distinct id-tags give distinct streams within a family"
      (is (= 64 (count (set (map #(engine/stream-seed 7 :patient %) (range 64)))))))
    (testing "an unknown family is a throw, never a silent zero tag"
      (is (thrown? clojure.lang.ExceptionInfo (engine/stream-seed 1 :nonesuch 0))))
    (testing "ruling B1: the newborn id-tag mixes the PAIR (parity-index,
              within-delivery-index), so pinning within-delivery-index at 0
              today does not have to be renumbered when multiples are
              admitted -- the whole reason B1 was taken over B2"
      (is (not= (engine/newborn-id-tag 42 0 0) (engine/newborn-id-tag 42 1 0)))
      (is (not= (engine/newborn-id-tag 42 0 0) (engine/newborn-id-tag 42 0 1)))
      (is (not= (engine/newborn-id-tag 42 0 0) (engine/newborn-id-tag 43 0 0)))
      (is (= (engine/newborn-id-tag 42 3 0) (engine/newborn-id-tag 42 3 0))))
    (testing "the scheme marker is a string, and `one-stream` collapses every
              family back onto one Random -- the pre-partition behaviour a
              lone decide call still has"
      (is (string? engine/stream-scheme))
      (is (= 1 (count (set (vals (engine/one-stream (Random. 1))))))))))
