(ns ehr-testing-tools.locators-doc-test
  "Pins every example locator printed in docs/locators.md to what the
  parsers actually do with it (DOC-3, author ruling: no unverified
  example in that document). This is the doc's test, not the grammar's
  -- ehr-testing-tools.locator-test and corpus.er7-test cover the
  grammars themselves. What this suite adds is that the *documentation*
  can't rot silently: change a grammar and the doc's examples fail
  here, in the ordinary `make test` run, rather than quietly becoming
  wrong prose.

  Every vector below appears verbatim in docs/locators.md. If you edit
  one, edit both."
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-tools.corpus.er7 :as er7]
            [ehr-testing-tools.locator :as locator]
            [ehr-testing-tools.result :as result]))

;; ---- FHIR ----

(def fhir-accepted
  "[path, the data path it parses to] -- every accepted FHIR example
  in docs/locators.md."
  [["gender"                                            ["gender"]]
   ["resourceType"                                      ["resourceType"]]
   ["entry[0].resource.gender"                          ["entry" 0 "resource" "gender"]]
   ["entry[0].resource.birthDate"                       ["entry" 0 "resource" "birthDate"]]
   ["entry[0].resource.active"                          ["entry" 0 "resource" "active"]]
   ["entry[0].resource.name[0].given[0]"                ["entry" 0 "resource" "name" 0 "given" 0]]
   ["entry[2].resource.identifier[1].type.coding[0].code"
    ["entry" 2 "resource" "identifier" 1 "type" "coding" 0 "code"]]
   ["_birthDate"                                        ["_birthDate"]]])

(def fhir-rejected
  "Every rejected FHIR example in docs/locators.md."
  ["" "0entry" "entry[x]" "entry[-1]" "entry.0.resource"
   "entry[0]..resource" "entry[0]resource" "entry[0][1]"
   "entry[0].resource."])

(deftest documented-fhir-locators-parse-to-the-documented-data-path-test
  (doseq [[path expected] fhir-accepted]
    (let [r (locator/fhir-data-path path)]
      (is (result/ok? r) (str path " must parse"))
      (is (= expected (:payload r)) (str path " must parse to the path docs/locators.md prints")))))

(deftest documented-fhir-non-locators-are-rejected-test
  (doseq [path fhir-rejected]
    (let [r (locator/fhir-data-path path)]
      (is (result/rejected? r) (str (pr-str path) " must be rejected"))
      (is (= :invalid-fhir-path (:category r))))))

(deftest documented-fhir-trailing-separator-is-rejected-test
  (testing "LOC-1 (2026-07-25) tightened this: a trailing dot used to be
            silently dropped by clojure.string/split before the grammar
            ever saw it, so \"entry[0].resource.\" parsed as
            \"entry[0].resource\". It is now a parse error, which is what
            docs/locators.md's grammar note states -- both grammars are
            fully anchored, and neither accepts a trailing separator"
    (let [r (locator/fhir-data-path "entry[0].resource.")]
      (is (result/rejected? r))
      (is (= :invalid-fhir-path (:category r))))))

;; ---- v2 ----

(def v2-accepted
  "[path, the structured map it parses to] -- every accepted v2
  example in docs/locators.md."
  [["PID"         {:segment "PID"}]
   ["MSH"         {:segment "MSH"}]
   ["OBX[2]"      {:segment "OBX" :segment-repeat 2}]
   ["PID-3"       {:segment "PID" :field 3}]
   ["MSH-2"       {:segment "MSH" :field 2}]
   ["MSH-7"       {:segment "MSH" :field 7}]
   ["MSH-9"       {:segment "MSH" :field 9}]
   ["PID-3[2]"    {:segment "PID" :field 3 :field-repeat 2}]
   ["PID-5.1"     {:segment "PID" :field 5 :component 1}]
   ["PID-5.1.2"   {:segment "PID" :field 5 :component 1 :subcomponent 2}]
   ["OBX[2]-5.1"  {:segment "OBX" :segment-repeat 2 :field 5 :component 1}]
   ["ZZ1-1"       {:segment "ZZ1" :field 1}]])

(def v2-rejected
  "Every rejected v2 example in docs/locators.md."
  ["" "pid" "P1" "PIDX" "1ID" "PID.3" "PID-" "PID-0" "PID-3."
   "PID[0]" "PID-3[0]" "PID-3.1.2.4" "MSH-1"])

(deftest documented-v2-locators-parse-to-the-documented-map-test
  (doseq [[path expected] v2-accepted]
    (let [r (locator/v2-data-path path)]
      (is (result/ok? r) (str path " must parse"))
      (is (= expected (:payload r)) (str path " must parse to the map docs/locators.md prints")))))

