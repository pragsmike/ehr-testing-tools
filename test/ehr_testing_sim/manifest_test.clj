(ns ehr-testing-sim.manifest-test
  "Tripwire for the corpus-manifest bridge: what `build` produces
  validates against the mirrored schema. The BINDING contract test --
  validating against ehr-testing-tools' actual ManifestV1_1 -- lives in
  that repo's test-integration tree, where both codebases share a
  classpath (see ehr-testing-sim.manifest's docstring)."
  (:require [clojure.test :refer [deftest is]]
            [ehr-testing-sim.manifest :as manifest]))

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
    (is (= "ehr-testing-sim" (get-in m [:generator :name])))))
