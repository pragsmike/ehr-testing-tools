(ns ehrt.sim.manifest
  "The corpus-manifest bridge: a `sim run` emits a provenance manifest
  shaped for ehr-testing-tools' corpus conventions (ManifestV1_1:
  :stage, :generator, :seeds, :engine-params, :config, :invocation,
  :canonicalizers-applied, :environment), so `ehr corpus intake` can
  ingest a sim run like any other pinned-input corpus.

  MIRROR RETIRED (sim split B, M1 step 4, 2026-08-04,
  `.agents/plans/2026-08-04-sim-split-b-plan.md` AR-M1-2): this
  namespace used to carry `MirroredManifest`, a structural copy of the
  authoritative schema, as a drift tripwire -- dependency-direction
  doctrine from the separate-repo era (tools -> sim only, sim/ADR-0001),
  now a fossil: both live in one workspace, and corpus -> sim already
  exists (`ehrt.corpus.sim-adapter` requires the sim façade,
  ADR-0012), so the acyclic single home for the real schema is
  `ehrt.provenance.interface`, depended on directly here now instead
  of copied.

  LESSON (M3 Task 0, quoted verbatim from the retired mirror's own
  docstring, the citation this retirement's disclosure rests on): 'this
  mirror once omitted :schema-version entirely -- both here and in
  `build` -- and its own tripwire test (manifest-test) stayed green
  throughout, because a mirror validates its OWN output against its
  OWN copy of the schema; it agreed with itself perfectly while both
  disagreed with the authoritative source. A mirror cannot catch
  itself agreeing with its own mistake.' Drift is impossible by
  construction now that `build`'s own validity is checked directly
  against provenance's real ManifestV1_1 (no copy in between) --
  `ehrt.sim.manifest-test/built-manifest-validates` (the mirror's own
  tripwire test) retires with it; its builder-validity purpose moved
  to `built-manifest-validates-against-provenance-test`, landed ahead
  of this retirement in Step 3. `valid?` retires too, undefined here
  now -- fresh grep at retirement time found no real caller outside
  its own now-retired test (`ehrt.provenance.interface/valid-v1-1?` is
  the real predicate; `build`'s own callers never validated its
  output, they just used it)."
  (:require [ehrt.sim.version :as version]
            [ehrt.sim-engine.interface :as engine]))

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
   ;; Author ruling Q-A (a), 2026-08-16 (event-log contract arc): the
   ;; ground-truth event log is a PUBLIC, VERSIONED contract, so every
   ;; run records which version of it produced this log. A consumer
   ;; holding an events.edn and its manifest can therefore tell whether
   ;; the contract it built against still applies -- without that, a
   ;; schema change and a schema break look identical from the outside,
   ;; which is the whole problem the arc exists to fix.
   ;;
   ;; Distinct from :schema-version above, which versions the MANIFEST.
   ;; Top-level rather than tucked inside :generator because it
   ;; describes the artifact, not the tool. ManifestV1_1 is an open map,
   ;; so this is additive at the provenance seam: no shared schema
   ;; changes, and no non-sim corpus grows a key that means nothing to
   ;; it.
   :event-schema-version engine/event-schema-version
   ;; ADR-0171 ruling D1 (arc 1, the RNG stream partition): which RNG
   ;; stream scheme produced this corpus. Top-level, a string, and a
   ;; sibling of :event-schema-version above for the identical reason --
   ;; it describes the ARTIFACT, not the tool -- and additive at this
   ;; seam because ManifestV1_1 is an open map, so no shared schema
   ;; changes and no non-sim corpus grows a key that means nothing to it.
   ;;
   ;; It could NOT go inside :seeds: provenance/manifest.clj declares
   ;; that map `[:map-of :keyword :int]`, int values only.
   ;;
   ;; A DISCRIMINATOR, not a warranty -- see engine/stream-scheme's own
   ;; docstring, and sim/ADR-0009 decision 1 for the within-version seed
   ;; stability policy this marker rides without changing.
   :stream-scheme engine/stream-scheme
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
