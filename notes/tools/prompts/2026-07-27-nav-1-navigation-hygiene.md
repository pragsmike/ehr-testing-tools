2026-07-27 — NAV-1 (`ehr-testing-tools`): fix the stale doctrine line, index the registers, archive spent plan rows, layer AGENTS.md

Context
Three findings, one theme: the cost of reading this repo. (1) `AGENTS.md`'s "Repo conventions" section still says internal src structure is "an open decision, not yet ratified" and calls `core.clj` placeholder scaffolding — but `docs/positioning.md` §Open decisions records that decision as resolved by ADR-0004 (2026-07-23): the primary agent instruction surface contradicts the positioning doc. (2) The reasoning-of-record files have no index: `notes/ADRs.md` is ~83KB and `notes/facts-register.md` ~42KB, so any agent needing one ADR or one F-row pays for the whole file or gambles on grep. (3) `.agents/plans/user-docs.md` is 801 lines of entirely-Done rows read as if live. This session is doc-only: no `src/`, no `test/`, no behavior. The governing rule: token economy by indexing and layering, never by deleting or rewriting records — append-only and fix-forward disciplines hold everywhere.

Read first

* `AGENTS.md` (whole), `docs/positioning.md` §Open decisions, `notes/ADRs.md` ADR-0004
* `notes/ADRs.md` headings (`grep -n '^## ADR-'`) and `notes/facts-register.md` (structure, not every row)
* `.agents/plans/` (all three), `AUTHORS-GUIDE.md`
* `CLAUDE.md` (must stay in sync with AGENTS.md per its own rule)

Author rulings

1. The contradiction resolves toward the ADR. ADR-0004 ratified the capability-namespace structure; `AGENTS.md`'s bullet is rewritten to state the ratified structure (one line, citing ADR-0004). Whether `core.clj` is still placeholder is a probe, not an assumption: check what it contains and what requires it; if it is genuinely vestigial, removing it is IN scope for this session only if `make test` and both lints stay green with zero other edits — otherwise leave it and record the finding.
2. Indexes are generated summaries, not new doctrine. ADRs.md gains a TOC at the top: one line per ADR — number, title, status (Accepted/Superseded-by-NNNN), nothing else; every line derived from the existing headings and supersession notes, no new characterization. facts-register.md gains an index table above the register: F#, a one-line claim digest, last-verified date, status. Digests are written fresh but must be extractive (a clause already present in the row), not interpretive.
3. Index freshness is enforced or honestly manual. Preferred: a small check (shape of `quickstart-fresh`'s structural enforcement) that every `## ADR-` heading and every `| F` row has an index line and vice versa, wired into T0's lint set only if it is trivially cheap; otherwise a maintenance note at the top of each index stating it is hand-maintained and updated in the same commit as any new entry — pick one, say which, never both half-done.
4. Spent plan rows move, they do not vanish. `.agents/plans/archive/` is created; `user-docs.md`'s Done rows (and any fully-Done rows in `corpus-foundations.md` — but NOT the plan's framing prose or open rows) move there under their original file names (`archive/user-docs.md` etc.), each live file keeping a one-line pointer to its archive. `judge-gate-refactor.md` is complete history referenced by AGENTS.md's tiers section — it moves only if that reference is updated in the same commit.
5. AGENTS.md layers; AUTHORS-GUIDE absorbs. AGENTS.md keeps: the user-vs-contributor fork, hard rules (each ≤ 3 lines + pointer), verification tiers (as-is — it is the working rule), quick start, skills/prompts sections trimmed to lists + pointers. Deep essays move to `AUTHORS-GUIDE.md` under new numbered sections: the hermeticity mechanics paragraph (why path-split beats tag-filter, cloverage details), the allowlist history and two-strikes narrative (the rule itself stays in AGENTS.md in two sentences). Nothing is dropped; every move leaves a pointer. Target: AGENTS.md meaningfully smaller while a fresh agent following only AGENTS.md + its pointers can still work correctly — that is the test, not a byte count.
6. CLAUDE.md sync rule honored; if AGENTS.md's structure changes, confirm CLAUDE.md's pointer text still holds.

Steps
Step 1 — Fix the contradiction (and probe core.clj)
Per ruling 1.
Commit: `docs: AGENTS.md src-structure bullet updated to ADR-0004's ratified answer (was stale 'open decision')` (second commit only if core.clj is removed: `refactor: remove vestigial core.clj scaffolding (ADR-0004 structure ratified; T0 green)`)
Step 2 — ADR index
Per rulings 2–3.
Commit: `notes: ADR table of contents — one line per record, status column, freshness policy stated`
Step 3 — Facts-register index
Per rulings 2–3.
Commit: `notes: facts-register index — F#, claim digest, verified date, above the full register`
Step 4 — Plans archive
Per ruling 4.
Commit: `plans: spent rows archived to .agents/plans/archive/; live files carry pointers`
Step 5 — AGENTS.md layering
Per rulings 5–6.
Commit: `docs: AGENTS.md layered — rules and pointers here, essays in AUTHORS-GUIDE; nothing dropped`
Step 6 — Verify, archive, push
T0 green (docs-only, but freshness gates read docs). The layering test of ruling 5, stated in the report: name what a fresh agent must read for (a) a docs-only session, (b) a src session, (c) an integration-adjacent session, with approximate sizes before and after. Archive this prompt; push.
Commit: `prompts: archive 2026-07-27 nav-1 session`

Final report
The before/after reading-path sizes (ruling 5's test), index freshness mechanism chosen, core.clj probe outcome, deviations.
