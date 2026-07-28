Prompt: the consumer loop — ehr-testing-tools consumes a sim corpus
Paste into a Claude Code session (Sonnet, high reasoning) in the ehr-testing-tools repo root (NOT sim), in WSL. Sibling checkout at `../ehr-testing-sim`. This repo's own AUTHORS-GUIDE, ADR rules, and test-first discipline (its ADR-0006) apply — read its AGENTS.md first and follow ITS conventions, which differ in places from sim's.
Goal: the first cross-repo ecological loop. Two long-standing debts retire in one session: (1) the binding manifest contract test that sim's ADR-0001 assigned to THIS repo's integration tree at scaffold time and that has never been built; (2) sim's validation claim #6 (fitness as a test instrument) gets its first motion — `ehr gate` judging sim-generated traffic.
Coupling rule (load-bearing): do NOT add ehr-testing-sim to this repo's classpath or deps.edn. Sim is a private repo; this repo is public, and a git/classpath dependency would break public CI and invert no arrows but tangle versions. Consume sim as a subprocess: invoke its CLI in the sibling checkout (`cd ../ehr-testing-sim && clojure -M:cli run ...`), capture its EDN output. All tests this session writes live under this repo's integration tree (the scheduled, non-blocking suite — see .github/workflows/integration.yml and the repo's staged-enforcement doctrine), and every test SKIPS cleanly with an explanatory message when `../ehr-testing-sim` is absent — public CI must stay green without the sibling.
Read first: this repo's `AGENTS.md`, `AUTHORS-GUIDE.md`, `notes/ADRs.md` (esp. its ADR-0004 result doctrine and ADR-0006 staged enforcement), `src/ehr_testing_tools/corpus/manifest.clj` (ManifestV1_1 — the authoritative schema), the gate/judge namespaces and their existing integration tests for harness conventions, and from the sibling: `../ehr-testing-sim/notes/ADRs.md` ADR-0001 clause 5 (why this test lives here), `../ehr-testing-sim/src/ehr_testing_sim/manifest.clj` (the mirror this contract test checks against drift), and sim's README for CLI invocation.
Task 1 — Sim-run harness (integration tree)
A small helper: run sim's CLI as a subprocess with fixed `--seed`/`--patients` (and optionally `--churn --emit hl7`), parse the EDN Result, fail with a clear message on nonzero exit, and skip (not fail) when the sibling directory is missing. Follow this repo's existing subprocess/injection conventions (`:run-invocation`-style fakes for the unit level if any unit tests accompany; the integration tests use the real thing).
Task 2 — The binding manifest contract test
Red first (against a stub harness if needed): sim run → extract `:manifest` from the payload → `m/validate` against THIS repo's authoritative `ManifestV1_1`. This is the drift tripwire sim's mirror cannot provide for itself. Assert also the fields intake cares about (`:stage`, `:generator :name/:version`, `:seeds`). If validation fails against current sim output, that is a FINDING — report the exact mismatch; do not bend the schema silently.
Task 3 — The gate loop (claim #6's first motion)
sim run with `--emit hl7` (churn on, fixed seed) → write each message to a `.hl7` file in a temp corpus dir → run `ehr gate` (v2 arm) over it via this repo's normal gate invocation path.
Assertion discipline — read carefully: the test asserts that the gate RUNS, produces a verdict per file, and the report is well-formed. It does NOT assert all-pass. Sim's v0 messages are minimal (bare PID, sparse MSH); the gate may legitimately reject some — rejections are the ecological findings this loop exists to produce. Write the verdict report to a committed baseline artifact (per this repo's report/baseline conventions), summarize every :rejected/:indeterminate finding in the session summary with the gate's stated reason, and — without fixing anything in sim — draft the list as a triage block the author will carry back to the sim repo (e.g. "MSH-9 message structure component missing", whatever the gate actually says). Subsequent runs compare against the baseline so sim-side fixes show up as verdict deltas.
Task 4 — Intake trial
`ehr corpus intake` the sim corpus (messages + manifest). Assert the catalog entry lands and the manifest's provenance fields survive intake. Any impedance mismatch between what intake expects and what sim's Package-less output provides is a FINDING for the summary, not something to paper over.
Task 5 — Record it (this repo's conventions)
An ADR in THIS repo's notes/ADRs.md (its numbering, its format): the cross-repo consumer loop — subprocess coupling (and why not classpath), skip-when-absent, findings-not-failures assertion discipline, baseline-delta workflow. Note that mounting `ehr sim` as a subcommand (sim ADR-0001's embedding contract) remains DEFERRED until the classpath question resolves (sim going public or a private-dep mechanism) — recorded so it isn't re-litigated. Update this repo's integration workflow docs/comments if the suite gains a new entry point.
Verification (report all)

1. Red evidence for Tasks 2–4.
2. Full integration run WITH the sibling present: contract test green (or the mismatch finding), gate verdict summary with counts (pass/rejected/indeterminate) and the triage block, intake result.
3. The suite with the sibling ABSENT (rename the dir temporarily): all new tests skip with clear messages; nothing fails.
4. This repo's own unit suite untouched and green; its ceremony (per ITS AUTHORS-GUIDE — commit → push; pack-push is dormant here) completed.
5. Confirm nothing was added to deps.edn.

End the summary with the sim-side triage list: every finding (gate rejections, manifest mismatches, intake impedance) phrased as actionable items for the sim repo, since that is where fixes land.

---

## Session deviation record (added post-execution, per AUTHORS-GUIDE.md section 7)

Task 3's own prompt text anticipated the gate "may legitimately reject
some" and asked for a triage block of rejections. Measured directly
(seed 42, `--patients 20 --churn --emit hl7`, 43 messages): **every**
message passed judge.v2's base-structural tier -- zero rejections, zero
findings of any kind. This is not a test-authoring gap: the prompt's own
assertion discipline ("does NOT assert all-pass") already anticipated
that the true count could land anywhere, including zero. The finding is
reported honestly below rather than a rejection being manufactured to
satisfy the anticipated shape of the triage block. See the session
summary and notes/ADRs.md ADR-0013 (decision 3, rejected alternatives)
for the full reasoning.
