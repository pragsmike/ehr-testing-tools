# Archived prompt: compression-d-state (2026-08-17)

**Repo/clone:** the ext4 clone at `/home/mg/src/ehr-testing-tools`
(AR-C-3, never a `/mnt/*` checkout). **HEAD at handoff:** `0b15e87`
(ADR-0146 close). **Ceremony:** R30, autonomous — commit and push at
each checkpoint. **Landed as:** ADR-0147,
`notes/adr/0147-compression-arc-close.md`.

---

## The prompt, verbatim

```
# Session prompt -- compression arc, session D: state.md re-derived,
# the dated indexes generated, arc close -- ADR-0147

## Context
Claude Code under R30 in ehr-testing-tools. HEAD at handoff: 0b15e87
(ADR-0146). Compression arc sessions A-C landed (ADR-0143/0144/0145);
D closes the arc. Channel probe at 0b15e87 (re-derive before use):
`.agents/state.md` 724 lines, 340 of preamble (13 dated update
blocks), nine sections stamped `[V @b96c246]` (ADR-0139 close --
stale by three arc closes), in no reading set; `session-records/
README.md` 223 and `prompts/README.md` 171, both in `:onboarding`,
growing per session. Everything derivable moves to a generated
surface; everything historical moves verbatim to an attic file with a
dated pointer; what remains is hand-owned and small.

## Read first
1. `.agents/state.md` whole (it is the specimen); its header's own
   regeneration contract; `state_staleness_tripwire_test.clj` (what it
   enforces: currency by newest arc-close heading; keep it green).
2. ADR-0143 (generation pattern: docsgen + CI freshness + parity test),
   ADR-0144 (migration script with --dry-run/--verify read-back),
   ADR-0145 (rows, attic'd headers, ratchet). Their `bin/*-migrate-*`.
3. `.agents/session-records/README.md`, `.agents/prompts/README.md`;
   `.agents/reading-sets.edn`, `-baseline.edn`; `AGENTS.md` registers
   section; `.agents/skills/repo-review/SKILL.md:49` (state.md's one
   consumer); `handoff` skill (the design channel reads state.md at
   open -- keep what it needs).
4. `.agents/rulings.md` rows on state (AR-C-1 lineage), on generated
   surfaces, on attic; `.agents/plans/roadmap.md` `**[compression-arc]**`
   row and the D charter text in ADR-0145.

## Author rulings, verbatim
- "C, then UX" done; D next ("go"). Q1-Q5: (a) throughout.
                                             [EDIT IF OTHERWISE]
- Owed from B: (2)(b) -- re-triage the five rows whose token suggests
  another section: each moves to the section its token names, no
  content change, disclosed. `intake-staging-dir` trigger: DEFERRED
  by the author, leave the row as is (lint permits).
- Standing: sessions verify CI via `gh run view <id>`; F-3 narrowed.
- Tag: `0b15e87` (ADR-0146 close) -> `stable-20260817-emitter-author-
  ux` on `gh run view 32041400966` success (record id+conclusion);
  else STOP for the tag only.
- Arc tag at close: `stable-20260817-compression-arc` at D's own tip,
  paid by the NEXT session's Step 0 after this session's own `gh`
  read of its tip run (R30: tags after CI, and this session cannot
  read its own final run to completion) -- OR pay it here if the tip
  run concludes success while you are still open. Say which happened.

## Step 0
Fresh, tip 0b15e87. Tag per license. Baseline `make test` unpiped
MAKE_EXIT captured; reconcile vs ADR-0146's recorded 338 blocks /
3,848 tests / 17,422 assertions (that ADR is the artifact). `poly
check`. Reading sets vs budgets AND baselines. Apply owed 2(b) as one
docs commit (roadmap lint must stay green: PRIORITY uniqueness holds
across sections? -- read the lint; if priorities are Next-only, moved
rows drop or gain them accordingly, disclosed).

## Step 1 -- census (docs-only commit)
1. state.md: every preamble block (date, kind, line span); every
   section: what claim it makes, whether it is DERIVABLE (name the
   source of truth and the gate) or HAND-OWNED, and its currency vs
   the tree at 0b15e87 (spot-probe three claims per section; stale
   claims are findings).
2. The two indexes: convention lines vs per-record rows; consumers
   of each (grep scan roots); whether any consumer needs the rows.
3. `state.md` consumers: `repo-review` skill line, `handoff` skill,
   any test. What each actually reads.
4. Reading-set actuals for `:onboarding` with and without the two
   indexes, and with a projected <=120-line state.md.
Open ADR-0147. Commit: "docs: ADR-0147 opens -- state.md derivability
census, index consumers, onboarding projection (compression arc D)"

## Step 2 -- red first (tests only)
- `state_derived_test`: `.agents/state-derived.md` == docsgen render
  from the live tree (bricks via `poly ws`/dir listing -- match how
  `poly check` counts; oracle roots from `digest.clj`'s list;
  vendored module count from the tree; test namespaces from
  `components/*/test`; reading-set actuals via the same fn the budget
  test uses; standing tags from `git tag -l 'stable-*'`
  -- if git is off-limits in tests, drop tags from the derived file
  and keep them hand-owned, say why); shape assertions; sanity cases.
- `state_residue_test`: `state.md` <= 120 lines; contains the header
  the tripwire needs; contains the pointer table with one row per
  register naming its gate; contains no `[V @...]` claim that
  `state-derived.md` renders (no duplication).
- Index tests: each README <= 40 lines, no per-record rows; the
  generated index (`.agents/session-records/INDEX.md`, `.agents/
  prompts/INDEX.md`, or folded into state-derived -- your call, say
  why) == directory listing render.
- Budget test: `:onboarding` paths updated per Q4; ratchet baseline
  <= previous; assert `state.md` IN and the two READMEs' rows OUT.
Witness RED with counts. Commit: "test: red -- state-derived parity,
state.md residue contract, generated record indexes, onboarding set
reshaped"

## Step 3 -- green: migration
1. `bin/state-migrate-0147` (--dry-run, --verify): preamble blocks
   verbatim -> `.agents/plans/state-history-2026-08.md` under dated
   headings; sections' historical prose (the "since last regeneration"
   narratives) -> same attic file; the Environment section and header
   kept and refreshed to 0b15e87/this ADR; the residue written. --verify
   read-back: every moved block found contiguous at destination.
2. docsgen `write-state-derived!` on `make docsgen`; CI freshness.
3. Index READMEs trimmed to convention (moved rows verbatim to the
   attic file, since the generated INDEX supersedes them but the
   old rows carried hand annotations -- keep them, dated).
4. `reading-sets.edn`/baseline: Q4; ratchet DOWN at actual for
   `:onboarding` (this is the exit condition C could not meet).
5. `repo-review`/`handoff` skills: point at `state.md` + `state-
   derived.md` as appropriate; mirror `.claude/skills`, diff zero.
Green: new tests, tripwire still green, full `make test` unpiped
MAKE_EXIT=0 reconciled per namespace; oracle IDENTICAL 35/35 (no src
outside docs-tooling); docsgen no drift; mirrors zero.
Commit: "feat: state.md re-derived -- 724 -> <=120 hand-owned lines,
countable facts generated into state-derived.md, history attic'd
verbatim, record indexes generated, onboarding set reshaped and
ratcheted down (ADR-0147)"

## Step 4 -- arc close
1. COLD-READ PROBE (Q5): in a fresh sub-agent context if available,
   else by explicit self-walk touching ONLY the `:onboarding` paths,
   answer in writing: tip sha and date; open work in priority order
   (first five); the five rules most likely to bind a build session;
   standing tags; where the event-log contract and its version live.
   Then diff each answer against the tree (`git log -1`, roadmap
   OPEN rows, rulings rows, `git tag`, formats.md). Table in ADR-0147:
   question / cold answer / tree / match. Any mismatch is a finding
   with a fix landed here if it is a pointer, or a row if not.
2. ADR-0147 close: before/after per file and per set for the WHOLE
   arc (A-D), the guards landed (#1-#5 + generation, ratchet, row
   contract, rulings rows), and the arc's own laws as rulings rows:
   "registers are generated where derivable, capped and linted where
   hand-owned, attic'd verbatim where historical"; "no register in a
   reading set without a lint on its growth". Roadmap: compression-arc
   row CLOSED -> Done; successors as OPEN rows if any. AGENTS.md
   registers section: state.md/state-derived.md line. Session record;
   prompt archived. Arc tag per the ruling above.
Commit: "docs: ADR-0147 -- compression arc close: state.md re-derived,
cold-read acceptance, arc laws as rules"

## Fences
src: docs-tooling only; oracle IDENTICAL. Nothing deleted; read-back
is the instrument. Tripwire test stays green at every commit. No
budget rise; `:onboarding` baseline must END lower than 1,665. Exit
codes unpiped; `out/` cleared; anchored edits; R-RP. STOP (F-3
narrowed) on: a state.md claim you cannot classify AND cannot list; a
consumer that needs a row you would drop; a cold-read mismatch that
implies a lint is missing (report it, do not improvise the lint).

## Self-archive
`.agents/prompts/2026-08-17-compression-d-state.md` in Step 4.
```

