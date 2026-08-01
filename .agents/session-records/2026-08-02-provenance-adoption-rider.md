# 2026-08-02 — Provenance-adoption rider session

## Scope

A rider session enacting the author's ruling on the finding migration
session 5 escalated: `notes/ADRs.md` ADR-0007's `[A]`/`[C]`
ruling-provenance-tag convention had gone unused since its own
adoption. Ruled `[A]` **Adopt** — active law from the item-14 prompt
onward (`.agents/prompts/2026-08-02-migration-session-6.md`, which ran
tagged as the demonstration); the five untagged prompts before it stay
untagged, not retroactively fixed. One checkpoint (**C1**): a dated
ADR-0007 amendment recording the ruling and, for the first time, its
escalation semantics (a `[C]` ruling conflicting with the live tree is
a default to fix-forward and reconcile; an `[A]` ruling conflicting
with the live tree always escalates, citing the refactoring review's
own R-1 vocabulary reconciliation as the motivating precedent);
`.agents/skills/session-prompt/SKILL.md` (mirrored to
`.claude/skills/`) gains tagging as a *required* step of its
Author-rulings anatomy, not an optional annotation; `docs/dev/way-of-working.md`'s
own open divergence note resolved with the ruling and its date. No
`.agents/plans/roadmap.md` row — the finding never had one, so none was
manufactured, per the ruling's own fence.

## Red→green evidence highlights

Docs-only change (skill prose and two reasoning-of-record files); no
behavior to prove red→green. Full `docs-tooling` brick rerun before and
after staging, both clean: `clojure -M:poly check` `OK`; 27 test
namespaces in the brick, 0 failures/errors both times, including
`ehrt.docs-tooling.skill-mirror-currency-test` (216 assertions —
confirms `.agents/skills/session-prompt/SKILL.md` and
`.claude/skills/session-prompt/SKILL.md` stayed byte-identical after
the edit) and `ehrt.docs-tooling.index-completeness-test` (43
assertions — confirms this record and its paired prompt archive are
indexed, both directions).

## Judgment calls and their ratification status

- **Dual-clone edit routing.** Per this workspace's own persisted
  agent-memory note on the dual-clone edit hazard, the four edits were
  made via the native-Windows path (`C:\Users\prags\Documents\ehr-testing-tools`,
  the tools' own default) and then copied byte-for-byte onto the WSL
  ext4 clone (`~/src/ehr-testing-tools`) — confirmed identical
  (`diff`) — before any git operation, rather than editing the ext4
  clone directly via its UNC path. Functionally equivalent end state
  (both clones identical, git/build ran against the ext4 clone of
  record); not separately author-ratified, but consistent with the
  memory's own stated goal (edits and git/build target the same
  clone), not its literal mechanism.
- **Fence held as ruled.** No stray-directory cleanup attempted
  (author-only per the ruling's own fence); no budget change beyond
  what the skill edit itself required (none — `session-prompt` is not
  a `:budget-lines`-tracked path in any `.agents/reading-sets.edn`
  set).

## Findings and HEAD landed

**Expected pre-existing red not observed — premise did not hold, tree
was already clean.** The driving prompt named a standing pre-existing
failure (`ehrt.docs-tooling.structure-currency-test` tripping on an
untracked, empty `bases/sim-cli/resources/sim-cli/` directory, per
migration session 6's own finding) to proceed past. That directory no
longer exists on either clone at this session's start
(`bases/sim-cli/` contains only `resources/`, itself empty) — the
`structure-currency-test` ran clean, 33 assertions, 0 failures, on both
the pre-edit and post-edit runs. The stray directory was evidently
removed by the author between migration session 6's close and this
session's start, outside any recorded session (matching the pattern of
that same record's own "stale prior memory, corrected" finding). No
fix-forward action needed; disclosed per this skill's premise-mismatch
step rather than silently assumed away.

**Budget delta, caught by the gate itself (same pattern as migration
sessions 5 and 6's own C3).** Adding this record's own filename to
`.agents/session-records/README.md` and `.agents/prompts/README.md` (both
already-cited `:onboarding` paths) grew that set's real line count from
715 to 717 — `reading-set-budget-test` failed red on the first run of
this checkpoint before that growth was reflected. Fixed forward in this
same commit: `.agents/reading-sets.edn`'s `:onboarding` `:budget-lines`
raised 715 → 717 with a dated comment, not amended into C1. Rerun after
the fix: 0 failures.

**HEAD landed:** C1 `251e2a1`, pushed and post-push-verified
(`git log --format=%B -1` against the commit matched the message file
exactly, modulo the trailing-blank-line artifact `git log --format=%B`
always adds). This record's own commit (C2) lands next, pushed
immediately after, per R30. HEAD at session start: `a80e73a` (both
clones already matched `origin/main`, confirmed by `git fetch` before
any edit).
