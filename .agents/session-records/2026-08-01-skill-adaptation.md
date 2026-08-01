# 2026-08-01 — Skill adaptation: repo-adaptation reconciled, discovery verified empirically, migration report produced

## Scope

Charter §7 items 2–3 (`.agents/plans/2026-08-01-agent-ux-charter.md`),
run as one session, assessment-only for the migration itself. Three
checkpoints, each closed by the full ceremony (scope check, gitleaks
scan, commit via message file, push, plus this session's own new
post-push message-verification step): **C1** — reconciled
`.agents/skills/repo-adaptation/` against `pragsmike/skills` and
`pragsmike/cyberneutics`, folded in one fix, wrote dated empirical
discovery findings into the skill's own `references/`, and landed a
second dated amendment on `notes/ADRs.md` ADR-0007 (the ceremony rider
this session's own prompt dispatched: verify every pushed commit
message against its source file). **C2** — ran the adapted skill's own
assessment mode (Steps 1–3) against this repo and produced
`.agents/plans/2026-08-01-migration-report.md`. **C3** (this record) —
session record and prompt archive, indexes updated same-commit.

## Red→green evidence highlights

Docs-only session; no Clojure source changed, so the proof is the
existing suite staying untouched, not a red→green cycle. `gitleaks
protect --staged` ran clean before both C1 and C2 (0 leaks each). Ran
`.agents/skills/repo-adaptation/scripts/inspect-repo.sh .` directly
(Step 1 of the skill's own procedure) rather than assuming its output —
it found 4 agent-facing items (`AGENTS.md`, `CLAUDE.md`, `.agents/`,
`.claude/`) and correctly found nothing for every tool this repo has
never touched (Cursor, Copilot, Windsurf, Aider, OpenCode, Codex-alt
`codex.md`) — a real run, not a template fill-in.

## Judgment calls and their ratification status

- **AR-1's three-way diff was two-way for content.** `pragsmike/cyberneutics`
  carries no `repo-adaptation` skill at all; the content diff ran
  in-repo vs. `pragsmike/skills` only. Not a scope failure — reported
  as such, with cyberneutics' own contribution (corroborating layout
  evidence for AR-2) kept distinct from a content diff that doesn't
  exist. Not author-ratified; a plausible reading of "three-way diff"
  applied to what was actually found, flagged in the prompt archive's
  deviation record for review.
- **The `.agents/skills/` discovery finding rests on three independent,
  individually-weak checks combined, not one strong one.** Judgment
  call about what "tested, not assumed" (AR-2) requires as evidentiary
  weight — described in full in the deviation record. Not
  author-ratified.
- **AR-3's register-merge design question (citation-only vs. physical
  origin-tagged import) was named as an open question, not resolved.**
  Direct application of AR-3's own instruction ("name open questions
  for the author rather than resolving them silently") to a case where
  the charter's literal text and this workspace's only actual precedent
  (the tools register) point different ways. Recommendation given
  (citation-only, cheaper, matches precedent) but not chosen.
- **"The use-cases split" (charter §7 item 1) was reported as
  unscopeable rather than guessed at.** No document this session
  checked — the charter, ADR-0023, the capture session's prompt
  archive, or its session record — ever defines what artifact splits
  into what. Candidate guesses were considered and explicitly not
  adopted (named in the migration report so the author can see what
  was ruled out). This is this session's clearest instance of AR-3's
  "name, don't resolve" instruction.
- **The nested `claude -p` CLI probe (an AR-2 empirical-verification
  attempt) was abandoned after the harness refused it**, rather than
  bypassed via `unset CLAUDECODE` — the harness's own warning ("will
  crash all active sessions") was judged to outweigh the value of one
  more data point once the subagent probe plus `pragsmike/skills`'
  own documentation already gave adequate corroboration. Not
  author-ratified as the right call, but low-stakes and disclosed.

## Findings and HEAD landed

**Three-way diff headline.** `.agents/skills/repo-adaptation/` vs.
`pragsmike/skills` (HEAD `311b022`, sole commit): 10 of 11 files
byte-identical; the 11th (`SKILL.md`) differed in exactly one
frontmatter key — in-repo used `allowed-tools:` (a real Claude Code
SKILL.md field, meaning something else entirely — which Claude tools a
skill may invoke), upstream had already renamed it to `compatibility:`
(matching its own `CONTRIBUTING.md` spec) to avoid exactly that
collision. Folded in. vs. `pragsmike/cyberneutics` (HEAD `c9aef26`): no
`repo-adaptation` skill exists there at all; its 8 skills (`committee`,
`diary`, `editorial-review`, `handoff`, `probe`, `review`, `scenarios`,
`string-diagram`) live at `.claude/skills/`, and its own `.agents/`
holds five unrelated persona files (`frankie.md`, `joe.md`, `maya.md`,
`tammy.md`, `vic.md`) — contributing corroborating placement evidence,
not a content diff.

