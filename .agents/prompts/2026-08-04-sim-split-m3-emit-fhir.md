2026-08-04 — M3: sim-emit-fhir extraction
Session prompt (design channel, 2026-08-04). Plan: `.agents/plans/2026-08-04-sim-split-b-plan.md` (RULED). Stage M3 of four; M1 and M2 landed and were independently verified (`f522db7..c037f37`). R30 ceremony throughout. This is the smallest stage: one namespace (267 lines), one interface var, and the AR-3 rename — the one sanctioned improvement this stage licenses. It also discharges plan AR-4's committed-second-consumer claim: with this landing, `sim-engine` has two consumers (`sim-emit-hl7` reads events; `sim-emit-fhir` reads folded state) and S4's trigger reasoning is honored in full substance.
Context
`emit_state.clj` is the state-based FHIR emitter: fold the ground-truth log via `engine/replay`, snapshot at an instant, render Bundles. Per plan AR-3 it becomes `components/sim-emit-fhir`, ns `ehrt.sim-emit-fhir.emit-fhir` (S3 precedent), sibling to `sim-emit-hl7` with `sim-emit-cda` the named-future third sibling. The siblings are peers as rendering accents, NOT same-shaped: hl7 renders per-event from the log; fhir (and future cda) render from folded state — the existing contract note in emit_state's docstring carries forward, not rewritten.
Design-channel caller evidence (call-position grep at `c037f37` — alias-plus-call-site, not bare tokens; the session re-derives fresh):

* src-scope union for the interface: `bundle-run` only (`run.clj:345`, `identifiers.clj:128`).
* `snapshot-at` has NO src-scope caller outside emit_state itself — its mentions in `identifiers.clj` (lines 36, 74) are docstrings. It stays internal; test-scope reaches it directly.
* test-scope callers: `emit_state_test.clj` (moves with the ns), `identifiers_test.clj:17,90` (repoints).
* `org.clojure/data.json`: declared in `components/sim/deps.edn`; its ONLY real user in sim is `emit_state_test.clj` (the src mention is a docstring). `bases/cli` declares its own copy independently.
* `emitter_order_independence_test.clj` tests emit-hl7 determinism (guard test, mining group B) — it does NOT move; its residency is recorded as a classification finding for M4.

Oracle status, verified at tip: `bin/regression-oracle`'s synthetic classpath heredoc still lists `poly/sim` but not `poly/sim-engine`, while `digest.clj` has required `ehrt.sim-engine.engine` since M2 — the script as-shipped cannot resolve on any post-M2 ref. M2's split-mode invocation worked around this ad hoc and correctly left the script alone per its own fence. AR-M3-4 rules the minimal fix. `digest.clj` itself does not require emit-state and needs NO change this stage.
Read first

* `.agents/plans/2026-08-04-sim-split-b-plan.md` — M3 section.
* `components/sim/src/ehrt/sim/emit_state.clj` — what moves (and its sibling-contract docstring, which carries forward).
* `components/sim/src/ehrt/sim/{run,identifiers}.clj` — the two src-scope callers.
* `components/sim/test/ehrt/sim/{emit_state_test,identifiers_test}.clj` — the test movers/repointers.
* `components/sim-emit-hl7/` — the sibling layout this component mirrors.
* `bin/regression-oracle` deps heredoc — AR-M3-4's target.
* `notes/ADRs.md` ADR-0043 M2 execution record — the format to match.

