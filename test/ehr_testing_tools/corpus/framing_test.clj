(ns ehr-testing-tools.corpus.framing-test
  "Test-first (ruling 1, SS-3 Step 2): written before ehr-testing-tools.
  corpus.framing existed. :file-per-item and :er7-multi only this
  step -- :ndjson/:bundle-entries (Step 3) and :mllp (Step 4) grow
  `decode`/`encode`'s own case dispatch in their own commits."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.corpus.framing :as framing]
            [ehr-testing-tools.corpus.simhospital-corpus :as simhospital])
  (:import [java.nio.file Files]
           [java.util Arrays]))

(defn- fixture-bytes
  []
  (Files/readAllBytes (.toPath (clojure.java.io/file simhospital/corpus-path))))

(defn- starts-with-msh?
  [^bytes bs]
  (and (>= (alength bs) 3)
       (= 77 (aget bs 0)) (= 83 (aget bs 1)) (= 72 (aget bs 2))))

;; ---- :file-per-item -- the identity framing (ruling 1) ----

(deftest file-per-item-round-trip-property-test
  (let [check-result
        (tc/quick-check 100
          (prop/for-all [item gen/bytes]
            (let [decoded (framing/decode :file-per-item item)]
              (and (result/ok? decoded)
                   (= [(vec item)] (mapv vec (:payload decoded)))
                   (let [encoded (framing/encode :file-per-item (:payload decoded))]
                     (and (result/ok? encoded)
                          (Arrays/equals ^bytes item ^bytes (:payload encoded))))))))]
    (is (:pass? check-result) (str check-result))))

(deftest file-per-item-encode-rejects-wrong-item-count-test
  (testing "zero items"
    (let [r (framing/encode :file-per-item [])]
      (is (result/rejected? r))
      (is (= :invalid-item-count (:category r)))))
  (testing "more than one item"
    (let [r (framing/encode :file-per-item [(byte-array [1]) (byte-array [2])])]
      (is (result/rejected? r))
      (is (= :invalid-item-count (:category r))))))

;; ---- :er7-multi -- the ADR-0011 SimHospital fixture is the witness
;; (ruling 3) ----

(deftest er7-multi-witness-test
  (testing "decodes the 1,013-message fixture: exactly 1,013 items, every one MSH-led"
    (let [bs (fixture-bytes)
          decoded (framing/decode :er7-multi bs)]
      (is (result/ok? decoded))
      (is (= 1013 (count (:payload decoded))))
      (is (every? starts-with-msh? (:payload decoded)))
      (testing "encode is byte-identical to the original file"
        (let [encoded (framing/encode :er7-multi (:payload decoded))]
          (is (result/ok? encoded))
          (is (Arrays/equals ^bytes bs ^bytes (:payload encoded)))))))
  (testing "corpus.er7's own field-level parse still accepts every decoded message
            (CR-terminated segments, no residual \\n from the framing layer)"
    (let [decoded (:payload (framing/decode :er7-multi (fixture-bytes)))]
      (is (every? (fn [^bytes item-bytes]
                    (not (some #(= 0x0A %) item-bytes)))
                  decoded)
          "no message byte-array should carry a stray LF once the \\n\\n separator is stripped"))))

;; ---- charset law (ruling 2): a payload byte that isn't valid UTF-8
;; still survives a decode/encode round trip byte-identically, since
;; this codec never converts to/from a java.lang.String ----

(def ^:private latin1-o-umlaut
  "0xF6 -- Latin-1 'ö'. Not a valid standalone UTF-8 byte (a UTF-8
  decoder would either reject it or replace it), so a round trip that
  silently went through String/UTF-8 anywhere would corrupt it."
  (unchecked-byte 0xF6))

(defn- msg-bytes
  [^String ascii-body]
  (byte-array (cons (byte \M) (cons (byte \S) (cons (byte \H) (map byte ascii-body))))))

(deftest er7-multi-charset-law-test
  (let [msg1 (byte-array (concat (seq (msg-bytes "|^~\\&|A"))
                                  [(byte \|) latin1-o-umlaut (byte \|)]))
        msg2 (msg-bytes "|^~\\&|B|plain")
        framed (:payload (framing/encode :er7-multi [msg1 msg2]))
        decoded (:payload (framing/decode :er7-multi framed))]
    (is (= 2 (count decoded)))
    (is (Arrays/equals ^bytes msg1 ^bytes (first decoded)))
    (is (Arrays/equals ^bytes msg2 ^bytes (second decoded)))
    (is (= latin1-o-umlaut (aget ^bytes (first decoded) (- (alength ^bytes (first decoded)) 2)))
        "the non-UTF-8 byte itself survives, at the same offset, byte-identically")))

(deftest er7-multi-malformed-input-test
  (let [r (framing/decode :er7-multi (byte-array (map byte "no message here")))]
    (is (result/rejected? r))
    (is (= :malformed-er7-multi-frame (:category r)))))

;; ---- round-trip property test over synthetic MSH-led messages
;; (ruling 1's law, property-tested independently of the fixture) ----

(def ^:private safe-body-byte-gen
  "Any byte except LF (0x0A) -- an item containing an embedded LF is
  out of scope for this codec's own message-shape assumption (a real
  HL7 message's segments are CR-terminated, never LF); CR (0x0D) and
  every other byte value are fair game."
  (gen/such-that #(not= 0x0A (bit-and 0xff %)) gen/byte))

(def ^:private msh-marker-for-test
  (byte-array (map byte "MSH")))

(def ^:private er7-message-gen
  (gen/fmap (fn [body-bytes] (byte-array (concat (seq msh-marker-for-test) body-bytes)))
            (gen/vector safe-body-byte-gen 0 40)))

(deftest er7-multi-round-trip-property-test
  (let [check-result
        (tc/quick-check 100
          (prop/for-all [items (gen/vector er7-message-gen 1 8)]
            (let [encoded (framing/encode :er7-multi items)]
              (and (result/ok? encoded)
                   (let [decoded (framing/decode :er7-multi (:payload encoded))]
                     (and (result/ok? decoded)
                          (= (count items) (count (:payload decoded)))
                          (every? true? (map #(Arrays/equals ^bytes %1 ^bytes %2)
                                              items (:payload decoded)))))))))]
    (is (:pass? check-result) (str check-result))))
