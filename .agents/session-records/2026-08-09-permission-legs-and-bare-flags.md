# 2026-08-09 — Permission legs and bare flags

## Scope

Session prompt executing the roadmap Next row anchored at the review-2
arc close (ADR-0096 Finding 1 / ADR-0097 AR-AC-1 item 1): `ehrt gate
fhir PATH`'s own permission-denied leg, still raising a raw
`FileNotFoundException` past `judge-fhir-official`'s `verdict-cache-
lookup`. Widened by author ruling ("Q1 a.") to the full judge family —
`judge-v2-hapi` and `judge-v2-nist` share the identical `.isFile`-only-
guard-then-bare-`slurp` defect shape — and carrying the D8-4 rider
("I choose a."): bare/`help`-level unknown flags, previously silently
swallowed into help text, now route through the same `:unknown-flag`
category a real subcommand's own typo already used. Two fix commits
landed: judge-family entry guards (three components), then the D8-4
rider (`bases/cli` only). This is ADR-0098.

## Red→green evidence highlights

Judge family: `git stash push --keep-index` isolated the three fixed
`src` files from their own new tests — pre-fix 11 failures/2 errors
(uncaught `FileNotFoundException` in both v2 engines' new permission-
denied tests; assertion failures everywhere the new categorized shape
was checked); post-fix 59 tests/168 assertions, 0 failures/0 errors.
CLI-level: `ehrt gate fhir` on a chmod-000 file went from a raw stack
trace bottoming at `fhir.clj:409`/`kernel/digest.clj:11` (exit 1) to
`{:status :error, :category :file-not-found, :payload {... :reason
:permission-denied}}` (exit 2); the missing-path leg — previously ALSO
raw, a disclosed finding, not assumed — categorizes the same way
without the `:reason` key.

D8-4 rider: same stash-and-restore technique on `bases/cli/src/ehrt/
cli/core.clj` alone — pre-fix 12 failures (both new `core_test.clj`
tests landing on `:category :cli-help`/exit 0 instead of the expected
`:unknown-flag`/exit 2); post-fix `ehrt.cli.core-test` 259 tests/762
assertions, 0 failures/0 errors. CLI-level: `ehrt --hlep` and `ehrt
help --hlep` both moved from "help text, exit 0" to `{:category
:unknown-flag, :payload {... :did-you-mean "--help"}}`, exit 2; `ehrt
--help` alone, `ehrt help`, and `ehrt help gate` all confirmed
unchanged.

Full-session totals: `clojure -M:poly check` OK at both commits;
`ehrt.cli.cli-parse-guard-lint-test` unaffected (3 tests/18 assertions,
0/0 — neither fix touches a bare read call site the lint scans);
full local suite (`clojure -M:poly test :all skip:integration`) 0
failures/0 errors anywhere in the log, 4m29s; `bin/verify-nist-lock`
OK (6 coordinates matched); `make docsgen` produced no `docs/cli.md`
diff (verified structurally, not just by omission — the ruling that
`docs/cli.md` stays untouched holds). Oracle bracket
(`bin/regression-oracle 558e6bf 104329f`, this session's opening tag to
the post-fix tip, both fix commits included): all THIRTY-FOUR roots
IDENTICAL — pure identity, exactly as predicted (this session touches
only judge components and CLI-shell code, never a digested root's own
vendored path).

## Judgment calls and their ratification status

- **`verb-label` strings for the two rider branches** (`"ehrt"` for
  bare, `"help"` for the help-verb): a naming choice within the ruled
  category shape, mirroring `flag-validation-context`'s own group-as-
  verb-label fallback. Not separately ratified — session discretion,
  disclosed in ADR-0098's own Decision section.
- **`check-readable`'s placement and shape in `judge-fhir-official`**
  (a shared private fn, called at the top of both `gate-file` and
  `gate-batch`, fail-fast on the first bad path in `gate-batch`): the
  channel-inferred placement ruling (verified against the live tree —
  `sha256-file` consumed at six sites across four components) plus a
  mechanical fail-fast choice matching `gate-batch`'s own existing
  first-failing-step contract for every other step. Session
  discretion within a ruled constraint, not a deviation.
- **"README.md (count only)" resolved to `notes/adr/README.md`**, not
  the root `README.md` (which carries no ADR-count text at all) —
  by precedent (both ADR-0096 and ADR-0097 updated this same file),
  disclosed in the prompt archive's own deviation record, not asked
  about mid-session since the precedent was unambiguous.
- **`.agents/rulings.md` entry shape**: Q1 recorded one-off (this
  session's own widening decision), Q2 recorded `standing` (a general
  category-shape convention). A phrasing judgment call, disclosed in
  the prompt archive.

## Findings and HEAD landed

**Finding, not a defect:** `judge-fhir-official/gate-file`'s own
missing-path leg (`ehrt gate fhir /nonexistent/no.json`) was ALSO raw
before this session — not just the permission-denied leg the roadmap
row named. Both are now covered by the same first-ever entry check;
disclosed in ADR-0098 rather than silently folded into "the fix," per
the driving prompt's own "report the actual" instruction for this leg.

**A newly-found, unrelated flake, disambiguated not smoothed past:**
the first post-fix full-suite run reported one failure,
`ehrt.sim-engine.engine-test/mixed-authored-and-compiled-run-
satisfies-the-full-invariant-catalog` — an unpinned `defspec`, entirely
outside this session's own fence. Re-run three times in isolation
(fresh random seeds each time): passed all three, a genuine
intermittent, not a regression this session's judge/CLI changes could
cause. Disclosed in ADR-0098's own Deviations section, not fixed
(out of fence); the full suite re-run clean immediately after, so the
close-phase commit never carried a knowingly-failing test.

**Tag paid forward:** `stable-20260809-review-2-arc-close` tagged at
`558e6bf` (Step 1, this session), the successor-tag debt ADR-0097's
own mechanical-debt section named — peeled ref verified.

**HEAD landed:** [recorded after the combined push — see this
record's own dated verification fill-in below, or the git log
directly for the final SHA].
