(ns ehrt.sim-check.check-test
  "The invariant catalog's Milestone M1 additions (docs/operational-
  models.md, docs/patient-state-model.md's event-validity table):
  admission/transfer legality, transfer-from accuracy, no double
  occupancy, one-slot-per-admitted-patient, capacity, and surge-only-
  when-earlier-rungs-exhausted. Written before ehrt.sim-check.check
  grows these (sim/ADR-0004 test-first).

  M2a (sim/ADR-0010) additions: every hand-written log below now carries
  :participants (the fold-routing mechanism replay/check.clj need since
  :mrn is no longer the fold key), plus the two structural invariants
  sim/ADR-0010 requires: every event has >=1 participant, and every
  participant id traces back to an :admission in the same log."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.java.io :as io]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.order-profiles :as order-profiles]
            [ehrt.patient-simulator.interface :as patient-simulator]))

(def test-facility
  {:id :t :wards [{:id :ed :name "ED" :beds 0 :surge-slots 4
                   :surge-format "%s-H%02d" :class :ed}
                  {:id :renal :name "Renal" :beds 1 :surge-slots 1
                   :surge-format "%s-H%02d" :class :inpatient}]})

(defn- subject
  "Test-fixture convenience: a single-participant :participants vector."
  [patient-id]
  [{:patient-id patient-id :role :subject}])

;; --- Event-validity rows (patient-state-model.md) -----------------------

(deftest admission-only-when-new-detects-double-admission
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :admission :t 10 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-02" :placement :licensed}}]]
    (is (seq (check/admission-only-when-new log)))))

(deftest admission-only-when-new-holds-for-legit-log
  (is (empty? (check/admission-only-when-new
               [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
                 :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
                {:event :discharge :t 10 :participants (subject "P1")}]))))

(deftest transfer-only-when-admitted-detects-transfer-before-admission
  (let [log [{:event :transfer :t 0 :home-ward "Renal" :participants (subject "P1")
              :from nil :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]]
    (is (seq (check/transfer-only-when-admitted log)))))

(deftest transfer-from-matches-state-detects-lying-event
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :transfer :t 10 :home-ward "Cardiology" :participants (subject "P1")
              :from {:ward "WRONG" :bed "WRONG-01" :placement :licensed}
              :location {:ward "Cardiology" :bed "CARDIOLOGY-01" :placement :licensed}}]]
    (is (seq (check/transfer-from-matches-state log)))))

(deftest transfer-from-matches-state-holds-when-honest
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :transfer :t 10 :home-ward "Cardiology" :participants (subject "P1")
              :from {:ward "Renal" :bed "RENAL-01" :placement :licensed}
              :location {:ward "Cardiology" :bed "CARDIOLOGY-01" :placement :licensed}}]]
    (is (empty? (check/transfer-from-matches-state log)))))

;; --- Occupancy invariants ------------------------------------------------

(deftest no-double-occupancy-detects-collision
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :admission :t 5 :home-ward "Renal" :participants (subject "P2")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]]
    (testing "ADR-0169's equivalence gate: the FULL finding map, not merely
              non-emptiness -- \"identical findings\" is a claim about content
              and order, so a discrimination test that only asserts `seq`
              cannot witness it."
      (is (= [{:invariant :no-double-occupancy :bed "RENAL-01" :at 5}]
             (check/no-double-occupancy log))))))

(deftest admitted-occupies-one-slot-detects-nil-location
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1") :location nil}]]
    (is (= [{:invariant :admitted-occupies-one-slot :patient-id "P1" :at 0}]
           (check/admitted-occupies-one-slot log)))))

(deftest occupancy-within-capacity-detects-overflow
  (let [ward {:id :renal :name "Renal" :beds 1 :surge-slots 0
              :surge-format "%s-H%02d" :class :inpatient}
        facility {:id :t :wards [ward]}
        ;; two patients both claim they hold licensed beds in a 1-bed
        ;; ward -- an impossible log a bug could still produce.
        log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :admission :t 5 :home-ward "Renal" :participants (subject "P2")
              :location {:ward "Renal" :bed "RENAL-99" :placement :licensed}}]]
    (is (= [{:invariant :occupancy-within-capacity :ward "Renal" :at 5
             :occupied 2 :capacity 1}]
           (check/occupancy-within-capacity log facility)))))

(deftest surge-only-when-earlier-rungs-exhausted-detects-premature-surge
  (let [facility test-facility
        ;; Renal has a free licensed bed, yet this admission claims surge.
        log [{:event :admission :t 0 :home-ward "Renal" :forced false :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-H01" :placement :surge}}]]
    (is (seq (check/surge-only-when-earlier-rungs-exhausted log facility)))))

(deftest surge-only-when-earlier-rungs-exhausted-allows-forced
  (let [facility test-facility
        log [{:event :admission :t 0 :home-ward "Renal" :forced true :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-H01" :placement :surge}}]]
    (is (empty? (check/surge-only-when-earlier-rungs-exhausted log facility)))))

;; --- sim/ADR-0010: structural participant invariants -------------------------

(deftest every-event-has-participants-detects-empty-participants
  (is (seq (check/every-event-has-participants
            [{:event :admission :t 0 :home-ward "Renal" :participants []
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]))))

(deftest every-event-has-participants-holds-when-present
  (is (empty? (check/every-event-has-participants
               [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
                 :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]))))

(deftest participant-ids-exist-in-run-detects-unadmitted-participant
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             ;; P2 never appears in an :admission -- a stray/mistyped id.
             {:event :discharge :t 10 :participants (subject "P2")}]]
    (is (seq (check/participant-ids-exist-in-run log)))))

(deftest participant-ids-exist-in-run-holds-for-legit-log
  (let [log [{:event :registered :t 0 :participants (subject "P1")}
             {:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :discharge :t 10 :participants (subject "P1")}]]
    (is (empty? (check/participant-ids-exist-in-run log)))))

(deftest participant-ids-exist-in-run-recognizes-registered-alone-as-proof
  (testing "M5b: a module-assigned patient can legitimately have NO
            operational encounter at all within this run's own horizon
            (their disease process may never produce one in the
            configured window) -- :registered is the one event type
            EVERY real patient this run creates always gets (M4), so it
            is sufficient proof on its own, without requiring an
            :admission/:outpatient-visit that may simply never come."
    (let [log [{:event :registered :t 0 :participants (subject "P1")}]]
      (is (empty? (check/participant-ids-exist-in-run log))))))

(deftest participant-ids-exist-in-run-recognizes-an-outpatient-visit-as-proof
  (testing "an outpatient patient never gets an :admission event at all --
            :registered (M5b's own broadened proof) covers it uniformly"
    (let [log [{:event :registered :t 0 :participants (subject "P1")}
               {:event :outpatient-visit :t 0 :participants (subject "P1")}
               {:event :outpatient-visit-end :t 10 :participants (subject "P1")}]]
      (is (empty? (check/participant-ids-exist-in-run log))))))

;; --- M5b: outpatient-visit / outpatient-visit-end -------------------------

(deftest outpatient-visit-only-when-new-detects-a-double-visit
  (let [log [{:event :outpatient-visit :t 0 :participants (subject "P1")}
             {:event :outpatient-visit :t 10 :participants (subject "P1")}]]
    (is (seq (check/outpatient-visit-only-when-new log)))))

(deftest outpatient-visit-only-when-new-holds-for-legit-log
  (is (empty? (check/outpatient-visit-only-when-new
               [{:event :outpatient-visit :t 0 :participants (subject "P1")}
                {:event :outpatient-visit-end :t 10 :participants (subject "P1")}]))))

(deftest admitted-occupies-one-slot-does-not-flag-a-nil-location-outpatient-visit
  (testing "item 6's conditional validity row: :location = nil is LEGAL
            exactly when :class = :outpatient -- the named exception to
            'never nil-bed while admitted'"
    (is (empty? (check/admitted-occupies-one-slot
                 [{:event :outpatient-visit :t 0 :participants (subject "P1")}])))))

(deftest outpatient-patients-occupy-no-bed-holds-for-legit-log
  (is (empty? (check/outpatient-patients-occupy-no-bed
               [{:event :outpatient-visit :t 0 :participants (subject "P1")}]))))

;; --- GMF coverage Wave C (2026-08-02, ADR-0028, C3): :expired ------------

(deftest expired-patient-retains-location-detects-a-nil-location
  (testing "evolve's own :discharge handling never nils :location for an
            :expired disposition -- this scenario (a patient who reaches
            an expired-disposition discharge with no location fold ever
            set) is not reachable through the engine as built, but the
            invariant checks the FOLDED state structurally, independent
            of whether decide/evolve themselves already enforce it"
    (let [log [{:event :discharge :t 30 :disposition :expired :participants (subject "P1")}]]
      (is (seq (check/expired-patient-retains-location log))))))

(deftest expired-patient-retains-location-holds-when-the-body-stays-put
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :discharge :t 30 :disposition :expired :participants (subject "P1")}]]
    (is (empty? (check/expired-patient-retains-location log)))))

(deftest expired-patient-retains-location-ignores-an-ordinary-discharge
  (testing "an ordinary discharge legitimately nils :location -- this
            invariant is scoped to the :expired disposition alone"
    (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
                :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
               {:event :discharge :t 30 :participants (subject "P1")}]]
      (is (empty? (check/expired-patient-retains-location log))))))

