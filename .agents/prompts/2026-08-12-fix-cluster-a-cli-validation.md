# 2026-08-12 — ehr-testing-tools: fix cluster A, CLI validation and error quality (build session)

## Context

Archived 2026-08-12. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `baf6a8c` (ADR-0116 close) and closed at
`c058706` (commit 3) plus this record's own commit. Original prompt
follows verbatim; a deviation record follows that.

## Original prompt (verbatim)

Session prompt — fix cluster A: CLI validation and error quality (ADR-0117)

You are Claude Code executing under R30 ceremony for ehr-testing-tools (github.com/pragsmike/ehr-testing-tools), working for mg, the sole author. This SRC session lands review-3's fix cluster A (chartered ADR-0115): eight fixes, F1–F8 below, each with red-before-green, each touching only error paths, validation, help text, or a flag rename on a verb no oracle root invokes — oracle identity is argued per-fix and verified once at the bracket. STOP-AND-REPORT on any conflict between this prompt and the tree.

Standing notes: full `make test` before EVERY push; gate-forced companions inside the fence by rule (named in the record); budget headroom expected fine (small register additions) — if the budget test trips, STOP-AND-REPORT. The kernel result/error envelope is the validation convention (R10, ADR-0116); every new error below uses it. Exit code for user errors is 2 throughout (the register's own canonical). No ADR tokens in any user-facing prose (the footnote gate scans generated docs).

**Read first**

1. `.agents/plans/2026-08-12-review-3-user-surface-findings.md` — rows R3-B2-1, B2-2, B2-3+B4-1, B1-5, B1-3, B2-5+B3-3, B1-1, B1-4 (each fix's evidence and recommendation; the recommendations are ruled into the specs below).
2. `bases/cli/src/ehrt/cli/core.clj` and `help.clj` — the dispatch boundary, the existing validation and error-category conventions (C-4's unknown-flag check, `:missing-required-opt`, `:out-dir-exists`, the unknown-group handling for `ehrt <unknown-group>`), and the exit-code mapping.
3. `components/kernel/src/ehrt/kernel/result.clj` — the envelope.
4. `notes/adr/0116-engine-seed-contract.md` — R9/R10 and the caller propagation precedent.
5. `.agents/rulings.md` tail; `.agents/plans/roadmap.md` (cluster A row).

**Step 0 — Preflight and tag ceremony**

* `git fetch`; confirm `origin/main` at `baf6a8c`. Else STOP-AND-REPORT.
* Confirm CI green on `main` (`gh run list --limit 5`) — this completes the one leg (CI) the channel's ADR-0116 verification left session-transcribed (API rate-limited; the session watched both runs green with matching SHAs).
* Tag `stable-20260812-engine-seed-contract`, ANNOTATED, at `baf6a8c`; push; peeled ref exact. License: case (i) — channel fresh-clone verification 2026-08-12: lineage, ASCII x2, footprint exact to the twice-widened fence, the engine diff read directly (pure guard, success path textually identical), caller propagation diffs read (error-gated, success path untouched), independent sweep census cross-check (exactly 24 constrained sites), oracle identity basis; CI per the preflight above.

**The fixes (Step 1 = red tests; Step 2 = fixes; commit per the structure below)**

For each fix: write the failing test FIRST (in `bases/cli/test/ehrt/cli/core_test.clj` unless noted), run it, capture the red; then implement; then green. The review's own B2 probe transcripts are the behavioral specs for the reds.

**F1 (R3-B2-1, HIGHEST)** — `check` target validation. Current (verify): `ehrt check` with no target, a nonexistent path, or an empty directory returns `:status :ok` with all-zero totals. New: the target DIR argument is required and must name an existing, non-empty directory; violations → `result/error` (missing arg → `:missing-required-opt`-shaped; nonexistent or empty → `:invalid-target` with payload `{:path ... :reason :not-found}` or `{:reason :empty}`), exit 2, hint naming the next action. Mirrors `corpus generate`'s refusal-to-proceed-silently discipline. The validation lives at the CLI boundary — do NOT touch judge/check components.

**F2 (R3-B2-2)** — parse-error translation. Current (verify): `--seed abc` (any coercion failure) surfaces a raw `babashka.cli` ExceptionInfo with library name and file:line, wrong exit code. New: catch parse-time ExceptionInfo at the dispatch boundary; translate to `:invalid-flag-value` (or the nearest existing sibling category — match, don't invent) naming the flag, the offending value, and the expected type; exit 2; no stack trace, no library name. One boundary-level catch covering every verb.

**F3 (R3-B2-3 + R3-B4-1, one fix)** — `corpus intake --out`. Current (verify): missing `--out` → NullPointerException four layers deep. New: `--out` is REQUIRED — validated at the boundary, `:missing-required-opt`, exit 2, same shape as `sim run --seed`. Ruled require-not-derive [C, un-vetoed reasoning recorded in the ADR: a derived path would fold the RQ3 wall-clock `--received` default into a filesystem name, quietly unreproducible; requiring is honest].

**F4 (R3-B1-5)** — missing-required-flag unification. Standardize every "required flag missing" case on `:missing-required-opt`-shaped payloads at exit 2, retiring verb-specific categories (the register names `:interval-required` and siblings — census them by grep, list the retired categories in the ADR). Update any tests asserting the old categories (these edits are in-fence; enumerate them in the record).

**F5 (R3-B1-3)** — source-scoping validation. Current (verify): a `synthea:`-scoped flag passes validation even when `--source` is not synthea. New: reject with a categorized error naming the flag, its scope, and the selected source (reject, not warn — consistent with this cluster's strict-validation direction).

**F6 (R3-B2-5 + R3-B3-3, one fix)** — `help <unknown-group>`. Current (verify): `ehrt help crops` silently shows general help. New: same treatment as `ehrt <unknown-group>` itself, reuse that existing category verbatim, name the bad group, list valid ones, exit 2.

**F7 (R3-B1-1, RULED ADR-0115 RQ1)** — the rename. `gate fhir`'s `--out-dir` → `--scratch-dir`. Clean rename, NO back-compat alias (the tool is unpublished — state this in the ADR). Sweep duty (ADR-0099 rule form): every reference to the old flag on this verb anywhere in the tree — cli-spec, tests, use-case strips (`components/corpus/docs/use-cases.edn`), demo READMEs, any docs — found by extension-blind, un-truncated grep; regen surfaces follow. The invocation lint and use-case gates co-verify.

**F8 (R3-B1-4, RULED ADR-0115 RQ2, + the ADR-0116 inherited seed-row wording, one edit)**. `corpus generate`'s `--seed` doc string becomes: "patient/master-generation seed (integer; non-negative when --source sim), shared by both sources; defaulted here as the ergonomic front door -- the sim-tier verbs (sim run, sim identifiers) require a seed explicitly". One row; regen.

**Commit structure**

* Commit 1 — F1+F2 (the two crash/silent-success fixes): `fix: cli check target validation and parse-error translation (ADR-0117)`
* Commit 2 — F3+F4+F5+F6 (validation unification): `fix: cli required-flag, source-scoping, and help-group validation (ADR-0117)`
* Commit 3 — F7+F8 + all doc regen + strip sweep: `fix: gate fhir scratch-dir rename; seed-row tiering note (ADR-0117)`
* Commit 4 — registers, ADR, record, archive: `docs: session record and prompt archive -- fix cluster A (ADR-0117)`

Full `make test` green before EACH push. Red evidence for every fix captured in the ADR (test name + failing run before its fix commit).

**Step 3 — Registers, ADR, close**

* Roadmap: cluster A row → RESOLVED (all eight, with the F3 require-not-derive reasoning noted); the ADR-0116-inherited seed-row item → closed by F8.
* Register: the eight rows gain dated `FIXED, ADR-0117` notes (fix-forward; summary table untouched per the ADR-0115 snapshot rule).
* `.agents/rulings.md` "From ADR-0117": F3's require-not-derive [C, un-vetoed] and F5's reject-not-warn [C, un-vetoed], recorded so they're strikeable at a glance.
* Self-archive at close-phase START; ADR-0117 (per-fix evidence: red run, green run, the F7 sweep census with file:line); indices (114 → 115); Done line; session record.

**Oracle bracket.** Pre-analysis: pure identity on all 35 roots. Argument: F1–F6 change only error paths on invalid inputs no root supplies; F7 renames a flag on a verb no root invokes (channel verified: zero `gate` references in `bin/regression-oracle`); F8 is help text. Run `bin/regression-oracle baf6a8c <final-commit>`; any non-identity is STOP-AND-REPORT.

Gates: as standing; ASCII x4; gitleaks per commit + pre-push; CI confirm or disclose.

**Fences**

* Touch ONLY: `bases/cli/src/ehrt/cli/core.clj`, `help.clj`; `bases/cli/test/ehrt/cli/core_test.clj`; the F7 sweep's strip/doc surfaces (enumerated by the census, each named in the record); regenerated `docs/cli.md` + use-case pages (deltas only where F7/F8 reach); intake's CLI-side validation point if it lives outside core.clj (name the file; validation only); registers, prompts, session-records, `notes/adr/0117-*.md`, `notes/ADRs.md`, `notes/adr/README.md`; gate-forced companions by rule.
* ZERO judge/check component internals, ZERO engine/sim src, ZERO behavior change on any valid input to any verb. A fix wanting to touch outside this list is STOP-AND-REPORT with the finding — the ADR-0116 precedent (widen by ruling, never silently).

STOP-AND-REPORT on: any "current (verify)" claim failing verification; a red test that will not go red; any regen delta outside F7/F8's reach; oracle non-identity; anything not pre-decided.

## Deviation record

None. Every "current (verify)" claim in this prompt was probed live
against the tree before its own fix landed and held exactly as stated
(F1's `check` triple probe, F2's malformed `--seed` probe, F3's `corpus
intake` NPE probe, F6's `help crops` probe — full transcripts in
`notes/adr/0117-fix-cluster-a-cli-validation.md`). No red test refused
to go red. No regen delta landed outside F7/F8's own predicted reach
(`docs/cli.md`'s diff after commit 3 was exactly the 4 lines predicted;
`make use-cases` re-run as a drift check, confirmed no-op). The oracle
bracket (`bin/regression-oracle baf6a8c c058706`) returned IDENTICAL
across all 35 roots, matching the pre-analysis exactly — no
STOP-AND-REPORT triggered anywhere in this session. The intake
validation point named as a possible fence-widener in the prompt's own
Fences section turned out to live in `core.clj` itself
(`intake-command`), so no separate file was touched for F3.
