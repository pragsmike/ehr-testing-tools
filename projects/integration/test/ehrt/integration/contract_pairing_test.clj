(ns ehrt.integration.contract-pairing-test
  "The test this whole architecture has been building toward (P5): for
  each of the five FHIR defect operators, mutate a real Synthea R4
  file, gate the mutant through the real official validator
  (fhir-validator-cli, a real subprocess -- not an injected fake),
  and assert the gate's response matches the operator's own contract.
  Lives on the `test-integration/` path, which neither the :test nor
  the :coverage alias includes -- AGENTS.md's hermetic-test-suite rule
  is a path split, not a tag filter, so `^:integration` here documents
  *why* this suite sits apart rather than being what excludes it. This
  is the first suite in the repo that genuinely needs a real external
  engine; every other test in the repo goes through an injected fake.
  Run explicitly with `make integration`, after `ehr artifact fetch`
  for synthea, temurin-jdk, and fhir-validator-cli.

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
  end-to-end, not re-measuring offline behavior.

  Batched (ADR-0016 ruling 4, F29 in notes/facts-register.md, this
  session): all five mutants are built up front and gated with ONE
  `judge.fhir/gate-batch` call in the :once fixture below, instead of
  five separate `gate-file` subprocess launches -- the validator's own
  fixed terminology/package-load cost (measured ~25s, independent of
  file count) was previously paid five times, once per contract test;
  batching pays it once for the whole suite. Each deftest below reads
  its own pre-computed outcome from `mutant-outcomes` rather than
  gating anything itself -- the polarity assertions this suite makes
  (a specific locator-matching finding, per file) are exactly what
  `gate-batch`'s own per-file attribution (matched by exact argv
  string, never position) preserves, which is what ruling 4 means by
  \"where polarity assertions permit.\""
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
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

(def ^:private work-dir "target/contract-pairing")
(def ^:private lockfile-artifacts
  (delay (:artifacts (:payload (artifact/read-lockfile "artifacts.lock.edn")))))

(def ^:private base-file (atom nil))

;; operator-id -> {:mutant-path :outcome}, populated once by
;; generate-fixture! below -- every deftest reads from here.
(def ^:private mutant-outcomes (atom nil))

(def ^:private mutant-specs
  "[operator-id locator-path], in the order each contract test below
  needs its own mutant -- the SAME five (operator, locator) pairings
  this suite has always used, now built and gated together."
  [[:duplicate-element "entry[0].resource.gender"]
   [:malformed-date "entry[0].resource.birthDate"]
   [:wrong-type-value "entry[0].resource.multipleBirthBoolean"]
   [:remove-required-element "entry[0].resource.resourceType"]
   [:invalid-code-value "entry[0].resource.gender"]])

