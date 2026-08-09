# 2026-08-09 — Repo review 2: the second assessment

## Scope

Session prompt naming AR-RR2-0 through AR-RR2-5, the author's own
ruling verbatim ("Review 2") opening the repo's second periodic
quality review (`repo-review` skill, `.agents/skills/repo-review/
SKILL.md`). Survey only: steps 1-4 of the skill plus the step-5
mitigation-plan draft — no fix, no roadmap Deferred/Next content edit,
no law append, no state.md regeneration, no src/test/deps touch
anywhere. Findings and the plan land in the register and ADR-0092;
rulings are the author's.

## Step 0 — Preflight + tag (AR-RR2-0)

Working directory confirmed the ext4 clone, HEAD `451d159` exactly
(storefront fixture, ADR-0091), branch up to date with `origin/main`,
working tree clean. Untracked/ignored files disclosed (all standard
build-cache/lock-file noise — `.cpcache`, `target`, `out`,
`.claude/settings.local.json`, none tracked-relevant). `clojure -M:poly
check`: OK. All workflow lanes' latest conclusions disclosed, not just
the push lane (the lane-visibility lesson this window's own history
scan surfaced applied to this session's own ceremony first): `test`
green at `451d159` and the prior ~30 pushes; `Integration` green on
its last two runs (the `c690ec3` hermeticity fix and the routine
scheduled run since) — no open red on either lane. `.github/
workflows/test.yml` and `integration.yml` read in full for D2's own
CI-lane map, ahead of schedule.

Oracle pre-digest (`bin/regression-oracle 451d159 451d159`): all
THIRTY-FOUR roots confirmed IDENTICAL, soundness "yes outside ns
form" — the expected trivial tip-against-itself result, and an
independent cross-check against D1-4's own direct count of
`digest.clj`'s `roots` map (also 34).

Tagged `stable-20260809-storefront-fixture` at `451d159`, annotated,
message "storefront fixture landed, design-channel-verified
2026-08-09 (ADR-0091)"; pushed; peeled ref verified against
`git ls-remote --tags origin`.

## Step 1 — Prior arithmetic + history scan (AR-RR2-2/3)

**AR-RR2-2.** Review 1's own summary arithmetic re-derived directly
from its 8 dimension tables, independent of both the register's
original (wrong) summary line and its own 2026-08-07 fix-forward
correction. This session's own recount reproduced the correction
exactly: 45 disposition-carrying rows (28/9/5/3), 46 total including
the D7-4 pointer row. No further drift found — the correction has now
held across two independent recounts.

**AR-RR2-3.** The history scan (ADR-0081 through ADR-0091, eleven
files, plus their paired session records) was delegated to a dedicated
sub-agent under the same re-derive-never-re-read discipline, instructed
to find and read each of the ten named-minimum incidents at its
PRIMARY source (not from memory or paraphrase) and dimension-classify
every disclosed deviation, not just the named ten. All ten were found,
each with exact commit SHAs and ADR citations, and folded into the
register's own History-scan section (rows H-1 through H-10) plus the
owning D-dimension tables. Two repeat-incident classes confirmed:
the warm-artifact-cache family (ADR-0004's pre-window origin, then
`cd08b20` in-window) and "diagnosis by adjacency is not a diagnosis"
(AR-EE-1c then AR-FP-2, now codified as a standing ruling rather than
left to recur a third time).

## Step 2 — Probe battery (AR-RR2-4/5)

D1 (claim-reality), D3 (environment independence), and D5 (mirror/
derivation drift) run directly by the landing session — matching
review 1's own precedent that single-command-shaped dimensions gain
nothing from delegation. D3's own headline probe (the cold-cache
fresh-clone full suite) was budgeted first as the longest-running
check: a genuine fresh `git clone` into an isolated temp directory,
`HOME` repointed to an empty temp dir (no `~/.m2`/`~/.gitlibs`/
`~/.deps.clj`, confirmed absent before the run) and the artifact cache
directory likewise repointed — `poly check` OK, `poly test :all
skip:integration` 293 namespace blocks / 14183 passes / 0 failures / 0
errors anywhere, 5m24s from cold. This is the `c690ec3` hermeticity
method applied at full-repository scope, this review's own new
standard per AR-RR2-4.

