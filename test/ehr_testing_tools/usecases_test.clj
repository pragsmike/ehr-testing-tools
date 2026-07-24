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

(deftest render-use-cases-md-includes-every-case-id-and-title-test
  (let [data {:schema-version 1 :cases [sample-case (assoc sample-case :id :second-case :title "Second Case")]}
        md (usecases/render-use-cases-md data {:sample-case "flowchart LR" :second-case "flowchart LR"})]
    (is (str/includes? md "Sample Case"))
    (is (str/includes? md "Second Case"))
    (is (str/starts-with? md "<!-- GENERATED"))))

(deftest render-use-cases-md-errors-loudly-on-a-missing-mermaid-entry-test
  (let [data {:schema-version 1 :cases [sample-case]}]
    (is (thrown? Exception (usecases/render-use-cases-md data {})))))
