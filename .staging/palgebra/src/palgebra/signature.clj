(ns palgebra.signature
  "Generic signature-loading machinery for the palgebra diagram
  language (design D13, docs/palgebra-design.md): the shapes of a
  signature's stages, union resources, and external stages, and their
  validation -- parameterized by the caller's own stage-kind set so no
  EHR (or any other instantiating repo's) vocabulary appears here.
  `read-signature-edn` reads any signature-shaped EDN document off
  disk; this namespace has no opinion on what a caller does with the
  data once loaded. Claimed from ehr-testing-tools.pipeline
  (`.agents/plans/judge-gate-refactor.md` Phase 2) -- moved and
  parameterized, not rewritten toward the specified language."
  (:require [clojure.edn :as edn]
            [malli.core :as m]))

(defn stage-schema
  "The Stage schema, parameterized by `kinds` -- the set of keyword
  stage kinds this signature admits. A caller's kind set is signature
  data (e.g. ehr-testing-tools' docs/signature.edn), not something
  this namespace hardcodes."
  [kinds]
  [:map
   [:id :keyword]
   [:label :string]
   [:kind (into [:enum] kinds)]
   [:status [:enum :built :planned]]
   [:inputs [:vector :string]]
   [:outputs [:vector :string]]
   [:catalytic {:optional true} [:vector :string]]
   [:laws {:optional true} [:vector :string]]
   [:contract {:optional true} :string]])

(def UnionResource
  "A named resource declared as the union of others -- a stage
  consuming the union accepts any member. :resource and :union-of
  members are plain strings, matching the string-typed resource names
  Stage's own :inputs/:outputs already use."
  [:map
   [:resource :string]
   [:union-of [:vector :string]]])

(def ExternalStage
  "The external stage marker: a black-box stage the instantiating
  signature doesn't implement. Carries inputs/outputs but no laws --
  unlike Stage, there is no :kind, :status, or :laws key at all, only
  the trivial :external? true marker (asserted, not inferred)."
  [:map
   [:id :keyword]
   [:label :string]
   [:external? [:= true]]
   [:inputs [:vector :string]]
   [:outputs [:vector :string]]])

(defn pipeline-schema
  "The top-level signature-document schema, parameterized by `kinds`
  the same way stage-schema is."
  [kinds]
  [:map
   [:schema-version [:= 1]]
   [:stages [:vector (stage-schema kinds)]]
   [:resources {:optional true} [:vector UnionResource]]
   [:external-stages {:optional true} [:vector ExternalStage]]])

(defn valid-stage?
  [kinds stage]
  (m/validate (stage-schema kinds) stage))

(defn valid-union-resource?
  [resource]
  (m/validate UnionResource resource))

(defn valid-external-stage?
  [stage]
  (m/validate ExternalStage stage))

(defn valid?
  [kinds pipeline]
  (m/validate (pipeline-schema kinds) pipeline))

(defn read-signature-edn
  [path]
  (edn/read-string (slurp path)))