;; --- M5b: :procedure/:observation/:medication-order/:medication-end ------

(def ^:private a-citation {:module "sinusitis" :state :doctor-visit})
(def ^:private a-concept {:system :snomed :code "36971009" :display "Sinusitis (disorder)"})

(deftest clinical-content-only-when-admitted-detects-a-procedure-before-admission
  (let [log [{:event :procedure :t 0 :codes [a-concept] :participants (subject "P1")}]]
    (is (seq (check/clinical-content-only-when-admitted log)))))

(deftest clinical-content-only-when-admitted-holds-for-legit-log
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :procedure :t 5 :codes [a-concept] :participants (subject "P1")}
             {:event :observation :t 6 :codes [a-concept] :participants (subject "P1")}
             {:event :medication-order :t 7 :codes [a-concept] :participants (subject "P1")}]]
    (is (empty? (check/clinical-content-only-when-admitted log)))))

(deftest medication-end-references-existing-order-and-follows-it-in-time-detects-phantom-order
  (let [log [{:event :medication-end :t 0 :order-event-id 99 :participants (subject "P1")}]]
    (is (seq (check/medication-end-references-existing-order-and-follows-it-in-time log)))))

(deftest medication-end-references-existing-order-and-follows-it-in-time-holds-for-legit-log
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :medication-order :t 5 :codes [a-concept] :participants (subject "P1")}
             {:event :medication-end :t 10 :order-event-id 1 :participants (subject "P1")}]]
    (is (empty? (check/medication-end-references-existing-order-and-follows-it-in-time log)))))

;; ADR-0123: the medication-end/pre-horizon-order straddle case
;; (trajectory-computation.md's "History phase" -- an order that fires
;; before registration, compiled to a :pre-horizon-facts entry riding
;; :registered, while its own end fires after registration and is
;; emitted as a normal ground-truth event with no :medication-order to
;; resolve :order-event-id against).

(deftest medication-end-references-existing-order-and-follows-it-in-time-detects-phantom-order-even-with-unrelated-pre-horizon-facts
  (testing "a :registered event carrying :pre-horizon-facts at all must
            not make the checker permissive in general -- only a
            CITATION match should satisfy the widened branch"
    (let [log [{:event :registered :t 0 :participants (subject "P1")
                :pre-horizon-facts [{:event :medication-order
                                     :citation {:module "m" :state :some-other-order}}]}
               {:event :medication-end :t 10
                :order-event-id nil
                :order-citation {:module "m" :state :prescribe-amoxicillin}
                :participants (subject "P1")}]]
      (is (seq (check/medication-end-references-existing-order-and-follows-it-in-time log))))))

;; --- ADR-0166: the :care-plan-end referential invariant -- the mirror
;; of the :medication-end pair above, on the twin span. Scripted logs,
;; asserted DIRECTLY against the invariant function (R5's convention:
;; these fixtures carry no :registered event except where the branch
;; under test needs one, so the full check-all catalog is unavailable
;; to them -- the same disposition every scripted case above already
;; carries).

(def ^:private a-care-plan-citation {:module "bronchitis" :state :nonsmoker-careplan})

(deftest care-plan-end-references-existing-start-and-follows-it-in-time-detects-phantom-start
  (testing "a :start-event-id pointing at an index the log does not have"
    (let [log [{:event :care-plan-end :t 0 :start-event-id 99 :participants (subject "P1")}]]
      (is (seq (check/care-plan-end-references-existing-start-and-follows-it-in-time log))))))

(deftest care-plan-end-references-existing-start-and-follows-it-in-time-detects-a-nil-start
  (testing "ADR-0163's own shape, at the checker: the unpaired end seed
            5 carried silently -- no :start-event-id at all and no
            pre-horizon fact to excuse it"
    (let [log [{:event :care-plan-end :t 10 :start-event-id nil
                :care-plan-citation a-care-plan-citation :participants (subject "P1")}]]
      (is (seq (check/care-plan-end-references-existing-start-and-follows-it-in-time log))))))

(deftest care-plan-end-references-existing-start-and-follows-it-in-time-detects-another-patients-start
  (testing "ADR-0164's own hazard, at the checker: a well-formed index
            naming a :care-plan-start that belongs to a DIFFERENT
            patient -- byte-identical citations across two patients
            walking the same module is ordinary, not contrived"
    (let [log [{:event :care-plan-start :t 5 :codes [a-concept] :participants (subject "P2")}
               {:event :care-plan-end :t 10 :start-event-id 0 :participants (subject "P1")}]]
      (is (seq (check/care-plan-end-references-existing-start-and-follows-it-in-time log))))))

(deftest care-plan-end-references-existing-start-and-follows-it-in-time-detects-a-start-that-follows-its-end
  (let [log [{:event :care-plan-end :t 5 :start-event-id 1 :participants (subject "P1")}
             {:event :care-plan-start :t 10 :codes [a-concept] :participants (subject "P1")}]]
    (is (seq (check/care-plan-end-references-existing-start-and-follows-it-in-time log)))))

(deftest care-plan-end-references-existing-start-and-follows-it-in-time-detects-a-target-of-the-wrong-kind
  (testing "an index that resolves to a real event of some OTHER kind"
    (let [log [{:event :medication-order :t 5 :codes [a-concept] :participants (subject "P1")}
               {:event :care-plan-end :t 10 :start-event-id 0 :participants (subject "P1")}]]
      (is (seq (check/care-plan-end-references-existing-start-and-follows-it-in-time log))))))

