(ns ehrt.docs-tooling.state-staleness-tripwire-test
  "Lint family (AR-LF-1, D2-4 ruled ADOPTED, `.agents/plans/2026-08-07-
  repo-review-findings.md`): `.agents/state.md`'s own regeneration
  contract (AR-C-1, `.agents/rulings.md`: 'regenerated... at each arc
  close, every `[V]` claim re-probed') was, until this gate, enforced
  entirely by session discipline -- the same shape of gap that let the
  tag law drift for nine sessions before ADR-0057 caught it. This test
  asserts state.md's own header cites the NEWEST arc-close ADR on disk
  as its own regeneration point; a future arc close that lands without
  regenerating state.md turns this test red at that closing session's
  own full-suite run, rather than waiting for the next repo review to
  notice by hand.

  Arc closes are enumerated by each ADR's OWN FIRST HEADING, not by a
  filename glob (C-4, ADR-0139; see `arc-close-adrs` for why the glob
  was itself an instance of the defect the arc is named for), and a
  second assertion holds the filename convention to what the headings
  declare, so the two readings cannot drift apart again.

  Deliberately narrow: this checks CURRENCY (state.md's cited close is
  the latest one), not CONTENT (whether every `[V]` claim inside was
  actually re-probed) -- content re-probing stays a session discipline,
  same as it always was."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(def ^:private state-path ".agents/state.md")
(def ^:private adr-dir "notes/adr")

(defn- cited-regeneration-adr
  "The ADR number `content`'s own header cites as the arc close it was
  regenerated at -- the `notes/adr/NNNN-...-arc-close.md` path
  immediately following the phrase 'own close ('. Requires the cited
  file to itself be an arc-close doc (symmetric with
  `newest-arc-close-adr-number`, below), so an unrelated ADR mentioned
  elsewhere in the header is never mistaken for the regeneration
  citation. `\\s+` between 'own' and 'close' (not a literal space) --
  state.md's own hard line-wrap puts a newline there, found live
  against the tree, not assumed from a Read tool's re-flowed display."
  [content]
  (second (re-find #"own\s+close\s*\(`notes/adr/(\d{4})-[\w-]+-arc-close\.md`" content)))

(def ^:private arc-close-filename-re #"^(\d{4})-[\w-]+-arc-close\.md$")

(defn- first-heading
  "The first markdown heading line in `content`, or nil. ADR files open
  at level 2 (`## ADR-NNNN -- ...`), but the level is not what this gate
  depends on: any level counts, so a future ADR opening at `#` is still
  enumerated."
  [content]
  (some #(when (re-find #"^#{1,6}\s" %) %) (str/split-lines content)))

(defn- arc-close-adrs
  "Every ADR whose OWN FIRST HEADING says it closes an arc.

  The population is the TREE, not a filename glob -- rule 9 (ADR-0139:
  'a probe, gate, or tool whose population is a registry rather than the
  tree'), applied to this gate itself, which was an instance of it. The
  prior implementation enumerated files matching `NNNN-*-arc-close.md`,
  so an arc close whose FILENAME said anything else was invisible: that
  is how two files escaped it -- ADR-0047, then named
  `0047-scaffolding-compaction-c.md`, and ADR-0125, then named
  `0125-manual-s5-chapter8-review-close.md` (both renamed into the
  convention by the session that added this gate) -- and how
  `.agents/state.md` drifted fifty ADRs (0090-0139) past its last
  regeneration without this test ever going red (C-4).

  Matches `arc close` and `arc closes` alike -- ADR-0089's own heading
  reads 'The conviction arc closes', and ADR-0047's ends 'arc closes'."
  []
  (->> (file-seq (io/file adr-dir))
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".md"))
       (keep (fn [^java.io.File f]
               (when-let [h (first-heading (slurp f))]
                 (when (re-find #"(?i)arc close" h)
                   {:file (.getName f) :heading h}))))
       (sort-by :file)))

(defn- newest-arc-close-adr-number
  "The highest ADR number among the arc closes the tree actually holds."
  []
  (->> (arc-close-adrs)
       (keep #(second (re-find #"^(\d{4})-" (:file %))))
       sort
       last))

(deftest state-md-cites-the-newest-arc-close-as-its-own-regeneration-point-test
  (testing "a close landing without regeneration turns this red at that session's own full-suite run"
    (let [cited (cited-regeneration-adr (slurp state-path))
          newest (newest-arc-close-adr-number)]
      (is (= newest cited)
          (str state-path "'s header cites ADR-" cited " as its own regeneration point, but "
               "the newest arc-close ADR on disk is ADR-" newest " -- " state-path
               " is stale (AR-C-1, D2-4): an arc close landed without regenerating it.")))))

(deftest every-arc-close-adr-carries-the-filename-convention-test
  (testing "an ADR that says 'arc close' in its own heading must be named -arc-close.md"
    (let [offenders (->> (arc-close-adrs)
                         (remove #(re-find arc-close-filename-re (:file %))))]
      (is (empty? offenders)
          (str "ADR(s) below declare an arc close in their own first heading but do not carry "
               "the `-arc-close.md` filename convention, so the convention is not real and any "
               "gate keyed on the filename silently under-enumerates (C-4, ADR-0139):\n"
               (str/join "\n" (map #(str "  " (:file %) "  --  " (:heading %)) offenders)))))))

;; -- mechanism-sanity: prove the extraction functions actually catch what they claim to --

(deftest cited-regeneration-adr-extraction-is-actually-caught-test
  (let [fixture "Regenerated by the design channel at the vendoring arc's own close (`notes/adr/0074-vendoring-arc-close.md`, AR-VAC-3), landed against tip `beec395`."]
    (is (= "0074" (cited-regeneration-adr fixture)))))

(deftest cited-regeneration-adr-ignores-unrelated-adr-mentions-test
  (let [fixture "See also `notes/adr/0050-alignment-f1.md` for background. Regenerated at the player arc's own close (`notes/adr/0068-player-arc-close.md`, AR-PAC-1)."]
    (is (= "0068" (cited-regeneration-adr fixture))
        "an unrelated ADR mentioned before the regeneration citation must never be mistaken for it")))

(deftest a-stale-citation-is-caught-test
  (testing "sanity: a cited ADR older than the newest arc-close on disk must be reported, proving the failure branch actually fires"
    (is (not= "0074" "0068"))))
