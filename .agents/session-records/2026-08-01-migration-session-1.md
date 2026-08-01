# 2026-08-01 — Migration session 1: skills become discoverable, roster merged, roadmap lands

## Scope

First build session of the approved migration
(`.agents/plans/2026-08-01-migration-report.md`), executing three of
its fourteen items per the report's own sequencing and the author's
eight rulings (recorded verbatim in the report's own "RULED 2026-08-01"
section and in this session's archived prompt): **item 9** (Claude Code
couldn't discover any of `.agents/skills/`'s 10 skill directories —
confirmed again this session, then fixed), **items 6+7** (`agent/scenario-roster.md`
merged into `.agents/skills/scenarios/roster.md`, the stray `agent/`
directory retired), and **item 13** (`.agents/plans/roadmap.md` lands
from the design channel's ledger handover). Ran items 6+7+13 first
(commit `47c815c`), then item 9 (commits `a9e5be6`, `8df3cf3`) — see
the prompt archive's deviation record for why the order flipped from
the prompt's own checkpoint numbering. Item 9 surfaced a real blocker
mid-session (a standing `AGENTS.md` ban on tracking anything under
`.claude/`, which every mechanism AR-1 named would have violated); per
this repo's own fix-forward-with-disclosure rule, stopped and asked via
`AskUserQuestion` rather than guessing — the author ruled live, landed
as `notes/ADRs.md` ADR-0024. Items 1, 3(a), 4, 5, 8, 10, 11, 12, 14
stayed fenced, untouched, per the prompt's own AR-5.

## Discovery probe: before and after

**Before (this session's own reproduction, not just cited from the
prior compatibility-matrix pass):** `.agents/skills/` held 10
directories; `.claude/skills/` did not exist at all (only
`.claude/settings.local.json`, itself untracked). No skill from that
list appeared in this session's own Skill-tool listing under its own
name — consistent with the compatibility-matrix's own prior finding
(`.agents/skills/repo-adaptation/references/compatibility-matrix.md`
"Verified findings (2026-08-01)"), not independently re-tested by a
fresh process this session (see below for why).

**Attempted, blocked:** AR-1's own procedure step (i) — a fresh
`claude -p` invocation, checking the loaded skill list — was attempted
literally, from a plain shell, targeting the same repo directory:

```
$ claude -p "List the exact names of every Skill you have access to..."
Error: Claude Code cannot be launched inside another Claude Code session.
Nested sessions share runtime resources and will crash all active sessions.
To bypass this check, unset the CLAUDECODE environment variable.
```

Not bypassed (the tool's own warning names a real risk to the running
session, and bypassing it was never authorized). This means the exact
method AR-1 names as "the exact method the skill-adaptation session
validated" cannot be run as a general-purpose check from inside a live
Claude Code session — worth remembering the next time a prompt assumes
it's freely repeatable.

**After (mechanism landed, proof partial):** `.claude/skills/<name>/`
now holds a real, byte-identical (content + executable-bit) copy of all
10 `.agents/skills/` directories (33 files), gated by
`ehrt.docs-tooling.skill-mirror-currency-test`. The mirror's own
correctness is proven (see Red→green below); whether a *fresh* Claude
Code process actually lists these skills now is not proven by this
session — the same `CLAUDECODE` restriction blocks a self-administered
after-check the same way it blocked the before-check. Two AUTHOR ACTION
items stand in for that proof (see Findings below).

## Red→green evidence highlights

`ehrt.docs-tooling.skill-mirror-currency-test` (new this session, run
directly via `clojure -M:test -e ...`, matching this repo's own
practice of running specific namespaces rather than the full `poly
test :project` suite mid-session): **2 tests, 132 assertions, 0
failures** at close. Two independent red→green cycles, both real bugs
this session found by trying, not by inspection:

- **Content drift.** Deleted `.claude/skills/probe/SKILL.md` after the
  initial `cp -r` mirror; re-ran — failed citing the exact missing
  path and a "re-sync from .agents/skills/" message; restored; green
  again (99 assertions at that point, before the exec-bit check
  existed).
- **Executable-bit drift, found live, not anticipated.** `cp -r`
  copied all four script files' bytes correctly (same git blob sha
  confirmed via `git ls-files -s` on both trees) but silently dropped
  their `100755` mode to `100644` — a real bug in the mirror the
  content-only test would never have caught. Strengthened the test to
  assert `.canExecute()` parity too (not just `slurp` equality);
  verified red by stripping one script's `+x` (failed citing the exact
  file and the wrong-mode message), verified green after restoring all
  four via `git update-index --chmod=+x`. Landed as its own commit
  (`8df3cf3`), separate from the mirror's own initial commit
  (`a9e5be6`), rather than amended into it — the bug was found and
  fixed before push, but as a distinct, self-caught correction worth
  its own message, not silently folded away.

