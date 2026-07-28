(ns ehrt.tools.corpus.generator-source
  "The unification (D1, docs/source-sink-design.md Part I.2; SS-2 Step
  2): a generator source resolves to a dir Source by validating its
  params (ehrt.tools.corpus.generators), deriving a stable
  out-dir, executing the registered engine, and verifying the
  directory materialized non-empty -- exactly the shape corpus.generate
  already produces for synthea, generalized to every registered kind.
  Result-valued throughout; three distinct rejections, not one:

  - a pre-existing, non-empty out-dir (:out-dir-exists) -- checked
    BEFORE the engine ever runs, so a caller never pays for (or
    silently no-ops through) a second run into the first run's own
    directory. Owned HERE, uniformly, rather than left to each
    registered kind to reimplement for itself (corpus.generate's own
    :out-dir-exists guard stays in place too, for its own direct
    `ehr corpus generate` call path, unchanged by this session --
    ruling 6);
  - the engine's own failure Result, propagated unchanged
    (:execute-fn's own category, whatever it is);
  - a successful execute-fn that nonetheless left the out-dir empty
    (:generator-produced-no-output) -- an engine claiming success
    while writing nothing is caught here, not silently accepted as a
    valid (empty) corpus."
  (:require [clojure.java.io :as io]
            [ehrt.tools.corpus.generators :as generators]
            [ehrt.tools.corpus.source-sink :as source-sink]
            [ehrt.tools.result :as result])
  (:import [java.io File]))

(defn- non-empty-existing-dir?
  [dir]
  (let [f (io/file dir)]
    (and (.isDirectory f) (seq (.listFiles f)))))

(defn resolve!
  "kind -- a registered generator :kind (ehrt.tools.corpus.
  generators); params -- the caller-supplied, kind-specific params
  (merged onto the registry's own pinned defaults, D8, by
  generators/resolve-params). Returns result/ok a canonical :dir
  Source over the freshly generated corpus, or one of the three
  rejections above, or generators/resolve-params's own
  :unknown-generator-kind / :invalid-generator-params, propagated
  unchanged."
  [kind params]
  (let [params-result (generators/resolve-params kind params)]
    (if-not (result/ok? params-result)
      params-result
      (let [merged-params (:payload params-result)
            entry (generators/lookup kind)
            out-dir ((:out-dir-fn entry) merged-params)]
        (if (non-empty-existing-dir? out-dir)
          (result/error :out-dir-exists
                        {:kind kind :out-dir out-dir
                         :hint "remove the directory, or pass different params (e.g. a different seed), to regenerate"})
          (let [execute-result ((:execute-fn entry) merged-params out-dir)]
            (if-not (result/ok? execute-result)
              execute-result
              (if-not (non-empty-existing-dir? out-dir)
                (result/error :generator-produced-no-output {:kind kind :out-dir out-dir})
                (source-sink/dir-source {:path out-dir})))))))))
