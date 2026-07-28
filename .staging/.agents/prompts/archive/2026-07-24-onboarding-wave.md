# Onboarding wave — agent-legible setup for the first trial audience

Task: Onboarding wave — agent-legible setup for the first trial audience
You are working in `ehr-testing-tools` (now public or about to be). Small session. The first trial audience is known: EHR domain experts, comfortable with Python, not Clojure; their primary AI tools are ChatGPT and Codex; most on Windows 11 (some WSL-aware), some on Ubuntu or NixOS; JVM/Clojure/make/jq likely absent. The strategy is NOT handcrafted per-platform install guides — it is making the repo agent-legible: a fresh clone must contain everything an agent needs to guide its human from nothing to a generated corpus.
Design fact this session is built on: Codex reads root `AGENTS.md` automatically. That file is currently maintainer-facing; an onboarding user's agent reading it today learns commit rituals, not setup. Fix the routing, not just the content.
Read first: `README.md`, `AGENTS.md`, `SETUP-relevant` parts of `Makefile` and `deps.edn`, `docs/README.md`, `docs/positioning.md`, `docs/notation.md`, `.agents/memory/patterns.md`, `artifacts.lock.edn`. Commits from WSL; `make test` green before and after. Save this prompt to `.agents/prompts/2026-07-24-onboarding-wave.md`; final commit archives it.

## Step 0 — Attribution correction (evidence now exists)

The publication wave declined to attribute the `string-diagram` skill to cyberneutics because the repo contained no evidence. The evidence now exists: the upstream skill lives at `https://raw.githubusercontent.com/pragsmike/cyberneutics/main/.claude/skills/string-diagram/SKILL.md` (retrieved HTTP 200, 2026-07-24, from the design channel; name and description match the copy here). Corrections:

1. `docs/notation.md` closing line: the notation and the string-diagram rendering skill both originate in the author's cyberneutics methodology (link the repo), formalization exploratory, as before.
2. `AGENTS.md` skills scoping: the cyberneutics-derived set is `scenarios`, `probe`, `review`, `committee`, and `string-diagram` — fix the list, cite the upstream path with the retrieval date in a brief comment or the skills paragraph.

Commit: `Attribute string-diagram to cyberneutics (upstream verified)`.

## Step 1 — AGENTS.md routing and audience scoping

Restructure `AGENTS.md` so its FIRST content after the title is a routing block, approximately:
If you are an AI agent helping a human install, set up, or use these tools: read `SETUP.md` first — it has prerequisites, platform guidance, verification steps, and a first-run walkthrough. The rest of this file governs contribution sessions (commits to this repo) and does not apply to using the tools.
Then, in the maintainer content below, scope explicitly: the WSL-only commit rule, pack ritual, F-row and test-first duties bind contributors/maintainer sessions; users running the tools never commit here and are not bound by any of it. Keep all existing maintainer content otherwise intact.
Commit: `AGENTS.md: onboarding routing block; scope maintainer rules`.

## Step 2 — SETUP.md (the load-bearing artifact)

Author `SETUP.md` at the repo root, written to serve a human reader and their agent equally. Structure:

