# Session prompt — 2026-08-13: manual-arc tag payment, glossary
# linkage (manual-review dim 4), citation errata sweep (ADR-0126)

You are Claude Code executing under R30 ceremony in
~/src/ehr-testing-tools. Autonomous session; mg's rulings below are
final. This prompt was drafted by the design channel from a fresh
public clone at HEAD c6d0257 (2026-08-13); every behavioral claim
below is marked current (verify) — re-derive from the live tree
before acting on it. The tree wins.

## Read first

- .agents/rulings.md (whole file; "From ADR-0125" section especially)
- .agents/plans/roadmap.md — the citation-sweep row, the two
  manual-review finding rows, the ceremony-scripts row
- .agents/plans/2026-08-13-manual-review-1.md — dimensions 1 and 4
  in full; dimension 4 is this session's fix target, dimension 1 is
  explicitly NOT (separate future charter)
- notes/ADRs.md top-of-file citation rule ("added 2026-07-30") —
  the fix-forward doctrine this sweep applies
- notes/tools/ADRs.md:442 — tools/ADR-0010 "Verdict partiality is
  explicit: the no-verdict arm" — the record the drifted citations
  actually mean (current (verify))
- notes/adr/0010-documentation-doctrine.md front matter — the
  workspace ADR-0010 the drifted citations do NOT mean
- components/docs-tooling/test/ehrt/docs_tooling/
  link_footnote_gate_test.clj — read WHOLE before touching any
  footnote name or link; it states at ~line 120 it doesn't
  distinguish bare from qualified (current (verify)); check for
  count locks
- docs/glossary.md — headings/anchors and its own footnote naming
  convention ([^adr-0010] vs [^sim-adr-0010])
- .agents/skills/manual-review/SKILL.md — dimension 4's scoring
  criteria are this session's acceptance test; dimensions 2 and 3
  are constraints (no duplication; anchor stability)
- Makefile docsgen target + .github/workflows/test.yml
  freshness-gate comment — which docs are GENERATED

## Author rulings in effect (verbatim, .agents/rulings.md)

- Citation sweep chartered: "a, go" (2026-08-13, ADR-0125).
- Session pairing (glossary row + sweep, one session): "b go"
  (2026-08-13, design channel).
- Sweep scope includes the .clj comment/docstring sites, whole
  sweep in one session per the ADR-0099 rule form: "a"
  (2026-08-13, design channel).
- Tag license: CI on c6d0257 verified green by the design channel
  (run 31717674233, test.yml, success, 2026-08-13T15:51:44Z) and by
  mg's own gh run list. Tag instructed in Step 0.
- Standing from ADR-0125: dimension-1 (strip executability) row
  stays OPEN — do not touch it, do not edit any exerciser/lint
  mechanism this session.

## Standing practices (explicit text, not memory)

- Gate policy: any generative/defspec failure at ANY seed is a NEW
  finding — STOP and report. No re-run license exists (R8 died at
  ADR-0116).
- Full `make test` before EVERY push, no exceptions.
- Never fabricate output: every command result shown in the record
  is pasted from a real run.
- Count-lock probe: before editing any cataloged collection or
  footnote set, grep the test tree for locks on it.
- Gate-forced companions land inside the fence by rule and are
  NAMED in the record (docsgen-regenerated pages are the expected
  one here).
- Verify-then-cite: re-verify every path:line this prompt names
  before citing it in the ADR.
- Tag ceremony is double-checked in your own session output — read
  your transcript before closing; ADR-0124 silently skipped its
  tag and ADR-0125 had to repay it.
- Sweep inventory discipline (ADR-0099 form): the channel's site
  list below is expected to UNDERCOUNT. Re-derive the full
  inventory yourself (Step 2a) across ALL file types including
  projects/, .edn, .svg, .yml, .agents/. Any site found beyond the
  list widens into the same commit and is disclosed.

## Step 0 — Ceremony + tag payment

Fresh-clone parity probe (`make ci-parity` or the standing
equivalent). Confirm HEAD c6d0257. Lay ANNOTATED tag
`stable-20260813-manual-arc-close` at c6d0257, message ASCII,
referencing ADR-0125 close and this channel's CI verification.
Push the tag. Verify by peeled ref against the remote
(`git ls-remote --tags` peeled ^{} must equal c6d0257 exactly).
Oracle pre-digest: all 35 roots, pure identity expected end-state
this session (comment-only src edits change zero behavior — the
oracle bracket is the proof).

## Step 1 — Glossary linkage (manual-review dim 4 fix)

Commit 1. Add docs/glossary.md links at FIRST USE of
glossary-defined terms in docs/manual/01, 03, 04, 05, 06, 07.
Chapters 02 and 08 already conform — leave untouched. Priority
targets from the review run (current (verify)): Ch 3 "Pathway"
(~63-77), "script space"/"truth space" (~95-111) — the glossary's
own named most-common misreading; Ch 6 operator/lineage/mutant;
Ch 7 verdict/gate/judge. Constraints: LINK, never restate a
definition (dimension 2 must stay PASS); every anchor verified
against glossary.md's actual headings under the GFM slug rule
(dimension 3 must stay PASS). Check 00-front.md's currency-commit
convention — if its own text requires naming the current commit,
update it in this commit; otherwise leave it.
Commit message: `docs: glossary linkage across manual chapters
1, 3-7 (ADR-0126)`

