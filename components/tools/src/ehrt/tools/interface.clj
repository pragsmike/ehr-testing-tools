(ns ehrt.tools.interface
  "Deliberately wide (H2 landing session ruling R13, notes/ADRs.md
  ADR-0002 -- same discipline as ehrt.sim.interface, ADR-0001 R5).
  Re-exports exactly what bases/cli and projects/conformance/test
  call from outside their own namespace -- determined by grep against
  the pre-carve ehr-testing-tools repo, not by interface-design
  judgment. NARROWED (ADR-0008, named hole H4 closed): result, digest,
  artifact, canonical, locator, and invocation moved to
  `ehrt.kernel.interface`, judge's five namespaces to
  `ehrt.judge.interface` -- this file now re-exports those two
  interfaces' own already-qualified names verbatim rather than reaching
  into their internals, plus whatever's left in this component
  (corpus.*, check, sim, docsgen). Don't treat this file's remaining
  width as evidence about how the rest of `components/tools` should be
  decomposed -- that's still a future, author-ruled call. NARROWED
  AGAIN (ADR-0011, the per-engine judge split): the two gate engines
  now live in their own components (`ehrt.judge-v2-hapi.interface`,
  `ehrt.judge-fhir-official.interface`), each exporting unqualified
  `gate-file`/`gate-dir`(`/gate-batch`) -- this file re-applies its OWN
  `v2-`/`fhir-` qualification at the re-export layer below, unchanged,
  so nothing downstream of this interface sees any difference.
  `ehrt.judge.interface` itself now only carries the verdict vocabulary
  (`Report`, `build-report`, etc.). ADR-0012 adds a third engine sibling,
  `ehrt.judge-v2-nist.interface` (profile-tier, direct NIST engine) --
  re-exported below as `v2-nist-*`, same qualification discipline as
  the other two. Its `make-validator`/`gate-file`/`gate-dir` differ in
  signature from `v2-gate-file`/`v2-gate-dir`: this tier takes a
  validator-state map (from `v2-nist-make-validator`, built once per
  profile bundle and reused across files, since context construction
  dominates cost) rather than a bare path, because a profile bundle
  (Π) is itself an input at this tier, not a fixed dependency.

  Two short names collided across two source namespaces each
  (`lookup`/`register!` in both corpus.operators and corpus.generators,
  `resolve!` in both corpus.generator-source and corpus.spool-source)
  -- each pair is qualified below (generators- prefix, spool- prefix)
  rather than picking one winner silently; every caller of the
  qualified half was updated at its call site in the same commit
  (ADR-0002). `report-valid?` stays qualified here too, even though
  judge.report's own collision partner (`result/valid?`) left this
  component entirely -- the collision now is with THIS namespace's own
  bare `valid?`, re-exported from kernel.interface a few lines above;
  unqualifying it would shadow that def, not resolve a stale problem."
  (:require [ehrt.kernel.interface :as kernel]
            [ehrt.judge.interface :as judge]
            [ehrt.judge-v2-hapi.interface :as judge-v2-hapi]
            [ehrt.judge-fhir-official.interface :as judge-fhir-official]
            [ehrt.judge-v2-nist.interface :as judge-v2-nist]
            [ehrt.tools.check :as check]
            [ehrt.tools.sim :as sim]
            [ehrt.tools.docsgen :as docsgen]
            [ehrt.tools.corpus.canonicalizers :as canonicalizers]
            [ehrt.tools.corpus.golden-comparison :as golden-comparison]
            [ehrt.tools.corpus.generators :as generators]
            [ehrt.tools.corpus.generator-source :as generator-source]
            [ehrt.tools.corpus.spool-source :as spool-source]
            [ehrt.tools.corpus.source-sink :as source-sink]
            [ehrt.tools.corpus.source-sink-url :as source-sink-url]
            [ehrt.tools.corpus.sink-write :as sink-write]
            [ehrt.tools.corpus.intake :as intake]
            [ehrt.tools.corpus.mutate :as mutate]
            [ehrt.tools.corpus.generate :as generate]
            [ehrt.tools.corpus.operators :as operators]
            [ehrt.tools.corpus.manifest :as manifest]
            [ehrt.tools.display :as display]
            [ehrt.tools.player :as player]))

;; kernel.interface (result/digest/artifact/locator, ADR-0008)
(def ok kernel/ok)
(def ok? kernel/ok?)
(def rejected kernel/rejected)
(def rejected? kernel/rejected?)
(def error kernel/error)
(def error? kernel/error?)
(def valid? kernel/valid?)
(def fetch kernel/fetch)
(def read-lockfile kernel/read-lockfile)
(def resolve-artifact kernel/resolve-artifact)
(def sha256-file kernel/sha256-file)
(def make kernel/make)

;; check
(def Assertion check/Assertion)
(def check-corpus check/check-corpus)

;; sim (ehrt.tools' own sim-consumer wrapper -- distinct from the
;; ehrt.sim component itself). ADR-0005: in-process as of 2026-07-28,
;; the ehr-sim-mount fulfillment of ADR-0012; no more available?/
;; default-sim-repo-dir/sim-dir-env-var -- there is nothing left to
;; discover or degrade gracefully without (see ehrt.tools.sim's own
;; docstring).
(def sim-run! sim/run!)

