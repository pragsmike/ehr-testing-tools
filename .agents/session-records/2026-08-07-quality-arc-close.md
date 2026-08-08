# 2026-08-07/08 — Quality arc close: the repo examined itself, ruled on what it saw, and fixed the worst of it

## Scope

Session prompt naming AR-QC-0 through AR-QC-6, closing the
quality-review arc (ADR-0075–0080) per the ADR-0074/0068 close pattern.
Single Code session, four checkpoints: Step 0 (preflight + the
`stable-20260807-lint-family` tag), Step 1 (the rulings appends plus
the `libs :outdated` cadence, commit `9eb7da9`), Step 2 (`state.md`
regeneration, all five reading-set budgets, and the Done rotation,
commit `c9c3b3f`), Step 3 (this record: ADR-0080, the index/README/
roadmap updates, the closing tag). Full account, rulings, the
regeneration table, the FINAL disposition tally, and the arc narrative:
`notes/ADRs.md` ADR-0080.

Preflight: working directory confirmed the ext4 clone (`uname -a`,
Linux/WSL2), tip `8eeafb2` exactly, working tree clean. Baseline:
`clojure -M:poly check` OK; full suite green (273 `Testing ...`
namespace announcements, 511 assertions, 0 failures/0 errors — up from
the vendoring arc's own 261, the quality-review arc's own twelve new
namespaces); last-five CI conclusions on `main` all green (`8eeafb2`,
`13cc046`, `9b5c2e1`, `758f3af`, `3684a30`); oracle pre-digest
(`bin/regression-oracle 8eeafb2 8eeafb2`) all twenty-seven roots
IDENTICAL; `gitleaks git --staged -v` clean throughout.

Step 0 tagged `stable-20260807-lint-family` at `8eeafb2`, pushed,
peeled-ref-verified. Step 1 appended four standing rulings to
`.agents/rulings.md` (multi-seed-once-flagged; the `defspec` seed
policy; I/O speaks Result or fails loud; CI is watched, never waited
on) and re-ran `libs :outdated` fresh — unchanged from the vendoring
arc's own report, no new upstream release across the entire
quality-review arc.

