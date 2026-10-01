# Handoff relocation: .agents/handoffs/, the handoff skill restored, notes/tools/ back to frozen (docs-only)

2026-10-01. Base `a97059bb`. Ceremony: R30, taken from the session
prompt (`.agents/prompts/2026-10-01-handoff-relocation.md`), which
stated no prepare-only mode. `bin/preflight` ran first, exit 0, no
findings (HEAD matched origin/main, tree clean, HEAD not tagged
stable-*, disclosed). Docs-only: no source, config or test file
changed; no local `make test` owed (AGENTS.md 22).

## What was done

- **The skill, restored** (author ruling 2026-10-01, one of the twelve
  the de-scaffold `e189418c` deleted; the other eleven stay deleted):
  `.agents/skills/handoff/{SKILL.md,README.md}` from `e189418c^`, and
  the `.claude/skills/handoff/` mirror. One content edit, the "Queue
  provenance, required" paragraph: a queue item cites the record, ADR
  section or roadmap slug that holds it; the `roadmap.md:NNN`
  prohibition stays; marked "adapted 2026-10-01 on restoration;
  de-scaffold 2026-08-25". `diff` against `e189418c^` shows only that
  paragraph (lines 80-82 -> 80-86); `diff -r` of the two trees is
  empty; ADR-0143 and ADR-0144 files and `roadmap.md#downstream-latency`
  resolve.
- **The directory.** `.agents/handoffs/README.md` (one live, `archive/`
  for the rest, the July two named as frozen provenance). `git mv` of
  the 10-01 handoff to `.agents/handoffs/` and the 09-29 one to
  `.agents/handoffs/archive/`. `git diff --stat 3d1228a2^ --
  notes/tools/` is empty; the frozen tree's handoffs directory holds
  exactly the 07-25 and 07-26 files.
- **Self-references.** The 10-01 handoff's two "ruling 1" statements
  carry ruling 5's correction in its own words and the author's
  2026-10-01 ruling; its 09-29 cite points at `archive/`. The 10-01
  record's path is repointed and it gains an "Errata 2026-10-01"
  section. `.agents/skills/README.md` (six skills, eleven deleted, the
  restoration sentence); AGENTS.md (a `.agents/handoffs/` bullet and a
  place in the live list).

## Where the tree differed from the prompt

The session halted before any edit and reported; the author ruled
"Edit both; add them to commit (b) and proceed".

1. **Step 3's grep gate could not pass on the listed files.**
   `.agents/session-records/2026-09-29-outpatient-order-location.md:11`
   cited the then-untracked 09-29 handoff at its frozen-tree path.
   Repointed by ruling, in commit (b).
2. **`.agents/session-records/README.md:37` said "`.agents/handoffs/`
   is deliberately not instantiated here."** Rewritten by ruling to
   point at `../handoffs/`, in commit (b).
3. Ruling 2's "22-26" is the twelve-name list; the condition ("an
   author ruling, not a session's") sat at lines 28-29. Same words;
   no ruling needed.

Also disclosed:

- **The step-3 grep, as landed,** returns the verbatim prompt archives
  (`2026-10-01-handoff.md` 16, 23, 37, and this session's own, which
  quotes its prompt), the handoffs README's
  frozen-provenance sentence, and one line the prompt did not predict:
  the archived 09-29 handoff's line 191, which names the old directory
  in its own Open Questions. Ruling 6 says that file "moves as it is",
  so it was not edited. The skills README carries no such sentence, so
  "the two READMEs" is one.
- **Commit (a) carries the two renames.** They were in the index from
  `git mv` when (a) was staged; `git diff --cached --stat` showed them
  and the commit went ahead anyway. Unpushed, so the message was amended
  (message only, `rulings.md#R-amend-unpushed-message-only`) to disclose
  it; the renames' content edits and the README landed in (b) as
  planned.
- The 10-01 handoff's errata paragraph avoids the literal old path so
  the step-3 grep is not widened by the correction itself.

## Gates

`make docsgen` exit 0, twice (the first finished before this record
existed, so the session-records INDEX had not yet picked it up); `make
state-derived` exit 0 after each later fix. The docs-tooling brick, by
the Makefile's own `TEST_TMP` invocation (`clojure
-J-Djava.io.tmpdir=$PWD/out/test-tmp -M:poly test brick:docs-tooling`),
went red three times before green, each failure this session's own
edit, real output quoted:

1. `FAIL in (record-readmes-stay-convention-only-test)
   (index_completeness_test.clj:170)` -- ".agents/session-records/README.md
   is 41 lines, over its 40-line cap by 1". Commit (b)'s rewrite of
   line 37 added a line; reflowed back to 40.
2. `FAIL in (state-derived-md-matches-a-fresh-render-test)
   (state_derived_test.clj:30)` -- ".agents/state-derived.md is stale";
   the reflow moved the `:onboarding` line count (1428 -> 1429 rendered).
   `make state-derived`.
3. `FAIL in (mirror-has-every-canonical-file-test)
   (skill_mirror_currency_test.clj:49)` -- "README.md differs between
   .agents/skills and .claude/skills". Commit (b) edited the canonical
   skills README and not its mirror; `cp -p` restored parity.

Also caught on the way, by no gate: the `:docs` reading set (AGENTS.md
is one of its five paths) went 784 -> 788 against a budget of 785,
from (b)'s four-line `.agents/handoffs/` bullet. Compacted, not bumped
(`rulings.md#R-budget-stop`): the bullet is one line, and `:docs` now
reads 785 of 785, headroom 0.

Fourth run: exit 0, 98 result lines, 0 failures, 0 errors (readme-presence,
skill-mirror-currency, index-completeness, state-derived, stale-path and
audience-entry-path among them). All three fixes, and the compaction,
are to files commit (b) touched; (b) was unpushed, but a content change
is a new commit (`rulings.md#R-amend-unpushed-message-only`), so they
ride in (c).

## Close

Pushed with the CI close commit; shas, `bin/post-push-verify` and the CI run are recorded there.
