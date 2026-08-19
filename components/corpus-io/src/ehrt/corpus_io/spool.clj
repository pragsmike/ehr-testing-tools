(ns ehrt.corpus-io.spool
  "The spool (ruling 4, docs/source-sink-design.md Part I.2/D1): the
  second unification. Just as a generator source resolves to a `dir`
  Source by executing its engine into a fresh directory (SS-2's
  ehrt.corpus.generator-source), a streaming or
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
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.corpus-io.framing :as framing]
            [ehrt.kernel.interface :as kernel])
  (:import [java.io ByteArrayOutputStream]))

(def default-max-bytes
  "1 GiB (D5): the default spool cap, overridable via :max-bytes."
  (* 1024 1024 1024))

(defn default-spool-out-dir
  "The derived out-dir (ruling 4): out/spool/<captured-at> (ADR-0013,
  2026-07-30: moved from target/spool/ -- every zero-flag default lives
  under the single tool-owned out/ root now), sanitized to
  filesystem-safe characters -- time-derived, since a stream's content
  isn't known ahead of a full capture (unlike a generator's own
  seed-derived out-dir, D9's precedent, there is no cheaper deterministic
  input to derive from here)."
  [captured-at]
  (str "out/spool/" (clojure.string/replace captured-at #"[^A-Za-z0-9._-]" "-")))

(defn- non-empty-existing-dir?
  "Mirrors sink-write's own non-empty-existing-dir? (D3's fail-if-exists
  convention): an existing but EMPTY directory is fine to spool into; a
  non-empty one is the fail-if-exists case. Result or loud (ADR-0078):
  delegates to ehrt.kernel.interface/existing-dir-nonempty? so an I/O
  failure listing an EXISTING dir refuses the run instead of silently
  reading as 'empty, safe to spool into.' Returns kernel/ok
  true/false, or kernel/error :listing-failed; callers must unwrap."
  [dir]
  (kernel/existing-dir-nonempty? dir))

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
  same vocabulary as ehrt.cli.core's own format-file-extension,
  applied to spooled items rather than mutate's input/output files."
  {:v2-er7 "hl7" :fhir-json "json" :inferred "dat"})

(defn- pad4
  [i]
  (let [s (str i)]
    (str (apply str (repeat (max 0 (- 4 (count s))) "0")) s)))

(defn- item-filename
  [idx format]
  (str "item-" (pad4 idx) "." (get format->extension format "dat")))

(def ^:private upstream-sniff-bytes
  "How far into the input to look for an upstream envelope's own
  opening `{:status :error`. A real envelope is one short line; a
  corpus is not, and must never be read into a second string just to
  find that out."
  4096)

(defn- upstream-error-envelope
  "The parsed result envelope of a FAILED upstream `ehrt` command, or
  nil when these bytes are not one.

  Every command in this repo prints its own result envelope as EDN on
  stdout, so a failing upstream in a pipe hands `{:status :error ...}`
  to this side's stdin in place of framed corpus bytes. Decoding that
  reports a framing defect in bytes the reader never wrote, and buries
  the cause that actually stopped the run -- so the seam distinguishes
  BEFORE it parses (the D4-3 pattern; fence-battery R-F7).

  Sniffed by prefix first so corpus bytes that merely also start with
  `{` (a FHIR Bundle, :bundle-entries' own input) cost one short regex
  and nothing more."
  [^bytes bs]
  (let [head (String. bs 0 (min (alength bs) upstream-sniff-bytes) "UTF-8")]
    (when (re-find #"^\s*\{\s*:status\s+:(?:error|rejected)[\s,}]" head)
      (let [parsed (try (edn/read-string (String. bs "UTF-8"))
                        (catch Exception _ nil))]
        (when (and (map? parsed) (#{:error :rejected} (:status parsed)))
          parsed)))))

(defn- item-bytes
  "An item, as returned by ehrt.corpus-io.framing/decode, is
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

  Returns kernel/ok {:out-dir :item-count :manifest}, or
  kernel/rejected:
  - :spool-target-exists -- :out-dir already exists and is non-empty
    (D3's fail-if-exists convention), checked before :in is even read.
  - :spool-cap-exceeded {:max-bytes} -- :in has more than :max-bytes
    of content; nothing is written (see the cap discussion above).
  - :empty-input {:origin :framing} -- :in carried no bytes at all.
  - whatever ehrt.corpus-io.framing/decode itself rejects
    with (:malformed-er7-multi-frame, :malformed-mllp-frame, ...),
    propagated unchanged; nothing is written on this path either.

  Or kernel/error :upstream-error {:origin :upstream} -- :in carried a
  failed upstream `ehrt` command's own result envelope rather than
  corpus bytes; :upstream is that envelope verbatim, so the cause
  survives the pipe instead of being reported as a framing defect
  (fence-battery R-F7). Both of the last two are decided BEFORE
  anything is decoded (the D4-3 pattern): a framing category names a
  fault in bytes the caller supplied, and neither of these is that."
  [{:keys [in framing format origin captured-at out-dir max-bytes]
    :or {max-bytes default-max-bytes}}]
  (let [out-dir (or out-dir (default-spool-out-dir captured-at))
        exists-result (non-empty-existing-dir? out-dir)]
    (cond
      (not (kernel/ok? exists-result)) exists-result

      (:payload exists-result)
      (kernel/rejected :spool-target-exists
                        {:out-dir out-dir
                         :hint "remove the directory, or pass a different :out-dir"})

      :else
      (let [{:keys [bytes exceeded?]} (read-capped in max-bytes)
            upstream (when-not exceeded? (upstream-error-envelope bytes))]
        (cond
          exceeded?
          (kernel/rejected :spool-cap-exceeded
                            {:max-bytes max-bytes :out-dir out-dir
                             :hint "pass a larger max-bytes override, or a smaller/paginated input"})

          (zero? (alength ^bytes bytes))
          (kernel/rejected :empty-input
                            {:origin origin :framing framing
                             :hint "nothing arrived to spool -- check that the producer on the other side of the pipe actually ran"})

          upstream
          (kernel/error :upstream-error
                         {:origin origin :upstream upstream
                          :hint "the command feeding this one failed; its own result envelope arrived here instead of corpus bytes"})

          :else
          (let [decode-result (framing/decode framing bytes)]
            (if-not (kernel/ok? decode-result)
              decode-result
              (let [items (:payload decode-result)
                    item-files (map-indexed (fn [i item] {:idx i :bytes (item-bytes framing item)}) items)]
                (kernel/mkdirs! (io/file out-dir))
                (doseq [{:keys [idx bytes]} item-files]
                  (with-open [out (io/output-stream (io/file out-dir (item-filename idx format)))]
                    (.write out ^bytes bytes)))
                (let [item-entries (mapv (fn [{:keys [idx bytes]}]
                                            {:file (item-filename idx format) :sha256 (kernel/sha256-bytes bytes)})
                                          item-files)
                      manifest {:captured-at captured-at :origin origin :framing framing :format format
                                :item-count (count items) :items item-entries}]
                  (spit (io/file out-dir "capture-manifest.edn") (pr-str manifest))
                  (kernel/ok {:out-dir out-dir :item-count (count items) :manifest manifest}))))))))))