Author rulings (record verbatim in ADR-0043's M3 execution record)

1. AR-M3-1 (what moves, and the rename). `emit_state.clj` → `components/sim-emit-fhir/src/ehrt/sim_emit_fhir/emit_fhir.clj`, ns `ehrt.sim-emit-fhir.emit-fhir` — plan AR-3's rename, the one sanctioned improvement. `emit_state_test.clj` moves alongside as `emit_fhir_test.clj`. `emitter_order_independence_test.clj` stays (it tests emit-hl7; classification finding recorded for M4). Body diffs: ns form, require paths, and docstring self-references only.
2. AR-M3-2 (interface). `ehrt.sim-emit-fhir.interface` carries `bundle-run` only — the call-position-verified src-scope union. `snapshot-at` stays internal (docstring mentions are not calls; both directions verified). If the session's fresh call-position grep disagrees with this in either direction, record the delta and follow the evidence.
3. AR-M3-3 (data.json). `org.clojure/data.json` moves to `components/sim-emit-fhir/deps.edn` as a test-scope dep (`:test` alias `:extra-deps` — its only user is the moved test); the declaration in `components/sim/deps.edn` drops in the same commit (no remaining sim user; verified, and re-verified by fresh grep before dropping). `bases/cli` is unaffected (own declaration).
4. AR-M3-4 (oracle script, minimal fix). Add one line to `bin/regression-oracle`'s synthetic deps heredoc: `poly/sim-engine {:local/root "$wt/components/sim-engine"}`. This restores normal-mode brackets (broken since M2 for any post-M2 ref, disclosed above) and is NOT the J2 redesign — the read-from-current-checkout defect and its Deferred row stand untouched. Prove the fix red→green: a same-ref bracket (`bin/regression-oracle c037f37 c037f37`) fails to resolve before the fix and reports IDENTICAL after it; record both runs. `poly/sim-emit-fhir` is NOT added — digest.clj never loads emit-fhir; adding unused classpath entries is unearned.
5. AR-M3-5 (AR-4 discharge). The ADR's M3 execution record states: the committed second engine consumer has landed; plan AR-4's override note is discharged in substance — `sim-engine`'s boundary now serves two shipping consumers with distinct surfaces (event-log reader vs folded-state reader). Dated line on the roadmap's S4 row closing the loop.
6. AR-M3-6 (stale-path fan-out). Tripwire learns `ehrt.sim.emit-state` and path-form `ehrt/sim/emit_state`; docstring mentions swept on current-tense surfaces per fresh grep (known sites: `run.clj:224`, `identifiers.clj:36,74`, the moved file's own self-references, sim-theory/patient-state docs). Frozen archives untouched. Red→green recorded for the tripwire extension.

Steps
Step 0 — Characterize (evidence, no edits). Verify tip = `c037f37` (STOP-AND-ESCALATE on mismatch). Fresh call-position grep of every `emit-state/` and `ehrt.sim.emit-state` reference across src/test/dev/bin, classified call vs docstring; data.json usage census; form-anchored deftest counts per tree. Record deltas against this prompt's evidence in both directions.
Step 1 — Move, rename, interface, repoint (one commit). Create `components/sim-emit-fhir` (S3 layout): the renamed ns + test, `ehrt.sim-emit-fhir.interface` (`bundle-run`), deps.edn per AR-M3-3. Repoint `run.clj`, `identifiers.clj` (src → interface), `identifiers_test.clj` (test → `ehrt.sim-emit-fhir.emit-fhir`); delete the old ns and drop sim's data.json line in this same commit (no duplicate-ns window). Workspace bookkeeping: root `deps.edn` `:dev`+`:test`, project deps.edn files, `workspace.edn`, `:necessary`, structure-currency surfaces. `poly check` clean, full suite green, both lanes. Commit: `refactor(sim-emit-fhir): the state-based FHIR emitter becomes sim-emit-hl7's sibling (M3 step 1, AR-M3-1/2/3, plan AR-3)`
Step 2 — Oracle script fix (AR-M3-4). The one-line heredoc addition; red→green proof via the same-ref bracket, both runs recorded verbatim. Commit: `fix(oracle): synthetic classpath learns sim-engine — normal-mode brackets restored (M3 step 2, AR-M3-4)`
Step 3 — Stale-path sweep + tripwire (AR-M3-6). Fresh grep, sweep, patterns extended, red→green recorded. Commit: `docs: emit-state stale-path sweep — tripwire learns the old name (M3 step 3, AR-M3-6)`
Step 4 — ADR execution record + AR-4 discharge (AR-M3-5). Append M3's dated execution record to ADR-0043 (the move, the one-var interface with its evidence, the data.json relocation, the oracle fix); the AR-4 discharge statement; roadmap S4-row dated line + M3 row update. Commit: `docs: ADR-0043 M3 execution record — second consumer lands, AR-4 discharged (M3 step 4, AR-M3-5)`
Step 5 — Verification + record. Normal-mode bracket (`bin/regression-oracle c037f37 <step-4-tip>`): all ELEVEN batches byte-identical, expected-change set NONE; any digest change is STOP-AND-ESCALATE. Deftest parity ledger vs Step 0 (pure wash — the moved test file relocates, nothing retires, nothing new). Façade seam byte-identical (M1/M2 worktree method). `clojure -M:poly check` clean; both lanes green. Session record `.agents/session-records/2026-08-04-sim-split-m3-emit-fhir.md`; prompt self-archives (pairing gate enforces). Final commit: `docs: M3 session record — sim-emit-fhir landed, bracket identical in normal mode`
Fences
No check move (M4). No emit logic edits — rendering changes of any kind are FINDINGS, never edits. `snapshot-at` does not enter the interface. No oracle changes beyond AR-M3-4's one line (J2 redesign stays Deferred). Façade (`ehrt.sim.interface`) byte-untouched. `emitter_order_independence_test.clj` does not move. Frozen archives untouched.

---

No STOP-AND-ESCALATE fired (tip matched `c037f37` at Step 0). Fresh
Step 0 call-position grep confirmed the prompt's own evidence in both
directions: `bundle-run` is the sole src-scope caller (`run.clj:345`,
`identifiers.clj:128`), `snapshot-at`'s two `identifiers.clj` mentions
(lines 36, 74) are docstring prose, `data.json`'s only real user is
`emit_state_test.clj`, and `emitter_order_independence_test.clj` tests
`sim-emit-hl7` determinism using `engine/run` only as a fixture
generator — no delta from the prompt's own candidate evidence.

One judgment call made without being spelled out verbatim in this
prompt: `docs/dev/architecture.md`'s own new `sim-emit-fhir` bricks-
table row was reworded mid-Step-1 to describe the AR-3 rename without
spelling out the literal retired `ehrt.sim.emit-state` token, once it
became clear Step 3's own tripwire addition would otherwise trip on
the row's own legitimate extraction-history citation — recorded in
Step 3's own commit message and ADR-0043's M3 stale-path-sweep
paragraph, not silently.

One finding disclosed, not fixed, out of this stage's named scope: the
emit-state demo's own `README.md` (`components/sim/docs/demos/
emit-state/README.md:86`) still bare-cites `ehrt.sim.emit-hl7` (the
S3/Wave-D-D0 move's own gap, never swept at the time) — a future
session's call.

Verification: the normal-mode regression-oracle bracket (`c037f37` vs
`5485ca4`, restored by this stage's own Step 2 fix) reported all eleven
batches byte-identical; façade seam (`ehrt help`/`sim run --format
ground-truth --seed 1 --patients 2`/piped `sim check`) confirmed
byte-identical via two disposable worktrees, no qualification needed;
deftest/defspec parity ledger balanced (95 residual + 14 moved = 109,
Step 0's own authoritative count — pure wash). Pushed
`ff82bf0..5485ca4` across four commits (three numbered steps plus this
record's own final commit).
