(ns ehrt.docs-tooling.strip-fresh-test
  "ADR-0129: the two new extraction shapes (:single-fence, :paired) on
  synthetic fixtures, seeded divergence (the check must be able to
  fail before it's trusted to pass), and check-entry's own delegation
  to quickstart-fresh/demo-exerciser-fresh proven live against the two
  real, already-committed pairs. The five new register rows' own
  scripts don't exist yet at this commit (ADR-0129 Step 2/Step 3
  ordering, disclosed in the session record) -- check-entry's own
  handling of an absent script (:ok? false, :script ::script-absent)
  is proven here on a synthetic fixture instead of the real rows, and
  those five real rows are re-checked live, green, in Step 3's own
  test commit once their scripts land."
  (:require [clojure.test :refer [deftest is]]
            [ehrt.docs-tooling.exercised-sources :as reg]
            [ehrt.docs-tooling.strip-fresh :as sf]))

(defn- temp-file!
  [content]
  (let [f (java.io.File/createTempFile "strip-fresh-fixture" ".md")]
    (spit f content)
    (.getAbsolutePath f)))

;; ---- :single-fence extraction, synthetic ----

(deftest single-fence-strips-comments-and-blanks-test
  (let [doc (temp-file! (str "# prelude\n"
                              "```sh\n"
                              "# a comment\n"
                              "\n"
                              "bin/ehrt help\n"
                              "\n"
                              "bin/ehrt corpus generate --seed 1 \\\n"
                              "  --output-dir out/x\n"
                              "```\n"
                              "not part of the fence\n"))]
    (is (= ["bin/ehrt help"
            "bin/ehrt corpus generate --seed 1 \\"
            "  --output-dir out/x"]
           (sf/single-fence-command-lines doc "sh")))))

(deftest single-fence-picks-the-first-matching-language-fence-test
  (let [doc (temp-file! (str "```mermaid\nflowchart LR\n```\n\n"
                              "```sh\nbin/ehrt help\n```\n\n"
                              "```sh\nbin/ehrt should-not-be-seen\n```\n"))]
    (is (= ["bin/ehrt help"] (sf/single-fence-command-lines doc "sh")))))

(deftest single-fence-nil-when-no-matching-fence-test
  (let [doc (temp-file! "```mermaid\nflowchart LR\n```\n")]
    (is (nil? (sf/single-fence-command-lines doc "sh")))))

;; ---- :paired extraction, synthetic ----

(deftest paired-fence-immediately-followed-by-different-language-pairs-test
  (let [doc (temp-file! (str "```bash\nbin/ehrt gate fhir out/x\n```\n\n"
                              "```clojure\n{:status :ok}\n```\n"))]
    (is (= [{:command-lines ["bin/ehrt gate fhir out/x"]
             :output-lines ["{:status :ok}"]}]
           (sf/command-output-pairs doc "bash")))))

(deftest paired-fence-followed-by-prose-is-command-only-test
  (let [doc (temp-file! (str "```bash\nbin/ehrt corpus generate\n```\n\n"
                              "Some prose, not an output fence.\n\n"
                              "```clojure\n{:status :ok}\n```\n"))]
    (is (= [{:command-lines ["bin/ehrt corpus generate"] :output-lines nil}]
           (sf/command-output-pairs doc "bash")))))

(deftest paired-fence-followed-by-same-language-fence-is-command-only-test
  (let [doc (temp-file! (str "```bash\nbin/ehrt help\n```\n\n"
                              "```bash\nbin/ehrt corpus generate\n```\n"))]
    (is (= [{:command-lines ["bin/ehrt help"] :output-lines nil}
            {:command-lines ["bin/ehrt corpus generate"] :output-lines nil}]
           (sf/command-output-pairs doc "bash")))))

;; ---- check-entry: seeded divergence, must fail before trusted to pass ----

(deftest check-entry-single-fence-catches-an-altered-script-line-test
  (let [doc (temp-file! "```sh\nbin/ehrt help\nbin/ehrt corpus generate\n```\n")
        script (temp-file!
                (str "# BEGIN t commands (verbatim from fixture)\n"
                     "expect 0 bin/ehrt help\n"
                     "expect 0 bin/ehrt corpus generate --typo\n"
                     "# END t commands\n"))
        result (sf/check-entry {:source doc :script script :extraction :single-fence
                                 :fence-lang "sh"
                                 :marker-open "# BEGIN t commands (verbatim from fixture)"
                                 :marker-close "# END t commands"})]
    (is (false? (:ok? result)))
    (is (= 1 (:index (:divergence result))))
    (is (= "bin/ehrt corpus generate" (:readme (:divergence result))))
    (is (= "bin/ehrt corpus generate --typo" (:script (:divergence result))))))

