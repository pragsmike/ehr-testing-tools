# 2026-08-12 — ehr-testing-tools: engine seed contract (build session)

## Context

Archived 2026-08-12. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `c6deb5a` (ADR-0115 close) and closed at
`fc72f54` (commit 1) plus this record's own commit. Original prompt
follows verbatim; a deviation record follows that.

## Original prompt (verbatim)

Session prompt — engine-test flake: negative-seed contract, repro, and fix (ADR-0116)

You are Claude Code executing under R30 ceremony for ehr-testing-tools (github.com/pragsmike/ehr-testing-tools), working for mg, the sole author. This session resolves the R8-chartered engine-test flake. It is the first SRC session in several: the fence is tight, red-before-green is mandatory, and the oracle bracket's identity claim is argued below, then verified. The classification is already made by the evidence and the author's contract ruling (R9, below): the defspec's generator exercised inputs outside the seed contract, and the engine accepted them unvalidated. Both halves get fixed. STOP-AND-REPORT on any conflict between this prompt and the tree — including any repro result that contradicts the classification.
Standing notes (ADR-0114/0115 lessons): full `make test` before EVERY push; gate-forced companion files are inside the fence by rule (name each in the session record); budget headroom pre-check — this session's register additions are small, no reading-set path grows materially, no re-baseline expected (if the budget test trips anyway, STOP-AND-REPORT rather than re-baselining).
The evidence base (channel-verified, 2026-08-12)

* The defspec `mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog` (`components/sim-engine/test/ehrt/sim_engine/engine_test.clj:1160`) draws `seed` from `gen/large-integer` — the FULL long range, negatives included — and passes it to `engine/run`.
* Two recorded failures: seed `7844068501` (failing-size 110, ADR-0112's disclosure) and seed `1786546687672` (failing-size 126, ADR-0115's CI on `ed00e3a`), the latter with the shrunk minimal counterexample `[-3377439408979484]` — a single negative long.
* The seed contract is documented nowhere: both `--seed` rows in `docs/cli.md` say "(integer)" unqualified; the engine interface states no constraint.
* The 35 oracle roots all use small positive seeds.

Author ruling R9, verbatim (record in Step 3)
The channel asked: is the seed contract (a) non-negative longs — engine validates at entry, generator constrained to contract, contract stated in the docs — or (b) all longs legal, making this an engine arithmetic bug? The author ruled, 2026-08-12: "a".
Read first

1. `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` — the defspec at :1160 and the file's existing deftest conventions.
2. `components/sim-engine/src/ehrt/sim_engine/` — `interface.clj` and the run implementation: find where `engine/run` receives and first uses `:seed`, and how the engine reports invalid options today (the existing error convention this fix must match — if NO invalid-option convention exists in the engine, STOP-AND-REPORT with what you found rather than inventing one).
3. `bases/cli/src/ehrt/cli/help.clj` — the two `--seed` doc strings in `cli-spec` (sim and corpus-generate groups).
4. `docs/dev/simulator-architecture.md` — Read-first standing practice for any sim-family session (ADR-0108).
5. `.agents/plans/roadmap.md` — the R8 flake-investigation row (this session resolves it).
6. `.agents/rulings.md` tail.

Step 0 — Preflight and tag ceremony

* `git fetch`; confirm `origin/main` at `c6deb5a` (`c6deb5a233477e562c3edcd2833798fab4f2719c`, ADR-0115 close). Else STOP-AND-REPORT.
* Tag `stable-20260812-review-3-rulings`, ANNOTATED, at `c6deb5a`; push; confirm peeled ref exact. License: tag-law case (i), FULLY EARNED — the design channel verified the ADR-0115 landing by fresh clone on 2026-08-12 (lineage, ASCII x3, footprint exact including the disclosed reading-set re-baseline whose arithmetic the channel re-derived independently, zero-src diff, register row flips and cluster charters content-verified) and channel-confirmed CI green on all three commits by direct API read.

