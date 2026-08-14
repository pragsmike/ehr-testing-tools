(ns ehrt.sim-trajectory.compile-trajectory-test
  "Red tests for CompileTrajectory (M5b Task 3, docs/gmf-interpreter.md
  section 1's per-state-type mapping table and section 6's own build-
  session test obligations) -- written before ehrt.sim-trajectory.compile-
  trajectory exists (sim/ADR-0004 test-first). Pure, RNG-free: every value
  CompileTrajectory ever touches was already sampled by the GMF
  interpreter (M5a); this stage only re-shapes already-decided content
  into pathway IR.

  Laws under test: clinical-content preservation (every non-internal
  trajectory event maps to >=1 IR artifact -- a step, a condition
  annotation, or a registration-time fact -- none dropped without a
  documented reason, none invented, clinical order preserved);
  provenance (every compiled step cites the trajectory event it
  realizes, the third link of the module-state -> trajectory-event ->
  IR-step glass-box chain); the day -> minutes conversion, at exactly
  one point (the durations rule's own day clause, docs/patient-state-
  model.md)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-trajectory.compile-trajectory :as ct]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-trajectory.gmf-interpreter :as interp])
  (:import [java.util Random]))

(def ^:private facility sim-model/default-facility)

(defn- ev
  "Test-fixture convenience: a trajectory event, defaults filled in."
  [event overrides]
  (merge {:module "m" :state :s :t 0 :event event :pre-horizon false} overrides))

;; --- Encounter-class mapping (docs/gmf-interpreter.md section 4) ---------

