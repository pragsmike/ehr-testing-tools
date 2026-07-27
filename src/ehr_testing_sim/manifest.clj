(ns ehr-testing-sim.manifest
  "The corpus-manifest bridge: a `sim run` emits a provenance manifest
  shaped for ehr-testing-tools' corpus conventions (its
  corpus/manifest.clj ManifestV1_1: :stage, :generator, :seeds,
  :engine-params, :config, :invocation, :canonicalizers-applied,
  :environment), so `ehr corpus intake` can ingest a sim run like any
  other pinned-input corpus.

  The authoritative schema lives in ehr-testing-tools; this namespace
  mirrors the *shape* without depending on it (dependency arrow points
  tools -> sim only, ADR-0001). The mirrored schema here is a tripwire,
  not the contract: the binding contract test belongs in
  ehr-testing-tools' test-integration tree, where both codebases are on
  the classpath and drift becomes a failing test rather than a latent
  incompatibility."
  (:require [malli.core :as m]))

(def MirroredManifest
  "Structural mirror of tools' ManifestV1_1 (verified against its
  source 2026-07-26; re-verify on tools schema changes).

  LESSON (M3 Task 0): this mirror once omitted :schema-version entirely
  -- both here and in `build` -- and its own tripwire test
  (manifest-test) stayed green throughout, because a mirror validates
  its OWN output against its OWN copy of the schema; it agreed with
  itself perfectly while both disagreed with the authoritative source.
  A mirror cannot catch itself agreeing with its own mistake. That is
  exactly why the BINDING contract test lives host-side, in tools'
  test-integration tree (sim-manifest-contract-test), where the real
  ManifestV1_1 is on the classpath to validate against -- not here."
  [:map
   [:schema-version [:= "1.1"]]
   [:stage :keyword]
   [:generator [:map
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

(defn environment
  []
  {:locale (str (java.util.Locale/getDefault))
   :timezone (str (java.time.ZoneId/systemDefault))
   :jvm-version (System/getProperty "java.version")})

(defn build
  "Builds a tools-ingestible manifest for a sim run.
  Required: :seed, :engine-params (the run config), :config
  {:path :sha256}, :invocation. :version should be this library's
  release version; :sha256 the release artifact digest (placeholders
  acceptable pre-release, but the fields are mandatory so nothing
  downstream learns to tolerate their absence)."
  [{:keys [seed engine-params config invocation version sha256]}]
  {:schema-version "1.1"
   :stage :simulated
   :generator {:name "ehr-testing-sim"
               :version (or version "0.0.0-SNAPSHOT")
               :sha256 (or sha256 (apply str (repeat 64 "0")))}
   :seeds {:primary seed}
   :engine-params (or engine-params {})
   :config config
   :invocation invocation
   :canonicalizers-applied []
   :environment (environment)})

(defn valid? [manifest] (m/validate MirroredManifest manifest))
