# 2026-08-06 — UX epilogue: muscle memory gets an answer, help gets a width

## Scope

Session prompt naming AR-EP-0 through AR-EP-6, triggered by the
founding incident's own command shape resurfacing live from the
author's own shell the same day the UX arc closed. Diagnosed (probe-
backed against the live tree, not assumed): `:cli` never existed in
the monorepo root `deps.edn`, so `clojure -M:cli run ...` falls
through to `clojure.main` treating the bare verb `run` as an
init-script path. Landed: a tombstone alias that redirects to
`bin/ehrt` instead of throwing (AR-EP-1); a real hint for the sibling
near-miss, `bin/ehrt run` (a bare top-level verb crossing a group
boundary, AR-EP-2); the `--width`/COLUMNS help-rendering affordance
ADR-0064's own close deferred (AR-EP-3); two long-dangling FIXED
Deferred rows relocated to the attic (AR-EP-4); two transcript-
witnessed corrections from the arc-close verification repo-recorded
(AR-EP-5). Full account, rulings verbatim, live-probe transcripts, and
the oracle bracket: `notes/ADRs.md` ADR-0065.

Step 0 (preflight) confirmed the working directory is the ext4 clone,
tip `2e77096`, working tree clean apart from the pre-existing
untracked `config/busy-weekday.md`. Baseline: `clojure -M:poly check`
OK; full suite green (511 assertions in the tail component, 0
failures/0 errors across the full log); oracle self-bracket
(`bin/regression-oracle 2e77096 2e77096`) all eleven roots IDENTICAL.
AR-EP-0 executed directly (current tag law licenses a session to tag
its own predecessor's verified stable point under standing ceremony):
`stable-20260806-ux-close` created annotated at `2e77096`, pushed,
peeled ref verified.

Step 1 (`8e9d078`, AR-EP-4) relocated the two FIXED-marked Deferred
rows into `.agents/plans/roadmap-done-2026-08.md`, notes intact, each
with a dated relocation note. Deferred row count after: 11.

Step 2 (`4bb14a1`, AR-EP-1) landed the tombstone: root `deps.edn`'s new
`:cli` alias, `bases/cli/src/ehrt/cli/retired.clj`, and two red-first
gates (`retired_test.clj`, `cli_tombstone_test.clj`) — both confirmed
red before the fix (namespace-load failure; three failing EDN-read
assertions), green after. Live probe: the author's own pasted command
shape, against the fixed tree, redirects on stderr with exit 2.

Step 3 (`bccd46a`, AR-EP-2/AR-EP-3) probed `bin/ehrt run --seed 1
--patients 1` first (generic `"run: ehrt help"` hint, confirming the
near-miss), then extended `unknown-command-error` with a new
`verb-name-groups` helper — red-first, confirmed failing before the
fix. Landed `--width`/COLUMNS: `ehrt.cli.help/resolve-width` and
`parse-width-flag` (pure), `global-flags`' new `--width` entry, and
`ehrt.cli.core`'s dispatch-layer threading (`resolved-help-width`, an
injectable `columns-env-fn`) — red-first at the dispatch layer,
confirmed failing before the fix. Extending `help_wrap_test` to
40/60/120 caught a real pre-existing bug (`render-top-level`'s own
"Run `ehrt help <group>`..." line was never wrapped, invisible at 80
columns, real at 40) — fixed in scope, not a wrap-algorithm change.
Live probe: `bin/ehrt help` at three widths, `COLUMNS=120` against the
real environment, the `--width abc` rejection case.

Step 4 (this record) authored `notes/adr/0065-ux-epilogue.md`
directly, appended its own index line to `notes/ADRs.md`, corrected
`notes/adr/README.md`'s own stale file count (62→63), added the Done
pointer (`- 2026-08-06 — ux-epilogue — ADR-0065`), ran the closing
oracle bracket, archived this prompt, and recorded this session.

## Red→green evidence highlights

