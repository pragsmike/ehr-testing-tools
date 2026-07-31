(ns ehrt.tools.display-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [ehrt.tools.display :as display]
            [ehrt.corpus-io.interface :as corpus-io]
            [ehrt.kernel.interface :as kernel]))

(def ^:private fixture-dir "components/tools/test-fixtures/v2")

(defn- fixture-content
  [name]
  (slurp (io/file fixture-dir name)))

;; ---- render-er7-message: the exact per-message call shape the
;; future player's ticker sink will make -- no stream layer involved. ----

(deftest render-er7-message-cr-to-lf-and-trailing-separator-stripped-test
  (is (= "MSH|^~\\&|A\nPID|1" (display/render-er7-message "MSH|^~\\&|A\rPID|1\r"))))

(deftest render-er7-message-no-trailing-cr-is-left-alone-test
  (is (= "MSH|^~\\&|A\nPID|1" (display/render-er7-message "MSH|^~\\&|A\rPID|1"))))

(deftest render-er7-message-contains-no-cr-bytes-test
  (let [rendered (display/render-er7-message (fixture-content "adt-a01-admit.hl7"))]
    (is (not (str/includes? rendered "\r")))))

(deftest render-er7-message-preserves-segment-count-test
  (let [raw (fixture-content "adt-a01-admit.hl7")
        raw-trimmed (if (str/ends-with? raw "\r") (subs raw 0 (dec (count raw))) raw)
        segment-count (count (str/split raw-trimmed #"\r" -1))
        rendered (display/render-er7-message raw)]
    (is (= segment-count (count (str/split rendered #"\n" -1))))))

;; ---- render-er7-stream: the stream layer -- split via the same
;; MSH-line-start boundary framing/decode's :er7-multi codec uses,
;; render each message, join with a blank line. ----

(defn- er7-multi-blob
  "Two real fixtures, joined by the same \\n\\n separator
  ehrt.corpus-io.framing's own encode-er7-multi produces -- an
  er7-multi stream a real SS-3 stdin capture would contain."
  []
  (str (fixture-content "adt-a01-admit.hl7") "\n\n" (fixture-content "adt-a02-transfer.hl7")))

(deftest render-er7-stream-renders-each-message-blank-line-separated-test
  (let [r (display/render-er7-stream (er7-multi-blob))]
    (is (kernel/ok? r))
    (is (= 2 (count (str/split (:payload r) #"\n\n"))))))

(deftest render-er7-stream-contains-no-cr-bytes-test
  (let [r (display/render-er7-stream (er7-multi-blob))]
    (is (kernel/ok? r))
    (is (not (str/includes? (:payload r) "\r")))))

(deftest render-er7-stream-agrees-with-framings-own-er7-multi-splitter-test
  ;; The message-boundary rule is the SAME one framing/decode's
  ;; :er7-multi codec uses -- this namespace does not invent a second
  ;; splitter. Asserted directly: decoding the same blob via
  ;; framing/decode must yield the same number of messages
  ;; render-er7-stream rendered (one blank-line-joined block per
  ;; message).
  (let [blob (er7-multi-blob)
        decoded (corpus-io/decode :er7-multi (.getBytes blob "UTF-8"))
        rendered (display/render-er7-stream blob)]
    (is (kernel/ok? decoded))
    (is (kernel/ok? rendered))
    (is (= (count (:payload decoded))
           (count (str/split (:payload rendered) #"\n\n"))))))

(deftest render-er7-stream-rejects-content-with-no-msh-led-message-test
  (let [r (display/render-er7-stream "not an hl7 message at all")]
    (is (kernel/rejected? r))
    (is (= :malformed-er7-multi-frame (:category r)))))

;; ---- render-fhir-json ----

(deftest render-fhir-json-pretty-prints-test
  (let [r (display/render-fhir-json "{\"resourceType\":\"Patient\",\"id\":\"1\"}")]
    (is (kernel/ok? r))
    (is (str/includes? (:payload r) "\n"))
    (is (str/includes? (:payload r) "resourceType"))))

(deftest render-fhir-json-rejects-malformed-content-test
  (let [r (display/render-fhir-json "not json")]
    (is (kernel/rejected? r))
    (is (= :malformed-fhir-json (:category r)))))

;; ---- read-only by construction: rendering never touches the source
;; file (asserted directly, not merely assumed from the function
;; signatures taking a string rather than a path). ----

(deftest rendering-never-modifies-the-source-file-test
  (let [f (io/file fixture-dir "adt-a01-admit.hl7")
        before (slurp f)]
    (display/render-er7-message before)
    (display/render-er7-stream before)
    (is (= before (slurp f)) "the fixture file must be byte-identical after rendering")))

;; ---- property: rendering preserves segment count per message, over
;; every v2 fixture in the shared test-fixtures directory. ----

(deftest render-er7-message-preserves-segment-count-across-all-v2-fixtures-property-test
  (doseq [f (.listFiles (io/file fixture-dir))
          :when (str/ends-with? (.getName f) ".hl7")]
    (let [raw (slurp f)
          raw-trimmed (if (str/ends-with? raw "\r") (subs raw 0 (dec (count raw))) raw)
          segment-count (count (str/split raw-trimmed #"\r" -1))
          rendered (display/render-er7-message raw)]
      (testing (.getName f)
        (is (= segment-count (count (str/split rendered #"\n" -1))))
        (is (not (str/includes? rendered "\r")))))))
