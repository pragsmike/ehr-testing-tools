2026-08-04 — M2: sim-engine extraction
Session prompt (design channel, 2026-08-04). Plan: `.agents/plans/2026-08-04-sim-split-b-plan.md` (RULED, AR-1..AR-6). Stage M2 of four; M1 landed and was independently verified (`f522db7..978c54f`). R30 ceremony throughout. Move-don't-improve: the interface design is this stage's one sanctioned improvement (plan R-4); everything else moves with ns-form/require diffs only. This is the plan's highest-risk stage (~1,883 src lines move) — the oracle bracket is the arbiter and it runs in split mode (AR-M2-4).
Context
`components/sim` still holds {engine 1573, churn 197, order-profiles 113} — the execution cluster — alongside the orchestration residual. This stage extracts them as `components/sim-engine` (`ehrt.sim-engine.engine` / `.churn` / `.order-profiles` + `.interface`), per the 08-02 plan's S4 scoping, proceeding under the plan's AR-4 (author override, dated notes already landed in M1). After this stage the residual's `run`, `check`, `emit_state`, and `identifiers` become src-scope consumers of `ehrt.sim-engine.interface` — Polylith law forces the interface to carry exactly their union of needs, no more. Design-channel caller evidence (var-level grep at `978c54f`, for cross-check — the session derives its own fresh):

* engine: `run`, `replay`, `patient-id-for`, `config-keys`, `assign-module`, `documented-step-rejection-reasons`
* churn: `default-churn-profile`, `sample-profile`
* order-profiles: `default-profiles`, `abnormal-flag`

