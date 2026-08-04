# 2026-08-04 — Sim split B, M2: `sim-engine` extracted, bracket identical in split mode

## Scope

M2 of four (`.agents/plans/2026-08-04-sim-split-b-plan.md`, RULED
AR-1..AR-6): the session prompt's own AR-M2-1 through AR-M2-6, executed
in five steps. `components/sim-engine` created holding `engine.clj`
(1573 LOC — the discrete-event core, `decide`/`evolve`, the seeded-RNG
run loop), `churn.clj` (197 LOC — InjectChurn), and `order_profiles.clj`
(113 LOC — the order/result catalytic), moved verbatim out of
`components/sim` as `ehrt.sim-engine.{engine,churn,order-profiles}`
(ns-form/require diffs only, every move verified byte-identical
otherwise). `ehrt.sim-engine.interface` designed from fresh var-level
caller-evidence grep, not from the design-channel candidate list (used
as cross-check only) — three documented sections (orchestration,
state-reader, acceptance). The one disclosed behavior-adjacent edit:
`order-profiles.edn`'s resource path moves with its loader. Residual
sim's src-scope callers (`run`, `check`, `emit-state`, `identifiers`)
repoint to the interface; every test-scope caller (sim's own five test
files, sim-emit-hl7's six vendored/replay tests, `bin/oracle-src/ehrt/
oracle/digest.clj`) repoints mechanically to `ehrt.sim-engine.engine`
internals. Stale-path tripwire extended (`ehrt.docs-tooling.
stale-path-test`), two real violations in `docs/site-profiles.md` fixed
forward, watched red→green live. `notes/ADRs.md` ADR-0043 gains this
stage's own execution record plus a dated correction to its M1-era
"Dependency directions" note (`sim-engine` depends on `sim-model` AND
`sim-trajectory`, never `kernel` — the M1 plan text guessed wrong) and
the M1 ratification line the prior session's record left open. Fences
held: no check/emit-state moves, no behavior change beyond the one
disclosed resource-path edit, no oracle redesign, façade byte-untouched,
no interface vars beyond the src-caller union, no engine logic edits.

## Red→green evidence highlights

Four commits, `9ccc04f..77a9a72`, `clojure -M:poly check` clean and the
full local suite green (0 failures, 0 errors, both projects) after
every one:

- `9ccc04f` (Step 1) — leaf slice: churn + order-profiles move;
  interface carries their vars plus a Step-1-only transitional
  accommodation (`inject`, `sample-analyte-value`) for residual sim's
  own pre-move `engine.clj`, which repoints to the interface via TWO
  aliases on the same namespace (`:as churn` / `:as order-profiles`)
  so its own call sites needed zero body edits. Self-caught by the
  reading-set budget gate (5 sets bumped, AGENTS.md's own growth).
- `701d0be` (Step 2) — `engine.clj` moves in; interface completes
  (`run`, `config-keys`, `replay`, `documented-step-rejection-reasons`
  join; the two transitional entries removed, re-derivation confirmed
  nothing else needed them); `engine.clj`'s own churn/order-profiles
  requires revert to plain sibling form now that all three share a
  component. Verified by grep: zero remaining `ehrt.sim.engine`
  require anywhere in the tree after this commit.
- `0543043` (Step 3) — stale-path sweep. Two real violations found
  under `docs/`, fixed forward before the tripwire patterns landed;
  watched red (1 failure) then green (0 failures), live. Mechanical
  docstring substitution across 41 live, current-tense files (every
  actual require form already repointed in Steps 1–2) — frozen
  archives and historical narration (`notes/ADRs.md`, `.agents/plans/
  *.md`, `.agents/session-records/*.md`, `notes/sim/`) deliberately
  untouched.
- `77a9a72` (Step 4) — ADR-0043's own M2 section (what moved, the
  interface union with both-direction evidence deltas, the resource
  move, the split-mode bracket plan) plus the M1 ratification line and
  a dated correction to the M1-era dependency-direction guess.
  roadmap.md's Now/Done sections updated. Self-caught by the budget
  gate a second time (roadmap.md's own growth). One transient
  test.check flake observed and disclosed (below), confirmed unrelated
  and non-reproducing on re-run.

**Deftest/defspec parity ledger** (Step 0's own authoritative count):

| tree | Step 0 | now | delta | why |
|---|---|---|---|---|
| `components/sim/test` (residual) | 109 (STAYS classification) | 109 | 0 | unchanged in place |
| `components/sim-engine/test` | 103 (MOVES classification) | 103 | 0 | received from sim, unchanged |
| workspace-wide (sim + sim-engine) | 212 | 212 | 0 | pure wash — nothing retired, nothing new |

**Regression oracle — split mode (AR-M2-4).** `digest.clj`'s own
cross-side diff (`978c54f` vs this session's own tip, `77a9a72`) is
exactly one line, ns/require-only (`[ehrt.sim.engine :as engine]` →
`[ehrt.sim-engine.engine :as engine]`) — the soundness condition
asserted and confirmed before running either side. Two disposable
`git worktree`s, each digested by its OWN `digest.clj` against its OWN
synthetic classpath (pre-side: `{kernel, sim, sim-model,
sim-trajectory, sim-emit-hl7}`; post-side: the same set plus
`sim-engine`) — exact commands:

```
git worktree add --detach /tmp/oracle-pre 978c54f
git worktree add --detach /tmp/oracle-post 77a9a72
# pre-side :deps points every poly/X :local/root at /tmp/oracle-pre/components/X
#   (no poly/sim-engine — it doesn't exist at 978c54f)
clojure -Sdeps "<pre-deps>" -M:oracle-run -m ehrt.oracle.digest /tmp/oracle-pre-out
# post-side :deps points every poly/X :local/root at /tmp/oracle-post/components/X
#   (poly/sim-engine added)
clojure -Sdeps "<post-deps>" -M:oracle-run -m ehrt.oracle.digest /tmp/oracle-post-out
sha256sum -- *.edn | sort -k2   # both output dirs, diffed
```

All ELEVEN batches (nine legacy + `ear-infections-history-engine` +
`urinary-tract-infections-history-engine`) byte-identical, expected-
change set NONE — the resource-path move is invisible in output, as
required. Both worktrees removed after (`git worktree remove --force`,
`git worktree prune`).

**Façade seam** (`ehr help`, `ehr sim run --format ground-truth`,
`ehr sim run --format ground-truth | ehr sim check`; two disposable
`git worktree`s at `978c54f` and `77a9a72`, `--format ground-truth`
used for `sim run` specifically to sidestep the manifest's own
`:generator` block, which legitimately differs by git SHA between the
two worktrees — not a behavioral difference, M1's own precedent for
avoiding that exact noise): all three byte-identical outright, no
qualification needed this time.

## Judgment calls and their ratification status

- **Interface var placement across the three documented sections**
  (AR-M2-2): `replay` is read by `emit-state`/`identifiers`
  (state-reader) AND `check` (acceptance) — landed once, under
  state-reader, with a docstring note naming both consumers rather
  than duplicating the def. `config-keys` is read identically by
  `run.clj` and `identifiers.clj` — landed under orchestration (its
  natural home, describing what `run` accepts) with the same kind of
  shared-use note. Not explicitly resolved by AR-M2-2's own text;
  disclosed here for review.
- **Step 1's double-alias transitional technique** (requiring
  `ehrt.sim-engine.interface` twice, once `:as churn` once `:as
  order-profiles`, so residual sim's own pre-move `engine.clj` needed
  zero body edits for one commit): a mechanical choice in service of
  the "ns-form/require diffs only" discipline, not named in the plan
  or prompt. Removed again in Step 2 once `engine.clj` itself moved.
- **`emitter_order_independence_test.clj` classification** (Step 0,
  STAYS): tests a `sim-emit-hl7` property using `engine/run` only as a
  fixture generator — fits neither MOVES nor STAYS cleanly. Resolved
  STAYS since it exercises neither engine/churn/order-profiles
  semantics; its require still repoints mechanically regardless of
  where the file lives.
- **Dependency-direction correction** (ADR-0043's M2 section): the
  M1-era "Dependency directions" note guessed `sim-engine` depends on
  `kernel`; fresh M2 grep found no `ehrt.kernel.*` require anywhere in
  `engine.clj`/`churn.clj`/`order_profiles.clj`, and a real
  `ehrt.sim-trajectory.interface` require in `engine.clj` the M1-era
  note missed entirely. Corrected by dated addendum in the ADR, not by
  rewriting the M1 text — annotate, not silently fix.

## Findings (disclosed, not fixed — out of this session's own scope)

- **`ehrt.sim-engine.order-profiles/explain-profiles` is dead code**:
  fresh grep at Step 0 found no caller anywhere, not even its own test
  file. Moved verbatim per move-don't-improve (this stage's own fence:
  no logic edits of any kind); a future session's call whether to
  delete it.
- **Pre-existing stale `ehrt.sim.facility` reference**,
  `docs/site-profiles.md` line 111 — predates this session (an earlier
  split, sim-model's own facility extraction, never swept). Left
  untouched: not part of AR-M2-6's own named scope (`ehrt.sim.engine`/
  `churn`/`order-profiles` only), and the stale-path tripwire has no
  denylist entry for it either.
- **`projects/ehrt-cli/deps.edn`'s own `:coverage` alias never included
  `components/sim` at all** (confirmed by reading its `-p`/`-s` lists
  in full before adding `sim-engine`) — pre-existing, unrelated to this
  session's own scope; `sim-engine` was not added there either, for
  consistency with the existing (unexplained) omission rather than
  silently expanding coverage's own scope.
- **`docs/dev/architecture.md`'s mermaid diagram was already missing
  the `provenance` node/edges** from M1 (landed same day, one session
  earlier) — noticed while adding `sim-engine`'s own node/edges to the
  same diagram. Not fixed: M1's own gap, out of this session's named
  scope.
- **Transient test.check generator flake**,
  `ehrt.corpus.sink-composability-test`'s
  `dir-sink-write-then-intake-hash-identity-property-test`: errored
  once ("Couldn't generate enough distinct elements!") on Step 4's
  first full-suite run, passed clean on immediate re-run in isolation
  and on a full second run. Pre-existing generator flakiness in an
  unrelated corpus-domain test this session never touched — not a
  regression, not fixed.

**HEAD landed:** `77a9a72` before this record's own commit; this record
and its paired prompt archive land as the final commit of the session.
