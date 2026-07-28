(ns ehrt.tools.corpus.operation-manifest-test
  "Test-first (ADR-0006; SS-4b Step 3): written before
  ehrt.tools.corpus.operation-manifest existed. Schema-level
  coverage only -- write-side wiring (ehrt.tools.corpus.sink-write)
  and intake-side recognition (ehrt.tools.corpus.intake) each own
  their own test namespaces."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.tools.corpus.operation-manifest :as om]))

(def ^:private producer
  {:name "ehr-testing-tools" :identity "pre-release" :git "abc1234-dirty"})

(def ^:private operation
  {:kind :mutate :operator-id :blank-required-field :operator-version "1"
   :locator {:format :v2 :path "MSH-9"}})

(deftest build-produces-a-valid-manifest-test
  (let [manifest (om/build {:producer producer
                             :operation operation
                             :written-at "2026-07-28"
                             :format :v2-er7
                             :framing :file-per-item
                             :items [{:name "a.hl7" :sha256 (apply str (repeat 64 "a"))}]})]
    (is (om/valid? manifest))
    (is (= :operation (:manifest-kind manifest)))
    (is (= 1 (:schema-version manifest)))))

(deftest build-preserves-optional-per-item-input-hash-test
  (let [sha (apply str (repeat 64 "a"))
        parent (apply str (repeat 64 "b"))
        manifest (om/build {:producer producer
                             :operation operation
                             :written-at "2026-07-28"
                             :format :v2-er7
                             :framing :file-per-item
                             :items [{:name "a.hl7" :sha256 sha :input-hash parent}
                                     {:name "b.hl7" :sha256 sha}]})]
    (is (om/valid? manifest))
    (is (= parent (:input-hash (first (:items manifest)))))
    (is (not (contains? (second (:items manifest)) :input-hash))
        "input-hash is present iff the producer actually held it -- never a nil placeholder")))

(deftest build-with-no-items-is-still-valid-test
  (let [manifest (om/build {:producer producer :operation operation
                             :written-at "2026-07-28" :format :v2-er7
                             :framing :file-per-item :items []})]
    (is (om/valid? manifest))
    (is (= [] (:items manifest)))))

(deftest invalid-manifests-test
  (testing "a stray ManifestV1_1-shaped :generator key is harmless -- malli's default :map is open, so this schema's own required fields are what's actually asserted, not a closed-map rejection of borrowed vocabulary"
    (is (om/valid? {:manifest-kind :operation :schema-version 1
                     :producer producer :operation operation
                     :written-at "2026-07-28" :format :v2-er7
                     :framing :file-per-item
                     :generator {:name "x" :version "1" :sha256 "y"}
                     :items []})))
  (testing "wrong :manifest-kind"
    (is (not (om/valid? {:manifest-kind :generate :schema-version 1
                          :producer producer :operation operation
                          :written-at "2026-07-28" :format :v2-er7
                          :framing :file-per-item :items []}))))
  (testing "missing :producer"
    (is (not (om/valid? {:manifest-kind :operation :schema-version 1
                          :operation operation :written-at "2026-07-28"
                          :format :v2-er7 :framing :file-per-item :items []})))))
