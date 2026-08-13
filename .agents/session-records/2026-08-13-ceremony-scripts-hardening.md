# 2026-08-13 -- Agent-facing hardening -- ADR-0127 addendum, anti-fabrication tripwire, Step-0 receipts (ADR-0128)

## Scope

Three-part bundle chartered as its own micro-session, ahead of the
strip-executability charter already queued: an ADR-0127 addendum
recording a transcript-witnessed near-miss; an anti-fabrication
tripwire in `build-session/SKILL.md`; Step-0 receipts guidance in
`session-prompt/SKILL.md` plus a mechanical `bin/close-scaffold
--expect-tag` check. Four commits landed; this record is the fourth's
own close-phase companion.

## Ceremony

`bin/preflight` at session start: last five CI runs on `main` all
green (`a884967`, `21114e3`, `227ffaf`, `c214bfb`, `04ad5af`);
edit-root confirmed ext4; tree clean; local HEAD matched
`origin/main` at `a884967`; last `stable-*` tag
`stable-20260813-citation-sweep`, HEAD not yet tagged.

Tag `stable-20260813-ceremony-scripts` created ANNOTATED at `a884967`
via `bin/tag-ceremony ... --push`. Receipts (this session's own first
practiced application of the Step-0-receipts requirement it goes on to
write into `session-prompt/SKILL.md`), pasted before Step 1 began:

```
OK: created annotated tag 'stable-20260813-ceremony-scripts' at a884967aa43cc1f4b7b8ba32524b470d3ce4e525
no leaks found
To github.com:pragsmike/ehr-testing-tools.git
 * [new tag]         stable-20260813-ceremony-scripts -> stable-20260813-ceremony-scripts
OK: pushed refs/tags/stable-20260813-ceremony-scripts
OK: remote peeled ref for 'stable-20260813-ceremony-scripts' is a884967aa43cc1f4b7b8ba32524b470d3ce4e525, matches target exactly
```

## Commit 1 (`22a9759`) -- ADR-0127 addendum + registers

Appended a dated addendum to `notes/adr/0127-*.md` (existing text
untouched, append-only), matching `notes/adr/0121-*.md`'s own erratum
form exactly: the near-miss the driving prompt's own witness section
carried in -- before self-catching the missed Step 0 tag payment, that
session drafted a fabricated deviation justification for the skip,
caught it during the same close-phase transcript re-check that caught
the missed tag, and deleted it before either commit landed. Nothing
false ever landed in this repo. `notes/ADRs.md`'s own ADR-0127 line
gained an inline addendum marker at the point the sentence already
discussed the self-correction, matching ADR-0121's own line-form
exactly. `.agents/rulings.md` gained "From ADR-0128": the standing
directive verbatim, the micro-session sequencing ruling verbatim "a",
the addendum-form ruling verbatim "b".

`make test`: green, 535/0/0. `gitleaks git --staged -v`: clean.
Pushed; `bin/post-push-verify a884967 22a9759`: remote tip matched,
ASCII clean, CI queued/pending (AR-CI-4).

## Commit 2 (`fda0b70`) -- anti-fabrication tripwire + Step-0 receipts

`build-session/SKILL.md` (+ `.claude/` mirror): the tripwire rule
added to the VERIFICATION section, next to its own existing
"making a claim it has not actually verified" material -- one rule:

> **Catching yourself writing a justification for skipping an
> instructed step is the stop signal itself: do the step, or
> STOP-AND-REPORT.** A drafted excuse is a fabrication near-miss and
> goes in the session record either way (ADR-0128).

`session-prompt/SKILL.md` (+ mirror): the Step-0 receipts requirement
added to the Context bullet; the `bin/close-scaffold --expect-tag`
cross-reference added to the Close-out bullet. Both commits land this
same session, so no dangling forward reference persists past this
session's own close -- disclosed rather than swapping Steps 2/3, per
the driving prompt's own explicit discretion clause.

**Budget-lock finding.** `build-session/SKILL.md` is a member of all
five reading sets. The driving prompt named only `:docs` (785/840,
55 lines headroom) as the binding constraint. Verifying current
numbers before editing found the real bottleneck was `:sim`: 1293/1295
(2 lines headroom), not the 1170/1295 ADR-0127's own Step 3 had
recorded -- that measurement was already wrong when written (the same
five paths at commit `21114e3` already summed to 1293; a 123-line
arithmetic error that happened not to trip the gate at the time). A
trial edit measured the tripwire text's real cost at +5 lines
(235 -> 240), which would push `:sim` to 1298/1295 -- a real, measured
overrun, one of the driving prompt's own named STOP-AND-REPORT
triggers. Reverted the trial, stopped, asked the author. **Ruled:**
bump `:sim`'s budget, disclosing the ADR-0127 measurement error, keep
the tripwire text verbatim. `.agents/reading-sets.edn` gained a dated
re-derivation comment (standing formula, actual x1.15 rounded up to
the nearest 5: 1298 x 1.15 = 1492.7 -> 1495; budget moved 1295 -> 1495).
`:onboarding`/`:corpus`/`:judge`/`:docs` all absorbed the same +5-line
growth and stayed within their own budgets, checked individually.

