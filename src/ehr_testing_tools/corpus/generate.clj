(ns ehr-testing-tools.corpus.generate
  "The generation capability (ADR-0004): resolves the Synthea artifact,
  invokes it as a pinned subprocess, and preserves its output tree
  verbatim -- no normalization, no renaming, no post-processing. This is
  the execute half of the two-step engine pattern (pattern nursery #1);
  interpretation into canonical data is a separate, later step. A
  manifest v0 (pattern nursery #2/#3, EXP-A4's hypothesis) is written
  alongside the output tree as the committed provenance record."
  (:require [clojure.java.io :as io]
            [ehr-testing-tools.artifact :as artifact]
            [ehr-testing-tools.invocation :as invocation]
            [ehr-testing-tools.digest :as digest]
            [ehr-testing-tools.corpus.manifest :as manifest]
            [ehr-testing-tools.result :as result])
  (:import [java.util Locale TimeZone]))

(def synthea-name "synthea")
(def synthea-version "4.0.0")
(def default-lockfile-path "artifacts.lock.edn")

(defn- environment-record
  []
  {:locale (str (Locale/getDefault))
   :timezone (str (TimeZone/getDefault))
   :jvm-version (System/getProperty "java.version")})

(defn- synthea-args
  [{:keys [jar-path seed population reference-date config-path output-dir extra-args]}]
  (vec (concat ["-jar" jar-path
                "-s" (str seed)
                "-p" (str population)
                "-r" reference-date
                "-c" config-path
                (str "--exporter.baseDirectory=" output-dir)]
               extra-args)))

(defn generate!
  "Generates a Synthea corpus. Options:
    :config-path     -- path to a repo-authored Synthea properties file
    :seed            -- integer seed
    :population      -- integer population size
    :reference-date  -- YYYYMMDD string. Required, not optional: Synthea
                        generates relative to wall-clock \"now\" unless
                        told otherwise, which would make every run
                        non-reproducible by construction regardless of
                        what else is pinned.
    :output-dir      -- directory for Synthea's output tree + manifest.edn
                        (created if missing; gitignored -- not committed)
    :extra-args      -- additional Synthea CLI args (e.g. for varying
                        generate.thread_pool_size in EXP-A4 rounds),
                        appended after the standard ones
    :java-bin        -- java executable to invoke (default \"java\")
    :lockfile-path, :read-lockfile, :resolve-artifact, :run-invocation
                     -- injectable for testing; default to the real
                        artifact/invocation implementations.

  Never auto-fetches: if the Synthea artifact isn't already resolvable
  from the cache, this returns the resolve failure as-is -- run
  `ehr artifact fetch` first. Returns result/ok {:manifest :output-dir},
  or the first failing step's result (lockfile read, artifact resolve,
  or invocation) unchanged."
  [{:keys [config-path seed population reference-date output-dir extra-args java-bin
           lockfile-path read-lockfile resolve-artifact run-invocation]
    :or {extra-args [] java-bin "java"
         lockfile-path default-lockfile-path
         read-lockfile artifact/read-lockfile
         resolve-artifact artifact/resolve
         run-invocation invocation/run!}}]
  (let [lockfile-result (read-lockfile lockfile-path)]
    (if-not (result/ok? lockfile-result)
      lockfile-result
      (let [artifacts (:artifacts (:payload lockfile-result))
            resolve-result (resolve-artifact artifacts synthea-name synthea-version)]
        (if-not (result/ok? resolve-result)
          resolve-result
          (let [{:keys [path artifact]} (:payload resolve-result)
                out-dir (io/file output-dir)
                _ (.mkdirs out-dir)
                stdout-path (.getAbsolutePath (io/file out-dir "synthea-stdout.log"))
                stderr-path (.getAbsolutePath (io/file out-dir "synthea-stderr.log"))
                args (synthea-args {:jar-path path :seed seed :population population
                                     :reference-date reference-date
                                     :config-path config-path
                                     :output-dir (.getAbsolutePath out-dir)
                                     :extra-args extra-args})
                invocation-result (run-invocation {:command java-bin :args args
                                                    :stdout-path stdout-path
                                                    :stderr-path stderr-path})]
            (if-not (result/ok? invocation-result)
              invocation-result
              (let [m (manifest/build
                       {:generator {:name (:name artifact)
                                    :version (:version artifact)
                                    :sha256 (:sha256 artifact)}
                        :seed seed
                        :config {:path config-path :sha256 (digest/sha256-file config-path)}
                        :invocation (:payload invocation-result)
                        :canonicalizers-applied []
                        :environment (environment-record)})]
                (spit (io/file out-dir "manifest.edn") (pr-str m))
                (result/ok {:manifest m :output-dir output-dir})))))))))
