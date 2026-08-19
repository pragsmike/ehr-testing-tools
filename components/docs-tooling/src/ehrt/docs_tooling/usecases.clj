(ns ehrt.docs-tooling.usecases
  "Schema and renderer for the use-cases catalog's own data
  (docs/use-cases.edn, P6) -- a sibling to ehrt.docs-tooling.pipeline
  (docs/pipeline.edn), same split: this namespace validates the
  authored EDN and assembles already-rendered equations/mermaid text
  into docs/use-cases.md, the generated index, plus one standalone
  page per case at docs/use-cases/<id>.md (`make use-cases`); it has
  nothing to do with actually running the string-diagram skill's
  python mermaid step, same as pipeline.clj's own render-pipeline-md.

  Split into an index plus per-case pages (migration item 14,
  2026-08-02, `.agents/plans/2026-08-01-migration-report.md` RULED
  2026-08-01 item 8, `notes/2026-07-30-refactoring-review.md` P3-1):
  before this, every case's full rendering (narrative fields, strip,
  equations, mermaid) lived in one ever-growing docs/use-cases.md.
  case->body-md is the shared rendering unit both the (now-retired)
  single-page section and the standalone per-case page were built
  from, so the split changed heading level and file boundaries only
  -- never a case's own narrative, strip, equations, or diagram text.

  Unlike docs/pipeline.edn's :stages (full Malli-schema'd Stage
  records, one equation each), a use case's :equations is a vector of
  already-formatted equation-line strings -- most use cases reuse
  several of docs/pipeline.edn's own named stages verbatim (Generate,
  Mutate, Gate, ...) in one short sequence, so re-declaring each
  stage's full :kind/:status/:laws record per use case would be pure
  duplication; the equation-line text itself (the same grammar
  docs/notation.md defines) is the right unit here."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ehrt.kernel.interface :as kernel]
            [malli.core :as m]))

(def maturities
  "Per-use-case honesty labels (distinct from README.md's per-
  capability maturity table): :usable (the equations below are
  exactly what's shipped, today); :experimental (shipped, interfaces
  may move); :illustrative (the pattern is real and composable from
  shipped stages, but no example pipeline ships it end-to-end);
  :planned (the equation names a resource/stage this repo doesn't
  fully support yet -- stated honestly in :get, not smoothed over)."
  #{:usable :experimental :illustrative :planned})

(def Commands
  "One case's runnable strip (DOC-4). :lines are the literal lines of a
  single copy-pasteable fenced block -- `bin/ehrt ...`
  invocations exactly as README.md's Quickstart spells them, plus `#`
  comment lines and $PLACEHOLDER shell variables for values the reader
  supplies. Nothing that a paste would break goes in :lines; markdown
  prose and cross-links (operators.md, locators.md, cli.md,
  judge-calibration.md -- linked, never restated) go in the optional
  :note, rendered as a paragraph *below* the fence."
  [:map
   [:lines [:vector :string]]
   [:note {:optional true} :string]])

(def UseCase
  [:and
   [:map
    [:id :keyword]
    [:title :string]
    [:audience :string]
    [:bring :string]
    [:get :string]
    [:maturity (into [:enum] maturities)]
    [:equations [:vector :string]]
    ;; DOC-4: a case carries EITHER a verified runnable strip
    ;; (:commands) OR the honest reason there isn't one (:no-commands),
    ;; never both and never an invented invocation. A case with neither
    ;; still renders a stub, derived from :bring alone.
    [:commands {:optional true} Commands]
    [:no-commands {:optional true} :string]]
   [:fn {:error/message ":commands and :no-commands are mutually exclusive"}
    (fn [c] (not (and (:commands c) (:no-commands c))))]])

(def StartHereRow
  "One row of the generated \"Start here\" table (ADR-0146, finding
  U-9): a reader's question in the words they actually arrive with, and
  the case id it routes to. The question is prose; the `:case` is a
  REFERENCE, and `UseCases` below refuses a document whose row names a
  case that doesn't exist -- a router that points at a missing page is
  worse than no router."
  [:map
   [:question :string]
   [:case :keyword]])

