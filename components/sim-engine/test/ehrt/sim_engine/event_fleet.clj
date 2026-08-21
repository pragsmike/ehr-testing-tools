(ns ehrt.sim-engine.event-fleet
  "The deterministic fixture fleet: four small engine runs whose union
  produces EXACTLY the 21 kinds `ehrt.sim-engine.event-schema` declares.

  Lives on the test path, and is shared by two consumers that must not
  be allowed to disagree:

  - `ehrt.sim-engine.event-schema-test`, which asserts the fleet's own
    kinds equal the declared vocabulary in both directions -- the
    coverage assertion that stops the schema tests passing vacuously.
  - `make event-schema-examples`, which lifts ONE real event per kind
    out of this same fleet into
    `resources/sim-engine/event-examples.edn`, the examples
    `docs/formats.md`'s generated event-log section shows a reader.

  Extracted here specifically so the documented example and the gated
  contract come from the SAME runs. Two fleets would drift, and the
  drift would land in the one place a consumer is most likely to copy
  from.

  WHY FIXTURES RATHER THAN THE DEMO CORPORA. The census
  (`.agents/plans/2026-08-16-event-log-census.md`) needed 400-patient,
  ten-year module runs to reach `:medication-end` and
  `:diagnostic-report`, and five churn seeds to reach `:cancel-admit`
  and `:step-rejected` -- minutes of work, per-push-hostile, and
  seed-fragile besides (a scenario retune could silently stop producing
  a kind, and the gate would quietly stop checking it). Here the churn
  family is authored as explicit IR steps rather than hunted for, and
  the clinical family comes from one GMF fixture module that walks the
  whole state vocabulary in a single encounter."
  (:require [ehrt.sim-engine.engine :as engine]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [clojure.pprint]))

;; --- fixtures -------------------------------------------------------------

(def crowded-facility
  "Two Renal rungs and an ED overflow -- enough contention for a
  bed-swap and a boarding merge, small enough to stay fast."
  {:id :schema-fixture
   :wards [{:id :ed :name "ED" :beds 0 :surge-slots 4 :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 1 :surge-slots 1 :surge-format "%s-H%02d" :class :inpatient}]})

(def clinical-fixture-module
  "One ambulatory encounter that walks the entire clinical state
  vocabulary, so a single small run produces every clinically-sourced
  event kind. Authored here rather than borrowed from a vendored root
  because a real module reaches these states only probabilistically --
  the census needed 400 patients over ten simulated years to see one
  `:medication-end`.

  The medication and care-plan pairs both close INSIDE the encounter,
  by explicit `:medication-order` / `:careplan` state citation, which
  is what makes `:medication-end` and `:care-plan-end` land as horizon
  events with RESOLVED back-references. That matters beyond coverage:
  the census observed all seven `:care-plan-end` events with a nil
  `:care-plan-citation`, and this fixture is the control proving the
  mechanism itself works -- the nils came from vendored modules using
  `referenced_by_attribute`, whose resolution `ehrt.patient-simulator.gmf`
  deliberately never declared for the CarePlan family (census S-2).

  THE LEAD-IN DELAY IS LOAD-BEARING, and was found by running rather
  than reasoned about. The GMF walk starts at the patient's DOB, and
  everything before the run's registration instant is history --
  dropped, or folded into `:pre-horizon-facts`. A newborn's DOB is
  still sampled up to a year before today, so an encounter authored at
  t=0 lands in that history window and produces nothing at all. 400
  days clears it for every DOB the persona sampler can draw."
  {:id "schema-fixture-mod" :name "Schema Fixture"
   :states {:initial {:type :initial :direct-transition :lead-in}
            :lead-in {:type :delay :exact {:quantity 400 :unit "days"}
                      :direct-transition :visit}
            :visit {:type :encounter :encounter-class :ambulatory
                    :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]
                    :direct-transition :the-procedure}
            :the-procedure {:type :procedure
                            :codes [{:system :snomed :code "80146002" :display "Excision of appendix"}]
                            :direct-transition :the-observation}
            :the-observation {:type :observation :category "vital-signs" :unit "Cel"
                              :codes [{:system :loinc :code "8310-5" :display "Body temperature"}]
                              :range {:low 37.5 :high 38.0}
                              :direct-transition :the-panel}
            :the-panel {:type :diagnostic-report
                        :codes [{:system :loinc :code "58410-2" :display "CBC panel"}]
                        :observations [{:category "laboratory" :unit "K/uL"
                                        :codes [{:system :loinc :code "6690-2" :display "Leukocytes"}]
                                        :range {:low 4.5 :high 11.0}}
                                       {:category "laboratory"
                                        :codes [{:system :snomed :code "10828004" :display "Positive"}]
                                        :value-code {:system :snomed :code "10828004" :display "Positive"}}]
                        :direct-transition :the-med}
            :the-med {:type :medication-order
                      :codes [{:system :rxnorm :code "308182" :display "Amoxicillin 250 MG"}]
                      :direct-transition :the-plan}
            :the-plan {:type :care-plan-start
                       :codes [{:system :snomed :code "324911001" :display "Antibiotic therapy"}]
                       :activities [{:system :snomed :code "710824005" :display "Assessment of health"}]
                       :direct-transition :course}
            :course {:type :delay :exact {:quantity 10 :unit "days"} :direct-transition :stop-med}
            :stop-med {:type :medication-end :medication-order :the-med :direct-transition :stop-plan}
            :stop-plan {:type :care-plan-end :careplan :the-plan :direct-transition :visit-end}
            :visit-end {:type :encounter-end :direct-transition :done}
            :done {:type :terminal}}})

