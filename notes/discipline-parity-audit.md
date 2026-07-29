# Discipline-parity audit

Produced 2026-07-28, `2026-07-28-ehr-testing-discipline-parity` session,
step 1. Three-way diff at the **mechanism** level — not the line level —
of every discipline/agent-infrastructure mechanism carried by
`ehr-testing-sim`'s final pre-merge tree (`213abaa`), `ehr-testing-tools`'
final tree (`stable-pre-monorepo`), and this workspace's current tree,
against R24's ruling: the workspace set is the UNION of both parents',
sim's form preferred on conflict, every tools-era mechanism individually
dispositioned rather than dropped by default.

**Read directly, not inferred:** `AGENTS.md`/`AUTHORS-GUIDE.md` at both
`213abaa` and `stable-pre-monorepo`; both repos' `.agents/` tree listings
at those points; `notes/{sim,tools}/facts-register.md`; this workspace's
current `AGENTS.md`, `AUTHORS-GUIDE.md`, `docs/way-of-working.md`,
`CONTRIBUTING.md`; `notes/ADRs.md` ADR-0001/0002/0004/0005;
`notes/carve-loss-audit.md`; and, for the two operational findings below,
the live `.agents/skills/{scenarios,probe,committee}/SKILL.md` files
checked against `git show stable-pre-monorepo:.agents/cyberneutics-config.yaml`
(does not exist even upstream — not a carve loss) and
`notes/tools/agent/scenario-roster.md` (exists, frozen, not live).

## Disposition rows

