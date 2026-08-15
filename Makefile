.PHONY: help test integration quickstart quickstart-fresh ci-parity pipeline use-cases operators-doc cli-doc sim-theory palgebra-examples docsgen lint-pipeline mirror-nist verify-nist-lock

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
# cli-doc invokes ehrt.docs-tooling.interface/write-cli-md! through a spec-
# supplying wrapper in bases/cli (ADR-0002's own deviation record
# on why docsgen can no longer require cli.help directly; base renamed
# ehr-cli -> cli, R35, ADR-0009).

help:
	@echo "Available targets:"
	@echo "  help         - show this message (default target)"
	@echo "  test         - the per-push lane: poly check -- every brick + projects/conformance's own suite, no artifact-fetch machinery (ADR-0004)"
	@echo "  integration  - projects/integration's own suite plus bin/demo-exerciser-ed-tuesday and bin/demo-exerciser-clinic-decade (R3, ADR-0120/ADR-0132): real Synthea, real FHIR validator -- requires 'ehrt artifact fetch' first, e.g.:"
	@echo "                   bin/ehrt artifact fetch --name synthea --version 4.0.0"
	@echo "                   bin/ehrt artifact fetch --name temurin-jdk --version 21.0.12+8"
	@echo "                   bin/ehrt artifact fetch --name fhir-validator-cli --version 6.9.12"
	@echo "  quickstart   - run README.md's Quickstart commands verbatim (bin/quickstart-demo), asserting each one's exit code and a clean tree afterward"
	@echo "  quickstart-fresh - assert README.md's Quickstart fence and bin/quickstart-demo teach the identical commands, in the identical order (ehrt.docs-tooling.quickstart-fresh)"
	@echo "  ci-parity    - fresh clone + cold artifact cache + the per-push lane: 'green as CI sees it', runnable locally (ADR-0004's own local-state-is-not-clone-state lesson)"
	@echo "  pipeline     - regenerate docs/dev/pipeline.md from components/corpus/docs/pipeline.edn"
	@echo "  use-cases    - regenerate docs/use-cases.md (index) and docs/use-cases/*.md (per-case pages) from components/corpus/docs/use-cases.edn"
	@echo "  operators-doc - regenerate docs/operators.md from the live operator registry"
	@echo "  cli-doc      - regenerate docs/cli.md from bases/cli's own cli-spec"
	@echo "  sim-theory   - regenerate components/sim/docs/sim-theory-diagram.mermaid from sim-theory-equations.txt and splice it into sim-theory-diagram.md's embedded block"
	@echo "  palgebra-examples - regenerate the three components/palgebra/examples/*-flow*.mermaid from their sibling *-equations.txt"
	@echo "  docsgen      - all six of the above"
	@echo "  lint-pipeline - assert every catalytic resource in docs/pipeline.edn and docs/use-cases.edn resolves to one of the four catalytic targets (ehrt.docs-tooling.lint)"
	@echo "  mirror-nist  - build ~/.ehrt/nist-mirror/ from this user's own ~/.m2 cache, sha256-verified against artifacts.lock.edn (ADR-0053) -- offline determinism without redistribution"
	@echo "  verify-nist-lock - check every hit-nexus-sourced artifacts.lock.edn entry's sha256 against ~/.m2 (ADR-0053); also runs as part of 'test'"

test:
	clojure -M:poly check
	clojure -M:poly test :all skip:integration
	bin/verify-nist-lock

integration:
	clojure -M:poly test :all project:integration
	bin/demo-exerciser-ed-tuesday
	bin/demo-exerciser-clinic-decade
	bin/usecase-judge-tier-calibration
	bin/usecase-profile-tier-v2
	bin/usecase-acceptance-qa
	bin/usecase-regression-baselining
	bin/readme-what-you-get

quickstart:
	bin/quickstart-demo

quickstart-fresh:
	clojure -X:dev ehrt.docs-tooling.quickstart-fresh/quickstart-fresh!

# Regenerates docs/dev/pipeline.md from components/corpus/docs/pipeline.edn
# (the hand-authored source of truth, staying component-adjacent --
# notes/docs-audit.md) -- equations text + mermaid diagram, same
# two-step shape as `use-cases` below. Output moved out of
# components/corpus/docs/ to docs/dev/ (ADR-0010, audience-forked docs).
pipeline:
	@mkdir -p target docs/dev
	clojure -X:dev ehrt.docs-tooling.pipeline/write-equations-txt! :pipeline-edn '"components/corpus/docs/pipeline.edn"' :out '"target/pipeline-equations.txt"'
	python3 components/palgebra/tools/resource_equations_to_mermaid.py target/pipeline-equations.txt -o target/pipeline-flow.mermaid
	clojure -X:dev ehrt.docs-tooling.pipeline/write-pipeline-md! :pipeline-edn '"components/corpus/docs/pipeline.edn"' :equations-txt '"target/pipeline-equations.txt"' :mermaid '"target/pipeline-flow.mermaid"' :out '"docs/dev/pipeline.md"'
	@echo "Regenerated docs/dev/pipeline.md"

