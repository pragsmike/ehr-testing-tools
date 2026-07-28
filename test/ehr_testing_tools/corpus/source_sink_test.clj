(ns ehr-testing-tools.corpus.source-sink-test
  "Test-first (ruling 4, SS-1): written before ehr-testing-tools.corpus.
  source-sink existed. Schema-level coverage only -- the URL<->map
  parser's own round-trip property lives in source_sink_url_test.clj
  (Step 3); this file covers the canonical-map shape and the dir/file
  constructors."
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-tools.corpus.source-sink :as ss]
            [ehr-testing-tools.result :as result]))

(deftest source-kinds-test
  (testing "the design's six named source kinds (D1) are all recognized as known"
    (is (= #{:dir :file :stdin :blaze :synthea :sim} ss/known-source-kinds)))
  (testing "SS-1's two reader kinds, SS-2's two generator kinds, and SS-3's :stdin are implemented"
    (is (= #{:dir :file :synthea :sim :stdin} ss/implemented-source-kinds))
    (is (every? ss/known-source-kinds ss/implemented-source-kinds)))
  (testing "printable-source-kinds stays narrower -- SS-2 parses generator Source values, never prints them"
    (is (= #{:dir :file} ss/printable-source-kinds))
    (is (every? ss/implemented-source-kinds ss/printable-source-kinds))))

(deftest sink-kinds-test
  (is (= #{:dir :file :stdout :blaze} ss/known-sink-kinds))
  (is (= #{:dir :file} ss/implemented-sink-kinds))
  (is (every? ss/known-sink-kinds ss/implemented-sink-kinds)))

(deftest valid-source?-test
  (testing "a minimal :kind-only map is a valid generic Source (kind is open, D4)"
    (is (ss/valid-source? {:kind :dir}))
    (is (ss/valid-source? {:kind :some-future-kind})))
  (testing ":format/:framing are optional (sources may infer, Part IV)"
    (is (ss/valid-source? {:kind :dir :format :fhir-json :framing :file-per-item})))
  (testing "not a map, or missing :kind, is invalid"
    (is (not (ss/valid-source? {})))
    (is (not (ss/valid-source? "dir:./corpus")))
    (is (not (ss/valid-source? nil)))))

(deftest valid-sink?-test
  (testing "a Sink requires :format explicitly -- no inference on the write side (D3)"
    (is (not (ss/valid-sink? {:kind :dir})))
    (is (ss/valid-sink? {:kind :dir :format :fhir-json})))
  (testing ":kind is open like Source's"
    (is (ss/valid-sink? {:kind :some-future-kind :format :v2-er7}))))

(deftest dir-source-test
  (testing "happy path: :path is required and round-trips into the map"
    (let [r (ss/dir-source {:path "./corpus"})]
      (is (result/ok? r))
      (is (= {:kind :dir :path "./corpus"} (:payload r)))))
  (testing ":format/:framing pass through when given"
    (let [r (ss/dir-source {:path "./corpus" :format :v2-er7 :framing :er7-multi})]
      (is (result/ok? r))
      (is (= {:kind :dir :path "./corpus" :format :v2-er7 :framing :er7-multi} (:payload r)))))
  (testing "missing :path is rejected, not a thrown exception (ADR-0004)"
    (let [r (ss/dir-source {})]
      (is (result/rejected? r))
      (is (= :invalid-source (:category r))))))

(deftest file-source-test
  (testing "happy path"
    (let [r (ss/file-source {:path "./corpus/one.json"})]
      (is (result/ok? r))
      (is (= {:kind :file :path "./corpus/one.json"} (:payload r)))))
  (testing "missing :path is rejected"
    (is (result/rejected? (ss/file-source {})))))

(deftest dir-sink-test
  (testing "happy path: :path and :format both required"
    (let [r (ss/dir-sink {:path "./out" :format :fhir-json})]
      (is (result/ok? r))
      (is (= {:kind :dir :path "./out" :format :fhir-json} (:payload r)))))
  (testing "missing :format is rejected (D3's no-inference-on-write law)"
    (is (result/rejected? (ss/dir-sink {:path "./out"}))))
  (testing "missing :path is rejected"
    (is (result/rejected? (ss/dir-sink {:format :fhir-json})))))

(deftest file-sink-test
  (testing "happy path"
    (let [r (ss/file-sink {:path "./out/one.json" :format :fhir-json})]
      (is (result/ok? r))
      (is (= {:kind :file :path "./out/one.json" :format :fhir-json} (:payload r)))))
  (testing "missing :format is rejected"
    (is (result/rejected? (ss/file-sink {:path "./out/one.json"}))))
  (testing "missing :path is rejected"
    (is (result/rejected? (ss/file-sink {:format :fhir-json})))))

;; ---- generator-source (SS-2 Step 4): validates+shapes only, never
;; executes -- the registry (ehr-testing-tools.corpus.generators) owns
;; param resolution, this constructor just calls through and tags the
;; result with :kind. ----

(deftest generator-source-happy-path-test
  (let [r (ss/generator-source :synthea {:seed 7})]
    (is (result/ok? r))
    (is (= :synthea (:kind (:payload r))))
    (is (= 7 (:seed (:payload r))))))

(deftest generator-source-unknown-kind-test
  (let [r (ss/generator-source :not-a-registered-kind {})]
    (is (result/rejected? r))
    (is (= :unknown-generator-kind (:category r)))))

;; ---- stdin-source (SS-3 Step 6): no :path, but :format/:framing are
;; how a caller declares what the piped bytes actually are ----

(deftest stdin-source-test
  (testing "happy path: no :path required"
    (let [r (ss/stdin-source {:format :v2-er7 :framing :er7-multi})]
      (is (result/ok? r))
      (is (= {:kind :stdin :format :v2-er7 :framing :er7-multi} (:payload r)))))
  (testing ":format/:framing are both optional here too"
    (let [r (ss/stdin-source {})]
      (is (result/ok? r))
      (is (= {:kind :stdin} (:payload r)))))
  (testing "an unrecognized framing keyword is rejected, same as every other kind"
    (is (result/rejected? (ss/stdin-source {:framing :not-a-real-framing})))))

(deftest generator-source-invalid-params-test
  (let [r (ss/generator-source :synthea {:seed "not-an-int"})]
    (is (result/rejected? r))
    (is (= :invalid-generator-params (:category r)))))

;; ---- framing is an explicit axis (SS-3 Step 1, D2/Part II): a closed
;; enum, not any keyword -- tightened from SS-1's open :keyword now that
;; ehr-testing-tools.corpus.framing (SS-3) gives every named kind a real
;; codec to dispatch to. ----

(deftest framing-enum-test
  (testing "the five design-named framing kinds all validate on Source and Sink"
    (doseq [f [:file-per-item :er7-multi :ndjson :bundle-entries :mllp]]
      (is (ss/valid-source? {:kind :dir :framing f}))
      (is (ss/valid-sink? {:kind :dir :format :fhir-json :framing f}))))
  (testing "an unrecognized framing keyword is rejected, not silently accepted"
    (is (not (ss/valid-source? {:kind :dir :framing :not-a-real-framing})))
    (is (not (ss/valid-sink? {:kind :dir :format :fhir-json :framing :not-a-real-framing})))
    (let [r (ss/dir-source {:path "./corpus" :framing :not-a-real-framing})]
      (is (result/rejected? r))
      (is (= :invalid-source (:category r))))))

(deftest default-framing-test
  (testing ":file-per-item is the design's stated default (D2/Part II) -- a named
            constant every framing-aware caller consults, never injected into a
            constructed map (preserves the D4 round-trip law on an absent :framing)"
    (is (= :file-per-item ss/default-framing))
    (is (= {:kind :dir :path "./corpus"} (:payload (ss/dir-source {:path "./corpus"})))
        "omitting :framing still omits it from the constructed map")))