Test-scope consumers (sim-emit-hl7's six files: `engine/run`, `initial-patient`, `decide`, `evolve`, `emit`, `check`, `replay`, `config-keys`; residual sim's own moved-adjacent tests) repoint to internals mechanically — they do NOT drive interface contents.
`bin/oracle-src/ehrt/oracle/digest.clj` requires `ehrt.sim.engine` directly and is always read from the current checkout (the script's own header, ADR-0030 J1/J2) — plan R-11 bites this stage; AR-M2-4 below is the workaround, not a redesign.
One resource moves: `components/sim/resources/sim/order-profiles.edn` (loaded only by order_profiles.clj, verified) → `components/sim-engine/resources/sim-engine/order-profiles.edn`, load path updated — the one behavior-adjacent edit, proven inert by the bracket. `sim/modules/**` and `sim/version.edn` STAY residual (run.clj and version.clj load them). Engine's tests load `sim/modules/*.json` cross-brick from test scope: legal, works in every project that includes both bricks (all of them — run depends on engine), recorded as an observation, not fixed.
Read first

* `.agents/plans/2026-08-04-sim-split-b-plan.md` — M2 section, R-1..R-11.
* `components/sim/src/ehrt/sim/{engine,churn,order_profiles}.clj` — what moves.
* `components/sim/src/ehrt/sim/{run,check,emit_state,identifiers}.clj` — the src-scope callers whose needs define the interface.
* `components/sim-emit-hl7/test/` — the six repointing test files.
* `bin/regression-oracle` header + `bin/oracle-src/ehrt/oracle/digest.clj` — R-11 mechanics.
* `notes/ADRs.md` ADR-0043 — the execution-record append target.
* `components/sim-emit-hl7/` S3 layout — the component-shape precedent.
* M1's session record — the parity-ledger and record format to match.

Author rulings (record verbatim in ADR-0043's M2 execution record)

1. AR-M2-1 (what moves). `engine.clj`, `churn.clj`, `order_profiles.clj` move to `components/sim-engine` as `ehrt.sim-engine.{engine,churn,order-profiles}`; their tests move with them, classified by what they test (M1's ratified split principle — Step 0 classifies every file in `components/sim/test/ehrt/sim/` and records the classification; `emitter_order_independence_test.clj` and others are classified by evidence, not filename). The order-profiles resource moves per the brick-named-subdirectory rule with its load path updated; modules and version.edn stay residual.
2. AR-M2-2 (interface shape). ONE `ehrt.sim-engine.interface`, thin delegation, three documented sections: state-reader (emit-state/identifiers surface), acceptance (check surface), orchestration (run surface). Contents = the union of src-scope caller needs, derived by fresh grep and confirmed by compilation — the design-channel list above is cross-check evidence, not the source of truth. If fresh grep finds a var the list missed, add it and record the delta; if the list names one grep can't find, record that too (both directions, unearned-specificity discipline). No var enters the interface for a test-scope caller.
3. AR-M2-3 (test-scope repoints). sim-emit-hl7's six test files and any moved-adjacent residual tests repoint mechanically: `ehrt.sim.engine` → `ehrt.sim-engine.engine` (internals, test-legal). `ehrt.sim.check` references DO NOT change — check moves in M4, not now.
4. AR-M2-4 (R-11 split-mode bracket). digest.clj's require updates to `ehrt.sim-engine.engine` as part of the move. The bracket runs in split mode: the pre-M2 side digested by the pre-M2 checkout's own digest.clj against the pre-M2 worktree, the post-M2 side by the post-M2 digest.clj against the post-M2 worktree; the two manifests diffed. Soundness condition, asserted and recorded: digest.clj's cross-side diff is ns/require-only — any logic diff voids the comparison and is STOP-AND-ESCALATE. The oracle script itself is NOT redesigned (roadmap Deferred row stands); the split-mode invocation is recorded in the session record with exact commands.
5. AR-M2-5 (M1 ratification, recorded). The author ratified (2026-08-04, design channel, post-verification) M1's three disclosed judgment calls: the corpus relay design, the 9/6 schema/builder test split, and `valid?`'s retirement. Append a dated ratification line to ADR-0043's execution record.
6. AR-M2-6 (stale-path fan-out). Extend the stale-path tripwire for `ehrt.sim.engine`, `ehrt.sim.churn`, `ehrt.sim.order-profiles` (and path-form `ehrt/sim/engine` etc.) on current-tense surfaces; update the docstring mentions in sim-model (`persona.clj`, `pathway.clj`), sim-trajectory (`gmf_interpreter.clj`), sim-emit-hl7 (`emit_hl7.clj`), and any others fresh grep finds. Frozen archives untouched. Red→green moment recorded for the tripwire extension.

Steps
Step 0 — Characterize (evidence, no edits). Verify tip = `978c54f` (STOP-AND-ESCALATE on mismatch). Fresh var-level grep of every `engine/`, `churn/`, `order-profiles/` call site across src, test, dev, and bin trees; diff against the design-channel list above per AR-M2-2, record both-direction deltas. Classify every `components/sim/test/ehrt/sim/*.clj` file (moves vs stays) by what it tests, with per-file one-line rationale. Authoritative deftest counts per tree (form-anchored: `^\(deftest `/ `^\(defspec `). Record all of it in the session record before any edit.
Step 1 — Move churn + order-profiles (the leaf slice). Create `components/sim-engine` (S3 layout precedent): move `churn.clj`, `order_profiles.clj`, their classified tests, and the order-profiles resource (path updated). Write `ehrt.sim-engine.interface` carrying the churn/order-profiles vars from the caller union. Repoint every caller in one commit: residual sim src (`engine.clj`'s churn/order-profiles requires → `ehrt.sim-engine.churn`/`ehrt.sim-engine.order-profiles`? NO — engine is still residual-side this step, and src-scope cross-component requires must go through the interface: engine.clj repoints to `ehrt.sim-engine.interface` for this one commit, then moves itself in Step 2; if the interface union lacks a var engine needs (engine-internal churn/order-profiles calls beyond the residual union), add it, record it, and note whether Step 2's move makes it removable — record the decision either way, don't silently shrink). Workspace bookkeeping: root `deps.edn` `:dev`+`:test`, every project including sim, `workspace.edn`, `:necessary`, structure-currency. `poly check` clean, full suite green, both lanes. Commit: `refactor(sim-engine): churn and order-profiles move — leaf slice first (M2 step 1, AR-M2-1)`
Step 2 — Move engine. `engine.clj` + its classified tests move; interface completes (engine vars from the union; any Step 1 engine-only additions re-derived — if now removable, remove and record). Residual src callers (`run`, `check`, `emit_state`, `identifiers`) repoint to `ehrt.sim-engine.interface`; sim-emit-hl7's six test files repoint per AR-M2-3; `digest.clj` require updates per AR-M2-4; `development/` and any other callers fresh grep found in Step 0 repoint. Old namespaces deleted in the same commit (no duplicate-ns window on the dev classpath). `poly check` clean, full suite green, both lanes. Commit: `refactor(sim-engine): engine moves — interface complete, residual repoints (M2 step 2, AR-M2-1/2/3)`
Step 3 — Stale-path sweep + tripwire (AR-M2-6). Docstring mentions updated per fresh grep; tripwire patterns extended; red→green recorded. Commit: `docs: sim-engine stale-path sweep — tripwire learns the old names (M2 step 3, AR-M2-6)`
Step 4 — ADR execution record + dated notes (AR-M2-5). Append M2's dated execution record to ADR-0043 (what moved, the interface union with its evidence deltas, the resource move, the split-mode bracket plan); append the M1 ratification line; roadmap M2 row updated. Commit: `docs: ADR-0043 M2 execution record + M1 ratification note (M2 step 4, AR-M2-5)`
Step 5 — Verification + record. Split-mode bracket per AR-M2-4: pre-side digest from `978c54f`'s checkout against `978c54f`'s worktree, post-side from Step 4's tip against its worktree, manifests diffed — all ELEVEN batches byte-identical, expected-change set NONE (the resource path move must be invisible in output; any digest change is STOP-AND-ESCALATE). Assert and record digest.clj's cross-side diff is ns/require-only. Deftest parity ledger vs Step 0 (pure wash expected — nothing retires, nothing new this stage). Façade seam: `ehr help` / `ehr sim run` / `ehr sim check` byte-identical (M1's worktree-to-worktree method). `clojure -M:poly check` clean; both lanes green. Session record `.agents/session-records/2026-08-04-sim-split-m2-engine.md`; prompt self-archives to `.agents/prompts/` (same slug); the pairing gate now enforces this mechanically. Final commit: `docs: M2 session record — sim-engine extracted, bracket identical in split mode`
Fences
No check or emit-state moves (M3/M4). No behavior changes — the resource path move is the single disclosed behavior-adjacent edit, and the bracket must prove it inert. No oracle redesign (split-mode invocation only; the Deferred row is the fix's home). Façade (`ehrt.sim.interface`) byte-untouched. No interface vars beyond the src-caller union (both-direction deltas recorded). No engine logic edits of any kind — if engine code looks wrong during the move, it is a FINDING for the record, never an edit. Frozen archives untouched.

---

No STOP-AND-ESCALATE fired (tip matched `978c54f` at Step 0). Judgment
calls and disclosed findings made without being spelled out verbatim in
this prompt — see `.agents/session-records/2026-08-04-sim-split-m2-
engine.md`'s own "Judgment calls" and "Findings" sections for the full
account: interface-section placement for shared vars (`replay`,
`config-keys`), the Step 1 double-alias transitional technique,
`emitter_order_independence_test.clj`'s STAYS classification, the
dependency-direction correction to ADR-0043's own M1-era note (`kernel`
guessed, `sim-trajectory` found), `explain-profiles`'s dead-code status
left unfixed, and one pre-existing (unrelated) test.check flake
observed and confirmed non-reproducing.

Verification: the split-mode regression-oracle bracket (`978c54f` vs
`77a9a72`, each side digested by its own `digest.clj` against its own
worktree) reported all eleven batches byte-identical; façade seam
(`ehr help`/`sim run --format ground-truth`/`sim check`) confirmed
byte-identical via two disposable worktrees, no qualification needed;
deftest/defspec parity ledger balanced (212 = 212, pure wash). Pushed
`9ccc04f..77a9a72` across four commits (three numbered steps plus this
record's own final commit).
