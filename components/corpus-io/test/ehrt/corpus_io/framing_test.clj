(ns ehrt.corpus-io.framing-test
  "Test-first (ruling 1, SS-3): written before ehrt.corpus-io.
  framing existed, then grown one codec per commit (Steps 2-4):
  :file-per-item/:er7-multi, then :ndjson/:bundle-entries, then :mllp."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus-io.framing :as framing]
            ;; simhospital-corpus is a test fixture helper that stayed
            ;; in components/corpus/test (corpus-io stage 2, 2026-07-31
            ;; -- not one of AR-1's 17 moved src namespaces).
            [ehrt.corpus.simhospital-corpus :as simhospital])
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
              (and (kernel/ok? decoded)
                   (= [(vec item)] (mapv vec (:payload decoded)))
                   (let [encoded (framing/encode :file-per-item (:payload decoded))]
                     (and (kernel/ok? encoded)
                          (Arrays/equals ^bytes item ^bytes (:payload encoded))))))))]
    (is (:pass? check-result) (str check-result))))

(deftest file-per-item-encode-rejects-wrong-item-count-test
  (testing "zero items"
    (let [r (framing/encode :file-per-item [])]
      (is (kernel/rejected? r))
      (is (= :invalid-item-count (:category r)))))
  (testing "more than one item"
    (let [r (framing/encode :file-per-item [(byte-array [1]) (byte-array [2])])]
      (is (kernel/rejected? r))
      (is (= :invalid-item-count (:category r))))))

;; ---- :er7-multi -- the ADR-0011 SimHospital fixture is the witness
;; (ruling 3) ----

(deftest er7-multi-witness-test
  (testing "decodes the 1,013-message fixture: exactly 1,013 items, every one MSH-led"
    (let [bs (fixture-bytes)
          decoded (framing/decode :er7-multi bs)]
      (is (kernel/ok? decoded))
      (is (= 1013 (count (:payload decoded))))
      (is (every? starts-with-msh? (:payload decoded)))
      (testing "encode is byte-identical to the original file"
        (let [encoded (framing/encode :er7-multi (:payload decoded))]
          (is (kernel/ok? encoded))
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
    (is (kernel/rejected? r))
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
              (and (kernel/ok? encoded)
                   (let [decoded (framing/decode :er7-multi (:payload encoded))]
                     (and (kernel/ok? decoded)
                          (= (count items) (count (:payload decoded)))
                          (every? true? (map #(Arrays/equals ^bytes %1 ^bytes %2)
                                              items (:payload decoded)))))))))]
    (is (:pass? check-result) (str check-result))))

;; ---- :ndjson -- byte-exact round trip (ruling 1) ----

(def ^:private ndjson-line-gen
  "A byte array with no embedded LF -- NDJSON's own real invariant (a
  valid JSON value never emits a raw 0x0A byte; embedded newlines are
  always \\n-escaped inside a JSON string)."
  (gen/fmap byte-array (gen/vector safe-body-byte-gen 0 40)))

(deftest ndjson-round-trip-property-test
  (let [check-result
        (tc/quick-check 100
          (prop/for-all [items (gen/vector ndjson-line-gen 0 8)]
            (let [encoded (framing/encode :ndjson items)]
              (and (kernel/ok? encoded)
                   (let [decoded (framing/decode :ndjson (:payload encoded))]
                     (and (kernel/ok? decoded)
                          (= (count items) (count (:payload decoded)))
                          (every? true? (map #(Arrays/equals ^bytes %1 ^bytes %2)
                                              items (:payload decoded)))))))))]
    (is (:pass? check-result) (str check-result))))

