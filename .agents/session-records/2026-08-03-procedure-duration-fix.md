# 2026-08-03 — Procedure-duration fix (D3c finding 1, ADR-0031 AR-6 first defect-fix)

## Scope

`ehrt.sim-trajectory.gmf-interpreter/emit-and-advance` called
`resolve-time-advance` with a Procedure's own flat `{:low :high :unit}`
`:duration` unwrapped — `resolve-time-advance` destructures
`:range`/`:exact` KEYS from its argument and found neither, so no
vendored Procedure state has ever advanced virtual time (found live,
Wave D stage D3, `docs/gmf-interpreter.md` §14's own D3c finding 1;
named, unowned, in the roadmap's own Deferred section until ADR-0031
AR-6 sequenced it as the FIRST of two defect-fix sessions ahead of the
census). This session fixed exactly that call site, per the design
channel's own AR-1 through AR-5 rulings (`notes/ADRs.md` ADR-0032),
oracle-bracketed the change, and closed out records. `engine.clj`, the
three J3 pinned round-trip tests, and anything closure-registration-
shaped were untouched — AR-6's SECOND defect-fix session, not this one.

## The fix

`emit-and-advance` (gmf-interpreter.clj) now wraps a Procedure's flat
`:duration` as `{:range duration}` before calling `resolve-time-advance`
(ADR-0032 AR-2) — `resolve-time-advance`'s own contract, Delay's nested
shape, Death's Wave-C usage, and the loader schema all stay unchanged.
`gmf.clj`'s `apply-gmf-v2-procedure-duration` docstring's own "bug and
all" clause is retired with a dated FIXED note pointing at the real fix
site, not rewritten.

## Red→green evidence

- The focused test pair (`gmf_interpreter_test.clj`,
  `procedure-duration-advances-virtual-time-within-its-range` +
  `procedure-duration-consumes-a-fixed-single-rng-draw`) was run against
  the PRE-fix tree first (`git stash` on the source file only, test file
  applied): both FAILED — `(not (<= 1 0 5))` and the draw-count property
  shrunk to `0` — the expected "silently never advances" shape, not a
  vacuous test. Restored the fix: both GREEN, `97 tests / 193 assertions,
  0 failures/0 errors` for the namespace.
- Full non-integration suite (`clojure -M:poly test :all
  skip:integration`) run before staging the commit: 0 failures/0 errors
  across every namespace tested (`sim-model`, `sim-trajectory`, and
  every other project on the classpath); the process exit code (0)
  is itself evidence poly's own aggregate test runner saw no failure
  anywhere, not only in the namespaces whose tail output this session
  happened to capture. No existing test encoded the zero-advance
  behavior, so AR-5's own test-triage clause needed no action.
- `gitleaks git --staged -v` clean and `clojure -M:poly check` clean
  before every push (2/2 so far).

## Oracle bracket, and a real escalation resolved mid-session

`bin/regression-oracle dc7b371 1ea1f4a` (tip before this session's fix
commit -> the fix commit) — the six roots `bin/oracle-src/ehrt/oracle/
digest.clj` actually covers:

| root | baseline (`dc7b371`) | target (`1ea1f4a`) | changed? |
|---|---|---|---|
| `appendicitis` | `4e6841749c26b61e3adbcf1a6d847f3a53566ccd3c13b078deb03d45356d7dcb` | `89bc2090fa783481e152b2e7a364f407d6332ece6baba71abd1a8008d0686c2d` | YES |
| `death-fixture` | `e6bddbdb7508a2993bca85ba87ef13b43de4c91da3dba3bf6faf0dd0aae6dcdf` | `28087e14d3692bc460182eca9475e4bc3e820b388eeee701368cc88c9fbf8602` | YES |
| `ear-infections` | `6dcd3d2d97059d23c10401d8aeda3f0d4b29aa4af602705fd1a1c574b53a6e54` | `6dcd3d2d97059d23c10401d8aeda3f0d4b29aa4af602705fd1a1c574b53a6e54` | no |
| `sepsis` | `7237b6d2d62554dcb7228d9121754e04f9c497768e8368384701ab22bfd2e96f` | `f0b8160db59e3177f2b24cde589c53ca97fc98566a211769e1e0d58d29af74b3` | YES |
| `sinusitis` | `e9931b60be52fe16257618141c6ac9c0a9e24a3d4fd8741c7c31316704885531` | `e9931b60be52fe16257618141c6ac9c0a9e24a3d4fd8741c7c31316704885531` | no |
| `sore-throat` | `b451881e86dd066a743e7eb0a6c257def4e2bcbcd4d925a5613a6f9e38e0daa9` | `b451881e86dd066a743e7eb0a6c257def4e2bcbcd4d925a5613a6f9e38e0daa9` | no |