(defn- select-patient-file
  [corpus-dir]
  (->> (.listFiles (io/file corpus-dir "fhir"))
       (filter #(and (str/ends-with? (.getName ^File %) ".json")
                     (not (re-find #"hospitalInformation|practitionerInformation" (.getName ^File %)))))
       first))

(defn- mutate!
  "Mutates @base-file at locator-path with operator-id, writes the
  mutant to its own path, and returns that path. Does not gate --
  gating happens once, batched, for every spec together (below)."
  [operator-id locator-path]
  (let [base-data (json/read-str (slurp @base-file))
        operator (operators/operator-lookup operator-id "1")
        mutate-result (mutate/mutate base-data operator {:format :fhir :path locator-path})]
    (when-not (result/ok? mutate-result)
      (throw (ex-info "contract-pairing: mutate failed" mutate-result)))
    (let [mutant (:mutant (:payload mutate-result))
          mutant-path (str work-dir "/" (name operator-id) "-mutant.json")]
      (io/make-parents mutant-path)
      (spit mutant-path (json/write-str mutant))
      mutant-path)))

(defn- delete-tree!
  "Clears a fixture's own last run before regenerating into it --
  corpus.generate!'s :out-dir-exists guard (Step 4, the determinism
  probe's 2026-07-28 finding) now rejects a rerun into a non-empty
  directory, so this suite's own fixed work-dir is no longer safely
  re-runnable without this."
  [^File f]
  (when (.exists f)
    (doseq [child (reverse (file-seq f))] (.delete ^File child))))

(defn- generate-fixture!
  "Once per suite run: a fresh, tiny, deterministic Synthea R4 corpus
  (EXP-A4/EXP-B2/EXP-C5's pinned settings -- seed 100, clinician-seed
  555, reference-date 20260101), so this suite is self-sufficient
  (never relies on another experiment's leftover, gitignored out/
  directory being present) -- then every mutant in mutant-specs is
  built and gated together in ONE gate-batch call (ADR-0016 ruling 4)."
  [f]
  (let [corpus-dir (str work-dir "/corpus")
        _ (delete-tree! (io/file corpus-dir))
        gen-result (generate/generate! {:config-path "config/synthea/synthea.properties"
                                         :seed 100 :clinician-seed 555 :population 1
                                         :reference-date "20260101" :out-dir corpus-dir})]
    (if-not (result/ok? gen-result)
      (throw (ex-info "contract-pairing fixture: corpus generation failed -- run `ehr artifact fetch` for synthea/temurin-jdk first" gen-result))
      (reset! base-file (.getAbsolutePath (select-patient-file corpus-dir))))
    (let [mutant-paths (mapv (fn [[operator-id locator-path]] (mutate! operator-id locator-path)) mutant-specs)
          batch-result (gate/gate-batch mutant-paths {:artifacts @lockfile-artifacts :out-dir work-dir})]
      (when-not (result/ok? batch-result)
        (throw (ex-info "contract-pairing: gate-batch failed" batch-result)))
      (reset! mutant-outcomes
              (into {} (map (fn [[operator-id _] path outcome] [operator-id {:mutant-path path :outcome outcome}])
                             mutant-specs mutant-paths (:results (:payload batch-result)))))))
  (f))

(use-fixtures :once generate-fixture!)

(defn- outcome-for
  [operator-id]
  (get @mutant-outcomes operator-id))

(defn- finding-matching-locator
  [findings locator-suffix]
  (filter #(str/ends-with? (:path (:locator %)) locator-suffix) findings))

;; ---- structurally detectable contracts: expect :rejected, with a
;; new finding whose locator matches the mutation's own locator and
;; whose code is in a small expected set derived from EXP-C5's
;; observations ----

(deftest ^:integration duplicate-element-contract-test
  (let [{:keys [outcome]} (outcome-for :duplicate-element)
        matches (finding-matching-locator (:findings outcome) "entry[0].resource.gender")]
    (is (= :rejected (:verdict outcome)))
    (is (seq matches) "expected a finding whose locator matches the mutation's own locator")
    (is (some #(= "invalid" (:code %)) matches))
    (is (some #(= :rejected (:disposition %)) matches))))

(deftest ^:integration malformed-date-contract-test
  (let [{:keys [outcome]} (outcome-for :malformed-date)
        matches (finding-matching-locator (:findings outcome) "entry[0].resource.birthDate")]
    (is (= :rejected (:verdict outcome)))
    (is (seq matches))
    (is (some #(= "invalid" (:code %)) matches))
    (is (some #(= :rejected (:disposition %)) matches))))

(deftest ^:integration wrong-type-value-contract-test
  (let [{:keys [outcome]} (outcome-for :wrong-type-value)
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
  (let [{:keys [outcome]} (outcome-for :remove-required-element)
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
  (let [{:keys [outcome]} (outcome-for :invalid-code-value)
        matches (finding-matching-locator (:findings outcome) "entry[0].resource.gender")]
    (is (= :rejected (:verdict outcome))
        "OBSERVED class (EXP-C5): detected, not indeterminate -- AdministrativeGender is base-bundled")
    (is (seq matches))
    (is (some #(#{"code-invalid" "not-found"} (:code %)) matches))
    (is (some #(= :rejected (:disposition %)) matches))))

;; ---- the Judge stage kind law (docs/notation.md): gating never
;; modifies the datum it judges -- tested here against the REAL
;; engine, not just the unit-level fakes judge.fhir's own test suite
;; already covers this with. Re-gates the already-batched
;; duplicate-element mutant through gate-file directly -- the verdict
;; cache (ADR-0016 ruling 3) makes this a cache hit, not a second
;; subprocess launch, since gate-batch stored this same file's result
;; under the same key gate-file looks up. ----

(deftest ^:integration gate-fhir-never-modifies-its-input-test
  (let [{:keys [mutant-path]} (outcome-for :duplicate-element)
        before (slurp mutant-path)
        _ (gate/gate-file mutant-path {:artifacts @lockfile-artifacts :out-dir work-dir})
        after (slurp mutant-path)]
    (is (= before after))))
