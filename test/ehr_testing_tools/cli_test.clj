(ns ehr-testing-tools.cli-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn]
            [clojure.java.io :as io]
            [clojure.string]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.artifact :as artifact]
            [ehr-testing-tools.judge.report :as report]
            [ehr-testing-tools.cli :as cli])
  (:import [java.io File]))

(defn- sample-artifact []
  {:kind :engine :name "synthea" :version "4.0.0"
   :sha256 (apply str (repeat 64 "a"))
   :source "https://example.invalid/synthea.jar"
   :acquired "2026-07-24" :license-status :verified})

(defn- temp-lockfile [artifacts]
  (let [f (File/createTempFile "lockfile" ".edn")]
    (spit f (pr-str {:artifacts artifacts}))
    (.getAbsolutePath f)))

;; ---- exit-code mapping (ADR-0004) ----

(deftest exit-code-mapping-test
  (is (= 0 (cli/result->exit-code (result/ok {}))))
  (is (= 1 (cli/result->exit-code (result/rejected :whatever {}))))
  (is (= 2 (cli/result->exit-code (result/error :whatever {})))))

;; ---- arg parsing ----

(deftest parse-splits-positional-and-opts-test
  (let [{:keys [args opts]} (cli/parse ["corpus" "generate" "--seed" "42" "--population" "100" "--json"])]
    (is (= ["corpus" "generate"] args))
    (is (= 42 (:seed opts)))
    (is (= 100 (:population opts)))
    (is (true? (:json opts)))))

(deftest parse-defaults-json-false-test
  (let [{:keys [opts]} (cli/parse ["artifact" "fetch" "--name" "synthea" "--version" "4.0.0"])]
    (is (= "synthea" (:name opts)))
    (is (not (:json opts))))) ; nil is falsy -- --json wasn't supplied

(deftest parse-keeps-numeric-looking-strings-as-strings-test
  ;; --version and --reference-date look numeric but must never be
  ;; auto-coerced -- "4.0.0"/"20260101" are identifiers, not numbers, and
  ;; a coerced long breaks java.lang.ProcessBuilder's String[] args.
  (let [{:keys [opts]} (cli/parse ["corpus" "generate" "--reference-date" "20260101"])]
    (is (= "20260101" (:reference-date opts)))
    (is (string? (:reference-date opts))))
  (let [{:keys [opts]} (cli/parse ["artifact" "fetch" "--version" "4.0.0"])]
    (is (= "4.0.0" (:version opts)))))

(deftest parse-maps-config-flag-to-config-path-test
  ;; corpus.generate's option key is :config-path; the CLI flag is
  ;; --config-path so the two line up without a renaming layer in dispatch.
  (let [{:keys [opts]} (cli/parse ["corpus" "generate" "--config-path" "config/synthea/synthea.properties"])]
    (is (= "config/synthea/synthea.properties" (:config-path opts)))))

;; ---- dispatch ----

(deftest dispatch-unknown-command-test
  (let [r (cli/dispatch ["bogus" "thing"] {} {})]
    (is (result/error? r))
    (is (= :unknown-command (:category r)))))

;; ---- honest errors (DOC-1 Step 4): the enumerable-options family
;; names its valid options plus a help pointer, bounded to
;; unknown-command (group and action) and mutate-command's own
;; :unknown-operator -- category unchanged in every case. ----