`appendicitis`/`sepsis` changed exactly as ADR-0032 AR-4 predicted. But
`death-fixture` ALSO changed — one of AR-4's own five "must stay
identical" roots, triggering that paragraph's own STOP-AND-ESCALATE
clause literally, not a re-baseline. Stopped and read `death-
fixture.json` directly rather than guessing: its `Stabilization_
Procedure` state carries `"duration": {"low": 30, "high": 30, "unit":
"minutes"}` — a genuine duration-bearing Procedure AR-4's own three-root
survey (`appendicitis`, `sepsis`, the UTI closure) never enumerated,
because it is a hand-authored fixture, not a vendored module, and sat
outside the "vendored roots" framing the survey was scoped to. The fix
behaved exactly as ADR-0032 AR-2/AR-3 specify — advance time for ANY
Procedure carrying `:duration` — so this is a corrected census, not a
fix defect.

**Escalated to the author before proceeding** (`AskUserQuestion`,
mid-Step-3): ruled to (1) correct AR-4 with a dated note reclassifying
`death-fixture` from the identity set into the duration-bearing set
(now four: `appendicitis`/`sepsis`/`death-fixture`/UTI) and accept its
new digest as baseline going forward, and (2) disclose, not fix under
this session, that `digest.clj` has never covered `total_joint_
replacement` or the UTI closure at all (a pre-existing six-root scope
from the post-Wave-D cleanup session, not introduced here) — both
recorded as dated notes on ADR-0032's own AR-4.

Corroborating evidence for the two roots `digest.clj` can't byte-verify:
`clojure -M:poly test :all skip:integration` stayed 0 failures/0 errors
both before and after the fix, including `vendored-uti-test`/
`vendored-tjr-test`'s own interpreter-layer walks (insensitive to timing
shifts by construction — they assert `:terminal`/`:blocked`/`:horizon-
complete`, never a literal digest) and the three J3 round-trip pins
(untouched, `engine.clj` not entered this session). A direct read of
`total_joint_replacement.json`/`functional_status_assessments.json`
confirmed neither of its two Procedure states (`Knee_Replacement_
Procedure`/`Hip_Replacement_Procedure`) carries a `:duration` field at
all (v1 or v2) — it has no mechanism to be affected regardless of oracle
coverage. A separate direct read of the UTI closure (`urinary_tract_
infections.json` + its 11 called submodules) confirmed 33 genuine
duration-bearing Procedure states (all v2-encoded), consistent with
AR-4's own "~30 states" estimate — this root DOES change in substance,
it simply isn't byte-verified by the current six-root tool. This is
disclosure, not a byte-digest oracle claim for those two roots — the
build-session skill's own VERIFICATION section names exactly that
distinction.

## Judgment calls and their ratification status

- **`death-fixture` reclassification and the `digest.clj` coverage gap
  were both put to the author via `AskUserQuestion` before continuing**,
  not resolved unilaterally — AR-4's own text names a digest change on
  an identity root as STOP-AND-ESCALATE, and the skill's own fix-forward
  rule requires asking rather than silently adapting on a premise
  mismatch. Both were ratified as presented (recommended options).
- **AR-2's own working name for the fix's call site,
  "`trajectory-and-advance`," names no function in the live tree** — the
  real call site is `emit-and-advance`. Corrected inline in ADR-0032's
  own AR-2 text (a naming mismatch between the ruling and the code, not
  a design disagreement) rather than silently using the wrong name or
  silently substituting the right one without saying so.
- **Extending `digest.clj` to cover `total_joint_replacement`/UTI was
  judged out of THIS session's own scope**, per the author's own answer
  — named as its own small follow-up rather than folded into a
  bug-fix session already carrying an oracle-partition correction.

## Findings and HEAD landed

- One real, unplanned finding this session: ADR-0032's own AR-4 survey
  (drafted 2026-08-03, same day, before this session ran) missed a
  duration-bearing Procedure in a hand-authored fixture — caught by the
  oracle bracket doing exactly the job AR-4 assigned it (prove BOTH
  halves), not by code review. A second, pre-existing finding
  (`digest.clj`'s six-root scope predates this session) was surfaced
  by the same bracket run and disclosed rather than silently worked
  around.
- This session ran under R30 (the standing default, ADR-0007/ADR-0023)
  — every checkpoint committed and pushed by this session itself, each
  verified against its own message file (the message-file/`git log`
  diff's only delta is `git log --format=%B`'s own trailing-newline
  artifact, at both checkpoints so far).
- Commits, in order: `1ea1f4a` (Step 1, the fix + focused test,
  red→green proven), `7587d1d` (Step 2, ADR-0032 capture), and this
  commit (Step 4 — the AR-4 dated correction, `gmf-interpreter.md`'s
  own D3c finding 1 note flipped to FIXED, roadmap Next→Done and the
  Deferred row annotated FIXED, this record and its paired prompt
  archive, both indexed). Step 3 (the oracle bracket) made no commit of
  its own — evidence only, per its own prompt.
- **Fence, explicit:** this session did NOT touch `engine.clj`, the
  three J3 pinned round-trip tests, or anything closure-registration-
  shaped (AR-6's SECOND, separate defect-fix session); did NOT extend
  `bin/oracle-src/ehrt/oracle/digest.clj` to cover `total_joint_
  replacement`/UTI (named, not built, its own follow-up); and did NOT
  touch the loader schema — exactly the prompt's own Fences section.
