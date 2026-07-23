.PHONY: help pack pack-skills pack-push test coverage ehr

SHELL := bash

REPO_NAME := ehr-testing-tools
PACK_OUTPUT := $(shell echo $$HOME)/ehr-testing-tools-pack.txt
PACK_SKILLS_OUTPUT := $(shell echo $$HOME)/ehr-testing-tools-skills-pack.txt
GIST_ID := 4fcd1abb4e74a5b54f9c241877edd02a

# Files elided from the default pack (see pack-skills below) -- an ERE
# matched against `git ls-files` output, anchored to line start.
PACK_ELIDE_PATTERN := ^\.agents/skills/|^\.agents/prompts/archive/

help:
	@echo "Available targets:"
	@echo "  help         - show this message (default target)"
	@echo "  pack         - concatenate tracked files (except .agents/skills, .agents/prompts/archive) into $(PACK_OUTPUT)"
	@echo "  pack-skills  - concatenate only .agents/skills + .agents/prompts/archive into $(PACK_SKILLS_OUTPUT)"
	@echo "  pack-push    - pack, then publish it to gist $(GIST_ID)"
	@echo "  test         - run the Clojure test suite (clojure -X:test)"
	@echo "  coverage     - run cloverage and report coverage (clojure -M:coverage)"
	@echo "  ehr          - invoke the ehr CLI, e.g. make ehr ARGS=\"artifact fetch --name synthea --version 4.0.0\""

test:
	clojure -X:test

coverage:
	clojure -M:coverage

ehr:
	clojure -M -m ehr-testing-tools.cli $(ARGS)

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

# Publishes the freshly generated (slim) pack to the author's gist, so a
# session can point at a URL instead of re-uploading the pack by hand.
pack-push: pack
	@jq -Rs '{files:{"ehr-testing-tools-pack.txt":{content:.}}}' $(PACK_OUTPUT) \
	  | gh api gists/$(GIST_ID) -X PATCH --input - > /dev/null
	@echo "Pack pushed to gist $(GIST_ID)"
