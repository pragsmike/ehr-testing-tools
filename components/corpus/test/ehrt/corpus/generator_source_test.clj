(ns ehrt.corpus.generator-source-test
  "Test-first (ruling 3, SS-2 Step 2): written before ehrt.corpus.
  generator-source existed. Hermetic throughout -- every
  registered test entry here is a fake (no real engine, no real
  subprocess); the real :synthea entry's own real-engine path is
  test-integration-tier, per corpus.generators-test's own hermetic
  execute-fn coverage. Covers the three distinct rejections ruling 3
  names by name: engine failure, empty output, and a pre-existing
  out-dir -- plus the two propagated-unchanged rejections from
  ehrt.corpus.generators/resolve-params (unknown kind,
  invalid params).

  Also carries generator-source's own tests (moved whole from
  ehrt.corpus-io.source-sink-test, corpus-io stage 2, 2026-07-31 --
  the constructor itself relocated here for the same reason) and
  parse-source-designator's own tests (moved whole from
  ehrt.corpus-io.source-sink-url-test, same stage/reason -- every
  case here calls parse-source-designator, not only the generator-
  kind ones, since the FUNCTION relocated wholesale, per AR-5 (tests
  follow their namespaces))."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus-io.interface :as corpus-io]
            [ehrt.corpus.generators :as generators]
            [ehrt.corpus.generate :as generate]
            [ehrt.corpus.generator-source :as generator-source])
  (:import [java.io File]))

;; ---- generator-source (SS-2 Step 4): validates+shapes only, never
;; executes -- the registry (ehrt.corpus.generators) owns
;; param resolution, this constructor just calls through and tags the
;; result with :kind. ----

(deftest generator-source-happy-path-test
  (let [r (generator-source/generator-source :synthea {:seed 7})]
    (is (kernel/ok? r))
    (is (= :synthea (:kind (:payload r))))
    (is (= 7 (:seed (:payload r))))))

(deftest generator-source-unknown-kind-test
  (let [r (generator-source/generator-source :not-a-registered-kind {})]
    (is (kernel/rejected? r))
    (is (= :unknown-generator-kind (:category r)))))

(deftest generator-source-invalid-params-test
  (let [r (generator-source/generator-source :synthea {:seed "not-an-int"})]
    (is (kernel/rejected? r))
    (is (= :invalid-generator-params (:category r)))))

(defn- temp-dir-path
  "A fresh, not-yet-existing temp directory path -- unlike generate_
  test.clj's own temp-dir (which mkdirs immediately), the pre-existing-
  out-dir test below needs a path this function has NOT yet created."
  []
  (let [f (File/createTempFile "generator-source-test" "")]
    (.delete f)
    (.getAbsolutePath f)))

(defn- register-fake!
  "Registers a fake generator entry under a fresh, test-local :kind
  (never :synthea -- this suite never touches the real registered
  entry) with the given :out-dir-fn/:execute-fn, defaulting to an
  empty params-schema/default-params."
  [kind {:keys [out-dir-fn execute-fn]}]
  (generators/register! {:kind kind
                          :default-params {}
                          :params-schema [:map]
                          :out-dir-fn out-dir-fn
                          :execute-fn execute-fn}))

(deftest resolve-happy-path-returns-a-dir-source-test
  (let [out-dir (temp-dir-path)]
    (register-fake! :fake-gen-happy
                     {:out-dir-fn (fn [_] out-dir)
                      :execute-fn (fn [_ dir]
                                    (.mkdirs (io/file dir))
                                    (spit (io/file dir "message.hl7") "MSH|...")
                                    (kernel/ok {:out-dir dir}))})
    (let [r (generator-source/resolve! :fake-gen-happy {})]
      (is (kernel/ok? r))
      (is (= {:kind :dir :path out-dir} (:payload r))))))

(deftest resolve-unknown-kind-propagates-test
  (let [r (generator-source/resolve! :no-such-generator-kind {})]
    (is (kernel/rejected? r))
    (is (= :unknown-generator-kind (:category r)))))

