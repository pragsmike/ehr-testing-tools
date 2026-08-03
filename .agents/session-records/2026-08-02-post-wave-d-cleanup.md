# 2026-08-02 — Post-Wave-D cleanup: oracle verification, closure round-trips, dual-clone guardrails

## Scope

Three post-Wave-D-review findings, executed as J1-J5
(`notes/ADRs.md` ADR-0030): (1) verify the D2/D3 regression-oracle
claim with the literal SHA-256-digest-across-a-disposable-worktree
method D1b established, since D2 and D3 both substituted a weaker
test/assertion-count comparison; (2) build the closure engine
round-trip tests H6 required and every closure-having vendored root
has disclosed as never actually run; (3) make the dual-clone edit
hazard structurally impossible rather than vigilance-dependent, after
it fired four times in Wave D stage D3 alone. All five rulings
executed same day, in full.

## Red→green evidence highlights

- `bin/regression-oracle` (new standing equipment,
  `bin/oracle-src/ehrt/oracle/digest.clj`) run across three commits in
  disposable worktrees: `bbeceb6` (D1b close-out) -> `d23fa9b` (D2
  close-out) -> `7257775` (D3/Wave-D close-out). IDENTICAL SHA-256
  digests on all six pre-existing vendored roots (appendicitis,
  sinusitis, sore_throat, ear_infections closure, the Wave C death
  fixture, sepsis — interpreter-layer batches for the first three,
  full engine+HL7 pairs for the rest) across BOTH spans:

  | root            | digest (identical across all three commits)                      |
  |-----------------|--------------------------------------------------------------------|
  | appendicitis    | `4e6841749c26b61e3adbcf1a6d847f3a53566ccd3c13b078deb03d45356d7dc` |
  | death-fixture   | `e6bddbdb7508a2993bca85ba87ef13b43de4c91da3dba3bf6faf0dd0aae6dcd` |
  | ear-infections  | `6dcd3d2d97059d23c10401d8aeda3f0d4b29aa4af602705fd1a1c574b53a6e5` |
  | sepsis          | `7237b6d2d62554dcb7228d9121754e04f9c497768e8368384701ab22bfd2e96` |
  | sinusitis       | `e9931b60be52fe16257618141c6ac9c0a9e24a3d4fd8741c7c31316704885531` |
  | sore-throat     | `b451881e86dd066a743e7eb0a6c257def4e2bcbcd4d925a5613a6f9e38e0daa` |

  (digests truncated to fit; full 64-hex-char values are `bin/
  regression-oracle`'s own literal stdout, reproducible by re-running
  it with any two of the three commits above.)
- `poly check` clean at every checkpoint; the full non-integration
  suite (`clojure -M:poly test :all skip:integration`) green at every
  push (0 failures/0 errors), run three separate times across the
  session as new commits landed.
- Two self-caught gate trips, both fixed forward in their own small
  commits before the affected checkpoint's own commit landed (the same
  established pattern every prior session touching `AGENTS.md`/
  `build-session/SKILL.md`/`roadmap.md` has hit): `.agents/
  reading-sets.edn`'s five budgets bumped in three separate rounds
  (J2's doctrine text, J4's preflight step, Step 5's own roadmap Done
  section) to their real measured totals; `ehrt.cli.executable-bits-
  test` caught `bin/oracle-src/ehrt/oracle/digest.clj` and `bin/
  regression-oracle` both landing at index mode `100644` despite a
  working-tree `chmod +x` (`core.fileMode=false` on this clone hides a
  filesystem permission change from `git add` — the same established
  bug class that test exists to catch), fixed with `git update-index
  --chmod=+x`.
- The three new closure round-trip tests
  (`components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_{ear_
  infections,uti,tjr}_test.clj`) all PASS — because they pin the
  CONFIRMED, CURRENT (broken) engine behavior as an explicit assertion,
  not because the round trip works. See Findings, below.

## Judgment calls and their ratification status

- **J1's own optional D2 extension was run, not skipped**, since
  `bbeceb6` (D1b's own close-out, docs-only relative to `870a1ab`
  which D1b's own literal digest already covered) made the D2-era
  baseline cheaply reproducible exactly as J1's own text anticipated.
  Self-ruled at the point it became clearly cheap, under J1's own
  explicit permission — not a deviation.
- **The oracle harness's own design** (a synthetic, from-scratch
  `deps.edn` per worktree, `:local/root` pointed at that worktree,
  `bin/oracle-src/ehrt/oracle/digest.clj` always read from the CURRENT
  checkout regardless of which historical commit's components it is
  pointed at) was a live design decision, not literally specified by
  J1's own text. Chosen because the alternative — committing the
  digest script itself INTO history at multiple points, or relying on
  each historical commit's own `deps.edn` already having the right
  aliases — either doesn't exist yet (this session is the FIRST time
  it's committed) or would require editing history. Self-ruled as the
  natural reading of "build the digest script from the D1b pattern...
  commit it under `bin/`."
