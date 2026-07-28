2026-07-27 — Verification tiers: stop paying the integration suite's price per commit

Context
`make integration` costs 19m11s locally (recorded, DOC-4) and 10m29s in CI cold-cache (ENF-1, run 30175880198), dominated by per-file `validator_cli.jar` subprocess launches (~1–2 min JVM startup each; a single `gate fhir` strip recorded at 99s). The suite's placement is already right — path-split from the hermetic suite, pre-push runs fast gates only, nightly CI runs integration — but the session discipline still demands it constantly: `.agents/plans/judge-gate-refactor.md` mandates "full test suite + both integration suites" after each commit, and recent plan rows treat "integration green throughout" as per-session ritual. This session establishes explicit verification tiers with change-aware triggers, adds a sub-2-minute integration-smoke tier, and lands two integrity-preserving cost reductions: a content-hash-keyed verdict cache and (if the probe supports it) batched validator invocation. Integrity is not negotiable: the full suite still runs nightly, at release gates, and whenever the change set touches the namespaces that integration uniquely exercises.
Read first

* `deps.edn` `:test` / `:integration` aliases; `Makefile` `test` / `integration` targets; `.githooks/pre-push`
* `test-integration/ehr_testing_tools/contract_pairing_test.clj` and `sim_harness.clj` (cost structure: how many validator subprocesses, how many sim subprocesses)
* `src/ehr_testing_tools/judge/fhir.clj` (validator invocation seam), `invocation.clj`, `digest.clj`
* `.agents/plans/judge-gate-refactor.md` (the "after each commit" rule this session amends), `AGENTS.md` §integration
* `notes/ADRs.md` ADR-0006 (staged enforcement)

Author rulings

1. Three tiers, named once, referenced everywhere.
   * T0 fast gates (exists, unchanged): `make test` + both lints + `quickstart-fresh`. Pre-push hook and per-commit verification.
   * T1 integration-smoke (new, target < 2 min): ONE real validator invocation over a tiny fixed corpus (one known-clean Synthea file + one mutant with a known conviction), asserting the pairing polarity only; plus ONE `sim_harness` run (fixed seed, manifest validates), skip-when-absent as today. Session boundaries and integration-adjacent commits.
   * T2 full integration (exists, unchanged in content): nightly CI, release gates, and change-triggered per ruling 2.
2. Change-aware trigger, stated as text not machinery. T2 is required in-session only when the change set touches: `judge/fhir.clj`, `judge/v2*`, `invocation.clj`, `artifact.clj`, `corpus/generate.clj`, anything under `test-integration/`, the `:integration` alias, or `.github/workflows/`. Everything else owes T0 per commit and T1 at session close; nightly T2 is the backstop. Encode this list in `AGENTS.md` and amend `judge-gate-refactor.md`'s verify-after-each-commit line (append-only where the plan's conventions require; otherwise edit with a dated note). A `bin/needs-integration` helper (diff changed paths against the list, exit 0/1) MAY be added if trivial; the text is the authority either way.
3. Verdict cache is content-addressed and inert on miss. Key = SHA-256 of (file content hash, validator artifact name+version+sha, IG/profile set, argv shape, `verdict-mapping-version`). Value = the judge's finding set + verdict, EDN. Location: `target/verdict-cache/` (gitignored). Cache hit skips the subprocess; miss behaves exactly as today. Determinism of the validator given identical inputs is the assumption that makes this sound — state it in the ADR, and note the escape hatch (`--no-verdict-cache` / deleting the directory). Any key-component omission that could alias two distinct judgments is a correctness bug, not a tuning knob: when in doubt, widen the key.
4. Batch invocation is probed before claimed. Step 4 empirically tests whether the pinned `validator_cli.jar` accepts multiple files or a directory per invocation and whether per-file findings remain attributable. If yes: batch within contract-pairing where polarity assertions permit, and record measured before/after wall time. If no, or attribution is lossy: record the probe result in the facts register and do NOT batch — the cache (ruling 3) is the primary win; batching is opportunistic.
5. No test deletion, no assertion weakening. T1 is a new small suite (or a marked subset), not a thinning of T2. T2's content is untouched this session.
6. Numbers or it didn't happen. The final report states measured wall times: T0, T1, T2-with-cold-cache, T2-with-warm-verdict-cache, on this machine. Improvements are claimed only from these measurements.

