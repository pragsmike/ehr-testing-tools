# 2026-08-07 — CI current: the derived docs catch up, the staleness guard comes home, preflight learns to look

## Scope

Session prompt naming AR-CI-0 through AR-CI-5, a ruled fix between arcs
(not an arc close — `.agents/state.md` is unedited this session, per the
prompt's own explicit deferral). Driven by the author's report that CI
"has been failing for a while." Diagnosis: `test.yml`'s generated-doc
freshness step regenerates the derived docs and diffs them; `docs/
cli.md` was stale (missing the `--width` global flag, ADR-0065; the
`--board` row and `--ticker`'s precedence sentence, ADR-0067). Root
cause, from `docsgen_test.clj`'s own docstring: the staleness guard was
designed CI-only, and nobody was watching CI — a gap named in both the
build-session discipline and the design channel's own verification
loop. Full root-cause narrative, the corrected red-span accounting, and
every ruling: `notes/adr/0075-ci-current.md`.

This session's own preflight: working directory confirmed the ext4
clone, tip `cd6c56c` exactly, working tree clean. Baseline: `clojure
-M:poly check` OK; full suite green (`clojure -M:poly test :all
skip:integration`, exit 0). AR-CI-1's own CI-status enumeration (new
this session, per AR-CI-3's own amendment): both workflows' latest runs
checked directly via `gh run list`/`gh run view --log-failed`.
`integration.yml` green (five most recent scheduled runs, all
success). `test.yml`'s latest run (`31216503285`, commit `cd6c56c`) red
at exactly the "generated-doc freshness (regen + diff)" step, with
exactly the three-row `docs/cli.md` diff the author had pasted; `poly
check` and `poly test :all skip:integration` both green in that same
run.

## The enumeration's own second finding

Cross-checking the driving prompt's "roughly twenty-five commits since
ADR-0065" framing against the live Actions history (not just the
latest run — every run's own step-level conclusion, back past
ADR-0065's own landing) surfaced a fact the prompt's own sandbox
couldn't have seen: `test.yml` had ALSO been red across a run of
EARLIER commits, starting at least `63f27e8` (2026-08-06T15:05, well
before ADR-0065), for an unrelated reason — `poly test :all
skip:integration` itself failing intermittently on `ehrt.sim.run-
test/merge-config-file-suggests-a-same-stem-sibling-file`. Not a
deterministic consequence of any one commit: red on some (`63f27e8`,
`d1bf847`, `01d9459`, `c6d2b19`, `b52afdb`), green on the immediately
adjacent ones, green in every local run this session made. Not present
on the latest run at enumeration time, so AR-CI-1's own literal
"check the latest run" test passed clean — the deeper trace was
prompted by wanting the commit count right, not by the ruling's own
letter. Disclosed to the author directly, mid-session, via
`AskUserQuestion`, before any fix landed. Ruled: name it in ADR-0075,
don't fix it — this session's own scope stays the docsgen staleness
alone (AR-CI-5's fence); a future session owns the flaky test's own
root cause. The corrected, deterministic docsgen-staleness span:
`03a8698` (ADR-0065's own landing commit) through `cd6c56c` — 32
commits inclusive, not the estimated 25.

## Red→green evidence highlights

`cli-md-is-current-test` (added this session, `bases/cli/test/ehrt/
cli/help_test.clj`) witnessed red against the pre-fix tree: full suite
run after Step 1's commit showed exactly one failure across the entire
suite, in that one test, everything else clean. `make docsgen` then
regenerated exactly `docs/cli.md` — byte-for-byte the diff the author
had pasted — confirmed by `git status` showing only that one file
touched after regeneration; `docs/operators.md`, `docs/dev/
pipeline.md`, and `docs/use-cases.md` were already current. Both new
local gates green after Step 2's commit; full suite green (511 passes,
0 failures, 0 errors).

## Judgment calls and their ratification status

- **Neither new test could live in `docsgen_test.clj` as the prompt
  literally named.** `cli-md-is-current-test` needs the real `cli-
  spec`, which lives in `bases/cli` — components never depend on
  bases, so a components/docs-tooling test cannot reach it without
  inverting Polylith's own direction (`docsgen_test.clj`'s own
  pre-existing docstring already explains exactly this constraint,
  for exactly this reason, about its own fixture-spec tests). Fixed
  forward: `cli-md-is-current-test` lives in `bases/cli/test/ehrt/cli/
  help_test.clj` instead, the one brick that can see both the spec
  and the renderer without inverting the direction.
  `operators-md-is-current-test` lives in `components/corpus/test/
  ehrt/corpus/operators_doc_test.clj` instead, beside its own
  renderer's existing tests — that renderer never lived in
  docs-tooling to begin with. Both namespace docstrings (`docsgen_
  test.clj`, `operators_doc_test.clj`) rewrite to name the real
  division. Disclosed in the AR-CI-2 commit message and in ADR-0075,
  not silently relocated.
- **`operators-md-is-current-test`, as first written, was itself
  flaky within a single test run** — a shared-JVM registry-pollution
  hazard (`ehrt.corpus.operators-test`'s own registry-mechanics tests
  inject throwaway entries into the same mutable atom at
  deftest-execution time) found by actually running the full suite,
  not assumed. Fixed with a reset-snapshot-reload-restore sequence
  around the one comparison that needs a clean registry, using this
  component's own `registry-snapshot`/`reset-registry!` convention
  (precedented in `components/corpus/test/ehrt/corpus/check/
  schemas_test.clj`) — no production code touched.
- **AR-CI-3's "Step-0 checklist line" landed in the "Done when"
  checklist**, the closest literal analogue this skill file carries
  to a checklist (its numbered "Procedure" section's own preflight
  step is prose, not a checklist) — placed immediately after the
  existing ext4-clone-confirmation line, matching that line's own
  position and register.
- **The flaky-test finding was disclosed and ruled before any further
  work, not folded silently into the ADR's narrative after the fact.**
  Given the choice between fixing it now (out of this session's own
  named scope) or naming it for a later session, the author chose the
  latter — recorded in ADR-0075 as a named, unscoped finding for
  next-arc intake.

## Findings and HEAD landed

The docsgen staleness itself (fixed this session) and the intermittent
`merge-config-file-suggests-a-same-stem-sibling-file` failure (named,
not fixed — see ADR-0075's own "flaky-test finding" section for the
full account and the specific commits it was observed red on). No
other finding surfaced.

Commits, in order (this session): `4df5a65` (Step 1, the staleness
guard, red), `b4c593f` (Step 2, the docs catch-up, green), `23935c7`
(Step 3, the preflight amendment), and this record's own closing commit
(Step 4).

## Verification

- `clojure -M:poly check`: OK, every step this session.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline; exactly one failure (`cli-md-is-current-test`,
  witnessed) after Step 1; green again after Step 2 (511 passes, 0
  failures, 0 errors).
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks` also ran automatically on every push (pre-push hook),
  clean throughout.
- Post-push message verification, every commit this session: one
  delta each against the message file, the known harmless
  trailing-blank-line artifact.
- `bin/regression-oracle cd6c56c 23935c7`: all twenty-seven
  vendored-root batches IDENTICAL, soundness "yes outside ns form" —
  this session's own touches were confined to test files and skill
  docs, no digest-relevant path.
- Tag verification: `stable-20260807-vendoring-close` peeled ref
  resolves to `cd6c56c` exactly (`git ls-remote --tags origin`).
- AR-CI-4's own watched run: [`31221343315`](https://github.com/pragsmike/ehr-testing-tools/actions/runs/31221343315),
  commit `eb2319b` (this session's own ADR-0075/records commit) —
  **conclusion: success**, every step green including "generated-doc
  freshness (regen + diff)". Watched to completion (`gh run watch
  31221343315 --exit-status`), not merely polled once. The two
  preceding pushes this session (`b4c593f`, Step 2; `23935c7`, Step 3)
  had also already come back green by the time this run was checked —
  the flaky `merge-config-file` test named above did not fire on any
  of this session's own three pushes. This is CI green on `main` for
  the first time since ADR-0065 landed.

## Deviations, disclosed

- **The prompt's own "roughly twenty-five commits since ADR-0065"
  framing was corrected, not repeated.** The actual deterministic
  docsgen-staleness span is 32 commits (`03a8698`→`cd6c56c`); the
  prompt's estimate additionally conflated a separate, earlier,
  intermittent test failure with the docsgen staleness it was
  actually describing. Both the correction and the reason for it are
  in ADR-0075's own Context section, not silently smoothed over.
- **Two new tests landed in different files than the prompt literally
  named**, and one of them needed an isolation fix the prompt didn't
  anticipate — both are Polylith-structural and shared-JVM-testing
  necessities respectively, not stylistic choices; both disclosed in
  the AR-CI-2 commit message and in ADR-0075's own Decision section
  (see Judgment calls, above, for the reasoning).
- **A second, unrelated finding (the flaky `merge-config-file` test)
  surfaced mid-session and was disclosed to the author before any
  further work, per the "fix-forward with disclosure on premise
  mismatch" discipline** — the prompt's own premise (a single, clean,
  25-commit red span) didn't fully hold against the live tree, and
  that was reported rather than silently absorbed into a tidier
  narrative.