## Step 2 — Citation errata sweep

Commit 2, per notes/ADRs.md's fix-forward doctrine: dated,
forward-only, frozen files never edited.

2a. INVENTORY: repo-wide grep for bare `ADR-0010` (all file
types), excluding already-qualified `sim/ADR-0010` and
`tools/ADR-0010`. Classify EVERY hit exactly one of:
  (i) verdict-family (the four-arm verdict, worst-of ranking,
      no-verdict causes, R3 amendment) → rewrite to
      `tools/ADR-0010`, link target `notes/tools/ADRs.md` where a
      link exists;
  (ii) documentation-doctrine (the workspace's own ADR-0010) →
      correctly bare, LEAVE UNTOUCHED. Known members (current
      (verify)): docs/dev/AUDIENCES.md:8,
      docs/dev/architecture.md:164;
  (iii) meta-mentions of the drift itself (ADR records, roadmap,
      link_footnote_gate_test's own comment, this prompt's
      archive) → leave untouched.
If any hit resists classification, STOP and report — do not guess.

2b. Channel-probed verdict-family sites, expected to undercount
(all current (verify)): docs/judge-calibration.md:186,
docs/formats.md:548, docs/glossary.md:581 (+ footnote name
[^adr-0010] — check the gate test and glossary's [^sim-adr-0010]
convention before renaming; a rename must update every referent),
docs/manual/assets/verdict-ranking.svg:4 (derivation comment —
edit the citation, PRESERVE the comment; dimension 8 requires it),
components/corpus/docs/palgebra-design.md (multiple),
components/corpus/docs/research/judge-v2-nist-spike-notes.md,
components/corpus/docs/use-cases.edn:419.

2c. GENERATED pages: docs/use-cases/*.md, docs/use-cases.md,
docs/cli.md, docs/operators.md, docs/dev/pipeline.md are docsgen
outputs. NEVER edit them directly — edit the EDN/spec sources,
run `make docsgen`, commit the regenerated pages in the SAME
commit (the CI freshness gate forces this; named companion).

2d. The ~13 .clj sites (comment/docstring-only, current (verify)):
components/judge/src/ehrt/judge/finding.clj, report.clj + both
tests; components/judge-fhir-official/src/.../fhir.clj + test;
components/judge-v2-hapi/src/.../v2.clj;
components/judge-v2-nist/src/.../v2.clj + test;
components/corpus/src/ehrt/corpus/check.clj;
bases/cli/src/ehrt/cli/core.clj, help.clj + core_test.clj.
For EACH: verify the citation sits in a comment or docstring, not
a runtime string. SPECIAL CHECK bases/cli/help.clj: if any
citation lives in emitted help TEXT, changing it changes CLI
output and regenerates docs/cli.md — that is allowed within this
fence via 2c, but must be disclosed in the record, and core_test
locks on help strings must be found by count-lock probe first.
Any edit that cannot be proven zero-behavior: STOP.

Commit message: `docs: citation errata sweep -- origin-qualify
verdict-family ADR-0010 to tools/ADR-0010 (ADR-0126)`

## Step 3 — Records + close

Commit 3: ADR-0126 in notes/adr/ + register line in notes/ADRs.md
(disclose: full classified inventory with counts per class, any
widening beyond 2b/2d, the dated fix-forward note); rulings
register "From ADR-0126" with the verbatim rulings above; roadmap:
glossary-linkage row CLOSED with a targeted re-run of
manual-review DIMENSION 4 ONLY (score it, record PASS/FAIL with
file:line evidence — do not re-run the other seven dimensions;
dimension-1 row explicitly stays open), citation-sweep row closed,
ceremony-scripts row now front of queue; .agents/state.md
refreshed with citations; session record + this prompt
self-archived to .agents/prompts/.
Commit message: `docs: session record and prompt archive --
citation sweep and glossary linkage (ADR-0126)`

## Fence

ONLY: docs/manual/ (chapters 01, 03-07; 00-front.md iff its
currency convention requires; assets/verdict-ranking.svg comment
line only); docs/judge-calibration.md; docs/formats.md;
docs/glossary.md; docs/dev/ — NO, dev pages are class-(ii),
untouched unless inventory proves otherwise; the docsgen sources
(components/corpus/docs/use-cases.edn) + their regenerated
outputs; components/corpus/docs/palgebra-design.md +
research/judge-v2-nist-spike-notes.md; the .clj files named in 2d
(comment/docstring edits only) + any inventory-widened comment
site, disclosed; notes/ADRs.md, notes/adr/0126-*.md; .agents/
tree. NOTHING ELSE. Zero edits to demos/, test-fixtures/,
.github/, frozen registers (notes/tools/, notes/sim/), any
exerciser or lint mechanism. Full `make test` green before each
of the three pushes. Oracle: pure identity, all 35 roots. ASCII
commit messages. STOP-AND-REPORT on: classification ambiguity,
any non-comment .clj hit, any test red, any generative-seed
failure (new finding), tag-ceremony anomaly.

Self-archive this prompt to .agents/prompts/ per convention.
