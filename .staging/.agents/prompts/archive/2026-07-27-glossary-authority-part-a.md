# 2026-07-27 — Part A (`ehr-testing-tools`): the authoritative glossary — conformance vocabulary comes home, corrected to R3 doctrine

## Context

`ehr-testing-tools` has no glossary. Its core vocabulary — judge,
verdict, findings, gate, baseline — is defined reader-facing only in
`ehr-testing-sim`'s `docs/GLOSSARY.md` ("Conformance & gating
vocabulary" block), which honestly declares tools authoritative and
has already drifted exactly as that disclaimer predicted: sim's block
teaches `:indeterminate` as "examined but cannot classify (e.g. a
check needing a terminology tier this judge lacks)" — the pre-R3
semantics. In this repo today, that exact case is
`no-verdict(:terminology-suppressed)`; `:indeterminate` is RESERVED
with no producer, and `no-verdict` carries a Malli-enforced mandatory
`:cause` pairing (`judge/finding.clj`, ADR-0010 as amended by R3).
Sim's block also predates ADR-0015's two-baseline design. This session
creates `docs/GLOSSARY.md` here as the authoritative home, seeded from
sim's prose but corrected against this repo's code and ADRs, records
the drift instance as a facts-register row, and lands a short ADR
making the authority placement explicit. A companion session (Part B,
sim repo, run AFTER this session's push) shrinks sim's block to a
pointer.

## Read first

* Sim's `docs/GLOSSARY.md` §"Conformance & gating vocabulary" and the
  "Diagnosis" entry in §"Terms with colliding meanings" (sibling
  checkout `../ehr-testing-sim`, or the public repo) — the prose donor
* `src/ehr_testing_tools/judge/finding.clj` — the verdict doctrine of
  record (four arms, reservation, cause pairing, worst-of ranking)
* `notes/ADRs.md` ADR-0010 (+ R3 amendment), ADR-0013 (findings not
  failures), ADR-0015 (two baselines), ADR-0016 (tiers)
* `docs/palgebra-design.md` D10; `src/ehr_testing_tools/cli/help.clj`
  (exit codes, `--treat-no-verdict-as`)
* `docs/positioning.md`, `docs/README.md` (where the glossary gets
  linked from)

## Author rulings

1. Code and ADRs are authoritative; sim's prose is a donor. Where
   sim's wording and this repo's code disagree, the code wins and the
   discrepancy is evidence for F30 (Step 1), not a judgment call.
2. Required entries (the family conformance set, corrected):
   * Judge — examines one artifact against one tier; judges decide,
     they do not act (the R1 judge/gate distinction, stated).
   * Verdict — four arms `:pass / :rejected / :indeterminate /
     :no-verdict`; `:indeterminate` RESERVED, no producer since R3;
     `:no-verdict` always paired with a `:cause` keyword (name
     `:terminology-suppressed` as the worked example); worst-of
     ranking `:pass < :indeterminate < :no-verdict < :rejected` with
     the one-sentence R3 rationale (a confirmed violation dominates);
     exit ladder 0/1/2/3 and `--treat-no-verdict-as`.
   * Error (vs. rejected) — sim's wording is good; keep its substance
     (rejected is an answer; error is the absence of the ability to
     answer).
   * Findings — itemized, located reasons; carries the "findings, not
     failures" assertion-discipline sentence (ADR-0013): the consumer
     loop asserts the gate runs and verdicts, never that everything
     passes.
   * Report, Baseline — baseline entry updated for ADR-0015's TWO
     baselines (legacy-floor and full-capability) and
     ratification-by-regeneration.
   * Gate — the workflow that runs judges and acts on verdicts;
     contrast with Judge per R1.
   * Diagnosis (never a judge term) — the family ruling from sim's
     colliding-terms section, ported whole: never used for a judge's
     explanation of a verdict (that is findings), with the
     clinical-audience rationale.
   * InjectChurn (disambiguation) — one defensive line: sim's own
     coherent operational-churn weaving, NOT fault injection; fault
     injection lives here, in the mutation operators.
3. Permitted, bounded extension: a short "Corpus vocabulary" section
   (operator, mutant, lineage, catalog, intake, manifest sidecar,
   corpus layer) sourced ONLY from existing docstrings and docs in
   this repo — no new doctrine authored in a glossary. If an entry
   would require deciding anything, leave it out.
4. Structure mirrors the family style: a colliding-terms-first section
   is optional; what is mandatory is that each entry cites its
   register of record (the ADR or namespace) so the glossary is
   downstream of doctrine, never a second source of it.
5. Numbering is verified at run time. Expected next: ADR-0017, facts
   row F30. If the frontier moved, take the actual next numbers and
   say so in the report.

## Steps

### Step 1 — Facts-register row F30 (the drift instance)

Record: sim's `docs/GLOSSARY.md` conformance block, as of sim's
current HEAD (pin the sha), teaches pre-R3 `:indeterminate` semantics
— quote the drifted clause, cite `judge/finding.clj` +
ADR-0010/R3 as the contradicting registers of record, and note this as
the motivating evidence for placing the authoritative glossary in this
repo (ADR-0017). Evidence column: direct read of both files this
session.

Commit: `notes: F30 — sim's family-vocabulary block drifted to pre-R3
verdict semantics; evidence for glossary authority placement`

### Step 2 — `docs/GLOSSARY.md`

Write it per rulings 2–4. Link it from `docs/README.md` and, where the
docs-index convention expects it, `README.md`/`docs/positioning.md`
(follow existing link style; do not restructure those docs otherwise).

Commit: `docs: authoritative glossary — conformance vocabulary
corrected to R3 doctrine, family rulings ported (ADR-0017)`

### Step 3 — ADR-0017

The decision: this repo's `docs/GLOSSARY.md` is the authoritative home
of the family's conformance-and-gating vocabulary; sim's block becomes
a pointer (its session is named, not performed here). Motivation: the
F30 drift instance; the principle that vocabulary is load-bearing and
must live where its registers of record live. Alternatives considered:
leave the full block in sim with a sync ritual (rejected — F30 shows
the ritual doesn't exist and wouldn't be enforced); duplicate in both
repos (rejected — two authoritative copies is how F30 happened);
glossary in the guide repo (rejected for the conformance set — the
guide teaches method, this repo owns the judge/gate doctrine; the
planned guide crosswalk reconciles against THIS glossary for family
terms).

Commit: `adr: ADR-0017 — glossary authority placement; sim's block
becomes a pointer`

### Step 4 — Verify and archive

T0 (`make test` + both lints + quickstart-fresh) green — docs-only
session, but the freshness gates read docs. Archive this prompt to
`.agents/prompts/archive/` with deviation appendix if any. Push per
the session-end ritual (Part B depends on this push).

Commit: `prompts: archive 2026-07-27 glossary-authority session (part
A)`

## Final report

Numbers actually taken (ADR/F row), entries authored beyond the
required set (ruling 3's bounded extension), any donor-prose vs. code
discrepancies found beyond the F30 clause, deviations.

---

# Part B (`ehr-testing-sim`, run AFTER Part A is pushed): shrink the family block to a pointer; repoint the crosswalk

## Context

`ehr-testing-tools` now carries the authoritative `docs/GLOSSARY.md`
(its ADR-0017; its F30 records that this repo's "Conformance & gating
vocabulary" block drifted to pre-R3 verdict semantics —
`:indeterminate` described with what is now
`no-verdict(:terminology-suppressed)`, and the mandatory `:cause`
pairing missing). Per this block's own standing disclaimer ("tools'
own code and ADRs are authoritative if any detail here drifts"), this
session shrinks the block to a pointer plus the minimal local
one-liners, with a dated note per fix-forward discipline — the pointer
IS the correction; no pre-R3 text survives. Three crosswalk references
are repointed for family terms.

## Read first

* `docs/GLOSSARY.md` — §"Conformance & gating vocabulary" (the
  block), the "Diagnosis" colliding-terms entry, and the crosswalk
  mentions at the block's preamble and near line 585
* `docs/README.md` (crosswalk mention, ~line 210)
* Tools' `docs/GLOSSARY.md` on the public repo (the pointer target —
  confirm it exists and note its exact URL and heading anchors)
* `notes/ADRs.md` ADR-0001 (boundary), ADR-0014 (no acceptance
  instruments) — context only, unchanged

## Author rulings

1. The block shrinks; it does not vanish. Keep the section heading.
   Its body becomes: (a) a dated note (2026-07-27) stating the
   definitions moved to tools' authoritative glossary, why (the drift
   tools' F30 records — one sentence, no self-flagellation, fix-forward
   register), and the link; (b) at most four one-line local glosses for
   the terms sim's own docs use mid-sentence — gate, findings, verdict,
   baseline — each ending "see tools' glossary," each too short to
   drift (no enumerations, no semantics of arms or causes).