Step 1 — Repro and evidence capture (NO fixes yet)
All runs from the workspace root; capture every output verbatim for the ADR.

1. The shrunk witness, directly. Evaluate the property's exact engine configuration at the shrunk seed: `(engine/run {:seed -3377439408979484 :patients 4 :pathways [...] :modules [...] :module-assignment [...] :module-horizon-days 3650})` with the pathway/module values copied verbatim from the defspec, then `check/check-all` on the result — via `clojure -M:dev -e` or an uncommitted scratch eval. Record: does it reproduce an invariant-catalog failure (expected), and WHICH invariant(s) fail, verbatim.
2. The two recorded defspec seeds. Re-run the property under `clojure.test.check/quick-check` with `:seed 7844068501` and separately `:seed 1786546687672` (150 trials each, matching the defspec). Expected: both fail, both shrink into negative-seed territory. If either does NOT reproduce, or shrinks to a NON-negative counterexample, STOP-AND-REPORT — the classification would be wrong and the ruled fix must not land on a misdiagnosis.
3. CLI reachability. Run `bin/ehrt sim run --seed -1 --patients 1 --out-dir <scratch>` and record the behavior (expected: a parse or validation error — evidence that negative seeds are unreachable from the user surface; whatever happens, record it, including if it surprisingly succeeds).
4. Red-before-green capture. Write (but do not yet commit) the new regression deftest `run-rejects-negative-seed-with-clean-error` in `engine_test.clj`: `engine/run` at `:seed -3377439408979484` (minimal config, e.g. `{:seed -3377439408979484 :patients 1}`) must return the engine's standard invalid-option error (the convention found in Read-first item 2), not a run result and not a raw throw. Run it against the CURRENT tree and capture the FAILURE — this is the red half of red-before-green.

Step 2 — The ruled fix (commit 1)
Three parts, all in one commit with the red evidence already captured:

1. Engine entry validation. In the engine's run entry point: `:seed` present and negative → the standard invalid-option error envelope (matching the existing convention exactly — category naming, payload shape, no throw). Touch nothing else in the run path; the validation is a guard clause at entry, not a refactor.
2. Contract statement. The engine's own `:seed` documentation point (interface docstring or run-options doc, whichever the engine uses) gains the contract: non-negative. The two `--seed` doc strings in `cli-spec` gain "(non-negative)" — minimal wording change — and `make cli-doc` (or the docsgen target that owns `docs/cli.md`) regenerates; the ONLY generated-doc delta must be those two rows.
3. Generator to contract. The defspec's `gen/large-integer` → `(gen/large-integer* {:min 0})`. Trial count stays 150. Nothing else in the defspec changes.

Then: the new regression deftest passes (green half — record it); the defspec runs green at both previously-failing recorded seeds (re-run the two quick-checks from Step 1.2 — both must now pass all 150 trials; record); full `make test` green.
Commit 1 (verbatim, ASCII):

```
fix: sim engine seed contract -- non-negative, validated at entry (ADR-0116)

```

Step 3 — Registers, ADR, close

