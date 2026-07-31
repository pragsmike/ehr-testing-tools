(ns ehrt.corpus-io.sink-write
  "Write (Part III/D3, docs/source-sink-design.md): datum x sink-map ->
  sink-bytes. write-stdout! (SS-4 Step 3): the :stdout sink, no
  manifest by design (no directory to drop one in), the byte-stream
  form of the composability law -- see Part III's own dated note.

  Write discipline (SS-4 Step 5, ruling 7): every write fn below takes
  an optional :mode, one of #{:fail-if-exists :overwrite :append}
  (`:fail-if-exists` the default, D3's original SS-1 behavior,
  unchanged). `:overwrite` is explicit and destructive: it writes
  through whatever fail-if-exists would otherwise have blocked.
  `:append` is honestly per-kind, never a uniform capability:
  - write-file!: sound only when the sink's own :framing is one that
    concatenates soundly at the byte level (append-sound-framings,
    below) -- :er7-multi/:ndjson/:mllp file sinks. Every other framing
    (:bundle-entries -- a JSON document does not concatenate; the
    :file-per-item default -- single-item semantics don't support
    append at all) is rejected :append-unsound, not attempted.
  - write-dir!: REJECTED :append-unsound unconditionally this session,
    regardless of framing -- append-to-a-corpus means merging into an
    existing catalog/manifest, which this session does not build
    (recorded as an OPEN item in docs/source-sink-design.md, not
    improvised here).

  Operation manifest emission (SS-4b Step 3, D-d resolved via ADR-0020,
  docs/source-sink-design.md Part III.5): write-file!/write-dir! both
  accept an optional :operation-manifest argument -- a map of
  :producer/:operation/:written-at plus either :items (already fully
  shaped -- the caller, e.g. a batched CLI loop, already knows exactly
  what it wrote) or :input-hashes (a {relative-path sha256} map used to
  enrich items this function derives itself from the files/content it
  was actually given). :format/:framing on the emitted manifest are
  always read off the sink, never re-declared by the caller -- the
  no-inference-on-write law applies here too. The manifest is written
  last, after every item file (items-then-manifest ordering, ruling 3):
  a process that dies mid-write leaves items without a manifest,
  detectable, never the reverse."
  (:require [clojure.java.io :as io]
            [ehrt.corpus-io.operation-manifest :as operation-manifest]
            [ehrt.corpus-io.source-sink :as ss]
            [ehrt.corpus-io.framing :as framing]
            [ehrt.kernel.interface :as kernel])
  (:import [java.io File OutputStream]))

(defn- derive-items
  "{relative-path content} -> the operation manifest's own :items shape,
  computing :sha256 from the content this call was actually given and
  merging in :input-hash wherever input-hashes (a {relative-path
  sha256} map) names that same path -- absent, not nil, everywhere it
  doesn't (ruling 1's own present-iff-known discipline)."
  [files input-hashes]
  (mapv (fn [[relative-path content]]
          (cond-> {:name relative-path :sha256 (kernel/sha256-string content)}
            (contains? input-hashes relative-path)
            (assoc :input-hash (get input-hashes relative-path))))
        files))

(defn- operation-manifest-payload
  [sink operation-manifest-input files]
  (let [items (or (:items operation-manifest-input)
                  (derive-items files (get operation-manifest-input :input-hashes {})))]
    (operation-manifest/build
     {:producer (:producer operation-manifest-input)
      :operation (:operation operation-manifest-input)
      :written-at (:written-at operation-manifest-input)
      :format (:format sink)
      :framing (or (:framing sink) ss/default-framing)
      :items items})))

(defn- write-dir-operation-manifest!
  "Writes operation-manifest.edn directly under dir-path, last, per
  items-then-manifest ordering. No-op when operation-manifest-input is
  nil -- absence is the SS-4-compatible default every existing
  write-dir! caller keeps getting."
  [dir-path sink operation-manifest-input files]
  (when operation-manifest-input
    (spit (io/file dir-path "operation-manifest.edn")
          (pr-str (operation-manifest-payload sink operation-manifest-input files)))))

