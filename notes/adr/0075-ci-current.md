## ADR-0075 — CI current: the derived docs catch up, the staleness guard comes home, preflight learns to look

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-07.

### Context

Prior: the vendoring arc closed (`notes/adr/0074-vendoring-arc-close.md`,
tip `cd6c56c`). This session is a ruled fix between arcs, driven by the
author's own report: CI had been failing "for a while." Diagnosis,
probe-backed against the live remote: `test.yml`'s "generated-doc
freshness (regen + diff)" step regenerates the derived docs and `git
diff --exit-code`s them; the committed `docs/cli.md` was STALE — it
lacked the `--width` global-flag row (cli-spec changed at ux epilogue,
`notes/adr/0065-ux-epilogue.md`, AR-EP-3) and the `--board` row plus
`--ticker`'s precedence sentence (player-board, `notes/adr/0067-player-
board.md`). ROOT CAUSE, in the repo's own words: `docsgen_test.clj`'s
own docstring said "the real staleness guard is CI" — the freshness
check was DESIGNED CI-only (the pipeline/use-cases half genuinely needs
`python3` plus the mermaid converter, outside a JVM test), and the
design assumed a watched CI. Nobody watched: not the build sessions
landing the width/board work, and not the design channel's own
verification loop either — a gap named honestly here, in both
disciplines, not attributed to one alone.

**The red span is messier than "since ADR-0065," and that correction
is itself part of this record.** AR-CI-1's own enumeration started from
the LATEST run on each workflow — `integration.yml` green throughout
(scheduled/dispatch only, never gating a push); `test.yml`'s latest run
red at exactly the docsgen step, with exactly the three-row diff the
author pasted (`--width`, `--board`, `--ticker`'s precedence sentence).
That much matched the prompt's premise cleanly. Cross-checking the
"roughly twenty-five commits" framing against the live Actions history
(`gh run list`, every run's own step-level conclusions, not just the
latest) surfaced a second, unrelated fact the prompt's own sandbox
could not have seen (API unreachable there): `test.yml` had ALSO been
red across a run of EARLIER commits — starting at least `63f27e8`
(2026-08-06T15:05, "ux fixes 2" era, well before ADR-0065 landed) —
for a completely different reason: `poly test :all skip:integration`
itself failing on `ehrt.sim.run-test/merge-config-file-suggests-a-
same-stem-sibling-file` (`components/sim/test/ehrt/sim/run_test.clj:
306`), a test that builds its own temp-dir fixture and asserts a
same-stem `.md` sibling gets suggested. That failure is INTERMITTENT —
red on some commits (`63f27e8`, `d1bf847`, `01d9459`, `c6d2b19`,
`b52afdb`), green on the very next with no relevant code change, green
in every local run this session made — not a deterministic consequence
of any single commit's own content. It was not failing on the latest
run at enumeration time, so AR-CI-1's own literal check ("the latest
run") passed clean; a deeper trace (prompted by wanting the exact
commit count right, not the round one) is what surfaced it. Disclosed
to the author directly, mid-session, before any fix landed; ruled:
name it, don't fix it — this session's fix scope stays the docsgen
staleness alone, per AR-CI-5's own fence, and the flaky test is next-
arc intake, unchanged in this session.

The DETERMINISTIC docsgen-staleness red span, isolated from the flaky
overlap: `03a8698` (ADR-0065's own landing commit, 2026-08-06T23:17,
the first commit whose `docs/cli.md` diff this repo's own history
shows) through `cd6c56c` (2026-08-07T20:35, this session's own
baseline) — **32 commits inclusive**, not the "roughly twenty-five" the
driving prompt estimated from outside the sandbox. The correction is
recorded plainly rather than silently rounded back to the prompt's own
number.

R30 ceremony. Read-first (this session): `.github/workflows/test.yml`
(the docsgen step) and `integration.yml`; `components/docs-tooling/
test/ehrt/docs_tooling/docsgen_test.clj` (the docstring this session
amends); `Makefile`'s `docsgen` target; `ehrt.cli.help/write-cli-md!`
and `ehrt.corpus.operators-doc/write-operators-md!` (both confirmed
pure-JVM renders by direct read — no python dependency, unlike
`pipeline`/`usecases`); both `build-session` `SKILL.md` copies (the
Step-0 checklist AR-CI-3 amends).

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-07). `[A]` author-ruled, `[C]` channel-inferred.

**AR-CI-0 `[A — tag law, case (ii); debt recorded in ADR-0074]`.**
Annotated `stable-20260807-vendoring-close` at `cd6c56c`, message
"vendoring arc closed, design-channel-verified 2026-08-07 (ADR-0074)";
pushed; peeled ref verified (`git ls-remote --tags origin` resolves
`stable-20260807-vendoring-close^{}` to `cd6c56c` exactly). **Executed
Step 0, this session.**

**AR-CI-1 `[C — the enumeration first]`.** Both workflows' latest runs
checked (`gh run list`/`gh run view --log-failed`, direct API reads,
not assumed). `test.yml` red at exactly the docsgen step, exactly the
pasted diff; `integration.yml` green. **The enumeration also surfaced
the flaky-test finding above (Context) — not itself a STOP-AND-REPORT
trigger under AR-CI-1's own literal "check the latest run" test, but
disclosed and ruled (name it, don't fix it) before proceeding, since
the letter of a stop-clause and the spirit of an honest enumeration
aren't always the same test.** **Executed Step 0, this session.**

**AR-CI-2 `[C — the guard comes home]`.** `docsgen_test.clj` gains
`cli-md-is-current-test` and `operators-md-is-current-test` — each
renders from the live spec/registry in-process and compares byte-for-
byte against the committed file, no git involved; `cli-md-is-current-
test` witnessed red first. The namespace docstring's "the real
staleness guard is CI" sentence rewrites to state the new division.
`make docsgen` then regenerates; the regenerated files land.

**Executed, with two disclosed deviations, both `[C]`-premise
conflicts fixed forward rather than escalated:**

1. **Neither new test could literally live in `docsgen_test.clj`.**
   `render-cli-md` is pure with no compile-time knowledge of the real
   `cli-spec` — that spec lives in `bases/cli`, and components never
   depend on bases (Polylith's direction is the reverse; `docsgen_
   test.clj`'s own pre-existing docstring already explains exactly
   this, for exactly this reason, about its OWN fixture-spec tests).
   `cli-md-is-current-test` lives in `bases/cli/test/ehrt/cli/
   help_test.clj` instead — the one brick that can see both `cli-spec`
   and the renderer (a component `bases/cli` already depends on)
   without inverting that direction. `operators-md-is-current-test`
   lives in `components/corpus/test/ehrt/corpus/operators_doc_test.
   clj` instead, beside its own renderer's existing tests — that
   renderer never lived in `docs-tooling` to begin with (ADR-0016's
   own circular-dependency finding kept it in the domain component),
   so its freshness test belongs there too, not in a sibling brick
   reaching across for no structural reason. Both `docsgen_test.clj`'s
   and `operators_doc_test.clj`'s namespace docstrings rewrite to name
   the real division: `cli.md`/`operators.md` now guarded locally AND
   in CI; `docs/dev/pipeline.md`/`docs/use-cases.md` stay CI-only (the
   `resource_equations_to_mermaid.py` dependency, named, not merely
   implied).
2. **`operators-md-is-current-test`, as first written, was itself
   flaky** — not from anything wrong with the renderer, but because
   `components/corpus`'s test suite shares one JVM across every test
   file in the brick, and `ehrt.corpus.operators-test`'s own registry-
   mechanics tests register throwaway entries (`:test-op`, `:e1`,
   `:no-doc-op`) into the SAME mutable registry atom at deftest-
   execution time — a documented, precedented pattern in that file's
   own comments, which every OTHER test in `operators_doc_test.clj`
   is immune to (they compare the render against `entries` live on
   both sides; a frozen-file comparison isn't). Fixed with a reset-
   snapshot-reload-restore sequence around the one comparison that
   needs a clean registry, using this component's own `registry-
   snapshot`/`reset-registry!` save-restore convention (precedented in
   `components/corpus/test/ehrt/corpus/check/schemas_test.clj`) rather
   than inventing a new mechanism or touching production code.

Witnessed: `cli-md-is-current-test` red against the pre-fix tree
(commit `4df5a65`), full suite otherwise green (65/66 assertions in
its own namespace, every other namespace clean). `make docsgen`
regenerated exactly `docs/cli.md`, byte-for-byte the pasted diff —
`docs/operators.md`, `docs/dev/pipeline.md`, `docs/use-cases.md` were
already current, confirmed by `git status` showing only the one file
touched (commit `b4c593f`). Both new gates green after; full suite
green (511 passes, 0 failures, 0 errors); `poly check` OK throughout.

**AR-CI-3 `[C — preflight learns to look]`.** Both `build-session`
`SKILL.md` copies gain one Step-0 checklist line: check the latest CI
conclusion for main and DISCLOSE it in the session record; a red CI at
preflight is a finding to report before proceeding, never silently
passed. **Executed** (commit `23935c7`) — added to the "Done when"
checklist (the closest literal analogue to a "Step-0 checklist" this
skill file carries; its numbered "Procedure" section's own step 2 is
prose, not a checklist), right after the existing ext4-clone-
confirmation line, in both `.agents/skills/build-session/SKILL.md`
(canonical) and `.claude/skills/build-session/SKILL.md` (mirror) —
verified byte-identical after editing (`diff`, clean; `ehrt.docs-
tooling.skill-mirror-currency-test`, 216 assertions, green). The
design channel's own contract note in `.agents/state.md` is NOT edited
here, per this session's own prompt — state regenerates at arc close;
this ADR carries the lesson until then, the same deferral AR-EP-5
used.

**AR-CI-4 `[C — the proof uses the mechanism]`.** The claim "CI is
fixed" is proven by CI: after the final push, the Actions run for this
session's own closing commit is watched to conclusion; run URL and
conclusion recorded in the session record. **Executed** — see the
session record for the watched run's own URL and conclusion (recorded
in a small follow-up commit after this ADR's own push, since the run
this ruling asks about is the push THIS ADR itself produces — the
verification necessarily follows it, per the same push-then-verify
discipline every commit in this session already ran under, not
squeezed into the commit it's verifying).

**AR-CI-5 `[C — scope]`.** No cli-spec changes, no workflow-file
changes, no Makefile changes, no other doc edits beyond `make
docsgen`'s own outputs. The oracle bracket shows all twenty-seven
batches identical. Standing untracked files untouched. **Held** —
`bin/regression-oracle cd6c56c 23935c7`: all twenty-seven vendored-root
batches IDENTICAL, soundness "yes outside ns form" (this session's own
src/test touches were confined to `bases/cli/test`, `components/
corpus/test`, `components/docs-tooling/test`, and the two `SKILL.md`
copies — no `components/oracle`, `components/sim*`, or any digest-
relevant path touched, so identity is the expected, not merely hoped-
for, result). Working tree was clean at session start and stayed clean
between every commit (`git status`, checked before each stage).

### Execution record

**Step 0 — preflight + tag + enumeration.** Cwd confirmed the ext4
clone; tip `cd6c56c` exactly; working tree clean. `clojure -M:poly
check`: OK. Full suite baseline green (`clojure -M:poly test :all
skip:integration`, exit 0). AR-CI-1's own CI-status enumeration:
`integration.yml` green (five most recent scheduled runs, all
success); `test.yml`'s latest run (`31216503285`, commit `cd6c56c`)
red at exactly the "generated-doc freshness (regen + diff)" step, the
`poly check` and `poly test :all skip:integration` steps both green in
that same run. AR-CI-0 executed: tag created, pushed, peeled ref
verified.

**Step 1 — the guard, red (AR-CI-2 first half).** Both new tests
written (see Decision, above, for the two disclosed location
deviations and the registry-isolation fix); `cli-md-is-current-test`
witnessed red. Committed `4df5a65` ("test: the staleness guard comes
home -- cli.md's drift witnessed locally for the first time (ci
current, AR-CI-2 red)"), pushed.

**Step 2 — the catch-up, green (AR-CI-2 second half).** `make
docsgen`; `docs/cli.md` alone changed, byte-for-byte the expected
diff; both gates green; full suite green; `poly check` OK. Committed
`b4c593f` ("fix: the derived docs catch up -- width and board reach
the manual, gated locally now (ci current, AR-CI-2)"), pushed.

**Step 3 — preflight amendment (AR-CI-3).** Both skill copies, one
line each, mirror gate green. Committed `23935c7` ("docs: preflight
learns to look -- CI status joins Step 0 (ci current, AR-CI-3)"),
pushed.

**Step 4 (this entry) — ADR-0075 + record.** This file lands;
`notes/ADRs.md` gains its index line; `notes/adr/README.md`'s own file
count corrected 72→73 ("as of ADR-0075"), verified by `ls`, not
arithmetic. Roadmap gets its own Done pointer, appended alongside
ADR-0074's (this is not an arc close — nothing rotates):

```
- 2026-08-07 — ci-current — ADR-0075
```

**Oracle bracket** (`bin/regression-oracle cd6c56c <this session's own
tip>`): this session's own touches were docs/test/skill-file only —
no `src/`, `deps.edn`, or `workspace.edn` in any component the oracle
digests. All twenty-seven vendored-root batches confirmed byte-
identical; see Verification, below.

### This close's own mechanical debt, recorded here

**The next session that opens fresh work tags `stable-20260807-ci-
current` at THIS session's own closing tip, under standing ceremony.**
No tag is created by this session for its own closing tip — the tag
law's own case (ii) licenses a session to tag its PREDECESSOR's
verified stable point, not its own mid-flight tip; this session
inherits `stable-20260807-vendoring-close` (AR-CI-0, above) and passes
its own tag forward exactly the same way.

### The horizon, restated unchanged

This session was a ruled fix between arcs — it did not touch the
horizon the vendoring arc close named (`notes/adr/0074-vendoring-arc-
close.md`, "The horizon note"): the EncounterEnd design pass, Wave E's
own register, vendoring batch 4, pairing-as-data, publish-prep. None
of those were in scope here and none were touched.

### The flaky-test finding, named for next-arc intake

**`ehrt.sim.run-test/merge-config-file-suggests-a-same-stem-sibling-
file`** (`components/sim/test/ehrt/sim/run_test.clj:306`) fails
intermittently on GitHub-hosted CI runners — observed red on `63f27e8`,
`d1bf847`, `01d9459`, `c6d2b19`, `b52afdb` (all between 2026-08-06T15:05
and 2026-08-07T00:56), green on immediately adjacent commits with no
relevant code change, and green in every local run this session made.
The test builds its own temp-dir fixture (a same-stem `.md` sibling
beside the `.edn` path it looks up) and asserts `merge-config-file`'s
`:did-you-mean` payload names it; on a bad run, the actual value is
`nil`. Not investigated further this session — named, disclosed to the
author mid-session, and ruled out of scope (AR-CI-5's own fence): a
separate root-cause session should look at whether this is a real
race in `merge-config-file`'s own same-stem lookup or purely a CI-
runner filesystem-timing artifact, since it currently fires on
roughly one push in five to seven and could, by chance, land on any
future session's own closing-commit CI watch (AR-CI-4-style) and read
as a fix failure that it isn't.

### Verification

- `clojure -M:poly check`: OK, every step this session.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline (exit 0); red with exactly one failure
  (`cli-md-is-current-test`) after Step 1's own edits, witnessed;
  green again after Step 2 (511 passes, 0 failures, 0 errors).
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks` also ran automatically on every push (pre-push hook),
  clean throughout (725→727 commits scanned across this session's
  three pushes so far).
