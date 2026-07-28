(ns ehrt.tools.corpus.source-sink-url-test
  "Test-first (ruling 4, SS-1 Step 3): written before ehrt.tools.
  corpus.source-sink-url existed. Covers the URL<->map parser: the
  round-trip law (D4 -- parse ∘ print = identity on canonical maps),
  explicit examples from docs/source-sink-design.md Part IV, and the
  negative cases ruling 4 names by name (unknown scheme, whitespace,
  missing required kind-specific fields) plus the six-schemes-fixed/
  two-implemented split (ruling 3, D-a)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.string :as str]
            [ehrt.tools.result :as result]
            [ehrt.tools.corpus.generate :as generate]
            [ehrt.tools.corpus.source-sink-url :as url]))

;; ---- generators: safe path segments only -- '?'/'&'/'=' would be
;; ambiguous with the query grammar, and this scheme doesn't percent-
;; encode :path the way it does query values (matching the design's
;; own unencoded example, "dir:./corpus") ----

(def safe-path-gen
  (gen/fmap (fn [segs] (str "./" (str/join "/" segs)))
            (gen/vector (gen/fmap #(apply str %) (gen/vector gen/char-alpha 1 8)) 1 4)))

(def format-gen (gen/elements [:fhir-json :v2-er7 :inferred]))
(def framing-gen (gen/elements [:file-per-item :er7-multi :ndjson :bundle-entries :mllp]))

(defn- source-map-gen
  [kind]
  (gen/let [path safe-path-gen
            format (gen/one-of [(gen/return nil) format-gen])
            framing (gen/one-of [(gen/return nil) framing-gen])]
    (cond-> {:kind kind :path path}
      format (assoc :format format)
      framing (assoc :framing framing))))

(defn- sink-map-gen
  [kind]
  (gen/let [path safe-path-gen
            format format-gen
            framing (gen/one-of [(gen/return nil) framing-gen])]
    (cond-> {:kind kind :path path :format format}
      framing (assoc :framing framing))))

(deftest source-designator-round-trip-property-test
  (testing "parse ∘ print = identity on canonical dir/file Source maps (D4 law)"
    (doseq [kind [:dir :file]]
      (let [check-result
            (tc/quick-check 100
              (prop/for-all [m (source-map-gen kind)]
                (let [printed (url/print-source-designator m)]
                  (and (result/ok? printed)
                       (let [parsed (url/parse-source-designator (:payload printed))]
                         (and (result/ok? parsed)
                              (= m (:payload parsed))))))))]
        (is (:pass? check-result) (str kind " source round-trip failed: " (:shrunk check-result)))))))

(deftest sink-designator-round-trip-property-test
  (testing "parse ∘ print = identity on canonical dir/file Sink maps (D4 law, sink twin)"
    (doseq [kind [:dir :file]]
      (let [check-result
            (tc/quick-check 100
              (prop/for-all [m (sink-map-gen kind)]
                (let [printed (url/print-sink-designator m)]
                  (and (result/ok? printed)
                       (let [parsed (url/parse-sink-designator (:payload printed))]
                         (and (result/ok? parsed)
                              (= m (:payload parsed))))))))]
        (is (:pass? check-result) (str kind " sink round-trip failed: " (:shrunk check-result)))))))

(defn- stdout-sink-map-gen
  "Like sink-map-gen, but no :path -- stdout: names no filesystem
  location (SS-4 Step 3, sink-side twin of stdin's own no-:path shape)."
  []
  (gen/let [format format-gen
            framing (gen/one-of [(gen/return nil) framing-gen])]
    (cond-> {:kind :stdout :format format}
      framing (assoc :framing framing))))

(deftest stdout-sink-designator-round-trip-property-test
  (testing "parse ∘ print = identity on canonical :stdout Sink maps (D4 law)"
    (let [check-result
          (tc/quick-check 100
            (prop/for-all [m (stdout-sink-map-gen)]
              (let [printed (url/print-sink-designator m)]
                (and (result/ok? printed)
                     (let [parsed (url/parse-sink-designator (:payload printed))]
                       (and (result/ok? parsed)
                            (= m (:payload parsed))))))))]
      (is (:pass? check-result) (str "stdout sink round-trip failed: " (:shrunk check-result))))))

;; ---- harness sanity: a concrete example, so the property tests above
;; aren't the only evidence the round-trip machinery does something
;; (same discipline as canonical_test/mutate_test's own harness-catches-
;; a-violation tests) ----

(deftest concrete-round-trip-example-test
  (let [m {:kind :dir :path "./corpus" :format :v2-er7 :framing :er7-multi}
        printed (url/print-source-designator m)]
    (is (result/ok? printed))
    (is (= "dir:./corpus?format=v2-er7&framing=er7-multi" (:payload printed)))
    (let [parsed (url/parse-source-designator (:payload printed))]
      (is (result/ok? parsed))
      (is (= m (:payload parsed))))))

;; ---- explicit examples from docs/source-sink-design.md Part IV ----

(deftest design-doc-example-urls-parse-test
  (testing "dir: with format+framing query params"
    (let [r (url/parse-source-designator "dir:./corpus?format=v2-er7&framing=er7-multi")]
      (is (result/ok? r))
      (is (= {:kind :dir :path "./corpus" :format :v2-er7 :framing :er7-multi} (:payload r)))))
  (testing "sim: is recognized (D-a) and now supported (SS-2 Step 4) -- ?seed=42 coerces to an int"
    (let [r (url/parse-source-designator "sim:?seed=42")]
      (is (result/ok? r))
      (is (= :sim (:kind (:payload r))))
      (is (= 42 (:seed (:payload r))))
      (is (= 1 (:patients (:payload r))) "sim's own pinned default (D8), not re-derived here")))
  (testing "blaze:// is recognized but not-yet-supported"
    (let [r (url/parse-source-designator "blaze://host:8080/fhir?query=Patient%3F_count%3D100")]
      (is (result/rejected? r))
      (is (= :unsupported-source-kind (:category r)))
      (is (= :blaze (:kind (:payload r))))))
  (testing "stdin: is recognized and now supported (SS-3 Step 6) -- format/framing thread through"
    (let [r (url/parse-source-designator "stdin:?framing=mllp&format=v2-er7")]
      (is (result/ok? r))
      (is (= {:kind :stdin :format :v2-er7 :framing :mllp} (:payload r)))))
  (testing "a bare stdin: (no query) is valid -- file-per-item over whatever arrives"
    (let [r (url/parse-source-designator "stdin:")]
      (is (result/ok? r))
      (is (= {:kind :stdin} (:payload r)))))
  (testing "synthea: is recognized and now supported (SS-2 Step 4) -- zero-param means exactly zero-flag `ehr corpus generate`"
    (let [r (url/parse-source-designator "synthea:")]
      (is (result/ok? r))
      (is (= :synthea (:kind (:payload r))))
      (is (= generate/default-seed (:seed (:payload r))))
      (is (= generate/default-population (:population (:payload r)))))))

;; ---- generator Source parsing (SS-2 Step 4): synthea:/sim: now
;; construct real, validated Source values through the registry
;; (ehrt.tools.corpus.generators) -- never executed here, only
;; validated+shaped; execution is ehrt.tools.corpus.generator-
;; source/resolve!'s own, later job. ----

(deftest generator-source-non-numeric-seed-is-invalid-params-not-a-thrown-exception-test
  (testing "\"abc\" doesn't coerce to an int -- left as a string, so the registry's own
            params-schema rejects it (ADR-0004: a bad external value is a rejection, never a throw)"
    (let [r (url/parse-source-designator "sim:?seed=abc")]
      (is (result/rejected? r))
      (is (= :invalid-generator-params (:category r))))))

(deftest generator-source-explicit-params-override-defaults-test
  (let [r (url/parse-source-designator "synthea:?seed=7&population=3")]
    (is (result/ok? r))
    (is (= 7 (:seed (:payload r))))
    (is (= 3 (:population (:payload r))))
    (is (= generate/default-reference-date (:reference-date (:payload r))))))

;; ---- negative cases (ruling 4) ----

(deftest unknown-scheme-test
  (let [r (url/parse-source-designator "ftp:./corpus")]
    (is (result/rejected? r))
    (is (= :unknown-source-scheme (:category r)))))

(deftest whitespace-is-malformed-test
  (let [r (url/parse-source-designator "dir: ./corpus")]
    (is (result/rejected? r))
    (is (= :malformed-source-designator (:category r)))))

(deftest missing-required-field-test
  (testing "dir: with no path at all propagates dir-source's own :invalid-source rejection"
    (let [r (url/parse-source-designator "dir:")]
      (is (result/rejected? r))
      (is (= :invalid-source (:category r))))))

(deftest no-scheme-at-all-test
  (let [r (url/parse-source-designator "just-a-bare-path")]
    (is (result/rejected? r))
    (is (= :malformed-source-designator (:category r)))))

;; ---- sink twins: same shape, :format is mandatory (D3) ----

;; ---- path-designator->path (ruling 7, Step 6's CLI-boundary sugar) ----

(deftest path-designator->path-test
  (testing "dir:/file: designators resolve to their :path component"
    (is (= "./corpus" (url/path-designator->path "dir:./corpus")))
    (is (= "./corpus" (url/path-designator->path "dir:./corpus?format=v2-er7")))
    (is (= "./out/one.json" (url/path-designator->path "file:./out/one.json"))))
  (testing "bare paths pass through unchanged"
    (is (= "./corpus" (url/path-designator->path "./corpus")))
    (is (= "target/gate-fhir" (url/path-designator->path "target/gate-fhir"))))
  (testing "a Windows absolute path is never mistaken for a scheme"
    (is (= "C:\\Users\\prags\\corpus" (url/path-designator->path "C:\\Users\\prags\\corpus"))))
  (testing "other recognized schemes (not file-path-shaped) pass through unchanged"
    (is (= "sim:?seed=42" (url/path-designator->path "sim:?seed=42")))
    (is (= "blaze://host:8080/fhir" (url/path-designator->path "blaze://host:8080/fhir"))))
  (testing "dir:/file: with no path at all falls back to the original string"
    (is (= "dir:" (url/path-designator->path "dir:")))))

(deftest sink-designator-examples-test
  (testing "file: sink round-trips with mandatory :format"
    (let [printed (url/print-sink-designator {:kind :file :path "./out/one.json" :format :fhir-json})]
      (is (result/ok? printed))
      (is (= "file:./out/one.json?format=fhir-json" (:payload printed)))))
  (testing "dir: sink missing :format is rejected, not silently inferred (D3)"
    (let [r (url/parse-sink-designator "dir:./out")]
      (is (result/rejected? r))
      (is (= :invalid-sink (:category r)))))
  (testing "stdout: is recognized and now supported (SS-4 Step 3) -- format/framing thread through"
    (let [r (url/parse-sink-designator "stdout:?format=v2-er7&framing=mllp")]
      (is (result/ok? r))
      (is (= {:kind :stdout :format :v2-er7 :framing :mllp} (:payload r)))))
  (testing "a bare stdout:?format=... (no framing) round-trips"
    (let [printed (url/print-sink-designator {:kind :stdout :format :fhir-json})]
      (is (result/ok? printed))
      (is (= "stdout:?format=fhir-json" (:payload printed)))
      (let [parsed (url/parse-sink-designator (:payload printed))]
        (is (result/ok? parsed))
        (is (= {:kind :stdout :format :fhir-json} (:payload parsed))))))
  (testing "blaze:// is a recognized sink scheme but not-yet-supported"
    (let [r (url/parse-sink-designator "blaze://host:8080/fhir")]
      (is (result/rejected? r))
      (is (= :unsupported-sink-kind (:category r)))))
  (testing "an unknown sink scheme is rejected by name"
    (let [r (url/parse-sink-designator "sim:?seed=1")]
      (is (result/rejected? r))
      (is (= :unknown-sink-scheme (:category r))))))
