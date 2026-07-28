(ns ehrt.tools.corpus.spool
  "The spool (ruling 4, docs/source-sink-design.md Part I.2/D1): the
  second unification. Just as a generator source resolves to a `dir`
  Source by executing its engine into a fresh directory (SS-2's
  ehrt.tools.corpus.generator-source), a streaming or
  multi-item input resolves to a `dir` Source by decoding its framing
  and spooling one file per item into a derived capture directory,
  plus a capture-manifest.edn sidecar -- one mechanism, no special
  cases.

  Law: every corpus is replayable (Part I.2). Network/pipe input
  exists on disk, one file per item, before anything judges it;
  determinism claims attach to the spool, not the wire.

  The cap (D5, ruling 4): unbounded input into a laptop disk is an
  error, not a surprise. `:in`'s bytes are read up to `:max-bytes`
  (default 1 GiB) BEFORE anything is decoded or written -- a capped
  read that finds more bytes still arriving is :spool-cap-exceeded,
  and because nothing is written to `:out-dir` until the full input is
  known to be under the cap AND decodes cleanly, there is no partial
  spool to clean up on this path; the invariant ruling 4 states
  (never a truncated corpus dressed as success) holds by construction
  rather than by an explicit delete-on-failure step.

  `:captured-at` is record-keeping, not a generation input -- the same
  D8 exemption `corpus.intake`'s own `:received` already has (a batch's
  capture timestamp doesn't determine what bytes get captured); this
  function takes it as a required, explicit string and never reads the
  wall clock itself. The CLI shell (SS-3 Step 6/7) is where a
  wall-clock default belongs, matching `:received`'s own discipline."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [ehrt.tools.digest :as digest]
            [ehrt.tools.corpus.framing :as framing]
            [ehrt.tools.result :as result])
  (:import [java.io ByteArrayOutputStream]))

(def default-max-bytes
  "1 GiB (D5): the default spool cap, overridable via :max-bytes."
  (* 1024 1024 1024))

(defn default-spool-out-dir
  "The derived out-dir (ruling 4): target/spool/<captured-at>, sanitized
  to filesystem-safe characters -- time-derived, since a stream's
  content isn't known ahead of a full capture (unlike a generator's own
  seed-derived out-dir, D9's precedent, there is no cheaper deterministic
  input to derive from here)."
  [captured-at]
  (str "target/spool/" (clojure.string/replace captured-at #"[^A-Za-z0-9._-]" "-")))

(defn- non-empty-existing-dir?
  "Mirrors sink-write's own non-empty-existing-dir? (D3's fail-if-exists
  convention): an existing but EMPTY directory is fine to spool into; a
  non-empty one is the fail-if-exists case."
  [dir]
  (let [f (io/file dir)]
    (and (.isDirectory f) (seq (.listFiles f)))))

(defn- read-capped
  "Reads in (any clojure.java.io/input-stream-coercible value) up to
  max-bytes + 1 bytes -- the +1 is enough to detect the cap was
  exceeded without needing to know the stream's true length, and
  without ever buffering more than max-bytes + 1 bytes regardless of
  how much more the stream still has to offer. Returns {:bytes ...
  :exceeded? bool}."
  [in max-bytes]
  (with-open [stream (io/input-stream in)]
    (let [buf (ByteArrayOutputStream.)
          chunk (byte-array 8192)]
      (loop []
        (let [n (.read stream chunk)]
          (cond
            (neg? n) {:bytes (.toByteArray buf) :exceeded? false}
            (> (+ (.size buf) n) max-bytes) {:bytes (.toByteArray buf) :exceeded? true}
            :else (do (.write buf chunk 0 n) (recur))))))))

(def ^:private format->extension
  "File extension per :format, for the one-file-per-item write below --
  same vocabulary as ehrt.tools.cli's own format-file-extension,
  applied to spooled items rather than mutate's input/output files."
  {:v2-er7 "hl7" :fhir-json "json" :inferred "dat"})

(defn- pad4
  [i]
  (let [s (str i)]
    (str (apply str (repeat (max 0 (- 4 (count s))) "0")) s)))

(defn- item-filename
  [idx format]
  (str "item-" (pad4 idx) "." (get format->extension format "dat")))

(defn- item-bytes
  "An item, as returned by ehrt.tools.corpus.framing/decode, is
  already a byte array for every byte-exact framing kind -- except
  :bundle-entries, whose items are parsed resource data (ruling 1: that
  codec's law is item-level identity, not byte-exact). This function is
  the spool's own materialization step: whatever decode handed back
  becomes the bytes actually written to that item's own file."
  [framing-kind item]
  (if (= :bundle-entries framing-kind)
    (.getBytes ^String (json/write-str item) "UTF-8")
    item))

(defn spool!
  "Spools framed input into one file per item under :out-dir (derived
  from :captured-at via default-spool-out-dir when omitted), plus
  capture-manifest.edn (:captured-at :origin :framing :format
  :item-count :items -- each item {:file :sha256}).

  :in is anything clojure.java.io/input-stream accepts (an
  InputStream, a File, a byte array, ...) -- read up to :max-bytes
  (default default-max-bytes) before anything is decoded or written.

  Returns result/ok {:out-dir :item-count :manifest}, or
  result/rejected:
  - :spool-target-exists -- :out-dir already exists and is non-empty
    (D3's fail-if-exists convention), checked before :in is even read.
  - :spool-cap-exceeded {:max-bytes} -- :in has more than :max-bytes
    of content; nothing is written (see the cap discussion above).
  - whatever ehrt.tools.corpus.framing/decode itself rejects
    with (:malformed-er7-multi-frame, :malformed-mllp-frame, ...),
    propagated unchanged; nothing is written on this path either."
  [{:keys [in framing format origin captured-at out-dir max-bytes]
    :or {max-bytes default-max-bytes}}]
  (let [out-dir (or out-dir (default-spool-out-dir captured-at))]
    (if (non-empty-existing-dir? out-dir)
      (result/rejected :spool-target-exists
                        {:out-dir out-dir
                         :hint "remove the directory, or pass a different :out-dir"})
      (let [{:keys [bytes exceeded?]} (read-capped in max-bytes)]
        (if exceeded?
          (result/rejected :spool-cap-exceeded
                            {:max-bytes max-bytes :out-dir out-dir
                             :hint "pass a larger max-bytes override, or a smaller/paginated input"})
          (let [decode-result (framing/decode framing bytes)]
            (if-not (result/ok? decode-result)
              decode-result
              (let [items (:payload decode-result)
                    item-files (map-indexed (fn [i item] {:idx i :bytes (item-bytes framing item)}) items)]
                (.mkdirs (io/file out-dir))
                (doseq [{:keys [idx bytes]} item-files]
                  (with-open [out (io/output-stream (io/file out-dir (item-filename idx format)))]
                    (.write out ^bytes bytes)))
                (let [item-entries (mapv (fn [{:keys [idx bytes]}]
                                            {:file (item-filename idx format) :sha256 (digest/sha256-bytes bytes)})
                                          item-files)
                      manifest {:captured-at captured-at :origin origin :framing framing :format format
                                :item-count (count items) :items item-entries}]
                  (spit (io/file out-dir "capture-manifest.edn") (pr-str manifest))
                  (result/ok {:out-dir out-dir :item-count (count items) :manifest manifest}))))))))))