Step 2 regenerated `.agents/state.md` in full (fourteen claims
re-probed, a fourteen-row regeneration table in ADR-0080, including two
entirely new sections — the `io` vocabulary law, the review instrument
as standing equipment), re-derived ALL FIVE reading-set budgets for the
first time in one regeneration (the shared `build-session/SKILL.md`'s
own growth this arc touched every set, not just `:onboarding`:
onboarding 1240→1285, corpus 2040→2060, sim 915→970, judge 980→1040,
docs 775→840), and rotated the Done section (ADR-0074's own disclosed
leftover joined the vendoring-arc attic section; ADR-0075–0079 relocated
under a new dated quality-review-arc header, with ADR-0075's own bridge
role noted explicitly). **This step required a deliberate sequencing
choice**, disclosed in both this record and ADR-0080's own execution
account: the newly-built staleness tripwire (`state_staleness_
tripwire_test.clj`, landed by the immediately-prior session, ADR-0079)
checks state.md's own header citation against the newest `*-arc-
close.md` file on disk — since ADR-0080's own file did not yet exist at
Step 2's commit boundary, Step 2 landed every section fresh while
holding the header's own citation sentence at its PRIOR value (ADR-0074,
still the newest file on disk at that instant), simulating the
tripwire's own regex extraction against the drafted commit before
landing it to confirm the gate would stay green, then running the full
suite to confirm the gate itself passed (4/4 assertions). Step 3 then
created ADR-0080's own file and moved the citation to it in the same
commit — closing the sequencing the gate's own logic demanded, rather
than evading it.

Step 3 (this record) authored `notes/adr/0080-quality-arc-close.md`
directly — including the register's FINAL disposition tally,
independently re-derived from the register's own forty-five rows a
second time (the AR-RL-R discipline, applied on principle, matching the
prior corrected 45/28/5 count exactly) — appended its own index line to
`notes/ADRs.md`, corrected `notes/adr/README.md`'s own stale file count
(77→78, verified by `ls`), replaced the live roadmap's own sentinel
HTML comment with the real Done pointer, moved `state.md`'s own header
citation to ADR-0080 (completing the sequencing above), ran the closing
oracle bracket spanning every commit this session made, archived this
session's own prompt, and recorded this session.

## Red→green evidence highlights

A docs-only session's proof is the suite staying green and untouched
throughout, not a red→green cycle — confirmed at every checkpoint: 273
`Testing ...` namespaces, 511 assertions, 0 failures/0 errors,
identical shape to the Step 0 baseline at every commit. The state
tripwire's own live sequencing exercise (above) is this session's own
headline verification beyond the ordinary suite-green pattern: the
gate's own regex-extraction logic was hand-simulated against the drafted
Step 2 commit before landing it, then confirmed green by the actual
gate itself — the first time this gate has governed a real arc-close's
own commit ordering, not merely existed as a possibility.

## Judgment calls and their ratification status

- **The staleness-tripwire sequencing was resolved by simulation before
  landing, not by trial-and-error against a real red.** Rather than
  landing Step 2's commit and discovering whether the gate passed or
  failed, this session extracted the gate's own regex logic and ran it
  against the drafted file content first — confirming `0074` (cited) =
  `0074` (newest-on-disk) before committing. This is a stronger
  discipline than "commit and see," consistent with this repo's own
  red-first-when-possible preference, though the gate itself was also
  run for real afterward (not merely trusted from the simulation).
- **The FINAL disposition tally was re-derived, not copied from
  ADR-0078's own AR-RL-R correction.** This session built its own
  row-by-row count directly from the register's forty-five rows,
  independently, before comparing it to the prior corrected summary —
  the tally happening to match exactly is confirmation, not
  circularity, since the counting method (direct row enumeration) was
  never itself trusted from the prior session's own arithmetic.
- **The GitHub notification-email toggle stayed genuinely unconfirmed,
  disclosed as such rather than assumed either way.** This session
  attempted two API probes (`gh api /repos/.../subscription`, `gh api
  /user`) and found neither exposes the specific personal Actions-
  notification preference the roadmap's own Externals row names — the
  row stays open, not silently closed on the assumption that "probably
  fine" is the same as "confirmed."

## Findings and HEAD landed

No new finding surfaced this session beyond what ADR-0075–0079 already
disclosed and this ADR-0080 restates with fresh counts — this was a
regeneration-and-close session, not a discovery one. The one process
note worth naming: this is the first arc close where every reading-set
budget moved in the same regeneration (a shared skill file's own growth
touching all five task classes at once), rather than the usual pattern
of `:onboarding` alone or `:onboarding` plus one other set.

Commits, in order (this session): `9eb7da9` (Step 1, rulings appends),
`c9c3b3f` (Step 2, state regeneration + budgets + rotation), and this
session's own closing records commit (Step 3).

## Verification

- `bin/regression-oracle 8eeafb2 8eeafb2` (Step 0): all twenty-seven
  vendored-root batches IDENTICAL.
- `bin/regression-oracle 8eeafb2 <this session's own closing commit>`
  (Step 3): all twenty-seven roots IDENTICAL — confirmed docs-only
  across the full session.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline (273 namespaces, 511 assertions, 0/0) and again after
  Step 2's own edits (273/511/0/0, identical shape).
- `clojure -M:poly check`: OK, every step this session.
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks` also ran automatically on every push (pre-push hook),
  clean throughout.
- Post-push message verification, every commit this session: one delta
  each against the message file, the known harmless trailing-blank-line
  artifact.
- Tag verification: `stable-20260807-lint-family` peeled ref resolves
  to `8eeafb2` exactly.
- CI, watched to conclusion at every push (not assumed): `9eb7da9`
  success, `c9c3b3f` success (run `31251728653`), and this session's
  own closing commit's run — see below.

## Deviations, disclosed

- **The state.md header-citation sequencing across Step 2/Step 3 is a
  deliberate two-commit landing, not an oversight.** Documented in full
  in ADR-0080's own "Step 2 — State + budgets + rotation" section and
  restated above — the staleness tripwire's own logic dictated the
  order, and this session let it govern rather than working around it.
