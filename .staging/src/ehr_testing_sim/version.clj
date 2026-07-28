(ns ehr-testing-sim.version
  "The single version source (go-public session, Task 2):
  `resources/version.edn` is the actual pin, edited by hand, never
  generated. `ehr-testing-sim.manifest`'s :generator block and `sim
  version`/`--version` both read it through this namespace, so they
  cannot silently disagree the way a hardcoded literal in each place
  could."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.security MessageDigest]))

(def version
  "This library's own version. Pre-release: no Clojars/Maven
  coordinate exists yet to publish a real release under (ADR-0015
  decision 4's own deferred release-gate ledger)."
  (edn/read-string (slurp (io/resource "version.edn"))))

(defn git-sha
  "This checkout's HEAD commit id, read directly from `.git` (no `git`
  subprocess -- a dev/CI-time nicety, not worth a process dependency)
  relative to the JVM's own working directory -- the common case for
  a clone or CI checkout. Follows one level of symbolic ref (the usual
  `ref: refs/heads/<branch>` HEAD shape); a detached-HEAD checkout's
  HEAD already names the commit id directly. nil, never a thrown
  exception, when `.git` is absent or unreadable (e.g. a stripped
  release artifact with no `.git` alongside it)."
  []
  (try
    (let [head-file (io/file ".git" "HEAD")]
      (when (.exists head-file)
        (let [head (string/trim (slurp head-file))]
          (if (string/starts-with? head "ref: ")
            (let [ref-file (io/file ".git" (subs head 5))]
              (when (.exists ref-file) (string/trim (slurp ref-file))))
            head))))
    (catch Exception _ nil)))

(def ^:private zero-sha256
  (apply str (repeat 64 "0")))

(defn- sha256-hex
  [s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes ^String s "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and (int %) 0xff)) digest))))

(defn generator-sha256
  "The manifest's :generator :sha256 field. Pre-release, there is no
  release artifact to hash -- this is NEVER an artifact digest. When
  `git-sha` finds a readable commit id, this is SHA-256 of that commit
  id's own string: real and deterministic, ties a manifest to the
  exact commit that produced it, but honestly documented as a
  stand-in, not an artifact hash (a real git commit id is 40 hex
  characters, SHA-1 -- too short for `ehr-testing-sim.manifest/
  MirroredManifest`'s own 64-hex-character :sha256 regex, hence the
  hash-of-the-sha rather than the bare sha itself). Absent a readable
  `.git`, this stays the all-zero placeholder this field has always
  shown -- never silently swapped for something that merely LOOKS like
  a hash but isn't derived from anything real."
  []
  (if-let [sha (git-sha)]
    (sha256-hex sha)
    zero-sha256))
