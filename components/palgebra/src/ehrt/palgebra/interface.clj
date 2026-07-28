(ns ehrt.palgebra.interface
  "Deliberately wide (H2 landing session ruling R13/R14, notes/ADRs.md
  ADR-0002). R13's own target layout for components/palgebra named no
  interface.clj, but Polylith's own dependency-direction enforcement
  (poly check: a brick that reaches into another brick's
  implementation namespace, not its interface, fails check) requires
  one the moment palgebra becomes its own component -- components/tools'
  lint.clj and pipeline.clj both genuinely require palgebra (two real
  :require sites, confirmed by grep against the pre-carve repo, not a
  hypothetical). Re-exports exactly what components/tools calls."
  (:require [ehrt.palgebra.lint :as lint]
            [ehrt.palgebra.signature :as signature]))

;; lint
(def stages-catalytic-resources lint/stages-catalytic-resources)
(def lines-catalytic-resources lint/lines-catalytic-resources)
(def lint lint/lint)

;; signature
(def read-signature-edn signature/read-signature-edn)
(def stage-schema signature/stage-schema)
(def UnionResource signature/UnionResource)
(def ExternalStage signature/ExternalStage)
(def pipeline-schema signature/pipeline-schema)
(def valid-stage? signature/valid-stage?)
(def valid-union-resource? signature/valid-union-resource?)
(def valid-external-stage? signature/valid-external-stage?)
(def valid? signature/valid?)
