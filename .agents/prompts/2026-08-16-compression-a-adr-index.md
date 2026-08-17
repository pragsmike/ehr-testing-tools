# Session prompt -- compression arc, session A: the ADR index becomes
# generated; guards and the skills rider -- ADR-0143

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-
tools workspace. HEAD at handoff: dc13a17 (ADR-0142). This session's
ADR is ADR-0143. It opens the register-compression arc the author
ordered (A -> B -> C -> D, one session each) after a queue item was
carried as open five days after its arc closed (roadmap.md:222,
"arc CLOSED", sitting at Next). Root causes, tree-verified: the
prior compaction arc's guards (ADR-0045..0047, 2026-08-05) were each
written to one specimen and outgrown by growth -- `notes/ADRs.md`
regrew from one-line rows to 947-char average rows (134 KB; ADR-
0142's own row is 6,541 chars); the reading-set budget was bumped
in place fourteen times; the roadmap closure lint covers Deferred
only. This session lands: (A) the ADR index as a GENERATED surface;
guard #2 (structural, via generation) and guard #3 (budget ratchet);
and the skills rider (guards #4, #5). Guard #1 and its dual ride
session B (their red set is B's population). Fix-forward, no history
rewrite; nothing deleted, everything moved with a dated pointer.

## Read first

1. `notes/adr/0046-scaffolding-compaction-b.md` -- the prior index
   split, its stated one-line-row intent (rows 0043-0048 still are).
2. `notes/ADRs.md` -- header prose (:1-40), the origin-qualification
   convention (frozen-era ADRs), every non-row line: census them.
3. `components/docs-tooling/src/ehrt/docs_tooling/docsgen.clj` --
   `banner`, `write-cli-md!`, the wholly-generated doctrine (DOC-3),
   and how CI's freshness step diffs generated docs (`Makefile`,
   `.github/`, `bin/preflight`).
4. `.agents/reading-sets.edn` header (:1-47, the fourteen-bump
   history) and `reading_set_budget_test.clj`.
5. `.agents/skills/{build-session,handoff,session-prompt}/SKILL.md`
   and the `.claude/skills/` mirror rule (`shared-skill-layout`).
6. `.agents/rulings.md` "From ADR-0141", "From ADR-0142"; the
   ADR-0142 session record's five attention items.

## Author rulings, verbatim

- Compression order: "I like that order, after OBR/OBX." (A -> B -> C
  -> D). Guards: "Ok on all five. Rider ok."
- Q1 (index shape): "a" -- generated from ADR headings + Status by
  docsgen, CI freshness-gated.            [EDIT IF RULED OTHERWISE]
- Q2 (narratives): "a" -- each row's narrative moved verbatim into
  its own ADR file, dated heading.         [EDIT IF RULED OTHERWISE]
- Tag: `dc13a17` (ADR-0142 close), case (i): channel fresh-clone
  verified; author relay 2026-08-16 shows run 31987012257 IN
  PROGRESS. Pay `stable-20260816-result-clinical-time` ONLY if that
  run is success at Step 0; otherwise STOP with the run id.

## Step 0 -- open
- Fresh: `git status --short | wc -l` = 0, tip dc13a17.
- Tag as licensed above; `git tag --points-at dc13a17` after push.
- Baseline `make test` unpiped, MAKE_EXIT captured; reconcile against
  ADR-0142's 334 blocks / 17,176 passes. `clojure -M:poly check` OK.

## Step 1 -- census (docs-only commit)
1. Every ADR file: heading `## ADR-NNNN -- Title` present? Status
   line present and its first token (Accepted/Superseded/...)? Record
   the exceptions (the tree suggests 2 lack Status; find them, do not
   trust the number). Any file whose heading number != filename
   number.
2. `notes/ADRs.md`: every line that is NOT an ADR row (header prose,
   section headings, origin-qualification notes, errata) -- each must
   have a destination (kept as generator preamble text, or moved
   with a pointer). Every ADR row: number, title as written, status
   as written, and whether title/status agree with the ADR file's
   own. Disagreements are findings, listed, not silently reconciled.
3. Inbound links: `grep -rn "ADRs.md#" ` and anchor forms across the
   scan-root class (`docs/**`, `components/*/docs/**`, `.agents/**`,
   `notes/**`, `AGENTS.md`, `README*`) -- anchors that the generated
   file must preserve, enumerated from the tree.
4. Reading sets: measured actual vs budget for every set, today.
Open ADR-0143 with the census. Commit: "docs: ADR-0143 opens --
ADR-index census, inbound anchors, reading-set actuals (compression
arc, session A)"

## Step 2 -- red first (tests only)
- `adr_index_test` (docs-tooling): (i) `notes/ADRs.md` == the
  generator's output for the live `notes/adr/` tree, byte-for-byte;
  (ii) every ADR file has a heading and a Status line in the uniform
  shape; (iii) every inbound anchor from Step 1.3 resolves in the
  generated file. RED against today's index, witnessed.
- Budget ratchet: `reading_set_budget_test` gains the rule that a
  `:budget-lines` value may not EXCEED the value recorded in a new
  committed `.agents/reading-sets-baseline.edn` (one integer per
  set); the failure message says "compact, or bump by compaction ADR
  -- never in a build session". The baseline file is written by THIS
  session at the post-compression measured actuals (Step 3), so this
  assertion is red until then. Witness the red.
Commit: "test: red -- generated ADR index parity and shape; reading-
set budgets ratchet against a committed baseline"

## Step 3 -- green: generator, migration, ratchet, skills
1. `docsgen`: `render-adr-index` / `write-adr-index!` -- banner (do-
   not-edit, what regenerates it), the preserved preamble prose from
   Step 1.2 as generator-owned text (verbatim, moved into the
   renderer's resource or a small hand-owned `notes/adr/INDEX-
   PREAMBLE.md` that the generator inlines -- your call, say why),
   then one row per ADR: `- [ADR-NNNN](adr/file.md) -- Title -- Status`.
   Frozen-era/origin-qualified rows keep their qualification if any
   exist (census decides). Wire into `make docsgen`/CI freshness like
   `cli.md`.
2. Migration, mechanical and scripted (script committed under
   `bin/` or `bin/*-src`, run once, its output diffed): for each ADR
   row, append to the ADR file: `### Index summary (moved verbatim
   from notes/ADRs.md by ADR-0143, 2026-08-16)` + the row's narrative
   text. Rows whose narrative is ALREADY the one-line form (0043-0048
   class) get no section. Zero edits to any pre-existing ADR text.
3. Regenerate `notes/ADRs.md`; add ADR-0143's own row by writing the
   ADR file (the index is now never hand-edited).
4. Ratchet: write `.agents/reading-sets-baseline.edn` at the measured
   actuals AFTER regeneration (the `:onboarding` set includes
   `notes/ADRs.md`? -- census says; if so its actual drops sharply and
   the baseline records the drop). Budgets in `reading-sets.edn`
   re-baselined DOWNWARD to the new actuals plus the header's own
   headroom formula; retire the fourteen-bump comment into the ADR.
5. Skills rider (edit `.agents/skills/*`, mirror `.claude/skills/*`,
   diff the mirrors to zero):
   - `handoff` and `session-prompt`: QUEUE PROVENANCE -- every queued
     item cites its roadmap row `roadmap.md:LINE` and quotes the
     row's status words; an uncited item is a drafting error, and the
     receiving session's Step 0 rejects a prompt whose queue has one.
   - `build-session`: (a) REGISTER HYGIENE AT CLOSE -- the close
     commit moves the session's own closed rows to Done and re-
     measures reading sets; (b) BUDGET STOP -- exceeding a reading-
     set budget is a STOP-AND-REPORT or a same-session compaction,
     never a bump; bumps happen only in a compaction ADR; (c) RED
     PUSH -- a red-first commit is pushed together with its green
     successor, never alone (ADR-0142's practice, now the rule); (d)
     the anchored-edit/diffstat rider from the ADR-0141 handoff.
Green: Step 2 tests, full `make test` unpiped MAKE_EXIT=0, block
count reconciled per namespace; `docsgen` freshness clean; `poly
check` OK; the oracle bracket IDENTICAL across all 35 roots (zero
`src` outside docs-tooling).
Commit: "feat: notes/ADRs.md is generated -- docsgen renders the ADR
index from ADR headings and Status; narratives moved verbatim into
their ADRs; reading-set budgets ratchet against a committed baseline;
queue-provenance and hygiene rules land in the skills (ADR-0143)"

## Step 4 -- records
- ADR-0143: census, findings (title/status disagreements, missing
  Status lines and how each was resolved -- a missing Status line is
  ADDED, "Accepted (status line added ADR-0143, from index)"), before/
  after sizes (bytes, lines) of `notes/ADRs.md` and each reading set,
  the ratchet baseline. Rulings register "From ADR-0143". Roadmap:
  compression arc row (A LANDED; B/C/D queued; guard #1 + dual
  chartered to B, with `roadmap.md:222`'s latency row named as its
  first specimen). Session record; prompt archived.
- STANDING LAW added to `AGENTS.md`'s registers section (one line):
  `notes/ADRs.md` is generated -- edit the ADR, regenerate.
Commit: "docs: ADR-0143 -- compression arc session A: the generated
ADR index, guards #2/#3, the skills rider; B/C/D chartered"

## Fences
- src: `components/docs-tooling` only. No `sim-*`, `corpus-*`,
  emitters, oracle. Oracle IDENTICAL, all 35 roots.
- ADR files: APPEND-only (the moved summary section, and a Status
  line where absent). No existing sentence changes.
- Nothing deleted: every non-row line of today's `notes/ADRs.md` is
  either in the generated preamble or in a named destination.
- Exit codes unpiped, `MAKE_EXIT` captured; `out/` cleared before
  fence re-runs; anchored register edits, diffstat before commit.
- Push at checkpoints (Steps 1, 3+2 together, 4);
  `bin/post-push-verify` after each.
- STOP-AND-REPORT on: any inbound anchor the generated file cannot
  preserve; a title/status disagreement you cannot resolve from the
  ADR's own text; more than 2 ADRs missing a Status line; a reading
  set whose measured actual is ABOVE its current budget before you
  touch anything.

## Self-archive
Copy verbatim to `.agents/prompts/2026-08-16-compression-a-adr-index.md`
in the Step 4 commit.
