(ns ehrt.corpus.mutate-test
  "Loading corpus.operators registers the seed catalog as a side
  effect (same convention as corpus.canonicalizers) -- requiring it
  below is enough."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus.lineage :as lineage]
            [ehrt.corpus.diff :as diff]
            [ehrt.corpus-io.er7 :as er7]
            [ehrt.corpus.operators :as operators]
            [ehrt.corpus.mutate :as mutate]))

(def sample-bundle
  {"resourceType" "Bundle"
   "type" "transaction"
   "entry" [{"resource" {"resourceType" "Patient" "id" "p1"
                          "gender" "female" "birthDate" "1985-03-12" "active" true}}]})

(defn- op [id] (operators/lookup id "1"))

(deftest mutate-happy-path-returns-mutant-and-lineage-test
  (let [r (mutate/mutate sample-bundle (op :remove-required-element)
                          {:format :fhir :path "entry[0].resource.gender"})]
    (is (kernel/ok? r))
    (let [{:keys [mutant lineage]} (:payload r)]
      (is (not (contains? (get-in mutant ["entry" 0 "resource"]) "gender")))
      (is (lineage/valid? lineage))
      (is (lineage/valid-content-hash? lineage))
      (is (= :mutate (:stage lineage)))
      (is (= :remove-required-element (:id (:operator (:transformation lineage)))))
      (is (= "1" (:version (:operator (:transformation lineage)))))
      (is (= {:type :violates :target (:target (:contract (op :remove-required-element)))}
             (:contract (:transformation lineage)))))))

(deftest mutate-does-not-touch-the-original-data-test
  (let [before sample-bundle
        _ (mutate/mutate sample-bundle (op :duplicate-element)
                          {:format :fhir :path "entry[0].resource.gender"})]
    (is (= before sample-bundle) "base-data must be untouched -- Clojure data is immutable, but pinning this documents the intent")))

(deftest mutate-rejects-a-locator-pointing-nowhere-test
  (let [r (mutate/mutate sample-bundle (op :remove-required-element)
                          {:format :fhir :path "entry[0].resource.nonExistentField"})]
    (is (kernel/rejected? r))
    (is (= :locator-not-found (:category r)))))

(deftest mutate-propagates-a-malformed-locator-path-test
  (let [r (mutate/mutate sample-bundle (op :remove-required-element)
                          {:format :fhir :path "entry[bad]"})]
    (is (kernel/rejected? r))
    (is (= :invalid-fhir-path (:category r)))))

(deftest mutate-lineage-parent-and-produced-are-content-hashes-of-canonical-json-test
  (let [r (mutate/mutate sample-bundle (op :invalid-code-value)
                          {:format :fhir :path "entry[0].resource.gender"})
        {:keys [mutant lineage]} (:payload r)]
    (is (= (mutate/content-hash sample-bundle) (:parent lineage)))
    (is (= (mutate/content-hash mutant) (:produced lineage)))
    (is (not= (:parent lineage) (:produced lineage)))))

;; ---- the Mutate stage law: intended-diff-only. diff(canon(base),
;; canon(mutant)) touches exactly the declared locator target and
;; nothing else. Property-tested across generated sample data --
;; hermetic and fast, unlike Step 4.4's report, which runs the same
;; operators against a real freshly-generated population and reports
;; those results as evidence (see the session report); baking a
;; dependency on that ephemeral, gitignored output into the permanent
;; test suite would make `make test` non-hermetic. ----

(def gender-gen (gen/elements ["male" "female" "other" "unknown"]))
(def date-gen (gen/let [y (gen/choose 1920 2020) m (gen/choose 1 12) d (gen/choose 1 28)]
                (format "%04d-%02d-%02d" y m d)))
