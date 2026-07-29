(ns ehrt.tools.zero-flag-reproducibility-test
  "D9's acceptance property (docs/source-sink-design.md Part IX.2,
  ADR-0019): `ehr corpus generate` with no flags is byte-reproducible.
  Two real generations at the pinned D9 values, into the same directory
  with a full delete between them (the supported \"regenerate\" workflow
  once the output-dir-exists guard lands, per the determinism probe's
  2026-07-28 addendum) -- payload files must be byte-identical modulo
  EXP-A4's two already-registered canonicalizations (filename timestamp
  suffix, Synthea's own metadata/*.json run-audit fields), and
  manifest.edn must be identical except for exactly the two fields named
  as D8-exempt record-keeping in Part IX.2: [:invocation :started-at]
  and [:invocation :duration-ms]. Red until Step 4 wires the D9 defaults
  and the output-dir-exists guard into corpus.generate."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [ehrt.tools.interface :as result]
            [ehrt.tools.interface :as generate]
            [ehrt.tools.interface :as canonicalizers])
  (:import [java.io File]))

(def ^:private work-dir "target/integration-ux1-zero-flag-repro")

(defn- delete-tree!
  [^File f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)] (delete-tree! child)))
  (.delete f))

(defn- canonical-fhir-entries
  "{canonical-filename -> raw file bytes as a string} for every file
  under dir/fhir, with the two timestamp-suffixed filenames normalized
  via the same canonicalizer EXP-A4 registered."
  [dir]
  (into {}
        (for [f (.listFiles (io/file dir "fhir"))]
          [(canonicalizers/strip-run-timestamp-suffix (.getName ^File f)) (slurp f)])))

(defn- canonical-metadata-entry
  "The single metadata/*.json file's content, parsed and stripped of
  Synthea's own per-execution audit fields via the same canonicalizer
  EXP-A4 registered -- there is exactly one such file per run."
  [dir]
  (let [f (first (.listFiles (io/file dir "metadata")))]
    (canonicalizers/strip-synthea-run-metadata (json/read-str (slurp f)))))

(def ^:private manifest-record-keeping-exemptions
  "D8-exempt record-keeping fields (docs/source-sink-design.md Part IX.2,
  2026-07-28 determinism probe): wall-clock timing describing the
  subprocess run, not a generation input."
  [[:invocation :started-at] [:invocation :duration-ms]])

(defn- strip-exemptions
  [manifest]
  (reduce (fn [m path] (update-in m (butlast path) dissoc (last path)))
          manifest manifest-record-keeping-exemptions))

(defn- generate-zero-flag!
  []
  (let [r (generate/generate! {:out-dir work-dir})]
    (when-not (result/ok? r)
      (throw (ex-info "zero-flag-reproducibility: generation failed -- run `ehr artifact fetch` for synthea/temurin-jdk first" r)))
    r))

(deftest ^:integration zero-flag-generate-is-byte-reproducible-test
  (delete-tree! (io/file work-dir))
  (let [run1 (generate-zero-flag!)
        manifest1 (:manifest (:payload run1))
        fhir1 (canonical-fhir-entries work-dir)
        metadata1 (canonical-metadata-entry work-dir)]
    (delete-tree! (io/file work-dir))
    (let [run2 (generate-zero-flag!)
          manifest2 (:manifest (:payload run2))
          fhir2 (canonical-fhir-entries work-dir)
          metadata2 (canonical-metadata-entry work-dir)]
      (is (= fhir1 fhir2) "canonicalized fhir/ payload must be byte-identical across two zero-flag runs")
      (is (= metadata1 metadata2) "canonicalized metadata/*.json content must be identical across two zero-flag runs")
      (is (not= (:started-at (:invocation manifest1)) (:started-at (:invocation manifest2)))
          "sanity: the two runs must actually be distinct invocations, not a cached no-op")
      (is (= (strip-exemptions manifest1) (strip-exemptions manifest2))
          "manifest.edn must be identical modulo exactly the D8-exempt record-keeping fields")
      ;; manifest.edn as written to disk (run2's, still on disk at this
      ;; point) must match run2's own returned payload.
      (is (= manifest2 (edn/read-string (slurp (io/file work-dir "manifest.edn"))))))))