2. No pre-R3 text survives anywhere in this repo. The full Judge /
   Verdict / Error / Findings / Report / Baseline / Gate definitions
   are deleted from the block, not corrected in place — correcting
   them here would recreate the second authoritative copy.
3. Diagnosis entry stays, halved. The clinical half (DG1, ICD-10-CM,
   generated content) is sim's and remains; the judge-side ruling
   ("never a judge's explanation — that is findings") compresses to
   one sentence pointing at tools' glossary entry.
4. Crosswalk references (GLOSSARY block preamble, GLOSSARY ~585,
   `docs/README.md` ~210): family conformance terms now reconcile
   against tools' glossary; the guide crosswalk remains planned for
   method/teaching terms. Adjust wording minimally at all three sites;
   do not expand the crosswalk plan itself.
5. No ADR here. The block's own disclaimer pre-authorized exactly this
   move; the dated note in the glossary is the record. If the agent
   finds this repo's conventions demand an ADR anyway (AUTHORS-GUIDE
   says so explicitly), STOP and report rather than authoring one
   unprompted.
6. Sweep before closing: grep this repo for `indeterminate`,
   `no-verdict`, `verdict` outside the glossary — any other doc
   teaching verdict-arm semantics (rather than merely using a term
   with a pointer available) is a finding for the report, not an
   in-session fix.

