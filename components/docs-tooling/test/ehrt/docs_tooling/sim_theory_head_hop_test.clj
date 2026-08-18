(ns ehrt.docs-tooling.sim-theory-head-hop-test
  "ADR-0152, closing `roadmap.md#sim-theory-edn-hop` and ADR-0139's
  finding C-1. `components/sim/docs/sim-theory-equations.txt` sat at the
  HEAD of an otherwise fully registered derivation chain
  (`equations.txt -> .mermaid -> the .md's embedded block`, registered by
  ADR-0136) and was, by its own header, \"hand-derived\" from
  `sim-theory.edn` with \"no Clojure translator\". Nothing gated the pair,
  so the `.edn` could drift while every downstream artifact regenerated
  byte-perfectly from the stale half and CI stayed green.

  Step 0's own probe found the drift already there, and it was NOT the
  drift the row predicted. Ten of thirteen equation lines matched
  byte-for-byte; the `.edn` could not express the other three at all:

  - `sim-theory.edn` did not VALIDATE against the `Pipeline` Malli its
    own header claimed it kept \"exactly\". Its two external stages
    (`:sut`, `:tools-intake`) sat inside `:stages` carrying
    `:external? true` but neither `:kind` nor `:status`, instead of in
    the top-level `:external-stages` key the schema provides. So
    `pipeline->equations-text` rendered them through
    `stage->equation-line` and dropped `{external: true}` from both --
    losing the `stroke-dasharray` on both black boxes, i.e. publishing a
    diagram that claims this repo implements SystemUnderTest and
    ToolsCorpusIntake.
  - There was no schema key of any kind for Calibrate's
    `{feedback: churn-profile→churn-profile}`, so that annotation was
    dropped too.

  Both were repaired at the `.edn`/renderer, not by blessing the lossy
  output, under an author licence widening this session's fence by
  exactly two items. Four claims, each red at bde5f37:

  (a) THE HEAD HOP IS MECHANICAL -- the committed equations file, minus
      its generated banner, is `pipeline->equations-text` of
      `sim-theory.edn` byte for byte. This is the claim C-1 says nothing
      made; it is what makes the `.edn` the single source of truth
      rather than one of two halves an editor is asked to keep in step.

  (b) THE `.edn` IS SCHEMA-VALID -- the dogfooding claim
      `pipeline_test`'s `committed-pipeline-edn-is-valid-test` has always
      made for `pipeline.edn`, now made for `sim-theory.edn` too. This
      is the assertion whose absence let an invalid file sit at the head
      of the chain since it was authored.

  (c) THE MAKE GRAPH -- `docsgen` depends on `sim-theory`, and
      `sim-theory` writes the equations file from the `.edn` BEFORE
      running the converter over it. Same shape as ADR-0149's own
      make-graph claim for `traces`, extended rather than duplicated.

  (d) THE CI DIFF LIST -- the equations file is on the freshness step's
      `git diff --exit-code` population. Without it the new first step
      regenerates a file nothing compares, which is the ungated state
      ADR-0136 registered the rest of this chain to end.

  The rider (same ADR, its own commit) is claim (e): `:execute`'s `:laws`
  state the two run-level event-contract properties ADR-0141 established.
  Laws do not render into equations, so it moves no artifact -- which is
  itself asserted, at the close, by `git status` over the regenerated
  three."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.docs-tooling.pipeline :as pipeline]
            [malli.core :as m]
            [malli.error :as me]))

(def ^:private theory-edn "components/sim/docs/sim-theory.edn")
(def ^:private equations-txt "components/sim/docs/sim-theory-equations.txt")
(def ^:private makefile "Makefile")
(def ^:private workflow ".github/workflows/test.yml")

;; ---- (b) the .edn is schema-valid ----

(deftest committed-sim-theory-edn-is-valid-test
  (let [data (edn/read-string (slurp theory-edn))]
    (is (pipeline/valid? data)
        (str theory-edn " must validate against the Pipeline Malli it claims to keep "
             "\"exactly\" -- an invalid head means the translator renders SOMETHING for it "
             "and nothing says the something is right. Explain: "
             (pr-str (me/humanize (m/explain pipeline/Pipeline data)))))))

