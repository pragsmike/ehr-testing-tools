## ADR-0118 — Fix clusters B and C: help enrichment, doc drift, scan roots

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-12.

### Context

`.agents/plans/roadmap.md`'s "Fix cluster B" (help-surface enrichment)
and "Fix cluster C" (doc drift and gate scan-roots) rows (chartered
`notes/ADRs.md` ADR-0115, from review-3's own `fix-session-candidate`
register rows,
`.agents/plans/2026-08-12-review-3-user-surface-findings.md`) land in
this session: cluster C first (commit 1, docs-only), then cluster B
(commit 2, the help surface). Read first: the findings register's own
rows R3-B5-3, R3-B5-4, R3-B3-2, R3-B3-1; `components/docs-tooling`'s
invocation lint; `bases/cli/src/ehrt/cli/help.clj` + `core.clj`; the
rulings tail; the roadmap's own cluster B/C rows.

### Tag ceremony

`git fetch` confirmed `origin/main` at `c68ec3e`
(`c68ec3efcbe2421888071a23e7225c6716f7c6fa`, ADR-0117 close) at session
start — matched the driving prompt's own stated premise exactly. **The
last five `main` CI runs** (`gh run list --limit 5 --branch main`,
checked at session start): all `completed`/`success` — `c68ec3e`
(4m38s), `e9c8b55` (4m28s), `c058706` (3m39s), `5d05825` (4m40s),
`573bae4` (4m39s) — no red among the five, completing the one
channel-unverified leg the driving prompt's own license clause named.

License: tag-law case (i) — the design channel's own 2026-08-12
fresh-clone verification of the ADR-0117 landing (lineage, ASCII x5,
footprint, F1/F2 boundary diffs read directly, independent F7 sweep
census: zero live survivors), CI confirmed per this preflight.
`stable-20260812-fix-cluster-a` tagged ANNOTATED at `c68ec3e`; pushed;
peeled ref verified via `git ls-remote --tags origin` — exact match
`c68ec3efcbe2421888071a23e7225c6716f7c6fa`.

### Decision

#### Commit 1 (`b711aa6`) — cluster C: docs drift + lint scan-root widening

**Step 1 — widen first, the widening IS the red.**
`components/docs-tooling/test/ehrt/docs_tooling/invocation_lint_test.clj`'s
own `scan-sources` widened from four roots (`README.md`,
`AUTHORS-GUIDE.md`, `docs/**`, `components/*/docs/**`) to six, adding
`demos/**` and `.github/**` — R3-B5-4's own "consider `.github/**`" is
ruled YES [C, un-vetoed], same recurrence-prevention logic as
`demos/**` (the demo tree relocated wholesale out of
`components/*/docs/demos/` to a new top-level `demos/` tree, ADR-0073,
after this gate's scan roots were last set, leaving the successor tree
unprotected).