(deftest dispatch-unknown-group-names-the-valid-groups-test
  (let [r (cli/dispatch ["bogus" "thing"] {} {})]
    (is (= :unknown-command (:category r)) "category survives the payload extension")
    (is (= #{"artifact" "corpus" "gate" "check"} (set (:valid-options (:payload r)))))
    (is (= "run: ehr help" (:hint (:payload r))))))

(deftest dispatch-unknown-artifact-action-names-fetch-and-resolve-test
  (let [r (cli/dispatch ["artifact" "bogus"] {} {})]
    (is (= :unknown-command (:category r)))
    (is (= #{"fetch" "resolve"} (set (:valid-options (:payload r)))))))

(deftest dispatch-unknown-corpus-action-names-its-verbs-test
  (let [r (cli/dispatch ["corpus" "bogus"] {} {})]
    (is (= :unknown-command (:category r)))
    (is (= #{"generate" "mutate" "intake" "operators"} (set (:valid-options (:payload r)))))))

(deftest dispatch-unknown-gate-action-names-v2-and-fhir-test
  (let [r (cli/dispatch ["gate" "bogus"] {} {})]
    (is (= :unknown-command (:category r)))
    (is (= #{"v2" "fhir"} (set (:valid-options (:payload r)))))))

;; ---- help / --help / bare ehr (DOC-1 Step 2): a :category :cli-help
;; result short-circuits before any capability -fn runs. ----

(deftest dispatch-help-verb-alone-returns-top-level-usage-test
  (let [r (cli/dispatch ["help"] {} {})]
    (is (result/ok? r))
    (is (= :cli-help (:category r)))
    (is (clojure.string/includes? (:text (:payload r)) "Usage:"))))

(deftest dispatch-help-verb-with-group-returns-group-usage-test
  (let [r (cli/dispatch ["help" "gate"] {} {})]
    (is (result/ok? r))
    (is (= :cli-help (:category r)))
    (is (clojure.string/includes? (:text (:payload r)) "gate"))))

(deftest dispatch-help-verb-with-unknown-group-falls-back-to-top-level-test
  (let [r (cli/dispatch ["help" "bogus"] {} {})]
    (is (result/ok? r))
    (is (clojure.string/includes? (:text (:payload r)) "Usage:"))))

(deftest dispatch-double-dash-help-short-circuits-before-command-runs-test
  (let [called (atom false)
        r (cli/dispatch ["gate" "v2"] {:help true}
                         {:gate-v2-fn (fn [_opts] (reset! called true) (result/ok {}))})]
    (is (not @called) "the verb's own command fn must not run when --help is given")
    (is (result/ok? r))
    (is (= :cli-help (:category r)))
    (is (clojure.string/includes? (:text (:payload r)) "gate"))))

(deftest dispatch-double-dash-help-with-no-group-returns-top-level-usage-test
  (let [r (cli/dispatch [] {:help true} {})]
    (is (result/ok? r))
    (is (= :cli-help (:category r)))
    (is (clojure.string/includes? (:text (:payload r)) "Usage:"))))

(deftest dispatch-bare-invocation-is-an-error-with-usage-text-test
  (let [r (cli/dispatch nil {} {})]
    (is (result/error? r))
    (is (= :cli-help (:category r)))
    (is (= 2 (cli/result->exit-code r)))
    (is (clojure.string/includes? (:text (:payload r)) "Usage:"))))

(deftest dispatch-routes-artifact-fetch-test
  (let [called (atom nil)
        r (cli/dispatch ["artifact" "fetch"] {:name "synthea" :version "4.0.0"}
                         {:fetch-fn (fn [opts] (reset! called opts) (result/ok {:cached true}))})]
    (is (result/ok? r))
    (is (= {:name "synthea" :version "4.0.0"} @called))))

(deftest dispatch-routes-artifact-resolve-test
  (let [called (atom nil)
        r (cli/dispatch ["artifact" "resolve"] {:name "synthea" :version "4.0.0"}
                         {:resolve-fn (fn [opts] (reset! called opts) (result/rejected :not-cached {}))})]
    (is (result/rejected? r))
    (is (= {:name "synthea" :version "4.0.0"} @called))))

(deftest dispatch-routes-corpus-generate-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "generate"] {:seed 1}
                         {:generate-fn (fn [opts] (reset! called opts) (result/ok {:manifest {}}))})]
    (is (result/ok? r))
    (is (= {:seed 1} @called))))

(deftest dispatch-routes-corpus-mutate-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "mutate"] {:input "x.json"}
                         {:mutate-fn (fn [opts] (reset! called opts) (result/ok {:count 0}))})]
    (is (result/ok? r))
    (is (= {:input "x.json"} @called))))

(deftest dispatch-routes-gate-v2-test
  (let [called (atom nil)
        r (cli/dispatch ["gate" "v2"] {:path "test/fixtures/v2"}
                         {:gate-v2-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= {:path "test/fixtures/v2"} @called))))

(deftest dispatch-routes-gate-fhir-test
  (let [called (atom nil)
        r (cli/dispatch ["gate" "fhir"] {:path "some-corpus/"}
                         {:gate-fhir-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= {:path "some-corpus/"} @called))))

(deftest dispatch-gate-accepts-a-positional-path-test
  ;; `ehr gate v2 PATH` -- PATH is the third positional arg, not a
  ;; --path flag, matching the CLI contract as specified.
  (let [called (atom nil)
        r (cli/dispatch ["gate" "v2" "test/fixtures/v2"] {}
                         {:gate-v2-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= "test/fixtures/v2" (:path @called)))))

(deftest dispatch-gate-explicit-path-opt-not-overridden-by-positional-test
  (let [called (atom nil)
        r (cli/dispatch ["gate" "v2" "positional-path"] {:path "explicit-path"}
                         {:gate-v2-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= "explicit-path" (:path @called)))))

(deftest dispatch-routes-corpus-intake-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "intake"] {:source-dir "src"}
                         {:intake-fn (fn [opts] (reset! called opts) (result/ok {:catalog []}))})]
    (is (result/ok? r))
    (is (= {:source-dir "src"} @called))))

(deftest dispatch-routes-corpus-operators-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "operators"] {}
                         {:operators-fn (fn [opts] (reset! called opts) (result/ok {:operators []}))})]
    (is (result/ok? r))
    (is (= {} @called))))

;; ---- render ----

(deftest render-edn-by-default-test
  (is (= "{:status :ok, :payload {:x 1}}" (cli/render (result/ok {:x 1}) false))))

(deftest render-json-when-requested-test
  (let [rendered (cli/render (result/ok {:x 1}) true)]
    (is (clojure.string/includes? rendered "\"status\""))
    (is (clojure.string/includes? rendered "\"ok\""))))

;; ---- fetch-command / resolve-command (the real default-fn command
;; paths that dispatch's :fetch-fn/:resolve-fn tests above only ever
;; exercise via injected stubs -- these test the real wiring) ----

(deftest fetch-command-unknown-artifact-test
  (let [lockfile (temp-lockfile [(sample-artifact)])
        r (cli/fetch-command {:name "nope" :version "9.9.9" :lockfile lockfile})]
    (is (result/rejected? r))
    (is (= :unknown-artifact (:category r)))))

(deftest fetch-command-propagates-lockfile-read-failure-test
  (let [r (cli/fetch-command {:name "synthea" :version "4.0.0" :lockfile "/no/such/lockfile.edn"})]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

(deftest fetch-command-delegates-known-artifact-to-artifact-fetch-test
  (let [art (sample-artifact)
        lockfile (temp-lockfile [art])
        called (atom nil)]
    (with-redefs [artifact/fetch (fn [artifact-entry] (reset! called artifact-entry) (result/ok {:cached true}))]
      (let [r (cli/fetch-command {:name "synthea" :version "4.0.0" :lockfile lockfile})]
        (is (result/ok? r))
        (is (= art @called))))))

(deftest fetch-command-defaults-to-repo-root-lockfile-when-omitted-test
  ;; No :lockfile opt -- falls back to "artifacts.lock.edn" (the real
  ;; repo-root lockfile, which is why this must reject :unknown-artifact
  ;; rather than :not-found for a name that really isn't in it).
  (let [r (cli/fetch-command {:name "definitely-not-a-real-artifact" :version "0.0.0"})]
    (is (result/rejected? r))
    (is (= :unknown-artifact (:category r)))))

(deftest resolve-command-delegates-to-artifact-resolve-test
  (let [art (sample-artifact)
        lockfile (temp-lockfile [art])
        called (atom nil)]
    (with-redefs [artifact/resolve (fn [artifacts name version]
                                     (reset! called [artifacts name version])
                                     (result/rejected :not-cached {:name name :version version}))]
      (let [r (cli/resolve-command {:name "synthea" :version "4.0.0" :lockfile lockfile})]
        (is (result/rejected? r))
        (is (= [[art] "synthea" "4.0.0"] @called))))))

(deftest resolve-command-propagates-lockfile-read-failure-test
  (let [r (cli/resolve-command {:name "synthea" :version "4.0.0" :lockfile "/no/such/lockfile.edn"})]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

;; ---- mutate-command (`ehr corpus mutate`): input file/dir, operator
;; id, locator, output dir -> writes mutant JSON files plus lineage
;; EDN sidecars under output-dir/lineage/ (design choice, documented
;; on mutate-command's own docstring: a subdirectory rather than
;; interleaved sidecars, so a downstream stage can glob output-dir for
;; data and output-dir/lineage for provenance without filtering). ----

(defn- temp-dir* []
  (let [f (File/createTempFile "cli-mutate-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

(def sample-bundle-json
  "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"p1\",\"gender\":\"female\"}}]}")

(deftest mutate-command-happy-path-writes-mutant-and-lineage-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "patient1.json") sample-bundle-json)
        r (cli/mutate-command {:input in-dir :operator-id "remove-required-element"
                                :locator-path "entry[0].resource.gender" :output-dir out-dir})]
    (is (result/ok? r))
    (is (= 1 (:count (:payload r))))
    (let [mutant (slurp (io/file out-dir "patient1.json"))
          lineage-file (io/file out-dir "lineage" "patient1.json.lineage.edn")]
      (is (not (clojure.string/includes? mutant "gender")))
      (is (.exists lineage-file))
      (let [lineage (clojure.edn/read-string (slurp lineage-file))]
        (is (= :remove-required-element (:id (:operator (:transformation lineage)))))))))

(deftest mutate-command-processes-every-json-file-in-a-directory-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        _ (spit (io/file in-dir "b.json") sample-bundle-json)
        _ (spit (io/file in-dir "not-json.txt") "ignore me")
        r (cli/mutate-command {:input in-dir :operator-id "duplicate-element"
                                :locator-path "entry[0].resource.gender" :output-dir out-dir})]
    (is (result/ok? r))
    (is (= 2 (:count (:payload r))))
    (is (.exists (io/file out-dir "a.json")))
    (is (.exists (io/file out-dir "b.json")))
    (is (not (.exists (io/file out-dir "not-json.txt"))))))

(deftest mutate-command-accepts-a-single-file-as-input-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        f (io/file in-dir "one.json")
        _ (spit f sample-bundle-json)
        r (cli/mutate-command {:input (.getAbsolutePath f) :operator-id "wrong-type-value"
                                :locator-path "entry[0].resource.gender" :output-dir out-dir})]
    (is (result/ok? r))
    (is (= 1 (:count (:payload r))))))

(deftest mutate-command-rejects-unknown-operator-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:input in-dir :operator-id "no-such-operator"
                                :locator-path "entry[0].resource.gender" :output-dir (temp-dir*)})]
    (is (result/rejected? r))
    (is (= :unknown-operator (:category r)))))

