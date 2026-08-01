# Migration Report — ehr-testing-tools

**Repository:** `ehr-testing-tools` (this repo)
**Date:** 2026-08-01
**Mode:** assessment (per `.agents/skills/repo-adaptation/SKILL.md` Steps 1–3; no implementation this session — see AR-4 fence, `.agents/prompts/2026-08-01-skill-adaptation.md`)
**Produced by:** the adapted repo-adaptation skill's own assessment mode, run against this repo, per charter §7 item 3 (`.agents/plans/2026-08-01-agent-ux-charter.md`)

This report has two parts. **Part A** is the skill's own template output (Steps 1–2: inspect and classify). **Part B** is this session's actual deliverable: every charter §4 migration-table row and §5 gate not yet executed, as an approvable work item with its co-landed gate, sequenced, session-sized, and with open questions named rather than resolved. Charter rows already closed by the 2026-08-01 capture session (ADR-0023) or by this session's own skill-adaptation checkpoint are listed too, marked done, so this report is a complete picture of the table — not just the gaps.

---

## Part A — Inspection and classification

### Classification

**State: Partial modern.**

Evidence: `AGENTS.md` exists at the repo root (10,815 bytes, restructured 2026-08-01 per ADR-0023), well-formed, and is the stated canonical instruction surface. `CLAUDE.md` exists (257 bytes) as exactly the compatibility shim `migration-rules.md`'s own edge-case guidance recommends — a pointer to `AGENTS.md`, not competing content — so this is not a Legacy or Mixed/conflicted repo despite two instruction files coexisting. `.agents/` exists with `skills/` (11 directories), `plans/`, `prompts/`, `session-records/`, and `memory/`. The "partial" half: per this session's own empirical testing (Part B, item 9), `.agents/skills/` is not a Claude Code discovery path at all, so the single largest piece of `.agents/` machinery does nothing for the tool most sessions here actually run under; and the charter's own diagnosis (§2) names four concrete structural gaps — a still-unresolved two-register split (`notes/prompts/` vs `.agents/prompts/`, `notes/sim/*` vs the live registers), a stray `agent/` (singular) directory, no index-completeness/README-presence/reading-set-budget gates, and a session-record ritual that lapsed for eight sessions before this week's charter closed the enforcement gap that let it.

### Inventory of discovered files (condensed from `scripts/inspect-repo.sh .`)

| File / Directory | Type | Tool | Summary |
|---|---|---|---|
| `AGENTS.md` | modern instruction file | Codex, OpenCode (native, unverified this pass); Claude Code (manual, via CLAUDE.md pointer — see Part B item 9) | Root policy, restructured 2026-08-01, 10,815 bytes |
| `CLAUDE.md` | compatibility shim | Claude Code | 257 bytes, points to `AGENTS.md`; auto-loaded into every Claude Code session's context (confirmed — see Part B item 9) |
| `.agents/` | modern structure | intended: all tools | 46 files: `skills/` (11 dirs), `plans/`, `prompts/`, `session-records/`, `memory/` |
| `.claude/` | tool settings | Claude Code | 2 files: `settings.local.json`, `scheduled_tasks.lock` — no `skills/` or `commands/` subdirectory |
| `agent/` (singular) | stray legacy dir | — | 1 file: `scenario-roster.md`, copied verbatim from `pragsmike/cyberneutics` 2026-07-23, never merged |
| `notes/` | archive/register | — | Not itself indexed (no `notes/README.md`); contains `ADRs.md`, `facts-register.md`, 5 loose audit/characterization docs, `prompts/` (indexed, 29 files), `sim/` (frozen, unindexed), `tools/` (frozen, unindexed) |
| Subdirectory `CLAUDE.md` files | — | — | None found |
| Ad hoc guidance (`docs/ai-*`, `AI_INSTRUCTIONS.md`, etc.) | — | — | None found |
| `.cursorrules`, `.opencode/`, `.github/copilot-instructions.md`, `.windsurfrules`, `.aider*` | legacy tool files | — | None found — no other tool has ever had bespoke config here |

Full raw script output is in this session's record (`.agents/session-records/2026-08-01-skill-adaptation.md`).

### Proposed target structure

