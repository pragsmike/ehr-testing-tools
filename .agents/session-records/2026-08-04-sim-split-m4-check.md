# 2026-08-04 — Sim split B, M4: `sim-check` extracted, residual sim thinned, arc COMPLETE

## Scope

M4 of four (`.agents/plans/2026-08-04-sim-split-b-plan.md`, RULED
AR-1..AR-6), the LAST stage: the session prompt's own AR-M4-1 through
AR-M4-7, executed in five steps. `components/sim-check` created
holding `check.clj` (571 LOC — the invariant catalog: 24 log-only
invariants, 2 facility-config invariants, 1 warm-up invariant, 1
order-profiles invariant, `check-all`'s four-arity aggregator), moved
verbatim out of `components/sim` as `ehrt.sim-check.check` —
move-don't-improve, the double `ehrt.sim-engine.interface` alias (an
M2-era artifact) carried unchanged. `ehrt.sim-check.interface`
designed from fresh call-position grep — `check-all` only, every
arity, no delta from the design channel's own candidate list.
Residual sim's two src-scope callers (`run.clj`, `interface.clj` —
the façade itself) repoint to the new interface; `engine_test.clj`
and the four vendored sim-emit-hl7 tests (test-scope) repoint
mechanically to `ehrt.sim-check.check` internals. The façade's own
fat-component disclosure docstring gains a dated closing note: the
extraction it disclosed across seven prior stages (S1/S2/S3/M1/M2/
M3/M4) is complete, residual sim is pure orchestration behind the
SAME unchanged façade (08-02 plan AR-3 honored, not revisited).
`emitter_order_independence_test.clj` moves to
`components/sim-emit-hl7/test` — it is emit-hl7's own determinism
guard, misplaced only because check.clj hadn't moved out yet when it
was written. Five parked findings disposed with fresh evidence:
`explain-profiles` (zero callers, disposal not deletion), the
`:coverage` alias gap (standing since M2, closed), the architecture
diagram's missing `provenance` node (an M1 gap the structure-currency
gate never covered), the emit-state demo README's stale
`ehrt.sim.emit-hl7` bare-cite (M3-disclosed, fixed forward), and
`components/sim/deps.edn`'s two unused deps (malli, babashka.cli,
dropped with leakage proof). Stale-path tripwire extended
(`ehrt.docs-tooling.stale-path-test`); no real gate-scope violations
existed, so the new patterns were proven via the mechanism-sanity
test's own synthetic-input assertions, same shape every prior
addendum in the family uses. `notes/ADRs.md` ADR-0043 gains this
stage's own execution record and the arc-complete statement: the
five-brick decomposition (`sim-engine`, `sim-emit-fhir`, `sim-check`,
`provenance`, orchestration-only residual `sim`) is landed in full,
four stages, all same-day, every stage oracle-proven byte-identical.
Fences held: no further sim-side extraction (this is the plan's own
last stage), no façade surface changes beyond the licensed docstring
note, no check logic edits, no oracle redesign, frozen archives
untouched.

## Red→green evidence highlights

Four commits, `c43f7cc..9db87c0`, `clojure -M:poly check` clean and
the full local suite green (0 failures, 0 errors, both projects, 202
Test-results blocks) after every one:

- `c43f7cc` (Step 1) — the move: `check.clj`/`check_test.clj` become
  `ehrt.sim-check.check`/`ehrt.sim-check.check-test` under
  `components/sim-check`; interface carries `check-all` only, all
  four arities; the façade's fat-component disclosure gains its dated
  retirement note; workspace bookkeeping (root `deps.edn`, all three
  projects, `AGENTS.md`, `docs/dev/architecture.md`) lands together.
  Self-caught by the reading-set budget gate (5 sets bumped:
  onboarding/corpus/judge +8 each from `AGENTS.md`'s growth alone,
  sim +20 (`AGENTS.md` +8 plus `interface.clj`'s own dated note +12),
  docs +14 (`AGENTS.md` +8 plus `architecture.md` +6)).
- `e948296` (Step 2) — findings disposition: the order-independence
  test moves to `sim-emit-hl7/test`; the coverage alias gains four
  test/src paths; the diagram gains the `provenance` node and two
  edges; the demo README's stale citation is fixed forward; two
  unused deps drop from `components/sim/deps.edn`, proven by leakage
  (full suite + `poly check` green after the drop). Self-caught by
  the reading-set budget gate a second time (`docs` +3, the diagram's
  own growth).
- `56b62a7` (Step 3) — stale-path sweep. Fresh grep of the gate's own
  scan scope found zero real violations, so the two new pattern
  clauses were proven via the mechanism-sanity test's own synthetic
  assertions (153 assertions, up from 148, all passing). Eight live
  current-tense surfaces outside the gate's own scan scope (component-
  owned `docs/` trees plus four src/test docstrings) swept forward
  anyway. The 08-04 plan's own historical mention
  (`.agents/plans/2026-08-04-sim-split-b-plan.md:45`) left untouched —
  annotate-not-rewrite, Step 4's own close-out note covers it instead.
