(ns ehrt.kernel.artifact
  "External-input registry (ADR-0005): every acquired, external, or binary
  input this repo depends on is an artifact -- {kind, name, version,
  sha256, source, acquired, license-status} -- resolved by name+version
  through a content-addressed cache, never committed to git. `fetch` is
  the only network-touching function (and even it short-circuits to a
  no-network cache hit when possible); `resolve` never touches the
  network at all -- it answers strictly from what's already verified in
  the cache."
  (:refer-clojure :exclude [resolve])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [malli.core :as m]
            [ehrt.kernel.digest :as digest]
            [ehrt.kernel.result :as result]))

(def ArtifactKind [:enum :engine :profile :module :runtime :other])
;; :use-permitted--unstated--confirmation-pending (ADR-0005's 2026-07-24
;; amendment): a fetched-by-the-user-at-their-own-initiative artifact whose
;; use rights are plausible but formal license is unconfirmed -- distinct
;; from :unverified, which this repo has not yet examined at all.
(def LicenseStatus [:enum :verified :unverified :license-blocked-indeterminate
                    :use-permitted--unstated--confirmation-pending])

;; :resolved-via (P2-3, ruled 2026-07-31): most artifacts resolve through
;; the content-addressed cache `resolve`/`fetch` manage (the implied
;; default when this key is absent, :artifact-cache); a row marked
;; :deps-edn instead is provenance/license-only -- the engine actually
;; loads it via a project's own deps.edn (a Maven coordinate resolving
;; into ~/.m2), engine-onboarding checklist item 4's third lockfile
;; target. `ehrt doctor`'s artifact-cache check skips :deps-edn rows
;; rather than reporting a false gap for something that was never meant
;; to be cache-resolved.
(def ResolvedVia [:enum :artifact-cache :deps-edn])