- Post-push message verification, every commit this session: one
  delta each against the message file, the known harmless trailing-
  blank-line artifact prior sessions already name.
- `bin/regression-oracle cd6c56c <this session's own closing tip>`:
  all twenty-seven vendored-root batches IDENTICAL, soundness "yes
  outside ns form."
- Tag verification: `stable-20260807-vendoring-close` peeled ref
  resolves to `cd6c56c` exactly (`git ls-remote --tags origin`).
- AR-CI-4's own watched run: see the session record for this closing
  commit's own Actions run URL and conclusion (recorded in a small
  follow-up commit, per the timing note under AR-CI-4 above).

### Fences

Everything AR-CI-5 names, held: no cli-spec change, no workflow-file
change, no Makefile change, no doc edit beyond `make docsgen`'s own
`docs/cli.md` output. The flaky `merge-config-file` test found during
enumeration was named and disclosed, never fixed — a separate root-
cause session owns that, per the author's own ruling mid-session
(Context, above). The `integration.yml` freshness of every OTHER
`test.yml` step beyond the docsgen one and the flaky test named above
was not re-audited past what AR-CI-1's enumeration covered — a fresh,
unrelated failure surfacing on a future run is that future session's
own finding, not retroactively this one's.

### Consequence

The derived-docs staleness that had kept `test.yml` red since ADR-0065
landed — 32 commits, not the round "twenty-five" the driving prompt
estimated from outside a sandbox that couldn't reach the Actions API —
is fixed: `docs/cli.md` carries `--width`, `--board`, and `--ticker`'s
real precedence now, byte-for-byte what the live `cli-spec` says. The
staleness guard that used to live CI-only now has a JVM-local half too,
for the two generated docs that don't need python — witnessed red
before it was made to pass, the same discipline every enforcement gate
in this repo is held to. Preflight itself gained a line: a session
starting fresh now checks CI's own pulse before touching anything,
named as the gap that let this staleness run unwatched for over four
weeks of session-time. A second, unrelated, genuinely intermittent
test failure was found in the course of getting the first number
right rather than repeating an estimate — named for the next session
rather than folded into this one's scope, per the author's own
mid-session ruling. The vendoring arc's own successor tag debt is
inherited and passed forward again, unchanged in kind. CI on `main`
was watched green (or, if not, STOP-AND-REPORTED) for the first time
since ADR-0065 — see the session record for which.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

CI current: the derived docs catch up, the staleness guard comes home, preflight learns to look — `docs/cli.md` had been stale since ADR-0065 (32 commits, not the estimated 25), CI red the whole time with nobody watching; docs/cli.md and docs/operators.md gain local, in-process staleness gates alongside CI's own; a second, unrelated intermittent test failure found and named for next-arc intake, not fixed here