**Step 2 — RED, disclosed premise mismatch.** Running the widened lint
produced exactly **one** failure:
`.github/ISSUE_TEMPLATE/bug-report.md teaches the stale \`clojure
-M:cli\` invocation` (R3-B5-4's own issue-template alias) — RED,
exactly as predicted. R3-B5-3's own `demos/traces/**` stale
config-header drift (the pre-relocation `docs/demos/<name>/config.edn`
paths in `order-result/config.edn` and `module-mix/config.edn`'s own
header comments, plus `module-mix`'s `--seed 7`-vs-`71` mismatch) did
**not** trip anything — a genuine premise mismatch against the driving
prompt's own "it must go RED on the known drift (R3-B5-3 ...
R3-B5-4)" step. Confirmed structurally, not guessed: the widened
lint's own two checks are a literal `clojure -M:cli` substring match,
and a check that `--config`/`--profile`/`--path` values *inside fenced
\`\`\`bash/\`\`\`sh blocks* resolve to real paths — R3-B5-3's own drift
lives in plain `;;` EDN comments with no code fence at all, so neither
check can structurally see it regardless of scan-root widening.
Confirmed the drift itself was still live (not "already gone," the
STOP-AND-REPORT's other disjunct) by direct grep, and found one
instance the register itself never named:
`demos/traces/module-mix/README.md:108`'s own stale
`docs/demos/emit-state/` prose reference (found by the census grep,
below, not the lint).

**STOP-AND-REPORT raised, user resolved.** Presented the mismatch and
its evidence; the user chose "proceed as a disclosed gap" (the reading
step 3's own wording already anticipated — "plus any the grep finds
that the lint's own patterns miss — report the latter, don't silently
extend the lint") over stopping the session entirely or extending the
lint's own content patterns beyond scan-root widening.

**Step 3 — fix by census, not by lint-widening.** An extension-blind,
un-truncated grep of `docs/demos`, `clojure -M:cli`, old base/component
names, and pre-`ehrt`-mount `sim version`/`sim run`/etc. spellings over
`demos/**` and `.github/**` found exactly four live instances, three
named in the register and one not:

- `demos/traces/order-result/config.edn:1,6` — `docs/demos/order-result/config.edn` → `demos/traces/order-result/config.edn` (R3-B5-3)
- `demos/traces/module-mix/config.edn:1,6` — same class, plus `:5`'s own `--seed 7` → `--seed 71` (matching the sibling README's actual runnable command) (R3-B5-3)
- `.github/ISSUE_TEMPLATE/bug-report.md:16` — `clojure -M:cli version` or `sim version` → `bin/ehrt sim version` (R3-B5-4)
- `demos/traces/module-mix/README.md:108` — `docs/demos/emit-state/` → `demos/traces/emit-state/` (found by the census, not in the register, not caught by the lint's own patterns — a prose cross-reference, not an invocation)

**Green.** Re-ran the widened lint after the fixes: 0 failures, 249
assertions (up from 201 pre-widening). A second census sweep after the
fixes confirmed zero remaining instances of every pattern searched.

Full isolated `make test` (commit 1's exact diff, no other file
staged or dirty) run green before push: `clojure -M:poly check` +
`clojure -M:poly test :all skip:integration` (0 FAIL/ERROR) +
`bin/verify-nist-lock` (OK, all 6 hit-nexus-sourced coordinates).

#### Commit 2 (`ab11d7b`) — cluster B: help enrichment

**B1 (R3-B3-2) — verb-level help narrowing.**

*Red first.* Added tests to `help_test.clj` (pure-function level) and
`core_test.clj` (dispatch level) asserting: `help/render-verb-help`
renders exactly one verb's own content, nothing from its siblings;
`ehrt help <group> <verb>` and `ehrt <group> <verb> --help` both
narrow to that render; a known group with an unknown verb reuses F6's
own `:unknown-command` treatment (ADR-0117) verbatim, naming the
group's real verbs, at exit 2; a group with no verbs at all
(check/version/doctor/show/play) is unaffected by either invocation
form. Also updated the one pre-existing test whose own assertion
encoded the OLD (pre-B1) behavior —
`dispatch-explicit-width-narrows-double-dash-help-rendering-test` used
`["gate" "v2"] {:help true}` and asserted full-group text; B1 makes
that narrow, so the assertion changed to the narrowed expectation.

First run: 15 failures. One was a test-authoring bug, not a real
gap — `render-verb-help`'s own output for `sim run` legitimately
includes the substring `"ehrt sim check"`, because `--format`'s own
doc string cites `` `ehrt sim check` `` in prose (the pipe-into-check
example). Fixed by checking for each sibling verb's own section-header
shape (`"\nehrt sim check\n"`) instead of a bare substring. Re-ran: 14
failures, every one dispatch-level (`core.clj` not yet touched) — clean
RED, confirmed.

*Fix.* `help.clj` gained `render-verb-help` (one verb's own
description + flags + the shared exit-code table, nothing else).
`core.clj` gained `group-takes-verbs?` (scopes the whole feature to
groups that declare `:verbs` — artifact/corpus/gate/sim; a verbless
group's second positional was never a verb selector and stays
unaffected), `verb-known?`, and `verb-help-response`; both the
`(:help opts)` and `(= group "help")` dispatch branches gained an
unknown-verb check (reusing `unknown-command-error` with `[group
verb]` as its own `args`, so its own hint logic reads the GROUP as the
known token — `"run: ehrt help sim"`, not a generic fallback) ahead of
the narrowed-vs-full-group render choice.

*Green.* 0 failures, 1035 assertions (`help_test.clj` + `core_test.clj`
together).

**B2 (R3-B3-1) — sourced per-group examples.**

*Content rule* [C, approved by dispatch of the driving prompt]: one
invocation per group, copied VERBATIM from a witnessed source
(README.md's Quickstart, a `docs/use-cases/*.md` strip, or a demo
README), never composed; no Example line for a group with no witnessed
invocation anywhere. Sources, cited per line:

| group | Example line | source |
|---|---|---|
| `artifact` | `bin/ehrt artifact fetch --name synthea --version 4.0.0` | `README.md:203` (Quickstart) |
| `corpus` | `bin/ehrt corpus generate` | `README.md:197` (Quickstart) |
| `gate` | `bin/ehrt gate fhir test-fixtures/fhir/storefront-patient.json` | `README.md:92` ("See it run") |
| `check` | `bin/ehrt check out/repro-b/fhir --expected out/repro-a/fhir --pair-by hash` | `docs/use-cases/reproduction-packages.md:36` |
| `sim` | `bin/ehrt sim run --seed 100 --patients 1` | `README.md:226` (Quickstart) |
| `show` | `bin/ehrt show out/corpus/sim-s42-p5` | `docs/use-cases/generate-sim-traffic.md:28` |
| `play` | `bin/ehrt play out/corpus/busy-tuesday --board 60 --rate 60` | `README.md:33` ("See it run") |
| `version` | *(none)* | no witnessed invocation anywhere (checked: README Quickstart, all 21 `docs/use-cases/*.md`, all `demos/**/README.md`) |
| `doctor` | *(none)* | same gap class as `version` |

The `version`/`doctor` gap is recorded as a register addendum row
(`.agents/plans/2026-08-12-review-3-user-surface-findings.md`, after
the B3 table), not silently absorbed into the fix.

*Red first.* `help_test.clj` gained
`render-group-shows-a-sourced-example-line-for-every-covered-group-test`
(asserts `"Example:"` present for the 7 covered groups) and
`render-group-omits-example-for-groups-with-no-witnessed-invocation-test`
(asserts its absence for `version`/`doctor`) — both failing against the
pre-fix `cli-spec` (no `:example` key existed).

*Fix.* Each covered group's map in `cli-spec` gained an `:example`
string (with a source-citing comment beside it); `render-group` gained
an `"Example:\n"` section, rendered via `wrap-with-hanging-indent` (not
a bare unwrapped line — see the regression below) right before the
exit-code table. `render-verb-help` deliberately never shows it — the
narrowed verb screen is explicitly not "the whole group screen."

**A real regression caught by an existing gate, not a new one.** The
first Example-line implementation concatenated the raw invocation
string unwrapped. `help_wrap_test.clj`'s own pre-existing
`every-group-page-lines-fit-at-non-default-widths-test` (AR-U5-2(a),
ADR-0063 — no rendered line may exceed `--width` unless it is a single
unbreakable token) caught this immediately: 7 failures at widths 40 and
60, e.g. `"  bin/ehrt gate fhir test-fixtures/fhir/storefront-patient.json"`
at width 40. Fixed by routing the Example line through
`wrap-with-hanging-indent` like every other rendered field. Re-ran:
0 failures.

*Green, full CLI suite.* `help_test.clj` + `core_test.clj` +
`cli_parse_guard_lint_test.clj` + `retired_test.clj` +
`executable_bits_test.clj` + `help_wrap_test.clj` + `help_voice_test.clj`
+ `claude_md_presence_test.clj`: 354 tests, 3948 assertions, 0
failures.

`docs/cli.md` regenerated (`make cli-doc`) twice — before and after
the wrap fix — both times **byte-identical**, zero diff. Not an
oversight: `docsgen.clj`'s own `render-cli-md` builds the page purely
from `cli-spec`'s `:group`/`:doc`/`:verbs`/`:flags`/`:positional`
fields and states explicitly, in its own preamble, "What this page
deliberately does not carry: worked invocations" — it never reads
`:example` at all, and B1's verb-narrowing is a dispatch/render-time
behavior change, not a `cli-spec` shape change, so neither commit
reaches the page docsgen builds.

**Live verification** (not just unit tests): `bin/ehrt help sim run`,
`bin/ehrt sim run --help`, `bin/ehrt help sim frobnicate` (→
`:unknown-command`, exit 2, `:valid-options ["run" "check"
"identifiers" "version"]`, hint `"run: ehrt help sim"`), `bin/ehrt gate
frobnicate --help` (same shape for `gate`'s own verbs), `bin/ehrt help
artifact` (shows the Example line), `bin/ehrt help version` (no
Example line), `bin/ehrt check somedir --help` (unaffected, full group
render) — all matched the design exactly.

Full isolated `make test` (commit 2's exact diff) run green before
push: 0 FAIL/ERROR, `bin/verify-nist-lock` OK.

### Oracle bracket

Pre-analysis: pure identity expected across all 35 vendored roots —
every touched file is help text, docs, or lint config; no root
invokes `ehrt help`, reads any of the drifted demo/issue-template
files, or is reachable through the CLI's help-dispatch short-circuit at
all (the oracle's own golden runs never pass `--help` or a `help`
group token).

`bin/regression-oracle c68ec3e ab11d7b` (baseline: the tag; target: the
tip of commit 2, the close of the fix commits) →
**`IDENTICAL: every root's digest matches between c68ec3e and
ab11d7b`**, all 35 roots. Matches the pre-analysis exactly.

### Verification

`gitleaks git --staged -v`: clean, both commits. `git diff --cached
--stat` reviewed before each commit: exactly the fenced files, nothing
stray. Post-push, both commits: pushed message diffed against its own
source file (only the known trailing-blank-line `git log --format=%B`
artifact); `git log --format=%B -1 | LC_ALL=C grep -n '[^ -~]'` empty
(ASCII clean) on both. CI (`gh run list --branch main`): commit 1
(`b711aa6`) `completed`/`success`, 3m37s; commit 2 (`ab11d7b`)
confirmed below in Deviations/close-out (checked again before this
record's own push, since it was still `in_progress` at first check).

### Deviations

**The RED-mismatch disclosed above (commit 1, Step 2)** is this
session's only real deviation from the driving prompt's own stated
expectation — surfaced via STOP-AND-REPORT, resolved by the user's own
explicit choice ("proceed as a disclosed gap") rather than guessed at.
No other "current (verify)" claim in the driving prompt failed
verification; no other red refused to go red; no regen delta landed
outside B1/B2's own predicted reach (`docs/cli.md`: zero delta, both
times, disclosed above); no oracle non-identity.

### Fences

Touched: `bases/cli/src/ehrt/cli/help.clj`, `core.clj` (help path
only); `bases/cli/test/ehrt/cli/help_test.clj`, `core_test.clj`;
`components/docs-tooling/test/ehrt/docs_tooling/invocation_lint_test.clj`
(scan-root config + its own test); the drift-fix files the commit-1
census named (`demos/traces/order-result/config.edn`,
`demos/traces/module-mix/config.edn`, `demos/traces/module-mix/README.md`,
`.github/ISSUE_TEMPLATE/bug-report.md`); `docs/cli.md` (regenerated,
confirmed zero delta); `.agents/plans/roadmap.md`;
`.agents/plans/2026-08-12-review-3-user-surface-findings.md` (four
disposition-cell notes plus one addendum, fix-forward, summary table
untouched per the ADR-0115 snapshot-table precedent);
`.agents/rulings.md`; `notes/ADRs.md`; `notes/adr/README.md`;
`notes/adr/0118-*.md` (this file); `.agents/prompts/*`;
`.agents/session-records/*`. ZERO engine/sim/judge/check component
`src` touched anywhere. ZERO behavior change on any valid input outside
the help surface (`--help`, `help <group>`, `help <group> <verb>`, and
the rendered text those paths return).

### Index line

```
- 2026-08-12 — fix-clusters-b-and-c-help-and-docs — ADR-0118
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Fix clusters B and C: help enrichment, doc drift, scan roots — lands review-3's two remaining fix clusters (ADR-0115), two commits, red-before-green: cluster C first — the invocation lint's own scan roots widen to `demos/**` and `.github/**` (R3-B5-4's "consider" ruled YES); the widening only goes RED on R3-B5-4's own issue-template `clojure -M:cli` alias, since R3-B5-3's own `demos/traces/**` stale config-header drift lives in unfenced EDN comments the lint's two checks structurally cannot see — disclosed as a premise mismatch (STOP-AND-REPORT), resolved by the author's own choice to proceed rather than silently extend the lint's content patterns; fixed instead by an extension-blind census grep, finding the 3 named instances plus 1 more the register never listed (`demos/traces/module-mix/README.md`'s own stale `docs/demos/emit-state/` prose reference); then cluster B — genuine verb-level help narrowing for both `<group> <verb> --help` and the 3-arg `help <group> <verb>` form (`help/render-verb-help`), a known group's unknown verb reusing F6's own `:unknown-command` treatment verbatim (R3-B3-2), and one sourced, verbatim "Example:" line per group with a witnessed invocation anywhere in README.md's Quickstart, `docs/use-cases/*.md`, or a demo README — 7 of 9 groups covered, `version`/`doctor` render none (no witnessed invocation exists for either, recorded as a register addendum) rather than an invented one (R3-B3-1); a real regression an existing gate caught mid-session (the unwrapped Example line broke `help_wrap_test.clj`'s own width-fit property at non-default widths) fixed by routing it through the same wrap function every other field uses; `docs/cli.md` regenerated, confirmed byte-identical both times (it deliberately excludes worked invocations by design); zero engine/sim/judge/check `src` touched, the oracle holds pure identity across all 35 roots
