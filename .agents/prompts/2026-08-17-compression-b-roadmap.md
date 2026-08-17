# Session prompt -- compression arc, session B: roadmap re-sectioning
# -- status tokens, slug anchors, the row cap; guard #1 and its dual
# -- ADR-0144

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-
tools workspace. HEAD at handoff: deb9a33 (ADR-0143, compression arc
session A). This session is B. `.agents/plans/roadmap.md` is the
authoritative backlog and is hand-owned intent, so it cannot be
generated the way `notes/ADRs.md` now is; B makes it SMALL and
LINTED instead. Channel probe at deb9a33 (re-derive every figure
here from the tree before using it): 1,685 lines; Next 1,111 lines
across 41 rows of which 13 carry closure words in place; header law
"one line per item" vs 59 rows over six lines; the Now paragraph
cites the latency specimen at `roadmap.md:222` while it sits at :237
-- line-number self-cites rot on every insert. Nothing is deleted:
every moved line lands in a named destination with a dated pointer.

## Read first

1. `.agents/plans/roadmap.md` header (:1-6, its own laws), section
   headings, the Now paragraph, and `.agents/plans/attic/README.md`
   + `roadmap-done-2026-08.md` (the Done rotation as practised).
2. `components/docs-tooling/test/ehrt/docs_tooling/
   roadmap_deferred_closure_lint_test.clj` -- guard #1's narrow
   ancestor; you widen it, you do not write beside it.
3. `notes/adr/0143-adr-index-generated.md` -- the migration-script
   discipline (dry run, numstat proof, nothing-lost census) and the
   `### Index summary (moved verbatim ...)` append pattern; Finding 6.
4. `.agents/skills/build-session/SKILL.md` (R-RH register hygiene,
   R-RP red-push, budget STOP) and `handoff`/`session-prompt` (queue
   provenance -- this session changes what a valid cite LOOKS LIKE).
5. `.agents/rulings.md` "From ADR-0143"; `AGENTS.md` registers section.

## Author rulings, verbatim

- Session B is ordered ("I like that order, after OBR/OBX"); guards
  "Ok on all five." Q1-Q5 for B: "(a) throughout." --
  Q1 a: every row's first token after the bullet is one of `OPEN` |
    `CLOSED <yyyy-mm-dd> <ADR-NNNN|sha>` | `DEFERRED (trigger: ...)`
    | `EXTERNAL`; guard #1 = a `CLOSED` row outside `## Done` is red;
    its dual = closure words (LANDED/CLOSED/FIXED/DONE) in the first
    sentence of a non-CLOSED row are red.
  Q2 a: every row carries a stable slug anchor `**[slug]**` right
    after the token; rows are cited `roadmap.md#slug`; a lint rejects
    `roadmap.md:NNN` line cites in `.agents/**`, `notes/**`, `AGENTS.md`.
  Q3 a: a row is at most 6 lines -- token, slug, one clause of what
    remains and why, an ADR cite; a lint enforces the cap.
  Q4 a: closed rows in Next/Deferred move verbatim to the dated attic
    file (`attic/roadmap-done-2026-08.md`), leaving one Done line
    each; open rows' overflow (chartering history, sequences,
    findings) is appended verbatim to the ADR that owns it under
    `### Roadmap history (moved verbatim from roadmap.md by ADR-0144,
    <date>)`; a row with no owning ADR overflows to the attic file.
  Q5 a: Next rows carry `PRIORITY n` (n = ruled queue order, from
    the ADR-0141 handoff as recorded in `roadmap.md`'s own compression
    charter row); ordered ascending so "what's next" is `head`.
- Tag: `deb9a33` (ADR-0143 close), case (i): channel fresh-clone
  verified; author relay 2026-08-17, run 31990808025 success. Pay
  `stable-20260816-adr-index-generated`.

## Step 0 -- open
- Fresh: `git status --short | wc -l` = 0, tip deb9a33.
- Pay the tag; `git tag --points-at deb9a33` after push.
- Baseline `make test` unpiped, MAKE_EXIT captured; reconcile against
  ADR-0143's 336 blocks / 17,220 passes. `clojure -M:poly check` OK.
- Reading-set actuals vs budgets (R-RH), recorded before you touch
  anything; a set already over budget is a STOP.

## Step 1 -- census (docs-only commit)
Population from the tree, never from this prompt:
1. Every row (top-level `- ` under each `## `): section, first line,
   line count, closure words present, an owning ADR if any (the row's
   own cites), and its current status as you'd token it. Table in
   the ADR. Rows you cannot token confidently are FINDINGS for the
   author, listed with the ambiguity, not guessed.
