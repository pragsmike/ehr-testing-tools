(ns ehrt.docs-tooling.usecases-test
  "Tests the use-cases catalog's own schema (docs/use-cases.edn) and
  its pure rendering functions -- mirrors pipeline_test.clj's own
  split (schema tests; rendering tests over already-rendered text, not
  over a real python-generated mermaid diagram, which stays outside
  the hermetic test suite exactly like `make pipeline`'s own mermaid
  step does).

  Rendering tests split with the renderer itself (migration item 14,
  2026-08-02): docs/use-cases.md's generated-index tests
  (render-use-cases-index-md, case->index-line) sit alongside the
  standalone-page tests (case->page-md, case->body-md, cases->pages)
  that replaced the former single-file case->markdown-section/
  render-use-cases-md pair."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [ehrt.docs-tooling.usecases :as usecases]))

(def sample-case
  {:id :sample-case
   :title "Sample Case"
   :audience "Someone."
   :bring "A thing."
   :get "Another thing."
   :maturity :usable
   :equations ["a → b  [Op]"]})

(deftest valid-use-case-passes-test
  (is (usecases/valid-use-case? sample-case)))

(deftest use-case-requires-every-narrative-field-test
  (doseq [k [:id :title :audience :bring :get :maturity :equations]]
    (is (not (usecases/valid-use-case? (dissoc sample-case k))) (str "missing " k " should be invalid"))))

(deftest use-case-requires-a-known-maturity-test
  (is (not (usecases/valid-use-case? (assoc sample-case :maturity :not-a-real-maturity)))))

(deftest all-four-maturities-are-known-test
  (doseq [m [:usable :experimental :illustrative :planned]]
    (is (usecases/valid-use-case? (assoc sample-case :maturity m)))))

;; ---- DOC-4: :commands (the runnable strip) and :no-commands (the stub) ----

(def sample-case-with-strip
  (assoc sample-case
         :commands {:lines ["# a comment a paste survives"
                            "bin/ehr corpus operators --format v2"]
                    :note "Operator ids: [operators.md](operators.md)."}))

(deftest use-case-accepts-a-commands-strip-test
  (is (usecases/valid-use-case? sample-case-with-strip)))

(deftest use-case-accepts-a-strip-without-a-note-test
  (is (usecases/valid-use-case?
       (assoc sample-case :commands {:lines ["bin/ehr help"]}))))

(deftest use-case-accepts-a-no-commands-stub-test
  (is (usecases/valid-use-case?
       (assoc sample-case :no-commands "The Transform stage is yours."))))

(deftest use-case-rejects-both-a-strip-and-a-stub-test
  (is (not (usecases/valid-use-case?
            (assoc sample-case-with-strip :no-commands "and also no commands")))))

(deftest commands-strip-requires-its-lines-test
  (is (not (usecases/valid-use-case?
            (assoc sample-case :commands {:note "a note with no commands"})))))

(deftest use-case-without-either-key-still-validates-test
  (is (usecases/valid-use-case? sample-case)))

(deftest valid-use-cases-document-passes-test
  (is (usecases/valid? {:schema-version 1 :cases [sample-case]})))

(deftest use-cases-document-rejects-a-bad-case-among-good-ones-test
  (is (not (usecases/valid? {:schema-version 1 :cases [sample-case (dissoc sample-case :title)]}))))

;; ---- dogfooding: the committed docs/use-cases.edn must itself validate ----

(deftest committed-use-cases-edn-is-valid-test
  (let [data (edn/read-string (slurp "components/corpus/docs/use-cases.edn"))]
    (is (usecases/valid? data))))

(deftest committed-use-cases-edn-has-twenty-cases-test
  ;; 14 -> 15, SS-2 Step 5: :simulator-traffic-as-intake-source, the
  ;; new eleventh verified command strip (ruling 7).
  ;; 15 -> 16, SS-3 Step 7: :piped-hl7-traffic-as-intake-source, the
  ;; stdin-intake strip, verified for real against the ADR-0011 fixture.
  ;; 16 -> 17, SS-4 Step 6: :mutate-output-piped-straight-into-intake,
  ;; the loopback strip, verified for real (test-integration/
  ;; mutate_stdout_stdin_loopback_test.clj).
  ;; 17 -> 18, ADR-0015 CLI trial-UX session, step 3:
  ;; :generate-sim-traffic, the `corpus generate sim` front-door strip,
  ;; verified for real.
  ;; 18 -> 19, ADR-0015 CLI trial-UX session, step 4:
  ;; :play-a-generated-corpus-back-over-time, the directory-input play
  ;; strip, verified for real.
  ;; 19 -> 20, ADR-0015 CLI trial-UX session, step 5:
  ;; :profile-tier-hl7v2-conformance-gating, the `gate v2-nist` strip
  ;; against the committed CDC fixture, verified for real and timed.
  (let [data (edn/read-string (slurp "components/corpus/docs/use-cases.edn"))]
    (is (= 20 (count (:cases data))))))

(deftest committed-use-cases-edn-has-unique-ids-test
  (let [data (edn/read-string (slurp "components/corpus/docs/use-cases.edn"))
        ids (map :id (:cases data))]
    (is (= (count ids) (count (set ids))))))

;; ---- content conservation (migration item 14): every real case's own
;; narrative/strip/equations text survives the index/per-page split,
;; and every real case is reachable from the generated index ----

(deftest every-real-cases-narrative-fields-survive-into-its-own-page-test
  (let [data (edn/read-string (slurp "components/corpus/docs/use-cases.edn"))
        pages (into {} (map (fn [{:keys [id] :as c}] [id (usecases/case->page-md c "flowchart LR")]))
                    (:cases data))]
    (doseq [{:keys [id title audience bring equations] you-get :get} (:cases data)]
      (let [page (clojure.core/get pages id)]
        (is (str/includes? page title) (str id " page is missing its own title"))
        (is (str/includes? page audience) (str id " page is missing its own :audience"))
        (is (str/includes? page bring) (str id " page is missing its own :bring"))
        (is (str/includes? page you-get) (str id " page is missing its own :get"))
        (doseq [eq equations]
          (is (str/includes? page eq) (str id " page is missing an equation line")))))))

(deftest every-real-case-is-linked-from-the-generated-index-test
  (let [data (edn/read-string (slurp "components/corpus/docs/use-cases.edn"))
        md (usecases/render-use-cases-index-md data)]
    (doseq [{:keys [title] :as c} (:cases data)]
      (is (str/includes? md (str "[" title "](use-cases/" (usecases/case-slug c) ".md)"))
          (str title " is missing from the generated index")))))

;; ---- rendering: one case -> its own standalone page ----

(deftest case-slug-is-the-cases-own-id-test
  (is (= "sample-case" (usecases/case-slug sample-case))))

(deftest case->page-md-includes-title-and-narrative-fields-test
  (let [page (usecases/case->page-md sample-case "flowchart LR\n    a --> b")]
    (is (str/starts-with? page "<!-- GENERATED by `make use-cases`"))
    (is (str/includes? page "# Sample Case"))
    (is (str/includes? page "Someone."))
    (is (str/includes? page "A thing."))
    (is (str/includes? page "Another thing."))
    (is (str/includes? page "usable"))
    (is (str/includes? page "a → b  [Op]"))
    (is (str/includes? page "```mermaid"))
    (is (str/includes? page "flowchart LR"))))

(deftest case->body-md-carries-every-narrative-field-but-no-heading-test
  (let [body (usecases/case->body-md sample-case "flowchart LR")]
    (is (not (str/includes? body "# Sample Case")) "the heading is the page's own job, not the body's")
    (is (str/includes? body "Someone."))
    (is (str/includes? body "A thing."))
    (is (str/includes? body "Another thing."))
    (is (str/includes? body "usable"))))

;; ---- DOC-4: both arms of the **You type:** block ----

(deftest commands-block-renders-the-strip-in-one-fence-test
  (let [block (usecases/case->commands-block sample-case-with-strip)]
    (is (str/includes? block "**You type:**"))
    (is (str/includes? block "```sh\n# a comment a paste survives\nbin/ehr corpus operators --format v2\n```"))
    (is (str/includes? block "[operators.md](operators.md)"))
    (is (= 2 (count (re-seq #"(?m)^```" block)))
        "exactly one fence: a note is prose below it, never inside")))

(deftest commands-block-renders-the-stub-from-bring-test
  (let [block (usecases/case->commands-block
               (assoc sample-case :no-commands "Its Transform stage is {external: true}."))]
    (is (str/includes? block "**You type:** no strip"))
    (is (str/includes? block "A thing.") "the stub is derived from :bring")
    (is (str/includes? block "Its Transform stage is {external: true}."))
    (is (not (str/includes? block "```")) "a stub never fences anything")
    (is (not (str/includes? block "bin/ehr")) "a stub never invents an invocation")))

(deftest commands-block-stub-needs-no-reason-key-test
  (let [block (usecases/case->commands-block sample-case)]
    (is (str/includes? block "**You type:** no strip"))
    (is (str/includes? block "A thing."))))

(deftest case->page-md-puts-the-strip-above-the-equations-test
  (let [page (usecases/case->page-md sample-case-with-strip "flowchart LR")]
    (is (< (str/index-of page "**Maturity:**")
           (str/index-of page "**You type:**")
           (str/index-of page "a → b  [Op]"))
        "strip sits between the narrative fields and the formal grounding")))

;; ---- rendering: docs/use-cases.md, the generated index ----

(deftest case->index-line-links-to-the-cases-own-page-and-cites-its-audience-test
  (let [line (usecases/case->index-line sample-case)]
    (is (= "- [Sample Case](use-cases/sample-case.md) -- Someone." line))))

(deftest render-use-cases-index-md-includes-every-case-title-and-page-link-test
  (let [data {:schema-version 1 :cases [sample-case (assoc sample-case :id :second-case :title "Second Case")]}
        md (usecases/render-use-cases-index-md data)]
    (is (str/includes? md "[Sample Case](use-cases/sample-case.md)"))
    (is (str/includes? md "[Second Case](use-cases/second-case.md)"))
    (is (str/starts-with? md "<!-- GENERATED"))
    (is (not (str/includes? md "```mermaid")) "the index links to each page rather than inlining its diagram")))

;; ---- rendering: docs/use-cases/<id>.md, one page per case ----

(deftest cases->pages-renders-one-page-per-case-under-its-own-id-filename-test
  (let [pages (usecases/cases->pages [sample-case (assoc sample-case :id :second-case :title "Second Case")]
                                      {:sample-case "flowchart LR" :second-case "flowchart LR"})]
    (is (= ["sample-case.md" "second-case.md"] (map :filename pages)))
    (is (str/includes? (:content (first pages)) "# Sample Case"))
    (is (str/includes? (:content (second pages)) "# Second Case"))))

(deftest cases->pages-errors-loudly-on-a-missing-mermaid-entry-test
  (is (thrown? Exception (usecases/cases->pages [sample-case] {}))))
