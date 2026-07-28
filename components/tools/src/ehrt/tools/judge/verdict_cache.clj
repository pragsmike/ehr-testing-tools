(ns ehrt.tools.judge.verdict-cache
  "Content-addressed cache at judge.fhir's validator-invocation seam
  (2026-07-27 verification-tiers session, ADR-0016). Skips the
  validator_cli.jar subprocess -- the dominant cost of `make
  integration` (a single `gate fhir` strip recorded at 99s; per-file
  JVM startup ~1-2 min) -- when an identical invocation has already
  been judged. Key = SHA-256 of {input content hash, validator artifact
  identity (name+version+sha256), IG/profile artifact identities, argv
  shape, judge.fhir/verdict-mapping-version}. Any key-component
  omission that could alias two distinct judgments is a correctness
  bug, not a tuning knob (session ruling 3): this namespace widens the
  key rather than narrows it whenever a new invocation-affecting input
  is added to judge.fhir. Value = the judge's own `interpret` output
  ({:verdict :findings [:cause]}), EDN, at
  target/verdict-cache/<key-sha256>.edn (gitignored, like every other
  target/ scratch directory this repo already has).

  Soundness rests on one assumption, stated here because nothing else
  in this repo states it: the pinned validator_cli.jar is DETERMINISTIC
  given identical inputs (same jar, same argv shape, same file bytes)
  -- ADR-0016. Escape hatches when that assumption is ever suspect:
  delete target/verdict-cache/, or disable caching for one invocation
  (CLI: `ehr gate fhir --no-verdict-cache`; library: :verdict-cache?
  false)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.tools.digest :as digest]))

(def default-cache-dir
  "target/verdict-cache")

(defn cache-key
  "SHA-256 hex digest of the EDN-printed key components -- see module
  docstring for the composition and why each component is present.
  :ig-artifacts is a seq of resolved {:name :version :sha256} maps, not
  filesystem paths: a path is machine-local (keyed under this
  machine's own artifact cache root), and the key means to capture the
  logical invocation, not one machine's layout of it."
  [{:keys [content-sha256 validator-artifact ig-artifacts argv-shape verdict-mapping-version]}]
  (digest/sha256-string
   (pr-str {:content-sha256 content-sha256
            :validator (select-keys validator-artifact [:name :version :sha256])
            :ig-artifacts (mapv #(select-keys % [:name :version :sha256]) ig-artifacts)
            :argv-shape (vec argv-shape)
            :verdict-mapping-version verdict-mapping-version})))

(defn- cache-file
  [cache-dir key]
  (io/file (or cache-dir default-cache-dir) (str key ".edn")))

(defn lookup
  "The cached interpret-shaped value for key, or nil on a miss --
  including an absent cache-dir, an absent entry, or a corrupt/
  unreadable one: a bad cache entry degrades to a miss, never a crash,
  since the validator subprocess is always a safe fallback."
  [cache-dir key]
  (let [f (cache-file cache-dir key)]
    (when (.isFile f)
      (try
        (edn/read-string (slurp f))
        (catch Exception _ nil)))))

(defn store!
  "Persists value (judge.fhir/interpret's own output shape) under key.
  Creates cache-dir if absent."
  [cache-dir key value]
  (let [f (cache-file cache-dir key)]
    (io/make-parents f)
    (spit f (pr-str value))))
