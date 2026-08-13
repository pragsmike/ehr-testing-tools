(ns ehrt.judge-fhir-official.fhir-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [ehrt.kernel.interface :as kernel]
            [ehrt.judge.finding :as finding]
            [ehrt.judge-fhir-official.fhir :as gate])
  (:import [java.io File]))

(defn- temp-dir []
  (let [f (File/createTempFile "gate-fhir-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

(def validator-artifact
  {:kind :engine :name "fhir-validator-cli" :version "6.9.12"
   :sha256 (apply str (repeat 64 "c"))
   :source "https://example.invalid/validator_cli.jar"
   :acquired "2026-07-24" :license-status :verified})

;; ---- fixture OperationOutcome issues, one per distinct {severity,
;; code} category EXP-C5 actually observed (docs/experiments/EXP-C5-
;; results.md) -- diagnostics text copied verbatim from that
;; classification table, not invented, so these tests exercise the
;; real terminology-suppression-detection patterns against real
;; observed strings. ----

(defn- issue
  [severity code expression details-text]
  {"severity" severity "code" code "expression" [expression]
   "details" {"text" details-text}})

(def genuine-structure-error
  (issue "error" "structure" "Bundle.entry[0].resource/*Patient/u*/.extension[5]"
         "The extension http://synthetichealth.github.io/synthea/disability-adjusted-life-years could not be found so is not allowed here"))

(def genuine-invalid-error
  (issue "error" "invalid" "Bundle.entry[0].resource/*Patient/u*/.birthDate"
         "Not a valid date format: '2026-13-45'"))

(def genuine-code-invalid-error
  (issue "error" "code-invalid" "Bundle.entry[0].resource/*Patient/u*/.gender"
         "The value provided ('not-a-valid-code-9f3a1c') was not found in the value set 'AdministrativeGender' (http://hl7.org/fhir/ValueSet/administrative-gender|4.0.1), and a code is required from this value set"))

(def terminology-suppressed-warning
  (issue "warning" "code-invalid" "Bundle.entry[0].resource/*Patient/u*/.extension[0].extension[0].value.ofType(Coding)"
         "Unable to validate code without using server because: Resolved system urn:oid:2.16.840.1.113883.6.238 (v3.0.2), but the definition doesn't include any codes, so the code has not been validated"))

(def terminology-suppressed-information
  (issue "information" "unknown" "Bundle.entry[0].resource/*Patient/u*/.extension[0].extension[0].value.ofType(Coding)"
         "The definition for the Code System with URI 'urn:oid:2.16.840.1.113883.6.238' from 'hl7.terminology.r4#6.2.0' doesn't provide any codes so the code cannot be validated"))

(def advisory-warning
  (issue "warning" "invariant" "Bundle.entry[1].resource/*Encounter/u*/"
         "Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)"))

(def advisory-information
  (issue "information" "structure" "Bundle.entry[1].resource/*Encounter/u*/.subject"
         "Details for urn:uuid:u matching against profile http://hl7.org/fhir/StructureDefinition/Patient|4.0.1"))

(def genuine-fatal-error
  ;; Found via P5's Step 6 contract-pairing exercise, not the original
  ;; EXP-C5 corpus: removing Bundle.entry[0].resource.resourceType
  ;; produces this, at severity "fatal" (not "error") -- FHIR's own
  ;; IssueSeverity ValueSet defines both.
  (issue "fatal" "invalid" "Bundle.entry[0].resource" "Unable to find resourceType property"))

(defn- outcome
  [& issues]
  {"resourceType" "OperationOutcome" "issue" (vec issues)})

(def sample-engine {:name "fhir-validator-cli" :version "6.9.12"})

;; ---- interpret: pure, versioned, the EXP-C5-derived verdict-mapping
;; table ----

(deftest interpret-no-issues-is-pass-test
  (let [o (gate/interpret (outcome) sample-engine)]
    (is (= :pass (:verdict o)))
    (is (= [] (:findings o)))))

(deftest interpret-genuine-error-is-rejected-test
  (let [o (gate/interpret (outcome genuine-structure-error) sample-engine)]
    (is (= :rejected (:verdict o)))
    (is (= 1 (count (:findings o))))
    (is (finding/valid? (first (:findings o))))
    (is (= :error (:severity (first (:findings o)))))))

(deftest interpret-fatal-severity-is-rejected-like-error-test
  (let [o (gate/interpret (outcome genuine-fatal-error) sample-engine)]
    (is (= :rejected (:verdict o)))
    (is (= :fatal (:severity (first (:findings o)))))
    (is (= :rejected (:disposition (first (:findings o)))))
    (is (not (contains? (first (:findings o)) :cause))
        "a :rejected finding carries no :cause -- only :no-verdict does")))

(deftest interpret-genuine-invalid-and-code-invalid-errors-are-rejected-test
  (testing "structural type/format violation"
    (is (= :rejected (:verdict (gate/interpret (outcome genuine-invalid-error) sample-engine)))))
  (testing "a code violating a base-FHIR-bundled ValueSet (AdministrativeGender) -- NOT terminology-suppressed"
    (let [o (gate/interpret (outcome genuine-code-invalid-error) sample-engine)]
      (is (= :rejected (:verdict o)))
      (is (= :rejected (:disposition (first (:findings o))) )
          "the finding itself should record which mapping-policy classified it, for auditability")))
  )

(deftest interpret-terminology-suppressed-issues-are-no-verdict-test
  ;; tools/ADR-0010/O2: terminology-suppressed is no-verdict(:terminology-
  ;; suppressed) -- the judge failed to fully apply the criterion; the
  ;; criterion didn't fail to decide. Formerly :indeterminate.
  (testing "warning-severity, 'without using server' diagnostics"
    (let [o (gate/interpret (outcome terminology-suppressed-warning) sample-engine)]
      (is (= :no-verdict (:verdict o)))
      (is (= :terminology-suppressed (:cause o)))
      (is (= :no-verdict (:disposition (first (:findings o)))))
      (is (= :terminology-suppressed (:cause (first (:findings o)))))
      (is (finding/valid-cause-pairing? (:disposition (first (:findings o)))
                                         (:cause (first (:findings o)))))))
  (testing "information-severity, 'doesn't provide any codes' diagnostics"
    (let [o (gate/interpret (outcome terminology-suppressed-information) sample-engine)]
      (is (= :no-verdict (:verdict o)))
      (is (= :terminology-suppressed (:cause o))))))

(deftest interpret-a-file-whose-findings-are-all-no-verdict-aggregates-to-no-verdict-test
  ;; Pinning test at the judge level (not just worst-of directly): a
  ;; real outcome whose every issue is terminology-suppressed -- no
  ;; :pass-worthy findings at all -- must not be mistaken for the
  ;; no-issues-at-all (empty findings, :pass) case.
  (let [o (gate/interpret (outcome terminology-suppressed-warning terminology-suppressed-information) sample-engine)]
    (is (= 2 (count (:findings o))))
    (is (= :no-verdict (:verdict o)))
    (is (= :terminology-suppressed (:cause o)))))

(deftest interpret-advisory-warning-and-information-are-pass-with-findings-test
  (let [o (gate/interpret (outcome advisory-warning advisory-information) sample-engine)]
    (is (= :pass (:verdict o)))
    (is (= 2 (count (:findings o))))))

(deftest interpret-worst-of-across-mixed-issues-test
  ;; :rejected still dominates the aggregate over an incidental
  ;; terminology-suppressed finding elsewhere in the same file -- the
  ;; revised ranking (judge/finding.clj), not the fourth arm's original
  ;; above-:rejected draft, which made a real corpus's genuine
  ;; violations invisible at the file level (Step 5 integration finding).
  (let [o (gate/interpret (outcome advisory-warning genuine-structure-error terminology-suppressed-warning) sample-engine)]
    (is (= :rejected (:verdict o)) "rejected beats no-verdict beats indeterminate beats pass")
    (is (not (contains? o :cause)))
    (is (= 3 (count (:findings o))))))

(deftest interpret-worst-of-no-verdict-beats-pass-when-nothing-is-rejected-test
  (let [o (gate/interpret (outcome advisory-warning terminology-suppressed-warning) sample-engine)]
    (is (= :no-verdict (:verdict o)))
    (is (= :terminology-suppressed (:cause o)))
    (is (= 2 (count (:findings o))))))

(deftest interpret-locator-strips-the-validator-own-type-disambiguation-test
  ;; The validator's own expression syntax embeds
  ;; \"/*ResourceType/id*/\" inline; interpret normalizes it away so the
  ;; finding's locator is directly comparable to a mutation's own
  ;; locator (\"entry[0].resource.gender\", not
  ;; \"entry[0].resource/*Patient/uuid*/.gender\").
  (let [o (gate/interpret (outcome genuine-code-invalid-error) sample-engine)
        loc (:locator (first (:findings o)))]
    (is (= :fhir (:format loc)))
    (is (clojure.string/ends-with? (:path loc) "entry[0].resource.gender"))
    (is (not (clojure.string/includes? (:path loc) "/*")))))

(deftest interpret-finding-carries-engine-and-native-ref-test
  (let [o (gate/interpret (outcome genuine-structure-error) sample-engine)
        f (first (:findings o))]
    (is (= sample-engine (:engine f)))
    (is (some? (:native-ref f)))))

;; ---- execute: two-step engine, subprocess wrapper (injectable, per
;; corpus.generate's own testing convention -- no real subprocess in
;; the hermetic suite) ----

(defn- ok-invocation []
  (kernel/ok {:command "java" :args ["-jar" "validator_cli.jar"]
              :exit-code 0 :duration-ms 42 :started-at "2026-07-24T00:00:00Z"
              :stdout-path "/fake/out.log" :stderr-path "/fake/err.log"
              :stdout-sha256 (apply str (repeat 64 "0"))
              :stderr-sha256 (apply str (repeat 64 "0"))}))

(deftest execute-happy-path-reads-back-the-outcome-file-test
  (let [out-dir (temp-dir)
        input-path (str out-dir "/input.json")
        _ (spit input-path "{\"resourceType\":\"Bundle\"}")
        outcome-json "{\"resourceType\":\"OperationOutcome\",\"issue\":[]}"
        run-invocation (fn [{:keys [args]}]
                         ;; the real validator writes -output=<path>;
                         ;; the fake mimics that side effect so
                         ;; execute's read-back has something real to
                         ;; read, exactly like corpus.generate's stubs
                         ;; simulate Synthea writing its output tree.
                         (let [output-arg (first (filter #(clojure.string/starts-with? % "-output=") args))
                               output-path (subs output-arg (count "-output="))]
                           (spit output-path outcome-json))
                         (ok-invocation))
        r (gate/execute {:input-path input-path
                          :artifacts [validator-artifact]
                          :java-bin "/fake/java"
                          :out-dir out-dir
                          :run-invocation run-invocation
                          :resolve-artifact (fn [_artifacts _name _version]
                                              (kernel/ok {:path "/fake/validator_cli.jar" :artifact validator-artifact}))})]
    (is (kernel/ok? r))
    (is (= {"resourceType" "OperationOutcome" "issue" []} (:raw-outcome (:payload r))))
    (is (= "fhir-validator-cli" (:name (:engine (:payload r)))))
    (is (= "6.9.12" (:version (:engine (:payload r)))))))

(deftest execute-invokes-with-tx-n-a-and-base-r4-version-test
  (let [out-dir (temp-dir)
        input-path (str out-dir "/input.json")
        _ (spit input-path "{}")
        captured-args (atom nil)
        run-invocation (fn [{:keys [args]}]
                         (reset! captured-args args)
                         (let [output-arg (first (filter #(clojure.string/starts-with? % "-output=") args))]
                           (spit (subs output-arg (count "-output=")) "{\"resourceType\":\"OperationOutcome\",\"issue\":[]}"))
                         (ok-invocation))]
    (gate/execute {:input-path input-path :artifacts [validator-artifact] :java-bin "/fake/java"
                   :out-dir out-dir :run-invocation run-invocation
                   :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/validator_cli.jar" :artifact validator-artifact}))})
    (is (clojure.string/includes? (clojure.string/join " " @captured-args) "-version 4.0"))
    (is (clojure.string/includes? (clojure.string/join " " @captured-args) "-tx n/a"))))

(deftest execute-propagates-artifact-resolve-failure-test
  (let [r (gate/execute {:input-path "x.json" :artifacts []
                         :resolve-artifact (fn [_ _ _] (kernel/rejected :not-cached {}))})]
    (is (kernel/rejected? r))
    (is (= :not-cached (:category r)))))

(deftest execute-propagates-invocation-failure-test
  (let [out-dir (temp-dir)
        r (gate/execute {:input-path "x.json" :artifacts [validator-artifact]
                         :java-bin "/fake/java" :out-dir out-dir
                         :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/validator_cli.jar" :artifact validator-artifact}))
                         :run-invocation (fn [_] (kernel/error :spawn-failed {:message "no java"}))})]
    (is (kernel/error? r))
    (is (= :spawn-failed (:category r)))))

;; ---- IG machinery: -ig wiring, unit tested with a stub (no IG
;; pinned in artifacts.lock.edn this session) ----

(deftest execute-wires-ig-refs-as-dash-ig-args-test
  (let [out-dir (temp-dir)
        input-path (str out-dir "/input.json")
        _ (spit input-path "{}")
        captured-args (atom nil)
        run-invocation (fn [{:keys [args]}]
                         (reset! captured-args args)
                         (let [output-arg (first (filter #(clojure.string/starts-with? % "-output=") args))]
                           (spit (subs output-arg (count "-output=")) "{\"resourceType\":\"OperationOutcome\",\"issue\":[]}"))
                         (ok-invocation))
        ig-artifact {:kind :profile :name "some.ig" :version "1.0.0"
                     :sha256 (apply str (repeat 64 "d")) :source "https://example.invalid/ig.tgz"
                     :acquired "2026-07-24" :license-status :verified}]
    (gate/execute {:input-path input-path :artifacts [validator-artifact ig-artifact]
                   :java-bin "/fake/java" :out-dir out-dir :run-invocation run-invocation
                   :ig-refs [{:name "some.ig" :version "1.0.0"}]
                   :resolve-artifact (fn [artifacts name version]
                                      (kernel/ok {:path (str "/fake/" name "-" version)
                                                  :artifact (first (filter #(= name (:name %)) artifacts))}))})
    (is (some #{"-ig"} @captured-args))
    (is (some #{"/fake/some.ig-1.0.0"} @captured-args))))

(deftest execute-propagates-ig-resolve-failure-test
  (let [out-dir (temp-dir)
        input-path (str out-dir "/input.json")
        _ (spit input-path "{}")]
    (let [r (gate/execute {:input-path input-path :artifacts [validator-artifact]
                           :java-bin "/fake/java" :out-dir out-dir
                           :ig-refs [{:name "missing.ig" :version "1.0.0"}]
                           :resolve-artifact (fn [artifacts name version]
                                              (if (= name "fhir-validator-cli")
                                                (kernel/ok {:path "/fake/validator_cli.jar" :artifact validator-artifact})
                                                (kernel/rejected :unknown-artifact {:name name :version version})))})]
      (is (kernel/rejected? r))
      (is (= :unknown-artifact (:category r))))))

;; ---- gate-file: read (never mutate) -> execute -> interpret ----

(deftest gate-file-does-not-modify-its-input-test
  (let [out-dir (temp-dir)
        input-path (str out-dir "/input.json")
        content "{\"resourceType\":\"Bundle\",\"entry\":[]}"
        _ (spit input-path content)
        run-invocation (fn [{:keys [args]}]
                         (let [output-arg (first (filter #(clojure.string/starts-with? % "-output=") args))]
                           (spit (subs output-arg (count "-output=")) "{\"resourceType\":\"OperationOutcome\",\"issue\":[]}"))
                         (ok-invocation))
        before (slurp input-path)
        ;; :verdict-cache-dir scoped under this test's own fresh out-dir
        ;; -- otherwise two tests gating byte-identical fixture content
        ;; ("{}", "{\"resourceType\":...}") through the same fake
        ;; validator-artifact would collide on the same cache key and
        ;; silently short-circuit each other's :run-invocation fake.
        _ (gate/gate-file input-path {:artifacts [validator-artifact] :java-bin "/fake/java" :out-dir out-dir
                                       :verdict-cache-dir (str out-dir "/verdict-cache")
                                       :run-invocation run-invocation
                                       :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/v.jar" :artifact validator-artifact}))})
        after (slurp input-path)]
    (is (= before after))))

(deftest gate-file-happy-path-test
  (let [out-dir (temp-dir)
        input-path (str out-dir "/input.json")
        _ (spit input-path "{}")
        run-invocation (fn [{:keys [args]}]
                         (let [output-arg (first (filter #(clojure.string/starts-with? % "-output=") args))]
                           (spit (subs output-arg (count "-output=")) "{\"resourceType\":\"OperationOutcome\",\"issue\":[]}"))
                         (ok-invocation))
        r (gate/gate-file input-path {:artifacts [validator-artifact] :java-bin "/fake/java" :out-dir out-dir
                                       :verdict-cache-dir (str out-dir "/verdict-cache")
                                       :run-invocation run-invocation
                                       :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/v.jar" :artifact validator-artifact}))})]
    (is (kernel/ok? r))
    (is (= :pass (:verdict (:payload r))))
    (is (= input-path (:path (:payload r))))))

;; ---- verdict cache integration at gate-file (ADR-0016, session ruling
;; 3): a hit must skip execute -- and therefore the subprocess -- entirely.
;; Key-sensitivity and pure lookup/store behavior are covered at the unit
;; level in judge.verdict-cache-test; these exercise gate-file's own
;; wiring of that seam. ----

(deftest gate-file-second-call-with-identical-input-is-a-cache-hit-and-skips-the-subprocess-test
  (let [out-dir (temp-dir)
        cache-dir (str out-dir "/verdict-cache")
        input-path (str out-dir "/input.json")
        _ (spit input-path "{\"resourceType\":\"Bundle\"}")
        invocation-count (atom 0)
        run-invocation (fn [{:keys [args]}]
                         (swap! invocation-count inc)
                         (let [output-arg (first (filter #(clojure.string/starts-with? % "-output=") args))]
                           (spit (subs output-arg (count "-output=")) "{\"resourceType\":\"OperationOutcome\",\"issue\":[]}"))
                         (ok-invocation))
        opts {:artifacts [validator-artifact] :java-bin "/fake/java" :out-dir out-dir
              :verdict-cache-dir cache-dir
              :run-invocation run-invocation
              :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/v.jar" :artifact validator-artifact}))}
        r1 (gate/gate-file input-path opts)
        r2 (gate/gate-file input-path opts)]
    (is (kernel/ok? r1))
    (is (kernel/ok? r2))
    (is (= (:verdict (:payload r1)) (:verdict (:payload r2))))
    (is (= (:findings (:payload r1)) (:findings (:payload r2))))
    (is (= 1 @invocation-count) "the second gate-file call should be a cache hit, never touching :run-invocation")))

(deftest gate-file-verdict-cache-false-always-re-invokes-test
  (let [out-dir (temp-dir)
        cache-dir (str out-dir "/verdict-cache")
        input-path (str out-dir "/input.json")
        _ (spit input-path "{\"resourceType\":\"Bundle\"}")
        invocation-count (atom 0)
        run-invocation (fn [{:keys [args]}]
                         (swap! invocation-count inc)
                         (let [output-arg (first (filter #(clojure.string/starts-with? % "-output=") args))]
                           (spit (subs output-arg (count "-output=")) "{\"resourceType\":\"OperationOutcome\",\"issue\":[]}"))
                         (ok-invocation))
        opts {:artifacts [validator-artifact] :java-bin "/fake/java" :out-dir out-dir
              :verdict-cache-dir cache-dir :verdict-cache? false
              :run-invocation run-invocation
              :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/v.jar" :artifact validator-artifact}))}]
    (gate/gate-file input-path opts)
    (gate/gate-file input-path opts)
    (is (= 2 @invocation-count) "--no-verdict-cache (:verdict-cache? false) must never short-circuit the subprocess")))

(deftest gate-file-different-content-is-a-cache-miss-test
  (let [out-dir (temp-dir)
        cache-dir (str out-dir "/verdict-cache")
        path-a (str out-dir "/a.json")
        path-b (str out-dir "/b.json")
        _ (spit path-a "{\"resourceType\":\"Bundle\",\"entry\":[1]}")
        _ (spit path-b "{\"resourceType\":\"Bundle\",\"entry\":[2]}")
        invocation-count (atom 0)
        run-invocation (fn [{:keys [args]}]
                         (swap! invocation-count inc)
                         (let [output-arg (first (filter #(clojure.string/starts-with? % "-output=") args))]
                           (spit (subs output-arg (count "-output=")) "{\"resourceType\":\"OperationOutcome\",\"issue\":[]}"))
                         (ok-invocation))
        opts {:artifacts [validator-artifact] :java-bin "/fake/java" :out-dir out-dir
              :verdict-cache-dir cache-dir
              :run-invocation run-invocation
              :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/v.jar" :artifact validator-artifact}))}]
    (gate/gate-file path-a opts)
    (gate/gate-file path-b opts)
    (is (= 2 @invocation-count) "distinct file content must never collide on the same cache key")))

;; ---- gate-batch: one subprocess, many files (ADR-0016 ruling 4, F29) ----

(defn- fake-batch-invocation
  "Mimics the real validator's own multi-file contract (F29): writes a
  Bundle with one entry per positional input arg (everything in argv
  after -output=<path>), each entry's operationoutcome-file extension
  set to that EXACT arg string, and its issues drawn from issues-by-arg
  (keyed by that same exact arg string, default [])."
  [issues-by-arg]
  (fn [{:keys [args]}]
    (let [args (vec args)
          output-arg (first (filter #(str/starts-with? % "-output=") args))
          output-path (subs output-arg (count "-output="))
          output-idx (.indexOf ^java.util.List args output-arg)
          input-args (subvec args (inc output-idx))
          entries (mapv (fn [arg]
                          {"resource"
                           {"resourceType" "OperationOutcome"
                            "extension" [{"url" "http://hl7.org/fhir/StructureDefinition/operationoutcome-file"
                                          "valueString" arg}]
                            "issue" (vec (get issues-by-arg arg []))}})
                        input-args)]
      (spit output-path (json/write-str {"resourceType" "Bundle" "type" "collection" "entry" entries}))
      (ok-invocation))))

(deftest gate-batch-attributes-results-per-file-in-input-order-test
  (let [out-dir (temp-dir)
        cache-dir (str out-dir "/verdict-cache")
        path-a (str out-dir "/a.json")
        path-b (str out-dir "/b.json")
        _ (spit path-a "{\"resourceType\":\"Bundle\",\"entry\":[1]}")
        _ (spit path-b "{\"resourceType\":\"Bundle\",\"entry\":[2]}")
        run-invocation (fake-batch-invocation
                         {path-a []
                          path-b [{"severity" "error" "code" "invalid" "expression" ["Patient.birthDate"]
                                    "details" {"text" "Not a valid date format"}}]})
        opts {:artifacts [validator-artifact] :java-bin "/fake/java" :out-dir out-dir
              :verdict-cache-dir cache-dir
              :run-invocation run-invocation
              :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/v.jar" :artifact validator-artifact}))}
        r (gate/gate-batch [path-a path-b] opts)]
    (is (kernel/ok? r))
    (let [[result-a result-b] (:results (:payload r))]
      (is (= path-a (:path result-a)))
      (is (= :pass (:verdict result-a)))
      (is (= path-b (:path result-b)))
      (is (= :rejected (:verdict result-b))))))

(deftest gate-batch-issues-exactly-one-subprocess-invocation-for-n-files-test
  (let [out-dir (temp-dir)
        paths (mapv #(let [p (str out-dir "/" % ".json")] (spit p (str "{\"n\":" % "}")) p) (range 5))
        invocation-count (atom 0)
        underlying (fake-batch-invocation {})
        run-invocation (fn [opts] (swap! invocation-count inc) (underlying opts))
        opts {:artifacts [validator-artifact] :java-bin "/fake/java" :out-dir out-dir
              :verdict-cache-dir (str out-dir "/verdict-cache")
              :run-invocation run-invocation
              :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/v.jar" :artifact validator-artifact}))}
        r (gate/gate-batch paths opts)]
    (is (kernel/ok? r))
    (is (= 5 (count (:results (:payload r)))))
    (is (= 1 @invocation-count) "five files must cost exactly one subprocess invocation")))

(deftest gate-batch-fully-warm-cache-makes-no-subprocess-call-test
  (let [out-dir (temp-dir)
        cache-dir (str out-dir "/verdict-cache")
        path-a (str out-dir "/a.json")
        path-b (str out-dir "/b.json")
        _ (spit path-a "{\"a\":1}")
        _ (spit path-b "{\"b\":2}")
        invocation-count (atom 0)
        underlying (fake-batch-invocation {})
        run-invocation (fn [opts] (swap! invocation-count inc) (underlying opts))
        opts {:artifacts [validator-artifact] :java-bin "/fake/java" :out-dir out-dir
              :verdict-cache-dir cache-dir
              :run-invocation run-invocation
              :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/v.jar" :artifact validator-artifact}))}
        r1 (gate/gate-batch [path-a path-b] opts)
        r2 (gate/gate-batch [path-a path-b] opts)]
    (is (kernel/ok? r1))
    (is (kernel/ok? r2))
    (is (= 1 @invocation-count) "the second gate-batch call must be entirely cache hits")
    (is (= (:results (:payload r1)) (:results (:payload r2))))))

(deftest gate-batch-partial-warm-cache-only-batches-the-misses-test
  (let [out-dir (temp-dir)
        cache-dir (str out-dir "/verdict-cache")
        path-a (str out-dir "/a.json")
        path-b (str out-dir "/b.json")
        _ (spit path-a "{\"a\":1}")
        _ (spit path-b "{\"b\":2}")
        captured-input-args (atom nil)
        underlying (fake-batch-invocation {})
        run-invocation (fn [{:keys [args] :as opts}]
                         (let [args (vec args)
                               output-arg (first (filter #(str/starts-with? % "-output=") args))
                               output-idx (.indexOf ^java.util.List args output-arg)]
                           (reset! captured-input-args (subvec args (inc output-idx))))
                         (underlying opts))
        opts {:artifacts [validator-artifact] :java-bin "/fake/java" :out-dir out-dir
              :verdict-cache-dir cache-dir
              :run-invocation run-invocation
              :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/v.jar" :artifact validator-artifact}))}
        _ (gate/gate-file path-a opts)]
    (reset! captured-input-args nil)
    (let [r (gate/gate-batch [path-a path-b] opts)]
      (is (kernel/ok? r))
      (is (= [path-b] @captured-input-args) "only the still-uncached path should reach the batch subprocess call"))))

(deftest gate-batch-missing-attribution-is-an-error-not-a-silent-mismatch-test
  (let [out-dir (temp-dir)
        path-a (str out-dir "/a.json")
        path-b (str out-dir "/b.json")
        _ (spit path-a "{\"a\":1}")
        _ (spit path-b "{\"b\":2}")
        ;; a broken fake: only ever attributes findings back to path-a,
        ;; simulating a validator-contract violation (a batch entry
        ;; whose own file this seam cannot identify).
        broken-run-invocation (fn [{:keys [args] :as opts}]
                                 (let [args (vec args)
                                       output-arg (first (filter #(str/starts-with? % "-output=") args))
                                       output-path (subs output-arg (count "-output="))]
                                   (spit output-path
                                         (json/write-str
                                          {"resourceType" "Bundle" "type" "collection"
                                           "entry" [{"resource" {"resourceType" "OperationOutcome"
                                                                  "extension" [{"url" "http://hl7.org/fhir/StructureDefinition/operationoutcome-file"
                                                                                "valueString" path-a}]
                                                                  "issue" []}}]}))
                                   (ok-invocation)))
        opts {:artifacts [validator-artifact] :java-bin "/fake/java" :out-dir out-dir
              :verdict-cache-dir (str out-dir "/verdict-cache")
              :run-invocation broken-run-invocation
              :resolve-artifact (fn [_ _ _] (kernel/ok {:path "/fake/v.jar" :artifact validator-artifact}))}
        r (gate/gate-batch [path-a path-b] opts)]
    (is (kernel/error? r))
    (is (= :batch-attribution-missing (:category r)))
    (is (= [path-b] (:paths (:payload r))))))

;; ---- gate-file/gate-batch entry guard (ADR-0098): missing and
;; exists-but-unreadable paths were both previously unguarded --
;; verdict-cache-lookup's own sha256-file call threw a raw
;; FileNotFoundException three frames past this component's own
;; boundary for either case. ----

(deftest gate-file-missing-path-is-a-categorized-error-test
  ;; fhir's own first-ever entry check (Finding 1, ADR-0096/ADR-0097):
  ;; this leg was never guarded at all before this session -- the raw
  ;; FileNotFoundException 2b's own red evidence captured is now a
  ;; categorized rejection instead.
  (let [r (gate/gate-file "/no/such/file.json" {:artifacts [validator-artifact]})]
    (is (kernel/error? r))
    (is (= :file-not-found (:category r)))
    (is (= "/no/such/file.json" (:path (:payload r))))
    (is (not (contains? (:payload r) :reason)))))

(deftest gate-file-permission-denied-path-is-a-categorized-error-test
  (let [out-dir (temp-dir)
        path (str out-dir "/unreadable.json")
        _ (spit path "{}")
        _ (shell/sh "chmod" "000" path)
        f (io/file path)]
    (if (.canRead f)
      ;; root (or an equivalent environment) bypasses permission bits
      ;; entirely -- chmod 000 would silently lie about reproducing the
      ;; unreadable-file leg here, so this test skips itself rather than
      ;; fail (or worse, pass) for the wrong reason.
      (println "SKIPPED gate-file-permission-denied-path-is-a-categorized-error-test: running as an environment where chmod 000 did not remove read access (root?)")
      (let [r (gate/gate-file path {:artifacts [validator-artifact]})]
        (is (kernel/error? r))
        (is (= :file-not-found (:category r)))
        (is (= path (:path (:payload r))))
        (is (= :permission-denied (:reason (:payload r))))))))

(deftest gate-batch-permission-denied-path-is-a-categorized-error-and-fails-fast-test
  (let [out-dir (temp-dir)
        path-a (str out-dir "/a.json")
        path-b (str out-dir "/unreadable.json")
        _ (spit path-a "{}")
        _ (spit path-b "{}")
        _ (shell/sh "chmod" "000" path-b)
        f (io/file path-b)]
    (if (.canRead f)
      (println "SKIPPED gate-batch-permission-denied-path-is-a-categorized-error-and-fails-fast-test: running as an environment where chmod 000 did not remove read access (root?)")
      (let [r (gate/gate-batch [path-a path-b] {:artifacts [validator-artifact]})]
        (is (kernel/error? r))
        (is (= :file-not-found (:category r)))
        (is (= path-b (:path (:payload r))))
        (is (= :permission-denied (:reason (:payload r))))))))
