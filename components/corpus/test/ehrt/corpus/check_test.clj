(ns ehrt.corpus.check-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus.check.schemas :as schemas]
            [ehrt.judge.interface :as judge]
            [ehrt.corpus.check :as check])
  (:import [java.io File]))

(defn- temp-dir* []
  (let [f (File/createTempFile "check-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

(def bundle-json
  "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"p1\",\"gender\":\"female\"}}]}")

(def bundle-json-different-gender
  "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"p1\",\"gender\":\"male\"}}]}")

;; ---- assertion vocabulary: valid/invalid shapes ----

(deftest matches-expected-assertion-is-valid-test
  (is (check/valid-assertion? {:kind :matches-expected})))

(deftest present-and-absent-assertions-require-a-locator-test
  (is (check/valid-assertion? {:kind :present :locator {:format :fhir :path "entry[0].resource.gender"}}))
  (is (check/valid-assertion? {:kind :absent :locator {:format :fhir :path "entry[0].resource.gender"}}))
  (is (not (check/valid-assertion? {:kind :present}))))

(deftest value-assertion-requires-locator-and-expected-test
  (is (check/valid-assertion? {:kind :value :locator {:format :fhir :path "entry[0].resource.gender"} :expected "female"}))
  (is (not (check/valid-assertion? {:kind :value :locator {:format :fhir :path "x"}}))))

(deftest count-assertion-requires-op-and-value-test
  (is (check/valid-assertion? {:kind :count :locator {:format :fhir :path "entry"} :op := :value 1}))
  (is (check/valid-assertion? {:kind :count :locator {:format :fhir :path "entry"} :op :<= :value 5}))
  (is (not (check/valid-assertion? {:kind :count :locator {:format :fhir :path "entry"} :op :not-an-op :value 1}))))

(deftest schema-assertion-requires-a-malli-ref-test
  (is (check/valid-assertion? {:kind :schema :malli {:id :fhir-resource-shape :version "1"}}))
  (is (not (check/valid-assertion? {:kind :schema}))))

(deftest unknown-assertion-kind-is-invalid-test
  (is (not (check/valid-assertion? {:kind :not-a-real-kind}))))

;; ---- golden equivalence (:matches-expected) ----

(deftest matches-expected-identical-corpora-pass-test
  (let [cand (temp-dir*) exp (temp-dir*)]
    (spit (io/file cand "a.json") bundle-json)
    (spit (io/file exp "a.json") bundle-json)
    (let [r (check/check-corpus {:candidate-dir cand :expected-dir exp})]
      (is (kernel/ok? r))
      (is (= {:pass 1 :rejected 0 :indeterminate 0 :no-verdict 0} (:totals (:payload r)))))))

(deftest matches-expected-differing-corpora-reject-with-a-locator-path-test
  (let [cand (temp-dir*) exp (temp-dir*)]
    (spit (io/file cand "a.json") bundle-json-different-gender)
    (spit (io/file exp "a.json") bundle-json)
    (let [r (check/check-corpus {:candidate-dir cand :expected-dir exp})]
      (is (kernel/rejected? r))
      (is (= :check-rejected (:category r)))
      (let [rpt (:payload r)
            finding (first (:findings (first (filter #(= "a.json" (:path %)) (:files rpt)))))]
        (is (= {:pass 0 :rejected 1 :indeterminate 0 :no-verdict 0} (:totals rpt)))))))

(deftest matches-expected-canonicalizer-makes-an-inequivalent-pair-equivalent-test
  ;; The point of the canonicalizer-list design: two files that differ
  ;; byte-for-byte can still be declared equivalent once a registered
  ;; canonicalizer strips the volatile field they differ on --
  ;; equivalence IS canonical equality, not byte equality.
  (let [cand (temp-dir*) exp (temp-dir*)
        strip-id (fn [data] (update-in data ["entry" 0 "resource"] dissoc "id"))]
    (kernel/register! {:id :test-strip-patient-id :version "1" :format :edn
                           :fn strip-id :docstring "test-only: drops entry[0].resource.id"})
    (spit (io/file cand "a.json") "{\"entry\":[{\"resource\":{\"id\":\"cand-id\",\"gender\":\"female\"}}]}")
    (spit (io/file exp "a.json") "{\"entry\":[{\"resource\":{\"id\":\"exp-id\",\"gender\":\"female\"}}]}")
    (let [without-canon (check/check-corpus {:candidate-dir cand :expected-dir exp})
          with-canon (check/check-corpus {:candidate-dir cand :expected-dir exp
                                           :canonicalizers [[:test-strip-patient-id "1"]]})]
      (is (kernel/rejected? without-canon) "byte-different without a canonicalizer -- must reject")
      (is (kernel/ok? with-canon) "equivalent once the volatile id field is canonicalized away"))))

(deftest matches-expected-reports-missing-and-extra-files-test
  (let [cand (temp-dir*) exp (temp-dir*)]
    (spit (io/file cand "shared.json") bundle-json)
    (spit (io/file cand "extra.json") bundle-json)
    (spit (io/file exp "shared.json") bundle-json)
    (spit (io/file exp "missing.json") bundle-json)
    (let [r (check/check-corpus {:candidate-dir cand :expected-dir exp})
          rpt (:payload r)
          codes-by-path (into {} (map (juxt :path :verdict)) (:files rpt))]
      (is (kernel/rejected? r))
      (is (= :rejected (get codes-by-path "extra.json")))
      (is (= :rejected (get codes-by-path "missing.json")))
      (is (= :pass (get codes-by-path "shared.json"))))))

(deftest matches-expected-pair-by-hash-matches-identical-content-regardless-of-filename-test
  (let [cand (temp-dir*) exp (temp-dir*)]
    (spit (io/file cand "candidate-name.json") bundle-json)
    (spit (io/file exp "expected-name.json") bundle-json)
    (let [r (check/check-corpus {:candidate-dir cand :expected-dir exp :pair-by :hash})]
      (is (kernel/ok? r))
      (is (= 1 (count (:files (:payload r))))))))

(deftest matches-expected-default-assertions-when-omitted-with-expected-dir-test
  (let [cand (temp-dir*) exp (temp-dir*)]
    (spit (io/file cand "a.json") bundle-json)
    (spit (io/file exp "a.json") bundle-json)
    ;; No :assertions key at all -- defaults to [{:kind :matches-expected}].
    (is (kernel/ok? (check/check-corpus {:candidate-dir cand :expected-dir exp})))))

;; ---- per-file assertions ----

(deftest present-assertion-red-green-test
  (let [cand (temp-dir*)
        _ (spit (io/file cand "a.json") bundle-json)
        assertions [{:kind :present :locator {:format :fhir :path "entry[0].resource.gender"}}]
        ok (check/check-corpus {:candidate-dir cand :assertions assertions})
        _ (spit (io/file cand "a.json") "{\"entry\":[{\"resource\":{}}]}")
        rejected (check/check-corpus {:candidate-dir cand :assertions assertions})]
    (is (kernel/ok? ok))
    (is (kernel/rejected? rejected))))

(deftest absent-assertion-red-green-test
  (let [cand (temp-dir*)
        assertions [{:kind :absent :locator {:format :fhir :path "entry[0].resource.deceasedBoolean"}}]
        _ (spit (io/file cand "a.json") bundle-json)
        ok (check/check-corpus {:candidate-dir cand :assertions assertions})
        _ (spit (io/file cand "a.json") "{\"entry\":[{\"resource\":{\"deceasedBoolean\":true}}]}")
        rejected (check/check-corpus {:candidate-dir cand :assertions assertions})]
    (is (kernel/ok? ok))
    (is (kernel/rejected? rejected))))

(deftest value-assertion-red-green-test
  (let [cand (temp-dir*)
        _ (spit (io/file cand "a.json") bundle-json)
        assertions [{:kind :value :locator {:format :fhir :path "entry[0].resource.gender"} :expected "female"}]
        ok (check/check-corpus {:candidate-dir cand :assertions assertions})
        wrong-value-assertions [{:kind :value :locator {:format :fhir :path "entry[0].resource.gender"} :expected "male"}]
        rejected (check/check-corpus {:candidate-dir cand :assertions wrong-value-assertions})]
    (is (kernel/ok? ok))
    (is (kernel/rejected? rejected))))

(deftest count-assertion-red-green-test
  (let [cand (temp-dir*)
        _ (spit (io/file cand "a.json") bundle-json)
        exactly-one [{:kind :count :locator {:format :fhir :path "entry"} :op := :value 1}]
        exactly-two [{:kind :count :locator {:format :fhir :path "entry"} :op := :value 2}]
        ok (check/check-corpus {:candidate-dir cand :assertions exactly-one})
        rejected (check/check-corpus {:candidate-dir cand :assertions exactly-two})]
    (is (kernel/ok? ok))
    (is (kernel/rejected? rejected))))

(deftest count-assertion-supports-lte-and-gte-test
  (let [cand (temp-dir*)
        _ (spit (io/file cand "a.json") bundle-json)]
    (is (kernel/ok? (check/check-corpus
                      {:candidate-dir cand
                       :assertions [{:kind :count :locator {:format :fhir :path "entry"} :op :<= :value 5}]})))
    (is (kernel/ok? (check/check-corpus
                      {:candidate-dir cand
                       :assertions [{:kind :count :locator {:format :fhir :path "entry"} :op :>= :value 1}]})))
    (is (kernel/rejected? (check/check-corpus
                            {:candidate-dir cand
                             :assertions [{:kind :count :locator {:format :fhir :path "entry"} :op :>= :value 2}]})))))

(deftest schema-assertion-red-green-test
  (let [cand (temp-dir*)
        assertions [{:kind :schema :malli {:id :fhir-resource-shape :version "1"}}]
        _ (spit (io/file cand "a.json") bundle-json)
        ok (check/check-corpus {:candidate-dir cand :assertions assertions})
        _ (spit (io/file cand "a.json") "{\"noResourceTypeHere\":true}")
        rejected (check/check-corpus {:candidate-dir cand :assertions assertions})]
    (is (kernel/ok? ok))
    (is (kernel/rejected? rejected))))

;; ---- shared finding envelope + report reuse ----

(deftest findings-validate-against-the-shared-finding-envelope-test
  (let [engine {:name "check" :version "v1"}
        assertion {:kind :present :locator {:format :fhir :path "entry[0].resource.gender"}}
        f (first (check/assertion-findings assertion {"entry" [{"resource" {}}]} engine))]
    (is (judge/finding-valid? f))
    (is (= "check" (:name (:engine f))))))

(deftest report-validates-against-gate-report-schema-test
  (let [cand (temp-dir*) exp (temp-dir*)]
    (spit (io/file cand "a.json") bundle-json)
    (spit (io/file exp "a.json") bundle-json)
    (is (judge/report-valid? (:payload (check/check-corpus {:candidate-dir cand :expected-dir exp}))))))

;; ---- gate-kind law: never modifies the datum it judges ----

(deftest check-does-not-modify-candidate-or-expected-files-test
  (let [cand (temp-dir*) exp (temp-dir*)
        cand-file (io/file cand "a.json") exp-file (io/file exp "a.json")]
    (spit cand-file bundle-json-different-gender)
    (spit exp-file bundle-json)
    (let [before-cand (slurp cand-file) before-exp (slurp exp-file)]
      (check/check-corpus {:candidate-dir cand :expected-dir exp})
      (is (= before-cand (slurp cand-file)))
      (is (= before-exp (slurp exp-file))))))

(deftest check-does-not-modify-files-under-per-file-assertions-test
  (let [cand (temp-dir*)
        cand-file (io/file cand "a.json")]
    (spit cand-file bundle-json)
    (let [before (slurp cand-file)]
      (check/check-corpus {:candidate-dir cand
                            :assertions [{:kind :present :locator {:format :fhir :path "entry[0].resource.gender"}}]})
      (is (= before (slurp cand-file))))))
