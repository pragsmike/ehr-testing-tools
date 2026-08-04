# 2026-08-04 — Sim split B, M1: provenance component, mirror retirement, vestige sweep

## Scope

M1 of four (`.agents/plans/2026-08-04-sim-split-b-plan.md`, RULED
AR-1..AR-6): the session prompt's own AR-M1-1 through AR-M1-7,
executed in seven steps. `components/provenance` created holding
ManifestV0/V1/V1_1 + validators, moved verbatim out of
`ehrt.corpus.manifest` (the acyclic single home both corpus and sim
depend on — corpus → sim already exists via `ehrt.corpus.sim-adapter`,
so sim → corpus for the schema would be a cycle). `ehrt.corpus.manifest`
repoints via a relay (zero consumer churn); the conformance project's
own `sim-manifest-contract-test` repoints to provenance directly with
a dated note; `ehrt.sim.manifest`'s `MirroredManifest` and its own
`valid?` retire entirely (fresh grep found no real caller of `valid?`
outside its own now-retired tripwire test). A two-repo vestige sweep
found one real mechanically-stale path
(`bases/cli/core.clj`'s broken `ehr-testing-sim-mounting-note.md`
citation) and otherwise confirmed the named files already describe
history correctly. A new docs-tooling gate
(`ehrt.docs-tooling.prompt-record-pairing-test`) enforces the
session-record/prompt pairing invariant both directions, proven
red→green live. `notes/ADRs.md` ADR-0043 records the plan's AR-1..AR-6
and this session's own AR-M1-1..AR-M1-7 verbatim, the dependency
directions (`provenance`'s forbidden-forever rule; `sim-engine`'s
PLANNED M2 shape named ahead of execution), and the intake-front-door
doctrine (no code — sim runs enter `ehr corpus intake` as if foreign).
Fences held: no engine/check/emit-state moves, no schema field
changes, no CLI surface changes, no interface narrowing.

## Red→green evidence highlights

Seven commits, `f522db7..a9154d8`, `clojure -M:poly check` clean and
the full local suite green (0 failures, 0 errors) after every one:

- `83304c1` (Step 1) — provenance created; 15 schema tests split 9
  (provenance, literal fixtures) / 6 (corpus, builders); self-caught by
  the reading-set budget gate (5 sets bumped) and the
  index-completeness gate (this session's own driving plan file had
  never been indexed — fixed forward).
