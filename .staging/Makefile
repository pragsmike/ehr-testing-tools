# ehr-testing-sim Makefile
#
# Pack format and recipes mirror ehr-testing-tools' Makefile verbatim
# (same header block, same file markers, same elide-pattern mechanism),
# so packs from all sibling repos read identically and any parser
# anchored to line-start markers works across them. See tools'
# AUTHORS-GUIDE.md section 2 for the pack ritual's full reasoning.
#
# DORMANT since 2026-07-27 (notes/ADRs.md ADR-0015): this repo's GitHub
# remote is now public, so raw.githubusercontent.com/pragsmike/
# ehr-testing-sim/... is the chat-read path -- the same demotion tools
# recorded at its own ADR-0008. `pack`/`pack-skills`/`pack-push` all
# still work (a local, offline pack is still occasionally useful) but
# none of them are a required session-end step anymore; the ceremony is
# commit -> `git push origin`.

REPO_NAME := ehr-testing-sim
PACK_OUTPUT := $(shell echo $$HOME)/ehr-testing-sim-pack.txt
PACK_SKILLS_OUTPUT := $(shell echo $$HOME)/ehr-testing-sim-skills-pack.txt

# Local clone of https://github.com/pragsmike/packs -- clone it once by
# hand if absent; pack-push checks for it and refuses to run without it.
PACKS_REPO_DIR := $(shell echo $$HOME)/.packs

# Same elision as tools: skills are large and change rarely. Today this
# repo's .agents/skills/ is empty and .agents/prompts/archive/ doesn't
# exist, so the pattern elides nothing -- kept anyway so the pack
# format (and its header's elides line) stays identical across repos
# and nothing changes when skills arrive.
PACK_ELIDE_PATTERN := ^\.agents/skills/|^\.agents/prompts/archive/

.PHONY: help test coverage run pack pack-skills pack-push

help:
	@echo "Targets:"
	@echo "  test         - clojure -X:test"
	@echo "  coverage     - run cloverage and report coverage (clojure -M:coverage)"
	@echo "  run          - demo run: clojure -M:cli run --seed 42 --patients 5"
	@echo "  pack         - concatenate tracked files (except .agents/skills, .agents/prompts/archive) into $(PACK_OUTPUT)"
	@echo "  pack-skills  - concatenate only .agents/skills + .agents/prompts/archive into $(PACK_SKILLS_OUTPUT)"
	@echo "  pack-push    - pack + pack-skills, then publish both to the pragsmike/packs repo ($(PACKS_REPO_DIR)) -- DORMANT since 2026-07-27 (ADR-0015); optional, not a required ceremony step"

test:
	clojure -X:test

coverage:
	clojure -M:coverage

run:
	clojure -M:cli run --seed 42 --patients 5

# Concatenates most git-tracked files in the repo into one pack file, for
# pasting into a chat UI that can't read the filesystem directly. Leads
# with a header (repo name, UTC generation timestamp, HEAD commit,
# working-tree status, elision note) so staleness is a one-glance check
# instead of a manual diff across files. File markers match the sibling
# repos' pack format exactly.
#
# NOTE: this repo has no commits yet at scaffold time -- `git ls-files`
# is empty until the initial `git add`. Run pack only after the initial
# commit (the header's HEAD line will say 'no commits yet' otherwise,
# which is your staleness signal).
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
# .agents/prompts/archive/**. Empty until this repo grows skills;
# kept for cross-repo symmetry so pack-push's two-file contract is
# uniform across siblings.
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

# DORMANT since 2026-07-27 (ADR-0015), same as tools' own pack-push at
# its ADR-0008 -- this repo's public GitHub remote is now the primary
# read path; this recipe still works and remains occasionally useful
# (a local, offline snapshot) but is no longer a required ceremony
# step. Publishes the freshly generated pack AND skills pack to the
# pragsmike/packs repo (local clone at $(PACKS_REPO_DIR), pushed to
# GitHub), fetchable at
#   raw.githubusercontent.com/pragsmike/packs/main/ehr-testing-sim-pack.txt
#
# Ordering caveat (same as tools): run pack-push LAST in a session, not
# mid-session, so the header's clean-tree line stays meaningful -- a
# pushed pack showing a dirty tree is a real signal something was left
# uncommitted.
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
