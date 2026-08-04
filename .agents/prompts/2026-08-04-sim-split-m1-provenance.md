# 2026-08-04 — M1: provenance component, mirror retirement, vestige sweep

Session prompt (design channel, 2026-08-04). Plan: `.agents/plans/2026-08-04-sim-split-b-plan.md` (RULED, AR-1..AR-6). This is stage M1 of four. R30 ceremony applies throughout: commit and push at every checkpoint unattended, staging hygiene, gitleaks, message-via-file, post-push verification. Move-don't-improve: schemas move verbatim (ns form and require paths are the only permitted diffs in moved forms); no interface narrowing, no CLI surface changes, no M2+ work of any kind.

## Context

The sim/tools consolidation left `components/sim/src/ehrt/sim/ manifest.clj` mirroring corpus's ManifestV1_1 "without depending on it" — a dependency-direction rule from the separate-repo era, now a fossil: both live in one workspace, and corpus → sim already exists (`sim_adapter` requires the sim façade, ADR-0012), so the acyclic single home for the schema is a new component both depend on. The author ruled it `provenance` (plan AR-2). The mirror's own docstring records why the tripwire could never catch its own drift (M3 Task 0 lesson); that text is the retirement disclosure's citation. The conformance-project contract test survives repointed — its "binding half of the mirror" rationale retires, its end-to-end builder-validity substance does not (AR-5(b) as refined in the design channel 2026-08-04). The vestige sweep and the AR-4 dated notes ride this session.

## Read first

* `.agents/plans/2026-08-04-sim-split-b-plan.md` — the whole plan; this session executes M1 only.
* `components/corpus/src/ehrt/corpus/manifest.clj` — the authoritative schemas (ManifestV0/V1/V1_1) and builders; only the schemas + validators move.
* `components/sim/src/ehrt/sim/manifest.clj` — the mirror; its docstring's M3-Task-0 lesson is quoted in Step 4's disclosure.
* `projects/conformance/test/ehrt/conformance/sim_manifest_contract_test.clj` — survives repointed; docstring rewritten with a dated note.
* `components/corpus/src/ehrt/corpus/interface.clj` — the ManifestV1_1 re-export ("test-consumer only") repoints or retires in Step 3.
* `.agents/plans/2026-08-02-sim-split-plan.md` and `.agents/plans/roadmap.md` (S4 row) — receive AR-4's dated notes, annotated never rewritten.
* `notes/ADRs.md` ADR-0042 tail — the numbering context for the new split ADR.
* AGENTS.md; the stale-path tripwire (`components/docs-tooling/test/ehrt/docs_tooling/stale_path_test.clj`).

## Author rulings (record verbatim in the ADR)

1. AR-M1-1 (what moves). ManifestV0, ManifestV1, ManifestV1_1 and their `valid?`/`valid-v1?`/`valid-v1-1?` predicates move verbatim from `ehrt.corpus.manifest` to `ehrt.provenance.manifest`, exposed through `ehrt.provenance.interface`. Builders stay producer-side: corpus keeps `build`/`build-v1-1`; sim keeps `build`. The frozen V0/V1 history moves with the schemas (it is schema history, not builder history) — docstrings intact.
2. AR-M1-2 (mirror retirement). `MirroredManifest` and `components/sim/test/ehrt/sim/manifest_test.clj`'s tripwire tests retire. The retirement disclosure (dated, in the ADR and the session record) quotes the mirror docstring's own lesson: a mirror validating its own output against its own schema copy agrees with its own mistakes. `ehrt.sim.manifest/valid?` repoints to `ehrt.provenance.interface/valid-v1-1?` or retires if nothing requires it — decide from fresh grep, record which.
3. AR-M1-3 (contract test, AR-5(b) refined). The conformance test survives, repointed to `ehrt.provenance.interface/ManifestV1_1`; its docstring is rewritten with a dated note: the binding-half rationale retires with the mirror, the end-to-end run-sim-validate-manifest substance is the test's continuing purpose. Additionally, one plain unit test lands next to sim's `build`: `(provenance/valid-v1-1? (manifest/build <minimal args>))` — fast-lane builder validity without the harness.
4. AR-M1-4 (intake front door). The split ADR records the doctrine: sim runs enter `ehr corpus intake` as if foreign; the discipline has caught real defects and survives the consolidation deliberately. No code change — this is a written-down rule.
5. AR-M1-5 (AR-4 notes). Roadmap S4 row and the 08-02 plan each get a dated note, framing (b) verbatim from the plan's AR-4: author override plainly stated, trigger's reasoning honored in substance (M3 committed scope), no claim the trigger fired.
6. AR-M1-6 (sweep scope). Vestige sweep touches current-tense surfaces only: `components/corpus/src/ehrt/corpus/sim_adapter.clj`, its test, `intake_test.clj`, `bases/cli/src/ehrt/cli/core.clj`, `docs/dev/way-of-working.md`, plus docstring-level cross-repo/pack-push language found by fresh grep (`ehr-testing-sim`, `pack-push`, `make pack`). Frozen archives (`notes/`, `.agents/session-records/`, sealed prompts) untouched. Each file: per-file judgment, dated note where meaning changes, silent fix only for mechanically stale paths.
7. AR-M1-7 (pairing gate). The prompt/record pairing invariant — every session record paired with its archived prompt, ADR-0023's own convention — gets enforcement: a docs-tooling deftest, both directions, the seven pre-cutover session-record slugs allowlisted (their prompts live in frozen `notes/prompts/` under the older `ehr-testing-` prefix, and that directory can never receive renamed copies — `notes_prompts_frozen_test` pins its set). Until now every session remembered to archive; nothing made forgetting fail.

