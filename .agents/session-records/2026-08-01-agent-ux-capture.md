# 2026-08-01 — Agent-UX charter capture: R30 default, AGENTS.md restructure, AUDIENCES rename, ritual resumed

## Scope

First session run under the agent-UX charter
(`.agents/plans/2026-08-01-agent-ux-charter.md`, adopted this session
as `notes/ADRs.md` ADR-0023) and the first under R-F's own flipped
default — R30 (commit and push at each checkpoint, unattended) is
enacted as the standing ceremony mode by this same session, which then
runs the rest of itself under it. Four checkpoints, each closed by the
full ceremony (scope check, gitleaks scan, `poly check`, commit via
message file, push): **C1** — ADR-0007 dated amendment ratifying R-F,
matching dated notes in `AUTHORS-GUIDE.md` §1 and
`docs/dev/way-of-working.md` §1, the new adoption ADR-0023 (including
its own recorded §7 sequencing amendment — capture runs first, ahead
of the use-cases split/skill-adaptation/migration-report items — and
the AR-7 fence naming what this session deliberately does not do), and
ADR-0001's R1 naming reconciliation. **C2** — `AGENTS.md` restructured
per the charter's table: mode+ceremony first, a reading-sets pointer
(forthcoming), a compact structure map with `.agents/` routing above
the fold replacing the old 80-line "Landed so far" narrative, then
constraints. **C3** — `docs/dev/positioning.md` → `docs/dev/AUDIENCES.md`,
agents added as an eighth audience segment, every live citation swept,
the stale-path tripwire extended to forbid the old filename. **C4**
(this record) — the session-record ritual resumes: `.agents/prompts/`
created with an indexed README, this session's own prompt archived
there as its first entry, `notes/prompts/README.md` created with a
one-line forward pointer, and the 2026-07-30..08-01 gap named in
`.agents/session-records/README.md` rather than fabricated
retroactively. Heavy migration work (register merge, `notes/prompts/`
migration, the gates themselves, reading-sets file, skill adaptation,
use-cases split) was explicitly out of scope (charter §7, AR-7) — see
"Findings" below for the full fenced list, restated from ADR-0023.

## Red→green evidence highlights

`ehrt.docs-tooling.structure-currency-test` and
`ehrt.docs-tooling.stale-path-test`, run directly (`clojure -M:test
-e ...`, not the full `poly test :project` suite — tests are CI's job
per ADR-0003, `poly check` is the push gate this session actually ran
before every commit): **8 tests, 72 assertions, 0 failures** at C4's
close, up from the pre-session baseline of 8 tests / 70 assertions
(the two new `positioning.md`-forbidden-pattern assertions added at
C3). Two explicit red→green cycles, both reverted after confirming
red, both re-confirmed green:

- **C2 (structure-currency).** Ran clean against the restructured
  `AGENTS.md` on the first pass — every one of the 11 real
  `components/`/`bases/` path tokens (`kernel`, `judge`,
  `judge-v2-hapi`, `judge-fhir-official`, `judge-v2-nist`, `corpus`,
  `corpus-io`, `docs-tooling`, `palgebra`, `sim`, `cli`) survived the
  rewrite as exact backtick tokens; no separate red case was forced
  here since the restructure was designed against the test's own
  presence-check requirement from the start, not discovered after.
- **C3 (stale-path, the new `:retired-positioning-filename` case).** A
  `positioning.md` reference was reintroduced into `docs/what-is-this.md`
  (temporarily, reverted immediately after) — failed citing
  `[:retired-positioning-filename]` exactly as designed; reverted;
  re-ran green. Full transcript in the C3 commit's own message and
  `.agents/prompts/2026-08-01-agent-ux-capture.md`'s deviation record
  is not needed there (this record is the fuller account).

`clojure -M:poly check`: green (`OK`) before every one of the four
commits.

## Judgment calls and their ratification status

- **§7 sequencing amendment (capture runs first).** Stated directly in
  this session's own prompt (Context paragraph) as something "made by
  the design channel," not this session's own call — recorded, not
  ratified-by-this-session, in `notes/ADRs.md` ADR-0023.
