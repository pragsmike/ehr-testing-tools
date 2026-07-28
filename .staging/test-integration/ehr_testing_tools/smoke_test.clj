(ns ehr-testing-tools.smoke-test
  "T1, integration-smoke (2026-07-27 verification-tiers session, ADR-0016):
  the sub-2-minute session-boundary tier between T0 (make test, hermetic,
  seconds) and T2 (make integration, ~19min cold / DOC-4) -- proves the
  two real-engine seams T2 exercises at length (the FHIR validator
  subprocess, the ehr-testing-sim cross-repo consumer loop) still wire
  end-to-end, without T2's own full cost. `make integration-smoke`
  (`:integration-smoke` alias).

  FHIR half: ONE clean/mutant pair (not contract_pairing_test.clj's five
  operator x locator pairings) -- asserts PAIRING POLARITY ONLY (the
  mutant is convicted at its own locator; the clean file is not), never
  an aggregate :pass verdict -- EXP-C5/contract_pairing_test.clj's own
  finding stands: a real US-Core-profiled Synthea file always carries
  hundreds of incidental profile-driven findings, so aggregate verdict
  can't discriminate here either. The two gate-file calls below share the
  session's own target/verdict-cache (judge.verdict-cache, ADR-0016) with
  every other `gate fhir` invocation this session makes -- the FIRST run
  in a session pays for two real validator_cli.jar subprocess launches;
  every subsequent T1 run against the SAME fixed corpus (same content
  hash, same operator/locator) is a cache hit on both files and finishes
  near-instantly. The 2-minute budget target is measured warm, per this
  session's own ruling 1; a cold run may exceed it once, the first time
  in a session.

  sim-harness half: ONE run! at a fixed seed (100, 1 patient -- matching
  sim_manifest_contract_test.clj's own smallest-known-fast invocation),
  skip-when-absent (sim-harness/available?, same convention every
  sim-consuming suite in this tree already uses) -- asserts the run
  completes and its own emitted :manifest validates against
  corpus.manifest/ManifestV1_1 (session ruling 1's own \"manifest
  validates\" -- the SAME binding contract
  sim_manifest_contract_test.clj checks at T2 depth, field-by-field;
  this tier asserts only the schema-validation half, not each field, and
  is not a substitute for that suite: a schema-conformant manifest can
  still drift on individual field values, which is exactly what T2's
  own per-field assertions catch and T1 deliberately does not)."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [malli.core :as m]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.artifact :as artifact]
            [ehr-testing-tools.corpus.generate :as generate]
            [ehr-testing-tools.corpus.manifest :as manifest]
            [ehr-testing-tools.corpus.mutate :as mutate]
            [ehr-testing-tools.corpus.operators :as operators]
            [ehr-testing-tools.judge.fhir :as gate]
            [ehr-testing-tools.sim-harness :as sim-harness])
  (:import [java.io File]))

(def ^:private work-dir "target/integration-smoke")
(def ^:private lockfile-artifacts
  (delay (:artifacts (:payload (artifact/read-lockfile "artifacts.lock.edn")))))

(def ^:private base-file (atom nil))

(defn- select-patient-file
  [corpus-dir]
  (->> (.listFiles (io/file corpus-dir "fhir"))
       (filter #(and (str/ends-with? (.getName ^File %) ".json")
                     (not (re-find #"hospitalInformation|practitionerInformation" (.getName ^File %)))))
       first))

(defn- delete-tree!
  "Clears a fixture's own last run before regenerating into it --
  corpus.generate!'s :out-dir-exists guard (Step 4, the determinism
  probe's 2026-07-28 finding) now rejects a rerun into a non-empty
  directory, so a fixed work-dir like this suite's own is no longer
  safely re-runnable without this."
  [^File f]
  (when (.exists f)
    (doseq [child (reverse (file-seq f))] (.delete ^File child))))

(defn- generate-fixture!
  "Same pinned Synthea settings as contract_pairing_test.clj's own
  fixture (EXP-A4/EXP-B2/EXP-C5) -- population 1, so generation itself
  stays cheap; a distinct work-dir keeps this suite's corpus independent
  of contract-pairing's own, so the two suites never race on the same
  directory when both run in the same `make integration`."
  [f]
  (let [corpus-dir (str work-dir "/corpus")
        _ (delete-tree! (io/file corpus-dir))
        gen-result (generate/generate! {:config-path "config/synthea/synthea.properties"
                                         :seed 100 :clinician-seed 555 :population 1
                                         :reference-date "20260101" :out-dir corpus-dir})]
    (if-not (result/ok? gen-result)
      (throw (ex-info "integration-smoke fixture: corpus generation failed -- run `ehr artifact fetch` for synthea/temurin-jdk first" gen-result))
      (reset! base-file (.getAbsolutePath (select-patient-file corpus-dir)))))
  (f))

(clojure.test/use-fixtures :once generate-fixture!)

(defn- finding-matching-locator
  [findings locator-suffix]
  (filter #(str/ends-with? (:path (:locator %)) locator-suffix) findings))

(deftest ^:integration fhir-clean-vs-mutant-pairing-polarity-smoke-test
  (let [gate-opts {:artifacts @lockfile-artifacts :out-dir work-dir}
        clean-result (gate/gate-file @base-file gate-opts)
        base-data (json/read-str (slurp @base-file))
        operator (operators/lookup :duplicate-element "1")
        mutate-result (mutate/mutate base-data operator {:format :fhir :path "entry[0].resource.gender"})
        _ (when-not (result/ok? mutate-result)
            (throw (ex-info "integration-smoke: mutate failed" mutate-result)))
        mutant-path (str work-dir "/duplicate-element-mutant.json")
        _ (io/make-parents mutant-path)
        _ (spit mutant-path (json/write-str (:mutant (:payload mutate-result))))
        mutant-result (gate/gate-file mutant-path gate-opts)]
    (is (result/ok? clean-result) "the clean file gates without an operational error")
    (is (result/ok? mutant-result) "the mutant gates without an operational error")
    (let [clean-matches (finding-matching-locator (:findings (:payload clean-result)) "entry[0].resource.gender")
          mutant-matches (finding-matching-locator (:findings (:payload mutant-result)) "entry[0].resource.gender")]
      (is (not (some #(= :rejected (:disposition %)) clean-matches))
          "polarity: the unmodified file must carry no rejecting finding at this locator")
      (is (some #(= :rejected (:disposition %)) mutant-matches)
          "polarity: the mutant must carry a rejecting finding at this locator")
      (is (some #(= "invalid" (:code %)) mutant-matches)))))

(deftest ^:integration sim-harness-manifest-smoke-test
  (if-not (sim-harness/available?)
    (do (println sim-harness/absence-message)
        (is true sim-harness/absence-message))
    (let [run-result (sim-harness/run! {:seed 100 :patients 1})]
      (is (result/ok? run-result) "sim-harness/run! completes at a fixed seed")
      (when (result/ok? run-result)
        (let [mf (:manifest (:payload run-result))]
          (is (m/validate manifest/ManifestV1_1 mf)
              (str "sim's emitted manifest does not conform to ManifestV1_1: "
                   (pr-str (m/explain manifest/ManifestV1_1 mf)))))))))
