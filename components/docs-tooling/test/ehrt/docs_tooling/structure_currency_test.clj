(ns ehrt.docs-tooling.structure-currency-test
  "P1-3 (2026-07-31 review catch-up, finding 2): AGENTS.md's own
  'Landed so far' section and docs/dev/architecture.md's mermaid
  diagram/bricks table are both promised to stay current with the
  workspace's real brick set, but nothing made that mechanical --
  AGENTS.md drifted (still called judge-v2-nist a named future
  addition, EXP-D3, after ADR-0012 landed it). This test makes 'kept
  current' a per-push check instead of an aspiration.

  Hardened (gate-hardening session, 2026-07-31, notes/ADRs.md
  ADR-0018 named-futures 3): the original version checked presence
  only, by raw substring -- so `corpus` passed trivially as a
  substring of `corpus-io` even before either doc named the new
  brick (ADR-0018's own AR-6, 'the structure-currency moment: honestly,
  it never went red'). Two independent checks now, both exact-token:

  Presence -- every real brick's exact backtick-delimited path token
  (`` `components/x` `` / `` `bases/x` ``) appears somewhere in each
  surface. The backtick delimiters make this substring-immune:
  `` `components/corpus` `` is not a substring of
  `` `components/corpus-io` ``.

  Absence -- every brick named as the FIRST cell of a structure-table
  row (the literal shape a docs/dev/architecture.md bricks-table row
  takes) must exist on disk. This catches a retired brick's row with
  no denylist to maintain (`tools`, ADR-0018). Deliberately
  line-anchored and scoped to table rows, not scanned over full-file
  prose: both AGENTS.md's 'Landed so far' section and
  architecture.md's own bricks-table description cells legitimately
  name retired bricks as history (`Extracted from `components/tools``,
  three-stage split narrative) -- a token appearing mid-sentence, not
  as a row's own identity cell, is never mistaken for a live row.
  Filesystem enumeration, not a `poly ws` shell-out, to keep this in
  the fast per-push lane."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn- disk-bricks
  "Every real {:kind :name} pair on disk right now, kind being
  \"components\" or \"bases\" -- the source of truth both directions
  check against."
  []
  (letfn [(names [dir kind]
            (->> (.listFiles (io/file dir))
                 (filter #(.isDirectory %))
                 (map (fn [f] {:kind kind :name (.getName f)}))))]
    (concat (names "components" "components") (names "bases" "bases"))))

(defn- path-token
  "The exact backtick-delimited path token a doc uses to name a brick,
  e.g. \"`components/corpus`\" -- never a bare name, so it can't be
  satisfied by another brick's name containing it as a substring."
  [{:keys [kind name]}]
  (str "`" kind "/" name "`"))

(defn- structure-table-row-bricks
  "Every {:kind :name} named as the FIRST cell of a markdown table row
  shaped like \"| `components/x` | ... |\" or \"| `bases/x` | ... |\"
  in `text`. Line-anchored on purpose: a row's OWN later cells (its
  prose description) may legitimately mention a different, retired
  brick by name -- architecture.md's kernel and corpus rows both cite
  `components/tools` as extraction history inside their 'What it is'
  column -- and matching only the line's leading cell never confuses
  that history with a live row naming the retired brick."
  [text]
  (->> (str/split-lines text)
       (keep (fn [line]
               (when-let [[_ kind name] (re-matches #"\|\s*`(components|bases)/([a-zA-Z0-9-]+)`\s*\|.*" line)]
                 {:kind kind :name name})))
       distinct))

(deftest every-real-brick-is-named-in-agents-and-architecture-test
  (testing "presence: every real brick's exact path token appears in both surfaces"
    (let [agents (slurp "AGENTS.md")
          architecture (slurp "docs/dev/architecture.md")]
      (doseq [brick (disk-bricks)]
        (let [tok (path-token brick)]
          (is (str/includes? agents tok)
              (str tok " (components/ or bases/) is missing from AGENTS.md's own structure prose"))
          (is (str/includes? architecture tok)
              (str tok " (components/ or bases/) is missing from docs/dev/architecture.md's bricks table")))))))

(deftest every-structure-table-row-names-a-real-brick-test
  (testing "absence: a structure-table row can't name a retired or nonexistent brick"
    (let [agents (slurp "AGENTS.md")
          architecture (slurp "docs/dev/architecture.md")
          on-disk (set (map (juxt :kind :name) (disk-bricks)))
          named (distinct (concat (structure-table-row-bricks agents)
                                   (structure-table-row-bricks architecture)))]
      (doseq [{:keys [kind name]} named]
        (is (contains? on-disk [kind name])
            (str "`" kind "/" name "` is named as a structure-table row but "
                 kind "/" name " does not exist on disk"))))))
