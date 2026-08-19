(ns ehrt.docs-tooling.front-door-fence-gate-test
  "ADR-0158, review-4 register rows D8-1/D8-2, author ruling R4-Q4 (a).

  ADR-0140 handed review 4 a PROPOSED default and said only a ruling
  could accept it: *'every fence a reader meets on the README / SETUP /
  manual / use-case path is exercised ... the census can gate
  bare-fence-count-on-reader-path = 0.'* Review 4 measured what the full
  rule costs -- 38 bare fences on the reader path (README 1, SETUP 3,
  manual 21, use-cases 13) -- and R4-Q4 (a) took the tiered option: gate
  the FRONT DOOR at zero now, register the manual's 21 and use-cases' 13
  as their own session (`roadmap.md#reader-path-fence-battery`).

  This is the ratchet. `README.md` and `SETUP.md` are the two files a
  cold reader meets first, and once their command fences are all
  accounted for they cannot regress: a NEW fence on either is `bare` by
  default, and bare is what claim (a) forbids.

  THREE DISPOSITIONS, and why there are three. Measuring the four
  fences this rule costs found that they are not four cheap ones. Two
  teach commands a checker cannot run without lying about what they do
  (`sudo apt install` mutates the machine; a fresh-clone `git clone` is
  not a check of the tree it runs inside), and one teaches a
  deliberately real-time viewing rate whose whole point is the rate. So
  a front-door command fence is `exercised` (a bin/ script re-runs it
  verbatim -- `exercised-sources.edn`), or `exempt` (a row in
  `fence-exemptions.edn` carrying a reason a human wrote), or `bare`.
  Folding the un-runnable ones into `exercised` would overstate what is
  actually re-run; leaving them in `bare` would make the gate
  un-passable, and an un-passable gate gets deleted. Naming them is the
  honest third thing.

  Four claims:

  (a) THE GATE -- zero bare command fences on README.md and SETUP.md.

  (b) THE POPULATION IS NON-EMPTY -- the census finds command fences on
      those two files at all (`rulings.md#R-empty-population-is-red`). A
      census whose classifier silently stopped matching would report
      zero bare and pass (a) while gating nothing, which is this
      review's own cross-dimension pattern.

  (c) NO STALE EXEMPTION -- every row in `fence-exemptions.edn` matches
      a live fence. A row that outlives the fence it excuses is an
      excuse nobody can see is spent.

  (d) EVERY EXEMPTION CARRIES A REASON -- non-empty, and long enough to
      be a sentence rather than a shrug. `bin/fence-census` drops a
      reasonless row on its own side, which would make the fence read as
      `bare`; this asserts it from the registry side so the failure
      names the row instead of the fence."
  (:require [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def ^:private front-door #{"README.md" "SETUP.md"})

(def ^:private exemptions-path
  "components/docs-tooling/resources/docs-tooling/fence-exemptions.edn")

(defn- census
  "Every fence the census enumerates, as maps. Loud on a non-zero exit:
  an empty parse would make every count below zero, and zero bare reads
  as a pass."
  []
  (let [{:keys [exit out err]} (shell/sh "bin/fence-census" "--tsv-only")]
    (when-not (zero? exit)
      (throw (ex-info (str "bin/fence-census exited " exit
                           " -- refusing to gate on an unmeasured census")
                      {:exit exit :err err})))
    (->> (str/split-lines out)
         (remove str/blank?)
         (map #(str/split % #"\t" 6))
         (filter #(= 6 (count %)))
         (map (fn [[file line lang klass reg cmd]]
                {:file file :line line :lang lang :klass klass :reg reg :cmd cmd})))))

(defn- exemptions [] (edn/read-string (slurp exemptions-path)))

(deftest no-bare-command-fence-on-the-front-door-test
  (let [rows (census)
        commands (filter #(and (front-door (:file %)) (= "command" (:klass %))) rows)
        bare (filter #(= "bare" (:reg %)) commands)]
    (testing "(b) the population is non-empty"
      (is (seq rows) "bin/fence-census enumerated no fences at all")
      (is (seq commands)
          (str "no command fences found on " (pr-str front-door)
               " -- either the classifier stopped matching or the files moved; "
               "either way claim (a) below would pass while gating nothing")))
    (testing "(a) zero bare command fences on README.md and SETUP.md"
      (is (empty? bare)
          (str (count bare) " bare command fence(s) on the front door "
               "(R4-Q4 (a): this count is gated at zero).\n"
               (str/join "\n" (map #(str "  " (:file %) ":" (:line %) "  " (:cmd %)) bare))
               "\n\nEach must be either EXERCISED -- a bin/ script re-running it verbatim, "
               "registered in exercised-sources.edn -- or EXEMPT, a row in "
               exemptions-path " saying in prose why it cannot be run.")))))

(deftest every-fence-exemption-matches-a-live-fence-test
  (let [rows (census)
        live (set (map (juxt :file :cmd) (filter #(= "command" (:klass %)) rows)))]
    (testing "(c) no exemption outlives the fence it excuses"
      (doseq [{:keys [file first-command]} (exemptions)]
        (is (contains? live [file first-command])
            (str "fence-exemptions.edn excuses " file " / " (pr-str first-command)
                 ", which is not a live command fence in that file. The fence was "
                 "edited or removed; retire or re-anchor the exemption."))))))

(deftest every-fence-exemption-states-a-reason-test
  (testing "(d) an exemption is a written reason, not a flag"
    (doseq [{:keys [file first-command reason]} (exemptions)]
      (testing (str file " / " first-command)
        (is (and reason (<= 40 (count (str/trim reason))))
            (str "the exemption for " file " / " (pr-str first-command)
                 " carries no usable :reason. An exemption is the place the "
                 "un-runnability is written down; without it the row is a silencer."))))))
