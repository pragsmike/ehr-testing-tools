(ns ehrt.tools.pipeline
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

(def signature-edn-path "components/tools/docs/signature.edn")

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
  [{:keys [label inputs outputs catalytic]}]
  (let [lhs (str/join " × " (concat inputs catalytic))
        rhs (str/join " + " outputs)
        base (str lhs " → " rhs "  [" label "]")]
    (if (seq catalytic)
      (str base "  {catalytic: " (str/join ", " catalytic) "}")
      base)))

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
  (str "<!-- GENERATED by `make pipeline` from components/tools/docs/pipeline.edn -- do not hand-edit.\n"
       "     Edit components/tools/docs/pipeline.edn and regenerate instead. -->\n\n"
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
       "[components/tools/docs/pipeline.edn](../../components/tools/docs/pipeline.edn). "
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
  [{:keys [pipeline-edn out] :or {pipeline-edn "components/tools/docs/pipeline.edn"}}]
  (spit out (pipeline->equations-text (read-pipeline-edn pipeline-edn))))

(defn write-pipeline-md!
  "-X-invokable: assembles docs/pipeline.md from the equations text and
  mermaid text already written to disk by the Makefile's `pipeline`
  target (the mermaid rendering step is the string-diagram skill's own
  Python script, not this namespace's job)."
  [{:keys [equations-txt mermaid out]}]
  (spit out (render-pipeline-md {:equations-text (slurp equations-txt)
                                  :mermaid-text (slurp mermaid)})))