- `ab8a50c` (Step 2) — corpus repoints via relay; `workspace.edn`'s
  temporary `:necessary` overrides (Step 1) re-derived and dropped,
  confirming the real edge. Self-caught by the budget gate again
  (corpus/interface.clj's own growth).
- `46fef14` (Step 3) — contract test repoints; sim-side fast-lane unit
  test added (`built-manifest-validates-against-provenance-test`,
  landed ahead of the mirror's own retirement so builder validity was
  never left uncovered).
- `dff47fb` (Step 4) — mirror retires; sim/manifest-test drops from 5
  tests to 4 (the tripwire retires, its replacement already landed
  Step 3).
- `9ec8360` (Step 5) — vestige sweep + pairing gate; the new gate's
  own allowlist deliberately corrupted and confirmed red (two real
  gates failed exactly as expected), then restored and reconfirmed
  green — a genuine red→green proof against live data, not just
  synthetic fixtures.
- `a9154d8` (Step 6) — ADR-0043 + roadmap/plan dated notes; self-caught
  by the budget gate a third time (roadmap.md's own growth).

**Deftest/defspec parity ledger** (Step 0's own authoritative count,
superseding the plan's provisional 229 for `components/sim/test`):

| tree | Step 0 | now | delta | why |
|---|---|---|---|---|
| `components/sim/test` | 212 | 212 | 0 | −1 mirror tripwire, +1 provenance fast-lane |
| `components/corpus/test` | 252 | 243 | −9 | moved to provenance |
| `components/provenance/test` | 0 (didn't exist) | 9 | +9 | received from corpus |
| `projects/conformance/test` | 16 | 16 | 0 | modified in place |
| `components/docs-tooling/test` (pairing gate only) | 0 | +5 | +5 | genuine new gate |

Net: manifest-schema work is a wash across sim/corpus/provenance/
conformance (480 = 480); the pairing gate is a real net addition (+5),
not a moved one.

**Regression oracle** (`bin/regression-oracle f522db7 a9154d8`, the
pre-session tip through this session's own Step 6 landing): all ELEVEN
batches (nine legacy + `ear-infections-history-engine` +
`urinary-tract-infections-history-engine`) byte-identical —
`IDENTICAL: every root's digest matches between f522db7 and a9154d8`.
No stage in this session changed behavior.

**Façade seam** (`ehr help`, `ehr sim run`, `ehr sim check`; two
disposable `git worktree`s at `f522db7` and `a9154d8`, apples-to-apples
so neither run's own git-HEAD-readability quirk skews the comparison):
`help` and `sim check` byte-identical outright; `sim run`
byte-identical modulo the `--out` directory argument itself (an
artifact of this verification's own two invocations using different
scratch directories, not a behavioral difference — confirmed by an
extra worktree-to-worktree cross-check after an initial worktree-vs-
main-checkout comparison showed a `:generator :sha256` diff that
turned out to be a git-worktree-readability artifact unrelated to any
code this session touched, not a regression).

## Judgment calls and their ratification status

- **`ehrt.corpus.manifest`'s relay design** (Step 2, not explicitly
  spelled out in either the plan or the prompt): rather than repointing
  every consumer (`generate.clj`, `intake.clj`, their tests) to
  `ehrt.provenance.interface` directly, `ehrt.corpus.manifest` itself
  now relays (`(def ManifestV1_1 provenance/ManifestV1_1)`, same var,
  not a copy) so every existing `manifest/...` call site needed zero
  changes — read from the prompt's own Step 2 text ("generate.clj/
  intake.clj requires unchanged if they only touch builders and
  valid-v1-1? — repoint what fresh grep says, nothing more"). Not yet
  ratified explicitly; the design is disclosed here and in ADR-0043's
  own execution record for review.
- **AR-M1-2's "repoint or retire, decide from fresh grep" instruction**
  resolved to RETIRE: fresh grep at Step 4 found no real caller of
  `ehrt.sim.manifest/valid?` outside its own now-retired tripwire test.
  Recorded in the code's own docstring and this record.
- **`corpus/manifest_test.clj`'s test split** (Step 1, "split the file
  by what it tests"): 9 of 15 tests classified as schema/validator
  tests (moved, rewritten against literal fixture maps rather than
  corpus's own builders — provenance can't depend on corpus), 6 as
  builder tests (stayed, still using `manifest/valid*?` via the Step 2
  relay). Judgment call, not explicitly enumerated in either the plan
  or the prompt; the per-test classification is visible in
  `83304c1`'s own diff for review.
- **Vestige sweep scope**: fresh grep found the AUTHORS-GUIDE.md/
  Makefile "pack-push" mentions and way-of-working.md's own
  `ehr-testing-sim` cross-repo attribution already correctly framed as
  retired/historical, not live drift — judged no-change-needed rather
  than silently skipped. Only one real fix landed (the cli/core.clj
  broken path).
- **Pairing-gate allowlist derivation** (AR-M1-7): derived via `comm
  -23` between the two directories' own slug sets at authoring time,
  not hand-enumerated from the prompt's own "seven" count (which this
  session independently confirmed, rather than trusted blind).

## Findings and HEAD landed

- **Pre-existing stale citation, unrelated to this session's own
  scope**: `bases/cli/src/ehrt/cli/core.clj` cited
  `notes/ehr-testing-sim-mounting-note.md` — missing the `tools/` path
  segment the file actually lives under. Fixed in `9ec8360` (vestige
  sweep, mechanical).
- **Pre-existing indexing gap**: this session's own driving plan file
  (`.agents/plans/2026-08-04-sim-split-b-plan.md`, authored by the
  design channel ahead of this session) had never been added to
  `.agents/plans/README.md`'s own list — caught by the
  index-completeness gate at Step 1, fixed forward in the same commit.
- No other findings outside this session's own planned scope.

**HEAD landed:** `a9154d8` before this record's own commit; this
record and its paired prompt archive land as the final commit of the
session.
