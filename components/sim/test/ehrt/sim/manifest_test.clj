(ns ehrt.sim.manifest-test
  "Tripwire for the corpus-manifest bridge: what `build` produces
  validates against the mirrored schema. The BINDING contract test --
  validating against ehr-testing-tools' actual ManifestV1_1 -- lives in
  that repo's test-integration tree, where both codebases share a
  classpath (see ehrt.sim.manifest's docstring)."
  (:require [clojure.test :refer [deftest is]]
            [ehrt.sim.manifest :as manifest]
            [ehrt.sim.version :as version]
            [ehrt.provenance.interface :as provenance]))

(deftest built-manifest-validates
  (let [m (manifest/build {:seed 42
                           :engine-params {:patients 5}
                           :config {:path "config.edn"
                                    :sha256 (apply str (repeat 64 "a"))}
                           :invocation {:verb "run" :opts {:seed 42}}})]
    (is (manifest/valid? m))
    (is (= "1.1" (:schema-version m))
        "mirrors tools' ManifestV1_1 :schema-version exactly -- a mirror
         that omits this key can't self-detect the drift; the binding
         check lives in tools' own sim-manifest-contract-test")
    (is (= :simulated (:stage m)))
    (is (= {:primary 42} (:seeds m)))
    (is (= "ehrt.sim" (get-in m [:generator :name])))))

;; --- sim split B, M1 step 3 (2026-08-04, plan AR-5(b) / AR-M1-3) ---------
;; The fast-lane companion to the conformance project's own
;; sim-manifest-contract-test: same builder-validity claim (what
;; `build` produces conforms to the real ManifestV1_1), checked
;; directly against ehrt.provenance.interface without the harness
;; (no sim-harness/run!, no subprocess/in-process boundary). This is
;; the replacement AR-5(b) names for the mirror tripwire's own
;; builder-validity purpose -- `built-manifest-validates` above still
;; exercises the mirror for now (Step 4 retires MirroredManifest and
;; that test together).
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