(deftest mutate-command-unknown-operator-names-the-valid-ids-test
  ;; DOC-1 Step 4: the enumerable-options error pass, extended to this
  ;; site (the CLI-reachable equivalent of the prompt's named
  ;; :invalid-operator -- see the DOC-1 close-out report). Category is
  ;; unchanged; only the payload gains :valid-options and a hint.
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:input in-dir :operator-id "no-such-operator"
                                :locator-path "entry[0].resource.gender" :output-dir (temp-dir*)})]
    (is (= :unknown-operator (:category r)) "category survives the payload extension")
    (is (contains? (set (:valid-options (:payload r))) :remove-required-element))
    (is (= 10 (count (:valid-options (:payload r)))))
    (is (= "run: ehr corpus operators" (:hint (:payload r))))))

(deftest mutate-command-defaults-operator-version-to-1-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:input in-dir :operator-id "remove-required-element"
                                :locator-path "entry[0].resource.gender" :output-dir (temp-dir*)})]
    (is (result/ok? r))))

(deftest mutate-command-propagates-a-locator-that-does-not-resolve-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:input in-dir :operator-id "remove-required-element"
                                :locator-path "entry[0].resource.noSuchField" :output-dir (temp-dir*)})]
    (is (result/rejected? r))
    (is (= :locator-not-found (:category r)))))

