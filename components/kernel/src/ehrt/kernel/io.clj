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

;; ---- directory creation and deletion (ADR-0157, review-4 row D4-1) ----
;;
;; `R-io-result-or-loud`'s lint forbade `.listFiles`/`.list`/`.renameTo`
;; and missed `.mkdirs`/`.delete` -- a gate whose population was
;; narrower than the rule it enforces, which is review 4's own
;; cross-dimension pattern. Thirteen production `.mkdirs` sites
;; discarded the boolean, one of them inside this very component, plus
;; two `.delete`.
;;
;; These two are LOUD rather than result-returning, deliberately, and
;; that is a departure from `list-files`/`rename!` above. Those three
;; sit in code that already threads results; the `.mkdirs` sites do not
;; -- they are single statements inside a `let` or a `->`, and handing
;; each one a result to check would have been thirteen new branches
;; written by a session whose fence was routing. A throw keeps each site
;; one expression and still makes the failure impossible to ignore,
;; which is what "result OR loud" names.

(defn mkdirs!
  "Ensures `dir` exists as a directory, creating parents as needed, and
  returns it as a `java.io.File`.

  `.mkdirs` returning false is AMBIGUOUS in a way `.listFiles`'s nil is
  not: it means either 'the directory was already there' (fine, and the
  common case) or 'creation failed' (not fine). That ambiguity is why
  every call site discarded it, and why a silent false is worse here
  than elsewhere -- the next line writes into the directory and the
  reader gets a `FileNotFoundException` naming a path nobody chose,
  instead of a failure naming the directory that could not be made
  (`rulings.md#R-errors-name-artifact`'s own subject).

  So the postcondition, not the boolean, is the contract: AFTERWARDS THE
  PATH IS A DIRECTORY. Throws ex-info :mkdirs-failed naming the path
  when it is not -- a permission-refused creation, or a parent that is
  itself a file."
  [dir]
  (let [f (io/file dir)]
    (if (or (.mkdirs f) (.isDirectory f))
      f
      (throw (ex-info (str "could not create directory: " (.getPath f))
                      {:error :mkdirs-failed :path (.getPath f)})))))

(defn delete!
  "Deletes `path`, loudly, and returns it as a `java.io.File`.

  `.delete` returning false is ambiguous the same way: the file was
  never there (harmless) or the deletion was refused (not harmless).
  The contract is again the postcondition -- AFTERWARDS THE PATH DOES
  NOT EXIST -- which makes deleting a missing file a SUCCESS, since the
  caller's intent is already satisfied. Throws ex-info :delete-failed
  naming the path when the path survives: a permission-refused
  deletion, or a non-empty directory, which `.delete` always refuses."
  [path]
  (let [f (io/file path)]
    (.delete f)
    (if (.exists f)
      (throw (ex-info (str "could not delete: " (.getPath f))
                      {:error :delete-failed :path (.getPath f)}))
      f)))

(defn delete-quietly!
  "Deletes `path` on a best-effort basis, never throwing. Returns true
  when the path does not exist afterwards, false when it survives.

  This is the DECLARED exception to `delete!`, and it exists so that the
  exception is declared rather than inferred from a bare `.delete`. Use
  it only where the deletion is cleanup after a failure that has
  ALREADY been diagnosed, and where throwing would replace that
  diagnosis with a worse one -- `ehrt.kernel.artifact/fetch`'s two
  sites are the whole population at the time of writing: one removes an
  unverified download before returning result/rejected :hash-mismatch
  with the expected and actual digests, the other removes it from
  inside a `catch` before returning result/error :download-failed with
  the original exception's own message. A throw from either would
  discard the answer the caller actually needs in order to report a
  cleanup problem the caller cannot act on.

  Anywhere else, `delete!` is the right function. A site that reaches
  for this one because a failure would be inconvenient is the silent
  discard `R-io-result-or-loud` exists to stop."
  [path]
  (let [f (io/file path)]
    (.delete f)
    (not (.exists f))))
