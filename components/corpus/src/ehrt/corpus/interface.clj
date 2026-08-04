(ns ehrt.corpus.interface
  "The corpus domain's interface (ADR-0018, tools split stage 3,
  2026-07-31): generate, mutate, intake, check, compare, display, and
  play synthetic clinical corpora, plus the operator/generator
  registries those capabilities are parameterized by.

  Designed from live consumers, not inherited (AR-2): every def below
  has a named caller in bases/cli, components/docs-tooling (lint's
  registry lookups), or a project test tree -- the classification
  table, def by def, is ADR-0018's own design rationale. The former
  `ehrt.tools.interface`'s 64 defs became 38 here: its 25 relay
  re-exports of kernel/judge/judge-engine entries dissolved (their
  consumers now require `ehrt.kernel.interface`,
  `ehrt.judge.interface`, and the three engine interfaces directly --
  this component no longer requires any judge engine at all), and
  `Assertion` (zero live consumers; `check-corpus`'s own docstring
  documents the assertion shape) was deleted with grep evidence.

  Naming (AR-2's sanctioned improvement -- names, never signatures):
  the two live registries export with symmetric noun prefixes instead
  of the collision-driven residue the façade carried (`operator-*` for
  corpus.operators' entries/lookup/register!/snapshot/reset,
  `generator-*` for corpus.generators' lookup/register!/resolve-params
  -- the old bare `lookup`/`entries`/`register!` meant \"operators\"
  only because that registry won a name collision, ADR-0002), and
  generator-source's bare `resolve!` (unqualified for the same
  historical reason, its spool twin having moved to corpus-io at
  ADR-0017) is now `resolve-generator-source!`. Everything else keeps
  the name its own ADR gave it.

  Defs kept solely for project/base test suites are marked
  \"test-consumer only\" below (AR-2's ruled disposition: keep, but
  say so) -- they are contract surface for the conformance/integration
  lanes, not CLI wiring.

  Not exported, deliberately: `diff`, `lineage`, and the check/mutate
  internals (component-internal); `operators-doc` (Makefile `-X` entry
  point, same rule as docs-tooling's own -X-invokables); `Assertion`
  (deleted, above). The sim adapter (`ehrt.corpus.sim-adapter`,
  renamed from `tools.sim` -- AR-1: the old name collided confusingly
  with the `sim` component itself) exports `run!`, `check!`,
  `identifiers!`, and `version!`, below, as `sim-run!`/`sim-check!`/
  `sim-identifiers!`/`sim-version!` (the latter three added P3-6,
  2026-08-01, mounting `ehrt sim check`/`ehrt sim identifiers`/`ehrt
  sim version` -- a parity gap found ahead of the sim-cli retirement
  review, see notes/facts-register.md F2)."
  (:require [ehrt.corpus.check :as check]
            [ehrt.corpus.check.schemas :as schemas]
            [ehrt.corpus.display :as display]
            [ehrt.corpus.generate :as generate]
            [ehrt.corpus.generator-source :as generator-source]
            [ehrt.corpus.generators :as generators]
            [ehrt.corpus.golden-comparison :as golden-comparison]
            [ehrt.corpus.intake :as intake]
            [ehrt.corpus.mutate :as mutate]
            [ehrt.corpus.operators :as operators]
            [ehrt.corpus.player :as player]
            [ehrt.corpus.sim-adapter :as sim-adapter]
            [ehrt.provenance.interface :as provenance]))

;; ---- generate ----
(def generate! generate/generate!)
(def jdk-name generate/jdk-name)
(def jdk-version generate/jdk-version)
(def resolve-java-bin generate/resolve-java-bin)
;; ADR-0015: shared by generate! and ehrt.cli.core/generate-sim-command,
;; so every generator source's own :out-dir-exists guard is the same
;; check and the same :hint text.
(def out-dir-exists? generate/non-empty-existing-dir?)
(def out-dir-exists-error generate/out-dir-exists-error)

;; ---- the generator registry (corpus.generators) ----
(def generator-lookup generators/lookup)
(def generator-register! generators/register!) ; test-consumer only (bases/cli tests)
(def generator-resolve-params generators/resolve-params)

;; ---- generator sources (corpus.generator-source) ----
;; resolve-generator-source! executes a generator engine and yields a
;; dir Source; parse-source-designator is the URL entry point whose
;; generator branch lives here rather than corpus-io (ADR-0017's own
;; seam ruling -- name and behavior unchanged since that stage).
(def resolve-generator-source! generator-source/resolve!)
(def parse-source-designator generator-source/parse-source-designator)

;; ---- intake ----
(def intake! intake/intake!)
(def intake-via-source! intake/intake-via-source!)
(def sniff-format intake/sniff-format)
(def valid-catalog-entry? intake/valid-catalog-entry?)   ; test-consumer only (conformance)
(def valid-intake-record? intake/valid-intake-record?)   ; test-consumer only (conformance)

;; ---- mutate ----
(def mutate mutate/mutate)

;; ---- the operator registry (corpus.operators) ----
(def operator-entries operators/entries)
(def operator-lookup operators/lookup)
(def operator-register! operators/register!)             ; test-consumer only (bases/cli tests)
(def operator-registry-snapshot operators/registry-snapshot) ; test-consumer only (bases/cli tests)
(def operator-registry-reset! operators/reset-registry!)     ; test-consumer only (bases/cli tests)

;; ---- manifest ----
;; Repointed to provenance directly (sim split B, M1 step 2, 2026-08-04):
;; ehrt.provenance.interface is the schema's real home now; this
;; interface names that dependency explicitly rather than relaying
;; through ehrt.corpus.manifest (which keeps its own relay too, for
;; its builder-side callers -- see that namespace's own docstring).
(def ManifestV1_1 provenance/ManifestV1_1)                 ; test-consumer only (conformance)

;; ---- check ----
(def check-corpus check/check-corpus)
;; docs-tooling.lint's target-4 (in-repo registry) verification needs
;; this registry's lookup from outside the component (stage 1's own
;; edge, unchanged in direction here: docs-tooling -> corpus).
(def check-schemas-lookup schemas/lookup)

;; ---- golden comparison ----
(def compare-catalogs golden-comparison/compare-catalogs) ; test-consumer only (integration)

;; ---- display (ADR-0013, `ehrt show`): pretty rendering for eyes,
;; never wire format -- see that namespace's own docstring. ----
(def render-er7-message display/render-er7-message)
(def render-er7-stream display/render-er7-stream)
(def render-fhir-json display/render-fhir-json)
;; display's own input-adapter seam, reused by ehrt play (ADR-0014) --
;; not a second splitter.
(def split-er7-multi display/split-er7-multi)

;; ---- player (ADR-0014, `ehrt play`): the pure pacing core -- no
;; clock, no IO -- see that namespace's own docstring. ----
(def default-rate player/default-rate)
(def default-idle-cap-ms player/default-idle-cap-ms)
(def plan player/plan)
(def message-timestamp-ms player/message-timestamp-ms)
(def message-type-trigger player/message-type-trigger)
(def message-patient-id player/message-patient-id)
(def frame-event player/frame-event)

;; ---- the sim adapter (ADR-0005: in-process since 2026-07-28) ----
(def sim-run! sim-adapter/run!)
(def sim-check! sim-adapter/check!)               ; P3-6 parity mount (2026-08-01)
(def sim-identifiers! sim-adapter/identifiers!)    ; P3-6 parity mount (2026-08-01)
(def sim-version! sim-adapter/version!)            ; P3-6 parity mount (2026-08-01)
