# 2026-08-05 — Standing-equipment promotion: census into sim-trajectory, oracle digest into a component (J2 closed)

## Scope

Design-channel session prompt naming AR-P-1 through AR-P-5, promoting
the two pieces of standing equipment the cleanup-arc seeds named — the
GMF census tool and the regression oracle's digest producer — into the
tested tree, and closing the J2 deferred row structurally. Move-don't-
improve throughout: relocations verbatim; the digest's interface-
repoint is the one sanctioned improvement (AR-P-2), licensed because it
is what makes the promotion possible at all. Step 0 verified tip
`f9830ec`; a fresh call-position census of every `digest.clj` dependency
found exactly one gap (`ehrt.sim-trajectory.gmf-interpreter/
dob-epoch-day`, not yet on the interface) — every other var it calls
was already there; census's own dependencies (`gmf`, `gmf-interpreter`)
become intra-component once it moves, legal by construction.

Step 1 (`a17fab1`, AR-P-1) moved `ehrt.sim-trajectory.census` and its
7-test co-landing suite from `development/{src,test}` into
`components/sim-trajectory`. Real finding, disclosed, fixed forward:
running these tests under `poly test` for the first time ever (a
full-suite baseline before this step: 202 `Test results:` blocks, zero
census assertions among them) surfaced two stale fixtures —
`load-failed-json`/`walk-failed-json` had each named a state/condition
type (`VitalSign` / `Vital Sign`) that GMF coverage Wave VS (ADR-0039,
2026-08-04) had already made real THREE SESSIONS before this test file
was ever actually exercised, so both fixtures silently stopped
exercising their own `:load-failed`/`:walk-failed` verdicts. Swapped to
deliberately fictional type names (`NoSuchStateType` /
`"No Such Condition Type"`) rather than another real-but-deferred type
— `gmf.clj`'s own closed whitelists (`gmf-type->keyword`,
`evaluate-condition`'s `case` default) make a fictional name
permanently unrecognized, immune to the next coverage wave going stale
the same way this one did.

Step 2 (`c065cdd`, AR-P-2) created `components/oracle`;
`ehrt.oracle.digest` moved out of `bin/oracle-src` (deleted) with four
require repoints: `ehrt.sim-trajectory.gmf`/`gmf-interpreter` collapse
into `ehrt.sim-trajectory.interface` (gaining `dob-epoch-day`);
`ehrt.sim-engine.engine` repoints to `ehrt.sim-engine.interface`
(already exposing `run` under the same name — those call sites are
byte-unchanged); `sim-model`/`emit-hl7` were already interface-clean.
`run-walk`'s own 6-arg interpreter call becomes an 8-arg interface call
with the SAME `{}`/`{}` defaults the impl's own 6-arg arity was already
filling in internally (verified against `gmf_interpreter.clj`'s own
arity chain — not a behavior change). Verified BEFORE committing: the
promoted digest run standalone is byte-identical (sha256, all 11 roots)
to `bin/oracle-src`'s own pre-promotion digest run in a disposable
worktree at `f9830ec`. `components/oracle` documented in `AGENTS.md`
and `docs/dev/architecture.md` (bricks table + mermaid) — the
structure-currency gate caught its own absence red, then green, live.

Step 3 (`3da479e`, AR-P-3) redesigned `bin/regression-oracle`: per-
worktree resolution (`oracle_wiring_for` — a worktree carrying
`components/oracle` uses it via `ehrt.oracle.interface`; one that
doesn't, transitionally, falls back to its own `bin/oracle-src` via
`ehrt.oracle.digest` directly), a cross-side soundness check (diff
outside the digest's own `(ns ...)` form — identical there proceeds
silently, anything else demands `--declared-digest-change` or aborts),
and the flag itself, recorded in the printed manifest header.

## Red→green evidence highlights

Four live proofs run before Step 3's own commit:

1. Same-ref bracket at the pre-promotion tip (`f9830ec f9830ec`) via
   the fallback branch: **IDENTICAL**, all 11 roots.
2. Same-ref bracket at the Step-2 tip (`c065cdd c065cdd`) via the
   `components/oracle` branch: **IDENTICAL**, all 11 roots — digests
   matched proof 1's own values exactly, confirming both branches
   produce identical content.
3. Mixed-side bracket (`f9830ec` vs `c065cdd`) WITHOUT the flag:
   **aborts, exit 1**, the ns-form-external diff printed as evidence.
4. The SAME mixed-side bracket WITH `--declared-digest-change`:
   **proceeds, IDENTICAL, all 11 roots** — in substance AR-P-5's own
   bracket, re-run and recorded formally below at the final code tip.

**AR-P-5's own formal verification** (Step 5, after Step 3 landed):
`bin/regression-oracle f9830ec 3da479e --declared-digest-change`.
Soundness check:

```
== soundness check: digest.clj outside its own (ns ...) form ==
DIFFERS outside the (ns ...) form -- --declared-digest-change asserted, proceeding anyway
```

