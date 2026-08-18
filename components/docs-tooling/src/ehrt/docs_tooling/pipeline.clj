(ns ehrt.docs-tooling.pipeline
  "Schema for the pipeline's own resource-equation data
  (docs/pipeline.edn, docs/notation.md, pattern nursery #13) -- the
  notation eats the repo's own dogfood: docs/pipeline.edn is authored
  EDN, and this namespace is what validates it. `docs/pipeline.md` is
  generated *from* docs/pipeline.edn via the string-diagram skill
  (`make pipeline`); this namespace has nothing to do with that
  rendering step, only with the source data's shape.

  The generic signature-loading machinery -- equation-EDN loading, the
  Stage/UnionResource/ExternalStage schema shapes, validation plumbing
  -- lives in palgebra.signature (design D13; claimed R2,
  `.agents/plans/judge-gate-refactor.md` Phase 2). This namespace
  supplies the one piece of signature data that's specific to this
  repo's pipeline: the five stage kinds, loaded from
  docs/signature.edn rather than hardcoded -- D13 arriving,
  instantiating the language means authoring data."
  (:require [clojure.string :as str]
            [ehrt.palgebra.interface :as signature]))

(def signature-edn-path "components/corpus/docs/signature.edn")

(def stage-kinds
  "The five stage kinds (docs/notation.md, docs/signature.edn) --
  every stage in this repo's pipeline is exactly one of these."
  (:kinds (signature/read-signature-edn signature-edn-path)))

(def Stage (signature/stage-schema stage-kinds))
(def UnionResource signature/UnionResource)
(def ExternalStage signature/ExternalStage)
(def Pipeline (signature/pipeline-schema stage-kinds))

(defn valid-stage?
  [stage]
  (signature/valid-stage? stage-kinds stage))

(defn valid-union-resource?
  [resource]
  (signature/valid-union-resource? resource))

(defn valid-external-stage?
  [stage]
  (signature/valid-external-stage? stage))

(defn valid?
  [pipeline]
  (signature/valid? stage-kinds pipeline))

;; ---- rendering: docs/pipeline.edn -> the string-diagram skill's
;; equation-line grammar (docs/notation.md's equation form) ----

(defn stage->equation-line
  "Renders one stage as one string-diagram-skill equation line. A
  catalytic resource appears on both sides -- once in the LHS product
  (it's syntactically an input) and once in the {catalytic: ...}
  annotation (semantically unconsumed) -- this duplication is the
  skill's own grammar (see resource_equations_to_mermaid.py's
  parse_annotations), not a copy/paste mistake here."
  [{:keys [label inputs outputs catalytic kind]}]
  (let [lhs (str/join " × " (concat inputs catalytic))
        rhs (str/join " + " outputs)
        ;; A :feedback stage's returning outputs -- those that re-enter
        ;; its own inputs -- carry the skill's {feedback: X→X} wire
        ;; annotation. DERIVED from the equation rather than declared by a
        ;; new schema key: :kind, :inputs and :outputs already hold the
        ;; fact, and a key would put it in two places for an editor to
        ;; keep in step -- the very defect ADR-0152 closes one hop
        ;; upstream. Gated on :kind, not on the name coincidence alone: a
        ;; :transform whose output happens to share an input's name is
        ;; not a loop, and only :kind declares one (docs/notation.md).
        returning (when (= kind :feedback) (filter (set inputs) outputs))]
    (cond-> (str lhs " → " rhs "  [" label "]")
      (seq catalytic) (str "  {catalytic: " (str/join ", " catalytic) "}")
      (seq returning) (str "  {feedback: " (str/join ", " (map #(str % "→" %) returning)) "}"))))

(defn- resource->union-op-label
  "\"datum\" -> \"UnionDatum\" -- PascalCases a hyphenated resource name
  for the synthetic merge operation's own equation label, prefixed
  \"Union\" so it reads as generated, not an authored stage."
  [resource]
  (str "Union" (apply str (map str/capitalize (str/split resource #"-")))))

(defn union-resource->equation-line
  "Renders a union resource as a merge equation, reusing the
  string-diagram skill's existing funnel/spider annotation (many-to-one
  convergence) for the union's merge node rather than inventing new
  diagram machinery for the same shape (docs/notation.md)."
  [{:keys [resource union-of]}]
  (str (str/join " × " union-of) " → " resource
       "  [" (resource->union-op-label resource) "]  {spider: funnel}"))

(defn external-stage->equation-line
  "Renders an external (black-box) stage as a plain equation carrying
  the {external: true} annotation, which the diagram renderer maps to
  a dashed box (docs/notation.md) -- no :kind/:catalytic/:laws to
  render, since ExternalStage has none of those fields."
  [{:keys [label inputs outputs]}]
  (str (str/join " × " inputs) " → " (str/join " + " outputs)
       "  [" label "]  {external: true}"))

(defn pipeline->equations-text
  "Renders every stage in :stages, then every union resource in
  :resources, then every external stage in :external-stages, to the
  skill's equation-line format, one per line. A :planned stage gets a
  leading comment line noting its status -- the skill ignores comment
  lines, so this is purely documentary in the rendered diagram, not a
  distinct visual state. :resources and :external-stages default to
  empty, so a plain {:stages [...]} pipeline (as every test predating
  P6 passes) renders exactly as before."
  [{:keys [stages resources external-stages]}]
  (str/join "\n"
            (concat
             (mapcat (fn [{:keys [status] :as stage}]
                       (cond-> []
                         (= status :planned) (conj (str "# planned: " (:label stage)))
                         true (conj (stage->equation-line stage))))
                     stages)
             (map union-resource->equation-line resources)
             (map external-stage->equation-line external-stages))))

(defn render-pipeline-md
  "Pure assembly of docs/pipeline.md's content from already-rendered
  equations text and already-rendered mermaid text -- no I/O here;
  write-pipeline-md! below is the thin, impure shell around it."
  [{:keys [equations-text mermaid-text]}]
  (str "<!-- GENERATED by `make pipeline` from components/corpus/docs/pipeline.edn -- do not hand-edit.\n"
       "     Edit components/corpus/docs/pipeline.edn and regenerate instead. -->\n\n"
       "# Pipeline\n\n"
       "This is the pipeline this repo builds: a fixed sequence of stages "
       "that turns a Synthea configuration into a mutated, gate-ready "
       "corpus. Each stage below is a resource equation -- inputs "
       "consumed, outputs produced, and a `{catalytic: ...}` set naming "
       "resources the stage uses without consuming (an engine artifact, a "
       "runtime, a versioned operator catalog); see "
       "[notation.md](notation.md) for what that notation means "
       "before reading the equations cold.\n\n"
       "The equations are the source of truth: "
       "[components/corpus/docs/pipeline.edn](../../components/corpus/docs/pipeline.edn). "
       "A stage marked `# planned` below is designed but not yet built -- "
       "its equation and law are fixed, its implementation isn't.\n\n"
       "## Equations\n\n```\n" equations-text "\n```\n\n"
       "## Diagram\n\n```mermaid\n" (str/trim mermaid-text) "\n```\n"))

;; ---- impure shell (I/O) ----

(defn read-pipeline-edn
  [path]
  (signature/read-signature-edn path))

(defn write-equations-txt!
  "-X-invokable: reads pipeline-edn (default docs/pipeline.edn), writes
  its rendered equations text to out."
  [{:keys [pipeline-edn out] :or {pipeline-edn "components/corpus/docs/pipeline.edn"}}]
  (spit out (pipeline->equations-text (read-pipeline-edn pipeline-edn))))

(defn generated-comment-header
  "The house \"generated file -- do not edit\" banner in `#`-comment
  syntax, for an equations file that is itself committed.

  docsgen.clj's own `banner` is not reused: it emits an HTML comment, and
  the string-diagram converter ignores only lines beginning `#`, so an
  HTML banner would parse as equations. It is not generalised with a
  comment-prefix argument either -- that helper serves markdown outputs,
  and parameterizing it for one caller in another namespace buys coupling
  with no second use (ADR-0152).

  EXACTLY FOUR LINES, pinned by test. The converter's `%% Arrow N`
  comments derive from the equations file's own line numbering, so this
  banner's length is load-bearing: change it and every arrow in the
  `.mermaid` and in `sim-theory-diagram.md`'s embedded block renumbers
  (ADR-0135 diagnosed exactly that, off by one)."
  [{:keys [make-target source]}]
  (str "# GENERATED by `make " make-target "` from " source " -- do not hand-edit.\n"
       "# Edit " source " and regenerate; it is the single source of truth.\n"
       "# LINE COUNT IS LOAD-BEARING: the converter's `%% Arrow N` numbering\n"
       "# derives from THIS file's line numbering (ADR-0135, ADR-0152).\n"))

(defn write-sim-theory-equations-txt!
  "-X-invokable: the sim theory's own head hop (ADR-0152). Same rendering
  as `write-equations-txt!`, with the generated banner prepended, because
  unlike `pipeline.edn`'s `target/` scratch output this file is COMMITTED
  and CI freshness-diffs it -- so a reader opening it needs to be told
  not to edit it.

  A wrapper rather than a `:header` option on `write-equations-txt!`:
  the banner is four lines of text, and passing it as an `-X` string
  argument would put multi-line content through shell quoting in the
  Makefile -- the mangling class this repo already bans for commit
  messages. `write-equations-txt!` is untouched, so `pipeline.edn`'s
  own output stays byte-identical by construction."
  [{:keys [pipeline-edn out]
    :or {pipeline-edn "components/sim/docs/sim-theory.edn"
         out "components/sim/docs/sim-theory-equations.txt"}}]
  (spit out (str (generated-comment-header {:make-target "sim-theory" :source pipeline-edn})
                 (pipeline->equations-text (read-pipeline-edn pipeline-edn)))))

(defn write-pipeline-md!
  "-X-invokable: assembles docs/pipeline.md from the equations text and
  mermaid text already written to disk by the Makefile's `pipeline`
  target (the mermaid rendering step is the string-diagram skill's own
  Python script, not this namespace's job)."
  [{:keys [equations-txt mermaid out]}]
  (spit out (render-pipeline-md {:equations-text (slurp equations-txt)
                                  :mermaid-text (slurp mermaid)})))
