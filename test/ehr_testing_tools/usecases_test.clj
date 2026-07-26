(ns ehr-testing-tools.usecases-test
  "Tests the use-cases catalog's own schema (docs/use-cases.edn) and
  its pure rendering functions -- mirrors pipeline_test.clj's own
  split (schema tests; rendering tests over already-rendered text, not
  over a real python-generated mermaid diagram, which stays outside
  the hermetic test suite exactly like `make pipeline`'s own mermaid
  step does)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [ehr-testing-tools.usecases :as usecases]))

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
  (let [data (edn/read-string (slurp "docs/use-cases.edn"))]
    (is (usecases/valid? data))))

(deftest committed-use-cases-edn-has-fourteen-cases-test
  (let [data (edn/read-string (slurp "docs/use-cases.edn"))]
    (is (= 14 (count (:cases data))))))

(deftest committed-use-cases-edn-has-unique-ids-test
  (let [data (edn/read-string (slurp "docs/use-cases.edn"))
        ids (map :id (:cases data))]
    (is (= (count ids) (count (set ids))))))

;; ---- rendering: one case -> a markdown section ----

(deftest case->markdown-section-includes-title-and-narrative-fields-test
  (let [section (usecases/case->markdown-section sample-case "flowchart LR\n    a --> b")]
    (is (str/includes? section "Sample Case"))
    (is (str/includes? section "Someone."))
    (is (str/includes? section "A thing."))
    (is (str/includes? section "Another thing."))
    (is (str/includes? section "usable"))
    (is (str/includes? section "a → b  [Op]"))
    (is (str/includes? section "```mermaid"))
    (is (str/includes? section "flowchart LR"))))

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

(deftest case->markdown-section-puts-the-strip-above-the-equations-test
  (let [section (usecases/case->markdown-section sample-case-with-strip "flowchart LR")]
    (is (< (str/index-of section "**Maturity:**")
           (str/index-of section "**You type:**")
           (str/index-of section "a → b  [Op]"))
        "strip sits between the narrative fields and the formal grounding")))

(deftest render-use-cases-md-includes-every-case-id-and-title-test
  (let [data {:schema-version 1 :cases [sample-case (assoc sample-case :id :second-case :title "Second Case")]}
        md (usecases/render-use-cases-md data {:sample-case "flowchart LR" :second-case "flowchart LR"})]
    (is (str/includes? md "Sample Case"))
    (is (str/includes? md "Second Case"))
    (is (str/starts-with? md "<!-- GENERATED"))))

(deftest render-use-cases-md-errors-loudly-on-a-missing-mermaid-entry-test
  (let [data {:schema-version 1 :cases [sample-case]}]
    (is (thrown? Exception (usecases/render-use-cases-md data {})))))