2. Every `roadmap.md:NNN` cite across `.agents/**`, `notes/**`,
   `AGENTS.md`, `docs/**`, `components/*/docs/**` (scan-root class):
   file:line and which row it meant -- resolved by content, since
   half of them already point at the wrong line.
3. Every roadmap anchor/heading linked from elsewhere (`roadmap.md#`).
4. Slug proposals: one per row, kebab-case, derived from the row's
   bold lead; collisions resolved and listed.
Open ADR-0144. Commit: "docs: ADR-0144 opens -- roadmap row census,
tokens proposed, slug table, line-cite inventory (compression arc,
session B)"

## Step 2 -- red first (tests only)
Widen `roadmap_deferred_closure_lint_test` into `roadmap_lint_test`
(rename with `git mv`; keep the old assertion as one case):
- token: every row's first token is one of the four; CLOSED rows only
  under `## Done`; dual: closure words in the first sentence of a
  non-CLOSED row are red.
- slug: every row has `**[slug]**`; slugs unique; every slug cited
  anywhere in the scan roots resolves.
- cap: no row exceeds 6 lines.
- cites: no `roadmap.md:NNN` in `.agents/**`, `notes/**`, `AGENTS.md`
  (allow the ADR-0144 census table itself by an explicit, dated
  exemption on that ONE file section, disclosed -- or better,
  write the census with slugs and no exemption; your call, say why).
- priority: Next rows carry `PRIORITY n`, n unique, ascending order.
Witness the RED with counts per assertion (guard #1's first
population is the 13-ish closure-word rows -- report the real number).
Commit: "test: red -- roadmap lint: status tokens, slug anchors, six-
line cap, no line-number cites, priority order (guards #1 + dual)"

## Step 3 -- green: migration
1. Script (`bin/roadmap-migrate-0144` or a docs-tooling dev ns; run
   once; committed): for each row -- write token + slug + PRIORITY;
   keep the first sentence and the ADR cite; move the rest verbatim
   to its destination per Q4 with the dated heading; closed rows to
   the attic verbatim, one Done line left in `## Done`. Dry run
   first; numstat proof: lines removed from `roadmap.md` == lines
   added across attic + ADR files, exactly, listed per row in the ADR.
2. Rewrite every `roadmap.md:NNN` cite found in Step 1.2 to
   `roadmap.md#slug` (content-resolved; ADR files are otherwise
   append-only -- a cite rewrite inside an existing ADR sentence is
   the ONE sanctioned in-place edit, each listed in ADR-0144).
3. Header: replace the "one line per item" law with the ruled row
   contract (tokens, slug, cap, priority) in six lines or fewer.
4. Now paragraph -> one row, `OPEN` `**[now]**`, or drop the section
   if empty is the norm (census decides; say why).
Green: lint tests, full `make test` unpiped MAKE_EXIT=0 reconciled
per namespace; oracle IDENTICAL across all 35 roots (no `src` outside
docs-tooling); reading sets re-measured (the roadmap is in which
sets? -- census says; expect drops); ratchet baseline may only move
DOWN in this session.
Commit: "feat: roadmap re-sectioned -- status tokens, slug anchors,
six-line rows, priority order; closed rows to the attic, overflow to
owning ADRs verbatim; roadmap lint widened to the whole file (ADR-0144)"

## Step 4 -- records
ADR-0144: census, findings, before/after (lines per section, rows,
longest row), the moved-line ledger, cite rewrites, budgets. Rulings
"From ADR-0144" (Q1-Q5, tag). `handoff`/`session-prompt` skills:
queue-provenance cite form becomes `roadmap.md#slug` + the row's
token (mirror `.claude/skills`, diff to zero). AGENTS.md registers
section: one line on the row contract. Roadmap: this arc's row
updated (B LANDED; C, D queued -- in the new form). Session record;
prompt archived.
Commit: "docs: ADR-0144 -- compression arc session B: roadmap row
contract, lint, migration ledger; C/D chartered"

## Fences
- src: `components/docs-tooling` only; oracle IDENTICAL.
- ADR files append-only except the enumerated cite rewrites.
- Nothing deleted: numstat ledger balances to zero per row.
- No reading-set budget increases; STOP if a set would exceed.
- Exit codes unpiped, `out/` cleared before fence re-runs, anchored
  edits, diffstat before commit; R-RP: red pushed with its green.
- STOP-AND-REPORT on: a row whose status you cannot token; a cite you
  cannot resolve by content; a numstat ledger that does not balance;
  more than one dated exemption anywhere in the lint.

## Self-archive
Copy verbatim to `.agents/prompts/2026-08-17-compression-b-roadmap.md`
in the Step 4 commit.
