(ns ehrt.sim.manifest
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
  (:require [malli.core :as m]
            [ehrt.sim.version :as version]))

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
  {:path :sha256}, :invocation. :version defaults to
  `ehrt.sim.version/version` -- the single version source `sim
  version`/--version also read, so a manifest and the binary that
  produced it cannot silently disagree (go-public session, Task 2);
  an explicit :version arg still wins, for a caller with its own
  reason to stamp something else. :sha256 defaults to
  `ehrt.sim.version/generator-sha256` -- pre-release, there is
  no release artifact to hash, so this is honestly a stand-in (SHA-256
  of the git HEAD commit id when readable, else the all-zero
  placeholder this field has always shown), never a silent zero
  presented as if it meant something -- see that function's own
  docstring for the full reasoning. Both fields are mandatory
  regardless (a caller may still pass its own :version/:sha256), so
  nothing downstream learns to tolerate their absence."
  [{:keys [seed engine-params config invocation version sha256]}]
  {:schema-version "1.1"
   :stage :simulated
   :generator {:name "ehrt.sim"
               :version (or version version/version)
               :sha256 (or sha256 (version/generator-sha256))}
   :seeds {:primary seed}
   :engine-params (or engine-params {})
   :config config
   :invocation invocation
   :canonicalizers-applied []
   :environment (environment)})

(defn valid? [manifest] (m/validate MirroredManifest manifest))
