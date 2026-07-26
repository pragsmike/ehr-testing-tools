.PHONY: help pack pack-skills pack-push test coverage integration ehr pipeline use-cases operators-doc cli-doc lint-pipeline lint-deps quickstart-demo quickstart-fresh

SHELL := bash

REPO_NAME := ehr-testing-tools
PACK_OUTPUT := $(shell echo $$HOME)/ehr-testing-tools-pack.txt
PACK_SKILLS_OUTPUT := $(shell echo $$HOME)/ehr-testing-tools-skills-pack.txt
# Retired as a pack-transport target (2026-07-24, pack transport v2): the
# gist API's rate-limited shared pool made design-channel fetches
# unreliable. The gist itself is left alone -- nothing here deletes it --
# but pack-push no longer publishes to it. Transport is now the
# `pragsmike/packs` repo (below), fetched by the design channel via
# raw.githubusercontent.com, which is CDN-served and doesn't share the
# gist API's rate limit.
GIST_ID := 4fcd1abb4e74a5b54f9c241877edd02a
PACKS_REPO_DIR := $(shell echo $$HOME)/.packs

# Two patterns, no longer one: PACK_SKILLS_PATTERN is what pack-skills
# packs and pack elides; PACK_ELIDE_PATTERN is everything pack elides,
# a strict superset once the vendored corpus joined it below. They
# diverged because the corpus (ADR-0011: vendored data, bytes belong in
# git, not in session context) must leave `pack` but must NOT enter
# `pack-skills` -- provenance (PROVENANCE.md, the vendored LICENSE)
# stays packed either way; only the ER7 bytes themselves are elided.
# Both are EREs matched against `git ls-files` output, anchored to line
# start.
PACK_SKILLS_PATTERN := ^\.agents/skills/|^\.agents/prompts/archive/
PACK_ELIDE_PATTERN  := $(PACK_SKILLS_PATTERN)|^test/fixtures/v2/simhospital/messages\.out$$

help:
	@echo "Available targets:"
	@echo "  help         - show this message (default target)"
	@echo "  pack         - concatenate tracked files (except .agents/skills, .agents/prompts/archive, and test/fixtures/v2/simhospital/messages.out -- corpus bytes; provenance stays packed) into $(PACK_OUTPUT)"
	@echo "  pack-skills  - concatenate only .agents/skills + .agents/prompts/archive into $(PACK_SKILLS_OUTPUT)"
	@echo "  pack-push    - dormant (2026-07-25, not part of the session ritual) - pack + pack-skills, then publish both to the pragsmike/packs repo ($(PACKS_REPO_DIR))"
	@echo "  test         - run the Clojure test suite (clojure -X:test); cold-cache/no-network hermetic"
	@echo "  coverage     - run cloverage and report coverage (clojure -M:coverage); cold-cache/no-network hermetic"
	@echo "  integration  - run the integration test suite (clojure -X:integration); requires"
	@echo "                 'ehr artifact fetch' for synthea, temurin-jdk, and fhir-validator-cli first, e.g.:"
	@echo "                   bin/ehr artifact fetch --name synthea --version 4.0.0"
	@echo "                   bin/ehr artifact fetch --name temurin-jdk --version 17.0.19+10"
	@echo "                   bin/ehr artifact fetch --name fhir-validator-cli --version 6.9.12"
	@echo "  ehr          - compatibility spelling for the CLI; bin/ehr is the entry point (it carries the 0/1/2/3 exit contract, where make reports its own status 2 for any non-zero exit)"
	@echo "                 e.g. bin/ehr artifact fetch --name synthea --version 4.0.0 -- see every command with bin/ehr help"
	@echo "  quickstart-demo - run README.md's Quickstart commands verbatim (bin/quickstart-demo), asserting each"
	@echo "                 one's exit code and a clean tree afterward; fetches real artifacts and runs the real"
	@echo "                 FHIR validator -- integration-tier, not hermetic (DOC-5)"
	@echo "  quickstart-fresh - check README.md's Quickstart fence and bin/quickstart-demo teach the identical"
	@echo "                 commands, in the identical order; cheap, hermetic, fast tier (DOC-5)"
	@echo "  pipeline     - regenerate docs/pipeline.md from docs/pipeline.edn"
	@echo "  use-cases    - regenerate docs/use-cases.md from docs/use-cases.edn"
	@echo "  operators-doc - regenerate docs/operators.md from the mutation-operator registry"
	@echo "  cli-doc      - regenerate docs/cli.md from the CLI help spec (src/ehr_testing_tools/cli/help.clj)"
	@echo "  lint-pipeline - check every catalytic resource in docs/pipeline.edn and docs/use-cases.edn resolves to one of the four catalytic targets (docs/notation.md)"
	@echo "  lint-deps     - check no palgebra.* namespace requires ehr-testing-tools.* (docs/palgebra-design.md D9)"

