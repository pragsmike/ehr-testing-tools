# 2026-08-04 — Sim split B, M3: `sim-emit-fhir` extracted, AR-4 discharged, bracket identical in normal mode

## Scope

M3 of four (`.agents/plans/2026-08-04-sim-split-b-plan.md`, RULED
AR-1..AR-6): the session prompt's own AR-M3-1 through AR-M3-6, executed
in five steps. `components/sim-emit-fhir` created holding `emit_fhir.clj`
(267 LOC — the state-based FHIR R4 emitter: fold the ground-truth log
via `ehrt.sim-engine.interface/replay`, snapshot at an instant, render
Bundles), moved verbatim out of `components/sim` as
`ehrt.sim-emit-fhir.emit-fhir` — plan AR-3's rename, the one sanctioned
improvement this stage licenses (ns-form/require diffs and the two
self-referential docstring mentions of the old test-namespace name
only; verified byte-identical otherwise). `ehrt.sim-emit-fhir.interface`
designed from fresh call-position grep — `bundle-run` only, the
smallest interface of the four stages. `org.clojure/data.json` moves
with its only real user (the test tree). Residual sim's two src-scope
callers (`run.clj`, `identifiers.clj`) repoint to the interface;
`identifiers_test.clj` (test-scope) repoints mechanically to
`ehrt.sim-emit-fhir.emit-fhir` internals. `bin/regression-oracle`'s
own synthetic classpath gains the `poly/sim-engine` line M2 should have
added and didn't (disclosed, fixed forward, red→green proven live) —
this restores normal-mode brackets for every post-M2 ref. Stale-path
tripwire extended (`ehrt.docs-tooling.stale-path-test`); no real
gate-scope violations existed this time, so the new patterns were
proven red→green directly instead of via a live doc fix. `notes/
ADRs.md` ADR-0043 gains this stage's own execution record and the
AR-4 discharge statement: with this landing, `sim-engine` serves two
shipping consumers with genuinely distinct surfaces (event-log reader,
folded-state reader), and the "committed, not yet present" framing
AR-4 stated at M2 time discharges in full substance. Fences held: no
check move (M4), no emit logic edits, `snapshot-at` stays internal, no
oracle changes beyond the one licensed line, façade byte-untouched,
`emitter_order_independence_test.clj` stays put.

## Red→green evidence highlights

Four commits, `ff82bf0..5485ca4`, `clojure -M:poly check` clean and the
full local suite green (0 failures, 0 errors, both projects, 202
Test-results blocks) after every one:

- `ff82bf0` (Step 1) — the move: `emit_state.clj`/`emit_state_test.clj`
  become `emit_fhir.clj`/`emit_fhir_test.clj` under
  `components/sim-emit-fhir`; interface carries `bundle-run` only;
  `data.json` relocates to the new component's `:test` alias and drops
  from `components/sim/deps.edn` in the same commit; workspace
  bookkeeping (root `deps.edn`, all three projects, `AGENTS.md`,
  `docs/dev/architecture.md`) lands together. Self-caught by the
  reading-set budget gate (5 sets bumped: onboarding/corpus/sim/judge
  +9 each, docs +13 — AGENTS.md's growth landed in every set that
  cites it).
- `438d762` (Step 2) — the oracle fix: one line,
  `poly/sim-engine {:local/root "$wt/components/sim-engine"}`, added to
  `bin/regression-oracle`'s synthetic classpath heredoc. Proven
  red→green with the same-ref bracket (`c037f37 c037f37`): before,
  `FileNotFoundException` resolving `ehrt/sim_engine/engine` (exit 1);
  after, `IDENTICAL` across all eleven batches (exit 0). Exec bit
  dropped by the edit tool, restored (`chmod +x`) before either run —
  a known hazard, checked for deliberately this time.
