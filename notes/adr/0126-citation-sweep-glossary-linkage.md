## ADR-0126 — Manual-arc tag payment, glossary linkage (dimension 4 fix), citation errata sweep

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-13.

### Context

Chartered from a fresh public clone at HEAD `c6d0257` (ADR-0125's own
close): two open backlog rows from the manual arc's own close —
the citation errata sweep (`.agents/rulings.md`, "From ADR-0125,"
author verbatim "a, go") and the manual-review skill's own dimension-4
FAIL (glossary linkage, `.agents/plans/2026-08-13-manual-review-1.md`)
— landed together per the design channel's own session-pairing ruling,
"b go." Sweep scope explicitly includes the `.clj` comment/docstring
sites, whole sweep in one session per the ADR-0099 rule form
(the author's third ruling, "a"). Dimension-1 (strip executability)
stays OPEN, untouched, per ADR-0125's own standing scope fence.

### Step 0 — Ceremony and tag payment

`make ci-parity` (fresh clone, cold artifact cache, `clojure -M:poly
check` + `test :all skip:integration`): green, 535 tests / 0 failures /
0 errors, 4m31s. `HEAD` confirmed `c6d0257`. CI license verified: the
design channel's own citation (run `31717674233`, `test.yml`,
`success`, `2026-08-13T15:51:44Z`) matches `gh run list --limit 5
--branch main`'s own top row exactly, re-confirmed at session start.

Tag `stable-20260813-manual-arc-close` created ANNOTATED at `c6d0257`;
pushed; peeled ref confirmed `c6d0257149e14fbad96c42130231996fdb6c2000`,
exact, via `git ls-remote --tags origin`.

### Step 1 — Glossary linkage (manual-review dimension 4 fix), commit 1

