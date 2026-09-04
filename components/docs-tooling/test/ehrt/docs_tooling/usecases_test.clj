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

(deftest committed-use-cases-edn-has-twenty-three-cases-test
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
  ;; 20 -> 21, ADR-0112: :supply-batch-straddling-traffic, the
  ;; batch-straddle transport-realism strip -- commands are ADR-0111's
  ;; own witnessed demo run, reused verbatim, not re-executed this
  ;; session.
  ;; 21 -> 22, event-log contract arc Step 4:
  ;; :custom-emitter-from-the-event-log, the ground-truth-log-to-your-
  ;; own-format strip. Exercised FROM BIRTH -- page, worked example
  ;; (bin/example-custom-emitter) and exerciser
  ;; (bin/usecase-custom-emitter, registered in exercised-sources.edn)
  ;; all landed in this same commit, satisfying the D8-5 battery's own
  ;; proposed reader-path rule by construction rather than retrofit.
  ;; Its strip was re-executed live, and caught a real defect on the
  ;; first run: the taught redirect wrote into an out/ subdirectory it
  ;; never created (the R-F5 class the battery had just fixed), so a
  ;; taught `mkdir -p` now leads the fence.
  ;; 22 -> 23, author ruling of 2026-09-04 (the prime audience):
  ;; :ground-truth-as-a-test-oracle, the ground-truth log used as a
  ;; semantic oracle for QA of a consumer's OWN downstream system.
  ;; FIRST in :cases and first in :start-here, both by that ruling's
  ;; prominence rule -- nothing existing was renumbered, folded or
  ;; demoted to make room. Exercised from birth, like the case above
  ;; it: bin/usecase-ground-truth-oracle and its exercised-sources row
  ;; land in the same arc. The record is a session record, not an ADR:
  ;; .agents/session-records/2026-09-04-prime-audience.md.
  (let [data (edn/read-string (slurp "components/corpus/docs/use-cases.edn"))]
    (is (= 23 (count (:cases data))))))

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

;; ---- the "Start here" actor table (ADR-0146, finding U-9) ----
;;
;; The emitter author's own cold walk found the catalog is a flat list of
;; twenty-two undifferentiated audience sentences, so a reader with ONE
;; question reads all twenty-two to find their row -- theirs was row 19.
;; These tests hold the fix's contract: rows are DATA, every row's :case
;; must RESOLVE (a table that routes to a nonexistent page is worse than
;; no table), and the table renders above the full list.

(deftest start-here-row-requires-a-question-and-a-case-test
  (is (usecases/valid-start-here-row? {:question "I have my own format" :case :sample-case}))
  (is (not (usecases/valid-start-here-row? {:question "no case"})))
  (is (not (usecases/valid-start-here-row? {:case :sample-case})))
  (is (not (usecases/valid-start-here-row? {:question "case must be a keyword" :case "sample-case"}))))

(deftest start-here-is-optional-so-a-minimal-document-stays-valid-test
  (is (usecases/valid? {:schema-version 1 :cases [sample-case]})))

(deftest start-here-rows-must-name-a-real-case-id-test
  (testing "a row naming a present case is valid"
    (is (usecases/valid? {:schema-version 1
                          :cases [sample-case]
                          :start-here [{:question "Q" :case :sample-case}]})))
  (testing "a row naming a case that does not exist is REJECTED, not silently dropped"
    (is (not (usecases/valid? {:schema-version 1
                               :cases [sample-case]
                               :start-here [{:question "Q" :case :no-such-case}]})))))

(deftest committed-use-cases-edn-declares-a-start-here-table-whose-rows-all-resolve-test
  (let [data (edn/read-string (slurp "components/corpus/docs/use-cases.edn"))
        ids (set (map :id (:cases data)))]
    (is (seq (:start-here data)) "the committed catalog declares a Start here table")
    (doseq [{:keys [question] c :case} (:start-here data)]
      (is (contains? ids c) (str "Start here row " (pr-str question) " routes to unknown case " c)))))

(deftest committed-start-here-table-carries-the-emitter-authors-own-row-test
  ;; The acceptance gate for ADR-0146's finding U-9: this actor's own
  ;; question is a table row on the catalog's first screen, not row 19
  ;; of a flat list. If a future edit drops it, this fails by name.
  (let [data (edn/read-string (slurp "components/corpus/docs/use-cases.edn"))]
    (is (some #(= :custom-emitter-from-the-event-log (:case %)) (:start-here data)))))

(deftest committed-start-here-table-leads-with-the-prime-audiences-own-row-test
  ;; The acceptance gate for the author ruling of 2026-09-04: the prime
  ;; audience's question is the FIRST row of the Start here table, not
  ;; merely present in it. Prominence was the whole of the ruling, so a
  ;; membership assertion would gate the wrong thing -- a later edit
  ;; that kept the row and moved it down would pass one and fail this.
  (let [data (edn/read-string (slurp "components/corpus/docs/use-cases.edn"))]
    (is (= :ground-truth-as-a-test-oracle (:case (first (:start-here data))))
        (str "the prime audience's row must LEAD the Start here table (author ruling, "
             "2026-09-04, docs/dev/AUDIENCES.md segment 7). Found "
             (pr-str (:case (first (:start-here data))))))
    (is (= :ground-truth-as-a-test-oracle (:id (first (:cases data))))
        "and lead the catalog itself, for the same reason")))

(deftest render-start-here-md-is-empty-when-no-rows-are-declared-test
  (is (= "" (usecases/render-start-here-md {:cases [sample-case]}))))

(deftest render-start-here-md-links-each-row-to-its-cases-own-page-test
  (let [md (usecases/render-start-here-md
             {:cases [sample-case]
              :start-here [{:question "I have my own format" :case :sample-case}]})]
    (is (str/includes? md "I have my own format"))
    (is (str/includes? md "[Sample Case](use-cases/sample-case.md)"))))

(deftest index-puts-the-start-here-table-above-the-full-case-list-test
  (let [md (usecases/render-use-cases-index-md
             {:cases [sample-case]
              :start-here [{:question "I have my own format" :case :sample-case}]})
        table-at (str/index-of md "I have my own format")
        list-at (str/index-of md "- [Sample Case](use-cases/sample-case.md)")]
    (is (some? table-at))
    (is (some? list-at))
    (is (< table-at list-at) "the actor table must come before the flat list, or it fixes nothing")))
