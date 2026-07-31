(ns ehrt.docs-tooling.structure-currency-test
  "P1-3 (2026-07-31 review catch-up, finding 2): AGENTS.md's own
  'Landed so far' section and docs/dev/architecture.md's mermaid
  diagram/bricks table are both promised to stay current with the
  workspace's real brick set, but nothing made that mechanical --
  AGENTS.md drifted (still called judge-v2-nist a named future
  addition, EXP-D3, after ADR-0012 landed it). This test makes 'kept
  current' a per-push check instead of an aspiration: every directory
  name under components/ and bases/ must appear verbatim in both
  files. Filesystem enumeration, not a `poly ws` shell-out, to keep
  this in the fast per-push lane."
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn- brick-names [dir]
  (->> (.listFiles (io/file dir))
       (filter #(.isDirectory %))
       (map #(.getName %))
       sort))

(deftest every-component-and-base-is-named-in-agents-and-architecture-test
  (let [agents (slurp "AGENTS.md")
        architecture (slurp "docs/dev/architecture.md")
        bricks (concat (brick-names "components") (brick-names "bases"))]
    (doseq [brick bricks]
      (is (str/includes? agents brick)
          (str brick " (components/ or bases/) is missing from AGENTS.md's own structure prose"))
      (is (str/includes? architecture brick)
          (str brick " (components/ or bases/) is missing from docs/dev/architecture.md's bricks table")))))
