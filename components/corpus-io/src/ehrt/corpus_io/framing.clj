(ns ehrt.corpus-io.framing
  "Framing codecs (D2, docs/source-sink-design.md Part II): pure
  bytes<->item-seq functions per :framing kind -- `decode` (framed
  bytes -> a vector of item byte-arrays) and `encode` (item byte-arrays
  -> framed bytes), dispatched on the same closed enum
  ehrt.corpus-io.source-sink/Framing names. No IO, no
  println; Result-valued on malformed input (ruling 1, SS-3).

  Charset law (ruling 2, SS-3): framing is a byte-level concern;
  charset decoding is not. :er7-multi and :mllp (and :ndjson) operate
  on raw byte arrays throughout -- no java.lang.String conversion, no
  charset applied anywhere in this namespace -- so a payload byte that
  isn't valid UTF-8 (or valid in any particular charset) survives a
  decode/encode round trip byte-identically. v2's MSH-18 (the
  message's own declared charset field) is exactly why this law
  exists: the payload's declared charset is the parser tier's own
  concern (ehrt.corpus-io.er7, judge.v2), never this codec's.

  :bundle-entries is the one named exception (ruling 1): FHIR JSON is
  structurally framed, not delimiter-framed, so decoding it requires
  parsing JSON text -- unavoidably a text operation. FHIR's own spec
  fixes JSON serialization as UTF-8, so that one codec alone reads/
  writes UTF-8 text; its law is item-level identity (entries survive,
  the Bundle envelope does not), never byte-exact."
  (:require [clojure.data.json :as json]
            [ehrt.kernel.interface :as kernel])
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

(defn- split-bytes
  "haystack split on every occurrence of delimiter, byte-exact -- every
  piece survives, including empty ones (the same no-token-dropped
  discipline ehrt.corpus-io.er7's own split-all uses, applied
  here at the byte level): joining the result with delimiter between
  pieces recovers haystack exactly."
  [^bytes haystack ^bytes delimiter]
  (loop [start 0 acc []]
    (let [idx (index-of-bytes haystack delimiter start)]
      (if (neg? idx)
        (conj acc (slice haystack start (alength haystack)))
        (recur (+ idx (alength delimiter)) (conj acc (slice haystack start idx)))))))

;; ---- :file-per-item -- the identity framing, made explicit as the
;; schema default (ruling 1) ----

(defn- decode-file-per-item
  [^bytes bs]
  (kernel/ok [bs]))

(defn- encode-file-per-item
  [items]
  (if (= 1 (count items))
    (kernel/ok (first items))
    (kernel/rejected :invalid-item-count
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
      (kernel/rejected :malformed-er7-multi-frame
                        {:hint "no MSH-led message found -- every er7-multi message must start with MSH at a line start"})
      (kernel/ok (mapv (fn [s e] (strip-trailing-separator (slice bs s e)))
                        starts
                        (conj (vec (rest starts)) (alength bs)))))))

(defn- encode-er7-multi
  [items]
  (kernel/ok (concat-bytes (interleave items (repeat message-separator)))))

;; ---- :mllp -- the 0x0B / 0x1C 0x0D envelope, byte-exact (ruling 1,
;; D2 Part II). Transport (the wire, nc) is explicitly out of scope --
;; this codec only frames/unframes bytes this repo already has in
;; hand, on either side of that external subprocess. Messages
;; concatenate directly, no filler bytes between one message's 0x1C
;; 0x0D and the next message's own 0x0B. ----

(def ^:private mllp-start (byte 0x0B))
(def ^:private mllp-end (byte-array [(byte 0x1C) (byte 0x0D)]))

(defn- decode-mllp
  [^bytes bs]
  (let [n (alength bs)]
    (loop [pos 0 acc []]
      (cond
        (= pos n) (kernel/ok acc)

        (not= mllp-start (aget bs pos))
        (kernel/rejected :malformed-mllp-frame
                          {:pos pos :hint "expected 0x0B start-of-block"})

        :else
        (let [end-idx (index-of-bytes bs mllp-end (inc pos))]
          (if (neg? end-idx)
            (kernel/rejected :malformed-mllp-frame
                              {:pos pos :hint "no 0x1C 0x0D end-of-block found"})
            (recur (+ end-idx (alength mllp-end))
                   (conj acc (slice bs (inc pos) end-idx)))))))))

(defn- encode-mllp
  [items]
  (kernel/ok (concat-bytes (mapcat (fn [item] [(byte-array [mllp-start]) item mllp-end]) items))))

;; ---- :ndjson -- one JSON value per LF-terminated line, byte-exact
;; (ruling 1). Every item, including the last, is followed by its own
;; trailing LF on encode -- the canonical NDJSON convention -- so
;; decode (which drops exactly one trailing empty piece, the mark of
;; that final LF) recovers the identical item seq. An item is expected
;; to carry no embedded LF of its own (a valid JSON value never emits a
;; raw, unescaped 0x0A byte) -- this codec does not itself validate
;; that the bytes are JSON; it only frames lines. ----

(defn- decode-ndjson
  [^bytes bs]
  (let [pieces (split-bytes bs (byte-array [lf]))
        trailing-empty? (and (seq pieces) (zero? (alength ^bytes (last pieces))))]
    (kernel/ok (vec (if trailing-empty? (butlast pieces) pieces)))))

(defn- encode-ndjson
  [items]
  (kernel/ok (concat-bytes (mapcat (fn [item] [item (byte-array [lf])]) items))))

;; ---- :bundle-entries -- entry-preserving, envelope-lossy (ruling 1).
;; Structural, not delimiter, framing: a FHIR Bundle is a JSON object,
;; so decoding it is unavoidably a text (UTF-8) operation -- the one
;; named exception to the byte-exact codecs above (FHIR JSON is itself
;; always UTF-8 by spec, not an assumption this codec invents). The
;; law is item-level identity: decode(encode(items)) == items as data,
;; never byte-identical -- encode always produces a canonical
;; `collection` Bundle, so any original Bundle-level metadata (id,
;; type, fullUrl, ...) a decoded-from Bundle carried is lost, by
;; design, not by omission. ----

(defn- decode-bundle-entries
  [^bytes bs]
  (try
    (let [parsed (json/read-str (String. bs "UTF-8"))]
      (if (and (map? parsed) (contains? parsed "entry"))
        (kernel/ok (mapv #(get % "resource") (get parsed "entry")))
        (kernel/rejected :malformed-bundle-entries-frame
                          {:hint "expected a FHIR Bundle JSON object with an \"entry\" array"})))
    (catch Exception e
      (kernel/rejected :malformed-bundle-entries-frame
                        {:hint (str "not parseable JSON: " (or (ex-message e) (str e)))}))))

(defn- encode-bundle-entries
  [items]
  (kernel/ok (.getBytes ^String (json/write-str {"resourceType" "Bundle"
                                                  "type" "collection"
                                                  "entry" (mapv (fn [resource] {"resource" resource}) items)})
                         "UTF-8")))

(def known-framings
  "Every framing kind decode/encode dispatch on below -- the in-repo
  'registry' ehrt.docs-tooling.lint's own framing-codec classification
  (target 4, docs/source-sink-design.md Part VIII: 'the same shape as
  corpus.operators/corpus.canonicalizers') checks against via `lookup`."
  #{:file-per-item :er7-multi :ndjson :bundle-entries :mllp})

(defn lookup
  "id (a framing keyword) x version (ignored -- framing kinds aren't
  versioned the way corpus.operators entries are) -> id itself when
  known-framings contains it, else nil. Registry-lookup shape for
  ehrt.docs-tooling.lint's target-4 verification (registry-lookup-fns,
  reached directly via ehrt.corpus-io.interface/lookup since the
  corpus-io split, 2026-07-31 -- docs-tooling.lint no longer relays
  through ehrt.tools.interface for this one, per AR-4's repoint-
  forward rule),
  matching corpus.operators/canonical's own {id version} -> entry-or-
  nil contract."
  [id _version]
  (when (contains? known-framings id) id))

;; ---- dispatch ----

(defn decode
  "framing (one of ehrt.corpus-io.source-sink/Framing's five
  kinds) x bs (a byte array) -> kernel/ok [item byte-arrays...] (item
  data-maps for :bundle-entries), or a framing-specific
  kernel/rejected on malformed input."
  [framing bs]
  (case framing
    :file-per-item (decode-file-per-item bs)
    :er7-multi (decode-er7-multi bs)
    :ndjson (decode-ndjson bs)
    :bundle-entries (decode-bundle-entries bs)
    :mllp (decode-mllp bs)))

(defn encode
  "The inverse of decode: framing x items -> kernel/ok a byte array."
  [framing items]
  (case framing
    :file-per-item (encode-file-per-item items)
    :er7-multi (encode-er7-multi items)
    :ndjson (encode-ndjson items)
    :bundle-entries (encode-bundle-entries items)
    :mllp (encode-mllp items)))