Already matches the skill's standard layout (`AGENTS.md` + `.agents/{skills,plans,memory,...}`) with two repo-specific extensions the skill's own `target-structure.md` doesn't anticipate: `.agents/prompts/` (session-prompt archive, R-A) and `.agents/session-records/` (session-record ritual, R-A) — both charter-native additions, not template deviations to fix. The one real deviation, found this session: `.agents/skills/` is populated but invisible to Claude Code (Part B item 9) — the standard layout's own skills location doesn't function for this repo's most-used tool.

### Compatibility notes

See `.agents/skills/repo-adaptation/references/compatibility-matrix.md`'s "Verified findings (2026-08-01)" section — the full empirical writeup lives there, folded into the skill itself rather than duplicated here, since it's evidence about the skill's own applicability, not just this repo's state.

---

## Part B — Charter work items

Fourteen items below: nine are charter §4 table rows, five are gates/artifacts from §5 and AR-3's own extra scope (reading-sets.edn, the way-of-working skill distillation, `agent/` retirement, memory/plans stub disposition, register-merge design). Each carries a status verified this session (not assumed from the charter's own prose), a concrete description of the remaining work, its co-landed gate, a session-size estimate, and dependencies. Open questions are called out inline and re-collected at the end.

### Already done — no work item

| # | Charter row | Evidence |
|---|---|---|
| D1 | `docs/dev/positioning.md` → `AUDIENCES.md` | File confirmed renamed; `stale_path_test.clj` already forbids `positioning.md` as a stray reference and asserts the rename both directions (lines 36–46, 70, 89–91) — the tripwire extension for this specific item is complete, not just the rename |
| D2 | AGENTS.md restructured | Confirmed present, 204 lines per ADR-0023's own claim (not re-measured line-by-line this session) |
| D3 | `.agents/skills/repo-adaptation/` adapted in place | This session's own C1 checkpoint (three-way diff, one fold-in, dated empirical findings) |
| D4 | `notes/prompts/` forward pointer | `notes/prompts/README.md` already links onward to `.agents/prompts/README.md` (line 5) — the pointer half of item 1 below is done; the file-move half is not |

### Open work items

**1. `notes/prompts/*` → `.agents/prompts/*` (historical migration)**

**RULED 2026-08-02 (migration session 2): executed.** The second
reading named below is ratified and sealed: the 29 files never
physically move; the landed forward pointer already was the whole
migration. `ehrt.docs-tooling.notes-prompts-frozen-test` now pins the
exact 29-file set as a per-push gate — any future addition, removal, or
rename to `notes/prompts/` fails the build (`README.md` itself stays
unpinned by content, only by existence). See item 12 below for the
paired tripwire extension.

Status (as assessed migration session 1, superseded above): not started beyond the forward pointer (D4). All 29 files stay at `notes/prompts/`; only prompts from sessions after 2026-08-01 land in `.agents/prompts/` (2 there now).

Work: charter's own instruction is explicit and narrow — "only this session's own new prompt lands in `.agents/prompts/`; the existing `notes/prompts/*.md` files stay where they are" (ADR-0023's fence). Re-reading the charter table itself ("Move with history; index both READMEs; retire the `notes/prompts` convention with a tombstone README") against ADR-0023's fence text, these disagree on whether the 29 files ever physically move. **Open question:** does "move with history" mean `git mv` all 29 files (preserving blame) with a tombstone left at `notes/prompts/README.md`, or does it mean what already happened — new prompts only, old ones frozen in place with a pointer, and "tombstone" describes the *README*, not the directory's contents? The forward pointer already landed under the second reading. Recommend ratifying the second reading (matches precedent: sim's and tools' own histories were never physically relocated file-by-file either, R8) unless the author wants full history-preserving moves.

Gate: index-completeness (§5.1) over `.agents/prompts/README.md`'s own list — already exact-match today (2 files, 2 listed).

Session size: **one session**, small, once the open question above is ratified — either a no-op (already done) or a single `git mv -k` sweep plus one tombstone paragraph.

**2. Session-record ritual (ongoing, not a one-time migration)**

Status: active since ADR-0023 (R-A). Not a work item to schedule — flagged here only so the report's coverage of charter §4 is complete. No gate beyond R-A's own index-completeness check (§5.1), not yet built (see item 10).

**3. Register merge: `notes/sim/{ADRs,facts-register}.md` → live registers**

Status: **not started**, and the charter's own wording underspecifies it enough to need an explicit design ruling before a build session can start — this is AR-3's own flagged riskiest item, and the investigation this session bears that out.

