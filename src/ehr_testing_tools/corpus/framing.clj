(ns ehr-testing-tools.corpus.framing
  "Framing codecs (D2, docs/source-sink-design.md Part II): pure
  bytes<->item-seq functions per :framing kind -- `decode` (framed
  bytes -> a vector of item byte-arrays) and `encode` (item byte-arrays
  -> framed bytes), dispatched on the same closed enum
  ehr-testing-tools.corpus.source-sink/Framing names. No IO, no
  println; Result-valued on malformed input (ruling 1, SS-3).

  Charset law (ruling 2, SS-3): framing is a byte-level concern;
  charset decoding is not. :er7-multi and :mllp (and :ndjson) operate
  on raw byte arrays throughout -- no java.lang.String conversion, no
  charset applied anywhere in this namespace -- so a payload byte that
  isn't valid UTF-8 (or valid in any particular charset) survives a
  decode/encode round trip byte-identically. v2's MSH-18 (the
  message's own declared charset field) is exactly why this law
  exists: the payload's declared charset is the parser tier's own
  concern (ehr-testing-tools.corpus.er7, judge.v2), never this codec's.

  :bundle-entries is the one named exception (ruling 1): FHIR JSON is
  structurally framed, not delimiter-framed, so decoding it requires
  parsing JSON text -- unavoidably a text operation. FHIR's own spec
  fixes JSON serialization as UTF-8, so that one codec alone reads/
  writes UTF-8 text; its law is item-level identity (entries survive,
  the Bundle envelope does not), never byte-exact."
  (:require [clojure.data.json :as json]
            [ehr-testing-tools.result :as result])
  (:import [java.util Arrays]
           [java.io ByteArrayOutputStream]))

;; ---- byte-level primitives: no String conversion anywhere in the
;; byte-exact codecs below -- see the charset-law docstring above ----

(defn- bytes-at?
  "true if pattern occurs in haystack starting at offset."
  [^bytes haystack ^bytes pattern offset]
  (let [plen (alength pattern)]
    (and (<= (+ ^long offset plen) (alength haystack))
         (loop [i 0]
           (cond
             (= i plen) true
             (= (aget haystack (+ ^long offset i)) (aget pattern i)) (recur (inc i))
             :else false)))))

(defn- index-of-bytes
  "First offset >= from at which pattern occurs in haystack, or -1."
  [^bytes haystack ^bytes pattern from]
  (let [limit (- (alength haystack) (alength pattern))]
    (loop [i (long from)]
      (cond
        (> i limit) -1
        (bytes-at? haystack pattern i) i
        :else (recur (inc i))))))

(defn- concat-bytes
  "Concatenates a seq of byte arrays into one."
  [byte-arrays]
  (let [out (ByteArrayOutputStream.)]
    (doseq [^bytes ba byte-arrays] (.write out ba))
    (.toByteArray out)))

(defn- slice
  [^bytes ba from to]
  (Arrays/copyOfRange ba (long from) (long to)))

;; ---- :file-per-item -- the identity framing, made explicit as the
;; schema default (ruling 1) ----

(defn- decode-file-per-item
  [^bytes bs]
  (result/ok [bs]))

(defn- encode-file-per-item
  [items]
  (if (= 1 (count items))
    (result/ok (first items))
    (result/rejected :invalid-item-count
                      {:framing :file-per-item :count (count items)
                       :hint ":file-per-item encodes exactly one item -- one file, one item"})))

;; ---- :er7-multi -- MSH-line-start detection (ruling 3; the probed
;; grammar, docs/source-sink-design.md Part II) ----

(def ^:private msh-marker (byte-array (map (comp byte int) [\M \S \H])))
(def ^:private lf (byte 0x0A))
(def ^:private message-separator (byte-array [lf lf]))

(defn- msh-line-start-offsets
  "Every offset in bs where a line begins with MSH -- offset 0, or the
  immediately preceding byte is a bare LF. Anchoring to a real message
  start (rather than splitting wherever \\n\\n happens to occur) is
  what makes this robust to a payload that happens to contain a
  literal \\n\\n substring internally (ruling 3)."
  [^bytes bs]
  (loop [i 0 acc []]
    (let [idx (index-of-bytes bs msh-marker i)]
      (if (neg? idx)
        acc
        (recur (inc idx)
               (if (or (zero? idx) (= lf (aget bs (dec idx))))
                 (conj acc idx)
                 acc))))))

(defn- strip-trailing-separator
  "Drops exactly one trailing \\n\\n from item-range, if present --
  the separator every message (including the last) is followed by in
  the probed grammar."
  [^bytes item-range]
  (let [n (alength item-range)]
    (if (and (>= n 2) (= lf (aget item-range (dec n))) (= lf (aget item-range (- n 2))))
      (slice item-range 0 (- n 2))
      item-range)))

(defn- decode-er7-multi
  [^bytes bs]
  (let [starts (msh-line-start-offsets bs)]
    (if (empty? starts)
      (result/rejected :malformed-er7-multi-frame
                        {:hint "no MSH-led message found -- every er7-multi message must start with MSH at a line start"})
      (result/ok (mapv (fn [s e] (strip-trailing-separator (slice bs s e)))
                        starts
                        (conj (vec (rest starts)) (alength bs)))))))

(defn- encode-er7-multi
  [items]
  (result/ok (concat-bytes (interleave items (repeat message-separator)))))

;; ---- dispatch ----

(defn decode
  "framing (one of ehr-testing-tools.corpus.source-sink/Framing's five
  kinds) x bs (a byte array) -> result/ok [item byte-arrays...], or a
  framing-specific result/rejected on malformed input."
  [framing bs]
  (case framing
    :file-per-item (decode-file-per-item bs)
    :er7-multi (decode-er7-multi bs)))

(defn encode
  "The inverse of decode: framing x items (a seq of item byte-arrays)
  -> result/ok a byte array."
  [framing items]
  (case framing
    :file-per-item (encode-file-per-item items)
    :er7-multi (encode-er7-multi items)))