- `d5e4417` (Step 3) — stale-path sweep. Fresh grep of the gate's own
  scan scope (`docs/**/*.md` + `components/corpus/docs/use-cases.edn`)
  found zero real violations this time (unlike M2's two real hits), so
  the two new pattern clauses were proven red→green by temporarily
  removing them from `violations`, watching the two new fixture
  assertions fail, restoring, watching them pass — both runs recorded.
  Four live current-tense surfaces outside the gate's own scan scope
  (`components/sim/docs/`, component-owned) swept forward anyway:
  `sim-theory.md` (two hits), `sim-theory.edn` (the `:emit-state`
  node's own `:contract`, plus a new dated note), `event-sourcing.md`
  (two hits), the emit-state demo's own `README.md` (two hits).
  `docs/dev/architecture.md`'s own new `sim-emit-fhir` row (landed in
  Step 1) was reworded ahead of this commit to describe the AR-3
  rename without spelling out the literal retired namespace token, so
  its own legitimate history citation wouldn't trip the pattern this
  commit adds.
- `5485ca4` (Step 4) — ADR-0043's own M3 section (what moved, the
  one-var interface with its call-position evidence, the data.json
  relocation, the oracle fix with both red/green runs, the stale-path
  sweep, the new "Dependency directions" entry) plus the AR-4 discharge
  statement (AR-M3-5). `roadmap.md`'s own new M3 Done section, "Now"
  section rewrite (M4 next), and the S4 Deferred row's own
  AR-4-DISCHARGED dated line. Self-caught by the reading-set budget
  gate a second time (`roadmap.md`'s own growth, onboarding +46).

**Deftest/defspec parity ledger** (Step 0's own authoritative count):

| tree | Step 0 | now | delta | why |
|---|---|---|---|---|
| `components/sim/test` (residual, STAYS) | 95 | 95 | 0 | unchanged in place |
| `components/sim-emit-fhir/test` (MOVES) | 14 | 14 | 0 | received from sim, unchanged |
| workspace-wide (sim + sim-emit-fhir) | 109 | 109 | 0 | pure wash — nothing retired, nothing new |

**Regression oracle — normal mode (AR-M3-4, restored this stage).**
`bin/regression-oracle c037f37 5485ca4` — a single normal-mode
invocation (unlike M2's split-mode workaround; the oracle fix landed in
Step 2 makes this possible again for the first time since M2). All
ELEVEN batches (nine legacy + `ear-infections-history-engine` +
`urinary-tract-infections-history-engine`) byte-identical, expected-
change set NONE:

```
89bc2090fa783481e152b2e7a364f407d6332ece6baba71abd1a8008d0686c2d  appendicitis.edn
28087e14d3692bc460182eca9475e4bc3e820b388eeee701368cc88c9fbf8602  death-fixture.edn
5a631475998e505c7edaf902c60bfa519ce171a4e673ae9e99a1eb2687742303  ear-infections-engine.edn
37885c6635918975be76abb37e9b662ebef7858ffefd883b3b4f5a6046b34af4  ear-infections-history-engine.edn
6ad02f827a66def26b5cd87e7c64fea2f48dd4fb782aaaf70fe6cfb10f1721ed  ear-infections.edn
f0b8160db59e3177f2b24cde589c53ca97fc98566a211769e1e0d58d29af74b3  sepsis.edn
e9931b60be52fe16257618141c6ac9c0a9e24a3d4fd8741c7c31316704885531  sinusitis.edn
b451881e86dd066a743e7eb0a6c257def4e2bcbcd4d925a5613a6f9e38e0daa9  sore-throat.edn
818bff1c424cbba98810696eac003a638bc3f87e92d261ecd45c050ee70cb103  total-joint-replacement-engine.edn
97bece7c0d659a6cf47a64544d9884e029dcd453785e48707174cd55872e04b0  urinary-tract-infections-engine.edn
ecc49eb4d6d632f09be24b563aabb4dd1c7dcd1736e91928edaf76726d3534d3  urinary-tract-infections-history-engine.edn
```
identical between baseline and target — `IDENTICAL: every root's
digest matches between c037f37 and 5485ca4`.

**Façade seam** (`ehrt help`, `ehrt sim run --format ground-truth
--seed 1 --patients 2`, `ehrt sim run --format ground-truth --seed 1
--patients 2 | ehrt sim check`; two disposable `git worktree`s at
`c037f37` and `5485ca4`): all three byte-identical outright, no
qualification needed. Worktrees removed after (`git worktree remove
--force`, `git worktree prune`).

## Judgment calls and their ratification status

- **`docs/dev/architecture.md`'s new row wording** (disclosed above,
  Step 3's own commit and the prompt's deviation-record footer): not
  named in the prompt's own AR-M3-1..6, a mechanical necessity once
  Step 3's tripwire pattern would otherwise have tripped on Step 1's
  own legitimate history citation. Not a scope change — the row still
  states the same fact (the AR-3 rename), just without the literal
  banned token.
- **`ehrt.sim-emit-fhir.interface/bundle-run`'s citation form in the
  emit-state demo README** (Step 3): the demo's own description of
  `--emit fhir`'s backing function was updated to cite the interface
  (`ehrt.sim-emit-fhir.interface/bundle-run`) rather than the internal
  namespace, matching `run.clj`/`identifiers.clj`'s own docstring
  convention for the same call path — `sim-theory.edn`'s general
  namespace citations use the internal `ehrt.sim-emit-fhir.emit-fhir`
  form instead, matching that file's own pre-existing convention for
  `ehrt.sim-engine.engine`. Not explicitly resolved by AR-M3-6's own
  text; disclosed here for review.

## Findings (disclosed, not fixed — out of this session's own scope)

- **The emit-state demo's own `README.md` bare-cites
  `ehrt.sim.emit-hl7`** (`components/sim/docs/demos/emit-state/
  README.md:86`): a pre-existing, unrelated stale reference from the
  S3/Wave-D-D0 move (2026-08-02), never swept at the time. Not part of
  AR-M3-6's own named scope (emit-state only), and the stale-path
  tripwire has no denylist entry for it either (component-owned docs
  are out of that gate's scan scope regardless) — a future session's
  call.
- **`bin/regression-oracle`'s own read-from-current-checkout
  limitation** (ADR-0030 J2) is unaffected by this stage's Step 2 fix
  — the fix restores normal-mode resolution, it does not redesign how
  the script reads `digest.clj`. The Deferred row stands untouched,
  per AR-M3-4's own explicit fence.

**HEAD landed:** `5485ca4` before this record's own commit; this record
and its paired prompt archive land as the final commit of the session.
