(ns ehr-testing-tools.corpus.manifest
  "Provenance records for generated corpora (ADR-0004). Schema v0 was
  EXP-A4's working hypothesis for the complete pinned-input set --
  generator artifact reference, seed, config identity, the invocation
  record, and the canonicalizers applied. EXP-A4's execution found one
  gap (:reference-date was only recoverable indirectly, embedded in the
  invocation's args, never a clean top-level field) and confirmed the
  rest; v1 is the corrected schema and the one corpus.generate now
  produces. v0 is kept, frozen, as the historical record of the
  hypothesis -- not deleted, not silently reinterpreted."
  (:require [malli.core :as m]))

(def ManifestV0
  [:map
   [:schema-version [:= 0]]
   [:generator [:map
                [:name :string]
                [:version :string]
                [:sha256 [:re #"^[0-9a-f]{64}$"]]]]
   [:seed :int]
   [:clinician-seed :int]
   [:config [:map
             [:path :string]
             [:sha256 [:re #"^[0-9a-f]{64}$"]]]]
   [:invocation :map]
   [:canonicalizers-applied [:vector [:tuple :keyword :string]]]
   [:environment [:map
                  [:locale :string]
                  [:timezone :string]
                  [:jvm-version :string]]]])

(defn valid?
  [manifest]
  (m/validate ManifestV0 manifest))

(defn build
  "Builds a schema-v0 manifest from the given fields.
  :canonicalizers-applied defaults to [] when omitted."
  [{:keys [generator seed clinician-seed config invocation canonicalizers-applied environment]}]
  {:schema-version 0
   :generator generator
   :seed seed
   :clinician-seed clinician-seed
   :config config
   :invocation invocation
   :canonicalizers-applied (or canonicalizers-applied [])
   :environment environment})

(def ManifestV1
  "EXP-A4's corrected schema: ManifestV0 plus :reference-date as an
  explicit, top-level pinned input."
  [:map
   [:schema-version [:= 1]]
   [:generator [:map
                [:name :string]
                [:version :string]
                [:sha256 [:re #"^[0-9a-f]{64}$"]]]]
   [:seed :int]
   [:clinician-seed :int]
   [:reference-date :string]
   [:config [:map
             [:path :string]
             [:sha256 [:re #"^[0-9a-f]{64}$"]]]]
   [:invocation :map]
   [:canonicalizers-applied [:vector [:tuple :keyword :string]]]
   [:environment [:map
                  [:locale :string]
                  [:timezone :string]
                  [:jvm-version :string]]]])

(defn valid-v1?
  [manifest]
  (m/validate ManifestV1 manifest))

(defn build-v1
  "Builds a schema-v1 manifest from the given fields.
  :canonicalizers-applied defaults to [] when omitted."
  [{:keys [generator seed clinician-seed reference-date config invocation
           canonicalizers-applied environment]}]
  {:schema-version 1
   :generator generator
   :seed seed
   :clinician-seed clinician-seed
   :reference-date reference-date
   :config config
   :invocation invocation
   :canonicalizers-applied (or canonicalizers-applied [])
   :environment environment})