- **AR-2's `<sha>` placeholder.** Rephrased to a self-referential form
  ("adopted; capture executed by this commit") rather than a literal
  sha, because the sha doesn't exist before the commit that would
  produce it. Judgment call, not author-specified; flagged in the
  prompt archive's own deviation record for author review.
- **AR-5 outcome: NOT already amended.** Checked `notes/ADRs.md` for
  an existing R1 naming amendment before writing a new one (the prompt
  named this possibility explicitly) — grepped `R1\.`, found only the
  original 2026-07-28 text, no prior amendment. Wrote the dated
  amendment per AR-5's "if not" branch. `git remote -v` confirmed the
  actual remote is `pragsmike/ehr-testing-tools`, not `ehr-testing`.
- **notes/prompts/README.md didn't exist; created new, not just
  amended.** AR-6's "its README gains a one-line forward pointer" was
  ambiguous about which README; resolved by creating both
  `.agents/prompts/README.md` (the fuller indexed one, new home) and
  `notes/prompts/README.md` (the one-line pointer, since no README was
  there to gain anything). Full reasoning in the prompt archive's
  deviation record. **Needs author confirmation this reading was
  correct** — the alternative reading (only `.agents/prompts/README.md`
  needed a forward-pointer sentence, and `notes/prompts/` staying
  README-less was fine) was not chosen but is plausible.
- **AR-4's agents-audience-section prose is a first draft**, per the
  prompt's own instruction ("write it plainly and flag it... the
  author reviews post-push"). Not yet author-ratified.
- **The `.agents/prompts/` and `.agents/session-records/` filename
  slug** (`2026-08-01-agent-ux-capture`) was chosen by this session,
  not specified verbatim in the prompt (which named the session-record
  path exactly but not a prompt-archive path) — paired by date-slug
  per R-A, following the existing `notes/prompts/` naming pattern
  minus its `ehr-testing-` repo-name stutter (this session's own
  editorial judgment, matching `.agents/session-records/`'s existing,
  un-stuttered convention instead).

## Findings and HEAD landed

**Finding: a new, more specific WSL-wrapper quoting hazard.**
Backtick-quoted words inside a commit-message heredoc get
command-substituted by the *outer* Bash-tool shell before reaching WSL
when the heredoc is embedded in a double-quoted `wsl -e bash -lc
"..."` invocation — C3's commit message silently lost two occurrences
of `` `positioning.md` `` this way (both inside backticks). Not
amended (never amend a pushed commit); disclosed in the prompt
archive's deviation record; every subsequent commit message in this
session was written to its file via a non-shell tool instead of a
heredoc inside a double-quoted wrapper command. Worth folding into
`AUTHORS-GUIDE.md` §1's existing heredoc-hazard note in a future
session — not done here, out of this checkpoint's own stated scope.

**The AR-7 fence, restated (full detail in `notes/ADRs.md` ADR-0023):**
this session did not touch the `notes/sim/`/`notes/tools/` register
merge into the live files; did not migrate any of the 28 existing
`notes/prompts/*.md` files; did not build the index-completeness,
reading-set-budget, or per-directory README-presence *gates*
(automated tests) — only the manually-maintained indexes those future
gates will check; did not create `.agents/reading-sets.edn`; did not
run the repo-adaptation skill's three-way diff or the use-cases split;
did not touch `agent/scenario-roster.md`; did not fill
`.agents/memory/`'s or `.agents/plans/`'s remaining stub content beyond
what this record itself adds to `.agents/plans/`'s status line (C1).

**AGENTS.md line count:** 276 → 204 (down 72 lines, 26%).

**HEAD landed:** the commit this record's own checkpoint produces
(`docs: session-record ritual resumes -- .agents/prompts created, this
session's record and the 07-30..08-01 gap note landed`), pushed
immediately after per R30 — the fourth and final checkpoint, same
convention `2026-07-29-storefront-polish.md`'s own record used for the
same self-reference problem (a commit's sha can't be known before it
exists, so a record produced in that same commit cites the commit by
its own message instead). Per-checkpoint shas for the three prior,
already-existing commits: C1 `e868aae`, C2 `3a5ca06`, C3 `163194e`.
