(ns ehrt.sim-trajectory.census-test
  "Co-landing invariants for the GMF census tool (ADR-0034): the census's
  own verdicts carry the properties AR-2/AR-3/AR-4 claim for them, proven
  against small inline fixture modules (never against the real Synthea
  catalog -- that is Step 2's own committed artifact, not a unit test's
  job) -- one fixture per verdict class, plus the AR-3 substitution tag."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.sim-trajectory.census :as census]))

(defn- write-fixture! ^java.io.File [dir id json-text]
  (let [f (io/file dir (str id ".json"))]
    (io/make-parents f)
    (spit f json-text)
    f))

(def ^:private census-opts
  {:seed-count 3 :mixer-seed 20260803
   :registration-offset-years 30 :horizon-years 50})

(def ^:private ok-json
  "Trivially walkable regardless of persona/seed: Initial -> Terminal."
  (str "{\"name\": \"Census OK Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(def ^:private load-failed-json
  "VitalSign is a real, still-deferred v1 state type (docs/gmf-
  interpreter.md's own deferred-type table, ADR-0036 AR-7 -- a
  calibration-content gap, distinct from the also-deferred `:vital-sign`
  CONDITION type `walk-failed-json` below exercises) -- gmf/load-closure
  REJECTS this at load time, before any walk. GMF coverage Wave F
  (2026-08-03, ADR-0036): swapped from ImagingStudy (ADR-0029 R5, now
  supported) to VitalSign, the same 'stale premise, not silently left'
  treatment `gmf-test`'s own deferred-type fixtures already document."
  (str "{\"name\": \"Census Load-Failed Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Scan\"},"
       "   \"Scan\": {\"type\": \"VitalSign\", \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(def ^:private walk-failed-json
  "Loads clean (no state-type/schema gate fires -- gmf.clj's own loader
  does not validate condition-type vocabulary, only state types) but a
  Guard whose :allow names an unrecognized condition type
  ('Vital Sign', ADR-0031's own §2 'stay OUT' predicate, never built)
  throws at `evaluate-condition`'s own default case the moment the walk
  reaches it -- every fixture seed reaches Initial's own unconditional
  transition into the Guard first, so every seed throws."
  (str "{\"name\": \"Census Walk-Failed Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Blocked\"},"
       "   \"Blocked\": {\"type\": \"Guard\","
       "     \"allow\": {\"condition_type\": \"Vital Sign\", \"vital_sign\": \"Height\","
       "                 \"operator\": \">\", \"value\": 0},"
       "     \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(def ^:private wellness-json
  "A bare `wellness: true` Encounter with no `encounter_class` key --
  ADR-0031 AR-5(b)'s own timing-substitution trigger. Loads and walks
  fine under the CURRENT (Wave-B-era) loader normalization (rewritten to
  an immediate ambulatory-shaped encounter, not a genuine wait -- Wave G
  supersedes this), so this fixture proves the tag fires on an
  `:ok-walked` module; AR-3 itself is verdict-independent, exercised
  directly below via `wellness-substitution?` rather than re-proven
  against a second, load-failed fixture."
  (str "{\"name\": \"Census Wellness Fixture\","
       " \"states\": {"
       "   \"Initial\": {\"type\": \"Initial\", \"direct_transition\": \"Visit\"},"
       "   \"Visit\": {\"type\": \"Encounter\", \"wellness\": true, \"direct_transition\": \"End\"},"
       "   \"End\": {\"type\": \"EncounterEnd\", \"direct_transition\": \"Done\"},"
       "   \"Done\": {\"type\": \"Terminal\"}"
       " }}"))

(deftest ok-walked-module-censuses-clean
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-ok")
        file (write-fixture! dir "census-ok-fixture" ok-json)
        entry (census/census-one dir census-opts {:id "census-ok-fixture" :file file})]
    (is (= :ok-walked (:verdict entry)))
    (is (= [] (:disclosed-substitutions entry)))
    (is (= 3 (count (:walks entry))))
    (is (every? :digest (:walks entry)))
    (is (empty? (:walk-errors (:gap entry))))))

(deftest load-failed-module-names-the-unrecognized-state-type
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-load-failed")
        file (write-fixture! dir "census-load-failed-fixture" load-failed-json)
        entry (census/census-one dir census-opts {:id "census-load-failed-fixture" :file file})]
    (is (= :load-failed (:verdict entry)))
    (is (= [] (:walks entry)))
    (is (contains? (get-in entry [:gap :unrecognized-state-types]) "VitalSign"))))

(deftest walk-failed-module-names-every-throwing-seed
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-walk-failed")
        file (write-fixture! dir "census-walk-failed-fixture" walk-failed-json)
        entry (census/census-one dir census-opts {:id "census-walk-failed-fixture" :file file})]
    (is (= :walk-failed (:verdict entry)))
    (is (= 3 (count (:walks entry))))
    (testing "every seed throws on the same unrecognized condition type -- caught, recorded, not propagated"
      (is (= 3 (count (get-in entry [:gap :walk-errors]))))
      (is (every? #(= :vital-sign (get-in % [:error :data :condition-type]))
                  (get-in entry [:gap :walk-errors]))))))

(deftest wellness-substitution-tag-fires-regardless-of-verdict
  (testing "directly against the mechanical scan (AR-3's own claim: verdict-independent)"
    (is (true? (census/wellness-substitution? {"root" wellness-json})))
    (is (false? (census/wellness-substitution? {"root" ok-json})))
    (is (false? (census/wellness-substitution? {"root" load-failed-json}))))
  (testing "and on a real censused (:ok-walked) module"
    (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-wellness")
          file (write-fixture! dir "census-wellness-fixture" wellness-json)
          entry (census/census-one dir census-opts {:id "census-wellness-fixture" :file file})]
      (is (= :ok-walked (:verdict entry)))
      (is (= [:wellness-timing] (:disclosed-substitutions entry))))))

(deftest verify-pin-falls-back-to-content-hash-with-no-git-checkout
  (let [dir (io/file (System/getProperty "java.io.tmpdir") "census-test-no-git")]
    (write-fixture! (io/file dir "src" "main" "resources" "modules") "x" ok-json)
    (let [result (census/verify-pin dir census/synthea-pin)]
      (is (= :ok (:status result)))
      (is (= :sha256-content (:method (:payload result))))
      (is (true? (:pin-unverified-by-git (:payload result)))))))
