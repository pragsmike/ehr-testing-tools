(ns ehrt.docs-tooling.index-completeness-test
  "Item 10 (migration session 3, 2026-08-02): indexes stop being prose
  and start being checked. Before this test, `.agents/prompts/README.md`
  and `.agents/session-records/README.md` (etc.) listed their own
  directory's contents by hand, verified accurate only by whoever last
  looked -- nothing failed the build if a session added a file and
  forgot the index line, or removed one and left a ghost entry behind.
  Same exact-token-both-directions shape this repo's own `1c3d77c`
  commit hardened two other gates into: presence (every real item is
  indexed) and absence (every indexed item is real) -- a one-sided
  check would pass an index that's stale in either direction.

  Two README formats in live use here, both already established by
  prior sessions rather than invented for this test (`AR-1`'s own
  'format adjusted, not the gate weakened' license was not needed --
  both formats already extract cleanly):

  - **Star-bullet** (`.agents/plans/`, `.agents/prompts/`,
    `.agents/session-records/`): a `## ... list`/`## Records` section
    with `  * filename.md[ — description]` lines, one file per line,
    no backticks.
  - **Backtick-bullet** (`notes/README.md`, `.agents/skills/README.md`):
    `- **[`token`](...)**` or `- **`token`**` lines, one or more
    backtick-wrapped tokens per line (a file `token.md` or a
    subdirectory `token/`). Scanning is line-anchored to `- **`-leading
    lines specifically so an unrelated backtick token elsewhere in the
    file's own prose (e.g. `notes/README.md`'s own discussion of
    `.claude/skills/`'s mirror, several paragraphs below any bullet)
    is never mistaken for an index entry.

  **Convention-exempt** (`notes/prompts/`, per AR-1's own license for a
  directory where listing every file would break the one-screen
  budget): no per-file list at all -- the README states the naming
  convention (`YYYY-MM-DD-<slug>.md`) and this test enforces that every
  real file matches it, in lieu of a literal list. No ghost-entry
  direction applies here (there is no list to have ghosts in) --
  documented, not silently skipped.

  **Exempt entirely** (ruling 6, extended to completeness this session
  -- design-channel ruling ratified by dispatch, `.agents/plans/2026-08-01-migration-report.md`
  RULED 2026-08-01 item 6, extension recorded in `notes/ADRs.md`
  ADR-0023's own dated-note thread): `notes/sim/` and `notes/tools/`,
  frozen provenance, already exempt from even having a README
  (`ehrt.docs-tooling.readme-presence-test`) -- extending that
  exemption to completeness is the same directory, the same
  rationale, not a new one. `.claude/skills/` is not walked at all:
  it is indexed via `.agents/skills/README.md` already (the same 10
  names), and `ehrt.docs-tooling.skill-mirror-currency-test` is the
  mirror's own drift gate -- indexing the mirror's contents a second
  time here would add nothing."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.java.io :as io]))

;; -- shared filesystem helpers --

(defn- real-files
  "Every regular file directly under `dir`, excluding the two files
  that index rather than are indexed -- `README.md` (the convention
  prose) and `INDEX.md` (the generated listing, ADR-0147). Neither is
  an item, so neither owes itself an entry."
  [dir]
  (->> (.listFiles (io/file dir))
       (filter #(.isFile %))
       (map #(.getName %))
       (remove #{"README.md" "INDEX.md"})
       set))

(defn- real-subdirs
  "Every direct subdirectory of `dir`, as a `name/`-suffixed token."
  [dir]
  (->> (.listFiles (io/file dir))
       (filter #(.isDirectory %))
       (map #(str (.getName %) "/"))
       set))

;; -- star-bullet format (.agents/plans, .agents/prompts, .agents/session-records) --

(defn- star-bullet-token
  "The `filename.md` named by a `  * filename.md[ — description]` line,
  or nil if `line` isn't one."
  [line]
  (second (re-matches #"\s*\*\s+(\S+\.md).*" line)))

(defn- star-bullet-index-tokens [content]
  (->> (str/split-lines content)
       (keep star-bullet-token)
       distinct
       set))

;; -- backtick-bullet format (notes/README.md, .agents/skills/README.md) --

(defn- backtick-bullet-line? [line]
  (boolean (re-find #"^\s*-\s+\*\*" line)))

(defn- backtick-tokens-in-line [line]
  (map second (re-seq #"`([\w.\-]+/?)`" line)))

(defn- backtick-bullet-index-tokens [content]
  (->> (str/split-lines content)
       (filter backtick-bullet-line?)
       (mapcat backtick-tokens-in-line)
       distinct
       set))

;; -- directories using an explicit token index (both directions checked) --

(def ^:private indexed-directories
  "Each entry: `:dir`, the README to scan (`:readme`, defaults to
  `<dir>/README.md`), `:real` (the real item set to check against),
  and `:tokens` (how to extract indexed tokens from the README's own
  content)."
  [{:dir ".agents/plans"
    :real (real-files ".agents/plans")
    :tokens star-bullet-index-tokens}
   {:dir ".agents/prompts"
    :index "INDEX.md"
    :real (real-files ".agents/prompts")
    :tokens star-bullet-index-tokens}
   {:dir ".agents/session-records"
    :index "INDEX.md"
    :real (real-files ".agents/session-records")
    :tokens star-bullet-index-tokens}
   {:dir "notes"
    :real (into (real-files "notes") (real-subdirs "notes"))
    :tokens backtick-bullet-index-tokens}
   {:dir ".agents/skills"
    :real (real-subdirs ".agents/skills")
    :tokens backtick-bullet-index-tokens}])

(deftest every-real-item-is-indexed-test
  (testing "presence: every real file/subdir appears in its directory's own index"
    (doseq [{:keys [dir real tokens index] :or {index "README.md"}} indexed-directories]
      (let [content (slurp (io/file dir index))
            indexed (tokens content)
            missing (set/difference real indexed)]
        (is (empty? missing)
            (str dir "/" index " is missing an index entry for: " missing))))))

(deftest every-indexed-item-is-real-test
  (testing "absence: every listed item actually exists on disk (no ghosts)"
    (doseq [{:keys [dir real tokens index] :or {index "README.md"}} indexed-directories]
      (let [content (slurp (io/file dir index))
            indexed (tokens content)
            ghosts (set/difference indexed real)]
        (is (empty? ghosts)
            (str dir "/" index " lists an entry that does not exist: " ghosts))))))

;; -- the two dated record READMEs: convention only, capped (ADR-0147) --
;;
;; Both were 223 and 171 lines, ~30 of convention prose and the rest a
;; per-file row growing by one every session -- in the `:onboarding`
;; set, so every cold session of every task class read all 390 lines to
;; learn a filename convention. The rows are now generated into a
;; sibling `INDEX.md` (`ehrt.docs-tooling.state-derived`), which no
;; reading set carries. What stays is the convention, and it is capped
;; so it cannot silently regrow the half that just left.

(def ^:private convention-only-readmes
  [".agents/session-records/README.md" ".agents/prompts/README.md"])

(def ^:private readme-line-cap 40)

(deftest record-readmes-stay-convention-only-test
  (testing "the per-record rows live in the generated INDEX.md; the README states the convention"
    (doseq [path convention-only-readmes]
      (let [lines (str/split-lines (slurp path))
            rows (filter star-bullet-token lines)]
        (is (<= (count lines) readme-line-cap)
            (str path " is " (count lines) " lines, over its " readme-line-cap "-line cap by "
                 (- (count lines) readme-line-cap) " -- this file states a convention; the listing "
                 "is generated into INDEX.md beside it."))
        (is (empty? rows)
            (str path " carries " (count rows) " per-record row(s) -- those are generated into "
                 "INDEX.md now, and a hand-maintained second copy is exactly the drift this gate "
                 "was written for in the first place: " (vec (take 3 rows))))))))

;; -- convention-exempt directory (notes/prompts/) --

(def ^:private dated-prompt-filename-re #"^\d{4}-\d{2}-\d{2}-.+\.md$")

(deftest notes-prompts-files-match-the-dated-convention-test
  (testing "every real file in notes/prompts/ (except README.md) matches YYYY-MM-DD-<slug>.md"
    (doseq [f (real-files "notes/prompts")]
      (is (re-matches dated-prompt-filename-re f)
          (str "notes/prompts/" f " does not match the dated-file convention this directory's README states in lieu of a literal list"))))
  (testing "the README states the convention (not silently exempt with no trace)"
    (is (str/includes? (slurp "notes/prompts/README.md") "YYYY-MM-DD-<slug>.md")
        "notes/prompts/README.md no longer states its own index-exemption convention")))

;; -- token-extraction sanity (each format actually works, on real content) --

(deftest star-bullet-extraction-is-actually-caught-test
  (is (= #{"roadmap.md" "2026-08-01-agent-ux-charter.md"}
         (star-bullet-index-tokens "## Plan list\n\n  * roadmap.md — the rolling plan\n  * 2026-08-01-agent-ux-charter.md\n"))))

(deftest backtick-bullet-extraction-is-actually-caught-test
  (testing "a single-token linked bullet"
    (is (= #{"ADRs.md"} (backtick-bullet-index-tokens "- **[`ADRs.md`](ADRs.md)** — every decision\n"))))
  (testing "a two-token bullet with no link"
    (is (= #{"sim/" "tools/"} (backtick-bullet-index-tokens "- **`sim/`** and **`tools/`** — frozen provenance\n"))))
  (testing "a backtick token outside a bullet line is not indexed prose, ignored"
    (is (empty? (backtick-bullet-index-tokens "`.claude/skills/` is a real-file mirror of this directory\n")))))
