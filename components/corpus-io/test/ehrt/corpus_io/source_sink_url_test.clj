(ns ehrt.corpus-io.source-sink-url-test
  "Test-first (ruling 4, SS-1 Step 3): written before ehrt.tools.
  corpus.source-sink-url existed. Covers the URL<->map parser's
  sink-side surface (print-sink-designator/parse-sink-designator, both
  still owned by this namespace) plus path-designator->path (also
  unmoved -- no domain edge). Source-side coverage (parse-source-
  designator and everything that calls it, including the non-
  generator :dir/:file/:stdin/:blaze cases) moved whole to
  ehrt.tools.corpus.generator-source-test (corpus-io stage 2,
  2026-07-31): the function itself relocated there, since its
  generator-kind branch is the one piece of this namespace's own
  parsing surface with a real edge into the domain's generator
  registry -- AR-5 (tests follow their namespaces)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.string :as str]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus-io.source-sink-url :as url]))

;; ---- generators: safe path segments only -- '?'/'&'/'=' would be
;; ambiguous with the query grammar, and this scheme doesn't percent-
;; encode :path the way it does query values (matching the design's
;; own unencoded example, "dir:./corpus") ----

(def safe-path-gen
  (gen/fmap (fn [segs] (str "./" (str/join "/" segs)))
            (gen/vector (gen/fmap #(apply str %) (gen/vector gen/char-alpha 1 8)) 1 4)))

(def format-gen (gen/elements [:fhir-json :v2-er7 :inferred]))
(def framing-gen (gen/elements [:file-per-item :er7-multi :ndjson :bundle-entries :mllp]))

(defn- sink-map-gen
  [kind]
  (gen/let [path safe-path-gen
            format format-gen
            framing (gen/one-of [(gen/return nil) framing-gen])]
    (cond-> {:kind kind :path path :format format}
      framing (assoc :framing framing))))

(deftest sink-designator-round-trip-property-test
  (testing "parse ∘ print = identity on canonical dir/file Sink maps (D4 law, sink twin)"
    (doseq [kind [:dir :file]]
      (let [check-result
            (tc/quick-check 100
              (prop/for-all [m (sink-map-gen kind)]
                (let [printed (url/print-sink-designator m)]
                  (and (kernel/ok? printed)
                       (let [parsed (url/parse-sink-designator (:payload printed))]
                         (and (kernel/ok? parsed)
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
                (and (kernel/ok? printed)
                     (let [parsed (url/parse-sink-designator (:payload printed))]
                       (and (kernel/ok? parsed)
                            (= m (:payload parsed))))))))]
      (is (:pass? check-result) (str "stdout sink round-trip failed: " (:shrunk check-result))))))

;; ---- path-designator->path (ruling 7, Step 6's CLI-boundary sugar) ----

(deftest path-designator->path-test
  (testing "dir:/file: designators resolve to their :path component"
    (is (= "./corpus" (url/path-designator->path "dir:./corpus")))
    (is (= "./corpus" (url/path-designator->path "dir:./corpus?format=v2-er7")))
    (is (= "./out/one.json" (url/path-designator->path "file:./out/one.json"))))
  (testing "bare paths pass through unchanged"
    (is (= "./corpus" (url/path-designator->path "./corpus")))
    (is (= "out/scratch/gate-fhir" (url/path-designator->path "out/scratch/gate-fhir"))))
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
      (is (kernel/ok? printed))
      (is (= "file:./out/one.json?format=fhir-json" (:payload printed)))))
  (testing "dir: sink missing :format is rejected, not silently inferred (D3)"
    (let [r (url/parse-sink-designator "dir:./out")]
      (is (kernel/rejected? r))
      (is (= :invalid-sink (:category r)))))
  (testing "stdout: is recognized and now supported (SS-4 Step 3) -- format/framing thread through"
    (let [r (url/parse-sink-designator "stdout:?format=v2-er7&framing=mllp")]
      (is (kernel/ok? r))
      (is (= {:kind :stdout :format :v2-er7 :framing :mllp} (:payload r)))))
  (testing "a bare stdout:?format=... (no framing) round-trips"
    (let [printed (url/print-sink-designator {:kind :stdout :format :fhir-json})]
      (is (kernel/ok? printed))
      (is (= "stdout:?format=fhir-json" (:payload printed)))
      (let [parsed (url/parse-sink-designator (:payload printed))]
        (is (kernel/ok? parsed))
        (is (= {:kind :stdout :format :fhir-json} (:payload parsed))))))
  (testing "blaze:// is a recognized sink scheme but not-yet-supported"
    (let [r (url/parse-sink-designator "blaze://host:8080/fhir")]
      (is (kernel/rejected? r))
      (is (= :unsupported-sink-kind (:category r)))))
  (testing "an unknown sink scheme is rejected by name"
    (let [r (url/parse-sink-designator "sim:?seed=1")]
      (is (kernel/rejected? r))
      (is (= :unknown-sink-scheme (:category r))))))
