(ns ehrt.corpus.manifest
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

(def ManifestV1_1
  "P4's manifest upgrade: v1's engine-shaped fields (:seed,
  :clinician-seed, :reference-date) leave the fixed top level --
  :seed/:clinician-seed become the :seeds map (naming: :master, not
  :seed, since a stage other than :generate may one day consume a
  different seed shape), :reference-date moves into the free-form
  :engine-params map alongside future engine-specific parameters that
  don't deserve their own permanent schema field. New top-level
  fields: :stage (which pipeline stage produced this manifest -- P4
  adds mutation, so a manifest is no longer implicitly a generation
  record) and :runtime (the JVM artifact that ran the engine,
  {name, version, sha256} like :generator; optional -- absent when
  :java-bin was overridden explicitly, bypassing registry resolution,
  since fabricating a fake artifact record would be a lie).

  :schema-version is the string \"1.1\", not the integer 2 -- this is
  a deliberate choice (report: schema-version-as-string over
  bump-to-2), because the change is additive/restructuring within the
  v1 lineage, not a breaking rewrite that deserves a new integer
  epoch; \"1.1\" says exactly that. ManifestV0 and ManifestV1 are kept,
  frozen, as valid historical records -- this is schema versioning,
  not migration; nothing here regenerates or reinterprets a v0/v1
  manifest as v1.1."
  [:map
   [:schema-version [:= "1.1"]]
   [:stage :keyword]
   [:generator [:map
                [:name :string]
                [:version :string]
                [:sha256 [:re #"^[0-9a-f]{64}$"]]]]
   [:runtime {:optional true} [:map
                                [:name :string]
                                [:version :string]
                                [:sha256 [:re #"^[0-9a-f]{64}$"]]]]
   [:seeds [:map-of :keyword :int]]
   [:engine-params [:map-of :keyword :any]]
   [:config [:map
             [:path :string]
             [:sha256 [:re #"^[0-9a-f]{64}$"]]]]
   [:invocation :map]
   [:canonicalizers-applied [:vector [:tuple :keyword :string]]]
   [:environment [:map
                  [:locale :string]
                  [:timezone :string]
                  [:jvm-version :string]]]])

(defn valid-v1-1?
  [manifest]
  (m/validate ManifestV1_1 manifest))

(defn build-v1-1
  "Builds a schema-v1.1 manifest from the given fields.
  :canonicalizers-applied defaults to []; :runtime is omitted entirely
  (not nil-valued) when absent from fields, since the schema treats it
  as optional rather than nullable."
  [{:keys [stage generator runtime seeds engine-params config invocation
           canonicalizers-applied environment]}]
  (cond-> {:schema-version "1.1"
           :stage stage
           :generator generator
           :seeds seeds
           :engine-params engine-params
           :config config
           :invocation invocation
           :canonicalizers-applied (or canonicalizers-applied [])
           :environment environment}
    (some? runtime) (assoc :runtime runtime)))
