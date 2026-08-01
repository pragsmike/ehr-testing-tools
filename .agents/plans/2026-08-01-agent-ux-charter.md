# 2026-08-01 — Agent-UX Charter (design doc for author ruling)

Status: DRAFT for ruling. On acceptance this is captured into the repo (ADR + docs)
via a capture session, then implemented via build sessions. Companion rulings already
made: use-cases split (yes); notes mapping (yes, per §4); register collapse (yes);
budgets (deferred, §6); skill adapted in-repo (canonical upstreams:
`pragsmike/skills`, `pragsmike/cyberneutics`); `positioning.md` → `AUDIENCES.md`;
ADR-0001 R1 naming fix rides the first commit.

## 1. Goal

Treat agents as a designed audience. Optimize: (a) onboarding token spend — what a
cold session must read to know what's where; (b) per-task token spend — what a
corpus / sim / judge / docs task must read; (c) navigation — signposts and
breadcrumbs so no session, human, or design channel invents a parallel convention
again; (d) durability — every rule here is gated, because this workspace's own
history shows ungated surfaces rot at session velocity. Human audiences, especially
non-programmers, see only improvement: agent machinery consolidates under `.agents/`,
`docs/` stays theirs.

## 2. Diagnosis (verified, with receipts)

The root cause is enforcement asymmetry — gated surfaces stayed true, ungated ones
rotted — expressed four ways:

1. **Installed machinery ≠ used machinery, at the meta level.** `.agents/` exists
   with skills, session-records (mandated "from this session forward", 2026-07-28/29),
   memory/plans stubs — and the session-record ritual stopped 2026-07-29. The eight
   2026-07-30..08-01 sessions archived prompts but wrote outcome summaries to chat.
   Nothing gated the ritual, so it lapsed the week velocity spiked.
2. **The design channel is itself a drift source.** Session prompts dominate docs
   (correctly), so a prompt written from stale memory propagates its author's gaps at
   session rate. The 07-30..08-01 prompts never routed to `.agents/` because the
   channel didn't know it existed.
3. **Signpost burial.** AGENTS.md documents `.agents/` — at line 234 of 267, after
   the sections every session actually reads and edits. Ordering is a UX property of
   the flagship surface, currently unmanaged.
4. **Append-only without zones.** Fix-forward grows every file monotonically;
   provenance interleaves with instruction; two registers (root + `notes/sim/`)
   split reasoning across origins that no longer exist.

## 3. Rules (the discipline that prevents recurrence)

- **R-A One record ritual, gated.** Every non-trivial session writes a dated record
  to `.agents/session-records/` as its last pre-push act — the close-out summary
  lands in the repo, not only in chat. The prompt that drove the session archives to
  `.agents/prompts/` (new), paired by date-slug. Both directories carry an indexed
  README; an index-completeness gate (presence + absence, exact token — the
  gate-hardening pattern) fails the lane on any unindexed or ghost entry.
- **R-B Design-channel preflight.** Prompt authoring starts from the repo, not from
  memory: re-read AGENTS.md's head, the `.agents/` index, and the current mode
  rulings before writing a session prompt. Encoded as a repo-local skill
  (`.agents/skills/session-prompt/`) whose checklist the design channel itself
  follows; each authored prompt states the HEAD sha its conventions were read at.
- **R-C Two zones.** *Current-truth* surfaces (AGENTS.md, `.agents/` READMEs and
  indexes, reading sets, memory/) are small, budgeted, and gated. *Archives*
  (session-records, prompts, notes/ registers, audits) are append-only, clearly
  marked, and never in a reading set. Instruction never lives in an archive.
- **R-D Reading sets as data.** `.agents/reading-sets.edn`: the onboarding set and
  per-task-class sets (corpus / sim / judge / docs) as path lists with line budgets;
  a test sums real line counts and fails over budget. Budget numbers deferred (§6);
  the mechanism is not.
- **R-E Navigation same-commit.** Any change creating/moving/retiring a doc updates
  the owning index in the same commit — the facts-register Index discipline,
  generalized and gated by R-A's completeness check plus per-directory README
  presence gates.
