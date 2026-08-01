(ns ehrt.integration.smoke-test
  "T1, integration-smoke (2026-07-27 verification-tiers session, ADR-0016):
  the sub-2-minute session-boundary tier between T0 (make test, hermetic,
  seconds) and T2 (make integration, ~19min cold / DOC-4) -- proves the
  FHIR validator subprocess seam T2 exercises at length still wires
  end-to-end, without T2's own full cost.

  FHIR half only, as of 2026-07-28 (ADR-0004, carve-loss recovery
  session): this file originally also carried a sim-harness half (ONE
  sim run! plus a manifest-schema check); that half moved to
  `projects/conformance/test/ehrt/conformance/smoke_test.clj` in the same
  session, along the seam the original docstring already drew (\"FHIR
  half\" / \"sim-harness half\") -- the sim-harness half needs
  `ehrt.conformance.sim-harness`, a conformance-project-local test helper also
  required by conformance's own five sim_*_test.clj suites, which made
  keeping both halves in one project-scoped test file impossible once
  they needed different CI lanes (this half needs `ehr artifact fetch`
  machinery -- synthea, temurin-jdk, fhir-validator-cli -- which R18's
  two-lane rule deliberately keeps off the per-push path; the
  sim-harness half needs no external fetch at all and stays on
  conformance's per-push lane). See ADR-0004 for the full disposition;
  see the conformance-side file for the sim-harness half.

  ONE clean/mutant pair (not contract_pairing_test.clj's five operator x
  locator pairings) -- asserts PAIRING POLARITY ONLY (the mutant is
  convicted at its own locator; the clean file is not), never an
  aggregate :pass verdict -- EXP-C5/contract_pairing_test.clj's own
  finding stands: a real US-Core-profiled Synthea file always carries
  hundreds of incidental profile-driven findings, so aggregate verdict
  can't discriminate here either. The two gate-file calls below share the
  session's own target/verdict-cache (judge.verdict-cache, ADR-0016) with
  every other `gate fhir` invocation this session makes -- the FIRST run
  in a session pays for two real validator_cli.jar subprocess launches;
  every subsequent T1 run against the SAME fixed corpus (same content
  hash, same operator/locator) is a cache hit on both files and finishes
  near-instantly. The 2-minute budget target is measured warm, per the
  original verification-tiers session's own ruling 1; a cold run may
  exceed it once, the first time in a session."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [ehrt.kernel.interface :as result]
            [ehrt.kernel.interface :as artifact]
            [ehrt.corpus.interface :as generate]
            [ehrt.corpus.interface :as mutate]
            [ehrt.corpus.interface :as operators]
            [ehrt.judge-fhir-official.interface :as gate])
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
  directory when both run in the same integration lane."
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
        operator (operators/operator-lookup :duplicate-element "1")
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