(deftest mutate-command-rejects-invalid-locator-path-syntax-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:input in-dir :operator-id "remove-required-element"
                                :locator-path "entry[bad]" :output-dir (temp-dir*)})]
    (is (result/rejected? r))
    (is (= :invalid-fhir-path (:category r)))))

;; ---- mutate-command, v2 dispatch (P7): same command, format dispatch
;; by operator lookup routes *.hl7 files through the er7 substrate
;; instead of *.json through plain FHIR data. ----

(def ^:private admit-content
  (delay (slurp (io/file "test/fixtures/v2/adt-a01-admit.hl7"))))

(deftest mutate-command-v2-happy-path-writes-mutant-and-lineage-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "adt.hl7") @admit-content)
        r (cli/mutate-command {:input in-dir :operator-id "blank-required-field"
                                :locator-path "MSH-9" :output-dir out-dir})]
    (is (result/ok? r))
    (is (= 1 (:count (:payload r))))
    (let [mutant (slurp (io/file out-dir "adt.hl7"))
          lineage-file (io/file out-dir "lineage" "adt.hl7.lineage.edn")]
      (is (not (clojure.string/includes? mutant "ADT^A01^ADT_A01")))
      (is (.exists lineage-file))
      (let [lineage (clojure.edn/read-string (slurp lineage-file))]
        (is (= :blank-required-field (:id (:operator (:transformation lineage)))))
        (is (= {:format :v2 :path "MSH-9"} (:locator (:transformation lineage))))))))

(deftest mutate-command-v2-processes-every-hl7-file-in-a-directory-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "a.hl7") @admit-content)
        _ (spit (io/file in-dir "b.hl7") @admit-content)
        _ (spit (io/file in-dir "not-hl7.json") sample-bundle-json)
        r (cli/mutate-command {:input in-dir :operator-id "corrupt-encoding-characters"
                                :locator-path "MSH-2" :output-dir out-dir})]
    (is (result/ok? r))
    (is (= 2 (:count (:payload r))))
    (is (.exists (io/file out-dir "a.hl7")))
    (is (.exists (io/file out-dir "b.hl7")))
    (is (not (.exists (io/file out-dir "not-hl7.json"))))))

(deftest mutate-command-v2-propagates-a-locator-that-does-not-resolve-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.hl7") @admit-content)
        r (cli/mutate-command {:input in-dir :operator-id "blank-required-field"
                                :locator-path "ZZZ-3" :output-dir (temp-dir*)})]
    (is (result/rejected? r))
    (is (= :locator-not-found (:category r)))))

(deftest mutate-command-v2-rejects-invalid-locator-path-syntax-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.hl7") @admit-content)
        r (cli/mutate-command {:input in-dir :operator-id "blank-required-field"
                                :locator-path "PID-0" :output-dir (temp-dir*)})]
    (is (result/rejected? r))
    (is (= :invalid-v2-path (:category r)))))

;; ---- intake-command (`ehr corpus intake`): the real wiring, not the
;; injected-stub path dispatch-routes-corpus-intake-test exercises ----

(deftest intake-command-delegates-to-corpus-intake-with-explicit-received-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "patient.json") sample-bundle-json)
        r (cli/intake-command {:source-dir in-dir :label "acme" :out out-dir :received "2026-07-24"})]
    (is (result/ok? r))
    (is (= 1 (:file-count (:intake-record (:payload r)))))
    (is (= "2026-07-24" (:date (:intake-record (:payload r)))))))

(deftest intake-command-defaults-received-to-today-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "patient.json") sample-bundle-json)
        r (cli/intake-command {:source-dir in-dir :label "acme" :out out-dir})]
    (is (result/ok? r))
    (is (= (str (java.time.LocalDate/now)) (:date (:intake-record (:payload r)))))))

;; ---- operators-command (`ehr corpus operators`): a pure read of
;; corpus.operators' registry -- no filesystem, no subprocess, no
;; options required. ----

