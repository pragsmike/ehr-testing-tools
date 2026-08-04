(ns ehrt.provenance.interface
  "The provenance-manifest schema family (sim split B, M1, 2026-08-04,
  `.agents/plans/2026-08-04-sim-split-b-plan.md` AR-2): ManifestV0/V1/
  V1_1 and their validators, moved verbatim out of
  `ehrt.corpus.manifest` -- corpus -> sim already exists
  (`ehrt.corpus.sim-adapter` requires the sim façade, ADR-0012), so
  sim -> corpus for this schema would be a cycle; `provenance`,
  depended on by both and depending on neither, is the only acyclic
  single home. Named `provenance` rather than bare `manifest` to avoid
  three-things-called-manifest ambiguity during the migration, and to
  leave room for the schema family to grow (e.g. corpus-io's own
  operation manifest -- noted, not proposed, by that plan).

  Builders stay producer-side, deliberately: corpus keeps
  `build`/`build-v1-1` (`ehrt.corpus.manifest`), sim keeps `build`
  (`ehrt.sim.manifest`) -- each validates its own output against the
  schemas here, but this component knows nothing about either
  producer."
  (:require [ehrt.provenance.manifest :as manifest]))

;; ---- schema v0 (EXP-A4's working hypothesis; frozen historical record) ----
(def ManifestV0 manifest/ManifestV0)
(def valid? manifest/valid?)

;; ---- schema v1 (EXP-A4's correction: adds :reference-date; frozen) ----
(def ManifestV1 manifest/ManifestV1)
(def valid-v1? manifest/valid-v1?)

;; ---- schema v1.1 (P4's upgrade: :stage, :seeds, :engine-params, :runtime) ----
(def ManifestV1_1 manifest/ManifestV1_1)
(def valid-v1-1? manifest/valid-v1-1?)