(deftest committed-sim-theory-edn-files-its-external-stages-under-external-stages-test
  ;; The specific invalidity Step 0 found, pinned so a future edit cannot
  ;; quietly reintroduce it: an :external? entry inside :stages renders
  ;; through stage->equation-line and silently loses {external: true}.
  (let [{:keys [stages external-stages]} (edn/read-string (slurp theory-edn))]
    (is (empty? (filter :external? stages))
        (str "no :stages entry may carry :external? -- external stages belong in "
             ":external-stages, or they render as ordinary implemented stages and both "
             "black boxes lose their dashed border. Offenders: "
             (pr-str (map :id (filter :external? stages)))))
    (is (= 2 (count external-stages))
        "SystemUnderTest and ToolsCorpusIntake are the two declared external stages")
    (is (every? pipeline/valid-external-stage? external-stages)
        "each :external-stages entry must satisfy ExternalStage")))

;; ---- (a) the head hop is mechanical ----

(defn- banner-lines
  "The leading `#`-comment banner of `text`, as a vector of lines."
  [text]
  (vec (take-while #(str/starts-with? % "#") (str/split-lines text))))

(defn- body-after-banner
  "`text` with its leading `#`-comment banner removed -- what the
  translator itself is responsible for, byte for byte."
  [text]
  (str/join "\n" (drop (count (banner-lines text)) (str/split-lines text))))

(deftest sim-theory-equations-txt-is-generated-from-the-edn-test
  (let [committed (slurp equations-txt)
        rendered (pipeline/pipeline->equations-text (edn/read-string (slurp theory-edn)))]
    (is (= rendered (body-after-banner committed))
        (str equations-txt " must equal `pipeline->equations-text` of " theory-edn
             " byte for byte after its banner. A difference here IS the C-1 drift: the "
             ".edn and the .txt disagree, and every downstream artifact regenerates "
             "byte-perfectly from whichever half the converter is pointed at."))))

(deftest sim-theory-equations-txt-carries-the-generated-banner-test
  (let [lines (banner-lines (slurp equations-txt))
        banner (str/join "\n" lines)]
    (is (= 4 (count lines))
        (str "the banner is pinned at 4 lines. LINE COUNT IS LOAD-BEARING: the converter's "
             "`%% Arrow N` comments derive from this file's own line numbering, so a banner "
             "line added or removed renumbers every arrow in the .mermaid and the .md block. "
             "Moving this pin means regenerating all three in the same commit."))
    (is (str/includes? banner "make sim-theory")
        "the banner must name the target that regenerates the file")
    (is (str/includes? banner theory-edn)
        "the banner must name the source to edit instead")
    (is (str/includes? banner "do not hand-edit")
        "the banner must say the file is not hand-edited")))

;; ---- (c) the make graph ----

(defn- make-target-prerequisites
  "The prerequisite list of `target` in `makefile-text`, as a vector of
  words -- nil if the target has no rule. Reads the Makefile as text on
  purpose, the same reason ADR-0149's own copy does: the claim is about
  what the committed build graph says, not what a `make -np` on one
  machine resolves it to."
  [makefile-text target]
  (when-let [[_ prereqs] (re-find (re-pattern (str "(?m)^" target ":(.*)$")) makefile-text)]
    (vec (remove str/blank? (str/split (str/trim prereqs) #"\s+")))))

(defn- make-target-recipe
  "The recipe lines of `target` -- every line after its rule line that
  begins with a tab, to the first line that does not."
  [makefile-text target]
  (when-let [after (second (str/split makefile-text (re-pattern (str "(?m)^" target ":.*$")) 2))]
    (vec (take-while #(str/starts-with? % "\t") (rest (str/split-lines after))))))

(deftest the-sim-theory-target-writes-the-equations-file-first-test
  (let [text (slurp makefile)
        recipe (make-target-recipe text "sim-theory")
        joined (str/join "\n" recipe)]
    (is (seq recipe) "a `sim-theory:` rule with a recipe must exist")
    (is (true? (str/includes? joined "write-sim-theory-equations-txt!"))
        (str "`sim-theory:` must run the translator, or the head hop is still a hand "
             "derivation. Recipe today: " (pr-str recipe)))
    (is (true? (str/includes? joined theory-edn))
        "the translator step must read the .edn")
    (is (true? (str/includes? joined equations-txt))
        "the translator step must write the equations file")
    ;; Ordering, not just presence: the converter reads what the
    ;; translator writes, so a translator step placed after it would
    ;; regenerate the .mermaid from the PREVIOUS run's equations.
    (let [translator (first (keep-indexed #(when (str/includes? %2 "write-sim-theory-equations-txt!") %1) recipe))
          converter (first (keep-indexed #(when (str/includes? %2 "resource_equations_to_mermaid.py") %1) recipe))]
      (is (and translator converter (< translator converter))
          (str "the translator step must precede the converter step, or the .mermaid is "
               "rendered from the previous run's equations file. Indices: translator "
               translator ", converter " converter)))))

(deftest docsgen-depends-on-the-sim-theory-target-test
  (let [text (slurp makefile)]
    (is (some #{"sim-theory"} (make-target-prerequisites text "docsgen"))
        (str "`make docsgen` must depend on `sim-theory`, or the equations file is "
             "regenerated only by a target CI never runs. docsgen prerequisites today: "
             (pr-str (make-target-prerequisites text "docsgen"))))))

;; ---- (d) the CI diff list ----

(defn- freshness-diff-paths
  "The paths `.github/workflows/test.yml`'s generated-doc freshness step
  hands `git diff --exit-code`, read out of the committed workflow: every
  backslash-continued line after the command, to the first line that does
  not continue. Same extractor as ADR-0149's own claim (d)."
  [workflow-text]
  (when-let [after (second (str/split workflow-text #"git diff --exit-code \\\R" 2))]
    (loop [lines (str/split-lines after) out []]
      (if-let [line (first lines)]
        (let [trimmed (str/trim line)
              continues? (str/ends-with? trimmed "\\")
              path (str/replace trimmed #"\s*\\$" "")]
          (if (str/blank? path)
            out
            (let [out (conj out path)]
              (if continues? (recur (rest lines) out) out))))
        out))))

(deftest the-ci-freshness-step-diffs-the-equations-file-test
  (let [paths (freshness-diff-paths (slurp workflow))]
    (is (pos? (count paths))
        "sanity: the freshness step must actually name paths -- if this returns none the extractor broke and the claim below is silently vacuous")
    (is (some #{equations-txt} paths)
        (str "`" equations-txt "` must be on CI's freshness diff list, or `make sim-theory` "
             "rewrites a file nothing compares and the `.edn` can drift from it exactly as "
             "before -- ADR-0139's C-1 surviving its own fix. Diffed paths today: "
             (pr-str paths)))
    (testing "the rest of the chain stays on the list -- this claim ADDS a path, never trades one"
      (is (some #{"components/sim/docs/sim-theory-diagram.mermaid"} paths))
      (is (some #{"components/sim/docs/sim-theory-diagram.md"} paths)))))

;; ---- (e) the rider: :execute states the event-contract laws ----

(def ^:private execute-law-substrings
  "Verbatim fragments of the two ADR-0141 run-level properties. Substrings
  rather than whole laws on purpose: the law text carries its own prose,
  and pinning the whole string would make every wording touch a test edit.
  These are the load-bearing names and claims."
  ["event-log contract (ADR-0141)"
   "ehrt.sim-engine.event-schema/schema-version"
   ":event-schema-version"
   "valid-ground-truth?"
   "run-t-monotone?"
   "never a per-event constraint"
   "no run-boundary marker"])

(deftest execute-stage-states-the-event-contract-laws-test
  (let [{:keys [stages]} (edn/read-string (slurp theory-edn))
        execute (first (filter #(= :execute (:id %)) stages))
        laws (str/join "\n" (:laws execute))]
    (is (some? execute) "the :execute stage must exist")
    (doseq [fragment execute-law-substrings]
      (is (true? (str/includes? laws fragment))
          (str ":execute's :laws must state the event-contract law naming `" fragment
               "` -- ADR-0141 established the ground-truth log's contract as run-level "
               "properties, and `rulings.md#R-law-surface-propagation` puts a standing law "
               "on every surface that states laws. :execute is the stage that PRODUCES "
               "ground-truth-log; its :laws had three entries, none of them this one.")))))
