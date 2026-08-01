(ns ehrt.corpus-io.er7-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ehrt.corpus-io.er7 :as er7]
            ;; simhospital-corpus is a test fixture helper, not a
            ;; moved src namespace -- it wasn't in AR-1's 17-file scope
            ;; and stayed in components/corpus/test (corpus-io stage 2,
            ;; 2026-07-31). Both projects that compose this test
            ;; (conformance) also declare poly/corpus, so its test dir
            ;; is on the classpath alongside this one.
            [ehrt.corpus.simhospital-corpus :as simhospital]))

(def ^:private fixture-dir "components/corpus/test-fixtures/v2")

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

;; ---- the vendored SimHospital corpus (ADR-0011) ----
;;
;; Division of labor, per the adoption session's ruling: the exhaustive
;; round-trip over all 1,013 messages ran ONCE, as a probe, and is
;; registered in notes/facts-register.md (F26) with its command and
;; result. It is deliberately not re-run per push -- git
;; content-addresses every byte of the corpus, so re-proving its
;; integrity on every test run buys nothing the revision control system
;; doesn't already give. What the committed tests guard is
;; path-and-framing *behavior* (a rename, a missing file, a
;; newline-translating checkout) plus round-trip fidelity on a small
;; hazard-selected slice. Promoting the exhaustive probe into this
;; suite would be reverting that decision, not improving coverage.

(deftest simhospital-corpus-loads-and-frames-into-1013-messages-test
  ;; Presence/framing guard, not integrity auditing: this fails loudly
  ;; here -- at the loader -- rather than confusingly downstream, if the
  ;; fixture is renamed/removed, or if a checkout normalizes line
  ;; endings (which would collapse the CR segment terminators the
  ;; .gitattributes -text entry exists to protect).
  (let [messages (simhospital/messages)]
    (is (= 1013 (count messages)))
    (is (every? #(str/starts-with? % "MSH|") messages)
        "every framed block must begin with an MSH segment")
    (is (every? #(str/includes? % "\r") messages)
        "segments must still be CR-separated -- an LF-normalized checkout fails here")))

(deftest simhospital-hazard-slice-round-trips-byte-identically-test
  ;; Chosen the same way the hand-written adversarial fixtures were: each
  ;; member earns its place by exhibiting a specific hazard, and each is
  ;; selected by a stable structural predicate (see
  ;; simhospital-corpus/hazard-slice), never by byte offset.
  (doseq [{:keys [label message]} (simhospital/hazard-slice)]
    (is (= message (er7/serialize (er7/parse message)))
        (str label " must round-trip byte-identically"))))

(deftest simhospital-hazard-slice-actually-exhibits-its-hazards-test
  ;; Without this, the slice test above could pass over three
  ;; indistinguishable messages and prove nothing -- the same guard the
  ;; hand-written adversarial fixtures carry ("fixture must actually
  ;; exercise ... otherwise this test proves nothing").
  (let [by-label (into {} (map (juxt :label identity)) (simhospital/hazard-slice))]
    (is (= #{:pid-3-repetition :oru-long-obx-tail :lone-adt-a34} (set (keys by-label))))
    (testing "PID-3 repetition (MRN ~ NHS number) -- the repeat separator must survive field-level split/join"
      (let [message (:message (:pid-3-repetition by-label))
            pid (first (filter #(= "PID" (first %)) (:segments (er7/parse message))))]
        (is (str/includes? (nth pid 3) "~"))
        (is (str/includes? (nth pid 3) "NHSNBR"))))
    (testing "ORU^R01 with a long OBX tail -- many repeated segments of one name, plus interleaved NTEs"
      (let [message (:message (:oru-long-obx-tail by-label))
            names (map first (:segments (er7/parse message)))]
        (is (str/starts-with? message "MSH|"))
        (is (= "ORU^R01" (simhospital/message-type message)))
        (is (<= 17 (count (filter #(= "OBX" %) names)))
            "the long-tail member must actually carry a long OBX tail")))
    (testing "the lone ADT^A34 -- the corpus's only merge message, and its only MRG segment"
      (let [message (:message (:lone-adt-a34 by-label))
            names (map first (:segments (er7/parse message)))]
        (is (= "ADT^A34" (simhospital/message-type message)))
        (is (some #{"MRG"} names))
        (is (= 1 (count (filter #(= "ADT^A34" (simhospital/message-type %))
                                (simhospital/messages))))
            "A34 must actually be unique in the corpus -- otherwise 'lone' is a lie")))))
