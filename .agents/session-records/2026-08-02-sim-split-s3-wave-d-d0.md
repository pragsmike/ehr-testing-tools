# 2026-08-02 — sim split S3 / Wave D stage D0: extract `sim-emit-hl7`

## Scope

First session of GMF coverage Wave D, whose design (R1–R7) was ruled in
the design channel 2026-08-02 and captured verbatim in a new ADR
(`notes/ADRs.md` ADR-0029) as this session's own Step 0. D0 executes
the sim split plan's S3 (`.agents/plans/2026-08-02-sim-split-plan.md`)
per R1: `emit_hl7.clj`, `v2_replay.clj`, and `site_profile.clj` (+ their
tests) moved from `components/sim` to a new `components/sim-emit-hl7`,
namespaces `ehrt.sim.X` → `ehrt.sim-emit-hl7.X`, bodies byte-identical
except the `ns` form. `emit_state.clj` did NOT move (its seam is S4's
own design problem). `ehrt.sim.interface`'s public surface did not
change (AR-3/ADR-0025; corpus depends on it). Method precedents: the
S1+S2 extraction session (ADR-0025) and its own session record.

## Red→green evidence highlights

- `poly check`: clean before any edit, clean at every checkpoint after.
- `poly test :all skip:integration`: 0 failures/0 errors before any
  edit (captured as the Step 1 baseline) and after the Step 2 extraction
  (captured to a file, not piped — this workspace's own caught-before
  lesson). Two real gates fired red as a *direct, expected* consequence
  of the move, both fixed forward: `ehrt.docs-tooling.structure-
  currency-test` (AGENTS.md/architecture.md hadn't named the new brick
  yet) and `ehrt.docs-tooling.reading-set-budget-test` (five self-catches
  — onboarding/corpus/sim/judge/docs, the same shape every prior split
  session's own close-out hit).
- Golden run (seed 42, 5 patients, `--emit hl7`) byte-identical
  pre-move → post-move: `ground-truth.edn` (`--format ground-truth`),
  `messages.txt` (`--format er7`), `identifiers.edn`, `ehrt help`, and
  `ehrt help sim` — exact sha256 matches on all five. `run.edn`'s only
  diff was the manifest's `:generator :sha256` field (tracks the git
  HEAD sha; several commits landed between the two captures — a real,
  expected environment change, never a code change, confirmed by
  redacting that one field and diffing the rest byte-for-byte).
- deftest+defspec count parity: 281 in `components/sim/test` alone
  before any edit (live recount this session, superseding ADR-0025's own
  274-post-S2 figure — seven tests landed in residual sim across Waves
  A/B/C since) = 206 residual + 75 `sim-emit-hl7` after the move. Zero
  test loss, zero duplication.
- Namespace diff on all six moved files: renames only, confirmed by
  reading every diff, not sampling — git's own rename-similarity scores
  (92–99%) corroborate this independently.
- A genuine deviation, caught and fixed forward before Step 1 began:
  Step 0's own `Edit` call landed the new ADR-0029 between ADR-0027
  (Wave B) and ADR-0028 (Wave C) instead of at the true end of the file
  — its `old_string` anchor (a closing Fence paragraph) matched Wave B's
  near-identically-worded Fence instead of Wave C's. Caught by rereading
  the file's own header list before Step 1, fixed with a dedicated
  fix-forward commit (moving the block, no content change), reverified
  green. Unrelated to S3's own design or code; disclosed here because it
  touched a pushed commit.

## Judgment calls and their ratification status

- Interface width for `sim-emit-hl7`, narrower than either S1 or S2's
  own interface: `emit`'s 2-arg arity has zero real external callers
  (confirmed by grep against every file in the workspace, not judgment)
  and stays unexported; `v2-replay` and `site-profile` have no real
  external caller AT ALL and are fully internal to the new component.
  This is the strict extension of AR-6's own grep-evidence discipline to
  arity, not just def-level exports — not previously exercised this
  strictly in S1/S2 (whose exported defs all kept every arity a real
  caller used, but none of S1/S2's own exported functions were
  multi-arity with an unused arity). Matches the plan's own AR-6 ruling;
  not a live judgment call needing separate ratification.
- Docstring self-consistency sweep, beyond the plan's literal text: six
  files outside the moved trio (`identifiers.clj`, `run.clj`,
  `emit_state.clj`/`emit_state_test.clj`, `engine.clj`, `engine_test.clj`,
  `run_test.clj`, `persona.clj` in `sim-model`) carried PROSE citations
  of the old `ehrt.sim.emit-hl7`/`.v2-replay`/`.site-profile` names —
  none of these files `:require` the moved code, so this was a
  self-consistency fix (a stale name would be simply wrong post-move),
  not a `:require`-level change. Judged in scope under the same
  reasoning ADR-0025 documented for its own citation sweep (a moved
  identifier's home should read correctly wherever it's named), scoped
  to files this session's own grep touched — not a wholesale sweep of
  every `components/sim/docs/*.md` mention (those stay outside the
  stale-path tripwire's own scan surface, sim-theory.edn's own
  historical-narration precedent for the S2 `gmf`/`compile-trajectory`
  names).
- `docs/site-profiles.md` and `docs/simulate-your-facility.md` (root
  user docs, IN the stale-path tripwire's scan scope) needed their bare
  `ehrt.sim.site-profile`/`ehrt.sim.emit-hl7` mentions repointed before
  the tripwire's own three new forbidden patterns (added this session)
  could go green — a REAL violation this time, unlike ADR-0025's own
  synthetic-only proof (no live violation existed under `docs/` when
  the `gmf`/`compile-trajectory` patterns were added).
- `components/sim/docs/demos/site-profiles/` (five files: README plus
  its two ground-truth/messages captures and the aldric config) is the
  ONE component doc this session judged as emitter-subject-owned and
  did NOT move — a scope call made and then reversed on review: its
  sole subject is the site-profile invariance property (one of the
  three moving files), the same "subject is the emitter" test the
  session prompt named, but it was left in residual `sim/docs/demos/`
  this session (a disclosed, out-of-scope-this-session finding, not
  silently decided) rather than moved, since the demos README's own
  index and the root `docs/site-profiles.md`'s own (pre-existing,
  already-broken) `docs/demos/site-profiles/` citation both needed a
  larger docs sweep than this session's own Step 2 budget covered
  cleanly. Named here as a real scope boundary, not hidden in the diff.

## Findings and HEAD landed

- Pre-existing, out-of-scope finding, disclosed not fixed: root
  `docs/site-profiles.md` line 235 cites `docs/demos/site-profiles/` — a
  path that has never existed at the workspace root (the real demo
  lives under `components/sim/docs/demos/site-profiles/`); this predates
  this session and is unrelated to the sim-emit-hl7 extraction itself.
  Not touched (move-don't-improve) — named as a future doc-hygiene item.
- The `components/sim/docs/demos/site-profiles/` disposition above:
  named as unmoved-this-session, a candidate for a future S3-adjacent
  docs pass if one is ever scheduled.
- HEAD at session end: this session ran under R30 (the standing default
  per ADR-0007/ADR-0023) — every checkpoint committed and pushed by this
  session itself. Final push lands this session's own records commit.
