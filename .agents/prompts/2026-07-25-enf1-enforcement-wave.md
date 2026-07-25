# ENF-1 — The enforcement wave: hooks, CI gates, freshness, nightly integration

You are working in `ehr-testing-tools` (public). This session executes the enforcement wave `corpus-foundations.md` has accumulated under ADR-0006's staged-enforcement decision: lints and checks that already exist and pass locally get wired into hooks and CI so they cannot silently rot. Nothing new is verified this session — every gate added here enforces a check that is already green; if wiring a gate turns something red, that's a real finding to fix or report, not to gate around.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`, `notes/ADRs.md` (ADR-0006 especially), `.agents/plans/corpus-foundations.md` (the enforcement-wave row — this session's checklist — and the First-release row), `.github/workflows/ci.yml` (including its header comment), `Makefile` (all lint/test/coverage/pipeline targets), `.githooks/pre-commit` (the WSL-enforcement precedent and the opt-in `core.hooksPath` mechanism), `deps.edn` (`:test`, `:coverage` aliases; facts-register F20/F21 context), `artifacts.lock.edn` + `src/ehr_testing_tools/artifact.clj` (how the engine cache is populated — the nightly job needs this), `notes/facts-register.md` F14 (hit-nexus volatility — relevant to what the nightly job may fetch). Ritual: commit → `git push origin`. Save this prompt to `.agents/prompts/2026-07-XX-enf1-enforcement-wave.md`; final commit archives it.

Author rulings in effect: Fast gates block, slow gates schedule — hooks and the per-push CI job carry only the fast checks (unit suite, lints, freshness); the integration suite goes in a separate scheduled/manual workflow, never in the per-push path. Coverage floor — set the cloverage fail threshold a modest margin below current (current: 89.43% forms / ~92.7% lines; recommended floor: 85% forms — a ratchet to revisit, not a target), implemented via cloverage's own fail-threshold flag in the `:coverage` alias or Makefile, single source of truth. Hooks stay opt-in — the existing `core.hooksPath .githooks` mechanism; no hook installation automation beyond what AGENTS.md already documents. No new checks — this session wires existing ones; authoring new lints is out of scope.

## Step 0 — Reconcile the coverage-gating contradiction

`corpus-foundations.md`'s First-release row says coverage-threshold gating landed; `ci.yml`'s header comment says it hasn't ("that's the enforcement wave"); the Makefile shows no threshold flag. Determine from evidence (git log for the First-release row's edit, Makefile history, cloverage invocation) which statement is true, correct the false one in place, and note the reconciliation in the commit message. Do not build the gating yet — this step only makes the record true; Step 2 builds it.

Commit: `ENF-1: reconcile coverage-gating claim (record corrected from evidence)`.

## Step 1 — Pre-push hook: the fast suite

`.githooks/pre-push`: runs `make test` plus both lints (`lint-pipeline`, `lint-deps`) and refuses the push on failure, with a clearly labeled escape hatch (`git push --no-verify`) mentioned in its error text for emergencies. Same opt-in mechanism and shell discipline as the existing pre-commit hook (WSL check included — pushes are git operations too). Keep it fast: no coverage, no integration, no doc regeneration. AGENTS.md's hook section gains the one-line description.

Commit: `ENF-1: pre-push hook — fast suite + lints (opt-in via core.hooksPath, per ADR-0006)`.

## Step 2 — Per-push CI gains the gates

`ci.yml`, extending the existing job (keep its caching and structure):

1. `make lint-pipeline` and `make lint-deps` as steps after the test step.
2. Freshness checks: `make pipeline && make use-cases && git diff --exit-code docs/pipeline.md docs/use-cases.md` — fails if a generated doc is stale relative to its source EDN. Requires python3 in the runner: add the setup step if the runner image lacks it (verify, don't assume).
3. Coverage threshold: the cloverage fail-threshold flag at the ruled floor, wired so `make coverage` itself fails below it (one source of truth — CI and local behavior identical). Update `ci.yml`'s header comment: the enforcement wave has arrived; name what gates here and what is deliberately elsewhere (integration → nightly workflow).

Commit: `ENF-1: CI gates — lints, generated-doc freshness, coverage floor at 85% forms`.

## Step 3 — The nightly/manual integration workflow

New `.github/workflows/integration.yml`: `schedule` (pick a quiet UTC hour) + `workflow_dispatch`, running `make integration` with artifact-cache priming — a step that populates the engine cache the way local runs do (inspect `artifact.clj` for the cache location and the fetch entry point; cache it with `actions/cache` keyed on `artifacts.lock.edn`'s hash). Two honesty requirements: (a) the job's header comment states what it fetches on a cold cache and cites F14's volatility note — a hit-nexus outage fails this job, and that is signal, not flake; (b) the job is not in the per-push path and its failure does not block merges — it reports. If any secret/licensing consideration surfaces around fetching an engine in CI (check `artifacts.lock.edn`'s entries against components.md's license rows), stop and report rather than fetch.

Commit: `ENF-1: nightly integration workflow with lockfile-keyed artifact cache (reports, never blocks)`.

## Step 4 — Close out

Run everything the wave now enforces, locally, once, from cold where feasible (`make test`, `make coverage` with the floor, both lints, the freshness pair) — green before push. Trigger the integration workflow once via `workflow_dispatch` after pushing if runner minutes permit; record the outcome either way. `corpus-foundations.md`: enforcement-wave row → Done with an itemized summary (and the palgebra direction-lint line marked landed-in-CI); the nightly-integration proposal line resolves into the shipped workflow. Archive this prompt.

Commit: `ENF-1 complete: enforcement wave landed (archives prompt)`.