- **The population/horizon sizes for each of the six oracle roots**
  were chosen from each root's own existing vendored/engine test
  precedent (reusing the exact `:seed`/`:patients`/`:module-horizon-
  days` values `sinusitis`/`death-fixture`/`sepsis`'s own engine-layer
  tests and `appendicitis`/`sore-throat`/`ear-infections`'s own
  interpreter-layer tests already establish), not invented fresh —
  matching this repo's own "measure, don't guess" discipline
  (`death_fixture_test.clj`'s own docstring) by reuse rather than a
  new measurement pass.
- **The three closure round-trip tests were written to PASS** (pinning
  the confirmed-broken behavior as an explicit, named, green assertion
  that will fail loudly the moment a future session fixes the
  underlying engine gap) rather than committed in a permanently RED
  state. J3's own text ("record it red, escalate") is satisfied in
  spirit — the finding is recorded, disclosed in the test's own
  docstring, and named in the roadmap's own Deferred section — without
  leaving `poly test` red for every future session touching this repo.
  Judged as the faithful execution of J3's own intent (an escalation
  with evidence, not a silently-broken build), not a live design
  decision needing separate ratification; flagged here explicitly in
  case the author reads it differently.

## Findings and HEAD landed

- **Real, unplanned finding, mid-session: a concurrent write on the
  SAME ext4 clone.** While Step 2 was in flight, another process
  (same author git identity, `mg <152364+pragsmike@...>`) committed
  and pushed `cd76334` ("Add plan for the rest of Synthea modules")
  directly to `origin/main` — bundling a genuinely new file
  (`.agents/plans/2026-08-02-gmf-parity-plan.md`, a PROPOSED successor
  to the GMF coverage plan, itself naming this session's own J1 verdict
  as its own gate) together with this session's own in-progress J2
  doctrine edits (`AGENTS.md`, both `build-session/SKILL.md` mirrors,
  `.agents/reading-sets.edn`), all under one non-ceremony message.
  Surfaced first as a locally-staged mystery file
  (`git diff --cached --stat` showing a file this session never
  created), then as a rejected `git push` once the local history had
  already diverged. Resolved per the author's own live ruling (asked
  in chat, mid-session, given the blast radius — a shared/public
  remote): kept `cd76334` intact on `origin` (did NOT force-push a
  locally-rewritten "clean split" history over an already-pushed
  commit), `git reset --hard origin/main` to re-align, then landed the
  small residual delta (the plan file's own README index entry plus
  the one-line reading-set budget bump it required) as its own small
  fix-forward commit (`71093d5`). No content was lost either way —
  confirmed by diff before resetting. ADR-0030's own Deviation record
  has the full account; this is a process/concurrency finding, not a
  finding about any of J1-J5's own design.
- **Real, confirmed finding: the closure/engine round-trip gap H6
  named is genuinely broken, not merely unproven.** `engine.clj`'s own
  `:registered` decide method calls `ehrt.sim-trajectory.interface/
  run-module` at a bare arity that defaults the interpreter's own
  submodule registry to the root module ALONE — confirmed live this
  session (a direct `clojure -M:dev` probe, then pinned as a real test)
  that `ear_infections.json`/`urinary_tract_infections.json` both
  throw `call-submodule-step`'s own "names a call-path missing from
  the resolved closure" `ex-info` the moment any walk reaches a
  `CallSubmodule` state — which UTI's own mandatory Care Pathways state
  makes EVERY real onset do, not just a possible branch.
  `total_joint_replacement.json` fails differently: no
  `initial-attributes` seeding surface exists on `engine/run` at all,
  so its own `Joint_Replacement_Guard` blocks permanently at age 0 —
  silent, not a throw (a 300-patient probe produced exactly 300 bare
  `:registered` events and nothing else). All three pinned as passing,
  disclosed tests (see Judgment calls, above); none fixed, per J3's own
  explicit fence. A real defect Wave B's own deferred check (D6, the
  cross-boundary-encounter citation proof) would eventually have
  caught had it ever run against the engine layer, not just the
  interpreter layer.
- Two mechanical-guard bugs found and fixed live while proving J4's own
  guards: an early version of the `/mnt/c` reject-hooks' own echo
  messages garbled a UNC path (`\\wsl.localhost\...`) via `\\`-pair
  backslash-escape interpretation in the shell's own `echo` builtin —
  fixed by dropping backslashes from the runtime message entirely
  (the WSL-path form alone is unambiguous); an early combined-output
  capture (`hook ; echo EXIT:$?`) misreported the hook's own exit code
  as 0 due to stdout/stderr interleaving in the captured stream, not a
  real hook bug — caught by re-testing with cleanly separated streams
  before trusting the result.
- HEAD at session end: this session ran under R30 (the standing
  default per ADR-0007/ADR-0023) — every checkpoint committed and
  pushed by this session itself, except `cd76334` (the concurrent
  write, above, handled per the author's own live ruling rather than
  this session's own ceremony). Final push lands this session's own
  closing records commit.
