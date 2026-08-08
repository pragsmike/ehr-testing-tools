# 2026-08-08 — Fidelity riders: the arc opens

## Scope

Session prompt naming AR-FR-0 through AR-FR-3, opening the fidelity
arc (the EncounterEnd interpreter design pass named as ADR-0080's own
horizon note). Docs-only: the design brief
(`.agents/plans/2026-08-08-encounterend-design.md`) lands, re-verified
against both the upstream Synthea pin and the live interpreter (no
factual error found), the author's R1-R3 rulings on the brief are
recorded verbatim in `notes/adr/0081-fidelity-riders.md`, the
roadmap's Deferred two-module-blocker row gains a dated pointer, and
two author-added backlog rows land in the Next section. No interpreter
code moved; the fix session this ADR licenses has not run.

Preflight: working directory confirmed the ext4 clone, tip `42cd1e0`
exactly, working tree clean except the untracked design brief (present
at session start — see the prompt archive's own Context note on the
prior stopped session this one is the prerequisite for). Baseline:
`clojure -M:poly check` OK; full suite showed one known, expected
failure (`index-completeness-test`, flagging the untracked brief
missing from `.agents/plans/README.md`'s own index — resolved by
Step 1) alongside 42 other passes in that namespace and green
everywhere else; oracle pre-digest (`bin/regression-oracle 42cd1e0
42cd1e0`) all twenty-seven roots IDENTICAL; last-five CI runs on
`main` all green (`42cd1e0`, `9eb7da9`, `c9c3b3f`, the scheduled
Integration run, `8eeafb2`).

Step 0 (AR-FR-0): annotated and pushed `stable-20260807-quality-close`
at `42cd1e0`, peeled ref verified both locally and via
`git ls-remote`.

Step 1 (`6cb4627`, AR-FR-1/AR-FR-2): staged and committed the brief
(untracked → tracked, unedited), its own index line in
`.agents/plans/README.md`, the roadmap's dated Deferred note, and the
two new Next rows. Full suite green (511 assertions, 0/0) before
committing; pushed; CI watched to conclusion (run `31254445113`,
success, 3m30s); post-push message verified (one delta, the known
trailing-blank-line artifact).

Step 2 (this record): authored `notes/adr/0081-fidelity-riders.md`
directly, appended its own index line to `notes/ADRs.md`, corrected
`notes/adr/README.md`'s own file count (78→79, verified by `ls`),
added the Done pointer, archived this session's own prompt, and wrote
this record.

## Red→green evidence highlights

A docs-only session's proof is the suite staying green, not a
red→green cycle — with one disclosed exception worth naming plainly
rather than smoothing over: the TRUE Step 0 baseline was not fully
green. `index-completeness-test` failed on the untracked design
brief's own absence from `.agents/plans/README.md`'s index (1 failure,
42 other passes, same namespace) — an expected, already-understood
failure (the untracked file was present before this session started),
not a surprise, and resolved by Step 1's own index addition in the
same commit that tracked the file. Every run after Step 1 landed: 511
assertions, 0 failures, 0 errors, matching ADR-0080's own closing
shape exactly.

The headline verification is the two upstream/in-tree fact-checks this
session ran before letting the brief land unedited (per this
workspace's own evidence-over-ruling discipline, [[feedback-
verification-discipline]] in the agent's own memory, not repo text):
a fresh `WebFetch` of `EncounterEnd.process` from the pinned raw
source confirmed all five arms exactly as the brief describes, and a
direct read of `gmf_interpreter.clj` at `42cd1e0` confirmed the two
in-tree defects (the unconditional `:encounter-end` compile at line
1697, the openness-blind `index-of-last-open-encounter` at lines
1207-1209) and the Wave H fold's own openness-tracking precedent
(lines 1936-1959), all at the cited lines. No factual error was found;
the brief lands unchanged.

## Judgment calls and their ratification status

- **The prompt archive does not duplicate Appendix A's full text a
  second time.** The archive's own "Prompt, verbatim" section points
  at `.agents/plans/2026-08-08-encounterend-design.md` (landed
  byte-for-byte by this session's own Step 1) rather than re-pasting
  its 123 lines inline, on the reasoning that the brief is now itself
  a permanent, tracked, citable file — duplicating it risks the two
  copies drifting apart, the opposite of what an archive is for. Every
  prior multi-file Appendix A this repo has archived (e.g. the
  vendoring-arc-close resumption prompt) was archiving a PRIOR
  SESSION'S OWN PROMPT TEXT, not a separately-tracked plan artifact —
  no exact precedent either way. **Not yet ratified** — a departure
  from the letter of "verbatim," disclosed here rather than left
  silent, for the author or a future session to strike if the letter
  matters more than the reasoning.
- **The fixture-relocation Next row's `.gitattributes` citation set
  and the ADR-footnote row's own scope were both independently
  verified against the live tree** (a fresh `grep` of `.gitattributes`
  and of `ADR-[0-9]{4}` across `docs/`) rather than carried forward
  from the design channel's own framing verbatim — the footnote row in
  particular was corrected mid-session (see below) once the live grep
  showed the design channel's implied four-file scope undercounted
  what a real inventory would find. Ratified by this ADR's own text
  (AR-FR-2, `notes/adr/0081-fidelity-riders.md`) — not a separate open
  question.

## Findings and HEAD landed

**One self-correction, disclosed rather than folded in silently:**
this session's own first draft of the ADR-footnote-links roadmap row
named four specific files as if they were the complete scope. A fresh
`grep -rl "ADR-[0-9]\{4\}" docs/` turned up eighteen files, most under
`docs/dev/` — ADR-0010's own audience fork excludes `docs/dev/` from
"user-facing," so the four originally named (`docs/site-profiles.md`,
`docs/judge-calibration.md`, `docs/glossary.md`, `docs/formats.md`)
were in fact the correct user-path subset, but the row as first
drafted implied an inventory had been done when it had not. Corrected
in place before committing: the row now names those four as confirmed
examples, states plainly that the full inventory is the row's own
prerequisite, not yet done, and points at ADR-0010's own three-way
split for the scope rule. No other finding surfaced this session
beyond what the brief itself already discloses.

Commits, in order (this session): `6cb4627` (Step 1, the brief + two
roadmap rows), and this session's own closing records commit (Step 2).
Preceded by the tag action at `42cd1e0` (Step 0).

## Verification

See `notes/adr/0081-fidelity-riders.md`'s own Verification section for
the full account (suite counts, gitleaks, oracle brackets, tag peel,
CI runs) — not restated here.

## Deviations, disclosed

- **A prior "encounterend fix" session had already found this same
  design brief, untracked, and stopped** rather than proceed without
  its own prerequisite capture session — recorded in this workspace's
  agent-memory system (not the repo itself). This session IS that
  prerequisite, run to completion; no repo-level deviation followed
  from the earlier stop (nothing was landed then to reconcile against).
- The prompt-archive duplication judgment call above, restated here
  per this record's own "judgment calls" convention.