---

## Deviation record

Every step was executed. Six deviations, each disclosed at the point
it was taken and carried into ADR-0147:

1. **Two of the prompt's `state.md` figures did not hold and were
   corrected rather than adopted** (ADR-0147 finding S-1): the preamble
   is 11 dated blocks, not 13, and 7 sections carry the `[V @b96c246]`
   stamp, not 9.

   **The prompt's BASELINE figure, by contrast, was right, and this
   session's Step-1 reading of it was wrong** (S-5). Step 1 preferred
   ADR-0146's recorded 338 / 3,830 / 17,354 on the reasoning that the ADR
   is the artifact. The clean baseline run measures **338 / 3,848 /
   17,420** at `0b15e87` — the prompt's block and test counts exactly,
   with two assertions unexplained and disclosed. ADR-0146's figure was
   taken mid-session, before its own last commits. Corrected in the ADR
   rather than quietly dropped.

2. **The prompt's read-first list names the `handoff` skill as a
   `state.md` consumer; it is not one** (S-3). `grep -c 'state.md'
   .agents/skills/handoff/SKILL.md` → 0. Nothing was "kept for what it
   needs", because it reads nothing. The `repo-review` skill line is
   the only live skill consumer and it was updated.

3. **The migration moves the WHOLE prior file rather than block-by-block.**
   ADR-0145's script keyed each block to a start line and prefix because
   it fanned blocks out to many destination ADRs; here there is one
   destination, so the strongest read-back available is byte-identity of
   the entire 724-line file as one contiguous run. A block table would
   only verify the blocks the census named — and the census is exactly
   what might be wrong, as S-1 shows.

