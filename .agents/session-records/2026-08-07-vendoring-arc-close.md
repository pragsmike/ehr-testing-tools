# 2026-08-07 — Vendoring arc close: the mix more than tripled, the bytes cannot lie

## Scope

Session prompt naming AR-VAC-0 through AR-VAC-6, closing the vendoring
arc (ADR-0069–0074) per the ADR-0068 close pattern. **This close ran
across two Code sessions.** The first executed Step 0 (AR-VAC-0: the
`stable-20260807-demos-front-door` tag, verified already present at
`5e2afaf`, disclosed, not re-created) and Step 1 (AR-VAC-1/2: the
rulings appends, committed `beec395`, pushed, verified), then was
killed mid-ceremony by an infrastructure-level API request block
unrelated to the work's own content. No work was lost — both landed
commits were already pushed. The design channel verified the seam by
fresh public-clone probe (tip, tag, rulings-append text, all re-read
against the live remote) before authoring this session's own
resumption prompt, which marked Step 0/Step 1 READ-ONLY and this
session executed Steps 2–3 only. Full account, rulings, the
regeneration table, the arc narrative, the intake list, and the
two-session deviation record: `notes/ADRs.md` ADR-0074.

This session's own preflight (re-run, not inherited): working directory
confirmed the ext4 clone (`/dev/sdd`, `df -T .`), tip `beec395` exactly,
working tree clean. Baseline: `clojure -M:poly check` OK; full suite
green (261 `Test results:` lines, 0 failures/0 errors — up from the
player arc's own 227, the arc's own thirty-four new namespaces); oracle
pre-digest (`bin/regression-oracle beec395 beec395`) all twenty-seven
roots IDENTICAL; `gitleaks detect -v` clean (721 commits). AR-VAC-0's
own tag re-verified by direct `git cat-file -p`, peels to `5e2afaf`
exactly; AR-VAC-1's own rulings appends re-verified by direct read,
match the ruling's own text exactly.

Step 2 (`2f474b8`, AR-VAC-3/AR-VAC-4/AR-VAC-5) regenerated
`.agents/state.md` in full against the live tree (thirteen claims
re-probed, a thirteen-row regeneration table in ADR-0074, including two
entirely new sections this file never carried before — the vendored
module inventory and the demos/scenarios geometry), re-derived one
reading-set budget (`:onboarding` 1180→1240, an increase — five
sessions' own churn outpaced this close's own rotation, the opposite of
the player close's own decrease), and rotated the Done section:
ADR-0068's own disclosed leftover joined the attic's existing
player-arc section; ADR-0069–0073 relocated under a new dated
vendoring-arc header. This session pre-empted the dangling-Done-pointer
hazard the same way every prior close has, with an HTML-comment
placeholder rather than a premature pointer.

Step 3 (this record) re-ran AR-VAC-2's own `libs :outdated` cadence
fresh (the killed session's own report was transcript-only and did not
survive it — no reconstruction from memory, a real re-run instead;
unchanged from the player arc's own report, no new upstream release
across the entire vendoring arc), authored `notes/adr/
0074-vendoring-arc-close.md` directly, appended its own index line to
`notes/ADRs.md`, corrected `notes/adr/README.md`'s own stale file count
(71→72, verified by `ls`), added the Done pointer
(`- 2026-08-07 — vendoring-arc-close — ADR-0074`) in the same commit as
the index line, ran the closing oracle bracket spanning BOTH sessions'
own commits, archived this session's own prompt (the resumption
preamble plus the original close prompt as Appendix A, one file per
the pairing gate), and recorded this session.

## Red→green evidence highlights

A docs-only session's proof is the suite staying green and untouched,
not a red→green cycle — confirmed at every checkpoint: 261 `Test
results:` lines, 0 failures/0 errors, identical shape to the Step 0
baseline throughout. This session's own headline verification is the
oracle bracket spanning the full two-session close: `bin/regression-
oracle 5e2afaf <this session's own closing tip>` confirmed all
twenty-seven vendored roots IDENTICAL across every commit either
session made, the mechanical proof that the interruption between
sessions changed nothing about the work's own docs-only character.