(def id-gen (gen/fmap #(apply str %) (gen/vector gen/char-alphanumeric 8 20)))

(def patient-resource-gen
  (gen/let [gender gender-gen
            birth-date date-gen
            active gen/boolean
            pid id-gen]
    {"resourceType" "Patient" "id" pid "gender" gender "birthDate" birth-date "active" active}))

(def filler-entry-gen
  (gen/let [oid id-gen]
    {"resource" {"resourceType" "Observation" "id" oid "status" "final"}}))

(def bundle-with-patient-at-index-gen
  (gen/let [patient patient-resource-gen
            fillers (gen/vector filler-entry-gen 0 3)
            idx (gen/choose 0 (count fillers))]
    (let [entries (vec (concat (take idx fillers) [{"resource" patient}] (drop idx fillers)))]
      {:bundle {"resourceType" "Bundle" "type" "transaction" "entry" entries}
       :idx idx})))

(def field-targeting-operator
  {:remove-required-element "gender"
   :duplicate-element "gender"
   :invalid-code-value "gender"
   :malformed-date "birthDate"
   :wrong-type-value "active"})

(deftest mutate-law-intended-diff-only-property-test
  (doseq [[operator-id field] field-targeting-operator]
    (let [check-result
          (tc/quick-check 50
            (prop/for-all [{:keys [bundle idx]} bundle-with-patient-at-index-gen]
              (let [path-str (str "entry[" idx "].resource." field)
                    locator {:format :fhir :path path-str}
                    r (mutate/mutate bundle (op operator-id) locator)]
                (and (kernel/ok? r)
                     (= #{["entry" idx "resource" field]}
                        (diff/diff-paths bundle (:mutant (:payload r))))))))]
      (is (:pass? check-result) (str operator-id " violated the intended-diff-only law: " (:shrunk check-result))))))

;; ---- proves the law harness actually catches a violation, not just
;; rubber-stamps (same discipline as canonical-test/lineage-test) ----

(deftest mutate-law-harness-catches-a-real-violation-test
  (let [base {"a" 1 "b" 2}
        broken-mutant {"a" 1 "b" 3 "c" 4}] ; "b" changed AND "c" added -- extra collateral change
    (is (not= #{["b"]} (diff/diff-paths base broken-mutant)))
    (is (= #{["b"] ["c"]} (diff/diff-paths base broken-mutant)))))

;; ---- content-hash helper ----

(deftest content-hash-is-deterministic-and-format-json-test
  (is (= (mutate/content-hash sample-bundle) (mutate/content-hash sample-bundle)))
  (is (re-matches #"^[0-9a-f]{64}$" (mutate/content-hash sample-bundle))))

;; ---- v2 dispatch (P7): same fn, same contract shape, a different
;; substrate underneath -- ehrt.corpus-io.er7 instead of
;; plain FHIR JSON, dispatched on operator's own :format. ----

(defn- v2-op [id] (operators/lookup id "1"))

(def ^:private admit-fixture "test-fixtures/v2/adt-a01-admit.hl7")
(defn- admit-content [] (slurp (io/file admit-fixture)))

(deftest mutate-v2-happy-path-returns-mutant-string-and-lineage-test
  (let [base (admit-content)
        r (mutate/mutate base (v2-op :blank-required-field) {:format :v2 :path "MSH-9"})]
    (is (kernel/ok? r))
    (let [{:keys [mutant lineage]} (:payload r)]
      (is (string? mutant))
      (is (= "" (nth (get-in (er7/parse mutant) [:segments 0]) 8))
          "MSH-9 (split-index 8) must be blanked in the mutant")
      (is (lineage/valid? lineage))
      (is (lineage/valid-content-hash? lineage))
      (is (= :mutate (:stage lineage)))
      (is (= :blank-required-field (:id (:operator (:transformation lineage)))))
      (is (= {:format :v2 :path "MSH-9"} (:locator (:transformation lineage)))))))

(deftest mutate-v2-lineage-parent-and-produced-are-er7-content-hashes-test
  (let [base (admit-content)
        r (mutate/mutate base (v2-op :corrupt-encoding-characters) {:format :v2 :path "MSH-2"})
        {:keys [mutant lineage]} (:payload r)]
    (is (= (er7/content-hash base) (:parent lineage)))
    (is (= (er7/content-hash mutant) (:produced lineage)))
    (is (not= (:parent lineage) (:produced lineage)))))

(deftest mutate-v2-does-not-touch-the-original-content-test
  (let [base (admit-content)]
    (mutate/mutate base (v2-op :malformed-datetime-value) {:format :v2 :path "PID-7"})
    (is (= base (admit-content)) "the fixture file on disk must be untouched")))

(deftest mutate-v2-rejects-a-locator-pointing-nowhere-test
  (let [r (mutate/mutate (admit-content) (v2-op :blank-required-field) {:format :v2 :path "ZZZ-3"})]
    (is (kernel/rejected? r))
    (is (= :locator-not-found (:category r)))))

(deftest mutate-v2-propagates-a-malformed-locator-path-test
  (let [r (mutate/mutate (admit-content) (v2-op :blank-required-field) {:format :v2 :path "PID-0"})]
    (is (kernel/rejected? r))
    (is (= :invalid-v2-path (:category r)))))
