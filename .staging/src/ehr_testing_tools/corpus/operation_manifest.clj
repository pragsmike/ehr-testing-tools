(ns ehr-testing-tools.corpus.operation-manifest
  "The operation manifest (D-d resolved via option A1, ADR-0020,
  docs/source-sink-design.md Part III.5): a distinct, independently-
  versioned schema for a dir/file sink's own transformation lineage --
  written as operation-manifest.edn, never manifest.edn, never sharing
  a field name with ehr-testing-tools.corpus.manifest/ManifestV1_1. A
  generator manifest states engine provenance (which artifact, which
  config, which subprocess); this manifest states transformation
  lineage (these input hashes, this operator at this version, at this
  locator, these output hashes) -- different speech acts, deliberately
  never reconciled into one schema.

  :producer carries this repo's own honest identity (the `ehr version`
  machinery -- ehr-testing-tools.cli/repo-identity,
  ehr-testing-tools.cli/real-git-describe) with no :sha256 field: an
  absent field is honest, a fabricated one is not. :items[].input-hash
  is per-item optional, present iff the producer actually held it --
  ehr-testing-tools.corpus.mutate always does (its own lineage record's
  :parent); a hypothetical future plain write might not."
  (:require [malli.core :as m]))

(def OperationManifestV1
  [:map
   [:manifest-kind [:= :operation]]
   [:schema-version [:= 1]]
   [:producer [:map
               [:name :string]
               [:identity :string]
               [:git :string]]]
   [:operation [:map
                [:kind :keyword]
                [:operator-id :keyword]
                [:operator-version :string]
                [:locator :map]]]
   [:written-at :string]
   [:format :keyword]
   [:framing :keyword]
   [:items [:vector [:map
                      [:name :string]
                      [:sha256 [:re #"^[0-9a-f]{64}$"]]
                      [:input-hash {:optional true} [:re #"^[0-9a-f]{64}$"]]]]]])

(defn valid?
  [manifest]
  (m/validate OperationManifestV1 manifest))

(defn build
  "Builds an operation manifest from :producer, :operation, :written-at,
  :format, :framing, and :items (each already shaped {:name :sha256
  :input-hash} -- this function computes nothing from raw bytes; that is
  the caller's job, since only the caller knows which content produced
  which item)."
  [{:keys [producer operation written-at format framing items]}]
  {:manifest-kind :operation
   :schema-version 1
   :producer producer
   :operation operation
   :written-at written-at
   :format format
   :framing framing
   :items (vec items)})
