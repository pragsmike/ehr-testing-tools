(ns ehrt.docs-tooling.event-log-doc-test
  "Unit tests for the event-log section renderer (event-log contract
  arc Step 3).

  CI's freshness diff (`make docsgen` + `git diff --exit-code`) is what
  catches a STALE section. These tests catch the things a freshness
  diff cannot: a renderer that is confidently, consistently wrong, and
  therefore regenerates the same wrong page every time. The
  nested-`:event` warning is the sharp case -- it is the one piece of
  the page a consumer most needs to be right, and it is derived rather
  than typed, so its derivation gets a test of its own."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [ehrt.docs-tooling.event-log-doc :as doc]))

(defn- schema [] (edn/read-string (slurp doc/schema-path)))
(defn- examples [] (edn/read-string (slurp doc/examples-path)))

(deftest describe-renders-the-shapes-this-schema-actually-uses
  (testing "every form family the committed schema contains"
    (is (= "string" (doc/describe :string)))
    (is (= "integer" (doc/describe :int)))
    (is (= "boolean" (doc/describe :boolean)))
    (is (= "vector of string" (doc/describe [:vector :string])))
    (is (= "set of string" (doc/describe [:set :string])))
    (is (= "integer, or nil" (doc/describe [:maybe :int])))
    (is (= "`:admission`" (doc/describe [:= :admission])))
    (is (= "one of `:licensed`, `:surge`" (doc/describe [:enum :licensed :surge])))
    (is (= "map with keys `:a`, `:b`" (doc/describe [:map [:a :string] [:b :int]])))
    (is (= "map with keys `:a`" (doc/describe [:map {:closed true} [:a :string]]))
        "a properties map must not be mistaken for an entry")
    (is (= "string matching `^\\d{4}$`" (doc/describe [:re "^\\d{4}$"]))
        "the portable string form, not a #\"...\" literal")))

(deftest generated-block-covers-every-declared-kind
  (let [s (schema)
        block (doc/render s (examples))
        kinds (map first (filter vector? (rest (:schema s))))]
    ;; 28 as of contract 1.7.0 (arc 3b sweep 3, ADR-0174 section 2(b)):
    ;; scheduling's four -- `:appointment`, `:reschedule`,
    ;; `:appointment-cancel`, `:no-show` -- joined the closed vocabulary,
    ;; on top of 1.6.0's own `:bed-status-change`.
    (is (= 28 (count kinds)))
    (doseq [k kinds]
      (is (str/includes? block (str "#### `" k "`"))
          (str "no section rendered for " k))
      (is (str/includes? block (str ":event " k))
          (str "no real example rendered for " k)))))

(deftest the-nested-event-warning-names-every-colliding-name
  (testing "the warning is DERIVED from PreHorizonFact's own enum, so it
            cannot drift from the vocabulary it warns about -- if a
            nested fact name is ever added that collides with a log
            kind, this must pick it up without anyone editing prose"
    (let [block (doc/render (schema) (examples))]
      (is (str/includes? block "Read the top-level vector only"))
      (is (str/includes? block "Do not walk the tree looking for `:event`"))
      (doseq [colliding [:medication-order :medication-end :care-plan-start :care-plan-end]]
        (is (str/includes? block (str "`" colliding "`"))
            (str "the warning does not name the colliding kind " colliding)))
      (is (str/includes? block "4 of them")
          "the count is derived; if it changes, the sentence must too"))))

(deftest the-warning-leads-the-section
  (testing "author ruling 2026-08-16: the nested-:event warning leads
            the prose. Position is the whole point of that ruling, so
            position is what is asserted."
    (let [block (doc/render (schema) (examples))]
      (is (< (str/index-of block "Read the top-level vector only")
             (str/index-of block "### The vocabulary"))))))

(deftest splice-replaces-only-the-marked-region
  (let [page (str "before\n" doc/begin-marker "\nOLD\n" doc/end-marker "\nafter\n")
        out (doc/splice page "NEW")]
    (is (str/includes? out "before"))
    (is (str/includes? out "after"))
    (is (str/includes? out "NEW"))
    (is (not (str/includes? out "OLD")))))

(deftest splice-refuses-a-page-with-no-markers
  (is (thrown? clojure.lang.ExceptionInfo (doc/splice "no markers here" "NEW"))))

(deftest the-committed-page-carries-the-markers
  (let [page (slurp doc/formats-path)]
    (is (str/includes? page doc/begin-marker))
    (is (str/includes? page doc/end-marker))))

(deftest the-authored-prose-states-the-regex-dialect
  (testing "author ruling 2026-08-16, added beyond the original: a
            consumer validating in another language is being handed
            java.util.regex's flavour and has to be told so"
    (is (str/includes? (slurp doc/formats-path) "java.util.regex"))))
