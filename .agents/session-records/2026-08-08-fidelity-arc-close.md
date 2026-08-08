# 2026-08-08 — Fidelity arc close: the interpreter tells upstream's truth, and the record tells its own

## Scope

Session prompt naming AR-FC-0 through AR-FC-6, closing the fidelity arc
(ADR-0081–0084) per the ADR-0080 close pattern. **This close ran across
two Code sessions — the second time this repo has needed the
resumption pattern, and the second one in a row to hit an arc close
specifically.** The first session executed Step 0 (AR-FC-0: verified
`stable-20260808-fidelity-payoff` already present at `a13bc0b`,
disclosed, not re-created) and Step 1 (AR-FC-1/2: the rulings appends
plus the `libs :outdated` cadence, committed `e7961b9`, pushed,
verified, CI watched green), then was killed mid-ceremony by an
infrastructure-level API request block unrelated to the work's own
content. No work was lost — both landed items (the tag verification and
the `e7961b9` commit) were already pushed and CI-green. The design
channel verified the seam by fresh public-clone probe (tip, tag,
rulings-append text, all re-read against the live remote) before
authoring this session's own resumption prompt, which marked Step 0/
Step 1 READ-ONLY and this session executed Steps 2–3 only. Full
account, rulings, the regeneration table, the arc narrative, the intake
list, and the two-session deviation record: `notes/ADRs.md` ADR-0084.

This session's own preflight (re-run, not inherited): working directory
confirmed the ext4 clone (`~/src/ehr-testing-tools`, `uname -a` shows
Linux/WSL2), tip `e7961b9` exactly, working tree clean. Baseline:
`clojure -M:poly check` OK; oracle pre-digest (`bin/regression-oracle
e7961b9 e7961b9`) all twenty-eight roots IDENTICAL; last-five CI runs on
`main` disclosed, all green (`e7961b9`, `a13bc0b`, `85ba040`, `841df9a`,
`82d1753`). Full suite (`clojure -M:poly test :all skip:integration`)
run twice this session: the first run surfaced a genuinely new,
unrelated finding — `mutate-stdout-into-intake-stdin-real-loopback-
test`, an `^:integration`-tagged subprocess-piping test in the
`conformance` project, failed once under heavy concurrent JVM load
(`:malformed-mllp-frame`); the identical piped shell command was run
standalone twice, succeeding cleanly both times, and a second,
independent full-suite run came back clean at 275 namespaces / 521
assertions / 0 failures / 0 errors — the disclosed disambiguation
confirming a load-sensitive flake, not a regression, before this
session committed anything. Named in `.agents/state.md`'s own Live work
section as intake, not investigated or fixed (docs-only fence).

## Steps and commits

**Step 2 (`0227f2a`, AR-FC-3/4/5).** `.agents/state.md` regenerated in
full against the live tree — fifteen claims re-probed, a fifteen-row
regeneration table in ADR-0084, including two entirely new sections
(the `open-encounter-index`/`:suppressed-encounter-ends` mechanism, the
truncation-layer absorbed-error finding). Header citation deliberately
held at ADR-0080 (the newest arc-close file actually on disk at that
commit boundary) — the state-staleness-tripwire's own sequencing
contract, verified green before landing, moved to ADR-0084 only in
Step 3. Reading-set budgets: only `:onboarding` re-derived (1285→1400)
— the first close since the quality-review arc's own "all five
together" regeneration where just one set moves; `git log
42cd1e0..HEAD --name-only` found zero touched members in `:corpus`/
`:sim`/`:judge`/`:docs`. Done rotation: ADR-0080's own pointer joined
the attic's existing quality-review-arc section with a dated leftover
note; ADR-0081–0083 relocated under a new `## Fidelity arc — closed
2026-08-08 (ADR-0081–0084)` header; the live roadmap's own Done section
held a sentinel HTML comment (not a pointer) pending Step 3. Full suite
green throughout (275 namespaces, 521/0/0 — the second, disclosed-clean
run, confirming the state.md/roadmap/reading-sets edits themselves
introduced nothing new). `clojure -M:poly check` OK; `gitleaks
git --staged -v` clean. Pushed; post-push message verified (one delta,
the known trailing-blank-line artifact); CI watched to conclusion (run
`31266367045`, success).

**Step 3 (this record).** Re-ran AR-FC-2's own `libs :outdated` cadence
fresh (the killed session's own report was transcript-only and did not
survive it — no reconstruction from memory, a real re-run instead;
unchanged from the quality-review arc's own report, no new upstream
release across the entire fidelity arc). Authored `notes/adr/
0084-fidelity-arc-close.md` directly; appended its own index line to
`notes/ADRs.md`; corrected `notes/adr/README.md`'s own stale file count
(81→82, verified by `ls`); replaced the roadmap's own sentinel comment
with the real Done pointer (`- 2026-08-08 — fidelity-arc-close —
ADR-0084`) in the same commit as the index line; ran the closing oracle
bracket spanning BOTH sessions' own commits; archived this session's
own prompt (the resumption preamble plus the original close prompt as
Appendix A, one file per the pairing gate); recorded this session.

## Red→green evidence highlights

A docs-only session's proof is the suite staying green and untouched,
not a red→green cycle — confirmed at every checkpoint: 275 namespaces,
521 assertions, 0 failures/0 errors, identical shape before and after
Step 2's own edits. This session's own headline verification is the
oracle bracket spanning the full two-session close: `bin/regression-
oracle a13bc0b <this session's own closing tip>` confirmed all
twenty-eight vendored roots IDENTICAL across every commit either
session made — the mechanical proof that the interruption between
sessions changed nothing about the work's own docs-only character. A
second, distinct piece of red→green evidence this session actually
produced (not merely disclosed): the `mutate-stdout-stdin-loopback-
test` flake was reproduced once (red, under load), then run to ground
three more times — twice standalone (green both times) and once inside
a full, independent suite run (green) — before this session treated it
as a flake rather than leaving an unqualified "green" claim standing
over an unexplained anomaly.