The printed diff is EXACTLY the AR-P-2 interface repoints — four
require-target renames, `run-walk`'s 6→8-arg expansion (with its own
explanatory comment), and the mechanical `gmf/`→`sim-trajectory/`
call-site rename across ten producer functions — nothing else,
inspected directly, not merely asserted. Manifests:

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
`IDENTICAL: every root's digest matches between f9830ec and 3da479e` —
all ELEVEN batches, expected-change set NONE, exactly as AR-P-5 ruled.
(Step 4's own commit, `68ebeac`, is docs-only — `digest.clj` unchanged
since `3da479e`, so this bracket's verdict still holds at HEAD.)

**Deftest parity** (deftest-only count, per ADR-0043 AR-D-6's own
disclosed convention — zero `defspec` forms in either moved file, so
the two definitions coincide here): `census_test.clj` carries 7
`deftest` forms, now counted for the first time by every real project
that composes `sim-trajectory` (`ehrt-cli`, `conformance`,
`integration`) — confirmed by the full-suite block count rising from
202 to 204 `Test results:` blocks (two additional project contexts now
running the same 7 tests). `digest.clj` carries zero `deftest`/
`defspec` forms — no tests move with it; this gap is disclosed in
ADR-0044's own Fence, not built (out of this session's own AR-P-4/
"relocation and test-exercise only" license).

**Façade seam**: `git diff --stat f9830ec..HEAD --
components/sim/src/ehrt/sim/interface.clj` is empty — the façade file
itself untouched, confirmed by diffstat, not merely asserted.

**Full session diffstat** (`git diff --stat f9830ec..HEAD`): 12 files
changed, 597 insertions, 87 deletions — exactly the files AR-P-1
through AR-P-4 name (census's two files, the oracle component's three,
`bin/regression-oracle`, `deps.edn`, `AGENTS.md`/`architecture.md`,
`notes/ADRs.md`, `roadmap.md`); no stray files.

`clojure -M:poly check`: clean at every one of the four code/docs
commits. `clojure -M:poly test :all skip:integration`: 204
`Test results:` blocks, 0 failures/0 errors, at every commit from Step
1 onward.

## Judgment calls and their ratification status

- **Test-fixture staleness (Step 1): fixed forward rather than
  disclosed-only.** AR-P-4 licenses "relocation and test-exercise,
  nothing else" for CENSUS BEHAVIOR; this finding is a TEST-FIXTURE
  premise going stale (a downstream coverage wave overtaking a hand-
  authored "still deferred" fixture), not a census behavior change —
  read as in-scope for "test-exercise" (Step 1's own explicit
  requirement: "Full suite green... show them in the run output"),
  since leaving the fixtures broken would have made Step 1
  unachievable under its own stated success condition. Fixed with a
  future-proofing choice (fictional type names) rather than another
  real-but-currently-deferred swap, matching this exact file's own
  prior precedent (the VitalSign-for-ImagingStudy swap its own
  docstring already narrates) one layer more durable.
- **Citation gap, disclosed rather than silently repeated.** This
  session's own driving prompt attributed the census/`poly test`
  invisibility to "the roadmap's own Wave I finding." A direct grep of
  `.agents/plans/roadmap.md` for that framing found nothing — GMF
  coverage Wave I/Wave I2 (2026-08-04, ADR-0040/ADR-0041) are a
  different, unrelated arc that happens to share the letter "I". The
  underlying claim was verified independently instead (a live
  before/after `poly test` run, 202 blocks before this promotion with
  zero census assertions, 204 after) — the ADR records the corrected
  provenance rather than repeating an unfindable citation.
- **`bin/regression-oracle`'s Step 2 edit (the minimal classpath swap,
  not yet the full AR-P-3 redesign).** Between Step 2's commit and Step
  3's, the script pointed unconditionally at `components/oracle` with
  no fallback — genuinely unable to resolve a pre-promotion baseline
  ref for those few minutes of session time. Accepted deliberately:
  Step 2's own "Full suite green" gate is `poly check`/`poly test`, not
  a live run of a bash script outside that gate; the same-session,
  immediately-following Step 3 commit is what makes the script whole
  again, and the header comment landed in Step 2 says so explicitly
  rather than describing a mechanism that didn't exist yet.
- **`poly/oracle` added to the root `deps.edn`'s `:dev` alias**, not
  named explicitly in AR-P-2's own text. Done for the same reason every
  other real component is listed there: without it, `poly check`/`poly
  test` have no project-level edge reaching `components/oracle` at all,
  and the dev REPL (the whole point of the `:dev` alias) couldn't load
  it. Consistent with how census (equipment, not API) was already
  reachable from `:dev` before this session, just via `extra-paths`
  instead of a `:local/root` dep.

## Findings (disclosed, not fixed — out of this session's own scope)

- **`digest.clj` itself carries zero unit tests** (confirmed this
  session, `grep -c deftest` = 0) — a real, disclosed test-coverage gap
  for `components/oracle`, not built this session (AR-P-2's own "write
  NO new tests beyond what moves" fence). Revisit trigger: a future
  session that wants real unit coverage for digest.clj's own pure
  helpers (`mixed-seeds`, `run-walk`'s default-expansion logic) rather
  than relying solely on the oracle bracket's own end-to-end proof.
- **Census tool refinements (a/b/c, ADR-0035/ADR-0036) stand,
  untouched** — the substance qualifier, per-module seed override, and
  same-calendar-day filename collision are unrelated to this session's
  own scope (AR-P-4) and were not tempted into fixing during the move,
  per the ruling's own explicit fence.

**HEAD landed:** `68ebeac` before this record's own commit; this record
and its paired prompt archive land as the final commit of the session.