# Regenerates docs/use-cases.md (the generated index) and
# docs/use-cases/*.md (one standalone page per case) from
# components/corpus/docs/use-cases.edn (source stays component-
# adjacent) -- one equations file + mermaid diagram per named use
# case. Output moved to docs/ (user path, ADR-0010); split into an
# index plus per-case pages, migration item 14, 2026-08-02.
use-cases:
	@mkdir -p target/use-cases docs/use-cases
	clojure -X:dev ehrt.docs-tooling.usecases/write-case-equations! :use-cases-edn '"components/corpus/docs/use-cases.edn"' :out-dir '"target/use-cases"'
	@for f in target/use-cases/*.txt; do \
		python3 components/palgebra/tools/resource_equations_to_mermaid.py "$$f" -o "$${f%.txt}.mermaid"; \
	done
	clojure -X:dev ehrt.docs-tooling.usecases/write-use-cases! :use-cases-edn '"components/corpus/docs/use-cases.edn"' :cases-dir '"target/use-cases"' :index-out '"docs/use-cases.md"' :pages-dir '"docs/use-cases"'
	@echo "Regenerated docs/use-cases.md and docs/use-cases/*.md"

# Regenerates docs/operators.md (user path, ADR-0010) from the live
# operator registry (requiring corpus.operators populates it at
# namespace load).
operators-doc:
	@mkdir -p docs
	clojure -X:dev ehrt.corpus.operators-doc/write-operators-md! :out '"docs/operators.md"'
	@echo "Regenerated docs/operators.md"

# Regenerates docs/cli.md (user path, ADR-0010) from bases/cli's own
# cli-spec, docs-tooling's docsgen doing the rendering (Polylith: bases
# depend on components, never the reverse) -- the wrapper lives in
# bases/cli/src/ehrt/cli/help.clj.
cli-doc:
	@mkdir -p docs
	clojure -X:dev ehrt.cli.help/write-cli-md! :out '"docs/cli.md"'
	@echo "Regenerated docs/cli.md"

# Regenerates components/sim/docs/sim-theory-diagram.mermaid from
# components/sim/docs/sim-theory-equations.txt AND splices the result
# into sim-theory-diagram.md's embedded ```mermaid block, so all three
# surfaces (equations -> .mermaid -> embedded block) agree byte for
# byte and one `git diff` sees any drift in any of them. Registered
# here by review 3 (D5-3/D5-4/D2-4, ADR-0136): this derivation had run
# by hand from a recipe in the two headers since it was authored, which
# is how ADR-0135's converter change reached it only by a careful
# manual sweep -- and how the palgebra examples below it were missed
# entirely. The hand recipes are retired; those headers now point here.
#
# CAUTION, and the reason the equations file's own header must keep its
# exact line count under edit: the converter's `%% Arrow N` comments
# derive from the equations file's LINE numbering, so adding or
# removing a header comment line silently renumbers every arrow in the
# output (ADR-0135 diagnosed exactly this, off by one).
sim-theory:
	@mkdir -p target
	python3 components/palgebra/tools/resource_equations_to_mermaid.py components/sim/docs/sim-theory-equations.txt -o components/sim/docs/sim-theory-diagram.mermaid
	@awk -v block=components/sim/docs/sim-theory-diagram.mermaid '\
		/^```mermaid$$/ && !spliced { print; while ((getline l < block) > 0) print l; close(block); inb=1; spliced=1; next } \
		inb && /^```$$/ { print; inb=0; next } \
		inb { next } \
		{ print }' components/sim/docs/sim-theory-diagram.md > target/sim-theory-diagram.md
	@cmp -s target/sim-theory-diagram.md components/sim/docs/sim-theory-diagram.md \
		|| cp target/sim-theory-diagram.md components/sim/docs/sim-theory-diagram.md
	@echo "Regenerated components/sim/docs/sim-theory-diagram.mermaid and its embedded block in sim-theory-diagram.md"

# Regenerates the string-diagram skill's three shipped teaching
# examples from their sibling equations files. Registered by review 3
# (D5-4): all three were stale against their own converter, each
# missing ADR-0135's result nodes -- teaching material demonstrating
# precisely the defect that ADR-0135 was chartered to fix.
#
# Only three of the five *-equations.txt in that directory have a
# committed .mermaid output; lemon-pie and decision-monad ship as
# equation sources only (they are the vendoring surface, not the
# rendered-example surface). That is the whole registered population --
# if a fourth example grows a committed .mermaid, it belongs on this
# target and in CI's freshness diff the same day.
palgebra-examples:
	python3 components/palgebra/tools/resource_equations_to_mermaid.py components/palgebra/examples/ai-study-equations.txt -o components/palgebra/examples/ai-study-flow-v3.mermaid
	python3 components/palgebra/tools/resource_equations_to_mermaid.py components/palgebra/examples/committee-equations.txt -o components/palgebra/examples/committee-flow.mermaid
	python3 components/palgebra/tools/resource_equations_to_mermaid.py components/palgebra/examples/deliberated-choice-equations.txt -o components/palgebra/examples/deliberated-choice-flow.mermaid
	@echo "Regenerated components/palgebra/examples/*-flow*.mermaid"

docsgen: pipeline use-cases operators-doc cli-doc sim-theory palgebra-examples

lint-pipeline:
	clojure -X:dev ehrt.docs-tooling.lint/lint-pipeline!

# Offline determinism without redistribution (ADR-0053, AR-F4-1/AR-F4-3):
# ADR-0005's 2026-07-24 amendment (notes/tools/ADRs.md) forecloses
# vendoring the NIST jars into this repo -- these two targets give the
# lockfile's supply-chain claims mechanized teeth instead. `mirror-nist`
# builds a Maven-layout mirror OUTSIDE this repo, on the invoking user's
# own machine, from jars that user already fetched from NIST's own
# official channel; `verify-nist-lock` is wired into `test` above (the
# target where `projects/conformance`'s own classpath resolution --
# it depends on `poly/judge-v2-nist` -- already pulls every one of these
# coordinates into ~/.m2), so a sha256 drift there fails the per-push
# lane. See components/judge-v2-nist/docs/nist-mirror.md.
mirror-nist:
	bin/mirror-nist

verify-nist-lock:
	bin/verify-nist-lock

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