- **R-F Ceremony default flips to R30 (PROPOSED RULING).** Amend ADR-0007: sessions
  commit and push at checkpoints unattended by default; "prepare-only" becomes the
  stated exception. The ceremony codifies this week's safeguards: staged scope must
  match the session's own file list (`--stat` check), personal-info scan, commit
  message via file (heredoc hazard), session record written before final push, hooks
  as backstop. AUTHOR ACTION checkpoints remain author-only regardless of mode.

## 4. Target structure and migration table

| From | To | Notes |
|---|---|---|
| `notes/prompts/*` | `.agents/prompts/*` | Move with history; index both READMEs; retire the `notes/prompts` convention with a tombstone README |
| (chat close-outs) | `.agents/session-records/*` | Ritual resumes per R-A; the seven existing records stay; no retroactive fabrication of the missing eight — one dated gap note instead |
| `notes/sim/ADRs.md`, `notes/sim/facts-register.md` | merged into `notes/ADRs.md`, `notes/facts-register.md` | **Origin-tagged import, not rewrite**: rows/entries gain a `sim/` origin marker, original files remain as frozen tombstones pointing forward (their own headers already claim frozen status — honor it) |
| `notes/` audits, characterizations | stay, indexed | Archive zone; marked historical in the notes README |
| `docs/dev/way-of-working.md` session mechanics | distilled into `.agents/skills/` (build-session, capture-session, extraction-stage, errata-sweep) | way-of-working stays as the human-readable narrative; skills are the operational encoding sessions load on demand |
| `docs/dev/positioning.md` | `docs/dev/AUDIENCES.md` | Adds **agents** as an explicit audience class; `positioning.md` joins the stale-path tripwire list |
| `agent/scenario-roster.md` | `.agents/skills/scenarios/` (merge) | The stray singular `agent/` dir retires |
| AGENTS.md | restructured, budgeted | Order: mode + ceremony, reading sets pointer, structure map, constraints; narrative ("Landed so far") replaced by a pointer to the ADR index; `.agents/` routing moves above the fold |
| `.agents/skills/repo-adaptation/` | adapted in place | Three-way diff against `pragsmike/skills` and `cyberneutics` copies first; verify current tool discovery paths (`.claude/skills` vs `.agents/skills`) by test, not assumption; upstream the adaptation after it works here |
| `.agents/memory/`, `.agents/plans/` | filled or explicitly deferred | Stubs either receive their first real content during migration or their READMEs state "deliberately empty until X" |

## 5. Enforcement gates (all red→green on landing, per house discipline)

1. Index-completeness over `.agents/` and `notes/` (R-A/R-E).
2. Reading-set budget test (R-D; budgets TBD but the test lands with placeholder
   budgets = current actuals, so growth is visible from day one).
3. Per-directory README presence for every `.agents/` and `notes/` subdirectory.
4. Register-merge integrity: every `sim/`-origin row present post-merge (one-to-one
   accounting, split-stage style); tombstones carry forward-pointers.
5. Existing tripwires extended: `positioning.md` and `notes/prompts/` join the
   forbidden-reference lists for current-tense prose.

## 6. Deferred

Budget numbers (R-D) — ruled after the migration lands and real reading-set sizes
are measured. The storefront demo fixture, `make quickstart` nightly wiring, and
pairing-as-data design remain separate backlog items, unblocked by this charter.

## 7. Sequencing

1. Use-cases split (independent, can run first or parallel).
2. Skill adaptation pass (assessment mode → three-way diff → patch in-repo skill).
3. Migration report (the adapted skill's own Step 3, assessment mode) → author review.
4. Capture session: this charter → ADR + AGENTS.md restructure + AUDIENCES rename +
   ADR-0001 fix + R-F amendment if ruled.
5. Build sessions: moves, merges, gates, reading sets, session-prompt skill.
6. Ritual resumes: from the capture session onward, every session writes its record.

## 8. Open ruling

R-F (ceremony default flips to commit-and-push-at-checkpoints). Everything else in
this charter operates under rulings already given.
