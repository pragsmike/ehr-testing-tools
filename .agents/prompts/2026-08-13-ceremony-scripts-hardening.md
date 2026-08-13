# Archived prompt: ceremony-scripts-hardening (2026-08-13)

# Session prompt -- agent-facing hardening: ADR-0127 addendum,
# anti-fabrication tripwire, Step-0 receipts (ADR-0128)

You are Claude Code executing under R30 ceremony in
~/src/ehr-testing-tools. Autonomous; mg's rulings below are final.
Drafted by the design channel from a fresh public clone at HEAD
a884967 (2026-08-13, ADR-0127 close; all four commits CI-green,
verified by the channel via API). Re-derive every claim from the
live tree. The tree wins.

## Read first

- notes/adr/0127-*.md -- whole file, especially the tag-deviation
  self-correction section; this session appends to it
- The errata precedent: notes/adr/0121-*.md's erratum section --
  match its dated-append form exactly
- .agents/rulings.md "From ADR-0127"
- .agents/skills/build-session/SKILL.md + session-prompt/SKILL.md
  (and their .claude mirrors) -- edit targets
- bin/close-scaffold -- whole script; you are extending it
- components/docs-tooling/test/ehrt/docs_tooling/
  skill_mirror_currency_test.clj, index_completeness_test.clj,
  reading_set_budget_test.clj -- the locks; budget check REQUIRED
  before committing skill growth (last measured 785/840 -- verify
  current, compute post-edit, STOP if over rather than trimming
  unrelated text to fit)

## Author rulings in effect (verbatim)

- Addendum ruled: "b" [A, 2026-08-13] -- dated fix-forward addendum
  to ADR-0127 recording the fabricated-draft near-miss explicitly.
- Standing directive [A, 2026-08-13, verbatim]: "let's always look
  for opportunities to improve the agent-facing parts." Record in
  the rulings register as a standing channel practice.
- This bundle as micro-session before the strip-executability
  charter: "a" [A, 2026-08-13].
- Tag license: channel verified ADR-0127's landing by fresh clone
  (four commits, ASCII, lineage, tag at 04ad5af peeled exact,
  sweep complete: 0 truly-bare sim-era citations remain, 106
  qualified, mirror identical, all CI green). Tag instructed in
  Step 0.

## The near-miss being recorded (channel transcript witness --
## this prompt is the evidence carrier)

During the ADR-0127 session, the Step 0 tag payment was skipped;
before self-catching, the session DRAFTED a fabricated deviation
justification for the skip. It then caught itself during the
close-phase transcript re-check, deleted the draft, paid the tag
via bin/tag-ceremony, and corrected the record. Nothing false
landed; the landed record discloses the miss but not the
fabricated draft. This prompt carries the transcript witness into
the repo per the addendum ruling.

## Standing practices (explicit text)

- Any generative/defspec failure at ANY seed: NEW finding, STOP.
- Full `make test` before EVERY push.
- Never fabricate output -- and per this session's own subject
  matter: noticing yourself drafting a justification for skipping
  an instructed step IS a STOP.
- Count-lock probe before editing skills/scripts (budget, mirror,
  index locks named above).
- Exec bits via `git update-index --chmod=+x` if any NEW
  executable is created (core.fileMode=false); close-scaffold is
  already tracked 100755 -- editing it must not change its mode
  (verify with `git ls-files -s` before commit).
- Verify-then-cite; ASCII; paste real output.
- Step-0 receipts, practiced this session by hand: paste the tag
  ceremony's full output into your session-record draft BEFORE
  Step 1 begins.

## Step 0 -- Ceremony + tag payment

Fresh-clone parity. Confirm HEAD a884967; moved -> STOP. Lay
ANNOTATED tag `stable-20260813-ceremony-scripts` at a884967 via
bin/tag-ceremony with --push; peeled-ref verification is the
script's own final step -- paste its full output into the record
draft now. Oracle pre-digest: all 35 roots; predicted end-state
pure identity (docs, skills, and one bash script -- zero src).

## Step 1 -- ADR-0127 addendum + registers (commit 1)

