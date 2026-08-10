(ns ehrt.docs-tooling.link-footnote-gate-test
  "ADR-0101 (ADR references in user-facing documentation become
  footnotes): the gate co-landed with that conversion, scoped to
  docs/ proper -- docs/dev/ excluded, matching the roadmap row's own
  scope (a dev-docs expansion is named as a future want, not built
  here). Mirrors ehrt.docs-tooling.stale-path-test's own file-listing
  shape (file-seq over docs/, filtering by .md extension), narrowed by
  one more filter to drop docs/dev/.

  ADR-0102 hardened this gate with a third check: the footnote form
  itself is now marker-only (no visible ADR-NNNN token left in prose),
  full user path, origin-qualified citations included -- see that ADR
  for the conversion and the red witness this check produced against
  the pre-conversion tree.

  Three independent checks:

  1. Every relative markdown link `](...)` resolves to a real file on
     disk, anchors stripped before resolution, http(s)/mailto: links
     skipped (external, nothing this repo can check), resolution
     relative to the LINKING file's own directory (not the repo
     root) -- the same resolution rule the doctrine's own link-audit
     script used (notes/adr/0010-documentation-doctrine.md).
  2. Every `[^id]` footnote marker used at a citation site has a
     matching `[^id]:` definition somewhere in the same file, and
     every definition is actually used by at least one marker --
     footnote definitions are identified by the anchored, start-of-
     line `[^id]:` form; usage markers are counted only on content
     with definition lines stripped out first, so a definition line's
     own `[^id]` substring is never miscounted as a second usage.
  3. No visible `ADR-NNNN` token (bare or origin-qualified -- the
     token match doesn't care which) survives in prose: fenced code
     blocks (triple-backtick, any info string) and footnote-
     definition lines are exempted first, then the remainder is
     scanned. A token surviving in a fenced code-comment is the one
     disclosed, intentional exception (ADR-0101's own finding:
     footnote markup cannot render inside a fence) -- exempting the
     whole fence, not just the token, is what keeps that exception
     from tripping this check."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]))

(defn- docs-proper-files
  "docs/**/*.md, docs/dev/ excluded -- narrower than stale-path-test's
  own scan (which covers all of docs/), matching this gate's own
  row scope."
  []
  (->> (file-seq (io/file "docs"))
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".md"))
       (map #(.getPath %))
       (remove #(str/starts-with? % "docs/dev/"))))

(def ^:private link-re
  "Matches a markdown link destination: `](dest)` or `](dest \"title\")`."
  #"\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")

(defn- external-link?
  [dest]
  (or (str/starts-with? dest "http://")
      (str/starts-with? dest "https://")
      (str/starts-with? dest "mailto:")))

(defn- strip-anchor
  [dest]
  (first (str/split dest #"#" 2)))

(defn- broken-links
  "Every link destination in `content` (the file at `path`) that,
  after stripping any #anchor and skipping external links, does not
  resolve to a real file relative to path's own directory. A pure
  #anchor link (same-file section link) strips to an empty path,
  which always resolves (the file itself exists) -- not a broken
  link."
  [path content]
  (let [dir (.getParentFile (io/file path))]
    (->> (re-seq link-re content)
         (map second)
         (remove external-link?)
         (map strip-anchor)
         (remove str/blank?)
         (remove #(.exists (io/file dir %)))
         distinct)))

(def ^:private footnote-def-re
  "A footnote definition: anchored at the start of its own line, per
  the [C] shape this ADR's conversion follows (one definition line per
  ADR, grouped at each file's bottom)."
  #"(?m)^\[\^([A-Za-z0-9-]+)\]:")

(def ^:private footnote-marker-re
  #"\[\^([A-Za-z0-9-]+)\]")

(defn- strip-definition-lines
  [content]
  (str/replace content #"(?m)^\[\^[A-Za-z0-9-]+\]:.*$" ""))

(defn- footnote-def-ids
  [content]
  (set (map second (re-seq footnote-def-re content))))

(defn- footnote-usage-ids
  [content]
  (set (map second (re-seq footnote-marker-re (strip-definition-lines content)))))

(defn- undefined-markers
  "Usage markers with no matching definition in the same file."
  [content]
  (set/difference (footnote-usage-ids content) (footnote-def-ids content)))

(defn- orphan-definitions
  "Definitions with no usage marker anywhere in the same file."
  [content]
  (set/difference (footnote-def-ids content) (footnote-usage-ids content)))

(def ^:private adr-token-re
  "Any visible ADR-NNNN token, bare or origin-qualified -- the regex
  itself doesn't distinguish `ADR-0010` from `sim/ADR-0010`, since
  ADR-0102's own ruling forbids the token in prose regardless of
  qualification."
  #"ADR-\d{4}")

(def ^:private fenced-code-block-re
  "A ``` ... ``` fenced block, any info string, non-greedy so adjacent
  fences pair with their own nearest close rather than spanning past it."
  #"(?s)```.*?```")

(defn- strip-fenced-code
  [content]
  (str/replace content fenced-code-block-re ""))

(def ^:private footnote-definition-line-re
  #"(?m)^\[\^[A-Za-z0-9-]+\]:.*$")

(defn- strip-footnote-definition-lines
  [content]
  (str/replace content footnote-definition-line-re ""))

(defn- adr-tokens-in-prose
  "Every ADR-NNNN token remaining once fenced code and footnote-
  definition lines are stripped -- what's left is prose, and this
  gate's own ADR-0102 ruling says none of it may still show the token."
  [content]
  (->> content
       strip-fenced-code
       strip-footnote-definition-lines
       (re-seq adr-token-re)
       set))

(deftest every-relative-link-in-docs-proper-resolves-test
  (doseq [path (docs-proper-files)]
    (let [broken (broken-links path (slurp path))]
      (is (empty? broken) (str path " has broken relative link(s): " broken)))))

(deftest every-footnote-marker-has-a-definition-and-vice-versa-test
  (doseq [path (docs-proper-files)]
    (let [content (slurp path)
          undefined (undefined-markers content)
          orphans (orphan-definitions content)]
      (is (empty? undefined) (str path " has footnote marker(s) with no definition: " undefined))
      (is (empty? orphans) (str path " has footnote definition(s) with no usage: " orphans)))))

(deftest no-visible-adr-token-in-prose-test
  (doseq [path (docs-proper-files)]
    (let [tokens (adr-tokens-in-prose (slurp path))]
      (is (empty? tokens) (str path " has visible ADR-NNNN token(s) in prose: " tokens)))))
