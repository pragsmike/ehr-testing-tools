(ns ehrt.docs-tooling.invocation-lint-test
  "AR-U1-2 (ux fixes 1, `notes/ADRs.md` ADR-0059): the stale `clojure
  -M:cli` invocation alias -- swept from every live doc surface this
  session (register row U1, `.agents/plans/2026-08-06-ux-audit-
  findings.md`) -- is forbidden outright on the same surface set the
  sweep covered, so a future demo doesn't silently reintroduce it.
  `bin/ehrt` is the taught, cwd-safe entry point; `clojure -M:cli` is a
  dead alias, not a documented alternate (the alternate that IS
  documented, bare `clojure -M:ehrt`, is untouched by this gate).

  Scoped exactly as the sweep was: README.md, AUTHORS-GUIDE.md, every
  .md and .edn file under docs/**, and every .md and .edn file under
  components/*/docs/**. Frozen archives, `.agents/prompts/`,
  `.agents/session-records/`, dated one-shot plan files, and the audit
  registers are out of scope by construction -- none of them live under
  any of the four scanned roots, same scoping discipline
  `ehrt.docs-tooling.stale-path-test`'s own family uses throughout."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private forbidden-invocation "clojure -M:cli")

(defn- doc-like-files [^java.io.File root]
  (->> (file-seq root)
       (filter #(.isFile ^java.io.File %))
       (filter #(or (str/ends-with? (.getName ^java.io.File %) ".md")
                     (str/ends-with? (.getName ^java.io.File %) ".edn")))
       (map #(.getPath ^java.io.File %))))

(defn- component-docs-roots []
  (->> (.listFiles (io/file "components"))
       (filter #(.isDirectory ^java.io.File %))
       (map #(io/file % "docs"))
       (filter #(.exists ^java.io.File %))))

(defn- scan-sources []
  (concat ["README.md" "AUTHORS-GUIDE.md"]
          (doc-like-files (io/file "docs"))
          (mapcat doc-like-files (component-docs-roots))))

(deftest no-stale-cli-alias-invocation-anywhere-in-live-docs-test
  (doseq [path (scan-sources)]
    (let [content (slurp path)]
      (is (not (str/includes? content forbidden-invocation))
          (str path " teaches the stale `clojure -M:cli` invocation -- "
               "bin/ehrt is the live entry point (register row U1, AR-U1-2)")))))

(deftest forbidden-invocation-pattern-is-actually-caught-test
  (is (str/includes? "run it with clojure -M:cli run --seed 1" forbidden-invocation))
  (is (not (str/includes? "run it with bin/ehrt sim run --seed 1" forbidden-invocation))))
