(ns ehrt.sim-model.pathway
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
  are properties of patient state, not of wire formats (`sim/ADR-0002`).

  The v0 step set was deliberately tiny (walking skeleton): :admission,
  :delay, :discharge. Milestone M1 (docs/operational-models.md) adds
  :transfer, engine-assigned via the allocation ladder. The full
  Simulated-Hospital-derived vocabulary (bed-swap, *-in-error, cancel-*,
  merge, order, result, ...) lands step by step, each with its engine
  decide/evolve methods and its invariants in ehrt.sim.check."
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

(def Citation
  "M5b (docs/gmf-interpreter.md section 6, obligation 3 -- provenance):
  the {:module :state} back-reference a CompileTrajectory-produced IR
  step carries, riding straight through from the trajectory event it
  realizes, which itself cites the module/state that produced IT
  (docs/gmf-interpreter.md section 6, obligation 1) -- the glass-box
  chain is three links long (module state -> trajectory event ->
  compiled IR step), and this is the third link. Present ONLY on a
  compiled step; a hand-authored step was never realized from any
  trajectory event, so it simply omits this optional field."
  [:map [:module :string] [:state :keyword]])

(def ConditionAnnotation
  "M5b: a ConditionOnset/ConditionEnd trajectory event compiles to an
  ANNOTATION on its enclosing Encounter-mapped step, never a standalone
  IR step of its own (docs/gmf-interpreter.md section 1's own table --
  this project's pathway IR has no diagnosis-list step yet). `:event` is
  which of the pair this is; `:references` mirrors the trajectory
  event's own (a ConditionEnd's back-reference to its ConditionOnset,
  by trajectory index -- docs/gmf-interpreter.md section 1). `:codes` is
  {:optional true}, not required: a ConditionEnd's own codes are
  resolved from its referenced onset when one exists, but a real
  vendored module can author that reference via `referenced_by_attribute`
  rather than a direct state citation (M5b finding, docs/gmf-
  interpreter.md's own findings section) -- the interpreter doesn't
  resolve THAT reference shape, so the annotation is left codeless
  rather than fabricating a concept it was never actually told."
  [:map
   [:event [:enum :condition-onset :condition-end]]
   [:codes {:optional true} [:maybe [:vector Concept]]]
   [:citation Citation]
   [:references {:optional true} [:maybe :int]]])

(def Step
  [:multi {:dispatch :type}
   [:admission [:map
                [:type [:= :admission]]
                [:location :string]
                [:reason {:optional true} [:or :string Concept]]
                [:force-placement {:optional true} ForcePlacement]
                ;; M5b: present only on a CompileTrajectory-produced step.
                [:citation {:optional true} Citation]
                [:conditions {:optional true} [:vector ConditionAnnotation]]]]
   [:delay [:map
            [:type [:= :delay]]
            ;; minutes (authoring ergonomics, sim/ADR-0011 -- the engine's
            ;; own clock is seconds; it converts minutes -> seconds at
            ;; decide-time, this field's authored unit never changes);
            ;; the engine samples uniformly in [from, to] from its own
            ;; seeded RNG (determinism guarantee). M5b extends this same
            ;; rule with a third unit (docs/patient-state-model.md's
            ;; durations rule): CompileTrajectory's own interpreter-days
            ;; -> authored-minutes conversion, at the ONE place a day-
            ;; denominated trajectory gap becomes a compiled :delay --
            ;; never a fourth engine-side conversion.
            [:from :int]
            [:to :int]]]
   [:discharge [:map
                [:type [:= :discharge]]
                [:citation {:optional true} Citation]]]
   [:transfer [:map
               [:type [:= :transfer]]
               [:location :string]
               [:force-placement {:optional true} ForcePlacement]]]
   ;; --- M2b churn family (docs/patient-state-model.md's event-validity
   ;; table; sim/ADR-0010's :participants). Cancel-* steps name no target
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
   ;; Genuinely two-participant (sim/ADR-0010). `:with` is the scripted-
   ;; authoring escape hatch (same precedent as :force-placement) naming
   ;; the peer patient-id explicitly; omitted, decide picks a uniformly
   ;; seeded eligible peer from `world` -- the same "decide resolves the
   ;; target dynamically" shape the bed-ready transfer already uses, so
   ;; InjectChurn (M2b) can insert these without knowing patient-ids.
   [:bed-swap [:map [:type [:= :bed-swap]] [:with {:optional true} :string]]]
   [:merge [:map [:type [:= :merge]] [:with {:optional true} :string]]]
   ;; M3: order-profiles catalytic (docs/sim-theory.edn, docs/operational-
   ;; models.md). :profile keys into ehrt.sim.order-profiles'
   ;; catalog (world's :order-profiles, default order-profiles/default-
   ;; profiles). NO authorable :result step -- the engine auto-pairs a
   ;; result-available event after a profile-sampled turnaround
   ;; (engine.clj's own :order decide method), the choice this
   ;; milestone documents there: it keeps authored pathways ergonomic
   ;; (just write the order; the result follows automatically) and
   ;; avoids inventing an :order-ref authoring burden a hand-authored
   ;; :result step would need.
   [:order [:map [:type [:= :order]] [:profile :keyword]]]
   ;; M5b: docs/gmf-interpreter.md section 4's outpatient sketch, items
   ;; 5-7 -- NO :location field (unlike :admission/:transfer): an
   ;; outpatient encounter occupies no bed, so there is no ward for the
   ;; allocation ladder to consult and no ward name for an author to
   ;; supply. Paired explicitly (like :admission/:discharge), never
   ;; auto-paired the way :order/:result-followup is -- a GMF module's
   ;; own Encounter/EncounterEnd pair already brackets start and end, so
   ;; there is no turnaround time to sample.
   [:outpatient-visit [:map
                       [:type [:= :outpatient-visit]]
                       [:reason {:optional true} [:or :string Concept]]
                       [:citation {:optional true} Citation]
                       [:conditions {:optional true} [:vector ConditionAnnotation]]]]
   [:outpatient-visit-end [:map [:type [:= :outpatient-visit-end]] [:citation {:optional true} Citation]]]
   ;; --- M5b: CompileTrajectory's own new step types (docs/gmf-
   ;; interpreter.md section 1's table) -- :procedure/:observation/
   ;; :medication-order/:medication-end. Every one carries :citation
   ;; (compiled) or omits it (hand-authored) -- these are compile targets
   ;; first, author-facing IR second; nothing stops a scenario author
   ;; from writing one directly, the same way :order already works.
   [:procedure [:map
                [:type [:= :procedure]]
                [:codes [:vector Concept]]
                [:citation {:optional true} Citation]]]
   [:observation [:map
                  [:type [:= :observation]]
                  [:codes [:vector Concept]]
                  [:value {:optional true} number?]
                  [:unit {:optional true} :string]
                  [:citation {:optional true} Citation]]]
   [:medication-order [:map
                       [:type [:= :medication-order]]
                       [:codes [:vector Concept]]
                       [:citation {:optional true} Citation]]]
   [:medication-end [:map
                     [:type [:= :medication-end]]
                     ;; the compiled :medication-order STEP's own
                     ;; citation, not a pathway-position index -- glass-
                     ;; box resolution by module/state, the same
                     ;; citation-matching mechanism CompileTrajectory
                     ;; itself uses to find the step to annotate for
                     ;; ConditionOnset/ConditionEnd (never a positional
                     ;; index, which churn/other IR transforms could
                     ;; invalidate by inserting steps around it).
                     [:order-citation {:optional true} Citation]
                     [:citation {:optional true} Citation]]]])

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
  at once. `ehrt.sim.engine/run`'s degenerate case -- today's
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