* `.agents/rulings.md`, "From ADR-0116": R9 verbatim (the question as framed, options, the author's "a"), with the concrete meaning: the seed contract repo-wide is non-negative longs; the engine validates; the class of generative tests over engine options must draw from documented contracts, not raw type ranges (state this last clause as the generalizable lesson, provenance [C, channel-inferred, un-vetoed]).
* Roadmap: the R8 flake-investigation row → RESOLVED with the classification recorded (generator out of contract + engine entry unvalidated; both fixed; both recorded seeds re-run green; cross-ref: the sibling ADR-0107 corpus defspec flake row remains open and is NOT this session's scope).
* Self-archive this prompt at close-phase START.
* `notes/adr/0116-engine-seed-contract.md`: context (the two failures, the shrunk witness, the silent contract), the full Step 1 evidence (verbatim outputs: which invariants failed at the shrunk seed, the two quick-check repros, the CLI reachability probe, the red run), decision (R9 and the three-part fix), the green evidence, tag ceremony, oracle bracket, gates, fences, index line. `notes/ADRs.md` + `notes/adr/README.md` (113 → 114, as-of line).
* Roadmap Done line: `- <run date> — engine-seed-contract — ADR-0116`
* Session record.

Oracle bracket — identity argued, then verified. Pre-analysis: pure identity on all 35 roots. Argument: every oracle root uses a small positive seed; the only behavioral change is a guard clause rejecting negative seeds at entry, unreachable from any root; the cli-spec doc-string change is help text, not behavior; the generator change is test code. Run `bin/regression-oracle c6deb5a <final-commit>`; ANY non-identity is STOP-AND-REPORT — it would mean the guard touched more than the negative path.
Gates: full `make test` before EVERY push; gitleaks staged + detect; ASCII byte-check on both messages; push; CI confirm or disclose rate-limiting. The formerly-flaking defspec now draws non-negative seeds only — a generative failure in it after this fix is a NEW finding, not the known flake: STOP-AND-REPORT, do not re-run past it.
Commit 2 (verbatim, ASCII):

```
docs: session record and prompt archive -- engine seed contract (ADR-0116)

```

Fences

* Touch ONLY: the engine run entry point file (guard clause + contract docstring — name the exact file in the session record); `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` (the generator line + the new regression deftest); the two `--seed` doc strings in `bases/cli/src/ehrt/cli/help.clj`; regenerated `docs/cli.md` (two rows only); `.agents/rulings.md`; `.agents/plans/roadmap.md`; `.agents/prompts/*`; `.agents/session-records/*`; `notes/adr/0116-*.md`; `notes/ADRs.md`; `notes/adr/README.md`; plus gate-forced companions by rule (named in the record).
* The rule (ADR-0099 form): the fix's three named parts, their tests, their generated mirror, and the session's registers — nothing else. A second engine file, a refactor "while in there," or any other generated-doc delta is STOP-AND-REPORT.
* The sim purity lint's allowlist is NOT touched (a guard clause introduces no mutable state).

STOP-AND-REPORT on: any Step 1 repro contradicting the classification (non-repro, or a non-negative shrunk counterexample); no existing engine invalid-option convention to match; any generated- doc delta beyond the two `--seed` rows; oracle non-identity; a generative failure in the fixed defspec; anything this prompt failed to pre-decide.

## Deviation record

Four STOP-AND-REPORT findings surfaced, each resolved by an explicit
author ruling before this session proceeded further — see
`notes/adr/0116-engine-seed-contract.md`'s own Deviations section for
the full account, and `.agents/session-records/
2026-08-12-engine-seed-contract.md` for the execution narrative:

1. Read-first's own named STOP condition fired as designed: `engine.clj`
   had no existing invalid-option envelope to match (only a throw-based
   `:pre` clause). Ruling: adopt `ehrt.kernel.interface`'s
   result-not-throw doctrine (R10).
2. The driving prompt's own named STOP condition fired: seed
   `7844068501` did not reproduce when pinned directly, contradicting
   the evidence base's implicit claim that both recorded seeds were
   equally-confirmed repros. Ruling: proceed on `1786546687672` alone,
   without re-litigating R9.
3. Not named verbatim in the prompt but within its STOP-AND-REPORT
   spirit: the single-generator fix (as fenced) broke an unfenced
   sibling defspec, evidence the fence's own single-site scope
   undercounted the blast radius. Ruling: widen the fence to a full
   24-site repo-wide sweep.
4. Surfaced while executing the prompt's own explicit instruction to
   witness the CLI's post-fix behavior: `run-command`/
   `identifiers-command` silently swallowed the engine's new error.
   Ruling: widen the fence again to fix both callers.

The `--seed` doc-string step also found a THIRD `--seed` row in
`docs/cli.md` (`corpus generate`, not named in the prompt's "the two
--seed rows") that the prompt's own evidence base undercounted;
verified dual-source (only the `:sim` half is governed by this
session's contract) and deliberately left unedited rather than guessed
at, disclosed in the ADR.