What's actually there: `notes/sim/ADRs.md` has 16 ADR entries (its own internal numbering, ADR-0001–0016); `notes/sim/facts-register.md` has 22 F-rows (F1–F22, its own numbering). Both carry a frozen-provenance header already. Current state of use: root `notes/ADRs.md` already cites sim entries 11 times as `sim/ADR-NNNN` — a citation-only pattern, never a physical copy — matching exactly how `notes/tools/ADRs.md` (16 entries, already merged in the 2026-07-28 workspace-formation session) is still cited today (12 `tools/ADR-` citations in the live file) rather than copied in. Root `notes/facts-register.md` cites `tools/F` rows 3 times — but has **zero** `sim/F` citations anywhere, despite 22 sim facts existing. Sim's ADRs are lightly cross-referenced already; sim's facts-register isn't cross-referenced at all.

**Open question (author ruling needed before this is buildable):** the charter table says these files get "merged into" the live registers with "rows/entries gain a `sim/` origin marker" — read literally, that means physically copying all 16 ADR entries and 22 F-rows into the live files. But the only precedent this workspace has (tools' register, already executed) did the opposite: left the frozen file in place, untouched, and cited it by number from the live file — no physical copy, no origin-tag column, no numbering collision to solve (sim's own ADR-0001–0016 and F1–F22 don't collide with anything if they're never copied into a sequence that also contains the live file's own ADR-0001–0023 and F1–F13). If the charter wants an actual physical merge, someone has to decide how the sim numbers land in the live sequence without colliding with the live file's own ADR-0001–0023 — appending an origin-tagged block at the end (`## ADR-0024 (sim/ADR-0001) — ...`) is the shape split-stage rigor would suggest, but that's this report naming the design option, not ruling it. Recommend the author choose between: **(a)** ratify the tools precedent for sim too — citation-only, close the row as "already the workspace's practice, formalize by adding the missing 22 `sim/F` citations where a live claim already repeats one" — cheapest, consistent, no new mechanism; or **(b)** a genuine physical import with origin tags — matches the charter's literal words, costs a numbering-collision design and a one-to-one accounting pass (38 entries total) that (a) doesn't need.

Gate: register-merge integrity (§5.4) — one-to-one accounting, every sim-origin row present post-merge, tombstone carries a forward pointer. This gate's shape depends entirely on which reading above is ratified; it's cheap under (a), real work under (b).

Session size: **one session** under reading (a); likely **two** under reading (b) — one to design the numbering/import mechanism and dry-run it against a copy, one to execute and verify one-to-one against both source files (split-stage style, per this workspace's own `31675e6`/`1c3d77c` precedent of red-evidence-first gate landing).

**4. `notes/` audits and characterizations — indexed**

**RULED 2026-08-02 (migration session 2): executed.** `notes/README.md`
lands, indexing all six top-level files plus the three subdirectories,
zone-marked (current-truth registers / historical audits / frozen
provenance) per R-C's two-zone rule, naming
`2026-07-30-refactoring-review.md` as the origin of the current
refactoring arc, and stating explicitly that open work lives in
`.agents/plans/roadmap.md`, not here. No README stub landed for
`notes/sim/`/`notes/tools/` — item 11's own ruling (below) exempts them.

Status (as assessed migration session 1, superseded above): **not started.** `notes/` itself has no `README.md`. Six loose files sit at its top level unindexed: `2026-07-30-refactoring-review.md`, `carve-loss-audit.md`, `discipline-parity-audit.md`, `docs-audit.md`, `judge-engine-extraction-characterization.md`, `storefront-parity-audit.md`. `notes/prompts/` has its own README; `notes/sim/` and `notes/tools/` do not (both frozen, but the per-directory README-presence gate (§5.3) would still require one — even a one-line "frozen provenance, see X" stub).

Work: one `notes/README.md` indexing the six top-level files plus the three subdirectories (`prompts/`, `sim/`, `tools/`), marked historical/archive per R-C's two-zone rule; one-line README stubs for `notes/sim/` and `notes/tools/` if the README-presence gate is built to require them uniformly (see item 11's own open question about whether frozen dirs are exempt).

Gate: per-directory README presence (§5.3).

Session size: **one session**, small — this is a writing task, not a design one, once item 11's exemption question is answered.

**5. `docs/dev/way-of-working.md` session mechanics → distilled into `.agents/skills/`**

Status: **not started.** No `build-session`, `capture-session`, `extraction-stage`, or `errata-sweep` skill exists under `.agents/skills/` (confirmed by directory listing: the 11 existing skills are `committee`, `find-skills`, `handoff`, `probe`, `repo-adaptation`, `review`, `scenarios`, `shared-skill-layout`, `string-diagram`, `wsl-windows-git-hygiene` — none match). `way-of-working.md` itself is intact and current (it received this session's own ceremony-rider amendment, C1).

Work: four new skills, each an operational encoding of a pattern `way-of-working.md` currently only narrates:
- `build-session` — the checkpoint/COMMIT/AUTHOR-ACTION model, staging hygiene, R30/R-F ceremony **including today's post-push message-verification rider** (this session's own C1 addition is exactly the kind of accreted safeguard a `build-session` skill should encode so the next session doesn't have to re-read three files to reconstruct it).
- `capture-session` — turning a ratified design doc into ADR + doc updates (this session's own predecessor, the 2026-08-01 agent-ux capture session, is the worked example).
- `extraction-stage` — the split-stage discipline (interface-from-consumers, census-based membership, red-evidence-first gates) the `tools`→`corpus` split (`31675e6`, `65e17c4`, `294caec`) and this repo's own `1c3d77c` gate-hardening commit both used.
- `errata-sweep` — the doc-freshness/citation-fix pattern (`2026-07-29-sim-sibling-errata-sweep.md` is the worked example).

Given this session's own empirical finding (item 9 below) that `.agents/skills/` isn't a Claude Code discovery path today, **these four skills would land invisible to Claude Code the same way the existing 11 do**, unless item 9's own recommendation (dual registration or a `.claude/skills/` mirror) lands first or alongside. Sequencing this after item 9 avoids writing four more skills into a location already known not to work for the tool actually in use.

Also unresolved and explicitly argued both ways, per AR-3's instruction: **should `engine-onboarding` (currently a doc, not named here but referenced as existing) become a fifth skill in this batch, or stay a doc?** For: consistency — if session mechanics are becoming skills, onboarding is session mechanics too. Against: onboarding is read once per new session/person, cold, before any task-matching context exists — a skill only loads when its description matches an in-flight task, which is the wrong trigger model for "read this first, unconditionally." Recommend: stays a doc, cross-linked from whichever skill or reading-set covers session start — but naming it as argued-both-ways per AR-3, not ruling it here.

Gate: none directly named in §5; index-completeness (§5.1) once these exist, same as any other skill directory.

Session size: **one session** for all four skills together (they share a source document and a template), contingent on item 9 landing first or in the same session.

**6. `agent/scenario-roster.md` → `.agents/skills/scenarios/` (merge)**

**RULED 2026-08-01 (migration session 1): executed.** Moved to
`.agents/skills/scenarios/roster.md` (committee's own precedent —
`.agents/skills/committee/roster.md` sits directly under the skill dir,
not under `references/`); every current-tense `agent/scenario-roster.md`
and `agent/roster.md` reference swept across `.agents/skills/{scenarios,probe,review}/SKILL.md`,
plus the moved file's own stale header citation
(`.agents/prompts/archive/...` → `notes/tools/prompts/2026-07-24-exp-sbom.md`,
the file that citation actually names). Dated/historical docs (this
report, the charter, session records) were left uncited-through per the
existing tripwire-test precedent for narration. `agent/` (singular) is
retired — the five-minute check (open question 4 below) found the
`scenarios` `SKILL.md` referenced the roster by the stray relative path
in six places; it was in fact broken as suspected, now fixed.

Status (as assessed this session, superseded above): **not started, and more than tidiness** — this is a live functional gap, not just a stray file. `.agents/skills/scenarios/SKILL.md` exists but is the *only* file in that skill directory; the roster data it presumably needs (`agent/scenario-roster.md`, the actual member list — Continuity, Disruption, Opportunity, etc. — copied verbatim from `pragsmike/cyberneutics` 2026-07-23) still sits in the stray singular `agent/` dir, never merged in. If the `scenarios` skill's own instructions reference a roster file at a `.agents/skills/scenarios/`-relative path, it's currently broken (not verified this session whether it's referenced by relative path or expected to be supplied ad hoc — worth checking in the build session, not this one).

Work: move `agent/scenario-roster.md` into `.agents/skills/scenarios/references/` (or wherever `SKILL.md`'s own instructions expect it — read that file first), update any relative citation, retire the now-empty `agent/` directory.

Gate: index-completeness (§5.1) once `.agents/skills/scenarios/` has more than one file.

Session size: **one session**, small — bundle with item 7 (`agent/` retirement) since finishing this item *is* item 7.

**7. `agent/` (singular) directory retirement**

**RULED 2026-08-01: executed**, as a side effect of item 6 landing (predicted correctly above). `agent/` no longer exists in the tree.

Status (as assessed this session, superseded above): **not started**, but trivially closes once item 6 lands — `agent/` has exactly one file, and moving it empties the directory. Not a separate session; listed separately here only because the charter table lists it separately.

**8. `.agents/reading-sets.edn`**

Status: **not started.** No file exists. Charter §5.2 specifies the mechanism (a test summing real line counts, budgets that start equal to current actuals so growth is visible from day one) but not the file's shape or which task-classes it covers.

Work: author `reading-sets.edn` with an onboarding set and per-task-class sets (corpus / sim / judge / docs, per the charter's own list at §1) as path lists; write the budget test (line-count sum per set, starting budget = current measured total, per §5.2's own "placeholder budgets = current actuals" instruction — not this report's job to pick real numbers, but to note that the mechanism requires measuring current reading-set sizes first, which hasn't been done). `components/docs-tooling/test/ehrt/docs_tooling/structure_currency_test.clj` and `stale_path_test.clj` are the closest existing precedent for "a test that scans real files and fails on drift" — worth reading before designing this one, not copying wholesale (they check content patterns, not aggregate size).

Gate: this item *is* its own gate (§5.2).

Session size: **one session** — defining the sets and measuring current sizes is the whole job; the numbers themselves are explicitly deferred (charter §6) to real measurement, not invented here.

**9. Claude Code skill-discovery gap** *(not a charter §4 row by name, but this session's own empirical finding, and the charter's own migration table row for repo-adaptation explicitly asks for this: "verify current tool discovery paths ... by test, not assumption")*

Status: **verified, not yet acted on.** `.agents/skills/`'s 11 directories are invisible to Claude Code today (full evidence: `compatibility-matrix.md`'s "Verified findings" section, this session's record). Codex and OpenCode's native `AGENTS.md`/`.agents/` support is unverified this pass (carried from the skill's original authoring) but plausible per their own stated design goals.

Work: the charter's own AR-2 instruction is explicit that a repo-change implication here "is a migration-report item, not this session's edits" — so naming the options, not picking one:
- **(a) Dual registration**: keep `.agents/skills/` as the AGENTS.md-native source of truth, add a `.claude/skills/<name>/` copy (or symlink, where the platform allows — Windows/WSL symlink support in this dual-clone setup is itself a known friction point per this workspace's own WSL-hygiene skill) per skill that should be live for Claude Code sessions.
- **(b) `.claude/skills/` as the real location, `.agents/skills/` as a thin AGENTS.md-native pointer** — inverts (a); costs more churn now (11 directories move) but matches what `pragsmike/skills`' own adoption guide treats as canonical for Claude Code and avoids a sync-drift risk between two copies.
- **(c) Do nothing** — accept that `.agents/skills/` serves Codex/OpenCode only, and Claude Code sessions in this repo get their operational knowledge from `AGENTS.md` and doc prose instead of skills. Consistent with today's actual reality (these 11 skills have apparently been dead weight for Claude Code since 2026-07-29 without anyone noticing until this pass), but wastes the skill-authoring investment already made.

**Open question for the author:** which of (a)/(b)/(c), given this repo's actual session population is presumably majority-Claude-Code (the ceremony docs, WSL hygiene, and this very report are all Claude-Code-session artifacts)? This report recommends (a) as the least disruptive that actually fixes the gap, but does not rule it.

Gate: none named in §5 directly; would need its own drift-prevention test (symlink-integrity or content-hash-match between the two copies) if (a) is chosen.

Session size: **one session** for (a) or (c); **one to two** for (b), given 11 directories to move and re-verify.

**RULED 2026-08-01, conditional, then re-blocked (migration session 1).** The
ruling was (a)/(b) conditional on an empirical symlink-discovery probe
— see the session record for the probe attempt. Before executing
either reading, this session found a **standing conflict neither the
ruling nor this report anticipated**: `AGENTS.md` §`.claude/` carries an
explicit, author-ruled, standing prohibition — *"`.claude/` stays
untracked... Do not `git add` anything under it"* (carve-loss audit,
2026-07-28) — and every option this item names ((a) symlink or mirror
committed at `.claude/skills/`, (b) relocate skills there as the real
location) requires committing content under `.claude/`. This is a
repo-law conflict, not a mechanism choice this session's own delegated
judgment covers (AR-1 delegated symlink-vs-mirror, not whether to breach
a separate standing ruling to do either). Per this repo's own
fix-forward-with-disclosure rule (`AGENTS.md` Constraints, ADR-0001
R10): stopped, recorded here, asked the author rather than silently
overriding or silently choosing (c). **Item 9 remains open pending that
answer** — see the session record for the exact question posed.

**10. Index-completeness gate (`.agents/` and `notes/`)**

Status: **not started.** `.agents/prompts/README.md` and `.agents/session-records/README.md` both already list their contents accurately by hand (verified this session — exact match, 2/2 and 8/8 respectively at last check) but nothing *enforces* that they stay that way; a future session could add a file and forget the index line with nothing failing.

Work: a test in the `docs-tooling` component (natural home, given `structure_currency_test.clj` and `stale_path_test.clj` already live there) that walks each indexed directory (`.agents/prompts/`, `.agents/session-records/`, `.agents/skills/*` once item 5/9 give it content worth indexing, `notes/prompts/`) and asserts the README's own file list matches the directory's real contents, both directions (presence — every real file is listed — and absence — every listed file is real), same shape as the exact-token-both-directions pattern this repo's own `1c3d77c` commit just hardened two other gates into.

Gate: this item *is* the gate.

Session size: **one session.**

**11. Per-directory README presence (`.agents/` and `notes/` subdirectories)**

**RULED 2026-08-02 (migration session 2): executed.** Open question
answered per ruling 6 (already recorded below): `notes/sim/` and
`notes/tools/` are exempt. `.agents/skills/README.md` plus all 10
skill-directory READMEs landed (mirrored into `.claude/skills/` too,
keeping `skill-mirror-currency-test` green); `notes/README.md` covers
item 4's own indexing need for `notes/`'s three subdirectories.
`ehrt.docs-tooling.readme-presence-test` now enforces this as a
per-push gate — every direct subdirectory of `.agents/`, of
`.agents/skills/`, and of `notes/` must carry a `README.md`, except the
ruling-6 pair.

Status (as assessed migration session 1, superseded above): **not started; currently 0 of 10 `.agents/skills/*` subdirectories have a README** (confirmed this session — `committee`, `find-skills`, `handoff`, `probe`, `repo-adaptation`, `review`, `scenarios`, `shared-skill-layout`, `string-diagram`, `wsl-windows-git-hygiene` all lack one), and neither do `notes/sim/` or `notes/tools/` (both frozen). `notes/prompts/` already has one.

**Open question:** does this gate apply to frozen/tombstone directories (`notes/sim/`, `notes/tools/`), or are they exempt as historical, already self-describing via their own frozen-header text? Recommend exempt with a one-line stub only if the index-completeness test (item 10) needs a real file to point at — otherwise skip them, since forcing a README onto a directory whose own charter is "byte-identical, never rewritten" is mildly in tension with that promise.

Work: 10 skill-directory READMEs (each can be short — name, one-line purpose, pointer to `SKILL.md`) plus whatever item 4 and the open question above settle for `notes/`.

Gate: this item *is* the gate (§5.3).

Session size: **one session**, bundled naturally with item 4 (both are "write short indexing READMEs" work) and possibly item 5 (new skills need READMEs too, if written after this gate lands — sequencing note below).

**12. Tripwire extension: `notes/prompts/`**

**RULED 2026-08-02 (migration session 2): executed.** `stale_path_test.clj`
gains a third addendum forbidding present-tense/imperative instruction
that work archives to `notes/prompts/`, scoped by verb tense (not the
bare path token) so legitimate historical narration
(`docs/dev/way-of-working.md`'s own past-participle reference) and
citations of a specific archived file both stay legal. Scanned over
`docs/**/*.md` + `AGENTS.md` + every `.agents/skills/**/SKILL.md`, per
this item's own scope note below.

Status (as assessed migration session 1, superseded above): **half done.** `positioning.md` is already in `stale_path_test.clj`'s forbidden list (confirmed, lines 36–46/70/89–91) — that half of §5.5 is closed. `notes/prompts/` is not yet in any forbidden-reference list, and per the existing test's own documented scope (it deliberately never reads `notes/ADRs.md`, `notes/prompts/`, or `.agents/session-records/`, since those "narrate history and legitimately cite the old names"), adding `notes/prompts/` as forbidden would need to be scoped to *current-tense instructional* text only (`AGENTS.md`, `docs/**/*.md`, skill files) — the same scope the existing test already uses for `docs/`.

Work: extend `stale_path_test.clj`'s (or a sibling test's) scan to flag `notes/prompts/` appearing in current-tense instructional prose, once item 1's own open question (does the directory retire, or just get a pointer?) is settled — this gate's exact shape depends on that answer.

Gate: this item *is* the gate; depends on item 1.

Session size: folds into item 1's session.

**13. `.agents/memory/`, `.agents/plans/` — filled or explicitly deferred**

**RULED 2026-08-01: `roadmap.md` specifically wanted, and landed** — `.agents/plans/roadmap.md`, seeded from Appendix A of the design channel's ledger handover, indexed in `plans/README.md`. `.agents/memory/` stays closed-as-is (no ruling needed there — only the roadmap question was live).

Status (as assessed this session, superseded above): **arguably already satisfied, pending author confirmation — not a build item.** `.agents/memory/README.md` already states, in its own words, why it's empty and what would end that ("this workspace hasn't yet accumulated its own durable design lineage distinct from what `notes/ADRs.md` already records") — that reads as exactly the "deliberately empty until X" language the charter row asks for. `.agents/plans/` is not empty at all — it holds `2026-08-01-agent-ux-charter.md` and, as of this session, `2026-08-01-migration-report.md` itself.

**Open question:** does the charter consider this row closed as-is, or does "filled" mean something more specific (e.g., a `roadmap.md`, which `plans/README.md` names as the not-yet-created rolling-plan file distinct from one-off plan docs like the charter)? Recommend closing this row with a one-line ADR note rather than scheduling work, unless the author wants `roadmap.md` created now.

Session size: **zero, pending a ruling** — likely closes without a build session.

**14. Use-cases split** *(charter §7 item 1, ruled "yes" but never defined anywhere this session could find)*

Status: **cannot be scoped — the charter, the adoption ADR, the capture session's own prompt and session record, and this session's own search of `notes/` and `.agents/` all name "the use-cases split" as a ruled, pending item without ever stating what document or artifact splits into what.** This is not this session's gap to fill by inference — AR-3 asks that open questions be named for the author, not resolved, and this is the clearest case of that in the whole report.

**Open question:** what, concretely, is "the use-cases split"? (Candidate guesses this report is deliberately *not* adopting: splitting `docs/use-cases.md`/`components/corpus/docs/use-cases.edn` by audience; splitting the charter's own use-cases enumeration in `AUDIENCES.md`; something from the design-channel conversation that produced the charter, never written down per R-B's own admission that "the design-channel conversation... holds whatever isn't in those deviation records.")

Session size: unknown until scoped — likely **one session** once defined, since the charter calls it "independent, can run first or parallel," implying it's not large.

---

## Sequencing recommendation

Given dependencies named above:

1. **Item 9** (Claude Code discovery fix) first, or explicitly deferred with the "do nothing" option (c) consciously chosen — everything that adds more content to `.agents/skills/` (items 5, 6) is more valuable once this is resolved, not before.
2. **Items 6 + 7** together (scenarios merge closes `agent/` retirement as a side effect) — small, no dependencies, can run anytime, but logically pairs with item 9 since it's more skill-directory work.
3. **Item 3** (register merge) needs its own open question ratified *before* scoping a session — flag to the author first, standalone.
4. **Item 1** (`notes/prompts/` migration) needs its own open question ratified first too; small either way.
5. **Item 12** (tripwire extension) rides item 1's session once ratified.
6. **Items 4 + 11** together (`notes/` index + README-presence gate) — natural pairing, same kind of work, item 11's frozen-dir exemption question should be answered first but doesn't block starting.
7. **Item 10** (index-completeness gate) after items 1, 4, 11 land — otherwise it's testing against a moving target.
8. **Item 5** (way-of-working → skills) after item 9, so the new skills don't land in a location already known not to work.
9. **Item 8** (reading-sets.edn) can run anytime independently — no dependencies on the above.
10. **Item 13** likely needs no session at all, just a ruling.
11. **Item 14** cannot be sequenced until scoped.

Rough total: **7–9 sessions** if every open question resolves toward the cheaper reading; **9–11** if the register merge (item 3) and skill relocation (item 9) both resolve toward their more expensive readings.

## Open questions, consolidated (for the author, not resolved here)

1. Item 1 — does `notes/prompts/` physically move, or stay in place with the pointer already landed being the whole of "migration"?
2. Item 3 — register merge: citation-only (matches the tools precedent) or physical origin-tagged import (matches the charter's literal words)?
3. Item 5 — does `engine-onboarding` join this skill-distillation batch, or stay a doc? (Argued both ways above; recommend stays a doc.)
4. Item 6/9 — does the `scenarios` `SKILL.md` currently reference the missing roster file by path? (Worth a five-minute check at the start of whichever session does item 6, not answered here.)
5. Item 9 — dual registration, relocate to `.claude/skills/`, or accept `.agents/skills/` as Codex/OpenCode-only?
6. Item 11 — are frozen `notes/sim/` and `notes/tools/` exempt from the README-presence gate?
7. Item 13 — does the charter consider `.agents/memory/`+`.agents/plans/` already closed, or is `roadmap.md` specifically wanted?
8. Item 14 — what concretely is "the use-cases split"?

## RULED 2026-08-01 (migration session 1)

The author ruled all eight open questions above before this session began work. Recorded verbatim:

1. `notes/prompts/` stays frozen in place; the landed pointer is the whole migration (second reading ratified; R8 precedent).
2. Register merge: (a) citation-only — the tools precedent extends to sim; the work item becomes adding the missing `sim/F`-citation stubs where live claims repeat frozen rows, plus the notes index.
3. `engine-onboarding` stays a doc.
4. Scenarios roster path check: do it at item 6's start (this session) — done; the reference was in fact broken (six relative-path citations to a file that had never been merged in), confirmed and fixed this session.
5. Discovery fix: report's (a), conditional on the empirical probe (AR-1) — probe attempted this session; see item 9's own body above for the standing-conflict finding that re-opened this question rather than closing it.
6. Frozen `notes/sim/`, `notes/tools/` are exempt from the README-presence gate.
7. `roadmap.md` is wanted; created this session from Appendix A — see `.agents/plans/roadmap.md`.
8. "Use-cases split" = review P3-1 (`notes/2026-07-30-refactoring-review.md` §5.2): split `docs/use-cases.md` at the `use-cases.edn` source into index + per-use-case files, generation/anchors/freshness-gate intact. Item 14 is hereby scoped.

This session (per its own author rulings AR-1..AR-5) executed items 9 (blocked, see above), 6, 7, and 13, and left items 1, 3(a), 4 (the underlying work, not the roster path-check), 5, 8, 10, 11, 12, 14 fenced for later sessions (AR-5) — see `.agents/session-records/2026-08-01-migration-session-1.md` for the full account.

## RULED 2026-08-02 (migration session 2)

This session (per its own author rulings AR-1..AR-5) executed items 1
and 12 together (`notes/prompts/` sealed: file-list gate,
archive-instruction tripwire) and items 4 and 11 together (`notes/README.md`,
the `.agents/skills/` README-presence gate and its 11 missing READMEs),
and left items 3(a), 5, 8, 10, 14 fenced for later sessions (AR-5) — see
`.agents/session-records/2026-08-02-migration-session-2.md` for the
full account, including the AR-1(b) sweep finding (neither
`docs/dev/way-of-working.md` nor `AUTHORS-GUIDE.md` needed repointing —
already correct) and the README survey (11 of 18 required directories
were missing one at session start).

## Urgent items

**None.** Nothing found this session is actively broken in a way that's causing failures today — the `.agents/skills/` discovery gap (item 9) has been silently true since 2026-07-29 without incident, precisely because nothing currently depends on Claude Code discovering those skills; the `scenarios` skill's possible missing-roster break (item 6) is unverified and, if real, has apparently never been hit either. Both are named as work items above, not flagged urgent.

## Validation (per skill template)

- [x] `AGENTS.md` exists and is well-formed
- [x] `.agents/skills/` exists (though see item 9 — existing ≠ discovered)
- [x] No legacy files deleted without approval (none touched this session)
- [x] Compatibility notes present for retained legacy files (`CLAUDE.md`; see `compatibility-matrix.md`)
- [x] Re-inspection confirms expected structure (`inspect-repo.sh` run this session, output in the session record)