;; --- the fleet ------------------------------------------------------------
;;
;; Each entry is [label run-result]. Together their kinds must equal the
;; schema's own declared vocabulary, exactly.

(defn clinical-run
  []
  (engine/run {:seed 11 :patients 2 :arrival-gap 0
               :pathway {:name "module-only" :steps []}
               ;; Newborns, so the walk's own clock starts near the run's
               ;; registration instant -- see the fixture's lead-in note.
               :persona-config {:age-min 0 :age-max 0}
               :modules [(patient-simulator/singleton-closure clinical-fixture-module)]
               :module-assignment [{:module-id "schema-fixture-mod" :weight 1}]
               :module-horizon-days 1200}))

(defn operational-run
  "The order/result pair plus the plain admit-transfer-discharge arc."
  []
  (engine/run {:seed 12 :patients 2 :arrival-gap 0
               :facility crowded-facility
               :pathways [{:patient-ordinal 0
                           :pathway {:name "worked-up"
                                     :steps [{:type :admission :location "ED" :reason "Chest pain"}
                                             {:type :order :profile :cbc}
                                             {:type :delay :from 60 :to 60}
                                             {:type :transfer :location "Renal"}
                                             {:type :discharge}]}}
                          {:patient-ordinal 1
                           :pathway {:name "quick" :steps [{:type :admission :location "ED"}
                                                           {:type :discharge}]}}]}))

(defn churn-run
  "The churn family, authored as explicit IR rather than hunted across
  seeds -- `:step-rejected` comes from a cancel-admit attempted by a
  patient who was never admitted (`:illegal-cancel-admit`), which is a
  legal log, not a bug."
  []
  (let [seed 13
        p1 (engine/patient-id-for seed 1)]
    (engine/run {:seed seed :patients 4 :arrival-gap 0
                 :facility crowded-facility
                 :pathways [{:patient-ordinal 0
                             :pathway {:name "cancels"
                                       :steps [{:type :admission :location "Renal"}
                                               {:type :transfer-in-error :location "ED"}
                                               {:type :discharge}
                                               {:type :cancel-discharge}
                                               {:type :bed-swap :with p1}
                                               {:type :merge :with p1}]}}
                            {:patient-ordinal 1
                             :pathway {:name "peer" :steps [{:type :admission :location "Renal"}]}}
                            {:patient-ordinal 2
                             :pathway {:name "admit-then-unadmit"
                                       :steps [{:type :admission :location "ED"}
                                               {:type :cancel-admit}]}}
                            ;; Never admitted: the cancel-admit below is
                            ;; rejected, which is exactly the point.
                            {:patient-ordinal 3
                             :pathway {:name "illegal" :steps [{:type :cancel-admit}]}}]})))

(defn death-run
  "The `:disposition :expired` discharge -- 1 event in the census's own
  4,997, and 0 in anything the docs teach, so it gets its own fixture
  rather than being left to chance."
  []
  (engine/run {:seed 14 :patients 1 :arrival-gap 0
               :facility crowded-facility
               :pathways [{:patient-ordinal 0
                           :pathway {:name "expires"
                                     :steps [{:type :admission :location "Renal"}
                                             {:type :discharge :disposition :expired
                                              :codes [{:system :snomed :code "410429000"
                                                       :display "Cardiac arrest"}]}]}}]}))

(defn fleet
  []
  [["clinical" (clinical-run)]
   ["operational" (operational-run)]
   ["churn" (churn-run)]
   ["death" (death-run)]])

;; --- examples for docs/formats.md -----------------------------------------

(defn examples
  "ONE real event per kind, lifted from the fleet -- the examples
  `docs/formats.md`'s generated event-log section shows a reader.

  Real, in the sense that matters: every one came out of an actual
  `engine/run`, not out of a hand-written sample that could quietly
  describe a shape the engine never emits. That is the same reason the
  census enumerated from corpora rather than from the schema.

  Selection is deterministic and explained rather than incidental: for
  each kind, the FIRST event of that kind in fleet order. Sorting by
  kind keeps the written resource stable under fleet reordering, so a
  fleet edit that does not change what the engine emits does not churn
  the committed file."
  []
  (into (sorted-map)
        (for [[kind evs] (group-by :event (mapcat (comp :ground-truth second) (fleet)))]
          [kind (first evs)])))

(def ^:private examples-header
  ";; GENERATED -- do not edit by hand.\n;;\n;; One real ground-truth event per kind, lifted from the deterministic\n;; fixture fleet in ehrt.sim-engine.event-fleet (test path) -- every one\n;; produced by an actual engine/run, never hand-written. Read by\n;; ehrt.docs-tooling.event-log-doc to render docs/formats.md's own\n;; event-log section, so the examples a consumer copies from are the\n;; same events the contract gate gets run against.\n;;\n;; Regenerate with `make event-schema-examples` (also run by `make\n;; docsgen`, so CI's freshness diff catches a stale example).\n")

(defn write-examples!
  "Writes `examples` to the source tree. `make event-schema-examples`."
  [_]
  (spit "components/sim-engine/resources/sim-engine/event-examples.edn"
        (str examples-header
             (with-out-str (clojure.pprint/pprint (examples))))))