Every fix this session was red-first at the layer where behavior
actually changes: `retired_test.clj`/`cli_tombstone_test.clj` (AR-EP-1,
namespace-load failure / 3 failing assertions → green), the
near-miss dispatch test (AR-EP-2, generic hint → `:did-you-mean`
present, green), the nine `--width`/COLUMNS dispatch-layer tests
(AR-EP-3, default-width text returned regardless of `--width` →
correctly-narrowed text, green), and `help_wrap_test`'s new
40/60/120 invariants (caught the unwrapped top-level line live, green
after the fix). Disclosed deviation: `ehrt.cli.help`'s own pure
`resolve-width`/`parse-width-flag` unit tests were authored alongside
their implementation, not strictly before it — the actually-observable
behavior (the dispatch-layer integration) WAS red-first. Full
non-integration suite green at every checkpoint: 511 assertions in the
tail component, 0 failures/0 errors in the full log (grepped, not
tail-inferred) at Step 0 baseline and after every step's own edits.
`clojure -M:poly check`: OK at every checkpoint.

## Judgment calls and their ratification status

- **`--width` joins `ehrt.cli.help/global-flags`, not a new "help
  flags" list.** Fresh read confirmed no such dedicated list exists in
  `cli-spec` — global-flags is the established join point every other
  command-wide flag already uses. Disclosed in its own `:doc` string
  that `--width`, unlike its siblings, affects help rendering only.
  Not escalated as STOP-AND-REPORT (the prompt's own named
  threshold) — the placement fit the existing structure; only the
  scope difference needed disclosure, not a structural fight.
- **`invocation-lint-test`'s scan surface does not cover
  `bases/cli/src`/`bases/cli/test`.** Checked live (fresh read of
  `scan-sources`) before writing `retired.clj`'s own source comment,
  which names `clojure -M:cli` literally for provenance — confirmed
  this needed no gate exemption, since the gate never reaches that
  file. Not a judgment call so much as a verification that a feared
  conflict wasn't real; recorded so a future reader doesn't re-check.
- **AR-EP-5's two corrections are repo-recorded in ADR-0065, not by
  editing `.agents/state.md` directly** — state.md regenerates at the
  next arc's own close per its own contract; editing it mid-arc would
  fight that contract rather than honor it. Follows the same pattern
  this session's own prompt named explicitly.

## Findings and HEAD landed

The unwrapped `render-top-level` line (above) was the one finding
outside the prompt's own named scope — caught by the 40/60/120 test
extension itself, fixed within AR-EP-3's own fence (applying the
existing wrap mechanism, not changing it).

Commits, in order: `8e9d078` (Step 1), `4bb14a1` (Step 2), `bccd46a`
(Step 3), and this session's own closing records commit (Step 4).

## Verification

- `bin/regression-oracle 2e77096 <this session's own closing commit>`:
  all eleven vendored-root batches IDENTICAL — a tombstone alias,
  help-width plumbing, and a hint touch no emitted byte, exactly as
  AR-EP-6 expected.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline and after every step, 0 failures/0 errors throughout.
- `clojure -M:poly check`: OK, every step.
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks detect -v` (full history) clean at every push.
- Post-push message verification, every commit: the known harmless
  trailing-newline artifact only, no other delta.
- Tag verification: `stable-20260806-ux-close` peeled ref resolves to
  `2e77096` exactly.
- Live probes (not only `clojure.test`): the founding command shape
  against the built `bin/ehrt`/`clojure -M:cli`; `bin/ehrt run`; `bin/ehrt
  help` at `--width 40`, `--width 60`, and `COLUMNS=120`; `bin/ehrt help
  --width abc`. Full transcripts: ADR-0065.

## Deviations, disclosed

- **Red-first was not strictly observed for `ehrt.cli.help`'s own two
  new pure functions** (`resolve-width`, `parse-width-flag`) — written
  alongside their unit tests rather than before them. The layer where
  this session's actual behavior change is observable — the
  `ehrt.cli.core` dispatch integration — WAS red-first throughout,
  confirmed failing before each fix. Named here rather than silently
  claimed as fully red-first.
- **The unwrapped `render-top-level` line** (Step 3, above) was fixed
  in scope rather than deferred, since AR-EP-3's own invariant
  extension is what surfaced it and leaving it broken would have
  shipped a known-red case for the width knob it was ruled in to
  serve.