(deftest care-plan-end-references-existing-start-and-follows-it-in-time-holds-for-legit-log
  (let [log [{:event :care-plan-start :t 5 :codes [a-concept] :participants (subject "P1")}
             {:event :care-plan-end :t 10 :start-event-id 0 :participants (subject "P1")}]]
    (is (empty? (check/care-plan-end-references-existing-start-and-follows-it-in-time log)))))

(deftest care-plan-end-references-existing-start-and-follows-it-in-time-allows-the-pre-horizon-straddle
  (testing "the designed straddle: a care plan opened before this run's
            own horizon is promoted to a :pre-horizon-facts entry on
            :registered (`compile-trajectory`'s own
            `pre-horizon-fact-types`, probed at ADR-0166 step 7), while
            its end fires in horizon with no top-level :care-plan-start
            to resolve :start-event-id against"
    (let [log [{:event :registered :t 0 :participants (subject "P1")
                :pre-horizon-facts [{:event :care-plan-start
                                     :citation a-care-plan-citation}]}
               {:event :care-plan-end :t 10 :start-event-id nil
                :care-plan-citation a-care-plan-citation :participants (subject "P1")}]]
      (is (empty? (check/care-plan-end-references-existing-start-and-follows-it-in-time log))))))

(deftest care-plan-end-references-existing-start-and-follows-it-in-time-detects-a-phantom-start-even-with-unrelated-pre-horizon-facts
  (testing "ADR-0123's own lesson, carried onto the twin: carrying
            :pre-horizon-facts at all must not make the checker
            permissive in general -- only a CITATION match satisfies
            the widened branch"
    (let [log [{:event :registered :t 0 :participants (subject "P1")
                :pre-horizon-facts [{:event :care-plan-start
                                     :citation {:module "bronchitis" :state :some-other-careplan}}]}
               {:event :care-plan-end :t 10 :start-event-id nil
                :care-plan-citation a-care-plan-citation :participants (subject "P1")}]]
      (is (seq (check/care-plan-end-references-existing-start-and-follows-it-in-time log))))))

(deftest care-plan-end-pre-horizon-escape-does-not-accept-a-medication-order-fact
  (testing "the fact's own :event key is load-bearing -- a
            :medication-order pre-horizon fact under the SAME citation
            must not excuse an unpaired care-plan end (the nested-
            :event hazard `event-schema/PreHorizonFact` names)"
    (let [log [{:event :registered :t 0 :participants (subject "P1")
                :pre-horizon-facts [{:event :medication-order
                                     :citation a-care-plan-citation}]}
               {:event :care-plan-end :t 10 :start-event-id nil
                :care-plan-citation a-care-plan-citation :participants (subject "P1")}]]
      (is (seq (check/care-plan-end-references-existing-start-and-follows-it-in-time log))))))

(def ^:private fixture-clinic-module
  "The same hand-authored module engine-test's own defspec assigns --
  its episode falls close enough to the engine's fixed registration
  anchor for some seeds to straddle it (engine_test.clj docstring,
  ~line 1080)."
  (:payload (patient-simulator/load-module "fixture-clinic"
                            (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json")))))

(deftest medication-end-references-existing-order-and-follows-it-in-time-holds-at-the-adr-0122-shrunk-seed
  (testing "ADR-0122's diagnosed regression: engine-test's own
            mixed-authored-and-compiled-run-satisfies-the-full-invariant-
            catalog defspec's exact config, at its shrunk failing seed
            (8589258984) -- a fixture-clinic patient whose medication
            order falls in history phase while its own end lands in
            horizon phase, the designed straddle case reproduced end to
            end through the real engine, not a hand-built log"
    (let [pathway {:name "scripted" :steps [{:type :admission :location "Renal"}
                                            {:type :delay :from 30 :to 30}
                                            {:type :discharge}]}
          empty-pathway {:name "module-only" :steps []}
          {:keys [ground-truth] :as result}
          (engine/run {:seed 8589258984 :patients 4
                       :pathways [{:patient-ordinal 0 :pathway pathway}
                                  {:patient-ordinal 1 :pathway pathway}
                                  {:patient-ordinal 2 :pathway empty-pathway}
                                  {:patient-ordinal 3 :pathway empty-pathway}]
                       :modules [(patient-simulator/singleton-closure fixture-clinic-module)]
                       :module-assignment [{:patient-ordinal 2 :module-id "fixture-clinic"}
                                           {:patient-ordinal 3 :module-id "fixture-clinic"}]
                       :module-horizon-days 3650})]
      (is (result/ok? (check/check-all ground-truth (:facility result)))))))

(deftest engine-run-with-compiled-clinical-steps-satisfies-check-all
  (let [pathway {:name "clinical" :steps [{:type :admission :location "Renal"}
                                          {:type :procedure :codes [a-concept]}
                                          {:type :observation :codes [a-concept] :value 38.2 :unit "Cel"}
                                          {:type :medication-order :codes [a-concept] :citation a-citation}
                                          {:type :medication-end :order-citation a-citation}
                                          {:type :discharge}]}
        {:keys [ground-truth] :as result} (engine/run {:seed 5 :patients 2 :pathways [{:pathway pathway :weight 1}]})]
    (is (result/ok? (check/check-all ground-truth (:facility result))))))

(deftest outpatient-patients-occupy-no-bed-detects-a-bed-assigned-to-an-outpatient
  (testing "structurally shouldn't happen (no decide path sets :location for
            :outpatient-visit), but this invariant checks any log directly,
            independent of whether decide itself enforces it"
    (let [log [{:event :outpatient-visit :t 0 :participants (subject "P1")}
               {:event :transfer :t 5 :home-ward "Renal" :participants (subject "P1")
                :from nil :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]]
      (is (= [{:invariant :outpatient-patients-occupy-no-bed :patient-id "P1" :at 5}]
             (check/outpatient-patients-occupy-no-bed log))))))

;; --- sim/ADR-0011: the warm-up mark -------------------------------------------

(deftest warm-up-mark-matches-window-detects-mismarked-event
  (let [log [{:event :admission :t 5 :warm-up false :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]]
    ;; t=5 < warm-up-seconds=10, so :warm-up should be true -- it's false.
    (is (seq (check/warm-up-mark-matches-window log 10)))))

(deftest warm-up-mark-matches-window-holds-when-correct
  (let [log [{:event :admission :t 5 :warm-up true :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :discharge :t 20 :warm-up false :participants (subject "P1")}]]
    (is (empty? (check/warm-up-mark-matches-window log 10)))))

(deftest engine-run-warm-up-seconds-marks-exactly-the-window
  (let [{:keys [ground-truth]} (engine/run {:seed 42 :patients 5 :warm-up-seconds 100})]
    (is (empty? (check/warm-up-mark-matches-window ground-truth 100)))
    (testing "a nonzero window actually marks at least one early event for this seed"
      (is (some :warm-up ground-truth)))))

;; --- M2b: churn family invariants (co-landed with the step types) ------

(deftest cancel-references-existing-uncancelled-event-detects-phantom-target
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             ;; :cancels-event-id 5 doesn't exist in this log at all.
             {:event :cancel-admit :t 10 :cancels-event-id 5 :participants (subject "P1")}]]
    (is (= [{:invariant :cancel-references-existing-uncancelled-event :patient-id "P1" :at 10}]
           (check/cancel-references-existing-uncancelled-event log)))))

(deftest cancel-references-existing-uncancelled-event-detects-wrong-type
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             ;; cancel-DISCHARGE pointed at an :admission event -- type mismatch.
             {:event :cancel-discharge :t 10 :cancels-event-id 0 :participants (subject "P1")}]]
    (is (= [{:invariant :cancel-references-existing-uncancelled-event :patient-id "P1" :at 10}]
           (check/cancel-references-existing-uncancelled-event log)))))

