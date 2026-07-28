(ns ehrt.tools.corpus.sink-composability-test
  "The composability law (D3, docs/source-sink-design.md Part III):
  every sink's output is a valid source. SS-4 ruling 4's dir/file form
  was REDUCED per D-d's own STOP (docs/source-sink-design.md Decision
  Register): dir/file sinks emitted no manifest that session, so the
  property proved only the hash-identity half -- write via a :dir Sink,
  intake the same directory back through a :dir Source, and the
  catalog's content hashes and :origin survive.

  SS-4b (2026-07-28, D-d resolved via ADR-0020) completes the deferred
  half below: dir sinks now accept an :operation-manifest
  (ehrt.tools.corpus.sink-write), and intake recognizes it
  (ehrt.tools.corpus.intake's second sidecar recognizer) --
  the property gains the provenance half ruling 5 names: the catalog's
  :operation-provenance :origin reflects the manifest's own :producer,
  and per-item :input-hash survives wherever the write actually
  supplied one, absent wherever it didn't (present-iff-known, not a
  nil placeholder). The original hash-identity-only property below is
  kept unchanged -- it still documents the no-operation-manifest case,
  which stays fully supported (:operation-manifest is opt-in)."
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.java.io :as io]
            [ehrt.tools.result :as result]
            [ehrt.tools.digest :as digest]
            [ehrt.tools.corpus.source-sink :as ss]
            [ehrt.tools.corpus.sink-write :as write]
            [ehrt.tools.corpus.intake :as intake])
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

;; ---- the provenance half (SS-4b, D-d resolved via ADR-0020, ruling 5) ----

(def ^:private producer
  {:name "ehr-testing-tools" :identity "pre-release" :git "abc1234-dirty"})

(def ^:private operation
  {:kind :mutate :operator-id :blank-required-field :operator-version "1"
   :locator {:format :v2 :path "MSH-9"}})

(def ^:private item-set-with-input-hash-flags-gen
  "Like item-set-gen, but pairs each item's content with a boolean:
  whether this write actually supplied an input-hash for it. The
  property must hold in both cases (present-iff-known, ruling 1) --
  not just the all-or-nothing case a simpler generator would only
  cover by chance."
  (gen/let [items item-set-gen
            flags (gen/vector gen/boolean (count items))]
    (zipmap (keys items) (map vector (vals items) flags))))

(deftest dir-sink-write-then-intake-provenance-property-test
  (let [check-result
        (tc/quick-check 30
          (prop/for-all [items item-set-with-input-hash-flags-gen]
            (let [target (temp-dir-path)
                  files (into {} (map (fn [[filename [content _]]] [filename content])) items)
                  input-hashes (into {}
                                      (keep (fn [[filename [content has-input-hash?]]]
                                              (when has-input-hash?
                                                [filename (digest/sha256-string (str "parent-of-" content))])))
                                      items)
                  sink (:payload (ss/dir-sink {:path target :format :fhir-json}))
                  write-result (write/write-dir! sink files
                                                  :operation-manifest
                                                  {:producer producer :operation operation
                                                   :written-at "2026-07-28"
                                                   :input-hashes input-hashes})
                  out-dir (temp-dir-path)
                  source (:payload (ss/dir-source {:path target}))
                  intake-result (intake/intake-via-source!
                                 {:source source :source-label "composability-prop"
                                  :out out-dir :received "2026-07-28"})]
              (and (result/ok? write-result)
                   (result/ok? intake-result)
                   (let [catalog (:catalog (:payload intake-result))]
                     (and (= (inc (count files)) (count catalog))
                          (every? #(= producer (:origin (:operation-provenance %))) catalog)
                          (every? (fn [[filename [content _]]]
                                    (let [entry (first (filter #(= filename (:path %)) catalog))]
                                      (and (some? entry)
                                           (= (digest/sha256-string content) (:id entry))
                                           (= (get input-hashes filename)
                                              (:input-hash (:operation-provenance entry))))))
                                  items)))))))]
    (is (:pass? check-result) (str check-result))))

(deftest dir-sink-write-then-intake-provenance-concrete-example-test
  (let [target (temp-dir-path)
        out-dir (temp-dir-path)
        items {"a.hl7" "AAA" "b.hl7" "BBB"}
        parent-hash (digest/sha256-string "parent-of-AAA")
        write-result (write/write-dir! (:payload (ss/dir-sink {:path target :format :v2-er7}))
                                        items
                                        :operation-manifest
                                        {:producer producer :operation operation
                                         :written-at "2026-07-28"
                                         :input-hashes {"a.hl7" parent-hash}})
        source (:payload (ss/dir-source {:path target}))
        intake-result (intake/intake-via-source!
                       {:source source :source-label "concrete" :out out-dir :received "2026-07-28"})]
    (is (result/ok? write-result))
    (is (result/ok? intake-result))
    (let [catalog (:catalog (:payload intake-result))
          a-entry (first (filter #(= "a.hl7" (:path %)) catalog))
          b-entry (first (filter #(= "b.hl7" (:path %)) catalog))]
      (is (= 3 (count catalog)) "a.hl7, b.hl7, and operation-manifest.edn's own catalog entry")
      (is (= producer (:origin (:operation-provenance a-entry))))
      (is (= parent-hash (:input-hash (:operation-provenance a-entry))))
      (is (not (contains? (:operation-provenance b-entry) :input-hash))
          "b.hl7 got no input-hash -- present iff the write actually supplied one")
      (is (= producer (:origin (:operation-provenance
                                 (first (filter #(= "operation-manifest.edn" (:path %)) catalog)))))
          "the sidecar's own catalog entry is not special-cased, same discipline as ADR-0014's manifest.edn"))))