(deftest resolve-invalid-params-propagates-test
  (generators/register! {:kind :fake-gen-strict-params
                          :default-params {}
                          :params-schema [:map [:seed :int]]
                          :out-dir-fn (fn [_] (temp-dir-path))
                          :execute-fn (fn [_ dir] (kernel/ok {:out-dir dir}))})
  (let [r (generator-source/resolve! :fake-gen-strict-params {:seed "not-an-int"})]
    (is (kernel/rejected? r))
    (is (= :invalid-generator-params (:category r)))))

(deftest resolve-pre-existing-out-dir-is-rejected-before-executing-test
  (testing "a non-empty out-dir is rejected up front; execute-fn is never called"
    (let [out-dir (temp-dir-path)
          _ (.mkdirs (io/file out-dir))
          _ (spit (io/file out-dir "leftover.txt") "from a previous run")
          executed? (atom false)]
      (register-fake! :fake-gen-collision
                       {:out-dir-fn (fn [_] out-dir)
                        :execute-fn (fn [_ dir]
                                      (reset! executed? true)
                                      (kernel/ok {:out-dir dir}))})
      (let [r (generator-source/resolve! :fake-gen-collision {})]
        (is (kernel/error? r))
        (is (= :out-dir-exists (:category r)))
        (is (false? @executed?))))))

(deftest resolve-engine-failure-propagates-unchanged-test
  (register-fake! :fake-gen-engine-failure
                   {:out-dir-fn (fn [_] (temp-dir-path))
                    :execute-fn (fn [_ _] (kernel/error :some-engine-failure {:detail "boom"}))})
  (let [r (generator-source/resolve! :fake-gen-engine-failure {})]
    (is (kernel/error? r))
    (is (= :some-engine-failure (:category r)))
    (is (= "boom" (:detail (:payload r))))))

(deftest resolve-empty-output-is-its-own-rejection-test
  (testing "execute-fn returns ok but writes nothing -- caught, not silently accepted"
    (let [out-dir (temp-dir-path)]
      (register-fake! :fake-gen-empty-output
                       {:out-dir-fn (fn [_] out-dir)
                        :execute-fn (fn [_ dir]
                                      (.mkdirs (io/file dir))
                                      (kernel/ok {:out-dir dir}))})
      (let [r (generator-source/resolve! :fake-gen-empty-output {})]
        (is (kernel/error? r))
        (is (= :generator-produced-no-output (:category r)))))))

;; ---- parse-source-designator (moved whole from
;; ehrt.corpus-io.source-sink-url-test, corpus-io stage 2, 2026-07-31)
;; ----

;; generators: safe path segments only -- '?'/'&'/'=' would be
;; ambiguous with the query grammar, and this scheme doesn't percent-
;; encode :path the way it does query values (matching the design's
;; own unencoded example, "dir:./corpus")