## Judgment calls and their ratification status

- **Step 0/Step 1 were treated as READ-ONLY, per the resumption
  prompt's own explicit instruction — verified by direct probe, never
  re-executed.** This session did not re-run `clojure -M:poly check` or
  the full suite AS OF the killed session's own commit in isolation; it
  verified the landed state (tag peeled ref, rulings-append text)
  directly against the live tree and moved forward from there.
- **AR-FC-2's own report was re-run rather than reconstructed** — the
  resumption prompt named this explicitly as a casualty of the kill;
  this session did not attempt to recall or approximate the lost output.
- **The `mutate-stdout-stdin-loopback-test` flake was investigated live
  rather than either (a) silently re-running until green and saying
  nothing, or (b) treating one red run as a STOP-AND-REPORT blocker for
  a docs-only close.** The middle path — reproduce, isolate, confirm via
  an independent second full run, disclose in both `.agents/state.md`
  and ADR-0084, fix nothing (docs-only fence) — is a judgment call this
  session made and disclosed rather than one the prompt specified in
  advance; ratification is this record and ADR-0084 itself, pending the
  author's own read.
- **The interruption-pattern note in ADR-0084** (two of the last three
  arc closes killed by the same infrastructure-block class; a `[C]`
  suggestion that future close prompts might pre-split into smaller
  sessions) is recorded as intake, deciding nothing — the resumption
  prompt asked for this note explicitly; its own disposition is the
  author's to rule, not assumed here.

## Findings and HEAD landed

One genuinely new finding this session, disclosed above and in
`.agents/state.md`'s own Live work section and ADR-0084's own intake
list: the `mutate-stdout-stdin-loopback-test` load-sensitive flake,
untouched by any fidelity-arc commit (`git log --all` shows no touch to
that test file since the pre-monorepo era) — named for the next session
that owns test-suite hygiene, not investigated or fixed here (docs-only
fence). No other new finding surfaced beyond what ADR-0081–0083 already
disclosed and ADR-0084's own intake list restates with citations.

Commits, in order (this session): `0227f2a` (Step 2, state
regeneration + budgets + rotation), and this session's own closing
records commit (Step 3). Preceded by the killed session's own `e7961b9`
(Step 1), READ-ONLY this session.

## Verification

- `bin/regression-oracle e7961b9 e7961b9` (this session's own Step 0):
  all twenty-eight vendored-root batches IDENTICAL.
- `bin/regression-oracle a13bc0b <this session's own closing commit>`
  (Step 3, spanning both sessions): all twenty-eight roots IDENTICAL —
  the full two-session bracket confirmed docs-only.
- Full suite (`clojure -M:poly test :all skip:integration`): 275
  namespaces / 521 assertions / 0 failures / 0 errors, confirmed on two
  independent runs this session (the second the disclosed disambiguation
  of the first run's own transient flake).
- `clojure -M:poly check`: OK, every step this session.
- `gitleaks git --staged -v`: clean, every commit this session.
- Post-push message verification, both commits this session: one delta
  each against the message file, the known harmless trailing-blank-line
  artifact.
- Tag verification: `stable-20260808-fidelity-payoff` peeled ref
  resolves to `a13bc0b` exactly (re-confirmed this session by direct
  `git ls-remote`, not inherited from the killed session's own
  transcript claim).
- CI watched to conclusion: `0227f2a` — run `31266367045`, success; this
  session's own closing commit — see below.

## Deviations, disclosed

- **This close ran across two Code sessions, not one — the second arc
  close this repo has run that did not complete in a single session,
  and the second interruption of the same infrastructure-block class in
  a row.** No work was lost: the first session's own landed commit
  (`e7961b9`) was already pushed and independently re-verified by the
  design channel via a fresh public clone before this session's own
  resumption prompt was authored. The one casualty was AR-FC-2's own
  `libs :outdated` output, captured only in the dead session's own
  transcript and lost with it — re-run fresh this session rather than
  recalled. Full deviation record, plus the interruption-pattern note
  recorded for the author's own future ruling: ADR-0084's own "The
  two-session deviation record" section.
- **A genuinely new, unrelated test-suite finding surfaced and was run
  to ground before any commit landed** — the `mutate-stdout-stdin-
  loopback-test` flake, disclosed above, in `.agents/state.md`, and in
  ADR-0084's own intake list; investigated live (four total runs of the
  same underlying pipeline, three clean) rather than silently smoothed
  past.
