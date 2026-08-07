# 2026-08-07 — Quality riders — driving prompt

## Context

Repo `ehr-testing-tools`, ext4 clone (`~/src/ehr-testing-tools`).
Prompt's own stated HEAD: `9acb79b` (the design channel's own
2026-08-07 pre-session read). Live tree at session start was actually
one commit past that: `74ebc6b` ("Added repo-review skill.") — a
premise mismatch found at preflight and disclosed before any git
action; see this date's own session record and `notes/adr/0076-
quality-riders.md` for the full account and its resolution (ruled:
fix forward now).

## Prompt, verbatim

2026-08-07 — quality riders: the review arc opens — the skill lands, the flake gets its fix, preflight widens its gaze Session prompt (design channel, 2026-08-07; conventions and every claim below read at HEAD `9acb79b` against a fresh public clone). OPENS THE QUALITY-REVIEW ARC — the first run of the `repo-review` skill, ruled by the author (design channel, 2026-08-07: the flaky-test fix rides this arc; a CI-red-duration policy balanced against never blocking on running jobs; a periodic rubric-driven review with assessment, plan, and execution). The skill's SKILL.md was authored by the design channel and HAND-PLACED by the author into BOTH skill homes (`.agents/skills/repo-review/SKILL.md`, `.claude/skills/repo-review/SKILL.md`) — it sits UNTRACKED in the working tree right now; this session's first landing commits it (verify the two copies are byte-identical to each other FIRST; a divergence is STOP-AND-REPORT — the author copied by hand, and hands slip). These placed files are NOT the standing-untracked class (`config/busy-weekday.md` stays untouched as always); they are cargo awaiting their commit. Arc shape after this session: an assessment session runs the skill's steps 1–4 (history scan, probe battery, dated register, scoreboard), the plan comes to the design channel for the author's rulings, and ruled fix sessions follow the standing pattern. R30 ceremony. Read-first: BOTH placed `repo-review/SKILL.md` copies (this session's cargo — read what you are landing); `components/sim/test/ehrt/sim/run_test.clj` `temp-dir-path*` (~270: `createTempFile`/`.delete`/`.mkdirs` with the boolean ignored — the race) and `merge-config-file-suggests-a-same-stem-sibling-file` (~298); `components/sim/src/ehrt/sim/run.clj` `similar-sibling-config` (~199: the `.listFiles` whose nil flows silently to "no suggestion"); ADR-0075 (the flake's discovery, its CI evidence, the two-cause history); both `build-session` `SKILL.md` copies (the Step-0 CI line AR-QR-3 widens); whatever gate enforces skill-mirror currency (fresh-read: confirm it will cover `repo-review/` once committed — if it enumerates from a list rather than globbing, extend the list, disclosed). Author rulings (record verbatim in ADR-0076; `[A]` author-ruled, `[C]` channel-inferred)

1. AR-QR-0 `[A — tag law, case (ii); debt recorded in ADR-0075]`. Annotated `stable-20260807-ci-current` at `9acb79b`, message `ci current landed, design-channel-verified 2026-08-07 (ADR-0075)`; push; verify peeled ref.
2. AR-QR-1 `[A for the skill's adoption (the author placed it); C for landing mechanics]` (the skill lands). Verify the two placed copies byte-identical to each other; commit both in one commit; confirm the mirror gate covers the new pair (extend its enumeration if list-based, disclosed); confirm no other gate (readme-presence or sibling) objects to the new directories — fix the layout to satisfy any gate that does, never the reverse. ADR-0076 records the skill's provenance: authored in the design channel 2026-08-07, from the incident history ADR-0075 crystallized; hand-placed by the author; landed here.
3. AR-QR-2 `[A for the fix mandate ("Add that as a rider on this next arc"); C for the shape, from the design channel's source analysis]` (the flake fix). Three parts: (i) `temp-dir-path*` replaces the delete-then-mkdirs race with `java.nio.file.Files/createTempDirectory` — atomic, throwing, no ignored booleans; every caller adjusted; (ii) `similar-sibling-config` handles `.listFiles` returning nil (an I/O failure, NOT an empty directory): retry once, then return nil WITH the distinction visible at least in a comment — production behavior stays best-effort (a did-you-mean is decoration; its absence must never fail the error path it decorates), but the failure mode is now named in the code instead of absorbed; (iii) the sibling-suggestion test's assertion gains self-diagnosis — on failure its message prints the temp directory's actual contents and the raw `listFiles` result, so a future CI fire carries its own evidence instead of a bare nil. Red-first where red exists locally (the helper's new behavior has unit-testable properties; the RACE itself does not reproduce locally — say so). THE PROOF IS A SOAK, NOT A RUN: ADR-0076 records the fix as hypothesis-with-mechanism; the evidence accumulates across subsequent pushes' CI runs, tracked by the very preflight check AR-QR-3 widens — state the expected fire rate it must beat (the flake fired roughly one push in five to seven, per ADR-0075).
4. AR-QR-3 `[A for the policy need ("not red for long, balanced against not waiting"); C for the shape]` (preflight widens, sessions never wait). Both `build-session` `SKILL.md` copies amend the Step-0 CI line: check the LAST FIVE runs' conclusions on main, not the latest alone (a probabilistic red hides behind any single green); disclose all five; a red anywhere in the five is a finding to report before proceeding. Watch-to-conclusion stays RESERVED for sessions whose own claim is about CI (the AR-CI-4 precedent) — ordinary sessions disclose and proceed, never block on a running job. The roadmap's Externals gains one author-action row: enable GitHub's workflow-failure notification email for this repository (one settings toggle; closes the nobody-watching gap at zero session cost).
5. AR-QR-4 `[C — scope]` (fences). Src edits ONLY in `run_test.clj`'s helper + sibling test and `run.clj`'s `similar-sibling-config`; skill files land verbatim as placed; the build-session amendment is the one Step-0 line in both copies; no assessment work this session (the register and scoreboard are the NEXT session's, run under the skill itself — opening riders do not pre-empt the survey they enable); no other flake-hardening sweeps however tempting (candidates the probe battery will find belong to the register). The oracle bracket must show all TWENTY-SEVEN batches identical. `config/busy-weekday.md` untouched.

Steps Step 0 — Preflight + tag. Cwd ext4; tip `9acb79b` or later-with-disclosure; the placed skill copies verified byte-identical to each other; full suite green baseline; last-five CI conclusions disclosed (the check this session widens, exercised early); oracle pre-digest. Execute AR-QR-0. Step 1 — The skill lands (AR-QR-1). Commit: `docs: the repo-review skill lands in both homes — the lens that rotates (quality riders, AR-QR-1)` Step 2 — The flake fix (AR-QR-2), red-first where red exists. Commit: `fix: the temp dir is atomic, the nil is named, the failure diagnoses itself (quality riders, AR-QR-2)` Step 3 — Preflight widens + the Externals row (AR-QR-3). Mirror gate green. Commit: `docs: preflight reads five runs deep, and the author gets an email (quality riders, AR-QR-3)` Step 4 — ADR-0076 + record. Rulings verbatim; the skill's provenance; the soak plan with its target rate; index line; README count by `ls`; Done pointer `- 2026-08-07 — quality-riders — ADR-0076`. Oracle bracket (`9acb79b` → tip). Successor tag debt IN THE ADR (`stable-20260807-quality-riders` at the closing tip). Session record + prompt self-archive. Final commit: `docs: quality riders record — the review arc opens with its instrument landed (ADR-0076)` Fences Everything AR-QR-4 names. `[C]` rulings conflicting with the live tree fix forward and disclose; `[A]` ones escalate. After landing: design channel verifies by fresh probe — the skill pair byte-diffed against the design channel's own delivered copy, the helper's new shape read, the widened preflight line in both mirrors — then the ASSESSMENT session gets its prompt: the skill's steps 1–4, producing the first dated register and scoreboard, with the plan returning to the design channel for the author's rulings.

## Deviation record

- **Premise mismatch, disclosed at preflight, before any git
  operation.** The prompt's own premise — the `repo-review` `SKILL.md`
  pair sits untracked, this session's own first commit lands it — did
  not hold. The pair was already committed directly to `main`
  (`74ebc6b`), off-ceremony (no session record, no ADR entry), and CI
  at that exact tip was red (`readme-presence-test`: `.agents/skills/
  repo-review` had no `README.md`). Disclosed via `AskUserQuestion`
  before any git action beyond inspection; the author chose "fix
  forward now." AR-QR-1's own text had already ruled the substance of
  the fix in advance ("fix the layout to satisfy any gate that does,
  never the reverse") — only the ceremony-bypass gap itself needed a
  live ruling, which the author gave. Full account: `notes/adr/0076-
  quality-riders.md`, Context section and AR-QR-1 entry.
- **AR-QR-2 carries no local red-first proof.** The prompt itself
  licensed this ("Red-first where red exists locally... the RACE
  itself does not reproduce locally — say so"); this session found
  that neither of the two behavioral changes (the atomic temp-dir
  helper, the `.listFiles` nil-vs-empty distinction) had a
  demonstrable local red state to witness, and said so rather than
  manufacturing one (a permission-based test for the nil path would
  have introduced its own environment-dependent flakiness). See
  ADR-0076's own AR-QR-2 entry.
- **A third, previously unnamed intermittent test failure surfaced
  during Step 3's own verification run** —
  `ehrt.sim-engine.engine-test/every-churned-run-satisfies-the-
  invariant-catalog` — outside anything the prompt named. Confirmed
  intermittent (failed once, passed clean on immediate re-run) and
  unrelated to this session's own touches. Disclosed, named for
  next-arc/review intake per AR-QR-4's own fence against unscoped
  flake-hardening, not fixed this session. Full account: ADR-0076's
  own AR-QR-4 entry and Findings section.