Added `docs/glossary.md` links at first use of glossary-defined terms
across Chapters 1, 3–7 (Chapters 2 and 8 already conformed, left
untouched per the driving prompt's own scope). Every link is a bare
`[term](../glossary.md)` page link, matching Chapter 2's and Chapter
8's own existing pattern exactly — no anchor, no restated definition
(dimension 2 stays PASS by construction: nothing added is a table or a
restated flag/operator list).

- **Chapter 1** (`01-what-this-is.md`): ground-truth log (line 19),
  encounter (line 100).
- **Chapter 3** (`03-a-simulated-hospital.md`, the priority target —
  the glossary's own named most-common misreading): census and churn
  (line 32), site profiles (line 55), pathways (line 66), **script
  space** (line 95), **truth space** (line 103), ground truth (line
  115), emitters (line 123).
- **Chapter 4** (`04-time-on-the-wire.md`): ground truth (line 49),
  emitter (line 55), pathway (line 116).
- **Chapter 5** (`05-batch-delivery.md`): determinism (line 115),
  encounter (line 159).
- **Chapter 6** (`06-breaking-data-on-purpose.md`, priority target):
  operator (line 13), lineage and mutant (line 16), finding (line 96).
- **Chapter 7** (`07-judging.md`, priority target): gate (line 3),
  judge (line 10), verdict (line 16).

`00-front.md`'s own currency-commit convention (`## The currency
contract`) states which commit each chapter's own *witnessed command
output* was captured against — it says nothing about incidental prose
edits, and none of the above touches a command strip or a captured
output. Read directly and left untouched, per the driving prompt's own
conditional instruction.

`components/docs-tooling/test/ehrt/docs_tooling/link_footnote_gate_test.clj`
read whole before this step: its first check (every `](dest)` resolves
on disk, anchors stripped) is the only one these links exercise — every
added link is a bare `../glossary.md` page reference, which resolves
trivially (the file exists), so no footnote-marker mechanics are
touched by this commit.

### Step 2 — Citation errata sweep, commit 2

**2a — inventory.** Repo-wide grep for bare `ADR-0010` (case-sensitive,
catching plain-text citations) plus a second, case-insensitive pass for
`[^adr-0010]`-style footnote markers (the first pass silently misses
these, since the marker string itself is lowercase and doesn't contain
the literal substring `ADR-0010` outside its own definition line) — the
second pass is what actually found `docs/formats.md`'s and
`docs/judge-calibration.md`'s own footnote *usage* sites (the
case-sensitive pass had only caught their definition lines, which
happen to spell the token inside `[ADR-0010]` link text). Every hit
classified:

- **(i) verdict-family** (the four-arm verdict, `worst-of` ranking,
  `:no-verdict`/`:cause`, the exit-code-3 policy) — origin-qualified to
  `tools/ADR-0010` wherever this session's own fence permits (see
  below); disclosed, not touched, where it doesn't.
- **(ii) documentation-doctrine** (the workspace's own ADR-0010,
  audience-forked docs, R34/R38) — correctly bare, left untouched
  everywhere found, including two sites this session corrected against
  the channel's own probe (below).
- **(iii) meta-mentions of the drift itself** — ADR/roadmap/rulings
  prose narrating the finding, in backticks or past tense, and every
  frozen `notes/adr/`, `notes/tools/`, `notes/sim/`, `.agents/prompts/`,
  `.agents/session-records/` historical record quoting either citation
  as it stood at the time — left untouched (frozen-archive discipline;
  none of these are load-bearing citations to fix, they're history).
- **A fourth class the channel's own two-class scheme did not
  anticipate, found this session:** a **sim-identity family** — bare
  `ADR-0010` in `components/sim/docs/*.md` and
  `components/sim-trajectory/docs/*.md` (event-sourcing.md,
  patient-state-model.md, sim-theory.md, sim-theory.edn,
  trajectory-computation.md, gmf-interpreter.md — 17 sites across 6
  files) meaning the *sim* repo's own frozen `sim/ADR-0010` (patient
  identity: `:patient-id`/`:mrns`/`:participants`), not this
  workspace's ADR-0010 nor the tools ADR-0010. Per the citation rule
  (`notes/ADRs.md`, "added 2026-07-30"), a bare citation in a live
  workspace document means *this file's own record* — so these are
  genuinely drifted too, a third referent the citation-sweep charter
  never named. **Disclosed, not fixed**: every one of these files sits
  outside this session's own touch fence (`components/sim/docs/`,
  `components/sim-trajectory/docs/` are not listed), so the finding
  widens the inventory without widening the edit. Flagged for a future
  session — this sweep's own scope was verdict-family only.

**A corrected disclosure against the channel's own probe:**
`docs/glossary.md:581` was named in the driving prompt as a
verdict-family site needing a footnote rename. Direct inspection found
otherwise: `docs/glossary.md`'s only `[^adr-0010]` usage (line 5) cites
R38, the doc-audit/glossary-merge decision — genuinely class (ii),
correctly bare, matching its own definition
(`[^adr-0010]: Design record [ADR-0010](../notes/ADRs.md).`, line 581).
No verdict-family citation exists anywhere in the live
`docs/glossary.md`. Per the standing verify-then-cite discipline, this
session follows the live tree over the probe: **`docs/glossary.md` is
untouched, zero sites fixed there.** (ADR-0124/ADR-0125's own listing
of `docs/glossary.md` among the drifted sites is not itself wrong as a
class-(i) *citation drift exists in this repo* claim — it is imprecise
about which file carries it; `docs/judge-calibration.md` and
`docs/formats.md`, both fixed below, are the real verdict-family
footnote sites.)

**2b/2d — fixed, within fence.** Every verdict-family site inside this
session's own touch fence origin-qualified to `tools/ADR-0010`
(bare text) or renamed to the `[^tools-adr-0010]` marker (footnote
form, matching `docs/glossary.md`'s own `[^sim-adr-NNNN]`/`[^tools-adr-
NNNN]` convention), targeting `notes/tools/ADRs.md`
(`## ADR-0010 — Verdict partiality is explicit: the no-verdict arm`,
line 442 — re-verified directly this session, not assumed):

| File | Sites | Form |
|---|---|---|
| `docs/judge-calibration.md` | 4 usages + 1 def | footnote, renamed `[^tools-adr-0010]` |
| `docs/formats.md` | 2 usages + 1 def | footnote, renamed `[^tools-adr-0010]` |
| `docs/manual/assets/verdict-ranking.svg` | 1 (derivation comment) | bare text; comment itself preserved, only the citation edited (dimension-8 requirement) |
| `components/corpus/docs/palgebra-design.md` | 4 | bare text |
| `components/corpus/docs/research/judge-v2-nist-spike-notes.md` | 1 | bare text |
| `components/corpus/docs/use-cases.edn` | 1 usage + 1 def (the `:profile-tier-hl7v2-conformance-gating` case's own `:note`) | footnote, renamed `[^tools-adr-0010]`; regenerated `docs/use-cases/profile-tier-hl7v2-conformance-gating.md` in this same commit (`make use-cases` — the docsgen-source rule, 2c) |
| `components/judge/src/ehrt/judge/finding.clj` | 9 | comment/docstring |
| `components/judge/src/ehrt/judge/report.clj` | 7 | comment |
| `components/judge/test/ehrt/judge/finding_test.clj` | 3 | comment |
| `components/judge/test/ehrt/judge/report_test.clj` | 3 | comment |
| `components/judge-fhir-official/src/ehrt/judge_fhir_official/fhir.clj` | 3 | comment/docstring |
| `components/judge-fhir-official/test/ehrt/judge_fhir_official/fhir_test.clj` | 1 | comment |
| `components/judge-v2-hapi/src/ehrt/judge_v2_hapi/v2.clj` | 1 | docstring |
| `components/judge-v2-nist/src/ehrt/judge_v2_nist/v2.clj` | 1 | docstring |
| `components/judge-v2-nist/test/ehrt/judge_v2_nist/v2_test.clj` | 1 | comment |
| `components/corpus/src/ehrt/corpus/check.clj` | 1 | docstring |
| `bases/cli/src/ehrt/cli/core.clj` | 6 | docstring |
| `bases/cli/src/ehrt/cli/help.clj` | 3 | docstring/comment |
| `bases/cli/test/ehrt/cli/core_test.clj` | 2 | comment |

**2d — the special check.** `bases/cli/src/ehrt/cli/help.clj` was
checked line by line: its three verdict-family sites (lines 16, 18, 42)
are all docstring/comment, never the rendered `:meaning` strings inside
`exit-codes`/`gate-common-flags` (grepped directly — neither literal
string contains the token). `help.clj:471` (`write-cli-md!`'s own
docstring, "moved out of components/corpus/docs/ to the root user
path, ADR-0010") is class (ii), doc-doctrine — a first sweep-pass sed
touched it by accident (a blanket `ADR-0010` → `tools/ADR-0010`
substitution across the file, not scoped per-line); caught before
commit by a full re-grep of the file, reverted to bare `ADR-0010`.
Recorded here as the sweep's own near-miss, not silently corrected
without disclosure. `bases/cli/test/ehrt/cli/core_test.clj`'s two sites
(lines 39, 2056) are both `;;` comments, never inside a string literal
a test asserts against — a repo-wide `grep -rn "adr-0010"
--include="*_test.clj"` count-lock probe, run before editing, found no
test anywhere locking on the literal string. Zero behavior change
across all thirteen `.clj` files: every edit is comment-or-docstring
text, confirmed by direct inspection of each site's own surrounding
form before editing, and by the oracle bracket after (below).

**Out-of-fence verdict-family sites, disclosed, not touched** (the
touch fence explicitly excludes these; the ADR-0099 form widens the
*inventory*, never the edit, beyond what a session's own fence
allows): `bin/ehrt:3` (entry-point comment); `notes/2026-07-30-
refactoring-review.md:33` (a historical review document, out of
`notes/`'s own editable scope this session); `test-fixtures/reports/
pre-split-baseline.edn:2` (explicitly fenced out, `test-fixtures/`).
Every `notes/adr/`, `notes/tools/`, `notes/sim/`, `.agents/prompts/`,
and `.agents/session-records/` hit is class (iii) or frozen-archive,
left untouched per the frozen-files-never-edited doctrine.

### Oracle

Blast-radius prediction: pure identity, all 35 roots — every `src`
edit this session is comment-or-docstring-only, and every `docs`/
generated-page edit is prose/citation text with zero effect on
generation logic. `bin/regression-oracle` run post-change: all 35
roots byte-identical, matching the prediction exactly.

### make test

Green before both pushes (Step 1's commit, Step 2's commit) — full
`clojure -M:poly check` + `test :all skip:integration` +
`bin/verify-nist-lock`, zero failures, zero generative-seed findings.

### Manual-review, dimension 4 only — targeted re-run

Per the roadmap row's own revisit trigger ("a future session willing
to sweep Chapters 1, 3-7 adding glossary links at each first use,
mirroring Chapter 2's and Chapter 8's own existing pattern"): re-scored
dimension 4 alone against the finished manual, all eight chapters,
after this session's own edits.

**PASS.** Every chapter now links `../glossary.md` at first use of a
glossary-defined term: Chapters 1, 3–7 per the table in Step 1 above;
Chapters 2 (`02-setup-first-corpus.md:61,63,104`) and 8
(`08-your-own-data.md:19,20,21,41`) already conformed and were left
untouched. Chapter 3's own "Pathway"/"script space"/"truth space" —
the glossary's own named most-common misreading — now link at first
use (lines 66, 95, 103). No chapter restates a definition (dimension 2
re-checked incidentally: every added link is a single bracketed word,
none is a table or a restated flag/operator list — still PASS). No
anchor risk introduced: every added link is a bare `../glossary.md`
page reference with no `#anchor`, so dimension 3 is untouched by
construction. The other seven dimensions were NOT re-run, per this
session's own narrower charter — dimension 1 (strip executability)
stays the open FAIL it was at ADR-0125's own close.

### Fences honored

Zero edits to `demos/`, `test-fixtures/`, `.github/`, frozen registers
(`notes/tools/`, `notes/sim/`), `docs/dev/`, or any exerciser/lint
mechanism. Zero widening beyond disclosure for the sim-identity family
and the three out-of-fence verdict-family sites named above.

### Disposition

Manual-review dimension-4 row: CLOSED. Citation errata sweep row:
CLOSED, with the sim-identity family disclosed as unfinished (a named
future, not this sweep's own scope). Ceremony-scripts row: now front
of the Next-section queue.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Manual-arc tag payment, glossary linkage, citation errata sweep — tags `stable-20260813-manual-arc-close` at `c6d0257` (ADR-0125's own close, CI-verified green); adds `docs/glossary.md` links at first use of glossary-defined terms across manual Chapters 1, 3-7 (Chapters 2, 8 already conformed), closing the manual-review skill's own dimension-4 FAIL with a targeted re-run: PASS; runs the citation errata sweep ADR-0125 chartered, origin-qualifying every in-fence bare `ADR-0010` verdict-family citation to `tools/ADR-0010` across `docs/judge-calibration.md`/`docs/formats.md` (footnote form, renamed `[^adr-0010]` -> `[^tools-adr-0010]`), `docs/manual/assets/verdict-ranking.svg`, `components/corpus/docs/palgebra-design.md`/`research/judge-v2-nist-spike-notes.md`, `components/corpus/docs/use-cases.edn` (regenerating `docs/use-cases/profile-tier-hl7v2-conformance-gating.md`), and thirteen `.clj` comment/docstring sites (the widened, author-licensed scope) -- zero behavior change, confirmed per-site and by a pure-identity oracle bracket; corrects the channel's own probe (`docs/glossary.md` carries no verdict-family citation in the live tree; untouched); discloses a fourth, previously-unnamed drift family (bare `ADR-0010` in `components/sim/docs/`/`components/sim-trajectory/docs/` meaning the frozen sim repo's own `sim/ADR-0010`, 17 sites, 6 files) found but out of this session's own touch fence, not fixed; dimension-1 (strip executability) stays open, untouched; zero `src` behavior touched anywhere, the oracle holds pure identity across all 35 roots

### Rulings-register history (moved verbatim from `.agents/rulings.md` by ADR-0145, 2026-08-17)

## From ADR-0126 (manual-arc tag payment, glossary linkage, citation
errata sweep; ruled 2026-08-13, all rulings from this session's own
driving prompt)

- **Citation sweep chartered, executed** [A, verbatim "a, go",
  2026-08-13, restated from ADR-0125's own charter above — recorded
  again here since this is the session that actually ran it].
- **Session pairing, glossary row + sweep in one session** [A, verbatim
  "b go", 2026-08-13, design channel]: the manual-review dimension-4
  fix (glossary linkage) and the citation errata sweep landed together,
  one session, rather than split.
- **Sweep scope includes the `.clj` comment/docstring sites, whole
  sweep in one session per the ADR-0099 rule form** [A, verbatim "a",
  2026-08-13, design channel]: the thirteen `.clj` sites named in the
  driving prompt's own Step 2d widened the sweep beyond the original
  ADR-0125 charter's "docs-only" framing — licensed explicitly by this
  ruling, not a silent scope creep.
- **Standing from ADR-0125, restated, unchanged**: dimension-1 (strip
  executability) stays OPEN — not touched this session, no
  exerciser/lint mechanism edited.
