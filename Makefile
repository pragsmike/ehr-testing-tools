.PHONY: help pack pack-skills pack-push test coverage integration ehr pipeline use-cases lint-pipeline lint-deps

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

# Files elided from the default pack (see pack-skills below) -- an ERE
# matched against `git ls-files` output, anchored to line start.
PACK_ELIDE_PATTERN := ^\.agents/skills/|^\.agents/prompts/archive/

help:
	@echo "Available targets:"
	@echo "  help         - show this message (default target)"
	@echo "  pack         - concatenate tracked files (except .agents/skills, .agents/prompts/archive) into $(PACK_OUTPUT)"
	@echo "  pack-skills  - concatenate only .agents/skills + .agents/prompts/archive into $(PACK_SKILLS_OUTPUT)"
	@echo "  pack-push    - dormant (2026-07-25, not part of the session ritual) - pack + pack-skills, then publish both to the pragsmike/packs repo ($(PACKS_REPO_DIR))"
	@echo "  test         - run the Clojure test suite (clojure -X:test); cold-cache/no-network hermetic"
	@echo "  coverage     - run cloverage and report coverage (clojure -M:coverage); cold-cache/no-network hermetic"
	@echo "  integration  - run the integration test suite (clojure -X:integration); requires"
	@echo "                 'ehr artifact fetch' for synthea, temurin-jdk, and fhir-validator-cli first, e.g.:"
	@echo "                   make ehr ARGS=\"artifact fetch --name synthea --version 4.0.0\""
	@echo "                   make ehr ARGS=\"artifact fetch --name temurin-jdk --version 17.0.19+10\""
	@echo "                   make ehr ARGS=\"artifact fetch --name fhir-validator-cli --version 6.9.12\""
	@echo "  ehr          - invoke the ehr CLI, e.g. make ehr ARGS=\"artifact fetch --name synthea --version 4.0.0\" -- see every command with make ehr ARGS=\"help\""
	@echo "  pipeline     - regenerate docs/pipeline.md from docs/pipeline.edn"
	@echo "  use-cases    - regenerate docs/use-cases.md from docs/use-cases.edn"
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
# -- see pack-skills below, which packs exactly what this elides.
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
		echo "elides: .agents/skills, .agents/prompts/archive"; \
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
	@git ls-files | grep -E '$(PACK_ELIDE_PATTERN)' | sort | while read -r file; do \
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
