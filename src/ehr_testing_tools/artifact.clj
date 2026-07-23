(ns ehr-testing-tools.artifact
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
            [malli.core :as m]
            [ehr-testing-tools.digest :as digest]
            [ehr-testing-tools.result :as result]))

(def ArtifactKind [:enum :engine :profile :module :other])
(def LicenseStatus [:enum :verified :unverified :license-blocked-indeterminate])

(def Artifact
  [:map
   [:kind ArtifactKind]
   [:name :string]
   [:version :string]
   [:sha256 [:re #"^[0-9a-f]{64}$"]]
   [:source :string]
   [:acquired :string]
   [:license-status LicenseStatus]
   [:license-note {:optional true} :string]])

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

(defn resolve
  "Looks up name+version in artifacts (a lockfile's :artifacts vector),
  and answers strictly from the cache -- never touches the network.
  Returns result/ok {:path :artifact}, or result/rejected
  :unknown-artifact (no such name+version in the lockfile) or
  :not-cached (known, but fetch hasn't been run for it yet)."
  ([artifacts name version] (resolve artifacts name version {}))
  ([artifacts name version {:keys [cache-dir-override]}]
   (if-let [artifact (clojure.core/first
                       (filter #(and (= name (:name %)) (= version (:version %))) artifacts))]
     (let [dir (cache-dir cache-dir-override)]
       (if (cached-and-verified? dir (:sha256 artifact))
         (result/ok {:path (.getAbsolutePath (io/file dir (:sha256 artifact))) :artifact artifact})
         (result/rejected :not-cached {:name name :version version})))
     (result/rejected :unknown-artifact {:name name :version version}))))