**Discovery findings, verbatim (from `compatibility-matrix.md`'s own
"Verified findings" section — repeated here per the close-out's own
instruction):**

> `.agents/skills/` is not a Claude Code discovery path in this
> environment. `ehr-testing-tools`'s own 11-skill directory there is
> invisible to Claude Code today — a fact inherited unmodified from
> sim's original bootstrap (`notes/ADRs.md`, the workspace-formation
> ADR's R8: "Sim's own copy of `.agents/skills/` is dropped as a
> duplicate — the workspace's live `.agents/skills/` already carries
> that content forward"), never independently checked against real
> tool behavior until this pass.

Evidentiary chain behind that conclusion (all three legs necessary,
none sufficient alone — see deviation record): (1) this session's own
Skill-tool listing contains `anthropic-skills:repo-adaptation`,
description byte-identical to this file's own, but zero files or
directories named `repo-adaptation` or `anthropic-skills` exist
anywhere under `~/.claude` (searched exhaustively, including the one
registered marketplace's own plugin list) — a fixed, built-in catalog
entry, not evidence of live discovery; (2) a live probe,
`.claude/skills/zzz-discovery-probe-20260801/SKILL.md`, created and
thrown away this session, did not appear in a freshly spawned
subagent's own listing (inconclusive alone — a subagent may inherit a
catalog fixed at the parent's session start); (3) `pragsmike/skills`'
own `README.md` and `docs/adoption-guide.md` (cloned this session, HEAD
`311b022`) state Claude Code's real paths as project
`.claude/skills/<name>/` and personal `~/.claude/skills/<name>/`, or a
plugin/marketplace install — never `.agents/skills/`.

**Migration report: 14 items, 4 already done, 10 open, 0 urgent.**
Full detail in `.agents/plans/2026-08-01-migration-report.md`. The two
most load-bearing open questions: whether the `notes/sim/` register
merge is citation-only (matching the `notes/tools/` precedent already
in live use — 11 `sim/ADR-` citations exist, 0 `sim/F-` citations
exist, both under the citation-only pattern today) or a physical
origin-tagged import (matching the charter's own literal wording, but
requiring a numbering-collision design the citation-only reading
sidesteps entirely); and what "the use-cases split" (charter §7 item
1) concretely refers to, unscopeable from any document this session
could find. Estimated 7–9 sessions to close every remaining item on the
cheap reading of every open question, 9–11 on the expensive reading.

**Post-push message verification, all three checkpoints:**

- **C1** (`973a9b3`): `git log --format=%B -1` vs. the source message
  file differed by exactly one trailing blank line (29 lines vs. 28) —
  confirmed at the byte level to be `git log --format=%B`'s own
  formatting artifact (it always appends a trailing newline beyond the
  message's own), not a recurrence of the C3 backtick-dropping hazard
  this rider exists to catch. No fix-forward needed.
- **C2** (`30a5ad4`): same artifact, same conclusion (22 lines vs. 21,
  single trailing-blank-line delta only).
- **C3** (this commit): cannot be checked before this push, by
  construction — the verification step requires the commit and its
  push to already exist, and this record (part of C3) must be written
  before C3's own final push, per R-A. Checked immediately after
  pushing, in-session, not deferred to a future record; if it had
  surfaced anything beyond the same trailing-newline artifact already
  characterized above, this session would have added a fix-forward
  note to a *new* record rather than amending this one (never amend a
  pushed commit) — it did not.

**Rider refinement named, not made.** Both real uses of the new
post-push check this session (C1, C2) tripped the same harmless
trailing-blank-line false positive. Worth tightening `notes/ADRs.md`
ADR-0007's own wording (e.g., "a diff whose only delta is one trailing
blank line is a match") in a future session, rather than reopening this
session's own already-pushed C1 commit to reword it.

**HEAD landed:** the commit this record's own checkpoint produces
(`docs: session record and prompt archive -- skill-adaptation
session`), pushed immediately after per R30. Per-checkpoint shas for
the two prior commits: C1 `973a9b3`, C2 `30a5ad4`. HEAD at session
start: `febb10d`.