(deftest ndjson-concrete-example-test
  (let [items [(byte-array (map byte "{\"a\":1}")) (byte-array (map byte "{\"b\":2}"))]
        encoded (:payload (framing/encode :ndjson items))]
    (is (= "{\"a\":1}\n{\"b\":2}\n" (String. ^bytes encoded "UTF-8"))
        "every item, including the last, gets its own trailing LF")
    (let [decoded (:payload (framing/decode :ndjson encoded))]
      (is (= 2 (count decoded)))
      (is (every? true? (map #(Arrays/equals ^bytes %1 ^bytes %2) items decoded))))))

(deftest ndjson-empty-input-test
  (let [decoded (framing/decode :ndjson (byte-array 0))]
    (is (kernel/ok? decoded))
    (is (= [] (:payload decoded)))))

;; ---- :bundle-entries -- entry-preserving, envelope-lossy (ruling 1) ----

(def ^:private resource-gen
  (gen/let [resource-type (gen/elements ["Patient" "Observation" "Encounter"])
            id (gen/fmap str gen/nat)]
    {"resourceType" resource-type "id" id}))

(deftest bundle-entries-entry-preserving-property-test
  (testing "decode(encode(resources)) == resources, as data -- never claimed byte-exact"
    (let [check-result
          (tc/quick-check 50
            (prop/for-all [resources (gen/vector resource-gen 0 5)]
              (let [encoded (framing/encode :bundle-entries resources)]
                (and (kernel/ok? encoded)
                     (let [decoded (framing/decode :bundle-entries (:payload encoded))]
                       (and (kernel/ok? decoded)
                            (= resources (:payload decoded))))))))]
      (is (:pass? check-result) (str check-result)))))

(deftest bundle-entries-envelope-is-lossy-test
  (testing "encode always produces a canonical `collection` Bundle -- a decoded-
            from Bundle's own id/type are never carried through re-encoding"
    (let [original (byte-array (map byte (str "{\"resourceType\":\"Bundle\",\"id\":\"abc\","
                                               "\"type\":\"searchset\",\"entry\":"
                                               "[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"1\"}}]}")))
          decoded (:payload (framing/decode :bundle-entries original))
          re-encoded (:payload (framing/encode :bundle-entries decoded))
          re-parsed (clojure.data.json/read-str (String. ^bytes re-encoded "UTF-8"))]
      (is (= [{"resourceType" "Patient" "id" "1"}] decoded))
      (is (= "collection" (get re-parsed "type")))
      (is (not (contains? re-parsed "id")) "the original Bundle's own :id is not fabricated back"))))

(deftest bundle-entries-malformed-input-test
  (testing "not JSON at all"
    (let [r (framing/decode :bundle-entries (byte-array (map byte "not json")))]
      (is (kernel/rejected? r))
      (is (= :malformed-bundle-entries-frame (:category r)))))
  (testing "valid JSON but no \"entry\" key"
    (let [r (framing/decode :bundle-entries (byte-array (map byte "{\"resourceType\":\"Bundle\"}")))]
      (is (kernel/rejected? r))
      (is (= :malformed-bundle-entries-frame (:category r))))))

;; ---- :mllp -- the 0x0B / 0x1C 0x0D envelope, byte-exact (ruling 1;
;; Step 4) ----

(def ^:private mllp-body-byte-gen
  "Any byte except 0x0B (VT, the start marker) and 0x1C (FS, the first
  end-marker byte) -- content containing either would be ambiguous
  with the envelope itself; every other byte, including CR (0x0D), is
  fair game."
  (gen/such-that #(not (contains? #{0x0B 0x1C} (bit-and 0xff %))) gen/byte))

(def ^:private mllp-message-gen
  (gen/fmap byte-array (gen/vector mllp-body-byte-gen 0 40)))

(deftest mllp-round-trip-property-test
  (let [check-result
        (tc/quick-check 100
          (prop/for-all [items (gen/vector mllp-message-gen 0 8)]
            (let [encoded (framing/encode :mllp items)]
              (and (kernel/ok? encoded)
                   (let [decoded (framing/decode :mllp (:payload encoded))]
                     (and (kernel/ok? decoded)
                          (= (count items) (count (:payload decoded)))
                          (every? true? (map #(Arrays/equals ^bytes %1 ^bytes %2)
                                              items (:payload decoded)))))))))]
    (is (:pass? check-result) (str check-result))))

(deftest mllp-concrete-example-test
  (let [items [(byte-array (map byte "MSH|^~\\&|A")) (byte-array (map byte "MSH|^~\\&|B"))]
        encoded (:payload (framing/encode :mllp items))]
    (is (= (concat [0x0B] (map int "MSH|^~\\&|A") [0x1C 0x0D]
                   [0x0B] (map int "MSH|^~\\&|B") [0x1C 0x0D])
           (map #(bit-and 0xff %) encoded)))
    (let [decoded (:payload (framing/decode :mllp encoded))]
      (is (every? true? (map #(Arrays/equals ^bytes %1 ^bytes %2) items decoded))))))

(deftest mllp-charset-law-test
  (let [msg (byte-array (concat (map byte "A|") [latin1-o-umlaut] (map byte "|B")))
        framed (:payload (framing/encode :mllp [msg]))
        decoded (:payload (framing/decode :mllp framed))]
    (is (= 1 (count decoded)))
    (is (Arrays/equals ^bytes msg ^bytes (first decoded)))))

(deftest mllp-malformed-input-test
  (testing "doesn't start with 0x0B"
    (let [r (framing/decode :mllp (byte-array (map byte "no vt here")))]
      (is (kernel/rejected? r))
      (is (= :malformed-mllp-frame (:category r)))))
  (testing "no end-of-block marker"
    (let [r (framing/decode :mllp (byte-array (cons 0x0B (map int "unterminated"))))]
      (is (kernel/rejected? r))
      (is (= :malformed-mllp-frame (:category r))))))

;; ---- :batch -- the HL7 v2 batch protocol's BHS/BTS envelope around an
;; :er7-multi-framed message block (ADR-0111) ----

(deftest batch-round-trip-property-test
  (let [check-result
        (tc/quick-check 100
          (prop/for-all [items (gen/vector er7-message-gen 1 8)]
            (let [encoded (framing/encode :batch items)]
              (and (kernel/ok? encoded)
                   (let [decoded (framing/decode :batch (:payload encoded))]
                     (and (kernel/ok? decoded)
                          (= (count items) (count (:payload decoded)))
                          (every? true? (map #(Arrays/equals ^bytes %1 ^bytes %2)
                                              items (:payload decoded)))))))))]
    (is (:pass? check-result) (str check-result))))

(deftest batch-empty-items-round-trip-test
  (let [encoded (:payload (framing/encode :batch []))
        decoded (framing/decode :batch encoded)]
    (is (kernel/ok? decoded))
    (is (= [] (:payload decoded)))))

(deftest batch-concrete-example-test
  (let [msg1 (byte-array (map byte "MSH|^~\\&|A"))
        msg2 (byte-array (map byte "MSH|^~\\&|B"))
        encoded (:payload (framing/encode :batch [msg1 msg2]))]
    (is (= (concat (map int "BHS|^~\\&") [0x0A 0x0A]
                   (map int "MSH|^~\\&|A") [0x0A 0x0A]
                   (map int "MSH|^~\\&|B") [0x0A 0x0A]
                   (map int "BTS|2") [0x0A 0x0A])
           (map #(bit-and 0xff %) encoded))
        "BHS, then each message er7-multi-framed, then BTS|<true count>, \\n\\n-separated throughout")
    (let [decoded (:payload (framing/decode :batch encoded))]
      (is (= 2 (count decoded)))
      (is (every? true? (map #(Arrays/equals ^bytes %1 ^bytes %2) [msg1 msg2] decoded))))))

(deftest batch-charset-law-test
  (let [msg (byte-array (concat (seq (msg-bytes "|^~\\&|A"))
                                 [(byte \|) latin1-o-umlaut (byte \|)]))
        encoded (:payload (framing/encode :batch [msg]))
        decoded (:payload (framing/decode :batch encoded))]
    (is (= 1 (count decoded)))
    (is (Arrays/equals ^bytes msg ^bytes (first decoded))
        "the non-UTF-8 message byte survives -- :batch's own BHS/BTS bytes are
         fixed ASCII structural bytes only, never touching message content")))

(deftest batch-bts1-mismatch-is-categorized-test
  (let [items [(byte-array (map byte "MSH|^~\\&|A")) (byte-array (map byte "MSH|^~\\&|B"))]
        encoded (:payload (framing/encode :batch items))
        ;; Tamper the declared count: "BTS|2" -> "BTS|9", same byte length.
        tampered (byte-array (map (fn [b] (if (= (byte \2) b) (byte \9) b)) encoded))
        r (framing/decode :batch tampered)]
    (is (kernel/rejected? r))
    (is (= :batch-count-mismatch (:category r)))
    (is (= 9 (:declared (:payload r))))
    (is (= 2 (:actual (:payload r))))))

(deftest batch-malformed-missing-bhs-test
  (let [r (framing/decode :batch (byte-array (map byte "MSH|^~\\&|A\n\nBTS|1\n\n")))]
    (is (kernel/rejected? r))
    (is (= :malformed-batch-frame (:category r)))))

(deftest batch-malformed-missing-bts-test
  (let [r (framing/decode :batch (byte-array (map byte "BHS|^~\\&\n\nMSH|^~\\&|A\n\n")))]
    (is (kernel/rejected? r))
    (is (= :malformed-batch-frame (:category r)))))

(deftest batch-malformed-non-numeric-bts1-test
  (let [r (framing/decode :batch (byte-array (map byte "BHS|^~\\&\n\nMSH|^~\\&|A\n\nBTS|abc\n\n")))]
    (is (kernel/rejected? r))
    (is (= :malformed-batch-frame (:category r)))))

;; ---- lookup -- the registry-lookup shape ehrt.docs-tooling.lint's own
;; target-4 framing-codec classification checks against (Step 7, via
;; ehrt.corpus-io.interface/lookup since the corpus-io split,
;; 2026-07-31) ----

(deftest lookup-test
  (testing "every known framing kind resolves, :version ignored"
    (doseq [f framing/known-framings]
      (is (= f (framing/lookup f "1")))
      (is (= f (framing/lookup f "anything")))))
  (testing "an unknown framing keyword resolves to nil"
    (is (nil? (framing/lookup :not-a-real-framing "1")))))
