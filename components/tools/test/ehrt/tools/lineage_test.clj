(ns ehrt.tools.lineage-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.tools.lineage :as lineage]
            [ehrt.kernel.interface :as kernel]))

(def sample-transformation
  {:operator {:id :remove-required-element :version "1"}
   :locator {:format :fhir :path "entry[0].resource.gender"}
   :contract {:type :violates :target "min-cardinality"}})

(defn- sample-fields []
  {:parent (kernel/sha256-string "base-datum-bytes")
   :stage :mutate
   :transformation sample-transformation
   :produced (kernel/sha256-string "mutant-datum-bytes")})

(deftest build-produces-a-schema-valid-record-test
  (let [r (lineage/build (sample-fields))]
    (is (lineage/valid? r))
    (is (= :mutate (:stage r)))
    (is (= sample-transformation (:transformation r)))))

(deftest build-references-parent-and-produced-by-content-hash-test
  (let [fields (sample-fields)
        r (lineage/build fields)]
    (is (= (kernel/sha256-string "base-datum-bytes") (:parent r)))
    (is (= (kernel/sha256-string "mutant-datum-bytes") (:produced r)))))

;; ---- the self-verification property: :id IS the content hash of the
;; rest of the record ----

(deftest build-computes-id-as-content-hash-of-the-rest-test
  (let [r (lineage/build (sample-fields))]
    (is (lineage/valid-content-hash? r))
    (is (= (:id r) (lineage/record-content-hash (dissoc r :id))))))

(deftest tampering-with-any-field-invalidates-the-content-hash-test
  ;; Proves the self-verification actually catches something, not
  ;; just rubber-stamps -- same discipline as canonical-test's
  ;; idempotence-catches-a-violation test.
  (let [r (lineage/build (sample-fields))]
    (is (not (lineage/valid-content-hash? (assoc r :produced (kernel/sha256-string "different")))))
    (is (not (lineage/valid-content-hash? (assoc r :stage :gate))))))

(deftest two-records-with-identical-content-get-identical-ids-test
  ;; Content-addressed: the id is a pure function of the content, not
  ;; of when/where the record was built.
  (is (= (lineage/build (sample-fields)) (lineage/build (sample-fields)))))

(deftest different-parent-produces-a-different-id-test
  (let [r1 (lineage/build (sample-fields))
        r2 (lineage/build (assoc (sample-fields) :parent (kernel/sha256-string "a different base")))]
    (is (not= (:id r1) (:id r2)))))

;; ---- schema rejects malformed records ----

(deftest valid-rejects-non-hash-parent-test
  (is (not (lineage/valid? (assoc (lineage/build (sample-fields)) :parent "not-a-hash")))))

(deftest valid-rejects-missing-transformation-test
  (is (not (lineage/valid? (dissoc (lineage/build (sample-fields)) :transformation)))))

;; ---- property: for any record built from valid-shaped inputs, the
;; content-hash self-verification holds (generative, not just the
;; fixed examples above) ----

(deftest content-addressing-property-test
  (let [hash-gen (gen/fmap kernel/sha256-string gen/string-ascii)
        stage-gen (gen/elements [:generate :normalize :mutate :gate :report])
        fields-gen (gen/let [parent hash-gen
                              produced hash-gen
                              stage stage-gen]
                     {:parent parent :produced produced :stage stage
                      :transformation sample-transformation})
        check-result (tc/quick-check 100
                       (prop/for-all [fields fields-gen]
                         (lineage/valid-content-hash? (lineage/build fields))))]
    (is (:pass? check-result))))
