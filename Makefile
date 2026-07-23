.PHONY: help pack pack-push test

SHELL := bash

REPO_NAME := ehr-testing-tools
PACK_OUTPUT := $(shell echo $$HOME)/ehr-testing-tools-pack.txt
GIST_ID := 4fcd1abb4e74a5b54f9c241877edd02a

help:
	@echo "Available targets:"
	@echo "  help       - show this message (default target)"
	@echo "  pack       - concatenate every git-tracked file into $(PACK_OUTPUT), with a freshness header"
	@echo "  pack-push  - pack, then publish it to gist $(GIST_ID)"
	@echo "  test       - run the Clojure test suite (clojure -X:test)"

test:
	clojure -X:test

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

# Publishes the freshly generated pack to the author's gist, so a session
# can point at a URL instead of re-uploading the pack by hand. Uses
# python3's json module rather than jq: jq is not installed on this
# machine and there is no passwordless sudo available to install it
# non-interactively; python3 is already present and produces the same
# {"files": {...}} payload gh api expects for a gist PATCH.
pack-push: pack
	@python3 -c "import json, sys; \
	  path = sys.argv[1]; \
	  content = open(path, encoding='utf-8').read(); \
	  print(json.dumps({'files': {'ehr-testing-tools-pack.txt': {'content': content}}}))" \
	  "$(PACK_OUTPUT)" \
	  | gh api gists/$(GIST_ID) -X PATCH --input - > /dev/null
	@echo "Pack pushed to gist $(GIST_ID)"
