(ns ehr-testing-sim.cli-test
  "The embedding contract's behavior: dispatch-action routes verbs to
  capability functions (injectable, so no simulation runs here), the
  Result->exit-code mapping matches the host's, and help-group is
  well-formed for the host's help machinery. Also: main!'s own
  --format rendering (edn/json/er7) -- render/help-text/main! are
  the standalone shell's own business, NOT part of the embedding
  contract (cli-spec/help-group/dispatch-action), so their tests live
  here rather than implying a contract change."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [ehr-testing-sim.cli :as cli]
            [ehr-testing-sim.check :as check]
            [ehr-testing-sim.result :as result]
            [ehr-testing-sim.run :as run]
            [ehr-testing-sim.version :as version]
            [ehr-testing-sim.manifest :as manifest]))

(deftest dispatch-routes-run
  (let [seen (atom nil)
        r (cli/dispatch-action "run" {:seed 1}
                               {:run-fn (fn [opts] (reset! seen opts) (result/ok :ran))})]
    (is (result/ok? r))
    (is (= {:seed 1} @seen))))

(deftest dispatch-rejects-unknown-verb
  (let [r (cli/dispatch-action "explode" {})]
    (is (result/error? r))
    (is (= :unknown-command (:category r)))
    (is (= ["run" "check" "identifiers" "version"] (:known (:payload r))))))

(deftest dispatch-routes-identifiers
  (let [seen (atom nil)
        r (cli/dispatch-action "identifiers" {:seed 1}
                               {:identifiers-fn (fn [opts] (reset! seen opts) (result/ok :listed))})]
    (is (result/ok? r))
    (is (= {:seed 1} @seen))))

(deftest run-requires-seed
  (let [r (cli/dispatch-action "run" {})]
    (is (result/error? r))
    (is (= :missing-required-opt (:category r)))))

(deftest exit-code-contract
  (is (= 0 (cli/result->exit-code (result/ok :x))))
  (is (= 1 (cli/result->exit-code (result/rejected :why :x))))
  (is (= 2 (cli/result->exit-code (result/error :why :x)))))

(deftest help-group-shape
  (testing "the shape ehr-testing-tools' help machinery walks"
    (is (string? (:group cli/help-group)))
    (is (string? (:doc cli/help-group)))
    (doseq [{:keys [verb doc flags]} (:verbs cli/help-group)]
      (is (string? verb))
      (is (string? doc))
      (is (vector? flags)))))