test:
	clojure -X:test

coverage:
	clojure -M:coverage

# Runs test-integration/ (tests needing real cached artifacts, network,
# or a warm cache -- e.g. judge.fhir's contract-pairing suite). Requires
# the three artifacts named under `help` above to already be fetched;
# see AGENTS.md's hermeticity policy for why these live apart from
# `make test`/`make coverage`.
integration:
	clojure -X:integration

# Runs README.md's Quickstart commands verbatim, under per-step exit-code
# assertions, via bin/quickstart-demo (DOC-5, .agents/plans/user-docs.md).
# Not hermetic -- fetches the same three artifacts.lock.edn artifacts as
# `integration` above and runs the real FHIR validator; belongs in the
# nightly tier (.github/workflows/integration.yml), never per-push.
# `make quickstart-fresh` is the fast, per-push sibling: it only proves
# this script and the README's fence teach the same commands, without
# running either.
quickstart-demo:
	bin/quickstart-demo

# Fast, per-push sibling of quickstart-demo above: proves README.md's
# Quickstart fence and bin/quickstart-demo teach the identical commands,
# in the identical order, without running either
# (ehr-testing-tools.quickstart-fresh -- structural extraction + ordered
# comparison, not a substring search; see that namespace's docstring for
# why substring matching is unsafe here). Hermetic, milliseconds; joins
# ci.yml's fast tier.
quickstart-fresh:
	clojure -X ehr-testing-tools.quickstart-fresh/quickstart-fresh!

# Compatibility spelling, kept working and unchanged (CLI-2, 2026-07-26).
# `bin/ehr` is the taught entry point: this target cannot carry the CLI's
# 0/1/2/3 exit contract (ADR-0004, ADR-0010), because make's own exit
# status is 2 for any failed recipe -- so a rejection (1) and a
# no-verdict aggregate (3) both arrive here as 2. bin/ehr execs the same
# invocation below, so the two are otherwise equivalent.
ehr:
	clojure -M -m ehr-testing-tools.cli $(ARGS)

# Regenerates docs/pipeline.md from docs/pipeline.edn (author-time
# source of truth, pattern nursery #13): Clojure renders the equation
# lines, the string-diagram skill's own Python script renders the
# mermaid diagram from those lines, Clojure assembles the two into the
# generated markdown file. docs/pipeline.md carries its own
# "do not hand-edit" header; this target is the only sanctioned way to
# update it.
pipeline:
	@mkdir -p target
	clojure -X ehr-testing-tools.pipeline/write-equations-txt! :out '"target/pipeline-equations.txt"'
	python3 palgebra/tools/resource_equations_to_mermaid.py target/pipeline-equations.txt -o target/pipeline-flow.mermaid
	clojure -X ehr-testing-tools.pipeline/write-pipeline-md! :equations-txt '"target/pipeline-equations.txt"' :mermaid '"target/pipeline-flow.mermaid"' :out '"docs/pipeline.md"'
	@echo "Regenerated docs/pipeline.md"