Steps
Step 0 — Baseline measurement
With artifacts fetched (`ehr artifact fetch` per `make help`), time `make test` and `make integration` once each (`time`, wall clock). Record in scratch; these are the "before" numbers for ruling 6. If the environment cannot run integration (no artifacts/network), STOP and report — this session's claims are measurement-backed or not made.
Step 1 — Verdict cache
Implement per ruling 3 at the `judge.fhir` validator-invocation seam (and `judge.v2`'s external seam only if one exists — probe; if v2 is in-process, it needs no cache and the ADR says so). Unit tests (hermetic, injected fake): hit/miss behavior, key sensitivity to every component (content, validator identity, args, mapping version), cache disabled flag. Wire `--no-verdict-cache` through the CLI where `gate` verbs invoke the validator.
Commit: `judge: content-addressed verdict cache at the validator seam (ADR-00NN)`
Step 2 — ADR
Append ADR-00NN (next number): the cache decision, key composition, determinism assumption, escape hatches, and the tier policy of rulings 1–2 (the ADR is the reasoning-of-record; AGENTS.md is the working rule).
Commit: `adr: verdict cache + verification tiers`
Step 3 — T1 integration-smoke
New `make integration-smoke` target and (if cleanest) an `:integration-smoke` alias or a `:dirs`/namespace selection over `test-integration/` — follow the repo's existing path-split philosophy: prefer a dedicated small namespace (`test-integration/ehr_testing_tools/smoke_test.clj`) over tag filtering. Content per ruling 1. Time it; if over 2 minutes with a warm verdict cache, shrink the corpus, not the assertions.
Commit: `test: integration-smoke tier — one real validator pairing + one sim harness run, sub-2-minute`
Step 4 — Batch-invocation probe (and adoption only if clean)
Per ruling 4. Probe against the real pinned validator with 3 files of known distinct verdicts; verify findings attribute per-file. Adopt in contract-pairing only if attribution is exact. Record the probe as a facts-register row either way.
Commit (adopting): `judge: batch validator invocation in contract pairing (probe: facts F-NN)` Commit (not adopting): `notes: facts F-NN — validator batch-invocation probe result; batching not adopted`
Step 5 — Discipline amendments
`AGENTS.md`: a "Verification tiers" section stating rulings 1–2 (tier definitions, trigger list, nightly backstop). `judge-gate-refactor.md`: amend the after-each-commit line per the plan file's own conventions (dated note). `Makefile` help text gains `integration-smoke`. Pre-push hook is UNCHANGED (T0 already correct).
Commit: `docs: verification tiers — T2 is change-triggered and nightly, not per-commit ritual`
Step 6 — Measure after
Re-time: T1, T2 cold verdict cache, T2 warm verdict cache. Full suite + both lints + freshness pair green. Record all numbers.
Step 7 — Archive this prompt
To `.agents/prompts/archive/`, deviation appendix if any.
Commit: `prompts: archive 2026-07-27 verification-tiers session`
Final report
Before/after wall times (T0/T1/T2 cold/T2 warm), cache hit behavior evidence, batch probe result, list of discipline texts amended, deviations.

## Session deviation record (author ruling, 2026-07-27)

* **ADR/F numbers.** `notes/ADRs.md`'s last entry at session start was ADR-0015; this session's ADR landed as **ADR-0016**. `notes/facts-register.md`'s last row was F28; the batch-invocation probe landed as **F29**. Both read directly from the committed files rather than assumed.
* **Ruling 1's "ONE real validator invocation" (T1).** Read as "one clean/mutant pairing" (two files gated, not a literal single subprocess call) rather than literally one subprocess launch — a true single-subprocess pairing would need T1 to depend on Step 4's batching landing first, which the step ordering (Step 3 before Step 4) does not guarantee. The 2-minute target is met on the WARM path (47.9s measured) via the verdict cache from Step 1, which is what ruling 1's own "shrink the corpus, not the assertions... if over 2 minutes with a warm verdict cache" phrasing already anticipates — cold T1 (first run in a session) measured 4m0s, over budget, and is reported as such rather than adjusted toward the target.
* **T1's sim-harness assertion.** "manifest validates" was read as the SAME binding check `sim_manifest_contract_test.clj` already performs (`m/validate corpus.manifest/ManifestV1_1`), at reduced depth (schema validation only, not that suite's own field-by-field assertions) — reusing the existing contract rather than inventing a new, weaker one.
* **Ruling 4 batch scope.** "Adopt in contract-pairing where polarity assertions permit" was read literally: `judge.fhir/gate-batch` was built as a general capability, but only `contract_pairing_test.clj` was rewritten to use it. `baseline_gating_test.clj` (also FHIR-validator-heavy) was left unbatched — its own two `gate-file` calls stay separate, out of this session's stated scope.
* **Batch per-file attribution, verified beyond the ruling's own 3-file ask.** The initial 3-file probe used bare filenames (no directory component), which cannot distinguish "validator echoes the exact argv string" from "validator echoes the basename." A second, smaller probe (2 files under a subdirectory) confirmed the former — `judge.fhir/gate-batch` matches results back to callers by the EXACT argv string, never a re-derived basename, and this distinction is recorded in F29 and the `gate-batch` docstring.
* **`--no-verdict-cache` wiring.** Wired only through `ehr gate fhir` (the one verb with a real subprocess to skip); `ehr gate v2` gained no such flag, consistent with judge.v2 needing no cache (ADR-0016).