(deftest documented-v2-non-locators-are-rejected-test
  (doseq [path v2-rejected]
    (let [r (locator/v2-data-path path)]
      (is (result/rejected? r) (str (pr-str path) " must be rejected"))
      (is (= :invalid-v2-path (:category r))))))

;; ---- v2 resolution: the MSH off-by-one and field granularity, against
;; the same two-segment message docs/locators.md prints ----

(def documented-msh-1-hint
  "The MSH-1 refusal's hint, exactly as docs/locators.md block-quotes it
  under \"Where a locator lands: the MSH off-by-one\". Verbatim in both
  places, per this namespace's own edit-one-edit-both rule."
  (str "MSH-1 is the field separator character itself (the character right "
       "after the literal \"MSH\"), not an addressable field: the split that "
       "produces a segment's fields consumes it, so it holds no position of "
       "its own. The encoding characters are MSH-2."))

(def sample-message
  "The message docs/locators.md walks its v2 resolution examples
  against. \\r is the segment terminator (corpus.er7/segment-terminator);
  the doc prints one segment per line for legibility."
  (str "MSH|^~\\&|SND|FAC|RCV|FAC|20260101||ADT^A01^ADT_A01|MSG1|P|2.4\r"
       "PID|1||12345||Doe^John||19800101"))

(defn- resolved-value
  "The field string a documented locator actually lands on, or nil for
  a segment-only locator or one that doesn't resolve."
  [path]
  (let [parsed (er7/parse sample-message)
        r (locator/v2-data-path path)]
    (when (result/ok? r)
      (when-let [{:keys [segment-index field-index]} (er7/resolve-locator parsed (:payload r))]
        (when field-index
          (get-in parsed [:segments segment-index field-index]))))))

(deftest documented-v2-resolution-examples-land-where-the-doc-says-test
  (is (= "ADT^A01^ADT_A01" (resolved-value "MSH-9")))
  (is (= "Doe^John" (resolved-value "PID-5")))
  (is (= "19800101" (resolved-value "PID-7")))
  (is (= "20260101" (resolved-value "MSH-7"))))

(deftest documented-msh-off-by-one-test
  (testing "MSH-1 IS the field separator, which the split consumes, so
            it holds no slot of its own; MSH-2 is therefore the first
            token after the segment name"
    (is (= 1 (er7/field-index "MSH" 2)))
    (is (= 8 (er7/field-index "MSH" 9)))
    (is (= 5 (er7/field-index "PID" 5)) "no other segment is shifted"))
  (testing "LOC-1 (2026-07-25) closed the trap docs/locators.md used to
            warn about. MSH-1 sits below the N>=2 shift, so it used to
            resolve onto MSH-2's slot and silently address the encoding
            characters; it is now refused at parse, with a hint that
            teaches the convention, and never reaches the substrate at
            all. corpus.er7/field-index is unchanged -- the substrate
            still maps field 1 the way it always did; what changed is
            that no locator can ask it to"
    (is (= 1 (er7/field-index "MSH" 1)) "substrate mapping untouched")
    (let [r (locator/v2-data-path "MSH-1")]
      (is (result/rejected? r))
      (is (= :invalid-v2-path (:category r)))
      (is (= documented-msh-1-hint (:hint (:payload r)))
          "docs/locators.md block-quotes this hint verbatim; if the
           parser's wording changes, change the doc's in the same commit"))
    (is (nil? (resolved-value "MSH-1")) "and so it lands nowhere")
    (is (= "^~\\&" (resolved-value "MSH-2")) "while MSH-2 still lands on the encoding characters")))

(deftest documented-component-granularity-test
  (testing "a locator naming a component resolves at its field: the
            substrate is field-granular, so PID-5.1 and PID-5 land on
            the same string"
    (is (= (resolved-value "PID-5") (resolved-value "PID-5.1")))
    (is (= "Doe^John" (resolved-value "PID-5.1")))))

(deftest documented-unresolvable-v2-locators-parse-but-do-not-resolve-test
  (testing "a well-formed locator naming a field or segment this
            message doesn't have parses fine and resolves to nil --
            two different failures, at two different layers"
    (doseq [path ["PID-99" "NK1-2"]]
      (is (result/ok? (locator/v2-data-path path)) (str path " must parse"))
      (is (nil? (er7/resolve-locator (er7/parse sample-message)
                                     (:payload (locator/v2-data-path path))))
          (str path " must not resolve against the sample message")))))
