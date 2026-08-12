# 2026-08-12 — ehr-testing-tools: fix clusters B + C, help enrichment, doc drift, scan roots (build session)

## Context

Archived 2026-08-12. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `c68ec3e` (ADR-0117 close) and closed at
`ab11d7b` (commit 2) plus this record's own commit. Original prompt
follows verbatim; a deviation record follows that.

## Original prompt (verbatim)

Session prompt — fix clusters B + C: help enrichment, doc drift, scan roots (ADR-0118)

You are Claude Code executing under R30 ceremony for ehr-testing-tools (github.com/pragsmike/ehr-testing-tools), working for mg, the sole author. One session lands review-3's two remaining fix clusters (chartered ADR-0115): C (doc drift + lint scan roots, commit 1) and B (help-surface enrichment, commit 2). Red-before-green throughout; kernel result/error for any new error (R10); exit 2 for user errors; no ADR tokens in user-facing prose. STOP-AND-REPORT on any conflict with the tree. Standing notes: full `make test` before EVERY push; gate-forced companions in-fence by rule (named in the record); budget trip → STOP-AND-REPORT.
Read first

1. `.agents/plans/2026-08-12-review-3-user-surface-findings.md` — rows R3-B5-3, R3-B5-4, R3-B3-2, R3-B3-1 (evidence + recs; recs are the specs below).
2. The invocation lint: `components/docs-tooling` (its scan-root configuration and test).
3. `bases/cli/src/ehrt/cli/help.clj` + `core.clj` — the help render path and F6's unknown-group handling (ADR-0117), which B's unknown-verb case mirrors.
4. `.agents/rulings.md` tail; `.agents/plans/roadmap.md` (cluster B and C rows).

Step 0 — Preflight and tag ceremony

* `git fetch`; confirm `origin/main` at `c68ec3e`. Else STOP.
* Confirm CI green (`gh run list --limit 5`) — completes the one channel-unverified leg of ADR-0117 (API rate-limited; session watched all five runs green with matching SHAs).
* Tag `stable-20260812-fix-cluster-a`, ANNOTATED, at `c68ec3e`; push; peeled ref exact. License: case (i) — channel fresh-clone verification 2026-08-12 (lineage, ASCII x5, footprint, F1/F2 boundary diffs read directly, independent F7 sweep census: zero live survivors), CI per this preflight.

Commit 1 — Cluster C (docs drift + scan roots)
Order matters — the widening IS the red:

1. Widen the invocation lint's scan roots to include `demos/**` and `.github/**` (R3-B5-4's "consider" is ruled YES [C, un-vetoed]: same recurrence-prevention logic as demos).
2. Run the lint: it must go RED on the known drift (R3-B5-3's `demos/traces/**` stale config headers; R3-B5-4's issue-template stale alias). Capture the red. If it does NOT go red on them, STOP-AND-REPORT — either the drift is already gone or the widening missed.
3. Fix the drift: census by extension-blind, un-truncated grep over the newly-scanned roots (the register's named instances are illustration; the rule is "every stale alias/reference the widened lint reports plus any the grep finds that the lint's patterns miss — report the latter, don't silently extend the lint"). Green.

Message: `docs: fix demo-trace and issue-template drift; widen lint scan roots (ADR-0118)`
Commit 2 — Cluster B (help enrichment)
B1 (R3-B3-2) — verb-level narrowing. New: `ehrt help <group> <verb>` renders that verb's own flags + description only (not the whole group screen); a known group with an unknown verb → the F6 unknown-group treatment, verbatim category reuse, naming valid verbs, exit 2. Red first: a test asserting the narrowed render and the unknown-verb error, failing on the current tree.
B2 (R3-B3-1, mechanism + sourced content). Each group's help screen gains one `Example:` line. Content rule [C — approved by dispatch of this prompt]: each line is an invocation copied VERBATIM from an existing witnessed source (a `docs/use-cases/*.md` strip, README Quickstart, or a demo README), one per group, source cited per line in the ADR. No composed invocations — if a group has no witnessed invocation anywhere, render no Example for it and record that gap as a register addendum row rather than inventing one. The invocation lint (now wider) co-verifies every line. Red first: a test asserting Example presence per covered group.
Regen `docs/cli.md`; deltas confined to the help surfaces B1/B2 touch.
Message: `feat: verb-level help narrowing and sourced per-group examples (ADR-0118)`
Commit 3 — Registers, ADR, close

* Register: the four rows gain dated `FIXED, ADR-0118` notes (summary snapshot untouched); any B2 no-witnessed-invocation gaps added as addendum rows.
* Roadmap: cluster B and C rows → RESOLVED; review-3 arc note — CLOSED except the design-channel-draft queue (B-3/B-4 carry-forward wording halves, the channel's own work, unchanged); the User manual design pass row flips to READY — awaiting the design channel's framing (the review it waited on is complete).
* `.agents/rulings.md` "From ADR-0118": the scan-root YES and the B2 sourcing rule, both [C, un-vetoed/approved-by-dispatch], strikeable at a glance.
* Self-archive at close-phase START; ADR-0118 (reds, greens, the C census, B2's per-line source citations); indices (115 → 116); Done line; session record.

Oracle bracket. Pre-analysis: pure identity on all 35 roots — help text, docs, lint config; no root invokes help or reads the drifted files. `bin/regression-oracle c68ec3e <final>`; non-identity → STOP.
Gates: standing; ASCII x3; gitleaks; CI confirm or disclose.
Fences
Touch ONLY: `bases/cli/src/ehrt/cli/help.clj`, `core.clj` (help path only — zero validation-logic change beyond B1's unknown-verb case); `bases/cli/test/ehrt/cli/` tests; the lint's scan-root config + test in `components/docs-tooling`; the drift-fix files the C census names (each named in the record); regenerated `docs/cli.md`; registers, prompts, session-records, `notes/adr/0118-*.md`, `notes/ADRs.md`, `notes/adr/README.md`; gate-forced companions by rule. ZERO engine/sim/judge src; ZERO behavior change on any valid input outside the help surface. Outside the list → STOP-AND-REPORT (the ADR-0116 widen-by-ruling precedent).
STOP-AND-REPORT on: the widened lint not going red on the known drift; any "current behavior" claim failing verification; a red that won't go red; regen deltas outside B's reach; oracle non-identity; anything not pre-decided.

## Deviation record

**Commit 1, Step 2 — the widened lint's own RED did not cover both
named drift classes.** The driving prompt's own step 2 named an
explicit STOP-AND-REPORT trigger: "it must go RED on the known drift
(R3-B5-3's ... stale config headers; R3-B5-4's ... stale alias) ... If
it does NOT go red on them, STOP-AND-REPORT." Running the widened lint
produced exactly one failure (R3-B5-4's issue-template alias) — R3-B5-3's
own `demos/traces/**` stale config-header drift tripped nothing.
Confirmed structurally: the lint has exactly two checks (a literal
`clojure -M:cli` substring match, and fenced-\`\`\`bash/sh
`--config`/`--profile`/`--path` value resolution), and R3-B5-3's own
drift lives in plain `;;` EDN comments with no code fence at all —
structurally invisible to either check regardless of scan-root
widening. Confirmed the drift itself was still live (not "already
gone," the trigger's other disjunct) by direct grep, and found one
instance the register itself never named
(`demos/traces/module-mix/README.md:108`'s own stale
`docs/demos/emit-state/` prose reference).

Raised via `AskUserQuestion` rather than guessed at, since the driving
prompt's own step 3 wording ("plus any the grep finds that the lint's
patterns miss — report the latter, don't silently extend the lint")
suggested this split was anticipated but the step 2 language read as
an unconditional stop trigger — genuinely ambiguous, and this session's
own reading of it should not be the deciding one. The user chose
"Proceed as disclosed gap" over stopping the session entirely or
extending the lint's own content patterns to also catch unfenced-EDN-
comment drift (which would have gone beyond "widen scan roots only"
and the fence's own "don't silently extend the lint" instruction).
Session continued on that basis; the full disclosure — evidence, the
structural cause, and the resolution — is recorded in `notes/adr/
0118-fix-clusters-b-and-c-help-and-docs.md`'s own Decision and
Deviations sections, not just here.

No other deviation this session. Every other "current (verify)" claim
in the driving prompt held exactly as stated before its own fix
landed; every red went red as expected once the false-positive test
bug (help_test.clj's own bare `"ehrt sim check"` substring check,
tripped by a legitimate cross-reference in `--format`'s own doc
string) was fixed; the `help_wrap_test.clj` regression the first
Example-line implementation caused was caught by an existing gate, not
a new one, and fixed before commit; no regen delta landed outside
B1/B2's own reach (`docs/cli.md`: zero delta, confirmed twice); no
oracle non-identity.
