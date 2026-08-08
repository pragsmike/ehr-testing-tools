(ns ehrt.kernel.interface
  "The foundation layer shared by judge and corpus (ADR-0002 R14, named
  hole H4; ADR-0008 closes it). Delegation surface sized by grep of
  actual external callers -- judge, the corpus domain's own
  corpus/check/lint, and the CLI (directly, since stage 3 retired the
  tools facade, ADR-0018) -- not a copy of
  each source namespace's full public API. A function private to
  `ehrt.kernel.artifact`/`.canonical`/`.invocation` with no caller
  outside this component stays unexported; see ADR-0008's own census
  table for the full accounting.

  `resolve-artifact` (not `resolve`, `ehrt.kernel.artifact/resolve`'s
  own name) avoids shadowing `clojure.core/resolve` the same way
  the pre-extraction tools facade's `resolve-artifact` already did
  (ADR-0002) -- same discipline, carried forward, not
  reinvented. `run-invocation!` (not `run!`, `ehrt.kernel.invocation/run!`'s
  own name) avoids shadowing `clojure.core/run!` the same way -- caught
  by this extraction's own verification run (a WARNING on every
  namespace load, same class of finding ADR-0002's own `resolve!`
  collision was), not by static review; borrows corpus.generate's own
  `:run-invocation` injection-seam name rather than inventing a new
  one."
  (:require [ehrt.kernel.result :as result]
            [ehrt.kernel.digest :as digest]
            [ehrt.kernel.artifact :as artifact]
            [ehrt.kernel.canonical :as canonical]
            [ehrt.kernel.locator :as locator]
            [ehrt.kernel.invocation :as invocation]
            [ehrt.kernel.io :as kernel-io]))

;; result
(def ok result/ok)
(def ok? result/ok?)
(def rejected result/rejected)
(def rejected? result/rejected?)
(def error result/error)
(def error? result/error?)
(def valid? result/valid?)

;; digest
(def sha256-file digest/sha256-file)
(def sha256-string digest/sha256-string)
(def sha256-bytes digest/sha256-bytes)

;; artifact
(def fetch artifact/fetch)
(def read-lockfile artifact/read-lockfile)
(def resolve-artifact artifact/resolve)
(def resolve-and-extract artifact/resolve-and-extract)
(def find-executable artifact/find-executable)

;; canonical
(def register! canonical/register!)
(def lookup canonical/lookup)
(def apply-canonicalizers canonical/apply-canonicalizers)

;; locator
(def Locator locator/Locator)
(def make locator/make)
(def fhir-data-path locator/fhir-data-path)
(def v2-data-path locator/v2-data-path)

;; invocation
(def run-invocation! invocation/run!)

;; io (result or loud, ADR-0078)
(def list-files kernel-io/list-files)
(def existing-dir-nonempty? kernel-io/existing-dir-nonempty?)
(def rename! kernel-io/rename!)