- `9db87c0` (Step 4) — ADR-0043's own M4 section (what moved, the
  one-var interface with its call-position evidence, the façade
  docstring retirement, the five findings' disposition, the stale-path
  sweep, the new "Dependency directions" entry) plus the arc-complete
  statement (AR-M4-7): the doctrine the decomposition landed (sibling
  emitters over one state machine, checker separate from doer,
  provenance as the shared contract), standing deferred items
  re-cited with triggers intact (J2 oracle redesign, carry-across
  emission, sim-cli retirement (closed), census-tool refinements, the
  new docs-coherence-pass row). `roadmap.md`'s own new M4 Done
  section, "Now" section rewrite (arc COMPLETE), and the new docs-
  coherence-pass Deferred row. The 08-04 plan gets its own dated
  close-out note. Self-caught by the reading-set budget gate a third
  time (`roadmap.md`'s own growth, onboarding +55).

**Deftest/defspec parity ledger** (deftest-only count, Step 0's own
methodology):

| tree | Step 0 (baseline `6f45a82`) | now (`9db87c0`) | delta | why |
|---|---|---|---|---|
| `components/sim/test` (residual) | 92 | 30 | -62 | `check_test.clj` (-61) and `emitter_order_independence_test.clj` (-1) both move out |
| `components/sim-check/test` (NEW, receives) | 0 | 61 | +61 | `check_test.clj` moves in, unchanged |
| `components/sim-engine/test` | 86 | 86 | 0 | unchanged — repoint only, no test body edit |
| `components/sim-emit-hl7/test` (receives) | 80 | 81 | +1 | `emitter_order_independence_test.clj` moves in, unchanged |
| workspace-wide (all four trees) | 258 | 258 | 0 | pure wash — nothing retired, nothing new (`explain-profiles` is src, no deftest effect) |

**Regression oracle — normal mode.** `bin/regression-oracle 6f45a82
9db87c0` — a single normal-mode invocation, no workaround needed
(`digest.clj` never required `check`/`sim-check` at any point, so no
producer-shape coupling to work around). All ELEVEN batches (nine
legacy + `ear-infections-history-engine` +
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
digest matches between 6f45a82 and 9db87c0`.

**Façade seam** (`ehrt help`, `ehrt sim run --format ground-truth
--seed 1 --patients 2`, `ehrt sim run --format ground-truth --seed 1
--patients 2 | ehrt sim check`; two disposable `git worktree`s at
`6f45a82` and `9db87c0`): all three byte-identical outright, no
qualification needed — this stage's own most load-bearing check,
since the façade file itself changed (the require line plus the
dated docstring note). Worktrees removed after (`git worktree remove
--force`, `git worktree prune`).

## Judgment calls and their ratification status

- **The docs-coherence-pass Deferred row** (`roadmap.md`, Step 4): not
  a pre-existing row before this session — named for the first time,
  sourced from this session's own driving prompt's AR-M4-7 text ("the
  docs coherence pass ... the cleanup arc's next front, not this
  session's"). Added as a lightweight roadmap row rather than left
  as ADR-only prose, matching this repo's own convention that every
  named-future gets a citable roadmap entry. Not explicitly required
  by AR-M4-7's own wording (which says "re-cited," and this item had
  nothing to re-cite from); disclosed here for review.
- **`sim-cli` retirement's citation in the arc-complete statement**:
  included per AR-M4-7's own explicit list despite being fully CLOSED
  history with no open trigger — read as the author wanting the
  connection stated (M4's own façade-permanence discipline rests on
  the same "façade is what external callers actually use" evidence
  method that retirement session established), not as an instruction
  to reopen it. No action taken beyond the citation.

## Findings (disclosed, not fixed — out of this session's own scope)

- **`explain-profiles` (`sim-engine/order_profiles.clj:66`) stays in
  place**, undeleted, despite zero callers found: AR-M4-5a licenses
  disposing the finding with evidence, not a deletion ruling. A future
  session's call.
- **The docs-coherence-pass gap itself** (see the new roadmap row):
  four component-owned `docs/` trees now carry current-tense
  namespace citations the stale-path tripwire structurally cannot
  gate, swept per-stage by hand across S1-S4/M1-M4 rather than
  systematically. Named as a future front, not built this session.

**HEAD landed:** `9db87c0` before this record's own commit; this
record and its paired prompt archive land as the final commit of the
session.