1. What you need and why — a table: prerequisite | why | minimum version | verify with. Rows: git; JDK 17+ (runs the `ehr` CLI and tests; note explicitly: Synthea's own JDK is fetched automatically into the artifact cache via `artifacts.lock.edn` — do not install a second JDK for it); Clojure CLI (state the version the repo is tested with); GNU make; bash; curl. A second, clearly separated table for maintainer-only tools (jq, gh, WSL git hooks) marked "not needed to use the tools."
2. Platform guidance — short, honest: Linux (Ubuntu is what the repo is developed and tested on; give the apt one-liner for the prerequisites); Windows 11 → WSL2 with Ubuntu is the supported path (native Windows is unsupported: the build uses make/bash; one sentence on enabling WSL2, link Microsoft's doc); NixOS (a one-line `nix-shell -p` suggestion naming the packages; untested, flagged as such); macOS (untested, likely fine, same prerequisites).
3. Verification ladder — the exact command sequence from fresh clone to green: version checks for each prerequisite, then `make test` with the expected result stated (166 tests, 0 failures — update if the number has moved).
4. First corpus walkthrough — the real, verified commands: artifact fetch (Synthea + JDK resolve on first use), a small `corpus generate` invocation (population 5–10, with seed, clinician-seed, reference-date — copy the working example from README and verify it again this session), where the output lands, what the manifest is, and one `corpus mutate` example against a generated patient file. End with: outputs are plain FHIR JSON and EDN manifests — consumable from Python or any language; no Clojure knowledge is needed to use the results.
5. The agent prompt — a fenced copy-paste block, addressed to the reader: "If you'd rather have an AI assistant do this, give it this prompt:" — the prompt itself short and pointed, e.g.: "I have a fresh clone of the ehr-testing-tools repository. I am on [Windows 11 / Ubuntu / NixOS / macOS]. Read SETUP.md and README.md in the repo root. Help me: (1) install the prerequisites for my platform, (2) run the verification ladder until `make test` passes, (3) generate my first small synthetic corpus per the walkthrough, and (4) explain what the output files are. If anything fails, diagnose from the repo's docs before searching the web." Adjust wording for clarity, keep it under ~120 words.
6. Troubleshooting seeds — the three or four likeliest failures, one line each: wrong/old java on PATH; Clojure CLI not on PATH after install; running on native Windows (symptom: `make` not found → go to WSL2 section); first `generate` slower than expected (artifact downloads ~200MB Synthea + JDK on first use — say the sizes).

Accuracy duty: every command in SETUP.md must be executed and verified this session (in your WSL environment); state in the report which you ran. Do not include commands you could not verify — for platforms you cannot test (NixOS, macOS), label the guidance untested.
Commit: `SETUP.md: prerequisites, platforms, verification, agent prompt`.

## Step 3 — README and index stitching

1. README: immediately before or inside the quickstart, add the prerequisites pointer: one line — "Prerequisites and platform setup (including a copy-paste prompt for your AI assistant): SETUP.md." Add the platform support sentence (Linux/WSL2 supported; native Windows not) wherever the quickstart implies running commands. Add the outputs-are-plain-JSON/Python sentence to the what-this-is paragraph or quickstart tail — once, not thrice.
2. `docs/README.md`: add SETUP.md to the reading order (first, for newcomers), one line.
3. `docs/positioning.md`: in the audience section, one added sentence recording the first trial cohort (EHR domain experts, Python-comfortable, agent-assisted workflows, largely Windows/WSL2) — it sharpens segment (2) rather than replacing it.

Commit: `README/docs index/positioning: onboarding stitching`.

## Step 4 — Legibility self-audit

Simulate the trial user's agent: using ONLY files present in a fresh clone (no session memory), answer in the report: (a) what must I install on Windows 11, exactly; (b) how do I verify the setup worked; (c) how do I generate and find my first corpus; (d) does the WSL commit rule apply to my human; (e) where do I learn what the output files mean. Each answer must cite the file (and section) that provides it. If any answer requires knowledge not in the clone, fix the gap before finalizing — that is the acceptance test of this session.

## Finalize

`make test` green; run the SETUP.md quickstart end-to-end once more after all edits; archive this prompt; `make pack-push`; verify the raw fetch shows final HEAD, clean tree. Report: the agent-prompt text verbatim (author will review it); commands verified vs labeled untested; the self-audit Q&A with citations; files changed; commits.

## Out of scope

No Dockerfiles, Nix flakes, or installer scripts (a one-line nix-shell suggestion is the ceiling); no capability code; no CI changes; no Python bindings or wrappers (the plain-JSON sentence is the whole bridge for now); no guide-repo edits; no changes to dated records.
