(ns ehrt.tools.interface
  "Deliberately wide (H2 landing session ruling R13, notes/ADRs.md
  ADR-0002 -- same discipline as ehrt.sim.interface, ADR-0001 R5).
  Re-exports exactly what bases/ehr-cli and projects/conformance/test
  call from outside their own namespace -- determined by grep against
  the pre-carve ehr-testing-tools repo, not by interface-design
  judgment. Narrowing this surface (splitting components/tools into
  judge/corpus/foundation bricks, per named hole H4 and the deferred
  judge-vs-corpus foundation-extraction question) is a future,
  author-ruled extraction session's call -- see AGENTS.md's
  fat-component disclosure. Don't treat this file's width as evidence
  about how components/tools should be decomposed.

  Five short names collided across two source namespaces each
  (`gate-file`/`gate-dir` in both judge.fhir and judge.v2, `lookup`/
  `register!` in both corpus.operators and corpus.generators, `valid?`
  in both judge.report and result, `resolve!` in both
  corpus.generator-source and corpus.spool-source) -- each pair is
  qualified below (fhir-/v2- prefix, generators- prefix, report-
  prefix, spool- prefix) rather than picking one winner silently;
  every caller of the qualified half was updated at its call site in
  the same commit. Two more names (`resolve`, `run!`) don't collide
  with each other but each shadows a clojure.core name, which loads
  fine but prints a WARNING on every namespace load -- polluting real
  `bin/ehr` output; qualified as `resolve-artifact`/`sim-run!`
  instead, callers updated to match."
  (:require [ehrt.tools.result :as result]
            [ehrt.tools.artifact :as artifact]
            [ehrt.tools.digest :as digest]
            [ehrt.tools.locator :as locator]
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
            [ehrt.tools.judge.v2 :as judge-v2]
            [ehrt.tools.judge.fhir :as judge-fhir]
            [ehrt.tools.judge.report :as judge-report]))

;; result
(def ok result/ok)
(def ok? result/ok?)
(def rejected result/rejected)
(def rejected? result/rejected?)
(def error result/error)
(def error? result/error?)
(def valid? result/valid?)

;; artifact
(def fetch artifact/fetch)
(def read-lockfile artifact/read-lockfile)
(def resolve-artifact artifact/resolve)

;; digest
(def sha256-file digest/sha256-file)

;; locator
(def make locator/make)

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

;; judge.v2 (collides with judge.fhir on gate-file/gate-dir -- qualified v2-*)
(def v2-gate-file judge-v2/gate-file)
(def v2-gate-dir judge-v2/gate-dir)

;; judge.fhir (collides with judge.v2 on gate-file/gate-dir -- qualified fhir-*)
(def fhir-gate-file judge-fhir/gate-file)
(def fhir-gate-dir judge-fhir/gate-dir)
(def fhir-gate-batch judge-fhir/gate-batch)

;; judge.report (collides with result on valid? -- qualified report-*)
(def Report judge-report/Report)
(def baseline-relative-report judge-report/baseline-relative-report)
(def build-report judge-report/build-report)
(def diff-reports judge-report/diff-reports)
(def report-valid? judge-report/valid?)

;; docsgen -- write-cli-md! is the one docsgen entry point a base needs
;; cross-brick (bases/ehr-cli owns the real cli-spec, ADR-0002's own
;; deviation record on why docsgen can no longer require cli.help
;; directly; the discipline-parity session's own docsgen-regen restoration
;; is the first live caller). write-operators-md!/write-equations-txt!/
;; write-pipeline-md!/write-case-equations!/write-use-cases-md! are all
;; invoked directly via `-X` from the Makefile, not required in source,
;; so they don't need an interface export.
(def write-cli-md! docsgen/write-cli-md!)
