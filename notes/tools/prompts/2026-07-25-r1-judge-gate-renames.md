# R1 — Judge/gate factorization: docs, ADR-0009, mechanical renames

You are working in `ehr-testing-tools` (public). This session executes Phases 0–1 of `.agents/plans/judge-gate-refactor.md`: land the design record and ADR-0009, then rename the decide-layer components out of the act-layer word `gate` — namespaces, EDN stage kind, calibration doc, and the `:policy` finding field — leaving behavior untouched and the tree green after every commit. No semantic changes this session: the verdict split (plan Phase 3) and the palgebra claim sweep (Phase 2) are later sessions.

Read first: `AGENTS.md`, `AUTHORS-GUIDE.md`, `notes/ADRs.md`, `docs/palgebra-design.md` (the decision register especially), `.agents/plans/judge-gate-refactor.md`, `docs/notation.md`, `docs/pipeline.edn` and `src/…/pipeline.clj`, `docs/gate-calibration.md`, `src/ehr_testing_tools/gate/`, `src/ehr_testing_tools/cli.clj`, `src/ehr_testing_tools/check.clj`, both `test-integration/` suites. Test-first per ADR-0006 where a change is testable; renames are verified by the suite staying green, `make pipeline` and `make use-cases` regenerating cleanly, and `make lint-pipeline` passing. Ritual: commit → `git push origin`. Save this prompt to `.agents/prompts/2026-07-XX-r1-judge-gate-renames.md`; final commit archives it.

Author rulings in effect: the design register's D1–D13 are accepted as written. O1 resolved: verdict value names `:pass`/`:rejected` are KEPT in v1 (serialization stability; revisit at signature-format v2). O2 resolved in principle (terminology-suppressed becomes `no-verdict(:terminology-suppressed)`) but NOT implemented this session — it is Phase 3's single semantic change; this session leaves `:indeterminate` in place, renamed context only. The CLI verb `ehr gate` keeps its name (D12). Archived prompts under `.agents/prompts/archive/` are history: never edited by this session.

## Step 0 — Design record and plan land as repo artifacts

1. Confirm `docs/palgebra-design.md` is committed as delivered (it may already be in the tree from the author's download; normalize location/name if needed, no content edits beyond link fixes).
2. Move the refactoring plan to `.agents/plans/judge-gate-refactor.md` if the author has not already; fix any relative links.
3. `.agents/plans/corpus-foundations.md`: add a phase row R (interstitial, before P7) — "Judge/gate factorization: vocabulary refactor per `docs/palgebra-design.md` and `.agents/plans/judge-gate-refactor.md`; no new capability" — status In progress, prompt column pointing at this prompt's archive path. Add one line to the enforcement-wave row: the palgebra namespace-direction lint (plan Phase 2) should land in CI alongside `make lint-pipeline` wiring.

Commit: `R: land palgebra design record + judge-gate refactor plan (ADR-0009 context)`.

## Step 1 — ADR-0009

Append to `notes/ADRs.md` in house format: ADR-0009 — Judge/gate factorization. Context: gate was an act-layer word on decide-layer components; the `:gate` kind bundles judge laws (verdict + findings, never modifies its subject) with a routing policy (three-way output split). Decision: components that decide are judges; `gate` is reserved for workflow positions that act on verdicts (the CLI verb qualifies — exit-code mapping is its policy; `--baseline` is already an explicit policy argument). The old kind's three outputs are derivable (`judge ⨟ route-by-verdict`), not primitive. The finding field `:policy` is renamed `:disposition` (criterion-layer datum, act-layer word — same conflation one level deeper). Rejected: renaming the CLI verb (it is genuinely a gate); renaming verdict values (O1 ruling above). Cites D1, D2, D11, D12 in `docs/palgebra-design.md`.

Commit: `ADR-0009: judge/gate factorization`.

## Step 2 — Namespace renames (behavior-neutral)

`git mv src/ehr_testing_tools/gate → src/ehr_testing_tools/judge` and the matching `test/ehr_testing_tools/gate → …/judge`; update ns forms (`ehr-testing-tools.gate.X` → `ehr-testing-tools.judge.X`) and every require site: `cli.clj`, `check.clj`, `check/schemas.clj`, `core.clj` if it aliases, both `test-integration/` suites. Grep for the OLD ns strings afterward — zero live hits outside `.agents/prompts/archive/` and experiment/results records (which stay as history). Internal fn names in `cli.clj` (`gate-command` etc.) may stay this session; the verb is the contract.

Commit: `Rename: gate.* namespaces → judge.* (ADR-0009; libraries of judges, not gates)`.

## Step 3 — `:disposition` rename (D11)

In `judge/fhir.clj`: the per-finding key `:policy` → `:disposition` (the field set near line 211 and the `worst-of` call site near 226, plus docstrings); in `judge/report.clj`: the echoes near lines 97 and 119; any fixtures/goldens in tests that serialize the key. Rationale one-liner in the fhir docstring: "disposition = this finding's verdict contribution per the versioned mapping; `policy` is reserved for the verdict→action layer (ADR-0009)."

Commit: `Rename: finding :policy → :disposition (ADR-0009; avoid collision with the policy layer)`.

## Step 4 — EDN and generated docs

1. `docs/pipeline.edn`: `:kind :gate` → `:kind :judge`; stage ids/labels as needed; the kind's law text split per the plan — judge laws stay on the kind; add one sentence noting the three-way output split is the route-by-verdict policy, a derived construct (full notation treatment deferred, plan Phase 4 / O3). Update the format-dispatch law's verbatim `gate.fhir`/`gate.v2` references.
2. `src/…/pipeline.clj`: `stage-kinds` `:gate` → `:judge`; the five-kinds docstring. (Flag with a `;; palgebra: signature data hardcoded — extracted Phase 2` comment, nothing more.)
3. `docs/use-cases.edn`: the 14 `gate` occurrences — ids, labels, equation text.
4. `docs/notation.md`: the five-kinds table row (`gate` → `judge`, law reworded to the judge law; routing noted as derived).
5. Regenerate: `make pipeline`, `make use-cases`; commit sources and generated docs together. `make lint-pipeline` green.

Commit: `Rename: :gate kind → :judge; route-by-verdict noted as derived policy (ADR-0009)`.

## Step 5 — Prose docs

`docs/gate-calibration.md` → `docs/judge-calibration.md` (title + inbound links: README.md, docs/README.md, components.md, pipeline.md, use-cases.md, engine-onboarding.md, positioning.md, experiments.md, pipeline.edn law text if any remain). README maturity-table wording. `check.clj` docstring: align its existing judge prose with the settled species vocabulary (validator = institutional criterion; checker = occasional expectation) — wording only.

Commit: `Docs: gate-calibration → judge-calibration; species vocabulary aligned (ADR-0009)`.

## Step 6 — Close out

Full suite + `make integration` if artifacts are cached locally (the contract-pairing suite is the polarity regression — it must pass untouched). Final grep for live `gate` references outside the CLI verb, history, and the words "gate" used correctly (the CLI section of README, ADRs, archived prompts). Archive this prompt; update the R row in `corpus-foundations.md` to Done with date.

Commit: `R1 complete: judge/gate renames landed, tree green (archives prompt)`.