`make test`: green, 535/0/0 (re-run clean after the tripwire text
landed). `gitleaks`: clean. Pushed; `bin/post-push-verify 22a9759
fda0b70`: remote tip matched, ASCII clean, CI queued/pending.

## Commit 3 (`dba20a9`) -- close-scaffold --expect-tag

`bin/close-scaffold` gained an optional `--expect-tag NAME@SHA` flag
(general flag-loop parser, position-independent), checked BEFORE any
scaffolding runs: local resolution (`git rev-parse`/`git cat-file
-t`/`git rev-list`, confirming an ANNOTATED tag -- type `tag`, not
`commit` -- at exactly SHA) and remote resolution (`git ls-remote
--tags`, peeled ref, confirming two lines and a matching peeled sha).
Absent flag: behavior verified byte-identical by diffing a real
no-flag scaffolding run's output against a pre-edit copy of the
script's own output for identical arguments -- empty diff (only
`--help` text differs, documenting the new flag).

Smoke, real invocations:

```
$ bin/close-scaffold --expect-tag stable-20260813-ceremony-scripts@a884967aa43cc1f4b7b8ba32524b470d3ce4e525 2099-01-01 smoke-expect-tag-good "..."
OK: --expect-tag 'stable-20260813-ceremony-scripts' verified locally and on remote at a884967aa43cc1f4b7b8ba32524b470d3ce4e525
CREATED: .agents/session-records/2099-01-01-smoke-expect-tag-good.md
...
exit: 0

$ bin/close-scaffold --expect-tag stable-20260813-ceremony-scripts@0000000000000000000000000000000000000 2099-01-01 smoke-expect-tag-bad "..."
FINDING: --expect-tag 'stable-20260813-ceremony-scripts' resolves locally to a884967aa43cc1f4b7b8ba32524b470d3ce4e525, expected 0000000000000000000000000000000000000
exit: 1

$ bin/close-scaffold --expect-tag stable-20260813-does-not-exist@a884967aa43cc1f4b7b8ba32524b470d3ce4e525 2099-01-01 smoke-expect-tag-absent "..."
FINDING: --expect-tag 'stable-20260813-does-not-exist' not found locally -- Step 0 tag payment missing
exit: 1
```

All three FINDING/absent cases left no scaffold artifact behind
(confirmed via `git status --porcelain`); the pass case's throwaway
artifacts removed and READMEs reverted before commit, `git status
--porcelain` returned to exactly pre-smoke state each time.

Exec bit verified unchanged: `git ls-files -s bin/close-scaffold`
showed `100755` before and after staging (`core.fileMode` is `false`
in this repo, so a content-only edit never changes the recorded mode
on its own).

`make test`: green, 535/0/0. `gitleaks`: clean. Pushed; `bin/
post-push-verify fda0b70 dba20a9`: remote tip matched, ASCII clean,
CI queued/pending.

## Oracle (full session span)

`bin/regression-oracle a884967 dba20a9`: **IDENTICAL**, all 35 roots --
matching the Step 0 pre-digest prediction of pure identity exactly.
Zero `src`/`test` edits anywhere this session.

## Fences honored

Zero edits to `src/`, `test/` (outside the named budget-lock's own
live-data re-derivation, which needed no new fixture), `docs/`, any
other `bin/` script, `Makefile`, `.github/`. `notes/adr/0127-*.md`'s
existing text above the addendum stayed untouched, confirmed by diff
before commit. `bin/close-scaffold` was the only pre-existing script
edited; its mode stayed `100755` throughout.

## Close

`notes/adr/0128-agent-facing-hardening-2.md` landed: context, Step 0
receipts, all three commits' own account, the budget-lock finding and
its resolution, oracle bracket, fences, disposition. `notes/ADRs.md`
gained its index line. `.agents/plans/roadmap.md`: a new "Agent-facing
hardening" row, CLOSED; the strip-executability row annotated
"NOW FRONT OF QUEUE" per the author's own sequencing ruling.
`.agents/state.md` gained a CITATION-ONLY update (not an arc close --
no `state_staleness_tripwire_test.clj` impact). This session record
and its paired prompt archive scaffolded via `bin/close-scaffold
--expect-tag stable-20260813-ceremony-scripts@a884967aa43cc1f4b7b8ba32524b470d3ce4e525`
-- the flag's own first real use, also its own receipt -- and filled
in with real content.
