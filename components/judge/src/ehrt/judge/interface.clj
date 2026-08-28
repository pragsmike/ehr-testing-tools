(ns ehrt.judge.interface
  "Judge's own verdict vocabulary (ADR-0011, the per-engine judge
  split): Report/build-report/diff-reports/baseline-relative-report
  (ehrt.judge.report) and finding-valid?/worst-of (ehrt.judge.finding).
  The gate functions themselves moved to ehrt.judge-v2-hapi.interface
  and ehrt.judge-fhir-official.interface -- this interface no longer
  re-exports them; every consumer (bases/cli, the project test trees)
  requires each engine's own interface directly since stage 3 retired
  the tools facade's qualified relays (ADR-0018).

  report-valid?/finding-valid? were already qualified before this split
  (ADR-0008) -- they collide with EACH OTHER (not with anything that
  just left), since result/valid?, their original collision partner,
  left this component entirely at the kernel/judge extraction, before
  this session.

  worst-of and the verdict-cache-* functions are NEW re-exports this
  session added, found necessary only by actually running `poly check`
  after the move (ADR-0011's own deviation record): judge-fhir-official.fhir
  genuinely calls ehrt.judge.finding/worst-of and four
  ehrt.judge.verdict-cache functions, which is exactly the kind of
  cross-brick internal-namespace reach Polylith's own interface
  enforcement forbids now that fhir lives in a different brick than
  finding/verdict-cache -- legal before this split (same brick), illegal
  after, fixed by routing through this interface instead of narrowing
  the call away. No collision with any existing export; left
  unqualified.

  ehrt.judge.pairing (ADR-0088, the pairing-as-data registry, AR-PD-1)
  is the newest addition -- no collision with anything above; left
  unqualified too."
  (:require [ehrt.judge.report :as report]
            [ehrt.judge.finding :as finding]
            [ehrt.judge.verdict-cache :as verdict-cache]
            [ehrt.judge.pairing :as pairing]
            [ehrt.judge.sampling :as sampling]))

;; judge.report (collides with judge.finding on valid? -- qualified report-*)
(def Report report/Report)
(def baseline-relative-report report/baseline-relative-report)
(def build-report report/build-report)
(def diff-reports report/diff-reports)
(def report-valid? report/valid?)

;; judge.finding (collides with judge.report on valid? -- qualified finding-*)
(def finding-valid? finding/valid?)
(def worst-of finding/worst-of)

;; judge.verdict-cache (ADR-0011: judge-fhir-official's own cross-brick
;; caller, found by poly check after the move -- see docstring above)
(def verdict-cache-key verdict-cache/cache-key)
(def verdict-cache-lookup verdict-cache/lookup)
(def verdict-cache-store! verdict-cache/store!)
(def verdict-cache-default-dir verdict-cache/default-cache-dir)

;; judge.pairing (ADR-0088, pairing-as-data): the mutate<->judge
;; witnessed-row registry. No collision with anything above.
(def PairingRow pairing/PairingRow)
(def PairingRegistry pairing/Registry)
(def PairingJudgeId pairing/JudgeId)
(def load-pairing-registry pairing/load-registry)
(def pairing-coverage pairing/coverage)

;; --- ARC 4 SWEEP 2 (ADR-0175 design (h), ruling D1): gating at scale ------
;; A PURE selection over corpus metadata, exported for `bases/cli`'s own
;; `gate v2 --sample-add-ons`. The classification set is the CALLER's,
;; which is what keeps this component free of any dependency on the
;; emitter whose registry defines it.
(def sampling-unknown-stratum sampling/unknown-stratum)
(defn sampling-header [content] (sampling/header content))
(defn stratified-selection [entries opts] (sampling/stratified-selection entries opts))
(defn render-strata [strata] (sampling/render-strata strata))