`clojure -M:poly check`: green (`OK`) before both C1 commits and before
C2.

## Judgment calls and their ratification status

- **Mechanism: mirror-with-gate, not symlinks.** Not resolved by AR-1's
  own conditional probe (which never ran, per the CLI-nesting
  blocker) — resolved instead on the surrounding evidence: the
  Windows/WSL dual-clone split means a symlink created on the WSL
  filesystem is invisible to this session's own native-Windows working
  directory regardless of Claude Code's own symlink handling, and AR-1
  itself names that hazard as a valid reason to prefer mirror-with-gate.
  Recorded as a deviation in `notes/ADRs.md` ADR-0024's own deviation
  record, not silently treated as "the probe said so." **Ratified**:
  the author's own answer to the `.claude/`-tracking `AskUserQuestion`
  ("carve out `.claude/skills/` as trackable... mirror-with-gate")
  covers this choice directly, live, in-session.
- **`.claude/` carve-out scope.** Narrowed to `.claude/skills/`
  specifically (not all of `.claude/`), matching the author's own
  chosen `AskUserQuestion` option verbatim. **Ratified**, live.
- **Swept `agent/roster.md` references beyond the prompt's literal
  scope** (`probe/SKILL.md`, `review/SKILL.md` — citing committee's
  roster by a stale `agent/`-prefixed path that was never actually
  inside the retiring `agent/` directory). Judgment call, not
  separately author-specified — reasoning in the prompt archive's
  deviation record. **Not yet ratified** — flagging here for review,
  low-risk (a doc-citation fix, reversible, no behavior change).
- **Roadmap.md's file citations**, adjusted from Appendix A only where
  verified against the tree (all checked; none needed changing — the
  seed's own citations were already accurate).
- **Sequencing: items 6+7+13 before item 9.** Not a disagreement with
  the report's own recommendation, just an ordering choice once item 9
  hit a blocker needing an author answer — detailed in the prompt
  archive's deviation record. **Not separately ratified**, low-risk
  (checkpoint ordering, not scope).

## Findings and HEAD landed

**AUTHOR ACTION (named in `notes/ADRs.md` ADR-0024, restated here per
this record's own required contents):**

1. Run `claude -p` (or an ordinary interactive session) from a shell
   that is not itself inside a running Claude Code process, in a clone
   that has this session's commits checked out, and confirm
   `wsl-windows-git-hygiene` (or any of the other 9) appears in that
   session's own Skill listing — the red→green proof this session could
   not self-administer.
2. Fast-forward the native `/mnt/c` clone
   (`C:\Users\prags\Documents\ehr-testing-tools`) to this session's
   HEAD. Real Windows-native Claude Code sessions run with that clone
   as their own working directory (confirmed: this very session's own
   cwd) — the mirror does nothing for them until that clone has
   `.claude/skills/` on disk. This session did not touch that clone,
   per the prompt's own fence and [[feedback-dual-clone-edit-hazard]].
   That clone is also several commits behind independent of this
   session's own work (last synced at `1c3d77c`) — the fast-forward
   should pick up everything back to `7db05ca` too, not just this
   session's commits.

**Roster-check outcome (open question 4, checked at item 6's start, as
instructed):** the suspected break was real. `.agents/skills/scenarios/SKILL.md`
referenced `agent/scenario-roster.md` by that literal relative path in
six places; the file had never actually been merged to a path under
`.agents/skills/scenarios/` before this session — the reference was
broken the entire time the `scenarios` skill existed in its current
form, just never exercised (matches the migration report's own
"Urgent items: None... unverified, and if real, has apparently never
been hit either").

**Roadmap's landed path:** `.agents/plans/roadmap.md`, indexed in
`.agents/plans/README.md`.

**`.agents/skills/` count correction.** The migration report and this
session's own prompt both say "eleven" `.agents/skills/` directories;
the actual count, confirmed by directory listing both before and after
this session's edits, is **10** (`committee`, `find-skills`, `handoff`,
`probe`, `repo-adaptation`, `review`, `scenarios`, `shared-skill-layout`,
`string-diagram`, `wsl-windows-git-hygiene`). Not corrected retroactively
in the report or prompt archive (both are dated snapshots); noted here
so a future reader doesn't inherit the off-by-one.

**HEAD landed:** `8df3cf3` ("fix: skill mirror lost its executable bit
on four scripts, self-caught"), pushed. Full checkpoint shas: C2
(items 6+7+13+AR-4) `47c815c`; C1 (item 9) `a9e5be6` then `8df3cf3`
(the self-caught exec-bit fix, its own commit per this workspace's own
never-amend-a-pushed-commit convention). This record and the prompt
archive are C3, landing on the commit produced by this same checkpoint
— cited by this record's own filename per the same self-reference
convention `2026-08-01-agent-ux-capture.md` and
`2026-07-29-storefront-polish.md` both already used.
