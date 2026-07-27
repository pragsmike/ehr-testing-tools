(ns ehr-testing-sim.pathway
  "The intermediate pathway representation (IR): the single format that
  both hand-authored scenario scripts and generated trajectories
  compile to, and the only format the engine executes.

  Lineage: the step vocabulary derives from Google's Simulated Hospital
  pathway steps (operational events: admission, transfer, discharge,
  churn like bed swaps, cancellations, merges); the *generation* of
  pathways from probabilistic clinical modules (Synthea GMF) is a
  separate, later layer that compiles down to this IR. Scripted and
  generated trajectories are therefore indistinguishable to the engine.

  A pathway is data (EDN), never code. Concepts are carried as coded
  triplets {:system :code :display} so every emitter (HL7v2 now,
  FHIR/CDA later) renders codes natively with no re-mapping -- codes
  are properties of patient state, not of wire formats.

  The v0 step set was deliberately tiny (walking skeleton): :admission,
  :delay, :discharge. Milestone M1 (docs/operational-models.md) adds
  :transfer, engine-assigned via the allocation ladder. The full
  Simulated-Hospital-derived vocabulary (bed-swap, *-in-error, cancel-*,
  merge, order, result, ...) lands step by step, each with its engine
  decide/evolve methods and its invariants in ehr-testing-sim.check."
  (:require [malli.core :as m]))

(def Concept
  "A coded clinical or administrative concept. :system is a keyword
  naming the code system (:snomed :loinc :rxnorm :icd10cm :cvx ...)."
  [:map
   [:system :keyword]
   [:code :string]
   [:display {:optional true} :string]])

(def ForcePlacement
  "Authoring escape hatch (docs/operational-models.md): forces a
  specific bed, overriding the allocation ladder outright and
  exempting the placement from the surge-only-when-full invariant."
  [:map [:ward :string] [:bed :string]])

(def Step
  [:multi {:dispatch :type}
   [:admission [:map
                [:type [:= :admission]]
                [:location :string]
                [:reason {:optional true} [:or :string Concept]]
                [:force-placement {:optional true} ForcePlacement]]]
   [:delay [:map
            [:type [:= :delay]]
            ;; minutes (authoring ergonomics, ADR-0011 -- the engine's
            ;; own clock is seconds; it converts minutes -> seconds at
            ;; decide-time, this field's authored unit never changes);
            ;; the engine samples uniformly in [from, to] from its own
            ;; seeded RNG (determinism guarantee).
            [:from :int]
            [:to :int]]]
   [:discharge [:map
                [:type [:= :discharge]]]]
   [:transfer [:map
               [:type [:= :transfer]]
               [:location :string]
               [:force-placement {:optional true} ForcePlacement]]]
   ;; --- M2b churn family (docs/patient-state-model.md's event-validity
   ;; table; ADR-0010's :participants). Cancel-* steps name no target
   ;; explicitly -- decide finds the most recent uncancelled event of the
   ;; class being cancelled in THIS patient's own log (docs/patient-
   ;; state-model.md's deterministic-event-id section), the same way
   ;; :discharge's bed-ready coupling already finds its target implicitly
   ;; rather than being told.
   [:cancel-admit [:map [:type [:= :cancel-admit]]]]
   [:cancel-transfer [:map [:type [:= :cancel-transfer]]]]
   [:cancel-discharge [:map [:type [:= :cancel-discharge]]]]
   ;; A transfer immediately followed by its own A12, in-error marked
   ;; (docs/patient-state-model.md) -- one IR step, decide emits both
   ;; events atomically.
   [:transfer-in-error [:map
                        [:type [:= :transfer-in-error]]
                        [:location :string]
                        [:force-placement {:optional true} ForcePlacement]]]
   ;; Genuinely two-participant (ADR-0010). `:with` is the scripted-
   ;; authoring escape hatch (same precedent as :force-placement) naming
   ;; the peer patient-id explicitly; omitted, decide picks a uniformly
   ;; seeded eligible peer from `world` -- the same "decide resolves the
   ;; target dynamically" shape the bed-ready transfer already uses, so
   ;; InjectChurn (M2b) can insert these without knowing patient-ids.
   [:bed-swap [:map [:type [:= :bed-swap]] [:with {:optional true} :string]]]
   [:merge [:map [:type [:= :merge]] [:with {:optional true} :string]]]
   ;; M3: order-profiles catalytic (docs/sim-theory.edn, docs/operational-
   ;; models.md). :profile keys into ehr-testing-sim.order-profiles'
   ;; catalog (world's :order-profiles, default order-profiles/default-
   ;; profiles). NO authorable :result step -- the engine auto-pairs a
   ;; result-available event after a profile-sampled turnaround
   ;; (engine.clj's own :order decide method), the choice this
   ;; milestone documents there: it keeps authored pathways ergonomic
   ;; (just write the order; the result follows automatically) and
   ;; avoids inventing an :order-ref authoring burden a hand-authored
   ;; :result step would need.
   [:order [:map [:type [:= :order]] [:profile :keyword]]]])

(def Pathway
  [:map
   [:name :string]
   [:steps [:vector Step]]])

(defn valid? [pathway] (m/validate Pathway pathway))

(defn explain [pathway] (m/explain Pathway pathway))

;; --- M3-adjacent: per-patient pathway assignment (roadmap.md's M3 entry,
;; SimHospital's percentage_of_patients analogue) -------------------------

(def PathwayAssignment
  "One entry in a :pathways run-config vector -- EITHER a weighted-pool
  member ({:pathway :weight}, a sampled mixture across the patient
  population) OR an explicit per-patient override ({:patient-ordinal
  :pathway}, a scripted assignment for a specific arrival), never both
  at once. `ehr-testing-sim.engine/run`'s degenerate case -- today's
  single :pathway config -- is a :pathways vector of exactly one
  weighted entry with :weight 1, unchanged behavior for every other
  entry shape."
  [:or
   [:map {:closed true} [:pathway Pathway] [:weight [:or :int :double]]]
   [:map {:closed true} [:patient-ordinal :int] [:pathway Pathway]]])

(def PathwaysConfig
  [:vector PathwayAssignment])

(defn valid-pathways-config? [config] (m/validate PathwaysConfig config))

(defn explain-pathways-config [config] (m/explain PathwaysConfig config))

(def sample-admission-discharge
  "The walking-skeleton pathway: admit, dwell, discharge. Mirrors
  Simulated Hospital's `simple_admission` example in spirit."
  {:name "simple-admission"
   :steps [{:type :admission :location "Renal" :reason "Kidney problems"}
           {:type :delay :from 60 :to 240}
           {:type :discharge}]})
