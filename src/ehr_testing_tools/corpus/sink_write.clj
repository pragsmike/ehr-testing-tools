(ns ehr-testing-tools.corpus.sink-write
  "Write (Part III/D3, docs/source-sink-design.md): datum x sink-map ->
  sink-bytes. SS-1 (ruling 8's scope fence) landed dir/file sinks with
  PLAIN write discipline -- fail-if-exists is the default (D3).
  ManifestV1_1 sidecar emission for dir/file is blocked this session on
  D-d (the manifest-interop STOP, docs/source-sink-design.md Decision
  Register) -- write-file!/write-dir! below still emit no sidecar.
  SS-4 Step 3 adds write-stdout!: the :stdout sink, no manifest by
  design (no directory to drop one in), the byte-stream form of the
  composability law -- see Part III's own dated note."
  (:require [clojure.java.io :as io]
            [ehr-testing-tools.corpus.source-sink :as ss]
            [ehr-testing-tools.corpus.framing :as framing]
            [ehr-testing-tools.result :as result])
  (:import [java.io File OutputStream]))

(defn write-file!
  "Writes content (a string) to a :file sink's :path. Fail-if-exists
  (D3's default): result/rejected :sink-target-exists if the target
  file is already there, never overwritten. Missing parent directories
  are created first (same convenience as cli.clj's write-report!).
  Returns result/ok {:path}, or result/rejected :invalid-sink if sink
  doesn't validate as a FileSink."
  [sink content]
  (if-not (ss/valid-sink? sink)
    (result/rejected :invalid-sink {:sink sink})
    (if (not= :file (:kind sink))
      (result/rejected :invalid-sink {:sink sink :hint "write-file! requires a :file sink"})
      (let [{:keys [path]} sink
            f (io/file path)]
        (if (.exists f)
          (result/rejected :sink-target-exists
                            {:path path :hint "remove the file, or wait for SS-4's :overwrite/:append"})
          (do
            (io/make-parents f)
            (spit f content)
            (result/ok {:path path})))))))

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
  implies. Fail-if-exists (D3's default): result/rejected
  :sink-target-exists if the target directory already exists AND is
  non-empty -- an existing empty directory is fine, same convention as
  corpus.generate's own :out-dir-exists guard. Returns result/ok
  {:path}, or result/rejected :invalid-sink if sink doesn't validate
  as a DirSink."
  [sink files]
  (if-not (ss/valid-sink? sink)
    (result/rejected :invalid-sink {:sink sink})
    (if (not= :dir (:kind sink))
      (result/rejected :invalid-sink {:sink sink :hint "write-dir! requires a :dir sink"})
      (let [{:keys [path]} sink]
        (if (non-empty-existing-dir? path)
          (result/rejected :sink-target-exists
                            {:path path :hint "remove the directory, or wait for SS-4's :overwrite/:append"})
          (do
            (doseq [[relative-path content] files]
              (let [f (io/file path relative-path)]
                (io/make-parents f)
                (spit f content)))
            (result/ok {:path path})))))))

(defn write-stdout!
  "Encodes items (ehr-testing-tools.corpus.framing/encode's own item
  shape per :framing kind -- byte arrays for every framing but
  :bundle-entries, whose items are parsed resource data) via a
  :stdout sink's own :framing (defaults to source-sink/default-framing
  when absent -- :file-per-item, exactly one item) and writes the
  resulting bytes to :out (an OutputStream, defaults to System/out --
  injectable so nothing in the hermetic suite writes to the real
  process stdout).

  No manifest: a :stdout sink names no directory to drop one in (Part
  III's own law statement) -- a designed exemption, not a gap.

  Returns result/ok {:bytes-written n}, or result/rejected :invalid-sink
  if sink doesn't validate as a StdoutSink, or
  ehr-testing-tools.corpus.framing/encode's own rejection (e.g.
  :invalid-item-count for :file-per-item with something other than
  exactly one item), propagated unchanged."
  [sink items & {:keys [out] :or {out System/out}}]
  (if (or (not (ss/valid-sink? sink)) (not= :stdout (:kind sink)))
    (result/rejected :invalid-sink {:sink sink :hint "write-stdout! requires a :stdout sink"})
    (let [chosen-framing (or (:framing sink) ss/default-framing)
          encode-result (framing/encode chosen-framing items)]
      (if-not (result/ok? encode-result)
        encode-result
        (let [^bytes bs (:payload encode-result)]
          (.write ^OutputStream out bs)
          (.flush ^OutputStream out)
          (result/ok {:bytes-written (alength bs)}))))))