(def ^:private safe-path-gen
  (gen/fmap (fn [segs] (str "./" (str/join "/" segs)))
            (gen/vector (gen/fmap #(apply str %) (gen/vector gen/char-alpha 1 8)) 1 4)))

(def ^:private format-gen (gen/elements [:fhir-json :v2-er7 :inferred]))
(def ^:private framing-gen (gen/elements [:file-per-item :er7-multi :ndjson :bundle-entries :mllp]))

(defn- source-map-gen
  [kind]
  (gen/let [path safe-path-gen
            format (gen/one-of [(gen/return nil) format-gen])
            framing (gen/one-of [(gen/return nil) framing-gen])]
    (cond-> {:kind kind :path path}
      format (assoc :format format)
      framing (assoc :framing framing))))

(deftest source-designator-round-trip-property-test
  (testing "parse ∘ print = identity on canonical dir/file Source maps (D4 law) --
            print-source-designator stayed in corpus-io (no domain edge), parse-
            source-designator is this namespace's own"
    (doseq [kind [:dir :file]]
      (let [check-result
            (tc/quick-check 100
              (prop/for-all [m (source-map-gen kind)]
                (let [printed (corpus-io/print-source-designator m)]
                  (and (kernel/ok? printed)
                       (let [parsed (generator-source/parse-source-designator (:payload printed))]
                         (and (kernel/ok? parsed)
                              (= m (:payload parsed))))))))]
        (is (:pass? check-result) (str kind " source round-trip failed: " (:shrunk check-result)))))))

(deftest concrete-round-trip-example-test
  (let [m {:kind :dir :path "./corpus" :format :v2-er7 :framing :er7-multi}
        printed (corpus-io/print-source-designator m)]
    (is (kernel/ok? printed))
    (is (= "dir:./corpus?format=v2-er7&framing=er7-multi" (:payload printed)))
    (let [parsed (generator-source/parse-source-designator (:payload printed))]
      (is (kernel/ok? parsed))
      (is (= m (:payload parsed))))))

(deftest design-doc-example-urls-parse-test
  (testing "dir: with format+framing query params"
    (let [r (generator-source/parse-source-designator "dir:./corpus?format=v2-er7&framing=er7-multi")]
      (is (kernel/ok? r))
      (is (= {:kind :dir :path "./corpus" :format :v2-er7 :framing :er7-multi} (:payload r)))))
  (testing "sim: is recognized (D-a) and now supported (SS-2 Step 4) -- ?seed=42 coerces to an int"
    (let [r (generator-source/parse-source-designator "sim:?seed=42")]
      (is (kernel/ok? r))
      (is (= :sim (:kind (:payload r))))
      (is (= 42 (:seed (:payload r))))
      (is (= 1 (:patients (:payload r))) "sim's own pinned default (D8), not re-derived here")))
  (testing "blaze:// is recognized but not-yet-supported"
    (let [r (generator-source/parse-source-designator "blaze://host:8080/fhir?query=Patient%3F_count%3D100")]
      (is (kernel/rejected? r))
      (is (= :unsupported-source-kind (:category r)))
      (is (= :blaze (:kind (:payload r))))))
  (testing "stdin: is recognized and now supported (SS-3 Step 6) -- format/framing thread through"
    (let [r (generator-source/parse-source-designator "stdin:?framing=mllp&format=v2-er7")]
      (is (kernel/ok? r))
      (is (= {:kind :stdin :format :v2-er7 :framing :mllp} (:payload r)))))
  (testing "a bare stdin: (no query) is valid -- file-per-item over whatever arrives"
    (let [r (generator-source/parse-source-designator "stdin:")]
      (is (kernel/ok? r))
      (is (= {:kind :stdin} (:payload r)))))
  (testing "synthea: is recognized and now supported (SS-2 Step 4) -- zero-param means exactly zero-flag `ehr corpus generate`"
    (let [r (generator-source/parse-source-designator "synthea:")]
      (is (kernel/ok? r))
      (is (= :synthea (:kind (:payload r))))
      (is (= generate/default-seed (:seed (:payload r))))
      (is (= generate/default-population (:population (:payload r)))))))

(deftest generator-source-non-numeric-seed-is-invalid-params-not-a-thrown-exception-test
  (testing "\"abc\" doesn't coerce to an int -- left as a string, so the registry's own
            params-schema rejects it (ADR-0004: a bad external value is a rejection, never a throw)"
    (let [r (generator-source/parse-source-designator "sim:?seed=abc")]
      (is (kernel/rejected? r))
      (is (= :invalid-generator-params (:category r))))))

(deftest generator-source-explicit-params-override-defaults-test
  (let [r (generator-source/parse-source-designator "synthea:?seed=7&population=3")]
    (is (kernel/ok? r))
    (is (= 7 (:seed (:payload r))))
    (is (= 3 (:population (:payload r))))
    (is (= generate/default-reference-date (:reference-date (:payload r))))))

(deftest unknown-scheme-test
  (let [r (generator-source/parse-source-designator "ftp:./corpus")]
    (is (kernel/rejected? r))
    (is (= :unknown-source-scheme (:category r)))))

(deftest whitespace-is-malformed-test
  (let [r (generator-source/parse-source-designator "dir: ./corpus")]
    (is (kernel/rejected? r))
    (is (= :malformed-source-designator (:category r)))))

(deftest missing-required-field-test
  (testing "dir: with no path at all propagates dir-source's own :invalid-source rejection"
    (let [r (generator-source/parse-source-designator "dir:")]
      (is (kernel/rejected? r))
      (is (= :invalid-source (:category r))))))

(deftest no-scheme-at-all-test
  (let [r (generator-source/parse-source-designator "just-a-bare-path")]
    (is (kernel/rejected? r))
    (is (= :malformed-source-designator (:category r)))))
