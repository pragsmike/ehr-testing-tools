(ns ehr-testing-tools.corpus.er7-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehr-testing-tools.corpus.er7 :as er7]))

(def ^:private fixture-dir "test/fixtures/v2")

(defn- fixture [name] (slurp (io/file fixture-dir name)))

(def ^:private all-fixtures
  ["adt-a01-admit.hl7"
   "adt-a01-admit-repeated-identifiers.hl7"
   "adt-a02-transfer.hl7"
   "adt-a03-discharge.hl7"
   "adt-a08-update-trailing-empty-fields.hl7"])

;; ---- the substrate law: split -> join is byte-identical, over every
;; fixture in this session's corpus, including the two adversarial ones
;; authored specifically to catch the canonicalization hazard EXP-B2
;; found in HAPI's PipeParser ----

(deftest split-join-round-trips-byte-identically-over-every-fixture-test
  (doseq [name all-fixtures]
    (let [content (fixture name)
          round-tripped (er7/serialize (er7/parse content))]
      (is (= content round-tripped) (str name " must round-trip byte-identically")))))

(deftest round-trip-preserves-repeated-identifiers-verbatim-test
  ;; adversarial: PID-3 carries two repeated identifiers joined by "~"
  ;; -- proves field-level split/join leaves repeat-separated content
  ;; untouched rather than mis-splitting around it.
  (let [content (fixture "adt-a01-admit-repeated-identifiers.hl7")]
    (is (re-find #"556677\^\^\^CGH\^MR~998877\^\^\^SSA\^SS" content)
        "fixture must actually exercise repeated identifiers -- otherwise this test proves nothing")
    (is (= content (er7/serialize (er7/parse content))))))

(deftest round-trip-preserves-trailing-empty-fields-test
  ;; adversarial: this is exactly the canonicalization hazard EXP-B2
  ;; found in PipeParser (trailing empty PID-18 subfields stripped on
  ;; re-encode) -- our own split/join must not reproduce it.
  (let [content (fixture "adt-a08-update-trailing-empty-fields.hl7")
        parsed (er7/parse content)
        pid (first (filter #(= "PID" (first %)) (:segments parsed)))]
    (is (= "" (last pid)) "fixture must actually end PID in a trailing empty field -- otherwise this test proves nothing")
    (is (= content (er7/serialize parsed)))))

;; ---- delimiters: read from MSH-1/MSH-2, not hardcoded ----

(deftest delimiters-reads-msh-1-and-msh-2-test
  (let [content (fixture "adt-a01-admit.hl7")]
    (is (= {:field "|" :component "^" :repetition "~" :escape "\\" :subcomponent "&"}
           (er7/delimiters content)))))

;; ---- parse: segments and fields, general convention (field N -> split
;; index N, segment name itself at index 0) ----

(deftest parse-splits-into-segments-and-fields-test
  (let [content (fixture "adt-a01-admit.hl7")
        {:keys [segments]} (er7/parse content)
        seg-names (map first segments)
        pid (first (filter #(= "PID" (first %)) segments))]
    ;; the fixture ends in a trailing segment terminator, so split
    ;; (limit -1, faithfully) yields one final empty-string "segment" --
    ;; exactly the token serialize needs back to reconstruct the file
    ;; byte-identically; dropping it would be the same canonicalization
    ;; hazard EXP-B2 found in PipeParser, just relocated to our own code.
    (is (= ["MSH" "EVN" "PID" "PV1" ""] seg-names))
    (is (= "445566^^^CGH^MR" (nth pid 3)) "PID-3 (patient identifier list) at split-index 3")))

(deftest parse-msh-encoding-characters-land-at-split-index-1-test
  ;; the off-by-one this whole namespace's docstring documents: MSH-1
  ;; (the field separator) has no split-array slot, so MSH-2 (the
  ;; encoding characters) is the FIRST split token, not the second.
  (let [content (fixture "adt-a01-admit.hl7")
        {:keys [segments]} (er7/parse content)
        msh (first segments)]
    (is (= "MSH" (first msh)))
    (is (= "^~\\&" (nth msh 1)))))

;; ---- content-hash ----

(deftest content-hash-is-deterministic-and-sha256-test
  (let [content (fixture "adt-a01-admit.hl7")]
    (is (= (er7/content-hash content) (er7/content-hash content)))
    (is (re-matches #"^[0-9a-f]{64}$" (er7/content-hash content)))))

(deftest content-hash-changes-with-content-test
  (let [a (fixture "adt-a01-admit.hl7")
        b (fixture "adt-a02-transfer.hl7")]
    (is (not= (er7/content-hash a) (er7/content-hash b)))))
