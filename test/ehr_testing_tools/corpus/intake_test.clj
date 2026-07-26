(ns ehr-testing-tools.corpus.intake-test
  "Loading corpus.operators registers the seed catalog (same convention
  as mutate-test) -- the lineage-chaining test below calls corpus.mutate
  directly against an intaken file, so the registry must be populated."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [clojure.string]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.digest :as digest]
            [ehr-testing-tools.corpus.operators :as operators]
            [ehr-testing-tools.corpus.mutate :as mutate]
            [ehr-testing-tools.corpus.intake :as intake]
            [ehr-testing-tools.corpus.simhospital-corpus :as simhospital])
  (:import [java.io File]))

(defn- temp-dir []
  (let [f (File/createTempFile "intake-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

(def sample-bundle-json
  "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"p1\",\"gender\":\"female\"}}]}")

(def sample-v2-message
  "MSH|^~\\&|SND|FAC|RCV|FAC|20260724120000||ADT^A01|MSG00001|P|2.4\rEVN|A01|20260724120000\rPID|1||123456||Doe^John||19800101|M\r")

;; ---- sniff-format ----

(deftest sniff-format-detects-fhir-json-test
  (is (= :fhir-json (intake/sniff-format sample-bundle-json))))

(deftest sniff-format-detects-v2-er7-test
  (is (= :v2-er7 (intake/sniff-format sample-v2-message))))

(deftest sniff-format-falls-back-to-unknown-test
  (is (= :unknown (intake/sniff-format "just some plain text, not FHIR or v2")))
  (is (= :unknown (intake/sniff-format "")))
  (is (= :unknown (intake/sniff-format "[1, 2, 3]")) "valid JSON, but not a FHIR-shaped object"))

;; ---- content-hash: format-aware, and the FHIR branch must be THE
;; SAME function corpus.mutate itself uses, not merely an
;; equal-valued reimplementation -- this is what lets lineage chain
;; across the intake/mutate boundary with no adapter. ----

(deftest content-hash-fhir-json-matches-mutate-content-hash-test
  (is (= (mutate/content-hash (json/read-str sample-bundle-json))
         (intake/content-hash sample-bundle-json :fhir-json))))

(deftest content-hash-non-fhir-hashes-raw-bytes-test
  (is (re-matches #"^[0-9a-f]{64}$" (intake/content-hash sample-v2-message :v2-er7)))
  (is (re-matches #"^[0-9a-f]{64}$" (intake/content-hash "whatever" :unknown))))

;; ---- intake! ----

(deftest intake-catalogs-every-file-with-format-and-hash-test
  (let [src (temp-dir)
        out (temp-dir)
        _ (spit (io/file src "patient.json") sample-bundle-json)
        _ (spit (io/file src "adt.hl7") sample-v2-message)
        _ (spit (io/file src "notes.txt") "not clinical data at all")
        r (intake/intake! {:source-dir src :source-label "acme-pipeline"
                            :out out :received "2026-07-24"})]
    (is (result/ok? r))
    (let [catalog (:catalog (:payload r))]
      (is (= 3 (count catalog)))
      (is (every? intake/valid-catalog-entry? catalog))
      (is (every? #(= :foreign (:layer %)) catalog))
      (is (every? #(= "acme-pipeline" (:source %)) catalog))
      (is (every? #(= "2026-07-24" (:received %)) catalog))
      (is (= #{:fhir-json :v2-er7 :unknown} (set (map :format catalog)))))))

(deftest intake-catalog-entry-id-is-the-format-aware-content-hash-test
  (let [src (temp-dir)
        out (temp-dir)
        _ (spit (io/file src "patient.json") sample-bundle-json)
        r (intake/intake! {:source-dir src :source-label "x" :out out :received "2026-07-24"})
        entry (first (:catalog (:payload r)))]
    (is (= (mutate/content-hash (json/read-str sample-bundle-json)) (:id entry)))))

(deftest intake-writes-catalog-and-intake-record-edn-test
  (let [src (temp-dir)
        out (temp-dir)
        _ (spit (io/file src "patient.json") sample-bundle-json)
        r (intake/intake! {:source-dir src :source-label "acme" :out out :received "2026-07-24"})]
    (is (result/ok? r))
    (let [written-catalog (edn/read-string (slurp (io/file out "catalog.edn")))
          written-record (edn/read-string (slurp (io/file out "intake-record.edn")))]
      (is (= (:catalog (:payload r)) written-catalog))
      (is (= (:intake-record (:payload r)) written-record))
      (is (intake/valid-intake-record? written-record))
      (is (= "acme" (:source written-record)))
      (is (= "2026-07-24" (:date written-record)))
      (is (= 1 (:file-count written-record)))
      (is (= (:catalog-hash written-record)
             (digest/sha256-file (io/file out "catalog.edn")))))))

(deftest intake-handles-empty-source-dir-test
  (let [src (temp-dir)
        out (temp-dir)
        r (intake/intake! {:source-dir src :source-label "empty" :out out :received "2026-07-24"})]
    (is (result/ok? r))
    (is (= 0 (:file-count (:intake-record (:payload r)))))
    (is (= [] (:catalog (:payload r))))))

(deftest intake-catalogs-nested-directories-with-relative-paths-test
  (let [src (temp-dir)
        out (temp-dir)
        _ (.mkdirs (io/file src "nested" "deeper"))
        _ (spit (io/file src "nested" "deeper" "patient.json") sample-bundle-json)
        r (intake/intake! {:source-dir src :source-label "x" :out out :received "2026-07-24"})
        entry (first (:catalog (:payload r)))]
    (is (result/ok? r))
    (is (= 1 (count (:catalog (:payload r)))))
    (is (= "nested/deeper/patient.json" (clojure.string/replace (:path entry) "\\" "/")))))

;; ---- lineage roots: an intaken FHIR file chains to its catalog
;; content hash exactly as with generated files -- corpus.mutate
;; needs no adapter, no special-casing, to operate on intaken data. ----

(deftest intaken-fhir-file-mutant-lineage-chains-to-its-catalog-id-test
  (let [src (temp-dir)
        out (temp-dir)
        _ (spit (io/file src "patient.json") sample-bundle-json)
        intake-result (intake/intake! {:source-dir src :source-label "acme"
                                        :out out :received "2026-07-24"})
        entry (first (:catalog (:payload intake-result)))
        ;; corpus.mutate operates unchanged: read the intaken file the
        ;; same way any other FHIR JSON input would be read, mutate it.
        base-data (json/read-str (slurp (io/file src (:path entry))))
        operator (operators/lookup :remove-required-element "1")
        mutate-result (mutate/mutate base-data operator
                                      {:format :fhir :path "entry[0].resource.gender"})]
    (is (result/ok? mutate-result))
    (let [lineage (:lineage (:payload mutate-result))]
      (is (= (:id entry) (:parent lineage))
          "the mutant's lineage :parent must equal the intake catalog's own content-hash id -- same hash space, no adapter"))))

;; ---- a real foreign corpus: the vendored SimHospital messages
;; (ADR-0011). Every intake test above builds its own synthetic input;
;; this one runs the route against data this repo did not author, which
;; is the case intake exists for.
;;
;; Same division of labor as the er7 corpus tests: the exhaustive run
;; (all 1,013 messages intaken, 403 distinct patients recovered through
;; the catalog) was a one-time probe, registered as F27 -- it does not
;; belong in a per-push suite. What is asserted here is that intake's
;; public entry points handle the same hazard-selected slice `er7-test`
;; proves round-trips: real MSH framing, a repeated PID-3, a long OBX
;; tail, and the corpus's lone merge message. ----

(deftest intakes-the-simhospital-hazard-slice-test
  (let [src (temp-dir)
        out (temp-dir)
        slice (simhospital/hazard-slice)]
    (doseq [{:keys [label message]} slice]
      (spit (io/file src (str (name label) ".hl7")) message))
    (let [r (intake/intake! {:source-dir src :source-label "simhospital"
                             :out out :received "2026-07-26"})
          {:keys [catalog intake-record]} (:payload r)]
      (is (result/ok? r))
      (is (= 3 (count catalog)))
      (is (every? intake/valid-catalog-entry? catalog))
      (is (every? #(= :v2-er7 (:format %)) catalog)
          "real ER7 must sniff as :v2-er7 -- not merely this repo's hand-written fixtures")
      (is (every? #(= :foreign (:layer %)) catalog))
      (is (= 3 (count (distinct (map :id catalog))))
          "three structurally distinct messages must yield three distinct catalog ids")
      (is (= (set (map #(intake/content-hash (:message %) :v2-er7) slice))
             (set (map :id catalog)))
          "each catalog id must be the format-aware content hash of that message's own bytes")
      (is (intake/valid-intake-record? intake-record))
      (is (= 3 (:file-count intake-record)))
      (is (= "2026-07-26" (:date intake-record)))
      (is (= (:catalog-hash intake-record)
             (digest/sha256-file (io/file out "catalog.edn")))))))