## Steps

### Step 1 — Shrink the block

Per rulings 1–2. Verify the tools glossary URL resolves (fetch it; if
Part A has not been pushed, STOP).

Commit: `docs: family conformance vocabulary now points at tools'
authoritative glossary (their ADR-0017); local one-liners only`

### Step 2 — Diagnosis entry and crosswalk references

Per rulings 3–4, all four edit sites.

Commit: `docs: diagnosis entry halved to sim's clinical side;
crosswalk repointed for family terms`

### Step 3 — Sweep, verify, archive

Ruling 6's sweep, classified in the report. Repo's own fast suite
green (docs-only, but run it). Archive this prompt to
`.agents/prompts/archive/` with deviation appendix if any. Push per
the session-end ritual.

Commit: `prompts: archive 2026-07-27 glossary-pointer session (part
B)`

## Final report

The four one-liners as landed, all crosswalk-site diffs summarized,
sweep findings (docs still teaching verdict semantics, if any),
deviations.

---

## Session deviation record (Part A, 2026-07-27)

Ruling 5 expected the next ADR number to be ADR-0017 and the next
facts-register row to be F30. At session start, `notes/ADRs.md`
already carried an **ADR-0017** ("Formal Source and Sink types...",
Accepted 2026-07-27, landed by an earlier session the same day) — the
ADR frontier had moved. F30 was still the correct next facts-register
row (F1–F29 already present, none named F30). Per ruling 5's own
instruction, the actual next ADR number was taken instead:
**ADR-0018**. All commit messages, the glossary's own citations, and
this prompt's Step 3/Final-report expectations are adjusted
accordingly — every place the prompt body above says "ADR-0017" for
*this session's* decision should be read as ADR-0018; the prompt body
itself archives unedited, as issued, per this repo's
research-doc-errata-style convention (`AUTHORS-GUIDE.md` §7). Part B's
own read of "their ADR-0017" (in its Step 1 commit-message template)
must likewise be read as ADR-0018 when that session runs.

No other deviations: F30's evidence, the required glossary entries
(ruling 2), and the bounded Corpus-vocabulary extension (ruling 3)
landed as specified. T0 (`make test` + `lint-pipeline` + `lint-deps` +
`quickstart-fresh`) ran green: 523 tests / 1577 assertions, 0
failures/errors; both lints OK; `quickstart-fresh` OK (15 commands,
README's Quickstart fence and `bin/quickstart-demo` agree line-for-line).
