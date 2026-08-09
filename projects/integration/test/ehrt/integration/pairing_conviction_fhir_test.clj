(ns ehrt.integration.pairing-conviction-fhir-test
  "The `:judge-fhir-official` half of the pairing registry's own tier-
  one conviction loop (AR-PD-4, ADR-0088; storefront-fixture session,
  ADR-0091, AR-SD-2): for every registry row whose `:judge` is
  `:judge-fhir-official`, load its `:fixture`, apply its `:operator`
  at its `:locator`, gate the mutant through the REAL official
  validator (`fhir-validator-cli`, a real subprocess -- not an
  injected fake), and assert at least one `:expected` class among the
  gate's own findings.

  Lives here, not in `components/judge/test/ehrt/judge/pairing_
  conviction_test.clj` alongside the v2 rows: `judge-fhir-official/
  gate-file` needs the real `fhir-validator-cli` artifact fetched
  first (`ehr artifact fetch`), and that file's own test tree is
  composed by EVERY project, including `conformance`/`ehrt-cli`, whose
  ordinary push-triggered CI lane never primes the artifact cache
  (AGENTS.md's hermetic-test-suite rule -- the same reason
  `contract_pairing_test.clj` and `baseline_gating_test.clj` already
  live here rather than in a brick's own test tree). Tagged
  `^:integration`; run explicitly via `make integration` (requires
  `ehr artifact fetch` for temurin-jdk and fhir-validator-cli first)."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as kernel]
            [ehrt.judge.interface :as judge]
            [ehrt.corpus.interface :as corpus]
            [ehrt.judge-fhir-official.interface :as v2-fhir]))

(def ^:private work-dir "target/pairing-conviction-fhir")

(def ^:private lockfile-artifacts
  (delay (:artifacts (:payload (kernel/read-lockfile "artifacts.lock.edn")))))

(defn- fhir-rows
  []
  (filter #(= :judge-fhir-official (:judge %)) (judge/load-pairing-registry)))

(defn- mutant-content
  [{:keys [fixture locator] {:keys [id]} :operator}]
  (let [base (json/read-str (slurp (io/file fixture)))
        operator (corpus/operator-lookup id "1")
        mutate-result (corpus/mutate base operator {:format :fhir :path locator})]
    (when-not (kernel/ok? mutate-result)
      (throw (ex-info "pairing-conviction-fhir: mutate failed" (assoc mutate-result :row fixture))))
    (:mutant (:payload mutate-result))))

(defn- write-mutant!
  [{:keys [judge] {:keys [id]} :operator} content]
  (let [path (str work-dir "/" (name judge) "-" (name id) "-mutant.json")]
    (io/make-parents path)
    (spit path (json/write-str content))
    path))

(defn- gate-mutant
  [path]
  (let [r (v2-fhir/gate-file path {:artifacts @lockfile-artifacts :out-dir work-dir})]
    (when-not (kernel/ok? r) (throw (ex-info "pairing-conviction-fhir: gate failed" r)))
    (->> (:findings (:payload r)) (map :code) set)))

(deftest ^:integration every-fhir-registry-row-witnesses-its-own-expected-class-test
  (doseq [{:keys [operator judge expected] :as row} (fhir-rows)]
    (testing (str (:id operator) " x " judge)
      (let [path (write-mutant! row (mutant-content row))
            observed (gate-mutant path)]
        (is (some expected observed)
            (str "expected one of " expected " among observed classes " observed))))))
