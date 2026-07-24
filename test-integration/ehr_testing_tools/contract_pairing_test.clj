(ns ehr-testing-tools.contract-pairing-test
  "The test this whole architecture has been building toward (P5): for
  each of the five FHIR defect operators, mutate a real Synthea R4
  file, gate the mutant through the real official validator
  (fhir-validator-cli, a real subprocess -- not an injected fake),
  and assert the gate's response matches the operator's own contract.
  Tagged ^:integration and excluded from the default `make test` run
  (deps.edn's :test alias, AGENTS.md's hermetic-test-suite rule) --
  this is the first suite in the repo that genuinely needs a real
  external engine; every other test in the repo goes through an
  injected fake. Run explicitly with `clojure -X:test :excludes '[]'`.

  Honest classification, not uniform \"rejected\": EXP-C5
  (docs/experiments/EXP-C5-results.md) already found that
  remove-required-element against Patient.gender detects nothing
  (gender is min-cardinality 0 in base FHIR, not actually required),
  so this suite uses Patient.resourceType instead -- a genuinely
  required locator, per EXP-C5's own addendum. It also found that
  invalid-code-value against Patient.gender IS detected offline
  (AdministrativeGender is a base-FHIR-bundled ValueSet, not
  terminology-server-dependent) -- contrary to the a-priori hypothesis
  that this defect class would be terminology-suppressed; the
  observed class is asserted here, not the anticipated one.

  Assertions match on the *specific new finding* the mutation
  introduces, never on the file's aggregate verdict alone: EXP-C5
  found every 'valid' baseline file in this corpus already carries
  hundreds of profile-driven errors (the validator auto-loads US Core
  from Synthea's own declared meta.profile), so aggregate verdict
  can't discriminate valid from mutant here -- only a specific,
  locator-matching, newly-appeared finding can. No network isolation
  is applied in this suite (unlike EXP-C5's own execution, which used
  unshare -r -n specifically to *measure* offline behavior) -- the
  FHIR package cache primed during EXP-C5 makes these runs fast
  either way, and this suite's job is proving detection is wired
  end-to-end, not re-measuring offline behavior."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.artifact :as artifact]
            [ehr-testing-tools.corpus.generate :as generate]
            [ehr-testing-tools.corpus.mutate :as mutate]
            [ehr-testing-tools.corpus.operators :as operators]
            [ehr-testing-tools.gate.fhir :as gate])
  (:import [java.io File]))

(def ^:private work-dir "target/contract-pairing")
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
  "Once per suite run: a fresh, tiny, deterministic Synthea R4 corpus
  (EXP-A4/EXP-B2/EXP-C5's pinned settings -- seed 100, clinician-seed
  555, reference-date 20260101), so this suite is self-sufficient
  (never relies on another experiment's leftover, gitignored out/
  directory being present)."
  [f]
  (let [corpus-dir (str work-dir "/corpus")
        gen-result (generate/generate! {:config-path "config/synthea/synthea.properties"
                                         :seed 100 :clinician-seed 555 :population 1
                                         :reference-date "20260101" :output-dir corpus-dir})]
    (if-not (result/ok? gen-result)
      (throw (ex-info "contract-pairing fixture: corpus generation failed -- run `ehr artifact fetch` for synthea/temurin-jdk first" gen-result))
      (reset! base-file (.getAbsolutePath (select-patient-file corpus-dir)))))
  (f))

(use-fixtures :once generate-fixture!)

(defn- mutate-and-gate!
  "Mutates @base-file at locator-path with operator-id, writes the
  mutant, gates it through the real validator. Returns {:mutant-path
  :outcome}."
  [operator-id locator-path]
  (let [base-data (json/read-str (slurp @base-file))
        operator (operators/lookup operator-id "1")
        mutate-result (mutate/mutate base-data operator {:format :fhir :path locator-path})]
    (when-not (result/ok? mutate-result)
      (throw (ex-info "contract-pairing: mutate failed" mutate-result)))
    (let [mutant (:mutant (:payload mutate-result))
          mutant-path (str work-dir "/" (name operator-id) "-mutant.json")
          _ (io/make-parents mutant-path)
          _ (spit mutant-path (json/write-str mutant))
          gate-result (gate/gate-file mutant-path {:artifacts @lockfile-artifacts :out-dir work-dir})]
      (when-not (result/ok? gate-result)
        (throw (ex-info "contract-pairing: gate failed" gate-result)))
      {:mutant-path mutant-path :outcome (:payload gate-result)})))

