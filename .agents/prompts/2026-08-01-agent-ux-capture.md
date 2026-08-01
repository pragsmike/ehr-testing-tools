# 2026-08-01 — ehr-testing-tools: capture the agent-UX charter (first session under the new ceremony)

Repo: `github.com/pragsmike/ehr-testing-tools`, WSL ext4 clone
(`~/src/ehr-testing-tools`). HEAD at session start: `3423ea5` ("Add
Agent UX Charter plan."), already equal to `origin/main` — no
fast-forward needed. `/mnt/c` clone not touched (all edits made via
the UNC path onto the WSL ext4 clone, per the dual-clone-edit-hazard
discipline). Mode: R30, commit and push at each checkpoint, unattended
— the first session run under R-F's own flipped default, stated
explicitly in this prompt's own Context section, not merely inferred.

## Original prompt (verbatim)

2026-08-01 — ehr-testing-tools: capture the agent-UX charter (first session under the new ceremony)
Context
The agent-UX charter is ruled accepted, including R-F: commit-and-push-at-checkpoints becomes the standing default. It lives at `.agents/plans/2026-08-01-agent-ux-charter.md` (committed). This session captures the charter into repo law — ADR, ceremony amendment, AGENTS.md restructure, AUDIENCES rename, ADR-0001 fix — and is itself the first session of the new regime: it commits and pushes at each checkpoint unattended, and writes its own session record as its last act. One sequencing amendment to the charter's §7, made by the design channel and recorded in the adoption ADR: capture runs first (before the use-cases split, skill adaptation, and migration report), because the ceremony flip and the record ritual should govern every subsequent session, including those.
Heavy migration work is explicitly NOT this session (see AR-7): this session changes law and signage, not residence.
Work in the WSL ext4 clone; fast-forward to `origin/main`, record HEAD. `/mnt/c` untouched.
Read first

1. `.agents/plans/2026-08-01-agent-ux-charter.md` — the whole thing; it is the spec.
2. `AGENTS.md` in full; `AUTHORS-GUIDE.md` §1; `docs/dev/way-of-working.md` §1; `notes/ADRs.md` ADR-0001 and ADR-0007; `.agents/session-records/README.md`.
3. `docs/dev/positioning.md` and every current-tense reference to it (`grep -rn "positioning" docs/ AGENTS.md README.md notes/` — classify historical vs. live before sweeping).
4. The stale-path tripwire implementation (docs-tooling lint) — AR-4 extends it.

Author rulings

* AR-1 Enact R-F. Dated amendment to ADR-0007: R30-mode (commit and push at each checkpoint, unattended) is now the standing default; prepare-only becomes the exception a session's prompt must state. The ceremony's codified safeguards, per the charter: staged scope must match the session's own file list (`git diff --cached --stat` check before every commit), personal-info scan of staged content, commit message written to a file (heredoc hazard through the WSL wrapper), the session record written before the final push, hooks as backstop. AUTHOR ACTION checkpoints remain author-only in every mode. `AUTHORS-GUIDE.md` §1 and `way-of-working.md` §1 get matching dated updates (fix-forward; original text stays, amendment noted in place).
* AR-2 Adoption ADR. New entry in `notes/ADRs.md`: charter adopted, pointer to the plan file, the §7 sequencing amendment recorded, R-F enacted, and the list of charter items this session deliberately does NOT execute (AR-7's fence) named as ruled-but-unexecuted. The charter file itself gains a dated status line ("adopted; capture executed <sha>") — plans stay in `plans/`.
* AR-3 AGENTS.md restructure, per the charter's table: new order — (1) session mode + ceremony, (2) reading-sets pointer (marked "forthcoming: build session"), (3) structure map with `.agents/` routing above the fold, (4) constraints and rules. The "Landed so far" narrative is replaced by one line pointing at the ADR index. Every component/base token the structure-currency test polices must survive exactly (run that test locally before committing this checkpoint — it is the gate most likely to catch a restructure slip). Total length must go down, not up; report before/after line counts.
* AR-4 AUDIENCES rename. `git mv docs/dev/positioning.md docs/dev/AUDIENCES.md`. Add agents as an explicit audience class — a short section (entry points: `AGENTS.md`, `.agents/`; needs: small budgeted surfaces, indexes, deterministic commands, current-truth/archive zoning; the seven human audiences are untouched). Write it plainly and flag it in the session record for author polish — the author reviews post-push under the new regime. Sweep every live reference; historical narrative stays. Extend the stale-path tripwire: `positioning.md` joins the forbidden list for current-tense prose in `docs/**/*.md` — old links must trip; prove red→green both directions per house discipline.
* AR-5 ADR-0001 R1. Check whether the naming clause was already amended (the author may have folded it into the charter commit). If not: dated amendment reconciling the recorded repo name with the actual remote. If so: say so and skip.
* AR-6 The ritual resumes, and this session goes first. Create `.agents/prompts/` with an indexed README (the new home for prompt archives — historical `notes/prompts/` migration is a later build session; its README gains a one-line forward pointer now). Archive this prompt there as the first entry. Write `.agents/session-records/2026-08-01-agent-ux-capture.md` before the final push, and add the gap note: one dated entry (in the session-records README) recording that the 2026-07-30..08-01 sessions' records live in their archived prompts' deviation records and the design-channel conversation, not here — no retroactive fabrication. Index entries land in the same commit as the files they index (R-E, practiced by hand until the gates exist).
* AR-7 Fence. Not this session: register merge, `notes/prompts/` migration, index-completeness/budget/README-presence gates, reading-sets file, skill adaptation, use-cases split, `agent/` dir retirement, memory/plans stub filling. All named in the adoption ADR as pending build work.

Checkpoints (each ends with the full ceremony: scope check → scan → commit via
message file → push)

* C1 — AR-1 + AR-2 + AR-5: the law. Proposed message: `docs: enact agent-ux charter -- R30 ceremony becomes default (ADR-0007 amended), adoption ADR, ADR-0001 naming reconciled`
* C2 — AR-3: the flagship surface. Proposed message: `docs: restructure AGENTS.md -- ceremony and .agents routing above the fold, narrative replaced by ADR index pointer, N->M lines`
* C3 — AR-4: the rename. Proposed message: `docs: positioning.md becomes AUDIENCES.md with agents as an explicit audience class; tripwire forbids the old name`
* C4 — AR-6: the ritual. Proposed message: `docs: session-record ritual resumes -- .agents/prompts created, this session's record and the 07-30..08-01 gap note landed`

Close-out
The session record IS the close-out (that's the point); the chat summary can be three lines and a pointer. Include in the record: HEAD at start and per-checkpoint shas, the AGENTS.md before/after line counts, the tripwire red→green, the AR-5 outcome, and anything the fence stopped.

## Deviation record

**AR-2's `<sha>` placeholder can't be filled literally.** The charter
file's own status line was specified as `"adopted; capture executed
<sha>"` — but the sha of C1's own commit doesn't exist until after C1
is committed, and this file (the charter) is staged and committed
*inside* C1, so no sha can be substituted in before the commit that
would produce it. Resolved by rephrasing to a self-referential form,
"adopted; capture executed by this commit," which is true regardless
of the eventual sha and needs no retrofit. Not a scope change — the
charter still gets its dated status line pointing at the adoption ADR.

**AR-6's "its README gains a one-line forward pointer" read as
ambiguous between `.agents/prompts/README.md` (being created new
anyway, so "gains" would be odd phrasing for a file that doesn't yet
exist) and `notes/prompts/README.md` (which turned out not to exist at
all — checked, not assumed). Resolved: created `notes/prompts/README.md`
new, one paragraph, the forward pointer AR-6 asked for; `.agents/prompts/README.md`
is the fuller indexed README AR-6's first sentence separately asks
for. Both created, distinct purposes, neither is "the" file AR-6 meant
to the exclusion of the other.

**C3's commit message lost two words to a WSL-wrapper quoting hazard,
found live, not previously catalogued.** Backtick-quoted words
(`` `positioning.md` ``) inside a heredoc passed through `wsl -e bash
-lc "..."` get command-substituted by the *outer* shell (the Bash
tool's own double-quoted invocation) before the heredoc ever reaches
WSL — a new, more specific case of the heredoc/quoting hazard
`AUTHORS-GUIDE.md` §1 already names, not previously known to include
backticks specifically. Two occurrences of `positioning.md` inside
backticks in C3's commit message silently became empty command
substitutions; the commit landed with those two spots blank rather
than the intended filename. Not amended (never amend a pushed
commit) — disclosed here, and every message written after this
finding avoided embedding the commit-message heredoc inside a
double-quoted outer command at all, writing the message file via a
non-shell tool instead.

**AR-4's agents-audience-section text is a first draft, flagged for
author polish per the prompt's own instruction, not a verbatim
authored ruling.** The entry-points and needs list follows the
charter's own bullet points closely, but the prose framing (segment 8,
its relationship to segment 4) is this session's own writing.
