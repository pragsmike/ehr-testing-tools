# Session: handoff relocation -- .agents/handoffs/ (one live, archive/), restore the handoff skill, notes/tools/ back to frozen (docs-only)

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-10-01-handoff-relocation.md`, which
> records where the live tree differed from this prompt: two files the
> prompt did not list (`.agents/session-records/README.md:37`, the
> 2026-09-29 outpatient-order record's line 11) contradicted it; the
> session halted, and the author ruled "Edit both; add them to commit
> (b) and proceed". Ruling 2's "22-26" is the twelve-name list; the
> condition itself sat at 28-29.

---

Session: handoff relocation -- .agents/handoffs/ (one live, archive/), restore the handoff skill, notes/tools/ back to frozen. DOCS-ONLY. Base `a97059bb` (HEAD == origin/main, clean tree, `bin/preflight`).
HALT on any premise mismatch and report it; do not resolve it yourself. The rulings below are the author's, 2026-10-01, given to the design channel.

READ FIRST (ranges at `a97059bb`):
  AGENTS.md 14-31 (de-scaffold), 250-305 (the discipline-surface map; 300-304 is the frozen-provenance rule)
  .agents/skills/README.md 1-38;  components/docs-tooling/test/ehrt/docs_tooling/readme_presence_test.clj 1-70;  .../skill_mirror_currency_test.clj 1-60
  `git show --stat e189418c -- .agents/skills/handoff .claude/skills/handoff` (the 2026-08-25 deletion: SKILL.md 194 lines + README.md 6, both trees)
  `git show e189418c^:.agents/skills/handoff/SKILL.md` and `...README.md` (the restore source; its "Queue provenance, required" paragraph is lines 78-96)
  `git log --oneline c3a03efd..HEAD -- notes/tools/` (seven commits; `3d1228a2`, `be2701ec`, `9c32a03e` are the three handoff writes into the frozen tree)
  .agents/session-records/2026-10-01-handoff.md 1-77;  notes/tools/agents/handoffs/handoff-2026-10-01.md 1-20, 78-90, 300-310

AUTHOR RULINGS (verbatim):
  1. To the channel's proposal -- "create .agents/handoffs/ (the README-presence gate requires a README.md) and .agents/handoffs/archive/; git mv handoff-2026-10-01.md to the live directory and handoff-2026-09-29.md to the archive, which returns notes/tools/agents/handoffs/ to its merge state; promote the skill to .agents/skills/handoff/ with the .claude/skills/handoff/ mirror; update the path self-references in the 10-01 handoff and record; add an errata line to the 10-01 record and handoff saying ruling 1 was the channel's inference from a commit, not an author ruling" -- the author: "a go". Live count: "b one" -- the skill's own rule, one live handoff, every previous one in archive/.
  2. This restores ONE skill deleted by the 2026-08-25 de-scaffold (`e189418c`); the other eleven stay deleted. `.agents/skills/README.md` 22-26 names the condition ("an author ruling, not a session's"); this is that ruling, dated 2026-10-01.
  3. Frozen provenance (AGENTS.md 300-304): `notes/tools/` is read-only. This session removes exactly what the three handoff commits added there and touches nothing else under it; the July handoffs stay byte-identical.
  4. The archived prompt `.agents/prompts/2026-10-01-handoff.md` is a record and stays verbatim; the correction lives in records, not in it.
  5. The 10-01 record's "ruling 1" was NOT an author ruling: the channel inferred it from commit `3d1228a2` and told the executor the author had committed all three handoffs there (false: the July two arrived with the merge, `c3a03efd`). Say so, in those words, where the record and the handoff state the ruling.
  6. Leak ruling unchanged (no downstream identity, build string, receipt values, config). Line 247 of the 09-29 handoff is OUT of scope: not ruled; the file moves as it is.

STEPS
  1. Restore the skill: write `git show e189418c^:.agents/skills/handoff/SKILL.md` to `.agents/skills/handoff/SKILL.md` and the README likewise; copy both to `.claude/skills/handoff/`. ONE content edit, in the "Queue provenance, required" paragraph: a queue item cites the record, ADR section or roadmap slug that holds it (de-scaffold put findings in records, not rows); the `roadmap.md:NNN` line-cite prohibition stays; add "(adapted 2026-10-01 on restoration; de-scaffold 2026-08-25)". Invariant: no other line differs from `e189418c^`; the skill's `.agents/handoffs/` paths and archive step are kept as written. Gate: `diff <(git show e189418c^:.agents/skills/handoff/SKILL.md) .agents/skills/handoff/SKILL.md` shows only that paragraph; `diff -r .agents/skills/handoff .claude/skills/handoff` is empty; the stale-path scan over `.agents/skills` is green (the restored text cites ADR-0143/0144 and roadmap.md -- check they resolve).
  2. The directory: `.agents/handoffs/README.md` (what lives here; exactly one live `handoff-YYYY-MM-DD.md`; `archive/` holds every superseded one; the skill writes both; the two July handoffs under `notes/tools/agents/handoffs/` are frozen pre-merge provenance, not this directory's history). `git mv notes/tools/agents/handoffs/handoff-2026-10-01.md .agents/handoffs/`; `git mv notes/tools/agents/handoffs/handoff-2026-09-29.md .agents/handoffs/archive/`. Invariant: nothing else under `notes/tools/` changes. Gate: `git diff --stat 3d1228a2^ -- notes/tools/` is EMPTY (the frozen tree is back to its pre-09-29 state); `ls notes/tools/agents/handoffs/` is exactly the 07-25 and 07-26 files.
  3. Self-references and registers. In the moved 10-01 handoff: lines 12-13 and 82 -> ruling 5's correction plus the author's 2026-10-01 ruling (layout per the skill, one live); line 305 -> the archive path. `.agents/skills/README.md`: six skills, a `handoff/` bullet, "eleven others", one sentence on the restoration ruling. AGENTS.md: a `.agents/handoffs/` entry in the map (beside 281) and in the live list (297-299). Invariant: no payload file, no ADR, no roadmap row, no edit to the archived prompt. Gate: `grep -rn 'notes/tools/agents/handoffs' .agents AGENTS.md` returns only the verbatim prompt archive and the two READMEs' frozen-provenance sentences.
  4. Ceremony: record `.agents/session-records/2026-10-01-handoff-relocation.md` and prompt archive `.agents/prompts/2026-10-01-handoff-relocation.md`; append to `.agents/session-records/2026-10-01-handoff.md` an "Errata 2026-10-01" line stating ruling 5 and pointing at the new record; `make docsgen`. Gate: the docs-tooling brick's tests green by the Makefile's own invocation (readme-presence, skill-mirror-currency, index-completeness, state-derived, stale-path, audience-entry-path among them); `git status` names exactly: the four skill files, the handoffs README, the two renames with the 10-01 edits, the skills README, AGENTS.md, the two records, the prompt archive, the generated files.
  5. Close, ASCII messages: (a) `docs(skills): restore handoff from e189418c^ -- author ruling 2026-10-01, one of twelve; mirror included`; (b) `docs: handoffs live in .agents/handoffs/, one live, archive/ for the rest; notes/tools/ back to frozen`; (c) `docs: record the handoff relocation; errata to the 10-01 record (archives prompt)`; then the CI close commit. Docs-only: no local `make test` (AGENTS.md 22). Push; `bin/post-push-verify a97059bb <tip>`; `gh run view <id>` to success. Report the shas, the CI id, and every premise above the tree contradicted.

---

Author ruling mid-session, after the halt (verbatim): "Edit both; add them to commit (b) and proceed".
