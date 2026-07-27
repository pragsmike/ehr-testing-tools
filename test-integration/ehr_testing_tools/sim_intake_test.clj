(ns ehr-testing-tools.sim-intake-test
  "An `ehr corpus intake` trial over an ehr-testing-sim corpus: sim run
  --emit hl7 -> messages written as .hl7 files, plus the run's own
  manifest dropped alongside them as manifest.edn (Package-less output
  -- sim has no Package stage yet, per its own README pipeline diagram,
  so 'corpus plus manifest sitting in one directory' is the closest
  thing to a delivered corpus this repo's intake can be pointed at
  today).

  ADR-0012's own [correction] originally predicted, and this test's own
  earlier form confirmed as a session FINDING (ADR-0013 clause 3), that
  corpus.intake never read a manifest at all: it catalogued every file
  by content hash and sniffed format only, and a manifest.edn sitting
  in the source directory catalogued as :unknown with none of its
  provenance fields (:stage, :generator, :seeds, ...) surviving into
  the catalog entry. ADR-0014 closes that gap: intake now reads an
  optional, directory-scoped manifest.edn sidecar and attaches
  :provenance to every catalog entry it covers -- manifest.edn's own
  entry included, not special-cased. This test now proves that
  resolution directly against a *real* sim manifest, not a synthetic
  unit fixture: every message file's :provenance carries the run's
  actual :stage/:generator/:seeds, and the manifest's own :generator
  map (real name, real version, real sha256) flows through byte-
  identically -- something the unit-level fixtures in
  test/ehr_testing_tools/corpus/intake_test.clj cannot cover, since
  they build their own synthetic ManifestV1_1 values rather than
  invoking sim. Skips cleanly (see sim-harness/absence-message) when
  ../ehr-testing-sim isn't checked out."
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
                  "manifest.edn's own sniffed :format is still :unknown -- ADR-0014 adds :provenance, it does not teach sniff-format to recognize manifests")
              ;; ADR-0014: every entry in this directory -- the messages
              ;; AND manifest.edn's own entry, not special-cased --
              ;; carries :provenance now that a validating sidecar sits
              ;; beside them.
              (is (every? #(some? (:provenance %)) catalog)
                  "every catalog entry in this directory gains :provenance from the real sim manifest")
              (is (every? #(= (:stage manifest) (get-in % [:provenance :stage])) catalog))
              (is (every? #(= (:seeds manifest) (get-in % [:provenance :seeds])) catalog))
              (is (= (select-keys manifest [:schema-version :stage :generator :seeds])
                     (:provenance manifest-entry))
                  "manifest.edn's own catalog entry carries its own identity as :provenance too")
              ;; The one assertion the unit fixtures can't cover: the
              ;; REAL sim manifest's own :generator (name, version, and
              ;; sha256 -- whatever they actually are for this sim
              ;; checkout) flows through byte-identically into a
              ;; message's catalog provenance. A synthetic fixture can
              ;; only assert its own hand-built generator map round-
              ;; trips; this asserts sim's real one does.
              (is (= (:generator manifest)
                     (get-in (first hl7-entries) [:provenance :generator]))
                  "the real sim manifest's own :generator flows through byte-identically, not a synthetic stand-in"))))))))
