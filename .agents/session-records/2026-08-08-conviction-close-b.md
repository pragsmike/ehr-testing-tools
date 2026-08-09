# 2026-08-08 — Conviction arc close, session B (state + rotation + ADR-0089)

## Scope

Session prompt driving the second of two pre-split close sessions for
the conviction arc (ADR-0085–0089), scoped to Steps 2–3 of the standard
close pattern: `state.md` regeneration, reading-set budget
re-derivation, Done-pointer rotation, and the closing ADR itself
(ADR-0089). Session A (`.agents/session-records/2026-08-08-conviction-
close-a.md`) executed Steps 0–1 (the tag verification, the rulings
appends, the dependency-review cadence) and was independently
re-verified before this session's own prompt was authored — the
verification gap the pre-split pattern intends, not an accident of
scheduling.

Preflight: working directory confirmed the ext4 clone
(`~/src/ehr-testing-tools`), `git config core.hooksPath` confirmed
`.githooks`. HEAD `a9c3abf` exactly (conviction close A's own closing
tip), branch up to date with `origin/main`, working tree clean, no
untracked files (`git status --porcelain=v1 --untracked-files=all`,
empty). Last five CI runs on `main` disclosed, all `success`:
`31286768535` (`a9c3abf`), `31286289031`, `31282587609`, `31282341319`,
`31282107053` — no red window. `clojure -M:poly check`: OK. Oracle
pre-digest (`bin/regression-oracle a9c3abf a9c3abf`): all 29 roots
IDENTICAL, byte-for-byte, soundness "yes outside ns form".

## Step 0 — Tag (AR-CB-0)

`stable-20260808-conviction-appends` did not already exist locally or
on the remote. Created ANNOTATED directly (`git tag -a
stable-20260808-conviction-appends a9c3abf -m "..."`) — session A's own
lightweight-tag lesson applied from the start, no correction needed
this time. Pushed; verified: peeled ref resolves exactly to `a9c3abf`,
both locally and via `git ls-remote --tags origin`.

## Step 1 — State + budgets + rotation (AR-CB-1/2/3), commit `0d7140d`

Rotation executed first (roadmap.md Done section replaced with a
sentinel comment deferring ADR-0089's own pointer; the attic gained
ADR-0084's own leftover append to the existing Fidelity-arc section
plus a new Conviction-arc section holding ADR-0086/0087/0088's own
three pointers), then budgets re-derived against the post-rotation
tree, then `state.md` regenerated in full against fresh probes of the
live tree (component/module/gate counts, oracle self-bracket, `gh run
list` for the CI soak, `git log 45eb2f4..HEAD --name-only` for both
the budget diff and the component-graph claim). Full regeneration
table (fifteen rows) and budget disposition table land in
`notes/adr/0089-conviction-arc-close.md`, not repeated here.

