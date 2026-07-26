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
               [:force-placement {:optional true} ForcePlacement]]]])

(def Pathway
  [:map
   [:name :string]
   [:steps [:vector Step]]])

(defn valid? [pathway] (m/validate Pathway pathway))

(defn explain [pathway] (m/explain Pathway pathway))

(def sample-admission-discharge
  "The walking-skeleton pathway: admit, dwell, discharge. Mirrors
  Simulated Hospital's `simple_admission` example in spirit."
  {:name "simple-admission"
   :steps [{:type :admission :location "Renal" :reason "Kidney problems"}
           {:type :delay :from 60 :to 240}
           {:type :discharge}]})