(def UseCases
  [:and
   [:map
    [:schema-version [:= 1]]
    [:cases [:vector UseCase]]
    ;; Optional so a minimal document (and every test fixture that
    ;; predates this key) stays valid; every row PRESENT must resolve,
    ;; which is the :fn below.
    [:start-here {:optional true} [:vector StartHereRow]]]
   [:fn {:error/message "every :start-here row's :case must name a case id present in :cases"}
    (fn [{:keys [cases start-here]}]
      (let [ids (set (map :id cases))]
        (every? #(contains? ids (:case %)) start-here)))]])

(defn valid-use-case?
  [use-case]
  (m/validate UseCase use-case))

(defn valid-start-here-row?
  [row]
  (m/validate StartHereRow row))

(defn valid?
  [use-cases]
  (m/validate UseCases use-cases))

;; ---- rendering: docs/use-cases.edn -> docs/use-cases.md ----

(defn case->commands-block
  "Renders one use case's **You type:** block (DOC-4) -- the runnable
  strip if the case has one, the honest stub if it doesn't. Both arms
  render from the case's own data: no invocation is synthesized here,
  and the stub's sentence is derived from :bring plus the case's own
  :no-commands reason, so a case this repo can't drive end to end says
  so rather than showing a hypothetical command."
  [{:keys [bring commands no-commands]}]
  (if commands
    (str "**You type:**\n\n"
         "```sh\n" (str/join "\n" (:lines commands)) "\n```\n"
         (when-let [note (:note commands)] (str "\n" note "\n")))
    (str "**You type:** no strip -- this repo doesn't drive this use case "
         "end to end, so there is no command sequence to copy. You bring: "
         bring
         (when no-commands (str " " no-commands))
         "\n")))

(defn case-slug
  "The per-case page's own filename stem, docs/use-cases/<slug>.md --
  the case's own :id, verbatim (already kebab-case, already unique per
  committed-use-cases-edn-has-unique-ids-test), never re-derived from
  :title's own GitHub heading-slug algorithm."
  [{:keys [id]}]
  (name id))

(defn case->body-md
  "Renders one use case's narrative fields (audience/bring/get/
  maturity), the runnable strip (or its honest stub), the raw
  equations block, and the case's own already-rendered mermaid
  diagram -- everything about a case except its own heading, which
  differs by rendering context (the per-case page's own H1). The strip
  sits above the equations rather than below the diagram: it is the
  most copy-pasted surface this catalog carries (AUTHORS-GUIDE.md
  section 6), and the equations are the formal grounding underneath
  it, not a preamble to it."
  [{:keys [audience bring get maturity equations] :as use-case} mermaid-text]
  (str "**Audience:** " audience "\n\n"
       "**You bring:** " bring "\n\n"
       "**You get:** " get "\n\n"
       "**Maturity:** " (name maturity) "\n\n"
       (case->commands-block use-case) "\n"
       "```\n" (str/join "\n" equations) "\n```\n\n"
       "```mermaid\n" (str/trim mermaid-text) "\n```\n"))

(defn case->page-md
  "Renders one use case as its own standalone page
  (docs/use-cases/<id>.md): a generated-source banner, the case's own
  title as an H1 (this page's only heading, so no GitHub heading-slug
  anchor is needed to reach it -- the file itself is the address),
  then case->body-md."
  [{:keys [id title] :as use-case} mermaid-text]
  (str "<!-- GENERATED by `make use-cases` from components/corpus/docs/use-cases.edn (case "
       (name id) ") -- do not hand-edit.\n"
       "     Edit components/corpus/docs/use-cases.edn and regenerate instead. -->\n\n"
       "# " title "\n\n"
       (case->body-md use-case mermaid-text)))

(defn case->index-line
  "One line of docs/use-cases.md's generated index: the case's own
  title, linked to its standalone page, followed by its own :audience
  prose verbatim -- a citation of the case's own data, never a
  rewritten summary or an invented tag."
  [{:keys [title audience] :as use-case}]
  (str "- [" title "](use-cases/" (case-slug use-case) ".md) -- " audience))

(defn render-start-here-md
  "The \"Start here\" table (ADR-0146, finding U-9), or \"\" when a
  document declares no rows -- so the catalog degrades to exactly its
  pre-ADR-0146 shape rather than rendering an empty table.

  Why it exists: the catalog was a flat list of twenty-two
  undifferentiated audience sentences in EDN order, so a reader with ONE
  question had to read all twenty-two to find their row. The cold walk
  that produced this table was the emitter author's, whose own case sat
  at row 19 -- but the fix is not about that one row, it is that a
  catalog this long needs a first screen a reader can scan. Rows carry
  the reader's own question, never a rewritten title, and route to the
  case's own page."
  [{:keys [cases start-here]}]
  (if (seq start-here)
    (let [by-id (into {} (map (juxt :id identity)) cases)]
      (str "## Start here\n\n"
           "One question each, in the words a reader tends to arrive "
           "with. The full catalog follows.\n\n"
           "| If this is you | Start here |\n|---|---|\n"
           (str/join "\n"
                     (for [row start-here
                           :let [uc (get by-id (:case row))]]
                       (str "| " (:question row) " | ["
                            (:title uc) "](use-cases/" (case-slug uc) ".md) |")))
           "\n\n## Every case\n\n"))
    ""))

(defn render-use-cases-index-md
  "Assembles docs/use-cases.md's full content -- a generated index, one
  line per case, over case data alone (no mermaid needed: the index
  links to each case's own page rather than rendering its diagram
  inline), preceded by the \"Start here\" actor table when the document
  declares one (ADR-0146)."
  [{:keys [cases] :as use-cases}]
  (str "<!-- GENERATED by `make use-cases` from components/corpus/docs/use-cases.edn -- do not hand-edit.\n"
       "     Edit components/corpus/docs/use-cases.edn and regenerate instead. -->\n\n"
       "# Use Cases\n\n"
       "What you can do with this repo, formally: one entry per use case, "
       "each anchored to the resource equations ([dev/notation.md](dev/notation.md)) it "
       "actually composes from `components/corpus/docs/pipeline.edn`'s own built stages. "
       "An `{external: true}` stage in a case's equations names a "
       "black-box component the use case's own author fills in -- code "
       "this repo doesn't implement and makes no claim about; a "
       "`{spider: funnel}` merge node is a union resource "
       "([dev/notation.md](dev/notation.md)) wherever a case's sources genuinely vary. "
       "Maturity here is a per-use-case honesty label distinct from "
       "`README.md`'s per-capability table -- see the header comment of "
       "`components/corpus/docs/use-cases.edn` for what each label means.\n\n"
       "Each case's own page answers **what do I type** as well as what "
       "you get. Every command in a **You type:** strip was run, once, "
       "locally, before it was committed here -- see the commit that "
       "added it for the dated evidence. Where a case has no strip, it "
       "is because this repo genuinely doesn't drive that case end to "
       "end (an `{external: true}` stage is yours to run, or the case "
       "is `planned`); those cases say so rather than showing an "
       "invocation that has never run. Strips use the same "
       "`bin/ehrt ...` convention as [README.md](../README.md)'s "
       "Quickstart, and `$UPPERCASE` names mark values you supply. For "
       "what a flag does see [cli.md](cli.md) (or `ehrt help <group>`), "
       "for operator ids [operators.md](operators.md), for locator "
       "syntax [locators.md](locators.md), for reading a verdict "
       "[judge-calibration.md](judge-calibration.md), and for the shape "
       "of what lands on disk [formats.md](formats.md).\n\n"
       (render-start-here-md use-cases)
       (str/join "\n" (map case->index-line cases))
       "\n"))

(defn cases->pages
  "Pure: pairs every case's own page filename
  (docs/use-cases/<id>.md's own basename) with its fully rendered page
  content, sourcing each case's mermaid text from mermaid-by-id (a map
  of case :id -> already-rendered mermaid text, produced by the
  string-diagram skill's python script). Throws (a programmer-error
  condition, not a result/rejected -- this is generator-time tooling,
  not a runtime capability) if a case's :id is missing from
  mermaid-by-id, so a `make use-cases` run fails loudly on a
  stale/incomplete mermaid set rather than silently omitting a case's
  page."
  [cases mermaid-by-id]
  (mapv (fn [{:keys [id] :as use-case}]
          (if-let [mermaid (get mermaid-by-id id)]
            {:filename (str (case-slug use-case) ".md")
             :content (case->page-md use-case mermaid)}
            (throw (ex-info (str "no mermaid text for use case " id) {:id id}))))
        cases))

;; ---- impure shell (I/O) ----

(defn read-use-cases-edn
  [path]
  (edn/read-string (slurp path)))

(defn write-case-equations!
  "-X-invokable: reads use-cases-edn (default docs/use-cases.edn),
  writes each case's own equations text to out-dir/<id>.txt (one file
  per case, so the Makefile can run the python mermaid step once per
  file, exactly as `make pipeline` already does for the single
  pipeline-wide equations file)."
  [{:keys [use-cases-edn out-dir] :or {use-cases-edn "components/corpus/docs/use-cases.edn"}}]
  (let [{:keys [cases]} (read-use-cases-edn use-cases-edn)]
    (doseq [{:keys [id equations]} cases]
      (spit (str out-dir "/" (name id) ".txt") (str/join "\n" equations)))))

(defn write-use-cases!
  "-X-invokable: assembles docs/use-cases.md (the generated index) and
  docs/use-cases/<id>.md (one standalone page per case) from
  docs/use-cases.edn and the per-case mermaid files already written to
  cases-dir by the Makefile's `use-cases` target (<id>.mermaid per
  case, produced by the string-diagram skill's own python script --
  not this namespace's job, same split as write-pipeline-md!)."
  [{:keys [use-cases-edn cases-dir index-out pages-dir]
    :or {use-cases-edn "components/corpus/docs/use-cases.edn"}}]
  (let [data (read-use-cases-edn use-cases-edn)
        mermaid-by-id (into {} (map (fn [{:keys [id]}]
                                       [id (slurp (str cases-dir "/" (name id) ".mermaid"))]))
                             (:cases data))]
    (kernel/mkdirs! (io/file pages-dir))
    (let [pages (cases->pages (:cases data) mermaid-by-id)]
      (doseq [{:keys [filename content]} pages]
        (spit (str pages-dir "/" filename) content))
      ;; PRUNE (ADR-0155, register L3-9). Writing without pruning let a
      ;; generated page outlive its own source indefinitely, green: drop
      ;; a case from use-cases.edn and the index loses its row, this
      ;; doseq never touches the orphaned page, `git diff --exit-code
      ;; docs/use-cases/` sees no change, and nothing compared the page
      ;; set to the case set. The generator owns the directory's
      ;; contents, so it deletes what it did not write -- only ever
      ;; `.md` files directly in pages-dir, never a subdirectory and
      ;; never another extension.
      ;;
      ;; Listed via `ehrt.kernel.interface/list-files` (result-or-loud,
      ;; ADR-0078), for the same reason `docsgen/parse-adr-dir` is --
      ;; and `io-vocabulary-lint-test` caught this prune's first draft
      ;; doing exactly what it caught that one's. The distinction is
      ;; load-bearing rather than ceremonial here too, in the opposite
      ;; direction: a nil from `.listFiles` is an I/O failure, and
      ;; reading it as an empty directory would silently prune nothing
      ;; while reporting success -- the orphan class this fix closes,
      ;; reintroduced by the fix itself.
      (let [written (set (map :filename pages))
            r (kernel/list-files pages-dir)]
        (when-not (kernel/ok? r)
          (throw (ex-info (str "failed to list " pages-dir
                               " -- refusing to report a prune that never ran")
                          r)))
        (doseq [^java.io.File f (:payload r)]
          (let [n (.getName f)]
            (when (and (.isFile f)
                       (str/ends-with? n ".md")
                       (not (contains? written n)))
              (io/delete-file f))))))
    (spit index-out (render-use-cases-index-md data))))
