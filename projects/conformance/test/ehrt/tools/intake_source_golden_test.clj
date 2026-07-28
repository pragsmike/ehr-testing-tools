(ns ehrt.tools.intake-source-golden-test
  "SS-1 Step 4 acceptance (ruling 5, docs/source-sink-design.md D1/D7):
  corpus.intake, called through the new dir: Source value
  (intake/intake-via-source!), must produce a byte-identical catalog to
  the pre-SS-1 call shape (intake/intake! with a bare :source-dir
  string) -- against a REAL generated corpus, not a hand-crafted
  fixture, since the acceptance claim is about a realistic corpus tree,
  not just intake_test.clj's own synthetic-fixture unit coverage of the
  same function. Needs a real `corpus generate` (network + subprocess
  on first run, cached after) -- ^:integration, run via `make
  integration`, not `make test`.

  Re-baseline note (Step 5, D-c's :source -> :origin rename): this
  comparison is a live A/B diff against a freshly generated corpus
  each run, not a stored golden fixture file, so the rename needed no
  separate fixture update -- both sides of the diff write :origin now,
  and still match each other."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [ehrt.tools.interface :as result]
            [ehrt.tools.interface :as generate]
            [ehrt.tools.interface :as intake]
            [ehrt.tools.interface :as source-sink]
            [ehrt.tools.interface :as golden])
  (:import [java.io File]))

(def ^:private corpus-dir "target/integration-ss1-golden-corpus")
(def ^:private out-a "target/integration-ss1-golden-catalog-a")
(def ^:private out-b "target/integration-ss1-golden-catalog-b")

(defn- delete-tree!
  [^File f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)] (delete-tree! child)))
  (.delete f))

(defn- ensure-corpus!
  "Reuses an already-generated corpus-dir across runs (generation is
  the slow part); only regenerates when missing."
  []
  (when-not (.isDirectory (io/file corpus-dir "fhir"))
    (delete-tree! (io/file corpus-dir))
    (let [r (generate/generate! {:out-dir corpus-dir})]
      (when-not (result/ok? r)
        (throw (ex-info "golden-catalog test: generation failed -- run `ehr artifact fetch` first" r)))))
  corpus-dir)

(deftest ^:integration intake-through-dir-source-is-byte-identical-to-pre-ss-1-catalog-test
  (let [dir (ensure-corpus!)]
    (delete-tree! (io/file out-a))
    (delete-tree! (io/file out-b))
    (let [pre-ss1 (intake/intake! {:source-dir dir :source-label "golden" :out out-a :received "2026-07-28"})
          source-result (source-sink/dir-source {:path dir})
          _ (is (result/ok? source-result))
          via-source (intake/intake-via-source!
                      {:source (:payload source-result)
                       :source-label "golden" :out out-b :received "2026-07-28"})]
      (is (result/ok? pre-ss1))
      (is (result/ok? via-source))
      (is (= (:catalog (:payload pre-ss1)) (:catalog (:payload via-source)))
          "the returned catalog values must be identical")
      (let [comparison (golden/compare-catalogs out-a out-b)]
        (is (:identical? comparison)
            (str "golden-comparison law violated -- diverging files: " (:diverging-files comparison)))))))