(defn- write-file-operation-manifest!
  "Writes operation-manifest.edn as a SIBLING of a :file sink's own
  :path (the target file has no directory of its own to nest a sidecar
  inside) -- one :items entry, named for the target file's own
  basename, :input-hash taken from operation-manifest-input's singular
  :input-hash (a :file sink writes exactly one item, so there is no
  {relative-path ...} map to key by). No-op when
  operation-manifest-input is nil."
  [path sink operation-manifest-input content]
  (when operation-manifest-input
    (let [basename (.getName (io/file path))
          item (cond-> {:name basename :sha256 (kernel/sha256-string content)}
                 (:input-hash operation-manifest-input)
                 (assoc :input-hash (:input-hash operation-manifest-input)))
          payload (operation-manifest-payload sink (assoc operation-manifest-input :items [item]) {})
          manifest-file (io/file (.getParentFile (io/file path)) "operation-manifest.edn")]
      (spit manifest-file (pr-str payload)))))

(def append-sound-framings
  "The framing kinds ruling 7 names as concatenating soundly at the
  byte level: appending a new batch's own framed bytes straight onto
  an existing file's bytes recovers exactly the same items a single
  combined write would have produced. Every other framing kind --
  :bundle-entries (structural JSON, not delimiter-framed) and
  :file-per-item (one file, one item -- append has no sound meaning at
  all) -- is excluded, not merely unlisted."
  #{:er7-multi :ndjson :mllp})

(def ^:private known-write-modes #{:fail-if-exists :overwrite :append})

(defn write-file!
  "Writes content (a string) to a :file sink's :path. :mode (see this
  namespace's own docstring) defaults to :fail-if-exists: kernel/rejected
  :sink-target-exists if the target file is already there, never
  overwritten. :overwrite always writes, existing file or not.
  :append is rejected :append-unsound before anything is written unless
  the sink's own :framing is in append-sound-framings; otherwise the
  content is appended onto the target (creating it, exactly like
  :overwrite, if it doesn't exist yet -- append onto nothing is just a
  write). Missing parent directories are created first in every mode
  (same convenience as cli.clj's write-report!).

  :operation-manifest (see this namespace's own docstring): when
  present, operation-manifest.edn is written as a sibling of :path,
  after content is written, describing this one item.

  Returns kernel/ok {:path}, or kernel/rejected :invalid-sink if sink
  doesn't validate as a FileSink, :invalid-write-mode for an
  unrecognized :mode, or :append-unsound (see above)."
  [sink content & {:keys [mode operation-manifest] :or {mode :fail-if-exists}}]
  (cond
    (not (ss/valid-sink? sink))
    (kernel/rejected :invalid-sink {:sink sink})

    (not= :file (:kind sink))
    (kernel/rejected :invalid-sink {:sink sink :hint "write-file! requires a :file sink"})

    (not (contains? known-write-modes mode))
    (kernel/rejected :invalid-write-mode {:mode mode :valid-options (sort known-write-modes)})

    (and (= :append mode) (not (contains? append-sound-framings (:framing sink))))
    (kernel/rejected :append-unsound
                      {:sink sink :sound-framings (sort append-sound-framings)
                       :hint "append only concatenates soundly for :er7-multi/:ndjson/:mllp -- this sink's own :framing isn't one of those"})

    :else
    (let [{:keys [path]} sink
          f (io/file path)]
      (if (and (= :fail-if-exists mode) (.exists f))
        (kernel/rejected :sink-target-exists
                          {:path path :hint "remove the file, or pass :mode :overwrite/:append"})
        (do
          (io/make-parents f)
          (if (= :append mode)
            (spit f content :append true)
            (spit f content))
          (write-file-operation-manifest! path sink operation-manifest content)
          (kernel/ok {:path path}))))))

(defn- non-empty-existing-dir?
  "Mirrors corpus.generate's own non-empty-existing-dir? (D9's
  :out-dir-exists guard) -- an existing but EMPTY directory is fine to
  write into (mkdirs on an already-existing empty dir is a no-op), a
  non-empty one is the fail-if-exists case."
  [dir]
  (let [f (io/file dir)]
    (and (.isDirectory f) (seq (.listFiles f)))))

(defn write-dir!
  "Writes files (a {relative-path content-string} map) under a :dir
  sink's :path, creating any nested subdirectories relative-path
  implies. :mode (see this namespace's own docstring) defaults to
  :fail-if-exists: kernel/rejected :sink-target-exists if the target
  directory already exists AND is non-empty -- an existing empty
  directory is fine, same convention as corpus.generate's own
  :out-dir-exists guard. :overwrite writes the named files regardless
  of what else the directory already contains (not a directory wipe --
  only the files this call names are touched). :append is REJECTED
  :append-unsound unconditionally (see this namespace's own docstring)
  -- nothing is written on that path.

  :operation-manifest (see this namespace's own docstring): when
  present, operation-manifest.edn is written under :path last, after
  every file this call names.

  Returns kernel/ok {:path}, or kernel/rejected :invalid-sink if sink
  doesn't validate as a DirSink, :invalid-write-mode for an
  unrecognized :mode, or :append-unsound."
  [sink files & {:keys [mode operation-manifest] :or {mode :fail-if-exists}}]
  (cond
    (not (ss/valid-sink? sink))
    (kernel/rejected :invalid-sink {:sink sink})

    (not= :dir (:kind sink))
    (kernel/rejected :invalid-sink {:sink sink :hint "write-dir! requires a :dir sink"})

    (not (contains? known-write-modes mode))
    (kernel/rejected :invalid-write-mode {:mode mode :valid-options (sort known-write-modes)})

    (= :append mode)
    (kernel/rejected :append-unsound
                      {:sink sink
                       :hint "dir sink append means catalog/manifest merge -- an OPEN item (docs/source-sink-design.md), not built this session"})

    :else
    (let [{:keys [path]} sink]
      (if (and (= :fail-if-exists mode) (non-empty-existing-dir? path))
        (kernel/rejected :sink-target-exists
                          {:path path :hint "remove the directory, or pass :mode :overwrite"})
        (do
          (doseq [[relative-path content] files]
            (let [f (io/file path relative-path)]
              (io/make-parents f)
              (spit f content)))
          (write-dir-operation-manifest! path sink operation-manifest files)
          (kernel/ok {:path path}))))))

(defn write-stdout!
  "Encodes items (ehrt.corpus-io.framing/encode's own item
  shape per :framing kind -- byte arrays for every framing but
  :bundle-entries, whose items are parsed resource data) via a
  :stdout sink's own :framing (defaults to source-sink/default-framing
  when absent -- :file-per-item, exactly one item) and writes the
  resulting bytes to :out (an OutputStream, defaults to System/out --
  injectable so nothing in the hermetic suite writes to the real
  process stdout).

  No manifest: a :stdout sink names no directory to drop one in (Part
  III's own law statement) -- a designed exemption, not a gap.

  Returns kernel/ok {:bytes-written n}, or kernel/rejected :invalid-sink
  if sink doesn't validate as a StdoutSink, or
  ehrt.corpus-io.framing/encode's own rejection (e.g.
  :invalid-item-count for :file-per-item with something other than
  exactly one item), propagated unchanged."
  [sink items & {:keys [out] :or {out System/out}}]
  (if (or (not (ss/valid-sink? sink)) (not= :stdout (:kind sink)))
    (kernel/rejected :invalid-sink {:sink sink :hint "write-stdout! requires a :stdout sink"})
    (let [chosen-framing (or (:framing sink) ss/default-framing)
          encode-result (framing/encode chosen-framing items)]
      (if-not (kernel/ok? encode-result)
        encode-result
        (let [^bytes bs (:payload encode-result)]
          (.write ^OutputStream out bs)
          (.flush ^OutputStream out)
          (kernel/ok {:bytes-written (alength bs)}))))))