(deftest check-entry-single-fence-agrees-when-identical-test
  (let [doc (temp-file! "```sh\nbin/ehrt help\n```\n")
        script (temp-file!
                (str "# BEGIN t commands (verbatim from fixture)\n"
                     "expect 0 bin/ehrt help\n"
                     "# END t commands\n"))
        result (sf/check-entry {:source doc :script script :extraction :single-fence
                                 :fence-lang "sh"
                                 :marker-open "# BEGIN t commands (verbatim from fixture)"
                                 :marker-close "# END t commands"})]
    (is (true? (:ok? result)))
    (is (= 1 (:readme-count result) (:script-count result)))))

(deftest check-entry-paired-catches-an-altered-script-line-test
  (let [doc (temp-file! (str "```bash\nbin/ehrt gate fhir out/x\n```\n\n"
                              "```clojure\n{:status :ok}\n```\n"))
        script (temp-file!
                (str "# BEGIN t commands (verbatim from fixture)\n"
                     "expect 0 bin/ehrt gate fhir out/y\n"
                     "# END t commands\n"))
        result (sf/check-entry {:source doc :script script :extraction :paired
                                 :fence-lang "bash"
                                 :marker-open "# BEGIN t commands (verbatim from fixture)"
                                 :marker-close "# END t commands"})]
    (is (false? (:ok? result)))
    (is (= "bin/ehrt gate fhir out/x" (:readme (:divergence result))))
    (is (= "bin/ehrt gate fhir out/y" (:script (:divergence result))))))

(deftest check-entry-reports-absent-script-test
  (let [doc (temp-file! "```sh\nbin/ehrt help\n```\n")
        result (sf/check-entry {:source doc :script "/nonexistent/bin/foo"
                                 :extraction :single-fence :fence-lang "sh"
                                 :marker-open "# BEGIN" :marker-close "# END"})]
    (is (false? (:ok? result)))
    (is (= 0 (:script-count result)))
    (is (= ::sf/script-absent (:script (:divergence result))))))

;; ---- check-entry: the two real, already-committed pairs, live ----

