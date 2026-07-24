(ns ehr-testing-tools.corpus.mutate-test
  "Loading corpus.operators registers the seed catalog as a side
  effect (same convention as corpus.canonicalizers) -- requiring it
  below is enough."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.data.json :as json]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.lineage :as lineage]
            [ehr-testing-tools.corpus.operators :as operators]
            [ehr-testing-tools.corpus.mutate :as mutate]))

(def sample-bundle
  {"resourceType" "Bundle"
   "type" "transaction"
   "entry" [{"resource" {"resourceType" "Patient" "id" "p1"
                          "gender" "female" "birthDate" "1985-03-12" "active" true}}]})

(defn- op [id] (operators/lookup id "1"))

(deftest mutate-happy-path-returns-mutant-and-lineage-test
  (let [r (mutate/mutate sample-bundle (op :remove-required-element)
                          {:format :fhir :path "entry[0].resource.gender"})]
    (is (result/ok? r))
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
    (is (result/rejected? r))
    (is (= :locator-not-found (:category r)))))

(deftest mutate-propagates-a-malformed-locator-path-test
  (let [r (mutate/mutate sample-bundle (op :remove-required-element)
                          {:format :fhir :path "entry[bad]"})]
    (is (result/rejected? r))
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

(defn- diff-paths
  "The minimal set of paths at which a and b differ -- if a whole
  subtree differs (added, removed, or replaced wholesale), reports
  that subtree's path once, not every path beneath it. Recurses into
  both maps (by key) and vectors (by index) -- FHIR JSON nests both
  (Bundle.entry is a vector of maps), so stopping at maps alone would
  under-report: a single differing element deep inside an otherwise-
  identical vector would wrongly blame the whole vector."
  ([a b] (diff-paths a b []))
  ([a b path]
   (cond
     (= a b) #{}
     (and (map? a) (map? b))
     (reduce (fn [acc k]
               (into acc (diff-paths (get a k ::missing) (get b k ::missing) (conj path k))))
             #{}
             (into (set (keys a)) (keys b)))
     (and (vector? a) (vector? b) (= (count a) (count b)))
     (reduce (fn [acc i]
               (into acc (diff-paths (nth a i) (nth b i) (conj path i))))
             #{}
             (range (count a)))
     :else #{path})))

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
                (and (result/ok? r)
                     (= #{["entry" idx "resource" field]}
                        (diff-paths bundle (:mutant (:payload r))))))))]
      (is (:pass? check-result) (str operator-id " violated the intended-diff-only law: " (:shrunk check-result))))))

;; ---- proves the law harness actually catches a violation, not just
;; rubber-stamps (same discipline as canonical-test/lineage-test) ----

(deftest mutate-law-harness-catches-a-real-violation-test
  (let [base {"a" 1 "b" 2}
        broken-mutant {"a" 1 "b" 3 "c" 4}] ; "b" changed AND "c" added -- extra collateral change
    (is (not= #{["b"]} (diff-paths base broken-mutant)))
    (is (= #{["b"] ["c"]} (diff-paths base broken-mutant)))))

;; ---- content-hash helper ----

(deftest content-hash-is-deterministic-and-format-json-test
  (is (= (mutate/content-hash sample-bundle) (mutate/content-hash sample-bundle)))
  (is (re-matches #"^[0-9a-f]{64}$" (mutate/content-hash sample-bundle))))