**One genuine finding this session's own probing surfaced beyond what
the prompt named:** the prompt's own drift list anticipated only
`:onboarding` moving (the single-item pattern every close since
quality-review has shown); a full `git log 45eb2f4..HEAD --name-only`
diff against every set's own `:paths` found `components/judge/src/
ehrt/judge/interface.clj` also touched (the pairing registry's own
five new re-exports) — `:judge` moved too, the first time since the
quality-review arc's own "all five together" regeneration that more
than one set has moved. Disclosed and re-derived properly (actual 914,
budget 1040→1055), not silently limited to the single set the prompt
named.

Full suite (`clojure -M:poly test :all skip:integration`), run after
all Step 1 edits landed: 283 `Testing ...` namespace announcements (up
from 275), 566 project-block "0 failures, 0 errors" confirmations
(grepped across the entire run output), 0 failures/0 errors, exit code
0. The disclosed `mutate-stdout-stdin-loopback-test` flake did not
fire — no disambiguation re-run needed. `clojure -M:poly check`: OK.
Staging hygiene: `git diff --cached --stat` reviewed, exactly the four
intended files. `gitleaks git --staged -v`: clean.

Committed `0d7140d` ("docs: the conviction arc's state is regenerated
— every claim re-probed at the close (arc close B, AR-CB-1/2/3)").
Pushed; post-push verification (`git log --format=%B -1` diffed
against the source message file): one delta, the known
trailing-blank-line artifact. CI watched to conclusion: run
`31287834460`, `success`, 3m20s.

## Step 2 — ADR-0089 (AR-CB-4), commit `e32fade`

`notes/adr/0089-conviction-arc-close.md` authored: rulings verbatim,
the arc narrative, the pre-split adoption record (with close A's tag
fix-forward named as the pattern's own first observed benefit), the
intake sweep (the oracle blind-spot finding, the three skipped pairing
cells, the cadence NOTE, the two channel errors — see Judgment calls,
below, for the second one's own citation mismatch), the horizon note
verbatim, this close's own mechanical debt (`stable-20260808-
conviction-close`, licensed for the next arc's opener, not created
here). `notes/ADRs.md` gained its index line. `notes/adr/README.md`'s
count corrected 86→87 (`ls notes/adr/*.md | grep -v README | wc -l`,
not arithmetic). `.agents/plans/roadmap.md`'s own sentinel comment
replaced with the real Done pointer (`- 2026-08-08 —
conviction-arc-close — ADR-0089`). `.agents/state.md`'s own header
citation moved from ADR-0084 to ADR-0089 in this same commit — verified
live against the staleness-tripwire's own regex before landing (both
before, citing 0084 = the then-newest arc-close file on disk; and
after, citing 0089 = the newly-created file).

Full suite re-run clean after these edits: 283 namespaces, 566
confirmations, 0/0, no flake. `clojure -M:poly check`: OK. Staging
hygiene: exactly the five intended files. `gitleaks git --staged -v`:
clean.

Committed `e32fade` ("docs: the conviction arc closes — two loops
convicted on evidence, and the close itself splits by design
(ADR-0089)"). Pushed; post-push verification: one delta, the known
trailing-blank-line artifact. CI watched to conclusion: run
`31288276758`, `success`.

## Step 3 — This record and the prompt archive

This file plus `.agents/prompts/2026-08-08-conviction-close-b.md` (the
driving prompt, archived verbatim, with a trailing deviation record)
land together, indexed in both READMEs' own entry lists, in the commit
"docs: session record and prompt archive — conviction arc close B".

## Judgment calls and their ratification status

- **The `:judge` reading-set budget move (Step 1).** Not separately
  ratified — the prompt's own drift list did not name it, and this
  session's own fresh `git log` diff found it independently. Disclosed
  above and in `notes/adr/0089-conviction-arc-close.md`'s own
  regeneration table; no judgment call beyond "follow AR-CB-2's own
  standing rule to wherever the diff actually points."
- **The AR-CB-4 citation mismatch ("AR-A-5 over-literal prompt
  wording").** This session's own driving prompt named a citation tag
  that does not exist anywhere in the conviction arc's committed
  record — verified by a direct grep across every ADR, session record,
  and archived prompt in the arc, finding `AR-A-5` used only as a
  citation of the scaffolding-compaction-A relocation-with-notes law,
  never as an error label. Not silently resolved by inventing a
  matching tag: `notes/adr/0089-conviction-arc-close.md`'s own Intake
  section names the mismatch directly and substitutes the closest
  genuine match (ADR-0087's own AR-CP-2 finding — the colorectal-payoff
  prompt's own under-specified sweep wording, read too literally on a
  first attempt that undercounted a rare branch). This is a disclosed
  correction to the driving prompt's own stated premise
  (`docs/dev/way-of-working.md` §2's own fix-forward-with-disclosure
  pattern), not author-ratified separately — flagged here for the
  author to confirm or correct.
- **State.md's citation tag, `[V @a9c3abf]` not `[V @<Step-1
  commit>]`.** The prompt's own AR-CB-1 text suggested citing the
  Step-1 commit itself; this session cited `a9c3abf` instead — the
  most recent REAL commit on disk when the probes actually ran (session
  A's own closing ceremony commit), matching the established
  precedent's own principle ("cite the tip you actually probed
  against") more precisely than a commit that did not yet exist at
  probe time. Disclosed as the literal reading applied; not separately
  ratified.

## Findings, disclosed not acted

- **The `:judge` budget move** (see Judgment calls, above) — acted on
  (re-derived), not merely disclosed; listed here too because it was
  genuinely unanticipated by the driving prompt.
- No STOP-AND-REPORT fired during regeneration; no re-probe failed
  against work this arc built on.

## Fences held

No `src/`/`test/`/`deps.edn` touch of any kind this session. No law
appends (session A landed the arc's own rulings appends). No roadmap
row content changes beyond the rotation and the ADR-0089 Done pointer.
No horizon ruling made — the horizon note is echoed verbatim, for the
author alone.

## HEAD landed

`e32fade` (Step 2's own commit; this record's own commit lands after,
same push as the prompt archive, per Step 3 of the driving prompt).
