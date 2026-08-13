# 2026-08-13 — User manual S5: chapter 8, the manual-review skill, arc close (ADR-0125)

## Scope

S5, the fifth and final session of the five-session user-manual arc:
Chapter 8 (`docs/manual/08-your-own-data.md`, intake/checking/
baselining a corpus the reader didn't generate), the `manual-review`
skill plus its own first scored run, two `stable-*` tag ceremonies (one
repaying an ADR-0124 deviation), and the author's own chartered
citation errata sweep row. Three content/registers commits landed,
this record's own close-phase commit is the third.

## Red->green evidence highlights

**Every strip in Chapter 8 re-derived by fresh regeneration this
session.** Docs-and-registers-only fence (zero `src`/`test`/`demos`),
so every excerpt is copied verbatim from an already-published witnessed
source (`docs/use-cases/acceptance-qa-of-vendor-corpora.md`, `README.md`'s
own Quickstart, `docs/use-cases/regression-baselining.md`) — but rather
than trust that prior witnessing, this session re-ran every generating
command directly against its own tree (`bin/ehrt corpus intake`, `bin/
ehrt check`, `bin/ehrt gate v2 ... --baseline`), writing only to
gitignored `out/`, and compared every resulting value against its own
source. No divergence found.

**`docs/cli.md` confirmed current before dimension 7 of the review
ran.** `make cli-doc` regenerated it fresh; byte-identical to the
tracked copy. Every currency claim the review checked is against a
confirmed-current file, not a possibly-stale one.

**Both `ehrt.docs-tooling.skill-mirror-currency-test` and
`ehrt.docs-tooling.index-completeness-test` proven RED before GREEN**:
the new skill directory and its `.claude/` mirror, and the new
`.agents/skills/manual-review/README.md`, were all created only after
confirming the tests failed without them.

**Oracle bracket:** `bin/regression-oracle a453fe1 39282a6` →
`IDENTICAL: every root's digest matches`, all 35 roots — matching the
pre-analysis (no oracle root's own `src` touched; only `docs/manual/*`,
the new skill's docs-tooling-adjacent files, and registers).

**Full `make test`** (`clojure -M:poly check` + `clojure -M:poly test
:all skip:integration` + `bin/verify-nist-lock`): run before every
push — GREEN both times.

## Judgment calls and their disclosure status

- **An undisclosed ADR-0124 deviation, found and repaid, not merely
  disclosed.** `notes/adr/0124-manual-s4-mutate-and-gate.md`'s own
  "Tag ceremony" section, and its own session record, both state that
  `stable-20260813-invariant-fix` was created at `da72533` during that
  session — but `git tag -l` at this session's own start showed no such
  tag existed on the remote or locally. The standing tag law makes a
  deferred license the deviation, and this one went undisclosed in the
  S4 record. This session repays it directly (creating the tag at
  `da72533`, re-verifying its license conditions against the live tree
  rather than trusting the S4 record's own claim) and records the
  deviation as owned to the S4 session, not this one, in ADR-0125's own
  "Deviations" section.
- **A fail-grade review finding, STOP-AND-REPORT, ruling requested and
  received in-session.** The `manual-review` skill's own first scored
  run came back FAIL overall (dimensions 1 and 4 of 8). Per the driving
  prompt's own gate and the skill's own review discipline, this session
  stopped after landing the report (Commit 2) rather than proceeding
  directly to Commit 3's arc-close declaration, and asked the author
  how to proceed. The author ruled: close the arc now, land both
  findings as open backlog rows for a future fix session. Commit 3
  executes exactly that ruling.

## Findings and HEAD landed

No discrepancies between the driving prompt's stated preflight premise
and the live tree beyond the ADR-0124 tag deviation itself (found and
repaid, above): `origin/main` was at `a453fe1` exactly; the last five CI
runs were all green; every Read-first document matched its own
characterization; every command excerpted by Chapter 8 ran exactly as
documented when re-run this session.

Both tags — `stable-20260813-invariant-fix` at `da72533` and
`stable-20260813-manual-s4` at `a453fe1` — created ANNOTATED, pushed,
peeled refs verified exact against both target commits.

**Oracle bracket:** `bin/regression-oracle a453fe1 39282a6` →
`IDENTICAL: every root's digest matches between a453fe1 and 39282a6`,
all 35 roots.

**HEAD landed**: `9592554` (chapter 8), `39282a6` (the manual-review
skill and its first scored run), and this record's own close-phase
commit.