## Steps

Step 0 — Characterize (evidence, no edits). Fresh recursive grep: every consumer of `ehrt.corpus.manifest` and `ehrt.sim.manifest` (requires AND docstring mentions, listed separately); authoritative deftest/defspec count across `components/sim/test` and `components/corpus/test` and the conformance project (the plan's 229 grep-count is provisional — this count is the parity baseline). Record in the session record. Verify the tip you're on matches the author's stated tip; STOP-AND-ESCALATE on mismatch.
Step 1 — Create `components/provenance`. New component: `ehrt.provenance.manifest` (schemas + validators, moved verbatim per AR-M1-1) and `ehrt.provenance.interface` (thin delegation). Move the corresponding schema tests from `corpus/test/.../manifest_test.clj` into provenance's test tree (builder tests stay corpus-side — split the file by what it tests). Workspace bookkeeping: root `deps.edn` `:dev` + `:test`, every project `deps.edn` that includes corpus or sim, `workspace.edn`, `:necessary` re-derived, structure-currency red→green on the new directory. `poly check` clean, full suite green. Commit: `feat(provenance): manifest schemas move to their single home (split plan M1 step 1, AR-M1-1)`
Step 2 — Repoint corpus. `ehrt.corpus.manifest` drops the moved defs, requires `ehrt.provenance.interface`, keeps builders; `generate.clj`/`intake.clj` requires unchanged if they only touch builders and `valid-v1-1?` — repoint what fresh grep says, nothing more. `corpus/interface.clj`'s ManifestV1_1 re-export repoints to provenance (kept for now — the conformance test migrates off it in Step 3; retire the re-export only if Step 3 leaves it consumer-less, and record which). Green. Commit: `refactor(corpus): manifest schemas resolve from provenance; builders stay home (M1 step 2)`
Step 3 — Contract test repoint (AR-M1-3). Conformance test requires `ehrt.provenance.interface` directly; docstring rewritten with the dated note; field-level assertions unchanged. Add the sim-side builder-validity unit test. Green in both lanes. Commit: `test: contract test repoints to provenance — binding-half rationale retires with the mirror (M1 step 3, AR-5(b) refined)`
Step 4 — Mirror retirement (AR-M1-2). `ehrt.sim.manifest` thins to build-only (+ `environment`), requiring `ehrt.provenance.interface`; MirroredManifest and the tripwire tests retire with the dated disclosure quoting the M3-Task-0 lesson. Parity ledger updated: name the retired tests and the Step 3 replacement explicitly. Green. Commit: `refactor(sim): manifest mirror retires — the tripwire that could not catch itself (M1 step 4, AR-M1-2)`
Step 5 — Vestige sweep (AR-M1-6) + pairing gate (AR-M1-7). Per-file, current-tense only, per the ruling. Check the stale-path tripwire's patterns still pass and extend only if the sweep introduces a newly-stale pattern class (record red→green if so).
Then add the prompt/record pairing gate: a new docs-tooling deftest asserting every `.agents/session-records/*.md` (README excluded) has a same-slug `.agents/prompts/*.md`, with the seven pre-cutover record slugs pinned as an allowlist (their prompts live in frozen `notes/prompts/` under the older `ehr-testing-` prefix and can never move — `notes_prompts_frozen_test` pins that set); assert the reverse direction too (every archived prompt has its record — holds today). Freshness-gate pattern, same shape as the frozen-set and exact-token-both-directions gates. Red→green moment: write the test listing the allowlist, watch it pass, record it. Derive the allowlist from a fresh directory diff at execution time, not from this prompt's enumeration — record the derived list in the session record.
Commit: `docs: two-repo vestige sweep + prompt/record pairing gate — disciplines written down and enforced (M1 step 5, AR-M1-6/7)`
Step 6 — ADR + dated notes (AR-M1-4, AR-M1-5). Write the split ADR (next number after ADR-0042): dependency directions (`provenance` ← {corpus, sim}; forbidden-forever: provenance depends on nothing but kernel-tier libs and malli), intake-front-door doctrine, plan AR-1..AR-6 and this prompt's AR-M1-* verbatim, citing the plan. Roadmap: S4 row's AR-4 dated note; M1 row moves toward Done with this session's record; M2–M4 rows present in Now per the plan. 08-02 plan annotated. Commit: `docs: split ADR — provenance directions, intake front door, AR-4 reconciliation (M1 step 6)`
Step 7 — Verification + record. `bin/regression-oracle <pre-session-tip> <step-6-tip>`: all ELEVEN batches byte-identical (expected-change set: none; any digest change is STOP-AND-ESCALATE — this session must not change behavior). Deftest parity vs Step 0's authoritative count, ledger balancing retired tripwire tests against the Step 3 unit test. Façade seam: `ehr sim run`, `ehr sim check`, `ehr help` byte-identical to pre-session output. `clojure -M:poly check` clean; both lanes green. Session record to `.agents/session-records/2026-08-04-sim-split-m1-provenance.md`; self-archive this prompt to `.agents/prompts/` per convention. Final commit: `docs: M1 session record — provenance landed, mirror retired, sweep complete`

## Fences

No engine/check/emit-state moves (M2–M4). No schema field changes — the move is verbatim; if a schema looks wrong, that is a FINDING for the record, never an edit. No CLI surface changes. No façade changes. No interface narrowing anywhere. The oracle's read-from-current- checkout defect (plan R-11) is not fixed here unless the bracket literally cannot run — in which case use the documented workaround and record it, don't redesign.

---

Executed 2026-08-04 in the Windows-native Claude Code session (`ehr-testing-tools` checked out under `C:\Users\prags\Documents\ehr-testing-tools`, all git/poly/test operations routed through WSL into the ext4 clone of record, `~/src/ehr-testing-tools`, per `.agents/skills/build-session/SKILL.md`'s own preflight). Tip at session start: `f522db7` ("Add sim split b plan."), matching the plan's own stated survey commit `8f697f7` plus one (the plan file itself) — no mismatch, no escalation needed. Recorded `notes/ADRs.md` ADR-0043.

No STOP-AND-ESCALATE fired. Two judgment calls made without being spelled out verbatim in this prompt, both disclosed rather than silently decided — see `.agents/session-records/2026-08-04-sim-split-m1-provenance.md`'s own "Judgment calls" section for the full account:

- **`ehrt.corpus.manifest`'s relay design** (Step 2): every schema/
  validator def there re-exports the SAME var provenance defines
  (`(def ManifestV1_1 provenance/ManifestV1_1)`), rather than deleting
  the names and repointing every consumer — read from this prompt's
  own "repoint what fresh grep says, nothing more" as license for zero
  consumer churn, not spelled out as the specific mechanism.
- **`corpus/manifest_test.clj`'s 15-test split** (Step 1, "split the
  file by what it tests"): 9 classified schema/validator (moved,
  rewritten against literal fixture maps — provenance can't depend on
  corpus) vs. 6 builder (stayed). The per-test classification is a
  judgment call this prompt left to the executing session.

Verification: `bin/regression-oracle f522db7 a9154d8` reported
IDENTICAL across all eleven batches; façade seam (`ehr help`/`sim run`/
`sim check`) confirmed byte-identical via two disposable worktrees;
deftest/defspec parity ledger balanced (see session record). Pushed
`f522db7..a9154d8` across seven commits (six numbered steps plus this
record's own final commit).
