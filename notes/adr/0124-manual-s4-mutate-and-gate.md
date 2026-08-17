## ADR-0124 — User manual S4: chapters 6-7, breaking data on purpose and judging

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-13.

### Context

S4 of the five-session user-manual arc (ADR-0119's own charter),
landing Chapter 6 (`docs/manual/06-breaking-data-on-purpose.md`,
mutation as deliberate defect injection) and Chapter 7
(`docs/manual/07-judging.md`, the three gates and verdict semantics).
Read first: `docs/manual/00-front.md` through `05-*.md` (voice,
structure, the S3 SVGs' own figure conventions); `docs/use-cases/`'s
own mutation and gate cases plus the root `README.md` Quickstart's own
gate strips; `docs/operators.md` and `docs/judge-calibration.md`
(linked, never restated); `components/corpus/docs/pipeline.edn` +
`palgebra-design.md` (Mutate/Gate stages) and the verdict ranking in
the judge component's own docs/`notes/ADRs.md` ADR-0010 register trace;
`.agents/rulings.md` R2/R6; `.agents/plans/roadmap.md`'s S4 row.

### Tag ceremony

`origin/main` at `da72533` (ADR-0123 close) at session start — matched
the driving prompt's own stated premise exactly. The last five `main`
CI runs (`gh run list --limit 5 --branch main`, checked at session
start): all `completed`/`success` — `da72533` (3m30s), `f9fbeca`
(4m36s), `6827f5b` (4m29s), `3f0db5e` (4m33s), and the scheduled
`Integration` run (9m36s) — no red among the five.

Tag `stable-20260813-invariant-fix` created ANNOTATED at `da72533`;
pushed; peeled ref verified exact. License: case (i), channel
fresh-clone verification 2026-08-13 per the driving prompt's own
citation (lineage, ASCII x2, `src` exactly checker+test, both ruled
conditions present, over-charter test removed), CI confirmed green per
this preflight.

### A citation drift found and disclosed, not fixed here

The Read-first list points at "the verdict ranking in judge docs/
ADR-0010's register trace." Checking `notes/adr/0010-documentation-
doctrine.md` directly shows that file is titled "Documentation
doctrine" — it does not discuss verdicts at all. The unqualified
citation `ADR-0010` used throughout `docs/judge-calibration.md`,
`docs/formats.md`, `docs/glossary.md`, every `use-cases/*.md` gate
page, and every source/test file under `components/judge/` for the
four-arm verdict design (`:pass`/`:rejected`/`:indeterminate`/
`:no-verdict`, the `worst-of` ranking) is a pre-existing, repo-wide
citation drift, not something introduced by this session and not
within this session's own fence to correct (DOCS-AND-REGISTERS-ONLY,
scoped to Chapters 6-7). Since the citation is the SOLE, consistently
used convention across dozens of already-published files for exactly
this content, this session follows it — `verdict-ranking.svg`'s own
source comment and Chapter 7's calibration section both cite `ADR-0010`
the same way `judge-calibration.md` already does — rather than either
inventing a different citation or blocking on it. Flagged here for the
author's own attention; a future errata-sweep session, not this one,
is the right shape to trace where the real verdict-design ADR content
lives and correct every citation site at once.

### Decision

#### Commit 1 (`b6256b6`) — chapter 6, breaking data on purpose

`docs/manual/06-breaking-data-on-purpose.md`: mutation taught as named,
traceable defect injection — one operator, one locator, an edit that
touches nothing else in the file, with a lineage record and an
operation manifest that never let the mutant's own provenance go
unrecorded. Choosing an operator is taught by starting from the
contract you want proven (`operators.md`'s own Contract column) rather
than browsing the edit list; the inject-a-defect-expect-the-matching-
finding loop is stated as the chapter's own organizing idea and closed
with a real, witnessed flip — the `README.md` storefront-patient
example, `remove-required-element` at `entry[0].resource.resourceType`,
clean pass before, `:invalid`/`:invariant` rejected after, both new
findings earned by the one edit and nothing else. Closes by naming
[Mutation-adequacy of your own checks](../../docs/use-cases/mutation-adequacy-of-your-own-checks.md)
as the same loop turned on a reader's own validation logic.

**The figure.** `docs/manual/assets/inject-expect-loop.svg`: canonical
file into Mutate (operator + locator labeled) into mutant-plus-lineage
(the contract stated) into Gate into the matching finding, with a
dashed correspondence arc naming the loop's own point — content derived
from `components/corpus/docs/pipeline.edn`'s Mutate/Gate stage
equations and `docs/operators.md`'s own What-it-does/Contract split,
cited in the SVG's own source comment; the worked instance's own values
are this session's fresh regeneration of the `README.md` example.

#### Commit 2 (`b340326`) — chapter 7, judging

`docs/manual/07-judging.md`: the three gates at reader level (`gate
fhir` against base spec plus any declared profile; `gate v2`,
base-structural HAPI; `gate v2-nist`, profile-tier against a supplied
bundle) — flags linked to `cli.md`, never restated. Verdict semantics:
`:rejected` (the criterion was applied and failed), `:pass` (applied,
nothing failed — a claim about what was checked, not a correctness
guarantee), and `:no-verdict` (the criterion could not be fully
applied at all — a terminology-suppressed code, or a defective
profile) taught as a genuinely distinct third answer, not a variant of
pass or rejected. The dominance order (`:rejected` beats `:no-verdict`
beats `:pass`, empty findings is `:pass`) is stated as the rule a
file's own single reported verdict follows. Closes linking
`judge-calibration.md` for per-operator conviction detail and
`--baseline` mode.

**The figure.** `docs/manual/assets/verdict-ranking.svg`: three
stacked bars, `rejected` over `no-verdict` over `pass`, each captioned
with what it means and a witnessed instance, "dominates" arrows between
them — content derived from the documented verdict ranking
(`worst-of`, D10/O2 in `palgebra-design.md`, `notes/ADRs.md` ADR-0010),
cited in the SVG's own source comment per the citation-drift note
above; the reader-facing vocabulary drawn is the honest three-way split
(`:indeterminate` is reserved, no producer anywhere in this repo, so it
is not shown).

**Witnessed strips, this session.** Because this session's own fence is
DOCS-AND-REGISTERS-ONLY (zero `src`/`test`/`demos`), every strip in
both chapters is copied verbatim from an already-published witnessed
source — `README.md`'s own "What you get" section (Chapter 6's mutate/
gate pair), `docs/use-cases/judge-tier-calibration-studies.md` (Chapter
7's `gate v2` before/after pair), and `docs/use-cases/profile-tier-hl7v2-conformance-gating.md`
(Chapter 7's `gate v2-nist` no-verdict run) — but rather than trust that
prior witnessing at a distance, this session re-ran every command
directly against its own tree (writing only to gitignored `out/`) and
compared the result:

- `bin/ehrt gate fhir test-fixtures/fhir/storefront-patient.json` —
  `:pass`, 1 finding (`:warning`/`invariant`, dom-6), matching
  `README.md` exactly.
- `bin/ehrt corpus mutate ... remove-required-element ...` — lineage id
  `1acdf4a2...`, contract text matching `operators.md`'s own
  `:remove-required-element` row exactly.
- `bin/ehrt gate fhir out/manual-s4-demo-mutants` — `:rejected`, 2
  findings (`:fatal`/`invalid` "Unable to find resourceType property";
  `:error`/`invariant` bdl-5), matching `README.md`'s own shown
  transcript exactly.
- `bin/ehrt gate v2 test-fixtures/v2/adt-a01-admit.hl7` (before) →
  `bin/ehrt corpus mutate ... blank-required-field ... MSH-9` →
  `bin/ehrt gate v2 out/manual-s4-calibration/blank-required-field`
  (after) — `{:pass 1}` → `{:rejected 1}`, `by-code {"hl7-exception" 1}`,
  matching `judge-tier-calibration-studies.md`'s own stated result
  exactly.
- `bin/ehrt gate v2-nist test-fixtures/v2-nist/covidELR/231HL7TestFilewithHHSData.txt --profile test-fixtures/v2-nist/COVID19_ELR-v2.3.1`
  — `:no-verdict`, 473 findings, `:cause :profile-spec-error` (via
  `--json`), exit code `3` — matching `profile-tier-hl7v2-conformance-gating.md`'s
  own stated `:no-verdict`/`:profile-spec-error` characterization
  exactly; the pretty totals/by-code table and the `:cause` field are
  this session's own witnessed addition, not previously shown as a
  literal transcript in that use-case page.

No divergence found anywhere — the STOP-AND-REPORT clause this
session's own prompt named for exactly that case never fired.

#### Front-page updates

`docs/manual/00-front.md`: Chapters 6-7 drop their working-title
markers and gain firm one-liners; the arc-status prose moves from
"Chapters 1-5 are landed" to "Chapters 1-7 are landed"; the "Chapters
6-8" framing narrows to naming only Chapter 8 as still a working
proposal; the currency contract's per-chapter witnessing-commit list
extends to name Chapters 4 through 7 together. No resequencing
occurred this session — Mutate and Gate land exactly where ADR-0121's
own disclosed judgment call already placed them.

### Oracle bracket

Pre-analysis: pure identity expected — every file touched this session
is `docs/manual/*` (new/edited docs), `docs/manual/assets/*` (two new
SVGs), registers, and this ADR/session-record/prompt-archive set;
nothing touches any oracle root's own `src`.

`bin/regression-oracle da72533 b340326` → **`IDENTICAL: every root's
digest matches between da72533 and b340326`**, all 35 roots. Matches
the pre-analysis exactly. Run against `b340326` (chapter 7's own
commit, the last content-bearing commit this session makes) rather
than this record's own close commit, matching ADR-0121's own
precedent — the close commit touches only registers and this
ADR/session-record/prompt-archive set, none of it any oracle root's
own `src`, so it cannot move any digest the bracket above already
covers.

### Verification

`clojure -M:poly check`: OK, before each commit. Full `make test`: run
before each push — GREEN. `gitleaks git --staged -v`: clean, every
commit. `git diff --cached --stat` reviewed before each commit: exactly
the fenced files. Post-push verification: every pushed commit message
diffed against its own message file — the only delta was `git log
--format=%B`'s own trailing-blank-line formatting artifact; the
ASCII-only check on each commit message empty every time.

### Deviations

**No premise mismatch.** Every Read-first document matched its own
characterization in the driving prompt; the tag license's stated
preflight conditions held exactly; every command excerpted from a
witnessed source ran exactly as written when re-run this session, with
no divergence.

**The ADR-0010 citation drift**, above, is the one finding this session
disclosed rather than silently absorbed or silently corrected — flagged
for the author, not fixed here (out of this session's own fence).

### Fences

Touched: `docs/manual/06-breaking-data-on-purpose.md` (new);
`docs/manual/07-judging.md` (new); `docs/manual/assets/
inject-expect-loop.svg` (new); `docs/manual/assets/verdict-ranking.svg`
(new); `docs/manual/00-front.md` (one-liners, arc-status prose,
currency contract); `.agents/plans/roadmap.md` (S4 LANDED row);
`.agents/rulings.md` (untouched — no mid-session ruling occurred);
`notes/adr/0124-manual-s4-mutate-and-gate.md` (this file);
`notes/ADRs.md`; `notes/adr/README.md`; `.agents/session-records/*`;
`.agents/prompts/*`. ZERO `src`/`test`/`demos` touched anywhere. ZERO
edits to Chapters 1-5.

### Index line

```
- 2026-08-13 — manual-s4-mutate-and-gate — ADR-0124
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

User manual S4: chapters 6-7, breaking data on purpose and judging — lands Chapter 6 (`docs/manual/06-breaking-data-on-purpose.md`: mutation as named, traceable defect injection, choosing an operator by the contract you want proven rather than browsing the catalog, the inject-a-defect-expect-the-matching-finding loop closed with the `README.md` storefront-patient example) and Chapter 7 (`docs/manual/07-judging.md`: the three gates -- official FHIR, v2 HAPI, v2 NIST -- at reader level, verdict semantics taught with `:no-verdict` as a genuinely distinct third answer rather than a variant of pass or rejected, the worst-of dominance ordering); two hand-authored SVG figures (`inject-expect-loop.svg`, `verdict-ranking.svg`); every strip in both chapters re-derived by fresh regeneration this session against the live tree, byte-identical to its own witnessed source (`README.md`, `judge-tier-calibration-studies.md`, `profile-tier-hl7v2-conformance-gating.md`), no divergence found; a pre-existing, repo-wide `ADR-0010` citation drift found while reading the driving prompt's own verdict-ranking pointer -- `notes/adr/0010-documentation-doctrine.md` is titled "Documentation doctrine," not the verdict design the citation is used for throughout `docs/judge-calibration.md`/`docs/formats.md`/`docs/glossary.md`/every `components/judge/` source and test file -- disclosed in full and followed as the sole established convention rather than fixed, out of this session's own docs-and-registers-only fence; zero `src`/`test`/`demos` touched anywhere, the oracle holds pure identity across all 35 roots
