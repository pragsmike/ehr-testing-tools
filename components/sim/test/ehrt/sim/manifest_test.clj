(ns ehrt.sim.manifest-test
  "Builder tests for the corpus-manifest bridge (sim split B, M1 step
  4, 2026-08-04, `.agents/plans/2026-08-04-sim-split-b-plan.md`
  AR-M1-2): the mirror tripwire test (`built-manifest-validates`,
  which asserted `manifest/valid?` against the now-retired
  `MirroredManifest`) retired in this same step -- see
  `ehrt.sim.manifest`'s own docstring for the retirement disclosure
  (quotes the M3-Task-0 lesson verbatim). Its builder-validity purpose
  is carried forward by `built-manifest-validates-against-provenance-
  test` below (landed Step 3, ahead of this retirement, so builder
  validity was never left uncovered). What remains besides that:
  build()'s own version/sha256-defaulting behavior, unrelated to the
  mirror either way."
  (:require [clojure.test :refer [deftest is]]
            [ehrt.sim.manifest :as manifest]
            [ehrt.sim.version :as version]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.provenance.interface :as provenance]))

(deftest built-manifest-validates-against-provenance-test
  (let [m (manifest/build {:seed 42
                           :engine-params {:patients 5}
                           :config {:path "config.edn"
                                    :sha256 (apply str (repeat 64 "a"))}
                           :invocation {:verb "run" :opts {:seed 42}}})]
    (is (provenance/valid-v1-1? m)
        "sim's own build() output must conform to provenance's real
         ManifestV1_1 -- no mirror in between")
    ;; ADR-0171 ruling D1 (arc 1, the RNG stream partition). Asserted
    ;; HERE rather than in a test of its own because the whole ruling is
    ;; that the marker rides ManifestV1_1's OPENNESS: the interesting
    ;; claim is not that the key exists but that adding it keeps the
    ;; manifest schema-valid with no shared-schema change, and this is
    ;; the test that already asserts schema validity.
    ;;
    ;; Version-independent, the same correction ADR-0151 applied to
    ;; `manifest-carries-the-live-event-schema-version` below: the pin is
    ;; the PROPERTY (a run stamps the scheme it was produced under), never
    ;; the literal "1.0", which a later scheme change would otherwise
    ;; teach a session to edit rather than think about.
    (is (string? engine/stream-scheme))
    (is (= engine/stream-scheme (:stream-scheme m))
        "the stream-scheme marker must be TOP-LEVEL, a sibling of
         :event-schema-version -- it describes the artifact, not the tool")
    (is (nil? (:stream-scheme (:seeds m)))
        "not inside :seeds: provenance/manifest.clj types that map
         [:map-of :keyword :int], so a keyword-or-string value there
         would not validate (ADR-0171 section 1f item 6)")
    (is (every? int? (vals (:seeds m)))
        "and :seeds stays int-valued, which is what forced D1 over D2")))

;; --- go-public Task 2: version single-sourced, manifest honest -----------

(deftest generator-version-defaults-to-the-single-version-source
  (let [m (manifest/build {:seed 1 :engine-params {} :config {:path "x" :sha256 (apply str (repeat 64 "a"))}
                          :invocation {}})]
    (is (= version/version (get-in m [:generator :version]))
        "retires the old hardcoded \"0.0.0-SNAPSHOT\" -- manifest and
         `sim version` must read the SAME source, never independently")))

(deftest generator-version-explicit-arg-still-wins
  (let [m (manifest/build {:seed 1 :engine-params {} :config {:path "x" :sha256 (apply str (repeat 64 "a"))}
                          :invocation {} :version "9.9.9-explicit"})]
    (is (= "9.9.9-explicit" (get-in m [:generator :version])))))

(deftest generator-sha256-defaults-to-a-schema-valid-hash-not-silent-zeros
  (let [m (manifest/build {:seed 1 :engine-params {} :config {:path "x" :sha256 (apply str (repeat 64 "a"))}
                          :invocation {}})]
    (is (= (version/generator-sha256) (get-in m [:generator :sha256]))
        "not hardcoded all-zeros anymore -- ehrt.sim.version's own
         honestly-documented placeholder-or-real-hash policy")))

;; --- ADR-0150 S-6 / ADR-0151 S-1: the contract version reaches the manifest -
;; `schema-version` is only a promise if a run STAMPS it, so this asserts the
;; manifest a run writes carries the LIVE contract version.
;;
;; RE-BASELINED 2026-08-18 (ADR-0151), semantically rather than by bumping a
;; literal from under it -- the same correction ADR-0150 applied to
;; `result_clock_test`'s own pin, and for the same reason. This asserted
;; `(= "1.1.0" ...)`, which pinned the property to the number the contract
;; HAPPENED to sit at on the day S-6 landed. That conflates the live property
;; -- a run stamps the version it was produced under -- with the accident of
;; the value, and it broke on the very next legitimate bump (1.1.0 -> 1.2.0,
;; census S-1), which is exactly what a literal pin teaches a session to edit
;; rather than think about. The version-independent property is asserted
;; instead, so no later bump has a literal here to re-baseline.

(deftest manifest-carries-the-live-event-schema-version
  (let [m (manifest/build {:seed 1 :engine-params {}
                           :config {:path "x" :sha256 (apply str (repeat 64 "a"))}
                           :invocation {}})]
    (is (string? engine/event-schema-version))
    (is (= engine/event-schema-version (:event-schema-version m))
        "a manifest whose stamp disagrees with the live contract dates a log
         to a contract it was not produced under -- which is the whole reason
         the key is in the manifest at all")))
