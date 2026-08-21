# Archived prompt: committee-skill-mojibake-repair (2026-08-21)

Session prompt -- docs-only repair of the mojibake-transcoded committee
`SKILL.md` in all three tracked copies, plus the C1(a) rewrite of the
2026-07-23 divergence annotations as history.

## Context

Claude Code under R30 in `ehr-testing-tools`, on penny. HEAD at
handoff: `de950fa` (ADR-0162 addendum; tree clean; CI green at tip;
last tag `stable-20260821-patient-simulator-charter` @ `6ce2160`, no
tag owed). The prompt gated its Step 3 on an author ruling ("ONLY IF
the author ruled C1(a)") that the prompt itself did not record; the
session asked in-chat before executing it and the author ruled C1(a)
YES. Push was withheld by the prompt's own instruction -- the author
pushes.

## The prompt, verbatim

You are an executing session (R30 ceremony) in ehr-testing-tools on
penny. Docs-only repair: the committee SKILL.md is mojibake-damaged
(UTF-8 transcode: em/en-dashes -> ??", arrows -> ??', >= -> ???) in all
three tracked copies. Author ruling: repair it. Channel-verified
facts as of de950fa -- re-derive, don't trust.

READ FIRST: AGENTS.md (the `.claude/` section -- the skills mirror is
generated/copied, edited only at .agents/skills/, with
ehrt.docs-tooling.skill-mirror-currency-test failing the build on
drift).

STEP 0 -- receipts. git fetch, ff-only pull, note HEAD. Verify the
damage fingerprint: the three copies
  .agents/skills/committee/SKILL.md
  .claude/skills/committee/SKILL.md
  notes/tools/agents/skills/committee/SKILL.md
are byte-identical to each other (cmp), each with exactly 40 `??`
sequences (31 ??", 7 ??', 2 ???). If they are not byte-identical or
the counts differ, STOP AND REPORT -- the repair plan assumed it.

STEP 1 -- reconstruct .agents/skills/committee/SKILL.md line-wise
from the clean upstream source, pinned:
  curl -fsSL https://raw.githubusercontent.com/pragsmike/skills/f033b321891a3bb70ee388343e2f428505ef4925/skills/cyberneutics/committee/SKILL.md -o /tmp/lib.md
  (confirm: 425 lines, zero `??`)
Then run exactly this algorithm (verified by the design channel to
resolve cleanly; embed as a script, run, delete):
  for each line of the damaged file: if it contains no `??`, keep it
  verbatim; else build a regex from the line with re.escape, then
  ??" -> (em|en dash), ??' -> (arrow|right quote), ??? -> (>=|ellipsis),
  anchored ^...$; find matching lines in /tmp/lib.md. Exactly one
  match -> take the library line. Zero or multiple matches -> STOP AND
  REPORT.
Expected: 0 stops, 0 residual `??`.

STEP 2 -- verify the oracle, then propagate.
  Post-repair, `diff repaired /tmp/lib.md` must show EXACTLY 4
  divergent lines: line 7 (description wording), line 10
  (allowed-tools: vs compatibility:), line 35 (the config step with
  its 2026-07-23 divergence annotation), and a trailing blank line.
  Any other divergence -> STOP AND REPORT.
  Then: install over .agents/skills/committee/SKILL.md; cp to
  .claude/skills/committee/SKILL.md; cp to
  notes/tools/agents/skills/committee/SKILL.md (byte-identity of the
  three, verified in Step 0, licenses identical treatment).
  Re-verify all three: zero `??`, valid UTF-8 (iconv), byte-identical
  to each other.
COMMIT 1:
  docs: repair mojibake-transcoded committee SKILL.md in all three
  copies (.agents canonical, .claude mirror, notes/tools record) --
  40 damaged sequences each (em/en-dashes, arrows, >=) reconstructed
  line-wise from pragsmike/skills@f033b32; ETT-divergent lines
  untouched (4-line residual verified)

STEP 3 -- ONLY IF the author ruled C1(a). In the live copies only
(.agents + regenerated .claude mirror; notes/ untouched), in each of
committee, probe, scenarios SKILL.md, replace the bolded span
"**2026-07-23 divergence from upstream cyberneutics**: ..." through
"...see `AGENTS.md`." with:
  **History**: this repo unified the three skills' config on
  `.agents/cyberneutics-config.yaml` (2026-07-23); the upstream
  skills library adopted the same convention on 2026-08-21
  (pragsmike/skills@88c5bf2), so this is no longer a divergence.
Keep each line's surrounding text (the canonical-path sentence and
"Then append `<topic-slug>/`.") intact. Six files total (three
skills x two trees).
COMMIT 2:
  docs: divergence annotations in committee/probe/scenarios
  rewritten as history -- upstream skills library adopted
  .agents/cyberneutics-config.yaml on 2026-08-21
  (pragsmike/skills@88c5bf2); live copies only, notes/ record
  copies keep their wording

STEP 4 -- gates. Run bin/preflight and `make test` (the mirror
currency test must pass). Docs-only change: no oracle declaration
applies. If preflight demands anything genuinely inapplicable and
blocking, STOP AND REPORT rather than improvise. Do NOT push; the
author pushes.

CLOSE -- self-archive this prompt to .agents/prompts/ per convention
at the START of close, then report: HEAD before/after, commit SHAs,
git show --stat per commit, Step 0 fingerprint output, Step 2 oracle
diff, and make test summary.

(Transcription note: the prompt's own Step 1 mapping and Step 2
oracle used the literal non-ASCII characters they are about; this
archive spells them in ASCII, since `.githooks/commit-msg` and the
repo's ASCII law govern what a session writes back. The characters
themselves are in the repaired file, which is the artifact.)

## Deviation record

1. **Step 3's gate was unrecorded, so the session asked rather than
   assumed.** The prompt said "ONLY IF the author ruled C1(a)" and
   carried no such ruling. Two readings were defensible (the author
   ruled it and the prompt omitted the record; or the author had not
   yet ruled), so this was STOP-AND-ASK, not fix-forward. The author
   ruled C1(a) YES in chat; Step 3 then ran as written.

2. **Step 3's span boundary held literally only for `committee`;
   fixed forward with disclosure.** The prompt described the span as
   running from `**2026-07-23 divergence from upstream cyberneutics**`
   through "...see `AGENTS.md`.", with the canonical-path sentence and
   "Then append `<topic-slug>/`." preserved around it. That shape is
   `committee`'s. In `probe` and `scenarios` the annotation runs to
   end-of-line and swallows the append clause ("...then appends
   `<topic-slug>/`"), so there is no "see `AGENTS.md`." terminator to
   cut at -- `AGENTS.md` is cited parenthetically mid-span instead.
   One reading is defensible (the prompt's stated INVARIANT is what
   survives: canonical-path sentence + History + "Then append
   `<topic-slug>/`."), so this was fix-forward, not a stop
   (`rulings.md#R-stop-only-on-two-defensible-readings`). All three
   lines are now byte-identical apart from nothing -- the same
   sentence in all three skills, which was the point of the rewrite.

3. **The 88c5bf2 claim was re-derived, not trusted.** The prompt's
   "re-derive, don't trust" standing instruction was applied to its
   own load-bearing factual claim: `skills/cyberneutics/{committee,
   probe,scenarios}/SKILL.md` were fetched at `88c5bf2` and all three
   confirmed to read `situations_root` from
   `.agents/cyberneutics-config.yaml`. The claim holds.

4. **The close needed a paired session record, not just the prompt
   archive.** `ehrt.docs-tooling.prompt-record-pairing-test` gates
   both directions: an archived prompt with no same-slug record fails
   the build. `bin/close-scaffold` writes both plus the two generated
   indexes, so the close ran through it -- a third commit the prompt
   did not name, disclosed here and in the record.

5. **No push.** The prompt withheld it explicitly. Three commits sit
   local at `main`; `bin/post-push-verify` is therefore not run, and
   no tag is owed or paid.