| # | Mechanism | Origin | Current workspace status | Disposition |
|---|---|---|---|---|
| M1 | `AGENTS.md` root contribution-discipline file | both (own form each) | Present, workspace's own union voice (ADR-0001 R4, ADR-0002) | ADOPT — current form stands; this audit's remaining rows are sub-mechanisms folded into its next revision (step 3) |
| M2 | `AUTHORS-GUIDE.md` §1 Git WSL-only | both (near-identical) | Present | ADOPT — unchanged |
| M3 | `AUTHORS-GUIDE.md` §2 Pack ritual (`make pack`/`pack-skills`/`pack-push`) | both | Workspace's own §2 says "not yet ported... undecided" | **RETIRE-with-reason.** Both parents had already independently retired `pack-push` from the session-end ritual before the merge (tools: "dormant... commit → push, full stop", 2026-07-25; sim: same, 2026-07-27) in favor of a real `git push` to a repo the design channel clones directly. This workspace's every session so far has had full filesystem/git access with no non-git chat surface to feed — the situation `pack` existed to work around does not exist here. Closes the "undecided" open item explicitly rather than leaving it open indefinitely. |
| M4 | `AUTHORS-GUIDE.md` §3 ADR rules | both | Present | ADOPT — unchanged |
| M5 | `AUTHORS-GUIDE.md` §4 Facts-register discipline (assert→register→date) | both | Present, but no live `notes/facts-register.md` instantiated | ADOPT + INSTANTIATE — R25, step 4 |
| M5a | Facts-register **Index** sub-table (hand-maintained digest above the full F-row table) | tools only | Not present (workspace has no live register yet) | ADAPT — real value-add once the live register grows past a handful of rows (both parents' registers are 20-40+ rows); add tools' Index format on top of sim's base F-row shape at instantiation (step 4) |
| M6 | `AUTHORS-GUIDE.md` §5 Checkpoint/deviation-record convention | workspace-native | Present | ADOPT — unchanged, this workspace's own convention, not from either parent |
| M7 | `AUTHORS-GUIDE.md` §6 "User-facing docs are agent-read" (positioning-audience authoring style: copy-pasteable commands, stable anchors, self-explanatory errors) | tools only (§6) | Absent at workspace level (tools' own `docs/positioning.md` is now component-scoped under `components/tools/docs/`) | ADOPT — generic authoring-style guidance, no conflict with sim's discipline, promote to workspace `AUTHORS-GUIDE.md` as a new numbered section (step 3) |
| M8 | NAV-1 navigation-hygiene principle (short pointers in `AGENTS.md`, detail moved to numbered `AUTHORS-GUIDE.md` sections) | tools only | Partially followed already (workspace `AGENTS.md` is already short) | ADOPT as a standing organizational principle, cited by provenance; not a concrete artifact to copy, a discipline to keep applying as both files grow |
| M9 | Verification tiers T0/fast-gate / T1-integration-smoke / T2-full-integration | tools only (ADR-0016) | Workspace has ADR-0004's own two-lane rule (R18/R19: per-push fast lane vs. nightly `projects/integration`) — a *structural* analogue, not tools' tier-naming | ADOPT-DONE, already superseded by a workspace-native mechanism (ADR-0004); not reopened here. Named so the audit shows it was considered, not missed. |
| M10 | Two-strikes rule for `.claude/settings.json` allowlist growth | tools only (§10) | Moot — `.claude/` is untracked in this workspace (carve-loss audit row, author-ruled) | RETIRE-with-reason — the mechanism only makes sense for a *committed* allowlist; this workspace deliberately keeps `.claude/` untracked (per-clone local state), so there is no shared allowlist file for a growth rule to govern |
| M11 | `.agents/memory/` (durable design lineage: sim `architecture.md`, tools `patterns.md`) | both (own file each) | Absent live (only frozen provenance under `notes/{sim,tools}/agents/memory/`) | ADOPT + INSTANTIATE — R25, step 4 (empty substrate + README contract, sim's naming convention preferred per R24 default) |
| M12 | `.agents/plans/` (sim: single rolling `roadmap.md`; tools: multiple named plans + `archive/` on completion) | both, divergent forms | Absent live | ADOPT sim's form (single rolling plan doc) per R24's conflict rule + INSTANTIATE — R25, step 4. Tools' archive-on-completion sub-pattern is compatible and not excluded, just not the default shape. |
| M13 | `.agents/session-records/` (one dated record per session, 4-part structure) | sim only | Absent live | ADOPT + INSTANTIATE — R25, step 4 |
| M14 | `.agents/handoffs/` (mid-flight cross-session continuity notes) | both (sim: `.gitkeep` only, never used; tools: 2 real files, actively used) | Absent live | **RETIRE-for-now, with reason** — not named in R25's explicit scope (memory/plans/session-records only), and this workspace's own process (`docs/way-of-working.md` §1: "one session prompt, author present throughout") structurally reduces the need for async cross-session handoffs that motivated it in both parents' 40-ish-session, no-shared-context models. Cheap to add the moment a session actually ends with open mid-flight work; not manufactured preemptively. |
| M15 | `.agents/prompts/` (tools: live dir + `archive/` subdir, moved-on-completion) vs sim's `.agents/prompts/archive/` (archive only, no live dir ever populated in sim) | both, divergent | Workspace already has its own **third** form: `notes/prompts/*.md`, flat, no archive subdirectory, self-archived in place with a dated deviation-record note appended to the same file (established by 4 prior sessions' precedent) | ADOPT workspace-native form, cited explicitly here rather than left implicit — this is neither parent's exact convention, a deliberate divergence given the checkpoint/single-session model (M14's same reasoning: no live/archive split is needed when nothing is ever mid-flight across sessions). AGENTS.md's discipline-surface map (step 3) will name `notes/prompts/` as the canonical location. |
| M16 | `.agents/skills/` union | both | DONE — ADR-0005 R21 | ADOPT-DONE, cited, not reopened |
| M17 | Scenario/probe operational substrate: `agent/scenario-roster.md` (repo-root, singular `agent/`, NOT under `.agents/`) | tools only | **Missing from the live tree entirely.** The skills (`scenarios`, `probe`) that require it at runtime are live in `.agents/skills/`; their `SKILL.md` files still reference the relative path `agent/scenario-roster.md`, which resolves to nothing in this workspace. Frozen copy exists at `notes/tools/agent/scenario-roster.md` (provenance only). | **ADOPT + FIX** — restore a live copy at workspace-root `agent/scenario-roster.md` (copied from the frozen provenance file, not re-fetched from upstream `pragsmike/cyberneutics` — same content, same citation) so the already-unioned skills actually function. Step 6 (sweep). A real gap the skills union (ADR-0005) surfaced but didn't close — skill *definitions* moved live; their *operational dependencies* didn't. |
| M18 | `.agents/cyberneutics-config.yaml` (`situations_root` key, shared by `scenarios`/`probe`/`committee`) | tools only, per its own `AGENTS.md`/skill docs | Absent, live and frozen both | **Not a carve loss — checked directly: this file never existed in tools' own committed history either** (`git show stable-pre-monorepo:.agents/cyberneutics-config.yaml` → does not exist). Documented as a repo convention but apparently kept as an uncommitted local/per-user file even in the source repo. | NAMED, DISCLOSED, NOT FIXED — nothing to restore; a future session that actually invokes `scenarios`/`probe`/`committee` for real will need to create this file fresh (its one key, `situations_root`, is a path preference, not a design decision) — not manufactured speculatively here. |
| M19 | Provenance stutter: `notes/tools/agent/` (singular, one file) vs `notes/tools/agents/` (plural, everything else) | tools frozen provenance | Both exist under `notes/tools/`, differ by one letter | **MERGE** (R29, step 8) — inspected: pre-carve, `agent/` (singular, cyberneutics upstream convention) and `.agents/` (plural, Claude-Code-era convention) were genuinely two different repo-root directories, not a naming mistake; once frozen as historical provenance the distinction no longer carries live meaning, so the single file merges into `notes/tools/agents/scenario-roster.md`. Distinct from M17 above, which is about a *live* copy at a *different* path (`agent/scenario-roster.md`, workspace root, no `notes/tools/` prefix). |
| M20 | Doc split: `doc/migration/polylith-brief.md` vs `docs/way-of-working.md` | workspace-native accident (bootstrap-era, not from either parent) | Both exist at root | MERGE (R29, step 8) — `doc/migration/` → `docs/migration/`, `doc/` removed |
| M21 | `CLAUDE.md` pointer file | both (sim: 3-line pointer; tools: pointer + one extra paragraph on skill-discovery layering) | **Absent from the entire tree** — confirmed via `git ls-files \| grep -i claude`, zero hits. `AGENTS.md`'s own header already promises it ("Claude Code users: see `CLAUDE.md`"). Carve-loss audit named this and marked it "restore: this session, step 6" (that session's step 6, not this one — never actually done) | ADOPT sim's form (shorter; carve-loss audit's own stated preference) — step 6 (sweep) |
| M22 | Session permissions allowlist (`.claude/settings.json`, git-tracked) | tools only | Ruled: stays untracked (carve-loss audit UNDECIDED row, closed by author in-session 2026-07-28) | ADOPT-DONE (ruled), not reopened |
| M23 | Staging-hygiene ritual (index-scope discipline between COMMIT checkpoints) | **no parent — new** | Two prior sessions had commit-boundary slips under R6 (ADR-0001's own "Step 7 commit-scoping mistake, self-caught" deviation record is one instance) | ADOPT-NEW — write into `AUTHORS-GUIDE.md` under R6 discipline, step 3, per this session's own prompt |
| M24 | Root fixture cwd-relative paths (`test/fixtures/**`, `test-integration/fixtures/**`) vs sim's `io/resource` convention | tools' own convention, ADR-0002's disclosed "correct, lowest-risk fix" at the time | 11 test files across `components/tools`, `bases/ehr-cli`, `projects/conformance` still read these paths cwd-relative; `notes/ADRs.md` ADR-0002 states this was a deliberate choice, not an oversight — but `docs/way-of-working.md`'s current framing (if any) claiming a completed `io/resource` conversion would be wrong | **ADAPT** (R28, step 7) — convert to brick-owned `resources/<brick>/test-fixtures/` + `io/resource`, ADR-0002 gets a dated erratum acknowledging the record's prior "moved to brick resources" framing (if any) was aspirational/partial, not completed. See step 7 for the actual per-file survey. |

## Pre-seeded rows from the session prompt, cross-checked

- **Facts/claims register** — M5/M5a/M11-M13 above.
- **Memory/plans/session-records** — M11-M13 above.
- **Positioning audiences** — M7 above.
- **NAV indexes** — M8 above.
- **Scenario roster** — M17/M19 above (split into the live-operational finding and the frozen-provenance stutter — two distinct issues, not one).
- **Deviation-record format** — checked: sim records deviations per-ADR (`notes/ADRs.md` appendix); tools records them per-prompt (`AUTHORS-GUIDE.md` §7, appended to the archived prompt body); this workspace already uses a **third**, its own form — per-ADR deviation-record sections (sim's shape) for structural decisions, plus a session-prompt-archival deviation note (tools' shape) for session-level findings — i.e. it already uses **both**, at the granularity each parent used it, rather than picking one. ADOPT-DONE, cited here, not reopened.
- **Staging-hygiene gap** — M23 above.

## UNDECIDED

None. Every row above (M1-M24) carries a disposition. Per R24/step 2,
this empty list means steps 3-5 may proceed without an author gate.