# Regenerates docs/use-cases.md from docs/use-cases.edn (author-time
# source of truth), same generation split as `pipeline` above: Clojure
# writes one equations .txt per use case, the string-diagram skill's
# python script renders each case's own small mermaid diagram, Clojure
# assembles the full generated markdown file from both. docs/use-cases.md
# carries its own "do not hand-edit" header; this target is the only
# sanctioned way to update it.
use-cases:
	@mkdir -p target/use-cases
	clojure -X ehr-testing-tools.usecases/write-case-equations! :use-cases-edn '"docs/use-cases.edn"' :out-dir '"target/use-cases"'
	@for f in target/use-cases/*.txt; do \
		python3 palgebra/tools/resource_equations_to_mermaid.py "$$f" -o "$${f%.txt}.mermaid"; \
	done
	clojure -X ehr-testing-tools.usecases/write-use-cases-md! :use-cases-edn '"docs/use-cases.edn"' :cases-dir '"target/use-cases"' :out '"docs/use-cases.md"'
	@echo "Regenerated docs/use-cases.md"

# Regenerates docs/operators.md from the mutation-operator registry
# (src/ehr_testing_tools/corpus/operators.clj -- the registry is the
# source of truth, the doc is derived). Simpler than `pipeline` and
# `use-cases` above: no mermaid step, so one Clojure call does the
# whole job. docs/operators.md carries its own "do not hand-edit"
# header; this target is the only sanctioned way to update it.
operators-doc:
	clojure -X ehr-testing-tools.docsgen/write-operators-md! :out '"docs/operators.md"'
	@echo "Regenerated docs/operators.md"

# Regenerates docs/cli.md from the CLI help spec
# (src/ehr_testing_tools/cli/help.clj's cli-spec -- the same data
# `ehr help` renders to plain text, so the page and the shell can't
# drift apart). Same single-call shape as `operators-doc` above.
# docs/cli.md carries its own "do not hand-edit" header; this target is
# the only sanctioned way to update it.
cli-doc:
	clojure -X ehr-testing-tools.docsgen/write-cli-md! :out '"docs/cli.md"'
	@echo "Regenerated docs/cli.md"

# Tier-1 pipeline lint (P6, pattern nursery #13): every catalytic
# resource named in docs/pipeline.edn and docs/use-cases.edn resolves
# to one of the four catalytic targets docs/notation.md defines.
# External stages ({external: true}) are exempt -- this repo makes no
# claim about a black-box stage's own catalytic inputs. Not wired into
# CI yet (see .agents/plans/corpus-foundations.md's enforcement-wave
# entry) -- this target and its own unit-test suite
# (test/ehr_testing_tools/lint_test.clj) are the tier-1 enforcement
# itself; CI wiring is a separate, later step.
lint-pipeline:
	clojure -X ehr-testing-tools.lint/lint-pipeline!

# Dependency-direction lint (D9, docs/palgebra-design.md Sec I.7): no
# palgebra.* namespace (src or test) may require ehr-testing-tools.*.
# Keeps palgebra extractable as its own repo without an EHR-shaped
# dependency to untangle first. Not wired into CI yet (see
# .agents/plans/corpus-foundations.md's enforcement-wave entry) --
# this target and its own unit-test suite
# (palgebra/test/palgebra/deps_lint_test.clj) are the enforcement
# itself; CI wiring is a separate, later step.
lint-deps:
	clojure -X palgebra.deps-lint/lint-deps!

# Concatenates most git-tracked files in the repo into one pack file, for
# pasting into a chat UI that can't read the filesystem directly. Leads
# with a header (repo name, UTC generation timestamp, HEAD commit,
# working-tree status, elision note) so staleness is a one-glance check
# instead of a manual diff across files. File markers match
# ehr-testing-guide's pack format exactly, so packs from both repos read
# identically.
#
# Elides .agents/skills/** and .agents/prompts/archive/**: skill content
# is large, changes rarely, and isn't needed for ordinary session context
# -- see pack-skills below, which packs exactly what this elides. Also
# elides the vendored SimHospital corpus bytes
# (test/fixtures/v2/simhospital/messages.out, ADR-0011): large, static,
# and already content-addressed by git -- a pack-consuming session needs
# PROVENANCE.md, not the ER7 bytes, so PROVENANCE.md and the vendored
# LICENSE stay packed while messages.out alone leaves. Unlike the skills
# elision, this one is NOT the complement of pack-skills -- see
# PACK_ELIDE_PATTERN/PACK_SKILLS_PATTERN above.
pack:
	@echo "Creating $(PACK_OUTPUT)..."
	@{ \
		echo "========== PACK HEADER =========="; \
		echo "repo: $(REPO_NAME)"; \
		echo "generated (UTC): $$(date -u +%Y-%m-%dT%H:%M:%SZ)"; \
		echo "HEAD: $$(git rev-parse HEAD 2>/dev/null || echo 'no commits yet')"; \
		echo "git status --porcelain:"; \
		st="$$(git status --porcelain)"; \
		if [ -z "$$st" ]; then echo "working tree clean"; else echo "$$st"; fi; \
		echo "elides: .agents/skills, .agents/prompts/archive, test/fixtures/v2/simhospital/messages.out (corpus bytes; provenance stays packed)"; \
		echo "========== END PACK HEADER =========="; \
		echo ""; \
	} > $(PACK_OUTPUT)
	@git ls-files | grep -Ev '$(PACK_ELIDE_PATTERN)' | sort | while read -r file; do \
		echo "========== FILE: ./$$file =========="; \
		cat "$$file"; \
		echo ""; \
		echo "========== END FILE =========="; \
		echo ""; \
	done >> $(PACK_OUTPUT)
	@echo "Done! Created $(PACK_OUTPUT)"

# The complement of pack: exactly .agents/skills/** and
# .agents/prompts/archive/**, for the author to upload to project
# knowledge by hand when skills change -- not pushed anywhere by any
# target here.
pack-skills:
	@echo "Creating $(PACK_SKILLS_OUTPUT)..."
	@{ \
		echo "========== PACK HEADER =========="; \
		echo "repo: $(REPO_NAME)"; \
		echo "generated (UTC): $$(date -u +%Y-%m-%dT%H:%M:%SZ)"; \
		echo "HEAD: $$(git rev-parse HEAD 2>/dev/null || echo 'no commits yet')"; \
		echo "git status --porcelain:"; \
		st="$$(git status --porcelain)"; \
		if [ -z "$$st" ]; then echo "working tree clean"; else echo "$$st"; fi; \
		echo "includes only: .agents/skills, .agents/prompts/archive"; \
		echo "========== END PACK HEADER =========="; \
		echo ""; \
	} > $(PACK_SKILLS_OUTPUT)
	@git ls-files | grep -E '$(PACK_SKILLS_PATTERN)' | sort | while read -r file; do \
		echo "========== FILE: ./$$file =========="; \
		cat "$$file"; \
		echo ""; \
		echo "========== END FILE =========="; \
		echo ""; \
	done >> $(PACK_SKILLS_OUTPUT)
	@echo "Done! Created $(PACK_SKILLS_OUTPUT)"

# Dormant as of 2026-07-25 (AUTHORS-GUIDE.md section 2): no longer part
# of the session-end ritual now that both this repo and pragsmike/packs
# are public and the design channel clones directly. Left in place, not
# deleted -- still works exactly as documented below, for the day a
# pack-consuming (non-git) surface is needed again.
#
# Publishes the freshly generated pack AND skills pack to the
# pragsmike/packs repo (a local clone at $(PACKS_REPO_DIR), pushed to
# GitHub), so a session -- or the design channel -- can fetch either one
# by a plain raw.githubusercontent.com URL instead of re-uploading either
# by hand. Requires $(PACKS_REPO_DIR) to already be a clone of
# https://github.com/pragsmike/packs (clone it once by hand if absent;
# this target does not create it).
pack-push: pack pack-skills
	@if [ ! -d "$(PACKS_REPO_DIR)/.git" ]; then \
		echo "error: $(PACKS_REPO_DIR) is not a git clone of pragsmike/packs -- clone it first"; \
		exit 1; \
	fi
	@cp $(PACK_OUTPUT) $(PACKS_REPO_DIR)/$(REPO_NAME)-pack.txt
	@cp $(PACK_SKILLS_OUTPUT) $(PACKS_REPO_DIR)/$(REPO_NAME)-skills-pack.txt
	@head="$$(git rev-parse HEAD 2>/dev/null || echo 'no commits yet')"; \
	cd $(PACKS_REPO_DIR) && \
	git add $(REPO_NAME)-pack.txt $(REPO_NAME)-skills-pack.txt && \
	if git diff --cached --quiet; then \
		echo "No pack changes to push (packs repo already up to date)"; \
	else \
		git commit -m "$(REPO_NAME) @ $$head" > /dev/null && \
		git push -u origin main && \
		echo "Packs pushed to pragsmike/packs ($(REPO_NAME) @ $$head)"; \
	fi