(deftest cancel-references-existing-uncancelled-event-detects-double-cancel
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :cancel-admit :t 10 :cancels-event-id 0 :participants (subject "P1")}
             {:event :cancel-admit :t 20 :cancels-event-id 0 :participants (subject "P1")}]]
    (testing "the SECOND cancel is the offender, at t 20 -- the first is legal.
              Pinning :at is what distinguishes 'found the double-cancel' from
              'found something'."
      (is (= [{:invariant :cancel-references-existing-uncancelled-event :patient-id "P1" :at 20}]
             (check/cancel-references-existing-uncancelled-event log))))))

(deftest cancel-references-existing-uncancelled-event-holds-for-legit-cancel
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :cancel-admit :t 10 :cancels-event-id 0 :participants (subject "P1")}]]
    (is (empty? (check/cancel-references-existing-uncancelled-event log)))))

(deftest bed-swap-both-admitted-before-swap-detects-a-non-admitted-participant
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             ;; P2 was never admitted.
             {:event :bed-swap :t 10
              :participants [{:patient-id "P1" :role :subject} {:patient-id "P2" :role :subject}]
              :swap {"P1" {:from {:ward "Renal" :bed "RENAL-01" :placement :licensed} :to nil}
                     "P2" {:from nil :to {:ward "Renal" :bed "RENAL-01" :placement :licensed}}}}]]
    (is (seq (check/bed-swap-both-admitted-before-swap log)))))

(deftest bed-swap-both-admitted-before-swap-holds-for-legit-swap
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :admission :t 5 :home-ward "Renal" :participants (subject "P2")
              :location {:ward "Renal" :bed "RENAL-H01" :placement :surge}}
             {:event :bed-swap :t 10
              :participants [{:patient-id "P1" :role :subject} {:patient-id "P2" :role :subject}]
              :swap {"P1" {:from {:ward "Renal" :bed "RENAL-01" :placement :licensed}
                          :to {:ward "Renal" :bed "RENAL-H01" :placement :surge}}
                     "P2" {:from {:ward "Renal" :bed "RENAL-H01" :placement :surge}
                          :to {:ward "Renal" :bed "RENAL-01" :placement :licensed}}}}]]
    (is (empty? (check/bed-swap-both-admitted-before-swap log)))))

