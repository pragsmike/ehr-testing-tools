(ns ehrt.corpus.manifest-test
  "Builder tests only (sim split B, M1, 2026-08-04,
  `.agents/plans/2026-08-04-sim-split-b-plan.md` AR-2 / this session's
  own AR-M1-1): the schema/validator tests (valid?/valid-v1?/
  valid-v1-1? against hand-shaped maps) moved to
  `ehrt.provenance.manifest-test` alongside the schemas themselves.
  What stays here tests `build`/`build-v1`/`build-v1-1` -- the
  producer-side functions that did not move -- using `valid?` still
  where a builder test's own assertion needs it."
  (:require [clojure.test :refer [deftest is]]
            [ehrt.corpus.manifest :as manifest]))

(def sample-fields
  {:generator {:name "synthea" :version "4.0.0" :sha256 (apply str (repeat 64 "a"))}
   :seed 42
   :clinician-seed 43
   :config {:path "config/synthea/synthea.properties" :sha256 (apply str (repeat 64 "b"))}
   :invocation {:command "java" :args ["-jar" "synthea.jar"] :exit-code 0}
   :canonicalizers-applied [[:strip-timestamps "1"]]
   :environment {:locale "en-US" :timezone "UTC" :jvm-version "21"}})

(deftest build-produces-schema-version-0-test
  (let [m (manifest/build sample-fields)]
    (is (= 0 (:schema-version m)))
    (is (= 43 (:clinician-seed m)))
    (is (manifest/valid? m))))

(deftest build-defaults-canonicalizers-applied-to-empty-test
  (let [m (manifest/build (dissoc sample-fields :canonicalizers-applied))]
    (is (= [] (:canonicalizers-applied m)))
    (is (manifest/valid? m))))

;; ---- schema v1 (EXP-A4's upgrade: adds :reference-date, the one
;; pinned-input field the v0 hypothesis omitted as an explicit,
;; top-level field -- it was only recoverable indirectly via the
;; embedded invocation args) ----

(def sample-fields-v1
  (assoc sample-fields :reference-date "20260101"))

(deftest build-v1-produces-schema-version-1-test
  (let [m (manifest/build-v1 sample-fields-v1)]
    (is (= 1 (:schema-version m)))
    (is (= "20260101" (:reference-date m)))
    (is (= 43 (:clinician-seed m)))
    (is (manifest/valid-v1? m))))

;; ---- schema v1.1 (P4: :stage, :seeds map, :engine-params map,
;; :runtime -- engine-shaped fields (seed, clinician-seed,
;; reference-date) leave the fixed top level; :schema-version becomes
;; the string "1.1", not the integer 2 -- this is an additive minor
;; revision, not a breaking rewrite, and the string says so directly
;; rather than implying a v2 that doesn't exist) ----

(def sample-fields-v1-1
  {:stage :generate
   :generator {:name "synthea" :version "4.0.0" :sha256 (apply str (repeat 64 "a"))}
   :runtime {:name "temurin-jdk" :version "17.0.19+10" :sha256 (apply str (repeat 64 "d"))}
   :seeds {:master 100 :clinician 555}
   :engine-params {:reference-date "20260101"}
   :config {:path "config/synthea/synthea.properties" :sha256 (apply str (repeat 64 "b"))}
   :invocation {:command "java" :args ["-jar" "synthea.jar"] :exit-code 0}
   :canonicalizers-applied [[:strip-timestamps "1"]]
   :environment {:locale "en-US" :timezone "UTC" :jvm-version "17.0.19"}})

(deftest build-v1-1-produces-schema-version-string-test
  (let [m (manifest/build-v1-1 sample-fields-v1-1)]
    (is (= "1.1" (:schema-version m)))
    (is (= :generate (:stage m)))
    (is (= {:master 100 :clinician 555} (:seeds m)))
    (is (= {:reference-date "20260101"} (:engine-params m)))
    (is (= "temurin-jdk" (:name (:runtime m))))
    (is (manifest/valid-v1-1? m))))

(deftest build-v1-1-runtime-is-optional-test
  ;; corpus.generate's :java-bin escape hatch (explicit override,
  ;; bypassing registry resolution) has no artifact to report -- the
  ;; manifest must not fabricate one; :runtime is simply absent.
  (let [m (manifest/build-v1-1 (dissoc sample-fields-v1-1 :runtime))]
    (is (manifest/valid-v1-1? m))
    (is (not (contains? m :runtime)))))

(deftest build-v1-1-defaults-canonicalizers-applied-to-empty-test
  (let [m (manifest/build-v1-1 (dissoc sample-fields-v1-1 :canonicalizers-applied))]
    (is (= [] (:canonicalizers-applied m)))
    (is (manifest/valid-v1-1? m))))

