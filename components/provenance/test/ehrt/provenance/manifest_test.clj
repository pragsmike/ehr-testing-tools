(ns ehrt.provenance.manifest-test
  "Schema/validator tests moved out of ehrt.corpus.manifest-test (sim
  split B, M1, 2026-08-04, `.agents/plans/2026-08-04-sim-split-b-plan.md`
  AR-2 / this session's own AR-M1-1) -- the tests that exercise
  valid?/valid-v1?/valid-v1-1? against hand-shaped manifest maps, split
  out from the tests that exercise the builders (`build`/`build-v1`/
  `build-v1-1`, which stayed producer-side in `ehrt.corpus.manifest`).
  Fixture manifests here are literal maps, not `ehrt.corpus.manifest/
  build*` output: provenance depends on nothing corpus-derived (the
  forbidden-forever direction this component exists to make possible),
  so its own tests can't reach for corpus's builders either -- the
  literal maps below are shaped identically to what those builders
  produce, verified against `ehrt.corpus.manifest-test`'s own
  `sample-fields`/`sample-fields-v1`/`sample-fields-v1-1` at move time."
  (:require [clojure.test :refer [deftest is]]
            [ehrt.provenance.manifest :as manifest]))

(def v0-manifest
  {:schema-version 0
   :generator {:name "synthea" :version "4.0.0" :sha256 (apply str (repeat 64 "a"))}
   :seed 42
   :clinician-seed 43
   :config {:path "config/synthea/synthea.properties" :sha256 (apply str (repeat 64 "b"))}
   :invocation {:command "java" :args ["-jar" "synthea.jar"] :exit-code 0}
   :canonicalizers-applied [[:strip-timestamps "1"]]
   :environment {:locale "en-US" :timezone "UTC" :jvm-version "21"}})

(deftest valid-rejects-missing-required-fields-test
  (is (not (manifest/valid? (dissoc v0-manifest :seed))))
  (is (not (manifest/valid? (dissoc v0-manifest :clinician-seed))))
  (is (not (manifest/valid? (dissoc v0-manifest :generator))))
  (is (not (manifest/valid? {:schema-version 0}))))

(deftest valid-rejects-wrong-schema-version-test
  (is (not (manifest/valid? (assoc v0-manifest :schema-version 1)))))

;; ---- schema v1 (EXP-A4's upgrade: adds :reference-date, the one
;; pinned-input field the v0 hypothesis omitted as an explicit,
;; top-level field -- it was only recoverable indirectly via the
;; embedded invocation args) ----

(def v1-manifest
  (assoc v0-manifest :schema-version 1 :reference-date "20260101"))

(deftest valid-v1-rejects-missing-reference-date-test
  (is (not (manifest/valid-v1? (dissoc v1-manifest :reference-date)))))

(deftest valid-v1-rejects-schema-version-0-test
  (is (not (manifest/valid-v1? (assoc v1-manifest :schema-version 0)))))

(deftest v0-manifest-is-not-a-valid-v1-manifest-test
  ;; The schemas are genuinely distinct -- a v0 manifest (no
  ;; reference-date, schema-version 0) must not pass as v1.
  (is (not (manifest/valid-v1? v0-manifest))))

;; ---- schema v1.1 (P4: :stage, :seeds map, :engine-params map,
;; :runtime -- engine-shaped fields (seed, clinician-seed,
;; reference-date) leave the fixed top level; :schema-version becomes
;; the string "1.1", not the integer 2 -- this is an additive minor
;; revision, not a breaking rewrite, and the string says so directly
;; rather than implying a v2 that doesn't exist) ----

(def v1-1-manifest
  {:schema-version "1.1"
   :stage :generate
   :generator {:name "synthea" :version "4.0.0" :sha256 (apply str (repeat 64 "a"))}
   :runtime {:name "temurin-jdk" :version "17.0.19+10" :sha256 (apply str (repeat 64 "d"))}
   :seeds {:master 100 :clinician 555}
   :engine-params {:reference-date "20260101"}
   :config {:path "config/synthea/synthea.properties" :sha256 (apply str (repeat 64 "b"))}
   :invocation {:command "java" :args ["-jar" "synthea.jar"] :exit-code 0}
   :canonicalizers-applied [[:strip-timestamps "1"]]
   :environment {:locale "en-US" :timezone "UTC" :jvm-version "17.0.19"}})

(deftest valid-v1-1-rejects-missing-required-fields-test
  (is (not (manifest/valid-v1-1? (dissoc v1-1-manifest :stage))))
  (is (not (manifest/valid-v1-1? (dissoc v1-1-manifest :seeds))))
  (is (not (manifest/valid-v1-1? (dissoc v1-1-manifest :engine-params))))
  (is (not (manifest/valid-v1-1? (dissoc v1-1-manifest :generator)))))

(deftest valid-v1-1-rejects-wrong-schema-version-test
  (is (not (manifest/valid-v1-1? (assoc v1-1-manifest :schema-version 1))))
  (is (not (manifest/valid-v1-1? (assoc v1-1-manifest :schema-version "2")))))

(deftest v1-manifest-is-not-a-valid-v1-1-manifest-test
  ;; Pre-v1.1 manifests remain valid *historical records* (schema
  ;; versioning, not migration) -- but they are genuinely a different
  ;; shape and must not pass as v1.1.
  (is (not (manifest/valid-v1-1? v1-manifest))))

(deftest v1-1-manifest-is-not-a-valid-v1-manifest-test
  (is (not (manifest/valid-v1? v1-1-manifest))))
