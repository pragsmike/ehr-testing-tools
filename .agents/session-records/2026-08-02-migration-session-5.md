# 2026-08-02 — Migration session 5: session disciplines become skills

## Scope

Fifth build session of the approved migration
(`.agents/plans/2026-08-01-migration-report.md`). Executed item 5:
distilled the session disciplines proven across the 2026-07-30..08-02
arc — spread over `docs/dev/way-of-working.md`, `AUTHORS-GUIDE.md`, the
amended `notes/ADRs.md` ADR-0007, and eleven session archives — into
five repo-local skills under `.agents/skills/`. Two checkpoints: **C1**
— the five skills themselves, mirrored to `.claude/skills/`, both
indexes updated (`60b9f87`). **C2** — `docs/dev/way-of-working.md`
gains a pointer block and a dated divergence note; `notes/ADRs.md`
ADR-0007 gains a matching third dated amendment; `.agents/reading-
sets.edn` absorbs `build-session` into every set with measured budget
raises; `.agents/plans/roadmap.md` and the migration report are
annotated (item 5 done, item 9 closed on the author's own confirmation)
(`586fb45`). **C3** (this record) — session record and prompt archive,
indexes updated same-commit. Item 14 (use-cases split) remains fenced,
per this session's own AR-5.

Also closed coming into this session: the author's fresh-session probe
(named in this session's own prompt context) confirmed
`.claude/skills/`'s mirror-with-gate reading (ADR-0024) actually works
— `wsl-windows-git-hygiene` visible in a fresh, non-nested Claude Code
session's Skill listing, description intact. That confirmation is an
external observation, not a commit; it's recorded in the roadmap and
migration report as closing item 9's own open question, done coming
into rather than during this session's own work.

## Red→green evidence highlights

Docs-only session; the proof is the suite staying green throughout, not
a red→green cycle. After C1's skill/mirror/index changes:
`readme-presence-test` + `index-completeness-test` +
`skill-mirror-currency-test` — 9 tests, 281 assertions, 0
failures/errors. After C2's way-of-working/ADR/reading-sets/roadmap/
report changes: the full docs-tooling gate family relevant to this
session's own edits (`stale-path-test`, `structure-currency-test`,
`index-completeness-test`, `readme-presence-test`,
`notes-prompts-frozen-test`, `reading-set-budget-test`,
`skill-mirror-currency-test`) — 26 tests, 412 assertions, 0
failures/errors. `clojure -M:poly check`: `OK`, run before both
commits. `reading-set-budget-test` in particular proved the new
`:budget-lines` numbers honest on the very content that produced them,
not asserted.

## Judgment calls and their ratification status

- **`session-prompt` as a fifth skill, beyond the migration report's
  own four-skill scope for item 5.** Directly authorized by this
  session's own prompt (AR-1), not a judgment call — named here only so
  the migration report's own item-5 body (which still says "four new
  skills") reads correctly against what actually landed.
- **`compatibility:` frontmatter field, not `allowed-tools:`.** Matches
  the adapted convention the 2026-08-01 skill-adaptation session found
  in `pragsmike/skills` upstream (`allowed-tools:` collides with a real
  Claude Code SKILL.md field meaning something else) and already
  applied to `repo-adaptation/SKILL.md`. This session is the first to
  apply it to genuinely new (non-adapted) skills — judged consistent
  with the discovered convention, not itself author-ratified for this
  specific application. The other ten skills still carry the stale
  `allowed-tools:` field, a pre-existing inconsistency this session
  did not touch (out of scope).
- **Which of the five skills join which reading set (AR-3).** The
  prompt named `build-session` ("likely everywhere") and `session-prompt`
  (comment-only, design-channel audience) explicitly. This session's
  own judgment extended the reading-sets.edn file's own already-stated
  reasoning for excluding the other ten skills from every task-class
  set ("session-mechanics/meta, not domain-specific") to
  `capture-session`/`extraction-stage`/`errata-sweep` as well, rather
  than being told to per-skill. Recorded as data in the file's own
  updated header comment, not silently applied.
- **The `[A]`/`[C]` provenance-tag divergence: escalated, not
  resolved.** Writing `session-prompt/SKILL.md` required checking
  ADR-0007's own tag convention against real practice; the check found
  it unused in all five session prompts written since adoption,
  including this one's. Per AR-2's own instruction ("escalate" a
  substantive contradiction rather than quietly encode drift as
  practice), this was named in three places — `docs/dev/way-of-working.md`'s
  new §6, `notes/ADRs.md` ADR-0007's own third dated amendment, and the
  migration report's new "RULED 2026-08-02 (migration session 5)"
  block — rather than resolved by retroactively tagging the five past
  prompts or by dropping the convention from the new skill. Not
  author-ratified; the author's own ruling is what's being asked for.
- **Item 9's closure scope.** This session's own opening context said
  the probe "passed... close its roadmap row citing that confirmation."
  Read as authorizing closing item 9 fully — the migration report's own
  open question too, not only the roadmap line — since the report's own
  blocking condition (the probe) is exactly what was confirmed and
  nothing else about item 9 remained undecided. A plausible reading,
  not a verbatim instruction to touch the report; flagged here for
  review.
- **Self-caught mid-session slip, never reached git.** The first
  attempt at inserting this session's own dated note into `notes/ADRs.md`
  ADR-0023's thread used a line number that landed mid-sentence, splitting
  migration session 4's own closing line ("...not an edit to the /
  paragraph above.") around the new block. Caught by re-reading the
  surrounding context immediately after the insert and before staging
  anything; fixed with two more `sed` operations before C2's `git add`.
  Named here per this repo's own house style of disclosing self-caught
  mistakes even when they never reached a commit.

## Findings and HEAD landed

**Skills landed, with line count and provenance:**

| Skill | Lines | Primary provenance |
|---|---|---|
| `build-session` | 121 | `AGENTS.md` ("Session mode and ceremony"), `AUTHORS-GUIDE.md` §1, `docs/dev/way-of-working.md`, `notes/ADRs.md` ADR-0007 (R6, R30, both dated amendments) |
| `capture-session` | 104 | `AUTHORS-GUIDE.md` §3, `notes/ADRs.md` ADR-0007's own provenance-tag convention and two dated amendments, `.agents/session-records/2026-08-01-agent-ux-capture.md` |
| `extraction-stage` | 118 | `notes/ADRs.md` ADR-0008, ADR-0011, ADR-0016, ADR-0017, ADR-0018; `.agents/session-records/2026-07-29-judge-engine-extraction.md` |
| `errata-sweep` | 104 | `.agents/session-records/2026-07-29-sim-sibling-errata-sweep.md`; `components/docs-tooling/test/ehrt/docs_tooling/stale_path_test.clj`'s own documented scope |
| `session-prompt` | 101 | `.agents/plans/2026-08-01-agent-ux-charter.md` §3 R-B; the prompt anatomy observed across `.agents/prompts/2026-08-01-migration-session-1.md` through `2026-08-02-migration-session-4.md` |

Each `SKILL.md`/`README.md` pair also exists byte-identical (content and
executable-bit parity, though none of these five carry executable
assets) under `.claude/skills/`, verified by
`skill-mirror-currency-test`.

**Practice-vs-narrative divergence, disposition.** The `[A]`/`[C]`
provenance-tag convention (ADR-0007) has never actually been used in
any session prompt since its adoption — five prompts checked, zero
tags found. Disposition: named, not silently resolved either direction
— see the three citations above. Author's ruling needed: retroactively
tag the five prompts, retire the convention, or start applying it
going forward from this record on.

**Reading-set budget deltas (AR-3), old → new, this session's own
measured actuals:**

| Set | Old | New | Delta | Cause |
|---|---|---|---|---|
| `:onboarding` | 538 | 697 | +159 | `+build-session/SKILL.md` (121) `+` this session's own growth to `.agents/skills/README.md`, `.agents/plans/roadmap.md`, `.agents/session-records/README.md`, and `.agents/prompts/README.md` (all four already-cited onboarding paths) |
| `:corpus` | 1519 | 1640 | +121 | `+build-session/SKILL.md` only |
| `:sim` | 574 | 695 | +121 | `+build-session/SKILL.md` only |
| `:judge` | 644 | 765 | +121 | `+build-session/SKILL.md` only |
| `:docs` | 433 | 554 | +121 | `+build-session/SKILL.md` only |

Every non-onboarding delta is exactly `build-session/SKILL.md`'s own
121 lines, cross-checked by direct `wc -l` summation against each set's
full path list before writing the new numbers (not inferred from the
old budget alone) — all four matched their pre-session budget exactly
before the addition, confirming no other drift occurred between
migration session 4 and this one.

**The gate caught itself twice this session, same species migration
session 4 first found.** C2 committed `:onboarding` at 695 (correct at
that moment). Writing C3 — indexing this record's own filename and its
paired prompt archive into `.agents/session-records/README.md` and
`.agents/prompts/README.md`, both already-cited `:onboarding` paths —
added 2 more lines, caught by re-measuring before finalizing this
checkpoint rather than assuming C2's number still held.
`.agents/reading-sets.edn` carries the corrected **697** (fixed forward
in C3, not an amendment to C2); `notes/ADRs.md` ADR-0023's own thread
gains a same-session correction paragraph; the migration report's own
"RULED 2026-08-02" block, already pushed with C2, still reads 695 —
left as originally written rather than rewritten, per this repo's own
no-rewrite-pushed-prose discipline (the enforced data file is what must
be correct; the narrative sentence is disclosed as stale-by-2 here
instead).

**HEAD landed:** the commit this record's own checkpoint produces
(`docs: session record and prompt archive -- migration session 5`),
pushed immediately after per R30. Per-checkpoint shas for the two
prior commits: C1 `60b9f87`, C2 `586fb45`. HEAD at session start:
`6e7b277` (already at `origin/main`, no fast-forward needed).

**Post-push message verification, both checkpoints:** C1 and C2 each
showed exactly one delta against their own message file — `git log
--format=%B -1`'s own trailing-newline artifact (2 lines vs. 1),
confirmed at the byte level to be that known formatting behavior, not a
recurrence of the backtick/control-byte-dropping hazard the rider
exists to catch. No fix-forward needed either time.
