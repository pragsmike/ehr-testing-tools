(ns ehrt.sim-engine.churn-scenarios-test
  "M2b's scripted regression fleet: authored, hand-driven scenarios
  exercising the compound cases the catalog names --
  docs/clinical-realities.md's newborn-merge entry (merge-while-
  boarding), transfer-in-error then cancel-then-retransfer, and a
  bed-swap between a licensed and a surge occupant.

  M3-adjacent (roadmap.md's per-patient pathway assignment): migrated to
  run end-to-end through `ehrt.sim-engine.run/run`'s :pathways option
  -- each scenario is now an explicit {:patient-ordinal :pathway}
  assignment per participant, exercising the REAL event loop (arrivals,
  the work queue, decide/evolve folding) rather than hand-driving
  decide/evolve one call at a time. `:with` fields that name a specific
  peer patient-id are computed via `engine/patient-id-for` -- a PURE
  function of this run's own seed and arrival ordinal (sim/ADR-0010), so a
  scripted scenario can name 'the 3rd patient' without engine/run having
  run yet. `:arrival-gap 0` makes every patient arrive at t=0; the queue's
  own seq-no tiebreak (assigned in arrival-ordinal order, then
  monotonically for every re-queued step) is what still guarantees each
  scenario's prerequisite admissions are fully processed before any
  patient's SECOND step fires -- every prerequisite patient's pathway
  here is exactly one step (:admission) long, so its only queue entry is
  its ORIGINAL arrival (seq < patient count); any patient's second step
  is re-queued with a freshly assigned seq (>= patient count, monotonic)
  by construction, so it can never be processed before any first-round
  arrival. See ehrt.sim-engine.engine-test's own
  bed-ready-transfer-scripted-two-patients for the ONE test this session
  deliberately keeps as a direct decide/evolve-driven API-level
  regression, per the roadmap's own migration note -- not everything
  needs to move to engine/run, just this scripted fleet."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.streams :as streams]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]
            [com.nervestaple.hl7-parser.parser :as parser]
            [com.nervestaple.hl7-parser.message :as message]))

(def ^:private ref-date "2024-01-01")
(def ^:private utc-offset "+00:00")

(def ^:private one-bed-one-surge-facility
  "Renal: exactly one licensed bed and one surge slot -- a second
  admission takes the surge slot (bed-swap fixture); a THIRD admission
  boards in ED (merge-while-boarding fixture)."
  {:id :scenario-fleet
   :wards [{:id :ed :name "ED" :beds 0 :surge-slots 4
            :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 1 :surge-slots 1
            :surge-format "%s-H%02d" :class :inpatient}]})

(def ^:private provider-templates
  [{:name {:family "Chen" :given "A"} :role :attending
    :specialty "Nephrology" :wards [:renal :ed]}])

