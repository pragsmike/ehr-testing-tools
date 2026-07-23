.PHONY: help pack

SHELL := bash

REPO_NAME := ehr-testing-tools
PACK_OUTPUT := ehr-testing-tools-pack.txt

help:
	@echo "Available targets:"
	@echo "  help  - show this message (default target)"
	@echo "  pack  - concatenate every git-tracked file into $(PACK_OUTPUT), with a freshness header"

# Concatenates every git-tracked file in the repo into one pack file at the
# repo root, for pasting into a chat UI that can't read the filesystem
# directly. Leads with a header (repo name, UTC generation timestamp, HEAD
# commit, working-tree status) so staleness is a one-glance check instead of
# a manual diff across files. File markers match ehr-testing-guide's pack
# format exactly, so packs from both repos read identically.
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
		echo "========== END PACK HEADER =========="; \
		echo ""; \
	} > $(PACK_OUTPUT)
	@git ls-files | sort | while read -r file; do \
		echo "========== FILE: ./$$file =========="; \
		cat "$$file"; \
		echo ""; \
		echo "========== END FILE =========="; \
		echo ""; \
	done >> $(PACK_OUTPUT)
	@echo "Done! Created $(PACK_OUTPUT)"
