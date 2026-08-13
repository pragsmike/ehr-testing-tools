# 2026-08-13 — User manual S4: chapters 6-7, breaking data on purpose and judging (ADR-0124)

## Scope

S4 of the five-session user-manual arc: chapter 6
(`docs/manual/06-breaking-data-on-purpose.md`, mutation as deliberate
defect injection) and chapter 7 (`docs/manual/07-judging.md`, the three
gates and verdict semantics). Two content commits landed, plus this
record's own close-phase commit.

## Red->green evidence highlights

**Every strip in both chapters re-derived by fresh regeneration this
session, not merely copied at a distance.** Because this session's
fence is docs-and-registers-only (zero `src`/`test`/`demos`), every
excerpt is copied verbatim from an already-published witnessed source
(`README.md`'s own "What you get" section, `docs/use-cases/judge-tier-calibration-studies.md`,
`docs/use-cases/profile-tier-hl7v2-conformance-gating.md`) — but rather
than trust that prior witnessing, this session re-ran every generating
command directly against its own tree (`bin/ehrt corpus mutate`, `bin/
ehrt gate fhir`, `bin/ehrt gate v2`, `bin/ehrt gate v2-nist`), writing
only to gitignored `out/`, and compared every resulting value against
its own source: the storefront-patient mutate/gate-fhir pair, the
`blank-required-field`/`gate v2` before-after pair, and the
`gate v2-nist` no-verdict run against the committed COVID19_ELR bundle
all matched byte-for-byte. No divergence found anywhere.

**Oracle bracket:** `bin/regression-oracle da72533 <final>` ->
`IDENTICAL: every root's digest matches`, all 35 roots — matching the
pre-analysis (no oracle root's own `src` touched; only `docs/manual/*`
and registers).

**Full `make test`** (`clojure -M:poly check` + `clojure -M:poly test
:all skip:integration` + `bin/verify-nist-lock`): run before every
push — GREEN.

## Judgment calls and their disclosure status

- **A pre-existing, repo-wide `ADR-0010` citation drift, found and
  disclosed, not fixed.** The driving prompt's own Read-first list
  points at "the verdict ranking in judge docs/ADR-0010's register
  trace." Reading `notes/adr/0010-documentation-doctrine.md` directly
  shows that file is titled "Documentation doctrine" and never
  discusses verdicts at all — the unqualified `ADR-0010` citation used
  throughout `docs/judge-calibration.md`, `docs/formats.md`,
  `docs/glossary.md`, every gate-family `use-cases/*.md` page, and
  every source/test file under `components/judge/` for the four-arm
  verdict design is a drift that predates this session by a wide
  margin — visible in the sheer number of already-published files that
  already treat it as settled. This session's own fence is
  docs-and-registers-only, scoped to Chapters 6-7 alone; tracing where
  the real verdict-design content actually lives and correcting every
  citation site is its own errata-sweep session, not something to
  attempt piecemeal here. This session follows the existing, universal
  convention (`verdict-ranking.svg`'s own source comment, Chapter 7's
  calibration section) rather than inventing a different citation or
  blocking on it, and discloses the finding in full in
  `notes/adr/0124-manual-s4-mutate-and-gate.md` for the author's own
  attention.
- **`docs/manual/00-front.md` lands with commit 2 (chapter 7), a
  deliberate choice, not the oversight S3's own record disclosed for
  the same file.** The arc-status prose ("Chapters 1-7 are landed")
  only becomes true once chapter 7 itself exists, so bundling the
  front-page edit into the commit that makes it true, rather than
  splitting it or deferring to the close commit, keeps every landed
  commit's own front-page claim accurate at the moment it lands.

## Findings and HEAD landed

No discrepancies between the driving prompt's stated preflight premise
and the live tree: `origin/main` was at `da72533` exactly; the last
five CI runs were all green; every Read-first document matched its own
characterization (aside from the ADR-0010 citation drift, above, which
is a finding about a document's own content, not a premise mismatch in
what the prompt asked this session to read); every command excerpted
by either chapter ran exactly as documented when re-run this session —
the STOP-AND-REPORT clause this session's own prompt named for a
witnessed-source/tree divergence never fired.

The tag `stable-20260813-invariant-fix` was created ANNOTATED at
`da72533` (this session's own Step 0), pushed, peeled ref verified
exact.

**Oracle bracket:** `bin/regression-oracle da72533 b340326` →
`IDENTICAL: every root's digest matches between da72533 and b340326`,
all 35 roots — matching the pre-analysis (no oracle root's own `src`
touched; only `docs/manual/*` and registers).

**HEAD landed**: `b6256b6` (chapter 6), `b340326` (chapter 7), and this
record's own close-phase commit.