(def Artifact
  [:map
   [:kind ArtifactKind]
   [:name :string]
   [:version :string]
   [:sha256 [:re #"^[0-9a-f]{64}$"]]
   [:source :string]
   [:acquired :string]
   [:license-status LicenseStatus]
   [:license-note {:optional true} :string]
   [:resolved-via {:optional true} ResolvedVia]])

(def Lockfile
  [:map [:artifacts [:vector Artifact]]])

;; ---- lockfile ----

(defn read-lockfile
  "Reads and validates a lockfile at path. Returns result/ok
  {:artifacts [...]}, or result/error :not-found, :parse-failed, or
  :invalid-lockfile."
  [path]
  (if-not (.exists (io/file path))
    (result/error :not-found {:path path})
    (try
      (let [data (edn/read-string (slurp path))]
        (if (m/validate Lockfile data)
          (result/ok data)
          (result/error :invalid-lockfile {:path path
                                            :explanation (m/explain Lockfile data)})))
      (catch Exception e
        (result/error :parse-failed {:path path :message (.getMessage e)})))))

;; ---- cache ----

(defn env-override
  "The EHR_TESTING_TOOLS_CACHE env var, if set. A separate, public
  function (rather than an inline System/getenv call) purely so tests
  can override it with with-redefs instead of mutating real env vars."
  []
  (System/getenv "EHR_TESTING_TOOLS_CACHE"))

(defn cache-dir
  "Resolves the artifact cache directory: an explicit override, else
  EHR_TESTING_TOOLS_CACHE, else ~/.cache/ehr-testing-tools/artifacts."
  ([] (cache-dir nil))
  ([override]
   (or override
       (env-override)
       (str (System/getProperty "user.home") "/.cache/ehr-testing-tools/artifacts"))))

(defn- cached-and-verified?
  [dir sha256]
  (let [f (io/file dir sha256)]
    (and (.exists f) (= sha256 (digest/sha256-file f)))))

(defn default-downloader!
  "The real, network-touching downloader: streams source (a URL string)
  to dest-path. The only function in this namespace that ever performs
  I/O against the network."
  [source dest-path]
  (with-open [in (io/input-stream (java.net.URL. ^String source))]
    (io/copy in (io/file dest-path))))

(defn fetch
  "Ensures artifact is present and hash-verified in the cache, downloading
  it if necessary. Short-circuits with no network access when a
  verified copy is already cached. Returns result/ok {:path :cached},
  result/rejected :hash-mismatch (downloaded bytes don't match the
  artifact's recorded sha256 -- nothing is left in the cache), or
  result/error :download-failed."
  ([artifact] (fetch artifact {}))
  ([{:keys [sha256 source] :as artifact}
    {:keys [downloader cache-dir-override]
     :or {downloader default-downloader!}}]
   (let [dir (cache-dir cache-dir-override)
         dest (io/file dir sha256)]
     (if (cached-and-verified? dir sha256)
       (result/ok {:path (.getAbsolutePath dest) :cached true})
       (do
         (io/make-parents dest)
         (let [tmp (io/file dir (str sha256 ".tmp-" (System/nanoTime)))]
           (try
             (downloader source (.getAbsolutePath tmp))
             (let [actual (digest/sha256-file tmp)]
               (if (= actual sha256)
                 (do (.renameTo tmp dest)
                     (result/ok {:path (.getAbsolutePath dest) :cached false}))
                 (do (.delete tmp)
                     (result/rejected :hash-mismatch {:expected sha256 :actual actual}))))
             (catch Exception e
               (.delete tmp)
               (result/error :download-failed {:source source :message (.getMessage e)})))))))))

(def artifact-remedy-hint
  "The shared :hint text for every rejection this namespace's own
  resolve/resolve-and-extract/find-executable can raise when an
  artifact this repo needs isn't ready yet (cold-start UX session,
  2026-07-30, ADR-0015 amendment) -- deliberately command-agnostic
  (never names a specific generate lane or verb), since the SAME
  rejection surfaces through several different CLI commands
  (`corpus generate synthea`, `gate fhir`, `artifact resolve`, ...),
  all sharing this one namespace's own resolution machinery."
  "run: bin/ehrt artifact fetch --all -- or bin/ehrt doctor to see exactly what's missing")

(defn resolve
  "Looks up name+version in artifacts (a lockfile's :artifacts vector),
  and answers strictly from the cache -- never touches the network.
  Returns result/ok {:path :artifact}, or result/rejected
  :unknown-artifact (no such name+version in the lockfile) or
  :not-cached (known, but fetch hasn't been run for it yet) -- both
  carrying artifact-remedy-hint."
  ([artifacts name version] (resolve artifacts name version {}))
  ([artifacts name version {:keys [cache-dir-override]}]
   (if-let [artifact (clojure.core/first
                       (filter #(and (= name (:name %)) (= version (:version %))) artifacts))]
     (let [dir (cache-dir cache-dir-override)]
       (if (cached-and-verified? dir (:sha256 artifact))
         (result/ok {:path (.getAbsolutePath (io/file dir (:sha256 artifact))) :artifact artifact})
         (result/rejected :not-cached {:name name :version version :hint artifact-remedy-hint})))
     (result/rejected :unknown-artifact {:name name :version version :hint artifact-remedy-hint}))))

;; ---- extraction (archives, not single files -- a JVM's bin/java is
;; unreachable until its tarball is unpacked) ----

(defn extracted-dir
  "The deterministic extraction directory for a cached archive, keyed
  by its own sha256 -- content-addressed like the cache entry itself,
  so extraction is a derived, idempotent side effect of a
  hash-verified download, not new state anyone has to separately
  trust."
  ([sha256] (extracted-dir sha256 nil))
  ([sha256 cache-dir-override]
   (str (cache-dir cache-dir-override) "/extracted/" sha256)))

(defn default-extractor!
  "The real, filesystem-touching extractor: shells out to `tar -xzf`
  -- the only function in this namespace that ever spawns a
  subprocess. Returns result/ok {:dest dest-dir} or result/error
  :extract-failed (carrying artifact-remedy-hint) on a nonzero exit."
  [archive-path dest-dir]
  (.mkdirs (io/file dest-dir))
  (let [pb (ProcessBuilder. (into-array String ["tar" "-xzf" archive-path "-C" dest-dir]))]
    (.redirectErrorStream pb true)
    (let [proc (.start pb)
          output (slurp (.getInputStream proc))
          exit-code (.waitFor proc)]
      (if (zero? exit-code)
        (result/ok {:dest dest-dir})
        (result/error :extract-failed {:archive archive-path :dest dest-dir
                                        :exit-code exit-code :output output
                                        :hint artifact-remedy-hint})))))

(defn- extracted-already?
  "True when dest-dir exists and is non-empty -- the idempotency check
  that lets resolve-and-extract skip re-extracting on every call."
  [dest-dir]
  (let [f (io/file dest-dir)]
    (and (.exists f) (.isDirectory f) (seq (.listFiles f)))))

(defn resolve-and-extract
  "Resolves name+version like `resolve`, then ensures the cached
  archive is unpacked under `extracted-dir` (extracting via
  `extractor` -- default `default-extractor!` -- only when not already
  extracted). Returns result/ok {:extracted-dir :artifact}, or
  propagates `resolve`'s own rejections, or result/error
  :extract-failed from the extractor."
  ([artifacts name version] (resolve-and-extract artifacts name version {}))
  ([artifacts name version {:keys [cache-dir-override extractor] :or {extractor default-extractor!}}]
   (let [resolve-result (resolve artifacts name version {:cache-dir-override cache-dir-override})]
     (if-not (result/ok? resolve-result)
       resolve-result
       (let [{:keys [path artifact]} (:payload resolve-result)
             dest (extracted-dir (:sha256 artifact) cache-dir-override)]
         (if (extracted-already? dest)
           (result/ok {:extracted-dir dest :artifact artifact})
           (let [extract-result (extractor path dest)]
             (if-not (result/ok? extract-result)
               extract-result
               (result/ok {:extracted-dir dest :artifact artifact})))))))))

(defn find-executable
  "Finds a file at relative-path (e.g. \"bin/java\") anywhere under a
  one-level subdirectory of root -- archives extract to a single
  version-named top directory whose exact name this deliberately
  doesn't hardcode. Returns result/ok {:path} or result/rejected
  :executable-not-found (carrying artifact-remedy-hint)."
  [root relative-path]
  (let [root-file (io/file root)
        candidates (when (.isDirectory root-file)
                     (for [child (.listFiles root-file)
                           :when (.isDirectory child)
                           :let [candidate (apply io/file child (str/split relative-path #"/"))]
                           :when (.exists candidate)]
                       candidate))]
    (if-let [found (clojure.core/first (sort-by str candidates))]
      (result/ok {:path (.getAbsolutePath found)})
      (result/rejected :executable-not-found
                        {:root root :relative-path relative-path :hint artifact-remedy-hint}))))
