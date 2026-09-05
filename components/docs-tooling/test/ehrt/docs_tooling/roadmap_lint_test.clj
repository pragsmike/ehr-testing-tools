(ns ehrt.docs-tooling.roadmap-lint-test
  "The roadmap's own row contract, gated (compression arc session B,
  `notes/adr/0144-roadmap-row-contract.md`, author rulings Q1-Q5 'a.
  throughout').

  `.agents/plans/roadmap.md` is hand-owned intent, so unlike
  `notes/ADRs.md` (ADR-0143) it cannot be generated. It is made SMALL
  and LINTED instead. Every top-level row in every section satisfies:

    - **token** -- the first token after the bullet is exactly one of
      `OPEN`, `CLOSED <yyyy-mm-dd> <ADR-NNNN|sha>`,
      `DEFERRED (trigger: ...)`, or `EXTERNAL`. Guard #1: a `CLOSED`
      row anywhere but `## Done` is red. Its DUAL: a closure word
      (LANDED/CLOSED/FIXED/DONE/RESOLVED) in the first sentence of a
      row that is NOT tokened `CLOSED` is red -- that is the shape the
      token was introduced to retire, a row whose prose says it is
      finished while its position says it is not.
    - **slug** -- a stable `**[slug]**` anchor right after the token,
      unique across the file. Rows are cited `roadmap.md#<slug>`; a
      `roadmap.md:NNN` line cite in any live surface is red, because
      line numbers rot on every insert (the specimen this arc opened
      against, `roadmap.md:222`, addressed the latency row when it was
      written and line 237 by the time session B read it).
    - **cap** -- `## Done` rows are capped at `done-row-cap` characters
      (R-cap, 2026-09-05). ADR-0144 worded this as 'six lines,
      maximum' and left it as prose with no assertion behind it; the
      instruction FAILED TWICE and `## Done` grew nineteen rows over
      400 characters, the longest 6,269. A gate is what an instruction
      becomes once it has failed. Open rows still state what remains
      and why, and cite the ADR that holds the rest.
    - **priority** -- `## Next` rows carry `PRIORITY n`, n unique and
      ascending in file order, so `head` answers 'what is next'.

  This namespace is `roadmap-deferred-closure-lint-test` widened
  (`git mv`, ADR-0144). Its ancestor's one assertion -- a `## Deferred`
  row that closes in place must disclose where its content relocated
  to (D2-5; the `myocardial_infarction.json` incident, ADR-0047) -- is
  kept below, unchanged, as one case among the rest. It is not
  redundant with guard #1: guard #1 catches a row TOKENED closed in
  the wrong section, while the ancestor catches a row that closes in
  its own PROSE, which is what a row does on the way to being
  mis-tokened.

  SCAN ROOTS, and why they are an include-list rather than `.agents/**`
  and `notes/**` (disclosed, ADR-0144 finding F-2). The author's Q2
  ruling named those two globs; the census found both contain standing
  FROZEN populations that a cite lint must not reach --
  `notes/prompts/` (frozen by `ehrt.docs-tooling.notes-prompts-frozen-
  test`), `notes/sim/` ('provenance, untouched by law', ADR-0143), and
  every dated one-shot file under `.agents/prompts/`,
  `.agents/session-records/` and `.agents/plans/`, which narrate
  history AT AUTHORING TIME. That boundary is not invented here: it is
  the same live-surface include-list `ehrt.docs-tooling.stale-path-test`
  has drawn since 2026-08-05 (register row S7, ADR-0050), for the same
  reason, and it is what lets this gate carry ZERO dated exemptions.
  A `roadmap.md:NNN` inside a 2026-08-16 session record is a true
  statement about that day's file and stays; a `roadmap.md:NNN` in a
  perpetually-live index is a pointer that has already rotted."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private roadmap-path ".agents/plans/roadmap.md")

;; -- the ancestor's own assertion (D2-5), unchanged --

(def ^:private closure-word-pattern #"\b(RESOLVED|CLOSED|FIXED)\b")
(def ^:private disclosure-phrase-pattern #"(?i)(see Done|relocated|moved to Done)")

;; -- the row contract (ADR-0144) --

(def ^:private token-pattern
  "The four ruled tokens, anchored at the start of a row's own text.
  `DEFERRED`'s trigger clause is deliberately paren-free inside
  (`[^()]+`) so the token's own extent is unambiguous without balanced-
  paren parsing; a trigger needing a parenthetical rewords instead.

  The terminator is `(?=\\s|$)`, not `\\b`: the DEFERRED form ends in
  `)`, and `\\b` requires a WORD character on one side, so a `\\b` here
  silently rejects every DEFERRED row. The mechanism-sanity case below
  caught exactly that on this pattern's first run -- recorded because a
  gate that cannot match one of its own four tokens would have passed
  green over every DEFERRED row forever."
  #"^(OPEN|CLOSED \d{4}-\d{2}-\d{2} (?:ADR-\d{4}|[0-9a-f]{7,40})|DEFERRED \(trigger: [^()]+\)|EXTERNAL)(?=\s|$)")

(def ^:private slug-pattern #"\*\*\[([a-z0-9]+(?:-[a-z0-9]+)*)\]\*\*")
(def ^:private priority-pattern #"\bPRIORITY (\d+)\b")

(def ^:private dual-closure-word-pattern
  "The closure vocabulary the DUAL reads, in the ruled ALL-CAPS form.
  Case-sensitive for the same reason the ancestor's is (its own
  docstring records the false positive a case-insensitive match threw
  on ordinary prose)."
  #"\b(LANDED|CLOSED|FIXED|DONE|RESOLVED)\b")

(def ^:private line-cite-pattern #"roadmap\.md:\d")
(def ^:private slug-cite-pattern #"roadmap\.md#([a-z0-9-]+)")

(def ^:private live-scan-roots
  "Perpetually-live surfaces: index/plan files edited in place session
  after session, plus every skill (both trees -- `.claude/skills` is a
  mirror `ehrt.docs-tooling.skill-mirror-currency-test` holds
  byte-equal). Dated one-shot files and frozen archives are out of
  population by the standing boundary the namespace docstring cites."
  (concat [".agents/plans/roadmap.md"
           ".agents/plans/README.md"
           ".agents/rulings.md"
           "AGENTS.md"]
          (->> (concat (file-seq (io/file ".agents/skills"))
                       (file-seq (io/file ".claude/skills")))
               (filter #(.isFile %))
               (filter #(= "SKILL.md" (.getName %)))
               (map #(.getPath %))
               sort)))

;; -- extraction --

(defn- sections
  "Every `## ` section of `content` as `[heading body-lines]`. A
  heading that wraps onto continuation lines keeps them in its body,
  where they are not rows and belong to no row."
  [content]
  (let [lines (str/split-lines content)
        starts (keep-indexed (fn [i l] (when (str/starts-with? l "## ") i)) lines)]
    (map (fn [start next-start]
           [(nth lines start) (subvec (vec lines) (inc start) (or next-start (count lines)))])
         starts
         (concat (rest starts) [nil]))))

(defn- rows
  "Groups a section's own body lines into top-level bullet rows: every
  line starting `- ` at column 0 begins a new row; every following line
  belongs to that same row until the next top-level bullet. Trailing
  blank lines belong to no row."
  [lines]
  (->> lines
       (reduce (fn [acc line]
                 (if (str/starts-with? line "- ")
                   (conj acc [line])
                   (if (seq acc) (conj (pop acc) (conj (peek acc) line)) acc)))
               [])
       (map (fn [row] (vec (reverse (drop-while str/blank? (reverse row))))))))

(defn- section-lines
  "The lines of `content`'s own section starting at a `## <heading>`
  line matching `heading`, header line included (the ancestor's own
  shape, kept so its assertion below reads unchanged)."
  [content heading]
  (let [lines (str/split-lines content)
        start (->> lines (keep-indexed (fn [i l] (when (str/starts-with? l heading) i))) first)]
    (when start
      (cons (nth lines start)
            (take-while #(not (str/starts-with? % "## ")) (drop (inc start) lines))))))

(defn- all-rows
  "Every row in the file as `{:heading :lines :text :body}` -- `:body`
  is the row's own text with the leading `- ` stripped, which is what
  the token anchors against."
  [content]
  (for [[heading body-lines] (sections content)
        row (rows body-lines)]
    {:heading heading
     :lines row
     :text (str/join "\n" row)
     :body (str/replace (str/join "\n" row) #"^- " "")}))

(defn- done-section? [heading] (str/starts-with? heading "## Done"))
(defn- next-section? [heading] (str/starts-with? heading "## Next"))

(defn- first-sentence [body] (first (str/split (str/replace body "\n" " ") #"(?<=[.!?])\s")))

(defn- token-of [row] (some-> (re-find token-pattern (:body row)) second))

(defn- abbrev [row] (subs (:text row) 0 (min 90 (count (:text row)))))

;; -- the gates --

(deftest every-row-carries-one-of-the-four-status-tokens-test
  (testing "Q1(a): the first token after the bullet is OPEN | CLOSED <date> <ADR|sha> | DEFERRED (trigger: ...) | EXTERNAL"
    (let [bad (mapv abbrev (remove token-of (all-rows (slurp roadmap-path))))]
      (is (empty? bad)
          (str (count bad) " roadmap row(s) carry no status token as their first token "
               "(ADR-0144 Q1)")))))

(deftest closed-rows-live-only-under-done-test
  (testing "guard #1: a CLOSED row outside ## Done is the shape this arc opened against"
    (let [bad (->> (all-rows (slurp roadmap-path))
                   (filter #(str/starts-with? (or (token-of %) "") "CLOSED"))
                   (remove #(done-section? (:heading %)))
                   (mapv abbrev))]
      (is (empty? bad)
          (str (count bad) " CLOSED row(s) sitting outside ## Done -- move each verbatim to "
               ".agents/plans/roadmap-done-2026-08.md and leave one Done line")))))

(deftest a-non-closed-row-does-not-lead-with-closure-words-test
  (testing "guard #1's dual: closure words in the first sentence of a row that is not tokened CLOSED"
    (let [bad (->> (all-rows (slurp roadmap-path))
                   (remove #(str/starts-with? (or (token-of %) "") "CLOSED"))
                   (filter #(re-find dual-closure-word-pattern (first-sentence (:body %))))
                   (mapv abbrev))]
      (is (empty? bad)
          (str (count bad) " row(s) whose first sentence claims closure while the row is not "
               "tokened CLOSED -- retoken the row, or move it to ## Done")))))

(deftest every-row-carries-a-slug-anchor-test
  (testing "Q2(a): a stable **[slug]** right after the token"
    (let [bad (mapv abbrev (remove #(re-find slug-pattern (:body %))
                                   (all-rows (slurp roadmap-path))))]
      (is (empty? bad)
          (str (count bad) " roadmap row(s) carry no **[slug]** anchor (ADR-0144 Q2)")))))

(deftest slugs-are-unique-test
  (testing "Q2(a): a slug addresses exactly one row, or a cite is ambiguous"
    (let [slugs (keep #(second (re-find slug-pattern (:body %))) (all-rows (slurp roadmap-path)))
          dupes (->> slugs frequencies (filter #(< 1 (val %))) (map key) sort)]
      (is (empty? dupes) (str "duplicate roadmap slug(s): " (vec dupes))))))

(deftest next-rows-carry-unique-ascending-priorities-test
  (testing "Q5(a): PRIORITY n on every ## Next row, unique, ascending, so head is what is next"
    (let [next-rows (filter #(next-section? (:heading %)) (all-rows (slurp roadmap-path)))
          ps (map #(some-> (re-find priority-pattern (:body %)) second parse-long) next-rows)]
      (is (every? some? ps)
          (str (count (remove some? ps)) " of " (count ps)
               " ## Next row(s) carry no PRIORITY n"))
      (when (every? some? ps)
        (is (apply distinct? ps) (str "duplicate PRIORITY value(s) in ## Next: " (vec ps)))
        (is (= ps (sort ps)) (str "## Next PRIORITY values are not ascending in file order: " (vec ps)))))))

(def ^:private done-row-cap
  "480 characters. Ruled 2026-09-05 (R-cap) after the prose form of the
  same rule failed twice -- see the namespace docstring's `cap` bullet.

  Why a CHARACTER count and not the stated line count: a roadmap row is
  written unwrapped, one physical line, so \"six lines, maximum\" was
  never measurable against a Done row at all. 480 is what a compacted
  row actually needs and no more -- the status token, the slug, the ADR
  or sha, at least one record path (which `ehrt.docs-tooling.stale-
  path-test` then holds RESOLVABLE, this file being one of its own scan
  roots), and one clause of outcome. Nothing else is required to
  survive, because the rest is in the record the row cites, and a Done
  row that re-tells its record is a second copy that can go stale
  against it."
  480)

(deftest done-rows-are-pointers-not-ledgers-test
  (testing "R-cap: a ## Done row is a pointer at its record, capped at done-row-cap characters"
    (let [bad (->> (all-rows (slurp roadmap-path))
                   (filter #(done-section? (:heading %)))
                   (filter #(< done-row-cap (count (:text %))))
                   (mapv #(str (count (:text %)) " chars -- " (abbrev %))))]
      (is (empty? bad)
          (str (count bad) " ## Done row(s) over " done-row-cap " characters -- compact each "
               "to its status token, slug, ADR/sha, one record path and one clause of "
               "outcome, and leave the rest in the record (R-cap): " bad)))))

(deftest no-live-surface-cites-the-roadmap-by-line-number-test
  (testing "Q2(a): line-number cites rot on every insert; live surfaces cite roadmap.md#<slug>"
    (let [bad (for [path live-scan-roots
                    :let [content (slurp path)]
                    [lineno line] (map-indexed (fn [i l] [(inc i) l]) (str/split-lines content))
                    :when (re-find line-cite-pattern line)]
                (str path ":" lineno))]
      (is (empty? (vec bad))
          (str (count bad) " live-surface line cite(s) into .agents/plans/roadmap.md -- "
               "rewrite as roadmap.md#<slug> (ADR-0144 Q2): " (vec bad))))))

(deftest every-cited-slug-resolves-test
  (testing "Q2(a): a roadmap.md#<slug> cite in a live surface addresses a row that exists"
    (let [defined (set (keep #(second (re-find slug-pattern (:body %)))
                             (all-rows (slurp roadmap-path))))
          bad (for [path live-scan-roots
                    :let [content (slurp path)]
                    [_ slug] (re-seq slug-cite-pattern content)
                    :when (not (defined slug))]
                (str path " -> roadmap.md#" slug))]
      (is (empty? bad)
          (str "live surface(s) cite a roadmap slug no row defines: " (vec bad))))))

;; -- the ancestor's own case (D2-5), kept unchanged --

(defn- closes-in-place-without-disclosure?
  [row]
  (and (re-find closure-word-pattern row)
       (not (re-find disclosure-phrase-pattern row))))

(deftest deferred-rows-that-close-in-place-disclose-their-own-relocation-test
  (testing "a row closing in place without naming where its content relocated to is the myocardial_infarction.json incident, recurring"
    (let [lines (section-lines (slurp roadmap-path) "## Deferred")
          violations (filter closes-in-place-without-disclosure?
                             (map #(str/join "\n" %) (rows (vec (rest lines)))))]
      (is (empty? violations)
          (str "Deferred row(s) close in place ('RESOLVED'/'CLOSED'/'FIXED') without disclosing "
               "where the closed content relocated to (D2-5, D7-3's compliant shape) -- "
               "either add a 'see Done'/'relocated'/'moved to Done' disclosure, or this is "
               "the myocardial_infarction.json pattern recurring: "
               (vec (map #(subs % 0 (min 80 (count %))) violations)))))))

;; -- mechanism-sanity: prove the extraction and the patterns actually catch what they claim to --

(deftest section-lines-extraction-is-actually-caught-test
  (let [fixture "## Now\n- in progress\n\n## Deferred\n- row one\n  continuation\n- row two\n\n## Done\n- irrelevant\n"]
    (is (= ["## Deferred" "- row one" "  continuation" "- row two" ""]
           (section-lines fixture "## Deferred")))))

(deftest rows-grouping-is-actually-caught-test
  (is (= [["- row one" "  continuation"] ["- row two"]]
         (rows ["- row one" "  continuation" "- row two" ""]))))

(deftest all-rows-attributes-each-row-to-its-own-section-test
  (let [fixture "## Next (backlog)\n- OPEN **[a]** PRIORITY 1 x\n\n## Done (live)\n- CLOSED 2026-08-01 ADR-0001 **[b]**\n"]
    (is (= [["## Next (backlog)" "OPEN"] ["## Done (live)" "CLOSED 2026-08-01 ADR-0001"]]
           (map (juxt :heading token-of) (all-rows fixture))))))

(deftest each-of-the-four-tokens-is-actually-recognized-test
  (is (= "OPEN" (token-of {:body "OPEN **[x]** PRIORITY 1 something"})))
  (is (= "CLOSED 2026-08-16 ADR-0141" (token-of {:body "CLOSED 2026-08-16 ADR-0141 **[x]**"})))
  (is (= "CLOSED 2026-08-16 30cc335" (token-of {:body "CLOSED 2026-08-16 30cc335 **[x]**"})))
  (is (= "DEFERRED (trigger: a session with a Synthea checkout)"
         (token-of {:body "DEFERRED (trigger: a session with a Synthea checkout) **[x]** why"})))
  (is (= "EXTERNAL" (token-of {:body "EXTERNAL **[x]** an author errand"}))))

(deftest a-row-with-no-token-is-actually-caught-test
  (is (nil? (token-of {:body "**Some item** — LANDED 2026-08-16, arc closed."})))
  (is (nil? (token-of {:body "closed 2026-08-16 ADR-0141 **[x]**"})) "lower-case is not the token")
  (is (nil? (token-of {:body "CLOSED 2026-08-16 **[x]**"})) "CLOSED owes a date AND an ADR or sha"))

(deftest the-dual-is-actually-caught-test
  (is (re-find dual-closure-word-pattern
               (first-sentence "OPEN **[x]** PRIORITY 1 — arc CLOSED, three deferrals remain."))
      "a closure word in the first sentence of an OPEN row is the dual's own population")
  (is (nil? (re-find dual-closure-word-pattern
                     (first-sentence "OPEN **[x]** PRIORITY 1 — what remains. The arc CLOSED earlier.")))
      "a closure word in a LATER sentence is history, not a status claim"))

(deftest the-done-row-cap-is-actually-caught-test
  (let [over (str "- CLOSED 2026-09-05 abc1234 **[over]** -- " (apply str (repeat done-row-cap "x")))
        under "- CLOSED 2026-09-05 abc1234 **[under]** -- done. Record: `.agents/session-records/r.md`."
        fixture (str "## Done\n" over "\n" under "\n")
        caught (->> (all-rows fixture)
                    (filter #(done-section? (:heading %)))
                    (filter #(< done-row-cap (count (:text %))))
                    (mapv #(second (re-find slug-pattern (:body %)))))]
    (is (= ["over"] caught) "the cap catches the over-long Done row and only it")))

(deftest the-done-row-cap-does-not-reach-open-rows-test
  (testing "R-cap is a Done-section rule: an open row's own budget is ADR-0144's, unchanged"
    (let [fixture (str "## Next (backlog)\n- OPEN **[long]** PRIORITY 1 "
                       (apply str (repeat done-row-cap "x")) "\n")
          caught (->> (all-rows fixture)
                      (filter #(done-section? (:heading %)))
                      (filter #(< done-row-cap (count (:text %)))))]
      (is (empty? (vec caught))))))

(deftest the-line-cite-pattern-is-actually-caught-test
  (is (re-find line-cite-pattern "B's first specimen is `roadmap.md:222`, the latency row"))
  (is (re-find line-cite-pattern "`.agents/plans/roadmap.md:82`, landed by ADR-0139"))
  (is (nil? (re-find line-cite-pattern "cited as `roadmap.md#downstream-latency`"))
      "the slug form is the fix, not another violation"))

(deftest the-slug-cite-pattern-is-actually-caught-test
  (is (= [["roadmap.md#downstream-latency" "downstream-latency"]]
         (re-seq slug-cite-pattern "see `roadmap.md#downstream-latency` for the survivors"))))

(deftest a-closed-in-place-row-without-disclosure-is-caught-test
  (is (closes-in-place-without-disclosure? "- **Some item** CLOSED this session, done.")))

(deftest a-closed-in-place-row-with-disclosure-is-not-flagged-test
  (testing "the live compliant precedent rows must pass -- disclosure anywhere in the row is sufficient"
    (is (not (closes-in-place-without-disclosure?
              "- **Census tool refinements**: (a) and (c) **CLOSED** ... their own original text relocated verbatim into ADR-0069's own record.")))
    (is (not (closes-in-place-without-disclosure?
              "- **Lookup-table column time**: The race half of the original combined row CLOSED this session — see Done, below.")))))

(deftest a-row-mentioning-neither-closure-word-is-never-flagged-test
  (is (not (closes-in-place-without-disclosure? "- an ordinary open Deferred row, untouched"))))
