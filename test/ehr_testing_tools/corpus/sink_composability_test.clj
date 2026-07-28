(ns ehr-testing-tools.corpus.sink-composability-test
  "The composability law (D3, docs/source-sink-design.md Part III):
  every sink's output is a valid source. SS-4 ruling 4's dir/file form,
  REDUCED per D-d's own STOP (docs/source-sink-design.md Decision
  Register): dir/file sinks emit no ManifestV1_1 sidecar this session
  (the manifest-interop probe found no honest field mapping for a sink
  write, ruling 2), so this property proves only the hash-identity
  half -- write via a :dir Sink, intake the same directory back through
  a :dir Source, and the catalog's content hashes and :origin survive
  -- not the :origin-reflects-the-sink's-manifest half ruling 4 also
  names, which waits on D-d. Test-first (ADR-0006): written before any
  Sink/intake code changed for this session, and green immediately --
  ehr-testing-tools.corpus.sink-write/write-dir! and
  ehr-testing-tools.corpus.intake/intake-via-source! both already exist
  from SS-1, so this property is a proof obligation on EXISTING code,
  not new production code -- exactly the composability law's own point:
  it should already hold, and this is what checks that it does."
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.java.io :as io]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.digest :as digest]
            [ehr-testing-tools.corpus.source-sink :as ss]
            [ehr-testing-tools.corpus.sink-write :as write]
            [ehr-testing-tools.corpus.intake :as intake])
  (:import [java.io File]))

(defn- temp-dir-path
  "A fresh, not-yet-existing directory path -- write-dir!'s own
  fail-if-exists discipline needs an unclaimed target, and a property
  test runs its body many times, so this must be re-derived per call,
  never a single fixture path shared across iterations."
  []
  (let [f (File/createTempFile "sink-composability-test" "")]
    (.delete f)
    (.getAbsolutePath f)))

;; ---- generators: small item sets, flat filenames (no nested dirs --
;; write-dir!'s own relative-path handling is already covered by
;; sink_write_test.clj; this property is about the round trip, not
;; about exercising every path shape write-dir! accepts) ----

(def ^:private safe-filename-gen
  (gen/fmap (fn [i] (str "item-" i ".dat")) gen/nat))

(def ^:private content-gen
  (gen/such-that seq (gen/fmap #(apply str %) (gen/vector gen/char-alphanumeric 1 40))))

(def ^:private item-set-gen
  "A map of {distinct-filename content}, 1-5 entries -- gen/map alone
  doesn't guarantee non-empty, so build from a vector of distinct keys
  paired with generated content instead."
  (gen/let [n (gen/choose 1 5)
            filenames (gen/vector-distinct safe-filename-gen {:num-elements n})
            contents (gen/vector content-gen n)]
    (zipmap filenames contents)))

(deftest dir-sink-write-then-intake-hash-identity-property-test
  (let [check-result
        (tc/quick-check 30
          (prop/for-all [items item-set-gen]
            (let [target (temp-dir-path)
                  sink (:payload (ss/dir-sink {:path target :format :fhir-json}))
                  write-result (write/write-dir! sink items)
                  out-dir (temp-dir-path)
                  source (:payload (ss/dir-source {:path target}))
                  intake-result (intake/intake-via-source!
                                 {:source source :source-label "composability-prop"
                                  :out out-dir :received "2026-07-28"})]
              (and (result/ok? write-result)
                   (result/ok? intake-result)
                   (let [catalog (:catalog (:payload intake-result))]
                     (and (= (count items) (count catalog))
                          (every? #(= "composability-prop" (:origin %)) catalog)
                          (every? (fn [[filename content]]
                                    (let [entry (first (filter #(= filename (:path %)) catalog))]
                                      (and (some? entry)
                                           (= (digest/sha256-string content) (:id entry)))))
                                  items)))))))]
    (is (:pass? check-result) (str check-result))))

(deftest dir-sink-write-then-intake-concrete-example-test
  (let [target (temp-dir-path)
        out-dir (temp-dir-path)
        items {"a.json" "AAA" "b.json" "BBB"}
        write-result (write/write-dir! (:payload (ss/dir-sink {:path target :format :fhir-json})) items)
        source (:payload (ss/dir-source {:path target}))
        intake-result (intake/intake-via-source!
                       {:source source :source-label "concrete" :out out-dir :received "2026-07-28"})]
    (is (result/ok? write-result))
    (is (result/ok? intake-result))
    (let [catalog (:catalog (:payload intake-result))]
      (is (= 2 (count catalog)))
      (is (= #{"a.json" "b.json"} (into #{} (map :path) catalog)))
      (is (= (digest/sha256-string "AAA")
             (:id (first (filter #(= "a.json" (:path %)) catalog))))))))