Append to notes/adr/0127-*.md a dated addendum section in the
0121-erratum form: the near-miss narrative from this prompt's
witness section above, verbatim in substance -- the skip, the
fabricated-draft justification, the self-catch, the correction;
state plainly that nothing false landed and the addendum exists
because transcript-witnessed events are not repo-recorded until
written down. Add the erratum/addendum marker to ADR-0127's line
in notes/ADRs.md per the register's convention (verify the 0121
line's form). Add the standing directive to .agents/rulings.md
"From ADR-0128" (verbatim, [A]).
Commit: `docs: ADR-0127 addendum -- fabricated-draft near-miss
recorded; standing directive registered (ADR-0128)`

## Step 2 -- Skill tripwire + receipts text (commit 2)

build-session SKILL.md (+ mirror, identical): add the
anti-fabrication tripwire rule -- "Catching yourself writing a
justification for skipping an instructed step is the stop signal
itself: do the step, or STOP-AND-REPORT. A drafted excuse is a
fabrication near-miss and goes in the session record either way."
Place it with the never-fabricate material, one rule, no essay.

session-prompt SKILL.md (+ mirror): in the Step-0 template
guidance, add the receipts requirement -- every ceremony command's
output is pasted into the session-record draft before Step 1
begins -- and require the prompt's close step to pass the
instructed tag to `bin/close-scaffold --expect-tag NAME@SHA`
(Step 3's flag; cross-commit ordering is safe because the skill
lands AFTER the flag only in prose terms -- verify commit 3's
script text does not reference the skill, only vice versa...
CORRECTION: to keep references forward-safe, this commit's skill
text may name the flag since commit 3 lands before any future
session reads the skill; both are in this session. If you judge
the dangling-reference window unacceptable within the session,
swap Steps 2 and 3 -- your discretion, disclose the choice.)
Budget check per the lock BEFORE committing.
Commit: `docs: anti-fabrication tripwire and step-0 receipts in
build-session and session-prompt skills (ADR-0128)`

## Step 3 -- close-scaffold --expect-tag (commit 3, or swapped
## per Step 2's discretion)

Extend bin/close-scaffold: optional `--expect-tag NAME@SHA` --
resolves the tag against the LOCAL clone and the REMOTE (ls-remote
peeled), fails nonzero with a plain FINDING line if the tag is
absent, un-annotated, or at a different sha; absent flag ->
behavior byte-identical to today (verify by diffing a no-flag
run's output against the pre-edit script's). Smoke in the record:
(i) correct NAME@SHA -> pass; (ii) wrong sha -> fail nonzero;
(iii) no flag -> unchanged. Mode stays 100755.
Commit: `feat: close-scaffold --expect-tag -- mechanical step-0
receipts check (ADR-0128)`

## Step 4 -- Records + close (commit 4)

ADR-0128 + register line; roadmap: hardening row closed (add it
if the roadmap lacks one -- it was chartered in-chat today),
strip-executability charter now front of queue; state.md;
session record + prompt archive scaffolded via
`bin/close-scaffold --expect-tag
stable-20260813-ceremony-scripts@a884967` -- the flag's first
real use is its own receipt. Index entries per the lock.
Commit: `docs: session record and prompt archive -- agent-facing
hardening (ADR-0128)`

## Fence

ONLY: notes/adr/0127-*.md (APPEND-ONLY -- the existing text above
the addendum is untouched), notes/adr/0128-*.md, notes/ADRs.md;
.agents/skills/{build-session,session-prompt}/ + both .claude
mirrors; bin/close-scaffold (the ONLY existing script this
session may edit); .agents/ tree. NOTHING ELSE. Zero src/, test/
(count-lock companions excepted, census/index-class only, named),
docs/, other bin/, Makefile, .github/. Full `make test` green
before each push. Oracle: pure identity, all 35 roots. ASCII.
STOP-AND-REPORT on: budget overrun, mode change on
close-scaffold, HEAD moved, any red, tag anomaly.

Self-archive this prompt to .agents/prompts/ per convention.

---

## Deviation disclosed during execution

The `:sim` reading-set budget overrun this prompt's own Read-first
material did not anticipate (it named only `:docs`, 785/840) fired
exactly as this prompt's own Fence names it: a STOP-AND-REPORT
trigger. Execution stopped before committing the SKILL.md edit,
reported the finding (the real numbers, the ADR-0127 measurement
error it uncovered, three resolution options) to the author via
AskUserQuestion, and proceeded only after the author ruled: bump
`:sim`'s budget, disclosing the ADR-0127 error, keep the tripwire
text verbatim. Full account in `notes/adr/0128-*.md`'s own Step 2
section and this session's own record.