(deftest check-entry-delegates-live-to-quickstart-fresh-test
  (let [rows (reg/load-registry)
        row (first (filter #(= "bin/quickstart-demo" (:script %)) rows))
        result (sf/check-entry row)]
    (is (true? (:ok? result)) (str "divergence: " (:divergence result)))
    (is (= 15 (:readme-count result)))))

(deftest check-entry-delegates-live-to-demo-exerciser-fresh-test
  (let [rows (reg/load-registry)
        row (first (filter #(= "bin/demo-exerciser-ed-tuesday" (:script %)) rows))
        result (sf/check-entry row)]
    (is (true? (:ok? result)) (str "divergence: " (:divergence result)))
    (is (= 21 (:readme-count result)))))

;; ---- check-entry: the five new rows, live, once their own scripts
;; land in this same commit (Step 3, ADR-0129) -- RED against these
;; same rows was witnessed and pasted into the session record before
;; the scripts existed (Step 2's own commit); these are the promised
;; five new freshness test cases, co-landed with the scripts per this
;; session's own disclosed discretion (committing a red test would
;; break the "make test green before every push" fence) ----

(defn- check-script-row
  [script-name]
  (let [rows (reg/load-registry)
        row (first (filter #(= script-name (:script %)) rows))]
    (sf/check-entry row)))

(deftest check-entry-live-usecase-judge-tier-calibration-test
  (let [result (check-script-row "bin/usecase-judge-tier-calibration")]
    (is (true? (:ok? result)) (str "divergence: " (:divergence result)))
    (is (= 9 (:readme-count result) (:script-count result)))))

(deftest check-entry-live-usecase-profile-tier-v2-test
  (let [result (check-script-row "bin/usecase-profile-tier-v2")]
    (is (true? (:ok? result)) (str "divergence: " (:divergence result)))
    (is (= 3 (:readme-count result) (:script-count result)))))

(deftest check-entry-live-usecase-acceptance-qa-test
  (let [result (check-script-row "bin/usecase-acceptance-qa")]
    (is (true? (:ok? result)) (str "divergence: " (:divergence result)))
    (is (= 6 (:readme-count result) (:script-count result)))))

(deftest check-entry-live-usecase-regression-baselining-test
  (let [result (check-script-row "bin/usecase-regression-baselining")]
    (is (true? (:ok? result)) (str "divergence: " (:divergence result)))
    (is (= 4 (:readme-count result) (:script-count result)))))

(deftest check-entry-live-readme-what-you-get-test
  (let [result (check-script-row "bin/readme-what-you-get")]
    (is (true? (:ok? result)) (str "divergence: " (:divergence result)))
    (is (= 6 (:readme-count result) (:script-count result)))))

;; ---- extraction against the real, live use-case pages and README --
;; -- proves the extraction layer alone is correct against the real
;; committed docs, independent of whether each row's own script exists
;; yet (Step 3 lands the five scripts; these counts are the ones each
;; new bin/ script's own BEGIN/END block must match line-for-line).

(deftest live-extraction-counts-for-the-five-new-sources-test
  (is (= 9 (count (sf/single-fence-command-lines
                    "docs/use-cases/judge-tier-calibration-studies.md" "sh"))))
  (is (= 3 (count (sf/single-fence-command-lines
                    "docs/use-cases/profile-tier-hl7v2-conformance-gating.md" "sh"))))
  (is (= 6 (count (sf/single-fence-command-lines
                    "docs/use-cases/acceptance-qa-of-vendor-corpora.md" "sh"))))
  (is (= 4 (count (sf/single-fence-command-lines
                    "docs/use-cases/regression-baselining.md" "sh"))))
  (let [pairs (filter :output-lines (sf/command-output-pairs "README.md" "bash"))]
    (is (= 2 (count pairs)))
    (is (= 6 (count (mapcat :command-lines pairs))))))

;; ---- paired-output comparison (bin/readme-what-you-get's own
;; runtime check): elision-tolerant subset match ----

(deftest subset-match-allows-extra-map-keys-test
  (is (true? (sf/subset-match? {:status :ok} {:status :ok :engine {:name "x"}}))))

(deftest subset-match-catches-a-changed-value-test
  (is (false? (sf/subset-match? {:status :ok} {:status :rejected}))))

(deftest subset-match-requires-exact-vector-length-test
  (is (false? (sf/subset-match? {:findings [{:code "a"}]} {:findings []})))
  (is (true? (sf/subset-match? {:findings [{:code "a"}]} {:findings [{:code "a" :extra 1}]}))))

(deftest subset-match-recurses-into-nested-maps-test
  (is (true? (sf/subset-match? {:payload {:totals {:pass 1}}}
                                {:payload {:totals {:pass 1 :rejected 0}} :extra-top-key 1}))))

(deftest parse-elided-edn-strips-trailing-ellipsis-test
  (is (= {:a 1 :b {:c 2}}
         (sf/parse-elided-edn ["{:a 1," " :b {:c 2, ...}}"]))))

(deftest paired-output-check-matches-real-live-readme-fence-against-real-captured-output-test
  (let [expected-lines (:output-lines
                         (first (filter :output-lines (sf/command-output-pairs "README.md" "bash"))))
        expected (sf/parse-elided-edn expected-lines)
        real-actual {:status :ok
                     :payload {:run {:gate :fhir :path "test-fixtures/fhir/storefront-patient.json"}
                               :totals {:pass 1 :rejected 0 :indeterminate 0 :no-verdict 0}
                               :by-code {"invariant" 1}
                               :files [{:path "test-fixtures/fhir/storefront-patient.json"
                                        :verdict :pass
                                        :finding-count 1
                                        :findings [{:severity :warning :code "invariant"
                                                     :locator {:format :fhir :path "Bundle.entry[0].resource"}
                                                     :message "Constraint failed: dom-6: 'A resource should have narrative for robust management' (defined in http://hl7.org/fhir/StructureDefinition/DomainResource) (Best Practice Recommendation)"
                                                     :engine {:name "fhir-validator-cli" :version "6.9.12"}
                                                     :disposition :pass
                                                     :native-ref {:expression ["Bundle.entry[0].resource/*Patient/storefront-patient*/"]}}]}]}}]
    (is (true? (sf/subset-match? expected real-actual))
        "the real gate-fhir run's own captured output (this session's own live invocation) must satisfy README.md's own first What-you-get fence")))
