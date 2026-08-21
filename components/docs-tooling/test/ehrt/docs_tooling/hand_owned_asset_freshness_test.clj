(ns ehrt.docs-tooling.hand-owned-asset-freshness-test
  "ADR-0158, review-4 register row D5-2, author ruling R4-Q5 (b).

  Six derived surfaces each name a specific live source and the trigger
  that invalidates them, and until this gate nothing watched any of
  them: `git grep -l 'manual/assets' -- components bases` returned
  nothing, none was on a freshness list, none had a make target. Five
  are `docs/manual/assets/*.svg`; the sixth, an embedded mermaid block
  in `components/patient-simulator/docs/trajectory-computation.md`, is
  ruled deliberately hand-owned under R4-Q5 (d) and carries a dated
  line saying so instead of a row here.

  This is `sim-theory`'s own lesson (ADR-0152: a freshness gate over a
  chain that EXCLUDES the chain's source proves only that the middle
  agrees with itself) applied where no translator can exist. It cannot
  regenerate anything; it can only refuse to let a source move under a
  drawing in silence.

  WHAT IT TRUSTS, stated plainly. The tripwire compares whole FILES
  while the banners' own triggers name SECTIONS ('section 4's own
  equations', 'the cited section's own field audit'). It is therefore
  deliberately over-sensitive: an unrelated edit anywhere in a cited
  source fires it. Step 0 of ADR-0158's own session measured exactly
  that -- four of five rows red, of which THREE were unrelated edits (a
  link-path fix, a scenario rename, an addendum to a different section)
  and ONE was a true stale asset. A section-level tripwire would have
  been quieter and would also have needed a section-extraction parser
  per banner, each its own silent-mismatch risk. The coarse version
  plus a recorded human review is the honest trade, and
  `hand-owned-assets.edn`'s `:reviewed-at` is where that review lands.

  Five claims:

  (a) POPULATION CLOSURE -- every `docs/manual/assets/*.svg` in the tree
      has a row, and every row names an asset that exists. Both
      populations non-empty (`rulings.md#R-empty-population-is-red`).

  (b) EVERY ROW'S SOURCE EXISTS -- a row citing a path that has been
      renamed away is a dead tripwire, and `git log` on a nonexistent
      path returns empty rather than failing, which would read as
      'unchanged' forever.

  (c) THE BANNER AND THE ROW AGREE -- each asset's own SVG comment
      banner names the `:source` this row claims for it. Without this,
      the registry could drift from the artifact it is supposed to
      watch, which is register row L3-5's defect one level up.

  (d) THE TRIPWIRE ITSELF -- for every `:verdict :fresh` row, the
      source's own last-change commit still starts with `:reviewed-at`.

  (e) A :stale ROW IS A ROWED FINDING -- it must name a `roadmap.md`
      anchor, and that anchor must still be present in `roadmap.md`.
      This is what keeps `:stale` from being a silencer: retiring the
      finding means deleting the roadmap row, which turns this red."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def ^:private registry-path
  "components/docs-tooling/resources/docs-tooling/hand-owned-assets.edn")

(def ^:private asset-dir "docs/manual/assets")
(def ^:private roadmap-path ".agents/plans/roadmap.md")

(defn- rows [] (edn/read-string (slurp registry-path)))

(defn- assets-in-tree []
  (->> (.listFiles (io/file asset-dir))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".svg"))
       (map #(str asset-dir "/" (.getName ^java.io.File %)))
       set))

(defn- last-change-sha
  "The commit that last touched `path`, or nil. Loud on a git failure:
  an empty string from a broken `git log` would read as 'no commits'
  and, compared against a `:reviewed-at`, as a difference -- red for the
  wrong reason -- while a nil here fails its own assertion by name."
  [path]
  (let [{:keys [exit out]} (shell/sh "git" "log" "-1" "--format=%H" "--" path)]
    (when (zero? exit)
      (let [s (str/trim out)]
        (when (seq s) s)))))

(defn- banner
  "The first XML comment block in an SVG -- where every one of these
  assets carries its own derivation note."
  [path]
  (let [content (slurp path)]
    (or (second (re-find #"(?s)<!--(.*?)-->\s*<svg" content)) "")))

(deftest every-manual-svg-asset-has-a-tripwire-row-test
  (testing "(a) population closure, both directions, both non-empty"
    (let [tree (assets-in-tree)
          registered (set (map :asset (rows)))]
      (is (seq tree) "no SVG assets found under docs/manual/assets -- the glob matched nothing")
      (is (seq registered) "hand-owned-assets.edn holds no rows")
      (is (= tree registered)
          (str "the tree and the registry disagree.\n"
               "  in the tree but un-tripwired: " (pr-str (sort (remove registered tree))) "\n"
               "  in the registry but absent from the tree: "
               (pr-str (sort (remove tree registered))))))))

(deftest every-tripwire-row-names-live-files-test
  (testing "(b) both the asset and its cited source exist on disk"
    (doseq [{:keys [asset source]} (rows)]
      (testing asset
        (is (.exists (io/file asset)) (str asset " does not exist"))
        (is (.exists (io/file source))
            (str asset " cites source " source ", which does not exist -- a renamed "
                 "source leaves a tripwire that can never fire"))))))

(deftest every-asset-banner-names-the-source-its-row-claims-test
  (testing "(c) the drawing and the registry agree about what feeds it"
    (doseq [{:keys [asset source]} (rows)]
      (testing asset
        (is (str/includes? (banner asset) source)
            (str asset "'s own banner does not name " source ", which its registry row "
                 "claims as its source. Either the banner or the row is wrong; a "
                 "tripwire watching a file the artifact never cites watches nothing."))))))

(deftest no-cited-source-has-moved-since-its-asset-was-last-reviewed-test
  (testing "(d) the tripwire"
    (doseq [{:keys [asset source reviewed-at verdict note]} (rows)
            :when (= :fresh verdict)]
      (testing asset
        (let [sha (last-change-sha source)]
          (is (some? sha) (str "git log returned nothing for " source))
          (is (and sha (str/starts-with? sha reviewed-at))
              (str "STALE-ASSET TRIPWIRE: " source " last changed at "
                   (some-> sha (subs 0 8)) ", but " asset " was last reviewed against "
                   reviewed-at ".\n"
                   "  Trigger: " (:trigger (first (filter #(= asset (:asset %)) (rows)))) "\n"
                   (when note (str "  Last review note: " note "\n"))
                   "  Open `git diff " reviewed-at ".." (some-> sha (subs 0 8)) " -- " source
                   "` and decide whether the trigger fired.\n"
                   "  If it did NOT: bump :reviewed-at and add a :note saying what moved.\n"
                   "  If it DID: the asset is stale -- open a roadmap row and set\n"
                   "             :verdict :stale with :stale-row naming that anchor.")))))))

(deftest every-stale-row-names-a-live-roadmap-anchor-test
  (testing "(e) a :stale verdict is a rowed finding, never a silencer"
    (let [roadmap (slurp roadmap-path)]
      (doseq [{:keys [asset verdict stale-row]} (rows)
              :when (= :stale verdict)]
        (testing asset
          (is (seq stale-row)
              (str asset " is marked :verdict :stale with no :stale-row. A stale asset "
                   "is a finding someone must action, so it owes a roadmap anchor."))
          (let [anchor (str/replace (or stale-row "") #"^roadmap\.md#" "")]
            (is (str/includes? roadmap (str "**[" anchor "]**"))
                (str asset " cites " stale-row ", which is not a row in " roadmap-path
                     " -- the finding has been retired while the asset is still stale."))))))))