D2 (guard coverage), D4 (error honesty, the headline re-score), D6
(sampling adequacy), D7 (continuity integrity), and D8 (operator
experience) each ran as an independent, parallel, read-only sub-agent.
D4's own probe read AND directly ran the recurrence gate
(`io_vocabulary_lint_test.clj`, `clojure -M:dev:test`, bypassing the
broad suite), ran an independent grep sweep with its own regex, and
live-executed three function-level calls against scratch malformed
input to confirm actual runtime behavior — not merely reading the fix
session's own prose. D8's probe was given live-execution latitude
(scratch output confined to fresh temp dirs) to build and run the CLI
for real. One sub-agent (D8) paused mid-flight waiting on a background
`make quickstart` run rather than reporting; resumed via a follow-up
message instructing it to abandon the hang, treat the kill itself as a
finding, and report immediately — it did, disclosing the kill (~9.8
minutes in, steady progress, no evidence of an actual hang) as D8-8,
an intake row for the next review rather than a silent retry.

Every dimension's own findings, including every clean probe, landed in
the register at `.agents/plans/2026-08-09-repo-review-findings.md`.

## Step 3 — Register + ADR + plan

The register landed: 76 rows across 8 dimensions (57 close-as-fine, 8
fix-session-candidate, 5 ruling-needed, 5 intake, 1 explicitly
non-tallied cross-reference row), the two-column scoreboard, the
History-scan section (AR-RR2-3), the AR-RR2-2 re-derivation. Fresh
`clojure -M:poly check` and a full local `poly test :all
skip:integration` run were both re-confirmed green (293 blocks, 521
final-block passes, 0 failures/0 errors) BEFORE committing — including
`index-completeness-test` — after the register's own new file and the
new `.agents/plans/README.md` index entry landed, learning directly
from review 1's own disclosed Step-2 mistake (a 10-minute CI-red
window from an un-indexed new file, caught only by re-running the
suite after the fact). `notes/adr/0092-repo-review-2.md` landed: the
survey narrative, the D4 verdict in full, the two-column scoreboard,
and the step-5 plan draft — three fix-session clusters each with a
proposed co-landed gate, six rulings-needed each with two options and
a stated recommendation, the deliberately-fine list, the carried-to-
review-3 intake list, and this session's own successor tag debt.
`notes/ADRs.md` gained its index line; `notes/adr/README.md`'s own
file count corrected 89→90 (`ls`-verified); the roadmap's Done section
gained one pointer, no Deferred/Next content touched.

Committed `0daf26c` ("docs: repo review 2 surveyed -- the scoreboard
moves, nothing else does (ADR-0092)"); pushed; message verified
against `origin/main`. `gitleaks git --staged -v` clean; the pre-push
hook ran it again, clean. CI watched to conclusion: `test` lane run
`31320378940`, green, 4m8s.

## Step 4 — Ceremony (this record)

Session record and prompt archive land together, both READMEs
updated, same commit.

## Deviations, disclosed

- **D8's own agent paused rather than reporting.** Disclosed above
  (Step 2) and in the register's own D8-8 row — not silently retried
  or absorbed; the resume-and-report pattern is itself a small,
  reusable lesson for future long-running live-execution probes (don't
  let a sub-agent's own patience for a background command exceed the
  review's own budget for it).
- **No other deviation from the driving prompt.** All five author
  rulings (AR-RR2-0 through AR-RR2-5) executed as named; the fence
  (AR-RR2-1's "nothing moves beyond the register, this ADR, and
  ceremony") held throughout — confirmed by `git status --porcelain`
  clean before the session's first tool call and after every
  live-execution sub-agent's own work (D8's `make quickstart`/`make
  integration`/`play` runs included).

## Findings, disclosed not acted

Every register row is, by this session's own design, disclosed and
not acted — see `.agents/plans/2026-08-09-repo-review-findings.md` and
`notes/adr/0092-repo-review-2.md`'s own plan draft for the full list.
The single most consequential: D4's headline RED (review 1's sole red
dimension) is independently re-verified GREEN — the repo-wide,
demonstrated-live silent-success I/O pattern is genuinely closed, not
merely claimed closed. Set against that: D7 and D8, both clean at
review 1, each regress to yellow on the same underlying shape — a fix
or gate that closed the literal trigger a prior probe hit while
leaving a structurally adjacent, more general trigger of the same
class open (D8-3's permission-denied gap vs. AR-RL-3's missing-path
fix; D7-7/D7-8's horizon-note drift in two more items after review-1's
own D7-6 fix held for exactly one restatement).

## HEAD landed

`0daf26c` (Step 3's own commit — Step 4's own commit lands after this
record, in the same push as the prompt archive).