(def ^:private legit-merge-log
  [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
    :active-mrn "MRN000001" :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
   {:event :admission :t 5 :home-ward "Renal" :participants (subject "P2")
    :active-mrn "MRN000002" :location {:ward "Renal" :bed "RENAL-H01" :placement :surge}}
   {:event :merge :t 10
    :participants [{:patient-id "P1" :role :survivor} {:patient-id "P2" :role :merged}]
    :surviving-mrn "MRN000001" :merged-mrn "MRN000002" :merged-mrns #{"MRN000002"}}])

(deftest merge-survivor-absorbs-merged-mrns-holds-for-legit-merge
  (is (empty? (check/merge-survivor-absorbs-merged-mrns legit-merge-log))))

(deftest merge-survivor-absorbs-merged-mrns-detects-wrong-active-mrn
  (let [log (update-in legit-merge-log [2] assoc :surviving-mrn "MRN999999")]
    (is (seq (check/merge-survivor-absorbs-merged-mrns log)))))

(deftest no-events-after-merged-terminal-holds-for-legit-log
  (is (empty? (check/no-events-after-merged-terminal legit-merge-log))))

(deftest no-events-after-merged-terminal-detects-a-zombie-event
  (let [log (conj legit-merge-log
                  {:event :discharge :t 20 :participants (subject "P2")})]
    (is (= [{:invariant :no-events-after-merged-terminal :patient-id "P2" :at 20}]
           (check/no-events-after-merged-terminal log)))))

;; --- sim/ADR-0012: :step-rejected -------------------------------------------

(deftest step-rejected-reason-is-documented-detects-an-undocumented-reason
  (let [log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
             {:event :step-rejected :t 10 :participants (subject "P1")
              :attempted-step {:type :cancel-admit} :reason :not-a-real-reason}]]
    (is (seq (check/step-rejected-reason-is-documented log)))))

(deftest step-rejected-reason-is-documented-holds-for-every-real-reason
  (is (empty? (check/step-rejected-reason-is-documented
               (for [reason engine/documented-step-rejection-reasons]
                 {:event :step-rejected :t 10 :participants (subject "P1")
                  :attempted-step {:type :cancel-admit} :reason reason})))))

;; --- M3: order/result -----------------------------------------------------

(def ^:private cbc-profile (:cbc order-profiles/default-profiles))

(defn- legit-order-result-log
  []
  [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
    :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
   {:event :order-placed :t 10 :profile :cbc :concept (:concept cbc-profile) :participants (subject "P1")}
   {:event :result-available :t 100 :profile :cbc :order-event-id 1 :concept (:concept cbc-profile)
    :participants (subject "P1")
    :results (mapv (fn [a] {:concept (:concept a) :unit (:units a) :value (:low (:reference-range a))
                            :reference-range (:reference-range a) :abnormal-flag :normal})
                    (:analytes cbc-profile))}])

(deftest order-only-when-admitted-holds-for-legit-log
  (is (empty? (check/order-only-when-admitted (legit-order-result-log)))))

(deftest order-only-when-admitted-detects-order-before-admission
  (let [log [{:event :order-placed :t 0 :profile :cbc :concept (:concept cbc-profile) :participants (subject "P1")}]]
    (is (seq (check/order-only-when-admitted log)))))

(deftest order-only-when-admitted-allows-a-result-arriving-after-discharge
  (testing "async turnaround: a result legitimately arrives after
            discharge (pending labs at discharge is real clinical
            traffic) -- NOT a violation"
    (let [log (conj (legit-order-result-log)
                    {:event :discharge :t 200 :participants (subject "P1")}
                    {:event :result-available :t 300 :profile :cbc :order-event-id 1
                     :concept (:concept cbc-profile) :participants (subject "P1")
                     :results []})]
      (is (empty? (check/order-only-when-admitted log))))))

(deftest result-references-existing-order-and-follows-it-in-time-holds-for-legit-log
  (is (empty? (check/result-references-existing-order-and-follows-it-in-time (legit-order-result-log)))))

(deftest result-references-existing-order-and-follows-it-in-time-detects-phantom-order
  (let [log [{:event :result-available :t 10 :profile :cbc :order-event-id 5
              :concept (:concept cbc-profile) :participants (subject "P1") :results []}]]
    (is (seq (check/result-references-existing-order-and-follows-it-in-time log)))))

(deftest result-references-existing-order-and-follows-it-in-time-detects-time-travel
  (let [log (update (legit-order-result-log) 2 assoc :t 5)] ;; before its own order's t=10
    (is (seq (check/result-references-existing-order-and-follows-it-in-time log)))))

(deftest result-analytes-match-order-profile-holds-for-legit-log
  (is (empty? (check/result-analytes-match-order-profile (legit-order-result-log) order-profiles/default-profiles))))

(deftest result-analytes-match-order-profile-detects-a-missing-analyte
  (let [log (update-in (legit-order-result-log) [2 :results] #(vec (rest %)))]
    (is (seq (check/result-analytes-match-order-profile log order-profiles/default-profiles)))))

(deftest abnormal-flags-consistent-with-value-vs-range-holds-for-legit-log
  (is (empty? (check/abnormal-flags-consistent-with-value-vs-range (legit-order-result-log)))))

(deftest abnormal-flags-consistent-with-value-vs-range-detects-a-lying-flag
  (let [log (update-in (legit-order-result-log) [2 :results 0] assoc :abnormal-flag :high)]
    (is (seq (check/abnormal-flags-consistent-with-value-vs-range log)))))

(deftest engine-run-with-order-profiles-satisfies-check-all
  (let [pathway {:name "cbc" :steps [{:type :admission :location "Renal"}
                                     {:type :order :profile :cbc}
                                     {:type :discharge}]}
        {:keys [ground-truth]} (engine/run {:seed 3 :patients 4 :pathways [{:pathway pathway :weight 1}]})]
    (is (result/ok? (check/check-all ground-truth)))))

;; --- check-all: facility-aware, backward-compatible arity ---------------

(deftest check-all-defaults-facility-when-omitted
  (let [{:keys [ground-truth]} (engine/run {:seed 42 :patients 3})]
    (is (result/ok? (check/check-all ground-truth)))))

;; --- D6-1: the invariant catalog's own sample, widened ------------------
;;
;; ADR-0158, review-4 register row D6-1. The defspec below used to run
;; 150 trials against a FIXED facility -- ED (0 beds / 15 surge) and
;; Renal (1 bed / 0 surge) -- with no churn and no pathways. ADR-0153's
;; real defect (`decide :discharge`'s bed-ready pull handing a boarder a
;; vacated SURGE slot while rung 1 stood free) was STRUCTURALLY
;; unreachable under that configuration at any trial count: the sample
;; was in the wrong place, not merely too small.
;;
;; What the sample needs, measured against the pre-fix engine at
;; `ceedcfd` (ADR-0158's own Verification section carries the runs):
;;
;;   1. a ward carrying BOTH a licensed bed and a surge slot -- Renal
;;      below always does. This is the row's own remedy, and on its own
;;      it is NOT ENOUGH: 400 trials with mixed wards and a hot churn
;;      profile, but the DEFAULT pathway, found ZERO violations.
;;   2. MORE THAN ONE HOME WARD -- necessary, and the ingredient D6-1's
;;      remedy text does not name. The default pathway admits every
;;      patient to Renal, so every boarder's home ward is the ward it
;;      would be pulled back into. ADR-0153's route needs a bed to
;;      vacate in a ward WITHOUT pulling anyone home to it, which a
;;      single-home-ward population can never produce.
;;   3. churn -- NOT necessary, but strongly amplifying: 0.5% of trials
;;      violate without it (2 of 400) against 2.8% with it (11 of 400).
;;      This defspec's own first historical failure carried no churn.
;;
;; TRIAL COUNT. 150 -- the count this defspec has always carried -- put
;; the widened sample at 5 reds in 6 runs against the pre-fix engine.
;; 300 puts it at 8 in 8, for 3.3s against 2.0s. In a session whose
;; subject IS sampling adequacy, 1.3s is the wrong thing to save.
;;
;; Ward ids and names are fixed while capacities and weights vary:
;; `sim-model/default-provider-templates` is ward-eligible for exactly
;; :ed / :renal / :cardiology, so a generated ward id would sample a
;; config error rather than a facility.

(def ^:private mixed-ward-facility-gen
  "A facility whose Renal ward always carries both bed classes, and
  whose Cardiology surge may be zero so the degenerate single-class
  shape the old fixed literal exercised stays in the sample."
  (gen/let [renal-beds  (gen/choose 1 3)
            renal-surge (gen/choose 1 2)
            card-beds   (gen/choose 1 3)
            card-surge  (gen/choose 0 2)
            ed-surge    (gen/choose 2 7)]
    {:id :t
     :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots ed-surge
              :surge-format "%s-H%02d" :class :ed}
             {:id :renal :name "Renal" :beds renal-beds :surge-slots renal-surge
              :surge-format "%s-H%02d" :class :inpatient}
             {:id :cardiology :name "Cardiology" :beds card-beds :surge-slots card-surge
              :surge-format "%s-H%02d" :class :inpatient}]}))

(defn- admit-dwell-discharge [ward]
  {:name (str "admit-" ward)
   :steps [{:type :admission :location ward :reason "catalog sample"}
           {:type :delay :from 60 :to 240}
           {:type :discharge}]})

(def ^:private multi-home-pathways-gen
  "A weighted pool over all three wards, so a run's patients do NOT all
  share one home ward -- ingredient (3) above."
  (gen/let [w-renal (gen/choose 1 3)
            w-card  (gen/choose 1 3)
            w-ed    (gen/choose 1 3)]
    [{:pathway (admit-dwell-discharge "Renal") :weight w-renal}
     {:pathway (admit-dwell-discharge "Cardiology") :weight w-card}
     {:pathway (admit-dwell-discharge "Emergency") :weight w-ed}]))

(def ^:private churn-on-a-fraction-gen
  "Churn on roughly two trials in three -- the row asks for `some
  fraction`, and the no-churn third keeps the churn-free configuration
  the old defspec covered inside the same sample. The profile is the
  repo's own sanctioned `churn/sample-profile`, not a hand-tuned one:
  it raises the per-trial hit rate about six-fold rather than making
  the defect reachable at all (measured, ADR-0158)."
  (gen/frequency [[1 (gen/return nil)]
                  [2 (gen/return churn/sample-profile)]]))

(defspec every-m1-run-satisfies-the-invariant-catalog 300
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 6 32)
                 arrival-gap (gen/choose 5 45)
                 facility mixed-ward-facility-gen
                 pathways multi-home-pathways-gen
                 churn-profile churn-on-a-fraction-gen]
    (let [{:keys [ground-truth]} (engine/run (cond-> {:seed seed :patients patients
                                                      :arrival-gap arrival-gap
                                                      :facility facility
                                                      :pathways pathways}
                                               churn-profile (assoc :churn-profile churn-profile)))]
      (and (seq ground-truth)
           (result/ok? (check/check-all ground-truth facility))))))

(deftest the-widened-catalog-sample-varies-what-it-claims-to-vary
  (testing "mechanism sanity for the three generators above (ADR-0158, D6-1)"
    ;; A defspec whose generators silently collapsed to one shape would
    ;; still pass all 300 trials and vouch for nothing -- which is
    ;; exactly what the FIXED facility this replaces did, at 150.
    (let [facilities (gen/sample mixed-ward-facility-gen 200)
          pathway-pools (gen/sample multi-home-pathways-gen 200)
          churns (gen/sample churn-on-a-fraction-gen 200)]
      (testing "every sampled facility carries a ward with BOTH bed classes"
        (is (every? (fn [f] (some #(and (pos? (:beds %)) (pos? (:surge-slots %)))
                                  (:wards f)))
                    facilities)))
      (testing "capacities actually vary across the sample"
        (is (< 1 (count (distinct (map (fn [f] (mapv (juxt :beds :surge-slots) (:wards f)))
                                       facilities))))))
      (testing "every sampled pathway pool names more than one home ward"
        (is (every? (fn [ps] (< 1 (count (distinct (map #(get-in % [:pathway :steps 0 :location])
                                                        ps)))))
                    pathway-pools)))
      (testing "churn is on for some trials and off for others"
        (is (some some? churns) "no trial carried a churn profile")
        (is (some nil? churns) "no trial ran churn-free")))))

;; --- M4: Persona ------------------------------------------------------

(def ^:private a-persona
  (sim-model/persona (java.util.Random. 1) {}))

(deftest registered-is-every-patients-first-event-holds-for-legit-log
  (is (empty? (check/registered-is-every-patients-first-event
               [{:event :registered :t 0 :active-mrn "MRN000001" :persona a-persona :participants (subject "P1")}
                {:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
                 :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]))))

(deftest registered-is-every-patients-first-event-detects-a-missing-registration
  (is (seq (check/registered-is-every-patients-first-event
            [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]))))

(deftest registered-persona-is-schema-valid-holds-for-a-real-persona
  (is (empty? (check/registered-persona-is-schema-valid
               [{:event :registered :t 0 :active-mrn "MRN000001" :persona a-persona :participants (subject "P1")}]))))

(deftest registered-persona-is-schema-valid-detects-a-malformed-persona
  (is (seq (check/registered-persona-is-schema-valid
            [{:event :registered :t 0 :active-mrn "MRN000001" :persona {:name "not a map"} :participants (subject "P1")}]))))

(deftest engine-run-satisfies-check-all-with-persona
  (let [{:keys [ground-truth]} (engine/run {:seed 5 :patients 4})]
    (is (result/ok? (check/check-all ground-truth)))))

;; --- ADR-0169 (arc 0): the naive reference oracles, and the defspec ------
;;
;; The six invariants below were rewritten to carry incremental state
;; through the fold instead of walking the whole population (or the whole
;; log) once per event. A pure refactor owes an EQUIVALENCE PROOF rather
;; than red-before-green, and this is the second half of it: the ORIGINAL
;; bodies, kept verbatim, as `naive-*` reference oracles, plus a defspec
;; asserting `(= (naive-x log) (fast-x log))` -- sequence equality, so
;; ORDER is asserted, not merely content.
;;
;; These are deliberate duplication. A future change to any of the six
;; must move both copies; the defspec is what notices if it does not.
;; They are cited by name in `ehrt.sim-check.check`'s own ADR-0169
;; comment, and their bodies are copied from `check.clj` at `d49f1c6`
;; with NOTHING changed but the name.

(defn- naive-no-double-occupancy [ground-truth]
  (for [{:keys [event world-after]} (engine/replay ground-truth)
        :let [beds (keep (comp :bed :location) (vals world-after))
              dupes (->> beds frequencies (filter (comp #(> % 1) val)) (map key))]
        bed dupes]
    {:invariant :no-double-occupancy :bed bed :at (:t event)}))

(defn- naive-admitted-occupies-one-slot [ground-truth]
  (for [{:keys [event world-after]} (engine/replay ground-truth)
        [patient-id {:keys [status location class]}] world-after
        :when (and (= status :admitted) (not= class :outpatient)
                   (or (nil? location) (nil? (:bed location))))]
    {:invariant :admitted-occupies-one-slot :patient-id patient-id :at (:t event)}))

(defn- naive-outpatient-patients-occupy-no-bed [ground-truth]
  (for [{:keys [event world-after]} (engine/replay ground-truth)
        [patient-id {:keys [class location]}] world-after
        :when (and (= class :outpatient) (some? location))]
    {:invariant :outpatient-patients-occupy-no-bed :patient-id patient-id :at (:t event)}))

(defn- naive-occupancy-within-capacity [ground-truth facility-config]
  (for [{:keys [event world-after]} (engine/replay ground-truth)
        ward (:wards facility-config)
        :let [cap (+ (:beds ward) (:surge-slots ward))
              occ (count (filter #(= (:name ward) (get-in % [:location :ward])) (vals world-after)))]
        :when (> occ cap)]
    {:invariant :occupancy-within-capacity :ward (:name ward) :at (:t event)
     :occupied occ :capacity cap}))

(def ^:private naive-cancel-target-type
  {:cancel-admit :admission :cancel-transfer :transfer :cancel-discharge :discharge})

(defn- naive-cancel-references-existing-uncancelled-event [ground-truth]
  (let [indexed (vec ground-truth)]
    (for [[idx event] (map-indexed vector indexed)
          :when (contains? naive-cancel-target-type (:event event))
          :let [target-idx (:cancels-event-id event)
                target (get indexed target-idx)
                expected-type (get naive-cancel-target-type (:event event))
                patient-id (:patient-id (first (:participants event)))
                cancelled-earlier? (some (fn [[i2 ev2]]
                                           (and (< i2 idx)
                                                (= (:event event) (:event ev2))
                                                (= target-idx (:cancels-event-id ev2))))
                                         (map-indexed vector indexed))]
          :when (or (nil? target)
                    (not= expected-type (:event target))
                    (not (some #(= patient-id (:patient-id %)) (:participants target)))
                    cancelled-earlier?)]
      {:invariant :cancel-references-existing-uncancelled-event :patient-id patient-id :at (:t event)})))

(defn- naive-no-events-after-merged-terminal [ground-truth]
  (let [indexed (vec ground-truth)]
    (for [[merge-idx event] (map-indexed vector indexed)
          :when (= :merge (:event event))
          :let [merged-id (:patient-id (first (filter #(= :merged (:role %)) (:participants event))))]
          [later-idx later-event] (map-indexed vector indexed)
          :when (and (> later-idx merge-idx)
                    (some #(= merged-id (:patient-id %)) (:participants later-event)))]
      {:invariant :no-events-after-merged-terminal :patient-id merged-id :at (:t later-event)})))

;; --- the mutations that make the comparison non-vacuous ------------------
;;
;; A generated run is self-check CLEAN by construction, so `(= (naive-x
;; log) (fast-x log))` over clean logs alone would compare `()` with `()`
;; six times and prove nothing about the emission path -- which is the
;; half where ORDER lives. Every mutation below therefore induces the
;; specific violation its invariant reports, at POPULATION scale and with
;; SEVERAL findings live at once, because a single finding cannot
;; distinguish one ordering from another.

(def ^:private shared-beds
  "Three shared beds, not one: `no-double-occupancy` emits one finding
  per DUPLICATED BED per event, and the order of those comes from
  `(frequencies beds)`. One collision bed could never witness it."
  ["SHARED-01" "SHARED-02" "SHARED-03"])

(defn- collide-beds
  "Every `n`-th bed-bearing event re-pointed at one of `shared-beds`,
  keeping its ward -- sustained multi-bed double-occupancy, and ward
  over-capacity along with it."
  [log n]
  (vec (map-indexed
        (fn [i ev]
          (if (and (pos? n) (zero? (mod i n)) (get-in ev [:location :bed]))
            ;; `(quot i n)`, NOT `i`: every hit has `i` a multiple of `n`,
            ;; so indexing by `i` would send every collision to the SAME
            ;; bed whenever `n` and the pool size share a factor -- one
            ;; duplicated bed, and the emission order this pool exists to
            ;; exercise never witnessed. Measured, not guessed: with `i`,
            ;; n=3 put 100% of collisions on SHARED-01.
            (assoc-in ev [:location :bed] (nth shared-beds (mod (quot i n) (count shared-beds))))
            ev))
        log)))

(defn- strip-locations
  "Every `n`-th :admission's `:location` nilled -- the patient is folded
  to :admitted holding nothing, which is `admitted-occupies-one-slot`'s
  own violation, and it PERSISTS, so several patients are offending
  simultaneously and the emission order over `world-after` is exercised."
  [log n]
  (vec (map-indexed
        (fn [i ev]
          (if (and (pos? n) (zero? (mod i n)) (= :admission (:event ev)))
            (assoc ev :location nil)
            ev))
        log)))

(defn- outpatient-ize
  "An `:outpatient-visit` inserted immediately AFTER every `n`-th
  :admission, for the same patient. `evolve :outpatient-visit` sets
  `:class :outpatient` and leaves `:location` alone, so the patient the
  admission just bedded is now an outpatient holding a bed -- and stays
  one, so several are live at once.

  Inserted rather than substituted, deliberately: relabelling the
  admission would leave the patient with NO location (the admission's
  own `evolve` is what sets it), and the violation needs both halves."
  [log n]
  (vec (mapcat (fn [i ev]
                 (if (and (pos? n) (zero? (mod i n)) (= :admission (:event ev)))
                   [ev {:event :outpatient-visit :t (:t ev) :participants (:participants ev)}]
                   [ev]))
               (range) log)))

(defn- duplicate-cancels
  "Each cancel event followed by a verbatim copy -- the copy is a
  double-cancel of a target an earlier cancel of the same kind already
  named, `cancel-references-existing-uncancelled-event`'s own
  `cancelled-earlier?` arm."
  [log]
  (vec (mapcat (fn [ev] (if (contains? naive-cancel-target-type (:event ev)) [ev ev] [ev])) log)))

(defn- zombie-events
  "Two post-merge events per merge, INTERLEAVED across merges rather
  than grouped -- the shape that tells merge-major emission (what
  `no-events-after-merged-terminal` promises) apart from event-major
  emission (what a single forward pass would produce). Grouped zombies
  would read the same either way and witness nothing."
  [log]
  (let [merged-ids (into [] (comp (filter #(= :merge (:event %)))
                                  (map (fn [ev] (:patient-id (first (filter #(= :merged (:role %))
                                                                            (:participants ev)))))))
                        log)
        t-max (reduce max 0 (map :t log))]
    (into (vec log)
          (for [round [1 2]
                [k pid] (map-indexed vector merged-ids)]
            {:event :discharge :t (+ t-max 1 (* 100 round) k)
             :participants [{:patient-id pid :role :subject}]}))))

(defn- mutate
  "One log, mutated every way at once -- the invariants are independent,
  so proving them one mutation at a time would be six times the trials
  for strictly less coverage of their interaction."
  [log a b c]
  (-> log (collide-beds a) (strip-locations b) (outpatient-ize c)
      duplicate-cancels zombie-events))

(defn- tight-view
  "The same wards, re-declared at one licensed bed and no surge.

  `occupancy-within-capacity` is the one invariant of the six a MUTATED
  LOG cannot make fire: the engine never over-fills a ward, and the
  mutations above move patients between BEDS, not between wards. But the
  invariant's signature is (log, facility-config) -- it judges a log
  against a DECLARED capacity -- so the way to make it fire is the way
  its own discrimination test already does: hand it a facility whose
  declared capacity the log exceeds. At one bed per ward every occupied
  ward over-fills, in every ward of the loop, on most events."
  [facility]
  (update facility :wards (fn [ws] (mapv #(assoc % :beds 1 :surge-slots 0) ws))))

(defn- all-six
  "The six rewritten invariants' findings, and their naive references',
  as two parallel vectors -- compared with `=`, so ORDER is asserted.
  Capacity appears TWICE: under the run's real facility (where it should
  stay silent) and under `tight-view` (where it fires in every ward)."
  [f-double f-slot f-outp f-cap f-cancel f-merged log facility]
  [(vec (f-double log)) (vec (f-slot log)) (vec (f-outp log))
   (vec (f-cap log facility)) (vec (f-cap log (tight-view facility)))
   (vec (f-cancel log)) (vec (f-merged log))])

(def ^:private roomy-facility-gen
  "A facility with room to run, unlike `mixed-ward-facility-gen` above.
  That one is tuned to make the ALLOCATION LADDER violate (1-3 licensed
  beds, so surge and boarding are reached quickly), which halts most runs
  on `:capacity-exhausted` inside twenty-odd events. This defspec needs
  the opposite: logs long enough, with enough concurrent occupancy, that
  the mutations below leave SEVERAL findings live at once -- which is the
  only condition under which sequence equality tests ORDER rather than
  just content. Measured: at these capacities a 60-80 patient run yields
  ~190-260 events with 2-4 merges and 2-8 cancels; at
  `mixed-ward-facility-gen`'s, 11-37 events with zero of either."
  (gen/let [renal-beds (gen/choose 8 14)
            card-beds  (gen/choose 8 14)
            ed-surge   (gen/choose 4 8)
            surge      (gen/choose 2 5)]
    {:id :t
     :wards [{:id :renal :name "Renal" :beds renal-beds :surge-slots surge
              :surge-format "%s-H%02d" :class :inpatient}
             {:id :cardiology :name "Cardiology" :beds card-beds :surge-slots surge
              :surge-format "%s-H%02d" :class :inpatient}
             {:id :ed :name "Emergency" :beds 8 :surge-slots ed-surge
              :surge-format "%s-H%02d" :class :ed}]}))

(defspec fast-invariants-equal-their-naive-reference-implementations
  {:num-tests 120 :seed 20260825}
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 30 80)
                 facility roomy-facility-gen
                 churn-profile churn-on-a-fraction-gen
                 a (gen/choose 3 9)
                 b (gen/choose 4 11)
                 c (gen/choose 5 13)]
    (let [{:keys [ground-truth]} (engine/run (cond-> {:seed seed :patients patients
                                                      :facility facility}
                                               churn-profile (assoc :churn-profile churn-profile)))
          mutated (mutate ground-truth a b c)]
      (and (seq ground-truth)
           ;; the CLEAN log: the guards must not false-positive
           (= (all-six naive-no-double-occupancy naive-admitted-occupies-one-slot
                       naive-outpatient-patients-occupy-no-bed naive-occupancy-within-capacity
                       naive-cancel-references-existing-uncancelled-event
                       naive-no-events-after-merged-terminal ground-truth facility)
              (all-six check/no-double-occupancy check/admitted-occupies-one-slot
                       check/outpatient-patients-occupy-no-bed check/occupancy-within-capacity
                       check/cancel-references-existing-uncancelled-event
                       check/no-events-after-merged-terminal ground-truth facility))
           ;; the MUTATED log: the emission path, with order
           (= (all-six naive-no-double-occupancy naive-admitted-occupies-one-slot
                       naive-outpatient-patients-occupy-no-bed naive-occupancy-within-capacity
                       naive-cancel-references-existing-uncancelled-event
                       naive-no-events-after-merged-terminal mutated facility)
              (all-six check/no-double-occupancy check/admitted-occupies-one-slot
                       check/outpatient-patients-occupy-no-bed check/occupancy-within-capacity
                       check/cancel-references-existing-uncancelled-event
                       check/no-events-after-merged-terminal mutated facility))))))

(deftest the-mutations-actually-make-all-six-invariants-fire
  (testing "ADR-0169: a comparison of two empty seqs is not an equivalence
            proof. This is the mechanism check for the defspec above --
            without it, a mutation that silently stopped inducing its
            violation would leave the defspec green and vouching for
            nothing (the same defect `the-widened-catalog-sample-varies-
            what-it-claims-to-vary` guards for the catalog defspec)."
    ;; seed 27 / 60 patients at these capacities: MEASURED 196 events,
    ;; 3 merges, 6 cancels, 5 transfers, self-check clean, not exhausted.
    ;; Two merges is the floor for the interleaving check at the end;
    ;; `mixed-ward-facility-gen`'s own capacities exhaust the run at ~20
    ;; events with zero merges and zero cancels, which is why this
    ;; fixture does not reuse it.
    (let [facility {:id :t :wards [{:id :renal :name "Renal" :beds 12 :surge-slots 4
                                    :surge-format "%s-H%02d" :class :inpatient}
                                   {:id :cardiology :name "Cardiology" :beds 12 :surge-slots 4
                                    :surge-format "%s-H%02d" :class :inpatient}
                                   {:id :ed :name "Emergency" :beds 8 :surge-slots 4
                                    :surge-format "%s-H%02d" :class :ed}]}
          {:keys [ground-truth]} (engine/run {:seed 27 :patients 60
                                              :facility facility
                                              :churn-profile churn/sample-profile})
          mutated (mutate ground-truth 3 5 7)]
      (is (seq ground-truth))
      (testing "the clean run really is clean, so the clean half of the defspec
                is comparing something the guards had to get right"
        (is (result/ok? (check/check-all ground-truth facility))))
      (doseq [[label found]
              [[:no-double-occupancy (check/no-double-occupancy mutated)]
               [:admitted-occupies-one-slot (check/admitted-occupies-one-slot mutated)]
               [:outpatient-patients-occupy-no-bed (check/outpatient-patients-occupy-no-bed mutated)]
               [:occupancy-within-capacity (check/occupancy-within-capacity mutated (tight-view facility))]
               [:cancel-references-existing-uncancelled-event
                (check/cancel-references-existing-uncancelled-event mutated)]
               [:no-events-after-merged-terminal (check/no-events-after-merged-terminal mutated)]]]
        (testing (str label " fires on the mutated log")
          (is (seq found) (str label " induced NO findings -- its mutation has gone inert"))))
      (testing "several findings are live at once, so ORDER is under test and not
                merely content"
        (is (< 1 (count (distinct (map :bed (check/no-double-occupancy mutated))))))
        (is (< 1 (count (distinct (map :patient-id (check/admitted-occupies-one-slot mutated)))))))
      (testing "the merged-terminal zombies interleave across merges, the shape
                that distinguishes merge-major from event-major emission"
        (let [merges (count (filter #(= :merge (:event %)) ground-truth))
              found (check/no-events-after-merged-terminal mutated)]
          (when (< 1 merges)
            (is (< 1 (count (distinct (map :patient-id found))))
                "more than one merge, but only one merged-id in the findings")))))))

(def ^:private small-mutated-fixtures
  "Every hand-written violating log this namespace already carries for
  the six invariants, gathered so the naive/fast comparison runs over
  them too.

  These matter out of proportion to their size. A Clojure map holds
  INSERTION order below eight entries (`PersistentArrayMap`) and hash
  order above it (`PersistentHashMap`), and three of the six invariants
  emit in the iteration order of a map -- so the regime where a carried
  index could most easily disagree with `world-after` about ordering is
  exactly the small one the defspec's population-scale runs never enter."
  [{:label :double-occupancy
    :log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
           :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
          {:event :admission :t 5 :home-ward "Renal" :participants (subject "P2")
           :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]}
   {:label :one-slot-nil-location
    :log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1") :location nil}]}
   {:label :capacity-overflow
    :log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
           :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
          {:event :admission :t 5 :home-ward "Renal" :participants (subject "P2")
           :location {:ward "Renal" :bed "RENAL-99" :placement :licensed}}]}
   {:label :outpatient-with-bed
    :log [{:event :outpatient-visit :t 0 :participants (subject "P1")}
          {:event :transfer :t 5 :home-ward "Renal" :participants (subject "P1")
           :from nil :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}]}
   {:label :cancel-phantom-target
    :log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
           :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
          {:event :cancel-admit :t 10 :cancels-event-id 5 :participants (subject "P1")}]}
   {:label :cancel-wrong-type
    :log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
           :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
          {:event :cancel-discharge :t 10 :cancels-event-id 0 :participants (subject "P1")}]}
   {:label :cancel-double
    :log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
           :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
          {:event :cancel-admit :t 10 :cancels-event-id 0 :participants (subject "P1")}
          {:event :cancel-admit :t 20 :cancels-event-id 0 :participants (subject "P1")}]}
   {:label :legit-merge
    :log legit-merge-log}
   {:label :merged-terminal-zombie
    :log (conj legit-merge-log {:event :discharge :t 20 :participants (subject "P2")})}
   {:label :two-merges-interleaved-zombies
    ;; TWO merges whose zombie events INTERLEAVE -- the shape that tells
    ;; merge-major emission apart from event-major, at a size where the
    ;; whole log is one array-map's worth of patients.
    :log [{:event :admission :t 0 :home-ward "Renal" :participants (subject "P1")
           :active-mrn "MRN000001" :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}}
          {:event :admission :t 1 :home-ward "Renal" :participants (subject "P2")
           :active-mrn "MRN000002" :location {:ward "Renal" :bed "RENAL-02" :placement :licensed}}
          {:event :admission :t 2 :home-ward "Renal" :participants (subject "P3")
           :active-mrn "MRN000003" :location {:ward "Renal" :bed "RENAL-03" :placement :licensed}}
          {:event :admission :t 3 :home-ward "Renal" :participants (subject "P4")
           :active-mrn "MRN000004" :location {:ward "Renal" :bed "RENAL-04" :placement :licensed}}
          {:event :merge :t 10
           :participants [{:patient-id "P1" :role :survivor} {:patient-id "P2" :role :merged}]
           :surviving-mrn "MRN000001" :merged-mrn "MRN000002" :merged-mrns #{"MRN000002"}}
          {:event :merge :t 11
           :participants [{:patient-id "P3" :role :survivor} {:patient-id "P4" :role :merged}]
           :surviving-mrn "MRN000003" :merged-mrn "MRN000004" :merged-mrns #{"MRN000004"}}
          {:event :observation :t 20 :participants (subject "P2")}
          {:event :observation :t 21 :participants (subject "P4")}
          {:event :observation :t 22 :participants (subject "P2")}
          {:event :observation :t 23 :participants (subject "P4")}]}])

(deftest fast-invariants-equal-their-naive-references-on-every-small-fixture
  (testing "ADR-0169's equivalence obligation over this namespace's own
            hand-written violating logs -- the small-map regime the
            population-scale defspec above cannot reach."
    (doseq [{:keys [label log]} small-mutated-fixtures]
      (testing (str "fixture " label)
        (is (= (all-six naive-no-double-occupancy naive-admitted-occupies-one-slot
                        naive-outpatient-patients-occupy-no-bed naive-occupancy-within-capacity
                        naive-cancel-references-existing-uncancelled-event
                        naive-no-events-after-merged-terminal log test-facility)
               (all-six check/no-double-occupancy check/admitted-occupies-one-slot
                        check/outpatient-patients-occupy-no-bed check/occupancy-within-capacity
                        check/cancel-references-existing-uncancelled-event
                        check/no-events-after-merged-terminal log test-facility)))))
    (testing "and the interleaved-zombie fixture really does discriminate the
              two orderings -- merge-major (P2,P2,P4,P4) is NOT the log order
              (P2,P4,P2,P4), so a single forward pass would be caught"
      (let [log (:log (last small-mutated-fixtures))]
        (is (= ["P2" "P2" "P4" "P4"]
               (mapv :patient-id (check/no-events-after-merged-terminal log))))))))