(defn- clinical-events
  "M4: strips :registered events -- every patient's now-automatic first
  event (Persona, decide.clj's own docstring) -- before asserting an
  EXACT ground-truth sequence, so these scenarios keep asserting the
  churn-relevant shape they were written for rather than every
  positional index shifting by one per patient. :registered never
  renders a message (no message-type-registry entry, same treatment
  :step-rejected gets), so the `triggers` sequence below is already
  unaffected and needs no equivalent filter."
  [ground-truth]
  (vec (remove #(= :registered (:event %)) ground-truth)))

(defn- triggers
  "The ordered A-trigger sequence a ground-truth log renders to, for
  compact exact-sequence assertions."
  [ground-truth facility providers]
  (mapv #(second (re-find #"\^(A\d+)" %))
        (emit-hl7/emit ground-truth ref-date utc-offset facility providers)))

(defn- run-scenario
  "Runs `pathways-by-ordinal` (a seq of pathway IR maps, one per patient
  in arrival order) end-to-end through engine/run, with churn OFF (these
  scenarios author their own churn steps directly in the IR -- no
  InjectChurn needed) and arrivals collapsed to t=0 (see namespace
  docstring)."
  [seed pathways-by-ordinal]
  (run/run {:seed seed
               :patients (count pathways-by-ordinal)
               :arrival-gap 0
               :facility one-bed-one-surge-facility
               :providers provider-templates
               :pathways (vec (map-indexed (fn [i p] {:patient-ordinal i :pathway p})
                                            pathways-by-ordinal))}))

;; --- Scenario 1: merge-while-boarding --------------------------------------
;; docs/clinical-realities.md's newborn Babyboy/Babygirl entry: "a natural
;; churn generator... merge steps get an organic scenario" -- here scripted
;; directly rather than emerging from a pathway, but the same shape: a
;; patient created while boarding gets merged into another identity.

(deftest merge-while-boarding
  ;; Renal has one licensed bed and one surge slot: P1 takes the licensed
  ;; bed (rung 1), P2 takes the surge slot (rung 2, still home-ward Renal,
  ;; NOT boarding), and P3 -- with both Renal rungs now exhausted -- boards
  ;; in ED surge (rung 4: home-ward Renal, physically ED). P3 is then
  ;; merged into P1 while still boarding.
  (let [seed 100
        p3-id (streams/patient-id-for seed 2)
        {:keys [ground-truth]} (run-scenario
                                 seed
                                 [{:name "p1" :steps [{:type :admission :location "Renal"}
                                                       {:type :merge :with p3-id}]}
                                  {:name "p2" :steps [{:type :admission :location "Renal"}]}
                                  {:name "p3" :steps [{:type :admission :location "Renal"}]}])
        p3-before-merge (first (filter #(and (= :admission (:event %))
                                              (= p3-id (:patient-id (first (:participants %)))))
                                        ground-truth))
        survivor (last (filter #(and (= :merge (:event %))) ground-truth))]
    (testing "sanity: P3 is genuinely boarding at this point"
      (is (= "Renal" (get-in p3-before-merge [:home-ward])))
      (is (= "ED" (get-in p3-before-merge [:location :ward]))))
    (testing "exact ground-truth event sequence"
      (is (= [:admission :admission :admission :merge] (mapv :event (clinical-events ground-truth)))))
    (testing "the merge names P3 as :merged, P1 as :survivor"
      (is (= #{[:survivor (streams/patient-id-for seed 0)] [:merged p3-id]}
             (set (map (juxt :role :patient-id) (:participants survivor))))))
    (testing "exact rendered message sequence: A01 A01 A01 A40"
      (is (= ["A01" "A01" "A01" "A40"]
             (triggers ground-truth one-bed-one-surge-facility provider-templates))))
    (testing "the A40 carries PID=survivor, MRG=merged's prior mrn"
      (let [a40 (last (emit-hl7/emit ground-truth ref-date utc-offset one-bed-one-surge-facility provider-templates))
            parsed (parser/parse a40)]
        (is (= "MRN000001" (message/get-field-first-value parsed "PID" 3)))
        (is (= "MRN000003" (message/get-field-first-value parsed "MRG" 1)))))))

;; --- Scenario 2: transfer-in-error, then cancel-then-retransfer -----------

(deftest transfer-in-error-then-cancel-then-retransfer
  (let [seed 100
        {:keys [ground-truth]} (run-scenario
                                 seed
                                 [{:name "p1" :steps [{:type :admission :location "Renal"}
                                                       ;; transfer-in-error: an erroneous
                                                       ;; transfer to ED, corrected atomically
                                                       {:type :transfer-in-error :location "ED"}
                                                       ;; a genuine, INTENDED transfer to ED
                                                       ;; follows -- the "retransfer"
                                                       {:type :transfer :location "ED"}]}])
        events (clinical-events ground-truth)]
    (testing "exact ground-truth event sequence: admission, transfer(err), cancel-transfer(err), transfer(real)"
      (is (= [:admission :transfer :cancel-transfer :transfer] (mapv :event events)))
      (is (= [nil true nil] (mapv :in-error (rest events)))))
    (testing "the in-error correction left the patient exactly where they started, and the retransfer is a real move"
      (is (= "Renal" (get-in (nth events 2) [:home-ward])))
      (is (= "Renal" (get-in (nth events 2) [:location :ward])))
      (is (= "ED" (get-in (nth events 3) [:location :ward]))))
    (testing "exact rendered message sequence: A01 A02 A12 A02"
      (is (= ["A01" "A02" "A12" "A02"]
             (triggers ground-truth one-bed-one-surge-facility provider-templates))))))

;; --- Scenario 3: bed-swap between a licensed and a surge occupant ---------

(deftest bed-swap-between-licensed-and-surge-occupant
  (let [seed 100
        p2-id (streams/patient-id-for seed 1)
        {:keys [ground-truth]} (run-scenario
                                 seed
                                 [{:name "p1" :steps [{:type :admission :location "Renal"}
                                                       {:type :bed-swap :with p2-id}]}
                                  {:name "p2" :steps [{:type :admission :location "Renal"}]}])
        [p1-admit p2-admit bed-swap] (clinical-events ground-truth)]
    (testing "sanity: one licensed, one surge, same ward"
      (is (= :licensed (get-in p1-admit [:location :placement])))
      (is (= :surge (get-in p2-admit [:location :placement]))))
    (testing "exact ground-truth event sequence"
      (is (= [:admission :admission :bed-swap] (mapv :event (clinical-events ground-truth)))))
    (testing "placements are exchanged"
      (is (= (:location p2-admit) (get-in bed-swap [:swap (streams/patient-id-for seed 0) :to])))
      (is (= (:location p1-admit) (get-in bed-swap [:swap p2-id :to]))))
    (testing "exact rendered message sequence: A01 A01 A17"
      (is (= ["A01" "A01" "A17"]
             (triggers ground-truth one-bed-one-surge-facility provider-templates))))))