(deftest ambulatory-and-wellness-encounters-compile-to-outpatient-visit
  (doseq [class [:ambulatory :wellness]]
    (let [trajectory [(ev :encounter {:t 100 :encounter-class class
                                      :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (= :outpatient-visit (:type (first steps))))
      (is (= {:module "m" :state :s} (:citation (first steps)))))))

(deftest emergency-encounters-compile-to-admission-targeting-an-ed-ward
  (let [trajectory [(ev :encounter {:t 100 :encounter-class :emergency
                                    :codes [{:system :snomed :code "50849002" :display "ED"}]})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= :admission (:type (first steps))))
    (is (= "Emergency" (:location (first steps))))))

(deftest inpatient-encounters-compile-to-admission-targeting-an-inpatient-ward
  (let [trajectory [(ev :encounter {:t 100 :encounter-class :inpatient
                                    :codes [{:system :snomed :code "32485007" :display "Hospital admission"}]})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= :admission (:type (first steps))))
    (is (contains? #{"Renal" "Cardiology"} (:location (first steps))))))

(deftest encounter-end-mirrors-its-opening-encounters-class
  (testing "outpatient-opened -> :outpatient-visit-end"
    (let [trajectory [(ev :encounter {:t 100 :encounter-class :ambulatory :codes []})
                      (ev :encounter-end {:t 110 :references 0})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (= [:outpatient-visit :delay :outpatient-visit-end] (mapv :type steps)))))
  (testing "inpatient-opened -> :discharge"
    (let [trajectory [(ev :encounter {:t 100 :encounter-class :inpatient :codes []})
                      (ev :encounter-end {:t 110 :references 0})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (= [:admission :delay :discharge] (mapv :type steps))))))

;; --- ADR-0133 (restoration cascade): :virtual resolves the deferred
;; decision ADR-0029 D3f left open (gmf.clj's own `encounter-class->
;; keyword` docstring) -- a phone/remote encounter compiles to the SAME
;; :outpatient-visit IR shape :wellness/:ambulatory already do, at BOTH
;; dispatch sites (`encounter->step`/`encounter-end->step`) --------------

(deftest virtual-encounters-compile-to-outpatient-visit-start-and-end
  (testing "start: encounter->step's own case gains :virtual (PRE-FIX,
            red: IllegalArgumentException, no matching clause)"
    (let [trajectory [(ev :encounter {:t 100 :encounter-class :virtual
                                      :codes [{:system :snomed :code "185347001" :display "Encounter for problem"}]})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (= :outpatient-visit (:type (first steps))))
      (is (= {:module "m" :state :s} (:citation (first steps))))))
  (testing "end: encounter-end->step's own opening-class set gains
            :virtual TOO -- patching only the start would silently pair
            a :virtual encounter's own :outpatient-visit with a
            :discharge end, no exception, wrong IR"
    (let [trajectory [(ev :encounter {:t 100 :encounter-class :virtual :codes []})
                      (ev :encounter-end {:t 110 :references 0})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (= [:outpatient-visit :delay :outpatient-visit-end] (mapv :type steps))))))

(deftest a-second-same-episode-encounter-after-the-first-encounter-end-is-dropped
  (testing "M7 finding (docs/gmf-interpreter.md's own M7 survey section,
            confirmed here against synthetic events rather than only
            reasoned about): `encounter-closed?` is a single boolean set by
            the FIRST :encounter-end and never cleared -- it was built to
            stop a module recurring across a patient's WHOLE LIFE
            (sinusitis.json's own Potential_Onset loop, M5b) from minting a
            second admission, but it fires identically for a SAME-EPISODE
            transfer to a different care setting (appendicitis.json's own
            real shape: an ED encounter closes, an inpatient surgical
            encounter opens immediately after, zero elapsed time) -- the
            second encounter, and everything compiled from it, is silently
            dropped. This is the real gap standing between
            appendicitis.json vendoring cleanly (Task 2) and a demo that
            shows its own surgery (Task 3) -- not a throw, not a load
            rejection, a genuine compile-time content-completeness gap,
            recorded here as a permanent regression witness, not merely a
            one-off finding."
    (let [trajectory [(ev :encounter {:t 100 :encounter-class :emergency
                                      :codes [{:system :snomed :code "50849002" :display "ED admission"}]})
                      (ev :encounter-end {:t 110 :references 0})
                      (ev :encounter {:t 110 :encounter-class :inpatient
                                      :codes [{:system :snomed :code "185347001" :display "Encounter for problem"}]})
                      (ev :procedure {:t 111 :codes [{:system :snomed :code "80146002" :display "Excision of appendix"}]})
                      (ev :encounter-end {:t 200 :references 2})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (= [:admission :delay :discharge] (mapv :type steps))
          "the inpatient admission, the appendectomy procedure, and the second discharge are all missing -- exactly the drop this test exists to witness"))))

;; --- Standalone clinical step types ----------------------------------------

(deftest procedure-compiles-to-a-procedure-step-with-codes-and-citation
  (let [codes [{:system :snomed :code "112790001" :display "Nasal sinus endoscopy"}]
        trajectory [(ev :procedure {:t 100 :codes codes})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= [{:type :procedure :codes codes :citation {:module "m" :state :s}}] steps))))

(deftest observation-compiles-with-its-sampled-value-and-unit
  (let [codes [{:system :loinc :code "8310-5" :display "Body temperature"}]
        trajectory [(ev :observation {:t 100 :codes codes :value 38.4 :unit "Cel"})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= :observation (:type (first steps))))
    (is (= 38.4 (:value (first steps))))
    (is (= "Cel" (:unit (first steps))))))

(deftest observation-with-no-sampled-value-omits-it
  (let [trajectory [(ev :observation {:t 100 :codes []})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (not (contains? (first steps) :value)))))

;; --- GMF coverage Wave D stage D1 (2026-08-02, ADR-0029): :observation's
;; new fields, and :diagnostic-report -----------------------------------

(deftest observation-compiles-with-value-code-category-and-reference-range
  (let [value-code {:system :snomed :code "10828004" :display "Positive (qualifier value)"}
        trajectory [(ev :observation {:t 100 :codes [] :value-code value-code :category "laboratory"
                                      :reference-range {:low 95 :high 100} :interpretation :normal})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)
        step (first steps)]
    (is (= value-code (:value-code step)))
    (is (= "laboratory" (:category step)))
    (is (= {:low 95 :high 100} (:reference-range step)))
    (is (= :normal (:interpretation step)))))

(deftest diagnostic-report-compiles-with-report-codes-and-flattened-children
  (let [report-codes [{:system :loinc :code "600-7" :display "Bacteria identified in Blood by Culture"}]
        value-code {:system :snomed :code "10828004" :display "Positive (qualifier value)"}
        trajectory [(ev :diagnostic-report
                        {:t 100 :codes report-codes
                         :observations [{:category "laboratory" :codes [] :value-code value-code}]})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)
        step (first steps)]
    (is (= :diagnostic-report (:type step)))
    (is (= report-codes (:codes step)))
    (is (= {:module "m" :state :s} (:citation step)))
    (is (= [{:codes [] :value-code value-code :category "laboratory"}] (:observations step)))))

(deftest diagnostic-report-with-no-report-level-codes-omits-the-key
  (let [trajectory [(ev :diagnostic-report {:t 100 :observations [{:codes []}]})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (not (contains? (first steps) :codes)))))

(deftest diagnostic-report-child-with-a-numeric-value-compiles-value-and-unit
  (let [trajectory [(ev :diagnostic-report
                        {:t 100 :observations [{:codes [] :value 92.0 :unit "mm[Hg]"}]})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)
        child (first (:observations (first steps)))]
    (is (= 92.0 (:value child)))
    (is (= "mm[Hg]" (:unit child)))))

(deftest medication-order-then-end-carries-a-matching-order-citation
  (let [codes [{:system :rxnorm :code "308191" :display "Amoxicillin"}]
        trajectory [(ev :medication-order {:t 100 :state :rx :codes codes})
                    (ev :medication-end {:t 110 :state :end-rx :references 0})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)
        order-step (first (filter #(= :medication-order (:type %)) steps))
        end-step (first (filter #(= :medication-end (:type %)) steps))]
    (is (= (:citation order-step) (:order-citation end-step)))))

;; --- Condition annotation (docs/gmf-interpreter.md section 1's own
;; "annotation on the enclosing Encounter-mapped step" ruling) --------------

(deftest condition-onset-annotates-the-most-recently-compiled-encounter-step
  (let [codes [{:system :snomed :code "36971009" :display "Sinusitis"}]
        trajectory [(ev :encounter {:t 100 :state :visit :encounter-class :ambulatory :codes []})
                    (ev :condition-onset {:t 100 :state :onset :codes codes})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)
        visit-step (first (filter #(= :outpatient-visit (:type %)) steps))]
    (is (= 1 (count (:conditions visit-step))))
    (is (= codes (:codes (first (:conditions visit-step)))))
    (is (= :condition-onset (:event (first (:conditions visit-step)))))
    (is (= {:module "m" :state :onset} (:citation (first (:conditions visit-step)))))))

(deftest condition-onset-with-no-prior-encounter-is-dropped-not-invented
  (testing "log-only fact, the same shape :step-rejected already
            established (docs/gmf-interpreter.md section 1) -- no step,
            no crash, no fabricated attachment point"
    (let [trajectory [(ev :condition-onset {:t 100 :codes []})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (empty? steps)))))

;; --- GMF coverage Wave C (2026-08-02, ADR-0028, C4): :death ----------------

(def ^:private death-codes [{:system :snomed :code "230690007" :display "Cerebrovascular accident (disorder)"}])

(deftest death-inside-an-encounter-attaches-as-its-terminal-disposition
  (testing "no new IR step type -- reuses :discharge, carrying :disposition
            :expired and the cause-of-death codes verbatim"
    (let [trajectory [(ev :encounter {:t 100 :encounter-class :emergency
                                      :codes [{:system :snomed :code "50849002" :display "ED"}]})
                      (ev :death {:t 110 :codes death-codes})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)
          death-step (last steps)]
      (is (= [:admission :delay :discharge] (mapv :type steps)) "a genuinely delayed death bridges via :delay, the same as any other gap")
      (is (= :expired (:disposition death-step)))
      (is (= death-codes (:codes death-step)))
      (is (= {:module "m" :state :s} (:citation death-step))))))

(deftest death-outside-any-encounter-closes-the-pathway-without-fabricating-a-discharge
  (testing "the same 'no attachment point, don't invent one' precedent
            condition-onset-with-no-prior-encounter already establishes"
    (let [trajectory [(ev :death {:t 100 :codes death-codes})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (empty? steps)))))

(deftest nothing-compiles-after-death-even-if-the-trajectory-somehow-carries-more
  (testing "belt-and-suspenders -- C2 already guarantees this never happens
            in a real interpreter-produced trajectory, checked here anyway
            since compile-trajectory takes a bare vector, not a proof"
    (let [trajectory [(ev :encounter {:t 100 :encounter-class :emergency
                                      :codes [{:system :snomed :code "50849002" :display "ED"}]})
                      (ev :death {:t 100 :codes death-codes})
                      (ev :procedure {:t 120 :state :p :codes []})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (= [:admission :discharge] (mapv :type steps))))))

;; --- Pre-horizon handling: the day -> seconds/minutes boundary is where
;; ratified item 5 (registration-time facts) actually lands -------------

(deftest pre-horizon-encounter-and-procedure-and-observation-are-dropped
  (testing "no operational trajectory event for the encounter machinery
            during history (docs/gmf-interpreter.md section 3) -- enforced
            HERE, since the M5a interpreter itself does not discriminate"
    (let [trajectory [(ev :encounter {:t 10 :pre-horizon true :encounter-class :ambulatory :codes []})
                      (ev :procedure {:t 11 :pre-horizon true :codes []})
                      (ev :observation {:t 12 :pre-horizon true :codes []})
                      (ev :diagnostic-report {:t 13 :pre-horizon true :observations [{:codes []}]})]
          {:keys [steps registration-facts]} (ct/compile-trajectory trajectory facility 100)]
      (is (empty? steps))
      (is (empty? registration-facts)))))

(deftest pre-horizon-condition-and-medication-facts-become-registration-facts
  (let [onset-codes [{:system :snomed :code "36971009" :display "Sinusitis"}]
        med-codes [{:system :rxnorm :code "308191" :display "Amoxicillin"}]
        trajectory [(ev :condition-onset {:t 10 :pre-horizon true :state :onset :codes onset-codes})
                    (ev :medication-order {:t 20 :pre-horizon true :state :rx :codes med-codes})]
        {:keys [steps registration-facts]} (ct/compile-trajectory trajectory facility 100)]
    (is (empty? steps) "never pathway IR -- these ride :registered instead")
    (is (= 2 (count registration-facts)))
    (is (= #{:condition-onset :medication-order} (into #{} (map :event) registration-facts)))
    (is (every? :citation registration-facts))))

;; --- The straddle fix (2026-08-08, ADR-0086, AR-SF-1): the legacy
;; (`history?` false) path gains its own version of the back-reference
;; principle Wave H's `history-phase?` already established -- a
;; straddling encounter (opening pre-horizon, closing and/or
;; intervening content post-horizon) receives the SAME disposition, in
;; full, as a fully pre-horizon one -- the exact shape ADR-0085
;; diagnosed (`clinical-content-only-when-admitted` tripping on a
;; compiled terminal step with no matching compiled opening). These
;; three tests are the legacy-path mirror of the pre-existing
;; `history-mode-straddling-encounter-*` tests above (line 391 on),
;; using `:pre-horizon` (the legacy field) rather than `:phase`.

(deftest legacy-straddling-encounter-emits-nothing-and-nothing-orphaned
  (testing "an encounter that opens pre-horizon claims its own close too,
            even though the close's own raw :pre-horizon is false --
            the whole span drops, leaving no orphaned :discharge/
            :outpatient-visit-end (and no admission either) in :steps"
    (let [trajectory [(ev :encounter {:t 50 :pre-horizon true :encounter-class :inpatient :codes []})
                      (ev :procedure {:t 90 :pre-horizon true :codes []})
                      (ev :observation {:t 95 :pre-horizon false :codes []})
                      (ev :encounter-end {:t 150 :pre-horizon false :references 0})]
          {:keys [steps registration-facts]} (ct/compile-trajectory trajectory facility 100)]
      (is (empty? steps))
      (is (empty? registration-facts)))))

(deftest legacy-straddling-encounter-in-span-facts-still-become-registration-facts
  (testing "a condition-onset/medication-order straddling INSIDE the span
            (its own raw :pre-horizon false, but the span it belongs to
            opened pre-horizon) still lands as a registration-time fact,
            never a fabricated IR step"
    (let [onset-codes [{:system :snomed :code "36971009" :display "Sinusitis"}]
          trajectory [(ev :encounter {:t 50 :pre-horizon true :encounter-class :inpatient :codes []})
                      (ev :condition-onset {:t 95 :pre-horizon false :state :onset :codes onset-codes})
                      (ev :encounter-end {:t 150 :pre-horizon false :references 0})]
          {:keys [steps registration-facts]} (ct/compile-trajectory trajectory facility 100)]
      (is (empty? steps))
      (is (= 1 (count registration-facts)))
      (is (= :condition-onset (:event (first registration-facts)))))))

(deftest legacy-post-straddle-horizon-encounter-still-compiles-normally
  (let [trajectory [(ev :encounter {:t 50 :pre-horizon true :encounter-class :inpatient :codes []})
                    (ev :procedure {:t 90 :pre-horizon true :codes []})
                    (ev :encounter-end {:t 150 :pre-horizon false :references 0})
                    (ev :encounter {:t 200 :pre-horizon false :encounter-class :ambulatory :codes []})
                    (ev :encounter-end {:t 210 :pre-horizon false :references 3})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= [:outpatient-visit :outpatient-visit-end]
           (mapv :type (remove #(= :delay (:type %)) steps)))
        "the dropped straddling encounter never sets encounter-closed?, so
         the loop finds the NEXT (fully horizon) encounter and compiles
         it normally -- byte-identical to the pre-existing history-mode
         precedent's own proof")))

(deftest legacy-non-straddling-fully-pre-horizon-encounter-unaffected
  (testing "an encounter fully inside history (opening AND closing both
            pre-horizon) is untouched by the straddle generalization --
            still dropped whole, the pre-existing behavior, and NOT
            counted as a suppressed straddle span"
    (let [trajectory [(ev :encounter {:t 10 :pre-horizon true :encounter-class :ambulatory :codes []})
                      (ev :procedure {:t 11 :pre-horizon true :codes []})
                      (ev :encounter-end {:t 12 :pre-horizon true :references 0})]
          {:keys [steps registration-facts suppressed-straddle-spans]}
          (ct/compile-trajectory trajectory facility 100)]
      (is (empty? steps))
      (is (empty? registration-facts))
      (is (= 0 suppressed-straddle-spans)))))

(deftest suppressed-straddle-spans-counts-spans-not-events
  (testing "AR-SF-7: a zero-cost additive diagnostic -- spans, not
            events, and only genuine straddles (raw pre-horizon on the
            opening, raw NOT-pre-horizon on the closing)"
    (let [trajectory [(ev :encounter {:t 50 :pre-horizon true :encounter-class :inpatient :codes []})
                      (ev :procedure {:t 90 :pre-horizon true :codes []})
                      (ev :observation {:t 95 :pre-horizon false :codes []})
                      (ev :observation {:t 96 :pre-horizon false :codes []})
                      (ev :encounter-end {:t 150 :pre-horizon false :references 0})]
          {:keys [suppressed-straddle-spans]} (ct/compile-trajectory trajectory facility 100)]
      (is (= 1 suppressed-straddle-spans)
          "one span, regardless of how many in-span events it swallowed"))))

;; --- The day -> minutes conversion, exactly once (durations rule) ---------

(deftest the-gap-between-registration-and-the-first-horizon-event-becomes-a-delay-in-minutes
  (let [trajectory [(ev :procedure {:t 105 :codes []})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= [{:type :delay :from 7200 :to 7200} {:type :procedure :codes [] :citation {:module "m" :state :s}}]
           steps)
        "5 days * 1440 minutes/day = 7200")))

(deftest no-delay-step-when-the-gap-is-zero
  (let [trajectory [(ev :procedure {:t 100 :codes []})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= 1 (count steps)))
    (is (= :procedure (:type (first steps))))))

(deftest gaps-between-consecutive-compiled-steps-also-become-delays
  (let [trajectory [(ev :procedure {:t 100 :state :p1 :codes []})
                    (ev :procedure {:t 103 :state :p2 :codes []})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= [:procedure :delay :procedure] (mapv :type steps)))
    (is (= 4320 (:from (second steps))) "3 days * 1440")))

;; --- Every compiled step validates as real pathway IR ---------------------

(deftest every-compiled-pathway-is-valid-ir
  (let [trajectory [(ev :encounter {:t 100 :state :visit :encounter-class :ambulatory
                                    :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]})
                    (ev :observation {:t 101 :codes [{:system :loinc :code "8310-5" :display "Temp"}] :value 38.0})
                    (ev :medication-order {:t 102 :state :rx :codes [{:system :rxnorm :code "308191" :display "Amox"}]})
                    (ev :medication-end {:t 103 :state :end-rx :references 2})
                    (ev :encounter-end {:t 104 :references 0})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (sim-model/valid? {:name "compiled" :steps steps}))))

;; --- End-to-end property tests, against the REAL interpreter output ------

(def fixture-clinic
  (:payload (gmf/load-module "fixture-clinic" (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json")))))

(def sinusitis
  (:payload (gmf/load-module "sinusitis" (slurp (io/resource "sim/modules/sinusitis.json")))))

(defn- adult [seed] (assoc (sim-model/persona (Random. seed) {}) :sex :female))
(defn- registration-t-for [p] (+ (interp/dob-epoch-day p) (* 365 20)))

(defn- accounted-for?
  "Every trajectory event is either (a) a horizon-phase event that
  produced a real compiled step, (b) a condition-onset/end that landed as
  an annotation OR was legitimately dropped (no open encounter), (c) a
  pre-horizon event dropped or captured as a registration-fact, or (d)
  everything from the FIRST horizon-phase :encounter-end onward -- this
  project's own single-encounter-horizon scope (sim/ADR-0007 point 3) -- per
  this session's own resolution -- never silently unaccounted for."
  [trajectory {:keys [steps registration-facts]}]
  (let [step-citations (into #{} (keep :citation) steps)
        annotation-citations (into #{} (comp (keep :conditions) cat (map :citation)) steps)
        registration-citations (into #{} (map :citation) registration-facts)
        first-close-idx (first (keep-indexed (fn [i ev] (when (and (not (:pre-horizon ev)) (= :encounter-end (:event ev))) i))
                                             trajectory))]
    (every? (fn [[i event]]
              (let [c {:module (:module event) :state (:state event)}]
                (or (contains? step-citations c)
                    (contains? annotation-citations c)
                    (contains? registration-citations c)
                    ;; dropped, documented: pre-horizon encounter-machinery,
                    ;; a condition with no open encounter to attach to, or
                    ;; past the first encounter this run's own scope closed.
                    (:pre-horizon event)
                    (#{:condition-onset :condition-end} (:event event))
                    (and first-close-idx (> i first-close-idx)))))
            (map-indexed vector trajectory))))

(defspec clinical-content-preservation-fixture-clinic 150
  (prop/for-all [seed gen/large-integer]
    (let [p (adult seed)
          reg-t (registration-t-for p)
          {:keys [trajectory]} (interp/run-module fixture-clinic (Random. seed) p reg-t)
          compiled (ct/compile-trajectory trajectory sim-model/default-facility reg-t)]
      (accounted-for? trajectory compiled))))

(defspec clinical-content-preservation-sinusitis 150
  (prop/for-all [seed gen/large-integer]
    (let [p (adult seed)
          reg-t (registration-t-for p)
          {:keys [trajectory]} (interp/run-module sinusitis (Random. seed) p reg-t (+ reg-t 90))]
      (accounted-for? trajectory (ct/compile-trajectory trajectory sim-model/default-facility reg-t)))))

(defspec every-compiled-step-cites-a-real-trajectory-event
  150
  (prop/for-all [seed gen/large-integer]
    (let [p (adult seed)
          reg-t (registration-t-for p)
          {:keys [trajectory]} (interp/run-module fixture-clinic (Random. seed) p reg-t)
          {:keys [steps]} (ct/compile-trajectory trajectory sim-model/default-facility reg-t)
          trajectory-citations (into #{} (map (fn [e] {:module (:module e) :state (:state e)})) trajectory)]
      (every? (fn [step] (or (nil? (:citation step)) (contains? trajectory-citations (:citation step)))) steps))))

(defspec compiled-pathway-is-always-valid-ir 150
  (prop/for-all [seed gen/large-integer]
    (let [p (adult seed)
          reg-t (registration-t-for p)
          {:keys [trajectory]} (interp/run-module sinusitis (Random. seed) p reg-t (+ reg-t 90))
          {:keys [steps]} (ct/compile-trajectory trajectory sim-model/default-facility reg-t)]
      (sim-model/valid? {:name "compiled" :steps steps}))))

;; --- Wave H pre-roll, Step 2 (2026-08-04, ADR-0042 AR-1/AR-2): the
;; `history?` true path -- uniform drop by `:phase`, no dropped-types/
;; fact-types bucketing, and the straddle rule (an `:encounter-end`
;; trusts whatever `:phase` the interpreter already inherited onto it,
;; never re-derived here -- `ehrt.sim-trajectory.gmf-interpreter/mark-
;; phase` is where AR-2 itself lives; this namespace only filters).

(defn- ev-h
  "Test-fixture convenience for the history? true path -- `:phase`
  instead of `:pre-horizon` (the legacy `ev` helper's own field, above,
  irrelevant once `history?` is true)."
  [event overrides]
  (merge {:module "m" :state :s :t 0 :event event} overrides))

(deftest history-mode-drops-every-history-phase-event-uniformly-no-registration-facts
  (testing "unlike legacy history?=false, condition/medication/care-plan
            content does NOT become a registration-fact under history?
            true -- AR-1's own 'compile step DROPS them, generalized to
            a phase'"
    (let [trajectory [(ev-h :encounter {:t 10 :phase :history :encounter-class :ambulatory :codes []})
                      (ev-h :condition-onset {:t 10 :phase :history :codes []})
                      (ev-h :medication-order {:t 10 :phase :history :codes []})
                      (ev-h :procedure {:t 10 :phase :history :codes []})
                      (ev-h :observation {:t 10 :phase :history :codes []})
                      (ev-h :diagnostic-report {:t 10 :phase :history :observations [{:codes []}]})
                      (ev-h :care-plan-start {:t 10 :phase :history :codes []})
                      (ev-h :encounter-end {:t 10 :phase :history :references 0})]
          {:keys [steps registration-facts]} (ct/compile-trajectory trajectory facility 100 true)]
      (is (empty? steps))
      (is (empty? registration-facts)))))

(deftest history-mode-straddling-encounter-emits-nothing-and-nothing-orphaned
  (testing "AR-2: an encounter that opens in history claims its own close
            too, even though the close's own raw :t (150) is well past
            registration-t (100) -- the interpreter already resolved
            this via inheritance before this namespace ever sees the
            event; this test proves the FILTER trusts that mark and
            drops the whole span, leaving no orphaned :discharge (and no
            admission either) in :steps"
    (let [trajectory [(ev-h :encounter {:t 50 :phase :history :encounter-class :inpatient :codes []})
                      (ev-h :procedure {:t 90 :phase :history :codes []})
                      (ev-h :encounter-end {:t 150 :phase :history :references 0})]
          {:keys [steps registration-facts]} (ct/compile-trajectory trajectory facility 100 true)]
      (is (empty? steps))
      (is (empty? registration-facts)))))

(deftest history-mode-post-straddle-horizon-encounter-still-compiles-normally
  (let [trajectory [(ev-h :encounter {:t 50 :phase :history :encounter-class :inpatient :codes []})
                    (ev-h :procedure {:t 90 :phase :history :codes []})
                    (ev-h :encounter-end {:t 150 :phase :history :references 0})
                    (ev-h :encounter {:t 200 :phase :horizon :encounter-class :ambulatory :codes []})
                    (ev-h :encounter-end {:t 210 :phase :horizon :references 3})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100 true)]
    (is (= [:outpatient-visit :outpatient-visit-end]
           (mapv :type (remove #(= :delay (:type %)) steps)))
        "the dropped straddling encounter never sets encounter-closed?, so
         the loop finds the NEXT (fully horizon) encounter and compiles
         it as this run's own operational one")))
