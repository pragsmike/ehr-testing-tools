(ns ehrt.corpus.manifest
  "Provenance-record builders for generated corpora (ADR-0004). The
  schemas themselves (ManifestV0/V1/V1_1) and their validators moved
  to `ehrt.provenance.interface` (sim split B, M1, 2026-08-04,
  `.agents/plans/2026-08-04-sim-split-b-plan.md` AR-2 / AR-M1-1) --
  corpus -> sim already exists (`ehrt.corpus.sim-adapter` requires the
  sim façade, ADR-0012), so sim -> corpus for the schema would be a
  cycle; provenance, depended on by both and depending on neither, is
  the only acyclic single home. `ManifestV0`/`valid?`,
  `ManifestV1`/`valid-v1?`, and `ManifestV1_1`/`valid-v1-1?` below are
  relays (the same vars provenance defines), not copies -- kept here
  so every existing caller of this namespace (`generate.clj`,
  `intake.clj`, their own tests) needs no repoint at all. The builders
  (`build`/`build-v1`/`build-v1-1`) stay producer-side, unmoved: this
  is corpus.generate's own schema-v1 upgrade history (v0 was EXP-A4's
  working hypothesis for the complete pinned-input set -- generator
  artifact reference, seed, config identity, the invocation record,
  and the canonicalizers applied; EXP-A4's execution found one gap,
  :reference-date only recoverable indirectly via the invocation's
  args, never a clean top-level field -- v1 is the corrected schema
  and the one corpus.generate now produces)."
  (:require [ehrt.provenance.interface :as provenance]))

(def ManifestV0 provenance/ManifestV0)

(def valid? provenance/valid?)

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

(def ManifestV1 provenance/ManifestV1)

(def valid-v1? provenance/valid-v1?)

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

(def ManifestV1_1 provenance/ManifestV1_1)

(def valid-v1-1? provenance/valid-v1-1?)

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
