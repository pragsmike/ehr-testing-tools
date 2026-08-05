<!-- Attic file: notes/adr/0023-agent-ux-charter-adopted.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0023 — Agent-UX charter adopted: capture executed, R-F enacted, sequencing amended

**Status:** Accepted (author-ratified via the charter's own ruling
process, `.agents/plans/2026-08-01-agent-ux-charter.md`), 2026-08-01.

### Context

The agent-UX charter (`.agents/plans/2026-08-01-agent-ux-charter.md`)
diagnosed an enforcement-asymmetry pattern across this workspace's own
`.agents/` machinery — gated surfaces (ADR discipline, facts-register,
tripwires) stayed true; ungated ones (the session-record ritual, the
design channel's own preflight reading) rotted at session velocity,
specifically the eight 2026-07-30..08-01 sessions that archived prompts
but never wrote `.agents/session-records/` entries (charter §2). The
charter's rules (§3, R-A through R-F) and target structure (§4) were
ratified by the author; this ADR is the capture session's own record of
what landed, per the charter's own §7 sequencing.

### Decision

**The charter is adopted in full** — all six rules (R-A–R-F) ruled —
this session (the "capture session," charter §7 item 4) executing the
capture step only: this ADR, the `AGENTS.md` restructure, the
`positioning.md` → `AUDIENCES.md` rename, and the ADR-0001 R1 naming
fix. Charter §7 items 1–3 and 5–6 are named below, fenced, not executed
here.

**Sequencing amendment to the charter's own §7 (design-channel ruling,
recorded here per the charter's own request).** The charter's written
sequencing put this capture step fourth, after the use-cases split (1),
the skill-adaptation pass (2), and the migration report (3). The design
channel amended this ordering before execution: **capture runs first**,
because the ceremony flip (R-F) and the record ritual (R-A) should
govern every subsequent session, *including* those three — running them
under the old, ungated ceremony first would repeat exactly the failure
mode (charter §2.1) this charter exists to close. Items 1–3 now follow
capture rather than precede it; §7's own numbered list is superseded in
place by this ordering, not rewritten — the charter file itself gains a
dated status line recording it (see below).

**R-F enacted.** `notes/ADRs.md` ADR-0007 carries its own dated
amendment (this same commit) ratifying R-F: R30-mode is now the
standing ceremony default, prepare-only the stated exception.
`AUTHORS-GUIDE.md` §1 and `docs/dev/way-of-working.md` §1 carry matching
dated notes.

**Charter status line.** `.agents/plans/2026-08-01-agent-ux-charter.md`
gains a one-line status header: "adopted; capture executed `<sha>`" —
plans stay in `plans/`, not moved or archived; the charter document
itself remains the spec R-A through R-D and R-E point back to.

### Fence — ruled but deliberately not executed this session (AR-7)

Named here as ruled-but-unexecuted, not silently deferred: the register
merge (`notes/sim/ADRs.md`/`notes/sim/facts-register.md` origin-tagged
import into the live files); the `notes/prompts/` → `.agents/prompts/`
historical migration (only this session's own new prompt lands in
`.agents/prompts/`; the existing `notes/prompts/*.md` files stay where
they are, with a one-line forward pointer added to their own README);
the index-completeness, reading-set budget, and per-directory
README-presence gates (charter §5 items 1–3); the
`.agents/reading-sets.edn` file itself; the repo-adaptation skill's
three-way diff and adaptation pass; the use-cases split; the
`agent/scenario-roster.md` → `.agents/skills/scenarios/` merge; and
filling `.agents/memory/`'s or `.agents/plans/`'s remaining stub content
beyond what this session's own record adds. This session changes law
and signage, not residence — heavy migration work is explicitly out of
scope (charter §7, sequencing amendment above).

### Deviation record

None.

**Dated note, 2026-08-01 (migration session 1).** Two items this ADR's
own fence named as ruled-but-unexecuted landed this session, per the
migration report's own sequencing (`.agents/plans/2026-08-01-migration-report.md`
items 6/7/13, all eight open questions ruled by the author — see that
report's own "RULED 2026-08-01" section for the verbatim rulings): the
`agent/scenario-roster.md` → `.agents/skills/scenarios/roster.md` merge
and `agent/` retirement (items 6+7), and `.agents/plans/roadmap.md`
landed from the design channel's ledger handover (item 13). The
Claude-Code skill-discovery fix (item 9, this ADR's charter did not
separately name it) was attempted and re-blocked this session on a
standing conflict with this file's own `.claude/`-untracked ruling
(carve-loss audit, 2026-07-28) — not resolved, referred back to the
author. Full account: `.agents/session-records/2026-08-01-migration-session-1.md`.

**Dated note, 2026-08-02 (migration session 2).** Four more items this
ADR's own fence named as ruled-but-unexecuted landed this session, per
the migration report's own sequencing
(`.agents/plans/2026-08-01-migration-report.md` items 1/12/4/11, both
"RULED 2026-08-02" blocks): `notes/prompts/` sealed (item 1 — the
landed forward pointer confirmed as the whole migration, now pinned by
`ehrt.docs-tooling.notes-prompts-frozen-test`) with its paired tripwire
extension (item 12 — `stale_path_test.clj`'s third addendum, scoped by
verb tense so historical narration and file citations stay legal); and
the `notes/` index (item 4 — `notes/README.md`) landing together with
the per-directory README-presence gate (item 11 —
`ehrt.docs-tooling.readme-presence-test`, `notes/sim/`/`notes/tools/`
exempt per ruling 6) and the 11 README files it required (`.agents/skills/README.md`
plus one per skill, mirrored to `.claude/skills/`). Items 3(a), 5, 8,
10, 14 remain fenced. Full account:
`.agents/session-records/2026-08-02-migration-session-2.md`.

**Dated note, 2026-08-02 (migration session 3).** Two more items landed
(`.agents/plans/2026-08-01-migration-report.md` items 10/3(a), both
"RULED 2026-08-02" blocks): the index-completeness gate (item 10 —
`ehrt.docs-tooling.index-completeness-test`, both directions, over the
same directory set item 11 established) and the sim citation-stubs
pass (item 3(a) — reading (a), citation-only, ratified migration
session 1; 8 F-rows and 10 ADRs of the frozen sim registers now cited at
their live restatement site, 4 of those corrections of an outright
miscitation rather than a fresh addition; `notes/facts-register.md`
gains an F20 stub naming the two-file topology). **Ruling 6 extension,
ratified by dispatch:** this session's own prompt explicitly extended
ruling 6 (the README-presence exemption for `notes/sim/`/`notes/tools/`,
ADR-0023's own prior dated note) to README-completeness too — same
directories, same frozen-provenance rationale, not a new ruling
requiring separate ratification, per the prompt's own AR-1 text
("extension recorded in the dated ADR note as a design-channel ruling
ratified by dispatch"). Items 5, 8, 14 remain fenced. A finding beyond
this session's own scope, named not fixed: `components/sim/src`
docstrings carry many bare, mis-qualified `ADR-NNNN` references beyond
the ones this session's citation survey flagged (`engine.clj` alone has
40+, most untouched) — recorded on the roadmap as a future dedicated
sweep. Full account:
`.agents/session-records/2026-08-02-migration-session-3.md`.

**Dated note, 2026-08-02 (migration session 4).** Item 8 landed
(`.agents/plans/2026-08-01-migration-report.md` item 8, no dependencies
per that report's own sequencing note): `.agents/reading-sets.edn` —
five named sets (`:onboarding`, `:corpus`, `:sim`, `:judge`, `:docs`),
each path justified inline by an EDN comment, gated by
`ehrt.docs-tooling.reading-set-budget-test` (path resolution + budget,
both red→green live-proven against the real file this session, in
addition to the test suite's own permanent fixture-based mechanism
checks). **Every `:budget-lines` value is this session's own measured
actual, the baseline the author's future budget ruling (charter §6) now
has a real number to cite against:** `:onboarding` 523, `:corpus` 1519,
`:sim` 574, `:judge` 644, `:docs` 433. Also landed: the
`components/sim/src`/`test` bare-`ADR-NNNN` docstring sweep migration
session 3 named as a future dedicated task (roadmap "Next") — 151 bare
references across 39 files classified and requalified: 149 to
`sim/ADR-NNNN` (sim's own frozen register, the overwhelming majority),
1 to `tools/ADR-0015` (`components/sim/src/ehrt/sim/run.clj` — a
cross-repo citation this session found was headed at the wrong frozen
register entirely, not just missing a qualifier), and 1 left
deliberately bare
(`components/sim/src/ehrt/sim/interface.clj`'s own citation of the
LIVE `notes/ADRs.md` ADR-0001 R5 — correct exactly as written, not
sim's frozen ADR-0001). Two further miscitations (a wrong file path
named outright, `notes/ADRs.md` where `notes/sim/ADRs.md` was meant)
also corrected. Docstring/comment/fixture-remark edits only, zero
behavior change (`clojure -M:poly check` and the full `:all
skip:integration` suite both green before and after, diffed by hand
against the pre-sweep baseline). Items 5, 14 remain fenced. Full
account, including the complete one-to-one accounting table:
`.agents/session-records/2026-08-02-migration-session-4.md`.

**Correction, same day.** The `:onboarding` figure above (523) was
measured before this same session's own C3 (this dated note, the
session record, and the roadmap's "Done" entries for this session,
below) added lines to `.agents/plans/roadmap.md` — itself one of
`:onboarding`'s own cited paths. Measured actual after C3:
**`:onboarding` 538** (the other four sets are unaffected; none of them
cite `roadmap.md`). `.agents/reading-sets.edn` carries the corrected
number; this note is a same-session fix-forward, not an edit to the
paragraph above.

**Dated note, 2026-08-02 (migration session 5).** Item 5 landed
(`.agents/plans/2026-08-01-migration-report.md` item 5, sequenced
after item 9 per that report's own recommendation, item 9 itself
closed the same session on the author's own confirmation -- see that
report's own dated `CONFIRMED`/`RULED` paragraphs): five repo-local
skills under `.agents/skills/` -- `build-session`, `capture-session`,
`extraction-stage`, `errata-sweep`, `session-prompt` (the fifth beyond
this item's own original four-skill scope, added by this session's own
ratifying prompt) -- each distilled from `AGENTS.md`,
`AUTHORS-GUIDE.md`, `docs/dev/way-of-working.md`, this ADR, and the
session records/ADRs each skill's own `SKILL.md` cites by name.
Mirrored to `.claude/skills/`; `ehrt.docs-tooling.readme-presence-test`,
`index-completeness-test`, and `skill-mirror-currency-test` all green.
`build-session` was additionally added to every `.agents/reading-
sets.edn` set (AR-3: the ceremony it encodes applies regardless of task
class) -- the resulting budget deltas, this session's own measured
actuals: **`:onboarding` 538 to 695, `:corpus` 1519 to 1640, `:sim` 574
to 695, `:judge` 644 to 765, `:docs` 433 to 554** (each non-onboarding
set's delta is exactly `build-session/SKILL.md`'s own 121 lines;
`:onboarding` additionally absorbed this session's own growth to
`.agents/skills/README.md` and `.agents/plans/roadmap.md`, both of
which it already cites). `ehrt.docs-tooling.reading-set-budget-test`
green against the corrected numbers.

**Divergence found, named not resolved.** Writing `session-prompt`
required checking this record's own `[A]`/`[C]` provenance-tag
convention against actual practice: none of the five session prompts
written since this convention's adoption tag any Author-ruling `[A]` or
`[C]`. Not retroactively fixed and not silently dropped from the new
skill -- see notes/ADRs.md ADR-0007's own third dated amendment and
`docs/dev/way-of-working.md`'s matching note, for the full disposition.
Full account: `.agents/session-records/2026-08-02-migration-session-5.md`.

**Correction, same session (C3).** The `:onboarding` figure in the
paragraph above (695) was measured before this same session's own C3
indexed its new session-record and prompt-archive filenames in
`.agents/session-records/README.md` and `.agents/prompts/README.md` --
both already-cited `:onboarding` paths, per the same mechanism migration
session 4's own correction note (above) first caught. Measured actual
after C3: **`:onboarding` 697**. `.agents/reading-sets.edn` carries the
corrected number; the migration report's own "RULED 2026-08-02
(migration session 5)" paragraph still reads 695, left as originally
written (a same-commit fix-forward for the enforced data file, not a
rewrite of already-pushed prose) -- this note is that fix-forward's
own record.

---

