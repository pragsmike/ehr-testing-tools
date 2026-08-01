(ns ehrt.docs-tooling.interface
  "Thin, deliberately narrow (AR-1, docs-tooling extraction, 2026-07-31,
  refactoring-review stage 1): re-exports exactly what a sibling brick
  calls from outside this component's own namespaces -- one entry.
  `bases/cli/help.clj`'s own `write-cli-md!` wrapper is the one real,
  live caller (not a grep false positive) -- it calls this directly,
  never through the retired tools façade; see ADR-0016 on the
  circular-dependency finding that ruled a relay out. Every
  other -X-invokable entry point this component carries
  (write-equations-txt!/write-pipeline-md!/write-case-equations!/
  write-use-cases-md!/quickstart-fresh!/lint-pipeline!) is invoked
  directly by the Makefile via `-X`, never `:require`d cross-brick, so
  none of them need an interface export -- `-X` addresses a namespace
  on the classpath directly, regardless of which brick it lives in;
  Polylith's own interface-boundary enforcement (`poly check`) applies
  to compile-time `:require`s, not `-X` invocations."
  (:require [ehrt.docs-tooling.docsgen :as docsgen]))

(def write-cli-md! docsgen/write-cli-md!)
