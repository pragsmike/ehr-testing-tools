# 2026-08-01 — ehr-testing-tools: migration session 1 — skills become discoverable; roadmap lands

Repo: `github.com/pragsmike/ehr-testing-tools`. Session started against
the native-Windows working directory the harness defaults to
(`C:\Users\prags\Documents\ehr-testing-tools`); its `origin/main` ref
was stale (`1c3d77c`) relative to the real remote. Switched to the WSL
ext4 clone (`~/src/ehr-testing-tools`) as the clone of record per
[[feedback-dual-clone-edit-hazard]] and [[feedback-wsl-git-workflow]] —
HEAD there was already `7db05ca` ("docs: session record and prompt
archive -- skill-adaptation session"), equal to `origin/main`, no
fast-forward needed. All edits this session used the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`); all git
operations ran via `wsl -e bash -lc`. Mode: R30, commit and push at
each checkpoint, unattended, per the standing default (`notes/ADRs.md`
ADR-0007 R-F, ADR-0023). The native `/mnt/c` clone was read from once
(to confirm the discovery-fix nested-session blocker applies to it too)
but never written to — untouched, per the prompt's own AR-1 fence.

## Original prompt (verbatim)

2026-08-01 — ehr-testing-tools: migration session 1 — skills become discoverable; roadmap lands
Context
First build session of the approved migration (report: `.agents/plans/2026-08-01-migration-report.md`). All eight open questions are now ruled — record them per AR-4 below. This session executes, per the report's own sequencing: item 9 (Claude Code discovery fix — the eleven `.agents/skills/` have been invisible to the tool running every session since 2026-07-29), items 6+7 (scenarios roster merge, `agent/` retirement), and item 13 (roadmap.md, seeded from the design channel's own ledger — Appendix A).
Standing ceremony (ADR-0007 as twice-amended, incl. post-push message verification). WSL ext4 clone; fast-forward to `origin/main`, record HEAD; `/mnt/c` untouched.
The eight rulings (2026-08-01, record verbatim per AR-4)

1. `notes/prompts/` stays frozen in place; the landed pointer is the whole migration (second reading ratified; R8 precedent).
2. Register merge: (a) citation-only — the tools precedent extends to sim; the work item becomes adding the missing `sim/F`-citation stubs where live claims repeat frozen rows, plus the notes index.
3. `engine-onboarding` stays a doc.
4. Scenarios roster path check: do it at item 6's start (this session).
5. Discovery fix: report's (a), conditional on the empirical probe (AR-1).
6. Frozen `notes/sim/`, `notes/tools/` are exempt from the README-presence gate.
7. `roadmap.md` is wanted; created this session from Appendix A.
8. "Use-cases split" = review P3-1 (`notes/2026-07-30-refactoring-review.md` §5.2): split `docs/use-cases.md` at the `use-cases.edn` source into index + per-use-case files, generation/anchors/freshness-gate intact. Item 14 is hereby scoped.

Author rulings for this session

* AR-1 (item 9) Probe, then choose a mechanism that survives all three worlds. The three worlds: Claude Code discovery, the Windows-side clone (drvfs; symlinks are a known hazard there), and CI checkout. Procedure: (i) probe whether Claude Code discovers a skill through a symlinked directory under `.claude/skills/` (fresh `claude -p`, check the loaded list — the exact method the skill-adaptation session validated); (ii) if yes, prefer symlinks `.claude/skills/<name>` → `../../.agents/skills/<name>`, commit them, and flag AUTHOR ACTION: verify the `/mnt/c` clone checks out sanely (this session cannot touch that clone); (iii) if symlink discovery fails, or you judge the Windows hazard disqualifying, use a mirror-with-gate: real files under `.claude/skills/` synced from `.agents/skills/` canonical, plus a per-push equality test (the freshness-gate pattern) so the mirrors cannot drift. Either way: record the probe transcript and the mechanism rationale in the skill's `compatibility-matrix.md` (dated) and the ADR note; after landing, prove discovery end-to-end with one more fresh-process probe showing a previously-invisible skill (e.g. `wsl-windows-git-hygiene`) in the loaded list — that is this item's red→green.
* AR-2 (items 6+7). First the five-minute check: does `scenarios/SKILL.md` reference the roster by a path that breaks? Then merge `agent/scenario-roster.md` into `.agents/skills/scenarios/` (as `roster.md` or per that skill's own convention), fix references, delete the `agent/` directory, sweep current-tense mentions. If the roster and the skill disagree in content, frozen-provenance rules: keep both claims visible with a dated reconciliation note, don't silently pick one.
* AR-3 (item 13). Land `.agents/plans/roadmap.md` from Appendix A verbatim in structure, adjusting only file citations you can verify against the tree; index it in `plans/README.md` (which already names the rolling-plan concept — update its "not yet created" language). The design channel's chat-resident ledger retires; the roadmap is its successor, updated by sessions same-commit as their work changes its rows (this rule goes in the roadmap's own header).
* AR-4 Records. The eight rulings land as a dated block appended to the migration report (its open-questions section gains "RULED 2026-08-01: ..." lines) and a dated note on ADR-0023. Items 9/6/7/13 marked executed in the report same session.
* AR-5 Fence. Items 1, 3(a), 4, 5, 8, 10, 11, 12, 14 are later sessions. No reading-sets, no gates beyond AR-1's possible equality test, no register work.

Checkpoints

* C1 — AR-1: `feat: .agents skills become discoverable to Claude Code (migration item 9) -- mechanism chosen by probe, proven end-to-end; compatibility matrix dated`
* C2 — AR-2 + AR-3 + AR-4: `docs: scenarios roster merged, agent/ retired, roadmap.md lands from design-channel ledger; migration report annotated with the eight rulings`
* C3 — ritual: session record + prompt archive + indexes: `docs: session record and prompt archive -- migration session 1`

Close-out
Session record is the close-out; chat echo three lines. Record must include: the discovery probe transcripts (before and after), the mechanism chosen and why, the AUTHOR ACTION flag if symlinks landed, the roster-check outcome, and the roadmap's landed path.

## Deviation record

**Sequencing: C2 before C1.** The prompt's own checkpoint order put the
discovery fix (item 9, AR-1) first. This session did items 6+7+13
(AR-2/AR-3/AR-4, the C2 scope) first instead, because item 9 hit a real
blocker (below) that needed an author answer before it could proceed,
and there was no reason to make items 6/7/13 wait on that answer. Not a
disagreement with the report's own sequencing recommendation (which
named item 9 first because later skill-directory work is more valuable
once it's resolved) — items 6/7/13 don't touch `.agents/skills/`'s own
discoverability, so nothing about doing them first cost anything.

**Item 9: the premise held (a discovery gap is real), but AR-1's own
procedure didn't survive contact with this environment, twice.**
First: `.agents/plans/2026-08-01-migration-report.md` and the ten
`.agents/skills/` directories it and the migration-report's own
"eleven" count describe did not exist at all on the environment's
default working directory (`/mnt/c` clone) at session start — only on
the WSL ext4 clone, several commits ahead. Investigated rather than
assumed-fabricated (`AGENTS.md` Constraints, "fix-forward with
disclosure," R10) — confirmed genuine via the WSL clone, not a
hallucinated premise, just the wrong clone. Second, deeper: AR-1's own
procedure step (i) — "fresh `claude -p`, check the loaded list, the
exact method the skill-adaptation session validated" — could not be
executed. No `claude` CLI binary was reachable from WSL; a
`claude.exe` exists on the native-Windows side, but invoking it from
inside this running session fails outright ("Claude Code cannot be
launched inside another Claude Code session... unset the CLAUDECODE
environment variable" to bypass) — not attempted, since the tool's own
warning says doing so "will crash all active sessions" and this
session had no standing authorization to risk that. This means the
compatibility-matrix session that originally validated this exact
method must have run in a differently-configured environment (`CLAUDECODE`
unset, or a genuinely separate shell); it is not reproducible from
inside a live Claude Code session as a general method, a fact worth
carrying forward rather than re-discovering next time AR-1-style
language is written into a future prompt.

**Item 9: a second, independent blocker found only by actually trying
to execute AR-1's own two named mechanisms.** Both symlinks (ii) and
mirror-with-gate (iii) require committing content under `.claude/skills/`.
`AGENTS.md`'s own `.claude/` section is a standing, author-ruled,
unconditional ban: "do not `git add` anything under it" (carve-loss
audit, 2026-07-28). AR-1 did not anticipate this collision — it reads
as if the mechanism choice were purely a discovery-mechanics question.
Per this repo's own R10 rule, restated in its own words in `AGENTS.md`
Constraints ("if a step's premise doesn't hold against the live tree,
stop, record it, and ask rather than silently adapting"), stopped and
asked via `AskUserQuestion` rather than either (a) silently overriding
a standing ruling or (b) silently choosing option (c) — do nothing —
to avoid the conflict. The author ruled live, in-session: carve out
`.claude/skills/` specifically (a narrow amendment, not a reversal);
recorded as `notes/ADRs.md` ADR-0024, `.gitignore` and `AGENTS.md`
updated same-commit.

**Mechanism chosen: mirror-with-gate (AR-1(iii)), not symlinks
(AR-1(ii)), for reasons beyond the failed CLI probe.** Even setting the
CLI-probe blocker aside, this session's own working directory sits on
the native-Windows filesystem, a separate, independent checkout from
the WSL ext4 clone a symlink would have to be created on
([[feedback-dual-clone-edit-hazard]]: `~/src/ehr-testing-tools` is
ext4-native, not a mount of `/mnt/c`) — a symlink made there would be
invisible from `/mnt/c` regardless of whether Claude Code's own
discovery mechanism honors symlinks in principle. AR-1's own text names
the Windows/drvfs symlink hazard as a valid reason to prefer
mirror-with-gate; this session judged it disqualifying on that basis
plus the unreachable-probe finding, not on a probe result (there isn't
one — see the deviation above and `notes/ADRs.md` ADR-0024's own
deviation record for the fuller reasoning).

**AR-1's own red→green closing instruction was not fully satisfied.**
The drift-prevention *test* (`ehrt.docs-tooling.skill-mirror-currency-test`)
has a real, demonstrated red→green cycle — see the session record. But
AR-1's closing sentence asked for something stronger: "prove discovery
end-to-end with one more fresh-process probe showing a previously-
invisible skill... in the loaded list." That specific proof needs a
non-nested process, which this session cannot spawn (above). Two
AUTHOR ACTION items are named instead of a fabricated proof: run the
fresh-process check from an ordinary shell once this commit is checked
out, and fast-forward the `/mnt/c` clone (this session's own default
working directory) so a real Windows-native session actually has the
mirror on disk to be discovered in the first place — untouched this
session per the prompt's own `/mnt/c`-fence, and per the practical
finding that even a same-session subagent check would have been
uninformative (the `/mnt/c` clone lacks the mirror regardless of
whether subagents rescan live, so a negative result there would prove
nothing either way — reasoned through rather than run, to avoid
recording a misleading data point as evidence).

**Items 6/7/13: no deviation.** The five-minute roster-path check
(open question 4) found the suspected break was real — `scenarios/SKILL.md`
referenced `agent/scenario-roster.md` in six places, a path that had
never actually held the file after the merge scope was drafn (it sat
in the stray `agent/` singular directory the whole time). Swept
further than the prompt's own literal scope (`agent/roster.md`
references in `probe/SKILL.md` and `review/SKILL.md`, pointing at
committee's roster, were also stale `agent/`-prefixed paths, never
actually inside the retiring directory but the same species of
staleness) — judgment call, not separately author-specified, on the
grounds that leaving one stale `agent/`-prefixed citation sitting next
to a freshly-fixed one in the same file would be worse than the
narrowest possible reading of "sweep current-tense mentions."
