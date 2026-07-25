(ns ehr-testing-tools.baseline-gating-test
  "Baseline-relative gating (P6) against the real official FHIR
  validator, not simulated -- the same EXP-C5-motivated scenario
  documented in docs/judge-calibration.md, proven end to end: a
  Synthea-generated R4 file already carries hundreds of profile-driven
  findings before any mutation (US Core, auto-loaded from the file's
  own declared meta.profile). Absolute verdict is :rejected for that
  reason alone; baseline-relative verdict against a baseline captured
  from the same unmutated file is :pass -- nothing NEW appeared. Once
  a real mutation is applied, the same baseline no longer covers it,
  and the relative verdict correctly flips to :rejected.

  Self-sufficient (generates its own fresh fixture), matching
  contract_pairing_test.clj's own discipline -- never relies on
  another experiment's leftover, gitignored out/ directory being
  present. Tagged ^:integration; run explicitly via `make integration`
  (requires `ehr artifact fetch` for synthea/temurin-jdk/
  fhir-validator-cli first)."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.artifact :as artifact]
            [ehr-testing-tools.corpus.generate :as generate]
            [ehr-testing-tools.corpus.mutate :as mutate]
            [ehr-testing-tools.corpus.operators :as operators]
            [ehr-testing-tools.judge.fhir :as gate]
            [ehr-testing-tools.judge.report :as report])
  (:import [java.io File]))

(def ^:private work-dir "target/baseline-gating")
(def ^:private lockfile-artifacts
  (delay (:artifacts (:payload (artifact/read-lockfile "artifacts.lock.edn")))))

(def ^:private base-file (atom nil))

(defn- select-patient-file
  [corpus-dir]
  (->> (.listFiles (io/file corpus-dir "fhir"))
       (filter #(and (str/ends-with? (.getName ^File %) ".json")
                     (not (re-find #"hospitalInformation|practitionerInformation" (.getName ^File %)))))
       first))

(defn- generate-fixture!
  [f]
  (let [corpus-dir (str work-dir "/corpus")
        gen-result (generate/generate! {:config-path "config/synthea/synthea.properties"
                                         :seed 100 :clinician-seed 555 :population 1
                                         :reference-date "20260101" :output-dir corpus-dir})]
    (if-not (result/ok? gen-result)
      (throw (ex-info "baseline-gating fixture: corpus generation failed -- run `ehr artifact fetch` for synthea/temurin-jdk first" gen-result))
      (reset! base-file (.getAbsolutePath (select-patient-file corpus-dir)))))
  (f))

(use-fixtures :once generate-fixture!)

(defn- gate-one!
  [path]
  (let [r (gate/gate-file path {:artifacts @lockfile-artifacts :out-dir work-dir})]
    (when-not (result/ok? r)
      (throw (ex-info "baseline-gating: gate failed" r)))
    (:payload r)))

(deftest ^:integration unmutated-file-is-absolute-rejected-but-relative-pass-against-its-own-baseline-test
  ;; EXP-C5's own finding, proven live: the unmutated file already
  ;; carries pre-existing US-Core profile findings -- absolute verdict
  ;; is :rejected for that reason alone. A baseline captured from
  ;; gating the SAME unmutated file again introduces nothing novel.
  (let [outcome (gate-one! @base-file)
        baseline (report/build-report [(assoc outcome :path "patient.json")] {:gate :fhir})
        current (assoc outcome :path "patient.json")
        br (report/baseline-relative-report [current] {:gate :fhir} baseline)]
    (is (= :rejected (:verdict outcome))
        "sanity: the unmutated file is genuinely rejected absolutely (EXP-C5 profile noise)")
    (is (= :rejected (:verdict (first (:files (:absolute br))))))
    (is (= :pass (:verdict (first (:files (:relative br)))))
        "nothing NEW relative to a baseline captured from the same unmutated file")))

(deftest ^:integration a-real-mutation-is-still-rejected-relative-to-the-unmutated-baseline-test
  (let [baseline-outcome (gate-one! @base-file)
        baseline (report/build-report [(assoc baseline-outcome :path "patient.json")] {:gate :fhir})
        base-data (json/read-str (slurp @base-file))
        operator (operators/lookup :duplicate-element "1")
        mutate-result (mutate/mutate base-data operator {:format :fhir :path "entry[0].resource.gender"})
        _ (when-not (result/ok? mutate-result)
            (throw (ex-info "baseline-gating: mutate failed" mutate-result)))
        mutant (:mutant (:payload mutate-result))
        mutant-path (str work-dir "/mutant.json")
        _ (io/make-parents mutant-path)
        _ (spit mutant-path (json/write-str mutant))
        mutant-outcome (gate-one! mutant-path)
        current (assoc mutant-outcome :path "patient.json")
        br (report/baseline-relative-report [current] {:gate :fhir} baseline)]
    (is (= :rejected (:verdict (first (:files (:relative br)))))
        "the duplicate-element mutation introduces a genuinely new finding, not covered by the unmutated baseline")))
