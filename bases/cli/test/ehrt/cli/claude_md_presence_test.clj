(ns ehrt.cli.claude-md-presence-test
  "P1-4 (2026-07-31 review catch-up, finding 11): AGENTS.md:5-6 promises
  'Claude Code users: see CLAUDE.md, which points here', but no such
  file was ever committed -- flagged by two prior audits (carve-loss,
  discipline-parity M21), closed zero times. A new namespace, not an
  edit to executable-bits-test.clj: that file's own family sits in
  this same directory but the /mnt/c clone carries independent WIP in
  bases/cli this session must not collide with (dual-clone hazard,
  session memory). Checks the git INDEX, the only thing a fresh clone
  inherits (AUTHORS-GUIDE.md sec7a) -- a working-tree-only file would
  pass this test and still not exist for CI or a collaborator."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn- tracked-paths []
  (let [{:keys [exit out err]} (shell/sh "git" "ls-files")]
    (when-not (zero? exit)
      (throw (ex-info "git ls-files failed -- can't verify CLAUDE.md presence" {:exit exit :err err})))
    (set (str/split-lines out))))

(deftest claude-md-is-tracked-at-repo-root-test
  (is (contains? (tracked-paths) "CLAUDE.md")
      "CLAUDE.md must be committed at the repo root -- AGENTS.md:5-6 promises it as the Claude Code on-ramp pointer"))
