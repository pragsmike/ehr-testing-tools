(ns ehrt.kernel.io
  "Guarded I/O primitives (result or loud, ADR-0078). `java.io.File`'s
  own `.listFiles`/`.renameTo` both signal an I/O failure by returning
  a falsy value indistinguishable, in code, from a genuinely empty
  directory or a completed rename -- the D4-1 story this repo's own
  first review (`.agents/plans/2026-08-07-repo-review-findings.md`,
  ADR-0077) found live at 9+ production call sites, one demonstrated
  to turn a real I/O failure into `mutate-command`'s own clean,
  successful, wrong `{:count 0}` answer. `list-files`/
  `existing-dir-nonempty?`/`rename!` generalize ADR-0076's own one-off
  fix (`ehrt.sim.run/similar-sibling-config`, AR-QR-2 -- same
  retry-once-then-name-the-failure idiom, same discipline of naming
  the failure mode instead of absorbing it silently) into this
  namespace's own shared vocabulary, so every call site that scans or
  moves a file can go through here instead of re-deriving the fix.

  `lister`/`renamer` are injectable (mirrors this repo's other I/O
  seams -- `ehrt.kernel.artifact/fetch`'s `:downloader`,
  `resolve-and-extract`'s `:extractor`) so a test can simulate a real
  I/O failure -- permission-denied, directory removed mid-read --
  without needing to actually provoke one hermetically, which is
  awkward and non-portable across CI runners."
  (:require [clojure.java.io :as io]
            [ehrt.kernel.result :as result]))

(defn list-files
  "dir -> result/ok a vector of its entries (empty when the directory
  genuinely has none), or result/error :listing-failed {:path} when
  `lister` returns nil -- an I/O failure, NOT an empty directory
  (which yields an empty array, unambiguously ok here). Retried once
  before erroring (a transient filesystem hiccup on a CI runner
  self-heals, ADR-0076's own AR-QR-2 precedent) -- a still-nil result
  after the retry is named as the failure it is."
  ([dir] (list-files dir (fn [^java.io.File f] (.listFiles f))))
  ([dir lister]
   (let [f (io/file dir)]
     (if-let [files (or (lister f) (lister f))]
       (result/ok (vec files))
       (result/error :listing-failed {:path (.getPath f)})))))

(defn existing-dir-nonempty?
  "Guards a :fail-if-exists/:out-dir-exists check (D9's determinism
  convention): a path that doesn't exist or isn't a directory is safe
  -- result/ok false. An existing directory's own emptiness is checked
  via `list-files`, whose own I/O-failure/empty distinction this guard
  inherits directly: an I/O failure listing an EXISTING directory must
  refuse the run, never silently read as 'directory is empty, safe to
  proceed' -- exactly the guard-defeat the register's D3-4/D4-1 rows
  found live at several call sites. Returns result/ok true/false, or
  result/error :listing-failed."
  ([dir] (existing-dir-nonempty? dir (fn [^java.io.File f] (.listFiles f))))
  ([dir lister]
   (let [f (io/file dir)]
     (if (.isDirectory f)
       (let [r (list-files f lister)]
         (if (result/ok? r)
           (result/ok (boolean (seq (:payload r))))
           r))
       (result/ok false)))))

(defn rename!
  "Renames src to dest via java.io.File's own .renameTo, whose boolean
  return is easy to ignore -- false (e.g. a cross-filesystem rename,
  common on CI runners) means the file was NEVER moved, silently,
  unless checked. Returns result/ok {:from :to} or result/error
  :rename-failed {:from :to}."
  ([src dest] (rename! src dest (fn [^java.io.File f ^java.io.File t] (.renameTo f t))))
  ([src dest renamer]
   (let [from (io/file src)
         to (io/file dest)]
     (if (renamer from to)
       (result/ok {:from (.getPath from) :to (.getPath to)})
       (result/error :rename-failed {:from (.getPath from) :to (.getPath to)})))))