(deftest help-group-documents-emit
  (testing "the run verb's flags cover --emit and --reference-date (EmitHL7 wiring)"
    (let [run-verb (first (filter #(= "run" (:verb %)) (:verbs cli/help-group)))
          flag-names (set (map :flag (:flags run-verb)))]
      (is (contains? flag-names "--emit"))
      (is (contains? flag-names "--reference-date")))))

(deftest help-group-documents-churn
  (testing "M2b: the run verb's flags cover --churn (InjectChurn wiring)"
    (let [run-verb (first (filter #(= "run" (:verb %)) (:verbs cli/help-group)))
          flag-names (set (map :flag (:flags run-verb)))]
      (is (contains? flag-names "--churn")))))

(deftest main!-with-churn-flag-runs-and-may-emit-churn-events
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["run" "--seed" "5" "--patients" "8" "--churn" "--emit" "hl7"]
               {:println-fn #(swap! printed conj %)
                :exit-fn #(reset! exited %)})
    (is (= 0 @exited))
    (let [payload (:payload (read-string (first @printed)))]
      (testing "this specific seed produces at least one churn-family message (A17 bed-swap)"
        (is (some #(string/includes? % "^A17") (:messages payload)))))))

(deftest main!-emits-hl7-messages
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["run" "--seed" "42" "--patients" "2" "--emit" "hl7"]
               {:println-fn #(swap! printed conj %)
                :exit-fn #(reset! exited %)})
    (is (= 0 @exited))
    (let [payload (:payload (read-string (first @printed)))]
      (is (= 4 (count (:messages payload))))
      (is (every? #(string/starts-with? % "MSH|") (:messages payload))))))

(deftest main!-prints-and-exits
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["run" "--seed" "3"]
               {:println-fn #(swap! printed conj %)
                :exit-fn #(reset! exited %)})
    (is (= 0 @exited))
    (is (= 1 (count @printed)))
    ;; canonical output is readable EDN
    (is (map? (read-string (first @printed))))))

;; --- M4 Task 0: the wiring fix, proven at the CLI-embedding level ---------

(deftest dispatch-action-run-with-pathways-reaches-the-engine-and-emits-orm-oru
  (testing "M3's :pathways (and, through it, an authored :order step)
            reaches the engine THROUGH dispatch-action -- the same
            entrypoint the standalone shell and a mounting host both
            go through -- proving the completeness fix, not just
            run-command's own unit test"
    (let [pathway {:name "cbc-order" :steps [{:type :admission :location "Renal"}
                                             {:type :order :profile :cbc}
                                             {:type :discharge}]}
          r (cli/dispatch-action "run" {:seed 7 :patients 1 :emit "hl7"
                                        :pathways [{:pathway pathway :weight 1}]})]
      (is (result/ok? r))
      (let [messages (:messages (:payload r))]
        (is (some #(string/includes? % "^O01") messages) "ORM^O01 (order-placed) present")
        (is (some #(string/includes? % "^R01") messages) "ORU^R01 (result-available) present")))))

(deftest check-catches-planted-violation
  (let [bad [{:event :discharge :t 0 :participants [{:patient-id "P1" :role :subject}]}
             {:event :admission :t 5 :participants [{:patient-id "P1" :role :subject}] :location "Renal"}]
        r (check/check-all bad)]
    (is (result/rejected? r))
    (is (= :invariant-violation (:category r)))
    (is (some #(= :discharge-follows-admission (:invariant %))
              (:violations (:payload r))))))

;; --- go-public Task 1: --format edn|json|er7 -----------------------------

(deftest format-defaults-to-edn
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["run" "--seed" "1"]
               {:println-fn #(swap! printed conj %) :exit-fn #(reset! exited %)})
    (is (map? (read-string (first @printed))))))

(deftest json-flag-remains-an-alias-for-format-json
  (testing "--json with no --format still projects to JSON, unchanged"
    (let [printed (atom []) exited (atom nil)]
      (cli/main! ["run" "--seed" "1" "--json"]
                 {:println-fn #(swap! printed conj %) :exit-fn #(reset! exited %)})
      (is (= 0 @exited))
      (is (map? (json/read-str (first @printed)))))))

(deftest format-json-explicit
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["run" "--seed" "1" "--format" "json"]
               {:println-fn #(swap! printed conj %) :exit-fn #(reset! exited %)})
    (is (= 0 @exited))
    (is (map? (json/read-str (first @printed))))))

(deftest format-er7-requires-emit-hl7
  (testing "--format er7 without --emit hl7 is a structured :rejected, not a
            silent edn dump -- the Result still drives the exit code (1)"
    (let [printed (atom []) errs (atom []) exited (atom nil)]
      (cli/main! ["run" "--seed" "1" "--format" "er7"]
                 {:println-fn #(swap! printed conj %)
                  :err-println-fn #(swap! errs conj %)
                  :exit-fn #(reset! exited %)})
      (is (= 1 @exited))
      (is (empty? @printed) "stdout carries nothing when the format gate rejects")
      (let [r (read-string (first @errs))]
        (is (= :rejected (:status r)))))))

(deftest format-er7-bare-stdout-only-messages
  (testing "stdout carries ONLY the rendered messages, blank-line separated --
            no manifest, no summary, no ground-truth"
    (let [printed (atom []) exited (atom nil)]
      (cli/main! ["run" "--seed" "42" "--patients" "2" "--emit" "hl7" "--format" "er7"]
                 {:println-fn #(swap! printed conj %) :exit-fn #(reset! exited %)})
      (is (= 0 @exited))
      (is (= 1 (count @printed)) "exactly one stdout write")
      (let [r (run/run-command {:seed 42 :patients 2 :emit "hl7"})
            expected (string/join "\n\n" (get-in r [:payload :messages]))]
        (is (= expected (first @printed)))
        (is (string/starts-with? (first @printed) "MSH|"))))))

(deftest format-er7-on-a-failing-run-shows-stderr-edn-and-exit-2
  (testing "the exit-code contract stays diagnosable and scriptable under
            er7: an operational error still renders EDN, but to stderr, and
            exit code 2 -- the verification ladder's own demonstration"
    (let [printed (atom []) errs (atom []) exited (atom nil)]
      (cli/main! ["run" "--emit" "hl7" "--format" "er7"] ; no --seed
                 {:println-fn #(swap! printed conj %)
                  :err-println-fn #(swap! errs conj %)
                  :exit-fn #(reset! exited %)})
      (is (= 2 @exited))
      (is (empty? @printed))
      (let [r (read-string (first @errs))]
        (is (= :error (:status r)))
        (is (= :missing-required-opt (:category r)))))))

(deftest help-group-documents-format
  (testing "the run verb's flags cover --format (Task 1)"
    (let [run-verb (first (filter #(= "run" (:verb %)) (:verbs cli/help-group)))
          flag-names (set (map :flag (:flags run-verb)))]
      (is (contains? flag-names "--format")))))

(deftest help-text-mentions-format-and-deprecates-json
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["help"] {:println-fn #(swap! printed conj %) :exit-fn #(reset! exited %)})
    (let [text (first @printed)]
      (is (string/includes? text "--format"))
      (is (string/includes? text "deprecated")))))

(defspec format-er7-stdout-byte-equals-joined-messages 40
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 6)
                 churn? gen/boolean]
    (let [printed (atom []) exited (atom nil)
          base-opts (cond-> {:seed seed :patients patients :emit "hl7"}
                      churn? (assoc :churn true))
          args (cond-> ["run" "--seed" (str seed) "--patients" (str patients)
                        "--emit" "hl7" "--format" "er7"]
                 churn? (conj "--churn"))]
      (cli/main! args {:println-fn #(swap! printed conj %) :exit-fn #(reset! exited %)})
      (let [r (run/run-command base-opts)]
        (and (= 0 @exited)
             (= (string/join "\n\n" (get-in r [:payload :messages]))
                (first @printed)))))))

(defspec exit-code-contract-unchanged-across-formats 40
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 6)]
    (let [run-with-format
          (fn [fmt]
            (let [exited (atom nil)]
              (cli/main! ["run" "--seed" (str seed) "--patients" (str patients)
                          "--emit" "hl7" "--format" fmt]
                         {:println-fn (constantly nil) :exit-fn #(reset! exited %)})
              @exited))]
      (apply = (map run-with-format ["edn" "json" "er7"])))))

;; --- go-public Task 2: version identity ----------------------------------

(deftest dispatch-routes-version
  (let [seen (atom nil)
        r (cli/dispatch-action "version" {}
                               {:version-fn (fn [opts] (reset! seen opts) (result/ok :versioned))})]
    (is (result/ok? r))
    (is (= {} @seen))))

(deftest help-group-documents-version-verb
  (is (contains? (set (map :verb (:verbs cli/help-group))) "version")))

(deftest sim-version-verb-reports-version-and-git-sha
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["version"] {:println-fn #(swap! printed conj %) :exit-fn #(reset! exited %)})
    (is (= 0 @exited))
    (let [r (read-string (first @printed))]
      (is (result/ok? r))
      (is (= version/version (:version (:payload r))))
      (is (contains? (:payload r) :git-sha)))))

(deftest version-flag-is-a-help-style-shortcut
  (testing "--version prints without needing a verb, exits 0, mentions the
            pinned version"
    (let [printed (atom []) exited (atom nil)]
      (cli/main! ["--version"] {:println-fn #(swap! printed conj %) :exit-fn #(reset! exited %)})
      (is (= 0 @exited))
      (is (string/includes? (first @printed) version/version)))))

(deftest manifest-and-version-verb-agree
  (testing "the manifest's default :generator :version and `sim version`'s
            own :version must be the SAME value -- both read
            ehr-testing-sim.version, never independently"
    (let [printed (atom []) exited (atom nil)]
      (cli/main! ["version"] {:println-fn #(swap! printed conj %) :exit-fn #(reset! exited %)})
      (let [verb-version (:version (:payload (read-string (first @printed))))
            manifest-version (get-in (manifest/build {:seed 1 :engine-params {}
                                                       :config {:path "x" :sha256 (apply str (repeat 64 "a"))}
                                                       :invocation {}})
                                      [:generator :version])]
        (is (= verb-version manifest-version))))))
