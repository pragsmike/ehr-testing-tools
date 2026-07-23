(ns ehr-testing-tools.corpus.manifest
  "Provenance records for generated corpora (ADR-0004). Schema v0 is EXP-A4's
  working hypothesis for the complete pinned-input set -- generator
  artifact reference, seed, config identity, the invocation record, and
  the canonicalizers applied. EXP-A4's findings are expected to upgrade
  this to v1 with whatever additional fields turn out to be load-bearing
  for byte-identical regeneration."
  (:require [malli.core :as m]))

(def ManifestV0
  [:map
   [:schema-version [:= 0]]
   [:generator [:map
                [:name :string]
                [:version :string]
                [:sha256 [:re #"^[0-9a-f]{64}$"]]]]
   [:seed :int]
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
  [{:keys [generator seed config invocation canonicalizers-applied environment]}]
  {:schema-version 0
   :generator generator
   :seed seed
   :config config
   :invocation invocation
   :canonicalizers-applied (or canonicalizers-applied [])
   :environment environment})
