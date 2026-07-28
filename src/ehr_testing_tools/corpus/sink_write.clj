(ns ehr-testing-tools.corpus.sink-write
  "Write (Part III/D3, docs/source-sink-design.md): datum x sink-map ->
  sink-bytes. SS-1 (ruling 8's scope fence) lands only dir/file sinks
  with PLAIN write discipline -- fail-if-exists is the default (D3);
  :overwrite/:append and ManifestV1_1 sidecar emission are both
  explicitly deferred to SS-4. Nothing here appends a manifest.edn
  beside what it writes -- the composability law (\"every sink's
  output is a valid source\") stays an SS-4 property test obligation,
  not something this namespace claims to satisfy yet."
  (:require [clojure.java.io :as io]
            [ehr-testing-tools.corpus.source-sink :as ss]
            [ehr-testing-tools.result :as result])
  (:import [java.io File]))

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
