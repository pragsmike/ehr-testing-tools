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
            [ehrt.provenance.interface :as provenance]))

(deftest built-manifest-validates-against-provenance-test
  (let [m (manifest/build {:seed 42
                           :engine-params {:patients 5}
                           :config {:path "config.edn"
                                    :sha256 (apply str (repeat 64 "a"))}
                           :invocation {:verb "run" :opts {:seed 42}}})]
    (is (provenance/valid-v1-1? m)
        "sim's own build() output must conform to provenance's real
         ManifestV1_1 -- no mirror in between")))

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
