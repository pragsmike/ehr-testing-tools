(ns ehrt.tools.usecases
  "Schema and renderer for the use-cases catalog's own data
  (docs/use-cases.edn, P6) -- a sibling to ehrt.tools.pipeline
  (docs/pipeline.edn), same split: this namespace validates the
  authored EDN and assembles already-rendered equations/mermaid text
  into docs/use-cases.md (`make use-cases`); it has nothing to do with
  actually running the string-diagram skill's python mermaid step,
  same as pipeline.clj's own render-pipeline-md.

  Unlike docs/pipeline.edn's :stages (full Malli-schema'd Stage
  records, one equation each), a use case's :equations is a vector of
  already-formatted equation-line strings -- most use cases reuse
  several of docs/pipeline.edn's own named stages verbatim (Generate,
  Mutate, Gate, ...) in one short sequence, so re-declaring each
  stage's full :kind/:status/:laws record per use case would be pure
  duplication; the equation-line text itself (the same grammar
  docs/notation.md defines) is the right unit here."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
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

(def UseCases
  [:map
   [:schema-version [:= 1]]
   [:cases [:vector UseCase]]])

(defn valid-use-case?
  [use-case]
  (m/validate UseCase use-case))

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

(defn case->markdown-section
  "Renders one use case as a markdown section: title, the narrative
  fields (audience/bring/get/maturity), the runnable strip (or its
  honest stub), the raw equations block, and the case's own
  already-rendered mermaid diagram. The strip sits above the equations
  rather than below the diagram: it is the most copy-pasted surface
  this page carries (AUTHORS-GUIDE.md section 6), and the equations
  are the formal grounding underneath it, not a preamble to it."
  [{:keys [title audience bring get maturity equations] :as use-case} mermaid-text]
  (str "## " title "\n\n"
       "**Audience:** " audience "\n\n"
       "**You bring:** " bring "\n\n"
       "**You get:** " get "\n\n"
       "**Maturity:** " (name maturity) "\n\n"
       (case->commands-block use-case) "\n"
       "```\n" (str/join "\n" equations) "\n```\n\n"
       "```mermaid\n" (str/trim mermaid-text) "\n```\n"))

(defn render-use-cases-md
  "Assembles docs/use-cases.md's full content from use-cases-data
  (docs/use-cases.edn, already read) and mermaid-by-id (a map of case
  :id -> that case's already-rendered mermaid text, produced by the
  string-diagram skill's python script, one small diagram per case).
  Throws (a programmer-error condition, not a result/rejected -- this
  is generator-time tooling, not a runtime capability) if a case's
  :id is missing from mermaid-by-id, so a `make use-cases` run fails
  loudly on a stale/incomplete mermaid set rather than silently
  omitting a case's diagram."
  [{:keys [cases]} mermaid-by-id]
  (str "<!-- GENERATED by `make use-cases` from components/tools/docs/use-cases.edn -- do not hand-edit.\n"
       "     Edit components/tools/docs/use-cases.edn and regenerate instead. -->\n\n"
       "# Use Cases\n\n"
       "What you can do with this repo, formally: one entry per use case, "
       "each anchored to the resource equations ([dev/notation.md](dev/notation.md)) it "
       "actually composes from `components/tools/docs/pipeline.edn`'s own built stages. "
       "An `{external: true}` stage in a case's equations names a "
       "black-box component the use case's own author fills in -- code "
       "this repo doesn't implement and makes no claim about; a "
       "`{spider: funnel}` merge node is a union resource "
       "([dev/notation.md](dev/notation.md)) wherever a case's sources genuinely vary. "
       "Maturity here is a per-use-case honesty label distinct from "
       "`README.md`'s per-capability table -- see the header comment of "
       "`components/tools/docs/use-cases.edn` for what each label means.\n\n"
       "Each case answers **what do I type** as well as what you get. "
       "Every command in a **You type:** strip was run, once, locally, "
       "before it was committed here -- see the commit that added it "
       "for the dated evidence. Where a case has no strip, it is "
       "because this repo genuinely doesn't drive that case end to end "
       "(an `{external: true}` stage is yours to run, or the case is "
       "`planned`); those cases say so rather than showing an "
       "invocation that has never run. Strips use the same "
       "`bin/ehrt ...` convention as [README.md](../README.md)'s "
       "Quickstart, and `$UPPERCASE` names mark values you supply. For "
       "what a flag does see [cli.md](cli.md) (or `ehrt help <group>`), "
       "for operator ids [operators.md](operators.md), for locator "
       "syntax [locators.md](locators.md), for reading a verdict "
       "[judge-calibration.md](judge-calibration.md), and for the shape "
       "of what lands on disk [formats.md](formats.md).\n\n"
       (str/join "\n" (map (fn [{:keys [id] :as use-case}]
                              (if-let [mermaid (get mermaid-by-id id)]
                                (case->markdown-section use-case mermaid)
                                (throw (ex-info (str "no mermaid text for use case " id) {:id id}))))
                            cases))))

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
  [{:keys [use-cases-edn out-dir] :or {use-cases-edn "components/tools/docs/use-cases.edn"}}]
  (let [{:keys [cases]} (read-use-cases-edn use-cases-edn)]
    (doseq [{:keys [id equations]} cases]
      (spit (str out-dir "/" (name id) ".txt") (str/join "\n" equations)))))

(defn write-use-cases-md!
  "-X-invokable: assembles docs/use-cases.md from docs/use-cases.edn
  and the per-case mermaid files already written to cases-dir by the
  Makefile's `use-cases` target (<id>.mermaid per case, produced by
  the string-diagram skill's own python script -- not this
  namespace's job, same split as write-pipeline-md!)."
  [{:keys [use-cases-edn cases-dir out] :or {use-cases-edn "components/tools/docs/use-cases.edn"}}]
  (let [data (read-use-cases-edn use-cases-edn)
        mermaid-by-id (into {} (map (fn [{:keys [id]}]
                                       [id (slurp (str cases-dir "/" (name id) ".mermaid"))]))
                             (:cases data))]
    (spit out (render-use-cases-md data mermaid-by-id))))