## Judgment calls and their ratification status

- **Step 0/Step 1 were treated as READ-ONLY, per the resumption
  prompt's own explicit instruction — verified by direct probe, never
  re-executed.** This session did not re-run `clojure -M:poly check`
  or the full suite AS OF the killed session's own commits in
  isolation; it verified their landed state (tag peeled ref, rulings
  append text) directly against the live tree and moved forward from
  there, exactly as a resumption after a mid-arc interruption should —
  distinct from a routine session's own Step 0, which runs the full
  preflight fresh because nothing prior in the SAME session already
  proved it.
- **AR-VAC-2's own report was re-run rather than reconstructed.** The
  resumption prompt named this explicitly as a casualty of the kill (the
  killed session's own transcript-only output does not survive
  termination) and instructed a fresh run "regardless" — this session
  did not attempt to recall or approximate the lost output from the
  resumption prompt's own framing, which would have risked stating a
  claim never actually re-verified.
- **The `:onboarding` budget's own increase, disclosed rather than
  smoothed over.** Five sessions' worth of index/Now/Done churn (five
  new session records, five new prompt archives, the roadmap's own
  repeated Now/Done edits) grew the set faster than this session's own
  rotation shrank it — the opposite direction from the player close's
  own decrease. Recorded plainly in both `.agents/reading-sets.edn`'s
  own dated comment and ADR-0074's own regeneration section, not
  presented as if budgets only ever fall.

## Findings and HEAD landed

No new finding surfaced this session beyond what ADR-0069–0073 already
disclosed and ADR-0074's own intake list restates with citations — this
was a regeneration-and-close session, not a discovery one. The one
process finding is the close's own two-session shape, recorded as a
deviation (below) rather than as a code-level finding, since nothing in
the repository itself was implicated.

Commits, in order (this session): `2f474b8` (Step 2, state
regeneration + budgets + rotation), and this session's own closing
records commit (Step 3). Preceded by the killed session's own `beec395`
(Step 1) and its earlier tag action at `5e2afaf` (Step 0), both
READ-ONLY this session.

## Verification

- `bin/regression-oracle beec395 beec395` (this session's own Step 0):
  all twenty-seven vendored-root batches IDENTICAL.
- `bin/regression-oracle 5e2afaf <this session's own closing commit>`
  (Step 3, spanning both sessions): all twenty-seven roots IDENTICAL —
  the full two-session bracket confirmed docs-only.
- Full suite (`clojure -M:poly test :all skip:integration`): green at
  Step 0 baseline (261 namespaces, 0/0) and again after Step 2's own
  edits (261 namespaces, 0/0, identical shape).
- `clojure -M:poly check`: OK, every step this session.
- `gitleaks git --staged -v`: clean, every commit this session;
  `gitleaks detect -v` (full history): clean at Step 0 baseline (721
  commits).
- Post-push message verification, both commits this session: one delta
  each against the message file, the known harmless trailing-newline
  artifact.
- Tag verification: `stable-20260807-demos-front-door` peeled ref
  resolves to `5e2afaf` exactly (re-confirmed this session by direct
  `git cat-file -p`, not inherited from the killed session's own
  transcript claim).

## Deviations, disclosed

- **This close ran across two Code sessions, not one — the first arc
  close this repo has run that did not complete in a single session.**
  An infrastructure-level API request block, unrelated to the work's
  own content, terminated the first session immediately after its own
  Step 1 landed and pushed. No work was lost: both of the first
  session's own commits (`beec395` and the Step 0 tag action at
  `5e2afaf`) were already pushed and were independently re-verified by
  the design channel via a fresh public clone before this session's own
  resumption prompt was authored. The one casualty was AR-VAC-2's own
  `libs :outdated` output, captured only in the dead session's own
  transcript and lost with it — re-run fresh this session rather than
  recalled. Full deviation record, with the reasoning for why this
  disclosure lives in both this record and ADR-0074 rather than only
  one: ADR-0074's own "The two-session deviation record" section.
