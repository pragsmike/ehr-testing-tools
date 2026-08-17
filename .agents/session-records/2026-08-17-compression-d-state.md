# 2026-08-17 -- compression arc session D: state.md re-derived, the two record indexes generated, the arc closed

## Scope

Close the register-compression arc (ADR-0143/0144/0145 → **ADR-0147**) by
taking its last two targets: `.agents/state.md`, the continuity register,
and the two dated record indexes.

Executed as chartered. `.agents/state.md` splits three ways — countable
facts GENERATED into `.agents/state-derived.md` on every `make docsgen`
and diffed by CI; hand-owned judgement kept in a 118-line residue capped
and linted; all 724 prior lines moved VERBATIM to
`.agents/plans/state-history-2026-08.md`. The two READMEs' 291 per-record
rows are generated into sibling `INDEX.md` files no reading set carries;
the READMEs keep the convention, capped at 40 lines. `:onboarding` gains
`state.md` (which was in no set at all) and still ratchets **1,665 →
1,530**, the move ADR-0145 said session C could not make.

Also landed: ADR-0144 F-8's owed section re-triage, three rows moved.

## Red→green evidence

**Red, before any artifact existed** (`out/red-0147.log`): 36 tests / 91
assertions, **9 failures and 8 errors** across four gates — no
`state-derived.md` or `INDEX.md` files (6 errors + 2), `state.md` 724
lines against a 120 cap with `[V @sha]` claims and no pointer table (3),
both READMEs over cap and full of rows (4), `:onboarding` without
`state.md` (1).

**Green:** the eleven affected docs-tooling gates at **94 tests / 649
assertions, 0 failures 0 errors**, then the full suite (below).

**What the red run caught that no one was looking for.** Two real defects
in this session's own oracle-root parser, neither visible to the
live-tree sanity case:

1. the map's first key shares the opening brace's line, so a `^\s+"`
   anchor dropped it — and one root fewer is still non-empty, distinct
   and well-formed, so only the SYNTHETIC fixture went red;
2. fixing that returned **35** where `state.md`'s own `[V]` section
   claims 34. Hand-checked against `digest.clj` lines 545–579 (35 keys)
   and against the prompt's own "oracle IDENTICAL 35/35" fence. The claim
   is stale — `injuries` joined at ADR-0107 and the section was never
   re-probed. Sixth stale claim in the census, and the only one found by
   writing the generator rather than by reading the file;
3. investigating that exposed the scan window: the map's last key closes
   it (`})`), so no line begins with `}` and the terminating `take-while`
   never fired — the scan ran into `-main`'s docstring and returned the
   right answer **by luck**. Brace-balanced now, with a fixture case for
   both shapes.

**Cold-read acceptance probe** (a fresh sub-agent fenced to the ten
`:onboarding` paths, no `git`, no other file): answered Q1/Q2/Q3
correctly, confirmed Q4 is answered *by design* with a command rather
than a census, and failed Q5 — it reached every ADR-0141 rule and the
`:event-schema-version` key but no path, then inferred
`components/provenance` (the schema is in `components/sim-engine`),
correctly flagging the inference. Three pointer-class findings, all fixed
in the close commit.

## Judgment calls and their ratification status

Ratified in ADR-0147; none awaiting the author.

- **The migration moves the WHOLE prior file, not block-by-block.** One
  destination, so the strongest read-back is byte-identity of all 724
  lines as a contiguous run. A block table would only verify blocks the
  census named — and the census is what might be wrong (it was: the
  channel's "13 preamble blocks / 9 stamped sections" are 11 and 7).
- **The tag census is deliberately NOT generated.** A tag is pushed after
  the commit it points at, so a committed count is wrong on arrival and
  CI's freshness diff would fail the next push, blaming a session that
  changed nothing. `state.md` carries the command instead. The cold-read
  probe independently called this the right call.
- **Separate `INDEX.md` files, not folded into `state-derived.md`** — the
  index belongs beside what it indexes, and folding would grow
  `state-derived.md` by one line per session forever, which is the exact
  curve this arc removes from a reading set.
- **Step 1's ADR heading did not yet declare the arc close.** The
  staleness tripwire keys on the newest arc-close ADR by heading, so that
  declaration and `state.md`'s citation must flip in one commit or the
  gate is red between them. Both flipped at Step 3.
- **The Step-0 priority gaps were closed after all.** Leaving gaps at
  7/11/14 was lint-legal and disclosed as minimal churn — and the cold
  reader tripped on it anyway (no `PRIORITY 1` at all, once this session
  closed the top row). Renumbered 1–15 in file order. Disclosure was not
  a substitute for the fix.

## Findings and HEAD landed

Six census findings (S-1..S-6) and three cold-read findings (C-1..C-3),
all in ADR-0147. The ones that outlive this session:

- **S-1:** two of the driving prompt's figures for `state.md` did not
  hold — 11 preamble blocks, not 13; 7 `[V @sha]`-stamped sections, not
  9. Probed, not adopted.
- **S-5, and its own correction.** Step 1 called the prompt's baseline
  figure a channel slip because ADR-0146 records something else. The
  clean run says otherwise: **338 / 3,848 / 17,420** at `0b15e87`, so the
  prompt's block and test counts are exactly right and ADR-0146's
  describes an earlier tree than its own closing tip. Two assertions
  (17,420 vs 17,422) remain unexplained and are disclosed as such. "The
  ADR is the artifact" is a good rule that gave a wrong answer here: an
  ADR's figure measures the tree at the moment it was taken, not the
  commit it ships in.
- **S-3:** the prompt named the `handoff` skill as a `state.md` consumer.
  It contains zero occurrences of `state.md`. Not a consumer.
- **S-4:** `AGENTS.md` did not route to `state.md` at all. Fixed.
- **C-2:** two rules binding every commit — WSL-only git, staging
  hygiene — were `AGENTS.md` prose with no `R-` row, so uncitable from
  the rulings register. Landed as `R-git-from-wsl` and
  `R-staging-hygiene`.
- **S-7, disclosed plainly: this session pushed a red commit.**
  `bin/state-migrate-0147` landed in the Step-3 commit as mode 100644
  (`core.fileMode=false` hides the working-tree bit locally), so CI run
  `32065822565` at `77f4fba` concluded **failure** on exactly one
  assertion — `ehrt.cli.executable-bits-test`, a gate this repo already
  had, whose failure message names the repair command. It did not fire
  in the Step-3 local run because that run STARTED before the script was
  staged, and an untracked file is outside a tracked-files gate's
  population. Fixed forward with `git update-index --chmod=+x` in the
  close commit; no amend, no force-push. The rule it yields is one line
  long: stage first, then run the gate.
- **For review 4, not fixed:** `.agents/memory/README.md` earned no
  citation on any cold-read question. 33 lines, inside budget; a note,
  not a row.

**Tag paid at Step 0:** `stable-20260817-emitter-author-ux` at `0b15e87`,
licensed on CI run `32041400966` (success, verified with `gh run view`
before tagging), pushed and peel-verified.

**HEAD landed:** `9b3432a` (close commit), pushed and post-push-verified
(remote tip match, per-commit ASCII over the range, CI reported once).

**Arc tag PAID IN SESSION**, not handed on: CI run `32068201062` at
`9b3432a` concluded **success** while this session was still open, which
is the branch the prompt's ruling licensed for that case.
`stable-20260817-compression-arc` created annotated via
`bin/tag-ceremony`, pushed, remote peeled ref verified equal to
`9b3432a`.