;; corpus.canonicalizers
(def strip-run-timestamp-suffix canonicalizers/strip-run-timestamp-suffix)
(def strip-synthea-run-metadata canonicalizers/strip-synthea-run-metadata)

;; corpus.golden-comparison
(def compare-catalogs golden-comparison/compare-catalogs)

;; corpus.generators (collides with corpus.operators on lookup/register! --
;; qualified generators-*)
(def generators-lookup generators/lookup)
(def generators-register! generators/register!)

;; corpus.generator-source (collides with corpus.spool-source on resolve! --
;; keeps the unqualified name; spool-source's twin is qualified instead)
(def resolve! generator-source/resolve!)

;; corpus.spool-source
(def spool-resolve! spool-source/resolve!)

;; corpus.source-sink
(def default-framing source-sink/default-framing)
(def dir-sink source-sink/dir-sink)
(def dir-source source-sink/dir-source)

;; corpus.source-sink-url
(def parse-sink-designator source-sink-url/parse-sink-designator)
(def parse-source-designator source-sink-url/parse-source-designator)
(def path-designator->path source-sink-url/path-designator->path)

;; corpus.sink-write
(def write-dir! sink-write/write-dir!)
(def write-stdout! sink-write/write-stdout!)

;; corpus.intake
(def intake! intake/intake!)
(def intake-via-source! intake/intake-via-source!)
(def sniff-format intake/sniff-format)
(def valid-catalog-entry? intake/valid-catalog-entry?)
(def valid-intake-record? intake/valid-intake-record?)

;; corpus.mutate
(def mutate mutate/mutate)

;; corpus.generate
(def generate! generate/generate!)
(def jdk-name generate/jdk-name)
(def jdk-version generate/jdk-version)
(def resolve-java-bin generate/resolve-java-bin)

;; corpus.operators (collides with corpus.generators on lookup/register! --
;; keeps the unqualified names; generators' twins are qualified instead)
(def entries operators/entries)
(def lookup operators/lookup)
(def register! operators/register!)
(def registry-snapshot operators/registry-snapshot)
(def reset-registry! operators/reset-registry!)

;; corpus.manifest
(def ManifestV1_1 manifest/ManifestV1_1)

;; judge-v2-hapi.interface / judge-fhir-official.interface (ADR-0011):
;; each exports unqualified gate-file/gate-dir(/gate-batch) now that
;; they live in their own components -- this file re-applies its OWN
;; v2-/fhir- qualification here (the original gate-file/gate-dir
;; collision, ADR-0002/ADR-0008), so every downstream caller of THIS
;; interface sees the exact same names as before the split.
(def v2-gate-file judge-v2-hapi/gate-file)
(def v2-gate-dir judge-v2-hapi/gate-dir)
(def fhir-gate-file judge-fhir-official/gate-file)
(def fhir-gate-dir judge-fhir-official/gate-dir)
(def fhir-gate-batch judge-fhir-official/gate-batch)

;; judge-v2-nist.interface (ADR-0012, third judge engine, profile
;; tier): make-validator/gate-file/gate-dir take a validator-state map
;; (built once per Π bundle), not a bare path -- see this ns's own
;; docstring for why.
(def v2-nist-make-validator judge-v2-nist/make-validator)
(def v2-nist-gate-file judge-v2-nist/gate-file)
(def v2-nist-gate-dir judge-v2-nist/gate-dir)

;; judge.interface (ADR-0008; ADR-0011 narrowed this to vocabulary only)
(def Report judge/Report)
(def baseline-relative-report judge/baseline-relative-report)
(def build-report judge/build-report)
(def diff-reports judge/diff-reports)
(def report-valid? judge/report-valid?)

;; ehrt.tools.display (ADR-0013, `ehrt show`): pretty rendering for
;; eyes, never wire format -- see that namespace's own docstring.
(def render-er7-message display/render-er7-message)
(def render-er7-stream display/render-er7-stream)
(def render-fhir-json display/render-fhir-json)

;; ehrt.tools.player (ADR-0014, `ehrt play`): the pure pacing core --
;; no clock, no IO -- see that namespace's own docstring. No
;; collisions with any existing export here, so no qualification.
(def default-rate player/default-rate)
(def default-idle-cap-ms player/default-idle-cap-ms)
(def plan player/plan)
(def message-timestamp-ms player/message-timestamp-ms)
(def message-type-trigger player/message-type-trigger)
(def message-patient-id player/message-patient-id)
(def frame-event player/frame-event)

;; ehrt.tools.display's own input-adapter seam, reused by ehrt play
;; (ADR-0014) -- not a second splitter.
(def split-er7-multi display/split-er7-multi)

;; docsgen -- write-cli-md! is the one docsgen entry point a base needs
;; cross-brick (bases/cli owns the real cli-spec, ADR-0002's own
;; deviation record on why docsgen can no longer require cli.help
;; directly; the discipline-parity session's own docsgen-regen restoration
;; is the first live caller). write-operators-md!/write-equations-txt!/
;; write-pipeline-md!/write-case-equations!/write-use-cases-md! are all
;; invoked directly via `-X` from the Makefile, not required in source,
;; so they don't need an interface export.
(def write-cli-md! docsgen/write-cli-md!)
