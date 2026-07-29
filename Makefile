.PHONY: help test integration quickstart ci-parity pipeline use-cases operators-doc cli-doc docsgen

# Thin, deliberately (R23, ADR-0004, 2026-07-28 carve-loss recovery
# session): every target below is a named entry point to a poly/CLI
# command, not logic of its own. The full pre-carve Makefile
# (pack/pack-skills/pack-push, lint-pipeline/lint-deps,
# check-palgebra-drift, coverage, integration-smoke) stays superseded
# per ADR-0002's own author-approved decision -- not reopened here; see
# notes/carve-loss-audit.md for the full disposition of every one of
# its old targets. Docsgen targets restored 2026-07-28
# (discipline-parity session, carve-loss-audit row "docs/*
# regeneration tooling") -- paths adjusted for the Polylith layout,
# cli-doc newly requires ehrt.tools.interface/write-cli-md! + a spec-
# supplying wrapper in bases/ehr-cli (ADR-0002's own deviation record
# on why docsgen can no longer require cli.help directly).

help:
	@echo "Available targets:"
	@echo "  help         - show this message (default target)"
	@echo "  test         - the per-push lane: poly check -- every brick + projects/conformance's own suite, no artifact-fetch machinery (ADR-0004)"
	@echo "  integration  - projects/integration's own suite: real Synthea, real FHIR validator -- requires 'ehr artifact fetch' first, e.g.:"
	@echo "                   bin/ehr artifact fetch --name synthea --version 4.0.0"
	@echo "                   bin/ehr artifact fetch --name temurin-jdk --version 21.0.12+8"
	@echo "                   bin/ehr artifact fetch --name fhir-validator-cli --version 6.9.12"
	@echo "  quickstart   - run README.md's Quickstart commands verbatim (bin/quickstart-demo), asserting each one's exit code and a clean tree afterward"
	@echo "  ci-parity    - fresh clone + cold artifact cache + the per-push lane: 'green as CI sees it', runnable locally (ADR-0004's own local-state-is-not-clone-state lesson)"
	@echo "  pipeline     - regenerate components/tools/docs/pipeline.md from pipeline.edn"
	@echo "  use-cases    - regenerate components/tools/docs/use-cases.md from use-cases.edn"
	@echo "  operators-doc - regenerate components/tools/docs/operators.md from the live operator registry"
	@echo "  cli-doc      - regenerate components/tools/docs/cli.md from bases/ehr-cli's own cli-spec"
	@echo "  docsgen      - all four of the above"

test:
	clojure -M:poly check
	clojure -M:poly test :all skip:integration

integration:
	clojure -M:poly test :all project:integration

quickstart:
	bin/quickstart-demo

# Regenerates components/tools/docs/pipeline.md from pipeline.edn (the
# hand-authored source of truth) -- equations text + mermaid diagram,
# same two-step shape as `use-cases` below.
pipeline:
	@mkdir -p target
	clojure -X:dev ehrt.tools.pipeline/write-equations-txt! :pipeline-edn '"components/tools/docs/pipeline.edn"' :out '"target/pipeline-equations.txt"'
	python3 components/palgebra/tools/resource_equations_to_mermaid.py target/pipeline-equations.txt -o target/pipeline-flow.mermaid
	clojure -X:dev ehrt.tools.pipeline/write-pipeline-md! :pipeline-edn '"components/tools/docs/pipeline.edn"' :equations-txt '"target/pipeline-equations.txt"' :mermaid '"target/pipeline-flow.mermaid"' :out '"components/tools/docs/pipeline.md"'
	@echo "Regenerated components/tools/docs/pipeline.md"

# Regenerates components/tools/docs/use-cases.md from use-cases.edn --
# one equations file + mermaid diagram per named use case.
use-cases:
	@mkdir -p target/use-cases
	clojure -X:dev ehrt.tools.usecases/write-case-equations! :use-cases-edn '"components/tools/docs/use-cases.edn"' :out-dir '"target/use-cases"'
	@for f in target/use-cases/*.txt; do \
		python3 components/palgebra/tools/resource_equations_to_mermaid.py "$$f" -o "$${f%.txt}.mermaid"; \
	done
	clojure -X:dev ehrt.tools.usecases/write-use-cases-md! :use-cases-edn '"components/tools/docs/use-cases.edn"' :cases-dir '"target/use-cases"' :out '"components/tools/docs/use-cases.md"'
	@echo "Regenerated components/tools/docs/use-cases.md"

# Regenerates components/tools/docs/operators.md from the live operator
# registry (requiring corpus.operators populates it at namespace load).
operators-doc:
	clojure -X:dev ehrt.tools.docsgen/write-operators-md! :out '"components/tools/docs/operators.md"'
	@echo "Regenerated components/tools/docs/operators.md"

# Regenerates components/tools/docs/cli.md from bases/ehr-cli's own
# cli-spec, ehrt.tools' docsgen not itself required (Polylith: bases
# depend on components, never the reverse) -- the wrapper lives in
# bases/ehr-cli/src/ehrt/ehr_cli/help.clj.
cli-doc:
	clojure -X:dev ehrt.ehr-cli.help/write-cli-md! :out '"components/tools/docs/cli.md"'
	@echo "Regenerated components/tools/docs/cli.md"

docsgen: pipeline use-cases operators-doc cli-doc

# ADR-0004's own generalized trap, made runnable: index modes, artifact
# caches, and sibling checkouts have each masked a CI failure behind a
# local green (the executable-bit incident, this same session's own
# cold-cache verification above). A real `git clone` (not a working-tree
# copy) into a scratch dir, with the artifact cache pointed at an empty
# directory via EHR_TESTING_TOOLS_CACHE, running exactly the per-push
# lane `test` above runs -- the closest a local machine can get to
# "what CI actually sees" without a real CI run.
ci-parity:
	@echo "== ci-parity: fresh clone + cold artifact cache + the per-push lane =="
	rm -rf /tmp/ehr-testing-ci-parity /tmp/ehr-testing-ci-parity-cache
	git clone --quiet . /tmp/ehr-testing-ci-parity
	mkdir -p /tmp/ehr-testing-ci-parity-cache
	cd /tmp/ehr-testing-ci-parity && \
	  EHR_TESTING_TOOLS_CACHE=/tmp/ehr-testing-ci-parity-cache clojure -M:poly check && \
	  EHR_TESTING_TOOLS_CACHE=/tmp/ehr-testing-ci-parity-cache clojure -M:poly test :all skip:integration
	rm -rf /tmp/ehr-testing-ci-parity /tmp/ehr-testing-ci-parity-cache
	@echo "== ci-parity: green as CI sees it =="