(deftest operators-command-lists-all-ten-registered-operators-test
  (let [r (cli/operators-command {})]
    (is (result/ok? r))
    (is (= 10 (count (:operators (:payload r)))))
    (is (= #{:remove-required-element :duplicate-element :invalid-code-value
             :malformed-date :wrong-type-value :blank-required-field
             :corrupt-encoding-characters :malformed-datetime-value
             :truncate-segment-fields :corrupt-segment-name}
           (set (map :id (:operators (:payload r))))))))

(deftest operators-command-filters-by-format-test
  (let [r (cli/operators-command {:format "v2"})]
    (is (result/ok? r))
    (is (= 5 (count (:operators (:payload r)))))
    (is (every? #(= :v2 (:format %)) (:operators (:payload r))))))

(deftest operators-command-sorted-by-format-then-id-test
  (let [rows (:operators (:payload (cli/operators-command {})))]
    (is (= rows (sort-by (juxt :format :id) rows)))))

(deftest operators-command-rows-carry-contract-type-and-target-test
  (let [rows (:operators (:payload (cli/operators-command {})))
        remove-req (first (filter #(= :remove-required-element (:id %)) rows))]
    (is (= :violates (:type remove-req)))
    (is (string? (:target remove-req)))
    (is (true? (:locator-required? remove-req)))
    (is (= "1" (:version remove-req)))))

(deftest operators-command-is-a-pure-registry-read-no-io-test
  ;; Proven by construction: redefining io/file to throw confirms
  ;; nothing in operators-command touches the filesystem (it only
  ;; derefs corpus.operators' in-memory registry atom).
  (with-redefs [io/file (fn [& _] (throw (ex-info "no filesystem access expected" {})))]
    (let [r (cli/operators-command {})]
      (is (result/ok? r))
      (is (= 10 (count (:operators (:payload r))))))))

;; ---- gate-command / gate-v2-command (`ehr gate v2`): builds a
;; format-agnostic gate report over a file or directory; exit-code
;; contract via result/ok vs result/rejected, not special-cased in
;; result->exit-code (ADR-0004's generic mapping already does the
;; right thing once gate-command itself signals :gate-rejected). ----

(deftest gate-v2-command-gates-a-single-passing-file-test
  (let [r (cli/gate-v2-command {:path "test/fixtures/v2/adt-a01-admit.hl7"})]
    (is (result/ok? r))
    (is (= {:pass 1 :rejected 0 :indeterminate 0 :no-verdict 0} (:totals (:payload r))))))

(deftest gate-v2-command-gates-a-directory-test
  (let [r (cli/gate-v2-command {:path "test/fixtures/v2"})]
    (is (result/ok? r))
    (is (= 5 (count (:files (:payload r)))))))

(deftest gate-v2-command-rejects-when-any-file-is-rejected-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "broken.hl7") "MSH|^~&|only-encoding-chars-broken")
        r (cli/gate-v2-command {:path in-dir})]
    (is (result/rejected? r))
    (is (= :gate-rejected (:category r)))
    (is (= 1 (cli/result->exit-code r)))))

(deftest gate-v2-command-writes-report-file-when-requested-test
  (let [out-file (str (temp-dir*) "/report.edn")
        r (cli/gate-v2-command {:path "test/fixtures/v2/adt-a01-admit.hl7" :report out-file})]
    (is (result/ok? r))
    (is (.exists (io/file out-file)))
    (is (= (:payload r) (clojure.edn/read-string (slurp out-file))))))

;; ---- fhir-gate-command: threads lockfile artifacts + :out-dir into
;; judge.fhir/gate-file|gate-dir, curried to gate-command's 1-arity
;; shape. No real subprocess exercised here (hermetic-suite discipline
;; -- judge.fhir's own test suite already covers execute/interpret with
;; injected fakes); this only proves the wiring propagates a real
;; lockfile-resolution failure correctly. ----

(deftest fhir-gate-command-propagates-unknown-artifact-when-validator-not-in-lockfile-test
  (let [lockfile (temp-lockfile [])
        r (cli/fhir-gate-command {:path "test/fixtures/v2/adt-a01-admit.hl7" :lockfile lockfile})]
    (is (result/rejected? r))
    (is (= :unknown-artifact (:category r)))))

(deftest fhir-gate-command-propagates-lockfile-read-failure-test
  (let [r (cli/fhir-gate-command {:path "x.json" :lockfile "/no/such/lockfile.edn"})]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

;; ---- gate --baseline (`ehr gate v2|fhir DIR --baseline report.edn`,
;; P6): a finding counts toward rejection only if it isn't already
;; present in the baseline for that file. ----

(deftest gate-v2-command-baseline-mode-suppresses-a-known-finding-test
  (let [in-dir (temp-dir*)
        broken-path (io/file in-dir "broken.hl7")
        _ (spit broken-path "MSH|^~&|only-encoding-chars-broken")
        baseline-run (cli/gate-v2-command {:path in-dir})
        baseline-file (str (temp-dir*) "/baseline.edn")
        _ (spit baseline-file (pr-str (:payload baseline-run)))
        r (cli/gate-v2-command {:path in-dir :baseline baseline-file})]
    (is (result/rejected? baseline-run) "sanity: the file is genuinely rejected absolutely")
    (is (result/ok? r) "every finding is already in the baseline -- nothing novel")
    (is (report/valid? (:absolute (:payload r))))
    (is (report/valid? (:relative (:payload r))))
    (is (= :rejected (:verdict (first (:files (:absolute (:payload r)))))))
    (is (= :pass (:verdict (first (:files (:relative (:payload r)))))))))

(deftest gate-v2-command-baseline-mode-still-rejects-a-genuinely-new-finding-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "ok.hl7") (slurp "test/fixtures/v2/adt-a01-admit.hl7"))
        baseline-run (cli/gate-v2-command {:path in-dir})
        baseline-file (str (temp-dir*) "/baseline.edn")
        _ (spit baseline-file (pr-str (:payload baseline-run)))
        ;; Now break the same file -- a genuinely new finding relative
        ;; to the passing baseline.
        _ (spit (io/file in-dir "ok.hl7") "MSH|^~&|only-encoding-chars-broken")
        r (cli/gate-v2-command {:path in-dir :baseline baseline-file})]
    (is (result/ok? baseline-run))
    (is (result/rejected? r))
    (is (= :gate-rejected (:category r)))
    (is (= 1 (cli/result->exit-code r)))))

;; ---- --treat-no-verdict-as (`ehr gate --treat-no-verdict-as
;; pass|rejected`, ADR-0010): policy totality at the act layer. Tested
;; against `gate-command` directly with an injected fake gate-file-fn --
;; neither judge.v2 (never produces :no-verdict) nor judge.fhir
;; (needs a real subprocess) can produce a :no-verdict outcome
;; hermetically, matching this repo's own dependency-injection
;; testing convention. ----

(defn- fake-no-verdict-gate-file
  [_path]
  (result/ok {:verdict :no-verdict :cause :terminology-suppressed
              :findings [{:severity :warning :code "code-invalid"
                          :locator {:format :fhir :path "x"} :message "m"
                          :engine {:name "e" :version "1"}
                          :disposition :no-verdict :cause :terminology-suppressed}]}))

(defn- fake-rejected-gate-file
  [_path]
  (result/ok {:verdict :rejected
              :findings [{:severity :error :code "invalid"
                          :locator {:format :fhir :path "x"} :message "m"
                          :engine {:name "e" :version "1"}
                          :disposition :rejected}]}))

(deftest gate-command-default-exit-code-for-no-verdict-is-distinct-test
  (let [gate-fn (cli/gate-command fake-no-verdict-gate-file (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json"})]
    (is (= :gate-no-verdict (:category r)))
    (is (= cli/no-verdict-exit-code (cli/result->exit-code r)))
    (is (not (contains? #{0 1 2} (cli/result->exit-code r)))
        "distinct from the existing ok/rejected/error codes -- no silent default")))

(deftest gate-command-treat-no-verdict-as-pass-remaps-to-ok-test
  (let [gate-fn (cli/gate-command fake-no-verdict-gate-file (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json" :treat-no-verdict-as "pass"})]
    (is (result/ok? r))
    (is (= 0 (cli/result->exit-code r)))))

(deftest gate-command-treat-no-verdict-as-rejected-remaps-to-rejected-test
  (let [gate-fn (cli/gate-command fake-no-verdict-gate-file (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json" :treat-no-verdict-as "rejected"})]
    (is (result/rejected? r))
    (is (= :gate-rejected (:category r)))
    (is (= 1 (cli/result->exit-code r)))))

(deftest gate-command-treat-no-verdict-as-rejects-other-values-test
  (let [gate-fn (cli/gate-command fake-no-verdict-gate-file (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json" :treat-no-verdict-as "bogus"})]
    (is (result/rejected? r))
    (is (= :invalid-treat-no-verdict-as (:category r)))
    (is (= 1 (cli/result->exit-code r)))))

(deftest gate-command-treat-no-verdict-as-pass-does-not-mask-a-genuine-rejection-test
  (let [gate-fn (cli/gate-command fake-rejected-gate-file (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json" :treat-no-verdict-as "pass"})]
    (is (result/rejected? r))
    (is (= :gate-rejected (:category r)))))

(deftest gate-command-no-flag-and-no-no-verdict-is-plain-ok-test
  (let [gate-fn (cli/gate-command (fn [_path] (result/ok {:verdict :pass :findings []}))
                                   (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json"})]
    (is (result/ok? r))
    (is (= 0 (cli/result->exit-code r)))))

(deftest fhir-gate-command-threads-treat-no-verdict-as-through-to-gate-command-test
  ;; No real subprocess exercised (hermetic-suite discipline) -- an
  ;; invalid flag value is rejected before the validator artifact is
  ;; even resolved, which is enough to prove the option reaches
  ;; gate-command's own opts map rather than being silently dropped.
  (let [art {:kind :engine :name "fhir-validator-cli" :version "6.9.12"
             :sha256 (apply str (repeat 64 "c")) :source "https://example.invalid/v.jar"
             :acquired "2026-07-24" :license-status :verified}
        lockfile (temp-lockfile [art])
        r (cli/fhir-gate-command {:path "test/fixtures/v2/adt-a01-admit.hl7" :lockfile lockfile
                                   :treat-no-verdict-as "bogus"
                                   :java-bin "/fake/java"})]
    (is (result/rejected? r))
    (is (= :invalid-treat-no-verdict-as (:category r)))))

(deftest gate-v2-command-baseline-mode-writes-the-baseline-relative-report-when-requested-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.hl7") (slurp "test/fixtures/v2/adt-a01-admit.hl7"))
        baseline-file (str (temp-dir*) "/baseline.edn")
        _ (spit baseline-file (pr-str (:payload (cli/gate-v2-command {:path in-dir}))))
        out-file (str (temp-dir*) "/relative-report.edn")
        r (cli/gate-v2-command {:path in-dir :baseline baseline-file :report out-file})]
    (is (result/ok? r))
    (is (.exists (io/file out-file)))
    (is (= (:payload r) (clojure.edn/read-string (slurp out-file))))))

;; ---- check-command (`ehr check`): reads --assertions as an EDN file,
;; parses --canonicalizers "id@v,..." into ordered [id version] pairs,
;; delegates to ehr-testing-tools.check/check-corpus. ----

(deftest check-command-matches-expected-happy-path-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        bundle "{\"resourceType\":\"Bundle\"}"
        _ (spit (io/file cand-dir "a.json") bundle)
        _ (spit (io/file exp-dir "a.json") bundle)
        r (cli/check-command {:path cand-dir :expected exp-dir})]
    (is (result/ok? r))
    (is (= {:pass 1 :rejected 0 :indeterminate 0 :no-verdict 0} (:totals (:payload r))))))

(deftest check-command-rejects-when-corpora-differ-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        _ (spit (io/file cand-dir "a.json") "{\"resourceType\":\"Bundle\",\"x\":1}")
        _ (spit (io/file exp-dir "a.json") "{\"resourceType\":\"Bundle\",\"x\":2}")
        r (cli/check-command {:path cand-dir :expected exp-dir})]
    (is (result/rejected? r))
    (is (= :check-rejected (:category r)))
    (is (= 1 (cli/result->exit-code r)))))

(deftest check-command-reads-assertions-file-test
  (let [cand-dir (temp-dir*)
        _ (spit (io/file cand-dir "a.json") "{\"resourceType\":\"Bundle\"}")
        assertions-file (str (temp-dir*) "/assertions.edn")
        _ (spit assertions-file (pr-str [{:kind :present :locator {:format :fhir :path "resourceType"}}]))
        r (cli/check-command {:path cand-dir :assertions assertions-file})]
    (is (result/ok? r))))

(deftest check-command-parses-canonicalizers-flag-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        _ (spit (io/file cand-dir "a.json") "{\"a\":1,\"b\":2}")
        _ (spit (io/file exp-dir "a.json") "{\"a\":1,\"b\":9}")
        r (cli/check-command {:path cand-dir :expected exp-dir
                               :canonicalizers "strip-run-timestamp-suffix@1"})]
    ;; strip-run-timestamp-suffix operates on filenames, not this
    ;; content, so it doesn't make the pair equivalent -- this only
    ;; proves the flag parses and reaches check-corpus without error
    ;; (an unknown-canonicalizer rejection would be a different
    ;; :category than :check-rejected).
    (is (result/rejected? r))
    (is (= :check-rejected (:category r)))))

(deftest check-command-parses-pair-by-flag-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        _ (spit (io/file cand-dir "candidate-name.json") "{\"resourceType\":\"Bundle\"}")
        _ (spit (io/file exp-dir "expected-name.json") "{\"resourceType\":\"Bundle\"}")
        r (cli/check-command {:path cand-dir :expected exp-dir :pair-by "hash"})]
    (is (result/ok? r))))

(deftest check-command-writes-report-file-when-requested-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        bundle "{\"resourceType\":\"Bundle\"}"
        _ (spit (io/file cand-dir "a.json") bundle)
        _ (spit (io/file exp-dir "a.json") bundle)
        out-file (str (temp-dir*) "/check-report.edn")
        r (cli/check-command {:path cand-dir :expected exp-dir :report out-file})]
    (is (result/ok? r))
    (is (.exists (io/file out-file)))
    (is (= (:payload r) (clojure.edn/read-string (slurp out-file))))))

(deftest dispatch-routes-check-test
  (let [called (atom nil)
        r (cli/dispatch ["check"] {:path "some-corpus/"}
                         {:check-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= {:path "some-corpus/"} @called))))

(deftest dispatch-check-accepts-a-positional-path-test
  ;; `ehr check DIR` -- check has no sub-verb, so the second positional
  ;; arg IS the path, unlike gate's third-positional convention.
  (let [called (atom nil)
        r (cli/dispatch ["check" "some-corpus/"] {}
                         {:check-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= "some-corpus/" (:path @called)))))

(deftest dispatch-check-explicit-path-opt-not-overridden-by-positional-test
  (let [called (atom nil)
        r (cli/dispatch ["check" "positional-path"] {:path "explicit-path"}
                         {:check-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= "explicit-path" (:path @called)))))

(deftest gate-v2-command-propagates-operational-error-for-a-missing-path-test
  ;; A path that is neither an existing file nor directory: gate-file
  ;; on it should surface as an operational error (slurp on a missing
  ;; file throws inside execute/gate-file's own boundary today via a
  ;; FileNotFoundException -- exercised here to confirm the CLI
  ;; doesn't silently swallow it as a false "pass").
  (let [r (cli/gate-v2-command {:path "/no/such/file.hl7"})]
    (is (not (result/ok? r)))))

;; ---- main! (the real -main body, refactored to take injectable
;; :dispatch-fn / :println-fn / :exit-fn so its exit-code mapping and
;; command routing can be unit-tested without a real dispatch, real
;; stdout, or -- critically -- a real System/exit that would kill the
;; test JVM) ----

(deftest main-bang-prints-rendered-result-and-exits-zero-on-ok-test
  (let [printed (atom nil)
        exit-code (atom nil)
        code (cli/main! ["artifact" "fetch" "--name" "synthea" "--version" "4.0.0"]
                         {:dispatch-fn (fn [_args _opts] (result/ok {:cached true}))
                          :println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [c] (reset! exit-code c))})]
    (is (= 0 code))
    (is (= 0 @exit-code))
    (is (clojure.string/includes? @printed ":status :ok"))))

(deftest main-bang-exits-one-on-rejected-test
  (let [exit-code (atom nil)
        code (cli/main! ["corpus" "generate"]
                         {:dispatch-fn (fn [_args _opts] (result/rejected :not-cached {}))
                          :println-fn (fn [_s] nil)
                          :exit-fn (fn [c] (reset! exit-code c))})]
    (is (= 1 code))
    (is (= 1 @exit-code))))

(deftest main-bang-exits-two-on-error-test
  (let [exit-code (atom nil)
        code (cli/main! ["bogus" "thing"]
                         {:dispatch-fn (fn [_args _opts] (result/error :unknown-command {:args ["bogus" "thing"]}))
                          :println-fn (fn [_s] nil)
                          :exit-fn (fn [c] (reset! exit-code c))})]
    (is (= 2 code))
    (is (= 2 @exit-code))))

(deftest main-bang-renders-json-when-flag-given-test
  (let [printed (atom nil)
        code (cli/main! ["artifact" "fetch" "--json"]
                         {:dispatch-fn (fn [_args opts] (is (true? (:json opts))) (result/ok {:cached true}))
                          :println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed "\"status\""))))

(deftest main-bang-passes-real-parsed-args-to-dispatch-fn-test
  (let [captured (atom nil)
        _code (cli/main! ["corpus" "generate" "--seed" "42"]
                          {:dispatch-fn (fn [args opts] (reset! captured [args opts]) (result/ok {}))
                           :println-fn (fn [_s] nil)
                           :exit-fn (fn [_c] nil)})]
    (is (= [["corpus" "generate"] {:seed 42}] @captured))))

(deftest main-bang-default-dispatch-fn-is-the-real-dispatch-test
  ;; No :dispatch-fn override -- main! must route through the real
  ;; `dispatch`, not silently no-op, when the caller doesn't inject one.
  (let [code (cli/main! ["bogus" "thing"] {:println-fn (fn [_s] nil) :exit-fn (fn [_c] nil)})]
    (is (= 2 code))))

;; ---- main! + help (DOC-1 Step 2): plain text, not EDN/JSON, exit 0
;; for a help request, exit 2 for a bare invocation. Real `dispatch`
;; (no :dispatch-fn override) -- these prove the wiring end to end. ----

(deftest main-bang-help-prints-plain-text-not-edn-and-exits-zero-test
  (let [printed (atom nil)
        exit-code (atom nil)
        code (cli/main! ["help"]
                         {:println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [c] (reset! exit-code c))})]
    (is (= 0 code))
    (is (= 0 @exit-code))
    (is (clojure.string/includes? @printed "Usage:"))
    (is (not (clojure.string/includes? @printed ":status")))))

(deftest main-bang-help-group-prints-that-groups-usage-test
  (let [printed (atom nil)
        code (cli/main! ["help" "corpus"]
                         {:println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed "mutate"))))

(deftest main-bang-double-dash-help-ignores-json-flag-test
  (let [printed (atom nil)
        code (cli/main! ["gate" "--help" "--json"]
                         {:println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed "--treat-no-verdict-as") "ehr gate --help --json still renders gate's own group usage")
    (is (not (clojure.string/includes? @printed "{")) "plain text, never a JSON/EDN projection, regardless of --json")))

(deftest main-bang-bare-invocation-prints-usage-and-exits-two-test
  (let [printed (atom nil)
        exit-code (atom nil)
        code (cli/main! []
                         {:println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [c] (reset! exit-code c))})]
    (is (= 2 code))
    (is (= 2 @exit-code))
    (is (clojure.string/includes? @printed "Usage:"))))
