(ns ehrt.corpus-io.interface
  "The transport/IO seam of the former corpus mega-component (ADR-0017
  stage 2, 2026-07-31, `notes/2026-07-30-refactoring-review.md`
  §5.1a): sources, sinks, spooling, framing codecs, and wire-level
  wrappers (v2 ER7 delimiter grammar, operation-manifest lineage) --
  no domain logic (intake/mutate/generate/operators stayed in
  `tools`). Deliberately thin, per this stage's own author ruling
  (AR-1): exports exactly what `tools`' domain namespaces and
  `bases/cli` call from outside this component, not an
  interface-design ideal.

  Directional rule (AR-2, the one that matters more than the file
  list): this component may NEVER require `ehrt.corpus.*` (né `ehrt.tools.*`),
  `ehrt.docs-tooling.*`, or any judge component -- the domain
  implements or consumes this component's protocols/constructors, never
  the reverse. Two real edges into the domain's generator registry
  were found during characterization and resolved by keeping the
  domain-touching code behind in `ehrt.corpus.generator-source`
  rather than routing it through here (author-ruled, both
  escalations): the generator-kind Source constructor (`source-sink`'s
  own `generator-source`, relocated whole) and the generator-URL
  parsing branch (`source-sink-url`'s `finish-source`/
  `parse-source-designator`, relocated whole). `parse-designator`
  below is the shared grammar skeleton that split enabled -- it's
  public and re-exported here specifically so
  `ehrt.corpus.generator-source` can supply its own
  domain-aware `finish` callback without this component ever
  depending on tools."
  (:require [ehrt.corpus-io.framing :as framing]
            [ehrt.corpus-io.er7 :as er7]
            [ehrt.corpus-io.er7-fields :as er7-fields]
            [ehrt.corpus-io.batch :as batch]
            [ehrt.corpus-io.spool-source :as spool-source]
            [ehrt.corpus-io.source-sink :as source-sink]
            [ehrt.corpus-io.source-sink-url :as source-sink-url]
            [ehrt.corpus-io.sink-write :as sink-write]
            [ehrt.corpus-io.operation-manifest :as operation-manifest]
            [ehrt.corpus-io.canonicalizers :as canonicalizers]))

;; corpus-io.framing
(def decode framing/decode)
(def encode framing/encode)
(def lookup framing/lookup)

;; corpus-io.er7 (the v2 mutation substrate, P7 -- mutate/operators'
;; own domain logic stayed in tools, this is only the delimiter-split
;; codec they call)
(def parse er7/parse)
(def serialize er7/serialize)
(def content-hash er7/content-hash)
(def field-index er7/field-index)
(def segment-occurrence-index er7/segment-occurrence-index)
(def resolve-locator er7/resolve-locator)

;; corpus-io.er7-fields (ADR-0111 move-don't-improve micro-relocation --
;; moved down from ehrt.corpus.player, which re-exports these same four
;; names unchanged; ehrt.corpus-io.batch/partition-messages is the new
;; in-component caller this move exists for)
(def parse-dtm-lenient er7-fields/parse-dtm-lenient)
(def message-timestamp-ms er7-fields/message-timestamp-ms)
(def message-type-trigger er7-fields/message-type-trigger)
(def message-patient-id er7-fields/message-patient-id)

;; corpus-io.batch (ADR-0111): the corpus batcher's own pure partition
;; fn -- messages -> epoch-aligned, schedule-partitioned buckets.
(def partition-messages batch/partition-messages)

;; corpus-io.spool-source (collided with corpus.generator-source on
;; `resolve!` back when both lived in one component, ADR-0002 -- kept
;; qualified here for continuity even though the collision itself
;; dissolved this stage: generator-source's own `resolve!` now lives
;; in a different component entirely)
(def spool-resolve! spool-source/resolve!)

;; corpus-io.source-sink
(def default-framing source-sink/default-framing)
(def implemented-source-kinds source-sink/implemented-source-kinds)
(def implemented-sink-kinds source-sink/implemented-sink-kinds)
(def dir-source source-sink/dir-source)
(def file-source source-sink/file-source)
(def stdin-source source-sink/stdin-source)
(def dir-sink source-sink/dir-sink)
(def file-sink source-sink/file-sink)
(def stdout-sink source-sink/stdout-sink)

;; corpus-io.source-sink-url. `parse-designator` is the shared parse
;; skeleton (see namespace docstring) -- its own cross-brick caller is
;; ehrt.corpus.generator-source/parse-source-designator.
(def parse-designator source-sink-url/parse-designator)
(def source-schemes source-sink-url/source-schemes)
(def parse-sink-designator source-sink-url/parse-sink-designator)
(def path-designator->path source-sink-url/path-designator->path)
;; print-source-designator has no domain edge (only :dir/:file are
;; printable, SS-1/SS-2) but is a real cross-brick caller:
;; ehrt.corpus.generator-source-test's own round-trip property
;; test pairs it with that namespace's parse-source-designator.
(def print-source-designator source-sink-url/print-source-designator)

;; corpus-io.sink-write
(def write-dir! sink-write/write-dir!)
(def write-stdout! sink-write/write-stdout!)

;; corpus-io.operation-manifest (the sink-write lineage sidecar --
;; corpus-io stage 2, 2026-07-31: moved here from tools, since it has
;; no domain edges of its own and sink-write, its most demanding
;; consumer, is transport, not domain; ehrt.corpus.intake's
;; own manifest-sidecar recognizer is the one domain consumer,
;; repointed here per AR-4)
(def OperationManifestV1 operation-manifest/OperationManifestV1)
(def operation-manifest-valid? operation-manifest/valid?)

;; corpus-io.canonicalizers (real cross-brick caller:
;; projects/integration's zero-flag-reproducibility test; the
;; kernel/register! load-time side effect this namespace's own
;; docstring describes fires transitively the moment this interface
;; loads, same discipline as the framing/check-schemas lookups
;; docs-tooling.lint depends on)
(def strip-run-timestamp-suffix canonicalizers/strip-run-timestamp-suffix)
(def strip-synthea-run-metadata canonicalizers/strip-synthea-run-metadata)