4. **Step 1's ADR heading deliberately did not yet declare the arc
   close.** `state-staleness-tripwire-test` keys on the newest
   arc-close ADR *by first heading*, so the heading's declaration and
   `state.md`'s matching citation have to flip in one commit or the
   tripwire is red between them. The prompt's fence requires it green at
   every commit, so both flipped together at Step 3.

5. **The first baseline run was discarded.** It was started before any
   edit and was still in flight when later steps' files landed in the
   working tree; it ran the new tests, went red, and aborted at 242
   blocks with `MAKE_EXIT=2`. Recorded as S-6 and replaced by a run in a
   disposable `git worktree` detached at `0b15e87`. No baseline figure
   in ADR-0147 comes from the contaminated run.

6. **A red commit was pushed, and is fixed forward rather than amended**
   (S-7). The Step-3 commit landed `bin/state-migrate-0147` as mode
   100644 — `core.fileMode=false` hides the working-tree bit — so CI run
   `32065822565` at `77f4fba` concluded failure on one assertion,
   `ehrt.cli.executable-bits-test`. The local Step-3 run could not have
   caught it: it started before the script was staged, and an untracked
   file is outside a tracked-files gate's population. Repaired with `git
   update-index --chmod=+x` in the close commit; no amend, no
   force-push. Against `rulings.md#R-ci-watched-not-awaited` this is a
   push that carried a failing test — not knowingly, which is what the
   rule turns on, but the honest record is that it happened and the
   ordering that caused it is now written down.

The `stable-20260817-emitter-author-ux` tag was licensed and paid at
Step 0 (CI run `32041400966`, conclusion success, read via `gh run view`
before tagging; remote peeled ref verified).