(defn- finding-matching-locator
  [findings locator-suffix]
  (filter #(str/ends-with? (:path (:locator %)) locator-suffix) findings))

;; ---- structurally detectable contracts: expect :rejected, with a
;; new finding whose locator matches the mutation's own locator and
;; whose code is in a small expected set derived from EXP-C5's
;; observations ----

(deftest ^:integration duplicate-element-contract-test
  (let [{:keys [outcome]} (mutate-and-gate! :duplicate-element "entry[0].resource.gender")
        matches (finding-matching-locator (:findings outcome) "entry[0].resource.gender")]
    (is (= :rejected (:verdict outcome)))
    (is (seq matches) "expected a finding whose locator matches the mutation's own locator")
    (is (some #(= "invalid" (:code %)) matches))
    (is (some #(= :rejected (:policy %)) matches))))

(deftest ^:integration malformed-date-contract-test
  (let [{:keys [outcome]} (mutate-and-gate! :malformed-date "entry[0].resource.birthDate")
        matches (finding-matching-locator (:findings outcome) "entry[0].resource.birthDate")]
    (is (= :rejected (:verdict outcome)))
    (is (seq matches))
    (is (some #(= "invalid" (:code %)) matches))
    (is (some #(= :rejected (:policy %)) matches))))

(deftest ^:integration wrong-type-value-contract-test
  (let [{:keys [outcome]} (mutate-and-gate! :wrong-type-value "entry[0].resource.multipleBirthBoolean")
        ;; the validator addresses this choice-type element as
        ;; \"multipleBirth[x]\" in its own expression syntax, not the
        ;; JSON-serialized field name -- match on the resource-level
        ;; path prefix instead of the exact field name.
        matches (finding-matching-locator (:findings outcome) "entry[0].resource.multipleBirth[x]")]
    (is (= :rejected (:verdict outcome)))
    (is (seq matches))
    (is (some #(= "invalid" (:code %)) matches))))

(deftest ^:integration remove-required-element-contract-test
  ;; EXP-C5's addendum: Patient.gender is min-cardinality 0 in base
  ;; FHIR -- removing it detects nothing, an operator/locator-choice
  ;; finding, not a gate limitation. Patient.resourceType IS
  ;; genuinely required (min=1 on every FHIR resource); this contract
  ;; test uses that locator so remove-required-element's own
  ;; :violates contract is tested honestly.
  (let [{:keys [outcome]} (mutate-and-gate! :remove-required-element "entry[0].resource.resourceType")
        matches (finding-matching-locator (:findings outcome) "entry[0].resource")]
    (is (= :rejected (:verdict outcome)))
    (is (seq matches) "expected a finding at/near the resource whose resourceType was removed")
    (is (some #(#{"invalid" "structure"} (:code %)) matches))
    (is (some #(#{:fatal :error} (:severity %)) matches)
        "FHIR's IssueSeverity defines both fatal and error; the validator uses fatal here")))

;; ---- terminology-dependent contract: assert the OBSERVED class,
;; documented honestly even where it contradicts the a-priori
;; hypothesis ----

(deftest ^:integration invalid-code-value-contract-test
  ;; Undetectable-at-this-tier does NOT hold for every code violation:
  ;; EXP-C5 found Patient.gender is bound to AdministrativeGender, a
  ;; small ValueSet bundled with the base FHIR core package -- checkable
  ;; fully offline, with no terminology server. This defect IS detected
  ;; at the base/offline tier for this specific locator, contrary to
  ;; the general expectation that invalid-code-value is terminology-
  ;; suppressed offline. A locator bound to a terminology-server-
  ;; dependent code system (e.g. a LOINC- or SNOMED-bound
  ;; Observation.code) would plausibly land :indeterminate instead --
  ;; untested here, out of this session's scope (no such locator in
  ;; the single-patient fixture this suite generates).
  (let [{:keys [outcome]} (mutate-and-gate! :invalid-code-value "entry[0].resource.gender")
        matches (finding-matching-locator (:findings outcome) "entry[0].resource.gender")]
    (is (= :rejected (:verdict outcome))
        "OBSERVED class (EXP-C5): detected, not indeterminate -- AdministrativeGender is base-bundled")
    (is (seq matches))
    (is (some #(#{"code-invalid" "not-found"} (:code %)) matches))
    (is (some #(= :rejected (:policy %)) matches))))

;; ---- the Gate stage kind law (docs/notation.md): gating never
;; modifies the datum it judges -- tested here against the REAL
;; engine, not just the unit-level fakes gate.fhir's own test suite
;; already covers this with ----

(deftest ^:integration gate-fhir-never-modifies-its-input-test
  (let [{:keys [mutant-path]} (mutate-and-gate! :duplicate-element "entry[0].resource.gender")
        before (slurp mutant-path)
        _ (gate/gate-file mutant-path {:artifacts @lockfile-artifacts :out-dir work-dir})
        after (slurp mutant-path)]
    (is (= before after))))
