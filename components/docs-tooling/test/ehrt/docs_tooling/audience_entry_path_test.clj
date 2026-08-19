(ns ehrt.docs-tooling.audience-entry-path-test
  "ADR-0156, closing register row D2-1 -- the gate
  `rulings.md#R-audience-has-entry-path` never had.

  ADR-0146 landed the rule *and* the sixth segment it exists to protect,
  and nothing enforced it: no test named `docs/dev/AUDIENCES.md` except
  `stale_path_test`, which resolves paths rather than checking that a
  segment HAS one. The register is hand-maintained, its header count has
  drifted before, and `docs/README.md` routes off it rather than
  defining its own paths -- so a segment without an entry path is, in
  the rule's own words, \"a routing gap everywhere that register is
  keyed off\".

  THE POPULATION IS THE NUMBERED SEGMENTS under `## Audience`,
  enumerated from the file rather than from the header's own count (the
  header has read \"Seven\" while the file held four; that is D1-5).

  AUTHOR RULING, 2026-08-19 (ADR-0156 Step 0 (f)): the law is
  UNIVERSAL -- every numbered segment carries at least one markdown
  link, with no exemption for explicitly-deferred segments. The
  alternative on offer was a declared `deferred stub` marker the gate
  would skip. Two things decided it: an exemption would have needed a
  marker vocabulary the rule's own text does not carry, and the
  re-derivation found TWO linkless segments, not the one the plan
  anticipated -- segment 5 (the deferred stub) and segment 1 (\"Guide
  readers, arriving method-first\"), which is not deferred and which no
  exemption would have covered.

  WHAT IS OUT OF SCOPE, and why. `docs/what-is-this.md` has its own
  `## Audience` -- a BULLETED list of seven, zero of them carrying a
  link. It is deliberately not gated here. The law's own text is scoped
  to `docs/dev/AUDIENCES.md`, and the two lists do different jobs: this
  one is the routing register `docs/README.md` keys off, that one
  describes who the software is for. Making the public list carry entry
  paths is a defensible thing to want, but it is a NEW rule and belongs
  to whoever rules it, not smuggled in as this gate's second
  population."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def ^:private audiences-path "docs/dev/AUDIENCES.md")

(def ^:private markdown-link #"\[[^\]]+\]\([^)]+\)")

(defn- audience-section
  "The lines under `## Audience`, up to the next `## ` heading."
  [text]
  (let [lines (str/split-lines text)
        start (first (keep-indexed #(when (= "## Audience" %2) %1) lines))
        _ (assert start "AUDIENCES.md must have a `## Audience` heading")
        rest-lines (drop (inc start) lines)
        body (take-while #(not (str/starts-with? % "## ")) rest-lines)]
    (vec body)))

(defn- numbered-segments
  "One entry per `N. **...**` item: its number, its bold lead, and every
  line of its body up to the next numbered item."
  [section]
  (let [starts (keep-indexed (fn [i l] (when (re-find #"^\d+\. \*\*" l) i)) section)
        bounds (partition 2 1 (concat starts [(count section)]))]
    (for [[from to] bounds
          :let [lines (subvec section from to)]]
      {:number (Integer/parseInt (re-find #"^\d+" (first lines)))
       :lead (str/trim (or (second (re-find #"^\d+\. \*\*(.+?)\*\*" (first lines)))
                           (first lines)))
       :text (str/join "\n" lines)})))

(deftest every-numbered-audience-segment-carries-an-entry-path-test
  (let [section (audience-section (slurp audiences-path))
        segments (numbered-segments section)]
    (testing "the population is real and enumerated from the file -- R-empty-population-is-red"
      (is (seq segments)
          (str "no numbered segments found under `## Audience` in " audiences-path
               " -- a link assertion over zero segments is a pass that proves nothing, which "
               "is exactly the class `rulings.md#R-empty-population-is-red` names"))
      (is (= 6 (count segments))
          (str "six segments today (ADR-0119 pared to five, ADR-0146 grew it back to six with "
               "the emitter author). Found " (count segments) ": "
               (pr-str (map :number segments))
               ". If this moved deliberately, move it here in the same commit."))
      (is (= (range 1 (inc (count segments))) (map :number segments))
          (str "segment numbers must run 1..N with no gap or repeat. Found "
               (pr-str (map :number segments)))))
    (testing "rulings.md#R-audience-has-entry-path, gated at last"
      (doseq [{:keys [number lead text]} segments]
        (is (re-find markdown-link text)
            (str "segment " number " (\"" lead "\") carries no markdown link. Every segment "
                 "states its own entry path: `docs/README.md` says explicitly that it routes "
                 "off this register rather than defining its own paths, so a segment without "
                 "one is a routing gap everywhere that register is keyed off (ADR-0146). "
                 "The law is universal by author ruling 2026-08-19 -- a deferred segment "
                 "points at the nearest real path (its gate section, its docstring entry "
                 "point), it is not exempt."))))))

(deftest the-audience-register-is-the-one-docs-readme-routes-off-test
  (testing "the premise the gate rests on is still true"
    (let [readme (slurp (io/file "docs/README.md"))]
      (is (str/includes? readme "AUDIENCES.md")
          (str "`docs/README.md` must still route off `" audiences-path "`. If it stops doing "
               "so, this gate is protecting a register nothing keys off, and D2-1's whole "
               "argument needs re-deriving rather than this test quietly staying green.")))))
