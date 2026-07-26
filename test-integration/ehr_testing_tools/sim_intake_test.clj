(ns ehr-testing-tools.sim-intake-test
  "Task 4 -- an `ehr corpus intake` trial over an ehr-testing-sim
  corpus: sim run --emit hl7 -> messages written as .hl7 files, plus the
  run's own manifest dropped alongside them as manifest.edn
  (Package-less output -- sim has no Package stage yet, per its own
  README pipeline diagram, so 'corpus plus manifest sitting in one
  directory' is the closest thing to a delivered corpus this repo's
  intake can be pointed at today).

  ADR-0012's own [correction] already predicts this session's finding
  before it runs: corpus.intake never reads a manifest at all -- it
  catalogs every file it finds by content hash and sniffed format
  (:fhir-json/:v2-er7/:unknown), and a manifest.edn sitting in the
  source directory is catalogued as :unknown like any other
  unrecognized file. This test proves that prediction directly rather
  than re-asserting it from the ADR: the manifest's own provenance
  fields (:stage, :generator, :seeds, ...) do NOT survive into the
  catalog entry -- a genuine impedance mismatch between what a
  hypothetical future manifest-aware intake might want and what today's
  intake actually does, recorded as this session's own FINDING rather
  than treated as a bug in this test. Skips cleanly (see
  sim-harness/absence-message) when ../ehr-testing-sim isn't checked
  out."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.corpus.intake :as intake]
            [ehr-testing-tools.sim-harness :as sim-harness]))

(def ^:private work-dir "target/sim-intake")

(deftest ^:integration sim-corpus-intake-test
  (if-not (sim-harness/available?)
    (do (println sim-harness/absence-message)
        (is true sim-harness/absence-message))
    (let [run-result (sim-harness/run! {:seed 7 :patients 3 :emit "hl7"})]
      (when-not (result/ok? run-result)
        (throw (ex-info "sim-intake: sim run failed" run-result)))
      (let [{:keys [messages manifest]} (:payload run-result)
            source-dir (str work-dir "/source")
            out-dir (str work-dir "/out")]
        (.mkdirs (io/file source-dir))
        (dorun (map-indexed
                (fn [i m] (spit (io/file source-dir (format "msg-%03d.hl7" i)) m))
                messages))
        (spit (io/file source-dir "manifest.edn") (pr-str manifest))
        (let [intake-result (intake/intake! {:source-dir source-dir
                                             :source-label "ehr-testing-sim"
                                             :out out-dir
                                             :received "2026-07-26"})]
          (is (result/ok? intake-result) "intake ran to completion over sim's output")
          (let [{:keys [catalog intake-record]} (:payload intake-result)]
            (is (every? intake/valid-catalog-entry? catalog)
                "every catalog entry conforms to CatalogEntry")
            (is (intake/valid-intake-record? intake-record))
            (is (= (inc (count messages)) (count catalog))
                "one catalog entry per message file, plus the manifest.edn sidecar")
            (is (= (count catalog) (:file-count intake-record)))
            (let [hl7-entries (filter #(= :v2-er7 (:format %)) catalog)
                  manifest-entry (first (filter #(= "manifest.edn" (:path %)) catalog))]
              (is (= (count messages) (count hl7-entries))
                  "every emitted HL7 message sniffs as :v2-er7")
              (is (every? #(= :foreign (:layer %)) catalog))
              (is (every? #(= "ehr-testing-sim" (:source %)) catalog))
              (is (some? manifest-entry) "manifest.edn is catalogued too, not skipped")
              (is (= :unknown (:format manifest-entry))
                  (str "FINDING (predicted by ADR-0012's own [correction]): intake "
                       "sniffs manifest.edn as :unknown -- it never parses it as a manifest"))
              ;; The impedance mismatch itself, asserted directly: the
              ;; manifest's catalog entry is CatalogEntry-shaped only --
              ;; none of the manifest's own provenance fields (:stage,
              ;; :generator, :seeds, ...) appear on it. A future
              ;; manifest-aware intake would need a different join, not
              ;; a bug fix to this one.
              (is (not (contains? manifest-entry :stage)))
              (is (not (contains? manifest-entry :generator)))
              (is (not (contains? manifest-entry :seeds)))
              (is (= #{:id :path :format :layer :source :received} (set (keys manifest-entry)))
                  "the manifest's own catalog entry carries exactly CatalogEntry's fields -- no provenance passthrough"))))))))
